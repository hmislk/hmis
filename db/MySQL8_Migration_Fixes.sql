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
-- FIX 3: BILLITEM.FROMTIME 
-- ============================================
-- Issue: Column 'FROMTIME' cannot be null 
-- Cause: MySQL 8 strict mode rejects NULL for NOT NULL columns
-- Solution: Change columns to allow NULL values
-- ============================================

-- Step 1: Create backup columns
ALTER TABLE BILLITEM ADD COLUMN FROMTIME_BACKUP DATETIME NULL;

-- Step 2: Copy existing data to backup
UPDATE BILLITEM SET FROMTIME_BACKUP = FROMTIME;

-- Step 3: Drop the problematic columns
ALTER TABLE BILLITEM DROP COLUMN FROMTIME;

-- Step 4: Recreate columns allowing NULL values
ALTER TABLE BILLITEM ADD COLUMN FROMTIME DATETIME NULL;


-- Step 5: Restore data from backup
UPDATE BILLITEM SET FROMTIME = FROMTIME_BACKUP;

-- Step 6: Verify data restored correctly
-- 6a: Summary comparison for FROMTIME
SELECT
    COUNT(*) AS total_rows,
    SUM(CASE WHEN FROMTIME IS NOT NULL AND FROMTIME_BACKUP IS NOT NULL AND FROMTIME = FROMTIME_BACKUP THEN 1 ELSE 0 END) AS matched_non_null,
    SUM(CASE WHEN FROMTIME IS NULL AND FROMTIME_BACKUP IS NULL THEN 1 ELSE 0 END) AS matched_null,
    SUM(CASE WHEN FROMTIME IS NOT NULL AND FROMTIME_BACKUP IS NOT NULL AND FROMTIME <> FROMTIME_BACKUP THEN 1 ELSE 0 END) AS value_mismatch,
    SUM(CASE WHEN FROMTIME IS NULL AND FROMTIME_BACKUP IS NOT NULL THEN 1 ELSE 0 END) AS restored_null_but_backup_has_value,
    SUM(CASE WHEN FROMTIME IS NOT NULL AND FROMTIME_BACKUP IS NULL THEN 1 ELSE 0 END) AS restored_has_value_but_backup_null,
    SUM(CASE
        WHEN (FROMTIME IS NULL AND FROMTIME_BACKUP IS NOT NULL)
          OR (FROMTIME IS NOT NULL AND FROMTIME_BACKUP IS NULL)
          OR (FROMTIME IS NOT NULL AND FROMTIME_BACKUP IS NOT NULL AND FROMTIME <> FROMTIME_BACKUP)
        THEN 1 ELSE 0
    END) AS total_mismatches
FROM BILLITEM;



-- 6c: Sample mismatched rows for FROMTIME (if any exist)
SELECT ID, FROMTIME, FROMTIME_BACKUP,
    CASE
        WHEN FROMTIME IS NOT NULL AND FROMTIME_BACKUP IS NOT NULL AND FROMTIME <> FROMTIME_BACKUP THEN 'VALUE_DIFFERS'
        WHEN FROMTIME IS NULL AND FROMTIME_BACKUP IS NOT NULL THEN 'RESTORED_IS_NULL'
        WHEN FROMTIME IS NOT NULL AND FROMTIME_BACKUP IS NULL THEN 'BACKUP_IS_NULL'
    END AS mismatch_type
FROM BILLITEM
WHERE (FROMTIME IS NULL AND FROMTIME_BACKUP IS NOT NULL)
   OR (FROMTIME IS NOT NULL AND FROMTIME_BACKUP IS NULL)
   OR (FROMTIME IS NOT NULL AND FROMTIME_BACKUP IS NOT NULL AND FROMTIME <> FROMTIME_BACKUP)
LIMIT 10;

-- Step 7: Drop backup columns ONLY if total_mismatches = 0 from Steps 6a and 6b
-- WARNING: Do NOT run this until you have verified mismatches = 0 for BOTH columns
ALTER TABLE BILLITEM DROP COLUMN FROMTIME_BACKUP;

-- ============================================
-- FIX 4: BILLFEE.FEEAT Column
-- ============================================
-- Issue: Column 'FEEAT' cannot be null (Error Code: 1048)
-- Cause: MySQL 8 strict mode rejects NULL for NOT NULL columns
-- Solution: Change column to allow NULL values
-- ============================================

-- Step 1: Create backup column
ALTER TABLE BILLFEE ADD COLUMN FEEAT_BACKUP DATETIME NULL;

-- Step 2: Copy existing data to backup
UPDATE BILLFEE SET FEEAT_BACKUP = FEEAT;

-- Step 3: Drop the problematic column
ALTER TABLE BILLFEE DROP COLUMN FEEAT;

-- Step 4: Recreate column allowing NULL values
ALTER TABLE BILLFEE ADD COLUMN FEEAT DATETIME NULL;

-- Step 5: Restore data from backup
UPDATE BILLFEE SET FEEAT = FEEAT_BACKUP;

-- Step 6: Verify data restored correctly by comparing against backup
-- 6a: Summary comparison - check for any mismatches
SELECT
    COUNT(*) AS total_rows,
    SUM(CASE WHEN FEEAT IS NOT NULL AND FEEAT_BACKUP IS NOT NULL AND FEEAT = FEEAT_BACKUP THEN 1 ELSE 0 END) AS matched_non_null,
    SUM(CASE WHEN FEEAT IS NULL AND FEEAT_BACKUP IS NULL THEN 1 ELSE 0 END) AS matched_null,
    SUM(CASE WHEN FEEAT IS NOT NULL AND FEEAT_BACKUP IS NOT NULL AND FEEAT <> FEEAT_BACKUP THEN 1 ELSE 0 END) AS value_mismatch,
    SUM(CASE WHEN FEEAT IS NULL AND FEEAT_BACKUP IS NOT NULL THEN 1 ELSE 0 END) AS restored_null_but_backup_has_value,
    SUM(CASE WHEN FEEAT IS NOT NULL AND FEEAT_BACKUP IS NULL THEN 1 ELSE 0 END) AS restored_has_value_but_backup_null,
    SUM(CASE
        WHEN (FEEAT IS NULL AND FEEAT_BACKUP IS NOT NULL)
          OR (FEEAT IS NOT NULL AND FEEAT_BACKUP IS NULL)
          OR (FEEAT IS NOT NULL AND FEEAT_BACKUP IS NOT NULL AND FEEAT <> FEEAT_BACKUP)
        THEN 1 ELSE 0
    END) AS total_mismatches
FROM BILLFEE;

-- 6b: Sample mismatched rows for inspection (if any exist)
SELECT ID, FEEAT, FEEAT_BACKUP,
    CASE
        WHEN FEEAT IS NOT NULL AND FEEAT_BACKUP IS NOT NULL AND FEEAT <> FEEAT_BACKUP THEN 'VALUE_DIFFERS'
        WHEN FEEAT IS NULL AND FEEAT_BACKUP IS NOT NULL THEN 'RESTORED_IS_NULL'
        WHEN FEEAT IS NOT NULL AND FEEAT_BACKUP IS NULL THEN 'BACKUP_IS_NULL'
    END AS mismatch_type
FROM BILLFEE
WHERE (FEEAT IS NULL AND FEEAT_BACKUP IS NOT NULL)
   OR (FEEAT IS NOT NULL AND FEEAT_BACKUP IS NULL)
   OR (FEEAT IS NOT NULL AND FEEAT_BACKUP IS NOT NULL AND FEEAT <> FEEAT_BACKUP)
LIMIT 10;

-- Step 7: Drop backup column ONLY if total_mismatches = 0 from Step 6a
-- WARNING: Do NOT run this until you have verified mismatches = 0
ALTER TABLE BILLFEE DROP COLUMN FEEAT_BACKUP;

-- ============================================
