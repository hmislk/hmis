# HMIS UI Development Handbook

## Scope and Principles
- Applies to all JSF pages
- Build with consistency, accessibility and privilege validation in mind.
- Prefer simple, template-aligned solutions before adding custom code or CSS.
- Keep behaviour aligned with centralized configuration (`configOptionApplicationController`) and feature toggles.

## Critical Rules for Claude Code

**🚨 These rules MUST be followed when working on UI tasks:**

**UI-ONLY CHANGES**: When UI improvements are requested, make ONLY frontend/XHTML changes
**KEEP IT SIMPLE**: Use existing controller properties and methods - avoid introducing filteredValues, globalFilter, or new backend logic
**FRONTEND FOCUS**: Stick to HTML/CSS styling, PrimeFaces component attributes, and layout improvements
**ERP UI RULE**: Use `h:outputText` instead of HTML headings (h1-h6)
**PRIMEFACES CSS**: Use PrimeFaces button classes, not Bootstrap button classes
**XHTML STRUCTURE**: HTML DOCTYPE with `ui:composition` and template inside `h:body`
**XML ENTITIES**: Always escape ampersands as `&amp;` in XHTML attributes

---

## Page Structure
- **Use full HTML documents**: `<!DOCTYPE html>`, `<html>` with namespaces, `<h:head>`, and `<h:body>`.
- **Embed the template inside the body** using `<ui:composition template="/resources/template/template.xhtml">`.
- **Escape XML entities** (`&amp;`, `&lt;`, etc.) to keep XHTML valid.
- **Case study – Accordion state loss**: Multiple forms inside accordion tabs broke tab persistence. Wrapping the whole accordion in one `<h:form>` and using the built-in `activeIndex` restored expected behaviour without extra controller logic.

### Recommended skeleton
```xhtml
<?xml version='1.0' encoding='UTF-8' ?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:h="http://xmlns.jcp.org/jsf/html"
      xmlns:p="http://primefaces.org/ui"
      xmlns:f="http://xmlns.jcp.org/jsf/core"
      xmlns:ui="http://xmlns.jcp.org/jsf/facelets">
<h:head>
    <title>Page Title</title>
</h:head>
<h:body>
    <ui:composition template="/resources/template/template.xhtml">
        <ui:define name="content">
            <h:form>
                <!-- Page content -->
            </h:form>
        </ui:define>
    </ui:composition>
</h:body>
</html>
```

---

## Layout, Typography, and Containers
- Use **PrimeFaces components for interaction** (buttons, dialogs, tables) and **Bootstrap utilities for layout** (`row`, `col-*`, `d-flex`, spacing helpers).
- Prefer `p:panelGrid` when you only need a grid with a header; only wrap in `p:panel` when you need facets or panel styling.
- Use `h:outputText` and `p:outputLabel` for headings, labels, and messages instead of HTML heading tags. Attach Bootstrap utility classes for emphasis when needed.
- Keep screens dense and business-focused; avoid marketing-style hero headers.

---

## Panel Structure Best Practices

### Panel Layout Patterns
- **Action placement**: Use `f:facet name="header"` for panel-related actions instead of separate button rows
- **Data display**: Use `p:panelGrid columns="2" layout="tabular"` for label-value pairs instead of Bootstrap grid divs
- **Avoid over-nesting**: Place dataTable directly in panel content when it's the primary element; don't wrap in additional panels

Example:
```xhtml
<p:panel header="Entity Information">
    <f:facet name="header">
        <p:commandButton value="Action" styleClass="ui-button-info"/>
    </f:facet>

    <p:panelGrid columns="2" layout="tabular">
        <p:outputLabel value="Name:"/>
        <h:outputText value="#{bean.name}"/>
    </p:panelGrid>

    <p:dataTable value="#{bean.items}" var="item">
        <!-- Direct table content -->
    </p:dataTable>
</p:panel>
```

---

## Forms and Input Patterns
- Align labels and inputs with `p:outputLabel` + PrimeFaces components; include `for` attributes for accessibility.
- Reuse controller state; avoid duplicating filters or adding new global variables when not required.
- Heavy operations (report generation, exports) should use `ajax="false"` to allow file downloads.
- Include descriptive `title` attributes on interactive elements.

### Report Filter Grid Layout (`h:panelGrid columns="8"`)

Report filter panels use an 8-column `h:panelGrid` with the pattern: `label(1) | input(2) | spacer(3) | label(4) | input(5) | spacer(6) | label(7) | input(8)`.

**Rules:**

1. **Every row must add up to exactly 8 cells.** `h:panelGrid` flows cells left-to-right with no concept of rows — miscounting by even one cell shifts every subsequent row. Count carefully.

2. **Use `p:spacer` for filler cells**, not `h:panelGroup`. Empty `h:panelGroup` elements render as block elements that can affect column widths unpredictably.

3. **When a row has fewer than 3 filter pairs, fill the unused tail with spacers.** For example, a row with only 2 filter pairs (5 cells: label+input+spacer+label+input) needs 3 trailing spacers to complete the row of 8.

4. **Group filters logically by row** — do not try to force unrelated filters into the same row just to fill columns. Natural groupings for admission-type reports:
   - Row 1: date range filters (From / To / Date Basis)
   - Row 2: admission classification filters (Admission Type / Payment Method) + 3 trailing spacers
   - Row 3: clinical filters (Speciality / Consultant) + 3 trailing spacers
   - Row 4: location filters (Institution / Site / Department)

   Logical grouping is more maintainable and readable than trying to pack every row to 8 cells with unrelated fields.

5. **Do not mix `p:spacer` counts to "push" a field rightward** as an alignment trick — this is fragile. If a field should appear in a specific column, count the cells from the start of the row.

Reference implementation: `src/main/webapp/inward/report_admission_by_consultant.xhtml`

---

## Buttons and Workflow Actions
### Navigation (page-to-page)
- Style with `ui-button-info ui-button-outlined`.
- Use consistent icons that reflect workflow stages: `fas fa-plus-circle` (create), `fas fa-tasks` (pending), `fas fa-check-circle` (approve), `fas fa-check-double` (completed).
- Example navigation block:
```xhtml
<div class="d-flex flex-wrap gap-2">
    <p:commandButton value="List Items To Create"
                     action="/workflow/create?faces-redirect=true"
                     ajax="false"
                     icon="fas fa-plus-circle"
                     styleClass="ui-button-info ui-button-outlined"
                     rendered="#{webUserController.hasPrivilege('CreatePrivilege')}"
                     title="Create a new request"/>
    <!-- Additional navigation buttons -->
</div>
```

### Primary data actions (row-level)
| Action | Style class | Icon suggestion | Notes |
|--------|-------------|-----------------|-------|
| Edit / Continue | `ui-button-warning` | `fas fa-edit` | Switch to view mode when finalized. |
| Finalize | `ui-button-success` | `fas fa-check-circle` | Disable or swap label when already finalized. |
| Approve / Complete | `ui-button-success` | `fas fa-check-circle` or `fas fa-check-double` | Reuse icon across the module. |
| Print / View | `ui-button-info` | `fas fa-print` / `fas fa-eye` | Keep `min-width: 90px` for consistency. |
| Cancel / Close | `ui-button-danger ui-button-outlined` | `fas fa-times` | Always add confirmation. |

Supporting rules:
- Use `p:growl` for feedback after actions.

### Confirmation Dialogs

**Prefer native JavaScript `confirm()` over `p:confirmDialog` / `p:confirm`.**

`p:confirmDialog` with global wiring is fragile — it frequently fails silently due to JSF lifecycle and AJAX partial-render ordering issues.

Use `onclick="return confirm('...');"` directly on `p:commandButton`:

```xhtml
<p:commandButton value="Complete"
                 action="#{controller.complete}"
                 ajax="false"
                 onclick="return confirm('Are you sure you want to complete this? This cannot be undone.');"/>
```

Rules:
- Write the confirmation message as a plain question the user can answer Yes/No.
- Do **not** add `p:confirm` child tags or a global `p:confirmDialog` in the same form.
- Only deviate from this pattern (e.g. custom modal) when explicitly required and approved.

---

## Data Presentation
- Align numeric fields with `text-end`, status columns with `text-center`, and specify column widths in `em`.
- Format numbers with `<f:convertNumber pattern="#,##0.00"/>` and dates with application preference patterns (`#{sessionController.applicationPreference.shortDateTimeFormat}` etc.).
- Avoid placing decorative icons in every cell; reserve icons for headers or action columns.
- Use neutral currency labels (e.g., `Requested Value`, `Net Amount`) and neutral icons such as `pi pi-money-bill` (or `fas fa-coins` when no PrimeFaces option exists) so pages stay multi-currency friendly.

### Badge Usage for Status Indicators
- **ALWAYS use PrimeFaces `p:badge`** instead of HTML/Bootstrap badge classes (`badge`, `badge-*`)
- PrimeFaces badges provide better visibility and theming support
- Use semantic severity attributes: `success`, `info`, `warning`, `danger`, `secondary`
- Example implementation:
```xhtml
<!-- ❌ AVOID: HTML badges (may not be visible in all themes) -->
<span class="badge badge-success">Active</span>

<!-- ✅ PREFER: PrimeFaces badges -->
<p:badge value="Active" severity="success"/>

<!-- ✅ Dynamic severity based on conditions -->
<p:badge value="#{item.status}"
         severity="#{item.active ? 'success' : 'danger'}"/>
```
- Center-align badge columns with `styleClass="text-center"` for better presentation
- Common severity mappings:
  - `success`: Active, Completed, Approved
  - `danger`: Retired, Failed, Rejected, Cancelled
  - `warning`: Pending, In Progress, Draft
  - `info`: Information counts, totals
  - `secondary`: Codes, identifiers

---

## Icon Standards
- Primary library: PrimeFaces `pi` icons. Use Font Awesome `fas` only when there is no `pi` equivalent.
- Use SVG assets at 80x80 for reusable art and declare `fill="currentColor"` (or rely on `currentColor`) for dynamic theming.
- Keep icon, label, tooltip, and button style combinations consistent across modules.
- Canonical icon pairings and additional patterns live in `icon-management.md`. Update that file first when introducing or changing icons.

---

## Accessibility, Security, and Behaviour
- Always pair icons with text labels; never rely on colour or icon alone.
- Provide `title` attributes or `aria` labels for buttons and links.
- Honour configuration toggles (feature flags, color schemes) via `configOptionApplicationController`.
- Prefer server-side sanitised data and avoid embedding secrets or hard-coded environment values.

---

## Troubleshooting Checklist
1. Start with the simplest fix (form structure, built-in bindings) before adding custom Java or JavaScript.
2. Verify tab or accordion behaviour with a single form wrapper to avoid JSF lifecycle conflicts.
3. Test heavy operations with non-AJAX submissions to confirm downloads still work.
4. Use browser print preview to validate layout and, for multi-page printouts, follow `page-break-implementation-guide.md`.

---

---

## `p:autoComplete` — Rules for Entity Values

### No `converter` attribute
Do NOT add a `converter` attribute to `p:autoComplete` when the value is a JPA entity. The framework registers `@FacesConverter(forClass = ...)` converters for all entities automatically; adding an explicit converter causes duplicate-conversion errors. Only add a `converter` when the value type has no `forClass` converter (e.g., a raw `Long` ID).

### No `dropdown` attribute
Do NOT use `dropdown="true"` on `p:autoComplete`. It renders a dropdown toggle button next to the input field which is not part of the project's UI style and causes layout issues in the filter grid.

```xml
<!-- CORRECT -->
<p:autoComplete
    id="cmbCc"
    styleClass="w-100"
    inputStyleClass="w-100 form-control"
    value="#{myController.institution}"
    completeMethod="#{institutionController.completeCreditCompany}"
    var="cc"
    itemLabel="#{cc.name}"
    itemValue="#{cc}"
    forceSelection="true" />

<!-- WRONG — both issues shown -->
<p:autoComplete
    converter="deal"
    dropdown="true"
    value="#{myController.institution}"
    ... />
```

This applies to all entity-backed autocompletes: `Institution`, `Department`, `Item`, `Patient`, etc.

---

## Related Guides
- `icon-management.md` – canonical icon library, terminology, and accessibility notes.
- `page-break-implementation-guide.md` – printing guidance for token/bill flows.
- Security and privilege patterns – see `developer_docs/security/privilege-system.md`.
