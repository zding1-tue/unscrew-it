package com.unscrewit.logic;

/**
 * Exception thrown when attempting to push a screw into a full buffer.
 *
 * <p>
 * This exception is used by game logic to signal that the screw buffer
 * (temporary storage area) has reached its maximum capacity and can no longer
 * accept new screws.
 * </p>
 *
 * <p>
 * It is typically caught by the controller or panel logic to prevent
 * unintended overflow behavior and to provide appropriate user feedback
 * (e.g., blocking the action or playing a warning sound).
 * </p>
 */
public final class BufferOverflowException extends Exception {

    /**
     * Constructs a new {@code BufferOverflowException} with a detailed message.
     *
     * @param message description of the error or context where the overflow occurred
     */
    public BufferOverflowException(String message) {
        super(message);
    }
}
