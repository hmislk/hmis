-- ============================================================
-- Fix Credit Bill Balances After Refund
-- Issue: Direct OPD credit bills have incorrect balance after returns
-- GitHub Issue: Related to credit bill settlement due amount calculation
-- Date: 2026-01-06
-- ============================================================

-- Step 1: Identify affected bills (verification query - run first to see what will be changed)
-- This shows direct credit bills (no batch bill) that have refunds but incorrect balance
SELECT
    b.id,
    b.insId,
    b.deptId,
    b.paymentMethod,
    b.netTotal + b.vat as original_amount,
    b.balance as current_balance,
    b.refundAmount,
    b.paidAmount,
    GREATEST(0, b.netTotal + b.vat - COALESCE(b.paidAmount, 0) - COALESCE(b.refundAmount, 0)) as calculated_balance,
    b.balance - GREATEST(0, b.netTotal + b.vat - COALESCE(b.paidAmount, 0) - COALESCE(b.refundAmount, 0)) as difference
FROM bill b
WHERE b.paymentMethod = 'Credit'
  AND b.backwardReferenceBill_id IS NULL
  AND b.retired = false
  AND COALESCE(b.refundAmount, 0) > 0
  AND ABS(b.balance - GREATEST(0, b.netTotal + b.vat - COALESCE(b.paidAmount, 0) - COALESCE(b.refundAmount, 0))) > 0.01
ORDER BY b.id;

-- Step 2: Count affected bills before fix
SELECT COUNT(*) as affected_bills_count
FROM bill b
WHERE b.paymentMethod = 'Credit'
  AND b.backwardReferenceBill_id IS NULL
  AND b.retired = false
  AND COALESCE(b.refundAmount, 0) > 0
  AND ABS(b.balance - GREATEST(0, b.netTotal + b.vat - COALESCE(b.paidAmount, 0) - COALESCE(b.refundAmount, 0))) > 0.01;

-- Step 3: Fix incorrect balances
-- This updates the balance field to correctly account for refund amounts
-- Formula: balance = netTotal + vat - paidAmount - refundAmount (minimum 0)
UPDATE bill b
SET balance = GREATEST(0, b.netTotal + b.vat - COALESCE(b.paidAmount, 0) - COALESCE(b.refundAmount, 0))
WHERE b.paymentMethod = 'Credit'
  AND b.backwardReferenceBill_id IS NULL
  AND b.retired = false
  AND COALESCE(b.refundAmount, 0) > 0
  AND ABS(b.balance - GREATEST(0, b.netTotal + b.vat - COALESCE(b.paidAmount, 0) - COALESCE(b.refundAmount, 0))) > 0.01;

-- Step 4: Verify fix (should return 0 rows if all fixed correctly)
SELECT COUNT(*) as remaining_issues
FROM bill b
WHERE b.paymentMethod = 'Credit'
  AND b.backwardReferenceBill_id IS NULL
  AND b.retired = false
  AND COALESCE(b.refundAmount, 0) > 0
  AND ABS(b.balance - GREATEST(0, b.netTotal + b.vat - COALESCE(b.paidAmount, 0) - COALESCE(b.refundAmount, 0))) > 0.01;

-- ============================================================
-- Notes:
-- 1. Run Step 1 and 2 first to identify and count affected bills
-- 2. Review the output before running Step 3
-- 3. Step 3 is the actual UPDATE statement
-- 4. Step 4 verifies all issues are resolved
--
-- The formula used:
-- balance = netTotal + vat - paidAmount - refundAmount
-- where balance >= 0 (cannot be negative)
-- ============================================================
