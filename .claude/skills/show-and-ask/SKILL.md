---
name: show-and-ask
description: >
  Use when an issue needs a human decision or is hard to understand from text,
  and seeing the situation happen in the running HMIS app would help: money or
  clinical meaning, policy, hard-to-reverse steps, or facts only the user knows.
  Typical triggers are a `triage: needs-human` issue, "show me this and ask me",
  "walk me through issue N", or `/show-and-ask`. Not for bugs the user drives
  and narrates (use demonstrate-issues) or for verification with no human
  question (use playwright-e2e).
allowed-tools: Read, Glob, Grep, Bash, PowerShell, mcp__playwright__browser_navigate,
  mcp__playwright__browser_click, mcp__playwright__browser_type,
  mcp__playwright__browser_fill_form, mcp__playwright__browser_select_option,
  mcp__playwright__browser_hover, mcp__playwright__browser_press_key,
  mcp__playwright__browser_wait_for, mcp__playwright__browser_snapshot,
  mcp__playwright__browser_take_screenshot, mcp__playwright__browser_evaluate,
  mcp__playwright__browser_handle_dialog, mcp__playwright__browser_tabs,
  mcp__playwright__browser_close
---

# Show and Ask (HMIS)

The reverse of `demonstrate-issues`: Claude drives a visible browser to simulate
a situation, and the user watches, understands and decides. Claude pauses at key
moments, asks, and records the answers on the issue.

**This skill never fixes anything.** A fix is separate `dev-issue` work.

## The bar for asking

First do everything a session can do alone: reproduce, find the cause, run the
tests. Then ask only what truly needs a human:

- money or clinical meaning
- policy
- hard-to-reverse steps
- facts only the user knows

Decide and report a reversible choice with a clear default. Run test steps
yourself instead of handing them to the user. If no human question is left, skip
the session and report that.

Before offering or queuing a session, check that the issue is still open, still
labelled `triage: needs-human` (unless the user asked for the session), and
that its question is not already answered in a comment or a merged PR. A closed or decided issue needs no session.

If nothing needs to be seen on screen, run the session as questions only, in the
terminal: explain the finding in plain words, then ask one question at a time
with numbered options. No app or browser is needed then.

## Where it runs

Run in the session the user is talking to, because a background agent cannot
talk to the user.

- Ask in plain text, not question dialogs, so a dialog does not block the session.
- Nothing else may drive this browser during a session.
- If a redeploy is needed, say that it logs the user out, and wait until the
  user is done.

## Prepare

1. Recheck the issue and any PR (`gh issue view`, `gh pr view <N> --json
   state,headRefOid`). Do this again before relying on any earlier result,
   because a contributor can push mid-session.
2. Write the plain-word explanation, the pause points and the questions in
   `tmp/show-and-ask-<issue>/`.
3. For a session that uses the app, confirm the environment: the local app
   only, on a test database. Check which database the app actually uses. If it is QA1, production or unknown, stop.
   Never write to production, and never write a copy of production patient data
   to a shared database.
4. For a session that uses the app, open the app root headed and ask whether
   the user can see the browser. If not, send a screenshot at each pause instead.

## Session

In a question-only session, skip steps 1, 3 and 6 and the print stub below;
steps 2, 4 and 5 and the posting rules still apply.

1. Log in yourself, using the login steps in `playwright-e2e`, and select the
   department first. Ask about login only if needed. If login fails with a
   correct password, compare the WEBUSER columns with a current schema before
   resetting anything.
2. Explain the error in plain words. Then show it. Then ask.
3. Open only the app root and click through the menus ([never navigate by
   URL](../../../developer_docs/testing/playwright-e2e-workflow.md#-never-navigate-by-typing-a-page-url)).
   Record the menu path. If no menu reaches the page, that is a finding.
4. At each pause, ask "did you see X?" or a comprehension or decision question.
   Ask one question at a time and wait. Give a decision question options and a
   recommendation. Make a comprehension question answerable without reading the
   screen, such as "in your words, what happens to the stock?"
5. Record each answer with its time in the session's `tmp/` log, using the
   user's own words.
6. Verify each test record you create in the intended database. Leave test data
   in place and list it. Never clean it up unasked.

### Before any `p:printer` Print button

`p:printer` calls `execCommand("print")` in an iframe, so stubbing `window.print`
is not enough. A native dialog hangs the browser tools until a person cancels it.
Replace `jQuery.fn.print` in the page right before each click:

```js
jQuery.fn.print = function () { window.__printStubbed = true; return this; };
```

A page load or AJAX update can restore the original, so do not reuse an earlier
stub. If the user wants to see the real dialog, warn them first.

## Evidence and posting

Draft first. Post nothing during the session.

1. Keep screenshots in `tmp/`. Avoid full-page screenshots of bills or patient
   data from copied databases unless needed.
2. Re-read the [public content policy](../../../developer_docs/git/github-public-content-policy.md).
   Redact patient, bill and hospital data from quoted answers before drafting,
   not only from screenshots.
3. Show the user the full redacted comment, including any label change.
4. Post only after the user approves that draft. Publish redacted images through
   the wiki image flow in `demonstrate-issues` (step 7).

## End state

- The decision and evidence are recorded on the issue.
- `triage: needs-human` is removed only if no human input is left and the user
  approved the removal in the same draft. A "not sure" answer keeps the label.
- A follow-up is handed to `dev-issue`.
