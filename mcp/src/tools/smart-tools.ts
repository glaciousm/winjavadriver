import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import { execSync } from "node:child_process";
import type { DriverManager } from "../driver-manager.js";
import type { WebDriverClient } from "../webdriver-client.js";
import type { LocatorStrategy } from "../types.js";

// --- XML parsing helpers ---

interface UiElement {
  tag: string;
  name: string;
  automationId: string;
  className: string;
  isEnabled: boolean;
  controlType: string;
  // Extracted from Value.Value or toggle state
  value: string;
  // Bounding rect from page source
  x: number;
  y: number;
  width: number;
  height: number;
  children: UiElement[];
}

function parseXml(xml: string): UiElement[] {
  const elements: UiElement[] = [];
  // Simple recursive regex-based XML parser for UIA page source
  // Format: <ControlType Name="..." AutomationId="..." ClassName="..." IsEnabled="true">
  const tagRegex = /<(\w+)\s+([^>]*?)\s*(\/?)>/g;
  const stack: UiElement[] = [];
  const closingRegex = /<\/(\w+)>/g;

  // Build a simple DOM from the XML
  let pos = 0;
  const tokens: Array<{ type: "open" | "close" | "selfclose"; tag: string; attrs: Record<string, string>; index: number }> = [];

  // Tokenize
  const tokenRegex = /<(\/?)([\w.]+)(\s[^>]*)?\s*(\/?)>/g;
  let match: RegExpExecArray | null;
  while ((match = tokenRegex.exec(xml)) !== null) {
    const isClosing = match[1] === "/";
    const tag = match[2];
    const attrStr = match[3] || "";
    const selfClose = match[4] === "/";

    const attrs: Record<string, string> = {};
    const attrRegex = /(\w[\w.]*)\s*=\s*"([^"]*)"/g;
    let attrMatch: RegExpExecArray | null;
    while ((attrMatch = attrRegex.exec(attrStr)) !== null) {
      attrs[attrMatch[1]] = attrMatch[2];
    }

    if (isClosing) {
      tokens.push({ type: "close", tag, attrs, index: match.index });
    } else if (selfClose) {
      tokens.push({ type: "selfclose", tag, attrs, index: match.index });
    } else {
      tokens.push({ type: "open", tag, attrs, index: match.index });
    }
  }

  // Build tree
  const root: UiElement[] = [];
  const nodeStack: UiElement[] = [];

  for (const token of tokens) {
    if (token.type === "open" || token.type === "selfclose") {
      const el: UiElement = {
        tag: token.tag,
        name: token.attrs["Name"] || "",
        automationId: token.attrs["AutomationId"] || "",
        className: token.attrs["ClassName"] || "",
        isEnabled: token.attrs["IsEnabled"] !== "False",
        controlType: token.tag,
        value: token.attrs["Value.Value"] || "",
        x: parseInt(token.attrs["x"] || "0", 10),
        y: parseInt(token.attrs["y"] || "0", 10),
        width: parseInt(token.attrs["width"] || "0", 10),
        height: parseInt(token.attrs["height"] || "0", 10),
        children: [],
      };

      if (nodeStack.length > 0) {
        nodeStack[nodeStack.length - 1].children.push(el);
      } else {
        root.push(el);
      }

      if (token.type === "open") {
        nodeStack.push(el);
      }
    } else if (token.type === "close") {
      if (nodeStack.length > 0 && nodeStack[nodeStack.length - 1].tag === token.tag) {
        nodeStack.pop();
      }
    }
  }

  return root;
}

// Control types that are interactive (user can act on them)
const INTERACTIVE_TYPES = new Set([
  "Button", "Edit", "TextBox", "ComboBox", "CheckBox", "RadioButton",
  "ListItem", "MenuItem", "Tab", "TabItem", "TreeItem", "DataItem",
  "Hyperlink", "Link", "Slider", "Spinner", "ScrollBar",
  "SplitButton", "ToggleButton", "MenuBar", "Menu",
]);

// Control types that contain readable content
const CONTENT_TYPES = new Set([
  "Text", "StatusBar", "ToolTip", "Header", "HeaderItem",
  "DataGrid", "List", "Tree", "Table", "TitleBar",
]);

interface ScreenElement {
  index: number;
  type: string;
  name: string;
  automationId: string;
  className: string;
  state: string; // "enabled", "disabled", "checked", "unchecked", etc.
  value: string;
  x: number;
  y: number;
}

function flattenElements(
  elements: UiElement[],
  maxDepth: number,
  includeDisabled: boolean,
  depth = 0
): UiElement[] {
  const result: UiElement[] = [];
  for (const el of elements) {
    if (!includeDisabled && !el.isEnabled) continue;

    // Include interactive and content elements
    const isInteractive = INTERACTIVE_TYPES.has(el.controlType);
    const isContent = CONTENT_TYPES.has(el.controlType);
    const hasIdentity = el.name || el.automationId;
    // VB6 Thunder* controls often lack name/id but have className — include them
    const hasClassName = el.className && el.className.length > 0;

    if ((isInteractive || isContent) && (hasIdentity || hasClassName)) {
      result.push(el);
    }

    if (depth < maxDepth) {
      result.push(...flattenElements(el.children, maxDepth, includeDisabled, depth + 1));
    }
  }
  return result;
}

// Detect common dialog patterns from the UI tree
function detectDialogHints(tree: UiElement[]): string[] {
  const hints: string[] = [];

  // Check for dialog-like patterns in top-level elements
  for (const el of tree) {
    const nameUpper = (el.name || "").toUpperCase();
    const typeUpper = el.controlType;

    // Common dialog patterns
    if (typeUpper === "Window" && el.children.length > 0) {
      const childTypes = el.children.map(c => c.controlType);
      const childNames = el.children.map(c => c.name.toUpperCase());

      // File dialog detection
      if (childNames.some(n => n.includes("FILE NAME") || n.includes("FILENAME")) ||
          childNames.some(n => n.includes("SAVE AS") || n.includes("OPEN"))) {
        hints.push("📁 File dialog detected — use win_type to enter filename, then click Save/Open button");
      }

      // Confirmation dialog (OK/Cancel/Yes/No buttons)
      const buttonNames = el.children
        .filter(c => c.controlType === "Button")
        .map(c => c.name.toUpperCase());
      if (buttonNames.some(n => ["OK", "YES", "NO", "CANCEL", "ABORT", "RETRY"].includes(n))) {
        if (buttonNames.includes("YES") && buttonNames.includes("NO")) {
          hints.push("❓ Confirmation dialog — click Yes/No to respond");
        } else if (buttonNames.includes("OK")) {
          hints.push("ℹ️ Message dialog — click OK to dismiss");
        }
      }

      // Error/warning dialog
      if (nameUpper.includes("ERROR") || nameUpper.includes("WARNING")) {
        hints.push("⚠️ Error/warning dialog — read the message, then dismiss");
      }
    }
  }

  return hints;
}

function buildScreenSummary(
  xml: string,
  maxDepth: number,
  includeDisabled: boolean
): { interactive: ScreenElement[]; content: ScreenElement[]; summary: string } {
  const tree = parseXml(xml);
  const flat = flattenElements(tree, maxDepth, includeDisabled);

  // Deduplicate by name+type (page source often has redundant entries)
  const seen = new Set<string>();
  const deduped: UiElement[] = [];
  for (const el of flat) {
    const key = `${el.controlType}|${el.name}|${el.automationId}|${el.className}|${el.x},${el.y}`;
    if (!seen.has(key)) {
      seen.add(key);
      deduped.push(el);
    }
  }

  const interactive: ScreenElement[] = [];
  const content: ScreenElement[] = [];
  let idx = 1;

  for (const el of deduped) {
    const isInteractive = INTERACTIVE_TYPES.has(el.controlType);
    const entry: ScreenElement = {
      index: idx++,
      type: el.controlType,
      name: el.name,
      automationId: el.automationId,
      className: el.className,
      state: el.isEnabled ? "enabled" : "disabled",
      value: el.value,
      x: el.x,
      y: el.y,
    };

    if (isInteractive) {
      interactive.push(entry);
    } else {
      content.push(entry);
    }
  }

  // Build concise text summary
  const lines: string[] = [];
  if (interactive.length > 0) {
    lines.push("Interactive elements:");
    for (const el of interactive) {
      const id = el.automationId ? ` (${el.automationId})` : "";
      const val = el.value ? ` = "${el.value}"` : "";
      const st = el.state !== "enabled" ? ` [${el.state}]` : "";
      const noId = !el.automationId && !el.name;
      const cls = noId && el.className ? ` [${el.className}]` : "";
      const pos = noId ? ` @(${el.x},${el.y})` : "";
      const warn = noId ? " ⚠no-id" : "";
      lines.push(`  [${el.index}] ${el.type} "${el.name}"${id}${cls}${val}${st}${pos}${warn}`);
    }
  }
  if (content.length > 0) {
    lines.push("Content:");
    for (const el of content) {
      const id = el.automationId ? ` (${el.automationId})` : "";
      lines.push(`  [${el.index}] ${el.type} "${el.name}"${id}`);
    }
  }
  if (interactive.length === 0 && content.length === 0) {
    lines.push("No visible elements found.");
  }

  // Auto-detect dialog patterns
  const dialogHints = detectDialogHints(tree);
  if (dialogHints.length > 0) {
    lines.push("");
    lines.push("Hints:");
    for (const hint of dialogHints) {
      lines.push(`  ${hint}`);
    }
  }

  return { interactive, content, summary: lines.join("\n") };
}

// --- Error enrichment helper ---
function enrichError(msg: string, context?: { using?: string; value?: string }): string {
  const suggestions: string[] = [];

  if (msg.includes("no such element")) {
    suggestions.push("Element not found. Try:");
    suggestions.push("  - Use win_explore to see available elements");
    suggestions.push("  - Check if a dialog or new window appeared (win_list_windows)");
    if (context?.using === "name") {
      suggestions.push("  - Name may include '&' prefix (accelerator key) — try without it");
    }
    if (context?.using === "accessibility id") {
      suggestions.push("  - VB6/Win32 controls may not have AutomationId — try 'name' or 'class name'");
    }
  } else if (msg.includes("element not interactable") || msg.includes("not interactable")) {
    suggestions.push("Element exists but cannot be interacted with. Try:");
    suggestions.push("  - Wait for it to become enabled: win_wait_for element_enabled");
    suggestions.push("  - Check if it's behind a modal dialog");
    suggestions.push("  - For VB6 controls, try win_send_keys instead of win_type");
  } else if (msg.includes("no such window") || msg.includes("target window already closed")) {
    suggestions.push("Window is gone. Try:");
    suggestions.push("  - Use win_list_windows to see available windows");
    suggestions.push("  - Switch to another window with win_switch_window");
  } else if (msg.includes("session not created") || msg.includes("No active session")) {
    suggestions.push("No active session. Use win_launch_app or win_attach_app first.");
  }

  return suggestions.length > 0 ? `${msg}\n\n${suggestions.join("\n")}` : msg;
}

// --- Locator enum shared across tools ---
const locatorEnum = z.enum(["name", "accessibility id", "class name", "tag name", "xpath"]);

// --- Action types for interact/batch ---
const actionEnum = z.enum(["click", "right_click", "double_click", "type", "clear", "clear_and_type", "read"]);

export function registerSmartTools(
  server: McpServer,
  driverManager: DriverManager,
  client: WebDriverClient
): void {

  // Track known window handles for new-window detection
  let knownWindowHandles: Set<string> = new Set();

  // ==========================================
  // win_explore — Smart page summary
  // ==========================================
  server.tool(
    "win_explore",
    "Get a concise, actionable summary of the current screen. Lists all visible interactive elements (buttons, fields, checkboxes) and content (labels, status bars) with numbered references. Much more token-efficient than win_page_source.",
    {
      maxDepth: z.number().optional().default(5).describe("Max UI tree depth to explore (default: 5)"),
      scope: z.string().optional().describe("Element ID to scope exploration to (explores children only)"),
      includeDisabled: z.boolean().optional().default(false).describe("Include disabled elements (default: false)"),
    },
    async ({ maxDepth, scope, includeDisabled }) => {
      try {
        await driverManager.ensureRunning();
        const xml = await client.getPageSource();
        const { summary } = buildScreenSummary(xml, maxDepth, includeDisabled);

        // Prepend window title
        const handle = await client.getWindowHandle();
        const title = await client.getAttribute(
          await client.findElement("xpath" as LocatorStrategy, "/*"),
          "Name"
        ).catch(() => null);

        const header = `Window: "${title ?? "Unknown"}" (${handle})`;
        return { content: [{ type: "text" as const, text: `${header}\n\n${summary}` }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text" as const, text: `Explore failed: ${msg}` }], isError: true };
      }
    }
  );

  // ==========================================
  // win_observe — Screenshot + explore in one call
  // ==========================================
  server.tool(
    "win_observe",
    "See the current screen: returns a screenshot AND a concise element summary in one call. This is the primary tool for understanding what's on screen — like a tester looking at the app.",
    {
      maxDepth: z.number().optional().default(5).describe("Max UI tree depth to explore (default: 5)"),
    },
    async ({ maxDepth }) => {
      try {
        await driverManager.ensureRunning();

        // Parallel fetch: screenshot + page source + window handles
        const [base64, xml, handles] = await Promise.all([
          client.takeScreenshot(),
          client.getPageSource(),
          client.getWindowHandles().catch(() => [] as string[]),
        ]);

        const { summary } = buildScreenSummary(xml, maxDepth, false);

        let title = "Unknown";
        try {
          const rootEl = await client.findElement("xpath" as LocatorStrategy, "/*");
          title = await client.getAttribute(rootEl, "Name") ?? "Unknown";
        } catch { /* ignore */ }
        const handle = await client.getWindowHandle().catch(() => "?");

        const header = `Window: "${title}" (${handle})`;

        // Detect new windows since last observation
        let newWindowWarning = "";
        if (knownWindowHandles.size > 0) {
          const newHandles = handles.filter(h => !knownWindowHandles.has(h));
          if (newHandles.length > 0) {
            newWindowWarning = `\n\n⚠ ${newHandles.length} new window(s) detected: ${newHandles.join(", ")}. Use win_switch_window to switch, then win_observe to see content.`;
          }
        }
        // Update tracked handles
        knownWindowHandles = new Set(handles);

        return {
          content: [
            { type: "image" as const, data: base64, mimeType: "image/png" as const },
            { type: "text" as const, text: `${header}\n\n${summary}${newWindowWarning}` },
          ],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text" as const, text: `Observe failed: ${msg}` }], isError: true };
      }
    }
  );

  // ==========================================
  // win_interact — Find + Act in one call
  // ==========================================
  server.tool(
    "win_interact",
    "Find an element and perform an action on it in a single call. Replaces the common find → click/type/clear pattern. For 'type' and 'clear_and_type', provide the text parameter. For 'read', returns the element's text content.",
    {
      using: locatorEnum.describe("Locator strategy"),
      value: z.string().describe("Locator value"),
      action: actionEnum.describe("Action to perform"),
      text: z.string().optional().describe("Text to type (required for 'type' and 'clear_and_type' actions)"),
      fromElement: z.string().optional().describe("Parent element ID for scoped search"),
    },
    async ({ using, value, action, text, fromElement }) => {
      try {
        await driverManager.ensureRunning();
        const elementId = await client.findElement(using as LocatorStrategy, value, fromElement ?? undefined);

        switch (action) {
          case "click":
            await client.click(elementId);
            return { content: [{ type: "text" as const, text: `Clicked "${value}".` }] };

          case "right_click":
            await client.rightClick(elementId);
            return { content: [{ type: "text" as const, text: `Right-clicked "${value}".` }] };

          case "double_click":
            await client.doubleClick(elementId);
            return { content: [{ type: "text" as const, text: `Double-clicked "${value}".` }] };


          case "type":
            if (!text) return { content: [{ type: "text" as const, text: `'text' parameter required for type action.` }], isError: true };
            await client.sendKeys(elementId, text);
            return { content: [{ type: "text" as const, text: `Typed "${text}" into "${value}".` }] };

          case "clear":
            await client.clear(elementId);
            return { content: [{ type: "text" as const, text: `Cleared "${value}".` }] };

          case "clear_and_type":
            if (!text) return { content: [{ type: "text" as const, text: `'text' parameter required for clear_and_type action.` }], isError: true };
            await client.clear(elementId);
            await client.sendKeys(elementId, text);
            return { content: [{ type: "text" as const, text: `Cleared and typed "${text}" into "${value}".` }] };

          case "read": {
            const content = await client.getText(elementId);
            return { content: [{ type: "text" as const, text: content || "(empty)" }] };
          }

          default:
            return { content: [{ type: "text" as const, text: `Unknown action: ${action}` }], isError: true };
        }
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text" as const, text: `Interact failed on "${value}": ${enrichError(msg, { using, value })}` }], isError: true };
      }
    }
  );

  // ==========================================
  // win_batch — Multiple actions in one call
  // ==========================================
  server.tool(
    "win_batch",
    "Execute multiple find-and-act operations in sequence. Each step finds an element and performs an action. Stops on first error. Returns results for all completed steps.",
    {
      steps: z.array(z.object({
        using: locatorEnum.describe("Locator strategy"),
        value: z.string().describe("Locator value"),
        action: actionEnum.describe("Action to perform"),
        text: z.string().optional().describe("Text for type/clear_and_type actions"),
      })).describe("Array of steps to execute in order"),
    },
    async ({ steps }) => {
      try {
        await driverManager.ensureRunning();
        const results: string[] = [];

        for (let i = 0; i < steps.length; i++) {
          const step = steps[i];
          try {
            const elementId = await client.findElement(step.using as LocatorStrategy, step.value);

            switch (step.action) {
              case "click":
                await client.click(elementId);
                results.push(`[${i + 1}] Clicked "${step.value}"`);
                break;
              case "right_click":
                await client.rightClick(elementId);
                results.push(`[${i + 1}] Right-clicked "${step.value}"`);
                break;
              case "double_click":
                await client.doubleClick(elementId);
                results.push(`[${i + 1}] Double-clicked "${step.value}"`);
                break;
              case "type":
                if (!step.text) { results.push(`[${i + 1}] ERROR: text required`); continue; }
                await client.sendKeys(elementId, step.text);
                results.push(`[${i + 1}] Typed "${step.text}" into "${step.value}"`);
                break;
              case "clear":
                await client.clear(elementId);
                results.push(`[${i + 1}] Cleared "${step.value}"`);
                break;
              case "clear_and_type":
                if (!step.text) { results.push(`[${i + 1}] ERROR: text required`); continue; }
                await client.clear(elementId);
                await client.sendKeys(elementId, step.text);
                results.push(`[${i + 1}] Cleared and typed "${step.text}" into "${step.value}"`);
                break;
              case "read": {
                const text = await client.getText(elementId);
                results.push(`[${i + 1}] "${step.value}" = "${text || "(empty)"}"`);
                break;
              }
              default:
                results.push(`[${i + 1}] ${step.action} "${step.value}"`);
            }
          } catch (err) {
            const msg = err instanceof Error ? err.message : String(err);
            results.push(`[${i + 1}] FAILED "${step.value}": ${enrichError(msg, { using: step.using, value: step.value })}`);
            // Stop on first error
            break;
          }
        }

        return { content: [{ type: "text" as const, text: results.join("\n") }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text" as const, text: `Batch failed: ${msg}` }], isError: true };
      }
    }
  );

  // ==========================================
  // win_read_all — Bulk read element values
  // ==========================================
  server.tool(
    "win_read_all",
    "Read text values from multiple elements at once. Useful for verifying form fields, reading labels, or checking multiple values after an action.",
    {
      elements: z.array(z.object({
        using: locatorEnum.describe("Locator strategy"),
        value: z.string().describe("Locator value"),
        attribute: z.string().optional().describe("Attribute to read instead of text (e.g., 'Value.Value', 'Name')"),
      })).describe("Array of elements to read"),
    },
    async ({ elements }) => {
      try {
        await driverManager.ensureRunning();

        const results = await Promise.all(
          elements.map(async (el) => {
            try {
              const elementId = await client.findElement(el.using as LocatorStrategy, el.value);
              const text = el.attribute
                ? await client.getAttribute(elementId, el.attribute) ?? "(null)"
                : await client.getText(elementId) || "(empty)";
              return { label: el.value, text };
            } catch (err) {
              const msg = err instanceof Error ? err.message : String(err);
              return { label: el.value, text: `ERROR: ${msg}` };
            }
          })
        );

        const lines = results.map(r => `${r.label}: ${r.text}`);
        return { content: [{ type: "text" as const, text: lines.join("\n") }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text" as const, text: `Read all failed: ${msg}` }], isError: true };
      }
    }
  );

  // ==========================================
  // win_wait_for — Wait for a condition
  // ==========================================
  server.tool(
    "win_wait_for",
    "Wait until a UI condition is met. Polls server-side so no token cost during the wait. Conditions: 'element_visible' (element appears), 'element_gone' (element disappears), 'element_enabled' (element becomes enabled), 'window_count' (number of windows changes), 'text_equals' (element text equals expected), 'text_contains' (element text contains substring).",
    {
      condition: z.enum(["element_visible", "element_gone", "element_enabled", "window_count", "text_equals", "text_contains"]).describe("Condition to wait for"),
      using: locatorEnum.optional().describe("Locator strategy (for element conditions)"),
      value: z.string().optional().describe("Locator value (for element conditions)"),
      expected: z.number().optional().describe("Expected window count (for window_count condition)"),
      expectedText: z.string().optional().describe("Expected text value (for text_equals/text_contains conditions)"),
      timeout: z.number().optional().default(10).describe("Max seconds to wait (default: 10)"),
      poll: z.number().optional().default(500).describe("Poll interval in ms (default: 500)"),
    },
    async ({ condition, using, value, expected, expectedText, timeout, poll }) => {
      try {
        await driverManager.ensureRunning();
        const deadline = Date.now() + timeout * 1000;

        while (Date.now() < deadline) {
          let conditionMet = false;

          switch (condition) {
            case "element_visible":
              if (!using || !value) return { content: [{ type: "text" as const, text: "'using' and 'value' required for element conditions." }], isError: true };
              try {
                const elId = await client.findElement(using as LocatorStrategy, value);
                conditionMet = await client.isDisplayed(elId);
              } catch { conditionMet = false; }
              break;

            case "element_gone":
              if (!using || !value) return { content: [{ type: "text" as const, text: "'using' and 'value' required for element conditions." }], isError: true };
              try {
                await client.findElement(using as LocatorStrategy, value);
                conditionMet = false; // Element still exists
              } catch { conditionMet = true; } // Element gone
              break;

            case "element_enabled":
              if (!using || !value) return { content: [{ type: "text" as const, text: "'using' and 'value' required for element conditions." }], isError: true };
              try {
                const elId = await client.findElement(using as LocatorStrategy, value);
                conditionMet = await client.isEnabled(elId);
              } catch { conditionMet = false; }
              break;

            case "window_count":
              if (expected === undefined) return { content: [{ type: "text" as const, text: "'expected' required for window_count condition." }], isError: true };
              try {
                const handles = await client.getWindowHandles();
                conditionMet = handles.length === expected;
              } catch { conditionMet = false; }
              break;

            case "text_equals":
              if (!using || !value || expectedText === undefined) return { content: [{ type: "text" as const, text: "'using', 'value', and 'expectedText' required for text_equals." }], isError: true };
              try {
                const elId = await client.findElement(using as LocatorStrategy, value);
                const text = await client.getText(elId);
                conditionMet = text === expectedText;
              } catch { conditionMet = false; }
              break;

            case "text_contains":
              if (!using || !value || expectedText === undefined) return { content: [{ type: "text" as const, text: "'using', 'value', and 'expectedText' required for text_contains." }], isError: true };
              try {
                const elId = await client.findElement(using as LocatorStrategy, value);
                const text = await client.getText(elId);
                conditionMet = text.includes(expectedText);
              } catch { conditionMet = false; }
              break;
          }

          if (conditionMet) {
            const elapsed = ((Date.now() + timeout * 1000 - deadline) / 1000).toFixed(1);
            return { content: [{ type: "text" as const, text: `Condition "${condition}" met after ${elapsed}s.` }] };
          }

          await new Promise(r => setTimeout(r, poll));
        }

        return {
          content: [{ type: "text" as const, text: `Timeout: "${condition}" not met within ${timeout}s.` }],
          isError: true,
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text" as const, text: `Wait failed: ${msg}` }], isError: true };
      }
    }
  );

  // ==========================================
  // win_diff — What changed since last observation?
  // ==========================================

  // State tracking for diff
  let lastExploreState: Map<string, string> | null = null;

  server.tool(
    "win_diff",
    "Show what changed on screen since the last win_explore or win_observe call. Reports new elements, removed elements, and changed values. Call win_explore first to establish a baseline.",
    {},
    async () => {
      try {
        await driverManager.ensureRunning();
        const xml = await client.getPageSource();
        const { interactive, content: contentEls } = buildScreenSummary(xml, 5, false);
        const allElements = [...interactive, ...contentEls];

        // Build current state map: key -> display string
        const currentState = new Map<string, string>();
        for (const el of allElements) {
          const key = el.automationId || `${el.type}|${el.name}`;
          const val = el.value ? ` = "${el.value}"` : "";
          currentState.set(key, `${el.type} "${el.name}"${val}`);
        }

        if (!lastExploreState) {
          // First call — establish baseline
          lastExploreState = currentState;
          return { content: [{ type: "text" as const, text: `Baseline established with ${currentState.size} elements. Call win_diff again after an action to see changes.` }] };
        }

        // Compare
        const added: string[] = [];
        const removed: string[] = [];
        const changed: string[] = [];

        for (const [key, display] of currentState) {
          if (!lastExploreState.has(key)) {
            added.push(`  + ${display}`);
          } else if (lastExploreState.get(key) !== display) {
            changed.push(`  ~ ${lastExploreState.get(key)}  →  ${display}`);
          }
        }
        for (const [key, display] of lastExploreState) {
          if (!currentState.has(key)) {
            removed.push(`  - ${display}`);
          }
        }

        // Update baseline
        lastExploreState = currentState;

        if (added.length === 0 && removed.length === 0 && changed.length === 0) {
          return { content: [{ type: "text" as const, text: "No changes detected." }] };
        }

        const lines: string[] = [];
        if (added.length > 0) { lines.push("New:", ...added); }
        if (removed.length > 0) { lines.push("Removed:", ...removed); }
        if (changed.length > 0) { lines.push("Changed:", ...changed); }

        return { content: [{ type: "text" as const, text: lines.join("\n") }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text" as const, text: `Diff failed: ${msg}` }], isError: true };
      }
    }
  );

  // ==========================================
  // win_clipboard — Read/write system clipboard
  // ==========================================
  server.tool(
    "win_clipboard",
    "Read or write the system clipboard. Useful for copying data from/to the application under test.",
    {
      action: z.enum(["read", "write"]).describe("'read' to get clipboard content, 'write' to set it"),
      text: z.string().optional().describe("Text to write to clipboard (required for 'write' action)"),
    },
    async ({ action, text }) => {
      try {
        if (action === "read") {
          const result = execSync("powershell -NoProfile -Command \"Get-Clipboard\"", { encoding: "utf8" }).trim();
          return { content: [{ type: "text" as const, text: result || "(empty)" }] };
        } else {
          if (!text) return { content: [{ type: "text" as const, text: "'text' required for write action." }], isError: true };
          // Escape for PowerShell
          const escaped = text.replace(/'/g, "''");
          execSync(`powershell -NoProfile -Command "Set-Clipboard -Value '${escaped}'"`, { encoding: "utf8" });
          return { content: [{ type: "text" as const, text: `Clipboard set to: "${text}"` }] };
        }
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text" as const, text: `Clipboard failed: ${msg}` }], isError: true };
      }
    }
  );

  // ==========================================
  // win_hover — Hover over an element
  // ==========================================
  server.tool(
    "win_hover",
    "Hover the mouse over an element. Useful for revealing tooltips, dropdown menus, or hover-dependent UI.",
    {
      using: locatorEnum.describe("Locator strategy"),
      value: z.string().describe("Locator value"),
      duration: z.number().optional().default(500).describe("How long to hover in ms (default: 500)"),
    },
    async ({ using, value, duration }) => {
      try {
        await driverManager.ensureRunning();
        const elementId = await client.findElement(using as LocatorStrategy, value);
        await client.hover(elementId, duration);
        return { content: [{ type: "text" as const, text: `Hovered over "${value}" for ${duration}ms.` }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text" as const, text: `Hover failed: ${enrichError(msg, { using, value })}` }], isError: true };
      }
    }
  );

  // ==========================================
  // win_form_fields — Read all form fields at once
  // ==========================================
  server.tool(
    "win_form_fields",
    "Discover and read all form fields on the current screen. Returns field names, types, current values, and enabled state. Useful for verifying form state or understanding what fields are available.",
    {
      scope: z.string().optional().describe("Element ID to scope search to (optional)"),
    },
    async ({ scope }) => {
      try {
        await driverManager.ensureRunning();
        const xml = await client.getPageSource();
        const tree = parseXml(xml);
        const flat = flattenElements(tree, 10, true);

        const FORM_TYPES = new Set(["Edit", "TextBox", "ComboBox", "CheckBox", "RadioButton", "Spinner", "Slider"]);
        const fields = flat.filter(el => FORM_TYPES.has(el.controlType));

        if (fields.length === 0) {
          return { content: [{ type: "text" as const, text: "No form fields found on this screen." }] };
        }

        const lines: string[] = [`Found ${fields.length} form field(s):`, ""];
        for (let i = 0; i < fields.length; i++) {
          const f = fields[i];
          const label = f.name || f.automationId || "(no label)";
          const val = f.value ? `"${f.value}"` : '""';
          const state = f.isEnabled ? "" : " [disabled]";
          lines.push(`  ${i + 1}. ${f.controlType} "${label}"${state}`);
          lines.push(`     Value: ${val}`);
          if (f.automationId) lines.push(`     AutomationId: ${f.automationId}`);
        }

        return { content: [{ type: "text" as const, text: lines.join("\n") }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text" as const, text: `Form fields failed: ${msg}` }], isError: true };
      }
    }
  );

  // ==========================================
  // win_menu — Navigate a menu path
  // ==========================================
  server.tool(
    "win_menu",
    "Navigate a menu by clicking a sequence of menu items. Provide the menu path as an array (e.g., ['File', 'Save As']). Each item is clicked in order with a small delay between clicks.",
    {
      path: z.array(z.string()).describe("Menu item names in order (e.g., ['File', 'Save As'])"),
      using: locatorEnum.optional().default("name").describe("Locator strategy for menu items (default: 'name')"),
    },
    async ({ path, using }) => {
      try {
        await driverManager.ensureRunning();

        for (let i = 0; i < path.length; i++) {
          const item = path[i];
          try {
            const elementId = await client.findElement(using as LocatorStrategy, item);
            await client.click(elementId);
            // Small delay to let submenus render
            if (i < path.length - 1) {
              await new Promise(r => setTimeout(r, 300));
            }
          } catch (err) {
            const msg = err instanceof Error ? err.message : String(err);
            const completed = path.slice(0, i).join(" → ");
            const failedAt = `Failed at "${item}" (step ${i + 1}/${path.length})`;
            return {
              content: [{ type: "text" as const, text: `${failedAt}: ${enrichError(msg, { using, value: item })}${completed ? `\nCompleted: ${completed}` : ""}` }],
              isError: true,
            };
          }
        }

        return { content: [{ type: "text" as const, text: `Menu navigated: ${path.join(" → ")}` }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text" as const, text: `Menu navigation failed: ${msg}` }], isError: true };
      }
    }
  );

  // ==========================================
  // win_select_option — Select from ComboBox/ListBox
  // ==========================================
  server.tool(
    "win_select_option",
    "Select an option from a ComboBox or ListBox. Expands the control, finds the option by name, and clicks it.",
    {
      using: locatorEnum.describe("Locator strategy for the ComboBox/ListBox"),
      value: z.string().describe("Locator value for the ComboBox/ListBox"),
      option: z.string().describe("The option text to select"),
    },
    async ({ using, value, option }) => {
      try {
        await driverManager.ensureRunning();

        // Find and click the ComboBox to expand it
        const comboId = await client.findElement(using as LocatorStrategy, value);
        await client.click(comboId);

        // Small delay for dropdown to open
        await new Promise(r => setTimeout(r, 300));

        // Find the option by name and click it
        try {
          const optionId = await client.findElement("name" as LocatorStrategy, option);
          await client.click(optionId);
          return { content: [{ type: "text" as const, text: `Selected "${option}" from "${value}".` }] };
        } catch {
          // Try as list item
          try {
            const optionId = await client.findElement("xpath" as LocatorStrategy, `//ListItem[@Name="${option}"]`);
            await client.click(optionId);
            return { content: [{ type: "text" as const, text: `Selected "${option}" from "${value}".` }] };
          } catch {
            // Press Escape to close the dropdown
            await client.sendKeyboardKeys("\uE00C"); // ESCAPE
            return {
              content: [{ type: "text" as const, text: `Option "${option}" not found in "${value}". Dropdown closed.` }],
              isError: true,
            };
          }
        }
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text" as const, text: `Select failed: ${enrichError(msg, { using, value })}` }], isError: true };
      }
    }
  );

  // ==========================================
  // win_grid_edit — Batch grid cell editing
  // ==========================================
  server.tool(
    "win_grid_edit",
    "Edit multiple cells in an MSFlexGrid in one call. Uses server-side grid endpoints for reliable cell access. Much more efficient than navigating with keyboard for each cell.",
    {
      gridLocator: z.object({
        using: locatorEnum.describe("Locator strategy for the grid"),
        value: z.string().describe("Locator value for the grid"),
      }).describe("How to find the grid element"),
      edits: z.array(z.object({
        row: z.number().describe("Row index (0-based, excluding header)"),
        col: z.number().optional().default(0).describe("Column index (0-based, default: 0)"),
        value: z.string().describe("Value to set in the cell"),
      })).describe("Array of cell edits to perform"),
      editFieldId: z.number().optional().default(22).describe("Control ID of the dynamic edit field (default: 22)"),
    },
    async ({ gridLocator, edits, editFieldId }) => {
      try {
        await driverManager.ensureRunning();
        const gridId = await client.findElement(gridLocator.using as LocatorStrategy, gridLocator.value);

        const results: string[] = [];
        for (let i = 0; i < edits.length; i++) {
          const edit = edits[i];
          try {
            await client.setGridCellValue(gridId, edit.row, edit.col, edit.value, editFieldId);
            results.push(`[${i + 1}] Row ${edit.row}, Col ${edit.col} = "${edit.value}"`);
          } catch (err) {
            const msg = err instanceof Error ? err.message : String(err);
            results.push(`[${i + 1}] FAILED Row ${edit.row}, Col ${edit.col}: ${msg}`);
            break;
          }
        }

        return { content: [{ type: "text" as const, text: results.join("\n") }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text" as const, text: `Grid edit failed: ${enrichError(msg, gridLocator)}` }], isError: true };
      }
    }
  );

}
