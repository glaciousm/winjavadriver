package io.github.winjavadriver.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriverException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuthenticationException.
 */
class AuthenticationExceptionTest {

    @Test
    @DisplayName("Should create exception with message")
    void shouldCreateWithMessage() {
        var ex = new AuthenticationException("Invalid credentials");

        assertThat(ex.getMessage()).contains("Invalid credentials");
    }

    @Test
    @DisplayName("Should create exception with message and cause")
    void shouldCreateWithMessageAndCause() {
        var cause = new RuntimeException("Win32 error");
        var ex = new AuthenticationException("Login failed", cause);

        assertThat(ex.getMessage()).contains("Login failed");
        assertThat(ex.getCause()).isEqualTo(cause);
    }

    @Test
    @DisplayName("Should extend WinDriverException and WebDriverException")
    void shouldExtendCorrectHierarchy() {
        var ex = new AuthenticationException("test");

        assertThat(ex).isInstanceOf(WinDriverException.class);
        assertThat(ex).isInstanceOf(WebDriverException.class);
    }
}
