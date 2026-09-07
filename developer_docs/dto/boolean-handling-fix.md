# DTO Boolean Handling Fix

## Problem
DTO constructors using JPQL queries with `COALESCE(boolean_field, false)` were failing silently due to database-specific boolean representation. The issue occurred because:

1. **Database Storage**: Boolean fields (`cancelled`, `refunded`) are stored as `TINYINT(1)` or similar numeric types
2. **JPQL COALESCE**: Returns numeric values (0/1) instead of Boolean objects
3. **Constructor Casting**: Direct casting `(Boolean) numericValue` throws `ClassCastException`
4. **Silent Failure**: `findLightsByJpql()` catches all exceptions and returns empty list

## Root Cause Analysis
```java
// FAILED: Direct casting of numeric boolean values
this.cancelled = cancelled != null ? (Boolean) cancelled : false;
// Throws ClassCastException when cancelled = 0 or 1 (Integer)
```

## Solution
Handle both Boolean objects and numeric boolean values safely:

```java
// FIXED: Safe handling of both Boolean and numeric values
if (cancelled instanceof Boolean) {
    this.cancelled = (Boolean) cancelled;
} else if (cancelled instanceof Number) {
    this.cancelled = ((Number) cancelled).intValue() != 0;
} else {
    this.cancelled = false;
}
```

## Implementation Pattern
For any DTO constructor parameter that may contain boolean values from COALESCE:

```java
public PharmacyTransferIssueDTO(Long billId, Object cancelled, Object refunded, ...) {
    this.billId = billId;
    
    // Handle Boolean/numeric conversion
    if (cancelled instanceof Boolean) {
        this.cancelled = (Boolean) cancelled;
    } else if (cancelled instanceof Number) {
        this.cancelled = ((Number) cancelled).intValue() != 0;
    } else {
        this.cancelled = false;
    }
    
    if (refunded instanceof Boolean) {
        this.refunded = (Boolean) refunded;
    } else if (refunded instanceof Number) {
        this.refunded = ((Number) refunded).intValue() != 0;
    } else {
        this.refunded = false;
    }
}
```

## Debugging Approach Used
When DTOs fail silently, use progressive constructor testing:

1. **Basic Constructor**: Test core fields without joins
2. **Department Constructor**: Test entity relationships  
3. **Staff Constructor**: Test complex joins
4. **Financial Constructor**: Test BigDecimal conversions

This isolates the exact cause of constructor failures.

## Key Learnings
1. **Database boolean fields** may be stored as numeric types
2. **JPQL COALESCE** preserves the underlying data type
3. **DTO constructors** must handle type variations gracefully
4. **Silent failures** in `findLightsByJpql()` require systematic debugging
5. **Always test** DTO constructors with real database data

## Prevention
- Use `Object` parameter types for COALESCE results
- Implement safe type checking in constructors
- Add comprehensive logging during development
- Test DTOs with actual database values, not mock data

---
*This fix resolves the silent failure of pharmacy transfer issue bill DTO queries by properly handling database-specific boolean representations.*