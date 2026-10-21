package com.divudi.bean.inward;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Builds a flat tabular report (heading, filter lines, one header row, data
 * rows, one totals row, notes) and streams it to the browser as an Excel
 * workbook or a landscape PDF.
 *
 * Used where p:dataExporter falls short: it cannot write a title or the
 * filter period, exports formatted amounts as text, drops footer totals and
 * squeezes long bill numbers in the PDF.
 *
 * Cell values are {@link String}, {@link Number} (written as a numeric cell,
 * formatted #,##0.00) or {@link Date} (a date cell in Excel, the date
 * pattern in the PDF).
 * A totals entry of null leaves that footer cell blank.
 */
public class TabularReportExporter {

    public enum Align {
        LEFT, RIGHT, CENTER
    }

    private final String institutionName;
    private final String title;
    private final String printedBy;
    private final String datePattern;
    private final List<String[]> filters = new ArrayList<>();
    private final List<String> headers = new ArrayList<>();
    private final List<Float> widths = new ArrayList<>();
    private final List<Align> aligns = new ArrayList<>();
    private final List<Object[]> rows = new ArrayList<>();
    private final List<String> notes = new ArrayList<>();
    private String totalLabel = "Total";
    private Double[] totals;

    public TabularReportExporter(String institutionName, String title, String printedBy, String datePattern) {
        this.institutionName = institutionName;
        this.title = title;
        this.printedBy = printedBy;
        this.datePattern = datePattern;
    }

    public TabularReportExporter filter(String label, String value) {
        filters.add(new String[]{label, value});
        return this;
    }

    public TabularReportExporter column(String header, float width, Align align) {
        headers.add(header);
        widths.add(width);
        aligns.add(align);
        return this;
    }

    public TabularReportExporter row(Object... values) {
        rows.add(values);
        return this;
    }

    public TabularReportExporter totals(String label, Double... values) {
        this.totalLabel = label;
        this.totals = values;
        return this;
    }

    public TabularReportExporter note(String note) {
        notes.add(note);
        return this;
    }

    private String format(Object value, SimpleDateFormat df) {
        if (value == null) {
            return "";
        }
        if (value instanceof Date) {
            return df.format((Date) value);
        }
        if (value instanceof Number) {
            return String.format("%,.2f", ((Number) value).doubleValue());
        }
        return value.toString();
    }

    private String printedLine() {
        return "Printed By: " + (printedBy == null ? "-" : printedBy)
                + "   at " + new SimpleDateFormat("dd MMM yyyy HH:mm").format(new Date());
    }

    public void writeExcel(String fileNameBase) throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        int colCount = headers.size();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Report");

            org.apache.poi.ss.usermodel.Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            org.apache.poi.ss.usermodel.Font boldWhiteFont = workbook.createFont();
            boldWhiteFont.setBold(true);
            boldWhiteFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            CellStyle subtitleStyle = workbook.createCellStyle();
            subtitleStyle.setAlignment(HorizontalAlignment.CENTER);
            CellStyle labelStyle = workbook.createCellStyle();
            labelStyle.setFont(boldFont);

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(boldWhiteFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(headerStyle);

            CellStyle textStyle = workbook.createCellStyle();
            setBorders(textStyle);
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.cloneStyleFrom(textStyle);
            numberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
            numberStyle.setAlignment(HorizontalAlignment.RIGHT);
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.cloneStyleFrom(textStyle);
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("dd-mmm-yyyy"));

            CellStyle totalStyle = workbook.createCellStyle();
            totalStyle.setFont(boldWhiteFont);
            totalStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            setBorders(totalStyle);
            CellStyle totalNumberStyle = workbook.createCellStyle();
            totalNumberStyle.cloneStyleFrom(totalStyle);
            totalNumberStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
            totalNumberStyle.setAlignment(HorizontalAlignment.RIGHT);

            int r = 0;
            r = mergedRow(sheet, r, institutionName, titleStyle, colCount);
            r = mergedRow(sheet, r, title, titleStyle, colCount);
            r = mergedRow(sheet, r, printedLine(), subtitleStyle, colCount);
            r++;
            for (String[] f : filters) {
                Row row = sheet.createRow(r);
                Cell l = row.createCell(0);
                l.setCellValue(f[0]);
                l.setCellStyle(labelStyle);
                row.createCell(1).setCellValue(f[1]);
                if (colCount > 2) {
                    sheet.addMergedRegion(new CellRangeAddress(r, r, 1, colCount - 1));
                }
                r++;
            }
            r++;

            Row headerRow = sheet.createRow(r++);
            for (int c = 0; c < colCount; c++) {
                Cell cell = headerRow.createCell(c);
                cell.setCellValue(headers.get(c));
                cell.setCellStyle(headerStyle);
            }

            for (Object[] values : rows) {
                Row row = sheet.createRow(r++);
                for (int c = 0; c < colCount; c++) {
                    Object v = c < values.length ? values[c] : null;
                    Cell cell = row.createCell(c);
                    if (v instanceof Number) {
                        cell.setCellValue(((Number) v).doubleValue());
                        cell.setCellStyle(numberStyle);
                    } else if (v instanceof Date) {
                        cell.setCellValue((Date) v);
                        cell.setCellStyle(dateStyle);
                    } else {
                        cell.setCellValue(v == null ? "" : v.toString());
                        cell.setCellStyle(textStyle);
                    }
                }
            }

            if (totals != null) {
                Row row = sheet.createRow(r++);
                for (int c = 0; c < colCount; c++) {
                    Cell cell = row.createCell(c);
                    Double v = c < totals.length ? totals[c] : null;
                    if (v != null) {
                        cell.setCellValue(v);
                        cell.setCellStyle(totalNumberStyle);
                    } else {
                        cell.setCellValue(c == 0 ? totalLabel : "");
                        cell.setCellStyle(totalStyle);
                    }
                }
            }

            // Size columns before writing the notes: they sit unmerged in column 0
            // and would otherwise stretch it to the width of the longest note.
            for (int c = 0; c < colCount; c++) {
                sheet.autoSizeColumn(c);
            }

            if (!notes.isEmpty()) {
                r++;
                for (String n : notes) {
                    sheet.createRow(r++).createCell(0).setCellValue(n);
                }
            }

            response.reset();
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileNameBase + "_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx");
            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
                out.flush();
            }
            facesContext.responseComplete();
        }
    }

    public void writePdf(String fileNameBase) throws IOException {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) facesContext.getExternalContext().getResponse();
        int colCount = headers.size();
        SimpleDateFormat df = new SimpleDateFormat(datePattern);

        // Wide tables (e.g. the debtor report) drop a point so long bill numbers
        // and amounts still fit their columns on A4 landscape.
        float bodySize = colCount > 12 ? 7f : 8f;
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, bodySize, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, bodySize);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, bodySize, Color.WHITE);
        Color headerBg = new Color(89, 89, 89);
        Color stripeBg = new Color(242, 242, 242);
        Color totalBg = new Color(0, 77, 64);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 18, 18, 24, 30);
            PdfWriter writer = PdfWriter.getInstance(document, baos);
            writer.setPageEvent(new PdfFooter(FontFactory.getFont(FontFactory.HELVETICA, 7, Color.DARK_GRAY)));
            document.open();

            Paragraph p1 = new Paragraph(institutionName, titleFont);
            p1.setAlignment(Element.ALIGN_CENTER);
            document.add(p1);
            Paragraph p2 = new Paragraph(title, subtitleFont);
            p2.setAlignment(Element.ALIGN_CENTER);
            p2.setSpacingAfter(4);
            document.add(p2);

            // Filters in two label/value pairs per line, so ten filters take five lines.
            if (!filters.isEmpty()) {
                PdfPTable filterTable = new PdfPTable(4);
                filterTable.setWidthPercentage(100);
                filterTable.setSpacingBefore(4);
                filterTable.setSpacingAfter(8);
                filterTable.setWidths(new float[]{1.2f, 3f, 1.2f, 3f});
                for (String[] f : filters) {
                    filterTable.addCell(filterCell(f[0] + ":", labelFont));
                    filterTable.addCell(filterCell(f[1], smallFont));
                }
                if (filters.size() % 2 != 0) {
                    filterTable.addCell(filterCell("", smallFont));
                    filterTable.addCell(filterCell("", smallFont));
                }
                document.add(filterTable);
            }

            PdfPTable table = new PdfPTable(colCount);
            table.setWidthPercentage(100);
            float[] w = new float[colCount];
            for (int c = 0; c < colCount; c++) {
                w[c] = widths.get(c);
            }
            table.setWidths(w);
            table.setHeaderRows(1);

            // Headers take their column's alignment, so amount headers sit over the amounts.
            for (int c = 0; c < colCount; c++) {
                addCell(table, headers.get(c), headerFont, pdfAlign(aligns.get(c)), headerBg);
            }
            int rowNo = 0;
            for (Object[] values : rows) {
                Color bg = rowNo++ % 2 == 1 ? stripeBg : null;
                for (int c = 0; c < colCount; c++) {
                    Object v = c < values.length ? values[c] : null;
                    addCell(table, format(v, df), cellFont, pdfAlign(aligns.get(c)), bg);
                }
            }
            if (totals != null) {
                // The label spans the blank leading cells instead of squeezing into the first column.
                int labelSpan = 0;
                while (labelSpan < colCount && (labelSpan >= totals.length || totals[labelSpan] == null)) {
                    labelSpan++;
                }
                if (labelSpan > 0) {
                    PdfPCell label = createCell(totalLabel, totalFont, Element.ALIGN_LEFT, totalBg);
                    label.setColspan(labelSpan);
                    table.addCell(label);
                }
                for (int c = labelSpan; c < colCount; c++) {
                    Double v = c < totals.length ? totals[c] : null;
                    addCell(table, v != null ? format(v, df) : "", totalFont, Element.ALIGN_RIGHT, totalBg);
                }
            }
            document.add(table);

            if (!notes.isEmpty()) {
                Paragraph np = new Paragraph("Notes", labelFont);
                np.setSpacingBefore(8);
                document.add(np);
                for (String n : notes) {
                    Paragraph line = new Paragraph("- " + n, smallFont);
                    line.setSpacingBefore(2);
                    document.add(line);
                }
            }
            document.close();

            byte[] bytes = baos.toByteArray();
            response.reset();
            response.setContentType("application/pdf");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileNameBase + "_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".pdf");
            response.setContentLength(bytes.length);
            try (OutputStream out = response.getOutputStream()) {
                out.write(bytes);
                out.flush();
            }
            facesContext.responseComplete();
        }
    }

    private int pdfAlign(Align align) {
        if (align == Align.RIGHT) {
            return Element.ALIGN_RIGHT;
        }
        if (align == Align.CENTER) {
            return Element.ALIGN_CENTER;
        }
        return Element.ALIGN_LEFT;
    }

    private void addCell(PdfPTable table, String text, Font font, int alignment, Color bg) {
        table.addCell(createCell(text, font, alignment, bg));
    }

    private PdfPCell createCell(String text, Font font, int alignment, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(3f);
        cell.setBorderColor(Color.GRAY);
        if (bg != null) {
            cell.setBackgroundColor(bg);
        }
        return cell;
    }

    private PdfPCell filterCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "" : text, font));
        cell.setBorder(PdfPCell.NO_BORDER);
        cell.setPadding(1.5f);
        return cell;
    }

    /**
     * Writes the printed-by line and the page number at the foot of every page.
     */
    private class PdfFooter extends PdfPageEventHelper {

        private final Font font;

        PdfFooter(Font font) {
            this.font = font;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            float y = document.bottom() - 14;
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(printedLine(), font), document.left(), y, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Page " + writer.getPageNumber(), font), document.right(), y, 0);
        }
    }

    private int mergedRow(XSSFSheet sheet, int r, String value, CellStyle style, int colCount) {
        Row row = sheet.createRow(r);
        Cell cell = row.createCell(0);
        cell.setCellValue(value == null ? "" : value);
        cell.setCellStyle(style);
        if (colCount > 1) {
            sheet.addMergedRegion(new CellRangeAddress(r, r, 0, colCount - 1));
        }
        return r + 1;
    }

    private void setBorders(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
