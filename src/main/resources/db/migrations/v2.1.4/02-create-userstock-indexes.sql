-- ==========================================
-- SCRIPT 2: CREATE USER_STOCK INDEXES
-- ==========================================
-- Purpose: Create composite index on USER_STOCK for performance optimization
-- Safe to run: YES (uses IF NOT EXISTS)
-- Can be run separately: YES
-- Idempotent: YES (safe to re-run)

SELECT 'Creating USER_STOCK Indexes - Script 2 of 5' AS status;
SELECT NOW() AS script_start_time;

-- ==========================================
-- STEP 1: CHECK IF INDEX ALREADY EXISTS
-- ==========================================

SELECT 'Step 1: Checking if index already exists...' AS progress;

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
         THEN 'Index idx_user_stock_fast_lookup already exists - skipping creation'
         ELSE 'Index does not exist - will create'
    END AS index_check_result;

-- ==========================================
-- STEP 2: CREATE COMPOSITE INDEX FOR PERFORMANCE
-- ==========================================

SELECT 'Step 2: Creating composite index for query optimization...' AS progress;

-- Try USER_STOCK (uppercase - Ubuntu production servers)
SET @table_exists_upper = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'USER_STOCK'
);

-- Try userstock (lowercase - Windows development environment)
SET @table_exists_lower = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'userstock'
);

-- Create index for uppercase table if it exists
SELECT
    CASE WHEN @table_exists_upper > 0 AND @index_exists = 0
         THEN 'Creating index on USER_STOCK (uppercase)...'
         WHEN @table_exists_upper > 0 AND @index_exists > 0
         THEN 'Index already exists on USER_STOCK - skipping'
         ELSE 'USER_STOCK table not found (uppercase) - trying lowercase'
    END AS uppercase_table_status;

-- Create index if table exists and index doesn't
CREATE INDEX IF NOT EXISTS idx_user_stock_fast_lookup
ON USER_STOCK(STOCK_ID, RETIRED, CREATEDAT, CREATER_ID);

SELECT 'Index created successfully on USER_STOCK (if table exists)' AS uppercase_index_status;

-- Create index for lowercase table if it exists
SELECT
    CASE WHEN @table_exists_lower > 0 AND @index_exists = 0
         THEN 'Creating index on userstock (lowercase)...'
         WHEN @table_exists_lower > 0 AND @index_exists > 0
         THEN 'Index already exists on userstock - skipping'
         ELSE 'userstock table not found (lowercase)'
    END AS lowercase_table_status;

-- Create index if table exists and index doesn't
CREATE INDEX IF NOT EXISTS idx_user_stock_fast_lookup
ON userstock(STOCK_ID, RETIRED, CREATEDAT, CREATER_ID);

SELECT 'Index created successfully on userstock (if table exists)' AS lowercase_index_status;

-- ==========================================
-- STEP 3: VERIFICATION
-- ==========================================

SELECT 'Step 3: Running verification checks...' AS progress;

-- Verify index was created successfully
SELECT 'Verifying index creation...' AS status;

SET @index_created = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND (TABLE_NAME = 'USER_STOCK' OR TABLE_NAME = 'userstock')
      AND INDEX_NAME = 'idx_user_stock_fast_lookup'
);

-- Show verification result
SELECT
    CASE WHEN @index_created > 0
         THEN CONCAT('✓ SUCCESS: Index idx_user_stock_fast_lookup created (columns: ', @index_created, ')')
         ELSE '⚠️ WARNING: Index not created - check table name case sensitivity'
    END AS index_creation_check;

-- Show index details
SELECT 'Index details:' AS status;
SELECT
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX,
    COLLATION,
    CARDINALITY,
    INDEX_TYPE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME = 'USER_STOCK' OR TABLE_NAME = 'userstock')
  AND INDEX_NAME = 'idx_user_stock_fast_lookup'
ORDER BY SEQ_IN_INDEX;

-- Verify all columns are included
SET @index_columns_count = (
    SELECT COUNT(DISTINCT COLUMN_NAME)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND (TABLE_NAME = 'USER_STOCK' OR TABLE_NAME = 'userstock')
      AND INDEX_NAME = 'idx_user_stock_fast_lookup'
);

SET @expected_columns = 4;

SELECT
    CASE WHEN @index_columns_count = @expected_columns
         THEN CONCAT('✓ SUCCESS: All ', @expected_columns, ' columns included in index')
         ELSE CONCAT('⚠️ WARNING: Only ', @index_columns_count, ' of ', @expected_columns, ' columns in index')
    END AS column_count_check;

SELECT 'USER_STOCK Index Creation Completed' AS status;
SELECT NOW() AS script_end_time;
