package io.github.winjavadriver.exceptions;

/**
 * Thrown when an operation times out.
 */
public class TimeoutException extends WinDriverException {
    public TimeoutException(String message) {
        super(message);
    }

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
