package com.unscrewit.logic;

import com.unscrewit.Board;
import com.unscrewit.LevelState;
import com.unscrewit.Screw;
import com.unscrewit.logic.BufferOverflowException;
import com.unscrewit.model.Buffer;
import com.unscrewit.model.ColorMapping;
import com.unscrewit.model.TargetColor;
import com.unscrewit.rules.Rules;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Game controller: coordinates core gameplay logic and event handling.
 *
 * <p>
 * This class serves as the main entry point for handling in-game actions such as:
 * player clicks, target and buffer transitions, automatic screw movement,
 * and win/lose state checks.
 * </p>
 *
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Interpret and process user clicks (hit detection, color matching).</li>
 *   <li>Manage screw transfers between boards, buffer, and targets.</li>
 *   <li>Handle automatic refill and module-based target color selection.</li>
 *   <li>Trigger callbacks for win/loss states when conditions are met.</li>
 * </ul>
 * </p>
 */
public final class GameController {

    /** Current game state, including boards, targets, buffer, and color pools. */
    private final LevelState state;

    /** Callback executed when the player wins. */
    private final Runnable onWin;

    /** Callback executed when the player loses (e.g., buffer overflow). */
    private final Runnable onFail;

    /**
     * Constructs the main game controller.
     *
     * @param state   current level state
     * @param onWin   callback invoked upon victory
     * @param onFail  callback invoked upon defeat (e.g., buffer overflow)
     */
    public GameController(final LevelState state, final Runnable onWin, final Runnable onFail) {
        this.state = state;
        this.onWin = onWin;
        this.onFail = onFail;
    }

    /**
     * Handles a single click event based on the provided screen coordinate.
     *
     * <p>
     * This method identifies the topmost visible screw at the click position,
     * validates visibility (must be >50% visible), determines its color mapping,
     * and attempts to move it either to the target area or the buffer queue.
     * </p>
     *
     * @param p the click position on screen
     */
    public void handleClick(final Point p) {
        // Step 1: Identify topmost screw under the cursor
        final Screw s = findTopmostScrewAt(p);
        if (s == null) {
            return;
        }

        // Step 2: Check visibility — screw must be at least 50% exposed
        final int radius = Screw.size() / 2;
        final Board holder = state.findHolder(s);
        final List<Rectangle> covers = state.coveringRectsFor(holder);
        final boolean clickable = Visibility.isClickable(
                s.x, s.y, radius, covers, Rules.CLICKABLE_COVERAGE_RATIO);
        if (!clickable) {
            return;
        }

        // Step 3: Determine logical color
        final TargetColor logicColor = ColorMapping.toTargetColor(s.color);

        // Step 4: Attempt to place the screw
        final boolean placedToTarget = state.targets().tryPlace(logicColor);
        if (placedToTarget) {
    
            state.removeScrew(s); // Remove from board

            drainBufferToTargets(); // Always process buffered screws first

            refreshTargetsIfNeeded(); // Update target colors if necessary

            if (state.allCleared()) {
                onWin.run();
            }
        } else {
            // Try to push into buffer; may throw overflow exception
            try {
                state.buffer().push(logicColor);
            } catch (BufferOverflowException ex) {
                onFail.run();
                return;
            }
            // Step 5: Handle automatic transfers and win checks
            drainBufferToTargets();
            refreshTargetsIfNeeded();
        }
    }

    /**
     * Refreshes target colors when both or one target is full.
     *
     * <p>
     * Implements the “three-in-a-row module-driven color selection” strategy:
     * when a target is full, new target colors are selected from available modules
     * and the current clickable color set. It avoids color repetition where possible.
     * </p>
     */
    private void refreshTargetsIfNeeded() {
        final boolean leftFull = state.targets().leftFull();
        final boolean rightFull = state.targets().rightFull();
        if (!leftFull && !rightFull) {
            return;
        }

        // Build pools of module colors and clickable candidates
        final Map<TargetColor, Integer> modulePool = buildModulePoolFromBoards();
        final Set<TargetColor> clickable = computeClickableColors();

        TargetColor newLeft = state.targets().leftColor();
        TargetColor newRight = state.targets().rightColor();

        // Both targets full — refresh both sides
        if (leftFull && rightFull) {
            // Prefer module-driven color selection, fallback to random pick
            TargetColor c0 = pickTargetColorByModule(modulePool, clickable, null);
            if (c0 != null) {
                modulePool.put(c0, Math.max(0, modulePool.getOrDefault(c0, 0) - 1));
            }
            TargetColor c1 = pickTargetColorByModule(modulePool, clickable, c0);
            if (c1 == null) {
                // allow reuse if only one color remains
                c1 = c0 != null ? c0 : pickTargetColorByModule(modulePool, clickable, null);
            }
            if (c0 != null) {
                newLeft = c0;
            } else {
                newLeft = pickNewColorExcluding(state.targets().rightColor());
            }
            if (c1 != null) {
                newRight = c1;
            } else {
                newRight = pickNewColorExcluding(newLeft);
            }

        // Only left full
        } else if (leftFull) {
            TargetColor c = pickTargetColorByModule(
                modulePool, 
                clickable, 
                state.targets().rightColor());
            if (c == null) {
                c = pickNewColorExcluding(state.targets().rightColor());
            }
            newLeft = c; 
        }

        // Only right full
        else { 
            TargetColor c = pickTargetColorByModule(
                modulePool, 
                clickable, 
                state.targets().leftColor());
            if (c == null) {
                c = pickNewColorExcluding(state.targets().leftColor());
            }
            newRight = c;
        }

        // Apply color refresh
        state.targets().refreshColors(leftFull, newLeft, rightFull, newRight);
    }

    /**
     * Transfers any queued screws from the buffer to the target area if possible.
     *
     * <p>
     * Continuously pops elements from the buffer queue and attempts to place
     * each screw’s color into a valid target slot. If a placement fails, the color
     * is reinserted back into the buffer and the transfer process stops.
     * </p>
     */
    private void drainBufferToTargets() {
        final Buffer buffer = state.buffer();
        
        while (true) {
            // Pop the head of the buffer (no peek available; decide later if reinsertion is needed)
            final TargetColor c = buffer.pop();
            if (c == null) {
                return;
            }

            // Try placing into a target
            if (state.targets().tryPlace(c)) {
                // Successfully placed — continue to next one
                continue;
            } else {
                // Failed to place — put it back to the buffer and stop
                try {
                    buffer.push(c);
                } catch (BufferOverflowException ignore) {
                    // This should never happen, since we just popped one element earlier.
                }
                return;
            }
        }
    }

    /**
     * Builds a mapping (“module pool”) between target colors and the number
     * of complete modules available across all boards.
     *
     * <p>
     * Each color is counted based on its appearance among all screws on
     * active boards. Every three screws of the same color form one module.
     * </p>
     *
     * @return a map of {@link TargetColor} to module counts
     */
    private Map<TargetColor, Integer> buildModulePoolFromBoards() {
        final Map<TargetColor, Integer> count = new HashMap<>();
        
        // Count occurrences of each color from all boards
        for (Board b : state.boards()) {
            for (Screw s : b.screws) {
                final TargetColor c = ColorMapping.toTargetColor(s.color);
                count.merge(c, 1, Integer::sum);
            }
        }

        // Convert raw screw counts into module counts (3 screws = 1 module)
        final Map<TargetColor, Integer> pool = new HashMap<>();
        for (Map.Entry<TargetColor, Integer> e : count.entrySet()) {
            final int modules = e.getValue() / 3;
            if (modules > 0) {
                pool.put(e.getKey(), modules);
            }
        }
        return pool;
    }

    /**
     * Computes the set of currently clickable colors based on screw visibility.
     *
     * <p>
     * For each screw on the board, this function checks if it is visible enough
     * (coverage below the threshold) and converts its physical color into a
     * logical target color. All visible colors are collected and returned.
     * </p>
     *
     * @return a set of {@link TargetColor} representing all currently clickable colors
     */
    private Set<TargetColor> computeClickableColors() {
        final Set<TargetColor> result = new HashSet<>();
        final int radius = Screw.size() / 2;
        for (Board b : state.boards()) {
            final List<Rectangle> cover = state.coveringRectsFor(b);
            for (Screw s : b.screws) {
                final boolean click = Visibility.isClickable(
                        s.x, s.y, radius, cover, Rules.CLICKABLE_COVERAGE_RATIO);
                if (click) {
                    result.add(ColorMapping.toTargetColor(s.color));
                }
            }
        }
        return result;
    }

    /**
     * Selects a target color from the module pool based on clickability and module count.
     *
     * <p>
     * The method prioritizes colors that are both clickable and have higher module counts.
     * If no clickable color fits, it falls back to other available module colors.
     * Optionally, one color can be excluded to avoid repetition.
     * </p>
     *
     * @param modulePool the color-to-module count map
     * @param clickable  the set of currently clickable colors
     * @param excluded   an optional color to exclude (can be {@code null})
     * @return the selected {@link TargetColor}, or {@code null} if no suitable color found
     */
    private TargetColor pickTargetColorByModule(
            final Map<TargetColor, Integer> modulePool,
            final Set<TargetColor> clickable,
            final TargetColor excluded) {

        final List<TargetColor> p1 = new ArrayList<>();
        final List<TargetColor> p2 = new ArrayList<>();

        for (Map.Entry<TargetColor, Integer> e : modulePool.entrySet()) {
            final TargetColor c = e.getKey();
            final int m = e.getValue();
            if (m <= 0) {
                continue;
            }
            // Skip the excluded color if defined
            if (excluded != null && c == excluded) {
                continue;
            }
            // Classify by clickability
            if (clickable.contains(c)) {
                p1.add(c);
            } else {
                p2.add(c);
            }
        }

        // Sort colors by module count (descending), then by enum order
        final Comparator<TargetColor> cmp = Comparator
                .comparingInt((TargetColor c) -> modulePool.getOrDefault(c, 0))
                .reversed()
                .thenComparingInt(Enum::ordinal);

        p1.sort(cmp);
        p2.sort(cmp);


        // Priority: clickable list first
        if (!p1.isEmpty()) {
            return p1.get(0);
        }
        if (!p2.isEmpty()) {
            return p2.get(0);
        }
        if (excluded != null && modulePool.getOrDefault(excluded, 0) > 0) {
            return excluded;
        }
        return null;
    }

    /**
     * Finds the topmost screw at a given screen coordinate across all boards.
     *
     * <p>
     * This method iterates through all boards in reverse order (topmost first)
     * and checks each screw to see if the click point lies within its hitbox.
     * The first matching screw encountered is returned as the topmost one.
     * </p>
     *
     * @param p the point clicked on screen
     * @return the topmost {@link Screw} under the click position, or {@code null} if none is hit
     */
    private Screw findTopmostScrewAt(final Point p) {
        // Boards are assumed to be ordered front-to-back, 
        // so iterate in reverse to find the top layer first.
        final List<Board> boards = state.boards();
        for (int i = boards.size() - 1; i >= 0; i--) {
            final Board b = boards.get(i);
            for (int j = b.screws.size() - 1; j >= 0; j--) {
                final Screw s = b.screws.get(j);
                if (s.contains(p)) {
                    return s;
                }
            }
        }
        return null;
    }

    /**
     * Randomly selects a new target color, excluding the specified one.
     * Used when refreshing or reassigning target colors. Ensures that
     * the newly picked color is different from the excluded one, preserving
     * color diversity while maintaining consistent game logic.
     *
     * @param exclude the color to exclude from random selection (may be {@code null})
     * @return a randomly chosen {@link TargetColor} that differs from {@code exclude}
     */
    private TargetColor pickNewColorExcluding(final TargetColor exclude) {
        final TargetColor[] vals = TargetColor.values();
        TargetColor candidate = vals[state.random().nextInt(vals.length)];
        

        // If the random pick matches the excluded color, pick the next one cyclically
        if (candidate == exclude) {
            candidate = vals[(candidate.ordinal() + 1) % vals.length];
        }
        return candidate;
    }
}
