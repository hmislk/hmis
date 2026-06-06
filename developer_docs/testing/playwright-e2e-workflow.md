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

**Use real key events instead:**

- **Autocomplete:** type the query slowly with `pressSequentially` (or
  `browser_type` with `slowly: true`) → wait ~1.5 s for the suggestion panel →
  `ArrowDown` → `Enter` to select the highlighted suggestion. Focus then
  auto-advances (e.g. to the quantity field).
- **Quantity / numeric fields:** type slowly, then **wait ~1 s** so the
  keyup-AJAX commits the bound value **before** you click the Add/Save button.
  Clicking immediately races the AJAX and submits a stale/empty value.
- **Add a row reliably:** type item (select via Enter) → type qty slowly →
  wait → click the Add button by its stable id (e.g. `#…:btnAddItem`).

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

## Quick checklist

- [ ] Confirmed environment + URL with the developer; credentials kept out of the repo.
- [ ] Logged in, selected a department, reached an inner page (menu visible).
- [ ] Clicked **Search** on every date-filtered list before expecting rows.
- [ ] Used real key events (slow type + wait) for autocompletes and qty fields.
- [ ] Handled `confirm()` dialogs; tested double-click on settle buttons.
- [ ] Filled required fields before non-AJAX actions.
- [ ] Verified stock + bill-item integrity in the DB; cleaned up temp files.
- [ ] Filed/fixed any accessibility gap that blocked the test.
