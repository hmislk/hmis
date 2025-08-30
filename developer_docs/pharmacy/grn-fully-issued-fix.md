# GRN fullyIssued Flag Fix

## Issue Description

**Problem**: When a Purchase Order is approved (PHARMACY_ORDER_APPROVAL), the Create GRN button becomes disabled because the `fullyIssued` flag was incorrectly set to `true` on the approval bill during GRN creation/finalization.

**Root Cause**: Some GRN finalization methods were missing the logic to check if the purchase order was fully received and set `fullyIssued=true` appropriately. This caused inconsistent behavior where some GRN operations would set the flag and others wouldn't.

## Data Analysis

The issue was visible in the database:
```sql
SELECT ID, BILLTYPE, BILLTYPEATOMIC, FULLYISSUED, FULLYISSUEDAT, FULLYISSUEDBY_ID 
FROM bill 
WHERE BILLTYPEATOMIC = 'PHARMACY_ORDER_APPROVAL';
```

Results showed:
- ID 3565831: PHARMACY_ORDER_APPROVAL with `fullyIssued=true` (after GRN finalization)
- ID 3565436: PHARMACY_ORDER_APPROVAL with `fullyIssued=false` (before full receipt)

## Workflow Context

1. Purchase Order (PHARMACY_ORDER) is created
2. Purchase Order is approved → creates PHARMACY_ORDER_APPROVAL bill
3. PHARMACY_ORDER_APPROVAL bills appear in "Purchase Orders for Receiving" page
4. Create GRN button uses: `disabled="#{p.cancelled or p.billClosed or p.fullyIssued}"`
5. When `fullyIssued=true`, the Create GRN button becomes disabled (correct behavior when all items fully received)

## Solution Implemented

**Fix**: Ensured all GRN finalization methods consistently check and set `fullyIssued=true` when the purchase order is fully received.

### Files Modified
- `src/main/java/com/divudi/bean/pharmacy/GrnController.java`

### Changes Made
Added consistent `fullyIssued` logic to all GRN finalization methods:
1. `settle()` (around line 709-717) - **ADDED**
2. `settleWholesale()` (around line 835-843) - **ADDED** 
3. `requestFinalizeWithSaveApprove()` (around line 2025-2033) - **ALREADY EXISTED**

### Code Added
```java
// Check if Purchase Order is fully received and update fullyIssued status
if (getApproveBill() != null && !getApproveBill().isFullyIssued()) {
    if (isPurchaseOrderFullyReceived(getApproveBill())) {
        getApproveBill().setFullyIssued(true);
        getApproveBill().setFullyIssuedAt(new Date());
        getApproveBill().setFullyIssuedBy(getSessionController().getLoggedUser());
        getBillFacade().edit(getApproveBill());
    }
}
```

## Important Notes

**DO NOT apply this fix to other controllers**: 
- GrnCostingController and other controllers have their own business logic for `fullyIssued` handling
- Only apply fixes to the specific controller experiencing the reported issue
- Each controller may have different business logic requirements

## Business Logic Clarification

The `fullyIssued` flag on PHARMACY_ORDER_APPROVAL bills should be set to `true` when:
- **During GRN finalization**: When ALL items from the approved purchase order have been fully received through GRN(s)
- **NOT during PO approval**: The approval process itself should not set this flag

The `isPurchaseOrderFullyReceived()` method checks if all ordered quantities (including free quantities) have been received through GRNs.

## Testing

After this fix:
1. Approved purchase orders should remain available for GRN creation until fully received
2. Create GRN button should be disabled only when `fullyIssued=true` (i.e., when all items are fully received)
3. The approval workflow should continue to function normally
4. All GRN finalization methods now consistently check and set the `fullyIssued` flag
5. Other controllers (GrnCostingController) should remain unchanged

## Related Files
- `src/main/webapp/pharmacy/pharmacy_purchase_order_list_for_recieve.xhtml` (button disable logic)
- `src/main/java/com/divudi/bean/pharmacy/PurchaseOrderController.java` (approval creation)
- `src/main/java/com/divudi/bean/pharmacy/GrnCostingController.java` (NOT modified - has own logic)