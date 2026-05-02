package com.divudi.core.data.reports;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.divudi.bean.channel.ChannelReportTemplateController.OnlineBookingDetialRow;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.OnlineBookingStatus;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.RefundBill;
import com.divudi.core.util.JsfUtil;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

public class Report<T> {

    // List<ReportColumn> columns;
    private LinkedHashMap<String, ReportColumn<T>> columns;
    private List<T> data;
    private boolean serialNoColumnAtStart = false;

    // report metadata
    private String reportName;
    private String fileName;
    private String institutionName;
    private String reportGeneratedBy;
    private Map<String, Object> searchCriteria;

    // Font
    private int fontSize = 8;
    private String boldFont = StandardFonts.HELVETICA_BOLD;
    // private String normalFont = StandardFonts.HELVETICA;

    private static final Logger logger = LoggerFactory.getLogger(Report.class);
    

    public Report() {

    }

    public Report(LinkedHashMap<String, ReportColumn<T>> columns) {
        this.columns = columns;
    }

    public Report(LinkedHashMap<String, ReportColumn<T>> columns, String reportName, String fileName, String institutionName, Map<String, Object> searchCriteria, List<T> data, String reportGeneratedBy) {
        this.columns = columns;
        this.reportName = reportName;
        this.fileName = fileName;
        this.institutionName = institutionName;
        this.searchCriteria = searchCriteria;
        this.data = data;
        this.reportGeneratedBy = reportGeneratedBy;
    }

    public Report(Map<String, Object> searchCriteria, String reportGeneratedBy, String institutionName) {
        this();
        this.searchCriteria = searchCriteria;
        this.reportGeneratedBy = reportGeneratedBy;
        this.institutionName = institutionName; 
    }

    public LinkedHashMap<String, ReportColumn<T>> getColumns() {
        return columns;
    }

    public void setColumns(LinkedHashMap<String, ReportColumn<T>> columns) {
        this.columns = columns;
    }

    public Map<String, Object> getSearchCriteria() {
        return searchCriteria;
    }

    public void setSearchCriteria(Map<String, Object> sh) {
        this.searchCriteria = sh;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getInstitutionName() {
        return institutionName;
    }

    public void setInstitutionName(String institutionName) {
        this.institutionName = institutionName;
    }

    public String getReportGeneratedBy() {
        return reportGeneratedBy;
    }

    public void setReportGeneratedBy(String reportGeneratedBy) {
        this.reportGeneratedBy = reportGeneratedBy;
    }

    public boolean isSerialNoColumnAtStart() {
        return serialNoColumnAtStart;
    }

    public void setSerialNoColumnAtStart(boolean serialNoColumnAtStart) {
        this.serialNoColumnAtStart = serialNoColumnAtStart;
    }


    public void setColumnFooter(Object data, String columnKey) {
        ReportColumn<T> c = columns.get(columnKey);

        if (c != null) {
            c.setFooter(data);
        }
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getBoldFont() {
        return boldFont;
    }

    public void setBoldFont(String boldFont) {
        this.boldFont = boldFont;
    }


    public StreamedContent createPdfAsStream() {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            byte[] pdfData = createPdfBytes();
            if (pdfData == null || pdfData.length == 0) {
                return null;
            }
            ByteArrayInputStream inputStream = new ByteArrayInputStream(pdfData);
            data = null;

            return DefaultStreamedContent.builder()
                    .name(((fileName != null && !fileName.isEmpty()) ? fileName : "Report") + ".pdf")
                    .contentType("application/pdf")
                    .stream(() -> inputStream)
                    .build();
        } catch (Exception e) {
            logger.error(("Failed to generate PDF file: ") + reportName, e);
            JsfUtil.addErrorMessage("Failed to generate PDF file. Please try again.");
            return null;
        }
    }

    public StreamedContent createExcelAsStream() {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            byte[] excelData = createExcelBytes();
            if (excelData == null || excelData.length == 0) {
                return null;
            }
            ByteArrayInputStream inputStream = new ByteArrayInputStream(excelData);
            data = null;

            return DefaultStreamedContent.builder()
                    .name(((fileName != null && !fileName.isEmpty()) ? fileName : "Report") + ".xlsx")
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .stream(() -> inputStream)
                    .build();
        } catch (Exception e) {
            logger.error(("Failed to generate Excel file: ") + reportName, e);
            JsfUtil.addErrorMessage("Failed to generate Excel file. Please try again.");
            return null;
        }
    }

    public byte[] createPdfBytes() throws IOException {
        if (columns == null || columns.isEmpty() || data == null) {
            return new byte[0];
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4.rotate());
        document.setMargins(20, 20, 20, 20);

        addInstitutionPdf(document);
        addReportTitlePdf(document);
        createInfoTablePdfExport(document);

        SolidLine headerLine = new SolidLine(1.5f);
        LineSeparator headerSeparator = new LineSeparator(headerLine);
        headerSeparator.setStrokeColor(ColorConstants.BLACK);
        document.add(headerSeparator);
        document.add(new Paragraph("").setMarginBottom(5));

        Table table = buildTable();
        document.add(table);

        addReportFooter(document);

        document.close();
        return outputStream.toByteArray();
    }

    public byte[] createExcelBytes() throws IOException {
        if (columns == null || columns.isEmpty() || data == null) {
            return new byte[0];
        }

        XSSFWorkbook workbook = new XSSFWorkbook();
        String safeName = WorkbookUtil.createSafeSheetName(reportName);
        XSSFSheet dataSheet = workbook.createSheet(safeName);

        // Create cell styles for headers
        CellStyle titleStyle = workbook.createCellStyle();
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        org.apache.poi.ss.usermodel.Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(boldFont);

        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        org.apache.poi.ss.usermodel.Font normalFont = workbook.createFont();
        normalFont.setBold(true);
        normalFont.setFontHeightInPoints((short) 12);
        centerStyle.setFont(normalFont);

        CellStyle centerSmallStyle = workbook.createCellStyle();
        centerSmallStyle.setAlignment(HorizontalAlignment.CENTER);
        centerSmallStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        org.apache.poi.ss.usermodel.Font smallFont = workbook.createFont();
        smallFont.setBold(true);
        centerSmallStyle.setFont(smallFont);

        CellStyle footer = workbook.createCellStyle();
        footer.setFont(smallFont);

        CellStyle wrapTextStyle = workbook.createCellStyle();
        wrapTextStyle.setWrapText(true);

        int currentRow = 0;

        // Row 0: Institution Name
        Row institutionRow = dataSheet.createRow(currentRow);
        org.apache.poi.ss.usermodel.Cell institutionCell = institutionRow.createCell(0);
        institutionCell.setCellValue(institutionName);
        institutionCell.setCellStyle(titleStyle);
        dataSheet.addMergedRegion(new CellRangeAddress(currentRow, currentRow, 0, 7));
        currentRow++;

        // Row 1: Report Title
        Row titleRow = dataSheet.createRow(currentRow);
        org.apache.poi.ss.usermodel.Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(reportName);
        titleCell.setCellStyle(centerStyle);
        dataSheet.addMergedRegion(new CellRangeAddress(currentRow, currentRow, 0, 7));
        currentRow++;

        // Row 2: Search Criteria
        if (searchCriteria != null && !searchCriteria.isEmpty()) {
            currentRow = addMetaDataToExcelSheet(workbook, dataSheet, currentRow, searchCriteria);
        } else {
            Row criteriaRow = dataSheet.createRow(currentRow);
            org.apache.poi.ss.usermodel.Cell criteriaCell = criteriaRow.createCell(0);
            criteriaCell.setCellValue("Search Criteria: N/A");
            criteriaCell.setCellStyle(centerSmallStyle);
            dataSheet.addMergedRegion(new CellRangeAddress(currentRow, currentRow, 0, 7));
            currentRow+=2;
        }

        // Header Row
        Row detailHeaderRow = dataSheet.createRow(currentRow++);
        int headerCol = 0;

        if (serialNoColumnAtStart) {
            org.apache.poi.ss.usermodel.Cell noCell = detailHeaderRow.createCell(headerCol++);
            noCell.setCellStyle(centerSmallStyle);
            noCell.setCellValue("No");
        }
        for (ReportColumn<T> column : columns.values()) {
            org.apache.poi.ss.usermodel.Cell headerCell = detailHeaderRow.createCell(headerCol++);
            headerCell.setCellStyle(centerSmallStyle);
            headerCell.setCellValue(column.getHeader());
        }

        // Data Rows
        int serial = 1;
        int dataCol = 0;
        for (T row : data) {
            Row dataRow = dataSheet.createRow(currentRow++);
            dataCol = 0;

            if (serialNoColumnAtStart) {
                dataRow.createCell(dataCol++).setCellValue(serial++);
            }
            for (ReportColumn<T> column : columns.values()) {
                Object value = column.extractData(row);
                org.apache.poi.ss.usermodel.Cell cell = dataRow.createCell(dataCol++);
                if (value instanceof String)  {
                    String text = (String) value;
                    cell.setCellValue(text);
                    if (text.contains("\n")) {
                        cell.setCellStyle(wrapTextStyle);
                    }
                } else if (value instanceof Double) {
                    cell.setCellValue((Double) value);
                } else {
                    cell.setCellValue(value != null ? value.toString() : "");
                }
            }
        }

        // Footer Row
        Row footerRow = dataSheet.createRow(currentRow++);
        int footerCol = 0;
        if (serialNoColumnAtStart) {
            footerRow.createCell(footerCol++).setCellValue("");
        }
        for (ReportColumn<T> column : columns.values()) {
            Object footerValue = column.getFooter();
            org.apache.poi.ss.usermodel.Cell cell = footerRow.createCell(footerCol++);
            if (footerValue == null) {
                cell.setCellValue("");
            } else if (footerValue instanceof String)  {
                String text = (String) footerValue;
                cell.setCellValue(text);
                cell.setCellStyle(centerSmallStyle);
            } else if (footerValue instanceof Double) {
                cell.setCellValue((Double) footerValue);
                cell.setCellStyle(footer);
            } else {
                cell.setCellValue(footerValue != null ? footerValue.toString() : "");
                cell.setCellStyle(footer);
            }
        }

        addReportFooterExcel(workbook, dataSheet, ++currentRow);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        return outputStream.toByteArray();
    }

    private void addReportTitlePdf(Document document) {
        if (reportName != null && !reportName.isBlank()) {
            Paragraph title = new Paragraph(reportName)
                    .setFontSize(14)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            document.add(new Paragraph(" "));
        }
    }

    private void addInstitutionPdf(Document document) {
        if (institutionName != null && !institutionName.isBlank()) {
            Paragraph title = new Paragraph(institutionName)
                    .setFontSize(16)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            document.add(new Paragraph(" "));
        }
    }

    // Info Taable using filters
    public void createInfoTablePdfExport(Document document)
            throws IOException {

        if (searchCriteria == null || searchCriteria.isEmpty()) {
            return;
        }
        float[] colWidths = {1.5f, 2f, 0.1f, 1.5f, 2f, 0.1f, 1.5f, 2f, 0.1f, 1.5f, 2f};
        Table infoTable = new Table(colWidths).useAllAvailableWidth().setFixedLayout();
        infoTable.setMarginBottom(10);

        int pairsInRow = 0;  

        for (Map.Entry<String, Object> entry : searchCriteria.entrySet()) {

            Cell labelCell = new Cell().add(new Paragraph(entry.getKey()).setFont(PdfFontFactory.createFont(boldFont)).setFontSize(8).setTextAlignment(TextAlignment.LEFT));
            infoTable.addCell(labelCell);

            String valueText = "";
            Object value = entry.getValue();

            if (value != null) {
                valueText = value.toString();
            }

            Cell valueCell = new Cell().add(new Paragraph(valueText).setTextAlignment(TextAlignment.LEFT).setFontSize(8));
            infoTable.addCell(valueCell);

            pairsInRow++;

            if (pairsInRow < 4) {
                Cell spacer = new Cell().add(new Paragraph(" "));
                infoTable.addCell(spacer);
            }

            if (pairsInRow == 4) {
                pairsInRow = 0;
            }
        }

        if (pairsInRow > 0) {
            int remainingPairs = 4 - pairsInRow;

            for (int i = 0; i < remainingPairs; i++) {
                Cell emptyLabel = new Cell().add(new Paragraph(" "));
                infoTable.addCell(emptyLabel);

                Cell emptyValue = new Cell().add(new Paragraph(" "));
                infoTable.addCell(emptyValue);

                if (i < remainingPairs - 1) {
                    Cell spacer = new Cell().add(new Paragraph(" "));
                    infoTable.addCell(spacer);
                }
            }
        }
        document.add(infoTable);
        return ;

    }

    public Table buildTable() throws IOException{
        float[] widths; 
        if (serialNoColumnAtStart) {
            widths = new float[columns.size() + 1];
        } else {
            widths = new float[columns.size()];
        }
        int i = 0;
        
        if (serialNoColumnAtStart) {
            widths[i++] = 2f;
        }

        for (ReportColumn<T> col : columns.values()) {
            widths[i++] = col.getColumnWidth() != null ? col.getColumnWidth() : 1f;
        }
        Table table = new Table(UnitValue.createPercentArray(widths));
        table.setWidth(UnitValue.createPercentValue(100));
        addHeaderRow(table);
        addDataRows(table);
        addFooterRow(table);
        return table;
    }

    public void addHeaderRow(Table table) throws IOException {
        if (serialNoColumnAtStart) {
            Cell cell = new Cell().add(new Paragraph("No").setFont(PdfFontFactory.createFont(boldFont)));
            cell.setFontSize(8).setTextAlignment(TextAlignment.CENTER).setBackgroundColor(new DeviceRgb(192, 192, 192));
            table.addCell(cell);
        }
        for (ReportColumn<T> column : columns.values()) {
            Cell cell = new Cell().add(new Paragraph(column.getHeader() != null ? column.getHeader() : "").setFont(PdfFontFactory.createFont(boldFont)));
            cell.setFontSize(8).setTextAlignment(TextAlignment.CENTER).setBackgroundColor(new DeviceRgb(192, 192, 192));
            table.addCell(cell);
        }
    }

    public void addDataRows(Table table) {
        int serial = 1;
        for (T row : data) {
            if (serialNoColumnAtStart) {
                Cell cell = new Cell().add(new Paragraph(String.valueOf(serial++))).setTextAlignment(TextAlignment.CENTER).setFontSize(fontSize);
                table.addCell(cell);
            }
            for (ReportColumn<T> column : columns.values()) {
                Object cellValue = column.extractData(row);
                String text = cellValue != null ? String.format(column.getFormat(), cellValue) : "";
                Cell cell = new Cell().add(new Paragraph(text)).setTextAlignment(column.getTextAlignment()).setFontSize(fontSize);
                cell.setKeepTogether(true);
                table.addCell(cell);
            }
        }
    }

    public void addFooterRow(Table table) throws IOException{
        int i = 0;
        int span;

        List<ReportColumn<T>> cols = new ArrayList<>(columns.values());

        while (i < cols.size()) {
            ReportColumn<T> column = cols.get(i);
            Object footerValue = column.getFooter();

            if (footerValue != null) {
                String text = String.format(column.getFormat(), footerValue);

                Cell cell = new Cell()
                        .add(new Paragraph(text).setFont(PdfFontFactory.createFont(boldFont)))
                        .setTextAlignment(column.getTextAlignment())
                        .setFontSize(fontSize)
                        .setBackgroundColor(new DeviceRgb(192, 192, 192));

                table.addCell(cell);
                i++;
            } 
            else {
                if (serialNoColumnAtStart) {
                    span = 1;
                } else {
                    span = 0;
                }

                while (i < cols.size() && cols.get(i).getFooter() == null) {
                    span++;
                    i++;
                }

                Cell mergedCell = new Cell(1, span)
                        .add(new Paragraph(""))
                        .setBackgroundColor(new DeviceRgb(192, 192, 192));

                table.addCell(mergedCell);
            }
        }
    }

    public void addReportFooter(Document document) {
        String userName = "";
        if (reportGeneratedBy != null && !reportGeneratedBy.trim().isEmpty()) {
            userName = reportGeneratedBy;
        }
        String printedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        document.add(new Paragraph("").setMarginTop(15));

        SolidLine footerLine = new SolidLine(0.5f);
        LineSeparator footerSeparator = new LineSeparator(footerLine);
        footerSeparator.setStrokeColor(ColorConstants.GRAY);
        document.add(footerSeparator);

        float[] columnWidths = {1, 1};
        Table footerTable = new Table(columnWidths).useAllAvailableWidth();
        footerTable.setBorder(Border.NO_BORDER);

        Cell userCell = new Cell()
                .add(new Paragraph("Printed by: " + userName).setFontSize(9).setTextAlignment(TextAlignment.LEFT))
                .setBorder(Border.NO_BORDER);
        footerTable.addCell(userCell);

        Cell timeCell = new Cell()
                .add(new Paragraph("Printed on: " + printedTime).setFontSize(9).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER);
        footerTable.addCell(timeCell);

        footerTable.setMarginTop(5);
        document.add(footerTable);
    }

    // Filter info to excel
    public int addMetaDataToExcelSheet(XSSFWorkbook wb, XSSFSheet sheet, int rowIndex, Map<String, Object> filters) {
        if (wb == null || sheet == null) {
            return rowIndex;
        }
        if (rowIndex < 0) {
            return 0;
        }
        
        CellStyle headerStyle = wb.createCellStyle();
        org.apache.poi.ss.usermodel.Font headerFont = wb.createFont();
        headerFont.setFontHeightInPoints((short) 14);
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        org.apache.poi.ss.usermodel.Font metaFontBold = wb.createFont();
        metaFontBold.setBold(true);
        CellStyle metaStyleBold = wb.createCellStyle();
        metaStyleBold.setFont(metaFontBold);
        
        int pairCounter = 0;
        Row row = sheet.createRow(rowIndex++);

        for (Map.Entry<String, Object> entry : filters.entrySet()) {

            org.apache.poi.ss.usermodel.Cell labelCell = row.createCell(pairCounter * 3);
            labelCell.setCellValue(entry.getKey());
            labelCell.setCellStyle(metaStyleBold);

            org.apache.poi.ss.usermodel.Cell valueCell = row.createCell(pairCounter * 3 + 1);
            Object value = entry.getValue();

            valueCell.setCellValue((value != null) ? value.toString() : "");

            pairCounter++;

            if (pairCounter == 3) {
                pairCounter = 0;
                row = sheet.createRow(rowIndex++);
            }
        }

        rowIndex++;

        return rowIndex;
    }

    public void addReportFooterExcel(XSSFWorkbook wb,XSSFSheet sheet, int rowIndex) {
        // Footer row: "Printed by: X" left, "Printed on: Y" right
        CellStyle leftSmallStyle = wb.createCellStyle();
        leftSmallStyle.setAlignment(HorizontalAlignment.LEFT);
        leftSmallStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        org.apache.poi.ss.usermodel.Font smallFont = wb.createFont();
        smallFont.setFontHeightInPoints((short) 9);
        leftSmallStyle.setFont(smallFont);

        String printedTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        Row footerRow = sheet.createRow(rowIndex);
        org.apache.poi.ss.usermodel.Cell printedByCell = footerRow.createCell(0);
        printedByCell.setCellValue("Printed by: " + reportGeneratedBy);
        printedByCell.setCellStyle(leftSmallStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 3));

        org.apache.poi.ss.usermodel.Cell printedOnCell = footerRow.createCell(4);
        printedOnCell.setCellValue("Printed on: " + printedTime);
        printedOnCell.setCellStyle(leftSmallStyle);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 4, 6));
    }

    public static class OnlineBookingCountReport extends Report<OnlineBookingDetialRow> {

        private static final LinkedHashMap<String, ReportColumn<OnlineBookingDetialRow>> rpCols;

        static {
            rpCols = new LinkedHashMap<>();
            rpCols.put("Bill No", new ReportColumn<>("Bill No", OnlineBookingDetialRow::getBillDeptId, TextAlignment.LEFT, "%s", 4f));

            rpCols.put("Session Date", new ReportColumn<>("Session Date",
                     row -> {
                            OnlineBookingDetialRow r = (OnlineBookingDetialRow) row;
                            return new SimpleDateFormat("dd MMM yyyy").format(r.getSessionDate());
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3f));

            rpCols.put("Consultant", new ReportColumn<>("Consultant", OnlineBookingDetialRow::getConsultantName, TextAlignment.LEFT, "%s", 5f));
            rpCols.put("Speciality", new ReportColumn<>("Speciality", OnlineBookingDetialRow::getConsultantSpeciality, TextAlignment.LEFT, "%s", 4f));
            rpCols.put("Session Name", new ReportColumn<>("Session Name", OnlineBookingDetialRow::getSessionName, TextAlignment.LEFT, "%s", 4f));
            rpCols.put("Patient Name", new ReportColumn<>("Patient Name", OnlineBookingDetialRow::getPatientName, TextAlignment.LEFT, "%s", 5f, "Total Amount"));
            rpCols.put("Amount", new ReportColumn<>("Amount", OnlineBookingDetialRow::getPaidAmount, TextAlignment.RIGHT, "%,.2f", 4f));
            rpCols.put("Agent", new ReportColumn<>("Agent", OnlineBookingDetialRow::getAgentName, TextAlignment.LEFT, "%s", 4f));
            rpCols.put("Phone Number", new ReportColumn<>("Phone Number", OnlineBookingDetialRow::getPatientPhone, TextAlignment.LEFT, "%s", 4f));
            rpCols.put("State", new ReportColumn<>("State", 
                    row -> {
                            OnlineBookingDetialRow r = (OnlineBookingDetialRow) row;
                            if (r.isBillCancelled()) return "Cancelled Bill";
                            else if (r.isBillRefunded()) return "Refunded Bill";
                            else if (r.getOnlineBookingStatus() == OnlineBookingStatus.COMPLETED) return "Completed Bill";
                            else if (!r.isBillCancelled() && !r.isBillRefunded() && r.isAbsent()) return "Absent";
                            return "";
                    }, 
                    TextAlignment.CENTER, 
                    "%s", 
                    3f));
        }

        public OnlineBookingCountReport(String fileName, String institutionName, Map<String, Object> searchCriteria, List<OnlineBookingDetialRow> data, String reportGeneratedBy) {
            super(rpCols);
            this.setSerialNoColumnAtStart(true);
            this.setReportName("Online Session Bookings");
            this.setFileName(fileName);
            this.setInstitutionName(institutionName);
            this.setSearchCriteria(searchCriteria);
            this.setData(data);
            this.setReportGeneratedBy(reportGeneratedBy);
            
        }

        @Override
        public void addFooterRow(Table table) throws IOException {
            if (this.isSerialNoColumnAtStart()) {
                table.addCell(new Cell(1, 6).add(new Paragraph("")).setBackgroundColor(new DeviceRgb(192, 192, 192)));
            } else {
                table.addCell(new Cell(1, 5).add(new Paragraph("")).setBackgroundColor(new DeviceRgb(192, 192, 192)));
            }
            Object footerValue;
            
            ReportColumn<OnlineBookingDetialRow> col1 = getColumns().get("Patient Name");
            footerValue = col1.getFooter();
            table.addCell(new Cell().add(new Paragraph((footerValue != null ? String.format(col1.getFormat(), footerValue) : ""))).setFont(PdfFontFactory.createFont(getBoldFont())).setTextAlignment(col1.getTextAlignment()).setFontSize(getFontSize()).setBackgroundColor(new DeviceRgb(192, 192, 192)));

            ReportColumn<OnlineBookingDetialRow> col2 = getColumns().get("Amount");
            footerValue = col2.getFooter();
            table.addCell(new Cell().add(new Paragraph((footerValue != null ? String.format(col2.getFormat(), footerValue) : "")).setFont(PdfFontFactory.createFont(getBoldFont()))).setTextAlignment(col2.getTextAlignment()).setFontSize(getFontSize()).setBackgroundColor(new DeviceRgb(192, 192, 192)));

            table.addCell(new Cell(1, 3).add(new Paragraph("")).setBackgroundColor(new DeviceRgb(192, 192, 192)));
        }
 
        
    }

    public static class ChannelBillSearch extends Report<Bill> {

        private static final LinkedHashMap<String, ReportColumn<Bill>> rpCols;

        static {
            rpCols = new LinkedHashMap<>();
            rpCols.put("Bill No", new ReportColumn<>("Bill No",
                    row -> {
                            Bill r = (Bill) row;
                            String billDept = r.getDeptId() != null ? r.getDeptId() : "";
                            if (r.isCancelled()) {
                                billDept += "\nCancelled" + (r.getCancelledBill() != null ? (" - " + r.getCancelledBill().getDeptId()) : "");
                            }
                            if (r.isRefunded()) {
                                billDept += "\nRefunded" + (r.getRefundedBill() != null ? (" - " + r.getRefundedBill().getDeptId()) : "" );
                            }
                            if (r instanceof RefundBill) {
                                billDept += "\nRefund Bill";
                            }
                            if (r.getBillTypeAtomic() != null && r.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT) {
                                billDept += "\nCancel Bill";
                            }
                            return billDept;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    4f));

            rpCols.put("Consultant", new ReportColumn<>("Consultant",
                    row -> {
                            Bill r = (Bill) row;
                            return (r.getSingleBillSession() != null && r.getSingleBillSession().getSessionInstance() != null && r.getSingleBillSession().getSessionInstance().getStaff() != null &&  r.getSingleBillSession().getSessionInstance().getStaff().getPerson() != null) ?
                                r.getSingleBillSession().getSessionInstance().getStaff().getPerson().getNameWithTitle() : "" ;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    4f));

            rpCols.put("Billed At", new ReportColumn<>("Billed At",
                    row -> {
                            Bill r = (Bill) row;
                            String date = (r.getCreatedAt() != null) ? new SimpleDateFormat("dd MMM yyyy hh:mm a").format(r.getCreatedAt()) : "";

                            if (r.isCancelled() && r.getCancelledBill() != null) {
                                date += "\n(Cancelled: " + (r.getCancelledBill().getCreatedAt() != null ? new SimpleDateFormat("dd MMM yyyy hh:mm a").format(r.getCancelledBill().getCreatedAt()) : "") + ")";
                            }
                            if (r.isRefunded() && r.getRefundedBill() != null) {
                                date += "\n(Refunded: " + (r.getRefundedBill().getCreatedAt() != null ? new SimpleDateFormat("dd MMM yyyy hh:mm a").format(r.getRefundedBill().getCreatedAt()) : "") + ")";
                            }

                            return date;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3f));

            rpCols.put("Session", new ReportColumn<>("Session",
                    row -> {
                            Bill r = (Bill) row;
                            return (r.getSingleBillSession() != null && r.getSingleBillSession().getSessionInstance() != null && r.getSingleBillSession().getSessionInstance().getOriginatingSession() != null) ?
                                r.getSingleBillSession().getSessionInstance().getOriginatingSession() .getName() : "" ;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    4f));

            rpCols.put("Billed For", new ReportColumn<>("Billed For",
                    row -> {
                            Bill r = (Bill) row;
                            return (r.getToDepartment() != null) ? r.getToDepartment().getName() : "";
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3f));

            rpCols.put("Billed By", new ReportColumn<>("Billed By",
                    row -> {
                            Bill r = (Bill) row;
                            String billedBy = r.getCreater() != null && r.getCreater().getWebUserPerson() != null ? r.getCreater().getWebUserPerson().getName() : "";
                            if (r.getCreditCompany() != null && (r.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT || r.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT_ONLINE_BOOKING)) {
                                billedBy += "\n" + r.getCreditCompany().getName();
                            }
                            if (r.isCancelled() && r.getCancelledBill() != null) {
                                billedBy += "\n(Cancelled: " +  (r.getCancelledBill().getCreater() != null && r.getCancelledBill().getCreater().getWebUserPerson() != null ? r.getCancelledBill().getCreater().getWebUserPerson().getName() : "" ) + ")";
                            }
                            if (r.isRefunded() && r.getRefundedBill() != null) {
                                billedBy += "\n(Refunded: " +  (r.getRefundedBill().getCreater() != null && r.getRefundedBill().getCreater().getWebUserPerson() != null ? r.getRefundedBill().getCreater().getWebUserPerson().getName() : "" ) + ")";
                            }
                            return billedBy;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3f));

            rpCols.put("PaymentMethod", new ReportColumn<>("PaymentMethod",
                    row -> {
                            Bill r = (Bill) row;
                            return (r.getPaymentMethod() != null) ? r.getPaymentMethod().getLabel() : "";
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3f));

            
            rpCols.put("Serial Number", new ReportColumn<>("Serial Number",
                    row -> {
                            Bill r = (Bill) row;
                            return (r.getSingleBillSession() != null) ? r.getSingleBillSession().getSerialNo() : "";
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3f));

            rpCols.put("Client", new ReportColumn<>("Client",
                    row -> {
                            Bill r = (Bill) row;
                            String client = "";

                            if (r.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT) {
                                client += r.getReferenceBill() != null && r.getReferenceBill().getOnlineBooking() != null ? r.getReferenceBill().getOnlineBooking().getPatientName() : "";
                            } else if (r.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT_ONLINE_BOOKING) {
                                client += r.getBilledBill() != null && r.getBilledBill().getReferenceBill() != null && r.getBilledBill().getReferenceBill().getOnlineBooking() != null ? r.getBilledBill().getReferenceBill().getOnlineBooking().getPatientName() : "";
                            } else {
                                client += r.getPatient() != null && r.getPatient().getPerson() != null ? r.getPatient().getPerson().getName() : "";
                            }
                            return client;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3f));

            rpCols.put("Remarks", new ReportColumn<>("Remarks",
                    row -> {
                            Bill r = (Bill) row;
                            String remarks = "";
                            if (r.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT) {
                                remarks += "OB";
                            } else if (r.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT_ONLINE_BOOKING) {
                                remarks += "OB Cancel";
                            } else {
                                remarks += "System";
                            }

                            return remarks;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3f));

            rpCols.put("Gross Value", new ReportColumn<>("Gross Value", Bill::getTotal, TextAlignment.LEFT, "%s", 4f));
            rpCols.put("Discount", new ReportColumn<>("Discount", Bill::getDiscount, TextAlignment.LEFT, "%s", 4f));
            rpCols.put("Net Value", new ReportColumn<>("Net Value", Bill::getNetTotal, TextAlignment.LEFT, "%s", 4f));
        }

        public ChannelBillSearch(String fileName, String institutionName, Map<String, Object> searchCriteria, List<Bill> data, String reportGeneratedBy) {
            super(rpCols);
            this.setReportName("Channel Bills");
            this.setFileName(fileName);
            this.setInstitutionName(institutionName);
            this.setSearchCriteria(searchCriteria);
            this.setData(data);
            this.setReportGeneratedBy(reportGeneratedBy);
      
        }
        
    }

}
