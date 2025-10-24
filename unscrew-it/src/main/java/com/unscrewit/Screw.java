package com.unscrewit;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;

/**
 * Represents a screw on the board. Each screw has a fixed position,
 * a color, and can determine whether a point hits it. It also handles
 * its own drawing.
 */
public class Screw {

    /** Diameter (in pixels). */
    private static final int SIZE = 18;

    /** Center x-coordinate of the screw (in pixels). */
    public final int x;

    /** Center y-coordinate of the screw (in pixels). */
    public final int y;

    /** The color of the screw. */
    public final Color color;

    /**
     * Constructs a screw with a specific position and color.
     *
     * @param x the x-coordinate of the screw center (in pixels)
     * @param y the y-coordinate of the screw center (in pixels)
     * @param color the color of the screw
     */
    public Screw(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
    }

    /**
     * Determines whether a given point lies within the screw's circular area.
     *
     * @param p the point to test
     * @return {@code true} if the point is inside the screw's hit area;
     *         {@code false} otherwise
     */
    public boolean contains(Point p) {
        // Compute squared distance from center to the point
        double dx = p.x - x;
        double dy = p.y - y;
        double r = SIZE / 2.0;
        return dx * dx + dy * dy <= r * r;
    }

    /**
     * Draws the screw on the provided graphics context.
     * It is rendered as a filled circle with a dark gray outline.
     *
     * @param g2 the graphics context
     */
    public void draw(Graphics2D g2) {
        int r = SIZE / 2;
        g2.setColor(color);
        g2.fillOval(x - r, y - r, SIZE, SIZE);

        g2.setColor(Color.DARK_GRAY);
        g2.drawOval(x - r, y - r, SIZE, SIZE);
    }

    /**
     * Returns the diameter of the screw (in pixels).
     *
     * @return the screw diameter
     */
    public static int size() {
        return SIZE;
    }
}

