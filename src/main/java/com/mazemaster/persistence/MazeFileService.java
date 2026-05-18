package com.mazemaster.persistence;

import com.mazemaster.model.Maze;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

/**
 * Saves and loads maze state using a simple versioned text format.
 */
public class MazeFileService {
    private static final String MAGIC = "MAZE_MASTER_SAVE";
    private static final int VERSION = 2;

    public void save(Maze maze, Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(MAGIC + " " + VERSION);
            writer.newLine();
            writer.write(maze.getRows() + " " + maze.getColumns());
            writer.newLine();
            writer.write("seed " + maze.getGenerationSeed());
            writer.newLine();
            writer.write("algorithm " + encode(maze.getGenerationAlgorithm()));
            writer.newLine();

            for (int row = 0; row < maze.getRows(); row++) {
                for (int col = 0; col < maze.getColumns(); col++) {
                    if (col > 0) {
                        writer.write(' ');
                    }
                    writer.write(Integer.toString(maze.getCell(row, col)));
                }
                writer.newLine();
            }
        }
    }

    public Maze load(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.size() < 3) {
            throw new IOException("Maze file is incomplete");
        }

        String[] header = lines.get(0).trim().split("\\s+");
        if (header.length != 2 || !MAGIC.equals(header[0])) {
            throw new IOException("Unsupported maze file format");
        }

        int version = parseInt(header[1], "version");
        if (version != 1 && version != VERSION) {
            throw new IOException("Unsupported maze file version: " + version);
        }

        String[] dimensions = lines.get(1).trim().split("\\s+");
        if (dimensions.length != 2) {
            throw new IOException("Maze dimensions are missing");
        }

        int rows = parseInt(dimensions[0], "rows");
        int columns = parseInt(dimensions[1], "columns");
        int gridStartLine = 2;
        long generationSeed = 0L;
        String generationAlgorithm = "";
        if (version >= 2) {
            if (lines.size() < 5) {
                throw new IOException("Maze file metadata is incomplete");
            }
            generationSeed = parseMetadataLong(lines.get(2), "seed");
            generationAlgorithm = decode(parseMetadataValue(lines.get(3), "algorithm"));
            gridStartLine = 4;
        }

        if (rows < 3 || columns < 3 || lines.size() < rows + gridStartLine) {
            throw new IOException("Maze dimensions do not match file contents");
        }

        Maze maze = new Maze(rows, columns);
        for (int row = 0; row < rows; row++) {
            String[] cells = lines.get(row + gridStartLine).trim().split("\\s+");
            if (cells.length != columns) {
                throw new IOException("Maze row " + row + " has an invalid cell count");
            }

            for (int col = 0; col < columns; col++) {
                int cell = parseInt(cells[col], "cell");
                if (!isSupportedCell(cell)) {
                    throw new IOException("Unsupported cell value: " + cell);
                }
                maze.setCell(row, col, cell);
            }
        }
        maze.setGenerationMetadata(generationSeed, generationAlgorithm);

        return maze;
    }

    private int parseInt(String value, String fieldName) throws IOException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid " + fieldName + ": " + value, e);
        }
    }

    private boolean isSupportedCell(int cell) {
        return cell == Maze.BACKGROUND ||
            cell == Maze.WALL ||
            cell == Maze.PATH ||
            cell == Maze.EMPTY ||
            cell == Maze.VISITED ||
            cell == Maze.START ||
            cell == Maze.GOAL;
    }

    private long parseMetadataLong(String line, String key) throws IOException {
        return parseLong(parseMetadataValue(line, key), key);
    }

    private String parseMetadataValue(String line, String key) throws IOException {
        String[] parts = line.trim().split("\\s+", 2);
        if (parts.length != 2 || !key.equals(parts[0])) {
            throw new IOException("Missing maze metadata: " + key);
        }
        return parts[1];
    }

    private long parseLong(String value, String fieldName) throws IOException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IOException("Invalid " + fieldName + ": " + value, e);
        }
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String decode(String value) throws IOException {
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new IOException("Invalid algorithm metadata", e);
        }
    }
}
