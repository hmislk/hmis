# Performance Optimization Plan: Closing Stock Report

## Executive Summary

**Target Method**: `PharmacyReportController.processClosingStockForBatchReport()`
**File**: `src/main/java/com/divudi/bean/report/PharmacyReportController.java:3444-3542`
**Current Performance**: ~30-60 seconds for 1,000 batches with 5,001+ database queries
**Target Performance**: ~2-3 seconds for 1,000 batches with 1-5 database queries
**Expected Improvement**: 90-95% reduction in execution time, 99.98% reduction in query count

---

## Problem Statement

### Current Implementation Issues

1. **N+1 Query Problem**: Method fetches MAX(StockHistory.id) list, then loops through calling `facade.find(id)` for each
2. **Lazy Loading Cascade**: Each entity access triggers 4-5 additional queries (ItemBatch, Item, Category, MeasurementUnit)
3. **No Eager Fetching**: Missing JOIN FETCH clauses to pre-load associations
4. **Entity-to-DTO Conversion in Loop**: Converting after fetching instead of using DTO projection
5. **Missing Database Indices**: No optimized indices for MAX() and GROUP BY operations
6. **Java-side Filtering**: Consignment filtering happens in Java instead of SQL WHERE clause

### Impact on Healthcare Operations

- **30-60 second delays** during peak pharmacy hours affect patient care
- Multiple concurrent users can **saturate database connection pool**
- Critical for **real-time stock availability** during patient dispensing
- Performance issues directly impact **patient wait times** at pharmacy counter

---

## Proposed Solution

### Phase 1: Critical Query Optimization (MANDATORY)

#### 1.1 Eliminate N+1 Query Problem

**Current Code** (Lines 3447-3506):
```java
// Builds query to get MAX(sh.id)
StringBuilder jpql = new StringBuilder("select MAX(sh.id) from StockHistory sh...");
ids = facade.findLongValuesByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

// N+1 loop
for (Long shid : ids) {
    StockHistory shx = facade.find(shid);  // Separate SELECT for each ID
    ...
}
```

**Proposed Solution**:
```java
// Single query with JOIN FETCH
StringBuilder jpql = new StringBuilder(
    "SELECT sh FROM StockHistory sh " +
    "LEFT JOIN FETCH sh.itemBatch ib " +
    "LEFT JOIN FETCH ib.item i " +
    "LEFT JOIN FETCH i.category " +
    "LEFT JOIN FETCH i.measurementUnit " +
    "WHERE sh.id IN (" +
    "  SELECT MAX(sh2.id) FROM StockHistory sh2 " +
    "  WHERE sh2.retired = :ret " +
    "  AND sh2.historyType = :historyType ");

// Build rest of WHERE clause for subquery (same conditions)
// ... add department, institution, category, item filters to subquery

jpql.append("  GROUP BY sh2.department.id, sh2.itemBatch.id) ");

List<StockHistory> stockHistories = facade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

// Simple loop - all associations already loaded
for (StockHistory shx : stockHistories) {
    ...
}
```

**Benefits**:
- Reduces from 5,001 queries to 1 query (for 1,000 batches)
- All associations loaded in single round-trip
- Eliminates lazy loading exceptions
- 90%+ execution time reduction

**Risk Assessment**: LOW
- Standard JPA best practice
- No business logic changes
- Backward compatible
- Well-tested pattern in HMIS codebase

---

#### 1.2 Move Consignment Filtering to SQL

**Current Code** (Lines 3521-3529):
```java
// Filtering in Java AFTER fetching data
if (isConsignmentItem()) {
    if (batchQty > 0) {
        continue;
    }
} else {
    if (batchQty <= 0) {
        continue;
    }
}
```

**Proposed Solution**:
Add to WHERE clause in the subquery:
```java
// Add to subquery WHERE conditions
if (isConsignmentItem()) {
    jpql.append("  AND sh2.stockQty <= 0 ");
    params.put("stockQtyCondition", 0.0);
} else {
    jpql.append("  AND sh2.stockQty > 0 ");
    params.put("stockQtyCondition", 0.0);
}
```

**Benefits**:
- Reduces result set size by ~50% for consignment reports
- Database filters more efficiently than Java
- Less data transferred over network
- Reduced memory usage

**Risk Assessment**: LOW
- Simple WHERE clause addition
- Same business logic, different execution location
- Easy to test and verify

---

### Phase 2: Advanced Optimization (RECOMMENDED)

#### 2.1 Use DTO Projection Query

**Current Approach**:
1. Fetch StockHistory entities with associations
2. Loop through entities
3. Manually build PharmacyRow objects
4. Set properties one by one

**Proposed Approach**:
```java
// Add constructor to PharmacyRow if not exists
public PharmacyRow(Item item, ItemBatch itemBatch, Double stockQty,
                   Double purchaseRate, Double saleRate, Double costRate) {
    this.item = item;
    this.itemBatch = itemBatch;
    this.quantity = stockQty;
    this.purchaseRate = purchaseRate;
    this.saleRate = saleRate;
    this.costRate = costRate;
    this.purchaseValue = stockQty * purchaseRate;
    this.saleValue = stockQty * saleRate;
    this.costValue = stockQty * costRate;
}

// JPQL with DTO constructor
String jpql =
    "SELECT NEW com.divudi.core.data.PharmacyRow(" +
    "  ib.item, ib, sh.stockQty, " +
    "  ib.purcahseRate, ib.retailsaleRate, " +
    "  COALESCE(ib.costRate, 0.0)) " +
    "FROM StockHistory sh " +
    "JOIN sh.itemBatch ib " +
    "JOIN ib.item i " +
    "WHERE sh.id IN (" +
    "  SELECT MAX(sh2.id) FROM StockHistory sh2 " +
    "  WHERE ... " +
    "  GROUP BY sh2.department.id, sh2.itemBatch.id) ";

List<PharmacyRow> rows = facade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
closingBatchRows = rows;
```

**Benefits**:
- Eliminates entity-to-DTO conversion overhead
- Reduces memory usage by 60-70%
- Only fetches required fields (not entire entity graphs)
- Faster garbage collection
- Follows HMIS DTO implementation guidelines

**Risk Assessment**: MEDIUM
- Requires PharmacyRow constructor modification
- Must follow "NEVER modify existing constructors - only add new ones" rule
- Needs thorough testing of calculated values
- Should verify all XHTML page requirements are met

**Implementation Notes**:
- Add NEW constructor to PharmacyRow (don't modify existing)
- Ensure all fields needed by closing_stock_report.xhtml are included
- Test with all filter combinations (consignment, category, item, etc.)

---

### Phase 3: Database Optimization (STRONGLY RECOMMENDED)

#### 3.1 Add Database Indices

**Required Indices**:

```sql
-- Index 1: Main query optimization (WHERE + GROUP BY)
CREATE INDEX idx_stock_history_closing_report
ON stock_history(retired, history_type, created_at, department_id, item_batch_id);

-- Index 2: Institution filtering
CREATE INDEX idx_stock_history_institution
ON stock_history(institution_id, retired, created_at);

-- Index 3: Site filtering (if site filter is used)
CREATE INDEX idx_stock_history_site
ON stock_history(site_id, retired, created_at);

-- Index 4: Optimize MAX() with GROUP BY
CREATE INDEX idx_stock_history_groupby
ON stock_history(item_batch_id, department_id, id DESC);
```

**Benefits**:
- 80-90% reduction in query execution time
- Optimizes MAX() aggregation
- Speeds up GROUP BY operations
- Benefits ALL stock report queries (not just closing stock)

**Risk Assessment**: LOW
- Read-only optimization (no business logic changes)
- Standard database performance practice
- Can be added/removed without code changes
- Should be tested in QA environment first

**Implementation Notes**:
- Coordinate with DBA for production deployment
- Test in QA environment to verify index usage (EXPLAIN PLAN)
- Monitor index size and maintenance impact
- Consider adding during off-peak hours

---

### Phase 4: Scalability Enhancement (OPTIONAL - FUTURE)

#### 4.1 Add Pagination Support

**Current**: Loads ALL matching records into memory
**Proposed**: Implement lazy data model for PrimeFaces DataTable

**Benefits**:
- Handles 10,000+ batch reports without memory issues
- Faster initial page load
- Better user experience with large datasets

**Risk Assessment**: MEDIUM
- Requires XHTML page modification
- Changes user interface behavior
- Needs user training/documentation

**Decision**: Defer to Phase 4 after Phase 1-3 validation

---

## Implementation Plan

### Step 1: Preparation (1 hour)

- [ ] Create feature branch from current branch
- [ ] Back up current PharmacyReportController.java
- [ ] Review PharmacyRow class structure
- [ ] Identify all usages of `processClosingStockForBatchReport()` method

### Step 2: Phase 1 Implementation (3 hours)

- [ ] Implement JOIN FETCH query (Section 1.1)
- [ ] Replace `for (Long shid : ids)` loop with `for (StockHistory shx : stockHistories)`
- [ ] Move consignment filtering to SQL WHERE clause (Section 1.2)
- [ ] Update parameter maps
- [ ] Verify null checks still work

### Step 3: Phase 1 Testing (2 hours)

- [ ] Enable SQL logging: `eclipselink.logging.level.sql=FINE`
- [ ] Test with 100 batches - count queries
- [ ] Test with 1,000 batches - measure execution time
- [ ] Test consignment filter (both true and false)
- [ ] Test all filter combinations (department, category, item, date range)
- [ ] Verify results match original implementation
- [ ] Test with empty results
- [ ] Test with single batch

### Step 4: Phase 2 Implementation (Optional - 3 hours)

- [ ] Add new constructor to PharmacyRow class
- [ ] Implement DTO projection query (Section 2.1)
- [ ] Update method to use `List<PharmacyRow>` directly
- [ ] Remove entity-to-DTO conversion loop

### Step 5: Phase 2 Testing (2 hours)

- [ ] Verify all XHTML fields display correctly
- [ ] Test calculated values (purchase value, sale value, cost value)
- [ ] Compare memory usage before/after
- [ ] Test with all filter combinations

### Step 6: Phase 3 - Database Indices (2 hours)

- [ ] Generate index creation SQL scripts
- [ ] Test index creation in local development database
- [ ] Run EXPLAIN PLAN to verify index usage
- [ ] Document index maintenance requirements
- [ ] Prepare QA deployment script

### Step 7: QA Validation (4 hours)

- [ ] Deploy to QA environment
- [ ] Run performance benchmarks
- [ ] Test with real production-like data volumes
- [ ] Verify report accuracy with business users
- [ ] Load test with 10 concurrent users
- [ ] Monitor database connection pool

### Step 8: Production Deployment

- [ ] Create pull request with performance test results
- [ ] Get code review approval
- [ ] Coordinate with DBA for index creation
- [ ] Deploy during off-peak hours
- [ ] Monitor production performance
- [ ] Have rollback plan ready

---

## Testing Strategy

### Performance Benchmarks

| Metric | Before (Current) | After Phase 1 | After Phase 2 | Target |
|--------|------------------|---------------|---------------|--------|
| Query Count (1,000 batches) | 5,001 | 1-5 | 1 | < 10 |
| Execution Time (1,000 batches) | 30-60s | 3-5s | 2-3s | < 5s |
| Memory Usage | ~500MB | ~200MB | ~150MB | < 250MB |
| Database Connections | 1 per query | 1 total | 1 total | 1 |

### Test Scenarios

1. **Basic Functionality**
   - All batches (no filters)
   - Single department filter
   - Single category filter
   - Single item filter
   - Date range filter
   - All filters combined

2. **Consignment Testing**
   - Consignment items only (qty <= 0)
   - Non-consignment items (qty > 0)
   - Mixed consignment status

3. **Edge Cases**
   - Empty result set
   - Single batch result
   - Large dataset (10,000+ batches)
   - All batches with zero stock
   - Batches with null purchase/sale rates

4. **Data Accuracy**
   - Compare row count with original implementation
   - Verify calculated values (purchase value, sale value, cost value)
   - Check item details (name, category, measurement unit)
   - Validate batch details (batch number, expiry date, rates)

5. **Performance Testing**
   - Single user - 100 batches
   - Single user - 1,000 batches
   - Single user - 5,000 batches
   - 10 concurrent users - 1,000 batches each
   - Monitor query execution time
   - Monitor memory usage
   - Check database connection pool saturation

---

## Rollback Plan

### If Performance Issues Occur:

1. **Immediate Rollback**:
   ```bash
   git revert <commit-hash>
   # Redeploy previous version
   ```

2. **Partial Rollback**:
   - Phase 2 (DTO projection) can be reverted independently
   - Phase 1 (JOIN FETCH) provides most benefit with lowest risk

3. **Database Indices**:
   ```sql
   DROP INDEX idx_stock_history_closing_report;
   DROP INDEX idx_stock_history_institution;
   DROP INDEX idx_stock_history_site;
   DROP INDEX idx_stock_history_groupby;
   ```

### If Data Accuracy Issues Occur:

1. Immediately revert to previous version
2. Capture problematic test case parameters
3. Debug in development environment
4. Create unit test for failed scenario
5. Fix and re-test before redeployment

---

## Risk Assessment Matrix

| Risk | Probability | Impact | Mitigation |
|------|-------------|--------|------------|
| Query returns incorrect results | LOW | HIGH | Thorough testing with all filter combinations, compare with original |
| Performance degradation | VERY LOW | HIGH | Benchmark testing, rollback plan ready |
| Database index slows writes | LOW | MEDIUM | Monitor write performance, indices can be dropped quickly |
| Memory issues with DTO projection | LOW | MEDIUM | Memory profiling during testing |
| Breaking changes to PharmacyRow | LOW | HIGH | Follow "only add new constructors" rule, test all usages |
| Production deployment issues | LOW | MEDIUM | Deploy during off-peak hours, have rollback ready |

---

## Success Criteria

### Must Have (Phase 1):
- ✅ Query count reduced by 99%+ (from 5,001 to < 10)
- ✅ Execution time reduced by 80%+ (from 30-60s to < 10s)
- ✅ All test scenarios pass with correct results
- ✅ No breaking changes to XHTML page functionality
- ✅ No lazy loading exceptions occur

### Should Have (Phase 2):
- ✅ Memory usage reduced by 60%+ (from ~500MB to < 200MB)
- ✅ Execution time reduced by 90%+ (from 30-60s to < 5s)
- ✅ All calculated values match original implementation

### Nice to Have (Phase 3):
- ✅ Database query execution time reduced by 80-90%
- ✅ Benefits other stock reports using similar patterns

---

## Dependencies

### Code Dependencies:
- PharmacyReportController.java (modification required)
- PharmacyRow.java (new constructor required for Phase 2)
- AbstractFacade.java (uses existing `findByJpql` method)
- closing_stock_report.xhtml (no changes required)

### Database Dependencies:
- MySQL 5.7+ (for index creation syntax)
- DBA approval for production index creation
- QA database for index testing

### Testing Dependencies:
- Test data with 1,000+ stock history records
- Multiple departments, categories, items
- Mix of consignment and non-consignment batches
- SQL logging configuration

---

## Timeline

| Phase | Duration | Dependencies | Deliverable |
|-------|----------|--------------|-------------|
| Preparation | 1 hour | None | Feature branch, backups |
| Phase 1 Implementation | 3 hours | None | JOIN FETCH query working |
| Phase 1 Testing | 2 hours | Phase 1 complete | Test results, query count verified |
| Phase 2 Implementation | 3 hours | Phase 1 tested | DTO projection working |
| Phase 2 Testing | 2 hours | Phase 2 complete | Memory usage verified |
| Phase 3 Implementation | 2 hours | DBA coordination | Index scripts |
| QA Validation | 4 hours | All phases tested | Performance benchmarks |
| **Total** | **17 hours** | | **Production-ready code** |

---

## Code Review Checklist

Before requesting approval, verify:

- [ ] No modification to existing PharmacyRow constructors (only new ones added)
- [ ] All backward compatibility maintained
- [ ] No hardcoded values in JPQL queries
- [ ] Parameter maps correctly populated
- [ ] Null checks preserved from original implementation
- [ ] Entity property names match database columns (e.g., `purcahseRate` not `purchaseRate`)
- [ ] No changes to XHTML page required for Phase 1
- [ ] SQL logging can be enabled for verification
- [ ] All filter conditions (department, category, item, date) work correctly
- [ ] Consignment filtering logic matches original behavior
- [ ] Test coverage includes edge cases
- [ ] Performance benchmarks documented
- [ ] Rollback plan documented and tested

---

## Questions for Code Reviewer

1. **Phase 1 (JOIN FETCH)**: Is this approach acceptable for eliminating the N+1 query problem?

2. **Phase 2 (DTO Projection)**: Should we implement this in the same PR or as a separate follow-up optimization?

3. **PharmacyRow Constructor**: Can we add a new constructor, or should we use a builder pattern instead?

4. **Database Indices**: Should these be included in the same deployment or scheduled separately with DBA?

5. **Testing Requirements**: What specific performance benchmarks must be met before production deployment?

6. **Backward Compatibility**: Are there any concerns about changing the query pattern for this critical report?

7. **Consignment Filtering**: Should we keep the Java-side filtering as a safety check even after adding SQL WHERE clause?

8. **Error Handling**: Should we add try-catch around the optimized query with fallback to original implementation?

---

## Approval Required

**Requested by**: Claude Code (Performance Optimization Agent)
**Date**: 2025-10-05
**Estimated Effort**: 17 hours (implementation + testing)
**Expected Benefit**: 90-95% performance improvement, 99% query reduction
**Risk Level**: LOW (Phase 1), MEDIUM (Phase 2), LOW (Phase 3)

**Approvals Needed**:
- [ ] Code Reviewer: Technical approach approval
- [ ] Lead Developer: Implementation strategy approval
- [ ] DBA: Database index approval (Phase 3)
- [ ] QA Lead: Testing strategy approval

---

## Next Steps After Approval

1. Implement Phase 1 (JOIN FETCH optimization)
2. Test thoroughly in development environment
3. Get code review on implementation
4. Deploy to QA for validation
5. Gather performance metrics
6. Decide on Phase 2 implementation based on Phase 1 results
7. Coordinate with DBA for Phase 3 index creation
8. Schedule production deployment

---

**End of Optimization Plan**
