package com.unscrewit;

import com.unscrewit.logic.GameController;
import com.unscrewit.model.TargetColor;
import com.unscrewit.rules.Rules;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.JPanel;

/**
 * 舞台画布：绘制棋盘格背景、板与螺丝，以及目标区与缓冲区的可视化.
 */
public class GamePanel extends JPanel {

    /** 关卡状态. */
    private transient LevelState levelState;

    /** 控制器. */
    private transient GameController controller;

    /** 顶部目标区外框的高度（像素）. */
    private static final int TARGET_BAR_H = 56;

    /** 底部缓冲区外框的高度（像素）. */
    private static final int BUFFER_BAR_H = 56;

    /** 外框圆角半径（像素）. */
    private static final int CORNER_R = 10;

    /** 槽位圆点直径（像素）. */
    private static final int DOT_D = 18;

    /** 外框与窗口边缘的安全边距（像素）. */
    private static final int MARGIN = 16;

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
     * 绘制棋盘格背景、标题、板与螺丝、目标区和缓冲区.
     *
     * @param g 绘图环境.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawCheckerboard(g2);
        drawTitle(g2);

        if (levelState != null) {
            // 先画目标/缓冲的 UI 背板，再画板与螺丝，避免 UI 被覆盖.
            drawTargetsUI(g2);
            drawBufferUI(g2);

            // 绘制所有板与其上的螺丝（下层先画，上层后画）.
            for (Board b : levelState.boards()) {
                b.draw(g2);
            }
        }

        g2.dispose();
    }

    /**
     * 绘制棋盘格背景.
     *
     * @param g2 图形环境.
     */
    private void drawCheckerboard(Graphics2D g2) {
        final int size = 24;
        for (int y = 0; y < getHeight(); y += size) {
            for (int x = 0; x < getWidth(); x += size) {
                boolean dark = ((x / size) + (y / size)) % 2 == 0;
                g2.setColor(dark ? new Color(230, 233, 238) : new Color(240, 242, 246));
                g2.fillRect(x, y, size, size);
            }
        }
    }

    /**
     * 绘制页面标题.
     *
     * @param g2 图形环境.
     */
    private void drawTitle(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 60));
        g2.setFont(g2.getFont().deriveFont(18f));
        g2.drawString("Unscrew It! – Stage 1 · Step B4 (targets & buffer UI)", 16, 28);
    }

    /**
     * 绘制目标区 UI（顶部两个目标，每个 3 槽）.
     *
     * @param g2 图形环境.
     */
    private void drawTargetsUI(Graphics2D g2) {
        if (levelState == null) {
            return;
        }
        Rectangle bar = new Rectangle(MARGIN, MARGIN + 28, getWidth() - 2 * MARGIN, TARGET_BAR_H);
        // 背板.
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillRoundRect(bar.x, bar.y, bar.width, bar.height, CORNER_R, CORNER_R);
        g2.setColor(new Color(120, 130, 140, 200));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(bar.x, bar.y, bar.width, bar.height, CORNER_R, CORNER_R);

        // 左右目标区域各占一半.
        int halfW = bar.width / 2;
        Rectangle left = new Rectangle(bar.x + 10, bar.y + 10, halfW - 20, bar.height - 20);
        Rectangle right = new Rectangle(bar.x + halfW + 10, bar.y + 10, halfW - 20, bar.height - 20);

        drawTargetSlots(g2, left, levelState.targets().leftColor(), levelState.targets().leftCount());
        drawTargetSlots(g2, right, levelState.targets().rightColor(), levelState.targets().rightCount());
    }

    /**
     * 在给定矩形内绘制单个目标的 3 个槽位（按颜色与计数高亮）.
     *
     * @param g2 图形环境.
     * @param area 目标区域矩形.
     * @param color 该目标的逻辑颜色.
     * @param count 当前已填数量.
     */
    private void drawTargetSlots(Graphics2D g2, Rectangle area, TargetColor color, int count) {
        // 目标标题色条.
        Color paint = ColorUtils.colorAt(color.ordinal());
        g2.setColor(new Color(paint.getRed(), paint.getGreen(), paint.getBlue(), 200));
        g2.fillRoundRect(area.x, area.y, area.width, area.height, CORNER_R, CORNER_R);
        g2.setColor(new Color(100, 100, 110, 200));
        g2.drawRoundRect(area.x, area.y, area.width, area.height, CORNER_R, CORNER_R);

        // 3 个槽位圆点水平排列.
        int spacing = (area.width - 3 * DOT_D) / 4;
        int cx = area.x + spacing + DOT_D / 2;
        int cy = area.y + area.height / 2;

        for (int i = 0; i < Rules.TARGET_SLOTS_PER_COLOR; i++) {
            int dotX = cx - DOT_D / 2 + i * (DOT_D + spacing);
            int dotY = cy - DOT_D / 2;

            boolean filled = i < count;
            if (filled) {
                g2.setColor(Color.WHITE);
                g2.fillOval(dotX, dotY, DOT_D, DOT_D);
                g2.setColor(new Color(0, 0, 0, 120));
                g2.drawOval(dotX, dotY, DOT_D, DOT_D);
            } else {
                g2.setColor(new Color(255, 255, 255, 160));
                g2.fillOval(dotX, dotY, DOT_D, DOT_D);
                g2.setColor(new Color(0, 0, 0, 60));
                g2.drawOval(dotX, dotY, DOT_D, DOT_D);
            }
        }

        // 角落标签，如 C3.
        g2.setColor(new Color(0, 0, 0, 160));
        g2.setFont(g2.getFont().deriveFont(12f));
        String label = "C" + color.ordinal();
        g2.drawString(label, area.x + area.width - 18, area.y + area.height - 8);
    }

    /**
     * 绘制底部缓冲区 UI（按实际颜色渲染每格）.
     *
     * @param g2 图形环境.
     */
    private void drawBufferUI(Graphics2D g2) {
        if (levelState == null) {
            return;
        }
        int barY = getHeight() - BUFFER_BAR_H - MARGIN;
        Rectangle bar = new Rectangle(MARGIN, barY, getWidth() - 2 * MARGIN, BUFFER_BAR_H);

        // 背板.
        g2.setColor(new Color(255, 255, 255, 200));
        g2.fillRoundRect(bar.x, bar.y, bar.width, bar.height, CORNER_R, CORNER_R);
        g2.setColor(new Color(120, 130, 140, 200));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(bar.x, bar.y, bar.width, bar.height, CORNER_R, CORNER_R);

        // 4 个格子（根据 Buffer.snapshot() 的顺序给已用格子上色）.
        int cells = Rules.BUFFER_CAPACITY;
        int pad = 10;
        int cellW = (bar.width - pad * (cells + 1)) / cells;
        int cellH = bar.height - 2 * pad;

        List<TargetColor> snap = levelState.buffer().snapshot();
        for (int i = 0; i < cells; i++) {
            int x = bar.x + pad + i * (cellW + pad);
            int y = bar.y + pad;
            Rectangle cell = new Rectangle(x, y, cellW, cellH);

            if (i < snap.size()) {
                TargetColor c = snap.get(i);
                Color paint = ColorUtils.colorAt(c.ordinal());
                Color fill = new Color(paint.getRed(), paint.getGreen(), paint.getBlue(), 210);
                g2.setColor(fill);
                g2.fillRoundRect(cell.x, cell.y, cell.width, cell.height, CORNER_R, CORNER_R);
            } else {
                g2.setColor(new Color(245, 248, 250, 220));
                g2.fillRoundRect(cell.x, cell.y, cell.width, cell.height, CORNER_R, CORNER_R);
            }

            // 边框.
            g2.setColor(new Color(120, 130, 140, 200));
            g2.drawRoundRect(cell.x, cell.y, cell.width, cell.height, CORNER_R, CORNER_R);
        }

        // 右下角状态文字，如 Buffer=3/4.
        g2.setColor(new Color(0, 0, 0, 150));
        g2.setFont(g2.getFont().deriveFont(12f));
        String label = "Buffer=" + snap.size() + "/" + Rules.BUFFER_CAPACITY;
        g2.drawString(label, bar.x + bar.width - 92, bar.y + bar.height - 10);
    }
}
