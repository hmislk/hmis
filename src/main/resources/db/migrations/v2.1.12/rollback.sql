-- Rollback for Migration v2.1.12: Backfill missing approval tracking fields for historical direct purchase bills
-- Author: Claude AI Assistant
-- Date: 2025-12-30
-- Description: Safely reverts the approval field backfill for historical direct purchase bills

-- ==============================================================================
-- ROLLBACK SAFETY CHECKS
-- ==============================================================================

-- Verify current state before rollback
SELECT 'Pre-Rollback State Check' as description;

-- Count bills with approval data that matches completion data (these will be rolled back)
SELECT 'Bills with matching approval/completion data' as description,
       COUNT(*) as count
FROM billedbill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND approve_user = completed_by
  AND approve_at = completed_at
  AND editor = completed_by
  AND edited_at = completed_at;

-- Show sample records that will be affected by rollback
SELECT id,
       deptId,
       billdate,
       completed_by,
       completed_at,
       approve_user,
       approve_at,
       editor,
       edited_at
FROM billedbill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND approve_user = completed_by
  AND approve_at = completed_at
LIMIT 5;

-- ==============================================================================
-- ROLLBACK STATEMENTS
-- ==============================================================================

-- Step 1: Reset approveUser to NULL for historical records where it matches completedBy
-- Only affects records where approval data matches completion data (migration artifacts)
UPDATE billedbill
SET approve_user = NULL
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND approve_user = completed_by
  AND approve_at = completed_at;

-- Check result of rollback Step 1
SELECT 'Rolled back approveUser fields' as description,
       ROW_COUNT() as affected_rows;

-- Step 2: Reset approveAt to NULL for historical records where it matches completedAt
UPDATE billedbill
SET approve_at = NULL
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND approve_at = completed_at
  AND editor = completed_by;

-- Check result of rollback Step 2
SELECT 'Rolled back approveAt fields' as description,
       ROW_COUNT() as affected_rows;

-- Step 3: Reset editor to NULL for historical records where it matches completedBy
UPDATE billedbill
SET editor = NULL
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND editor = completed_by
  AND edited_at = completed_at;

-- Check result of rollback Step 3
SELECT 'Rolled back editor fields' as description,
       ROW_COUNT() as affected_rows;

-- Step 4: Reset editedAt to NULL for historical records where it matches completedAt
UPDATE billedbill
SET edited_at = NULL
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND edited_at = completed_at;

-- Check result of rollback Step 4
SELECT 'Rolled back editedAt fields' as description,
       ROW_COUNT() as affected_rows;

-- ==============================================================================
-- POST-ROLLBACK VERIFICATION
-- ==============================================================================

-- Verify rollback completed successfully
SELECT 'Post-Rollback State Check' as description;

-- Count bills with NULL approval data (should match pre-migration count)
SELECT 'Bills with NULL approval data' as description,
       COUNT(*) as count
FROM billedbill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND (approve_user IS NULL OR approve_at IS NULL OR editor IS NULL OR edited_at IS NULL);

-- Count bills still having approval data that matches completion data (should be 0 or very few)
SELECT 'Bills with matching approval/completion data (should be minimal)' as description,
       COUNT(*) as count
FROM billedbill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND approve_user = completed_by
  AND approve_at = completed_at
  AND editor = completed_by
  AND edited_at = completed_at;

-- Sample of rolled back records
SELECT 'Sample Rolled Back Records' as description;
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
         WHEN approve_user IS NULL AND approve_at IS NULL
              AND editor IS NULL AND edited_at IS NULL
         THEN 'ROLLED_BACK'
         ELSE 'HAS_APPROVAL_DATA'
       END as rollback_status
FROM billedbill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
ORDER BY billdate DESC
LIMIT 10;

-- ==============================================================================
-- IMPORTANT NOTES
-- ==============================================================================
-- 1. This rollback only affects historical data filled by the migration
-- 2. Records with manually set approval data (different from completion data) are preserved
-- 3. Future direct purchase transactions will continue to work normally
-- 4. The enhanced approval workflow remains active for new transactions
-- 5. This rollback can be safely re-executed if needed

-- Rollback completed successfully