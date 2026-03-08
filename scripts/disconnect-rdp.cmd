@echo off
REM ============================================================
REM disconnect-rdp.cmd
REM
REM Disconnects the current RDP session WITHOUT locking the
REM screen. The desktop stays active and unlocked, allowing
REM UI Automation to continue working.
REM
REM MUST be run as Administrator.
REM
REM This is the standard industry workaround used by WinAppDriver,
REM Ranorex, TestComplete, and pywinauto for unattended desktop
REM automation on remote machines.
REM ============================================================

echo Disconnecting RDP session without locking screen...
for /f "skip=1 tokens=3" %%s in ('query user %USERNAME%') do (
    echo Transferring session %%s to console...
    %windir%\System32\tscon.exe %%s /dest:console
)
echo Done. Desktop remains active and unlocked.
