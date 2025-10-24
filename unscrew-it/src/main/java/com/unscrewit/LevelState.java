package com.unscrewit;

import com.unscrewit.model.Buffer;
import com.unscrewit.model.Palette;
import com.unscrewit.model.TargetColor;
import com.unscrewit.model.TargetSlots;
import com.unscrewit.rules.Rules;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Level state: holds all runtime data for the current level.
 *
 * <p>Includes the boards, target slots, and buffer. Also provides helpers for
 * querying and updating state during gameplay.</p>
 *
 * <p>Coordinate system: screen-space pixel positions from the top-left.</p>
 */
public final class LevelState {

    /** All boards in the current level. Index increases from back to front. */
    private final List<Board> boards = new ArrayList<>(Rules.BOARD_COUNT);

    /** The two target areas at the top of the screen. */
    private final TargetSlots targetSlots;

    /** Temporary holding buffer for screws. */
    private final Buffer buffer = new Buffer();

    /** Pseudorandom generator for shuffles and random choices. */
    private final Random random = new Random();

    /**
     * Creates a level state using only the given canvas dimensions.
     *
     * @param canvasW canvas width in pixels
     * @param canvasH canvas height in pixels
     */
    public LevelState(int canvasW, int canvasH) {
        // Create random boards back-to-front.
        for (int i = 0; i < Rules.BOARD_COUNT; i++) {
            boards.add(Board.randomBoard(i, canvasW, canvasH, random));
        }
        // Pick two distinct logical colors for the initial targets.
        List<TargetColor> palette = Palette.defaultPalette();
        Collections.shuffle(palette, random);
        this.targetSlots = new TargetSlots(palette.get(0), palette.get(1));
    }

    /**
     * Returns all boards in this level.
     *
     * @return the list of boards (back-to-front order)
     */
    public List<Board> boards() {
        return boards;
    }

    /**
     * Returns the target slots object.
     *
     * @return the {@link TargetSlots}
     */
    public TargetSlots targets() {
        return targetSlots;
    }

    /**
     * Returns the buffer.
     *
     * @return the {@link Buffer}
     */
    public Buffer buffer() {
        return buffer;
    }

    /**
     * Returns the internal random generator.
     *
     * @return the {@link Random} instance
     */
    public Random random() {
        return random;
    }

    /**
     * Checks whether all boards are cleared of screws.
     *
     * @return {@code true} if every board has no screws; {@code false} otherwise
     */
    public boolean allCleared() {
        for (Board b : boards) {
            if (!b.screws.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the rectangles of every board located visually above the given holder.
     * Useful for hit testing when dragging across layers.
     *
     * @param holder the board whose covering boards to compute
     * @return rectangles of boards above the holder (front-most first)
     */
    public List<Rectangle> coveringRectsFor(Board holder) {
        List<Rectangle> rects = new ArrayList<>();
        int idx = boards.indexOf(holder);
        if (idx < 0) {
            return rects;
        }
        // Collect rectangles from all boards in front of the holder.
        for (int i = boards.size() - 1; i > idx; i--) {
            rects.add(boards.get(i).rect);
        }
        return rects;
    }

    /**
     * Removes the given screw from whichever board currently holds it.
     *
     * @param screw the screw to remove
     * @return {@code true} if the screw was found and removed; {@code false} otherwise
     */
    public boolean removeScrew(Screw screw) {
        for (Board b : boards) {
            if (b.screws.remove(screw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Finds the board that currently contains the given screw.
     *
     * @param screw the screw to search for
     * @return the board that contains the screw, or {@code null} if none
     */
    public Board findHolder(Screw screw) {
        for (Board b : boards) {
            if (b.screws.contains(screw)) {
                return b;
            }
        }
        return null;
    }
}
