# Native SQL Bill Migration Guide

**Scope**: Converting a pharmacy bill type from the JPA-based old flow to a `NativeSqlService` settlement path  
**Reference implementation**: `RetailSaleNativeSqlService` + `RetailSaleNativeSqlController` (Issue #20260)  
**Last updated**: 2026-05-11

---

## Why Native SQL for Settlement?

The old JPA flow (e.g., `PharmacySaleController`) suffers from:
- EAGER cascade on `Stock → ItemBatch → Item` loaded per-item on every settle
- Multiple JPQL aggregate queries per item for StockHistory values
- Entity graph inflate/flush overhead under concurrent load

The native service replaces only the **hot-path INSERT/UPDATE** operations with direct SQL while keeping JPA for entities that need IDENTITY PKs (`BillItemFinanceDetails`, `BillFinanceDetails`, `Payment`).

---

## Entities Written Per Transaction

The following entities are created or updated during a single pharmacy retail sale settlement. Every native migration must account for all of them:

| Entity | Table | Written by | Notes |
|---|---|---|---|
| PreBill | `bill` (DTYPE=PreBill) | JPA persist | BillType=PharmacyPre, BillTypeAtomic=SALE_PRE |
| BilledBill | `bill` (DTYPE=BilledBill) | JPA persist | BillType=PharmacySale, BillTypeAtomic=RETAIL_SALE |
| PreBill→BilledBill link | `bill.referenceBill_ID` | JPA merge on PreBill | Must use JPA, not native SQL, to keep L2 cache coherent |
| BilledBill→PreBill link | `bill.referenceBill_ID` | JPA (set before persist) | `saleBill.setReferenceBill(preBill)` before persist |
| BillItem (PreBill copy) | `billitem` | Native INSERT | Bare PBI — rates only, costValue=0 |
| PharmaceuticalBillItem (PreBill) | `pharmaceuticalbillitem` | Native INSERT | rates set, costValue/retailValue/purchaseValue = 0 |
| BillItem (BilledBill copy) | `billitem` | Native INSERT | Full values; BIFD linked here |
| PharmaceuticalBillItem (BilledBill) | `pharmaceuticalbillitem` | Native INSERT + UPDATE | Fully populated after finance calc |
| Stock deduction | `stock.stock` | Native UPDATE | Atomic decrement |
| StockHistory | `stockhistory` | Native INSERT | `PBITEM_ID` → PreBill PBI ID |
| BillItemFinanceDetails | `billitemfinancedetails` | JPA persist | On BilledBill items; IDENTITY PK |
| BillFinanceDetails | `billfinancedetails` | JPA persist | On BilledBill; IDENTITY PK |
| Payment | `payment` | JPA persist | On BilledBill |
| Drawer update | via `DrawerController.updateDrawerForIns()` | Controller (post-settle) | Called after service returns Payment |

---

## The Two-Bill Structure

This is the most critical architectural requirement. **Every stock-out bill type must use a PreBill + BilledBill pair** — the old flow has this, and all downstream consumers depend on it.

```
PreBill (DTYPE=PreBill)
├── BillType:      PharmacyPre
├── BillTypeAtomic: PHARMACY_RETAIL_SALE_PRE
├── referenceBill: → BilledBill   (cross-link)
├── billedBill:    NULL           (used by older cashier flows; not set here)
├── BillItems:     2 items        (bare PBI: rates only)
├── BILLFINANCEDETAILS_ID: NULL
└── PBI.costValue/retailValue/purchaseValue = 0

BilledBill (DTYPE=BilledBill)
├── BillType:      PharmacySale
├── BillTypeAtomic: PHARMACY_RETAIL_SALE
├── referenceBill: → PreBill      (cross-link)
├── BILLFINANCEDETAILS_ID: → BillFinanceDetails
├── BillItems:     2 items        (populated PBI + BIFD linked)
├── Payment:       1 record
└── PBI.costValue/retailValue/purchaseValue = computed
```

**Why two copies of BillItems?**
- PreBill items → used by `SaleReturnController.generateBillComponent()` (return flow reads PBI from PreBill)
- BilledBill items → used by `createTableByBillType()` search, income reports (query BilledBill BillItems for BIFD)
- StockHistory → `PBITEM_ID` points to PreBill PBI (matches old flow)

---

## Persist Order (Critical)

```
1. persist(preBill)  → flush  → get preBillId
2. saleBill.setReferenceBill(preBill)
   persist(saleBill) → flush  → get billId
3. preBill.setReferenceBill(saleBill)
   merge(preBill)    → flush      ← JPA, NOT native SQL (L2 cache!)
4. INSERT BillItem + PBI on PreBill  (bare PBI)
5. INSERT BillItem + PBI on BilledBill (rates only; values updated in step 7)
6. deductStock + insertStockHistory  (PBITEM_ID = PreBill PBI)
7. insertFinanceDetails(billId, biIds_BilledBill, pbIds_BilledBill, items)
   → persists BIFD on BilledBill items
   → persists BFD on BilledBill
   → native UPDATE pharmaceuticalbillitem SET costValue/retailValue/purchaseValue
8. native UPDATE bill SET total/netTotal WHERE ID=preBillId OR ID=billId
9. persist(Payment) linked to BilledBill
   → flush
10. Controller calls drawerController.updateDrawerForIns(payment)
```

**Why JPA merge for step 3?**  
Native SQL `UPDATE bill SET referenceBill_ID=? WHERE ID=?` bypasses EclipseLink's L2 cache. The PreBill entity in cache remains with `referenceBill=null`. Subsequent JPQL queries that load the PreBill from cache will see `referenceBill=null`, causing "NOT PAID" display and "Billed At" empty. Using `preBill.setReferenceBill(saleBill); em.merge(preBill)` keeps cache coherent.

---

## Sign Conventions

These must match the old JPA flow exactly. Wrong signs cause incorrect income reports, costing reports, and inventory valuations.

### BillItemFinanceDetails (BIFD)

| Field | Sign | Rule |
|---|---|---|
| `lineNetRate` | Positive | Rate is always unsigned |
| `grossRate` / `lineGrossRate` | Positive | Rate is always unsigned |
| `costRate` / `lineCostRate` / `totalCostRate` | Positive | Rate is always unsigned |
| `purchaseRate` / `retailSaleRate` / `wholesaleRate` | Positive | Rate is always unsigned |
| `lineGrossTotal` / `grossTotal` | Positive | Gross revenue per item |
| `lineNetTotal` / `netTotal` | Positive | Net revenue per item |
| `lineCost` / `totalCost` | Positive | Cost burden (not negated in retail sale) |
| `valueAtCostRate` | **Negative** | `costRate × totalQty × -1` — inventory value reduced |
| `valueAtPurchaseRate` | **Negative** | `purchaseRate × totalQty × -1` |
| `valueAtRetailRate` | **Negative** | `retailRate × totalQty × -1` |
| `valueAtWholesaleRate` | **Negative** | `wholesaleRate × totalQty × -1` |
| `quantity` / `quantityByUnits` | **Negative** | Stock goes out |
| `totalQuantity` | **Negative** | Stock goes out |
| `freeQuantity` | **Negative** | Free stock also goes out |

### BillFinanceDetails (BFD)

| Field | Sign | Rule |
|---|---|---|
| `netTotal` / `grossTotal` | Positive | Revenue |
| `totalCostValue` | **Negative** | `.negate()` on accumulated cost sum |
| `totalPurchaseValue` | **Negative** | `.negate()` |
| `totalRetailSaleValue` | **Negative** | `.negate()` |
| `totalWholesaleValue` | **Negative** | `.negate()` |
| `totalQuantity` | **Negative** | `.negate()` |
| `totalFreeQuantity` | **Negative** | `.negate()` |

Reference: `PharmacySaleController.updateRetailSaleFinanceDetails()` and `PharmacySaleController.updateBillFinanceDetailsForRetailSale()` are the ground-truth implementations to compare against.

---

## Database Verification Checklist

After deploying a new native service, create two test bills — one with the old flow, one with the new native flow — using the same items and quantities. Then run these queries and compare every column.

### Step 1 — Locate both bill pairs

```sql
SELECT ID, insId, DTYPE, billType, billTypeAtomic,
       netTotal, total, referenceBill_ID, billedBill_ID,
       BILLFINANCEDETAILS_ID, paymentMethod, paidAmount, cashPaid,
       institution_ID, department_ID, createdAt, cancelled, retired
FROM bill
WHERE insId IN ('<old_bill_no>', '<new_bill_no>')
   OR deptId IN ('<old_bill_no>', '<new_bill_no>')
ORDER BY ID;
```

**Expected**: Two rows per bill (PreBill + BilledBill). Check every column against the reference.

| Column | PreBill | BilledBill |
|---|---|---|
| DTYPE | PreBill | BilledBill |
| billType | PharmacyPre | PharmacySale |
| billTypeAtomic | PHARMACY_RETAIL_SALE_PRE | PHARMACY_RETAIL_SALE |
| referenceBill_ID | → BilledBill ID | → PreBill ID |
| BILLFINANCEDETAILS_ID | NULL | set |
| netTotal / total | match | match |

### Step 2 — BillItems

```sql
SELECT bi.ID, bi.bill_ID, b.DTYPE, bi.qty, bi.netValue, bi.grossValue,
       bi.BILLITEMFINANCEDETAILS_ID, bi.retired
FROM billitem bi
JOIN bill b ON b.ID = bi.bill_ID
WHERE bi.bill_ID IN (<preBillId>, <billedBillId>)
  AND bi.retired = 0
ORDER BY bi.bill_ID, bi.ID;
```

**Expected**:
- PreBill items: `BILLITEMFINANCEDETAILS_ID = NULL`
- BilledBill items: `BILLITEMFINANCEDETAILS_ID` set
- Both have the same `qty`, `netValue`, `grossValue`

### Step 3 — PharmaceuticalBillItem

```sql
SELECT pbi.ID, pbi.billItem_ID, bi.bill_ID, b.DTYPE,
       pbi.qty, pbi.costRate, pbi.costValue, pbi.retailValue, pbi.purchaseValue,
       pbi.retailRate, pbi.purchaseRate, pbi.wholesaleRate
FROM pharmaceuticalbillitem pbi
JOIN billitem bi ON bi.ID = pbi.billItem_ID
JOIN bill b ON b.ID = bi.bill_ID
WHERE bi.bill_ID IN (<preBillId>, <billedBillId>)
  AND bi.retired = 0
ORDER BY bi.bill_ID, pbi.ID;
```

**Expected**:
- PreBill PBI: `costValue=0, retailValue=0, purchaseValue=0`; rates set
- BilledBill PBI: all values computed and populated; rates set

### Step 4 — BillItemFinanceDetails

```sql
SELECT bifd.ID,
       bifd.lineNetRate, bifd.lineGrossTotal, bifd.lineNetTotal,
       bifd.lineCost, bifd.totalCost,
       bifd.valueAtCostRate, bifd.valueAtPurchaseRate, bifd.valueAtRetailRate, bifd.valueAtWholesaleRate,
       bifd.quantity, bifd.quantityByUnits, bifd.totalQuantity, bifd.freeQuantity,
       bifd.costRate, bifd.lineCostRate, bifd.totalCostRate,
       bifd.purchaseRate, bifd.retailSaleRate, bifd.wholesaleRate
FROM billitemfinancedetails bifd
WHERE bifd.ID IN (
    SELECT bi.BILLITEMFINANCEDETAILS_ID FROM billitem bi
    WHERE bi.bill_ID IN (<billedBillId_old>, <billedBillId_new>)
      AND bi.BILLITEMFINANCEDETAILS_ID IS NOT NULL
);
```

**Expected signs**: `valueAtCostRate`, `valueAtPurchaseRate`, `valueAtRetailRate`, `valueAtWholesaleRate`, `quantity`, `quantityByUnits`, `totalQuantity`, `freeQuantity` are all **negative**. Rates and `lineCost`/`totalCost` are positive.

### Step 5 — BillFinanceDetails

```sql
SELECT bfd.ID, bfd.NETTOTAL, bfd.GROSSTOTAL,
       bfd.TOTALCOSTVALUE, bfd.TOTALPURCHASEVALUE,
       bfd.TOTALRETAILSALEVALUE, bfd.TOTALWHOLESALEVALUE,
       bfd.TOTALQUANTITY, bfd.TOTALFREEQUANTITY
FROM billfinancedetails bfd
WHERE bfd.ID IN (
    SELECT b.BILLFINANCEDETAILS_ID FROM bill b
    WHERE b.ID IN (<billedBillId_old>, <billedBillId_new>)
      AND b.BILLFINANCEDETAILS_ID IS NOT NULL
);
```

**Expected**: `TOTALCOSTVALUE`, `TOTALPURCHASEVALUE`, `TOTALRETAILSALEVALUE`, `TOTALWHOLESALEVALUE`, `TOTALQUANTITY`, `TOTALFREEQUANTITY` are all **negative**. `NETTOTAL` and `GROSSTOTAL` are **positive**.

### Step 6 — StockHistory

```sql
SELECT sh.ID, sh.PBITEM_ID, sh.HISTORYTYPE, sh.STOCKQTY,
       sh.PURCHASERATE, sh.RETAILRATE, sh.COSTRATE
FROM stockhistory sh
WHERE sh.PBITEM_ID IN (
    SELECT pbi.ID FROM pharmaceuticalbillitem pbi
    JOIN billitem bi ON bi.ID = pbi.billItem_ID
    WHERE bi.bill_ID = <preBillId_new>
);
```

**Expected**: `PBITEM_ID` points to PreBill PBI IDs. `STOCKQTY` reflects post-deduction level. Rates match batch rates.

### Step 7 — Payment

```sql
SELECT p.ID, p.BILL_ID, b.DTYPE, p.PAYMENTMETHOD, p.PAIDVALUE,
       p.CREDITCARDREFNO, p.CHEQUEREFNO, p.COMMENTS
FROM payment p
JOIN bill b ON b.ID = p.BILL_ID
WHERE p.BILL_ID IN (<preBillId_new>, <billedBillId_new>);
```

**Expected**: Payment is on the **BilledBill** only. `PAIDVALUE` matches `bill.netTotal`.

### Step 8 — Drawer (manual check in UI)

The drawer is updated by `DrawerController.updateDrawerForIns(payment)` in the controller, after the service returns the `Payment`. This is not in the native service itself. Verify via the cashier drawer UI that the cash entry appears with the correct amount and timestamp.

---

## Search Page Visibility

The pharmacy sale bill search page (`pharmacy_search_sale_bill.xhtml`) uses two queries:

1. `createPharmacyRetailBills()` — fetches `PreBill` entities with `billType=PharmacyPre` and `billedBill=null`
2. `fetchNativeRetailSaleBills()` — fetches `BilledBill` entities with `billTypeAtomic=PHARMACY_RETAIL_SALE`

The XHTML shows **PAID** when `bill.referenceBill ne null OR billTypeAtomic = 'PHARMACY_RETAIL_SALE'`.

For **PreBill** records in the list:
- `bill.referenceBill` = BilledBill → not null → PAID ✅ (requires L2 cache coherence — use JPA merge, not native SQL, for the cross-link)
- `bill.referenceBill.createdAt` = BilledBill's `createdAt` → populates "Billed At" column

For **BilledBill** records from `fetchNativeRetailSaleBills()`:
- `billTypeAtomic = 'PHARMACY_RETAIL_SALE'` → always PAID ✅

If a native bill shows **NOT PAID** or **Billed At** is empty, the cause is almost always that the PreBill→BilledBill `referenceBill` cross-link was written via native SQL instead of JPA, leaving the L2 cache stale.

---

## Return Flow Compatibility

`SaleReturnController` at `navigateToReturnItemsAndPaymentsForPharmacyRetailSale()`:

1. Sets `bill` = PreBill (from search DTO — `createPreBillSearchDTOs()`)
2. Calls `generateBillComponent()` → `getPharmaceuticalBillItems(getBill())` — reads PBI from **PreBill** items
3. `updateOriginalBillsForReturn()` at line 862:
   - `PHARMACY_RETAIL_SALE_PRE` case: `saleBill = originalBill.getReferenceBill()` → needs `referenceBill` set on PreBill
   - `PHARMACY_RETAIL_SALE` case: `salePreBill = originalBill.getReferenceBill()` → needs `referenceBill` set on BilledBill
4. `updateBillFinancialFields(salePreBill, returnAmount)` at line 913: reads `salePreBill.getRefundAmount()` → NPE if `salePreBill` is null (i.e., `referenceBill` was not set)

**Requirement**: Both cross-links must be set and L2-cache-coherent before the transaction commits.

The PreBill PBI `costValue=0` is fine — `calculateAndRecordCostingValues()` in SaleReturnController recomputes cost values from `pharmaItem.getRetailRate()` / `purchaseRate` / `getItemBatch().getCostRate()`.

---

## Common Mistakes

### 1. Native SQL for the PreBill→BilledBill cross-link

```java
// WRONG — bypasses L2 cache; PreBill entity cached with referenceBill=null
em.createNativeQuery("UPDATE bill SET referenceBill_ID=? WHERE ID=?")
    .setParameter(1, billId).setParameter(2, preBillId).executeUpdate();

// CORRECT — keeps L2 cache coherent
preBill.setReferenceBill(saleBill);
em.merge(preBill);
em.flush();
```

### 2. BFD on PreBill instead of BilledBill

`insertFinanceDetails(billId, ...)` must use the **BilledBill** ID, not `preBillId`. Reports query `BilledBill.billFinanceDetails`.

### 3. BIFD on PreBill items instead of BilledBill items

BIFD must be linked to BilledBill items. Pass `biIds` (BilledBill item IDs) to `insertFinanceDetails`, not `biPreIds`.

### 4. StockHistory on BilledBill PBI instead of PreBill PBI

`insertStockHistory(pbPreIds[i], ...)` must use the PreBill PBI ID. This mirrors the old flow where `StockHistory.PBITEM_ID` points to the PreBill's PBI.

### 5. Missing `.negate()` on BFD totals

`totalFreeQuantity` is easy to miss — it must also be negated. Every BFD field that represents a quantity or value movement for stock-out must be negated.

### 6. Not evicting PreBill and BilledBill from L2 cache

After native INSERT/UPDATE operations, evict all affected classes:

```java
javax.persistence.Cache cache = em.getEntityManagerFactory().getCache();
cache.evict(StockHistory.class);
cache.evict(Stock.class);
cache.evict(BillItem.class);
cache.evict(Bill.class);
cache.evict(PreBill.class);
cache.evict(BilledBill.class);
```

### 7. Drawer not updated

`DrawerController.updateDrawerForIns(payment)` must be called in the **controller** after the service returns the `Payment` object. It cannot be called inside the `@Stateless` service.

---

## Adapting for Other BillTypeAtomics

When implementing a native service for a different atomic type (e.g., `PHARMACY_DIRECT_ISSUE`, `PHARMACY_TRANSFER_ISSUE`):

1. **Identify the old JPA controller** that currently handles settlement for that type.
2. **Find all `setBillType` / `setBillTypeAtomic` calls** — reproduce exactly in `buildPreBill()` / `buildSaleBill()`.
3. **Check the old BFD update method** (e.g., `updateBillFinanceDetailsForRetailSale`) — match every field and sign.
4. **Check the old BIFD update method** — match every field and sign.
5. **Confirm PBI field population** — which bill gets bare PBI and which gets full PBI.
6. **Run the DB verification checklist** above with side-by-side old vs new bill comparison.
7. **Check the search page query** — confirm the new bill appears correctly (PAID, dates populated).
8. **Test the return flow end-to-end** — return item only, and return item + payment.

For bill types without a return flow (e.g., GRN, Transfer Receive), skip return flow testing but still verify the search page and BFD/BIFD values.

---

## Reference Files

| File | Purpose |
|---|---|
| `RetailSaleNativeSqlService.java` | Reference native service implementation |
| `RetailSaleNativeSqlController.java` | Reference controller (bill building, drawer update) |
| `PharmacySaleController.java` → `updateRetailSaleFinanceDetails()` | Ground truth for BIFD/BFD sign conventions |
| `SearchController.java` → `createPharmacyRetailBills()` / `fetchNativeRetailSaleBills()` | How bills appear in search |
| `SaleReturnController.java` → `updateOriginalBillsForReturn()` | How return flow uses the two-bill structure |
| `developer_docs/pharmacy/cost-accounting-sign-conventions.md` | Sign convention rationale |
