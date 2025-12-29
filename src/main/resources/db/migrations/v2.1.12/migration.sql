-- Migration v2.1.12: Backfill missing approval tracking fields for historical direct purchase bills
-- Author: Claude AI Assistant
-- Date: 2025-12-30
-- GitHub Issue: #17317
-- Description: Updates historical direct purchase bills to include missing approval audit trail data

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
FROM bill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE;

-- Count bills missing approval data
SELECT 'Bills Missing Approval Data' as description,
       COUNT(*) as count
FROM bill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND (approve_user IS NULL OR approve_at IS NULL OR editor IS NULL OR edited_at IS NULL);

-- Show sample of bills that will be updated
SELECT id,
       deptId,
       billdate,
       completed_by,
       completed_at,
       approve_user,
       approve_at,
       editor,
       edited_at
FROM bill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND approve_user IS NULL
LIMIT 5;

-- ==============================================================================
-- MIGRATION STATEMENTS
-- ==============================================================================

-- Step 1: Update approveUser field using completedBy
-- Sets approveUser to the user who completed the transaction
UPDATE bill
SET approve_user = completed_by
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND approve_user IS NULL
  AND completed_by IS NOT NULL;

-- Check result of Step 1
SELECT 'Updated approveUser fields' as description,
       ROW_COUNT() as affected_rows;

-- Step 2: Update approveAt field using completedAt
-- Sets approveAt to the timestamp when the transaction was completed
UPDATE bill
SET approve_at = completed_at
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND approve_at IS NULL
  AND completed_at IS NOT NULL;

-- Check result of Step 2
SELECT 'Updated approveAt fields' as description,
       ROW_COUNT() as affected_rows;

-- Step 3: Update editor field using completedBy
-- Sets editor to the user who completed the transaction
UPDATE bill
SET editor = completed_by
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND editor IS NULL
  AND completed_by IS NOT NULL;

-- Check result of Step 3
SELECT 'Updated editor fields' as description,
       ROW_COUNT() as affected_rows;

-- Step 4: Update editedAt field using completedAt
-- Sets editedAt to the timestamp when the transaction was completed
UPDATE bill
SET edited_at = completed_at
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND edited_at IS NULL
  AND completed_at IS NOT NULL;

-- Check result of Step 4
SELECT 'Updated editedAt fields' as description,
       ROW_COUNT() as affected_rows;

-- ==============================================================================
-- POST-MIGRATION VERIFICATION
-- ==============================================================================

-- Count remaining bills with missing approval data (should be 0)
SELECT 'Bills Still Missing Approval Data' as description,
       COUNT(*) as count
FROM bill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND (approve_user IS NULL OR approve_at IS NULL OR editor IS NULL OR edited_at IS NULL);

-- Count successfully updated bills
SELECT 'Bills With Complete Approval Data' as description,
       COUNT(*) as count
FROM bill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND approve_user IS NOT NULL
  AND approve_at IS NOT NULL
  AND editor IS NOT NULL
  AND edited_at IS NOT NULL;

-- Verify data consistency (approval fields should match completion fields for historical data)
SELECT 'Data Consistency Check' as description,
       COUNT(*) as consistent_records
FROM bill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND approve_user = completed_by
  AND approve_at = completed_at
  AND editor = completed_by
  AND edited_at = completed_at;

-- Sample of updated records for manual verification
SELECT 'Sample Updated Records' as description;
SELECT id,
       deptId,
       billdate,
       completed_by,
       completed_at,
       approve_user,
       approve_at,
       editor,
       edited_at,
       CASE
         WHEN approve_user = completed_by AND approve_at = completed_at
              AND editor = completed_by AND edited_at = completed_at
         THEN 'CONSISTENT'
         ELSE 'INCONSISTENT'
       END as data_status
FROM bill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
ORDER BY billdate DESC
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