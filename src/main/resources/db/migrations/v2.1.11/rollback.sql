-- Rollback v2.1.11: Remove performance optimization indexes
-- Author: Claude AI Assistant
-- Date: 2025-12-25
-- Description: Removes the performance optimization indexes added in v2.1.11

-- ==============================================================================
-- ROLLBACK PERFORMANCE OPTIMIZATION INDEXES
-- ==============================================================================

-- WARNING: Rolling back these indexes will impact performance of autocomplete queries
-- Ensure this rollback is necessary before proceeding

-- Remove ITEM table performance indexes
-- This will slow down autocomplete searches on item names and codes
DROP INDEX IF EXISTS idx_item_name_search ON ITEM;
DROP INDEX IF EXISTS idx_item_code_search ON ITEM;

-- Remove STOCK table composite index
-- This will slow down department-stock-batch filtering queries
DROP INDEX IF EXISTS idx_stock_dept_stock_batch ON STOCK;

-- Remove ITEMBATCH table composite index
-- This will slow down item-expiry join and sorting operations
DROP INDEX IF EXISTS idx_itembatch_item_expire ON ITEMBATCH;

-- ==============================================================================
-- ROLLBACK VERIFICATION
-- ==============================================================================

-- Verify indexes were successfully removed
SELECT
    table_name,
    index_name,
    'Index should be removed' as status
FROM information_schema.statistics
WHERE table_schema = DATABASE()
    AND table_name IN ('ITEM', 'STOCK', 'ITEMBATCH')
    AND index_name IN (
        'idx_item_name_search',
        'idx_item_code_search',
        'idx_stock_dept_stock_batch',
        'idx_itembatch_item_expire'
    );

-- NOTE: After rollback, performance will return to pre-migration levels
-- Autocomplete queries will be slower until indexes are recreated

-- Rollback completed successfully