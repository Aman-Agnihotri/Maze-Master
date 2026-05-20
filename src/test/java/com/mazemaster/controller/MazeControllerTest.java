package com.mazemaster.controller;

import com.mazemaster.model.Maze;
import com.mazemaster.model.MazeMetrics;
import com.mazemaster.ui.MazeView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.Point;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

class MazeControllerTest {

    @Test
    void shouldReportGenerationCompletionOnce() throws InterruptedException {
        MazeController controller = new MazeController();
        RecordingMazeView view = new RecordingMazeView();
        controller.setView(view);
        controller.createNewMaze(7, 7);
        controller.setAnimationSpeed(1);

        try {
            controller.generateMaze();
            await(() -> !controller.isGenerating());

            assertThat(view.generationCompleted.get()).isEqualTo(1);
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldReportSolvingCompletionOnce() throws InterruptedException {
        MazeController controller = new MazeController();
        RecordingMazeView view = new RecordingMazeView();
        controller.setView(view);
        controller.createNewMaze(7, 7);
        controller.setAnimationSpeed(1);

        try {
            controller.generateMaze();
            await(() -> !controller.isGenerating());

            controller.solveMaze();
            await(() -> !controller.isSolving());

            assertThat(view.solvingCompleted.get()).isEqualTo(1);
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldResetPausedGenerationToBlankMaze() throws InterruptedException {
        MazeController controller = new MazeController();
        RecordingMazeView view = new RecordingMazeView();
        controller.setView(view);
        controller.createNewMaze(21, 21);
        controller.setAnimationSpeed(50);

        try {
            controller.generateMaze();
            await(() -> controller.isGenerating() && hasAnyOpenCell(controller.getMaze()));

            controller.pauseResumeCurrentOperation();
            await(controller::isGenerationPaused);

            controller.resetMaze();

            assertThat(controller.isGenerating()).isFalse();
            assertThat(controller.isGenerationPaused()).isFalse();
            assertThat(hasAnyOpenCell(controller.getMaze())).isFalse();
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldResumeGenerationFromPausedState() throws InterruptedException {
        MazeController controller = new MazeController();
        RecordingMazeView view = new RecordingMazeView();
        controller.setView(view);
        controller.createNewMaze(21, 21);
        controller.setAnimationSpeed(50);

        try {
            controller.generateMaze();
            await(() -> controller.isGenerating() && hasAnyOpenCell(controller.getMaze()));

            controller.pauseResumeCurrentOperation();
            await(controller::isGenerationPaused);
            int cellsAtPause = waitForStableCellCount(controller.getMaze(), MazeControllerTest::countNonWallCells);

            Thread.sleep(150);

            assertThat(controller.isGenerating()).isTrue();
            assertThat(countNonWallCells(controller.getMaze())).isEqualTo(cellsAtPause);

            controller.setAnimationSpeed(1);
            controller.pauseResumeCurrentOperation();
            await(() -> !controller.isGenerating());

            assertThat(controller.isGenerationPaused()).isFalse();
            assertThat(controller.isMazeFullyGenerated()).isTrue();
            assertThat(view.generationCompleted.get()).isEqualTo(1);
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldResumeSolvingFromPausedState() throws InterruptedException {
        MazeController controller = new MazeController();
        RecordingMazeView view = new RecordingMazeView();
        controller.setView(view);
        controller.createNewMaze(21, 21);
        controller.setAnimationSpeed(1);

        try {
            controller.generateMaze();
            await(() -> !controller.isGenerating());

            controller.setAnimationSpeed(50);
            controller.solveMaze();
            await(() -> controller.isSolving() && countSolvingCells(controller.getMaze()) > 0);

            controller.pauseResumeCurrentOperation();
            await(controller::isSolvingPaused);
            int cellsAtPause = waitForStableCellCount(controller.getMaze(), MazeControllerTest::countSolvingCells);

            Thread.sleep(150);

            assertThat(controller.isSolving()).isTrue();
            assertThat(countSolvingCells(controller.getMaze())).isEqualTo(cellsAtPause);

            controller.setAnimationSpeed(1);
            controller.pauseResumeCurrentOperation();
            await(() -> !controller.isSolving());

            assertThat(controller.isSolvingPaused()).isFalse();
            assertThat(view.solvingCompleted.get()).isEqualTo(1);
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldNormalizeDimensionsWithinSupportedOddBounds() {
        MazeController controller = new MazeController();

        try {
            controller.createNewMaze(200, 4);

            assertThat(controller.getMaze().getRows()).isEqualTo(199);
            assertThat(controller.getMaze().getColumns()).isEqualTo(5);
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldTreatMazeAsGeneratedOnlyWhenStartCanReachGoal() {
        MazeController controller = new MazeController();

        try {
            controller.createNewMaze(5, 5);
            Maze maze = controller.getMaze();
            maze.setCell(maze.getStartRow(), maze.getStartCol(), Maze.EMPTY);
            maze.setCell(maze.getGoalRow(), maze.getGoalCol(), Maze.EMPTY);

            assertThat(controller.isMazeFullyGenerated()).isFalse();

            maze.setCell(1, 2, Maze.EMPTY);
            maze.setCell(1, 3, Maze.EMPTY);
            maze.setCell(2, 3, Maze.EMPTY);

            assertThat(controller.isMazeFullyGenerated()).isTrue();
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldNotSolveBlankMaze() {
        MazeController controller = new MazeController();
        RecordingMazeView view = new RecordingMazeView();
        controller.setView(view);
        controller.createNewMaze(7, 7);

        try {
            assertThat(controller.canSolveMaze()).isFalse();

            controller.solveMaze();

            assertThat(controller.isSolving()).isFalse();
            assertThat(view.solvingCompleted.get()).isZero();
            assertThat(view.latestMessage.get()).isEqualTo("Generate a reachable maze before solving.");
            assertThat(view.latestMessageWasError.get()).isFalse();
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldSaveAndLoadMazeUsingExplicitFileFormat(@TempDir Path tempDir) {
        MazeController controller = new MazeController();
        Path saveFile = tempDir.resolve("maze.maze");

        try {
            controller.setRandomSeed(1234L);
            controller.setGenerationAlgorithm("Prim");
            controller.createNewMaze(5, 5);
            Maze maze = controller.getMaze();
            maze.setCell(1, 1, Maze.EMPTY);
            maze.setCell(1, 2, Maze.PATH);
            maze.setCell(1, 3, Maze.START);

            controller.saveMaze(saveFile);
            controller.createNewMaze(7, 7);
            controller.loadMaze(saveFile);

            assertThat(controller.getMaze().getRows()).isEqualTo(5);
            assertThat(controller.getMaze().getColumns()).isEqualTo(5);
            assertThat(controller.getMaze().getCell(1, 1)).isEqualTo(Maze.EMPTY);
            assertThat(controller.getMaze().getCell(1, 2)).isEqualTo(Maze.PATH);
            assertThat(controller.getMaze().getCell(1, 3)).isEqualTo(Maze.START);
            assertThat(controller.getCurrentGenerationSeed()).isEqualTo(1234L);
            assertThat(controller.getCurrentGenerationAlgorithm()).isEqualTo("Prim");
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldCreateMazeFromSeedReproducibly() throws InterruptedException {
        MazeController controller = new MazeController();
        controller.setGenerationAlgorithm("Kruskal");
        controller.setAnimationSpeed(1);

        try {
            controller.createMazeFromSeed(21, 21, 5678L);
            await(() -> !controller.isGenerating());
            int[][] firstGrid = controller.getMaze().getGrid();

            controller.createMazeFromSeed(21, 21, 5678L);
            await(() -> !controller.isGenerating());

            assertThat(controller.getCurrentGenerationSeed()).isEqualTo(5678L);
            assertThat(controller.getMaze().getGenerationSeed()).isEqualTo(5678L);
            assertThat(controller.getMaze().getGenerationAlgorithm()).isEqualTo("Kruskal");
            assertThat(controller.getMaze().getGrid()).isDeepEqualTo(firstGrid);
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldSetStartAndGoalCellsOnOpenMazeCells() {
        MazeController controller = new MazeController();

        try {
            controller.createNewMaze(5, 5);
            Maze maze = controller.getMaze();
            maze.setCell(1, 1, Maze.EMPTY);
            maze.setCell(1, 2, Maze.EMPTY);
            maze.setCell(1, 3, Maze.EMPTY);
            maze.setCell(2, 3, Maze.EMPTY);
            maze.setCell(3, 3, Maze.EMPTY);

            assertThat(controller.setStartCell(1, 3)).isTrue();
            assertThat(controller.setGoalCell(3, 3)).isTrue();

            assertThat(maze.getStartRow()).isEqualTo(1);
            assertThat(maze.getStartCol()).isEqualTo(3);
            assertThat(maze.getGoalRow()).isEqualTo(3);
            assertThat(maze.getGoalCol()).isEqualTo(3);
            assertThat(controller.isMazeFullyGenerated()).isTrue();
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldRejectEndpointCellsOnWallsOrExistingEndpoint() {
        MazeController controller = new MazeController();

        try {
            controller.createNewMaze(5, 5);
            Maze maze = controller.getMaze();
            maze.setCell(1, 1, Maze.EMPTY);
            maze.setCell(3, 3, Maze.EMPTY);

            assertThat(controller.setStartCell(0, 0)).isFalse();
            assertThat(controller.setStartCell(maze.getGoalRow(), maze.getGoalCol())).isFalse();
            assertThat(controller.setGoalCell(maze.getStartRow(), maze.getStartCol())).isFalse();

            assertThat(maze.getStartRow()).isEqualTo(1);
            assertThat(maze.getStartCol()).isEqualTo(1);
            assertThat(maze.getGoalRow()).isEqualTo(3);
            assertThat(maze.getGoalCol()).isEqualTo(3);
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldClearSolutionWhenEndpointChanges() {
        MazeController controller = new MazeController();

        try {
            controller.createNewMaze(5, 5);
            Maze maze = controller.getMaze();
            maze.setCell(1, 1, Maze.PATH);
            maze.setCell(1, 2, Maze.START);
            maze.setCell(1, 3, Maze.VISITED);
            maze.setCell(3, 3, Maze.EMPTY);

            assertThat(controller.setStartCell(1, 2)).isTrue();

            assertThat(maze.getStartRow()).isEqualTo(1);
            assertThat(maze.getStartCol()).isEqualTo(2);
            assertThat(maze.getCell(1, 1)).isEqualTo(Maze.EMPTY);
            assertThat(maze.getCell(1, 2)).isEqualTo(Maze.EMPTY);
            assertThat(maze.getCell(1, 3)).isEqualTo(Maze.EMPTY);
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldTrackGenerationAndSolvingMetrics() throws InterruptedException {
        MazeController controller = new MazeController();
        RecordingMazeView view = new RecordingMazeView();
        controller.setView(view);
        controller.createNewMaze(11, 11);
        controller.setAnimationSpeed(1);
        controller.setSolvingAlgorithm("Breadth First Search");

        try {
            controller.generateMaze();
            await(() -> !controller.isGenerating());

            MazeMetrics afterGeneration = controller.getMetrics();
            assertThat(afterGeneration.generationTimeMillis()).isGreaterThanOrEqualTo(0L);
            assertThat(afterGeneration.solvingTimeMillis()).isEqualTo(MazeMetrics.NOT_RECORDED);
            assertThat(afterGeneration.walkableCells()).isEqualTo(countWalkableCells(controller.getMaze()));
            assertThat(afterGeneration.walkableCells()).isPositive();
            assertThat(afterGeneration.solvingAttempted()).isFalse();

            controller.solveMaze();
            await(() -> !controller.isSolving());

            MazeMetrics afterSolving = controller.getMetrics();
            assertThat(afterSolving.generationTimeMillis()).isEqualTo(afterGeneration.generationTimeMillis());
            assertThat(afterSolving.solvingTimeMillis()).isGreaterThanOrEqualTo(0L);
            assertThat(afterSolving.walkableCells()).isEqualTo(afterGeneration.walkableCells());
            assertThat(afterSolving.exploredCells()).isPositive();
            assertThat(afterSolving.exploredCells()).isLessThanOrEqualTo(afterSolving.walkableCells());
            assertThat(afterSolving.pathLength()).isPositive();
            assertThat(afterSolving.solvingAttempted()).isTrue();
            assertThat(afterSolving.solved()).isTrue();
            assertThat(view.latestMetrics.get()).isEqualTo(afterSolving);
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldClearSolvingMetricsWhenSolutionIsCleared() throws InterruptedException {
        MazeController controller = new MazeController();
        controller.createNewMaze(11, 11);
        controller.setAnimationSpeed(1);
        controller.setSolvingAlgorithm("Breadth First Search");

        try {
            controller.generateMaze();
            await(() -> !controller.isGenerating());
            long generationTimeMillis = controller.getMetrics().generationTimeMillis();

            controller.solveMaze();
            await(() -> !controller.isSolving());
            assertThat(controller.getMetrics().solvingAttempted()).isTrue();

            controller.clearSolution();

            MazeMetrics metrics = controller.getMetrics();
            assertThat(metrics.generationTimeMillis()).isEqualTo(generationTimeMillis);
            assertThat(metrics.solvingTimeMillis()).isEqualTo(MazeMetrics.NOT_RECORDED);
            assertThat(metrics.walkableCells()).isPositive();
            assertThat(metrics.exploredCells()).isZero();
            assertThat(metrics.pathLength()).isZero();
            assertThat(metrics.solvingAttempted()).isFalse();
            assertThat(metrics.solved()).isFalse();
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldResetMetricsWhenCreatingNewMaze() throws InterruptedException {
        MazeController controller = new MazeController();
        controller.createNewMaze(11, 11);
        controller.setAnimationSpeed(1);

        try {
            controller.generateMaze();
            await(() -> !controller.isGenerating());
            assertThat(controller.getMetrics().generationTimeMillis()).isGreaterThanOrEqualTo(0L);

            controller.createNewMaze(7, 7);

            assertThat(controller.getMetrics()).isEqualTo(MazeMetrics.empty());
        } finally {
            controller.shutdown();
        }
    }

    @Test
    void shouldResetMetricsWhenLoadingMaze(@TempDir Path tempDir) throws InterruptedException {
        MazeController controller = new MazeController();
        Path saveFile = tempDir.resolve("maze.maze");
        controller.createNewMaze(7, 7);
        controller.setAnimationSpeed(1);
        controller.setSolvingAlgorithm("Breadth First Search");

        try {
            controller.generateMaze();
            await(() -> !controller.isGenerating());
            controller.saveMaze(saveFile);

            controller.solveMaze();
            await(() -> !controller.isSolving());
            assertThat(controller.getMetrics().solvingAttempted()).isTrue();

            controller.loadMaze(saveFile);

            MazeMetrics metrics = controller.getMetrics();
            assertThat(metrics.generationTimeMillis()).isEqualTo(MazeMetrics.NOT_RECORDED);
            assertThat(metrics.solvingTimeMillis()).isEqualTo(MazeMetrics.NOT_RECORDED);
            assertThat(metrics.walkableCells()).isEqualTo(countWalkableCells(controller.getMaze()));
            assertThat(metrics.walkableCells()).isPositive();
            assertThat(metrics.exploredCells()).isZero();
            assertThat(metrics.pathLength()).isZero();
            assertThat(metrics.solvingAttempted()).isFalse();
            assertThat(metrics.solved()).isFalse();
        } finally {
            controller.shutdown();
        }
    }

    private static void await(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertThat(condition.getAsBoolean()).isTrue();
    }

    private static int waitForStableCellCount(Maze maze, CellCounter counter) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5_000;
        int previous = counter.count(maze);
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(120);
            int current = counter.count(maze);
            if (current == previous) {
                return current;
            }
            previous = current;
        }
        return previous;
    }

    private static boolean hasAnyOpenCell(Maze maze) {
        for (int row = 0; row < maze.getRows(); row++) {
            for (int col = 0; col < maze.getColumns(); col++) {
                if (maze.getCell(row, col) != Maze.WALL) {
                    return true;
                }
            }
        }
        return false;
    }

    private static int countNonWallCells(Maze maze) {
        int count = 0;
        for (int row = 0; row < maze.getRows(); row++) {
            for (int col = 0; col < maze.getColumns(); col++) {
                if (maze.getCell(row, col) != Maze.WALL) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countSolvingCells(Maze maze) {
        int count = 0;
        for (int row = 0; row < maze.getRows(); row++) {
            for (int col = 0; col < maze.getColumns(); col++) {
                int cell = maze.getCell(row, col);
                if (cell == Maze.PATH || cell == Maze.VISITED || cell == Maze.START) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countWalkableCells(Maze maze) {
        int count = 0;
        for (int row = 0; row < maze.getRows(); row++) {
            for (int col = 0; col < maze.getColumns(); col++) {
                if (maze.isWalkable(row, col)) {
                    count++;
                }
            }
        }
        return count;
    }

    @FunctionalInterface
    private interface CellCounter {
        int count(Maze maze);
    }

    private static class NoOpMazeView implements MazeView {
        @Override
        public void updateMaze(Maze maze) {
            ignoreCallback();
        }

        @Override
        public void refresh() {
            ignoreCallback();
        }

        @Override
        public void onCellChanged(int row, int col, int newValue) {
            ignoreCallback();
        }

        @Override
        public void onGenerationStarted() {
            ignoreCallback();
        }

        @Override
        public void onGenerationCompleted() {
            ignoreCallback();
        }

        @Override
        public void onSolvingStarted() {
            ignoreCallback();
        }

        @Override
        public void onSolvingCompleted(boolean solved) {
            ignoreCallback();
        }

        @Override
        public void onPathFound(List<Point> path) {
            ignoreCallback();
        }

        @Override
        public void onOperationPaused() {
            ignoreCallback();
        }

        @Override
        public void onOperationResumed() {
            ignoreCallback();
        }

        @Override
        public void showMessage(String message, boolean isError) {
            ignoreCallback();
        }

        @Override
        public int[] getMazeDimensions() {
            return new int[0];
        }

        @Override
        public void showProgress(boolean show, String message) {
            ignoreCallback();
        }

        @Override
        public void updateGenerationAlgorithms(Set<String> algorithms) {
            ignoreCallback();
        }

        @Override
        public void updateSolvingAlgorithms(Set<String> algorithms) {
            ignoreCallback();
        }

        @Override
        public void setSelectedGenerationAlgorithm(String algorithm) {
            ignoreCallback();
        }

        @Override
        public void setGenerationSeed(long seed) {
            ignoreCallback();
        }

        @Override
        public void setSelectedSolvingAlgorithm(String algorithm) {
            ignoreCallback();
        }

        @Override
        public void updateControlsState(boolean isGenerating, boolean isSolving) {
            ignoreCallback();
        }

        @Override
        public boolean exportToImage(String filename) {
            return false;
        }

        @Override
        public String getExportFilename() {
            return null;
        }

        private void ignoreCallback() {
            // This test adapter intentionally ignores callbacks that are not asserted by a given test.
        }
    }

    private static class RecordingMazeView extends NoOpMazeView {
        private final AtomicInteger generationCompleted = new AtomicInteger();
        private final AtomicInteger solvingCompleted = new AtomicInteger();
        private final AtomicReference<MazeMetrics> latestMetrics = new AtomicReference<>(MazeMetrics.empty());
        private final AtomicReference<String> latestMessage = new AtomicReference<>("");
        private final AtomicReference<Boolean> latestMessageWasError = new AtomicReference<>(false);

        @Override
        public void onGenerationCompleted() {
            generationCompleted.incrementAndGet();
        }

        @Override
        public void onSolvingCompleted(boolean solved) {
            solvingCompleted.incrementAndGet();
        }

        @Override
        public void updateMetrics(MazeMetrics metrics) {
            latestMetrics.set(metrics);
        }

        @Override
        public void showMessage(String message, boolean isError) {
            latestMessage.set(message);
            latestMessageWasError.set(isError);
        }
    }
}
