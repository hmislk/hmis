---
name: merge-gate
description: >
  Gate one or more open PRs before merge: CI check, then a code-level
  regression/business-rule review, then (only if that's clean) end-to-end
  Playwright verification of the PR's own workflow(s) plus two fixed
  baseline checks. Use when asked to "gate PR(s) for merge", "run
  merge-gate on #N", "check these PRs are safe to merge", or before merging
  any PR that touches shared/core code (API, billing, pharmacy) where a
  regression could silently break unrelated functions.
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

- Failing → record `BLOCKED-CI`, stop this PR, move to the next.
- Pending → `ScheduleWakeup` ~270s (same cadence as `dev-issue` §14), recheck
  once. Still not green → `BLOCKED-CI`, stop this PR.
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

Classify the findings it returns:

| Category | Effect |
|---|---|
| correctness, regression, business-rule violation | **Blocking.** Comments are already posted by `--comment`; summarize in chat; record `BLOCKED-REVIEW`; stop this PR — do not run Phase 2/3. |
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
deployment errors, stop this PR here**: capture the failure output, record
`BLOCKED-BUILD`, and move to the next PR — don't attempt Phase 3 against a
stale or undeployed build.

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
uncertainties"); fixing is separate `dev-issue` work.

All three pass → record `PASSED`.

## Final report

After all PRs are processed, print a table:

| PR # | Outcome | Workflows tested | Notes/links |
|---|---|---|---|

Outcome is one of `PASSED`, `BLOCKED-CI`, `BLOCKED-REVIEW`, `BLOCKED-BUILD`,
`BLOCKED-E2E`. For every `PASSED` row, say it's ready for the user's final
review and merge. For blocked rows, link the PR comment (Phase 1), the
build/deploy failure output (2a), or the evidence captured (Phase 3). Never
merge, approve, or request changes on the user's behalf.

## Hygiene

- Discard and restore `persistence.xml` to local JNDI around every PR's
  checkout (see step 1), left unstaged — repeat for each PR in the batch,
  not just the first.
- Screenshots/evidence go to the project `tmp/` folder (never system
  `/tmp/`), per CLAUDE.md — redacted of patient/sensitive data before they
  leave `tmp/`.

## Required permissions

Same as `playwright-e2e` (full `mcp__playwright__*` set, Maven +
`asadmin`, `mysql` read access) plus `gh` for PR checkout/checks/diff and
the `Agent` tool for the Phase 2 `Explore` dispatch.
