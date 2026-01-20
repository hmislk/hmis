-- Migration v2.1.14: Backfill approveAt for historical pharmacy return bills
-- Author: Dr M H B Ariyaratne
-- Date: 2026-01-20
-- Issue: GRN Returns, Direct Purchase Returns, and Returns Without Tracing not appearing in QB Report
-- Root Cause: These bill types were not setting approveAt during approval, and QB report filters by approveAt
-- Fix: Code was updated to set approveAt going forward; 
-- this migration backfills historical data

-- ==========================================
-- STEP 1: DIAGNOSTIC - CHECK CURRENT STATE
-- ==========================================

SELECT 'Migration v2.1.14 - Starting approveAt backfill for pharmacy return bills' AS status;

-- Count bills by type that are missing approveAt
SELECT 'BEFORE: Bills missing approveAt by type' AS status;
SELECT
    BILLTYPEATOMIC,
    COUNT(*) as total_bills,
    SUM(CASE WHEN APPROVEAT IS NULL THEN 1 ELSE 0 END) as missing_approve_at,
    SUM(CASE WHEN APPROVEAT IS NOT NULL THEN 1 ELSE 0 END) as has_approve_at,
    SUM(CASE WHEN COMPLETED = 1 OR COMPLETED = TRUE THEN 1 ELSE 0 END) as completed_bills
FROM BILL
WHERE BILLTYPEATOMIC IN ('PHARMACY_GRN_RETURN', 'PHARMACY_DIRECT_PURCHASE_REFUND', 'PHARMACY_RETURN_WITHOUT_TREASING')
  AND RETIRED = FALSE
GROUP BY BILLTYPEATOMIC
ORDER BY BILLTYPEATOMIC;

-- Show sample of affected PHARMACY_GRN_RETURN bills
SELECT 'Sample PHARMACY_GRN_RETURN bills missing approveAt' AS status;
SELECT ID, DEPTID, BILLTYPEATOMIC, CREATEDAT, COMPLETEDAT, APPROVEAT, COMPLETED
FROM BILL
WHERE BILLTYPEATOMIC = 'PHARMACY_GRN_RETURN'
  AND RETIRED = FALSE
  AND APPROVEAT IS NULL
  AND COMPLETED = TRUE
LIMIT 10;

-- ==========================================
-- STEP 2: BACKFILL PHARMACY_GRN_RETURN
-- ==========================================

SELECT 'Updating PHARMACY_GRN_RETURN bills - setting approveAt from completedAt' AS status;

UPDATE BILL
SET APPROVEAT = COMPLETEDAT
WHERE BILLTYPEATOMIC = 'PHARMACY_GRN_RETURN'
  AND RETIRED = FALSE
  AND APPROVEAT IS NULL
  AND COMPLETEDAT IS NOT NULL
  AND COMPLETED = TRUE;

-- For bills with completedAt NULL but are completed, use createdAt as fallback
UPDATE BILL
SET APPROVEAT = CREATEDAT
WHERE BILLTYPEATOMIC = 'PHARMACY_GRN_RETURN'
  AND RETIRED = FALSE
  AND APPROVEAT IS NULL
  AND COMPLETEDAT IS NULL
  AND COMPLETED = TRUE
  AND CREATEDAT IS NOT NULL;

SELECT 'PHARMACY_GRN_RETURN update complete' AS status;

-- ==========================================
-- STEP 3: BACKFILL PHARMACY_DIRECT_PURCHASE_REFUND
-- ==========================================

SELECT 'Updating PHARMACY_DIRECT_PURCHASE_REFUND bills - setting approveAt from completedAt' AS status;

UPDATE BILL
SET APPROVEAT = COMPLETEDAT
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE_REFUND'
  AND RETIRED = FALSE
  AND APPROVEAT IS NULL
  AND COMPLETEDAT IS NOT NULL
  AND COMPLETED = TRUE;

-- For bills with completedAt NULL but are completed, use createdAt as fallback
UPDATE BILL
SET APPROVEAT = CREATEDAT
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE_REFUND'
  AND RETIRED = FALSE
  AND APPROVEAT IS NULL
  AND COMPLETEDAT IS NULL
  AND COMPLETED = TRUE
  AND CREATEDAT IS NOT NULL;

SELECT 'PHARMACY_DIRECT_PURCHASE_REFUND update complete' AS status;

-- ==========================================
-- STEP 4: BACKFILL PHARMACY_RETURN_WITHOUT_TREASING
-- ==========================================

-- Note: This bill type doesn't have a formal approval workflow, so we use createdAt
SELECT 'Updating PHARMACY_RETURN_WITHOUT_TREASING bills - setting approveAt from createdAt' AS status;

UPDATE BILL
SET APPROVEAT = CREATEDAT,
    COMPLETED = TRUE
WHERE BILLTYPEATOMIC = 'PHARMACY_RETURN_WITHOUT_TREASING'
  AND RETIRED = FALSE
  AND APPROVEAT IS NULL
  AND CREATEDAT IS NOT NULL;

SELECT 'PHARMACY_RETURN_WITHOUT_TREASING update complete' AS status;

-- ==========================================
-- STEP 5: VERIFICATION
-- ==========================================

SELECT 'AFTER: Verifying approveAt is now populated' AS status;
SELECT
    BILLTYPEATOMIC,
    COUNT(*) as total_bills,
    SUM(CASE WHEN APPROVEAT IS NULL THEN 1 ELSE 0 END) as still_missing_approve_at,
    SUM(CASE WHEN APPROVEAT IS NOT NULL THEN 1 ELSE 0 END) as now_has_approve_at,
    SUM(CASE WHEN COMPLETED = 1 OR COMPLETED = TRUE THEN 1 ELSE 0 END) as completed_bills
FROM BILL
WHERE BILLTYPEATOMIC IN ('PHARMACY_GRN_RETURN', 'PHARMACY_DIRECT_PURCHASE_REFUND', 'PHARMACY_RETURN_WITHOUT_TREASING')
  AND RETIRED = FALSE
GROUP BY BILLTYPEATOMIC
ORDER BY BILLTYPEATOMIC;

-- Show sample of now-updated bills
SELECT 'Sample updated PHARMACY_GRN_RETURN bills with approveAt' AS status;
SELECT ID, DEPTID, BILLTYPEATOMIC, CREATEDAT, COMPLETEDAT, APPROVEAT, COMPLETED
FROM BILL
WHERE BILLTYPEATOMIC = 'PHARMACY_GRN_RETURN'
  AND RETIRED = FALSE
  AND APPROVEAT IS NOT NULL
  AND COMPLETED = TRUE
LIMIT 10;

-- ==========================================
-- STEP 6: FINAL SUCCESS CHECK
-- ==========================================

SELECT
    CASE
        WHEN (SELECT COUNT(*) FROM BILL
              WHERE BILLTYPEATOMIC IN ('PHARMACY_GRN_RETURN', 'PHARMACY_DIRECT_PURCHASE_REFUND', 'PHARMACY_RETURN_WITHOUT_TREASING')
                AND RETIRED = FALSE
                AND COMPLETED = TRUE
                AND APPROVEAT IS NULL) = 0
        THEN 'Migration v2.1.14 SUCCESS: All completed pharmacy return bills now have approveAt populated'
        ELSE CONCAT('Migration v2.1.14 PARTIAL: ',
                    (SELECT COUNT(*) FROM BILL
                     WHERE BILLTYPEATOMIC IN ('PHARMACY_GRN_RETURN', 'PHARMACY_DIRECT_PURCHASE_REFUND', 'PHARMACY_RETURN_WITHOUT_TREASING')
                       AND RETIRED = FALSE
                       AND COMPLETED = TRUE
                       AND APPROVEAT IS NULL),
                    ' completed bills still missing approveAt - may need manual review')
    END as final_result;

SELECT 'Migration v2.1.14 completed - QB Report should now include historical pharmacy returns' AS final_status;
