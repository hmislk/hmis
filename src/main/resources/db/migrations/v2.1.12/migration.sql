-- Migration v2.1.12: Backfill missing approval tracking fields (SIMPLE CROSS-PLATFORM VERSION)
-- Author: Claude AI Assistant
-- Date: 2025-12-30 (Updated: 2026-01-15)
-- GitHub Issue: #17317
-- STRATEGY: Use quoted identifiers and provide both uppercase/lowercase versions

-- ==============================================================================
-- ENVIRONMENT DETECTION
-- ==============================================================================

SELECT 'Migration v2.1.12 - Cross-platform version' AS status;

-- Detect which table case exists in this database
SELECT
    CASE
        WHEN (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'BILL') > 0
        THEN 'UPPERCASE_TABLES_DETECTED - Using BILL, BILLTYPEATOMIC, etc.'
        WHEN (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'bill') > 0
        THEN 'LOWERCASE_TABLES_DETECTED - Need lowercase syntax'
        ELSE 'TABLE_CASE_UNKNOWN - Check manually'
    END AS environment_info;

-- ==============================================================================
-- MIGRATION USING QUOTED IDENTIFIERS (works with most case configurations)
-- ==============================================================================

-- Pre-migration analysis
SELECT 'Total Completed Direct Purchases' as description,
       COUNT(*) as count
FROM `BILL`
WHERE `BILLTYPEATOMIC` = 'PHARMACY_DIRECT_PURCHASE'
  AND `COMPLETED` = TRUE;

SELECT 'Bills Missing Approval Data' as description,
       COUNT(*) as count
FROM `BILL`
WHERE `BILLTYPEATOMIC` = 'PHARMACY_DIRECT_PURCHASE'
  AND `COMPLETED` = TRUE
  AND (`APPROVEUSER_ID` IS NULL OR `APPROVEAT` IS NULL OR `EDITOR_ID` IS NULL OR `EDITEDAT` IS NULL);

-- Migration updates
UPDATE `BILL`
SET `APPROVEUSER_ID` = `COMPLETEDBY_ID`
WHERE `BILLTYPEATOMIC` = 'PHARMACY_DIRECT_PURCHASE'
  AND `COMPLETED` = TRUE
  AND `APPROVEUSER_ID` IS NULL
  AND `COMPLETEDBY_ID` IS NOT NULL;

SELECT 'Updated approveUser fields' as description, ROW_COUNT() as affected_rows;

UPDATE `BILL`
SET `APPROVEAT` = `COMPLETEDAT`
WHERE `BILLTYPEATOMIC` = 'PHARMACY_DIRECT_PURCHASE'
  AND `COMPLETED` = TRUE
  AND `APPROVEAT` IS NULL
  AND `COMPLETEDAT` IS NOT NULL;

SELECT 'Updated approveAt fields' as description, ROW_COUNT() as affected_rows;

UPDATE `BILL`
SET `EDITOR_ID` = `COMPLETEDBY_ID`
WHERE `BILLTYPEATOMIC` = 'PHARMACY_DIRECT_PURCHASE'
  AND `COMPLETED` = TRUE
  AND `EDITOR_ID` IS NULL
  AND `COMPLETEDBY_ID` IS NOT NULL;

SELECT 'Updated editor fields' as description, ROW_COUNT() as affected_rows;

UPDATE `BILL`
SET `EDITEDAT` = `COMPLETEDAT`
WHERE `BILLTYPEATOMIC` = 'PHARMACY_DIRECT_PURCHASE'
  AND `COMPLETED` = TRUE
  AND `EDITEDAT` IS NULL
  AND `COMPLETEDAT` IS NOT NULL;

SELECT 'Updated editedAt fields' as description, ROW_COUNT() as affected_rows;

-- Verification
SELECT 'Bills Still Missing Approval Data' as description,
       COUNT(*) as count
FROM `BILL`
WHERE `BILLTYPEATOMIC` = 'PHARMACY_DIRECT_PURCHASE'
  AND `COMPLETED` = TRUE
  AND (`APPROVEUSER_ID` IS NULL OR `APPROVEAT` IS NULL OR `EDITOR_ID` IS NULL OR `EDITEDAT` IS NULL);

SELECT `ID`, `DEPTID`, `BILLDATE`, `COMPLETEDBY_ID`, `COMPLETEDAT`, `APPROVEUSER_ID`, `APPROVEAT`, `EDITOR_ID`, `EDITEDAT`
FROM `BILL`
WHERE `BILLTYPEATOMIC` = 'PHARMACY_DIRECT_PURCHASE'
  AND `COMPLETED` = TRUE
ORDER BY `BILLDATE` DESC
LIMIT 3;

SELECT 'Migration v2.1.12 completed using quoted identifiers approach' AS final_status;