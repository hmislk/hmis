# Three-Level Discounts for Inward Final Bills — Design

**Date:** 2026-07-19
**Status:** Approved design, pending implementation plan
**Pages affected:** `/inward/inward_bill_final.xhtml`, `/inward/inward_bill_intrim.xhtml`
**Main controller:** `BhtSummeryController`

## Problem

Inward final bills show Gross Total, Discount, Margin (Service Charge), Net Total, VAT, and VAT + Net Total, with a per-`InwardChargeType` breakdown in the Charges panel. The business now requires discounts at **three levels, each recorded separately**:

1. **Item/fee level** (exists) — auto-computed from the membership scheme / discount matrix onto `BillFee`, `PatientRoom` charge components, and `PatientItem`s.
2. **Inward charge type level** (new) — a manual additional discount per charge type.
3. **Final bill level** (new) — a manual additional discount on the whole bill.

### Current defects the design removes

- The editable per-charge-type "Discount" column on `inward_bill_final.xhtml` does not record a charge-type discount; `changeDiscountListener()` converts the amount to a percentage and pushes it down onto item-level fees, mutating level 1. It is also disabled for all room-related types and professional charges.
- The bill-level "Discount Amount" input binds to the same single `discount` field that `calFinalValue()` recomputes as the sum of charge-type discounts, so recalculation clobbers manual entry, and `Bill.discount` stores one merged number with no recoverable breakdown.

## Decisions (agreed with product owner)

| Decision | Choice |
|---|---|
| Layering | Three independent additive layers; push-down behavior of the existing column is **replaced** |
| Entry point | Final bill page only, during the settle workflow; interim page is display-only |
| Coverage | New charge-type discount is editable for **all** charge types, including rooms and professional charges |
| Privileges | No new privilege; same access as today's discount fields |
| VAT | Unchanged — VAT stays computed on pre-discount values |
| Level 3 storage | `BillFinanceDetails.billDiscount` (reuse the standardized field; no new `Bill` column) |
| Level 2 storage | New `BillItem.chargeTypeDiscount` column (BIFD has no slot for a third discount component; no `BillItemFinanceDetails` rows are created for inward lines) |

## Data model

New columns are added via DDL generation (plain columns, no backfill → no migration script).

- **`BillItem.chargeTypeDiscount`** (`double`, new) — the manual level-2 discount for the charge-type line on temp/original/provisional/final bills. `BillItem.discount` continues to hold the **combined** discount for that type (level 1 aggregate + level 2), so existing prints/reports are unchanged. Level 1 aggregate per type is derivable as `discount − chargeTypeDiscount`.
- **`BillFinanceDetails.billDiscount`** (exists) — the manual level-3 discount. At settle we also populate `lineDiscount` (Σ of per-type combined discounts) and `totalDiscount` (all three levels) so the standardized financial layer is self-consistent for inward final bills. `Bill.getBillFinanceDetails()` lazily creates the row; cancellation inversion (`invertValuesOf`) and bill-copy cloning already handle it.
- **`Bill.discount`** (exists, unchanged meaning) — remains the grand total: Σ item discounts + Σ charge-type discounts + bill-level discount. All existing reports keep working.
- **`ChargeItemTotal`** (DTO, not an entity) — gains `chargeTypeDiscount`; `discount` stays the level-1 aggregate; `netTotal` becomes `total − discount − chargeTypeDiscount`.

## Controller changes (`BhtSummeryController`)

- New field `billLevelDiscount` (double), bound to the Discount Details input on the final bill page.
- `calFinalValue()`: `discount = Σ cit.discount + Σ cit.chargeTypeDiscount + billLevelDiscount`. Manual values live in their own fields, so recalculation no longer clobbers them.
- `changeDiscountListener(ChargeItemTotal)`: push-down logic removed. It validates the entered amount (0 ≤ value; per-type combined discount must not exceed the type's total) and recalculates totals. The XHTML `disabled` conditions for room/professional types are removed.
- Item-level discounts remain purely auto-computed (`calculateDiscount()`), plus the existing per-room manual edit listeners which are untouched.
- `saveBillItem` / `saveOriginalBillItem` / `saveTempBillItem`: set `chargeTypeDiscount` on each per-type `BillItem`; `discount` = combined; `netValue` = `total − combined`.
- `saveBill` / `saveOriginalBill` / `saveTempBill`: write `billFinanceDetails.billDiscount / lineDiscount / totalDiscount`; `Bill.discount` = grand total as today.
- `errorCheck()`: add validation that the grand total discount does not exceed `grantTotal`.
- `settleProvisionalBill` path needs no extra work — it reuses the same persisted bill/bill-item records.

## UI changes

**`inward_bill_final.xhtml`**
- Charge Types table: current editable "Discount" column becomes read-only **"Item Discounts"**; new editable **"Type Discount"** column (all charge types enabled); "NetTotal" reflects both.
- Discount Details panel: input rebinds to `billLevelDiscount`.
- Charges Overview panel: shows four discount lines — Item Discounts, Charge Type Discounts, Bill Level Discount, Total Discount.

**`inward_bill_intrim.xhtml`** (display-only)
- Relabel the current misleading "Bill Level Discount" summary line to "Item Discounts".
- Add Charge Type Discounts and Bill Level Discount lines (zero until entered at settle).

## Out of scope

- Printed breakdown of the three levels on final-bill prints (follow-up if requested).
- VAT recomputation on discounted values.
- New privileges or approval workflow for discounts.
- Persisting level-2/3 entries before the settle session (no pre-settle storage; the interim page does not accept discount entry).

## Edge cases

- **Recalculate / Process buttons**: preserve manual `chargeTypeDiscount` and `billLevelDiscount` values across `createTables()` / `updateTotal()` within the session.
- **Validation**: per-type combined discount ≤ type total; grand total discount ≤ grand total; negative values rejected.
- **Cancellation**: final-bill cancellation copies/inverts `BillFinanceDetails` via existing clone/invert paths; the new `BillItem` column must be included in the bill-item copy used by the cancellation flow.
- **Credit patients**: net total after all three discount levels feeds the existing credit-company allocation logic unchanged (it already works from `netTotal`).

## Testing

- Unit-level: totals math in `calFinalValue()` with combinations of the three levels.
- E2E (Playwright, local deployment): admit → add charges → process final bill → enter type-level and bill-level discounts → settle → verify `Bill.discount`, `BillItem.discount` / `chargeTypeDiscount`, and `BillFinanceDetails` rows in MySQL; re-open final bill page and confirm values display; cancel the final bill and verify inverted finance details.
