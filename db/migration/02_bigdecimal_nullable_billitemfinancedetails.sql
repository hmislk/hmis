-- Migration Script: Make BigDecimal fields nullable in BillItemFinanceDetails
-- Part of BigDecimal Refactoring Initiative (Issue #12437)
-- Phase 5: Database Migration Scripts
-- 
-- This script modifies the BillItemFinanceDetails table to allow NULL values
-- for BigDecimal fields, supporting the null-safe refactoring.
-- NOTE: unitsPerPack is excluded as it maintains its default BigDecimal.ONE value
--
-- Author: Generated for HMIS BigDecimal Refactoring
-- Date: 2025-07-21

-- =====================================================
-- BillItemFinanceDetails Table - 56 BigDecimal Fields
-- (excluding unitsPerPack which keeps its default)
-- =====================================================

-- Rate fields
ALTER TABLE BillItemFinanceDetails ALTER COLUMN lineGrossRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN billGrossRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN grossRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN lineNetRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN billNetRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN netRate DROP NOT NULL;

-- Discount rate fields
ALTER TABLE BillItemFinanceDetails ALTER COLUMN lineDiscountRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN billDiscountRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN totalDiscountRate DROP NOT NULL;

-- Expense rate fields
ALTER TABLE BillItemFinanceDetails ALTER COLUMN lineExpenseRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN billExpenseRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN totalExpenseRate DROP NOT NULL;

-- Tax rate fields
ALTER TABLE BillItemFinanceDetails ALTER COLUMN billTaxRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN lineTaxRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN totalTaxRate DROP NOT NULL;

-- Cost rate fields
ALTER TABLE BillItemFinanceDetails ALTER COLUMN billCostRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN lineCostRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN totalCostRate DROP NOT NULL;

-- Total fields (gross and net)
ALTER TABLE BillItemFinanceDetails ALTER COLUMN lineGrossTotal DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN billGrossTotal DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN grossTotal DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN lineNetTotal DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN billNetTotal DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN netTotal DROP NOT NULL;

-- Discount total fields
ALTER TABLE BillItemFinanceDetails ALTER COLUMN lineDiscount DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN billDiscount DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN totalDiscount DROP NOT NULL;

-- Retail and wholesale rate fields
ALTER TABLE BillItemFinanceDetails ALTER COLUMN retailSaleRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN wholesaleRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN retailSaleRatePerUnit DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN wholesaleRatePerUnit DROP NOT NULL;

-- Value at different rates
ALTER TABLE BillItemFinanceDetails ALTER COLUMN valueAtRetailRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN valueAtPurchaseRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN valueAtCostRate DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN valueAtWholesaleRate DROP NOT NULL;

-- Absolute tax values
ALTER TABLE BillItemFinanceDetails ALTER COLUMN billTax DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN lineTax DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN totalTax DROP NOT NULL;

-- Absolute expense values
ALTER TABLE BillItemFinanceDetails ALTER COLUMN billExpense DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN lineExpense DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN totalExpense DROP NOT NULL;

-- Absolute cost values
ALTER TABLE BillItemFinanceDetails ALTER COLUMN billCost DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN lineCost DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN totalCost DROP NOT NULL;

-- Quantity fields
ALTER TABLE BillItemFinanceDetails ALTER COLUMN freeQuantity DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN quantity DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN totalQuantity DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN freeQuantityByUnits DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN quantityByUnits DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN totalQuantityByUnits DROP NOT NULL;

-- Return quantity fields
ALTER TABLE BillItemFinanceDetails ALTER COLUMN returnQuantity DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN returnFreeQuantity DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN totalReturnQuantity DROP NOT NULL;

-- Return total fields
ALTER TABLE BillItemFinanceDetails ALTER COLUMN returnGrossTotal DROP NOT NULL;
ALTER TABLE BillItemFinanceDetails ALTER COLUMN returnNetTotal DROP NOT NULL;

-- Profit margin
ALTER TABLE BillItemFinanceDetails ALTER COLUMN profitMargin DROP NOT NULL;

-- =====================================================
-- Data Migration Notes:
-- =====================================================
-- Existing BigDecimal.ZERO values will remain as 0.00 in the database
-- This approach maintains data integrity and backward compatibility
-- The application now handles NULL values gracefully using BigDecimalUtil
-- 
-- IMPORTANT: unitsPerPack is NOT modified in this script as it:
-- - Maintains its business-critical default value of BigDecimal.ONE
-- - Represents a conversion factor that should never be null
-- - Is essential for AMPP (pack-based) item calculations
--
-- Alternative approach (if converting zeros to NULL):
-- UPDATE BillItemFinanceDetails SET lineGrossRate = NULL WHERE lineGrossRate = 0;
-- (Apply similar UPDATE statements for other fields where zero represents "no value")
-- =====================================================

COMMIT;