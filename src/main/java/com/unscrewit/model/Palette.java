package com.unscrewit.model;

import com.unscrewit.rules.Rules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Provides helper methods for managing the logical color palette
 * used in the game.
 * <p>
 * The palette defines a fixed number of logical colors (C0–C7)
 * that correspond to real RGB colors defined elsewhere, e.g. in
 * {@code ColorUtils}. This class also supports generating a
 * shuffled list of colors arranged in triplets, which is used
 * when distributing screw colors across all boards.
 * </p>
 */
public final class Palette {

    private Palette() {
        // Utility class.
    }

    /**
     * Returns the default logical color palette consisting of
     * {@link Rules#PALETTE_SIZE} entries (C0–C7).
     *
     * @return a list of {@link TargetColor} objects in default order
     */
    public static List<TargetColor> defaultPalette() {
        List<TargetColor> list = new ArrayList<>(Rules.PALETTE_SIZE);
        for (int i = 0; i < Rules.PALETTE_SIZE; i++) {
            list.add(TargetColor.values()[i]);
        }
        return list;
    }

    /**
     * Generates a list of colors arranged in triplets (groups of three
     * identical colors) and then shuffles them globally.
     * <p>
     * This method ensures that the total number of returned elements is
     * always a multiple of three, which simplifies later grouping and
     * matching logic in the game.
     * </p>
     *
     * @param total the desired total number of screws;
     *              the result is rounded up to a multiple of three
     * @return a shuffled list of {@link TargetColor} entries,
     *         containing triplets of matching colors
     */
    public static List<TargetColor> generateTripletShuffled(int total) {
        int adjusted = ((total + 2) / 3) * 3;
        List<TargetColor> palette = defaultPalette();
        List<TargetColor> pool = new ArrayList<>(adjusted);

        int index = 0;
        while (pool.size() < adjusted) {
            TargetColor color = palette.get(index % palette.size());
            pool.add(color);
            pool.add(color);
            pool.add(color);
            index++;
        }

        Collections.shuffle(pool);
        return pool;
    }
}
