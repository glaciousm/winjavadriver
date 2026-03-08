# WinJavaDriver Remote Execution

Run desktop UI tests from Jenkins or any CI pipeline against apps on a remote Windows machine.

## Recommended: Selenium Grid 4 Relay

The recommended approach for remote execution is via Selenium Grid 4. WinJavaDriver integrates as a Grid relay node — zero code changes needed.

See [grid-node.md](grid-node.md) for full setup instructions.

```java
// Point tests at the Grid URL — routes to WinJavaDriver node automatically
WinJavaOptions options = new WinJavaOptions()
    .setApp("calc.exe")
    .setWaitForAppLaunch(10);

WinJavaDriver driver = new WinJavaDriver(
    new URL("http://grid-machine:4444"), options);

// Use exactly like local — all Selenium APIs work
driver.findElement(WinBy.name("Five")).click();
driver.quit();
```

## Alternative: Direct Agent Connection

WinJavaDriver also provides an Agent service that runs on the remote machine. Tests connect directly with Windows credentials:

```java
// Remote — connect to Agent on another machine with Windows credentials
WinJavaOptions options = new WinJavaOptions()
    .setApp("calc.exe")
    .setRemoteUser("testuser")
    .setRemotePassword("pass123")
    .setRemoteDomain("CORP");

WinJavaDriver driver = new WinJavaDriver(
    new URL("http://192.168.1.50:9515"), options);
```

### Local vs Remote — Only the constructor changes

```java
// Local — auto-starts server on same machine
WinJavaDriver driver = new WinJavaDriver(options);

// Remote via Grid (recommended)
WinJavaDriver driver = new WinJavaDriver(new URL("http://grid:4444"), options);

// Remote via Agent (direct)
WinJavaDriver driver = new WinJavaDriver(new URL("http://remote:9515"), options);
```

### Agent Setup (Remote Machine)

1. Copy `winjavadriver-agent.exe` and `winjavadriver.exe` to the remote machine
2. Install as a Windows Service:

```powershell
# Option 1: PowerShell script (recommended)
.\install-agent.ps1 -Port 9515

# Option 2: Manual
sc create WinJavaDriverAgent binPath= "C:\tools\winjavadriver-agent.exe --port 9515 --worker-exe C:\tools\winjavadriver.exe --verbose --log-file C:\tools\agent.log" start= auto
netsh advfirewall firewall add rule name="WinJavaDriver Agent" dir=in action=allow protocol=TCP localport=9515
sc start WinJavaDriverAgent
```

3. Verify: `curl http://machine-b:9515/status`

### Agent CLI Options

```
winjavadriver-agent [options]
  --port <port>            Listening port (default: 9515)
  --worker-exe <path>      Path to winjavadriver.exe (default: same directory)
  --session-timeout <sec>  Idle session timeout (default: 300)
  --verbose                Verbose logging
  --log-file <path>        Log to file
```

## Jenkins Pipeline

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

For a complete Docker-based Jenkins setup, see the `jenkins/` directory.

## Desktop Session Requirement

UI Automation requires an active, unlocked desktop session on the remote machine. This is a Windows platform constraint shared by all desktop automation tools.

See the [Desktop Session](grid-node.md#desktop-session) section in the Grid Node docs for setup instructions.

## Troubleshooting

| Problem | Solution |
|---------|----------|
| Agent returns 401 | Invalid Windows credentials — check username, password, domain |
| "No interactive desktop session" | No one is logged into the machine — enable auto-logon or RDP in |
| Worker fails to start | Check that `winjavadriver.exe` exists at the expected path |
| Tests hang | Check firewall rules — port 9515 must be open |
| Agent service won't start | Run `winjavadriver-agent.exe --verbose` manually to see errors |
