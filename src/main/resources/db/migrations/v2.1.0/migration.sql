-- Migration Script v2.1.0: Add consumption_allowed field to Item table
-- Purpose: Add consumptionAllowed attribute with proper defaults and constraints
-- Date: 2025-11-09
-- Issue: https://github.com/hmislk/hmis/issues/16415

-- Step 1: Add the consumption_allowed column (nullable initially)
-- This allows existing rows to be updated before adding NOT NULL constraint
ALTER TABLE item
ADD COLUMN consumption_allowed BOOLEAN;

-- Step 2: Backfill existing records with TRUE (default allowed behavior)
-- This ensures all legacy items remain available for consumption
UPDATE item
SET consumption_allowed = TRUE
WHERE consumption_allowed IS NULL;

-- Step 3: Add NOT NULL constraint with default value
-- This prevents future NULL values and sets default for new records
ALTER TABLE item
MODIFY COLUMN consumption_allowed BOOLEAN NOT NULL DEFAULT TRUE;

-- Step 4: Verification queries (these are comments for manual verification)
-- Verify all rows have non-null values:
-- SELECT COUNT(*) as total_items,
--        SUM(CASE WHEN consumption_allowed IS NULL THEN 1 ELSE 0 END) as null_count,
--        SUM(CASE WHEN consumption_allowed = TRUE THEN 1 ELSE 0 END) as allowed_count,
--        SUM(CASE WHEN consumption_allowed = FALSE THEN 1 ELSE 0 END) as blocked_count
-- FROM item;

-- Verify column constraints:
-- DESCRIBE item;
-- or
-- SHOW COLUMNS FROM item LIKE 'consumption_allowed';

-- Migration Success Criteria:
-- 1. All existing items have consumption_allowed = TRUE
-- 2. Column is NOT NULL with DEFAULT TRUE
-- 3. No NULL values exist in the table
-- 4. New items will automatically get consumption_allowed = TRUE