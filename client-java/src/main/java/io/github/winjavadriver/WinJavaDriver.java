package io.github.winjavadriver;

import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;
import java.time.Duration;
import java.util.*;

/**
 * Main class for automating Windows desktop applications.
 * Extends Selenium's RemoteWebDriver for full ecosystem compatibility.
 *
 * <p>Usage:
 * <pre>
 * // Auto-discover winjavadriver.exe and start server
 * WinJavaDriver driver = new WinJavaDriver(new WinJavaOptions().setApp("calc.exe"));
 *
 * // Or connect to an already-running server
 * WinJavaDriver driver = new WinJavaDriver(new URL("http://localhost:9515"), options);
 *
 * // Standard Selenium API works
 * WebElement btn = driver.findElement(WinBy.name("One"));
 * btn.click();
 * driver.quit();
 * </pre>
 */
public class WinJavaDriver extends RemoteWebDriver {

    private String previousWindowHandle;

    /**
     * Create a new driver with default options.
     * Auto-discovers and starts winjavadriver.exe.
     */
    public WinJavaDriver() {
        this(new WinJavaOptions());
    }

    /**
     * Create a new driver with the given options.
     * Auto-discovers and starts winjavadriver.exe.
     *
     * @param options Session capabilities
     */
    public WinJavaDriver(WinJavaOptions options) {
        this(WinJavaDriverService.createDefaultService(), options);
    }

    /**
     * Create a new driver using an existing service.
     *
     * @param service Running WinJavaDriverService
     * @param options Session capabilities
     */
    public WinJavaDriver(WinJavaDriverService service, WinJavaOptions options) {
        super(new WinJavaDriverCommandExecutor(service), options);
        ((WinJavaDriverCommandExecutor) getCommandExecutor()).registerCustomFindCommands();
    }

    /**
     * Create a new driver connecting to a remote server URL.
     *
     * @param remoteUrl URL of the running WinJavaDriver server
     * @param options   Session capabilities
     */
    public WinJavaDriver(URL remoteUrl, WinJavaOptions options) {
        super(new WinJavaDriverCommandExecutor.ForUrl(remoteUrl), options);
        ((WinJavaDriverCommandExecutor.ForUrl) getCommandExecutor()).registerCustomFindCommands();
    }

    /**
     * Create a new driver connecting to a remote server URL with generic capabilities.
     *
     * @param remoteUrl    URL of the running WinJavaDriver server
     * @param capabilities Session capabilities
     */
    public WinJavaDriver(URL remoteUrl, Capabilities capabilities) {
        super(new WinJavaDriverCommandExecutor.ForUrl(remoteUrl), capabilities);
        ((WinJavaDriverCommandExecutor.ForUrl) getCommandExecutor()).registerCustomFindCommands();
    }

    /**
     * Find element, routing WinBy locators through a custom command that
     * bypasses Selenium's W3C codec strategy conversion (which converts
     * "class name" and "name" to CSS selectors the server doesn't understand).
     */
    @Override
    public WebElement findElement(By locator) {
        if (isWinByLocator(locator)) {
            By.Remotable.Parameters params = ((By.Remotable) locator).getRemoteParameters();
            return (WebElement) execute(WinJavaDriverCommandExecutor.WIN_FIND_ELEMENT,
                    Map.of("using", params.using(), "value", params.value())).getValue();
        }
        return super.findElement(locator);
    }

    /**
     * Find elements, routing WinBy locators through a custom command.
     * @see #findElement(By)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<WebElement> findElements(By locator) {
        if (isWinByLocator(locator)) {
            By.Remotable.Parameters params = ((By.Remotable) locator).getRemoteParameters();
            Object value = execute(WinJavaDriverCommandExecutor.WIN_FIND_ELEMENTS,
                    Map.of("using", params.using(), "value", params.value())).getValue();
            if (value == null) {
                return Collections.emptyList();
            }
            return (List<WebElement>) value;
        }
        return super.findElements(locator);
    }

    private static boolean isWinByLocator(By locator) {
        return locator instanceof WinBy.ByAccessibilityId
                || locator instanceof WinBy.ByName
                || locator instanceof WinBy.ByClassName;
    }

    /**
     * Create an explicit wait with default timeout (10 seconds).
     *
     * @return A new WebDriverWait instance
     */
    public WebDriverWait newWait() {
        return new WebDriverWait(this, Duration.ofSeconds(10));
    }

    /**
     * Create an explicit wait with custom timeout.
     *
     * @param timeout The timeout duration
     * @return A new WebDriverWait instance
     */
    public WebDriverWait newWait(Duration timeout) {
        return new WebDriverWait(this, timeout);
    }

    // =============================================
    // Wait Helpers (WinJavaDriver Convenience)
    // =============================================

    /**
     * Wait for an element to be present in the UI tree (default 10 seconds).
     *
     * @param locator Element locator
     * @return The found element
     */
    public WebElement waitForElement(By locator) {
        return waitForElement(locator, Duration.ofSeconds(10));
    }

    /**
     * Wait for an element to be present in the UI tree.
     *
     * @param locator Element locator
     * @param timeout Maximum time to wait
     * @return The found element
     */
    public WebElement waitForElement(By locator, Duration timeout) {
        return newWait(timeout).until(ExpectedConditions.presenceOfElementLocated(locator));
    }

    /**
     * Wait for an element to be clickable (visible and enabled, default 10 seconds).
     *
     * @param locator Element locator
     * @return The clickable element
     */
    public WebElement waitForClickable(By locator) {
        return waitForClickable(locator, Duration.ofSeconds(10));
    }

    /**
     * Wait for an element to be clickable (visible and enabled).
     *
     * @param locator Element locator
     * @param timeout Maximum time to wait
     * @return The clickable element
     */
    public WebElement waitForClickable(By locator, Duration timeout) {
        return newWait(timeout).until(ExpectedConditions.elementToBeClickable(locator));
    }

    /**
     * Wait until the expected number of windows are open (default 10 seconds).
     *
     * @param count Expected window count
     * @return The set of window handles
     */
    public Set<String> waitForWindowCount(int count) {
        return waitForWindowCount(count, Duration.ofSeconds(10));
    }

    /**
     * Wait until the expected number of windows are open.
     *
     * @param count   Expected window count
     * @param timeout Maximum time to wait
     * @return The set of window handles
     */
    public Set<String> waitForWindowCount(int count, Duration timeout) {
        newWait(timeout).until(ExpectedConditions.numberOfWindowsToBe(count));
        return getWindowHandles();
    }

    // =============================================
    // Window Switching (WinJavaDriver Extension)
    // =============================================

    @Override
    public TargetLocator switchTo() {
        return new WinTargetLocator(super.switchTo());
    }

    /**
     * Switch back to the previous window.
     * Remembers the window handle from before the last {@code switchTo().window()} call.
     *
     * <p>Usage:
     * <pre>
     * driver.switchTo().window(popupHandle).findElement(WinBy.name("OK")).click();
     * driver.switchBack();
     * </pre>
     *
     * @return this driver for chaining
     * @throws org.openqa.selenium.NoSuchWindowException if no previous window to switch to
     */
    public WinJavaDriver switchBack() {
        if (previousWindowHandle == null) {
            throw new org.openqa.selenium.NoSuchWindowException("No previous window to switch back to");
        }
        String handle = previousWindowHandle;
        previousWindowHandle = null;
        // Use super.switchTo() to bypass tracking — switchBack shouldn't record a new previous
        super.switchTo().window(handle);
        return this;
    }

    /**
     * Switch to a window whose title contains the given text.
     * Searches all visible windows on the system, not just the current process.
     * The previous window handle is tracked for {@link #switchBack()}.
     *
     * @param titleFragment Text to search for in window titles (case-sensitive)
     * @return this driver for chaining
     * @throws NoSuchWindowException if no window matches
     */
    public WinJavaDriver switchToWindowByTitle(String titleFragment) {
        List<Map<String, Object>> windows = listAllWindows();
        for (Map<String, Object> win : windows) {
            String title = (String) win.get("title");
            if (title != null && title.contains(titleFragment)) {
                String handle = (String) win.get("handle");
                switchTo().window(handle);
                return this;
            }
        }
        throw new NoSuchWindowException("No window with title containing: " + titleFragment);
    }

    /**
     * List all visible top-level windows on the system.
     * Each map contains: handle (hex), title, className, processId.
     *
     * <p>Usage:
     * <pre>
     * List&lt;Map&lt;String, Object&gt;&gt; windows = driver.listAllWindows();
     * for (Map&lt;String, Object&gt; w : windows) {
     *     System.out.println(w.get("title") + " → " + w.get("handle"));
     * }
     * </pre>
     *
     * @return List of window info maps
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> listAllWindows() {
        Object value = execute(WinJavaDriverCommandExecutor.WIN_LIST_ALL_WINDOWS).getValue();
        return (List<Map<String, Object>>) value;
    }

    private class WinTargetLocator implements TargetLocator {
        private final TargetLocator delegate;

        WinTargetLocator(TargetLocator delegate) {
            this.delegate = delegate;
        }

        @Override
        public org.openqa.selenium.WebDriver window(String nameOrHandle) {
            try {
                previousWindowHandle = getWindowHandle();
            } catch (Exception ignored) {
                // Window may already be closed
            }
            return delegate.window(nameOrHandle);
        }

        @Override
        public org.openqa.selenium.WebDriver frame(int index) { return delegate.frame(index); }
        @Override
        public org.openqa.selenium.WebDriver frame(String nameOrId) { return delegate.frame(nameOrId); }
        @Override
        public org.openqa.selenium.WebDriver frame(WebElement frameElement) { return delegate.frame(frameElement); }
        @Override
        public org.openqa.selenium.WebDriver parentFrame() { return delegate.parentFrame(); }
        @Override
        public org.openqa.selenium.WebDriver defaultContent() { return delegate.defaultContent(); }
        @Override
        public WebElement activeElement() { return delegate.activeElement(); }
        @Override
        public org.openqa.selenium.Alert alert() { return delegate.alert(); }
        @Override
        public org.openqa.selenium.WebDriver newWindow(org.openqa.selenium.WindowType typeHint) { return delegate.newWindow(typeHint); }
    }

    // =============================================
    // Desktop Screenshot (WinJavaDriver Extension)
    // =============================================

    /**
     * Take a screenshot of the entire desktop, not just the application window.
     * Useful when dialogs, popups, or other windows are visible outside the app.
     *
     * @param outputType The desired output type (BASE64, BYTES, or FILE)
     * @param <T> The type determined by the OutputType
     * @return The screenshot in the requested format
     */
    public <T> T getDesktopScreenshot(org.openqa.selenium.OutputType<T> outputType) {
        String base64 = (String) execute(WinJavaDriverCommandExecutor.WIN_DESKTOP_SCREENSHOT).getValue();
        return outputType.convertFromBase64Png(base64);
    }

    // =============================================
    // Input Helpers (WinJavaDriver Convenience)
    // =============================================

    /**
     * Send keys to the active/focused element without needing an element reference.
     * Useful for keyboard shortcuts and navigation keys.
     *
     * <p>Usage:
     * <pre>
     * driver.sendKeys(Keys.ENTER);
     * driver.sendKeys(Keys.chord(Keys.CONTROL, "o")); // Ctrl+O
     * </pre>
     *
     * @param keys Keys to send
     */
    public void sendKeys(CharSequence... keys) {
        new Actions(this).sendKeys(keys).perform();
    }

    // =============================================
    // MSFlexGrid Support (WinJavaDriver Extensions)
    // =============================================

    /**
     * Create a virtual element for a grid cell.
     * This is a WinJavaDriver extension for VB6 MSFlexGrid controls.
     *
     * @param gridElement The MSFlexGrid element
     * @param row Row index (0-based, excluding header)
     * @param col Column index (0-based)
     * @return A WebElement representing the grid cell
     */
    public WebElement getGridCell(WebElement gridElement, int row, int col) {
        return getGridCell(gridElement, row, col, "", 22);
    }

    /**
     * Create a virtual element for a grid cell with custom parameters.
     *
     * @param gridElement The MSFlexGrid element
     * @param row Row index (0-based, excluding header)
     * @param col Column index (0-based)
     * @param fieldName Optional display name for the cell
     * @param editFieldId Control ID of the dynamic edit field (default: 22)
     * @return A WebElement representing the grid cell
     */
    public WebElement getGridCell(WebElement gridElement, int row, int col, String fieldName, int editFieldId) {
        String elementId = ((org.openqa.selenium.remote.RemoteWebElement) gridElement).getId();
        Map<String, Object> params = Map.of(
                "elementId", elementId,
                "row", row,
                "col", col,
                "fieldName", fieldName,
                "editFieldId", editFieldId
        );
        return (WebElement) execute(WinJavaDriverCommandExecutor.WIN_GRID_CELL, params).getValue();
    }

    /**
     * Get the value of a grid cell.
     * This clicks the cell and reads from the dynamic edit field.
     *
     * @param gridElement The MSFlexGrid element
     * @param row Row index (0-based, excluding header)
     * @param col Column index (0-based)
     * @return The cell value
     */
    public String getGridCellValue(WebElement gridElement, int row, int col) {
        return getGridCellValue(gridElement, row, col, 22);
    }

    /**
     * Get the value of a grid cell with custom edit field ID.
     *
     * @param gridElement The MSFlexGrid element
     * @param row Row index (0-based, excluding header)
     * @param col Column index (0-based)
     * @param editFieldId Control ID of the dynamic edit field
     * @return The cell value
     */
    public String getGridCellValue(WebElement gridElement, int row, int col, int editFieldId) {
        String elementId = ((org.openqa.selenium.remote.RemoteWebElement) gridElement).getId();
        Map<String, Object> params = Map.of(
                "elementId", elementId,
                "row", row,
                "col", col,
                "editFieldId", editFieldId
        );
        return (String) execute(WinJavaDriverCommandExecutor.WIN_GRID_GET_VALUE, params).getValue();
    }

    /**
     * Set the value of a grid cell.
     * This clicks the cell and types into the dynamic edit field.
     *
     * @param gridElement The MSFlexGrid element
     * @param row Row index (0-based, excluding header)
     * @param col Column index (0-based)
     * @param value The value to set
     */
    public void setGridCellValue(WebElement gridElement, int row, int col, String value) {
        setGridCellValue(gridElement, row, col, value, 22);
    }

    /**
     * Set the value of a grid cell with custom edit field ID.
     *
     * @param gridElement The MSFlexGrid element
     * @param row Row index (0-based, excluding header)
     * @param col Column index (0-based)
     * @param value The value to set
     * @param editFieldId Control ID of the dynamic edit field
     */
    public void setGridCellValue(WebElement gridElement, int row, int col, String value, int editFieldId) {
        String elementId = ((org.openqa.selenium.remote.RemoteWebElement) gridElement).getId();
        Map<String, Object> params = Map.of(
                "elementId", elementId,
                "row", row,
                "col", col,
                "value", value,
                "editFieldId", editFieldId
        );
        execute(WinJavaDriverCommandExecutor.WIN_GRID_SET_VALUE, params);
    }

    // =============================================
    // Element Lookup Helpers (WinJavaDriver Convenience)
    // =============================================

    /**
     * Find the Nth element matching a locator.
     * Useful for VB6 controls that have no unique name or automation ID.
     *
     * @param locator Element locator
     * @param index   Zero-based index
     * @return The element at the given index
     * @throws IndexOutOfBoundsException if not enough elements found
     */
    public WebElement findElementByIndex(By locator, int index) {
        List<WebElement> elements = findElements(locator);
        if (index < 0 || index >= elements.size()) {
            throw new IndexOutOfBoundsException(
                    "Requested index " + index + " but only " + elements.size()
                    + " elements found for " + locator);
        }
        return elements.get(index);
    }

    /**
     * Find an element by its visual row/column position among matching elements.
     * Groups elements by Y coordinate (±20px tolerance) into rows, sorts each row by X.
     * Useful for grid-like VB6 layouts where elements have no unique identifiers.
     *
     * <p>Note: Each element's position requires a server round-trip. For large element
     * sets, prefer {@link #findElementByIndex(By, int)} if the list order is predictable.
     *
     * @param locator Element locator
     * @param row     Zero-based row index (top to bottom)
     * @param col     Zero-based column index (left to right)
     * @return The element at the given position
     * @throws IndexOutOfBoundsException if the position is out of range
     */
    public WebElement findElementByPosition(By locator, int row, int col) {
        return findElementByPosition(locator, row, col, 20);
    }

    /**
     * Find an element by its visual row/column position with custom Y-tolerance.
     *
     * @param locator   Element locator
     * @param row       Zero-based row index (top to bottom)
     * @param col       Zero-based column index (left to right)
     * @param tolerance Pixel tolerance for grouping elements into the same row
     * @return The element at the given position
     * @throws IndexOutOfBoundsException if the position is out of range
     */
    public WebElement findElementByPosition(By locator, int row, int col, int tolerance) {
        List<WebElement> elements = findElements(locator);
        if (elements.isEmpty()) {
            throw new IndexOutOfBoundsException("No elements found for " + locator);
        }

        // Group by Y coordinate into rows
        TreeMap<Integer, List<WebElement>> rows = new TreeMap<>();
        for (WebElement el : elements) {
            Rectangle rect = el.getRect();
            int y = rect.getY();
            Integer matchingKey = null;
            for (Integer key : rows.keySet()) {
                if (Math.abs(key - y) <= tolerance) {
                    matchingKey = key;
                    break;
                }
            }
            if (matchingKey != null) {
                rows.get(matchingKey).add(el);
            } else {
                List<WebElement> rowList = new ArrayList<>();
                rowList.add(el);
                rows.put(y, rowList);
            }
        }

        // Sort each row by X, collect into list
        List<List<WebElement>> sortedRows = new ArrayList<>();
        for (List<WebElement> rowElements : rows.values()) {
            rowElements.sort(Comparator.comparingInt(e -> e.getRect().getX()));
            sortedRows.add(rowElements);
        }

        if (row < 0 || row >= sortedRows.size()) {
            throw new IndexOutOfBoundsException(
                    "Requested row " + row + " but only " + sortedRows.size() + " rows found");
        }
        List<WebElement> targetRow = sortedRows.get(row);
        if (col < 0 || col >= targetRow.size()) {
            throw new IndexOutOfBoundsException(
                    "Requested col " + col + " in row " + row + " but only "
                    + targetRow.size() + " columns found");
        }
        return targetRow.get(col);
    }

    /**
     * Get all direct children of an element.
     * Works with both UIA and Win32/MSAA elements (VB6 controls).
     *
     * @param parent Parent element
     * @return List of child elements
     */
    @SuppressWarnings("unchecked")
    public List<WebElement> findChildren(WebElement parent) {
        String elementId = ((org.openqa.selenium.remote.RemoteWebElement) parent).getId();
        Object value = execute(WinJavaDriverCommandExecutor.WIN_ELEMENT_CHILDREN,
                Map.of("elementId", elementId)).getValue();
        // The W3C codec auto-deserializes element references into RemoteWebElement objects
        return (List<WebElement>) value;
    }

    /**
     * Get the Nth direct child of an element.
     * Works with both UIA and Win32/MSAA elements (VB6 controls).
     *
     * @param parent Parent element
     * @param index  Zero-based child index
     * @return The child element at the given index
     * @throws IndexOutOfBoundsException if the parent has fewer children
     */
    public WebElement findChildren(WebElement parent, int index) {
        List<WebElement> children = findChildren(parent);
        if (index < 0 || index >= children.size()) {
            throw new IndexOutOfBoundsException(
                    "Requested child index " + index + " but parent has "
                    + children.size() + " children");
        }
        return children.get(index);
    }

    // =============================================
    // Retry (WinJavaDriver Convenience)
    // =============================================

    /**
     * Retry an action up to {@code maxAttempts} times with a delay between attempts.
     * Useful for flaky VB6 controls that may not respond to the first interaction.
     *
     * <p>Usage:
     * <pre>
     * driver.retry(() -&gt; driver.findElement(WinBy.name("Save")).click(), 3, Duration.ofMillis(500));
     * </pre>
     *
     * @param action      The action to retry
     * @param maxAttempts Maximum number of attempts
     * @param delay       Delay between retries
     * @throws RuntimeException wrapping the last exception if all attempts fail
     */
    public void retry(Runnable action, int maxAttempts, Duration delay) {
        Exception lastException = null;
        for (int i = 0; i < maxAttempts; i++) {
            try {
                action.run();
                return;
            } catch (Exception e) {
                lastException = e;
                if (i < maxAttempts - 1) {
                    try {
                        Thread.sleep(delay.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Retry interrupted", ie);
                    }
                }
            }
        }
        throw new RuntimeException("Action failed after " + maxAttempts + " attempts", lastException);
    }
}
