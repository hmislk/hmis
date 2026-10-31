# Fail-safe scheduled PR merge for Windows Task Scheduler (no cron/flock on this machine).
# Same rules as .claude/skills/scheduled-pr-merge/scheduled-merge.sh:
#   merge only inside [At, At+Window), only the pinned head, only MERGEABLE/CLEAN with no failed check;
#   give up (comment + delete task) on changes that retrying cannot fix; otherwise retry next minute.
param(
    [string]$Repo, [int]$Pr, [string]$Base, [string]$Sha,
    [datetime]$At, [int]$WindowMinutes = 30, [string]$TaskName, [string]$Method = 'merge',
    [switch]$DryRun
)
$ErrorActionPreference = 'Continue'
$gh = (Get-Command gh).Source
$stateDir = Join-Path $env:USERPROFILE '.local\state\hmis-scheduled-merge'
New-Item -ItemType Directory -Force $stateDir | Out-Null
$log = Join-Path $stateDir "pr-$Pr.log"
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
    if ($now -ge $end) {
        Log 'window over without merging; giving up'
        Finish "⏰ Scheduled merge window ($($At.ToString('yyyy-MM-dd HH:mm'))–$($end.ToString('HH:mm')) Asia/Colombo) ended without merging. Last state is in the local log. Not merged; needs a new schedule."
    }
}

$json = & $gh pr view $Pr --repo $Repo --json state,baseRefName,headRefOid,mergeable,mergeStateStatus,statusCheckRollup 2>&1
if ($LASTEXITCODE -ne 0) { Log "gh pr view failed (retry next minute): $json"; exit 0 }
$p = $json | ConvertFrom-Json

if ($p.state -eq 'MERGED') { Log 'already merged'; Finish $null }
if ($p.state -ne 'OPEN') { Log "PR is $($p.state); giving up"; Finish "⏰ Scheduled merge cancelled: the PR is $($p.state)." }
if ($p.baseRefName -ne $Base) { Log "base changed to $($p.baseRefName); giving up"; Finish "⏰ Scheduled merge cancelled: the base branch changed from ``$Base`` to ``$($p.baseRefName)``." }
if ($p.headRefOid -ne $Sha) { Log "head changed to $($p.headRefOid); giving up"; Finish "⏰ Scheduled merge cancelled: new commits were pushed after review (reviewed ``$($Sha.Substring(0,10))``, now ``$($p.headRefOid.Substring(0,10))``). Re-review and reschedule." }

$failed = @($p.statusCheckRollup | Where-Object { @('FAILURE','ERROR','CANCELLED','TIMED_OUT','ACTION_REQUIRED') -contains (($_.conclusion, $_.state | Where-Object { $_ }) | Select-Object -First 1) })
if ($failed.Count -gt 0) { $n = ($failed | ForEach-Object { if ($_.name) { $_.name } else { $_.context } }) -join ', '; Log "failed checks: $n; giving up"; Finish "⏰ Scheduled merge cancelled: check(s) failed: $n." }
$pending = @($p.statusCheckRollup | Where-Object { $s = (($_.conclusion, $_.state | Where-Object { $_ }) | Select-Object -First 1); -not $s -or @('PENDING','IN_PROGRESS','QUEUED','EXPECTED') -contains $s })
if ($p.mergeable -eq 'CONFLICTING') { Log 'conflicts; giving up'; Finish '⏰ Scheduled merge cancelled: the PR has merge conflicts.' }
if ($pending.Count -gt 0 -or $p.mergeable -ne 'MERGEABLE' -or $p.mergeStateStatus -ne 'CLEAN') {
    Log "not ready (mergeable=$($p.mergeable) state=$($p.mergeStateStatus) pending=$($pending.Count)); retry next minute"; exit 0
}

if ($DryRun) { Log 'DRY RUN: would merge now'; Write-Output 'RESULT: would merge now'; exit 0 }
if ((Get-Date) -ge $end) { Log 'window ended just before merge; retry logic will give up'; exit 0 }

$out = & $gh pr merge $Pr --repo $Repo "--$Method" --match-head-commit $Sha 2>&1
Log "merge call: $out"
Start-Sleep -Seconds 5
$st = (& $gh pr view $Pr --repo $Repo --json state | ConvertFrom-Json).state
if ($st -eq 'MERGED') {
    Log 'MERGED'
    Finish "⏰ Merged as scheduled at $(Get-Date -Format 'yyyy-MM-dd HH:mm') Asia/Colombo (reviewed head ``$($Sha.Substring(0,10))``)."
}
Log "not merged yet (state=$st); retry next minute"
exit 0
