# Fail-safe scheduled PR merge for Windows Task Scheduler (no cron/flock on this machine).
# Same rules as .claude/skills/scheduled-pr-merge/scheduled-merge.sh:
#   merge only inside [At, At+Window), only the pinned head, only MERGEABLE/CLEAN with no failed check;
#   give up (comment + delete task) on changes that retrying cannot fix; otherwise retry next minute.
param(
    [string]$Repo, [int]$Pr, [string]$Base, [string]$Sha,
    [datetime]$At, [int]$WindowMinutes = 30, [string]$TaskName,
    [ValidateSet('merge', 'squash', 'rebase')][string]$Method = 'merge',
    [switch]$DryRun
)
$ErrorActionPreference = 'Continue'
$gh = (Get-Command gh).Source
$stateDir = Join-Path $env:USERPROFILE '.local\state\hmis-scheduled-merge'
New-Item -ItemType Directory -Force $stateDir | Out-Null
$log = Join-Path $stateDir "pr-$Pr.log"
$attempted = Join-Path $stateDir "pr-$Pr.merge-requested"
function Log($m) { "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $m" | Out-File -Append -Encoding utf8 $log }
function Finish($comment) {
    if ($comment -and -not $DryRun) { & $gh pr comment $Pr --repo $Repo --body $comment | Out-Null }
    if ($TaskName -and -not $DryRun) { schtasks /Delete /TN $TaskName /F | Out-Null }
    exit 0
}

# One attempt at a time
$lock = Join-Path $stateDir "pr-$Pr.lock"
try { $fs = [System.IO.File]::Open($lock, 'OpenOrCreate', 'ReadWrite', 'None') } catch { Log 'previous attempt still running; skipping'; exit 0 }

$now = Get-Date
$end = $At.AddMinutes($WindowMinutes)
if (-not $DryRun) {
    if ($now -lt $At) { Log 'before window; nothing to do'; exit 0 }
    if ($now -ge $end -and -not (Test-Path $attempted)) {
        Log 'window over without merging; giving up'
        Finish "⏰ Scheduled merge window ($($At.ToString('yyyy-MM-dd HH:mm'))–$($end.ToString('HH:mm')) Asia/Colombo) ended without merging. Last state is in the local log. Not merged; needs a new schedule."
    }
}

$json = & $gh pr view $Pr --repo $Repo --json state,baseRefName,headRefOid,mergeable,mergeStateStatus,statusCheckRollup 2>&1
if ($LASTEXITCODE -ne 0) { Log "gh pr view failed (retry next minute): $json"; exit 0 }
$p = $json | ConvertFrom-Json

if ($p.state -eq 'MERGED') {
    if (Test-Path $attempted) {
        Remove-Item $attempted -Force
        Log 'MERGED (completed after the merge request)'
        Finish "⏰ Merged as scheduled at $(Get-Date -Format 'yyyy-MM-dd HH:mm') Asia/Colombo (reviewed head ``$($Sha.Substring(0,10))``)."
    }
    Log 'already merged'
    Finish $null
}
if ($p.state -ne 'OPEN') { Log "PR is $($p.state); giving up"; Finish "⏰ Scheduled merge cancelled: the PR is $($p.state)." }
if ($p.baseRefName -ne $Base) { Log "base changed to $($p.baseRefName); giving up"; Finish "⏰ Scheduled merge cancelled: the base branch changed from ``$Base`` to ``$($p.baseRefName)``." }
if ($p.headRefOid -ne $Sha) { Log "head changed to $($p.headRefOid); giving up"; Finish "⏰ Scheduled merge cancelled: new commits were pushed after review (reviewed ``$($Sha.Substring(0,10))``, now ``$($p.headRefOid.Substring(0,10))``). Re-review and reschedule." }

if (Test-Path $attempted) {
    if ((Get-Date) -ge $end) {
        Remove-Item $attempted -Force
        Log 'merge requested but not MERGED when the window ended; giving up'
        Finish "⏰ Merge was requested, but GitHub did not show the PR as merged before the window ended ($($At.ToString('yyyy-MM-dd HH:mm'))–$($end.ToString('HH:mm')) Asia/Colombo). Check the PR."
    }
    Log 'merge requested, waiting for MERGED'
    exit 0
}

# Only the latest run of each check counts (an older cancelled run can sit
# beside a newer successful one).
$checks = @($p.statusCheckRollup | ForEach-Object {
    [pscustomobject]@{
        Name   = if ($_.name) { $_.name } else { $_.context }
        Result = "$(($_.conclusion, $_.state | Where-Object { $_ }) | Select-Object -First 1)"
        Time   = "$(($_.completedAt, $_.startedAt | Where-Object { $_ }) | Select-Object -First 1)"
    }
} | Group-Object Name | ForEach-Object { $_.Group | Sort-Object Time | Select-Object -Last 1 })
$waiting = @('', 'PENDING', 'QUEUED', 'IN_PROGRESS', 'EXPECTED', 'WAITING', 'REQUESTED')
$passed = @('SUCCESS', 'NEUTRAL', 'SKIPPED')
$failed = @($checks | Where-Object { $waiting -notcontains $_.Result -and $passed -notcontains $_.Result })
if ($failed.Count -gt 0) { $n = ($failed | ForEach-Object { $_.Name }) -join ', '; Log "failed checks: $n; giving up"; Finish "⏰ Scheduled merge cancelled: check(s) failed: $n." }
$pending = @($checks | Where-Object { $waiting -contains $_.Result })
if ($p.mergeable -eq 'CONFLICTING') { Log 'conflicts; giving up'; Finish '⏰ Scheduled merge cancelled: the PR has merge conflicts.' }
if ($pending.Count -gt 0 -or $p.mergeable -ne 'MERGEABLE' -or $p.mergeStateStatus -ne 'CLEAN') {
    Log "not ready (mergeable=$($p.mergeable) state=$($p.mergeStateStatus) pending=$($pending.Count)); retry next minute"; exit 0
}

if ($DryRun) { Log 'DRY RUN: would merge now'; Write-Output 'RESULT: would merge now'; exit 0 }
if ((Get-Date) -ge $end) { Log 'window ended just before merge; retry logic will give up'; exit 0 }

$out = & $gh pr merge $Pr --repo $Repo "--$Method" --match-head-commit $Sha 2>&1
Log "merge call: $out"
if ($LASTEXITCODE -eq 0) { New-Item -ItemType File -Force $attempted | Out-Null }
Start-Sleep -Seconds 5
$st = (& $gh pr view $Pr --repo $Repo --json state | ConvertFrom-Json).state
if ($st -eq 'MERGED') {
    Remove-Item $attempted -Force -ErrorAction SilentlyContinue
    Log 'MERGED'
    Finish "⏰ Merged as scheduled at $(Get-Date -Format 'yyyy-MM-dd HH:mm') Asia/Colombo (reviewed head ``$($Sha.Substring(0,10))``)."
}
Log "not merged yet (state=$st); retry next minute"
exit 0
