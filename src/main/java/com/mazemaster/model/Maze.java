// src/main/java/com/mazemaster/model/Maze.java
package com.mazemaster.model;

/**
 * Core data model representing the maze structure and state.
 * Contains the maze grid and provides methods for accessing and modifying maze cells.
 */
public class Maze {
    // Cell type constants
    public static final int BACKGROUND = 0;
    public static final int WALL = 1;
    public static final int PATH = 2;
    public static final int EMPTY = 3;
    public static final int VISITED = 4;
    public static final int START = 5;
    public static final int GOAL = 6;
    
    private int[][] grid;
    private final int rows;
    private final int columns;
    private int startRow;
    private int startCol;
    private int goalRow;
    private int goalCol;
    private long generationSeed;
    private String generationAlgorithm;
    
    public Maze(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.grid = new int[rows][columns];
        resetEndpoints();
        this.generationSeed = 0L;
        this.generationAlgorithm = "";
        initializeWalls();
    }
    
    private void initializeWalls() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                grid[i][j] = WALL;
            }
        }
    }
    
    public void reset() {
        initializeWalls();
        resetEndpoints();
    }
    
    public void resetSolution() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (grid[i][j] == PATH || grid[i][j] == VISITED || grid[i][j] == START || grid[i][j] == GOAL) {
                    grid[i][j] = EMPTY;
                }
            }
        }
    }
    
    // Getters
    public int getCell(int row, int col) {
        if (isValidPosition(row, col)) {
            return grid[row][col];
        }
        return WALL; // Out of bounds treated as wall
    }
    
    public void setCell(int row, int col, int value) {
        if (isValidPosition(row, col)) {
            grid[row][col] = value;
        }
    }
    
    public boolean isValidPosition(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < columns;
    }
    
    public boolean isWalkable(int row, int col) {
        return isValidPosition(row, col) && grid[row][col] != WALL;
    }
    
    public boolean isEmpty(int row, int col) {
        return isValidPosition(row, col) && grid[row][col] == EMPTY;
    }

    public boolean setStartPosition(int row, int col) {
        if (!isValidPosition(row, col) || isGoalCell(row, col)) {
            return false;
        }

        this.startRow = row;
        this.startCol = col;
        return true;
    }

    public boolean setGoalPosition(int row, int col) {
        if (!isValidPosition(row, col) || isStartCell(row, col)) {
            return false;
        }

        this.goalRow = row;
        this.goalCol = col;
        return true;
    }

    public void resetEndpoints() {
        this.startRow = 1;
        this.startCol = 1;
        this.goalRow = rows - 2;
        this.goalCol = columns - 2;
    }

    public boolean isStartCell(int row, int col) {
        return row == startRow && col == startCol;
    }

    public boolean isGoalCell(int row, int col) {
        return row == goalRow && col == goalCol;
    }
    
    public int getRows() { return rows; }
    public int getColumns() { return columns; }
    public int getStartRow() { return startRow; }
    public int getStartCol() { return startCol; }
    public int getGoalRow() { return goalRow; }
    public int getGoalCol() { return goalCol; }
    public long getGenerationSeed() { return generationSeed; }
    public String getGenerationAlgorithm() { return generationAlgorithm; }

    public void setGenerationMetadata(long generationSeed, String generationAlgorithm) {
        this.generationSeed = generationSeed;
        this.generationAlgorithm = generationAlgorithm == null ? "" : generationAlgorithm;
    }
    
    public int[][] getGrid() {
        // Return defensive copy
        int[][] copy = new int[rows][columns];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(grid[i], 0, copy[i], 0, columns);
        }
        return copy;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                switch (grid[i][j]) {
                    case WALL -> sb.append("█");
                    case EMPTY -> sb.append(" ");
                    case PATH -> sb.append("·");
                    case VISITED -> sb.append("x");
                    default -> sb.append("?");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}
