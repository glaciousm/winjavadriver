import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { DriverManager } from "../driver-manager.js";
import type { WebDriverClient } from "../webdriver-client.js";
import type { LocatorStrategy } from "../types.js";

const locatorEnum = z.enum(["name", "accessibility id", "class name", "tag name", "xpath"]);

export function registerFindTools(
  server: McpServer,
  driverManager: DriverManager,
  client: WebDriverClient
): void {
  server.tool(
    "win_find_element",
    "Find a single UI element. Strategies: 'name' (display text), 'accessibility id' (AutomationId, most reliable), 'class name' (Win32 class), 'tag name' (control type like 'button'), 'xpath' (XPath over UI tree).",
    {
      using: locatorEnum.describe("Locator strategy"),
      value: z.string().describe("Locator value (e.g., 'Save', 'btnSave', 'Edit', 'button', '//Button[@Name=\"Save\"]')"),
      fromElement: z.string().optional().describe("Parent element ID for scoped search. If omitted, searches from root."),
    },
    async ({ using, value, fromElement }) => {
      try {
        await driverManager.ensureRunning();
        const elementId = await client.findElement(using as LocatorStrategy, value, fromElement ?? undefined);
        return {
          content: [{ type: "text", text: JSON.stringify({ elementId }, null, 2) }],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Element not found: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_find_elements",
    "Find multiple UI elements matching a locator. Returns element IDs, optionally with summary info (name, automationId, className, displayed, rect).",
    {
      using: locatorEnum.describe("Locator strategy"),
      value: z.string().describe("Locator value"),
      fromElement: z.string().optional().describe("Parent element ID for scoped search"),
      includeInfo: z.boolean().optional().default(false).describe("Include element summary info (name, automationId, className, displayed, rect)"),
    },
    async ({ using, value, fromElement, includeInfo }) => {
      try {
        await driverManager.ensureRunning();

        if (includeInfo) {
          const elements = await client.findElementsWithInfo(using as LocatorStrategy, value, fromElement ?? undefined);
          return {
            content: [{
              type: "text",
              text: JSON.stringify({ count: elements.length, elements }, null, 2),
            }],
          };
        } else {
          const elementIds = await client.findElements(using as LocatorStrategy, value, fromElement ?? undefined);
          return {
            content: [{
              type: "text",
              text: JSON.stringify({ count: elementIds.length, elementIds }, null, 2),
            }],
          };
        }
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Find failed: ${msg}` }], isError: true };
      }
    }
  );
}
