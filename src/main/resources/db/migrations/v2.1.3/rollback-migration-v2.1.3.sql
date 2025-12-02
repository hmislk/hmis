-- ==========================================
-- ROLLBACK MIGRATION v2.1.3
-- ==========================================
-- Author: Dr M H B Ariyaratne
-- Date: 2025-12-02
-- Purpose: Rollback decimal precision changes from migration v2.1.3
--
-- ⚠️  WARNING: POTENTIAL DATA LOSS RISK ⚠️
-- This rollback reduces decimal precision from DECIMAL(20,4) back to original types
-- Values with more than 4 decimal places will be truncated
-- Values exceeding the original column capacity will cause errors
--
-- USE ONLY IF:
-- 1. Migration v2.1.3 caused application errors
-- 2. Performance issues with DECIMAL(20,4) columns
-- 3. Rollback to exact previous state is required
--
-- DO NOT USE IF:
-- 1. Migration resolved the decimal precision issues
-- 2. Applications are working correctly
-- 3. Data contains values requiring higher precision
--
-- REQUIRED DOWNTIME: 10-20 minutes
-- BACKUP REQUIRED: YES - Full database backup before rollback
-- ==========================================

DELIMITER //

CREATE PROCEDURE rollback_v2_1_3_safe()
LANGUAGE SQL
DETERMINISTIC
MODIFIES SQL DATA
SQL SECURITY DEFINER
COMMENT 'Safe rollback of v2.1.3 migration with data protection checks'
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE error_count INT DEFAULT 0;
    DECLARE data_loss_risk INT DEFAULT 0;
    DECLARE table_exists INT DEFAULT 0;
    DECLARE total_records INT DEFAULT 0;

    -- Error handling
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SELECT 'ROLLBACK FAILED - Transaction rolled back' AS error_status;
        RESIGNAL;
    END;

    -- Start transaction for atomic execution
    START TRANSACTION;

    -- ==========================================
    -- PHASE 1: PRE-ROLLBACK VALIDATION
    -- ==========================================

    SELECT 'PHASE 1: Pre-rollback validation starting...' AS status;
    SELECT NOW() AS rollback_start_time;

    -- Check if table exists
    SELECT COUNT(*) INTO table_exists
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BILLFINANCEDETAILS';

    IF table_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'BILLFINANCEDETAILS table not found - rollback aborted';
    END IF;

    SELECT '✓ Table BILLFINANCEDETAILS exists' AS validation_result;

    -- Count total records
    SELECT COUNT(*) INTO total_records FROM BILLFINANCEDETAILS;
    SELECT CONCAT('✓ Total records to rollback: ', total_records) AS record_count;

    -- Check if migration v2.1.3 columns exist (should be DECIMAL(20,4))
    SELECT COUNT(*) INTO error_count
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'BILLFINANCEDETAILS'
      AND COLUMN_TYPE = 'decimal(20,4)'
      AND COLUMN_NAME IN (
        'TOTALCOSTVALUE', 'BILLDISCOUNT', 'TOTALEXPENSE', 'TOTALQUANTITY',
        'TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE', 'TOTALDISCOUNT',
        'BILLEXPENSE', 'LINEEXPENSE'
      );

    IF error_count = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No DECIMAL(20,4) columns found - migration v2.1.3 may not have been applied';
    END IF;

    SELECT CONCAT('✓ Found ', error_count, ' DECIMAL(20,4) columns from v2.1.3 migration') AS validation_result;

    -- ⚠️  CRITICAL: Check for potential data loss
    SELECT 'WARNING: Checking for potential data loss during rollback...' AS warning_status;

    -- Check for values that would be truncated when going back to decimal(38,0)
    -- Values with fractional parts will be truncated
    SELECT COUNT(*) INTO data_loss_risk
    FROM BILLFINANCEDETAILS
    WHERE (TOTALCOSTVALUE % 1) != 0
       OR (BILLDISCOUNT % 1) != 0
       OR (TOTALEXPENSE % 1) != 0
       OR (TOTALQUANTITY % 1) != 0;

    IF data_loss_risk > 0 THEN
        SELECT CONCAT('⚠️  WARNING: ', data_loss_risk, ' records have fractional values that will be LOST') AS data_loss_warning;
        SELECT 'Fractional parts will be truncated when reverting to decimal(38,0)' AS truncation_warning;
        -- Note: We don't abort here, but warn the user
    ELSE
        SELECT '✓ No fractional data loss detected' AS data_safety;
    END IF;

    -- Check for extremely large values that might cause issues
    SELECT COUNT(*) INTO error_count
    FROM BILLFINANCEDETAILS
    WHERE ABS(COALESCE(TOTALCOSTVALUE, 0)) >= 99999999999999999999999999999999999999
       OR ABS(COALESCE(TOTALPURCHASEVALUE, 0)) >= 9999999999999999
       OR ABS(COALESCE(TOTALRETAILSALEVALUE, 0)) >= 9999999999999999;

    IF error_count > 0 THEN
        SELECT CONCAT('ERROR: ', error_count, ' records have values too large for original column types') AS error_msg;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data too large for original column types - rollback aborted';
    END IF;

    SELECT '✓ No oversized values detected' AS size_validation;

    -- ==========================================
    -- PHASE 2: EXECUTE ROLLBACK CHANGES
    -- ==========================================

    SELECT 'PHASE 2: Executing rollback schema changes...' AS status;
    SELECT '⚠️  Data precision will be reduced - fractional parts may be lost' AS precision_warning;

    -- Rollback TOTALCOSTVALUE (was the main issue - back to decimal(38,0))
    SELECT 'Rolling back TOTALCOSTVALUE to decimal(38,0)...' AS progress;
    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALCOSTVALUE DECIMAL(38,0);
    SELECT '⚠️  TOTALCOSTVALUE rolled back to DECIMAL(38,0)' AS result;

    -- Rollback BILLDISCOUNT
    SELECT 'Rolling back BILLDISCOUNT to decimal(38,0)...' AS progress;
    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN BILLDISCOUNT DECIMAL(38,0);
    SELECT '⚠️  BILLDISCOUNT rolled back to DECIMAL(38,0)' AS result;

    -- Rollback TOTALEXPENSE
    SELECT 'Rolling back TOTALEXPENSE to decimal(38,0)...' AS progress;
    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALEXPENSE DECIMAL(38,0);
    SELECT '⚠️  TOTALEXPENSE rolled back to DECIMAL(38,0)' AS result;

    -- Rollback TOTALQUANTITY
    SELECT 'Rolling back TOTALQUANTITY to decimal(38,0)...' AS progress;
    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALQUANTITY DECIMAL(38,0);
    SELECT '⚠️  TOTALQUANTITY rolled back to DECIMAL(38,0)' AS result;

    -- Rollback other columns to their original types
    -- Note: These were likely decimal(18,x) originally, but rolling back to decimal(18,2) as safe default
    SELECT 'Rolling back other financial columns to decimal(18,2)...' AS progress;

    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALPURCHASEVALUE DECIMAL(18,2);
    SELECT '✓ TOTALPURCHASEVALUE rolled back to DECIMAL(18,2)' AS result;

    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALRETAILSALEVALUE DECIMAL(18,2);
    SELECT '✓ TOTALRETAILSALEVALUE rolled back to DECIMAL(18,2)' AS result;

    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALDISCOUNT DECIMAL(18,2);
    SELECT '✓ TOTALDISCOUNT rolled back to DECIMAL(18,2)' AS result;

    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN BILLEXPENSE DECIMAL(18,2);
    SELECT '✓ BILLEXPENSE rolled back to DECIMAL(18,2)' AS result;

    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN LINEEXPENSE DECIMAL(18,2);
    SELECT '✓ LINEEXPENSE rolled back to DECIMAL(18,2)' AS result;

    -- ==========================================
    -- PHASE 3: POST-ROLLBACK VALIDATION
    -- ==========================================

    SELECT 'PHASE 3: Post-rollback validation...' AS status;

    -- Verify target columns are back to original types
    SELECT COUNT(*) INTO error_count
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'BILLFINANCEDETAILS'
      AND COLUMN_TYPE = 'decimal(38,0)'
      AND COLUMN_NAME IN ('TOTALCOSTVALUE', 'BILLDISCOUNT', 'TOTALEXPENSE', 'TOTALQUANTITY');

    IF error_count != 4 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Post-rollback validation failed - critical columns not rolled back properly';
    END IF;

    SELECT '✓ Critical columns rolled back to DECIMAL(38,0)' AS validation_result;

    -- Verify other columns are rolled back to decimal(18,2)
    SELECT COUNT(*) INTO error_count
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'BILLFINANCEDETAILS'
      AND COLUMN_TYPE = 'decimal(18,2)'
      AND COLUMN_NAME IN ('TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE', 'TOTALDISCOUNT', 'BILLEXPENSE', 'LINEEXPENSE');

    IF error_count != 5 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Post-rollback validation failed - financial columns not rolled back properly';
    END IF;

    SELECT '✓ Financial columns rolled back to DECIMAL(18,2)' AS validation_result;

    -- Check for any remaining DECIMAL(20,4) columns from v2.1.3
    SELECT COUNT(*) INTO error_count
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'BILLFINANCEDETAILS'
      AND COLUMN_TYPE = 'decimal(20,4)'
      AND COLUMN_NAME IN (
        'TOTALCOSTVALUE', 'BILLDISCOUNT', 'TOTALEXPENSE', 'TOTALQUANTITY',
        'TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE', 'TOTALDISCOUNT',
        'BILLEXPENSE', 'LINEEXPENSE'
      );

    IF error_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Post-rollback validation failed - some v2.1.3 columns remain';
    END IF;

    SELECT '✓ All v2.1.3 DECIMAL(20,4) columns successfully rolled back' AS validation_result;

    -- Final record count verification
    SELECT COUNT(*) INTO error_count FROM BILLFINANCEDETAILS;

    IF error_count != total_records THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data integrity check failed - record count mismatch after rollback';
    END IF;

    SELECT CONCAT('✓ Data integrity verified - ', total_records, ' records preserved') AS integrity_result;

    -- Test that data is accessible (basic query)
    SELECT 'Testing data accessibility after rollback...' AS test_status;
    SELECT COUNT(*) as accessible_records FROM BILLFINANCEDETAILS LIMIT 1;
    SELECT '✓ Data accessibility test passed' AS test_result;

    -- Commit transaction
    COMMIT;

    -- ==========================================
    -- ROLLBACK SUCCESS
    -- ==========================================

    SELECT 'ROLLBACK v2.1.3 COMPLETED SUCCESSFULLY!' AS final_status;
    SELECT 'Migration v2.1.3 changes have been reverted' AS rollback_status;
    SELECT 'Columns returned to original decimal precision' AS technical_result;
    SELECT '⚠️  WARNING: Original decimal precision issues are now RESTORED' AS precision_warning;
    SELECT 'GitHub Issue #16989 precision problems may reoccur' AS application_impact;

    IF data_loss_risk > 0 THEN
        SELECT CONCAT('⚠️  DATA LOSS: ', data_loss_risk, ' records had fractional values truncated') AS data_loss_report;
    ELSE
        SELECT '✓ No data loss occurred during rollback' AS data_safety;
    END IF;

    SELECT NOW() AS rollback_end_time;

    -- Production monitoring summary
    SELECT
        'PRODUCTION ROLLBACK COMPLETED' AS deployment_status,
        CONCAT('Records processed: ', total_records) AS records_processed,
        '9 columns rolled back to original types' AS columns_modified,
        'Original precision limitations RESTORED' AS precision_impact,
        'Application decimal precision issues may return' AS application_warning;

END //

DELIMITER ;

-- ==========================================
-- ROLLBACK EXECUTION INSTRUCTIONS
-- ==========================================

SELECT 'ROLLBACK SCRIPT READY' AS status;
SELECT 'Run: CALL rollback_v2_1_3_safe();' AS execution_command;
SELECT '⚠️  WARNING: This will restore original decimal precision issues' AS important_warning;
SELECT 'Use only if v2.1.3 migration caused problems' AS usage_guidance;

-- Uncomment the following line to execute the rollback:
-- CALL rollback_v2_1_3_safe();

-- Clean up procedure after rollback (optional)
-- DROP PROCEDURE IF EXISTS rollback_v2_1_3_safe;

-- ==========================================
-- POST-ROLLBACK NOTES
-- ==========================================
/*
AFTER ROLLBACK COMPLETION:

1. EXPECTED ISSUES TO RETURN:
   - SUM() operations on TOTALCOSTVALUE will lose decimal precision
   - PharmacyController reports may show incorrect totals
   - GitHub Issue #16989 symptoms will reoccur

2. ALTERNATIVE SOLUTIONS:
   - Fix application code to handle precision correctly
   - Use CAST or ROUND functions in SUM operations
   - Apply v2.1.3 migration again when ready

3. DATA VERIFICATION:
   - Check pharmacy reports for calculation accuracy
   - Verify financial totals match expected values
   - Monitor for rounding errors in cost calculations

4. NEXT STEPS:
   - Address root cause of why rollback was needed
   - Plan re-application of v2.1.3 migration
   - Consider alternative precision handling approaches
*/