// src/main/java/com/mazemaster/ui/swing/SwingMazeView.java
package com.mazemaster.ui.swing;

import com.mazemaster.controller.MazeController;
import com.mazemaster.model.Maze;
import com.mazemaster.ui.MazeView;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import java.util.prefs.Preferences;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * Swing implementation of the MazeView interface.
 * Provides a modern, responsive user interface for the maze application.
 */
public class SwingMazeView extends JFrame implements MazeView, ActionListener {
    
    private final transient MazeController controller;
    
    // UI Components
    private MazePanel mazePanel;
    private JButton generateButton;
    private JButton solveButton;
    private JButton resetButton;
    private JButton pauseResumeButton; // New pause/resume button
    private JButton saveButton;
    private JButton loadButton;
    private JButton exportButton;
    private JButton newMazeButton;
    
    private JComboBox<String> generationAlgorithmBox;
    private JComboBox<String> solvingAlgorithmBox;
    private JTextField rowsField;
    private JTextField columnsField;
    private JLabel statusLabel;
    private JSlider speedSlider;

    // Window persistence
    private static final Preferences prefs = Preferences.userNodeForPackage(SwingMazeView.class);
    private static final String PREF_WINDOW_X = "window.x";
    private static final String PREF_WINDOW_Y = "window.y";
    private static final String PREF_WINDOW_WIDTH = "window.width";
    private static final String PREF_WINDOW_HEIGHT = "window.height";
    private static final String PREF_WINDOW_MAXIMIZED = "window.maximized";
    private static final String PREF_ZOOM_LEVEL = "maze.zoom.level";
    private static final String PREF_ANIMATION_SPEED = "animation.speed";
    
    // Color scheme
    private final Color[] mazeColors = {
        new Color(240, 240, 240), // Background
        new Color(60, 60, 60),    // Wall - dark gray
        new Color(100, 150, 255), // Path - blue
        Color.WHITE,              // Empty - white
        new Color(200, 200, 200), // Visited - light gray
        new Color(50, 200, 50),   // Start/Final path - green
        new Color(255, 100, 100)  // Goal - red
    };
    
    public SwingMazeView(MazeController controller) {
        this.controller = controller;
        controller.setView(this);
        
        initializeUI();
        setupEventHandlers();
        updateControlsFromController();
    }
    
    private void initializeUI() {
        setTitle("Maze Master - Definitive Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Create maze panel
        mazePanel = new MazePanel(mazeColors);
        JScrollPane scrollPane = new JScrollPane(mazePanel);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        add(scrollPane, BorderLayout.CENTER);
        
        // Create control panels
        add(createTopControlPanel(), BorderLayout.NORTH);
        add(createSideControlPanel(), BorderLayout.WEST);
        add(createBottomStatusPanel(), BorderLayout.SOUTH);
        
        // Initialize with current maze
        if (controller.getMaze() != null) {
            updateMaze(controller.getMaze());
        }
        
        pack();
        loadWindowSettings();
        addWindowPersistenceListeners();
    }

    private void loadWindowSettings() {
        // Default window size if no preferences exist
        int defaultWidth = 1200;
        int defaultHeight = 800;
        int defaultZoom = 12; // Default cell size
        int defaultSpeed = 25; // Default animation speed
        
        // Load saved window settings
        int x = prefs.getInt(PREF_WINDOW_X, -1);
        int y = prefs.getInt(PREF_WINDOW_Y, -1);
        int width = prefs.getInt(PREF_WINDOW_WIDTH, defaultWidth);
        int height = prefs.getInt(PREF_WINDOW_HEIGHT, defaultHeight);
        boolean wasMaximized = prefs.getBoolean(PREF_WINDOW_MAXIMIZED, false);
        int zoomLevel = prefs.getInt(PREF_ZOOM_LEVEL, defaultZoom);
        int animationSpeed = prefs.getInt(PREF_ANIMATION_SPEED, defaultSpeed);
        
        // Set window size
        setSize(width, height);
        
        // Set window position (center if no saved position)
        if (x >= 0 && y >= 0) {
            setLocation(x, y);
        } else {
            setLocationRelativeTo(null); // Center on screen
        }
        
        // Set maximized state
        if (wasMaximized) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        // Set zoom level
        if (mazePanel != null) {
            mazePanel.setCellSize(zoomLevel);
        }

        // Set animation speed
        if (speedSlider != null) {
            speedSlider.setValue(animationSpeed);
            controller.setAnimationSpeed(animationSpeed);
        }
    }

    private void addWindowPersistenceListeners() {
        // Save window settings when moved or resized
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                saveWindowSettings();
            }
            
            @Override
            public void componentMoved(ComponentEvent e) {
                saveWindowSettings();
            }
        });
        
        // Save maximized state changes
        addPropertyChangeListener("extendedState", evt -> saveWindowSettings());
    }

    public void saveWindowSettings() {
        // Don't save if window is maximized (save the restored size instead)
        if (getExtendedState() != JFrame.MAXIMIZED_BOTH) {
            prefs.putInt(PREF_WINDOW_X, getX());
            prefs.putInt(PREF_WINDOW_Y, getY());
            prefs.putInt(PREF_WINDOW_WIDTH, getWidth());
            prefs.putInt(PREF_WINDOW_HEIGHT, getHeight());
        }
        
        // Always save maximized state
        prefs.putBoolean(PREF_WINDOW_MAXIMIZED, getExtendedState() == JFrame.MAXIMIZED_BOTH);

        // Save zoom level
        if (mazePanel != null) {
            prefs.putInt(PREF_ZOOM_LEVEL, mazePanel.getCellSize());
        }

        // Save animation speed
        if (speedSlider != null) {
            prefs.putInt(PREF_ANIMATION_SPEED, speedSlider.getValue());
        }
    }
    
    private JPanel createTopControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEtchedBorder());
        
        // Main action buttons
        newMazeButton = createButton("New Maze", "Create a new maze with custom dimensions");
        generateButton = createButton("Generate", "Generate a new maze using selected algorithm");
        solveButton = createButton("Solve", "Solve the current maze");
        pauseResumeButton = createButton("Pause", "Pause or resume current operation");
        resetButton = createButton("Reset", "Context-aware reset: clears solution if maze is generated, or resets to blank if not");
        
        // Initially disable pause button
        pauseResumeButton.setEnabled(false);
        
        panel.add(newMazeButton);
        panel.add(Box.createHorizontalStrut(10));
        panel.add(generateButton);
        panel.add(solveButton);
        panel.add(pauseResumeButton);
        panel.add(resetButton);
        panel.add(Box.createHorizontalStrut(20));
        
        // File operations
        saveButton = createButton("Save", "Save current maze to file");
        loadButton = createButton("Load", "Load maze from file");
        exportButton = createButton("Export", "Export maze as image");
        
        panel.add(saveButton);
        panel.add(loadButton);
        panel.add(exportButton);
        
        return panel;
    }
    
    private JPanel createSideControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Settings"));
        panel.setPreferredSize(new Dimension(250, 0));
        
        // Algorithm selection
        panel.add(createAlgorithmPanel());
        panel.add(Box.createVerticalStrut(10));
        
        // Maze dimensions
        panel.add(createDimensionsPanel());
        panel.add(Box.createVerticalStrut(10));
        
        // Speed control
        panel.add(createSpeedPanel());
        
        return panel;
    }
    
    private JPanel createAlgorithmPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        panel.setBorder(BorderFactory.createTitledBorder("Algorithms"));
        
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Generation algorithm
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Generation:"), gbc);
        
        generationAlgorithmBox = new JComboBox<>();
        generationAlgorithmBox.addActionListener(this);
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(generationAlgorithmBox, gbc);
        
        // Solving algorithm
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Solving:"), gbc);
        
        solvingAlgorithmBox = new JComboBox<>();
        solvingAlgorithmBox.addActionListener(this);
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(solvingAlgorithmBox, gbc);
        
        return panel;
    }
    
    private JPanel createDimensionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        panel.setBorder(BorderFactory.createTitledBorder("Dimensions"));
        
        gbc.insets = new Insets(5, 5, 5, 5);
        
        // Rows
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Rows:"), gbc);
        
        rowsField = new JTextField("41", 8);
        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(rowsField, gbc);
        
        // Columns
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Columns:"), gbc);
        
        columnsField = new JTextField("51", 8);
        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(columnsField, gbc);
        
        return panel;
    }
    
    private JPanel createSpeedPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Animation Speed"));

        speedSlider = new JSlider(JSlider.VERTICAL, 1, 100, 30);
        speedSlider.setMajorTickSpacing(25);
        speedSlider.setMinorTickSpacing(5);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        speedSlider.setInverted(true); // Higher position = faster
        speedSlider.addChangeListener(e -> {
            int speed = speedSlider.getValue();
            controller.setAnimationSpeed(speed);
            saveWindowSettings();
        });
        
        panel.add(new JLabel("Fast", JLabel.CENTER), BorderLayout.NORTH);
        panel.add(speedSlider, BorderLayout.CENTER);
        panel.add(new JLabel("Slow", JLabel.CENTER), BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createBottomStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        panel.add(statusLabel, BorderLayout.WEST);
        
        return panel;
    }
    
    private JButton createButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.addActionListener(this);
        button.setFocusPainted(false);
        return button;
    }
    
    private void setupEventHandlers() {
        // Window closing
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                saveWindowSettings();
                controller.shutdown();
                System.exit(0);
            }
        });
    }
    
    private void updateControlsFromController() {
        // Update algorithm lists
        updateGenerationAlgorithms(controller.getAvailableGenerationAlgorithms());
        updateSolvingAlgorithms(controller.getAvailableSolvingAlgorithms());
        
        // Set current selections
        setSelectedGenerationAlgorithm(controller.getCurrentGenerationAlgorithm());
        setSelectedSolvingAlgorithm(controller.getCurrentSolvingAlgorithm());
        
        // Update control states
        updateControlsState(controller.isGenerating(), controller.isSolving());
    }
    
    // =========================
    // MazeView Interface Implementation
    // =========================
    
    @Override
    public void updateMaze(Maze maze) {
        if (mazePanel != null) {
            mazePanel.setMaze(maze);
            
            // Update dimensions fields
            if (maze != null) {
                SwingUtilities.invokeLater(() -> {
                    rowsField.setText(String.valueOf(maze.getRows()));
                    columnsField.setText(String.valueOf(maze.getColumns()));
                });
            }
        }
    }
    
    @Override
    public void refresh() {
        if (mazePanel != null) {
            SwingUtilities.invokeLater(() -> mazePanel.repaint());
        }
    }
    
    @Override
    public void onCellChanged(int row, int col, int newValue) {
        // Cell changes are handled through refresh() for performance
        refresh();
    }
    
    @Override
    public void onGenerationStarted() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Generating maze...");
            pauseResumeButton.setText("Pause");
            pauseResumeButton.setEnabled(true);
            updateControlsState(true, false);
        });
    }
    
    @Override
    public void onGenerationCompleted() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Maze generation completed");
            pauseResumeButton.setText("Pause");
            pauseResumeButton.setEnabled(false);
            updateControlsState(false, false);
        });
    }
    
    @Override
    public void onSolvingStarted() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Solving maze...");
            pauseResumeButton.setText("Pause");
            pauseResumeButton.setEnabled(true);
            updateControlsState(false, true);
        });
    }
    
    @Override
    public void onSolvingCompleted(boolean solved) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(solved ? "Maze solved!" : "No solution found");
            pauseResumeButton.setText("Pause");
            pauseResumeButton.setEnabled(false);
            updateControlsState(false, false);
        });
    }
    
    @Override
    public void onPathFound(List<Point> path) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("Solution found! Path length: " + path.size()));
    }
    
    @Override
    public void onOperationPaused() {
        SwingUtilities.invokeLater(() -> {
            pauseResumeButton.setText("Resume");
            if (controller.isGenerating()) {
                statusLabel.setText("Maze generation paused");
            } else if (controller.isSolving()) {
                statusLabel.setText("Maze solving paused");
            }
            
            // Update controls state to enable reset button during pause
            updateControlsState(controller.isGenerating(), controller.isSolving());
        });
    }
    
    @Override
    public void onOperationResumed() {
        SwingUtilities.invokeLater(() -> {
            pauseResumeButton.setText("Pause");
            if (controller.isGenerating()) {
                statusLabel.setText("Generating maze...");
            } else if (controller.isSolving()) {
                statusLabel.setText("Solving maze...");
            }
            
            // Update controls state to disable reset button during active operation
            updateControlsState(controller.isGenerating(), controller.isSolving());
        });
    }
    
    @Override
    public void showMessage(String message, boolean isError) {
        SwingUtilities.invokeLater(() -> {
            int messageType = isError ? JOptionPane.ERROR_MESSAGE : JOptionPane.INFORMATION_MESSAGE;
            JOptionPane.showMessageDialog(this, message, 
                isError ? "Error" : "Information", messageType);
        });
    }
    
    @Override
    public int[] getMazeDimensions() {
        try {
            int rows = Integer.parseInt(rowsField.getText().trim());
            int cols = Integer.parseInt(columnsField.getText().trim());
            
            if (rows < 5 || cols < 5) {
                showMessage("Dimensions must be at least 5x5", true);
                return new int[0];
            }
            
            if (rows > 200 || cols > 200) {
                showMessage("Dimensions too large (max 200x200)", true);
                return new int[0];
            }
            
            return new int[]{rows, cols};
        } catch (NumberFormatException e) {
            showMessage("Invalid dimensions. Please enter valid numbers.", true);
            return new int[0];
        }
    }
    
    @Override
    public void showProgress(boolean show, String message) {
        SwingUtilities.invokeLater(() -> {
            if (show && message != null) {
                statusLabel.setText(message);
            }
        });
    }
    
    @Override
    public void updateGenerationAlgorithms(Set<String> algorithms) {
        SwingUtilities.invokeLater(() -> {
            generationAlgorithmBox.removeAllItems();
            algorithms.forEach(generationAlgorithmBox::addItem);
        });
    }
    
    @Override
    public void updateSolvingAlgorithms(Set<String> algorithms) {
        SwingUtilities.invokeLater(() -> {
            solvingAlgorithmBox.removeAllItems();
            algorithms.forEach(solvingAlgorithmBox::addItem);
        });
    }
    
    @Override
    public void setSelectedGenerationAlgorithm(String algorithm) {
        SwingUtilities.invokeLater(() -> generationAlgorithmBox.setSelectedItem(algorithm));
    }
    
    @Override
    public void setSelectedSolvingAlgorithm(String algorithm) {
        SwingUtilities.invokeLater(() -> solvingAlgorithmBox.setSelectedItem(algorithm));
    }
    
    @Override
    public void updateControlsState(boolean isGenerating, boolean isSolving) {
        SwingUtilities.invokeLater(() -> {
            boolean isBusy = isGenerating || isSolving;
            boolean isGenerationPaused = controller.isGenerationPaused();
            boolean isSolvingPaused = controller.isSolvingPaused();
            boolean isAnyPaused = isGenerationPaused || isSolvingPaused;
            
            newMazeButton.setEnabled(!isBusy);
            generateButton.setEnabled(!isBusy);
            solveButton.setEnabled(!isBusy && controller.getMaze() != null);
            saveButton.setEnabled(!isBusy && controller.getMaze() != null);
            loadButton.setEnabled(!isBusy);
            exportButton.setEnabled(!isBusy && controller.getMaze() != null);
            
            generationAlgorithmBox.setEnabled(!isBusy);
            solvingAlgorithmBox.setEnabled(!isBusy);
            rowsField.setEnabled(!isBusy);
            columnsField.setEnabled(!isBusy);
            
            // Reset button logic based on new requirements:
            // - Disabled during running operations
            // - Enabled during paused states
            // - Enabled when idle with maze
            boolean resetEnabled = false;
            if (controller.getMaze() != null) {
                if (!isBusy) {
                    // Idle state - always enabled
                    resetEnabled = true;
                } else if (isAnyPaused) {
                    // Paused state - enabled
                    resetEnabled = true;
                } else {
                    // Running state - disabled
                    resetEnabled = false;
                }
            }
            resetButton.setEnabled(resetEnabled);
            
            // Pause button is enabled only when busy
            pauseResumeButton.setEnabled(isBusy);
            
            // Update pause button text based on pause state
            if (isBusy) {
                if (isAnyPaused) {
                    pauseResumeButton.setText("Resume");
                } else {
                    pauseResumeButton.setText("Pause");
                }
            } else {
                pauseResumeButton.setText("Pause");
            }
        });
    }
    
    @Override
    public boolean exportToImage(String filename) {
        if (mazePanel == null || controller.getMaze() == null) {
            return false;
        }
        
        try {
            Dimension size = mazePanel.getPreferredSize();
            BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = image.createGraphics();
            
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            mazePanel.paint(g2d);
            g2d.dispose();
            
            File file = new File(filename);
            String format = filename.toLowerCase().endsWith(".png") ? "PNG" : "JPEG";
            
            return ImageIO.write(image, format, file);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public String getExportFilename() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Maze");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("JPEG Images", "jpg", "jpeg"));
        fileChooser.setSelectedFile(new File("maze.png"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile().getAbsolutePath();
        }
        
        return null;
    }
    
    // =========================
    // ActionListener Implementation
    // =========================
    
    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        
        if (source == newMazeButton) {
            int[] dimensions = getMazeDimensions();
            if (dimensions.length == 2) { // Check for valid dimensions array
                controller.createNewMaze(dimensions[0], dimensions[1]);
                statusLabel.setText("New maze created");
            }
        } else if (source == generateButton) {
            controller.generateMaze();
        } else if (source == solveButton) {
            controller.solveMaze();
        } else if (source == pauseResumeButton) {
            controller.pauseResumeCurrentOperation();
        } else if (source == resetButton) {
            boolean wasGenerationPaused = controller.isGenerating() && controller.isGenerationPaused();
            boolean wasSolvingPaused = controller.isSolving() && controller.isSolvingPaused();
            boolean isMazeGenerated = controller.isMazeFullyGenerated();
            
            if (wasGenerationPaused) {
                statusLabel.setText("Stopping paused generation and resetting to blank maze...");
            } else if (wasSolvingPaused || (!controller.isBusy() && isMazeGenerated)) {
                statusLabel.setText("Clearing solution and keeping generated maze...");
            } else {
                statusLabel.setText("Resetting to blank maze...");
            }
            
            controller.resetMaze();
            
            if (wasGenerationPaused) {
                statusLabel.setText("Maze reset - ready for new generation");
            } else if (wasSolvingPaused || (!controller.isBusy() && isMazeGenerated)) {
                statusLabel.setText("Solution cleared - maze ready for solving");
            } else {
                statusLabel.setText("Maze reset to blank state");
            }
        } else if (source == saveButton) {
            controller.saveMaze();
        } else if (source == loadButton) {
            controller.loadMaze();
        } else if (source == exportButton) {
            String filename = getExportFilename();
            if (filename != null) {
                if (exportToImage(filename)) {
                    statusLabel.setText("Maze exported to " + filename);
                } else {
                    showMessage("Failed to export maze", true);
                }
            }
        } else if (source == generationAlgorithmBox) {
            String selected = (String) generationAlgorithmBox.getSelectedItem();
            if (selected != null) {
                controller.setGenerationAlgorithm(selected);
            }
        } else if (source == solvingAlgorithmBox) {
            String selected = (String) solvingAlgorithmBox.getSelectedItem();
            if (selected != null) {
                controller.setSolvingAlgorithm(selected);
            }
        }
    }
}