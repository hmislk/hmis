# Payment Service Side Effect Control Design

## Executive Summary

This document provides a comprehensive design for adding granular control over side effects in PaymentService.java while maintaining 100% backward compatibility. The design introduces overloaded methods with Boolean parameters to control drawer updates, cashbook writes, patient deposit updates, credit company updates, and staff credit/welfare updates.

**Current State:**
- 152 calls to `createPayment()` across 59 controllers
- 30 calls to `updateBalances()` across 20 controllers
- 136 direct `new Payment()` instantiations across 57 files
- Side effects split between two methods with no granular control
- No transaction management for atomicity

**Target State:**
- Granular Boolean control over each side effect
- 100% backward compatibility with existing calls
- Prevention of duplicate updates when methods are chained
- Clear migration path for controllers
- Optional transaction management support

---

## Table of Contents

1. [Context and Problem Statement](#context-and-problem-statement)
2. [Architecture Overview](#architecture-overview)
3. [Detailed Design](#detailed-design)
4. [Method Signatures](#method-signatures)
5. [Implementation Approach](#implementation-approach)
6. [Backward Compatibility Guarantees](#backward-compatibility-guarantees)
7. [Migration Strategy](#migration-strategy)
8. [Testing Requirements](#testing-requirements)
9. [Risk Analysis](#risk-analysis)
10. [Implementation Phases](#implementation-phases)
11. [Transaction Management](#transaction-management)
12. [Edge Cases and Special Scenarios](#edge-cases-and-special-scenarios)

---

## Context and Problem Statement

### Current Architecture

The `PaymentService` class has two main public methods:

1. **`createPayment(Bill bill, PaymentMethodData paymentMethodData)`**
   - Creates Payment entities
   - Updates drawer
   - Writes to cashbook
   - Does NOT update: patient deposits, credit company balances, staff credit/welfare

2. **`updateBalances(List<Payment> payments)`**
   - Updates patient deposits (PaymentMethod.PatientDeposit)
   - Updates credit company balances (PaymentMethod.Credit)
   - Updates staff credit (PaymentMethod.Staff)
   - Updates staff welfare (PaymentMethod.Staff_Welfare)
   - Does NOT update: drawer, cashbook

### Problems with Current Design

1. **No Granular Control**: Cannot selectively disable specific side effects
2. **Controller Coupling**: Controllers must know which side effects each method performs
3. **Duplicate Updates Risk**: When both methods are called, no protection against double-updates
4. **Bypass Pattern**: 136 direct `new Payment()` calls bypass service layer entirely
5. **No Transaction Boundaries**: Side effects span multiple database operations without atomicity
6. **Testing Challenges**: Cannot easily test payment creation without side effects

### Usage Patterns Identified

From code analysis:

**Pattern 1: Both Methods Called (Most Common)**
```java
List<Payment> payments = paymentService.createPayment(bill, paymentMethodData);
paymentService.updateBalances(payments);
```
Found in: PharmacyRefundForItemReturnsController, SaleReturnController, OpdBillController, etc.

**Pattern 2: Only createPayment Called**
```java
List<Payment> ps = paymentService.createPayment(getBill(), getPaymentMethodData());
```
Found in: PharmacyPurchaseController, SupplierPaymentController, etc.

**Pattern 3: Direct Payment Creation (Legacy)**
```java
Payment payment = new Payment();
// set properties
paymentFacade.create(payment);
// manual side effects
```
Found in: 57 files with 136 occurrences

**Pattern 4: Cancellation Payments**
```java
List<Payment> newPayments = paymentService.createPaymentsForCancelling(cancellationBill);
```
Special handling with inverted values

---

## Architecture Overview

### Design Principles

1. **Backward Compatibility First**: All existing method calls must work identically
2. **Explicit Over Implicit**: Boolean flags make intentions clear
3. **Fail-Safe Defaults**: Default behavior matches current production behavior
4. **Idempotency Protection**: Detect and prevent duplicate side effect application
5. **Single Responsibility**: Each internal method handles one side effect
6. **Composition Over Configuration**: Build complex behaviors from simple flags

### High-Level Design

```
┌─────────────────────────────────────────────────────────────────┐
│                    PUBLIC API LAYER                             │
│  (Backward compatible methods + New overloaded methods)         │
├─────────────────────────────────────────────────────────────────┤
│  Existing Methods:                                              │
│  - createPayment(bill, pmd)                                     │
│  - updateBalances(payments)                                     │
│  - createPaymentsForCancelling(bill)                            │
├─────────────────────────────────────────────────────────────────┤
│  New Overloaded Methods:                                        │
│  - createPayment(bill, pmd, SideEffectControl)                  │
│  - createPaymentWithFullControl(bill, pmd, options...)          │
│  - createPaymentsForCancelling(bill, SideEffectControl)         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   CONTROL LAYER                                 │
│  - SideEffectControl class (encapsulates Boolean flags)         │
│  - Preset factory methods (common configurations)               │
│  - Validation and idempotency checks                            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   CORE PAYMENT LOGIC                            │
│  - createPayment(..., SideEffectControl) [private]              │
│  - Payment entity creation and persistence                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                   SIDE EFFECT LAYER                             │
│  - updateDrawerIfEnabled(payment, control)                      │
│  - writeCashBookIfEnabled(payment, control)                     │
│  - updatePatientDepositIfEnabled(payment, control)              │
│  - updateCreditCompanyIfEnabled(payment, control)               │
│  - updateStaffIfEnabled(payment, control)                       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Detailed Design

### New Data Structure: SideEffectControl

```java
/**
 * Encapsulates control flags for payment side effects.
 * Provides factory methods for common configurations.
 */
public class SideEffectControl {
    private boolean updateDrawer;
    private boolean writeToCashBook;
    private boolean updatePatientDeposit;
    private boolean updateCreditCompany;
    private boolean updateStaffCredit;
    private boolean updateStaffWelfare;

    // Tracking flags to prevent duplicate updates
    private boolean drawerAlreadyUpdated;
    private boolean cashBookAlreadyWritten;
    private boolean patientDepositAlreadyUpdated;
    private boolean creditCompanyAlreadyUpdated;
    private boolean staffAlreadyUpdated;

    // Private constructor - use factory methods
    private SideEffectControl() {}

    /**
     * DEFAULT: Matches current createPayment() behavior
     * Updates drawer and cashbook ONLY
     */
    public static SideEffectControl createPaymentDefaults() {
        SideEffectControl control = new SideEffectControl();
        control.updateDrawer = true;
        control.writeToCashBook = true;
        control.updatePatientDeposit = false;
        control.updateCreditCompany = false;
        control.updateStaffCredit = false;
        control.updateStaffWelfare = false;
        return control;
    }

    /**
     * FOR UPDATE_BALANCES: Matches current updateBalances() behavior
     * Updates deposits, credit companies, and staff ONLY
     */
    public static SideEffectControl updateBalancesDefaults() {
        SideEffectControl control = new SideEffectControl();
        control.updateDrawer = false;
        control.writeToCashBook = false;
        control.updatePatientDeposit = true;
        control.updateCreditCompany = true;
        control.updateStaffCredit = true;
        control.updateStaffWelfare = true;
        return control;
    }

    /**
     * ALL SIDE EFFECTS: For new code that wants everything
     */
    public static SideEffectControl allSideEffects() {
        SideEffectControl control = new SideEffectControl();
        control.updateDrawer = true;
        control.writeToCashBook = true;
        control.updatePatientDeposit = true;
        control.updateCreditCompany = true;
        control.updateStaffCredit = true;
        control.updateStaffWelfare = true;
        return control;
    }

    /**
     * NO SIDE EFFECTS: For testing or manual control
     */
    public static SideEffectControl noSideEffects() {
        return new SideEffectControl();
    }

    /**
     * CUSTOM BUILDER: For specific combinations
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private SideEffectControl control = new SideEffectControl();

        public Builder updateDrawer(boolean value) {
            control.updateDrawer = value;
            return this;
        }

        public Builder writeToCashBook(boolean value) {
            control.writeToCashBook = value;
            return this;
        }

        public Builder updatePatientDeposit(boolean value) {
            control.updatePatientDeposit = value;
            return this;
        }

        public Builder updateCreditCompany(boolean value) {
            control.updateCreditCompany = value;
            return this;
        }

        public Builder updateStaffCredit(boolean value) {
            control.updateStaffCredit = value;
            return this;
        }

        public Builder updateStaffWelfare(boolean value) {
            control.updateStaffWelfare = value;
            return this;
        }

        public SideEffectControl build() {
            return control;
        }
    }

    // Getters and setters for all fields
    // ... (omitted for brevity)

    /**
     * Marks drawer as already updated to prevent duplicate updates
     */
    public void markDrawerUpdated() {
        this.drawerAlreadyUpdated = true;
    }

    /**
     * Checks if drawer update should be skipped due to already being applied
     */
    public boolean shouldSkipDrawerUpdate() {
        return drawerAlreadyUpdated || !updateDrawer;
    }

    // Similar methods for other side effects
    // ... (omitted for brevity)
}
```

---

## Method Signatures

### Public API Methods (Backward Compatible)

#### Existing Methods (NO CHANGES)

```java
/**
 * EXISTING METHOD - NO CHANGES
 * Creates payments for the given bill and updates relevant records.
 * Updates drawer and cashbook ONLY.
 * Does NOT update patient deposits, credit companies, or staff balances.
 *
 * @param bill The bill for which payments are being created
 * @param paymentMethodData Additional data for processing the payment method
 * @return A list of created payments associated with the bill
 */
public List<Payment> createPayment(Bill bill, PaymentMethodData paymentMethodData) {
    return createPayment(bill, paymentMethodData, SideEffectControl.createPaymentDefaults());
}

/**
 * EXISTING METHOD - NO CHANGES
 * Updates balances for deposits, credit companies, and staff.
 * Does NOT update drawer or cashbook.
 *
 * @param payments List of payments to process
 */
public void updateBalances(List<Payment> payments) {
    if (payments == null) {
        return;
    }
    SideEffectControl control = SideEffectControl.updateBalancesDefaults();
    for (Payment p : payments) {
        applySideEffects(p, control);
    }
}

/**
 * EXISTING METHOD - NO CHANGES
 * Creates payments for cancellation bills using original bill payment data.
 *
 * @param cancellationBill The cancellation bill
 * @return List of created payments (with inverted values)
 */
public List<Payment> createPaymentsForCancelling(Bill cancellationBill) {
    return createPaymentsForCancelling(cancellationBill, SideEffectControl.createPaymentDefaults());
}
```

#### New Overloaded Methods

```java
/**
 * NEW METHOD: Creates payments with explicit side effect control
 *
 * @param bill The bill for which payments are being created
 * @param paymentMethodData Additional data for processing the payment method
 * @param sideEffectControl Control flags for side effects
 * @return A list of created payments associated with the bill
 */
public List<Payment> createPayment(
        Bill bill,
        PaymentMethodData paymentMethodData,
        SideEffectControl sideEffectControl) {
    return createPaymentInternal(
        bill,
        bill.getPaymentMethod(),
        paymentMethodData,
        bill.getDepartment(),
        bill.getCreater(),
        sideEffectControl
    );
}

/**
 * NEW METHOD: Creates payments with individual Boolean flags
 * Convenience method for controllers that prefer explicit parameters
 *
 * @param bill The bill for which payments are being created
 * @param paymentMethodData Additional data for processing the payment method
 * @param updateDrawer Whether to update drawer
 * @param writeToCashBook Whether to write to cashbook
 * @param updatePatientDeposit Whether to update patient deposit balances
 * @param updateCreditCompany Whether to update credit company balances
 * @param updateStaffCredit Whether to update staff credit
 * @param updateStaffWelfare Whether to update staff welfare
 * @return A list of created payments associated with the bill
 */
public List<Payment> createPaymentWithFullControl(
        Bill bill,
        PaymentMethodData paymentMethodData,
        boolean updateDrawer,
        boolean writeToCashBook,
        boolean updatePatientDeposit,
        boolean updateCreditCompany,
        boolean updateStaffCredit,
        boolean updateStaffWelfare) {

    SideEffectControl control = SideEffectControl.builder()
        .updateDrawer(updateDrawer)
        .writeToCashBook(writeToCashBook)
        .updatePatientDeposit(updatePatientDeposit)
        .updateCreditCompany(updateCreditCompany)
        .updateStaffCredit(updateStaffCredit)
        .updateStaffWelfare(updateStaffWelfare)
        .build();

    return createPayment(bill, paymentMethodData, control);
}

/**
 * NEW METHOD: Creates payments with all side effects enabled
 * Equivalent to calling createPayment() + updateBalances() but in one atomic operation
 *
 * @param bill The bill for which payments are being created
 * @param paymentMethodData Additional data for processing the payment method
 * @return A list of created payments associated with the bill
 */
public List<Payment> createPaymentWithAllSideEffects(
        Bill bill,
        PaymentMethodData paymentMethodData) {
    return createPayment(bill, paymentMethodData, SideEffectControl.allSideEffects());
}

/**
 * NEW METHOD: Creates payments without any side effects
 * Useful for testing or when side effects will be applied manually
 *
 * @param bill The bill for which payments are being created
 * @param paymentMethodData Additional data for processing the payment method
 * @return A list of created payments associated with the bill
 */
public List<Payment> createPaymentWithoutSideEffects(
        Bill bill,
        PaymentMethodData paymentMethodData) {
    return createPayment(bill, paymentMethodData, SideEffectControl.noSideEffects());
}

/**
 * NEW METHOD: Creates cancellation payments with explicit side effect control
 *
 * @param cancellationBill The cancellation bill
 * @param sideEffectControl Control flags for side effects
 * @return List of created payments (with inverted values)
 */
public List<Payment> createPaymentsForCancelling(
        Bill cancellationBill,
        SideEffectControl sideEffectControl) {
    // Implementation with side effect control
    List<Payment> newPayments = new ArrayList<>();
    List<Payment> originalBillPayments = billService.fetchBillPayments(cancellationBill.getBilledBill());
    if (originalBillPayments != null) {
        for (Payment originalBillPayment : originalBillPayments) {
            Payment p = originalBillPayment.clonePaymentForNewBill();
            p.invertValues();
            p.setBill(cancellationBill);
            p.setInstitution(cancellationBill.getInstitution());
            p.setDepartment(cancellationBill.getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(cancellationBill.getCreater());
            paymentFacade.create(p);
            newPayments.add(p);
            applySideEffects(p, sideEffectControl);
        }
    }
    return newPayments;
}

/**
 * NEW METHOD: Apply side effects to existing payments
 * Useful when payments were created without side effects and need to be applied later
 *
 * @param payments List of payments to apply side effects to
 * @param sideEffectControl Control flags for side effects
 */
public void applySideEffects(List<Payment> payments, SideEffectControl sideEffectControl) {
    if (payments == null) {
        return;
    }
    for (Payment p : payments) {
        applySideEffects(p, sideEffectControl);
    }
}
```

### Private/Internal Methods

```java
/**
 * REFACTORED: Core payment creation logic with side effect control
 */
private List<Payment> createPaymentInternal(
        Bill bill,
        PaymentMethod pm,
        PaymentMethodData paymentMethodData,
        Department department,
        WebUser webUser,
        SideEffectControl sideEffectControl) {

    CashBook cashbook = null;
    if (sideEffectControl.isWriteToCashBook()) {
        cashbook = cashbookService.findAndSaveCashBookBySite(
            department.getSite(),
            department.getInstitution(),
            department
        );
    }

    List<Payment> payments = new ArrayList<>();
    Date currentDate = new Date();

    if (pm == PaymentMethod.MultiplePaymentMethods) {
        for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple()
                .getMultiplePaymentMethodComponentDetails()) {
            Payment payment = createPaymentFromComponentDetail(
                cd, bill, department, webUser, currentDate
            );
            if (payment != null) {
                paymentFacade.create(payment);
                applySideEffects(payment, sideEffectControl, webUser, cashbook, department);
                payments.add(payment);
            }
        }
    } else {
        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setInstitution(department.getInstitution());
        payment.setDepartment(department);
        payment.setCreatedAt(currentDate);
        payment.setCreater(webUser);
        payment.setPaymentMethod(pm);

        if (!populatePaymentDetails(payment, pm, paymentMethodData)) {
            LOGGER.log(Level.WARNING,
                "Skipping payment creation for bill {0} due to missing payment data for method {1}.",
                new Object[]{bill != null ? bill.getId() : null, pm});
            return payments;
        }

        payment.setPaidValue(bill.getNetTotal());
        paymentFacade.create(payment);
        applySideEffects(payment, sideEffectControl, webUser, cashbook, department);
        payments.add(payment);
    }

    return payments;
}

/**
 * NEW: Unified side effect application method
 */
private void applySideEffects(
        Payment payment,
        SideEffectControl control) {
    applySideEffects(payment, control, null, null, null);
}

/**
 * NEW: Unified side effect application method with optional parameters
 */
private void applySideEffects(
        Payment payment,
        SideEffectControl control,
        WebUser webUser,
        CashBook cashbook,
        Department department) {

    if (payment == null || control == null) {
        return;
    }

    // Apply side effects based on control flags
    updateDrawerIfEnabled(payment, control);
    writeCashBookIfEnabled(payment, control, webUser, cashbook, department);
    updatePatientDepositIfEnabled(payment, control);
    updateCreditCompanyIfEnabled(payment, control);
    updateStaffCreditIfEnabled(payment, control);
    updateStaffWelfareIfEnabled(payment, control);
}

/**
 * NEW: Update drawer if enabled and not already updated
 */
private void updateDrawerIfEnabled(Payment payment, SideEffectControl control) {
    if (control.shouldSkipDrawerUpdate()) {
        return;
    }
    drawerService.updateDrawer(payment);
    control.markDrawerUpdated();
}

/**
 * NEW: Write to cashbook if enabled and not already written
 */
private void writeCashBookIfEnabled(
        Payment payment,
        SideEffectControl control,
        WebUser webUser,
        CashBook cashbook,
        Department department) {

    if (control.shouldSkipCashBookWrite()) {
        return;
    }

    if (cashbook != null && webUser != null && department != null) {
        cashbookService.writeCashBookEntryAtPaymentCreation(
            payment, webUser, cashbook, department
        );
    } else {
        cashbookService.writeCashBookEntryAtPaymentCreation(payment);
    }

    control.markCashBookWritten();
}

/**
 * NEW: Update patient deposit if enabled and applicable
 */
private void updatePatientDepositIfEnabled(Payment payment, SideEffectControl control) {
    if (control.shouldSkipPatientDepositUpdate()) {
        return;
    }

    if (payment.getPaymentMethod() == PaymentMethod.PatientDeposit) {
        updatePatientDeposits(payment);
        control.markPatientDepositUpdated();
    }
}

/**
 * NEW: Update credit company if enabled and applicable
 */
private void updateCreditCompanyIfEnabled(Payment payment, SideEffectControl control) {
    if (control.shouldSkipCreditCompanyUpdate()) {
        return;
    }

    if (payment.getPaymentMethod() == PaymentMethod.Credit) {
        updateCompanyCredit(payment);
        control.markCreditCompanyUpdated();
    }
}

/**
 * NEW: Update staff credit if enabled and applicable
 */
private void updateStaffCreditIfEnabled(Payment payment, SideEffectControl control) {
    if (control.shouldSkipStaffUpdate()) {
        return;
    }

    if (payment.getPaymentMethod() == PaymentMethod.Staff) {
        updateStaffCredit(payment);
        control.markStaffUpdated();
    }
}

/**
 * NEW: Update staff welfare if enabled and applicable
 */
private void updateStaffWelfareIfEnabled(Payment payment, SideEffectControl control) {
    if (control.shouldSkipStaffUpdate()) {
        return;
    }

    if (payment.getPaymentMethod() == PaymentMethod.Staff_Welfare) {
        updateStaffWelare(payment);
        control.markStaffUpdated();
    }
}

// Existing private methods remain unchanged:
// - createPaymentFromComponentDetail()
// - populatePaymentDetails()
// - updateCompanyCredit()
// - updateStaffCredit()
// - updateStaffWelare()
// - updatePatientDeposits()
```

---

## Implementation Approach

### Phase 1: Add SideEffectControl Class

**File**: `/home/buddhika/development/rh/src/main/java/com/divudi/service/SideEffectControl.java`

1. Create new class in same package as PaymentService
2. Implement all factory methods
3. Implement builder pattern
4. Add tracking flags for idempotency
5. Add comprehensive Javadoc

### Phase 2: Refactor Internal Methods

**File**: `/home/buddhika/development/rh/src/main/java/com/divudi/service/PaymentService.java`

1. Rename existing `createPayment()` private method to `createPaymentInternal()`
2. Add `SideEffectControl` parameter to `createPaymentInternal()`
3. Create new private methods for conditional side effects:
   - `applySideEffects()`
   - `updateDrawerIfEnabled()`
   - `writeCashBookIfEnabled()`
   - `updatePatientDepositIfEnabled()`
   - `updateCreditCompanyIfEnabled()`
   - `updateStaffCreditIfEnabled()`
   - `updateStaffWelfareIfEnabled()`
4. Keep existing private helper methods unchanged:
   - `createPaymentFromComponentDetail()`
   - `populatePaymentDetails()`
   - `updateCompanyCredit()`
   - `updateStaffCredit()`
   - `updateStaffWelare()`
   - `updatePatientDeposits()`

### Phase 3: Update Public Methods for Backward Compatibility

**File**: `/home/buddhika/development/rh/src/main/java/com/divudi/service/PaymentService.java`

1. Update existing `createPayment(Bill, PaymentMethodData)`:
   ```java
   public List<Payment> createPayment(Bill bill, PaymentMethodData paymentMethodData) {
       return createPayment(bill, paymentMethodData, SideEffectControl.createPaymentDefaults());
   }
   ```

2. Update existing `updateBalances(List<Payment>)`:
   ```java
   public void updateBalances(List<Payment> payments) {
       if (payments == null) {
           return;
       }
       SideEffectControl control = SideEffectControl.updateBalancesDefaults();
       for (Payment p : payments) {
           applySideEffects(p, control);
       }
   }
   ```

3. Update existing `createPaymentsForCancelling(Bill)`:
   ```java
   public List<Payment> createPaymentsForCancelling(Bill cancellationBill) {
       return createPaymentsForCancelling(cancellationBill, SideEffectControl.createPaymentDefaults());
   }
   ```

### Phase 4: Add New Overloaded Methods

**File**: `/home/buddhika/development/rh/src/main/java/com/divudi/service/PaymentService.java`

Add all new public methods defined in [Method Signatures](#method-signatures) section.

### Phase 5: Update Javadoc

**File**: `/home/buddhika/development/rh/src/main/java/com/divudi/service/PaymentService.java`

1. Update Javadoc for existing methods to clarify side effects
2. Add comprehensive Javadoc for new methods
3. Add usage examples in class-level Javadoc

---

## Backward Compatibility Guarantees

### Guarantee 1: Exact Behavior Preservation

**Existing Call Pattern 1:**
```java
List<Payment> payments = paymentService.createPayment(bill, paymentMethodData);
```

**Behavior Before and After:**
- Creates payment entities: YES
- Updates drawer: YES
- Writes to cashbook: YES
- Updates patient deposit: NO
- Updates credit company: NO
- Updates staff credit/welfare: NO

**Implementation:**
```java
public List<Payment> createPayment(Bill bill, PaymentMethodData paymentMethodData) {
    // Delegates to internal method with createPaymentDefaults()
    return createPayment(bill, paymentMethodData, SideEffectControl.createPaymentDefaults());
}
```

---

**Existing Call Pattern 2:**
```java
List<Payment> payments = paymentService.createPayment(bill, paymentMethodData);
paymentService.updateBalances(payments);
```

**Behavior Before and After:**
- Creates payment entities: YES
- Updates drawer: YES (from createPayment)
- Writes to cashbook: YES (from createPayment)
- Updates patient deposit: YES (from updateBalances)
- Updates credit company: YES (from updateBalances)
- Updates staff credit/welfare: YES (from updateBalances)

**No duplicate updates** because:
1. `createPayment()` uses `createPaymentDefaults()` which has balance updates OFF
2. `updateBalances()` uses `updateBalancesDefaults()` which has drawer/cashbook OFF
3. Tracking flags prevent duplicate application even if called multiple times

---

**Existing Call Pattern 3:**
```java
paymentService.updateBalances(payments);
```

**Behavior Before and After:**
- Updates patient deposit: YES (if PaymentMethod.PatientDeposit)
- Updates credit company: YES (if PaymentMethod.Credit)
- Updates staff credit: YES (if PaymentMethod.Staff)
- Updates staff welfare: YES (if PaymentMethod.Staff_Welfare)
- Updates drawer: NO
- Writes to cashbook: NO

---

### Guarantee 2: No Compilation Breaks

- All existing method signatures remain unchanged
- No changes to return types
- No changes to parameter types
- New methods are additions, not replacements

### Guarantee 3: No Runtime Breaks

- All existing code paths preserved
- No changes to exception handling
- No changes to null handling
- Logging behavior unchanged

### Guarantee 4: No Data Integrity Issues

- Transaction boundaries unchanged for existing calls
- No double-updates (protected by tracking flags)
- No missing updates (defaults match current behavior)

### Guarantee 5: Performance Unchanged

- No additional database queries for existing calls
- Control object creation is negligible overhead
- Conditional checks are in-memory operations

---

## Migration Strategy

### Phase 1: Documentation and Training (Week 1)

1. **Create Migration Guide**
   - Document new methods and usage patterns
   - Provide decision tree for choosing methods
   - Show before/after examples

2. **Developer Training**
   - Present design to team
   - Walk through common migration scenarios
   - Answer questions

3. **Update Developer Documentation**
   - Add to `/developer_docs/services/payment-service-guide.md`
   - Include usage examples
   - Document best practices

### Phase 2: Internal Implementation (Week 1-2)

1. **Implement Core Changes**
   - Create SideEffectControl class
   - Refactor internal methods
   - Update existing methods for backward compatibility
   - Add new overloaded methods

2. **Add Unit Tests**
   - Test backward compatibility
   - Test new methods with various flag combinations
   - Test idempotency protection
   - Test edge cases

3. **Code Review**
   - Team review of implementation
   - Security review of changes
   - Performance review

### Phase 3: Controller Migration (Gradual, 3-6 months)

**Priority 1: High-Value Targets (Month 1)**

Migrate controllers with complex payment logic first:
- PharmacyRefundForItemReturnsController
- PharmacyPreSettleController
- OpdBillController
- InwardPaymentController

**Migration Pattern:**
```java
// BEFORE
List<Payment> payments = paymentService.createPayment(bill, paymentMethodData);
paymentService.updateBalances(payments);

// AFTER (Option 1: Explicit control)
List<Payment> payments = paymentService.createPaymentWithAllSideEffects(bill, paymentMethodData);

// AFTER (Option 2: Selective control)
List<Payment> payments = paymentService.createPaymentWithFullControl(
    bill,
    paymentMethodData,
    true,  // updateDrawer
    true,  // writeToCashBook
    true,  // updatePatientDeposit
    true,  // updateCreditCompany
    true,  // updateStaffCredit
    true   // updateStaffWelfare
);

// AFTER (Option 3: Advanced control)
SideEffectControl control = SideEffectControl.builder()
    .updateDrawer(true)
    .writeToCashBook(true)
    .updatePatientDeposit(bill.getPaymentMethod() == PaymentMethod.PatientDeposit)
    .updateCreditCompany(bill.getPaymentMethod() == PaymentMethod.Credit)
    .updateStaffCredit(bill.getPaymentMethod() == PaymentMethod.Staff)
    .updateStaffWelfare(bill.getPaymentMethod() == PaymentMethod.Staff_Welfare)
    .build();
List<Payment> payments = paymentService.createPayment(bill, paymentMethodData, control);
```

**Priority 2: Medium-Value Targets (Month 2-3)**

Migrate controllers that only call createPayment():
- PharmacyPurchaseController
- SupplierPaymentController
- GrnController

**Migration Pattern:**
```java
// BEFORE
List<Payment> ps = paymentService.createPayment(getBill(), getPaymentMethodData());

// AFTER (if no balance updates needed - no change required!)
List<Payment> ps = paymentService.createPayment(getBill(), getPaymentMethodData());

// AFTER (if balance updates should be added)
List<Payment> ps = paymentService.createPaymentWithAllSideEffects(getBill(), getPaymentMethodData());
```

**Priority 3: Low-Value Targets (Month 4-6)**

Migrate remaining controllers and refactor direct `new Payment()` instantiations:
- Replace direct instantiation with service method calls
- Add proper side effect control

**Migration Pattern for Direct Instantiation:**
```java
// BEFORE
Payment payment = new Payment();
payment.setBill(bill);
// ... set properties
paymentFacade.create(payment);
drawerService.updateDrawer(payment);
cashbookService.writeCashBookEntryAtPaymentCreation(payment);

// AFTER
List<Payment> payments = paymentService.createPaymentWithFullControl(
    bill,
    paymentMethodData,
    true,  // updateDrawer
    true,  // writeToCashBook
    false, // updatePatientDeposit
    false, // updateCreditCompany
    false, // updateStaffCredit
    false  // updateStaffWelfare
);
```

### Phase 4: Testing and Validation (Ongoing)

1. **Regression Testing**
   - Run full test suite after each controller migration
   - Manual testing of critical payment flows
   - Verify side effects are applied correctly

2. **Data Validation**
   - Compare drawer balances before/after migration
   - Verify cashbook entries are identical
   - Check patient deposit balances
   - Validate credit company balances
   - Confirm staff credit/welfare updates

3. **Monitoring**
   - Add logging to track method usage
   - Monitor for duplicate side effect application
   - Track any errors or anomalies

### Phase 5: Deprecation Planning (Month 6+)

1. **Mark Legacy Patterns**
   - Add `@Deprecated` annotations where appropriate
   - Document recommended replacements
   - Set timeline for removal

2. **Remove Redundant Code**
   - After all controllers migrated, consider removing direct instantiation patterns
   - Clean up commented-out code
   - Refactor remaining technical debt

---

## Testing Requirements

### Unit Tests

**File**: `/home/buddhika/development/rh/src/test/java/com/divudi/service/PaymentServiceTest.java`

#### Test Suite 1: Backward Compatibility

```java
@Test
public void testCreatePayment_BackwardCompatibility_OnlyDrawerAndCashbook() {
    // Arrange
    Bill bill = createTestBill();
    PaymentMethodData pmd = createTestPaymentMethodData(PaymentMethod.Cash);

    // Act
    List<Payment> payments = paymentService.createPayment(bill, pmd);

    // Assert
    assertEquals(1, payments.size());
    verify(drawerService, times(1)).updateDrawer(any(Payment.class));
    verify(cashbookService, times(1)).writeCashBookEntryAtPaymentCreation(any(Payment.class));
    verify(patientDepositService, never()).updateBalance(any(), any());
    verify(institutionFacade, never()).edit(any());
    verify(staffService, never()).updateStaffWelfare(any(), anyDouble());
}

@Test
public void testUpdateBalances_BackwardCompatibility_OnlyBalanceUpdates() {
    // Arrange
    List<Payment> payments = createTestPayments(PaymentMethod.Credit);

    // Act
    paymentService.updateBalances(payments);

    // Assert
    verify(institutionFacade, times(1)).edit(any()); // Credit company update
    verify(drawerService, never()).updateDrawer(any());
    verify(cashbookService, never()).writeCashBookEntryAtPaymentCreation(any());
}

@Test
public void testCreatePaymentThenUpdateBalances_NoDuplicates() {
    // Arrange
    Bill bill = createTestBill();
    PaymentMethodData pmd = createTestPaymentMethodData(PaymentMethod.Credit);

    // Act
    List<Payment> payments = paymentService.createPayment(bill, pmd);
    paymentService.updateBalances(payments);

    // Assert
    verify(drawerService, times(1)).updateDrawer(any()); // From createPayment
    verify(cashbookService, times(1)).writeCashBookEntryAtPaymentCreation(any()); // From createPayment
    verify(institutionFacade, times(1)).edit(any()); // From updateBalances
}
```

#### Test Suite 2: New Methods

```java
@Test
public void testCreatePaymentWithAllSideEffects_AllUpdatesApplied() {
    // Arrange
    Bill bill = createTestBill(PaymentMethod.Credit);
    PaymentMethodData pmd = createTestPaymentMethodData(PaymentMethod.Credit);

    // Act
    List<Payment> payments = paymentService.createPaymentWithAllSideEffects(bill, pmd);

    // Assert
    assertEquals(1, payments.size());
    verify(drawerService, times(1)).updateDrawer(any());
    verify(cashbookService, times(1)).writeCashBookEntryAtPaymentCreation(any());
    verify(institutionFacade, times(1)).edit(any()); // Credit company update
}

@Test
public void testCreatePaymentWithoutSideEffects_NoUpdatesApplied() {
    // Arrange
    Bill bill = createTestBill();
    PaymentMethodData pmd = createTestPaymentMethodData(PaymentMethod.Cash);

    // Act
    List<Payment> payments = paymentService.createPaymentWithoutSideEffects(bill, pmd);

    // Assert
    assertEquals(1, payments.size());
    verify(drawerService, never()).updateDrawer(any());
    verify(cashbookService, never()).writeCashBookEntryAtPaymentCreation(any());
}

@Test
public void testCreatePaymentWithFullControl_SelectiveSideEffects() {
    // Arrange
    Bill bill = createTestBill();
    PaymentMethodData pmd = createTestPaymentMethodData(PaymentMethod.Cash);

    // Act
    List<Payment> payments = paymentService.createPaymentWithFullControl(
        bill, pmd,
        true,  // updateDrawer
        false, // writeToCashBook
        false, // updatePatientDeposit
        false, // updateCreditCompany
        false, // updateStaffCredit
        false  // updateStaffWelfare
    );

    // Assert
    assertEquals(1, payments.size());
    verify(drawerService, times(1)).updateDrawer(any());
    verify(cashbookService, never()).writeCashBookEntryAtPaymentCreation(any());
}
```

#### Test Suite 3: Idempotency

```java
@Test
public void testApplySideEffects_CalledMultipleTimes_NoDoubleUpdate() {
    // Arrange
    Payment payment = createTestPayment(PaymentMethod.Credit);
    SideEffectControl control = SideEffectControl.allSideEffects();

    // Act
    paymentService.applySideEffects(Collections.singletonList(payment), control);
    paymentService.applySideEffects(Collections.singletonList(payment), control);

    // Assert - should only be called once due to tracking flags
    verify(drawerService, times(1)).updateDrawer(payment);
    verify(cashbookService, times(1)).writeCashBookEntryAtPaymentCreation(payment);
    verify(institutionFacade, times(1)).edit(any());
}
```

#### Test Suite 4: Edge Cases

```java
@Test
public void testCreatePayment_NullPaymentMethodData_HandlesGracefully() {
    // Arrange
    Bill bill = createTestBill();

    // Act
    List<Payment> payments = paymentService.createPayment(bill, null);

    // Assert
    assertTrue(payments.isEmpty());
}

@Test
public void testUpdateBalances_NullPaymentList_NoError() {
    // Act
    paymentService.updateBalances(null);

    // Assert - no exception thrown
}

@Test
public void testCreatePayment_MultiplePaymentMethods_AllComponentsProcessed() {
    // Arrange
    Bill bill = createTestBill();
    PaymentMethodData pmd = createTestMultiplePaymentMethodData();

    // Act
    List<Payment> payments = paymentService.createPayment(bill, pmd);

    // Assert
    assertEquals(3, payments.size()); // Assuming 3 payment components
    verify(drawerService, times(3)).updateDrawer(any());
}
```

#### Test Suite 5: Payment Method Specific

```java
@Test
public void testCreatePayment_PatientDeposit_OnlyUpdatesWhenFlagEnabled() {
    // Arrange
    Bill bill = createTestBill(PaymentMethod.PatientDeposit);
    PaymentMethodData pmd = createTestPaymentMethodData(PaymentMethod.PatientDeposit);

    // Act - without balance updates
    List<Payment> payments1 = paymentService.createPayment(bill, pmd);

    // Assert
    verify(patientDepositService, never()).updateBalance(any(), any());

    // Act - with balance updates
    List<Payment> payments2 = paymentService.createPaymentWithAllSideEffects(bill, pmd);

    // Assert
    verify(patientDepositService, times(1)).updateBalance(any(), any());
}

@Test
public void testUpdateBalances_CreditPayment_UpdatesCreditCompany() {
    // Arrange
    Payment payment = createTestPayment(PaymentMethod.Credit);
    payment.getCreditCompany().setAllowedCredit(1000.0);
    payment.setPaidValue(500.0);

    // Act
    paymentService.updateBalances(Collections.singletonList(payment));

    // Assert
    ArgumentCaptor<Institution> captor = ArgumentCaptor.forClass(Institution.class);
    verify(institutionFacade).edit(captor.capture());
    assertEquals(1500.0, captor.getValue().getAllowedCredit(), 0.01);
}

@Test
public void testUpdateBalances_StaffCreditPayment_UpdatesStaffBalance() {
    // Arrange
    Payment payment = createTestPayment(PaymentMethod.Staff);

    // Act
    paymentService.updateBalances(Collections.singletonList(payment));

    // Assert
    verify(staffService, times(1)).updateStaffWelfare(any(Staff.class), anyDouble());
}
```

### Integration Tests

**File**: `/home/buddhika/development/rh/src/test/java/com/divudi/service/PaymentServiceIntegrationTest.java`

```java
@Test
@Transactional
public void testPaymentWorkflow_OpdBilling_EndToEnd() {
    // Simulate complete OPD billing workflow
    // 1. Create bill
    // 2. Create payment with all side effects
    // 3. Verify database state
    // 4. Verify drawer balance
    // 5. Verify cashbook entry
}

@Test
@Transactional
public void testPaymentWorkflow_PharmacyRefund_EndToEnd() {
    // Simulate pharmacy refund workflow
    // 1. Create original sale
    // 2. Create refund bill
    // 3. Create refund payment with inverted values
    // 4. Verify patient deposit updated
    // 5. Verify drawer updated correctly
}

@Test
@Transactional
public void testPaymentWorkflow_CreditCompanyBilling_EndToEnd() {
    // Simulate credit company billing
    // 1. Create bill with credit payment
    // 2. Create payment
    // 3. Verify credit company balance increased
    // 4. Create cancellation
    // 5. Verify credit company balance decreased
}
```

### Manual Testing Checklist

1. **Pharmacy Sales**
   - [ ] Cash payment creates drawer entry
   - [ ] Credit card payment creates cashbook entry
   - [ ] Patient deposit payment updates patient balance
   - [ ] Multiple payment methods handled correctly

2. **Pharmacy Refunds**
   - [ ] Refund reduces drawer correctly
   - [ ] Refund to patient deposit increases balance
   - [ ] Refund cashbook entry has correct sign

3. **OPD Billing**
   - [ ] Cash payment drawer update
   - [ ] Credit company payment updates company balance
   - [ ] Staff credit payment updates staff balance

4. **Inward Billing**
   - [ ] Patient deposit payments
   - [ ] Staff welfare payments
   - [ ] Multiple payment methods

5. **Cancellations**
   - [ ] Cancellation reverses drawer entries
   - [ ] Cancellation reverses cashbook entries
   - [ ] Cancellation reverses balance updates

---

## Risk Analysis

### High Risks

#### Risk 1: Backward Compatibility Breaks

**Description**: Existing controller calls might behave differently after refactoring

**Likelihood**: Medium

**Impact**: Critical

**Mitigation**:
- Comprehensive unit test suite covering all existing call patterns
- Integration tests for common workflows
- Gradual rollout with extensive testing between phases
- Code review focused on backward compatibility
- Monitoring after deployment

**Detection**:
- Automated test failures
- Regression test suite
- User reports of incorrect balances
- Audit of drawer/cashbook discrepancies

#### Risk 2: Double Updates

**Description**: Side effects applied twice, causing incorrect balances

**Likelihood**: Low (with tracking flags)

**Impact**: High

**Mitigation**:
- Implement tracking flags in SideEffectControl
- Add idempotency checks before each side effect
- Log when duplicate attempts detected
- Unit tests specifically for idempotency
- Monitor logs for duplicate attempts

**Detection**:
- Balance discrepancies in drawer
- Double entries in cashbook
- Patient deposit balance errors
- Log analysis

#### Risk 3: Missing Updates

**Description**: Side effects not applied when they should be

**Likelihood**: Medium

**Impact**: High

**Mitigation**:
- Careful default configuration (match current behavior)
- Extensive testing of all payment methods
- Integration tests for complete workflows
- Manual testing of critical paths

**Detection**:
- Missing drawer entries
- Missing cashbook entries
- Patient deposits not updated
- Credit company balances incorrect

### Medium Risks

#### Risk 4: Performance Degradation

**Description**: Additional control logic slows down payment processing

**Likelihood**: Low

**Impact**: Medium

**Mitigation**:
- Keep control objects lightweight
- Minimize additional database queries
- Profile performance before/after
- Load testing with realistic data volumes

**Detection**:
- Performance monitoring
- User reports of slowness
- Increased transaction times

#### Risk 5: Migration Complexity

**Description**: 59+ controllers are complex to migrate

**Likelihood**: High

**Impact**: Medium

**Mitigation**:
- Phased migration approach
- Start with high-value targets
- Create clear migration guide
- Provide team training
- Code review for each migration

**Detection**:
- Migration timeline slipping
- Inconsistent controller implementations
- Errors during testing

### Low Risks

#### Risk 6: Developer Confusion

**Description**: Developers unsure which method to use

**Likelihood**: Medium

**Impact**: Low

**Mitigation**:
- Comprehensive documentation
- Clear naming conventions
- Usage examples in Javadoc
- Decision tree in migration guide
- Team training sessions

**Detection**:
- Questions in code reviews
- Incorrect method usage
- Pull request comments

#### Risk 7: Transaction Boundary Issues

**Description**: Side effects outside transaction boundaries

**Likelihood**: Low

**Impact**: Medium

**Mitigation**:
- Document transaction requirements
- Add @Transactional where needed
- Test rollback scenarios
- Consider adding transaction management layer

**Detection**:
- Partial updates after errors
- Inconsistent data state
- Failed rollbacks

---

## Implementation Phases

### Phase 1: Foundation (Week 1)

**Goal**: Create supporting infrastructure without affecting existing code

**Tasks**:
1. Create SideEffectControl class
2. Add unit tests for SideEffectControl
3. Code review and approval
4. Merge to development branch

**Deliverables**:
- `/src/main/java/com/divudi/service/SideEffectControl.java`
- `/src/test/java/com/divudi/service/SideEffectControlTest.java`

**Success Criteria**:
- All tests pass
- Code review approved
- No impact on existing functionality

---

### Phase 2: Internal Refactoring (Week 2)

**Goal**: Refactor PaymentService internals without changing public API

**Tasks**:
1. Create new private methods for conditional side effects
2. Refactor `createPaymentInternal()` method
3. Add `applySideEffects()` method
4. Add unit tests for private methods (via public API)
5. Code review and approval

**Deliverables**:
- Updated PaymentService.java with new private methods
- Unit tests verifying internal behavior
- No changes to public method signatures

**Success Criteria**:
- All existing tests pass
- New tests cover internal refactoring
- Performance unchanged
- Code review approved

---

### Phase 3: Public API Expansion (Week 2-3)

**Goal**: Add new public methods while maintaining backward compatibility

**Tasks**:
1. Update existing public methods to use SideEffectControl
2. Add new overloaded methods
3. Add comprehensive Javadoc
4. Create unit tests for all new methods
5. Create integration tests for common workflows
6. Code review and approval

**Deliverables**:
- Complete set of new public methods
- Comprehensive test suite
- Updated Javadoc
- Migration guide

**Success Criteria**:
- All tests pass (existing + new)
- 100% backward compatibility verified
- Code review approved
- Documentation complete

---

### Phase 4: Documentation and Training (Week 3)

**Goal**: Prepare team for migration

**Tasks**:
1. Create migration guide
2. Update developer documentation
3. Create usage examples
4. Conduct training session
5. Answer questions and clarify usage

**Deliverables**:
- `/developer_docs/services/payment-service-migration-guide.md`
- Updated `/developer_docs/services/payment-service-guide.md`
- Training presentation
- FAQ document

**Success Criteria**:
- Team understands new methods
- Migration guide is clear and comprehensive
- Questions answered

---

### Phase 5: Pilot Migration (Week 4)

**Goal**: Migrate 2-3 high-priority controllers as pilot

**Tasks**:
1. Select pilot controllers (e.g., PharmacyRefundForItemReturnsController)
2. Migrate each controller
3. Create unit tests for migrated controllers
4. Manual testing of migrated workflows
5. Code review
6. Deploy to QA environment
7. Validate in QA

**Deliverables**:
- 2-3 migrated controllers
- Test results
- QA validation report

**Success Criteria**:
- All tests pass
- No regressions in QA
- Balances correct in test data
- Team comfortable with migration process

---

### Phase 6: Gradual Migration (Month 2-6)

**Goal**: Migrate remaining controllers in priority order

**Tasks**:
1. Month 2: High-priority controllers (10-15 controllers)
2. Month 3: Medium-priority controllers (20-25 controllers)
3. Month 4-6: Remaining controllers and direct instantiation refactoring

**Deliverables**:
- All controllers migrated
- Direct `new Payment()` patterns eliminated
- Updated tests
- QA validation after each batch

**Success Criteria**:
- No production issues
- All balances correct
- Performance maintained
- Team proficient with new methods

---

### Phase 7: Cleanup and Optimization (Month 6+)

**Goal**: Remove technical debt and optimize

**Tasks**:
1. Remove deprecated code patterns
2. Add @Deprecated annotations where appropriate
3. Optimize performance if needed
4. Final documentation review
5. Post-implementation review

**Deliverables**:
- Cleaned codebase
- Performance analysis
- Lessons learned document
- Final documentation

**Success Criteria**:
- Codebase fully migrated
- No deprecated patterns in use
- Performance meets requirements
- Team satisfied with new approach

---

## Transaction Management

### Current State

PaymentService is a `@Stateless` EJB with no explicit transaction management. Individual operations have transaction boundaries:

```java
@Stateless
public class PaymentService {
    // No @TransactionManagement annotation
    // Methods don't have @Transactional
    // Each facade call has its own transaction
}
```

### Problem Statement

Side effects span multiple database operations:

1. **Payment creation** (`paymentFacade.create()`)
2. **Drawer update** (`drawerService.updateDrawer()`)
3. **Cashbook write** (`cashbookService.writeCashBookEntryAtPaymentCreation()`)
4. **Patient deposit update** (`patientFacade.edit()` + `patientDepositService.updateBalance()`)
5. **Credit company update** (`institutionFacade.edit()`)
6. **Staff update** (`staffService.updateStaffWelfare()`)

**Risk**: If step 3 fails, steps 1-2 are already committed. Data becomes inconsistent.

### Recommended Approach

#### Option 1: Container-Managed Transactions (Recommended)

Add transaction attributes to public methods:

```java
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

@Stateless
public class PaymentService {

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Payment> createPayment(Bill bill, PaymentMethodData paymentMethodData) {
        // All operations in one transaction
        // Rollback on any exception
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateBalances(List<Payment> payments) {
        // All balance updates in one transaction
    }
}
```

**Pros**:
- Declarative, minimal code changes
- Automatic rollback on exceptions
- Container manages transaction lifecycle
- Works with existing EJB architecture

**Cons**:
- Less granular control
- Transaction timeout may need adjustment for large operations

#### Option 2: Explicit Transaction Management

Use EntityManager and UserTransaction:

```java
@Resource
private UserTransaction userTransaction;

public List<Payment> createPaymentWithTransaction(
        Bill bill,
        PaymentMethodData paymentMethodData) throws Exception {
    try {
        userTransaction.begin();

        List<Payment> payments = createPayment(bill, paymentMethodData);
        // All side effects

        userTransaction.commit();
        return payments;
    } catch (Exception e) {
        userTransaction.rollback();
        throw e;
    }
}
```

**Pros**:
- Fine-grained control
- Can commit/rollback at specific points

**Cons**:
- More verbose
- Error-prone
- Requires careful exception handling

#### Option 3: Two-Phase Approach (Minimal Change)

Keep current behavior but add compensating transactions:

```java
public List<Payment> createPayment(Bill bill, PaymentMethodData pmd) {
    List<Payment> payments = null;
    try {
        payments = createPaymentInternal(bill, pmd, control);
        return payments;
    } catch (Exception e) {
        // Compensate: reverse side effects
        if (payments != null) {
            compensatePayments(payments);
        }
        throw e;
    }
}

private void compensatePayments(List<Payment> payments) {
    for (Payment p : payments) {
        // Reverse drawer update
        // Reverse cashbook entry
        // Mark payment as retired
    }
}
```

**Pros**:
- Minimal changes to existing code
- No transaction boundary changes

**Cons**:
- Complex compensation logic
- Not truly atomic
- Potential for partial failures

### Recommendation

**Use Option 1 (Container-Managed Transactions)** with the following implementation:

```java
@Stateless
@TransactionManagement(TransactionManagementType.CONTAINER)
public class PaymentService {

    // Default transaction attribute for all public methods
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Payment> createPayment(
            Bill bill,
            PaymentMethodData paymentMethodData) {
        // Implementation with all side effects in one transaction
    }

    // Explicit REQUIRES_NEW for independent operations
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public List<Payment> createPaymentIndependent(
            Bill bill,
            PaymentMethodData paymentMethodData) {
        // New transaction, independent of caller
    }

    // Support for caller's transaction
    @TransactionAttribute(TransactionAttributeType.MANDATORY)
    public List<Payment> createPaymentInCallerTransaction(
            Bill bill,
            PaymentMethodData paymentMethodData) {
        // Must be called within existing transaction
        // Throws exception if no transaction
    }
}
```

### Transaction Configuration

Add to `persistence.xml` if needed:

```xml
<properties>
    <!-- Existing properties -->
    <property name="javax.persistence.transactionType" value="JTA"/>
    <property name="eclipselink.transaction.type" value="JTA"/>

    <!-- Transaction timeout (30 seconds) -->
    <property name="javax.persistence.lock.timeout" value="30000"/>
</properties>
```

### Testing Transaction Behavior

```java
@Test
public void testCreatePayment_RollbackOnError() {
    // Arrange
    Bill bill = createTestBill();
    PaymentMethodData pmd = createTestPaymentMethodData();

    // Mock drawer service to throw exception
    doThrow(new RuntimeException("Drawer update failed"))
        .when(drawerService).updateDrawer(any());

    // Act & Assert
    assertThrows(RuntimeException.class, () -> {
        paymentService.createPayment(bill, pmd);
    });

    // Verify payment was NOT persisted (rollback worked)
    List<Payment> payments = paymentFacade.findAll();
    assertEquals(0, payments.size());
}
```

---

## Edge Cases and Special Scenarios

### Edge Case 1: Multiple Payment Methods

**Scenario**: Bill uses MultiplePaymentMethods (e.g., Cash + Card)

**Current Behavior**:
- Each component creates separate Payment entity
- Each Payment gets drawer and cashbook entries
- Each Payment independently checks for balance updates

**New Behavior**:
- Same as current, but with explicit control
- Can disable side effects for specific components
- Tracking prevents duplicate updates across components

**Implementation**:
```java
if (pm == PaymentMethod.MultiplePaymentMethods) {
    for (ComponentDetail cd : paymentMethodData.getPaymentMethodMultiple()
            .getMultiplePaymentMethodComponentDetails()) {
        Payment payment = createPaymentFromComponentDetail(cd, bill, department, webUser, currentDate);
        if (payment != null) {
            paymentFacade.create(payment);
            // Same SideEffectControl applies to all components
            // Tracking prevents duplicate updates
            applySideEffects(payment, sideEffectControl, webUser, cashbook, department);
            payments.add(payment);
        }
    }
}
```

**Testing**:
```java
@Test
public void testMultiplePaymentMethods_AllComponentsProcessed() {
    // Arrange: Create payment data with 3 components
    PaymentMethodData pmd = createMultiplePaymentMethodData(
        createCashComponent(500.0),
        createCardComponent(300.0),
        createChequeComponent(200.0)
    );

    // Act
    List<Payment> payments = paymentService.createPayment(bill, pmd);

    // Assert
    assertEquals(3, payments.size());
    // Each component creates drawer entry
    verify(drawerService, times(3)).updateDrawer(any());
    // But patient deposit only updated once (if applicable)
    verify(patientDepositService, atMost(1)).updateBalance(any(), any());
}
```

---

### Edge Case 2: Cancellation Bills

**Scenario**: Cancelling original bill requires inverted payment values

**Current Behavior**:
- `createPaymentsForCancelling()` clones original payments
- Inverts values (positive becomes negative)
- Creates new Payment entities
- Updates drawer and cashbook with inverted values
- Does NOT call `updateBalances()`

**New Behavior**:
- Same as current by default
- Can now explicitly control whether to reverse balances
- Useful for full cancellation vs. partial cancellation

**Implementation**:
```java
public List<Payment> createPaymentsForCancelling(
        Bill cancellationBill,
        SideEffectControl sideEffectControl) {
    List<Payment> newPayments = new ArrayList<>();
    List<Payment> originalBillPayments = billService.fetchBillPayments(
        cancellationBill.getBilledBill()
    );

    if (originalBillPayments != null) {
        for (Payment originalBillPayment : originalBillPayments) {
            Payment p = originalBillPayment.clonePaymentForNewBill();
            p.invertValues(); // Positive becomes negative
            p.setBill(cancellationBill);
            p.setInstitution(cancellationBill.getInstitution());
            p.setDepartment(cancellationBill.getDepartment());
            p.setCreatedAt(new Date());
            p.setCreater(cancellationBill.getCreater());

            paymentFacade.create(p);
            newPayments.add(p);

            // Apply side effects (with inverted values)
            applySideEffects(p, sideEffectControl);
        }
    }

    return newPayments;
}
```

**Usage Example**:
```java
// Cancel with full reversal (including balances)
SideEffectControl cancelControl = SideEffectControl.allSideEffects();
List<Payment> cancelPayments = paymentService.createPaymentsForCancelling(
    cancellationBill,
    cancelControl
);
```

**Testing**:
```java
@Test
public void testCancellation_InvertsValuesCorrectly() {
    // Arrange
    Bill originalBill = createBillWithPayment(1000.0);
    Bill cancellationBill = createCancellationBill(originalBill);

    // Act
    List<Payment> cancelPayments = paymentService.createPaymentsForCancelling(
        cancellationBill,
        SideEffectControl.allSideEffects()
    );

    // Assert
    assertEquals(1, cancelPayments.size());
    assertEquals(-1000.0, cancelPayments.get(0).getPaidValue(), 0.01);

    // Verify drawer was updated with negative value
    ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
    verify(drawerService).updateDrawer(captor.capture());
    assertEquals(-1000.0, captor.getValue().getPaidValue(), 0.01);
}
```

---

### Edge Case 3: Patient Deposit Insufficient Balance

**Scenario**: Patient deposit payment requested but insufficient balance

**Current Behavior**:
- Validation in controllers before calling `createPayment()`
- `checkForErrorsInPaymentDetailsForInBills()` checks balance
- If insufficient, error message shown, payment not created

**New Behavior**:
- Same validation happens before payment creation
- If validation bypassed, payment creation succeeds
- Balance update will occur (may go negative if allowed)

**Recommendation**:
- Keep validation in controllers
- Add additional validation in service layer
- Throw exception if balance update would violate constraints

**Implementation**:
```java
private void updatePatientDepositIfEnabled(Payment payment, SideEffectControl control) {
    if (control.shouldSkipPatientDepositUpdate()) {
        return;
    }

    if (payment.getPaymentMethod() == PaymentMethod.PatientDeposit) {
        // Pre-validation
        Patient pt = patientFacade.findWithoutCache(payment.getBill().getPatient().getId());
        double newBalance = pt.getRunningBalance() - payment.getPaidValue();
        double creditLimitAbsolute = Math.abs(pt.getCreditLimit());

        if (newBalance < -creditLimitAbsolute) {
            throw new InsufficientDepositException(
                "Patient deposit balance would exceed credit limit. " +
                "Current: " + pt.getRunningBalance() +
                ", Payment: " + payment.getPaidValue() +
                ", Credit Limit: " + creditLimitAbsolute
            );
        }

        updatePatientDeposits(payment);
        control.markPatientDepositUpdated();
    }
}
```

---

### Edge Case 4: Credit Company Limit Exceeded

**Scenario**: Credit payment would exceed company's allowed credit

**Current Behavior**:
- No validation in service layer
- Credit company balance always updated
- May exceed configured limits

**New Behavior**:
- Add optional validation (controlled by config)
- Throw exception if limit exceeded
- Allow override for administrative users

**Implementation**:
```java
private void updateCreditCompanyIfEnabled(Payment payment, SideEffectControl control) {
    if (control.shouldSkipCreditCompanyUpdate()) {
        return;
    }

    if (payment.getPaymentMethod() == PaymentMethod.Credit) {
        Institution creditCompany = payment.getCreditCompany();
        if (creditCompany == null) {
            return;
        }

        Institution institution = institutionFacade.find(creditCompany.getId());
        if (institution == null) {
            return;
        }

        double currentBalance = institution.getAllowedCredit();
        double newBalance = currentBalance + payment.getPaidValue();

        // Optional: Check credit limit
        if (configOptionApplicationController.getBooleanValueByKey(
                "Enforce Credit Company Limits", false)) {
            if (institution.getCreditLimit() != null &&
                newBalance > institution.getCreditLimit()) {
                throw new CreditLimitExceededException(
                    "Credit company limit exceeded. " +
                    "Company: " + institution.getName() +
                    ", Current Balance: " + currentBalance +
                    ", Payment: " + payment.getPaidValue() +
                    ", Limit: " + institution.getCreditLimit()
                );
            }
        }

        institution.setAllowedCredit(newBalance);
        institutionFacade.edit(institution);
        control.markCreditCompanyUpdated();

        LOGGER.log(Level.INFO,
            "Updated credit company balance. Company: {0}, New Balance: {1}",
            new Object[]{institution.getName(), newBalance});
    }
}
```

---

### Edge Case 5: Staff Credit/Welfare Insufficient

**Scenario**: Staff credit/welfare payment exceeds available balance

**Current Behavior**:
- Validation in `checkPaymentMethodError()`
- Checks before payment creation
- Error message if insufficient

**New Behavior**:
- Keep validation in controllers
- Add service-layer validation
- Consistent with other payment methods

**Testing**:
```java
@Test
public void testStaffCredit_InsufficientBalance_ThrowsException() {
    // Arrange
    Staff staff = createStaff();
    staff.setCurrentCreditValue(900.0);
    staff.setCreditLimitQualified(1000.0);

    Bill bill = createBillForStaff(staff, 200.0); // Would exceed limit

    // Act & Assert
    assertThrows(InsufficientCreditException.class, () -> {
        paymentService.createPaymentWithAllSideEffects(bill, paymentMethodData);
    });
}
```

---

### Edge Case 6: Direct Payment Instantiation

**Scenario**: 136 occurrences of `new Payment()` bypass service layer

**Current Behavior**:
- Controllers create Payment directly
- Manually call facade methods
- Manually call side effect services
- Inconsistent patterns

**Migration Strategy**:
```java
// BEFORE (in controller)
Payment payment = new Payment();
payment.setBill(bill);
payment.setInstitution(bill.getInstitution());
payment.setDepartment(bill.getDepartment());
payment.setCreatedAt(new Date());
payment.setCreater(sessionController.getLoggedUser());
payment.setPaymentMethod(PaymentMethod.Cash);
payment.setPaidValue(bill.getNetTotal());
paymentFacade.create(payment);
drawerService.updateDrawer(payment);
cashbookService.writeCashBookEntryAtPaymentCreation(payment);

// AFTER (using service)
PaymentMethodData pmd = new PaymentMethodData();
pmd.setCash(new CashComponent(bill.getNetTotal()));
List<Payment> payments = paymentService.createPayment(bill, pmd);
```

**Gradual Migration**:
1. Identify all `new Payment()` occurrences
2. Group by pattern (some include side effects, some don't)
3. Create PaymentMethodData from existing code
4. Replace with service call
5. Remove manual side effect calls
6. Test thoroughly

---

### Edge Case 7: Payment Without Bill

**Scenario**: Some Payment entities created without associated Bill

**Current Behavior**:
- Rare but exists in some controllers
- Used for internal transfers or adjustments
- No bill validation

**New Behavior**:
- Add support for bill-less payments
- Require explicit flag or separate method
- Document use cases

**Implementation**:
```java
/**
 * Creates a payment without associated bill (for internal transfers)
 *
 * @param paymentMethod The payment method
 * @param paidValue The payment amount
 * @param department The department
 * @param webUser The user creating the payment
 * @param sideEffectControl Side effect control
 * @return Created payment
 */
public Payment createPaymentWithoutBill(
        PaymentMethod paymentMethod,
        double paidValue,
        Department department,
        WebUser webUser,
        SideEffectControl sideEffectControl) {

    Payment payment = new Payment();
    payment.setPaymentMethod(paymentMethod);
    payment.setPaidValue(paidValue);
    payment.setInstitution(department.getInstitution());
    payment.setDepartment(department);
    payment.setCreatedAt(new Date());
    payment.setCreater(webUser);

    paymentFacade.create(payment);
    applySideEffects(payment, sideEffectControl, webUser, null, department);

    return payment;
}
```

---

## Summary

This design provides a comprehensive solution for adding granular side effect control to PaymentService while maintaining 100% backward compatibility. The key features are:

1. **SideEffectControl Class**: Encapsulates all Boolean flags with factory methods for common configurations

2. **Backward Compatible Methods**: All existing method calls work identically through delegation to new internal methods

3. **New Overloaded Methods**: Provide explicit control over side effects for new code

4. **Idempotency Protection**: Tracking flags prevent duplicate side effect application

5. **Transaction Management**: Recommendations for ensuring atomicity

6. **Comprehensive Testing**: Unit, integration, and manual testing strategies

7. **Gradual Migration**: Phased approach starting with high-value controllers

8. **Edge Case Handling**: Solutions for complex scenarios like multiple payment methods, cancellations, and insufficient balances

The design is ready for implementation and can be executed by any developer with this document as a guide.

---

## File Locations

- **Implementation File**: `/home/buddhika/development/rh/src/main/java/com/divudi/service/PaymentService.java`
- **New Class**: `/home/buddhika/development/rh/src/main/java/com/divudi/service/SideEffectControl.java`
- **Unit Tests**: `/home/buddhika/development/rh/src/test/java/com/divudi/service/PaymentServiceTest.java`
- **This Document**: `/home/buddhika/development/rh/developer_docs/architecture/payment-service-side-effect-control-design.md`

---

**Document Version**: 1.0
**Date**: 2025-10-16
**Author**: Claude (Senior Health Informatics Architect)
**Status**: Ready for Implementation
