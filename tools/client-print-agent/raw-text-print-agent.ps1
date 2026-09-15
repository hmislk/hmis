<#
  raw-text-print-agent.ps1
  Watches a folder for .prn files and raw-copies each to a dot-matrix printer
  (bypassing the Windows print driver / rasteriser), then deletes it.
  Settings come from print-agent-config.json (same folder as this script) so
  the printer path/target folder can be changed without editing this file.

  Runs with no admin rights required: launched from the per-user Startup
  folder (start-agent-hidden.vbs), not Task Scheduler.
#>

$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$ConfigPath = Join-Path $ScriptDir "print-agent-config.json"
$LogPath    = Join-Path $ScriptDir "agent.log"

# Sends raw bytes via the Print Spooler API (winspool.drv), using whatever
# printer connection is already installed in Windows (Settings > Printers &
# scanners). Needed because this account has no direct SMB file-share
# permission on the print server, but printing through the installed
# connection object works via the spooler's own session.
. (Join-Path $ScriptDir "RawPrinterHelper.ps1")

# ---- defaults, overridden by config file if present ------------------------
$WatchFolder  = "C:\hims-print"
$PrinterPath  = "DEPOSIT"
$FileGlob     = "inward-*.prn"
$PollSeconds  = 2
$FailedFolder = $null   # resolved below once $WatchFolder is final
# ------------------------------------------------------------------------------

if (Test-Path $ConfigPath) {
    try {
        $cfg = Get-Content -LiteralPath $ConfigPath -Raw | ConvertFrom-Json
        if ($cfg.WatchFolder)  { $WatchFolder  = $cfg.WatchFolder }
        if ($cfg.PrinterPath)  { $PrinterPath  = $cfg.PrinterPath }
        if ($cfg.FileGlob)     { $FileGlob     = $cfg.FileGlob }
        if ($cfg.PollSeconds)  { $PollSeconds  = [int]$cfg.PollSeconds }
        if ($cfg.FailedFolder) { $FailedFolder = $cfg.FailedFolder }
    } catch {
        Write-Warning ("Could not parse {0}, using built-in defaults: {1}" -f $ConfigPath, $_.Exception.Message)
    }
}

if (-not $FailedFolder) { $FailedFolder = Join-Path $WatchFolder "failed" }
if (-not (Test-Path $WatchFolder)) { New-Item -ItemType Directory -Path $WatchFolder | Out-Null }

function Write-Log($msg) {
    $line = "{0}  {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $msg
    Write-Host $line
    Add-Content -LiteralPath $LogPath -Value $line
}

# ---- single-instance guard --------------------------------------------------
# If the agent gets started twice (double-clicked by mistake, started both
# from Startup and manually, etc.) two pollers racing the same folder would
# each pick up and print the same files -> duplicated print-outs. A named
# mutex makes every extra instance exit immediately instead.
$mutexName = "Local\HimsPrintAgent_" + ($WatchFolder -replace '[^A-Za-z0-9]', '_')
$mutex = New-Object System.Threading.Mutex($false, $mutexName)
if (-not $mutex.WaitOne(0)) {
    Write-Log "another instance of the print agent is already running in this folder - exiting"
    exit
}

# ---- claim a file before touching it ----------------------------------------
# Renaming (not copying) is atomic at the filesystem level: if the file is
# still being written by the browser the rename fails and we just retry next
# poll; if it succeeds, this is now the only copy and only this run of the
# loop will ever act on it, even if another instance briefly slipped past the
# mutex. The claimed name no longer matches $FileGlob, so it can never be
# picked up again by the watch loop - that is what stops a file from being
# reprinted over and over if something goes wrong after printing.
function Claim-File($path) {
    $claimed = $path + ".processing"
    try {
        Move-Item -LiteralPath $path -Destination $claimed -Force -ErrorAction Stop
        return $claimed
    } catch {
        return $null
    }
}

function Send-Raw($claimedFile, $originalName) {
    $sent = $false
    $lastErr = ""

    # Retry ONLY the send-to-printer step. Deleting the file afterwards is a
    # separate, independent step below - a delete failure must never cause
    # the job to be sent to the printer again.
    for ($i = 0; $i -lt 3; $i++) {
        try {
            $bytes = [System.IO.File]::ReadAllBytes($claimedFile)
            $err = ""
            $ok = [RawPrinterHelper]::SendBytesToPrinter($PrinterPath, $bytes, [ref]$err)
            if ($ok) { $sent = $true; break }
            $lastErr = $err
        } catch {
            $lastErr = $_.Exception.Message
        }
        Start-Sleep -Milliseconds 500
    }

    if ($sent) {
        $deleted = $false
        for ($i = 0; $i -lt 3; $i++) {
            try {
                Remove-Item -LiteralPath $claimedFile -Force -ErrorAction Stop
                $deleted = $true
                break
            } catch {
                Start-Sleep -Milliseconds 300
            }
        }
        if ($deleted) {
            Write-Log ("printed and removed {0}" -f $originalName)
        } else {
            # Printed fine; only the cleanup of the temp copy failed (e.g.
            # file locked). It will never be reprinted since its name no
            # longer matches the watch pattern - just needs manual deletion.
            Write-Log ("printed {0} but could not delete temp file, remove manually: {1}" -f $originalName, $claimedFile)
        }
    } else {
        # Printer unavailable (off/disconnected/wrong name/etc). Move the job
        # out of the watch folder into a "failed" subfolder so it is NOT
        # retried every poll forever, and does not pile up invisibly in the
        # watched folder waiting to all fire off at once. It stays there for
        # manual inspection / re-drop once the printer issue is fixed.
        try {
            if (-not (Test-Path $FailedFolder)) { New-Item -ItemType Directory -Path $FailedFolder | Out-Null }
            $dest = Join-Path $FailedFolder $originalName
            if (Test-Path $dest) {
                $dest = Join-Path $FailedFolder ("{0}_{1}" -f (Get-Date -Format "yyyyMMdd_HHmmss"), $originalName)
            }
            Move-Item -LiteralPath $claimedFile -Destination $dest -Force
        } catch {
            Write-Log ("ERROR: could not move failed job {0} to failed folder: {1}" -f $originalName, $_.Exception.Message)
        }
        Write-Log ("WARNING: printer '{0}' unavailable, moved {1} to failed folder for manual retry ({2})" -f $PrinterPath, $originalName, $lastErr)
    }
}

Write-Log ("watching {0} for {1} -> {2} (polling every {3}s)" -f $WatchFolder, $FileGlob, $PrinterPath, $PollSeconds)

# Simple poll loop instead of FileSystemWatcher: Register-ObjectEvent's -Action
# scriptblock runs in a separate event-job runspace that does not inherit this
# script's functions or variables, so Send-Raw/$PrinterPath would be undefined
# there. A poll loop is simpler and reliable for a low-volume receipt printer.
while ($true) {
    Get-ChildItem -Path $WatchFolder -Filter $FileGlob -File -ErrorAction SilentlyContinue |
        ForEach-Object {
            $originalName = $_.Name
            $claimed = Claim-File $_.FullName
            if ($claimed) { Send-Raw $claimed $originalName }
        }
    Start-Sleep -Seconds $PollSeconds
}
