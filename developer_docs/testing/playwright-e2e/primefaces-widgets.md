# Playwright E2E — PrimeFaces widgets and AJAX in tests

Part of the [Playwright E2E Workflow](../playwright-e2e-workflow.md). Read only the section you need.

- [9. Interacting with non-accessible canvas widgets (e.g. `p:timeline` / vis-timeline)](#9-interacting-with-non-accessible-canvas-widgets-eg-ptimeline--vis-timeline)
- [10. `browser_click` / `browser_type` parameter name](#10-browser_click--browser_type-parameter-name)
- [12. JSF form validation blocks navigation buttons](#12-jsf-form-validation-blocks-navigation-buttons)
- [13. PrimeFaces `p:selectOneMenu` is not a native `<select>`](#13-primefaces-pselectonemenu-is-not-a-native-select)
- [18. `p:datePicker` — changing the date via the calendar grid](#18-pdatepicker--changing-the-date-via-the-calendar-grid)
- [37. A required `p:selectOneMenu` with no default silently swallows an entire non-AJAX submit — no network request, no error](#37-a-required-pselectonemenu-with-no-default-silently-swallows-an-entire-non-ajax-submit--no-network-request-no-error)
- [50. `p:calendar`/`p:selectOneMenu` widgets can silently ignore a plain Playwright `fill()`/`click()` — drive the PrimeFaces widget JS API directly when the visible value won't stick](#50-pcalendarpselectonemenu-widgets-can-silently-ignore-a-plain-playwright-fillclick--drive-the-primefaces-widget-js-api-directly-when-the-visible-value-wont-stick)
- [56. `p:datePicker timeInput="true"` — typing into the input does not commit; use the PrimeFaces widget API for non-AJAX forms](#56-pdatepicker-timeinputtrue--typing-into-the-input-does-not-commit-use-the-primefaces-widget-api-for-non-ajax-forms)
- [68. PrimeFaces `p:growl` error/success messages fade before a subsequent `browser_snapshot`/`wait_for` — snapshot immediately after the triggering action, not after a delay](#68-primefaces-pgrowl-errorsuccess-messages-fade-before-a-subsequent-browser_snapshotwait_for--snapshot-immediately-after-the-triggering-action-not-after-a-delay)
- [71. A fresh `p:selectOneMenu` visually shows its first `<option>` as selected even when the bound model value is still `null` — clicking that same-looking option fires no `change` event](#71-a-fresh-pselectonemenu-visually-shows-its-first-option-as-selected-even-when-the-bound-model-value-is-still-null--clicking-that-same-looking-option-fires-no-change-event)
- [72. `p:calendar`'s `pattern` attribute can differ from the usual `dd/mm/yyyy` — type in the exact server-side pattern, not a guessed format](#72-pcalendars-pattern-attribute-can-differ-from-the-usual-ddmmyyyy--type-in-the-exact-server-side-pattern-not-a-guessed-format)
- [77. `p:autoComplete` with `<p:column>` children renders suggestions as a `<table>` of `<tr data-item-label>`, not `<li>` — a `li`-only selector reports "no matches" on a working autocomplete](#77-pautocomplete-with-pcolumn-children-renders-suggestions-as-a-table-of-tr-data-item-label-not-li--a-li-only-selector-reports-no-matches-on-a-working-autocomplete)
- [78. Typing into an input that a `p:ajax` just re-rendered prepends to the restored model value — set the dropdown first, then the numbers, and always confirm in the DB](#78-typing-into-an-input-that-a-pajax-just-re-rendered-prepends-to-the-restored-model-value--set-the-dropdown-first-then-the-numbers-and-always-confirm-in-the-db)
- [79. Capturing a `p:growl` message in a screenshot: suppress its auto-hide timer, don't race it](#79-capturing-a-pgrowl-message-in-a-screenshot-suppress-its-auto-hide-timer-dont-race-it)
- [80. A `p:dataTable` column present in `browser_snapshot` can still be 0px wide and invisible to the user — measure `offsetWidth`](#80-a-pdatatable-column-present-in-browser_snapshot-can-still-be-0px-wide-and-invisible-to-the-user--measure-offsetwidth)
- [90. Simulating a barcode scan with `browser_type(..., submit: true)` on a `p:autoComplete` can submit the wrong button — use `slowly: true` and wait, don't press Enter](#90-simulating-a-barcode-scan-with-browser_type-submit-true-on-a-pautocomplete-can-submit-the-wrong-button--use-slowly-true-and-wait-dont-press-enter)
- [94. PrimeFaces `p:datePicker` popups don't always close on their own — the previous field's panel can intercept clicks meant for the next field](#94-primefaces-pdatepicker-popups-dont-always-close-on-their-own--the-previous-fields-panel-can-intercept-clicks-meant-for-the-next-field)
- [101. Some PrimeFaces buttons need a jQuery-triggered click](#101-some-primefaces-buttons-need-a-jquery-triggered-click)
- [105. A `p:calendar` bound to Date of Birth can ignore real keystrokes — use the widget's `setDate()` API](#105-a-pcalendar-bound-to-date-of-birth-can-ignore-real-keystrokes--use-the-widgets-setdate-api)
- [108. This dashboard's Sample Transporter `p:autoComplete` ignores synthetic keystrokes — drive `widget.search()` directly](#108-this-dashboards-sample-transporter-pautocomplete-ignores-synthetic-keystrokes--drive-widgetsearch-directly)
- [109. A `p:confirm` dialog is `position: fixed`, so an `offsetParent` visibility probe wrongly reports it hidden — the click did work](#109-a-pconfirm-dialog-is-position-fixed-so-an-offsetparent-visibility-probe-wrongly-reports-it-hidden--the-click-did-work)
- [113. `p:tag` silently drops `title` — a tooltip on a tag needs `p:tooltip`](#113-ptag-silently-drops-title--a-tooltip-on-a-tag-needs-ptooltip)
- [116. `p:datePicker` with a mask silently truncates `pressSequentially`](#116-pdatepicker-with-a-mask-silently-truncates-presssequentially)
- [117. `p:tabView` renders every tab's markup — a text-matched `browser_evaluate` click hits a hidden tab's copy](#117-ptabview-renders-every-tabs-markup--a-text-matched-browser_evaluate-click-hits-a-hidden-tabs-copy)
- [123. `reports/index.xhtml`'s report-category accordion needs the PrimeFaces widget API, not a plain click, to reliably expand a tab](#123-reportsindexxhtmls-report-category-accordion-needs-the-primefaces-widget-api-not-a-plain-click-to-reliably-expand-a-tab)
- [128. The department `p:selectOneMenu` is a *filterable, table-based* dropdown — there are no `<li>` items to click](#128-the-department-pselectonemenu-is-a-filterable-table-based-dropdown--there-are-no-li-items-to-click)
- [129. Pressing Enter to accept a *loaded* autocomplete suggestion also fires the page's `p:defaultCommand` — the action runs before you click its button](#129-pressing-enter-to-accept-a-loaded-autocomplete-suggestion-also-fires-the-pages-pdefaultcommand--the-action-runs-before-you-click-its-button)
- [131. `p:autoComplete` gives no suggestions to `fill`/`pressSequentially` — drive the widget's `search()`, and pick the right widget id](#131-pautocomplete-gives-no-suggestions-to-fillpresssequentially--drive-the-widgets-search-and-pick-the-right-widget-id)
- [137. `button[title="…"]` locator works once, then finds nothing](#137-buttontitle-locator-works-once-then-finds-nothing)

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


## 18. `p:datePicker` — changing the date via the calendar grid

To change a `p:datePicker` (with `timeInput="true"`) to a different day:
click the input to open the "Choose Date" dialog, then click the target day
cell in the calendar `grid` (refs like `gridcell "June 15"`). Typing into the
input directly is unreliable. After picking the date, the calendar overlay
can intercept subsequent clicks ("subtree intercepts pointer events") — click
a neutral element on the page first (e.g. a heading) to dismiss the overlay
before clicking Search.


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


## 56. `p:datePicker timeInput="true"` — typing into the input does not commit; use the PrimeFaces widget API for non-AJAX forms

On `theater/inward_timed_service_consume_surgery.xhtml`'s Start/End Time
fields (`p:datePicker showTime="true" timeInput="true"`, no `readonlyInput`
set — `input.readOnly` is `false`), the documented "click → Ctrl+A →
pressSequentially → Escape" pattern (§ "p:datePicker / p:calendar") left the
input **empty** every time: `document.getElementById(...).value` read `""`
both before and after `Escape`, with no visible error. Root cause wasn't
narrowed further, but the fix that reliably works is to skip DOM typing
entirely and drive the PrimeFaces widget directly — safe here because the
submit button (`+ Add Service`) is `ajax="false"`, so (per §29) only the
final submitted `_input` value matters:
```js
Object.keys(PrimeFaces.widgets).filter(k => /starttime|endtime/i.test(k))
// -> ["widget_form_startTime", "widget_form_endTime"]
PrimeFaces.widgets.widget_form_startTime.setDate(new Date(2026, 7, 5, 19, 0, 0));
```
`setDate()` both sets the widget's internal date **and** re-serializes the
visible `_input` text using the field's configured pattern, so a DOM read
right after confirms the committed value. Verified end-to-end for issue #20890:
the typed-looking string round-tripped correctly through the
non-AJAX submit and the saved `PATIENTITEM.FROMTIME`/`TOTIME` matched. Only
use this shortcut for non-AJAX (full-postback) submits — for an AJAX
`p:datePicker` where the *change* event itself must fire a listener, this
bypasses that and the real key-event pattern would still be required (untested
here).


## 68. PrimeFaces `p:growl` error/success messages fade before a subsequent `browser_snapshot`/`wait_for` — snapshot immediately after the triggering action, not after a delay

A validation error or success growl (e.g. from a blocked/allowed refund
submission) can disappear from the DOM within ~1-2 seconds. If you
`browser_wait_for` a couple of seconds and *then* snapshot, the growl may
already be gone even though the underlying action definitely ran (verify via
DB query if in doubt). To reliably capture the message as evidence, call
`browser_snapshot` (or `browser_take_screenshot`) right after
`browser_handle_dialog`/the click that triggers the AJAX response — do not
insert a `wait_for` in between when the growl itself is the thing being
captured. Verified while testing issue #22906.


## 71. A fresh `p:selectOneMenu` visually shows its first `<option>` as selected even when the bound model value is still `null` — clicking that same-looking option fires no `change` event

On `pharmacy_bill_retail_sale_native.xhtml`'s Payment Method dropdown (default
list order: Cash, Credit Card, Multiple Payment Methods, ...), a freshly
loaded/newly-logged-in page renders the native `<select>` showing "Cash" as
selected — this is just the browser defaulting an unset `<select>` to its
first `<option>`, not evidence that `paymentMethod` is actually bound to
`PaymentMethod.Cash` server-side. It's still `null` until the user makes a
real selection. `browser_click` on that already-visually-"Cash" option is a
no-op: the native select's `selectedIndex` doesn't change, so no `change`
event fires, so `p:ajax event="change"` never runs and any `rendered="#{bean.paymentMethod
eq 'Cash'}"` block downstream stays on its null-branch (nothing rendered)
even though the dropdown *looks* set to Cash. Symptom: fields that should
appear for Cash (e.g. Tendered/Balance) never show up, with no error and no
network request — easy to misdiagnose as a `rendered` condition bug in the
code when the page itself is correct.

**Fix**: to genuinely land on the visually-default option, select a
*different* option first, then select the desired one back — each of those
is a real change, so both `change` events fire and the bean field updates
both times:
```text
click dropdown → click a different option (e.g. "Credit Card")
click dropdown → click the target option (e.g. "Cash")
```
Only needed the first time a dropdown is touched in a fresh session/after a
redeploy-forced relogin; once a real change has fired once, subsequent
same-value clicks are fine since the model is no longer null. Found verifying
issue #22991.


## 72. `p:calendar`'s `pattern` attribute can differ from the usual `dd/mm/yyyy` — type in the exact server-side pattern, not a guessed format

`pharmacy/direct_purchase.xhtml`'s "Date of Expiry" field (`bill:calDoe`) is a
`p:calendar` with `pattern="dd MM yy"` — space-separated, numeric month, and a
**2-digit** year, not the more common `dd/mm/yyyy`. Typing a plausibly-formatted
date like `31-12-2027` gets silently rejected: the input renders with
`aria-invalid`/a red border, the value never converts to a real `Date`, and
the downstream action (`addItem()` here) hits its own `doe == null` validation
and no-ops with an error message — easy to misread as "the button doesn't
work" rather than "the date didn't parse". Symptom is worse than a normal
validation error because nothing about the on-screen state screams "wrong
format" unless you're specifically looking for the invalid-state styling.

**Fix**: read the `pattern="..."` attribute straight from the component's
source (`grep -n -A6 -B1 'id="calDoe"' src/main/webapp/pharmacy/direct_purchase.xhtml`)
before typing anything, and match it exactly — here, `31 12 27` (Ctrl+A, type slowly,
`Escape` to close the overlay without resetting the value, same pattern as
the general `p:calendar` guidance in §3). Don't assume `dd/mm/yyyy` just
because that's the most common pattern elsewhere in the app. Verified while
testing issue #23005.


## 77. `p:autoComplete` with `<p:column>` children renders suggestions as a `<table>` of `<tr data-item-label>`, not `<li>` — a `li`-only selector reports "no matches" on a working autocomplete

`.ui-autocomplete-panel li` is the right selector only for a plain autocomplete. As soon as the component declares
`<p:column>` children (the multi-column suggestion form used by the timed-service, admission and item pickers), the
panel renders a `<table class="ui-autocomplete-items ui-autocomplete-table">` whose rows are
`<tr id="<clientId>_item_N" data-item-value="…" data-item-label="…">`. Querying only `li` returns an empty list, which
looks exactly like "the server found nothing" and sends you off debugging the `completeMethod`'s JPQL. Diagnose by
reading the actual AJAX response (`browser_network_request` → `response-body`) — if the partial response contains the
rows, the query is fine and only the selector is wrong. **Fix**: select `tr[id^="<clientId>_item_"]` (or query both
`tr, li`), and click the row by `data-item-label`. Verified while testing issue #23206.


## 78. Typing into an input that a `p:ajax` just re-rendered prepends to the restored model value — set the dropdown first, then the numbers, and always confirm in the DB

A `p:ajax` on a `p:selectOneMenu` with `update="txtDuration txtOverShoot"` re-renders those inputs *from the server-side
model*. Clearing them client-side beforehand is undone by that re-render, so a later `pressSequentially('1')` lands in a
field that once again reads `0.0` and produces **`10.0`**, not `1`. Nothing errors and the page looks right at a glance —
the wrong value only shows up in the database. **Fix**: choose the dropdown value *first*, let the AJAX settle, then fill
the dependent inputs; and verify every configuration value with a `SELECT` after saving rather than trusting the form.
Related: `p:datePicker` ignores a direct `input.value = '…'` assignment (it re-formats from its own internal model) —
drive it with `PF('widgetVar').setDate(new Date(...))`, resolving the widget via
`Object.keys(PrimeFaces.widgets).find(k => PrimeFaces.widgets[k].id === '<clientId>')` when the page sets no
`widgetVar`. Verified while testing issue #23206.


## 79. Capturing a `p:growl` message in a screenshot: suppress its auto-hide timer, don't race it

§68 says snapshot immediately — but with `life="3000"` even a single `browser_take_screenshot` round-trip usually misses
it. Reading the DOM inside one `browser_evaluate` (click, `await` ~900ms, read `#growl_container.innerText`) proves the
message *arrived*, and `getBoundingClientRect()` on `.ui-growl-item-container` proves it was actually on screen — but
neither produces a screenshot. To capture one, suppress only the fade timer before triggering the action:

```js
window.__origSetTimeout = window.setTimeout;
window.setTimeout = function (fn, delay) { return delay >= 2500 ? 0 : window.__origSetTimeout.apply(window, arguments); };
```

The real growl then stays rendered for the screenshot (nothing about the message is faked — only the dismissal is
deferred). Reload the page afterwards to drop the patch. Verified while testing issue #23206.


## 80. A `p:dataTable` column present in `browser_snapshot` can still be 0px wide and invisible to the user — measure `offsetWidth`

PrimeFaces renders its DataTable with `table-layout: fixed`. Under that layout the browser honours the columns that
carry an explicit `width` first and hands the leftovers to the rest — and when the widths already declared exceed the
table width, "the rest" gets **zero**. Those columns still render their `<th>`/`<td>` with the correct text, so they
appear in `browser_snapshot`, in `get_page_text`, and in `innerText`. They are simply not on screen.

This is how issue #23224 was misdiagnosed at first: the accessibility snapshot listed all 17 headers including
`COST RATE` and `COST VALUE`, the footer totals were right, and the DB reconciled — so the report looked finished. The
columns the user was asking for were 0px wide, exactly as their screenshot showed.

Whenever a report "already has" a column a user says is missing, measure before concluding:

```js
[...document.querySelectorAll('.ui-datatable thead th')]
    .map(th => ({ h: th.innerText.trim(), w: Math.round(th.offsetWidth) }))
```

Any `w: 0` is an invisible column. The fix is in the XHTML, not the test: once *any* `p:column` on a fixed-layout table
declares a `width`, **every** column must declare one, or the undeclared ones collapse. Widening the viewport does not
reveal them — it makes it worse, because the extra space goes to the columns that did declare a width.


## 90. Simulating a barcode scan with `browser_type(..., submit: true)` on a `p:autoComplete` can submit the wrong button — use `slowly: true` and wait, don't press Enter

Found while verifying issue #23165's barcode auto-select/auto-advance
composite (`admcc:admission_search`) on `inward_room_change.xhtml`. Filling
the autocomplete in one shot and pressing Enter immediately
(`browser_type(..., submit: true)`, which does `fill()` then
`press('Enter')`) fires the Enter keypress **before** PrimeFaces' debounced
`query` AJAX call has completed, so the autocomplete's own suggestion list is
still empty and doesn't intercept the key. The Enter falls through to the
browser's native implicit-form-submission behavior, which submits via the
**first** submit-type button in the form — not the intended "Continue"
button, and not whatever the composite's own auto-advance JS would have
clicked. On this page that meant landing on the unrelated "Nursing
WorkBench" page instead of the admission's detail view, silently, with no
error in `server.log` or the browser console.

This is a test-methodology artifact, not evidence of a real bug — a real
scanner still just types characters via keyboard events, and the auto-select
JS runs off the `query` AJAX `oncomplete` callback, not off Enter. Simulate
it correctly instead: `browser_type` with `slowly: true` (fires per-character
keyup events, which is what triggers PrimeFaces' query debounce), no
`submit`, then `browser_wait_for` a second or two before asserting on the
result. Confirmed working this way: an exact single-match query auto-selects
and auto-advances; an exact query matching multiple admissions (e.g. several
active admissions sharing one PHN) shows the dropdown and does not
auto-advance.


## 94. PrimeFaces `p:datePicker` popups don't always close on their own — the previous field's panel can intercept clicks meant for the next field

Seen adding a timed service on
`theater/inward_timed_service_consume_surgery.xhtml` (Start Time / End Time,
both `p:datePicker` with `showTime="true"`). Clicking the End Time input right
after picking a Start Time did not open a new calendar — it silently reused
the Start Time picker that was still open underneath, so time-spinner clicks
kept editing the wrong field. A later click on the real End Time input then
timed out with `<div class="ui-datepicker-header">... intercepts pointer
events` because the stale panel from the previous field was still on top.

**Fix**: click a neutral, non-input element on the page (e.g. a panel header)
to dismiss the open picker before clicking the next date field — `Escape`
alone was not reliable here. Re-`browser_snapshot` after opening a picker to
confirm which field's `_panel` id is actually active before interacting with
its spinners.


## 101. Some PrimeFaces buttons need a jQuery-triggered click

Most `p:commandButton`s submit fine with a normal Playwright click — including
`ajax="false"` text-valued buttons such as `form:btnNursingDischarge` on
`admission_profile.xhtml`, which navigate correctly on a plain
`page.locator('#form\\:btnNursingDischarge').click()`.

The exception is **icon-only row controls** inside a `p:dataTable` (e.g. the
`ui-button-icon-only` action buttons on `inpatient_search.xhtml`, which render with
no `value` and no `onclick`). There, PrimeFaces binds the handler through jQuery and a
raw `element.click()` from inside `browser_evaluate` can fire the DOM event without
invoking it — the form never submits and the page silently stays put, with no error and
no server-log entry. Fall back to triggering the jQuery handler for those:

```js
window.jQuery(document.getElementById('form:tblBills:0:j_idt638')).trigger('click');
```

Inspect `$._data(el, 'events')` if unsure — the absence of a `click` key alongside
`mousedown`/`mouseup` is the tell. Either way, **assert the outcome** (URL change, or the
expected element on the destination page) rather than assuming the click worked.
Verified while testing issue #23222.


## 105. A `p:calendar` bound to Date of Birth can ignore real keystrokes — use the widget's `setDate()` API

On `inward_admission_child.xhtml`'s "Admit a Baby" form, the DOB field (`dpDob`, a `p:calendar`
with `timeInput="true"`) rejected even a real slow-typed keystroke sequence
(`click` → `Control+a` → `pressSequentially('05/09/2026 00:00:00 am')` → `Escape`): the input
stayed visually blank and the server still reported "Patient Age is Required" (the DOB-backed
check) on submit. This is a step further than §3's "JS-set values are silently discarded" note —
that one only warns about `page.evaluate`/`fill()`, but here the *keyboard* path documented
elsewhere in this guide as the fix for datepickers also silently failed to commit.

**Fix:** call the widget's own API directly instead of trying to type into it:

```js
() => { PrimeFaces.widgets['widget_<formId>_<fieldId>_dpDob'].setDate(new Date()); }
```

Find the exact widget variable name first with
`Object.keys(PrimeFaces.widgets).filter(k => k.toLowerCase().includes('dob'))` — it's generated
from the component's full client id, not the plain `id` attribute, so don't guess it. Confirm the
commit by reading `document.getElementById('<formId>:<fieldId>:dpDob_input').value` before
submitting. Verified while testing issue #23509 (baby admission with no NIC/phone).


## 108. This dashboard's Sample Transporter `p:autoComplete` ignores synthetic keystrokes — drive `widget.search()` directly

Same page/issue as item 107. Playwright's `browser_type`
(`pressSequentially`) into the Sample Transporter input updated the DOM
value but never fired PrimeFaces' autocomplete AJAX query — no network
request went out, and the suggestion panel stayed empty even after a 1.5s
wait. Driving the widget directly worked immediately:
```js
const w = PrimeFaces.widgets['widget_<formId>_<inputId>'];
w.search('a');                         // fires the AJAX query
// then, after the panel populates:
w.panel[0].querySelector('.ui-autocomplete-item').click();
```
Confirmed the resulting hidden value actually stuck (visible input showed
the selected staff name, and the subsequent Send to Lab submit used it
correctly) — this is the same class of gotcha as item 50, but for
`p:autoComplete` specifically rather than `p:calendar`/`p:selectOneMenu`.


## 109. A `p:confirm` dialog is `position: fixed`, so an `offsetParent` visibility probe wrongly reports it hidden — the click did work

Cancelling an inward service bill (`inward_cancel_bill_service.xhtml`) appeared
to do nothing: clicking **Cancel Service Bill** produced no growl, no page
change, and no row change in the DB. A probe for open dialogs came back empty:

```js
[...document.querySelectorAll('.ui-confirm-dialog,.ui-dialog')]
    .filter(d => d.offsetParent)          // <-- always empty for this dialog
```

The dialog *was* open. PrimeFaces renders `p:confirm`'s dialog with
`position: fixed`, and **`offsetParent` is `null` for any fixed-position
element** — so the usual "is it visible" shorthand reports every confirm dialog
as hidden. The follow-up symptom is the giveaway: a retried click on the
underlying button fails with

```
<div class="ui-widget-overlay ui-dialog-mask" ...> intercepts pointer events
```

which is the modal mask doing its job, not a broken button.

**Probe `display`/`.ui-dialog-mask` instead, and click the dialog's own button:**

```js
[...document.querySelectorAll('.ui-confirm-dialog')].map(d => ({
    id: d.id,
    display: getComputedStyle(d).display,        // 'block' when open
    buttons: [...d.querySelectorAll('button')].map(b => ({t: b.innerText.trim(), id: b.id}))
}))
```

then click the **Yes** button by its id. Note this is a PrimeFaces dialog, not a
native `confirm()` — `browser_handle_dialog` does not apply and will time out
waiting for a dialog that never reaches the browser. The same page can use both:
`inward_bill_service_refund.xhtml`'s **Refund Bill** is a native `confirm()`
(handled with `browser_handle_dialog`), while the cancel screen's button is a
`p:confirm`. Check the markup for `<p:confirm>` before deciding which to use.


## 113. `p:tag` silently drops `title` — a tooltip on a tag needs `p:tooltip`

`inward_patient_room_details.xhtml` carried a room-conflict explanation as

```xhtml
<p:tag value="Overlap" severity="danger" title="#{bean.overlapDescription(rm)}"/>
```

and the text had **never once reached a user**: `p:tag` has no `title`
passthrough, so the rendered markup is just

```html
<span class="ui-tag ui-widget ui-tag-danger">…Overlap</span>
```

with no `title` attribute at all. There is no warning at build or render time
— the page looks right, the EL is even evaluated, and the string is thrown
away. Found on #23641 only because the E2E check read the attribute back:

```js
() => { const t = [...document.querySelectorAll('.ui-tag')]
          .find(e => e.textContent.trim() === 'Overlap');
        return t && t.getAttribute('title'); }   // → null
```

Use the component the codebase already uses elsewhere
(`admin/lims/investigation_format_multiple.xhtml`):

```xhtml
<p:tag id="roomOverlapTag" value="Overlap" severity="danger"/>
<p:tooltip for="roomOverlapTag" position="top" showDelay="150"
           value="#{bean.overlapDescription(rm)}"/>
```

Inside a `p:dataTable` the plain `for="roomOverlapTag"` resolves per row —
no need to build the full row client id.

**Testing rule:** a `title` tooltip is invisible to a screenshot, so
"the page rendered" is not evidence it works. Assert the attribute (or the
`.ui-tooltip` text after a `browser_hover`) explicitly. The same blind spot
applies to any attribute a component may not support — verify the *rendered
DOM*, not the source.


## 116. `p:datePicker` with a mask silently truncates `pressSequentially`

Typing `10 Sep 2026 04:00:00` into a masked `p:datePicker` character by
character produced `'10 Sep 2026 04:'` and a JSF conversion error
(*"could not be understood as a date and time"*) — the mask consumed part of
the input mid-type. Setting the value in one assignment works, because JSF reads
the submitted string on the full form post:

```js
browser_evaluate(() => { document.getElementById('form:dateStamp_input').value = '10 Sep 2026 04:00:00'; });
```

Do **not** follow it with a synthetic `change` event — on these pickers that
re-runs the mask and blanks the field again. §18's calendar-grid technique
remains the option when the widget's own parsing needs to run.


## 117. `p:tabView` renders every tab's markup — a text-matched `browser_evaluate` click hits a hidden tab's copy

`inward_bill_intrim.xhtml` has a "View Bill" `p:commandButton` in **six**
different tabs (Room Charges, Professional Fees, Deposits & Payments, …). A
`p:tabView` keeps all inactive panels in the DOM (just `display:none`), so
`[...document.querySelectorAll('button')].find(b => b.textContent.trim() === 'View Bill')`
returns the **first in document order** — a hidden tab's button — and clicking it
fires that tab's action (it navigated to `inward_reprint_bill_service.xhtml` for
a bill that did not exist, "No records found").

Scope the query to the active panel by its server id before matching text:

```js
browser_evaluate(() => {
  const panel = document.querySelector('[id="pageForm:tvPt:tabP"]');   // the Deposits & Payments panel
  const row = [...panel.querySelectorAll('tr')].find(r => /050558/.test(r.textContent)); // the exact bill row
  row.querySelector('button').click();
});
```

Matching on a stable substring of the row (bill number) also guards against
clicking the wrong row once the table has several entries.


## 123. `reports/index.xhtml`'s report-category accordion needs the PrimeFaces widget API, not a plain click, to reliably expand a tab

Found while testing issue #23604 (GRN Summary Report). The category strip
("Inventory Reports", "Financial Reports", …) at the top of
`reports/index.xhtml` looks like a `p:tabView` (ARIA `role="tab"`/`tablist`)
but is actually a `p:accordionPanel` whose headers are styled to look like
tabs. A `browser_click` (or `getByRole('tab', {name: ...}).click()`) on a
header is unreliable for two reasons: (1) the panel's report buttons are lazy
— the ARIA state can flip to `expanded`/`selected` before the AJAX call that
actually populates the panel content has returned, so an immediate DOM check
finds an "open" but still-empty panel; (2) a raw `element.click()` via
`browser_evaluate` bypasses jQuery's bound handler and can **toggle the
accordion closed** if PrimeFaces already considered it open from an earlier
click, silently undoing the navigation.

Reliable pattern: drive the PrimeFaces widget directly instead of clicking.
```js
const w = PrimeFaces.widgets['widget_j_idt533_reportsAccordion']; // find via
  // Object.keys(PrimeFaces.widgets).filter(k => /reportsAccordion$/.test(k))
const headers = w.headers;
let idx = -1;
headers.each(function (i) { if (this.textContent.trim() === 'Inventory Reports') idx = i; });
w.select(idx);
```
`select()` still triggers the same lazy-load AJAX that a real click would,
it just does so through the real widget lifecycle instead of a spoofed
click, so the panel populates instead of toggling shut — but the AJAX is
still async, so don't search the DOM immediately after `select()`. Per §5a,
wait on content rather than a fixed delay: `browser_wait_for({text: '<a
label expected in that category, e.g. "1. Closing Stock">'})` before
searching the DOM for the target report button. A fixed-time wait risks
checking before a slow response has populated the panel.


## 128. The department `p:selectOneMenu` is a *filterable, table-based* dropdown — there are no `<li>` items to click

The login department selector renders its options as `<tr data-label="...">`
rows inside `…_table`, not as `<li>` elements, and it has a filter box. A
`.ui-selectonemenu-items li` query returns `[]` and the panel looks empty even
though it is visible. Drive it the way a user does:

```js
// 1. open the dropdown, 2. type into the filter, 3. click the matching row
await page.locator('#form\\:dept_filter').pressSequentially('OPD Pharmacy');
await page.locator('#form\\:dept_table tr[data-label="OPD Pharmacy"]').click();
```

Note the doubled backslash. A JavaScript single-quoted `'\\:'` produces the one
literal backslash CSS needs in order to escape the colon in a JSF client id.
Writing `'\:'` collapses to a bare `:`, which CSS parses as a pseudo-class, so
the locator silently matches nothing.

Check `offsetParent !== null` per row to confirm the filter actually narrowed
the list before clicking — the non-matching rows stay in the DOM, just hidden.


## 129. Pressing Enter to accept a *loaded* autocomplete suggestion also fires the page's `p:defaultCommand` — the action runs before you click its button

§3's Pattern 1 ("type slowly, press Enter to select") is safe only when the form
containing the autocomplete declares no `p:defaultCommand`. It is a form-level
Enter target, so an unrelated form elsewhere on the page is harmless — what
matters is the autocomplete's own form.
`pharmacy/pharmacy_purhcase_order_request_native.xhtml` puts both in one form
(`<p:defaultCommand target="btnSave">`), and there the Enter that accepts the
suggestion keeps travelling: the item is selected **and** that form's default
command runs, adding the line. Clicking the real "Add" button afterwards attempts
a second add, which the `Prevent Duplicate Items in Purchase Orders` guard
rejects before anything is added — with that option turned off, the second click
would instead add a duplicate line:

> This item has already been added to the purchase order. Please update the
> quantity of the existing item instead of adding it again.

Read in a scripted run, that message looks like the feature under test is
broken — the new warning "didn't fire" — when in fact the first Enter already
performed the add and the guard is doing its job. This cost several
redeploy/retest cycles while verifying issue #23811.

This is distinct from §90, which is about Enter arriving *before* the suggestion
list has loaded and falling through to the first submit button. Here the list is
loaded and the selection itself works correctly; the extra action is the page's
own default command.

**How to spot it**: grep the **XHTML source** for `p:defaultCommand` — it is a
markup-less component, so the rendered DOM will not show it (§84); in the browser
you would have to look for its generated Enter handler instead. Then count the
item-table rows straight after the Enter, before clicking anything — if a row
already appeared, the action has run.

**What to do instead**: click the suggestion row rather than pressing Enter
(`tr.ui-autocomplete-item` / `tr[id^="<clientId>_item_"]` — see §77), let the
`itemSelect` AJAX settle, then click the real action button. On a page with
`p:defaultCommand`, prefer this over Pattern 1 even when your query narrows to a
single match.


## 131. `p:autoComplete` gives no suggestions to `fill`/`pressSequentially` — drive the widget's `search()`, and pick the right widget id

Playwright typing into a PrimeFaces `p:autoComplete` (e.g. the lab *Sent Sample → Sample Transporter* dialog) fires no
`_query` request, so the panel stays empty. Call the widget instead, then click the option:

```js
const w = PrimeFaces.widgets['widget_<formId>_<inputId>'];   // NOT the first autocomplete on the page
w.search('Pavan');
```

Find the correct id from the typed text: the widget whose `<id>_input` received your text is the one to search
(a neighbouring filter autocomplete answers "No results found" and looks like a broken query). The same dialog also
requires *Sending to Department* (`selectOneMenu` — `widget.selectValue('<deptId>')`) or the send silently
re-closes the dialog with only a transient growl error.


## 137. `button[title="…"]` locator works once, then finds nothing

PrimeFaces' global tooltip removes a button's `title` on its first hover. Locate icon-only row buttons by icon class (`button:has(.fa-pills)`) or `id`, never by `title`. Give new row buttons a stable `id`.
