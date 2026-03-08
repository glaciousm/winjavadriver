package io.github.winjavadriver.exceptions;

/**
 * Thrown when an unknown or unimplemented command is called.
 */
public class UnknownCommandException extends WinDriverException {
    public UnknownCommandException(String message) {
        super(message);
    }

    public UnknownCommandException(String message, Throwable cause) {
        super(message, cause);
    }
}
