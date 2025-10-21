package com.unscrewit.model;

import com.unscrewit.ColorUtils;
import java.awt.Color;

/**
 * 颜色映射工具：在渲染颜色 {@link java.awt.Color} 与逻辑颜色 {@link TargetColor} 之间建立索引映射关系.
 * <p>约定：{@code ColorUtils.COLORS[i]} 与 {@code TargetColor.values()[i]} 一一对应.</p>
 */
public final class ColorMapping {

    private ColorMapping() {
        // 工具类不应被实例化.
    }

    /**
     * 将渲染颜色映射为逻辑颜色.
     *
     * @param color 渲染颜色.
     * @return 对应的逻辑颜色；若未匹配则回退为 {@code TargetColor.C0}.
     */
    public static TargetColor toTargetColor(Color color) {
        Color[] arr = ColorUtils.COLORS;
        TargetColor[] logical = TargetColor.values();
        int n = Math.min(arr.length, logical.length);
        for (int i = 0; i < n; i++) {
            if (arr[i].equals(color)) {
                return logical[i];
            }
        }
        return TargetColor.C0;
    }
}
