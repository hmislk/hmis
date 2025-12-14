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
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME LIKE 'idx_psd_%'
ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;

-- ==========================================
-- DROP INDEXES - ITEM LEVEL
-- ==========================================

-- Drop item-level index idx_psd_item from UPPERCASE table (Production/Ubuntu)
SET @idx_psd_item_upper = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'PRICEMATRIX'
      AND INDEX_NAME = 'idx_psd_item'
);

SET @sql = IF(@idx_psd_item_upper > 0,
              'ALTER TABLE PRICEMATRIX DROP INDEX idx_psd_item',
              'SELECT ''Index idx_psd_item does not exist on PRICEMATRIX - skipping'' AS skip_message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Index idx_psd_item dropped from PRICEMATRIX (if existed)' AS uppercase_result;

-- Drop item-level index idx_psd_item from lowercase table (Development/Windows)
SET @idx_psd_item_lower = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pricematrix'
      AND INDEX_NAME = 'idx_psd_item'
);

SET @sql = IF(@idx_psd_item_lower > 0,
              'ALTER TABLE pricematrix DROP INDEX idx_psd_item',
              'SELECT ''Index idx_psd_item does not exist on pricematrix - skipping'' AS skip_message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Index idx_psd_item dropped from pricematrix (if existed)' AS lowercase_result;

-- ==========================================
-- DROP INDEXES - CATEGORY LEVEL
-- ==========================================

-- Drop category-level index idx_psd_category from UPPERCASE table (Production/Ubuntu)
SET @idx_psd_category_upper = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'PRICEMATRIX'
      AND INDEX_NAME = 'idx_psd_category'
);

SET @sql = IF(@idx_psd_category_upper > 0,
              'ALTER TABLE PRICEMATRIX DROP INDEX idx_psd_category',
              'SELECT ''Index idx_psd_category does not exist on PRICEMATRIX - skipping'' AS skip_message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Index idx_psd_category dropped from PRICEMATRIX (if existed)' AS uppercase_result;

-- Drop category-level index idx_psd_category from lowercase table (Development/Windows)
SET @idx_psd_category_lower = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pricematrix'
      AND INDEX_NAME = 'idx_psd_category'
);

SET @sql = IF(@idx_psd_category_lower > 0,
              'ALTER TABLE pricematrix DROP INDEX idx_psd_category',
              'SELECT ''Index idx_psd_category does not exist on pricematrix - skipping'' AS skip_message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Index idx_psd_category dropped from pricematrix (if existed)' AS lowercase_result;

-- ==========================================
-- DROP INDEXES - DEPARTMENT LEVEL
-- ==========================================

-- Drop department-level index idx_psd_department from UPPERCASE table (Production/Ubuntu)
SET @idx_psd_department_upper = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'PRICEMATRIX'
      AND INDEX_NAME = 'idx_psd_department'
);

SET @sql = IF(@idx_psd_department_upper > 0,
              'ALTER TABLE PRICEMATRIX DROP INDEX idx_psd_department',
              'SELECT ''Index idx_psd_department does not exist on PRICEMATRIX - skipping'' AS skip_message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Index idx_psd_department dropped from PRICEMATRIX (if existed)' AS uppercase_result;

-- Drop department-level index idx_psd_department from lowercase table (Development/Windows)
SET @idx_psd_department_lower = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pricematrix'
      AND INDEX_NAME = 'idx_psd_department'
);

SET @sql = IF(@idx_psd_department_lower > 0,
              'ALTER TABLE pricematrix DROP INDEX idx_psd_department',
              'SELECT ''Index idx_psd_department does not exist on pricematrix - skipping'' AS skip_message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT 'Index idx_psd_department dropped from pricematrix (if existed)' AS lowercase_result;

-- ==========================================
-- POST-ROLLBACK VERIFICATION
-- ==========================================

-- Verify all indexes were dropped successfully (should return 0 rows)
SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME
FROM INFORMATION_SCHEMA.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND INDEX_NAME LIKE 'idx_psd_%';

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
