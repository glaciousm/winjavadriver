import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { z } from "zod";
import type { DriverManager } from "../driver-manager.js";
import type { WebDriverClient } from "../webdriver-client.js";

/**
 * Register MSFlexGrid extension tools.
 * These provide cell-level access to VB6 MSFlexGrid controls.
 */
export function registerGridTools(
  server: McpServer,
  driverManager: DriverManager,
  client: WebDriverClient
): void {
  server.tool(
    "win_grid_cell",
    "Create a virtual element for an MSFlexGrid cell. Grid cells are not exposed as individual UI elements, so this creates a wrapper element that can be clicked, typed into, etc.",
    {
      gridElementId: z.string().describe("Element ID of the MSFlexGrid (class MSFlexGridWndClass)"),
      row: z.number().describe("Row index (0-based, excluding header row)"),
      col: z.number().describe("Column index (0-based). Usually 1 for the 'Content' column"),
      fieldName: z.string().optional().describe("Optional display name for the cell (e.g., 'F02_PAN')"),
      editFieldId: z.number().optional().describe("Control ID of the dynamic edit field (default: 22)"),
      editableColumn: z.number().optional().describe("The only column this grid's editor can read or write (default: 1). Text operations on other columns are rejected rather than silently using this column"),
    },
    async ({ gridElementId, row, col, fieldName, editFieldId, editableColumn }) => {
      try {
        const cellElementId = await client.getGridCell(gridElementId, row, col, fieldName, editFieldId, editableColumn);
        return {
          content: [{
            type: "text",
            text: JSON.stringify({
              elementId: cellElementId,
              row,
              col,
              fieldName: fieldName ?? "",
            }, null, 2),
          }],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Failed to create grid cell: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_grid_get_value",
    "Get the value of an MSFlexGrid cell. Moves the grid selection by row/column steps (no screen coordinates, so it works at any resolution) and reads the grid editor. Only the editable column can be read.",
    {
      gridElementId: z.string().describe("Element ID of the MSFlexGrid"),
      row: z.number().describe("Row index (0-based, excluding header)"),
      col: z.number().describe("Column index (0-based); must be the editable column"),
      editFieldId: z.number().optional().describe("Control ID of the dynamic edit field (default: 22)"),
      editableColumn: z.number().optional().describe("The only column this grid's editor can read (default: 1). Other columns are rejected rather than silently returning this column's value"),
    },
    async ({ gridElementId, row, col, editFieldId, editableColumn }) => {
      try {
        const value = await client.getGridCellValue(gridElementId, row, col, editFieldId, editableColumn);
        return {
          content: [{
            type: "text",
            text: JSON.stringify({ row, col, value }, null, 2),
          }],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Failed to get cell value: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_grid_set_value",
    "Set the value of an MSFlexGrid cell. Moves the grid selection by row/column steps (no screen coordinates, so it works at any resolution) and types into the grid editor. Only the editable column can be written.",
    {
      gridElementId: z.string().describe("Element ID of the MSFlexGrid"),
      row: z.number().describe("Row index (0-based, excluding header)"),
      col: z.number().describe("Column index (0-based)"),
      value: z.string().describe("Value to set in the cell"),
      editFieldId: z.number().optional().describe("Control ID of the dynamic edit field (default: 22)"),
      editableColumn: z.number().optional().describe("The only column this grid's editor can write (default: 1). Other columns are rejected rather than writing to the wrong cell"),
    },
    async ({ gridElementId, row, col, value, editFieldId, editableColumn }) => {
      try {
        await client.setGridCellValue(gridElementId, row, col, value, editFieldId, editableColumn);
        return {
          content: [{
            type: "text",
            text: `Set cell (${row}, ${col}) to "${value}" successfully.`,
          }],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Failed to set cell value: ${msg}` }], isError: true };
      }
    }
  );

  server.tool(
    "win_grid_info",
    "Get information about an MSFlexGrid control, including dimensions and dynamic edit field detection.",
    {
      gridElementId: z.string().describe("Element ID of the MSFlexGrid"),
    },
    async ({ gridElementId }) => {
      try {
        const info = await client.getGridInfo(gridElementId);
        return {
          content: [{
            type: "text",
            text: JSON.stringify(info, null, 2),
          }],
        };
      } catch (err) {
        const msg = err instanceof Error ? err.message : String(err);
        return { content: [{ type: "text", text: `Failed to get grid info: ${msg}` }], isError: true };
      }
    }
  );
}
