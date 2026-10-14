# Playwright End-to-End Testing Workflow (HMIS)

How to drive the running HMIS app through the **Playwright MCP server** for
end-to-end verification of a feature (e.g. the pharmacy transfer
Request → Issue → Receive flow). These are operational learnings gathered while
testing against a local deployment; follow them to avoid the dead-ends that
waste a session.

> **Companion guide:** write pages to be *accessible-first* so Playwright can
> find elements at all — see
> [UI Handbook § Accessibility-first development](../ui/comprehensive-ui-guidelines.md#accessibility-first-development-required).
> This document is the *runtime* workflow; that section is the *authoring*
> rule.

## Contents

The workflow is §0–§8, plus the test-data rules in §15/§15a. Everything else is an
independent gotcha in a topic file. Find it by symptom in that file's heading list,
or by § number below, and read only that section.

- **[PrimeFaces widgets and AJAX in tests](playwright-e2e/primefaces-widgets.md)**: §9, §10, §12, §13, §18, §37, §50, §56, §68, §71, §72, §77, §78, §79, §80, §90, §94, §101, §105, §108, §109, §113, §116, §117, §123, §128, §129, §131, §137, §140
- **[Menus, navigation, sessions, hung pages](playwright-e2e/navigation-session.md)**: §11, §14, §16, §17, §19, §28, §32, §36, §46, §47, §52, §57, §62, §64, §89, §99, §106, §114b, §127, §136, §142
- **[Privileges, departments, ConfigOption and caches](playwright-e2e/privileges-config.md)**: §20, §24, §26, §35, §44, §48, §93, §96, §97, §107b, §114, §124, §135, §138
- **[Local Payara, asadmin, DB drift and seed data](playwright-e2e/environment-db.md)**: §27, §34, §38, §39, §41, §63, §73, §74, §87, §98, §102, §104, §111, §118, §119, §141, §142
- **[Screenshots, printing, exports, styling checks](playwright-e2e/evidence-printing.md)**: §43, §83, §84, §110, §115, §120, §133
- **[Code/authoring bugs that E2E exposes (JSF, EL, JPQL)](playwright-e2e/jsf-code-pitfalls.md)**: §25, §33, §42, §45, §51, §69, §76, §82, §86, §92, §100, §103, §112, §121, §122, §126, §132, §134
- **[Page- and module-specific quirks (pharmacy, inward, theatre, GRN, lab, reports)](playwright-e2e/module-pages.md)**: §21, §22, §23, §29, §30, §31, §40, §49, §53, §54, §55, §58, §59, §60, §61, §65, §66, §67, §70, §75, §81, §85, §88, §91, §95, §107, §125, §130

---

## 0. Before you start

- **Confirm the target environment with the developer.** Never assume which
  deployment or database a local URL points at. Credentials live **outside** the
  repo (`C:\Credentials\`) — never paste them into docs, code, or commit
  messages.
- The app must already be deployed and running. Playwright does **not** build or
  deploy — it only drives a browser against a running instance.
- Take screenshots at every meaningful stage into the project `tmp/` folder
  (not the system temp). They double as wiki material later.

---

## 0a. Rebuild and redeploy local code changes before testing

If the change under test isn't deployed yet, rebuild and redeploy to the local
Payara instance first (see [Local build tools](../../CLAUDE.md) for tool
locations). **This section is local dev only** — it is the `asadmin`
build/deploy loop the `playwright-e2e` skill already mandates on a developer
laptop. It does **not** apply to shared/staging/production Payara, where the
"no manual/root deployment; everything through CI/CD" rule in `CLAUDE.md`
still governs.

```powershell
# Paths vary per machine — check C:\Credentials\Credentials.txt for your local values
$env:JAVA_HOME="<path-to-jdk>"
& "<path-to-mvn.cmd>" clean package -DskipTests
& "<path-to-asadmin.bat>" [--port <admin-port>] redeploy --name rh "<project-root>\target\rh-3.0.0.war"
```

- `clean` is required when switching branches or after structural changes
  (new/renamed/deleted classes, resources); a plain `compile`/`package` can
  leave stale `.class` files in `target/`.
- A redeploy invalidates the current session (see §1) — log in again
  afterward.
- Watch `<payara-install>\glassfish\domains\domain1\logs\server.log` for deployment errors
  before starting the browser flow.
- **`--name` must match the actual deployed app name.** `asadmin list-applications`
  first — on some machines it is `rh-3.0.0`, not `rh`. A `redeploy` with the
  wrong `--name` fails with `Application with name [...] is not deployed`.
- **Payara must run on JDK 11.** If Payara was started with JDK 21 on `PATH`,
  deployment fails with `Unsupported class file major version 65` (65 = Java 21).
  Fix: `asadmin stop-domain`, then set **both** `$env:JAVA_HOME` and
  `$env:AS_JAVA` to the JDK 11 path before `start-domain`. A failed `deploy`
  (as opposed to `redeploy`) also *removes* the app, so the next `redeploy`
  then fails with "not deployed" — recover with a plain
  `deploy --name <name-from-list-applications> --contextroot <its-context-root> <war>`
  (the name/context root you confirmed above, not a hardcoded guess).
- **Running `asadmin` from Git Bash: set `MSYS_NO_PATHCONV=1`.** MSYS rewrites any
  argument that looks like a Unix path, so `--contextroot /rh` is handed to Payara
  as `D:/Program Files/Git/rh`. The deploy then fails deep inside JSF parsing —
  `Unable to parse document 'jndi:/server/D:/Program%20Files/Git/rh/WEB-INF/faces-config.xml'`
  — which reads like a broken WAR and is nothing of the sort. Worse, that failed
  `redeploy` leaves the app *undeployed*, so the retry fails with "not deployed" and
  you have to `deploy` fresh. Prefix the command:
  `MSYS_NO_PATHCONV=1 asadmin deploy --name rh --contextroot /rh <war>`. PowerShell
  is unaffected.

---

## 1. Login and department selection

The HMIS login + landing flow has a fixed shape:

1. `browser_navigate` to the deployment URL.
2. Fill username/password and submit. Use real key events (see §3) if a plain
   fill doesn't register.
3. After login the app lands on an **index/landing** page. The main menu bar
   (Pharmacy, Inward, etc.) **only appears on inner pages**, not on the
   department-selection screen.
4. **Select a department** before doing anything else. The app remembers the
   *last* department, so a fresh login often pre-fills it — still click through
   the **Select Department** screen to reach an inner page.

**Do not navigate directly to an inner page URL before selecting a department.**
`sessionController.department` is null until department selection completes.
The template wraps `<ez:menu />` in `rendered="#{sessionController.department ne null}"`,
so the entire menu — including the notification bell, websocket, and remoteCommand —
is absent from the page. Any Playwright check for these components will fail silently.
Always go through the department-selection screen first.

**The department gate is a hard prerequisite for every session, not a one-time
step to satisfy and forget.** After clicking **Select** on the department
screen, the app redirects to `home.xhtml` — that redirect (not a specific
target page) is the real confirmation the gate passed. Only after landing on
`home.xhtml` is it safe to `browser_navigate` straight to a specific report/page
URL. Prefer clicking through the actual menu (Pharmacy Analytics → tab →
Generate Report, etc.) over guessing/typing report URLs directly wherever a
menu path is reasonably discoverable — direct URL navigation is a fallback for
pages with no simple menu path, not the default technique, since several pages
(e.g. Inward final-bill pages, see §17 below) rely on session-bean state that a
URL alone won't set up correctly even post-department-selection.

**A redeploy invalidates the session.** Every time the WAR is redeployed you are
logged out and must log in again. Plan test runs so you are not mid-flow when a
deploy lands.

### Switching departments mid-workflow

Some flows (transfers) require acting as two different departments. To switch:
**Logout** (top-right of the menu bar) → log back in → reselect the correct
department on the Select Department screen. There is no in-session department
switch for these flows.

---

## 2. Navigating menus

### 🚨 NEVER navigate by typing a page URL

**Real users never reach an inner page by its URL.** Many terminals are kiosks
with no address bar; the rest reach every page through the menus. If a page has
no menu path to it, that page is not reachable in production and the correct
finding is "this page has no navigation path", not "this page is broken".

**Only ever type a URL for the application root / login page.** Everything after
that must be reached by clicking through the menus, exactly as a user would.

This is not a style preference. It changes what the page does:

- **Session-scoped controllers are populated by the navigation method, not by
  the page.** `sessionController.toManageDepartmentPreferences()`,
  `inwardSearch.toSearchServiceBill()` and friends set the entity the page then
  renders. Skipping the navigation method leaves that entity null, or leaves a
  lazily-created transient placeholder in its place.
- **What you observe afterwards is therefore not the real behaviour.** A
  URL-loaded page can 500 (`Target Unreachable, 'null' returned null`), render
  blank, render against an empty entity, or run pathologically slowly — none of
  which any user can ever hit.
- **Getters that lazily instantiate are the usual trap.** e.g.
  `InwardSearch.getBill()` returns `new BilledBill()` when nothing is selected,
  so a URL-loaded page renders every print component against an id-less entity
  and every `WHERE bill = :bl` query against a transient parameter.

Two live examples of this producing a false bug report:

| Page | Symptom when opened by URL | Reality via the menus |
|---|---|---|
| `inward_reprint_bill_service.xhtml` | appeared to hang indefinitely, JVM into the GB range, no exception logged | loads in 0.7-5.3 s (issue #23519, retracted) |
| `admin_mange_department_preferences.xhtml` | HTTP 500, `Target Unreachable, 'null' returned null` | works normally |

**Before testing a page, establish its menu path first** and record it in the
issue/PR, in the form the user can follow:

> Menu → Inpatient → Search → Service Bill → set From Date → Search Bill
> → click Bill No → Return

If you cannot find a menu path, search `menu.xhtml` for the page name and check
the privileges gating it — see §20. Do **not** fall back to the URL to "get on
with the test".

### Menu mechanics

- The Pharmacy top menu is a PrimeFaces menubar. **Hover** the parent
  (`smPharmacy`) to expand it, then **click** the submenu link
  (e.g. `a:has-text("Disbursement")`). A direct click on the parent without the
  hover can fail to open the submenu.
- **Most transfer/disbursement lists are date-filtered and do not auto-load.**
  After opening a list (Approve Requests, Issue for Requests, Receive Issued
  Items) you must click **Search** (adjusting the date range if needed) before
  any rows appear. An empty list usually means "Search not yet clicked", not "no
  data".

---

## 3. Committing PrimeFaces inputs (the #1 gotcha)

`browser_fill_form` / `fill()` sets the DOM value but **does not fire the
key/blur events** PrimeFaces relies on to commit a value. Symptoms: an
autocomplete shows text but no selection is made; a quantity field looks filled
but arrives as empty/`0` on the server.

### ⚠️ `p:inputText` with `p:ajax event="blur"` — cannot be automated

**None of the following work** to commit a `p:inputText` value server-side via
`p:ajax event="blur"`: jQuery `.trigger('blur')`, `.triggerHandler('blur')`,
native `el.onblur()`, `dispatchEvent(new FocusEvent('blur'))`, or even
Playwright's real `Tab` key press. JSF inspects the event source and rejects
synthetic/programmatic events.

The **only known fix** is adding `async="true"` to the `<p:ajax>` tag:
```xml
<p:ajax event="blur" async="true" process="@this" update="..." />
```
This is a server-side change requiring rebuild. If a page has `p:inputText`
fields with `p:ajax event="blur"` that must be filled, either add `async="true"`
first, or have a human enter those values manually.

### `p:autoComplete` — type slowly, Enter to select ✅

Two proven patterns, depending on how specific your query is:

**Pattern 1 — specific query, just press Enter (1 snapshot):**
Use when your query narrows to the desired item as the first suggestion:
```text
browser_click on autocomplete textbox
browser_press_key Control+a
browser_press_key Backspace
browser_type "Paracetamol 500" slowly:true     ← character by character
browser_wait_for text "Paracetamol 500Mg Tablet"
browser_press_key Enter                         ← selects first match, no snapshot needed
```

⚠️ **Only when the autocomplete's own form declares no `p:defaultCommand`.**
`p:defaultCommand` is a form-level Enter target, so what matters is the form the
autocomplete sits in, not the page. Where that form declares one, the same Enter
also fires it, and the action runs before you click its button — see
[§129](playwright-e2e/primefaces-widgets.md#129-pressing-enter-to-accept-a-loaded-autocomplete-suggestion-also-fires-the-pages-pdefaultcommand--the-action-runs-before-you-click-its-button).
Use Pattern 2 there.

**Pattern 2 — generic query, click from snapshot (2 snapshots):**
Use when the desired item is not the first suggestion and you need to pick:
```text
browser_click → Ctrl+A → Backspace → browser_type slowly →
browser_wait_for text → browser_snapshot →
browser_click on suggestion ref
```

**Never use `browser_fill_form` or `fill()` for autocomplete** — they set
the DOM value but don't fire the keyup events that PrimeFaces needs to
query the server for suggestions.
4. browser_snapshot — find the suggestion ref in the listbox/table
5. browser_click the suggestion item
Autocomplete items are in a `.ui-autocomplete-panel` that contains a `<table>`
(not `<ul>/<li>`). Click the `<tr>` row directly. Do NOT set the hidden input
value — the `itemSelect` AJAX must fire for the server to see the selection.

### `p:selectOneMenu` — click-option pattern ✅

PrimeFaces dropdowns are not native `<select>` elements. `browser_select_option`
fails. Instead: click the combobox → snapshot → click the option from the
dropdown panel.

### `p:datePicker` / `p:calendar` — keyboard or click ✅

Type the date string directly or click to open the calendar popup and select.

**JS-set values are silently discarded** (found on `cost_of_goods_sold.xhtml`,
issue #22011): setting `input.value` via `page.evaluate` + dispatching
`input`/`change` events looks committed in the DOM, and Playwright's `fill()`
has the same problem — but on submit the widget re-serializes its own internal
date, so the report runs with the OLD dates and no error is shown. The only
reliable pattern is real key events: click the input → `Ctrl+A` →
`pressSequentially` the date string → `Escape` (closes the overlay without
resetting the typed value). Verify with a DOM read *after* pressing Escape,
then submit — and because the DOM can look right while the widget still
serializes its old internal date, always confirm the intended dates in the
**result** too (e.g. report rows fall inside the requested window, or the
server-side query used the right range) before trusting the run.

**Widget API alternative ✅** (verified on `inward_search_deposit.xhtml`, issue
#23764): calling the PrimeFaces widget's own `setDate()` updates its internal
date, so the submit uses it. Find the widget by client-id suffix when there is
no `widgetVar`:
`Object.values(PrimeFaces.widgets).find(w => w.id && w.id.endsWith('fromDate')).setDate(new Date(2026, 8, 1))`.
Still confirm the dates in the result rows.

### Always add `widgetVar`

Every `p:inputText`, `p:autoComplete`, `p:calendar`, and `p:selectOneMenu`
that a Playwright test needs to interact with MUST carry a `widgetVar`
attribute. It costs nothing and makes elements identifiable across sessions.

### `p:inputText` driving a client-side recalculation (e.g. "Difference") ✅

Some totals fields (like GRN costing's "Invoice Total" vs. "Difference") are
recalculated by a client-side script bound to a plain blur event, not a
`p:ajax`. A single `fill()` or even a plain `Tab` key press after typing can
leave the dependent field stale, so a validation check reading that stale
value (e.g. "The invoice does not match..! Check again") fires even though
the number you typed is correct. Fix: click into the field, `Control+a` to
select existing content, type the new value with `slowly: true`
(`pressSequentially`), then click a neutral, non-interactive element elsewhere
on the page (a heading works well) to force a real blur. Re-check the
dependent field's value in the next snapshot before proceeding — don't assume
it recalculated just because no error was shown yet.

---

## 4. Confirmations and double-click protection

- Settle/Issue/Receive buttons use a JS `confirm()` guard
  (`onclick="if (!confirm('…')) return false;"`). Playwright must accept the
  dialog: register a handler with `browser_handle_dialog` (accept) or override
  `window.confirm` to return `true` before clicking.
- **To test double-click protection**, override `window.confirm` to always
  return true, then fire `btn.click()` **twice in the same tick** on the
  non-AJAX settle button (e.g. `btnSettleReceive`). A correct implementation
  produces exactly one bill with no duplicate items.
- **Do not register `page.once('dialog', ...)` inside `browser_run_code_unsafe`.**
  The MCP server tracks dialogs itself; a script-registered handler accepts the
  dialog but leaves the harness's modal state stuck — subsequent tool calls fail
  with "does not handle the modal state" while `browser_handle_dialog` reports
  "already handled". Recover with a `browser_snapshot` (clears the stale modal
  state). Prefer overriding `window.confirm = () => true` via `page.evaluate`
  *before* the click; note the override is lost on every full (non-AJAX) page
  reload and must be re-applied per page instance.

---

## 5. Required fields block non-AJAX actions

Non-AJAX actions (`ajax="false"`) run a full form submit, so JSF validation
fires first. If a **required** field is empty (e.g. the transfer **Comment**
field), the submit is rejected and the action — including an unrelated
`remove(row)` button on the same form — silently does nothing. Fill required
fields before exercising any non-AJAX button on the page.

---

## 5a. Waiting for AJAX without hard timeouts

- Prefer `browser_snapshot` (accessibility tree) over screenshots for finding
  and confirming elements — it's far cheaper in tokens and is what the agent
  actually reasons over.
- After an AJAX action (PrimeFaces `p:ajax`/`update`), use `browser_wait_for`
  on the expected resulting text/element rather than a fixed `sleep`. Fall
  back to the explicit waits in §3 (slow type + ~1–1.5 s) only for the known
  PrimeFaces commit-timing gotchas, since those are races against a keyup
  handler that no DOM state change reliably signals.
- `browser_network_requests` is useful to confirm a `javax.faces.partial.ajax`
  POST actually fired (and what it returned) when a UI update silently does
  nothing — cheaper than guessing at another `wait_for`.

---

## 5b. Pending-list pages may need an explicit "Refresh" click

Some "pending items" list pages (e.g.
`pharmacy_return_from_ward_receive_list.xhtml`) populate their backing list
via an action method bound to a "Refresh" button, not via a `viewAction` on
direct GET. Navigating straight to the page (or returning to it via a
redirect) can show "No pending ... " even though matching rows exist in the
DB. If a pending list looks empty right after navigation, click "Refresh"
before concluding the underlying JPQL is wrong.

---

## 6. Verify against the database

After the UI flow, confirm correctness directly in the DB (the local copy, with
credentials from `C:\Credentials\`). For pharmacy transfers the key checks are:

- **No duplicate bill items:** group `billitem` by `ITEM_ID + ITEMBATCH_ID`;
  every combination should appear exactly once. Confirm the
  `BillItem : PharmaceuticalBillItem : BillItemFinanceDetails` counts are 1:1:1.
- **Exactly one downstream bill** per source (one receive bill per issue, etc.).
- **Stock reconciles end-to-end:** supplying dept ↓ → carrying staff ↑ → on
  receive, staff → 0 and requesting dept ↑ by the received qty, with no
  over-/under-movement.
- Prefer **JPQL-shaped** reasoning, but ad-hoc read-only SQL is fine for
  verification. Clean up any temp `.sql` files from `tmp/` afterward.

---

## 7. When Playwright can't find an element — fix the page, not the test

If Playwright cannot identify a control from the accessibility snapshot, that is
a **product accessibility gap**, not a test problem. Improve the page (stable
`id`, interpolated `title`, accessible name) per the UI handbook, then continue.
Accessibility work done this way benefits real assistive-technology users too.

---

## 8. Publishing screenshot evidence

Use screenshots as durable evidence only after checking that they do not expose
patient details, credentials, or other sensitive data. Prefer capturing
configuration screens, reports with non-sensitive rows, or cropped states that
show the fixed control without private information.

1. Capture verification screenshots with `browser_take_screenshot` into the
   project `tmp/` folder.
2. For user-facing documentation, copy final screenshots into the sibling wiki
   repo under `../hmis.wiki/images/`.
3. **Embed each image in the wiki page for the feature** — this is the step
   that is most often skipped, and skipping it is why the wiki currently holds
   ~600 images but only ~55 pages reference any. An image nobody links to
   documents nothing.

   Find the page by feature name, screen title, or menu path — pages are named
   after the user-facing screen (e.g. `Inpatient-Nursing-Discharge.md`):

   ```bash
   cd ../hmis.wiki
   ls *.md | grep -iE "<feature|module keyword>"
   grep -ril "<feature name>" *.md | head
   ```

   Reference the image with a relative path. Markdown alt text is *not* a
   rendered caption, so add a visible italic line beneath it — readers
   skimming a long page rely on that, and screen readers use the alt text:

   ```markdown
   ![Nursing discharge blocked by pending pharmacy items](images/23222-fixed-discharge-blocked.png)

   *Nursing discharge blocked: the pending pharmacy items are listed and Confirm stays disabled.*
   ```

   **Replace outdated screenshots rather than accumulating them.** If the page
   already shows a screen your change altered — or one that no longer matches
   the current UI at all — swap it out. Two contradictory screenshots of the
   same screen are worse than one stale one.

   If no page covers the feature: create one when the change is user-visible
   (screen, workflow, report, setting), following a neighbouring page in the
   same module. Skip it when the change is invisible to end users (internal
   query fix, refactor, build change) — there the screenshot is evidence for
   the issue/PR only.
4. Commit and push the wiki immediately from `../hmis.wiki` — images and page
   edits together.
5. To embed the same image in a GitHub issue or PR comment, use the raw wiki
   URL (and link the page itself as
   `https://github.com/hmislk/hmis/wiki/<Page-Name>`, so the reader can see the
   feature documented in context rather than a floating screenshot):

```text
https://raw.githubusercontent.com/wiki/hmislk/hmis/images/example_name.png
```

Example issue comment:

```powershell
gh issue comment 21364 --repo hmislk/hmis --body "Verified with Playwright.

![Verification screenshot](https://raw.githubusercontent.com/wiki/hmislk/hmis/images/example_name.png)"
```

Remove temporary screenshots from the main repository after copying the durable
ones into the wiki so they are not accidentally committed with application code.

### 8a. Bug fixes: pair "before" and "after" evidence

When the underlying issue is a bug report **and reproducing it required a
live check** (the root cause wasn't already confirmed by reading code),
capture evidence at **two** points instead of one, and publish them together
as a comparison rather than as a single final-state screenshot:

1. **Before** — during reproduction (before any fix is written), capture the
   broken state: a screenshot for UI bugs, or the raw request/response for
   API-only bugs. This is also the evidence that the bug is real if the
   report turns out to be stale — save it even when the answer turns out to
   be "does not reproduce" (record that it didn't reproduce under the tested
   environment/data/inputs — that is not proof the bug is absent).
2. **After** — once the fix is deployed, capture the same view/state again
   (or replay the same request, for API-only bugs) showing correct behavior.
3. Redact patient identifiers, credentials, tokens, cookies, and other
   sensitive fields from any API request/response snippet before it leaves
   `tmp/` or is published — the same sanitization rule §8 applies to
   screenshots.
4. Place both images (or both sanitized response snippets) in the same issue
   comment / PR description, labeled "Before" and "After", so a reviewer can
   see the fix without redeploying locally.

If the root cause was already confirmed by reading code (no live
reproduction needed), there is no "before" evidence — publish only the
post-fix confirmation, without implying a comparison.

---

## 15. Always generate test data — never fall back to code-only verification

If the database has no suitable records, **create them** through the UI.
For returns, create a purchase first (Direct Purchase is simplest), then
return against it. For issues, create a purchase → issue → return. The
For qty fields with `async="true"` blur handlers, use slow `browser_type` + Tab key to commit — do not rely on jQuery-blur (see §3).

Never close a QA session with "code looks correct" as the only evidence.

If the app genuinely can't get you to the required state (the path is blocked
by unrelated broken data, or needs a second user session you lack credentials
for), **write the local database directly** — `INSERT`/`UPDATE` against a
local DB is fine, and a local DB is disposable: don't revert test data or
treat local rows as precious. Going through the app stays the preference
because it exercises the same validation and business logic the fix has to
survive, so a hand-built fixture that bypasses that validation proves less —
weigh that when it matters, and note in the PR how the data was made.

**This applies to local databases only.** A remote or SSH-tunnelled database
(production, staging) is read-only, always. No development environment is ever
set up on a hosting server, so "localhost, no tunnel" is a reliable proxy for
"safe to modify freely".

---

## 15a. Leave test data where it is — never clean up unasked

Test records you create while verifying a fix — patients, admissions, bills,
surgeries, config rows — **stay**. Do not offer to remove them, and do not
remove them on your own initiative after a test passes.

A testing environment exists to hold test data. Records left behind are a
worked example the next person can pick up, and deleting them costs the setup
effort again. Cancelling a bill or discharging a test patient to "tidy up" is
also not free: it writes further rows (contra-bills, room-change history) and
can leave the data in a stranger state than just leaving it alone.

Clean up **only when explicitly asked**, and only what was asked for.

### The environment tells you whether you should be writing at all

| Environment | Create test data? | Clean up afterwards? |
|---|---|---|
| Local (`localhost`, no tunnel) | Yes, freely | No — and the DB is disposable anyway (§15) |
| Hospital test/staging box (e.g. `rhLocal`) | Yes | No, unless explicitly asked |
| Production on Azure | You should not be testing here | n/a — if you are about to write, stop and ask |

Production is the real signal: a request to *test* a workflow is essentially
never a request to test it in production. If a verification step is about to
write to a production environment, that is a sign the target is wrong — stop
and confirm which environment is meant, rather than proceeding carefully.

Say what you created, so the user can decide:

> Test data created on rhLocal: patient ZZ TEST THEATRE PATIENT, admission
> Day Case/3, settled service bill RHDDC006INWSER/71 (5.00).

That sentence is the whole obligation. Naming records with an obvious test
prefix (`ZZ TEST ...`) is what makes them harmless to leave.

---

## Quick checklist

- [ ] Confirmed environment + URL with the developer; credentials kept out of the repo.
- [ ] Logged in, selected a department, reached an inner page (menu visible).
- [ ] Checked for stale department pre-selection — the app remembers the last department; always re-select explicitly.
- [ ] Clicked **Search** on every date-filtered list before expecting rows.
- [ ] Used real key events (slow type + wait) for autocompletes; for qty fields with blur AJAX, used slow type + Tab (not jQuery-blur — see §3).
- [ ] Handled `confirm()` dialogs; tested double-click on settle buttons.
- [ ] Distinguished a native `confirm()` (use `browser_handle_dialog`) from a PrimeFaces `p:confirm` dialog (click its own Yes button) — and did not treat an `offsetParent`-based visibility probe as proof a `p:confirm` dialog is closed (§109).
- [ ] Filled required fields before non-AJAX actions.
- [ ] Checked that navigation buttons are not blocked by JSF validation on required fields in the same form.
- [ ] Verified stock + bill-item integrity in the DB; cleaned up temp files.
- [ ] Filed/fixed any accessibility gap that blocked the test.
- [ ] Published only non-sensitive screenshot evidence and removed temporary files.
- [ ] For canvas-based widgets (vis-timeline etc.), used `page.$`/bounding-box
      clicks and closed dialogs via `.ui-dialog-titlebar-close`, not `Escape`.
- [ ] Re-logged in and re-selected department after any redeploy before continuing.
- [ ] If test data is unavailable, **generated it through the app** (create purchase → return → etc.) rather than falling back to code-only checks.
- [ ] Asserted the outcome of every click (URL/destination element); for icon-only datatable row buttons that silently no-op, fell back to `$(el).trigger('click')`.
- [ ] Resolved any local "Unknown column" 500 via the app's own `/faces/mf.xhtml` migration page, not hand-written DDL.
- [ ] Treated a greyed-out admin **Add New**/**Edit** as "wrong department for that privilege row" (§20) before assuming the page is broken.
- [ ] When a Save did nothing with a clean `server.log` and an unchanged DB, read the form's `<p:messages>` **by id** and grepped the page for `required="true"` (§91) before hunting the controller.
- [ ] For a guard fix: asserted the **action actually executed** (expected message in the response) before treating unchanged DB state as proof — a JSF-disabled button skips its action entirely — and ran the negative test (clean record still succeeds), reverting it through the app.
- [ ] For a menu item nested three levels deep, fired the anchor's own `onclick` (which submits the menu form, so the navigation method still runs) instead of falling back to typing the page URL — scoping the lookup to its own submenu, since labels repeat within one menu.
- [ ] Before writing "did not reproduce", checked every `getBooleanValueByKey(...)` branch in the code path and flipped any option whose local value differs from the reporter's likely setting (§107) — and, when verifying a fix, exercised **both** settings of any option gating the changed code.
- [ ] For any admission flow: confirmed the local DB actually has a vacant room (`completeRoom` returns suggestions); if not, freed some by SQL (§111) before concluding the Room autocomplete is broken.
- [ ] Before trying to reproduce a same-session state-change race (item A staged, then a dependency of A is invalidated by a legitimate app action before A is submitted), checked whether a `@SessionScoped` controller's already-held entity reference would even observe the change (§96) rather than assuming any in-app mutation propagates live.
