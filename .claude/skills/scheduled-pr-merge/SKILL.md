---
name: scheduled-pr-merge
description: >
  Use when the developer has finished reviewing a PR (often a hotfix to a
  production or staging branch) and wants it merged, and so CI/CD deployed,
  at a specific later time or window agreed with a customer (e.g. "merge
  #24289 between 3:00 and 3:30am on 5 October"), or wants to list or cancel
  such a scheduled merge. Also /scheduled-pr-merge.
---

# Scheduled PR Merge

A **cron** job merges the PR within the agreed window, by default 30
minutes from the start time. It does not need a Claude session, only this
machine on with cron running.

The merge is essential, so the job keeps trying. Cron starts it every minute
of the window, and anything temporary is retried the next minute. A start
missed by a few minutes still merges.

It is also **fail-safe**. It pins the reviewed head commit and never merges
anything that has changed since the review.

Tools in this folder:
- `schedule.sh` handles `add`, `test`, `list` and `cancel`.
- `scheduled-merge.sh` is the runner.

`add` and `test` install both scripts in
`~/.local/share/hmis-scheduled-merge/`. Jobs run from there, and so can
`list` and `cancel`, whatever branch the repo is on.

## What each minute's attempt does

| Situation | Action |
|---|---|
| All checks passed, head is the reviewed commit, GitHub reports `MERGEABLE`/`CLEAN` | **Merge** (`gh pr merge --match-head-commit`), then stop |
| Already merged, by hand or by an earlier minute | Stop quietly |
| Checks still running, mergeability still computing, network or API error, merge call failed | Retry next minute |
| PR closed, base changed, **new commits pushed**, a check failed, conflicts | Give up now: retrying can't fix these |
| Still not ready in the last minute of the window | Give up |
| Machine was off for the whole window | Give up, from the first run that does happen |

Every give-up and every merge posts a PR comment.

It never uses `--admin` and never merges outside the window. An overlapping
attempt skips its minute.

Results go to two places:
- **PR comment:** a ⏰ comment saying whether it merged, and why not if it
  didn't. This is public, so it states the defect and contains no hospital
  data.
- **Log:** `~/.local/state/hmis-scheduled-merge/pr-<N>.log`.

A merge or give-up deletes the job's cron lines. Cron has no year field,
so without that the job would fire again next year.

## Steps

1. **Confirm the request.** Get the PR number, the date and time in
   **Asia/Colombo** unless told otherwise, and the merge method (default
   `merge`, matching repo history). Confirm the review is complete. The job
   pins whatever head commit exists *now*, so ask the developer to push any
   last fixes first.
2. **Token file (once per machine, and again after `gh auth refresh`).**
   Cron cannot read the desktop keyring (`gh` returns HTTP 401). Ask before
   creating the token file, because it is a credential on disk, outside the
   repo:
   ```bash
   mkdir -p ~/.config/hmis-scheduled-merge && chmod 700 ~/.config/hmis-scheduled-merge
   (umask 077; gh auth token > ~/.config/hmis-scheduled-merge/gh-token)
   ```
3. **Dry run in a cron-like environment.** This must print `DRY-RUN OK`:
   ```bash
   .claude/skills/scheduled-pr-merge/schedule.sh test --repo hmislk/hmis --pr <N>
   ```
4. **Schedule.** Claude Code's auto mode may refuse to edit the crontab. If
   it does, give the developer the exact line to run with `!`. Do not work
   around the refusal.
   ```bash
   ! ~/.local/share/hmis-scheduled-merge/schedule.sh add --repo hmislk/hmis --pr <N> --at "2026-10-05 03:00" --window-minutes 30
   ```
   Then confirm with `schedule.sh list`.
5. **Report.** Give the job id, the pinned head commit, the window with its
   time zone, the log path, and how to cancel it
   (`schedule.sh cancel <job-id>`).
6. **Optional session watch.** Ask whether to keep this session open. If
   yes, set a one-shot reminder (`CronCreate`) for just after the window
   ends to read the log and the PR state and report. If not, the PR
   comment and log are the record. Either way the merge does not depend on
   the session.
7. **After the merge,** if this was a hotfix, do `hotfix-deploy` Step 9a:
   mirror the change into `development` unless it was cherry-picked from
   there.

## Common mistakes

| Mistake | Fix |
|---|---|
| Scheduling before the last fix is pushed | The job aborts because the head changed. Push first, then `add`. |
| `systemd-run --user` timer or `at` | A user timer dies at logout without linger, and `at` isn't installed. Use cron. |
| Relying on the keyring | Cron gets HTTP 401. Use the token file. |
| Time given without a zone | `--at` is read in the machine's zone (Asia/Colombo). Repeat the window back with its zone. |
| Window ends too early | Give the full customer window with `--window-minutes`. The job never merges after it ends. |
| A new push after `add` | The job aborts by design. Re-run `add` after re-review. |
