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
 * 游戏主画布: 负责关卡生成、交互与 UI 渲染.
 * 本版本实现: 半透明板子、板清空后淡出移除、结束按钮 UI、以及“生成即保证可解”.
 */
public class GamePanel extends JPanel {

    /** 目标槽位个数. */
    private static final int TARGET_SLOTS = 3;

    /** 缓冲区容量. */
    private static final int BUFFER_SIZE = 4;

    /** 板子数量. */
    private static final int BOARD_COUNT = 7;

    /** 标题文本. */
    private static final String TITLE = "Click visible screws to match the target colors";

    /** 螺丝移动动画时长(毫秒). */
    private static final int MOVE_DURATION_MS = 260;

    /** 随机源. */
    private final Random rand = new Random();

    /** 板子集合(下标越大层级越高). */
    private final List<Board> boards = new ArrayList<>();

    /** 下方缓冲区. */
    private final List<Screw> buffer = new ArrayList<>();

    /** 顶部两组目标颜色. */
    private final Color[] targetColors = new Color[2];

    /** 顶部两组目标当前已填数量. */
    private final int[] targetUsed = new int[2];

    /** 剩余“颗数池”. */
    private final Map<Color, Integer> piecesPool = new HashMap<>();

    /** 剩余“模块池”. */
    private final Map<Color, Integer> modulesPool = new HashMap<>();

    /** 正在播放的螺丝移动动画. */
    private final List<MovingScrew> movingScrews = new ArrayList<>();

    /** 动画定时器. */
    private final Timer animationTimer;

    /** 是否已经初始化. */
    private boolean inited = false;

    /** 是否处于游戏结束状态. */
    private boolean gameOver = false;

    /** 是否胜利. */
    private boolean win = false;

    /** 结束时按钮区域. */
    private Rectangle btnNewRect;
    private Rectangle btnQuitRect;

    /**
     * 构造函数.
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
     * 兼容旧框架: 接收外部 LevelState, 这里不需要实际使用.
     *
     * @param state 关卡状态对象.
     */
    public void setLevelState(final LevelState state) {
        // no-op.
    }

    /**
     * 兼容旧框架: 接收外部控制器, 这里不需要实际使用.
     *
     * @param controller 控制器对象.
     */
    public void setController(final GameController controller) {
        // no-op.
    }

    /** 首次挂载后进行一次初始化. */
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

    /** 画布建议尺寸. */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(1050, 700);
    }

    /** 渲染流程. */
    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        final Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

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

        // 顶部标题.
        g2.setColor(new Color(70, 70, 70));
        g2.setFont(getFont().deriveFont(Font.BOLD, 16f));
        g2.drawString(TITLE, 24, 28);

        // 目标与缓冲 UI.
        drawTargets(g2, w, topH);
        drawBuffer(g2, w, topH, midH);

        // 板子.
        for (int i = 0; i < boards.size(); i++) {
            List<Board> above = boards.subList(i + 1, boards.size());
            boards.get(i).draw(g2, above);
        }

        drawMovingScrews(g2);

        // 结束遮罩与按钮.
        if (gameOver) {
            drawEndOverlay(g2, w, h);
        }
    }

    /* ===========================
       关卡初始化(保证可解)
       =========================== */

    /**
     * 初始化一局游戏, 实现“生成即保证可解”.
     *
     * @param w 画布宽度.
     * @param h 画布高度.
     */
    private void initGame(final int w, final int h) {
        gameOver = false;
        win = false;
        buffer.clear();
        boards.clear();
        targetUsed[0] = targetUsed[1] = 0;
        piecesPool.clear();
        modulesPool.clear();
        movingScrews.clear();
        if (animationTimer.isRunning()) {
            animationTimer.stop();
        }

        // 1) 生成板子与螺丝位置(先不着色).
        for (int i = 0; i < BOARD_COUNT; i++) {
            boards.add(Board.randomBoard(i, w, h, rand));
        }

        // 2) 确保总数量为 3 的倍数: 如有需要补 1~2 颗.
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

        // 3) 使用逻辑调色板生成“三连模块”并洗牌.
        List<TargetColor> logicalPool = Palette.generateTripletShuffled(total);

        // 4) 将逻辑颜色映射为实际 RGB 颜色并发放到每颗螺丝.
        //    注意: Screw.color 是 final, 不能赋值, 需创建新 Screw 替换.
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

        // 5) 初始化池: 模块池与颗数池.
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

        // 6) 初始目标色: 从模块池>0的颜色中随机选择两种不同颜色.
        targetColors[0] = pickTargetColor(null);
        targetColors[1] = pickTargetColor(targetColors[0]);
        if (targetColors[0] == null && !countByColor.isEmpty()) {
            targetColors[0] = countByColor.keySet().iterator().next();
        }
        if (targetColors[1] == null) {
            targetColors[1] = targetColors[0];
        }
    }

    /** 选择一个目标颜色, 要求模块>0 且尽量不同于 avoid. */
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

    /* ===========================
       交互与状态推进
       =========================== */

    /** 处理鼠标点击. */
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

    /** 将一颗螺丝搬运到目标或缓冲. */
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

    /** 自动从缓冲向目标搬运, 直到不能再搬为止. */
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

    /** 点击或转运后更新板状态并检测胜负. */
    private void afterBoardUpdate(final Board b) {
        if (b.allRemoved()) {
            boards.remove(b);
        }
        checkWin();
    }

    /** 胜利检测. */
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

    /** 胜利状态. */
    private void win() {
        gameOver = true;
        win = true;
        repaint();
    }

    /** 失败状态. */
    private void lose() {
        gameOver = true;
        win = false;
        repaint();
    }

    /* ===========================
       绘制: 目标 / 缓冲 / 结束 UI
       =========================== */

    /** 绘制顶部目标条. */
    private void drawTargets(final Graphics2D g2, final int w, final int topH) {
        int pad = 24;
        int cardW = (w - pad * 3) / 2;
        int barH = 44;
        int y = 54;

        for (int t = 0; t < 2; t++) {
            int x = pad + t * (cardW + pad);
            int cardX = x - 10;
            int cardY = y - 18;
            int cardWidth = cardW + 20;
            int cardHeight = barH + 52;

            // 阴影.
            g2.setColor(new Color(0, 0, 0, 28));
            g2.fillRoundRect(cardX + 4, cardY + 8, cardWidth, cardHeight, 18, 18);

            // 卡片主体.
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

            // 标题.
            g2.setFont(getFont().deriveFont(Font.BOLD, 13f));
            g2.setColor(new Color(85, 90, 98));
            g2.drawString("TARGET " + (t + 1), cardX + 18, cardY + 26);

            // 彩条区域.
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

            // 三个槽位圆点.
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

            // 状态文本.
            String progress = targetUsed[t] + "/" + TARGET_SLOTS + " slots";
            g2.setFont(getFont().deriveFont(12f));
            g2.setColor(new Color(110, 115, 125));
            g2.drawString(progress, barX, barY + barH + 20);
        }
    }

    /** 绘制底部缓冲区. */
    private void drawBuffer(final Graphics2D g2, final int w, final int topH, final int midH) {
        int pad = 24;
        int slotW = (w - pad * (BUFFER_SIZE + 1)) / BUFFER_SIZE;
        int slotH = 48;
        int y = getHeight() - slotH - 28;

        int cardX = pad - 12;
        int cardWidth = w - (pad - 12) * 2;
        int cardY = y - 32;
        int cardHeight = slotH + 70;

        // 阴影与卡片.
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

        // 标题与状态.
        g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
        g2.setColor(new Color(85, 90, 98));
        g2.drawString("BUFFER BAY", cardX + 20, cardY + 28);

        String usage = buffer.size() + "/" + BUFFER_SIZE + " slots used";
        g2.setFont(getFont().deriveFont(12f));
        g2.setColor(new Color(110, 115, 125));
        int usageWidth = g2.getFontMetrics().stringWidth(usage);
        g2.drawString(usage, cardX + cardWidth - usageWidth - 20, cardY + 28);

        // 缓冲槽位.
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

    /** 绘制运动中的螺丝动画. */
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

    /** 绘制结束遮罩与按钮. */
    private void drawEndOverlay(final Graphics2D g2, final int w, final int h) {
        // 半透明遮罩.
        g2.setColor(new Color(0, 0, 0, 90));
        g2.fillRect(0, 0, w, h);

        // 面板.
        int pw = 420;
        int ph = 180;
        int px = (w - pw) / 2;
        int py = (h - ph) / 2;
        g2.setColor(new Color(250, 250, 250));
        g2.fillRoundRect(px, py, pw, ph, 16, 16);
        g2.setColor(new Color(70, 70, 70));
        g2.drawRoundRect(px, py, pw, ph, 16, 16);

        // 标题.
        g2.setFont(getFont().deriveFont(Font.BOLD, 18f));
        g2.setColor(new Color(50, 50, 50));
        g2.drawString(win ? "You Win!" : "Game Over", px + 24, py + 38);

        // 两个按钮.
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

    /** 绘制一个简单按钮. */
    private void drawButton(final Graphics2D g2, final Rectangle r, final String text) {
        g2.setColor(new Color(235, 239, 245));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 10, 10);
        g2.setColor(new Color(90, 96, 102));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 10, 10);

        g2.setColor(new Color(40, 40, 40));
        Font f = getFont().deriveFont(Font.BOLD, 14f);
        g2.setFont(f);
        int tw = g2.getFontMetrics().stringWidth(text);
        int th = g2.getFontMetrics().getAscent();
        int tx = r.x + (r.width - tw) / 2;
        int ty = r.y + (r.height + th) / 2 - 3;
        g2.drawString(text, tx, ty);
    }

    /** 结束状态下处理按钮点击. */
    private void handleEndButtons(final Point p) {
        if (btnNewRect != null && btnNewRect.contains(p)) {
            newGame();
        } else if (btnQuitRect != null && btnQuitRect.contains(p)) {
            System.exit(0);
        }
    }

    /** 重开一局. */
    private void newGame() {
        initGame(getWidth(), getHeight());
        inited = true;
        gameOver = false;
        win = false;
        repaint();
    }

    /* ===========================
       工具与池操作
       =========================== */

    /** 提亮颜色. */
    private Color lightenColor(final Color color, final float ratio) {
        Color base = color != null ? color : new Color(200, 205, 212);
        float r = Math.max(0f, Math.min(1f, ratio));
        int red = Math.min(255, Math.round(base.getRed() + (255 - base.getRed()) * r));
        int green = Math.min(255, Math.round(base.getGreen() + (255 - base.getGreen()) * r));
        int blue = Math.min(255, Math.round(base.getBlue() + (255 - base.getBlue()) * r));
        return new Color(red, green, blue);
    }

    /** 变暗颜色. */
    private Color darkenColor(final Color color, final float ratio) {
        Color base = color != null ? color : new Color(180, 185, 192);
        float r = Math.max(0f, Math.min(1f, ratio));
        int red = Math.max(0, Math.round(base.getRed() * (1f - r)));
        int green = Math.max(0, Math.round(base.getGreen() * (1f - r)));
        int blue = Math.max(0, Math.round(base.getBlue() * (1f - r)));
        return new Color(red, green, blue);
    }

    /** 查找可用目标槽位. */
    private int findAvailableTarget(final Color color) {
        for (int t = 0; t < 2; t++) {
            if (sameColor(color, targetColors[t]) && targetUsed[t] < TARGET_SLOTS) {
                return t;
            }
        }
        return -1;
    }

    /** 应用目标槽填充并驱动后续逻辑. */
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

    /** 计算目标槽中心位置. */
    private Point computeTargetSlotCenter(final int targetIndex, final int slotIndex, final int width, final int height) {
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

    /** 计算缓冲槽中心位置. */
    private Point computeBufferSlotCenter(final int slotIndex, final int width, final int height) {
        int pad = 24;
        int slotW = (width - pad * (BUFFER_SIZE + 1)) / BUFFER_SIZE;
        int slotH = 48;
        int y = height - slotH - 28;
        int x = pad + slotIndex * (slotW + pad);
        return new Point(x + slotW / 2, y + slotH / 2);
    }

    /** 启动一段螺丝移动动画. */
    private void startMovingScrew(final Color color, final float startX, final float startY,
        final float endX, final float endY, final Runnable onComplete) {
        MovingScrew ms = new MovingScrew(color, startX, startY, endX, endY, MOVE_DURATION_MS, onComplete);
        movingScrews.add(ms);
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
        repaint();
    }

    /** 动画时钟回调. */
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

    /** 缓动函数 (ease-out cubic). */
    private static double easeOutCubic(final double t) {
        double clamped = Math.max(0.0, Math.min(1.0, t));
        double inv = clamped - 1.0;
        return inv * inv * inv + 1.0;
    }

    /** 颜色相等判定(处理 null). */
    private boolean sameColor(final Color a, final Color b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }

    /** 颗数池 -1. */
    private void decPiece(final Color c) {
        piecesPool.put(c, Math.max(0, piecesPool.getOrDefault(c, 0) - 1));
    }

    /** 模块池 -1. */
    private void decModule(final Color c) {
        int now = Math.max(0, modulesPool.getOrDefault(c, 0) - 1);
        modulesPool.put(c, now);
    }

    /** 定义单颗螺丝的移动动画状态. */
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

        private void update(final long now) {
            double elapsed = (now - startTime) / (double) durationMs;
            double eased = easeOutCubic(elapsed);
            currentX = startX + (float) ((endX - startX) * eased);
            currentY = startY + (float) ((endY - startY) * eased);
            finished = elapsed >= 1.0;
        }

        private boolean isFinished() {
            return finished;
        }

        private void finish() {
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }
}
