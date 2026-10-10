# Playwright E2E — Page- and module-specific quirks (pharmacy, inward, theatre, GRN, lab, reports)

Part of the [Playwright E2E Workflow](../playwright-e2e-workflow.md). Read only the section you need.

- [21. Inward "Add Services" item picker — the Filter box does not load other departments' items](#21-inward-add-services-item-picker--the-filter-box-does-not-load-other-departments-items)
- [22. Inward pharmacy margin lookup uses the *inpatient* department, not the issuing pharmacy](#22-inward-pharmacy-margin-lookup-uses-the-inpatient-department-not-the-issuing-pharmacy)
- [23. Three authoring gotchas found via E2E on the role-template pages (issue #22023)](#23-three-authoring-gotchas-found-via-e2e-on-the-role-template-pages-issue-22023)
- [29. GRN costing Save→Finalize→Approve: `Difference` guard needs a real keyup on Invoice Total at EVERY step](#29-grn-costing-savefinalizeapprove-difference-guard-needs-a-real-keyup-on-invoice-total-at-every-step)
- [30. `ward_pharmacy_bht_issue_request_bill.xhtml` — "New Bill" silently discards unsaved items](#30-ward_pharmacy_bht_issue_request_billxhtml--new-bill-silently-discards-unsaved-items)
- [31. `ward_pharmacy_bht_issue_request_bill.xhtml`'s "Add Dispense Only" path sets `department`/`toDepartment` backwards](#31-ward_pharmacy_bht_issue_request_billxhtmls-add-dispense-only-path-sets-departmenttodepartment-backwards)
- [40. Auto-substitution can silently turn a "zero stock" test case into "issued in full"](#40-auto-substitution-can-silently-turn-a-zero-stock-test-case-into-issued-in-full)
- [49. Pharmacy Transfer Issue: "Request From" is the requester, not the issuer — and the entity-based `TransferIssueForRequestsController` "Issue" button is commented out in favor of the native-SQL path](#49-pharmacy-transfer-issue-request-from-is-the-requester-not-the-issuer--and-the-entity-based-transferissueforrequestscontroller-issue-button-is-commented-out-in-favor-of-the-native-sql-path)
- [53. "Pharmacy Bill Search by Bill Type" has two different pages — pick the one keyed on `billType`, not `billTypeAtomic`](#53-pharmacy-bill-search-by-bill-type-has-two-different-pages--pick-the-one-keyed-on-billtype-not-billtypeatomic)
- [54. `pharmacy_fast_retail_sale_for_cashier.xhtml` "Settle Bill At Cashier" 500s with a Patient cascade error if the Patient Name field is left blank](#54-pharmacy_fast_retail_sale_for_cashierxhtml-settle-bill-at-cashier-500s-with-a-patient-cascade-error-if-the-patient-name-field-is-left-blank)
- [55. `inward_bill_professional.xhtml` "Add Professional Fee" silently no-ops if the Speciality autocomplete is left empty](#55-inward_bill_professionalxhtml-add-professional-fee-silently-no-ops-if-the-speciality-autocomplete-is-left-empty)
- [58. `inward_admission.xhtml` Room No autocomplete excludes the room already reserved by the very appointment being admitted; a required-config error can look like a blocked flow](#58-inward_admissionxhtml-room-no-autocomplete-excludes-the-room-already-reserved-by-the-very-appointment-being-admitted-a-required-config-error-can-look-like-a-blocked-flow)
- [59. `CreditCompanyBillSearch.printPreview` is a single shared flag reused for two different meanings — viewing a bill before cancelling can make the cancel form permanently unreachable via normal navigation](#59-creditcompanybillsearchprintpreview-is-a-single-shared-flag-reused-for-two-different-meanings--viewing-a-bill-before-cancelling-can-make-the-cancel-form-permanently-unreachable-via-normal-navigation)
- [60. GRN receive/approve `Invoice Total` resets to 0.00 on every page (re)load and needs a *real* blur, not just `fill()`, to pass the "invoice does not match" check](#60-grn-receiveapprove-invoice-total-resets-to-000-on-every-page-reload-and-needs-a-real-blur-not-just-fill-to-pass-the-invoice-does-not-match-check)
- [61. "Generate Supplier Payments" only lists Credit-payment-method GRNs — a Cash GRN's return never shows up there, by design](#61-generate-supplier-payments-only-lists-credit-payment-method-grns--a-cash-grns-return-never-shows-up-there-by-design)
- [65. Theatre "Add New Surgery" — the Surgery Name autocomplete is `Item` rows (`DTYPE='ClinicalEntity'`), not a dedicated table](#65-theatre-add-new-surgery--the-surgery-name-autocomplete-is-item-rows-dtypeclinicalentity-not-a-dedicated-table)
- [66. PrimeFaces `p:tree` privilege picker (`admin/users/user_privileges.xhtml`) — clicking a toggler icon directly does nothing; use the Search box instead, and re-login after any privilege change](#66-primefaces-ptree-privilege-picker-adminusersuser_privilegesxhtml--clicking-a-toggler-icon-directly-does-nothing-use-the-search-box-instead-and-re-login-after-any-privilege-change)
- [67. `inpatient_search.xhtml` / `inward_search.xhtml` date filter defaults to "last 7 days" and silently returns "No records found" for older admissions — even when searching by exact BHT No](#67-inpatient_searchxhtml--inward_searchxhtml-date-filter-defaults-to-last-7-days-and-silently-returns-no-records-found-for-older-admissions--even-when-searching-by-exact-bht-no)
- [70. `inward/inward_bill_service.xhtml`'s "Settle" button silently returns to the edit screen — with the same items still loaded — when a fee row needs a Staff pick](#70-inwardinward_bill_servicexhtmls-settle-button-silently-returns-to-the-edit-screen--with-the-same-items-still-loaded--when-a-fee-row-needs-a-staff-pick)
- [75. Inpatient discharge chain has a strict, undocumented order — and Physical Discharge requires the Final Bill to already exist](#75-inpatient-discharge-chain-has-a-strict-undocumented-order--and-physical-discharge-requires-the-final-bill-to-already-exist)
- [81. `theater/inward_search_surgery.xhtml`'s "Search All" checkbox does not widen the date filter](#81-theaterinward_search_surgeryxhtmls-search-all-checkbox-does-not-widen-the-date-filter)
- [85. `inward_bill_service.xhtml`'s patient search auto-selects on an exact BHT match — no suggestion click needed](#85-inward_bill_servicexhtmls-patient-search-auto-selects-on-an-exact-bht-match--no-suggestion-click-needed)
- [88. `pharmacy_search_pre_bill_for_return_item_only.xhtml`'s "Return Item Only" button can fail completely silently — no `p:messages`/growl update, plus `Bill` is L2-cached](#88-pharmacy_search_pre_bill_for_return_item_onlyxhtmls-return-item-only-button-can-fail-completely-silently--no-pmessagesgrowl-update-plus-bill-is-l2-cached)
- [91. `ward/ward_pharmacy_bht_issue_request_bill.xhtml`'s "New Bill" button discards the current draft instead of saving it, and `ward_pharmacy_bht_issue.xhtml`'s "Issue to BHT" rejects (with a growl message, easy to miss in a scripted run) until a Porter/Staff is picked](#91-wardward_pharmacy_bht_issue_request_billxhtmls-new-bill-button-discards-the-current-draft-instead-of-saving-it-and-ward_pharmacy_bht_issuexhtmls-issue-to-bht-rejects-with-a-growl-message-easy-to-miss-in-a-scripted-run-until-a-porterstaff-is-picked)
- [95. Theatre's "+ Add New Surgery" briefly lands on a generic, blank-looking `admission_profile.xhtml` — the surgery Bill is already created; go through "Surgeries for BHT" to reach it](#95-theatres--add-new-surgery-briefly-lands-on-a-generic-blank-looking-admission_profilexhtml--the-surgery-bill-is-already-created-go-through-surgeries-for-bht-to-reach-it)
- [107. `inward_lab_dashboard.xhtml`'s "Send to Lab" has no `p:messages`/`p:growl` at all — a missing Transporter silently no-ops the whole action](#107-inward_lab_dashboardxhtmls-send-to-lab-has-no-pmessagespgrowl-at-all--a-missing-transporter-silently-no-ops-the-whole-action)
- [125. Setting up an inward final-bill test: charges are blocked after nursing discharge, and MRI items are billed from the Diagnostic Centre](#125-setting-up-an-inward-final-bill-test-charges-are-blocked-after-nursing-discharge-and-mri-items-are-billed-from-the-diagnostic-centre)
- [130. A failed `errorCheck()` can look exactly like a dead Settle button](#130-a-failed-errorcheck-can-look-exactly-like-a-dead-settle-button)
- [143. Cashier float / handover / shift-end flows: menu paths, required fields, and what to verify](#143-cashier-float--handover--shift-end-flows-menu-paths-required-fields-and-what-to-verify)

---

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


## 55. `inward_bill_professional.xhtml` "Add Professional Fee" silently no-ops if the Speciality autocomplete is left empty

On "Add New Professional Fees", the `+ Add Professional Fee` button is a
`type="submit"` full postback guarded only by a JS `confirm(...)` — clicking
it and accepting the dialog looks successful (page reloads, no visible
error) but the row never appears in "Professional Fees for This Encounter"
and no `BILLFEE` row is inserted, if the **Speciality** autocomplete (above
Doctor) was left blank. This is the same zero-observable-signal
required-field pattern as §37, just on a different page/field — the Doctor
field alone is not enough. Fix: search and select a Speciality (e.g. type
`PHYSICIAN`, press Enter) before Doctor/Fee Amount/Add. Confirmed via
`mysql.general_log`: with Speciality empty, no `INSERT INTO BILLFEE`
statement reaches the server at all; with it filled, the insert fires
immediately. Verified while testing issue #22665.


## 58. `inward_admission.xhtml` Room No autocomplete excludes the room already reserved by the very appointment being admitted; a required-config error can look like a blocked flow

While testing issue #22719 (appointment → admission → deposit conversion), two admission-form gotchas surfaced together:

- **Room No autocomplete only lists currently-*available* rooms** — a room
  already reserved for the appointment/patient being admitted (e.g. via the
  appointment's own `Reservation`) does **not** appear in the completion list,
  even though it's "theirs." Typing the exact room number/name returns "No
  results found." This isn't a bug in the flow under test — just pick any
  other available room from the list (e.g. `Room 410` instead of the
  originally-reserved `Room 101`) to proceed; the room shown on the
  reservation and the room picked at admission time are independent fields.
- **A hidden `ConfigOption` boolean can block the whole Admit action with no
  visual hint on the form.** `AdmissionController.errorCheck()`
  (`AdmissionController.java` ~2235-2297) runs a whole chain of these gates,
  but only when the umbrella key `"Patient Details Required in Patient
  Admission"` is `true` **and** the admission isn't a Rapid/Temp A&E one
  (those admit with deliberately-incomplete demographics — issue #21183).
  Once past that gate, the individual checks run in a fixed order — Title,
  Gender, **Age**, Name, **Address**, Area, Mail, then (for a non-baby
  admission) NIC and Phone Number — each independently toggled by its own
  `"Patient <Field> is Required in Patient Admission"` `ConfigOption` (all
  default `false`). On this Galle Co-op local dev DB, Age and Address were
  both `true` (confirmed 2026-09-30 while recording the Package Admission
  demo video), so fixing the DOB-triggered "Patient Age is Required" error
  just revealed "Patient Address is Required" next — **and if Name were also
  enabled, it would have fired in between the two**, since Name is checked
  before Address. The method returns on the *first* failing check, so a
  single error message never tells you how many more are enabled behind it —
  check the config keys directly (or just fill every field) rather than
  assuming the one error you see is the last one. Related traps while fixing
  these:
  - Typing into the **Years/Months/Days** age inputs on `patient_edit.xhtml`
    looks like it commits (`textbox "Years": "30"`) but doesn't persist a DOB —
    that widget only *computes* a DOB client-side via a JS listener that a
    plain `fill()`/`pressSequentially()` doesn't reliably trigger. Set the
    **Date of Birth** `p:calendar` field directly instead (click → Ctrl+A →
    type the value → Escape → Save). The field's expected format is not a
    generic `dd/mm/yyyy` — it's whatever the "could not be understood as a
    date and time" validation error echoes back as its `Example:` (on this
    build, `dd/MMM/yyyy - HH:mm:ss`, e.g. `01/Jan/1990 - 00:00:00`); typing a
    plausible-looking but wrong format fails validation silently-ish (a growl
    naming the field, easy to miss in a hurry). Verify
    `SELECT DOB FROM person WHERE ID = (SELECT PERSON_ID FROM patient WHERE ID = <patientId>)`
    returns a non-NULL row for the specific patient under test before
    retrying the admission — an unfiltered `SELECT DOB FROM person` returns
    every patient in the DB and can't confirm the one that matters.
  - To find *which* config key is blocking an error message with no field
    reference, `grep` the exact error string in
    `src/main/java/com/divudi/bean/inward/AdmissionController.java` to find
    the `configOptionApplicationController.getBooleanValueByKey("...")` call,
    then toggle it via **Admin → Manage → Application Options → List
    Application Options → filter by key → Edit Option** (per §26 — never via
    raw SQL, the L2 cache won't see it). If you flip a real setting to unblock
    a test, **toggle it back afterward** and confirm via
    `SELECT OPTIONVALUE FROM configoption WHERE OPTIONKEY = '...'` — this is
    live config on a real hospital's local dev copy, not disposable test data.
    Simplest path for a one-off demo/test admission: fill DOB, Name, and
    Address for real rather than chasing the config toggle — all three are
    normal fields on the form anyway.


## 59. `CreditCompanyBillSearch.printPreview` is a single shared flag reused for two different meanings — viewing a bill before cancelling can make the cancel form permanently unreachable via normal navigation

On `credit/credit_company_bill_search.xhtml` → **View** → `inpatient_credit_company_bill_reprint.xhtml` → **To Cancel** → `inpatient_credit_company_bill_cancel.xhtml`, the cancel page conditionally
renders either the cancel **form** (`rendered="#{!creditCompanyBillSearch.printPreview}"`) or a
read-only "cancellation receipt" preview (`rendered="#{creditCompanyBillSearch.printPreview}"`).
`BillSearch.navigateToViewBillByAtomicBillType()` (used by the search page's **View** button) calls
`creditCompanyBillSearch.setBill(bill)` (which resets `printPreview=false` via `recreateModel()`)
**immediately followed by** `creditCompanyBillSearch.setPrintPreview(true)` — by design, so the
Reprint page shows a print preview. But `printPreview` is `@SessionScoped` and shared with the
Cancel page, and clicking **To Cancel** is a plain outcome-string navigation (no bean method call)
that never resets it. Result: landing on the cancel page via the only in-UI path always shows the
receipt view instead of the cancel form — **there is no button an end user can click to actually
reach the cancel form**, even though nothing has been cancelled yet (verify via
`SELECT CANCELLED FROM BILL WHERE ID=...` — it's still `0`). This is a real product bug, not a
Playwright limitation; flag/file it rather than silently building around it. Found while verifying
issue #19931.

**Workaround used only for E2E verification** (not a fix an end user has access to): reach the same
bill via `credit/credit_company_bill_search_billItems.xhtml` → **Search BHT** → **View Bill**
instead. That page's button does a *raw* `f:setPropertyActionListener value="#{b.bill}"
target="#{creditCompanyBillSearch.bill}"` with no follow-up `setPrintPreview(true)`, so
`printPreview` stays `false`. It navigates to the *generic* `credit_company_bill_reprint.xhtml`
(whose own **To Cancel** button targets `credit_company_bill_cancel.xhtml` and the *different*
`creditCompanyBillSearch.cancelBill()` method — **do not click that button**, it may produce the
wrong `BillTypeAtomic` for an inpatient bill). Instead, once `creditCompanyBillSearch.bill` +
`printPreview=false` are set, `browser_navigate` directly to
`inpatient_credit_company_bill_cancel.xhtml` — the session-scoped bean state carries over and the
correct cancel form (bound to `cancelCreditCompanyPaymentBill()`) renders.

Separately: the cancel form's "Enter a comment" `p:inputText` is required by
`CreditCompanyBillSearch.errorCheck()` (`"Please enter a comment"`), but that page has no visible
`<p:messages>`/`<h:messages>` for the resulting `JsfUtil.addErrorMessage(...)` — clicking **Cancel**
with it empty just re-renders the identical form with **zero visible feedback and zero DB change**,
easy to mistake for the click not registering at all. Always fill the comment field first; if a
"Cancel" (or similarly `ajax="false"`) button appears to no-op, check for this pattern before
assuming a click/ref problem.


## 60. GRN receive/approve `Invoice Total` resets to 0.00 on every page (re)load and needs a *real* blur, not just `fill()`, to pass the "invoice does not match" check

On `pharmacy_grn_costing_with_save_approve.xhtml` (both the initial Finalize and the separate
Approve page load), `Invoice Total` starts blank/0.00 every time the page is (re)rendered —
including the second time you land on the same GRN for the Approve step, even though you already
filled it once during Finalize. Two gotchas stack here:

1. **It must be re-filled at every stage** (Finalize *and* Approve) — don't assume a value entered
   once persists across the finalize→approve navigation.
2. **A plain `browser_type`/`.fill()` + `Tab` does not reliably commit it** — the page kept
   re-showing `Difference: -<amount>` (computed server-side from the *old* 0.00) and Finalize
   failed with "The invoice does not match..! Check again" even though the input visibly showed
   the typed value. The fix: `browser_click` into the field, `Control+a`, `browser_type` with
   `slowly: true`, then an explicit `browser_click` on an unrelated static element (e.g. the page
   heading) to force a real blur — only then does `Difference` recompute to `0.00` and
   Finalize/Approve succeed. Verify via `browser_find` on "Difference" before clicking
   Finalize/Approve, not just by eyeballing the Invoice Total box.

Found while verifying issue #18280 (GRN Return refundAmount fix).


## 61. "Generate Supplier Payments" only lists Credit-payment-method GRNs — a Cash GRN's return never shows up there, by design

`SupplierPaymentController.fillUnsettledCreditPharmacyBills()` (and the sibling return-bills
method) hard-filter on `PaymentMethod.Credit`. A GRN received with Payment Method = **Cash** will
never appear on `list_bills_to_generate_supplier_payments.xhtml` or `list_all_grns.xhtml`'s
"Prepare Payment" flow, no matter its return/refund state — there's nothing owed to the supplier
for a bill already settled in cash at receipt, so this is correct behavior, not a bug. If a test
needs to reach the actual Supplier Payment screen (`generate_supplier_payment.xhtml`), the GRN
**must** be created with Payment Method = **Credit** at receive time; a Cash-paid test GRN is only
verifiable at the DB level (`BILL.REFUNDAMOUNT`/`PAIDAMOUNT`/`NETTOTAL`), not through this UI path.
Found while verifying issue #18280.


## 65. Theatre "Add New Surgery" — the Surgery Name autocomplete is `Item` rows (`DTYPE='ClinicalEntity'`), not a dedicated table

On `theater/patient_surgery.xhtml`'s "Add Surgery" panel, the "Surgery Name" `p:autoComplete`
(`ProcedureController.completeProcedures`) queries `ClinicalEntity` — which is a
`SINGLE_TABLE`-inheritance subclass of `Item` (discriminator `DTYPE='ClinicalEntity'`), not its
own table. There is no `CLINICALENTITY` table to query directly; look up seed rows with:
```sql
SELECT ID, NAME FROM ITEM WHERE DTYPE='ClinicalEntity' AND SYMANTICTYPE='Therapeutic_Procedure' AND RETIRED=0;
```
A query for an item name absent from that set (e.g. "Appendec" typo, or a name that isn't seeded)
silently returns "No results found" with no error — this looks like a missing feature but is just
an empty/mistyped query. Confirmed working seed name: "Appendicectomy". Verified while testing
issue #20891.


## 66. PrimeFaces `p:tree` privilege picker (`admin/users/user_privileges.xhtml`) — clicking a toggler icon directly does nothing; use the Search box instead, and re-login after any privilege change

The "Manage User Privileges" tree (widget var `privTree`) lazily renders — its
`ui-treenode-children` `<ul>`s stay `display:none` until PrimeFaces actually
expands that node client-side. A raw DOM `.click()` on the toggler icon (or
calling the widget's `expandNode()` directly) does **not** flip
`aria-expanded`/unhide the children — the tree only reliably expands and
scrolls to a match through its own **Search** textbox: type the privilege's
exact display label, then send a `Backspace` (a plain `fill()` doesn't fire
the keyup the search listens on) and wait ~1-2s for the AJAX re-render. After
that the matched `treeitem`'s checkbox can be clicked directly by locating it
under `span.ui-treenode-label` → `closest('li.ui-treenode')` →
`div.ui-chkbox-box`.

Also: privileges are loaded into the session at login, not read live. After
granting/revoking a privilege for the test user, you must log out and log
back in (department re-selection included) before the new grant takes
effect in that user's session — testing "immediately after Update User
Privileges" without a re-login will silently show the old (stale)
privilege behavior. Verified while testing issue #22906.


## 67. `inpatient_search.xhtml` / `inward_search.xhtml` date filter defaults to "last 7 days" and silently returns "No records found" for older admissions — even when searching by exact BHT No

The Admissions search page's `From Date`/`To Date` fields default to a
rolling 7-day window and are combined with the BHT No / other filters via
AND, not OR. Searching by an exact BHT number for an admission outside that
window returns "No records found" with no indication that the date range
(not the BHT number) is the reason. Always widen `From Date` back to (or
before) the admission's actual `DATEOFADMISSION` — check it in the DB first
(`SELECT DATEOFADMISSION FROM PATIENTENCOUNTER WHERE ID=...`) — before
concluding a BHT number search failed. The `p:calendar` popup only navigates
one month per "Previous Month" click; budget one click per month of gap.
Verified while testing issue #22906.


## 70. `inward/inward_bill_service.xhtml`'s "Settle" button silently returns to the edit screen — with the same items still loaded — when a fee row needs a Staff pick

Clicking **Settle** (`ajax="false"`, `confirm()`-guarded) for a bill whose Fees tab
has a row with a non-null `speciality` (e.g. a "Technician Fee") but no `staff`
selected does **not** navigate to print preview and does **not** throw a visible
error near the button — the page does a full reload and lands back on the exact
same "Add Services" edit view, Bill Items/Fees tabs still populated, looking
almost identical to the pre-click state. The only server-side evidence is a
`Growl` message baked into the reloaded HTML (`msgs:[{summary:"Please select
Staff",...,severity:'error'}]`), which is easy to miss since no dialog or
distinct page state change signals failure. Confirm success/failure by grepping
the full-postback response body for `Growl`/`severity:'error'` (per §32's
pattern), or simply check whether the "Investigation or Service" picker /
"Add" button are still rendered afterward — their presence means Settle did not
go through. Fix in automation: after any Fees-tab row shows a "Select Staff"
dropdown, pick a value from it (PrimeFaces click-option pattern, §13) before
clicking Settle. Found verifying issue #22916.


## 75. Inpatient discharge chain has a strict, undocumented order — and Physical Discharge requires the Final Bill to already exist

To reach "Create Final Bill" on `inward_bill_intrim.xhtml` for a fresh test
admission, the discharge steps on `admission_profile.xhtml` must run in this
exact order — doing them out of order produces a visible error banner, not a
silent failure:

1. **Room discharge** (Room Management → "Discharge from Room") — must
   happen before Nursing Discharge.
2. **Nursing Discharge** (`inward_nursing_discharge.xhtml`, "Confirm Nursing
   Discharge") and **Clinical Discharge** — both must complete before the
   Interim Bill's own "Discharge" button will accept the bill.
3. **Interim Bill → Discharge** (sets `PATIENTENCOUNTER.DISCHARGED=1`) —
   only after step 2. Attempting it earlier shows "Nursing discharge must be
   completed before the bill can be settled."
4. **Create Final Bill** — only after step 3.
5. **Physical Discharge** — counter-intuitively, this can only be confirmed
   **after** the Final Bill already exists; attempting it earlier errors with
   "administrative discharge (final bill) has not been completed."

Verify each stage against `PATIENTENCOUNTER` (`NURSINGDISCHARGED`,
`CLINICALLYDISCHARGED`, `DISCHARGED`, `PHYSICALDISCHARGED`) before moving to
the next — see §32 below for why the UI alone can't be trusted here.

A native `confirm()` dialog handled via `browser_handle_dialog(accept:true)`
immediately returns a "Loading..." page title — that is not proof the
server-side action succeeded. The actual result (success or an error banner)
only appears in the *next* `browser_snapshot`/reload. Always re-snapshot (or
re-query the DB) after handling the dialog before assuming the step passed;
in one session `NURSINGDISCHARGED` stayed `0` for several tool calls after
the dialog was accepted, because the click had actually been rejected
server-side (wrong order) but nothing in the dialog-handling response showed
that.


## 81. `theater/inward_search_surgery.xhtml`'s "Search All" checkbox does not widen the date filter

`SearchController.searchSurgery()` always applies `b.createdAt between :fromDate
and :toDate` — the From/To Date calendars default to **today only**. The
"Search All" checkbox does *not* bypass that; it toggles a separate, required
"select a Surgery Name to search all" filter (`searchKeyword.activeAdvanceOption`)
and throws a validation error ("You Need To select Surgury to Search All") if
checked with no item picked. To find an older surgery bill by BHT/bill number,
leave "Search All" unchecked and widen the **From Date** via the calendar grid
(§18) to cover the record's actual `createdAt` — a plain BHT-number search with
today's default date range silently returns "No records found." for anything
not created today. Verified while testing issue #23249.


## 85. `inward_bill_service.xhtml`'s patient search auto-selects on an exact BHT match — no suggestion click needed

Typing a complete BHT number (e.g. `BHT/55359`) into the "Patient Search"
autocomplete on the Patient Selection screen commits the encounter and re-renders
straight into the Add Services view, without ever showing an autocomplete panel
to click. A test that types the BHT and then waits for a `.ui-autocomplete-panel`
row will time out on a working page. Detect the transition instead — e.g.
`document.body.innerText.includes('Patient Selection') === false`, or the presence
of `form:btnAddIx`. Found while verifying issue #23342.


## 88. `pharmacy_search_pre_bill_for_return_item_only.xhtml`'s "Return Item Only" button can fail completely silently — no `p:messages`/growl update, plus `Bill` is L2-cached

Two independent gotchas stack here, found while testing issue #23304 (reject negative return quantity):

1. **Silent navigation failure.** `PreReturnController.navigateToReturnRetailSaleItemsOnly()` calls
   `pharmacyRetailSaleReturnPolicyService.checkReturnAllowed(bill)` and returns `null` (staying on the
   same page) when the sale bill is older than the "no approval" day limit (default 3 days) and has no
   approved return request — see `PharmacyRetailSaleReturnPolicyService`. `pharmacy_search_pre_bill_for_return_item_only.xhtml`
   has **no `p:messages`/`p:growl` component at all**, so `JsfUtil.addErrorMessage(...)` is added to the
   `FacesContext` but never rendered — clicking "Return Item Only" just silently reloads the same search
   page with zero visible feedback. Don't mistake this for the button/click not registering; check the
   row's day-limit first (a "Request Approval" button appearing alongside "Return Item Only" is the tell
   that the bill is past the no-approval window).
2. **`Bill` is also L2-cached.** Same class of staleness as `feedback_cogs_report_testing_gotcha` and item
   87's `WebUser` case: if you shift a `Bill.createdAt` via raw SQL to get a test bill inside the day-limit
   window, and the app already loaded that `Bill` earlier in the same session (e.g. an earlier search hit
   it), `checkReturnAllowed` keeps evaluating against the stale cached `createdAt` — the day-limit block
   persists even though the DB row is correct. Use a bill your test session has never touched yet for the
   `UPDATE`, or restart the domain, rather than assuming the fresh `UPDATE` took effect.

Also: revert any `createdAt` shift back to the bill's real original value immediately after the test —
day-based reports (Cost of Goods Sold, F15) key off it, and leaving it shifted taints those reports for
both the original date and the shifted date.


## 91. `ward/ward_pharmacy_bht_issue_request_bill.xhtml`'s "New Bill" button discards the current draft instead of saving it, and `ward_pharmacy_bht_issue.xhtml`'s "Issue to BHT" rejects (with a growl message, easy to miss in a scripted run) until a Porter/Staff is picked

Found verifying issue #23470 (BHT substitute suggestions). On the ward-side
request page, the toolbar has both **"New Bill"** and **"Settle Request"**
next to each other. "New Bill" looks like a generic submit/save action but
it actually **discards the in-progress request and resets the form** to a
blank "Start Pharmacy Request for Inpatients" screen — no `BILL` row is
written, no error is shown. Only **"Settle Request"** (which fires a native
`confirm()` — handle it with `browser_handle_dialog`) persists the request
as a `REQUEST_MEDICINE_INWARD` bill.

On the pharmacy-side issue page (`ward_pharmacy_bht_issue.xhtml`), the
**"Issue to BHT"** button (`PharmacySaleBhtController.settlePharmacyBhtIssueAccept`)
also fires a `confirm()`, but if `getPreBill().getToStaff() == null` it
rejects the attempt with the growl message **"Please select the staff
member (porter) who will carry the medicines to the ward."** and returns —
i.e. the **"Porter / Staff Carrying Medicines to Ward"** autocomplete near
the top of the page must be filled first. It's not a silent no-op (the
message is real), but it's easy to miss when driving the page via
Playwright without checking for growl text after every action, and no
exception is logged server-side either way. A DB check for a fresh
`ISSUE_MEDICINE_ON_REQUEST_INWARD` row is the reliable way to catch this in
a script: it comes back empty after a seemingly-successful click-and-confirm
if the Porter field was skipped. Always fill the Porter field (any active
`STAFF` row works for local testing) before clicking "Issue to BHT", and
verify success by querying `BILL` for a new row
rather than trusting the confirm dialog alone.

Also: that Porter/Staff autocomplete searches the `STAFF`/`PERSON` tables,
not `WEBUSER` — a logged-in user's own username (e.g. "Lawan") will not
resolve; search by an actual staff member's name from `STAFF` joined to
`PERSON`.


## 95. Theatre's "+ Add New Surgery" briefly lands on a generic, blank-looking `admission_profile.xhtml` — the surgery Bill is already created; go through "Surgeries for BHT" to reach it

Seen creating test data for issue #23510. Clicking **+ Add New Surgery** on
`theater/patient_surgery.xhtml` navigates to `inward/admission_profile.xhtml`
showing empty Name/Gender/DOB fields and a *different*, just-now Date of
Admission — which looks like the action failed or created a stray blank
encounter. It did not: the surgery `Bill` (`BILLTYPE = 'SurgeryBill'`) was
created against the *original* BHT encounter, and a second, child
`PatientEncounter` (the "procedure" record, `PARENTENCOUNTER_ID` = the BHT's
id) was also created — `admission_profile.xhtml` is just rendering that new,
still-mostly-empty child encounter, which is a separate, likely
pre-existing display gap and not something this fix touched.

**To get back to the surgery you just created**, don't fight that page —
navigate to `theater/inward_bill_surgery_list.xhtml` ("Surgeries for BHT"),
which lists every `SurgeryBill` for the current `patientEncounter` and has a
**Surgery Dashboard** button per row that loads it into
`surgeryBillController.surgeryBill` correctly.


## 107. `inward_lab_dashboard.xhtml`'s "Send to Lab" has no `p:messages`/`p:growl` at all — a missing Transporter silently no-ops the whole action

Found while verifying issue #23578 (block Nursing Discharge until lab
investigations are sent to lab). The dashboard's "Send to Lab" button calls
`InwardLaboratoryController.sendSamplesToLab()` →
`PatientInvestigationController.sendSamplesToLab(true)`, which requires the
"Sample Transporter" `p:autoComplete` to be filled (`transporterMandatory =
true`) and otherwise calls `JsfUtil.addErrorMessage("Transporter is
Missing")` and returns. The page has no `p:messages`/`p:growl` component
anywhere, so clicking Send to Lab with the transporter empty just silently
reloads the search results with the sample still at "Sample Collected" —
same failure class as item 88's first finding, different page. If a
sample's status doesn't advance after clicking Send to Lab with no visible
error, check the Sample Transporter field is actually populated before
suspecting the controller logic.


## 125. Setting up an inward final-bill test: charges are blocked after nursing discharge, and MRI items are billed from the Diagnostic Centre

Found while verifying issue #23723 on the local `coop` DB.

- **Pick a fully open admission.** *Inpatient → Services & Items → Add Services &
  Investigations* refuses to settle for a discharged BHT ("Sorry Patient is
  Discharged!!!") and also for one whose nursing discharge is confirmed ("Cannot
  add charges: nursing discharge has been confirmed for this patient."). Both
  messages appear only in a `p:messages`/growl on a full-page reload that looks
  like the edit screen (§70), so check the messages or the DB. Add every charge
  and professional fee first, then run the discharge chain (§75): Room Details →
  Discharge from Room, Nursing Discharge, Clinical Discharge, then Interim Bill →
  set **Discharge Time** (a `p:datePicker`; use the widget's `setDate()`, §56) →
  Discharge, then Create Final Bill.
- **MRI items (`REPORTING - Dr ...`, `MRI - ...`) live in the `MRI` department,
  which is not one of the department buttons on the Inward session's Add Services
  page.** In production they're billed by the Karapitiya Diagnostic Centre. To
  reproduce that, log in to **OPD - Diagnostic Centre**, open the same Add
  Services menu item, set the page's **Institution** dropdown to *Galle Co
  Operative Hospital* (it defaults to the Diagnostic Centre, and the BHT search
  then finds nothing), search the BHT, and click the **MRI** department button.
  The resulting bill number is `OPDDC//...`, like production's.
- **Add Professional Fee's patient search** only finds the BHT when its Institution
  is right. Changing that dropdown on the Diagnostic Centre session did not take
  effect, but the Inward session defaults to *All Institutions* and works. Enter
  professional fees from the Inward department.
- **Save Final Bill stays disabled until you click Process**, and it silently
  refuses (`checkCatTotal()`) while any category's Adjusted Total differs from
  its Total. That's the default of `Block Inward Final Bill When Category
  Adjusted Total Differs From Actual Total`. Undo test adjustments, or balance
  them, before saving.
- **Creating a new final bill version** (Admission Profile → Manage Final Bills →
  Create New Version) reopens `inward_bill_final.xhtml` in edit mode on an
  already-settled admission. That's a quick way to get a fresh edit view without
  building another admission.


## 130. A failed `errorCheck()` can look exactly like a dead Settle button

On `opd/opd_pre_bill.xhtml` (*Menu → OPD → Billing → Billing for Cashier*),
clicking **Settle** accepted the `confirm()`, left the page unchanged, wrote no
row, and showed **no message** — the classic shape of a broken button. Nothing
was broken: `OpdPreBillController.errorCheck()` had returned `true` on
`patient.getPerson().getArea() == null` ("Please Add Patient Area"), and the
growl carrying that message was gone (or never rendered into the region being
scraped) by the time the snapshot ran.

Before concluding a control is dead, read the controller's `errorCheck()` /
validation method and satisfy **every** field it tests — on this page
`Area` is required even though it carries no `*` marker in the UI. A cheap tell:
the form still holds the values just typed and the URL hasn't changed, which
means the action ran and bailed, not that the click was lost.

Corollary for scraping messages: `.ui-growl-item` is transient. Capture messages
immediately after the click (or screenshot right away) rather than after the
several-second settle wait, or a real validation error reads as silence.


## 143. Cashier float / handover / shift-end flows: menu paths, required fields, and what to verify

All under *Cashier → Financial Transaction Manager*. Design: [Handover Float Propagation](../../billing/handover-float-propagation.md).

| Action | Menu path |
|---|---|
| Send float | Float Management → Float Transfer |
| Receive float | Float Management → Float Transfers to Receive |
| Cancel a sent float (only before it is received) | Float Management → My Float Outs → Cancel |
| Hand over | Shift → Handover Current Shift |
| Recall a handover (sender, only before accept) | Handover → My Handovers → Recall |
| Accept or reject a handover | Handover → Shift Handovers to Accept → To Accept |
| Record excess / shortage | Shift → Record Shift Excess / Record Shift Shortage |
| Shift-end cash, end shift | Shift → Record Shift End Cash in Hand, then End Shift |
| Drawer history | Drawer → My Drawer History |
| Reset a drawer (no API yet, #24433) | Admin → Adjust Drawer Balance (needs `DrawerAdjustmentDirect`) |

- **Float receive needs a comment and a denomination count.** Fill both before clicking Receive.
- **A handover's counted denominations must equal expected cash.** Record any excess or shortage bill first; never test a "handover with a difference".
- **An accepted handover cannot be cancelled.** "Handover cancel" means Recall (sender) or Reject (receiver) while it is still pending.
- **OPD Billing** is under *OPD → Billing*; hover "Billing" to open the submenu. The same item can't be added twice to one bill, so build amounts from different items or quantities.
- **Accept `confirm()` guards** by setting `window.confirm = () => true` before each click (see [§4](../playwright-e2e-workflow.md#4-confirmations-and-double-click-protection)).
- **Verify three things after every step**, not just the screen you are on:
  1. *My Drawer History* for **both** users. An accept adds one Cash row (the counted cash, float included) and no separate float row (#24428). Each non-cash payment in the handover (Card, Cheque, …) adds its own row.
  2. The next *Handover Current Shift* screen: Net Float (Cash) must equal the user's own signed floats plus the signed net floats carried in from accepted handovers.
  3. *Record Shift End Cash in Hand*: if the drawer started the shift at 0, expected cash should equal the drawer's cash balance.
