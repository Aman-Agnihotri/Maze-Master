// src/main/java/com/mazemaster/solving/MazeSolvingStrategy.java
package com.mazemaster.solving;

import com.mazemaster.model.Maze;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interface for maze solving algorithms.
 * Updated to support pause/resume functionality.
 */
public interface MazeSolvingStrategy {
    /**
     * Solve a maze using this algorithm.
     * 
     * @param maze The maze to solve
     * @param listener The listener for solving events
     * @param stopFlag Flag to indicate when solving should stop completely
     * @param pauseFlag Flag to indicate when solving should pause/resume
     * @return true if a solution was found, false otherwise
     */
    boolean solve(Maze maze, MazeSolvingListener listener, AtomicBoolean stopFlag, AtomicBoolean pauseFlag);
}