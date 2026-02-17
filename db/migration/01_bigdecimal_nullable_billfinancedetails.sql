-- Migration Script: Make BigDecimal fields nullable in BillFinanceDetails
-- Part of BigDecimal Refactoring Initiative (Issue #12437)
-- Phase 5: Database Migration Scripts
-- 
-- This script modifies the BillFinanceDetails table to allow NULL values
-- for all BigDecimal fields, supporting the null-safe refactoring.
--
-- Author: Generated for HMIS BigDecimal Refactoring
-- Date: 2025-07-21

-- =====================================================
-- BillFinanceDetails Table - 36 BigDecimal Fields
-- =====================================================

-- Discount fields
ALTER TABLE BillFinanceDetails ALTER COLUMN billDiscount DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN lineDiscount DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalDiscount DROP NOT NULL;

-- Expense fields
ALTER TABLE BillFinanceDetails ALTER COLUMN billExpense DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN lineExpense DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalExpense DROP NOT NULL;

-- Cost value fields
ALTER TABLE BillFinanceDetails ALTER COLUMN billCostValue DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN lineCostValue DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalCostValue DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalCostValueFree DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalCostValueNonFree DROP NOT NULL;

-- Tax value fields
ALTER TABLE BillFinanceDetails ALTER COLUMN billTaxValue DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN itemTaxValue DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalTaxValue DROP NOT NULL;

-- Purchase value fields
ALTER TABLE BillFinanceDetails ALTER COLUMN totalPurchaseValue DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalPurchaseValueFree DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalPurchaseValueNonFree DROP NOT NULL;

-- Free item value fields
ALTER TABLE BillFinanceDetails ALTER COLUMN totalOfFreeItemValues DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalOfFreeItemValuesFree DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalOfFreeItemValuesNonFree DROP NOT NULL;

-- Retail sale value fields
ALTER TABLE BillFinanceDetails ALTER COLUMN totalRetailSaleValue DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalRetailSaleValueFree DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalRetailSaleValueNonFree DROP NOT NULL;

-- Wholesale value fields
ALTER TABLE BillFinanceDetails ALTER COLUMN totalWholesaleValue DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalWholesaleValueFree DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalWholesaleValueNonFree DROP NOT NULL;

-- Quantity fields
ALTER TABLE BillFinanceDetails ALTER COLUMN totalQuantity DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalFreeQuantity DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalQuantityInAtomicUnitOfMeasurement DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN totalFreeQuantityInAtomicUnitOfMeasurement DROP NOT NULL;

-- Total fields (gross and net)
ALTER TABLE BillFinanceDetails ALTER COLUMN lineGrossTotal DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN billGrossTotal DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN grossTotal DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN lineNetTotal DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN billNetTotal DROP NOT NULL;
ALTER TABLE BillFinanceDetails ALTER COLUMN netTotal DROP NOT NULL;

-- =====================================================
-- Data Migration Notes:
-- =====================================================
-- Existing BigDecimal.ZERO values will remain as 0.00 in the database
-- This approach maintains data integrity and backward compatibility
-- The application now handles NULL values gracefully using BigDecimalUtil
--
-- Alternative approach (if converting zeros to NULL):
-- UPDATE BillFinanceDetails SET billDiscount = NULL WHERE billDiscount = 0;
-- (Apply similar UPDATE statements for other fields where zero represents "no value")
-- =====================================================

COMMIT;