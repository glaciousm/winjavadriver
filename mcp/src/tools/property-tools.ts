import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { DriverManager } from "../driver-manager.js";
import type { WebDriverClient } from "../webdriver-client.js";

export function registerPropertyTools(
  server: McpServer,
  driverManager: DriverManager,
  client: WebDriverClient
): void {
  server.tool(
    "win_get_text",
    "Get the text content of a UI element.",
    {
      elementId: z.string().describe("Element ID returned by win_find_element"),
    },
    async ({ elementId }) => {
      try {
        const text = await client.getText(elementId);
        return { content: [{ type: "text", text }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Get text failed: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_get_attribute",
    "Get an attribute value from a UI element. Common attributes: 'Name', 'AutomationId', 'ClassName', 'Value.Value', 'IsEnabled', 'IsOffscreen'.",
    {
      elementId: z.string().describe("Element ID returned by win_find_element"),
      name: z.string().describe("Attribute name (e.g., 'Name', 'AutomationId', 'Value.Value', 'ClassName')"),
    },
    async ({ elementId, name }) => {
      try {
        const value = await client.getAttribute(elementId, name);
        return {
          content: [{ type: "text", text: value !== null ? value : "(null)" }],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Get attribute failed: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_element_info",
    "Get comprehensive information about a UI element: text, tag name, enabled, displayed, selected, rect, plus className, automationId, and name attributes.",
    {
      elementId: z.string().describe("Element ID returned by win_find_element"),
    },
    async ({ elementId }) => {
      try {
        const info = await client.getElementInfoExtended(elementId);
        return {
          content: [{ type: "text", text: JSON.stringify(info, null, 2) }],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Get info failed: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_screenshot",
    "Take a screenshot of the application window, a specific element, or the entire screen. Returns a PNG image.",
    {
      elementId: z.string().optional().describe("Element ID to screenshot. If omitted, screenshots the entire window."),
      fullscreen: z.boolean().optional().default(false).describe("Capture the entire screen instead of just the attached window (useful when dialogs open)"),
    },
    async ({ elementId, fullscreen }) => {
      try {
        if (fullscreen) {
          // Use the server's desktop screenshot endpoint (Win32 BitBlt capture)
          // This is more reliable than the old PowerShell approach which could
          // return empty results depending on the execution context
          const base64 = await client.takeDesktopScreenshot();
          if (!base64) {
            return { content: [{ type: "text", text: "Desktop screenshot returned empty — the server may not have access to the interactive desktop." }], isError: true };
          }
          return {
            content: [{
              type: "image" as const,
              data: base64,
              mimeType: "image/png",
            }],
          };
        }

        const base64 = await client.takeScreenshot(elementId ?? undefined);
        if (!base64) {
          return { content: [{ type: "text", text: "Screenshot returned empty data from server." }], isError: true };
        }
        return {
          content: [{
            type: "image" as const,
            data: base64,
            mimeType: "image/png",
          }],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Screenshot failed: ${msg}` }], isError: true };
      }
    }
  );
}
