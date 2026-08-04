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

## 20. A privilege-gated button that never renders may be a missing DB row, not a session issue

If a `rendered="#{webUserController.hasPrivilege('SomePrivilege')}"` button never
appears even after the §17 logout/relogin-and-reselect-department trick, the
`webuserprivilege` row for that (user, department, privilege) triple may simply not
exist in the local seed data — no amount of re-login fixes a privilege that was never
granted for that department. Check first:

```sql
SELECT ID, PRIVILEGE, DEPARTMENT_ID, RETIRED
FROM webuserprivilege
WHERE WEBUSER_ID = <id> AND PRIVILEGE = 'SomePrivilege';
```

If the row for the target department is absent, insert it (`RETIRED = 0`) for that
`WEBUSER_ID`/`DEPARTMENT_ID`, then follow §17 (logout → login → reselect department)
to force `SessionController.fillUserPrivileges()` to re-read it — the privilege list is
cached per session at login and won't pick up a new row otherwise. This came up testing
`BhtSummeryController.settle()` (`InwardSettleFinalBill`), where the local `buddhika`
user had the privilege for `Store`/`Main Pharmacy` departments but not `Inward`.

**`WebUser.department` is not a fixed "home department" — `SessionController.selectDepartment()`
overwrites and persists it (`loggedUser.setDepartment(department); getFacede().edit(loggedUser)`)
every time the department-selection screen is submitted, which is why it pre-fills with
whatever was picked last time.** The catch for privilege testing:
`SessionController.getUserPrivileges()` calls
`fillUserPrivileges(getLoggedUser(), getLoggedUser().getDepartment(), false)` — by the time
this runs, `getLoggedUser().getDepartment()` already equals the department just selected for
*this* login, and `deptIsNull=false` means a `DEPARTMENT_ID IS NULL` privilege row is **never**
matched, no matter which department that is. Query `SELECT DEPARTMENT_ID FROM webuser WHERE
ID=<id>` *after* selecting the department you're about to test with, and insert the privilege
row with that exact `DEPARTMENT_ID` — a NULL-department row silently does nothing, even after
a full logout/login cycle.

## 21. Inward "Add Services" item picker — the Filter box does not load other departments' items

On `inward/inward_bill_service.xhtml` (and the surgery equivalent) the item selector shows
a **department button row** (OPD, ETU, Inward, MRI, …) above an "Investigation or Service"
list. That list is scoped to the **currently selected department button**, defaulting to the
first (usually OPD). The "Filter" textbox only narrows the *already-loaded* department's list —
typing an item name that belongs to another department returns nothing. To bill a service
that lives in a different department (e.g. `CT SCANNING CHARGES` / `SUTURING & DRESSING
CHARGES` under **ETU**), first click that department's button to load its items, *then* pick
from the list. Symptom if you skip this: the filter shows "no match" even though the item
exists and the DB confirms it. Refs churn after the department-button AJAX, so re-`snapshot`
before clicking the option, and click the visible listbox row (the hidden native `<option>`
with the same text is not clickable). Verified while testing room-category service margins
(issue #21977).

## 22. Inward pharmacy margin lookup uses the *inpatient* department, not the issuing pharmacy

When testing the inward price-adjustment (service-charge) margin for **pharmacy** issues to an
inpatient, the matrix department is resolved by `PharmacySaleBhtController.determineMatrixDepartment()`,
which is gated by config `"Price Matrix is calculated from Inpatient Department for <issuing dept>"`
(**default true**). When on, the lookup uses the patient's **current room's facility-charge
department** — for A/C/Non-A/C rooms in the model DB that is **Inward**, *not* the pharmacy you
are logged into (e.g. Main Pharmacy). Symptom if you create the matrix row against the pharmacy
department: the margin resolves to 0 / the wrong row even though the row exists. Fix: create the
`InwardPriceAdjustment` row for the **room facility charge's department**
(`SELECT rfc.department_id FROM patientencounter pe JOIN patientroom pr ON pe.currentpatientroom_id=pr.id
JOIN roomfacilitycharge rfc ON pr.roomfacilitycharge_id=rfc.id WHERE pe.id=<enc>`), and set the
row's payment method to match the encounter's (`patientencounter.paymentMethod`). Also beware
**pre-existing overlapping rows** for the same dept/category/price-range/payment-method — they make
the wildcard-vs-specific comparison ambiguous; temporarily `retired=1` them for a clean A/B test,
then restore. Fastest confirmation without the full multi-page issue flow:
`GET /api/inward-price-adjustment/diagnose?itemId=&departmentId=&paymentMethod=&patientEncounterId=&price=`
with a `Finance` API-key header — it runs the identical `fetchInwardMargin(...)` call the pharmacy
controllers use and returns the matched row id + margin %. Verified while testing room-category
pharmacy margins (issue #21981).

## 23. Three authoring gotchas found via E2E on the role-template pages (issue #22023)

Testing `admin/users/user_role_users.xhtml` / `user_role_bulk_operations.xhtml` surfaced
three silent-failure patterns worth checking on any new admin page:

1. **`p:selectManyCheckbox` over a `List<Entity>` needs an explicit named converter.**
   The `@FacesConverter(forClass = Department.class)` converter is *not* applied to
   `UISelectMany` bound to a generic `List` (type erasure — JSF can't detect the element
   type), so submitted values stay `String`s and the action later dies with
   `ClassCastException: java.lang.String cannot be cast to ... Department` inside the EJB.
   Fix: register a named converter (e.g. `userRoleDepartmentConverter`) and set
   `converter="..."` on the component explicitly.
2. **`process="cmbA cmbB"` without `@this` silently skips the button's own action.**
   The AJAX request fires, inputs are applied, the `update` render runs — but the
   `action` never executes because the button itself wasn't in the execute list.
   Symptom: "No records found" with no error anywhere. Always write
   `process="@this cmbA cmbB"`.
3. **Multi-select checkbox column: this PrimeFaces version wants `selectionMode="multiple"`
   on the `p:dataTable` + `<p:column selectionBox="true"/>`** — a
   `<p:column selectionMode="multiple"/>` (the pattern current PF docs show) renders an
   *empty* cell. Copy the working pattern from `user_remove_multiple.xhtml`.

Also (rendering): a `p:selectOneMenu` bound to `#{bean.current.field}` blows up the whole
page with `PropertyNotFoundException: Target Unreachable` when `current` is null on first
GET — unlike `p:inputText`, select components resolve the value expression's *type* during
render. Guard with `rendered="#{bean.current ne null}"`.

## 24. Granting a privilege that doesn't exist on the checked-out branch silently blanks ALL privileges for that department

If you insert a `webuserprivilege` row for a `Privileges` enum value that exists on
*another* branch (e.g. one you tested earlier today) but not on the branch currently
checked out and deployed, the entire menu goes blank and every `hasPrivilege(...)` check
returns `false` for that user **in that department** — not just the one bad privilege.
`WebUserPrivilege.privilege` is `@Enumerated(EnumType.STRING)`; EclipseLink converts the
DB string to the Java enum via `Enum.valueOf(...)` when it hydrates the full result list
for `SessionController.fillUserPrivileges()`, and a single row whose string isn't a valid
constant on the *currently running* code silently poisons that entire fetch — with no
`SEVERE` entry in `server.log` and no visible page error, just empty menus / "not
authorized" everywhere for that department, while other departments the row doesn't
affect work fine (a strong tell if you compare departments). A domain restart or a full
undeploy+redeploy does **not** fix this — it's a data/branch mismatch, not a cache.

Diagnose fast: enable the MySQL general log to a table (`SET GLOBAL log_output='TABLE';
SET GLOBAL general_log='ON';`) and check `mysql.general_log` for the exact
`SELECT ... FROM WEBUSERPRIVILEGE WHERE ...` query, run it directly, then diff the
distinct `PRIVILEGE` values for that user/department against
`grep -oP '(?<=^    )[A-Za-z0-9_]+(?=\(")' src/main/java/com/divudi/core/data/Privileges.java`
(note: some enum lines have a trailing `//` comment that breaks a naive end-of-line
regex — verify any apparent mismatch with a direct `grep` before trusting the diff).

This came up switching from the GRN privilege-guard branch (issue #22019, which added
`PharmacyGrnCancel`/`PharmacyGrnReturnCancel`) to the PO privilege-guard branch (#22020,
checked out fresh from `origin/development` since #22019 wasn't merged yet) — rows
granted while testing #22019 were still sitting in the shared local DB and broke every
privilege check for that department under the PO branch's code. Fix: delete (or retire)
the rows for privileges that don't exist on the currently deployed branch, re-grant only
what the current branch's `Privileges.java` actually declares, then re-login.

## 25. A JPQL path expression through a nullable relationship silently INNER-JOINs and drops rows

Writing `b.patientEncounter.bhtNo` or `b.patient.person.name` directly in a `SELECT`/`WHERE`
generates an **implicit INNER JOIN** on that relationship. If the relationship is null for
some rows (e.g. `patientEncounter`/`patient` are null on OPD bills), **every one of those
rows silently disappears** from the result — no error, no log entry. The report just looks
"wrong" (too few rows), and it's easy to blame filters/dates first.

Tell: a combined OPD+Inward report showed only the 2 Inward rows (which have a
`patientEncounter`) and dropped all 20 OPD rows for the same item/date range. DB count
didn't match the report count. Fix: use explicit `left join b.patientEncounter pe` /
`left join b.patient pat left join pat.person per` and reference the aliases (`pe.bhtNo`,
`per.name`) in the projection. Verified on issue #21920 (`fetchItemizedServiceInstanceDTOs`
in `BillService`). Always cross-check report row count against a direct
`SELECT ... FROM billitem bi JOIN bill b ...` when a report projects fields from an
optional relationship.

Related sign gotcha found the same pass: cancellation/refund `BillItem` fee columns
(`netValue`, `hospitalFee`, …) are **already stored negative** in the DB. A
`case when billClassType in (cancel, refund) then -bi.netValue else bi.netValue end`
double-negates them to positive — this is the "fee doubling after cancellation" symptom
(issue #21918). Use the stored values as-is; only synthesize a sign for the row **count**
(which has no stored value). Verify by summing `bi.netValue` directly in SQL and matching
the report's Grand Total.

## 26. Editing a `ConfigOption` via raw SQL is invisible to the running app — use the admin UI

`ConfigOptionApplicationController.getApplicationOption(key)` reads through EclipseLink's
shared L2 entity cache. A direct `UPDATE configoption SET optionvalue=... WHERE optionkey=...`
via the `mysql` CLI changes the DB row but the already-cached `ConfigOption` entity in the
running Payara instance keeps serving the old value — `getLongValueByKey`/`getBooleanValueByKey`
never see the change, with no error or log entry. This wasted a full test cycle while verifying
a day-limit config for issue #22055 (two `UPDATE` statements had zero effect on rendered button
state).

**Fix:** edit config values through `admin/institutions/admin_mange_application_options.xhtml`
(List Application Options → filter by Key → **Edit Option** → Save). That path goes through
the entity manager and correctly invalidates the cache, and the change is visible on the very
next page load — no redeploy or Payara restart needed. Reserve raw SQL for *reading* config
state (e.g. confirming a key auto-created with the right default on first access), never for
writing it mid-test.

## 27. Multi-Payara machines: `asadmin` without `--port` may hit ANOTHER USER'S domain

On a box with two Payara installs (e.g. `/home/carecode/payara` domain `rh` admin port **9048**,
and `/home/buddhika/payara` domain1 on default **4848**), a bare `asadmin redeploy/undeploy/deploy`
connects to whoever owns 4848 — which can be the *other user's* server. Tells that you're on the
wrong DAS: `redeploy` fails with **"Cannot determine the path of application"**, `deploy` fails with
**"File not found"** for a WAR that clearly exists (the other user's Payara process can't traverse
your 750-mode home directory), and `list-applications` shows an app list that doesn't match your
domain. An `undeploy` in this state removes the app from the *other* server — before any
state-changing asadmin call, confirm which process owns the admin port you're about to use
(`ss -tlnp | grep <port>` + `ps -o user= -p <pid>`), run `asadmin --port <port> list-applications`
on that same endpoint to confirm the expected app list, and always pass the explicit admin port on
**every** command (`asadmin --port 9048 redeploy/undeploy/deploy ...` for the local `rh` domain —
never the bare default).

Recovery after removing the wrong domain's app: copy the WAR to a path the *target* domain's user
can read (its Payara can't traverse your `0750` home directory) — keep permissions as tight as that
allows (e.g. a dedicated directory rather than bare `/tmp`, no wider than `0644` on the file),
rewrite `WEB-INF/classes/META-INF/persistence.xml` inside the copy to that domain's JNDI names via
`unzip`/`sed`/`zip`, **verify the rewritten `<jta-data-source>` values before deploying**, deploy
with `--port <that domain's admin port>` and `--name`/`--contextroot` matching what was removed,
and **delete the staged copy immediately after** the deploy succeeds. (Hit while deploying for
issue #14863.)

## 28. Claude-in-Chrome on heavy non-AJAX report pages (full-submit + long query)

Verifying `slow_fast_none_movement.xhtml` (multi-minute aggregate queries, `ajax="false"` Process
button) surfaced these:

- **Screenshots time out while the server renders** (`Page.captureScreenshot ... renderer may be
  frozen`). Don't retry screenshots in a loop — poll cheaply with `javascript_tool` on
  `document.readyState` + a marker string in `document.body.innerText`, and screenshot once ready.
- **`find`-ref and coordinate clicks on the submit button intermittently do nothing** (stale refs
  after each full reload; overlay panels intercepting clicks). The reliable submit is DOM-level:
  `[...document.querySelectorAll('button')].find(b=>b.textContent.includes('Process')).click()`.
- **Set PrimeFaces inputs directly on the hidden native elements before a full submit**: p:selectOneMenu
  → `select[id$="..._input"].value = '...'`; p:selectCheckboxMenu → toggle
  `input[name$="billTypes"]` checkboxes; p:datePicker → `PF widget .setDate(new Date(...))`. For a
  non-AJAX submit only the submitted values matter, so skipping the widget UI is safe and immune to
  overlay/timing issues. (AJAX listeners do NOT fire this way — only use for full-form submits.)
- **html2canvas does not capture PrimeFaces overlay panels** (`*_panel` appended near body root render
  blank/absent) — capture page states instead, or read the panel's `innerText` as textual evidence.

## 29. GRN costing Save→Finalize→Approve: `Difference` guard needs a real keyup on Invoice Total at EVERY step

On `pharmacy_grn_costing_with_save_approve.xhtml` the controller field `difference` (checked by
`Math.abs(difference) > 1` in the finalize/approve actions) is recomputed **only** by the
`p:ajax event="keyup"` listener on the Invoice Total input (`insv`) — a DOM-set value applied by an
`ajax="false"` full submit updates `insTotal` server-side but never recalculates `difference`, so the
approve fails with "The invoice does not match..! Check again" even though the submitted total is
correct. Worse, after the Finalize → "To Approve GRNs" → Approve navigation the page reloads with
Invoice Total rendered as `0.00`, so a value that passed at Save/Finalize is gone at the Approve step.
Fix in automation: on the approve pass, click into `insv`, `Control+a`, `browser_type` the total
`slowly: true` (real keyups fire the AJAX), confirm the `diff` input reads `0.00`, then click Approve.
Everything else on that page (row qty/free-qty/batch/expiry/retail-rate inputs, invoice number/date)
CAN be set directly on the DOM inputs — the `ajax="false"` Save/Finalize buttons submit and apply them
(verified while testing issue #22120).

## 30. `ward_pharmacy_bht_issue_request_bill.xhtml` — "New Bill" silently discards unsaved items

On the "Start Pharmacy Request for Inpatients" flow, the "Add Dispense Only" button only stages
`BillItem`s in the in-memory `PreBill` — nothing is persisted until "Settle Request" is clicked (the
"Save Draft" button that would otherwise persist an intermediate `PharmacyBhtPre` is `rendered="false"`,
per a comment in the page noting there's currently no way to resume a saved draft). The "New Bill"
button (`actionListener="#{pharmacyRequestForBhtController.resetAll}"`) looks like a reasonable "finish
this request" action but actually **discards all staged items with no confirmation** and resets the form
to "Start Pharmacy Request for Inpatients". If a Playwright pass adds items and then clicks "New Bill"
expecting the request to be saved, a DB check afterward will show nothing was created. Always use
**"Settle Request"** (confirm-dialog-guarded) to actually persist a BHT pharmacy request. Verified while
testing issue #22153.

## 31. `ward_pharmacy_bht_issue_request_bill.xhtml`'s "Add Dispense Only" path sets `department`/`toDepartment` backwards

`PharmacyRequestForBhtController`'s no-prescription creation path (the one behind
"Add Dispense Only" → "Settle Request") sets `getPreBill().setToDepartment(getDepartment())`,
where `getDepartment()` is the page's *Requesting Department* selector (the ward, e.g.
"Inward") — the opposite of what the prescription-based "Calculate & Add" path does. The
resulting bill ends up with `department` = the requesting ward and `toDepartment` = the
requesting ward too, instead of `toDepartment` = the fulfilling pharmacy. Per §16, the
pharmacist's "Issue Medicines" list (`ward_pharmacy_bht_issue_request_list_for_issue.xhtml`)
filters on `toDepartment = session department`, so a request created via "Add Dispense Only"
silently never appears there — "Search All"/"Search Not Issued" both return "No records
found." even with the correct BHT number. This looks like a pre-existing, unrelated bug (not
reproducible via the prescription-based creation path) — found incidentally while testing
issue #22000; not fixed there since it was out of that issue's scope. If blocked on this
during a future E2E pass, either use "Calculate & Add" instead of "Add Dispense Only" to
create the test request, or correct `BILL.DEPARTMENT_ID`/`TODEPARTMENT_ID` directly in the
local dev DB to unblock testing.

## 32. A `FacesMessage` can be server-confirmed even when the browser never shows it

Two related traps when checking whether `JsfUtil.addWarningMessage(...)` actually fired:

- **A page-local `p:growl` without a `life` attribute never auto-dismisses**, unlike
  `template.xhtml`'s global growl (`life="3000"`). If a later click lands on where the toast is
  rendered, Playwright's actionability check reports `<span class="ui-growl-title">...
  intercepts pointer events` and the click times out. Work around it in a test session with
  `browser_evaluate`: `() => document.querySelectorAll('.ui-growl-item').forEach(el =>
  el.remove())` — do not treat this as something the product code needs to fix unless the
  issue you're working on is specifically about that page's growl behavior.
- **On an `ajax="false"` (full-postback) button, a `life`-bound growl can auto-hide before you
  take a snapshot**, making it look like the message never fired even though it did. Don't
  trust a missed visual — inspect the actual HTTP response instead:
  `browser_network_requests` (filter on the page's `.xhtml`, `static: true` if needed) to find
  the POST matching the button's `name` parameter (e.g. `j_idt523%3AbtnAdd=`), then
  `browser_network_request` with `part: "response-body"` on that index. For an AJAX
  (`javax.faces.partial.ajax=true`) update, look for `<update id="...:growl">` containing
  `PrimeFaces.cw("Growl",...,msgs:[{summary:"...",severity:'warn'}]})`. For a full postback,
  grep the (often huge) HTML response body for the expected message text instead of loading it
  into context. Verified while testing issue #22000, where this was the deciding evidence that
  the warning fired correctly on a page whose *unrelated* pre-existing widget-init JS error
  (`TypeError: Cannot read properties of undefined (reading 'hasAttribute')`, present since
  before any interaction) prevented the growl from rendering visually at all.

## 33. Binding a `p:inputText` through a nullable session-scoped entity property crashes on first submit

`ward/issue_for_bht_request_list.xhtml` bound its BHT-No search box to
`#{searchController.patientEncounter.bhtNo}` — a nested property path through
`SearchController.patientEncounter`, which is `@SessionScoped` and never
initialized on this page (nothing sets it before rendering; unlike the 3 lab
pages that bind `value="#{searchController.patientEncounter}"` — the whole
object, not a nested field — via `p:autoComplete`/`p:selectOneMenu`, which
tolerate `null`). Every submit threw `EL: Target Unreachable, 'null' returned
null` (HTTP 500) during `UIInput.getConvertedValue`, both with and without
typing into the field — the crash happens on *any* postback of the form, not
just when the bound property is touched. Fixed for #22196 by rebinding to
`searchController.searchKeyword.bhtNo` (a plain `String`, default `""`),
matching the pattern already used by ~28 other pages in this codebase (grep
`searchController.searchKeyword.bhtNo` across `*.xhtml`). **Lesson**: never
bind a `p:inputText` through a nested path on a session-scoped controller's
entity-typed field unless something on the *same* page is guaranteed to have
set that entity first — bind through a dedicated search-keyword/DTO field
instead.

## 34. Local dev DB can silently drift behind the entity model — watch for `Unknown column` on unrelated pages

While testing #22196, clicking "View Request" on an inward pharmacy request
threw `SQLSyntaxErrorException: Unknown column 'VATPERCENTAGE' in 'field
list'` loading `BillItem` rows — the local `coop` DB's `BILLITEM` table
predates the `vatPercentage` field added to the `BillItem` entity, and there
is no DDL/migration step in the local dev workflow that keeps schema in sync
automatically. This is unrelated to whatever feature is under test and will
recur for any page that touches `BillItem`. Fix locally with a plain additive
column matching the sibling `VAT`/`VATPLUSNETVALUE` columns:
`ALTER TABLE BILLITEM ADD COLUMN VATPERCENTAGE DOUBLE NULL DEFAULT NULL AFTER
VAT;` — do not add this to a migration script (it's a local-only environment
gap, not a schema change accompanying a code change). If a fresh
`Unknown column` error appears on an otherwise-unrelated page, check
`SHOW COLUMNS FROM <table>` against the entity's fields before assuming the
feature under test is broken.

## 20. Don't `disable` + `enable` the app to clear the L2 cache — restart the domain

The disable→enable trick for flushing a poisoned EclipseLink shared cache (§ noted in
earlier sessions) loads the whole application a **second time in the same JVM**, and on
this codebase that reliably ends in `java.lang.OutOfMemoryError: Java heap space` +
`CDI deployment failure` mid-enable, leaving the app 404 (hit during issue #22011
verification). Restart the domain instead — slower, but it actually comes back up:

```bash
asadmin stop-domain <dom>    # "domain is already stopped" is fine — continue
asadmin start-domain <dom>
```

Run the two commands separately (not chained with `&&`): if the domain is already
down, `stop-domain` exits nonzero and a chained `start-domain` would be skipped,
leaving the app offline.

## 35. Department-scoped dashboards need the department actually switched, not just full privileges

While verifying #22213 (theatre stay billing), the Theatre Dashboard's "Awaiting
Theatre Acceptance" / "Pending Return to Ward" lists showed **0** rows even though
a request definitely existed (confirmed via direct DB query) and the logged-in
user had every relevant privilege. Root cause: `PatientTransferController`'s
loader methods (`loadPendingForTheatre()`, `loadInTheatreRequests()`, etc.) filter
on `r.toRoomFacilityCharge.department = sessionController.getDepartment()` — the
**currently selected** department, not "any department the user has access to."
Being logged in under "Inward" and merely navigating to a Theatre page renders it
fine but shows empty lists. Fix: log out and back in, and on the Select Department
screen explicitly pick the department the workflow actually belongs to (here,
"THEATRE") before testing department-scoped actions — the app remembers your last
selection and will silently keep applying it across unrelated page navigations.

## 36. `@SessionScoped` bean data can go stale after a direct URL hit — navigate through the real action chain

Also during #22213 verification: after completing a theatre "return to ward" action
(via a proper button click with a bound `action="..."` method), directly typing the
URL for `/inward/inward_patient_room_details.xhtml` showed the just-discharged room
still as "Active" — the underlying DB was already correct (verified via SQL), but
`BhtSummeryController` (`@SessionScoped`) lazily caches `patientRooms` on first
access and only a handful of specific action methods actually refresh it. A raw URL
navigation skips whatever `action="..."` a genuine button click would have invoked,
so it reads the stale in-memory list from earlier in the session. Fix: always
reach the page under test by clicking through the real navigation chain (search →
dashboard button → target page) rather than pasting/typing the target URL directly,
especially right after an action that's supposed to change what that page displays.

## 37. A required `p:selectOneMenu` with no default silently swallows an entire non-AJAX submit — no network request, no error

On `inward/inward_bill_professional_payment.xhtml` (and likely the surgery
equivalent), the "Find Due Payments" button (`type="submit"`, `onclick=""`,
plain `ajax="false"` postback — verified via `outerHTML`) does **nothing
at all** when the "WHT Calculation" dropdown is left at its default empty
"Select" option — no `browser_network_requests` entry appears, no MySQL
`general_log` query fires, no visible error, and the page doesn't even
reload. `browser_click` and a JS `.click()` on the button both silently
no-op. The accessibility snapshot's only tell is the dropdown rendering as
`combobox "Select" [invalid]` — client-side JSF validation
(`PrimeFaces.settings.validateEmptyFields=true`) blocks the *entire* form
submit before it reaches the network layer, exactly like the required-field
gotcha in §5/§12 but with **zero observable signal** beyond that one
`[invalid]` accessibility attribute (this button isn't even in the same
visual section as the required field, so it's easy to miss).

**Diagnosis technique for local dev DBs only — never on staging/production**:
`mysql.general_log` captures every statement verbatim, including patient
identifiers and payment values, and adds real overhead while active, so only
enable it against your own local dev database, for the shortest possible
window. Enable it (`SET GLOBAL log_output='TABLE'; SET GLOBAL
general_log='ON';`, `TRUNCATE TABLE mysql.general_log;`), click the button,
then check
`SELECT event_time, argument FROM mysql.general_log ORDER BY event_time DESC`
— if the expected query never appears at all (not even a failed one), the
submit never reached the server, which points at client-side validation
rather than a bean/JPQL bug. Immediately run `SET GLOBAL general_log='OFF'`
afterward and truncate the table again to avoid leaving captured rows
sitting around.

**Fix**: before clicking any non-AJAX submit button on this page, first
select a real option in every required dropdown on the same form (here:
click the "WHT Calculation" combobox → click e.g. "Include Withholding
Tax" from the listbox), even if that dropdown looks unrelated to the
button you're about to click — required-field validation on a JSF
`ajax="false"` postback applies to the whole `<h:form>`, not just the
fields near the button.

## 38. A local dev DB missing columns for an already-shipped entity field surfaces as a hung page, and `ALTER TABLE` alone doesn't fix it — the pool needs a flush

Entity fields that were added to the codebase a while ago (e.g.
`PatientEncounter.professionalPaymentsOnHold` / `...HoldDateTime` /
`...HoldBy` / `...HoldNotes`) can be **missing from a local dev database**
that was never migrated, even though nothing about the current change
touches those fields. Symptoms are confusing because EclipseLink issues a
`SELECT *`-style query for the whole entity on any page that touches it, so
the failure isn't localized to the field you'd expect:

- Direct-navigating to a page via URL (bypassing the app's normal
  click-through flow) can appear to **hang indefinitely** in Playwright
  (`browserBackend.callTool` timeouts on `navigate`/`snapshot`/even
  `tabs list`) rather than showing an error — the request never actually
  hangs server-side, but an error response mid-navigation can leave the
  MCP browser bridge stuck. If a normal in-app link/button navigation to
  the same destination works cleanly and shows the real `SQLSyntaxErrorException:
  Unknown column '...' in 'field list'` page, that confirms it's this
  gotcha, not a broken browser.
- The fix is a plain `ALTER TABLE ... ADD COLUMN ...` matching the
  entity's field type (check the `@Column`/type in the entity class), but
  **the running Payara connection pool caches connections/statement
  metadata from before the ALTER** — re-hitting the page immediately after
  the ALTER still throws the identical "Unknown column" error. Flush the
  pool before retrying:
  `asadmin flush-connection-pool <poolName>` (find the pool name via
  `grep -B2 'jndi-name="jdbc/coop"' domain.xml` → look for the
  `<jdbc-resource pool-name="...">` line, e.g. `poolCoopLocal` for
  `jdbc/coop`).
- Combining this with §20's privilege-row gotcha: if panels are still
  missing after the schema+pool fix, check privileges next — they're
  independent causes of the same "content silently doesn't render" symptom.

Verified while testing the Inward Dashboard "Manage Allergies" /
"Hold Professional Payments" button relocation (issue #22248), where the
local `coop.patientencounter` table was missing all four
`professionalpayments*` columns and `patienttransferrequest` was missing
`theatreroom_id`.

## 39. Local dev DB has no `FrequencyUnit`/`DurationUnit`/`DoseUnit` seed rows — the prescription "Calculate & Add" path is untestable locally

`ward_pharmacy_bht_issue_request_bill.xhtml`'s Prescription section (Dose/Dose
Unit/Frequency/Duration/Duration Unit → "Calculate & Add") requires selecting
a `FrequencyUnit` and `DurationUnit` — both are `Category` subclasses stored
in the single-table `category` (via `@Inheritance` with no strategy = default
`SINGLE_TABLE`, discriminated by `DTYPE`). The local `coop` DB has **zero**
rows with `DTYPE` in (`FrequencyUnit`, `DurationUnit`, `DoseUnit`) — confirmed
via `SELECT DISTINCT DTYPE FROM category`. Both dropdowns render as
`combobox "Select"` with no other options, and submitting anyway fails with
`"Calculation Error: Incomplete prescription: dose, frequency, duration and
duration unit are required"`. **Workaround**: use the "Dispense Request" →
"+ Add Dispense Only" path instead (item autocomplete + plain qty field, no
prescription fields) — but that path has the toDepartment bug from §31, so
still fix `TODEPARTMENT_ID` via SQL afterward. Verified while testing issue
#22312.

## 40. Auto-substitution can silently turn a "zero stock" test case into "issued in full"

When testing a BHT/pharmacy-request stock-shortfall feature, don't assume an
item with 0 stock at the issuing department will exercise the "no stock"
code path — `PharmacySaleBhtController.generateIssueBillComponentsForBhtRequest`
(and similar issuing flows) auto-substitutes to a same-VMP sibling AMP with
stock before falling back to "no stock". An item whose exact AMP has 0 stock
but has an in-stock sibling under the same VMP (e.g. `Levo 500mg Tablet` →
`EVITRA 500MG`) will be silently issued in full via the substitute, hiding the
zero-stock code path entirely. To reliably hit "no stock at all", pick an item
with **no in-stock siblings under its VMP either** — verify first:
```sql
SELECT a.ID, a.NAME, a.VMP_ID FROM item a WHERE a.DTYPE='Amp'
AND a.ID NOT IN (SELECT ib.ITEM_ID FROM stock s JOIN itembatch ib ON s.ITEMBATCH_ID=ib.ID
                 WHERE s.DEPARTMENT_ID=<dept> AND s.STOCK>0)
AND (a.VMP_ID IS NULL OR a.VMP_ID NOT IN (
  SELECT a2.VMP_ID FROM item a2 JOIN itembatch ib2 ON ib2.ITEM_ID=a2.ID
  JOIN stock s2 ON s2.ITEMBATCH_ID=ib2.ID WHERE s2.DEPARTMENT_ID=<dept> AND s2.STOCK>0 AND a2.DTYPE='Amp');
```
Verified while testing issue #22312.

## 41. A local dev DB with an empty `TRIGGERSUBSCRIPTION` table means notification-generating actions silently produce zero `UserNotification` rows

Discharging a patient, changing a room, etc. always creates a `Notification`
row, but the actual per-user `UserNotification` rows (what the bell icon and
`/Notification/user_notifications.xhtml` show) only get created for webusers
who hold a matching `TriggerSubscription`
(`NotificationController.createNotification(...)` →
`userNotificationController.createUserNotifications(nn)` →
`TriggerSubscriptionController.fillSubscribedUsersByDepartment(...)`). A
freshly-restored or never-fully-seeded local DB can have **zero rows in
`TRIGGERSUBSCRIPTION`**, in which case discharging any number of patients
produces `Notification` rows but no `UserNotification` rows for anyone —
this looks identical to "the feature doesn't work" but is actually missing
test-fixture data, not a bug.

- Diagnose with `SELECT COUNT(*) FROM TRIGGERSUBSCRIPTION;` — 0 confirms this.
- Fix through the UI, not SQL (per this doc's "use the admin UI" pattern,
  §26): Admin → Manage Users → select the target user → **Manage User
  Subscriptions** → tick **Application-wide** → pick the relevant
  `TriggerType` (e.g. "Inward Patient Room Discharge - System Notification")
  → **Add Subscription**.
- The **Application-wide** checkbox's visible box intercepts Playwright's
  normal click on the underlying `p:selectBooleanCheckbox` input — click via
  a selector scoped to its own JSF id (`chkApplicationWide` in
  `admin/users/user_subscription.xhtml`), not a bare `.ui-chkbox-box` index,
  which picks whichever checkbox happens to be first/nth on the page and can
  silently toggle the wrong control if the page has more than one:
  `document.querySelector('[id$="chkApplicationWide"] .ui-chkbox-box')`.

## 47. Local Payara can come up with a dead MySQL connection pool after any host sleep/restart — every page hangs, not just one touching a stale entity

Unlike §38 (a pool holding connections from *before* an `ALTER TABLE`), this is
the pool holding connections to a MySQL instance that was itself restarted or
the host machine slept/resumed. Symptoms are more severe than §38's
single-page hang: **the app root itself** (`GET /rh`, even the pre-login page)
times out in both a direct `Invoke-WebRequest`/`curl` and
`browser_navigate`/`browser_snapshot` (30-60s timeouts with no response) —
because `ConfigOptionApplicationController.init()` runs on first
request/session and hits the DB immediately. `server.log` shows
`CJCommunicationsException: Communications link failure` /
`SQLNonTransientConnectionException: No operations allowed after connection
closed` from background EJB timers even while `mysql -h <local-mysql-host>
... SELECT 1` succeeds fine from the shell — proving MySQL itself is up and
it's specifically Payara's pool holding dead connections.

**Diagnose**: confirm MySQL responds directly first (rules out "DB is down"),
then confirm Payara's admin port responds to `list-applications` (rules out
"domain is down") — if both succeed but the HTTP listener (9090) times out,
suspect the connection pool.

**Fix**: flush both the main and audit pools (find pool names via
`grep -B2 'jndi-name="jdbc/coop"' domain.xml` /
`grep -B2 'jndi-name="jdbc/ruhunuAudit"' domain.xml` — e.g. `poolCoop` and
`poolRuhunuAuditLocal` locally):
```powershell
& asadmin.bat --port 5858 flush-connection-pool poolCoop
& asadmin.bat --port 5858 flush-connection-pool poolRuhunuAuditLocal
```
No redeploy or domain restart needed — a plain HTTP request succeeds
immediately after the flush. Verified while testing issue #22423 (itself a
stale-audit-pool-connection bug), where the local dev machine's own audit
pool had gone stale exactly the way the issue described.

## 48. A leftover Playwright-MCP Chrome profile can lock out `browser_navigate` with no relation to the user's real browser windows

`mcp__playwright__browser_navigate`/`browser_snapshot` can fail with `Browser
is already in use for <profile-dir>, use --isolated to run multiple instances
of the same browser` even when no Playwright session is visibly active. This
comes from an orphaned Chrome process tree still holding that specific
`--user-data-dir` (named `ms-playwright-mcp\mcp-chrome-<hash>` on Windows),
left behind by a prior session that didn't shut down cleanly — it is **not**
related to the user's everyday Chrome windows, which run under a different
profile entirely. Confirm before touching anything:
```powershell
Get-CimInstance Win32_Process -Filter "Name = 'chrome.exe'" |
  Where-Object { $_.CommandLine -like '*ms-playwright-mcp*' } |
  Select-Object ProcessId, CommandLine
```
Every process whose `CommandLine` contains `ms-playwright-mcp` (main browser,
crashpad handler, gpu-process, utility, renderer subprocesses) is safe to
`Stop-Process -Force` — they all share that same isolated profile directory,
distinct from the user's real Chrome profile. After clearing them,
`browser_navigate` launches a fresh instance normally (the very first
navigation after relaunch can still take up to 60s — a single retry is
usually enough). Verified while testing issue #22423.

Found while verifying issue #21538 (discharge notifications routing to the
wrong patient) — the fix couldn't be end-to-end tested at all until this was
discovered and worked around.

## 42. PrimeFaces bare `update="someId"` can 500 from inside a `p:dataTable`/`ui:repeat` row even though the id exists on the page

A `p:commandButton update="someId"` where `someId` is a **sibling id
declared outside** the enclosing `p:dataTable`/`ui:repeat`/`p:column` throws
a hard 500 (`javax.faces.component.search.ComponentNotFoundException:
Cannot find component for expressions "someId"`) as soon as that button is
rendered for any row — not just on click, since PrimeFaces builds the ajax
request descriptor (including resolving `update`) during **encode**, not
decode. It can appear to work for row 0 by coincidence and break only from
row 1 onward, or break for every row once a row's content changes (e.g. a
row toggling into "retired" state and rendering a previously-`rendered=false`
button for the first time) — so it can look like a row-index-specific bug
rather than a general one.

Fix: don't rely on plain-id resolution reaching outside the table/repeat.
Use `update="@form"` (safe/simple when refreshing the whole form is
acceptable) or an absolute id path — this project's `jsf-ajax` skill already
documents `@this`/`@form`/`:#{p:resolveFirstComponentWithId(...)}` as the
required patterns for exactly this reason.

Found while fixing issue #21538: `Notification/user_notifications.xhtml`'s
"Restore" button (`update="reNot"`, `reNot` being the `h:panelGroup`
wrapping the whole list) crashed the page load itself once a retired
notification was shown in a row other than the first.

## 43. Clicking a `p:printer` button hangs the whole browser session — verify with print-media emulation instead

`p:printer` calls `window.print()`, which opens a real native OS print dialog.
In a Playwright-driven session this dialog blocks not just the click (which
times out and gets moved to a background task) but **every subsequent tool
call on that browser** — `browser_tabs list/new/close` all hang too, because
the dialog is modal at the OS/browser-process level, not a JS `confirm()`
that `browser_handle_dialog` can intercept. The only recovery is asking the
human operator to manually dismiss the dialog in the actual browser window.

**Don't click the Print button to verify print CSS.** Instead, emulate print
media on the existing page and screenshot that — `p:printer` clones the
current document's `<head>` (including inline `<style>` blocks and linked
stylesheets) into its print iframe, so `@media print` rules apply identically
whether triggered by the real dialog or by emulation:

```js
async (page) => { await page.emulateMedia({ media: 'print' }); }
```

Then `browser_take_screenshot` — this shows exactly what would print (hidden
`.noPrintButton` elements, `.printOnlyReport` toggled visible, etc.) without
ever touching `window.print()`. Verified while fixing issue #22316 (Time
Service Report print truncation).

**Scope of this check**: this only proves `@media print` visibility/layout
rules apply correctly — it does not verify pagination, page-fit, or page
breaks across multiple printed pages. For reports where those matter, follow
up with an actual PDF export or a manual print-preview pass.

## 44. A freshly-created test user needs a `WebUserDepartment` row, not just a `Department` field, to log in at all

Creating a disposable test user via Admin > Manage Users > Add New User
(`admin/users/user_add_new.xhtml`) and setting its `Department` field is not
enough to let it log in. Login checks `listLoggableDepts(user)`
(`SessionController.java`), which queries the `WebUserDepartment` join table
— not the `WebUser.department` column. With no matching `WebUserDepartment`
row, login fails with "This user has no privilage to login to any
Department. Please conact system administrator." even though the user
record itself looks fully configured.

The Add New User form has no field for this; department-login grants are
managed separately via **Manage Users > (select user) > Manage User
Departments**. For a quick disposable test account it's simplest to insert
the row directly:
```sql
INSERT INTO webuserdepartment (CREATEDAT, RETIRED, DEPARTMENT_ID, WEBUSER_ID)
VALUES (NOW(), 0, <department_id>, <webuser_id>);
```
Also useful: no `WebUserRole` in this DB grants `ShowServiceCharges` (verified
via `webuserroleprivilege`) — it's only assigned to individual users
directly in `webuserprivilege`. So any freshly-created user with no role
already lacks it, no extra step needed to test privilege-gated hiding.

Found while verifying issue #22310 (fee row hidden along with its item name
when `ShowServiceCharges` is absent) — needed a throwaway non-privileged
login to confirm the fix without touching any real staff account.

## 45. `&&` written as `&amp;&amp;` inside a `<script><![CDATA[...]]>` block parses as valid XML but throws a JS `SyntaxError` at runtime, silently breaking every function in that script

When copying a `<script>` block that lives inside `<![CDATA[ ... ]]>` into a
new XHTML page, writing the literal characters `&amp;&amp;` (instead of `&&`)
is easy to do by habit — most other XML/XHTML text content genuinely needs
`&` escaped — but CDATA sections are explicitly exempt from entity
expansion, so this is always wrong there. The bug hides unusually well:

- `xml.etree.ElementTree` (or any XML well-formedness check) parses the file
  without complaint — `&amp;` is perfectly valid character data whether or
  not it's inside CDATA, so a "did the file parse" check gives a false all-clear.
- Facelets' XML parser reads the CDATA content literally (per spec, no entity
  expansion inside CDATA), so the in-memory text node keeps the literal
  6-character sequence `&amp;`. When Facelets serializes the response back
  out as plain text (not treating it as a CDATA passthrough), it re-escapes
  `&` to `&amp;` — so the browser receives doubled-up `&amp;&amp;` in the
  final HTML.
- Because `<script>` is an HTML5 "raw text" element, the browser does **not**
  decode entities inside it — it hands the literal text straight to the JS
  parser, which throws `SyntaxError: Unexpected token ';'` trying to parse
  `&amp;&amp;` as code. **This aborts parsing of the entire `<script>`
  block**, so every function defined anywhere in that block — even ones
  with no `&&` in them at all — ends up undefined, surfacing later as
  unrelated-looking `ReferenceError: xyz is not defined` console errors when
  something tries to call them (e.g. via a PrimeFaces AJAX partial update
  that re-inserts an inline `<script>` calling one of those functions).

Detection: `curl` the deployed page and `grep -c '&amp;&amp;'` vs
`grep -c '&&'` in the raw response — if the doubled-entity count is nonzero,
the source file has the bug. A quick sed fix:
```bash
sed -i "s/&amp;&amp;/\&\&/g" path/to/page.xhtml
```
(safe because it only touches doubled `&amp;&amp;`, leaving legitimate single
`&amp;` — e.g. in URL query strings or "Bills &amp; Appointments" body
text — untouched).

Found while verifying issue #22370 (Client Portal login/password-reset):
two new pages copied `register_phone.xhtml`'s OTP-digit-box `<script>` block,
and the copy silently escaped `&&` to `&amp;&amp;`. The OTP boxes never
rendered and the browser console showed `initOtpBoxes is not defined` and
`startOtpCountdown is not defined` — errors that look like a missing/renamed
JS function, not a stray HTML entity three screens away in the same script tag.

## 46. A fixed-position status banner (e.g. "Database Migration Pending") can silently swallow every click on the page below it

When a global banner is rendered with fixed/sticky positioning and no
`pointer-events: none`, Playwright's actionability check reports the target
element as "visible, enabled and stable" and still fails the click with
`<div class="nonPrintBlock">…</div> intercepts pointer events` — this can hit
*any* element on the page, not just ones physically near the banner, if the
banner's box overlaps them in the stacking order. Real symptoms seen while
verifying issue #22415 (Custom Bills tab): clicking a `p:tabView` tab header
and a `p:commandButton` both timed out this way, even though the elements
themselves were correctly rendered and enabled.

Standard fixes (Escape, clicking a neutral area first, waiting) don't help
because the banner isn't a transient overlay (like a datepicker popup) — it's
a permanent part of the page layout. The reliable workaround is to bypass
Playwright's actionability gate entirely and dispatch the click straight to
the element via `browser_evaluate`:

```js
() => { document.getElementById('theActualElementId').click(); return 'clicked'; }
```

Get the id from the failed click's error output (it echoes the resolved
locator's outer HTML, e.g. `id="j_idt524:j_idt867:j_idt3539_header"`). This
is a real accessibility gap worth fixing in the banner itself (add
`pointer-events: none` unless the banner has its own interactive controls,
or `z-index`/positioning that keeps it from overlapping page content) — but
until that's fixed, `browser_evaluate` + `.click()` is the dependable way to
drive the page underneath it.

## 47. Any JSF page under a plain (non-`/faces/`) webapp path must still be loaded through `/faces/` — otherwise the raw `.xhtml` source is served unprocessed

`FacesServlet` is mapped to `/faces/*` in `web.xml` (`<url-pattern>/faces/*</url-pattern>`).
Requesting `http://localhost:8080/rh/client_portal/login.xhtml` directly (no `/faces/`
segment) does **not** 404 — the container serves the file as a static resource, so the
page loads with a real `<title>`, but every EL expression renders as literal text
(`#{clientPortalLoginController.login}`, `#{bean.property}` etc.) and there are **zero**
`<input>` elements in the DOM (Facelets never ran, so `p:inputText`/`h:commandButton`
components were never compiled to HTML). This looks like a broken page at first glance —
confirmed via issue #22371 verification, where `register_phone.xhtml` initially appeared
to have no input fields at all. Always use `/rh/faces/<same-path>.xhtml` for any new page
under `src/main/webapp/`, matching the pattern already used for `client_portal/login.xhtml`
→ `/rh/faces/client_portal/login.xhtml`. (JSF's own `action`/`outcome` navigation strings
like `"/client_portal/home?faces-redirect=true"` and `<h:link outcome="/client_portal/login"/>`
already resolve to the correct `/faces/`-prefixed URL automatically — this gotcha only
bites when a human or a script types the URL by hand.)

## 48. Raw SQL `UPDATE` on an already-cached EclipseLink-mapped entity (not just `ConfigOption`) can be invisible to the running app — the shared L2 cache is general, not `ConfigOption`-specific

§26 documents this for `ConfigOption` specifically, but `eclipselink.cache.size.default`
in `persistence.xml` applies to every entity class, so the same trap exists for `Institution`,
`Patient`, or any other frequently-read entity **once that row has already been loaded into
the shared L2 cache during the current app run** — visibility depends on persistence-context/
cache state, not a blanket guarantee that every raw SQL update is invisible. Hit while
verifying issue #22371: a direct `UPDATE INSTITUTION SET DEFAULTINSTITUTION=1,
POINTOFISSUENO='COOP' WHERE id=2` via the `mysql` CLI changed the DB row, but the next page
load still showed the old (unset) values in the edit form and the PHN-generation code path
still saw a blank POI — the already-cached `Institution` entity in the running Payara instance
kept serving stale field values, with no error anywhere. Re-doing the exact same change through
the admin UI form (Save button, which goes through `EntityManager.merge`/`edit`) fixed it
immediately, confirming the raw SQL path was the problem. **Rule of thumb: if a row might
already be cached (anything read earlier in the same test session), don't `UPDATE` it via raw
SQL mid-test — use the corresponding admin UI/CRUD screen instead, so the cache gets properly
refreshed, and reserve raw SQL for read-only verification queries.**

## 49. Pharmacy Transfer Issue: "Request From" is the requester, not the issuer — and the entity-based `TransferIssueForRequestsController` "Issue" button is commented out in favor of the native-SQL path

Two traps found verifying issue #19168's Transfer Issue Department Type filter fix:

- On `pharmacy_transfer_request.xhtml`, "Request From: X / Request To: Y" means
  **X is requesting stock FROM Y** — Y is the department that later approves and
  issues. Logging in as X and searching "Issue for Requests" after approval shows
  nothing ("No records found.") because the issue action belongs to Y's session,
  not X's. Switch department (§17) to Y before expecting the request to appear
  in "Select Request For Department: Y".
- `pharmacy_transfer_request_list.xhtml`'s `p:commandButton` calling
  `transferIssueForRequestsController.navigateToPharmacyIssueForRequestsById`
  (id `btnToIssue`) is commented out in the current XHTML — the active "Issue"
  button now calls `transferIssueNativeSqlController.navigateToIssueRequestNative()`
  (`pharmacy_transfer_issue_native.xhtml`, the "Fast Issue" path) instead. The
  entity-based controller class and its tests/fixes still exist and matter (the
  comment says it can be re-enabled if the native path shows data-correctness
  issues), but it is **not reachable through today's UI** — don't expect a code
  change there to be exercisable via a normal click-through without first
  re-enabling that button. Confirm which controller a page's button actually
  wires to (`grep` the `.xhtml` for the bean name) before planning an E2E pass
  around it, rather than assuming the "obvious" controller for a named flow.
- If the department picked as issuer has zero stock for the item under test
  (common for a secondary pharmacy like OPD Pharmacy in local seed data), the
  Fast Issue page renders `Available Stock: 0.00` and blocks entering an issue
  qty. Switch to the department that actually holds stock (usually Main
  Pharmacy) and use **Direct Issue** (`pharmacy_transfer_issue_direct_department.xhtml`,
  `TransferIssueDirectController`) instead — it doesn't require a prior
  approved request and reaches the same `Bill.departmentType` stamping logic.

## 50. `p:calendar`/`p:selectOneMenu` widgets can silently ignore a plain Playwright `fill()`/`click()` — drive the PrimeFaces widget JS API directly when the visible value won't stick

Hit while verifying issue #22414 (blocking Hold on an already-paid professional
fee), which needed a specific old BHT found by widening a search page's date
filter and switching its payment-method dropdown off "Cash" (no cash-drawer
balance locally):

- **`p:calendar`**: `browser_type`/`.fill()` on the visible text input updates
  the DOM, but on some pages the value silently reverts to today's date after
  the next postback (`inward_search_professional_payment_due.xhtml` did this;
  `inpatient_search.xhtml`'s calendar accepted `.fill()` normally — behavior
  isn't consistent across pages, so don't assume either way). If a submitted
  search comes back with unexpectedly narrow/empty results right after typing
  a date, suspect this before suspecting the query. Fix: drive the widget
  directly via `browser_evaluate`:
  ```js
  const w = PrimeFaces.widgets['widget_<id_with_colons_as_underscores>'];
  w.setDate(new Date(2020,0,1,0,0,0));
  w.input.val('01 Jan 2020 00:00:00').trigger('change');
  ```
  Find the widget name with
  `Object.keys(PrimeFaces.widgets).filter(k => k.includes('<idFragment>'))`.
- **`p:selectOneMenu`**: setting the underlying native `<select>`'s `.value`
  directly (even to a matching `<option value>`) does not reliably update the
  PrimeFaces display label — it can silently resync to a stale/wrong option.
  What actually works is clicking the real `<li>` inside the (JS-rendered,
  `display:none` until opened) `..._panel` element:
  ```js
  const panel = document.getElementById('<id_with_colons>_panel');
  const li = Array.from(panel.querySelectorAll('li')).find(li => li.textContent.trim() === 'Cheque');
  li.click();
  ```
  This fires the widget's real `itemClick` handler, which updates both the
  hidden select and the visible label consistently.
- Both patterns require the target element to actually exist in
  `PrimeFaces.widgets` first — a plain `browser_click` to open the dropdown
  panel beforehand isn't necessary once you're driving it via JS, but doing a
  quick `browser_snapshot` after any of this is worth it to confirm the
  visible label actually changed before submitting the form.
- Confirmed again while verifying issue #22649 (OPD Itemized Sales Summary
  cancellation-doubling report): `itemized_sale_summary_dto.xhtml`'s From/To
  `p:calendar` inputs silently reverted to today's date after `.fill()`, every
  time — as soon as the *other* date field (or any other input on the page)
  was touched next, both fields resnapped to their pre-fill value. If you'd
  rather not reach for `browser_evaluate`/widget internals, driving the actual
  calendar UI works just as reliably: click the input to open its popup, click
  the "Previous"/"Next" month arrows (found via `browser_find` for the visible
  month/year text, since the arrows' refs change every re-render) until the
  target month is showing, then click the day-number link. This sets the
  widget's real internal Date object (unlike a raw `.fill()`), so the value
  survives subsequent postbacks/field changes. Note this page has no
  `showTime`/`timeInput` attribute, so clicking a day only changes the date —
  the time-of-day stays whatever that field's default already was (00:00:00
  for From, 23:59:59 for To here). If a test needs the submitted range to land
  on a specific time or cross midnight, pick the From/To *days* accordingly
  (e.g. From = day N 00:00:00, To = day N+1 23:59:59) rather than assuming the
  time resets.
- Separately: local test data can have **zero** BillFee rows with
  `paidValue == feeValue` (nobody has ever settled a professional payment
  through this exact local DB copy) — check with a quick SQL count before
  assuming a "must find an already-paid row" test fixture exists; if it
  doesn't, settle one through the real UI first (Search Outstanding
  Professional Payments → select one row only → Settle) rather than writing
  `paidValue` via raw SQL, so the whole flow is genuinely exercised. Settling
  with "Cash" fails locally with "Not enough cash in your drawer" — switch
  Payment Method to Cheque/Card/Slip/ewallet (whichever needs no drawer
  balance) to unblock the settlement without needing a funded cash drawer.

## 51. `p:dialog appendTo="@(body)"` silently drops that dialog's own bound inputs from every AJAX submission

A `p:dialog` with `appendTo="@(body)"` gets physically relocated by PrimeFaces
to be a direct child of `<body>` in the DOM — taking it **outside** whatever
`<h:form>` it's declared inside in the JSF source. Any `p:selectOneMenu`/
`p:inputText` inside that dialog that's bound via a normal `value="#{...}"`
expression (rather than captured through
`<f:setPropertyActionListener>`/an iteration var on a `p:dataTable` row) will
never have its value included in the form's AJAX POST body, because
PrimeFaces serializes the enclosing `<form>`'s actual DOM subtree, and the
dialog's inputs are no longer part of it. The request still looks legitimate
— `javax.faces.partial.execute` correctly lists the dialog's component IDs,
and the response comes back `200` with no exception — but the parameter
names for those specific inputs are simply absent from the POST body, so the
server-side bean properties they're bound to never get updated. Symptom:
"nothing happens" when clicking Save inside the dialog — a value the user
just typed silently reverts, with no error unless you also check the
`update` target's message component actually renders (see #32).

Confirmed by injecting an `XMLHttpRequest.prototype.send` hook via
`javascript_tool` to capture the real request body and diffing the parameter
names against a known-good submission from the same form (see issue
`#22352`'s `ward_pharmacy_bht_issue_request_bill.xhtml` "Edit / Substitute
Item" dialog). The working counterpart,
`pharmacy_bill_retail_sale_native.xhtml`'s `substituteDlg`, also uses
`appendTo="@(body)"` but avoids the problem entirely by using
`f:setPropertyActionListener` on the row's own "Replace" button instead of a
submitted form field — worth checking as the reference pattern before
assuming `appendTo` itself needs to be removed.

## 52. A non-AJAX search that looks "hung" in Playwright may actually be a real, still-running N+1 query — check a Payara thread dump before assuming the button is broken

Clicking a date-filtered `ajax="false"` Search button (e.g.
`SearchController.fillSavedTranserRequestBills()` behind
`pharmacy_transfer_request_list_search_for_approval.xhtml`'s "Search") can
time out on `browser_click` ("waiting for scheduled navigations to finish"),
and every subsequent `browser_snapshot`/`browser_evaluate`/`browser_tabs`
call on that page then also times out — indistinguishable, from Playwright's
side, from a broken client-side handler that never reaches the server (the
symptom described in §37). Opening a **fresh tab** in the same browser
context can even reproduce the identical hang on the very first click,
which looks like confirmation the page itself is broken.

It isn't, necessarily. Check `server.log` first for whether the request even
arrived — but a wide date range that doesn't filter by `billTypeAtomic`
(only by `billType`, so it pulls every PRE **and** approved/downstream bill
over the range) can trigger a classic EclipseLink N+1: one `ReadAllQuery` for
the bill list, then a lazy `OneToOneMapping`/`ForeignReferenceMapping` round
trip **per row per relationship** (`fromDepartment`, `toDepartment`,
`creater`, `checkedBy`, …). Over hundreds of matching rows this is genuinely
slow (multiple minutes), not stuck — but produces no new `server.log` lines
if that code path (unlike the heavily-instrumented login flow) has no
`LOGGER.log(...)` trace statements, making "no new log output" look like
proof the request never arrived when it actually is just quiet.

**Definitive diagnostic**: `asadmin generate-jvm-report --type=thread` prints
straight to stdout (no file to locate). Payara's report format — unlike a raw
`jstack` dump — puts each thread's name and state on one physical line (e.g.
`Thread "http-thread-pool::http-listener-1(2)" thread-id: 75 thread-state:
RUNNABLE Running in native`), so a single-line `grep -A3
'http-thread-pool.*RUNNABLE'` reliably catches it and the frames below. A
thread whose stack shows your controller method (e.g.
`TransferRequestController.navigateToApproveRequest`) blocked in
`SocketInputStream.socketRead0` under
`com.mysql.cj.protocol...`/`EclipseLink` frames is **genuinely executing** —
not stuck. `SELECT ... FROM information_schema.PROCESSLIST WHERE
COMMAND='Query' AND ID != CONNECTION_ID()` corroborates this (a short-lived
but constantly-refreshing row is the N+1 loop grinding through rows, not a
single frozen query).

**Fix for testing purposes**: don't fight the browser hang — stop issuing
more clicks/tabs. Each retry adds another slow in-flight request; enough of
these piling up can exhaust server thread-pool, session, or DB connection
capacity, making *every* tab/request against that origin appear to hang, even
unrelated ones. Instead, narrow the date filter to the single day the
target record was created before searching, which keeps the row count (and
therefore the N+1 fan-out) small enough to return in a couple of seconds.
The wide-range search's result **does** eventually land in the session-scoped
searchController.bills once it finishes, so a plain navigate-away-and-back
on a fresh tab can pick up the now-populated list without re-submitting.
Verified while testing issue #22455 (Pharmacy Transfer Request Approval —
`pharmacy_transfer_request_list_search_for_approval.xhtml` and its twin
`pharmacy_transfer_request_list_to_approve.xhtml`, both driven by the same
session-scoped `SearchController`).

## 53. "Pharmacy Bill Search by Bill Type" has two different pages — pick the one keyed on `billType`, not `billTypeAtomic`

Two separate JSF pages both claim to be the pharmacy bill-type search: `pharmacy/pharmacy_search.xhtml`
(dropdown bound to `searchController.billType`, the plain `BillType` enum) and
`pharmacy/pharmacy_search_by_bill_type_atomic.xhtml` (dropdown bound to `searchController.billTypeAtomic`,
the finer-grained `BillTypeAtomic` enum). Both render a "Pharmacy Bill Search" panel with a Bill Type
dropdown, so it's easy to land on the wrong one and see misleading results. For
`PHARMACY_RETURN_WITHOUT_TREASING`/`PharmacyReturnWithoutTraising` specifically, the atomic-driven page is
**dead for this bill type**: `BillTypeAtomic.PHARMACY_RETURN_WITHOUT_TREASING`'s constructor declares its
associated `BillType` as `PharmacySale` (not `PharmacyReturnWithoutTraising`), so the atomic page's
`rendered="#{searchController.billTypeAtomic.billType eq 'PharmacyReturnWithoutTraising'}"` panel can never
match — selecting "Pharmacy Return without a Receipt" there silently falls through to an unrelated stale
"SALE BILL SEARCH" panel showing "No Bills Found", with no error. Grep
`BillTypeAtomic.java` for other atomics whose declared `BillType` doesn't match their own name before trusting
the atomic-based search page for a given bill type — `pharmacy_search.xhtml`'s plain-`billType` dropdown is
the reliable one when in doubt. Found verifying issue #22563.

## 54. `pharmacy_fast_retail_sale_for_cashier.xhtml` "Settle Bill At Cashier" 500s with a Patient cascade error if the Patient Name field is left blank

`PharmacyFastRetailSaleForCashierController.settlePreBill()` → `settlePharmacyToken()` creates a `Token`
referencing an in-memory `Patient` placeholder when no patient is selected/entered. Committing that
transaction throws `IllegalStateException: During synchronization a new object was found through a
relationship that was not marked cascade PERSIST: com.divudi.core.entity.Patient[ id=null ]`, rolling back
the whole "Settle Bill At Cashier" action with an HTTP 500 (unrelated to whatever feature is actually under
test). Always type something into "Enter the Name of the patient" before clicking "Settle Bill At Cashier"
on this page — a walk-in placeholder name is enough. Found verifying issue #21419.

Also for this page: "Settle Bill At Cashier" only creates a `PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER`
pre-bill and deducts stock — it does **not** create the final sale bill. To reach the actual
`PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER` bill (the one with a cancellable "To Cancel" button on
`pharmacy_reprint_bill_sale_cashier.xhtml`), separately go to `pharmacy_search_pre_bill.xhtml` → **Search
Not Paid Tokens** → **Call Customer** → **Accept Payment** → enter Tendered amount → **Accept Payment and
Settle**. Then from `pharmacy_search_pre_bill.xhtml` → **Search Paid Only Tokens** → **View Payment Bill**
lands on the reprint/cancel page for that bill.

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
