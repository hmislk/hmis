---
name: ui-guidelines
description: >
  HMIS UI development guidelines for JSF/XHTML pages. Use when creating or modifying
  XHTML pages, PrimeFaces components, page layouts, forms, buttons, panels, datatables,
  or any frontend/UI work. Covers page structure, layout patterns, panel best practices,
  form inputs, button styling, responsive design, and accessibility rules.
user-invocable: true
---

# HMIS UI Development Guidelines

When working on any UI/XHTML task, follow these rules and patterns.

## Critical Rules

- **UI-ONLY CHANGES**: When UI improvements are requested, make ONLY frontend/XHTML changes
- **KEEP IT SIMPLE**: Use existing controller properties and methods - avoid introducing filteredValues, globalFilter, or new backend logic
- **FRONTEND FOCUS**: Stick to HTML/CSS styling, PrimeFaces component attributes, and layout improvements
- **ERP UI RULE**: Use `h:outputText` instead of HTML headings (h1-h6)
- **PRIMEFACES CSS**: Use PrimeFaces button classes, not Bootstrap button classes
- **XHTML STRUCTURE**: HTML DOCTYPE with `ui:composition` and template inside `h:body`
- **XML ENTITIES**: Always escape ampersands as `&amp;` in XHTML attributes

## Page Skeleton

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

## Layout and Typography

- Use **PrimeFaces components for interaction** and **Bootstrap utilities for layout** (`row`, `col-*`, `d-flex`, spacing)
- Prefer `p:panelGrid` for grid with header; only use `p:panel` when you need facets/panel styling
- Use `h:outputText` and `p:outputLabel` for headings/labels instead of HTML heading tags
- Keep screens dense and business-focused

## Panel Structure

- Use `f:facet name="header"` for panel-related actions
- Use `p:panelGrid columns="2" layout="tabular"` for label-value pairs
- Avoid over-nesting; place dataTable directly in panel content

## Forms and Inputs

- Align labels with `p:outputLabel` + PrimeFaces components; include `for` attributes
- Heavy operations (reports, exports) use `ajax="false"` for file downloads
- Include descriptive `title` attributes on interactive elements

## Buttons

- **Navigation**: `ui-button-info ui-button-outlined`
- **Save/Submit**: `ui-button-success`
- **Delete/Cancel**: `ui-button-danger`
- **Secondary actions**: `ui-button-warning` or `ui-button-secondary`
- **Always add icons**: Use Font Awesome (`fa fa-*`) icons

## Report Pages

- **One layout for every report**, top to bottom and full width: panel header → 8-column filter grid → one row of action buttons (Process, then exports) → compact results table with footer totals
- **Never put filters in a side column** (`col-md-2` + `col-md-10`) or stack one filter per line; this is a web application, so save space
- **Standard filters**: From / To, Institution / Site / Department on one row, then report-specific filters; not Department alone
- Reference page: `pharmacy/reports/summary_reports/pharmacy_income_report.xhtml`. Rules: [comprehensive-ui-guidelines.md § Report Page Layout](../../../developer_docs/ui/comprehensive-ui-guidelines.md#report-page-layout-all-report-pages-look-the-same)

## Touched page = upgraded page

When a change edits an `.xhtml` page, bring that page up to these guidelines in the same PR.

Pre-PR checklist (link, don't copy; read the section):
- [ ] Panel header with both navigation and action buttons is the three-zone header (report pages keep Process/export below the filter grid, not in a header bar): [Panel Header — Three-Zone Layout](../../../developer_docs/ui/comprehensive-ui-guidelines.md#panel-header--three-zone-layout-required-for-action-rich-panels); sticky if the page scrolls: [Data-Dense § 2](../../../developer_docs/ui/data-dense-page-patterns.md#2-action-reachability--sticky-action-areas)
- [ ] Data-entry, transaction and report-result tables are compact (`p-datatable-sm`, `stickyHeader` on long ones): [DataTable Compact Size](../../../developer_docs/ui/comprehensive-ui-guidelines.md#datatable-compact-size-required-for-data-entry-and-multi-column-tables)
- [ ] Buttons use the standard styles and Font Awesome icons: [Buttons and Workflow Actions](../../../developer_docs/ui/comprehensive-ui-guidelines.md#buttons-and-workflow-actions); fewer buttons: [Data-Dense § 5](../../../developer_docs/ui/data-dense-page-patterns.md#5-reducing-button-clutter)
- [ ] Labels, filter grid and panel layout match sibling pages: [Panel Structure](../../../developer_docs/ui/comprehensive-ui-guidelines.md#panel-structure-best-practices); reports: [Report Page Layout](../../../developer_docs/ui/comprehensive-ui-guidelines.md#report-page-layout-all-report-pages-look-the-same)

Bounds:
- No change to behaviour, bean calls, ids used by `update=`/`process=` or by tests, or composite component names.
- Very large page: upgrade the panels and forms the change touches; list the rest in the PR body.
- Keep the tidy-up in its own commit (`style(ui): ...`) so reviewers can separate it.
- Shared templates, layout files and anything under `resources/` or composite components are excluded.
- Hotfix branches: do not apply it unless asked; keep hotfixes minimal.

## For complete reference

Read [developer_docs/ui/comprehensive-ui-guidelines.md](../../../developer_docs/ui/comprehensive-ui-guidelines.md) for the full UI handbook including responsive design, accessibility, datatables, dialogs, and more.

For data-dense pages (large tables, many inputs, many buttons), also read [developer_docs/ui/data-dense-page-patterns.md](../../../developer_docs/ui/data-dense-page-patterns.md) — sticky headers/action bars, keyboard-first entry (`p:defaultCommand`, `p:focus`), lazy loading, and busy indicators.
