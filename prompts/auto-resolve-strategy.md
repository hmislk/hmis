# Auto-Resolution Strategy for Simple Conflicts

## Safe Auto-Resolution Categories

### 1. Documentation Files (.md) - Use Development Version
**Strategy**: Always take development version for documentation conflicts

**Files to Auto-Resolve**:
- `developer_docs/config/printer-configuration-system.md`
- `docs/wiki/Pharmacy/README.md`
- All other .md files in conflicts

**Command Pattern**:
```bash
# For modify/delete conflicts (file deleted in coop-prod)
git add <file-path>  # Keep development version

# For content conflicts in .md files
git checkout --ours <file-path>  # Use development version
git add <file-path>
```

### 2. Agent Prompts - Use Development Version
**Strategy**: Keep all agent prompts from development

**Files**:
- `agents/prompts/Cashier Summaries.txt`
- `agents/prompts/GRN.txt`

**Reasoning**: These are documentation/guidance files, development version is more recent

### 3. Configuration Files - Manual Review Required

#### .gitignore
**File**: `.gitignore`
**Strategy**: Manual merge required
**Approach**: Combine entries from both branches, remove duplicates

#### SQL Files
**File**: `src/main/webapp/resources/sql/bills.sql`
**Strategy**: Manual review for schema changes
**Approach**: Preserve all DDL changes, merge carefully

## Auto-Resolution Commands

### For Deleted Files (Keep Development Version)
```bash
# Files deleted in coop-prod but modified in development
# Keep them by adding to index
git add agents/prompts/Cashier\ Summaries.txt
git add agents/prompts/GRN.txt
git add developer_docs/config/printer-configuration-system.md
git add docs/wiki/Pharmacy/README.md
git add src/main/java/com/divudi/bean/pharmacy/DirectPurchaseReturnWorkflowController.java
git add src/main/java/com/divudi/bean/pharmacy/GrnReturnWorkflowController.java
git add src/main/java/com/divudi/bean/pharmacy/PharmacyRetailConfigController.java
git add src/main/java/com/divudi/bean/pharmacy/PharmacyStockTakeController.java
git add src/main/java/com/divudi/bean/pharmacy/PurchaseOrderConfigController.java
git add src/main/java/com/divudi/core/data/dto/PharmacyItemPoDTO.java
git add src/main/webapp/pharmacy/pharmacy_report_department_stock_by_batch_dto.xhtml
git add src/main/webapp/pharmacy/pharmacy_stock_take_list.xhtml
git add src/main/webapp/pharmacy/pharmacy_stock_take_print.xhtml
git add src/main/webapp/pharmacy/pharmacy_stock_take_upload.xhtml
git add src/main/webapp/resources/pharmacy/po_custom_2.xhtml
git add src/main/webapp/resources/pharmacy/purchase_order_request_custom_1.xhtml
```

## Manual Review Required (DO NOT AUTO-RESOLVE)

### High Priority Java Files
- `src/main/java/com/divudi/bean/clinical/ClinicalEntityController.java`
- `src/main/java/com/divudi/bean/common/BillSearch.java`
- `src/main/java/com/divudi/bean/common/SearchController.java`
- `src/main/java/com/divudi/bean/pharmacy/PharmacyAdjustmentController.java`
- `src/main/java/com/divudi/bean/pharmacy/PharmacyController.java`
- `src/main/java/com/divudi/bean/pharmacy/PharmacyDirectPurchaseController.java`
- `src/main/java/com/divudi/bean/pharmacy/PharmacyIssueController.java`
- `src/main/java/com/divudi/bean/report/PharmacyReportController.java`
- `src/main/java/com/divudi/service/pharmacy/PharmacyCostingService.java`

### XHTML Files
- `src/main/webapp/pharmacy/adjustments/pharmacy_adjustment_index.xhtml`
- `src/main/webapp/pharmacy/direct_purchase.xhtml`
- `src/main/webapp/pharmacy/pharmacy_purchase_order_list_for_recieve.xhtml`
- `src/main/webapp/pharmacy/pharmacy_purhcase_order_approving.xhtml`
- `src/main/webapp/pharmacy/pharmacy_report_adjustment_bill_item.xhtml`
- `src/main/webapp/resources/pharmacy/history.xhtml`

## Step-by-Step Auto-Resolution Process

### Step 1: Auto-resolve Documentation
```bash
# Keep all development documentation
find . -name "*.md" -type f | xargs git add
```

### Step 2: Auto-resolve Agent Prompts
```bash
# Keep development agent prompts
git add agents/prompts/
```

### Step 3: Keep Deleted Development Files
```bash
# Add all files deleted in coop-prod
git status | grep "deleted by them" | awk '{print $4}' | xargs git add
```

### Step 4: Verify Auto-Resolution
```bash
# Check what's been auto-resolved
git status
```

## What Remains After Auto-Resolution
After running auto-resolution, only critical business logic conflicts should remain:
- Java controller conflicts
- Service layer conflicts
- XHTML UI conflicts
- Configuration file conflicts (.gitignore, SQL)

These require careful manual review and testing.