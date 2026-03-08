import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { DriverManager } from "../driver-manager.js";
import type { WebDriverClient } from "../webdriver-client.js";

export function registerSessionTools(
  server: McpServer,
  driverManager: DriverManager,
  client: WebDriverClient
): void {
  server.tool(
    "win_launch_app",
    "Launch a Windows application and create an automation session. Returns session ID and capabilities.",
    {
      app: z.string().describe("Path to executable (e.g., 'notepad.exe', 'calc.exe', 'C:\\\\Program Files\\\\MyApp\\\\app.exe')"),
      appArguments: z.string().optional().describe("Command line arguments for the app"),
      appWorkingDir: z.string().optional().describe("Working directory for the app"),
      waitForAppLaunch: z.number().optional().default(10).describe("Seconds to wait for the app window to appear (default: 10)"),
      verbose: z.boolean().optional().default(false).describe("Enable verbose server logging for debugging"),
    },
    async ({ app, appArguments, appWorkingDir, waitForAppLaunch, verbose }) => {
      try {
        // Enable verbose logging if requested
        if (verbose) {
          driverManager.setVerbose(true);
        }

        await driverManager.ensureRunning();
        const capabilities: Record<string, unknown> = {
          "winjavadriver:app": app,
          "winjavadriver:waitForAppLaunch": waitForAppLaunch,
          "winjavadriver:shouldCloseApp": true,
        };
        if (appArguments) capabilities["winjavadriver:appArguments"] = appArguments;
        if (appWorkingDir) capabilities["winjavadriver:appWorkingDir"] = appWorkingDir;

        const result = await client.createSession(capabilities);
        return {
          content: [{ type: "text", text: JSON.stringify(result, null, 2) }],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Failed to launch app: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_attach_app",
    "Attach to a running Windows application by its window handle (hex string like '0x1A2B3C').",
    {
      appTopLevelWindow: z.string().describe("Hex window handle of the target app (e.g., '0x1A2B3C')"),
    },
    async ({ appTopLevelWindow }) => {
      try {
        await driverManager.ensureRunning();
        const result = await client.createSession({
          "winjavadriver:appTopLevelWindow": appTopLevelWindow,
          "winjavadriver:shouldCloseApp": false,
        });
        return {
          content: [{ type: "text", text: JSON.stringify(result, null, 2) }],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Failed to attach: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_quit",
    "Close the automation session and the application.",
    {
      sessionId: z.string().optional().describe("Session ID to close. If omitted, closes the current session."),
    },
    async ({ sessionId }) => {
      try {
        await client.deleteSession(sessionId ?? undefined);
        return {
          content: [{ type: "text", text: "Session closed successfully." }],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Failed to quit: ${msg}` }], isError: true };
      }
    }
  );
}
