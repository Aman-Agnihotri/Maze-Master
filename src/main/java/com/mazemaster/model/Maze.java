// src/main/java/com/mazemaster/model/Maze.java
package com.mazemaster.model;

import java.io.Serializable;

/**
 * Core data model representing the maze structure and state.
 * Contains the maze grid and provides methods for accessing and modifying maze cells.
 */
public class Maze implements Serializable {
    private static final long serialVersionUID = 1L;
    
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
    private final int startRow;
    private final int startCol;
    private final int goalRow;
    private final int goalCol;
    
    public Maze(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        this.grid = new int[rows][columns];
        this.startRow = 1;
        this.startCol = 1;
        this.goalRow = rows - 2;
        this.goalCol = columns - 2;
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
    }
    
    public void resetSolution() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (grid[i][j] == PATH || grid[i][j] == VISITED) {
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
    
    public int getRows() { return rows; }
    public int getColumns() { return columns; }
    public int getStartRow() { return startRow; }
    public int getStartCol() { return startCol; }
    public int getGoalRow() { return goalRow; }
    public int getGoalCol() { return goalCol; }
    
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