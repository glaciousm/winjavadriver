package io.github.winjavadriver.exceptions;

/**
 * Thrown when an element cannot be interacted with.
 */
public class ElementNotInteractableException extends WinDriverException {
    public ElementNotInteractableException(String message) {
        super(message);
    }

    public ElementNotInteractableException(String message, Throwable cause) {
        super(message, cause);
    }
}
