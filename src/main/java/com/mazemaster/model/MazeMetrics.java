package com.mazemaster.model;

/**
 * Immutable runtime metrics for the current maze generation and solving cycle.
 */
public record MazeMetrics(
    long generationTimeMillis,
    long solvingTimeMillis,
    int walkableCells,
    int exploredCells,
    int pathLength,
    boolean solvingAttempted,
    boolean solved
) {
    public static final long NOT_RECORDED = -1L;

    public MazeMetrics {
        if (generationTimeMillis < NOT_RECORDED) {
            generationTimeMillis = NOT_RECORDED;
        }
        if (solvingTimeMillis < NOT_RECORDED) {
            solvingTimeMillis = NOT_RECORDED;
        }
        walkableCells = Math.max(0, walkableCells);
        exploredCells = Math.max(0, exploredCells);
        pathLength = Math.max(0, pathLength);
        if (!solvingAttempted) {
            solved = false;
        }
    }

    public static MazeMetrics empty() {
        return new MazeMetrics(NOT_RECORDED, NOT_RECORDED, 0, 0, 0, false, false);
    }

    public MazeMetrics withGenerationTime(long durationMillis) {
        return new MazeMetrics(durationMillis, NOT_RECORDED, walkableCells, 0, 0, false, false);
    }

    public MazeMetrics withGenerationTime(long durationMillis, int walkableCells) {
        return new MazeMetrics(durationMillis, NOT_RECORDED, walkableCells, 0, 0, false, false);
    }

    public MazeMetrics withWalkableCells(int walkableCells) {
        return new MazeMetrics(generationTimeMillis, solvingTimeMillis, walkableCells, exploredCells, pathLength, solvingAttempted, solved);
    }

    public MazeMetrics withSolvingStarted() {
        return new MazeMetrics(generationTimeMillis, NOT_RECORDED, walkableCells, 0, 0, true, false);
    }

    public MazeMetrics withExploredCells(int exploredCells) {
        return new MazeMetrics(generationTimeMillis, solvingTimeMillis, walkableCells, exploredCells, pathLength, solvingAttempted, solved);
    }

    public MazeMetrics withPathLength(int pathLength) {
        return new MazeMetrics(generationTimeMillis, solvingTimeMillis, walkableCells, exploredCells, pathLength, solvingAttempted, solved);
    }

    public MazeMetrics withSolvingComplete(long durationMillis, boolean solved, int pathLength) {
        return new MazeMetrics(generationTimeMillis, durationMillis, walkableCells, exploredCells, pathLength, true, solved);
    }

    public MazeMetrics withoutSolvingMetrics() {
        return new MazeMetrics(generationTimeMillis, NOT_RECORDED, walkableCells, 0, 0, false, false);
    }
}
