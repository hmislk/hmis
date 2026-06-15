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
step below — do not re-ask before each one. Discussion gates (steps 3, 4, 13)
are the points where you pause for the user.

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

## 3. Discuss the approach (Plan Mode)

Enter Plan Mode. Present:
- What you found in step 2
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
  requires testing against a remote env, in which case confirm which one —
  credentials come from `C:\Credentials\credentials.txt`, never inlined

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
- Verify the result in the local DB (credentials: `local_mysql_credentials.md`
  memory — user `buddhika`)

## 8. Iterate

If the test reveals a bug: fix the code (step 5), rebuild/redeploy (step 6),
retest (step 7). Repeat until the flow passes end-to-end.

## 9. Record learnings

If this pass surfaced a new Playwright/dev gotcha (a new PrimeFaces timing
quirk, a new accessibility gap, a new verification pattern), append it to
`developer_docs/testing/playwright-e2e-workflow.md` — same pattern as the
§0a/§5a additions from issue #21499. Don't force this if nothing new came up.

## 10. Pre-push check

Run the `verify-persistence` skill: confirm `persistence.xml` is back to
`${JDBC_DATASOURCE}` / `${JDBC_AUDIT_DATASOURCE}` placeholders before
committing.

## 11. Commit and push

Run `commit-code` with `$0` as the issue number, then `git push`. Immediately
restore `persistence.xml` to local JNDI per `verify-persistence`'s post-push
step (unstaged).

## 12. Create the PR

Target `development`. The PR description should state what was implemented
and summarize the Playwright + DB verification performed in steps 7-8
(concrete enough that a reviewer trusts it was actually tested).

## 13. Review loop (until mergeable)

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

## 14. Notify

Report the PR link, a short summary of what changed, and what was verified.
**Never merge** — that's the user's call.
