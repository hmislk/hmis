package com.divudi.core.data.reports;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
import com.divudi.bean.channel.ChannelReportController;
import com.divudi.bean.channel.ChannelReportTemplateController.OnlineBookingDetialRow;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.OnlineBookingStatus;
import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.dto.channel.ChannelAbsentPatientsDTO;
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
    private HashMap<String, Object> footers;
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
        this.footers = new HashMap<>();
    }

    public Report(LinkedHashMap<String, ReportColumn<T>> columns) {
        this.columns = columns;
        this.footers = new HashMap<>();
    }

    public Report(LinkedHashMap<String, ReportColumn<T>> columns, String reportName, String fileName, String institutionName, Map<String, Object> searchCriteria, List<T> data, String reportGeneratedBy) {
        this.columns = columns;
        this.reportName = reportName;
        this.fileName = fileName;
        this.institutionName = institutionName;
        this.searchCriteria = searchCriteria;
        this.data = data;
        this.reportGeneratedBy = reportGeneratedBy;
        this.footers = new HashMap<>();
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

    public HashMap<String, Object> getFooters() {
        return footers;
    }

    public void setFooters(HashMap<String, Object> footers) {
        this.footers = footers;
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
        if (columns.containsKey(columnKey)) {
            footers.put(columnKey, data);
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
        if (footers != null && !footers.isEmpty()) {
            Row footerRow = dataSheet.createRow(currentRow++);
            int footerCol = 0;
            if (serialNoColumnAtStart) {
                footerRow.createCell(footerCol++).setCellValue("");
            }
            for (String column : columns.keySet()) {
                Object footerValue = footers.get(column);
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
                if (value instanceof Date) {
                    valueText = new SimpleDateFormat("dd MMM yyyy hh:mm a").format(value);
                } else {
                    valueText = value.toString();
                }   
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

        List<String> cols = new ArrayList<>(columns.keySet());

        if (footers == null || footers.isEmpty()) {
            return;
        }

        while (i < cols.size()) {
            String column = cols.get(i);
            if (footers.containsKey(column)) {
                Object footerValue = footers.get(column);

                if (footerValue != null) {
                    String text = String.format(columns.get(column).getFormat(), footerValue);

                    Cell cell = new Cell()
                            .add(new Paragraph(text).setFont(PdfFontFactory.createFont(boldFont)))
                            .setTextAlignment(columns.get(column).getTextAlignment())
                            .setFontSize(fontSize)
                            .setBackgroundColor(new DeviceRgb(192, 192, 192));

                    table.addCell(cell);
                    i++;
                } 
            } else {
                if (serialNoColumnAtStart) {
                    span = 1;
                } else {
                    span = 0;
                }

                while (i < cols.size() && !footers.containsKey(cols.get(i))) {
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

            if (value != null) {
                if (value instanceof Date) {
                    valueCell.setCellValue( new SimpleDateFormat("dd MMM yyyy hh:mm a").format(value));
                } else {
                    valueCell.setCellValue(value.toString());
                }
            } else {
                valueCell.setCellValue( "");
            }

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
            rpCols.put("Patient Name", new ReportColumn<>("Patient Name", OnlineBookingDetialRow::getPatientName, TextAlignment.LEFT, "%s", 5f));
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
            
            this.setColumnFooter("Total Amount", "Patient Name");
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
            footerValue = this.getFooters().get("Patient Name");
            table.addCell(new Cell().add(new Paragraph((footerValue != null ? String.format(col1.getFormat(), footerValue) : ""))).setFont(PdfFontFactory.createFont(getBoldFont())).setTextAlignment(col1.getTextAlignment()).setFontSize(getFontSize()).setBackgroundColor(new DeviceRgb(192, 192, 192)));

            ReportColumn<OnlineBookingDetialRow> col2 = getColumns().get("Amount");
            footerValue = this.getFooters().get("Amount");
            table.addCell(new Cell().add(new Paragraph((footerValue != null ? String.format(col2.getFormat(), footerValue) : "")).setFont(PdfFontFactory.createFont(getBoldFont()))).setTextAlignment(col2.getTextAlignment()).setFontSize(getFontSize()).setBackgroundColor(new DeviceRgb(192, 192, 192)));

            table.addCell(new Cell(1, 3).add(new Paragraph("")).setBackgroundColor(new DeviceRgb(192, 192, 192)));
        }
 
        
    }

    public static class ChannelPatientAbsentReport extends Report<ChannelAbsentPatientsDTO> {

        private static final LinkedHashMap<String, ReportColumn<ChannelAbsentPatientsDTO>> rpCols;

        static {
            rpCols = new LinkedHashMap<>();
            rpCols.put("Channel Receipt No", new ReportColumn<>("Channel Receipt No", ChannelAbsentPatientsDTO::getChannelReceiptNumber, TextAlignment.LEFT, "%s", 4f));
            rpCols.put("Session Name", new ReportColumn<>("Session Name", ChannelAbsentPatientsDTO::getServiceSessionName, TextAlignment.LEFT, "%s", 4f));
            rpCols.put("Booking Type", new ReportColumn<>("Booking Type", ChannelAbsentPatientsDTO::getBookingType, TextAlignment.LEFT, "%s", 2.5f));
            rpCols.put("Appointment No", new ReportColumn<>("Appointment No", ChannelAbsentPatientsDTO::getBillSessionSerialNo, TextAlignment.LEFT, "%s", 2.5f));
            rpCols.put("Patient Name", new ReportColumn<>("Patient Name", ChannelAbsentPatientsDTO::getPatientName, TextAlignment.LEFT, "%s", 4f));
            rpCols.put("Cashier Name", new ReportColumn<>("Cashier Name", ChannelAbsentPatientsDTO::getCashierName, TextAlignment.LEFT, "%s", 3f));

            rpCols.put("Payment Method", new ReportColumn<>("Payment Method",
                     row -> {
                            ChannelAbsentPatientsDTO r = (ChannelAbsentPatientsDTO) row;
                            return r.getPaymentMethod() != null ? r.getPaymentMethod().getLabel() : "";
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3f));

            rpCols.put("Doctor Name", new ReportColumn<>("Doctor Name", ChannelAbsentPatientsDTO::getDoctorName, TextAlignment.LEFT, "%s", 4f));
            rpCols.put("Doctor Fee", new ReportColumn<>("Doctor Fee", ChannelAbsentPatientsDTO::getStaffFee, TextAlignment.RIGHT, "%,.2f", 3.5f));
            rpCols.put("Hospital Fee", new ReportColumn<>("Hospital Fee", ChannelAbsentPatientsDTO::getHospitalFee, TextAlignment.RIGHT, "%,.2f", 3.5f));
            rpCols.put("Net Total", new ReportColumn<>("Net Total", ChannelAbsentPatientsDTO::getNetTotal, TextAlignment.RIGHT, "%,.2f", 3.5f));
        }

        public ChannelPatientAbsentReport(String fileName, String institutionName, Map<String, Object> searchCriteria, List<ChannelAbsentPatientsDTO> data, String reportGeneratedBy) {
            super(rpCols);
            this.setSerialNoColumnAtStart(false);
            this.setReportName("Patient Absent Report");
            this.setFileName(fileName);
            this.setInstitutionName(institutionName);
            this.setSearchCriteria(searchCriteria);
            this.setData(data);
            this.setReportGeneratedBy(reportGeneratedBy); 
            
            //footers
            this.setColumnFooter("Total", "Doctor Name");
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

                            if (r.isCancelled() && r.getCancelledBill() != null && r.getCancelledBill().getCreatedAt() != null) {
                                date += "\n(Cancelled: " + new SimpleDateFormat("dd MMM yyyy hh:mm a").format(r.getCancelledBill().getCreatedAt()) + ")";
                            }
                            if (r.isRefunded() && r.getRefundedBill() != null && r.getRefundedBill().getCreatedAt() != null) {
                                date += "\n(Refunded: " + new SimpleDateFormat("dd MMM yyyy hh:mm a").format(r.getRefundedBill().getCreatedAt()) + ")";
                            }

                            return date;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3f));

            rpCols.put("Appointment Date", new ReportColumn<>("Appointment Date",
                    row -> {
                            Bill r = (Bill) row;
                            String date = (r.getSingleBillSession() != null && r.getSingleBillSession().getSessionInstance() != null && r.getSingleBillSession().getSessionInstance().getSessionDate() != null) ? new SimpleDateFormat("dd MMM yyyy").format(r.getSingleBillSession().getSessionInstance().getSessionDate()) : "";

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
                            if (r.isCancelled() && r.getCancelledBill() != null && r.getCancelledBill().getCreater() != null && r.getCancelledBill().getCreater().getWebUserPerson() != null) {
                                billedBy += "\n(Cancelled: " +  r.getCancelledBill().getCreater().getWebUserPerson().getName() + ")";
                            }
                            if (r.isRefunded() && r.getRefundedBill() != null && r.getRefundedBill().getCreater() != null && r.getRefundedBill().getCreater().getWebUserPerson() != null) {
                                billedBy += "\n(Refunded: " +  r.getRefundedBill().getCreater().getWebUserPerson().getName() + ")";
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

    public static class OpdBillSearch extends Report<Bill> {

        private static final LinkedHashMap<String, ReportColumn<Bill>> rpCols;

        static {
            rpCols = new LinkedHashMap<>();
            rpCols.put("Bill No", new ReportColumn<>("Bill No",
                    row -> {
                            Bill r = (Bill) row;
                            String billDept = r.getDeptId() != null ? r.getDeptId() : "";
                            if (r.isCancelled()) {
                                billDept += "\nCancelled" + (r.getCancelledBill() != null ? (": " + r.getCancelledBill().getDeptId()) : "");
                            }
                            if (r.isRefunded()) {
                                billDept += "\nRefunded" + (r.getRefundedBill() != null ? (": " + r.getRefundedBill().getDeptId()) : "" );
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

            rpCols.put("Batch Bill", new ReportColumn<>("Batch Bill",
                    row -> {
                            Bill r = (Bill) row;
                            return (r.getBackwardReferenceBill() != null) ?
                               r.getBackwardReferenceBill().getDeptId() : "" ;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    4f));

            rpCols.put("Billed At", new ReportColumn<>("Billed At",
                    row -> {
                            Bill r = (Bill) row;
                            String date = (r.getCreatedAt() != null) ? new SimpleDateFormat("dd MMM yyyy hh:mm a").format(r.getCreatedAt()) : "";

                            if (r.isCancelled() && r.getCancelledBill() != null && r.getCancelledBill().getCreatedAt() != null) {
                                date += "\n(Cancelled: " + new SimpleDateFormat("dd MMM yyyy hh:mm a").format(r.getCancelledBill().getCreatedAt()) + ")";
                            }
                            if (r.isRefunded() && r.getRefundedBill() != null && r.getRefundedBill().getCreatedAt() != null ) {
                                date += "\n(Refunded: " + new SimpleDateFormat("dd MMM yyyy hh:mm a").format(r.getRefundedBill().getCreatedAt()) + ")";
                            }

                            return date;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3f));

            rpCols.put("Billed By (Department)", new ReportColumn<>("Billed By (Department)",
                    row -> {
                            Bill r = (Bill) row;
                            String dept = r.getFromDepartment() != null ? r.getFromDepartment().getName() : "";
                            
                            if (r.isCancelled() && r.getCancelledBill() != null && r.getCancelledBill().getDepartment() != null) {
                                dept += "\n(Cancelled: " + r.getCancelledBill().getDepartment().getName() + ")";
                            }
                            if (r.isRefunded() && r.getRefundedBill() != null && r.getRefundedBill().getDepartment() != null) {
                                dept += "\n(Refunded: " + r.getRefundedBill().getDepartment().getName() + ")";
                            }
                            return dept;
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

            rpCols.put("Billed By (Cashier)", new ReportColumn<>("Billed By (Cashier)",
                    row -> {
                            Bill r = (Bill) row;
                            String billedBy = r.getCreater() != null && r.getCreater().getWebUserPerson() != null ? r.getCreater().getWebUserPerson().getName() : "";
                            if (r.isCancelled() && r.getCancelledBill() != null && r.getCancelledBill().getCreater() != null && r.getCancelledBill().getCreater().getWebUserPerson() != null) {
                                billedBy += "\n(Cancelled: " + r.getCancelledBill().getCreater().getWebUserPerson().getName() + ")";
                            }
                            if (r.isRefunded() && r.getRefundedBill() != null && r.getRefundedBill().getCreater() != null && r.getRefundedBill().getCreater().getWebUserPerson() != null) {
                                billedBy += "\n(Refunded: " +  r.getRefundedBill().getCreater().getWebUserPerson().getName() + ")";
                            }
                            return billedBy;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3f));

            rpCols.put("Client", new ReportColumn<>("Client",
                    row -> {
                            Bill r = (Bill) row;
                            return (r.getPatient() != null && r.getPatient().getPerson() != null) ? r.getPatient().getPerson().getName() : "";
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3f));

            rpCols.put("Remarks", new ReportColumn<>("Remarks",
                    row -> {
                            Bill r = (Bill) row;
                            String remarks = "";
                            if (r.getMembershipScheme() != null) {
                                remarks += r.getMembershipScheme().getName();
                            }
                            if (r.isCancelled()) {
                                remarks += "\nCancelled";
                            }
                            if (r.getBillTypeAtomic() == BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT) {
                                remarks += "\nPackage Bill";
                            }
                            if(r.isRefunded()) {
                                remarks += "\nRefunded";
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

        public OpdBillSearch(String fileName, String institutionName, Map<String, Object> searchCriteria, List<Bill> data, String reportGeneratedBy) {
            super(rpCols);
            this.setReportName("OPD Bills");
            this.setFileName(fileName);
            this.setInstitutionName(institutionName);
            this.setSearchCriteria(searchCriteria);
            this.setData(data);
            this.setReportGeneratedBy(reportGeneratedBy);
      
        }
        
    }

    public static class ChannelIncomeReport extends Report<ReportTemplateRow> {
        private static final LinkedHashMap<String, ReportColumn<ReportTemplateRow>> rpCols;

        static {
            rpCols = new LinkedHashMap<>();
            rpCols.put("Billed Date", new ReportColumn<>("Billed Date",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            Bill b = r.getBill();
                            if (b == null) {
                                return "";
                            }
                            String date = (b.getCreatedAt() != null) ? new SimpleDateFormat("dd MMM yyyy hh:mm a").format(b.getCreatedAt()) : "";

                            if (b.isCancelled() && b.getCancelledBill() != null && b.getCancelledBill().getCreatedAt() != null) {
                                date += "\n(Cancelled: " + new SimpleDateFormat("dd MMM yyyy hh:mm a").format(b.getCancelledBill().getCreatedAt()) + ")";
                            }
                            if (b.isRefunded() && b.getRefundedBill() != null && b.getRefundedBill().getCreatedAt() != null ) {
                                date += "\n(Refunded: " + new SimpleDateFormat("dd MMM yyyy hh:mm a").format(b.getRefundedBill().getCreatedAt()) + ")";
                            }

                            return date;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3.5f));

            rpCols.put("Appointment Date", new ReportColumn<>("Appointment Date",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            Bill b = r.getBill();
                            if (b == null) {
                                return "";
                            }
                            String date = (b.getSingleBillSession() != null && b.getSingleBillSession().getSessionInstance() != null && b.getSingleBillSession().getSessionInstance().getSessionDate() != null) ? new SimpleDateFormat("dd MMM yyyy").format(b.getSingleBillSession().getSessionInstance().getSessionDate()) : "";

                            return date;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3f));

            rpCols.put("Bill No", new ReportColumn<>("Bill No",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            Bill b = r.getBill();
                            if (b == null) {
                                return "";
                            }
                            String billDept = b.getDeptId() != null ? b.getDeptId() : "";
                            if (b.isCancelled()) {
                                billDept += "\nCancelled" + (b.getCancelledBill() != null ? (": " + b.getCancelledBill().getDeptId()) : "");
                            }
                            if (b.isRefunded()) {
                                billDept += "\nRefunded" + (b.getRefundedBill() != null ? (": " + b.getRefundedBill().getDeptId()) : "" );
                            }
                            if (b instanceof RefundBill) {
                                billDept += "\nRefund Bill";
                            }
                            if (b.getBillTypeAtomic() != null && b.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT) {
                                billDept += "\nCancel Bill";
                            }
                            return billDept;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    5f));

            rpCols.put("Bill Type", new ReportColumn<>("Bill Type",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            Bill b = r.getBill();
                            if (b == null || b.getBillTypeAtomic() == null) {
                                return "";
                            }
                            String billType = "";
                            if (b.getBillTypeAtomic() == BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_SESSION) {
                                billType = "Dr Payment";
                            } else if (b.getBillTypeAtomic() == BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_CHANNELING_SERVICE_RETURN) {
                                billType = "Dr Payment Return";
                            } else if (b.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_AGENT_PAID_TO_HOSPITAL_FOR_ONLINE_BOOKINGS_BILL) {
                                billType = "OB Agent Payment To Hospital";
                            } else if (b.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_AGENT_PAID_TO_HOSPITAL_FOR_ONLINE_BOOKINGS_BILL_CANCELLATION) {
                                billType = "OB Agent Cancel Payment To Hospital";
                            } else {
                                billType = "Channel Bill";
                            }
                            return billType;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    5f));

            rpCols.put("Patient", new ReportColumn<>("Patient",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            Bill b = r.getBill();
                            if (b == null || b.getBillTypeAtomic() == null) {
                                return "";
                            }
                            String patientName = "";
                            if (b.getBillTypeAtomic() != null) {
                                if (b.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT) {
                                    patientName = (b.getReferenceBill() != null && b.getReferenceBill().getOnlineBooking() != null) ? b.getReferenceBill().getOnlineBooking().getPatientName() : "";
                                } else if (b.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT_ONLINE_BOOKING) {
                                patientName = (b.getBilledBill() != null && b.getBilledBill().getReferenceBill() != null && b.getBilledBill().getReferenceBill().getOnlineBooking() != null) ? b.getBilledBill().getReferenceBill().getOnlineBooking().getPatientName() : "";
                                } else {
                                    patientName = (b.getPatient() != null && b.getPatient().getPerson() != null) ? b.getPatient().getPerson().getNameWithTitle() : "";
                                }
                            }
                            return patientName;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    5f));

            rpCols.put("Cashier", new ReportColumn<>("Cashier",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            Bill b = r.getBill();
                            if (b == null) {
                                return "";
                            }
                            return (b.getCreater() != null && b.getCreater().getName() != null ? b.getCreater().getName() : "");
                    },
                    TextAlignment.LEFT,
                    "%s",
                    3.5f));

            

            rpCols.put("Payment Method", new ReportColumn<>("Payment Method",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            Bill b = r.getBill();
                            if (b == null) {
                                return "";
                            }
                            String pM = "";
                            if (b.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT || b.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT_ONLINE_BOOKING) {
                                pM = "WEB";
                            } else {
                                pM = b.getPaymentMethod().getLabel();
                            }
                            if (b.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT && b.getReferenceBill() != null && b.getReferenceBill().getOnlineBooking() != null && b.getReferenceBill().getOnlineBooking().getAgency() != null) {
                                pM += "\n" + b.getReferenceBill().getOnlineBooking().getAgency().getName();
                            } else if (b.getBillTypeAtomic() == BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT_ONLINE_BOOKING && b.getBilledBill() != null && b.getBilledBill().getReferenceBill() != null && b.getBilledBill().getReferenceBill().getOnlineBooking() != null && b.getBilledBill().getReferenceBill().getOnlineBooking().getAgency() != null) {
                                pM += "\n" + b.getBilledBill().getReferenceBill().getOnlineBooking().getAgency().getName();
                            } else if (b.getBillType() == BillType.ChannelAgent && b.getBillTypeAtomic() != BillTypeAtomic.CHANNEL_BOOKING_FOR_PAYMENT_ONLINE_COMPLETED_PAYMENT && b.getBillTypeAtomic() != BillTypeAtomic.CHANNEL_CANCELLATION_WITH_PAYMENT_ONLINE_BOOKING && b.getCreditCompany() != null) {
                                pM += "\n" + (b.getCreditCompany().getName());
                            }
                            return pM ;
                    },
                    TextAlignment.LEFT,
                    "%s",
                    4f));

            rpCols.put("Hospital Fee", new ReportColumn<>("Hospital Fee",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            Bill b = r.getBill();
                            if (b == null) {
                                return "";
                            }
                            return b.getHospitalFee();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));
            rpCols.put("Staff Fee", new ReportColumn<>("Staff Fee",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            Bill b = r.getBill();
                            if (b == null) {
                                return "";
                            }
                            return b.getStaffFee();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));
            rpCols.put("Gross Total", new ReportColumn<>("Gross Total",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            Bill b = r.getBill();
                            if (b == null) {
                                return "";
                            }
                            return b.getTotal();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));
        }

        public ChannelIncomeReport(String fileName, String institutionName, Map<String, Object> searchCriteria, List<ReportTemplateRow> data, boolean addNetTotalColumns, ChannelReportController.PaymentMethodFlags f, String reportGeneratedBy) {
            super(rpCols);
            if (addNetTotalColumns) {
                LinkedHashMap<String, ReportColumn<ReportTemplateRow>> columns = this.getColumns();
                columns.put("Discount", new ReportColumn<>("Discount",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            Bill b = r.getBill();
                            if (b == null) {
                                return "";
                            }
                            return b.getDiscount();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));

                columns.put("Net Total", new ReportColumn<>("netTotal",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            Bill b = r.getBill();
                            if (b == null) {
                                return "";
                            }
                            return b.getNetTotal();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));
            }
            setPaymentMethodColumns(f);
            this.setSerialNoColumnAtStart(true);
            this.setReportName("Channel Income Repoort");
            this.setFileName(fileName);
            this.setInstitutionName(institutionName);
            this.setSearchCriteria(searchCriteria);
            this.setData(data);
            this.setReportGeneratedBy(reportGeneratedBy);
      
        }

        public void setPaymentMethodColumns(ChannelReportController.PaymentMethodFlags f) {
            LinkedHashMap<String, ReportColumn<ReportTemplateRow>> payCols = this.getColumns();

            if (f.hasCash) { 
                payCols.put("Cash", new ReportColumn<>("Cash",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            return r.getCashValue();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));}

            if (f.hasCard) { 
                payCols.put("Card", new ReportColumn<>("Card",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            return r.getCardValue();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));}

            if (f.hasCredit) { 
                payCols.put("Credit", new ReportColumn<>("Credit",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            return r.getCreditValue();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));}

            if (f.hasStaffWelfare) { 
                payCols.put("Staff Welfare", new ReportColumn<>("Staff Welfare",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            return r.getStaffWelfareValue();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));}

            if (f.hasVoucher) { 
                payCols.put("Voucher", new ReportColumn<>("Voucher",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            return r.getVoucherValue();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));}
            
            if (f.hasIou) { 
                payCols.put("IOU", new ReportColumn<>("IOU",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            return r.getIouValue();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));}

            if (f.hasAgent) { 
                payCols.put("Agent", new ReportColumn<>("Agent",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            return r.getAgentValue();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));}

            if (f.hasCheque) { 
                payCols.put("Cheque", new ReportColumn<>("Cheque",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            return r.getChequeValue();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));}

            if (f.hasSlip) { 
                payCols.put("Slip", new ReportColumn<>("Slip",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            return r.getSlipValue();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));}

            if (f.hasEWallet) { 
                payCols.put("eWallet", new ReportColumn<>("eWallet",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            return r.getEwalletValue();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));}

            if (f.hasPatientDeposit) { 
                payCols.put("Patient Deposits", new ReportColumn<>("Patient Deposits",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            return r.getPatientDepositValue();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));}

            if (f.hasPatientPoints) { 
                payCols.put("Patient Points", new ReportColumn<>("Patient Points",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            return r.getPatientPointsValue();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));}

            if (f.hasOnlineSettlement) { 
                payCols.put("Online Settlement", new ReportColumn<>("Online Settlement",
                    row -> {
                            ReportTemplateRow r = (ReportTemplateRow) row;
                            return r.getOnlineSettlementValue();
                    },
                    TextAlignment.RIGHT,
                    "%,.2f",
                    3.5f));}
        }
    }

}
