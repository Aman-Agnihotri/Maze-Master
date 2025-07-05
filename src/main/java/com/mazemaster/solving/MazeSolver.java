// src/main/java/com/mazemaster/solving/MazeSolver.java
package com.mazemaster.solving;

import com.mazemaster.model.Maze;
import java.awt.Point;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main maze solver that orchestrates different solving algorithms.
 */
public class MazeSolver {
    private final Map<String, MazeSolvingStrategy> strategies;
    private MazeSolvingListener listener;
    private int delayMs = 30;
    
    public MazeSolver() {
        strategies = new HashMap<>();
        strategies.put("Depth First Search", new DepthFirstSearchSolver());
        strategies.put("Breadth First Search", new BreadthFirstSearchSolver());
        strategies.put("A*", new AStarSolver());
    }
    
    public void setSolvingListener(MazeSolvingListener listener) {
        this.listener = listener;
    }
    
    public void setDelay(int delayMs) {
        this.delayMs = Math.max(1, delayMs);
    }
    
    public Set<String> getAvailableAlgorithms() {
        return strategies.keySet();
    }
    
    public boolean solve(Maze maze, String algorithm, AtomicBoolean stopFlag, AtomicBoolean pauseFlag) {
        MazeSolvingStrategy strategy = strategies.get(algorithm);
        if (strategy == null) {
            strategy = strategies.get("Depth First Search"); // Default fallback
        }
        
        maze.resetSolution();
        return strategy.solve(maze, listener, stopFlag, pauseFlag);
    }
    
    /**
     * Utility method to handle pause checking during animation delays
     */
    private void waitForResume(AtomicBoolean pauseFlag) {
        while (pauseFlag.get()) {
            try {
                Thread.sleep(100); // Check every 100ms
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
    
    /**
     * Enhanced sleep method that respects pause state
     */
    private boolean pauseAwareSleep(int milliseconds, AtomicBoolean stopFlag, AtomicBoolean pauseFlag) {
        try {
            // First, wait for any pause to be lifted
            waitForResume(pauseFlag);
            
            // If stopped during pause, return immediately
            if (stopFlag.get()) {
                return false;
            }
            
            // Normal sleep with interrupt handling
            Thread.sleep(milliseconds);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * Depth-First Search solver (recursive backtracking)
     */
    private class DepthFirstSearchSolver implements MazeSolvingStrategy {
        private final int[] deltaRow = {-1, 0, 1, 0};
        private final int[] deltaCol = {0, 1, 0, -1};
        
        @Override
        public boolean solve(Maze maze, MazeSolvingListener listener, AtomicBoolean stopFlag, AtomicBoolean pauseFlag) {
            boolean result = solveDFS(maze, maze.getStartRow(), maze.getStartCol(), listener, stopFlag, pauseFlag);
            
            if (listener != null && !stopFlag.get()) {
                listener.onSolvingComplete(result);
            }
            
            return result;
        }
        
        private boolean solveDFS(Maze maze, int row, int col, MazeSolvingListener listener, AtomicBoolean stopFlag, AtomicBoolean pauseFlag) {
            if (stopFlag.get() || !maze.isEmpty(row, col)) {
                return false;
            }
            
            // Mark current cell as part of path
            maze.setCell(row, col, Maze.PATH);
            notifyExploration(listener, row, col);
            
            // Check if goal is reached
            if (isGoalReached(maze, row, col)) {
                return true;
            }
            
            // Wait for animation with pause support
            if (!pauseAwareSleep(delayMs, stopFlag, pauseFlag)) {
                return false;
            }
            
            // Try all four directions
            if (exploreDirections(maze, row, col, listener, stopFlag, pauseFlag)) {
                return true;
            }
            
            // Backtrack - mark as visited and notify
            backtrack(maze, row, col, listener, stopFlag, pauseFlag);
            return false;
        }
        
        private void notifyExploration(MazeSolvingListener listener, int row, int col) {
            if (listener != null) {
                listener.onCellExplored(row, col);
            }
        }
        
        private boolean isGoalReached(Maze maze, int row, int col) {
            return row == maze.getGoalRow() && col == maze.getGoalCol();
        }
        
        private boolean exploreDirections(Maze maze, int row, int col, MazeSolvingListener listener, AtomicBoolean stopFlag, AtomicBoolean pauseFlag) {
            for (int i = 0; i < 4; i++) {
                int newRow = row + deltaRow[i];
                int newCol = col + deltaCol[i];
                
                if (solveDFS(maze, newRow, newCol, listener, stopFlag, pauseFlag)) {
                    return true;
                }
            }
            return false;
        }
        
        private void backtrack(Maze maze, int row, int col, MazeSolvingListener listener, AtomicBoolean stopFlag, AtomicBoolean pauseFlag) {
            maze.setCell(row, col, Maze.VISITED);
            
            if (listener != null) {
                listener.onCellBacktracked(row, col);
            }
            
            pauseAwareSleep(delayMs, stopFlag, pauseFlag);
        }
    }
    
    /**
     * Breadth-First Search solver (guarantees shortest path)
     */
    private class BreadthFirstSearchSolver implements MazeSolvingStrategy {
        private final int[] deltaRow = {-1, 0, 1, 0};
        private final int[] deltaCol = {0, 1, 0, -1};

        @Override
        public boolean solve(Maze maze, MazeSolvingListener listener, AtomicBoolean stopFlag, AtomicBoolean pauseFlag) {
            Queue<Point> queue = new LinkedList<>();
            Map<Point, Point> parent = new HashMap<>();
            
            Point start = new Point(maze.getStartRow(), maze.getStartCol());
            Point goal = new Point(maze.getGoalRow(), maze.getGoalCol());
            
            initializeBFS(maze, queue, parent, start);
            
            while (!queue.isEmpty() && !stopFlag.get()) {
                Point current = queue.poll();
                
                if (current.equals(goal)) {
                    return handleSolutionFound(maze, parent, goal, listener, stopFlag, pauseFlag);
                }
                
                exploreNeighbors(maze, current, queue, parent, listener, stopFlag, pauseFlag);
            }
            
            if (listener != null && !stopFlag.get()) {
                listener.onSolvingComplete(false);
            }
            return false;
        }

        private void initializeBFS(Maze maze, Queue<Point> queue, Map<Point, Point> parent, Point start) {
            queue.offer(start);
            maze.setCell(start.x, start.y, Maze.PATH);
            parent.put(start, null);
        }
        
        private boolean handleSolutionFound(Maze maze, Map<Point, Point> parent, Point goal, MazeSolvingListener listener, AtomicBoolean stopFlag, AtomicBoolean pauseFlag) {
            List<Point> path = reconstructPath(parent, goal);
            highlightPath(maze, path, listener, stopFlag, pauseFlag);
            
            if (listener != null && !stopFlag.get()) {
                listener.onPathFound(path);
                listener.onSolvingComplete(true);
            }
            return true;
        }
        
        private void exploreNeighbors(Maze maze, Point current, Queue<Point> queue, 
                                    Map<Point, Point> parent, MazeSolvingListener listener, AtomicBoolean stopFlag, AtomicBoolean pauseFlag) {
            for (int i = 0; i < 4; i++) {
                if (stopFlag.get()) {
                    return;
                }
                
                int newRow = current.x + deltaRow[i];
                int newCol = current.y + deltaCol[i];
                Point neighbor = new Point(newRow, newCol);
                
                if (maze.isEmpty(newRow, newCol)) {
                    processValidNeighbor(maze, neighbor, current, queue, parent, listener);
                    
                    if (!pauseAwareSleep(delayMs, stopFlag, pauseFlag)) {
                        return;
                    }
                }
            }
        }
        
        private void processValidNeighbor(Maze maze, Point neighbor, Point current, 
                                        Queue<Point> queue, Map<Point, Point> parent, MazeSolvingListener listener) {
            maze.setCell(neighbor.x, neighbor.y, Maze.PATH);
            queue.offer(neighbor);
            parent.put(neighbor, current);
            
            if (listener != null) {
                listener.onCellExplored(neighbor.x, neighbor.y);
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
        
        private void highlightPath(Maze maze, List<Point> path, MazeSolvingListener listener, AtomicBoolean stopFlag, AtomicBoolean pauseFlag) {
            for (Point point : path) {
                if (stopFlag.get()) {
                    return;
                }
                
                maze.setCell(point.x, point.y, Maze.START); // Use START color for final path
                
                if (listener != null) {
                    listener.onCellExplored(point.x, point.y);
                }
                
                if (!pauseAwareSleep(delayMs, stopFlag, pauseFlag)) {
                    return;
                }
            }
        }
    }
    
    /**
     * A* Search solver (heuristic-based, optimal)
     */
    private class AStarSolver implements MazeSolvingStrategy {
        private final int[] deltaRow = {-1, 0, 1, 0};
        private final int[] deltaCol = {0, 1, 0, -1};

        @Override
        public boolean solve(Maze maze, MazeSolvingListener listener, AtomicBoolean stopFlag, AtomicBoolean pauseFlag) {
            PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
            Map<Point, Node> allNodes = new HashMap<>();
            Set<Point> closedSet = new HashSet<>();
            
            Point start = new Point(maze.getStartRow(), maze.getStartCol());
            Point goal = new Point(maze.getGoalRow(), maze.getGoalCol());
            
            initializeSearch(openSet, allNodes, start, goal);
            
            while (!openSet.isEmpty() && !stopFlag.get()) {
                Node current = openSet.poll();
                closedSet.add(current.position);
                
                updateMazeAndNotify(maze, current, listener);
                
                if (current.position.equals(goal)) {
                    return handleSolutionFound(maze, current, listener, stopFlag, pauseFlag);
                }
                
                exploreNeighbors(maze, current, goal, openSet, allNodes, closedSet);
                
                if (!pauseAwareSleep(delayMs, stopFlag, pauseFlag)) {
                    return false;
                }
            }
            
            if (listener != null && !stopFlag.get()) {
                listener.onSolvingComplete(false);
            }
            return false;
        }

        private void initializeSearch(PriorityQueue<Node> openSet, Map<Point, Node> allNodes, Point start, Point goal) {
            Node startNode = new Node(start, 0, manhattanDistance(start, goal), null);
            openSet.offer(startNode);
            allNodes.put(start, startNode);
        }
        
        private void updateMazeAndNotify(Maze maze, Node current, MazeSolvingListener listener) {
            maze.setCell(current.position.x, current.position.y, Maze.PATH);
            if (listener != null) {
                listener.onCellExplored(current.position.x, current.position.y);
            }
        }
        
        private boolean handleSolutionFound(Maze maze, Node goalNode, MazeSolvingListener listener, AtomicBoolean stopFlag, AtomicBoolean pauseFlag) {
            List<Point> path = reconstructPath(goalNode);
            highlightPath(maze, path, listener, stopFlag, pauseFlag);
            
            if (listener != null && !stopFlag.get()) {
                listener.onPathFound(path);
                listener.onSolvingComplete(true);
            }
            return true;
        }
        
        private void exploreNeighbors(Maze maze, Node current, Point goal, PriorityQueue<Node> openSet, 
                                    Map<Point, Node> allNodes, Set<Point> closedSet) {
            for (int i = 0; i < 4; i++) {
                int newRow = current.position.x + deltaRow[i];
                int newCol = current.position.y + deltaCol[i];
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
        
        private void processNeighbor(Node current, Point neighbor, Point goal, PriorityQueue<Node> openSet, Map<Point, Node> allNodes) {
            double tentativeGCost = current.gCost + 1;
            Node neighborNode = allNodes.get(neighbor);
            
            if (neighborNode == null || tentativeGCost < neighborNode.gCost) {
                double hCost = manhattanDistance(neighbor, goal);
                neighborNode = new Node(neighbor, tentativeGCost, hCost, current);
                allNodes.put(neighbor, neighborNode);
                
                if (!openSet.contains(neighborNode)) {
                    openSet.offer(neighborNode);
                }
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
        
        private void highlightPath(Maze maze, List<Point> path, MazeSolvingListener listener, AtomicBoolean stopFlag, AtomicBoolean pauseFlag) {
            for (Point point : path) {
                if (stopFlag.get()) {
                    return;
                }
                
                maze.setCell(point.x, point.y, Maze.START); // Use START color for final path
                
                if (listener != null) {
                    listener.onCellExplored(point.x, point.y);
                }
                
                if (!pauseAwareSleep(delayMs, stopFlag, pauseFlag)) {
                    return;
                }
            }
        }
        
        private static class Node {
            final Point position;
            final double gCost;
            @SuppressWarnings("unused")
            final double hCost;
            final double fCost;
            final Node parent;
            
            Node(Point position, double gCost, double hCost, Node parent) {
                this.position = position;
                this.gCost = gCost;
                this.hCost = hCost;
                this.fCost = gCost + hCost;
                this.parent = parent;
            }
        }
    }
}