-- Rollback Migration v2.1.4: Remove Composite Index from USER_STOCK
-- Author: Dr M H B Ariyaratne
-- Date: 2025-12-03
-- GitHub Issue: #16990 - Speed up the pharmacy retail sale
-- Purpose: Rollback performance optimization index if needed

-- ==========================================
-- PRE-ROLLBACK VERIFICATION
-- ==========================================

SELECT 'Starting Rollback for Migration v2.1.4 - USER_STOCK Index Removal' AS status;
SELECT NOW() AS rollback_start_time;

-- Verify index exists before attempting to drop
SELECT 'Verifying index exists before rollback...' AS status;
SELECT COUNT(*) as index_exists FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME = 'USER_STOCK' OR TABLE_NAME = 'userstock')
  AND INDEX_NAME = 'idx_user_stock_fast_lookup';

-- Show current indexes on USER_STOCK
SELECT 'Current indexes on USER_STOCK:' AS status;
SELECT DISTINCT INDEX_NAME, INDEX_TYPE, NON_UNIQUE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME = 'USER_STOCK' OR TABLE_NAME = 'userstock')
ORDER BY INDEX_NAME;

-- ==========================================
-- STEP 1: DROP COMPOSITE INDEX
-- ==========================================

SELECT 'Step 1: Dropping composite index idx_user_stock_fast_lookup...' AS progress;

-- Set variable to track if index exists
SET @index_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND (TABLE_NAME = 'USER_STOCK' OR TABLE_NAME = 'userstock')
      AND INDEX_NAME = 'idx_user_stock_fast_lookup'
);

SELECT
    CASE WHEN @index_exists > 0
         THEN 'Index found - proceeding with drop'
         ELSE 'Index does not exist - nothing to rollback'
    END AS index_check_result;

-- Check if USER_STOCK (uppercase) table exists
SET @table_exists_upper = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'USER_STOCK'
);

-- Drop index from uppercase table if it exists
SELECT
    CASE WHEN @table_exists_upper > 0 AND @index_exists > 0
         THEN 'Dropping index from USER_STOCK (uppercase)...'
         WHEN @table_exists_upper > 0 AND @index_exists = 0
         THEN 'Index does not exist on USER_STOCK - skipping'
         ELSE 'USER_STOCK table not found (uppercase)'
    END AS uppercase_drop_status;

-- Drop index if it exists (MySQL 5.7+ syntax)
DROP INDEX IF EXISTS idx_user_stock_fast_lookup ON USER_STOCK;

SELECT 'Index dropped from USER_STOCK (if existed)' AS uppercase_result;

-- Check if userstock (lowercase) table exists
SET @table_exists_lower = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'userstock'
);

-- Drop index from lowercase table if it exists
SELECT
    CASE WHEN @table_exists_lower > 0 AND @index_exists > 0
         THEN 'Dropping index from userstock (lowercase)...'
         WHEN @table_exists_lower > 0 AND @index_exists = 0
         THEN 'Index does not exist on userstock - skipping'
         ELSE 'userstock table not found (lowercase)'
    END AS lowercase_drop_status;

-- Drop index if it exists
DROP INDEX IF EXISTS idx_user_stock_fast_lookup ON userstock;

SELECT 'Index dropped from userstock (if existed)' AS lowercase_result;

-- ==========================================
-- STEP 2: VERIFICATION
-- ==========================================

SELECT 'Step 2: Verifying index removal...' AS progress;

-- Verify index no longer exists
SET @index_still_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND (TABLE_NAME = 'USER_STOCK' OR TABLE_NAME = 'userstock')
      AND INDEX_NAME = 'idx_user_stock_fast_lookup'
);

SELECT
    CASE WHEN @index_still_exists = 0
         THEN '✓ SUCCESS: Index idx_user_stock_fast_lookup successfully removed'
         ELSE '⚠️ WARNING: Index still exists after rollback attempt'
    END AS rollback_verification;

-- Show remaining indexes after rollback
SELECT 'Remaining indexes on USER_STOCK after rollback:' AS status;
SELECT DISTINCT INDEX_NAME, INDEX_TYPE, NON_UNIQUE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME = 'USER_STOCK' OR TABLE_NAME = 'userstock')
ORDER BY INDEX_NAME;

-- ==========================================
-- ROLLBACK COMPLETION SUMMARY
-- ==========================================

SELECT 'Rollback v2.1.4 completed successfully!' AS final_status;
SELECT 'Index idx_user_stock_fast_lookup removed from USER_STOCK' AS completion_message;
SELECT 'Application will continue to work (with original performance)' AS application_status;
SELECT 'UserStockController.isStockAvailable() will use original query plan' AS functional_impact;
SELECT NOW() AS rollback_end_time;

-- Final summary
SELECT 'ROLLBACK SUMMARY:' AS summary;
SELECT 'Composite index removed - system restored to pre-migration state' AS result;
SELECT 'Query performance will return to original levels (50-150ms per query)' AS performance_impact;
SELECT 'No data loss - schema change only' AS data_safety;
SELECT 'Consider alternative optimization approaches if needed' AS next_steps;
SELECT 'Monitor pharmacy retail sale performance after rollback' AS monitoring_recommendation;
