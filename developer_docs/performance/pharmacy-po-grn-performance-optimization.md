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

## Fix 2 — PO list: N+1 GRN loading (DONE)

**Issue:** hmislk/hmis#19987
**Branch:** `feature/19987-fix-po-list-bulk-grn-load` (stacked on Fix 1)
**Files:**
- `src/main/java/com/divudi/bean/common/SearchController.java`
- `src/main/webapp/pharmacy/pharmacy_purchase_order_list_for_recieve.xhtml`

### Root cause

`createPoTable()` looped over every loaded PO and called `getGrns()` (2 queries
per PO). For 50 POs → **101 total queries** just to load the list.
GRN details were also rendered inline for every row (always-visible nested table).

### Fix applied

- **Backend**: Replaced the per-PO loop with `fillGrnsByBulkQuery()`, which
  runs **2 queries total** for all POs:
  - Path 1: `g.referenceBill IN :pos`
  - Path 2: `g.billedBill.referenceBill IN :pos`
  Results grouped into `Map<Long, List<Bill>>` and assigned in one pass.
- **Frontend**: Converted the always-visible nested GRN table to
  `p:rowExpansion` — GRN details only render when the user expands a row.
  Added a GRN count badge (green = has GRNs, grey = none) in a narrow column
  as the at-a-glance indicator.

### Testing checklist (for QA)

- [ ] Search POs — list loads noticeably faster than before.
- [ ] GRN count badge shows correct count for each PO row.
- [ ] Expand a row — GRN table appears with correct GRN numbers, dates, values.
- [ ] GRN "View" button navigates to finalized GRN page correctly.
- [ ] GRN "Edit" button navigates to saved GRN edit page correctly.
- [ ] Cancelled GRNs shown with correct red styling in expansion.
- [ ] Action buttons (Create GRN, Receive, PO Close/Re-Open) still work correctly.
- [ ] POs with no GRNs show "0" badge and empty expansion message.

---

## Context for a new session

If this conversation is disconnected, resume from here:

1. Fix 1 (GRN entry bulk rate) — branch `feature/19982-fix-grn-entry-bulk-rate-lookup`, PR #19983
2. Fix 2 (PO list bulk GRN) — branch `feature/19987-fix-po-list-bulk-grn-load`, PR TBD (stacked on Fix 1)
3. Both fixes are stacked (Fix 2 based on Fix 1). They should be merged together to `development`.
4. After both pass QA: consider remaining lazy-load optimization on the PO list
   main query (JOIN FETCH for `creater.webUserPerson`, `toInstitution`,
   `fromDepartment`) — lower priority now that the N+1 is resolved.
