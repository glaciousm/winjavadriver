import {
  W3C_ELEMENT_ID,
  type W3CErrorValue,
  type SessionCreatedValue,
  type ElementRect,
  type ElementInfo,
  type ElementInfoExtended,
  type ElementSummary,
  type LocatorStrategy,
} from "./types.js";

export class WebDriverClient {
  private baseUrl: string;
  private currentSessionId: string | null = null;

  constructor(baseUrl: string) {
    this.baseUrl = baseUrl;
  }

  getCurrentSessionId(): string | null {
    return this.currentSessionId;
  }

  // --- Session Management ---

  async createSession(capabilities: Record<string, unknown>): Promise<SessionCreatedValue> {
    const result = await this.request<SessionCreatedValue>("POST", "/session", {
      capabilities: {
        alwaysMatch: {
          platformName: "windows",
          ...capabilities,
        },
      },
    });
    this.currentSessionId = result.sessionId;
    return result;
  }

  async deleteSession(sessionId?: string): Promise<void> {
    const sid = sessionId ?? this.resolveSessionId();
    await this.request("DELETE", `/session/${sid}`);
    if (sid === this.currentSessionId) {
      this.currentSessionId = null;
    }
  }

  // --- Element Finding ---

  async findElement(strategy: LocatorStrategy, value: string, fromElement?: string): Promise<string> {
    const sid = this.resolveSessionId();
    const path = fromElement
      ? `/session/${sid}/element/${fromElement}/element`
      : `/session/${sid}/element`;
    const result = await this.request<Record<string, string>>("POST", path, {
      using: strategy,
      value,
    });
    return result[W3C_ELEMENT_ID];
  }

  async findElements(strategy: LocatorStrategy, value: string, fromElement?: string): Promise<string[]> {
    const sid = this.resolveSessionId();
    const path = fromElement
      ? `/session/${sid}/element/${fromElement}/elements`
      : `/session/${sid}/elements`;
    const result = await this.request<Record<string, string>[]>("POST", path, {
      using: strategy,
      value,
    });
    return result.map((el) => el[W3C_ELEMENT_ID]);
  }

  // --- Element Interactions ---

  async click(elementId: string): Promise<void> {
    const sid = this.resolveSessionId();
    await this.request("POST", `/session/${sid}/element/${elementId}/click`, {});
  }

  async sendKeys(elementId: string, text: string): Promise<void> {
    const sid = this.resolveSessionId();
    await this.request("POST", `/session/${sid}/element/${elementId}/value`, { text });
  }

  async clear(elementId: string): Promise<void> {
    const sid = this.resolveSessionId();
    await this.request("POST", `/session/${sid}/element/${elementId}/clear`, {});
  }

  // --- Element Properties ---

  async getText(elementId: string): Promise<string> {
    const sid = this.resolveSessionId();
    return this.request<string>("GET", `/session/${sid}/element/${elementId}/text`);
  }

  async getTagName(elementId: string): Promise<string> {
    const sid = this.resolveSessionId();
    return this.request<string>("GET", `/session/${sid}/element/${elementId}/name`);
  }

  async isEnabled(elementId: string): Promise<boolean> {
    const sid = this.resolveSessionId();
    return this.request<boolean>("GET", `/session/${sid}/element/${elementId}/enabled`);
  }

  async isDisplayed(elementId: string): Promise<boolean> {
    const sid = this.resolveSessionId();
    return this.request<boolean>("GET", `/session/${sid}/element/${elementId}/displayed`);
  }

  async isSelected(elementId: string): Promise<boolean> {
    const sid = this.resolveSessionId();
    return this.request<boolean>("GET", `/session/${sid}/element/${elementId}/selected`);
  }

  async getRect(elementId: string): Promise<ElementRect> {
    const sid = this.resolveSessionId();
    return this.request<ElementRect>("GET", `/session/${sid}/element/${elementId}/rect`);
  }

  async getAttribute(elementId: string, name: string): Promise<string | null> {
    const sid = this.resolveSessionId();
    return this.request<string | null>("GET", `/session/${sid}/element/${elementId}/attribute/${name}`);
  }

  async getElementInfo(elementId: string): Promise<ElementInfo> {
    const [text, tagName, enabled, displayed, selected, rect] = await Promise.all([
      this.getText(elementId),
      this.getTagName(elementId),
      this.isEnabled(elementId),
      this.isDisplayed(elementId),
      this.isSelected(elementId),
      this.getRect(elementId),
    ]);
    return { text, tagName, enabled, displayed, selected, rect };
  }

  async getElementInfoExtended(elementId: string): Promise<ElementInfoExtended> {
    const [info, name, automationId, className] = await Promise.all([
      this.getElementInfo(elementId),
      this.getAttribute(elementId, "Name"),
      this.getAttribute(elementId, "AutomationId"),
      this.getAttribute(elementId, "ClassName"),
    ]);
    return { ...info, name, automationId, className };
  }

  async getElementSummary(elementId: string): Promise<ElementSummary> {
    const [name, automationId, className, displayed, rect] = await Promise.all([
      this.getAttribute(elementId, "Name"),
      this.getAttribute(elementId, "AutomationId"),
      this.getAttribute(elementId, "ClassName"),
      this.isDisplayed(elementId),
      this.getRect(elementId),
    ]);
    return { elementId, name, automationId, className, displayed, rect };
  }

  async findElementsWithInfo(strategy: LocatorStrategy, value: string, fromElement?: string): Promise<ElementSummary[]> {
    const elementIds = await this.findElements(strategy, value, fromElement);
    // Fetch info for each element in parallel
    return Promise.all(elementIds.map(id => this.getElementSummary(id)));
  }

  // --- Screenshots ---

  async takeScreenshot(elementId?: string): Promise<string> {
    const sid = this.resolveSessionId();
    const path = elementId
      ? `/session/${sid}/element/${elementId}/screenshot`
      : `/session/${sid}/screenshot`;
    return this.request<string>("GET", path);
  }

  // --- Window Management ---

  async getPageSource(): Promise<string> {
    const sid = this.resolveSessionId();
    return this.request<string>("GET", `/session/${sid}/source`);
  }

  async getWindowHandle(): Promise<string> {
    const sid = this.resolveSessionId();
    return this.request<string>("GET", `/session/${sid}/window`);
  }

  async getWindowHandles(): Promise<string[]> {
    const sid = this.resolveSessionId();
    return this.request<string[]>("GET", `/session/${sid}/window/handles`);
  }

  async switchToWindow(handle: string): Promise<void> {
    const sid = this.resolveSessionId();
    await this.request("POST", `/session/${sid}/window`, { handle });
  }

  async maximizeWindow(): Promise<void> {
    const sid = this.resolveSessionId();
    await this.request("POST", `/session/${sid}/window/maximize`, {});
  }

  async minimizeWindow(): Promise<void> {
    const sid = this.resolveSessionId();
    await this.request("POST", `/session/${sid}/window/minimize`, {});
  }

  async fullscreenWindow(): Promise<void> {
    const sid = this.resolveSessionId();
    await this.request("POST", `/session/${sid}/window/fullscreen`, {});
  }

  async closeWindow(): Promise<void> {
    const sid = this.resolveSessionId();
    await this.request("DELETE", `/session/${sid}/window`);
  }

  // --- MSFlexGrid Extension ---

  async getGridCell(gridElementId: string, row: number, col: number, fieldName?: string, editFieldId?: number): Promise<string> {
    const sid = this.resolveSessionId();
    const result = await this.request<Record<string, string>>("POST", `/session/${sid}/winjavadriver/grid/${gridElementId}/cell`, {
      row,
      col,
      fieldName: fieldName ?? "",
      editFieldId: editFieldId ?? 22,
    });
    return result[W3C_ELEMENT_ID];
  }

  async getGridCellValue(gridElementId: string, row: number, col: number, editFieldId?: number): Promise<string> {
    const sid = this.resolveSessionId();
    return this.request<string>("POST", `/session/${sid}/winjavadriver/grid/${gridElementId}/cell/value`, {
      row,
      col,
      editFieldId: editFieldId ?? 22,
    });
  }

  async setGridCellValue(gridElementId: string, row: number, col: number, value: string, editFieldId?: number): Promise<void> {
    const sid = this.resolveSessionId();
    await this.request("PUT", `/session/${sid}/winjavadriver/grid/${gridElementId}/cell/value`, {
      row,
      col,
      value,
      editFieldId: editFieldId ?? 22,
    });
  }

  async getGridInfo(gridElementId: string): Promise<{ className: string; rect: ElementRect; dynamicEditFieldId: number | null; editFieldFound: boolean }> {
    const sid = this.resolveSessionId();
    return this.request<{ className: string; rect: ElementRect; dynamicEditFieldId: number | null; editFieldFound: boolean }>("GET", `/session/${sid}/winjavadriver/grid/${gridElementId}/info`);
  }

  // --- Keyboard Actions ---

  // --- W3C Actions ---

  async clickAt(elementId: string, xOffset: number, yOffset: number): Promise<void> {
    const sid = this.resolveSessionId();
    await this.request("POST", `/session/${sid}/actions`, {
      actions: [{
        type: "pointer",
        id: "mouse",
        parameters: { pointerType: "mouse" },
        actions: [
          { type: "pointerMove", origin: { [W3C_ELEMENT_ID]: elementId }, x: xOffset, y: yOffset },
          { type: "pointerDown", button: 0 },
          { type: "pointerUp", button: 0 },
        ],
      }],
    });
  }

  async hover(elementId: string, durationMs: number = 500): Promise<void> {
    const sid = this.resolveSessionId();
    await this.request("POST", `/session/${sid}/actions`, {
      actions: [{
        type: "pointer",
        id: "mouse",
        parameters: { pointerType: "mouse" },
        actions: [
          { type: "pointerMove", origin: { [W3C_ELEMENT_ID]: elementId }, x: 0, y: 0 },
          { type: "pause", duration: durationMs },
        ],
      }],
    });
  }

  async rightClick(elementId: string): Promise<void> {
    const sid = this.resolveSessionId();
    await this.request("POST", `/session/${sid}/actions`, {
      actions: [{
        type: "pointer",
        id: "mouse",
        parameters: { pointerType: "mouse" },
        actions: [
          { type: "pointerMove", origin: { [W3C_ELEMENT_ID]: elementId }, x: 0, y: 0 },
          { type: "pointerDown", button: 2 },
          { type: "pointerUp", button: 2 },
        ],
      }],
    });
  }

  async doubleClick(elementId: string): Promise<void> {
    const sid = this.resolveSessionId();
    await this.request("POST", `/session/${sid}/actions`, {
      actions: [{
        type: "pointer",
        id: "mouse",
        parameters: { pointerType: "mouse" },
        actions: [
          { type: "pointerMove", origin: { [W3C_ELEMENT_ID]: elementId }, x: 0, y: 0 },
          { type: "pointerDown", button: 0 },
          { type: "pointerUp", button: 0 },
          { type: "pointerDown", button: 0 },
          { type: "pointerUp", button: 0 },
        ],
      }],
    });
  }

  /** Unicode key codes for special keys (W3C WebDriver spec) */
  static readonly KEYS: Record<string, string> = {
    // Modifier keys
    NULL: "\uE000",
    CANCEL: "\uE001",
    HELP: "\uE002",
    BACKSPACE: "\uE003",
    TAB: "\uE004",
    CLEAR: "\uE005",
    RETURN: "\uE006",
    ENTER: "\uE007",
    SHIFT: "\uE008",
    CONTROL: "\uE009",
    ALT: "\uE00A",
    PAUSE: "\uE00B",
    ESCAPE: "\uE00C",
    SPACE: "\uE00D",
    PAGE_UP: "\uE00E",
    PAGE_DOWN: "\uE00F",
    END: "\uE010",
    HOME: "\uE011",
    LEFT: "\uE012",
    UP: "\uE013",
    RIGHT: "\uE014",
    DOWN: "\uE015",
    INSERT: "\uE016",
    DELETE: "\uE017",
    SEMICOLON: "\uE018",
    EQUALS: "\uE019",
    // Numpad
    NUMPAD0: "\uE01A",
    NUMPAD1: "\uE01B",
    NUMPAD2: "\uE01C",
    NUMPAD3: "\uE01D",
    NUMPAD4: "\uE01E",
    NUMPAD5: "\uE01F",
    NUMPAD6: "\uE020",
    NUMPAD7: "\uE021",
    NUMPAD8: "\uE022",
    NUMPAD9: "\uE023",
    MULTIPLY: "\uE024",
    ADD: "\uE025",
    SEPARATOR: "\uE026",
    SUBTRACT: "\uE027",
    DECIMAL: "\uE028",
    DIVIDE: "\uE029",
    // Function keys
    F1: "\uE031",
    F2: "\uE032",
    F3: "\uE033",
    F4: "\uE034",
    F5: "\uE035",
    F6: "\uE036",
    F7: "\uE037",
    F8: "\uE038",
    F9: "\uE039",
    F10: "\uE03A",
    F11: "\uE03B",
    F12: "\uE03C",
  };

  /** W3C modifier key codes */
  private static readonly MODIFIER_KEYS = new Set([
    "\uE008", // SHIFT
    "\uE009", // CONTROL
    "\uE00A", // ALT
    "\uE03D", // META
  ]);

  /** Send keyboard keys to the active element or a specific element */
  async sendKeyboardKeys(keys: string, elementId?: string): Promise<void> {
    const sid = this.resolveSessionId();

    // If an element is specified, click it first to focus
    if (elementId) {
      await this.click(elementId);
    }

    // Build W3C Actions sequence with proper modifier handling.
    // Modifier keys (SHIFT, CONTROL, ALT) are held down until the end of the sequence
    // or until a NULL key (\uE000) is encountered.
    const keyActions: Array<{ type: string; value?: string }> = [];
    const heldModifiers: string[] = [];

    for (const key of keys) {
      if (key === "\uE000") {
        // NULL key — release all held modifiers
        for (const mod of heldModifiers.reverse()) {
          keyActions.push({ type: "keyUp", value: mod });
        }
        heldModifiers.length = 0;
      } else if (WebDriverClient.MODIFIER_KEYS.has(key)) {
        // Modifier key — hold it down, release at end
        keyActions.push({ type: "keyDown", value: key });
        heldModifiers.push(key);
      } else {
        // Regular key — press and release immediately
        keyActions.push({ type: "keyDown", value: key });
        keyActions.push({ type: "keyUp", value: key });
      }
    }

    // Release any remaining held modifiers (in reverse order)
    for (const mod of heldModifiers.reverse()) {
      keyActions.push({ type: "keyUp", value: mod });
    }

    await this.request("POST", `/session/${sid}/actions`, {
      actions: [
        {
          type: "key",
          id: "keyboard",
          actions: keyActions,
        },
      ],
    });
  }

  /** Convert key name (e.g., "ENTER") to unicode character */
  static resolveKey(keyName: string): string {
    const upper = keyName.toUpperCase();
    if (upper in WebDriverClient.KEYS) {
      return WebDriverClient.KEYS[upper];
    }
    // If not a special key, return as-is (single character)
    return keyName;
  }

  // --- Status ---

  async getStatus(): Promise<{ ready: boolean; message: string }> {
    return this.request<{ ready: boolean; message: string }>("GET", "/status");
  }

  // --- Internal ---

  private resolveSessionId(sessionId?: string): string {
    const sid = sessionId ?? this.currentSessionId;
    if (!sid) {
      throw new Error("No active session. Call win_launch_app or win_attach_app first.");
    }
    return sid;
  }

  private async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const url = `${this.baseUrl}${path}`;
    const options: RequestInit = {
      method,
      headers: body !== undefined ? { "Content-Type": "application/json" } : undefined,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      signal: AbortSignal.timeout(30000),
    };

    let response: Response;
    try {
      response = await fetch(url, options);
    } catch (err) {
      const msg = err instanceof Error ? err.message : String(err);
      throw new Error(`Failed to connect to winjavadriver at ${this.baseUrl}: ${msg}`);
    }

    const json = await response.json() as { value: T | W3CErrorValue };

    // Check for W3C error response
    if (
      json.value !== null &&
      typeof json.value === "object" &&
      "error" in (json.value as Record<string, unknown>)
    ) {
      const err = json.value as W3CErrorValue;
      throw new Error(`[${err.error}] ${err.message}`);
    }

    return json.value as T;
  }
}
