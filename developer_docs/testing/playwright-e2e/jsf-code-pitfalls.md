# Playwright E2E — Code/authoring bugs that E2E exposes (JSF, EL, JPQL)

Part of the [Playwright E2E Workflow](../playwright-e2e-workflow.md). Read only the section you need.

- [25. A JPQL path expression through a nullable relationship silently INNER-JOINs and drops rows](#25-a-jpql-path-expression-through-a-nullable-relationship-silently-inner-joins-and-drops-rows)
- [33. Binding a `p:inputText` through a nullable session-scoped entity property crashes on first submit](#33-binding-a-pinputtext-through-a-nullable-session-scoped-entity-property-crashes-on-first-submit)
- [42. PrimeFaces bare `update="someId"` can 500 from inside a `p:dataTable`/`ui:repeat` row even though the id exists on the page](#42-primefaces-bare-updatesomeid-can-500-from-inside-a-pdatatableuirepeat-row-even-though-the-id-exists-on-the-page)
- [45. `&&` written as `&amp;&amp;` inside a `<script><![CDATA[...]]>` block parses as valid XML but throws a JS `SyntaxError` at runtime, silently breaking every function in that script](#45--written-as-ampamp-inside-a-scriptcdata-block-parses-as-valid-xml-but-throws-a-js-syntaxerror-at-runtime-silently-breaking-every-function-in-that-script)
- [51. `p:dialog appendTo="@(body)"` silently drops that dialog's own bound inputs from every AJAX submission](#51-pdialog-appendtobody-silently-drops-that-dialogs-own-bound-inputs-from-every-ajax-submission)
- [69. An earlier `p:ajax` event mutating the field a later button's enclosing `rendered` depends on silently skips that button's action — canary-test with a `throw` to prove it](#69-an-earlier-pajax-event-mutating-the-field-a-later-buttons-enclosing-rendered-depends-on-silently-skips-that-buttons-action--canary-test-with-a-throw-to-prove-it)
- [76. `isXxx(arg)` boolean methods with a parameter don't resolve via the JSF EL property-getter convention — drop the `is` prefix](#76-isxxxarg-boolean-methods-with-a-parameter-dont-resolve-via-the-jsf-el-property-getter-convention--drop-the-is-prefix)
- [82. Any AJAX call from inside an editable `p:dataTable`'s row scope has its `update` target silently constrained to the table itself — extra ids you add are dropped, even on a plain `p:commandButton`](#82-any-ajax-call-from-inside-an-editable-pdatatables-row-scope-has-its-update-target-silently-constrained-to-the-table-itself--extra-ids-you-add-are-dropped-even-on-a-plain-pcommandbutton)
- [86. A `p:inputText`/`p:inputNumber` bound to a `Map<String, Integer>` entry silently stores the raw `String` — the write is lost with no error until you read it back](#86-a-pinputtextpinputnumber-bound-to-a-mapstring-integer-entry-silently-stores-the-raw-string--the-write-is-lost-with-no-error-until-you-read-it-back)
- [92. A `p:commandButton` save that does nothing — no growl, no error, no DB row, a clean `server.log` — is usually a required field whose message went to a `<p:messages>` you never looked at](#92-a-pcommandbutton-save-that-does-nothing--no-growl-no-error-no-db-row-a-clean-serverlog--is-usually-a-required-field-whose-message-went-to-a-pmessages-you-never-looked-at)
- [100. `PrimeFaces.current().executeScript(...)` silently no-ops on an `ajax="false"` button — use `p:dialog visible="#{bean.flag}"` instead](#100-primefacescurrentexecutescript-silently-no-ops-on-an-ajaxfalse-button--use-pdialog-visiblebeanflag-instead)
- [103. An unchanged database does NOT prove a server-side guard ran](#103-an-unchanged-database-does-not-prove-a-server-side-guard-ran)
- [112. Relaxing a "required" validation? Audit every downstream reader of that field for null-safety](#112-relaxing-a-required-validation-audit-every-downstream-reader-of-that-field-for-null-safety)
- [121. `p:ajax update="..."` targeting a raw `<div id="...">` throws `ComponentNotFoundException` at render time — wrap it in `h:panelGroup`](#121-pajax-update-targeting-a-raw-div-id-throws-componentnotfoundexception-at-render-time--wrap-it-in-hpanelgroup)
- [122. A `p:commandButton`'s `process="X"` that excludes the button itself silently skips its own `action` — no exception, no error, a real `200 OK` with the *previous* data](#122-a-pcommandbuttons-processx-that-excludes-the-button-itself-silently-skips-its-own-action--no-exception-no-error-a-real-200-ok-with-the-previous-data)
- [126. JSF will not decode a `disabled` command button — you cannot reach the controller guard behind it by re-enabling the button in the DOM](#126-jsf-will-not-decode-a-disabled-command-button--you-cannot-reach-the-controller-guard-behind-it-by-re-enabling-the-button-in-the-dom)
- [132. `Bill.referenceBill` means different things on different flows — don't branch on "is it set", branch on the bill's own type](#132-billreferencebill-means-different-things-on-different-flows--dont-branch-on-is-it-set-branch-on-the-bills-own-type)
- [134. `f:validateRegex` on a `p:inputText` bound to a `Map<String,String>` entry fires even on a blank/untouched row — with no visible error](#134-fvalidateregex-on-a-pinputtext-bound-to-a-mapstringstring-entry-fires-even-on-a-blankuntouched-row--with-no-visible-error)

---

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


## 69. An earlier `p:ajax` event mutating the field a later button's enclosing `rendered` depends on silently skips that button's action — canary-test with a `throw` to prove it

On `inward/admit_room.xhtml`, a `p:autoComplete`'s `itemSelect` ajax handler bound directly to
`roomChangeController.current` set that field as soon as a patient was selected — *before* the
"Continue" `p:commandButton` (bound to `roomChangeController.selectRoomForAdmit()`, `ajax="false"`)
was ever clicked. The Continue button lived inside a panel gated
`rendered="#{roomChangeController.current eq null}"`. By the time the Continue postback started,
`current` was already non-null (set by that earlier ajax request, persisted in the
`@SessionScoped` bean) — so JSF evaluated the *whole panel*, including the Continue button, as not
rendered for this request and silently skipped decoding/invoking its action. The button's own
network POST still looked completely normal (correct hidden field values, correct button
parameter) — nothing in the request/response cycle hinted the action never ran.

**How this was proven, not just suspected**: added `if (true) { throw new RuntimeException("canary"); }`
as the literal first line of the suspected action method, rebuilt, redeployed, and repeated the
click. No exception, no 500, no log line — page rendered its normal "success" output. That's the
tell: if the action method actually executed, a first-line unconditional throw is unmissable
(crashes the page). Silence under that canary means the method body never ran at all — reach for
this test before trusting any subtler theory (stale ViewState, lazy-loading timing, EL caching)
about a command button that "looks like" it does nothing.

**Fix pattern**: don't bind the ajax-updated input directly to the field that gates the
surrounding panel's `rendered`. Introduce a separate staging field (e.g. `selectedAdmission`) for
the autocomplete's `value` and for anything displayed *before* the confirm button is clicked; only
assign it into the gating field (`current`) inside the confirm button's own action method. That
keeps the panel's `rendered` condition — and therefore whether the button inside it gets
decoded/invoked at all — stable for the entire lifecycle of that button's own request. Verified
against a real waiting-room patient (DB `ROOMADMITTED` flipped 0→1 after "Assign Room") while
fixing issue #22911.

Two other Payara/asadmin quirks hit while chasing this on the carecode dev machine, worth knowing
before you spend time debugging "missing" log output:
- **`java.util.logging` calls (even `.severe(...)`) can silently not reach `server.log`** despite
  `logging.properties` listing `GFFileHandler` with `logStandardStreams=true` — don't trust
  "no log line appeared" as proof a code path didn't run; use the canary-throw test above instead,
  since an uncaught exception during `INVOKE_APPLICATION` reliably surfaces as a rendered error
  page regardless of the logging pipeline's state.
- **A `redeploy` that exceeds the foreward-call timeout can leave the domain's DAS memory-bloated
  and totally unresponsive** (`curl` to the app hangs/times out, `asadmin` commands against the
  same domain also hang) — matches the existing "Local DAS stalls when memory-bloated" pattern.
  Recovery: `kill -9` the stuck DAS `java` process (find via `ps aux | grep domains/<name>`),
  `asadmin start-domain <name>`, then a plain `deploy` (not `redeploy`).


## 76. `isXxx(arg)` boolean methods with a parameter don't resolve via the JSF EL property-getter convention — drop the `is` prefix

A boolean bean method named `isHasPendingTheatreReturnForAdmission(Admission admission)` and called from EL as
`#{bean.hasPendingTheatreReturnForAdmission(admissionController.current)}` (mirroring how existing no-arg booleans like
`isHasPendingRequestsForDepartment()` are already referenced as `hasPendingRequestsForDepartment` elsewhere in the same
page) throws `javax.el.MethodNotFoundException` at render time — caught immediately by a Playwright pass (500 error
page) rather than silently misbehaving. The JavaBean `isXxx()`/`getXxx()` → property-name stripping is an EL
*property-access* convention (`#{bean.propertyName}`, zero arguments only); once the call includes an argument list,
EL falls back to literal method-name resolution and looks for a method named exactly `hasPendingTheatreReturnForAdmission`,
not `isHasPendingTheatreReturnForAdmission`. **Fix**: for any boolean helper method that takes a parameter, name it
*without* the `is` prefix (`hasPendingTheatreReturnForAdmission(Admission)`), matching how the call site will
naturally read in EL — reserve the `is`/`get` prefix convention for genuine no-arg property getters. Verified while
testing issue #23166 (theatre transfer per-patient page).


## 82. Any AJAX call from inside an editable `p:dataTable`'s row scope has its `update` target silently constrained to the table itself — extra ids you add are dropped, even on a plain `p:commandButton`

`ward_pharmacy_bht_issue.xhtml`'s "Issuing Items" grid needed the row-edit
checkmark, the inline item-swap autocomplete, AND the row's delete
(trash) button to also refresh a separate `billDetailsPanel` outside the
table. The obvious fix — add `billDetailsPanel` to each component's own
`update=""` (`p:ajax event="rowEdit"`, a `p:ajax event="itemSelect"` nested
inside an autocomplete that itself lives inside a `p:cellEditor`, and even
a completely ordinary `p:commandButton` in a row with no cellEditor/rowEdit
involvement at all) — compiles and deploys with no error, but the extra id
is silently dropped at runtime in **all three cases**: the browser's actual
AJAX request always sends `javax.faces.partial.render=<table-id>` only,
never the second id, no matter what `update=""` says server-side. Confirmed
by reading the button's own rendered `onclick` directly off the live DOM
(`document.querySelector('button.ui-button-danger').getAttribute('onclick')`)
— the compiled `PrimeFaces.ab({...})` call has `u:"<table-id>"` baked in
verbatim, already missing the second id before the click ever happens. This
survives a normal `asadmin deploy --force`, a full `undeploy`+`deploy`, and
even a full `stop-domain`/`start-domain` — it isn't a caching artifact. A
component genuinely *outside* the table (e.g. a `p:remoteCommand` declared
as the table's sibling) is unaffected and can update `billDetailsPanel`
freely — so this isn't about the DataTable's own `rowEdit`/`rowEditCancel`
widget behaviors specifically (as originally assumed here); it's the row
*scope* itself, for anything nested inside it.

Confirm this is what's happening by reading the actual request body
Playwright captured (`browser_network_request` with `part: "request-body"`)
— or the rendered `onclick` off the live DOM for a plain button — and
checking for the missing id, not by re-reading the source XHTML, which will
keep looking correct.

Fix: add a `p:remoteCommand name="refreshX" update="theOtherPanel"` once,
elsewhere in the same form (outside the table), then set
`oncomplete="refreshX();"` on every row-scoped component that needs the
extra update — `rowEdit`/`rowEditCancel`, a nested `itemSelect`, or a plain
`p:commandButton` alike — instead of trying to widen their own `update`.
The remote command fires as a second, independent AJAX call that isn't
subject to the same row-scope constraint. Verified while fixing issue
#23328 (including a CodeRabbit-caught follow-up: the row's delete button
needed the identical treatment, not just the two components fixed in the
original pass).


## 86. A `p:inputText`/`p:inputNumber` bound to a `Map<String, Integer>` entry silently stores the raw `String` — the write is lost with no error until you read it back

Binding a form input to `#{bean.someMap[key]}` where `someMap` is declared `Map<String, Integer>`
compiles fine and *looks* like it should coerce, because a normal bean property setter
(`setSomeField(int)`) does get EL's automatic string-to-primitive coercion via reflection on the
setter's declared parameter type. A `Map` entry gets no such coercion: `MapELResolver.setValue()`
just calls `map.put(key, value)` with whatever raw type the component submitted — generics are
erased at the bytecode level, so the resolver has no way to know the map is supposed to hold
`Integer`. The submitted value lands in the map as a plain `String`.

The failure doesn't surface where you'd look for it. The command button's `update` re-renders the
component from that same in-memory map object, so the browser still shows the value you just typed
— it *looks* saved. The real breakage happens the next time server-side code reads the map entry as
`Integer` (e.g. `Integer order = orderMap.get(key);`) — a `ClassCastException: String cannot be cast
to Integer` that aborts the whole action method, so nothing after that line (including the actual
persistence call) ever runs. `p:messages`/`p:growl` stays silent because the exception happens inside
the JSF lifecycle's invoke-application phase, not inside a `catch` the page bothers to show — the
only trace is a `SEVERE javax.faces.el.EvaluationException` in `server.log`.

**Fix**: keep the map typed `Map<String, String>` (matching how every other free-text-bound map in
this codebase already works) and parse the string to the target type only where the value is actually
consumed — never type a directly-bound map as anything but `String`. **Verification**: a screenshot or
an in-session AJAX re-read is not proof of a save — the model object doesn't go away just because the
action method threw. Always confirm with a fresh `SELECT` after the request completes (a full page
reload session's own display of "the value I set" proves nothing, since it's the same still-open
transactional model that never got rolled back). Found and fixed while testing issue #23340.


## 92. A `p:commandButton` save that does nothing — no growl, no error, no DB row, a clean `server.log` — is usually a required field whose message went to a `<p:messages>` you never looked at

Seen on `pharmacy/admin/lab_amp.xhtml` and `store_amp.xhtml` while verifying
issue #23484. Clicking **Save** with Name, Code and VMP filled produced: no
growl, nothing under `.ui-message*`, no new `Amp` row, and not a single new
line in `server.log`. The form still held every value, so it looked like the
action method had run and silently returned.

It had not run at all. Both pages mark **Dosage Form** (`selDosageForm`) and
**Category** (`ampCat`) `required="true"`, and the Save button uses
`process="@form"` with `update="form:msg ..."` — so JSF failed validation in
the Process Validations phase, never invoked `labAmpController.save()`, and
routed both `requiredMessage`s into the `form:msg` `<p:messages>` component.
That component contributes no accessible name when its own re-render is what
populated it, so it does not show up in `browser_snapshot`, in
`browser_find` for `/error|required/i`, or in a `.ui-growl-item` query.

**Diagnose it this way** — the three symptoms together (form still populated
+ DB unchanged + `server.log` clean) mean the action method never fired, which
narrows it to client-side or validation-phase rejection, not business logic:

```js
// enumerate every required input in the form and which ones are still empty
() => Array.from(document.querySelectorAll('#form [aria-required="true"], #form .ui-state-error'))
        .map(e => ({ id: e.id, cls: e.className, val: e.value }))
```

and read the message panel by id rather than by class:

```js
() => document.querySelector('#form\:msg')?.textContent.trim()
```

Cheaper still: `grep -n 'required="true"' <page>.xhtml` and fill every one of
them before the first Save attempt. Note that a `required` `p:autoComplete`
(Category here) is only satisfied by **clicking a suggestion** — typing the
exact label and leaving it is an empty model value as far as JSF is
concerned, even though the textbox looks filled.


## 100. `PrimeFaces.current().executeScript(...)` silently no-ops on an `ajax="false"` button — use `p:dialog visible="#{bean.flag}"` instead

Seen fixing issue #23514. `inward_admission.xhtml`'s "Admit" button is
`ajax="false"` (a full postback, deliberately, per issue #21175's foreigner-
checkbox fix). The pre-existing "patient already admitted" warning dialog was
shown via `PrimeFaces.current().executeScript("PF('dlg').show();")` from the
backing bean — that call only queues JS into an *ajax* partial response, so on
this button it silently did nothing: no dialog, no error, no admission
created, no evidence in `server.log` that anything was rejected. It looked
exactly like the button doing nothing.

The fix: bind the dialog's own `visible` attribute to a session-scoped
boolean the bean sets before returning (`visible="#{bean.showWarning}"`).
`p:dialog visible="true"` renders its own show-on-load script regardless of
whether the surrounding request was ajax or a full page render, so it works
for both `ajax="false"` and `ajax="true"` buttons — but only if the triggering
`ajax="true"` request's own `update` actually includes the dialog (or the
whole form); an ajax caller that updates some narrower region will still
leave the dialog's old, unrendered markup in the DOM with the stale `visible`
value baked in. Remember to also clear the flag via a real server round-trip
on the dialog's "Cancel" button *and* on its header close ("x") icon (`p:dialog
closable="true"` gives that its own client-side close path via `p:ajax
event="close"`, separate from any button) — a client-only `PF('dlg').hide()`
leaves the session-scoped flag `true`, and it will reappear on the next
unrelated full postback of that form.


## 103. An unchanged database does NOT prove a server-side guard ran

When a fix disables a button via `disabled="#{bean.someCheck()}"`, it is tempting to strip the
attribute, submit, observe the DB unchanged, and call the server-side guard proven. **That
inference is invalid** — and on JSF it is usually wrong.

JSF **skips the action of a component it rendered as `disabled`**: the decode phase ignores the
activation, so the action method is never invoked. The postback returns 200, the page re-renders,
and the database is unchanged — identical to what a working guard looks like from the outside.
Confirmed on `inward_nursing_discharge.xhtml`, whose Confirm button carries
`disabled="#{nursingDischargeController.hasPendingPharmacyItems()}"`: re-POSTing the form produced
`msgs:[]` (no growl message at all), proving `confirmNursingDischarge()` never ran.

So before asserting DB state, **assert the action executed** — the expected message is the cheapest
signal. Re-POST the form and inspect the response body directly:

```js
const fd = new FormData(document.getElementById('formNursingDischarge'));
fd.set('formNursingDischarge:btnConfirmNursingDischarge', '');   // use the button's real (often empty) value
const html = await (await fetch(location.href, {
  method: 'POST', body: new URLSearchParams(fd),
  headers: {'Content-Type': 'application/x-www-form-urlencoded'}
})).text();
// msgs:[...] populated => the action ran; msgs:[] => it did not
```

An `ajax="false"` button reloads the page, so a growl is usually gone before any screenshot or
follow-up `browser_evaluate` — read it out of the raw response as above rather than from the DOM.

To exercise a guard that is unreachable while the button renders disabled, recreate the **race it
exists for**: load the page while the record is clean (button enabled), make the blocking condition
true, then submit the stale page.

Always pair this with the **negative test** — a record with nothing pending must still succeed —
otherwise you have not distinguished "correctly blocks" from "blocks everything". Undo any state the
negative test creates through the app's own Cancel action, never with an `UPDATE`.
Verified while testing issue #23222.


## 112. Relaxing a "required" validation? Audit every downstream reader of that field for null-safety

#23618 removed the `settleBill()` guard that forced `reservedToDate` to be
non-null for a Room Admission appointment. That guard was also the de-facto
protection for code that read the value unconditionally later:
`updateChangesReservation()` did `reservedToDate.before(...)` (NPE), and both
that method and `settleBill()` did `sdf.format(res.getReservedTo())` when
reporting a room conflict (NPE if the *conflicting* reservation was itself
saved with a null end). None of these are in the diff of the validation
change, so a review that only looks at changed lines misses them — CodeRabbit
flagged it on PR #23628.

When a change makes a previously-guaranteed field nullable (or merely more
often null), grep the whole class (and callers) for every read of that
field — `.before(`, `.after(`, `.format(`, `.getTime()`, arithmetic — and
guard or apply the same fallback the new code uses (here: treat a missing
end as the start instant).


## 121. `p:ajax update="..."` targeting a raw `<div id="...">` throws `ComponentNotFoundException` at render time — wrap it in `h:panelGroup`

Found while building the "Add New Option" dialog for issue #23678. A
`p:selectOneMenu` with a `<p:ajax update=":someForm:someContainer" />`
listener, where `someContainer` was a plain `<div id="someContainer">`
(no JSF component behind that `id`, just an HTML attribute), fails the
*entire page render* — not just the ajax call — with:

```
javax.faces.component.search.ComponentNotFoundException: Cannot find
component for expressions ":addOptionForm:newOptionInitialValue"
referenced from "addOptionForm:j_idt653".
```

PrimeFaces resolves `update`/`process` search expressions against the JSF
component tree, not the rendered HTML DOM — a raw `<div id="...">` has no
corresponding `UIComponent`, so the search fails even on the component's
first, non-ajax render (the failing call is inside `SelectOneMenuRenderer`
building the `onchange` script, not inside any ajax round-trip). The fix is
the same one already documented for `p:printer`/`p:dataExporter target=` in
the [Report Favorites Implementation Guide](../../feature/report-favorites.md):
give the target container a real JSF component id via `h:panelGroup
layout="block" id="..."` (never a bare `<div id="...">`) wherever anything
elsewhere on the page names that id in `update`, `process`, or `target`.


## 122. A `p:commandButton`'s `process="X"` that excludes the button itself silently skips its own `action` — no exception, no error, a real `200 OK` with the *previous* data

Found while fixing issue #23678's Department/Institution Options pages,
which had "List Department Options" / "List Options" buttons wired as
`process="cmbDepartment"` (only the picker, not the button). Clicking such a
button produces a completely normal-looking ajax exchange — real `200 OK`,
a `<partial-response>` that updates the target table, no console error, no
server-log exception — but the bound `action`/`actionListener` **never
runs**. The rendered table is whatever `update` happens to touch given
whatever state the backing bean was already in (here: `null`/stale from an
earlier action), which can look deceptively like "the query returned the
wrong rows" when the real story is "the query never ran at all."

This is a JSF partial-processing rule: `process` (like `execute`) scopes
which components participate in `APPLY_REQUEST_VALUES` through
`INVOKE_APPLICATION`. A `p:commandButton` is itself a component that must be
processed for its own `action`/`actionListener` to fire — restricting
`process` to *only* some other input, with no `@this` (and no `@all`/form
default), removes the button from every one of those phases, so JSF never
invokes it. The symptom looks exactly like a query bug (verified here by
adding a temporary `System.out.println` at the very top of the suspected
method — it never printed, proving the method wasn't entered at all, before
tracing it back to this `process` misconfiguration).

Fix: always include the button in its own `process` — `process="@this
cmbDepartment"` — whenever `process` is set to anything narrower than the
default. A button with no `process` attribute at all (defaulting to the
enclosing form) does not have this problem; it only bites when `process` is
explicitly restricted and the button is left out.

**Diagnostic recipe** when an ajax button "does nothing" with a real `200`
response and no logged exception: add a one-line `System.out.println` (or
check EclipseLink SQL logging, `eclipselink.logging.level.sql=FINE` in
`persistence.xml`, temporarily) at the very top of the bound method. If it
never prints despite a `200` response, the action isn't being invoked at
all — go straight to the button's `process`/`execute` attribute rather than
debugging the method's own logic.


## 126. JSF will not decode a `disabled` command button — you cannot reach the controller guard behind it by re-enabling the button in the DOM

A common pattern in this codebase is a button that is both bound to
`disabled="#{...someState}"` in the XHTML **and** guarded again inside the
controller method. Testing the controller guard by stripping `disabled` in the
browser does not work, and the result looks alarmingly like a silent failure:

```js
// looks like it should reach the server, but does not
const b = document.getElementById('form:btnApprove');
b.disabled = false;
b.removeAttribute('disabled');
b.classList.remove('ui-state-disabled');
b.click();                       // POST happens, action never runs
```

The POST *is* sent, the page reloads, and **nothing happens** — no navigation,
no message, no exception, an empty `div.ui-messages`, and nothing in
`server.log`. That is not a bug in the page: JSF re-evaluates the component's
`disabled` attribute server-side during *decode* and refuses to queue the action
event for a disabled `UICommand`, no matter what the client submitted. The
client-side attribute is irrelevant.

Two consequences when verifying a fix:

- **The `disabled` binding is itself a real server-side guard**, not just
  cosmetics — worth stating in the PR rather than dismissing it as "`disabled`
  is not a guard". It also survives browser **Back**: a JSF page re-renders from
  the server on back-navigation, so a button disabled by current state comes
  back disabled, not enabled from cache.
- **A controller guard sitting behind such a binding is unreachable from the
  UI** while the binding holds. It is still worth having as defence-in-depth
  (non-UI callers, another page that doesn't bind `disabled`), but do not claim
  you "verified the guard fires" — you verified the *outcome* (the action was
  refused). Verify that outcome in the DB instead: assert the row the action
  would have written does not exist.

To exercise the controller guard for real you need a caller that isn't the
disabled button — a second session whose page was rendered before the state
changed *and* whose button is not `disabled`-bound, or a direct unit/integration
call.


## 132. `Bill.referenceBill` means different things on different flows — don't branch on "is it set", branch on the bill's own type

While fixing issue #23871 (Interim Bill Medicine list/total missing the
porter-flow ward return), a first JPQL attempt resolved a Medicine bill's
"issuing department" by checking `bb IS NULL AND rb IS NULL` → use the bill's
own department, else (when `referenceBill` was set) walk back through
`referenceBill.fromDepartment`. That looked safe until DB verification showed
a plain `ISSUE_MEDICINE_ON_REQUEST_INWARD` issue bill get silently
misclassified under the *requesting ward's* department instead of the issuing
pharmacy's — because `PharmacySaleBhtController`/`PharmacyRequestForBhtController`
also set `referenceBill` on the **issue** bill, pointing back to the
`REQUEST_MEDICINE_INWARD` request bill that spawned it. That's a completely
different relationship from `WardPharmacyReturnToPharmacyController`'s
porter-return bill, which sets `referenceBill` to point at the
`ACCEPT_ISSUED_MEDICINE_INWARD` *receive* bill it's returning against.

Both relationships exist on the *same entity field* (`Bill.referenceBill`),
so `rb IS NOT NULL` alone cannot tell them apart. The fix: also gate on
`type(b) = BilledBill` (the porter return's own entity subclass) before
trusting `referenceBill` for department resolution — everything else
(including an issue bill that happens to carry a `referenceBill`) falls
through to the bill's own `department`. Caught only by comparing a JPQL
query's actual output against a hand-computed expectation per bill
(`SELECT ID, BILLTYPEATOMIC, NETTOTAL, BILLEDBILL_ID, REFERENCEBILL_ID FROM
bill WHERE ...` cross-checked against the resolved department for each row) —
the UI total looked plausible (off by the exact value of one issue bill
landing in the wrong bucket, easy to miss without doing the arithmetic).
General lesson: when a link field is reused across unrelated flows, key the
branch off the row's own concrete type, not off whether the field is
populated.


## 134. `f:validateRegex` on a `p:inputText` bound to a `Map<String,String>` entry fires even on a blank/untouched row — with no visible error

Building the Inpatient Package "Charge Type Amounts" grid (issue #24127) — one `p:inputText` per
`InwardChargeType`, each bound to a per-row `Map<String,String>` entry
(`#{controller.amountInputMap[ct.name()]}`, the established pattern from
`InwardChargeTypeLabelController`) — an `<f:validateRegex pattern="^\d{1,10}(\.\d{1,2})?$"/>` on that
input silently blocked every Save: JSF's normal "skip attached validators when the submitted value is an
empty string" behavior did not hold here, so **every blank row in the grid failed the regex**, not just
the ones a user actually typed into. Symptom was easy to miss: Save just did nothing — `p:growl` stayed
empty (no `ui-message` element rendered anywhere, since no `h:message` was wired to the per-row inputs),
and the only server-visible trace was `aria-invalid="true"` plus a `ui-state-error` class on every empty
`<input>` in the re-rendered panel (confirm with a `browser_evaluate` counting
`input.className.includes('ui-state-error')` across the grid — in this case 24 of 25 rows on the visible
page, the one exception being the single row that actually had a valid value typed in).

Fix: drop the `f:validateRegex` entirely and rely on server-side parsing instead (the controller's
`saveSelected()` already had to tolerantly parse each map entry into a `Double`, skipping blank/unparsable
ones — that parse step is the real validation and doesn't need a client-side echo). If client-side format
hinting is still wanted for a Map-bound field, verify empty rows explicitly (fill nothing, Save, assert no
`ui-state-error`) rather than assuming the standard JSF empty-value skip applies.

Separately: a `p:dataTable` with `paginator="true"` only keeps the **current page's** rows in the DOM —
editing page 1, clicking to page 2, then Save only submits page 2's inputs; page 1's edits are silently
lost (nothing in the DOM to decode them from). For a grid with a single page-level Save button (no
per-row save), use `scrollable="true" scrollHeight="..."` instead of pagination so every row stays in the
DOM and submits together.
