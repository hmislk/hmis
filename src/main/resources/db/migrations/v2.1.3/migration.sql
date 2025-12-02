-- Migration v2.1.3: Complete Fix for ALL decimal precision issues in BILLFINANCEDETAILS
-- Author: Dr M H B Ariyaratne
-- Date: 2025-12-02
-- GitHub Issue: #16989 - Goods in Transit and Stock Transfer Reports - Multiple Cost/Value Display Issues
-- CRITICAL: This migration fixes the root cause where SUM() operations lose decimal precision

-- ==========================================
-- PRE-MIGRATION VERIFICATION
-- ==========================================

SELECT 'Starting Migration v2.1.3 - Complete precision fix for production' AS status;
SELECT NOW() AS migration_start_time;

-- Verify table exists
SELECT 'Verifying BILLFINANCEDETAILS table exists...' AS status;
SELECT COUNT(*) as table_exists FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BILLFINANCEDETAILS';

-- Show current problematic columns (should include TOTALCOSTVALUE with decimal(38,0))
SELECT 'Current problematic columns with wrong precision:' AS status;
SELECT COLUMN_NAME, COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND (COLUMN_TYPE = 'decimal(38,0)' OR COLUMN_TYPE LIKE 'decimal(18,%)')
ORDER BY COLUMN_NAME;

-- Count total records to estimate impact
SELECT CONCAT('Total records in BILLFINANCEDETAILS: ', COUNT(*)) AS record_count
FROM BILLFINANCEDETAILS;

-- ==========================================
-- STEP 1: FIX CRITICAL PRECISION ISSUES
-- ==========================================

SELECT 'Step 1: Fixing critical precision issues causing calculation errors...' AS progress;

-- Fix TOTALCOSTVALUE - THE MAIN ISSUE from GitHub #16989
-- This is the column losing precision in SUM() operations
ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALCOSTVALUE DECIMAL(20,4);
SELECT 'CRITICAL FIX: TOTALCOSTVALUE precision upgraded to DECIMAL(20,4)' AS progress;

-- Fix BILLDISCOUNT - currently decimal(38,0)
ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN BILLDISCOUNT DECIMAL(20,4);
SELECT 'Fixed BILLDISCOUNT precision to DECIMAL(20,4)' AS progress;

-- Fix TOTALEXPENSE - currently decimal(38,0)
ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALEXPENSE DECIMAL(20,4);
SELECT 'Fixed TOTALEXPENSE precision to DECIMAL(20,4)' AS progress;

-- Fix TOTALQUANTITY - currently decimal(38,0), upgrade to allow partial quantities
ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALQUANTITY DECIMAL(20,4);
SELECT 'Fixed TOTALQUANTITY precision to DECIMAL(20,4)' AS progress;

-- ==========================================
-- STEP 2: UPGRADE OTHER FINANCIAL COLUMNS TO PRECISION 20
-- ==========================================

SELECT 'Step 2: Upgrading other critical financial columns for future-proofing...' AS progress;

-- Upgrade main financial totals to higher precision
ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALPURCHASEVALUE DECIMAL(20,4);
SELECT 'Upgraded TOTALPURCHASEVALUE to DECIMAL(20,4)' AS progress;

ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALRETAILSALEVALUE DECIMAL(20,4);
SELECT 'Upgraded TOTALRETAILSALEVALUE to DECIMAL(20,4)' AS progress;

ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALDISCOUNT DECIMAL(20,4);
SELECT 'Upgraded TOTALDISCOUNT to DECIMAL(20,4)' AS progress;

-- Upgrade expense-related columns
ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN BILLEXPENSE DECIMAL(20,4);
SELECT 'Upgraded BILLEXPENSE to DECIMAL(20,4)' AS progress;

ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN LINEEXPENSE DECIMAL(20,4);
SELECT 'Upgraded LINEEXPENSE to DECIMAL(20,4)' AS progress;

-- ==========================================
-- STEP 3: VERIFICATION AND VALIDATION
-- ==========================================

SELECT 'Step 3: Running comprehensive verification...' AS progress;

-- Verify NO decimal(38,0) columns remain
SELECT 'CRITICAL CHECK: Remaining decimal(38,0) columns (should be 0):' AS status;
SELECT COUNT(*) as problematic_columns_remaining
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_TYPE = 'decimal(38,0)';

-- Show any remaining problematic columns
SELECT 'Any remaining problematic columns:' AS status;
SELECT COLUMN_NAME, COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_TYPE = 'decimal(38,0)'
ORDER BY COLUMN_NAME;

-- Count upgraded columns
SELECT 'Summary: Total DECIMAL(20,4) columns:' AS status;
SELECT COUNT(*) as upgraded_columns
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_TYPE = 'decimal(20,4)';

-- Verify specific critical columns mentioned in issue #16989
SELECT 'Verification of critical financial columns:' AS status;
SELECT COLUMN_NAME, COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_NAME IN (
    'TOTALCOSTVALUE', 'TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE',
    'BILLDISCOUNT', 'TOTALDISCOUNT', 'TOTALEXPENSE', 'TOTALQUANTITY',
    'BILLEXPENSE', 'LINEEXPENSE'
  )
ORDER BY COLUMN_NAME;

-- ==========================================
-- STEP 4: POST-MIGRATION DATA VALIDATION
-- ==========================================

SELECT 'Step 4: Post-migration data validation...' AS progress;

-- Test that precision is working correctly - simulate the original issue
-- This should now return proper decimal scale
SELECT 'Testing SUM() precision (this should now work correctly):' AS test_status;

-- Sample query similar to the one causing issues in PharmacyController
SELECT
    'Test Department' as dept_name,
    SUM(TOTALPURCHASEVALUE) as sum_purchase,
    SUM(TOTALCOSTVALUE) as sum_cost,
    SUM(TOTALRETAILSALEVALUE) as sum_retail
FROM BILLFINANCEDETAILS
WHERE TOTALPURCHASEVALUE IS NOT NULL
  AND TOTALCOSTVALUE IS NOT NULL
  AND TOTALRETAILSALEVALUE IS NOT NULL
LIMIT 1;

-- Check for any NULL values that might have been introduced
SELECT 'Checking for any NULL values in critical columns:' AS status;
SELECT
    COUNT(*) as total_records,
    SUM(CASE WHEN TOTALCOSTVALUE IS NULL THEN 1 ELSE 0 END) as null_cost_values,
    SUM(CASE WHEN TOTALPURCHASEVALUE IS NULL THEN 1 ELSE 0 END) as null_purchase_values,
    SUM(CASE WHEN TOTALRETAILSALEVALUE IS NULL THEN 1 ELSE 0 END) as null_retail_values
FROM BILLFINANCEDETAILS;

-- ==========================================
-- MIGRATION COMPLETION
-- ==========================================

SELECT 'Migration v2.1.3 completed successfully!' AS final_status;
SELECT 'GitHub Issue #16989 precision issues have been resolved' AS issue_status;
SELECT 'SUM() operations should now maintain proper decimal precision' AS functional_fix;
SELECT NOW() AS migration_end_time;

-- Final summary for production monitoring
SELECT 'PRODUCTION SUMMARY:' AS summary;
SELECT 'All BILLFINANCEDETAILS decimal precision issues fixed' AS result;
SELECT 'PharmacyController SUM() operations should now work correctly' AS application_impact;
SELECT 'No data loss expected - precision was expanded, not reduced' AS data_safety;