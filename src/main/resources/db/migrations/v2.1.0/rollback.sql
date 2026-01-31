-- Rollback Script v2.1.0: Remove consumption_allowed field from Item table
-- Purpose: Revert the consumptionAllowed attribute changes
-- Date: 2025-11-09
-- Issue: https://github.com/hmislk/hmis/issues/16415

-- WARNING: This rollback will permanently delete the consumption_allowed column
-- and all data stored in it. Ensure you have a backup before proceeding.

-- Step 1: Remove the consumption_allowed column
-- This will drop the column and all its data
ALTER TABLE item
DROP COLUMN consumption_allowed;

-- Step 2: Verification queries (these are comments for manual verification)
-- Verify column has been removed:
-- DESCRIBE item;
-- or
-- SHOW COLUMNS FROM item LIKE 'consumption_allowed';
-- (Should return no results)

-- Step 3: Application Code Considerations
-- After running this rollback, you must also:
-- 1. Remove the consumptionAllowed property from Item.java entity
-- 2. Remove the consumption allowed UI elements from amp.xhtml
-- 3. Remove the configuration option from ConfigOptionApplicationController.java
-- 4. Revert the pharmacy issue autocomplete method changes
-- 5. Update the pharmacy_issue.xhtml to use the original autocomplete method

-- Rollback Success Criteria:
-- 1. consumption_allowed column no longer exists in item table
-- 2. Database schema is restored to pre-migration state
-- 3. No references to consumption_allowed in database constraints
-- 4. Application code changes are also reverted to maintain consistency