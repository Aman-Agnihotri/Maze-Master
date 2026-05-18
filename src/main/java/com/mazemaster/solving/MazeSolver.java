package com.mazemaster.solving;

import com.mazemaster.model.Maze;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates registered maze solving algorithms.
 */
public class MazeSolver {
    private final Map<String, MazeSolvingStrategy> strategies;
    private MazeSolvingListener listener;
    private volatile int delayMs = 30;

    public MazeSolver() {
        strategies = new LinkedHashMap<>();
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
        MazeSolvingStrategy strategy = strategies.getOrDefault(algorithm, strategies.get("Depth First Search"));
        MazeSolvingContext context = new MazeSolvingContext(listener, stopFlag, pauseFlag, () -> delayMs);

        maze.resetSolution();
        boolean solved = strategy.solve(maze, context);
        if (listener != null && !stopFlag.get()) {
            listener.onSolvingComplete(solved);
        }
        return solved;
    }
}
