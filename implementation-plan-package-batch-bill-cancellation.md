# Package Batch Bill Cancellation Payment Flexibility Implementation Plan

**Date**: 2025-12-20
**Issue**: Improve OPD Package Batch Bill Cancellation
**Scope**: Focus on batch bill cancellation only (individual package bill cancellation was already fixed in PR #17398)

## Overview

Currently, package batch bill cancellation restricts payment method to the original bill's payment method. This implementation will allow any payment combination while defaulting to original payment details, following the same patterns established in individual bill cancellation guidelines.

## Requirements Summary

1. **Payment Flexibility**: Allow any payment combination for cancellation (not restricted to original)
2. **Default Behavior**: Populate original payment details by default, allow user to change
3. **Negative Payments**: Create negative payment values for money leaving hospital
4. **Balance Updates**: Handle patient deposits, staff welfare, and other payment types properly
5. **Audit Trail**: Display original payment details for verification
6. **Full Refunds**: Always cancel with full net total (no partial cancellations)
7. **Batch Level**: Use batch bill level payments (not individual bill aggregates)

## Current State Analysis

### Files Involved
- **Controller**: `/src/main/java/com/divudi/bean/common/BillPackageController.java`
- **UI**: `/src/main/webapp/opd/opd_package_batch_bill_cancel.xhtml`
- **Navigation**: Page accessed from "To Cancel Batch Bill" button on `opd_package_batch_bill_print.xhtml`

### Current Issues
1. `cancelPackageBatchBill()` method uses old payment creation patterns
2. `navigateToCancelOpdPackageBatchBill()` doesn't load original payment details
3. Manual balance updates only handle PatientDeposit and Credit (misses Staff_Welfare)
4. No original payment details display in UI
5. Payment method restriction to original method

## Implementation Plan

### Phase 1: Java Backend Changes

#### 1.1 Update Navigation Method (`navigateToCancelOpdPackageBatchBill()`)

**Location**: BillPackageController.java, around line 2211

**Changes Required**:
```java
public String navigateToCancelOpdPackageBatchBill() {
    if (batchBill == null) {
        JsfUtil.addErrorMessage("No Batch Bill is selected to cancel");
        return "";
    }

    // Load original payment details from batch bill
    originalBillPayments = billService.fetchBillPayments(batchBill);

    // Initialize cancellation payment data from original payments
    initializeCancellationPaymentFromOriginalPayments(originalBillPayments);

    // Allow all payment methods (remove restriction)
    paymentMethods = new ArrayList<>();
    for (PaymentMethod pm : PaymentMethod.values()) {
        paymentMethods.add(pm);
    }

    // Set default payment method to original or Cash as fallback
    if (originalBillPayments != null && !originalBillPayments.isEmpty()) {
        paymentMethod = originalBillPayments.get(0).getPaymentMethod();
    } else {
        paymentMethod = PaymentMethod.Cash;
    }

    return "/opd/opd_package_batch_bill_cancel?faces-redirect=true";
}
```

#### 1.2 Refactor Main Cancellation Method (`cancelPackageBatchBill()`)

**Location**: BillPackageController.java, lines 747-847

**Key Changes**:
1. Add staff data transfer before payment creation
2. Replace old payment creation with modern PaymentService
3. Remove manual balance updates (let PaymentService handle all)
4. Apply negative signs to payment data

**Specific Modifications**:

**Before payment creation** (around line 808):
```java
// Transfer staff data to paymentMethodData before creating payments
transferStaffDataToPaymentMethodData();

// Apply negative signs for cancellation
applyCancellationSignToPaymentData();

getBillFacade().create(cancellationBatchBill);
```

**Replace payment creation section** (lines 829-843):
```java
// OLD CODE TO REMOVE:
// if (cancellationBatchBill.getPaymentMethod() == PaymentMethod.PatientDeposit) {
//     PatientDeposit pd = patientDepositController.getDepositOfThePatient(...);
//     patientDepositController.updateBalance(...);
// } else if (cancellationBatchBill.getPaymentMethod() == PaymentMethod.Credit) {
//     if (cancellationBatchBill.getToStaff() != null) {
//         staffService.updateStaffCredit(...);
//     }
// }
// payments = paymentService.createPaymentsForCancelling(cancellationBatchBill);

// NEW CODE:
// Create payments using modern PaymentService with paymentMethodData
payments = paymentService.createPayment(cancellationBatchBill, paymentMethodData);

// Update all balances automatically
paymentService.updateBalances(payments);
```

#### 1.3 Add Missing Helper Method

**Location**: BillPackageController.java (add new method)

```java
/**
 * Initialize cancellation payment data from original batch bill payments
 */
private void initializeCancellationPaymentFromOriginalPayments(List<Payment> originalPayments) {
    // This method should follow the same pattern as in individual bill cancellation
    // Call the existing method that's already implemented in the controller
    initializeCancellationPaymentFromOriginalPayments();
}
```

### Phase 2: UI Enhancements

#### 2.1 Add Original Payment Details Panel

**Location**: `/src/main/webapp/opd/opd_package_batch_bill_cancel.xhtml`

**Add after line 75** (before Payment Details panel):

```xml
<!-- Original Payment Details Panel -->
<p:panel rendered="#{not empty billPackageController.originalBillPayments}"
         header="Original Payment Details">
    <p:dataTable value="#{billPackageController.originalBillPayments}" var="op">
        <p:column headerText="Method">
            <h:outputText value="#{op.paymentMethod}" />
        </p:column>
        <p:column headerText="Amount">
            <h:outputText value="#{op.paidValue}">
                <f:convertNumber pattern="#,##0.00" />
            </h:outputText>
        </p:column>
        <p:column headerText="Staff" rendered="#{op.paymentMethod eq 'Staff_Welfare' or op.paymentMethod eq 'Staff'}">
            <h:outputText value="#{op.toStaff.person.name}" />
        </p:column>
        <p:column headerText="Institution" rendered="#{op.paymentMethod eq 'Credit' or op.paymentMethod eq 'Card'}">
            <h:outputText value="#{op.creditCompany.name}" rendered="#{op.paymentMethod eq 'Credit'}"/>
            <h:outputText value="#{op.bank.name}" rendered="#{op.paymentMethod eq 'Card'}"/>
        </p:column>
        <p:column headerText="Reference" rendered="#{op.paymentMethod eq 'Credit' or op.paymentMethod eq 'Card' or op.paymentMethod eq 'Cheque'}">
            <h:outputText value="#{op.referenceNo}" rendered="#{op.paymentMethod eq 'Credit'}"/>
            <h:outputText value="#{op.creditCardRefNo}" rendered="#{op.paymentMethod eq 'Card'}"/>
            <h:outputText value="#{op.chequeRefNo}" rendered="#{op.paymentMethod eq 'Cheque'}"/>
        </p:column>
    </p:dataTable>
</p:panel>
```

#### 2.2 Update Payment Method Selection

**Location**: Lines 88-102 in the same file

**Modify the select one menu**:
```xml
<p:selectOneMenu
    id="pay"
    class="w-100"
    value="#{billPackageController.paymentMethod}">
    <!-- Remove "Same as Billed" option, show actual methods -->
    <f:selectItems
        value="#{billPackageController.paymentMethods}"
        var="pm"
        itemLabel="#{pm.label}"
        itemValue="#{pm}"/>
    <p:ajax
        process="@this cmbPs"
        update="paymentDetails"
        event="change"/>
</p:selectOneMenu>
```

### Phase 3: Testing Strategy

#### 3.1 Test Scenarios

1. **Cash Original → Different Payment Methods**
   - Original: Cash payment
   - Cancel with: Staff Welfare, Patient Deposit, Credit Card
   - Verify: Negative payments created, balances updated correctly

2. **Staff Welfare Original → Staff Welfare Cancel**
   - Original: Staff welfare payment to Staff A
   - Cancel with: Staff welfare to Staff B
   - Verify: Staff A gets refund, negative payment recorded

3. **Multiple Payment Methods**
   - Original: Multiple payment methods (Cash + Card)
   - Cancel with: Single payment method or different combination
   - Verify: All original payments shown, new payments created correctly

4. **Patient Deposit Scenarios**
   - Original: Patient deposit payment
   - Cancel with: Patient deposit
   - Verify: Patient deposit balance increased properly

#### 3.2 Balance Verification

For each test:
1. Record balances before cancellation
2. Execute cancellation
3. Verify balance changes match payment amounts
4. Check audit trail completeness

## Risk Assessment

### Low Risk
- UI changes (display panels, dropdown modifications)
- Adding original payment loading to navigation

### Medium Risk
- Modifying payment creation logic
- Balance update integration

### Mitigation Strategies
1. **Incremental Implementation**: Implement navigation and UI first, then payment logic
2. **Existing Pattern Reuse**: Use proven patterns from individual bill cancellation
3. **Comprehensive Testing**: Test all payment method combinations before deployment

## Dependencies

### Required Services
- `PaymentService.createPayment()` and `updateBalances()` methods
- `BillService.fetchBillPayments()` method
- Existing `initializeCancellationPaymentFromOriginalPayments()` method

### UI Components
- Existing payment method composite components in `/resources/paymentMethod/`
- Standard PrimeFaces components for data display

## Deployment Considerations

1. **Database**: No schema changes required
2. **Configuration**: No new configuration needed
3. **Compatibility**: Maintains backward compatibility with existing bills
4. **Performance**: Minimal impact (same number of database operations)

## Success Criteria

1. ✅ User can select any payment method for batch bill cancellation
2. ✅ Original payment details are displayed for audit purposes
3. ✅ Staff welfare, patient deposits, and all payment types refunded correctly
4. ✅ Negative payment amounts created for money leaving hospital
5. ✅ Balance updates work for all payment method combinations
6. ✅ Audit trail maintained for compliance
7. ✅ UI follows existing application patterns and styling

## Next Steps

1. **Phase 1**: Implement Java backend changes in BillPackageController
2. **Phase 2**: Add UI enhancements for original payment display
3. **Phase 3**: Execute comprehensive testing with all payment methods
4. **Phase 4**: User acceptance testing and deployment

---

**Note**: This plan follows the exact patterns established in `/developer_docs/billing/payment-cancellation-guidelines.md` and ensures consistency with the already-implemented individual package bill cancellation improvements from PR #17398.