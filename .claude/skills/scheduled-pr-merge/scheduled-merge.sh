#!/usr/bin/env bash
# Fail-safe scheduled merge of a reviewed GitHub PR within a time window.
# Cron starts this every minute from --at until --at + --window-minutes
# (entries created by schedule.sh). Each start makes ONE attempt:
#
#   merged now / already merged            -> remove cron entries, done
#   something changed since review         -> comment, remove entries, give up
#     (PR closed, base changed, new commits, a check failed, conflicts)
#   temporarily not ready                  -> log, exit; the next minute retries
#     (checks pending, GitHub still computing mergeability, network/API error)
#   still not ready at the end of window   -> comment, remove entries, give up
#
# Usage:
#   scheduled-merge.sh --repo OWNER/REPO --pr N --base BRANCH --sha HEADSHA \
#                      --at EPOCH --job-id ID [--window-minutes 30] \
#                      [--method merge|squash|rebase] [--dry-run]

set -u

REPO="" PR="" BASE="" SHA="" AT="" JOB_ID="" METHOD="merge" WINDOW_MIN=30 DRY_RUN=0
while [ $# -gt 0 ]; do
  case "$1" in
    --repo) REPO="$2"; shift 2 ;;
    --pr) PR="$2"; shift 2 ;;
    --base) BASE="$2"; shift 2 ;;
    --sha) SHA="$2"; shift 2 ;;
    --at) AT="$2"; shift 2 ;;
    --job-id) JOB_ID="$2"; shift 2 ;;
    --method) METHOD="$2"; shift 2 ;;
    --window-minutes) WINDOW_MIN="$2"; shift 2 ;;
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

# Never let two attempts overlap (a slow attempt may outlast its minute).
exec 9>"$STATE_DIR/pr-$PR.lock"
flock -n 9 || { log "job $JOB_ID: previous attempt still running; skipping this minute"; exit 0; }

END=$(( AT + WINDOW_MIN * 60 ))
now=$(date +%s)
LAST_CHANCE=0; [ $(( now + 60 )) -gt "$END" ] && LAST_CHANCE=1
window_txt="$(date -d "@$AT" '+%H:%M')–$(date -d "@$END" '+%H:%M %Z')"

remove_cron() {
  [ "$DRY_RUN" -eq 1 ] && return
  crontab -l 2>/dev/null | grep -v "hmis-scheduled-merge:$JOB_ID\$" | crontab -
  log "removed cron entries for job $JOB_ID"
}
comment() {
  [ "$DRY_RUN" -eq 1 ] && { log "(dry-run) would comment: $1"; return; }
  gh pr comment "$PR" --repo "$REPO" --body "$1" >/dev/null 2>&1 \
    && log "commented on PR" || log "WARN: could not comment on PR"
}
give_up() {
  log "GIVE UP: $1"
  comment "⏰ Scheduled merge (window $window_txt) **not performed**: $1 Nothing was merged; please merge manually."
  remove_cron
  exit 1
}
not_ready() {
  if [ "$LAST_CHANCE" -eq 1 ]; then
    give_up "the window ended and the PR was still not ready ($1)."
  fi
  log "not ready yet ($1); will retry next minute"
  exit 0
}

log "--- job $JOB_ID attempt (dry-run=$DRY_RUN, window $window_txt, last-chance=$LAST_CHANCE)"

[ "$now" -ge $(( AT - 60 )) ] || { log "before the window; nothing to do"; exit 0; }
# Far past the window (e.g. machine was off, or cron's yearless line came round again).
[ "$now" -le $(( END + 120 )) ] || give_up "the job only got to run at $(date '+%Y-%m-%d %H:%M'), after the window."

if [ -r "$TOKEN_FILE" ]; then
  GH_TOKEN="$(cat "$TOKEN_FILE")"; export GH_TOKEN
else
  log "token file $TOKEN_FILE not readable"
  not_ready "GitHub token file missing on the scheduling machine"
fi

info=$(gh pr view "$PR" --repo "$REPO" \
  --json state,baseRefName,headRefOid,mergeable,mergeStateStatus,statusCheckRollup 2>&1) \
  || not_ready "GitHub API error: $(echo "$info" | head -1)"
state=$(jq -r .state <<<"$info")
base=$(jq -r .baseRefName <<<"$info")
head=$(jq -r .headRefOid <<<"$info")
mergeable=$(jq -r .mergeable <<<"$info")
mstate=$(jq -r .mergeStateStatus <<<"$info")
pending=$(jq '[.statusCheckRollup[] | (.conclusion // .state // "") | select(IN("","PENDING","QUEUED","IN_PROGRESS","EXPECTED","WAITING","REQUESTED"))] | length' <<<"$info")
failed=$(jq -r '[.statusCheckRollup[] | select(((.conclusion // .state // "") | IN("SUCCESS","NEUTRAL","SKIPPED","","PENDING","QUEUED","IN_PROGRESS","EXPECTED","WAITING","REQUESTED")) | not) | (.name // .context)] | join(", ")' <<<"$info")
log "state=$state base=$base head=${head:0:10} mergeable=$mergeable mergeState=$mstate pending=$pending failed=[$failed]"

# Already done (e.g. someone merged by hand, or a previous minute did).
[ "$state" = "MERGED" ] && { log "already merged"; remove_cron; exit 0; }

# Changed since review: retrying cannot help, so stop now and say why.
[ "$state" = "OPEN" ] || give_up "the PR is $state."
[ "$base" = "$BASE" ] || give_up "the PR now targets \`$base\`, not \`$BASE\`."
[ "$head" = "$SHA" ] || give_up "new commits were pushed after review (head is now \`${head:0:10}\`, reviewed \`${SHA:0:10}\`)."
[ -z "$failed" ] || give_up "status checks failed: $failed."
[ "$mergeable" = "CONFLICTING" ] && give_up "the PR has merge conflicts."

# Temporarily not ready: retry next minute until the window ends.
[ "$pending" -eq 0 ] || not_ready "$pending check(s) still running"
[ "$mergeable" = "MERGEABLE" ] || not_ready "GitHub mergeability is $mergeable"
case "$mstate" in CLEAN|HAS_HOOKS) ;; *) not_ready "merge state is $mstate" ;; esac

if [ "$DRY_RUN" -eq 1 ]; then
  log "DRY-RUN OK: all preconditions met; would run: gh pr merge $PR --$METHOD --match-head-commit $SHA"
  exit 0
fi
if out=$(gh pr merge "$PR" --repo "$REPO" "--$METHOD" --match-head-commit "$SHA" 2>&1); then
  log "MERGED: $out"
  comment "⏰ Merged by the scheduled merge job at $(date '+%Y-%m-%d %H:%M %Z'), within the agreed window $window_txt. Head commit \`${SHA:0:10}\` (the reviewed one); all checks passed."
  remove_cron
  exit 0
fi
# A failed merge call is retried; a real change (e.g. new head) is caught as
# a give-up on the next attempt.
not_ready "\`gh pr merge\` failed: $(echo "$out" | head -1)"
