package com.mazemaster.solving;

import com.mazemaster.model.Maze;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MazeSolverTest {

    @ParameterizedTest
    @MethodSource("solvingAlgorithms")
    void shouldSolveReachableMazeAndReportCompletionOnce(String algorithm) {
        Maze maze = createOpenMaze();
        MazeSolver solver = new MazeSolver();
        RecordingSolvingListener listener = new RecordingSolvingListener();

        solver.setDelay(1);
        solver.setSolvingListener(listener);

        boolean solved = solver.solve(maze, algorithm, new AtomicBoolean(false), new AtomicBoolean(false));

        assertThat(solved).isTrue();
        assertThat(listener.completionResults).containsExactly(true);
        assertThat(maze.getCell(maze.getGoalRow(), maze.getGoalCol()))
            .isIn(Maze.PATH, Maze.START);
    }

    @ParameterizedTest
    @MethodSource("solvingAlgorithms")
    void shouldReportUnsolvableMazeOnce(String algorithm) {
        Maze maze = new Maze(5, 5);
        MazeSolver solver = new MazeSolver();
        RecordingSolvingListener listener = new RecordingSolvingListener();

        maze.setCell(maze.getStartRow(), maze.getStartCol(), Maze.EMPTY);
        solver.setDelay(1);
        solver.setSolvingListener(listener);

        boolean solved = solver.solve(maze, algorithm, new AtomicBoolean(false), new AtomicBoolean(false));

        assertThat(solved).isFalse();
        assertThat(listener.completionResults).containsExactly(false);
    }

    private static Stream<String> solvingAlgorithms() {
        return Stream.of("Depth First Search", "Breadth First Search", "A*");
    }

    private Maze createOpenMaze() {
        Maze maze = new Maze(5, 5);
        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 3; col++) {
                maze.setCell(row, col, Maze.EMPTY);
            }
        }
        return maze;
    }

    private static class RecordingSolvingListener implements MazeSolvingListener {
        private final List<Boolean> completionResults = new ArrayList<>();

        @Override
        public void onCellExplored(int row, int col) {
        }

        @Override
        public void onCellBacktracked(int row, int col) {
        }

        @Override
        public void onPathFound(List<Point> path) {
        }

        @Override
        public void onSolvingComplete(boolean solved) {
            completionResults.add(solved);
        }
    }
}
