# F15 Report – BFD Backfill Guide for AI Agents

**Context:** The F15 Daily Stock Values report uses `BillFinanceDetails` (BFD) records to compute
`stockValueAtRetailRate` and `stockValueAtCostRate` for each pharmacy bill. When a BFD record is
missing or has zeroed values, the JPQL query (`LEFT JOIN … COALESCE(bfd.totalRetailSaleValue, 0.0)`)
silently treats that bill's stock value as **zero**, producing a discrepancy in the report.

---

## Which Bill Types May Have Missing or Zeroed BFDs

| Bill Type Atomic | Problem | Fixed | Backfillable |
|---|---|---|---|
| `PHARMACY_RETAIL_SALE_CANCELLED_PRE` | No BFD created before Nov 30, 2025 (commit `5e4ba42c`) | ✅ Nov 2025 | Manual via `bill_data_correction` API |
| `PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER` (path 1) | No BFD created before Jan 31, 2026 (commit `b611916985`) | ✅ Jan 2026 | Manual via `bill_data_correction` API |
| `PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER` (path 2) | No BFD created before Feb 20, 2026 | ✅ Feb 2026 | Manual via `bill_data_correction` API |
| `PHARMACY_STOCK_ADJUSTMENT` | No BFD created at all before Feb 23, 2026 | ✅ Feb 23, 2026 | **✅ `POST /api/pharmacy/backfill_bfd`** |
| `PHARMACY_RETAIL_RATE_ADJUSTMENT` | `bill.total = 0` not persisted; BFD `grossTotal` partial before Feb 23, 2026 | ✅ Feb 23, 2026 | **✅ `POST /api/pharmacy/backfill_bfd`** |
| `PHARMACY_GRN` (and related GRN types) | `BillItemFinanceDetails` (BIFD) not linked to BillItems — GRN reprint shows blank item columns | GRN creation logic unchanged; BIFD must be backfilled manually | **✅ `POST /api/pharmacy/backfill_grn_bifd`** |

---

## When to Use This Guide

- **Adjustment bill discrepancies**: If the F15 `adjustments` section shows 0.00 for
  `PHARMACY_STOCK_ADJUSTMENT` or `PHARMACY_RETAIL_RATE_ADJUSTMENT` bills, use the
  `POST /api/pharmacy/backfill_bfd` endpoint described below.

- **Pre-bill / cancelled-pre discrepancies**: If the F15 `sales` section is missing values for
  pre-bills or cancelled pre-bills, use the `PATCH /api/bill_data_correction` endpoint (see
  Option B below), as those bill types store values differently.

- **GRN reprint shows blank item rows** (Purchase Rate, Line Net Value, Sale Rate, Cost Value all
  empty): The `BillItemFinanceDetails` records are missing for those bill items. Use
  `POST /api/pharmacy/backfill_grn_bifd` (Option C below).

---

## Step 1 – Diagnose: Which Bills Are Missing BFD?

### For PHARMACY_STOCK_ADJUSTMENT and PHARMACY_RETAIL_RATE_ADJUSTMENT

```sql
SELECT
    b.ID              AS bill_id,
    b.BILLTYPEATOMIC  AS bill_type,
    DATE(b.CREATEDAT) AS bill_date,
    b.TOTAL           AS bill_total,
    b.NETTOTAL        AS bill_net_total,
    b.BILLFINANCEDETAILS_ID AS bfd_id,   -- NULL = missing BFD
    bfd.GROSSTOTAL    AS bfd_gross,
    bfd.NETTOTAL      AS bfd_net,
    bfd.TOTALRETAILSALEVALUE AS bfd_retail
FROM bill b
LEFT JOIN billfinancedetails bfd ON bfd.ID = b.BILLFINANCEDETAILS_ID
WHERE b.BILLTYPEATOMIC IN (
        'PHARMACY_STOCK_ADJUSTMENT',
        'PHARMACY_RETAIL_RATE_ADJUSTMENT'
      )
  AND b.CREATEDAT BETWEEN '2020-01-01' AND '2026-02-22'
  AND b.RETIRED = 0
  AND (b.BILLFINANCEDETAILS_ID IS NULL OR b.TOTAL = 0)
ORDER BY b.BILLTYPEATOMIC, b.CREATEDAT;
```

Bills with `bfd_id = NULL` (stock adjustments) or `bill_total = 0` (retail rate adjustments) need
backfill.

### For PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER and PHARMACY_RETAIL_SALE_CANCELLED_PRE

```sql
SELECT
    b.ID              AS bill_id,
    b.BILLTYPEATOMIC,
    DATE(b.CREATEDAT) AS bill_date,
    b.NETTOTAL,
    bfd.ID            AS bfd_id   -- NULL means BFD is missing
FROM bill b
LEFT JOIN billfinancedetails bfd ON bfd.ID = b.BILLFINANCEDETAILS_ID
WHERE b.BILLTYPEATOMIC IN (
        'PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER',
        'PHARMACY_RETAIL_SALE_CANCELLED_PRE'
      )
  AND b.CREATEDAT BETWEEN '2026-01-01' AND '2026-02-20'
  AND b.RETIRED = 0
ORDER BY b.CREATEDAT;
```

### For GRN bills with blank reprint columns (missing BIFD)

```sql
SELECT
    b.ID              AS bill_id,
    b.BILLTYPEATOMIC,
    b.DEPTID          AS grn_number,
    DATE(b.CREATEDAT) AS bill_date,
    COUNT(bi.ID)      AS total_items,
    SUM(CASE WHEN bi.BILLITEMFINANCEDETAILS_ID IS NULL THEN 1 ELSE 0 END) AS items_missing_bifd
FROM bill b
JOIN billitem bi ON bi.BILL_ID = b.ID AND bi.RETIRED = 0
WHERE b.BILLTYPEATOMIC IN (
        'PHARMACY_GRN',
        'PHARMACY_GRN_CANCELLED',
        'PHARMACY_GRN_REFUND',
        'PHARMACY_GRN_RETURN',
        'PHARMACY_GRN_WHOLESALE',
        'PHARMACY_DIRECT_PURCHASE',
        'PHARMACY_DIRECT_PURCHASE_CANCELLED'
      )
  AND b.RETIRED = 0
GROUP BY b.ID, b.BILLTYPEATOMIC, b.DEPTID, DATE(b.CREATEDAT)
HAVING items_missing_bifd > 0
ORDER BY b.CREATEDAT DESC;
```

Bills with `items_missing_bifd > 0` need BIFD backfill.

---

## Step 2 – Apply Correction

### Option A: Backfill API — for PHARMACY_STOCK_ADJUSTMENT and PHARMACY_RETAIL_RATE_ADJUSTMENT (recommended)

> **✅ Implemented 2026-02-23.**
>
> Files:
> - `src/main/java/com/divudi/ws/pharmacy/PharmacyBfdBackfillApi.java`
> - `src/main/java/com/divudi/service/pharmacy/PharmacyBfdBackfillService.java`

**Endpoint:** `POST /api/pharmacy/backfill_bfd`
**Auth:** `Finance: <API_KEY>` header

**Request body:**

```json
{
  "billTypeAtomics": [
    "PHARMACY_STOCK_ADJUSTMENT",
    "PHARMACY_RETAIL_RATE_ADJUSTMENT"
  ],
  "departmentId": 485,
  "fromDate": "2020-01-01",
  "toDate": "2026-02-22",
  "approvedBy": "Dr. Smith",
  "auditComment": "Backfill BFDs missing before 2026-02-23 fix – approved 2026-02-23"
}
```

**Parameters:**

| Field | Required | Description |
|---|---|---|
| `billTypeAtomics` | No | List of bill types. Omit to backfill both adjustment types. |
| `departmentId` | No | Filter to a specific department. Omit for all departments. |
| `fromDate` | Yes | Start date inclusive, format `yyyy-MM-dd`. |
| `toDate` | Yes | End date inclusive, format `yyyy-MM-dd`. |
| `auditComment` | Yes | Audit trail comment. |
| `approvedBy` | Yes | Name of approver. |

**Example cURL:**

```bash
curl -X POST "$BASE_URL/api/pharmacy/backfill_bfd" \
  -H "Finance: $FINANCE_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "billTypeAtomics": ["PHARMACY_STOCK_ADJUSTMENT", "PHARMACY_RETAIL_RATE_ADJUSTMENT"],
    "fromDate": "2020-01-01",
    "toDate": "2026-02-22",
    "approvedBy": "Dr. Smith",
    "auditComment": "Backfill BFDs missing before 2026-02-23 fix"
  }'
```

**Response:**

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "backfilledBills": 12,
    "skipped": 2,
    "errors": []
  }
}
```

- `backfilledBills`: number of bills that had BFD created or corrected
- `skipped`: bills found but with no computable values (e.g. no PharmaceuticalBillItems)
- `errors`: per-bill error messages if any bill failed

**What the endpoint does (per bill):**

For `PHARMACY_STOCK_ADJUSTMENT`:
- `pbi.qty` = changingQty (signed: positive = stock increase, negative = decrease)
- `pbi.retailRate` = retail rate at time of adjustment
- `pbi.costRate` = cost rate at time of adjustment
- Computes: `grossTotal = Σ |changingQty × retailRate|`, `netTotal = Σ changingQty × retailRate`,
  `totalRetailSaleValue = netTotal`, `totalCostValue = Σ changingQty × costRate`

For `PHARMACY_RETAIL_RATE_ADJUSTMENT`:
- `pbi.beforeAdjustmentValue` = old retail rate
- `pbi.afterAdjustmentValue` = new retail rate
- `bi.qty` = stock quantity on hand at time of adjustment
- Computes: `grossTotal = Σ |stockQty × (newRate - oldRate)|`,
  `netTotal = Σ stockQty × (newRate - oldRate)`,
  `totalRetailSaleValue = netTotal`

Also sets `bill.total = grossTotal` and `bill.netTotal = netTotal` for consistent reporting.
Appends a `[BFD Backfill]` entry to `bill.comments` as the audit trail.

---

### Option B: Patch via `bill_data_correction` API

**Use when:** A BFD already exists but has wrong values. This endpoint **cannot create** a missing
BFD — use Option A for that.

This is the correct approach for `PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER` and
`PHARMACY_RETAIL_SALE_CANCELLED_PRE` bills, which need manual value computation from bill items.

```bash
PATCH /api/bill_data_correction
{
  "targetType": "BILL_FINANCE_DETAILS",
  "targetId": <bfd_id>,
  "fields": {
    "totalRetailSaleValue": -1234.56
  },
  "auditComment": "Backfill retail sale value – approved by Dr. Smith",
  "approvedBy": "Dr. Smith"
}
```

Use `GET /api/costing_data/by_bill_id/{billId}` to retrieve bill items and compute the correct
`totalRetailSaleValue` (sum of `qty × retailRate`, negated for sales-type bills).

See `developer_docs/API_BILL_DATA_CORRECTION.md` for full field reference.

---

### Option C: GRN BIFD Backfill API — for GRN reprint blank columns

> **✅ Implemented 2026-02-26.**
>
> Files:
> - `src/main/java/com/divudi/ws/pharmacy/PharmacyGrnBifdBackfillApi.java`
> - `src/main/java/com/divudi/service/pharmacy/PharmacyGrnBifdBackfillService.java`
> - `tmp/backfill_grn_bifd.py` (agent script — processes month by month)

**Problem:** GRN bills saved before `BillItemFinanceDetails` was introduced have
`billItemFinanceDetails_id = NULL` on their `BillItem` rows. The GRN reprint template
(`grn.xhtml`) reads `bip.billItemFinanceDetails.lineGrossRate`, `.lineNetTotal`,
`.retailSaleRate`, `.totalCost` etc., so every item row appears blank even though the
underlying `PharmaceuticalBillItem` records have the correct rates and quantities.

**Endpoint:** `POST /api/pharmacy/backfill_grn_bifd`
**Auth:** `Finance: <API_KEY>` header

**Request body:**

```json
{
  "fromDate": "2020-01-01",
  "toDate": "2026-02-26",
  "approvedBy": "Dr. Ariyaratne",
  "auditComment": "Backfill missing BIFD for GRN reprints – approved 2026-02-26",
  "departmentId": 485,
  "billTypeAtomics": ["PHARMACY_GRN"]
}
```

**Parameters:**

| Field | Required | Description |
|---|---|---|
| `fromDate` | Yes | Start date inclusive, format `yyyy-MM-dd`. |
| `toDate` | Yes | End date inclusive, format `yyyy-MM-dd`. |
| `auditComment` | Yes | Audit trail comment. |
| `approvedBy` | Yes | Name of approver. |
| `departmentId` | No | Filter to a specific department. Omit for all departments. |
| `billTypeAtomics` | No | List of GRN bill type names. Omit to process all GRN types. |

**Supported `billTypeAtomics`:**
`PHARMACY_GRN`, `PHARMACY_GRN_CANCELLED`, `PHARMACY_GRN_REFUND`, `PHARMACY_GRN_RETURN`,
`PHARMACY_GRN_WHOLESALE`, `PHARMACY_DIRECT_PURCHASE`, `PHARMACY_DIRECT_PURCHASE_CANCELLED`

**Example cURL:**

```bash
curl -X POST "$BASE_URL/api/pharmacy/backfill_grn_bifd" \
  -H "Finance: $FINANCE_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "fromDate": "2020-01-01",
    "toDate": "2026-02-26",
    "approvedBy": "Dr. Ariyaratne",
    "auditComment": "Backfill missing BIFD for GRN reprints"
  }'
```

**Response:**

```json
{
  "status": "success",
  "code": 200,
  "data": {
    "totalBillsFound": 150,
    "processedBills": 148,
    "skippedBills": 2,
    "processedItems": 620,
    "skippedItems": 4,
    "errors": []
  }
}
```

| Field | Description |
|---|---|
| `totalBillsFound` | Bills matched by the query (have at least one item with null BIFD). |
| `processedBills` | Bills where at least one item was backfilled. |
| `skippedBills` | Bills where all items lacked `PharmaceuticalBillItem` data. |
| `processedItems` | Individual BillItem records updated. |
| `skippedItems` | Items skipped (no `PharmaceuticalBillItem` or no `ItemBatch`). |
| `errors` | Per-bill error messages (bill continues on error; others are not affected). |

**What the endpoint does per bill item:**

- Source: `PharmaceuticalBillItem.qty`, `freeQty`, `retailRate` + `ItemBatch.purcahseRate`, `costRate`
- Sign convention: normal GRN = factor `-1` (stock in, expense); cancellation/refund = factor `+1`
- Only writes BIFD fields that are still `null` (safe to re-run)
- Creates new `BillFinanceDetails` if missing, or updates existing
- Appends `[GRN BIFD Backfill]` entry to `bill.comments`

**For large date ranges, use the agent script instead of a single cURL call:**

```bash
# Edit START_DATE / END_DATE / DEPARTMENT_ID in the script first
python tmp/backfill_grn_bifd.py
```

The script splits the range into one-month batches with a 2-second pause between each,
avoiding server timeouts.

**Verify after running:**

Re-run the diagnostic query above — `items_missing_bifd` should be 0 for all bills in the
processed range. Then reprint any affected GRN — all item columns should now show values.

---

## Step 3 – Verify

After backfill, re-run the diagnostic query from Step 1 — all `bfd_id` should be non-null and
`bfd_gross` should be non-zero.

Then call the F15 API to confirm the discrepancy is resolved:

```bash
curl "$BASE_URL/api/pharmacy_f15_report?date=<date>&departmentId=<id>" \
  -H "Finance: $FINANCE_KEY"
```

Check `balanceCheck.discrepancyAtRetailRate` — it should be near zero (within tolerance).

---

## Important Constraints

- **NEVER** invent or guess BFD values. Always derive them from `PharmaceuticalBillItem` records.
- **ALWAYS** get human approval before applying any correction.
- **ALWAYS** include `auditComment` and `approvedBy` in every write API call.
- A `PHARMACY_STOCK_ADJUSTMENT` with a net decrease has **negative** `netTotal` (stock flowing out).
- A `PHARMACY_RETAIL_RATE_ADJUSTMENT` with a rate increase has **positive** `netTotal`.
- A `PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER` bill has **negative** `totalRetailSaleValue`
  (stock flows out).
- A `PHARMACY_RETAIL_SALE_CANCELLED_PRE` bill has **positive** `totalRetailSaleValue` (stock
  is restored).

---

## Related Files

| File | Role |
|---|---|
| `PharmacyBfdBackfillApi.java` | `POST /api/pharmacy/backfill_bfd` REST endpoint (adjustment bills) |
| `PharmacyBfdBackfillService.java` | EJB service: finds adjustment bills, computes and persists BFD values |
| `PharmacyGrnBifdBackfillApi.java` | `POST /api/pharmacy/backfill_grn_bifd` REST endpoint (GRN bills) |
| `PharmacyGrnBifdBackfillService.java` | EJB service: finds GRN bills with null BIFD, backfills from PBI data |
| `tmp/backfill_grn_bifd.py` | Agent script: runs GRN BIFD backfill month by month to avoid timeouts |
| `PharmacyAdjustmentController.java` | Where adjustment bills are saved; `pbi.beforeAdjustmentValue` meaning varies by bill type |
| `PharmacyService.java` | `fetchPharmacyAdjustmentValueByBillTypeDto()` – uses adjustment-specific BillLight query |
| `BillService.java` | `fetchBillLightsForAdjustmentsWithFinanceDetails()` – prefers `bfd.grossTotal` over `bill.total` for legacy bills |
| `BillLight.java` | 14-parameter constructor: uses `bfd.grossTotal` as fallback when `bill.total = 0` |
| `PharmacySaleForCashierController.java` | `calculateAndRecordCostingValues()` – BFD logic for sales |
| `PharmacyBean.java` | `createFinanceDetailsForCancellationBill()` – BFD for CANCELLED_PRE |
| `resources/pharmacy/grn.xhtml` | GRN reprint template: reads `bip.billItemFinanceDetails.*` fields |
| `developer_docs/API_F15_REPORT.md` | F15 API usage guide and investigation workflow |
| `developer_docs/API_BILL_DATA_CORRECTION.md` | Correction API reference (UPDATE only, no CREATE) |
