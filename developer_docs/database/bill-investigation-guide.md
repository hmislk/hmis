# Bill Investigation Guide

## How Staff Refer to Bills

When pharmacy staff, cashiers, or managers report a billing issue using a number like `MP/SALE/124491`, they are referring to the **`deptId`** (department-generated ID) field in the `bill` table — **not** the `id` (primary key).

| Staff language | Database field | Example |
|---|---|---|
| "bill MP/SALE/124491" | `bill.deptId` | `'MP/SALE/124491'` |
| "bill number 124491" | `bill.deptId` (suffix) | `WHERE deptId LIKE '%124491'` |
| internal reference | `bill.id` | `13884660` (never shown to staff) |

Always start an investigation with:

```sql
SELECT id, deptId, billType, billTime, nettotal, cancelled, paid, billtypeatomic,
       creater_id, referencebill_id
FROM bill
WHERE deptId = 'MP/SALE/124491';
```

## Bill Lifecycle — Pharmacy Retail Sale

The standard flow for a pharmacy pre-bill (`PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER`) produces **two** bill records sharing the same `deptId`:

```
PharmacyPre (deptId = MP/SALE/XXXXXX)   →   PharmacySale (deptId = MP/SALE/XXXXXX)
CANCELLED = 1                                 CANCELLED = 0
REFERENCEBILL_ID = PharmacySale.id            REFERENCEBILL_ID = PharmacyPre.id
```

A cancelled PharmacyPre **with no linked PharmacySale** means the settlement failed and no payment was collected.

## Key Fields at a Glance

| Field | Meaning |
|---|---|
| `deptId` | Human-readable bill number shown on receipts and to staff |
| `id` | Internal primary key — use for JOINs |
| `billType` | `PharmacyPre` (pending), `PharmacySale` (settled), etc. |
| `billtypeatomic` | Precise workflow step, e.g. `PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER` |
| `cancelled` | 1 = pre-bill was cancelled (settlement failed or explicitly cancelled) |
| `completed` | 1 = bill fully processed |
| `paid` | 1 = payment received |
| `referencebill_id` | Links PharmacySale ↔ PharmacyPre |
| `nettotal` | Financial total — may **not** include all `billitem` rows if items were added after initial save |

## Investigating Stock Issues on a Cancelled Pre-Bill

```sql
-- 1. Find the bill
SELECT id, deptId, billType, billTime, nettotal, cancelled, billtypeatomic, creater_id
FROM bill WHERE deptId = 'MP/SALE/124491';

-- 2. List all bill items (including those added after bill creation)
SELECT bi.id, i.name, bi.qty, bi.rate, bi.grossvalue, bi.retired, bi.createdat
FROM billitem bi
LEFT JOIN item i ON bi.item_id = i.id
WHERE bi.bill_id = <bill_id>;

-- 3. Check pharmaceutical (stock) movements
SELECT pbi.id, pbi.qty, pbi.stock_id, pbi.remainingqty, pbi.completedqty, pbi.createdat
FROM pharmaceuticalbillitem pbi
WHERE pbi.billitem_id IN (SELECT id FROM billitem WHERE bill_id = <bill_id>);

-- 4. Check for reversal bill
SELECT id, deptId, billType, billTime, nettotal, billtypeatomic, creater_id
FROM bill
WHERE referencebill_id = <bill_id>
   OR deptId LIKE 'MPPRECAN/%';

-- 5. Check audit events for the creator around the bill time
SELECT id, eventdatatime, eventtrigger, webuserid, beforejson, afterjson
FROM coopaudit.auditevent
WHERE webuserid = <creator_id>
  AND eventdatatime BETWEEN '<billtime - 1min>' AND '<billtime + 5min>';
```

## "Stock Locks Released - Exception" Audit Event

If the audit log shows `eventtrigger = 'Stock Locks Released - Exception'` near the bill creation time:

- `beforejson` contains the number of locks held and the failure reason (e.g. `"Settlement failed: Transaction aborted"`)
- `afterjson` shows whether locks were released successfully
- The number of locks corresponds to the number of items the user was settling at the time

This event means the settlement transaction was aborted. The pre-bill will be `cancelled=1` with no linked PharmacySale. Any items already persisted to `billitem` before the transaction started will remain in the database, and their `pharmaceuticalbillitem` stock deductions must be reversed manually (via a `PHARMACY_RETAIL_SALE_PRE_ADD_TO_STOCK` bill, `deptId` starting with `MPPRECAN/`).

## NETTOTAL vs Sum of Bill Items

`bill.nettotal` is computed at the time the bill is **saved**. If items are added to the bill after the initial save (common in pre-bills), the `nettotal` will **not** include those later items even though their `billitem` rows exist. Always cross-check:

```sql
SELECT SUM(bi.netvalue) as actualTotal, b.nettotal as recordedTotal
FROM bill b
JOIN billitem bi ON bi.bill_id = b.id
WHERE b.id = <bill_id>;
```

A discrepancy indicates items were added incrementally after the bill was first persisted.
