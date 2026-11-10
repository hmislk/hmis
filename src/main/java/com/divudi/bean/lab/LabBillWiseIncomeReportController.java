package com.divudi.bean.lab;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.dto.LabBillWiseIncomeItemDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.WebUser;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.util.CommonFunctions;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Lab Analytics → Income → Lab Bill-wise Income (A4).
 *
 * Lists every original lab bill (OPD, Inward, CC, Package) day by day in bill
 * number order, with each test's gross, discount and net value. Cancelled bills
 * and refunded items are shown and marked, and the day totals show them
 * separately.
 *
 * @author H.K. Damith Deshan | hkddrajapaksha@gmail.com
 */
@Named
@SessionScoped
public class LabBillWiseIncomeReportController implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final List<BillTypeAtomic> ORIGINAL_BILL_TYPES = Arrays.asList(
            BillTypeAtomic.OPD_BILL_WITH_PAYMENT,
            BillTypeAtomic.OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER,
            BillTypeAtomic.INWARD_SERVICE_BILL,
            BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT,
            BillTypeAtomic.PACKAGE_OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER,
            BillTypeAtomic.CC_BILL
    );

    @EJB
    private BillItemFacade billItemFacade;

    private Date fromDate;
    private Date toDate;
    private Institution institution;
    private Department department;
    private WebUser webUser;
    private String visitType;

    private List<DayGroup> days;
    private Totals grandTotals;
    private List<Map.Entry<String, Totals>> paymentMethodSummary;
    private List<Map.Entry<String, Totals>> visitTypeSummary;

    public String navigateToReport() {
        days = null;
        grandTotals = null;
        return "/reportLab/lab_bill_wise_income?faces-redirect=true";
    }

    public void clearFilters() {
        institution = null;
        department = null;
        webUser = null;
        visitType = null;
        days = null;
        grandTotals = null;
    }

    public void processReport() {
        Map<String, Object> params = new HashMap<>();
        String jpql = "select new com.divudi.core.data.dto.LabBillWiseIncomeItemDTO("
                + " b.id, b.deptId, b.createdAt, b.cancelled, b.paymentMethod, "
                + " pe.id, cc.id, "
                + " bi.id, i.name, bi.grossValue, bi.discount, bi.netValue, bi.refunded) "
                + " from BillItem bi "
                + " join bi.bill b "
                + " left join b.patientEncounter pe "
                + " left join b.collectingCentre cc "
                + " left join bi.item i "
                + " where b.retired=:ret "
                + " and bi.retired=:ret "
                + " and b.billTypeAtomic in :btas "
                + " and b.toDepartment.departmentType=:dtype "
                + " and b.createdAt between :fd and :td ";
        params.put("ret", false);
        params.put("btas", ORIGINAL_BILL_TYPES);
        params.put("dtype", DepartmentType.Lab);
        params.put("fd", getFromDate());
        params.put("td", getToDate());

        if (institution != null) {
            jpql += " and b.institution=:ins ";
            params.put("ins", institution);
        }
        if (department != null) {
            jpql += " and b.department=:dep ";
            params.put("dep", department);
        }
        if (webUser != null) {
            jpql += " and b.creater=:wu ";
            params.put("wu", webUser);
        }
        // Same rule as Bill.getIpOpOrCc(), so the filter matches the shown visit type
        if ("OP".equals(visitType)) {
            jpql += " and b.patientEncounter is null and b.collectingCentre is null ";
        } else if ("IP".equals(visitType)) {
            jpql += " and b.patientEncounter is not null ";
        } else if ("CC".equals(visitType)) {
            jpql += " and b.patientEncounter is null and b.collectingCentre is not null ";
        }
        jpql += " order by b.deptId, b.id, bi.id";

        @SuppressWarnings("unchecked")
        List<LabBillWiseIncomeItemDTO> rows = (List<LabBillWiseIncomeItemDTO>) billItemFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        // day (start of day) -> bill id -> bill group; bills keep bill number order
        Map<Date, Map<Long, BillGroup>> byDay = new TreeMap<>();
        for (LabBillWiseIncomeItemDTO row : rows) {
            Date day = CommonFunctions.getStartOfDay(row.getBillCreatedAt());
            Map<Long, BillGroup> bills = byDay.computeIfAbsent(day, d -> new LinkedHashMap<>());
            BillGroup group = bills.computeIfAbsent(row.getBillId(), id -> new BillGroup(row));
            group.addItem(row);
        }

        days = new ArrayList<>();
        grandTotals = new Totals();
        Map<String, Totals> byPaymentMethod = new TreeMap<>();
        Map<String, Totals> byVisitType = new LinkedHashMap<>();
        byVisitType.put("OP", new Totals());
        byVisitType.put("IP", new Totals());
        byVisitType.put("CC", new Totals());
        for (Map.Entry<Date, Map<Long, BillGroup>> e : byDay.entrySet()) {
            DayGroup day = new DayGroup(e.getKey(), new ArrayList<>(e.getValue().values()));
            days.add(day);
            grandTotals.add(day.getTotals());
            for (BillGroup bg : day.getBills()) {
                String pm = bg.getPaymentMethodLabel().isEmpty() ? "Not Specified" : bg.getPaymentMethodLabel();
                byPaymentMethod.computeIfAbsent(pm, k -> new Totals()).addBill(bg);
                byVisitType.computeIfAbsent(bg.getVisitType(), k -> new Totals()).addBill(bg);
            }
        }
        // SimpleEntry is Serializable (TreeMap entries are not); this bean is session scoped
        paymentMethodSummary = new ArrayList<>();
        for (Map.Entry<String, Totals> e : byPaymentMethod.entrySet()) {
            paymentMethodSummary.add(new AbstractMap.SimpleEntry<>(e));
        }
        visitTypeSummary = new ArrayList<>();
        for (Map.Entry<String, Totals> e : byVisitType.entrySet()) {
            if (e.getValue().getBillCount() > 0) {
                visitTypeSummary.add(new AbstractMap.SimpleEntry<>(e));
            }
        }
    }

    // ------------------------------------------------------------------
    // Row classes
    // ------------------------------------------------------------------
    public static class DayGroup implements Serializable {

        private final Date date;
        private final List<BillGroup> bills;
        private final Totals totals = new Totals();

        public DayGroup(Date date, List<BillGroup> bills) {
            this.date = date;
            this.bills = bills;
            for (BillGroup b : bills) {
                totals.addBill(b);
            }
        }

        public Date getDate() {
            return date;
        }

        public List<BillGroup> getBills() {
            return bills;
        }

        public Totals getTotals() {
            return totals;
        }
    }

    public static class BillGroup implements Serializable {

        /**
         * Bill-level values are read from the first item row of the bill.
         */
        private final LabBillWiseIncomeItemDTO header;
        private final List<LabBillWiseIncomeItemDTO> items = new ArrayList<>();
        private double gross;
        private double discount;
        private double net;
        private double refundedNet;
        private long refundedCount;

        public BillGroup(LabBillWiseIncomeItemDTO header) {
            this.header = header;
        }

        void addItem(LabBillWiseIncomeItemDTO row) {
            items.add(row);
            gross += row.getGross();
            discount += row.getDiscountValue();
            net += row.getNet();
            if (row.isItemRefunded()) {
                refundedNet += row.getNet();
                refundedCount++;
            }
        }

        public long getRefundedCount() {
            return refundedCount;
        }

        public String getBillNo() {
            return header.getBillNo();
        }

        public String getVisitType() {
            return header.getVisitType();
        }

        public List<LabBillWiseIncomeItemDTO> getItems() {
            return items;
        }

        public double getGross() {
            return gross;
        }

        public double getDiscount() {
            return discount;
        }

        public double getNet() {
            return net;
        }

        public double getRefundedNet() {
            return refundedNet;
        }

        public boolean isCancelled() {
            return header.isCancelled();
        }

        public String getPaymentMethodLabel() {
            return header.getPaymentMethodLabel();
        }
    }

    /**
     * Totals of all listed bills, with cancelled bills and refunded items
     * shown separately. A refunded item on a cancelled bill is counted only as
     * cancelled.
     */
    public static class Totals implements Serializable {

        private long billCount;
        private double gross;
        private double discount;
        private double net;
        private double cancelledNet;
        private double refundedNet;
        private long testCount;
        private long cancelledBillCount;
        private long refundedTestCount;

        void addBill(BillGroup b) {
            billCount++;
            testCount += b.getItems().size();
            gross += b.getGross();
            discount += b.getDiscount();
            net += b.getNet();
            if (b.isCancelled()) {
                cancelledBillCount++;
                cancelledNet += b.getNet();
            } else {
                refundedTestCount += b.getRefundedCount();
                refundedNet += b.getRefundedNet();
            }
        }

        void add(Totals t) {
            billCount += t.billCount;
            testCount += t.testCount;
            cancelledBillCount += t.cancelledBillCount;
            refundedTestCount += t.refundedTestCount;
            gross += t.gross;
            discount += t.discount;
            net += t.net;
            cancelledNet += t.cancelledNet;
            refundedNet += t.refundedNet;
        }

        public long getBillCount() {
            return billCount;
        }

        public long getTestCount() {
            return testCount;
        }

        public long getCancelledBillCount() {
            return cancelledBillCount;
        }

        public long getRefundedTestCount() {
            return refundedTestCount;
        }

        public double getGross() {
            return gross;
        }

        public double getDiscount() {
            return discount;
        }

        public double getNet() {
            return net;
        }

        public double getCancelledNet() {
            return cancelledNet;
        }

        public double getRefundedNet() {
            return refundedNet;
        }

        public double getNetIncome() {
            return net - cancelledNet - refundedNet;
        }
    }

    // ------------------------------------------------------------------
    // Getters / setters
    // ------------------------------------------------------------------
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public String getVisitType() {
        return visitType;
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public List<DayGroup> getDays() {
        return days;
    }

    public Totals getGrandTotals() {
        return grandTotals;
    }

    public List<Map.Entry<String, Totals>> getPaymentMethodSummary() {
        return paymentMethodSummary;
    }

    public List<Map.Entry<String, Totals>> getVisitTypeSummary() {
        return visitTypeSummary;
    }
}
