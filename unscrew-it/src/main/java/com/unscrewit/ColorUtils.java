package com.unscrewit;

import com.unscrewit.model.TargetColor;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;

/**
 * 颜色工具: 提供可选的螺丝颜色集合与映射方法.
 */
public final class ColorUtils {

    private ColorUtils() {
        // 工具类不应被实例化.
    }

    /** 一组可用颜色(可按需增删). */
    public static final Color[] COLORS = {
        Color.RED,
        Color.ORANGE,
        Color.YELLOW,
        Color.GREEN,
        Color.BLUE,
        new Color(128, 0, 128),   // Purple.
        new Color(0, 200, 200),   // Cyan/Teal.
        new Color(255, 105, 180)  // Pink.
    };

    /**
     * 从预设颜色中取一个.
     *
     * @param idx 索引(会按长度取模).
     * @return 一种颜色.
     */
    public static Color colorAt(final int idx) {
        return COLORS[Math.floorMod(idx, COLORS.length)];
    }

    /**
     * 将逻辑颜色映射为实际 RGB 颜色.
     *
     * @param tc 逻辑颜色.
     * @return 实际颜色.
     */
    public static Color fromTargetColor(final TargetColor tc) {
        return COLORS[tc.ordinal() % COLORS.length];
    }

    /**
     * 返回可用调色板列表.
     *
     * @return 可用颜色列表.
     */
    public static List<Color> palette() {
        return Arrays.asList(COLORS);
    }

    /**
     * 与 {@link #palette()} 等价的别名方法.
     *
     * @return 可用颜色列表.
     */
    public static List<Color> getPalette() {
        return palette();
    }
}
