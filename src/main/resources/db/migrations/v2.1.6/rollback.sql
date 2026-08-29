-- Rollback Script for Migration v2.1.6
-- Description: Remove REPORTLOG table from audit database
-- Author: Dr M H B Ariyaratne
-- Date: 2025-12-16
-- WARNING: This will delete all report generation audit logs!

-- ==========================================
-- PRE-ROLLBACK VERIFICATION
-- ==========================================

-- Step 1: Verify current database
SELECT DATABASE() AS current_database;

-- Step 2: Check if REPORTLOG table exists
SELECT COUNT(*) AS table_exists
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'reportlog';

-- Step 3: Check row count before deletion (for backup verification)
SELECT COUNT(*) AS rows_to_be_deleted
FROM reportlog;

-- ==========================================
-- BACKUP RECOMMENDATION
-- ==========================================
-- IMPORTANT: Before running this rollback, consider backing up the data:
-- mysqldump -u [user] -p rhaudit reportlog > reportlog_backup_$(date +%Y%m%d_%H%M%S).sql

-- ==========================================
-- ROLLBACK EXECUTION
-- ==========================================

-- Step 4: Drop REPORTLOG table
-- This will remove all indexes and data associated with the table
DROP TABLE IF EXISTS reportlog;

-- ==========================================
-- POST-ROLLBACK VERIFICATION
-- ==========================================

-- Step 5: Verify table was dropped
SELECT COUNT(*) AS table_exists_after_drop
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'reportlog';

-- ==========================================
-- ROLLBACK COMPLETE
-- ==========================================
-- Expected outcome:
-- - REPORTLOG table removed from audit database
-- - All report generation logs deleted
-- - All associated indexes removed
-- - Database returns to pre-migration state
-- NOTE: Application will fail with PersistenceException if it tries to log reports
-- ==========================================
