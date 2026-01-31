# GRN Expense Calculation Fix Summary

## Issue #14465 - GRN Expense Categorization Problems

### Problem
- GRN expense totals for "Considered for Costing" and "Not Considered for Costing" were showing 0.00
- Net total was not including expenses properly
- User reported seeing expense totals as 0.00 despite adding expenses

### Root Cause
**Bill Object Mismatch**: Expenses were being added to `getBill()` (StorePurchase type) but calculations were performed on `getGrnBill()` (PharmacyGrnBill type). This caused the service to never see the expenses.

### Solution Applied
1. **Fixed Bill Object Consistency**: Updated all expense-related methods to use `getGrnBill()` consistently:
   - `addExpense()` - Now uses `getGrnBill()` instead of `getBill()`
   - `recalculateExpenseTotals()` - Now updates `getGrnBill()`
   - `updateExpenseCosting()` - Now uses `getGrnBill()`
   - `removeExpense()` - Now removes from `getGrnBill()`

2. **Enhanced PharmacyCostingService**: Added proper calculation for `expensesTotalNotConsideredForCosting`
   - Both expense categories are now calculated correctly
   - This improvement also benefits the direct purchase module

3. **Added Missing JSF Method**: Added `onExpenseItemSelect()` to resolve binding errors

### Test Results
**Before Fix:**
```
Bill Expenses: 444.00
Bill Expenses (Considered for Costing): 0.00  ❌
Bill Expenses (Not Considered for Costing): 0.00  ❌
```

**After Fix:**
```  
Bill Expenses: 444.00
Bill Expenses (Considered for Costing): 444.00  ✅
Bill Expenses (Not Considered for Costing): 0.00  ✅
Net Total: Now includes considered expenses  ✅
```

### Files Modified
- `src/main/java/com/divudi/bean/pharmacy/GrnCostingController.java`
- `src/main/java/com/divudi/service/pharmacy/PharmacyCostingService.java`
- `src/main/webapp/pharmacy/pharmacy_grn_costing.xhtml`

### Status
✅ **RESOLVED** - GRN expense categorization now works correctly
✅ **TESTED** - User confirmed the fix is working
✅ **PRODUCTION READY** - Debug logging removed, clean implementation

---
*Fix implemented by Claude Code on branch 14503-add-automation-scripts-for-persistence-jndi-configuration*
*Issue #14465 successfully resolved*