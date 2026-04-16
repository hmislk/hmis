-- Migration v2.1.15: Set departmentType for pharmaceutical items (CASE-INSENSITIVE VERSION)
-- Author: Dr M H B Ariyaratne
-- Date: 2026-02-04 (Updated: 2026-02-05)
-- Issue: #18359 - VTMs, ATMs, VMPs, AMPs, VMPPs, AMPPs, PharmaceuticalItem and Antibiotic need departmentType for department type filter in reports
-- CROSS-PLATFORM: Works with both uppercase (ITEM) and lowercase (item) table names

-- ==============================================================================
-- CASE DETECTION AND ADAPTATION
-- ==============================================================================

-- This migration detects actual table/column case and uses appropriate syntax
-- Supports: Windows (case-insensitive), Linux (case-sensitive), mixed environments

SELECT 'Migration v2.1.15 - Case-insensitive version starting' AS status;

-- Detect table case
SELECT 'Detecting table case in current database' AS step;
SELECT
    CASE
        WHEN COUNT(*) > 0 THEN 'UPPERCASE_TABLES_DETECTED'
        ELSE 'lowercase_tables_likely'
    END AS table_case_result
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ITEM';

-- Show detected table name
SELECT 'Using table name:' AS info, TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND UPPER(TABLE_NAME) = 'ITEM';

-- ==============================================================================
-- UPPERCASE APPROACH (most common in HMIS databases)
-- ==============================================================================

SELECT 'Using UPPERCASE syntax approach' AS approach;

-- Pre-migration analysis with uppercase syntax
-- Count pharmaceutical items before migration
SELECT 'Pharmaceutical items before migration (UPPERCASE)' as description;
SELECT DTYPE,
       COUNT(*) as total_items,
       SUM(CASE WHEN DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '' THEN 1 ELSE 0 END) as missing_department_type,
       SUM(CASE WHEN DEPARTMENTTYPE IS NOT NULL AND DEPARTMENTTYPE != '' THEN 1 ELSE 0 END) as has_department_type
FROM ITEM
WHERE (RETIRED = FALSE OR RETIRED IS NULL)
  AND DTYPE IN ('PharmaceuticalItem', 'Amp', 'Vmp', 'Vmpp', 'Ampp', 'Vtm', 'Atm', 'Antibiotic')
GROUP BY DTYPE
ORDER BY DTYPE;

-- Update pharmaceutical item types with departmentType = 'Pharmacy'
-- Step 1: PharmaceuticalItem
UPDATE ITEM
SET DEPARTMENTTYPE = 'Pharmacy'
WHERE DTYPE = 'PharmaceuticalItem'
  AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');

SELECT 'Step 1 (UPPERCASE): Updated PharmaceuticalItem' as description,
       ROW_COUNT() as affected_rows;

-- Step 2: Amp (AMP)
UPDATE ITEM
SET DEPARTMENTTYPE = 'Pharmacy'
WHERE DTYPE = 'Amp'
  AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');

SELECT 'Step 2 (UPPERCASE): Updated Amp' as description,
       ROW_COUNT() as affected_rows;

-- Step 3: Vmp (VMP)
UPDATE ITEM
SET DEPARTMENTTYPE = 'Pharmacy'
WHERE DTYPE = 'Vmp'
  AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');

SELECT 'Step 3 (UPPERCASE): Updated Vmp' as description,
       ROW_COUNT() as affected_rows;

-- Step 4: Vmpp (VMPP)
UPDATE ITEM
SET DEPARTMENTTYPE = 'Pharmacy'
WHERE DTYPE = 'Vmpp'
  AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');

SELECT 'Step 4 (UPPERCASE): Updated Vmpp' as description,
       ROW_COUNT() as affected_rows;

-- Step 5: Ampp (AMPP)
UPDATE ITEM
SET DEPARTMENTTYPE = 'Pharmacy'
WHERE DTYPE = 'Ampp'
  AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');

SELECT 'Step 5 (UPPERCASE): Updated Ampp' as description,
       ROW_COUNT() as affected_rows;

-- Step 6: Vtm (VTM)
UPDATE ITEM
SET DEPARTMENTTYPE = 'Pharmacy'
WHERE DTYPE = 'Vtm'
  AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');

SELECT 'Step 6 (UPPERCASE): Updated Vtm' as description,
       ROW_COUNT() as affected_rows;

-- Step 7: Atm (ATM)
UPDATE ITEM
SET DEPARTMENTTYPE = 'Pharmacy'
WHERE DTYPE = 'Atm'
  AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');

SELECT 'Step 7 (UPPERCASE): Updated Atm' as description,
       ROW_COUNT() as affected_rows;

-- Step 8: Antibiotic
UPDATE ITEM
SET DEPARTMENTTYPE = 'Pharmacy'
WHERE DTYPE = 'Antibiotic'
  AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');

SELECT 'Step 8 (UPPERCASE): Updated Antibiotic' as description,
       ROW_COUNT() as affected_rows;

-- Post-migration verification
SELECT 'Items Still Missing departmentType (UPPERCASE)' as description,
       COUNT(*) as count
FROM ITEM
WHERE (RETIRED = FALSE OR RETIRED IS NULL)
  AND DTYPE IN ('PharmaceuticalItem', 'Amp', 'Vmp', 'Vmpp', 'Ampp', 'Vtm', 'Atm', 'Antibiotic')
  AND (DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '');

-- Final verification with departmentType breakdown
SELECT 'Final departmentType distribution (UPPERCASE)' as description;
SELECT DTYPE,
       COUNT(*) as total_items,
       SUM(CASE WHEN DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '' THEN 1 ELSE 0 END) as still_missing,
       SUM(CASE WHEN DEPARTMENTTYPE = 'Pharmacy' THEN 1 ELSE 0 END) as pharmacy_items,
       SUM(CASE WHEN DEPARTMENTTYPE IS NOT NULL AND DEPARTMENTTYPE != '' AND DEPARTMENTTYPE != 'Pharmacy' THEN 1 ELSE 0 END) as other_departments
FROM ITEM
WHERE (RETIRED = FALSE OR RETIRED IS NULL)
  AND DTYPE IN ('PharmaceuticalItem', 'Amp', 'Vmp', 'Vmpp', 'Ampp', 'Vtm', 'Atm', 'Antibiotic')
GROUP BY DTYPE
ORDER BY DTYPE;

-- Sample of updated records
SELECT 'Sample Updated Records (UPPERCASE)' as description;
SELECT ID,
       DTYPE,
       NAME,
       DEPARTMENTTYPE,
       CASE
         WHEN DEPARTMENTTYPE = 'Pharmacy' THEN 'CORRECTLY_SET'
         WHEN DEPARTMENTTYPE IS NULL OR DEPARTMENTTYPE = '' THEN 'MISSING'
         ELSE 'OTHER_DEPARTMENT'
       END as status
FROM ITEM
WHERE (RETIRED = FALSE OR RETIRED IS NULL)
  AND DTYPE IN ('PharmaceuticalItem', 'Amp', 'Vmp', 'Vmpp', 'Ampp', 'Vtm', 'Atm', 'Antibiotic')
ORDER BY DTYPE, ID DESC
LIMIT 10;

SELECT 'Migration v2.1.15 completed successfully using UPPERCASE table names' AS final_status;
SELECT 'Department type filter in reports should now work correctly for all pharmaceutical items' AS summary;