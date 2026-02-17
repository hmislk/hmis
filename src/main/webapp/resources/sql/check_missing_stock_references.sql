-- ============================================================================
-- DIAGNOSTIC SQL: Check for Missing Stock References
-- ============================================================================
-- Purpose: Identifies PharmaceuticalBillItems that reference non-existent Stock records
--
-- Background:
-- In some databases, Stock records are being deleted instead of marked as retired.
-- This causes StockHistory records to be hidden from Batch Bin Card reports when
-- the query tries to access batch information via pbItem.stock.itemBatch.batchNo
--
-- Fix Applied:
-- StockHistoryController.findBinCardDTOs() now uses pbItem.itemBatch.batchNo directly
-- instead of going through the Stock entity (which may be deleted).
--
-- Created: 2025-09-13
-- Related Issue: #15861 - Discrepancy in closing stock report
-- ============================================================================

-- ============================================================================
-- QUERY 1: Summary of Missing Stock References
-- ============================================================================
-- Shows total count of affected records across the entire database
SELECT
    COUNT(*) as TotalAffectedRecords,
    COUNT(DISTINCT sh.ID) as AffectedStockHistories,
    MIN(pbi.STOCK_ID) as MinMissingStockID,
    MAX(pbi.STOCK_ID) as MaxMissingStockID,
    MIN(sh.CREATEDAT) as EarliestAffectedDate,
    MAX(sh.CREATEDAT) as LatestAffectedDate
FROM STOCKHISTORY sh
JOIN PHARMACEUTICALBILLITEM pbi ON sh.PBITEM_ID = pbi.ID
LEFT JOIN STOCK s ON pbi.STOCK_ID = s.ID
WHERE pbi.STOCK_ID IS NOT NULL
  AND s.ID IS NULL;

-- ============================================================================
-- QUERY 2: Missing Stocks by Bill Type
-- ============================================================================
-- Breakdown showing which transaction types are most affected
SELECT
    b.BILLTYPE,
    b.BILLTYPEATOMIC,
    COUNT(*) as AffectedCount,
    MIN(sh.CREATEDAT) as EarliestDate,
    MAX(sh.CREATEDAT) as LatestDate
FROM STOCKHISTORY sh
JOIN PHARMACEUTICALBILLITEM pbi ON sh.PBITEM_ID = pbi.ID
LEFT JOIN STOCK s ON pbi.STOCK_ID = s.ID
JOIN BILLITEM bi ON pbi.BILLITEM_ID = bi.ID
JOIN BILL b ON bi.BILL_ID = b.ID
WHERE pbi.STOCK_ID IS NOT NULL
  AND s.ID IS NULL
GROUP BY b.BILLTYPE, b.BILLTYPEATOMIC
ORDER BY AffectedCount DESC;

-- ============================================================================
-- QUERY 3: Missing Stocks by Department
-- ============================================================================
-- Shows which departments are most affected
SELECT
    d.ID as DepartmentID,
    d.NAME as DepartmentName,
    i.NAME as InstitutionName,
    COUNT(*) as AffectedCount
FROM STOCKHISTORY sh
JOIN PHARMACEUTICALBILLITEM pbi ON sh.PBITEM_ID = pbi.ID
LEFT JOIN STOCK s ON pbi.STOCK_ID = s.ID
JOIN DEPARTMENT d ON sh.DEPARTMENT_ID = d.ID
LEFT JOIN INSTITUTION i ON d.INSTITUTION_ID = i.ID
WHERE pbi.STOCK_ID IS NOT NULL
  AND s.ID IS NULL
GROUP BY d.ID, d.NAME, i.NAME
ORDER BY AffectedCount DESC
LIMIT 20;

-- ============================================================================
-- QUERY 4: Missing Stocks by Date Range
-- ============================================================================
-- Shows when the problem is occurring most frequently
-- Adjust the date range as needed
SELECT
    DATE(sh.CREATEDAT) as TransactionDate,
    COUNT(*) as AffectedCount,
    COUNT(DISTINCT pbi.STOCK_ID) as UniqueStocksReferenced
FROM STOCKHISTORY sh
JOIN PHARMACEUTICALBILLITEM pbi ON sh.PBITEM_ID = pbi.ID
LEFT JOIN STOCK s ON pbi.STOCK_ID = s.ID
WHERE pbi.STOCK_ID IS NOT NULL
  AND s.ID IS NULL
  AND sh.CREATEDAT >= DATE_SUB(NOW(), INTERVAL 30 DAY)  -- Last 30 days
GROUP BY DATE(sh.CREATEDAT)
ORDER BY TransactionDate DESC;

-- ============================================================================
-- QUERY 5: Verify Data Integrity - ItemBatch Availability
-- ============================================================================
-- Checks if affected records have valid ItemBatch references
-- (This is why the fix works - we can use itemBatch directly)
SELECT
    'Total Affected Records' as Category,
    COUNT(*) as RecordCount
FROM STOCKHISTORY sh
JOIN PHARMACEUTICALBILLITEM pbi ON sh.PBITEM_ID = pbi.ID
LEFT JOIN STOCK s ON pbi.STOCK_ID = s.ID
WHERE pbi.STOCK_ID IS NOT NULL
  AND s.ID IS NULL

UNION ALL

SELECT
    'Records with Valid ItemBatch' as Category,
    COUNT(*) as RecordCount
FROM STOCKHISTORY sh
JOIN PHARMACEUTICALBILLITEM pbi ON sh.PBITEM_ID = pbi.ID
LEFT JOIN STOCK s ON pbi.STOCK_ID = s.ID
LEFT JOIN ITEMBATCH ib ON pbi.ITEMBATCH_ID = ib.ID
WHERE pbi.STOCK_ID IS NOT NULL
  AND s.ID IS NULL
  AND pbi.ITEMBATCH_ID IS NOT NULL
  AND ib.ID IS NOT NULL

UNION ALL

SELECT
    'Records WITHOUT Valid ItemBatch' as Category,
    COUNT(*) as RecordCount
FROM STOCKHISTORY sh
JOIN PHARMACEUTICALBILLITEM pbi ON sh.PBITEM_ID = pbi.ID
LEFT JOIN STOCK s ON pbi.STOCK_ID = s.ID
LEFT JOIN ITEMBATCH ib ON pbi.ITEMBATCH_ID = ib.ID
WHERE pbi.STOCK_ID IS NOT NULL
  AND s.ID IS NULL
  AND (pbi.ITEMBATCH_ID IS NULL OR ib.ID IS NULL);

-- ============================================================================
-- QUERY 6: Stock Table Status
-- ============================================================================
-- Shows current state of the Stock table
SELECT
    'Total Stock Records' as Metric,
    COUNT(*) as Value
FROM STOCK

UNION ALL

SELECT
    'Retired Stock Records' as Metric,
    COUNT(*) as Value
FROM STOCK
WHERE RETIRED = 1

UNION ALL

SELECT
    'Active Stock Records' as Metric,
    COUNT(*) as Value
FROM STOCK
WHERE RETIRED = 0 OR RETIRED IS NULL

UNION ALL

SELECT
    'Min Stock ID' as Metric,
    MIN(ID) as Value
FROM STOCK

UNION ALL

SELECT
    'Max Stock ID' as Metric,
    MAX(ID) as Value
FROM STOCK;

-- ============================================================================
-- QUERY 7: Sample of Affected Records
-- ============================================================================
-- Shows actual examples for investigation
-- Limited to 10 records for quick review
SELECT
    sh.ID as StockHistoryID,
    sh.CREATEDAT as TransactionDate,
    pbi.ID as PharmBillItemID,
    pbi.STOCK_ID as MissingStockID,
    pbi.ITEMBATCH_ID,
    ib.BATCHNO as BatchNumber,
    b.BILLTYPE,
    b.BILLTYPEATOMIC,
    d.NAME as Department,
    i.NAME as Item,
    sh.STOCKQTY
FROM STOCKHISTORY sh
JOIN PHARMACEUTICALBILLITEM pbi ON sh.PBITEM_ID = pbi.ID
LEFT JOIN STOCK s ON pbi.STOCK_ID = s.ID
LEFT JOIN ITEMBATCH ib ON pbi.ITEMBATCH_ID = ib.ID
JOIN BILLITEM bi ON pbi.BILLITEM_ID = bi.ID
JOIN BILL b ON bi.BILL_ID = b.ID
LEFT JOIN DEPARTMENT d ON sh.DEPARTMENT_ID = d.ID
LEFT JOIN ITEM i ON bi.ITEM_ID = i.ID
WHERE pbi.STOCK_ID IS NOT NULL
  AND s.ID IS NULL
ORDER BY sh.CREATEDAT DESC
LIMIT 10;

-- ============================================================================
-- USAGE NOTES:
-- ============================================================================
-- 1. Run these queries on any HMIS database to check for the issue
--    Example: mysql -u username -p -D database_name < check_missing_stock_references.sql
--
-- 2. If Query 1 shows affected records > 0, the fix is needed
--
-- 3. Query 5 should show that all affected records have valid ItemBatch
--    (this confirms the fix will work)
--
-- 4. If Query 5 shows records WITHOUT valid ItemBatch, those need
--    manual investigation as they cannot be fixed by the code change
--
-- Expected Results After Fix:
-- - All affected StockHistory records will appear in Batch Bin Card reports
-- - Batch numbers will be displayed correctly
-- - No data loss or errors
--
-- ============================================================================
