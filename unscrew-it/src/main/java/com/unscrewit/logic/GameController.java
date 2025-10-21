package com.unscrewit.logic;

import com.unscrewit.Board;
import com.unscrewit.LevelState;
import com.unscrewit.Screw;
import com.unscrewit.model.Buffer;
import com.unscrewit.model.ColorMapping;
import com.unscrewit.model.TargetColor;
import com.unscrewit.rules.Rules;
import com.unscrewit.logic.BufferOverflowException;
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
 * 游戏控制器：处理点击事件并推进核心游戏流程.
 * <p>流程包括命中检测、遮挡判定、目标与缓冲区流转、自动转运、螺丝移除与胜负检查。</p>
 */
public final class GameController {

    /** 关卡状态（包含板、目标、缓冲、随机源等）。 */
    private final LevelState state;

    /** 胜利回调。 */
    private final Runnable onWin;

    /** 失败回调（缓冲溢出）。 */
    private final Runnable onFail;

    /**
     * 构造控制器。
     *
     * @param state 关卡状态。
     * @param onWin 胜利时回调。
     * @param onFail 失败时回调。
     */
    public GameController(final LevelState state, final Runnable onWin, final Runnable onFail) {
        this.state = state;
        this.onWin = onWin;
        this.onFail = onFail;
    }

    /**
     * 处理一次点击（由画布传入像素坐标）。
     *
     * @param p 点击点。
     */
    public void handleClick(final Point p) {
        // 命中检测：取最上层的被点中的螺丝。
        final Screw s = findTopmostScrewAt(p);
        if (s == null) {
            return;
        }

        // 遮挡判定：小于 50% 遮挡才可点击。
        final int radius = Screw.size() / 2;
        final Board holder = state.findHolder(s);
        final List<Rectangle> covers = state.coveringRectsFor(holder);
        final boolean clickable = Visibility.isClickable(
                s.x, s.y, radius, covers, Rules.CLICKABLE_COVERAGE_RATIO);
        if (!clickable) {
            return;
        }

        // 逻辑色。
        final TargetColor logicColor = ColorMapping.toTargetColor(s.color);

        // 放入目标或缓冲。
        final boolean placedToTarget = state.targets().tryPlace(logicColor);
        if (placedToTarget) {
            // 从板面移除该螺丝。
            state.removeScrew(s);

            // 自动从缓冲向目标转运（如果有可放入的）。
            drainBufferToTargets();

            // 目标满三则刷新为新色（6.1：采用模块池驱动的选色策略）。
            refreshTargetsIfNeeded();

            // 检查胜利。
            if (state.allCleared()) {
                onWin.run();
            }
        } else {
            // 放入缓冲；若溢出则失败。
            try {
                state.buffer().push(logicColor);
            } catch (BufferOverflowException ex) {
                onFail.run();
                return;
            }
            // 尝试从缓冲继续转运（可能刚好能放）。
            drainBufferToTargets();
            // 如果因为转运导致某侧满三，则刷新。
            refreshTargetsIfNeeded();
        }
    }

    /**
     * 在需要时刷新目标颜色（当某侧满三）。本实现为 6.1 版本：使用主体区“三连小模块池”驱动目标色选择，优先从“可点击且有模块”的颜色中选；同一轮尽量避免双槽同色（除非候选仅剩一种）。 
     */
    private void refreshTargetsIfNeeded() {
        final boolean leftFull = state.targets().leftFull();
        final boolean rightFull = state.targets().rightFull();
        if (!leftFull && !rightFull) {
            return;
        }

        // 即时构建模块池与可点击颜色集合。
        final Map<TargetColor, Integer> modulePool = buildModulePoolFromBoards();
        final Set<TargetColor> clickable = computeClickableColors();

        TargetColor newLeft = state.targets().leftColor();
        TargetColor newRight = state.targets().rightColor();

        if (leftFull && rightFull) {
            // 同一轮刷新两个槽：先选一个，再临时占用该色一个模块名额后选第二个，尽量避免同色。
            TargetColor c0 = pickTargetColorByModule(modulePool, clickable, null);
            if (c0 != null) {
                modulePool.put(c0, Math.max(0, modulePool.getOrDefault(c0, 0) - 1));
            }
            TargetColor c1 = pickTargetColorByModule(modulePool, clickable, c0);
            if (c1 == null) {
                // 模块池可能只有一种颜色，允许同色。
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
        } else if (leftFull) {
            TargetColor c = pickTargetColorByModule(modulePool, clickable, state.targets().rightColor());
            if (c == null) {
                c = pickNewColorExcluding(state.targets().rightColor());
            }
            newLeft = c;
        } else { // rightFull
            TargetColor c = pickTargetColorByModule(modulePool, clickable, state.targets().leftColor());
            if (c == null) {
                c = pickNewColorExcluding(state.targets().leftColor());
            }
            newRight = c;
        }

        state.targets().refreshColors(leftFull, newLeft, rightFull, newRight);
    }

    /**
     * 从缓冲区尽可能将元素转运到目标槽。若队首颜色可放入目标则弹出并放入；一旦队首不可放入则停止（保持原有“队列语义”）。 
     */
    private void drainBufferToTargets() {
        final Buffer buffer = state.buffer();
        while (true) {
            // 取队首（Buffer 未提供 peek，这里用 pop 后决定是否放回）。
            final TargetColor c = buffer.pop();
            if (c == null) {
                return;
            }
            if (state.targets().tryPlace(c)) {
                // 放入成功：继续尝试下一项。
                continue;
            } else {
                // 放不进去：把元素放回队尾并停止。
                try {
                    buffer.push(c);
                } catch (BufferOverflowException ignore) {
                    // 理论不会发生：我们刚刚从该缓冲弹出了一个元素。
                }
                return;
            }
        }
    }

    /**
     * 构建“颜色 → 可消三连模块数量”的映射；仅统计主体区在场螺丝，按 3 为单位向下取整。 
     *
     * @return 模块池映射。
     */
    private Map<TargetColor, Integer> buildModulePoolFromBoards() {
        final Map<TargetColor, Integer> count = new HashMap<>();
        for (Board b : state.boards()) {
            for (Screw s : b.screws) {
                final TargetColor c = ColorMapping.toTargetColor(s.color);
                count.merge(c, 1, Integer::sum);
            }
        }
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
     * 计算当前“可点击”的逻辑颜色集合（与点击命中口径一致）。 
     *
     * @return 可点击颜色集合。
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
     * 从模块池中选择目标色：优先“可点击 ∩ 模块数>0”，否则“模块数>0”；尽量回避 excluded。 
     *
     * @param modulePool 模块池。
     * @param clickable 可点击颜色集合。
     * @param excluded 需要尽量回避的颜色（可为 null）。
     * @return 选中的颜色或 {@code null}。
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
            if (excluded != null && c == excluded) {
                // 暂避该色，若最终无候选再考虑回退。
                continue;
            }
            if (clickable.contains(c)) {
                p1.add(c);
            } else {
                p2.add(c);
            }
        }

        final Comparator<TargetColor> cmp = Comparator
                .comparingInt((TargetColor c) -> modulePool.getOrDefault(c, 0))
                .reversed()
                .thenComparingInt(Enum::ordinal);

        p1.sort(cmp);
        p2.sort(cmp);

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
     * 在所有板中查找点击位置命中的最上层螺丝。 
     *
     * @param p 点击坐标。
     * @return 命中的螺丝；若未命中则为 {@code null}。
     */
    private Screw findTopmostScrewAt(final Point p) {
        // 假定 LevelState.boards() 自下而上排序，则自后向前遍历即可得到最上层命中项。
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
     * 随机挑选一个不等于排除色的目标颜色（保留旧逻辑作为兜底）。 
     *
     * @param exclude 需要排除的颜色。
     * @return 新的目标颜色。
     */
    private TargetColor pickNewColorExcluding(final TargetColor exclude) {
        final TargetColor[] vals = TargetColor.values();
        TargetColor candidate = vals[state.random().nextInt(vals.length)];
        if (candidate == exclude) {
            candidate = vals[(candidate.ordinal() + 1) % vals.length];
        }
        return candidate;
    }
}
