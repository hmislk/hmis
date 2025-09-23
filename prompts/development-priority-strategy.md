# DEVELOPMENT PRIORITY MERGE STRATEGY

## CORE PRINCIPLE
**Coop-prod is OLD branch → Development is NEWER → Always prefer Development**

## Default Resolution Rules

### 1. Content Conflicts (UU status) - Use Development
```bash
# For any UU conflict, default to development
git checkout --ours <file-path>
git add <file-path>
```

### 2. Deleted Files (DU status) - Keep Development
```bash
# File exists in development, deleted in coop-prod → KEEP
git add <file-path>
```

### 3. New Files (UD status) - Keep Development
```bash
# File new in development → KEEP
git add <file-path>
```

## PRIORITY CATEGORIES (All favor Development)

### HIGHEST PRIORITY - Development Only
1. **ALL costing-related files** (financial accuracy)
2. **ALL .claude/ configuration files** (development tools)
3. **ALL documentation (.md files)** (newer docs)
4. **ALL deleted files** (keep functionality)

### HIGH PRIORITY - Development Preferred
1. **Pharmacy controllers** (newer business logic)
2. **Service layer files** (newer algorithms)
3. **DTO files** (improved data structures)
4. **Entity files** (database improvements)

### MEDIUM PRIORITY - Development Default
1. **XHTML/UI files** (newer UI improvements)
2. **Configuration files** (merge carefully but prefer development)
3. **Search controllers** (performance improvements)

## Auto-Resolution Commands

### Costing Files (Critical - Development Only)
```bash
git checkout --ours src/main/java/com/divudi/service/pharmacy/PharmacyCostingService.java
git checkout --ours src/main/java/com/divudi/bean/pharmacy/PharmacyController.java
git checkout --ours src/main/java/com/divudi/bean/pharmacy/PharmacyDirectPurchaseController.java
git checkout --ours src/main/java/com/divudi/bean/pharmacy/PharmacyAdjustmentController.java
git add src/main/java/com/divudi/service/pharmacy/PharmacyCostingService.java
git add src/main/java/com/divudi/bean/pharmacy/PharmacyController.java
git add src/main/java/com/divudi/bean/pharmacy/PharmacyDirectPurchaseController.java
git add src/main/java/com/divudi/bean/pharmacy/PharmacyAdjustmentController.java
```

### Other Controllers (Development Preferred)
```bash
git checkout --ours src/main/java/com/divudi/bean/pharmacy/PharmacyIssueController.java
git checkout --ours src/main/java/com/divudi/bean/report/PharmacyReportController.java
git checkout --ours src/main/java/com/divudi/bean/common/SearchController.java
git checkout --ours src/main/java/com/divudi/bean/common/BillSearch.java
git checkout --ours src/main/java/com/divudi/bean/clinical/ClinicalEntityController.java
git add src/main/java/com/divudi/bean/pharmacy/PharmacyIssueController.java
git add src/main/java/com/divudi/bean/report/PharmacyReportController.java
git add src/main/java/com/divudi/bean/common/SearchController.java
git add src/main/java/com/divudi/bean/common/BillSearch.java
git add src/main/java/com/divudi/bean/clinical/ClinicalEntityController.java
```

### UI Files (Development Default)
```bash
git checkout --ours src/main/webapp/pharmacy/adjustments/pharmacy_adjustment_index.xhtml
git checkout --ours src/main/webapp/pharmacy/direct_purchase.xhtml
git checkout --ours src/main/webapp/pharmacy/pharmacy_purchase_order_list_for_recieve.xhtml
git checkout --ours src/main/webapp/pharmacy/pharmacy_purhcase_order_approving.xhtml
git checkout --ours src/main/webapp/pharmacy/pharmacy_report_adjustment_bill_item.xhtml
git checkout --ours src/main/webapp/resources/pharmacy/history.xhtml
git add src/main/webapp/pharmacy/adjustments/pharmacy_adjustment_index.xhtml
git add src/main/webapp/pharmacy/direct_purchase.xhtml
git add src/main/webapp/pharmacy/pharmacy_purchase_order_list_for_recieve.xhtml
git add src/main/webapp/pharmacy/pharmacy_purhcase_order_approving.xhtml
git add src/main/webapp/pharmacy/pharmacy_report_adjustment_bill_item.xhtml
git add src/main/webapp/resources/pharmacy/history.xhtml
```

## Configuration Files (Manual Review Needed)

### .gitignore - Merge Both
- Need to combine entries from both branches
- Remove duplicates
- Keep all valid ignore patterns

### bills.sql - Development Preferred
- Development likely has newer schema changes
- Check for any critical schema in coop-prod
- Default to development if unsure

## When to Consider Coop-Prod
**ONLY in these rare cases:**
1. Development version has obvious compilation errors
2. Development version breaks critical functionality
3. Coop-prod has a specific bug fix not in development

**Even then**: Try to fix development version rather than use coop-prod

## Testing Priority
1. **Costing calculations** (highest risk)
2. **Pharmacy workflows** (business critical)
3. **Search functionality** (user experience)
4. **UI rendering** (visual verification)

## Safety Notes
- **Make notes** of any deviations from development-first strategy
- **Test thoroughly** after each resolution
- **Document** any business logic decisions
- **Backup** before starting resolution