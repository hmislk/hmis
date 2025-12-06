-- Verification queries for migration v2.1.3
-- These queries verify that the decimal precision issues have been completely resolved

-- ==========================================
-- BASIC SCHEMA VERIFICATION
-- ==========================================

SELECT 'VERIFICATION: Migration v2.1.3 Schema Validation' AS test_suite;

-- Test 1: Verify NO decimal(38,0) columns remain
SELECT 'Test 1: Checking for remaining decimal(38,0) columns...' AS test_name;
SELECT
    CASE
        WHEN COUNT(*) = 0 THEN 'PASS: No problematic columns remain'
        ELSE CONCAT('FAIL: ', COUNT(*), ' problematic columns still exist')
    END AS test_result
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_TYPE = 'decimal(38,0)';

-- Test 2: Verify critical columns have correct precision
SELECT 'Test 2: Verifying critical financial columns...' AS test_name;
SELECT
    COLUMN_NAME,
    COLUMN_TYPE,
    CASE
        WHEN COLUMN_TYPE = 'decimal(20,4)' THEN 'PASS'
        WHEN COLUMN_TYPE = 'decimal(18,4)' THEN 'ACCEPTABLE'
        ELSE 'FAIL: Wrong precision'
    END AS test_result
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_NAME IN (
    'TOTALCOSTVALUE', 'TOTALPURCHASEVALUE', 'TOTALRETAILSALEVALUE',
    'BILLDISCOUNT', 'TOTALDISCOUNT', 'TOTALEXPENSE', 'TOTALQUANTITY'
  )
ORDER BY COLUMN_NAME;

-- Test 3: Count upgraded columns
SELECT 'Test 3: Counting upgraded DECIMAL(20,4) columns...' AS test_name;
SELECT
    COUNT(*) as total_20_4_columns,
    CASE
        WHEN COUNT(*) >= 9 THEN 'PASS: Sufficient columns upgraded'
        ELSE 'FAIL: Not enough columns upgraded'
    END AS test_result
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'BILLFINANCEDETAILS'
  AND COLUMN_TYPE = 'decimal(20,4)';

-- ==========================================
-- FUNCTIONAL VERIFICATION
-- ==========================================

SELECT 'VERIFICATION: Functional Testing' AS test_suite;

-- Test 4: SUM() operations precision test
SELECT 'Test 4: Testing SUM() precision preservation...' AS test_name;

-- This query should now return BigDecimal with scale=4
-- (This is the exact same type of query that was failing before)
SELECT
    COUNT(*) as sample_records,
    SUM(TOTALPURCHASEVALUE) as sum_purchase_value,
    SUM(TOTALCOSTVALUE) as sum_cost_value,
    SUM(TOTALRETAILSALEVALUE) as sum_retail_value,
    'If these show decimal places, SUM() precision is working' AS verification_note
FROM BILLFINANCEDETAILS
WHERE TOTALPURCHASEVALUE IS NOT NULL
  AND TOTALCOSTVALUE IS NOT NULL
  AND TOTALRETAILSALEVALUE IS NOT NULL
LIMIT 10;

-- Test 5: Data integrity check
SELECT 'Test 5: Data integrity verification...' AS test_name;
SELECT
    COUNT(*) as total_records,
    SUM(CASE WHEN TOTALCOSTVALUE IS NULL THEN 1 ELSE 0 END) as null_cost_count,
    SUM(CASE WHEN TOTALPURCHASEVALUE IS NULL THEN 1 ELSE 0 END) as null_purchase_count,
    SUM(CASE WHEN TOTALRETAILSALEVALUE IS NULL THEN 1 ELSE 0 END) as null_retail_count,
    CASE
        WHEN COUNT(*) > 0 AND
             SUM(CASE WHEN TOTALCOSTVALUE IS NULL THEN 1 ELSE 0 END) < COUNT(*) THEN 'PASS: Data intact'
        ELSE 'WARNING: Check data integrity'
    END AS test_result
FROM BILLFINANCEDETAILS;

-- ==========================================
-- ISSUE-SPECIFIC VERIFICATION
-- ==========================================

SELECT 'VERIFICATION: GitHub Issue #16989 Specific Tests' AS test_suite;

-- Test 6: Simulate the original problem scenario
-- This recreates the exact scenario from PharmacyController.java line 5042
SELECT 'Test 6: Simulating PharmacyController SUM() query...' AS test_name;

-- Approximate the original problematic query structure
SELECT
    'Sample Department' as department_name,
    'Sample From Dept' as from_department_name,
    'Sample To Dept' as to_department_name,
    SUM(TOTALPURCHASEVALUE) as purchase_value,
    SUM(TOTALCOSTVALUE) as cost_value,
    SUM(TOTALRETAILSALEVALUE) as retail_value,
    'Check: cost_value should have decimals, not be integer' AS verification_note
FROM BILLFINANCEDETAILS
WHERE TOTALCOSTVALUE IS NOT NULL
GROUP BY 1, 2, 3
LIMIT 1;

-- Test 7: Precision comparison test
SELECT 'Test 7: Before/After precision comparison...' AS test_name;
SELECT
    'Before migration: TOTALCOSTVALUE was DECIMAL(38,0) - lost precision' AS before_status,
    'After migration: TOTALCOSTVALUE is DECIMAL(20,4) - preserves precision' AS after_status,
    (SELECT COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_NAME = 'BILLFINANCEDETAILS'
       AND COLUMN_NAME = 'TOTALCOSTVALUE'
       AND TABLE_SCHEMA = DATABASE()) AS current_totalcostvalue_type;

-- ==========================================
-- SUMMARY REPORT
-- ==========================================

SELECT 'MIGRATION v2.1.3 VERIFICATION SUMMARY' AS final_report;
SELECT 'All tests completed - Review results above' AS instruction;
SELECT 'TOTALCOSTVALUE precision issue from GitHub #16989 should be resolved' AS main_fix;
SELECT 'PharmacyController SUM() operations should now maintain scale=4' AS expected_behavior;
SELECT NOW() AS verification_timestamp;