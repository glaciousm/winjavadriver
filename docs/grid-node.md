# WinJavaDriver Grid Node Setup

Run desktop UI tests on remote Windows machines using Selenium Grid 4. WinJavaDriver integrates via the built-in relay feature — the same proven pattern used by Appium.

## Deployment Modes

WinJavaDriver supports two Grid deployment modes. Choose based on your infrastructure.

### Mode 1: Standalone (Recommended)

Each Windows machine runs a self-contained Grid (Hub + Node + relay) in one process. Test clients connect directly to the machine's Grid URL. Simple, requires only **one port (4444)**, and works in restrictive corporate networks.

```
Test Client (any machine)              Windows Machine
┌──────────────────┐            ┌──────────────────────────────┐
│ Maven / JUnit    │            │ Selenium Grid Standalone     │
│ WinJavaDriver    │   HTTP     │   Port 4444                  │
│ tests            │ ─────────→ │   [relay] → localhost:9515   │
└──────────────────┘            │          │                   │
                                │          ▼                   │
                                │ winjavadriver.exe :9515      │
                                │   (W3C WebDriver server)     │
                                │                              │
                                │ Interactive desktop session  │
                                └──────────────────────────────┘
```

For parallel execution across multiple machines, CI distributes tests at the pipeline level (e.g., Jenkins parallel stages each pointing at a different machine's Grid URL).

### Mode 2: Hub and Node (Multi-Machine Grid)

A central Hub routes sessions to remote Nodes. Enables automatic session queuing and distribution. Requires the Hub's **ZMQ Event Bus ports (4442-4443)** to be reachable from Node machines — see [Corporate Firewall Considerations](#corporate-firewall-considerations).

```
Test Client (any machine)                   Windows Machine (Node)
┌──────────────────┐                  ┌──────────────────────────────┐
│ Maven / JUnit    │                  │ Selenium Grid Node           │
│ WinJavaDriver    │     HTTP         │   --detect-drivers false     │
│ tests            │ ──────────┐      │   [relay] → localhost:9515   │
└──────────────────┘           │      │          │                   │
                               ▼      │          ▼                   │
                    ┌──────────────┐  │ winjavadriver.exe :9515      │
                    │ Selenium Hub │──│   (W3C WebDriver server)     │
                    │ Port 4444    │  │                              │
                    │ ZMQ 4442-43  │  │ Interactive desktop session  │
                    └──────────────┘  └──────────────────────────────┘
```

If you already have a Selenium Grid for browser tests, you just add WinJavaDriver nodes alongside your Chrome/Firefox nodes. Same Hub, same dashboard, same CI pipeline.

## Prerequisites

### Node machine (Windows) — both modes
- Windows 10/11
- Java 11+ (for Selenium Grid)
- `winjavadriver.exe` (self-contained, no .NET runtime needed)
- `selenium-server.jar` ([download](https://github.com/SeleniumHQ/selenium/releases))
- Firewall: **inbound TCP 4444** open (test clients connect here)
- An interactive desktop session (see [Desktop Session](#desktop-session) below)

### Hub machine (Mode 2 only)
- Java 11+
- `selenium-server.jar`
- Firewall: **inbound TCP 4444** (HTTP API) + **inbound TCP 4442-4443** (ZMQ Event Bus)

### Node machine additional (Mode 2 only)
- Firewall: **inbound TCP 5555** open (Hub calls back to validate node)
- Network: **outbound TCP 4442-4443** to Hub must not be filtered (ZMQ Event Bus)

### Firewall setup (run as admin)

**Standalone mode (on each node machine):**
```powershell
New-NetFirewallRule -DisplayName "Selenium Grid (Port 4444)" -Direction Inbound -Protocol TCP -LocalPort 4444 -Action Allow
```

**Hub + Node mode — Hub machine:**
```powershell
New-NetFirewallRule -DisplayName "Selenium Grid Hub (Port 4444)" -Direction Inbound -Protocol TCP -LocalPort 4444 -Action Allow
New-NetFirewallRule -DisplayName "Selenium Grid Event Bus (4442-4443)" -Direction Inbound -Protocol TCP -LocalPort 4442,4443 -Action Allow
```

**Hub + Node mode — Node machine:**
```powershell
New-NetFirewallRule -DisplayName "Selenium Grid Node (Port 5555)" -Direction Inbound -Protocol TCP -LocalPort 5555 -Action Allow
```

## Quick Start — Standalone Mode (Recommended)

### Node Machine Setup (Windows)

RDP into the Windows machine and follow these steps.

#### 1. Clone or pull the WinJavaDriver repo

```cmd
git clone https://github.com/glaciousm/winjavadriver.git
cd winjavadriver
```

Or if already cloned:
```cmd
cd <your-winjavadriver-path>
git pull origin main
```

> **Note:** The repo path varies per machine. All examples below use `%WINJAVADRIVER%` — set it to your actual path:
> ```cmd
> set WINJAVADRIVER=D:\Development\Java\winjavadriver
> ```

#### 2. Build winjavadriver

```cmd
cd %WINJAVADRIVER%\server\WinJavaDriver
dotnet publish -c Release -r win-x64 --self-contained
```

#### 3. Download selenium-server.jar

```cmd
mkdir %WINJAVADRIVER%\tools
curl -L -o %WINJAVADRIVER%\tools\selenium-server.jar https://github.com/SeleniumHQ/selenium/releases/download/selenium-4.29.0/selenium-server-4.29.0.jar
```

#### 4. Start winjavadriver (Terminal 1)

```cmd
%WINJAVADRIVER%\server\WinJavaDriver\bin\Release\net8.0-windows\win-x64\publish\winjavadriver.exe --port 9515
```

Wait for the "ready" message.

#### 5. Start the Grid (Terminal 2)

```cmd
java -jar %WINJAVADRIVER%\tools\selenium-server.jar standalone --config %WINJAVADRIVER%\configs\winjavadriver-node.toml --port 4444
```

Verify: open `http://node-machine:4444/ui` in a browser. The relay should appear with `windows/winjavadriver` capability.

#### 6. Disconnect RDP without locking (run as admin)

```cmd
%WINJAVADRIVER%\scripts\disconnect-rdp.cmd
```

Then close the RDP window. The desktop stays active and unlocked.

## Quick Start — Hub and Node Mode

### Hub Machine Setup

Start the Selenium Grid Hub on any machine (Linux, Windows, or Mac):

```bash
java -jar selenium-server.jar hub --port 4444
```

Verify: open `http://hub-machine:4444/ui` in a browser.

### Node Machine Setup (Windows)

RDP into the Windows machine. Follow steps 1-4 from Standalone mode above, then:

#### 5. Start the Grid Node (Terminal 2)

```cmd
java -jar %WINJAVADRIVER%\tools\selenium-server.jar node --config %WINJAVADRIVER%\configs\winjavadriver-node.toml --hub hub-machine
```

Replace `hub-machine` with the Hub's hostname or IP address. The `--hub` flag auto-configures the Event Bus addresses (`tcp://hub-machine:4442` and `tcp://hub-machine:4443`).

> **Multi-NIC machines:** If the node has multiple network adapters, the Grid Node may auto-detect the wrong IP. The Hub validates nodes by calling back to the reported address, so it must be reachable from the Hub. Use `--external-url` to override:
> ```cmd
> java -jar %WINJAVADRIVER%\tools\selenium-server.jar node ^
>   --config %WINJAVADRIVER%\configs\winjavadriver-node.toml ^
>   --hub hub-machine ^
>   --external-url http://node-ip:5555
> ```

The node appears in the Grid UI at `http://hub-machine:4444/ui` with `windows/winjavadriver` capability.

#### 6. Disconnect RDP without locking (run as admin)

```cmd
%WINJAVADRIVER%\scripts\disconnect-rdp.cmd
```

Then close the RDP window. The desktop stays active and unlocked.

### Running Tests

From any machine, point tests at the Grid URL (standalone machine or Hub):

```java
WinJavaOptions options = new WinJavaOptions()
    .setApp("calc.exe")
    .setWaitForAppLaunch(10);

// Grid — routes to the WinJavaDriver node automatically
WinJavaDriver driver = new WinJavaDriver(
    new URL("http://grid-machine:4444"), options);

driver.findElement(WinBy.name("Five")).click();
// ... test logic ...
driver.quit();
```

Or with the example Cucumber tests:

```bash
cd examples/calculator-tests
mvn test -Dcucumber.filter.tags="@Modern and not @Combo" -Dwinjavadriver.grid.url=http://grid-machine:4444
```

> **Note:** App paths in capabilities (e.g., `setApp("C:\\path\\to\\app.exe")`) are resolved on the **node machine**, not the test client. Use paths that exist on the node, or use app names on the system PATH (e.g., `calc.exe`).

## Desktop Session

UI Automation requires an active, unlocked desktop session. This is a Windows platform constraint shared by all desktop automation tools (WinAppDriver, Ranorex, TestComplete).

### Setup (one-time per node machine)

#### Option 1: Automated setup script

```powershell
# Run as Administrator on the node machine
.\scripts\setup-grid-node.ps1 -GridUrl "http://hub-machine:4444"
```

This script:
- Disables screensaver and lock screen
- Disables sleep/standby
- Creates scheduled tasks for auto-start on logon
- Creates `disconnect-rdp.cmd`

#### Option 2: Manual setup

1. **Disable screensaver**: Settings > Personalization > Lock screen > Screen saver settings > None
2. **Disable lock timeout**: Run `gpedit.msc` > Computer Configuration > Windows Settings > Security Settings > Local Policies > Security Options > "Interactive logon: Machine inactivity limit" → 0
3. **Disable sleep**: Settings > System > Power & sleep > Never

### Keeping the session alive

When you connect via RDP and then disconnect, Windows locks the screen — killing UI Automation. The standard workaround:

```cmd
REM Run disconnect-rdp.cmd as Administrator BEFORE closing RDP
REM This disconnects RDP without locking the screen
scripts\disconnect-rdp.cmd
```

This uses `tscon` to transfer the RDP session to the console, keeping the desktop active. All programs continue running.

### Auto-recovery from reboots

The setup script creates scheduled tasks that start winjavadriver.exe and the Grid Node on logon. Combined with auto-logon (optional), the node recovers automatically after Windows Update reboots:

```powershell
# Include auto-logon setup (stores password in registry — use only on dedicated test VMs)
.\scripts\setup-grid-node.ps1 -GridUrl "http://hub:4444" -ConfigureAutoLogon
```

## CI/CD Integration

### Jenkins Pipeline (Docker)

A complete Jenkins setup is included in the `jenkins/` directory:

```bash
cd jenkins
docker compose up -d
# Jenkins UI: http://localhost:8888
```

The Docker Compose maps `grid-node` to the Windows machine running the standalone Grid. The pipeline:
1. Copies (or clones) the source code
2. Builds the WinJavaDriver Java client
3. Runs Modern Calculator tests against the remote Grid
4. Publishes JUnit test results

See `jenkins/Jenkinsfile` for the full pipeline definition.

### Jenkins Pipeline (standalone example)

```groovy
pipeline {
    agent any
    environment {
        GRID_URL = 'http://windows-node:4444'
    }
    stages {
        stage('Desktop UI Tests') {
            steps {
                sh """mvn test \
                    -Dwinjavadriver.grid.url=${GRID_URL} \
                    -Dcucumber.filter.tags='@Modern'"""
            }
        }
    }
}
```

### Same Grid for browser + desktop tests

If your Grid already has Chrome/Firefox nodes, desktop tests use the same Hub:

```groovy
stages {
    stage('Browser Tests') {
        steps {
            // Routes to Chrome node
            sh "mvn test -Dselenium.grid=${GRID_URL} -Dbrowser=chrome"
        }
    }
    stage('Desktop Tests') {
        steps {
            // Routes to WinJavaDriver node (platformName=windows)
            sh "mvn test -Dwinjavadriver.grid.url=${GRID_URL}"
        }
    }
}
```

## Corporate Firewall Considerations

### The ZMQ Event Bus Problem (Hub + Node mode)

Selenium Grid 4 Hub↔Node communication uses **ZeroMQ (ZMQ)** on TCP ports 4442-4443 for node registration and heartbeats. This is a non-HTTP binary protocol.

Many corporate networks employ deep packet inspection (DPI) or port-based filtering that blocks non-HTTP traffic between machines. **If ports 4442-4443 are unreachable between Hub and Node machines, nodes cannot register with the Hub.** This is not a WinJavaDriver-specific issue — it affects all Selenium Grid 4 Hub+Node deployments.

**Symptoms:**
- Node logs show `Sending registration event...` repeatedly but Hub never acknowledges
- `Test-NetConnection -ComputerName hub-ip -Port 4442` hangs from the node machine
- Hub UI shows no registered nodes

**Solutions (in order of preference):**

1. **Use Standalone mode** — Each node runs its own Grid. Only requires port 4444 (HTTP). Works in all corporate networks. See [Standalone Mode](#quick-start--standalone-mode-recommended).

2. **Request firewall exceptions** — Ask IT to open TCP 4442-4443 between Hub and Node machines. This is standard for test infrastructure and is the same requirement as any Selenium Grid 4 deployment.

3. **Dedicated test VLAN** — Place test machines on a network segment with relaxed inter-machine rules. Common in enterprise CI/CD setups.

4. **SSH tunneling** — Forward ZMQ ports through an SSH tunnel:
   ```cmd
   REM On the node machine, tunnel to the Hub's ZMQ ports
   ssh -L 4442:localhost:4442 -L 4443:localhost:4443 hub-user@hub-machine
   REM Then start the node with --hub localhost
   ```

### Standalone Mode Avoids This Entirely

In Standalone mode, Hub and Node run in the same process on the same machine. ZMQ communication is internal (localhost only). Test clients connect via HTTP on port 4444, which passes through virtually all corporate firewalls.

For desktop automation, Standalone mode is often the better choice regardless of firewall rules:
- Desktop automation is inherently **1 session per machine** (one screen)
- A central Hub adds complexity without much benefit for single-session nodes
- CI/CD tools (Jenkins, GitLab CI, Azure DevOps) can distribute tests across machines at the pipeline level

## Multiple Nodes

### Standalone mode (parallel via CI)

```
CI Pipeline
  ├── Stage A → http://vm-1:4444 (standalone Grid + winjavadriver)
  ├── Stage B → http://vm-2:4444 (standalone Grid + winjavadriver)
  └── Stage C → http://vm-3:4444 (standalone Grid + winjavadriver)
```

```groovy
// Jenkins example — parallel desktop tests across machines
parallel {
    stage('VM-1') { steps { sh "mvn test -Dwinjavadriver.grid.url=http://vm-1:4444" } }
    stage('VM-2') { steps { sh "mvn test -Dwinjavadriver.grid.url=http://vm-2:4444" } }
    stage('VM-3') { steps { sh "mvn test -Dwinjavadriver.grid.url=http://vm-3:4444" } }
}
```

### Hub + Node mode (Grid-managed distribution)

```
Hub (port 4444)
  ├── Node A (VM-1): winjavadriver relay → localhost:9515
  ├── Node B (VM-2): winjavadriver relay → localhost:9515
  └── Node C (VM-3): winjavadriver relay → localhost:9515
```

Each node handles one session at a time (desktop automation is single-session per machine). The Grid queues incoming requests and distributes them to available nodes.

## Limitations

| Limitation | Reason | Workaround |
|------------|--------|------------|
| 1 session per node | Desktop automation uses one screen | Use VMs for parallelism |
| Interactive session required | Windows UIA constraint | tscon + disable lock screen |
| Java required on nodes | Selenium Grid Node is Java | JRE 11+ (small footprint) |
| Combo tests (2 apps) deadlock | Single slot per node | Split into separate scenarios or use 2 nodes |

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Node not in Grid UI (Hub+Node mode) | Check `--hub` points to Hub IP; check firewall allows **inbound 4444 + 4442-4443 on Hub** and **inbound 5555 on Node**. Run `Test-NetConnection -ComputerName hub-ip -Port 4442` from the node — if it hangs, ZMQ ports are filtered (see [Corporate Firewall](#corporate-firewall-considerations)) |
| Node sends registration events but Hub ignores | ZMQ Event Bus ports 4442-4443 blocked by corporate firewall/DPI. Use Standalone mode or request firewall exception |
| Node registers but Hub rejects | Hub calls back to the node's reported IP. If the node has multiple NICs, it may pick the wrong one. Use `--external-url http://correct-ip:5555` |
| "Unable to find provider" | Ensure winjavadriver.exe is running on the node (`curl http://localhost:9515/status`) |
| "Cannot assign requested address" | Don't use `--host` with an IP not bound to a local adapter. Use `--external-url` instead |
| App path not found on node | App paths in capabilities resolve on the **node machine**. Use absolute paths valid on that machine, or app names on the system PATH (e.g., `calc.exe`) |
| Test hangs waiting for session | Node slot may be occupied; check Grid UI for active sessions |
| Combo tests deadlock | Grid node has maxSessions=1 (one screen). Tests requiring 2 simultaneous sessions will deadlock. Split into separate scenarios |
| UI Automation fails on node | Screen is locked; re-RDP in, run `disconnect-rdp.cmd`, disconnect |
| Elements not found after RDP disconnect | Screen resolution changed; set resolution before disconnecting |
| Node goes offline after reboot | Configure auto-logon + scheduled tasks via `setup-grid-node.ps1` |

## Security Considerations

| Concern | Mitigation |
|---------|------------|
| Machine stays unlocked | Use dedicated test VMs, not user workstations |
| Auto-logon stores password in registry | Optional; only for dedicated test infrastructure |
| No authentication on Grid | Place Hub behind reverse proxy with auth; restrict network access |
| Corporate GPO re-enables lock screen | Request GPO exception for test machines (standard for automation infra) |
