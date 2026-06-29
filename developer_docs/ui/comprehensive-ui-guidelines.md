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
**ERP UI RULE**: Use `h:outputText` for ALL text content in JSF pages — headings, labels, descriptions, static strings, and link labels. Do NOT write bare text nodes directly inside `<td>`, `<p:panel>`, or any JSF composite component. This is JSF best practice.
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
- Use `h:outputText` and `p:outputLabel` for **all text content** — headings, labels, messages, descriptions, static strings, and link labels. Never write bare text nodes directly inside JSF/PrimeFaces components. Attach Bootstrap utility classes for emphasis when needed.
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

### Panel Header — Three-Zone Layout (required for action-rich panels)

When a panel header contains both navigation and actions, divide it into three zones using a full-width flex row. This layout is aligned with enterprise UI standards (SAP Fiori shell bar, IBM Carbon top navigation).

```text
[title + config]          [navigation buttons]          [secondary actions → primary]
LEFT                       CENTER                         RIGHT
```

**Zone rules:**

| Zone | Contents | Button style |
|------|----------|--------------|
| **Left** | Panel title (icon + label) + contextual config button (admin-only) | Config: `ui-button-secondary ui-button-outlined` |
| **Center** | Entity navigation buttons (e.g. Patient Lookup, Patient Profile) | `ui-button-info ui-button-outlined` |
| **Right** | Page actions, **ordered left → right by ascending importance** | See action styles below |

**Right-zone action ordering (left = least prominent → right = most prominent):**
1. Rarely-used / dangerous / irreversible actions → `ui-button-secondary` (leftmost)
2. Fast-path or warning actions → `ui-button-warning`
3. **Primary action → `ui-button-success`, rightmost, with an icon and `style="min-width:120px"`**

**Rules:**
- The primary action MUST be the rightmost button. Users scan left-to-right; the last element is the natural "confirm" position.
- Short-label primary buttons (e.g. "Admit", "Save", "Submit") look weak without an icon and minimum width. Always add `icon="pi pi-check-circle"` (or equivalent) and `style="min-width:120px"`.
- Config belongs in the left zone, next to the title it configures — not on the far right. Wrap it in `rendered="#{webUserController.hasPrivilege('Admin')}"`.
- Navigation buttons go in the center zone, never mixed into the action zone. Navigation style is `ui-button-info ui-button-outlined`.
- Remove explicit `ms-*` / `mx-*` spacing classes from buttons inside a flex container; use `gap-2` on the parent instead.

**Skeleton:**
```xhtml
<f:facet name="header">
    <div class="d-flex justify-content-between align-items-center w-100">

        <!-- LEFT: title + contextual config -->
        <div class="d-flex align-items-center gap-2">
            <h:outputText styleClass="fa fa-some-icon" />
            <h:outputText value="Panel Title"/>
            <h:panelGroup rendered="#{webUserController.hasPrivilege('Admin')}">
                <p:commandButton value="Config" icon="fa fa-cog"
                                 immediate="true" ajax="false"
                                 title="Configure settings"
                                 action="#{controller.navigateToConfig()}"
                                 styleClass="ui-button-secondary ui-button-outlined"/>
            </h:panelGroup>
        </div>

        <!-- CENTER: entity-level navigation -->
        <div class="d-flex gap-2">
            <p:commandButton value="Patient Lookup" icon="fa fa-search"
                             ajax="false" immediate="true"
                             action="#{patientController.navigateToSearchPatients()}"
                             styleClass="ui-button-info ui-button-outlined"/>
            <p:commandButton value="Patient Profile" icon="fa fa-user"
                             ajax="false" immediate="true"
                             action="#{patientController.navigateToOpdPatientProfile()}"
                             styleClass="ui-button-info ui-button-outlined"/>
        </div>

        <!-- RIGHT: actions, left=least important → right=primary -->
        <div class="d-flex gap-2 align-items-center">
            <!-- Rare / dangerous — leftmost -->
            <p:commandButton value="Dangerous Action" icon="fas fa-exclamation"
                             styleClass="ui-button-secondary"
                             onclick="PF('dlgConfirm').show();"/>
            <!-- Warning fast-path — middle -->
            <p:commandButton value="Emergency Action" icon="fas fa-bolt"
                             styleClass="ui-button-warning"
                             onclick="PF('dlgEmergency').show();"/>
            <!-- Primary action — rightmost, widened -->
            <p:commandButton id="btnPrimary"
                             value="Save" icon="pi pi-check-circle"
                             action="#{controller.save}"
                             update="@form"
                             style="min-width:120px"
                             styleClass="ui-button-success"/>
        </div>

    </div>
</f:facet>
```

Reference implementation: `src/main/webapp/inward/inward_admission.xhtml`

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

6. **🚨 Institution / Site / Department MUST share one row and align on the same level.** When a report filters by location, the three location selectors always go together on their own row in this exact order and column position: `Institution(label+input) | spacer | Site(label+input) | spacer | Department(label+input)` = 8 cells. Never stagger them across multiple rows (the classic symptom of a `columns="2"` or `columns="4"` grid) or put another filter between them. They are a single visual unit and end users expect to scan them left-to-right on one line. This is the single most common filter-layout defect in the report pages.

7. **Department is always four rendered `p:selectOneMenu` variants** keyed on the institution/site selection, wrapped in a single `<h:panelGroup id="...">` that the institution and site `p:ajax` re-render via `update`. Use the correct `DepartmentController` method per variant — they are NOT interchangeable:
   - both null → `getDepartmentsOfInstitutionAndSite()`
   - site only → `getDepartmentsOfInstitutionAndSite(site)`
   - **institution only → `getDepartmentsOfInstitutionAndSiteForInstitution(institution)`** (NOT `getDepartmentsOfInstitutionAndSite(institution)` — that overload takes a *site* and silently filters by `d.site`, returning the wrong departments)
   - both → `getDepartmentsOfInstitutionAndSite(institution, site)`

Reference implementations:
- `src/main/webapp/inward/report_admission_by_consultant.xhtml` (location filters as the last row)
- `src/main/webapp/reports/inventoryReports/cost_of_goods_sold.xhtml` (Institution/Site/Department aligned on one row in an inventory report)

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

### Index / landing page menus (accordion-sidebar button lists)

Module landing pages (e.g. `pharmacy/disbursement_index.xhtml`, `opd/analytics/index.xhtml`) place a vertical list of navigation buttons inside `p:accordionPanel` tabs in a narrow left sidebar, with the working area (`subcontent`) on the right. This is an ERP, not a marketing site — **maximise the data area and eliminate wasted whitespace in the menu.**

**🚨 Use `<div class="d-grid gap-2">` to stack the buttons — never `<p:panelGrid columns="1">`.** `p:panelGrid` renders an HTML `<table>`: each button lands in a padded `<td>` that does not fill the tab width even with `w-100`, producing ragged button widths and wasted horizontal/vertical space. The Bootstrap `d-grid` makes every button truly full-width with a tight, uniform `gap-2`.

```xhtml
<p:tab title="Transfer Requests">
    <div class="d-grid gap-2">
        <p:commandButton value="Create Request"
                         ajax="false"
                         style="text-align: left"
                         icon="fas fa-plus-circle"
                         styleClass="ui-button-stage-save w-100"
                         action="#{...}"
                         rendered="#{webUserController.hasPrivilege('...')}"/>
        <p:commandButton value="Finalize Requests"
                         ajax="false"
                         style="text-align: left"
                         icon="fas fa-lock"
                         styleClass="ui-button-stage-finalize w-100"
                         action="#{...}"
                         rendered="#{webUserController.hasPrivilege('...')}"/>
        <p:commandButton value="Approve Requests"
                         ajax="false"
                         style="text-align: left"
                         icon="fas fa-check-double"
                         styleClass="ui-button-stage-approve w-100"
                         action="#{...}"
                         rendered="#{webUserController.hasPrivilege('...')}"/>
        <p:commandButton value="Completed Requests"
                         ajax="false"
                         style="text-align: left"
                         icon="fas fa-check-double"
                         styleClass="ui-button-secondary w-100"
                         action="#{...}"
                         rendered="#{webUserController.hasPrivilege('...')}"/>
    </div>
</p:tab>
```

Rules:
- Each button: `styleClass="... w-100"`, `style="text-align: left"` (vertical menus read better left-aligned), and a leading `icon` reflecting the workflow stage.
- **Always use `styleClass` (not `class`) for stage-class buttons** so PrimeFaces applies them correctly.
- **Workflow stage colour convention for all sidebar menus:**
  - Create / New / Save → `styleClass="ui-button-stage-save w-100"` (blue/primary) + action-appropriate icon (e.g. `fas fa-plus-circle`, `fas fa-trash`, `fas fa-recycle`)
  - Finalize → `styleClass="ui-button-stage-finalize w-100"` (amber) + `icon="fas fa-lock"`
  - Approve → `styleClass="ui-button-stage-approve w-100"` (green) + `icon="fas fa-check-double"`
  - View completed / search / informational → `styleClass="ui-button-secondary w-100"` or `ui-button-info w-100`
- Wrap the whole accordion in a single `<h:form>` and use `activeIndex` for tab persistence (see § Forms & State).
- Favour a narrow menu column (`col-2`) over a wide one so the data area gets the space; only widen to `col-md-3` when button labels genuinely need it.

Reference implementation: `src/main/webapp/pharmacy/disposal_index.xhtml` — canonical example of the correct `d-grid` layout and stage-class buttons (Create = `ui-button-stage-save`, Finalize = `ui-button-stage-finalize`, Approve = `ui-button-stage-approve`).

### Primary data actions (row-level)
| Action | Style class | Icon suggestion | Notes |
|--------|-------------|-----------------|-------|
| Edit / Continue | `ui-button-warning` | `fas fa-edit` | Switch to view mode when finalized. |
| Finalize | `ui-button-stage-finalize` | `fas fa-lock` | Use `styleClass`. Disable or swap label when already finalized. |
| Approve / Complete | `ui-button-stage-approve` | `fas fa-check-double` | Use `styleClass`. Reuse icon across the module. |
| Print / View | `ui-button-info` | `fas fa-print` / `fas fa-eye` | Keep `min-width: 90px` for consistency. |
| Cancel / Close | `ui-button-danger ui-button-outlined` | `fas fa-times` | Always add confirmation. |

Supporting rules:
- Use `p:growl` for feedback after actions.
- For any Save / Finalize / Approve button, always use the `ui-button-stage-*` classes — see § Three-Stage Transaction Workflow below.

### 🚨 Three-Stage Transaction Workflow (Save → Finalize → Approve) — MANDATORY STANDARD

Many transaction workflows in the system (pharmacy transfer request/issue/receive, disposals, GRN returns, purchase orders, etc.) progress through three stages: **Save (Draft) → Finalize → Approve.** Historically each page chose its own colour and icon — most stages were all green (`ui-button-success`) with random icons (`pi pi-save`, `pi pi-check`, `fas fa-check`, `fas fa-check-circle`), so a user could not tell the stages apart. **This is now standardised through dedicated CSS classes** so the palette is defined once and every workflow button follows. The colour temperature progresses with the workflow (blue → amber → green) and each stage has one fixed icon:

| Stage | Meaning | Style class (colour) | Icon | Confirm? |
|-------|---------|----------------------|------|----------|
| **Create / New** | Navigate to a new transaction form | `ui-button-stage-save` (blue) | Action-specific (e.g. `fas fa-plus-circle`, `fas fa-trash`) | No |
| **Save / Save Draft** | Save an editable draft in-form, nothing committed yet | `ui-button-stage-save` (blue) | `fas fa-floppy-disk` | No |
| **Finalize** | Locks the draft; sends it forward for approval | `ui-button-stage-finalize` (amber) | `fas fa-lock` | Yes — `onclick="return confirm('…?')"` |
| **Approve** | Final sign-off, completes the transaction | `ui-button-stage-approve` (green) | `fas fa-check-double` | Yes — `onclick="return confirm('…?')"` |

The three `ui-button-stage-*` classes are defined **once** in `src/main/webapp/resources/css/ohmis.css` (loaded globally by the template) and are the single source of truth for the workflow palette. Use them via `styleClass` instead of `ui-button-info/warning/success` — to re-theme every workflow button across the system, edit those three classes, not the pages. The matching **icon stays on the markup** (PrimeFaces renders button icons natively), so each button carries `styleClass="ui-button-stage-…"` **and** the `icon` listed above.

```xhtml
<p:commandButton id="btnSave"     value="Save"     icon="fas fa-floppy-disk"  styleClass="mx-1 ui-button-stage-save"     ajax="false" action="#{ctrl.save()}" />
<p:commandButton id="btnFinalize" value="Finalize" icon="fas fa-lock"         styleClass="mx-1 ui-button-stage-finalize" ajax="false" action="#{ctrl.finalize()}" onclick="return confirm('Are you sure you want to finalize? This cannot be undone.');" />
<p:commandButton id="btnApprove"  value="Approve"  icon="fas fa-check-double" styleClass="mx-1 ui-button-stage-approve"  ajax="false" action="#{ctrl.approve()}" onclick="return confirm('Are you sure you want to approve?');" />
```

Rules:
- Always use the `ui-button-stage-*` class (not the raw `ui-button-info/warning/success`) for any Save/Finalize/Approve button so the palette stays centralised. Use `styleClass`, not `class` (canonical PrimeFaces attribute).
- Apply the **same** stage class + icon everywhere it appears — on the action page header, in list-page row actions ("Edit and Finalize", "Edit and Approve"), and in landing-page menus. The "Edit and …" list variants keep the destination stage's class/icon (e.g. "Edit and Finalize" = `ui-button-stage-finalize` + `fas fa-lock`).
- Finalize and Approve are irreversible transitions → always guard with native `confirm()` (see Confirmation Dialogs below).
- Never reuse the green Approve class for Save or Finalize, and never reuse `pi pi-check` / `fas fa-check-circle` for these three stages — those collisions are exactly what this standard removes.
- This overrides the generic row-level "Finalize / Approve" rows in the table above for any button that is part of a three-stage transaction workflow.

CSS source: `src/main/webapp/resources/css/ohmis.css` (search "Three-Stage Transaction Workflow buttons").

**Reference implementations:**
- **Action page** (the transaction form itself): `src/main/webapp/pharmacy/pharmacy_issue.xhtml` — canonical example of a three-zone panel header containing Save (`ui-button-stage-save`), Finalize (`ui-button-stage-finalize`), and Approve (`ui-button-stage-approve`) in the right zone, with outlined navigation links in the center zone (`ui-button-info ui-button-outlined` — **always use `ui-button-info ui-button-outlined` for center nav, never stage classes there**, because the `!important` background in stage classes prevents the outlined modifier from rendering correctly).
- **Index / landing page** (sidebar accordion menu): `src/main/webapp/pharmacy/disposal_index.xhtml` — canonical example of `d-grid` layout with stage-class buttons (Create = `ui-button-stage-save`, Finalize = `ui-button-stage-finalize`, Approve = `ui-button-stage-approve`).

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

### DataTable Compact Size (required for data-entry and multi-column tables)

Always add `styleClass="p-datatable-sm"` to `p:dataTable` components on data-entry and transaction pages. This is the PrimeFaces 14 standard for compact row padding and keeps more rows visible without scrolling.

```xhtml
<p:dataTable id="tbl"
             value="#{bean.items}"
             var="item"
             styleClass="p-datatable-sm">
```

**Column width rules** — always use the `width` attribute on `p:column`, never `style="min-width:…"` (PrimeFaces ignores CSS min-width in its column width allocation algorithm):

| Column type | Recommended width |
|---|---|
| Row index (`#`) | `2em` |
| Item / description name | `15em` |
| Quantity / short number | `6em` |
| Currency value | `6em` |
| Date | `6em` |
| Actions | `8em`–`10em` |

If the sum of all column widths exceeds the table container, add `scrollable="true"` to enable horizontal scroll rather than letting columns collapse to zero.

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

### Accessibility-first development (required)

We drive Chrome via the Playwright MCP server for end-to-end verification. Playwright's accessibility snapshot is the primary way Claude and tooling identify elements, so every interactive component must carry an accessible name. **Do this while writing the page, not after.**

**On every new or modified page:**

- Give the `p:dataTable` an `id`, `widgetVar`, `summary`, `rowKey="#{row.id}"`, and `rowIndexVar="rowIndex"`. The `summary` becomes the table's accessible description; `rowKey` makes specific rows targetable.
- Every `p:commandButton`, `p:commandLink`, and `p:button` must have an interpolated `title` that includes the row's identifier — e.g. `title="Fast receive items from #{p.deptId}"`, `title="View bill #{b.deptId}"`. The button's visible label alone (`"Fast Receive"`, `"View"`) is identical across rows and useless to Playwright.
- For buttons that render only an icon (no `value`), add `title="…"` with the action AND the row identifier. Without it the accessibility tree falls back to the base CSS class (`"ui-button"`) and the row becomes anonymous.
- Wrap row-level buttons that depend on a value in `<h:panelGroup rendered="#{not empty value}">` so an empty value doesn't render a button with no accessible name.
- Add `id` to every form input, calendar, and dropdown — Playwright `browser_fill_form` needs stable ids. Inside iterating components (column inside dataTable, `ui:repeat`) JSF auto-prefixes ids with the iteration index, which is fine — just give them a stable suffix.
- For status badges and other read-only indicators, prefer text + colour over colour alone, and surface the status in a `title` if the badge is icon-only.

When you finish a UI change, mentally check: "If I asked Playwright to click the Fast Receive button on row PHPHTI/2878, can it identify that row uniquely from the accessibility snapshot?" If not, add titles until it can.

### Data-entry components — make them automatable and robust (required)

These patterns came out of end-to-end transfer testing. Apply them **while
writing the page** so both real users and the Playwright MCP server can drive
the form reliably. The runtime counterpart is
[Playwright E2E Testing Workflow](../testing/playwright-e2e-workflow.md).

- **Give every actionable button a stable `id`.** Add/Save/Settle/Issue/Receive
  buttons must have an explicit `id` (e.g. `id="btnAddItem"`,
  `id="btnSettleReceive"`). Reference one button from another component's
  JavaScript via `#{p:resolveFirstComponentWithId('btnAddItem',view).clientId}`.
  Do **not** use the `p:component(...)` EL function — it is not registered in
  this project and throws a 500 (`Function 'p:component' not found`).

- **Limit autocomplete result counts.** Add `maxResults="10"` (or a config-driven
  cap) to every `p:autoComplete`. Unbounded result lists are slow and unusable.
  For large master lists (staff, items), also set `minQueryLength="3"` and
  `queryDelay="600"` so the server query fires once the user pauses, not on every
  keystroke.

- **Never let Enter clear or wrongly submit the form.** A JSF form with no
  default command submits on Enter via the first command button, which on an
  item-entry page silently wipes the in-progress bill. Guard it:
  - On the item autocomplete, `onkeydown`: when the suggestion panel is open, let
    Enter select the highlighted item; otherwise `event.preventDefault()` so
    Enter does not submit.
  - On the quantity field, `onkeydown`: `preventDefault()` on Enter to stop the
    submit; `onkeyup`: on Enter, after a short `setTimeout` (≈350 ms, to let the
    keyup-AJAX commit the bound quantity first) click the Add button by its
    resolved client id. The delay matters — without it the quantity arrives empty.

- **Multi-word search must match in any order.** When a `completeMethod` backs a
  full-name search (staff, patients), split the query on whitespace and AND each
  token across the relevant fields (name/code) server-side. A naïve single-`LIKE`
  query fails the moment the user types `First Last`. (See
  `StaffController.completeStaffWithoutDoctors`.)

- **Protect settle/issue/receive against double submission.** Use a JS
  `confirm()` guard in `onclick`
  (`onclick="if (!confirm('Are you sure …?')) return false;"`) **and** a
  server-side re-entrancy guard (a `synchronized` settle method and/or a boolean
  `settling` flag) so a rapid double-click cannot create duplicate bills/items.
  Do **not** rely on `this.disabled=true` in `onclick` — disabled fields are
  excluded from the POST, so the values they hold never reach the server.

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
