package com.unscrewit;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Represents a single board that holds multiple screws.
 * 
 * <p>
 * Each board can fade out (disappear) when cleared, and supports visibility checks
 * and rendering logic for screws. Boards are stacked in depth order during gameplay.
 * </p>
 */
public class Board {

    /** Pixel offset used when sampling screw visibility. */
    private static final int VIS_SAMPLE_OFFSET = 8;

    /** Minimum visible ratio threshold (below this means fully hidden). */
    private static final double VISIBLE_THRESHOLD = 0.5;

    /** Rectangle bounds of the board (public for convenience). */
    public final Rectangle rect;

    /** List of screws on this board (public for convenience). */
    public final List<Screw> screws = new ArrayList<>();

    /** Whether this board is currently in the fade-removal process. */
    private boolean removing = false;

    /** Whether the board has completely faded out (invisible). */
    private boolean hidden = false;

    /**
     * Constructs a new {@code Board} instance with a fixed rectangle region.
     *
     * @param r the rectangular bounds of this board
     */
    public Board(final Rectangle r) {
        this.rect = r;
    }

    /**
     * Creates a randomized board (2×3 or 2×4 layout) and distributes screws evenly.
     *
     * @param idx the board index
     * @param w canvas width
     * @param h canvas height
     * @param rand random generator
     * @return a newly created random {@code Board}
     */
    public static Board randomBoard(final int idx, final int w, final int h, final Random rand) {
        final int topH = h / 4;
        final int midH = h / 8;
        final int playTop = topH + midH;
        final int slotH = 48;
        final int bufferMargin = 28;
        final int yBufferTop = h - slotH - bufferMargin;
        final int playHeight = Math.max(1, yBufferTop - playTop - 20);

        final int bw = 160 + rand.nextInt(120);
        final int bh = 70 + rand.nextInt(50);

        final int x = rand.nextInt(Math.max(1, w - bw));
        final int y = playTop + rand.nextInt(Math.max(1, playHeight - bh));

        final Board b = new Board(new Rectangle(x, y, bw, bh));

        final int rows = 2;
        final int cols = 3 + rand.nextInt(2); // 3 or 4 columns

        // Generate evenly spaced screw positions
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                final int sx = x + 20 + c * (bw - 40) / Math.max(1, cols - 1);
                final int sy = y + 20 + r * (bh - 40) / Math.max(1, rows - 1);
                b.screws.add(new Screw(sx, sy, Color.GRAY)); // color unified later
            }
        }
        return b;
    }

    /**
     * Performs hit detection: returns the screw at the clicked point if visible.
     *
     * @param p click position
     * @param above list of boards above this one (for occlusion)
     * @return the clicked screw, or {@code null} if none
     */
    public Screw getClickedScrew(final Point p, final List<Board> above) {
        if (hidden) {
            return null;
        }
        for (Screw s : screws) {
            if (s.contains(p) && isScrewVisible(s, above)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Determines whether a screw is visible (not occluded by boards above it).
     * 
     * <p>Uses sample-based detection with 9 surrounding points.</p>
     *
     * @param s the screw to test
     * @param above list of boards above this one
     * @return {@code true} if the screw is visible; {@code false} otherwise
     */
    public boolean isScrewVisible(final Screw s, final List<Board> above) {
        final int o = VIS_SAMPLE_OFFSET;
        final int[][] samples = new int[][] {
            {0, 0},
            {-o, 0}, {o, 0},
            {0, -o}, {0, o},
            {-o, -o}, {-o, o}, {o, -o}, {o, o}
        };
        int covered = 0;
        for (int[] d : samples) {
            int px = s.x + d[0];
            int py = s.y + d[1];
            boolean c = false;
            for (Board b : above) {
                if (b.rect.contains(px, py) && !b.hidden) {
                    c = true;
                    break;
                }
            }
            if (c) {
                covered++;
            }
        }
        double ratio = covered / (double) samples.length;
        return ratio < VISIBLE_THRESHOLD;
    }

    /**
     * Removes the given screw from this board.
     *
     * @param s the screw to remove
     */
    public void removeScrew(final Screw s) {
        screws.remove(s);
    }

    /**
     * Starts the fade-out process when all screws are removed.
     */
    public void startRemoving() {
        if (!removing && !hidden) {
            removing = true;
        }
    }

    /**
     * Returns whether this board has no screws remaining.
     *
     * @return {@code true} if the board is empty
     */
    public boolean allRemoved() {
        return screws.isEmpty();
    }

    /**
     * Returns whether this board is completely hidden.
     *
     * @return {@code true} if hidden
     */
    public boolean isHidden() {
        return hidden;
    }

    /**
     * Draws the board and its screws.
     * 
     * <p>Requires the list of boards above to ensure visibility consistency.</p>
     *
     * @param g the graphics context
     * @param above list of boards above this one
     */
    public void draw(final Graphics2D g, final List<Board> above) {
        if (hidden) {
            return;
        }

        // --- Draw board body ---
        Composite old = g.getComposite();
        g.setColor(new Color(205, 211, 217));
        g.fill(rect);
        g.setColor(new Color(90, 96, 102));
        g.draw(rect);

        // --- Draw screws ---
        for (Screw s : screws) {
            boolean vis = isScrewVisible(s, above);
            if (vis) {
                s.draw(g);
                g.setStroke(new BasicStroke(2.2f));
                g.setColor(new Color(255, 255, 255, 200));
                int r = 22;
                g.drawOval(s.x - r / 2, s.y - r / 2, r, r);
                g.setStroke(new BasicStroke(1f));
            } else {
                // partially covered screws drawn darker
                int size = Screw.size();
                int x = s.x - size / 2;
                int y = s.y - size / 2;
                g.setColor(new Color(90, 90, 90, 120));
                g.fillOval(x, y, size, size);
                g.setColor(new Color(40, 40, 40, 180));
                g.drawOval(x, y, size, size);
                g.drawLine(x + 4, y + 4, x + size - 4, y + size - 4);
                g.drawLine(x + size - 4, y + 4, x + 4, y + size - 4);
            }
        }

        // Display screw count at top-left corner
        g.setComposite(old);
        g.setColor(Color.BLACK);
        g.drawString(String.valueOf(screws.size()), rect.x + 4, rect.y + 14);
    }

    /**
     * Simplified draw version used when no occlusion info is needed.
     *
     * @param g the graphics context
     */
    public void draw(final Graphics2D g) {
        draw(g, Collections.emptyList());
    }

}
