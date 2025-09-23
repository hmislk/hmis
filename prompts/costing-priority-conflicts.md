# COSTING CONFLICTS - DEVELOPMENT PRIORITY

## CRITICAL RULE FOR COSTING
**ALWAYS take development version for ANY costing-related conflicts**
- Coop-prod is OLD branch
- Development has newer, safer costing logic
- Financial accuracy is critical

## Costing-Related Files to Take from Development

### Service Layer - DEVELOPMENT ONLY
- `src/main/java/com/divudi/service/pharmacy/PharmacyCostingService.java` ✅ **DEVELOPMENT**

### Controllers with Costing Logic - DEVELOPMENT PRIORITY
- `src/main/java/com/divudi/bean/pharmacy/PharmacyController.java` ✅ **DEVELOPMENT**
- `src/main/java/com/divudi/bean/pharmacy/PharmacyDirectPurchaseController.java` ✅ **DEVELOPMENT**
- `src/main/java/com/divudi/bean/pharmacy/PharmacyAdjustmentController.java` ✅ **DEVELOPMENT**
- `src/main/java/com/divudi/bean/pharmacy/GrnCostingController.java` ✅ **DEVELOPMENT**

### Entities with Costing Fields - DEVELOPMENT PRIORITY
- Any Bill/BillItem entities with costing calculations ✅ **DEVELOPMENT**
- Stock/Inventory entities with cost fields ✅ **DEVELOPMENT**

### DTO Files with Costing - DEVELOPMENT PRIORITY
- Any DTO with cost, price, or financial calculations ✅ **DEVELOPMENT**

## Resolution Commands for Costing Conflicts

### For Content Conflicts (UU status)
```bash
# Take development version completely
git checkout --ours src/main/java/com/divudi/service/pharmacy/PharmacyCostingService.java
git add src/main/java/com/divudi/service/pharmacy/PharmacyCostingService.java

git checkout --ours src/main/java/com/divudi/bean/pharmacy/PharmacyController.java
git add src/main/java/com/divudi/bean/pharmacy/PharmacyController.java

git checkout --ours src/main/java/com/divudi/bean/pharmacy/PharmacyDirectPurchaseController.java
git add src/main/java/com/divudi/bean/pharmacy/PharmacyDirectPurchaseController.java

git checkout --ours src/main/java/com/divudi/bean/pharmacy/PharmacyAdjustmentController.java
git add src/main/java/com/divudi/bean/pharmacy/PharmacyAdjustmentController.java
```

## Why Development for Costing?
1. **Newer Branch**: Development has latest costing improvements
2. **Bug Fixes**: Development likely has costing bug fixes not in old coop-prod
3. **DTO Improvements**: Development has better costing DTOs
4. **Performance**: Development has costing performance optimizations
5. **Safety**: Better to have newer costing logic than old potentially buggy logic

## Testing After Resolution
- [ ] Test COGS calculations
- [ ] Test purchase rate calculations
- [ ] Test retail rate calculations
- [ ] Test average cost calculations
- [ ] Test profit margin calculations
- [ ] Test expense allocations
- [ ] Test stock valuation reports

**NOTE**: If any costing tests fail with development version, then investigate specific methods, but DEFAULT is always development.