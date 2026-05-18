package com.mazemaster.generation;

/**
 * Represents a wall between two maze rooms.
 */
final class Wall {
    final int row;
    final int col;
    final int room1Row;
    final int room1Col;
    final int room2Row;
    final int room2Col;

    Wall(int row, int col) {
        this(row, col, -1, -1, -1, -1);
    }

    Wall(int row, int col, int room1Row, int room1Col, int room2Row, int room2Col) {
        this.row = row;
        this.col = col;
        this.room1Row = room1Row;
        this.room1Col = room1Col;
        this.room2Row = room2Row;
        this.room2Col = room2Col;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Wall wall = (Wall) obj;
        return row == wall.row && col == wall.col;
    }

    @Override
    public int hashCode() {
        return row * 31 + col;
    }
}
