-- Rollback for Migration v2.1.14: Reset approveAt for pharmacy return bills
-- Author: Dr M H B Ariyaratne
-- Date: 2026-01-20
-- WARNING: This will cause historical returns to not appear in QB reports until re-migrated
-- Use only if migration caused issues and you need to restore previous state

-- ==========================================
-- STEP 1: DIAGNOSTIC - CHECK CURRENT STATE
-- ==========================================

SELECT 'Rollback v2.1.14 - Starting rollback of approveAt backfill' AS status;

-- Count bills that would be affected
SELECT 'BEFORE ROLLBACK: Bills with approveAt by type' AS status;
SELECT
    BILLTYPEATOMIC,
    COUNT(*) as total_bills,
    SUM(CASE WHEN APPROVEAT IS NOT NULL THEN 1 ELSE 0 END) as has_approve_at
FROM BILL
WHERE BILLTYPEATOMIC IN ('PHARMACY_GRN_RETURN', 'PHARMACY_DIRECT_PURCHASE_REFUND', 'PHARMACY_RETURN_WITHOUT_TREASING')
  AND RETIRED = FALSE
GROUP BY BILLTYPEATOMIC
ORDER BY BILLTYPEATOMIC;

-- ==========================================
-- STEP 2: ROLLBACK - RESET approveAt TO NULL
-- ==========================================

-- Note: This will reset ALL approveAt values for these bill types
-- This includes both migrated historical bills AND any new bills created after the code fix
-- If you only want to rollback migrated bills, you need a more selective approach

SELECT 'Resetting approveAt for PHARMACY_GRN_RETURN bills' AS status;
UPDATE BILL
SET APPROVEAT = NULL
WHERE BILLTYPEATOMIC = 'PHARMACY_GRN_RETURN'
  AND RETIRED = FALSE;

SELECT 'Resetting approveAt for PHARMACY_DIRECT_PURCHASE_REFUND bills' AS status;
UPDATE BILL
SET APPROVEAT = NULL
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE_REFUND'
  AND RETIRED = FALSE;

SELECT 'Resetting approveAt for PHARMACY_RETURN_WITHOUT_TREASING bills' AS status;
UPDATE BILL
SET APPROVEAT = NULL
WHERE BILLTYPEATOMIC = 'PHARMACY_RETURN_WITHOUT_TREASING'
  AND RETIRED = FALSE;

-- ==========================================
-- STEP 3: VERIFICATION
-- ==========================================

SELECT 'AFTER ROLLBACK: Verifying approveAt is reset' AS status;
SELECT
    BILLTYPEATOMIC,
    COUNT(*) as total_bills,
    SUM(CASE WHEN APPROVEAT IS NULL THEN 1 ELSE 0 END) as approve_at_is_null,
    SUM(CASE WHEN APPROVEAT IS NOT NULL THEN 1 ELSE 0 END) as approve_at_not_null
FROM BILL
WHERE BILLTYPEATOMIC IN ('PHARMACY_GRN_RETURN', 'PHARMACY_DIRECT_PURCHASE_REFUND', 'PHARMACY_RETURN_WITHOUT_TREASING')
  AND RETIRED = FALSE
GROUP BY BILLTYPEATOMIC
ORDER BY BILLTYPEATOMIC;

SELECT 'Rollback v2.1.14 completed - Historical returns will no longer appear in QB reports' AS final_status;
