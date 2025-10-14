package com.unscrewit.model;

import com.unscrewit.logic.BufferOverflowException;
import com.unscrewit.rules.Rules;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 有界的类队列缓冲区：用于暂存无法直接放入目标槽的螺丝颜色.
 */
public final class Buffer {

    /** 内部双端队列，头出尾进，容量为 {@link Rules#BUFFER_CAPACITY}. */
    private final Deque<TargetColor> deque = new ArrayDeque<>(Rules.BUFFER_CAPACITY);

    /**
     * 向缓冲区压入一个颜色，若已满则抛出异常.
     *
     * @param color 需要压入的逻辑颜色.
     * @throws BufferOverflowException 当缓冲区达到最大容量时抛出.
     */
    public void push(TargetColor color) throws BufferOverflowException {
        if (deque.size() >= Rules.BUFFER_CAPACITY) {
            throw new BufferOverflowException("Buffer is full (capacity=" + Rules.BUFFER_CAPACITY + ").");
        }
        deque.addLast(color);
    }

    /**
     * 从缓冲区弹出一个颜色（队首），若为空则返回 {@code null}.
     *
     * @return 弹出的颜色，若为空则为 {@code null}.
     */
    public TargetColor pop() {
        return deque.pollFirst();
    }

    /**
     * 返回当前缓冲区内的元素个数.
     *
     * @return 已占用数量.
     */
    public int size() {
        return deque.size();
    }

    /**
     * 判断缓冲区是否为空.
     *
     * @return 为空时为 {@code true}.
     */
    public boolean isEmpty() {
        return deque.isEmpty();
    }

    /**
     * 返回缓冲区内容的只读快照（从队首到队尾的顺序）.
     * <p>该方法仅用于 UI 渲染，不暴露可修改的内部结构.</p>
     *
     * @return 颜色列表的副本.
     */
    public List<TargetColor> snapshot() {
        return new ArrayList<>(deque);
    }
}
