# BigDecimal Migration Strategy
## HMIS BigDecimal Refactoring Initiative - Phase 5

### Overview
This document outlines the data migration strategy for converting BigDecimal fields from NOT NULL to nullable in the HMIS database schema, supporting the BigDecimal refactoring initiative (Issue #12437).

### Migration Approach: Conservative Zero Preservation

#### **Strategy Selected: Leave Existing Zeros As-Is**

We have chosen the **conservative approach** of leaving all existing BigDecimal.ZERO values (0.00) in the database unchanged. This decision is based on:

1. **Data Integrity**: Preserves all historical financial data without semantic changes
2. **Backward Compatibility**: Ensures existing reports and calculations remain consistent
3. **Reduced Risk**: Minimizes potential data corruption during migration
4. **Business Continuity**: Avoids disruption to ongoing financial operations

### Migration Scripts

#### 1. **01_bigdecimal_nullable_billfinancedetails.sql**
- Modifies **36 BigDecimal fields** in BillFinanceDetails table
- Changes column constraints from NOT NULL to nullable
- Covers: discounts, expenses, costs, taxes, values, quantities, totals

#### 2. **02_bigdecimal_nullable_billitemfinancedetails.sql**  
- Modifies **56 BigDecimal fields** in BillItemFinanceDetails table
- Changes column constraints from NOT NULL to nullable
- **Excludes** `unitsPerPack` field (maintains NOT NULL with BigDecimal.ONE default)
- Covers: rates, totals, discounts, taxes, expenses, costs, quantities, returns

### Database Schema Changes

#### **Before Migration:**
```sql
-- Example field constraints
billDiscount DECIMAL(18,4) NOT NULL DEFAULT 0.0000
quantity DECIMAL(18,4) NOT NULL DEFAULT 0.0000
```

#### **After Migration:**
```sql
-- Updated field constraints  
billDiscount DECIMAL(18,4) NULL
quantity DECIMAL(18,4) NULL
```

### JPA Entity Updates

#### **Annotation Changes:**
- **Before**: `@Column(precision = 18, scale = 4)`
- **After**: `@Column(precision = 18, scale = 4, nullable = true)`

#### **Special Case - unitsPerPack:**
- **Remains**: `@Column(precision = 18, scale = 4, nullable = false)`
- **Rationale**: Business-critical conversion factor, must never be null
- **Default Value**: `BigDecimal.ONE` maintained

### Data Handling Post-Migration

#### **Application Layer:**
- **BigDecimalUtil** methods handle null values gracefully
- `valueOrZero()` converts null to BigDecimal.ZERO for calculations
- `isNullOrZero()` treats both null and zero as equivalent for validation

#### **Existing Data:**
- **Zero Values (0.00)**: Remain as stored, treated as valid data
- **Future Nulls**: New records may have null values where no data exists
- **Calculations**: Both null and zero handled identically by utility methods

### Alternative Approach (Not Implemented)

#### **Zero-to-NULL Conversion** (Available but not chosen):
```sql
-- Example of alternative approach
UPDATE BillFinanceDetails SET billDiscount = NULL WHERE billDiscount = 0;
UPDATE BillItemFinanceDetails SET quantity = NULL WHERE quantity = 0;
```

**Why Not Chosen:**
- Semantic ambiguity: Does zero mean "no discount" or "0% discount"?
- Risk of data misinterpretation in legacy reports
- Complex business logic to determine which zeros are meaningful
- Potential inconsistency in financial reporting

### Migration Execution Plan

#### **Phase 1: Pre-Migration**
1. **Database Backup**: Full backup of production database
2. **Development Testing**: Execute scripts on development environment
3. **Data Validation**: Verify existing data integrity
4. **Application Testing**: Confirm BigDecimalUtil functionality

#### **Phase 2: Migration Execution**
1. **Schedule Maintenance Window**: During low-activity period
2. **Execute Scripts**: Run migration scripts in sequence
   - 01_bigdecimal_nullable_billfinancedetails.sql
   - 02_bigdecimal_nullable_billitemfinancedetails.sql
3. **Verify Schema**: Confirm column constraints updated
4. **Smoke Testing**: Validate critical financial workflows

#### **Phase 3: Post-Migration**
1. **Application Deployment**: Deploy updated entities with new annotations
2. **Monitoring**: Monitor application logs for null-related issues
3. **Data Validation**: Verify financial calculations remain consistent
4. **Performance Check**: Ensure no performance degradation

### Rollback Strategy

#### **Schema Rollback:**
```sql
-- Emergency rollback commands
ALTER TABLE BillFinanceDetails ALTER COLUMN billDiscount SET NOT NULL DEFAULT 0.0000;
-- (Apply to all modified columns)
```

#### **Application Rollback:**
- Revert JPA entity annotations to remove `nullable = true`
- Restore previous entity versions without BigDecimalUtil integration

### Testing Checklist

- [ ] Migration scripts execute without errors
- [ ] All column constraints updated correctly
- [ ] Existing data remains intact and accessible
- [ ] Application starts successfully with new entities
- [ ] Financial transaction workflows function correctly
- [ ] Reports generate consistent results
- [ ] No null pointer exceptions in BigDecimal operations
- [ ] Performance benchmarks remain within acceptable ranges

### Success Criteria

1. **Schema Consistency**: Database schema matches updated JPA entity expectations
2. **Data Integrity**: All existing financial data remains accurate and accessible
3. **Application Stability**: No runtime errors related to BigDecimal null handling
4. **Business Continuity**: All financial workflows continue to operate correctly
5. **Performance**: No significant performance degradation in database operations

### Risk Mitigation

#### **Identified Risks:**
- Schema migration failures
- Data corruption during conversion
- Application compatibility issues
- Performance impact

#### **Mitigation Measures:**
- Comprehensive testing in development environment
- Full database backups before migration
- Rollback procedures documented and tested
- Gradual deployment with monitoring
- Conservative approach preserving existing data semantics

---

**Document Version:** 1.0  
**Last Updated:** 2025-07-21  
**Author:** Claude AI Assistant  
**Review Status:** Ready for Implementation