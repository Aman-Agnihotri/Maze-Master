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
     * Kruskal's algorithm for maze generation using Union-Find
     */
    private class KruskalGenerator implements MazeGenerationStrategy {
        private Random random = new Random();
        
        @Override
        public void generate(Maze maze, MazeGenerationListener listener, AtomicBoolean stopFlag) {
            int rows = maze.getRows();
            int cols = maze.getColumns();
            
            // Ensure odd dimensions for proper generation
            if (rows % 2 == 0) rows--;
            if (cols % 2 == 0) cols--;
            
            // Initialize Union-Find structure for cells
            UnionFind unionFind = new UnionFind(rows, cols);
            
            // Create list of all possible walls to remove
            List<Wall> walls = createAllWalls(rows, cols);
            Collections.shuffle(walls, random);
            
            // Create initial rooms
            createInitialRooms(maze, rows, cols, listener, stopFlag);
            
            // Process walls using Kruskal's algorithm
            processWallsKruskal(maze, walls, unionFind, listener, stopFlag);
            
            if (listener != null) {
                listener.onGenerationComplete();
            }
        }
        
        private void createInitialRooms(Maze maze, int rows, int cols, MazeGenerationListener listener, AtomicBoolean stopFlag) {
            for (int i = 1; i < rows - 1; i += 2) {
                for (int j = 1; j < cols - 1; j += 2) {
                    if (stopFlag.get()) return;
                    
                    maze.setCell(i, j, Maze.EMPTY);
                    if (listener != null) {
                        listener.onCellChanged(i, j, Maze.EMPTY);
                        listener.onGenerationStep();
                    }
                    
                    try {
                        Thread.sleep(delayMs / 8); // Fast room creation
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
        
        private List<Wall> createAllWalls(int rows, int cols) {
            List<Wall> walls = new ArrayList<>();
            
            for (int i = 1; i < rows - 1; i += 2) {
                for (int j = 1; j < cols - 1; j += 2) {
                    // Add wall below this room (if exists)
                    if (i < rows - 2) {
                        walls.add(new Wall(i + 1, j, i, j, i + 2, j));
                    }
                    // Add wall to the right of this room (if exists)
                    if (j < cols - 2) {
                        walls.add(new Wall(i, j + 1, i, j, i, j + 2));
                    }
                }
            }
            
            return walls;
        }
        
        private void processWallsKruskal(Maze maze, List<Wall> walls, UnionFind unionFind, 
                                    MazeGenerationListener listener, AtomicBoolean stopFlag) {
            for (Wall wall : walls) {
                if (stopFlag.get()) return;
                
                // Check if the two rooms are already connected
                if (!unionFind.isConnected(wall.room1Row, wall.room1Col, wall.room2Row, wall.room2Col)) {
                    // Connect the rooms by removing the wall
                    unionFind.union(wall.room1Row, wall.room1Col, wall.room2Row, wall.room2Col);
                    maze.setCell(wall.row, wall.col, Maze.EMPTY);
                    
                    if (listener != null) {
                        listener.onCellChanged(wall.row, wall.col, Maze.EMPTY);
                        listener.onGenerationStep();
                    }
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
        
        /**
         * Union-Find data structure for tracking connected components
         */
        private static class UnionFind {
            private final int[] parent;
            private final int[] rank;
            private final int cols;
            
            public UnionFind(int rows, int cols) {
                this.cols = cols;
                int size = rows * cols;
                parent = new int[size];
                rank = new int[size];
                
                for (int i = 0; i < size; i++) {
                    parent[i] = i;
                    rank[i] = 0;
                }
            }
            
            private int getIndex(int row, int col) {
                return row * cols + col;
            }
            
            public int find(int row, int col) {
                int index = getIndex(row, col);
                if (parent[index] != index) {
                    parent[index] = find(parent[index] / cols, parent[index] % cols); // Path compression
                }
                return parent[index];
            }
            
            public void union(int row1, int col1, int row2, int col2) {
                int root1 = find(row1, col1);
                int root2 = find(row2, col2);
                
                if (root1 != root2) {
                    // Union by rank
                    if (rank[root1] < rank[root2]) {
                        parent[root1] = root2;
                    } else if (rank[root1] > rank[root2]) {
                        parent[root2] = root1;
                    } else {
                        parent[root2] = root1;
                        rank[root1]++;
                    }
                }
            }
            
            public boolean isConnected(int row1, int col1, int row2, int col2) {
                return find(row1, col1) == find(row2, col2);
            }
        }
    }
    
    /**
     * Prim's algorithm for maze generation
     */
    private class PrimGenerator implements MazeGenerationStrategy {
        private Random random = new Random();
        
        @Override
        public void generate(Maze maze, MazeGenerationListener listener, AtomicBoolean stopFlag) {
            int rows = maze.getRows();
            int cols = maze.getColumns();
            
            // Ensure odd dimensions for proper generation
            if (rows % 2 == 0) rows--;
            if (cols % 2 == 0) cols--;
            
            // Track which cells are part of the maze
            boolean[][] inMaze = new boolean[rows][cols];
            
            // List of walls that could potentially be removed
            List<Wall> frontierWalls = new ArrayList<>();
            
            // Start with a random cell
            int startRow = 1 + (random.nextInt((rows - 2) / 2)) * 2;
            int startCol = 1 + (random.nextInt((cols - 2) / 2)) * 2;
            
            addCellToMaze(maze, inMaze, startRow, startCol, frontierWalls, listener, stopFlag);
            
            // Process frontier walls until maze is complete
            while (!frontierWalls.isEmpty() && !stopFlag.get()) {
                // Pick a random wall from the frontier
                int wallIndex = random.nextInt(frontierWalls.size());
                Wall wall = frontierWalls.get(wallIndex);
                frontierWalls.remove(wallIndex);
                
                // Check if we can remove this wall
                if (canRemoveWall(inMaze, wall)) {
                    removeWall(maze, inMaze, wall, frontierWalls, listener, stopFlag);
                }
            }
            
            if (listener != null) {
                listener.onGenerationComplete();
            }
        }
        
        private void addCellToMaze(Maze maze, boolean[][] inMaze, int row, int col, 
                                List<Wall> frontierWalls, MazeGenerationListener listener, AtomicBoolean stopFlag) {
            if (stopFlag.get()) return;
            
            // Mark cell as part of maze
            maze.setCell(row, col, Maze.EMPTY);
            inMaze[row][col] = true;
            
            if (listener != null) {
                listener.onCellChanged(row, col, Maze.EMPTY);
                listener.onGenerationStep();
            }
            
            // Add walls around this cell to frontier
            addSurroundingWalls(row, col, frontierWalls, inMaze, maze.getRows(), maze.getColumns());
            
            try {
                Thread.sleep(delayMs / 4);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        private void addSurroundingWalls(int row, int col, List<Wall> frontierWalls, boolean[][] inMaze, int rows, int cols) {
            // Check all four directions
            int[][] directions = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
            int[][] wallPositions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            
            for (int i = 0; i < 4; i++) {
                int newRow = row + directions[i][0];
                int newCol = col + directions[i][1];
                int wallRow = row + wallPositions[i][0];
                int wallCol = col + wallPositions[i][1];
                
                // Check if the adjacent cell is valid and not in maze
                if (isValidCell(newRow, newCol, rows, cols) && !inMaze[newRow][newCol]) {
                    Wall wall = new Wall(wallRow, wallCol, row, col, newRow, newCol);
                    if (!frontierWalls.contains(wall)) {
                        frontierWalls.add(wall);
                    }
                }
            }
        }
        
        private boolean isValidCell(int row, int col, int rows, int cols) {
            return row > 0 && row < rows - 1 && col > 0 && col < cols - 1 && 
                row % 2 == 1 && col % 2 == 1;
        }
        
        private boolean canRemoveWall(boolean[][] inMaze, Wall wall) {
            // Can remove wall if exactly one of the two rooms is already in the maze
            boolean room1InMaze = inMaze[wall.room1Row][wall.room1Col];
            boolean room2InMaze = inMaze[wall.room2Row][wall.room2Col];
            
            return room1InMaze != room2InMaze; // XOR - exactly one should be true
        }
        
        private void removeWall(Maze maze, boolean[][] inMaze, Wall wall, List<Wall> frontierWalls, MazeGenerationListener listener, AtomicBoolean stopFlag) {
            if (stopFlag.get()) return;
            
            // Remove the wall
            maze.setCell(wall.row, wall.col, Maze.EMPTY);
            
            // Add the new cell to the maze
            int newCellRow = inMaze[wall.room1Row][wall.room1Col] ? wall.room2Row : wall.room1Row;
            int newCellCol = inMaze[wall.room1Row][wall.room1Col] ? wall.room2Col : wall.room1Col;
            
            addCellToMaze(maze, inMaze, newCellRow, newCellCol, frontierWalls, listener, stopFlag);
            
            if (listener != null) {
                listener.onCellChanged(wall.row, wall.col, Maze.EMPTY);
                listener.onGenerationStep();
            }
            
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Helper class to represent walls between rooms
     */
    private static class Wall {
        final int row, col; // Wall position
        final int room1Row, room1Col; // First room
        final int room2Row, room2Col; // Second room
        
        // Constructor for DFS (existing)
        Wall(int row, int col) {
            this.row = row;
            this.col = col;
            this.room1Row = this.room1Col = this.room2Row = this.room2Col = -1;
        }
        
        // Constructor for Kruskal's and Prim's algorithms
        Wall(int row, int col, int room1Row, int room1Col, int room2Row, int room2Col) {
            this.row = row;
            this.col = col;
            this.room1Row = room1Row;
            this.room1Col = room1Col;
            this.room2Row = room2Row;
            this.room2Col = room2Col;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            Wall wall = (Wall) obj;
            return row == wall.row && col == wall.col;
        }
        
        @Override
        public int hashCode() {
            return row * 31 + col;
        }
    }
}