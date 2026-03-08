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

### Navigation Pattern

```java
// Navigate to top of grid
new Actions(driver).sendKeys(Keys.PAGE_UP).perform();

// Move down to specific row
for (int i = 0; i < targetRow; i++) {
    new Actions(driver).sendKeys(Keys.DOWN).perform();
}

// Commit cell edit
new Actions(driver).sendKeys(Keys.ENTER).perform();
```

### Dynamic Edit Field

When a cell is selected, a floating edit field appears (AutomationId `"22"`). This is where you read/write cell values:

```java
// Click grid to select cell, then find the edit field
WebElement editField = driver.findElement(WinBy.accessibilityId("22"));
editField.clear();
editField.sendKeys("new value");
```

### Status Bar Shows Current Cell Value

The status bar (AutomationId `"23"`) displays the currently selected cell's value — useful for verification:

```java
WebElement statusBar = driver.findElement(WinBy.accessibilityId("23"));
String cellValue = statusBar.getText();
```

### Field Numbers Skip

VB6 grid field numbers are non-sequential (e.g., F21, F27, F29 — gaps exist). Row number does not equal field number. Use the grid endpoints with 0-based row/col indices instead.

### Grid Endpoints (Java Client)

```java
// Using custom grid endpoints (0-based row/col)
WinJavaDriver driver = ...;
WebElement grid = driver.findElement(WinBy.className("MSFlexGridWndClass"));

// Read cell value
String value = driver.getGridCellValue(grid, 2, 1); // row 2, col 1

// Write cell value
driver.setGridCellValue(grid, 2, 1, "new value");
```

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
