# Payment Cancellation Implementation Guidelines

**Version**: 2.0
**Author**: HMIS Development Team
**Scope**: All bill cancellation features (OPD, Package, Pharmacy, etc.)

---

## Core Implementation Pattern

### Required Dependencies

```java
@EJB
private PaymentService paymentService;

@EJB
private BillService billService;

private List<Payment> originalBillPayments;
private PaymentMethodData paymentMethodData;
```

### Standard Cancellation Method

```java
public String cancelBill() {
    // 1. Validation
    if (errorsPresentOnCancellation()) {
        return "";
    }

    // 2. Create cancellation bill
    Bill cancellationBill = createCancellationBill();
    billFacade.create(cancellationBill);

    // 3. Load and populate original payment details
    originalBillPayments = billService.fetchBillPayments(getOriginalBill());
    initializeCancellationPaymentFromOriginalPayments();

    // 4. Apply negative signs to payment data
    applyCancellationSignToPaymentData();

    // 5. Create payments and update balances
    List<Payment> payments = paymentService.createPayment(cancellationBill, getPaymentMethodData());
    paymentService.updateBalances(payments);

    // 6. Mark original bill as cancelled
    originalBill.setCancelled(true);
    originalBill.setCancelledBill(cancellationBill);
    billFacade.edit(originalBill);

    return "success";
}
```

### BillTypeAtomic Configuration

```java
private Bill createCancellationBill() {
    Bill cancellationBill = new CancelledBill();

    // Critical: Use cancellation-specific atomic types for correct balance updates
    switch (originalBill.getBillTypeAtomic()) {
        case PACKAGE_OPD_BILL_WITH_PAYMENT:
            cancellationBill.setBillTypeAtomic(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
            break;
        case PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT:
            cancellationBill.setBillTypeAtomic(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
            break;
        case OPD_BILL_WITH_PAYMENT:
            cancellationBill.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_CANCELLATION);
            break;
    }

    // Set context for audit trail
    cancellationBill.setCreater(sessionController.getLoggedUser());
    cancellationBill.setDepartment(sessionController.getDepartment());
    cancellationBill.setInstitution(sessionController.getInstitution());

    return cancellationBill;
}
```

---

## Payment Detail Preservation

### Original Payment Loading

```java
private void loadOriginalPaymentDetails() {
    originalBillPayments = billService.fetchBillPayments(getBillToCancel());

    // For package bills: check if payments are on batch bill instead
    if (originalBillPayments == null || originalBillPayments.isEmpty()) {
        Bill batchBill = getBillToCancel().getForwardReferenceBill();
        if (batchBill != null) {
            originalBillPayments = billService.fetchBillPayments(batchBill);
        }
    }
}
```

### Payment Detail Population

```java
private void initializeCancellationPaymentFromOriginalPayments() {
    if (originalBillPayments == null || originalBillPayments.isEmpty()) {
        return;
    }

    if (paymentMethodData == null) {
        paymentMethodData = new PaymentMethodData();
    }

    if (originalBillPayments.size() == 1) {
        populateSinglePaymentMethod(originalBillPayments.get(0));
    } else {
        populateMultiplePaymentMethods(originalBillPayments);
    }
}

private void populateSinglePaymentMethod(Payment originalPayment) {
    switch (originalPayment.getPaymentMethod()) {
        case Staff_Welfare:
            paymentMethodData.getStaffWelfare().setToStaff(originalPayment.getToStaff());
            paymentMethodData.getStaffWelfare().setTotalValue(Math.abs(bill.getNetTotal()));
            paymentMethodData.getStaffWelfare().setComment(originalPayment.getComments());
            break;

        case Staff:
        case OnCall:
            paymentMethodData.getStaffCredit().setToStaff(originalPayment.getToStaff());
            paymentMethodData.getStaffCredit().setTotalValue(Math.abs(bill.getNetTotal()));
            paymentMethodData.getStaffCredit().setComment(originalPayment.getComments());
            break;

        case Credit:
            paymentMethodData.getCredit().setInstitution(originalPayment.getCreditCompany());
            paymentMethodData.getCredit().setReferenceNo(originalPayment.getReferenceNo());
            paymentMethodData.getCredit().setTotalValue(Math.abs(bill.getNetTotal()));
            break;

        case Card:
            paymentMethodData.getCreditCard().setInstitution(originalPayment.getBank());
            paymentMethodData.getCreditCard().setNo(originalPayment.getCreditCardRefNo());
            paymentMethodData.getCreditCard().setTotalValue(Math.abs(bill.getNetTotal()));
            break;

        case PatientDeposit:
            paymentMethodData.getPatient_deposit().setPatient(bill.getPatient());
            paymentMethodData.getPatient_deposit().setTotalValue(Math.abs(bill.getNetTotal()));

            // Load patient deposit account
            PatientDeposit pd = patientDepositController.getDepositOfThePatient(
                bill.getPatient(), sessionController.getDepartment());
            if (pd != null) {
                paymentMethodData.getPatient_deposit().setPatientDepost(pd);
            }
            break;
    }
}
```

### Payment Sign Application

```java
private void applyCancellationSignToPaymentData() {
    if (paymentMethod == PaymentMethod.MultiplePaymentMethods) {
        for (ComponentDetail cd : componentDetails) {
            cd.setTotalValue(0 - Math.abs(cd.getTotalValue()));
        }
    } else {
        switch (paymentMethod) {
            case Staff_Welfare:
                double welfareValue = paymentMethodData.getStaffWelfare().getTotalValue();
                paymentMethodData.getStaffWelfare().setTotalValue(0 - Math.abs(welfareValue));
                break;

            case Staff:
            case OnCall:
                double creditValue = paymentMethodData.getStaffCredit().getTotalValue();
                paymentMethodData.getStaffCredit().setTotalValue(0 - Math.abs(creditValue));
                break;

            case PatientDeposit:
                double depositValue = paymentMethodData.getPatient_deposit().getTotalValue();
                paymentMethodData.getPatient_deposit().setTotalValue(0 - Math.abs(depositValue));
                break;
        }
    }
}
```

---

## Refund-Specific UI Components

### Component File Structure

```
/resources/paymentMethod/
├── staffWelfareAsRefundPayment.xhtml
├── staffCreditAsRefundPayment.xhtml
└── patientDepositAsRefundPayment.xhtml
```

### Component Usage Pattern

```xml
<!-- Staff Welfare Refund -->
<h:panelGroup rendered="#{controller.paymentMethod eq 'Staff_Welfare'}">
    <pa:staffWelfareAsRefundPayment paymentMethodData="#{controller.paymentMethodData}"/>
</h:panelGroup>

<!-- Staff Credit Refund -->
<h:panelGroup rendered="#{controller.paymentMethod eq 'Staff' or controller.paymentMethod eq 'OnCall'}">
    <pa:staffCreditAsRefundPayment paymentMethodData="#{controller.paymentMethodData}"/>
</h:panelGroup>

<!-- Patient Deposit Refund -->
<h:panelGroup rendered="#{controller.paymentMethod eq 'PatientDeposit'}">
    <pa:patientDepositAsRefundPayment paymentMethodData="#{controller.paymentMethodData}"/>
</h:panelGroup>
```

### Component Interface Structure

```xml
<!-- Standard refund component interface -->
<cc:interface>
    <cc:attribute name="paymentMethodData" required="true" />
</cc:interface>

<cc:implementation>
    <!-- Auto-complete field for staff/entity selection -->
    <p:autoComplete value="#{cc.attrs.paymentMethodData.staffWelfare.toStaff}"
                   completeMethod="#{staffController.completeStaff}" />

    <!-- Read-only refund amount display -->
    <p:inputNumber value="#{cc.attrs.paymentMethodData.staffWelfare.totalValue}"
                  readonly="true" />

    <!-- Balance calculation display -->
    <h:outputText value="#{currentBalance + refundAmount}">
        <f:convertNumber pattern="#,##0.00" />
    </h:outputText>

    <!-- Positive refund messaging -->
    <div class="alert alert-success">
        This amount will be <strong>added back</strong> to the account upon cancellation.
    </div>
</cc:implementation>
```

### Original Payment Display Panel

```xml
<p:panel rendered="#{not empty controller.originalBillPayments}"
         header="Original Payment Details">
    <p:dataTable value="#{controller.originalBillPayments}" var="op">
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
    </p:dataTable>
</p:panel>
```

---

## Account Balance Updates

### PaymentService Integration

```java
// PaymentService automatically handles all balance updates
List<Payment> payments = paymentService.createPayment(cancellationBill, paymentMethodData);
paymentService.updateBalances(payments);
```

### Balance Update Flow by Payment Method

| Payment Method | Service Used | Update Type | Balance Direction |
|---------------|--------------|-------------|------------------|
| `PatientDeposit` | `PatientDepositService.updateBalance()` | Add to deposit | Positive |
| `Staff_Welfare` | `StaffService.updateStaffWelfare()` | Add to welfare | Positive |
| `Staff` / `OnCall` | `StaffService.updateStaffCredit()` | Add to credit | Positive |
| `Credit` | `InstitutionService.updateCredit()` | Reduce company balance | Negative |

### BillTypeAtomic Impact on Balance Updates

```java
// PatientDepositService.updateBalance() logic
switch (payment.getBill().getBillTypeAtomic()) {
    case PACKAGE_OPD_BILL_CANCELLATION:
    case OPD_BILL_CANCELLATION:
        handleInPayment(payment, patientDeposit);  // Adds money
        break;
    case PACKAGE_OPD_BILL_WITH_PAYMENT:
    case OPD_BILL_WITH_PAYMENT:
        handleOutPayment(payment, patientDeposit); // Deducts money
        break;
}
```

---

## Bill Creation Payment Transfer

### Problem Pattern
When original bills use simple form fields instead of `paymentMethodData`, staff/company details may not transfer to Payment records.

### Solution Pattern
Add transfer method in bill settlement process:

```java
public void settleBill() {
    // ... bill creation logic ...

    // Transfer form data to paymentMethodData before payment creation
    transferStaffDataToPaymentMethodData();

    List<Payment> payments = paymentService.createPayment(bill, paymentMethodData);
    paymentService.updateBalances(payments);
}

private void transferStaffDataToPaymentMethodData() {
    if (paymentMethodData == null) {
        paymentMethodData = new PaymentMethodData();
    }

    if (toStaff != null) {
        switch (paymentMethod) {
            case Staff_Welfare:
                paymentMethodData.getStaffWelfare().setToStaff(toStaff);
                paymentMethodData.getStaffWelfare().setTotalValue(bill.getNetTotal());
                break;
            case Staff:
            case OnCall:
                paymentMethodData.getStaffCredit().setToStaff(toStaff);
                paymentMethodData.getStaffCredit().setTotalValue(bill.getNetTotal());
                break;
        }
    }

    if (paymentMethod == PaymentMethod.Credit && creditCompany != null) {
        paymentMethodData.getCredit().setInstitution(creditCompany);
        paymentMethodData.getCredit().setTotalValue(bill.getNetTotal());
    }
}
```

---

## Testing Patterns

### Payment Detail Preservation Verification

```java
// Test that original payment details are retrieved correctly
@Test
public void testOriginalPaymentRetrieval() {
    // Create bill with Staff_Welfare payment
    Payment originalPayment = createPaymentWithStaff(staff, PaymentMethod.Staff_Welfare, 1000.0);

    // Navigate to cancellation
    controller.navigateToCancelBill();

    // Verify staff member is auto-populated
    assertEquals(staff, controller.getPaymentMethodData().getStaffWelfare().getToStaff());
    assertEquals(1000.0, controller.getPaymentMethodData().getStaffWelfare().getTotalValue(), 0.01);
}
```

### Balance Update Verification

```java
// Test that balances are updated correctly during cancellation
@Test
public void testStaffWelfareBalanceUpdate() {
    double initialBalance = staff.getCurrentWelfare(); // TODO: Wrong, there is no attribute like this 
    double billAmount = 500.0;

    // Cancel bill with Staff_Welfare payment
    controller.cancelBill();

    // Verify balance increased by bill amount
    assertEquals(initialBalance + billAmount, staff.getCurrentWelfare(), 0.01);
}
```

---

## Common Implementation Patterns

### Error Handling Pattern

```java
private boolean errorsPresentOnCancellation() {
    if (bill == null) {
        JsfUtil.addErrorMessage("No Bill selected to cancel");
        return true;
    }

    if (bill.isCancelled()) {
        JsfUtil.addErrorMessage("Bill is already cancelled");
        return true;
    }

    if (bill.isRefunded()) {
        JsfUtil.addErrorMessage("Bill is already refunded");
        return true;
    }

    return false;
}
```

### Navigation Pattern

```java
public String navigateToCancelBill() {
    loadOriginalPaymentDetails();
    initializeCancellationPaymentFromOriginalPayments();

    // Set default payment method
    if (originalBillPayments != null && !originalBillPayments.isEmpty()) {
        paymentMethod = originalBillPayments.get(0).getPaymentMethod();
    }

    return "/path/to/cancellation/page?faces-redirect=true";
}
```

### Multiple Payment Method Handling

```java
private void populateMultiplePaymentMethods(List<Payment> payments) {
    paymentMethod = PaymentMethod.MultiplePaymentMethods;
    componentDetails = new ArrayList<>();

    for (Payment payment : payments) {
        ComponentDetail cd = new ComponentDetail();
        cd.setPaymentMethod(payment.getPaymentMethod());
        cd.setTotalValue(Math.abs(payment.getPaidValue()));

        // Copy specific details based on payment method
        switch (payment.getPaymentMethod()) {
            case Staff_Welfare:
                cd.setToStaff(payment.getToStaff());
                break;
            case Credit:
                cd.setCreditCompany(payment.getCreditCompany());
                break;
        }

        componentDetails.add(cd);
    }
}
```

---

## Related Technical References

- **PaymentService**: `com.divudi.service.PaymentService`
- **PatientDepositService**: `com.divudi.service.PatientDepositService`
- **BillService**: `com.divudi.service.BillService`
- **PaymentMethodData**: `com.divudi.core.data.dataStructure.PaymentMethodData`
- **ComponentDetail**: `com.divudi.core.data.dataStructure.ComponentDetail`