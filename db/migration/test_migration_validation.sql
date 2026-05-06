-- Migration Validation Test Script
-- HMIS BigDecimal Refactoring - Phase 5
-- 
-- This script validates the successful execution of BigDecimal nullable migration
-- Run this after executing the main migration scripts

-- =====================================================
-- PRE-MIGRATION VALIDATION (Run Before Migration)
-- =====================================================

-- Check current NOT NULL constraints on BillFinanceDetails
SELECT COLUMN_NAME, IS_NULLABLE, DATA_TYPE, NUMERIC_PRECISION, NUMERIC_SCALE
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'BillFinanceDetails' 
  AND DATA_TYPE = 'DECIMAL'
  AND NUMERIC_PRECISION = 18
  AND NUMERIC_SCALE = 4
ORDER BY COLUMN_NAME;

-- Check current NOT NULL constraints on BillItemFinanceDetails  
SELECT COLUMN_NAME, IS_NULLABLE, DATA_TYPE, NUMERIC_PRECISION, NUMERIC_SCALE
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'BillItemFinanceDetails' 
  AND DATA_TYPE = 'DECIMAL'
  AND NUMERIC_PRECISION = 18
  AND NUMERIC_SCALE = 4
ORDER BY COLUMN_NAME;

-- Count existing records with zero values
SELECT 'BillFinanceDetails' as table_name, COUNT(*) as total_records
FROM BillFinanceDetails;

SELECT 'BillItemFinanceDetails' as table_name, COUNT(*) as total_records  
FROM BillItemFinanceDetails;

-- =====================================================
-- POST-MIGRATION VALIDATION (Run After Migration)
-- =====================================================

-- Verify all BigDecimal fields are now nullable (except unitsPerPack)
-- Expected: IS_NULLABLE = 'YES' for all except unitsPerPack
SELECT 
    'BillFinanceDetails' as table_name,
    COLUMN_NAME, 
    IS_NULLABLE,
    CASE 
        WHEN IS_NULLABLE = 'YES' THEN 'PASS' 
        ELSE 'FAIL' 
    END as validation_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'BillFinanceDetails' 
  AND DATA_TYPE = 'DECIMAL'
  AND NUMERIC_PRECISION = 18
  AND NUMERIC_SCALE = 4
ORDER BY COLUMN_NAME;

-- Check BillItemFinanceDetails (unitsPerPack should remain NOT NULL)
SELECT 
    'BillItemFinanceDetails' as table_name,
    COLUMN_NAME, 
    IS_NULLABLE,
    CASE 
        WHEN COLUMN_NAME = 'unitsPerPack' AND IS_NULLABLE = 'NO' THEN 'PASS (NOT NULL)'
        WHEN COLUMN_NAME != 'unitsPerPack' AND IS_NULLABLE = 'YES' THEN 'PASS (NULLABLE)'
        ELSE 'FAIL' 
    END as validation_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'BillItemFinanceDetails' 
  AND DATA_TYPE = 'DECIMAL'
  AND NUMERIC_PRECISION = 18
  AND NUMERIC_SCALE = 4
ORDER BY COLUMN_NAME;

-- Verify data integrity - no records should be lost
SELECT 'BillFinanceDetails' as table_name, COUNT(*) as records_after_migration
FROM BillFinanceDetails;

SELECT 'BillItemFinanceDetails' as table_name, COUNT(*) as records_after_migration
FROM BillItemFinanceDetails;

-- Sample data check - verify existing zeros are preserved
SELECT 
    'BillFinanceDetails sample' as description,
    COUNT(CASE WHEN billDiscount = 0.0000 THEN 1 END) as zero_discounts,
    COUNT(CASE WHEN billDiscount IS NULL THEN 1 END) as null_discounts,
    COUNT(*) as total_records
FROM BillFinanceDetails;

SELECT 
    'BillItemFinanceDetails sample' as description, 
    COUNT(CASE WHEN quantity = 0.0000 THEN 1 END) as zero_quantities,
    COUNT(CASE WHEN quantity IS NULL THEN 1 END) as null_quantities,
    COUNT(*) as total_records
FROM BillItemFinanceDetails;

-- Verify unitsPerPack maintains its default and NOT NULL constraint
SELECT 
    'unitsPerPack validation' as description,
    COUNT(CASE WHEN unitsPerPack IS NULL THEN 1 END) as null_units_per_pack,
    COUNT(CASE WHEN unitsPerPack = 1.0000 THEN 1 END) as default_units_per_pack,
    COUNT(*) as total_records
FROM BillItemFinanceDetails;

-- =====================================================
-- SUCCESS CRITERIA VALIDATION
-- =====================================================

-- All validation queries should return:
-- 1. BillFinanceDetails: 36 fields with IS_NULLABLE = 'YES'  
-- 2. BillItemFinanceDetails: 56 fields with IS_NULLABLE = 'YES' + 1 field (unitsPerPack) with IS_NULLABLE = 'NO'
-- 3. Record counts should remain identical before and after migration
-- 4. No NULL values should exist for unitsPerPack
-- 5. Existing zero values should be preserved (not converted to NULL)

-- If all validation checks pass, the migration was successful!