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
            int rows = ensureOddDimension(maze.getRows());
            int cols = ensureOddDimension(maze.getColumns());
            
            List<Wall> walls = createInitialRoomsAndWalls(maze, rows, cols, listener, stopFlag);
            if (stopFlag.get()) return;
            
            processWalls(maze, walls, listener, stopFlag);
            if (stopFlag.get()) return;
            
            convertRoomNumbersToEmptyCells(maze, rows, cols, listener);
            
            if (listener != null) {
                listener.onGenerationComplete();
            }
        }
        
        private int ensureOddDimension(int dimension) {
            return dimension % 2 == 0 ? dimension - 1 : dimension;
        }
        
        private List<Wall> createInitialRoomsAndWalls(Maze maze, int rows, int cols, 
                                                    MazeGenerationListener listener, AtomicBoolean stopFlag) {
            List<Wall> walls = new ArrayList<>();
            int roomCount = 0;
            
            for (int i = 1; i < rows - 1; i += 2) {
                for (int j = 1; j < cols - 1; j += 2) {
                    if (stopFlag.get()) return walls;
                    
                    roomCount++;
                    maze.setCell(i, j, -roomCount);
                    
                    notifyRoomCreated(listener, i, j);
                    addWallsAroundRoom(walls, i, j, rows, cols);
                    
                    sleepBriefly();
                }
            }
            
            return walls;
        }
        
        private void notifyRoomCreated(MazeGenerationListener listener, int row, int col) {
            if (listener != null) {
                listener.onCellChanged(row, col, Maze.EMPTY);
                listener.onGenerationStep();
            }
        }
        
        private void addWallsAroundRoom(List<Wall> walls, int row, int col, int rows, int cols) {
            if (row < rows - 2) {
                walls.add(new Wall(row + 1, col));
            }
            if (col < cols - 2) {
                walls.add(new Wall(row, col + 1));
            }
        }
        
        private void processWalls(Maze maze, List<Wall> walls, MazeGenerationListener listener, AtomicBoolean stopFlag) {
            Collections.shuffle(walls, random);
            
            for (Wall wall : walls) {
                if (stopFlag.get()) return;
                
                tearDownWall(maze, wall.row, wall.col, listener);
                sleep();
            }
        }
        
        private void convertRoomNumbersToEmptyCells(Maze maze, int rows, int cols, MazeGenerationListener listener) {
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
        }
        
        private void tearDownWall(Maze maze, int row, int col, MazeGenerationListener listener) {
            if (isHorizontalWall(row)) {
                tearDownHorizontalWall(maze, row, col, listener);
            } else {
                tearDownVerticalWall(maze, row, col, listener);
            }
        }
        
        private boolean isHorizontalWall(int row) {
            return row % 2 == 1;
        }
        
        private void tearDownHorizontalWall(Maze maze, int row, int col, MazeGenerationListener listener) {
            int leftRoom = maze.getCell(row, col - 1);
            int rightRoom = maze.getCell(row, col + 1);
            
            if (leftRoom != rightRoom) {
                fillRoom(maze, row, col - 1, leftRoom, rightRoom);
                maze.setCell(row, col, rightRoom);
                notifyWallRemoved(listener, row, col);
            }
        }
        
        private void tearDownVerticalWall(Maze maze, int row, int col, MazeGenerationListener listener) {
            int topRoom = maze.getCell(row - 1, col);
            int bottomRoom = maze.getCell(row + 1, col);
            
            if (topRoom != bottomRoom) {
                fillRoom(maze, row - 1, col, topRoom, bottomRoom);
                maze.setCell(row, col, bottomRoom);
                notifyWallRemoved(listener, row, col);
            }
        }
        
        private void notifyWallRemoved(MazeGenerationListener listener, int row, int col) {
            if (listener != null) {
                listener.onCellChanged(row, col, Maze.EMPTY);
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
        
        private void sleepBriefly() {
            sleep(delayMs / 4); // Faster room creation
        }
        
        private void sleep() {
            sleep(delayMs);
        }
        
        private void sleep(int milliseconds) {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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