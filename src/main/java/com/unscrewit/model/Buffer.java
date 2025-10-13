package com.unscrewit.model;

import com.unscrewit.logic.BufferOverflowException;
import com.unscrewit.rules.Rules;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Bounded queue-like buffer for screws when they cannot go to targets.
 */
public final class Buffer {

    private final Deque<TargetColor> deque = new ArrayDeque<>(Rules.BUFFER_CAPACITY);

    /**
     * Adds a screw color into the buffer.
     * <p>
     * If the buffer is already full (reaching
     * {@link com.unscrewit.rules.Rules#BUFFER_CAPACITY}),
     * this method throws a {@link com.unscrewit.logic.BufferOverflowException}.
     * </p>
     *
     * @param color the color of the screw to add
     * @throws BufferOverflowException if the buffer has reached its maximum capacity
     */
    public void push(TargetColor color) throws BufferOverflowException {
        if (deque.size() >= Rules.BUFFER_CAPACITY) {
            throw new BufferOverflowException("Buffer is full (capacity="
                    + Rules.BUFFER_CAPACITY + ")");
        }
        deque.addLast(color);
    }

    public TargetColor pop() {
        return deque.pollFirst();
    }

    public int size() {
        return deque.size();
    }

    public boolean isEmpty() {
        return deque.isEmpty();
    }
}
