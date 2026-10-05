#!/usr/bin/env bash
# Schedule, list or cancel PR merges within a time window (cron-based,
# survives Claude sessions ending; needs the machine on and cron running).
#
#   schedule.sh add    --repo OWNER/REPO --pr N --at "2026-10-05 03:00" [--window-minutes 30] [--method merge]
#   schedule.sh test   --repo OWNER/REPO --pr N      # dry-run now, in a cron-like env
#   schedule.sh list
#   schedule.sh cancel JOB_ID
#
# "add" pins the PR's CURRENT head commit and base branch as the reviewed
# state: if either changes before the merge, the job will not merge.
# Cron starts the runner every minute of the window; it retries temporary
# problems until the window ends and stops for good once merged.

set -euo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SHARE="$HOME/.local/share/hmis-scheduled-merge"
TOKEN_FILE="$HOME/.config/hmis-scheduled-merge/gh-token"
STATE_DIR="$HOME/.local/state/hmis-scheduled-merge"
RUNNER="$SHARE/scheduled-merge.sh"

install_runner() {
  mkdir -p "$SHARE"
  # Installed copies, so jobs (and this tool) keep working whatever branch
  # the repo is on. Skip when already running from the installed copy.
  if [ "$HERE" != "$SHARE" ]; then
    install -m 700 "$HERE/scheduled-merge.sh" "$RUNNER"
    install -m 700 "$HERE/schedule.sh" "$SHARE/schedule.sh"
  fi
}

need_token() {
  [ -s "$TOKEN_FILE" ] || { echo "No token file at $TOKEN_FILE. Create it first (see SKILL.md, step 2)." >&2; exit 1; }
  [ "$(stat -c %a "$TOKEN_FILE")" = "600" ] || { echo "$TOKEN_FILE must be chmod 600" >&2; exit 1; }
}

cmd="${1:-}"; shift || true
REPO="" PR="" AT_STR="" METHOD="merge" WINDOW=30
while [ $# -gt 0 ]; do
  case "$1" in
    --repo) REPO="$2"; shift 2 ;;
    --pr) PR="$2"; shift 2 ;;
    --at) AT_STR="$2"; shift 2 ;;
    --method) METHOD="$2"; shift 2 ;;
    --window-minutes) WINDOW="$2"; shift 2 ;;
    *) break ;;
  esac
done

pr_field() { gh pr view "$PR" --repo "$REPO" --json "$1" --jq ".$1"; }

# Values that end up in the cron command line are validated and then quoted,
# so PR metadata (e.g. a branch name) can never be read as shell syntax.
validate() {
  [[ "$REPO" =~ ^[A-Za-z0-9._-]+/[A-Za-z0-9._-]+$ ]] || { echo "bad --repo: $REPO" >&2; exit 2; }
  [[ "$PR" =~ ^[0-9]+$ ]] || { echo "bad --pr: $PR" >&2; exit 2; }
  [[ "$METHOD" =~ ^(merge|squash|rebase)$ ]] || { echo "bad --method: $METHOD" >&2; exit 2; }
  [[ "$WINDOW" =~ ^[0-9]+$ ]] && [ "$WINDOW" -ge 1 ] && [ "$WINDOW" -le 240 ] || { echo "--window-minutes must be 1..240" >&2; exit 2; }
}
validate_pr_meta() {
  [[ "$SHA" =~ ^[0-9a-f]{40}$ ]] || { echo "unexpected head SHA: $SHA" >&2; exit 1; }
  [[ "$BASE" =~ ^[A-Za-z0-9._/-]+$ ]] || { echo "base branch name has unusual characters, refusing: $BASE" >&2; exit 1; }
}

case "$cmd" in
  add)
    [ -n "$REPO" ] && [ -n "$PR" ] && [ -n "$AT_STR" ] || { echo "add needs --repo --pr --at" >&2; exit 2; }
    validate
    need_token
    AT=$(date -d "$AT_STR" +%s)
    [ "$AT" -gt $(( $(date +%s) + 120 )) ] || { echo "Time must be at least 2 minutes in the future: $(date -d "@$AT")" >&2; exit 1; }
    SHA=$(pr_field headRefOid); BASE=$(pr_field baseRefName); STATE=$(pr_field state)
    [ "$STATE" = "OPEN" ] || { echo "PR #$PR is $STATE" >&2; exit 1; }
    validate_pr_meta
    install_runner
    JOB_ID="${REPO//\//-}-pr$PR-$AT"
    CMD="PATH=/usr/local/bin:/usr/bin:/bin $(printf '%q ' "$RUNNER" --repo "$REPO" --pr "$PR" --base "$BASE" --sha "$SHA" --at "$AT" --job-id "$JOB_ID" --window-minutes "$WINDOW" --method "$METHOD")# hmis-scheduled-merge:$JOB_ID"
    # One line per minute inside the window [start, start+window), which also
    # handles hour/midnight crossings.
    LINES=""
    for m in $(seq 0 $(( WINDOW - 1 ))); do
      LINES+="$(date -d "@$(( AT + m * 60 ))" '+%-M %-H %-d %-m *') $CMD"$'\n'
    done
    ( crontab -l 2>/dev/null | grep -v "hmis-scheduled-merge:$JOB_ID\$" || true; printf '%s' "$LINES" ) | crontab -
    echo "Scheduled job $JOB_ID"
    echo "  PR:      $REPO#$PR -> $BASE ($METHOD)"
    echo "  Head:    $SHA (pinned; any new push cancels the merge)"
    echo "  Window:  $(date -d "@$AT" '+%Y-%m-%d %H:%M')–$(date -d "@$(( AT + WINDOW * 60 ))" '+%H:%M %Z') (tries every minute; gives up at the end)"
    echo "  Log:     $STATE_DIR/pr-$PR.log"
    MS=$(pr_field mergeStateStatus)
    [ "$MS" = "CLEAN" ] || echo "  WARNING: GitHub currently reports merge state $MS (not CLEAN). Unless that changes before the window, the job will not merge. Run 'test' for details."
    ;;
  test)
    [ -n "$REPO" ] && [ -n "$PR" ] || { echo "test needs --repo --pr" >&2; exit 2; }
    validate
    need_token
    install_runner
    SHA=$(pr_field headRefOid); BASE=$(pr_field baseRefName)
    validate_pr_meta
    LOGF="$STATE_DIR/pr-$PR.log"; mkdir -p "$STATE_DIR"; touch "$LOGF"
    before=$(wc -l <"$LOGF")
    # env -i mimics cron: no keyring, no session variables.
    rc=0
    env -i HOME="$HOME" PATH=/usr/local/bin:/usr/bin:/bin "$RUNNER" --repo "$REPO" --pr "$PR" \
      --base "$BASE" --sha "$SHA" --at "$(date +%s)" --job-id "test-$PR" --window-minutes 30 --dry-run || rc=$?
    # Show only this invocation's lines, never an older result from the shared log.
    tail -n +$(( before + 1 )) "$LOGF"
    if [ "$rc" -eq 0 ] && tail -n +$(( before + 1 )) "$LOGF" | grep -q "DRY-RUN OK"; then
      echo "RESULT: would merge now"
    else
      echo "RESULT: would NOT merge now (exit $rc); see the lines above"; exit 1
    fi
    ;;
  list)
    { crontab -l 2>/dev/null | grep "hmis-scheduled-merge:" || true; } \
      | sed -E 's/.*--pr ([0-9]+).*--at ([0-9]+).*--window-minutes ([0-9]+).*hmis-scheduled-merge:(.*)$/\4 \1 \2 \3/' | sort -u \
      | while read -r id num at_epoch win; do
          echo "$id  PR #$num  $(date -d "@$at_epoch" '+%Y-%m-%d %H:%M')–$(date -d "@$(( at_epoch + win * 60 ))" '+%H:%M %Z')"
        done
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
