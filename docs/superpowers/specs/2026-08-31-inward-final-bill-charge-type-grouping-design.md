# Inward Final Bill — Configurable Charge-Type Grouping ("Bundled Custom 1")

**Date:** 2026-08-31
**Branch:** `23340-inward-charge-type-ordering` (continues the Report Order / Final Bill Order work from PR closing #23340)
**Related issue:** none filed yet — client requirement from Coop, raised verbally

## Problem

Coop's inpatient final bill charges are broken down under many `InwardChargeType`
values (Room Charges, Meal Charges, various service charge types, etc.). Coop wants
the **Inward Ledger Report** to keep showing this full breakdown, unchanged — but
wants the **Final Bill print** to show a subset of those charge types (e.g. Room
Charges + Meal Charges + specific service charge types) combined into **one**
printed line, e.g. "Room Charges : <sum>", with no further breakdown. This is a
view-only, print-only requirement — it does not change how charges are billed,
stored, or reported anywhere else.

No other hospital wants this. The design must not change any existing hospital's
final bill print, and must not touch the ledger/breakdown reporting code at all.

Client explicitly wants the grouping to be **admin-configurable** ("pick Room
Charges, Meal Charges, Service Charges as Room Charges") rather than a hardcoded
list — so a future request for a different combination, or from a different
hospital, needs no further code change.

## Existing building blocks this reuses

- PR closing #23340 already added, per `InwardChargeType`, two admin-editable
  `ConfigOption` numbers — **Report Order** and **Final Bill Order** — on the
  existing `InwardChargeTypeLabelController` / `inward_charge_type_labels.xhtml`
  admin page. **Final Bill Order was stored but never consumed anywhere** (its own
  commit message says so). This design is its first real consumer.
- `inward_reprint_bill_final.xhtml` already has a **"Custom Bills" tab** with an
  established pattern for hospital-opt-in print formats: each format
  (`finalBillCustom2/3/4.xhtml`) is gated by its own
  `configOptionController.getBooleanValueByKeyReadOnly('Inward Final Bill - Show
  Custom Bill N Format', default)`, with a "Settings" dialog
  (`custom2ConfigDialog`) exposing a checkbox per format plus per-format display
  toggles, backed by `BhtSummeryController` session fields. This is the pattern
  the new format follows, rather than inventing a new one.
- **Found, and in scope to fix as part of this work:** a `'Group Bed Charges in
  Bills'` boolean + `'Bed Charges Label'` mechanism already exists in
  `finalBill.xhtml`, `finalBillCustom4.xhtml`, and `intrimBill.xhtml`, intended to
  combine `NursingCharges + MOCharges + LinenCharges + MedicalCareICU +
  AdministrationCharge` into one line. It is **broken**: all three call
  `bhtSummeryController.getBedChargesTotal(...)`, a method that does not exist
  anywhere in `BhtSummeryController`. Since the config defaults to `false` and no
  hospital has it enabled, this has never actually run in production — turning it
  on would throw a JSF `PropertyNotFoundException` and break that print. It is
  fully superseded by the general mechanism below and will be deleted.

## Design

### 1. Config data model — per-charge-type "Final Bill Group"

Add a third per-`InwardChargeType` setting alongside the existing Custom Label /
Report Order / Final Bill Order, all on the same admin page:

- New `ConfigOption` key pattern: `"Inward Charge Type Final Bill Group - <ENUM>"`
  — free text, default empty.
- **Empty** (the default for every charge type, everywhere) → that charge type
  prints on its own line, exactly as it does today. No hospital is affected until
  an admin explicitly fills this in.
- **Non-empty** → every charge type sharing the *exact same* group text is summed
  into one printed row, labeled with that text. E.g. an admin sets this to `Room
  Charges` on `RoomCharges`, `MealCharges`, and whichever specific service
  charge type(s) Coop names — those three now print as a single combined row.
- The combined row's sort position uses the **minimum Final Bill Order** among its
  member charge types (reusing the dormant field from #23340).

**`ConfigOptionApplicationController`** gets two new methods mirroring the
existing Report Order / Final Bill Order pair:
`getInwardChargeTypeFinalBillGroup(InwardChargeType)` /
`saveInwardChargeTypeFinalBillGroup(InwardChargeType, String)`.

**`InwardChargeTypeLabelController`** gets a fourth map (`groupMap`,
`Map<String,String>`, same `p:inputText`-bound pattern as the other three — not
`p:inputNumber`/`Integer`, to avoid repeating the `MapELResolver` bug documented
from #23340) and saves it alongside the other three in `saveAll()` / `saveOne()`.

**`inward_charge_type_labels.xhtml`** gets one more column: "Final Bill Group".

**`GET /api/config/inward-charge-types`** (the discovery endpoint from #23340)
returns the new field per charge type and seeds its `ConfigOption` row the same
way it already seeds the other three, so the generic config PUT/POST endpoints
and the AI chat `manage_config_option` tool work on it immediately.

### 2. Rendering — new print composite, not a change to existing ones

- **New file**: `src/main/webapp/resources/inward/bill/finalBillBundledCustom1.xhtml`.
  Copies the header, patient-info block, payment/paid-by-patient section, and
  signature block verbatim from `finalBill.xhtml`. Only the charge-lines table
  differs.
- **New helper**, `BhtSummeryController.getBundledFinalBillRows(Bill bill)` →
  `List<FinalBillPrintRowDTO>` (new small DTO: `label`, `amount`, `order`).
  Logic:
  1. Walk `bill.billItems`; resolve each item's charge type's Final Bill Group.
  2. Items whose group is non-empty are summed by group text into one row per
     distinct group value; items with an empty group keep their own row (label =
     existing custom/default label for that charge type, one row per item exactly
     as `finalBill.xhtml` does today).
  3. Sort by Final Bill Order (group rows use the min across members).
  4. Skip zero/near-zero rows (same `!= 0` guard as today).
- `finalBillBundledCustom1.xhtml`'s charge table is a single `ui:repeat` over this
  DTO list — replacing the long hand-written chain of per-charge-type `ui:repeat`
  + `rendered="...eq 'X'..."` blocks that `finalBill.xhtml` uses, but **only**
  inside this new file.
- **Trade-off, intentional**: a grouped row prints as a plain `Label : Total` line
  only. It does not carry Room Charges' per-stay date sub-table or a
  Professional/Doctor-fee per-staff breakdown — matching "one entity... combined
  payment is sufficient, view only." A charge type an admin leaves *ungrouped*
  keeps its normal individual rendering, sub-tables included, same as
  `finalBill.xhtml`.
- Nothing stops an admin from grouping `ProfessionalCharge`/`DoctorAndNurses` too,
  but there's no reason to for this request, and it isn't recommended — that's the
  one place per-staff detail matters to the patient.

### 3. Wiring — opt-in via the existing "Custom Bills" tab pattern

Follows the established convention in `inward_reprint_bill_final.xhtml` exactly
(rather than the older, unrelated `applicationInstitution eq 'Ruhuna'`
string-literal branches used elsewhere in `inward_bill_final.xhtml` — this new
format doesn't touch that page's older pattern):

- New gating key: `'Inward Final Bill - Show Bundled Custom 1 Format'`
  (`configOptionController.getBooleanValueByKeyReadOnly(...)`, default `false`),
  resolved per-department/institution the normal way — Coop turns it on for their
  own department(s); no other hospital sees anything different.
- New panel in the "Custom Bills" tab (same Original/Duplicate two-column layout
  as the existing Custom Bill 2/3/4 panels), rendering
  `bi:finalBillBundledCustom1`, gated by the key above.
- New checkbox in the existing "Custom Bills Printer Configuration" settings
  dialog: "Show 'Bundled Custom 1' format", backed by a new
  `BhtSummeryController.showBundledCustom1Format` session field, following the
  exact pattern of `showCustomBill2Format` / `showCustomBill3Format` /
  `showCustomBill4Format`.
- The freshly-created final bill page (`inward_bill_final.xhtml`) is **not**
  touched — Coop staff reach this new format via the reprint page's Custom Bills
  tab, same as Custom Bill 2/3/4 today. (If Coop later wants it on the
  just-created-bill screen too, that's a small follow-up, not blocking.)

### 4. Cleanup — remove the broken dead code

Remove the `'Group Bed Charges in Bills'` / `'Bed Charges Label'` branches and
their `bhtSummeryController.getBedChargesTotal(...)` calls from all three places
they exist:

- `finalBill.xhtml`
- `finalBillCustom4.xhtml`
- `intrimBill.xhtml` (Interim Bill print — no grouping feature is being added
  here, just the dead branch removed; only the Final Bill was asked for)

Each file collapses back to just its always-worked ungrouped rendering path. No
data migration needed — since the method never existed, no hospital could have
had this config successfully enabled, so nothing is lost.

## Out of scope / explicitly unaffected

- **Inward Ledger Report**, `InwardChargeTypeBreakdownController`,
  `InwardInvoiceJournalController`, and `inward_bill_final_break_down.xhtml` —
  none read the new Final Bill Group config. Every hospital's breakdown reporting,
  Coop included, stays exactly as granular as it is today.
- Every existing final bill print composite (`finalBill.xhtml`,
  `finalBillCustom2/3/4.xhtml`, `finalBillGreenSheet.xhtml`, `finalBill_vat.xhtml`,
  `finalBill_Cancel.xhtml`) — untouched, byte-for-byte, aside from the dead-code
  deletion in item 4 above.
- `Bill` / `BillItem` / `InwardChargeType` data model, billing calculation logic,
  privileges, and DB schema — no changes. This is pure `ConfigOption` rows (same
  table already used by #23340) plus one new JSF composite and one new helper
  method.
- Interim Bill grouping — not requested, not built; only its dead code is removed.

## Testing

- Unit-level: `getBundledFinalBillRows` with a mix of grouped/ungrouped charge
  types, verifying sums, ordering by min Final Bill Order, and zero-value
  suppression.
- Playwright: with the new config off (default), confirm `finalBill.xhtml` and
  the Custom Bills tab render identically to current production for an existing
  hospital's test BHT. With it on for a test department, confirm the grouped row
  shows the correct combined total and the ledger/breakdown report for the same
  BHT still shows every charge type separately.
- Confirm removing the dead `'Group Bed Charges in Bills'` branches doesn't
  change output for any hospital (the config defaults to `false`, so the removed
  branch was never the one rendering).

## Open questions for implementation

- Exact list of charge types Coop wants under the "Room Charges" group (which
  specific "service charge" type(s) beyond Room + Meal) — to be gathered from the
  client before or during implementation; the mechanism doesn't need this decided
  up front since it's admin-configurable.
- Exact wording for the new tab panel / button labels ("Bundled Custom 1" vs.
  something more descriptive) — cosmetic, decide at implementation time.
