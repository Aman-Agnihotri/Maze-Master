// src/main/java/com/mazemaster/generation/MazeGenerationStrategy.java
package com.mazemaster.generation;

import com.mazemaster.model.Maze;

/**
 * Interface for maze generation algorithms.
 */
public interface MazeGenerationStrategy {
    /**
     * Generate a maze using this algorithm.
     * 
     * @param maze The maze to generate
     * @param context Runtime controls and listener callbacks for generation
     */
    void generate(Maze maze, MazeGenerationContext context);
}
