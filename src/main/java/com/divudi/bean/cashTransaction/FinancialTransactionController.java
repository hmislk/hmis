package com.divudi.bean.cashTransaction;

import com.divudi.bean.channel.analytics.ReportTemplateController;
import java.util.HashMap;
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.BillSearch;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.entity.Bill;
import com.divudi.entity.Payment;
import com.divudi.facade.BillFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.AtomicBillTypeTotals;
import com.divudi.data.BillCategory;
import com.divudi.data.BillFinanceType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.CountedServiceType;
import com.divudi.data.Denomination;
import com.divudi.data.FinancialReport;
import com.divudi.data.PaymentMethod;
import com.divudi.data.PaymentMethodValues;
import com.divudi.data.ReportTemplateRow;
import com.divudi.data.ReportTemplateRowBundle;
import com.divudi.data.ServiceType;
import com.divudi.data.analytics.ReportTemplateType;
import com.divudi.data.dataStructure.PaymentMethodData;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.WebUser;
import com.divudi.java.CommonFunctions;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@SessionScoped
public class FinancialTransactionController implements Serializable {

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    BillFacade billFacade;
    @EJB
    PaymentFacade paymentFacade;
    // </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
    @Inject
    ReportTemplateController reportTemplateController;
    @Inject
    BillController billController;
    @Inject
    PaymentController paymentController;
    @Inject
    SearchController searchController;
    @Inject
    BillSearch billSearch;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    // </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private Bill currentBill;
    private ReportTemplateType reportTemplateType;
    private ReportTemplateRowBundle reportTemplateRowBundle;
    private ReportTemplateRowBundle opdServiceBundle;
    private ReportTemplateRowBundle channellingBundle;
    private ReportTemplateRowBundle opdDocPayment;
    private ReportTemplateRowBundle channellingDocPayment;

    private ReportTemplateRowBundle opdBilled;
    private ReportTemplateRowBundle opdReturns;
    private ReportTemplateRowBundle opdBundle;
    private ReportTemplateRowBundle channellingBilled;
    private ReportTemplateRowBundle channellingReturns;
    private ReportTemplateRowBundle pharmacyBilld;
    private ReportTemplateRowBundle pharmacyReturned;

    private ReportTemplateRowBundle channellingOnsite;
    private ReportTemplateRowBundle channellingAgent;
    private ReportTemplateRowBundle channellingOnline;
    private ReportTemplateRowBundle opdByDepartment;

    private List<ReportTemplateRowBundle> shiftEndBundles;

    private Payment currentPayment;
    private PaymentMethodData paymentMethodData;
    private Payment removingPayment;
    private List<Payment> currentBillPayments;
    private List<Bill> currentBills;
    private List<Bill> shiaftStartBills;
    private List<Bill> fundTransferBillsToReceive;
    private List<Bill> fundBillsForClosureBills;
    private Bill selectedBill;
    private Bill nonClosedShiftStartFundBill;
    private List<Payment> paymentsFromShiftSratToNow;
    private List<Payment> paymentsSelected;
    private List<Payment> recievedBIllPayments;
    private List<Bill> allBillsShiftStartToNow;
    @Deprecated
    private PaymentMethodValues paymentMethodValues;
    private AtomicBillTypeTotals atomicBillTypeTotalsByBills;
    private AtomicBillTypeTotals atomicBillTypeTotalsByPayments;
    private FinancialReport financialReportByBills;
    private FinancialReport financialReportByPayments;
    //Billed Totals
    private double totalOpdBillValue;
    private double totalPharmecyBillValue;
    private double totalChannelBillValue;
    private double totalCcBillValue;
    private double totalProfessionalPaymentBillValue;

    //Cancelled Totals
    private double totalOpdBillCanceledValue;
    private double totalPharmecyBillCanceledValue;
    private double totalChannelBillCancelledValue;
    private double totalCcBillCanceledValue;
    private double totalProfessionalPaymentBillCancelledValue;

    //Refund Totals
    private double totalOpdBillRefundValue;
    private double totalPharmacyBillRefundValue;
    private double totalChannelBillRefundValue;
    private double totalCcBillRefundValue;

    //Totals
    private double totalBillRefundValue;
    private double totalBillCancelledValue;
    private double totalBilledBillValue;

    private double totalShiftStartValue;
    private double totalBalanceTransfer;
    private double totalTransferRecive;

    private double totalFunds;
    private double shiftEndTotalValue;
    private double shiftEndRefundBillValue;
    private double shiftEndCanceledBillValue;
    private double totalWithdrawals;
    private double totalDeposits;

    private double Deductions;
    private double additions;

    private int fundTransferBillsToReceiveCount;
    private Date fromDate;
    private Date toDate;

    private ReportTemplateRowBundle paymentSummaryBundle;

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    public FinancialTransactionController() {
    }

    // </editor-fold> 
    // <editor-fold defaultstate="collapsed" desc="Navigational Methods">
    public String navigateToFinancialTransactionIndex() {
        resetClassVariables();
        fillFundTransferBillsForMeToReceive();
        return "/cashier/index?faces-redirect=true;";
    }

    public String navigateToCreateNewInitialFundBill() {
        resetClassVariables();
        prepareToAddNewInitialFundBill();
        return "/cashier/initial_fund_bill?faces-redirect=true;";
    }

    // Method to calculate duration between two Date objects
    public String calculateDuration(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return "N/A";
        }

        LocalDateTime startDateTime = convertToLocalDateTime(startDate);
        LocalDateTime endDateTime = convertToLocalDateTime(endDate);

        Duration duration = Duration.between(startDateTime, endDateTime);
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();

        return String.format("%d hours, %d minutes", hours, minutes);
    }

    // Helper method to convert Date to LocalDateTime
    private LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    public ReportTemplateRowBundle addChannellingByCategories(
            ReportTemplateType type,
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {

        ReportTemplateRowBundle bundle;
        List<BillTypeAtomic> inBts = BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.CHANNELLING, BillFinanceType.CASH_IN);
        List<BillTypeAtomic> outBts = BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.CHANNELLING, BillFinanceType.CASH_OUT);

        ReportTemplateRowBundle ins = reportTemplateController.generateReport(
                type,
                inBts,
                paramDate,
                paramFromDate,
                paramToDate,
                paramInstitution,
                paramDepartment,
                paramFromInstitution,
                paramFromDepartment,
                paramToInstitution,
                paramToDepartment,
                paramUser,
                paramCreditCompany,
                paramStartId,
                paramEndId);

        System.out.println("ins = " + ins.getReportTemplateRows().size());

        ReportTemplateRowBundle outs = reportTemplateController.generateReport(
                type,
                outBts,
                paramDate,
                paramFromDate,
                paramToDate,
                paramInstitution,
                paramDepartment,
                paramFromInstitution,
                paramFromDepartment,
                paramToInstitution,
                paramToDepartment,
                paramUser,
                paramCreditCompany,
                paramStartId,
                paramEndId);

        System.out.println("outes = " + outs.getReportTemplateRows().size());

        bundle = combineBundlesByCategory(ins, outs);

        return bundle;
    }

    public ReportTemplateRowBundle addProfessionalPayments(
            ReportTemplateType type,
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        System.out.println("addOpdByDepartments");

        ReportTemplateRowBundle bundle;
        List<BillTypeAtomic> inBts = BillTypeAtomic.findByCountedServiceType(CountedServiceType.CHANNELLING_PROFESSIONAL_PAYMENT);
        inBts.addAll(BillTypeAtomic.findByCountedServiceType(CountedServiceType.OPD_PROFESSIONAL_PAYMENT));
        List<BillTypeAtomic> outBts = BillTypeAtomic.findByCountedServiceType(CountedServiceType.CHANNELLING_PROFESSIONAL_PAYMENT_RETURN);
        outBts.addAll(BillTypeAtomic.findByCountedServiceType(CountedServiceType.OPD_PROFESSIONAL_PAYMENT_RETURN));

        ReportTemplateRowBundle ins = reportTemplateController.generateReport(
                type,
                inBts,
                paramDate,
                paramFromDate,
                paramToDate,
                paramInstitution,
                paramDepartment,
                paramFromInstitution,
                paramFromDepartment,
                paramToInstitution,
                paramToDepartment,
                paramUser,
                paramCreditCompany,
                paramStartId,
                paramEndId);

        System.out.println("ins = " + ins.getReportTemplateRows().size());

        ReportTemplateRowBundle outs = reportTemplateController.generateReport(
                type,
                outBts,
                paramDate,
                paramFromDate,
                paramToDate,
                paramInstitution,
                paramDepartment,
                paramFromInstitution,
                paramFromDepartment,
                paramToInstitution,
                paramToDepartment,
                paramUser,
                paramCreditCompany,
                paramStartId,
                paramEndId);

        System.out.println("outes = " + outs.getReportTemplateRows().size());

        bundle = combineBundlesByBillTypeAtomic(ins, outs);

        return bundle;
    }

    public ReportTemplateRowBundle addOpdByDepartments(
            ReportTemplateType type,
            List<BillTypeAtomic> btas,
            Date paramDate,
            Date paramFromDate,
            Date paramToDate,
            Institution paramInstitution,
            Department paramDepartment,
            Institution paramFromInstitution,
            Department paramFromDepartment,
            Institution paramToInstitution,
            Department paramToDepartment,
            WebUser paramUser,
            Institution paramCreditCompany,
            Long paramStartId,
            Long paramEndId) {
        System.out.println("addOpdByDepartments");

        ReportTemplateRowBundle bundle;
        List<BillTypeAtomic> inBts = BillTypeAtomic.findByCountedServiceType(CountedServiceType.OPD_IN);
        List<BillTypeAtomic> outBts = BillTypeAtomic.findByCountedServiceType(CountedServiceType.OPD_OUT);

        ReportTemplateRowBundle ins = reportTemplateController.generateReport(
                type,
                inBts,
                paramDate,
                paramFromDate,
                paramToDate,
                paramInstitution,
                paramDepartment,
                paramFromInstitution,
                paramFromDepartment,
                paramToInstitution,
                paramToDepartment,
                paramUser,
                paramCreditCompany,
                paramStartId,
                paramEndId);

        System.out.println("ins = " + ins.getReportTemplateRows().size());

        ReportTemplateRowBundle outs = reportTemplateController.generateReport(
                type,
                outBts,
                paramDate,
                paramFromDate,
                paramToDate,
                paramInstitution,
                paramDepartment,
                paramFromInstitution,
                paramFromDepartment,
                paramToInstitution,
                paramToDepartment,
                paramUser,
                paramCreditCompany,
                paramStartId,
                paramEndId);

        System.out.println("outes = " + outs.getReportTemplateRows().size());

        bundle = combineBundlesByItemDepartment(ins, outs);

        return bundle;
    }

    public void processShiftEndReport() {
        shiftEndBundles = new ArrayList<>();
        ReportTemplateType channelingType = ReportTemplateType.ITEM_CATEGORY_SUMMARY_BY_BILL;
        ReportTemplateType opdType = ReportTemplateType.ITEM_DEPARTMENT_SUMMARY_BY_BILL_ITEM;
        ReportTemplateType paymentsType = ReportTemplateType.BILL_TYPE_ATOMIC_SUMMARY_USING_BILLS;

        List<BillTypeAtomic> btas = null;
        Date paramDate = null;
        Date paramFromDate = null;
        Date paramToDate = null;
        Institution paramInstitution = null;
        Department paramDepartment = null;
        Institution paramFromInstitution = null;
        Department paramFromDepartment = null;
        Institution paramToInstitution = null;
        Department paramToDepartment = null;
        WebUser paramUser = nonClosedShiftStartFundBill.getCreater();
        Institution paramCreditCompany = null;
        Long paramStartId = nonClosedShiftStartFundBill.getId();
        Long paramEndId = nonClosedShiftStartFundBill.getReferenceBill().getId();

        ReportTemplateRowBundle tmpChannellingBundle = addChannellingByCategories(
                channelingType,
                btas,
                paramDate,
                paramFromDate,
                paramToDate,
                paramInstitution,
                paramDepartment,
                paramFromInstitution,
                paramFromDepartment,
                paramToInstitution,
                paramToDepartment,
                paramUser,
                paramCreditCompany,
                paramStartId,
                paramEndId
        );

        ReportTemplateRowBundle tmpOpdBundle
                = addOpdByDepartments(
                        opdType,
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId
                );

        ReportTemplateRowBundle tmpPaymentBundle
                = addProfessionalPayments(
                        paymentsType,
                        btas,
                        paramDate,
                        paramFromDate,
                        paramToDate,
                        paramInstitution,
                        paramDepartment,
                        paramFromInstitution,
                        paramFromDepartment,
                        paramToInstitution,
                        paramToDepartment,
                        paramUser,
                        paramCreditCompany,
                        paramStartId,
                        paramEndId
                );

        tmpChannellingBundle.setName("Channelling");
        tmpOpdBundle.setName("OPD");
        tmpPaymentBundle.setName("Payments");
        shiftEndBundles.add(tmpChannellingBundle);
        shiftEndBundles.add(tmpOpdBundle);
        shiftEndBundles.add(tmpPaymentBundle);

    }

    public String navigateToDayEndReport() {
        return "/cashier/day_end_report?faces-redirect=true;";
    }

    public void processDayEndReport() {
        List<BillTypeAtomic> channellingOnlineBooking = new ArrayList<>();
        channellingOnlineBooking.add(BillTypeAtomic.CHANNEL_BOOKING_WITH_PAYMENT_ONLINE);
        channellingOnline = reportTemplateController.generateReport(
                ReportTemplateType.BILL_NET_TOTAL,
                channellingOnlineBooking,
                null,
                fromDate,
                toDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        Institution agent = new Institution();
        channellingAgent = reportTemplateController.generateReport(
                ReportTemplateType.BILL_NET_TOTAL,
                BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.CHANNELLING, BillFinanceType.CASH_IN),
                null,
                fromDate,
                toDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        agent.setId(1l);
        channellingBilled = reportTemplateController.generateReport(
                ReportTemplateType.BILL_NET_TOTAL,
                BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.CHANNELLING, BillFinanceType.CASH_IN),
                null,
                fromDate,
                toDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        opdByDepartment = reportTemplateController.generateReport(
                ReportTemplateType.ITEM_DEPARTMENT_SUMMARY_BY_BILL_ITEM,
                BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.OPD, BillFinanceType.CASH_IN),
                null,
                fromDate,
                toDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        channellingReturns = reportTemplateController.generateReport(
                ReportTemplateType.BILL_NET_TOTAL,
                BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.CHANNELLING, BillFinanceType.CASH_OUT),
                null,
                fromDate,
                toDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        opdBilled = reportTemplateController.generateReport(
                ReportTemplateType.BILL_NET_TOTAL,
                BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.OPD, BillFinanceType.CASH_IN),
                null,
                fromDate,
                toDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        opdReturns = reportTemplateController.generateReport(
                ReportTemplateType.BILL_NET_TOTAL,
                BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.OPD, BillFinanceType.CASH_OUT),
                null,
                fromDate,
                toDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        channellingDocPayment = reportTemplateController.generateReport(
                ReportTemplateType.BILL_NET_TOTAL,
                BillTypeAtomic.findBillTypeAtomic(ServiceType.CHANNELLING, BillCategory.PAYMENTS),
                null,
                fromDate,
                toDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        opdDocPayment = reportTemplateController.generateReport(
                ReportTemplateType.BILL_NET_TOTAL,
                BillTypeAtomic.findBillTypeAtomic(ServiceType.OPD, BillCategory.PAYMENTS),
                null,
                fromDate,
                toDate,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }

    private ReportTemplateRowBundle combineBundlesByItem(ReportTemplateRowBundle inBundle, ReportTemplateRowBundle outBundle) {
        ReportTemplateRowBundle temOutBundle = new ReportTemplateRowBundle();
        Map<Item, ReportTemplateRow> combinedRows = new HashMap<>();

        if (inBundle != null && inBundle.getReportTemplateRows() != null) {
            for (ReportTemplateRow inRow : inBundle.getReportTemplateRows()) {
                if (inRow != null) {
                    Item item = inRow.getItem();
                    if (item != null) {
                        if (!combinedRows.containsKey(item)) {
                            combinedRows.put(item, new ReportTemplateRow(item));
                        }
                        ReportTemplateRow combinedRow = combinedRows.get(item);
                        combinedRow.setRowCountIn((combinedRow.getRowCountIn() != null ? combinedRow.getRowCountIn() : 0L) + (inRow.getRowCount() != null ? inRow.getRowCount() : 0L));
                        combinedRow.setRowValueIn((combinedRow.getRowValueIn() != null ? combinedRow.getRowValueIn() : 0.0) + (inRow.getRowValue() != null ? inRow.getRowValue() : 0.0));
                    }
                }
            }
        }

        if (outBundle != null && outBundle.getReportTemplateRows() != null) {
            for (ReportTemplateRow outRow : outBundle.getReportTemplateRows()) {
                if (outRow != null) {
                    Item item = outRow.getItem();
                    if (item != null) {
                        if (!combinedRows.containsKey(item)) {
                            combinedRows.put(item, new ReportTemplateRow(item));
                        }
                        ReportTemplateRow combinedRow = combinedRows.get(item);
                        combinedRow.setRowCountOut((combinedRow.getRowCountOut() != null ? combinedRow.getRowCountOut() : 0L) + (outRow.getRowCount() != null ? outRow.getRowCount() : 0L));
                        combinedRow.setRowValueOut((combinedRow.getRowValueOut() != null ? combinedRow.getRowValueOut() : 0.0) + (outRow.getRowValue() != null ? outRow.getRowValue() : 0.0));
                    }
                }
            }
        }

        long totalInCount = 0;
        long totalOutCount = 0;
        double totalInValue = 0.0;
        double totalOutValue = 0.0;

        for (ReportTemplateRow row : combinedRows.values()) {
            row.setRowCount((row.getRowCountIn() != null ? row.getRowCountIn() : 0L) + (row.getRowCountOut() != null ? row.getRowCountOut() : 0L));
            row.setRowValue((row.getRowValueIn() != null ? row.getRowValueIn() : 0.0) + (row.getRowValueOut() != null ? row.getRowValueOut() : 0.0));
            temOutBundle.getReportTemplateRows().add(row);

            totalInCount += (row.getRowCountIn() != null ? row.getRowCountIn() : 0L);
            totalOutCount += (row.getRowCountOut() != null ? row.getRowCountOut() : 0L);
            totalInValue += (row.getRowValueIn() != null ? row.getRowValueIn() : 0.0);
            totalOutValue += (row.getRowValueOut() != null ? row.getRowValueOut() : 0.0);
        }

        temOutBundle.setCountIn(totalInCount);
        temOutBundle.setCountOut(totalOutCount);
        temOutBundle.setCount(totalInCount + totalOutCount);
        temOutBundle.setTotalIn(totalInValue);
        temOutBundle.setTotalOut(totalOutValue);
        temOutBundle.setTotal(totalInValue + totalOutValue);

        return temOutBundle;
    }

    private ReportTemplateRowBundle combineBundlesByCategory(ReportTemplateRowBundle inBundle, ReportTemplateRowBundle outBundle) {
        ReportTemplateRowBundle temOutBundle = new ReportTemplateRowBundle();
        Map<Category, ReportTemplateRow> combinedRows = new HashMap<>();

        if (inBundle != null && inBundle.getReportTemplateRows() != null) {
            for (ReportTemplateRow inRow : inBundle.getReportTemplateRows()) {
                if (inRow != null) {
                    Category c = inRow.getCategory();
                    if (c != null) {
                        if (!combinedRows.containsKey(c)) {
                            combinedRows.put(c, new ReportTemplateRow(c));
                        }
                        ReportTemplateRow combinedRow = combinedRows.get(c);
                        combinedRow.setRowCountIn((combinedRow.getRowCountIn() != null ? combinedRow.getRowCountIn() : 0L) + (inRow.getRowCount() != null ? inRow.getRowCount() : 0L));
                        combinedRow.setRowValueIn((combinedRow.getRowValueIn() != null ? combinedRow.getRowValueIn() : 0.0) + (inRow.getRowValue() != null ? inRow.getRowValue() : 0.0));
                    }
                }
            }
        }

        if (outBundle != null && outBundle.getReportTemplateRows() != null) {
            for (ReportTemplateRow outRow : outBundle.getReportTemplateRows()) {
                if (outRow != null) {
                    Category c = outRow.getCategory();
                    if (c != null) {
                        if (!combinedRows.containsKey(c)) {
                            combinedRows.put(c, new ReportTemplateRow(c));
                        }
                        ReportTemplateRow combinedRow = combinedRows.get(c);
                        combinedRow.setRowCountOut((combinedRow.getRowCountOut() != null ? combinedRow.getRowCountOut() : 0L) + (outRow.getRowCount() != null ? outRow.getRowCount() : 0L));
                        combinedRow.setRowValueOut((combinedRow.getRowValueOut() != null ? combinedRow.getRowValueOut() : 0.0) + (outRow.getRowValue() != null ? outRow.getRowValue() : 0.0));
                    }
                }
            }
        }

        long totalInCount = 0;
        long totalOutCount = 0;
        double totalInValue = 0.0;
        double totalOutValue = 0.0;

        for (ReportTemplateRow row : combinedRows.values()) {
            row.setRowCount((row.getRowCountIn() != null ? row.getRowCountIn() : 0L) + (row.getRowCountOut() != null ? row.getRowCountOut() : 0L));
            row.setRowValue((row.getRowValueIn() != null ? row.getRowValueIn() : 0.0) + (row.getRowValueOut() != null ? row.getRowValueOut() : 0.0));
            temOutBundle.getReportTemplateRows().add(row);

            totalInCount += (row.getRowCountIn() != null ? row.getRowCountIn() : 0L);
            totalOutCount += (row.getRowCountOut() != null ? row.getRowCountOut() : 0L);
            totalInValue += (row.getRowValueIn() != null ? row.getRowValueIn() : 0.0);
            totalOutValue += (row.getRowValueOut() != null ? row.getRowValueOut() : 0.0);
        }

        temOutBundle.setCountIn(totalInCount);
        temOutBundle.setCountOut(totalOutCount);
        temOutBundle.setCount(totalInCount + totalOutCount);
        temOutBundle.setTotalIn(totalInValue);
        temOutBundle.setTotalOut(totalOutValue);
        temOutBundle.setTotal(totalInValue + totalOutValue);

        return temOutBundle;
    }

    private ReportTemplateRowBundle combineBundlesByItemDepartment(ReportTemplateRowBundle inBundle, ReportTemplateRowBundle outBundle) {
        System.out.println("Starting combineBundlesByDepartment method");

        ReportTemplateRowBundle temOutBundle = new ReportTemplateRowBundle();
        temOutBundle.setReportTemplateRows(new ArrayList<>());  // Ensure the list is initialized
        Map<Object, ReportTemplateRow> combinedRows = new HashMap<>();

        final Object NULL_DEPARTMENT_KEY = new Object();  // Placeholder for null departments

        if (inBundle != null && inBundle.getReportTemplateRows() != null) {
            System.out.println("Processing inBundle");
            for (ReportTemplateRow inRow : inBundle.getReportTemplateRows()) {
                if (inRow != null) {
                    Department d = inRow.getItemDepartment();
                    Object key = (d != null) ? d : NULL_DEPARTMENT_KEY;
                    if (d == null) {
                        System.out.println("Department is null in inRow");
                    } else {
                        System.out.println("Processing inRow for Department: " + d.getName());
                    }
                    if (!combinedRows.containsKey(key)) {
                        combinedRows.put(key, new ReportTemplateRow(d));
                    }
                    ReportTemplateRow combinedRow = combinedRows.get(key);
                    combinedRow.setRowCountIn((combinedRow.getRowCountIn() != null ? combinedRow.getRowCountIn() : 0L) + (inRow.getRowCount() != null ? inRow.getRowCount() : 0L));
                    combinedRow.setRowValueIn((combinedRow.getRowValueIn() != null ? combinedRow.getRowValueIn() : 0.0) + (inRow.getRowValue() != null ? inRow.getRowValue() : 0.0));
                } else {
                    System.out.println("inRow is null");
                }
            }
        } else {
            System.out.println("inBundle or inBundle.getReportTemplateRows() is null");
        }

        if (outBundle != null && outBundle.getReportTemplateRows() != null) {
            System.out.println("Processing outBundle");
            for (ReportTemplateRow outRow : outBundle.getReportTemplateRows()) {
                if (outRow != null) {
                    Department d = outRow.getItemDepartment();
                    Object key = (d != null) ? d : NULL_DEPARTMENT_KEY;
                    if (d == null) {
                        System.out.println("Department is null in outRow");
                    } else {
                        System.out.println("Processing outRow for Department: " + d.getName());
                    }
                    if (!combinedRows.containsKey(key)) {
                        combinedRows.put(key, new ReportTemplateRow(d));
                    }
                    ReportTemplateRow combinedRow = combinedRows.get(key);
                    combinedRow.setRowCountOut((combinedRow.getRowCountOut() != null ? combinedRow.getRowCountOut() : 0L) + (outRow.getRowCount() != null ? outRow.getRowCount() : 0L));
                    combinedRow.setRowValueOut((combinedRow.getRowValueOut() != null ? combinedRow.getRowValueOut() : 0.0) + (outRow.getRowValue() != null ? outRow.getRowValue() : 0.0));
                } else {
                    System.out.println("outRow is null");
                }
            }
        } else {
            System.out.println("outBundle or outBundle.getReportTemplateRows() is null");
        }

        long totalInCount = 0;
        long totalOutCount = 0;
        double totalInValue = 0.0;
        double totalOutValue = 0.0;

        for (ReportTemplateRow row : combinedRows.values()) {
            String deptName = (row.getItemDepartment() != null) ? row.getItemDepartment().getName() : "NULL_DEPARTMENT";
            System.out.println("Combining row for Department: " + deptName);
            row.setRowCount((row.getRowCountIn() != null ? row.getRowCountIn() : 0L) + (row.getRowCountOut() != null ? row.getRowCountOut() : 0L));
            row.setRowValue((row.getRowValueIn() != null ? row.getRowValueIn() : 0.0) + (row.getRowValueOut() != null ? row.getRowValueOut() : 0.0));
            temOutBundle.getReportTemplateRows().add(row);

            totalInCount += (row.getRowCountIn() != null ? row.getRowCountIn() : 0L);
            totalOutCount += (row.getRowCountOut() != null ? row.getRowCountOut() : 0L);
            totalInValue += (row.getRowValueIn() != null ? row.getRowValueIn() : 0.0);
            totalOutValue += (row.getRowValueOut() != null ? row.getRowValueOut() : 0.0);
        }

        temOutBundle.setCountIn(totalInCount);
        temOutBundle.setCountOut(totalOutCount);
        temOutBundle.setCount(totalInCount + totalOutCount);
        temOutBundle.setTotalIn(totalInValue);
        temOutBundle.setTotalOut(totalOutValue);
        temOutBundle.setTotal(totalInValue + totalOutValue);

        System.out.println("Total In Count: " + totalInCount);
        System.out.println("Total Out Count: " + totalOutCount);
        System.out.println("Total In Value: " + totalInValue);
        System.out.println("Total Out Value: " + totalOutValue);

        System.out.println("Finished combineBundlesByDepartment method");
        return temOutBundle;
    }

    private ReportTemplateRowBundle combineBundlesByBillTypeAtomic(ReportTemplateRowBundle inBundle, ReportTemplateRowBundle outBundle) {
        System.out.println("Starting combineBundlesByDepartment method");

        ReportTemplateRowBundle temOutBundle = new ReportTemplateRowBundle();
        temOutBundle.setReportTemplateRows(new ArrayList<>());  // Ensure the list is initialized
        Map<Object, ReportTemplateRow> combinedRows = new HashMap<>();

        final Object NULL_DEPARTMENT_KEY = new Object();  // Placeholder for null departments

        if (inBundle != null && inBundle.getReportTemplateRows() != null) {
            System.out.println("Processing inBundle");
            for (ReportTemplateRow inRow : inBundle.getReportTemplateRows()) {
                if (inRow != null) {
                    BillTypeAtomic d = inRow.getBillTypeAtomic();
                    Object key = (d != null) ? d : NULL_DEPARTMENT_KEY;
                    if (d == null) {
                        System.out.println("Department is null in inRow");
                    } else {
                        System.out.println("Processing inRow for Department: " + d.getLabel());
                    }
                    if (!combinedRows.containsKey(key)) {
                        combinedRows.put(key, new ReportTemplateRow(d));
                    }
                    ReportTemplateRow combinedRow = combinedRows.get(key);
                    combinedRow.setRowCountIn((combinedRow.getRowCountIn() != null ? combinedRow.getRowCountIn() : 0L) + (inRow.getRowCount() != null ? inRow.getRowCount() : 0L));
                    combinedRow.setRowValueIn((combinedRow.getRowValueIn() != null ? combinedRow.getRowValueIn() : 0.0) + (inRow.getRowValue() != null ? inRow.getRowValue() : 0.0));
                } else {
                    System.out.println("inRow is null");
                }
            }
        } else {
            System.out.println("inBundle or inBundle.getReportTemplateRows() is null");
        }

        if (outBundle != null && outBundle.getReportTemplateRows() != null) {
            System.out.println("Processing outBundle");
            for (ReportTemplateRow outRow : outBundle.getReportTemplateRows()) {
                if (outRow != null) {
                    BillTypeAtomic d = outRow.getBillTypeAtomic();
                    Object key = (d != null) ? d : NULL_DEPARTMENT_KEY;
                    if (d == null) {
                        System.out.println("Department is null in outRow");
                    } else {
                        System.out.println("Processing outRow for Department: " + d.getLabel());
                    }
                    if (!combinedRows.containsKey(key)) {
                        combinedRows.put(key, new ReportTemplateRow(d));
                    }
                    ReportTemplateRow combinedRow = combinedRows.get(key);
                    combinedRow.setRowCountOut((combinedRow.getRowCountOut() != null ? combinedRow.getRowCountOut() : 0L) + (outRow.getRowCount() != null ? outRow.getRowCount() : 0L));
                    combinedRow.setRowValueOut((combinedRow.getRowValueOut() != null ? combinedRow.getRowValueOut() : 0.0) + (outRow.getRowValue() != null ? outRow.getRowValue() : 0.0));
                } else {
                    System.out.println("outRow is null");
                }
            }
        } else {
            System.out.println("outBundle or outBundle.getReportTemplateRows() is null");
        }

        long totalInCount = 0;
        long totalOutCount = 0;
        double totalInValue = 0.0;
        double totalOutValue = 0.0;

        for (ReportTemplateRow row : combinedRows.values()) {
            String deptName = (row.getItemDepartment() != null) ? row.getItemDepartment().getName() : "NULL_DEPARTMENT";
            System.out.println("Combining row for Department: " + deptName);
            row.setRowCount((row.getRowCountIn() != null ? row.getRowCountIn() : 0L) + (row.getRowCountOut() != null ? row.getRowCountOut() : 0L));
            row.setRowValue((row.getRowValueIn() != null ? row.getRowValueIn() : 0.0) + (row.getRowValueOut() != null ? row.getRowValueOut() : 0.0));
            temOutBundle.getReportTemplateRows().add(row);

            totalInCount += (row.getRowCountIn() != null ? row.getRowCountIn() : 0L);
            totalOutCount += (row.getRowCountOut() != null ? row.getRowCountOut() : 0L);
            totalInValue += (row.getRowValueIn() != null ? row.getRowValueIn() : 0.0);
            totalOutValue += (row.getRowValueOut() != null ? row.getRowValueOut() : 0.0);
        }

        temOutBundle.setCountIn(totalInCount);
        temOutBundle.setCountOut(totalOutCount);
        temOutBundle.setCount(totalInCount + totalOutCount);
        temOutBundle.setTotalIn(totalInValue);
        temOutBundle.setTotalOut(totalOutValue);
        temOutBundle.setTotal(totalInValue + totalOutValue);

        System.out.println("Total In Count: " + totalInCount);
        System.out.println("Total Out Count: " + totalOutCount);
        System.out.println("Total In Value: " + totalInValue);
        System.out.println("Total Out Value: " + totalOutValue);

        System.out.println("Finished combineBundlesByDepartment method");
        return temOutBundle;
    }

    public void processShiftEndReportOpdCategory() {
        reportTemplateType = ReportTemplateType.ITEM_CATEGORY_SUMMARY_BY_BILL_ITEM;
        List<BillTypeAtomic> bts = new ArrayList<>();
        bts.addAll(BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.OPD, BillFinanceType.CASH_IN));
        bts.addAll(BillTypeAtomic.findByCountedServiceType(CountedServiceType.OPD_IN));
        opdBilled = reportTemplateController.generateReport(
                ReportTemplateType.ITEM_CATEGORY_SUMMARY_BY_BILL_ITEM,
                BillTypeAtomic.findByCountedServiceType(CountedServiceType.OPD_IN),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                nonClosedShiftStartFundBill.getCreater(),
                null,
                nonClosedShiftStartFundBill.getId(),
                nonClosedShiftStartFundBill.getReferenceBill().getId());
        opdReturns = reportTemplateController.generateReport(
                ReportTemplateType.ITEM_CATEGORY_SUMMARY_BY_BILL_ITEM,
                BillTypeAtomic.findByCountedServiceType(CountedServiceType.OPD_OUT),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                nonClosedShiftStartFundBill.getCreater(),
                null,
                nonClosedShiftStartFundBill.getId(),
                nonClosedShiftStartFundBill.getReferenceBill().getId());
        opdBundle = combineBundlesByCategory(opdBilled, opdReturns);
    }

    public void processShiftEndReportOpdItem() {
        reportTemplateType = ReportTemplateType.ITEM_SUMMARY_BY_BILL;
        List<BillTypeAtomic> bts = new ArrayList<>();
        bts.addAll(BillTypeAtomic.findByServiceTypeAndFinanceType(ServiceType.OPD, BillFinanceType.CASH_IN));
        bts.addAll(BillTypeAtomic.findByCountedServiceType(CountedServiceType.OPD_IN));
        opdBilled = reportTemplateController.generateReport(
                ReportTemplateType.ITEM_SUMMARY_BY_BILL,
                BillTypeAtomic.findByCountedServiceType(CountedServiceType.OPD_IN),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                nonClosedShiftStartFundBill.getCreater(),
                null,
                nonClosedShiftStartFundBill.getId(),
                nonClosedShiftStartFundBill.getReferenceBill().getId());
        opdReturns = reportTemplateController.generateReport(
                ReportTemplateType.ITEM_SUMMARY_BY_BILL,
                BillTypeAtomic.findByCountedServiceType(CountedServiceType.OPD_OUT),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                nonClosedShiftStartFundBill.getCreater(),
                null,
                nonClosedShiftStartFundBill.getId(),
                nonClosedShiftStartFundBill.getReferenceBill().getId());
        opdBundle = combineBundlesByItem(opdBilled, opdReturns);
    }

    public String navigateToListShiftEndSummaries() {
        resetClassVariables();
        return "/cashier/initial_fund_bill_list?faces-redirect=true;";
    }

    public void listShiftStartBills() {
        String jpql = "select b "
                + " from Bill b "
                + " where b.retired=:ret"
                + " and b.billTypeAtomic=:bta "
                + " and b.createdAt between :fd and :td "
                + " order by b.id ";
        Map params = new HashMap<>();
        params.put("ret", false);
        params.put("bta", BillTypeAtomic.FUND_SHIFT_START_BILL);
        params.put("fd", fromDate);
        params.put("td", toDate);
        shiaftStartBills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public String navigateToFundTransferBill() {
        resetClassVariables();
        prepareToAddNewFundTransferBill();
        return "/cashier/fund_transfer_bill?faces-redirect=true";
    }

    public String navigateToFundDepositBill() {
        resetClassVariables();
        prepareToAddNewFundDepositBill();
        return "/cashier/deposit_funds?faces-redirect=true";
    }

    public String navigateToCashierSummary() {
        return "/cashier/cashier_summary?faces-redirect=true";
    }

    public String navigateToCashierReport() {
        processShiftEndReport();
        return "/cashier/shift_end_report_bill_of_selected_user?faces-redirect=true";
    }

    public String navigateToCashierReportOpd() {
        resetBundles();
        return "/cashier/shift_end_report_bill_of_selected_user_opd?faces-redirect=true";
    }

    public String navigateToCashierSummaryBreakdown() {
        return "/cashier/shift_end_summary_breakdown?faces-redirect=true";
    }

    public String navigateToCashierSummaryBreakdownFromShiftClosingForCash() {
        List<PaymentMethod> pms = new ArrayList<>();
        pms.add(PaymentMethod.Cash);
        return navigateToCashierSummaryBreakdownFromShiftClosing(pms);
    }

    public String navigateToCashierSummaryBreakdownFromShiftClosingForCashAdmin() {
        List<PaymentMethod> pms = new ArrayList<>();
        pms.add(PaymentMethod.Cash);
        return navigateToCashierSummaryBreakdownFromShiftClosing(pms, nonClosedShiftStartFundBill.getCreater(), nonClosedShiftStartFundBill.getId(), nonClosedShiftStartFundBill.getReferenceBill().getId());
    }

    public void selectCashierSummaryBreakdownFromShiftClosingForCashAdmin() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForCollectedCash();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForCollectedCash();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectRefundedCashDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForRefundedCash();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForRefundedCash();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectNetCashTotalDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForNetCashTotal();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForNetCashTotal();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectCollectedCreditCardDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForCollectedCreditCard();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForCollectedCreditCard();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectRefundedCreditCardDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForRefundedCreditCard();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForRefundedCreditCard();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectNetCreditCardTotalDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForNetCreditCardTotal();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForNetCreditCardTotal();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectCollectedVoucherDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForCollectedVoucher();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForCollectedVoucher();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectRefundedVoucherDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForRefundedVoucher();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForRefundedVoucher();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectNetVoucherTotalDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForNetVoucherTotal();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForNetVoucherTotal();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectCollectedOtherNonCreditDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForCollectedDebitCard();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForCollectedDebitCard();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectRefundedOtherNonCreditDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForRefundedDebitCard();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForRefundedDebitCard();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectNetOtherNonCreditTotalDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForBankDeposits();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForBankDeposits();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectShiftStartFundsDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForFloatMySafe();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForFloatMySafe();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectFloatReceivedDetails() {
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForFloatCollected();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectFloatHandoverDetails() {
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForFloatHandover();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectNetFloatDetails() {

        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForFloatCollected();
        billTypes.addAll(financialReportByPayments.getBillTypesForFloatHandover());
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectBankWithdrawalsDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForBankWithdrawals();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForBankWithdrawals();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectBankDepositsDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForBankDeposits();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForBankDeposits();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectNetBankTransactionDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForBankWithdrawals();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForBankWithdrawals();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public void selectTotalDetails() {
        // Assuming this method should simply select all payments
        paymentsSelected = new ArrayList<>(paymentsFromShiftSratToNow);
    }

    public void selectShortExcessDetails() {
        List<PaymentMethod> paymentMethods = financialReportByPayments.getPaymentMethodsForShortExcess();
        List<BillTypeAtomic> billTypes = financialReportByPayments.getBillTypesForShortExcess();
        List<Payment> allPayments = paymentsFromShiftSratToNow;
        List<Payment> selectedPayments = new ArrayList<>();
        for (Payment payment : allPayments) {
            if (paymentMethods.contains(payment.getPaymentMethod())
                    && billTypes.contains(payment.getBill().getBillTypeAtomic())) {
                selectedPayments.add(payment);
            }
        }
        paymentsSelected = selectedPayments;
    }

    public String navigateToCashierSummaryBreakdownFromShiftClosingForCard() {
        List<PaymentMethod> pms = new ArrayList<>();
        pms.add(PaymentMethod.Card);
        return navigateToCashierSummaryBreakdownFromShiftClosing(pms);
    }

    public String navigateToCashierSummaryBreakdownFromShiftClosingForVoucher() {
        List<PaymentMethod> pms = new ArrayList<>();
        pms.add(PaymentMethod.Voucher);
        return navigateToCashierSummaryBreakdownFromShiftClosing(pms);
    }

    public String navigateToCashierSummaryBreakdownFromShiftClosingForAllExceptCashCardVaucher() {
        List<PaymentMethod> pms = PaymentMethod.asList();
        pms.remove(PaymentMethod.Voucher);
        pms.remove(PaymentMethod.Cash);
        pms.remove(PaymentMethod.Card);
        return navigateToCashierSummaryBreakdownFromShiftClosing(pms);
    }

    public String navigateToCashierSummaryBreakdownFromShiftClosing(List<PaymentMethod> pms) {
        navigateToCashierSummaryBreakdownFromShiftClosing(pms, sessionController.getLoggedUser(), nonClosedShiftStartFundBill.getId(), null);
        return "/cashier/shift_end_summary_breakdown?faces-redirect=true";
    }

    public String navigateToCashierSummaryBreakdownFromShiftClosing(List<PaymentMethod> pms, WebUser user, Long startId, Long endId) {
        searchController.setWebUser(user);
        searchController.setStartBillId(startId);
        searchController.setEndBillId(endId);
        searchController.setPaymentMethods(pms);
        searchController.processAllFinancialTransactionalSummarybySingleUserByIds();
        return "/analytics/shift_end_summary_breakdown?faces-redirect=true";
    }

    public String navigateToReceiveNewFundTransferBill() {
        if (selectedBill == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return "";
        }
        if (selectedBill.getBillType() != BillType.FundTransferBill) {
            JsfUtil.addErrorMessage("Wrong Bill Type");
            return "";
        }
        resetClassVariablesWithoutSelectedBill();
        prepareToAddNewFundTransferReceiveBill();
        return "/cashier/fund_transfer_receive_bill?faces-redirect=true";
    }

    public String navigateToReceiveFundTransferBillsForMe() {
        fillFundTransferBillsForMeToReceive();
        return "/cashier/fund_transfer_bills_for_me_to_receive?faces-redirect=true";
    }

    private void prepareToAddNewInitialFundBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.ShiftStartFundBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_START_BILL);
        currentBill.setBillClassType(BillClassType.Bill);
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
    }

    private void prepareToAddNewFundTransferBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.FundTransferBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_TRANSFER_BILL);
        currentBill.setBillClassType(BillClassType.Bill);
    }

    private void prepareToAddNewFundDepositBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.DepositFundBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_DEPOSIT_BILL);
        currentBill.setBillClassType(BillClassType.Bill);
    }

    private void prepareToAddNewFundTransferReceiveBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.FundTransferReceivedBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_TRANSFER_RECEIVED_BILL);

        currentBill.setBillClassType(BillClassType.Bill);
        currentBill.setReferenceBill(selectedBill);
        if (selectedBill != null) {
        }
        currentBillPayments = new ArrayList<>();
        if (selectedBill.getPayments() == null || selectedBill.getPayments().isEmpty()) {
            selectedBill.setPayments(findPaymentsForBill(selectedBill));
        }

        for (Payment p : selectedBill.getPayments()) {
            Payment np = p.copyAttributes();
            currentBillPayments.add(np);

        }
    }

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Functional Methods">
    public void resetClassVariables() {
        currentBill = null;
        currentPayment = null;
        removingPayment = null;
        currentBillPayments = null;
        fundTransferBillsToReceive = null;
        fundBillsForClosureBills = null;
        selectedBill = null;
        nonClosedShiftStartFundBill = null;
        paymentsFromShiftSratToNow = null;

    }

    public void resetClassVariablesWithoutSelectedBill() {
        currentBill = null;
        currentPayment = null;
        removingPayment = null;
        //currentBillPayments = null;
        fundTransferBillsToReceive = null;
        fundBillsForClosureBills = null;
        nonClosedShiftStartFundBill = null;
        paymentsFromShiftSratToNow = null;
    }

    public List<Payment> findPaymentsForBill(Bill b) {
        String jpql = "select p "
                + " from Payment p "
                + " where p.retired=:ret "
                + " and p.bill=:b"
                + " order by p.id";
        Map m = new HashMap();
        m.put("b", b);
        m.put("ret", false);
        return paymentFacade.findByJpql(jpql, m);
    }

    public void addPaymentToInitialFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.ShiftStartFundBill) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a Payment Method");
            return;
        }
        getCurrentBillPayments().add(currentPayment);
        calculateInitialFundBillTotal();
        currentPayment = null;
        getCurrentPayment();
        getCurrentPayment().setCurrencyDenominations(null);
        getCurrentPayment().setCurrencyDenominationsJson("");
    }

    public void addPaymentToFundTransferBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.FundTransferBill) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a Payment Method");
            return;
        }
        getCurrentBillPayments().add(currentPayment);
        calculateFundTransferBillTotal();
        currentPayment = null;
    }

    public void addPaymentToShiftEndFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.ShiftEndFundBill) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a Payment Method");
            return;
        }
        getCurrentBillPayments().add(currentPayment);
        calculateShiftEndFundBillTotal();
        currentPayment = null;
    }

    public void addPaymentToWithdrawalFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.WithdrawalFundBill) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a Payment Method");
            return;
        }
        getCurrentBillPayments().add(currentPayment);
        calculateWithdrawalFundBillTotal();
        currentPayment = null;
    }

    public void removePayment() {
        getCurrentBillPayments().remove(removingPayment);
        calculateInitialFundBillTotal();
        currentPayment = null;
    }

    private void calculateInitialFundBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    private void calculateFundTransferBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    private void calculateShiftEndFundBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    private void calculateWithdrawalFundBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    public String settleInitialFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.ShiftStartFundBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        findNonClosedShiftStartFundBillIsAvailable();
        if (nonClosedShiftStartFundBill != null) {
            JsfUtil.addErrorMessage("A shift start fund bill is already available for closure.");
            return "";
        }

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            // Serialize denominations before saving
            p.serializeDenominations();
            paymentController.save(p);
        }
        return "/cashier/initial_fund_bill_print?faces-redirect=true";
    }

    public String settleFundTransferBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.FundTransferBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getToStaff() == null) {
            JsfUtil.addErrorMessage("Select to whom to transfer");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setFromStaff(sessionController.getLoggedUser().getStaff());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        currentBill.getPayments().addAll(currentBillPayments);
        billController.save(currentBill);
        return "/cashier/fund_transfer_bill_print?faces-redirect=true";
    }

    public String settleWithdrawalFundBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.WithdrawalFundBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        return "/cashier/fund_withdrawal_bill_print?faces-redirect=true";
    }

    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="Sample Code Block">
    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="ShiftEndFundBill">
    public void refreshCashNetTotalForMenu() {
        findNonClosedShiftStartFundBillIsAvailable();
        fillPaymentsFromShiftStartToNow();
    }

    public String navigateToCreateShiftEndSummaryBill() {
        resetClassVariables();
        findNonClosedShiftStartFundBillIsAvailable();
        fillPaymentsFromShiftStartToNow();
        if (nonClosedShiftStartFundBill != null) {
            currentBill = new Bill();
            currentBill.setBillType(BillType.ShiftEndFundBill);
            currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_END_BILL);
            currentBill.setBillClassType(BillClassType.Bill);
            currentBill.setReferenceBill(nonClosedShiftStartFundBill);
        } else {
            currentBill = null;
        }
        return "/cashier/shift_end_summery_bill?faces-redirect=true";
    }

    public String navigateToDayEndSummary() {
        return "/analytics/day_end_summery?faces-redirect=true";
    }

    public void processDayEndSummary() {
        resetClassVariables();
        fillPaymentsForDateRange();
        createPaymentSummery();

    }

    private void createPaymentSummery() {
        System.out.println("createPaymentSummery");

        if (paymentsFromShiftSratToNow == null) {
            return;
        }
        paymentSummaryBundle = new ReportTemplateRowBundle();
        Map<String, Double> aggregatedPayments = new HashMap<>();
        Map<String, ReportTemplateRow> keyMap = new HashMap<>();

        for (Payment p : paymentsFromShiftSratToNow) {

            if (p == null || p.getBill() == null) {
                continue; // Skip this iteration if p or p.getBill() is null
            }

            ReportTemplateRow row = new ReportTemplateRow();

            if (p.getBill().getCategory() != null) {
                row.setCategory(p.getBill().getCategory());
            }

            if (p.getBill().getBillTypeAtomic() != null) {
                row.setBillTypeAtomic(p.getBill().getBillTypeAtomic());

                if (p.getBill().getBillTypeAtomic().getServiceType() != null) {
                    row.setServiceType(p.getBill().getBillTypeAtomic().getServiceType());
                }
            }

            if (p.getBill().getCreditCompany() != null) {
                row.setCreditCompany(p.getBill().getCreditCompany());
            }

            if (p.getBill().getToDepartment() != null) {
                row.setToDepartment(p.getBill().getToDepartment());
            }

            row.setRowValue(p.getPaidValue());

            String keyString = row.getCustomKey();

            if (keyString != null) {
                keyMap.putIfAbsent(keyString, row);
                aggregatedPayments.merge(keyString, p.getPaidValue(), Double::sum);
            }

        }

        List<ReportTemplateRow> rows = aggregatedPayments.entrySet().stream().map(entry -> {
            ReportTemplateRow row = keyMap.get(entry.getKey());

            if (row != null) {
                row.setRowValue(entry.getValue());
            }

            return row;
        }).collect(Collectors.toList());

        getPaymentSummaryBundle().getReportTemplateRows().addAll(rows);

    }

    public String navigateToViewShiftSratToNow(Bill startBill) {
        resetClassVariables();
        if (startBill == null) {
            JsfUtil.addErrorMessage("No Start Bill");
            return null;
        }
        if (startBill.getCreater() == null) {
            JsfUtil.addErrorMessage("No User");
            return null;
        }
        nonClosedShiftStartFundBill = startBill;
        fillPaymentsFromShiftStartToNow(startBill, startBill.getCreater());
        return "/cashier/shift_end_summery_bill_of_selected_user?faces-redirect=true";
    }

    public String navigateToViewShiftSrartToEnd(Bill startBill) {
        resetClassVariables();
        if (startBill == null) {
            JsfUtil.addErrorMessage("No Start Bill");
            return null;
        }
        if (startBill.getCreater() == null) {
            JsfUtil.addErrorMessage("No User");
            return null;
        }
        Bill endBill;
        if (startBill.getReferenceBill() == null) {
            JsfUtil.addErrorMessage("No Start Bill");
            return null;
        }
        endBill = startBill.getReferenceBill();
        nonClosedShiftStartFundBill = startBill;
        fillPaymentsFromShiftStartToEnd(startBill, endBill, startBill.getCreater());
        return "/cashier/shift_end_summery_bill_of_selected_user?faces-redirect=true";
    }

    public String navigateToCreateShiftEndSummaryBillByBills() {
        resetClassVariables();
        findNonClosedShiftStartFundBillIsAvailable();
        fillBillsFromShiftStartToNow();
        fillPaymentsFromShiftStartToNow();
        if (nonClosedShiftStartFundBill != null) {
            currentBill = new Bill();
            currentBill.setBillType(BillType.ShiftEndFundBill);
            currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_END_BILL);
            currentBill.setBillClassType(BillClassType.Bill);
            currentBill.setReferenceBill(nonClosedShiftStartFundBill);
        } else {
            currentBill = null;
        }
        return "/cashier/shift_end_summery_bill_by_bills?faces-redirect=true";
    }

    public void fillPaymentsFromShiftStartToNow() {
        paymentsFromShiftSratToNow = new ArrayList<>();
        if (nonClosedShiftStartFundBill == null) {
            return;
        }
        Long shiftStartBillId = nonClosedShiftStartFundBill.getId();
        String jpql = "SELECT p "
                + "FROM Payment p "
                + "WHERE p.creater = :cr "
                + "AND p.retired = :ret "
                + "AND p.id > :cid "
                + "ORDER BY p.id DESC";
        Map<String, Object> m = new HashMap<>();
        m.put("cr", nonClosedShiftStartFundBill.getCreater());
        m.put("ret", false);
        m.put("cid", nonClosedShiftStartFundBill.getId());
        paymentsFromShiftSratToNow = paymentFacade.findByJpql(jpql, m);
//        paymentMethodValues = new PaymentMethodValues(PaymentMethod.values());
        atomicBillTypeTotalsByPayments = new AtomicBillTypeTotals();
        for (Payment p : paymentsFromShiftSratToNow) {
            if (p.getBill().getBillTypeAtomic() == null) {
            } else {
                atomicBillTypeTotalsByPayments.addOrUpdateAtomicRecord(p.getBill().getBillTypeAtomic(), p.getPaymentMethod(), p.getPaidValue());
            }
            //            calculateBillValuesFromBillTypes(p);
        }
//        calculateTotalFundsFromShiftStartToNow();
        financialReportByPayments = new FinancialReport(atomicBillTypeTotalsByPayments);

    }

    public void fillPaymentsForDateRange() {
        System.out.println("fillPaymentsForDateRange");
        paymentsFromShiftSratToNow = new ArrayList<>();
        String jpql = "SELECT p "
                + "FROM Payment p "
                + "WHERE p.bill.retired <> :ret "
                + "AND p.bill.createdAt between :fd and :td "
                + "ORDER BY p.id DESC";
        Map<String, Object> m = new HashMap<>();
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ret", true);
        System.out.println("m = " + m);
    }

    public void fillPaymentsFromShiftStartToNow(Bill startBill, WebUser user) {
        paymentsFromShiftSratToNow = new ArrayList<>();
        if (startBill == null) {
            JsfUtil.addErrorMessage("No Start Bill");
            return;
        }
        if (user == null) {
            JsfUtil.addErrorMessage("No User");
            return;
        }
        Long shiftStartBillId = startBill.getId();
        String jpql = "SELECT p "
                + "FROM Payment p "
                + "WHERE p.creater = :cr "
                + "AND p.retired = :ret "
                + "AND p.id > :cid "
                + "ORDER BY p.id DESC";
        Map<String, Object> m = new HashMap<>();
        m.put("cr", user);
        m.put("ret", false);
        m.put("cid", shiftStartBillId);
        paymentsFromShiftSratToNow = paymentFacade.findByJpql(jpql, m);
        atomicBillTypeTotalsByPayments = new AtomicBillTypeTotals();
        for (Payment p : paymentsFromShiftSratToNow) {
            if (p.getBill().getBillTypeAtomic() == null) {
            } else {
                atomicBillTypeTotalsByPayments.addOrUpdateAtomicRecord(p.getBill().getBillTypeAtomic(), p.getPaymentMethod(), p.getPaidValue());
            }
        }
        financialReportByPayments = new FinancialReport(atomicBillTypeTotalsByPayments);
    }

    public void fillPaymentsFromShiftStartToEnd(Bill startBill, Bill endBill, WebUser user) {
        paymentsFromShiftSratToNow = new ArrayList<>();
        if (startBill == null) {
            JsfUtil.addErrorMessage("No Start Bill");
            return;
        }
        if (user == null) {
            JsfUtil.addErrorMessage("No User");
            return;
        }
        Long shiftStartBillId = startBill.getId();
        Long shiftEndBillId = endBill.getId();
        String jpql = "SELECT p "
                + "FROM Payment p "
                + "WHERE p.creater = :cr "
                + "AND p.retired = :ret "
                + "AND p.id > :sid "
                + "AND p.id < :eid "
                + "ORDER BY p.id DESC";
        Map<String, Object> m = new HashMap<>();
        m.put("cr", user);
        m.put("ret", false);
        m.put("sid", shiftStartBillId);
        m.put("eid", shiftEndBillId);

        System.out.println("m = " + m);
        System.out.println("jpql = " + jpql);

        paymentsFromShiftSratToNow = paymentFacade.findByJpql(jpql, m);
        System.out.println("paymentsFromShiftSratToNow = " + paymentsFromShiftSratToNow);
        atomicBillTypeTotalsByPayments = new AtomicBillTypeTotals();
        for (Payment p : paymentsFromShiftSratToNow) {
            if (p.getBill().getBillTypeAtomic() == null) {
            } else {
                atomicBillTypeTotalsByPayments.addOrUpdateAtomicRecord(p.getBill().getBillTypeAtomic(), p.getPaymentMethod(), p.getPaidValue());
            }
        }
        financialReportByPayments = new FinancialReport(atomicBillTypeTotalsByPayments);
    }

    public void fillBillsFromShiftStartToNow() {
        currentBills = new ArrayList<>();
        if (nonClosedShiftStartFundBill == null) {
            return;
        }
        List<BillTypeAtomic> billTypesToFilter = new ArrayList<>();
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT_REVERSE));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_DECREASE));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_INCREASE));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_STARTING_BALANCE));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_CLOSING_BALANCE));

        Long shiftStartBillId = nonClosedShiftStartFundBill.getId();
        String jpql = "SELECT p "
                + "FROM Bill p "
                + "WHERE p.creater = :cr "
                + "AND p.retired = :ret "
                + "AND p.billTypeAtomic in :btas "
                + "AND p.id > :cid "
                + "ORDER BY p.id DESC";
        Map<String, Object> m = new HashMap<>();
        m.put("cr", nonClosedShiftStartFundBill.getCreater());
        m.put("btas", billTypesToFilter);
        m.put("ret", false);
        m.put("cid", shiftStartBillId);
        currentBills = billFacade.findByJpql(jpql, m);
//        paymentMethodValues = new PaymentMethodValues(PaymentMethod.values());
        atomicBillTypeTotalsByBills = new AtomicBillTypeTotals();
        for (Bill p : currentBills) {
            if (p.getBillTypeAtomic() == null) {
            } else {
            }
            atomicBillTypeTotalsByBills.addOrUpdateAtomicRecord(p.getBillTypeAtomic(), p.getPaymentMethod(), p.getNetTotal());
//            calculateBillValuesFromBillTypes(p);
        }
        financialReportByBills = new FinancialReport(atomicBillTypeTotalsByBills);
        nonClosedShiftStartFundBill.setTotal(financialReportByBills.getTotal());
        nonClosedShiftStartFundBill.setNetTotal(financialReportByBills.getTotal());
    }

//    public void calculateBillValuesFromBillTypes(Payment p) {
//        if (p.getBill() == null) {
//            return;
//        }
//        if (p.getBill().getBillType() == null) {
//            return;
//        }
//        if (p.getBill().getBillTypeAtomic() == null) {
//            return;
//        }
//
//        switch (p.getBill().getBillTypeAtomic().getBillCategory()) {
//            case BILL:
//                if (p.getPaidValue() != 0.0) {
//                    paymentMethodValues.addValue(p);
//                } else {
//                    paymentMethodValues.addValue(p.getBill());
//                }
//                break;
//            case CANCELLATION:
//            case REFUND:
//                if (p.getPaidValue() != 0.0) {
//                    paymentMethodValues.deductAbsoluteValue(p);
//                } else {
//                    paymentMethodValues.deductAbsoluteValue(p.getBill());
//                }
//                break;
//            default:
//                break;
//
//        }
//    }
    public void calculateBillValuesFromBillTypes(Bill p) {
        if (p == null) {
            return;
        }
        if (p.getBillType() == null) {
            return;
        }
        if (p.getBillTypeAtomic() == null) {
            return;
        }

        paymentMethodValues.addValue(p);

//        switch (p.getBillTypeAtomic().getBillCategory()) {
//            case BILL:
//                if (p.getNetTotal() != 0.0) {
//                    paymentMethodValues.addValue(p);
//                } else {
//                    paymentMethodValues.addValue(p);
//                }
//                break;
//            case CANCELLATION:
//            case REFUND:
//                if (p.getNetTotal() != 0.0) {
//                    paymentMethodValues.deductAbsoluteValue(p);
//                } else {
//                    paymentMethodValues.deductAbsoluteValue(p);
//                }
//                break;
//            default:
//                break;
//
//        }
    }

    public void calculateTotalFundsFromShiftStartToNow() {
        //System.out.println("additions");
        //System.out.println("financialReportByPayments.getBankWithdrawals() = " + financialReportByPayments.getBankWithdrawals());
        //System.out.println("financialReportByPayments.getCashTotal() = " + financialReportByPayments.getCashTotal());
        //System.out.println("financialReportByPayments.getNetCreditCardTotal() = " + financialReportByPayments.getNetCreditCardTotal());
        //System.out.println("financialReportByPayments.getCollectedVoucher() = " + financialReportByPayments.getCollectedVoucher());
        //System.out.println("financialReportByPayments.getNetOtherNonCreditTotal() = " + financialReportByPayments.getNetOtherNonCreditTotal());
        //System.out.println("financialReportByPayments.getBankWithdrawals() = " + financialReportByPayments.getFloatReceived());

        //additions = financialReportByPayments.getCashTotal() + financialReportByPayments.getNetCreditCardTotal() + financialReportByPayments.getCollectedVoucher() + financialReportByPayments.getNetOtherNonCreditTotal() + financialReportByPayments.getBankWithdrawals();
        //Deductions = financialReportByPayments.getRefundedCash() + financialReportByPayments.getRefundedCreditCard() + financialReportByPayments.getRefundedVoucher() + financialReportByPayments.getRefundedOtherNonCredit() + financialReportByPayments.getFloatHandover() + financialReportByPayments.getBankDeposits();
        additions = financialReportByPayments.getBankWithdrawals() + financialReportByPayments.getCashTotal() + financialReportByPayments.getNetCreditCardTotal() + financialReportByPayments.getCollectedVoucher() + financialReportByPayments.getNetOtherNonCreditTotal() + financialReportByPayments.getFloatReceived();
        Deductions = financialReportByPayments.getFloatHandover() + financialReportByPayments.getBankDeposits();

        //System.out.println("\n\nDeductions");
        //System.out.println("financialReportByPayments.getFloatHandover() = " + financialReportByPayments.getFloatHandover());
        //System.out.println("financialReportByPayments.getBankDeposits() = " + financialReportByPayments.getBankDeposits());
        totalFunds = additions - Deductions;
        shiftEndTotalValue = totalFunds;

    }

    public void resetTotalFundsValues() {
        totalOpdBillValue = 0.0;
        totalPharmecyBillValue = 0.0;
        totalChannelBillValue = 0.0;
        totalCcBillValue = 0.0;
        totalProfessionalPaymentBillValue = 0.0;

        totalOpdBillCanceledValue = 0.0;
        totalPharmecyBillCanceledValue = 0.0;
        totalChannelBillCancelledValue = 0.0;
        totalCcBillCanceledValue = 0.0;
        totalProfessionalPaymentBillCancelledValue = 0.0;

        totalOpdBillRefundValue = 0.0;
        totalPharmacyBillRefundValue = 0.0;
        totalChannelBillRefundValue = 0.0;
        totalCcBillRefundValue = 0.0;

        totalBillRefundValue = 0.0;
        totalBillCancelledValue = 0.0;
        totalBilledBillValue = 0.0;

        totalShiftStartValue = 0.0;
        totalBalanceTransfer = 0.0;
        totalTransferRecive = 0.0;

        totalFunds = 0.0;
        shiftEndTotalValue = 0.0;
        shiftEndRefundBillValue = 0.0;
        shiftEndCanceledBillValue = 0.0;
        totalWithdrawals = 0.0;
        totalDeposits = 0.0;

        Deductions = 0.0;
        additions = 0.0;

        fundTransferBillsToReceiveCount = 0;
    }

    public void findNonClosedShiftStartFundBillIsAvailable() {
        nonClosedShiftStartFundBill = null;
        String jpql = "select b "
                + " from Bill b "
                + " where b.staff=:staff "
                + " and b.retired=:ret "
                + " and b.billType=:ofb "
                + " and b.referenceBill is null";
        Map m = new HashMap();
        m.put("staff", sessionController.getLoggedUser().getStaff());
        m.put("ret", false);
        m.put("ofb", BillType.ShiftStartFundBill);
        nonClosedShiftStartFundBill = billFacade.findFirstByJpql(jpql, m);
    }

    public void listBillsFromInitialFundBillUpToNow() {
        List<Bill> shiftStartFundBill;
        String jpql = "select b "
                + " from Bill b "
                + " where b.staff=:staff "
                + " and b.retired=:ret "
                + " and b.billType=:ofb "
                + " and b.referenceBill is null";
        Map m = new HashMap();
        m.put("staff", sessionController.getLoggedUser().getStaff());
        m.put("ret", false);
        m.put("ofb", BillType.ShiftStartFundBill);
        shiftStartFundBill = billFacade.findByJpql(jpql, m);

    }

    public String settleShiftEndFundBill() {
        boolean fundTransferBillTocollect = false;

        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.ShiftEndFundBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getReferenceBill() == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (fundTransferBillsToReceive != null && !fundTransferBillsToReceive.isEmpty()) {
            fundTransferBillTocollect = true;
        }

        if (fundTransferBillTocollect) {
            JsfUtil.addErrorMessage("Please collect funds transferred to you before closing.");
            return "";
        }

        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());
        billController.save(currentBill);
        currentBill.setTotal(financialReportByPayments.getTotal());
        currentBill.setNetTotal(financialReportByPayments.getTotal());
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        calculateTotalFundsFromShiftStartToNow();
        nonClosedShiftStartFundBill.setReferenceBill(currentBill);
        billController.save(nonClosedShiftStartFundBill);

        return "/cashier/shift_end_summery_bill_print?faces-redirect=true";
    }

// </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="BalanceTransferFundBill">
    /**
     *
     * User click to Crete Transfer Bill Add (fromStaff 0 the user) Select User
     * to transfer (toStaff) settle to print
     *
     */
    // </editor-fold>   
    // <editor-fold defaultstate="collapsed" desc="BalanceTransferReceiveFundBill">
    /**
     *
     * pavan
     *
     * Another User create a BalanceTransferFundBill It has a toStaff attribute
     * loggedUser.getStaff =toStaff List such bills Click on one of them Copy
     * Payments from BalanceTransferFundBill User may change them settle to
     * print
     *
     * @return
     */
    public void fillFundTransferBillsForMeToReceive() {
        String sql;
        fundTransferBillsToReceive = null;
        Map tempMap = new HashMap();
        sql = "select s "
                + "from Bill s "
                + "where s.retired=:ret "
                + "and s.billType=:btype "
                + "and s.toStaff=:logStaff "
                + "and s.referenceBill is null "
                + "order by s.createdAt ";
        tempMap.put("btype", BillType.FundTransferBill);
        tempMap.put("ret", false);
        tempMap.put("logStaff", sessionController.getLoggedUser().getStaff());
        fundTransferBillsToReceive = billFacade.findByJpql(sql, tempMap);
        fundTransferBillsToReceiveCount = fundTransferBillsToReceive.size();

    }

    public String settleFundTransferReceiveBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }

        if (currentBill.getReferenceBill() == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }

        if (currentBill.getBillType() != BillType.FundTransferReceivedBill) {
            JsfUtil.addErrorMessage("Error - bill type");
            return "";
        }

        if (currentBill.getReferenceBill().getBillType() != BillType.FundTransferBill) {
            JsfUtil.addErrorMessage("Error - Reference bill type");
            return "";
        }

        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setToStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setFromStaff(currentBill.getReferenceBill().getFromStaff());
        billController.save(currentBill);
        for (Payment p : currentBillPayments) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        currentBill.getReferenceBill().setReferenceBill(currentBill);
        billController.save(currentBill.getReferenceBill());

        return "/cashier/fund_transfer_receive_bill_print?faces-redirect=true";
    }

    // </editor-fold>      
    // <editor-fold defaultstate="collapsed" desc="DepositFundBill">
    //Lawan
    public void addPaymentToFundDepositBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillType() != BillType.DepositFundBill) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentPayment.getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Select a Payment Method");
            return;
        }
        getCurrentBillPayments().add(currentPayment);
        calculateFundDepositBillTotal();
        currentPayment = null;
    }

    private void calculateFundDepositBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    public String settleFundDepositBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.DepositFundBill) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            paymentController.save(p);
        }
        return "/cashier/deposit_funds_print?faces-redirect=true";
    }
    // </editor-fold>  
    // <editor-fold defaultstate="collapsed" desc="WithdrawalFundBill">

    public String navigateToCreateNewFundWithdrawalBill() {
        prepareToAddNewWithdrawalProcessingBill();
        return "/cashier/fund_withdrawal_bill?faces-redirect=true;";
    }

    private void prepareToAddNewWithdrawalProcessingBill() {
        currentBillPayments = null;
        currentBill = new Bill();
        currentBill.setBillType(BillType.WithdrawalFundBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_WITHDRAWAL_BILL);
        currentBill.setBillClassType(BillClassType.Bill);
    }

    //Damith
    // </editor-fold>      
    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public Bill getCurrentBill() {
        return currentBill;
    }

    public void setCurrentBill(Bill currentBill) {
        this.currentBill = currentBill;
    }

    public Payment getCurrentPayment() {
        if (currentPayment == null) {
            currentPayment = new Payment();
        }
        if (currentPayment.getCurrencyDenominations() == null) {
            currentPayment.setCurrencyDenominations(configOptionApplicationController.getDenominations());
        }
        return currentPayment;
    }

    public PaymentMethodData getPaymentMethodData() {
        if (paymentMethodData == null) {
            paymentMethodData = new PaymentMethodData();
        }
        return paymentMethodData;
    }

    public void setPaymentMethodData(PaymentMethodData paymentMethodData) {
        this.paymentMethodData = paymentMethodData;
    }

    public void updateCashDenominations(AjaxBehaviorEvent event) {
        System.out.println("updateCashDenominations called");

        if (currentPayment == null) {
            return;
        }

        double total = 0;
        List<Denomination> denominations = currentPayment.getCurrencyDenominations();
        for (Denomination denomination : denominations) {
            int value = denomination.getCount();
            total += denomination.getValue() * value;
        }
        currentPayment.setPaidValue(total);
        System.out.println("Total value calculated: " + total);

        // Serialize updated denominations to JSON
        JSONArray jsonArray = new JSONArray();
        for (Denomination denomination : denominations) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("value", denomination.getValue());
            jsonObject.put("count", denomination.getCount());
            jsonArray.put(jsonObject);
        }
        currentPayment.setCurrencyDenominationsJson(jsonArray.toString());
    }

    public void setCurrentPayment(Payment currentPayment) {
        this.currentPayment = currentPayment;
    }

    public List<Payment> getCurrentBillPayments() {
        if (currentBillPayments == null) {
            currentBillPayments = new ArrayList<>();
        }
        return currentBillPayments;
    }

    public void setCurrentBillPayments(List<Payment> currentBillPayments) {
        this.currentBillPayments = currentBillPayments;
    }

    public Payment getRemovingPayment() {
        return removingPayment;
    }

    public void setRemovingPayment(Payment removingPayment) {
        this.removingPayment = removingPayment;
    }

    public List<Bill> getFundTransferBillsToReceive() {
        return fundTransferBillsToReceive;
    }

    public void setFundTransferBillsToReceive(List<Bill> fundTransferBillsToReceive) {
        this.fundTransferBillsToReceive = fundTransferBillsToReceive;
    }

    public Bill getSelectedBill() {
        return selectedBill;
    }

    public void setSelectedBill(Bill selectedBill) {
        this.selectedBill = selectedBill;
    }

    // </editor-fold>  
    public List<Bill> getFundBillsForClosureBills() {
        return fundBillsForClosureBills;
    }

    public void setFundBillsForClosureBills(List<Bill> fundBillsForClosureBills) {
        this.fundBillsForClosureBills = fundBillsForClosureBills;
    }

    public Bill getNonClosedShiftStartFundBill() {
        return nonClosedShiftStartFundBill;
    }

    public void setNonClosedShiftStartFundBill(Bill nonClosedShiftStartFundBill) {
        this.nonClosedShiftStartFundBill = nonClosedShiftStartFundBill;
    }

    public List<Payment> getPaymentsFromShiftSratToNow() {
        if (paymentsFromShiftSratToNow == null) {
            paymentsFromShiftSratToNow = new ArrayList<>();
        }
        return paymentsFromShiftSratToNow;
    }

    public void setPaymentsFromShiftSratToNow(List<Payment> paymentsFromShiftSratToNow) {
        this.paymentsFromShiftSratToNow = paymentsFromShiftSratToNow;
    }

    public List<Bill> getAllBillsShiftStartToNow() {
        if (allBillsShiftStartToNow == null) {
            allBillsShiftStartToNow = new ArrayList<>();
        }
        return allBillsShiftStartToNow;
    }

    public void setAllBillsShiftStartToNow(List<Bill> allBillsShiftStartToNow) {
        this.allBillsShiftStartToNow = allBillsShiftStartToNow;
    }

    public double getTotalOpdBillValue() {
        return totalOpdBillValue;
    }

    public void setTotalOpdBillValue(double totalOpdBillValue) {
        this.totalOpdBillValue = totalOpdBillValue;
    }

    public double getTotalPharmecyBillValue() {
        return totalPharmecyBillValue;
    }

    public void setTotalPharmecyBillValue(double totalPharmecyBillValue) {
        this.totalPharmecyBillValue = totalPharmecyBillValue;
    }

    public double getTotalShiftStartValue() {
        return totalShiftStartValue;
    }

    public void setTotalShiftStartValue(double totalShiftStartValue) {
        this.totalShiftStartValue = totalShiftStartValue;
    }

    public double getTotalBalanceTransfer() {
        return totalBalanceTransfer;
    }

    public void setTotalBalanceTransfer(double totalBalanceTransfer) {
        this.totalBalanceTransfer = totalBalanceTransfer;
    }

    public double getTotalTransferRecive() {
        return totalTransferRecive;
    }

    public void setTotalTransferRecive(double totalTransferRecive) {
        this.totalTransferRecive = totalTransferRecive;
    }

    public double getTotalFunds() {
        return totalFunds;
    }

    public void setTotalFunds(double totalFunds) {
        this.totalFunds = totalFunds;
    }

    public double getShiftEndTotalValue() {
        return shiftEndTotalValue;
    }

    public void setShiftEndTotalValue(double shiftEndTotalValue) {
        this.shiftEndTotalValue = shiftEndTotalValue;
    }

    public double getShiftEndRefundBillValue() {
        return shiftEndRefundBillValue;
    }

    public void setShiftEndRefundBillValue(double shiftEndRefundBillValue) {
        this.shiftEndRefundBillValue = shiftEndRefundBillValue;
    }

    public double getShiftEndCanceledBillValue() {
        return shiftEndCanceledBillValue;
    }

    public void setShiftEndCanceledBillValue(double shiftEndCanceledBillValue) {
        this.shiftEndCanceledBillValue = shiftEndCanceledBillValue;
    }

    public double getTotalWithdrawals() {
        return totalWithdrawals;
    }

    public void setTotalWithdrawals(double totalWithdrawals) {
        this.totalWithdrawals = totalWithdrawals;
    }

    public double getTotalDeposits() {
        return totalDeposits;
    }

    public void setTotalDeposits(double totalDeposits) {
        this.totalDeposits = totalDeposits;
    }

    public double getTotalBillRefundValue() {
        return totalBillRefundValue;
    }

    public void setTotalBillRefundValue(double totalBillRefundValue) {
        this.totalBillRefundValue = totalBillRefundValue;
    }

    public double getTotalBillCancelledValue() {
        return totalBillCancelledValue;
    }

    public void setTotalBillCancelledValue(double totalBillCancelledValue) {
        this.totalBillCancelledValue = totalBillCancelledValue;
    }

    public double getTotalBilledBillValue() {
        return totalBilledBillValue;
    }

    public void setTotalBilledBillValue(double totalBilledBillValue) {
        this.totalBilledBillValue = totalBilledBillValue;
    }

    public double getDeductions() {
        return Deductions;
    }

    public void setDeductions(double Deductions) {
        this.Deductions = Deductions;
    }

    public double getAdditions() {
        return additions;
    }

    public void setAdditions(double additions) {
        this.additions = additions;
    }

    public double getTotalCcBillValue() {
        return totalCcBillValue;
    }

    public void setTotalCcBillValue(double totalCcBillValue) {
        this.totalCcBillValue = totalCcBillValue;
    }

    public double getTotalOpdBillCanceledValue() {
        return totalOpdBillCanceledValue;
    }

    public void setTotalOpdBillCanceledValue(double totalOpdBillCanceledValue) {
        this.totalOpdBillCanceledValue = totalOpdBillCanceledValue;
    }

    public double getTotalPharmecyBillCanceledValue() {
        return totalPharmecyBillCanceledValue;
    }

    public void setTotalPharmecyBillCanceledValue(double totalPharmecyBillCanceledValue) {
        this.totalPharmecyBillCanceledValue = totalPharmecyBillCanceledValue;
    }

    public double getTotalCcBillCanceledValue() {
        return totalCcBillCanceledValue;
    }

    public void setTotalCcBillCanceledValue(double totalCcBillCanceledValue) {
        this.totalCcBillCanceledValue = totalCcBillCanceledValue;
    }

    public int getFundTransferBillsToReceiveCount() {
        return fundTransferBillsToReceiveCount;
    }

    public void setFundTransferBillsToReceiveCount(int fundTransferBillsToReceiveCount) {
        this.fundTransferBillsToReceiveCount = fundTransferBillsToReceiveCount;
    }

    public List<Payment> getRecievedBIllPayments() {
        return recievedBIllPayments;
    }

    public void setRecievedBIllPayments(List<Payment> recievedBIllPayments) {
        this.recievedBIllPayments = recievedBIllPayments;
    }

    public double getTotalChannelBillValue() {
        return totalChannelBillValue;
    }

    public void setTotalChannelBillValue(double totalChannelBillValue) {
        this.totalChannelBillValue = totalChannelBillValue;
    }

    public double getTotalChannelBillCancelledValue() {
        return totalChannelBillCancelledValue;
    }

    public void setTotalChannelBillCancelledValue(double totalChannelBillCancelledValue) {
        this.totalChannelBillCancelledValue = totalChannelBillCancelledValue;
    }

    public double getTotalOpdBillRefundValue() {
        return totalOpdBillRefundValue;
    }

    public void setTotalOpdBillRefundValue(double totalOpdBillRefundValue) {
        this.totalOpdBillRefundValue = totalOpdBillRefundValue;
    }

    public double getTotalPharmacyBillRefundValue() {
        return totalPharmacyBillRefundValue;
    }

    public void setTotalPharmacyBillRefundValue(double totalPharmacyBillRefundValue) {
        this.totalPharmacyBillRefundValue = totalPharmacyBillRefundValue;
    }

    public double getTotalChannelBillRefundValue() {
        return totalChannelBillRefundValue;
    }

    public void setTotalChannelBillRefundValue(double totalChannelBillRefundValue) {
        this.totalChannelBillRefundValue = totalChannelBillRefundValue;
    }

    public double getTotalCcBillRefundValue() {
        return totalCcBillRefundValue;
    }

    public void setTotalCcBillRefundValue(double totalCcBillRefundValue) {
        this.totalCcBillRefundValue = totalCcBillRefundValue;
    }

    public double getTotalProfessionalPaymentBillValue() {
        return totalProfessionalPaymentBillValue;
    }

    public void setTotalProfessionalPaymentBillValue(double totalProfessionalPaymentBillValue) {
        this.totalProfessionalPaymentBillValue = totalProfessionalPaymentBillValue;
    }

    public double getTotalProfessionalPaymentBillCancelledValue() {
        return totalProfessionalPaymentBillCancelledValue;
    }

    public void setTotalProfessionalPaymentBillCancelledValue(double totalProfessionalPaymentBillCancelledValue) {
        this.totalProfessionalPaymentBillCancelledValue = totalProfessionalPaymentBillCancelledValue;
    }

    public PaymentMethodValues getPaymentMethodValues() {
        return paymentMethodValues;
    }

    public AtomicBillTypeTotals getAtomicBillTypeTotalsByBills() {
        return atomicBillTypeTotalsByBills;
    }

    public void setAtomicBillTypeTotalsByBills(AtomicBillTypeTotals atomicBillTypeTotalsByBills) {
        this.atomicBillTypeTotalsByBills = atomicBillTypeTotalsByBills;
    }

    public FinancialReport getFinancialReportByBills() {
        return financialReportByBills;
    }

    public void setFinancialReportByBills(FinancialReport financialReportByBills) {
        this.financialReportByBills = financialReportByBills;
    }

    public List<Bill> getCurrentBills() {
        return currentBills;
    }

    public void setCurrentBills(List<Bill> currentBills) {
        this.currentBills = currentBills;
    }

    public AtomicBillTypeTotals getAtomicBillTypeTotalsByPayments() {
        return atomicBillTypeTotalsByPayments;
    }

    public void setAtomicBillTypeTotalsByPayments(AtomicBillTypeTotals atomicBillTypeTotalsByPayments) {
        this.atomicBillTypeTotalsByPayments = atomicBillTypeTotalsByPayments;
    }

    public FinancialReport getFinancialReportByPayments() {
        return financialReportByPayments;
    }

    public void setFinancialReportByPayments(FinancialReport financialReportByPayments) {
        this.financialReportByPayments = financialReportByPayments;
    }

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
            toDate = CommonFunctions.getEndOfDay(toDate);
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public ReportTemplateRowBundle getPaymentSummaryBundle() {
        if (paymentSummaryBundle == null) {
            paymentSummaryBundle = new ReportTemplateRowBundle();
        }
        return paymentSummaryBundle;
    }

    public void setPaymentSummaryBundle(ReportTemplateRowBundle paymentSummaryBundle) {
        this.paymentSummaryBundle = paymentSummaryBundle;
    }

    public List<Bill> getShiaftStartBills() {
        return shiaftStartBills;
    }

    public void setShiaftStartBills(List<Bill> shiaftStartBills) {
        this.shiaftStartBills = shiaftStartBills;

    }

    public List<Payment> getPaymentsSelected() {
        return paymentsSelected;
    }

    public void setPaymentsSelected(List<Payment> paymentsSelected) {
        this.paymentsSelected = paymentsSelected;

    }

    public ReportTemplateRowBundle getReportTemplateRowBundle() {
        return reportTemplateRowBundle;
    }

    public void setReportTemplateRowBundle(ReportTemplateRowBundle reportTemplateRowBundle) {
        this.reportTemplateRowBundle = reportTemplateRowBundle;
    }

    public ReportTemplateRowBundle getOpdServiceBundle() {
        return opdServiceBundle;
    }

    public void setOpdServiceBundle(ReportTemplateRowBundle opdServiceBundle) {
        this.opdServiceBundle = opdServiceBundle;
    }

    public ReportTemplateRowBundle getChannellingBundle() {
        return channellingBundle;
    }

    public void setChannellingBundle(ReportTemplateRowBundle channellingBundle) {
        this.channellingBundle = channellingBundle;
    }

    public ReportTemplateRowBundle getOpdDocPayment() {
        return opdDocPayment;
    }

    public void setOpdDocPayment(ReportTemplateRowBundle opdDocPayment) {
        this.opdDocPayment = opdDocPayment;
    }

    public ReportTemplateRowBundle getChannellingDocPayment() {
        return channellingDocPayment;
    }

    public void setChannellingDocPayment(ReportTemplateRowBundle channellingDocPayment) {
        this.channellingDocPayment = channellingDocPayment;
    }

    public ReportTemplateRowBundle getOpdBilled() {
        return opdBilled;
    }

    public void setOpdBilled(ReportTemplateRowBundle opdBilled) {
        this.opdBilled = opdBilled;
    }

    public ReportTemplateRowBundle getOpdReturns() {
        return opdReturns;
    }

    public void setOpdReturns(ReportTemplateRowBundle opdReturns) {
        this.opdReturns = opdReturns;
    }

    public ReportTemplateRowBundle getChannellingBilled() {
        return channellingBilled;
    }

    public void setChannellingBilled(ReportTemplateRowBundle channellingBilled) {
        this.channellingBilled = channellingBilled;
    }

    public ReportTemplateRowBundle getChannellingReturns() {
        return channellingReturns;
    }

    public void setChannellingReturns(ReportTemplateRowBundle channellingReturns) {
        this.channellingReturns = channellingReturns;
    }

    public ReportTemplateRowBundle getPharmacyBilld() {
        return pharmacyBilld;
    }

    public void setPharmacyBilld(ReportTemplateRowBundle pharmacyBilld) {
        this.pharmacyBilld = pharmacyBilld;
    }

    public ReportTemplateRowBundle getPharmacyReturned() {
        return pharmacyReturned;
    }

    public void setPharmacyReturned(ReportTemplateRowBundle pharmacyReturned) {
        this.pharmacyReturned = pharmacyReturned;
    }

    public ReportTemplateRowBundle getOpdBundle() {
        return opdBundle;
    }

    public void setOpdBundle(ReportTemplateRowBundle opdBundle) {
        this.opdBundle = opdBundle;
    }

    private void resetBundles() {
        reportTemplateRowBundle = new ReportTemplateRowBundle();
        opdServiceBundle = new ReportTemplateRowBundle();
        channellingBundle = new ReportTemplateRowBundle();
        opdDocPayment = new ReportTemplateRowBundle();
        channellingDocPayment = new ReportTemplateRowBundle();
        opdBilled = new ReportTemplateRowBundle();
        opdReturns = new ReportTemplateRowBundle();
        opdBundle = new ReportTemplateRowBundle();
        channellingBilled = new ReportTemplateRowBundle();
        channellingReturns = new ReportTemplateRowBundle();
        pharmacyBilld = new ReportTemplateRowBundle();
        pharmacyReturned = new ReportTemplateRowBundle();
    }

    public ReportTemplateType getReportTemplateType() {
        return reportTemplateType;
    }

    public void setReportTemplateType(ReportTemplateType reportTemplateType) {
        this.reportTemplateType = reportTemplateType;
    }

    public ReportTemplateRowBundle getChannellingOnsite() {
        return channellingOnsite;
    }

    public void setChannellingOnsite(ReportTemplateRowBundle channellingOnsite) {
        this.channellingOnsite = channellingOnsite;
    }

    public ReportTemplateRowBundle getChannellingAgent() {
        return channellingAgent;
    }

    public void setChannellingAgent(ReportTemplateRowBundle channellingAgent) {
        this.channellingAgent = channellingAgent;
    }

    public ReportTemplateRowBundle getChannellingOnline() {
        return channellingOnline;
    }

    public void setChannellingOnline(ReportTemplateRowBundle channellingOnline) {
        this.channellingOnline = channellingOnline;
    }

    public ReportTemplateRowBundle getOpdByDepartment() {
        return opdByDepartment;
    }

    public void setOpdByDepartment(ReportTemplateRowBundle opdByDepartment) {
        this.opdByDepartment = opdByDepartment;
    }

    public List<ReportTemplateRowBundle> getShiftEndBundles() {
        return shiftEndBundles;
    }

    public void setShiftEndBundles(List<ReportTemplateRowBundle> shiftEndBundles) {
        this.shiftEndBundles = shiftEndBundles;
    }

}
