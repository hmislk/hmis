-- Rollback Script: Revert TOTALCOSTVALUE decimal precision change
-- This script reverts the TOTALCOSTVALUE column back to decimal(38,0)
-- WARNING: This will truncate any decimal precision in existing data
--
-- Author: Claude Code Assistant
-- Date: 2025-12-22

-- =====================================================
-- ROLLBACK: BILLFINANCEDETAILS.TOTALCOSTVALUE Precision
-- =====================================================

-- Revert TOTALCOSTVALUE column back to decimal(38,0)
-- WARNING: This will lose decimal precision for any values with decimals
ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALCOSTVALUE decimal(38,0);

-- =====================================================
-- Rollback Notes:
-- =====================================================
-- - Data with decimal places will be truncated (e.g., 150.7500 becomes 150)
-- - Summary reports will display cost values as whole numbers again
-- - This rollback should only be used if the migration causes issues
-- =====================================================