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
    private static final int VERSION = 3;

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
            writer.write("start " + maze.getStartRow() + " " + maze.getStartCol());
            writer.newLine();
            writer.write("goal " + maze.getGoalRow() + " " + maze.getGoalCol());
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
        if (version < 1 || version > VERSION) {
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
        int startRow = 1;
        int startCol = 1;
        int goalRow = rows - 2;
        int goalCol = columns - 2;
        if (version >= 2) {
            if (lines.size() < 5) {
                throw new IOException("Maze file metadata is incomplete");
            }
            generationSeed = parseMetadataLong(lines.get(2), "seed");
            generationAlgorithm = decode(parseMetadataValue(lines.get(3), "algorithm"));
            gridStartLine = 4;
        }
        if (version >= 3) {
            if (lines.size() < 7) {
                throw new IOException("Maze file endpoint metadata is incomplete");
            }
            int[] start = parseEndpointMetadata(lines.get(4), "start");
            int[] goal = parseEndpointMetadata(lines.get(5), "goal");
            startRow = start[0];
            startCol = start[1];
            goalRow = goal[0];
            goalCol = goal[1];
            gridStartLine = 6;
        }

        if (rows < 5 || columns < 5 || lines.size() < rows + gridStartLine) {
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
        if (!maze.setStartPosition(startRow, startCol) || !maze.setGoalPosition(goalRow, goalCol)) {
            throw new IOException("Invalid maze endpoint metadata");
        }

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

    private int[] parseEndpointMetadata(String line, String key) throws IOException {
        String[] parts = line.trim().split("\\s+");
        if (parts.length != 3 || !key.equals(parts[0])) {
            throw new IOException("Missing maze endpoint metadata: " + key);
        }
        return new int[]{parseInt(parts[1], key + " row"), parseInt(parts[2], key + " column")};
    }

    private String parseMetadataValue(String line, String key) throws IOException {
        String trimmedLine = line.stripLeading();
        if (trimmedLine.equals(key)) {
            return "";
        }
        if (!trimmedLine.startsWith(key + " ")) {
            throw new IOException("Missing maze metadata: " + key);
        }
        return trimmedLine.substring(key.length()).trim();
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
