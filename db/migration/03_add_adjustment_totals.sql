-- Migration Script: Add before/after adjustment totals
-- Adds columns totalBeforeAdjustmentValue and totalAfterAdjustmentValue
-- to BillFinanceDetails for purchase rate adjustment totals.
-- Author: Codex
-- Date: 2025-07-25

ALTER TABLE BillFinanceDetails ADD COLUMN totalBeforeAdjustmentValue DECIMAL(18,4);
ALTER TABLE BillFinanceDetails ADD COLUMN totalAfterAdjustmentValue DECIMAL(18,4);
