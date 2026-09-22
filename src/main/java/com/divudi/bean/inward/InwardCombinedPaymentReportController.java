package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.dto.InwardCombinedPaymentGroupDto;
import com.divudi.core.data.dto.InwardCombinedPaymentRowDto;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.BillFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 * Combined Inward Payments report: inward deposits, inward payments and
 * post-final-bill payments listed together with their refunds, which the
 * application otherwise only offers on three separate per-type search pages.
 *
 * Refunds are signed negative rather than given a column of their own, so one
 * amount column nets correctly over a period. The same result set is presented
 * four ways through the View selector - a per-type summary, grouped by bill
 * type, grouped by BHT, or a flat detail list - rather than as four reports.
 */
@Named
@SessionScoped
public class InwardCombinedPaymentReportController implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The money movements this report covers: the three inward payment kinds
     * and the refund of each. Cancellation bills are deliberately not listed -
     * a cancelled bill already appears here flagged as cancelled and
     * contributing zero, so listing its contra bill as well would double the
     * reversal.
     */
    private static final List<BillTypeAtomic> REPORTED_TYPES = Arrays.asList(
            BillTypeAtomic.INWARD_DEPOSIT,
            BillTypeAtomic.INWARD_PAYMENT,
            BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT,
            BillTypeAtomic.INWARD_DEPOSIT_REFUND,
            BillTypeAtomic.INWARD_PAYMENT_REFUND,
            BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT_REFUND);

    public static final String VIEW_SUMMARY = "SUMMARY";
    public static final String VIEW_GROUPED_TYPE = "GROUPED_TYPE";
    public static final String VIEW_GROUPED_BHT = "GROUPED_BHT";
    public static final String VIEW_DETAIL = "DETAIL";

    @EJB
    private BillFacade billFacade;
    @Inject
    private SessionController sessionController;

    private Date fromDate = startOfToday();
    private Date toDate = endOfToday();
    private Department department;
    private Institution institution;
    private String viewMode = VIEW_SUMMARY;

    private List<InwardCombinedPaymentRowDto> reportRows;
    private List<InwardCombinedPaymentGroupDto> groups;
    private double grandTotal;
    private double totalCashIn;
    private double totalCashOut;
    private int cancelledCount;
    private boolean reportGenerated;

    public void makeNull() {
        reportRows = null;
        groups = null;
        grandTotal = 0;
        totalCashIn = 0;
        totalCashOut = 0;
        cancelledCount = 0;
        reportGenerated = false;
    }

    public void generateReport() {
        reportRows = fetchRows();
        buildGroups();
        reportGenerated = true;
    }

    /**
     * Every relationship is joined with an explicit LEFT JOIN. A path
     * expression such as b.patientEncounter.patient.person.name would be
     * turned into an INNER JOIN, silently dropping any bill whose encounter,
     * patient or person is missing - which is exactly the kind of row a
     * reconciliation report must not lose.
     */
    private List<InwardCombinedPaymentRowDto> fetchRows() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "select new com.divudi.core.data.dto.InwardCombinedPaymentRowDto("
                + " b.id, b.deptId, b.createdAt, b.billTypeAtomic,"
                + " pe.bhtNo, p.name, b.paymentMethod, b.netTotal, b.cancelled) "
                + " from Bill b "
                + " left join b.patientEncounter pe "
                + " left join pe.patient pt "
                + " left join pt.person p "
                + " where b.retired = false "
                + " and b.billTypeAtomic in :btas "
                + " and b.createdAt between :fromDate and :toDate ");

        if (department != null) {
            jpql.append(" and b.department = :dept ");
            params.put("dept", department);
        }
        if (institution != null) {
            jpql.append(" and b.institution = :ins ");
            params.put("ins", institution);
        }

        jpql.append(" order by b.createdAt, b.id ");

        params.put("btas", REPORTED_TYPES);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        List<InwardCombinedPaymentRowDto> fetched
                = (List<InwardCombinedPaymentRowDto>) billFacade.findLightsByJpqlWithoutCache(
                        jpql.toString(), params, TemporalType.TIMESTAMP);
        return fetched != null ? fetched : new ArrayList<InwardCombinedPaymentRowDto>();
    }

    /**
     * Builds the grouping the selected view needs and the running totals.
     * Summary and "grouped by type" share the same per-type grouping; the
     * summary view simply prints the group headers without their rows.
     */
    private void buildGroups() {
        groups = new ArrayList<>();
        grandTotal = 0;
        totalCashIn = 0;
        totalCashOut = 0;
        cancelledCount = 0;

        Map<String, InwardCombinedPaymentGroupDto> byKey = new LinkedHashMap<>();
        boolean groupByBht = VIEW_GROUPED_BHT.equals(viewMode);

        for (InwardCombinedPaymentRowDto row : reportRows) {
            double signed = row.getSignedAmount();
            grandTotal += signed;
            if (signed < 0) {
                totalCashOut += signed;
            } else {
                totalCashIn += signed;
            }
            if (row.isCancelled()) {
                cancelledCount++;
            }

            String key;
            if (groupByBht) {
                key = row.getBhtNo() != null && !row.getBhtNo().trim().isEmpty()
                        ? row.getBhtNo() : "(no BHT)";
            } else {
                key = row.getTypeLabel();
            }
            InwardCombinedPaymentGroupDto group = byKey.get(key);
            if (group == null) {
                group = new InwardCombinedPaymentGroupDto(key);
                byKey.put(key, group);
            }
            row.setGroupKey(key);
            group.add(row);
        }

        groups.addAll(byKey.values());
    }

    /**
     * Rows in the order the selected view shows them: grouped together under
     * their group in a grouped view, plain chronological order otherwise. The
     * grouped order comes from walking the groups rather than re-sorting, so
     * rows keep their chronological order inside each group.
     */
    public List<InwardCombinedPaymentRowDto> getDisplayRows() {
        if (reportRows == null) {
            return new ArrayList<>();
        }
        if (!isGroupedView() || groups == null) {
            return reportRows;
        }
        List<InwardCombinedPaymentRowDto> ordered = new ArrayList<>();
        for (InwardCombinedPaymentGroupDto group : groups) {
            ordered.addAll(group.getRows());
        }
        return ordered;
    }

    /**
     * Heading for the subtotal table, which depends on what the rows are
     * grouped by.
     */
    public String getGroupColumnHeader() {
        return VIEW_GROUPED_BHT.equals(viewMode) ? "BHT No" : "Bill Type";
    }

    /**
     * postProcessor for the rows export. In a grouped view the screen shows a
     * subtotals table above the rows, but p:dataExporter only writes the one
     * table it targets - so the group totals are added here as a second
     * "Totals" sheet, with a grand total line, keeping the workbook in step
     * with what is displayed. The Details view has no subtotals and is left
     * as exported.
     */
    public void postProcessRowsExport(Object document) {
        if (!(document instanceof Workbook) || !isGroupedView() || groups == null) {
            return;
        }
        Workbook workbook = (Workbook) document;
        Sheet sheet = workbook.createSheet("Totals");

        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle boldStyle = workbook.createCellStyle();
        boldStyle.setFont(bold);
        CellStyle money = workbook.createCellStyle();
        money.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        CellStyle boldMoney = workbook.createCellStyle();
        boldMoney.setFont(bold);
        boldMoney.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

        String[] headers = {getGroupColumnHeader(), "Bills", "Received", "Refunded", "Net"};
        Row header = sheet.createRow(0);
        for (int c = 0; c < headers.length; c++) {
            Cell cell = header.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(boldStyle);
        }

        int r = 1;
        int totalBills = 0;
        for (InwardCombinedPaymentGroupDto g : groups) {
            Row row = sheet.createRow(r++);
            row.createCell(0).setCellValue(g.getGroupLabel());
            row.createCell(1).setCellValue(g.getCount());
            writeMoney(row, 2, g.getCashIn(), money);
            writeMoney(row, 3, g.getCashOut(), money);
            writeMoney(row, 4, g.getTotal(), money);
            totalBills += g.getCount();
        }

        Row total = sheet.createRow(r);
        Cell label = total.createCell(0);
        label.setCellValue("Total");
        label.setCellStyle(boldStyle);
        Cell bills = total.createCell(1);
        bills.setCellValue(totalBills);
        bills.setCellStyle(boldStyle);
        writeMoney(total, 2, totalCashIn, boldMoney);
        writeMoney(total, 3, totalCashOut, boldMoney);
        writeMoney(total, 4, grandTotal, boldMoney);

        for (int c = 0; c < headers.length; c++) {
            sheet.autoSizeColumn(c);
        }
    }

    private void writeMoney(Row row, int column, double value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    public boolean isSummaryView() {
        return VIEW_SUMMARY.equals(viewMode);
    }

    public boolean isDetailView() {
        return VIEW_DETAIL.equals(viewMode);
    }

    public boolean isGroupedView() {
        return VIEW_GROUPED_TYPE.equals(viewMode) || VIEW_GROUPED_BHT.equals(viewMode);
    }

    public boolean isNoResults() {
        return reportGenerated && (reportRows == null || reportRows.isEmpty());
    }

    private Date startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private Date endOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public String getViewMode() {
        return viewMode;
    }

    public void setViewMode(String viewMode) {
        this.viewMode = viewMode;
    }

    public List<InwardCombinedPaymentRowDto> getReportRows() {
        return reportRows;
    }

    public List<InwardCombinedPaymentGroupDto> getGroups() {
        return groups;
    }

    public double getGrandTotal() {
        return grandTotal;
    }

    public double getTotalCashIn() {
        return totalCashIn;
    }

    public double getTotalCashOut() {
        return totalCashOut;
    }

    public int getCancelledCount() {
        return cancelledCount;
    }

    public boolean isReportGenerated() {
        return reportGenerated;
    }

    public SessionController getSessionController() {
        return sessionController;
    }
}
