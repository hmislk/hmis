# Inward Credit Company Settlement Tracking — Developer Guide

## Overview

This document explains the data model and code flows for tracking inward (inpatient) credit company
settlements. It covers how payments and cancellations are recorded across two settlement UI paths, and
how reports must query this data correctly.

---

## Data Model: Three Tracking Levels

When a patient's final inward bill is raised for a credit company (CC), the system maintains balances
at three levels simultaneously:

| Level | Entity / Bill type | Key fields | Scope |
|---|---|---|---|
| **Per-CC commitment** | `INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY` | `netTotal`, `paidAmount`, `settledAmountBySponsor` | One bill **per company per admission** |
| **Admission aggregate** | Main `INWARD_FINAL_BILL` | `paidAmount`, `settledAmountBySponsor` | Total from **all CCs** for the admission |
| **Encounter** | `PatientEncounter` | `creditPaidAmount` | Same total at encounter level |

### CC Commitment Bills

At finalization (`BhtSummeryController.saveCCBillForAllocation`), one `INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY`
bill is created **per allocation**. A two-company admission produces two commitment bills:

```
Admission bill (INWARD_FINAL_BILL)  netTotal = 100,000
  ├─ CC commitment bill A  (INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY)  netTotal = 70,000  creditCompany = Insurer A
  └─ CC commitment bill B  (INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY)  netTotal = 30,000  creditCompany = Insurer B
```

Each commitment bill's `referenceBill` points back to the main `INWARD_FINAL_BILL`.

---

## Settlement Paths

### Path 1 — CC Commitment Bill Based (recommended)

**UI page**: `credit/credit_compnay_bill_payment_inward.xhtml`  
**Controller method**: `CashRecieveBillController.settleCreditForInwardCreditCompanyPaymentBills()`  
**Data load**: `selectInstitutionListenerInwardCC()` → `CreditBean.getUnpaidInwardCCBills()`

Each bill item has:
- `bill` = `INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED`
- `referenceBill` = **the specific CC commitment bill** ← critical link

What gets updated on **payment**:
- Per-CC commitment bill `paidAmount` ✅ via `updateReferanceBills()` + `updateSettlingCreditBillSettledValues()`
- Per-CC commitment bill `settledAmountBySponsor` ✅ via `updateSettlingCreditBillSettledValues()`
- Main final bill `paidAmount` + `settledAmountBySponsor` ✅ via `updateInwardDipositList()`

What gets updated on **cancellation** (`CreditCompanyBillSearch.cancelCreditCompanyPaymentBill()`):
- Per-CC commitment bill `paidAmount` ✅ via `cancelBillItems()` → `updateReferenceBill()` → `CreditBean.getSettledAmountByCompany()` (DB recalculation)
- Main final bill ✅ via `updateInwardDipositList()`
- Encounter `creditPaidAmount` ✅ via `CreditCompanyBillSearch.updateReferenceBht()`

### Path 2 — BHT (Encounter) Based

**UI page**: `credit/credit_compnay_bill_inward_all.xhtml`  
**Controller method**: `CashRecieveBillController.settleBillBht()`  
**Data load**: `selectInstitutionListenerBht()` → `AdmissionController.getCreditPaymentBillsBht()`

Each bill item has:
- `bill` = `INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED`
- `referenceBill` = **the specific CC commitment bill** ✅ (set via `setReferenceBill(b)` — see fix below)

> **Historical note (pre-fix):** Before issue #19771, `selectInstitutionListenerBht` called
> `setBill(b)` but never `setReferenceBill(b)`, leaving `referenceBill = null`. This meant Path 2
> payments never updated the per-CC commitment bills. The fix added `setReferenceBill(b)` to align
> Path 2 with Path 1.

What gets updated on **payment** (post-fix):
- Per-CC commitment bill `paidAmount` ✅ via `saveBillItemBht()` → `updateReferenceBht()` → `updateSettlingCreditBillSettledValues()`
- Main final bill `paidAmount` + `settledAmountBySponsor` ✅ via `updateInwardDipositList()`

What gets updated on **cancellation** (post-fix):
- Per-CC commitment bill `paidAmount` ✅ via `cancelBillItems()` → `updateReferenceBill()`
- Main final bill ✅ via `updateInwardDipositList()`
- Encounter `creditPaidAmount` ✅ via `CreditCompanyBillSearch.updateReferenceBht()`

---

## The `getSettledAmountByCompany` Calculation

`CreditBean.getSettledAmountByCompany(Bill ccCommitmentBill)` is the authoritative way to calculate
how much a CC has paid for a specific commitment bill. It sums `BillItem.netValue` for all items where:
- `retired = false`
- `referenceBill = ccCommitmentBill`
- `bill.billTypeAtomic IN` all types with `CountedServiceType.CREDIT_SETTLE_BY_COMPANY`

Because cancellation bill types (`INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION`) also have
`CountedServiceType.CREDIT_SETTLE_BY_COMPANY`, and their items have **negative** `netValue`
(via `invertValue`), they naturally subtract from the total. The net result is always accurate
regardless of how many payments or cancellations have occurred.

---

## Debtor Report Pattern (Issue #19771)

### Problem

`InwardReportController1.inwardCreditCompanyDebtors()` showed the "Total Paid" and "Settled by Company"
columns from **stored** `paidAmount`/`settledAmountBySponsor` on the CC commitment bills. These stored
values were:
- Never updated for Path 2 (pre-fix)
- Potentially stale after cancellation due to transaction timing

### Fix

The report now **recalculates per bill** using `BillItemFacade.findDoubleByJpql` with the same
`CREDIT_SETTLE_BY_COMPANY` type list used by `getSettledAmountByCompany`. This is the same pattern
applied to the BHT credit settlement report in issue #19772.

Key points:
1. The `outstandingOnly` SQL filter was moved from JPQL (where it used the stale stored `paidAmount`)
   to a post-calculation Java filter using the freshly computed value.
2. The computed `paidAmount` and `settledAmountBySponsor` are set **in memory** on the detached bill
   entities — they are never persisted back.
3. The approach is robust: includes both received and cancellation items, signed values naturally
   offset, and is independent of stored fields.

```java
// Pattern for per-bill dynamic settlement calculation
List<BillTypeAtomic> settlementTypes =
    BillTypeAtomic.findByCountedServiceType(CountedServiceType.CREDIT_SETTLE_BY_COMPANY);

for (Bill b : allBills) {
    String settledSql = "Select sum(bi.netValue) from BillItem bi "
        + " where bi.retired=false "
        + " and bi.referenceBill=:bill "
        + " and bi.bill.billTypeAtomic in :types";
    Map<String, Object> params = new HashMap<>();
    params.put("bill", b);
    params.put("types", settlementTypes);
    double settled = BillItemFacade.findDoubleByJpql(settledSql, params);
    b.setPaidAmount(settled);
    b.setSettledAmountBySponsor(settled);
    // ... apply outstandingOnly filter, accumulate totals
}
```

---

## Related Files

| File | Role |
|---|---|
| `bean/inward/BhtSummeryController.java` | Creates CC commitment bills at finalization |
| `bean/common/CashRecieveBillController.java` | Processes CC payments (both paths) |
| `bean/common/CreditCompanyBillSearch.java` | Cancellation logic |
| `bean/common/BillBeanController.java` | `updateInwardDipositList()` — updates main final bill |
| `ejb/CreditBean.java` | `getSettledAmountByCompany()` — authoritative settlement calculation |
| `bean/inward/InwardReportController1.java` | Debtor report controller |
| `webapp/credit/credit_compnay_bill_payment_inward.xhtml` | Path 1 settlement UI |
| `webapp/credit/credit_compnay_bill_inward_all.xhtml` | Path 2 settlement UI |
| `webapp/credit/inward_credit_company_debtor_report.xhtml` | Debtor report UI |

---

## Related Issues

- **#19771** — Debtor report not reflecting cancelled CC payments → report fix + Path 2 `referenceBill` fix
- **#19772** — BHT credit settlement report not reflecting cancelled CC payments → signed netValue fix in `BhtPaymentSummaryReportController`
