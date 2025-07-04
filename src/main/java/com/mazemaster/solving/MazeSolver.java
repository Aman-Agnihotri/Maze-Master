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
        @Override
        public boolean solve(Maze maze, MazeSolvingListener listener, AtomicBoolean stopFlag) {
            boolean result = solveDFS(maze, maze.getStartRow(), maze.getStartCol(), listener, stopFlag);
            
            if (listener != null) {
                listener.onSolvingComplete(result);
            }
            
            return result;
        }
        
        private boolean solveDFS(Maze maze, int row, int col, MazeSolvingListener listener, AtomicBoolean stopFlag) {
            if (stopFlag.get()) return false;
            
            if (maze.isEmpty(row, col)) {
                maze.setCell(row, col, Maze.PATH);
                
                if (listener != null) {
                    listener.onCellExplored(row, col);
                }
                
                // Check if goal reached
                if (row == maze.getGoalRow() && col == maze.getGoalCol()) {
                    return true;
                }
                
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                
                // Try all four directions
                int[] deltaRow = {-1, 0, 1, 0};
                int[] deltaCol = {0, 1, 0, -1};
                
                for (int i = 0; i < 4; i++) {
                    if (solveDFS(maze, row + deltaRow[i], col + deltaCol[i], listener, stopFlag)) {
                        return true;
                    }
                }
                
                // Backtrack
                maze.setCell(row, col, Maze.VISITED);
                
                if (listener != null) {
                    listener.onCellBacktracked(row, col);
                }
                
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            
            return false;
        }
    }
    
    /**
     * Breadth-First Search solver (guarantees shortest path)
     */
    private class BreadthFirstSearchSolver implements MazeSolvingStrategy {
        @Override
        public boolean solve(Maze maze, MazeSolvingListener listener, AtomicBoolean stopFlag) {
            Queue<Point> queue = new LinkedList<>();
            Map<Point, Point> parent = new HashMap<>();
            
            Point start = new Point(maze.getStartRow(), maze.getStartCol());
            Point goal = new Point(maze.getGoalRow(), maze.getGoalCol());
            
            queue.offer(start);
            maze.setCell(start.x, start.y, Maze.PATH);
            parent.put(start, null);
            
            int[] deltaRow = {-1, 0, 1, 0};
            int[] deltaCol = {0, 1, 0, -1};
            
            while (!queue.isEmpty() && !stopFlag.get()) {
                Point current = queue.poll();
                
                if (current.equals(goal)) {
                    // Reconstruct and highlight path
                    List<Point> path = reconstructPath(parent, goal);
                    highlightPath(maze, path, listener);
                    
                    if (listener != null) {
                        listener.onPathFound(path);
                        listener.onSolvingComplete(true);
                    }
                    
                    return true;
                }
                
                // Explore neighbors
                for (int i = 0; i < 4; i++) {
                    int newRow = current.x + deltaRow[i];
                    int newCol = current.y + deltaCol[i];
                    Point neighbor = new Point(newRow, newCol);
                    
                    if (maze.isEmpty(newRow, newCol)) {
                        maze.setCell(newRow, newCol, Maze.PATH);
                        queue.offer(neighbor);
                        parent.put(neighbor, current);
                        
                        if (listener != null) {
                            listener.onCellExplored(newRow, newCol);
                        }
                        
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return false;
                        }
                    }
                }
            }
            
            if (listener != null) {
                listener.onSolvingComplete(false);
            }
            
            return false;
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
     * A* Search solver (heuristic-based, optimal)
     */
    private class AStarSolver implements MazeSolvingStrategy {
        @Override
        public boolean solve(Maze maze, MazeSolvingListener listener, AtomicBoolean stopFlag) {
            PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fCost));
            Map<Point, Node> allNodes = new HashMap<>();
            Set<Point> closedSet = new HashSet<>();
            
            Point start = new Point(maze.getStartRow(), maze.getStartCol());
            Point goal = new Point(maze.getGoalRow(), maze.getGoalCol());
            
            Node startNode = new Node(start, 0, manhattanDistance(start, goal), null);
            openSet.offer(startNode);
            allNodes.put(start, startNode);
            
            int[] deltaRow = {-1, 0, 1, 0};
            int[] deltaCol = {0, 1, 0, -1};
            
            while (!openSet.isEmpty() && !stopFlag.get()) {
                Node current = openSet.poll();
                closedSet.add(current.position);
                
                maze.setCell(current.position.x, current.position.y, Maze.PATH);
                
                if (listener != null) {
                    listener.onCellExplored(current.position.x, current.position.y);
                }
                
                if (current.position.equals(goal)) {
                    // Reconstruct and highlight path
                    List<Point> path = reconstructPath(current);
                    highlightPath(maze, path, listener);
                    
                    if (listener != null) {
                        listener.onPathFound(path);
                        listener.onSolvingComplete(true);
                    }
                    
                    return true;
                }
                
                // Explore neighbors
                for (int i = 0; i < 4; i++) {
                    int newRow = current.position.x + deltaRow[i];
                    int newCol = current.position.y + deltaCol[i];
                    Point neighbor = new Point(newRow, newCol);
                    
                    if (!maze.isValidPosition(newRow, newCol) || 
                        !maze.isWalkable(newRow, newCol) || 
                        closedSet.contains(neighbor)) {
                        continue;
                    }
                    
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
                
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            
            if (listener != null) {
                listener.onSolvingComplete(false);
            }
            
            return false;
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