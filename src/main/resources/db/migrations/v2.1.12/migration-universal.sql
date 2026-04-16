-- Migration v2.1.12: Backfill missing approval tracking fields (UNIVERSAL VERSION)
-- Author: Claude AI Assistant
-- Date: 2025-12-30 (Updated: 2026-01-15)
-- GitHub Issue: #17317
-- UNIVERSAL: Works on ANY MySQL case sensitivity configuration

-- ==============================================================================
-- UNIVERSAL APPROACH USING PREPARED STATEMENTS
-- ==============================================================================

-- This migration detects actual table and column names and builds appropriate SQL
-- Works on: Windows (case-insensitive), Linux (case-sensitive), any MySQL config

SELECT 'Migration v2.1.12 - Universal case-insensitive version starting' AS status;

-- ==============================================================================
-- STEP 1: DETECT ACTUAL TABLE AND COLUMN NAMES
-- ==============================================================================

-- Get the actual table name (could be BILL, bill, or Bill)
SET @bill_table = (
    SELECT TABLE_NAME
    FROM INFORMATION_SCHEMA.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND UPPER(TABLE_NAME) = 'BILL'
    LIMIT 1
);

-- Get actual column names for bill type atomic
SET @billtypeatomic_col = (
    SELECT COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND (UPPER(COLUMN_NAME) = 'BILLTYPEATOMIC' OR UPPER(COLUMN_NAME) = 'BILL_TYPE_ATOMIC')
    LIMIT 1
);

-- Get actual column names for completion fields
SET @completed_col = (
    SELECT COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND UPPER(COLUMN_NAME) = 'COMPLETED'
    LIMIT 1
);

SET @completedby_col = (
    SELECT COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND (UPPER(COLUMN_NAME) = 'COMPLETEDBY_ID' OR UPPER(COLUMN_NAME) = 'COMPLETED_BY')
    LIMIT 1
);

SET @completedat_col = (
    SELECT COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND (UPPER(COLUMN_NAME) = 'COMPLETEDAT' OR UPPER(COLUMN_NAME) = 'COMPLETED_AT')
    LIMIT 1
);

-- Get actual column names for approval fields
SET @approveuser_col = (
    SELECT COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND (UPPER(COLUMN_NAME) = 'APPROVEUSER_ID' OR UPPER(COLUMN_NAME) = 'APPROVE_USER')
    LIMIT 1
);

SET @approveat_col = (
    SELECT COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND (UPPER(COLUMN_NAME) = 'APPROVEAT' OR UPPER(COLUMN_NAME) = 'APPROVE_AT')
    LIMIT 1
);

SET @editor_col = (
    SELECT COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND (UPPER(COLUMN_NAME) = 'EDITOR_ID' OR UPPER(COLUMN_NAME) = 'EDITOR')
    LIMIT 1
);

SET @editedat_col = (
    SELECT COLUMN_NAME
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = @bill_table
      AND (UPPER(COLUMN_NAME) = 'EDITEDAT' OR UPPER(COLUMN_NAME) = 'EDITED_AT')
    LIMIT 1
);

-- Display detected names for verification
SELECT 'Detected table and column names:' AS info;
SELECT @bill_table AS table_name,
       @billtypeatomic_col AS billtypeatomic_column,
       @completed_col AS completed_column,
       @completedby_col AS completedby_column,
       @completedat_col AS completedat_column,
       @approveuser_col AS approveuser_column,
       @approveat_col AS approveat_column,
       @editor_col AS editor_column,
       @editedat_col AS editedat_column;

-- ==============================================================================
-- STEP 2: PRE-MIGRATION ANALYSIS USING DETECTED NAMES
-- ==============================================================================

-- Build and execute dynamic query for total count
SET @sql_count = CONCAT(
    'SELECT ''Total Completed Direct Purchases'' as description, COUNT(*) as count FROM ',
    @bill_table,
    ' WHERE ', @billtypeatomic_col, ' = ''PHARMACY_DIRECT_PURCHASE'' AND ', @completed_col, ' = TRUE'
);

PREPARE stmt FROM @sql_count;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Build and execute dynamic query for missing approval data count
SET @sql_missing = CONCAT(
    'SELECT ''Bills Missing Approval Data'' as description, COUNT(*) as count FROM ',
    @bill_table,
    ' WHERE ', @billtypeatomic_col, ' = ''PHARMACY_DIRECT_PURCHASE'' AND ', @completed_col, ' = TRUE AND (',
    @approveuser_col, ' IS NULL OR ', @approveat_col, ' IS NULL OR ', @editor_col, ' IS NULL OR ', @editedat_col, ' IS NULL)'
);

PREPARE stmt FROM @sql_missing;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ==============================================================================
-- STEP 3: MIGRATION UPDATES USING DETECTED NAMES
-- ==============================================================================

-- Step 1: Update approveUser field using completedBy
SET @sql_update1 = CONCAT(
    'UPDATE ', @bill_table,
    ' SET ', @approveuser_col, ' = ', @completedby_col,
    ' WHERE ', @billtypeatomic_col, ' = ''PHARMACY_DIRECT_PURCHASE''',
    ' AND ', @completed_col, ' = TRUE',
    ' AND ', @approveuser_col, ' IS NULL',
    ' AND ', @completedby_col, ' IS NOT NULL'
);

PREPARE stmt FROM @sql_update1;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Step 1: Updated approveUser fields' as description, ROW_COUNT() as affected_rows;

-- Step 2: Update approveAt field using completedAt
SET @sql_update2 = CONCAT(
    'UPDATE ', @bill_table,
    ' SET ', @approveat_col, ' = ', @completedat_col,
    ' WHERE ', @billtypeatomic_col, ' = ''PHARMACY_DIRECT_PURCHASE''',
    ' AND ', @completed_col, ' = TRUE',
    ' AND ', @approveat_col, ' IS NULL',
    ' AND ', @completedat_col, ' IS NOT NULL'
);

PREPARE stmt FROM @sql_update2;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Step 2: Updated approveAt fields' as description, ROW_COUNT() as affected_rows;

-- Step 3: Update editor field using completedBy
SET @sql_update3 = CONCAT(
    'UPDATE ', @bill_table,
    ' SET ', @editor_col, ' = ', @completedby_col,
    ' WHERE ', @billtypeatomic_col, ' = ''PHARMACY_DIRECT_PURCHASE''',
    ' AND ', @completed_col, ' = TRUE',
    ' AND ', @editor_col, ' IS NULL',
    ' AND ', @completedby_col, ' IS NOT NULL'
);

PREPARE stmt FROM @sql_update3;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Step 3: Updated editor fields' as description, ROW_COUNT() as affected_rows;

-- Step 4: Update editedAt field using completedAt
SET @sql_update4 = CONCAT(
    'UPDATE ', @bill_table,
    ' SET ', @editedat_col, ' = ', @completedat_col,
    ' WHERE ', @billtypeatomic_col, ' = ''PHARMACY_DIRECT_PURCHASE''',
    ' AND ', @completed_col, ' = TRUE',
    ' AND ', @editedat_col, ' IS NULL',
    ' AND ', @completedat_col, ' IS NOT NULL'
);

PREPARE stmt FROM @sql_update4;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Step 4: Updated editedAt fields' as description, ROW_COUNT() as affected_rows;

-- ==============================================================================
-- STEP 4: POST-MIGRATION VERIFICATION
-- ==============================================================================

-- Count remaining bills with missing approval data
SET @sql_verify = CONCAT(
    'SELECT ''Bills Still Missing Approval Data'' as description, COUNT(*) as count FROM ',
    @bill_table,
    ' WHERE ', @billtypeatomic_col, ' = ''PHARMACY_DIRECT_PURCHASE'' AND ', @completed_col, ' = TRUE AND (',
    @approveuser_col, ' IS NULL OR ', @approveat_col, ' IS NULL OR ', @editor_col, ' IS NULL OR ', @editedat_col, ' IS NULL)'
);

PREPARE stmt FROM @sql_verify;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Show sample of updated records
SELECT 'Migration v2.1.12 completed successfully using UNIVERSAL approach' AS final_status;
SELECT 'Approach: Detected table/column names and used prepared statements' AS method;