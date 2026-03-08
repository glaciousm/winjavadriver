package io.github.winjavadriver.exceptions;

/**
 * Thrown when the WinJavaDriver Agent rejects the provided Windows credentials.
 * This happens when the username, password, or domain is invalid for the remote machine.
 */
public class AuthenticationException extends WinDriverException {
    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
