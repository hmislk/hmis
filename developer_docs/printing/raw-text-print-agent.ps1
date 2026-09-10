<#
  raw-text-print-agent.ps1
  Watches a folder for .prn files and raw-copies each to a dot-matrix printer
  (bypassing the Windows print driver / rasteriser), then deletes it.
  Settings come from print-agent-config.json (same folder as this script) so
  the printer path/target folder can be changed without editing this file.
  See print-agent-config.example.json and the wiki page
  "Dot-Matrix-Printing-for-Inward-Deposit-and-Payment-Receipts" for setup.

  Runs with no admin rights required: launch it from the per-user Startup
  folder (a small .vbs that runs this hidden), not Task Scheduler, if
  Task Scheduler requires elevation you don't have.
#>

$ScriptDir  = Split-Path -Parent $MyInvocation.MyCommand.Path
$ConfigPath = Join-Path $ScriptDir "print-agent-config.json"
$LogPath    = Join-Path $ScriptDir "agent.log"

# Sends raw bytes via the Print Spooler API (winspool.drv), using whatever
# printer connection is already installed in Windows (Settings > Printers &
# scanners). Use this instead of raw SMB "copy /b file \host\share" when the
# client account has file-share permission denied on the print server but can
# still print through the locally installed printer connection object.
. (Join-Path $ScriptDir "RawPrinterHelper.ps1")

# ---- defaults, overridden by config file if present ------------------------
$WatchFolder = "C:\hmis-print"        # where the browser saves .prn files
$PrinterPath = "\\localhost\LQ310"    # installed printer name, or "\\.\LPT1"
$FileGlob    = "inward-*.prn"         # only touch our receipts
$PollSeconds = 2                      # how often to check the folder
# ------------------------------------------------------------------------------

if (Test-Path $ConfigPath) {
    try {
        $cfg = Get-Content -LiteralPath $ConfigPath -Raw | ConvertFrom-Json

        # Validate into temp variables first so a bad value can't leave the
        # config partially applied (some fields overridden, others still
        # defaults) while the warning below claims "using built-in defaults".
        $newWatchFolder = $WatchFolder
        $newPrinterPath = $PrinterPath
        $newFileGlob    = $FileGlob
        $newPollSeconds = $PollSeconds
        if ($cfg.WatchFolder) { $newWatchFolder = $cfg.WatchFolder }
        if ($cfg.PrinterPath) { $newPrinterPath = $cfg.PrinterPath }
        if ($cfg.FileGlob)    { $newFileGlob    = $cfg.FileGlob }
        if ($cfg.PollSeconds) {
            $newPollSeconds = [int]$cfg.PollSeconds
            if ($newPollSeconds -lt 1) {
                throw "PollSeconds must be at least 1, got $($cfg.PollSeconds)"
            }
        }

        $WatchFolder = $newWatchFolder
        $PrinterPath = $newPrinterPath
        $FileGlob    = $newFileGlob
        $PollSeconds = $newPollSeconds
    } catch {
        Write-Warning ("Could not parse {0}, using built-in defaults: {1}" -f $ConfigPath, $_.Exception.Message)
    }
}

if (-not (Test-Path $WatchFolder)) { New-Item -ItemType Directory -Path $WatchFolder | Out-Null }

function Write-Log($msg) {
    $line = "{0}  {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $msg
    Write-Host $line
    Add-Content -LiteralPath $LogPath -Value $line
}

function Send-Raw($file) {
    $printed = $false
    for ($i = 0; $i -lt 5; $i++) {
        try {
            $bytes = [System.IO.File]::ReadAllBytes($file)
            $err = ""
            $ok = [RawPrinterHelper]::SendBytesToPrinter($PrinterPath, $bytes, [ref]$err)
            if (-not $ok) {
                throw "spooler write failed: $err"
            }
            $printed = $true
            break
        } catch {
            Start-Sleep -Milliseconds 400
        }
    }
    if (-not $printed) {
        Write-Log ("WARNING: could not print {0} after retries, left in place for manual retry" -f (Split-Path $file -Leaf))
        return
    }

    # Printing already succeeded above — from here on, never let a cleanup
    # failure fall back into a retry (that would resubmit an already-printed
    # receipt). Remove-Item's non-terminating errors are made catchable with
    # -ErrorAction Stop so a locked/permission-denied delete is handled here
    # instead of silently leaving the file for the next poll to reprint.
    try {
        Remove-Item -LiteralPath $file -Force -ErrorAction Stop
        Write-Log ("printed and removed {0}" -f (Split-Path $file -Leaf))
    } catch {
        $renamedLeaf = (Split-Path $file -Leaf) + ".printed"
        try {
            Rename-Item -LiteralPath $file -NewName $renamedLeaf -Force -ErrorAction Stop
            Write-Log ("WARNING: printed {0} but could not delete it ({1}); renamed to {2} for manual cleanup" -f (Split-Path $file -Leaf), $_.Exception.Message, $renamedLeaf)
        } catch {
            Write-Log ("WARNING: printed {0} but could neither delete nor rename it ({1}); it WILL be resubmitted next poll" -f (Split-Path $file -Leaf), $_.Exception.Message)
        }
    }
}

Write-Log ("watching {0} for {1} -> {2} (polling every {3}s)" -f $WatchFolder, $FileGlob, $PrinterPath, $PollSeconds)

while ($true) {
    Get-ChildItem -Path $WatchFolder -Filter $FileGlob -File -ErrorAction SilentlyContinue |
        ForEach-Object { Send-Raw $_.FullName }
    Start-Sleep -Seconds $PollSeconds
}
