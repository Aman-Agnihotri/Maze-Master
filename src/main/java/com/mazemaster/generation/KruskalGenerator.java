package com.mazemaster.generation;

import com.mazemaster.model.Maze;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Kruskal maze generation using union-find.
 */
final class KruskalGenerator implements MazeGenerationStrategy {

    @Override
    public void generate(Maze maze, MazeGenerationContext context) {
        int rows = ensureOddDimension(maze.getRows());
        int cols = ensureOddDimension(maze.getColumns());

        UnionFind unionFind = new UnionFind(rows, cols);
        List<Wall> walls = createAllWalls(rows, cols);
        Collections.shuffle(walls, context.getRandom());

        createInitialRooms(maze, rows, cols, context);
        processWalls(maze, walls, unionFind, context);
    }

    private int ensureOddDimension(int dimension) {
        return dimension % 2 == 0 ? dimension - 1 : dimension;
    }

    private void createInitialRooms(Maze maze, int rows, int cols, MazeGenerationContext context) {
        for (int i = 1; i < rows - 1; i += 2) {
            for (int j = 1; j < cols - 1; j += 2) {
                if (context.isStopped()) return;

                maze.setCell(i, j, Maze.EMPTY);
                context.notifyCellChanged(i, j, Maze.EMPTY);
                context.notifyGenerationStep();

                if (!context.pauseAwareSleep(context.scaledDelay(8))) {
                    return;
                }
            }
        }
    }

    private List<Wall> createAllWalls(int rows, int cols) {
        List<Wall> walls = new ArrayList<>();

        for (int i = 1; i < rows - 1; i += 2) {
            for (int j = 1; j < cols - 1; j += 2) {
                if (i < rows - 2) {
                    walls.add(new Wall(i + 1, j, i, j, i + 2, j));
                }
                if (j < cols - 2) {
                    walls.add(new Wall(i, j + 1, i, j, i, j + 2));
                }
            }
        }

        return walls;
    }

    private void processWalls(Maze maze, List<Wall> walls, UnionFind unionFind, MazeGenerationContext context) {
        for (Wall wall : walls) {
            if (context.isStopped()) return;

            if (!unionFind.isConnected(wall.room1Row, wall.room1Col, wall.room2Row, wall.room2Col)) {
                unionFind.union(wall.room1Row, wall.room1Col, wall.room2Row, wall.room2Col);
                maze.setCell(wall.row, wall.col, Maze.EMPTY);
                context.notifyCellChanged(wall.row, wall.col, Maze.EMPTY);
                context.notifyGenerationStep();

                if (!context.pauseAwareSleep(context.delayMs())) {
                    return;
                }
            }
        }
    }

    private static class UnionFind {
        private final int[] parent;
        private final int[] rank;
        private final int cols;

        UnionFind(int rows, int cols) {
            this.cols = cols;
            int size = rows * cols;
            parent = new int[size];
            rank = new int[size];

            for (int i = 0; i < size; i++) {
                parent[i] = i;
            }
        }

        private int getIndex(int row, int col) {
            return row * cols + col;
        }

        int find(int row, int col) {
            int index = getIndex(row, col);
            if (parent[index] != index) {
                parent[index] = find(parent[index] / cols, parent[index] % cols);
            }
            return parent[index];
        }

        void union(int row1, int col1, int row2, int col2) {
            int root1 = find(row1, col1);
            int root2 = find(row2, col2);

            if (root1 != root2) {
                if (rank[root1] < rank[root2]) {
                    parent[root1] = root2;
                } else if (rank[root1] > rank[root2]) {
                    parent[root2] = root1;
                } else {
                    parent[root2] = root1;
                    rank[root1]++;
                }
            }
        }

        boolean isConnected(int row1, int col1, int row2, int col2) {
            return find(row1, col1) == find(row2, col2);
        }
    }
}
