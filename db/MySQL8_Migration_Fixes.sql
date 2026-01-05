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

-- Step 1: Create backup column
ALTER TABLE BILL ADD COLUMN APPROVEAT_BACKUP TIMESTAMP NULL;

-- Step 2: Copy existing data to backup
UPDATE BILL SET APPROVEAT_BACKUP = APPROVEAT;

-- Step 3: Drop the problematic column
ALTER TABLE BILL DROP COLUMN APPROVEAT;

-- Step 4: Recreate column allowing NULL values
ALTER TABLE BILL ADD COLUMN APPROVEAT DATETIME NULL;

-- Step 5: Restore data from backup
UPDATE BILL SET APPROVEAT = APPROVEAT_BACKUP;

-- Step 6: Verify data restored correctly
SELECT COUNT(*) AS total,
       SUM(CASE WHEN APPROVEAT IS NOT NULL THEN 1 ELSE 0 END) AS with_data,
       SUM(CASE WHEN APPROVEAT IS NULL THEN 1 ELSE 0 END) AS null_values
FROM BILL;

-- Step 7: Drop backup column (only after verification)
ALTER TABLE BILL DROP COLUMN APPROVEAT_BACKUP;

-- ============================================
-- FIX 2: PAYMENT.PAYMENTMETHOD Column (Enum INT to String)
-- ============================================
-- Issue: Incorrect integer value: 'Cash' for column 'PAYMENTMETHOD' (Error Code: 1366)
-- Cause: Java enum changed from numeric ordinal to string values, but column still INT
-- Solution: Change column from INT to VARCHAR(255)
-- Note: PAYMENT table was empty, so no data migration needed
-- ============================================

-- Simple change (if table is empty or you want to lose existing data)
ALTER TABLE PAYMENT MODIFY COLUMN PAYMENTMETHOD VARCHAR(255) NULL;

-- ============================================
-- FIX 3: [Next Issue]
-- ============================================
-- Issue:
-- Cause:
-- Solution:
-- ============================================
