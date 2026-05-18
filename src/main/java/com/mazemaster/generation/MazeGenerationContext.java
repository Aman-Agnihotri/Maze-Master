package com.mazemaster.generation;

import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;

/**
 * Runtime controls shared by generation algorithms.
 */
public final class MazeGenerationContext {
    private final MazeGenerationListener listener;
    private final AtomicBoolean stopFlag;
    private final AtomicBoolean pauseFlag;
    private final Random random;
    private final IntSupplier delaySupplier;

    public MazeGenerationContext(MazeGenerationListener listener, AtomicBoolean stopFlag,
                                AtomicBoolean pauseFlag, Random random, IntSupplier delaySupplier) {
        this.listener = listener;
        this.stopFlag = stopFlag;
        this.pauseFlag = pauseFlag;
        this.random = random;
        this.delaySupplier = delaySupplier;
    }

    public MazeGenerationListener getListener() {
        return listener;
    }

    public Random getRandom() {
        return random;
    }

    public boolean isStopped() {
        return stopFlag.get() || Thread.currentThread().isInterrupted();
    }

    public int delayMs() {
        return Math.max(1, delaySupplier.getAsInt());
    }

    public int scaledDelay(int divisor) {
        return Math.max(1, delayMs() / divisor);
    }

    public void notifyCellChanged(int row, int col, int newValue) {
        if (listener != null) {
            listener.onCellChanged(row, col, newValue);
        }
    }

    public void notifyGenerationStep() {
        if (listener != null) {
            listener.onGenerationStep();
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
