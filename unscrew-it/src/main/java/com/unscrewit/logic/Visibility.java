package com.unscrewit.logic;

import java.awt.Rectangle;
import java.util.List;

/**
 * Utility class that provides visibility checks for screws.
 * <p>
 * A screw is considered clickable only when the area covered by other boards
 * is below a certain threshold ratio. This helper approximates each screw
 * as a bounding box (square) for the coverage calculation.
 * </p>
 */
public final class Visibility {

    private Visibility() {
        // Utility class: prevent instantiation.
    }

    /**
     * Determines whether a screw is clickable based on its coverage by
     * upper boards.
     * <p>
     * The screw is represented by a bounding box centered at {@code (x, y)}
     * with the given {@code radius}. The function iterates over all rectangles
     * representing the boards above it, sums the total overlapping area, and
     * compares it to the screw's total area.
     * </p>
     *
     * @param x the x-coordinate of the screw center
     * @param y the y-coordinate of the screw center
     * @param radius the radius of the screw (pixels)
     * @param coveringRects the list of rectangles representing upper boards
     *                      potentially covering the screw
     * @param clickableThresholdRatio the threshold ratio for visibility,
     *        e.g., {@code 0.5} means the screw is clickable if less than
     *        50% of its area is covered
     * @return {@code true} if the screw is clickable; {@code false} otherwise
     */
    public static boolean isClickable(int x, int y, int radius,
            List<Rectangle> coveringRects, double clickableThresholdRatio) {

        // Define the screw’s bounding box      
        Rectangle boundingBox = new Rectangle(x - radius, y - radius,
                radius * 2, radius * 2);

        long coveredArea = 0;
        
        // Calculate the total overlapping area with upper boards
        for (Rectangle rect : coveringRects) {
            Rectangle intersection = boundingBox.intersection(rect);
            if (!intersection.isEmpty()) {
                coveredArea += (long) intersection.width * intersection.height;
            }
        }

        // Total screw area
        long totalArea = (long) boundingBox.width * boundingBox.height;
        
        // Compute ratio of visible area to total area
        double coverageRatio = totalArea == 0 ? 1.0
                : (coveredArea * 1.0 / totalArea);

        // The screw is clickable if less area is covered than the allowed threshold        
        return coverageRatio < clickableThresholdRatio;
    }
}
