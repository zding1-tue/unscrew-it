package com.unscrewit.rules;

/**
 * Centralized, immutable game rules and tuning constants.
 *
 * <p>This class defines the knobs that control overall gameplay and UI layout.
 * All fields are {@code public static final} so they can be referenced from
 * rendering and logic code without instantiating this class.</p>
 *
 * <p><b>Notes:</b>
 * <ul>
 *   <li>Changing these values affects both logic and drawing. Ensure dependent
 *       calculations in {@link com.unscrewit.GamePanel}, {@link com.unscrewit.model.TargetSlots},
 *       {@link com.unscrewit.model.Palette}, and {@link com.unscrewit.rules.Board}
 *       remain consistent.</li>
 *   <li>Units are integers unless otherwise documented; angles or ratios are
 *       expressed as {@code double} in the range {@code [0.0, 1.0]}.</li>
 * </ul>
 * </p>
 */
public final class Rules {

    /** Number of boards in a level. */
    public static final int BOARD_COUNT = 7;

    /** Slots per target color (two targets on screen, each needs 3 screws). */
    public static final int TARGET_SLOTS_PER_COLOR = 3;

    /** Buffer capacity (queue-like). */
    public static final int BUFFER_CAPACITY = 4;

    /** Clickable threshold: a screw is clickable if covered less than 50%. */
    public static final double CLICKABLE_COVERAGE_RATIO = 0.5;

    /** Preferred number of distinct screw colors in the palette. */
    public static final int PALETTE_SIZE = 8;

    private Rules() {
        // Utility class; not meant to be instantiated.
    }
}
