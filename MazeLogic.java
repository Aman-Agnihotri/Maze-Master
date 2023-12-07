import javax.swing.*;
import java.awt.*;

public class MazeLogic extends JPanel {

    private MazeApp maze;

    public MazeLogic(MazeApp maze) {
        this.maze = maze;
        setBackground(maze.color[MazeApp.backgroundCode]);

        setPreferredSize(new Dimension(maze.blockSize * maze.columns, maze.blockSize * maze.rows));
        
    }

    @Override
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        maze.checkSize();
        maze.redrawMaze(g);
    }
}
