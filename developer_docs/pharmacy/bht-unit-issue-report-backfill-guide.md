# BHT Issue & Unit Issue Report — Historical Data Backfill Guide

**Related issue:** [#20996](https://github.com/hmislk/hmis/issues/20996)  
**Affected reports:**
- `/pharmacy/pharmacy_report_bht_issue_bill.xhtml` — BHT Issue Report
- `/pharmacy/pharmacy_report_unit_issue_bill.xhtml` — Unit Issue Report

---

## Background

After a recent update, both reports showed data for the current period but not for earlier months.
Each report has a **different root cause**.

---

## Report 1 — BHT Issue Report

### Root Cause

`ReportsTransfer.fillDepartmentBHTIssueByBillByDTOs()` navigates
`b.billFinanceDetails.totalPurchaseValue` as a path expression directly in the JPQL SELECT clause:

```java
"CASE WHEN b.billFinanceDetails IS NOT NULL THEN COALESCE(b.billFinanceDetails.totalPurchaseValue, 0.0) ELSE 0.0 END"
```

JPA/EclipseLink converts **any path navigation in the SELECT clause into an implicit INNER JOIN**,
even inside a `CASE WHEN`. Bills created before `BillFinanceDetails` rows were populated have
`BILLFINANCEDETAILS_ID = NULL` in the database, so the implicit INNER JOIN silently excludes them.

New bills have `BillFinanceDetails` populated at creation time → they appear.  
Old bills do not → they disappear.

### Permanent Code Fix (included in the PR that introduced this guide — issue #20996)

In `ReportsTransfer.java`, `fillDepartmentBHTIssueByBillByDTOs()`:

```java
// ADD this LEFT JOIN to the FROM clause:
.append("LEFT JOIN b.billFinanceDetails bfd ")

// REPLACE the CASE WHEN expression with:
.append("COALESCE(bfd.totalPurchaseValue, 0.0), ")
```

### Diagnosis Query

```sql
-- How many PharmacyBhtPre bills are missing BillFinanceDetails?
SELECT COUNT(*) AS bills_missing_bfd
FROM bill b
WHERE b.BILLTYPE = 'PharmacyBhtPre'
  AND b.RETIRED   = 0
  AND b.CREATEDAT >= '2026-03-01 00:00:00'
  AND b.CREATEDAT <  '2026-05-26 00:00:00'   -- adjust end date as needed
  AND b.BILLFINANCEDETAILS_ID IS NULL;
```

### Backfill — via Admin UI (preferred)

Go to `/dataAdmin/admin_functions.xhtml`:

1. Set the **date range** at the top of the page to the period with missing data.
2. Click **"Correct Finance Details for Inpatient Direct Issue Bills"**.
3. Confirm the prompt.
4. Check `executionFeedback` — should report 0 skipped bills.

This calls `DataAdministrationController.correctInpatientDirectIssueBillFinanceDetails()`, which
covers `DIRECT_ISSUE_INWARD_MEDICINE`, `DIRECT_ISSUE_THEATRE_MEDICINE`, `DIRECT_ISSUE_STORE_INWARD`,
and `ISSUE_MEDICINE_ON_REQUEST_INWARD`. Safe to re-run (skips bills already having a non-zero
`totalCostValue`).

### Backfill — via SQL (when UI is unavailable or for bulk historical periods)

Run the following on the target database. Replace the date range as needed.

**Step 1 — Check count:**
```sql
SELECT COUNT(*) AS bills_missing_bfd
FROM bill b
WHERE b.BILLTYPE = 'PharmacyBhtPre'
  AND b.RETIRED   = 0
  AND b.CREATEDAT >= '2026-03-01 00:00:00'
  AND b.CREATEDAT <  '2026-05-26 00:00:00'
  AND b.BILLFINANCEDETAILS_ID IS NULL;
```

**Step 2 — Run the stored procedure:**

```sql
DROP PROCEDURE IF EXISTS backfill_bht_bfd;

DELIMITER $$

CREATE PROCEDURE backfill_bht_bfd()
BEGIN
    DECLARE done        INT DEFAULT 0;
    DECLARE v_bill_id   BIGINT;
    DECLARE v_createdat DATETIME;
    DECLARE v_total     DOUBLE;
    DECLARE v_nettotal  DOUBLE;

    DECLARE v_totalPurchase  DECIMAL(38,4) DEFAULT 0;
    DECLARE v_totalCost      DECIMAL(38,4) DEFAULT 0;
    DECLARE v_totalRetail    DECIMAL(38,4) DEFAULT 0;
    DECLARE v_totalWholesale DECIMAL(38,4) DEFAULT 0;
    DECLARE v_totalQty       DECIMAL(38,4) DEFAULT 0;
    DECLARE v_totalFreeQty   DECIMAL(38,4) DEFAULT 0;
    DECLARE v_new_bfd_id     BIGINT;

    DECLARE cur CURSOR FOR
        SELECT b.ID, b.CREATEDAT, b.TOTAL, b.NETTOTAL
        FROM bill b
        WHERE b.BILLTYPE = 'PharmacyBhtPre'
          AND b.RETIRED   = 0
          AND b.CREATEDAT >= '2026-03-01 00:00:00'   -- adjust as needed
          AND b.CREATEDAT <  '2026-05-26 00:00:00'   -- adjust as needed
          AND b.BILLFINANCEDETAILS_ID IS NULL;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur;

    read_loop: LOOP
        FETCH cur INTO v_bill_id, v_createdat, v_total, v_nettotal;
        IF done THEN LEAVE read_loop; END IF;

        -- Aggregate rates from ItemBatch for this bill's items.
        -- Values are NEGATIVE (stock going OUT to patient).
        SELECT
            -SUM((ABS(bi.QTY) + COALESCE(pbi.FREEQTY, 0)) * ib.PURCAHSERATE),
            -SUM(ABS(bi.QTY)  * COALESCE(NULLIF(ib.COSTRATE, 0), ib.PURCAHSERATE)),
            -SUM((ABS(bi.QTY) + COALESCE(pbi.FREEQTY, 0)) * ib.RETAILSALERATE),
            -SUM((ABS(bi.QTY) + COALESCE(pbi.FREEQTY, 0)) * ib.WHOLESALERATE),
            -SUM(ABS(bi.QTY)),
            -SUM(COALESCE(pbi.FREEQTY, 0))
        INTO
            v_totalPurchase,
            v_totalCost,
            v_totalRetail,
            v_totalWholesale,
            v_totalQty,
            v_totalFreeQty
        FROM BillItem bi
        JOIN PharmaceuticalBillItem pbi ON pbi.BILLITEM_ID = bi.ID
        JOIN Stock s                    ON s.ID            = pbi.STOCK_ID
        JOIN ItemBatch ib               ON ib.ID           = s.ITEMBATCH_ID
        WHERE bi.BILL_ID = v_bill_id
          AND bi.RETIRED = 0;

        INSERT INTO BillFinanceDetails (
            CREATEDAT,
            TOTALPURCHASEVALUE,
            TOTALCOSTVALUE,
            TOTALRETAILSALEVALUE,
            TOTALWHOLESALEVALUE,
            TOTALQUANTITY,
            TOTALFREEQUANTITY,
            GROSSTOTAL,
            NETTOTAL
        ) VALUES (
            v_createdat,
            v_totalPurchase,
            v_totalCost,
            v_totalRetail,
            v_totalWholesale,
            v_totalQty,
            v_totalFreeQty,
            v_total,
            v_nettotal
        );

        -- LAST_INSERT_ID() is reliable here — one INSERT per loop iteration,
        -- single session, no concurrent inserts on this connection.
        SET v_new_bfd_id = LAST_INSERT_ID();

        UPDATE bill
        SET BILLFINANCEDETAILS_ID = v_new_bfd_id
        WHERE ID = v_bill_id;

    END LOOP;

    CLOSE cur;

    -- Final check — should return 0
    SELECT COUNT(*) AS still_missing
    FROM bill
    WHERE BILLTYPE = 'PharmacyBhtPre'
      AND RETIRED  = 0
      AND CREATEDAT >= '2026-03-01 00:00:00'
      AND CREATEDAT <  '2026-05-26 00:00:00'
      AND BILLFINANCEDETAILS_ID IS NULL;

END$$

DELIMITER ;

CALL backfill_bht_bfd();

DROP PROCEDURE IF EXISTS backfill_bht_bfd;
```

**Step 3 — Verify:**
```sql
SELECT
    COUNT(*)                      AS bills_linked,
    SUM(bfd.TOTALPURCHASEVALUE)   AS total_purchase,
    SUM(bfd.TOTALRETAILSALEVALUE) AS total_retail,
    MIN(b.CREATEDAT)              AS earliest,
    MAX(b.CREATEDAT)              AS latest
FROM bill b
JOIN BillFinanceDetails bfd ON bfd.ID = b.BILLFINANCEDETAILS_ID
WHERE b.BILLTYPE  = 'PharmacyBhtPre'
  AND b.RETIRED   = 0
  AND b.CREATEDAT >= '2026-03-01 00:00:00'
  AND b.CREATEDAT <  '2026-05-26 00:00:00';
```

`total_purchase` and `total_retail` should be negative (stock issued out). `bills_linked` should
match the count from Step 1.

### Sign Convention

| Field | Expected sign | Reason |
|---|---|---|
| `TOTALPURCHASEVALUE` | Negative | Stock leaving pharmacy |
| `TOTALCOSTVALUE` | Negative | Stock leaving pharmacy |
| `TOTALRETAILSALEVALUE` | Negative | Stock leaving pharmacy |
| `TOTALWHOLESALEVALUE` | Negative | Stock leaving pharmacy |
| `TOTALQUANTITY` | Negative | Quantity going out |
| `TOTALFREEQUANTITY` | Negative | Free quantity going out |
| `GROSSTOTAL` | Positive | From `bill.total` (bill header value) |
| `NETTOTAL` | Positive | From `bill.netTotal` (bill header value) |

---

## Report 2 — Unit Issue Report

### Root Cause

`ReportsTransfer.fillDepartmentUnitIssueByBill()` filters on:
```java
BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE
BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_CANCELLED
BillTypeAtomic.PHARMACY_DISPOSAL_ISSUE_RETURN
```

Historical bills (created before the update that introduced `PHARMACY_DISPOSAL_ISSUE`) were saved
with `BILLTYPEATOMIC = 'PHARMACY_ISSUE'` and `BILLTYPE = 'PharmacyIssue'`. When the enum value was
renamed/split out, no migration updated the existing rows, so those bills no longer match the query.

> **Important:** Bills with `BILLTYPE = 'PharmacyTransferIssue'` also have
> `BILLTYPEATOMIC = 'PHARMACY_ISSUE'` — these are inter-pharmacy transfers and must **not** be
> updated. The WHERE clause must always include `BILLTYPE = 'PharmacyIssue'`.

### Diagnosis Query

```sql
-- Check stale BILLTYPEATOMIC values for the affected period
SELECT BILLTYPE, BILLTYPEATOMIC, COUNT(*) AS cnt,
       MIN(CREATEDAT) AS earliest, MAX(CREATEDAT) AS latest
FROM bill
WHERE BILLTYPE IN ('PharmacyIssue', 'PharmacyDisposalIssue')
  AND RETIRED = 0
  AND CREATEDAT >= '2026-03-01 00:00:00'
  AND CREATEDAT <  '2026-05-26 00:00:00'
GROUP BY BILLTYPE, BILLTYPEATOMIC
ORDER BY BILLTYPE, cnt DESC;
```

Rows where `BILLTYPE = 'PharmacyIssue'` and `BILLTYPEATOMIC = 'PHARMACY_ISSUE'` or
`'PHARMACY_ISSUE_RETURN'` need to be migrated.

### Migration SQL

```sql
-- Preview counts before updating
SELECT BILLTYPEATOMIC, COUNT(*) AS cnt
FROM bill
WHERE BILLTYPE      = 'PharmacyIssue'
  AND BILLTYPEATOMIC IN ('PHARMACY_ISSUE', 'PHARMACY_ISSUE_RETURN')
  AND RETIRED       = 0
  AND CREATEDAT >= '2026-03-01 00:00:00'   -- adjust as needed
  AND CREATEDAT <  '2026-05-26 00:00:00'   -- adjust as needed
GROUP BY BILLTYPEATOMIC;

-- Migrate disposal issue bills
UPDATE bill
SET BILLTYPEATOMIC = 'PHARMACY_DISPOSAL_ISSUE'
WHERE BILLTYPE      = 'PharmacyIssue'
  AND BILLTYPEATOMIC = 'PHARMACY_ISSUE'
  AND RETIRED       = 0
  AND CREATEDAT >= '2026-03-01 00:00:00'
  AND CREATEDAT <  '2026-05-26 00:00:00';

SELECT ROW_COUNT() AS updated_disposal_issue;

-- Migrate disposal issue return bills
UPDATE bill
SET BILLTYPEATOMIC = 'PHARMACY_DISPOSAL_ISSUE_RETURN'
WHERE BILLTYPE      = 'PharmacyIssue'
  AND BILLTYPEATOMIC = 'PHARMACY_ISSUE_RETURN'
  AND RETIRED       = 0
  AND CREATEDAT >= '2026-03-01 00:00:00'
  AND CREATEDAT <  '2026-05-26 00:00:00';

SELECT ROW_COUNT() AS updated_disposal_issue_return;
```

### Verify

```sql
SELECT BILLTYPEATOMIC, COUNT(*) AS cnt, MIN(CREATEDAT) AS earliest, MAX(CREATEDAT) AS latest
FROM bill
WHERE BILLTYPEATOMIC IN (
        'PHARMACY_DISPOSAL_ISSUE',
        'PHARMACY_DISPOSAL_ISSUE_RETURN',
        'PHARMACY_DISPOSAL_ISSUE_CANCELLED'
      )
  AND RETIRED = 0
  AND CREATEDAT >= '2026-03-01 00:00:00'
  AND CREATEDAT <  '2026-05-26 00:00:00'
GROUP BY BILLTYPEATOMIC;
```

The report should immediately show historical data after the UPDATE — no server restart needed.

---

## How to Connect to a Target Database

See `developer_docs/database/mysql-developer-guide.md` for connection credentials and access
instructions for each deployment.

---

## Related Files

| File | Role |
|---|---|
| `ReportsTransfer.java` | `fillDepartmentBHTIssueByBillByDTOs()` — BHT report query (implicit join bug) |
| `ReportsTransfer.java` | `fillDepartmentUnitIssueByBill()` — Unit issue report query (BILLTYPEATOMIC filter) |
| `BillFinanceDetails.java` | Entity — `BILLFINANCEDETAILS_ID` FK lives on `bill` table |
| `BillService.java` | `createBillFinancialDetailsForInpatientDirectIssueBill()` — Java equivalent of SQL backfill |
| `DataAdministrationController.java` | `correctInpatientDirectIssueBillFinanceDetails()` — Admin UI backfill button |
| `admin_functions.xhtml` | "Correct Finance Details for Inpatient Direct Issue Bills" button |
| `developer_docs/pharmacy/f15-bfd-backfill-guide.md` | BFD backfill guide for F15 report (other bill types) |

---

## History

| Date | Action | Deployment | Bills affected |
|---|---|---|---|
| 2026-05-25 | BFD rows inserted for `PharmacyBhtPre` bills (Mar–May 2026) | First affected site | 797 |
| 2026-05-25 | `BILLTYPEATOMIC` migrated for `PharmacyIssue` bills (Mar–May 2026) | First affected site | 507 |
