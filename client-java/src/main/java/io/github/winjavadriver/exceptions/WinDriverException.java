package io.github.winjavadriver.exceptions;

import org.openqa.selenium.WebDriverException;

/**
 * Base exception for WinJavaDriver-specific errors.
 */
public class WinDriverException extends WebDriverException {
    public WinDriverException(String message) {
        super(message);
    }

    public WinDriverException(String message, Throwable cause) {
        super(message, cause);
    }
}
