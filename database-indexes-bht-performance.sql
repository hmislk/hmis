-- ============================================================
-- Database Index Optimization for BHT Interim Bill Performance
-- Southern Lanka Production Database
-- ============================================================
--
-- Purpose: Fix slow queries in inward_bill_intrim.xhtml
--
-- Problem Analysis:
-- 1. createDepartmentBillItems: 9-30 seconds (22 items × 400ms each)
-- 2. createChargeItemTotals: 54 seconds (15-20 queries × 3 seconds each)
-- 3. Total page load: 180+ seconds
--
-- Root Cause: Missing indexes on frequently queried columns
--
-- Expected Impact: Reduce page load from 180s to under 10s
-- ============================================================

USE southernlankaprod;

-- Show current table sizes
SELECT
    TABLE_NAME,
    TABLE_ROWS,
    ROUND(((DATA_LENGTH + INDEX_LENGTH) / 1024 / 1024), 2) AS 'Size_MB'
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'southernlankaprod'
  AND TABLE_NAME IN ('bill', 'billitem')
ORDER BY TABLE_ROWS DESC;

-- ============================================================
-- CRITICAL INDEXES - Run these first
-- ============================================================

-- Index 1: bill.RETIRED (used in almost every query)
-- Impact: High - filters out retired records
ALTER TABLE bill
ADD INDEX idx_bill_retired (RETIRED);

-- Index 2: bill.BILLTYPE (used to filter InwardBill, PharmacyBhtPre, etc.)
-- Impact: High - very selective filter
ALTER TABLE bill
ADD INDEX idx_bill_billtype (BILLTYPE);

-- Index 3: bill.DTYPE (used for type() checks: BilledBill, CancelledBill, RefundBill)
-- Impact: High - discriminator column for inheritance
ALTER TABLE bill
ADD INDEX idx_bill_dtype (DTYPE);

-- Index 4: billitem.RETIRED (used in every billitem query)
-- Impact: High - filters out retired records
ALTER TABLE billitem
ADD INDEX idx_billitem_retired (RETIRED);

-- ============================================================
-- COMPOSITE INDEXES - Run these during off-hours/maintenance
-- These provide even better performance but take longer to create
-- ============================================================

-- Composite index for calBillItemCount queries (createDepartmentBillItems)
-- Covers: WHERE retired=false AND bill.patientEncounter IN :pe AND bill.billType=:btp AND bill.dtype=:cls
ALTER TABLE bill
ADD INDEX idx_bill_pe_bt_dtype_ret (PATIENTENCOUNTER_ID, BILLTYPE, DTYPE, RETIRED);

-- Composite index for billitem queries
-- Covers: WHERE retired=false AND item=:itm AND bill_id=:bid
ALTER TABLE billitem
ADD INDEX idx_billitem_ret_item_bill (RETIRED, ITEM_ID, BILL_ID);

-- ============================================================
-- VERIFICATION - Check that indexes were created
-- ============================================================

SHOW INDEX FROM bill WHERE Key_name LIKE 'idx_%';
SHOW INDEX FROM billitem WHERE Key_name LIKE 'idx_%';

-- ============================================================
-- TESTING - Run these queries to verify performance improvement
-- ============================================================

-- Test Query 1: Count bill items by type (used in createDepartmentBillItems)
-- Before: ~400ms per item, After: <10ms per item
EXPLAIN SELECT COUNT(bi.ID)
FROM billitem bi
INNER JOIN bill b ON bi.BILL_ID = b.ID
WHERE bi.RETIRED = 0
  AND b.BILLTYPE = 'InwardBill'
  AND b.DTYPE = 'BilledBill'
  AND b.RETIRED = 0
LIMIT 1;

-- Test Query 2: Sum charges by inward charge type (used in createChargeItemTotals)
-- Before: ~3000ms per type, After: <100ms per type
EXPLAIN SELECT SUM(bi.GROSSVALUE + bi.MARGINVALUE)
FROM billitem bi
INNER JOIN bill b ON bi.BILL_ID = b.ID
INNER JOIN item i ON bi.ITEM_ID = i.ID
WHERE bi.RETIRED = 0
  AND b.BILLTYPE = 'InwardBill'
  AND b.RETIRED = 0
LIMIT 1;

-- ============================================================
-- NOTES
-- ============================================================
--
-- 1. Run these indexes one at a time if the system is under load
-- 2. The ALGORITHM=INPLACE, LOCK=NONE option requires MySQL 5.6+ or 8.0+
-- 3. Index creation time estimate:
--    - Single column indexes: 2-5 minutes each on 280K-1.5M rows
--    - Composite indexes: 5-15 minutes each
-- 4. Monitor with: SHOW PROCESSLIST;
-- 5. If a query is blocked, check: SELECT * FROM sys.innodb_lock_waits;
--
-- ============================================================
