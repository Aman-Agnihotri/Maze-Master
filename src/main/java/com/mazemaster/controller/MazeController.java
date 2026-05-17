// src/main/java/com/mazemaster/controller/MazeController.java
package com.mazemaster.controller;

import com.mazemaster.model.Maze;
import com.mazemaster.generation.MazeGenerator;
import com.mazemaster.generation.MazeGenerationListener;
import com.mazemaster.solving.MazeSolver;
import com.mazemaster.solving.MazeSolvingListener;
import com.mazemaster.ui.MazeView;

import java.awt.Point;
import java.io.*;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Controller class that coordinates between the model (Maze), 
 * algorithms (Generator/Solver), and view (UI).
 * Follows the MVC pattern and handles application state management.
 */
public class MazeController implements MazeGenerationListener, MazeSolvingListener {
    
    // Core components
    private Maze maze;
    private final MazeGenerator generator;
    private final MazeSolver solver;
    private MazeView view;
    
    // State management
    private volatile boolean isGenerating = false;
    private volatile boolean isSolving = false;
    private volatile boolean isGenerationPaused = false;
    private volatile boolean isSolvingPaused = false;
    
    private final AtomicBoolean stopGeneration = new AtomicBoolean(false);
    private final AtomicBoolean stopSolving = new AtomicBoolean(false);
    private final AtomicBoolean pauseGeneration = new AtomicBoolean(false);
    private final AtomicBoolean pauseSolving = new AtomicBoolean(false);
    
    // Threading
    private Thread generationThread;
    private Thread solvingThread;
    
    // Configuration
    private String currentGenerationAlgorithm = "DFS";
    private String currentSolvingAlgorithm = "Depth First Search";
    private static final String SAVE_FILE_NAME = "mazeSave.ser";
    private static final int MIN_MAZE_DIMENSION = 5;
    private static final int MAX_MAZE_DIMENSION = 200;
    
    public MazeController() {
        this.generator = new MazeGenerator();
        this.solver = new MazeSolver();
        
        // Set up listeners
        generator.setGenerationListener(this);
        solver.setSolvingListener(this);
        
        // Initialize with default maze
        createNewMaze(41, 51);
    }
    
    public void setView(MazeView view) {
        this.view = view;
        if (view != null) {
            view.updateMaze(maze);
        }
    }
    
    // =========================
    // Public API Methods
    // =========================
    
    public void createNewMaze(int rows, int columns) {
        stopAllOperations();
        
        rows = normalizeDimension(rows);
        columns = normalizeDimension(columns);
        
        this.maze = new Maze(rows, columns);
        
        if (view != null) {
            view.updateMaze(maze);
            view.refresh();
        }
    }
    
    public void generateMaze() {
        if (isGenerating || isSolving) {
            return;
        }
        
        isGenerating = true;
        isGenerationPaused = false;
        stopGeneration.set(false);
        pauseGeneration.set(false);
        
        generationThread = new Thread(() -> {
            try {
                if (view != null) {
                    view.onGenerationStarted();
                }
                
                generator.generate(maze, currentGenerationAlgorithm, stopGeneration, pauseGeneration);
                
            } catch (Exception e) {
                e.printStackTrace();
                if (view != null) {
                    view.showMessage("Error generating maze: " + e.getMessage(), true);
                }
            } finally {
                isGenerating = false;
                isGenerationPaused = false;
            }
        });
        
        generationThread.start();
    }
    
    public void solveMaze() {
        if (isGenerating || isSolving || maze == null) {
            return;
        }
        
        isSolving = true;
        isSolvingPaused = false;
        stopSolving.set(false);
        pauseSolving.set(false);
        
        solvingThread = new Thread(() -> {
            try {
                if (view != null) {
                    view.onSolvingStarted();
                }
                
                solver.solve(maze, currentSolvingAlgorithm, stopSolving, pauseSolving);
                // Completion is reported through MazeSolvingListener to keep lifecycle events centralized.
                
            } catch (Exception e) {
                e.printStackTrace();
                if (view != null) {
                    view.showMessage("Error solving maze: " + e.getMessage(), true);
                }
            } finally {
                isSolving = false;
                isSolvingPaused = false;
            }
        });
        
        solvingThread.start();
    }
    
    public void pauseResumeCurrentOperation() {
        if (isGenerating) {
            if (isGenerationPaused) {
                resumeGeneration();
            } else {
                pauseGeneration();
            }
        } else if (isSolving) {
            if (isSolvingPaused) {
                resumeSolving();
            } else {
                pauseSolving();
            }
        }
    }
    
    private void pauseGeneration() {
        if (isGenerating && !isGenerationPaused) {
            isGenerationPaused = true;
            pauseGeneration.set(true);
            if (view != null) {
                view.onOperationPaused();
            }
        }
    }
    
    private void resumeGeneration() {
        if (isGenerating && isGenerationPaused) {
            isGenerationPaused = false;
            pauseGeneration.set(false);
            if (view != null) {
                view.onOperationResumed();
            }
        }
    }
    
    private void pauseSolving() {
        if (isSolving && !isSolvingPaused) {
            isSolvingPaused = true;
            pauseSolving.set(true);
            if (view != null) {
                view.onOperationPaused();
            }
        }
    }
    
    private void resumeSolving() {
        if (isSolving && isSolvingPaused) {
            isSolvingPaused = false;
            pauseSolving.set(false);
            if (view != null) {
                view.onOperationResumed();
            }
        }
    }
    
    public void stopCurrentOperation() {
        if (isGenerating) {
            stopGeneration.set(true);
            pauseGeneration.set(false);
            isGenerationPaused = false;
            if (generationThread != null) {
                try {
                    generationThread.join(1000); // Wait up to 1 second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        if (isSolving) {
            stopSolving.set(true);
            pauseSolving.set(false);
            isSolvingPaused = false;
            if (solvingThread != null) {
                try {
                    solvingThread.join(1000); // Wait up to 1 second
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    
    public void resetMaze() {
        boolean wasGenerationPaused = isGenerating && isGenerationPaused;
        boolean wasSolvingPaused = isSolving && isSolvingPaused;
        
        // If operations are running (including paused), we need to stop them completely
        if (isGenerating || isSolving) {
            stopAllOperations();
        }
        
        if (maze != null) {
            if (wasGenerationPaused) {
                // A paused generation leaves a partial maze. Reset to a blank structure for a clean start.
                maze.reset();
                if (view != null) {
                    view.updateMaze(maze);
                    view.refresh();
                    view.updateControlsState(false, false);
                }
            } else if (wasSolvingPaused || isMazeFullyGenerated()) {
                // Solving paused OR maze is fully generated: clear only solver markings.
                maze.resetSolution();
                if (view != null) {
                    view.updateMaze(maze);
                    view.refresh();
                    view.updateControlsState(false, false);
                }
            } else {
                // Blank or incomplete idle maze: reset to all walls.
                maze.reset();
                if (view != null) {
                    view.updateMaze(maze);
                    view.refresh();
                    view.updateControlsState(false, false);
                }
            }
        }
    }
    
    public boolean isMazeFullyGenerated() {
        if (maze == null) return false;

        if (!maze.isWalkable(maze.getStartRow(), maze.getStartCol()) ||
            !maze.isWalkable(maze.getGoalRow(), maze.getGoalCol())) {
            return false;
        }

        boolean[][] visited = new boolean[maze.getRows()][maze.getColumns()];
        Queue<Point> queue = new ArrayDeque<>();
        queue.offer(new Point(maze.getStartRow(), maze.getStartCol()));
        visited[maze.getStartRow()][maze.getStartCol()] = true;

        int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        while (!queue.isEmpty()) {
            Point current = queue.poll();
            if (current.x == maze.getGoalRow() && current.y == maze.getGoalCol()) {
                return true;
            }

            for (int[] direction : directions) {
                int nextRow = current.x + direction[0];
                int nextCol = current.y + direction[1];
                if (maze.isWalkable(nextRow, nextCol) && !visited[nextRow][nextCol]) {
                    visited[nextRow][nextCol] = true;
                    queue.offer(new Point(nextRow, nextCol));
                }
            }
        }

        return false;
    }
    
    public void clearSolutionOnly() {
        // This method only clears the solution, keeping the generated maze structure
        if (maze != null) {
            // If operations are running, stop them first
            if (isGenerating || isSolving) {
                stopAllOperations();
            }
            
            maze.resetSolution();
            if (view != null) {
                view.updateMaze(maze);
                view.refresh();
            }
        }
    }
    
    public void clearSolution() {
        if (!isGenerating && !isSolving && maze != null) {
            maze.resetSolution();
            if (view != null) {
                view.updateMaze(maze);
                view.refresh();
            }
        }
    }
    
    public void saveMaze() {
        if (maze == null) return;
        
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(SAVE_FILE_NAME))) {
            out.writeObject(maze);
            if (view != null) {
                view.showMessage("Maze saved successfully!", false);
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (view != null) {
                view.showMessage("Error saving maze: " + e.getMessage(), true);
            }
        }
    }
    
    public void loadMaze() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(SAVE_FILE_NAME))) {
            stopAllOperations();
            
            Maze loadedMaze = (Maze) in.readObject();
            this.maze = loadedMaze;
            
            if (view != null) {
                view.updateMaze(maze);
                view.refresh();
                view.showMessage("Maze loaded successfully!", false);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            if (view != null) {
                view.showMessage("Error loading maze: " + e.getMessage(), true);
            }
        }
    }
    
    // =========================
    // Configuration Methods
    // =========================
    
    public void setGenerationAlgorithm(String algorithm) {
        if (!isGenerating && generator.getAvailableAlgorithms().contains(algorithm)) {
            this.currentGenerationAlgorithm = algorithm;
        }
    }
    
    public void setSolvingAlgorithm(String algorithm) {
        if (!isSolving && solver.getAvailableAlgorithms().contains(algorithm)) {
            this.currentSolvingAlgorithm = algorithm;
        }
    }
    
    public void setAnimationSpeed(int speed) {
        // Speed is inverse - higher slider value = slower animation
        int delay = Math.max(1, speed);
        generator.setDelay(delay);
        solver.setDelay(delay);
    }
    
    // =========================
    // State Query Methods
    // =========================
    
    public boolean isGenerating() { return isGenerating; }
    public boolean isSolving() { return isSolving; }
    public boolean isBusy() { return isGenerating || isSolving; }
    public boolean isGenerationPaused() { return isGenerationPaused; }
    public boolean isSolvingPaused() { return isSolvingPaused; }
    public boolean isAnyOperationPaused() { return isGenerationPaused || isSolvingPaused; }
    
    public Maze getMaze() { return maze; }
    public String getCurrentGenerationAlgorithm() { return currentGenerationAlgorithm; }
    public String getCurrentSolvingAlgorithm() { return currentSolvingAlgorithm; }
    
    public java.util.Set<String> getAvailableGenerationAlgorithms() {
        return generator.getAvailableAlgorithms();
    }
    
    public java.util.Set<String> getAvailableSolvingAlgorithms() {
        return solver.getAvailableAlgorithms();
    }
    
    // =========================
    // MazeGenerationListener Implementation
    // =========================
    
    @Override
    public void onCellChanged(int row, int col, int newValue) {
        if (view != null) {
            view.onCellChanged(row, col, newValue);
        }
    }
    
    @Override
    public void onGenerationStep() {
        if (view != null) {
            view.refresh();
        }
    }
    
    @Override
    public void onGenerationComplete() {
        isGenerating = false;
        isGenerationPaused = false;
        if (view != null) {
            view.onGenerationCompleted();
        }
    }
    
    // =========================
    // MazeSolvingListener Implementation
    // =========================
    
    @Override
    public void onCellExplored(int row, int col) {
        if (view != null) {
            view.onCellChanged(row, col, maze.getCell(row, col));
            view.refresh();
        }
    }
    
    @Override
    public void onCellBacktracked(int row, int col) {
        if (view != null) {
            view.onCellChanged(row, col, maze.getCell(row, col));
            view.refresh();
        }
    }
    
    @Override
    public void onPathFound(List<Point> path) {
        if (view != null) {
            view.onPathFound(path);
        }
    }
    
    @Override
    public void onSolvingComplete(boolean solved) {
        isSolving = false;
        isSolvingPaused = false;
        if (view != null) {
            view.onSolvingCompleted(solved);
        }
    }
    
    // =========================
    // Private Helper Methods
    // =========================
    
    private void stopAllOperations() {
        // First, clear pause flags to allow threads to continue and check stop flags
        pauseGeneration.set(false);
        pauseSolving.set(false);
        
        // Then set stop flags
        stopGeneration.set(true);
        stopSolving.set(true);
        
        // Wait for threads to finish
        if (generationThread != null && generationThread.isAlive()) {
            try {
                generationThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        if (solvingThread != null && solvingThread.isAlive()) {
            try {
                solvingThread.join(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        // Reset all state flags
        isGenerating = false;
        isSolving = false;
        isGenerationPaused = false;
        isSolvingPaused = false;
    }

    private int normalizeDimension(int dimension) {
        int normalized = Math.max(MIN_MAZE_DIMENSION, Math.min(MAX_MAZE_DIMENSION, dimension));
        if (normalized % 2 == 0) {
            normalized = normalized == MAX_MAZE_DIMENSION ? normalized - 1 : normalized + 1;
        }
        return normalized;
    }
    
    // =========================
    // Cleanup
    // =========================
    
    public void shutdown() {
        stopAllOperations();
    }
}
