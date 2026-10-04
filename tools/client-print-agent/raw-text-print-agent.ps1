<#
  raw-text-print-agent.ps1
  Watches a folder for .prn files and raw-copies each to a dot-matrix printer,
  then deletes it. Settings come from print-agent-config.json (same folder as
  this script) so the printer name/target folder can be changed without
  editing this file.

  Runs with no admin rights required: launched from the per-user Startup
  folder (start-agent-hidden.vbs), not Task Scheduler.

  Printing method: raw bytes are sent by copying the file straight to the
  printer's local share (\\<computername>\<sharename>) rather than via
  Add-Type + P/Invoke. Add-Type -Language CSharp compiles code by spawning
  csc.exe, and on this machine (and possibly other client PCs with similar
  endpoint-security hardening) that spawn is blocked outright ("Access is
  denied"), which silently prevented every print. A plain file copy to the
  printer's UNC share needs no compilation and no child process, and Windows
  routes it straight to the spooler in RAW mode - this is the same mechanism
  as the classic "copy /b file.prn \\host\printershare" trick. This requires
  the printer to be shared locally (Settings > Printers & scanners > printer
  properties > Sharing) - $PrinterPath below must be its Windows printer
  name, and the script resolves the matching share name itself.
#>

$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$ConfigPath = Join-Path $ScriptDir "print-agent-config.json"
$LogPath    = Join-Path $ScriptDir "agent.log"

# ---- defaults, overridden by config file if present ------------------------
$WatchFolder  = "C:\hmis-print"
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

# ---- resolve the printer's UNC share target once at startup ----------------
function Resolve-PrinterShareTarget($printerName) {
    $p = Get-Printer -Name $printerName -ErrorAction SilentlyContinue
    if (-not $p) {
        throw "no printer named '$printerName' is installed on this PC (check Settings > Printers & scanners)"
    }
    if (-not $p.Shared -or [string]::IsNullOrWhiteSpace($p.ShareName)) {
        throw "printer '$printerName' is not shared - enable Printer Properties > Sharing so raw bytes can be sent via its UNC path"
    }
    return "\\$env:COMPUTERNAME\$($p.ShareName)"
}

try {
    $PrinterShareTarget = Resolve-PrinterShareTarget $PrinterPath
} catch {
    Write-Log ("FATAL: {0}" -f $_.Exception.Message)
    exit 1
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

# ---- recover jobs stranded by a previous run --------------------------------
# A leftover .processing file means an earlier run claimed a job and then died
# (reboot, logoff, crash) before finishing with it. Its name no longer matches
# $FileGlob, so the watch loop below would never see it again and the receipt
# would be lost silently. Move it to the failed folder so a human notices it.
# It is deliberately NOT reprinted: the dead run may already have handed the
# bytes to the spooler, and reprinting here would be exactly the duplicate
# print this agent exists to prevent.
Get-ChildItem -Path $WatchFolder -Filter "*.processing" -File -ErrorAction SilentlyContinue |
    ForEach-Object {
        # Capture these before the try: inside a catch block $_ is the error
        # record, not the pipeline item.
        $strandedPath = $_.FullName
        $strandedName = $_.Name
        try {
            if (-not (Test-Path $FailedFolder)) { New-Item -ItemType Directory -Path $FailedFolder -ErrorAction Stop | Out-Null }
            $dest = Join-Path $FailedFolder $strandedName
            if (Test-Path $dest) {
                $dest = Join-Path $FailedFolder ("{0}_{1}" -f (Get-Date -Format "yyyyMMdd_HHmmss"), $strandedName)
            }
            Move-Item -LiteralPath $strandedPath -Destination $dest -Force -ErrorAction Stop
            Write-Log ("recovered stranded job {0} -> failed folder (NOT reprinted - check whether it already printed before re-dropping it)" -f $strandedName)
        } catch {
            Write-Log ("ERROR: could not move stranded job {0} to failed folder: {1}" -f $strandedName, $_.Exception.Message)
        }
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
            [System.IO.File]::Copy($claimedFile, $PrinterShareTarget, $true)
            $sent = $true
            break
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
        # -ErrorAction Stop on both: these are non-terminating by default, so
        # without it a failed move would skip the catch and still be reported
        # below as "moved to failed folder" when nothing had moved at all.
        try {
            if (-not (Test-Path $FailedFolder)) { New-Item -ItemType Directory -Path $FailedFolder -ErrorAction Stop | Out-Null }
            $dest = Join-Path $FailedFolder $originalName
            if (Test-Path $dest) {
                $dest = Join-Path $FailedFolder ("{0}_{1}" -f (Get-Date -Format "yyyyMMdd_HHmmss"), $originalName)
            }
            Move-Item -LiteralPath $claimedFile -Destination $dest -Force -ErrorAction Stop
            Write-Log ("WARNING: printer '{0}' unavailable, moved {1} to failed folder for manual retry ({2})" -f $PrinterPath, $originalName, $lastErr)
        } catch {
            Write-Log ("ERROR: printer '{0}' unavailable ({1}) AND {2} could not be moved to the failed folder: {3} - it stays as {4} and will NOT be retried automatically" -f $PrinterPath, $lastErr, $originalName, $_.Exception.Message, $claimedFile)
        }
    }
}

Write-Log ("watching {0} for {1} -> {2} ({3}) (polling every {4}s)" -f $WatchFolder, $FileGlob, $PrinterPath, $PrinterShareTarget, $PollSeconds)

# Simple poll loop instead of FileSystemWatcher: Register-ObjectEvent's -Action
# scriptblock runs in a separate event-job runspace that does not inherit this
# script's functions or variables, so Send-Raw/$PrinterShareTarget would be
# undefined there. A poll loop is simpler and reliable for a low-volume
# receipt printer.
while ($true) {
    Get-ChildItem -Path $WatchFolder -Filter $FileGlob -File -ErrorAction SilentlyContinue |
        ForEach-Object {
            $originalName = $_.Name
            $claimed = Claim-File $_.FullName
            if ($claimed) { Send-Raw $claimed $originalName }
        }
    Start-Sleep -Seconds $PollSeconds
}
