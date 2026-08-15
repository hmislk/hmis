---
name: dev-issue
description: >
  Run a GitHub issue through its full lifecycle end-to-end: investigate,
  discuss the approach, gather test context (department/data), implement,
  rebuild + local redeploy, verify with Playwright and the database, iterate
  until passing, record any testing-workflow learnings, commit/push, open a
  PR, and loop on review comments until mergeable. Use when asked to "take
  issue #N end to end", "do issue #N fully", "develop and ship issue #N", or
  similar full-cycle requests.
argument-hint: "<issue-number>"
---

# Full Issue Lifecycle (HMIS)

Invoking this skill is the explicit authorization for every commit/push/PR
step below — do not re-ask before each one. Discussion gates (steps 2a
non-repro case, 3, 4, 14) are the points where you pause for the user.

This authorization also covers `superpowers:writing-plans`' Execution
Handoff question, if that chain gets invoked anywhere in this flow (e.g.
during step 5): auto-select **option 1, Subagent-Driven** without asking —
do not stop for it as an additional discussion gate.

## 1. Setup

Run the `start-issue` skill for `$0`: creates the branch from
`origin/development`, sets `persistence.xml` to local JNDI, assigns the
issue, sets the project board status to In Progress.

## 2. Investigate

- Read the issue body and comments (`gh issue view $0 --comments`).
- Explore the relevant code. Use the `Explore` agent for anything spanning
  more than a few files.
- Identify: which entities/services/JSF pages are involved, which existing
  patterns to follow (DTOs, privileges, AJAX), and what's actually broken or
  missing.
- **If the issue is a bug report**, try to pin down the root cause by reading
  code first. Note explicitly whether this succeeded — that decides whether
  step 2a runs.

## 2a. Reproduce the bug (bug issues only)

Skip this step for feature/enhancement issues, and for bug issues where step
2's code reading already found a clear, confirmed root cause.

Run it when the issue is a bug and step 2 left the cause unconfirmed or
unfound:

- Prefer reproducing against existing data first (read-only navigation or
  API `GET`s). If reproduction requires creating or modifying a record,
  confirm the target department/record with the user first
  (`AskUserQuestion`, same pattern as step 4) rather than picking one
  unilaterally.
- Reproduce live against local Payara — the `playwright-e2e` skill for
  UI-facing bugs, or direct REST calls (per `api-development`) for API-only
  ones.
- Save "before" evidence into the project `tmp/` folder: screenshots for UI
  bugs, request/response bodies for API bugs. Redact patient identifiers,
  credentials, tokens, cookies, and other sensitive fields from any saved
  API body before it leaves `tmp/`.
  - If it reproduces, this evidence proves the bug and becomes the "before"
    half of the before/after comparison published in step 10.
  - If it does **not** reproduce, record that — under the tested
    environment, data, and inputs — the bug did not reproduce; that is not
    proof the bug is absent. Stop here, post the finding to the issue, and
    confirm with the user whether to still proceed (per CLAUDE.md "discuss
    uncertainties") rather than guessing at a fix for a bug you couldn't
    observe.

## 3. Discuss the approach (Plan Mode)

Enter Plan Mode. Present:
- What you found in step 2 (and step 2a's reproduction evidence, for bugs)
- The proposed change (files to touch, approach)
- Anything uncertain (per CLAUDE.md rule "discuss uncertainties")

Exit Plan Mode only once the user approves or adjusts the plan.

## 4. Gather test context

Before writing code, ask the user (via `AskUserQuestion`):
- **Department** to use for Playwright testing (must match a real department
  in the local DB the feature touches — e.g. Pharmacy, Inward, OPD)
- **Specific records** to exercise (e.g. an admission ID, bill number, item
  code) — pick something that exists in the local DB and is relevant to the
  feature
- **Environment**: local Payara (default) unless the issue specifically
  requires testing against a remote env, in which case confirm which one.
  Credentials live outside the repo in `C:\Credentials\` — never inlined

Don't guess these — wrong department/record selection wastes the whole
Playwright pass later.

## 5. Develop

Delegate implementation by file type, per CLAUDE.md module rules (DTOs,
JPQL-first, privilege system, AJAX update rules, etc.):
- Java entities/services/DTOs/REST → `java-backend-developer` agent
- XHTML/PrimeFaces views → `jsf-frontend-dev` agent
- Mixed changes: split into per-area tasks and delegate each

Review the diffs from each agent before moving on — don't trust a summary
without checking the actual edits.

## 5a. Regenerate the DDL if the schema changed

If step 5 added or renamed any entity field, or added a new entity/table,
run the `generate-ddl` skill before moving on. This keeps
`tmp/createDDL.jdbc` and the
[Database-Schema-DDL-Generation-Guide](https://github.com/hmislk/hmis/wiki/Database-Schema-DDL-Generation-Guide)
wiki page in sync with the actual schema, so other developers and fresh
installs can pick up the new column/table without hand-writing a migration.
Skip this step entirely if the issue only changed business logic with no
new persisted fields.

## 6. Build and local redeploy ("deploy sos")

Per `playwright-e2e` §0a:

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-11.0.23.9-hotspot"
& "D:\Program Files\NetBeans-18\netbeans\java\maven\bin\mvn.cmd" clean package -DskipTests
& "D:\Payara\bin\asadmin.bat" redeploy --name rh "D:\Development\2024\hmis\target\rh-3.0.0.war"
```

Check `D:\Payara\glassfish\domains\domain1\logs\server.log` for deployment
errors before moving on.

## 7. Test with Playwright + verify in DB

Run the `playwright-e2e` skill workflow:
- Login, select the department from step 4
- Exercise the feature using the records chosen in step 4
- **Take screenshots** (`browser_take_screenshot`) into the project `tmp/`
  folder at each meaningful stage (before/after states, confirmation dialogs,
  final result) — per playwright-e2e §0. These double as evidence for the
  issue/PR and wiki in step 10. For bug issues where step 2a ran, capture the
  same view/state it reproduced, so it pairs cleanly as the "after" half of
  that before/after comparison. For API-only bugs, replay the original
  request (the actual parameters, not the redacted evidence artifact)
  against the same confirmed target instead. If that request mutates state,
  reuse a resettable/disposable target or get the user's confirmation again
  before replaying it — don't apply a write twice against real data just to
  capture evidence. Save the response status/body (redacted, same rule as
  step 2a) as the "after" evidence.
- Verify the result in the local DB (credentials: see the
  `local_mysql_credentials.md` memory)

## 8. Iterate

If the test reveals a bug: fix the code (step 5), rebuild/redeploy (step 6),
retest (step 7). Repeat until the flow passes end-to-end.

## 9. Record learnings

If this pass surfaced a new Playwright/dev gotcha (a new PrimeFaces timing
quirk, a new accessibility gap, a new verification pattern), append it to
`developer_docs/testing/playwright-e2e-workflow.md` — same pattern as the
§0a/§5a additions from issue #21499. Don't force this if nothing new came up.

## 10. Publish evidence (wiki, issue, PR)

Follow playwright-e2e
[§8 Publishing screenshot evidence](../../../developer_docs/testing/playwright-e2e-workflow.md#8-publishing-screenshot-evidence)
(and, for bug issues,
[§8a Before/after pairing](../../../developer_docs/testing/playwright-e2e-workflow.md#8a-bug-fixes-pair-before-and-after-evidence)):

1. Review the screenshots from steps 2a and 7 and discard/crop any that
   expose patient data, credentials, or other sensitive information. For API
   evidence, redact patient identifiers, credentials, tokens, cookies, and
   other sensitive fields from the request/response bodies before they leave
   `tmp/`.
2. Copy the durable, non-sensitive screenshots into `../hmis.wiki/images/`,
   then commit and push the wiki from `../hmis.wiki`. Redacted API
   request/response snippets aren't images — post them as fenced code blocks
   directly in the issue comment/PR instead of adding them to the wiki.
3. Add a comment (or update the description) on issue `$0`, embedding the
   wiki images via their raw URLs
   (`https://raw.githubusercontent.com/wiki/hmislk/hmis/images/<name>.png`)
   or the redacted API snippets as code blocks. For bug issues where step 2a
   ran, label and pair the step 2a "before" evidence with the step 7 "after"
   evidence so the fix is visible as a comparison. For bug issues where step
   2a was skipped (root cause already confirmed by reading code), there is
   no "before" evidence — publish only the step 7 confirmation, with no
   comparison implied.
4. Remove the temporary screenshots/evidence from the project `tmp/` folder.

These wiki image URLs are reused in the PR description in step 13.

## 11. Pre-push check

Check `src/main/resources/META-INF/persistence.xml` yourself — no skill
needed. If `<jta-data-source>` holds a local JNDI name (e.g. `jdbc/coop`,
`jdbc/ruhunuAudit`) in either persistence unit, note the values (you'll
restore them in step 12) and swap them back to `${JDBC_DATASOURCE}` /
`${JDBC_AUDIT_DATASOURCE}` with `Edit` before staging. If it already reads
placeholders, there's nothing to do here — proceed to commit.

## 12. Commit and push

Stage the intended source/doc files (`git add <files>`), including
`persistence.xml` now that it has placeholders. Commit directly (`git
commit`) with the message format from
[Commit Conventions](../../../developer_docs/git/commit-conventions.md) —
issue number in the closing keyword, Co-Authored-By trailer — then `git
push`. Immediately after the push, restore `persistence.xml` to the local
JNDI names noted in step 11 with `Edit`, leaving that change **unstaged**.

## 13. Create the PR

Target `development`. The PR description should state what was implemented
and summarize the Playwright + DB verification performed in steps 7-8
(concrete enough that a reviewer trusts it was actually tested), and embed
the same wiki-hosted screenshots from step 10 so reviewers can see the
verified behavior without redeploying locally.

## 14. Review loop (until mergeable)

Repeat, up to **3 cycles**:

1. `gh pr checks <PR#>` — if checks are still pending, `ScheduleWakeup` for
   ~270s and recheck (don't block with `--watch` past a few minutes).
2. If checks fail: investigate the failure, fix, push, go to 1.
3. Once checks pass: run the `review-pr` skill for `<PR#>`.
   - Auto-apply fixes for comments matching `review-pr`'s documented
     false-positive/valid-fix patterns.
   - For genuinely ambiguous comments, pause and ask the user — don't burn a
     cycle guessing.
   - If fixes were applied, push and go to 1.
4. If checks are green and there are no unresolved review comments, stop —
   this cycle is done.

If 3 cycles pass without convergence (flaky CI, unresolved disagreement with
a reviewer, etc.), stop and ask the user how to proceed rather than looping
indefinitely.

## 15. Notify

Report the PR link, the issue comment from step 10, a short summary of what
changed, and what was verified (including the published screenshots).
**Never merge** — that's the user's call.
