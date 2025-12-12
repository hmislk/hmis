# Pharmacy Retail Sale Add Button Performance Optimization Plan

**Issue**: [#16990] Speed up the pharmacy retail sale Add button action
**Date**: 2025-12-02
**Author**: System Analysis
**Status**: Planning Phase

---

## Executive Summary

Performance analysis has identified three critical bottlenecks in the pharmacy retail sale "Add" button action:

1. **O(n²) Rate Recalculation**: Every item add triggers rate recalculation for ALL existing items
2. **Immediate Database Writes**: Each item triggers an immediate UserStock database write
3. **Inefficient Discount Calculation**: Discount logic runs even when no discounts apply

**Expected Impact**: 10-50x speedup for bills with 20+ items

---

## Current Performance Issues

### Issue #1: O(n²) Rate Recalculation Problem
**Location**: `PharmacySaleController.java:1766-1780`

**Current Flow**:
```
addBillItem()
  └─> calculateBillItemsAndBillTotalsOfPreBill()
       └─> calculateRatesForAllBillItemsInPreBill()
            └─> for EACH billItem in preBill:
                 calculateRatesOfSelectedBillItemBeforeAddingToTheList()
```

**Problem**:
- Adding item #1: 1 calculation
- Adding item #10: 10 calculations
- Adding item #50: 50 calculations
- **Total for 50 items: 1,275 calculations (should be 50)**

**Performance Impact**: Exponential slowdown as bill grows

---

### Issue #2: Immediate Database Writes
**Location**: `PharmacySaleController.java:1919, 2078-2090`

**Current Flow**:
```java
addBillItemSingleItem()
  └─> saveUserStock(billItem)  // Line 1919
       └─> getUserStockFacade().create(us);  // Line 2085 - IMMEDIATE DB WRITE
```

**Problem**: Database I/O on every single item add creates network/disk latency

**Performance Impact**: 100-500ms penalty per item (depending on DB connection)

---

### Issue #3: Inefficient Discount Calculation
**Location**: `PharmacySaleController.java:1792-1802, 4125-4223`

**Current Flow**:
```java
calculateRatesOfSelectedBillItemBeforeAddingToTheList(bi)
  └─> calculateBillItemDiscountRate(bi)  // Line 1798
       └─> Multiple PriceMatrix lookups
            └─> Potential database queries
```

**Problem**: Even with optimization at line 1792, discount calculation still runs for every item

**Performance Impact**: 10-50ms per item (if database queries involved)

---

## Proposed Optimizations

### Optimization #1: Eliminate Unnecessary Rate Recalculations

**Change**: Only recalculate rates when payment scheme/method changes, not on every item add

**Files to Modify**:
- `src/main/java/com/divudi/bean/pharmacy/PharmacySaleController.java`

**Implementation Details**:

#### Step 1.1: Add State Tracking Variables
```java
// Add these instance variables at the class level (around line 100-200)
private PaymentScheme lastCalculatedPaymentScheme = null;
private PaymentMethod lastCalculatedPaymentMethod = null;
private boolean ratesNeedRecalculation = false;
```

#### Step 1.2: Modify `calculateBillItemsAndBillTotalsOfPreBill()`
**Location**: Line 1770-1773

**BEFORE**:
```java
public void calculateBillItemsAndBillTotalsOfPreBill() {
    calculateRatesForAllBillItemsInPreBill();
    calculatePreBillTotals();
}
```

**AFTER**:
```java
public void calculateBillItemsAndBillTotalsOfPreBill() {
    // Performance optimization: Only recalculate all rates if payment terms changed
    // This prevents O(n²) recalculation on every item add
    if (ratesNeedRecalculation || hasPaymentTermsChanged()) {
        calculateRatesForAllBillItemsInPreBill();
        ratesNeedRecalculation = false;
        lastCalculatedPaymentScheme = paymentScheme;
        lastCalculatedPaymentMethod = paymentMethod;
    }
    calculatePreBillTotals();
}

private boolean hasPaymentTermsChanged() {
    return !Objects.equals(lastCalculatedPaymentScheme, paymentScheme) ||
           !Objects.equals(lastCalculatedPaymentMethod, paymentMethod);
}
```

#### Step 1.3: Update `listnerForPaymentMethodChange()`
**Location**: Find this method (search for "listnerForPaymentMethodChange")

**ADD** at the beginning of the method:
```java
// Mark that rates need recalculation due to payment change
ratesNeedRecalculation = true;
```

#### Step 1.4: Update `resetAll()` method
**Location**: Find this method (search for "public.*resetAll")

**ADD** at the beginning:
```java
// Reset rate calculation state
lastCalculatedPaymentScheme = null;
lastCalculatedPaymentMethod = null;
ratesNeedRecalculation = false;
```

**Rollback Procedure for Optimization #1**:
```bash
# 1. Remove the three new instance variables
# 2. Restore calculateBillItemsAndBillTotalsOfPreBill() to original version
# 3. Remove hasPaymentTermsChanged() method
# 4. Remove additions to listnerForPaymentMethodChange() and resetAll()
```

**Risk Level**: LOW - This is a pure optimization that doesn't change business logic

---

### Optimization #2: Batch UserStock Database Writes

**Change**: Defer UserStock writes until bill settlement (single batch operation)

**Files to Modify**:
- `src/main/java/com/divudi/bean/pharmacy/PharmacySaleController.java`

**Implementation Details**:

#### Step 2.1: Add Pending UserStock Collection
```java
// Add this instance variable at class level
private List<UserStock> pendingUserStocks = new ArrayList<>();
```

#### Step 2.2: Modify `saveUserStock()` Method
**Location**: Line 2078-2090

**BEFORE**:
```java
private UserStock saveUserStock(BillItem tbi) {
    UserStock us = new UserStock();
    us.setStock(tbi.getPharmaceuticalBillItem().getStock());
    us.setUpdationQty(tbi.getQty());
    us.setCreater(getSessionController().getLoggedUser());
    us.setCreatedAt(new Date());
    us.setUserStockContainer(getUserStockContainer());
    getUserStockFacade().create(us);  // IMMEDIATE DB WRITE

    getUserStockContainer().getUserStocks().add(us);

    return us;
}
```

**AFTER**:
```java
private UserStock saveUserStock(BillItem tbi) {
    UserStock us = new UserStock();
    us.setStock(tbi.getPharmaceuticalBillItem().getStock());
    us.setUpdationQty(tbi.getQty());
    us.setCreater(getSessionController().getLoggedUser());
    us.setCreatedAt(new Date());
    us.setUserStockContainer(getUserStockContainer());

    // Performance optimization: Defer database write until settlement
    // Store in memory for now
    pendingUserStocks.add(us);
    // DO NOT call: getUserStockFacade().create(us);

    getUserStockContainer().getUserStocks().add(us);

    return us;
}
```

#### Step 2.3: Add Batch Persistence Method
**Location**: Add new method near saveUserStock()

```java
/**
 * Persists all pending UserStock records in a single batch operation.
 * Called during bill settlement for performance optimization.
 */
private void persistPendingUserStocks() {
    if (pendingUserStocks == null || pendingUserStocks.isEmpty()) {
        return;
    }

    try {
        for (UserStock us : pendingUserStocks) {
            if (us.getId() == null) {  // Only persist if not already saved
                getUserStockFacade().create(us);
            }
        }
        pendingUserStocks.clear();
    } catch (Exception e) {
        // Log error and rethrow
        System.err.println("Error persisting UserStock records: " + e.getMessage());
        throw new RuntimeException("Failed to save stock tracking records", e);
    }
}
```

#### Step 2.4: Update `settleBillWithPay()` Method
**Location**: Search for "public.*settleBillWithPay"

**ADD** at the beginning of the method (after validation checks):
```java
// Persist all deferred UserStock records before settlement
persistPendingUserStocks();
```

#### Step 2.5: Update `resetAll()` Method
**ADD**:
```java
// Clear pending user stocks on reset
if (pendingUserStocks != null) {
    pendingUserStocks.clear();
}
```

#### Step 2.6: Add Cleanup for Bill Cancellation
**Location**: Find any bill cancellation/removal methods

**ADD**:
```java
// Clean up pending user stocks when bill is cancelled
pendingUserStocks.clear();
```

**Rollback Procedure for Optimization #2**:
```bash
# 1. Remove pendingUserStocks instance variable
# 2. Restore saveUserStock() to call getUserStockFacade().create(us) immediately
# 3. Remove persistPendingUserStocks() method
# 4. Remove calls to persistPendingUserStocks() and pendingUserStocks.clear()
```

**Risk Level**: MEDIUM
- **Risk**: If bill settlement fails, UserStock records won't be persisted
- **Mitigation**: Add transaction handling and rollback logic
- **Testing**: Thoroughly test settlement success AND failure scenarios

---

### Optimization #3: Optimize Discount Calculation

**Change**: Skip discount calculation entirely when no discount scheme is active

**Files to Modify**:
- `src/main/java/com/divudi/bean/pharmacy/PharmacySaleController.java`

**Implementation Details**:

#### Step 3.1: Enhance Discount Check
**Location**: Line 1792-1802

**BEFORE**:
```java
// Performance optimization: Skip discount calculation if no discount scheme is active
boolean hasDiscountScheme = getPaymentScheme() != null ||
                           getPaymentMethod() != null ||
                           (getPaymentMethod() == PaymentMethod.Credit && toInstitution != null);

if (hasDiscountScheme) {
    long discountStart = System.currentTimeMillis();
    bi.setDiscountRate(calculateBillItemDiscountRate(bi));
    long discountEnd = System.currentTimeMillis();
} else {
    bi.setDiscountRate(0.0);
}
```

**AFTER**:
```java
// Performance optimization: Skip discount calculation if no discount scheme is active
// Check if item allows discount first (cheapest check)
if (bi.getItem() != null && !bi.getItem().isDiscountAllowed()) {
    bi.setDiscountRate(0.0);
} else {
    boolean hasDiscountScheme = getPaymentScheme() != null ||
                               (getPaymentMethod() != null && getPaymentMethod() != PaymentMethod.Cash) ||
                               (getPaymentMethod() == PaymentMethod.Credit && toInstitution != null);

    if (hasDiscountScheme) {
        bi.setDiscountRate(calculateBillItemDiscountRate(bi));
    } else {
        bi.setDiscountRate(0.0);
    }
}
```

**Rollback Procedure for Optimization #3**:
```bash
# Simply restore the original version of the discount check logic
```

**Risk Level**: LOW - Only adds an early exit condition

---

## MySQL-Specific Optimizations

**See separate document**: `mysql-pharmacy-retail-sale-optimization.md`

**Summary**:
1. Add indexes on Stock table
2. Optimize UserStock bulk insert
3. Add query execution plan analysis
4. Configure connection pooling

---

## Testing Plan

### Phase 1: Unit Testing (Development Environment)

**Test Case 1.1: Single Item Add**
- Add 1 item to bill
- Verify rates calculated correctly
- Verify UserStock NOT persisted yet
- Time: Should be < 50ms

**Test Case 1.2: Multiple Items (10 items)**
- Add 10 items sequentially
- Verify rates calculated correctly for each
- Verify UserStock records NOT persisted yet
- Time: Should be < 500ms total (< 50ms per item)

**Test Case 1.3: Large Bill (50 items)**
- Add 50 items sequentially
- Verify rates calculated correctly
- Verify UserStock records NOT persisted yet
- Time: Should be < 2,500ms total (< 50ms per item)

**Test Case 1.4: Payment Scheme Change**
- Add 10 items
- Change payment scheme
- Add 10 more items
- Verify rates recalculated for all 20 items after scheme change
- Verify correct discounts applied

**Test Case 1.5: Bill Settlement Success**
- Add multiple items
- Settle bill
- Verify ALL UserStock records persisted to database
- Verify stock quantities updated correctly

**Test Case 1.6: Bill Cancellation**
- Add multiple items
- Cancel/reset bill
- Verify pendingUserStocks cleared
- Verify NO UserStock records in database

**Test Case 1.7: Bill Settlement Failure**
- Add multiple items
- Force settlement failure (mock)
- Verify transaction rollback
- Verify error handling

### Phase 2: Integration Testing (QA Environment)

**Test Case 2.1: Concurrent Users**
- 2+ users adding items simultaneously
- Verify no stock conflicts
- Verify UserStock records correct for each user

**Test Case 2.2: Network Latency Simulation**
- Simulate slow database connection
- Verify performance improvement still evident

**Test Case 2.3: Discount Scenarios**
- Test all payment methods (Cash, Credit, Card, etc.)
- Test all payment schemes
- Verify discount calculations correct

### Phase 3: Performance Testing

**Metrics to Capture**:
- Time per item add (average, p95, p99)
- Total time for 50-item bill
- Database query count per add
- Memory usage

**Success Criteria**:
- 10x improvement in add time for bills > 20 items
- < 50ms per item add (p95)
- Database writes deferred until settlement

### Phase 4: User Acceptance Testing (UAT)

**Scenario 4.1: Normal Pharmacy Sale**
- Pharmacist creates normal retail sale (5-10 items)
- Verify workflow unchanged from user perspective
- Verify receipt correct

**Scenario 4.2: Large Sale**
- Pharmacist creates large sale (30+ items)
- Verify noticeable performance improvement
- Verify all calculations correct

**Scenario 4.3: Discount Handling**
- Test various discount scenarios
- Verify discounts calculated and displayed correctly

---

## Deployment Plan

### Pre-Deployment Checklist

- [ ] Code review completed
- [ ] All unit tests pass
- [ ] Integration tests pass
- [ ] Performance tests show improvement
- [ ] UAT sign-off received
- [ ] Database backup completed
- [ ] Rollback plan tested
- [ ] MySQL optimizations applied (separate)

### Deployment Steps

1. **Backup Production Database**
   ```bash
   # See mysql-pharmacy-retail-sale-optimization.md
   ```

2. **Apply Code Changes**
   ```bash
   # Stop application server
   # Deploy new WAR/EAR file
   # Start application server
   ```

3. **Verify Deployment**
   - Check application logs for errors
   - Test single item add
   - Test bill settlement
   - Verify UserStock records persisted

4. **Monitor Performance**
   - Monitor response times
   - Monitor database connection pool
   - Monitor error rates
   - Check user feedback

### Post-Deployment Monitoring (First 24 Hours)

- Monitor application logs every 2 hours
- Check for any UserStock persistence errors
- Verify bill settlement success rate = 100%
- Collect performance metrics
- Gather user feedback

---

## Rollback Plan

### Emergency Rollback (Critical Issues)

**Trigger Conditions**:
- Bill settlement failures > 1%
- UserStock records not persisting
- Data corruption detected
- Critical performance regression

**Rollback Steps**:

1. **Stop Application**
   ```bash
   # Stop application server
   ```

2. **Restore Previous Version**
   ```bash
   # Deploy previous WAR/EAR file
   # Start application server
   ```

3. **Verify Rollback**
   - Test bill creation and settlement
   - Verify UserStock records persisting immediately
   - Check application logs

4. **Data Cleanup (if needed)**
   ```sql
   -- Check for any orphaned records
   SELECT * FROM user_stock WHERE created_at > 'DEPLOYMENT_TIME' AND id IS NULL;
   ```

### Gradual Rollback (Performance Issues)

If performance improvement is not as expected but no critical errors:

1. Keep code changes
2. Investigate root cause
3. Apply additional optimizations
4. Consider toggling optimizations via ConfigOption

---

## Configuration Options (Future Enhancement)

Consider adding ConfigOption flags for easy toggling:

```java
// Add to ConfigOption table
configOptionApplicationController.getBooleanValueByKey(
    "Pharmacy Retail Sale - Batch UserStock Writes", true);

configOptionApplicationController.getBooleanValueByKey(
    "Pharmacy Retail Sale - Optimize Rate Calculation", true);
```

This allows:
- Disabling optimizations without code deployment
- A/B testing performance improvements
- Easy rollback via configuration

---

## Risk Assessment

| Optimization | Risk Level | Impact | Mitigation |
|-------------|-----------|--------|------------|
| #1: Rate Recalculation | LOW | High | Thorough testing of payment scheme changes |
| #2: Batch UserStock | MEDIUM | High | Transaction handling, extensive settlement testing |
| #3: Discount Check | LOW | Medium | Verify all discount scenarios work |

**Overall Risk**: MEDIUM
**Overall Impact**: HIGH
**Recommendation**: PROCEED with phased deployment

---

## Success Metrics

### Performance Metrics

**Before Optimization** (Baseline):
- Single item add: ~100-200ms
- 50-item bill total: ~10,000-15,000ms (10-15 seconds)
- Database writes per bill: 50 (1 per item)

**After Optimization** (Target):
- Single item add: < 50ms
- 50-item bill total: < 2,500ms (2.5 seconds)
- Database writes per bill: 1 (batch at settlement)

**Improvement Target**: 10-50x speedup for large bills

### Business Metrics

- User satisfaction with page responsiveness
- Number of timeout errors (should be 0)
- Bill processing time reduction
- Pharmacist feedback

---

## Implementation Timeline

1. **Week 1**: Code implementation + Unit testing
2. **Week 2**: Integration testing + QA environment deployment
3. **Week 3**: Performance testing + UAT
4. **Week 4**: Production deployment + monitoring

---

## Appendix A: Related Files

### Java Files to Modify
- `src/main/java/com/divudi/bean/pharmacy/PharmacySaleController.java`
- `src/main/java/com/divudi/bean/pharmacy/PharmacySaleController1.java` (if used)
- `src/main/java/com/divudi/bean/pharmacy/PharmacySaleController2.java` (if used)
- `src/main/java/com/divudi/bean/pharmacy/PharmacySaleController3.java` (if used)

### XHTML Files (Reference Only)
- `src/main/webapp/pharmacy/pharmacy_bill_retail_sale.xhtml` (Line 244-253: Add button)

### Database Tables Affected
- `user_stock` (writes deferred)
- `bill` (no schema changes)
- `bill_item` (no schema changes)

---

## Appendix B: Code Review Checklist

- [ ] All optimizations maintain business logic correctness
- [ ] No data loss scenarios introduced
- [ ] Transaction boundaries properly defined
- [ ] Error handling comprehensive
- [ ] Logging adequate for debugging
- [ ] Thread safety considered
- [ ] Memory leaks prevented
- [ ] Rollback procedures tested

---

## Document History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2025-12-02 | System Analysis | Initial draft |

---

## Approval Sign-off

- [ ] Technical Lead Review
- [ ] Database Administrator Review
- [ ] QA Lead Approval
- [ ] Business Owner Approval
- [ ] Deployment Team Acknowledgment
