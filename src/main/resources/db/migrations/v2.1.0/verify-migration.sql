-- Verification Script for Migration v2.1.0
-- Purpose: Verify consumption_allowed migration completed successfully
-- Run this after executing migration.sql

-- 1. Check column exists and has correct properties
SELECT
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'item'
  AND COLUMN_NAME = 'consumption_allowed';

-- Expected result:
-- COLUMN_NAME: consumption_allowed
-- DATA_TYPE: tinyint(1) or boolean (depending on MySQL version)
-- IS_NULLABLE: NO
-- COLUMN_DEFAULT: 1 (TRUE)

-- 2. Count items by consumption_allowed status
SELECT
    COUNT(*) as total_items,
    SUM(CASE WHEN consumption_allowed IS NULL THEN 1 ELSE 0 END) as null_count,
    SUM(CASE WHEN consumption_allowed = TRUE THEN 1 ELSE 0 END) as allowed_count,
    SUM(CASE WHEN consumption_allowed = FALSE THEN 1 ELSE 0 END) as blocked_count
FROM item;

-- Expected results:
-- total_items: [total number of items in system]
-- null_count: 0 (no NULL values should exist)
-- allowed_count: [should equal total_items initially]
-- blocked_count: 0 (no items blocked initially)

-- 3. Test default value works for new inserts
-- (This is a test query - don't run in production)
-- INSERT INTO item (name, code, retired, createdAt, creater_id)
-- VALUES ('Test Item', 'TEST001', 0, NOW(), 1);
-- SELECT consumption_allowed FROM item WHERE code = 'TEST001';
-- DELETE FROM item WHERE code = 'TEST001';

-- 4. Verify constraint prevents NULL values
-- (This should fail - don't run in production)
-- UPDATE item SET consumption_allowed = NULL WHERE id = 1;

-- Migration Success Indicators:
-- ✓ Column exists with correct data type
-- ✓ Column is NOT NULL
-- ✓ Column has DEFAULT TRUE
-- ✓ All existing records have consumption_allowed = TRUE
-- ✓ No NULL values exist
-- ✓ New records automatically get TRUE value