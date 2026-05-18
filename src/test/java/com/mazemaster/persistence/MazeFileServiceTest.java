package com.mazemaster.persistence;

import com.mazemaster.model.Maze;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MazeFileServiceTest {

    @Test
    void shouldRoundTripMazeState(@TempDir Path tempDir) throws IOException {
        MazeFileService service = new MazeFileService();
        Maze maze = new Maze(5, 5);
        Path saveFile = tempDir.resolve("round-trip.maze");

        maze.setCell(1, 1, Maze.EMPTY);
        maze.setCell(1, 2, Maze.PATH);
        maze.setCell(2, 2, Maze.VISITED);
        maze.setCell(3, 3, Maze.START);

        service.save(maze, saveFile);
        Maze loadedMaze = service.load(saveFile);

        assertThat(loadedMaze.getRows()).isEqualTo(5);
        assertThat(loadedMaze.getColumns()).isEqualTo(5);
        assertThat(loadedMaze.getGrid()).isDeepEqualTo(maze.getGrid());
    }

    @Test
    void shouldRejectUnsupportedFormat(@TempDir Path tempDir) throws IOException {
        MazeFileService service = new MazeFileService();
        Path saveFile = tempDir.resolve("invalid.maze");
        Files.writeString(saveFile, "not a maze");

        assertThatThrownBy(() -> service.load(saveFile))
            .isInstanceOf(IOException.class)
            .hasMessageContaining("incomplete");
    }
}
