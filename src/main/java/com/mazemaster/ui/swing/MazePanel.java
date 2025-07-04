// src/main/java/com/mazemaster/ui/swing/MazePanel.java
package com.mazemaster.ui.swing;

import com.mazemaster.model.Maze;
import javax.swing.*;
import java.awt.*;

/**
 * Custom JPanel responsible for rendering the maze.
 * Handles efficient drawing and scaling of the maze grid.
 */
public class MazePanel extends JPanel {
    
    private Maze maze;
    private final Color[] colors;
    private int cellSize = 12;
    private static final int minCellSize = 2;
    private static final int maxCellSize = 50;
    
    // Rendering settings
    private boolean antialiasing = true;
    private boolean showGrid = false;
    
    public MazePanel(Color[] colors) {
        this.colors = colors.clone();
        setBackground(colors[Maze.BACKGROUND]);
        setOpaque(true);
        
        // Add mouse wheel listener for zooming
        addMouseWheelListener(e -> {
            int rotation = e.getWheelRotation();
            if (rotation < 0) {
                zoomIn();
            } else {
                zoomOut();
            }
        });
    }
    
    public void setMaze(Maze maze) {
        this.maze = maze;
        updatePreferredSize();
        revalidate();
        repaint();
    }
    
    public void setCellSize(int cellSize) {
        this.cellSize = Math.max(minCellSize, Math.min(maxCellSize, cellSize));
        updatePreferredSize();
        revalidate();
        repaint();

        // Notify parent to save zoom level
        notifyZoomChanged();
    }

    private void notifyZoomChanged() {
    // Find the parent SwingMazeView and save settings
    Container parent = getParent();
    while (parent != null && !(parent instanceof SwingMazeView)) {
        parent = parent.getParent();
    }
    if (parent instanceof SwingMazeView swingMazeView) {
        swingMazeView.saveWindowSettings();
    }
}
    
    public void zoomIn() {
        if (cellSize < maxCellSize) {
            setCellSize(cellSize + 1);
        }
    }
    
    public void zoomOut() {
        if (cellSize > minCellSize) {
            setCellSize(cellSize - 1);
        }
    }
    
    public void setAntialiasing(boolean antialiasing) {
        this.antialiasing = antialiasing;
        repaint();
    }
    
    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
        repaint();
    }
    
    private void updatePreferredSize() {
        if (maze != null) {
            int width = maze.getColumns() * cellSize;
            int height = maze.getRows() * cellSize;
            setPreferredSize(new Dimension(width, height));
        }
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (maze == null) {
            drawEmptyState(g);
            return;
        }
        
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Set rendering hints
        if (antialiasing) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }
        
        drawMaze(g2d);
        
        if (showGrid && cellSize >= 8) {
            drawGrid(g2d);
        }
        
        g2d.dispose();
    }
    
    private void drawEmptyState(Graphics g) {
        g.setColor(Color.GRAY);
        String message = "No maze loaded";
        FontMetrics fm = g.getFontMetrics();
        int x = (getWidth() - fm.stringWidth(message)) / 2;
        int y = getHeight() / 2;
        g.drawString(message, x, y);
    }
    
    private void drawMaze(Graphics2D g2d) {
        int rows = maze.getRows();
        int cols = maze.getColumns();
        
        // Calculate visible area for performance optimization
        Rectangle clipBounds = g2d.getClipBounds();
        int startRow = 0;
        int endRow = rows;
        int startCol = 0;
        int endCol = cols;
        
        if (clipBounds != null) {
            startRow = Math.max(0, clipBounds.y / cellSize);
            endRow = Math.min(rows, (clipBounds.y + clipBounds.height) / cellSize + 1);
            startCol = Math.max(0, clipBounds.x / cellSize);
            endCol = Math.min(cols, (clipBounds.x + clipBounds.width) / cellSize + 1);
        }
        
        // Draw cells
        for (int row = startRow; row < endRow; row++) {
            for (int col = startCol; col < endCol; col++) {
                drawCell(g2d, row, col);
            }
        }
        
        // Highlight start and goal positions if visible
        if (cellSize >= 6) {
            highlightSpecialCells(g2d);
        }
    }
    
    private void drawCell(Graphics2D g2d, int row, int col) {
        int cellType = maze.getCell(row, col);
        
        // Handle negative values (during generation)
        if (cellType < 0) {
            cellType = Maze.EMPTY;
        }
        
        // Ensure valid color index
        if (cellType >= 0 && cellType < colors.length) {
            g2d.setColor(colors[cellType]);
        } else {
            g2d.setColor(colors[Maze.WALL]); // Default to wall color
        }
        
        int x = col * cellSize;
        int y = row * cellSize;
        
        g2d.fillRect(x, y, cellSize, cellSize);
        
        // Add subtle border for larger cells
        if (cellSize >= 10 && cellType != Maze.WALL) {
            g2d.setColor(g2d.getColor().darker());
            g2d.drawRect(x, y, cellSize - 1, cellSize - 1);
        }
    }
    
    private void highlightSpecialCells(Graphics2D g2d) {
        // Highlight start position
        int startRow = maze.getStartRow();
        int startCol = maze.getStartCol();
        drawSpecialMarker(g2d, startRow, startCol, Color.GREEN, "S");
        
        // Highlight goal position
        int goalRow = maze.getGoalRow();
        int goalCol = maze.getGoalCol();
        drawSpecialMarker(g2d, goalRow, goalCol, Color.RED, "G");
    }
    
    private void drawSpecialMarker(Graphics2D g2d, int row, int col, Color color, String text) {
        int x = col * cellSize;
        int y = row * cellSize;
        
        // Draw colored circle
        g2d.setColor(color);
        int margin = Math.max(1, cellSize / 6);
        g2d.fillOval(x + margin, y + margin, cellSize - 2 * margin, cellSize - 2 * margin);
        
        // Draw text if cell is large enough
        if (cellSize >= 16) {
            g2d.setColor(Color.WHITE);
            Font font = new Font(Font.SANS_SERIF, Font.BOLD, Math.max(8, cellSize / 2));
            g2d.setFont(font);
            
            FontMetrics fm = g2d.getFontMetrics();
            int textX = x + (cellSize - fm.stringWidth(text)) / 2;
            int textY = y + (cellSize + fm.getAscent()) / 2;
            
            g2d.drawString(text, textX, textY);
        }
    }
    
    private void drawGrid(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 50)); // Semi-transparent black
        g2d.setStroke(new BasicStroke(1));
        
        int rows = maze.getRows();
        int cols = maze.getColumns();
        
        // Draw vertical lines
        for (int col = 0; col <= cols; col++) {
            int x = col * cellSize;
            g2d.drawLine(x, 0, x, rows * cellSize);
        }
        
        // Draw horizontal lines
        for (int row = 0; row <= rows; row++) {
            int y = row * cellSize;
            g2d.drawLine(0, y, cols * cellSize, y);
        }
    }
    
    // Utility methods for external access
    public Point getCellAt(Point screenPoint) {
        if (maze == null) return null;
        
        int col = screenPoint.x / cellSize;
        int row = screenPoint.y / cellSize;
        
        if (maze.isValidPosition(row, col)) {
            return new Point(row, col);
        }
        
        return null;
    }
    
    public Rectangle getCellBounds(int row, int col) {
        return new Rectangle(col * cellSize, row * cellSize, cellSize, cellSize);
    }
    
    public int getCellSize() {
        return cellSize;
    }
    
    public Maze getMaze() {
        return maze;
    }
    
    @Override
    public String getToolTipText(java.awt.event.MouseEvent event) {
        Point cell = getCellAt(event.getPoint());
        if (cell != null && maze != null) {
            int cellType = maze.getCell(cell.x, cell.y);
            String typeName = getCellTypeName(cellType);
            return String.format("Cell (%d, %d): %s", cell.x, cell.y, typeName);
        }
        return null;
    }
    
    private String getCellTypeName(int cellType) {
        switch (cellType) {
            case Maze.WALL: return "Wall";
            case Maze.EMPTY: return "Empty";
            case Maze.PATH: return "Path";
            case Maze.VISITED: return "Visited";
            case Maze.START: return "Start/Solution";
            case Maze.GOAL: return "Goal";
            default: return cellType < 0 ? "Room " + (-cellType) : "Unknown";
        }
    }
}