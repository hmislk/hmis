-- Migration v2.1.2: Fix remaining decimal precision columns in BILLFINANCEDETAILS
-- Author: Dr M H B Ariyaratne
-- Date: 2025-01-23
-- Issue: v2.1.1 migration was marked as successful but missed 5 columns
-- This migration completes the decimal precision fix for production environments

-- PRODUCTION SAFETY: Check table exists before proceeding
SELECT 'Starting migration v2.1.2 - fixing remaining decimal precision columns' AS status;

-- Verify the table exists
SELECT COUNT(*) as table_exists FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BILLFINANCEDETAILS';

-- Show current problematic columns before fix
SELECT 'BEFORE: Columns with decimal(38,0) precision:' AS status;
SELECT COLUMN_NAME, COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_TYPE = 'decimal(38,0)'
ORDER BY COLUMN_NAME;

-- ==========================================
-- FIX REMAINING COLUMNS
-- ==========================================

-- Fix the 5 columns that were missed in v2.1.1
ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALEXPENSE DECIMAL(18,4);
SELECT 'Fixed TOTALEXPENSE column' AS progress;

ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALOFBILLLINEDISCOUNTS DECIMAL(18,4);
SELECT 'Fixed TOTALOFBILLLINEDISCOUNTS column' AS progress;

ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALOFFREEITEMVALUES DECIMAL(18,4);
SELECT 'Fixed TOTALOFFREEITEMVALUES column' AS progress;

ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALPAIDASCASH DECIMAL(18,4);
SELECT 'Fixed TOTALPAIDASCASH column' AS progress;

ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALTAXVALUE DECIMAL(18,4);
SELECT 'Fixed TOTALTAXVALUE column' AS progress;

-- ==========================================
-- VERIFICATION
-- ==========================================

-- Verify no decimal(38,0) columns remain
SELECT 'AFTER: Remaining columns with decimal(38,0) precision (should be empty):' AS status;
SELECT COLUMN_NAME, COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_TYPE = 'decimal(38,0)'
ORDER BY COLUMN_NAME;

-- Count total decimal(18,4) columns (should be high)
SELECT CONCAT('Total decimal(18,4) columns: ', COUNT(*)) AS summary
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_TYPE = 'decimal(18,4)';

-- Show key financial columns to confirm they're all decimal(18,4)
SELECT 'Key financial columns verification:' AS status;
SELECT COLUMN_NAME, COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_NAME IN (
    'TOTALCOSTVALUE', 'TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE',
    'BILLDISCOUNT', 'TOTALDISCOUNT', 'TOTALEXPENSE', 'TOTALTAXVALUE',
    'TOTALQUANTITY', 'TOTALPAIDASCASH', 'TOTALOFFREEITEMVALUES'
  )
ORDER BY COLUMN_NAME;

SELECT 'Migration v2.1.2 completed successfully - all decimal precision issues fixed for production' AS final_status;