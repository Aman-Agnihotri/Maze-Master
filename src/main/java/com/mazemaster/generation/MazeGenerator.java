package com.mazemaster.generation;

import com.mazemaster.model.Maze;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Orchestrates registered maze generation algorithms.
 */
public class MazeGenerator {
    private final Map<String, MazeGenerationStrategy> strategies;
    private final Random random;
    private MazeGenerationListener listener;
    private volatile int delayMs = 30;

    public MazeGenerator() {
        this(new Random());
    }

    public MazeGenerator(long seed) {
        this(new Random(seed));
    }

    MazeGenerator(Random random) {
        this.random = Objects.requireNonNull(random, "random");
        strategies = new LinkedHashMap<>();
        strategies.put("DFS", new DepthFirstSearchGenerator());
        strategies.put("Kruskal", new KruskalGenerator());
        strategies.put("Prim", new PrimGenerator());
    }

    public void setGenerationListener(MazeGenerationListener listener) {
        this.listener = listener;
    }

    public void setDelay(int delayMs) {
        this.delayMs = Math.max(1, delayMs);
    }

    public void setRandomSeed(long seed) {
        random.setSeed(seed);
    }

    public Set<String> getAvailableAlgorithms() {
        return strategies.keySet();
    }

    public void generate(Maze maze, String algorithm, AtomicBoolean stopFlag, AtomicBoolean pauseFlag) {
        MazeGenerationStrategy strategy = strategies.getOrDefault(algorithm, strategies.get("DFS"));
        MazeGenerationContext context = new MazeGenerationContext(listener, stopFlag, pauseFlag, random, () -> delayMs);

        maze.reset();
        strategy.generate(maze, context);
        if (listener != null && !stopFlag.get()) {
            listener.onGenerationComplete();
        }
    }
}
