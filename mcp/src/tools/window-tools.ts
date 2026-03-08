import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { DriverManager } from "../driver-manager.js";
import type { WebDriverClient } from "../webdriver-client.js";
import type { WindowAction } from "../types.js";

export function registerWindowTools(
  server: McpServer,
  driverManager: DriverManager,
  client: WebDriverClient
): void {
  server.tool(
    "win_page_source",
    "Get the UI element tree of the application as XML. Useful for understanding the structure of the app and finding element locators.",
    {},
    async () => {
      try {
        const xml = await client.getPageSource();
        return { content: [{ type: "text", text: xml }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Get page source failed: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_window_handle",
    "Get the current window handle as a hex string.",
    {},
    async () => {
      try {
        const handle = await client.getWindowHandle();
        return { content: [{ type: "text", text: handle }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Get window handle failed: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_list_windows",
    "List all window handles for the current application process.",
    {},
    async () => {
      try {
        const handles = await client.getWindowHandles();
        return {
          content: [{
            type: "text",
            text: JSON.stringify({ count: handles.length, handles }, null, 2),
          }],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `List windows failed: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_switch_window",
    "Switch to a different window by its hex handle.",
    {
      handle: z.string().describe("Hex window handle to switch to (from win_list_windows)"),
    },
    async ({ handle }) => {
      try {
        await client.switchToWindow(handle);
        return { content: [{ type: "text", text: `Switched to window ${handle}.` }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Switch window failed: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_set_window",
    "Set the window state: maximize, minimize, or fullscreen.",
    {
      action: z.enum(["maximize", "minimize", "fullscreen"]).describe("Window action to perform"),
    },
    async ({ action }) => {
      try {
        const actionMap: Record<WindowAction, () => Promise<void>> = {
          maximize: () => client.maximizeWindow(),
          minimize: () => client.minimizeWindow(),
          fullscreen: () => client.fullscreenWindow(),
        };
        await actionMap[action as WindowAction]();
        return { content: [{ type: "text", text: `Window ${action}d.` }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Set window failed: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_close_window",
    "Close the current window (sends WM_CLOSE). This does not end the session.",
    {},
    async () => {
      try {
        await client.closeWindow();
        return { content: [{ type: "text", text: "Window closed." }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Close window failed: ${msg}` }], isError: true };
      }
    }
  );
}
