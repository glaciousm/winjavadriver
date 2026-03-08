package io.github.winjavadriver.exceptions;

/**
 * Thrown when a session cannot be created.
 */
public class SessionNotCreatedException extends WinDriverException {
    public SessionNotCreatedException(String message) {
        super(message);
    }

    public SessionNotCreatedException(String message, Throwable cause) {
        super(message, cause);
    }
}
