package com.unscrewit.model;

import com.unscrewit.ColorUtils;
import java.awt.Color;

/**
 * Utility class for mapping between visual colors ({@link java.awt.Color})
 * and logical colors used in the game ({@link TargetColor}).
 *
 * <p>Convention: {@code ColorUtils.COLORS[i]} corresponds one-to-one
 * with {@code TargetColor.values()[i]}.</p>
 */
public final class ColorMapping {

    /** Private constructor to prevent instantiation. */
    private ColorMapping() {
        // Utility class should not be instantiated.
    }

    /**
     * Converts a visual {@link Color} into its logical {@link TargetColor}
     * representation.
     *
     * @param color the visual color to map
     * @return the corresponding logical color; if no match is found,
     *         {@code TargetColor.C0} is returned as default
     */
    public static TargetColor toTargetColor(Color color) {
        Color[] arr = ColorUtils.COLORS;
        TargetColor[] logical = TargetColor.values();
        int n = Math.min(arr.length, logical.length);
        
        // Iterate through available color pairs and find a match.
        for (int i = 0; i < n; i++) {
            if (arr[i].equals(color)) {
                return logical[i];
            }
        }
        
        // Return default if no matching color is found.
        return TargetColor.C0;
    }
}
