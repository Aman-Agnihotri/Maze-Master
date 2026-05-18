// src/main/java/com/mazemaster/controller/MazeController.java
package com.mazemaster.controller;

import com.mazemaster.model.Maze;
import com.mazemaster.generation.MazeGenerator;
import com.mazemaster.generation.MazeGenerationListener;
import com.mazemaster.persistence.MazeFileService;
import com.mazemaster.solving.MazeSolver;
import com.mazemaster.solving.MazeSolvingListener;
import com.mazemaster.ui.MazeView;

import java.awt.Point;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ThreadLocalRandom;
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
    private final MazeFileService mazeFileService;
    private final ExecutorService operationExecutor;
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
    
    // Background operations
    private Future<?> generationTask;
    private Future<?> solvingTask;
    
    // Configuration
    private String currentGenerationAlgorithm = "DFS";
    private String currentSolvingAlgorithm = "Depth First Search";
    private long currentGenerationSeed = createRandomSeed();
    private static final Path SAVE_FILE_PATH = Path.of("mazeSave.maze");
    private static final int MIN_MAZE_DIMENSION = 5;
    private static final int MAX_MAZE_DIMENSION = 200;
    
    public MazeController() {
        this.generator = new MazeGenerator();
        this.solver = new MazeSolver();
        this.mazeFileService = new MazeFileService();
        this.operationExecutor = Executors.newSingleThreadExecutor();
        
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
        maze.setGenerationMetadata(currentGenerationSeed, currentGenerationAlgorithm);
        
        if (view != null) {
            view.updateMaze(maze);
            view.setGenerationSeed(currentGenerationSeed);
            view.refresh();
        }
    }

    public void createMazeFromSeed(int rows, int columns, long seed) {
        if (isBusy()) {
            return;
        }

        setRandomSeed(seed);
        createNewMaze(rows, columns);
        generateMaze();
    }
    
    public void generateMaze() {
        if (isGenerating || isSolving) {
            return;
        }
        
        isGenerating = true;
        isGenerationPaused = false;
        stopGeneration.set(false);
        pauseGeneration.set(false);
        generator.setRandomSeed(currentGenerationSeed);
        maze.setGenerationMetadata(currentGenerationSeed, currentGenerationAlgorithm);
        
        generationTask = operationExecutor.submit(() -> {
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
    }
    
    public void solveMaze() {
        if (isGenerating || isSolving || maze == null) {
            return;
        }
        
        isSolving = true;
        isSolvingPaused = false;
        stopSolving.set(false);
        pauseSolving.set(false);
        
        solvingTask = operationExecutor.submit(() -> {
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
            waitForTask(generationTask);
        }
        
        if (isSolving) {
            stopSolving.set(true);
            pauseSolving.set(false);
            isSolvingPaused = false;
            waitForTask(solvingTask);
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
        saveMaze(SAVE_FILE_PATH);
    }

    public void saveMaze(Path path) {
        if (maze == null) return;
        
        try {
            mazeFileService.save(maze, path);
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
        loadMaze(SAVE_FILE_PATH);
    }

    public void loadMaze(Path path) {
        try {
            stopAllOperations();
            
            this.maze = mazeFileService.load(path);
            currentGenerationSeed = maze.getGenerationSeed();
            String loadedAlgorithm = maze.getGenerationAlgorithm();
            if (generator.getAvailableAlgorithms().contains(loadedAlgorithm)) {
                currentGenerationAlgorithm = loadedAlgorithm;
            }
            
            if (view != null) {
                view.updateMaze(maze);
                view.setGenerationSeed(currentGenerationSeed);
                view.setSelectedGenerationAlgorithm(currentGenerationAlgorithm);
                view.refresh();
                view.showMessage("Maze loaded successfully!", false);
            }
        } catch (IOException e) {
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

    public void setRandomSeed(long seed) {
        if (!isBusy()) {
            currentGenerationSeed = seed;
            generator.setRandomSeed(seed);
            if (view != null) {
                view.setGenerationSeed(seed);
            }
        }
    }

    public long randomizeGenerationSeed() {
        long seed = createRandomSeed();
        setRandomSeed(seed);
        return currentGenerationSeed;
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
    public long getCurrentGenerationSeed() { return currentGenerationSeed; }
    
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
        
        waitForTask(generationTask);
        waitForTask(solvingTask);
        
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

    private long createRandomSeed() {
        return ThreadLocalRandom.current().nextLong();
    }

    private void waitForTask(Future<?> task) {
        if (task == null || task.isDone()) {
            return;
        }

        try {
            task.get(1000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            task.cancel(true);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            task.cancel(true);
        } catch (ExecutionException e) {
            // Operation tasks report user-facing errors before they complete.
        }
    }

    // =========================
    // Cleanup
    // =========================
    
    public void shutdown() {
        stopAllOperations();
        operationExecutor.shutdownNow();
    }
}
