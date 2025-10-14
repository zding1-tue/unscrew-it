package com.unscrewit.logic;

import com.unscrewit.Board;
import com.unscrewit.LevelState;
import com.unscrewit.Screw;
import com.unscrewit.model.ColorMapping;
import com.unscrewit.model.TargetColor;
import com.unscrewit.rules.Rules;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * 游戏控制器：处理点击事件并推进核心游戏流程.
 * <p>流程包括命中检测、遮挡判定、目标与缓冲区流转、自动转运、螺丝移除与胜负检查.</p>
 */
public final class GameController {

    /** 关卡状态. */
    private final LevelState state;

    /** 胜利回调. */
    private final Runnable onWin;

    /** 失败回调. */
    private final Runnable onFail;

    /**
     * 使用关卡状态与回调创建控制器.
     *
     * @param state 关卡状态.
     * @param onWin 胜利回调，可为 {@code null}.
     * @param onFail 失败回调，可为 {@code null}.
     */
    public GameController(LevelState state, Runnable onWin, Runnable onFail) {
        this.state = state;
        this.onWin = onWin;
        this.onFail = onFail;
    }

    /**
     * 处理一次鼠标点击.
     *
     * @param p 点击点坐标.
     */
    public void handleClick(Point p) {
        Screw s = findTopmostScrewAt(p);
        if (s == null) {
            return;
        }

        int radius = Screw.size() / 2;
        Board holder = state.findHolder(s);
        List<Rectangle> covers = state.coveringRectsFor(holder);
        boolean clickable = Visibility.isClickable(
                s.x, s.y, radius, covers, Rules.CLICKABLE_COVERAGE_RATIO);
        if (!clickable) {
            return;
        }

        // 1) 先尝试把本次点击的螺丝放入目标，否则入缓冲.
        TargetColor color = ColorMapping.toTargetColor(s.color);
        boolean placed = state.targets().tryPlace(color);
        if (!placed) {
            try {
                state.buffer().push(color);
            } catch (BufferOverflowException ex) {
                if (onFail != null) {
                    onFail.run();
                }
                return;
            }
        }

        // 从板面移除该螺丝.
        state.removeScrew(s);

        // 2) 若有目标满 3，刷新该目标颜色.
        refreshTargetsIfNeeded();

        // 3) 自动转运：从缓冲区尽量把可匹配颜色转入目标.
        drainBufferToTargets();

        // 4) 胜负判定：所有螺丝清空且缓冲区为空 → 胜利.
        if (state.allCleared() && state.buffer().isEmpty()) {
            if (onWin != null) {
                onWin.run();
            }
        }
    }

    /**
     * 在所有板中自上而下查找命中的最上层螺丝.
     *
     * @param p 点击点坐标.
     * @return 命中的最上层螺丝，若无则返回 {@code null}.
     */
    private Screw findTopmostScrewAt(Point p) {
        List<Board> boards = state.boards();
        for (int i = boards.size() - 1; i >= 0; i--) {
            Board b = boards.get(i);
            for (int j = b.screws.size() - 1; j >= 0; j--) {
                Screw s = b.screws.get(j);
                if (s.contains(p)) {
                    return s;
                }
            }
        }
        return null;
    }

    /**
     * 自动转运：尝试将缓冲区中能够匹配当前目标颜色的螺丝依次送入目标槽.
     * <p>算法：按队列顺序逐个弹出；若能放入目标则消耗之并继续；否则放回队尾。若一整轮无任何放入，则终止。</p>
     */
    private void drainBufferToTargets() {
        int guard = state.buffer().size() + 2;
        while (guard-- > 0) {
            boolean madeProgress = false;
            int n = state.buffer().size();
            for (int i = 0; i < n; i++) {
                TargetColor c = state.buffer().pop();
                if (c == null) {
                    continue;
                }
                if (state.targets().tryPlace(c)) {
                    madeProgress = true;
                    refreshTargetsIfNeeded();
                } else {
                    try {
                        state.buffer().push(c);
                    } catch (BufferOverflowException ignore) {
                        // 不会发生：我们刚从缓冲区弹出了该元素.
                    }
                }
            }
            if (!madeProgress) {
                break;
            }
        }
    }

    /**
     * 检查并在需要时刷新左右目标颜色（满三则刷新为新色）.
     */
    private void refreshTargetsIfNeeded() {
        boolean leftFull = state.targets().leftFull();
        boolean rightFull = state.targets().rightFull();
        if (leftFull || rightFull) {
            TargetColor newLeft = state.targets().leftColor();
            TargetColor newRight = state.targets().rightColor();
            if (leftFull) {
                newLeft = pickNewColorExcluding(state.targets().rightColor());
            }
            if (rightFull) {
                newRight = pickNewColorExcluding(leftFull ? newLeft : state.targets().leftColor());
            }
            state.targets().refreshColors(leftFull, newLeft, rightFull, newRight);
        }
    }

    /**
     * 随机挑选一个不等于排除色的目标颜色.
     *
     * @param exclude 需要排除的颜色.
     * @return 新的目标颜色.
     */
    private TargetColor pickNewColorExcluding(TargetColor exclude) {
        TargetColor[] vals = TargetColor.values();
        TargetColor candidate = vals[state.random().nextInt(vals.length)];
        if (candidate == exclude) {
            candidate = vals[(candidate.ordinal() + 1) % vals.length];
        }
        return candidate;
    }
}
