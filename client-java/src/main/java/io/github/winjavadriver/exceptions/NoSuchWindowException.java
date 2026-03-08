package io.github.winjavadriver.exceptions;

/**
 * Thrown when a window handle is no longer valid or cannot be found.
 */
public class NoSuchWindowException extends WinDriverException {
    public NoSuchWindowException(String message) {
        super(message);
    }

    public NoSuchWindowException(String message, Throwable cause) {
        super(message, cause);
    }
}
