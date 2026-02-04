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

## Related Guides
- `icon-management.md` – canonical icon library, terminology, and accessibility notes.
- `page-break-implementation-guide.md` – printing guidance for token/bill flows.
- Security and privilege patterns – see `developer_docs/security/privilege-system.md`.
