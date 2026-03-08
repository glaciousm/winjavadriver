package io.github.winjavadriver;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.io.Serializable;
import java.util.List;

/**
 * Windows-specific locator strategies for WinJavaDriver.
 *
 * <p><b>Important:</b> Use {@code WinBy} methods instead of Selenium's {@code By.name()} and
 * {@code By.className()} — Selenium converts those to CSS selectors internally, but the
 * WinJavaDriver server expects the raw strategy strings {@code "name"} and {@code "class name"}.
 *
 * <p>You CAN use Selenium's {@code By.xpath()} and {@code By.tagName()} directly, as those
 * use the same wire format.
 *
 * <p>Usage:
 * <pre>
 * driver.findElement(WinBy.accessibilityId("btnLogin"));   // accessibility id
 * driver.findElement(WinBy.name("Calculator"));             // name (Win32 Name property)
 * driver.findElement(WinBy.className("Edit"));              // class name
 * driver.findElement(By.xpath("//Button[@Name='OK']"));     // xpath (Selenium's By works)
 * driver.findElement(By.tagName("Button"));                 // tag name (Selenium's By works)
 * </pre>
 */
public class WinBy {

    private WinBy() {}

    /**
     * Find by accessibility ID (AutomationId in UI Automation).
     * This is the most reliable locator for Windows desktop elements.
     */
    public static By accessibilityId(String id) {
        return new ByAccessibilityId(id);
    }

    /**
     * Find by element name (the Name property in UI Automation).
     * <p>Do NOT use Selenium's {@code By.name()} — it converts to a CSS selector
     * which the WinJavaDriver server does not understand.
     */
    public static By name(String name) {
        return new ByName(name);
    }

    /**
     * Find by class name (the ClassName property in UI Automation).
     * <p>Do NOT use Selenium's {@code By.className()} — it converts to a CSS selector
     * which the WinJavaDriver server does not understand.
     */
    public static By className(String className) {
        return new ByClassName(className);
    }

    /**
     * Locator strategy for "accessibility id".
     */
    public static class ByAccessibilityId extends By implements By.Remotable, Serializable {
        private final String id;

        public ByAccessibilityId(String id) {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("Accessibility ID must not be null or empty");
            }
            this.id = id;
        }

        @Override
        public List<WebElement> findElements(SearchContext context) {
            // Must NOT call context.findElements(this) — causes infinite recursion.
            // Selenium's ElementLocation tries REMOTE first (via getRemoteParameters),
            // and only calls this method as a CONTEXT fallback. Delegating back to
            // context.findElements(this) re-enters ElementLocation, which tries CONTEXT
            // again, calling this method in an infinite loop.
            throw new WebDriverException(
                    "WinBy.accessibilityId locator requires a RemoteWebDriver-compatible context. "
                            + "The server may not support the 'accessibility id' locator strategy.");
        }

        @Override
        public Parameters getRemoteParameters() {
            return new Parameters("accessibility id", id);
        }

        @Override
        public String toString() {
            return "WinBy.accessibilityId: " + id;
        }
    }

    /**
     * Locator strategy for "name" (sends raw "name" strategy, not CSS selector).
     */
    public static class ByName extends By implements By.Remotable, Serializable {
        private final String name;

        public ByName(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("Name must not be null or empty");
            }
            this.name = name;
        }

        @Override
        public List<WebElement> findElements(SearchContext context) {
            throw new WebDriverException(
                    "WinBy.name locator requires a RemoteWebDriver-compatible context. "
                            + "The server may not support the 'name' locator strategy.");
        }

        @Override
        public Parameters getRemoteParameters() {
            return new Parameters("name", name);
        }

        @Override
        public String toString() {
            return "WinBy.name: " + name;
        }
    }

    /**
     * Locator strategy for "class name" (sends raw "class name" strategy, not CSS selector).
     */
    public static class ByClassName extends By implements By.Remotable, Serializable {
        private final String className;

        public ByClassName(String className) {
            if (className == null || className.isEmpty()) {
                throw new IllegalArgumentException("Class name must not be null or empty");
            }
            this.className = className;
        }

        @Override
        public List<WebElement> findElements(SearchContext context) {
            throw new WebDriverException(
                    "WinBy.className locator requires a RemoteWebDriver-compatible context. "
                            + "The server may not support the 'class name' locator strategy.");
        }

        @Override
        public Parameters getRemoteParameters() {
            return new Parameters("class name", className);
        }

        @Override
        public String toString() {
            return "WinBy.className: " + className;
        }
    }
}
