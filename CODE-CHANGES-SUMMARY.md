# Code Optimization Changes - BHT/8733 Performance Issue

**Date:** 2026-02-05
**Status:** ⚠️ CODE CHANGES READY - NEEDS COMPILATION BY USER

## Problem Summary

Network latency to Azure database causing severe performance issues:
- Each query takes ~100ms network round-trip
- N+1 query pattern causes 88+ separate queries
- **Result: 180+ second page load time**

## Root Cause Analysis

### Issue 1: createDepartmentBillItems (30 seconds)
- 22 items × 4 queries each = 88 database round-trips
- Each item: BilledBill count + CancelledBill count + RefundBill count + Checked count
- With 100ms latency: 88 × 100ms = 9+ seconds minimum

### Issue 2: createChargeItemTotals (52 seconds)
- ~18 InwardChargeType values × 1 query each = 18 database round-trips
- Each query: SUM(grossValue+marginValue) grouped by charge type
- With network latency: 18 × 3 seconds = 54 seconds

## Solution Implemented

### 1. Optimized createDepartmentBillItems
**Changed from:** 88 separate queries (one per item × 4 types)
**Changed to:** 4 bulk GROUP BY queries (one per bill type)

**File:** `src/main/java/com/divudi/bean/inward/InwardBeanController.java`

**New Methods Added:**
- `createDepartmentBillItemsOptimized()` - Replacement for createDepartmentBillItems
- `getBulkBillItemCounts()` - Bulk query for billed/cancelled/refund counts
- `getBulkCheckedBillItemCounts()` - Bulk query for checked counts

**Expected Performance:** 30 seconds → <2 seconds (15x faster)

### 2. Optimized createChargeItemTotals
**Changed from:** 18 separate SUM queries (one per InwardChargeType)
**Changed to:** 1 bulk GROUP BY query for all types

**File:** `src/main/java/com/divudi/bean/inward/InwardBeanController.java`

**New Method Added:**
- `calServiceBillItemsTotalByInwardChargeTypeBulk()` - Bulk version that returns Map

**File:** `src/main/java/com/divudi/bean/inward/BhtSummeryController.java`

**Method Updated:**
- `setServiceTotCategoryWise()` - Now uses bulk query

**Expected Performance:** 52 seconds → <1 second (50x faster)

## Files Modified

### 1. InwardBeanController.java
- ✅ Added `createDepartmentBillItemsOptimized()` after line 1552
- ✅ Added `getBulkBillItemCounts()` helper method
- ✅ Added `getBulkCheckedBillItemCounts()` helper method
- ✅ Added `calServiceBillItemsTotalByInwardChargeTypeBulk()` after line 416

### 2. BhtSummeryController.java
- ✅ Replaced 3 calls to `createDepartmentBillItems()` with `createDepartmentBillItemsOptimized()`
  - Line 2231
  - Line 2298
  - Line 3169
- ✅ Updated `setServiceTotCategoryWise()` to use bulk query (~line 2976)

## Database Indexes Created

All 6 indexes successfully created on Southern Lanka Production:

### Single-Column Indexes:
1. ✅ `idx_bill_retired` on `bill.RETIRED`
2. ✅ `idx_bill_billtype` on `bill.BILLTYPE`
3. ✅ `idx_bill_dtype` on `bill.DTYPE`
4. ✅ `idx_billitem_retired` on `billitem.RETIRED`

### Composite Indexes:
5. ✅ `idx_bill_pe_bt_dtype_ret` on `bill(PATIENTENCOUNTER_ID, BILLTYPE, DTYPE, RETIRED)`
6. ✅ `idx_billitem_ret_item_bill` on `billitem(RETIRED, ITEM_ID, BILL_ID)`

**Note:** Indexes alone did NOT fix the performance issue. The network latency required code optimization.

## Expected Overall Performance

### Before Optimization:
- createDepartmentBillItems: **30 seconds**
- fetchIssueTable (Pharmacy): **121 seconds** (not yet optimized)
- createChargeItemTotals: **52 seconds**
- **Total: 180+ seconds**

### After Optimization (Expected):
- createDepartmentBillItems: **<2 seconds** ✨
- fetchIssueTable (Pharmacy): **121 seconds** (still slow - needs separate optimization)
- createChargeItemTotals: **<1 second** ✨
- **Total: ~125 seconds** (60 second improvement)

### After Full Optimization (If fetchIssueTable is also optimized):
- **Total: <10 seconds** 🎉

## Next Steps - ACTION REQUIRED

### 1. Compile the Code ⚠️
**You need to compile the changes:**
```bash
# Use your preferred build method
mvn clean compile
# OR
./your-build-script.bat
```

### 2. Restart Payara
```bash
# Stop Payara
C:\Users\buddhika\Payara_Server\bin\asadmin stop-domain domain1

# Start Payara
C:\Users\buddhika\Payara_Server\bin\asadmin start-domain domain1
```

### 3. Test the Optimizations
- Navigate to: http://localhost:9090/sl/faces/inward/inward_bill_intrim.xhtml
- Search for: BHT/8733
- Click: **Select** button
- **Expected:** createDepartmentBillItems: <2 seconds (was 30s)
- Click: **Settle Bill** button
- **Expected:** createChargeItemTotals: <1 second (was 52s)

### 4. Check Server Logs
Look for these success indicators:
```
=== createDepartmentBillItemsOptimized END: Total time = 1500ms ===
setServiceTotCategoryWise END: Total time = 800ms (OPTIMIZED)
```

## Remaining Bottleneck

**fetchIssueTable (Pharmacy): Still 121 seconds**

This also likely has an N+1 query problem that needs investigation. Check:
- `InwardBeanController.fetchIssueTable()` method
- Similar bulk query optimization may be needed

## Rollback Plan

If issues occur, you can revert to the old methods:

### In BhtSummeryController.java:
Change all instances of:
```java
createDepartmentBillItemsOptimized(...)
```
Back to:
```java
createDepartmentBillItems(...)
```

And revert `setServiceTotCategoryWise()` to use the loop with individual queries.

## Technical Notes

### Bulk Query Pattern Used:
```sql
-- Instead of N queries like this:
SELECT count(b) FROM BillItem b WHERE ... AND b.item = :item1
SELECT count(b) FROM BillItem b WHERE ... AND b.item = :item2
-- etc...

-- We now use ONE query:
SELECT b.item.id, count(b)
FROM BillItem b
WHERE ... AND b.item IN :items
GROUP BY b.item.id
```

### Why Network Latency Matters:
- Database is in Azure (high latency ~100ms)
- Each query: 100ms network + query time
- 88 queries: 88 × 100ms = 8.8 seconds minimum (just network!)
- 4 bulk queries: 4 × 100ms = 0.4 seconds network time

## Files for Reference

- `database-indexes-bht-performance.sql` - SQL for index creation
- `PERFORMANCE-FIX-SUMMARY.md` - Overall performance fix summary
- `CODE-CHANGES-SUMMARY.md` - This file

---
**Status:** Code changes complete, awaiting compilation and testing
**Created:** 2026-02-05 07:40 AM
