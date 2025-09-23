# Service Layer Conflicts Resolution

## Critical Service Layer Conflicts

### 1. PharmacyCostingService.java
**File**: `src/main/java/com/divudi/service/pharmacy/PharmacyCostingService.java`
**Impact**: Core costing calculations - HIGHEST PRIORITY

**Resolution Strategy**:
1. **PRIORITIZE DEVELOPMENT VERSION** - coop-prod is old branch
2. **Take development version for ALL costing methods** - newer = safer
3. **Only merge coop-prod if development is clearly broken**
4. **Test extensively** - but development has priority

**Key Areas to Examine**:
- Cost calculation algorithms
- FIFO/LIFO logic
- Average cost calculations
- Margin calculations
- Stock valuation methods

**Merge Priority**:
- **Development: HIGHEST PRIORITY** (newer branch)
- Coop-Prod: Old branch, use only if development missing critical fixes
- **Default**: Use development version for all costing conflicts

**Testing Required After Merge**:
- [ ] Test COGS calculations
- [ ] Verify stock valuation accuracy
- [ ] Check profit margin calculations
- [ ] Test cost center allocations
- [ ] Validate expense costing logic

### 2. Related Service Files (No Conflicts but Related)

#### PharmacyCalculation.java
**Status**: Auto-merged successfully
**Note**: Monitor for side effects from PharmacyCostingService changes

#### PharmacyBean.java
**Status**: Auto-merged successfully
**Note**: Contains calculation methods that interact with costing service

## Implementation Steps

### Step 1: Backup Current Costing Logic
```bash
# Create backup of current PharmacyCostingService
cp src/main/java/com/divudi/service/pharmacy/PharmacyCostingService.java PharmacyCostingService.backup
```

### Step 2: Analyze Conflicts
1. Compare method signatures
2. Identify new methods in each branch
3. Look for algorithm changes in existing methods
4. Check for performance improvements

### Step 3: Merge Strategy
1. **Add all new methods** from both branches
2. **For modified methods**:
   - If logic improvement: Use the improved version
   - If bug fix: Combine both fixes
   - If performance optimization: Test both and use better performer

### Step 4: Validation
1. **Unit Test Coverage**: Ensure all costing methods have tests
2. **Integration Testing**: Test with real pharmacy workflows
3. **Performance Testing**: Verify no regression in calculation speed
4. **Accuracy Testing**: Compare results with known good data

## Critical Costing Methods to Review

### Cost Calculation Methods
- `calculateItemCost()`
- `calculateStockValue()`
- `updateCostingData()`
- `calculateAverageCost()`

### COGS Methods
- `calculateCOGS()`
- `updateCOGSForSale()`
- `updateCOGSForReturn()`
- `calculateConsumptionCost()`

### Expense Allocation
- `allocateExpenses()`
- `calculateExpensePerUnit()`
- `updateCostWithExpenses()`

## Red Flags to Watch For
- Changes to decimal precision in calculations
- Modifications to rounding logic
- New validation rules that might reject valid data
- Performance regressions in cost calculations
- Thread safety issues in calculation methods

## Post-Merge Verification Checklist
- [ ] All existing unit tests pass
- [ ] New functionality tests added
- [ ] Performance benchmarks maintained
- [ ] No calculation accuracy regressions
- [ ] Proper error handling maintained
- [ ] Logging preserved for audit trail
- [ ] Thread safety maintained