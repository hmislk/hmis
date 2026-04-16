# Pharmacy PO / GRN Performance Optimization

Tracking document for the multi-issue performance optimization of the pharmacy
Purchase Order list page and GRN costing entry page.

---

## Background

Two pages were identified as slow in April 2026:

| Page | URL |
|------|-----|
| PO list (for receiving) | `pharmacy/pharmacy_purchase_order_list_for_recieve.xhtml` |
| GRN costing (entry) | `pharmacy/pharmacy_grn_costing_with_save_approve.xhtml` |

Investigation revealed two distinct root causes — each fixed separately.

---

## Fix 1 — GRN entry: bulk fallback rate queries (DONE)

**Issue:** hmislk/hmis#19982
**Branch:** `feature/19982-fix-grn-entry-bulk-rate-lookup`
**File:** `src/main/java/com/divudi/bean/pharmacy/GrnCostingController.java`

### Root cause

`generateBillComponent()` called two per-item methods inside its loop:

```java
getPharmacyBean().getLastPurchaseRate(item, dept)
getPharmacyBean().getLastRetailRateByBillItemFinanceDetails(item, dept)
```

Each method executes one `BillItem` query. For a 100-item PO with missing rates
this produces **up to 200 extra DB queries** just to open the GRN page.

### Fix applied

- Added `buildLastRatesMap(List<Item> items)` — one query fetching
  `bi.item.id`, `bi.billItemFinanceDetails.lineGrossRate`,
  `bi.billItemFinanceDetails.retailSaleRate` for all items at once,
  ordered `bi.id DESC`. First occurrence per item is kept (= latest).
- `generateBillComponent()` now stores `getPharmaceuticalBillItems()` in a
  local list, pre-collects all items, calls `buildLastRatesMap` once, and uses
  the resulting map inside the loop.
- N×2 per-item queries → **1 bulk query** regardless of PO size.

### Testing checklist (for QA)

- [ ] Open a PO with ≥20 items and click "Create GRN" — page should load noticeably faster.
- [ ] Rates pre-filled correctly from the last GRN for each item.
- [ ] Items with rates on the PO (non-zero) are unaffected — map lookup is only
      used when `pr == 0.0` or `rr == 0.0`.
- [ ] Ampp (pack) items: pack rate conversion still works correctly.
- [ ] New PO items (never received before) show 0 rates as before.
- [ ] Save / Finalize / Approve flow works end-to-end.

---

## Fix 2 — PO list: N+1 GRN loading (PENDING)

**Issue:** TBD
**Status:** Not yet started — waiting for Fix 1 QA to pass.

### Root cause

`createPoTable()` (`SearchController.java:7157`) loops over every loaded PO and
calls `getGrns(b, referenceBillTypes)`, which runs **2 queries per PO**
(one for `referenceBill = :ref`, one for `billedBill.referenceBill = :ref`).
For 50 POs this is 101 total queries. Additionally, the PO main query loads full
`Bill` entities, triggering lazy loads on `creater.webUserPerson.name`,
`toInstitution.name`, `fromDepartment.name` per row.

### Planned fix

1. Replace per-PO `getGrns()` loop with one bulk query that loads all GRNs for
   all matching POs at once, groups them by PO ID in a `Map<Long, List<Bill>>`,
   then assigns in one pass.
2. Add `JOIN FETCH` clauses to the main PO query for the common lazy paths.
3. (Optional) Convert the always-visible nested GRN sub-table to
   `p:rowExpansion` so GRN detail is loaded on demand.  Show a GRN count badge
   in the main row as the indicator.

**Key files:**
- `src/main/java/com/divudi/bean/common/SearchController.java` — `createPoTable()` (line ~7103) and `getGrns()` (line ~7177)
- `src/main/webapp/pharmacy/pharmacy_purchase_order_list_for_recieve.xhtml`

---

## Context for a new session

If this conversation is disconnected, resume from here:

1. Fix 1 is merged / in QA.
2. Start Fix 2: create a new issue, branch from `origin/development`, bulk the
   GRN load in `SearchController.createPoTable()`, optionally add row expansion
   to the XHTML.
3. After Fix 2: consider a `PurchaseOrderSummaryDto` with a JOIN FETCH
   constructor query to eliminate remaining lazy loads on the PO list (lower
   priority once the N+1 is gone).
