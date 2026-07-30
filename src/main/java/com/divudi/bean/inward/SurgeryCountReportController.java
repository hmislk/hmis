package com.divudi.bean.inward;

import com.divudi.core.data.BillType;
import com.divudi.core.data.dto.SurgeryCountTypeWiseDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.inward.SurgeryType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;
import software.xdev.chartjs.model.charts.BarChart;
import software.xdev.chartjs.model.charts.LineChart;
import software.xdev.chartjs.model.color.RGBAColor;
import software.xdev.chartjs.model.data.BarData;
import software.xdev.chartjs.model.data.LineData;
import software.xdev.chartjs.model.dataset.BarDataset;
import software.xdev.chartjs.model.dataset.LineDataset;
import software.xdev.chartjs.model.options.BarOptions;
import software.xdev.chartjs.model.options.Legend;
import software.xdev.chartjs.model.options.LineOptions;
import software.xdev.chartjs.model.options.Plugins;
import software.xdev.chartjs.model.options.Title;
import software.xdev.chartjs.model.options.elements.Fill;
import software.xdev.chartjs.model.options.scale.Scales;
import software.xdev.chartjs.model.options.scale.cartesian.linear.LinearScaleOptions;
import software.xdev.chartjs.model.options.scale.cartesian.linear.LinearTickOptions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Base64;

// POI (Excel) — Font intentionally NOT imported; fully-qualified in createBoldStyle()
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

// iText (PDF)
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Named
@SessionScoped
public class SurgeryCountReportController implements Serializable {

    @EJB
    BillFacade billFacade;

    private Institution institution;
    private Institution site;
    private Department department;

    private Date fromYearStartDate;
    private Date toYearEndDate;

    private int selectedYear;

    private SurgeryType surgeryType;
    private Item surgeryItem;

    private String surgeryCountChartType;
    private String surgeryCountBarChartModel;
    private String surgeryCountLineChartModel;

    private List<SurgeryCountTypeWiseDTO> surgeryCountTypeList;
    private List<String> surgeryCategoryNames;
    private Map<String, Integer> totalCategoryCounts;
    private int totalAllSurgeryCount;

    private String barChartImageBase64;
    private String lineChartImageBase64;

    private StreamedContent downloadedFile;

    public void processSurgeryCountTypeReport() {
        resetState();

        if (!isDateRangeValid()) {
            return;
        }

        Map<String, Object> params = new HashMap<>();
        String jpql = buildSurgeryCountJpql(params);

        List<Object[]> results = billFacade.findObjectArrayByJpql(
                jpql, params, TemporalType.TIMESTAMP);

        if (results == null || results.isEmpty()) {
            JsfUtil.addErrorMessage("No surgery records found for the selected period.");
            return;
        }

        aggregateResults(results);

        if (surgeryCountChartType != null && !surgeryCountChartType.isEmpty()) {
            createSurgeryCountChartModels();
        }
    }

    private void resetState() {
        surgeryCountTypeList = new ArrayList<>();
        surgeryCategoryNames = new ArrayList<>();
        totalCategoryCounts = new HashMap<>();
        totalAllSurgeryCount = 0;
    }

    private boolean isDateRangeValid() {
        if (fromYearStartDate == null || toYearEndDate == null) {
            JsfUtil.addErrorMessage("Please select both From and To dates.");
            return false;
        }
        if (fromYearStartDate.after(toYearEndDate)) {
            JsfUtil.addErrorMessage("From Date must not be after To Date.");
            return false;
        }

        Calendar from = Calendar.getInstance();
        from.setTime(fromYearStartDate);
        Calendar to = Calendar.getInstance();
        to.setTime(toYearEndDate);

        if (from.get(Calendar.YEAR) != to.get(Calendar.YEAR)) {
            JsfUtil.addErrorMessage(
                    "Please select a date range within a single calendar year. "
                    + "Monthly totals are grouped by month only, so a "
                    + "multi-year range would merge counts from different years.");
            return false;
        }
        return true;
    }

    private String buildSurgeryCountJpql(Map<String, Object> params) {
        StringBuilder jpql = new StringBuilder();
        jpql.append(" select ")
                .append("   coalesce(upper(c.name), 'OTHER'), ")
                .append("   function('MONTH', b.createdAt), ")
                .append("   count(b) ")
                .append(" from BilledBill b ")
                .append(" join b.procedure p ")
                .append(" join p.item i ")
                .append(" left join i.category c ")
                .append(" where b.retired = false ")
                .append(" and b.cancelled = false ")
                .append(" and b.billType = :bt ")
                .append(" and b.createdAt between :fd and :td ");

        params.put("bt", BillType.SurgeryBill);
        params.put("fd", fromYearStartDate);
        params.put("td", toYearEndDate);

        if (institution != null) {
            jpql.append(" and b.institution = :inst ");
            params.put("inst", institution);
        }
        if (department != null) {
            jpql.append(" and b.department = :dept ");
            params.put("dept", department);
        }
        if (site != null) {
            jpql.append(" and b.department.site = :site ");
            params.put("site", site);
        }
        if (surgeryType != null) {
            jpql.append(" and c = :stype ");
            params.put("stype", surgeryType);
        }
        if (surgeryItem != null) {
            jpql.append(" and i = :sitem ");
            params.put("sitem", surgeryItem);
        }

        jpql.append(" group by coalesce(upper(c.name), 'OTHER'), ")
                .append(" function('MONTH', b.createdAt) ")
                .append(" order by 1 ");

        return jpql.toString();
    }

    private void aggregateResults(List<Object[]> results) {
        SurgeryCountTypeWiseDTO[] monthDtos = new SurgeryCountTypeWiseDTO[12];
        for (int i = 0; i < 12; i++) {
            monthDtos[i] = new SurgeryCountTypeWiseDTO(localizedMonthName(i), i);
        }

        Set<String> categorySet = new TreeSet<>(); // sorted, dedupe on insert

        for (Object[] row : results) {
            String categoryName = (String) row[0];
            int month = ((Number) row[1]).intValue();
            int count = ((Number) row[2]).intValue();
            int monthIndex = month - 1;

            if (monthIndex < 0 || monthIndex >= 12) {
                continue; // defensive, shouldn't happen
            }

            categorySet.add(categoryName);
            monthDtos[monthIndex].addCount(categoryName, count);
            totalCategoryCounts.merge(categoryName, count, Integer::sum);
            totalAllSurgeryCount += count;
        }

        surgeryCategoryNames = new ArrayList<>(categorySet);
        surgeryCountTypeList.addAll(Arrays.asList(monthDtos));
    }

    private String localizedMonthName(int monthIndex) {
        return Month.of(monthIndex + 1).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    private void createSurgeryCountChartModels() {
        if (surgeryCountTypeList == null || surgeryCountTypeList.isEmpty()) {
            surgeryCountBarChartModel = null;
            surgeryCountLineChartModel = null;
            return;
        }

        createSurgeryCountBarChart();
        createSurgeryCountLineChart();
    }

    private void createSurgeryCountBarChart() {
        if (surgeryCountTypeList == null || surgeryCountTypeList.isEmpty()) {
            surgeryCountBarChartModel = null;
            return;
        }

        BarChart barChart = new BarChart();
        BarData barData = new BarData();
        barData.addLabels(shortMonthLabels());

        int colorIndex = 0;
        for (String categoryName : surgeryCategoryNames) {
            int[] rgb = parseRgb(CHART_COLORS[colorIndex % CHART_COLORS.length]);

            BarDataset dataset = new BarDataset()
                    .setLabel(categoryName)
                    .setBackgroundColor(toRgba(rgb, 0.7))
                    .setBorderColor(toRgba(rgb, 1))
                    .setBorderWidth(1);

            for (SurgeryCountTypeWiseDTO dto : surgeryCountTypeList) {
                dataset.addData(dto.getCount(categoryName));
            }
            barData.addDataset(dataset);
            colorIndex++;
        }

        barChart.setData(barData);

        BarOptions barOptionsObj = new BarOptions();
        barOptionsObj.setPlugins(buildChartPlugins());
        barOptionsObj.setScales(buildChartScales());
        barChart.setOptions(barOptionsObj);

        surgeryCountBarChartModel = barChart.toJson();
    }

    private void createSurgeryCountLineChart() {
        if (surgeryCountTypeList == null || surgeryCountTypeList.isEmpty()) {
            surgeryCountLineChartModel = null;
            return;
        }

        LineChart lineChart = new LineChart();
        LineData lineData = new LineData();
        lineData.addLabels(shortMonthLabels());

        int colorIndex = 0;
        for (String categoryName : surgeryCategoryNames) {
            int[] rgb = parseRgb(CHART_COLORS[colorIndex % CHART_COLORS.length]);

            LineDataset dataset = new LineDataset()
                    .setLabel(categoryName)
                    .setBorderColor(toRgba(rgb, 1))
                    .setFill(new Fill(false))
                    .setTension(0.4f);

            for (SurgeryCountTypeWiseDTO dto : surgeryCountTypeList) {
                dataset.addData(dto.getCount(categoryName));
            }
            lineData.addDataset(dataset);
            colorIndex++;
        }

        lineChart.setData(lineData);

        LineOptions lineOptionsObj = new LineOptions();
        lineOptionsObj.setPlugins(buildChartPlugins());
        lineOptionsObj.setScales(buildChartScales());
        lineChart.setOptions(lineOptionsObj);

        surgeryCountLineChartModel = lineChart.toJson();
    }

    private String[] shortMonthLabels() {
        return new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
    }

    private int[] parseRgb(String csv) {
        String[] parts = csv.split(",");
        return new int[]{
            Integer.parseInt(parts[0].trim()),
            Integer.parseInt(parts[1].trim()),
            Integer.parseInt(parts[2].trim())
        };
    }

    private RGBAColor toRgba(int[] rgb, double alpha) {
        return new RGBAColor(rgb[0], rgb[1], rgb[2], alpha);
    }

    private Plugins buildChartPlugins() {
        Plugins plugins = new Plugins();
        plugins.setTitle(new Title().setDisplay(true)
                .setText("Surgery Count Type - Year " + getSelectedYear()));
        plugins.setLegend(new Legend().setDisplay(true).setPosition(Legend.Position.TOP));
        return plugins;
    }

    private Scales buildChartScales() {
        Scales scales = new Scales();
        scales.addScale("y", new LinearScaleOptions()
                .setBeginAtZero(true)
                .setTicks(new LinearTickOptions().setStepSize(1)));
        return scales;
    }

    private static final String[] CHART_COLORS = {
        "75, 192, 192", "255, 99, 132", "54, 162, 235", "255, 206, 86",
        "153, 102, 255", "255, 159, 64", "199, 199, 199", "83, 102, 255",
        "255, 99, 255", "99, 255, 132", "220, 20, 60", "65, 105, 225"
    };

    public void downloadExcelWithChart() {
        if (surgeryCountTypeList == null || surgeryCountTypeList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            downloadedFile = null;
            return;
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Surgery Count");

            int nextFreeRow = writeSurgeryCountExcelTable(workbook, sheet);

            byte[] chartImage = resolveChartImageBytes();
            if (chartImage != null) {
                embedImageInSheet(workbook, sheet, chartImage, nextFreeRow + 2);
            } else if (surgeryCountChartType != null && !surgeryCountChartType.isEmpty()) {
                JsfUtil.addErrorMessage("Chart image could not be captured; "
                        + "downloaded Excel contains table data only. "
                        + "Make sure the chart has fully rendered before downloading.");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            InputStream is = new ByteArrayInputStream(out.toByteArray());

            downloadedFile = DefaultStreamedContent.builder()
                    .name("Surgery_Count_Report_" + getSelectedYear() + ".xlsx")
                    .contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .stream(() -> is)
                    .build();

        } catch (Exception ex) {
            JsfUtil.addErrorMessage("Failed to generate Excel report: " + ex.getMessage());
            downloadedFile = null;
        }
    }

    private int writeSurgeryCountExcelTable(Workbook workbook, Sheet sheet) {
        CellStyle titleStyle = createBoldStyle(workbook, (short) 14, false);
        CellStyle headerStyle = createBoldStyle(workbook, (short) 11, true);
        CellStyle totalStyle = createBoldStyle(workbook, (short) 11, true);

        int rowIdx = 0;

        Row titleRow = sheet.createRow(rowIdx++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Surgery Count Report - Year " + getSelectedYear());
        titleCell.setCellStyle(titleStyle);
        rowIdx++; // spacer

        Row headerRow = sheet.createRow(rowIdx++);
        int col = 0;
        setHeaderCell(headerRow, col++, "Month", headerStyle);
        for (String cat : surgeryCategoryNames) {
            setHeaderCell(headerRow, col++, cat, headerStyle);
        }
        setHeaderCell(headerRow, col, "Total", headerStyle);

        for (SurgeryCountTypeWiseDTO dto : surgeryCountTypeList) {
            Row row = sheet.createRow(rowIdx++);
            col = 0;
            row.createCell(col++).setCellValue(dto.getMonthString());
            for (String cat : surgeryCategoryNames) {
                row.createCell(col++).setCellValue(dto.getCount(cat));
            }
            row.createCell(col).setCellValue(dto.getTotalCount());
        }

        Row totalRow = sheet.createRow(rowIdx++);
        col = 0;
        Cell totalLabel = totalRow.createCell(col++);
        totalLabel.setCellValue("Total");
        totalLabel.setCellStyle(totalStyle);
        for (String cat : surgeryCategoryNames) {
            Cell c = totalRow.createCell(col++);
            c.setCellValue(totalCategoryCounts.getOrDefault(cat, 0));
            c.setCellStyle(totalStyle);
        }
        Cell grandTotal = totalRow.createCell(col);
        grandTotal.setCellValue(totalAllSurgeryCount);
        grandTotal.setCellStyle(totalStyle);

        for (int i = 0; i <= surgeryCategoryNames.size() + 1; i++) {
            sheet.autoSizeColumn(i);
        }

        return rowIdx;
    }

    private void setHeaderCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle createBoldStyle(Workbook workbook, short fontSize, boolean withBorder) {
        org.apache.poi.ss.usermodel.Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints(fontSize);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        if (withBorder) {
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
        return style;
    }

    private void embedImageInSheet(Workbook workbook, Sheet sheet, byte[] imageBytes, int startRow) {
        int pictureIdx = workbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
        anchor.setCol1(0);
        anchor.setRow1(startRow);
        anchor.setCol2(10);
        anchor.setRow2(startRow + 25);
        drawing.createPicture(anchor, pictureIdx);
    }

    public void downloadPdfWithChart() {
        if (surgeryCountTypeList == null || surgeryCountTypeList.isEmpty()) {
            JsfUtil.addErrorMessage("No data to export. Please process the report first.");
            downloadedFile = null;
            return;
        }

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph("Surgery Count Report - Year " + getSelectedYear(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph(" "));

            addSurgeryCountPdfTable(document);

            byte[] chartImage = resolveChartImageBytes();
            if (chartImage != null) {
                document.newPage();
                Image img = Image.getInstance(chartImage);
                img.scaleToFit(760, 500);
                img.setAlignment(Image.ALIGN_CENTER);
                document.add(img);
            }

            document.close();
            InputStream is = new ByteArrayInputStream(out.toByteArray());

            downloadedFile = DefaultStreamedContent.builder()
                    .name("Surgery_Count_Report_" + getSelectedYear() + ".pdf")
                    .contentType("application/pdf")
                    .stream(() -> is)
                    .build();

        } catch (Exception ex) {
            JsfUtil.addErrorMessage("Failed to generate PDF report: " + ex.getMessage());
            downloadedFile = null;
        }
    }

    private void addSurgeryCountPdfTable(Document document) throws DocumentException {
        int numCols = surgeryCategoryNames.size() + 2;
        PdfPTable table = new PdfPTable(numCols);
        table.setWidthPercentage(100);

        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        BaseColor headerBg = new BaseColor(52, 73, 94);
        BaseColor totalBg = new BaseColor(230, 230, 230);

        addPdfHeaderCell(table, "Month", headerFont, headerBg);
        for (String cat : surgeryCategoryNames) {
            addPdfHeaderCell(table, cat, headerFont, headerBg);
        }
        addPdfHeaderCell(table, "Total", headerFont, headerBg);

        for (SurgeryCountTypeWiseDTO dto : surgeryCountTypeList) {
            table.addCell(new Phrase(dto.getMonthString(), cellFont));
            for (String cat : surgeryCategoryNames) {
                table.addCell(new Phrase(String.valueOf(dto.getCount(cat)), cellFont));
            }
            table.addCell(new Phrase(String.valueOf(dto.getTotalCount()), cellFont));
        }

        addPdfHeaderCell(table, "Total", totalFont, totalBg);
        for (String cat : surgeryCategoryNames) {
            addPdfTotalsCell(table, String.valueOf(totalCategoryCounts.getOrDefault(cat, 0)), totalFont, totalBg);
        }
        addPdfTotalsCell(table, String.valueOf(totalAllSurgeryCount), totalFont, totalBg);

        document.add(table);
    }

    private void addPdfHeaderCell(PdfPTable table, String text, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addPdfTotalsCell(PdfPTable table, String text, Font font, BaseColor bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private byte[] resolveChartImageBytes() {
        String base64 = null;
        if ("bar".equalsIgnoreCase(surgeryCountChartType)) {
            base64 = barChartImageBase64;
        } else if ("line".equalsIgnoreCase(surgeryCountChartType)) {
            base64 = lineChartImageBase64;
        }
        if (base64 == null || base64.isEmpty()) {
            return null;
        }
        int comma = base64.indexOf(',');
        String pureBase64 = comma >= 0 ? base64.substring(comma + 1) : base64;
        return Base64.getDecoder().decode(pureBase64);
    }

    public Date getFromYearStartDate() {
        if (fromYearStartDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, Calendar.JANUARY);
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            fromYearStartDate = cal.getTime();
        }
        return fromYearStartDate;
    }

    public void setFromYearStartDate(Date fromYearStartDate) {
        this.fromYearStartDate = fromYearStartDate;
    }

    public Date getToYearEndDate() {
        if (toYearEndDate == null) {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.MONTH, Calendar.DECEMBER);
            cal.set(Calendar.DAY_OF_MONTH, 31);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);

            toYearEndDate = cal.getTime();
        }
        return toYearEndDate;
    }

    public void setToYearEndDate(Date toYearEndDate) {
        this.toYearEndDate = toYearEndDate;
    }

    public String getSurgeryCountChartType() {
        return surgeryCountChartType;
    }

    public void setSurgeryCountChartType(String surgeryCountChartType) {
        this.surgeryCountChartType = surgeryCountChartType;
    }

    public List<SurgeryCountTypeWiseDTO> getSurgeryCountTypeList() {
        return surgeryCountTypeList;
    }

    public int getSelectedYear() {
        Calendar cal = Calendar.getInstance();
        if (fromYearStartDate != null) {
            cal.setTime(fromYearStartDate);
        }
        selectedYear = cal.get(Calendar.YEAR);
        return selectedYear;
    }

    public String getSurgeryCountBarChartModel() {
        return surgeryCountBarChartModel;
    }

    public void setSurgeryCountBarChartModel(String surgeryCountBarChartModel) {
        this.surgeryCountBarChartModel = surgeryCountBarChartModel;
    }

    public String getSurgeryCountLineChartModel() {
        return surgeryCountLineChartModel;
    }

    public void setSurgeryCountLineChartModel(String surgeryCountLineChartModel) {
        this.surgeryCountLineChartModel = surgeryCountLineChartModel;
    }

    public List<String> getSurgeryCategoryNames() {
        return surgeryCategoryNames;
    }

    public void setSurgeryCategoryNames(List<String> surgeryCategoryNames) {
        this.surgeryCategoryNames = surgeryCategoryNames;
    }

    public Map<String, Integer> getTotalCategoryCounts() {
        return totalCategoryCounts;
    }

    public void setTotalCategoryCounts(Map<String, Integer> totalCategoryCounts) {
        this.totalCategoryCounts = totalCategoryCounts;
    }

    public int getTotalAllSurgeryCount() {
        return totalAllSurgeryCount;
    }

    public void setTotalAllSurgeryCount(int totalAllSurgeryCount) {
        this.totalAllSurgeryCount = totalAllSurgeryCount;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public SurgeryType getSurgeryType() {
        return surgeryType;
    }

    public void setSurgeryType(SurgeryType surgeryType) {
        this.surgeryType = surgeryType;
    }

    public Item getSurgeryItem() {
        return surgeryItem;
    }

    public void setSurgeryItem(Item surgeryItem) {
        this.surgeryItem = surgeryItem;
    }

    public String getBarChartImageBase64() {
        return barChartImageBase64;
    }

    public void setBarChartImageBase64(String barChartImageBase64) {
        this.barChartImageBase64 = barChartImageBase64;
    }

    public String getLineChartImageBase64() {
        return lineChartImageBase64;
    }

    public void setLineChartImageBase64(String lineChartImageBase64) {
        this.lineChartImageBase64 = lineChartImageBase64;
    }

    public StreamedContent getDownloadedFile() {
        return downloadedFile;
    }

    public void setDownloadedFile(StreamedContent downloadedFile) {
        this.downloadedFile = downloadedFile;
    }
}
