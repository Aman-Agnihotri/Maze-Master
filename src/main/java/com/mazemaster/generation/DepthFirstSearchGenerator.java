package com.mazemaster.generation;

import com.mazemaster.model.Maze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Depth-first maze generation using randomized wall removal.
 */
final class DepthFirstSearchGenerator implements MazeGenerationStrategy {

    @Override
    public void generate(Maze maze, MazeGenerationContext context) {
        int rows = ensureOddDimension(maze.getRows());
        int cols = ensureOddDimension(maze.getColumns());

        List<Wall> walls = createInitialRoomsAndWalls(maze, rows, cols, context);
        if (context.isStopped()) return;

        processWalls(maze, walls, context);
        if (context.isStopped()) return;

        convertRoomNumbersToEmptyCells(maze, rows, cols, context);
    }

    private int ensureOddDimension(int dimension) {
        return dimension % 2 == 0 ? dimension - 1 : dimension;
    }

    private List<Wall> createInitialRoomsAndWalls(Maze maze, int rows, int cols, MazeGenerationContext context) {
        List<Wall> walls = new ArrayList<>();
        int roomCount = 0;

        for (int i = 1; i < rows - 1; i += 2) {
            for (int j = 1; j < cols - 1; j += 2) {
                if (context.isStopped()) return walls;

                roomCount++;
                maze.setCell(i, j, -roomCount);

                context.notifyCellChanged(i, j, Maze.EMPTY);
                context.notifyGenerationStep();
                addWallsAroundRoom(walls, i, j, rows, cols);

                if (!context.pauseAwareSleep(context.scaledDelay(4))) {
                    return walls;
                }
            }
        }

        return walls;
    }

    private void addWallsAroundRoom(List<Wall> walls, int row, int col, int rows, int cols) {
        if (row < rows - 2) {
            walls.add(new Wall(row + 1, col));
        }
        if (col < cols - 2) {
            walls.add(new Wall(row, col + 1));
        }
    }

    private void processWalls(Maze maze, List<Wall> walls, MazeGenerationContext context) {
        Collections.shuffle(walls, context.getRandom());

        for (Wall wall : walls) {
            if (context.isStopped()) return;

            tearDownWall(maze, wall.row, wall.col, context);

            if (!context.pauseAwareSleep(context.delayMs())) {
                return;
            }
        }
    }

    private void convertRoomNumbersToEmptyCells(Maze maze, int rows, int cols, MazeGenerationContext context) {
        for (int i = 1; i < rows - 1; i++) {
            for (int j = 1; j < cols - 1; j++) {
                if (maze.getCell(i, j) < 0) {
                    maze.setCell(i, j, Maze.EMPTY);
                    context.notifyCellChanged(i, j, Maze.EMPTY);
                }
            }
        }
    }

    private void tearDownWall(Maze maze, int row, int col, MazeGenerationContext context) {
        if (isHorizontalWall(row)) {
            tearDownHorizontalWall(maze, row, col, context);
        } else {
            tearDownVerticalWall(maze, row, col, context);
        }
    }

    private boolean isHorizontalWall(int row) {
        return row % 2 == 1;
    }

    private void tearDownHorizontalWall(Maze maze, int row, int col, MazeGenerationContext context) {
        int leftRoom = maze.getCell(row, col - 1);
        int rightRoom = maze.getCell(row, col + 1);

        if (leftRoom != rightRoom) {
            fillRoom(maze, row, col - 1, leftRoom, rightRoom);
            maze.setCell(row, col, rightRoom);
            context.notifyCellChanged(row, col, Maze.EMPTY);
        }
    }

    private void tearDownVerticalWall(Maze maze, int row, int col, MazeGenerationContext context) {
        int topRoom = maze.getCell(row - 1, col);
        int bottomRoom = maze.getCell(row + 1, col);

        if (topRoom != bottomRoom) {
            fillRoom(maze, row - 1, col, topRoom, bottomRoom);
            maze.setCell(row, col, bottomRoom);
            context.notifyCellChanged(row, col, Maze.EMPTY);
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
