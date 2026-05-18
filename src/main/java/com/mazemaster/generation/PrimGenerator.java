package com.mazemaster.generation;

import com.mazemaster.model.Maze;

import java.util.ArrayList;
import java.util.List;

/**
 * Prim maze generation using randomized frontier walls.
 */
final class PrimGenerator implements MazeGenerationStrategy {

    @Override
    public void generate(Maze maze, MazeGenerationContext context) {
        int rows = ensureOddDimension(maze.getRows());
        int cols = ensureOddDimension(maze.getColumns());
        boolean[][] inMaze = new boolean[rows][cols];
        List<Wall> frontierWalls = new ArrayList<>();

        int startRow = 1 + (context.getRandom().nextInt((rows - 2) / 2)) * 2;
        int startCol = 1 + (context.getRandom().nextInt((cols - 2) / 2)) * 2;

        addCellToMaze(maze, inMaze, startRow, startCol, frontierWalls, context, rows, cols);

        while (!frontierWalls.isEmpty() && !context.isStopped()) {
            int wallIndex = context.getRandom().nextInt(frontierWalls.size());
            Wall wall = frontierWalls.remove(wallIndex);

            if (canRemoveWall(inMaze, wall)) {
                removeWall(maze, inMaze, wall, frontierWalls, context, rows, cols);
            }
        }
    }

    private int ensureOddDimension(int dimension) {
        return dimension % 2 == 0 ? dimension - 1 : dimension;
    }

    private void addCellToMaze(Maze maze, boolean[][] inMaze, int row, int col,
                               List<Wall> frontierWalls, MazeGenerationContext context,
                               int rows, int cols) {
        if (context.isStopped()) return;

        maze.setCell(row, col, Maze.EMPTY);
        inMaze[row][col] = true;
        context.notifyCellChanged(row, col, Maze.EMPTY);
        context.notifyGenerationStep();

        addSurroundingWalls(row, col, frontierWalls, inMaze, rows, cols);
        context.pauseAwareSleep(context.scaledDelay(4));
    }

    private void addSurroundingWalls(int row, int col, List<Wall> frontierWalls, boolean[][] inMaze,
                                     int rows, int cols) {
        int[][] directions = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        int[][] wallPositions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

        for (int i = 0; i < 4; i++) {
            int newRow = row + directions[i][0];
            int newCol = col + directions[i][1];
            int wallRow = row + wallPositions[i][0];
            int wallCol = col + wallPositions[i][1];

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
        boolean room1InMaze = inMaze[wall.room1Row][wall.room1Col];
        boolean room2InMaze = inMaze[wall.room2Row][wall.room2Col];

        return room1InMaze != room2InMaze;
    }

    private void removeWall(Maze maze, boolean[][] inMaze, Wall wall, List<Wall> frontierWalls,
                            MazeGenerationContext context, int rows, int cols) {
        if (context.isStopped()) return;

        maze.setCell(wall.row, wall.col, Maze.EMPTY);

        int newCellRow = inMaze[wall.room1Row][wall.room1Col] ? wall.room2Row : wall.room1Row;
        int newCellCol = inMaze[wall.room1Row][wall.room1Col] ? wall.room2Col : wall.room1Col;

        addCellToMaze(maze, inMaze, newCellRow, newCellCol, frontierWalls, context, rows, cols);
        context.notifyCellChanged(wall.row, wall.col, Maze.EMPTY);
        context.notifyGenerationStep();
        context.pauseAwareSleep(context.delayMs());
    }
}
