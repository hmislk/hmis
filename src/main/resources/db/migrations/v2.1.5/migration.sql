-- Migration v2.1.5: Add Composite Indexes for PAYMENT_SCHEME_DISCOUNT Performance Optimization
-- Description: Optimize discount calculation queries to reduce item selection delay from 552ms to 2-5ms
-- Author: Dr M H B Ariyaratne
-- Date: 2025-12-13
-- GitHub Issue: #16990
-- Branch: 16990-speed-up-the-pharmacy-retail-sale
--
-- IMPORTANT: This migration targets PRICEMATRIX (uppercase) for Production/Ubuntu/Linux environments
-- For Development/Windows environments using lowercase 'pricematrix', use migration-dev.sql instead
-- or manually run: s/PRICEMATRIX/pricematrix/g on this file before execution

-- ==========================================
-- PRE-MIGRATION VERIFICATION
-- ==========================================

-- Step 1: Verify table existence
SELECT COUNT(*) AS table_count
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME IN ('PRICEMATRIX', 'pricematrix');

-- Step 2: Check current indexes
SELECT INDEX_NAME, COLUMN_NAME
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_NAME IN ('PRICEMATRIX', 'pricematrix')
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- ==========================================
-- INDEX CREATION - ITEM LEVEL DISCOUNTS
-- ==========================================

-- Step 3: Create index for item-level discount queries on PRICEMATRIX (Production/Ubuntu - uppercase)
-- Using IF NOT EXISTS for idempotency (can be re-run safely)
CREATE INDEX IF NOT EXISTS idx_psd_item ON PRICEMATRIX(RETIRED, PAYMENTMETHOD, ITEM_ID, PAYMENTSCHEME_ID, MEMBERSHIPSCHEME_ID);

-- ==========================================
-- INDEX CREATION - CATEGORY LEVEL DISCOUNTS
-- ==========================================

-- Step 4: Create index for category-level discount queries on PRICEMATRIX (Production/Ubuntu - uppercase)
CREATE INDEX IF NOT EXISTS idx_psd_category ON PRICEMATRIX(RETIRED, PAYMENTMETHOD, CATEGORY_ID, PAYMENTSCHEME_ID, MEMBERSHIPSCHEME_ID);

-- ==========================================
-- INDEX CREATION - DEPARTMENT LEVEL DISCOUNTS
-- ==========================================

-- Step 5: Create index for department-level discount queries on PRICEMATRIX (Production/Ubuntu - uppercase)
CREATE INDEX IF NOT EXISTS idx_psd_department ON PRICEMATRIX(RETIRED, PAYMENTMETHOD, DEPARTMENT_ID, PAYMENTSCHEME_ID, MEMBERSHIPSCHEME_ID);

-- ==========================================
-- POST-MIGRATION VERIFICATION
-- ==========================================

-- Step 9: Verify all indexes were created successfully
SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX, CARDINALITY
FROM INFORMATION_SCHEMA.STATISTICS
WHERE INDEX_NAME LIKE 'idx_psd_%'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- ==========================================
-- QUERY PLAN VERIFICATION (EXPLAIN)
-- ==========================================

-- Test 1: Item-level discount query
EXPLAIN SELECT DISCOUNTPERCENT
FROM pricematrix
WHERE RETIRED=0
  AND PAYMENTMETHOD='Cash'
  AND PAYMENTSCHEME_ID IS NULL
  AND MEMBERSHIPSCHEME_ID IS NULL
  AND ITEM_ID=1;

-- Test 2: Category-level discount query
EXPLAIN SELECT DISCOUNTPERCENT
FROM pricematrix
WHERE RETIRED=0
  AND PAYMENTMETHOD='Cash'
  AND PAYMENTSCHEME_ID IS NULL
  AND MEMBERSHIPSCHEME_ID IS NULL
  AND CATEGORY_ID=1;

-- Test 3: Department-level discount query
EXPLAIN SELECT DISCOUNTPERCENT
FROM pricematrix
WHERE RETIRED=0
  AND PAYMENTMETHOD='Cash'
  AND PAYMENTSCHEME_ID IS NULL
  AND MEMBERSHIPSCHEME_ID IS NULL
  AND DEPARTMENT_ID=1;

-- ==========================================
-- MIGRATION COMPLETE
-- ==========================================
-- Expected outcome:
-- - Query time reduction: 552ms → 2-5ms (100x faster)
-- - Item selection delay eliminated
-- - Discount hierarchy queries optimized (Item → Category → Department)
-- - No data changes, only schema optimization
-- - Application remains fully functional
-- ==========================================
