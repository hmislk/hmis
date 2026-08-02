# Native-SQL "Sale for Cashier" — Design

**Issue:** [#20261](https://github.com/hmislk/hmis/issues/20261)
**Master issue:** [#22442](https://github.com/hmislk/hmis/issues/22442) — Phase 2 of the
pharmacy retail sale native migration.
**Date:** 2026-07-27
**Branch:** `20261-native-sale-for-cashier`

---

## 1. Goal

Replace the entity-based "Sale for Cashier" page with a native-SQL implementation that
avoids the EAGER `Stock → ItemBatch → Item` cascade load, mirroring what
`RetailSaleNativeSqlController` / `RetailSaleNativeSqlService` did for plain "Sale"
(#20260 / PR #20362).

**Requirement: 100% functional replication.** Token system, discounts, multiple payment
methods, patient validation, bill-number strategies and every print format carry over.
Nothing is deferred to a follow-up.

Phase 1 (#22443, PR #22449, merged 2026-07-26) delivered 4 simultaneous windows for the
native retail sale. Phase 4 (#22444) will do the same for this page, gated on production
testing (Phase 3). This spec covers Phase 2 only — the single, unnumbered page.

---

## 2. What "Sale for Cashier" actually does

Established by reading `PharmacySaleForCashierController.settlePreBillAndNavigateToPrint()`
(line 3580) and `savePreBillFinallyForRetailSaleForCashier()` (line 2957):

- Creates **one** bill: a `PreBill` of `BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER`.
  There is no `BilledBill`, and no `Payment` rows — that atomic type is declared
  `BillFinanceType.NO_FINANCE_TRANSACTIONS` / `PaymentCategory.NO_PAYMENT`
  (`BillTypeAtomic.java:129`).
- **Deducts stock at pharmacy time**, before the patient has paid.
- Captures `paymentMethod`, `paymentScheme`, `cashPaid` and the multiple-payment breakdown
  on the bill's **own columns** via `billBean.setPaymentMethodData(bill, pm, pmd)` — as
  information for the cashier, not as `Payment` entities.
- Optional token integration gated on config `Enable token system in sale for cashier`.
- Prints via `/pharmacy/printing/retail_sale_for_cashier.xhtml`.

The cashier then takes payment on a **separate** page — `PharmacyPreSettleController` →
`/pharmacy/printing/settle_retail_sale_for_cashier.xhtml`. That page is **out of scope**;
it must continue to work unchanged against natively-created bills.

---

## 3. Approach

**Chosen: copy the native pair, re-apply the cashier deltas.**

Clone `RetailSaleNativeSqlController` (1,822 lines) and `RetailSaleNativeSqlService`
(1,017 lines), then port cashier-specific behaviour back from
`PharmacySaleForCashierController` (6,282 lines).

Rationale:

- The native pair carries five rounds of production hardening (PRs #20600, #20637,
  #20667, #21698, #21705) — native settle ordering, L2 cache eviction, `PrintBillData`
  plumbing, discount/gross-total persistence, multiple-payment print lines. Starting from
  the legacy controller would discard all of it.
- It matches this codebase's own precedent: `WholesaleSaleNativeSqlController` /
  `...Service` (1,703 / 1,012 lines) were produced exactly this way.

Rejected alternatives:

- **Copy the legacy cashier controller and swap its persistence layer.** Parity would be
  guaranteed, but it retains the entity-based `Stock` autocomplete that causes the
  slowness in the first place, and loses every native-path fix.
- **Extract a shared abstract base first.** Architecturally better — Phase 4 multiplies
  whatever we build ×4 — but refactoring a live, production-proven page to serve a
  different feature is the "unrelated refactoring" CLAUDE.md warns against. Worth revisiting
  as a standalone issue after Phase 4.

The risk in the chosen approach is **omission**. It is mitigated by the inventory diff in §4.

---

## 4. Feature-inventory diff

Method sets extracted from all three controllers and compared. Cashier-family behaviour
with no equivalent in `RetailSaleNativeSqlController`:

| Delta | Detail |
|---|---|
| Token system | `settlePharmacyToken`, `markToken`, `markInprogress`, `currentToken`, `TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER`. Native has zero token code. |
| Qty helpers | `multiplyQuantityByTwo`, `divideQuantityByHalf`, `multiplyAllQuantitiesByTwo`, `divideAllQuantitiesByHalf` — all four bound in the cashier page, absent from the native page. |
| DeptType autocomplete | `completeAvailableStockOptimizedDtoFilteredByDepartmentType`. Native has only the unfiltered `completeAvailableStockOptimizedDto`. |
| Single-bill settle | `savePreBillFinallyForRetailSaleForCashier` + 4 configurable bill-number strategies, bill-number suffix `SCPB`. |
| Navigation | `navigateToPharmacyBillForCashierFromMenu`, `toPharmacyRetailSaleForCashier`, `navigateToSaleBillForCashierPrint`. |
| Item details | `showItemDetailsForSelectedStock`. |

Everything else the cashier controller has is either already present in the native
controller under a different name, or is entity-persistence machinery the native service
replaces.

---

## 5. Components

### 5.1 New files

```
src/main/java/com/divudi/service/pharmacy/RetailSaleForCashierNativeSqlService.java
src/main/java/com/divudi/bean/pharmacy/RetailSaleForCashierNativeSqlController.java
src/main/webapp/pharmacy/pharmacy_bill_retail_sale_for_cashier_native.xhtml
src/main/webapp/resources/pharmacy/       — 6 new *_native composites
src/main/webapp/resources/pharmacy/print/ — 3 new *_native composites
```

### 5.2 Service

`RetailSaleForCashierNativeSqlService.settle(...)` derives from
`RetailSaleNativeSqlService.settle(...)` with the two-bill structure collapsed to one:

```java
public void settle(PreBill preBill, List<BillItemData> items,
                   PaymentMethod paymentMethod, PaymentMethodData paymentMethodData,
                   PaymentScheme paymentScheme)
```

| Step in `RetailSaleNativeSqlService` | Cashier variant |
|---|---|
| 1a persist `PreBill` | kept |
| 1b persist `BilledBill` | **deleted** |
| 1c cross-link the two bills | **deleted** |
| 2a `BillItem` + bare PBI on `PreBill` | kept, but PBI is **fully populated** (see below) |
| 2b `BillItem` + populated PBI on `BilledBill` | **deleted** |
| 3 stock deduct + aggregates + `StockHistory` | kept |
| L2 cache evict | kept, minus `BilledBill.class` |
| 4 `insertFinanceDetails` (BFD + BIFD) | kept, **retargeted to the PreBill** |
| 5 totals update | kept, single bill ID |
| 6 `insertPayment` | **deleted** |

Return type becomes `void` — there are no `Payment` entities to hand back.

**Print payment lines.** `RetailSaleNativeSqlController` builds `PrintBillData.payments`
from the `Payment` rows the service returns. With none created here, the cashier controller
builds those lines directly from the in-memory `PaymentMethodData` components instead —
same DTO shape, same print output, sourced from the cart rather than the database.

**PBI population.** In the two-bill retail flow the PreBill's `PharmaceuticalBillItem`
carries rates only, with `costValue`/`retailValue`/`purchaseValue` zeroed, because the
BilledBill's PBI carries the real values. With no BilledBill, the single PreBill PBI must
be fully populated or costing and F15 lose their source values.

**Finance details (deliberate deviation from legacy).** Legacy writes no BFD/BIFD for
these bills at all — which is precisely why
`DataAdministrationController.backfillBfdForPreToSettleAtCashierBills()` had to be written:
without BFD, F15's `totalRetailSaleValue` reads 0 for every cashier bill
(`DataAdministrationController.java:1356-1364`). Writing them at settle means natively
created cashier bills never need that backfill. Approved as an intentional improvement.

### 5.3 Controller

`RetailSaleForCashierNativeSqlController` — `@Named @SessionScoped`, derived from
`RetailSaleNativeSqlController`.

Unchanged from the native retail controller: `StockDTO` autocomplete, `BillItemData` cart
model, discount handling, `PrintBillData` / `printBillItems` print plumbing, allergy check,
`billSettlingStarted` double-submit guard.

Changed:

- `BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER`, `BillType.PharmacyPre`.
- `saleBill` (`BilledBill`) field removed.
- The six deltas from §4 ported in.

### 5.4 DTO change

`PrintBillData` gains two additive fields — `billIdStr` and `cancelled` — required by the
token composites, which render `#{bill.idStr}` as a Code128 barcode and
`#{bill.cancelled}` as a "**Cancelled**" marker. Fields plus getters/setters only; no
constructor is modified (per CLAUDE.md's constructor rule).

---

## 6. Data flow

### 6.1 Cart building

Unchanged from native retail: `StockDTO` autocomplete → `BillItemData` rows held in the
controller, no `Stock` entities loaded. Adds the deptType-filtered autocomplete variant and
the four qty helpers.

### 6.2 Settle

1. Validation, ported wholesale from legacy `settlePreBillAndNavigateToPrint`:
   session present, cart non-empty, no qty ≤ 0, no duplicate stock ID,
   `discountSchemeValidationService.validateDiscountScheme(paymentMethod, paymentScheme)`,
   departmentType consistency across items, and the patient-field config gauntlet
   (`Patient / Patient Name / Phone / Gender / Address is required in Pharmacy Retail Sale`).
2. `savePatient()` when a patient is required or a valid name was entered.
3. Bill-number generation — all four config strategies, falling back to
   `departmentBillNumberGenerator(..., BillClassType.PreBill, BillNumberSuffix.SALE)`.
4. `billBean.setPaymentMethodData(preBill, paymentMethod, paymentMethodData)`.
5. Balance rule: `Credit` / `Staff` → `balance = netTotal`, `paidAmount = 0`;
   all other methods (including `MultiplePaymentMethods`) → `balance = 0`,
   `paidAmount = netTotal`.
6. `service.settle(...)`.
7. Token block when `Enable token system in sale for cashier` is true:
   `tokenController.findPharmacyTokens` → `findPharmacyTokenSaleForCashier` →
   `settlePharmacyToken(TokenType.PHARMACY_TOKEN_SALE_FOR_CASHIER)` + `markInprogress()`,
   else `markToken()`.
8. Build `PrintBillData` + `printBillItems` from in-memory state. **No entity reload.**
9. `resetAll()`, `billPreview = true`, render the inline print panel.

**Stock locking.** The legacy `stockLockingService.lockAndValidateStocks` / `releaseLocks`
scaffolding is not carried over. The native `deductStock` performs an atomic
`UPDATE ... WHERE qty >= ?` that provides the same guarantee in one statement — this is
what native retail sale already does in production.

---

## 7. Page and print

### 7.1 Page

`pharmacy_bill_retail_sale_for_cashier_native.xhtml`, built from
`pharmacy_bill_retail_sale_native.xhtml`, adding:

- the token header block (`Back to token management`, token panel),
- the four qty-helper buttons,
- the cashier print formats in place of the retail ones,
- the printer-config dialog gated on `hasPrivilege('ChangeReceiptPrintingPaperTypes')`.

Printing is **inline** in the page, following the native pattern, rather than navigating to
`/pharmacy/printing/retail_sale_for_cashier.xhtml`. `navigateToSaleBillForCashierPrint` is
therefore not ported.

### 7.2 Print composites

The legacy print page references 10 composites. One — `phi:saleBill_Header`, under config
key `Pharmacy Retail Sale Bill Paper is POS paper with header` — already has a native
equivalent that the native retail page uses (`saleBill_Header_Inward_native`), so it is
reused. The other **9 are new**, each taking
`bill="com.divudi.core.data.dto.PrintBillData"` + `items="java.util.List"`:

| New composite | Ports | Config key |
|---|---|---|
| `saleBill_five_five_token_native` | `phi:saleBill_five_five_token` | `Pharmacy Retail Sale Token Paper is FiveFivePaper With Blank Space For Printed Heading` |
| `saleBillToken_native` | `phi:saleBillToken` | `Pharmacy Retail Sale Token Paper is POS Paper` |
| `saleBillToken_Custom_1_native` | `phi:saleBillToken_Custom_1` | `Pharmacy Retail Sale Token Paper is POS Paper Custom 1` |
| `saleBill_for_Cashier_native` | `phi:saleBill_for_Cashier` | `Pharmacy Retail Sale Bill Paper is POS Paper` |
| `saleBill_for_Cashier_Pos_paper_Custom_1_native` | `phi:saleBill_for_Cashier_Pos_paper_Custom_1` | `Pharmacy Retail Sale Bill Paper is POS Paper Custom 1` |
| `saleBill_five_five_for_Cashier_native` | `phi:saleBill_five_five_for_Cashier` | `Pharmacy Retail Sale Bill Paper is FiveFive Paper without Blank Space for Header` |
| `retail_sale_for_cashier_custom_1_native` | `pp:retail_sale_for_cashier_custom_1` | `Pharmacy Retail Sale Bill Paper is Custom 1` |
| `retail_sale_for_cashier_custom_2_native` | `pp:retail_sale_for_cashier_custom_2` | `Pharmacy Retail Sale Bill Paper is Custom 2` |
| `retail_sale_for_cashier_custom_3_native` | `pp:retail_sale_for_cashier_custom_3` | `Pharmacy Retail Sale Bill Paper is Custom 3` |

Namespaces: `phi` → `src/main/webapp/resources/pharmacy/`,
`pp` → `src/main/webapp/resources/pharmacy/print/`.

Config keys are reused verbatim so a site's existing paper-format selection keeps working
after the switchover.

### 7.3 Menu

`src/main/webapp/resources/ezcomp/menu.xhtml` — mirrors what "Sale" already does
(line ~1089):

- Line ~1116, "Sale for cashier" → retarget to
  `/pharmacy/pharmacy_bill_retail_sale_for_cashier_native?faces-redirect=true`,
  keeping `rendered="#{webUserController.hasPrivilege('PharmacySaleForCashier')}"`.
- New item "Sale for cashier (Legacy)" →
  `#{pharmacySaleForCashierController.navigateToPharmacyBillForCashierFromMenu()}`,
  rendered when `hasPrivilege('PharmacySaleForCashier')` **and**
  `Legacy Functions Allowed` is true.

---

## 8. Error handling

- `billSettlingStarted` guards double-submit; every early return resets it to `false`.
- Every validation failure raises `JsfUtil.addErrorMessage` and returns without persisting.
- Service exceptions are caught at the controller boundary, logged at `SEVERE`, and
  surfaced as "Settlement failed. Please try again."
- The service participates in the caller's transaction, so a failing native insert rolls the
  whole settle back. There is no half-written bill and no orphaned stock deduction.

---

## 9. Verification

No unit-test harness exists for these controllers; verification is E2E against the local
Payara domain `rh`, matching how Phase 1 was signed off.

- `mvn compile` clean; deploy locally.
- Settle a bill, then assert in MySQL:
  - exactly **one** `Bill` row, `billTypeAtomic = PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER`;
  - **no** `BilledBill` row and **zero** `Payment` rows for it;
  - `BillItem` and `PharmaceuticalBillItem` counts match the cart, no duplicates;
  - `Stock.stock` decremented by exactly the sold qty;
  - `StockHistory` row written per item;
  - **`BillFinanceDetails` and `BillItemFinanceDetails` present** (the §5.2 deviation).
- Multiple-payment breakdown lands on the bill's own columns.
- Token path verified with `Enable token system in sale for cashier` both on and off.
- Discount path (bill-level and item-level) produces correct `total` / `discount` / `netTotal`.
- Bill number matches the configured generation strategy.
- **Downstream cross-check:** the natively created bill settles cleanly through
  `PharmacyPreSettleController` → `settle_retail_sale_for_cashier.xhtml`. The native bill
  must be indistinguishable to the cashier-side page.
- All 10 print formats render with correct values, including the token barcode.

---

## 10. Out of scope

- `PharmacyPreSettleController` and the cashier-side settle page — unchanged, but
  cross-checked (§9).
- `/pharmacy_wholesale/pharmacy_bill_retail_sale_for_cashier.xhtml` — wholesale-for-cashier,
  a separate page sharing the legacy controller.
- Numbered `_1` / `_2` / `_3` copies — Phase 4, issue #22444, gated on Phase 3.
- Retiring the entity-based Family A pages — Phase 5, issue #22445.
- Issue #15845 (legacy cashier windows 2/3/4 phantom beans) — independent, not gated.

---

## 11. References

- `developer_docs/pharmacy/native-sql-bill-migration-guide.md` — native rewrite
  architecture: persist order, sign conventions, cache-coherence pitfalls.
- `developer_docs/pharmacy/PHARMACY_RETAIL_SALE_MULTI_WINDOW_GUIDE.md.md` — corrected in
  Phase 1; relevant to Phase 4, not this spec.
- PR #20362 — native retail sale (#20260), the template for this work.
- PR #22449 — Phase 1, 4 native retail sale windows (#22443).
- `tmp/retail-sale-native-migration-master-plan.md` — untracked progress tracker for the
  whole migration.
