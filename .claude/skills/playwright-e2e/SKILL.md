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

## Workflow

1. **Confirm the target** with the user: which feature/page, which local
   deployment URL, and whether the code under test is already deployed.
2. **If not yet deployed**, rebuild and redeploy — see
   [§0a Rebuild and redeploy](../../../developer_docs/testing/playwright-e2e-workflow.md#0a-rebuild-and-redeploy-local-code-changes-before-testing).
   A redeploy invalidates the session, so this must happen *before* login.
3. **Login + department selection** — see
   [§1](../../../developer_docs/testing/playwright-e2e-workflow.md#1-login-and-department-selection).
   Never hit an inner page URL directly before department selection.
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
