package com.unscrewit;

import com.unscrewit.logic.GameController;
import com.unscrewit.model.Palette;
import com.unscrewit.model.TargetColor;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Main game canvas responsible for level UI and interaction.
 *
 * <p>This implementation renders:
 * boards with screws, the two target areas at the top, the temporary buffer,
 * moving-screw animations, and the end-game overlay. It also owns the game
 * state used by the UI (e.g., selection, animation timer, and pools of
 * pieces/modules) and drives interaction through mouse clicks.</p>
 *
 * <p>High-level flow:
 * <ol>
 *   <li>{@code addNotify()} → lazy initialization on the EDT.</li>
 *   <li>{@code paintComponent(...)} → draw background, targets, buffer,
 *       boards, moving screws, and end overlay.</li>
 *   <li>Mouse click → {@code handleClick(...)} → pick a visible screw and
 *       {@code moveScrew(...)} to a target or to the buffer, with animation.</li>
 * </ol>
 * </p>
 */
public class GamePanel extends JPanel {

    /** 
     * Number of slots in each target area.
     * Each target can hold up to this many screws.
     */
    private static final int TARGET_SLOTS = 3;

    /** 
     * Capacity of the temporary buffer area below the targets.
     * If the buffer is full, the player cannot store more screws there.
     */
    private static final int BUFFER_SIZE = 4;

    /** Number of boards to generate for a level. */
    private static final int BOARD_COUNT = 7;

    /** Title text drawn at the top. */
    private static final String TITLE = "Click visible screws to match the target colors";

    /** Duration in milliseconds for a screw-moving animation step. */
    private static final int MOVE_DURATION_MS = 260;

    /** Random source used for board generation and selections. */
    private final Random rand = new Random();

    /** Collection of boards (back-to-front drawing order). */
    private final List<Board> boards = new ArrayList<>();

    /** Temporary buffer below the targets. */
    private final List<Screw> buffer = new ArrayList<>();

    /** The two current target colors (left / right). */
    private final Color[] targetColors = new Color[2];

    /** Used slots for each target (0..TARGET_SLOTS). */
    private final int[] targetUsed = new int[2];

    /** Count of loose screws by color (for win detection). */
    private final Map<Color, Integer> piecesPool = new HashMap<>();

    /** Count of 3-piece modules by color (for win detection / picking). */
    private final Map<Color, Integer> modulesPool = new HashMap<>();

    /** Currently animating screws. */
    private final List<MovingScrew> movingScrews = new ArrayList<>();

    /** Swing timer that ticks animations. */
    private final Timer animationTimer;

    /** Whether the panel has been lazily initialized. */
    private boolean inited = false;

    /** Whether the game is currently over. */
    private boolean gameOver = false;

    /** Whether the last game was won. */
    private boolean win = false;

    /** Clickable area for the “New” button on the end overlay. */
    private Rectangle btnNewRect;

    /** Clickable area for the “Quit” button on the end overlay. */
    private Rectangle btnQuitRect;

    /**
     * Creates the game panel and installs the mouse handler and animation timer.
     */
    public GamePanel() {
        setBackground(new Color(245, 245, 245));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(final MouseEvent e) {
                if (!inited) {
                    return;
                }
                if (gameOver) {
                    handleEndButtons(e.getPoint());
                } else {
                    handleClick(e.getPoint());
                }
            }
        });

        animationTimer = new Timer(16, e -> onAnimationTick());
        animationTimer.setCoalesce(true);
    }

    /**
     * Compatibility hook: accepts a {@code LevelState} from outside.
     * <p>Not used in this implementation.</p>
     *
     * @param state the level state object
     */
    public void setLevelState(final LevelState state) {
        // no-op.
    }

    /**
     * Compatibility hook: accepts the game controller from outside.
     * <p>Not used in this implementation.</p>
     *
     * @param controller the controller instance
     */
    public void setController(final GameController controller) {
        // no-op.
    }

    /**
     * Lazily initializes once after the component is added to the UI hierarchy.
     */
    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> {
            if (!inited) {
                initGame(getWidth(), getHeight());
                inited = true;
                repaint();
            }
        });
    }

    /**
     * Preferred size of the canvas.
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1050, 700);
    }

    /**
     * Rendering pipeline.
     *
     * <p>Draws background, title, targets, buffer, boards, moving screws,
     * and end overlay (if any). The list of boards above the current one is
     * passed so visibility checks are consistent with the draw order.</p>
     */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // Background gradient.
        GradientPaint bg = new GradientPaint(
            0f, 0f, new Color(240, 244, 248),
            0f, h, new Color(226, 233, 240)
        );
        java.awt.Paint oldPaint = g2.getPaint();
        g2.setPaint(bg);
        g2.fillRect(0, 0, w, h);
        g2.setPaint(oldPaint);

        int topH = h / 4;
        int midH = h / 8;

        // Title.
        g2.setColor(new Color(70, 70, 70));
        g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
        g2.drawString(TITLE, 24, 28);

        // Targets and buffer UI.
        drawTargets(g2, w, topH);
        drawBuffer(g2, w, topH, midH);

        // Boards (back-to-front).
        for (int i = 0; i < boards.size(); i++) {
            List<Board> above = boards.subList(i + 1, boards.size());
            boards.get(i).draw(g2, above);
        }

        // Active moving screws.
        drawMovingScrews(g2);

        // End overlay + buttons.
        if (gameOver) {
            drawEndOverlay(g2, w, h);
        }
    }

    /**
     * Initializes one round of the game.
     *
     * @param w canvas width
     * @param h canvas height
     */
    private void initGame(final int w, final int h) {
        gameOver = false;
        win = false;
        
        // Reset collections and counters.
        buffer.clear();
        boards.clear();
        targetUsed[0] = targetUsed[1] = 0;
        piecesPool.clear();
        modulesPool.clear();
        movingScrews.clear();
        if (animationTimer.isRunning()) {
            animationTimer.stop();
        }

        // 1) Generate boards and screw positions (no colors yet).
        for (int i = 0; i < BOARD_COUNT; i++) {
            boards.add(Board.randomBoard(i, w, h, rand));
        }

        // 2) Ensure the total number of screws is a multiple of 3.
        int total = 0;
        for (Board b : boards) {
            total += b.screws.size();
        }
        int need = (3 - (total % 3)) % 3;
        for (int k = 0; k < need; k++) {
            Board b = boards.get(rand.nextInt(boards.size()));
            Rectangle r = b.rect;
            int sx = r.x + 20 + rand.nextInt(Math.max(1, r.width - 40));
            int sy = r.y + 20 + rand.nextInt(Math.max(1, r.height - 40));
            b.screws.add(new Screw(sx, sy, Color.GRAY));
        }
        total += need;

        // 3) Build the logical color pool in triplets and shuffle it.
        List<TargetColor> logicalPool = Palette.generateTripletShuffled(total);

        // 4) Map logical colors to actual RGB colors and assign to screws.
        //    Note: Screw.color is final; create new Screw instances instead.
        Iterator<TargetColor> it = logicalPool.iterator();
        for (Board b : boards) {
            List<Screw> replaced = new ArrayList<>(b.screws.size());
            for (Screw s : b.screws) {
                if (it.hasNext()) {
                    Color mapped = ColorUtils.fromTargetColor(it.next());
                    replaced.add(new Screw(s.x, s.y, mapped));
                } else {
                    replaced.add(s);
                }
            }
            b.screws.clear();
            b.screws.addAll(replaced);
        }

        // 5) Initialize pools: count loose pieces and 3-piece modules.
        Map<Color, Integer> countByColor = new HashMap<>();
        for (Board b : boards) {
            for (Screw s : b.screws) {
                Color c = s.color;
                countByColor.put(c, countByColor.getOrDefault(c, 0) + 1);
            }
        }
        for (Map.Entry<Color, Integer> e : countByColor.entrySet()) {
            int pieces = e.getValue();
            piecesPool.put(e.getKey(), pieces);
            modulesPool.put(e.getKey(), pieces / 3);
        }

        // 6) Initialize target colors: pick two different colors from modules.
        targetColors[0] = pickTargetColor(null);
        targetColors[1] = pickTargetColor(targetColors[0]);
        if (targetColors[0] == null && !countByColor.isEmpty()) {
            targetColors[0] = countByColor.keySet().iterator().next();
        }
        if (targetColors[1] == null) {
            targetColors[1] = targetColors[0];
        }
    }

    /**
     * Picks a target color from the module pool, avoiding the given color
     * and preferring colors with available modules.
     *
     * @param avoid color to avoid (may be {@code null})
     * @return a chosen color, or {@code null} if no suitable color exists
     */
    private Color pickTargetColor(final Color avoid) {
        List<Color> candidates = new ArrayList<>();
        for (Map.Entry<Color, Integer> e : modulesPool.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                if (avoid == null || !sameColor(e.getKey(), avoid)) {
                    candidates.add(e.getKey());
                }
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(rand.nextInt(candidates.size()));
    }

    /* ================================
     * Interaction and state advance
     * ================================ */

    /**
     * Handles a mouse click.
     * <p>Searches boards from front to back to find a clicked visible screw,
     * then attempts to move it.</p>
     *
     * @param p the click position
     */
    private void handleClick(final Point p) {
        for (int i = boards.size() - 1; i >= 0; i--) {
            Board b = boards.get(i);
            List<Board> above = boards.subList(i + 1, boards.size());
            Screw s = b.getClickedScrew(p, above);
            if (s != null) {
                moveScrew(s, b);
                return;
            }
        }
    }

    /**
     * Moves a screw either to a target slot (if a matching color slot exists)
     * or into the buffer (if space remains). Losing condition is triggered if
     * neither is possible.
     *
     * @param s    the screw to move
     * @param from the board holding the screw
     */
    private void moveScrew(final Screw s, final Board from) {
        Color c = s.color;
        from.removeScrew(s);

        int width = Math.max(getWidth(), 1);
        int height = Math.max(getHeight(), 1);

        int targetIndex = findAvailableTarget(c);
        if (targetIndex >= 0) {
            int slotIndex = targetUsed[targetIndex];
            Point end = computeTargetSlotCenter(targetIndex, slotIndex, width, height);
            startMovingScrew(c, s.x, s.y, end.x, end.y, () -> {
                applyTargetFill(targetIndex, c);
                afterBoardUpdate(from);
                repaint();
            });
            return;
        }

        if (buffer.size() < BUFFER_SIZE) {
            int slotIndex = buffer.size();
            Point end = computeBufferSlotCenter(slotIndex, width, height);
            startMovingScrew(c, s.x, s.y, end.x, end.y, () -> {
                buffer.add(s);
                autoTransfer();
                afterBoardUpdate(from);
                repaint();
            });
        } else {
            afterBoardUpdate(from);
            lose();
        }
    }

    /**
     * Automatically transfers screws from the buffer to targets while possible.
     * <p>Continues until no more moves can be made in a single pass.</p>
     */
    private void autoTransfer() {
        boolean moved;
        do {
            moved = false;
            for (int i = 0; i < buffer.size(); ) {
                Screw s = buffer.get(i);
                boolean placed = false;
                for (int t = 0; t < 2; t++) {
                    if (sameColor(s.color, targetColors[t]) && targetUsed[t] < TARGET_SLOTS) {
                        targetUsed[t]++;
                        buffer.remove(i);
                        decPiece(s.color);
                        placed = true;
                        moved = true;
                        if (targetUsed[t] == TARGET_SLOTS) {
                            targetUsed[t] = 0;
                            decModule(s.color);
                            targetColors[t] = pickTargetColor(targetColors[1 - t]);
                            if (targetColors[t] == null) {
                                targetColors[t] = targetColors[1 - t];
                            }
                        }
                        break;
                    }
                }
                if (!placed) {
                    i++;
                }
            }
        } while (moved);
        checkWin();
    }

    /**
     * Called after a click or a transfer to update board state and re-check win.
     *
     * @param b the board that was updated
     */
    private void afterBoardUpdate(final Board b) {
        if (b.allRemoved()) {
            boards.remove(b);
        }
        checkWin();
    }

    /**
     * Checks whether the win condition is met.
     * <p>Early exits if the buffer is not empty.</p>
     */
    private void checkWin() {
        if (!buffer.isEmpty()) {
            return;
        }
        for (Map.Entry<Color, Integer> e : piecesPool.entrySet()) {
            if (e.getValue() != null && e.getValue() > 0) {
                return;
            }
        }
        for (Board b : boards) {
            if (!b.isHidden() && !b.allRemoved()) {
                return;
            }
        }
        win();
    }

    /**
     * Switches to the “win” state and requests a repaint.
     * Sets {@code gameOver = true} and {@code win = true}.
     */
    private void win() {
        gameOver = true;
        win = true;
        repaint();
    }

    /**
     * Switches to the “lose” state and requests a repaint.
     * Sets {@code gameOver = true} and {@code win = false}.
     */
    private void lose() {
        gameOver = true;
        win = false;
        repaint();
    }

    /* ============================
       Rendering Section: 
       Target / Buffer / End UI
       ============================ */

    /** 
     * Draws the top target panels, showing target colors and progress.
     *
     * @param g2    the graphics context
     * @param w     the canvas width
     * @param topH  the top panel height
     */
    private void drawTargets(final Graphics2D g2, final int w, final int topH) {
        // Layout parameters
        int pad = 24;
        int cardW = (w - pad * 3) / 2;
        int barH = 44;
        int y = 54;

        for (int t = 0; t < 2; t++) {
            // Card position
            int x = pad + t * (cardW + pad);
            int cardX = x - 10;
            int cardY = y - 18;
            int cardWidth = cardW + 20;
            int cardHeight = barH + 52;

            // Drop shadow
            g2.setColor(new Color(0, 0, 0, 28));
            g2.fillRoundRect(cardX + 4, cardY + 8, cardWidth, cardHeight, 18, 18);

            // Card body background
            GradientPaint cardPaint = new GradientPaint(
                cardX, cardY, new Color(255, 255, 255, 235),
                cardX, cardY + cardHeight, new Color(244, 248, 252, 235)
            );
            java.awt.Paint prevPaint = g2.getPaint();
            g2.setPaint(cardPaint);
            g2.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 18, 18);
            g2.setPaint(prevPaint);
            g2.setColor(new Color(205, 210, 220));
            g2.drawRoundRect(cardX, cardY, cardWidth, cardHeight, 18, 18);

            // Title label
            g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g2.setColor(new Color(85, 90, 98));
            g2.drawString("TARGET " + (t + 1), cardX + 18, cardY + 26);

            // Color bar area
            int barX = x;
            int barY = cardY + 32;
            Color base = targetColors[t] != null ? targetColors[t] : new Color(190, 198, 206);
            GradientPaint barPaint = new GradientPaint(
                barX, barY, lightenColor(base, 0.35f),
                barX, barY + barH, darkenColor(base, 0.18f)
            );
            prevPaint = g2.getPaint();
            g2.setPaint(barPaint);
            g2.fillRoundRect(barX, barY, cardW, barH, 12, 12);
            g2.setPaint(prevPaint);
            g2.setColor(new Color(60, 65, 80));
            g2.setStroke(new BasicStroke(1.6f));
            g2.drawRoundRect(barX, barY, cardW, barH, 12, 12);

            // Slot indicators
            int dotR = 16;
            int gap = (cardW - dotR * TARGET_SLOTS) / (TARGET_SLOTS + 1);
            for (int i = 0; i < TARGET_SLOTS; i++) {
                int cx = barX + gap * (i + 1) + dotR * i;
                int cy = barY + (barH - dotR) / 2;
                boolean filled = i < targetUsed[t];
                if (filled) {
                    g2.setColor(new Color(255, 255, 255, 210));
                    g2.fillOval(cx, cy, dotR, dotR);
                    g2.setColor(new Color(50, 50, 70, 120));
                    g2.drawOval(cx, cy, dotR, dotR);
                } else {
                    g2.setColor(new Color(245, 246, 248, 180));
                    g2.fillOval(cx, cy, dotR, dotR);
                    g2.setColor(new Color(210, 214, 220));
                    g2.drawOval(cx, cy, dotR, dotR);
                }
            }

            // Progress text
            String progress = targetUsed[t] + "/" + TARGET_SLOTS + " slots";
            g2.setFont(getFont().deriveFont(12f));
            g2.setColor(new Color(110, 115, 125));
            g2.drawString(progress, barX, barY + barH + 20);
        }
    }

    /** 
     * Draws the lower buffer area (temporary screw slots).
     *
     * @param g2    the graphics context
     * @param w     the panel width
     * @param topH  top panel height
     * @param midH  middle section height
     */
    private void drawBuffer(final Graphics2D g2, final int w, final int topH, final int midH) {
        int pad = 24;
        int slotW = (w - pad * (BUFFER_SIZE + 1)) / BUFFER_SIZE;
        int slotH = 48;
        int y = getHeight() - slotH - 28;

        int cardX = pad - 12;
        int cardWidth = w - (pad - 12) * 2;
        int cardY = y - 32;
        int cardHeight = slotH + 70;

        // Drop shadow and card background
        g2.setColor(new Color(0, 0, 0, 24));
        g2.fillRoundRect(cardX + 4, cardY + 8, cardWidth, cardHeight, 20, 20);
        GradientPaint cardPaint = new GradientPaint(
            cardX, cardY, new Color(255, 255, 255, 235),
            cardX, cardY + cardHeight, new Color(240, 245, 250, 235)
        );
        java.awt.Paint prevPaint = g2.getPaint();
        g2.setPaint(cardPaint);
        g2.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 20, 20);
        g2.setPaint(prevPaint);
        g2.setColor(new Color(205, 210, 220));
        g2.drawRoundRect(cardX, cardY, cardWidth, cardHeight, 20, 20);

        // Title label
        g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
        g2.setColor(new Color(85, 90, 98));
        g2.drawString("BUFFER BAY", cardX + 20, cardY + 28);

        String usage = buffer.size() + "/" + BUFFER_SIZE + " slots used";
        g2.setFont(getFont().deriveFont(12f));
        g2.setColor(new Color(110, 115, 125));
        int usageWidth = g2.getFontMetrics().stringWidth(usage);
        g2.drawString(usage, cardX + cardWidth - usageWidth - 20, cardY + 28);

        // Draw each slot
        for (int i = 0; i < BUFFER_SIZE; i++) {
            int x = pad + i * (slotW + pad);
            GradientPaint slotPaint = new GradientPaint(
                x, y, new Color(252, 253, 255, 235),
                x, y + slotH, new Color(234, 239, 245, 235)
            );
            prevPaint = g2.getPaint();
            g2.setPaint(slotPaint);
            g2.fillRoundRect(x, y, slotW, slotH, 12, 12);
            g2.setPaint(prevPaint);
            g2.setColor(new Color(190, 195, 204));
            g2.drawRoundRect(x, y, slotW, slotH, 12, 12);

            if (i < buffer.size()) {
                Screw screw = buffer.get(i);
                int r = Screw.size();
                int cx = x + slotW / 2 - r / 2;
                int cy = y + slotH / 2 - r / 2;
                Color c = screw.color != null ? screw.color : Color.GRAY;
                GradientPaint screwPaint = new GradientPaint(
                    cx, cy, lightenColor(c, 0.28f),
                    cx, cy + r, darkenColor(c, 0.22f)
                );
                prevPaint = g2.getPaint();
                g2.setPaint(screwPaint);
                g2.fillOval(cx, cy, r, r);
                g2.setPaint(prevPaint);
                g2.setColor(new Color(30, 35, 40, 180));
                g2.drawOval(cx, cy, r, r);
            }
        }
    }

    /**
     * Draws all screws that are currently moving between slots or targets.
     * This method is called every frame during animation.
     *
     * @param g2 the graphics context
     */
    private void drawMovingScrews(final Graphics2D g2) {
        if (movingScrews.isEmpty()) {
            return;
        }
        java.awt.Paint prev = g2.getPaint();
        for (MovingScrew ms : movingScrews) {
            int size = Screw.size();
            int radius = size / 2;
            int x = Math.round(ms.currentX) - radius;
            int y = Math.round(ms.currentY) - radius;
            // Create gradient color for screw appearance
            GradientPaint paint = new GradientPaint(
                x, y, lightenColor(ms.color, 0.28f),
                x, y + size, darkenColor(ms.color, 0.24f)
            );
            g2.setPaint(paint);
            g2.fillOval(x, y, size, size);
            g2.setPaint(prev);
            g2.setColor(new Color(30, 35, 40, 180));
            g2.drawOval(x, y, size, size);
        }
        g2.setPaint(prev);
    }

    /**
     * Draws the end-game overlay with result message and action buttons.
     *
     * @param g2 the graphics context
     * @param w  the panel width
     * @param h  the panel height
     */
    private void drawEndOverlay(final Graphics2D g2, final int w, final int h) {
        // Semi-transparent background
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRect(0, 0, w, h);

        // Result card background
        int pw = 420;
        int ph = 180;
        int px = (w - pw) / 2;
        int py = (h - ph) / 2;
        g2.setColor(new Color(250, 250, 250));
        g2.fillRoundRect(px, py, pw, ph, 16, 16);
        g2.setColor(new Color(70, 70, 70));
        g2.drawRoundRect(px, py, pw, ph, 16, 16);

        // Title (win/lose text)
        g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
        g2.setColor(new Color(50, 50, 50));
        g2.drawString(win ? "You Win!" : "Game Over", px + 24, py + 38);

        // Two buttons: New Game / Quit
        int bw = 160;
        int bh = 44;
        int gap = 24;
        int bx1 = px + (pw - (bw * 2 + gap)) / 2;
        int bx2 = bx1 + bw + gap;
        int by = py + ph - bh - 28;

        btnNewRect = new Rectangle(bx1, by, bw, bh);
        btnQuitRect = new Rectangle(bx2, by, bw, bh);

        drawButton(g2, btnNewRect, "New Game");
        drawButton(g2, btnQuitRect, "Quit");
    }

    /**
     * Draws a single rounded button with a text label.
     *
     * @param g2   the graphics context
     * @param r    the rectangle area of the button
     * @param text the label displayed on the button
     */
    private void drawButton(final Graphics2D g2, final Rectangle r, final String text) {
        // Button background and border
        g2.setColor(new Color(235, 239, 245));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g2.setColor(new Color(90, 96, 102));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);

        // Button label text
        g2.setColor(new Color(40, 40, 40));
        Font f = getFont().deriveFont(Font.BOLD, 14f);
        g2.setFont(f);
        int tw = g2.getFontMetrics().stringWidth(text);
        int th = g2.getFontMetrics().getAscent();
        int tx = r.x + (r.width - tw) / 2;
        int ty = r.y + (r.height + th) / 2 - 3;
        g2.drawString(text, tx, ty);
    }

    /**
     * Handles mouse click events when the end overlay is shown.
     *
     * @param p the mouse click point
     */
    private void handleEndButtons(final Point p) {
        if (btnNewRect != null && btnNewRect.contains(p)) {
            newGame();
        } else if (btnQuitRect != null && btnQuitRect.contains(p)) {
            System.exit(0);
        }
    }

    /**
     * Starts a new round of the game.
     * Reinitializes the board and resets win/loss state.
     */
    private void newGame() {
        initGame(getWidth(), getHeight());
        inited = true;
        gameOver = false;
        win = false;
        repaint();
    }

    /* ============================
       Tools and Utility Functions
       ============================ */

    /**
     * Brightens a given color by the specified ratio.
     *
     * @param color the base color
     * @param ratio how much to lighten (0–1)
     * @return the lightened color
     */
    private Color lightenColor(final Color color, final float ratio) {
        Color base = color != null ? color : new Color(200, 205, 212);
        float r = Math.max(0f, Math.min(1f, ratio));
        int red = Math.min(255, Math.round(base.getRed() + (255 - base.getRed()) * r));
        int green = Math.min(255, Math.round(base.getGreen() + (255 - base.getGreen()) * r));
        int blue = Math.min(255, Math.round(base.getBlue() + (255 - base.getBlue()) * r));
        return new Color(red, green, blue);
    }

    /**
     * Darkens a given color by the specified ratio.
     *
     * @param color the base color
     * @param ratio how much to darken (0–1)
     * @return the darkened color
     */
    private Color darkenColor(final Color color, final float ratio) {
        Color base = color != null ? color : new Color(180, 185, 192);
        float r = Math.max(0f, Math.min(1f, ratio));
        int red = Math.max(0, Math.round(base.getRed() * (1f - r)));
        int green = Math.max(0, Math.round(base.getGreen() * (1f - r)));
        int blue = Math.max(0, Math.round(base.getBlue() * (1f - r)));
        return new Color(red, green, blue);
    }

    /**
     * Finds a target index that still has available slots for the given color.
     *
     * @param color the target color to search for
     * @return the index of the available target, or -1 if none found
     */
    private int findAvailableTarget(final Color color) {
        for (int t = 0; t < 2; t++) {
            if (sameColor(color, targetColors[t]) && targetUsed[t] < TARGET_SLOTS) {
                return t;
            }
        }
        return -1;
    }

    /**
     * Fills a target slot with the given color and triggers the next action.
     *
     * @param targetIndex which target to fill
     * @param color       the color to fill
     */
    private void applyTargetFill(final int targetIndex, final Color color) {
        targetUsed[targetIndex]++;
        decPiece(color);
        if (targetUsed[targetIndex] == TARGET_SLOTS) {
            targetUsed[targetIndex] = 0;
            decModule(color);
            targetColors[targetIndex] = pickTargetColor(targetColors[1 - targetIndex]);
            if (targetColors[targetIndex] == null) {
                targetColors[targetIndex] = targetColors[1 - targetIndex];
            }
        }
        autoTransfer();
        checkWin();
    }

    /**
     * Computes the center coordinates of a target slot.
     *
     * @param targetIndex the index of the target panel (0 or 1)
     * @param slotIndex   the index of the slot inside that target
     * @param width       total panel width
     * @param height      total panel height
     * @return a Point representing the center position of that slot
     */
    private Point computeTargetSlotCenter(
        final int targetIndex, 
        final int slotIndex, 
        final int width, 
        final int height) {
        int pad = 24;
        int cardW = (width - pad * 3) / 2;
        int barH = 44;
        int y = 54;
        int barX = pad + targetIndex * (cardW + pad);
        int cardY = y - 18;
        int barY = cardY + 32;
        int dotR = 16;
        int gap = (cardW - dotR * TARGET_SLOTS) / (TARGET_SLOTS + 1);
        int topLeftX = barX + gap * (slotIndex + 1) + dotR * slotIndex;
        int topLeftY = barY + (barH - dotR) / 2;
        return new Point(topLeftX + dotR / 2, topLeftY + dotR / 2);
    }

    /**
     * Computes the center coordinates of a buffer slot.
     *
     * @param slotIndex the index of the buffer slot
     * @param width     total panel width
     * @param height    total panel height
     * @return a Point representing the slot center position
     */
    private Point computeBufferSlotCenter(final int slotIndex, final int width, final int height) {
        int pad = 24;
        int slotW = (width - pad * (BUFFER_SIZE + 1)) / BUFFER_SIZE;
        int slotH = 48;
        int y = height - slotH - 28;
        int x = pad + slotIndex * (slotW + pad);
        return new Point(x + slotW / 2, y + slotH / 2);
    }

    /**
     * Starts an animation for a moving screw traveling from one position to another.
     * The movement is animated over time and triggers a callback when finished.
     *
     * @param color        screw color
     * @param startX       starting x coordinate
     * @param startY       starting y coordinate
     * @param endX         target x coordinate
     * @param endY         target y coordinate
     * @param onComplete   action to run when movement ends
     */
    private void startMovingScrew(final Color color, final float startX, final float startY,
        final float endX, final float endY, final Runnable onComplete) {
        MovingScrew ms = new MovingScrew(color, startX, startY, endX, endY, 
                                         MOVE_DURATION_MS, onComplete);
        movingScrews.add(ms);
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
        repaint();
    }

    /**
     * Called every animation tick to update all active moving screws.
     * Removes completed animations and repaints the panel.
     */
    private void onAnimationTick() {
        if (movingScrews.isEmpty()) {
            animationTimer.stop();
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<MovingScrew> it = movingScrews.iterator();
        while (it.hasNext()) {
            MovingScrew ms = it.next();
            ms.update(now);
            if (ms.isFinished()) {
                it.remove();
                ms.finish();
            }
        }
        if (movingScrews.isEmpty()) {
            animationTimer.stop();
        }
        repaint();
    }

    /**
     * Easing function (ease-out cubic).
     * Produces a smooth deceleration effect for animation motion.
     *
     * @param t normalized time value (0.0–1.0)
     * @return eased progress value
     */
    private static double easeOutCubic(final double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));
        double inv = clamped - 1.0;
        return inv * inv * inv + 1.0;
    }

    /**
     * Compares two colors for equality, safely handling null values.
     *
     * @param a first color
     * @param b second color
     * @return true if colors are equal (or both null), false otherwise
     */
    private boolean sameColor(final Color a, final Color b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    /**
     * Decreases the count of the specified color in the piece pool by 1.
     *
     * @param c the color to decrement
     */
    private void decPiece(final Color c) {
        piecesPool.put(c, Math.max(0, piecesPool.getOrDefault(c, 0) - 1));
    }

    /**
     * Decreases the count of the specified color in the module pool by 1.
     *
     * @param c the color to decrement
     */
    private void decModule(final Color c) {
        int now = Math.max(0, modulesPool.getOrDefault(c, 0) - 1);
        modulesPool.put(c, now);
    }

    /**
     * Represents a single moving screw animation between two points.
     * Each screw interpolates its position over time and optionally triggers
     * a completion callback once the animation ends.
     */
    private static final class MovingScrew {
        private final Color color;
        private final float startX;
        private final float startY;
        private final float endX;
        private final float endY;
        private final long startTime;
        private final int durationMs;
        private final Runnable onComplete;

        private float currentX;
        private float currentY;
        private boolean finished;

        /**
         * Creates a new moving screw animation.
         *
         * @param color       screw color
         * @param startX      starting x coordinate
         * @param startY      starting y coordinate
         * @param endX        target x coordinate
         * @param endY        target y coordinate
         * @param durationMs  total animation duration in milliseconds
         * @param onComplete  callback executed when the movement finishes
         */
        private MovingScrew(final Color color, final float startX, final float startY,
            final float endX, final float endY, final int durationMs, final Runnable onComplete) {
            this.color = color;
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.durationMs = Math.max(1, durationMs);
            this.onComplete = onComplete;
            this.startTime = System.currentTimeMillis();
            update(this.startTime);
        }

        /**
         * Updates the screw’s position based on the elapsed time.
         *
         * @param now the current system time in milliseconds
         */
        private void update(final long now) {
            double elapsed = (now - startTime) / (double) durationMs;
            double eased = easeOutCubic(elapsed);
            currentX = startX + (float) ((endX - startX) * eased);
            currentY = startY + (float) ((endY - startY) * eased);
            finished = elapsed >= 1.0;
        }

        /**
         * Checks whether this animation has finished.
         *
         * @return true if completed, false otherwise
         */
        private boolean isFinished() {
            return finished;
        }

        /**
         * Executes the completion callback (if any).
         */
        private void finish() {
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }
}
