# Payment Cancellation Implementation Guidelines for HMIS

**Document Version**: 1.0
**Created**: 2025-12-20
**Author**: HMIS Development Team
**Scope**: All bill cancellation features (OPD, Package, Pharmacy, etc.)

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [PaymentService Integration Pattern](#paymentservice-integration-pattern)
3. [Payment Method Detail Preservation](#payment-method-detail-preservation)
4. [BillTypeAtomic Requirements](#billtypeatomic-requirements)
5. [Account Balance Updates](#account-balance-updates)
6. [Implementation Examples](#implementation-examples)
7. [UI Guidelines](#ui-guidelines)
8. [Testing Checklist](#testing-checklist)
9. [Common Pitfalls](#common-pitfalls)

---

## Executive Summary

### Core Principles

1. **🚨 ALWAYS use PaymentService**: Never create payments manually - use `paymentService.createPayment()`
2. **🚨 PRESERVE payment details**: All original payment details must be auto-populated during cancellation
3. **🚨 PROPER BillTypeAtomic**: Use correct cancellation-specific BillTypeAtomic for balance updates
4. **🚨 NEGATIVE payment values**: Apply negation to payment data before creation

### Quick Reference

```java
// ✅ CORRECT cancellation pattern
applyCancellationSignToPaymentData();                        // Step 1: Negate values
List<Payment> payments = paymentService.createPayment(       // Step 2: Create payments
    cancellationBill, getPaymentMethodData());
paymentService.updateBalances(payments);                     // Step 3: Update balances

// ❌ WRONG - Manual payment creation
Payment p = new Payment();
p.setPaidValue(-bill.getNetTotal());  // DON'T DO THIS
```

---

## PaymentService Integration Pattern

### Standard Implementation Flow

```java
public String cancelBill() {
    // 1. Validation and setup
    if (errorsPresentOnCancellation()) {
        return "";
    }

    // 2. Create cancellation bill with proper BillTypeAtomic
    Bill cancellationBill = new CancelledBill();
    cancellationBill.setBillTypeAtomic(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
    cancellationBill.setCreater(sessionController.getLoggedUser());
    cancellationBill.setDepartment(sessionController.getDepartment());
    cancellationBill.setInstitution(sessionController.getInstitution());
    // ... set other bill properties

    billFacade.create(cancellationBill);

    // 3. Initialize cancellation payment from original payment details
    initializeCancellationPaymentFromOriginalPayments();

    // 4. Apply negative signs to payment data for cancellation
    applyCancellationSignToPaymentData();

    // 5. Create payments using PaymentService
    List<Payment> payments = paymentService.createPayment(cancellationBill, getPaymentMethodData());

    // 6. Update all account balances using PaymentService
    paymentService.updateBalances(payments);

    // 7. Complete cancellation
    originalBill.setCancelled(true);
    originalBill.setCancelledBill(cancellationBill);
    billFacade.edit(originalBill);

    return "success";
}
```

### Why PaymentService is Required

**✅ PaymentService Benefits**:
- **Automatic negative payment creation** for cancellations
- **Database persistence** with proper foreign key handling
- **Cashbook and drawer updates** handled automatically
- **Account balance updates** through `updateBalances()`
- **Audit trail creation** for all payment transactions
- **Support for multiple payment methods** in single transaction

**❌ Manual Payment Creation Problems**:
- Foreign key constraint violations
- Missing cashbook entries
- No drawer balance updates
- Incomplete audit trails
- Balance update inconsistencies

---

## Payment Method Detail Preservation

### Problem Statement

When canceling bills, users expect all original payment details to be automatically populated:

**Example Issue**:
- Original bill: Staff Welfare payment with "Buddhika Illeperuma"
- Cancellation: Payment method shows "Staff" but staff name field is empty
- **Expected**: Staff name should be pre-populated with "Buddhika Illeperuma"

### Required Implementation

#### Step 1: Retrieve Original Payment Details

```java
private List<Payment> originalBillPayments;
private boolean originalBillCredit;

private void loadOriginalPaymentDetails() {
    // Fetch all payments for the original bill
    originalBillPayments = billService.fetchBillPayments(bill.getBackwardReferenceBill());

    // Check if original bill was a credit bill
    originalBillCredit = (bill.getBackwardReferenceBill().getPaymentMethod() == PaymentMethod.Credit);

    // Log for debugging
    System.out.println("Found " + (originalBillPayments != null ? originalBillPayments.size() : 0) +
                      " original payments");
}
```

#### Step 2: Auto-populate Payment Method Details

```java
private void initializeCancellationPaymentFromOriginalPayments() {
    if (originalBillPayments == null || originalBillPayments.isEmpty()) {
        return;
    }

    if (originalBillPayments.size() == 1) {
        // Single payment method - populate specific details
        Payment originalPayment = originalBillPayments.get(0);

        // Set default payment method from original
        paymentMethod = originalPayment.getPaymentMethod();

        switch (originalPayment.getPaymentMethod()) {
            case PatientDeposit:
                populatePatientDepositDetails(originalPayment);
                break;

            case Staff_Welfare:
                populateStaffWelfareDetails(originalPayment);
                break;

            case Staff:
            case OnCall:
                populateStaffCreditDetails(originalPayment);
                break;

            case Credit:
                populateCreditCompanyDetails(originalPayment);
                break;

            case Card:
                populateCardDetails(originalPayment);
                break;

            case Cheque:
                populateChequeDetails(originalPayment);
                break;

            case Slip:
                populateSlipDetails(originalPayment);
                break;

            case ewallet:
                populateEWalletDetails(originalPayment);
                break;

            default:
                // Cash and other methods don't need specific details
                break;
        }
    } else {
        // Multiple payment methods - set up component details list
        populateMultiplePaymentMethodDetails();
    }
}
```

#### Step 3: Specific Payment Detail Population Methods

```java
private void populateStaffWelfareDetails(Payment originalPayment) {
    // Auto-populate staff member details
    if (originalPayment.getToStaff() != null) {
        getPaymentMethodData().getStaff_welfare().setToStaff(originalPayment.getToStaff());
        getPaymentMethodData().getStaff_welfare().setTotalValue(Math.abs(bill.getNetTotal()));
        getPaymentMethodData().getStaff_welfare().setComment(originalPayment.getComments());

        // Display staff balance information
        Staff staff = originalPayment.getToStaff();
        JsfUtil.addSuccessMessage("Staff Welfare - " + staff.getPerson().getName() +
                                 " (Current Balance: " + staff.getCurrentCredit() + ")");
    }
}

private void populateCreditCompanyDetails(Payment originalPayment) {
    // Auto-populate credit company details
    if (originalPayment.getCreditCompany() != null) {
        getPaymentMethodData().getCredit().setCreditCompany(originalPayment.getCreditCompany());
        getPaymentMethodData().getCredit().setTotalValue(Math.abs(bill.getNetTotal()));
        getPaymentMethodData().getCredit().setComment(originalPayment.getComments());

        // Set bill-level credit company for proper processing
        setCreditCompany(originalPayment.getCreditCompany());

        JsfUtil.addSuccessMessage("Credit Company - " + originalPayment.getCreditCompany().getName() +
                                 " will process the refund");
    }
}

private void populateCardDetails(Payment originalPayment) {
    // Auto-populate card payment details
    if (originalPayment.getBank() != null || originalPayment.getReferenceNo() != null) {
        getPaymentMethodData().getCard().setBank(originalPayment.getBank());
        getPaymentMethodData().getCard().setReferenceNo(originalPayment.getReferenceNo());
        getPaymentMethodData().getCard().setTotalValue(Math.abs(bill.getNetTotal()));
        getPaymentMethodData().getCard().setComment(originalPayment.getComments());

        JsfUtil.addSuccessMessage("Card refund will be processed" +
                                 (originalPayment.getBank() != null ? " via " + originalPayment.getBank().getName() : ""));
    }
}

private void populateChequeDetails(Payment originalPayment) {
    // Auto-populate cheque details
    getPaymentMethodData().getCheque().setBank(originalPayment.getBank());
    getPaymentMethodData().getCheque().setReferenceNo(originalPayment.getReferenceNo());
    getPaymentMethodData().getCheque().setTotalValue(Math.abs(bill.getNetTotal()));
    getPaymentMethodData().getCheque().setComment(originalPayment.getComments());
}

private void populateSlipDetails(Payment originalPayment) {
    // Auto-populate slip details
    getPaymentMethodData().getSlip().setBank(originalPayment.getBank());
    getPaymentMethodData().getSlip().setReferenceNo(originalPayment.getReferenceNo());
    getPaymentMethodData().getSlip().setTotalValue(Math.abs(bill.getNetTotal()));
    getPaymentMethodData().getSlip().setComment(originalPayment.getComments());
}

private void populateEWalletDetails(Payment originalPayment) {
    // Auto-populate eWallet details
    getPaymentMethodData().getEwallet().setTotalValue(Math.abs(bill.getNetTotal()));
    getPaymentMethodData().getEwallet().setComment(originalPayment.getComments());

    // If eWallet had specific provider details, populate those too
    if (originalPayment.getReferenceNo() != null) {
        getPaymentMethodData().getEwallet().setReferenceNumber(originalPayment.getReferenceNo());
    }
}

private void populatePatientDepositDetails(Payment originalPayment) {
    // Auto-populate patient deposit details
    if (bill.getPatient() != null) {
        getPaymentMethodData().getPatient_deposit().setPatient(bill.getPatient());
        getPaymentMethodData().getPatient_deposit().setTotalValue(Math.abs(bill.getNetTotal()));
        getPaymentMethodData().getPatient_deposit().setComment(originalPayment.getComments());

        // Load and display current deposit balance
        PatientDeposit pd = patientDepositController.getDepositOfThePatient(
            bill.getPatient(), sessionController.getDepartment());
        if (pd != null && pd.getId() != null) {
            getPaymentMethodData().getPatient_deposit().getPatient().setHasAnAccount(true);
            getPaymentMethodData().getPatient_deposit().setPatientDepost(pd);

            JsfUtil.addSuccessMessage("Patient Deposit - Current Balance: " +
                                     String.format("%.2f", pd.getBalance()));
        }
    }
}
```

#### Step 4: Multiple Payment Method Support

```java
private void populateMultiplePaymentMethodDetails() {
    // Set payment method to multiple
    paymentMethod = PaymentMethod.MultiplePaymentMethods;

    // Initialize component details list
    componentDetails = new ArrayList<>();

    for (Payment originalPayment : originalBillPayments) {
        ComponentDetail cd = new ComponentDetail();
        cd.setPaymentMethod(originalPayment.getPaymentMethod());
        cd.setTotalValue(Math.abs(originalPayment.getPaidValue()));
        cd.setComment(originalPayment.getComments());

        // Copy specific payment details based on method type
        switch (originalPayment.getPaymentMethod()) {
            case Staff_Welfare:
                cd.setToStaff(originalPayment.getToStaff());
                break;
            case Credit:
                cd.setCreditCompany(originalPayment.getCreditCompany());
                break;
            case Card:
            case Cheque:
            case Slip:
                cd.setBank(originalPayment.getBank());
                cd.setReferenceNo(originalPayment.getReferenceNo());
                break;
            // Add other payment methods as needed
        }

        componentDetails.add(cd);
    }

    JsfUtil.addSuccessMessage("Multiple payment method cancellation prepared with " +
                             componentDetails.size() + " payment methods");
}
```

---

## BillTypeAtomic Requirements

### Correct BillTypeAtomic Values

**Package Bill Cancellations**:
- `BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION` - Individual package bill cancellation
- `BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION` - Batch package bill cancellation

**OPD Bill Cancellations**:
- `BillTypeAtomic.OPD_BILL_CANCELLATION` - Individual OPD bill cancellation
- `BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION` - Batch OPD bill cancellation

**Pharmacy Cancellations**:
- `BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED` - Retail sale cancellation
- `BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED` - Direct purchase cancellation

### Why BillTypeAtomic Matters

**PatientDepositService.updateBalance()** uses BillTypeAtomic to determine the correct balance update action:

```java
// From PatientDepositService.java
switch (p.getBill().getBillTypeAtomic()) {
    case PACKAGE_OPD_BATCH_BILL_CANCELLATION:
    case PACKAGE_OPD_BILL_CANCELLATION:
    case OPD_BILL_CANCELLATION:
        handleInPayment(p, pd);  // Adds money back to patient deposit
        break;

    case PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT:
    case OPD_BATCH_BILL_WITH_PAYMENT:
        handleOutPayment(p, pd); // Deducts money from patient deposit
        break;
}
```

**Key Point**: Using wrong BillTypeAtomic will cause **incorrect balance updates**!

---

## Account Balance Updates

### PaymentService.updateBalances() Method

```java
// PaymentService automatically handles balance updates for:
paymentService.updateBalances(payments);

// This method updates:
// - Patient deposit balances (via PatientDepositService)
// - Staff welfare balances (via StaffService)
// - Staff credit balances (via StaffService)
// - Credit company balances (via InstitutionService)
// - Patient running balances
// - Audit history records
```

### Supported Payment Methods

| Payment Method | Balance Updated | Service Used |
|----------------|----------------|--------------|
| `PatientDeposit` | Patient deposit account | `PatientDepositService.updateBalance()` |
| `Staff_Welfare` | Staff welfare account | `StaffService.updateStaffWelfare()` |
| `Staff` / `OnCall` | Staff credit account | `StaffService.updateStaffCredit()` |
| `Credit` | Credit company balance | `InstitutionService.updateCredit()` |
| `Cash` / `Card` / `Cheque` / `Slip` | No balance update | N/A |

### Audit Trail Creation

PaymentService automatically creates audit records:

- **PatientDepositHistory** - For patient deposit transactions
- **Payment** records with proper **creater**, **department**, **institution** from bill context
- **BillAudit** entries for payment reversals

---

## Implementation Examples

### Example 1: Package Bill Cancellation

```java
// File: BillPackageController.java
public String cancelPackageBill() {
    // Setup cancellation bill
    String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(
        sessionController.getDepartment(), BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);

    Bill cancellationBill = new CancelledBill();
    cancellationBill.copy(bill);
    cancellationBill.setBillTypeAtomic(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
    cancellationBill.setDeptId(deptId);
    cancellationBill.setInsId(deptId);
    cancellationBill.setCreater(sessionController.getLoggedUser());
    cancellationBill.setDepartment(sessionController.getDepartment());
    cancellationBill.setInstitution(sessionController.getInstitution());
    // ... set negative values ...
    billFacade.create(cancellationBill);

    // Initialize payment details from original bill
    initializeCancellationPaymentFromOriginalPayments();

    // Apply cancellation signs to payment data
    applyCancellationSignToPaymentData();

    // Create and process payments
    List<Payment> payments = paymentService.createPayment(cancellationBill, getPaymentMethodData());
    paymentService.updateBalances(payments);

    // Mark original bill as cancelled
    bill.setCancelled(true);
    bill.setCancelledBill(cancellationBill);
    billFacade.edit(bill);

    return "success";
}
```

### Example 2: Sign Application Method

```java
private void applyCancellationSignToPaymentData() {
    if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
        // Handle multiple payment methods
        if (componentDetails != null) {
            for (ComponentDetail componentDetail : componentDetails) {
                applyCancellationSignToComponent(componentDetail);
            }
        }
    } else {
        // Handle single payment method
        applyCancellationSignToSinglePaymentMethod();
    }
}

private void applyCancellationSignToComponent(ComponentDetail componentDetail) {
    if (componentDetail == null || componentDetail.getTotalValue() == null) {
        return;
    }
    // Apply negative sign (idempotent operation)
    componentDetail.setTotalValue(0 - Math.abs(componentDetail.getTotalValue()));
}

private void applyCancellationSignToSinglePaymentMethod() {
    switch (paymentMethod) {
        case PatientDeposit:
            if (getPaymentMethodData().getPatient_deposit() != null) {
                double value = getPaymentMethodData().getPatient_deposit().getTotalValue();
                getPaymentMethodData().getPatient_deposit().setTotalValue(0 - Math.abs(value));
            }
            break;
        case Staff_Welfare:
            if (getPaymentMethodData().getStaff_welfare() != null) {
                double value = getPaymentMethodData().getStaff_welfare().getTotalValue();
                getPaymentMethodData().getStaff_welfare().setTotalValue(0 - Math.abs(value));
            }
            break;
        // ... other payment methods ...
    }
}
```

---

## UI Guidelines

### Refund-Specific Composite Components

**Problem**: Regular payment components show inappropriate warnings during refunds (e.g., "Insufficient Welfare Balance") because they're designed for deductions, not additions.

**Solution**: Use specialized refund composite components that show positive messaging and balance increases.

#### Staff Welfare Refund Component

**File**: `/resources/ezcomp/paymentMethod/staffWelfareAsRefundPayment.xhtml`

**Features**:
- ✅ **No balance warnings** - Refunds ADD money to welfare accounts
- ✅ **Auto-populated staff member** from original payment
- ✅ **Current balance display** with calculated new balance after refund
- ✅ **Positive refund messaging** - "This amount will be added back"
- ✅ **Professional card layout** with clear transaction information

**Usage**:
```xml
<h:panelGroup rendered="#{billPackageController.paymentMethod eq 'Staff_Welfare'}">
    <pa:staffWelfareAsRefundPayment paymentMethodData="#{billPackageController.paymentMethodData}"/>
</h:panelGroup>
```

#### Staff Credit Refund Component

**File**: `/resources/ezcomp/paymentMethod/staffCreditAsRefundPayment.xhtml`

**Features**:
- ✅ **No balance warnings** - Refunds ADD money to credit accounts
- ✅ **Auto-populated staff member** from original payment
- ✅ **Current balance display** with calculated new balance after refund
- ✅ **Positive refund messaging** - "This amount will be added back"
- ✅ **Professional card layout** with clear transaction information

**Usage**:
```xml
<h:panelGroup rendered="#{billPackageController.paymentMethod eq 'Staff' or billPackageController.paymentMethod eq 'OnCall'}">
    <pa:staffCreditAsRefundPayment paymentMethodData="#{billPackageController.paymentMethodData}"/>
</h:panelGroup>
```

#### Component Features Comparison

| Feature | Regular Component | Refund Component |
|---------|------------------|------------------|
| **Balance Validation** | Shows "Insufficient Balance" warnings | No warnings - refunds add money |
| **Messaging** | Deduction-focused | Addition-focused ("added back") |
| **Balance Display** | Current balance only | Current + New balance after refund |
| **Visual Design** | Standard form | Enhanced card layout with transaction info |
| **Staff Auto-population** | Manual selection | Auto-populated from original payment |

#### Creating New Refund-Specific Components

**When to Create**: When regular payment components show inappropriate warnings or messaging during refunds.

**Implementation Pattern**:

1. **Create Component File**: `/resources/ezcomp/paymentMethod/[paymentMethod]AsRefundPayment.xhtml`

2. **Key Features to Include**:
   ```xml
   <!-- Remove balance validation warnings -->
   <!-- Add positive refund messaging -->
   <div class="alert alert-success">
       This amount will be <strong>added back</strong> to [account type] upon cancellation.
   </div>

   <!-- Show balance calculation: current + refund = new -->
   <span class="text-success">New Balance: #{currentBalance + refundAmount}</span>
   ```

3. **Auto-population Support**: Ensure staff/company details are populated from `paymentMethodData`

4. **Update Cancellation Pages**: Replace regular component with refund component:
   ```xml
   <!-- Before -->
   <pa:regular_payment_component paymentMethodData="#{controller.paymentMethodData}"/>

   <!-- After -->
   <pa:paymentMethodAsRefundPayment paymentMethodData="#{controller.paymentMethodData}"/>
   ```

### Original Payment Display Panel

Add this panel to show users the original payment details:

```xml
<!-- Original Payment Details Panel -->
<p:panel rendered="#{not empty billPackageController.originalBillPayments}"
         class="mb-2" header="Original Payment Details">
    <p:dataTable value="#{billPackageController.originalBillPayments}" var="op"
                 styleClass="table table-sm">
        <p:column headerText="Method">
            <h:outputText value="#{op.paymentMethod}" />
        </p:column>

        <p:column headerText="Amount" styleClass="text-end">
            <h:outputText value="#{op.paidValue}">
                <f:convertNumber pattern="#,##0.00" />
            </h:outputText>
        </p:column>

        <!-- Staff Details Column -->
        <p:column headerText="Staff Member"
                  rendered="#{op.paymentMethod eq 'Staff_Welfare' or op.paymentMethod eq 'Staff' or op.paymentMethod eq 'OnCall'}">
            <h:outputText value="#{op.toStaff.person.name}" rendered="#{op.toStaff ne null}"/>
        </p:column>

        <!-- Credit Company Column -->
        <p:column headerText="Credit Company" rendered="#{op.paymentMethod eq 'Credit'}">
            <h:outputText value="#{op.creditCompany.name}" rendered="#{op.creditCompany ne null}"/>
        </p:column>

        <!-- Bank/Reference Column -->
        <p:column headerText="Bank/Reference"
                  rendered="#{op.paymentMethod eq 'Card' or op.paymentMethod eq 'Slip' or op.paymentMethod eq 'Cheque'}">
            <h:panelGroup>
                <h:outputText value="#{op.bank.name}" rendered="#{op.bank ne null}"/>
                <h:outputText value=" - #{op.referenceNo}" rendered="#{op.referenceNo ne null}"/>
            </h:panelGroup>
        </p:column>

        <!-- Comments Column -->
        <p:column headerText="Comments">
            <h:outputText value="#{op.comments}" rendered="#{op.comments ne null}"/>
        </p:column>
    </p:dataTable>
</p:panel>
```

### Auto-populated Payment Forms

Ensure payment method forms show pre-populated values:

```xml
<!-- Staff Welfare Form with Auto-populated Staff -->
<h:panelGroup layout="block" rendered="#{billPackageController.paymentMethod eq 'Staff_Welfare'}">
    <div class="col-md-6">
        <p:outputLabel for="staffWelfareStaff" value="Staff Member: " />
        <p:autoComplete id="staffWelfareStaff"
                       value="#{billPackageController.paymentMethodData.staff_welfare.toStaff}"
                       completeMethod="#{staffController.completeStaff}"
                       converter="staffConverter"
                       var="stf" itemLabel="#{stf.person.name}"
                       itemValue="#{stf}"
                       forceSelection="true">
            <p:column>#{stf.person.name} - #{stf.speciality.name}</p:column>
        </p:autoComplete>

        <!-- Display current welfare balance -->
        <small class="text-muted d-block"
               rendered="#{billPackageController.paymentMethodData.staff_welfare.toStaff ne null}">
            Current Welfare Balance:
            <h:outputText value="#{billPackageController.paymentMethodData.staff_welfare.toStaff.currentCredit}">
                <f:convertNumber pattern="#,##0.00" />
            </h:outputText>
        </small>
    </div>
</h:panelGroup>
```

---

## Testing Checklist

### Functional Testing

**✅ Payment Detail Preservation**:
- [ ] Staff Welfare: Staff member name auto-populated
- [ ] Credit: Credit company name auto-populated
- [ ] Card: Bank and reference number auto-populated
- [ ] Cheque: Bank and cheque number auto-populated
- [ ] Slip: Bank and slip reference auto-populated
- [ ] Patient Deposit: Patient balance displayed
- [ ] eWallet: Wallet details preserved

**✅ Refund-Specific UI Components**:
- [ ] **Staff Welfare Refunds**: No "Insufficient Balance" warnings shown
- [ ] **Staff Credit Refunds**: No "Insufficient Balance" warnings shown
- [ ] **Balance Calculations**: Shows current balance + refund amount = new balance
- [ ] **Positive Messaging**: Displays "amount will be added back" messaging
- [ ] **Card Layout**: Professional card design renders correctly
- [ ] **Auto-populated Data**: Staff member details appear automatically
- [ ] **Comments Section**: Refund comments field works correctly

**✅ Balance Updates**:
- [ ] Patient deposit balance increased when canceling PatientDeposit payment
- [ ] Staff welfare balance increased when canceling Staff_Welfare payment
- [ ] Staff credit balance increased when canceling Staff payment
- [ ] Credit company balance decreased when canceling Credit payment

**✅ Multiple Payment Methods**:
- [ ] All original payment methods preserved in component list
- [ ] Each component has correct details auto-populated
- [ ] Total cancellation amount matches original bill total
- [ ] All balance updates work correctly for each component

**✅ Audit Trail**:
- [ ] Payment records created with negative values
- [ ] PatientDepositHistory entries created correctly
- [ ] Bill marked as cancelled with reference to cancellation bill
- [ ] Proper user, department, institution recorded in cancellation

### Edge Case Testing

**✅ Data Validation**:
- [ ] Handle missing original payment details gracefully
- [ ] Validate payment method change restrictions if any
- [ ] Handle cancelled bills that can't be cancelled again
- [ ] Proper error messages for invalid cancellation attempts

### Performance Testing

**✅ Database Operations**:
- [ ] Payment creation doesn't cause foreign key violations
- [ ] Balance updates complete in reasonable time
- [ ] No duplicate payment history entries created
- [ ] Proper transaction rollback on errors

---

## Common Pitfalls

### ❌ Pitfall 1: Manual Payment Creation

```java
// ❌ WRONG - Creates orphaned payments
Payment refundPayment = new Payment();
refundPayment.setPaidValue(-bill.getNetTotal());
paymentFacade.create(refundPayment); // No cashbook, drawer, or balance updates!

// ✅ CORRECT - Use PaymentService
List<Payment> payments = paymentService.createPayment(cancellationBill, paymentMethodData);
paymentService.updateBalances(payments);
```

### ❌ Pitfall 2: Forgetting Payment Detail Population

```java
// ❌ WRONG - User has to manually re-enter staff member
paymentMethod = PaymentMethod.Staff_Welfare;
// Staff field is empty!

// ✅ CORRECT - Auto-populate from original
paymentMethod = originalPayment.getPaymentMethod();
getPaymentMethodData().getStaff_welfare().setToStaff(originalPayment.getToStaff());
```

### ❌ Pitfall 3: Wrong BillTypeAtomic

```java
// ❌ WRONG - Uses generic bill type
cancellationBill.setBillTypeAtomic(BillTypeAtomic.OPD_BILL);
// PatientDepositService won't know this is a cancellation!

// ✅ CORRECT - Use cancellation-specific type
cancellationBill.setBillTypeAtomic(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
```

### ❌ Pitfall 4: Not Applying Negative Signs

```java
// ❌ WRONG - Positive values create charges instead of refunds
getPaymentMethodData().getPatient_deposit().setTotalValue(bill.getNetTotal());

// ✅ CORRECT - Negative values for cancellation
getPaymentMethodData().getPatient_deposit().setTotalValue(0 - Math.abs(bill.getNetTotal()));
```

### ❌ Pitfall 5: Inconsistent Context Information

```java
// ❌ WRONG - Missing context in cancellation bill
Bill cancellationBill = new CancelledBill();
// Missing creater, department, institution!

// ✅ CORRECT - Proper context for audit trail
cancellationBill.setCreater(sessionController.getLoggedUser());
cancellationBill.setDepartment(sessionController.getDepartment());
cancellationBill.setInstitution(sessionController.getInstitution());
```

---

## Summary

This document establishes the standard pattern for implementing payment cancellations in HMIS. The key requirements are:

1. **PaymentService Integration**: Always use `paymentService.createPayment()` and `paymentService.updateBalances()`
2. **Payment Detail Preservation**: Auto-populate all original payment details for user convenience
3. **Proper BillTypeAtomic**: Use cancellation-specific types for correct balance updates
4. **Negative Payment Values**: Apply proper signs for refund processing
5. **Complete Audit Trail**: Ensure proper user, department, institution context
6. **Refund-Specific UI Components**: Use specialized components that show positive messaging and eliminate inappropriate warnings

## Implementation Summary

This document and implementation addresses a complete payment cancellation workflow that was implemented for OPD Package Bill Cancellations:

### ✅ **Complete Solution Delivered**

1. **Root Cause Fixed**: Staff member details now save correctly during bill creation
   - Added `transferStaffDataToPaymentMethodData()` method in `settleBill()`
   - Transfers `toStaff` property to `paymentMethodData` before payment creation

2. **Auto-Population Implemented**: Original payment details auto-populate during cancellation
   - Added `initializeCancellationPaymentFromOriginalPayments()` method
   - Populates all payment method details (staff, credit company, bank details, etc.)
   - Handles both single and multiple payment method scenarios

3. **Improved User Experience**: Created refund-specific UI components
   - **New Components**: `staffWelfareAsRefundPayment.xhtml`, `staffCreditAsRefundPayment.xhtml`
   - **Features**: No balance warnings, positive messaging, balance calculations
   - **Visual Design**: Professional card layout with transaction information

4. **Enhanced UI**: Original payment details display panel
   - Shows method, amount, staff member, bank details, etc.
   - Helps users understand what they're canceling

5. **Documentation**: Comprehensive implementation guidelines
   - Complete patterns for future payment cancellation implementations
   - Testing checklists and troubleshooting guides

### 🎯 **User Experience Improvements**

**Before**:
- Staff welfare cancellations showed "Select Staff" (empty dropdown)
- "Insufficient Welfare Balance" warnings during refunds
- Manual data entry required

**After**:
- Staff member auto-populated ("Buddhika Illeperuma")
- Positive refund messaging ("amount will be added back")
- Current balance + refund calculation displayed
- Professional card layout with transaction details

### 📁 **Files Modified/Created**

**Backend**:
- `BillPackageController.java` - Payment detail preservation and staff data transfer

**Frontend**:
- `opd_package_bill_cancel.xhtml` - Original payment display and refund components
- `staffWelfareAsRefundPayment.xhtml` - New refund-specific component
- `staffCreditAsRefundPayment.xhtml` - New refund-specific component

**Documentation**:
- `payment-cancellation-guidelines.md` - Complete implementation guide

Following this pattern ensures consistent, reliable, and user-friendly payment cancellation across all HMIS modules.

---

**Related Documentation**:
- `/home/buddhika/development/rh/developer_docs/pr_description.md` - PaymentService integration examples
- `/home/buddhika/development/rh/src/main/java/com/divudi/service/PaymentService.java` - PaymentService implementation
- `/home/buddhika/development/rh/src/main/java/com/divudi/service/PatientDepositService.java` - Patient deposit balance logic

**Document History**:

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-20 | HMIS Development Team | Initial version based on package bill cancellation implementation |