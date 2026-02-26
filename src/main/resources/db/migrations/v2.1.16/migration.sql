-- Migration v2.1.16: Fix GRN Return and Direct Purchase Return bills missing COMPLETED and APPROVEAT
-- Author: Dr M H B Ariyaratne
-- Date: 2026-02-27
-- Issue: #18261 - QB report does not show GRN/Direct Purchase returns
-- Root Cause: The QB report filters returns by completed=true and approveAt date range.
--             GRN returns saved via the older approval path (GrnReturnApprovalController)
--             were not setting completed=true. All return bills that have been approved
--             (approveUser is set) but are missing completed=true or approveAt need fixing.
--             For returns where approveAt is still NULL but the bill is not cancelled,
--             we use createdAt as the approveAt value.

SELECT 'Migration v2.1.16 - Fix GRN/Direct Purchase return completed and approveAt fields' AS status;

-- ==========================================
-- STEP 1: PRE-MIGRATION ANALYSIS
-- ==========================================

SELECT 'BEFORE: Returns missing completed=true or approveAt' AS status;
SELECT
    BILLTYPE,
    BILLTYPEATOMIC,
    COUNT(*) AS total,
    SUM(CASE WHEN COMPLETED = 0 THEN 1 ELSE 0 END) AS not_completed,
    SUM(CASE WHEN APPROVEAT IS NULL THEN 1 ELSE 0 END) AS missing_approveat
FROM bill
WHERE BILLTYPEATOMIC IN ('PHARMACY_GRN_RETURN', 'PHARMACY_DIRECT_PURCHASE_REFUND')
  AND CANCELLED = 0
  AND RETIRED = 0
GROUP BY BILLTYPE, BILLTYPEATOMIC;

-- ==========================================
-- STEP 2: FIX RETURNS THAT HAVE AN APPROVEUSER BUT MISSING COMPLETED OR APPROVEAT
-- ==========================================

-- These were approved via GrnReturnApprovalController which set approveUser/approveAt
-- but did not set completed=true. Use the existing approveAt value.

UPDATE bill
SET COMPLETED = 1
WHERE BILLTYPEATOMIC IN ('PHARMACY_GRN_RETURN', 'PHARMACY_DIRECT_PURCHASE_REFUND')
  AND CANCELLED = 0
  AND RETIRED = 0
  AND COMPLETED = 0
  AND APPROVEAT IS NOT NULL;

SELECT 'Step 1: Set completed=true for approved returns (had approveAt, missing completed)' AS description,
       ROW_COUNT() AS affected_rows;

-- ==========================================
-- STEP 3: FIX RETURNS WHERE BOTH APPROVEAT AND COMPLETED ARE MISSING
-- ==========================================

-- These are returns that were created but never formally approved
-- (or the approval did not record approveAt). Use createdAt as the approveAt.

UPDATE bill
SET APPROVEAT = CREATEDAT,
    COMPLETED = 1
WHERE BILLTYPEATOMIC IN ('PHARMACY_GRN_RETURN', 'PHARMACY_DIRECT_PURCHASE_REFUND')
  AND CANCELLED = 0
  AND RETIRED = 0
  AND COMPLETED = 0
  AND APPROVEAT IS NULL;

SELECT 'Step 2: Set approveAt=createdAt and completed=true for returns missing both fields' AS description,
       ROW_COUNT() AS affected_rows;

-- ==========================================
-- STEP 4: POST-MIGRATION VERIFICATION
-- ==========================================

SELECT 'AFTER: Returns still missing completed=true or approveAt (should be 0)' AS status;
SELECT
    BILLTYPE,
    BILLTYPEATOMIC,
    COUNT(*) AS total,
    SUM(CASE WHEN COMPLETED = 0 THEN 1 ELSE 0 END) AS not_completed,
    SUM(CASE WHEN APPROVEAT IS NULL THEN 1 ELSE 0 END) AS missing_approveat
FROM bill
WHERE BILLTYPEATOMIC IN ('PHARMACY_GRN_RETURN', 'PHARMACY_DIRECT_PURCHASE_REFUND')
  AND CANCELLED = 0
  AND RETIRED = 0
GROUP BY BILLTYPE, BILLTYPEATOMIC;

SELECT 'Sample of fixed records' AS status;
SELECT ID, BILLTYPE, BILLTYPEATOMIC, CREATEDAT, APPROVEAT, COMPLETED, CANCELLED
FROM bill
WHERE BILLTYPEATOMIC IN ('PHARMACY_GRN_RETURN', 'PHARMACY_DIRECT_PURCHASE_REFUND')
  AND CANCELLED = 0
  AND RETIRED = 0
ORDER BY ID DESC
LIMIT 10;

SELECT
    CASE
        WHEN (SELECT COUNT(*) FROM bill
              WHERE BILLTYPEATOMIC IN ('PHARMACY_GRN_RETURN', 'PHARMACY_DIRECT_PURCHASE_REFUND')
                AND CANCELLED = 0
                AND RETIRED = 0
                AND (COMPLETED = 0 OR APPROVEAT IS NULL)) = 0
        THEN 'Migration v2.1.16 SUCCESS: All active GRN/Direct Purchase returns now have completed=true and approveAt set'
        ELSE 'Migration v2.1.16 PARTIAL: Some returns still missing fields - manual review required'
    END AS final_result;

SELECT 'Migration v2.1.16 completed' AS final_status;
