#!/usr/bin/env node

import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { DriverManager } from "./driver-manager.js";
import { WebDriverClient } from "./webdriver-client.js";
import { registerStatusTools } from "./tools/status-tools.js";
import { registerSessionTools } from "./tools/session-tools.js";
import { registerFindTools } from "./tools/find-tools.js";
import { registerElementTools } from "./tools/element-tools.js";
import { registerPropertyTools } from "./tools/property-tools.js";
import { registerWindowTools } from "./tools/window-tools.js";
import { registerGridTools } from "./tools/grid-tools.js";
import { registerUtilityTools } from "./tools/utility-tools.js";
import { registerSmartTools } from "./tools/smart-tools.js";

// Create MCP server
const server = new McpServer({
  name: "winjavadriver",
  version: "1.0.0",
});

// Create driver manager and WebDriver HTTP client
const driverManager = new DriverManager();
const client = new WebDriverClient(driverManager.getBaseUrl());

// Register all tool groups
registerStatusTools(server, driverManager, client);
registerSessionTools(server, driverManager, client);
registerFindTools(server, driverManager, client);
registerElementTools(server, driverManager, client);
registerPropertyTools(server, driverManager, client);
registerWindowTools(server, driverManager, client);
registerGridTools(server, driverManager, client);
registerUtilityTools(server, driverManager, client);
registerSmartTools(server, driverManager, client);

// Graceful shutdown
async function cleanup() {
  await driverManager.shutdown();
  process.exit(0);
}

process.on("SIGINT", cleanup);
process.on("SIGTERM", cleanup);

// Start stdio transport
const transport = new StdioServerTransport();
await server.connect(transport);
