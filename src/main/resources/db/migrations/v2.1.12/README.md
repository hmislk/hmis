# Migration v2.1.12: Direct Purchase Approval Data Backfill

## Overview
This migration addresses missing approval tracking fields in historical direct purchase bills. Recent enhancements added comprehensive approval audit trails to the direct purchase workflow, but existing completed transactions lacked this information.

## Background
- **GitHub Issue**: [#17317](https://github.com/hmislk/hmis/issues/17317)
- **Branch**: `17317-unable-to-settle-petty-cash-payments-due-to-error-validation-messages-being-displayed-when-selecting-cash-credit-or-cheque-payment-methods`
- **Code Enhancement**: Added `approveUser`, `approveAt`, `editor`, and `editedAt` fields to direct purchase settle process

## Problem Statement
Historical direct purchase bills completed before the approval enhancement were missing:
- `approveUser` - User who approved the transaction
- `approveAt` - Timestamp of approval
- `editor` - User who edited/approved
- `editedAt` - Timestamp of edit/approval

This created incomplete audit trails and inconsistent data patterns compared to the new workflow.

## Solution
The migration backfills missing approval data using existing completion information:
- Sets `approveUser` = `completedBy` (user who completed the transaction)
- Sets `approveAt` = `completedAt` (timestamp of completion)
- Sets `editor` = `completedBy` (same user for historical consistency)
- Sets `editedAt` = `completedAt` (same timestamp for historical consistency)

## Scope
**Affected Records**: Only completed direct purchase bills (`bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'` AND `completed = TRUE`)

**Safety**:
- Only updates NULL fields - preserves any existing approval data
- Non-destructive operation - can be safely rolled back
- Does not affect current workflow or future transactions

## Verification Queries

### Before Migration
```sql
-- Count bills missing approval data
SELECT COUNT(*) FROM billedbill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND (approve_user IS NULL OR approve_at IS NULL);
```

### After Migration
```sql
-- Verify no missing approval data
SELECT COUNT(*) FROM billedbill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND (approve_user IS NULL OR approve_at IS NULL OR editor IS NULL OR edited_at IS NULL);
-- Result should be 0

-- Verify data consistency for historical records
SELECT COUNT(*) FROM billedbill
WHERE bill_type_atomic = 'PHARMACY_DIRECT_PURCHASE'
  AND completed = TRUE
  AND approve_user = completed_by
  AND approve_at = completed_at;
-- Result should match the number of records updated by migration
```

## Rollback Safety
- Rollback only affects records where approval data exactly matches completion data
- Records with manually set approval data (different from completion data) are preserved
- Safe to execute rollback multiple times if needed
- No impact on current workflow functionality

## Business Impact
**Positive:**
- Complete audit trails for all direct purchase transactions
- Consistent data patterns across historical and new records
- Enhanced compliance and reporting capabilities
- Improved data integrity for audit purposes

**Risk Assessment:**
- **Very Low Risk**: Only adds missing data, doesn't modify existing business logic
- **No Downtime Required**: Migration operates on historical data only
- **Reversible**: Complete rollback capability available

## Testing Recommendations
1. **Pre-Migration**: Count records missing approval data
2. **Post-Migration**: Verify all completed direct purchases have approval fields
3. **Data Consistency**: Confirm approval data matches completion data for historical records
4. **Workflow Test**: Verify new direct purchase transactions continue to work normally

## Related Changes
- **PharmacyDirectPurchaseController.java**: Enhanced `approveBill()` method
- **Direct Purchase Workflow**: Now creates complete approval audit trails
- **Future GRN Enhancement**: Planned workflow splitting for consistency

## Maintenance Notes
- This is a one-time data fix migration
- No ongoing maintenance required
- Future workflow enhancements should consider this historical data pattern
- Consider similar migrations for other transaction types if approval workflows are enhanced

## Contact
- **Developer**: Claude AI Assistant
- **Issue Tracker**: [GitHub Issues](https://github.com/hmislk/hmis/issues)
- **Documentation**: [Project Wiki](https://github.com/hmislk/hmis/wiki)