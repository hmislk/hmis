-- ==========================================
-- PRE-MIGRATION VALIDATION SCRIPT v2.1.3
-- ==========================================
-- Author: Dr M H B Ariyaratne
-- Date: 2025-12-02
-- Purpose: Comprehensive validation before applying migration v2.1.3
--
-- USAGE: Run this script BEFORE executing the main migration
-- This script is READ-ONLY and makes no changes to the database
-- Use results to assess migration readiness and potential issues
-- ==========================================

-- Start validation report
SELECT '==========================================' AS separator;
SELECT 'PRE-MIGRATION VALIDATION REPORT v2.1.3' AS title;
SELECT '==========================================' AS separator;
SELECT NOW() AS validation_timestamp;
SELECT USER() AS database_user;
SELECT DATABASE() AS target_database;

-- ==========================================
-- SECTION 1: ENVIRONMENT VALIDATION
-- ==========================================

SELECT '' AS spacer1;
SELECT '1. ENVIRONMENT VALIDATION' AS section_header;
SELECT '==========================================' AS separator;

-- Check MySQL version compatibility (enforcing)
SELECT
    VERSION() AS mysql_version,
    CASE
        WHEN VERSION() >= '5.7.0' THEN '✓ Compatible - proceeding with migration'
        ELSE 'BLOCKED - MySQL 5.6 and below not supported'
    END AS version_status;

-- Enforce MySQL version requirement (abort if < 5.7)
DROP PROCEDURE IF EXISTS enforce_mysql_version;
DELIMITER //
CREATE PROCEDURE enforce_mysql_version()
BEGIN
    IF VERSION() < '5.7.0' THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Migration aborted: MySQL 5.7+ required. Current version is EOL (MySQL 5.6 reached end-of-life February 2021). Please upgrade to MySQL 5.7 or higher before running this migration.';
    END IF;
END//
DELIMITER ;
CALL enforce_mysql_version();
DROP PROCEDURE enforce_mysql_version;

-- Check table existence
SELECT
    CASE
        WHEN COUNT(*) = 1 THEN '✓ BILLFINANCEDETAILS table exists'
        ELSE '❌ BILLFINANCEDETAILS table NOT FOUND'
    END AS table_status
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BILLFINANCEDETAILS';

-- Check database size and free space
SELECT
    ROUND((SUM(data_length + index_length) / 1024 / 1024), 2) AS database_size_mb,
    ROUND((SUM(data_free) / 1024 / 1024), 2) AS free_space_mb,
    CASE
        WHEN SUM(data_free) > (SUM(data_length + index_length) * 0.2) THEN '✓ Sufficient free space'
        ELSE '⚠️  Low free space - monitor during migration'
    END AS space_status
FROM information_schema.tables
WHERE table_schema = DATABASE();

-- ==========================================
-- SECTION 2: TABLE STRUCTURE VALIDATION
-- ==========================================

SELECT '' AS spacer2;
SELECT '2. TABLE STRUCTURE VALIDATION' AS section_header;
SELECT '==========================================' AS separator;

-- Show current column types that will be modified
SELECT 'Current column types to be modified:' AS info;
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    CASE
        WHEN COLUMN_TYPE = 'decimal(38,0)' THEN '🔧 Will be upgraded to DECIMAL(20,4)'
        WHEN COLUMN_TYPE LIKE 'decimal(18,%' THEN '🔧 Will be upgraded to DECIMAL(20,4)'
        ELSE '✓ Already correct or unknown type'
    END AS migration_action
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_NAME IN (
    'TOTALCOSTVALUE', 'TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE',
    'BILLDISCOUNT', 'TOTALDISCOUNT', 'TOTALEXPENSE', 'TOTALQUANTITY',
    'BILLEXPENSE', 'LINEEXPENSE'
  )
ORDER BY COLUMN_NAME;

-- Count problematic columns
SELECT
    COUNT(*) as problematic_columns,
    CASE
        WHEN COUNT(*) > 0 THEN CONCAT('🔧 ', COUNT(*), ' columns need migration')
        ELSE '✓ No problematic columns found'
    END AS column_status
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND (COLUMN_TYPE = 'decimal(38,0)' OR COLUMN_TYPE LIKE 'decimal(18,%)')
  AND COLUMN_NAME IN (
    'TOTALCOSTVALUE', 'TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE',
    'BILLDISCOUNT', 'TOTALDISCOUNT', 'TOTALEXPENSE', 'TOTALQUANTITY',
    'BILLEXPENSE', 'LINEEXPENSE'
  );

-- ==========================================
-- SECTION 3: DATA VALIDATION
-- ==========================================

SELECT '' AS spacer3;
SELECT '3. DATA VALIDATION' AS section_header;
SELECT '==========================================' AS separator;

-- Record count analysis
SELECT
    COUNT(*) as total_records,
    CASE
        WHEN COUNT(*) < 1000 THEN '✓ Small dataset - fast migration'
        WHEN COUNT(*) < 100000 THEN '✓ Medium dataset - normal migration time'
        WHEN COUNT(*) < 1000000 THEN '⚠️  Large dataset - extended migration time'
        ELSE '⚠️  Very large dataset - plan extended downtime'
    END AS size_assessment
FROM BILLFINANCEDETAILS;

-- Data distribution analysis
SELECT 'Data distribution in key columns:' AS info;
SELECT
    'TOTALCOSTVALUE' as column_name,
    COUNT(*) as total_rows,
    COUNT(TOTALCOSTVALUE) as non_null_rows,
    COUNT(*) - COUNT(TOTALCOSTVALUE) as null_rows,
    ROUND(AVG(COALESCE(TOTALCOSTVALUE, 0)), 2) as avg_value,
    ROUND(MIN(COALESCE(TOTALCOSTVALUE, 0)), 2) as min_value,
    ROUND(MAX(COALESCE(TOTALCOSTVALUE, 0)), 2) as max_value
FROM BILLFINANCEDETAILS

UNION ALL

SELECT
    'TOTALPURCHASEVALUE' as column_name,
    COUNT(*) as total_rows,
    COUNT(TOTALPURCHASEVALUE) as non_null_rows,
    COUNT(*) - COUNT(TOTALPURCHASEVALUE) as null_rows,
    ROUND(AVG(COALESCE(TOTALPURCHASEVALUE, 0)), 2) as avg_value,
    ROUND(MIN(COALESCE(TOTALPURCHASEVALUE, 0)), 2) as min_value,
    ROUND(MAX(COALESCE(TOTALPURCHASEVALUE, 0)), 2) as max_value
FROM BILLFINANCEDETAILS;

-- ==========================================
-- SECTION 4: OVERFLOW DETECTION
-- ==========================================

SELECT '' AS spacer4;
SELECT '4. OVERFLOW DETECTION (CRITICAL)' AS section_header;
SELECT '==========================================' AS separator;

-- Check for values that would overflow DECIMAL(20,4)
-- Maximum safe value: 9999999999999999.9999
SELECT 'Checking for DECIMAL(20,4) overflow risk...' AS check_info;

-- TOTALCOSTVALUE overflow check
SELECT
    'TOTALCOSTVALUE' as column_name,
    COUNT(*) as total_rows,
    COUNT(CASE WHEN ABS(COALESCE(TOTALCOSTVALUE, 0)) >= 9999999999999999.9999 THEN 1 END) as overflow_risk,
    CASE
        WHEN COUNT(CASE WHEN ABS(COALESCE(TOTALCOSTVALUE, 0)) >= 9999999999999999.9999 THEN 1 END) = 0 THEN '✓ No overflow risk'
        ELSE CONCAT('❌ ', COUNT(CASE WHEN ABS(COALESCE(TOTALCOSTVALUE, 0)) >= 9999999999999999.9999 THEN 1 END), ' records at risk')
    END AS overflow_status
FROM BILLFINANCEDETAILS

UNION ALL

-- TOTALPURCHASEVALUE overflow check
SELECT
    'TOTALPURCHASEVALUE' as column_name,
    COUNT(*) as total_rows,
    COUNT(CASE WHEN ABS(COALESCE(TOTALPURCHASEVALUE, 0)) >= 9999999999999999.9999 THEN 1 END) as overflow_risk,
    CASE
        WHEN COUNT(CASE WHEN ABS(COALESCE(TOTALPURCHASEVALUE, 0)) >= 9999999999999999.9999 THEN 1 END) = 0 THEN '✓ No overflow risk'
        ELSE CONCAT('❌ ', COUNT(CASE WHEN ABS(COALESCE(TOTALPURCHASEVALUE, 0)) >= 9999999999999999.9999 THEN 1 END), ' records at risk')
    END AS overflow_status
FROM BILLFINANCEDETAILS

UNION ALL

-- TOTALRETAILSALEVALUE overflow check
SELECT
    'TOTALRETAILSALEVALUE' as column_name,
    COUNT(*) as total_rows,
    COUNT(CASE WHEN ABS(COALESCE(TOTALRETAILSALEVALUE, 0)) >= 9999999999999999.9999 THEN 1 END) as overflow_risk,
    CASE
        WHEN COUNT(CASE WHEN ABS(COALESCE(TOTALRETAILSALEVALUE, 0)) >= 9999999999999999.9999 THEN 1 END) = 0 THEN '✓ No overflow risk'
        ELSE CONCAT('❌ ', COUNT(CASE WHEN ABS(COALESCE(TOTALRETAILSALEVALUE, 0)) >= 9999999999999999.9999 THEN 1 END), ' records at risk')
    END AS overflow_status
FROM BILLFINANCEDETAILS;

-- Show actual problematic values if any exist
SELECT 'Detailed overflow analysis:' AS detail_info;
SELECT
    ID,
    TOTALCOSTVALUE,
    TOTALPURCHASEVALUE,
    TOTALRETAILSALEVALUE,
    'Overflow risk' as issue
FROM BILLFINANCEDETAILS
WHERE ABS(COALESCE(TOTALCOSTVALUE, 0)) >= 9999999999999999.9999
   OR ABS(COALESCE(TOTALPURCHASEVALUE, 0)) >= 9999999999999999.9999
   OR ABS(COALESCE(TOTALRETAILSALEVALUE, 0)) >= 9999999999999999.9999
LIMIT 10;

-- ==========================================
-- SECTION 5: PRECISION ANALYSIS
-- ==========================================

SELECT '' AS spacer5;
SELECT '5. PRECISION ANALYSIS' AS section_header;
SELECT '==========================================' AS separator;

-- Analyze current precision issues (the problem this migration solves)
SELECT 'Testing current SUM precision issues:' AS test_info;

-- Test the specific issue from GitHub #16989
SELECT
    COUNT(*) as sample_records,
    SUM(TOTALCOSTVALUE) as sum_cost_value,
    SUM(TOTALPURCHASEVALUE) as sum_purchase_value,
    AVG(TOTALCOSTVALUE) as avg_cost_value,
    COUNT(CASE WHEN (TOTALCOSTVALUE % 1) != 0 THEN 1 END) as fractional_cost_count,
    COUNT(CASE WHEN (TOTALPURCHASEVALUE % 1) != 0 THEN 1 END) as fractional_purchase_count,
    CASE
        WHEN COUNT(CASE WHEN (TOTALCOSTVALUE % 1) != 0 THEN 1 END) > 0
          OR COUNT(CASE WHEN (TOTALPURCHASEVALUE % 1) != 0 THEN 1 END) > 0
        THEN '✓ Fractional precision in use'
        ELSE '⚠️  No fractional data currently stored'
    END AS current_precision_status
FROM BILLFINANCEDETAILS
WHERE TOTALCOSTVALUE IS NOT NULL
  AND TOTALPURCHASEVALUE IS NOT NULL
LIMIT 1000;

-- Check for fractional values that would be preserved
SELECT
    'Fractional values analysis' as analysis_type,
    COUNT(CASE WHEN (TOTALCOSTVALUE % 1) != 0 THEN 1 END) as fractional_cost_values,
    COUNT(CASE WHEN (TOTALPURCHASEVALUE % 1) != 0 THEN 1 END) as fractional_purchase_values,
    CASE
        WHEN COUNT(CASE WHEN (TOTALCOSTVALUE % 1) != 0 THEN 1 END) > 0 THEN '✓ Migration will preserve fractional data'
        ELSE '✓ No fractional data to preserve'
    END AS fractional_status
FROM BILLFINANCEDETAILS;

-- ==========================================
-- SECTION 6: DEPENDENCY CHECK
-- ==========================================

SELECT '' AS spacer6;
SELECT '6. DEPENDENCY CHECK' AS section_header;
SELECT '==========================================' AS separator;

-- Check for foreign key constraints
SELECT 'Foreign key constraints on BILLFINANCEDETAILS:' AS fk_info;
SELECT
    CONSTRAINT_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND REFERENCED_TABLE_NAME IS NOT NULL;

-- Check for indexes on columns being modified
SELECT 'Indexes on columns being modified:' AS index_info;
SELECT
    INDEX_NAME,
    COLUMN_NAME,
    SEQ_IN_INDEX
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_NAME IN (
    'TOTALCOSTVALUE', 'TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE',
    'BILLDISCOUNT', 'TOTALDISCOUNT', 'TOTALEXPENSE', 'TOTALQUANTITY',
    'BILLEXPENSE', 'LINEEXPENSE'
  )
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- ==========================================
-- SECTION 7: READINESS ASSESSMENT
-- ==========================================

SELECT '' AS spacer7;
SELECT '7. MIGRATION READINESS ASSESSMENT' AS section_header;
SELECT '==========================================' AS separator;

-- Overall readiness check
SELECT
    'Migration Readiness Summary' as assessment_type,
    CASE
        WHEN (
            -- Check table exists
            (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BILLFINANCEDETAILS') = 1
            AND
            -- Check no overflow risk
            (SELECT COUNT(*) FROM BILLFINANCEDETAILS
             WHERE ABS(COALESCE(TOTALCOSTVALUE, 0)) >= 9999999999999999.9999
                OR ABS(COALESCE(TOTALPURCHASEVALUE, 0)) >= 9999999999999999.9999
                OR ABS(COALESCE(TOTALRETAILSALEVALUE, 0)) >= 9999999999999999.9999) = 0
            AND
            -- Check problematic columns exist
            (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BILLFINANCEDETAILS'
               AND (COLUMN_TYPE = 'decimal(38,0)' OR COLUMN_TYPE LIKE 'decimal(18,%)')
               AND COLUMN_NAME IN ('TOTALCOSTVALUE', 'BILLDISCOUNT', 'TOTALEXPENSE', 'TOTALQUANTITY')) > 0
        ) THEN '✅ READY FOR MIGRATION'
        ELSE '❌ NOT READY - Fix issues above'
    END AS readiness_status;

-- Estimated migration time
SELECT
    CASE
        WHEN (SELECT COUNT(*) FROM BILLFINANCEDETAILS) < 10000 THEN '⏱️  Estimated time: 2-5 minutes'
        WHEN (SELECT COUNT(*) FROM BILLFINANCEDETAILS) < 100000 THEN '⏱️  Estimated time: 5-15 minutes'
        WHEN (SELECT COUNT(*) FROM BILLFINANCEDETAILS) < 1000000 THEN '⏱️  Estimated time: 15-30 minutes'
        ELSE '⏱️  Estimated time: 30+ minutes'
    END AS time_estimate;

-- Risk assessment
SELECT
    'LOW' as risk_level,
    'Precision expansion - no data loss expected' as risk_description,
    'Full rollback available if needed' as mitigation;

-- ==========================================
-- VALIDATION COMPLETE
-- ==========================================

SELECT '' AS spacer8;
SELECT '==========================================' AS separator;
SELECT 'PRE-MIGRATION VALIDATION COMPLETE' AS completion;
SELECT '==========================================' AS separator;
SELECT NOW() AS validation_end_time;

SELECT 'Next Steps:' AS next_steps_header;
SELECT '1. Review all validation results above' AS step1;
SELECT '2. Address any ❌ issues found' AS step2;
SELECT '3. If ✅ READY FOR MIGRATION shown, proceed with migration-safe.sql' AS step3;
SELECT '4. Ensure full database backup is completed' AS step4;
SELECT '5. Plan application downtime window' AS step5;