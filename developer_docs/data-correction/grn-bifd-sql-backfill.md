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

## ID Generation — AUTO_INCREMENT (all deployments as of v2.2.0)

All entity tables use `AUTO_INCREMENT` primary keys. There is no longer any need to:
- Stop Payara before running the script
- Check or bump the `SEQUENCE` table
- Pre-assign IDs manually

MySQL assigns IDs automatically. For a single `INSERT ... SELECT` statement, MySQL assigns
consecutive IDs and `LAST_INSERT_ID()` returns the **first** ID in that batch. The link-back
`UPDATE` uses `LAST_INSERT_ID() + rownum - 1` to reconstruct the mapping.

> **Note:** The `SEQUENCE` / `SEQ_GEN` table still exists in older databases but is no longer
> updated by EclipseLink. It can be ignored for this procedure.

---

## Prerequisites

### Check what needs fixing

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

## SQL Template (one month or date range at a time)

Process one period at a time and verify before moving to the next. Replace the date range
as needed.

```sql
-- =============================================================================
-- GRN BIFD + BFD Backfill - <PERIOD> (e.g. 2026-01-01 to 2026-06-01)
-- All IDs are assigned by MySQL AUTO_INCREMENT — no manual ID management needed.
-- Safe to run while Payara is running.
-- =============================================================================

SET SESSION TRANSACTION ISOLATION LEVEL READ COMMITTED;
SET @now = NOW();

-- ── STEP 1: Build work table ──────────────────────────────────────────────────
DROP TEMPORARY TABLE IF EXISTS tmp_bifd_work;
CREATE TEMPORARY TABLE tmp_bifd_work AS
SELECT
    (@rownum := @rownum + 1)            AS rownum,
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
AND b.CREATEDAT >= '<FROM_DATE>'   -- e.g. '2026-01-01'
AND b.CREATEDAT <  '<TO_DATE>'     -- e.g. '2026-06-01'
AND bi.BILLITEMFINANCEDETAILS_ID IS NULL
ORDER BY bi.ID;

-- Verify count before inserting
SELECT COUNT(*) AS items_to_insert FROM tmp_bifd_work;

-- ── STEP 2: INSERT BillItemFinanceDetails (AUTO_INCREMENT assigns IDs) ────────
INSERT INTO BillItemFinanceDetails (
    CREATEDAT, UNITSPERPACK,
    QUANTITY, FREEQUANTITY, QUANTITYBYUNITS, TOTALQUANTITYBYUNITS,
    GROSSRATE, LINEGROSSRATE, RETAILSALERATE, RETAILSALERATEPERUNIT,
    PURCHASERATE, COSTRATE, LINEDISCOUNTRATE,
    LINEGROSSTOTAL, LINENETTOTAL, TOTALCOST,
    VALUEATRETAILRATE, VALUEATPURCHASERATE, VALUEATCOSTRATE
)
SELECT
    @now, 1.0,
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
FROM tmp_bifd_work
ORDER BY rownum;   -- ← must match the order in tmp_bifd_work

SELECT ROW_COUNT() AS bifd_inserted;

-- Capture the first AUTO_INCREMENT ID assigned by the INSERT above
SET @first_bifd_id = LAST_INSERT_ID();

-- ── STEP 3: Link BIFD to BillItem ─────────────────────────────────────────────
-- Each inserted row's ID = @first_bifd_id + rownum - 1 (consecutive, same order)
UPDATE BillItem bi
JOIN tmp_bifd_work w ON w.bill_item_id = bi.ID
SET bi.BILLITEMFINANCEDETAILS_ID = @first_bifd_id + w.rownum - 1
WHERE bi.BILLITEMFINANCEDETAILS_ID IS NULL;

SELECT ROW_COUNT() AS billitems_linked;

-- ── STEP 4: Per-bill totals ────────────────────────────────────────────────────
DROP TEMPORARY TABLE IF EXISTS tmp_bill_totals;
CREATE TEMPORARY TABLE tmp_bill_totals AS
SELECT
    bill_id, existing_bfd_id,
    SUM(ABS(retail_rate   * qty))       AS total_sale_value,
    SUM(ABS(purchase_rate * qty))       AS total_purchase_value,
    SUM(ABS(cost_rate     * total_qty)) AS total_cost_value
FROM tmp_bifd_work
GROUP BY bill_id, existing_bfd_id;

-- ── STEP 5: INSERT new BFD rows for bills that had none ───────────────────────
DROP TEMPORARY TABLE IF EXISTS tmp_new_bfd;
CREATE TEMPORARY TABLE tmp_new_bfd AS
SELECT
    (@bfd_row := @bfd_row + 1)  AS bfd_rownum,
    t.bill_id,
    t.total_sale_value,
    t.total_purchase_value,
    t.total_cost_value
FROM (SELECT @bfd_row := 0) r,
     tmp_bill_totals t
WHERE t.existing_bfd_id IS NULL;

SELECT COUNT(*) AS new_bfd_count FROM tmp_new_bfd;

INSERT INTO BillFinanceDetails (
    CREATEDAT,
    TOTALRETAILSALEVALUE, TOTALPURCHASEVALUE, TOTALCOSTVALUE, BILLGROSSTOTAL
)
SELECT @now,
       total_sale_value, total_purchase_value, total_cost_value, total_purchase_value
FROM tmp_new_bfd
ORDER BY bfd_rownum;

SELECT ROW_COUNT() AS bfd_inserted;

SET @first_bfd_id = LAST_INSERT_ID();

UPDATE Bill b
JOIN tmp_new_bfd n ON n.bill_id = b.ID
SET b.BILLFINANCEDETAILS_ID = @first_bfd_id + n.bfd_rownum - 1
WHERE b.BILLFINANCEDETAILS_ID IS NULL;

SELECT ROW_COUNT() AS bills_linked_to_new_bfd;

-- ── STEP 6: UPDATE existing BFD rows if any ───────────────────────────────────
UPDATE BillFinanceDetails bfd
JOIN tmp_bill_totals t ON t.existing_bfd_id = bfd.ID
SET
    bfd.TOTALRETAILSALEVALUE = t.total_sale_value,
    bfd.TOTALPURCHASEVALUE   = t.total_purchase_value,
    bfd.TOTALCOSTVALUE       = t.total_cost_value,
    bfd.BILLGROSSTOTAL       = t.total_purchase_value
WHERE t.existing_bfd_id IS NOT NULL;

SELECT ROW_COUNT() AS existing_bfd_updated;

-- ── STEP 7: Verify ────────────────────────────────────────────────────────────
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
mysql -h 127.0.0.1 -P 3346 -u hmis_admin -p<password> <dbname> < backfill_month.sql
```

Check the output after each period:
- `bifd_inserted` should equal `items_to_insert`
- `billitems_linked` should equal `bifd_inserted`
- `remaining_missing_bifd` should be 0 (any non-zero items are orphans with no source data)

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
- `BillItemFacade.allocateSequenceBlock()` is used by the stock-take bulk INSERT path and
  still functions (it syncs the SEQ_COUNT to the current AUTO_INCREMENT max before advancing
  it). It is not needed for this backfill procedure.
