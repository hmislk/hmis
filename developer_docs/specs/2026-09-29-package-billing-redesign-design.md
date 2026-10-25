# Inpatient Package Billing Redesign: Remove Locking, Compare at Final Bill

**Date:** 2026-09-29
**Branch:** to be created from `origin/development` via `start-issue`
**Related issue:** to be filed via `dev-issue` skill
**Related prior work:** #24127 (per-charge-category package pricing config), #24133 (Package Admission menu relocation + Change Package screen, merged)

## Problem

Today, admitting a patient under an `InpatientPackage` immediately locks in room/service/professional-fee/
timed-item billing rows at admission time (`InpatientPackageApplicationBean.applyPackageToAdmission(...)`),
each flagged `fromPackage=true` and defended by ~15 scattered guard checks across the codebase that block
editing, removing, or recalculating them. This has two problems:

1. It bills a *guessed* price at admission time, before the actual stay's real usage is known, and then
   makes correcting that guess (if the stay's real needs differ from the package's assumptions)
   artificially hard — staff have to fight the lock rather than just order what's actually needed.
2. `InpatientPackage.chargeTypeAmounts` (the per-category price map added in #24127) is **not actually
   consumed anywhere** by this locking mechanism — only `fixedRoomCharge` and each component's
   `fixedPrice` are used. The per-category pricing work has had no real effect on billing since it shipped.

This redesign removes the locking entirely. An admission under a package bills **exactly like a normal
admission** for the whole stay — real orders, real prices, no special-cased editing restrictions. The
package only comes into play once, at final-bill time: the bill shows the package's price for what it
covers, any real usage beyond the package's total gets grouped into one new charge type
(`PackageExcessCharges`), and staff can apply the **already-existing** per-charge-type discount if real
usage came in under the package price and they want to pass some of that back to the patient.

**🚨 Non-package admissions must be completely unaffected.** Every change in this spec is scoped so that
an admission with no `InpatientPackage` produces byte-for-byte identical final-bill output before and
after this work — see [Non-package invariant](#non-package-invariant) below.

## Findings that shape this design (verified against current code)

- **"Locking" is a plain `boolean fromPackage` field** (+ a `sourcePackageItem` traceability reference) on
  `BillItem` (`:155`), `BillFee` (`:149`), and `PatientRoom` (`:105`) — there is no DB-level enforcement,
  just ~15 scattered `if (x.isFromPackage())` guards (full list in the implementation plan).
- **The final bill's totals are computed fresh, in memory, every time**, by
  `BhtSummeryController.createChargeItemTotals()` (`:5657`), which builds a `List<ChargeItemTotal>` — one
  DTO per `InwardChargeType`, seeded from `EnumController.getInwardChargeTypesForSetting()` (the same
  method that also populates the Inpatient Package admin screen's "Charge Type Amounts" table) — then
  fills each from several bulk-query sweeps (`setKnownChargeTot()`, `setServiceTotCategoryWise()`, etc.).
  `calFinalValue()` (`:5511`) sums every `ChargeItemTotal.getTotal()` into `grantTotal`.
- **At Settle**, `saveBillItem()` (`:4171`) converts that in-memory `chargeItemTotals` list directly into
  **real, persisted `BillItem` rows — one per `ChargeItemTotal`, keyed only by `InwardChargeType`, with
  `item` left `null`** (this is already how every existing charge-type row on the final bill works, package
  or not — confirmed no existing final-bill charge-type row has a real `Item` behind it).
- **Cancellation walks real `BillItem` rows** to build the reversing contra-bill
  (`InwardSearch.cancelBillItems()`/`copyCancelledBillItem()`) — it does not touch `BillFinanceDetails` or
  any aggregate total. **This means `PackageExcessCharges` must exist as a real, persisted, item-less
  `BillItem` at settle time** (exactly like every other charge-type row already does) so cancellation
  reverses it correctly. It must **not** live only on `BillFinanceDetails` — that entity is a
  write-once discount-breakdown record that `Bill.invertAndAssignValuesFromOtherBill()` never touches, and
  its fields are never summed into `grantTotal` in the first place.
- **A three-level discount mechanism already exists, live and audited**: item-level, charge-type-level
  (`BillItem.chargeTypeDiscount` / `ChargeItemTotal.chargeTypeDiscount`, entered via
  `changeDiscountListener(ChargeItemTotal)`, capped so net can't go negative, logged via
  `auditService.logEncounterAudit(..., "Settlement Discount Changed", ...)`), and bill-level
  (`billLevelDiscount` → `BillFinanceDetails.billDiscount`). **This is reused as-is** for the "adjustment
  when actual is less than package value" requirement — no new field or mechanism.
- **`InwardChargeType.allowToSetItems`** gates exactly two call sites, both via
  `EnumController.getInwardChargeTypesForSetting()`: the final bill's `chargeItemTotals` seeding loop, and
  the Inpatient Package admin screen's "Charge Type Amounts" table. Setting `PackageExcessCharges` to
  `allowToSetItems=false` correctly keeps it out of the package-price-assignment admin screen (you can't
  pre-assign a package price to "the excess over the package price") — **but this also means it won't be
  auto-seeded into `chargeItemTotals`**, so `createChargeItemTotals()` must append it explicitly, the same
  way the existing `CancelledReturnedMedicine` value (also `allowToSetItems=false`) is computed and applied
  outside that loop rather than through it.
- **Zero production admissions** in the `coop`/`ruhunu` databases actually use package locking today — only
  Playwright E2E test fixtures do. This is a from-scratch behavior change, not a live-data migration.

## Design

### 1. Admission-time and during-stay billing — bill normally, no locks

- **`InpatientPackageApplicationBean`**: delete `lockRoomCharge(...)` entirely — the assigned room bills at
  its real facility rate for its real duration, exactly like a non-package admission.
  `InpatientPackage.includedRoomDurationHours` becomes unused for billing (left on the entity/config UI;
  not consumed by any billing code after this change — flagged here as a known no-op rather than deleted,
  to keep this PR's diff focused on billing behavior).
- Component creation (`createLockedServiceBillItem`/`createLockedTimedItem`/`createLockedProfessionalFee`/
  `createLockedOutsideCharge`) is **kept** — components still auto-create a bill row per component at
  admission time — but every one drops `fromPackage=true`. Rename away the "Locked" naming
  (`createServiceBillItemFromComponent`, etc.) since these rows are now ordinary, fully editable/removable
  from the moment they're created. `sourcePackageItem` stays as a plain traceability reference (which
  package config produced this row) — nothing reads it as a guard anymore.
  Pharmacy components keep behaving exactly as today (never pre-created — real pharmacy issues during the
  stay are what create real charges).
- **`PackageChangeController`** needs no change — its "doesn't touch billing" doc comment becomes simply
  true rather than a caveat, since there is no lock state left to disagree with after a package swap.
- **Full removal** of the `fromPackage` field and every guard site (per your decision — nothing in
  production depends on this behavior):
  - `BillItem.fromPackage` (field + accessors + copy-constructor propagation)
  - `BillFee.fromPackage` (field + accessors + copy-constructor propagation)
  - `PatientRoom.fromPackage` (field + accessors)
  - Every guard site found in research: `BhtSummeryController` (room-charge live-recalculation guards,
    `isPackageRoomDurationExceeded`, `getPackageRoomVarianceCharge`, running-timed-service auto-stop skip,
    `syncTimedServiceCharge` short-circuit, discount-matrix suppression, TimedItemFee-block suppression),
    `InwardBeanController` (bulk-query `fromPackage=false` filters), `InwardTimedItemController` (block
    remove/update/retire on package timed items — both the server check and the matching client-side
    `disabled`/`rendered` bindings in `inward_timed_service_consume.xhtml`), `InwardProfessionalBillController`
    (block remove/retire on package fees; `assignStaffToPackageFee`'s special-case), the pharmacy-issue
    controllers' `fromPackage`/`isPackageRate` propagation and rate-override short-circuits
    (`InpatientDirectIssueNativeSqlController`, `PharmacyRequestForBhtController`, `PharmacySaleBhtController`).
  - The exact file:line list goes in the implementation plan (already captured from research); this spec
    fixes the *decision* (delete all of it), not the mechanical enumeration.

### 2. What "the package covers" means, and its per-category display value

A charge type is package-covered if either:
- it has a non-null entry in `InpatientPackage.chargeTypeAmounts`, or
- a component resolves to that `InwardChargeType` (via `component.getItem().getInwardChargeType()` for
  `SERVICE`/`TIMED_ITEM`/`OUTSIDE_CHARGE`/`PHARMACY_ITEM` component types; the implementer must confirm
  during coding what `InwardChargeType` `PROFESSIONAL_FEE_ROLE` components resolve to today, since
  `InpatientPackageItem` has no `item` for that component type — it uses `speciality`/`roleLabel` instead).

The **per-category display value** for a covered type = its `chargeTypeAmounts` entry, plus any
component(s) resolving to that same type not already counted in `chargeTypeAmounts` (summed if more than
one). These per-category values sum to exactly `InpatientPackage.totalPrice` (same total the config screen
already computes via `InpatientPackagePricing.calculateTotalPrice`, just re-expressed per category).

### 3. Final-bill computation (`BhtSummeryController.createChargeItemTotals()`)

Branch **only** when `getPatientEncounter().getInpatientPackage() != null`. All existing per-category sweeps
(`setKnownChargeTot()`, `setServiceTotCategoryWise()`, `setTimedServiceTotCategoryWise()`,
`setChargeValueFromAdditional()`, etc.) **still run unchanged** — they're what produces the real, actual
per-category totals this design needs for the comparison. After they run, for a package admission:

1. `actualCovered` = sum of the *real* `ChargeItemTotal.getTotal()` across every package-covered category
   (computed from the already-run sweeps, before any override).
2. `packageTotal` = `InpatientPackage.totalPrice`.
3. For each package-covered category's `ChargeItemTotal`, **overwrite** `setTotal(...)` with that
   category's per-category package display value (§2) — this is what gets shown on screen and, at Settle,
   persisted as that category's `BillItem.grossValue`. Non-covered categories are left completely alone.
4. If `actualCovered > packageTotal`: append one new `ChargeItemTotal` with
   `InwardChargeType.PackageExcessCharges` and `total = actualCovered - packageTotal` to `chargeItemTotals`
   (bypassing the `getInwardChargeTypesForSetting()` seeding loop, same pattern as the existing
   `CancelledReturnedMedicine` computed value). This flows through the unmodified `calFinalValue()` sum and
   `saveBillItem()` persistence automatically — no changes needed to either of those methods.
5. If `actualCovered <= packageTotal`: no excess row. The package-covered categories show their full
   package-allocated total as usual. Staff may enter a per-charge-type discount on any of those rows via
   the **existing** `changeDiscountListener(ChargeItemTotal)` — already-live, already-audited, already
   capped so net can't go negative — to pass back some or all of the difference between the package price
   and the lower real cost, entirely at their discretion. No new adjustment field or UI.

### 4. New `InwardChargeType` constant

```java
PackageExcessCharges("Package Excess Charges", false),
```
`allowToSetItems=false` — excludes it from the package config "Charge Type Amounts" screen (correct: you
can't pre-assign a price to a computed excess) and from `createChargeItemTotals()`'s automatic seeding loop
(handled by the explicit append in §3.4 instead). No `CalculationMethod` override needed (defaults to
`BILL_ITEM`) since nothing queries a distinct source table for it — it's computed inline, like the
`CancelledReturnedMedicine` precedent.

<a name="non-package-invariant"></a>
### 5. Non-package invariant

Every change above is scoped so it has **zero effect** on an admission with no `InpatientPackage`:

- `createChargeItemTotals()`'s new logic is entirely inside an `if (inpatientPackage != null)` branch —
  the existing sweeps and `chargeItemTotals` construction for a non-package admission are untouched.
- Deleting `fromPackage` and its guards has no behavioral effect on non-package admissions, because no
  non-package `BillItem`/`BillFee`/`PatientRoom` has ever had `fromPackage=true` — the field only existed to
  support package admissions in the first place.
- `PackageExcessCharges` is never appended to `chargeItemTotals` for a non-package admission (§3.4 only
  runs inside the package branch), so it never appears as a row, is never summed into `grantTotal`, and
  never becomes a persisted `BillItem` for one.

**Verification requirement**: before/after this change, settle the *same* non-package test admission (same
BHT, same real charges) and confirm the final bill's `grantTotal`, every `ChargeItemTotal`/`BillItem` row,
and the persisted `Bill` totals are identical.

## Explicitly out of scope

- Any change to interim-bill display during the stay — this redesign only changes what happens at
  **final-bill** time (`createChargeItemTotals()` is shared by both interim and final bill screens per its
  class doc comment; confirm during implementation whether the package substitution should apply to the
  interim bill too, or only once the bill is actually being finalized — **default assumption for this
  spec: apply identically to both**, since showing a mid-stay running total that already reflects "you're
  under/over the package" is more useful to staff than surprising them only at the very end. Flag this
  explicitly to the user if investigation during implementation suggests otherwise.)
- `InpatientPackage.includedRoomDurationHours` — left on the entity/config UI as a no-op field rather than
  removed, to keep this PR scoped to billing behavior.
- Any change to how packages are configured (#24127) or how a package is attached to an admission or
  changed on an existing one (#24133) — both already merged and unaffected by this redesign.

## Files likely touched (confirmed to exist and touched in research; exact list finalized in the plan)

- `src/main/java/com/divudi/core/data/inward/InwardChargeType.java` — new constant
- `src/main/java/com/divudi/ejb/InpatientPackageApplicationBean.java` — strip locking, keep component creation
- `src/main/java/com/divudi/core/entity/BillItem.java`, `BillFee.java`, `src/main/java/com/divudi/core/entity/inward/PatientRoom.java` — delete `fromPackage`
- `src/main/java/com/divudi/bean/inward/BhtSummeryController.java` — remove guards; add package-aware branch in `createChargeItemTotals()`
- `src/main/java/com/divudi/bean/inward/InwardBeanController.java` — remove `fromPackage=false` bulk-query filters
- `src/main/java/com/divudi/bean/inward/InwardTimedItemController.java`, `InwardProfessionalBillController.java` — remove guards
- `src/main/webapp/inward/inward_timed_service_consume.xhtml` — remove matching client-side `disabled`/`rendered` bindings
- `src/main/java/com/divudi/bean/inward/InpatientDirectIssueNativeSqlController.java`, `PharmacyRequestForBhtController.java`, `PharmacySaleBhtController.java` — remove `fromPackage`/`isPackageRate` propagation and rate-override short-circuits

## Verification plan

1. Unit tests for the new package-branch logic in `createChargeItemTotals()` (per-category override,
   excess computation, whole-package netting) — isolate the pure math into a testable helper where possible
   (mirroring the `InpatientPackagePricing` precedent from #24127), rather than testing through the full
   `BhtSummeryController`.
2. `mvn clean package -DskipTests` build check, local redeploy.
3. Playwright: admit a patient under a package (via the existing Package Admission entry point), order real
   services/room/professional fees during the stay whose total is deliberately *more* than the package
   price for at least one covered category, reach the final bill (menu path recorded), confirm each
   covered category shows the package's allocated amount and a `PackageExcessCharges` row appears with the
   correct excess.
4. Repeat with real usage deliberately *less* than the package price — confirm no excess row, and that
   entering a charge-type discount on a package-covered row works exactly as it does for any other bill.
5. Settle both bills, verify in the local DB that `BillItem` rows exist for `PackageExcessCharges` (where
   applicable) with `item IS NULL`, and that `Bill.grantTotal` matches the on-screen total.
6. Cancel one of the settled package bills, verify the contra-bill correctly reverses the
   `PackageExcessCharges` `BillItem` alongside every other row.
7. **Non-package regression**: settle a non-package admission before and after this change (same BHT, same
   charges), confirm identical `grantTotal` and identical `ChargeItemTotal`/`BillItem` rows.
8. Confirm the Inpatient Package admin screen's "Charge Type Amounts" table does **not** show
   `PackageExcessCharges` as a settable row.
