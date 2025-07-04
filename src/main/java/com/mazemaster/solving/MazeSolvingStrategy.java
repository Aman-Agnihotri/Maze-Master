// src/main/java/com/mazemaster/solving/MazeSolvingStrategy.java
package com.mazemaster.solving;

import com.mazemaster.model.Maze;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interface for maze solving algorithms.
 */
public interface MazeSolvingStrategy {
    boolean solve(Maze maze, MazeSolvingListener listener, AtomicBoolean stopFlag);
}