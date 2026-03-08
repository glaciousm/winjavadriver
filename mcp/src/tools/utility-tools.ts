import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import { execSync } from "node:child_process";
import { writeFileSync, unlinkSync, mkdtempSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { randomBytes } from "node:crypto";
import type { DriverManager } from "../driver-manager.js";
import { WebDriverClient } from "../webdriver-client.js";
import type { WindowInfo } from "../types.js";

export function registerUtilityTools(
  server: McpServer,
  driverManager: DriverManager,
  client: WebDriverClient
): void {
  server.tool(
    "win_send_keys",
    `Send keyboard keys to the focused element. Supports special keys: ENTER, TAB, ESCAPE, BACKSPACE, DELETE, LEFT, RIGHT, UP, DOWN, HOME, END, PAGE_UP, PAGE_DOWN, F1-F12, SHIFT, CONTROL, ALT. Multiple keys can be combined (e.g., "ENTER" or "TAB TAB ENTER"). Use repeat syntax: "DOWN*5" sends DOWN 5 times.`,
    {
      keys: z.string().describe("Space-separated key names (e.g., 'ENTER', 'DOWN*5', 'TAB TAB', 'CONTROL a') or text to type"),
      elementId: z.string().optional().describe("Optional element ID to focus before sending keys"),
    },
    async ({ keys, elementId }) => {
      try {
        // Parse the keys - split by space and resolve each, supporting repeat syntax (e.g., "DOWN*5")
        const keyParts = keys.split(/\s+/);
        let resolvedKeys = "";
        for (const part of keyParts) {
          const repeatMatch = part.match(/^(.+)\*(\d+)$/);
          if (repeatMatch) {
            const key = WebDriverClient.resolveKey(repeatMatch[1]);
            const count = parseInt(repeatMatch[2], 10);
            resolvedKeys += key.repeat(count);
          } else {
            resolvedKeys += WebDriverClient.resolveKey(part);
          }
        }

        await client.sendKeyboardKeys(resolvedKeys, elementId);
        return { content: [{ type: "text", text: `Sent keys: ${keys}` }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Send keys failed: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_get_logs",
    "Get the last N lines from the WinJavaDriver server logs. Requires verbose mode to be enabled.",
    {
      lines: z.number().optional().default(50).describe("Number of log lines to retrieve (default: 50)"),
    },
    async ({ lines }) => {
      try {
        const logs = driverManager.getLogs(lines);
        if (logs.length === 0) {
          return {
            content: [{
              type: "text",
              text: "No logs available. Make sure verbose mode is enabled (set WINJAVADRIVER_VERBOSE=true or use win_set_verbose).",
            }],
          };
        }
        return { content: [{ type: "text", text: logs.join("\n") }] };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Get logs failed: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_set_verbose",
    "Enable or disable verbose logging for the WinJavaDriver server. Takes effect on next server start.",
    {
      enabled: z.boolean().describe("Whether to enable verbose logging"),
    },
    async ({ enabled }) => {
      driverManager.setVerbose(enabled);
      return {
        content: [{
          type: "text",
          text: `Verbose logging ${enabled ? "enabled" : "disabled"}. Will take effect on next server start.`,
        }],
      };
    }
  );

  server.tool(
    "win_clear_logs",
    "Clear the server log buffer.",
    {},
    async () => {
      driverManager.clearLogs();
      return { content: [{ type: "text", text: "Log buffer cleared." }] };
    }
  );

  server.tool(
    "win_list_all_windows",
    "List all visible top-level windows on the system. Useful for finding window handles to attach to.",
    {
      filter: z.string().optional().describe("Optional filter string to match against window titles (case-insensitive)"),
    },
    async ({ filter }) => {
      try {
        // Write PowerShell script to temp file (here-strings don't work inline)
        const psScript = `Add-Type -TypeDefinition @"
using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Text;
public class Win32Enum {
    public delegate bool EnumWindowsProc(IntPtr hwnd, IntPtr lParam);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);
    [DllImport("user32.dll")] public static extern int GetWindowText(IntPtr hwnd, StringBuilder lpString, int nMaxCount);
    [DllImport("user32.dll")] public static extern int GetWindowTextLength(IntPtr hwnd);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hwnd);
    [DllImport("user32.dll")] public static extern int GetClassName(IntPtr hwnd, StringBuilder lpClassName, int nMaxCount);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hwnd, out uint lpdwProcessId);
}
"@
$results = @()
$callback = {
    param([IntPtr]$hwnd, [IntPtr]$lParam)
    if ([Win32Enum]::IsWindowVisible($hwnd)) {
        $len = [Win32Enum]::GetWindowTextLength($hwnd)
        if ($len -gt 0) {
            $sb = New-Object System.Text.StringBuilder($len + 1)
            [Win32Enum]::GetWindowText($hwnd, $sb, $sb.Capacity) | Out-Null
            $title = $sb.ToString()
            $classSb = New-Object System.Text.StringBuilder(256)
            [Win32Enum]::GetClassName($hwnd, $classSb, 256) | Out-Null
            $procId = [uint32]0
            [Win32Enum]::GetWindowThreadProcessId($hwnd, [ref]$procId) | Out-Null
            $script:results += [PSCustomObject]@{
                handle = "0x" + $hwnd.ToInt64().ToString("X")
                title = $title
                className = $classSb.ToString()
                processId = $procId
            }
        }
    }
    return $true
}
[Win32Enum]::EnumWindows($callback, [IntPtr]::Zero) | Out-Null
$results | ConvertTo-Json -Compress
`;
        const scriptPath = join(tmpdir(), `wjd-enum-windows-${randomBytes(8).toString("hex")}.ps1`);
        writeFileSync(scriptPath, psScript);

        const output = execSync(`powershell -NoProfile -ExecutionPolicy Bypass -File "${scriptPath}"`, {
          encoding: "utf8",
          maxBuffer: 10 * 1024 * 1024,
        });

        // Clean up temp file
        try { unlinkSync(scriptPath); } catch { /* ignore */ }

        let windows: WindowInfo[] = JSON.parse(output.trim() || "[]");

        // Apply filter if provided
        if (filter) {
          const filterLower = filter.toLowerCase();
          windows = windows.filter(w =>
            w.title.toLowerCase().includes(filterLower) ||
            w.className.toLowerCase().includes(filterLower)
          );
        }

        return {
          content: [{
            type: "text",
            text: JSON.stringify({ count: windows.length, windows }, null, 2),
          }],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `List windows failed: ${msg}` }], isError: true };
      }
    }
  );
}
