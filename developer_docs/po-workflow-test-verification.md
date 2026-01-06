# PO Workflow Test Verification Guide

## Issue #15028 - Purchase Order Workflow Fix Testing

### Pre-Testing Setup

1. **Test Environment**: Use development/staging environment
2. **Test Users**: 
   - Pharmacy staff user (PO creation rights)
   - Pharmacy supervisor (PO approval rights)
3. **Test Data**: Sample supplier and medication items

### Test Case 1: Complete PO Workflow

#### Step 1: Create PO Request
1. Login as pharmacy staff user
2. Navigate to: `Pharmacy → Purchase Order Request`
3. **Actions**:
   - Select a supplier
   - Add 2-3 medication items with quantities and prices
   - Click "Save"
4. **Expected Result**: 
   - ✅ PO saved successfully
   - ✅ Status shows as draft
   - ✅ Can still edit items

#### Step 2: Finalize PO Request  
1. Continue from Step 1
2. **Actions**:
   - Verify all items have quantities and prices
   - Click "Finalize" button
3. **Expected Result**:
   - ✅ Success message: "Request successfully finalized"
   - ✅ Print preview appears
   - ✅ Shows finalization details (user, timestamp)

#### Step 3: Verify PO in Approval Queue
1. Navigate to: `Pharmacy → PO List to Approve`
2. Click "Search To Approve Requests"
3. **Expected Result**:
   - ✅ Finalized PO appears in list
   - ✅ Status column shows "Pending Approval" (yellow badge)
   - ✅ Approve button is enabled (green checkmark)
   - ✅ Shows finalization timestamp and user

#### Step 4: Approve PO
1. Login as pharmacy supervisor (with approval rights)
2. Navigate to: `Pharmacy → PO List to Approve`
3. Click "Search To Approve Requests"
4. **Actions**:
   - Find the finalized PO
   - Click green approve button (checkmark icon)
5. **Expected Result**:
   - ✅ Navigates to approval page
   - ✅ Shows PO details for review
   - ✅ Can modify quantities if needed
   - ✅ Click "Approve" button

#### Step 5: Verify Approved Status
1. After approval, check PO lists
2. **Expected Results**:
   - ✅ PO no longer appears in "To Approve" list
   - ✅ PO appears in "Approved Requests" search
   - ✅ Status shows "Approved" (green badge)
   - ✅ Has approval number and timestamp

### Test Case 2: Permission Verification

#### Test 2A: User Without Approval Rights
1. Login as regular pharmacy staff (no approval privilege)
2. Navigate to: `Pharmacy → PO List to Approve`
3. **Expected Result**:
   - ✅ Approve button is disabled
   - ✅ Tooltip shows permission required

#### Test 2B: Approval Button States
1. Create POs in different states
2. Check button conditions:
   - **Draft PO**: Approve button disabled (not finalized)
   - **Finalized PO**: Approve button enabled
   - **Already Approved PO**: Approve button disabled
   - **Cancelled PO**: Approve button disabled

### Test Case 3: Error Handling

#### Test 3A: Finalize Without Items
1. Create new PO
2. Select supplier but don't add items
3. Click "Finalize"
4. **Expected Result**: ❌ Error message: "Please add bill items"

#### Test 3B: Finalize Without Supplier
1. Create new PO
2. Add items but don't select supplier
3. Click "Finalize"
4. **Expected Result**: ❌ Error message: "Please select a supplier"

#### Test 3C: Finalize With Zero Quantities
1. Create new PO with items
2. Set item quantities to 0
3. Click "Finalize"
4. **Expected Result**: ❌ Error message about item quantities

### Test Case 4: Status Indicators

#### Visual Status Verification
1. Create POs in different states
2. Navigate to approval list
3. **Verify Status Badges**:
   - Draft: Gray "Draft" badge
   - Finalized: Yellow "Pending Approval" badge  
   - Approved: Green "Approved" badge

### Test Case 5: Workflow Integration

#### Test 5A: GRN Creation After Approval
1. Complete PO approval (Test Case 1)
2. Navigate to: `Pharmacy → GRN`
3. **Expected Result**:
   - ✅ Approved PO appears in GRN creation list
   - ✅ Can create GRN against approved PO

#### Test 5B: Search Functionality
1. Test different search options:
   - "Search All Requests" - shows all POs
   - "Search Approved Requests" - shows only approved
   - "Search To Approve Requests" - shows only finalized

### Test Case 6: Audit Trail

#### Verify Logging
1. Complete full workflow
2. Check database/logs for:
   - ✅ PO creation logged
   - ✅ Finalization logged with user/timestamp
   - ✅ Approval logged with user/timestamp
   - ✅ All actions traceable

### Test Case 7: Regression Testing

#### Existing Functionality
1. **PO Creation**: Verify all existing features work
2. **Item Management**: Add/remove items functions
3. **Supplier Selection**: Autocomplete works
4. **Print Preview**: Generates correctly
5. **Email Functionality**: Send PO email works

### Performance Testing

#### Load Testing
1. Create multiple POs simultaneously
2. Finalize multiple POs
3. Approve multiple POs
4. **Expected**: No performance degradation

### Browser Compatibility

Test on:
- ✅ Chrome (latest)
- ✅ Firefox (latest)  
- ✅ Edge (latest)
- ✅ Safari (if applicable)

### Mobile Responsiveness

1. Test on tablet/mobile devices
2. Verify:
   - ✅ Status badges display correctly
   - ✅ Buttons are clickable
   - ✅ Tables are scrollable

## Test Results Documentation

### Test Execution Log

| Test Case | Status | Notes | Tester | Date |
|-----------|--------|-------|--------|------|
| TC1: Complete Workflow | ⏳ | | | |
| TC2: Permissions | ⏳ | | | |
| TC3: Error Handling | ⏳ | | | |
| TC4: Status Indicators | ⏳ | | | |
| TC5: Integration | ⏳ | | | |
| TC6: Audit Trail | ⏳ | | | |
| TC7: Regression | ⏳ | | | |

### Issue Tracking

| Issue | Severity | Status | Resolution |
|-------|----------|--------|------------|
| | | | |

### Sign-off

- **Developer**: _________________ Date: _______
- **QA Tester**: _________________ Date: _______  
- **Pharmacy Lead**: _____________ Date: _______
- **Hospital IT**: _______________ Date: _______

---

**Test Plan Version**: 1.0  
**Created**: August 19, 2025  
**Issue**: #15028 - Ruhunu Hospital PO Workflow Fix
