-- Rollback Migration v2.1.5: Remove PAYMENT_SCHEME_DISCOUNT Performance Indexes
-- Description: Drops composite indexes added for discount calculation optimization
-- Author: Dr M H B Ariyaratne
-- Date: 2025-12-12
-- GitHub Issue: #16990
-- Branch: 16990-speed-up-the-pharmacy-retail-sale

-- ==========================================
-- PRE-ROLLBACK VERIFICATION
-- ==========================================

-- Step 1: Verify indexes exist before dropping
SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX
FROM INFORMATION_SCHEMA.STATISTICS
WHERE INDEX_NAME LIKE 'idx_psd_%'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- ==========================================
-- DROP INDEXES - ITEM LEVEL
-- ==========================================

-- Drop item-level index from UPPERCASE table (Production/Ubuntu)
DROP INDEX idx_psd_item ON PRICEMATRIX;

-- Drop item-level index from lowercase table (Development/Windows)
DROP INDEX idx_psd_item ON pricematrix;

-- ==========================================
-- DROP INDEXES - CATEGORY LEVEL
-- ==========================================

-- Drop category-level index from UPPERCASE table (Production/Ubuntu)
DROP INDEX idx_psd_category ON PRICEMATRIX;

-- Drop category-level index from lowercase table (Development/Windows)
DROP INDEX idx_psd_category ON pricematrix;

-- ==========================================
-- DROP INDEXES - DEPARTMENT LEVEL
-- ==========================================

-- Drop department-level index from UPPERCASE table (Production/Ubuntu)
DROP INDEX idx_psd_department ON PRICEMATRIX;

-- Drop department-level index from lowercase table (Development/Windows)
DROP INDEX idx_psd_department ON pricematrix;

-- ==========================================
-- POST-ROLLBACK VERIFICATION
-- ==========================================

-- Verify all indexes were dropped successfully (should return 0 rows)
SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME
FROM INFORMATION_SCHEMA.STATISTICS
WHERE INDEX_NAME LIKE 'idx_psd_%';

-- ==========================================
-- ROLLBACK COMPLETE
-- ==========================================
-- Expected outcome:
-- - All idx_psd_* indexes removed
-- - Table structure unchanged (data intact)
-- - Application remains functional (but slower discount queries)
-- - Query time reverts to: 2-5ms → 552ms
-- - Safe rollback - no data loss
-- ==========================================
