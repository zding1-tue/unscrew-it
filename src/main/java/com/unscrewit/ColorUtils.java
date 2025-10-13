package com.unscrewit;

import java.awt.Color;

/**
 * 颜色工具：提供可选的螺丝颜色集合.
 */
public final class ColorUtils {

    private ColorUtils() {
        // 工具类不应被实例化
    }

    /** 一组可用颜色（可按需增删）. */
    public static final Color[] COLORS = {
        Color.RED,
        Color.ORANGE,
        Color.YELLOW,
        Color.GREEN,
        Color.BLUE,
        new Color(128, 0, 128),   // Purple
        new Color(0, 200, 200),   // Cyan/Teal
        new Color(255, 105, 180)  // Pink
    };

    /**
     * 从预设颜色中取一个.
     *
     * @param idx 索引（会按长度取模）
     * @return 一种颜色
     */
    public static Color colorAt(int idx) {
        return COLORS[Math.floorMod(idx, COLORS.length)];
    }
}
