# User Settings Implementation Guide

## Overview

This guide provides comprehensive instructions for implementing user-specific settings in HMIS pages, particularly for column visibility and other user preferences. These settings are persistent and user-specific, allowing each user to customize their UI according to their preferences.

## 🚨 Version 2.0 - Battle-Tested Implementation

**Key Updates Based on Real-World Experience**:
- ✅ **Critical synchronization fix** - Resolved checkbox/column property binding mismatch
- ✅ **Enhanced troubleshooting** - Added solutions for the most common production issues
- ✅ **Comprehensive verification checklist** - Step-by-step validation process
- ✅ **Real-world debugging examples** - Based on actual pharmacy GRN implementation
- ✅ **Common gotchas section** - Prevent the pitfalls we encountered

**🎯 Production Validated**: This pattern has been successfully implemented and debugged in the pharmacy GRN return request page, fixing critical JSF EL expression errors and AJAX update issues.

## Architecture

### Components Overview

1. **UserSettingsController** - Session-scoped controller managing user preferences
2. **ConfigOption Entity** - Database storage for settings with hierarchical scope
3. **ColumnVisibilitySettings DTO** - Structured data for UI preferences
4. **JSF Property Binding Pattern** - Getter/setter pairs for UI component binding

### Key Features

- **Session-scoped caching** - Settings loaded once at login for performance
- **Persistent storage** - Settings saved to database automatically
- **Hierarchical fallback** - User → Department → Institution → Application scope
- **JSON serialization** - Complex settings stored as JSON in database
- **Automatic persistence** - Changes saved immediately when user modifies settings

## Implementation Steps

### Step 1: Backend - Add Property Methods to UserSettingsController

For each page that needs user-specific column visibility, add getter/setter pairs to `UserSettingsController.java`:

```java
// Example for pharmacy_grn_return_request page
public boolean isPharmacyGrnReturnRequestSupplierVisible() {
    return isColumnVisible("pharmacy_grn_return_request", "supplier");
}

public void setPharmacyGrnReturnRequestSupplierVisible(boolean visible) {
    ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_return_request");
    settings.setColumnVisible("supplier", visible);
    saveColumnVisibility("pharmacy_grn_return_request", settings);
}
```

**🚨 CRITICAL PATTERN**:
- **Naming Convention**: `[PageName][ColumnName]Visible` (camelCase)
- **Getter**: Calls `isColumnVisible(pageId, columnId)`
- **Setter**: Updates settings and saves automatically
- **No AJAX listeners needed** - JSF handles the binding automatically

#### Complete Implementation Template

```java
// Add these methods to UserSettingsController.java

// Page: [your_page_name] - Column: [column_name]
public boolean is[PageName][ColumnName]Visible() {
    return isColumnVisible("[page_id]", "[column_id]");
}

public void set[PageName][ColumnName]Visible(boolean visible) {
    ColumnVisibilitySettings settings = getColumnVisibility("[page_id]");
    settings.setColumnVisible("[column_id]", visible);
    saveColumnVisibility("[page_id]", settings);
}
```

**Real Examples**:
```java
// Supplier column for pharmacy GRN return request
public boolean isPharmacyGrnReturnRequestSupplierVisible() {
    return isColumnVisible("pharmacy_grn_return_request", "supplier");
}

public void setPharmacyGrnReturnRequestSupplierVisible(boolean visible) {
    ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_return_request");
    settings.setColumnVisible("supplier", visible);
    saveColumnVisibility("pharmacy_grn_return_request", settings);
}

// PO Number column
public boolean isPharmacyGrnReturnRequestPoNoVisible() {
    return isColumnVisible("pharmacy_grn_return_request", "poNo");
}

public void setPharmacyGrnReturnRequestPoNoVisible(boolean visible) {
    ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_grn_return_request");
    settings.setColumnVisible("poNo", visible);
    saveColumnVisibility("pharmacy_grn_return_request", settings);
}
```

### Step 2: Frontend - XHTML Implementation

#### 2.1 JSF Component Pattern

Use `p:selectBooleanCheckbox` with proper property binding:

```xml
<p:selectBooleanCheckbox value="#{userSettingsController.[propertyName]}"
                         itemLabel="[Column Display Name]">
    <p:ajax update="[tableId]" />
</p:selectBooleanCheckbox>
```

**✅ CORRECT Examples**:
```xml
<!-- Supplier column checkbox -->
<p:selectBooleanCheckbox value="#{userSettingsController.pharmacyGrnReturnRequestSupplierVisible}"
                         itemLabel="Supplier">
    <p:ajax update="tblBills" />
</p:selectBooleanCheckbox>

<!-- PO Number column checkbox -->
<p:selectBooleanCheckbox value="#{userSettingsController.pharmacyGrnReturnRequestPoNoVisible}"
                         itemLabel="PO No">
    <p:ajax update="tblBills" />
</p:selectBooleanCheckbox>
```

**❌ WRONG - Method Call Approach** (causes "Illegal Syntax for Set Operation"):
```xml
<!-- Don't use method calls - JSF can't set them -->
<p:selectBooleanCheckbox value="#{userSettingsController.isColumnVisible('page', 'column')}"
                         itemLabel="Column">
    <p:ajax listener="#{userSettingsController.toggleColumnVisibility('page', 'column')}" />
</p:selectBooleanCheckbox>
```

#### 2.2 Subtle UI Placement Pattern

Place column visibility controls in a **non-prominent location** with **subtle styling**:

```xml
<!-- Place AFTER the main data table, not before -->
</p:dataTable>

<!-- Subtle Column Visibility Controls -->
<div class="mt-3 pt-2" style="border-top: 1px solid #e9ecef;">
    <p:panel toggleable="true" collapsed="true" styleClass="ui-panel-sm">
        <f:facet name="header">
            <span style="font-size: 0.85rem; color: #6c757d;">
                <i class="fas fa-eye me-1" style="font-size: 0.75rem;"></i>
                Customize Columns
            </span>
        </f:facet>
        <div class="d-flex flex-wrap gap-2" style="font-size: 0.8rem;">
            <!-- Column checkboxes here -->
            <p:selectBooleanCheckbox value="#{userSettingsController.pageColumnNameVisible}"
                                     itemLabel="Column Name">
                <p:ajax update="dataTableId" />
            </p:selectBooleanCheckbox>
            <!-- Add more columns as needed -->
        </div>
    </p:panel>
</div>
```

**🎨 Styling Key Points**:
- **Collapsible panel** - Starts collapsed (`collapsed="true"`)
- **Small fonts** - 0.85rem header, 0.8rem content
- **Muted colors** - #6c757d for subtle appearance
- **Subtle icon** - Eye icon instead of prominent columns icon
- **Visual separation** - Border-top to separate from main content
- **Compact layout** - Smaller gaps (gap-2) between checkboxes

#### 2.3 Data Table Column Binding

Use the settings in your data table columns:

```xml
<p:dataTable id="tblBills" value="#{controller.data}" var="item">

    <!-- Conditional columns based on user settings -->
    <p:column headerText="Supplier"
              rendered="#{userSettingsController.pharmacyGrnReturnRequestSupplierVisible}">
        <h:outputText value="#{item.supplier.name}" />
    </p:column>

    <p:column headerText="PO No"
              rendered="#{userSettingsController.pharmacyGrnReturnRequestPoNoVisible}">
        <h:outputText value="#{item.purchaseOrder.deptId}" />
    </p:column>

    <!-- Add more conditional columns -->
</p:dataTable>
```

### Step 3: Database Storage

Settings are automatically stored in the `ConfigOption` table with this structure:

```sql
-- User-specific column visibility setting example
INSERT INTO config_option (
    option_key,           -- "ui.pharmacy_grn_return_request.columns.visibility"
    option_value,         -- JSON: {"columnVisible":{"supplier":true,"poNo":false},...}
    scope,                -- "USER"
    web_user_id,          -- Current user ID
    value_type,           -- "LONG_TEXT" (for JSON)
    created_at,
    created_by_id
);
```

**JSON Structure**:
```json
{
  "columnVisible": {
    "supplier": true,
    "poNo": false,
    "grnNo": true,
    "invoiceNo": false
  },
  "pageSize": 10,
  "sortField": "createdAt",
  "sortOrder": "desc"
}
```

## Common Patterns

### Pattern 1: Simple Column Visibility

For basic show/hide column functionality:

**Backend (UserSettingsController.java)**:
```java
public boolean isPharmacySaleItemCodeVisible() {
    return isColumnVisible("pharmacy_sale", "itemCode");
}

public void setPharmacySaleItemCodeVisible(boolean visible) {
    ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_sale");
    settings.setColumnVisible("itemCode", visible);
    saveColumnVisibility("pharmacy_sale", settings);
}
```

**Frontend (XHTML)**:
```xml
<p:selectBooleanCheckbox value="#{userSettingsController.pharmacySaleItemCodeVisible}"
                         itemLabel="Item Code">
    <p:ajax update="salesTable" />
</p:selectBooleanCheckbox>

<p:column headerText="Item Code"
          rendered="#{userSettingsController.pharmacySaleItemCodeVisible}">
    <h:outputText value="#{item.pharmaceutical.code}" />
</p:column>
```

### Pattern 2: Multiple Related Settings

For pages with many customizable elements:

**Page ID Convention**: Use clear, consistent page identifiers
- `pharmacy_sale` - Pharmacy retail sale
- `pharmacy_grn_return_request` - GRN return requests
- `opd_bill` - OPD billing
- `inward_patient_search` - Inward patient search

**Column ID Convention**: Use descriptive, consistent column identifiers
- `supplier` - Supplier information
- `itemCode` - Item/medicine code
- `batchNo` - Batch number
- `expiry` - Expiry date
- `rate` - Unit rate/price
- `value` - Total value

### Pattern 3: Integration with Existing Controllers

Add settings to existing page controllers without disrupting functionality:

```java
// In your existing controller (e.g., PharmacyGrnController.java)
@Named
@SessionScoped
public class PharmacyGrnController {

    @Inject
    UserSettingsController userSettingsController;  // Add this injection

    // Your existing methods remain unchanged
    public void searchGrns() {
        // Existing functionality
    }

    // No need to add settings methods - they're in UserSettingsController
}
```

## Advanced Features

### Feature 1: Page Size Preferences

```java
// UserSettingsController methods for page size
public Integer getPharmacySalePageSize() {
    return getPageSize("pharmacy_sale", 10); // Default: 10
}

public void savePharmacySalePageSize(Integer pageSize) {
    savePageSize("pharmacy_sale", pageSize);
}
```

### Feature 2: Sort Preferences

```java
// Save user's preferred sort order
public void savePharmacySaleSortPreference(String field, String order) {
    ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_sale");
    settings.setSortField(field);
    settings.setSortOrder(order);
    saveColumnVisibility("pharmacy_sale", settings);
}
```

### Feature 3: Reset to Defaults

```java
// Add reset functionality to your XHTML
<p:commandButton value="Reset to Defaults"
                 action="#{userSettingsController.resetPageSettings('pharmacy_sale')}"
                 styleClass="btn btn-sm btn-outline-secondary">
    <p:ajax update="salesTable,columnSettings" />
</p:commandButton>
```

## Troubleshooting

### 🚨 Problem 1: "Illegal Syntax for Set Operation" (Most Common)

**Error Message**:
```
javax.el.PropertyNotWritableException: /your_page.xhtml @XX,XX value="#{userSettingsController.isColumnVisible('page', 'column')}": Illegal Syntax for Set Operation
```

**Root Cause**: JSF `p:selectBooleanCheckbox` expects a writable property (getter + setter), but you're using a method call that JSF can't write to.

**❌ WRONG - Method Call Pattern**:
```xml
<p:selectBooleanCheckbox value="#{userSettingsController.isColumnVisible('page', 'column')}"
                         itemLabel="Column">
    <p:ajax listener="#{userSettingsController.toggleColumnVisibility('page', 'column')}" />
</p:selectBooleanCheckbox>
```

**✅ CORRECT - Property Binding Pattern**:
```xml
<p:selectBooleanCheckbox value="#{userSettingsController.pageColumnNameVisible}"
                         itemLabel="Column">
    <p:ajax update="dataTableId" />
</p:selectBooleanCheckbox>
```

**✅ Required Backend Implementation**:
```java
// Add these methods to UserSettingsController.java
public boolean isPageColumnNameVisible() {
    return isColumnVisible("page_id", "column_id");
}

public void setPageColumnNameVisible(boolean visible) {
    ColumnVisibilitySettings settings = getColumnVisibility("page_id");
    settings.setColumnVisible("column_id", visible);
    saveColumnVisibility("page_id", settings);
}
```

### 🚨 Problem 2: Checkboxes Work But Columns Don't Update (Critical Synchronization Issue)

**Symptoms**:
- Checkboxes can be clicked without errors
- AJAX updates trigger correctly
- BUT table columns don't actually show/hide
- Settings appear to save but have no visual effect

**Root Cause**: **Mismatch between checkbox property binding and column rendered attributes** - This is the most insidious issue because everything appears to work!

**❌ WRONG - Mismatched References**:
```xml
<!-- Checkbox sets this property -->
<p:selectBooleanCheckbox value="#{userSettingsController.pharmacyGrnReturnRequestSupplierVisible}">
    <p:ajax update="tblBills" />
</p:selectBooleanCheckbox>

<!-- But column checks different method! -->
<p:column headerText="Supplier"
          rendered="#{userSettingsController.isColumnVisible('pharmacy_grn_return_request', 'supplier')}">
```

**✅ CORRECT - Synchronized References**:
```xml
<!-- Checkbox sets this property -->
<p:selectBooleanCheckbox value="#{userSettingsController.pharmacyGrnReturnRequestSupplierVisible}">
    <p:ajax update="tblBills" />
</p:selectBooleanCheckbox>

<!-- Column checks SAME property -->
<p:column headerText="Supplier"
          rendered="#{userSettingsController.pharmacyGrnReturnRequestSupplierVisible}">
```

**🔍 How to Detect This Issue**:
1. Click a checkbox - does the column actually appear/disappear immediately?
2. Refresh the page - do your settings persist correctly?
3. Check browser developer tools - are there any JSF AJAX errors?

**✅ Systematic Fix Process**:
1. **Identify all columns** that should be controlled by user settings
2. **For EACH column**, ensure the `rendered` attribute uses the EXACT same property as the corresponding checkbox
3. **Verify naming consistency** - property names must match exactly (case-sensitive)

### Problem 3: Settings Not Persisting

**Cause**: Missing setter method or incorrect implementation

**✅ Solution**: Ensure setter calls `saveColumnVisibility()`:
```java
public void setPageColumnNameVisible(boolean visible) {
    ColumnVisibilitySettings settings = getColumnVisibility("page_id");
    settings.setColumnVisible("column_id", visible);
    saveColumnVisibility("page_id", settings);  // This line is critical!
}
```

**🔍 Debug Steps**:
1. Check browser network tab - are AJAX calls succeeding?
2. Check server logs - any exceptions during setting save?
3. Verify UserSettingsController is session-scoped and properly injected
4. Test with a simple boolean property first

### Problem 4: AJAX Updates Not Working

**Symptoms**: Checkbox changes but table doesn't refresh

**Common Causes & Solutions**:

**❌ Wrong component ID**:
```xml
<p:ajax update="wrongTableId" />  <!-- Table ID doesn't match -->
<p:dataTable id="correctTableId">
```

**✅ Correct component ID**:
```xml
<p:ajax update="correctTableId" />  <!-- Exact match required -->
<p:dataTable id="correctTableId">
```

**❌ Component not in same form**:
```xml
<h:form id="form1">
    <p:selectBooleanCheckbox>
        <p:ajax update="tableOutsideForm" />  <!-- Won't work! -->
    </p:selectBooleanCheckbox>
</h:form>
<p:dataTable id="tableOutsideForm">  <!-- Outside form -->
```

**✅ Component in same form or use absolute reference**:
```xml
<h:form id="form1">
    <p:selectBooleanCheckbox>
        <p:ajax update=":form2:tableId" />  <!-- Absolute reference -->
    </p:selectBooleanCheckbox>
</h:form>
<h:form id="form2">
    <p:dataTable id="tableId">
</h:form>
```

### Problem 5: Performance Issues

**Cause**: Too many database queries for settings

**✅ Solution**: Settings are cached in session automatically by UserSettingsController. No additional caching needed.

**Warning Signs**:
- Slow page loads after implementing settings
- Multiple database queries in logs during page rendering
- Session memory usage growing excessively

**Performance Debugging**:
```java
// Add to UserSettingsController for debugging
public boolean isPageColumnNameVisible() {
    System.out.println("DEBUG: Checking column visibility for page_column");
    return isColumnVisible("page_id", "column_id");
}
```

## Implementation Verification Checklist

Use this checklist to verify your implementation is correct BEFORE testing:

### ✅ Backend Verification (UserSettingsController.java)

**For EACH column you want to make configurable**:
- [ ] **Getter method exists**: `public boolean is[PageName][ColumnName]Visible()`
- [ ] **Setter method exists**: `public void set[PageName][ColumnName]Visible(boolean visible)`
- [ ] **Setter calls saveColumnVisibility()**: Settings are persisted to database
- [ ] **Naming convention followed**: CamelCase, descriptive names
- [ ] **Method returns correct values**: Test with simple boolean first

**Example naming verification**:
```java
// Page: pharmacy_grn_return_request, Column: supplier
✅ CORRECT: isPharmacyGrnReturnRequestSupplierVisible()
❌ WRONG: isSupplierVisible() (not specific enough)
❌ WRONG: isPharmacyGRNReturnRequestSupplierVisible() (inconsistent casing)
```

### ✅ Frontend Verification (XHTML)

**For EACH checkbox**:
- [ ] **Property binding used**: `value="#{userSettingsController.propertyName}"`
- [ ] **NO method calls**: Avoid `value="#{controller.isColumnVisible(...)}"`
- [ ] **AJAX update specified**: `<p:ajax update="tableId" />`
- [ ] **Correct table ID referenced**: Update target matches table ID exactly

**For EACH corresponding table column**:
- [ ] **SAME property referenced**: `rendered="#{userSettingsController.propertyName}"`
- [ ] **NO method calls in rendered**: Use property, not `isColumnVisible(...)`
- [ ] **Property name matches checkbox exactly**: Case-sensitive match required

**Critical Synchronization Check**:
```xml
<!-- These MUST use the SAME property name -->
<p:selectBooleanCheckbox value="#{userSettingsController.pharmacyGrnReturnRequestSupplierVisible}">
<p:column rendered="#{userSettingsController.pharmacyGrnReturnRequestSupplierVisible}">
                       ↑ MUST MATCH EXACTLY ↑
```

### ✅ Integration Verification

- [ ] **UserSettingsController injected**: Page controller has proper injection
- [ ] **Table has correct ID**: ID referenced in AJAX updates exists
- [ ] **Components in same form**: Or use absolute references for cross-form updates
- [ ] **No JavaScript errors**: Browser console shows no JS errors

### ✅ Testing Protocol

**Phase 1: Basic Functionality**
1. **Load page** - does it render without errors?
2. **Click checkbox** - does column immediately appear/disappear?
3. **Try all checkboxes** - does each one work independently?
4. **Check browser console** - any JavaScript errors?

**Phase 2: Persistence Testing**
1. **Set preferences** - check/uncheck several columns
2. **Refresh page** - do settings persist correctly?
3. **Logout/login** - do settings persist across sessions?
4. **Different user** - do settings isolate correctly?

**Phase 3: Edge Case Testing**
1. **All columns hidden** - does page still work correctly?
2. **Network interruption** - do settings save when connection is restored?
3. **Multiple tabs** - do settings sync across browser tabs?

## Common Gotchas and Pitfalls

### 🚨 Gotcha 1: Case Sensitivity

Property names in EL expressions are **case-sensitive**. These are all different:
- `pharmacyGrnReturnRequestSupplierVisible` ✅
- `PharmacyGrnReturnRequestSupplierVisible` ❌
- `pharmacyGRNReturnRequestSupplierVisible` ❌

### 🚨 Gotcha 2: Form Boundaries

AJAX updates can't cross form boundaries without absolute references:
```xml
<!-- Won't work - different forms -->
<h:form id="settingsForm">
    <p:ajax update="dataTable" />
</h:form>
<h:form id="dataForm">
    <p:dataTable id="dataTable">
</h:form>

<!-- Use absolute reference -->
<p:ajax update=":dataForm:dataTable" />
```

### 🚨 Gotcha 3: Property vs Method Confusion

EL expressions look similar but behave differently:
```xml
#{controller.propertyName}     <!-- Property access - can read AND write -->
#{controller.getPropertyName()} <!-- Method call - can only read -->
#{controller.methodName('param')} <!-- Method with params - can only read -->
```

### 🚨 Gotcha 4: Premature AJAX Updates

Don't update the table before the setting is saved:
```xml
<!-- Wrong order - update fires before setter completes -->
<p:ajax update="table" listener="#{controller.saveSettings()}" />

<!-- Correct - setter called automatically, then update -->
<p:ajax update="table" />
```

## Real-World Debugging Example

**Scenario**: Column visibility checkboxes work but columns don't hide/show

**Investigation Steps**:
1. **Check synchronization**:
   ```bash
   # In your XHTML file, search for mismatches
   grep -n "pharmacyGrnReturnRequestSupplierVisible" your_page.xhtml
   # Should find BOTH checkbox AND column using same property
   ```

2. **Verify property implementation**:
   ```java
   // Add debug logging to getter
   public boolean isPharmacyGrnReturnRequestSupplierVisible() {
       boolean result = isColumnVisible("pharmacy_grn_return_request", "supplier");
       System.out.println("DEBUG: Supplier column visible: " + result);
       return result;
   }
   ```

3. **Test AJAX updates**:
   - Open browser developer tools → Network tab
   - Click checkbox
   - Verify AJAX request is sent and returns success
   - Check console for any JavaScript errors

4. **Verify table refresh**:
   ```xml
   <!-- Add logging to table rendering -->
   <p:column rendered="#{userSettingsController.pharmacyGrnReturnRequestSupplierVisible}">
       <!-- This should appear/disappear when checkbox changes -->
   </p:column>
   ```

**Common Resolution**: Usually the issue is mismatched property names between checkbox and column rendered attributes.

## Security Considerations

### User Isolation
- Settings are automatically isolated by user (web_user_id)
- One user cannot see or modify another user's preferences
- Session-scoped controller ensures user-specific caching

### Data Validation
- Boolean settings are validated automatically by JSF
- JSON serialization is handled safely by the framework
- No additional validation needed for basic column visibility

## Performance Best Practices

### 1. Session Caching
- Settings loaded once at login and cached for entire session
- No database queries on each page request
- Cache cleared only when user explicitly resets settings

### 2. Minimal Database Impact
- Settings stored as single JSON record per page per user
- Hierarchical fallback reduces database size
- Automatic cleanup when user accounts are deleted

### 3. UI Performance
- Use `rendered` attribute for conditional columns (not `style="display:none"`)
- AJAX updates only affect the data table, not entire page
- Collapsible settings panel reduces DOM size when collapsed

## Testing Checklist

### Functional Testing
- [ ] Checkbox changes immediately affect column visibility
- [ ] Settings persist after page refresh
- [ ] Settings persist after user logout/login
- [ ] Multiple users have isolated settings
- [ ] Reset functionality works correctly

### UI Testing
- [ ] Settings panel is subtle and non-intrusive
- [ ] Collapsible panel works correctly
- [ ] Checkbox labels are clear and accurate
- [ ] Mobile responsiveness maintained

### Performance Testing
- [ ] Page load time not significantly affected
- [ ] No additional database queries during normal page usage
- [ ] Session memory usage reasonable with many settings

## Extension Opportunities

### Future Enhancements
1. **Column Order Customization** - Drag-and-drop column reordering
2. **Column Width Preferences** - User-defined column widths
3. **Filter Preferences** - Save commonly used filter combinations
4. **Export Settings** - Allow users to export/import their preferences
5. **Department-wide Defaults** - Admin-defined default preferences for departments

### Integration Points
- **Reporting System** - Apply user column preferences to reports
- **Mobile Views** - Responsive column hiding based on screen size
- **Accessibility** - Integration with screen reader preferences

## Real-World Example: Complete Implementation

Here's a complete example implementing column visibility for a pharmacy sales page:

### UserSettingsController.java additions:
```java
// Pharmacy Sale Column Visibility Properties
public boolean isPharmacySaleItemCodeVisible() {
    return isColumnVisible("pharmacy_sale", "itemCode");
}

public void setPharmacySaleItemCodeVisible(boolean visible) {
    ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_sale");
    settings.setColumnVisible("itemCode", visible);
    saveColumnVisibility("pharmacy_sale", settings);
}

public boolean isPharmacySaleBatchNoVisible() {
    return isColumnVisible("pharmacy_sale", "batchNo");
}

public void setPharmacySaleBatchNoVisible(boolean visible) {
    ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_sale");
    settings.setColumnVisible("batchNo", visible);
    saveColumnVisibility("pharmacy_sale", settings);
}

public boolean isPharmacySaleExpiryVisible() {
    return isColumnVisible("pharmacy_sale", "expiry");
}

public void setPharmacySaleExpiryVisible(boolean visible) {
    ColumnVisibilitySettings settings = getColumnVisibility("pharmacy_sale");
    settings.setColumnVisible("expiry", visible);
    saveColumnVisibility("pharmacy_sale", settings);
}
```

### pharmacy_sale.xhtml implementation:
```xml
<!-- Main data table -->
<p:dataTable id="salesTable" value="#{pharmacyController.saleItems}" var="item">

    <!-- Always visible columns -->
    <p:column headerText="Medicine Name">
        <h:outputText value="#{item.pharmaceutical.name}" />
    </p:column>

    <p:column headerText="Quantity">
        <h:outputText value="#{item.quantity}" />
    </p:column>

    <!-- User-customizable columns -->
    <p:column headerText="Item Code"
              rendered="#{userSettingsController.pharmacySaleItemCodeVisible}">
        <h:outputText value="#{item.pharmaceutical.code}" />
    </p:column>

    <p:column headerText="Batch No"
              rendered="#{userSettingsController.pharmacySaleBatchNoVisible}">
        <h:outputText value="#{item.pharmaceuticalBillItem.stock.batch.batchNo}" />
    </p:column>

    <p:column headerText="Expiry"
              rendered="#{userSettingsController.pharmacySaleExpiryVisible}">
        <h:outputText value="#{item.pharmaceuticalBillItem.stock.batch.expiry}">
            <f:convertDateTime pattern="yyyy-MM-dd" />
        </h:outputText>
    </p:column>

</p:dataTable>

<!-- Subtle column customization controls -->
<div class="mt-3 pt-2" style="border-top: 1px solid #e9ecef;">
    <p:panel toggleable="true" collapsed="true" styleClass="ui-panel-sm">
        <f:facet name="header">
            <span style="font-size: 0.85rem; color: #6c757d;">
                <i class="fas fa-eye me-1" style="font-size: 0.75rem;"></i>
                Customize Columns
            </span>
        </f:facet>
        <div class="d-flex flex-wrap gap-2" style="font-size: 0.8rem;">
            <p:selectBooleanCheckbox value="#{userSettingsController.pharmacySaleItemCodeVisible}"
                                     itemLabel="Item Code">
                <p:ajax update="salesTable" />
            </p:selectBooleanCheckbox>

            <p:selectBooleanCheckbox value="#{userSettingsController.pharmacySaleBatchNoVisible}"
                                     itemLabel="Batch No">
                <p:ajax update="salesTable" />
            </p:selectBooleanCheckbox>

            <p:selectBooleanCheckbox value="#{userSettingsController.pharmacySaleExpiryVisible}"
                                     itemLabel="Expiry Date">
                <p:ajax update="salesTable" />
            </p:selectBooleanCheckbox>
        </div>
    </p:panel>
</div>
```

## Conclusion

This pattern provides a robust, user-friendly way to implement customizable UI preferences in HMIS pages. The implementation has been battle-tested in real HMIS production environments.

### Key Benefits

- **User Experience** - Each user can customize their interface according to their workflow
- **Performance** - Session-scoped caching prevents repeated database queries
- **Maintainability** - Clear separation of concerns between settings and business logic
- **Scalability** - JSON storage allows for complex preference structures
- **Consistency** - Standardized patterns across all HMIS pages
- **Reliability** - Comprehensive troubleshooting guide prevents common pitfalls

### Critical Success Factors

Based on real-world implementation experience:

1. **🚨 Property Synchronization**: Ensure checkbox `value` and column `rendered` attributes use identical properties
2. **⚡ AJAX Configuration**: Proper component IDs and form boundaries are essential
3. **🔍 Systematic Testing**: Follow the verification checklist BEFORE deploying
4. **📋 Naming Consistency**: Use clear, consistent naming conventions for properties
5. **🛡️ Error Prevention**: Use the troubleshooting guide to avoid common pitfalls

### Implementation Recommendations

1. **Start Simple**: Begin with 2-3 columns to validate the pattern works
2. **Test Incrementally**: Add one column at a time, testing each thoroughly
3. **Use Verification Checklist**: Follow the step-by-step checklist for each implementation
4. **Document Property Names**: Keep a mapping of page/column to property names
5. **Monitor Performance**: Verify session memory usage remains reasonable

### Production-Ready Features

This implementation pattern includes:

- ✅ **Real-world validation** - Fixes based on actual production issues encountered
- ✅ **Comprehensive error handling** - Covers all major JSF binding and AJAX scenarios
- ✅ **Performance optimization** - Session caching and minimal database impact
- ✅ **Security considerations** - User isolation and data validation
- ✅ **Extensive troubleshooting** - Battle-tested solutions for common problems
- ✅ **Subtle UI design** - Non-intrusive, collapsible settings panels

### When NOT to Use This Pattern

Consider alternative approaches for:

- **Temporary filtering** - Use client-side JavaScript for temporary column hiding
- **Role-based visibility** - Use privilege system for permanent access control
- **Simple pages** - Don't over-engineer pages with only 2-3 columns total
- **High-frequency changes** - Settings that change multiple times per session

### Support and Maintenance

For ongoing support:

1. **Reference this guide** for troubleshooting steps
2. **Use the verification checklist** when adding new configurable columns
3. **Monitor server logs** for setting-related exceptions
4. **Test across browsers** - ensure AJAX updates work consistently
5. **Update documentation** when adding new patterns or discovering new issues

Follow this guide to implement consistent, performant, and maintainable user settings throughout the HMIS application.

---

**Last Updated**: 2025-11-29
**Version**: 2.0 (Enhanced troubleshooting and real-world validation)
**Author**: HMIS Development Team
**Production Validated**: ✅ Tested in pharmacy GRN return request implementation