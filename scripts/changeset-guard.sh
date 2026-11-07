#!/usr/bin/env bash
# ============================================================
# Changeset guard: fail when commits ADD risky files (screenshots, PDFs,
# dumps, logs, archives, scratch scripts, tmp/ files, secrets, files over
# 5 MB). Shared by CI (.github/workflows/changeset_guard.yml) and the local
# pre-push hook (scripts/hooks/pre-push). Rules live in
# .github/changeset-guard-patterns.txt.
#
# Usage: scripts/changeset-guard.sh <git rev-list arguments>
#   e.g. scripts/changeset-guard.sh HEAD --not origin/development
# Every commit in the range is checked, so a file that was added and later
# deleted still fails: it would stay in the public history.
#
# Portable: bash 3.2+, POSIX grep/sed/awk (Linux, macOS, Git Bash on Windows).
# ============================================================
set -u

if [ "$#" -eq 0 ]; then
  echo "usage: $0 <git rev-list arguments>" >&2
  exit 2
fi

REPO_ROOT="$(git rev-parse --show-toplevel)" || exit 2
PATTERNS="${CHANGESET_GUARD_PATTERNS:-$REPO_ROOT/.github/changeset-guard-patterns.txt}"
MAX_BYTES="${CHANGESET_GUARD_MAX_BYTES:-5242880}"
case "$MAX_BYTES" in
  ''|*[!0-9]*) echo "changeset-guard: CHANGESET_GUARD_MAX_BYTES must be a whole number of bytes: $MAX_BYTES" >&2; exit 2 ;;
esac

if [ ! -f "$PATTERNS" ]; then
  echo "changeset-guard: patterns file not found: $PATTERNS" >&2
  exit 2
fi

BLOCK_FILE="$(mktemp)"
ALLOW_FILE="$(mktemp)"
DENY_FILE="$(mktemp)"
ADDED_FILE="$(mktemp)"
trap 'rm -f "$BLOCK_FILE" "$ALLOW_FILE" "$DENY_FILE" "$ADDED_FILE"' EXIT

# Split the patterns file into block, allow and deny lists (tolerates CRLF).
while IFS= read -r line || [ -n "$line" ]; do
  line="$(printf '%s' "$line" | tr -d '\r')"
  case "$line" in
    ''|'#'*) continue ;;
    'block '*) printf '%s\n' "${line#block }" >> "$BLOCK_FILE" ;;
    'allow '*) printf '%s\n' "${line#allow }" >> "$ALLOW_FILE" ;;
    'deny '*) printf '%s\n' "${line#deny }" >> "$DENY_FILE" ;;
    *) echo "changeset-guard: ignoring malformed rule: $line" >&2 ;;
  esac
done < "$PATTERNS"

if ! git rev-list "$@" >/dev/null 2>&1; then
  echo "changeset-guard: invalid revision range: $*" >&2
  exit 2
fi

# One line per added file: <commit> <blob> <mode> <path>
git -c core.quotePath=false log --no-renames --diff-filter=A --raw --no-abbrev \
  --format='commit %H' "$@" 2>/dev/null \
  | awk '
      /^commit / { c = $2; next }
      /^:/ {
        tab = index($0, "\t"); path = substr($0, tab + 1)
        split(substr($0, 1, tab - 1), f, " ")
        print c " " f[4] " " f[2] " " path
      }' > "$ADDED_FILE"

FOUND=0
REPORT=""
while IFS=' ' read -r commit blob mode path; do
  [ -n "${path:-}" ] || continue
  reason=""
  list=""
  if [ -s "$DENY_FILE" ] && printf '%s\n' "$path" | grep -iqE -f "$DENY_FILE"; then
    list="$DENY_FILE"
  elif ! printf '%s\n' "$path" | grep -iqE -f "$ALLOW_FILE" 2>/dev/null; then
    if [ -s "$BLOCK_FILE" ] && printf '%s\n' "$path" | grep -iqE -f "$BLOCK_FILE"; then
      list="$BLOCK_FILE"
    fi
  fi
  if [ -n "$list" ]; then
    rule="$(while IFS= read -r re; do
              printf '%s\n' "$path" | grep -iqE -- "$re" && { printf '%s' "$re"; break; }
            done < "$list")"
    reason="matches risky pattern '$rule'"
  fi
  if [ "$mode" != "160000" ]; then
    size="$(git cat-file -s "$blob" 2>/dev/null || echo 0)"
    if [ "$size" -gt "$MAX_BYTES" ]; then
      big="larger than $((MAX_BYTES / 1048576)) MB ($size bytes)"
      if [ -n "$reason" ]; then reason="$reason; $big"; else reason="$big"; fi
    fi
  fi
  if [ -n "$reason" ]; then
    FOUND=$((FOUND + 1))
    REPORT="$REPORT  $path  (added in $(printf '%s' "$commit" | cut -c1-10)): $reason
"
  fi
done < "$ADDED_FILE"

if [ "$FOUND" -gt 0 ]; then
  echo "changeset-guard: $FOUND risky file(s) added. This repository is PUBLIC."
  printf '%s' "$REPORT"
  echo ""
  echo "Remove each file from the commits that add it (git rebase / git reset), not"
  echo "just in a later commit: a deleted file stays in the public history."
  echo "Screenshots, PDFs, dumps and logs belong outside the repo. If a file is"
  echo "legitimate, add an 'allow' rule to .github/changeset-guard-patterns.txt"
  echo "('allow' does not exempt 'deny' rules or files over the size limit)."
  exit 1
fi

echo "changeset-guard: OK, no risky files added."
exit 0
