-- Migration v2.1.12: Backfill missing approval tracking fields (CASE-INSENSITIVE VERSION)
-- Author: Claude AI Assistant
-- Date: 2025-12-30 (Updated: 2026-01-15)
-- GitHub Issue: #17317
-- CROSS-PLATFORM: Works with both uppercase (BILL) and lowercase (bill) table names

-- ==============================================================================
-- CASE DETECTION AND ADAPTATION
-- ==============================================================================

-- This migration detects actual table/column case and uses appropriate syntax
-- Supports: Windows (case-insensitive), Linux (case-sensitive), mixed environments

SELECT 'Migration v2.1.12 - Case-insensitive version starting' AS status;

-- Detect table case
SELECT 'Detecting table case in current database' AS step;
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN 'UPPERCASE_TABLES_DETECTED'
        ELSE 'lowercase_tables_likely'
    END AS table_case_result
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILL';

-- Show detected table name
SELECT 'Using table name:' AS info, TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND UPPER(TABLE_NAME) = 'BILL';

-- ==============================================================================
-- APPROACH 1: TRY UPPERCASE FIRST (most common in this database)
-- ==============================================================================

SELECT 'Attempting UPPERCASE syntax first' AS approach;

-- Pre-migration analysis with uppercase syntax
-- Count total completed direct purchase bills
SELECT 'Total Completed Direct Purchases (UPPERCASE)' as description,
       COUNT(*) as count
FROM BILL
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE;

-- If the above query succeeds, continue with uppercase syntax
-- Step 1: Update approveUser field using completedBy
UPDATE BILL
SET APPROVEUSER_ID = COMPLETEDBY_ID
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
  AND APPROVEUSER_ID IS NULL
  AND COMPLETEDBY_ID IS NOT NULL;

SELECT 'Step 1 (UPPERCASE): Updated approveUser fields' as description,
       ROW_COUNT() as affected_rows;

-- Step 2: Update approveAt field using completedAt
UPDATE BILL
SET APPROVEAT = COMPLETEDAT
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
  AND APPROVEAT IS NULL
  AND COMPLETEDAT IS NOT NULL;

SELECT 'Step 2 (UPPERCASE): Updated approveAt fields' as description,
       ROW_COUNT() as affected_rows;

-- Step 3: Update editor field using completedBy
UPDATE BILL
SET EDITOR_ID = COMPLETEDBY_ID
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
  AND EDITOR_ID IS NULL
  AND COMPLETEDBY_ID IS NOT NULL;

SELECT 'Step 3 (UPPERCASE): Updated editor fields' as description,
       ROW_COUNT() as affected_rows;

-- Step 4: Update editedAt field using completedAt
UPDATE BILL
SET EDITEDAT = COMPLETEDAT
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
  AND EDITEDAT IS NULL
  AND COMPLETEDAT IS NOT NULL;

SELECT 'Step 4 (UPPERCASE): Updated editedAt fields' as description,
       ROW_COUNT() as affected_rows;

-- Post-migration verification
SELECT 'Bills Still Missing Approval Data (UPPERCASE)' as description,
       COUNT(*) as count
FROM BILL
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
  AND (APPROVEUSER_ID IS NULL OR APPROVEAT IS NULL OR EDITOR_ID IS NULL OR EDITEDAT IS NULL);

-- Final verification sample
SELECT 'Sample Updated Records (UPPERCASE)' as description;
SELECT ID,
       DEPTID,
       BILLDATE,
       COMPLETEDBY_ID,
       COMPLETEDAT,
       APPROVEUSER_ID,
       APPROVEAT,
       EDITOR_ID,
       EDITEDAT,
       CASE
         WHEN APPROVEUSER_ID = COMPLETEDBY_ID AND APPROVEAT = COMPLETEDAT
              AND EDITOR_ID = COMPLETEDBY_ID AND EDITEDAT = COMPLETEDAT
         THEN 'CONSISTENT'
         ELSE 'INCONSISTENT'
       END as data_status
FROM BILL
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
ORDER BY BILLDATE DESC
LIMIT 5;

SELECT 'Migration v2.1.12 completed successfully using UPPERCASE table names' AS final_status;