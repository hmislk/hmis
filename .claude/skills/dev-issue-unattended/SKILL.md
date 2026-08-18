---
name: dev-issue-unattended
description: >
  Run one or more GitHub issues through their full lifecycle end-to-end with
  NO pauses for user input — investigate, decide the approach, gather test
  context, and run the CI/review loop entirely autonomously, documenting
  every judgment call for after-the-fact review instead of asking. Accepts
  a single issue, a batch of issue numbers/URLs, or an unfiled bug/request
  described in plain text. Use ONLY when the user has explicitly said they
  will be unreachable (away from the computer, asleep, offline) and wants
  the issue(s) shipped as mergeable PR(s) without them. Do not use this for
  normal work — use dev-issue instead, which asks for input at the same
  points this skill auto-resolves.
argument-hint: "[issue-number|issue-url][, issue-number|issue-url ...] | problem description"
---

# Full Issue Lifecycle, Unattended (HMIS)

This is `dev-issue` with every input-gate replaced by an autonomous,
evidence-documented decision. **`dev-issue` itself is unchanged and remains
the default** — use it whenever the user is actually present. Reach for this
skill only when the user has told you up front they won't be reachable.

Invoking this skill is explicit authorization for every commit/push/PR/issue
step below, including filing the GitHub issue itself if none exists yet — do
not re-ask before any of them.

## 0. Parse input

Accept a comma/space/newline-separated list mixing bare issue numbers
(`23100`) and full GitHub issue URLs
(`https://github.com/hmislk/hmis/issues/23100` — extract the trailing
number with a regex on `/issues/(\d+)`).

- Dedupe the resulting set.
- Validate each with `gh issue view <n> --repo hmislk/hmis --json title`.
  Any that don't resolve are **not** a whole-batch abort — drop them, note
  "could not resolve issue #N" for the final summary (step 15), and
  continue with the rest.
- Process the remaining issues **one at a time, lowest issue number
  first**. Steps 1–14 below are the per-issue body of this loop: each issue
  gets its own branch (via `start-issue`), its own commits, its own PR, and
  its own review loop, exactly as if `dev-issue-unattended` had been run
  solo on just that issue.
- If an issue hits any stop — an existing hard limit, the "insufficient
  issue description" flag (see Hard limits below), or the "could not
  resolve" case above — record the outcome for that issue and move on to
  the next one. The batch is done once every issue in the list has been
  attempted.
- A single free-text problem description with no issue number (the
  existing "if given only a problem description" mode in step 1) is
  unchanged and is not part of batch mode — it still handles exactly one ad
  hoc request per run.

## Hard limits — never bypassed, no matter how confident

These are not judgment calls. If one of these is required to proceed, STOP,
post the blocker as a comment on the issue (creating one first if needed),
and end the run — do not guess.

- Never merge a PR, and never push directly to `development`, `master`, or
  any production branch — everything goes through a PR.
- Never touch anything outside this repo (plus its `../hmis.wiki` sibling,
  needed for step 10's documentation publishing), the local Payara/MySQL dev
  environment, and the GitHub API for this repo — no remote/production hosts.
- Never write a security-privilege or access-control change autonomously.
- Never write, apply, or execute a database schema/migration change
  autonomously — if the fix needs one, stop and describe what's needed
  instead of designing it unsupervised. Step 5a's DDL regeneration is the one
  narrow exception, and only as far as `generate-ddl` itself goes:
  generating the `tmp/createDDL.jdbc` artifact for a column that's a direct,
  evidence-backed part of the approved fix. Applying that DDL to any
  database — even local — stays a human's call via the admin UI's "Add
  Missing..." page, same as it always is; this skill never runs it.
- Never create test data with a direct database write (`INSERT`/`UPDATE`).
  Same rule as `playwright-e2e`
  [§15](../../../developer_docs/testing/playwright-e2e-workflow.md#15-always-generate-test-data--never-fall-back-to-code-only-verification):
  generate it through the app, or stop — see step 4.
- Never put institution names, patient/doctor names, or credentials in any
  GitHub issue, PR, or comment (same rule as `dev-issue`, non-negotiable here
  too since there's no human proofreading before it posts).
- Never resolve genuinely ambiguous behavior — where the codebase, git
  history, and related issues give no clear signal either way — by picking an
  option silently. That is exactly the "stop and flag" case in steps 3 and 14.

### Insufficient issue description — hand back to the reporter, don't just stop

One flavor of stop gets a different resolution than the rest of this
section: when a stop at step 2, 2a, or 3 traces back to **the issue's own
description being inadequate** — not a code-architecture question, not a
schema/security limit — hand it back to whoever filed it instead of posting
a bare blocker comment.

This is a judgment call at runtime, same as any other step-3 decision:
document the reasoning. When genuinely unclear whether a stop is a
description problem or an architecture problem, default to the plain hard
limit above (post blocker, stop) — don't reassign work to a reporter who
can't actually resolve a code-level question.

When it does fire:

1. Post a comment on the issue asking **only** for what actually stalled
   this run — drawn from, not a fixed template dumped every time:
   - the exact page/screen: URL or menu breadcrumb (`Menu > Submenu >
     Page`)
   - a clear description of current vs. expected behavior
   - for report/analytics requests: desired filters, columns, grouping
   - repro steps including a concrete example record/BHT/bill number
   - screenshot(s) of the current behavior or desired layout

   Ground the ask in what was actually tried, e.g. "Searched for a page
   matching this description under Inward and Reports; couldn't identify
   which screen this refers to. Could you share the URL or navigation
   path?" — not a bare template.
2. `@mention` the issue's creator:
   `gh issue view <n> --repo hmislk/hmis --json author --jq '.author.login'`
3. `gh issue edit <n> --repo hmislk/hmis --add-assignee <creator-login>` —
   **added alongside** `buddhika75`, never replacing them.
4. Set the project-board (#11) Status field back to **Backlog** (same
   GraphQL mutation pattern `start-issue` step 5 uses to set it forward to
   "In Progress" — same field, different target option).
5. Record this issue's outcome as "needs info" for the batch summary (step
   15), then continue to the next issue in the batch (step 0).

## 1. Setup

**If given an issue number:** run the `start-issue` skill for it, as
`dev-issue` step 1 does.

**If given only a problem description (no issue number):** do step 2's
investigation first, using `gh issue list --search` to check for an existing
duplicate. If none exists, file the issue yourself with `gh issue create` —
structure it like a normal bug/feature issue (Problem / Root cause found /
Proposed fix / Acceptance criteria), then run `start-issue` on the number you
just created. Filing the issue is not optional busywork — it's what makes the
rest of this run auditable later. **Redact before filing** — the supplied
problem description may itself contain an institution/patient name; strip it
per the hard limit below before it becomes the public issue body.

## 2. Investigate

Same as `dev-issue` step 2: read the issue, explore the code (`Explore` agent
for anything spanning more than a few files), identify the entities/pages
involved and the existing patterns to follow. For bug issues, try to pin down
root cause by reading code — git archaeology (`git log -p -S<term>`,
`git blame`, related closed issues/PRs) is often decisive here and costs
nothing to try before falling back to live reproduction.

If this investigation — including the git archaeology above — cannot
identify which entities/services/pages are even involved, that's the
**Insufficient issue description** case from the Hard limits section, not
a reason to guess. Route there instead of continuing to step 2a/3.

## 2a. Reproduce the bug (bug issues only, root cause still unconfirmed)

Skip for feature/enhancement issues and for bugs where step 2 already found a
confirmed root cause from code + history alone.

- Prefer non-mutating reproduction first (read-only navigation, API `GET`s)
  against existing data.
- If reproduction needs a record that doesn't exist in the local DB, **do
  not** ask which one to use — auto-discover the closest real match with a
  read-only query, and only fall back to generating one through the app (see
  step 4) if nothing suitable exists.
- If it still doesn't reproduce under a reasonable, documented attempt: stop
  rather than guessing at a fix for a bug you couldn't observe. If the reason
  it didn't reproduce is missing specifics from the issue itself (no concrete
  example record, no repro steps, an ambiguous "sometimes it fails" with no
  stated conditions), that's the **Insufficient issue description** case —
  route there. If instead it's an environment mismatch unrelated to what the
  issue said, use the plain hard limit: post the finding to the issue and end
  the run. This is a hard limit either way, not a style preference — an
  unverified fix for an unreproduced bug is worse than no fix.

## 3. Decide the approach (no Plan Mode pause)

Where `dev-issue` enters Plan Mode and waits for approval, instead:

1. Gather the same evidence a plan would need — related issues/PRs, git
   history of the affected code, in-code comments explaining prior intent
   (e.g. this is how issue #22931's fix found and extended the intent behind
   the original #19963 design instead of guessing at a new one).
2. Pick the option best supported by that evidence. When two options are
   both plausible and the evidence doesn't clearly favor one, that is
   "genuinely ambiguous" — stop, but which stop depends on *why* it's
   ambiguous: if the codebase itself gives conflicting signals (two
   existing patterns both plausible), use the plain hard limit above (post
   blocker, stop). If the ambiguity is instead about *what the reporter
   wants* — e.g. a report request with no filters/columns/grouping
   specified, a feature request with no acceptance criteria — that's the
   **Insufficient issue description** case; route there instead.
3. Write the reasoning down **now**, in a form that survives to the PR
   description (step 13) and, for any non-obvious interpretation, an issue
   comment — not just in conversation. The user is reviewing this after the
   fact instead of before, so the trail has to carry the weight a Plan Mode
   approval normally would.

## 4. Gather test context (no AskUserQuestion pause)

Where `dev-issue` step 4 asks for department/records/environment, instead
query the local DB yourself for something real and relevant:

```sql
-- e.g. find an existing record this feature already touches
SELECT ... FROM <entity-table> WHERE <feature-relevant condition>
ORDER BY <recency> LIMIT 5;
```

- Prefer an existing record over creating one — it's already representative
  and needs no cleanup.
- If nothing suitable exists, **generate it through the app** (per
  `playwright-e2e`
  [§15](../../../developer_docs/testing/playwright-e2e-workflow.md#15-always-generate-test-data--never-fall-back-to-code-only-verification):
  create a purchase before a return, a shift-start before a shift-end, etc.)
  instead of asking which record to use.
- If the app itself can't produce what's needed either (e.g. the only path
  to the required state is blocked by unrelated broken data, or requires a
  second user session you don't have credentials for): this is a hard-limit
  stop, same as an unreproducible bug in step 2a — post what was tried and
  why it didn't work, and end the run. Do not paper over it with a direct
  database write; a fixture that skips the app's own validation/business
  logic can pass a test while proving nothing real.
- Environment is local Payara unless the issue explicitly requires otherwise
  — never assume a remote/production environment unattended (hard limit).

## 5. Develop

Same as `dev-issue` step 5 — delegate by file type (`java-backend-developer`,
`jsf-frontend-dev`), review each agent's actual diff before moving on.

## 5a. Regenerate the DDL if the schema changed

Same as `dev-issue` step 5a — but see the hard limit above: this covers
*generating* DDL for a column that's a direct, already-decided part of the
fix, not designing new schema unsupervised and not applying/executing the
generated script against any database.

## 6. Build and local redeploy

Same as `dev-issue` step 6 (adapt the exact commands to whichever machine
this session is running on — see the `playwright-e2e` skill and this
project's local-environment memory for the current host's paths/ports).
Check the server log for deploy errors before moving on.

## 7. Test with Playwright + verify in DB

Same as `dev-issue` step 7: exercise the feature with the department/records
from step 4, screenshot each meaningful stage into `tmp/`, verify in the DB.
If step 4 generated new records through the app, this is also where you
confirm the fix's actual effect on them (e.g. confirm a bypassed guard left
a pending record untouched rather than silently resolving it).

## 8. Iterate

Same as `dev-issue` step 8 — fix, rebuild, retest until it passes end-to-end.
If 3+ fix attempts don't converge, that's the systematic-debugging
architecture-question trigger, not a reason to keep guessing: stop, post
findings, end the run.

## 9. Record learnings

Same as `dev-issue` step 9 — append new Playwright/dev gotchas to
`developer_docs/testing/playwright-e2e-workflow.md` if any surfaced.

## 10. Publish evidence (wiki, issue, PR)

Same as `dev-issue` step 10 (screenshots to `../hmis.wiki/images/`, wiki
commit/push, issue comment with embedded raw wiki-image URLs, clean up
`tmp/`) — with extra weight on redaction since no human reviews the
screenshots before they're published: when in doubt about a screenshot,
crop tighter or drop it rather than publish it uncertain.

## 11. Pre-push check

Same as `dev-issue` step 11 — restore `persistence.xml` placeholders before
staging.

## 12. Commit and push

Same as `dev-issue` step 12 (commit format per
[Commit Conventions](../../../developer_docs/git/commit-conventions.md),
restore local JNDI unstaged after push) — and fold step 3's documented
reasoning into the commit body so it's not only in the PR description. A
commit body is published the moment it's pushed: redact it per the hard
limit above before writing it, same as an issue or PR body.

## 13. Create the PR

Same as `dev-issue` step 13, plus a **"Decisions made without approval"**
section up front listing every step-3 judgment call in one place, so the
user can scan exactly what to double-check first. Redact this body per the
hard limit above too — same as every other publication point.

## 14. Review loop (until mergeable) — waiting without the user present

Repeat, up to **3 cycles**:

1. Use `Monitor` (poll `gh pr checks <PR#> --json name,bucket`, emit each
   newly-resolved check) to wait for CI instead of a synchronous watch. Use
   `ScheduleWakeup` as a fallback heartbeat (~20 min) in case the monitor is
   missed, per this project's autonomous-session pattern.
2. If checks fail: investigate, fix, push, go to 1.
3. Once checks pass: run the `review-pr` skill. Auto-apply fixes matching its
   documented false-positive/valid-fix patterns.
4. A genuinely ambiguous review comment is a hard-limit stop, same as step 3
   — post your assessment as a reply and end the run rather than guess.
5. Checks passing and no unresolved comments are necessary but not
   sufficient. Confirm mergeability itself:
   `gh pr view <PR#> --json mergeable,mergeStateStatus,isDraft,reviewDecision`
   — done only once `mergeable: MERGEABLE`, `mergeStateStatus: CLEAN`,
   `isDraft: false`, and `reviewDecision` isn't blocking (e.g. not
   `CHANGES_REQUESTED`). A required-approval `reviewDecision` with no
   reviewer assigned isn't something this skill can resolve — that's normal
   (a human still has to approve/merge, see step 15), not a stop condition.

3 cycles without convergence → stop, summarize the sticking point, end the
run (same as `dev-issue`).

## 15. Notify

Produce one skimmable summary the user can catch up on in a single read:
what was found, every decision made and why (from step 3/13), what was
verified and how, links to issue/PR/wiki. **Never merge.**
