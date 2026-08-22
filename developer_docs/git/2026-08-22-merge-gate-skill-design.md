# `/merge-gate` Skill — Design Spec

Date: 2026-08-22
Status: Approved

## Motivation

An API improvement PR shipped an NPE that broke all API functions. CodeRabbit
review and the developer's own manual review both missed it. There is no
existing gate that combines (a) a focused code-level regression/business-rule
review and (b) live end-to-end verification before a PR is considered
mergeable. This skill closes that gap.

## Goal

Given one or more PR numbers, run each through: CI check → code-level review
→ (if clean) end-to-end Playwright verification, including two fixed
baseline regression checks that exercise core billing/reporting paths
regardless of what the PR touches → a final merge-readiness report. The
skill never merges — the developer always does the final review and clicks
merge.

## Non-goals

- Not a replacement for `review-pr` (handling reviewer/bot comment threads
  on an already-open review cycle) — that's a separate, later step.
- Not a general test-automation framework — it drives the existing
  `code-review` and `playwright-e2e` skills rather than reimplementing them.
- Does not auto-fix bugs found during E2E — a bug found here is reported and
  the PR is blocked; fixing it is separate `dev-issue` work.

## Skill location

`.claude/skills/merge-gate/SKILL.md`

Not mirrored to `.codex/skills/` — this skill depends on the `Agent`
(Explore) tool, `playwright-e2e`'s full `mcp__playwright__*` tool set, and
`ScheduleWakeup`-style polling, matching the existing pattern where
`dev-issue`, `dev-issue-unattended`, `playwright-e2e`, and `start-issue` are
Claude-only and not present under `.codex/skills/`.

## Invocation

`/merge-gate <pr-number> [pr-number...]`

Arguments are PR numbers directly (not issue numbers). If the user supplies
an issue number by mistake, note the PR/issue distinction and ask them to
give the PR number, rather than guessing which PR closes it.

## Per-PR pipeline

Multiple PRs are processed **sequentially** — one PR goes all the way
through (or is blocked) before the next starts — because only one
branch/deployment can be live on local Payara at a time.

### Step 0 — CI gate

```bash
gh pr checks <PR>
```

- Failing → report and stop this PR (`BLOCKED-CI`). Do not proceed to Phase 1.
- Pending → wait once via `ScheduleWakeup` (~270s, same cadence as
  `dev-issue` §14) and recheck. Still pending/failing → stop this PR
  (`BLOCKED-CI`).
- Passing → proceed to Phase 1.

### Step 1 — Fetch and checkout

```bash
git fetch origin
git checkout -- src/main/resources/META-INF/persistence.xml
gh pr checkout <PR>
```

The `git checkout --` discards the previous PR's uncommitted local-JNDI
edit before switching branches — otherwise the branch switch can fail or
behave inconsistently whenever the committed `persistence.xml` differs
between branches. Safe no-op on the first PR. Restore `persistence.xml` to
local JNDI (`jdbc/coop` / `jdbc/ruhunuAudit`) per CLAUDE.md, unstaged.

### Phase 1 — Code-level review

Invoke the existing `code-review` skill against this PR at **high** effort
with `--comment`, so it posts findings directly to the PR (per the design
discussion — this avoids duplicating review logic in the new skill and
keeps it in sync with future improvements to `code-review`).

Classify returned findings:

- **Blocking categories**: correctness, regression, business-rule
  violation. Any finding in these categories → the comments are already
  posted by `code-review --comment`; report the summary in chat; **stop
  this PR here** (`BLOCKED-REVIEW`). Do not proceed to Phase 2.
- **Non-blocking categories**: style, simplification, efficiency,
  reuse-only suggestions. Note them in chat but continue to Phase 2
  regardless.

If `code-review` finds nothing at all, proceed to Phase 2.

### Phase 2 — Identify affected workflows

Use the `Explore` agent against the PR's changed files (`gh pr diff <PR>
--name-only` as the starting point) to map changed
controllers/services/endpoints/XHTML to the user-facing pages/flows that
exercise that code. Produce a short, concrete list (e.g. "GRN receipt
flow", "Stock Transfer approval"). No user confirmation pause unless the
mapping is genuinely ambiguous (e.g. a shared utility touched by many
unrelated flows) — in that case, ask via `AskUserQuestion` which flow(s) to
prioritize.

### Step 2a — Build and local redeploy

Per `playwright-e2e` §0a / `dev-issue` §6:

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-11.0.23.9-hotspot"
& "D:\Program Files\NetBeans-18\netbeans\java\maven\bin\mvn.cmd" clean package -DskipTests
& "D:\Payara\bin\asadmin.bat" redeploy --name rh "D:\Development\2024\hmis\target\rh-3.0.0.war"
```

Check the server log for deployment errors before proceeding. If `mvn
clean package` fails, `asadmin redeploy` fails, or the server log shows
deployment errors, stop this PR here: capture the failure output, record
`BLOCKED-BUILD`, and move to the next PR — do not attempt Phase 3 against a
stale or undeployed build.

### Phase 3 — End-to-end verification (Playwright)

Run via the `playwright-e2e` skill workflow (login, department selection,
AJAX-aware waits, DB verification). For **every** PR that reaches this
phase, test:

1. **PR-specific workflow(s)** identified in Phase 2 — exercise the actual
   changed behavior with real data (department/records chosen the same way
   `dev-issue` §4 does, via `AskUserQuestion` if not obvious from the diff).
2. **Fixed baseline check A — Pharmacy retail sale → COGS variance.**
   Make a pharmacy retail sale, then open Reports → Inventory Reports →
   Cost Of Good Sold (`/reports/inventoryReports/cost_of_goods_sold`) and
   confirm no unexplained variance. Apply the known testing gotcha: this
   report's Process button can silently no-op or show stale results — poll
   AJAX/query completion twice a few seconds apart before reading the grid
   (see `feedback_cogs_report_testing_gotcha` memory).
3. **Fixed baseline check B — OPD sale → Cashier Details.**
   Log into an OPD department, make an OPD sale, then open Reports →
   Cashier Reports → Cashier Details
   (`/reports/cashier_reports/cashier_detailed`) and confirm the sale
   appears correctly for the cashier/shift used.

These two baseline checks run regardless of what the PR touches, because
the motivating incident (API NPE) broke core flows unrelated to the
changed code — the goal is catching exactly that class of global breakage.

A failure at this phase (PR workflow OR either baseline check) means a real
bug was found: report it with evidence (screenshots/DB query results) and
stop this PR (`BLOCKED-E2E`). Redact patient identifiers, credentials,
tokens, and other sensitive fields from that evidence before it leaves
`tmp/` (chat, PR comment, etc.) — same rule as `dev-issue` §2a/§10. Do not
attempt to fix it inline — flag for discussion per CLAUDE.md "discuss
uncertainties"; fixing is separate `dev-issue` work.

### Step 4 — Record outcome

One of: `PASSED`, `BLOCKED-CI`, `BLOCKED-REVIEW`, `BLOCKED-BUILD`,
`BLOCKED-E2E`, with a one-line reason and links (PR comment URL,
screenshots) for anything blocked.

## Final report (after all PRs processed)

A table: PR # | outcome | workflows tested | notes/links. For `PASSED`
PRs: "ready for your final review to merge." The skill never merges or
approves — that's always the user's call, matching every other skill's
convention (`dev-issue` §15, `review-pr`).

## Hygiene

- `persistence.xml` discarded and restored to local JNDI around each PR's
  checkout (CLAUDE.md rule), for every PR in the batch, not just the first.
- Temporary screenshots go to the project `tmp/` folder, same as
  `playwright-e2e`/`dev-issue` conventions, redacted of patient/sensitive
  data before they leave `tmp/`.
