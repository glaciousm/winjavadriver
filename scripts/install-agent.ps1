#Requires -RunAsAdministrator
<#
.SYNOPSIS
    Installs the WinJavaDriver Agent as a Windows Service.

.DESCRIPTION
    This script installs winjavadriver-agent.exe as a Windows Service running as LocalSystem.
    It configures the firewall rule and validates prerequisites.

.PARAMETER AgentPath
    Path to winjavadriver-agent.exe. Default: same directory as this script.

.PARAMETER WorkerPath
    Path to winjavadriver.exe. Default: same directory as AgentPath.

.PARAMETER Port
    Listening port for the Agent. Default: 9515.

.PARAMETER ServiceName
    Windows Service name. Default: WinJavaDriverAgent.

.EXAMPLE
    .\install-agent.ps1
    .\install-agent.ps1 -Port 4444
    .\install-agent.ps1 -AgentPath "C:\tools\winjavadriver-agent.exe" -Port 9515
#>

param(
    [string]$AgentPath,
    [string]$WorkerPath,
    [int]$Port = 9515,
    [string]$ServiceName = "WinJavaDriverAgent"
)

$ErrorActionPreference = "Stop"

# Resolve agent path
if (-not $AgentPath) {
    $AgentPath = Join-Path $PSScriptRoot "winjavadriver-agent.exe"
}

if (-not (Test-Path $AgentPath)) {
    Write-Error "Agent executable not found at: $AgentPath"
    exit 1
}

$AgentPath = (Resolve-Path $AgentPath).Path
Write-Host "Agent executable: $AgentPath" -ForegroundColor Cyan

# Resolve worker path
if (-not $WorkerPath) {
    $WorkerPath = Join-Path (Split-Path $AgentPath) "winjavadriver.exe"
}

if (-not (Test-Path $WorkerPath)) {
    Write-Warning "Worker executable not found at: $WorkerPath"
    Write-Warning "The Agent will fail to create sessions until winjavadriver.exe is available."
} else {
    $WorkerPath = (Resolve-Path $WorkerPath).Path
    Write-Host "Worker executable: $WorkerPath" -ForegroundColor Cyan
}

# Check if service already exists
$existingService = Get-Service -Name $ServiceName -ErrorAction SilentlyContinue
if ($existingService) {
    Write-Host "Service '$ServiceName' already exists. Stopping and removing..." -ForegroundColor Yellow
    Stop-Service -Name $ServiceName -Force -ErrorAction SilentlyContinue
    sc.exe delete $ServiceName | Out-Null
    Start-Sleep -Seconds 2
}

# Build service command line
$binPath = "`"$AgentPath`" --port $Port --worker-exe `"$WorkerPath`" --verbose --log-file `"$(Split-Path $AgentPath)\agent.log`""

Write-Host "Installing service..." -ForegroundColor Cyan
Write-Host "  Name: $ServiceName"
Write-Host "  Port: $Port"
Write-Host "  Command: $binPath"

# Create the service
sc.exe create $ServiceName `
    binPath= $binPath `
    start= auto `
    DisplayName= "WinJavaDriver Agent" | Out-Null

if ($LASTEXITCODE -ne 0) {
    Write-Error "Failed to create service (exit code: $LASTEXITCODE)"
    exit 1
}

# Set description
sc.exe description $ServiceName "WinJavaDriver Agent - Manages remote UI automation sessions with Windows credential authentication." | Out-Null

# Configure failure recovery (restart on failure)
sc.exe failure $ServiceName reset= 86400 actions= restart/5000/restart/10000/restart/30000 | Out-Null

Write-Host "Service created successfully." -ForegroundColor Green

# Configure firewall rule
$ruleName = "WinJavaDriver Agent (Port $Port)"
$existingRule = Get-NetFirewallRule -DisplayName $ruleName -ErrorAction SilentlyContinue

if ($existingRule) {
    Write-Host "Firewall rule already exists: $ruleName" -ForegroundColor Yellow
} else {
    Write-Host "Creating firewall rule: $ruleName" -ForegroundColor Cyan
    New-NetFirewallRule `
        -DisplayName $ruleName `
        -Direction Inbound `
        -Protocol TCP `
        -LocalPort $Port `
        -Action Allow `
        -Profile Domain,Private | Out-Null
    Write-Host "Firewall rule created." -ForegroundColor Green
}

# Start the service
Write-Host "Starting service..." -ForegroundColor Cyan
Start-Service -Name $ServiceName

$svc = Get-Service -Name $ServiceName
if ($svc.Status -eq "Running") {
    Write-Host ""
    Write-Host "WinJavaDriver Agent is running!" -ForegroundColor Green
    Write-Host "  URL: http://$(hostname):$Port" -ForegroundColor Cyan
    Write-Host "  Status: http://$(hostname):$Port/status" -ForegroundColor Cyan
    Write-Host "  Log: $(Split-Path $AgentPath)\agent.log" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "To connect from Java:" -ForegroundColor Yellow
    Write-Host '  WinJavaOptions options = new WinJavaOptions()'
    Write-Host '      .setApp("calc.exe")'
    Write-Host '      .setRemoteUser("username")'
    Write-Host '      .setRemotePassword("password")'
    Write-Host '      .setRemoteDomain("DOMAIN");'
    Write-Host "  WinJavaDriver driver = new WinJavaDriver(new URL(`"http://$(hostname):$Port`"), options);"
} else {
    Write-Warning "Service status: $($svc.Status). Check the log file for errors."
}
