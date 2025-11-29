# User Settings Implementation Guide

## Overview

This guide provides comprehensive instructions for implementing user-specific settings in HMIS pages, particularly for column visibility and other user preferences. These settings are persistent and user-specific, allowing each user to customize their UI according to their preferences.

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

### Problem 1: "Illegal Syntax for Set Operation"

**Cause**: Using method calls instead of property binding in JSF components

**❌ Wrong**:
```xml
<p:selectBooleanCheckbox value="#{userSettingsController.isColumnVisible('page', 'column')}" />
```

**✅ Solution**:
```xml
<p:selectBooleanCheckbox value="#{userSettingsController.pageColumnNameVisible}" />
```

### Problem 2: Settings Not Persisting

**Cause**: Missing setter method or incorrect implementation

**✅ Solution**: Ensure setter calls `saveColumnVisibility()`:
```java
public void setPageColumnNameVisible(boolean visible) {
    ColumnVisibilitySettings settings = getColumnVisibility("page_id");
    settings.setColumnVisible("column_id", visible);
    saveColumnVisibility("page_id", settings);  // This line is critical!
}
```

### Problem 3: Checkboxes Not Updating

**Cause**: Missing AJAX update or wrong component ID

**✅ Solution**: Ensure correct update target:
```xml
<p:selectBooleanCheckbox value="#{...}">
    <p:ajax update="tblData" />  <!-- Must match table ID exactly -->
</p:selectBooleanCheckbox>

<p:dataTable id="tblData">  <!-- ID must match update target -->
```

### Problem 4: Performance Issues

**Cause**: Too many database queries for settings

**✅ Solution**: Settings are cached in session automatically by UserSettingsController. No additional caching needed.

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

This pattern provides a robust, user-friendly way to implement customizable UI preferences in HMIS pages. The key benefits are:

- **User Experience** - Each user can customize their interface
- **Performance** - Session-scoped caching prevents repeated database queries
- **Maintainability** - Clear separation of concerns between settings and business logic
- **Scalability** - JSON storage allows for complex preference structures
- **Consistency** - Standardized patterns across all HMIS pages

Follow this guide to implement consistent, performant user settings throughout the HMIS application.

---

**Last Updated**: 2025-11-29
**Version**: 1.0
**Author**: HMIS Development Team