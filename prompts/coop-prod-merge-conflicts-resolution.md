# Coop-Prod to Development Merge Conflict Resolution Guide

## Overview
This document provides step-by-step resolution strategies for the 300+ conflicts arising from merging coop-prod (OLD BRANCH) into development branch.

**IMPORTANT**: Coop-prod is an OLD branch, NOT a production cleanup. Development branch is newer and should be prioritized in most conflicts.

## Conflict Categories

### 1. Auto-Resolvable Conflicts (Take Development Version)
**RULE**: Since coop-prod is OLD, always prefer development version unless specifically noted.

#### Files to ALWAYS take from Development:
- **ALL .md documentation files**
- **ALL .claude/ configuration files** (agent definitions, settings)
- **ALL files deleted in coop-prod but present in development**
- **ALL costing-related files** (financial accuracy priority)
- **ALL new features/controllers in development**

**Resolution Strategy**: Development version is newer and safer.

### 2. Critical Code Conflicts (Requires Manual Review)

#### Java Controller Files
1. **src/main/java/com/divudi/bean/clinical/ClinicalEntityController.java**
   - Type: Content conflict
   - Impact: Clinical functionality
   - Review Required: Check for method signature changes

2. **src/main/java/com/divudi/bean/common/BillSearch.java**
   - Type: Content conflict
   - Impact: Bill search functionality
   - Review Required: Search algorithm changes

3. **src/main/java/com/divudi/bean/common/SearchController.java**
   - Type: Content conflict
   - Impact: Core search functionality
   - Review Required: Search performance optimizations

#### Pharmacy Controllers (High Priority)
4. **src/main/java/com/divudi/bean/pharmacy/PharmacyAdjustmentController.java**
   - Type: Content conflict
   - Impact: Pharmacy adjustments
   - Review Required: Adjustment calculation logic

5. **src/main/java/com/divudi/bean/pharmacy/PharmacyController.java**
   - Type: Content conflict
   - Impact: Core pharmacy operations
   - Review Required: Main pharmacy workflow changes

6. **src/main/java/com/divudi/bean/pharmacy/PharmacyDirectPurchaseController.java**
   - Type: Content conflict
   - Impact: Direct purchase workflow
   - Review Required: Purchase calculation changes

7. **src/main/java/com/divudi/bean/pharmacy/PharmacyIssueController.java**
   - Type: Content conflict
   - Impact: Pharmacy issuing
   - Review Required: Issue workflow modifications

8. **src/main/java/com/divudi/bean/report/PharmacyReportController.java**
   - Type: Content conflict
   - Impact: Pharmacy reporting
   - Review Required: Report generation logic

#### Service Layer
9. **src/main/java/com/divudi/service/pharmacy/PharmacyCostingService.java**
   - Type: Content conflict
   - Impact: Costing calculations
   - Review Required: Cost calculation algorithms

### 3. Deleted Files in Coop-Prod (Modify/Delete Conflicts)
Files that exist in development but were deleted in coop-prod:

#### Java Controllers/Services
- `src/main/java/com/divudi/bean/pharmacy/DirectPurchaseReturnWorkflowController.java`
- `src/main/java/com/divudi/bean/pharmacy/GrnReturnWorkflowController.java`
- `src/main/java/com/divudi/bean/pharmacy/PharmacyRetailConfigController.java`
- `src/main/java/com/divudi/bean/pharmacy/PharmacyStockTakeController.java`
- `src/main/java/com/divudi/bean/pharmacy/PurchaseOrderConfigController.java`

#### DTOs
- `src/main/java/com/divudi/core/data/dto/PharmacyItemPoDTO.java`

#### XHTML Files
- `src/main/webapp/pharmacy/pharmacy_report_department_stock_by_batch_dto.xhtml`
- `src/main/webapp/pharmacy/pharmacy_stock_take_list.xhtml`
- `src/main/webapp/pharmacy/pharmacy_stock_take_print.xhtml`
- `src/main/webapp/pharmacy/pharmacy_stock_take_upload.xhtml`
- `src/main/webapp/resources/pharmacy/po_custom_2.xhtml`
- `src/main/webapp/resources/pharmacy/purchase_order_request_custom_1.xhtml`

**Decision**: KEEP ALL - Development version since coop-prod is old branch.

### 4. XHTML UI Conflicts
1. **src/main/webapp/pharmacy/adjustments/pharmacy_adjustment_index.xhtml**
2. **src/main/webapp/pharmacy/direct_purchase.xhtml**
3. **src/main/webapp/pharmacy/pharmacy_purchase_order_list_for_recieve.xhtml**
4. **src/main/webapp/pharmacy/pharmacy_purhcase_order_approving.xhtml**
5. **src/main/webapp/pharmacy/pharmacy_report_adjustment_bill_item.xhtml**
6. **src/main/webapp/resources/pharmacy/history.xhtml**

### 5. Configuration Files
1. **.gitignore** - Content conflict
2. **src/main/webapp/resources/sql/bills.sql** - Content conflict

### 6. Agent Prompts (Modify/Delete)
- `agents/prompts/Cashier Summaries.txt` (deleted in coop-prod, modified in development)
- `agents/prompts/GRN.txt` (deleted in coop-prod, modified in development)

## Resolution Priority Order

### Phase 1: Critical Business Logic (Do First)
1. PharmacyCostingService.java
2. PharmacyController.java
3. PharmacyDirectPurchaseController.java
4. PharmacyAdjustmentController.java
5. PharmacyIssueController.java

### Phase 2: Search and Reporting
1. SearchController.java
2. BillSearch.java
3. PharmacyReportController.java

### Phase 3: UI Components
1. pharmacy_adjustment_index.xhtml
2. direct_purchase.xhtml
3. pharmacy_purchase_order_list_for_recieve.xhtml
4. pharmacy_purhcase_order_approving.xhtml

### Phase 4: Configuration and Cleanup
1. .gitignore
2. bills.sql
3. Agent prompts
4. Documentation files (auto-resolve with development)

### Phase 5: Deleted File Decisions
Review each deleted file to determine if functionality is still needed.

## Pre-Resolution Checklist
- [ ] Backup current branch
- [ ] Understand business impact of each conflict
- [ ] Test critical pharmacy workflows after resolution
- [ ] Verify costing calculations are preserved
- [ ] Check UI functionality in pharmacy modules
- [ ] Run full test suite before merge completion

## Notes
- All .md documentation conflicts should use development version
- Pharmacy-related conflicts require careful business logic review
- Test thoroughly before finalizing merge