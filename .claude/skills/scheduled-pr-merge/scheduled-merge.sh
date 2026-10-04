#!/usr/bin/env bash
# One-shot, fail-safe scheduled merge of a reviewed GitHub PR.
# Installed copy + cron entry are created by the scheduled-pr-merge skill.
#
# Merges ONLY if, at run time:
#   - the PR is still OPEN and targets the expected base branch
#   - its head commit is still the one that was reviewed (no pushes since)
#   - every status check has passed (pending checks are waited on, briefly)
#   - GitHub reports it MERGEABLE and CLEAN
#   - the job is not running more than --late-minutes after the scheduled time
# Otherwise it does nothing except log and comment on the PR.
#
# Usage:
#   scheduled-merge.sh --repo OWNER/REPO --pr N --base BRANCH --sha HEADSHA \
#                      --at EPOCH --job-id ID [--method merge|squash|rebase] \
#                      [--late-minutes 30] [--dry-run]

set -u

REPO="" PR="" BASE="" SHA="" AT="" JOB_ID="" METHOD="merge" LATE_MIN=30 DRY_RUN=0
while [ $# -gt 0 ]; do
  case "$1" in
    --repo) REPO="$2"; shift 2 ;;
    --pr) PR="$2"; shift 2 ;;
    --base) BASE="$2"; shift 2 ;;
    --sha) SHA="$2"; shift 2 ;;
    --at) AT="$2"; shift 2 ;;
    --job-id) JOB_ID="$2"; shift 2 ;;
    --method) METHOD="$2"; shift 2 ;;
    --late-minutes) LATE_MIN="$2"; shift 2 ;;
    --dry-run) DRY_RUN=1; shift ;;
    *) echo "unknown argument: $1" >&2; exit 2 ;;
  esac
done
for v in REPO PR BASE SHA AT JOB_ID; do
  [ -n "${!v}" ] || { echo "missing --$(echo "$v" | tr 'A-Z_' 'a-z-')" >&2; exit 2; }
done

STATE_DIR="$HOME/.local/state/hmis-scheduled-merge"
TOKEN_FILE="$HOME/.config/hmis-scheduled-merge/gh-token"
LOG="$STATE_DIR/pr-$PR.log"
mkdir -p "$STATE_DIR"
exec >>"$LOG" 2>&1
log() { echo "[$(date '+%Y-%m-%d %H:%M:%S %z')] $*"; }

log "=== job $JOB_ID start (dry-run=$DRY_RUN) repo=$REPO pr=#$PR base=$BASE sha=${SHA:0:10} method=$METHOD"

# One-shot: a real run removes its own cron line first, so it can never fire
# again (cron has no year field; the same line would fire next year).
if [ "$DRY_RUN" -eq 0 ]; then
  crontab -l 2>/dev/null | grep -v "hmis-scheduled-merge:$JOB_ID\$" | crontab -
  log "removed cron entry for job $JOB_ID"
fi

if [ -r "$TOKEN_FILE" ]; then
  GH_TOKEN="$(cat "$TOKEN_FILE")"; export GH_TOKEN
else
  log "ABORT: token file $TOKEN_FILE not readable"; exit 1
fi

comment() {
  [ "$DRY_RUN" -eq 1 ] && { log "(dry-run) would comment: $1"; return; }
  gh pr comment "$PR" --repo "$REPO" --body "$1" >/dev/null 2>&1 \
    && log "commented on PR" || log "WARN: could not comment on PR"
}
abort() {
  log "ABORT: $1"
  comment "⏰ Scheduled merge **not performed**: $1 Nothing was merged; please merge manually when ready."
  exit 1
}

now=$(date +%s)
if [ $((now - AT)) -gt $((LATE_MIN * 60)) ]; then
  abort "the job ran $(( (now - AT) / 60 )) minutes after its scheduled time (limit ${LATE_MIN} min), outside the agreed window."
fi

# Wait (max ~15 min) for pending checks / mergeability to settle.
for attempt in $(seq 1 15); do
  info=$(gh pr view "$PR" --repo "$REPO" \
    --json state,baseRefName,headRefOid,mergeable,mergeStateStatus,statusCheckRollup 2>&1) \
    || { log "gh error (attempt $attempt): $info"; sleep 60; continue; }
  state=$(jq -r .state <<<"$info")
  base=$(jq -r .baseRefName <<<"$info")
  head=$(jq -r .headRefOid <<<"$info")
  mergeable=$(jq -r .mergeable <<<"$info")
  mstate=$(jq -r .mergeStateStatus <<<"$info")
  pending=$(jq '[.statusCheckRollup[] | (.conclusion // .state // "") | select(. == "" or . == "PENDING" or . == "QUEUED" or . == "IN_PROGRESS" or . == "EXPECTED")] | length' <<<"$info")
  failed=$(jq -r '[.statusCheckRollup[] | select(((.conclusion // .state // "") | IN("SUCCESS","NEUTRAL","SKIPPED","","PENDING","QUEUED","IN_PROGRESS","EXPECTED")) | not) | (.name // .context)] | join(", ")' <<<"$info")
  log "attempt $attempt: state=$state base=$base head=${head:0:10} mergeable=$mergeable mergeState=$mstate pending=$pending failed=[$failed]"

  [ "$state" = "MERGED" ] && { log "already merged; nothing to do"; exit 0; }
  [ "$state" = "OPEN" ] || abort "the PR is $state."
  [ "$base" = "$BASE" ] || abort "the PR now targets \`$base\`, not \`$BASE\`."
  [ "$head" = "$SHA" ] || abort "new commits were pushed after review (head is now \`${head:0:10}\`, reviewed \`${SHA:0:10}\`)."
  [ -z "$failed" ] || abort "status checks failed: $failed."
  if [ "$pending" -eq 0 ] && [ "$mergeable" = "MERGEABLE" ] && { [ "$mstate" = "CLEAN" ] || [ "$mstate" = "HAS_HOOKS" ]; }; then
    if [ "$DRY_RUN" -eq 1 ]; then
      log "DRY-RUN OK: all preconditions met; would run: gh pr merge $PR --$METHOD --match-head-commit $SHA"
      exit 0
    fi
    if out=$(gh pr merge "$PR" --repo "$REPO" "--$METHOD" --match-head-commit "$SHA" 2>&1); then
      log "MERGED: $out"
      comment "⏰ Merged by the scheduled merge job at $(date '+%Y-%m-%d %H:%M %Z'), as agreed with the customer. Head commit \`${SHA:0:10}\` (the reviewed one); all checks passed."
      exit 0
    fi
    abort "\`gh pr merge\` failed: $(echo "$out" | head -1)"
  fi
  [ "$mergeable" = "CONFLICTING" ] && abort "the PR has merge conflicts."
  sleep 60
done
abort "checks were still pending or GitHub did not report the PR as cleanly mergeable after 15 minutes (mergeable=$mergeable, state=$mstate)."
