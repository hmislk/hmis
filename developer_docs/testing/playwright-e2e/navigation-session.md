# Playwright E2E — Menus, navigation, sessions, hung pages

Part of the [Playwright E2E Workflow](../playwright-e2e-workflow.md). Read only the section you need.

- [11. Session is lost on redeploy and on stale snapshots](#11-session-is-lost-on-redeploy-and-on-stale-snapshots)
- [14. Non-AJAX search buttons can timeout on click](#14-non-ajax-search-buttons-can-timeout-on-click)
- [16. List pages that filter by `toDepartment = session department`](#16-list-pages-that-filter-by-todepartment--session-department)
- [17. Switching session department mid-test (no redeploy)](#17-switching-session-department-mid-test-no-redeploy)
- [19. Some `confirm()`-guarded `type="submit"` buttons never reach the server — prefer existing data](#19-some-confirm-guarded-typesubmit-buttons-never-reach-the-server--prefer-existing-data)
- [28. Claude-in-Chrome on heavy non-AJAX report pages (full-submit + long query)](#28-claude-in-chrome-on-heavy-non-ajax-report-pages-full-submit--long-query)
- [32. A `FacesMessage` can be server-confirmed even when the browser never shows it](#32-a-facesmessage-can-be-server-confirmed-even-when-the-browser-never-shows-it)
- [36. `@SessionScoped` bean data can go stale after a direct URL hit — navigate through the real action chain](#36-sessionscoped-bean-data-can-go-stale-after-a-direct-url-hit--navigate-through-the-real-action-chain)
- [99. A leftover Playwright-MCP Chrome profile can lock out `browser_navigate` with no relation to the user's real browser windows](#99-a-leftover-playwright-mcp-chrome-profile-can-lock-out-browser_navigate-with-no-relation-to-the-users-real-browser-windows)
- [46. A fixed-position status banner (e.g. "Database Migration Pending") can silently swallow every click on the page below it](#46-a-fixed-position-status-banner-eg-database-migration-pending-can-silently-swallow-every-click-on-the-page-below-it)
- [47. Any JSF page under a plain (non-`/faces/`) webapp path must still be loaded through `/faces/` — otherwise the raw `.xhtml` source is served unprocessed](#47-any-jsf-page-under-a-plain-non-faces-webapp-path-must-still-be-loaded-through-faces--otherwise-the-raw-xhtml-source-is-served-unprocessed)
- [52. A non-AJAX search that looks "hung" in Playwright may actually be a real, still-running N+1 query — check a Payara thread dump before assuming the button is broken](#52-a-non-ajax-search-that-looks-hung-in-playwright-may-actually-be-a-real-still-running-n1-query--check-a-payara-thread-dump-before-assuming-the-button-is-broken)
- [57. `nurse/index.xhtml` (Nursing WorkBench) Rooms/BHT tabs render empty on a plain `browser_navigate` — must click through the actual menu link](#57-nurseindexxhtml-nursing-workbench-roomsbht-tabs-render-empty-on-a-plain-browser_navigate--must-click-through-the-actual-menu-link)
- [62. Direct `browser_navigate` to a fund-bill page (deposit/withdrawal/etc.) leaves `currentBill` null — the "+Add" button then silently no-ops with zero visible feedback](#62-direct-browser_navigate-to-a-fund-bill-page-depositwithdrawaletc-leaves-currentbill-null--the-add-button-then-silently-no-ops-with-zero-visible-feedback)
- [64. A slow, unfiltered `p:dataTable` search can make *every* subsequent Playwright tool call time out — don't `browser_navigate` away to "recover", just wait longer](#64-a-slow-unfiltered-pdatatable-search-can-make-every-subsequent-playwright-tool-call-time-out--dont-browser_navigate-away-to-recover-just-wait-longer)
- [89. A missing `/faces/` prefix can also hang the Playwright tab itself, not just serve broken markup — don't assume item 47's symptom is the only failure mode](#89-a-missing-faces-prefix-can-also-hang-the-playwright-tab-itself-not-just-serve-broken-markup--dont-assume-item-47s-symptom-is-the-only-failure-mode)
- [106. A third-level menu item can't be clicked directly — fire its own `onclick`, which is still the menu path, not URL navigation](#106-a-third-level-menu-item-cant-be-clicked-directly--fire-its-own-onclick-which-is-still-the-menu-path-not-url-navigation)
- [114b. PrimeFaces menubar flyouts close between MCP tool calls — click the leaf `<a>` in one `browser_evaluate`](#114b-primefaces-menubar-flyouts-close-between-mcp-tool-calls--click-the-leaf-a-in-one-browser_evaluate)
- [127. A `position: fixed` bottom banner eats clicks on dialog buttons — and dialogs on some pages pin their bottom to the window bottom](#127-a-position-fixed-bottom-banner-eats-clicks-on-dialog-buttons--and-dialogs-on-some-pages-pin-their-bottom-to-the-window-bottom)
- [136. Forcing `.click()` on a still-hidden PrimeFaces menu leaf (parent flyout never actually opened) can crash the JSF view state and log you out](#136-forcing-click-on-a-still-hidden-primefaces-menu-leaf-parent-flyout-never-actually-opened-can-crash-the-jsf-view-state-and-log-you-out)

---

## 11. Session is lost on redeploy and on stale snapshots

- **A redeploy invalidates the session** (already noted in §1) — re-login and
  re-select department before continuing.
- If a click navigates somewhere unexpected (e.g. `about:blank` or a
  `TypeError: Cannot read properties of undefined (reading 'url')`), the page
  state and your last `browser_snapshot` have diverged. Re-navigate to the app
  root (`/rh`), log in again, and re-take a snapshot before continuing — don't
  keep issuing actions against stale `ref=` values.


## 14. Non-AJAX search buttons can timeout on click

When a JSF search button triggers a full page reload (non-AJAX, `ajax="false"`),
`browser_click` may time out with "waiting for scheduled navigations to finish"
if the response is slow. The click usually succeeds — use `browser_snapshot`
after the timeout to check the new page state rather than assuming the action
failed. If stuck, `browser_navigate` directly to the page URL to recover.


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


## 99. A leftover Playwright-MCP Chrome profile can lock out `browser_navigate` with no relation to the user's real browser windows

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


## 57. `nurse/index.xhtml` (Nursing WorkBench) Rooms/BHT tabs render empty on a plain `browser_navigate` — must click through the actual menu link

`inward/nurse/index.xhtml` populates its Rooms/BHT tab lists (room and BHT
buttons per ward) only when reached via the real PrimeFaces menu action
(**Inward → Nursing WorkBench**, an `onclick`/`PrimeFaces.addSubmitParam`
command link that posts a form before navigating). A `browser_navigate`
straight to `/rh/faces/nurse/index.xhtml` — even from an already-authenticated,
department-selected session — loads the page shell but leaves both tab panels
empty, with no console error and no failed network request to explain it; the
list is populated by server-side controller init tied to the menu's action
listener, not by a `f:viewAction` or ajax poll that a plain GET would trigger.
Same rule as the admission/final-bill pages noted in §1 §17: prefer clicking
through the actual menu path over guessing the URL, and if a page you reached
by URL shows a suspiciously empty list with no error, retry via the menu link
before assuming the data itself is missing. Verified while testing issue
`#22689`. Note: `NursingWorkBenchController.loadLists()` populates the Rooms
and BHT tabs from the identical query (same `discharged=false /
paymentFinalized=false / currentPatientRoom` filter) — they always list the
same admissions, just labeled/sorted by room name vs. BHT number
respectively. If a specific admission seems "missing" from one tab, search by
the label that tab actually renders (BHT number on the BHT tab, room name on
the Rooms tab), not by patient name — neither tab's buttons show it.


## 62. Direct `browser_navigate` to a fund-bill page (deposit/withdrawal/etc.) leaves `currentBill` null — the "+Add" button then silently no-ops with zero visible feedback

On `cashier/fund_withdrawal_bill.xhtml` (and the same pattern likely applies
to `deposit_funds.xhtml` and other `FinancialTransactionController` fund-bill
pages), navigating straight to the page URL skips the menu action method
(`navigateToCreateNewFundWithdrawalBill()` → `prepareToAddNewWithdrawalProcessingBill()`)
that initializes the `@SessionScoped` bean's `currentBill`/`currentBillPayments`.
The page still renders fully — Payment Method dropdown, Value field, "+Add"
button all present and clickable — but `addPaymentToWithdrawalFundBill()`
starts with `if (currentBill == null) { JsfUtil.addErrorMessage("Error"); return; }`,
and the page has no `<p:messages>`/`<h:messages>` bound, so the growl error
never renders. Clicking "+Add" just re-shows the same empty payment fields
with **no error, no added row, no total change** — indistinguishable from a
Playwright click/ref problem unless you check the "Withdrawal List" /
"Deposits to Submit" table state after the click. Fix: always reach these
pages via **Drawer tab → Withdrawals/Deposit to Safe/Bank button**, not
`browser_navigate` to the URL directly — same root cause as §57, but here the
consequence is a silent no-op rather than an empty page. Found while
verifying issue #22870.


## 64. A slow, unfiltered `p:dataTable` search can make *every* subsequent Playwright tool call time out — don't `browser_navigate` away to "recover", just wait longer

On `opd_search_professional_payment_due.xhtml` ("OPD Payments Due Search"), the `ajax="false"`
Search button runs an unindexed JPQL join across `BILLFEE`/`BILL`/`STAFF`/`PERSON` with no
department scoping. A wide date range with no name filter can run long enough that
`browser_click`'s "waiting for scheduled navigations to finish" times out (5s), and every
following `browser_snapshot`/`browser_wait_for` also times out (30s) because the page is still
mid-navigation — this looks identical to a wedged browser session. **Do not `browser_navigate` away
to recover in this situation**: a plain GET reload creates a fresh request and abandons whatever
the slow POST was about to render, so the search results are lost even though the query eventually
would have completed server-side (confirmed via `mysql.general_log` — the correct query, with the
correct bind parameters, executed and matched rows, but the client never saw the response and a
subsequent GET showed stale/empty state instead). Instead, once the click has already timed out,
stay on the page and retry `browser_snapshot` after a real wait (`sleep 20` via Bash, not a tool
`time` argument — those get capped short); the page does eventually render with results. Narrowing
the date range and adding a name filter before clicking Search avoids the slow path entirely and
should be preferred when the target staff/date are already known. Found while verifying issue #22860.


## 89. A missing `/faces/` prefix can also hang the Playwright tab itself, not just serve broken markup — don't assume item 47's symptom is the only failure mode

Addendum to item 47, found while testing issue #23407. Item 47 established
that a missing `/faces/` segment makes Payara serve the raw Facelets source as
a static file — confirmed here too: `curl -D-` against
`http://localhost:8080/rh/inward/inward_bill_outside_charge.xhtml` (no
`/faces/`) returns instantly with `HTTP/1.1 200`, `Content-Type:
application/xhtml+xml`, and the literal unprocessed `<ui:composition>` source
— consistent with item 47, and **not** a server-side hang; `TransactionLeakGuardFilter`
(the only `/*`-mapped filter in `web.xml`) only runs cleanup after the chain
completes and cannot cause this either.

But driving the same URL through `browser_navigate` in an authenticated
Playwright session did not render item 47's "broken page with literal EL
text" — the tab hung indefinitely instead, with `browser_navigate` and every
subsequent `browser_snapshot` timing out at 30s and zero new lines appended to
`server.log` in the meantime (the server had already answered; the hang was
client-side). The exact trigger wasn't isolated further — possibly Chromium's
handling of a non-`text/html` `application/xhtml+xml` response during a
Playwright-driven navigation — but don't burn time debugging the server or
checking Payara/system health when this happens, since the server side is
demonstrably fine either way. Close the hung tab (`browser_tabs` → `close`),
open a fresh tab, and navigate to
`http://localhost:8080/rh/faces/inward/inward_bill_outside_charge.xhtml`
(with `/faces/`) instead — this always loads normally. Any local deep-link
into an inner page (bypassing a menu click) needs the `/faces/` segment
regardless of which of the two symptoms it would otherwise hit.


## 106. A third-level menu item can't be clicked directly — fire its own `onclick`, which is still the menu path, not URL navigation

`Inpatient → Billing → Interim Bill` is three levels deep. Clicking the top-level item opens the
second level, but the third-level flyout closes again before Playwright can reach the item:
`click`, `hover`, and `:has-text()` selectors all fail with *element is not visible*, and clicking
the parent a second time just toggles the whole menu shut.

**Do not fall back to typing the page URL** — §2 explains why that produces false findings. Instead
invoke the menu item's own anchor:

```js
() => { const a = Array.from(document.querySelectorAll('.ui-menubar a'))
          .find(x => x.textContent.trim() === 'Interim Bill');
        a.click(); }
```

This is genuinely equivalent to a user's click, not a shortcut around it. The anchor carries the
menu's own handler —
`PrimeFaces.addSubmitParam('...:menuForm', {...}).submit('...:menuForm')` — so the JSF action, and
therefore the `@SessionScoped` navigation method behind it, runs exactly as it does for a real
click. The page lands with its state populated, which is the whole point of the URL rule. Check the
anchor's `onclick` attribute first: if it submits the menu form, this is safe; if it is a plain
`href` to a page, you are back to URL navigation and must find another route.

Verified while testing issue #23543 (professional/assisting fee merge).

### A *second*-level flyout does open — but only from a CSS-selector click on the top-level anchor

Before reaching for the `onclick` escape hatch above, try this: a two-level menu path
(`Administration → Manage Lab Services`) opens normally, but only if the top-level item is clicked
through a **CSS selector**, not through its accessibility `ref`.

Clicking the ref sets `ui-menuitem-active ui-menuitem-highlight` on the `<li>` while the child
`<ul>` stays `display: none` — the item looks selected and nothing opens. `browser_hover` on either
the `<li>` or its `<a>` does nothing at all. Clicking the same anchor by selector opens it properly
(`display: block`, with the inline `z-index/top/left` PrimeFaces sets):

```
browser_click  .ui-menubar > .ui-menu-list > li:nth-child(20) > a
browser_click  .ui-menubar > .ui-menu-list > li:nth-child(20) a:has-text("Manage Lab Services")
```

Confirm it actually opened before clicking the child, rather than eating a 5s timeout:

```js
() => getComputedStyle(document.querySelectorAll('.ui-menubar > .ui-menu-list > li')[19]
        .querySelector('ul')).display   // 'block' once open
```

Finding the right `nth-child` is itself awkward, because most top-level items are icon-only with
their label in visually-hidden markup — `browser_snapshot` shows them as bare `menuitem` entries
with no name. List them with their index first:

```js
() => Array.from(document.querySelectorAll('.ui-menubar > .ui-menu-list > li'))
        .map((li,i) => ({i, text: (li.innerText||'').trim().split('\n')[0]}))
```

Verified while testing issue #23529 (common report template API), reaching
*Administration → Manage Lab Services → Report Templates → Report Format Templates*.

### Scope the anchor lookup to its own submenu — menu labels repeat

The `onclick` recipe above searches every anchor in the menubar for an exact label:

```js
Array.from(document.querySelectorAll('.ui-menubar a')).find(x => x.textContent.trim() === '…')
```

That is fine for a label that happens to be unique, but labels are **not** unique within a
single top-level menu, and `find()` returns the first match with no warning. The Inpatient menu
alone carries *Admissions* twice — once as the group header at the top, and again under *Search*
as the admission-search screen. A global lookup for `'Admissions'` silently picks the header,
which is not a navigable item, so the click does nothing and looks like the flyout problem all
over again.

Scope the search to the submenu that actually contains the item:

```js
() => { const top = document.querySelectorAll('.ui-menubar > ul > li')[3];   // Inpatient
        const sub = top.querySelectorAll('ul')[4];                            // Services & Items
        const a = Array.from(sub.querySelectorAll('a'))
                    .find(x => x.textContent.trim().startsWith('Add Services & Investigations'));
        a.click(); }
```

Find the submenu index by listing each `<ul>` with its first few children, rather than counting
them by eye in the markup:

```js
() => Array.from(document.querySelectorAll('.ui-menubar > ul > li')[3].querySelectorAll('ul'))
        .map((u,i) => i + ': ' + Array.from(u.children).slice(0,3)
              .map(c => (c.innerText||'').trim().split('
')[0]).join(' | '))
```

Two things that cost time here:

- **Prefer `startsWith` over `===`.** The anchor wraps an icon `<span>` alongside the label span,
  so its `textContent` is not always just the label.
- **Every click toggles.** If you mix an in-page `a.click()` with a `browser_click` on the same
  top-level anchor, the second one closes what the first opened, and the flyout reads as "won't
  open" when it is simply shut again. Re-check `getComputedStyle(...).display` after *each* click
  rather than assuming a click opens.

Verified while testing issue #23570 (day-case admissions billed without a room), reaching
*Inpatient → Services & Items → Add Services & Investigations*.


## 114b. PrimeFaces menubar flyouts close between MCP tool calls — click the leaf `<a>` in one `browser_evaluate`

The main menu's nested submenus (e.g. *Inpatient → Services & Items → Add Timed
Services*) are `autoDisplay="false"`, so they open on **click**, not hover —
`browser_hover` leaves the parent `ui-menuitem-active` but the child list stays
`display: none`. Worse, each Playwright tool call is a fresh round trip, and the
flyout collapses in between: opening the top level in one call and reaching for
the leaf in the next always fails with *"element is not visible"*, and clicking
the parent again just toggles it shut.

Driving it click-by-click is not worth the fight. Invoke the leaf item's own
handler in a single call:

```js
browser_evaluate(() => {
  Array.from(document.querySelectorAll('.ui-menubar a'))
    .find(a => a.textContent.trim() === 'Add Timed Services')
    .click();
});
```

This is **not** the same as URL navigation and does not violate §2: the anchor's
`onclick` is `PrimeFaces.addSubmitParam(...).submit('menuForm')`, so the menu
form posts exactly as it would for a user and the `@SessionScoped` navigation
method runs normally. You are reproducing the click, not skipping it. Still
record the human menu path in the issue/PR.

Related: menubar items are icon-only with no accessible name, so
`browser_snapshot` shows a wall of anonymous `menuitem` nodes. To map them,
read the submenu text rather than guessing:

```js
browser_evaluate(() => Array.from(document.querySelectorAll('.ui-menubar > .ui-menu-list > li'))
  .map((li, i) => i + ': ' + Array.from(li.querySelectorAll('.ui-menu-child a'))
    .slice(0, 4).map(a => a.textContent.trim()).join(' / ')).join('\n'));
```


## 127. A `position: fixed` bottom banner eats clicks on dialog buttons — and dialogs on some pages pin their bottom to the window bottom

The local "Database Migration Pending — Missing fields or tables detected."
banner is `position: fixed; z-index: 9999` at the bottom of the viewport
(`div.nonPrintBlock`). Playwright reports:

```
<div class="nonPrintBlock">…</div> intercepts pointer events
```

…for any dialog button that lands in the bottom ~41px, and retrying or resizing
the window does not help, because several HMIS dialogs position their **bottom
edge at the window bottom** rather than centring (observed on
`pharmacy_transfer_request_approval.xhtml` for both its config dialog and a
newly added one — so it is page behaviour, not something a new dialog
introduces).

Hide the banner for the duration of the test — it only renders because the local
DB has pending migrations, so hiding it reproduces what a migrated environment
looks like rather than masking a defect:

```js
document.querySelectorAll('.nonPrintBlock').forEach(e => e.style.display = 'none');
```

Worth a second thought before dismissing it, though: if a dialog's buttons sit
that low, they can also be clipped on a genuinely short laptop viewport. Check
the dialog's `getBoundingClientRect()` against `innerHeight` before concluding
it is purely a local-banner artifact.


## 136. Forcing `.click()` on a still-hidden PrimeFaces menu leaf (parent flyout never actually opened) can crash the JSF view state and log you out

Reaching a deeply-nested menu item (e.g. *Settings → Manage My API Keys*, itself two levels under an
icon-only top-bar menu) by grabbing its `<a class="ui-menuitem-link">` out of a flat
`querySelectorAll` index and firing `.click()` on it directly — without first opening every ancestor
`<li>` so the leaf is actually `display: block` — still submits the menu's JSF form (the `onclick`
still runs `PrimeFaces.addSubmitParam(...).submit(...)`, same as the §106 escape hatch), but against a
component tree the client never actually rendered as open. On this codebase that didn't just no-op: it
came back `HTTP 500 View ... could not be restored`, and the next page load landed on the raw login
form — the whole session was invalidated, not merely the click ignored. This is a step beyond the
"just doesn't work" failures in §106/§114: it's destructive. Any unsaved in-progress form state
elsewhere on the same page (e.g. bill items staged but not yet Settled) is lost with it.

Fix: open every ancestor level for real — `browser_hover` each parent `<li>`'s own anchor in turn,
confirming `getComputedStyle(...).display === 'block'` on its child `<ul>` before going one level
deeper — so the leaf becomes genuinely visible, then click it normally by CSS selector. Never skip
straight to `.click()` on an element whose `offsetParent` is `null`; verify visibility first, the same
way §106's second-level note already recommends confirming `display: block` before descending.

Verified 2026-09-29 while trying to reach *Settings → Manage My API Keys* on the Ruhunu local-staging
server (`rh-local-staging` branch) mid-investigation of an inward pharmacy margin bug.
