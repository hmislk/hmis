#!/usr/bin/env bash
# Schedule, list or cancel one-shot PR merges (cron-based, survives Claude
# sessions ending; needs the machine on and the cron daemon running).
#
#   schedule.sh add    --repo OWNER/REPO --pr N --at "2026-10-05 03:00" [--method merge] [--late-minutes 30]
#   schedule.sh test   --repo OWNER/REPO --pr N      # dry-run now, in a cron-like env
#   schedule.sh list
#   schedule.sh cancel JOB_ID
#
# "add" pins the PR's CURRENT head commit and base branch as the reviewed
# state: if either changes before the scheduled time, the job will not merge.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SHARE="$HOME/.local/share/hmis-scheduled-merge"
TOKEN_FILE="$HOME/.config/hmis-scheduled-merge/gh-token"
STATE_DIR="$HOME/.local/state/hmis-scheduled-merge"
RUNNER="$SHARE/scheduled-merge.sh"

install_runner() {
  mkdir -p "$SHARE"
  # Installed copy, so the job keeps working whatever branch the repo is on.
  install -m 700 "$HERE/scheduled-merge.sh" "$RUNNER"
}

need_token() {
  [ -s "$TOKEN_FILE" ] || { echo "No token file at $TOKEN_FILE. Create it first (see SKILL.md, step 2)." >&2; exit 1; }
  [ "$(stat -c %a "$TOKEN_FILE")" = "600" ] || { echo "$TOKEN_FILE must be chmod 600" >&2; exit 1; }
}

cmd="${1:-}"; shift || true
REPO="" PR="" AT_STR="" METHOD="merge" LATE=30
while [ $# -gt 0 ]; do
  case "$1" in
    --repo) REPO="$2"; shift 2 ;;
    --pr) PR="$2"; shift 2 ;;
    --at) AT_STR="$2"; shift 2 ;;
    --method) METHOD="$2"; shift 2 ;;
    --late-minutes) LATE="$2"; shift 2 ;;
    *) break ;;
  esac
done

pr_field() { gh pr view "$PR" --repo "$REPO" --json "$1" --jq ".$1"; }

case "$cmd" in
  add)
    [ -n "$REPO" ] && [ -n "$PR" ] && [ -n "$AT_STR" ] || { echo "add needs --repo --pr --at" >&2; exit 2; }
    need_token
    AT=$(date -d "$AT_STR" +%s)
    [ "$AT" -gt $(( $(date +%s) + 120 )) ] || { echo "Time must be at least 2 minutes in the future: $(date -d "@$AT")" >&2; exit 1; }
    SHA=$(pr_field headRefOid); BASE=$(pr_field baseRefName); STATE=$(pr_field state)
    [ "$STATE" = "OPEN" ] || { echo "PR #$PR is $STATE" >&2; exit 1; }
    install_runner
    JOB_ID="pr$PR-$AT"
    CRON_TIME=$(date -d "@$AT" '+%-M %-H %-d %-m *')
    LINE="$CRON_TIME PATH=/usr/local/bin:/usr/bin:/bin $RUNNER --repo $REPO --pr $PR --base $BASE --sha $SHA --at $AT --job-id $JOB_ID --method $METHOD --late-minutes $LATE # hmis-scheduled-merge:$JOB_ID"
    ( crontab -l 2>/dev/null | grep -v "hmis-scheduled-merge:$JOB_ID\$" || true; echo "$LINE" ) | crontab -
    echo "Scheduled job $JOB_ID"
    echo "  PR:      $REPO#$PR -> $BASE ($METHOD)"
    echo "  Head:    $SHA (pinned; any new push cancels the merge)"
    echo "  When:    $(date -d "@$AT" '+%Y-%m-%d %H:%M %Z') (aborts if more than $LATE min late)"
    echo "  Log:     $STATE_DIR/pr-$PR.log"
    ;;
  test)
    [ -n "$REPO" ] && [ -n "$PR" ] || { echo "test needs --repo --pr" >&2; exit 2; }
    need_token
    install_runner
    SHA=$(pr_field headRefOid); BASE=$(pr_field baseRefName)
    # env -i mimics cron: no keyring, no session variables.
    env -i HOME="$HOME" PATH=/usr/local/bin:/usr/bin:/bin "$RUNNER" --repo "$REPO" --pr "$PR" \
      --base "$BASE" --sha "$SHA" --at "$(date +%s)" --job-id "test-$PR" --dry-run || true
    tail -n 5 "$STATE_DIR/pr-$PR.log"
    ;;
  list)
    { crontab -l 2>/dev/null | grep "hmis-scheduled-merge:" || true; } | sed -E 's/.*--pr ([0-9]+).*--at ([0-9]+).*hmis-scheduled-merge:(.*)$/\3  PR #\1  at \2/' \
      | while read -r id pr num at at_epoch; do echo "$id  $pr $num  $(date -d "@$at_epoch" '+%Y-%m-%d %H:%M %Z')"; done
    crontab -l 2>/dev/null | grep -q "hmis-scheduled-merge:" || echo "No scheduled merges."
    ;;
  cancel)
    JOB_ID="${1:?cancel needs JOB_ID (see list)}"
    crontab -l 2>/dev/null | grep -v "hmis-scheduled-merge:$JOB_ID\$" | crontab -
    echo "Cancelled $JOB_ID"
    ;;
  *)
    sed -n '2,10p' "$0"; exit 2 ;;
esac
