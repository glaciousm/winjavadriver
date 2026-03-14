# WinJavaDriver

[![CI](https://github.com/glaciousm/winjavadriver/actions/workflows/ci.yml/badge.svg)](https://github.com/glaciousm/winjavadriver/actions/workflows/ci.yml)

A Windows desktop automation tool that implements the W3C WebDriver protocol. Automate Windows applications from Java using a familiar Selenium-style API — like ChromeDriver, but for Windows desktop apps.

## Features

- **W3C WebDriver Protocol** — Standard HTTP-based automation interface
- **Java Client Library** — Extends Selenium's `RemoteWebDriver` for full ecosystem compatibility
- **Windows UI Automation** — Uses Microsoft's UIAutomation framework for modern apps
- **Legacy Win32/VB6 Support** — Automatic fallback for apps invisible to UIA
- **Focus-Independent Interaction** — Click, type, and screenshot without bringing the window to front
- **W3C Actions API** — Double-click, right-click, drag-and-drop, hover, and keyboard shortcuts via Selenium's `Actions` class
- **MSFlexGrid Cell Automation** — Read and write individual cells in VB6 MSFlexGrid controls
- **Inspector GUI** — Chrome DevTools-style element spy with hover-highlight, multi-locator panel, and VB6 label support
- **Record & Replay** — Record user interactions, generate Java Page Object or JUnit test code, replay steps
- **MCP Server** — AI-driven desktop automation with smart, token-efficient tools
- **Multiple Locator Strategies** — name, accessibilityId, className, tagName, xpath
- **Name Normalization** — `WinBy.name("Open")` automatically matches both `"Open"` and `"&Open"` (Windows accelerator key prefix)
- **Window Chaining** — `switchTo().window(handle)` tracks previous window, `switchBack()` returns to it
- **System Window Discovery** — `listAllWindows()` enumerates all visible windows; `switchToWindowByTitle()` switches by title fragment
- **Convenience API** — Wait helpers, element indexing, child enumeration, position-based lookup, retry, and global `sendKeys()`
- **Screenshots** — Window, element, and full desktop screenshot capture (z-order independent)
- **Selenium Grid 4 Integration** — Run tests on remote Windows machines via Grid relay
- **Cucumber/BDD Ready** — Example projects for Calculator automation

## Quick Start

### Prerequisites

- Windows 10/11
- Java 21+ (for the client)

### Installation

1. Add the Java client to your project — the server binary is **auto-downloaded** on first run:

```xml
<dependency>
    <groupId>io.github.glaciousm</groupId>
    <artifactId>winjavadriver-client</artifactId>
    <version>1.1.0</version>
</dependency>
```

### Basic Usage

```java
import io.github.glaciousm.*;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.OutputType;

public class NotepadAutomation {
    public static void main(String[] args) {
        // Create options to launch Notepad
        WinJavaOptions options = new WinJavaOptions()
            .setApp("notepad.exe");

        // Start driver — auto-discovers winjavadriver.exe, auto-starts server
        // (identical to how ChromeDriver works)
        WinJavaDriver driver = new WinJavaDriver(options);

        try {
            // Standard Selenium API — returns WebElement, not a custom type
            WebElement editArea = driver.findElement(WinBy.className("RichEditD2DPT"));
            editArea.sendKeys("Hello from WinJavaDriver!");

            // Selenium-standard screenshot
            driver.getScreenshotAs(OutputType.FILE);

        } finally {
            // Close the session and stop the server
            driver.quit();
        }
    }
}
```

## Locator Strategies

| Strategy | Description | Example |
|----------|-------------|---------|
| `WinBy.name(value)` | Element's Name property | `WinBy.name("Save")` |
| `WinBy.accessibilityId(value)` | AutomationId (most reliable) | `WinBy.accessibilityId("btnSave")` |
| `WinBy.className(value)` | Win32 class name | `WinBy.className("Edit")` |
| `By.tagName(value)` | Control type | `By.tagName("button")` |
| `By.xpath(expression)` | XPath over UI tree | `By.xpath("//Button[@Name='Save']")` |

### Name Normalization

Windows controls often include accelerator key prefixes (e.g., `"&Open"` for Alt+O shortcuts). WinJavaDriver automatically normalizes names so `WinBy.name("Open")` matches both `"Open"` and `"&Open"`. No configuration needed.

### Discovering Element Locators

#### GUI Inspector (Recommended)

Launch the standalone Inspector tool:
```bash
winjavadriver-inspector.exe
```

Features:
- **Dark theme** (Material Design) with Chrome DevTools-style layout
- **Hover-highlight** — hover over elements to see their type, name, and size
- **Click to capture** — click any element (or Ctrl+Q) to select it
- **Multi-locator panel** — see all locator strategies (accessibilityId, name, className, xpath) with copy buttons
- **Java code panel** — ready-to-paste Selenium/WinJavaDriver code snippets
- **Locator console** — test locators against the live UI tree
- **VB6 label support** — discovers windowless VB6 Label controls
- **Breadcrumb navigation** — click path segments to navigate the element tree
- **DPI-aware** — works correctly on high-DPI displays

#### CLI Inspect Mode (Legacy)
```bash
winjavadriver.exe --inspect
```

## Record & Replay

The Inspector includes a built-in recorder that captures user interactions and generates executable test code.

### Recording

1. Open the Inspector and click **Record**
2. The Inspector minimizes and a floating recording toolbar appears (always-on-top, draggable)
3. Interact with your application normally — clicks, typing, and keyboard shortcuts are captured passively
4. Press **Stop** on the toolbar (or ESC) to finish recording

**What gets recorded:**
- **Clicks** — single click, double-click, right-click with element identification
- **Text input** — keystrokes are buffered and merged into SendKeys actions (5-second flush)
- **Keyboard shortcuts** — Ctrl+S, Alt+F4, Ctrl+Shift+N, etc. with modifier tracking
- **Navigation keys** — Arrow keys, Tab, Enter, Page Up/Down, Home, End, Backspace, Delete
- **Screenshots** — each step captures an element screenshot for visual reference

**Additional features:**
- **Pause/Resume** — temporarily pause recording without stopping
- **Editable text** — expand a step to edit the recorded text or expected value
- **Add comments** — annotate steps with user comments
- **Add assertions** — Ctrl+Shift+Click captures element Name as an assertion
- **Step management** — reorder, delete, or modify recorded steps
- **Self-filtering** — clicks on the Inspector or toolbar are not recorded

### Code Generation

After recording, click **Generate Code** to produce:

- **Java Page Object** — class with `By` locator fields, constructor, and `performActions()` method
- **JUnit 5 Test** — standalone test class with `@BeforeEach`/`@AfterEach` lifecycle, `@Test` method

Generated code features:
- `WebDriverWait.until()` for reliable element lookup (10-second default timeout)
- Window switching via title-based matching across `getWindowHandles()`
- `Actions` class for right-click (`contextClick`) and double-click (`doubleClick`)
- `Keys.chord()` for keyboard shortcuts (Ctrl+S, Alt+F4, etc.)
- Navigation keys mapped to Selenium `Keys` constants
- `assertEquals()` for assertion steps
- Position-based element filtering (±20px tolerance) when no stable identifier exists
- User comments preserved as Java code comments

### Replay

Click **Replay** to re-execute recorded steps against the live application:
- Re-finds elements by AutomationId (preferred) or Name+ClassName
- Handles window switching via title matching
- Supports all action types (click, type, shortcuts, assertions)
- Press **ESC** to cancel replay at any point

## API Reference

### WinJavaDriver (extends Selenium's RemoteWebDriver)

```java
import io.github.glaciousm.*;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.*;

// Auto-discover exe, auto-start server (like ChromeDriver)
WinJavaDriver driver = new WinJavaDriver(options);

// Or connect to an already-running server
WinJavaDriver driver = new WinJavaDriver(new URL("http://localhost:9515"), options);

// Standard Selenium API — returns WebElement
WebElement element = driver.findElement(WinBy.name("Save"));
List<WebElement> elements = driver.findElements(By.tagName("button"));

// Selenium's WebDriverWait + ExpectedConditions
WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10))
    .until(ExpectedConditions.presenceOfElementLocated(WinBy.name("Ready")));

// Window management (inherited from RemoteWebDriver)
String handle = driver.getWindowHandle();
Set<String> handles = driver.getWindowHandles();
driver.switchTo().window(handle);
driver.manage().window().maximize();

// Switch to popup, interact, switch back — no need to store the main handle
driver.switchTo().window(popupHandle).findElement(WinBy.name("OK")).click();
driver.switchBack();

// Switch to a window by title (searches all system windows, not just current process)
driver.switchToWindowByTitle("POS_PREPROD");

// List all visible windows on the system (handle, title, className, processId)
List<Map<String, Object>> windows = driver.listAllWindows();

// Wait helpers (default 10s timeout, or provide custom Duration)
WebElement el = driver.waitForElement(WinBy.name("Ready"));
WebElement btn = driver.waitForClickable(WinBy.name("Save"), Duration.ofSeconds(5));
Set<String> handles = driver.waitForWindowCount(2);

// Send keys to focused element (no element reference needed)
driver.sendKeys(Keys.chord(Keys.CONTROL, "o")); // Ctrl+O

// Find element by index (useful for VB6 controls without unique identifiers)
WebElement thirdButton = driver.findElementByIndex(WinBy.className("Button"), 2);

// Find element by visual grid position (row, col — groups by Y, sorts by X)
WebElement cell = driver.findElementByPosition(WinBy.className("ThunderRT6TextBox"), 1, 3);

// Get direct children of an element (works with UIA and Win32/VB6)
List<WebElement> children = driver.findChildren(parentElement);
WebElement firstChild = driver.findChildren(parentElement, 0);

// Retry flaky interactions
driver.retry(() -> driver.findElement(WinBy.name("Save")).click(), 3, Duration.ofMillis(500));

// Screenshots (Selenium standard — captures app window)
File screenshot = driver.getScreenshotAs(OutputType.FILE);

// Desktop screenshot (captures entire screen — useful for dialogs/popups outside the app)
File desktopShot = driver.getDesktopScreenshot(OutputType.FILE);

// Page source (UI tree as XML)
String xml = driver.getPageSource();

// Timeouts
driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

// Cleanup (auto-stops the server)
driver.quit();
```

### WebElement (standard Selenium)

```java
// Interactions
element.click();
element.clear();
element.sendKeys("text to type");

// Properties
String text = element.getText();
String tagName = element.getTagName();
boolean enabled = element.isEnabled();
boolean displayed = element.isDisplayed();
String attr = element.getAttribute("ClassName");
Rectangle rect = element.getRect();

// Find child elements
WebElement child = element.findElement(WinBy.name("Child"));
List<WebElement> children = element.findElements(By.tagName("listitem"));
```

### W3C Actions API

Selenium's `Actions` class is fully supported for complex interactions:

```java
import org.openqa.selenium.interactions.Actions;

Actions actions = new Actions(driver);

// Right-click (context menu)
actions.contextClick(element).perform();

// Double-click
actions.doubleClick(element).perform();

// Hover over element
actions.moveToElement(element).perform();

// Drag and drop
actions.dragAndDrop(source, target).perform();

// Keyboard shortcut (Ctrl+S)
actions.keyDown(Keys.CONTROL).sendKeys("s").keyUp(Keys.CONTROL).perform();

// Ctrl+Click
actions.keyDown(Keys.CONTROL).click(element).keyUp(Keys.CONTROL).perform();

// Key combos with modifier tracking (proper release order)
actions.keyDown(Keys.SHIFT).sendKeys(Keys.F10).keyUp(Keys.SHIFT).perform();
```

### Explicit Waits (Selenium standard)

```java
import org.openqa.selenium.support.ui.*;

WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

// Wait for element to be present in the UI tree
WebElement element = wait.until(
    ExpectedConditions.presenceOfElementLocated(WinBy.name("Ready")));

// Wait for element to be visible (present + isDisplayed)
WebElement visible = wait.until(
    ExpectedConditions.visibilityOfElementLocated(WinBy.name("Ready")));

// Wait for element to be clickable (visible + isEnabled)
WebElement clickable = wait.until(
    ExpectedConditions.elementToBeClickable(WinBy.name("Save")));

// Custom condition with lambda
wait.until(d -> {
    WebElement el = d.findElement(WinBy.name("Status"));
    return el.getText().contains("Done") ? el : null;
});
```

### WinJavaOptions

```java
WinJavaOptions options = new WinJavaOptions()
    .setApp("C:\\Program Files\\MyApp\\app.exe")  // App to launch
    .setAppArguments("--flag value")              // Command line args
    .setAppWorkingDir("C:\\Working")              // Working directory
    .setWaitForAppLaunch(10)                      // Seconds to wait
    .setShouldCloseApp(true);                     // Close on quit

// Or attach to running app by window handle
WinJavaOptions options = new WinJavaOptions()
    .setAppTopLevelWindow("0x1A2B3C");            // Hex window handle
```

## Legacy Win32/VB6 App Support

WinJavaDriver automatically detects legacy apps invisible to UI Automation and falls back to alternative discovery methods. No configuration needed — it just works.

### VB6 Special Handling

- **Thunder\* controls** (ThunderRT6TextBox, ThunderRT6ComboBox, etc.): Standard UIA input methods silently fail on these controls. WinJavaDriver detects Thunder\* class names and uses Win32 messages instead.
- **VB6 Labels**: Discoverable despite having no window handle. Found via `WinBy.className("VB6Label")`.

```java
// VB6 apps work the same as modern apps
WinJavaDriver driver = new WinJavaDriver(
    new WinJavaOptions().setApp("C:\\path\\to\\LegacyApp.exe"));

// VB6 Labels are discoverable
List<WebElement> labels = driver.findElements(WinBy.className("VB6Label"));
labels.forEach(l -> System.out.println(l.getText()));  // runtime captions
```

### Custom Server Endpoints

WinJavaDriver extends the W3C WebDriver protocol with custom endpoints:

| Method | Endpoint | Purpose |
|--------|----------|---------|
| GET | `/session/{id}/winjavadriver/element/{eid}/children` | Get direct children (UIA + Win32/VB6) |
| GET | `/session/{id}/winjavadriver/windows/all` | List all visible windows on the system |
| GET | `/session/{id}/winjavadriver/screenshot/desktop` | Full desktop screenshot |
| POST | `/session/{id}/winjavadriver/grid/{eid}/cell` | Create virtual MSFlexGrid cell element |
| GET | `/session/{id}/winjavadriver/grid/{eid}/info` | Get grid dimensions and edit field info |
| POST | `/session/{id}/winjavadriver/grid/{eid}/cell/value` | Read MSFlexGrid cell value |
| PUT | `/session/{id}/winjavadriver/grid/{eid}/cell/value` | Write MSFlexGrid cell value |

- Row/col are 0-based (excluding header row)
- The MCP server exposes this via `win_grid_edit` for batch cell editing

## MCP Server (AI Desktop Automation)

The `mcp/` directory contains an MCP server that enables AI agents to automate Windows desktop applications with token-efficient smart tools.

### Setup

```json
{
  "mcpServers": {
    "winjavadriver": {
      "command": "node",
      "args": ["<path-to-repo>/mcp/dist/index.js"],
      "env": {
        "WINJAVADRIVER_PORT": "9515"
      }
    }
  }
}
```

Build the MCP server:
```bash
cd mcp
npm install
npm run build
```

### Smart Tools (AI-Optimized)

These tools compose multiple WebDriver calls into single, token-efficient operations:

| Tool | Description |
|------|-------------|
| `win_observe` | Screenshot + element summary in one call — primary "look at the screen" tool |
| `win_explore` | Concise element summary with positions @(x,y) and no-id warnings |
| `win_interact` | Find + act in one call (click, type, clear, clear_and_type, right_click, double_click, read) |
| `win_batch` | Execute multiple find-and-act steps in sequence (fill a form in one call) |
| `win_read_all` | Bulk read text/attributes from multiple elements |
| `win_wait_for` | Server-side polling (element_visible, element_gone, text_equals, etc.) — zero token cost during wait |
| `win_diff` | Show what changed since last explore (new, removed, changed elements) |
| `win_hover` | Hover over element using W3C Actions API |
| `win_form_fields` | Discover form fields (Edit, ComboBox, CheckBox) with current values |
| `win_menu` | Navigate menu path by clicking items in sequence (e.g., File > Save As) |
| `win_select_option` | Select option from ComboBox/ListBox — expands, finds, clicks |
| `win_grid_edit` | Batch-edit multiple MSFlexGrid cells in one call |

**Preferred AI agent workflow:**
1. `win_observe` — see the screen (screenshot + element summary)
2. `win_interact` or `win_batch` — perform actions
3. `win_diff` or `win_observe` — verify results
4. `win_wait_for` — when timing matters (dialogs, loading)

### Standard Tools

| Tool | Description |
|------|-------------|
| `win_launch_app` | Launch app with optional `verbose: true` for debugging |
| `win_attach_app` | Attach to running app by window handle |
| `win_quit` | Close session and application |
| `win_find_element` | Find single element (name, accessibility id, class name, tag name, xpath) |
| `win_find_elements` | Find multiple elements with optional `includeInfo: true` |
| `win_click` | Click element (supports x/y offset) |
| `win_type` | Type text into element |
| `win_clear` | Clear element value |
| `win_send_keys` | Send keyboard keys with repeat syntax (`DOWN*5`) |
| `win_get_text` | Get element text |
| `win_get_attribute` | Get element attribute |
| `win_element_info` | Get element info (text, rect, className, automationId, name, enabled, displayed) |
| `win_screenshot` | Screenshot of window, element, or entire screen (`fullscreen: true`) |
| `win_page_source` | Get UI tree as XML |
| `win_window_handle` | Get current window handle |
| `win_list_windows` | List window handles for current process |
| `win_list_all_windows` | List ALL visible windows (titles, handles, PIDs) |
| `win_switch_window` | Switch to different window |
| `win_set_window` | Maximize, minimize, or fullscreen |
| `win_close_window` | Close current window |
| `win_clipboard` | Read/write system clipboard |
| `win_get_logs` | Get server verbose logs |
| `win_set_verbose` | Enable/disable verbose logging |
| `win_clear_logs` | Clear log buffer |
| `win_status` | Check if server is running |

## Server CLI

```bash
winjavadriver.exe [options]

Options:
  --port <port>         Port to listen on (default: 9515)
  --host <host>         Host to bind to (default: localhost)
  --verbose             Enable verbose logging
  --log-file <path>     Write logs to file
  --inspect             Launch inspect mode (element spy)
  --version             Print version
  --help                Show help
```

## Remote Execution via Selenium Grid

Run desktop UI tests on remote Windows machines using Selenium Grid 4. WinJavaDriver integrates via the built-in relay feature — the same pattern used by Appium.

```java
// Point tests at the Grid — routes to WinJavaDriver node automatically
WinJavaDriver driver = new WinJavaDriver(
    new URL("http://grid-machine:4444"), options);
```

For full setup instructions, see [docs/grid-node.md](docs/grid-node.md).

## UWP Apps (Calculator, Paint, etc.)

UWP apps are fully supported:

```java
// Launch Windows Calculator (UWP app)
WinJavaOptions options = new WinJavaOptions()
    .setApp("calc.exe")
    .setWaitForAppLaunch(10);

WinJavaDriver driver = new WinJavaDriver(options);

// Find and click button "Five"
driver.findElement(WinBy.name("Five")).click();
driver.findElement(WinBy.name("Plus")).click();
driver.findElement(WinBy.name("Three")).click();
driver.findElement(WinBy.name("Equals")).click();

// Get result
WebElement result = driver.findElement(WinBy.accessibilityId("CalculatorResults"));
System.out.println(result.getText()); // "Display is 8"
```

**Note:** For UWP apps, the launcher process (e.g., `calc.exe`) exits immediately and the actual app runs as a different process. WinJavaDriver handles this automatically.

## Building from Source

### Client (Java)

```bash
cd client-java
mvn clean install
```

### MCP Server (Node.js)

```bash
cd mcp
npm install
npm run build
```

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Java Client                          │
│  WinJavaDriver (extends RemoteWebDriver)                │
│  WinBy → WebElement → WebDriverWait                     │
│  WinJavaDriverService (extends DriverService)           │
└─────────────────────┬───────────────────────────────────┘
                      │ W3C WebDriver Protocol
                      │ (HTTP + JSON)
┌─────────────────────▼───────────────────────────────────┐
│              winjavadriver.exe (server)                  │
│  Handles element discovery, interaction, screenshots    │
│  Supports UIA, Win32, MSAA, and VB6 controls            │
└─────────────────────────────────────────────────────────┘
```

```
client-java/                    (Java client extending Selenium RemoteWebDriver)
mcp/                            (MCP server for AI-driven automation)
examples/                       (Cucumber BDD test examples)
configs/                        (Selenium Grid Node TOML config templates)
scripts/                        (Node setup scripts)
jenkins/                        (Docker-based Jenkins CI/CD)
docs/                           (Documentation)
```

## Supported Control Types

Button, Calendar, CheckBox, ComboBox, Custom, DataGrid, DataItem, Document, Edit, Group, Header, HeaderItem, Hyperlink, Image, List, ListItem, Menu, MenuBar, MenuItem, Pane, ProgressBar, RadioButton, ScrollBar, Separator, Slider, Spinner, SplitButton, StatusBar, Tab, TabItem, Table, Text, Thumb, TitleBar, ToolBar, ToolTip, Tree, TreeItem, Window

## Example Projects

The `examples/` directory contains complete Cucumber BDD test projects:

| Project | Description |
|---------|-------------|
| `calculator-tests` | Windows 11 + VB6 Calculator automation (3 scenarios) |

### Running the examples

```bash
cd examples/calculator-tests
mvn test
```

The example uses the SeleniumHQ pattern — no hardcoded paths, no manual server management:

```java
// Each driver auto-discovers winjavadriver.exe and manages its own server
WinJavaDriver driver = new WinJavaDriver(
    new WinJavaOptions().setApp("calc.exe").setWaitForAppLaunch(10));
// ...
driver.quit();  // auto-stops the server
```

## Troubleshooting

### Element not found

1. Use the Inspector GUI to verify the element exists and see its properties
2. Check if the element is in a different window — use `driver.switchTo().window(handle)`
3. Add explicit waits for dynamic elements
4. Try different locator strategies (accessibilityId is most reliable)

### Session creation fails

1. Ensure the app path is correct
2. Check if the app requires elevated permissions
3. Verify the app window appears within the timeout

### Click not working

1. Ensure the element is visible and enabled
2. Try using `sendKeys("\n")` for buttons
3. For complex interactions, use the `Actions` class (right-click, double-click, hover)

### VB6 sendKeys not working

VB6 Thunder\* controls ignore standard UIA input methods. WinJavaDriver detects this automatically and uses Win32 messages instead. Note that this replaces the entire text — call `element.clear()` before chaining multiple `sendKeys()` calls.

### Verbose logging

Enable verbose logging to debug issues:
```java
WinJavaDriverService service = new WinJavaDriverService.Builder()
    .withVerboseLogging(true)
    .build();
```

## Contributing

Contributions welcome! Please open an issue or pull request.

## License

MIT License. See [LICENSE](LICENSE) file.
