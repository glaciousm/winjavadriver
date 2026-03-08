# winjavadriver-mcp

MCP server for AI-driven Windows desktop automation via [WinJavaDriver](https://github.com/glaciousm/winjavadriver).

Enables AI agents (Claude, GPT, etc.) to automate Windows desktop applications through the Model Context Protocol using token-efficient smart tools.

## Installation

```bash
npm install -g winjavadriver-mcp
```

Or install from source:

```bash
git clone https://github.com/glaciousm/winjavadriver.git
cd winjavadriver/mcp
npm install
npm run build
```

## Prerequisites

- Windows 10/11
- Node.js 18+
- WinJavaDriver server — [auto-downloaded](https://github.com/glaciousm/winjavadriver/releases) on first use, or download manually

## Configuration

Add to your MCP client config (e.g., Claude Desktop `claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "winjavadriver": {
      "command": "winjavadriver-mcp"
    }
  }
}
```

Or if running from source:

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

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `WINJAVADRIVER_PORT` | `9515` | Port for WinJavaDriver server |

## Smart Tools (AI-Optimized)

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

### Recommended AI Agent Workflow

1. `win_observe` — see the screen (screenshot + element summary)
2. `win_interact` or `win_batch` — perform actions
3. `win_diff` or `win_observe` — verify results
4. `win_wait_for` — when timing matters (dialogs, loading)

## Standard Tools

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

## Selenium Grid Support

For remote desktop automation via Selenium Grid 4:

| Tool | Description |
|------|-------------|
| `win_grid_connect` | Connect to Grid Hub (auto-discovers nodes) |
| `win_grid_status` | Show Hub URL, connected nodes, and sessions |
| `win_grid_launch_app` | Launch app on a remote Grid node |

## License

MIT
