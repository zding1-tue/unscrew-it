package com.unscrewit;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Unscrew It! (seed: not set)");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setSize(1000, 800);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
