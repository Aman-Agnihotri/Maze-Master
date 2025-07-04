// src/test/java/com/mazemaster/model/MazeTest.java
package com.mazemaster.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.assertj.core.api.Assertions.*;

class MazeTest {
    private Maze maze;
    
    @BeforeEach
    void setUp() {
        maze = new Maze(21, 31);
    }
    
    @Test
    void shouldCreateMazeWithCorrectDimensions() {
        assertThat(maze.getRows()).isEqualTo(21);
        assertThat(maze.getColumns()).isEqualTo(31);
    }
    
    @Test
    void shouldInitializeAllCellsAsWalls() {
        for (int i = 0; i < 21; i++) {
            for (int j = 0; j < 31; j++) {
                assertThat(maze.getCell(i, j)).isEqualTo(Maze.WALL);
            }
        }
    }
}