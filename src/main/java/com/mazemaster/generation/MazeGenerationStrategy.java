// src/main/java/com/mazemaster/generation/MazeGenerationStrategy.java
package com.mazemaster.generation;

import com.mazemaster.model.Maze;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interface for maze generation algorithms.
 * Updated to support pause/resume functionality.
 */
public interface MazeGenerationStrategy {
    /**
     * Generate a maze using this algorithm.
     * 
     * @param maze The maze to generate
     * @param listener The listener for generation events
     * @param stopFlag Flag to indicate when generation should stop completely
     * @param pauseFlag Flag to indicate when generation should pause/resume
     */
    void generate(Maze maze, MazeGenerationListener listener, AtomicBoolean stopFlag, AtomicBoolean pauseFlag);
}