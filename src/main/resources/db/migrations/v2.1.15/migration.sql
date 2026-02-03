-- Migration v2.1.15: Set default departmentType for items without one
-- Author: Dr M H B Ariyaratne
-- Date: 2026-02-03
-- Issue: #18359 - Items need departmentType for department type filter in reports
-- Context: Department type filter added to Consumption, Stock Transfers, Stock Ledger reports
--          requires items to have a departmentType set for filtering to work correctly

-- ==========================================
-- STEP 1: DIAGNOSTIC - CHECK CURRENT STATE
-- ==========================================

SELECT 'Migration v2.1.15 - Starting departmentType backfill for items' AS status;

-- Count items by DTYPE that are missing departmentType
SELECT 'BEFORE: Items missing departmentType by type' AS status;
SELECT
    DTYPE,
    COUNT(*) as total_items,
    SUM(CASE WHEN departmentType IS NULL OR departmentType = '' THEN 1 ELSE 0 END) as missing_department_type,
    SUM(CASE WHEN departmentType IS NOT NULL AND departmentType != '' THEN 1 ELSE 0 END) as has_department_type
FROM item
WHERE RETIRED = FALSE OR RETIRED IS NULL
GROUP BY DTYPE
ORDER BY DTYPE;

-- ==========================================
-- STEP 2: UPDATE PharmaceuticalItem -> Pharmacy
-- ==========================================

SELECT 'Updating PharmaceuticalItem items - setting departmentType to Pharmacy' AS status;

UPDATE item
SET departmentType = 'Pharmacy'
WHERE DTYPE = 'PharmaceuticalItem'
  AND (departmentType IS NULL OR departmentType = '');

SELECT CONCAT('PharmaceuticalItem update complete - ', ROW_COUNT(), ' rows affected') AS status;

-- ==========================================
-- STEP 3: UPDATE Amp (also pharmaceutical) -> Pharmacy
-- ==========================================

SELECT 'Updating Amp items - setting departmentType to Pharmacy' AS status;

UPDATE item
SET departmentType = 'Pharmacy'
WHERE DTYPE = 'Amp'
  AND (departmentType IS NULL OR departmentType = '');

SELECT CONCAT('Amp update complete - ', ROW_COUNT(), ' rows affected') AS status;

-- ==========================================
-- STEP 4: UPDATE Vmp (also pharmaceutical) -> Pharmacy
-- ==========================================

SELECT 'Updating Vmp items - setting departmentType to Pharmacy' AS status;

UPDATE item
SET departmentType = 'Pharmacy'
WHERE DTYPE = 'Vmp'
  AND (departmentType IS NULL OR departmentType = '');

SELECT CONCAT('Vmp update complete - ', ROW_COUNT(), ' rows affected') AS status;

-- ==========================================
-- STEP 5: UPDATE Vmpp (also pharmaceutical) -> Pharmacy
-- ==========================================

SELECT 'Updating Vmpp items - setting departmentType to Pharmacy' AS status;

UPDATE item
SET departmentType = 'Pharmacy'
WHERE DTYPE = 'Vmpp'
  AND (departmentType IS NULL OR departmentType = '');

SELECT CONCAT('Vmpp update complete - ', ROW_COUNT(), ' rows affected') AS status;

-- ==========================================
-- STEP 6: UPDATE Ampp (also pharmaceutical) -> Pharmacy
-- ==========================================

SELECT 'Updating Ampp items - setting departmentType to Pharmacy' AS status;

UPDATE item
SET departmentType = 'Pharmacy'
WHERE DTYPE = 'Ampp'
  AND (departmentType IS NULL OR departmentType = '');

SELECT CONCAT('Ampp update complete - ', ROW_COUNT(), ' rows affected') AS status;

-- ==========================================
-- STEP 7: UPDATE Vtm (also pharmaceutical) -> Pharmacy
-- ==========================================

SELECT 'Updating Vtm items - setting departmentType to Pharmacy' AS status;

UPDATE item
SET departmentType = 'Pharmacy'
WHERE DTYPE = 'Vtm'
  AND (departmentType IS NULL OR departmentType = '');

SELECT CONCAT('Vtm update complete - ', ROW_COUNT(), ' rows affected') AS status;

-- ==========================================
-- STEP 8: UPDATE Atm (also pharmaceutical) -> Pharmacy
-- ==========================================

SELECT 'Updating Atm items - setting departmentType to Pharmacy' AS status;

UPDATE item
SET departmentType = 'Pharmacy'
WHERE DTYPE = 'Atm'
  AND (departmentType IS NULL OR departmentType = '');

SELECT CONCAT('Atm update complete - ', ROW_COUNT(), ' rows affected') AS status;

-- ==========================================
-- STEP 9: VERIFICATION
-- ==========================================

SELECT 'AFTER: Verifying departmentType is now populated' AS status;
SELECT
    DTYPE,
    COUNT(*) as total_items,
    SUM(CASE WHEN departmentType IS NULL OR departmentType = '' THEN 1 ELSE 0 END) as still_missing,
    SUM(CASE WHEN departmentType IS NOT NULL AND departmentType != '' THEN 1 ELSE 0 END) as now_has_department_type
FROM item
WHERE RETIRED = FALSE OR RETIRED IS NULL
GROUP BY DTYPE
ORDER BY DTYPE;

-- Show distribution of departmentType values
SELECT 'Distribution of departmentType values after migration' AS status;
SELECT
    departmentType,
    COUNT(*) as item_count
FROM item
WHERE (RETIRED = FALSE OR RETIRED IS NULL)
  AND departmentType IS NOT NULL
  AND departmentType != ''
GROUP BY departmentType
ORDER BY item_count DESC;

-- ==========================================
-- STEP 10: FINAL SUCCESS CHECK
-- ==========================================

SELECT
    CASE
        WHEN (SELECT COUNT(*) FROM item
              WHERE DTYPE IN ('PharmaceuticalItem', 'Amp', 'Vmp', 'Vmpp', 'Ampp', 'Vtm', 'Atm')
                AND (RETIRED = FALSE OR RETIRED IS NULL)
                AND (departmentType IS NULL OR departmentType = '')) = 0
        THEN 'Migration v2.1.15 SUCCESS: All pharmaceutical items now have departmentType set to Pharmacy'
        ELSE CONCAT('Migration v2.1.15 PARTIAL: ',
                    (SELECT COUNT(*) FROM item
                     WHERE DTYPE IN ('PharmaceuticalItem', 'Amp', 'Vmp', 'Vmpp', 'Ampp', 'Vtm', 'Atm')
                       AND (RETIRED = FALSE OR RETIRED IS NULL)
                       AND (departmentType IS NULL OR departmentType = '')),
                    ' pharmaceutical items still missing departmentType - may need manual review')
    END as final_result;

SELECT 'Migration v2.1.15 completed - Department type filter in reports should now work correctly' AS final_status;
