# Excel Export for HTML-Based Report Tables

## Overview

Reports that render data in plain HTML `<table>` elements (rather than PrimeFaces `<p:dataTable>`) cannot use PrimeFaces' built-in `<p:dataExporter>`. Instead, the project uses a direct Apache POI approach where the controller writes the workbook bytes straight to the `HttpServletResponse`.

## Pattern: `void downloadExcel()` via `HttpServletResponse`

### When to use
- The report table is rendered as a plain HTML `<table>` (not a `<p:dataTable>`)
- Data is already held in memory in a `@SessionScoped` controller
- The structure is custom (e.g. grouped rows, subtotals, grand totals)

### When NOT to use
- For PrimeFaces DataTables → use `<p:dataExporter>`
- For `ReportTemplateRowBundle`-structured data → use `ExcelController.createExcelForBundle()`

---

## Implementation

### 1. Controller method

```java
public void downloadExcel() {
    if (groups == null || groups.isEmpty()) {
        return;
    }

    FacesContext facesContext = FacesContext.getCurrentInstance();
    HttpServletResponse response =
            (HttpServletResponse) facesContext.getExternalContext().getResponse();

    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
        XSSFSheet sheet = workbook.createSheet("Sheet Name");

        // 1. Build cell styles (title, header, data, subtotal, grand total)
        // 2. Write header rows (institution, report title, date range)
        // 3. Write column header row
        // 4. Iterate data and write rows
        // 5. Write grand total row
        // 6. Auto-size columns

        String filename = "Report_" + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + filename);
        try (OutputStream out = response.getOutputStream()) {
            workbook.write(out);
        }
        facesContext.responseComplete();   // <-- critical: tells JSF not to render the view

    } catch (Exception e) {
        e.printStackTrace();
    }
}
```

**Key rules:**
- `facesContext.responseComplete()` must always be called — it prevents JSF from trying to render the page on top of the binary stream.
- The method return type is `void`; all output goes via `HttpServletResponse`.
- Use `try-with-resources` on the workbook so it is always closed.

### 2. Required imports

```java
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
```

Apache POI is already a project dependency; no `pom.xml` changes are needed.

### 3. XHTML button

```xml
<p:commandButton value="Excel"
                 icon="fas fa-file-excel"
                 class="ui-button-success mr-1"
                 actionListener="#{myController.downloadExcel}"
                 ajax="false" />
```

`ajax="false"` is mandatory — without it the browser will not receive the file download.

---

## Excel file structure

```text
Row 0   : Institution name                  (title, 14pt bold, centered, full-width merged)
Row 1   : Report title                      (centered, full-width merged)
Row 2   : Printed By / timestamp            (centered, full-width merged)
Row 3   : (blank)
Row 4   : Filter — Date Basis              (col 0 = bold label, col 1–10 = value)
Row 5   : Filter — From
Row 6   : Filter — To
Row 7   : Filter — Credit Company
Row 8   : Filter — Institution
Row 9   : Filter — Site
Row 10  : Filter — Department
Row 11  : Filter — Admission Type
Row 12  : Filter — Payment Method
Row 13  : Filter — Outstanding Only
Row 14  : (blank)
Row 15  : Column header row
Row 16+ : Data (group header → bill rows → subtotal, repeating per company)
Last    : Grand total row
```

### Filter rows pattern

Each active filter is shown as a two-cell row: bold label in column 0, value merged across columns 1–10. Always show all filters — use "All" / "No" for unset values so the reader can reconstruct the exact query that produced the report.

```java
rowIdx = createFilterRow(sheet, rowIdx, "Credit Company",
        institution != null ? institution.getName() : "All",
        filterLabelStyle, filterValueStyle, COL_COUNT);
```

## Styling conventions

| Row type        | Background            | Font         |
|-----------------|-----------------------|--------------|
| Title / heading | None (centered)       | Bold 14pt    |
| Subtitle        | None (centered)       | Normal       |
| Filter label    | None                  | Bold, left   |
| Filter value    | None                  | Normal, left |
| Column headers  | `GREY_50_PERCENT`     | Bold, White  |
| Group header    | `GREY_25_PERCENT`     | Bold         |
| Data row        | None                  | Normal       |
| Subtotal row    | `LIGHT_YELLOW`        | Bold         |
| Grand total row | `DARK_TEAL`           | Bold, White  |

Use `#,##0.00` data format for all monetary cells.

---

## Reference implementations

| Report | Controller | XHTML |
|--------|-----------|-------|
| OPD Credit Due | `CreditCompanyDueController.downloadExcel()` | `credit/credit_company_opd_due.xhtml` |
| Inpatient CC Debtor Grouped | `CreditCompanyDebtorGroupedReportController.downloadExcel()` | `credit/inward_credit_company_debtor_grouped_report.xhtml` |

---

## Common mistakes

| Mistake | Consequence |
|---------|-------------|
| Forgetting `ajax="false"` on button | File never downloads; AJAX partial update replaces part of the page with binary garbage |
| Forgetting `facesContext.responseComplete()` | JSF appends the HTML view after the binary stream, corrupting the file |
| Not closing the workbook | Memory leak in production; file may be incomplete |
| Using `p:fileDownload` with a `void` method | `p:fileDownload` requires a `StreamedContent` property — incompatible with this pattern |
