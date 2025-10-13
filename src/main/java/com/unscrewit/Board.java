package com.unscrewit;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 板：一个矩形区域，包含若干个螺丝.
 * <p>下标越大的板位于越上层。</p>
 */
public class Board {

    /** 该板的矩形区域. */
    public final Rectangle rect;

    /** 板上的所有螺丝. */
    public final List<Screw> screws = new ArrayList<>();

    /**
     * 用既定矩形创建一块板.
     *
     * @param r 板的矩形区域
     */
    public Board(Rectangle r) {
        this.rect = r;
    }

    /**
     * 生成一块随机板（用于关卡初始化）.
     * <p>
     * 约定：不同层的板大小/位置带有少量随机性；螺丝在板内随机分布，并留一定内边距。
     * </p>
     *
     * @param idx  层索引（0 为最底层，越大越靠上）
     * @param w    画布宽度
     * @param h    画布高度
     * @param rand 随机源
     * @return 生成的板
     */
    public static Board randomBoard(int idx, int w, int h, Random rand) {
        // 基于面板尺寸给出一个合理范围的随机矩形
        // 宽度 ~ (w/6 .. w/3)，高度 ~ (h/10 .. h/5)
        int minW = Math.max(120, w / 7);
        int maxW = Math.max(minW + 40, w / 3);
        int minH = Math.max(70, h / 12);
        int maxH = Math.max(minH + 30, h / 5);

        int bw = clamp(randRange(rand, minW, maxW), 80, w - 40);
        int bh = clamp(randRange(rand, minH, maxH), 60, h - 40);

        // 给每层一个略微不同的“偏移带”，让层与层之间更容易产生遮挡关系
        int margin = 24;
        int x = clamp(randRange(rand, margin, w - margin - bw), margin, w - margin - bw);
        int y = clamp(randRange(rand, h / 5, h - margin - bh), margin, h - margin - bh);

        Board b = new Board(new Rectangle(x, y, bw, bh));

        // 随机螺丝个数（3..6），并分布在板内（留 10px 内边距）
        int screwsCount = randRange(rand, 3, 6);
        int pad = 10;
        for (int i = 0; i < screwsCount; i++) {
            int sx = randRange(rand, x + pad, x + bw - pad);
            int sy = randRange(rand, y + pad, y + bh - pad);
            Color c = ColorUtils.colorAt(rand.nextInt(ColorUtils.COLORS.length));
            b.screws.add(new Screw(sx, sy, c));
        }
        return b;
    }

    /**
     * （可选）绘制该板（灰色底 + 边框 + 螺丝）.
     * <p>如果你在 GamePanel 里已经手绘板子，可以不调用这个方法。</p>
     *
     * @param g2 图形环境
     */
    public void draw(Graphics2D g2) {
        g2.setColor(new Color(220, 224, 230));
        g2.fill(rect);
        g2.setColor(new Color(120, 130, 140));
        g2.draw(rect);

        for (Screw s : screws) {
            s.draw(g2);
        }
    }

    /**
     * 判断该板是否覆盖某个点（常用于“上层遮挡”判断）.
     *
     * @param p 点
     * @return 是否覆盖
     */
    public boolean contains(Point p) {
        return rect.contains(p);
    }

    // ───── 小工具 ─────────────────────────────────────────────

    private static int randRange(Random r, int minInclusive, int maxInclusive) {
        if (maxInclusive <= minInclusive) {
            return minInclusive;
        } 
        return minInclusive + r.nextInt(maxInclusive - minInclusive + 1);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
