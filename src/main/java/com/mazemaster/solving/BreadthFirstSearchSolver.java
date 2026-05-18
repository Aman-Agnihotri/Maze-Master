package com.mazemaster.solving;

import com.mazemaster.model.Maze;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Breadth-first maze solver, which finds shortest paths in unweighted mazes.
 */
final class BreadthFirstSearchSolver implements MazeSolvingStrategy {
    private static final int[] DELTA_ROW = {-1, 0, 1, 0};
    private static final int[] DELTA_COL = {0, 1, 0, -1};

    @Override
    public boolean solve(Maze maze, MazeSolvingContext context) {
        Queue<Point> queue = new ArrayDeque<>();
        Map<Point, Point> parent = new HashMap<>();

        Point start = new Point(maze.getStartRow(), maze.getStartCol());
        Point goal = new Point(maze.getGoalRow(), maze.getGoalCol());

        queue.offer(start);
        maze.setCell(start.x, start.y, Maze.PATH);
        parent.put(start, null);

        while (!queue.isEmpty() && !context.isStopped()) {
            Point current = queue.poll();

            if (current.equals(goal)) {
                return handleSolutionFound(maze, parent, goal, context);
            }

            exploreNeighbors(maze, current, queue, parent, context);
        }

        return false;
    }

    private boolean handleSolutionFound(Maze maze, Map<Point, Point> parent, Point goal, MazeSolvingContext context) {
        List<Point> path = reconstructPath(parent, goal);
        highlightPath(maze, path, context);
        context.notifyPathFound(path);
        return true;
    }

    private void exploreNeighbors(Maze maze, Point current, Queue<Point> queue,
                                  Map<Point, Point> parent, MazeSolvingContext context) {
        for (int i = 0; i < 4; i++) {
            if (context.isStopped()) {
                return;
            }

            int newRow = current.x + DELTA_ROW[i];
            int newCol = current.y + DELTA_COL[i];

            if (maze.isEmpty(newRow, newCol)) {
                Point neighbor = new Point(newRow, newCol);
                maze.setCell(neighbor.x, neighbor.y, Maze.PATH);
                queue.offer(neighbor);
                parent.put(neighbor, current);
                context.notifyCellExplored(neighbor.x, neighbor.y);

                if (!context.pauseAwareSleep(context.delayMs())) {
                    return;
                }
            }
        }
    }

    private List<Point> reconstructPath(Map<Point, Point> parent, Point goal) {
        List<Point> path = new ArrayList<>();
        Point current = goal;

        while (current != null) {
            path.add(current);
            current = parent.get(current);
        }

        Collections.reverse(path);
        return path;
    }

    private void highlightPath(Maze maze, List<Point> path, MazeSolvingContext context) {
        for (Point point : path) {
            if (context.isStopped()) {
                return;
            }

            maze.setCell(point.x, point.y, Maze.START);
            context.notifyCellExplored(point.x, point.y);

            if (!context.pauseAwareSleep(context.delayMs())) {
                return;
            }
        }
    }
}
