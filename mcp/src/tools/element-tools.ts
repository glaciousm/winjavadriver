import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { DriverManager } from "../driver-manager.js";
import type { WebDriverClient } from "../webdriver-client.js";

export function registerElementTools(
  server: McpServer,
  driverManager: DriverManager,
  client: WebDriverClient
): void {
  server.tool(
    "win_click",
    "Click a UI element. Optionally specify x,y offset from element center (useful for clicking specific positions within large controls like grids or toolbars).",
    {
      elementId: z.string().describe("Element ID returned by win_find_element"),
      xOffset: z.number().optional().describe("X offset from element center (pixels)"),
      yOffset: z.number().optional().describe("Y offset from element center (pixels)"),
    },
    async ({ elementId, xOffset, yOffset }) => {
      try {
        if (xOffset !== undefined || yOffset !== undefined) {
          await client.clickAt(elementId, xOffset ?? 0, yOffset ?? 0);
        } else {
          await client.click(elementId);
        }
        return { content: [{ type: "text", text: "Clicked successfully." }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Click failed: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_type",
    "Type text into a UI element (e.g., a text field). The element must be focusable.",
    {
      elementId: z.string().describe("Element ID returned by win_find_element"),
      text: z.string().describe("Text to type into the element"),
    },
    async ({ elementId, text }) => {
      try {
        await client.sendKeys(elementId, text);
        return { content: [{ type: "text", text: `Typed "${text}" successfully.` }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Type failed: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_clear",
    "Clear the value of a UI element (e.g., clear a text field).",
    {
      elementId: z.string().describe("Element ID returned by win_find_element"),
    },
    async ({ elementId }) => {
      try {
        await client.clear(elementId);
        return { content: [{ type: "text", text: "Cleared successfully." }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Clear failed: ${msg}` }], isError: true };
      }
    }
  );
}
