// src/main/java/com/mazemaster/generation/MazeGenerationListener.java
package com.mazemaster.generation;

/**
 * Listener interface for maze generation events.
 */
public interface MazeGenerationListener {
    void onCellChanged(int row, int col, int newValue);
    void onGenerationStep();
    void onGenerationComplete();
}