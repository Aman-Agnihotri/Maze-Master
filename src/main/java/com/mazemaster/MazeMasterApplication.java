// src/main/java/com/mazemaster/MazeMasterApplication.java
package com.mazemaster;

import com.mazemaster.controller.MazeController;
import com.mazemaster.ui.swing.SwingMazeView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

/**
 * Main application entry point for Maze Master.
 * Sets up the MVC architecture and launches the application.
 */
public class MazeMasterApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(MazeMasterApplication.class);

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
                LOGGER.error("Failed to start Maze Master", e);
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
            LOGGER.warn("Could not set system look and feel", e);
        }
    }
    
    private static void createAndShowGUI() {
        // Create MVC components
        MazeController controller = new MazeController();
        SwingMazeView view = new SwingMazeView(controller);
        
        // Show the application
        view.setVisible(true);
        SwingUtilities.invokeLater(view::showWelcomeMessageIfNeeded);
    }
}
