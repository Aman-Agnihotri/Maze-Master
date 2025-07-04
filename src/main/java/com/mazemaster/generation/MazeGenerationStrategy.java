// src/main/java/com/mazemaster/generation/MazeGenerationStrategy.java
package com.mazemaster.generation;

import com.mazemaster.model.Maze;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interface for maze generation algorithms.
 */
public interface MazeGenerationStrategy {
    void generate(Maze maze, MazeGenerationListener listener, AtomicBoolean stopFlag);
}