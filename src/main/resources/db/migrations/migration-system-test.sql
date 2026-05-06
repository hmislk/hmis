-- Test Script for Database Migration System
-- Purpose: Verify the migration system is working correctly
-- Run this manually to test the system before production use

-- 1. Check if DatabaseMigration table exists
DESCRIBE database_migration;

-- Expected: Table should exist with all required columns
-- If error: Migration system tables not created yet

-- 2. Check current migration records
SELECT * FROM database_migration ORDER BY version;

-- Expected: Should show executed migrations
-- If empty: No migrations have been executed yet

-- 3. Verify consumption_allowed column exists in item table
DESCRIBE item;

-- Expected: Should show consumption_allowed BOOLEAN NOT NULL DEFAULT TRUE
-- If missing: v2.1.0 migration not executed

-- 4. Check item data for consumption_allowed
SELECT
    COUNT(*) as total_items,
    SUM(CASE WHEN consumption_allowed = 1 THEN 1 ELSE 0 END) as allowed_items,
    SUM(CASE WHEN consumption_allowed = 0 THEN 1 ELSE 0 END) as blocked_items,
    SUM(CASE WHEN consumption_allowed IS NULL THEN 1 ELSE 0 END) as null_items
FROM item;

-- Expected:
-- - total_items: [number of items in system]
-- - allowed_items: should equal total_items initially
-- - blocked_items: 0 initially
-- - null_items: 0 (no NULL values should exist)

-- 5. Test configuration options exist
SELECT option_key, option_value, scope
FROM config_option
WHERE option_key LIKE '%Migration%' OR option_key LIKE '%Database%Version%'
ORDER BY option_key;

-- Expected: Should show migration-related configuration options

-- 6. Test migration version comparison
-- This is a manual test - insert a test migration record
-- (Only run this in development environment)
/*
INSERT INTO database_migration (version, description, filename, status, executed_at, executed_by_id, execution_time_ms)
VALUES ('v2.0.0', 'Test Migration', 'test.sql', 'SUCCESS', NOW(), 1, 500);

-- Verify it appears in order
SELECT version FROM database_migration ORDER BY version;

-- Clean up test record
DELETE FROM database_migration WHERE version = 'v2.0.0';
*/

-- Test Success Criteria:
-- ✓ database_migration table exists with correct structure
-- ✓ item table has consumption_allowed column with NOT NULL constraint
-- ✓ All existing items have consumption_allowed = TRUE
-- ✓ Configuration options are created for migration system
-- ✓ No NULL values in consumption_allowed column
-- ✓ Migration records can be inserted and queried correctly

-- Performance Check:
-- Check query performance for migration-related operations
EXPLAIN SELECT * FROM database_migration ORDER BY version;
EXPLAIN SELECT COUNT(*) FROM item WHERE consumption_allowed = 1;

-- Security Check:
-- Verify only authorized users can access migration management
-- (This requires application-level testing)

-- Integration Test:
-- Test that the application can start successfully with new entities
-- Test that AMP management UI shows consumption_allowed field
-- Test that pharmacy issue autocomplete uses new filtering method