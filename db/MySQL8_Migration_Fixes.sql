-- ============================================
-- MySQL 5 to MySQL 8 Migration Fixes
-- Database: roseth (Production)
-- ============================================
--
-- INSTRUCTIONS:
-- 1. Backup the database before running these scripts
-- 2. Run each section one at a time
-- 3. Verify data after each fix
-- 4. Document any issues encountered
--
-- ============================================

-- ============================================
-- FIX 1: BILL.APPROVEAT Column
-- ============================================
-- Issue: Column 'APPROVEAT' cannot be null (Error Code: 1048)
-- Cause: MySQL 8 strict mode rejects NULL for NOT NULL columns
-- Solution: Change column to allow NULL values
-- ============================================

-- Step 1: Create backup column (use DATETIME to match target column type)
ALTER TABLE BILL ADD COLUMN APPROVEAT_BACKUP DATETIME NULL;

-- Step 2: Copy existing data to backup
UPDATE BILL SET APPROVEAT_BACKUP = APPROVEAT;

-- Step 3: Drop the problematic column
ALTER TABLE BILL DROP COLUMN APPROVEAT;

-- Step 4: Recreate column allowing NULL values
ALTER TABLE BILL ADD COLUMN APPROVEAT DATETIME NULL;

-- Step 5: Restore data from backup
UPDATE BILL SET APPROVEAT = APPROVEAT_BACKUP;

-- Step 6: Verify data restored correctly by comparing against backup
-- 6a: Summary comparison - check for any mismatches
SELECT
    COUNT(*) AS total_rows,
    SUM(CASE WHEN APPROVEAT IS NOT NULL AND APPROVEAT_BACKUP IS NOT NULL AND APPROVEAT = APPROVEAT_BACKUP THEN 1 ELSE 0 END) AS matched_non_null,
    SUM(CASE WHEN APPROVEAT IS NULL AND APPROVEAT_BACKUP IS NULL THEN 1 ELSE 0 END) AS matched_null,
    SUM(CASE WHEN APPROVEAT IS NOT NULL AND APPROVEAT_BACKUP IS NOT NULL AND APPROVEAT <> APPROVEAT_BACKUP THEN 1 ELSE 0 END) AS value_mismatch,
    SUM(CASE WHEN APPROVEAT IS NULL AND APPROVEAT_BACKUP IS NOT NULL THEN 1 ELSE 0 END) AS restored_null_but_backup_has_value,
    SUM(CASE WHEN APPROVEAT IS NOT NULL AND APPROVEAT_BACKUP IS NULL THEN 1 ELSE 0 END) AS restored_has_value_but_backup_null,
    SUM(CASE
        WHEN (APPROVEAT IS NULL AND APPROVEAT_BACKUP IS NOT NULL)
          OR (APPROVEAT IS NOT NULL AND APPROVEAT_BACKUP IS NULL)
          OR (APPROVEAT IS NOT NULL AND APPROVEAT_BACKUP IS NOT NULL AND APPROVEAT <> APPROVEAT_BACKUP)
        THEN 1 ELSE 0
    END) AS total_mismatches
FROM BILL;

-- 6b: Sample mismatched rows for inspection (if any exist)
SELECT ID, APPROVEAT, APPROVEAT_BACKUP,
    CASE
        WHEN APPROVEAT IS NOT NULL AND APPROVEAT_BACKUP IS NOT NULL AND APPROVEAT <> APPROVEAT_BACKUP THEN 'VALUE_DIFFERS'
        WHEN APPROVEAT IS NULL AND APPROVEAT_BACKUP IS NOT NULL THEN 'RESTORED_IS_NULL'
        WHEN APPROVEAT IS NOT NULL AND APPROVEAT_BACKUP IS NULL THEN 'BACKUP_IS_NULL'
    END AS mismatch_type
FROM BILL
WHERE (APPROVEAT IS NULL AND APPROVEAT_BACKUP IS NOT NULL)
   OR (APPROVEAT IS NOT NULL AND APPROVEAT_BACKUP IS NULL)
   OR (APPROVEAT IS NOT NULL AND APPROVEAT_BACKUP IS NOT NULL AND APPROVEAT <> APPROVEAT_BACKUP)
LIMIT 10;

-- Step 7: Drop backup column ONLY if total_mismatches = 0 from Step 6a
-- WARNING: Do NOT run this until you have verified mismatches = 0
ALTER TABLE BILL DROP COLUMN APPROVEAT_BACKUP;

-- ============================================
-- FIX 2: PAYMENT.PAYMENTMETHOD Column (Enum INT to String)
-- ============================================
-- Issue: Incorrect integer value: 'Cash' for column 'PAYMENTMETHOD' (Error Code: 1366)
-- Cause: Java enum changed from numeric ordinal to string values, but column still INT
-- Solution: Change column from INT to VARCHAR(255)
-- Note: PAYMENT table was empty, so no data migration needed
-- ============================================

-- PAYMENT table was verified empty before migration - safe to modify column type directly
ALTER TABLE PAYMENT MODIFY COLUMN PAYMENTMETHOD VARCHAR(255) NULL;

-- ============================================
-- FIX 3: [Next Issue]
-- ============================================
-- Issue:
-- Cause:
-- Solution:
-- ============================================
