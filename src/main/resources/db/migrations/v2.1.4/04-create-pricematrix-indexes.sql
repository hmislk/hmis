-- ==========================================
-- SCRIPT 4: CREATE PRICEMATRIX INDEXES
-- ==========================================
-- Purpose: Create indexes on PRICEMATRIX for settle button optimization
-- Safe to run: YES (uses IF NOT EXISTS)
-- Can be run separately: YES
-- Idempotent: YES (safe to re-run)

SELECT 'Creating PRICEMATRIX Indexes - Script 4 of 5' AS status;
SELECT NOW() AS script_start_time;

-- The Settle button recalculates discounts for all bill items
-- These queries look up price matrices by payment scheme, department, category, and retired status
-- Optimizing these lookups significantly improves settle button performance

-- Create composite index for payment scheme + department + category lookups on uppercase table
CREATE INDEX IF NOT EXISTS idx_pricematrix_payment_dept_category
ON PRICEMATRIX(PAYMENTSCHEME_ID, DEPARTMENT_ID, CATEGORY_ID, RETIRED);

SELECT 'Index idx_pricematrix_payment_dept_category created on PRICEMATRIX (if table exists)' AS uppercase_pm_payment_dept_category_result;

-- Create composite index for payment scheme + department + category lookups on lowercase table
CREATE INDEX IF NOT EXISTS idx_pricematrix_payment_dept_category
ON pricematrix(PAYMENTSCHEME_ID, DEPARTMENT_ID, CATEGORY_ID, RETIRED);

SELECT 'Index idx_pricematrix_payment_dept_category created on pricematrix (if table exists)' AS lowercase_pm_payment_dept_category_result;

-- Create composite index for payment scheme + category lookups (without department filter) on uppercase table
CREATE INDEX IF NOT EXISTS idx_pricematrix_payment_category
ON PRICEMATRIX(PAYMENTSCHEME_ID, CATEGORY_ID, RETIRED);

SELECT 'Index idx_pricematrix_payment_category created on PRICEMATRIX (if table exists)' AS uppercase_pm_payment_category_result;

-- Create composite index for payment scheme + category lookups (without department filter) on lowercase table
CREATE INDEX IF NOT EXISTS idx_pricematrix_payment_category
ON pricematrix(PAYMENTSCHEME_ID, CATEGORY_ID, RETIRED);

SELECT 'Index idx_pricematrix_payment_category created on pricematrix (if table exists)' AS lowercase_pm_payment_category_result;

-- ==========================================
-- VERIFICATION
-- ==========================================

SELECT 'Verifying PRICEMATRIX indexes...' AS progress;

-- Verify indexes were created successfully
SET @pm_indexes_created = (
    SELECT COUNT(DISTINCT INDEX_NAME)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND (TABLE_NAME = 'PRICEMATRIX' OR TABLE_NAME = 'pricematrix')
      AND INDEX_NAME IN ('idx_pricematrix_payment_dept_category', 'idx_pricematrix_payment_category')
);

SELECT
    CASE WHEN @pm_indexes_created >= 2
         THEN CONCAT('✓ SUCCESS: ', @pm_indexes_created, ' PRICEMATRIX indexes created')
         ELSE CONCAT('⚠️ WARNING: Only ', @pm_indexes_created, ' of 2 expected indexes created')
    END AS pm_index_verification;

-- Show index details for PRICEMATRIX
SELECT 'PRICEMATRIX indexes created:' AS status;
SELECT DISTINCT INDEX_NAME, INDEX_TYPE, NON_UNIQUE
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND (TABLE_NAME = 'PRICEMATRIX' OR TABLE_NAME = 'pricematrix')
  AND INDEX_NAME LIKE 'idx_pricematrix%'
ORDER BY INDEX_NAME;

SELECT 'PRICEMATRIX Index Creation Completed' AS status;
SELECT NOW() AS script_end_time;
