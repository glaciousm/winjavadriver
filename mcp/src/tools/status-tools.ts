import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { DriverManager } from "../driver-manager.js";
import type { WebDriverClient } from "../webdriver-client.js";

export function registerStatusTools(
  server: McpServer,
  driverManager: DriverManager,
  client: WebDriverClient
): void {
  server.tool(
    "win_status",
    "Check if the WinJavaDriver server is running and ready for automation",
    {},
    async () => {
      try {
        await driverManager.ensureRunning();
        const status = await client.getStatus();
        return {
          content: [{ type: "text", text: JSON.stringify(status, null, 2) }],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return {
          content: [{ type: "text", text: `WinJavaDriver is not available: ${msg}` }],
          isError: true,
        };
      }
    }
  );
}
