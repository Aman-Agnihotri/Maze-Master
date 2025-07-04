// src/main/java/com/mazemaster/solving/MazeSolvingListener.java
package com.mazemaster.solving;

import java.awt.Point;
import java.util.List;

/**
 * Listener interface for maze solving events.
 */
public interface MazeSolvingListener {
    void onCellExplored(int row, int col);
    void onCellBacktracked(int row, int col);
    void onPathFound(List<Point> path);
    void onSolvingComplete(boolean solved);
}