// src/main/java/com/mazemaster/generation/MazeGenerator.java
package com.mazemaster.generation;

import com.mazemaster.model.Maze;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main maze generator that orchestrates different generation algorithms.
 */
public class MazeGenerator {
    private final Map<String, MazeGenerationStrategy> strategies;
    private MazeGenerationListener listener;
    private int delayMs = 30;
    
    public MazeGenerator() {
        strategies = new HashMap<>();
        strategies.put("DFS", new DepthFirstSearchGenerator());
        strategies.put("Kruskal", new KruskalGenerator());
        strategies.put("Prim", new PrimGenerator());
    }
    
    public void setGenerationListener(MazeGenerationListener listener) {
        this.listener = listener;
    }
    
    public void setDelay(int delayMs) {
        this.delayMs = Math.max(1, delayMs);
    }
    
    public Set<String> getAvailableAlgorithms() {
        return strategies.keySet();
    }
    
    public void generate(Maze maze, String algorithm, AtomicBoolean stopFlag) {
        MazeGenerationStrategy strategy = strategies.get(algorithm);
        if (strategy == null) {
            strategy = strategies.get("DFS"); // Default fallback
        }
        
        maze.reset();
        strategy.generate(maze, listener, stopFlag);
    }
    
    /**
     * Depth-First Search maze generation (Recursive Backtracking)
     */
    private class DepthFirstSearchGenerator implements MazeGenerationStrategy {
        private Random random = new Random();
        
        @Override
        public void generate(Maze maze, MazeGenerationListener listener, AtomicBoolean stopFlag) {
            int rows = maze.getRows();
            int cols = maze.getColumns();
            
            // Ensure odd dimensions for proper generation
            if (rows % 2 == 0) rows--;
            if (cols % 2 == 0) cols--;
            
            // Create room grid and wall list
            List<Wall> walls = new ArrayList<>();
            int roomCount = 0;
            
            // Create rooms (negative numbers to track connectivity)
            for (int i = 1; i < rows - 1; i += 2) {
                for (int j = 1; j < cols - 1; j += 2) {
                    roomCount++;
                    maze.setCell(i, j, -roomCount);
                    
                    if (listener != null) {
                        listener.onCellChanged(i, j, Maze.EMPTY);
                        listener.onGenerationStep();
                    }
                    
                    // Add walls around this room
                    if (i < rows - 2) {
                        walls.add(new Wall(i + 1, j));
                    }
                    if (j < cols - 2) {
                        walls.add(new Wall(i, j + 1));
                    }
                    
                    if (stopFlag.get()) return;
                    
                    try {
                        Thread.sleep(delayMs / 4); // Faster room creation
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
            
            // Randomly tear down walls
            Collections.shuffle(walls, random);
            
            for (Wall wall : walls) {
                if (stopFlag.get()) return;
                
                tearDownWall(maze, wall.row, wall.col, listener);
                
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
            
            // Convert negative room numbers to empty cells
            for (int i = 1; i < rows - 1; i++) {
                for (int j = 1; j < cols - 1; j++) {
                    if (maze.getCell(i, j) < 0) {
                        maze.setCell(i, j, Maze.EMPTY);
                        if (listener != null) {
                            listener.onCellChanged(i, j, Maze.EMPTY);
                        }
                    }
                }
            }
            
            if (listener != null) {
                listener.onGenerationComplete();
            }
        }
        
        private void tearDownWall(Maze maze, int row, int col, MazeGenerationListener listener) {
            if (row % 2 == 1) { // Horizontal wall
                int leftRoom = maze.getCell(row, col - 1);
                int rightRoom = maze.getCell(row, col + 1);
                
                if (leftRoom != rightRoom) {
                    fillRoom(maze, row, col - 1, leftRoom, rightRoom);
                    maze.setCell(row, col, rightRoom);
                    
                    if (listener != null) {
                        listener.onCellChanged(row, col, Maze.EMPTY);
                    }
                }
            } else { // Vertical wall
                int topRoom = maze.getCell(row - 1, col);
                int bottomRoom = maze.getCell(row + 1, col);
                
                if (topRoom != bottomRoom) {
                    fillRoom(maze, row - 1, col, topRoom, bottomRoom);
                    maze.setCell(row, col, bottomRoom);
                    
                    if (listener != null) {
                        listener.onCellChanged(row, col, Maze.EMPTY);
                    }
                }
            }
        }
        
        private void fillRoom(Maze maze, int row, int col, int oldValue, int newValue) {
            if (maze.getCell(row, col) == oldValue) {
                maze.setCell(row, col, newValue);
                fillRoom(maze, row + 1, col, oldValue, newValue);
                fillRoom(maze, row - 1, col, oldValue, newValue);
                fillRoom(maze, row, col + 1, oldValue, newValue);
                fillRoom(maze, row, col - 1, oldValue, newValue);
            }
        }
    }
    
    /**
     * Placeholder for Kruskal's algorithm
     */
    private class KruskalGenerator implements MazeGenerationStrategy {
        @Override
        public void generate(Maze maze, MazeGenerationListener listener, AtomicBoolean stopFlag) {
            // TODO: Implement Kruskal's algorithm
            // For now, delegate to DFS
            new DepthFirstSearchGenerator().generate(maze, listener, stopFlag);
        }
    }
    
    /**
     * Placeholder for Prim's algorithm
     */
    private class PrimGenerator implements MazeGenerationStrategy {
        @Override
        public void generate(Maze maze, MazeGenerationListener listener, AtomicBoolean stopFlag) {
            // TODO: Implement Prim's algorithm
            // For now, delegate to DFS
            new DepthFirstSearchGenerator().generate(maze, listener, stopFlag);
        }
    }
    
    /**
     * Helper class to represent walls between rooms
     */
    private static class Wall {
        final int row;
        final int col;
        
        Wall(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }
}