// src/main/java/com/mazemaster/MazeMasterApplication.java
package com.mazemaster;

import com.mazemaster.controller.MazeController;
import com.mazemaster.ui.swing.SwingMazeView;

import javax.swing.*;
import javax.swing.UIManager;

/**
 * Main application entry point for Maze Master.
 * Sets up the MVC architecture and launches the application.
 */
public class MazeMasterApplication {
    
    public static void main(String[] args) {
        // Set system properties for better UI experience
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        
        // Set look and feel
        setLookAndFeel();
        
        // Launch application on EDT
        SwingUtilities.invokeLater(() -> {
            try {
                createAndShowGUI();
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, 
                    "Failed to start Maze Master: " + e.getMessage(),
                    "Startup Error", 
                    JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
    
    private static void setLookAndFeel() {
        try {
            // Try to use system look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            
            // Customize some UI properties
            UIManager.put("Button.focusPainted", false);
            UIManager.put("ToolTip.background", new java.awt.Color(255, 255, 225));
            UIManager.put("ToolTip.border", BorderFactory.createLineBorder(java.awt.Color.GRAY));
            
        } catch (Exception e) {
            // If system L&F fails, continue with default
            System.err.println("Could not set system look and feel: " + e.getMessage());
        }
    }
    
    private static void createAndShowGUI() {
        // Create MVC components
        MazeController controller = new MazeController();
        SwingMazeView view = new SwingMazeView(controller);
        
        // Show the application
        view.setVisible(true);
        
        // Optional: Show welcome message
        SwingUtilities.invokeLater(() -> {
            view.showMessage(
                """
                Welcome to Maze Master - Definitive Edition!

                Features:
                • Multiple generation algorithms
                • Advanced solving algorithms (DFS, BFS, A*)
                • Real-time visualization
                • Save/Load functionality
                • Export to images
                • Mouse wheel zooming

                Click 'Generate' to create your first maze!
                """,
                false
            );
        });
    }
}
