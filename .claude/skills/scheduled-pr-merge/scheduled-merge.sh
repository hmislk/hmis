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
# A merge happens only inside [--at, --at + window): the clock is re-read
# immediately before `gh pr merge`, and success is reported only once GitHub
# shows the PR as MERGED (a merge queue may accept it first and merge later).
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

[ "$now" -ge "$AT" ] || { log "before the window; nothing to do"; exit 0; }
ATTEMPTED="$STATE_DIR/$JOB_ID.merge-requested"
# Past the window (machine was off, or cron's yearless line came round again).
if [ "$now" -ge "$END" ] && [ ! -e "$ATTEMPTED" ]; then
  give_up "the job only got to run at $(date '+%Y-%m-%d %H:%M'), after the window."
fi

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
# Only the latest run of each check counts (an older cancelled run can sit
# beside a newer successful one).
checks=$(jq '[.statusCheckRollup[] | {n: (.name // .context), r: (.conclusion // .state // ""), t: (.completedAt // .startedAt // "")}]
              | group_by(.n) | map(max_by(.t))' <<<"$info")
pending=$(jq '[.[] | select(.r | IN("","PENDING","QUEUED","IN_PROGRESS","EXPECTED","WAITING","REQUESTED"))] | length' <<<"$checks")
failed=$(jq -r '[.[] | select((.r | IN("SUCCESS","NEUTRAL","SKIPPED","","PENDING","QUEUED","IN_PROGRESS","EXPECTED","WAITING","REQUESTED")) | not) | .n] | join(", ")' <<<"$checks")
log "state=$state base=$base head=${head:0:10} mergeable=$mergeable mergeState=$mstate pending=$pending failed=[$failed]"

# Already done (e.g. someone merged by hand, or a previous minute did).
if [ "$state" = "MERGED" ]; then
  if [ -e "$ATTEMPTED" ]; then
    log "MERGED (completed after the merge request, e.g. via merge queue)"
    comment "⏰ Merged by the scheduled merge job (completed $(date '+%Y-%m-%d %H:%M %Z')), within the agreed window $window_txt. Head commit \`${SHA:0:10}\` (the reviewed one)."
    rm -f "$ATTEMPTED"
  else
    log "already merged (not by this job)"
  fi
  remove_cron; exit 0
fi
# A merge was requested earlier (e.g. queued) and has not landed: keep watching.
if [ -e "$ATTEMPTED" ]; then
  if [ "$LAST_CHANCE" -eq 1 ] || [ "$now" -ge "$END" ]; then
    log "window over: merge was requested but is still not MERGED (state=$state)"
    comment "⏰ The scheduled merge was **requested** within the window $window_txt, but GitHub had not completed it when the window ended (state: $state, e.g. a merge queue). It may still complete; please check this PR."
    rm -f "$ATTEMPTED"; remove_cron; exit 1
  fi
  log "merge requested earlier, not merged yet; checking again next minute"
  exit 0
fi

# Changed since review: retrying cannot help, so stop now and say why.
[ "$state" = "OPEN" ] || give_up "the PR is $state."
[ "$base" = "$BASE" ] || give_up "the PR now targets \`$base\`, not \`$BASE\`."
[ "$head" = "$SHA" ] || give_up "new commits were pushed after review (head is now \`${head:0:10}\`, reviewed \`${SHA:0:10}\`)."
[ -z "$failed" ] || give_up "status checks failed: $failed."
[ "$mergeable" = "CONFLICTING" ] && give_up "the PR has merge conflicts."

# Temporarily not ready: retry next minute until the window ends.
[ "$pending" -eq 0 ] || not_ready "$pending check(s) still running"
[ "$mergeable" = "MERGEABLE" ] || not_ready "GitHub mergeability is $mergeable"
case "$mstate" in
  CLEAN|HAS_HOOKS) ;;
  BLOCKED)
    owner=${REPO%%/*}; name=${REPO##*/}
    unresolved=$(gh api graphql -f query="query{repository(owner:\"$owner\",name:\"$name\"){pullRequest(number:$PR){reviewThreads(first:100){nodes{isResolved}}}}}" \
      --jq '[.data.repository.pullRequest.reviewThreads.nodes[] | select(.isResolved == false)] | length' 2>/dev/null || echo "?")
    if [ "$unresolved" != "?" ] && [ "$unresolved" -gt 0 ]; then
      not_ready "blocked by $unresolved unresolved review conversation(s), which branch rules require to be resolved"
    fi
    not_ready "merge is blocked by branch rules (e.g. a required review)" ;;
  *) not_ready "merge state is $mstate" ;;
esac

if [ "$DRY_RUN" -eq 1 ]; then
  log "DRY-RUN OK: all preconditions met; would run: gh pr merge $PR --$METHOD --match-head-commit $SHA"
  exit 0
fi
# Re-read the clock: the checks above may have taken a while.
[ "$(date +%s)" -lt "$END" ] || give_up "the window ended while the final checks were running."
if out=$(gh pr merge "$PR" --repo "$REPO" "--$METHOD" --match-head-commit "$SHA" 2>&1); then
  : >"$ATTEMPTED"
  log "merge requested: $out"
  # Success only once GitHub shows MERGED (a merge queue may accept first).
  for i in 1 2 3 4 5 6; do
    st=$(gh pr view "$PR" --repo "$REPO" --json state --jq .state 2>/dev/null || echo "?")
    if [ "$st" = "MERGED" ]; then
      log "MERGED"
      comment "⏰ Merged by the scheduled merge job at $(date '+%Y-%m-%d %H:%M %Z'), within the agreed window $window_txt. Head commit \`${SHA:0:10}\` (the reviewed one); all checks passed."
      rm -f "$ATTEMPTED"; remove_cron; exit 0
    fi
    sleep 10
  done
  log "merge accepted but PR not MERGED yet (state=$st); will keep checking each minute"
  comment "⏰ Scheduled merge requested at $(date '+%Y-%m-%d %H:%M %Z') within the window $window_txt, but GitHub has not merged it yet (state: $st, e.g. merge queue). The job keeps checking and will comment again."
  exit 0
fi
# A failed merge call is retried; a real change (e.g. new head) is caught as
# a give-up on the next attempt.
not_ready "\`gh pr merge\` failed: $(echo "$out" | head -1)"
