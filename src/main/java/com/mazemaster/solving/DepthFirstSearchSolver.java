package com.mazemaster.solving;

import com.mazemaster.model.Maze;

/**
 * Depth-first maze solver using recursive backtracking.
 */
final class DepthFirstSearchSolver implements MazeSolvingStrategy {
    private static final int[] DELTA_ROW = {-1, 0, 1, 0};
    private static final int[] DELTA_COL = {0, 1, 0, -1};

    @Override
    public boolean solve(Maze maze, MazeSolvingContext context) {
        return solveDFS(maze, maze.getStartRow(), maze.getStartCol(), context);
    }

    private boolean solveDFS(Maze maze, int row, int col, MazeSolvingContext context) {
        if (context.isStopped() || !maze.isEmpty(row, col)) {
            return false;
        }

        maze.setCell(row, col, Maze.PATH);
        context.notifyCellExplored(row, col);

        if (row == maze.getGoalRow() && col == maze.getGoalCol()) {
            return true;
        }

        if (!context.pauseAwareSleep(context.delayMs())) {
            return false;
        }

        for (int i = 0; i < 4; i++) {
            int newRow = row + DELTA_ROW[i];
            int newCol = col + DELTA_COL[i];

            if (solveDFS(maze, newRow, newCol, context)) {
                return true;
            }
        }

        maze.setCell(row, col, Maze.VISITED);
        context.notifyCellBacktracked(row, col);
        context.pauseAwareSleep(context.delayMs());
        return false;
    }
}
