package com.unscrewit;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 表示一块承载若干螺丝的板子, 负责自身可见性命中与渲染逻辑.
 * 支持在被清空后淡出并从交互与渲染中移除.
 */
public class Board {

    /** 默认静态填充透明度(0~1). */
    private static final float BASE_ALPHA = 0.78f;

    /** 清空后淡出时长(毫秒). */
    private static final int FADE_OUT_MS = 250;

    /** 可见性判定采样相对偏移像素. */
    private static final int VIS_SAMPLE_OFFSET = 8;

    /** 可见性阈值: 被遮挡比例小于该值则视为可见. */
    private static final double VISIBLE_THRESHOLD = 0.5;

    /** 板体矩形区域(为兼容现有代码保持 public). */
    public final Rectangle rect;

    /** 板上的螺丝列表(为兼容现有代码保持 public). */
    public final List<Screw> screws = new ArrayList<>();

    /** 当前是否处于淡出移除过程. */
    private boolean removing = false;

    /** 是否已完全隐藏(淡出完成). */
    private boolean hidden = false;

    /** 当前透明度, 0~1. */
    private float alpha = BASE_ALPHA;

    /** 开始淡出的时间戳(纳秒), 未开始为 -1. */
    private long fadeStartNano = -1L;

    /**
     * 使用给定矩形创建板子实例.
     *
     * @param r 板体矩形区域.
     */
    public Board(final Rectangle r) {
        this.rect = r;
    }

    /**
     * 生成一个随机板子(2×3 或 2×4 布局, 先放置位置, 颜色稍后统一分配).
     *
     * @param idx 序号.
     * @param w 画布宽度.
     * @param h 画布高度.
     * @param rand 随机源.
     * @return 随机生成的板子.
     */
    public static Board randomBoard(final int idx, final int w, final int h, final Random rand) {
        final int topH = h / 4;
        final int midH = h / 8;
        final int playTop = topH + midH;
        final int slotH = 48; // 与 GamePanel.drawBuffer 一致的高度常量。
        final int bufferMargin = 28; // 与 GamePanel.drawBuffer 中的底部边距一致。
        final int yBufferTop = h - slotH - bufferMargin;
        final int playHeight = Math.max(1, yBufferTop - playTop - 20);

        final int bw = 160 + rand.nextInt(120);
        final int bh = 70 + rand.nextInt(50);

        final int x = rand.nextInt(Math.max(1, w - bw));
        final int y = playTop + rand.nextInt(Math.max(1, playHeight - bh));

        final Board b = new Board(new Rectangle(x, y, bw, bh));

        final int rows = 2;
        final int cols = 3 + rand.nextInt(2); // 3 或 4 列.

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                final int sx = x + 20 + c * (bw - 40) / Math.max(1, cols - 1);
                final int sy = y + 20 + r * (bh - 40) / Math.max(1, rows - 1);
                b.screws.add(new Screw(sx, sy, Color.GRAY)); // 颜色稍后统一分配.
            }
        }
        return b;
    }

    /**
     * 命中检测: 若点击位置落到可见螺丝上, 则返回该螺丝.
     *
     * @param p 点击位置.
     * @param above 在该板之上的板列表(用于遮挡判定).
     * @return 被命中的可见螺丝, 未命中返回 null.
     */
    public Screw getClickedScrew(final Point p, final List<Board> above) {
        if (hidden) {
            return null;
        }
        for (Screw s : screws) {
            if (s.contains(p) && isScrewVisible(s, above)) {
                return s;
            }
        }
        return null;
    }

    /**
     * 判断一颗螺丝是否可见(被上层板遮挡比例小于阈值). 采用九点采样近似判断.
     *
     * @param s 待测螺丝.
     * @param above 在该板之上的板列表.
     * @return 是否可见.
     */
    public boolean isScrewVisible(final Screw s, final List<Board> above) {
        final int o = VIS_SAMPLE_OFFSET;
        final int[][] samples = new int[][] {
            {0, 0},
            {-o, 0}, {o, 0},
            {0, -o}, {0, o},
            {-o, -o}, {-o, o}, {o, -o}, {o, o}
        };
        int covered = 0;
        for (int[] d : samples) {
            int px = s.x + d[0];
            int py = s.y + d[1];
            boolean c = false;
            for (Board b : above) {
                if (b.rect.contains(px, py) && !b.hidden) {
                    c = true;
                    break;
                }
            }
            if (c) {
                covered++;
            }
        }
        double ratio = covered / (double) samples.length;
        return ratio < VISIBLE_THRESHOLD;
    }

    /**
     * 将一颗螺丝从本板移除.
     *
     * @param s 螺丝.
     */
    public void removeScrew(final Screw s) {
        screws.remove(s);
    }

    /**
     * 当板上螺丝全部被移除时触发淡出流程.
     */
    public void startRemoving() {
        if (!removing && !hidden) {
            removing = true;
            fadeStartNano = System.nanoTime();
        }
    }

    /**
     * 返回是否已无螺丝.
     *
     * @return 是否空板.
     */
    public boolean allRemoved() {
        return screws.isEmpty();
    }

    /**
     * 返回是否已完全隐藏.
     *
     * @return 是否隐藏.
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * 绘制板子与其螺丝, 需要传入上方板列表以保证与可见性判定一致.
     *
     * @param g 图形环境.
     * @param above 在该板之上的板列表.
     */
    public void draw(final Graphics2D g, final List<Board> above) {
        if (hidden) {
            return;
        }

        // 旧版逻辑：不使用淡出动画，直接绘制。
        Composite old = g.getComposite();

        // 板体.
        g.setColor(new Color(205, 211, 217));
        g.fill(rect);
        g.setColor(new Color(90, 96, 102));
        g.draw(rect);

        // 螺丝.
        for (Screw s : screws) {
            boolean vis = isScrewVisible(s, above);
            if (vis) {
                s.draw(g);
                g.setStroke(new BasicStroke(2.2f));
                g.setColor(new Color(255, 255, 255, 200));
                int r = 22;
                g.drawOval(s.x - r / 2, s.y - r / 2, r, r);
                g.setStroke(new BasicStroke(1f));
            } else {
                int size = Screw.size();
                int x = s.x - size / 2;
                int y = s.y - size / 2;
                g.setColor(new Color(90, 90, 90, 120));
                g.fillOval(x, y, size, size);
                g.setColor(new Color(40, 40, 40, 180));
                g.drawOval(x, y, size, size);
                g.drawLine(x + 4, y + 4, x + size - 4, y + size - 4);
                g.drawLine(x + size - 4, y + 4, x + 4, y + size - 4);
            }
        }

        g.setComposite(old);

        // 左上角显示剩余颗数.
        g.setColor(Color.BLACK);
        g.drawString(String.valueOf(screws.size()), rect.x + 4, rect.y + 14);
    }

    /**
     * 简化重载: 当无需遮挡信息时调用.
     *
     * @param g 图形环境.
     */
    public void draw(final Graphics2D g) {
        draw(g, Collections.emptyList());
    }

    /**
     * 更新淡出进度并在结束时标记为隐藏.
     */
    private void updateFadeProgress() {
        if (!removing) {
            return;
        }
        if (fadeStartNano < 0L) {
            fadeStartNano = System.nanoTime();
        }
        long now = System.nanoTime();
        double ms = (now - fadeStartNano) / 1_000_000.0;
        double t = Math.min(1.0, ms / FADE_OUT_MS);
        alpha = (float) ((1.0 - t) * BASE_ALPHA);
        if (t >= 1.0) {
            hidden = true;
        }
    }
}
