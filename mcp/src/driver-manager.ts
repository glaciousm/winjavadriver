import { spawn, ChildProcess } from "node:child_process";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { existsSync } from "node:fs";

const __dirname = dirname(fileURLToPath(import.meta.url));

/** Default paths to search for winjavadriver.exe, relative to the mcp/ directory */
const DEFAULT_EXE_PATHS = [
  resolve(__dirname, "../../server/WinJavaDriver/bin/Release/net8.0-windows/win-x64/winjavadriver.exe"),
  resolve(__dirname, "../../server/WinJavaDriver/bin/Release/net8.0-windows/win-x64/publish/winjavadriver.exe"),
];

/** Maximum log lines to keep in buffer */
const MAX_LOG_LINES = 500;

export class DriverManager {
  private process: ChildProcess | null = null;
  private port: number;
  private exePath: string;
  private baseUrl: string;
  private starting = false;
  private verbose = false;
  private logBuffer: string[] = [];

  constructor(options?: { port?: number; exePath?: string; verbose?: boolean }) {
    this.port = options?.port ?? parseInt(process.env.WINJAVADRIVER_PORT ?? "9515", 10);
    this.exePath = options?.exePath ?? process.env.WINJAVADRIVER_PATH ?? this.findExe();
    this.baseUrl = `http://localhost:${this.port}`;
    this.verbose = options?.verbose ?? process.env.WINJAVADRIVER_VERBOSE === "true";
  }

  /** Enable or disable verbose logging for subsequent starts */
  setVerbose(enabled: boolean): void {
    this.verbose = enabled;
  }

  /** Get the last N lines from the server log buffer */
  getLogs(lines = 50): string[] {
    return this.logBuffer.slice(-lines);
  }

  /** Clear the log buffer */
  clearLogs(): void {
    this.logBuffer = [];
  }

  private appendLog(line: string): void {
    this.logBuffer.push(line);
    if (this.logBuffer.length > MAX_LOG_LINES) {
      this.logBuffer.shift();
    }
  }

  private findExe(): string {
    for (const p of DEFAULT_EXE_PATHS) {
      if (existsSync(p)) return p;
    }
    throw new Error(
      `winjavadriver.exe not found. Searched:\n${DEFAULT_EXE_PATHS.join("\n")}\n` +
      `Set WINJAVADRIVER_PATH environment variable to the correct path.`
    );
  }

  getBaseUrl(): string {
    return this.baseUrl;
  }

  getPort(): number {
    return this.port;
  }

  isRunning(): boolean {
    return this.process !== null && this.process.exitCode === null;
  }

  async ensureRunning(): Promise<void> {
    // Already running
    if (this.isRunning()) return;

    // Check if an external instance is already running
    if (await this.isServerReady()) return;

    // Prevent concurrent start attempts
    if (this.starting) {
      await this.waitForReady(10000);
      return;
    }

    this.starting = true;
    try {
      const args = ["--port", String(this.port)];
      if (this.verbose) {
        args.push("--verbose");
      }

      this.process = spawn(this.exePath, args, {
        stdio: ["ignore", "pipe", "pipe"],
        detached: false,
        windowsHide: true,
      });

      // Capture stdout for verbose logs
      this.process.stdout?.on("data", (data: Buffer) => {
        const lines = data.toString().split("\n").filter(l => l.trim());
        lines.forEach(line => this.appendLog(line));
      });

      // Capture stderr for error logs
      this.process.stderr?.on("data", (data: Buffer) => {
        const lines = data.toString().split("\n").filter(l => l.trim());
        lines.forEach(line => this.appendLog(`[ERR] ${line}`));
      });

      this.process.on("error", (err) => {
        this.appendLog(`[PROCESS ERROR] ${err.message}`);
        console.error(`winjavadriver process error: ${err.message}`);
      });

      this.process.on("exit", (code) => {
        this.appendLog(`[PROCESS EXIT] code=${code}`);
        this.process = null;
        if (code !== null && code !== 0) {
          console.error(`winjavadriver exited with code ${code}`);
        }
      });

      await this.waitForReady(10000);
    } finally {
      this.starting = false;
    }
  }

  private async isServerReady(): Promise<boolean> {
    try {
      const resp = await fetch(`${this.baseUrl}/status`, { signal: AbortSignal.timeout(2000) });
      if (resp.ok) {
        const data = await resp.json();
        return data?.value?.ready === true;
      }
      return false;
    } catch {
      return false;
    }
  }

  private async waitForReady(timeoutMs: number): Promise<void> {
    const start = Date.now();
    while (Date.now() - start < timeoutMs) {
      if (await this.isServerReady()) return;
      await new Promise((r) => setTimeout(r, 300));
    }
    throw new Error(`winjavadriver did not become ready within ${timeoutMs}ms`);
  }

  async shutdown(): Promise<void> {
    if (this.process && this.process.exitCode === null) {
      this.process.kill("SIGTERM");
      // Give it a moment to shut down gracefully
      await new Promise((r) => setTimeout(r, 1000));
      if (this.process && this.process.exitCode === null) {
        this.process.kill("SIGKILL");
      }
      this.process = null;
    }
  }
}
