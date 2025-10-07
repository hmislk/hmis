# UI Best Practices and Troubleshooting

## Accordion Panel Tab Persistence Issue Resolution

### Problem
Lab summaries index page (`reportLab/lab_summeries_index.xhtml`) was not persisting active tab state and preventing navigation to other tabs.

### Root Cause
**Multiple forms inside accordion tabs** were causing JSF form processing conflicts.

### Simple Solution
1. **Remove all individual `<h:form>` tags** inside each accordion tab
2. **Wrap the entire accordion** in a single `<h:form>` tag
3. **Remove complex event handling logic** - just use `activeIndex` binding

### Key Lesson
**Always explore simple measures first before adding complex solutions.**

Common UI issues can often be resolved by:
- Proper form structure (one form per logical group)
- Understanding JSF component hierarchy
- Using built-in binding mechanisms

### Code Changes Made
- `lab_summeries_index.xhtml`: Consolidated 8+ individual forms into one wrapping form
- `LaboratoryReportController.java`: Removed unnecessary `onTabChange` and `updateActiveIndex` methods

### Result
- All tabs accessible ✓
- Tab state persists across session ✓  
- Cleaner, maintainable code ✓
- No PrimeFaces version compatibility issues ✓

---

## Command Button Patterns

For canonical icon, label, and color pairings see the [Icon Management Guidelines](icon-management.md).

Quick reminders when applying those standards:
- Keep heavy operations non-AJAX (`ajax="false"`) to avoid download issues.
- Add descriptive `title` attributes so screen readers announce the action.
- Group related buttons with flex utilities such as `d-flex gap-2` and use PrimeFaces button classes for color consistency.

---

## Workflow Action Button Patterns

When implementing multi-stage workflows (create → finalize → approve), use these standardized button patterns for consistency across the application.

### Navigation Buttons (Header)

Place workflow navigation buttons in the page header using outlined info style:

```xhtml
<div class="d-flex gap-2">
    <p:commandButton
        value="List Items To Create Return"
        action="/path/to/create?faces-redirect=true"
        ajax="false"
        icon="fas fa-plus-circle"
        styleClass="ui-button-info ui-button-outlined"
        rendered="#{webUserController.hasPrivilege('CreatePrivilege')}"
        title="Create a new request">
    </p:commandButton>

    <p:commandButton
        value="List Items to Finalize"
        action="/path/to/finalize?faces-redirect=true"
        ajax="false"
        icon="fas fa-tasks"
        styleClass="ui-button-info ui-button-outlined"
        rendered="#{webUserController.hasPrivilege('FinalizePrivilege')}"
        title="Finalize pending requests">
    </p:commandButton>

    <p:commandButton
        value="List Items To Approve"
        action="/path/to/approve?faces-redirect=true"
        ajax="false"
        icon="fas fa-check-circle"
        styleClass="ui-button-info ui-button-outlined"
        rendered="#{webUserController.hasPrivilege('ApprovePrivilege')}"
        title="Approve finalized requests">
    </p:commandButton>

    <p:commandButton
        value="Completed Items"
        action="/path/to/completed?faces-redirect=true"
        ajax="false"
        icon="fas fa-check-double"
        styleClass="ui-button-info ui-button-outlined"
        rendered="#{webUserController.hasPrivilege('ViewPrivilege')}"
        title="View completed items">
    </p:commandButton>
</div>
```

**Pattern Rules**:
- All navigation buttons use `ui-button-info ui-button-outlined` style
- Icons follow workflow progression: `plus-circle` → `tasks` → `check-circle` → `check-double`
- Always include privilege checks with `rendered` attribute
- Use descriptive `title` attributes for tooltips
- Non-AJAX navigation (`ajax="false"`) for page changes

### Action Buttons (Data Table)

#### Primary Workflow Actions

**Create/Edit Action** (Warning Style):
```xhtml
<p:commandButton
    value="#{item.finalized ? 'View' : 'Edit'}"
    icon="fas #{item.finalized ? 'fa-eye' : 'fa-edit'}"
    styleClass="#{item.finalized ? 'ui-button-info' : 'ui-button-warning'}"
    action="#{controller.processItem(item)}"
    disabled="#{item.cancelled eq true}"
    style="min-width: 90px;">
</p:commandButton>
```

**Finalize Action** (Warning Style):
```xhtml
<p:commandButton
    value="Finalize"
    icon="fas fa-check-circle"
    styleClass="ui-button-success"
    action="#{controller.finalizeItem(item)}"
    disabled="#{item.finalized or item.cancelled}"
    style="min-width: 90px;">
</p:commandButton>
```

**Approve Action** (Success Style):
```xhtml
<p:commandButton
    value="#{item.completed ? 'Approved' : 'Approve'}"
    icon="fas #{item.completed ? 'fa-check-double' : 'fa-check-circle'}"
    styleClass="#{item.completed ? 'ui-button-success ui-button-outlined' : 'ui-button-success'}"
    action="#{controller.approveItem(item)}"
    disabled="#{item.completed or item.cancelled}"
    style="min-width: 90px;">
</p:commandButton>
```

**Cancel/Close Action** (Danger Outlined):
```xhtml
<p:commandButton
    value="Close"
    icon="fas fa-times"
    styleClass="ui-button-danger ui-button-outlined"
    action="#{controller.cancelItem(item)}"
    ajax="true"
    update="tbl :messages"
    disabled="#{item.cancelled or item.completed}"
    rendered="#{item.cancelled ne true and item.completed ne true}"
    style="min-width: 70px;">
    <p:confirm header="Confirm Cancel"
               message="Are you sure you want to cancel this item?"
               icon="pi pi-exclamation-triangle"/>
</p:commandButton>
```

**Print Action** (Info Style):
```xhtml
<p:commandButton
    value="Print"
    icon="fas fa-print"
    styleClass="ui-button-info"
    action="#{controller.printItem(item)}"
    style="min-width: 90px;">
</p:commandButton>
```

#### Button Width Guidelines

Use `min-width` to maintain consistent button sizes:
- **Primary actions**: `min-width: 90px` (Finalize, Approve, Print)
- **Secondary actions**: `min-width: 70px` (Close, Delete)
- **Icon-only buttons**: No min-width needed

### Status Indicators

Show item status below action buttons:

```xhtml
<!-- Cancelled Status -->
<h:panelGroup rendered="#{item.cancelled}" styleClass="d-block mt-1">
    <small class="text-muted fst-italic">
        <i class="fas fa-ban me-1"></i>Cancelled
    </small>
</h:panelGroup>

<!-- Finalized Status -->
<h:panelGroup rendered="#{item.finalized and not item.cancelled}" styleClass="d-block mt-1">
    <small class="text-success fst-italic">
        <i class="fas fa-user-check me-1"></i>Checked
    </small>
</h:panelGroup>

<!-- Approved Status -->
<h:panelGroup rendered="#{item.completed and not item.cancelled}" styleClass="d-block mt-1">
    <small class="text-success fst-italic">
        <i class="fas fa-check-double me-1"></i>Approved
    </small>
</h:panelGroup>
```

### Confirmation Dialogs

Always add confirmation for destructive actions:

```xhtml
<!-- Global Confirmation Dialog (add once per page) -->
<p:confirmDialog global="true" showEffect="fade" hideEffect="fade" responsive="true" width="350">
    <p:commandButton value="No" type="button" styleClass="ui-confirmdialog-no ui-button-flat"/>
    <p:commandButton value="Yes" type="button" styleClass="ui-confirmdialog-yes" />
</p:confirmDialog>
```

Use with `<p:confirm>` inside action buttons:
```xhtml
<p:confirm header="Confirm Delete"
           message="Are you sure you want to delete this item?"
           icon="pi pi-exclamation-triangle"/>
```

### Color Scheme Reference

| Action Type | Style Class | Icon Color | When to Use |
|-------------|-------------|------------|-------------|
| **Navigation** | `ui-button-info ui-button-outlined` | Info Blue | All workflow navigation buttons |
| **Create/Start** | `ui-button-warning` | Warning Yellow | Initial actions, edits |
| **Finalize** | `ui-button-success` | Success Green | Completing/finalizing items |
| **Approve** | `ui-button-success` | Success Green | Final approval actions |
| **Cancel/Close** | `ui-button-danger ui-button-outlined` | Danger Red | Cancellation, destructive actions |
| **Print/View** | `ui-button-info` | Info Blue | Read-only actions |
| **Save** | `ui-button-secondary` | Secondary Gray | Draft/temporary saves |
| **Disabled** | `ui-button-*` + `disabled` | Grayed out | Already completed/unavailable |

### Icon Reference for Workflows

| Workflow Stage | Icon | Usage |
|----------------|------|-------|
| **Create New** | `fas fa-plus-circle` | Navigation to create page |
| **Edit Draft** | `fas fa-edit` | Editing unsaved items |
| **View** | `fas fa-eye` | Viewing finalized items |
| **Finalize** | `fas fa-check-circle` | Marking as complete/ready |
| **Approve** | `fas fa-check-circle` | Approving finalized items |
| **Completed** | `fas fa-check-double` | Navigation to completed items |
| **Tasks/Pending** | `fas fa-tasks` | Navigation to pending items |
| **Cancel/Close** | `fas fa-times` | Closing/cancelling items |
| **Print** | `fas fa-print` | Printing documents |
| **Settings** | `fas fa-cog` | Configuration dialogs |
| **Delete** | `fas fa-trash-alt` | Removing items |

### Column Width Guidelines

Set appropriate widths for action columns:

```xhtml
<!-- Single action button -->
<p:column headerText="Actions" styleClass="text-center" exportable="false" width="120">

<!-- Two action buttons -->
<p:column headerText="Actions" styleClass="text-center" exportable="false" width="180">

<!-- Three action buttons -->
<p:column headerText="Actions" styleClass="text-center" exportable="false" width="240">
```

### Complete Example: Disposal Return Finalize Page

Reference implementation showing all patterns:

**File**: `pharmacy_disposal_return_finalize.xhtml`

```xhtml
<!-- Header Navigation Buttons -->
<div class="d-flex gap-2">
    <p:commandButton value="List Issues To Create Return" ... />
    <p:commandButton value="List Returns to Finalize" ... />
    <p:commandButton value="List Returns To Approve" ... />
    <p:commandButton value="Completed Returns" ... />
</div>

<!-- Action Column with Finalize and Close -->
<p:column headerText="Actions" styleClass="text-center" exportable="false" width="180">
    <div class="d-flex justify-content-center align-items-center gap-2">
        <!-- Primary Action: Finalize -->
        <p:commandButton
            value="#{b.checkedBy ne null ? 'Finalized' : 'Finalize'}"
            icon="fas #{b.checkedBy ne null ? 'fa-check-double' : 'fa-edit'}"
            styleClass="#{b.checkedBy ne null ? 'ui-button-success ui-button-outlined' : 'ui-button-warning'}"
            .../>

        <!-- Secondary Action: Close -->
        <p:commandButton
            value="Close"
            icon="fas fa-times"
            styleClass="ui-button-danger ui-button-outlined"
            rendered="#{b.cancelled ne true and b.checkedBy eq null}"
            ...>
            <p:confirm header="Confirm Cancel" message="..."/>
        </p:commandButton>
    </div>

    <!-- Status Indicators -->
    <h:panelGroup rendered="#{b.cancelled}" ...>
        <small class="text-muted fst-italic">
            <i class="fas fa-ban me-1"></i>Cancelled
        </small>
    </h:panelGroup>
</p:column>

<!-- Global Components -->
<p:growl id="messages" showDetail="true" />
<p:confirmDialog global="true" .../>
```

### Key Principles

1. **Consistency**: Use the same colors, icons, and styles for the same actions across all pages
2. **Visual Hierarchy**: Primary actions (solid buttons) stand out more than secondary (outlined)
3. **Progressive Disclosure**: Show only relevant buttons based on item state
4. **Confirmation**: Always confirm destructive actions (delete, cancel)
5. **Feedback**: Use messages (`p:growl`) to inform users of action results
6. **Accessibility**: Include `title` attributes for tooltips and screen readers
7. **Privilege Checks**: Always gate actions with appropriate privilege checks

### Common Mistakes to Avoid

❌ **Don't**: Mix solid and outlined styles randomly
```xhtml
<!-- WRONG: Inconsistent styling -->
<p:commandButton styleClass="ui-button-info" .../>
<p:commandButton styleClass="ui-button-info ui-button-outlined" .../>
```

✅ **Do**: Use outlined for navigation, solid for primary actions
```xhtml
<!-- CORRECT: Navigation uses outlined -->
<p:commandButton styleClass="ui-button-info ui-button-outlined" .../>
<!-- CORRECT: Action uses solid -->
<p:commandButton styleClass="ui-button-warning" .../>
```

❌ **Don't**: Switch icon libraries within the same view without a reason
```xhtml
<!-- WRONG: In-view inconsistency -->
<p:commandButton icon="pi pi-check" .../>
<p:commandButton icon="fas fa-times" .../>
```

✅ **Do**: Keep a single library per page and reuse icons across the workflow
```xhtml
<!-- CORRECT: One view using PrimeFaces icons throughout -->
<p:commandButton icon="pi pi-play" .../>
<p:commandButton icon="pi pi-print" .../>
<p:commandButton icon="pi pi-file-excel" .../>
```

Additional guidance:
- Prefer PrimeFaces `pi` icons when a suitable option exists; fall back to Font Awesome `fas` only when needed.
- Reuse the same icon for the same action in every step of a workflow so users build recognition.
- It is acceptable to use different libraries on different pages if that keeps each page internally consistent.

❌ **Don't**: Skip confirmation for destructive actions
```xhtml
<!-- WRONG: No confirmation for delete -->
<p:commandButton value="Delete" action="#{bean.delete}" .../>
```

✅ **Do**: Always confirm destructive actions
```xhtml
<!-- CORRECT: Confirmation dialog -->
<p:commandButton value="Delete" action="#{bean.delete}" ...>
    <p:confirm header="Confirm Delete" message="Are you sure?" .../>
</p:commandButton>
```

---
**Remember: Simple solutions first. Complex solutions should be a last resort.**
