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
    private final AtomicBoolean stopGeneration = new AtomicBoolean(false);
    private final AtomicBoolean stopSolving = new AtomicBoolean(false);
    
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
        stopGeneration.set(false);
        
        generationThread = new Thread(() -> {
            try {
                if (view != null) {
                    view.onGenerationStarted();
                }
                
                generator.generate(maze, currentGenerationAlgorithm, stopGeneration);
                
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isGenerating = false;
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
        stopSolving.set(false);
        
        solvingThread = new Thread(() -> {
            try {
                if (view != null) {
                    view.onSolvingStarted();
                }
                
                boolean solved = solver.solve(maze, currentSolvingAlgorithm, stopSolving);
                
                if (view != null && !stopSolving.get()) {
                    view.onSolvingCompleted(solved);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isSolving = false;
            }
        });
        
        solvingThread.start();
    }
    
    public void stopCurrentOperation() {
        if (isGenerating) {
            stopGeneration.set(true);
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
        stopAllOperations();
        
        if (maze != null) {
            maze.reset();
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
        
        isGenerating = false;
        isSolving = false;
    }
    
    // =========================
    // Cleanup
    // =========================
    
    public void shutdown() {
        stopAllOperations();
    }
}