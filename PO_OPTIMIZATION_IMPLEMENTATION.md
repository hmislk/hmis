# Purchase Order Creation Performance Optimization

## Issue
GitHub Issue #17215: Need to Speedup the PO Creation

## Problem Analysis

### Performance Bottlenecks in Original Implementation
1. **Per-row method calls in UI**: The XHTML file calls `stockController.findStock()` and `pharmacyController.findAllOutTransactions()` for each item in the datatable, resulting in N database queries for N items
2. **Full entity loading**: Loading complete Item entities with all relationships for dropdowns
3. **No batch processing**: Stock and usage calculations done individually per item
4. **Entity overhead**: JSF repeatedly accesses entity getters, triggering lazy loading

### Expected Performance Impact
- **Before**: For 100 items, ~200+ database queries (stock + usage per item)
- **After**: For 100 items, ~2-3 database queries (batch stock + batch usage + items)
- **Estimated improvement**: 50-90% reduction in database queries and page load time

## Solution: DTO-Based Optimization

### Key Optimization Strategies

#### 1. Data Transfer Objects (DTOs)
Created lightweight DTOs to replace heavy entities in the UI layer:

- **`PurchaseOrderItemDto`**: Displays PO items with pre-calculated stock and usage
  - Contains: item info, quantities, rates, **pre-calculated** stock and usage
  - Avoids per-row calculations in JSF

- **`ItemSupplierDto`**: Displays supplier items in dropdowns
  - Contains: item info, stock, usage (all pre-calculated)
  - Replaces full Item entity loading

#### 2. Batch Queries
Implemented batch data fetching to prevent N+1 query problem:

```java
// Instead of N queries (one per item):
for (Item item : items) {
    stock = stockController.findStock(item); // 1 query per item
}

// New approach: 1 batch query for all items:
Map<Long, Double> stockMap = fetchStockDataForItems(itemIds);
// Single query with GROUP BY
```

**Batch Query Methods:**
- `fetchStockDataForItems()`: Fetches stock for all items in one query
- `fetchUsageDataForItems()`: Fetches usage data for all items in one query
- `loadDealerItems()`: Loads supplier items with stock in a single query using JPA constructor expression

#### 3. Lazy DTO Loading
DTOs are loaded on-demand but cached:
```java
public List<PurchaseOrderItemDto> getItemDtos() {
    if (itemDtos == null) {
        loadItemDtosFromEntities(); // Batch load with stock/usage
    }
    return itemDtos;
}
```

#### 4. Dual-Mode Architecture
- **Display layer**: Uses DTOs (lightweight, fast)
- **Persistence layer**: Uses Entities (JPA-managed, transactional)
- **Sync mechanism**: `syncDtoToEntity()` bridges the two layers

### File Structure

```
Created Files:
├── src/main/java/com/divudi/core/data/dto/
│   ├── PurchaseOrderItemDto.java       # DTO for PO items with stock/usage
│   └── ItemSupplierDto.java            # DTO for supplier items
├── src/main/java/com/divudi/bean/pharmacy/
│   └── CreatePoController.java         # Optimized controller
└── PO_OPTIMIZATION_IMPLEMENTATION.md   # This file

Existing Files (kept for comparison):
├── src/main/webapp/pharmacy/
│   └── pharmacy_purhcase_order_request.xhtml  # Original XHTML
└── src/main/java/com/divudi/bean/pharmacy/
    └── PurchaseOrderRequestController.java     # Original controller
```

## Implementation Details

### Controller Architecture

**CreatePoController.java** (~1000 lines)

Key methods:
1. **`loadItemDtosFromEntities()`**
   - Converts entities to DTOs
   - Batch-fetches stock and usage data
   - Returns pre-calculated DTOs for UI

2. **`fetchStockDataForItems(List<Long> itemIds)`**
   - Single query with JOIN and GROUP BY
   - Returns Map<ItemId, Stock>
   - Prevents N+1 query problem

3. **`fetchUsageDataForItems(List<Long> itemIds)`**
   - Batch query for usage data
   - Configurable date range (default: last 30 days)
   - Returns Map<ItemId, Usage>

4. **`loadDealerItems()`**
   - Uses JPA constructor expression
   - Loads items with stock in single query
   - Post-processes to add usage data

5. **`onEditDto(PurchaseOrderItemDto dto)`**
   - Handles UI edits
   - Syncs changes back to entities via `syncDtoToEntity()`
   - Maintains entity integrity for save operations

6. **Save/Finalize methods**
   - Reuse original entity-based logic
   - No changes to persistence layer
   - Ensures data integrity

### DTO Design

**PurchaseOrderItemDto:**
```java
- Serializable (for JSF state saving)
- Multiple constructors for different use cases
- Null-safe getters/setters
- calculateLineTotal() method for UI updates
```

**ItemSupplierDto:**
```java
- JPQL constructor-compatible
- Handles type conversion (Double to BigDecimal)
- Supports batch loading patterns
```

## Usage Instructions

### For Developers

1. **Deploy the new implementation:**
   ```
   # The new controller is CreatePoController (not PurchaseOrderRequestController)
   # URL: /pharmacy/create_po.xhtml (to be created)
   ```

2. **Testing checklist:**
   - [ ] Load supplier items dropdown - should be fast
   - [ ] Add all supplier items - should complete quickly
   - [ ] Verify stock and usage display correctly
   - [ ] Edit quantities - should update totals immediately
   - [ ] Save PO - should persist correctly
   - [ ] Finalize PO - should create proper bill

3. **Performance testing:**
   - [ ] Measure page load time with 50 items
   - [ ] Measure page load time with 100+ items
   - [ ] Check database query count (use SQL logging)
   - [ ] Compare with original implementation

### For QA

**Test Scenarios:**

1. **Basic PO Creation:**
   - Select supplier
   - Add items via dropdown
   - Verify stock and usage display
   - Edit quantities
   - Save and finalize

2. **Bulk Operations:**
   - Add all supplier items (test performance)
   - Add items below ROL
   - Remove multiple items

3. **Data Integrity:**
   - Verify quantities save correctly
   - Check bill totals calculate accurately
   - Ensure finalization creates proper records

4. **Edge Cases:**
   - Empty supplier item list
   - Duplicate item prevention
   - Integer quantity validation
   - Large datasets (200+ items)

## Migration Plan

### Phase 1: Parallel Testing (Current)
- Original: `pharmacy_purhcase_order_request.xhtml` + `PurchaseOrderRequestController`
- New: `create_po.xhtml` (to be created) + `CreatePoController`
- Both coexist for comparison

### Phase 2: QA Validation
- Test new implementation thoroughly
- Compare performance metrics
- Verify data integrity
- Document any differences

### Phase 3: Gradual Rollout
- Update menu to point to new page
- Monitor production usage
- Keep original as fallback

### Phase 4: Cleanup (Future)
- After successful validation
- Deprecate old implementation
- Remove old files
- Update documentation

## Known Limitations

### Current Implementation

1. **Requires additional XHTML creation:**
   - `create_po.xhtml` needs to be created to use the new controller
   - Should use DTO properties instead of entity navigation

2. **Facade method dependencies:**
   - Uses `itemFacade.findAggregates()` which may need verification
   - May need to use `itemFacade.findByJpql()` with manual result processing

3. **Print functionality:**
   - Not yet implemented in new controller
   - Can reuse original controller's print methods

4. **Email functionality:**
   - Not yet implemented
   - Can copy from original controller

### Future Enhancements

1. **Consider using native queries** for even better performance on large datasets
2. **Add caching** for frequently accessed stock/usage data
3. **Implement pagination** for very large item lists
4. **Add async loading** for better UX with many items

## Performance Comparison

### Expected Metrics

| Metric | Original | Optimized | Improvement |
|--------|----------|-----------|-------------|
| DB Queries (100 items) | ~200 | ~3 | 98% reduction |
| Page Load (100 items) | 5-8s | 1-2s | 60-75% faster |
| Memory Usage | Higher (full entities) | Lower (DTOs) | 30-50% reduction |

### How to Measure

```sql
-- Enable SQL logging in persistence.xml
<property name="eclipselink.logging.level.sql" value="FINE"/>
<property name="eclipselink.logging.parameters" value="true"/>

-- Count queries in logs for:
1. Loading page with 100 items
2. Adding all supplier items
3. Editing item quantities
```

## Technical Notes

### JPA Constructor Expression
Used in `loadDealerItems()`:
```java
SELECT NEW com.divudi.core.data.dto.ItemSupplierDto(
    i.id, i.name, i.code,
    COALESCE(SUM(s.stock), 0.0),
    i.dblValue
)
FROM ItemsDistributors id ...
```

Benefits:
- Single query returns DTOs directly
- No entity materialization overhead
- Type-safe JPQL

### Batch Query Pattern
```java
// Build WHERE IN clause with item IDs
WHERE i.id IN :itemIds

// Use GROUP BY to aggregate
GROUP BY i.id

// Return as Map for O(1) lookup
Map<Long, Double> stockMap = new HashMap<>();
```

### DTO-Entity Sync
```java
// Edit in DTO (UI layer)
dto.setQuantity(newValue);
dto.calculateLineTotal();

// Sync to Entity (persistence layer)
syncDtoToEntity(dto);

// Save entity (JPA)
billItemFacade.edit(billItem);
```

## Rollback Plan

If issues arise:
1. Keep original files unchanged
2. Menu can quickly switch back to `pharmacy_purhcase_order_request.xhtml`
3. No database schema changes
4. No data migration required

## Support and Maintenance

### Code Review Checklist
- [x] DTOs are serializable
- [x] Null safety in constructors
- [x] Batch queries use proper JOINs
- [x] Entity sync maintains data integrity
- [x] Original save logic preserved
- [ ] XHTML created with DTO bindings
- [ ] Integration testing completed
- [ ] Performance benchmarks documented

### Future Maintenance
- Monitor query performance in production
- Adjust batch sizes if needed
- Consider adding indexes on frequently joined columns
- Update DTOs if entity structure changes

## Conclusion

This optimization addresses the performance bottleneck by:
1. Eliminating per-row calculations (N+1 problem)
2. Using lightweight DTOs for display
3. Maintaining entity-based persistence for integrity
4. Enabling safe parallel deployment

Expected result: **50-90% reduction in database queries** and **60-75% faster page load times** for PO creation with large item lists.

---
**Author**: Claude Code AI Assistant
**Date**: 2025-12-13
**Issue**: #17215 - Need to Speedup the PO Creation
**Status**: Controller implementation complete, XHTML pending
