package io.github.winjavadriver.exceptions;

/**
 * Thrown when an element cannot be found using the specified locator strategy.
 */
public class NoSuchElementException extends WinDriverException {
    public NoSuchElementException(String message) {
        super(message);
    }

    public NoSuchElementException(String message, Throwable cause) {
        super(message, cause);
    }
}
