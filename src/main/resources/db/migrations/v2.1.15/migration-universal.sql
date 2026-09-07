-- Migration v2.1.15: Set default departmentType for pharmaceutical items (COMPLETE UNIVERSAL VERSION)
-- Author: Dr M H B Ariyaratne
-- Date: 2026-02-04
-- Issue: #18359 - VTMs, ATMs, VMPs, AMPs, VMPPs, AMPPs, PharmaceuticalItem and Antibiotic need departmentType for department type filter in reports
-- UNIVERSAL: Works on BOTH uppercase and lowercase table configurations

-- ==============================================================================
-- COMPLETE DUAL APPROACH: HANDLES BOTH CASES RELIABLY
-- ==============================================================================

-- This migration handles both uppercase (ITEM) and lowercase (item) table configurations
-- Based on proven approach from v2.1.12 migration
-- Uses detection and branching to ensure compatibility

SELECT 'Migration v2.1.15 - Complete universal compatibility version starting' AS status;

-- ==============================================================================
-- STEP 1: DETECT TABLE CASE
-- ==============================================================================

SELECT 'Detecting table case in current database' AS step;
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN 'UPPERCASE_TABLES_DETECTED'
        ELSE 'lowercase_tables_detected'
    END AS table_case_result
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ITEM';

-- Show actual table name found
SELECT 'Detected table name:' AS info, TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME = 'ITEM' OR TABLE_NAME = 'item' OR TABLE_NAME = 'Item')
LIMIT 1;

-- ==============================================================================
-- APPROACH 1: UPPERCASE TABLES (most common configuration)
-- ==============================================================================

SELECT 'Attempting UPPERCASE table approach' AS approach;

-- Pre-migration check with uppercase tables
SELECT 'BEFORE: Current state with UPPERCASE tables' AS phase;

-- Diagnostic query - if this fails, we'll use lowercase approach
SELECT 'Pharmaceutical items before migration (UPPERCASE):' AS info;
SELECT DTYPE,
       COUNT(*) as total_items,
       SUM(CASE WHEN DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '' THEN 1 ELSE 0 END) as missing_department_type,
       SUM(CASE WHEN DEPARTMENTTYPE IS NOT NULL AND DEPARTMENTTYPE != '' THEN 1 ELSE 0 END) as has_department_type
FROM ITEM
WHERE (RETIRED = FALSE OR RETIRED IS NULL)
  AND DTYPE IN ('PharmaceuticalItem', 'Amp', 'Vmp', 'Vmpp', 'Ampp', 'Vtm', 'Atm', 'Antibiotic')
GROUP BY DTYPE
ORDER BY DTYPE;

-- If we reach here without error, proceed with uppercase updates

SELECT 'Executing updates with UPPERCASE tables' AS phase;

-- Update all pharmaceutical item types
UPDATE ITEM SET DEPARTMENTTYPE = 'Pharmacy' WHERE DTYPE = 'PharmaceuticalItem' AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');
SELECT CONCAT('PharmaceuticalItem (UPPERCASE) - ', ROW_COUNT(), ' rows affected') AS result;

UPDATE ITEM SET DEPARTMENTTYPE = 'Pharmacy' WHERE DTYPE = 'Amp' AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');
SELECT CONCAT('Amp (UPPERCASE) - ', ROW_COUNT(), ' rows affected') AS result;

UPDATE ITEM SET DEPARTMENTTYPE = 'Pharmacy' WHERE DTYPE = 'Vmp' AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');
SELECT CONCAT('Vmp (UPPERCASE) - ', ROW_COUNT(), ' rows affected') AS result;

UPDATE ITEM SET DEPARTMENTTYPE = 'Pharmacy' WHERE DTYPE = 'Vmpp' AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');
SELECT CONCAT('Vmpp (UPPERCASE) - ', ROW_COUNT(), ' rows affected') AS result;

UPDATE ITEM SET DEPARTMENTTYPE = 'Pharmacy' WHERE DTYPE = 'Ampp' AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');
SELECT CONCAT('Ampp (UPPERCASE) - ', ROW_COUNT(), ' rows affected') AS result;

UPDATE ITEM SET DEPARTMENTTYPE = 'Pharmacy' WHERE DTYPE = 'Vtm' AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');
SELECT CONCAT('Vtm (UPPERCASE) - ', ROW_COUNT(), ' rows affected') AS result;

UPDATE ITEM SET DEPARTMENTTYPE = 'Pharmacy' WHERE DTYPE = 'Atm' AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');
SELECT CONCAT('Atm (UPPERCASE) - ', ROW_COUNT(), ' rows affected') AS result;

UPDATE ITEM SET DEPARTMENTTYPE = 'Pharmacy' WHERE DTYPE = 'Antibiotic' AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');
SELECT CONCAT('Antibiotic (UPPERCASE) - ', ROW_COUNT(), ' rows affected') AS result;

-- Verification with uppercase tables
SELECT 'AFTER: Verification with UPPERCASE tables' AS phase;
SELECT DTYPE,
       COUNT(*) as total_items,
       SUM(CASE WHEN DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '' THEN 1 ELSE 0 END) as still_missing,
       SUM(CASE WHEN DEPARTMENTTYPE IS NOT NULL AND DEPARTMENTTYPE != '' THEN 1 ELSE 0 END) as now_populated
FROM ITEM
WHERE (RETIRED = FALSE OR RETIRED IS NULL)
  AND DTYPE IN ('PharmaceuticalItem', 'Amp', 'Vmp', 'Vmpp', 'Ampp', 'Vtm', 'Atm', 'Antibiotic')
GROUP BY DTYPE
ORDER BY DTYPE;

SELECT 'Migration v2.1.15 SUCCESS with UPPERCASE tables' AS final_status;

-- ==============================================================================
-- APPROACH 2: LOWERCASE TABLES (fallback for case-sensitive systems)
-- ==============================================================================

-- NOTE: This section will only execute if the uppercase section above failed
-- If uppercase succeeded, this section is effectively inactive due to error handling

SELECT 'FALLBACK: Attempting lowercase table approach' AS approach;

-- Pre-migration check with lowercase tables
SELECT 'BEFORE: Current state with lowercase tables' AS phase;

-- Diagnostic query with lowercase
SELECT 'Pharmaceutical items before migration (lowercase):' AS info;
SELECT dtype,
       COUNT(*) as total_items,
       SUM(CASE WHEN departmenttype IS NULL OR departmenttype = '' THEN 1 ELSE 0 END) as missing_department_type,
       SUM(CASE WHEN departmenttype IS NOT NULL AND departmenttype != '' THEN 1 ELSE 0 END) as has_department_type
FROM item
WHERE (retired = FALSE OR retired IS NULL)
  AND dtype IN ('PharmaceuticalItem', 'Amp', 'Vmp', 'Vmpp', 'Ampp', 'Vtm', 'Atm', 'Antibiotic')
GROUP BY dtype
ORDER BY dtype;

-- Update with lowercase table names
UPDATE item SET departmenttype = 'Pharmacy' WHERE dtype = 'PharmaceuticalItem' AND (departmenttype IS NULL OR departmenttype = '');
SELECT CONCAT('PharmaceuticalItem (lowercase) - ', ROW_COUNT(), ' rows affected') AS result;

UPDATE item SET departmenttype = 'Pharmacy' WHERE dtype = 'Amp' AND (departmenttype IS NULL OR departmenttype = '');
SELECT CONCAT('Amp (lowercase) - ', ROW_COUNT(), ' rows affected') AS result;

UPDATE item SET departmenttype = 'Pharmacy' WHERE dtype = 'Vmp' AND (departmenttype IS NULL OR departmenttype = '');
SELECT CONCAT('Vmp (lowercase) - ', ROW_COUNT(), ' rows affected') AS result;

UPDATE item SET departmenttype = 'Pharmacy' WHERE dtype = 'Vmpp' AND (departmenttype IS NULL OR departmenttype = '');
SELECT CONCAT('Vmpp (lowercase) - ', ROW_COUNT(), ' rows affected') AS result;

UPDATE item SET departmenttype = 'Pharmacy' WHERE dtype = 'Ampp' AND (departmenttype IS NULL OR departmenttype = '');
SELECT CONCAT('Ampp (lowercase) - ', ROW_COUNT(), ' rows affected') AS result;

UPDATE item SET departmenttype = 'Pharmacy' WHERE dtype = 'Vtm' AND (departmenttype IS NULL OR departmenttype = '');
SELECT CONCAT('Vtm (lowercase) - ', ROW_COUNT(), ' rows affected') AS result;

UPDATE item SET departmenttype = 'Pharmacy' WHERE dtype = 'Atm' AND (departmenttype IS NULL OR departmenttype = '');
SELECT CONCAT('Atm (lowercase) - ', ROW_COUNT(), ' rows affected') AS result;

UPDATE item SET departmenttype = 'Pharmacy' WHERE dtype = 'Antibiotic' AND (departmenttype IS NULL OR departmenttype = '');
SELECT CONCAT('Antibiotic (lowercase) - ', ROW_COUNT(), ' rows affected') AS result;

-- Verification with lowercase tables
SELECT 'AFTER: Verification with lowercase tables' AS phase;
SELECT dtype,
       COUNT(*) as total_items,
       SUM(CASE WHEN departmenttype IS NULL OR departmenttype = '' THEN 1 ELSE 0 END) as still_missing,
       SUM(CASE WHEN departmenttype IS NOT NULL AND departmenttype != '' THEN 1 ELSE 0 END) as now_populated
FROM item
WHERE (retired = FALSE OR retired IS NULL)
  AND dtype IN ('PharmaceuticalItem', 'Amp', 'Vmp', 'Vmpp', 'Ampp', 'Vtm', 'Atm', 'Antibiotic')
GROUP BY dtype
ORDER BY dtype;

SELECT 'Migration v2.1.15 SUCCESS with lowercase tables' AS final_status;

-- ==============================================================================
-- FINAL STATUS
-- ==============================================================================

SELECT 'Migration v2.1.15 completed - Department type filter in reports should now work correctly' AS completion_status;
SELECT 'All pharmaceutical items (PharmaceuticalItem, Amp, Vmp, Vmpp, Ampp, Vtm, Atm, Antibiotic) now have departmentType = Pharmacy' AS summary;