-- Migration Script: Fix TOTALCOSTVALUE decimal precision
-- Issue: Decimal places are not displayed under Cost Rate and Cost Value in Summary report
-- Root Cause: TOTALCOSTVALUE column defined as decimal(38,0) while item-level VALUEATCOSTRATE is decimal(18,4)
-- Solution: Change TOTALCOSTVALUE to decimal(38,4) to preserve decimal precision
--
-- Author: Claude Code Assistant
-- Date: 2025-12-22
-- Related Issue: Stock Transfer Report decimal precision issue

-- =====================================================
-- BILLFINANCEDETAILS Table - TOTALCOSTVALUE Precision Fix
-- =====================================================

-- Step 1: Modify TOTALCOSTVALUE column to support 4 decimal places
ALTER TABLE BILLFINANCEDETAILS MODIFY COLUMN TOTALCOSTVALUE decimal(38,4);

-- =====================================================
-- Migration Notes:
-- =====================================================
-- This change ensures consistency between:
-- 1. Individual item costs (BILLITEMFINANCEDETAILS.VALUEATCOSTRATE) - decimal(18,4)
-- 2. Aggregated bill costs (BILLFINANCEDETAILS.TOTALCOSTVALUE) - now decimal(38,4)
--
-- Impact Analysis:
-- - Summary reports will now display decimal places for cost values
-- - Breakdown summary already displays decimals (uses BILLITEMFINANCEDETAILS)
-- - No data loss: existing integer values will be converted to decimal format
-- - Example: 150 becomes 150.0000
--
-- Testing Strategy:
-- 1. Verify Summary report now shows decimal places in Cost Value column
-- 2. Ensure consistency between Summary and Breakdown Summary reports
-- 3. Validate that aggregation calculations preserve precision
-- =====================================================