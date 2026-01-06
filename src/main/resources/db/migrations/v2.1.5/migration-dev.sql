-- Migration v2.1.5: Add Composite Indexes for PAYMENT_SCHEME_DISCOUNT Performance Optimization
-- Description: Optimize discount calculation queries to reduce item selection delay from 552ms to 2-5ms
-- Author: Dr M H B Ariyaratne
-- Date: 2025-12-13
-- GitHub Issue: #16990
-- Branch: 16990-speed-up-the-pharmacy-retail-sale
--
-- IMPORTANT: This migration targets pricematrix (lowercase) for Development/Windows environments
-- For Production/Ubuntu/Linux environments using uppercase 'PRICEMATRIX', use migration.sql instead

-- ==========================================
-- PRE-MIGRATION VERIFICATION
-- ==========================================

-- Step 1: Verify table existence
SELECT COUNT(*) AS table_count
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_NAME = 'pricematrix';

-- Step 2: Check current indexes
SELECT INDEX_NAME, COLUMN_NAME
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_NAME = 'pricematrix'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- ==========================================
-- INDEX CREATION - ITEM LEVEL DISCOUNTS
-- ==========================================

-- Step 3: Create index for item-level discount queries on pricematrix (Development/Windows - lowercase)
-- Using IF NOT EXISTS for idempotency (can be re-run safely)
CREATE INDEX IF NOT EXISTS idx_psd_item ON pricematrix(RETIRED, PAYMENTMETHOD, ITEM_ID, PAYMENTSCHEME_ID, MEMBERSHIPSCHEME_ID);

-- ==========================================
-- INDEX CREATION - CATEGORY LEVEL DISCOUNTS
-- ==========================================

-- Step 4: Create index for category-level discount queries on pricematrix (Development/Windows - lowercase)
CREATE INDEX IF NOT EXISTS idx_psd_category ON pricematrix(RETIRED, PAYMENTMETHOD, CATEGORY_ID, PAYMENTSCHEME_ID, MEMBERSHIPSCHEME_ID);

-- ==========================================
-- INDEX CREATION - DEPARTMENT LEVEL DISCOUNTS
-- ==========================================

-- Step 5: Create index for department-level discount queries on pricematrix (Development/Windows - lowercase)
CREATE INDEX IF NOT EXISTS idx_psd_department ON pricematrix(RETIRED, PAYMENTMETHOD, DEPARTMENT_ID, PAYMENTSCHEME_ID, MEMBERSHIPSCHEME_ID);

-- ==========================================
-- POST-MIGRATION VERIFICATION
-- ==========================================

-- Step 6: Verify all indexes were created successfully
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
