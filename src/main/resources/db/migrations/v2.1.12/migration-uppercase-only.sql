-- Migration v2.1.12: Backfill missing approval tracking fields for historical direct purchase bills (FIXED CASE SENSITIVITY)
-- Author: Claude AI Assistant
-- Date: 2025-12-30 (Updated: 2026-01-15)
-- GitHub Issue: #17317
-- Description: Updates historical direct purchase bills to include missing approval audit trail data
-- FIXED: All table names now use correct uppercase case (BILL instead of bill)

-- ==============================================================================
-- BACKGROUND
-- ==============================================================================
-- Recent enhancement added approval tracking fields (approveUser, approveAt, editor, editedAt)
-- to the direct purchase settle process to match the GRN approval workflow.
-- Historical direct purchase bills are missing these fields, creating incomplete audit trails.
-- This migration backfills the missing data using existing completion information.

-- ==============================================================================
-- PRE-MIGRATION ANALYSIS
-- ==============================================================================

-- Count total completed direct purchase bills
SELECT 'Total Completed Direct Purchases' as description,
       COUNT(*) as count
FROM BILL
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE;

-- Count bills missing approval data
SELECT 'Bills Missing Approval Data' as description,
       COUNT(*) as count
FROM BILL
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
  AND (APPROVEUSER_ID IS NULL OR APPROVEAT IS NULL OR EDITOR_ID IS NULL OR EDITEDAT IS NULL);

-- Show sample of bills that will be updated
SELECT ID,
       DEPTID,
       BILLDATE,
       COMPLETEDBY_ID,
       COMPLETEDAT,
       APPROVEUSER_ID,
       APPROVEAT,
       EDITOR_ID,
       EDITEDAT
FROM BILL
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
  AND APPROVEUSER_ID IS NULL
LIMIT 5;

-- ==============================================================================
-- MIGRATION STATEMENTS
-- ==============================================================================

-- Step 1: Update approveUser field using completedBy
-- Sets approveUser to the user who completed the transaction
UPDATE BILL
SET APPROVEUSER_ID = COMPLETEDBY_ID
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
  AND APPROVEUSER_ID IS NULL
  AND COMPLETEDBY_ID IS NOT NULL;

-- Check result of Step 1
SELECT 'Updated approveUser fields' as description,
       ROW_COUNT() as affected_rows;

-- Step 2: Update approveAt field using completedAt
-- Sets approveAt to the timestamp when the transaction was completed
UPDATE BILL
SET APPROVEAT = COMPLETEDAT
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
  AND APPROVEAT IS NULL
  AND COMPLETEDAT IS NOT NULL;

-- Check result of Step 2
SELECT 'Updated approveAt fields' as description,
       ROW_COUNT() as affected_rows;

-- Step 3: Update editor field using completedBy
-- Sets editor to the user who completed the transaction
UPDATE BILL
SET EDITOR_ID = COMPLETEDBY_ID
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
  AND EDITOR_ID IS NULL
  AND COMPLETEDBY_ID IS NOT NULL;

-- Check result of Step 3
SELECT 'Updated editor fields' as description,
       ROW_COUNT() as affected_rows;

-- Step 4: Update editedAt field using completedAt
-- Sets editedAt to the timestamp when the transaction was completed
UPDATE BILL
SET EDITEDAT = COMPLETEDAT
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
  AND EDITEDAT IS NULL
  AND COMPLETEDAT IS NOT NULL;

-- Check result of Step 4
SELECT 'Updated editedAt fields' as description,
       ROW_COUNT() as affected_rows;

-- ==============================================================================
-- POST-MIGRATION VERIFICATION
-- ==============================================================================

-- Count remaining bills with missing approval data (should be 0)
SELECT 'Bills Still Missing Approval Data' as description,
       COUNT(*) as count
FROM BILL
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
  AND (APPROVEUSER_ID IS NULL OR APPROVEAT IS NULL OR EDITOR_ID IS NULL OR EDITEDAT IS NULL);

-- Count successfully updated bills
SELECT 'Bills With Complete Approval Data' as description,
       COUNT(*) as count
FROM BILL
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
  AND APPROVEUSER_ID IS NOT NULL
  AND APPROVEAT IS NOT NULL
  AND EDITOR_ID IS NOT NULL
  AND EDITEDAT IS NOT NULL;

-- Verify data consistency (approval fields should match completion fields for historical data)
SELECT 'Data Consistency Check' as description,
       COUNT(*) as consistent_records
FROM BILL
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
  AND APPROVEUSER_ID = COMPLETEDBY_ID
  AND APPROVEAT = COMPLETEDAT
  AND EDITOR_ID = COMPLETEDBY_ID
  AND EDITEDAT = COMPLETEDAT;

-- Sample of updated records for manual verification
SELECT 'Sample Updated Records' as description;
SELECT ID,
       DEPTID,
       BILLDATE,
       COMPLETEDBY_ID,
       COMPLETEDAT,
       APPROVEUSER_ID,
       APPROVEAT,
       EDITOR_ID,
       EDITEDAT,
       CASE
         WHEN APPROVEUSER_ID = COMPLETEDBY_ID AND APPROVEAT = COMPLETEDAT
              AND EDITOR_ID = COMPLETEDBY_ID AND EDITEDAT = COMPLETEDAT
         THEN 'CONSISTENT'
         ELSE 'INCONSISTENT'
       END as data_status
FROM BILL
WHERE BILLTYPEATOMIC = 'PHARMACY_DIRECT_PURCHASE'
  AND COMPLETED = TRUE
ORDER BY BILLDATE DESC
LIMIT 10;

-- ==============================================================================
-- EXPECTED RESULTS
-- ==============================================================================
-- After this migration:
-- 1. All completed direct purchase bills will have complete approval audit trails
-- 2. Historical data will show the original completion user as the approver
-- 3. Timestamps will be consistent between completion and approval for historical data
-- 4. New direct purchase transactions will continue to use the enhanced workflow
-- 5. Audit reports will show complete approval information for all transactions

-- Migration completed successfully