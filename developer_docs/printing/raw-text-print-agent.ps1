<#
  raw-text-print-agent.ps1
  Watches a folder for inward-*.prn files and raw-copies each to a dot-matrix
  printer (bypassing the Windows print driver / rasteriser), then deletes it.

  Setup on the cashier PC:
    1. Share the Epson LQ-310, e.g. share name "LQ310", OR note its LPT port.
    2. Edit the three settings below.
    3. Set Chrome: Settings > Downloads > Location = the watch folder, and turn
       OFF "Ask where to save each file".
    4. Task Scheduler > Create Task > Trigger "At log on" > Action:
         powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden `
           -File "C:\hmis-print\raw-text-print-agent.ps1"
#>

# ---- settings -------------------------------------------------------------
$WatchFolder = "C:\hmis-print"          # where the browser saves .prn files
$PrinterPath = "\\localhost\LQ310"      # printer share  (or "\\.\LPT1")
$FileGlob    = "inward-*.prn"           # only touch our receipts
$PollSeconds = 2                        # how often to check the folder
# ------------------------------------------------------------------------------

if (-not (Test-Path $WatchFolder)) { New-Item -ItemType Directory -Path $WatchFolder | Out-Null }

function Send-Raw($file) {
    for ($i = 0; $i -lt 5; $i++) {
        try {
            # /b = binary copy, no EOF translation, no driver involvement
            cmd /c copy /b "`"$file`"" "$PrinterPath" | Out-Null
            if ($LASTEXITCODE -ne 0) {
                throw "copy /b exited with code $LASTEXITCODE"
            }
            Remove-Item -LiteralPath $file -Force
            Write-Host ("{0}  printed {1}" -f (Get-Date), (Split-Path $file -Leaf))
            return
        } catch {
            Start-Sleep -Milliseconds 400   # file may still be locked by the browser
        }
    }
    Write-Warning ("Could not print {0} after retries" -f $file)
}

Write-Host ("{0}  watching {1} for {2} -> {3} (polling every {4}s)" -f (Get-Date), $WatchFolder, $FileGlob, $PrinterPath, $PollSeconds)

# Simple poll loop instead of FileSystemWatcher: Register-ObjectEvent's -Action
# scriptblock runs in a separate event-job runspace that does not inherit this
# script's functions or variables, so Send-Raw/$PrinterPath would be undefined
# there. A poll loop is simpler and reliable for a low-volume receipt printer.
while ($true) {
    Get-ChildItem -Path $WatchFolder -Filter $FileGlob -File -ErrorAction SilentlyContinue |
        ForEach-Object { Send-Raw $_.FullName }
    Start-Sleep -Seconds $PollSeconds
}
