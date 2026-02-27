-- Migration v2.1.16: Fix GRN Return and Direct Purchase Return bills missing COMPLETED and APPROVEAT
-- Author: Dr M H B Ariyaratne
-- Date: 2026-02-27
-- Issue: #18261 - QB report does not show GRN/Direct Purchase returns
-- Root Cause: The QB report filters returns by completed=true and approveAt date range.
--             GRN returns saved via the older approval path (GrnReturnApprovalController)
--             were not setting completed=true. Return bills that have been approved
--             (approveAt IS NOT NULL) but are missing completed=true are fixed here.
--             Returns where approveAt IS NULL have never been formally approved and are
--             intentionally left unchanged; they will not appear in the QB report.

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
-- STEP 3: SKIP RETURNS WHERE APPROVEAT IS MISSING
-- ==========================================

-- Returns where APPROVEAT IS NULL have never been formally approved.
-- We do NOT back-fill APPROVEAT with CREATEDAT because that would promote
-- unapproved draft bills into the QB report. These rows are intentionally
-- left with APPROVEAT NULL and COMPLETED=0.

SELECT 'Step 2: Returns with APPROVEAT IS NULL are left unchanged (not yet approved)' AS description,
       COUNT(*) AS skipped_rows
FROM bill
WHERE BILLTYPEATOMIC IN ('PHARMACY_GRN_RETURN', 'PHARMACY_DIRECT_PURCHASE_REFUND')
  AND CANCELLED = 0
  AND RETIRED = 0
  AND COMPLETED = 0
  AND APPROVEAT IS NULL;

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
                AND APPROVEAT IS NOT NULL
                AND COMPLETED = 0) = 0
        THEN 'Migration v2.1.16 SUCCESS: All approved GRN/Direct Purchase returns now have completed=true'
        ELSE 'Migration v2.1.16 PARTIAL: Some approved returns still missing completed=true - manual review required'
    END AS final_result;

SELECT 'Migration v2.1.16 completed' AS final_status;
