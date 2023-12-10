import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.Queue;

/**
 * Creates a random maze, then solves it by finding a path from the
 * upper left corner to the lower right corner. (After doing
 * one maze, it waits a while then starts over by creating a
 * new random maze.)
 */
public class MazeApp extends JPanel implements ActionListener {

    // a main routine makes it possible to run this class as a program
    public static void main(String[] args) {
        JFrame window = new JFrame("Maze Master");
        window.setContentPane(new MazeApp());
        window.setSize(1100, 900);
        window.setLocationRelativeTo(null);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
    }

    int[][] maze; // Description of state of maze. The value of maze[i][j]
                  // is one of the constants wallCode, pathcode, emptyCode,
                  // or visitedCode. (Value can also be negative, temporarily,
                  // inside createMaze().)
                  // A maze is made up of walls and corridors. maze[i][j]
                  // is either part of a wall or part of a corridor. A cell
                  // cell that is part of a corridor is represented by pathCode
                  // if it is part of the current path through the maze, by
                  // visitedCode if it has already been explored without finding
                  // a solution, and by emptyCode if it has not yet been explored.

    static final int backgroundCode = 0;
    static final int wallCode = 1;
    static final int pathCode = 2;
    static final int emptyCode = 3;
    static final int visitedCode = 4;

    Color[] color; // colors associated with the preceding 5 constants
    static int rows = 41; // number of rows of cells in maze, including a wall around edges
    static int columns = 51; // number of columns of cells in maze, including a wall around edges
    int border = 0; // minimum number of pixels between maze and edge of panel
    int speedSleep = 30; // short delay between steps in making and solving maze
    int blockSize = 12; // size of each cell

    int width = -1; // width of panel, to be set by checkSize()
    int height = -1; // height of panel, to be set by checkSize()

    int totalWidth; // width of panel, minus border area (set in checkSize())
    int totalHeight; // height of panel, minus border area (set in checkSize())
    int left; // left edge of maze, allowing for border (set in checkSize())
    int top; // top edge of maze, allowing for border (set in checkSize())

    static boolean mazeExists = false; // set to true when maze[][] is valid; used in redrawMaze().

    private boolean isGenerating = false;
    boolean isSolving = false;
    private volatile boolean stopGeneration = false;
    private volatile boolean stopSolving = false;

    private transient Thread generationThread;

    MazePanel mazePanel;

    JButton solveButton;
    private JButton resetButton;
    private JButton exportButton;
    private JSlider speedSlider;
    private JTextField rowsField;
    private JTextField columnsField;
    private JComboBox<String> algorithmComboBox;
    private JPanel sidePanel;
    String algorithmSelected = "";

    public MazeApp() {
        color = new Color[] {
                new Color(200, 0, 0),
                new Color(200, 0, 0),
                new Color(128, 128, 255),
                Color.WHITE,
                new Color(200, 200, 200),
                Color.GREEN
        };

        setLayout(new BorderLayout());

        mazePanel = new MazePanel(this);
        add(mazePanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));

        JButton generateButton = new JButton("Generate Maze");
        generateButton.addActionListener(this);
        controlPanel.add(generateButton);

        solveButton = new JButton("Solve Maze");
        solveButton.addActionListener(this);
        controlPanel.add(solveButton);

        resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetMaze());
        controlPanel.add(resetButton);

        JButton saveButton = new JButton("Save Maze");
        saveButton.addActionListener(this);
        controlPanel.add(saveButton);

        JButton loadButton = new JButton("Load Maze");
        loadButton.addActionListener(this);
        controlPanel.add(loadButton);

        exportButton = new JButton("Export Maze");
        exportButton.addActionListener(e -> exportMaze());
        controlPanel.add(exportButton);

        // Initialize the JComboBox for algorithm selection
        String[] algorithms = {"Depth First Search", "Breadth First Search", "A*"};
        algorithmComboBox = new JComboBox<>(algorithms);
        algorithmComboBox.addActionListener(this);

        controlPanel.add(algorithmComboBox, BorderLayout.EAST);

        rowsField = new JTextField("41", 5); // Default value: 41
        columnsField = new JTextField("51", 5); // Default value: 51

        JPanel sizePanel = new JPanel();
        sizePanel.add(new JLabel("Rows:"));
        sizePanel.add(rowsField);
        sizePanel.add(new JLabel("Columns:"));
        sizePanel.add(columnsField);

        controlPanel.add(sizePanel);

        add(controlPanel, BorderLayout.NORTH);

        speedSlider = new JSlider(JSlider.VERTICAL, 1, 100, speedSleep);
        speedSlider.setMajorTickSpacing(5);
        speedSlider.setPaintTicks(true);
        speedSlider.addChangeListener(e -> {
            speedSleep = speedSlider.getValue();
        });

        JPanel speedPanel = new JPanel(new BorderLayout());
        speedPanel.add(new JLabel("Speed:"), BorderLayout.NORTH);
        speedPanel.add(speedSlider);

        add(speedPanel, BorderLayout.WEST);
    }

    private static final String SAVE_FILE_NAME = "mazeSave.ser";

    public void saveMazeState() {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(SAVE_FILE_NAME))) {
            outputStream.writeObject(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadMazeState() {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(SAVE_FILE_NAME))) {
            MazeApp loadedMaze = (MazeApp) inputStream.readObject();
            this.maze = loadedMaze.maze;
            this.rows = loadedMaze.rows;
            this.columns = loadedMaze.columns;
            this.mazeExists = true;
            this.repaint();
            mazePanel.setPreferredSize(new Dimension(blockSize * columns, blockSize * rows));
            mazePanel.revalidate();
            mazePanel.repaint();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void exportMaze() {
        if (!isGenerating) {
            int mazeWidth = totalWidth;
            int mazeHeight = totalHeight;
            BufferedImage image = new BufferedImage(mazeWidth, mazeHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();

            mazePanel.paint(g); // Paint only the maze panel

            g.dispose();

            try {
                File file = new File("maze.png");
                ImageIO.write(image, "png", file);
                JOptionPane.showMessageDialog(this, "Maze exported as maze.png");
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error exporting maze", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (isGenerating) {
            return;  // Do nothing if maze generation is in progress
        }

        // Check which button was clicked
        if (e.getSource() == solveButton) {
            // Get the selected algorithm from the JComboBox
            algorithmSelected = (String) algorithmComboBox.getSelectedItem();
        }

        if (e.getActionCommand().equals("Generate Maze")) {
            startMazeGeneration();
        } else if (e.getActionCommand().equals("Solve Maze")) {
            startMazeSolving();
        } else if (e.getActionCommand().equals("Save Maze")) {
            saveMazeState();
        } else if (e.getActionCommand().equals("Load Maze")) {
            loadMazeState();
        }
    }
    
    private void generateMaze() {
        isGenerating = true;
        generationThread = Thread.currentThread(); // Store the reference to the generation thread

        stopGeneration = false; // Reset the stop flag
        resetButton.setText("Stop"); // Change the button text to "Stop"
        
        makeMazeDFS();
        isGenerating = false;
        resetButton.setText("Reset"); // Change the button text back to "Reset"
    }
    
    private void solveMaze() {

        switch (algorithmSelected) {
            case "Depth First Search" -> solveMazeDFS(1,1);

            case "Breadth First Search" -> solveMazeBFS(1, 1);
        
            case "A*" -> solveMazeAStar(1, 1);

            default -> solveMazeDFS(1, 1);
        }
    }

    public void startMazeGeneration() {
        if (!isGenerating) {
            new Thread(this::generateMaze).start();
        }
    }
    public void startMazeSolving() {
        if (!isGenerating) {
            new Thread(this::solveMaze).start();
        }
    }

    private void resetMaze() {
        if (isGenerating) {
            stopGeneration = true;  // Set the flag to stop maze generation
            try {
                generationThread.join();  // Wait for the generation thread to finish
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stopGeneration = false;  // Reset the flag
        } else {
            // Reset the maze only if not generating the maze.
            maze = null;
            mazeExists = false;
            repaint();
        }
    }

    void checkSize() {
        // Called before drawing the maze, to set parameters used for drawing.
        if (getWidth() != width || getHeight() != height) {
            width = getWidth();
            height = getHeight();
            int w = (width - 2 * border) / columns;
            int h = (height - 2 * border) / rows;
            left = (width - w * columns) / 2;
            top = (height - h * rows) / 2;
            totalWidth = w * columns - left;
            totalHeight = h * rows - top;
        }
    }

    void redrawMaze(Graphics g) {
        // draws the entire maze
        if (mazeExists) {
            int w = totalWidth / columns; // width of each cell
            int h = totalHeight / rows; // height of each cell
            for (int j = 0; j < columns; j++)
                for (int i = 0; i < rows; i++) {
                    if (maze[i][j] < 0)
                        g.setColor(color[emptyCode]);
                    else
                        g.setColor(color[maze[i][j]]);
                    g.fillRect((j * w) + left, (i * h) + top, w, h);
                }
        }
    }
    
    void makeMazeDFS() {
        // Create a random maze. The strategy is to start with
        // a grid of disconnected "rooms" separated by walls.
        // then look at each of the separating walls, in a random
        // order. If tearing down a wall would not create a loop
        // in the maze, then tear it down. Otherwise, leave it in place.

        // Use user-inputted values for rows and columns
        rows = Integer.parseInt(rowsField.getText());
        columns = Integer.parseInt(columnsField.getText());

        if (maze == null || maze.length != rows || maze[0].length != columns)
            maze = new int[rows][columns];
        int i, j;
        int emptyCt = 0; // number of rooms
        int wallCt = 0; // number of walls
        int[] wallrow = new int[(rows * columns) / 2]; // position of walls between rooms
        int[] wallcol = new int[(rows * columns) / 2];
        for (i = 0; i < rows; i++) // start with everything being a wall
            for (j = 0; j < columns; j++)
                maze[i][j] = wallCode;
        for (i = 1; i < rows - 1; i += 2) // make a grid of empty rooms
            for (j = 1; j < columns - 1; j += 2) {
                emptyCt++;
                maze[i][j] = -emptyCt; // each room is represented by a different negative number
                if (i < rows - 2) { // record info about wall below this room
                    wallrow[wallCt] = i + 1;
                    wallcol[wallCt] = j;
                    wallCt++;
                }
                if (j < columns - 2) { // record info about wall to right of this room
                    wallrow[wallCt] = i;
                    wallcol[wallCt] = j + 1;
                    wallCt++;
                }
            }
        mazeExists = true;
        repaint();
        int r;
        for (i = wallCt - 1; i > 0; i--) {
            if (stopGeneration) {
                return;  // Stop maze generation if the flag is set
            }

            r = (int) (Math.random() * i); // choose a wall randomly and maybe tear it down
            tearDown(wallrow[r], wallcol[r]);
            wallrow[r] = wallrow[i];
            wallcol[r] = wallcol[i];
        }
        for (i = 1; i < rows - 1; i++) // replace negative values in maze[][] with emptyCode
            for (j = 1; j < columns - 1; j++)
                if (maze[i][j] < 0)
                    maze[i][j] = emptyCode;
    }

    synchronized void tearDown(int row, int col) {
        // Tear down a wall, unless doing so will form a loop. Tearing down a wall
        // joins two "rooms" into one "room". (Rooms begin to look like corridors
        // as they grow.) When a wall is torn down, the room codes on one side are
        // converted to match those on the other side, so all the cells in a room
        // have the same code. Note that if the room codes on both sides of a
        // wall already have the same code, then tearing down that wall would
        // create a loop, so the wall is left in place.
        if (row % 2 == 1 && maze[row][col - 1] != maze[row][col + 1]) {
            // row is odd; wall separates rooms horizontally
            fill(row, col - 1, maze[row][col - 1], maze[row][col + 1]);
            maze[row][col] = maze[row][col + 1];
            repaint();
            try {
                wait(speedSleep);
            } catch (InterruptedException e) {
            }
        } else if (row % 2 == 0 && maze[row - 1][col] != maze[row + 1][col]) {
            // row is even; wall separates rooms vertically
            fill(row - 1, col, maze[row - 1][col], maze[row + 1][col]);
            maze[row][col] = maze[row + 1][col];
            repaint();
            try {
                wait(speedSleep);
            } catch (InterruptedException e) {
            }
        }
    }
    
    void fill(int row, int col, int replace, int replaceWith) {
        // called by tearDown() to change "room codes".
        if (maze[row][col] == replace) {
            maze[row][col] = replaceWith;
            fill(row + 1, col, replace, replaceWith);
            fill(row - 1, col, replace, replaceWith);
            fill(row, col + 1, replace, replaceWith);
            fill(row, col - 1, replace, replaceWith);
        }
    }
    
    boolean solveMazeDFS(int row, int col) {
        // Try to solve the maze by continuing current path from position
        // (row,col). Return true if a solution is found. The maze is
        // considered to be solved if the path reaches the lower right cell.
        if (maze[row][col] == emptyCode) {
            maze[row][col] = pathCode; // add this cell to the path
            repaint();
            if (row == rows - 2 && col == columns - 2)
                return true; // path has reached the goal
            
            try {
                Thread.sleep(speedSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (solveMazeDFS(row - 1, col) || // try to solve maze by extending path
                solveMazeDFS(row, col - 1) || // in each possible direction
                solveMazeDFS(row + 1, col) ||
                solveMazeDFS(row, col + 1))
                return true;

            // maze can't be solved from this cell, so backtrack out of the cell
            maze[row][col] = visitedCode; // mark cell as having been visited
            repaint();

            synchronized (this) {
                try {
                    wait(speedSleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    private final int[] deltaRow = { -1, 0, 1, 0 };
    private final int[] deltaCol = { 0, -1, 0, 1 };

    static final int startCode = 5; // Code for the start cell
    static final int endCode = 6;   // Code for the end cell
    static final int pathRetracedCode = 5;
    Point[][] parent = new Point[rows][columns]; // To store parent information for backtracking

    boolean solveMazeBFS(int startRow, int startCol) {
        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(startRow, startCol)); // Start from the entrance
        maze[startRow][startCol] = pathCode; // Mark the starting point as part of the path

        while (!queue.isEmpty()) {
            Point current = queue.poll();

            int row = current.x;
            int col = current.y;

            // If the exit is reached, return true
            if (row == rows - 2 && col == columns - 2) {
                repaint();
                showShortestPath();
                return true;
            }

            // Try to move in each direction
            for (int i = 0; i < 4; i++) {
                int newRow = row + deltaRow[i];
                int newCol = col + deltaCol[i];

                // If the new cell is a valid path cell, mark it, add to the queue, and set parent information
                if (maze[newRow][newCol] == emptyCode) {
                    maze[newRow][newCol] = pathCode;
                    repaint();
                    queue.add(new Point(newRow, newCol));
                    parent[newRow][newCol] = new Point(row, col); // Save parent information

                    try {
                        Thread.sleep(speedSleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return false; // No solution found
    }
    
    void showShortestPath() {
        int row = rows - 2;
        int col = columns - 2;

        while (parent[row][col] != null) {
            Point current = parent[row][col];
            row = current.x;
            col = current.y;

            if (maze[row][col] != startCode && maze[row][col] != endCode) {
                maze[row][col] = pathRetracedCode; // Mark the path in green
                repaint();

                try {
                    Thread.sleep(speedSleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    boolean solveMazeAStar(int startRow, int startCol) {
        PriorityQueue<Point> priorityQueue = new PriorityQueue<>(Comparator.comparingDouble(this::calculateTotalCost));
        priorityQueue.add(new Point(startRow, startCol)); // Start from the entrance
        maze[startRow][startCol] = pathCode; // Mark the starting point as part of the path

        while (!priorityQueue.isEmpty()) {
            Point current = priorityQueue.poll();
            int row = current.x;
            int col = current.y;

            // If the exit is reached, backtrack to find the shortest path
            if (row == rows - 2 && col == columns - 2) {
                repaint();
                showShortestPath();
                return true;
            }

            // Try to move in each direction
            for (int i = 0; i < 4; i++) {
                int newRow = row + deltaRow[i];
                int newCol = col + deltaCol[i];

                // If the new cell is a valid path cell, mark it, add to the priority queue, and set parent information
                if (maze[newRow][newCol] == emptyCode) {
                    maze[newRow][newCol] = pathCode;
                    repaint();
                    priorityQueue.add(new Point(newRow, newCol));
                    parent[newRow][newCol] = new Point(row, col); // Save parent information

                    try {
                        Thread.sleep(speedSleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        return false; // No solution found
    }

    private double calculateTotalCost(Point point) {
        int row = point.x;
        int col = point.y;
        return calculateCostFromStart(row, col) + calculateHeuristic(row, col);
    }

    private double calculateCostFromStart(int row, int col) {
        // Calculate the cost from the start to the current cell
        return parent[row][col] == null ? 0 : calculateCostFromStart(parent[row][col].x, parent[row][col].y) + 1;
    }

    private double calculateHeuristic(int row, int col) {
        // Calculate the heuristic estimate from the current cell to the destination
        return Math.abs(row - (rows - 2)) + Math.abs(col - (columns - 2));
    }

}