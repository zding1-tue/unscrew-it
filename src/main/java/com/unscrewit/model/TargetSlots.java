package com.unscrewit.model;

import com.unscrewit.rules.Rules;

/**
 * Represents the two target areas at the top of the screen.
 * <p>
 * Each target area corresponds to one logical color and consists of
 * {@link Rules#TARGET_SLOTS_PER_COLOR} slots. When all slots for a target color
 * are filled, that color is considered cleared and should be replaced with a
 * new target color.
 * </p>
 */
public final class TargetSlots {

    /** The color of the left target. */
    private TargetColor leftColor;

    /** The color of the right target. */
    private TargetColor rightColor;

    /** The number of filled slots for the left target. */
    private int leftCount;

    /** The number of filled slots for the right target. */
    private int rightCount;

    /**
     * Creates a new {@code TargetSlots} instance with the given initial colors.
     *
     * @param left  the initial color of the left target
     * @param right the initial color of the right target; should not equal left
     */
    public TargetSlots(TargetColor left, TargetColor right) {
        this.leftColor = left;
        this.rightColor = right;
        this.leftCount = 0;
        this.rightCount = 0;
    }

    /**
     * Attempts to place a screw of the given color into one of the targets.
     * <p>
     * If the screw's color matches a target color that is not yet full, the screw
     * is added to that target. Otherwise, it is rejected.
     * </p>
     *
     * @param color the color of the screw to place
     * @return {@code true} if successfully placed; {@code false} otherwise
     */
    public boolean tryPlace(TargetColor color) {
        if (color == leftColor && leftCount < Rules.TARGET_SLOTS_PER_COLOR) {
            leftCount++;
            return true;
        }
        if (color == rightColor && rightCount < Rules.TARGET_SLOTS_PER_COLOR) {
            rightCount++;
            return true;
        }
        return false;
    }

    /**
     * Checks whether the left target is full.
     *
     * @return {@code true} if the left target has reached its slot capacity
     */
    public boolean leftFull() {
        return leftCount >= Rules.TARGET_SLOTS_PER_COLOR;
    }

    /**
     * Checks whether the right target is full.
     *
     * @return {@code true} if the right target has reached its slot capacity
     */
    public boolean rightFull() {
        return rightCount >= Rules.TARGET_SLOTS_PER_COLOR;
    }

    /**
     * Refreshes one or both target colors and resets their counters to zero.
     *
     * @param replaceLeft  whether to refresh the left target color
     * @param newLeft      the new left target color if {@code replaceLeft} is true
     * @param replaceRight whether to refresh the right target color
     * @param newRight     the new right target color if {@code replaceRight} is true
     */
    public void refreshColors(boolean replaceLeft, TargetColor newLeft,
            boolean replaceRight, TargetColor newRight) {
        if (replaceLeft) {
            leftColor = newLeft;
            leftCount = 0;
        }
        if (replaceRight) {
            rightColor = newRight;
            rightCount = 0;
        }
    }

    /**
     * Returns the color of the left target.
     *
     * @return the left target color
     */
    public TargetColor leftColor() {
        return leftColor;
    }

    /**
     * Returns the color of the right target.
     *
     * @return the right target color
     */
    public TargetColor rightColor() {
        return rightColor;
    }

    /**
     * Returns the current fill count of the left target.
     *
     * @return the number of screws currently placed in the left target
     */
    public int leftCount() {
        return leftCount;
    }

    /**
     * Returns the current fill count of the right target.
     *
     * @return the number of screws currently placed in the right target
     */
    public int rightCount() {
        return rightCount;
    }
}
