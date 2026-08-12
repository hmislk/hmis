/*
 * Open Hospital Management Information System
 */
package com.divudi.bean.report;

import com.divudi.bean.lab.LaborataryReportController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.dataStructure.AnalyzerInvestigationCountRow;
import com.divudi.core.entity.BilledBill;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.Machine;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.InvestigationFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Controller for the "Analyzer vise Investigation Counts" report.
 *
 * Lists the number of times each investigation was performed within a date
 * range, grouped by investigation and its analyzer (machine). Uses a direct
 * DTO JPQL query ({@link AnalyzerInvestigationCountRow}) instead of the
 * per-item counting used by the legacy report.
 */
@Named
@SessionScoped
public class AnalyzerInvestigationCountController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private InvestigationFacade investigationFacade;

    @Inject
    private LaborataryReportController laborataryReportController;

    private Date fromDate;
    private Date toDate;
    private Machine machine;

    private List<AnalyzerInvestigationCountRow> items;
    private Long totalCount;

    public AnalyzerInvestigationCountController() {
    }

    public void createAnalyzerInvestigationCounts() {
        items = new ArrayList<>();
        totalCount = 0L;

        Map<String, Object> m = new HashMap<>();
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT new com.divudi.core.data.dataStructure.AnalyzerInvestigationCountRow("
                + "ix.id, ix.code, ix.name, d.name, m.name, count(bi)) ");
        jpql.append("FROM BillItem bi ");
        jpql.append("JOIN bi.item ix ");
        jpql.append("LEFT JOIN ix.department d ");
        jpql.append("JOIN ix.machine m ");
        jpql.append("WHERE type(ix) = :ixtype ");
        jpql.append("AND type(bi.bill) = :billClass ");
        jpql.append("AND bi.bill.billType in :bts ");
        jpql.append("AND bi.retired = false ");
        jpql.append("AND bi.bill.retired = false ");
        jpql.append("AND bi.bill.cancelled = false ");
        jpql.append("AND (bi.refunded is null or bi.refunded = false) ");
        jpql.append("AND bi.bill.createdAt between :fd and :td ");

        if (machine != null) {
            jpql.append("AND ix.machine = :mac ");
            m.put("mac", machine);
        }

        jpql.append("GROUP BY ix, m ");
        jpql.append("ORDER BY ix.name");

        m.put("ixtype", Investigation.class);
        m.put("billClass", BilledBill.class);
        m.put("bts", Arrays.asList(BillType.OpdBill, BillType.LabBill, BillType.InwardBill, BillType.CollectingCentreBill));
        m.put("fd", fromDate);
        m.put("td", toDate);

        items = (List<AnalyzerInvestigationCountRow>) (Object) billItemFacade.findLightsByJpqlWithoutCache(jpql.toString(), m, TemporalType.TIMESTAMP);

        for (AnalyzerInvestigationCountRow row : items) {
            if (row.getCount() != null) {
                totalCount += row.getCount();
            }
        }
    }

    /**
     * Navigate to the investigation bill-item list for the selected test,
     * pre-filtered by this report's date range and the chosen investigation.
     */
    public String navigateToBillItemList(Long investigationId) {
        if (investigationId == null) {
            JsfUtil.addErrorMessage("Investigation not found.");
            return null;
        }
        Investigation ix = investigationFacade.find(investigationId);
        if (ix == null) {
            JsfUtil.addErrorMessage("Investigation not found.");
            return null;
        }
        laborataryReportController.resetAllFiltersExceptDateRange();
        laborataryReportController.setFromDate(fromDate);
        laborataryReportController.setToDate(toDate);
        laborataryReportController.setInvestigation(ix);
        laborataryReportController.processLaborataryBillItemReportDto();
        return "/reportLab/bill_item_list?faces-redirect=true";
    }

    public void downloadExcel() {
        if (items == null || items.isEmpty()) {
            return;
        }

        FacesContext facesContext = FacesContext.getCurrentInstance();
        HttpServletResponse response =
                (HttpServletResponse) facesContext.getExternalContext().getResponse();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Investigation Counts");

            final int colCount = 6;

            // Styles
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.CENTER);

            CellStyle subtitleStyle = workbook.createCellStyle();
            subtitleStyle.setAlignment(HorizontalAlignment.CENTER);

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            CellStyle totalStyle = workbook.createCellStyle();
            totalStyle.setFont(totalFont);
            totalStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMMM dd hh:mm a");
            int rowIdx = 0;

            // Title
            Row titleRow = sheet.createRow(rowIdx++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Analyzer vise Investigation Counts");
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, colCount - 1));

            // Analyzer filter
            Row analyzerRow = sheet.createRow(rowIdx++);
            Cell analyzerCell = analyzerRow.createCell(0);
            analyzerCell.setCellValue("Analyzer: " + (machine != null ? machine.getName() : "All Analyzers"));
            analyzerCell.setCellStyle(subtitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, colCount - 1));

            // Date range
            Row dateRow = sheet.createRow(rowIdx++);
            Cell dateCell = dateRow.createCell(0);
            dateCell.setCellValue(sdf.format(fromDate) + "  to  " + sdf.format(toDate));
            dateCell.setCellStyle(subtitleStyle);
            sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, colCount - 1));

            rowIdx++; // blank row

            // Column headers
            String[] headers = {"No", "Code", "Test", "Department", "Analyzer", "Count"};
            Row headerRow = sheet.createRow(rowIdx++);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int no = 1;
            for (AnalyzerInvestigationCountRow r : items) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(no++);
                row.createCell(1).setCellValue(r.getCode() != null ? r.getCode() : "");
                row.createCell(2).setCellValue(r.getTestName() != null ? r.getTestName() : "");
                row.createCell(3).setCellValue(r.getDepartmentName() != null ? r.getDepartmentName() : "");
                row.createCell(4).setCellValue(r.getAnalyzerName() != null ? r.getAnalyzerName() : "");
                row.createCell(5).setCellValue(r.getCount() != null ? r.getCount() : 0);
            }

            // Grand total
            Row totalRow = sheet.createRow(rowIdx++);
            Cell totalLabel = totalRow.createCell(2);
            totalLabel.setCellValue("Total");
            totalLabel.setCellStyle(totalStyle);
            Cell totalValue = totalRow.createCell(5);
            totalValue.setCellValue(totalCount != null ? totalCount : 0);
            totalValue.setCellStyle(totalStyle);

            for (int i = 0; i < colCount; i++) {
                sheet.autoSizeColumn(i);
            }

            String filename = "Analyzer_Investigation_Counts_"
                    + new SimpleDateFormat("yyyyMMdd_HHmm").format(new Date()) + ".xlsx";
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            try (OutputStream out = response.getOutputStream()) {
                workbook.write(out);
            }
            facesContext.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfMonth();
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Machine getMachine() {
        return machine;
    }

    public void setMachine(Machine machine) {
        this.machine = machine;
    }

    public List<AnalyzerInvestigationCountRow> getItems() {
        return items;
    }

    public void setItems(List<AnalyzerInvestigationCountRow> items) {
        this.items = items;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

}
