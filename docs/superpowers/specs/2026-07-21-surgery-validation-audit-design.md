# Surgery Validation with Audit Tracking — Design

**Issue**: [hmislk/hmis#22287](https://github.com/hmislk/hmis/issues/22287)
**Date**: 2026-07-21

## Problem

After Theatre completes a surgery, Billing needs to review every bill generated
under that surgery (room, service, timed-service, pharmacy issue, professional
fee, assisting fee, payment) against the paper Green Sheet, and mark each bill
as checked — exactly like the existing interim-bill "Mark As Checked" flow,
but scoped to one surgery instead of one admission.

Once every bill under the surgery is checked, an authorized Billing user
should be able to mark the surgery **Completed** ("validated"). After that:

- Theatre/Billing can no longer add further room charges, services, timed
  services, medicines, or professional fees against that surgery.
- The Surgery Dashboard shows the surgery as Completed with those add actions
  disabled.
- An authorized user can **Revert** the Completed status (separate
  privilege), re-enabling additions.
- Every validate/revert action must be recorded for audit: who, when, and
  (for revert) why.

There is currently no page that shows "all bills for one surgery" — only the
per-admission interim bill (`inward_bill_intrim.xhtml`), and no concept of a
surgery-level completed/locked state.

## Existing mechanisms this design reuses

- **A surgery *is* a `Bill`** (`billType=BillType.SurgeryBill`), with its
  clinical data in a child `PatientEncounter` (`Bill.procedure`). There is no
  separate `Surgery` entity.
- **Child bills link back via `Bill.forwardReferenceBill`** — every
  room/service/timed-service/pharmacy/professional-fee bill generated under a
  surgery sets `forwardReferenceBill` to the surgery bill. This is the
  existing pattern `SurgeryBillController` already queries
  (`findActiveBillsByForwardRef`, `createIssueTable`).
- **"Checked" already exists** as `Bill.checkeAt` / `Bill.checkedBy`, set by
  `InwardSearch.markAsChecked()` / `markAsUnChecked()`. (`Bill.checked` — the
  boolean — is a *different*, unrelated flag used by GRN/purchase workflows;
  not to be confused with the checked-bill mechanism.)
- **`Bill.completed` / `completedBy` / `completedAt` already exist** and are
  used by several pharmacy GRN/return/purchase workflows, but are **never
  set or read for `SurgeryBill`-typed bills**. Confirmed via full-repo grep.
  Since only the *current* completed/not-completed state matters (per user:
  "complete or not is all that is required... completing and reverting any
  number of times will have no issue"), these fields are reused directly for
  surgery validation — no new persisted state field needed.
- **Audit trail**: reuse the generic `AuditEvent` entity /
  `AuditService.logEncounterAudit(...)`, the same mechanism built for the
  Inpatient Event History work (#22232). Every validate/revert action is
  logged as a full history row (user, timestamp, before/after JSON) — this
  is where "who/when/why, every occurrence" lives, not on the `Bill` row
  itself.
- **Privilege pattern**: enum entries in `Privileges.java` (with a menu
  category `switch` case) + registration in `UserPrivilageController.java`'s
  privilege tree, checked via `webUserController.hasPrivilege(...)`.

## Design

### 1. Data model — no schema changes

No new entity, no new columns. Surgery-validated state is:

| Concept | Field | Notes |
|---|---|---|
| Is the surgery completed/validated? | `Bill.completed` (on the surgery bill) | reused, currently unused for SurgeryBill |
| Who validated / last un-reverted it | `Bill.completedBy` | reused |
| When | `Bill.completedAt` | reused |
| Revert history (who/when/reason) | `AuditEvent` rows only | not persisted on `Bill` — see below |

Revert does **not** get dedicated `Bill` columns. Validate/revert can happen
any number of times; only the current state matters on the `Bill`, and full
history is recoverable from `AuditEvent`.

### 2. New page: per-surgery bill summary

New XHTML page `src/main/webapp/theater/surgery_bill_summary.xhtml`, logic
added directly to the existing `SurgeryBillController` (`@SessionScoped`,
already holds the "current surgery" as `surgeryBill`, already `@EJB`-injects
`AuditService`).

Structure mirrors `inward_bill_intrim.xhtml`'s tabbed layout, but every query
filters by `forwardReferenceBill = surgeryBill` instead of
`patientEncounter = admission`. Tabs:

- Room Details (theatre/surgery room charges)
- Timed Service
- Service Details
- Medicine Issue
- Store Issue
- Medicines & Surgical Supplies
- Professional Fees
- Assisting Fees
- Payments

("Out Side Charge Details" is intentionally omitted — it's an admission-level
concept, not tied to a specific surgery.)

Each row shows `checkeAt` / `checkedBy` (read-only, same as interim bill) and
a "View Bill" link using the same `f:setPropertyActionListener` /
bill-type-specific reprint page pattern as the interim bill.

Entry point: a new button on `patient_surgery.xhtml`'s surgery-selected panel
("View Surgery Bill Summary" or similar), navigating via a new
`SurgeryBillController` method that keeps `surgeryBill` set (it already is,
being session-scoped) and returns the new page outcome.

### 3. Back-link from bill view pages

On the bill-view pages that already have "Back To Interim"
(`inward_reprint_bill.xhtml`, `inward_reprint_bill_professional.xhtml`,
`inward_reprint_bill_payment.xhtml`, and the pharmacy reprint equivalents
reached from Medicine/Store Issue rows), add a second button — "Back to
Surgery Summary" — rendered only when the viewed bill's `forwardReferenceBill`
is non-null and is itself a `SurgeryBill`. Action navigates to
`/theater/surgery_bill_summary` with an actionListener that re-seeds
`surgeryBillController.surgeryBill` from the viewed bill's
`forwardReferenceBill`, mirroring how "Back To Interim" reseeds
`bhtSummeryController` via `createTables()`.

### 4. Validate action

- New privilege `InwardSurgeryValidate`.
- Button on the surgery summary page (and/or dashboard), rendered when
  `hasPrivilege('InwardSurgeryValidate')`.
- **Enabled only when every bill under the surgery has `checkeAt != null`**
  (query all bills with `forwardReferenceBill = surgeryBill`, reject if any
  has a null `checkeAt`). Show a clear validation error listing what's still
  unchecked if the user tries anyway.
- Action: `surgeryBill.setCompleted(true)`, `setCompletedBy(loggedUser)`,
  `setCompletedAt(now)`; `billFacade.edit(...)`.
- Audit: `auditService.logEncounterAudit(surgeryBill.getPatientEncounter().getParentEncounter() /* admission-level PE */, "Validate Surgery", before, after, loggedUser, "Bill", surgeryBill.getId())` —
  logged against the **admission-level** `PatientEncounter` (not the
  surgery's own `procedure` sub-encounter) so it surfaces in the existing
  Patient Story / Inpatient Event History timeline for free.

### 5. Revert action

- New privilege `InwardSurgeryValidationRevert` (separate from Validate, per
  issue requirement that only "authorized Billing users" can revert).
- Explicit "Revert Validation" button, rendered when
  `hasPrivilege('InwardSurgeryValidationRevert')` **and** `surgeryBill.completed`.
- Prompts for a reason (simple text input in a confirm dialog / small
  `p:dialog`, not a new persisted field — the reason travels only into the
  audit log's JSON payload).
- Action: `surgeryBill.setCompleted(false)`, `setCompletedBy(null)`,
  `setCompletedAt(null)`; `billFacade.edit(...)`.
- Audit: `auditService.logEncounterAudit(..., "Revert Surgery Validation", before, after, loggedUser, "Bill", surgeryBill.getId())`
  with the reason embedded in the after-JSON (or as part of `eventTrigger`
  detail) — same target admission-level `PatientEncounter`.

### 6. Lock enforcement

**UI layer** — `patient_surgery.xhtml`, the surgery-selected panel's four
add-action buttons:

- "Add Services & Investigations"
- "Direct Issue Medicines"
- "Add Timed Services"
- "Add Professional Fees"

each get `disabled="#{surgeryBillController.surgeryBill.completed}"`
(mirroring the existing `disabled="#{...patientEncounter.paymentFinalized}"`
precedent already used on the "Edit Surgery" / "Add New Surgery" buttons on
the same page). Dashboard also shows a "Completed" status badge when
`surgeryBill.completed`.

**Server layer** (defense in depth — blocks stale-page / direct-navigation
bypass), each rejecting with a JSF error message when
`surgeryBill.isCompleted()`:

- `SurgeryBillController.generalChecking()` — the existing single choke
  point already used for save/edit of the surgery bill itself (guards lines
  ~132, 275, 331, 577, 959, 982, 1007, 1031). Add the completed-check here.
- `BillBhtController`'s surgery-service save path (entered via
  `navigateToSurgeryServices`).
- `InwardTimedItemController.saveSurgeryTimedService()`.
- `InwardProfessionalBillController.saveProfessionalFeeBill()` (entered via
  `navigateToSurgeryProfessionalFees` → `addProfessionalFee()`).

Each of these can load the target surgery bill fresh
(`surgeryBillController.getSurgeryBill()` or the `Bill` reference already
held by the controller) and check `.isCompleted()` before persisting.

### 7. Privileges

In `Privileges.java`:
```java
InwardSurgeryValidate("Inward Surgery Validate"),
InwardSurgeryValidationRevert("Inward Surgery Validation Revert"),
```
plus corresponding `case` labels in the menu-category `switch` (grouped with
the existing `InwardSurgeryAdd` / `InwardSurgeryManage` cases, category
`"Inward"`).

In `UserPrivilageController.java`, register both under the existing
`inwardSurgeryNode`:
```java
new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSurgeryValidate, "Validate Surgery"), inwardSurgeryNode);
new DefaultTreeNode(new PrivilegeHolder(Privileges.InwardSurgeryValidationRevert, "Revert Surgery Validation"), inwardSurgeryNode);
```

## Out of scope

- Changing the meaning or usage of `Bill.completed` for any other bill type —
  this reuse is additive and confirmed non-conflicting for `SurgeryBill`.
- A dedicated UI for browsing surgery-validation audit history — the events
  land in the existing Inpatient Event History timeline
  (`admission_event_history.xhtml`) automatically; no new report page is
  built as part of this issue.
- Auto-revert on unchecking an individual bill — revert is always an
  explicit, separately-privileged action.
- Any changes to the baby-admission workflow that is being developed in
  parallel; this issue only touches surgery/theatre bills
  (`forwardReferenceBill` chains rooted at a `SurgeryBill`).

## Testing plan

Playwright pass against local Payara: select a surgery with a mix of
service/pharmacy/professional-fee child bills, check each bill from the new
summary page's "View Bill" links, confirm the Validate button stays disabled
until all are checked, validate, confirm dashboard buttons disable and show
Completed, revert with a reason, confirm buttons re-enable, and confirm both
events appear in the admission's Inpatient Event History timeline. DB
verification: `bill.completed`/`completedby`/`completedat` columns on the
surgery bill row, and corresponding `AuditEvent` rows in the audit schema.
