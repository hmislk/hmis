-- Migration v2.1.13: Fix TOTALCOSTVALUE decimal precision with proper validation
-- Author: Dr M H B Ariyaratne
-- Date: 2026-01-15
-- Issue: Previous migrations v2.1.1 and v2.1.2 reported success but didn't actually fix TOTALCOSTVALUE column
-- Root Cause: MySQL ALTER TABLE can succeed without making schema changes under certain conditions
-- This migration includes proper validation to ensure the schema actually changes

-- ==========================================
-- STEP 1: DIAGNOSTIC - CHECK CURRENT STATE
-- ==========================================

SELECT 'Migration v2.1.13 - Starting TOTALCOSTVALUE decimal precision fix with validation' AS status;

-- Show current problematic columns
SELECT 'BEFORE: Current column definitions' AS status;
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_NAME IN ('TOTALCOSTVALUE', 'BILLDISCOUNT', 'TOTALEXPENSE', 'TOTALQUANTITY')
ORDER BY COLUMN_NAME;

-- Check sample data to understand current values
SELECT 'Current sample data from BILLFINANCEDETAILS' AS status;
SELECT COUNT(*) as total_records,
       COUNT(TOTALCOSTVALUE) as records_with_cost,
       MIN(TOTALCOSTVALUE) as min_cost,
       MAX(TOTALCOSTVALUE) as max_cost,
       AVG(TOTALCOSTVALUE) as avg_cost
FROM BILLFINANCEDETAILS;

-- ==========================================
-- STEP 2: DATA VALIDATION AND PREPARATION
-- ==========================================

-- Check for any NULL or problematic data that might prevent conversion
SELECT 'Data validation check' AS status;
SELECT
    SUM(CASE WHEN TOTALCOSTVALUE IS NULL THEN 1 ELSE 0 END) as null_cost_values,
    SUM(CASE WHEN TOTALCOSTVALUE > 999999999999999 THEN 1 ELSE 0 END) as oversized_values,
    SUM(CASE WHEN TOTALCOSTVALUE < -999999999999999 THEN 1 ELSE 0 END) as undersized_values
FROM BILLFINANCEDETAILS;

-- Show distribution of cost values to understand precision needs
SELECT 'Cost value distribution analysis' AS status;
SELECT
    CASE
        WHEN TOTALCOSTVALUE = 0 THEN 'Zero values'
        WHEN TOTALCOSTVALUE > 0 AND TOTALCOSTVALUE < 1 THEN 'Fractional positive'
        WHEN TOTALCOSTVALUE > 0 AND TOTALCOSTVALUE >= 1 THEN 'Integer positive'
        WHEN TOTALCOSTVALUE < 0 AND TOTALCOSTVALUE > -1 THEN 'Fractional negative'
        WHEN TOTALCOSTVALUE < 0 AND TOTALCOSTVALUE <= -1 THEN 'Integer negative'
        ELSE 'Other'
    END as value_type,
    COUNT(*) as count
FROM BILLFINANCEDETAILS
WHERE TOTALCOSTVALUE IS NOT NULL
GROUP BY
    CASE
        WHEN TOTALCOSTVALUE = 0 THEN 'Zero values'
        WHEN TOTALCOSTVALUE > 0 AND TOTALCOSTVALUE < 1 THEN 'Fractional positive'
        WHEN TOTALCOSTVALUE > 0 AND TOTALCOSTVALUE >= 1 THEN 'Integer positive'
        WHEN TOTALCOSTVALUE < 0 AND TOTALCOSTVALUE > -1 THEN 'Fractional negative'
        WHEN TOTALCOSTVALUE < 0 AND TOTALCOSTVALUE <= -1 THEN 'Integer negative'
        ELSE 'Other'
    END;

-- ==========================================
-- STEP 3: CRITICAL COLUMNS FIRST (with validation)
-- ==========================================

-- Fix TOTALCOSTVALUE (this is the main issue from the stock transfer report)
SELECT 'Attempting to fix TOTALCOSTVALUE column' AS status;
ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALCOSTVALUE DECIMAL(18,4);

-- VALIDATION: Check if the change actually took effect
SELECT 'VALIDATION: Checking TOTALCOSTVALUE column change' AS status;
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    CASE
        WHEN COLUMN_TYPE = 'decimal(18,4)' THEN '✅ SUCCESS - Fixed to decimal(18,4)'
        WHEN COLUMN_TYPE = 'decimal(38,0)' THEN '❌ FAILED - Still decimal(38,0)'
        ELSE CONCAT('⚠️ UNEXPECTED - Now: ', COLUMN_TYPE)
    END as change_status
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_NAME = 'TOTALCOSTVALUE';

-- Fix BILLDISCOUNT
SELECT 'Attempting to fix BILLDISCOUNT column' AS status;
ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN BILLDISCOUNT DECIMAL(18,4);

-- VALIDATION: Check if the change actually took effect
SELECT 'VALIDATION: Checking BILLDISCOUNT column change' AS status;
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    CASE
        WHEN COLUMN_TYPE = 'decimal(18,4)' THEN '✅ SUCCESS - Fixed to decimal(18,4)'
        WHEN COLUMN_TYPE = 'decimal(38,0)' THEN '❌ FAILED - Still decimal(38,0)'
        ELSE CONCAT('⚠️ UNEXPECTED - Now: ', COLUMN_TYPE)
    END as change_status
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_NAME = 'BILLDISCOUNT';

-- Fix TOTALEXPENSE
SELECT 'Attempting to fix TOTALEXPENSE column' AS status;
ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALEXPENSE DECIMAL(18,4);

-- VALIDATION: Check if the change actually took effect
SELECT 'VALIDATION: Checking TOTALEXPENSE column change' AS status;
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    CASE
        WHEN COLUMN_TYPE = 'decimal(18,4)' THEN '✅ SUCCESS - Fixed to decimal(18,4)'
        WHEN COLUMN_TYPE = 'decimal(38,0)' THEN '❌ FAILED - Still decimal(38,0)'
        ELSE CONCAT('⚠️ UNEXPECTED - Now: ', COLUMN_TYPE)
    END as change_status
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_NAME = 'TOTALEXPENSE';

-- Fix TOTALQUANTITY
SELECT 'Attempting to fix TOTALQUANTITY column' AS status;
ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALQUANTITY DECIMAL(18,4);

-- VALIDATION: Check if the change actually took effect
SELECT 'VALIDATION: Checking TOTALQUANTITY column change' AS status;
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    CASE
        WHEN COLUMN_TYPE = 'decimal(18,4)' THEN '✅ SUCCESS - Fixed to decimal(18,4)'
        WHEN COLUMN_TYPE = 'decimal(38,0)' THEN '❌ FAILED - Still decimal(38,0)'
        ELSE CONCAT('⚠️ UNEXPECTED - Now: ', COLUMN_TYPE)
    END as change_status
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_NAME = 'TOTALQUANTITY';

-- ==========================================
-- STEP 4: COMPREHENSIVE VALIDATION
-- ==========================================

-- Show final status of all critical columns
SELECT 'FINAL VALIDATION: All critical financial columns' AS status;
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    CASE
        WHEN COLUMN_TYPE = 'decimal(18,4)' THEN '✅ CORRECT'
        WHEN COLUMN_TYPE = 'decimal(38,0)' THEN '❌ NEEDS FIX'
        ELSE '⚠️ CHECK'
    END as status
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_NAME IN (
    'TOTALCOSTVALUE', 'TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE', 'TOTALWHOLESALEVALUE',
    'BILLDISCOUNT', 'TOTALDISCOUNT', 'TOTALTAXVALUE', 'TOTALEXPENSE',
    'TOTALQUANTITY', 'TOTALFREEQUANTITY', 'TOTALOFFREEITEMVALUES'
  )
ORDER BY COLUMN_NAME;

-- Count remaining problematic columns
SELECT 'Summary: Remaining problematic columns' AS status;
SELECT
    COUNT(*) as total_columns_checked,
    SUM(CASE WHEN COLUMN_TYPE = 'decimal(18,4)' THEN 1 ELSE 0 END) as correctly_fixed,
    SUM(CASE WHEN COLUMN_TYPE = 'decimal(38,0)' THEN 1 ELSE 0 END) as still_need_fixing,
    SUM(CASE WHEN COLUMN_TYPE NOT IN ('decimal(18,4)', 'decimal(38,0)') THEN 1 ELSE 0 END) as other_types
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_NAME IN (
    'TOTALCOSTVALUE', 'TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE', 'TOTALWHOLESALEVALUE',
    'BILLDISCOUNT', 'TOTALDISCOUNT', 'TOTALTAXVALUE', 'TOTALEXPENSE',
    'TOTALQUANTITY', 'TOTALFREEQUANTITY', 'TOTALOFFREEITEMVALUES'
  );

-- ==========================================
-- STEP 5: DATA INTEGRITY CHECK
-- ==========================================

-- Verify that existing data is preserved after column changes
SELECT 'Data integrity check after schema changes' AS status;
SELECT
    COUNT(*) as total_records_after,
    COUNT(TOTALCOSTVALUE) as cost_records_after,
    MIN(TOTALCOSTVALUE) as min_cost_after,
    MAX(TOTALCOSTVALUE) as max_cost_after,
    AVG(TOTALCOSTVALUE) as avg_cost_after
FROM BILLFINANCEDETAILS;

-- ==========================================
-- STEP 6: MIGRATION SUCCESS INDICATOR
-- ==========================================

-- Final success check - this will help determine if migration truly succeeded
SELECT
    CASE
        WHEN (SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE()
                AND TABLE_NAME = 'BILLFINANCEDETAILS'
                AND COLUMN_NAME = 'TOTALCOSTVALUE') = 'decimal(18,4)'
        THEN '🎉 MIGRATION v2.1.13 SUCCESS: TOTALCOSTVALUE fixed to decimal(18,4) - Stock transfer reports will now show decimal places'
        ELSE '💥 MIGRATION v2.1.13 FAILED: TOTALCOSTVALUE still has wrong precision - Manual investigation required'
    END as final_result;

-- Show MySQL warnings if any (these might explain why previous migrations failed)
SHOW WARNINGS;

SELECT 'Migration v2.1.13 completed with full validation and diagnostic information' AS final_status;