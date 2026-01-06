# PrimeFaces DataTable Selection - Points to Note

## Overview
PrimeFaces DataTable selection behavior has evolved in newer versions. This document outlines the correct implementation patterns for multiple row selection with checkboxes.

## Multiple Selection Implementation

### Correct Pattern (Current PrimeFaces)

```xml
<p:dataTable
    id="myTable"
    value="#{controller.items}"
    var="item"
    selection="#{controller.selectedItems}"
    rowKey="#{item.id}"
    selectionMode="multiple"
    selectAllFilteredOnly="true"
    selectionPageOnly="false"
    paginator="true"
    rows="20">

    <!-- Selection column with checkboxes -->
    <p:column selectionBox="true" style="width: 60px;" />

    <!-- Other columns -->
    <p:column headerText="Name">
        <h:outputText value="#{item.name}" />
    </p:column>

</p:dataTable>
```

### Key Attributes

#### On `<p:dataTable>` Element:

1. **`selection="#{controller.selectedItems}"`**
   - Binds to an array property in the controller
   - Type should be `YourDTO[]` (array, not List)
   - Example: `private ChannelHospitalFeeDTO[] selectedItems;`

2. **`rowKey="#{item.id}"`**
   - **REQUIRED** for selection to work
   - Must be a unique identifier for each row
   - Used internally by PrimeFaces to track selected rows

3. **`selectionMode="multiple"`**
   - Enables multiple row selection
   - This attribute goes on the **dataTable**, not the column
   - Adds checkbox functionality to selection column

4. **`selectAllFilteredOnly="true"`**
   - When `true`: Header checkbox selects only filtered/visible rows
   - When `false`: Header checkbox selects all rows (including filtered-out ones)
   - **Recommended**: `true` for better user experience

5. **`selectionPageOnly="false"`**
   - When `true`: Selection is limited to current page only
   - When `false`: Selection works across all pages
   - **Recommended**: `false` for bulk operations across dataset

#### On `<p:column>` Element:

1. **`selectionBox="true"`**
   - Creates the selection column with checkboxes
   - Automatically adds header checkbox for "select all"
   - This replaces the old `selectionMode="multiple"` on column

### Backend Controller Pattern

```java
// Selection property (must be array)
private YourDTO[] selectedItems;

// Getter and setter
public YourDTO[] getSelectedItems() {
    return selectedItems;
}

public void setSelectedItems(YourDTO[] selectedItems) {
    this.selectedItems = selectedItems;
}

// Method to process selected items
public void processSelectedItems() {
    if (selectedItems == null || selectedItems.length == 0) {
        JsfUtil.addErrorMessage("Please select at least one item");
        return;
    }

    for (YourDTO dto : selectedItems) {
        // Process each selected item
    }

    // Clear selection after processing
    selectedItems = null;
}
```

## Common Mistakes to Avoid

### ❌ Wrong: Selection mode on column
```xml
<!-- OLD PATTERN - DON'T USE -->
<p:column selectionMode="multiple" />
```

### ✅ Correct: Selection mode on dataTable, selectionBox on column
```xml
<p:dataTable selectionMode="multiple" ...>
    <p:column selectionBox="true" />
</p:dataTable>
```

### ❌ Wrong: Using List for selection
```java
private List<YourDTO> selectedItems; // WRONG
```

### ✅ Correct: Using Array for selection
```java
private YourDTO[] selectedItems; // CORRECT
```

### ❌ Wrong: Missing rowKey
```xml
<p:dataTable
    selection="#{controller.selectedItems}"
    selectionMode="multiple">
    <!-- Selection won't work without rowKey! -->
```

### ✅ Correct: Always include rowKey
```xml
<p:dataTable
    selection="#{controller.selectedItems}"
    rowKey="#{item.id}"
    selectionMode="multiple">
```

## Selection with Filtering and Sorting

When using `selectAllFilteredOnly="true"`, the header checkbox behavior changes based on active filters:

```xml
<p:dataTable
    selectionMode="multiple"
    selectAllFilteredOnly="true">

    <p:column selectionBox="true" />

    <!-- Filterable column -->
    <p:column headerText="Name"
              filterBy="#{item.name}"
              filterMatchMode="contains">
        <h:outputText value="#{item.name}" />
    </p:column>
</p:dataTable>
```

**Behavior:**
- No filters active: Header checkbox selects all rows
- Filters active: Header checkbox selects only visible (filtered) rows
- User sees visual feedback on which rows are affected

## Example: Bulk Hospital Fee Editing

Complete working example from the HMIS system:

### XHTML View
```xml
<p:dataTable
    id="tblHospitalFees"
    value="#{channelScheduleController.hospitalFeeDTOs}"
    var="fee"
    selection="#{channelScheduleController.selectedHospitalFeeDTOs}"
    rowKey="#{fee.itemFeeId}"
    selectionMode="multiple"
    selectAllFilteredOnly="true"
    selectionPageOnly="false"
    paginator="true"
    rows="20">

    <p:column selectionBox="true" style="width: 60px;" />

    <p:column headerText="Consultant"
              sortBy="#{fee.consultantName}"
              filterBy="#{fee.consultantName}">
        <h:outputText value="#{fee.consultantName}" />
    </p:column>

    <p:column headerText="Hospital Fee"
              sortBy="#{fee.hospitalFee}">
        <h:outputText value="#{fee.hospitalFee}">
            <f:convertNumber pattern="#,##0.00" />
        </h:outputText>
    </p:column>
</p:dataTable>

<p:commandButton
    value="Update Selected"
    action="#{channelScheduleController.updateSelectedHospitalFees()}"
    update="@form" />
```

### Java Controller
```java
@Named
@SessionScoped
public class ChannelScheduleController implements Serializable {

    private List<ChannelHospitalFeeDTO> hospitalFeeDTOs;
    private ChannelHospitalFeeDTO[] selectedHospitalFeeDTOs;

    public void updateSelectedHospitalFees() {
        if (selectedHospitalFeeDTOs == null || selectedHospitalFeeDTOs.length == 0) {
            JsfUtil.addErrorMessage("Please select at least one hospital fee");
            return;
        }

        for (ChannelHospitalFeeDTO dto : selectedHospitalFeeDTOs) {
            // Update each selected fee
            ItemFee fee = itemFeeFacade.find(dto.getItemFeeId());
            fee.setFee(newFeeValue);
            itemFeeFacade.edit(fee);
        }

        JsfUtil.addSuccessMessage("Updated " + selectedHospitalFeeDTOs.length + " fees");

        // Clear selection
        selectedHospitalFeeDTOs = null;
    }

    // Getters and setters
    public ChannelHospitalFeeDTO[] getSelectedHospitalFeeDTOs() {
        return selectedHospitalFeeDTOs;
    }

    public void setSelectedHospitalFeeDTOs(ChannelHospitalFeeDTO[] selected) {
        this.selectedHospitalFeeDTOs = selected;
    }
}
```

## Best Practices

1. **Always use unique rowKey**
   - Use database ID or another unique identifier
   - Don't use index or computed values that might change

2. **Use arrays for selection binding**
   - `YourDTO[]` not `List<YourDTO>`
   - PrimeFaces requires array type for selection

3. **Clear selection after processing**
   - Set selection array to `null` after bulk operations
   - Prevents confusion about what's selected

4. **Enable selectAllFilteredOnly for better UX**
   - Users expect "select all" to select visible rows only
   - Especially important with filters applied

5. **Consider selectionPageOnly carefully**
   - `false`: Better for bulk operations across entire dataset
   - `true`: Better for page-by-page processing
   - Choose based on your use case

6. **Provide visual feedback**
   - Show count of selected items
   - Confirm before destructive operations
   - Display success message with count

## Troubleshooting

### Selection not working
- ✓ Check `rowKey` is present and unique
- ✓ Verify selection property is an array, not List
- ✓ Ensure `selectionMode="multiple"` is on dataTable
- ✓ Confirm `selectionBox="true"` is on column

### Header checkbox not appearing
- ✓ Use `selectionBox="true"` on column (not `selectionMode`)
- ✓ Ensure dataTable has `selectionMode="multiple"`

### Selection lost after update
- ✓ Clear selection explicitly: `selectedItems = null;`
- ✓ Update the dataTable in AJAX response: `update="tblMyTable"`

### Can't select across pages
- ✓ Set `selectionPageOnly="false"` on dataTable
- ✓ Verify `rowKey` is stable across page changes

## Version Notes

- **PrimeFaces 8.0+**: Use `selectionBox="true"` on column
- **PrimeFaces 7.0 and earlier**: Use `selectionMode="multiple"` on column
- **Migration**: If upgrading, change column attribute from `selectionMode` to `selectionBox`

## Related Documentation

- [JSF Ajax Update Guidelines](ajax-update-guidelines.md)
- [PrimeFaces Official Documentation](https://primefaces.github.io/primefaces/)
- [Comprehensive UI Guidelines](../ui/comprehensive-ui-guidelines.md)

---

**Last Updated**: 2024-01-23
**Tested with**: PrimeFaces 12.0+
