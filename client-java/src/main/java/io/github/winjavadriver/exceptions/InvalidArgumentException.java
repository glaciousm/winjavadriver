package io.github.winjavadriver.exceptions;

/**
 * Thrown when an invalid argument is passed to a command.
 */
public class InvalidArgumentException extends WinDriverException {
    public InvalidArgumentException(String message) {
        super(message);
    }

    public InvalidArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
