# F15 Report – BFD Backfill Guide for AI Agents

**Context:** The F15 Daily Stock Values report uses `BillFinanceDetails` (BFD) records to compute
`stockValueAtRetailRate` for each pharmacy bill. When a BFD record is missing, the query
(`LEFT JOIN … COALESCE(bfd.totalRetailSaleValue, 0.0)`) silently treats that bill's stock value
as **zero**, producing a discrepancy.

---

## Which Bill Types May Have Missing BFDs

| Bill Type Atomic | BFD Creation Added | Bills Before That Date |
|---|---|---|
| `PHARMACY_RETAIL_SALE_CANCELLED_PRE` | Nov 30, 2025 (commit `5e4ba42c`) | **No BFD** |
| `PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER` (path 1 – `settlePreBillAndNavigateToPrint`) | Jan 31, 2026 (commit `b611916985`) | **No BFD** |
| `PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER` (path 2 – `settlePreBill`) | Feb 20, 2026 (current fix) | **No BFD** |

All other sales bill types (`PHARMACY_RETAIL_SALE`, direct issues, etc.) have had BFD creation
since their original implementation.

---

## Step 1 – Diagnose: Which Bills Are Missing BFD?

Run this SQL to find affected bills for a given department and date range:

```sql
-- Replace department_id, from_date, to_date as needed
SELECT
    b.id              AS bill_id,
    b.bill_type_atomic,
    DATE(b.created_at) AS bill_date,
    b.net_total,
    bfd.id            AS bfd_id   -- NULL means BFD is missing
FROM bill b
LEFT JOIN bill_finance_details bfd ON bfd.bill_id = b.id
WHERE b.bill_type_atomic IN (
        'PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER',
        'PHARMACY_RETAIL_SALE_CANCELLED_PRE'
      )
  AND b.department_id = 485          -- change as needed
  AND b.created_at BETWEEN '2026-01-01' AND '2026-02-20'  -- change as needed
  AND b.retired = 0
ORDER BY b.created_at;
```

Bills with `bfd_id = NULL` need backfill.

---

## Step 2 – Correction Options

### Option A: Use the Backfill API Endpoint (recommended)

> **⚠️ This endpoint does not exist yet.** It must be implemented before an AI agent can use it.
> See [implementation spec below](#backfill-api-endpoint-spec).

Once implemented, call:

```http
POST /api/pharmacy/backfill_bfd
Content-Type: application/json
Finance: <API_KEY>

{
  "billTypeAtomics": [
    "PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER",
    "PHARMACY_RETAIL_SALE_CANCELLED_PRE"
  ],
  "departmentId": 485,
  "fromDate": "2026-01-01",
  "toDate": "2026-02-20",
  "approvedBy": "Dr. Smith",
  "auditComment": "Backfill BFDs missing from pre-bill creation – approved 2026-02-20"
}
```

The endpoint should:
1. Find all bills of the given types in the date+department range that have no BFD
2. For each bill, reload it with its `BillItems` and `PharmaceuticalBillItems`
3. Call the same `calculateAndRecordCostingValues(bill)` logic used in `PharmacySaleForCashierController`
4. Persist the new BFD and BIFD records
5. Return a summary: `{ backfilledBills: N, skipped: M, errors: [] }`

### Option B: Patch via `bill_data_correction` API (partial workaround)

**Limitation:** This only works for bills that *already have* a BFD record with a wrong value
(e.g. `totalRetailSaleValue = 0.0`). It **cannot create** a missing BFD.

For bills that already have a BFD but with zeroed values, compute the correct value from the bill
items and patch:

```json
PATCH /api/bill_data_correction
{
  "targetType": "BILL_FINANCE_DETAILS",
  "targetId": <bfd_id>,
  "fields": {
    "totalRetailSaleValue": -1234.56
  },
  "auditComment": "Backfill retail sale value for pre-bill BFD – approved by Dr. Smith",
  "approvedBy": "Dr. Smith"
}
```

Use `GET /api/costing_data/by_bill_id/{billId}` to retrieve the bill's items and compute the
correct `totalRetailSaleValue` (sum of `qty × retailRate`, negated for sales).

### Option C: SQL Migration (requires DBA access)

For deployment environments where an AI agent can request a DB migration:

```sql
-- Step 1: Create missing BFD rows with zero values as placeholders
INSERT INTO bill_finance_details (bill_id, total_retail_sale_value, total_cost_value,
                                   total_purchase_value, total_wholesale_value)
SELECT b.id, 0, 0, 0, 0
FROM bill b
LEFT JOIN bill_finance_details bfd ON bfd.bill_id = b.id
WHERE b.bill_type_atomic IN (
        'PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER',
        'PHARMACY_RETAIL_SALE_CANCELLED_PRE'
      )
  AND bfd.id IS NULL
  AND b.retired = 0;

-- Step 2: Update total_retail_sale_value by aggregating from bill items
-- (This requires joining through bill_item → pharmaceutical_bill_item → item_batch)
-- Run PharmacyBean.calculateAndRecordCostingValues logic here or trigger via the
-- backfill API endpoint (Option A) after implementing it.
```

---

## Backfill API Endpoint Spec

**File to create:** A new method in an existing REST controller (e.g.
`src/main/java/com/divudi/rest/pharmacy/PharmacyDataCorrectionController.java`) or in a new
`PharmacyBfdBackfillController.java`.

**Logic:**
```java
@POST
@Path("/pharmacy/backfill_bfd")
public Response backfillBfd(BackfillBfdRequest request, @HeaderParam("Finance") String apiKey) {
    // 1. Validate API key
    // 2. Find bills matching billTypeAtomics + departmentId + fromDate/toDate with no BFD
    //    SELECT b FROM Bill b LEFT JOIN b.billFinanceDetails bfd
    //    WHERE bfd IS NULL AND b.billTypeAtomic IN :types AND b.department.id = :deptId
    //    AND b.createdAt BETWEEN :from AND :to AND b.retired = false
    // 3. For each bill: reload with billItems + pharmaceuticalBillItems
    // 4. Call calculateAndRecordCostingValues(bill)  -- extract this to PharmacyBean or a shared service
    // 5. Persist and audit-log each backfilled bill
    // 6. Return summary
}
```

**Key shared service to extract:**
`PharmacySaleForCashierController.calculateAndRecordCostingValues(Bill)` is currently private.
Extract it to `PharmacyBean` (EJB) as a public method so both the controller and the backfill
endpoint can call it.

---

## Step 3 – Verify

After backfill, re-run the diagnostic query (Step 1) – all `bfd_id` should now be non-null.

Then call the F15 API to confirm discrepancy is resolved:

```bash
curl "$BASE_URL/api/pharmacy_f15_report?date=2026-02-18&departmentId=485" \
  -H "Finance: $FINANCE_KEY"
```

`balanceCheck.discrepancyAtRetailRate` should be near zero.

---

## Important Constraints

- **NEVER** invent or guess BFD values. Always derive them from `BillItem.qty × PharmaceuticalBillItem.retailRate` (negated for sales-type bills).
- **ALWAYS** get human approval before applying any correction.
- **ALWAYS** include `auditComment` and `approvedBy` in every API call that writes data.
- A `PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER` bill has **negative** `totalRetailSaleValue` (stock flows out).
- A `PHARMACY_RETAIL_SALE_CANCELLED_PRE` bill has **positive** `totalRetailSaleValue` (stock is restored).

---

## Related Files

| File | Role |
|---|---|
| `PharmacySaleForCashierController.java` | `calculateAndRecordCostingValues()` – the BFD creation logic (currently private) |
| `PharmacyBean.java` | `createFinanceDetailsForCancellationBill()` – BFD for CANCELLED_PRE |
| `PharmacyService.java` | `getPharmacyF15StockMovementBillTypes()` – the bill types F15 tracks |
| `PharmacyF15ReportApiService.java` | `generateReport()` – calls the fetch with stock-movement types |
| `BillService.java` | `fetchBillLightsWithFinanceDetailsAndPaymentScheme()` – LEFT JOINs BFD |
| `developer_docs/API_F15_REPORT.md` | F15 API usage guide |
| `developer_docs/API_BILL_DATA_CORRECTION.md` | Correction API (UPDATE only, no CREATE) |
