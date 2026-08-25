# Credit Company Balance Management - Developer Guide

## Overview

This guide covers the technical implementation patterns for managing credit company bill balances in the HMIS system. Credit bills require special handling to maintain accurate due amounts for credit company settlement reporting.

## 🏥 Healthcare Domain Context

### Credit Payment Workflow
1. **Patient visits hospital** → Services provided → Bill created
2. **Payment method**: Credit (bill to be settled later by credit company)
3. **Bill balance**: Represents amount owed by credit company to hospital
4. **Settlement**: Credit company pays hospital periodically based on due amounts

### Why Balance Accuracy Matters
- **Financial Reconciliation**: Incorrect balances lead to over/under billing credit companies
- **Regulatory Compliance**: Health insurance requires accurate settlement reporting
- **Cash Flow Management**: Hospitals depend on credit company payments for operations

## 🏗️ Technical Architecture

### Bill Entity Structure

```java
@Entity
public class Bill {
    private double balance;        // Outstanding amount owed (due amount)
    private double paidAmount;     // Amount already paid by credit company
    private double refundAmount;   // Total amount refunded to date
    private double netTotal;       // Original bill total
    private double vat;            // VAT amount

    // Batch bill relationships
    private Bill backwardReferenceBill;    // Points to batch bill
    private List<Bill> forwardReferenceBills; // Individual bills in batch
}
```

### Hierarchical Bill Structure

```
Batch Bill (Credit Payment Method)
├── balance: Outstanding due amount
├── paidAmount: Settled by credit company
├── refundAmount: Total refunds processed
└── Individual Bills (by Department)
    ├── Individual Bill A (Cardiology): $200
    ├── Individual Bill B (Radiology): $300
    └── Individual Bill C (Laboratory): $100
```

**Critical Relationship:**
- **Payment records** → Link to Batch Bill only
- **Individual bills** → Hold line items but no payments
- **Balance tracking** → Maintained at Batch Bill level

## 🔧 Core Patterns

### 1. Balance Update Pattern

**Standard Implementation:**
```java
private void updateBatchBillFinancialFields(Bill individualBill, Bill operationBill) {
    // Get batch bill that holds financial tracking
    Bill batchBill = individualBill.getBackwardReferenceBill();

    if (batchBill == null || batchBill.getPaymentMethod() != PaymentMethod.Credit) {
        return; // Only update Credit bills
    }

    // Refresh from database for optimistic locking
    batchBill = billFacade.find(batchBill.getId());

    double operationAmount = Math.abs(operationBill.getNetTotal());

    // Three-field update pattern
    batchBill.setRefundAmount(batchBill.getRefundAmount() + operationAmount);

    if (batchBill.getPaidAmount() > 0) {
        batchBill.setPaidAmount(Math.max(0d, batchBill.getPaidAmount() - operationAmount));
    }

    if (batchBill.getBalance() > 0) {
        batchBill.setBalance(Math.max(0d, batchBill.getBalance() - operationAmount));
    }

    billFacade.edit(batchBill);
}
```

### 2. Safety Validations

**Credit Company Settlement Protection:**
```java
// Always check if credit company has already settled before allowing modifications
Bill batchBill = individualBill.getBackwardReferenceBill();
if (batchBill != null && batchBill.getPaymentMethod() == PaymentMethod.Credit) {
    List<BillItem> settledItems = billService.checkCreditBillPaymentReciveFromCreditCompany(batchBill);

    if (settledItems != null && !settledItems.isEmpty()) {
        throw new IllegalStateException("Cannot modify: Bill already settled by credit company");
    }
}
```

**Data Integrity Checks:**
```java
// Prevent negative balances
batchBill.setBalance(Math.max(0d, batchBill.getBalance() - refundAmount));

// Validate refund doesn't exceed original amount
if (refundAmount > batchBill.getNetTotal()) {
    throw new IllegalStateException("Refund amount cannot exceed original bill total");
}
```

### 3. Concurrency Protection

```java
// Always refresh entity before update to trigger optimistic locking
try {
    batchBill = billFacade.find(batchBill.getId());
    billFacade.edit(batchBill);
} catch (OptimisticLockException ole) {
    JsfUtil.addErrorMessage("Concurrent modification detected. Please retry.");
    throw new RuntimeException("Concurrent batch bill modification", ole);
}
```

## 📋 Implementation Scenarios

### Scenario 1: Individual Bill Cancellation

**File:** `BillSearch.java`
**Method:** `cancelOpdBill()`
**Trigger:** User cancels individual bill from batch

```java
// After creating cancellation bill
CancelledBill cancellationBill = createOpdCancelBill(bill);
billController.save(cancellationBill);

// Update batch bill balance
updateBatchBillFinancialFieldsForIndividualCancellation(bill, cancellationBill);
```

**Result:** Batch balance reduces by cancelled individual bill amount

### Scenario 2: Batch Bill Cancellation

**Files:** `BillController.java` (multiple methods)
**Trigger:** User cancels entire batch bill

```java
// Before persisting batch bill changes
if (batchBill.getPaymentMethod() == PaymentMethod.Credit && batchBill.getBalance() > 0) {
    batchBill.setBalance(0); // Full cancellation resets balance to 0
}
billFacade.edit(batchBill);
```

**Result:** Batch balance becomes 0 (no amount owed)

### Scenario 3: Partial Item Returns

**File:** `BillReturnController.java`
**Method:** `settleOpdReturnBill()`
**Trigger:** User returns specific items from individual bill

```java
// After creating return bill
billController.save(newlyReturnedBill);

// Update batch bill balance for returned items
updateBatchBillFinancialFieldsForIndividualReturn(originalBillToReturn, newlyReturnedBill);
```

**Result:** Batch balance reduces by returned item values

### Scenario 4: Settlement Page Calculation

**File:** `CashRecieveBillController.java`
**Method:** `getReferenceBallance()`
**Trigger:** Credit company settlement process

```java
// Use direct balance field for Credit bills (post-fix)
if (referenceBill.getPaymentMethod() == PaymentMethod.Credit) {
    return Math.abs(referenceBill.getBalance()); // Updated field
}

// Use calculated approach for other payment methods
return neTotal - (paidAmount + refundAmount);
```

**Result:** Settlement page shows accurate due amounts

## ⚠️ Critical Implementation Guidelines

### 1. Credit-Only Logic
```java
// ALWAYS check payment method before balance updates
if (batchBill.getPaymentMethod() != PaymentMethod.Credit) {
    return; // Only Credit bills need balance tracking
}
```

### 2. Database Refresh Pattern
```java
// ALWAYS refresh before update for concurrency protection
batchBill = billFacade.find(batchBill.getId());
```

### 3. Three-Field Update Pattern
```java
// ALWAYS update all three fields together for consistency
batchBill.setRefundAmount(currentRefund + operationAmount);    // Track total refunds
batchBill.setPaidAmount(Math.max(0d, currentPaid - operationAmount)); // Reduce paid
batchBill.setBalance(Math.max(0d, currentBalance - operationAmount));  // Reduce due
```

### 4. Exception Handling
```java
try {
    billFacade.edit(batchBill);
} catch (Exception e) {
    // Log error but don't re-throw to prevent operation failure
    System.err.println("Failed to update batch bill balance: " + e.getMessage());
    JsfUtil.addErrorMessage("Error updating credit balance: " + e.getMessage());
}
```

## 🚫 Common Anti-Patterns

### ❌ Wrong: Updating Individual Bill Balance
```java
// NEVER update individual bill balance - they don't hold payments
individualBill.setBalance(newAmount); // Wrong!
```

### ❌ Wrong: Skipping Payment Method Check
```java
// NEVER update balance without checking payment method
batchBill.setBalance(newAmount); // Missing PaymentMethod.Credit check
```

### ❌ Wrong: Using Calculated Values for Credit Bills
```java
// NEVER use CreditBean calculation for Credit bills after implementing direct balance
double balance = getCreditBean().getRefundAmount(bill); // Outdated for Credit bills
```

### ❌ Wrong: Modifying Settled Bills
```java
// NEVER modify bills already settled by credit company
// Always check: billService.checkCreditBillPaymentReciveFromCreditCompany()
```

## 🧪 Testing Guidelines

### Unit Test Coverage

```java
@Test
public void testIndividualBillCancellation_ReducesBatchBalance() {
    // Setup: Batch bill $500 with 2 individual bills ($300 + $200)
    // Action: Cancel $200 individual bill
    // Verify: Batch balance = $300
}

@Test
public void testPartialReturn_ReducesBatchBalanceProportionally() {
    // Setup: Individual bill with 3 items ($100 + $150 + $50)
    // Action: Return 1 item worth $100
    // Verify: Batch balance reduces by $100
}

@Test
public void testFullBatchCancellation_ResetsBalanceToZero() {
    // Setup: Credit batch bill with $1000 balance
    // Action: Cancel entire batch
    // Verify: Balance = $0
}

@Test
public void testConcurrentModification_HandlesOptimisticLocking() {
    // Setup: Two users modify same batch simultaneously
    // Verify: Optimistic locking prevents lost updates
}
```

### Integration Testing

**Settlement Report Validation:**
1. Create credit bills with various scenarios
2. Process cancellations/returns
3. Generate credit company settlement report
4. Verify due amounts match expected values

**End-to-End Testing:**
1. OPD visit → Credit bill → Item return → Settlement → Payment
2. Verify balance accuracy throughout entire lifecycle

## 📊 Monitoring and Troubleshooting

### Balance Reconciliation Query

```sql
-- Check for balance inconsistencies
SELECT
    b.insId as batch_bill,
    b.netTotal + b.vat as original_amount,
    b.balance as current_balance,
    b.refundAmount as total_refunds,
    b.paidAmount as total_paid,
    (b.netTotal + b.vat - b.refundAmount - b.paidAmount) as calculated_balance
FROM bill b
WHERE b.paymentMethod = 'Credit'
  AND b.balance != (b.netTotal + b.vat - b.refundAmount - b.paidAmount)
  AND b.cancelled = false;
```

### Common Issues

**Issue: Settlement page shows wrong amounts**
- **Cause:** Using CreditBean calculation instead of direct balance field
- **Fix:** Update `getReferenceBallance()` to use `bill.getBalance()` for Credit bills

**Issue: Balance not reducing after cancellation**
- **Cause:** Missing batch bill update logic
- **Fix:** Add `updateBatchBillFinancialFields()` call after operation

**Issue: Negative balances**
- **Cause:** Missing `Math.max(0d, ...)` protection
- **Fix:** Add boundary checks in update logic

## 🔄 Migration Considerations

### Existing Bills
```java
// One-time migration script for existing bills with incorrect balances
UPDATE bill b
SET balance = (
    SELECT (b2.netTotal + b2.vat) -
           COALESCE((SELECT SUM(r.netTotal + r.vat) FROM bill r WHERE r.billedBill = b2.id), 0) -
           COALESCE((SELECT SUM(p.paidValue) FROM payment p WHERE p.bill_id = b2.id), 0)
    FROM bill b2
    WHERE b2.id = b.id
)
WHERE b.paymentMethod = 'Credit'
  AND b.billTypeAtomic IN ('OPD_BATCH_BILL_WITH_PAYMENT', 'PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT');
```

### Backward Compatibility
- Preserve existing behavior for non-Credit payment methods
- Gradual rollout with feature flags if needed
- Comprehensive testing in QA environment

## 🚀 Future Enhancements

### Audit Trail Integration
```java
// Enhanced logging for compliance
auditEventApplicationController.logAuditEvent(
    AuditEventType.CREDIT_BALANCE_ADJUSTMENT,
    "Credit balance updated: " + oldBalance + " → " + newBalance,
    batchBill
);
```

### Real-time Reporting
- WebSocket notifications for balance changes
- Dashboard integration for credit company monitoring
- Automated reconciliation reports

### API Endpoints
```java
@RestController
public class CreditBalanceController {
    @GetMapping("/api/credit/balance/{billId}")
    public CreditBalanceDto getCreditBalance(@PathVariable Long billId) {
        // Return real-time balance information
    }
}
```

## 📚 Related Documentation

- [Bill Number Generation Strategy](bill-number-generation-strategy-guide.md)
- [Payment Cancellation Guidelines](payment-cancellation-guidelines.md)
- [Database Migration Guide](../database/migration-development-guide.md)
- [JSF Ajax Update Guidelines](../jsf/ajax-update-guidelines.md)

## 🔗 Code References

**Key Files:**
- `src/main/java/com/divudi/bean/common/BillSearch.java:7533-7639`
- `src/main/java/com/divudi/bean/common/CashRecieveBillController.java:326-384`
- `src/main/java/com/divudi/bean/common/BillController.java:2474-2482`
- `src/main/java/com/divudi/bean/common/BillPackageController.java:3282-3365`

**Entity References:**
- `src/main/java/com/divudi/core/entity/Bill.java`
- `src/main/java/com/divudi/core/entity/Payment.java`
- `src/main/java/com/divudi/core/entity/BillItem.java`

---

## ⚡ Quick Reference

### Essential Pattern Checklist
- [ ] Check `PaymentMethod.Credit` before balance updates
- [ ] Refresh entity from database before update
- [ ] Update all three fields: `balance`, `refundAmount`, `paidAmount`
- [ ] Use `Math.max(0d, ...)` to prevent negative values
- [ ] Handle `OptimisticLockException` gracefully
- [ ] Add audit logging for compliance
- [ ] Test with realistic hospital data volumes

### Integration Points
- **Settlement System**: `CashRecieveBillController.getReferenceBallance()`
- **OPD Cancellation**: `BillSearch.cancelOpdBill()`
- **OPD Returns**: `BillReturnController.settleOpdReturnBill()`
- **Package Bills**: `BillPackageController.cancelPackageBill()`

*Last Updated: December 2024*
*Related GitHub Issue: [#17138](https://github.com/hmislk/hmis/issues/17138)*