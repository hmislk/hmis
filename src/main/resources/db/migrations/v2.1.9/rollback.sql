-- =====================================================
-- ROLLBACK for Migration v2.1.9: Performance Indexes
-- This script safely removes all performance indexes created in v2.1.9
-- SAFE TO EXECUTE: No data loss, only removes performance optimizations
-- =====================================================

-- Verify current database
SELECT DATABASE() AS current_database;

-- Show current indexes before rollback
SELECT 'Current indexes before rollback:' AS info;
SELECT
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS Index_Columns
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME LIKE 'idx_%'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

-- =====================================================
-- ROLLBACK: DROP PERFORMANCE INDEXES
-- =====================================================

-- Drop Index 5: Payment query optimization
DROP INDEX idx_payment_bill_paymentmethod ON PAYMENT;
SELECT 'Dropped payment index' AS rollback_step;

-- Drop Index 4: Bill query optimization
DROP INDEX idx_bill_createdat_billtype_dept_retired ON BILL;
SELECT 'Dropped bill index' AS rollback_step;

-- Drop Index 3: Batch-level stock queries
DROP INDEX idx_stockhistory_batch_date_retired ON STOCKHISTORY;
SELECT 'Dropped batch stock index' AS rollback_step;

-- Drop Index 2: Opening/closing stock queries
DROP INDEX idx_stockhistory_date_dept_retired ON STOCKHISTORY;
SELECT 'Dropped date stock index' AS rollback_step;

-- Drop Index 1: Stock value calculation performance (most critical)
DROP INDEX idx_stockhistory_dept_batch_date_retired ON STOCKHISTORY;
SELECT 'Dropped critical stock calculation index' AS rollback_step;

-- =====================================================
-- ROLLBACK VERIFICATION
-- =====================================================

-- Verify all indexes have been removed
SELECT 'Verification: Remaining v2.1.9 indexes (should be 0):' AS info;
SELECT
    COUNT(*) AS remaining_v219_indexes
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
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS Index_Columns
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('STOCKHISTORY', 'BILL', 'PAYMENT')
  AND INDEX_NAME != 'PRIMARY'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

-- =====================================================
-- ROLLBACK COMPLETE
-- =====================================================

SELECT 'ROLLBACK v2.1.9 completed successfully!' AS final_status;
SELECT 'WARNING: Daily Stock Report will now take 30-60 seconds again.' AS performance_warning;
SELECT 'To restore performance, re-run migration v2.1.9' AS restore_note;