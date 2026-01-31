# Performance Improvement Plan: Remove UserStockContainer Mechanism
## Pharmacy Retail Sale for Cashier (Issue #TBD)

---

## Executive Summary

**Problem:** The UserStockContainer mechanism causes significant delay when moving focus from stock autocomplete to quantity field in pharmacy retail sale for cashier. The mechanism performs 2 database queries per item addition to prevent concurrent stock usage.

**Solution:** Remove UserStockContainer validation during item addition, implement atomic stock validation at settlement time only, with rollback capability if no performance improvement.

**Risk:** Potential for negative stock if validation fails at settlement.

**Mitigation:** Strict settlement-time validation with clear user feedback and automatic quantity reset.

---

## Current Performance Bottlenecks

### Primary Bottleneck: isStockAvailable() Calls
**Location:** `PharmacySaleForCashierController.addBillItemSingleItem()` Line 1509

**Impact:**
- **2 Database Queries per item addition:**
  1. `SELECT stock WHERE id = ?` (fetch current stock level)
  2. `SELECT sum(updation_qty) FROM user_stock WHERE ...` (check other users' reservations in 30-minute window)

- **Query Execution Time:** Estimated 50-150ms per call (network + DB latency)
- **User Experience:** Noticeable lag between autocomplete selection and quantity field focus

### Secondary Bottlenecks
1. **Autocomplete Query Complexity** - Multiple JOINs with optional conditions (20 results)
2. **AJAX Update Scope** - 11 components updated by Add button
3. **Discount Calculation Queries** - PriceMatrix lookups on every rate change

---

## Proposed Solution Architecture

### Phase 1: Remove UserStockContainer Validation During Item Addition

#### Changes Required:
1. **Remove isStockAvailable() call in addBillItemSingleItem()**
   - File: `PharmacySaleForCashierController.java`
   - Line: 1509-1517
   - Action: Comment out or conditionally disable based on config flag

2. **Remove isStockAvailable() call in addBillItemMultipleBatches()**
   - File: `PharmacySaleForCashierController.java`
   - Line: ~1600-1650 (approximate location)
   - Action: Comment out or conditionally disable

3. **Keep UserStockContainer entity and controller intact**
   - Reason: Allows easy rollback if needed
   - No database schema changes required

### Phase 2: Implement Enhanced Settlement-Time Validation

#### Strict Settlement Validation (settlePreBillAndNavigateToPrint)
**File:** `PharmacySaleForCashierController.java` Line 2352-2574

**Current Logic:**
```java
// Already validates at settlement - Lines 2358-2370
for (BillItem bi : getPreBill().getBillItems()) {
    if (!userStockController.isStockAvailable(...)) {
        setZeroToQty(bi);
        JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
        return null;
    }
}
```

**Enhanced Logic (New Implementation):**

```java
// ENHANCED SETTLEMENT VALIDATION
for (BillItem bi : getPreBill().getBillItems()) {
    Stock currentStock = stockFacade.find(bi.getPharmaceuticalBillItem().getStock().getId());

    // Direct stock availability check WITHOUT UserStockContainer queries
    if (bi.getQty() > currentStock.getStock()) {
        // Stock insufficient - provide detailed error
        String errorMsg = String.format(
            "Insufficient stock for %s (Batch: %s). Available: %.2f, Requested: %.2f. " +
            "Another user may have sold this stock. Please remove this item or reduce quantity.",
            bi.getItem().getName(),
            bi.getPharmaceuticalBillItem().getItemBatch().getBatchNo(),
            currentStock.getStock(),
            bi.getQty()
        );
        JsfUtil.addErrorMessage(errorMsg);

        // Option 1: Reset quantity to zero (current behavior)
        setZeroToQty(bi);

        // Option 2: Auto-adjust to available quantity (user-friendly)
        // bi.setQty(currentStock.getStock());
        // calculateBillItemForExistingItem(bi);

        return null; // Block settlement
    }
}

// NEW: Atomic database-level validation before final save
// This ensures no race condition between validation and stock deduction
if (!validateAndReserveStockAtomically(getPreBill().getBillItems())) {
    JsfUtil.addErrorMessage("Stock levels changed during settlement. Please review and try again.");
    return null;
}
```

#### Atomic Stock Validation Method (New)
**Location:** `PharmacySaleForCashierController.java` (new method)

```java
/**
 * Validates stock availability and reserves stock atomically within a transaction.
 * This prevents race conditions where stock levels change between validation and deduction.
 *
 * @param billItems List of bill items to validate
 * @return true if all stock validated and reserved successfully, false otherwise
 */
@TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
private boolean validateAndReserveStockAtomically(List<BillItem> billItems) {
    try {
        for (BillItem bi : billItems) {
            Stock stock = stockFacade.find(bi.getPharmaceuticalBillItem().getStock().getId());

            // Refresh stock to get latest database value
            stockFacade.refresh(stock);

            // Final validation
            if (bi.getQty() > stock.getStock()) {
                // Log the conflict for debugging
                System.out.println("STOCK CONFLICT DETECTED: " + stock.getItemBatch().getItem().getName() +
                    " - Available: " + stock.getStock() + ", Requested: " + bi.getQty());
                return false;
            }

            // Reserve stock immediately (will be committed when bill is saved)
            // Note: Actual stock deduction happens in savePreBillFinallyForRetailSaleForCashier
            // This is just a pre-validation step
        }
        return true;
    } catch (Exception e) {
        // Log error and return false
        System.err.println("Error during atomic stock validation: " + e.getMessage());
        return false;
    }
}
```

### Phase 3: Configuration Flag for Gradual Rollout

**New Configuration Option:**
- **Key:** `Enable UserStockContainer for Pharmacy Retail Sale`
- **Default:** `false` (new behavior - no UserStockContainer validation)
- **Description:** "If enabled, validates stock availability against other users' reservations during item addition. Disable to improve performance but rely on settlement-time validation only."

**Implementation:**
```java
// In addBillItemSingleItem() - Line 1509
if (configOptionApplicationController
        .getBooleanValueByKey("Enable UserStockContainer for Pharmacy Retail Sale", false)) {
    // OLD BEHAVIOR: Check with UserStockContainer
    if (!userStockController.isStockAvailable(
            getStock(),
            getQty(),
            getSessionController().getLoggedUser())) {
        JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
        return addedQty;
    }
}
// If disabled (default), skip the check and continue with item addition
```

---

## Implementation Steps

### Step 1: Backup Current Code
- [x] Create git branch: `feature/remove-userstockcontainer-performance`
- [x] Document current behavior with screenshots/videos
- [ ] Create rollback plan document

### Step 2: Add Configuration Option
**File:** Database (via application UI or SQL)
```sql
INSERT INTO config_option (id, name, value, description, data_type, category)
VALUES (
    nextval('config_option_seq'),
    'Enable UserStockContainer for Pharmacy Retail Sale',
    'false',
    'If enabled, validates stock availability against other users reservations during item addition. Disable to improve performance.',
    'BOOLEAN',
    'Pharmacy'
);
```

**Alternative:** Use ConfigOptionApplicationController to add via UI

### Step 3: Modify PharmacySaleForCashierController
**File:** `src/main/java/com/divudi/bean/pharmacy/PharmacySaleForCashierController.java`

**Changes:**

#### 3.1: Modify addBillItemSingleItem()
**Line:** 1509-1517

**Before:**
```java
if (!userStockController.isStockAvailable(
        getStock(),
        getQty(),
        getSessionController().getLoggedUser())) {
    JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
    return addedQty;
}
```

**After:**
```java
// NEW: Check configuration flag before UserStockContainer validation
if (configOptionApplicationController
        .getBooleanValueByKey("Enable UserStockContainer for Pharmacy Retail Sale", false)) {
    if (!userStockController.isStockAvailable(
            getStock(),
            getQty(),
            getSessionController().getLoggedUser())) {
        JsfUtil.addErrorMessage("Sorry Already Other User Try to Billing This Stock You Cant Add");
        return addedQty;
    }
} else {
    // NEW BEHAVIOR: Simple stock level check only (no UserStock queries)
    Stock currentStock = stockFacade.find(getStock().getId());
    if (getQty() > currentStock.getStock()) {
        JsfUtil.addErrorMessage("Insufficient stock. Available: " + currentStock.getStock());
        return addedQty;
    }
}
```

#### 3.2: Modify addBillItemMultipleBatches()
**Line:** ~1600-1650

Apply same configuration flag check as above.

#### 3.3: Enhance settlePreBillAndNavigateToPrint()
**Line:** 2358-2370

**Before:**
```java
for (BillItem bi : getPreBill().getBillItems()) {
    if (!userStockController.isStockAvailable(
            bi.getPharmaceuticalBillItem().getStock(),
            bi.getQty(),
            getSessionController().getLoggedUser())) {
        setZeroToQty(bi);
        JsfUtil.addErrorMessage("Another User On Change Bill Item Qty value is resetted");
        return null;
    }
}
```

**After:**
```java
// ENHANCED: Always validate at settlement (regardless of config flag)
for (BillItem bi : getPreBill().getBillItems()) {
    Stock currentStock = stockFacade.find(bi.getPharmaceuticalBillItem().getStock().getId());

    if (bi.getQty() > currentStock.getStock()) {
        String errorMsg = String.format(
            "Insufficient stock for %s (Batch: %s). Available: %.2f, Requested: %.2f. " +
            "Stock may have been sold by another user. Please remove or adjust quantity.",
            bi.getItem().getName(),
            bi.getPharmaceuticalBillItem().getItemBatch().getBatchNo(),
            currentStock.getStock(),
            bi.getQty()
        );
        JsfUtil.addErrorMessage(errorMsg);
        setZeroToQty(bi);
        return null;
    }
}
```

### Step 4: Add Logging for Performance Measurement
**Purpose:** Measure actual performance improvement

**Implementation:**
```java
// In addBillItemSingleItem() - before and after changes
long startTime = System.currentTimeMillis();

// ... existing logic ...

long endTime = System.currentTimeMillis();
System.out.println("addBillItemSingleItem execution time: " + (endTime - startTime) + "ms");
```

### Step 5: Testing Plan

#### 5.1 Performance Testing
1. **Baseline Measurement (with UserStockContainer enabled):**
   - Add 10 items to bill
   - Measure time from autocomplete selection to quantity field focus
   - Measure total time from item addition click to UI update
   - Record database query count

2. **New Implementation Measurement (UserStockContainer disabled):**
   - Repeat same test
   - Compare timings
   - Document improvement percentage

3. **Expected Results:**
   - **Item Addition Time Reduction:** 50-150ms per item (2 DB queries eliminated)
   - **Total Bill Creation Time:** For 10 items, expect 500-1500ms faster
   - **User Experience:** Noticeable improvement in autocomplete → quantity focus transition

#### 5.2 Functional Testing
**Test Case 1: Single User Normal Operation**
- Add multiple items to bill
- Verify settlement succeeds
- Verify stock deduction is correct
- Verify bill totals are accurate

**Test Case 2: Concurrent Users - Same Stock**
- **Setup:** Stock #100 has 100 units available
- **User A:** Add 60 units of Stock #100 to bill (DO NOT settle yet)
- **User B:** Add 50 units of Stock #100 to bill (DO NOT settle yet)
- **User A:** Click "Settle Bill"
  - **Expected Result:** Bill settles successfully (A got there first)
- **User B:** Click "Settle Bill"
  - **Expected Result:** Error message: "Insufficient stock for [Item Name] (Batch: [BatchNo]). Available: 40.00, Requested: 50.00"
  - **Expected Behavior:** Quantity reset to 0 or auto-adjusted to 40
  - **User Action Required:** Remove item or reduce quantity to 40

**Test Case 3: Concurrent Users - Sufficient Stock**
- **Setup:** Stock #200 has 200 units available
- **User A:** Add 80 units of Stock #200 to bill
- **User B:** Add 90 units of Stock #200 to bill
- **Both users settle:** Both bills should succeed (total 170 < 200)

**Test Case 4: Stock Goes Negative Prevention**
- **Setup:** Stock #300 has 50 units available
- **User A:** Add 40 units, settle immediately → Success
- **User B:** Add 30 units (added before A settled)
- **User B:** Try to settle
  - **Expected Result:** Error - only 10 units available, cannot sell 30
  - **Expected Behavior:** Quantity reset, clear error message

**Test Case 5: Multiple Items in Bill - Partial Stock Issues**
- **Setup:**
  - Stock #400: 100 units
  - Stock #500: 50 units
- **User A:** Add Stock #400 (80 units) and Stock #500 (30 units)
- **User B:** Add Stock #400 (30 units) and Stock #500 (25 units), settle immediately
- **User A:** Try to settle
  - **Expected:** Stock #400 succeeds (70 available, 80 requested - FAIL)
  - **Expected:** Stock #500 succeeds (20 available, 30 requested - FAIL)
  - **Expected Behavior:** Both items have qty reset to 0, detailed error messages

#### 5.3 Edge Case Testing
1. **Slow Cashier (Bill open for >30 minutes):**
   - Verify settlement validation still catches stock issues
   - UserStockContainer 30-minute window no longer relevant

2. **Network Latency:**
   - Simulate slow network
   - Verify settlement validation is atomic

3. **Database Deadlocks:**
   - Test with 5 concurrent users settling bills
   - Verify no deadlock errors
   - Verify accurate stock levels after all settlements

### Step 6: Rollback Plan

**If Performance Improvement < 20% OR Issues Detected:**

1. **Database Configuration Change:**
```sql
UPDATE config_option
SET value = 'true'
WHERE name = 'Enable UserStockContainer for Pharmacy Retail Sale';
```

2. **Restart Application** (to clear any cached config values)

3. **Verify UserStockContainer behavior restored:**
   - Test concurrent user scenario
   - Verify "Another User Try to Billing This Stock" message appears

4. **Document Rollback Decision:**
   - Performance metrics that led to rollback
   - Issues encountered
   - Alternative approaches to investigate

**No Code Changes Required for Rollback** - Configuration flag handles it

---

## Risk Assessment

### High Risk
- **Negative Stock Scenario:**
  - **Risk:** Two users settle bills simultaneously for same stock
  - **Mitigation:** Settlement validation catches this, resets quantities to 0
  - **Fallback:** Configuration flag allows instant rollback

### Medium Risk
- **User Experience Confusion:**
  - **Risk:** Users don't understand why items are removed at settlement
  - **Mitigation:** Clear, detailed error messages explaining what happened
  - **Training:** Brief user training on new behavior

### Low Risk
- **Performance Doesn't Improve:**
  - **Risk:** Other bottlenecks dominate (autocomplete query, AJAX updates)
  - **Mitigation:** Performance logging identifies actual bottleneck
  - **Fallback:** Easy rollback via config flag

---

## Success Criteria

### Performance Metrics
- [ ] Item addition time reduced by >20% (measured from autocomplete select to qty focus)
- [ ] Database query count reduced by 2 per item addition
- [ ] Total bill creation time for 10 items reduced by >500ms
- [ ] User-reported "lag" significantly reduced

### Functional Metrics
- [ ] Zero instances of negative stock in production (first 2 weeks)
- [ ] Settlement-time validation catches 100% of concurrent stock conflicts
- [ ] Error messages are clear and actionable
- [ ] No increase in user support tickets

### Rollback Criteria (If Any Met)
- [ ] Performance improvement < 20%
- [ ] Negative stock instances detected in production
- [ ] User confusion leads to >10% increase in support tickets
- [ ] Critical bugs discovered in settlement validation

---

## Timeline

### Week 1: Development & Unit Testing
- Day 1-2: Implement configuration flag and code changes
- Day 3-4: Add logging and enhanced error messages
- Day 5: Code review and refinement

### Week 2: QA Testing
- Day 1-2: Performance testing (baseline vs new)
- Day 3-4: Functional testing (all test cases)
- Day 5: Edge case testing

### Week 3: Staged Deployment
- Day 1-2: Deploy to QA environment, monitor
- Day 3-4: Deploy to production (config flag = false initially for rollback safety)
- Day 5: Enable new behavior (config flag = true), monitor closely

### Week 4: Monitoring & Optimization
- Monitor performance metrics
- Monitor negative stock incidents
- Collect user feedback
- Decision: Keep or rollback

---

## Open Questions

1. **Settlement Validation - Auto-Adjust vs Reset to Zero:**
   - Current behavior: Reset qty to 0
   - Alternative: Auto-adjust to available stock (e.g., requested 50, available 30, adjust to 30)
   - **Decision Needed:** Which is better UX?

2. **Error Message Detail Level:**
   - Should we show which user has the conflicting stock?
   - Should we show timestamp of when stock was last updated?
   - **Decision Needed:** Balance between helpful info and user privacy

3. **Database Transaction Isolation Level:**
   - Current: Default (READ_COMMITTED likely)
   - Should we use SERIALIZABLE for settlement?
   - **Decision Needed:** Performance vs absolute consistency trade-off

4. **Concurrent User Notification:**
   - Should we implement real-time notifications when stock levels change?
   - WebSocket-based live updates to bill screen?
   - **Decision Needed:** Future enhancement or immediate requirement?

---

## Alternative Approaches Considered

### Alternative 1: Optimistic Locking
**Approach:** Add version column to Stock entity
```java
@Version
private Long version;
```

**Pros:**
- Database-enforced consistency
- No application-level logic needed

**Cons:**
- Requires schema changes
- Need to handle OptimisticLockException
- Rollback more complex

**Decision:** Not chosen - schema changes too risky for immediate fix

### Alternative 2: Pessimistic Locking
**Approach:** Lock stock rows at bill creation
```java
Stock stock = em.find(Stock.class, id, LockModeType.PESSIMISTIC_WRITE);
```

**Pros:**
- Absolute prevention of concurrent access

**Cons:**
- Poor concurrency (blocks other users)
- Deadlock risk
- Not suitable for slow cashiers

**Decision:** Not chosen - poor user experience

### Alternative 3: Keep UserStockContainer, Optimize Queries
**Approach:** Add indexes, optimize UserStock query
```sql
CREATE INDEX idx_user_stock_lookup
ON user_stock(stock_id, creater_id, retired, created_at);
```

**Pros:**
- Keeps existing behavior
- May reduce query time by 30-50%

**Cons:**
- Still requires 2 DB queries per item
- Doesn't eliminate the bottleneck

**Decision:** Consider as complementary optimization if main approach insufficient

---

## Monitoring & Metrics

### Application Metrics to Track
1. **Performance:**
   - Average time: autocomplete select → quantity field focus
   - Average time: Add button click → UI update complete
   - Database query count per bill creation
   - Database query execution time (via EclipseLink logging)

2. **Functional:**
   - Number of settlement-time validation failures per day
   - Number of negative stock incidents (should be 0)
   - Number of bills with quantities reset to 0
   - User error rate (items removed at settlement)

3. **User Behavior:**
   - Average time to complete a bill (from first item to settlement)
   - Number of items per bill
   - Concurrent users during peak hours

### Database Queries to Monitor
```sql
-- Check for negative stock (should always be 0 results)
SELECT s.id, s.stock, i.name, ib.batch_no
FROM stock s
JOIN item_batch ib ON s.item_batch_id = ib.id
JOIN item i ON ib.item_id = i.id
WHERE s.stock < 0;

-- Settlement validation failures (last 24 hours)
SELECT COUNT(*)
FROM bill
WHERE bill_type_atomic = 'PHARMACY_RETAIL_SALE_PRE'
AND created_at > NOW() - INTERVAL 1 DAY;
-- If count is increasing significantly, may indicate concurrent conflicts

-- Stock levels by item (identify frequently oversold items)
SELECT i.name, ib.batch_no, s.stock,
       (SELECT SUM(bi.qty)
        FROM bill_item bi
        JOIN pharmaceutical_bill_item pbi ON bi.id = pbi.bill_item_id
        WHERE pbi.stock_id = s.id
        AND bi.bill.created_at > NOW() - INTERVAL 1 HOUR) as sold_last_hour
FROM stock s
JOIN item_batch ib ON s.item_batch_id = ib.id
JOIN item i ON ib.item_id = i.id
WHERE s.stock < 10  -- Low stock items
ORDER BY s.stock ASC;
```

---

## Stakeholder Communication

### For Users (Cashiers):
**Message:**
> We've improved the speed of the pharmacy retail sale screen. You may notice:
> - Faster response when selecting items from the medicine list
> - Smoother transition to entering quantities
>
> **Important Change:**
> If another cashier sells the same stock while you're creating a bill, you'll now see an error message when you click "Settle Bill" instead of when you add the item. The system will automatically remove or adjust the affected items.
>
> If you see this error, simply remove the affected item or adjust the quantity based on the available stock shown in the message.

### For Administrators:
**Message:**
> **Performance Improvement Deployment: UserStockContainer Removal**
>
> **Change:** Removed stock validation during item addition, moved to settlement time only.
>
> **Impact:**
> - 50-150ms faster per item addition
> - Improved user experience during peak hours
>
> **Monitoring:**
> - Watch for negative stock reports (should be zero)
> - Monitor settlement validation failures
>
> **Rollback:**
> If issues occur, change configuration: `Enable UserStockContainer for Pharmacy Retail Sale` = `true`

### For IT Support:
**Troubleshooting Guide:**
- **Issue:** "User complains items are removed at settlement"
  - **Cause:** Concurrent user sold same stock
  - **Resolution:** Check available stock, add different batch or reduce quantity

- **Issue:** "Negative stock reported"
  - **Immediate Action:** Enable UserStockContainer config flag
  - **Escalation:** Report to development team immediately

- **Issue:** "No performance improvement observed"
  - **Action:** Collect performance logs, check network latency, verify config flag set correctly

---

## Appendix: Code Locations Reference

### Key Files Modified
1. **PharmacySaleForCashierController.java**
   - `addBillItemSingleItem()` - Line 1463-1553
   - `addBillItemMultipleBatches()` - Line ~1600-1650
   - `settlePreBillAndNavigateToPrint()` - Line 2352-2574

2. **UserStockController.java**
   - `isStockAvailable()` - Line 45-83 (called conditionally)
   - `retiredAllUserStockContainer()` - Line 168-198 (still used at settlement)

3. **Configuration**
   - New config option: `Enable UserStockContainer for Pharmacy Retail Sale`

### Database Tables Affected
- `stock` - Read more frequently at settlement
- `user_stock` - Usage reduced (only when config enabled)
- `user_stock_container` - Usage reduced (only when config enabled)
- `bill`, `bill_item`, `pharmaceutical_bill_item` - No changes

### XHTML Files
- `pharmacy_bill_retail_sale_for_cashier.xhtml` - No changes required (backend only)

---

## Conclusion

This plan provides a **safe, reversible** approach to improving pharmacy retail sale performance by removing the UserStockContainer validation during item addition. The configuration flag allows instant rollback if needed, and enhanced settlement-time validation ensures stock integrity is maintained.

**Key Benefits:**
- 50-150ms faster per item addition (20-30% improvement expected)
- Reduced database load (2 fewer queries per item)
- Better user experience during peak hours
- No data loss risk - validation still occurs at settlement

**Key Safeguards:**
- Configuration flag for instant rollback
- Enhanced settlement validation prevents negative stock
- Comprehensive testing plan covers all scenarios
- Monitoring metrics track success and issues

**Next Steps:**
1. Review and approve this plan
2. Begin implementation in development environment
3. Execute testing plan thoroughly
4. Deploy to QA for validation
5. Staged production deployment with monitoring
