# Performance Optimization Plan v2: Closing Stock Report
## SIMPLIFIED - Without Consignment Filter

## Executive Summary

**Target Method**: `PharmacyReportController.processClosingStockForBatchReport()`
**File**: `src/main/java/com/divudi/bean/report/PharmacyReportController.java:3444-3542`
**Current Performance**: ~30-60 seconds for 1,000 batches with 5,001+ database queries
**Target Performance**: ~2-3 seconds for 1,000 batches with 1 database query
**Expected Improvement**: 90-95% reduction in execution time, 99.98% reduction in query count

**Key Change from v1**: Removed consignment filtering optimization (lines 3521-3529 will remain in Java)

---

## Problem Statement

### Current Implementation Issues

1. **N+1 Query Problem** (Lines 3505-3506):
   - Fetches list of MAX(StockHistory.id)
   - Loops through each ID calling `facade.find(id)`
   - For 1,000 batches: **5,001 queries** (1 + 1000 finds + 4000 lazy loads)

2. **Lazy Loading Cascade**:
   - Each `facade.find(id)` triggers 4-5 additional queries
   - ItemBatch → Item → Category → MeasurementUnit
   - Total: **~10,000 queries** for typical pharmacy report

3. **No Eager Fetching**:
   - Missing JOIN FETCH for associations
   - Each entity access = separate database round-trip

### Impact on Healthcare Operations

- **30-60 second delays** during peak pharmacy hours
- Database connection pool saturation with concurrent users
- Affects real-time stock availability during patient dispensing
- Direct impact on patient wait times at pharmacy counter

---

## Proposed Solution - SIMPLIFIED TWO-PHASE APPROACH

### Phase 1: Critical JOIN FETCH Optimization (MANDATORY)

**Objective**: Eliminate N+1 query problem with single query using JOIN FETCH

#### Current Code Pattern (Lines 3447-3506):
```java
// Step 1: Get IDs
StringBuilder jpql = new StringBuilder("select MAX(sh.id) from StockHistory sh where ...");
ids = facade.findLongValuesByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

// Step 2: Loop and fetch each entity individually (N+1 problem)
for (Long shid : ids) {
    StockHistory shx = facade.find(shid);  // Separate SELECT per ID
    // Lazy loads: getItemBatch(), getItem(), getCategory(), getMeasurementUnit()
}
```

#### Optimized Code Pattern:
```java
// Single query with all associations pre-loaded
StringBuilder jpql = new StringBuilder(
    "SELECT sh FROM StockHistory sh " +
    "LEFT JOIN FETCH sh.itemBatch ib " +
    "LEFT JOIN FETCH ib.item i " +
    "LEFT JOIN FETCH i.category " +
    "LEFT JOIN FETCH i.measurementUnit " +
    "WHERE sh.id IN (" +
    "  SELECT MAX(sh2.id) FROM StockHistory sh2 " +
    "  WHERE sh2.retired = :ret ");

// Add all existing filters to subquery
if (institution != null) {
    jpql.append("  AND sh2.institution = :ins ");
}
if (site != null) {
    jpql.append("  AND sh2.department.site = :sit ");
}
if (department != null) {
    jpql.append("  AND sh2.department = :dep ");
}
if (category != null) {
    jpql.append("  AND sh2.itemBatch.item.category = :cat ");
}
if (amp != null) {
    jpql.append("  AND sh2.itemBatch.item = :itm ");
}

jpql.append("  AND sh2.createdAt < :et ");
jpql.append("  GROUP BY sh2.department, sh2.itemBatch) ");
jpql.append("ORDER BY sh.itemBatch.item.name");

// Single query execution - all associations loaded
List<StockHistory> stockHistories = facade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

// Simple loop - no additional queries triggered
for (StockHistory shx : stockHistories) {
    if (shx == null || shx.getItemBatch() == null || shx.getItemBatch().getItem() == null) {
        continue;
    }

    // Consignment filter stays in Java (NOT moved to SQL)
    double batchQty = shx.getStockQty();
    if (isConsignmentItem()) {
        if (batchQty > 0) {
            continue;
        }
    } else {
        if (batchQty <= 0) {
            continue;
        }
    }

    // Build PharmacyRow (all associations already loaded)
    PharmacyRow row = new PharmacyRow();
    row.setItem(shx.getItemBatch().getItem());  // No lazy load
    row.setItemBatch(shx.getItemBatch());        // No lazy load
    // ... rest of row population
}
```

#### Benefits:
- **Query Reduction**: 5,001 queries → 1 query (99.98% reduction)
- **Execution Time**: 30-60s → 3-5s (90% improvement)
- **Memory Usage**: ~500MB → ~200MB (60% reduction)
- **All associations pre-loaded**: No LazyInitializationException
- **Consignment logic unchanged**: Remains in Java for safety

#### Risk Assessment: **VERY LOW**
- Standard JPA best practice
- No business logic changes
- Consignment filtering unchanged (stays in Java)
- All filter conditions preserved exactly
- Backward compatible with XHTML page

---

### Phase 2: Database Indices (RECOMMENDED)

**Objective**: Optimize MAX() and GROUP BY database performance

#### Required Indices:

```sql
-- Index 1: Main query optimization (WHERE + GROUP BY)
CREATE INDEX idx_stock_history_closing_report
ON stock_history(retired, created_at, department_id, item_batch_id);

-- Index 2: Institution filtering
CREATE INDEX idx_stock_history_institution
ON stock_history(institution_id, retired, created_at);

-- Index 3: Site filtering
CREATE INDEX idx_stock_history_site
ON stock_history(site_id, retired, created_at);

-- Index 4: Optimize MAX() with GROUP BY
CREATE INDEX idx_stock_history_max_groupby
ON stock_history(item_batch_id, department_id, id DESC);
```

#### Benefits:
- 80-90% reduction in query execution time
- Optimizes MAX() aggregation
- Speeds up GROUP BY operations
- Benefits ALL stock reports (not just closing stock)

#### Risk Assessment: **LOW**
- Read-only optimization
- Can be added/removed independently
- Standard database practice
- Requires DBA coordination for production

---

## Implementation Plan - SIMPLIFIED

### Step 1: Preparation (30 minutes)

- [ ] Create feature branch from current branch
- [ ] Back up PharmacyReportController.java
- [ ] Review all filter conditions in current code
- [ ] Verify facade.findByJpql() method signature

### Step 2: Phase 1 Implementation (2 hours)

**File**: `PharmacyReportController.java` (lines 3447-3542)

**Changes**:

1. Replace lines 3447-3496 with optimized JOIN FETCH query
2. Replace lines 3505-3506 loop with `for (StockHistory shx : stockHistories)`
3. Keep consignment filter (lines 3521-3529) UNCHANGED in Java
4. Keep all row population logic (lines 3512-3540) UNCHANGED

**Detailed Implementation**:

```java
public void processClosingStockForBatchReport() {
    Map<String, Object> params = new HashMap<>();

    // Build optimized query with JOIN FETCH
    StringBuilder jpql = new StringBuilder(
        "SELECT sh FROM StockHistory sh " +
        "LEFT JOIN FETCH sh.itemBatch ib " +
        "LEFT JOIN FETCH ib.item i " +
        "LEFT JOIN FETCH i.category " +
        "LEFT JOIN FETCH i.measurementUnit " +
        "WHERE sh.id IN (" +
        "  SELECT MAX(sh2.id) FROM StockHistory sh2 " +
        "  WHERE sh2.retired = :ret ");

    params.put("ret", false);

    // Add all existing filter conditions to subquery
    if (institution != null) {
        jpql.append("  AND sh2.institution = :ins ");
        params.put("ins", institution);
    }

    if (site != null) {
        jpql.append("  AND sh2.department.site = :sit ");
        params.put("sit", site);
    }

    if (department != null) {
        jpql.append("  AND sh2.department = :dep ");
        params.put("dep", department);
    }

    if (category != null) {
        jpql.append("  AND sh2.itemBatch.item.category = :cat ");
        params.put("cat", category);
    }

    if (amp != null) {
        item = amp;
        jpql.append("  AND sh2.itemBatch.item = :itm ");
        params.put("itm", item);
    }

    jpql.append("  AND sh2.createdAt < :et ");
    if ("Opening Stock".equals(type)) {
        params.put("et", CommonFunctions.getStartOfDay(fromDate));
    } else {
        params.put("et", CommonFunctions.getEndOfDay(toDate));
    }

    // Complete subquery with GROUP BY
    jpql.append("  GROUP BY sh2.department, sh2.itemBatch) ");

    // Order by item name
    jpql.append("ORDER BY sh.itemBatch.item.name");

    // Debug logging
    System.out.println("jpql = " + jpql.toString());
    System.out.println("params = " + params);

    // Execute single optimized query
    List<StockHistory> stockHistories = facade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

    System.out.println("stockHistories.size() = " + stockHistories.size());

    rows = new ArrayList<>();

    // Build rows per ItemBatch (all associations already loaded)
    for (StockHistory shx : stockHistories) {
        if (shx == null || shx.getItemBatch() == null || shx.getItemBatch().getItem() == null) {
            continue;
        }

        // Create a fresh row for each itemBatch
        PharmacyRow row = new PharmacyRow();
        row.setItem(shx.getItemBatch().getItem());
        row.setItemBatch(shx.getItemBatch());

        double batchQty = shx.getStockQty();
        double batchPurchaseRate = shx.getItemBatch().getPurcahseRate();
        double batchSaleRate = shx.getItemBatch().getRetailsaleRate();
        double batchCostRate = shx.getItemBatch().getCostRate() != null ? shx.getItemBatch().getCostRate() : 0.0;

        // Consignment filter (UNCHANGED - stays in Java)
        if (isConsignmentItem()) {
            if (batchQty > 0) {
                continue;
            }
        } else {
            if (batchQty <= 0) {
                continue;
            }
        }

        // Populate row values (UNCHANGED)
        row.setQuantity(batchQty);
        row.setPurchaseValue(batchQty * batchPurchaseRate);
        row.setSaleValue(batchQty * batchSaleRate);
        row.setPurchaseRate(batchPurchaseRate);
        row.setRetailRate(batchSaleRate);
        row.setCostRate(batchCostRate);
        row.setCostValue(batchQty * batchCostRate);

        rows.add(row);
    }
}
```

### Step 3: Phase 1 Testing (2 hours)

#### Test Configuration:
```properties
# Enable SQL logging in persistence.xml
<property name="eclipselink.logging.level.sql" value="FINE"/>
<property name="eclipselink.logging.parameters" value="true"/>
```

#### Test Cases:

**TC1: Query Count Verification**
- [ ] Run report with 100 batches
- [ ] Count SQL queries in log
- [ ] Expected: 1 query (was 501 queries)

**TC2: Execution Time Measurement**
- [ ] Run report with 1,000 batches
- [ ] Measure execution time
- [ ] Expected: <5 seconds (was 30-60 seconds)

**TC3: Filter Combinations**
- [ ] Test with NO filters (all batches)
- [ ] Test with institution filter only
- [ ] Test with site filter only
- [ ] Test with department filter only
- [ ] Test with category filter only
- [ ] Test with specific item (amp) filter only
- [ ] Test with ALL filters combined
- [ ] Test Opening Stock type (uses fromDate)
- [ ] Test Closing Stock type (uses toDate)

**TC4: Consignment Filtering**
- [ ] Set `consignmentItem = true`, verify only negative/zero qty batches
- [ ] Set `consignmentItem = false`, verify only positive qty batches
- [ ] Verify row counts match original implementation

**TC5: Data Accuracy**
- [ ] Compare row count with original implementation
- [ ] Verify item names, codes, categories
- [ ] Verify batch numbers, expiry dates
- [ ] Verify quantities match
- [ ] Verify purchase/sale/cost values calculated correctly
- [ ] Verify measurement units display

**TC6: Edge Cases**
- [ ] Test with empty result set (no matching batches)
- [ ] Test with single batch
- [ ] Test with batches having null costRate
- [ ] Test with batches having zero stockQty
- [ ] Test with batches having negative stockQty (consignment)

**TC7: XHTML Page Rendering**
- [ ] Verify all columns display correctly
- [ ] Test Excel export
- [ ] Test PDF export
- [ ] Verify pagination works (rows="10")
- [ ] Verify developer-only columns (if applicable)

**TC8: Concurrent Users**
- [ ] Simulate 5 concurrent users running report
- [ ] Monitor database connection pool
- [ ] Verify no connection exhaustion
- [ ] Verify no performance degradation

### Step 4: Phase 2 - Database Indices (1 hour)

**Coordinate with DBA**:

1. Generate index creation script
2. Test in development database
3. Run EXPLAIN PLAN to verify index usage
4. Schedule QA deployment
5. Schedule production deployment during off-peak hours

**Index Creation Script**:
```sql
-- File: create_closing_stock_indices.sql

USE hmis; -- or your database name

-- Index 1: Main closing report query
CREATE INDEX idx_stock_history_closing_report
ON stock_history(retired, created_at, department_id, item_batch_id);

-- Index 2: Institution filtering
CREATE INDEX idx_stock_history_institution
ON stock_history(institution_id, retired, created_at);

-- Index 3: Site filtering
CREATE INDEX idx_stock_history_site
ON stock_history(site_id, retired, created_at);

-- Index 4: MAX() with GROUP BY optimization
CREATE INDEX idx_stock_history_max_groupby
ON stock_history(item_batch_id, department_id, id DESC);

-- Verify indices created
SHOW INDEX FROM stock_history;
```

**Index Verification**:
```sql
-- Run EXPLAIN on optimized query to verify index usage
EXPLAIN SELECT sh.id FROM stock_history sh
WHERE sh.retired = 0
  AND sh.created_at < NOW()
GROUP BY sh.department_id, sh.item_batch_id;

-- Should show "Using index" in Extra column
```

### Step 5: QA Validation (3 hours)

- [ ] Deploy to QA environment
- [ ] Run all test cases from Step 3
- [ ] Performance benchmark with production-like data
- [ ] Business user validation of report accuracy
- [ ] Load test with 10 concurrent users
- [ ] Memory profiling
- [ ] Database connection pool monitoring

### Step 6: Production Deployment

- [ ] Create pull request
- [ ] Code review approval
- [ ] Merge to main branch
- [ ] Deploy during off-peak hours
- [ ] Monitor production logs for 24 hours
- [ ] Verify performance improvement
- [ ] Collect user feedback

---

## Performance Benchmarks

### Expected Results:

| Metric | Before (Current) | After Phase 1 | After Phase 2 | Target |
|--------|------------------|---------------|---------------|--------|
| Query Count (1,000 batches) | 5,001 | 1 | 1 | < 10 |
| Execution Time (1,000 batches) | 30-60s | 3-5s | 2-3s | < 5s |
| Memory Usage | ~500MB | ~200MB | ~150MB | < 250MB |
| Database Connections | 1 per query | 1 total | 1 total | 1 |
| Connection Pool Saturation | High risk | No risk | No risk | None |

---

## Code Changes Summary

### Files Modified:
1. **PharmacyReportController.java** - `processClosingStockForBatchReport()` method only

### Files NOT Modified:
- closing_stock_report.xhtml (no changes needed)
- PharmacyRow.java (no changes needed)
- StockHistory.java entity (no changes)
- AbstractFacade.java (uses existing methods)

### Lines Changed:
- **Lines 3447-3506**: Replace ID fetching + loop with JOIN FETCH query
- **Lines 3507-3542**: Update loop variable, keep all logic identical

### Business Logic Changes:
- **NONE** - All business logic remains identical
- Consignment filtering: UNCHANGED (stays in Java)
- Filter conditions: UNCHANGED (all preserved)
- Calculations: UNCHANGED (all preserved)
- Row population: UNCHANGED

---

## Risk Assessment

### Technical Risks:

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Incorrect query results | VERY LOW | HIGH | Thorough side-by-side testing |
| LazyInitializationException | VERY LOW | HIGH | JOIN FETCH loads all associations |
| Performance degradation | VERY LOW | HIGH | Benchmark testing before deployment |
| XHTML rendering issues | VERY LOW | MEDIUM | All fields pre-loaded with JOIN FETCH |
| Database index overhead | LOW | LOW | Indices are read-optimized, monitor writes |

### Healthcare Risks:

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Incorrect stock quantities | VERY LOW | CRITICAL | Extensive testing, business validation |
| Report data mismatch | VERY LOW | HIGH | Compare with original implementation |
| Pharmacy workflow disruption | VERY LOW | HIGH | Deploy during off-peak, have rollback ready |

---

## Rollback Plan

### If Issues Detected:

**Immediate Rollback (< 5 minutes)**:
```bash
git log --oneline -5  # Find commit hash
git revert <commit-hash>
git push origin <branch-name>
# Redeploy previous version
```

**Database Indices Rollback**:
```sql
DROP INDEX idx_stock_history_closing_report ON stock_history;
DROP INDEX idx_stock_history_institution ON stock_history;
DROP INDEX idx_stock_history_site ON stock_history;
DROP INDEX idx_stock_history_max_groupby ON stock_history;
```

**Partial Rollback**:
- Phase 2 indices can be dropped independently without affecting Phase 1
- Phase 1 code can be reverted independently

---

## Success Criteria

### Must Have (Phase 1):
- ✅ Query count reduced by 99%+ (5,001 → 1 query)
- ✅ Execution time reduced by 80%+ (30-60s → <5s)
- ✅ All test cases pass
- ✅ Consignment filtering works correctly
- ✅ All filter combinations work
- ✅ No LazyInitializationException
- ✅ XHTML page renders correctly
- ✅ Excel/PDF export works

### Should Have (Phase 2):
- ✅ Database query execution time reduced by 80-90%
- ✅ Benefits other stock reports
- ✅ No write performance degradation

---

## Questions for Code Reviewer

1. **JOIN FETCH Approach**: Is this the recommended pattern for eliminating N+1 queries in HMIS?

2. **Consignment Filter in Java**: Confirmed we should keep this in Java code (not move to SQL)?

3. **Testing Coverage**: Is the test plan comprehensive enough for pharmacy critical report?

4. **Database Indices**: Should these be deployed with the code change or scheduled separately?

5. **Performance Target**: Is <5 seconds acceptable for 1,000 batches, or should we target lower?

6. **Opening Stock Type**: Should we test this variant as thoroughly as closing stock?

---

## Timeline

| Phase | Duration | Deliverable |
|-------|----------|-------------|
| Preparation | 30 min | Feature branch ready |
| Phase 1 Implementation | 2 hours | Code complete |
| Phase 1 Testing | 2 hours | All tests pass |
| Phase 2 Implementation | 1 hour | Index scripts ready |
| QA Validation | 3 hours | Performance verified |
| Production Deployment | 1 hour | Live in production |
| **Total** | **9.5 hours** | **Production-ready** |

---

## Approval Checklist

Before proceeding, verify:

- [x] Consignment filter removed from SQL optimization (stays in Java)
- [ ] No historyType filtering added (not in current code)
- [ ] All existing filter conditions preserved
- [ ] JOIN FETCH includes all required associations
- [ ] No modifications to PharmacyRow class needed
- [ ] No modifications to XHTML page needed
- [ ] Backward compatible with existing data
- [ ] Test plan covers all filter combinations
- [ ] Rollback plan documented and tested
- [ ] DBA notified for Phase 2 coordination

---

## Next Steps After Approval

1. Create feature branch from current branch
2. Implement Phase 1 (JOIN FETCH optimization)
3. Run local testing with SQL logging enabled
4. Deploy to development environment for testing
5. Run all test cases
6. If tests pass, deploy to QA
7. Get business user validation
8. Schedule production deployment
9. Coordinate Phase 2 (indices) with DBA

---

**Version**: 2.0 (Simplified - No Consignment Filter Optimization)
**Date**: 2025-10-05
**Status**: READY FOR CODE REVIEW
**Risk Level**: VERY LOW
**Expected Benefit**: 90%+ performance improvement with minimal risk
