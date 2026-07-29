# Troubleshooting Guide

Common issues and solutions discovered during real-world Windows desktop automation with WinJavaDriver.

## VB6 / MSAA Controls

### Text Input on Thunder* Controls

VB6 controls (ThunderRT6TextBox, ThunderRT6ComboBox, etc.) don't respond to standard UIA input methods. WinJavaDriver detects Thunder* class names automatically and uses Win32 messages instead.

**Key behavior**: Win32 text input replaces the entire text content. If you need to append text, read the current value first.

```java
// This works — server handles Thunder* routing internally
element.sendKeys("new value");
```

### Keyboard Navigation: Use Actions API, Not element.sendKeys()

`element.sendKeys(Keys.DOWN)` is silently ignored by some controls (MSFlexGrid, certain VB6 controls). Use the Selenium Actions API instead:

```java
// WRONG — silently ignored on some controls
grid.sendKeys(Keys.DOWN);

// CORRECT — works reliably
new Actions(driver).sendKeys(Keys.DOWN).perform();

// Multiple keys
new Actions(driver)
    .sendKeys(Keys.DOWN, Keys.DOWN, Keys.DOWN, Keys.ENTER)
    .perform();
```

### isDisplayed() Returns False for Visible Elements

Some MSAA-bridged elements (Thunder* controls, MSFlexGrid child elements) report `isDisplayed() = false` even when visible on screen.

**Workaround**: Use `presenceOfElementLocated()` instead of `visibilityOfElementLocated()`:

```java
// WRONG — may time out for VB6 elements
wait.until(ExpectedConditions.visibilityOfElementLocated(locator));

// CORRECT — works for all element types
wait.until(ExpectedConditions.presenceOfElementLocated(locator));
```

### Bounding Rectangles May Be Incorrect

Some MSAA elements return bounding rectangles equal to the entire window size instead of their actual bounds. This affects position-based clicking and element screenshots.

**Workaround**: For position-critical operations, use known coordinates or navigate by keyboard instead.

## MSFlexGrid Automation

### Grid Structure

MSFlexGrid is exposed as a single element — individual cells are **not** accessible as child elements. The server provides custom grid endpoints for cell-level access.

### Use the Grid Endpoints, Not Coordinates

Address cells by row and column index. The server moves the grid's selection by logical row/column steps, so cells outside the visible area are reached correctly and results do not depend on screen resolution or display scaling:

```java
WinJavaDriver driver = ...;
WebElement grid = driver.findElement(WinBy.className("MSFlexGridWndClass"));

// Read a cell (0-based row/col, excluding the header row)
String value = driver.getGridCellValue(grid, 32, 1);

// Write a cell
driver.setGridCellValue(grid, 32, 1, "new value");
```

Off-screen rows need no scrolling on your part — the grid scrolls its own selection into view.

**Do not compute cell positions and click them.** A grid's row height, header height and column widths vary per application, and the driver is per-monitor DPI aware while VB6 hosts are DPI unaware — so a coordinate that works on one machine selects a different cell on another.

### Only the Editable Column Can Be Read or Written

Cell text is read from and written through the grid's *editor* (the floating edit field, control ID `22` in most applications). That editor only ever holds the **editable** column's value, so other columns — for example a field-name label in column 0 — cannot be read or written this way.

Requesting a different column fails with a clear error rather than silently returning the editable column's value. If your grid's editable column is not column 1, declare it:

```java
// This grid's editor edits column 2
String value = driver.getGridCellValue(grid, 5, 2, 22, 2);
driver.setGridCellValue(grid, 5, 2, "new value", 22, 2);
```

### Grid Information

`win_grid_info` (or the `/grid/{id}/info` endpoint) reports `rowCountHint`, derived from the grid's scroll range. It is `null` when the grid has no scrollbar because every row fits. `columnCount` is always `null` — a self-drawing grid exposes no column count to an external process.

### Status Bar Shows Current Cell Value

The status bar (AutomationId `"23"`) displays the currently selected cell's value — useful for verification:

```java
WebElement statusBar = driver.findElement(WinBy.accessibilityId("23"));
String cellValue = statusBar.getText();
```

### Field Numbers Skip

VB6 grid field numbers are non-sequential (e.g., F21, F27, F29 — gaps exist). Row number does not equal field number. Use the grid endpoints with 0-based row/col indices instead.

## File Dialogs

### Finding the Filename Field

The filename field in Windows file dialogs is a ComboBox. Type into its Edit child:

```java
// Find the filename ComboBox
WebElement fileNameBox = driver.findElement(WinBy.accessibilityId("1148"));
// Find the Edit child within it
WebElement editField = fileNameBox.findElement(WinBy.className("Edit"));
editField.clear();
editField.sendKeys("C:\\path\\to\\file.txt");
```

### Button Names Include Ampersand

Windows buttons often include `&` for keyboard accelerators (e.g., `"&Open"`, `"&Save"`):

```java
// Include the ampersand in the name
driver.findElement(WinBy.name("&Open")).click();
```

### Wait for Dialog Before Interacting

File dialogs appear asynchronously. Wait for the new window handle:

```java
String mainWindow = driver.getWindowHandle();

// Trigger dialog open
menuItem.click();

// Wait for new window
wait.until(d -> d.getWindowHandles().size() > 1);

// Switch to dialog
for (String handle : driver.getWindowHandles()) {
    if (!handle.equals(mainWindow)) {
        driver.switchTo().window(handle);
        break;
    }
}

// ... interact with dialog ...

// Switch back to main window
driver.switchTo().window(mainWindow);
```

## Window Management

### Detecting New Windows/Dialogs

```java
int initialCount = driver.getWindowHandles().size();

// Action that opens a new window
button.click();

// Wait for new window
wait.until(d -> d.getWindowHandles().size() > initialCount);
```

### Switching by Window Title

```java
for (String handle : driver.getWindowHandles()) {
    driver.switchTo().window(handle);
    if (driver.getTitle().contains("Expected Title")) {
        break;
    }
}
```

### Always Switch Back After Dialog

After closing a dialog, the driver's window context may be invalid. Always switch back:

```java
dialogCloseButton.click();
driver.switchTo().window(mainWindowHandle);
```

## Elements Without Stable Identifiers

### Position-Based Fallback Locators

Some VB6 controls have no AutomationId or Name. Use position-based lookup as a last resort:

```java
// Position-based lookup (no unique identifier available)
WebElement target = driver.findElements(WinBy.className("ThunderRT6UserControlDC")).stream()
    .filter(e -> Math.abs(e.getRect().getX() - 441) < 20 && Math.abs(e.getRect().getY() - 283) < 20)
    .findFirst().orElseThrow(() -> new RuntimeException("Element not found at position (441, 283)"));
target.click();
```

**Limitations**: Position-based locators are fragile — they break if the window is resized or moved.

### Locator Quality in Inspector

The Inspector shows a locator quality indicator for each element:
- **● Excellent** — has AutomationId (most stable)
- **◐ Fair** — has Name but no AutomationId
- **○ Poor** — no identifier, needs position-based or XPath locator

## MCP Tools

### Keyboard Modifiers with win_send_keys

Modifier keys (CONTROL, SHIFT, ALT) are held down while subsequent keys are pressed:

```
win_send_keys("CONTROL a")   → Ctrl+A (select all)
win_send_keys("CONTROL c")   → Ctrl+C (copy)
win_send_keys("SHIFT END")   → Shift+End (select to end)
```

### Fullscreen Screenshots

When dialogs open, `win_screenshot` only captures the attached window. Use `fullscreen: true` to capture the entire screen.

### VB6 Apps and win_explore

VB6 controls (Thunder* classes) often lack Name and AutomationId. `win_explore` shows their ClassName and position instead:

```
[3] Button "" [ThunderRT6CommandButton] @(441,283) ⚠no-id
```

Use `class name` strategy with `win_find_elements` for these controls.

### New Window Detection

After clicking a button that opens a dialog, call `win_observe` — it automatically detects new windows and warns you to switch.

### Use Smart Tools for Efficiency

Prefer compound smart tools over individual calls:

| Instead of | Use |
|------------|-----|
| `win_page_source` + manual parsing | `win_explore` |
| `win_screenshot` + `win_explore` | `win_observe` |
| `win_find_element` + `win_click` | `win_interact` |
| Multiple find+click sequences | `win_batch` |
| Multiple find+getText calls | `win_read_all` |
