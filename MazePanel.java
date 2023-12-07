import javax.swing.*;
import java.awt.*;

public class MazePanel extends JPanel {

    private MazeApp maze;

    public MazePanel(MazeApp maze) {
        this.maze = maze;

        setBackground(maze.color[MazeApp.backgroundCode]);
        setPreferredSize(new Dimension(maze.blockSize * MazeApp.columns, maze.blockSize * MazeApp.rows));
        
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        maze.checkSize();
        maze.redrawMaze(g);
    }
}
