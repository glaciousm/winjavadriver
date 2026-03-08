import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import { execSync } from "node:child_process";
import { writeFileSync, unlinkSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { randomBytes } from "node:crypto";
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
          // Use PowerShell with Win32 BitBlt for robust screen capture
          // (CopyFromScreen can fail with "handle is invalid" after window switches)
          const psScript = `
Add-Type @"
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
public class ScreenCapture {
    [DllImport("user32.dll")] static extern IntPtr GetDesktopWindow();
    [DllImport("user32.dll")] static extern IntPtr GetWindowDC(IntPtr hWnd);
    [DllImport("user32.dll")] static extern int ReleaseDC(IntPtr hWnd, IntPtr hDC);
    [DllImport("gdi32.dll")] static extern IntPtr CreateCompatibleDC(IntPtr hdc);
    [DllImport("gdi32.dll")] static extern IntPtr CreateCompatibleBitmap(IntPtr hdc, int w, int h);
    [DllImport("gdi32.dll")] static extern IntPtr SelectObject(IntPtr hdc, IntPtr obj);
    [DllImport("gdi32.dll")] static extern bool BitBlt(IntPtr hdcDest, int x1, int y1, int w, int h, IntPtr hdcSrc, int x2, int y2, uint rop);
    [DllImport("gdi32.dll")] static extern bool DeleteDC(IntPtr hdc);
    [DllImport("gdi32.dll")] static extern bool DeleteObject(IntPtr obj);
    [DllImport("user32.dll")] static extern int GetSystemMetrics(int nIndex);
    const uint SRCCOPY = 0x00CC0020;
    public static string Capture() {
        int w = GetSystemMetrics(0); // SM_CXSCREEN
        int h = GetSystemMetrics(1); // SM_CYSCREEN
        IntPtr desktop = GetDesktopWindow();
        IntPtr hdc = GetWindowDC(desktop);
        IntPtr memDC = CreateCompatibleDC(hdc);
        IntPtr hBmp = CreateCompatibleBitmap(hdc, w, h);
        IntPtr old = SelectObject(memDC, hBmp);
        BitBlt(memDC, 0, 0, w, h, hdc, 0, 0, SRCCOPY);
        SelectObject(memDC, old);
        Bitmap bmp = Bitmap.FromHbitmap(hBmp);
        var ms = new System.IO.MemoryStream();
        bmp.Save(ms, ImageFormat.Png);
        string result = Convert.ToBase64String(ms.ToArray());
        ms.Dispose(); bmp.Dispose();
        DeleteObject(hBmp); DeleteDC(memDC);
        ReleaseDC(desktop, hdc);
        return result;
    }
}
"@
[ScreenCapture]::Capture()
`;
          const scriptPath = join(tmpdir(), `wjd-screenshot-${randomBytes(8).toString("hex")}.ps1`);
          writeFileSync(scriptPath, psScript);
          try {
            const base64 = execSync(`powershell -NoProfile -ExecutionPolicy Bypass -File "${scriptPath}"`, {
              encoding: "utf8",
              maxBuffer: 50 * 1024 * 1024,
              timeout: 10000,
            }).trim();
            return {
              content: [{
                type: "image" as const,
                data: base64,
                mimeType: "image/png",
              }],
            };
          } finally {
            try { unlinkSync(scriptPath); } catch { /* ignore */ }
          }
        }

        const base64 = await client.takeScreenshot(elementId ?? undefined);
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
