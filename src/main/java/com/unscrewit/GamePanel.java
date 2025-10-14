package com.unscrewit;

import com.unscrewit.logic.GameController;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;

/**
 * 舞台画布：绘制棋盘格背景与所有板，并接收点击交给控制器处理.
 */
public class GamePanel extends JPanel {

    /** 关卡状态. */
    private transient LevelState levelState;

    /** 控制器. */
    private transient GameController controller;

    /**
     * 创建画布并设置鼠标监听.
     */
    public GamePanel() {
        setBackground(new Color(245, 246, 248));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (controller != null) {
                    controller.handleClick(e.getPoint());
                    repaint();
                }
            }
        });
    }

    /**
     * 设置关卡状态.
     *
     * @param state 关卡状态对象.
     */
    public void setLevelState(LevelState state) {
        this.levelState = state;
    }

    /**
     * 设置控制器.
     *
     * @param c 控制器实例.
     */
    public void setController(GameController c) {
        this.controller = c;
    }

    /**
     * 绘制棋盘格背景并叠加绘制所有板及其螺丝.
     *
     * @param g 绘图环境.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final int size = 24;
        for (int y = 0; y < getHeight(); y += size) {
            for (int x = 0; x < getWidth(); x += size) {
                boolean dark = ((x / size) + (y / size)) % 2 == 0;
                g.setColor(dark ? new Color(230, 233, 238) : new Color(240, 242, 246));
                g.fillRect(x, y, size, size);
            }
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(new Color(0, 0, 0, 60));
        g2.setFont(g2.getFont().deriveFont(18f));
        g2.drawString("Unscrew It! – Stage 1 · Step B3 (click enabled)", 16, 28);

        if (levelState != null) {
            for (Board b : levelState.boards()) {
                b.draw(g2);
            }
            // HUD：显示目标与缓冲区状态.
            String hud = String.format(
                "Targets: L=%s (%d/%d), R=%s (%d/%d) | Buffer=%d/%d",
                levelState.targets().leftColor(),
                levelState.targets().leftCount(),
                com.unscrewit.rules.Rules.TARGET_SLOTS_PER_COLOR,
                levelState.targets().rightColor(),
                levelState.targets().rightCount(),
                com.unscrewit.rules.Rules.TARGET_SLOTS_PER_COLOR,
                levelState.buffer().size(),
                com.unscrewit.rules.Rules.BUFFER_CAPACITY
            );
            g2.setFont(g2.getFont().deriveFont(13f));
            g2.setColor(new Color(0, 0, 0, 150));
            g2.drawString(hud, 16, getHeight() - 12);
        }
        g2.dispose();
    }
}
