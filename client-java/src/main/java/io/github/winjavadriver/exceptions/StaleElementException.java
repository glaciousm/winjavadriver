package io.github.winjavadriver.exceptions;

/**
 * Thrown when an element reference is no longer valid.
 */
public class StaleElementException extends WinDriverException {
    public StaleElementException(String message) {
        super(message);
    }

    public StaleElementException(String message, Throwable cause) {
        super(message, cause);
    }
}
