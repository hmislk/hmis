# `/review-and-fix` Skill — Design Spec

Date: 2026-09-02
Status: Approved
Issue: hmislk/hmis#23431

## Motivation

`/merge-gate` runs a batch of open PRs through CI → code-level review → live
end-to-end verification and produces a merge-readiness report. By design it
**never touches the code** — a blocking finding is reported and the PR is
handed back.

On the first substantial run against contributor PRs (`#23348`, `#23229`,
`#22041`) the gate did its job — it found a footer column-alignment defect
on `#23229` and four High-severity correctness findings on `#22041`
(`paidAt` overwritten on every row save, implicit INNER-join row drops
inside `SELECT NEW`, date-only "To Date" excluding the last selected day,
`paidAmount` left stale when "Paid" is unchecked). Handing that report to
the contributor to fix proved slower and less reliable than fixing it
directly, for two reasons:

1. **Project-convention knowledge.** One flagged "bug" on `#22041` — the
   "Invoice Approved" filter keying on `b.createdAt` instead of an approval
   timestamp — was a false positive. `b.createdAt` is the exact proxy the
   sibling `InwardReportController` uses (`pe.finalBill.createdAt`), and
   outside-charge bills never populate `approveAt`, so "fixing" it would
   make the filter match nothing. Distinguishing that from a real bug needs
   someone who can read the sibling report.
2. **A real verification loop.** Confidence that the `paidAt`-preservation
   fix actually held required rebuilding, redeploying locally, seeding two
   `InwardOutSideBill` rows, driving the Update flow in Playwright, and
   checking the `bill` row in the DB before and after editing an unrelated
   field on an already-paid row.

There is no skill for that middle step. `review-pr` only triages
*existing* CodeRabbit/Codex comment threads (and is `Read/Grep/Glob/Bash`
only — it cannot even `Edit`). `review-code` is a manual checklist. The
built-in `code-review --fix` blind-applies a patch with no live
verification. This skill fills the gap.

## Goal

Given one open PR, run: fresh deep code review → classify and discuss the
non-obvious findings → apply the fixes → verify each one live (build →
local redeploy → Playwright + DB, or browser-only for JSF-only changes) →
commit → push → reply to the review threads → **drive CI to green**. The
skill ends only when the head commit is fully green and the PR is ready for
the user's final review and merge. It never merges, approves, or requests
changes.

## Non-goals

- **Not a replacement for `merge-gate`.** `merge-gate` decides *whether* a
  batch of PRs is safe to merge and includes two fixed baseline regression
  checks unrelated to any PR's scope. This skill *fixes* one PR the gate (or
  the user) has already flagged.
- **Not a replacement for `review-pr`.** It *invokes* `review-pr` for the
  thread-reply step rather than re-implementing the cardinal rules
  (`/replies` endpoint, CodeRabbit-Chat trap, one re-review request).
- **Not a general test-automation framework.** It drives the existing
  `code-review`, `playwright-e2e`, and `review-pr` skills.
- **Not batch.** One PR per invocation — the verify loop needs focus.
  `merge-gate` already frames the batch.
- **Does not merge.** Final review and the merge button are always the
  user's, matching every other skill.

## Skill location

`.claude/skills/review-and-fix/SKILL.md`

Not mirrored to `.codex/skills/` — it depends on the `Agent` tool,
`playwright-e2e`'s full `mcp__playwright__*` set, and `ScheduleWakeup`
polling, matching the existing pattern where `dev-issue`,
`dev-issue-unattended`, `playwright-e2e`, `start-issue`, and `merge-gate`
are Claude-only.

## Invocation

`/review-and-fix <pr-number>`

Optional second argument: a `merge-gate` status-comment URL (or "merge-gate
findings"). When present, the skill fixes only what that gate flagged —
reads the linked inline comments, skips the fresh `code-review`, and starts
at the classify/discuss step.

If an issue number is passed by mistake, note the PR/issue distinction and
ask for the PR number rather than guessing which PR closes it.

## Steps

### 1. Checkout

`git fetch origin`; `git checkout --` any leftover `persistence.xml` local
edit; `gh pr checkout <PR>`; restore `persistence.xml` to local JNDI
(unstaged); record the base branch and the commit distance behind
`origin/development` (a branch hundreds of commits behind earns a rebase
note in the final report — a clean textual auto-merge can hide semantic
drift in a called helper).

### 2. Fresh review

Invoke `code-review <PR> high` **without** `--comment` / `--fix`. Collect
findings + categories. Skipped when the invocation targeted a prior
`merge-gate` result.

### 3. Classify and discuss — the one discussion gate

correctness / regression / business-rule → must fix. **security / privacy /
data-integrity / availability → must fix** (never optional). style /
simplification / efficiency / reuse-only → optional, ask. Before editing,
present the must-fix list and every **non-obvious** call (candidates that
could be project intent) to the user, each checked against the codebase and
the `review-pr` / `review-code` false-positive list (lazy-init null
handling, intentional typos, constructor rule, JPQL-first,
PrimeFaces-not-Bootstrap, sibling-report convention). Mark each Valid /
False positive / Discuss. Wait on Discuss.

### 4. Apply the fixes

One logical commit. Match surrounding style. Honour CLAUDE.md hard rules.
Run `generate-ddl` if a persisted field changed.

### 5. Verify each fix live

- **JSF-only:** local Payara serves the exploded WAR, so an edited `.xhtml`
  is usually picked up without a full package/redeploy — but confirm the
  changed markup is actually live in the DOM before asserting; if it isn't
  (stale facelet cache), redeploy per the Java path first. Then drive the
  affected page in Playwright and assert the new behaviour (DOM /
  screenshot).
- **Java:** `mvn clean package` → `asadmin redeploy` → check `server.log` →
  Playwright the specific changed behaviour with real records → verify in
  the local DB (read-only `mysql`). Test data, in order of preference:
  (a) an existing record that fits; (b) create one **through the app**;
  (c) only if both are blocked by an unrelated pre-existing bug, **ask the
  user** before seeding by direct SQL — then keep it minimal and
  schema-valid, scope it to this one check, and delete it in the same
  session. Flush / restart the local Payara pools if
  `Client's transaction aborted` appears on unrelated queries — that is
  environmental, not the fix. Evidence to `tmp/`, redacted **as written**
  (crop screenshots, select non-sensitive columns).

Never accept "the code looks right" as the evidence.

### 6. Commit and push

Swap `persistence.xml` to placeholders → `git add` → commit (Commit
Conventions format; body = one line per finding fixed with file:line + how
verified, plus a paragraph for any deliberate non-fix) → `git push` →
restore local JNDI unstaged → clean up `tmp/`. The `persistence.xml` restore
is a `finally`: a failed commit or push, or an abort here, still ends with
local JNDI back in the working tree, grep-confirmed.

### 7. Drive CI to green — hard exit condition (before replies)

`developer_docs/git/pr-review-workflow.md` requires CI green **before**
replying to threads and **one** re-review request at the very end, so the
full `review-pr` run is step 8, not here. This step only reaches a green
head commit:

Wait for every check on the head commit. `pending` is polled out
(`ScheduleWakeup` ~270s). A check failure is read, fixed, committed, pushed
(step 6's `finally` applies), re-checked. New CodeRabbit / Codex comments on
the fix commit loop back to step 3; for those threads, post **reply-only**
notes now (`/replies` endpoint) — no full `review-pr`, no re-review request
yet.

Loop bounds: at most **3 review→fix cycles**; at most **~40 min** of
wall-clock polling for a stuck `pending` non-required check (CodeRabbit is
often rate-limited on this repo — past that, report it and let the user
decide if it blocks); a check that stays **red** after a fix attempt →
stop and report which check, the failure, and what was tried. Only a fully
green head commit (or an explicit user OK on a stuck-pending non-required
check) proceeds to step 8. Never hand back a half-green PR silently.

### 8. Reply to the review threads (once, after CI is green)

Run `review-pr` for the same PR — it owns the `/replies` cardinal rules and
issues the single re-review request. Reply text describes what was done
("Fixed in `<head-sha>`: ..."), not what a reviewer should do. Threads
already answered reply-only in step 7 need no second reply. If the PR came
from a `merge-gate` run, also post one new top-level status comment
recording the fixes (the same carved-out exception `merge-gate` uses for
its own outcome comments).

### 9. Report

PR link + head SHA; one line per finding (fix / dismissal + live
verification); CI state ("green" only when it truly is); any rebase caveat;
"ready for your final review and merge." Never merge / approve / request
changes.

## Composed workflow

```text
merge-gate #A #B #C          # gate a batch
  -> #A PASSED
  -> #B BLOCKED-REVIEW
  -> #C BLOCKED-REVIEW
review-and-fix #B            # fix + verify + push + CI-green #B
review-and-fix #C            # fix + verify + push + CI-green #C
merge-gate #B #C             # re-gate -> PASSED
# user merges
```

Each skill stays single-purpose; `merge-gate` keeps its "never touches
code" identity.

## Relationship to the existing review skills

| Skill | Fixes? | Fresh review? | Live verify? | CI-green loop? |
|---|---|---|---|---|
| `merge-gate` | No (by design) | Yes (`code-review --comment`) | Yes (E2E + 2 baselines) | No — reports outcome |
| `review-pr` | Yes | No — triages existing bot threads | No (`Read/Grep/Glob/Bash`) | Partial (checks green before replying) |
| `review-code` | No | Manual checklist | No | No |
| `code-review` (built-in) | `--fix` blind-applies | Yes | No | No |
| **`review-and-fix`** (this) | **Yes** | **Yes** | **Yes** | **Yes — hard exit condition** |

## Hygiene

- `persistence.xml` discarded and restored to local JNDI around checkout
  and after every push, always unstaged.
- `tmp/` evidence redacted before leaving `tmp/` and removed at the end.
- No `git push --force`, no hook skipping.
