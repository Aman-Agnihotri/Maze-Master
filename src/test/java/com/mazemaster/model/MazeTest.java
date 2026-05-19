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

    @Test
    void shouldAllowEndpointPositionsToBeChanged() {
        assertThat(maze.setStartPosition(3, 5)).isTrue();
        assertThat(maze.setGoalPosition(17, 25)).isTrue();

        assertThat(maze.getStartRow()).isEqualTo(3);
        assertThat(maze.getStartCol()).isEqualTo(5);
        assertThat(maze.getGoalRow()).isEqualTo(17);
        assertThat(maze.getGoalCol()).isEqualTo(25);
        assertThat(maze.isStartCell(3, 5)).isTrue();
        assertThat(maze.isGoalCell(17, 25)).isTrue();
    }

    @Test
    void shouldRejectInvalidEndpointPositions() {
        assertThat(maze.setStartPosition(-1, 5)).isFalse();
        assertThat(maze.setGoalPosition(21, 5)).isFalse();
        assertThat(maze.setStartPosition(maze.getGoalRow(), maze.getGoalCol())).isFalse();
        assertThat(maze.setGoalPosition(maze.getStartRow(), maze.getStartCol())).isFalse();
    }

    @Test
    void shouldResetEndpointsToDefaultsWhenMazeIsReset() {
        maze.setStartPosition(3, 5);
        maze.setGoalPosition(17, 25);

        maze.reset();

        assertThat(maze.getStartRow()).isEqualTo(1);
        assertThat(maze.getStartCol()).isEqualTo(1);
        assertThat(maze.getGoalRow()).isEqualTo(19);
        assertThat(maze.getGoalCol()).isEqualTo(29);
    }
}
