package com.unscrewit.logic;

/**
 * Thrown when trying to push a screw into a full buffer.
 */
public final class BufferOverflowException extends Exception {

    public BufferOverflowException(String message) {
        super(message);
    }
}
