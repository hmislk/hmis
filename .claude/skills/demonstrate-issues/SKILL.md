---
name: demonstrate-issues
description: >
  Run a live bug-demonstration capture session before filing GitHub issues.
  The user drives the browser and narrates bugs one after another ("see
  this — clicking X should do Y but does Z"); this skill captures each demo
  (snapshot + screenshot + description + environment context) without
  investigating, only after the user signals the session is over does it
  investigate root causes, then only after the user reviews the write-ups
  does it file issues. Use when asked to "demonstrate some bugs", "show you
  issues before filing them", or via `/demonstrate-issues`. Never fixes
  anything — it ends at filing GitHub issue(s); any actual fix is separate,
  later `dev-issue` work.
allowed-tools: Read, Glob, Grep, Bash, PowerShell, mcp__playwright__browser_navigate,
  mcp__playwright__browser_navigate_back, mcp__playwright__browser_click,
  mcp__playwright__browser_type, mcp__playwright__browser_fill_form,
  mcp__playwright__browser_select_option, mcp__playwright__browser_hover,
  mcp__playwright__browser_press_key, mcp__playwright__browser_wait_for,
  mcp__playwright__browser_snapshot, mcp__playwright__browser_take_screenshot,
  mcp__playwright__browser_console_messages, mcp__playwright__browser_network_requests,
  mcp__playwright__browser_evaluate, mcp__playwright__browser_resize,
  mcp__playwright__browser_tabs, mcp__playwright__browser_close
---

# Demonstrate Issues (HMIS)

Structurally separates three phases with hard stops between them:
**demonstrate** → **investigate** → **file**. This exists to prevent acting
on a partial picture — jumping from "here's a bug" straight to code before
every bug the user wants to show is on the table, and before the full
picture of each one is understood.

## Non-goals

- **No development or fixing ever happens inside this skill.** It ends at
  filing GitHub issue(s). Any actual fix is separate, later work — hand the
  filed issue number(s) to `dev-issue`.
- Does not change the behavior of `dev-issue`, `playwright-e2e`, or any
  other skill — they keep auto-logging in and driving the browser
  themselves by default. The user-drives-login default below is local to
  this skill only.

## Reference docs

- [Playwright E2E Testing Workflow](../../../developer_docs/testing/playwright-e2e-workflow.md) —
  PrimeFaces widget commit patterns, dialog handling, the §1 login/department
  gate, and the §8/§8a screenshot-privacy-check convention this skill reuses
  for evidence capture.
- [Playwright MCP Guide](../../../developer_docs/tools/playwright-mcp-guide.md) —
  generic MCP tool mechanics (clicking, dropdowns, common errors).

## 1. Deploy check (environment-agnostic — never hardcode names/ports)

- Resolve the WAR path deterministically each run: prefer `<finalName>` from
  `pom.xml` over globbing. Only fall back to `target/*.war` if `pom.xml`
  doesn't resolve one, and if that glob matches more than one file, treat it
  as the "ambiguous working tree state" case below rather than guessing
  which one to use — never assume a fixed filename like `rh-3.0.0.war`, the
  version differs across checkouts/machines.
- Resolve the Payara **admin port** and the app's **HTTP port** from the
  local credentials file for this machine (`C:\Credentials\credentials.txt`
  or equivalent) — never assume the defaults (4848 / 8080). Multiple Payara
  installs can coexist on one box on non-default ports (see
  [playwright-e2e-workflow §27](../../../developer_docs/testing/playwright-e2e-workflow.md#27-multi-payara-machines-asadmin-without---port-may-hit-another-users-domain)).
  **Don't `Read` the whole credentials file into context** — it may hold
  passwords/tokens alongside the ports. Extract just the port line(s) (e.g.
  `grep`/`findstr` for the admin-port/http-port keys) and use only those
  values.
- Resolve the deployed **context root / URL path** via
  `asadmin --port <admin-port> list-applications` rather than assuming
  `/rh` — different instances are deployed as `/rh`, `/hmis`, `/coop`, etc.
- Compare the resolved WAR's build time against `git log -1` (HEAD) — mtime
  alone doesn't prove the WAR was built from HEAD, so also check
  `git status --short` and record the current commit SHA alongside it. If
  the running app is behind HEAD, rebuild (`mvn clean package -DskipTests`,
  JDK 11) and redeploy automatically using the resolved admin port/app name
  — see [§0a](../../../developer_docs/testing/playwright-e2e-workflow.md#0a-rebuild-and-redeploy-local-code-changes-before-testing).
  Only pause and ask if the build fails or the working tree state is
  ambiguous (e.g. uncommitted changes on a file that affects the build, or
  more than one candidate WAR as above).

## 2. Open login page, hand off (default), but overridable

- `browser_navigate` to the resolved login URL and tell the user it's ready.
- **Default:** the user logs in and selects department themselves, directly
  in the visible Playwright-launched browser window — they may need a
  different department or user account than whatever would be defaulted to,
  and may want to keep those credentials off-screen.
- **Override:** only if the user says something like "you may continue" (or
  otherwise hands control back), log in and navigate for the rest of the
  session, same as `playwright-e2e`'s normal login flow.

## 3. Demonstration loop

- The user narrates/points out each issue in chat while driving the browser
  themselves (e.g. "see this — clicking X should do Y but does Z").
- On the user's cue, capture:
  - an accessibility snapshot (`browser_snapshot`)
  - a screenshot (`browser_take_screenshot`) into the project `tmp/`
    folder, one subfolder per session
  - the user's description, verbatim
  - auto-detected environment context: department and user **role** read
    from the page header (not the raw account/login name), current git
    branch + commit SHA (`git rev-parse --abbrev-ref HEAD` / `git rev-parse
    HEAD`), timestamp
- **Pure capture only at this stage — no code investigation yet.** Confirm
  each capture ("Got it, recorded as demo #N") and wait for the next cue or
  the end signal.
- Multiple issues can be demonstrated in one session, back to back.

### Content-free capture cues

If the cue to capture carries no description at all (e.g. just "capture", or
"check playwrite"), don't capture speculatively and ask afterward what it
was for — ask for the one-line description first (or in the same turn as the
capture), so each recorded demo has its description attached from the start
instead of needing a follow-up round-trip.

### Forward-looking narration vs a firm bug cue

Distinguish "I'm about to show you X" (scene-setting for a demo that hasn't
happened yet) from "see this, X is broken" (the actual cue). Narration that
describes what's coming next is not itself a capture cue — wait for the
concrete bug before recording anything.

If a capture already happened and the user's next message reveals it wasn't
meant as a bug report (e.g. "no error in this page yet, just gathering
facts"), treat that as an explicit instruction to drop the prior capture:
acknowledge it ("Dropping demo #N — noted as context only") and exclude it
from the investigation/filing phases. Don't carry the ambiguity forward and
make the user re-resolve it during investigation.

**HARD STOP** — do not read application code, form a root-cause hypothesis,
or otherwise start investigating any demo until the end signal in step 4.

## 4. End signal

The user says "that's all" (or equivalent) to end the demonstration loop.

## 5. Investigation phase

For each recorded demo: full codebase access is available (grep, read
controllers/JSF pages, `git blame`/`git log` on the relevant file, DB
queries if needed) to work out expected-vs-actual behavior and a root-cause
code pointer (file/line). Database queries are **read-only** and select only
the fields needed to confirm the root cause (same rule as
[playwright-e2e-workflow §6](../../../developer_docs/testing/playwright-e2e-workflow.md#6-verify-against-the-database));
keep raw query results — and especially patient data — out of the
transcript and out of draft/filed issue text.

Rhythm is flexible and assistant-judged: default to investigating all
recorded issues quietly and bringing finished write-ups back for batch
review (step 6), but switch to narrating findings live, issue-by-issue,
when that reads better for a given case — ask the user when genuinely
unsure which mode fits.

## 6. Discuss before filing

Present the **complete sanitized issue body** for each draft — not just
title/summary/root cause/grouping, but the full content from step 7
(environment, steps to reproduce, expected vs actual, root cause, and the
evidence/attachments after redaction) — together for the user's review.

Default is **one GitHub issue per demonstrated bug**, but if two demos seem
to share a root cause (or one demo should split into two issues), ask the
user before filing rather than deciding unilaterally.

**HARD STOP** — do not file anything until the user confirms the exact
final body and attachments for this batch.

## 7. File

One GitHub issue per confirmed bug (per the discussion in step 6), each
following a standard template:

- **Summary**
- **Environment** (branch/commit, department, user role, instance if
  relevant)
- **Steps to reproduce** — minimal and deterministic (strip anything not
  required to trigger the bug)
- **Expected** vs **Actual**
- **Root cause** — code file/line pointer, with a short explanation
- **Evidence** — inspect **every** captured artifact for identifiable data
  (patient names, NICs, phone numbers, financial details, etc.) before it
  goes anywhere near the issue: screenshots, accessibility snapshots, the
  user's verbatim description, environment context, and the assembled issue
  body text itself — not just screenshots. If clean, keep it; if it
  contains identifiable info, redact it, or ask the user how to handle it if
  clean redaction isn't straightforward.

  `gh issue create --body` is text-only — it cannot upload local
  screenshots. Publish screenshots through the existing wiki flow instead,
  same as every other HMIS skill: copy the sanitized images into
  `../hmis.wiki/images/`, commit and push the wiki, then embed the raw wiki
  URLs (`https://raw.githubusercontent.com/wiki/hmislk/hmis/images/<name>.png`)
  in the issue body — see
  [playwright-e2e-workflow §8](../../../developer_docs/testing/playwright-e2e-workflow.md#8-publishing-screenshot-evidence).

File with `gh issue create --repo hmislk/hmis --title "<title>" --body-file
<file>` (use `--body-file` for the multiline body assembled above). Verify
the created issue — including that its embedded images render — before
removing the session's temporary screenshots from the project `tmp/`
folder.
