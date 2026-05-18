package com.mazemaster.solving;

import com.mazemaster.model.Maze;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * A* maze solver using Manhattan distance.
 */
final class AStarSolver implements MazeSolvingStrategy {
    private static final int[] DELTA_ROW = {-1, 0, 1, 0};
    private static final int[] DELTA_COL = {0, 1, 0, -1};

    @Override
    public boolean solve(Maze maze, MazeSolvingContext context) {
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
        Map<Point, Node> allNodes = new HashMap<>();
        Set<Point> closedSet = new HashSet<>();

        Point start = new Point(maze.getStartRow(), maze.getStartCol());
        Point goal = new Point(maze.getGoalRow(), maze.getGoalCol());

        Node startNode = new Node(start, 0, manhattanDistance(start, goal), null);
        openSet.offer(startNode);
        allNodes.put(start, startNode);

        while (!openSet.isEmpty() && !context.isStopped()) {
            Node current = openSet.poll();
            if (current != allNodes.get(current.position)) {
                continue;
            }
            closedSet.add(current.position);

            maze.setCell(current.position.x, current.position.y, Maze.PATH);
            context.notifyCellExplored(current.position.x, current.position.y);

            if (current.position.equals(goal)) {
                return handleSolutionFound(maze, current, context);
            }

            exploreNeighbors(maze, current, goal, openSet, allNodes, closedSet);

            if (!context.pauseAwareSleep(context.delayMs())) {
                return false;
            }
        }

        return false;
    }

    private boolean handleSolutionFound(Maze maze, Node goalNode, MazeSolvingContext context) {
        List<Point> path = reconstructPath(goalNode);
        highlightPath(maze, path, context);
        context.notifyPathFound(path);
        return true;
    }

    private void exploreNeighbors(Maze maze, Node current, Point goal, PriorityQueue<Node> openSet,
                                  Map<Point, Node> allNodes, Set<Point> closedSet) {
        for (int i = 0; i < 4; i++) {
            int newRow = current.position.x + DELTA_ROW[i];
            int newCol = current.position.y + DELTA_COL[i];
            Point neighbor = new Point(newRow, newCol);

            if (shouldSkipNeighbor(maze, neighbor, closedSet)) {
                continue;
            }

            processNeighbor(current, neighbor, goal, openSet, allNodes);
        }
    }

    private boolean shouldSkipNeighbor(Maze maze, Point neighbor, Set<Point> closedSet) {
        return !maze.isValidPosition(neighbor.x, neighbor.y) ||
            !maze.isWalkable(neighbor.x, neighbor.y) ||
            closedSet.contains(neighbor);
    }

    private void processNeighbor(Node current, Point neighbor, Point goal, PriorityQueue<Node> openSet,
                                 Map<Point, Node> allNodes) {
        double tentativeGCost = current.gCost + 1;
        Node neighborNode = allNodes.get(neighbor);

        if (neighborNode == null || tentativeGCost < neighborNode.gCost) {
            double hCost = manhattanDistance(neighbor, goal);
            neighborNode = new Node(neighbor, tentativeGCost, hCost, current);
            allNodes.put(neighbor, neighborNode);
            openSet.offer(neighborNode);
        }
    }

    private double manhattanDistance(Point a, Point b) {
        return (double) Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
    }

    private List<Point> reconstructPath(Node goal) {
        List<Point> path = new ArrayList<>();
        Node current = goal;

        while (current != null) {
            path.add(current.position);
            current = current.parent;
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

    private static class Node {
        final Point position;
        final double gCost;
        final double fCost;
        final Node parent;

        Node(Point position, double gCost, double hCost, Node parent) {
            this.position = position;
            this.gCost = gCost;
            this.fCost = gCost + hCost;
            this.parent = parent;
        }
    }
}
