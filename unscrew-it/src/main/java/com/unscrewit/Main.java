package com.unscrewit;

import java.awt.*;
import javax.swing.*;


/**
 * Program entry point of the Unscrew It game.
 * <p>
 * Sets the system look and feel and creates the main game window
 * on the Event Dispatch Thread (EDT).
 * </p>
 */


public class Main {
    /**
     * Launches the game window.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        try {
            // Attempt to apply the system look and feel for consistency with the OS.
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Ignore look-and-feel setup failures (non-critical).
        }

        // Run GUI creation on the Event Dispatch Thread (EDT)
        EventQueue.invokeLater(() -> {
            GameFrame frame = new GameFrame();
            frame.setVisible(true); // Display the main game window.
        });
    }
}
