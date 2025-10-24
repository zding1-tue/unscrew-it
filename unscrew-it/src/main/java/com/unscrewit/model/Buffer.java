package com.unscrewit.model;

import com.unscrewit.logic.BufferOverflowException;
import com.unscrewit.rules.Rules;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Bounded queue-like buffer used to temporarily hold colors that cannot be
 * placed directly into their target slots.
 *
 * <p>The internal structure is a double-ended queue (FIFO for pop, push to
 * the tail). Capacity is {@link Rules#BUFFER_CAPACITY}.</p>
 */
public final class Buffer {

    /** Internal deque (head = front, tail = back), capacity is {@link Rules#BUFFER_CAPACITY}. */
    private final Deque<TargetColor> deque = new ArrayDeque<>(Rules.BUFFER_CAPACITY);

    /**
     * Pushes a color into the buffer; throws if the buffer is already full.
     *
     * @param color the color to push
     * @throws BufferOverflowException if the buffer has reached its maximum capacity
     */
    public void push(TargetColor color) throws BufferOverflowException {
        if (deque.size() >= Rules.BUFFER_CAPACITY) {
            // Keep the message short to satisfy line-length checks.
            throw new BufferOverflowException(
                "Buffer is full (capacity=" + Rules.BUFFER_CAPACITY + ")."
                );
        }
        deque.addLast(color);
    }

    /**
     * Pops a color from the buffer front (FIFO). Returns {@code null} if empty.
     *
     * @return the popped color, or {@code null} if the buffer is empty
     */
    public TargetColor pop() {
        return deque.pollFirst();
    }

    /**
     * Returns the current number of elements in the buffer.
     *
     * @return the element count
     */
    public int size() {
        return deque.size();
    }

    /**
     * Indicates whether the buffer is empty.
     *
     * @return {@code true} if empty; {@code false} otherwise
     */
    public boolean isEmpty() {
        return deque.isEmpty();
    }

    /**
     * Returns a read-only friendly snapshot (iteration order is head to tail).
     *
     * <p>This is intended for UI rendering only; it returns a copy so the
     * buffer's internal structure is not exposed.</p>
     *
     * @return a copy of the colors currently in the buffer
     */
    public List<TargetColor> snapshot() {
        return new ArrayList<>(deque);
    }
}
