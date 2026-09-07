-- =====================================================
-- ROLLBACK for Migration v2.1.10: Performance Indexes
-- This script safely removes all performance indexes
-- SAFE TO EXECUTE: No data loss, only removes performance optimizations
-- =====================================================

-- Verify current database
SELECT DATABASE() AS current_database;

-- Show current performance indexes before rollback
SELECT 'Performance indexes before rollback:' AS info;
SELECT
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS Index_Columns
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME LIKE 'idx_%'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

-- =====================================================
-- ROLLBACK: DROP PERFORMANCE INDEXES (with error handling)
-- =====================================================

-- Drop Index 5: Payment query optimization
SELECT 'Dropping payment optimization index...' AS step;
DROP INDEX IF EXISTS idx_payment_bill_paymentmethod ON PAYMENT;

-- Drop Index 4: Bill query optimization
SELECT 'Dropping bill query optimization index...' AS step;
DROP INDEX IF EXISTS idx_bill_createdat_billtype_dept_retired ON BILL;

-- Drop Index 3: Batch-level stock queries
SELECT 'Dropping batch stock optimization index...' AS step;
DROP INDEX IF EXISTS idx_stockhistory_batch_date_retired ON STOCKHISTORY;

-- Drop Index 2: Opening/closing stock queries
SELECT 'Dropping date-based stock optimization index...' AS step;
DROP INDEX IF EXISTS idx_stockhistory_date_dept_retired ON STOCKHISTORY;

-- Drop Index 1: Stock value calculation performance (most critical)
SELECT 'Dropping critical stock calculation index...' AS step;
DROP INDEX IF EXISTS idx_stockhistory_dept_batch_date_retired ON STOCKHISTORY;

-- =====================================================
-- ROLLBACK VERIFICATION
-- =====================================================

-- Verify all indexes have been removed
SELECT 'Verification: Remaining performance indexes (should be 0):' AS info;
SELECT
    COUNT(DISTINCT INDEX_NAME) AS remaining_indexes,
    CASE
        WHEN COUNT(DISTINCT INDEX_NAME) = 0 THEN 'SUCCESS: All performance indexes removed'
        ELSE CONCAT('WARNING: ', COUNT(DISTINCT INDEX_NAME), ' indexes still exist')
    END AS rollback_status
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME IN (
    'idx_stockhistory_dept_batch_date_retired',
    'idx_stockhistory_date_dept_retired',
    'idx_stockhistory_batch_date_retired',
    'idx_bill_createdat_billtype_dept_retired',
    'idx_payment_bill_paymentmethod'
  );

-- Show remaining indexes on affected tables
SELECT 'Remaining indexes after rollback:' AS info;
SELECT
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ', ') AS Index_Columns
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('STOCKHISTORY', 'BILL', 'PAYMENT')
  AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

-- =====================================================
-- ROLLBACK COMPLETE
-- =====================================================

SELECT 'ROLLBACK v2.1.10 completed successfully!' AS final_status;
SELECT 'WARNING: Daily Stock Report will now take 30-60 seconds again.' AS performance_warning;
SELECT 'To restore performance, re-run migration v2.1.10' AS restore_note;