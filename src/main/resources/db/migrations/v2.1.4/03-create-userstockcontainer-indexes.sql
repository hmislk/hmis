-- ==========================================
-- SCRIPT 3: CREATE USERSTOCKCONTAINER INDEXES
-- ==========================================
-- Purpose: Create indexes on USERSTOCKCONTAINER for join optimization
-- Safe to run: YES (uses IF NOT EXISTS)
-- Can be run separately: YES
-- Idempotent: YES (safe to re-run)

SELECT 'Creating USERSTOCKCONTAINER Indexes - Script 3 of 5' AS status;
SELECT NOW() AS script_start_time;

-- The isStockAvailable() query also filters by userStockContainer.retired
-- This requires efficient filtering on the USERSTOCKCONTAINER table
-- With 587,593 retired containers vs only 10 active, these indexes are critical!

-- Check if USERSTOCKCONTAINER (uppercase) table exists
SET @usc_table_exists_upper = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'USERSTOCKCONTAINER'
);

-- Check if userstockcontainer (lowercase) table exists
SET @usc_table_exists_lower = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'userstockcontainer'
);

SELECT
    CASE WHEN @usc_table_exists_upper > 0 OR @usc_table_exists_lower > 0
         THEN 'UserStockContainer table found - proceeding with index creation'
         ELSE 'WARNING: UserStockContainer table not found'
    END AS usc_table_check;

-- Create single column index on RETIRED for uppercase table
CREATE INDEX IF NOT EXISTS idx_userstockcontainer_retired
ON USERSTOCKCONTAINER(RETIRED);

SELECT 'Index idx_userstockcontainer_retired created on USERSTOCKCONTAINER (if table exists)' AS uppercase_usc_result;

-- Create single column index on RETIRED for lowercase table
CREATE INDEX IF NOT EXISTS idx_userstockcontainer_retired
ON userstockcontainer(RETIRED);

SELECT 'Index idx_userstockcontainer_retired created on userstockcontainer (if table exists)' AS lowercase_usc_result;

-- Create composite index for potential future optimizations on uppercase table
CREATE INDEX IF NOT EXISTS idx_userstockcontainer_retired_created
ON USERSTOCKCONTAINER(RETIRED, CREATEDAT, CREATER_ID);

SELECT 'Index idx_userstockcontainer_retired_created created on USERSTOCKCONTAINER (if table exists)' AS uppercase_usc_composite_result;

-- Create composite index for potential future optimizations on lowercase table
CREATE INDEX IF NOT EXISTS idx_userstockcontainer_retired_created
ON userstockcontainer(RETIRED, CREATEDAT, CREATER_ID);

SELECT 'Index idx_userstockcontainer_retired_created created on userstockcontainer (if table exists)' AS lowercase_usc_composite_result;

-- Create optimized composite index for retiredAllUserStockContainer query on uppercase table
-- This index optimizes the query: WHERE CREATER_ID = :userId AND RETIRED = 0
-- Column order: CREATER_ID first (most selective), then RETIRED
CREATE INDEX IF NOT EXISTS idx_userstockcontainer_creater_retired
ON USERSTOCKCONTAINER(CREATER_ID, RETIRED);

SELECT 'Index idx_userstockcontainer_creater_retired created on USERSTOCKCONTAINER (if table exists)' AS uppercase_usc_creater_retired_result;

-- Create optimized composite index for retiredAllUserStockContainer query on lowercase table
CREATE INDEX IF NOT EXISTS idx_userstockcontainer_creater_retired
ON userstockcontainer(CREATER_ID, RETIRED);

SELECT 'Index idx_userstockcontainer_creater_retired created on userstockcontainer (if table exists)' AS lowercase_usc_creater_retired_result;

-- ==========================================
-- VERIFICATION
-- ==========================================

SELECT 'Verifying USERSTOCKCONTAINER indexes...' AS progress;

-- Verify indexes were created successfully
SET @usc_indexes_created = (
    SELECT COUNT(DISTINCT INDEX_NAME)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND (TABLE_NAME = 'USERSTOCKCONTAINER' OR TABLE_NAME = 'userstockcontainer')
      AND INDEX_NAME IN ('idx_userstockcontainer_retired', 'idx_userstockcontainer_retired_created', 'idx_userstockcontainer_creater_retired')
);

SELECT
    CASE WHEN @usc_indexes_created >= 3
         THEN CONCAT('✓ SUCCESS: ', @usc_indexes_created, ' USERSTOCKCONTAINER indexes created')
         ELSE CONCAT('⚠️ WARNING: Only ', @usc_indexes_created, ' of 3 expected indexes created')
    END AS usc_index_verification;

-- Show index details for USERSTOCKCONTAINER
SELECT 'USERSTOCKCONTAINER indexes created:' AS status;
SELECT DISTINCT INDEX_NAME, INDEX_TYPE, NON_UNIQUE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME = 'USERSTOCKCONTAINER' OR TABLE_NAME = 'userstockcontainer')
  AND INDEX_NAME LIKE 'idx_userstockcontainer%'
ORDER BY INDEX_NAME;

SELECT 'USERSTOCKCONTAINER Index Creation Completed' AS status;
SELECT NOW() AS script_end_time;
