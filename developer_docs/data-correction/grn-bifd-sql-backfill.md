# GRN BIFD & BFD SQL Backfill Guide

**Purpose:** Direct SQL method for backfilling missing `BillItemFinanceDetails` (BIFD) and
`BillFinanceDetails` (BFD) records on GRN-type pharmacy bills.

**Use when:** The REST API (`POST /api/pharmacy/backfill_grn_bifd`) is not deployed or not
accessible, and you need to correct data directly on the database.

**Related guide:** [`developer_docs/pharmacy/f15-bfd-backfill-guide.md`](../pharmacy/f15-bfd-backfill-guide.md)

---

## Background

GRN bills saved before `BillItemFinanceDetails` was linked properly have
`BILLITEMFINANCEDETAILS_ID = NULL` on their `BillItem` rows. The GRN reprint template
(`grn.xhtml`) reads `bip.billItemFinanceDetails.lineGrossRate`, `.retailSaleRate`,
`.lineNetTotal`, `.totalCost` etc., so every item row appears blank on reprints.

Source data for the correction comes from:
- `PharmaceuticalBillItem` — `QTY`, `FREEQTY`, `RETAILRATE`, `PURCHASERATE`
- `ItemBatch` — `PURCAHSERATE` (intentional typo — DB compatibility), `COSTRATE`

---

## Prerequisites

### 1. Stop local Payara (if running)

EclipseLink uses a single shared `SEQ_GEN` sequence table for **all** entity ID generation.
If Payara is running, it will compete for IDs and cause duplicate key errors.

- **Local Payara:** Stop it before running this script.
- **Production Payara (remote):** Cannot be stopped. Use the ID-bumping technique below to
  stay ahead of its ID allocation.

### 2. Check what needs fixing

```sql
SELECT
    DATE_FORMAT(b.CREATEDAT, '%Y-%m') AS month,
    b.BILLTYPEATOMIC,
    COUNT(DISTINCT bi.ID)             AS items_missing_bifd,
    COUNT(DISTINCT b.ID)              AS bills
FROM BillItem bi
JOIN Bill b ON b.ID = bi.BILL_ID
WHERE b.BILLTYPEATOMIC IN (
    'PHARMACY_GRN', 'PHARMACY_GRN_CANCELLED', 'PHARMACY_GRN_REFUND',
    'PHARMACY_GRN_RETURN', 'PHARMACY_GRN_WHOLESALE',
    'PHARMACY_DIRECT_PURCHASE', 'PHARMACY_DIRECT_PURCHASE_CANCELLED'
)
AND b.RETIRED = 0 AND bi.RETIRED = 0
AND bi.BILLITEMFINANCEDETAILS_ID IS NULL
GROUP BY month, b.BILLTYPEATOMIC
ORDER BY month DESC, b.BILLTYPEATOMIC;
```

### 3. Reserve a block of IDs

Check the current sequence and max IDs in use:

```sql
SELECT SEQ_COUNT FROM SEQUENCE WHERE SEQ_NAME = 'SEQ_GEN';
SELECT MAX(ID) AS max_bifd FROM BillItemFinanceDetails;
SELECT MAX(ID) AS max_bfd  FROM BillFinanceDetails;
```

Bump `SEQ_GEN` well ahead of the current max to reserve a safe block. Allow ~1.5× the number
of rows you need (BIFD rows + BFD rows + margin for production Payara activity):

```sql
-- Example: bumping by 2000 to cover ~1300 rows with margin
UPDATE SEQUENCE SET SEQ_COUNT = <current_value> + 2000 WHERE SEQ_NAME = 'SEQ_GEN';
```

> **Why bump by extra?** Production Payara pre-allocates ID blocks of 50 in memory. Even after
> bumping SEQ_GEN, it may use IDs up to ~50 beyond its last allocated block before re-reading
> the sequence. Bumping by 1.5–2× the needed count ensures you stay ahead.

---

## Sign Convention

This is critical — getting signs wrong corrupts financial reports.

| Bill Category | Bill Types | Factor | Result |
|---|---|---|---|
| `BILL` (normal GRN) | `PHARMACY_GRN`, `PHARMACY_GRN_WHOLESALE`, `PHARMACY_DIRECT_PURCHASE` | **-1** | Negative values (stock in = expense) |
| `CANCELLATION` | `PHARMACY_GRN_CANCELLED`, `PHARMACY_DIRECT_PURCHASE_CANCELLED` | **+1** | Positive values (reverses expense) |
| `REFUND` | `PHARMACY_GRN_REFUND`, `PHARMACY_GRN_RETURN` | **+1** | Positive values (money/stock back) |

This matches the Java implementation in
`PharmacySummaryReportController.addFinancialDetailsForPharmacyGRNsFromBillItemData`.

---

## SQL Template (one month at a time)

Process one month at a time and verify before moving to the next. Replace the date range and
starting ID values as needed.

```sql
-- =============================================================================
-- GRN BIFD + BFD Backfill - <MONTH YEAR> (e.g. 2025-07-01 to 2025-08-01)
-- BIFD IDs start at: <BIFD_START>   (= SEQ_GEN value before bump + 1)
-- BFD  IDs start at: <BFD_START>    (= BIFD_START + estimated item count + 1)
-- Run while local Payara is STOPPED
-- =============================================================================

SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
SET @now = NOW();

-- ── STEP 1: Build work table with pre-assigned IDs ───────────────────────────
DROP TEMPORARY TABLE IF EXISTS tmp_bifd_work;
CREATE TEMPORARY TABLE tmp_bifd_work AS
SELECT
    (@rownum := @rownum + 1)            AS rownum,
    (<BIFD_START> + @rownum)            AS new_bifd_id,   -- <-- adjust BIFD_START
    bi.ID                               AS bill_item_id,
    b.ID                                AS bill_id,
    b.BILLFINANCEDETAILS_ID             AS existing_bfd_id,
    CASE
        WHEN b.BILLTYPEATOMIC IN (
            'PHARMACY_GRN_CANCELLED', 'PHARMACY_GRN_REFUND',
            'PHARMACY_DIRECT_PURCHASE_CANCELLED'
        ) THEN 1.0 ELSE -1.0
    END                                 AS factor,
    ABS(IFNULL(pbi.QTY,     0))        AS qty,
    ABS(IFNULL(pbi.FREEQTY, 0))        AS free_qty,
    ABS(IFNULL(pbi.QTY, 0)) + ABS(IFNULL(pbi.FREEQTY, 0)) AS total_qty,
    ABS(IFNULL(ib.PURCAHSERATE, 0))    AS purchase_rate,
    ABS(IFNULL(
        CASE WHEN ib.COSTRATE > 0 THEN ib.COSTRATE ELSE NULL END,
        ib.PURCAHSERATE
    ))                                  AS cost_rate,
    ABS(IFNULL(pbi.RETAILRATE, 0))     AS retail_rate,
    IFNULL(bi.GROSSVALUE, 0)           AS bi_gross_value,
    IFNULL(bi.NETVALUE,   0)           AS bi_net_value,
    IFNULL(bi.DISCOUNT,   0)           AS bi_discount
FROM (SELECT @rownum := 0) r,
     BillItem bi
JOIN Bill b   ON b.ID  = bi.BILL_ID
JOIN PharmaceuticalBillItem pbi ON pbi.BILLITEM_ID = bi.ID
JOIN ItemBatch ib ON ib.ID = pbi.ITEMBATCH_ID
WHERE b.BILLTYPEATOMIC IN (
    'PHARMACY_GRN', 'PHARMACY_GRN_CANCELLED', 'PHARMACY_GRN_REFUND',
    'PHARMACY_GRN_RETURN', 'PHARMACY_GRN_WHOLESALE',
    'PHARMACY_DIRECT_PURCHASE', 'PHARMACY_DIRECT_PURCHASE_CANCELLED'
)
AND b.RETIRED = 0 AND bi.RETIRED = 0
AND b.CREATEDAT >= '<FROM_DATE>'   -- e.g. '2025-07-01'
AND b.CREATEDAT <  '<TO_DATE>'     -- e.g. '2025-08-01'
AND bi.BILLITEMFINANCEDETAILS_ID IS NULL
ORDER BY bi.ID;

-- Verify ID range before inserting
SELECT COUNT(*) AS items_to_insert,
       MIN(new_bifd_id) AS id_from,
       MAX(new_bifd_id) AS id_to
FROM tmp_bifd_work;
-- ⚠️ Check that id_to < SEQ_GEN bumped value and no overlap with existing IDs

-- ── STEP 2: INSERT BillItemFinanceDetails ────────────────────────────────────
INSERT INTO BillItemFinanceDetails (
    ID, CREATEDAT, UNITSPERPACK,
    QUANTITY, FREEQUANTITY, QUANTITYBYUNITS, TOTALQUANTITYBYUNITS,
    GROSSRATE, LINEGROSSRATE, RETAILSALERATE, RETAILSALERATEPERUNIT,
    PURCHASERATE, COSTRATE, LINEDISCOUNTRATE,
    LINEGROSSTOTAL, LINENETTOTAL, TOTALCOST,
    VALUEATRETAILRATE, VALUEATPURCHASERATE, VALUEATCOSTRATE
)
SELECT
    new_bifd_id, @now, 1.0,
    qty, free_qty, qty, total_qty,
    purchase_rate, purchase_rate, retail_rate, retail_rate,
    purchase_rate, cost_rate,
    CASE WHEN qty > 0 THEN bi_discount / qty ELSE 0 END,
    -- Use existing bill values if present, otherwise calculate
    CASE WHEN ABS(bi_gross_value) > 0.001
         THEN bi_gross_value ELSE factor * purchase_rate * qty END,
    CASE WHEN ABS(bi_net_value) > 0.001
         THEN bi_net_value   ELSE factor * purchase_rate * qty END,
    ABS(cost_rate * total_qty),          -- TOTALCOST always positive
    factor * retail_rate   * qty,
    factor * purchase_rate * qty,
    factor * cost_rate     * total_qty
FROM tmp_bifd_work;

SELECT ROW_COUNT() AS bifd_inserted;

-- ── STEP 3: Link BIFD to BillItem ────────────────────────────────────────────
UPDATE BillItem bi
JOIN tmp_bifd_work w ON w.bill_item_id = bi.ID
SET bi.BILLITEMFINANCEDETAILS_ID = w.new_bifd_id
WHERE bi.BILLITEMFINANCEDETAILS_ID IS NULL;

SELECT ROW_COUNT() AS billitems_linked;

-- ── STEP 4: Per-bill totals ───────────────────────────────────────────────────
DROP TEMPORARY TABLE IF EXISTS tmp_bill_totals;
CREATE TEMPORARY TABLE tmp_bill_totals AS
SELECT
    bill_id, existing_bfd_id,
    SUM(ABS(retail_rate   * qty))       AS total_sale_value,
    SUM(ABS(purchase_rate * qty))       AS total_purchase_value,
    SUM(ABS(cost_rate     * total_qty)) AS total_cost_value
FROM tmp_bifd_work
GROUP BY bill_id, existing_bfd_id;

-- Materialise count into variable to avoid "can't reopen temp table" MySQL error
SELECT COUNT(*) INTO @null_bfd_count FROM tmp_bill_totals WHERE existing_bfd_id IS NULL;

-- ── STEP 5: INSERT new BFD rows for bills that had none ──────────────────────
DROP TEMPORARY TABLE IF EXISTS tmp_new_bfd;
CREATE TEMPORARY TABLE tmp_new_bfd AS
SELECT
    (@bfd_row := @bfd_row + 1)   AS bfd_rownum,
    (<BFD_START> + @bfd_row)     AS new_bfd_id,   -- <-- adjust BFD_START
    t.bill_id,
    t.total_sale_value,
    t.total_purchase_value,
    t.total_cost_value
FROM (SELECT @bfd_row := 0) r,
     tmp_bill_totals t
WHERE t.existing_bfd_id IS NULL;

SELECT COUNT(*) AS new_bfd_count,
       MIN(new_bfd_id) AS id_from,
       MAX(new_bfd_id) AS id_to
FROM tmp_new_bfd;

INSERT INTO BillFinanceDetails (
    ID, CREATEDAT,
    TOTALRETAILSALEVALUE, TOTALPURCHASEVALUE, TOTALCOSTVALUE, BILLGROSSTOTAL
)
SELECT new_bfd_id, @now,
       total_sale_value, total_purchase_value, total_cost_value, total_purchase_value
FROM tmp_new_bfd;

SELECT ROW_COUNT() AS bfd_inserted;

UPDATE Bill b
JOIN tmp_new_bfd n ON n.bill_id = b.ID
SET b.BILLFINANCEDETAILS_ID = n.new_bfd_id
WHERE b.BILLFINANCEDETAILS_ID IS NULL;

SELECT ROW_COUNT() AS bills_linked_to_new_bfd;

-- ── STEP 6: UPDATE existing BFD rows if any ──────────────────────────────────
UPDATE BillFinanceDetails bfd
JOIN tmp_bill_totals t ON t.existing_bfd_id = bfd.ID
SET
    bfd.TOTALRETAILSALEVALUE = t.total_sale_value,
    bfd.TOTALPURCHASEVALUE   = t.total_purchase_value,
    bfd.TOTALCOSTVALUE       = t.total_cost_value,
    bfd.BILLGROSSTOTAL       = t.total_purchase_value
WHERE t.existing_bfd_id IS NOT NULL;

SELECT ROW_COUNT() AS existing_bfd_updated;

-- ── STEP 7: Verify ───────────────────────────────────────────────────────────
SELECT COUNT(*) AS remaining_missing_bifd
FROM BillItem bi JOIN Bill b ON b.ID = bi.BILL_ID
WHERE b.BILLTYPEATOMIC IN (
    'PHARMACY_GRN', 'PHARMACY_GRN_CANCELLED', 'PHARMACY_GRN_REFUND',
    'PHARMACY_GRN_RETURN', 'PHARMACY_GRN_WHOLESALE',
    'PHARMACY_DIRECT_PURCHASE', 'PHARMACY_DIRECT_PURCHASE_CANCELLED'
)
AND b.RETIRED = 0 AND bi.RETIRED = 0
AND b.CREATEDAT >= '<FROM_DATE>'
AND b.CREATEDAT <  '<TO_DATE>'
AND bi.BILLITEMFINANCEDETAILS_ID IS NULL;
-- Expected: 0 (or small number of orphaned items with no PharmaceuticalBillItem)

-- Cleanup
DROP TEMPORARY TABLE IF EXISTS tmp_bifd_work;
DROP TEMPORARY TABLE IF EXISTS tmp_bill_totals;
DROP TEMPORARY TABLE IF EXISTS tmp_new_bfd;
```

---

## Running the Script

```bash
mysql -h 127.0.0.1 -P 3336 -u hmis_admin -p<password> <dbname> < backfill_month.sql
```

Check the output after each month:
- `bifd_inserted` should equal `items_to_insert`
- `billitems_linked` should equal `bifd_inserted`
- `remaining_missing_bifd` should be 0 (any non-zero items are orphans with no source data)

---

## Handling ID Collisions

If you get `ERROR 1062: Duplicate entry '...' for key 'PRIMARY'`, production Payara has
consumed IDs inside your reserved block. Fix:

```sql
-- 1. Check current state
SELECT SEQ_COUNT FROM SEQUENCE WHERE SEQ_NAME = 'SEQ_GEN';
SELECT MAX(ID) AS max_bifd FROM BillItemFinanceDetails;
SELECT MAX(ID) AS max_bfd  FROM BillFinanceDetails;

-- 2. Bump SEQ_GEN well ahead (use a large gap if production is active)
UPDATE SEQUENCE SET SEQ_COUNT = <max_bifd or max_bfd> + 2000
WHERE SEQ_NAME = 'SEQ_GEN';

-- 3. Re-run the script with BIFD_START = current max_bifd + 1
```

The query in Step 2 (`AND bi.BILLITEMFINANCEDETAILS_ID IS NULL`) is **idempotent** — already
fixed items are automatically excluded, so re-running is safe.

---

## Orphaned Items (Unfixable)

Some items have no `PharmaceuticalBillItem` record — the JOIN in Step 1 excludes them.
These cannot be corrected via SQL because there is no source data for the rates and quantities.

To identify them:

```sql
SELECT b.DEPTID, bi.ID AS bill_item_id, b.BILLTYPEATOMIC
FROM BillItem bi
JOIN Bill b ON b.ID = bi.BILL_ID
LEFT JOIN PharmaceuticalBillItem pbi ON pbi.BILLITEM_ID = bi.ID
WHERE b.BILLTYPEATOMIC IN (
    'PHARMACY_GRN', 'PHARMACY_GRN_CANCELLED', 'PHARMACY_GRN_REFUND',
    'PHARMACY_GRN_RETURN', 'PHARMACY_GRN_WHOLESALE',
    'PHARMACY_DIRECT_PURCHASE', 'PHARMACY_DIRECT_PURCHASE_CANCELLED'
)
AND b.RETIRED = 0 AND bi.RETIRED = 0
AND bi.BILLITEMFINANCEDETAILS_ID IS NULL
AND pbi.ID IS NULL;
```

These are historical data integrity issues and can be reported but not corrected.

---

## Notes

- `PURCAHSERATE` on `ItemBatch` is an **intentional typo** preserved for database compatibility.
  Do not rename it.
- `TOTALCOST` is always stored as a **positive** value regardless of bill category.
- `LINEGROSSTOTAL` and `LINENETTOTAL` carry the sign (negative for normal GRNs, positive for
  cancellations/returns).
- The existing admin page function (`addFinancialDetailsForPharmacyGRNsFromBillItemData`) does
  the same calculation but processes bills one at a time via JPA, making it extremely slow for
  large backlogs. Use this SQL method or the REST API instead.
