// src/main/java/com/mazemaster/solving/MazeSolvingStrategy.java
package com.mazemaster.solving;

import com.mazemaster.model.Maze;

/**
 * Interface for maze solving algorithms.
 */
public interface MazeSolvingStrategy {
    /**
     * Solve a maze using this algorithm.
     * 
     * @param maze The maze to solve
     * @param context Runtime controls and listener callbacks for solving
     * @return true if a solution was found, false otherwise
     */
    boolean solve(Maze maze, MazeSolvingContext context);
}
