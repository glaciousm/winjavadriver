/** W3C WebDriver element identifier constant */
export const W3C_ELEMENT_ID = "element-6066-11e4-a52e-4f735466cecf";

/** W3C success response shape */
export interface W3CResponse<T = unknown> {
  value: T;
}

/** W3C error value */
export interface W3CErrorValue {
  error: string;
  message: string;
  stacktrace: string;
}

/** Session created response */
export interface SessionCreatedValue {
  sessionId: string;
  capabilities: Record<string, unknown>;
}

/** Element bounding rect */
export interface ElementRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

/** Comprehensive element info (combo tool result) */
export interface ElementInfo {
  text: string;
  tagName: string;
  enabled: boolean;
  displayed: boolean;
  selected: boolean;
  rect: ElementRect;
}

/** Extended element info with additional attributes */
export interface ElementInfoExtended extends ElementInfo {
  name: string | null;
  automationId: string | null;
  className: string | null;
}

/** Element summary for find_elements results */
export interface ElementSummary {
  elementId: string;
  name: string | null;
  automationId: string | null;
  className: string | null;
  displayed: boolean;
  rect: ElementRect;
}

/** Window info for list_all_windows */
export interface WindowInfo {
  handle: string;
  title: string;
  className: string;
  processId: number;
}

/** Locator strategies supported by winjavadriver */
export type LocatorStrategy =
  | "name"
  | "accessibility id"
  | "class name"
  | "tag name"
  | "xpath";

/** Window actions */
export type WindowAction = "maximize" | "minimize" | "fullscreen";
