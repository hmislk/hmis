# Deleted Files Decision Matrix

## Files Deleted in Coop-Prod but Modified in Development

**DECISION OVERRIDE**: Since coop-prod is an OLD branch, KEEP ALL files that exist in development.
**Default Action**: Use development version for all deleted files.

### Category 1: Workflow Controllers (High Impact)

#### DirectPurchaseReturnWorkflowController.java
**Path**: `src/main/java/com/divudi/bean/pharmacy/DirectPurchaseReturnWorkflowController.java`
**Decision**: KEEP (development version)
**Reasoning**: Coop-prod is old branch, development has newer features
**Action**: Auto-add to keep development version

#### GrnReturnWorkflowController.java
**Path**: `src/main/java/com/divudi/bean/pharmacy/GrnReturnWorkflowController.java`
**Decision Required**: Keep or Remove?
**Analysis**:
- GRN return workflow controller
- Critical for return processing
- Check if functionality moved to GrnController or similar

**Action**: Verify if GRN return methods exist elsewhere before removing

#### PharmacyStockTakeController.java
**Path**: `src/main/java/com/divudi/bean/pharmacy/PharmacyStockTakeController.java`
**Decision Required**: Keep or Remove?
**Analysis**:
- Stock take functionality controller
- Important for inventory management
- Related XHTML files also deleted

**Action**: Check if stock take functionality was moved to another controller

### Category 2: Configuration Controllers

#### PharmacyRetailConfigController.java
**Path**: `src/main/java/com/divudi/bean/pharmacy/PharmacyRetailConfigController.java`
**Decision Required**: Keep or Remove?
**Analysis**:
- Retail configuration controller
- May have been merged into main PharmacyController
- Check for retail config methods elsewhere

#### PurchaseOrderConfigController.java
**Path**: `src/main/java/com/divudi/bean/pharmacy/PurchaseOrderConfigController.java`
**Decision Required**: Keep or Remove?
**Analysis**:
- PO configuration controller
- Check if config functionality moved to PurchaseOrderController

### Category 3: DTOs

#### PharmacyItemPoDTO.java
**Path**: `src/main/java/com/divudi/core/data/dto/PharmacyItemPoDTO.java`
**Decision Required**: Keep or Remove?
**Analysis**:
- Purchase Order DTO
- Check for references in codebase
- If still used, KEEP; if replaced, REMOVE

### Category 4: XHTML Pages

#### Stock Take Pages
- `pharmacy_stock_take_list.xhtml`
- `pharmacy_stock_take_print.xhtml`
- `pharmacy_stock_take_upload.xhtml`

**Decision**: If PharmacyStockTakeController.java is kept, these should be kept too

#### Report Pages
- `pharmacy_report_department_stock_by_batch_dto.xhtml`

**Decision**: Check if report functionality exists elsewhere

#### Custom Components
- `po_custom_2.xhtml`
- `purchase_order_request_custom_1.xhtml`

**Decision**: Check if these custom components are referenced anywhere

### Category 5: Agent Prompts

#### Cashier Summaries.txt
**Path**: `agents/prompts/Cashier Summaries.txt`
**Decision**: Keep (development version) - these are documentation

#### GRN.txt
**Path**: `agents/prompts/GRN.txt`
**Decision**: Keep (development version) - these are documentation

## Decision Workflow

### Step 1: Search for References
For each deleted file, search the codebase for:
- Class name references
- Import statements
- Method calls
- XHTML page navigation

### Step 2: Functionality Check
- Verify if functionality was moved to another class
- Check if features were deprecated/removed intentionally
- Look for replacement implementations

### Step 3: Business Impact Assessment
- Critical functionality: KEEP
- Deprecated/replaced functionality: REMOVE
- Configuration-only: Check if config moved elsewhere

### Step 4: Testing Requirements
For files decided to KEEP:
- [ ] Test the functionality works
- [ ] Check for compilation errors
- [ ] Verify UI pages load correctly
- [ ] Test complete workflows

## FINAL DECISION
**KEEP ALL FILES** that exist in development but are deleted in coop-prod.

**Reasoning**:
- Coop-prod is OLD branch
- Development has newer features and fixes
- Safer to keep functionality than lose it
- Can remove later if truly deprecated after testing