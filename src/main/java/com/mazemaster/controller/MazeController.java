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
import java.util.List;
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
        
        // Ensure odd dimensions for proper generation
        if (rows % 2 == 0) rows++;
        if (columns % 2 == 0) columns++;
        
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
            } finally {
                isGenerating = false;
                isGenerationPaused = false;
                if (view != null && !stopGeneration.get()) {
                    view.onGenerationCompleted();
                }
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
                
                boolean solved = solver.solve(maze, currentSolvingAlgorithm, stopSolving, pauseSolving);
                
                if (view != null && !stopSolving.get()) {
                    view.onSolvingCompleted(solved);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
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
            if (wasSolvingPaused || isMazeFullyGenerated()) {
                // Solving paused OR maze is fully generated: Just clear the solution, keep the generated maze
                maze.resetSolution(); // This keeps the maze structure, clears only the solution
                if (view != null) {
                    view.updateMaze(maze);
                    view.refresh();
                    // Update UI normally (maze exists, ready for solving again)
                    view.updateControlsState(false, false);
                }
            } else if (wasGenerationPaused) {
                // Generation paused OR normal case (idle with incomplete/blank maze): Reset to completely blank state
                maze.reset(); // This makes it completely blank
                if (view != null) {
                    view.updateMaze(maze);
                    view.refresh();
                    // Update UI as if we created a new maze (all buttons active)
                    view.updateControlsState(false, false);
                }
            }
        }
    }
    
    public boolean isMazeFullyGenerated() {
        if (maze == null) return false;
        
        // Check if there are any empty cells (indicating the maze has been generated)
        // A fully generated maze will have paths (empty cells) between walls
        for (int i = 0; i < maze.getRows(); i++) {
            for (int j = 0; j < maze.getColumns(); j++) {
                int cellValue = maze.getCell(i, j);
                if (cellValue == Maze.EMPTY || cellValue == Maze.PATH || 
                    cellValue == Maze.VISITED || cellValue == Maze.START) {
                    return true; // Found at least one non-wall cell, maze is generated
                }
            }
        }
        return false; // All cells are walls, maze is not generated
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
    
    // =========================
    // Cleanup
    // =========================
    
    public void shutdown() {
        stopAllOperations();
    }
}
