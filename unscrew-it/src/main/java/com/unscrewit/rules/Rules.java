package com.unscrewit.rules;

/**
 * Centralized game constants according to the agreed rules.
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
        // Utility class.
    }
}
