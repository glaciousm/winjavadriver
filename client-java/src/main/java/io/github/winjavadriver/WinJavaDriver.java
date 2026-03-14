package io.github.winjavadriver;

import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
}
