---
name: merge-gate
description: >
  Gate one or more open PRs before merge: CI check, then a code-level
  regression/business-rule review, then (only if that's clean) end-to-end
  Playwright verification of the PR's own workflow(s) plus two fixed
  baseline checks. Posts one top-level status comment per PR per run
  (PASSED or BLOCKED-*) so the outcome is visible on GitHub, not just in
  chat. Use when asked to "gate PR(s) for merge", "run merge-gate on #N",
  "check these PRs are safe to merge", or before merging any PR that
  touches shared/core code (API, billing, pharmacy) where a regression
  could silently break unrelated functions.
argument-hint: "<pr-number> [pr-number...]"
---

# Merge Gate (HMIS)

Full design rationale: `developer_docs/git/2026-08-22-merge-gate-skill-design.md`.

This exists because a past API-improvement PR shipped an NPE that broke
**all** API functions, and neither CodeRabbit nor manual review caught it
before merge. This skill adds a code-level review pass plus a live
end-to-end sweep — including two fixed baseline checks unrelated to the
PR's own scope — as a safety net for that class of regression.

**This skill never merges anything.** It ends with a report telling you
which PRs are ready for *your* final review and merge.

## Arguments

- `$0`, `$1`, ... — one or more **PR numbers** (not issue numbers). If a
  number given doesn't resolve to an open PR, say so and ask for the
  correct PR number rather than guessing which PR closes an issue.

## Multi-PR handling

Process PRs **sequentially**, one fully through the pipeline (or blocked)
before starting the next — only one branch/deployment can be live on local
Payara at a time. Keep a running outcome table and print the final report
after the last PR.

## Pipeline (per PR)

### 0. CI gate

```bash
gh pr checks <PR>
```

- Failing → record `BLOCKED-CI`, post the PR status comment (see
  § PR status comment), stop this PR, move to the next.
- Pending → `ScheduleWakeup` ~270s (same cadence as `dev-issue` §14), recheck
  once. Still not green → `BLOCKED-CI`, post the comment, stop this PR.
- Passing → continue.

### 1. Fetch and checkout

```bash
git fetch origin
git checkout -- src/main/resources/META-INF/persistence.xml
gh pr checkout <PR>
```

The `git checkout --` discards the previous PR's uncommitted local-JNDI edit
first — skip it and the branch switch can fail or behave inconsistently
whenever the committed `persistence.xml` differs between branches. Safe
no-op on the very first PR (nothing to discard).

Then restore `persistence.xml` to local JNDI (`jdbc/coop` /
`jdbc/ruhunuAudit`) per CLAUDE.md, left **unstaged**.

### Phase 1 — Code-level regression review

Invoke the `code-review` skill against this PR at **high** effort with
`--comment`, so findings post directly to the PR (reuses the existing
review engine instead of duplicating its logic).

**Before classifying anything, run the fallback below** to confirm every
finding the sub-agent claims to have posted actually landed — do this
*before* the classification step decides to record `BLOCKED-REVIEW` and
stop, not after. Running it after the stop decision defeats the point:
by the time you've stopped this PR and moved to the next one, an
unverified silent drop stays silently dropped.

#### Fallback — verify every finding actually landed as a comment

`code-review --comment` posts via GitHub's PR-review-comment API, which can
only anchor a comment on a line that appears in **this PR's own diff**. A
finding whose defect lives in a file the PR doesn't touch (e.g. a downstream
method the new code merely *calls*) has nowhere to anchor — and can be
silently dropped instead of posted, with no error surfaced back. Confirmed
happening in practice (PR #23082, 2026-08-22: two blocking findings were
returned by the sub-agent as "posted" but never actually appeared on the
PR, because one lived in a file outside the diff and the anchor silently
failed).

Before trusting the sub-agent's "posted successfully" claim, verify for
every finding it returned (classification happens after this, once you
know what's actually posted). Get the actual poster identity first
(don't hardcode a username or just exclude `coderabbitai[bot]` — an
unrelated human/bot comment at the same path:line would then be
misread as confirmation), and **always paginate** — the default page size
silently truncates past ~30 comments, which would make a real PR with
substantial CodeRabbit + human discussion look falsely under-verified (or
mask a genuinely missing finding sitting past the cutoff):

```bash
me=$(gh api user --jq '.login')
gh api --paginate repos/hmislk/hmis/pulls/<PR>/comments \
  --jq --arg me "$me" '.[] | select(.user.login == $me) | "\(.path):\(.line)"'
```

Cross-check this list against the findings returned. For any finding
missing from it (blocking or not — verify all of them, since
classification hasn't happened yet):

1. Check whether its file is part of the PR's diff at all:
   `gh pr diff <PR> --name-only`.
2. **File is in the diff, but the exact line isn't inside a commentable
   hunk** (GitHub only allows anchoring within a hunk's context window —
   posting will fail with `"could not be resolved"`): get the real hunk
   boundaries with `gh api repos/hmislk/hmis/pulls/<PR>/files --jq
   '.[] | select(.filename=="<path>") | .patch'` and anchor on the nearest
   line that's actually inside it instead.
3. **File isn't in the diff at all**: anchor the comment on the *calling*
   line in a file that IS in the diff (typically the new code that invokes
   the problematic downstream method) and cite the true file:line of the
   actual defect explicitly in the comment body text, since the anchor
   line is just the closest available hook, not the defect's real
   location.
4. **No related diff line exists at all**: don't force a mis-anchored
   comment. Fold the finding into the top-level PR status comment instead
   (see § PR status comment) — that comment isn't line-anchored, so it can
   reference any file/line freely.

Post any recovered findings with `gh api repos/hmislk/hmis/pulls/<PR>/comments
-f commit_id=<head-sha> -f path=<path> -F line=<n> -f side=RIGHT -f
body=<text>` (head SHA from `gh pr view <PR> --json commits -q
'.commits[-1].oid'`). Re-run the verification query once more before
classifying anything — don't classify or stop on an unconfirmed assumption
that everything posted.

#### Classify

Now that every finding is confirmed actually posted (or folded into the
status comment per case 4 above), classify what the sub-agent returned:

| Category | Effect |
|---|---|
| correctness, regression, business-rule violation | **Blocking.** Inline comments are already confirmed posted; summarize in chat; post the PR status comment (see § PR status comment); record `BLOCKED-REVIEW`; stop this PR — do not run Phase 2/3. |
| style, simplification, efficiency, reuse-only | **Non-blocking.** Note in chat, continue to Phase 2 regardless. |

No findings at all → continue to Phase 2.

### Phase 2 — Identify affected workflows

```bash
gh pr diff <PR> --name-only
```

Use the `Explore` agent to map the changed files (controllers, services,
entities, XHTML, REST resources) to the user-facing pages/flows that
exercise them. Produce a short concrete list (e.g. "GRN receipt flow",
"Stock Transfer approval"). Only pause for `AskUserQuestion` if the mapping
is genuinely ambiguous (e.g. a shared utility touched by many unrelated
flows) — don't ask when the diff makes the affected flow obvious.

### 2a. Build and local redeploy

Per `playwright-e2e` §0a / `dev-issue` §6:

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-11.0.23.9-hotspot"
& "D:\Program Files\NetBeans-18\netbeans\java\maven\bin\mvn.cmd" clean package -DskipTests
& "D:\Payara\bin\asadmin.bat" redeploy --name rh "D:\Development\2024\hmis\target\rh-3.0.0.war"
```

Check the server log for deployment errors before proceeding. **If `mvn
clean package` fails, `asadmin redeploy` fails, or the server log shows
deployment errors, stop this PR here**: capture the failure output, redact
it of credentials/connection strings/hostnames (same rule as § Phase 3's
evidence redaction), post the PR status comment (see § PR status comment)
with the redacted excerpt, record `BLOCKED-BUILD`, and move to the next
PR — don't attempt Phase 3 against a stale or undeployed build.

### Phase 3 — End-to-end verification

Run via the `playwright-e2e` skill (login, department selection, AJAX-aware
waits, DB verification per its workflow doc). For **every** PR that reaches
this phase, run all three of the following — the two baseline checks are
fixed and always run, regardless of what the PR touches, because the
motivating incident broke core flows unrelated to the changed code:

1. **PR-specific workflow(s)** from Phase 2, using real test data (pick
   department/records the same way `dev-issue` §4 does — confirm with
   `AskUserQuestion` rather than guessing).
2. **Fixed baseline A — pharmacy retail sale → COGS variance.** Make a
   pharmacy retail sale, then open Reports → Inventory Reports → Cost Of
   Good Sold (`/reports/inventoryReports/cost_of_goods_sold`) and confirm
   no unexplained variance. This report's Process button can silently no-op
   or show stale results — poll for AJAX/query completion twice, a few
   seconds apart, before reading the grid (see the project's COGS-report
   testing-gotcha memory).
3. **Fixed baseline B — OPD sale → Cashier Details.** Log into an OPD
   department, make an OPD sale, then open Reports → **Cashier Reports** →
   Cashier Details (`/reports/cashier_reports/cashier_detailed`) and
   confirm the sale appears correctly for the cashier/shift used.

A failure in any of the three (PR workflow or either baseline) is a real
bug the gate caught: capture evidence (screenshots, DB query output) into
the project `tmp/` folder, record `BLOCKED-E2E`, and stop this PR. Redact
patient identifiers, credentials, tokens, and other sensitive fields from
that evidence before it leaves `tmp/` (referenced in chat, posted to a PR
comment, etc.) — same rule as `dev-issue` §2a/§10. Do not attempt a fix
inline — report it and discuss next steps (per CLAUDE.md "discuss
uncertainties"); fixing is separate `dev-issue` work. Post the PR status
comment (see § PR status comment) describing what failed, with the
evidence redacted.

All three pass → record `PASSED` and post the PR status comment (see
§ PR status comment).

## PR status comment

Post exactly **one top-level PR comment per PR per run**, at whatever
terminal state that PR's pipeline reaches (whether it stops early or runs
all the way to `PASSED`). This is an intentional, carved-out exception to
the general "never post top-level PR comments, only reply to reviewer
threads" rule — merge-gate is reporting its own factual outcome, not review
opinion needing threading, so a fresh top-level comment each run is
correct, not noise. Post via:

```bash
gh pr comment <PR> --body "..."
```

If merge-gate is re-run on the same PR later (e.g. after the author
pushed fixes), post a **new** comment rather than editing/deleting the
previous one — the history of gate runs staying visible is the point.

Use an outcome-specific template so a PR author or a merger who wasn't in
this session gets enough context without opening the chat transcript:

**`PASSED`:**
```markdown
## 🚦 Merge Gate: PASSED

**Tested:**
- PR workflow: <short description of what was exercised>
- Baseline A (pharmacy sale → COGS variance): ✅ no unexplained variance
- Baseline B (OPD sale → Cashier Details): ✅ sale appears correctly

Ready for final human review and merge.
```

**`BLOCKED-CI`:** covers both an explicit check failure and a check that
never went green after the recheck — use wording that fits either.
```markdown
## 🚦 Merge Gate: BLOCKED — CI not passing

CI did not reach a passing state: <failed check name and run URL, or
"still pending after the ~270s recheck">. Merge-gate stopped before the
code review pass.
```

**`BLOCKED-REVIEW`:** keep this one short — the inline `--comment` findings
from Phase 1 already carry the detail. Covers correctness, regression, AND
business-rule-violation findings, not just regressions — say "blocking
findings," not "regressions."
```markdown
## 🚦 Merge Gate: BLOCKED — blocking code-review findings

See the inline comments above for specifics. Merge-gate stopped here;
Phase 2/3 (build, E2E, baselines) did not run.
```

**`BLOCKED-BUILD`:** redact the failure output the same way as the E2E
evidence rule below — Maven/Payara/asadmin output can contain JDBC
connection strings, JNDI names, hostnames, or other values CLAUDE.md's
credentials rule forbids ever leaving the machine. Redact before this
excerpt goes anywhere near `gh pr comment`, and cap its length.
```markdown
## 🚦 Merge Gate: BLOCKED — build/deploy failed

<relevant tail of the compile or asadmin failure output, redacted of
credentials, connection strings, hostnames, and other sensitive values>

Merge-gate could not verify this PR end-to-end because the build/deploy
step itself failed.
```

**`BLOCKED-E2E`:** the description below is inline, redacted prose — **not**
a link or attachment. Raw evidence (screenshots, DB output) stays local in
`tmp/` and is never uploaded anywhere; there's no attachment mechanism in
this workflow. Don't imply otherwise in the Final report either (see
§ Final report).
```markdown
## 🚦 Merge Gate: BLOCKED — end-to-end verification found a bug

**Failed:** <PR workflow name, or "Baseline A: COGS variance", or
"Baseline B: Cashier Details">

<redacted description of what went wrong — no patient identifiers,
credentials, or tokens>

This was not auto-fixed — it needs discussion (per CLAUDE.md "discuss
uncertainties") as separate `dev-issue` work.
```

## Final report

After all PRs are processed, print a table:

| PR # | Outcome | Workflows tested | Notes/links |
|---|---|---|---|

Outcome is one of `PASSED`, `BLOCKED-CI`, `BLOCKED-REVIEW`, `BLOCKED-BUILD`,
`BLOCKED-E2E`. Every row's `Notes/links` cell — PASSED included — should
include the URL of that PR's status comment (see § PR status comment); this
is the durable GitHub record, the table itself is just a summary for this
chat. For every `PASSED` row, additionally say it's ready for the user's
final review and merge. For blocked rows, the status comment itself points
to whatever's relevant: the Phase 1 inline comments, the (redacted)
build/deploy failure excerpt, or a redacted description of the Phase 3
failure — evidence never leaves `tmp/` as a link or attachment, only as
inline redacted prose (see § PR status comment). Every PR should already
have its status comment posted by the time this table prints. Never merge,
approve, or request changes on the user's behalf.

## Hygiene

- Discard and restore `persistence.xml` to local JNDI around every PR's
  checkout (see step 1), left unstaged — repeat for each PR in the batch,
  not just the first.
- Screenshots/evidence go to the project `tmp/` folder (never system
  `/tmp/`), per CLAUDE.md — redacted of patient/sensitive data before they
  leave `tmp/`.

## Required permissions

Same as `playwright-e2e` (full `mcp__playwright__*` set, Maven +
`asadmin`, `mysql` read access) plus `gh` for PR checkout/checks/diff/comment
(`gh pr comment`, per § PR status comment) and the `Agent` tool for the
Phase 2 `Explore` dispatch.
