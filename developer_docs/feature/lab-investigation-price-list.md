# Lab Investigation & Service Price List

## Overview

A read-only price list report available under **Lab Analytics → Reference → Price List**.

Users select a lab department and the system returns all investigations and services configured for that department, showing their effective prices. An Excel export and print option are provided.

## Navigation Path

```text
Top navbar → Lab (flask icon, fa-flask)
 └─ Lab Analytics
     └─ Left sidebar → "Reference" accordion tab (last tab)
         └─ Price List button
             └─ Select Department → Generate
```

## Fee Resolution Logic

Prices mirror the billing fallback in `BillBeanController.billFeefromBillItem()`:

1. **Site-specific fees** — `ItemFee` where `forInstitution = department.getSite()` and `forCategory IS NULL`
2. **Base fees (fallback)** — `ItemFee` where `forInstitution IS NULL` and `forCategory IS NULL`, only for items not covered by step 1

This means the price shown is exactly the price that would be charged when billing from that department — no discrepancy between what staff see on the price list and what appears on OPD billing.

## Changed Files

| File | Change |
|---|---|
| `com.divudi.bean.common.ItemFeeManager` | Added `fillItemLightsForDepartment(Department dept)` — two-pass JPQL query |
| `com.divudi.bean.lab.LaborataryReportController` | Added `investigationPriceList` field, `generateInvestigationPriceList()`, `downloadInvestigationPriceListExcel()`, `navigateToInvestigationPriceListFromLabAnalytics()` |
| `src/main/webapp/reportLab/investigation_price_list.xhtml` | New page (uses `lab_summeries_index.xhtml` as template) |
| `src/main/webapp/reportLab/lab_summeries_index.xhtml` | Added "Reference" accordion tab with "Price List" button |

## Key Method: `fillItemLightsForDepartment`

Located in `ItemFeeManager.java`. Takes a `Department`, derives `department.getSite()`, runs two JPQL queries against `ItemFee`, merges results, and returns a list of `ItemLight` sorted by name.

If `department.getSite()` is null (department not linked to a site), only the base fees query runs.

## Related Issue

GitHub issue: [#21295](https://github.com/hmislk/hmis/issues/21295)
