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
        strategies.put("Dijkstra", new DijkstraSolver());
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
    
    public boolean solve(Maze maze, String algorithm, AtomicBoolean stopFlag) {
        MazeSolvingStrategy strategy = strategies.get(algorithm);
        if (strategy == null) {
            strategy = strategies.get("Depth First Search"); // Default fallback
        }
        
        maze.resetSolution();
        return strategy.solve(maze, listener, stopFlag);
    }
    
    /**
     * Depth-First Search solver (recursive backtracking)
     */
    private class DepthFirstSearchSolver implements MazeSolvingStrategy {
        private final int[] deltaRow = {-1, 0, 1, 0};
        private final int[] deltaCol = {0, 1, 0, -1};
        
        @Override
        public boolean solve(Maze maze, MazeSolvingListener listener, AtomicBoolean stopFlag) {
            boolean result = solveDFS(maze, maze.getStartRow(), maze.getStartCol(), listener, stopFlag);
            
            if (listener != null) {
                listener.onSolvingComplete(result);
            }
            
            return result;
        }
        
        private boolean solveDFS(Maze maze, int row, int col, MazeSolvingListener listener, AtomicBoolean stopFlag) {
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
            
            // Wait for animation
            if (!sleep()) {
                return false;
            }
            
            // Try all four directions
            if (exploreDirections(maze, row, col, listener, stopFlag)) {
                return true;
            }
            
            // Backtrack - mark as visited and notify
            backtrack(maze, row, col, listener);
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
        
        private boolean sleep() {
            try {
                Thread.sleep(delayMs);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        
        private boolean exploreDirections(Maze maze, int row, int col, MazeSolvingListener listener, AtomicBoolean stopFlag) {
            for (int i = 0; i < 4; i++) {
                int newRow = row + deltaRow[i];
                int newCol = col + deltaCol[i];
                
                if (solveDFS(maze, newRow, newCol, listener, stopFlag)) {
                    return true;
                }
            }
            return false;
        }
        
        private void backtrack(Maze maze, int row, int col, MazeSolvingListener listener) {
            maze.setCell(row, col, Maze.VISITED);
            
            if (listener != null) {
                listener.onCellBacktracked(row, col);
            }
            
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Breadth-First Search solver (guarantees shortest path)
     */
    private class BreadthFirstSearchSolver implements MazeSolvingStrategy {
        private final int[] deltaRow = {-1, 0, 1, 0};
        private final int[] deltaCol = {0, 1, 0, -1};

        @Override
        public boolean solve(Maze maze, MazeSolvingListener listener, AtomicBoolean stopFlag) {
            Queue<Point> queue = new LinkedList<>();
            Map<Point, Point> parent = new HashMap<>();
            
            Point start = new Point(maze.getStartRow(), maze.getStartCol());
            Point goal = new Point(maze.getGoalRow(), maze.getGoalCol());
            
            initializeBFS(maze, queue, parent, start);
            
            while (!queue.isEmpty() && !stopFlag.get()) {
                Point current = queue.poll();
                
                if (current.equals(goal)) {
                    return handleSolutionFound(maze, parent, goal, listener);
                }
                
                exploreNeighbors(maze, current, queue, parent, listener, stopFlag);
            }
            
            if (listener != null) {
                listener.onSolvingComplete(false);
            }
            return false;
        }

        private void initializeBFS(Maze maze, Queue<Point> queue, Map<Point, Point> parent, Point start) {
            queue.offer(start);
            maze.setCell(start.x, start.y, Maze.PATH);
            parent.put(start, null);
        }
        
        private boolean handleSolutionFound(Maze maze, Map<Point, Point> parent, Point goal, MazeSolvingListener listener) {
            List<Point> path = reconstructPath(parent, goal);
            highlightPath(maze, path, listener);
            
            if (listener != null) {
                listener.onPathFound(path);
                listener.onSolvingComplete(true);
            }
            return true;
        }
        
        private void exploreNeighbors(Maze maze, Point current, Queue<Point> queue, 
                                    Map<Point, Point> parent, MazeSolvingListener listener, AtomicBoolean stopFlag) {
            for (int i = 0; i < 4; i++) {
                if (stopFlag.get()) {
                    return;
                }
                
                int newRow = current.x + deltaRow[i];
                int newCol = current.y + deltaCol[i];
                Point neighbor = new Point(newRow, newCol);
                
                if (maze.isEmpty(newRow, newCol)) {
                    processValidNeighbor(maze, neighbor, current, queue, parent, listener);
                    
                    if (!sleep()) {
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
        
        private boolean sleep() {
            try {
                Thread.sleep(delayMs);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
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
        
        private void highlightPath(Maze maze, List<Point> path, MazeSolvingListener listener) {
            for (Point point : path) {
                maze.setCell(point.x, point.y, Maze.START); // Use START color for final path
                
                if (listener != null) {
                    listener.onCellExplored(point.x, point.y);
                }
                
                if (!sleep()) {
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
        public boolean solve(Maze maze, MazeSolvingListener listener, AtomicBoolean stopFlag) {
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
                    return handleSolutionFound(maze, current, listener);
                }
                
                exploreNeighbors(maze, current, goal, openSet, allNodes, closedSet);
                
                if (!sleep()) {
                    return false;
                }
            }
            
            if (listener != null) {
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
        
        private boolean handleSolutionFound(Maze maze, Node goalNode, MazeSolvingListener listener) {
            List<Point> path = reconstructPath(goalNode);
            highlightPath(maze, path, listener);
            
            if (listener != null) {
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
        
        private boolean sleep() {
            try {
                Thread.sleep(delayMs);
                return true;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
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
        
        private void highlightPath(Maze maze, List<Point> path, MazeSolvingListener listener) {
            for (Point point : path) {
                maze.setCell(point.x, point.y, Maze.START); // Use START color for final path
                
                if (listener != null) {
                    listener.onCellExplored(point.x, point.y);
                }
                
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
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
    
    /**
     * Dijkstra's algorithm solver (guaranteed shortest path without heuristic)
     */
    private class DijkstraSolver implements MazeSolvingStrategy {
        @Override
        public boolean solve(Maze maze, MazeSolvingListener listener, AtomicBoolean stopFlag) {
            // TODO: Implement Dijkstra's algorithm
            // For now, delegate to BFS (which is equivalent for unweighted graphs)
            return new BreadthFirstSearchSolver().solve(maze, listener, stopFlag);
        }
    }
}