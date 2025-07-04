// src/main/java/com/mazemaster/ui/MazeView.java
package com.mazemaster.ui;

import com.mazemaster.model.Maze;
import java.awt.Point;
import java.util.List;

/**
 * Interface defining the contract between the controller and the UI layer.
 * This allows for multiple UI implementations (Swing, JavaFX, etc.) 
 * while maintaining separation of concerns.
 */
public interface MazeView {
    
    // =========================
    // Maze Display Methods
    // =========================
    
    /**
     * Update the view with a new maze model
     * @param maze The maze to display
     */
    void updateMaze(Maze maze);
    
    /**
     * Refresh the display to show current maze state
     */
    void refresh();
    
    /**
     * Notify that a specific cell has changed
     * @param row The row of the changed cell
     * @param col The column of the changed cell
     * @param newValue The new value of the cell
     */
    void onCellChanged(int row, int col, int newValue);
    
    // =========================
    // Generation Event Methods
    // =========================
    
    /**
     * Called when maze generation starts
     */
    void onGenerationStarted();
    
    /**
     * Called when maze generation completes
     */
    void onGenerationCompleted();
    
    // =========================
    // Solving Event Methods
    // =========================
    
    /**
     * Called when maze solving starts
     */
    void onSolvingStarted();
    
    /**
     * Called when maze solving completes
     * @param solved True if a solution was found, false otherwise
     */
    void onSolvingCompleted(boolean solved);
    
    /**
     * Called when a complete path is found
     * @param path The list of points representing the solution path
     */
    void onPathFound(List<Point> path);
    
    // =========================
    // User Interaction Methods
    // =========================
    
    /**
     * Display a message to the user
     * @param message The message to display
     * @param isError True if this is an error message, false for info
     */
    void showMessage(String message, boolean isError);
    
    /**
     * Get user input for maze dimensions
     * @return An array with [rows, columns] or null if cancelled
     */
    int[] getMazeDimensions();
    
    /**
     * Show progress indicator
     * @param show True to show, false to hide
     * @param message Optional message to display with progress
     */
    void showProgress(boolean show, String message);
    
    // =========================
    // Configuration Methods
    // =========================
    
    /**
     * Update the list of available generation algorithms
     * @param algorithms Set of algorithm names
     */
    void updateGenerationAlgorithms(java.util.Set<String> algorithms);
    
    /**
     * Update the list of available solving algorithms
     * @param algorithms Set of algorithm names
     */
    void updateSolvingAlgorithms(java.util.Set<String> algorithms);
    
    /**
     * Set the current generation algorithm selection
     * @param algorithm The algorithm name
     */
    void setSelectedGenerationAlgorithm(String algorithm);
    
    /**
     * Set the current solving algorithm selection
     * @param algorithm The algorithm name
     */
    void setSelectedSolvingAlgorithm(String algorithm);
    
    /**
     * Update UI state based on controller state
     * @param isGenerating True if generation is in progress
     * @param isSolving True if solving is in progress
     */
    void updateControlsState(boolean isGenerating, boolean isSolving);
    
    // =========================
    // Export Methods
    // =========================
    
    /**
     * Export the current maze view to an image file
     * @param filename The filename to save to
     * @return True if export was successful
     */
    boolean exportToImage(String filename);
    
    /**
     * Get the preferred export filename from user
     * @return The filename or null if cancelled
     */
    String getExportFilename();
}