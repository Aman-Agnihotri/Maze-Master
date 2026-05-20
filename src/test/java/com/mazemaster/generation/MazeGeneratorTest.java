package com.mazemaster.generation;

import com.mazemaster.model.Maze;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MazeGeneratorTest {

    @ParameterizedTest
    @MethodSource("generationAlgorithms")
    void shouldGenerateReachableMazeAndReportCompletionOnce(String algorithm) {
        Maze maze = new Maze(21, 21);
        MazeGenerator generator = new MazeGenerator();
        AtomicInteger completionCount = new AtomicInteger();

        generator.setDelay(1);
        generator.setGenerationListener(new RecordingGenerationListener(completionCount));

        generator.generate(maze, algorithm, new AtomicBoolean(false), new AtomicBoolean(false));

        assertThat(completionCount.get()).isEqualTo(1);
        assertThat(isReachable(maze)).isTrue();
    }

    @ParameterizedTest
    @MethodSource("generationAlgorithms")
    void shouldGenerateSameMazeForSameSeed(String algorithm) {
        Maze firstMaze = new Maze(21, 21);
        Maze secondMaze = new Maze(21, 21);
        MazeGenerator firstGenerator = new MazeGenerator(12345L);
        MazeGenerator secondGenerator = new MazeGenerator(12345L);

        firstGenerator.setDelay(1);
        secondGenerator.setDelay(1);

        firstGenerator.generate(firstMaze, algorithm, new AtomicBoolean(false), new AtomicBoolean(false));
        secondGenerator.generate(secondMaze, algorithm, new AtomicBoolean(false), new AtomicBoolean(false));

        assertThat(firstMaze.getGrid()).isDeepEqualTo(secondMaze.getGrid());
    }

    private static Stream<String> generationAlgorithms() {
        return Stream.of("DFS", "Kruskal", "Prim");
    }

    private boolean isReachable(Maze maze) {
        Point start = new Point(maze.getStartRow(), maze.getStartCol());
        Point goal = new Point(maze.getGoalRow(), maze.getGoalCol());
        Queue<Point> queue = new ArrayDeque<>();
        Set<Point> visited = new HashSet<>();

        queue.offer(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Point current = queue.poll();
            if (current.equals(goal)) {
                return true;
            }

            for (int[] direction : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                Point next = new Point(current.x + direction[0], current.y + direction[1]);
                if (!visited.contains(next) && maze.isWalkable(next.x, next.y)) {
                    visited.add(next);
                    queue.offer(next);
                }
            }
        }

        return false;
    }

    private static class RecordingGenerationListener implements MazeGenerationListener {
        private final AtomicInteger completionCount;

        RecordingGenerationListener(AtomicInteger completionCount) {
            this.completionCount = completionCount;
        }

        @Override
        public void onCellChanged(int row, int col, int newValue) {
            ignoreCallback();
        }

        @Override
        public void onGenerationStep() {
            ignoreCallback();
        }

        @Override
        public void onGenerationComplete() {
            completionCount.incrementAndGet();
        }

        private void ignoreCallback() {
            // These tests only assert generation completion; per-cell callbacks are intentionally ignored.
        }
    }
}
