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

## Pattern: header/footer on top of `<p:dataExporter>` via `postProcessor`

### When to use
- The report already renders as a `<p:dataTable>` and exports fine with `<p:dataExporter>`
- You just need to add a report title, active filters, and/or a "Printed by / Printed at" line to the exported file — not restructure the export itself

`<p:dataExporter>` only serializes the table's column headers and row data; it ignores any `<f:facet name="header">`/`name="footer">` set directly on `<p:dataTable>`. To inject extra rows, use the exporter's `postProcessor` attribute, which hands your controller the already-populated POI `Workbook` before it's streamed to the response.

### Shared utility (`ExcelController`)
Two reusable methods added for issue #17615 do the row-shifting/writing so individual reports don't reimplement it:
- `insertExcelReportHeader(Sheet sheet, String reportTitle, List<String[]> filterPairs, int mergeCol)` — shifts existing rows down and writes a title row + one row per `{label, value}` filter pair above the table. Always pass every filter the report supports, with an "All"/"No" fallback for unset ones.
- `appendExcelPrintedByFooter(Sheet sheet, int mergeCol)` — appends a "Printed by: X" / "Printed at: Y" row after the last row.

`mergeCol` is the last exported column's 0-based index (column count − 1). **Both methods guard `mergeCol == 0`/`1` before calling `addMergedRegion`** — POI throws `IllegalStateException: Merged region ... must contain 2 or more cells` if you try to merge a single cell, which happens for narrow reports (2-3 columns) or reports that export zero data rows. Any new caller passing a dynamically-computed `mergeCol` (e.g. `sheet.getRow(0).getLastCellNum() - 1`) should rely on these guards rather than assuming a fixed column count.

### Controller method
```java
public void postProcessXLS<ReportName>(Object document) {
    if (!(document instanceof Workbook)) {
        return;
    }
    Sheet sheet = ((Workbook) document).getSheetAt(0);
    List<String[]> filterPairs = new ArrayList<>();
    filterPairs.add(new String[]{"Department", department != null ? department.getName() : "All"});
    // ... one entry per filter this report exposes ...
    excelController.insertExcelReportHeader(sheet, "<Report Title>", filterPairs, mergeCol);
    excelController.appendExcelPrintedByFooter(sheet, mergeCol);
}
```

### XHTML wiring
```xml
<p:dataExporter type="xlsx" target="tbl" fileName="..." postProcessor="#{bean.postProcessXLS<ReportName>}"/>
```
For the on-screen/print rendering (not the Excel file), add matching facets directly to `<p:dataTable>` — these two are independent of the exporter:
```xml
<f:facet name="header">...report title + filter summary...</f:facet>
<f:facet name="footer"><common:report_print_footer/></f:facet>
```
`report_print_footer` (`resources/ezcomp/common/report_print_footer.xhtml`) is a no-arg composite that prints "Printed By: `#{sessionController.loggedUser.webUserPerson.name}`" / "Printed At: `#{sessionController.currentDate}`" — reuse it instead of duplicating the markup per report.

PrimeFaces also supports the same `preProcessor`/`postProcessor` attributes on `<p:dataExporter type="pdf">`, receiving a `com.lowagie.text.Document` (OpenPDF) — see `ReportsStock.preProcessPdfDepartmentViceStock`/`postProcessPdfDepartmentViceStock` for a working example. Fully-qualify `com.lowagie.text.*` types if the same controller also has iText5 (`com.itextpdf.text.*`) imports for an unrelated export method, since the two libraries share class names.

### Reference implementation
`ReportsStock.postProcessXLSStockReportByItem` + `pharmacy_report_department_stock_by_item_DTO.xhtml`.

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
