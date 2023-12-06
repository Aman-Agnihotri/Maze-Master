import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Creates a random maze, then solves it by finding a path from the
 * upper left corner to the lower right corner.
 */
public class Maze extends JPanel implements ActionListener {

    // a main routine makes it possible to run this class as a program
    public static void main(String[] args) {
        JFrame window = new JFrame("Maze Master");
        window.setContentPane(new Maze());
        window.pack();
        window.setLocation(120, 80);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);
    }

    int[][] maze; // Description of state of maze. The value of maze[i][j]
                  // is one of the constants wallCode, pathcode, emptyCode,
                  // or visitedCode. (Value can also be negative, temporarily, inside
                  // createMaze().)
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
    int rows = 41; // number of rows of cells in maze, including a wall around edges
    int columns = 51; // number of columns of cells in maze, including a wall around edges
    int border = 0; // minimum number of pixels between maze and edge of panel
    int sleepTime = 5000; // wait time after solving one maze before making another
    int speedSleep = 30; // short delay between steps in making and solving maze
    int blockSize = 12; // size of each cell

    int width = -1; // width of panel, to be set by checkSize()
    int height = -1; // height of panel, to be set by checkSize()

    int totalWidth; // width of panel, minus border area (set in checkSize())
    int totalHeight; // height of panel, minus border area (set in checkSize())
    int left; // left edge of maze, allowing for border (set in checkSize())
    int top; // top edge of maze, allowing for border (set in checkSize())

    boolean mazeExists = false; // set to true when maze[][] is valid; used in
                                // redrawMaze(); set to true in createMaze(), and
                                // reset to false in resetMaze()
    private boolean isGenerating = false;
    private JButton resetButton;

    public Maze() {
        color = new Color[] {
                new Color(200, 0, 0),
                new Color(200, 0, 0),
                new Color(128, 128, 255),
                Color.WHITE,
                new Color(200, 200, 200)
        };
        setBackground(color[backgroundCode]);
        setPreferredSize(new Dimension(blockSize * columns, blockSize * rows));

        JButton startButton = new JButton("Generate Maze");
        startButton.addActionListener(this);
        add(startButton);

        resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetMaze());
        add(resetButton);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isGenerating) {
            new Thread(this::generateAndSolveMaze).start();
        }
    }

    private void generateAndSolveMaze() {
        isGenerating = true;
        makeMaze();
        solveMaze(1, 1);
        BFS(maze);
        isGenerating = false;
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
            totalWidth = w * columns;
            totalHeight = h * rows;
        }
    }

    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        checkSize();
        redrawMaze(g);
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

    private void resetMaze() {
        if (!isGenerating) {
            // Reset the maze only if not generating the maze.
            maze = null;
            mazeExists = false;
            repaint();
        }
    }

    void makeMaze() {
        // Create a random maze. The strategy is to start with
        // a grid of disconnected "rooms" separated by walls.
        // then look at each of the separating walls, in a random
        // order. If tearing down a wall would not create a loop
        // in the maze, then tear it down. Otherwise, leave it in place.
        if (maze == null)
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

    boolean solveMaze(int row, int col) {
        // Try to solve the maze by continuing current path from position
        // (row,col). Return true if a solution is found. The maze is
        // considered to be solved if the path reaches the lower right cell.
        if (maze[row][col] == emptyCode) {
            maze[row][col] = pathCode; // add this cell to the path
            repaint();
            if (row == rows - 2 && col == columns - 2)
                return true; // path has reached goal
            try {
                Thread.sleep(speedSleep);
            } catch (InterruptedException e) {
            }
            if (solveMaze(row - 1, col) || // try to solve maze by extending path
                    solveMaze(row, col - 1) || // in each possible direction
                    solveMaze(row + 1, col) ||
                    solveMaze(row, col + 1))
                return true;
            // maze can't be solved from this cell, so backtrack out of the cell
            maze[row][col] = visitedCode; // mark cell as having been visited
            repaint();
            synchronized (this) {
                try {
                    wait(speedSleep);
                } catch (InterruptedException e) {
                }
            }
        }
        return false;
    }



    static class Point {
        int row, col;

        Point(int row, int col) {
            this.row = row;
            this.col = col;
        }
    }

    public static boolean BFS(int[][] maze) {
        // Maze maze1=new Maze();
        int numRows = maze.length;
        int numCols = maze[0].length;
        boolean[][] visited = new boolean[numRows][numCols];

        Queue<Point> queue = new LinkedList<>();
        queue.add(new Point(0, 0));
        visited[0][0] = true;

        int[][] directions = { { -1, 0 }, { 0, -1 }, { 0, 1 }, { 1, 0 } }; // Up, Left, Right, Down

        while (!queue.isEmpty()) {
            Point current = queue.poll();

            for (int[] dir : directions) {
                int newRow = current.row + dir[0];
                int newCol = current.col + dir[1];

                if (isValid(maze, newRow, newCol, visited)) {
                    queue.add(new Point(newRow, newCol));
                    visited[newRow][newCol] = true;
                    repaint();
                    // Mark the path (optional)
                    maze[newRow][newCol] = 3; // You can change this value to any code you want for marking the path
                }

                if (newRow == numRows - 1 && newCol == numCols - 1) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isValid(int[][] maze, int row, int col, boolean[][] visited) {
        int numRows = maze.length;
        int numCols = maze[0].length;
        return (row >= 0 && row < numRows && col >= 0 && col < numCols && maze[row][col] == 2
                && !visited[row][col]);
    }
}