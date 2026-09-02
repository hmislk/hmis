---
name: review-and-fix
description: >
  Deep-review one open PR, apply the resulting fixes, verify each one live
  (build → local redeploy → Playwright + DB), commit, push, drive CI to
  green, then reply to the review threads. Use when asked to "fix the review
  findings on #N", "review and fix PR #N", "clean up #N before merge", or on
  a PR a prior merge-gate run left BLOCKED-REVIEW. It never merges, approves,
  or requests changes — it hands back a green, fixed PR ready for the user's
  final review and merge.
argument-hint: "<pr-number>"
---

# Review and Fix (HMIS)

Full design rationale: `developer_docs/git/2026-09-02-review-and-fix-skill-design.md`.

This exists because `merge-gate` deliberately **never fixes anything** — it
finds blocking issues and hands back a report. On a real run (PRs `#23348`,
`#23229`, `#22041`) handing that report to the contributor to fix turned out
to be slower and less reliable than fixing it directly: the fixes needed
project-convention knowledge (one flagged "bug" on #22041 was a false
positive — `b.createdAt` is the same "invoice approved" proxy the sibling
inpatient reports use) plus a real verify loop (rebuild → local redeploy →
drive the workflow in Playwright → check the DB) to prove each fix held.
That middle step had no skill. This is it.

**This skill never merges, approves, or requests changes.** It ends when the
PR is fixed, pushed, its review threads answered, and **CI is green on the
head commit** — ready for the user's final review and merge.

Invoking this skill is the explicit authorization for every commit / push /
thread-reply step below — do not re-ask before each one. The discussion
gate is step 3 (classify + discuss the non-obvious findings); that is the
only point where you pause for the user.

## Arguments

- `$0` — one **PR number** (not an issue number). If the number doesn't
  resolve to an open PR, say so and ask for the correct PR number rather
  than guessing which PR closes an issue.
- Optional second argument: a `merge-gate` status-comment URL (or the words
  "merge-gate findings"). When given, fix only what that prior gate flagged
  rather than re-reviewing from scratch — read the linked inline comments,
  skip step 2's fresh `code-review`, and go straight to step 3.

Process exactly one PR per invocation. `merge-gate` already handles the
batch framing; the verify loop here needs to stay focused on one branch.

## 1. Checkout

```bash
git fetch origin
git checkout -- src/main/resources/META-INF/persistence.xml
gh pr checkout <PR>
```

The `git checkout --` discards any leftover uncommitted local-JNDI edit
before the branch switch (safe no-op if there is none). Then restore
`persistence.xml` to local JNDI (`jdbc/coop` / `jdbc/ruhunuAudit`) per
CLAUDE.md, left **unstaged**. Note the exact JNDI names — you restore them
again after the push in step 6.

Record the PR's base branch and how far behind it is. Measure from the
checked-out working tree, not a `origin/<head>` ref — `gh pr checkout` does
not create one for a fork-backed PR:

```bash
git rev-list --count HEAD..origin/development
```

A branch more than a few hundred commits behind `development` is worth a
rebase note in the final report — a clean textual auto-merge can still hide
semantic drift in a helper the changed code calls.

## 2. Fresh review

Skip this step if the optional second argument pointed at a prior
`merge-gate` result — use those findings instead.

Otherwise invoke the `code-review` skill against this PR at **high** effort,
**without** `--comment` or `--fix` — you are going to fix and verify each
finding by hand, not annotate the PR or blind-apply a patch.

Collect the findings it returns with their categories.

## 3. Classify and discuss (the one discussion gate)

| Category | Handling |
|---|---|
| correctness, regression, business-rule violation | **Must fix.** |
| security, privacy, data-integrity, availability | **Must fix** — never treated as optional. |
| style, simplification, efficiency, reuse-only | **Optional.** List them; ask the user whether to include any. |

Before touching code, present the must-fix list and the non-obvious calls
to the user and get a nod. Non-obvious means: anything that could be
**project intent rather than a bug**. Check each candidate against the
codebase and the known false-positive patterns from `review-pr` /
`review-code` first:

- Null checks where lazy init already handles it (e.g.
  `getBillFinanceDetails()`).
- "Fixes" for intentional typos (`purcahseRate`) — database compatibility.
- Constructor-signature changes — CLAUDE.md forbids modifying existing
  constructors; only add new ones.
- Native-SQL suggestions where JPQL is adequate (JPQL-first rule).
- Bootstrap CSS classes where the project uses PrimeFaces.
- A filter/column that looks "wrong" but matches how a sibling report in
  the same module does it (verify against that sibling before "fixing").

For each candidate, state: **Valid — will fix** / **False positive —
<reason>** / **Discuss**. Wait for the user on anything marked Discuss;
don't burn a build guessing.

## 4. Apply the fixes

Apply the confirmed batch. Match the surrounding code's style, naming, and
comment density. Respect the HMIS hard rules (CLAUDE.md): JPQL-first, never
modify existing constructors, `findLongByJpql` for `COUNT`, no hospital-name
gating in `rendered`/conditionals, wire new report buttons into Report
Favorites, etc.

Group everything into **one logical commit** (drafted in step 6), not one
commit per finding.

If a fix adds or renames a persisted entity field, run the `generate-ddl`
skill before moving on (same as `dev-issue` §5a). Skip it for pure
business-logic / query / view fixes.

## 5. Verify each fix live

Do not trust "the code looks right." Every must-fix finding gets exercised.

### 5a. JSF-only changes (XHTML, no Java)

Local Payara serves the exploded WAR and picks up an edited `.xhtml` on the
next request, so a full `mvn package` / `redeploy` is usually unnecessary.
**Confirm the edit is actually live before asserting anything** — hard-reload
the page and check the changed markup is present in the DOM; if it isn't
(stale facelet cache, WAR not exploded), redeploy per §5b first. Then drive
the affected page via the `playwright-e2e` skill: login, select a relevant
department, reproduce the exact scenario the finding was about, and confirm
the new behaviour with DOM assertions or a screenshot. Column-alignment,
`rendered` guards, AJAX-update targets, dialog wiring — all observable this
way once the edit is confirmed live.

### 5b. Java changes

Rebuild and redeploy to local Payara, per `playwright-e2e` §0a / `dev-issue`
§6 (tool paths in CLAUDE.md § Local build tools — verify against the
`reference_maven_path` memory; the paths hardcoded in some skill snippets
are stale for this machine):

```powershell
$env:JAVA_HOME="<JDK 11 path>"
& "<mvn.cmd>" clean package -DskipTests
& "<asadmin.bat>" [--port <admin-port>] redeploy --name <app> "<project-root>\target\rh-3.0.0.war"
```

Check `server.log` for deployment errors before touching the browser. If
`mvn clean package` or `asadmin redeploy` fails, fix the compile/deploy
problem before continuing — a stale WAR verifies nothing.

Then, via `playwright-e2e`: log in, select a department the feature
touches, and exercise the **specific** changed behaviour with real records.
Verify the result in the local DB with read-only `mysql` queries
(credentials: `local_mysql_credentials` memory / `C:\Credentials\`).

If the local DB lacks data to exercise the finding, in order of preference:

1. Use an **existing** record that fits (read-only navigation / API `GET`s
   to find one).
2. Create it **through the app** — the normal billing/admission/report
   workflow — so it is a real, consistent entity.
3. Only if both are blocked (e.g. the UI path 500s on an unrelated
   pre-existing bug), **ask the user** before seeding anything by direct
   SQL. If they approve, keep the insert minimal and schema-valid (real
   enum names, required FKs), scope it to this one verification, and
   **delete it in the same session** — it is throwaway test scaffolding,
   never left behind. This is not "mock data in business logic" (which
   CLAUDE.md forbids); it is a disposable fixture for one browser check.

Never fall back to "code looks correct" as the evidence.

**Local Payara connection-pool note:** a long-idle local domain can start
throwing `EJBTransactionRolledbackException: Client's transaction aborted`
on unrelated queries (patient allergies, favourite reports). Flush the
pools (`asadmin flush-connection-pool poolCoop`,
`... poolRuhunuAuditLocal`) or `restart-domain` — it is not a bug in the
fix. See the `stale_audit_connection_pool_local` memory.

Capture a screenshot / query output for each verified finding into the
project `tmp/` folder. Redact patient identifiers, credentials, and tokens
**as it is written** — `tmp/` is on disk in the project tree, so raw
sensitive evidence must not land there even transiently. Crop/mask
screenshots before saving; select only non-sensitive columns in the
verification query. Remove the `tmp/` artifacts at the end (step 6).

## 6. Commit and push

`persistence.xml` holds a local JNDI name for the duration of this skill and
must end back that way **no matter how this step exits**. Treat the restore
as a `finally`: if the commit or push fails, or you abort here for any
reason, your very next action is to put the local JNDI names back and leave
that change unstaged. Never walk away from this step with `${JDBC_DATASOURCE}`
in the working tree.

1. Check `src/main/resources/META-INF/persistence.xml` — if
   `<jta-data-source>` holds a local JNDI name, note both values, then swap
   both units to `${JDBC_DATASOURCE}` / `${JDBC_AUDIT_DATASOURCE}` with
   `Edit`.
2. `git add` the intended source/doc files plus `persistence.xml` (now
   holding placeholders).
3. Commit with the
   [Commit Conventions](../../../developer_docs/git/commit-conventions.md)
   format — imperative subject, Co-Authored-By trailer. Body: one line per
   finding fixed, each naming the file:line and how it was verified; a
   final short paragraph for any finding deliberately **not** fixed (a
   false positive) and why. **If the commit fails → do 6.5 below and stop.**
4. `git push`. **If the push fails → do 6.5 below and stop.**
5. (6.5) Restore `persistence.xml` to the local JNDI names from 6.1 with
   `Edit`, left **unstaged**. Then `grep` the file to confirm both units
   read `jdbc/...` and not `${...}` before moving on. This restore runs on
   every exit from §6 — success or failure.

Then clean up the `tmp/` evidence.

## 7. Drive CI to green — the skill does not end until it is

`developer_docs/git/pr-review-workflow.md` is explicit that CI must be green
**before** replying to review threads and that there is exactly **one**
re-review request, at the very end. So the reply-in-full step (8) runs after
CI is green — not here. This step only reaches a green head commit, applying
review fixes reply-only along the way.

Wait for **every** check on the head commit: `validate-compilation`,
`validate-jdbc-data-sources`, CodeRabbit, and anything else the PR runs.

- `pending` is **not** a stopping point. Poll it out — `ScheduleWakeup`
  ~270s (same cadence as `dev-issue` §14) and recheck; don't block with
  `gh pr checks --watch` past a couple of minutes.
- On a **check failure**: read the failing job's log, fix the cause, commit,
  push (step 6's `finally` rule for `persistence.xml` applies to every push),
  go back to the top of this step.
- **New CodeRabbit / Codex comments on the fix commit** → loop back to step 3
  for those (classify → fix → verify → commit → push). For each thread you
  acted on, post a **reply-only** note now (`/replies` endpoint,
  `gh api .../pulls/<PR>/comments/<id>/replies`) — "Fixed in `<sha>`: `<what
  changed>`" or "Dismissed because: `<reason>`". Do **not** run the full
  `review-pr` skill here and do **not** re-request review yet — those happen
  once in step 8.

### Loop bounds

- At most **3 review→fix cycles**. If CodeRabbit is still raising new
  substantive findings after the third, stop and ask the user.
- At most **~40 minutes** of wall-clock polling for a stuck `pending` check
  (CodeRabbit is frequently slow / rate-limited on this repo). Past that,
  stop: report which check is stuck and that the two `validate-*` checks are
  green, and let the user decide whether CodeRabbit is a blocker.
- A check that goes **red and stays red** after a fix attempt → stop, report
  exactly which check, the failure, and everything tried. Never hand back a
  half-green PR silently.

Only a **fully green head commit** (or an explicit user decision that a
stuck-pending non-required check is acceptable) lets you proceed to step 8.

## 8. Reply to the review threads (once, after CI is green)

Run the `review-pr` skill for the same PR number. It owns the cardinal
rules — `/replies` endpoint only, never a new top-level thread, no "please
resolve" wording (it triggers a CodeRabbit-Chat auto-PR against a stale
snapshot), self-review items live in the commit message, **one** re-review
request at the end. The fixes are applied, pushed, and CI-verified by now,
so `review-pr`'s reply text describes what was done — "Fixed in
`<head-commit-sha>`: `<what changed>`" for the findings you fixed,
"Dismissed because: `<reason>`" for any false positive — not what a reviewer
should do next. Threads you already answered reply-only in step 7 don't need
a second reply; `review-pr` covers whatever remains and issues the single
re-review request.

If this PR came from a `merge-gate` run, also post one new top-level status
comment recording the fixes applied (commit SHA, one line per finding, and
what was verified live) — this is the same carved-out exception `merge-gate`
uses for its own outcome comments, so a merger who wasn't in the session can
see the gate's findings were addressed.

## 9. Report

Give the user:

- The PR link and the head-commit SHA.
- One line per finding: what it was, how it was fixed (or why dismissed),
  and how it was verified live.
- The CI state — say "green" only when every check on the head commit is
  actually green; if a non-required check is stuck pending and the user
  accepted that per step 7, say so explicitly instead.
- Any rebase caveat from step 1 (branch far behind `development`).
- "Ready for your final review and merge."

**Never merge, approve, or request changes** — that is always the user's
call (matching `dev-issue` §15, `merge-gate`, `review-pr`).

## Definition of done

The skill has **not** completed until **all** of these hold:

- every must-fix finding is fixed **and** verified live (JSF-only: exercised
  in the browser after confirming the edit is live);
- a false positive is left unfixed only with its reasoning recorded in the
  commit body and the final report;
- the fixes are committed and pushed (one logical commit for the review
  batch; additional small commits for any follow-up review-loop fixes are
  fine);
- every review thread has a threaded reply (fixed / dismissed-with-reason) —
  intermediate loop threads answered reply-only in step 7, the rest via
  `review-pr` in step 8, with its single re-review request;
- **CI is fully green on the head commit** — not pending, not "probably
  fine", green — OR the user has explicitly accepted a stuck-pending
  non-required check per step 7's loop bounds;
- `persistence.xml` is back to local JNDI, unstaged; `tmp/` evidence
  removed; working tree otherwise clean.

Stopping after the push, or after replying to threads, or with CI still
pending / red (and no explicit user sign-off on it), is a bug in the skill
— that is the exact failure mode that motivated it.

## Hygiene

- `persistence.xml` discarded and restored to local JNDI around checkout
  (step 1) and again right after **every** push — including the review-loop
  pushes in step 7 — as a `finally`, never only on the success path. Always
  left unstaged, and `grep`-confirmed to read `jdbc/...` afterwards.
- Temporary screenshots and query output go to the project `tmp/` folder,
  redacted of patient / sensitive data **as they are written**, and removed
  at the end.
- Never `git push --force` or skip hooks.

## Not mirrored to `.codex/skills/`

This skill drives `code-review`, `playwright-e2e`, and `review-pr`, and uses
the `Agent`, `mcp__playwright__*`, and `ScheduleWakeup` tools — the same
Claude-only dependency set as `dev-issue`, `dev-issue-unattended`, and
`merge-gate`, none of which are present under `.codex/skills/`.
