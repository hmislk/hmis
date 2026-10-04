---
name: scheduled-pr-merge
description: >
  Use when the developer has finished reviewing a PR (often a hotfix to a
  production or staging branch) and wants it merged, and so CI/CD deployed,
  at a specific later time agreed with a customer (e.g. "merge #24289 at 3am
  on 5 October"), or wants to list or cancel such a scheduled merge. Also
  /scheduled-pr-merge.
---

# Scheduled PR Merge

A one-shot **cron** job merges the PR at the agreed time. It does not need
a Claude session, only this machine on with cron running. It is
**fail-safe**: the job pins the reviewed head commit and merges only if
nothing has changed since the review.

Tools in this folder:
- `schedule.sh` handles `add`, `test`, `list` and `cancel`.
- `scheduled-merge.sh` is the runner.

`add` and `test` install both scripts in
`~/.local/share/hmis-scheduled-merge/`. Jobs run from there, and so can
`list` and `cancel`, whatever branch the repo is on.

## What the runner checks at merge time

The runner merges only if **all** of these hold:
- The PR is still open and targets the same base branch.
- The head commit is the one that was reviewed. It merges with `gh pr merge --match-head-commit`.
- No status check has failed. It waits up to 15 minutes for pending checks.
- GitHub reports the PR as `MERGEABLE` and `CLEAN`.
- The job is no more than `--late-minutes` (default 30) past the agreed
  time. A late merge outside a customer window is worse than none.

It never uses `--admin`.

Results go to two places:
- **PR comment:** a ⏰ comment saying whether it merged, and why not if it
  didn't. This is public, so it states the defect and contains no hospital
  data.
- **Log:** `~/.local/state/hmis-scheduled-merge/pr-<N>.log`.

A real run deletes its own cron line first. Cron has no year field, so
without that the job would fire again next year.

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
   ! .claude/skills/scheduled-pr-merge/schedule.sh add --repo hmislk/hmis --pr <N> --at "2026-10-05 03:00"
   ```
   Then confirm with `schedule.sh list`.
5. **Report.** Give the job id, the pinned head commit, the time with its
   time zone, the log path, and how to cancel it
   (`schedule.sh cancel <job-id>`).
6. **Optional session watch.** Ask whether to keep this session open. If
   yes, set a one-shot reminder (`CronCreate`) for about 20 minutes after the
   merge time to read the log and the PR state and report. If not, the PR
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
| Time given without a zone | `--at` is read in the machine's zone (Asia/Colombo). Repeat the time back with its zone. |
| A new push after `add` | The job aborts by design. Re-run `add` after re-review. |
