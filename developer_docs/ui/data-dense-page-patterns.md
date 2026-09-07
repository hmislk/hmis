# Data-Dense Page Patterns

HMIS pages routinely show large datasets, many inputs, and many action buttons on one screen. End users work heads-down: they should be able to see the data, reach the actions, and enter rows **without scrolling back and forth**. This guide defines the standard patterns for achieving that. It complements the [UI Development Handbook](comprehensive-ui-guidelines.md) — read that first for page structure, button styling, and accessibility rules.

Industry references these patterns align with: SAP Fiori (footer toolbar, dense responsive tables, keyboard shortcuts), IBM Carbon (sticky headers, condensed data tables), and PrimeFaces 14 DataTable documentation (sticky header, frozen columns, lazy loading).

---

## 1. Scroll Management for Tables

The user must never lose the column headers or the row-identifying column while scrolling a large table.

### Sticky header — default for every long table

Add `stickyHeader="true"` to any `p:dataTable` that can grow beyond one screen. The header row stays pinned to the viewport top while the user scrolls.

```xhtml
<p:dataTable id="tblBillItem"
             value="#{controller.items}"
             var="item"
             stickyHeader="true"
             styleClass="p-datatable-sm">
```

Rules:
- **Every data-entry and transaction table gets `stickyHeader="true"`.** There is no cost when the table is short.
- Combine with `styleClass="p-datatable-sm"` (compact rows — mandatory per the handbook).
- `stickyHeader` works on non-scrollable tables; do not add `scrollable="true"` just for this.

### Fixed-height scroll region — when the page has content below the table

When a table sits above other panels (e.g. bill items above payment details), give it its own scroll region so the content below stays reachable:

```xhtml
<p:dataTable ... scrollable="true" scrollHeight="400">
```

- Use a pixel `scrollHeight` (e.g. `400`) or a viewport-relative one (`scrollHeight="60vh"`).
- Inside a scrollable table the header is automatically fixed — `stickyHeader` is not needed.

### Frozen columns — wide tables only

When a table needs horizontal scrolling (sum of column widths exceeds the container), freeze the identifying column(s) so rows stay recognizable:

```xhtml
<p:dataTable ... scrollable="true" scrollWidth="100%" frozenColumns="2">
```

- Freeze at most the row-index + item/name columns. Freezing more defeats the purpose.
- Column `width` attributes are required for frozen layouts to compute correctly (see handbook § DataTable Compact Size for the width table).

---

## 2. Action Reachability — Sticky Action Areas

**Rule: the page's primary actions (Save / Finalize / Approve / Settle / Add) must be reachable without scrolling, at any scroll position.**

On long data-entry pages the three-zone panel header (with Save/Finalize/Approve) scrolls out of view as soon as the user works down the item list — then they scroll up to save, and scroll back down to continue. Two standard fixes, both backed by classes in `src/main/webapp/resources/css/ohmis.css`:

### Option A — sticky panel header (preferred for pages using the three-zone header)

Add `styleClass="sticky-panel-header"` to the main transaction `p:panel`. The panel's title bar — which contains the three-zone action layout — stays pinned to the viewport top while its content scrolls.

```xhtml
<p:panel styleClass="sticky-panel-header" rendered="...">
    <f:facet name="header">
        <!-- three-zone header with Save / Finalize / Approve -->
    </f:facet>
    <!-- long content -->
</p:panel>
```

### Option B — sticky bottom action bar (for pages whose actions are below the form)

Wrap the action buttons in a block `h:panelGroup` with `styleClass="sticky-action-bar"`. The bar pins to the bottom of the viewport until the user scrolls past its natural position.

```xhtml
<h:panelGroup layout="block" styleClass="sticky-action-bar d-flex gap-2 justify-content-end">
    <p:commandButton id="btnCancel" value="Cancel" styleClass="ui-button-danger ui-button-outlined" .../>
    <p:commandButton id="btnSettle" value="Settle" icon="pi pi-check-circle"
                     styleClass="ui-button-stage-approve" style="min-width:120px" .../>
</h:panelGroup>
```

Rules:
- Pick **one** of the two per page — never both.
- Button ordering inside a sticky bar follows the handbook rule: least important on the left, primary action rightmost.
- The bar must contain **only buttons** (and a total/summary text at most). Inputs never go in a sticky bar.
- Both classes degrade gracefully: if an ancestor container breaks `position: sticky`, the layout simply behaves as before.

Reference implementation: `src/main/webapp/pharmacy/pharmacy_issue.xhtml` (sticky panel header + sticky table header).

---

## 3. Keyboard-First Data Entry

Heads-down entry (pharmacy issue, GRN, billing) must be completable **without touching the mouse**: focus lands in the first field, Enter adds the row, focus returns for the next row.

### `p:defaultCommand` — the standard for the Enter key

`p:defaultCommand` declares which button the Enter key triggers within a form. This is the **canonical mechanism** — already used on 400+ pages — and is always preferred over per-field `onkeydown` JavaScript when the goal is simply "Enter = Add/Search":

```xhtml
<p:commandButton id="btnAdd" value="Add" action="#{controller.addBillItem}" ... />
<p:defaultCommand target="btnAdd" />
```

Rules:
- **Every data-entry form and every report-filter form gets a `p:defaultCommand`.** Without it, Enter submits via the first button in the form — on entry pages that silently wipes the in-progress bill (the failure mode described in the handbook § Data-entry components).
- Point it at the *safe, repeatable* action: **Add** on item-entry forms, **Process/Search** on filter forms. Never point it at Settle/Finalize/Approve — irreversible actions must be a deliberate click (plus `confirm()`).
- Only fall back to the `onkeydown`/`onkeyup` guard pattern (handbook § Data-entry components) when a single form needs *different* Enter behaviour per field (e.g. Enter in quantity commits the AJAX-bound value first). `p:defaultCommand` and the guard pattern can coexist: the guard suppresses Enter on specific fields; `defaultCommand` handles the rest.

### `p:focus` — put the cursor where the work starts

```xhtml
<!-- Initial focus: first entry field on page load -->
<p:focus id="focusItem" for="acStock" />
```

Rules:
- Every data-entry page sets initial focus on the first input the user needs (usually the item autocomplete).
- After an Add action, return focus to the first field by putting the relevant `p:focus` component id in the button's `update` list — that re-renders the focus component and re-applies it (see `pharmacy_issue.xhtml`: `btnAdd` updates `focusItem`).
- After item selection, move focus forward to quantity the same way (`p:ajax event="itemSelect" update="... focusQty"`).

### `accesskey` — shortcuts for the page's main actions

Assign `accesskey` on the handful of buttons/fields a power user hits constantly. Project convention (from `pharmacy_issue.xhtml`):

| Key | Action |
|-----|--------|
| `i` | Item / first entry field |
| `q` | Quantity |
| `a` | Add (row) |
| `s` | Save draft |
| `f` | Finalize |
| `n` | New transaction |

Keep these assignments consistent across modules — a pharmacist moving between issue/transfer/GRN pages should not relearn keys.

### Tab order and field behaviour

- Lay fields out in entry order (left→right, top→bottom) so the natural DOM tab order matches the workflow — do not use explicit `tabindex` values (they are unmaintainable across includes).
- Read-only display fields (rates, computed values) must not trap tabbing: they are `readonly="true"`, which keeps them out of the way, and they should never sit *between* two fields the user types into.
- On numeric fields add `onfocus="this.select();"` so tabbing in replaces the old value instead of appending.

---

## 4. Large Datasets — Lazy Loading and Pagination

A page that loads thousands of rows into the DOM is the root cause of "the page is slow and scrolling is painful". Budget: **a table should render at most ~50–100 rows per request.**

### Decision guide

| Situation | Pattern |
|---|---|
| Transaction items being entered (10s of rows) | Plain table, `stickyHeader`, no paginator |
| Search/list pages, report results (100s–1000s of rows) | `paginator="true" rows="25"` — and **lazy loading** when the full list is expensive |
| Very large result sets (10,000+ rows), or lists users scan continuously | `LazyDataModel` + `virtualScroll="true" scrollRows="40"` |
| Unbounded exports | Do not render in the browser — Excel export (`developer_docs/feature/excel-export-html-table.md`) |

### Lazy loading with `LazyDataModel`

A paginated table over a `List` still executes the full query and holds the whole list in the session bean; only rendering is paginated. For large tables bind a `org.primefaces.model.LazyDataModel` so **only the visible page is queried** (`load()` receives first/pageSize/sort/filter and issues a ranged JPQL query + a count query):

```xhtml
<p:dataTable value="#{controller.lazyBills}" var="b" lazy="true"
             paginator="true" rows="25" stickyHeader="true"
             styleClass="p-datatable-sm">
```

```java
lazyBills = new LazyDataModel<Bill>() {
    @Override
    public int count(Map<String, FilterMeta> filterBy) {
        return billFacade.countByJpql(countJpql, params).intValue();
    }
    @Override
    public List<Bill> load(int first, int pageSize, Map<String, SortMeta> sortBy, Map<String, FilterMeta> filterBy) {
        return billFacade.findByJpql(jpql, params, first, pageSize);
    }
};
```

Rules:
- New search/list pages over unbounded tables (bills, patients, stock histories) should be lazy from day one.
- Keep default `rows` at 25 (50 max). Offer `rowsPerPageTemplate="25,50,100"` rather than a big default.
- Report pages that intentionally show a full day/period in one table must cap the range in the query (date filters are mandatory) and state the row count in the header.
- Autocompletes are the same problem in miniature — `maxResults`, `minQueryLength`, `queryDelay` are mandatory per the handbook.

---

## 5. Reducing Button Clutter

A page with 10+ visible buttons has no visual hierarchy — users scan longer and misclick more. Budget: **at most 5 always-visible buttons per action zone.**

- **Primary + stage actions stay as buttons** (Save/Finalize/Approve, Add, Process, Print). These follow the three-stage classes and three-zone header layout in the handbook.
- **Demote secondary/rare actions into a `p:splitButton`** — the common action is the button face, the variants live in the dropdown:

```xhtml
<p:splitButton id="btnPrint" value="Print" icon="pi pi-print" ajax="false"
               action="#{controller.printA4()}" styleClass="ui-button-info">
    <p:menuitem value="Print (Thermal)" icon="pi pi-print" action="#{controller.printPos()}" ajax="false"/>
    <p:menuitem value="Export to Excel" icon="pi pi-file-excel" action="#{controller.exportExcel()}" ajax="false"/>
</p:splitButton>
```

- **Group unrelated rare actions under a `p:menuButton`** labelled "More Actions" (e.g. reprint, audit trail, view log). Never bury a stage action (Save/Finalize/Approve) in a menu.
- Row-level actions: maximum 3 icon buttons per row (each with the interpolated `title` required by the accessibility rules); further actions go in a row-level menu or the detail view.

---

## 6. Feedback During Slow Operations

Users double-click and re-submit because nothing tells them the system is working. Every page with an action that can take >1 second needs a busy indicator.

### Global AJAX indicator

For AJAX actions, add one `p:ajaxStatus` per page (or rely on the template if present):

```xhtml
<p:ajaxStatus onstart="PF('statusDialog').show()" oncomplete="PF('statusDialog').hide()"/>
<p:dialog widgetVar="statusDialog" modal="true" draggable="false" closable="false" resizable="false" showHeader="false">
    <h:outputText value="Processing..." />
</p:dialog>
```

### Blocking a region

To block just a form/table during an AJAX action, pair the trigger with `p:blockUI`:

```xhtml
<p:commandButton id="btnProcess" value="Process" action="#{controller.process}" update="tblResults"/>
<p:blockUI block="tblResults" trigger="btnProcess">
    <h:outputText value="Loading..." />
</p:blockUI>
```

### Non-AJAX submits

`ajax="false"` buttons (report generation, downloads) cannot use `p:ajaxStatus`. Keep the `confirm()` + server-side re-entrancy guard from the handbook — and never `this.disabled=true` (breaks the POST; see [AJAX guidelines](../jsf/ajax-update-guidelines.md)).

---

## Checklist for a New Data-Dense Page

- [ ] `stickyHeader="true"` + `styleClass="p-datatable-sm"` on every long table
- [ ] Wide table? `scrollable` + `frozenColumns` on the identifying columns
- [ ] Primary actions reachable at any scroll position (`sticky-panel-header` **or** `sticky-action-bar`)
- [ ] `p:defaultCommand` targets the safe repeatable action; irreversible actions require a click + `confirm()`
- [ ] `p:focus` on the first entry field; focus returned via `update` after Add
- [ ] `accesskey` on the main entry/action controls, following the project key map
- [ ] Large lists are lazy (`LazyDataModel`) or capped; default page size 25
- [ ] ≤5 always-visible buttons per zone; secondary actions demoted to split/menu buttons
- [ ] Busy indicator (`p:ajaxStatus` / `p:blockUI`) on actions that can take >1 s

## Related Guides

- [UI Development Handbook](comprehensive-ui-guidelines.md) — page structure, button standards, accessibility
- [JSF AJAX Update Guidelines](../jsf/ajax-update-guidelines.md)
- [PrimeFaces DataTable Selection](../jsf/primefaces-datatable-selection.md)
- [Performance Optimization skill](../../.claude/skills/performance-optimization/SKILL.md)
