package com.mazemaster.solving;

import java.awt.Point;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;

/**
 * Runtime controls shared by solving algorithms.
 */
public final class MazeSolvingContext {
    private final MazeSolvingListener listener;
    private final AtomicBoolean stopFlag;
    private final AtomicBoolean pauseFlag;
    private final IntSupplier delaySupplier;

    public MazeSolvingContext(MazeSolvingListener listener, AtomicBoolean stopFlag,
                              AtomicBoolean pauseFlag, IntSupplier delaySupplier) {
        this.listener = listener;
        this.stopFlag = stopFlag;
        this.pauseFlag = pauseFlag;
        this.delaySupplier = delaySupplier;
    }

    public boolean isStopped() {
        return stopFlag.get() || Thread.currentThread().isInterrupted();
    }

    public int delayMs() {
        return Math.max(1, delaySupplier.getAsInt());
    }

    public void notifyCellExplored(int row, int col) {
        if (listener != null) {
            listener.onCellExplored(row, col);
        }
    }

    public void notifyCellBacktracked(int row, int col) {
        if (listener != null) {
            listener.onCellBacktracked(row, col);
        }
    }

    public void notifyPathFound(List<Point> path) {
        if (listener != null && !isStopped()) {
            listener.onPathFound(path);
        }
    }

    public boolean pauseAwareSleep(int milliseconds) {
        if (!waitForResume()) {
            return false;
        }

        try {
            Thread.sleep(Math.max(1, milliseconds));
            return !isStopped();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private boolean waitForResume() {
        while (pauseFlag.get() && !stopFlag.get()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return !isStopped();
    }
}
