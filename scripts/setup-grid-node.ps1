#Requires -RunAsAdministrator
<#
.SYNOPSIS
    Prepares a Windows machine as a WinJavaDriver Grid Node.

.DESCRIPTION
    This script configures a Windows machine for unattended desktop automation:
    - Disables screensaver and lock screen
    - Sets screen resolution (optional)
    - Creates scheduled tasks for auto-start on logon
    - Copies disconnect-rdp.cmd for RDP session management

    After running this script:
    1. Log in via RDP
    2. Run disconnect-rdp.cmd as admin (keeps session alive)
    3. The machine is ready to execute desktop UI tests via Selenium Grid

.PARAMETER WinJavaDriverPath
    Path to winjavadriver.exe. Default: looks in current directory.

.PARAMETER SeleniumServerPath
    Path to selenium-server.jar. Default: looks in current directory.

.PARAMETER GridUrl
    URL of the Selenium Grid Hub. Default: http://localhost:4444

.PARAMETER Port
    Port for the Selenium Grid Node. Default: 5555

.PARAMETER WinJavaDriverPort
    Port for winjavadriver.exe. Default: 9515

.PARAMETER SkipScheduledTasks
    Skip creating scheduled tasks (for manual management).

.PARAMETER ConfigureAutoLogon
    Configure auto-logon (prompts for credentials). USE WITH CAUTION.

.EXAMPLE
    .\setup-grid-node.ps1 -GridUrl "http://192.168.1.12:4444"
    .\setup-grid-node.ps1 -GridUrl "http://hub:4444" -ConfigureAutoLogon
#>

param(
    [string]$WinJavaDriverPath,
    [string]$SeleniumServerPath,
    [string]$GridUrl = "http://localhost:4444",
    [int]$Port = 5555,
    [int]$WinJavaDriverPort = 9515,
    [switch]$SkipScheduledTasks,
    [switch]$ConfigureAutoLogon
)

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  WinJavaDriver Grid Node Setup" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# --- Resolve paths ---

if (-not $WinJavaDriverPath) {
    $WinJavaDriverPath = Join-Path $PSScriptRoot "winjavadriver.exe"
    if (-not (Test-Path $WinJavaDriverPath)) {
        $WinJavaDriverPath = Join-Path (Split-Path $PSScriptRoot) "server\WinJavaDriver\bin\Release\net8.0-windows\win-x64\publish\winjavadriver.exe"
    }
}

if (-not (Test-Path $WinJavaDriverPath)) {
    Write-Error "winjavadriver.exe not found at: $WinJavaDriverPath"
    exit 1
}
$WinJavaDriverPath = (Resolve-Path $WinJavaDriverPath).Path
Write-Host "WinJavaDriver: $WinJavaDriverPath" -ForegroundColor Green

if (-not $SeleniumServerPath) {
    $SeleniumServerPath = Join-Path $PSScriptRoot "selenium-server.jar"
    if (-not (Test-Path $SeleniumServerPath)) {
        $SeleniumServerPath = Join-Path (Split-Path $PSScriptRoot) "tools\selenium-server.jar"
    }
}

if (-not (Test-Path $SeleniumServerPath)) {
    Write-Warning "selenium-server.jar not found at: $SeleniumServerPath"
    Write-Warning "Download from: https://github.com/SeleniumHQ/selenium/releases"
    Write-Warning "Skipping Selenium Node scheduled task."
    $SeleniumServerPath = $null
}
else {
    $SeleniumServerPath = (Resolve-Path $SeleniumServerPath).Path
    Write-Host "Selenium Server: $SeleniumServerPath" -ForegroundColor Green
}

# Check Java
try {
    $javaVersion = & java -version 2>&1 | Select-Object -First 1
    Write-Host "Java: $javaVersion" -ForegroundColor Green
}
catch {
    Write-Error "Java not found. JRE 11+ is required for Selenium Grid Node."
    exit 1
}

Write-Host ""

# --- Disable screensaver ---

Write-Host "Disabling screensaver..." -ForegroundColor Cyan
Set-ItemProperty -Path "HKCU:\Control Panel\Desktop" -Name "ScreenSaveActive" -Value "0" -ErrorAction SilentlyContinue
Set-ItemProperty -Path "HKCU:\Control Panel\Desktop" -Name "ScreenSaverIsSecure" -Value "0" -ErrorAction SilentlyContinue
Write-Host "  Screensaver disabled." -ForegroundColor Green

# --- Disable lock screen ---

Write-Host "Disabling lock screen..." -ForegroundColor Cyan
$regPath = "HKLM:\SOFTWARE\Policies\Microsoft\Windows\Personalization"
if (-not (Test-Path $regPath)) {
    New-Item -Path $regPath -Force | Out-Null
}
Set-ItemProperty -Path $regPath -Name "NoLockScreen" -Value 1 -Type DWord
Write-Host "  Lock screen disabled." -ForegroundColor Green

# --- Disable machine inactivity timeout ---

Write-Host "Disabling inactivity timeout..." -ForegroundColor Cyan
$regPath2 = "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\System"
Set-ItemProperty -Path $regPath2 -Name "InactivityTimeoutSecs" -Value 0 -Type DWord -ErrorAction SilentlyContinue
Write-Host "  Inactivity timeout disabled." -ForegroundColor Green

# --- Power settings (prevent sleep) ---

Write-Host "Configuring power settings (prevent sleep)..." -ForegroundColor Cyan
powercfg /change standby-timeout-ac 0
powercfg /change monitor-timeout-ac 0
Write-Host "  Sleep and monitor timeout disabled." -ForegroundColor Green

Write-Host ""

# --- Create TOML config ---

$tomlDir = Split-Path $WinJavaDriverPath
$tomlPath = Join-Path $tomlDir "winjavadriver-node.toml"

Write-Host "Creating Grid Node config: $tomlPath" -ForegroundColor Cyan

$tomlContent = @"
[server]
port = $Port

[node]
detect-drivers = false
grid-url = "$GridUrl"

[relay]
url = "http://localhost:$WinJavaDriverPort"
status-endpoint = "/status"
configs = [
  "1", "{\"platformName\": \"windows\", \"browserName\": \"winjavadriver\"}"
]
"@

Set-Content -Path $tomlPath -Value $tomlContent -Encoding UTF8
Write-Host "  Config created." -ForegroundColor Green

# --- Copy disconnect-rdp.cmd ---

$rdpScript = Join-Path $PSScriptRoot "disconnect-rdp.cmd"
if (Test-Path $rdpScript) {
    $destRdpScript = Join-Path $tomlDir "disconnect-rdp.cmd"
    Copy-Item $rdpScript $destRdpScript -Force
    Write-Host "  disconnect-rdp.cmd copied to: $destRdpScript" -ForegroundColor Green
}

Write-Host ""

# --- Scheduled tasks ---

if (-not $SkipScheduledTasks) {
    Write-Host "Creating scheduled tasks..." -ForegroundColor Cyan

    # Task 1: Start winjavadriver.exe on logon
    $taskName1 = "WinJavaDriver Server"
    $existingTask = Get-ScheduledTask -TaskName $taskName1 -ErrorAction SilentlyContinue
    if ($existingTask) {
        Unregister-ScheduledTask -TaskName $taskName1 -Confirm:$false
    }

    $action1 = New-ScheduledTaskAction -Execute $WinJavaDriverPath -Argument "--port $WinJavaDriverPort --verbose"
    $trigger1 = New-ScheduledTaskTrigger -AtLogOn
    $settings1 = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
    Register-ScheduledTask -TaskName $taskName1 -Action $action1 -Trigger $trigger1 -Settings $settings1 -Description "Starts WinJavaDriver W3C WebDriver server on logon" | Out-Null
    Write-Host "  Task '$taskName1' created (starts on logon)." -ForegroundColor Green

    # Task 2: Start Selenium Grid Node on logon (with 10s delay for winjavadriver to start)
    if ($SeleniumServerPath) {
        $taskName2 = "WinJavaDriver Grid Node"
        $existingTask = Get-ScheduledTask -TaskName $taskName2 -ErrorAction SilentlyContinue
        if ($existingTask) {
            Unregister-ScheduledTask -TaskName $taskName2 -Confirm:$false
        }

        $nodeArgs = "-jar `"$SeleniumServerPath`" --node --config `"$tomlPath`""
        $action2 = New-ScheduledTaskAction -Execute "java" -Argument $nodeArgs
        $trigger2 = New-ScheduledTaskTrigger -AtLogOn
        $trigger2.Delay = "PT10S"  # 10 second delay to let winjavadriver start first
        $settings2 = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -RestartCount 3 -RestartInterval (New-TimeSpan -Minutes 1)
        Register-ScheduledTask -TaskName $taskName2 -Action $action2 -Trigger $trigger2 -Settings $settings2 -Description "Starts Selenium Grid Node with WinJavaDriver relay on logon" | Out-Null
        Write-Host "  Task '$taskName2' created (starts on logon with 10s delay)." -ForegroundColor Green
    }
}

Write-Host ""

# --- Auto-logon (optional) ---

if ($ConfigureAutoLogon) {
    Write-Host "Configuring auto-logon..." -ForegroundColor Yellow
    Write-Host "  WARNING: This stores your password in the registry." -ForegroundColor Red

    $cred = Get-Credential -Message "Enter Windows credentials for auto-logon"
    $regPath3 = "HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon"
    Set-ItemProperty -Path $regPath3 -Name "AutoAdminLogon" -Value "1"
    Set-ItemProperty -Path $regPath3 -Name "DefaultUserName" -Value $cred.UserName
    Set-ItemProperty -Path $regPath3 -Name "DefaultPassword" -Value $cred.GetNetworkCredential().Password
    if ($cred.GetNetworkCredential().Domain) {
        Set-ItemProperty -Path $regPath3 -Name "DefaultDomainName" -Value $cred.GetNetworkCredential().Domain
    }
    Write-Host "  Auto-logon configured for: $($cred.UserName)" -ForegroundColor Green
}

# --- Summary ---

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Setup Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "  1. If connected via RDP, run disconnect-rdp.cmd as admin" -ForegroundColor White
Write-Host "     to keep the desktop session alive after disconnecting." -ForegroundColor White
Write-Host ""
Write-Host "  2. Start the services (or reboot to trigger scheduled tasks):" -ForegroundColor White
Write-Host "     winjavadriver.exe --port $WinJavaDriverPort" -ForegroundColor Cyan
if ($SeleniumServerPath) {
    Write-Host "     java -jar `"$SeleniumServerPath`" --node --config `"$tomlPath`"" -ForegroundColor Cyan
}
Write-Host ""
Write-Host "  3. Verify the node appears in Grid UI:" -ForegroundColor White
Write-Host "     $GridUrl/ui" -ForegroundColor Cyan
Write-Host ""
Write-Host "  4. Run tests pointing at the Hub:" -ForegroundColor White
Write-Host "     WinJavaDriver driver = new WinJavaDriver(" -ForegroundColor Cyan
Write-Host "         new URL(`"$GridUrl`"), options);" -ForegroundColor Cyan
Write-Host ""
