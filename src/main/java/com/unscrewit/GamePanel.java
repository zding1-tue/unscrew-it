package com.unscrewit;

import java.awt.*;
import javax.swing.*;


/**
 * 舞台画布：暂时只绘制棋盘格背景，用于验证绘图循环正常.
 */
public class GamePanel extends JPanel {

    public GamePanel() {
        setBackground(new Color(245, 246, 248)); // 浅灰背景
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final int size = 24;
        for (int y = 0; y < getHeight(); y += size) {
            for (int x = 0; x < getWidth(); x += size) {
                boolean dark = ((x / size) + (y / size)) % 2 == 0;
                g.setColor(dark ? new Color(230, 233, 238)
                                : new Color(240, 242, 246));
                g.fillRect(x, y, size, size);
            }
        }
        g.setColor(new Color(0, 0, 0, 60));
        g.setFont(g.getFont().deriveFont(Font.BOLD, 18f));
        g.drawString("Unscrew It! – Stage 1 · Step A", 16, 28);
    }
}

