# Schedule, list or cancel PR merges on Windows, which has no cron. Uses Task
# Scheduler; needs the machine on and this user logged on during the window.
#
#   schedule.ps1 add    -Repo OWNER/REPO -Pr N -At "2026-10-05 03:00" [-WindowMinutes 30] [-Method merge]
#   schedule.ps1 test   -Repo OWNER/REPO -Pr N     # dry-run now
#   schedule.ps1 list
#   schedule.ps1 cancel -TaskName TASK_NAME
#
# "add" pins the PR's CURRENT head commit and base branch as the reviewed
# state: if either changes before the merge, the job will not merge.
param(
    [Parameter(Position = 0, Mandatory = $true)][ValidateSet('add', 'test', 'list', 'cancel')][string]$Command,
    [string]$Repo, [int]$Pr, [string]$At, [int]$WindowMinutes = 30, [string]$Method = 'merge', [string]$TaskName
)
$ErrorActionPreference = 'Stop'
$share = Join-Path $env:USERPROFILE '.local\share\hmis-scheduled-merge'
$runner = Join-Path $share 'scheduled-merge.ps1'

function Read-Pr {
    if (-not $Repo -or -not $Pr) { throw 'Give -Repo and -Pr.' }
    $json = & gh pr view $Pr --repo $Repo --json state,baseRefName,headRefOid
    if ($LASTEXITCODE -ne 0) { throw "Could not read PR $Pr." }
    return $json | ConvertFrom-Json
}

function Find-Tasks {
    return @(Get-ScheduledTask | Where-Object {
        @($_.Actions | Where-Object { $_.Arguments -match 'hmis-scheduled-merge\\(scheduled-merge|merge-pr)\.ps1' }).Count -gt 0
    })
}

switch ($Command) {
    'test' {
        $p = Read-Pr
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'scheduled-merge.ps1') `
            -Repo $Repo -Pr $Pr -Base $p.baseRefName -Sha $p.headRefOid -At (Get-Date -Format 'yyyy-MM-dd HH:mm') -DryRun
        $log = Join-Path $env:USERPROFILE ".local\state\hmis-scheduled-merge\pr-$Pr.log"
        Get-Content $log -Tail 1
    }
    'add' {
        $p = Read-Pr
        if ($p.state -ne 'OPEN') { throw "PR $Pr is $($p.state)." }
        if (-not $At) { throw 'Give -At "yyyy-MM-dd HH:mm".' }
        $start = [datetime]$At
        if ($start -le (Get-Date)) { throw "$At is in the past." }
        New-Item -ItemType Directory -Force $share | Out-Null
        Copy-Item (Join-Path $PSScriptRoot 'scheduled-merge.ps1') $runner -Force

        $name = "$($Repo -replace '/', '-')-pr$Pr"
        $arguments = "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$runner`" -Repo $Repo -Pr $Pr " +
            "-Base $($p.baseRefName) -Sha $($p.headRefOid) -At `"$($start.ToString('yyyy-MM-dd HH:mm'))`" " +
            "-WindowMinutes $WindowMinutes -TaskName $name -Method $Method"
        $action = New-ScheduledTaskAction -Execute 'powershell.exe' -Argument $arguments
        $trigger = New-ScheduledTaskTrigger -Once -At $start -RepetitionInterval (New-TimeSpan -Minutes 1) `
            -RepetitionDuration (New-TimeSpan -Minutes ($WindowMinutes + 1))
        $settings = New-ScheduledTaskSettingsSet -StartWhenAvailable -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
            -ExecutionTimeLimit (New-TimeSpan -Minutes 10)
        $principal = New-ScheduledTaskPrincipal -UserId $env:USERNAME -LogonType Interactive
        # No -Force: an existing job for this PR must be cancelled first.
        Register-ScheduledTask -TaskName $name -Action $action -Trigger $trigger -Settings $settings -Principal $principal | Out-Null

        Write-Output "Scheduled $name"
        Write-Output "  head   $($p.headRefOid)"
        Write-Output "  base   $($p.baseRefName)"
        Write-Output "  window $($start.ToString('yyyy-MM-dd HH:mm')) to $($start.AddMinutes($WindowMinutes).ToString('HH:mm')) local time"
    }
    'list' {
        foreach ($t in Find-Tasks) {
            $info = Get-ScheduledTaskInfo -TaskName $t.TaskName
            Write-Output "$($t.TaskName)  next=$($info.NextRunTime)  $($t.Actions[0].Arguments -replace '^.*\.ps1" ', '')"
        }
    }
    'cancel' {
        if (-not $TaskName) { throw 'Give -TaskName, as printed by list.' }
        if (-not (Find-Tasks | Where-Object { $_.TaskName -eq $TaskName })) { throw "$TaskName is not a scheduled merge." }
        Unregister-ScheduledTask -TaskName $TaskName -Confirm:$false
        Write-Output "Cancelled $TaskName"
    }
}
