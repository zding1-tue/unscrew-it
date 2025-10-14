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
 * 关卡状态：保存当前关卡中的所有游戏要素.
 * <p>包含 7 块板、目标槽与缓冲区.</p>
 * <p>约定：索引越大的板位于越上层.</p>
 */
public final class LevelState {

    /** 当前关卡中包含的所有板. */
    private final List<Board> boards = new ArrayList<>(Rules.BOARD_COUNT);

    /** 顶部的两个目标槽. */
    private final TargetSlots targetSlots;

    /** 缓冲区. */
    private final Buffer buffer = new Buffer();

    /** 随机数生成器. */
    private final Random random = new Random();

    /**
     * 使用画布尺寸创建新的关卡状态.
     *
     * @param canvasW 画布宽度（像素）.
     * @param canvasH 画布高度（像素）.
     */
    public LevelState(int canvasW, int canvasH) {
        for (int i = 0; i < Rules.BOARD_COUNT; i++) {
            boards.add(Board.randomBoard(i, canvasW, canvasH, random));
        }
        List<TargetColor> palette = Palette.defaultPalette();
        Collections.shuffle(palette, random);
        this.targetSlots = new TargetSlots(palette.get(0), palette.get(1));
    }

    /**
     * 获取当前关卡的全部板集合.
     *
     * @return 板的列表，索引越大越上层.
     */
    public List<Board> boards() {
        return boards;
    }

    /**
     * 获取目标槽对象.
     *
     * @return 目标槽.
     */
    public TargetSlots targets() {
        return targetSlots;
    }

    /**
     * 获取缓冲区对象.
     *
     * @return 缓冲区.
     */
    public Buffer buffer() {
        return buffer;
    }

    /**
     * 获取内部使用的随机数生成器.
     *
     * @return 随机数生成器.
     */
    public Random random() {
        return random;
    }

    /**
     * 判断当前是否所有板面都已无螺丝.
     *
     * @return 若所有板无螺丝则为 {@code true}，否则为 {@code false}.
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
     * 获取位于指定板之上的所有板的矩形边界，用于遮挡计算.
     *
     * @param holder 包含螺丝的板.
     * @return 位于其上的板的矩形列表.
     */
    public List<Rectangle> coveringRectsFor(Board holder) {
        List<Rectangle> rects = new ArrayList<>();
        int idx = boards.indexOf(holder);
        if (idx < 0) {
            return rects;
        }
        for (int i = boards.size() - 1; i > idx; i--) {
            rects.add(boards.get(i).rect);
        }
        return rects;
    }

    /**
     * 从关卡中移除一个给定的螺丝.
     *
     * @param screw 需要移除的螺丝.
     * @return 若移除成功则为 {@code true}，否则为 {@code false}.
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
     * 查找指定螺丝所在的板.
     *
     * @param screw 目标螺丝.
     * @return 包含该螺丝的板，若未找到返回 {@code null}.
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
