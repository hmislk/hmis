# Inward Service Bill — Partial Return (Issue #21247)

Return selected inward service items from a bill **without cancelling the whole
bill**, in one or more passes, with the same payment-state verification the OPD
refund applies.

## User flow

1. **Inpatient Profile** (`inward/admission_profile.xhtml`) → **Inward Service Bills**
   button → encounter-scoped list page.
2. **List page** (`inward/reports/inpatient_service_bill_list_dto.xhtml`) — lists
   the admission's service bills (status: Cancelled / Returned). **View** opens the
   reprint page.
3. **Reprint page** (`inward/inward_reprint_bill_service.xhtml`) → **Return** button
   → calls `inwardServiceRefundController.navigateToRefundInwardServiceBill(inwardSearch.bill)`.
4. **Refund page** (`inward/inward_bill_service_refund.xhtml`) — tick the items to
   return (multi-select datatable), enter a comment, **Refund Bill**. Prints the
   refund bill on the configured paper type.

## Key files

| Concern | File |
|---|---|
| Refund controller (new) | `bean/inward/InwardServiceRefundController.java` |
| Refund page | `webapp/inward/inward_bill_service_refund.xhtml` |
| Reprint page (Return button) | `webapp/inward/inward_reprint_bill_service.xhtml` |
| Encounter list page | `webapp/inward/reports/inpatient_service_bill_list_dto.xhtml` |
| List query / nav | `bean/inward/InwardReportControllerBht.java` (`fetchServiceBillDtos`, `navigateToInpatientServiceBillListDto`, `navigateToReprintServiceBill`) |
| List DTO | `core/data/dto/BillListReportDTO.java` |

## Why a dedicated controller (not `BillSearch`)

Inpatient refunds differ from OPD / Collecting-Centre refunds: no drawer /
cash-in-hand check, no lab-sampling guard, credit-spend against the BHT encounter
rather than a cash collection. `BillSearch` is already overloaded with
`refundOpdBill` / `refundCollectingCenterBill` and a graveyard of commented-out
`refundBill` / `returnBill`. The old refund page bound to `billSearch.refundBill()`,
which had been **renamed to `refundCollectingCenterBill()`** long ago — so the page
threw `MethodNotFoundException` and the return never worked. Fix = new
`@Named @ViewScoped InwardServiceRefundController`.

## Design rules (do not regress)

- **Scope / state.** Controller is `@ViewScoped`. The **bill lives in
  session-scoped `InwardSearch`** (`getBill()` delegates to `inwardSearch.getBill()`)
  so it survives the `faces-redirect` from reprint → refund page. The view-scoped
  controller holds only refund-workflow state (`refundingItems`, `comment`,
  `printPreview`). Do **not** store the bill as a field on this view-scoped bean —
  the redirect would discard it.

- **Partial totals.** Refund bill totals are computed from the **selected items
  only** (`sumGross` / `sumDiscount` / sum of `netValue`), then negated — NOT by
  `invertAndAssignValuesFromOtherBill(wholeBill)`. That is what makes partial
  return correct.

- **Mark items refunded.** Each original `BillItem` → `setRefunded(true)` +
  `setBillItemRefunded(true)`. Each inverted refund `BillFee` →
  `setReferenceBillFee(originalFee)`. There is **no boolean `refunded` field on
  `BillFee`** — the reference link is the marker. `BillController.hasRefunded(bf)`
  detects a non-retired refund fee referencing the original; this is how a second
  pass knows a fee is already refunded.

- **Verification before refund** (mirrors OPD `refundOpdBill`), per selected
  item's original fees:
  - `billController.hasRefunded(bf)` → block already-refunded fees.
  - `billController.hasPaidToStaff(bf)` (= `bf.getPaidValue() > 0`) → block fees
    already **paid to the service provider** (staff professional payment done).
  - **"Unless cancelled" is automatic:** cancelling an inward professional payment
    runs `InwardSearch.cancelPaymentItems()`, which resets the original
    `BillFee.paidValue` to `0`. So after the payment is cancelled,
    `hasPaidToStaff` returns false and the refund is allowed again. No extra
    cancellation-status check is needed.

- **Cancellation guard.** `bill.setRefunded(true)` on **every** pass (even
  partial). This is the guard that prevents the bill from being **cancelled** once
  any item has been returned (the "To Cancel" button is disabled on
  `bill.cancelled or bill.refunded`).

- **Multiple passes.** The **Return** button is gated on
  `inwardServiceRefundController.fullyReturned` (`isFullyReturned()` = no original,
  non-refund item with `refunded = false` remains), **not** on `bill.refunded`.
  So successive partial returns are allowed until everything is returned;
  already-returned rows are non-selectable (`disabledSelection="#{b.refunded}"`).

- **Refund bill type / numbering.** `RefundBill`, `BillType.InwardBill`,
  `BillTypeAtomic.INWARD_SERVICE_BILL_REFUND`, `ipOpOrCc = "IP"`. Bill number via
  `billNumberBean.departmentBillNumberGeneratorYearly(dept, INWARD_SERVICE_BILL_REFUND)`
  (single yearly number for both `deptId` and `insId`).

- **Print preview** reads `inwardServiceRefundController.bill.refundedBill` (the
  newly created refund bill), after reloading the original into `InwardSearch`.

## List page — separate bug fixed (#21247 "not listing")

`fetchServiceBillDtos` originally wrapped every projected field in
`COALESCE(..., false/0.0)`. `Bill.retired/cancelled/refunded` are primitive
`boolean` and `total/discount/netTotal/margin` are primitive `double` — never null,
so `COALESCE` over them makes EclipseLink return a mismatched type (e.g. Integer
for a boolean), breaking the reflective DTO-constructor binding. The exception was
**silently swallowed** by `findLightsByJpqlWithoutCache` → empty list. Fix: project
those primitive fields **directly**, keep `COALESCE` only on nullable String /
relationship fields. (The working sibling `fetchServiceIssueDtos` projects
`bi.bill.cancelled` directly — proof of the correct pattern.) The mis-targeted
"Service Department" filter (`b.department` = billing dept, not service dept) was
removed.

## Bill-type atomics involved

`INWARD_SERVICE_BILL`, `INWARD_SERVICE_BILL_CANCELLATION`,
`INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION`,
`INWARD_OUTSIDE_CHARGES_BILL`, `INWARD_OUTSIDE_CHARGES_BILL_CANCELLATION`,
`INWARD_SERVICE_BILL_REFUND`. The individual `INWARD_SERVICE_BILL` carries the
`patientEncounter` (`BillBhtController.saveBill`); the `INWARD_SERVICE_BATCH_BILL`
is the parent batch.
