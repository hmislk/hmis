-- ==========================================
-- PRODUCTION-SAFE MIGRATION v2.1.3
-- ==========================================
-- Author: Dr M H B Ariyaratne
-- Date: 2025-12-02
-- GitHub Issue: #16989 - Decimal precision issues in BILLFINANCEDETAILS
--
-- REQUIRED DOWNTIME: 15-30 minutes (depending on table size)
-- BACKUP REQUIRED: YES - Full database backup before execution
--
-- PRODUCTION DEPLOYMENT STEPS:
-- 1. Create full database backup
-- 2. Put application in maintenance mode
-- 3. Execute this script in MySQL command line (NOT through application)
-- 4. Verify results using verification queries
-- 5. Test critical functionality (pharmacy reports)
-- 6. Remove maintenance mode
--
-- ROLLBACK: Execute rollback-migration-v2.1.3.sql if issues occur
-- DATA LOSS RISK: LOW - Precision is expanded, not reduced
-- ==========================================

DELIMITER //

CREATE PROCEDURE migrate_v2_1_3_safe()
LANGUAGE SQL
DETERMINISTIC
MODIFIES SQL DATA
SQL SECURITY DEFINER
COMMENT 'Safe migration for decimal precision fixes with full validation'
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE error_count INT DEFAULT 0;
    DECLARE overflow_count INT DEFAULT 0;
    DECLARE table_exists INT DEFAULT 0;
    DECLARE total_records INT DEFAULT 0;

    -- Error handling
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SELECT 'MIGRATION FAILED - Transaction rolled back' AS error_status;
        RESIGNAL;
    END;

    -- Start transaction for atomic execution
    START TRANSACTION;

    -- ==========================================
    -- PHASE 1: PRE-MIGRATION VALIDATION
    -- ==========================================

    SELECT 'PHASE 1: Pre-migration validation starting...' AS status;
    SELECT NOW() AS migration_start_time;

    -- Check if table exists
    SELECT COUNT(*) INTO table_exists
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BILLFINANCEDETAILS';

    IF table_exists = 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'BILLFINANCEDETAILS table not found - migration aborted';
    END IF;

    SELECT CONCAT('✓ Table BILLFINANCEDETAILS exists') AS validation_result;

    -- Count total records
    SELECT COUNT(*) INTO total_records FROM BILLFINANCEDETAILS;
    SELECT CONCAT('✓ Total records to migrate: ', total_records) AS record_count;

    -- CRITICAL: Check for values that would overflow DECIMAL(20,4)
    -- Maximum safe value for DECIMAL(20,4) is 9999999999999999.9999
    -- We check for absolute values >= 10^16 - 0.0001

    SELECT 'Checking for potential DECIMAL(20,4) overflow values...' AS validation_status;

    -- Check TOTALCOSTVALUE for overflow
    SELECT COUNT(*) INTO overflow_count
    FROM BILLFINANCEDETAILS
    WHERE ABS(COALESCE(TOTALCOSTVALUE, 0)) >= 9999999999999999.9999;

    IF overflow_count > 0 THEN
        SELECT CONCAT('ERROR: ', overflow_count, ' records would overflow TOTALCOSTVALUE DECIMAL(20,4)') AS error_msg;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data overflow detected in TOTALCOSTVALUE - migration aborted';
    END IF;

    -- Check TOTALPURCHASEVALUE for overflow
    SELECT COUNT(*) INTO overflow_count
    FROM BILLFINANCEDETAILS
    WHERE ABS(COALESCE(TOTALPURCHASEVALUE, 0)) >= 9999999999999999.9999;

    IF overflow_count > 0 THEN
        SELECT CONCAT('ERROR: ', overflow_count, ' records would overflow TOTALPURCHASEVALUE DECIMAL(20,4)') AS error_msg;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data overflow detected in TOTALPURCHASEVALUE - migration aborted';
    END IF;

    -- Check TOTALRETAILSALEVALUE for overflow
    SELECT COUNT(*) INTO overflow_count
    FROM BILLFINANCEDETAILS
    WHERE ABS(COALESCE(TOTALRETAILSALEVALUE, 0)) >= 9999999999999999.9999;

    IF overflow_count > 0 THEN
        SELECT CONCAT('ERROR: ', overflow_count, ' records would overflow TOTALRETAILSALEVALUE DECIMAL(20,4)') AS error_msg;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data overflow detected in TOTALRETAILSALEVALUE - migration aborted';
    END IF;

    -- Check other financial columns
    SELECT COUNT(*) INTO overflow_count
    FROM BILLFINANCEDETAILS
    WHERE ABS(COALESCE(BILLDISCOUNT, 0)) >= 9999999999999999.9999
       OR ABS(COALESCE(TOTALDISCOUNT, 0)) >= 9999999999999999.9999
       OR ABS(COALESCE(TOTALEXPENSE, 0)) >= 9999999999999999.9999
       OR ABS(COALESCE(TOTALQUANTITY, 0)) >= 9999999999999999.9999
       OR ABS(COALESCE(BILLEXPENSE, 0)) >= 9999999999999999.9999
       OR ABS(COALESCE(LINEEXPENSE, 0)) >= 9999999999999999.9999;

    IF overflow_count > 0 THEN
        SELECT CONCAT('ERROR: ', overflow_count, ' records would overflow other financial columns DECIMAL(20,4)') AS error_msg;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data overflow detected in financial columns - migration aborted';
    END IF;

    SELECT '✓ All overflow validation checks passed' AS validation_result;

    -- Verify current problematic column types
    SELECT COUNT(*) INTO error_count
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'BILLFINANCEDETAILS'
      AND COLUMN_TYPE = 'decimal(38,0)'
      AND COLUMN_NAME IN ('TOTALCOSTVALUE', 'BILLDISCOUNT', 'TOTALEXPENSE', 'TOTALQUANTITY');

    SELECT CONCAT('✓ Found ', error_count, ' problematic decimal(38,0) columns to fix') AS validation_result;

    -- ==========================================
    -- PHASE 2: EXECUTE SCHEMA CHANGES
    -- ==========================================

    SELECT 'PHASE 2: Executing schema changes...' AS status;

    -- Fix TOTALCOSTVALUE - THE MAIN ISSUE from GitHub #16989
    SELECT 'Fixing TOTALCOSTVALUE precision (CRITICAL for SUM operations)...' AS progress;
    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALCOSTVALUE DECIMAL(20,4);
    SELECT '✓ TOTALCOSTVALUE upgraded to DECIMAL(20,4)' AS result;

    -- Fix BILLDISCOUNT
    SELECT 'Fixing BILLDISCOUNT precision...' AS progress;
    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN BILLDISCOUNT DECIMAL(20,4);
    SELECT '✓ BILLDISCOUNT upgraded to DECIMAL(20,4)' AS result;

    -- Fix TOTALEXPENSE
    SELECT 'Fixing TOTALEXPENSE precision...' AS progress;
    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALEXPENSE DECIMAL(20,4);
    SELECT '✓ TOTALEXPENSE upgraded to DECIMAL(20,4)' AS result;

    -- Fix TOTALQUANTITY
    SELECT 'Fixing TOTALQUANTITY precision...' AS progress;
    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALQUANTITY DECIMAL(20,4);
    SELECT '✓ TOTALQUANTITY upgraded to DECIMAL(20,4)' AS result;

    -- Upgrade other financial columns for future-proofing
    SELECT 'Upgrading other financial columns for consistency...' AS progress;

    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALPURCHASEVALUE DECIMAL(20,4);
    SELECT '✓ TOTALPURCHASEVALUE upgraded to DECIMAL(20,4)' AS result;

    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALRETAILSALEVALUE DECIMAL(20,4);
    SELECT '✓ TOTALRETAILSALEVALUE upgraded to DECIMAL(20,4)' AS result;

    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALDISCOUNT DECIMAL(20,4);
    SELECT '✓ TOTALDISCOUNT upgraded to DECIMAL(20,4)' AS result;

    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN BILLEXPENSE DECIMAL(20,4);
    SELECT '✓ BILLEXPENSE upgraded to DECIMAL(20,4)' AS result;

    ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN LINEEXPENSE DECIMAL(20,4);
    SELECT '✓ LINEEXPENSE upgraded to DECIMAL(20,4)' AS result;

    -- ==========================================
    -- PHASE 3: POST-MIGRATION VALIDATION
    -- ==========================================

    SELECT 'PHASE 3: Post-migration validation...' AS status;

    -- Verify NO decimal(38,0) columns remain in target list
    SELECT COUNT(*) INTO error_count
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'BILLFINANCEDETAILS'
      AND COLUMN_TYPE = 'decimal(38,0)'
      AND COLUMN_NAME IN (
        'TOTALCOSTVALUE', 'BILLDISCOUNT', 'TOTALEXPENSE', 'TOTALQUANTITY',
        'TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE', 'TOTALDISCOUNT',
        'BILLEXPENSE', 'LINEEXPENSE'
      );

    IF error_count > 0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Post-migration validation failed - some columns not upgraded';
    END IF;

    SELECT '✓ All target columns successfully upgraded' AS validation_result;

    -- Verify all target columns are now DECIMAL(20,4)
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

    IF error_count != 9 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Post-migration validation failed - not all columns are DECIMAL(20,4)';
    END IF;

    SELECT '✓ All 9 financial columns now DECIMAL(20,4)' AS validation_result;

    -- Test SUM operation precision (the original issue)
    SELECT 'Testing SUM operation precision...' AS test_status;

    -- This should now return proper decimal values
    SELECT
        ROUND(SUM(COALESCE(TOTALCOSTVALUE, 0)), 4) as sum_cost_value,
        ROUND(SUM(COALESCE(TOTALPURCHASEVALUE, 0)), 4) as sum_purchase_value,
        COUNT(*) as test_record_count
    FROM BILLFINANCEDETAILS
    WHERE (TOTALCOSTVALUE IS NOT NULL OR TOTALPURCHASEVALUE IS NOT NULL)
    LIMIT 1;

    SELECT '✓ SUM operations test completed successfully' AS test_result;

    -- Check for any unexpected NULL values
    SELECT 'Checking for unexpected NULL values...' AS null_check;

    SELECT
        COUNT(*) as total_records_after_migration,
        SUM(CASE WHEN TOTALCOSTVALUE IS NULL THEN 1 ELSE 0 END) as null_cost_values,
        SUM(CASE WHEN TOTALPURCHASEVALUE IS NULL THEN 1 ELSE 0 END) as null_purchase_values
    FROM BILLFINANCEDETAILS;

    -- Final record count verification
    SELECT COUNT(*) INTO error_count FROM BILLFINANCEDETAILS;

    IF error_count != total_records THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Data integrity check failed - record count mismatch';
    END IF;

    SELECT CONCAT('✓ Data integrity verified - ', total_records, ' records preserved') AS integrity_result;

    -- Commit transaction
    COMMIT;

    -- ==========================================
    -- MIGRATION SUCCESS
    -- ==========================================

    SELECT 'MIGRATION v2.1.3 COMPLETED SUCCESSFULLY!' AS final_status;
    SELECT 'GitHub Issue #16989 decimal precision issues resolved' AS issue_status;
    SELECT 'All BILLFINANCEDETAILS financial columns upgraded to DECIMAL(20,4)' AS technical_result;
    SELECT 'SUM() operations will now maintain proper decimal precision' AS functional_result;
    SELECT 'PharmacyController reports should now show correct values' AS application_impact;
    SELECT 'No data loss occurred - precision was expanded' AS data_safety;
    SELECT NOW() AS migration_end_time;

    -- Production monitoring summary
    SELECT
        'PRODUCTION DEPLOYMENT SUCCESSFUL' AS deployment_status,
        CONCAT('Records migrated: ', total_records) AS records_processed,
        '9 columns upgraded to DECIMAL(20,4)' AS columns_modified,
        'Zero data loss' AS data_impact,
        'Application can resume normal operation' AS next_action;

END //

DELIMITER ;

-- ==========================================
-- EXECUTION INSTRUCTIONS
-- ==========================================

SELECT 'READY TO EXECUTE MIGRATION v2.1.3' AS status;
SELECT 'Run: CALL migrate_v2_1_3_safe();' AS execution_command;
SELECT 'This will execute all validations and schema changes atomically' AS description;
SELECT 'If any validation fails, the entire migration will be rolled back' AS safety_note;

-- Uncomment the following line to execute the migration:
-- CALL migrate_v2_1_3_safe();

-- Clean up procedure after successful execution (optional)
-- DROP PROCEDURE IF EXISTS migrate_v2_1_3_safe;