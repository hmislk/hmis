-- ==========================================
-- SCRIPT 1: PRE-MIGRATION VERIFICATION
-- ==========================================
-- Purpose: Verify tables exist and show current structure before migration
-- Safe to run: YES (read-only queries)
-- Can be run separately: YES

SELECT 'Starting Pre-Migration Checks for v2.1.4' AS status;
SELECT NOW() AS check_start_time;

-- Verify USER_STOCK table exists
SELECT 'Verifying USER_STOCK table exists...' AS status;
SELECT COUNT(*) as table_exists FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME = 'USER_STOCK' OR TABLE_NAME = 'userstock');

-- Show current table structure
SELECT 'Current USER_STOCK table structure:' AS status;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME = 'USER_STOCK' OR TABLE_NAME = 'userstock')
ORDER BY ORDINAL_POSITION;

-- Show existing indexes (before migration)
SELECT 'Existing indexes on USER_STOCK:' AS status;
SELECT DISTINCT INDEX_NAME, INDEX_TYPE, NON_UNIQUE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME = 'USER_STOCK' OR TABLE_NAME = 'userstock')
ORDER BY INDEX_NAME;

-- Count total records to estimate impact
-- NOTE: UNION ALL across USER_STOCK and userstock cannot be used here because MySQL validates all
-- table references at parse time before any WHERE guard executes, causing error 1146 when the
-- table exists only under one casing (e.g. userstock on Linux, USER_STOCK on Windows).
-- Use INFORMATION_SCHEMA only — always present regardless of table casing.
SELECT 'Checking USER_STOCK/userstock table existence via INFORMATION_SCHEMA...' AS status;
SELECT CONCAT('USER_STOCK/userstock table variants found: ',
    COUNT(*)) AS table_variant_count
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('USER_STOCK', 'userstock');

-- Check USERSTOCKCONTAINER table
SELECT 'Verifying USERSTOCKCONTAINER table exists...' AS status;
SELECT COUNT(*) as table_exists FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME = 'USERSTOCKCONTAINER' OR TABLE_NAME = 'userstockcontainer');

-- Show existing USERSTOCKCONTAINER indexes
SELECT 'Existing indexes on USERSTOCKCONTAINER:' AS status;
SELECT DISTINCT INDEX_NAME, INDEX_TYPE, NON_UNIQUE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME = 'USERSTOCKCONTAINER' OR TABLE_NAME = 'userstockcontainer')
ORDER BY INDEX_NAME;

-- Check PRICEMATRIX table
SELECT 'Verifying PRICEMATRIX table exists...' AS status;
SELECT COUNT(*) as table_exists FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME = 'PRICEMATRIX' OR TABLE_NAME = 'pricematrix');

-- Show existing PRICEMATRIX indexes
SELECT 'Existing indexes on PRICEMATRIX:' AS status;
SELECT DISTINCT INDEX_NAME, INDEX_TYPE, NON_UNIQUE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME = 'PRICEMATRIX' OR TABLE_NAME = 'pricematrix')
ORDER BY INDEX_NAME;

SELECT 'Pre-Migration Checks Completed' AS status;
SELECT NOW() AS check_end_time;
