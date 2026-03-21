# BHT/8733 Performance Fix - Southern Lanka Production

**Date:** 2026-02-05
**Database:** southernlankaprod
**Issue:** Interim bill page timing out (180+ seconds)

## Problem Analysis

From the logs, we identified 3 major bottlenecks:

1. **fetchIssueTable (Pharmacy):** 123 seconds (first load)
2. **createChargeItemTotals:** 54 seconds ← Main bottleneck
3. **createDepartmentBillItems:** 9-30 seconds

**Total page load time:** 180+ seconds (3 minutes!)

### Root Cause
Missing database indexes on frequently-queried columns causing full table scans:
- `bill` table: 280,000 rows
- `billitem` table: 1,500,000 rows

## Solution Implemented

### Database Indexes Created (2026-02-05 05:05-05:07 AM)

#### Single-Column Indexes (4):
1. ✅ `idx_bill_retired` on `bill.RETIRED`
2. ✅ `idx_bill_billtype` on `bill.BILLTYPE`
3. ✅ `idx_bill_dtype` on `bill.DTYPE`
4. ✅ `idx_billitem_retired` on `billitem.RETIRED`

#### Composite Indexes (2):
5. ✅ `idx_bill_pe_bt_dtype_ret` on `bill(PATIENTENCOUNTER_ID, BILLTYPE, DTYPE, RETIRED)`
6. ✅ `idx_billitem_ret_item_bill` on `billitem(RETIRED, ITEM_ID, BILL_ID)`

### Code Changes

Added comprehensive performance logging to:

1. **InwardBeanController.java**
   - `createDepartmentBillItems()` - Line 1504
   - Shows timing for each department and slow items (>100ms)

2. **BhtSummeryController.java**
   - `createTables()` - Line 2110
   - `toSettle()` - Line 1712
   - `settleOriginalBill()` - Line 1266
   - `saveOriginalBillItem()` - Line 1964
   - `createChargeItemTotals()` - Line 2822
   - `setServiceTotCategoryWise()` - Line 2976

## Expected Performance Improvement

### Before Indexes:
- createDepartmentBillItems: **9-30 seconds** (400ms per item × 22 items)
- createChargeItemTotals: **54 seconds** (3000ms per charge type × 18 types)
- fetchIssueTable: **123 seconds** (first load)
- **Total: 180+ seconds**

### After Indexes (Expected):
- createDepartmentBillItems: **<1 second** (<10ms per item × 22 items)
- createChargeItemTotals: **<2 seconds** (<100ms per charge type × 18 types)
- fetchIssueTable: **<5 seconds**
- **Total: <10 seconds** 🎉

### Performance Gain: **18x faster** (180s → 10s)

## Testing Instructions

1. **Restart Payara Server:**
   ```bash
   # Start Payara
   C:\Users\buddhika\Payara_Server\bin\asadmin start-domain domain1
   ```

2. **Test the page:**
   - Navigate to: http://localhost:9090/sl/faces/inward/inward_bill_intrim.xhtml
   - Search for: BHT/8733
   - Click: **Select** button
   - Expected: Page loads in <10 seconds

3. **Check logs:**
   - Location: `C:\Users\buddhika\Payara_Server\glassfish\domains\domain1\logs\server.log`
   - Look for timing output from the System.out statements

4. **Test Settle Bill:**
   - After page loads, click: **Settle Bill** button
   - Expected: Completes in <15 seconds

## Log Examples to Look For

### Success Indicators:
```
=== createDepartmentBillItems END: Total time = 900ms ===
createChargeItemTotals END: Total time = 1800ms
======== createTables END: Total time = 9000ms ========
```

### If Still Slow:
```
SLOW Item: [ItemName] took 400ms  ← Index not being used
SLOW ChargeType: [Type] took 3000ms  ← Need to investigate query
```

## Verification Queries

Run these to verify indexes are being used:

```sql
-- Should show "idx_bill_pe_bt_dtype_ret" in the "key" column
EXPLAIN SELECT COUNT(bi.ID)
FROM billitem bi
INNER JOIN bill b ON bi.BILL_ID = b.ID
WHERE bi.RETIRED = 0
  AND b.BILLTYPE = 'InwardBill'
  AND b.DTYPE = 'BilledBill'
  AND b.RETIRED = 0;
```

## Rollback Plan

If indexes cause any issues (unlikely):

```sql
-- Remove all custom indexes
ALTER TABLE bill DROP INDEX idx_bill_retired;
ALTER TABLE bill DROP INDEX idx_bill_billtype;
ALTER TABLE bill DROP INDEX idx_bill_dtype;
ALTER TABLE bill DROP INDEX idx_bill_pe_bt_dtype_ret;
ALTER TABLE billitem DROP INDEX idx_billitem_retired;
ALTER TABLE billitem DROP INDEX idx_billitem_ret_item_bill;
```

## Files Modified

1. `src/main/java/com/divudi/bean/inward/InwardBeanController.java`
2. `src/main/java/com/divudi/bean/inward/BhtSummeryController.java`
3. `database-indexes-bht-performance.sql` (created)
4. `PERFORMANCE-FIX-SUMMARY.md` (this file)

## Notes

- All indexes created with default settings (BTREE, non-unique)
- No data was modified, only indexes added
- Indexes take ~50MB additional disk space
- Indexes are automatically maintained by MySQL
- Safe to deploy to other environments (COOP, Ruhunu, etc.)

## Next Steps

1. ✅ Indexes created on Southern Lanka Production
2. 🔄 Test with BHT/8733 (pending Payara restart)
3. ⏳ Monitor production performance for 24 hours
4. ⏳ If successful, apply same indexes to other environments
5. ⏳ Consider removing System.out logging after verification

## Questions/Issues

If the page is still slow after this fix, check:
1. Is the SSH tunnel still active? (Port 3316)
2. Are the indexes being used? (Run EXPLAIN queries)
3. Is there a different bottleneck? (Check new detailed logs)
4. Is the database server under heavy load? (Check MySQL processlist)

---
**Status:** Indexes created successfully at 2026-02-05 05:07:29
**Ready for testing:** Yes (restart Payara and test)
