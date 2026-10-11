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
 * Every row is signed by the direction the money moves (refunds and the
 * cancellation of a receipt are negative), so one amount column nets correctly
 * over a period. A cancelled bill stays on its own date with its original
 * amount, flagged Cancelled; the cancellation bill is listed as a separate
 * signed row on the date it was made. Net is therefore the true cash movement
 * for any date range. The same result set is presented
 * four ways through the View selector - a per-type summary, grouped by bill
 * type, grouped by BHT, or a flat detail list - rather than as four reports.
 */
@Named
@SessionScoped
public class InwardCombinedPaymentReportController implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The money movements this report covers: the three inward payment kinds,
     * the refund of each, and the cancellation bills that reverse them. The
     * original of a cancelled bill keeps its amount on its own date and the
     * cancellation bill is a separate signed row on the cancel date, so a
     * period that sees only one of the two still reports the cash that really
     * moved in it.
     */
    private static final List<BillTypeAtomic> REPORTED_TYPES = Arrays.asList(
            BillTypeAtomic.INWARD_DEPOSIT,
            BillTypeAtomic.INWARD_PAYMENT,
            BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT,
            BillTypeAtomic.INWARD_DEPOSIT_REFUND,
            BillTypeAtomic.INWARD_PAYMENT_REFUND,
            BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT_REFUND,
            BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION,
            BillTypeAtomic.INWARD_PAYMENT_CANCELLATION,
            BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT_CANCELLATION,
            BillTypeAtomic.INWARD_DEPOSIT_REFUND_CANCELLATION,
            BillTypeAtomic.INWARD_PAYMENT_REFUND_CANCELLATION);

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
    private double totalCancelled;
    private int cancelledCount;
    private boolean reportGenerated;

    public void makeNull() {
        reportRows = null;
        groups = null;
        grandTotal = 0;
        totalCashIn = 0;
        totalCashOut = 0;
        totalCancelled = 0;
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
        totalCancelled = 0;
        cancelledCount = 0;

        Map<String, InwardCombinedPaymentGroupDto> byKey = new LinkedHashMap<>();
        boolean groupByBht = VIEW_GROUPED_BHT.equals(viewMode);

        for (InwardCombinedPaymentRowDto row : reportRows) {
            double signed = row.getSignedAmount();
            grandTotal += signed;
            // By kind, not by sign: a deposit cancellation is negative but is not a refund.
            if (row.isCancellation()) {
                totalCancelled += signed;
            } else if (row.isRefund()) {
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
     * postProcessor shared by both Excel exports. p:dataExporter writes only
     * the one table it targets, so the Received / Refunded / Cancelled / Net
     * cards shown above the tables would otherwise be missing from the
     * workbook. They are appended to the first sheet below the exported table.
     * In a grouped view the subtotals table is also shown above the rows, so
     * its group totals are added as a second "Totals" sheet with a grand total
     * line.
     */
    public void postProcessExport(Object document) {
        if (!(document instanceof Workbook)) {
            return;
        }
        Workbook workbook = (Workbook) document;

        Font bold = workbook.createFont();
        bold.setBold(true);
        CellStyle boldStyle = workbook.createCellStyle();
        boldStyle.setFont(bold);
        CellStyle money = workbook.createCellStyle();
        money.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        CellStyle boldMoney = workbook.createCellStyle();
        boldMoney.setFont(bold);
        boldMoney.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

        Sheet first = workbook.getSheetAt(0);
        int r = first.getLastRowNum() + 2;
        Cell periodHeader = first.createRow(r++).createCell(0);
        periodHeader.setCellValue("Period totals");
        periodHeader.setCellStyle(boldStyle);

        Row received = first.createRow(r++);
        received.createCell(0).setCellValue("Received");
        writeMoney(received, 1, totalCashIn, money);
        Row refunded = first.createRow(r++);
        refunded.createCell(0).setCellValue("Refunded");
        writeMoney(refunded, 1, totalCashOut, money);
        Row cancelledRow = first.createRow(r++);
        cancelledRow.createCell(0).setCellValue("Cancelled");
        writeMoney(cancelledRow, 1, totalCancelled, money);
        Row net = first.createRow(r++);
        Cell netLabel = net.createCell(0);
        netLabel.setCellValue("Net");
        netLabel.setCellStyle(boldStyle);
        writeMoney(net, 1, grandTotal, boldMoney);
        Row cancelledBills = first.createRow(r);
        cancelledBills.createCell(0).setCellValue("Cancelled bills");
        cancelledBills.createCell(1).setCellValue(cancelledCount);

        if (!isGroupedView() || groups == null) {
            return;
        }
        Sheet sheet = workbook.createSheet("Totals");

        String[] headers = {getGroupColumnHeader(), "Bills", "Received", "Refunded", "Cancelled", "Net"};
        Row header = sheet.createRow(0);
        for (int c = 0; c < headers.length; c++) {
            Cell cell = header.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(boldStyle);
        }

        int gr = 1;
        int totalBills = 0;
        for (InwardCombinedPaymentGroupDto g : groups) {
            Row row = sheet.createRow(gr++);
            row.createCell(0).setCellValue(g.getGroupLabel());
            row.createCell(1).setCellValue(g.getCount());
            writeMoney(row, 2, g.getCashIn(), money);
            writeMoney(row, 3, g.getCashOut(), money);
            writeMoney(row, 4, g.getCancelled(), money);
            writeMoney(row, 5, g.getTotal(), money);
            totalBills += g.getCount();
        }

        Row total = sheet.createRow(gr);
        Cell label = total.createCell(0);
        label.setCellValue("Total");
        label.setCellStyle(boldStyle);
        Cell bills = total.createCell(1);
        bills.setCellValue(totalBills);
        bills.setCellStyle(boldStyle);
        writeMoney(total, 2, totalCashIn, boldMoney);
        writeMoney(total, 3, totalCashOut, boldMoney);
        writeMoney(total, 4, totalCancelled, boldMoney);
        writeMoney(total, 5, grandTotal, boldMoney);

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

    public double getTotalCancelled() {
        return totalCancelled;
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
