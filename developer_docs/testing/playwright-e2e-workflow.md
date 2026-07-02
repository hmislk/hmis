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
locations):

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

### Always add `widgetVar`

Every `p:inputText`, `p:autoComplete`, `p:calendar`, and `p:selectOneMenu`
that a Playwright test needs to interact with MUST carry a `widgetVar`
attribute. It costs nothing and makes elements identifiable across sessions.

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
3. Reference wiki images in markdown as `images/example_name.png`.
4. Commit and push the wiki immediately from `../hmis.wiki`.
5. To embed the same image in a GitHub issue or PR comment, use the raw wiki
   URL:

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

---

## 9. Interacting with non-accessible canvas widgets (e.g. `p:timeline` / vis-timeline)

`p:timeline` renders into an HTML canvas-like DOM (vis-timeline `div`s with no
useful accessibility tree), so `browser_click`/`browser_snapshot` `ref=`
targeting won't find individual events. Use `browser_run_code_unsafe` instead:

```js
async (page) => {
  const el = await page.$('.vis-item.mar-given'); // or .vis-item.timeline-active
  const box = await el.boundingBox();
  await page.mouse.click(box.x + box.width / 2, box.y + box.height / 2);
  await page.waitForTimeout(1500);
  const dlg = await page.$('#formTimeline\\:panelAdministrationDetail');
  return { style: await dlg.getAttribute('style'), text: await dlg.innerText() };
}
```

To enumerate all items first (positions shift after dialogs open/close and
change page layout):

```js
await page.$$eval('.vis-item', els =>
  els.map(e => ({ cls: e.className, text: e.innerText, box: e.getBoundingClientRect() })));
```

**Closing a `p:dialog` after inspection**: pressing `Escape` does **not**
reliably close a PrimeFaces modal `p:dialog` in a scripted session. Click the
titlebar close button explicitly:

```js
await page.click('#formTimeline\\:panelAdministrationDetail .ui-dialog-titlebar-close');
```

If you click a timeline item while a previous dialog's overlay is still up, the
click lands on the dialog/overlay (not the timeline) and silently produces no
AJAX request — always close the prior dialog first and re-query item positions.

## 10. `browser_click` / `browser_type` parameter name

These tools take `target` (an element reference like `e123` from the latest
`browser_snapshot`, or a selector) plus a human-readable `element` description —
**not** `ref`. If a call fails with "expected string, received undefined at
target", the tool schema may not be loaded yet; reload it via
`ToolSearch` (`query: "browser_type playwright"`) and retry with `target`.

## 11. Session is lost on redeploy and on stale snapshots

- **A redeploy invalidates the session** (already noted in §1) — re-login and
  re-select department before continuing.
- If a click navigates somewhere unexpected (e.g. `about:blank` or a
  `TypeError: Cannot read properties of undefined (reading 'url')`), the page
  state and your last `browser_snapshot` have diverged. Re-navigate to the app
  root (`/rh`), log in again, and re-take a snapshot before continuing — don't
  keep issuing actions against stale `ref=` values.

---

## 12. JSF form validation blocks navigation buttons

On pages where the *same* `h:form` contains both a data-entry section (with
required fields) and navigation buttons (e.g. "List GRNs"), clicking a
navigation button can unexpectedly trigger form validation. If a required
`p:selectOneMenu` (like Payment Method) is empty, JSF rejects the entire
form submission and the navigation action never fires — the user sees a silent
"Please select a payment method" error instead of the expected dialog/page.

**Workaround:** Navigate directly to the target page URL instead of clicking
the button, or fill all required fields first. Better: ensure the navigation
button uses `process="@this"` or is in a separate form so it doesn't submit
the data-entry fields.

## 13. PrimeFaces `p:selectOneMenu` is not a native `<select>`

`browser_select_option` fails with "Element is not a <select> element" on
PrimeFaces dropdowns. Use the click-option pattern instead:
1. Click the dropdown label/combobox to expand the panel
2. `browser_snapshot` to find the option ref in the `listbox`
3. Click the option by its `ref=` from the snapshot
4. The value commits on selection; no extra "Select" click is needed

## 14. Non-AJAX search buttons can timeout on click

When a JSF search button triggers a full page reload (non-AJAX, `ajax="false"`),
`browser_click` may time out with "waiting for scheduled navigations to finish"
if the response is slow. The click usually succeeds — use `browser_snapshot`
after the timeout to check the new page state rather than assuming the action
failed. If stuck, `browser_navigate` directly to the page URL to recover.

## 15. Always generate test data — never fall back to code-only verification

If the database has no suitable records, **create them** through the UI.
For returns, create a purchase first (Direct Purchase is simplest), then
return against it. For issues, create a purchase → issue → return. The
For qty fields with `async="true"` blur handlers, use slow `browser_type` + Tab key to commit — do not rely on jQuery-blur (see §3).

Never close a QA session with "code looks correct" as the only evidence.
If you cannot generate data through the app, stop and discuss alternatives
with the developer before falling back.

## 16. List pages that filter by `toDepartment = session department`

Several ward/pharmacy list pages (e.g. `pharmacy_return_from_ward_receive_list.xhtml`
via `PharmacyReturnFromWardReceiveController.loadPendingReturnBills()`, and
`ward_pharmacy_bht_issue_request_list_for_issue.xhtml` via
`SearchController.createInwardBHTForIssueTable()`) filter bills by
`b.toDepartment = sessionController.getDepartment()` — i.e. only bills whose
**pharmacy/target department** matches the department currently selected in
the session. A bill created with a different target department (e.g. a BHT
issue request where "Pharmacy Dept" was set to "Temp-Inward") will show
"No records found." when searched from "Inward" or "Main Pharmacy", even with
"Search All" and a wide date range — this is filtering, not a bug. Either pick
the matching department when creating the test record, or switch the session
to the bill's `toDepartment` before searching for it.

## 17. Switching session department mid-test (no redeploy)

To test as a different department without redeploying: navigate to
`/rh/faces/logout.xhtml`, then `/rh/faces/index1.xhtml`, click **Login**
(credentials are pre-filled after a recent login), then use the "Select
Department" combobox + **Select** button to pick the new department. This
re-runs `SessionController.fillUserPrivileges()` for that department, so
privilege-gated buttons render correctly without a full app redeploy.

## 18. `p:datePicker` — changing the date via the calendar grid

To change a `p:datePicker` (with `timeInput="true"`) to a different day:
click the input to open the "Choose Date" dialog, then click the target day
cell in the calendar `grid` (refs like `gridcell "June 15"`). Typing into the
input directly is unreliable. After picking the date, the calendar overlay
can intercept subsequent clicks ("subtree intercepts pointer events") — click
a neutral element on the page first (e.g. a heading) to dismiss the overlay
before clicking Search.

## 19. Some `confirm()`-guarded `type="submit"` buttons never reach the server — prefer existing data

On `pharmacy/pharmacy_bill_retail_sale_native.xhtml` ("Pharmacy Retail Sale"), the
**Settle** button is a PrimeFaces `p:commandButton` with `ajax="false"` and a
`confirm(...)` guard. Across several approaches — real `browser_click` +
`browser_handle_dialog`, overriding `window.confirm` before clicking, and dispatching
a synthetic click via `browser_evaluate` — the click always ran the `onclick` handler
(confirm dialog appeared/was accepted each time) but the browser never actually
submitted the form: no new request appeared in `browser_network_requests`, and the
server log had no corresponding entries. Root cause not identified (possibly a
`Tendered` client-side balance check, or a JS handler outside the visible `onclick`
attribute, silently calling `preventDefault()`).

**Workaround used:** rather than fighting this page, the existing `pharmacy_search_*`
pages were verified against **pre-existing historical demo data** (CareCode Model
Hospital ships with substantial seeded pharmacy sale history) instead of creating a
fresh bill through this specific page. If a fresh bill genuinely must be created for a
test, try the token-based "Sale for Cashier" flow instead — its item-add/quantity
inputs worked fine in this session, only the retail-native page's Settle button was
unreachable.

## Quick checklist

- [ ] Confirmed environment + URL with the developer; credentials kept out of the repo.
- [ ] Logged in, selected a department, reached an inner page (menu visible).
- [ ] Checked for stale department pre-selection — the app remembers the last department; always re-select explicitly.
- [ ] Clicked **Search** on every date-filtered list before expecting rows.
- [ ] Used real key events (slow type + wait) for autocompletes; for qty fields with blur AJAX, used slow type + Tab (not jQuery-blur — see §3).
- [ ] Handled `confirm()` dialogs; tested double-click on settle buttons.
- [ ] Filled required fields before non-AJAX actions.
- [ ] Checked that navigation buttons are not blocked by JSF validation on required fields in the same form.
- [ ] Verified stock + bill-item integrity in the DB; cleaned up temp files.
- [ ] Filed/fixed any accessibility gap that blocked the test.
- [ ] Published only non-sensitive screenshot evidence and removed temporary files.
- [ ] For canvas-based widgets (vis-timeline etc.), used `page.$`/bounding-box
      clicks and closed dialogs via `.ui-dialog-titlebar-close`, not `Escape`.
- [ ] Re-logged in and re-selected department after any redeploy before continuing.
- [ ] If test data is unavailable, **generated it through the app** (create purchase → return → etc.) rather than falling back to code-only checks.
