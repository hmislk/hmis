---
name: playwright-e2e
description: >
  Drive the running HMIS app with the Playwright MCP server for end-to-end
  verification of a feature (login, department selection, PrimeFaces AJAX
  forms, confirm dialogs, DB-backed verification). Use when asked to test,
  verify, or screenshot a feature in the browser, or to confirm a fix works
  end-to-end against a real deployment. Rebuilds/redeploys local changes via
  Maven + asadmin first if needed.
allowed-tools: Read, Glob, Grep, Bash, PowerShell, mcp__playwright__browser_navigate,
  mcp__playwright__browser_navigate_back, mcp__playwright__browser_click,
  mcp__playwright__browser_type, mcp__playwright__browser_fill_form,
  mcp__playwright__browser_select_option, mcp__playwright__browser_hover,
  mcp__playwright__browser_drag, mcp__playwright__browser_drop,
  mcp__playwright__browser_press_key, mcp__playwright__browser_file_upload,
  mcp__playwright__browser_handle_dialog, mcp__playwright__browser_wait_for,
  mcp__playwright__browser_snapshot, mcp__playwright__browser_take_screenshot,
  mcp__playwright__browser_console_messages, mcp__playwright__browser_network_request,
  mcp__playwright__browser_network_requests, mcp__playwright__browser_evaluate,
  mcp__playwright__browser_run_code_unsafe, mcp__playwright__browser_resize,
  mcp__playwright__browser_tabs, mcp__playwright__browser_close
---

# Playwright E2E Testing (HMIS)

Full operational workflow lives in
[Playwright E2E Testing Workflow](../../../developer_docs/testing/playwright-e2e-workflow.md) —
read it before driving the browser. This skill is the entry point and adds the
rebuild/redeploy and permission context.

For general MCP tool mechanics (tool reference, clicking/dropdown/file-upload
patterns, common errors) see the companion
[Playwright MCP Guide](../../../developer_docs/tools/playwright-mcp-guide.md) —
the workflow doc above is HMIS-specific; the guide is generic Playwright MCP usage.

## 🚨 Never navigate by URL

**Only ever type a URL for the application root / login page.** Every inner page
must be reached by clicking through the menus, exactly as a user would. Real
users have no other way in — some terminals are kiosks with no address bar.

This is not cosmetic. HMIS pages are backed by `@SessionScoped` controllers whose
state is set by the **navigation method** (`toSearchServiceBill()`,
`toManageDepartmentPreferences()`, …), not by the page. Load the page by URL and
that state is null, or a lazily-created transient placeholder — so the page can
500, render blank, or run pathologically slowly in a way no user can ever hit.
Anything you observe that way is an artifact, not a defect.

Establish the menu path **before** testing and record it in the issue/PR in the
form the user can follow, e.g. *Menu → Inpatient → Search → Service Bill → set
From Date → Search Bill → click Bill No → Return*. If no menu path exists, that
is the finding — the page is unreachable in production. Do not fall back to the
URL to get on with the test. See
[§2](../../../developer_docs/testing/playwright-e2e-workflow.md#-never-navigate-by-typing-a-page-url).

## If the Playwright MCP server shows as unavailable

An MCP server that failed to connect at session start (e.g. a system-reminder
saying `playwright (CONNECT_TIMEOUT)` or similar) will **not** self-heal
mid-session — a session's MCP connections are established once at startup, so
retrying a Playwright tool call again later in the same session wastes a
round-trip and always fails the same way.

Instead, **immediately and without asking the user**, run:
```bash
claude mcp list
```
This performs a fresh, independent health check outside the current session's
stale connection state. `claude mcp list` reports one of several distinct
statuses per server — treat each differently rather than collapsing them into
a binary "healthy or not":

- **`✔ Connected`** — the server is actually reachable; the earlier failure
  was specific to this session's startup timing, not a real outage. Tell the
  user briefly that the initial connection attempt timed out but the server
  is confirmed healthy, then fall back to `claude-in-chrome` for *this*
  session (per [[feedback-prefer-playwright-over-claude-in-chrome]] project
  memory) and recommend starting a fresh session next time to pick up the
  working connection.
- **`! Needs authentication`** — not an outage; the server needs a sign-in or
  header the current session hasn't provided. Tell the user it needs
  re-authentication (`/mcp` panel or `claude mcp login`) rather than treating
  it as down.
- **`⏸ Pending approval`** — project-scoped server awaiting manual approval;
  tell the user to run `claude` interactively to approve it, not a real
  outage either.
- **`! Connected · tools fetch failed`** — the connection itself works but
  tool listing errored; this is a real (if partial) problem worth surfacing,
  distinct from a clean outage.
- **`✘ Failed to connect` / `✘ Connection error`** — only *these* two count
  as a genuine outage. Say so explicitly and ask the user how to proceed (fix
  the server, proceed with `claude-in-chrome`, or skip live browser testing)
  rather than silently substituting one tool for the other.

(Statuses per the Claude Code MCP docs as of this writing — reconfirm against
current documentation if `claude mcp list`'s output format has changed.)

Never silently swap in `claude-in-chrome` without first running this check and
telling the user which case applies — `claude-in-chrome` cannot handle native
`confirm()`/`alert()`/`prompt()` dialogs (they freeze the extension), which
HMIS billing/save flows trigger routinely, so the substitution carries real
risk the user should know about upfront.

## Workflow

1. **Confirm the target** with the user: which feature/page, which local
   deployment URL, and whether the code under test is already deployed.
2. **If not yet deployed**, rebuild and redeploy — see
   [§0a Rebuild and redeploy](../../../developer_docs/testing/playwright-e2e-workflow.md#0a-rebuild-and-redeploy-local-code-changes-before-testing).
   A redeploy invalidates the session, so this must happen *before* login.
3. **Login + department selection** — see
   [§1](../../../developer_docs/testing/playwright-e2e-workflow.md#1-login-and-department-selection).
   Then reach the page under test **through the menus only** — never by URL
   (see above).
4. **Drive the feature** using accessibility snapshots (`browser_snapshot`)
   to locate elements, real key events for PrimeFaces inputs (§3), and
   `browser_handle_dialog` for `confirm()` guards (§4). Wait on the expected
   result (`browser_wait_for`) rather than fixed sleeps (§5a). Watch for
   [§12](../../../developer_docs/testing/playwright-e2e-workflow.md#12-jsf-form-validation-blocks-navigation-buttons)
   (required-field validation blocking unrelated nav buttons),
   [§13](../../../developer_docs/testing/playwright-e2e-workflow.md#13-primefaces-pselectonemenu-is-not-a-native-select)
   (`p:selectOneMenu` click-option pattern), and
   [§14](../../../developer_docs/testing/playwright-e2e-workflow.md#14-non-ajax-search-buttons-can-timeout-on-click)
   (non-AJAX search clicks that time out but still succeed).
5. **If the DB lacks suitable test data, generate it through the app** — see
   [§15](../../../developer_docs/testing/playwright-e2e-workflow.md#15-always-generate-test-data--never-fall-back-to-code-only-verification).
   Never fall back to "code looks correct" as evidence.
   **Then leave it there.** Test records are not litter — do not clean up, and
   do not offer to, unless explicitly asked; just say what you created. A
   request to test is also never a request to test in production: if a
   verification step is about to write to a production environment, stop and
   confirm the target. See
   [§15a](../../../developer_docs/testing/playwright-e2e-workflow.md#15a-leave-test-data-where-it-is--never-clean-up-unasked).
6. **Verify in the database** — read-only `mysql` queries against the local
   DB per [§6](../../../developer_docs/testing/playwright-e2e-workflow.md#6-verify-against-the-database).
   Credentials come from `C:\Credentials\` (outside the repo).
7. **Capture evidence** into the project `tmp/` folder, then follow
   [§8](../../../developer_docs/testing/playwright-e2e-workflow.md#8-publishing-screenshot-evidence)
   for anything destined for the wiki/issue. Remove temp screenshots from the
   repo afterward.
8. If Playwright can't find a control, treat it as a product accessibility gap
   (§7) — fix the page, not the test.

## Required permissions

This skill needs, beyond the defaults:

- The full `mcp__playwright__*` tool set (browser automation).
- Maven `clean package` and Payara `asadmin redeploy`/`deploy`/`undeploy` for
  the local rebuild step (paths in `CLAUDE.md` § Local build tools).
- `mysql` read access to the local database for verification queries.

If any of these prompt for approval, the user's `settings.local.json` should
already allow them for this project — flag it if a prompt appears repeatedly.
