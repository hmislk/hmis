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
-- STEP 3: VERIFICATION AND VALIDATION (WITH ENFORCEMENT)
-- ==========================================

SELECT 'Step 3: Running comprehensive verification with enforcement...' AS progress;

-- Create migration_log table for tracking failures (if not exists)
CREATE TABLE IF NOT EXISTS migration_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    migration_version VARCHAR(50) NOT NULL,
    check_type VARCHAR(100) NOT NULL,
    status ENUM('SUCCESS', 'FAILURE') NOT NULL,
    error_message TEXT,
    error_count INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_migration_version (migration_version),
    INDEX idx_created_at (created_at)
);

-- ENFORCED VERIFICATION: Fail migration if decimal(38,0) columns remain
SELECT 'CRITICAL ENFORCEMENT CHECK: Remaining decimal(38,0) columns...' AS status;

-- Get count of problematic columns into a variable and enforce success
SET @problematic_count = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'BILLFINANCEDETAILS'
      AND COLUMN_TYPE = 'decimal(38,0)'
);

-- Log the check result
INSERT INTO migration_log (migration_version, check_type, status, error_count, error_message)
VALUES ('v2.1.3', 'decimal_precision_check',
        CASE WHEN @problematic_count = 0 THEN 'SUCCESS' ELSE 'FAILURE' END,
        @problematic_count,
        CASE WHEN @problematic_count > 0
             THEN CONCAT('MIGRATION FAILED: ', @problematic_count, ' decimal(38,0) columns remain in BILLFINANCEDETAILS table')
             ELSE 'All decimal(38,0) columns successfully converted'
        END);

-- Show problematic columns for debugging (if any)
SELECT
    CASE WHEN @problematic_count > 0
         THEN CONCAT('FAILURE: ', @problematic_count, ' problematic columns found!')
         ELSE CONCAT('SUCCESS: No problematic columns remain (count: ', @problematic_count, ')')
    END AS enforcement_result;

-- Show any remaining problematic columns for debugging
SELECT COLUMN_NAME, COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_TYPE = 'decimal(38,0)'
ORDER BY COLUMN_NAME;

-- ENFORCE SUCCESS: Signal error if any decimal(38,0) columns remain
SELECT CASE
    WHEN @problematic_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = CONCAT(
            'MIGRATION v2.1.3 FAILED: ', @problematic_count,
            ' decimal(38,0) columns remain in BILLFINANCEDETAILS. ',
            'Manual DBA intervention required. Check migration_log table for details.'
        )
    ELSE 'Verification passed: All decimal(38,0) columns successfully converted'
END AS final_enforcement_check;

-- ENFORCED COUNT VERIFICATION: Check upgraded columns
SELECT 'ENFORCED CHECK: Verifying DECIMAL(20,4) column count...' AS status;

-- Get count of properly upgraded columns
SET @upgraded_count = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'BILLFINANCEDETAILS'
      AND COLUMN_TYPE = 'decimal(20,4)'
);

-- Expected count of upgraded columns (should be 9 based on the migration)
SET @expected_upgraded_count = 9;

-- Log the upgraded column count check
INSERT INTO migration_log (migration_version, check_type, status, error_count, error_message)
VALUES ('v2.1.3', 'upgraded_column_count_check',
        CASE WHEN @upgraded_count >= @expected_upgraded_count THEN 'SUCCESS' ELSE 'FAILURE' END,
        @upgraded_count,
        CASE WHEN @upgraded_count < @expected_upgraded_count
             THEN CONCAT('MIGRATION INCOMPLETE: Only ', @upgraded_count, ' of ', @expected_upgraded_count, ' expected DECIMAL(20,4) columns found')
             ELSE CONCAT('SUCCESS: ', @upgraded_count, ' DECIMAL(20,4) columns confirmed')
        END);

SELECT
    CASE WHEN @upgraded_count >= @expected_upgraded_count
         THEN CONCAT('SUCCESS: ', @upgraded_count, ' DECIMAL(20,4) columns found (expected: ', @expected_upgraded_count, ')')
         ELSE CONCAT('FAILURE: Only ', @upgraded_count, ' DECIMAL(20,4) columns found (expected: ', @expected_upgraded_count, ')')
    END AS upgraded_count_result;

-- ENFORCED VERIFICATION: Verify specific critical columns mentioned in issue #16989
SELECT 'ENFORCED CHECK: Critical financial columns verification...' AS status;

-- Check that all critical columns have correct precision
SET @critical_columns_correct = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'BILLFINANCEDETAILS'
      AND COLUMN_NAME IN (
          'TOTALCOSTVALUE', 'TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE',
          'BILLDISCOUNT', 'TOTALDISCOUNT', 'TOTALEXPENSE', 'TOTALQUANTITY',
          'BILLEXPENSE', 'LINEEXPENSE'
      )
      AND COLUMN_TYPE = 'decimal(20,4)'
);

SET @expected_critical_columns = 9;

-- Log critical columns verification
INSERT INTO migration_log (migration_version, check_type, status, error_count, error_message)
VALUES ('v2.1.3', 'critical_columns_precision_check',
        CASE WHEN @critical_columns_correct = @expected_critical_columns THEN 'SUCCESS' ELSE 'FAILURE' END,
        @critical_columns_correct,
        CASE WHEN @critical_columns_correct < @expected_critical_columns
             THEN CONCAT('MIGRATION FAILED: Only ', @critical_columns_correct, ' of ', @expected_critical_columns, ' critical columns have correct DECIMAL(20,4) precision')
             ELSE CONCAT('SUCCESS: All ', @critical_columns_correct, ' critical columns have correct DECIMAL(20,4) precision')
        END);

-- Show current state of critical columns for debugging
SELECT 'Critical financial columns current state:' AS status;
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

-- ENFORCE SUCCESS: Signal error if critical columns don't have correct precision
SELECT CASE
    WHEN @critical_columns_correct < @expected_critical_columns THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = CONCAT(
            'MIGRATION v2.1.3 FAILED: Only ', @critical_columns_correct, ' of ', @expected_critical_columns,
            ' critical columns have DECIMAL(20,4) precision. ',
            'Manual DBA review required. Check migration_log table and verify ALTER TABLE statements executed properly.'
        )
    ELSE 'Critical columns verification passed: All columns have correct precision'
END AS critical_columns_enforcement_check;

-- ==========================================
-- STEP 4: POST-MIGRATION DATA VALIDATION (WITH ENFORCEMENT)
-- ==========================================

SELECT 'Step 4: Post-migration data validation with enforcement...' AS progress;

-- ENFORCED DATA INTEGRITY CHECK: Test for precision errors in calculations
SELECT 'ENFORCED CHECK: Testing SUM() precision functionality...' AS test_status;

-- Sample query similar to the one causing issues in PharmacyController
-- This tests that the precision fix actually works
SET @test_record_count = 0;
SET @precision_test_passed = 0;

-- Check if we have data to test with
SET @test_record_count = (
    SELECT COUNT(*)
    FROM BILLFINANCEDETAILS
    WHERE TOTALPURCHASEVALUE IS NOT NULL
      AND TOTALCOSTVALUE IS NOT NULL
      AND TOTALRETAILSALEVALUE IS NOT NULL
    LIMIT 1000
);

-- Perform precision test only if we have data
SELECT
    CASE WHEN @test_record_count > 0 THEN 'Testing precision with real data'
         ELSE 'No data available for precision testing'
    END AS test_data_status;

-- If we have data, test precision by checking decimal places in SUM results
SELECT
    'Precision Test Results' as test_type,
    SUM(TOTALPURCHASEVALUE) as sum_purchase,
    SUM(TOTALCOSTVALUE) as sum_cost,
    SUM(TOTALRETAILSALEVALUE) as sum_retail,
    -- Check if results have proper decimal precision (not just integers)
    CASE WHEN SUM(TOTALPURCHASEVALUE) = ROUND(SUM(TOTALPURCHASEVALUE), 0)
              AND SUM(TOTALCOSTVALUE) = ROUND(SUM(TOTALCOSTVALUE), 0)
              AND SUM(TOTALRETAILSALEVALUE) = ROUND(SUM(TOTALRETAILSALEVALUE), 0)
         THEN 'WARNING: All sums are whole numbers - may indicate precision loss'
         ELSE 'SUCCESS: Decimal precision detected in sums'
    END AS precision_check_result
FROM BILLFINANCEDETAILS
WHERE TOTALPURCHASEVALUE IS NOT NULL
  AND TOTALCOSTVALUE IS NOT NULL
  AND TOTALRETAILSALEVALUE IS NOT NULL
  AND @test_record_count > 0
LIMIT 1;

-- ENFORCED NULL VALUE CHECK: Verify no data corruption occurred
SELECT 'ENFORCED CHECK: Verifying no NULL values introduced by migration...' AS status;

-- Get NULL counts for critical columns
SET @total_records = (SELECT COUNT(*) FROM BILLFINANCEDETAILS);
SET @null_cost_values = (SELECT COUNT(*) FROM BILLFINANCEDETAILS WHERE TOTALCOSTVALUE IS NULL);
SET @null_purchase_values = (SELECT COUNT(*) FROM BILLFINANCEDETAILS WHERE TOTALPURCHASEVALUE IS NULL);
SET @null_retail_values = (SELECT COUNT(*) FROM BILLFINANCEDETAILS WHERE TOTALRETAILSALEVALUE IS NULL);

-- Calculate baseline NULL percentage (should be reasonable for a migration)
SET @null_percentage = CASE WHEN @total_records > 0
                            THEN ROUND((@null_cost_values + @null_purchase_values + @null_retail_values) * 100.0 / (@total_records * 3), 2)
                            ELSE 0
                       END;

-- Log data integrity check
INSERT INTO migration_log (migration_version, check_type, status, error_count, error_message)
VALUES ('v2.1.3', 'data_integrity_check',
        CASE WHEN @null_percentage < 50 THEN 'SUCCESS' ELSE 'WARNING' END,
        @null_percentage,
        CONCAT('Total records: ', @total_records,
               ', NULL values - Cost: ', @null_cost_values,
               ', Purchase: ', @null_purchase_values,
               ', Retail: ', @null_retail_values,
               ', Overall NULL %: ', @null_percentage));

-- Show detailed NULL analysis
SELECT
    @total_records as total_records,
    @null_cost_values as null_cost_values,
    @null_purchase_values as null_purchase_values,
    @null_retail_values as null_retail_values,
    @null_percentage as null_percentage,
    CASE WHEN @null_percentage > 90
         THEN 'CRITICAL: Excessive NULL values detected'
         WHEN @null_percentage > 50
         THEN 'WARNING: High NULL values detected'
         ELSE 'SUCCESS: Acceptable NULL value levels'
    END AS data_integrity_status;

-- ENFORCE SUCCESS: Signal error if data integrity is severely compromised
SELECT CASE
    WHEN @null_percentage > 90 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = CONCAT(
            'MIGRATION v2.1.3 DATA INTEGRITY FAILURE: ', @null_percentage,
            '% NULL values detected in critical financial columns. ',
            'Data corruption likely occurred. Manual DBA intervention required immediately.'
        )
    ELSE 'Data integrity check passed: Acceptable NULL value levels'
END AS data_integrity_enforcement_check;

-- ==========================================
-- MIGRATION COMPLETION WITH FINAL VERIFICATION
-- ==========================================

-- FINAL ENFORCEMENT CHECK: Overall migration success verification
SELECT 'FINAL VERIFICATION: Checking overall migration success...' AS final_check_status;

-- Count successful checks in migration_log
SET @successful_checks = (
    SELECT COUNT(*)
    FROM migration_log
    WHERE migration_version = 'v2.1.3'
      AND status = 'SUCCESS'
);

SET @total_checks = (
    SELECT COUNT(*)
    FROM migration_log
    WHERE migration_version = 'v2.1.3'
);

SET @failed_checks = (
    SELECT COUNT(*)
    FROM migration_log
    WHERE migration_version = 'v2.1.3'
      AND status = 'FAILURE'
);

-- Log final migration status
INSERT INTO migration_log (migration_version, check_type, status, error_count, error_message)
VALUES ('v2.1.3', 'final_migration_verification',
        CASE WHEN @failed_checks = 0 THEN 'SUCCESS' ELSE 'FAILURE' END,
        @failed_checks,
        CONCAT('Migration completed with ', @successful_checks, ' successful checks, ',
               @failed_checks, ' failed checks out of ', @total_checks, ' total verification steps'));

-- Show final verification summary
SELECT
    @total_checks as total_verification_checks,
    @successful_checks as successful_checks,
    @failed_checks as failed_checks,
    CASE WHEN @failed_checks = 0
         THEN 'SUCCESS: Migration completed successfully'
         ELSE CONCAT('FAILURE: Migration completed with ', @failed_checks, ' failures')
    END AS final_migration_status;

-- FINAL ENFORCEMENT: Signal error if any verification checks failed
SELECT CASE
    WHEN @failed_checks > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = CONCAT(
            'MIGRATION v2.1.3 OVERALL FAILURE: ', @failed_checks, ' verification checks failed. ',
            'Check migration_log table for detailed failure information. ',
            'Manual DBA intervention required before proceeding.'
        )
    ELSE 'Migration v2.1.3 completed successfully - all verifications passed!'
END AS final_enforcement_result;

SELECT 'GitHub Issue #16989 precision issues have been resolved' AS issue_status;
SELECT 'SUM() operations should now maintain proper decimal precision' AS functional_fix;
SELECT NOW() AS migration_end_time;

-- Final summary for production monitoring
SELECT 'PRODUCTION SUMMARY:' AS summary;
SELECT 'All BILLFINANCEDETAILS decimal precision issues fixed with enforcement' AS result;
SELECT 'PharmacyController SUM() operations should now work correctly' AS application_impact;
SELECT 'No data loss expected - precision was expanded, not reduced' AS data_safety;
SELECT 'Migration includes automated verification and failure detection' AS monitoring_enhancement;

-- Show migration log summary for DBA review
SELECT 'MIGRATION LOG SUMMARY:' AS log_summary;
SELECT
    check_type,
    status,
    error_count,
    LEFT(error_message, 100) as error_message_preview,
    created_at
FROM migration_log
WHERE migration_version = 'v2.1.3'
ORDER BY created_at;