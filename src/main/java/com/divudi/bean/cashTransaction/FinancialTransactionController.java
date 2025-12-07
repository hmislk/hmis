package com.divudi.bean.cashTransaction;

import com.divudi.bean.channel.analytics.ReportTemplateController;
import java.util.HashMap;
import com.divudi.bean.common.BillController;
import com.divudi.bean.common.BillSearch;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SearchController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.*;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Payment;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.analytics.ReportTemplateType;
import com.divudi.core.data.dataStructure.PaymentMethodData;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.core.entity.BillComponent;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.PaymentHandoverItem;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.cashTransaction.CashBook;
import com.divudi.core.entity.cashTransaction.CashBookEntry;
import com.divudi.core.entity.cashTransaction.DenominationTransaction;
import com.divudi.core.entity.cashTransaction.DetailedFinancialBill;
import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.facade.BillComponentFacade;
import com.divudi.core.facade.DrawerFacade;
import com.divudi.core.facade.PaymentHandoverItemFacade;
import com.divudi.core.facade.PaymentMethodValueFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.BillService;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.faces.event.AjaxBehaviorEvent;
import javax.inject.Inject;
import javax.persistence.TemporalType;
import kotlin.collections.ArrayDeque;
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
    private BillFacade billFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    private PaymentMethodValueFacade paymentMethodValueFacade;
    @EJB
    BillNumberGenerator billNumberGenerator;
    @EJB
    DrawerFacade drawerFacade;
    @EJB
    BillService billService;
    @EJB
    PaymentHandoverItemFacade paymentHandoverItemFacade;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private SessionController sessionController;
    @Inject
    private CashBookEntryController cashBookEntryController;
    @Inject
    CashBookController cashBookController;
    @Inject
    private ReportTemplateController reportTemplateController;
    @Inject
    private BillController billController;
    @Inject
    private PaymentController paymentController;
    @Inject
    PaymentHandoverItemController paymentHandoverItemController;
    @Inject
    private SearchController searchController;
    @Inject
    private BillSearch billSearch;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    DetailedFinancialBillController detailedFinancialBillController;
    @Inject
    private DenominationTransactionController denominationTransactionController;
    @Inject
    private DrawerController drawerController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    private Bill currentBill;
    private DetailedFinancialBill currentDetailedFinancialBill;
    private List<BillComponent> currentBillComponents;
    private ReportTemplateType reportTemplateType;
    private ReportTemplateRowBundle reportTemplateRowBundle;
    private ReportTemplateRowBundle opdServiceBundle;
    private ReportTemplateRowBundle channellingBundle;
    private ReportTemplateRowBundle opdDocPayment;
    private ReportTemplateRowBundle channellingDocPayment;
    private boolean handoverValuesCreated = false;

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
    private List<Bill> handovertBillsToReceive;
    private List<Bill> fundBillsForClosureBills;
    private Bill selectedBill;
    private Bill nonClosedShiftStartFundBill;

    private ReportTemplateRowBundle refundBundle;
    private ReportTemplateRowBundle cancelledBundle;

    private List<Payment> paymentsFromShiftSratToNow;
    private List<Payment> cardPaymentsFromShiftSratToNow;
    private List<Payment> chequeFromShiftSratToNow;
    private List<Payment> paymentsSelected;
    private List<Payment> recievedBIllPayments;
    private List<Bill> allBillsShiftStartToNow;

    private ReportTemplateRowBundle chequeTransactionPaymentBundle;
    private ReportTemplateRowBundle cardTransactionPaymentBundle;

    private ReportTemplateRowBundle bundle;
    private ReportTemplateRowBundle selectedBundle;
    private PaymentMethod selectedPaymentMethod;

    private Boolean patientDepositsAreConsideredInHandingover;

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
    private int handoverBillsToReceiveCount;
    private Date fromDate;
    private Date toDate;
    private Date cashbookDate;
    private Department cashbookDepartment;
    private List<Date> cashbookDates;
    private List<BillItem> billItems;
    private BillItem removingBillItem;
    private List<Department> cashbookDepartments;
    private List<com.divudi.core.data.PaymentMethodValue> handingOverPaymentMethodValues;

    private ReportTemplateRowBundle paymentSummaryBundle;

    private Department department;
    private WebUser user;
    private Staff fromStaff;
    private Staff toStaff;
    private Department forDepartment;
    private Department toDepartment;
    private Date forDate;

    private int tabIndex;

    private Drawer loggedUserDrawer;

    private List<DenominationTransaction> denominationTransactions;
    private DenominationTransaction dt;
    private double totalCashFund;

    boolean floatTransferStarted = false;

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Constructors">
    public FinancialTransactionController() {
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Navigational Methods">
    public String navigateToFinancialTransactionIndex() {
        resetClassVariables();
        fillFundTransferBillsForMeToReceive();
        return "/cashier/index?faces-redirect=true";
    }

    public String navigateToPaymentManagement() {
        if (sessionController.getPaymentManagementAfterShiftStart()) {
            findNonClosedShiftStartFundBillIsAvailable();
            if (getNonClosedShiftStartFundBill() != null) {
                return "/payments/pay_index?faces-redirect=true";
            } else {
                JsfUtil.addErrorMessage("Start Your Shift First !");
                return "/cashier/index?faces-redirect=true";
            }
        } else {
            return "/payments/pay_index?faces-redirect=true";
        }
    }

    public String navigateToNewIncomeBill() {
        resetClassVariables();
        currentBill = new Bill();
        currentBill.setBillType(BillType.SUPPLEMENTARY_INCOME);
        currentBill.setBillTypeAtomic(BillTypeAtomic.SUPPLEMENTARY_INCOME);
        return "/cashier/income_bill?faces-redirect=true";
    }

    public String navigateToNewExpenseBill() {
        resetClassVariables();
        currentBill = new Bill();
        currentBill.setBillType(BillType.OPERATIONAL_EXPENSES);
        currentBill.setBillTypeAtomic(BillTypeAtomic.OPERATIONAL_EXPENSES);
        fillFundTransferBillsForMeToReceive();
        return "/cashier/expense_bill?faces-redirect=true";
    }

    public String navigateToTraceIncomeExpenseBills() {
        resetClassVariables();
        fillFundTransferBillsForMeToReceive();
        return "/cashier/trace_income_expenses?faces-redirect=true";
    }

    public String navigateToMyDrawer() {
        loggedUserDrawer = null;
        return "/cashier/my_drawer?faces-redirect=true";
    }

    public String navigateToCreateNewInitialFundBill() {
        resetClassVariables();
        prepareToAddNewInitialFundBill();
        return "/cashier/initial_fund_bill?faces-redirect=true";
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

        bundle = combineBundlesByItemDepartment(ins, outs);

        return bundle;
    }

    public String navigateToMyServiceDepartmentRevenueReportByPeriod() {
        return "/cashier/my_service_department_revenue_report_by_period";
    }

    public void processMyServiceDepartmentRevenueReportByPeriod() {
        List<BillTypeAtomic> btas = null;
        Date paramDate = null;
        Date paramFromDate = fromDate;
        Date paramToDate = toDate;
        Institution paramInstitution = null;
        Department paramDepartment = null;
        Institution paramFromInstitution = null;
        Department paramFromDepartment = null;
        Institution paramToInstitution = null;
        Department paramToDepartment = null;
        WebUser paramUser = sessionController.getLoggedUser();
        Institution paramCreditCompany = null;
        Long paramStartId = null;
        Long paramEndId = null;

        // Calls the new overloaded method with all parameters for backward compatibility
        processShiftEndReport(btas, paramDate, paramFromDate, paramToDate, paramInstitution, paramDepartment,
                paramFromInstitution, paramFromDepartment, paramToInstitution, paramToDepartment,
                paramUser, paramCreditCompany, paramStartId, paramEndId);
    }

    // Original method for backwards compatibility
    public void processShiftEndReport() {
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
        Long paramEndId = (nonClosedShiftStartFundBill.getReferenceBill() != null) ? nonClosedShiftStartFundBill.getReferenceBill().getId() : null;

        // Calls the new overloaded method with all parameters for backward compatibility
        processShiftEndReport(btas, paramDate, paramFromDate, paramToDate, paramInstitution, paramDepartment,
                paramFromInstitution, paramFromDepartment, paramToInstitution, paramToDepartment,
                paramUser, paramCreditCompany, paramStartId, paramEndId);
    }

// Overloaded method with all parameters
    public void processShiftEndReport(
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

        shiftEndBundles = new ArrayList<>();
        ReportTemplateType channelingType = ReportTemplateType.ITEM_CATEGORY_SUMMARY_BY_BILL;
        ReportTemplateType opdType = ReportTemplateType.ITEM_DEPARTMENT_SUMMARY_BY_BILL_ITEM;
        ReportTemplateType paymentsType = ReportTemplateType.BILL_TYPE_ATOMIC_SUMMARY_USING_BILLS;

        // Create report bundles by calling helper methods
        ReportTemplateRowBundle tmpChannellingBundle = addChannellingByCategories(
                channelingType, btas, paramDate, paramFromDate, paramToDate, paramInstitution,
                paramDepartment, paramFromInstitution, paramFromDepartment, paramToInstitution,
                paramToDepartment, paramUser, paramCreditCompany, paramStartId, paramEndId);

        ReportTemplateRowBundle tmpOpdBundle = addOpdByDepartments(
                opdType, btas, paramDate, paramFromDate, paramToDate, paramInstitution,
                paramDepartment, paramFromInstitution, paramFromDepartment, paramToInstitution,
                paramToDepartment, paramUser, paramCreditCompany, paramStartId, paramEndId);

        ReportTemplateRowBundle tmpPaymentBundle = addProfessionalPayments(
                paymentsType, btas, paramDate, paramFromDate, paramToDate, paramInstitution,
                paramDepartment, paramFromInstitution, paramFromDepartment, paramToInstitution,
                paramToDepartment, paramUser, paramCreditCompany, paramStartId, paramEndId);

        // Set names for each bundle
        tmpChannellingBundle.setName("Channelling");
        tmpOpdBundle.setName("OPD");
        tmpPaymentBundle.setName("Payments");

        // Add to shiftEndBundles list
        shiftEndBundles.add(tmpChannellingBundle);
        shiftEndBundles.add(tmpOpdBundle);
        shiftEndBundles.add(tmpPaymentBundle);
    }

    public String navigateToDayEndReport() {
        return "/cashier/day_end_report?faces-redirect=true";
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

        fillPayments(fromDate, toDate, null);
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

        ReportTemplateRowBundle temOutBundle = new ReportTemplateRowBundle();
        temOutBundle.setReportTemplateRows(new ArrayList<>());  // Ensure the list is initialized
        Map<Object, ReportTemplateRow> combinedRows = new HashMap<>();

        final Object NULL_DEPARTMENT_KEY = new Object();  // Placeholder for null departments

        if (inBundle != null && inBundle.getReportTemplateRows() != null) {
            for (ReportTemplateRow inRow : inBundle.getReportTemplateRows()) {
                if (inRow != null) {
                    Department d = inRow.getItemDepartment();
                    Object key = (d != null) ? d : NULL_DEPARTMENT_KEY;
                    if (d == null) {
                    } else {
                    }
                    if (!combinedRows.containsKey(key)) {
                        combinedRows.put(key, new ReportTemplateRow(d));
                    }
                    ReportTemplateRow combinedRow = combinedRows.get(key);
                    combinedRow.setRowCountIn((combinedRow.getRowCountIn() != null ? combinedRow.getRowCountIn() : 0L) + (inRow.getRowCount() != null ? inRow.getRowCount() : 0L));
                    combinedRow.setRowValueIn((combinedRow.getRowValueIn() != null ? combinedRow.getRowValueIn() : 0.0) + (inRow.getRowValue() != null ? inRow.getRowValue() : 0.0));
                } else {
                }
            }
        } else {
        }

        if (outBundle != null && outBundle.getReportTemplateRows() != null) {
            for (ReportTemplateRow outRow : outBundle.getReportTemplateRows()) {
                if (outRow != null) {
                    Department d = outRow.getItemDepartment();
                    Object key = (d != null) ? d : NULL_DEPARTMENT_KEY;
                    if (d == null) {
                    } else {
                    }
                    if (!combinedRows.containsKey(key)) {
                        combinedRows.put(key, new ReportTemplateRow(d));
                    }
                    ReportTemplateRow combinedRow = combinedRows.get(key);
                    combinedRow.setRowCountOut((combinedRow.getRowCountOut() != null ? combinedRow.getRowCountOut() : 0L) + (outRow.getRowCount() != null ? outRow.getRowCount() : 0L));
                    combinedRow.setRowValueOut((combinedRow.getRowValueOut() != null ? combinedRow.getRowValueOut() : 0.0) + (outRow.getRowValue() != null ? outRow.getRowValue() : 0.0));
                } else {
                }
            }
        } else {
        }

        long totalInCount = 0;
        long totalOutCount = 0;
        double totalInValue = 0.0;
        double totalOutValue = 0.0;

        for (ReportTemplateRow row : combinedRows.values()) {
            String deptName = (row.getItemDepartment() != null) ? row.getItemDepartment().getName() : "NULL_DEPARTMENT";
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

    private ReportTemplateRowBundle combineBundlesByBillTypeAtomic(ReportTemplateRowBundle inBundle, ReportTemplateRowBundle outBundle) {

        ReportTemplateRowBundle temOutBundle = new ReportTemplateRowBundle();
        temOutBundle.setReportTemplateRows(new ArrayList<>());  // Ensure the list is initialized
        Map<Object, ReportTemplateRow> combinedRows = new HashMap<>();

        final Object NULL_DEPARTMENT_KEY = new Object();  // Placeholder for null departments

        if (inBundle != null && inBundle.getReportTemplateRows() != null) {
            for (ReportTemplateRow inRow : inBundle.getReportTemplateRows()) {
                if (inRow != null) {
                    BillTypeAtomic d = inRow.getBillTypeAtomic();
                    Object key = (d != null) ? d : NULL_DEPARTMENT_KEY;
                    if (d == null) {
                    } else {
                    }
                    if (!combinedRows.containsKey(key)) {
                        combinedRows.put(key, new ReportTemplateRow(d));
                    }
                    ReportTemplateRow combinedRow = combinedRows.get(key);
                    combinedRow.setRowCountIn((combinedRow.getRowCountIn() != null ? combinedRow.getRowCountIn() : 0L) + (inRow.getRowCount() != null ? inRow.getRowCount() : 0L));
                    combinedRow.setRowValueIn((combinedRow.getRowValueIn() != null ? combinedRow.getRowValueIn() : 0.0) + (inRow.getRowValue() != null ? inRow.getRowValue() : 0.0));
                } else {
                }
            }
        } else {
        }

        if (outBundle != null && outBundle.getReportTemplateRows() != null) {
            for (ReportTemplateRow outRow : outBundle.getReportTemplateRows()) {
                if (outRow != null) {
                    BillTypeAtomic d = outRow.getBillTypeAtomic();
                    Object key = (d != null) ? d : NULL_DEPARTMENT_KEY;
                    if (d == null) {
                    } else {
                    }
                    if (!combinedRows.containsKey(key)) {
                        combinedRows.put(key, new ReportTemplateRow(d));
                    }
                    ReportTemplateRow combinedRow = combinedRows.get(key);
                    combinedRow.setRowCountOut((combinedRow.getRowCountOut() != null ? combinedRow.getRowCountOut() : 0L) + (outRow.getRowCount() != null ? outRow.getRowCount() : 0L));
                    combinedRow.setRowValueOut((combinedRow.getRowValueOut() != null ? combinedRow.getRowValueOut() : 0.0) + (outRow.getRowValue() != null ? outRow.getRowValue() : 0.0));
                } else {
                }
            }
        } else {
        }

        long totalInCount = 0;
        long totalOutCount = 0;
        double totalInValue = 0.0;
        double totalOutValue = 0.0;

        for (ReportTemplateRow row : combinedRows.values()) {
            String deptName = (row.getItemDepartment() != null) ? row.getItemDepartment().getName() : "NULL_DEPARTMENT";
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
        return "/cashier/initial_fund_bill_list?faces-redirect=true";
    }

    public void listShiftStartBills() {
        String jpql = "select b "
                + " from Bill b "
                + " where b.retired=:ret"
                + " and b.billTypeAtomic=:bta "
                + " and b.createdAt between :fd and :td ";

        Map params = new HashMap<>();
        params.put("ret", false);
        params.put("bta", BillTypeAtomic.FUND_SHIFT_START_BILL);
        params.put("fd", fromDate);
        params.put("td", toDate);

        if (getDepartment() != null) {
            jpql += " and b.department =:dept";
            params.put("dept", getDepartment());
        }
        jpql += " order by b.id ";

        shiaftStartBills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public String navigateToSelectPaymentsForHandoverCreate(ReportTemplateRowBundle inputBundle, PaymentMethod inputPaymentMethod) {
        if (inputBundle == null) {
            JsfUtil.addErrorMessage("No Bundle Selected");
            return null;
        }
        selectedBundle = inputBundle;
        selectedPaymentMethod = inputPaymentMethod;
        return "/cashier/handover_start_select?faces-redirect=true";
    }

    @Deprecated
    public String navigateToSelectPaymentsForHandoverAccept(ReportTemplateRowBundle inputBundle, PaymentMethod inputPaymentMethod) {
        if (inputBundle == null) {
            JsfUtil.addErrorMessage("No Bundle Selected");
            return null;
        }
        selectedBundle = inputBundle;
        selectedPaymentMethod = inputPaymentMethod;
        return "/cashier/handover_accept_select?faces-redirect=true";
    }

    public String navigateBackToPaymentHandoverCreate() {
        selectedBundle.markSelectedAtHandover();
        bundle.calculateTotalsByChildBundlesForHandover();
        return "/cashier/handover_start_all?faces-redirect=true";
    }

    public void updateForPaymentHandoverSelectionAtCreate() {
//        if (selectedBundle != null) {
//            selectedBundle.calculateTotalsByPaymentsAndDenominationsForHandover();
//        }
        bundle.setPatientDepositsAreConsideredInHandingover(getPatientDepositsAreConsideredInHandingover());
        bundle.calculateTotalsByChildBundlesForHandover();
    }

    public void selectAllForPaymentHandoverSelectionAtCreate() {
        selectedBundle.markAllAtHandover(selectedPaymentMethod);
        selectedBundle.calculateTotalsOfSelectedRowsPlusAllCashForHandover(getPatientDepositsAreConsideredInHandingover());
        boolean selectAllCashToHandover = configOptionApplicationController.getBooleanValueByKey("Select All Cash During Handover", true);
        bundle.setSelectAllCashToHandover(selectAllCashToHandover);
        bundle.calculateTotalsByChildBundlesForHandover();
    }

    public void unselectAllForPaymentHandoverSelection() {
        selectedBundle.unmarkAllAtHandover();
        selectedBundle.calculateTotalsByPaymentsAndDenominations();
        bundle.calculateTotalsBySelectedChildBundles();
    }

    public String navigateBackToPaymentHandoverAccept() {
        selectedBundle.calculateTotalsByPaymentsAndDenominations();
        bundle.calculateTotalsBySelectedChildBundles();
        return "/cashier/handover_accept?faces-redirect=true";
    }

    public String navigateToFundTransferBill() {
        resetClassVariables();
        prepareToAddNewFundTransferBill();
        floatTransferStarted = false;
        return "/cashier/fund_transfer_bill?faces-redirect=true";
    }

    public String navigateToFundDepositBill() {
        resetClassVariables();
        prepareToAddNewFundDepositBill();
        return "/cashier/deposit_funds?faces-redirect=true";
    }

    // Method to navigate to the Record Shift Shortage page
    public String navigateToRecordShiftShortage() {
        resetClassVariables();
        prepareToAddNewShiftShortageRecord();
        return "/cashier/record_shift_shortage?faces-redirect=true";
    }

    // Method to navigate to the Record Shift Excess page
    public String navigateToRecordShiftExcess() {
        resetClassVariables();
        prepareToAddNewShiftExcessRecord();
        return "/cashier/record_shift_excess?faces-redirect=true";
    }

    public String navigateToViewDetailsOfSelectedBundleDuringHandoverInNewWindow() {
        if (selectedBundle == null) {
            return null;
        }
        return "/cashier/handover_start_all_bill_type_details?faces-redirect=true";
    }

    public String navigateToCashierShiftBillSearch() {
        resetClassVariables();
        return "/cashier/cashier_shift_bill_search?faces-redirect=true";
    }

    // Method to navigate to the Transfer Payment Method page
    public String navigateToTransferPaymentMethod() {
        resetClassVariables();
        prepareToTransferPaymentMethod();
        return "/cashier/transfer_payment_method?faces-redirect=true";
    }

    // Method to navigate to the Payment Recording page
    public String navigateToPaymentRecording() {
        resetClassVariables();
        prepareToRecordPayments();
        return "/cashier/payment_recording?faces-redirect=true";
    }

    private void prepareToAddNewShiftShortageRecord() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.ShiftShortage);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_SHORTAGE_BILL);
        currentBill.setBillClassType(BillClassType.Bill);
    }

    private void prepareToAddNewShiftExcessRecord() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.ShiftExcess);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_END_BILL);
        currentBill.setBillClassType(BillClassType.Bill);
    }

    private void prepareToTransferPaymentMethod() {
        // Prepare any necessary configurations or data before transferring payment methods
    }

    private void prepareToRecordPayments() {
        // Set up required data and context before recording payments
    }

    public String navigateToCashierSummary() {
        return "/cashier/cashier_summary?faces-redirect=true";
    }

    public String navigateToMyCashierSummary() {
        return "/cashier/my_cashier_summary?faces-redirect=true";
    }

    public String navigateToMyCashierDetails() {
        return "/cashier/my_cashier_detailed?faces-redirect=true";
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

    @Deprecated
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
        floatTransferStarted = false;
        return "/cashier/fund_transfer_receive_bill?faces-redirect=true";
    }

    public String navigateToReceiveFloatTransferForMe(Bill bill) {
        if (bill == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return "";
        }
        if (bill.getBillType() != BillType.FundTransferBill) {
            JsfUtil.addErrorMessage("Wrong Bill Type");
            return "";
        }
        floatTransferStarted = false;
        setSelectedBill(bill);
        resetClassVariablesWithoutSelectedBill();
        prepareToAddNewFundTransferReceiveBill();
        return "/cashier/fund_transfer_receive_bill?faces-redirect=true";
    }

    public String navigateToReceiveNewHandoverBill() {
        // Check for null or invalid selected bill
        if (selectedBill == null) {
            JsfUtil.addErrorMessage("Please select a bill.");
            return null;
        }
        if (selectedBill.getBillTypeAtomic() != BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE) {
            JsfUtil.addErrorMessage("Wrong Bill Type.");
            return null;
        }

        // Reset class variables for accepting handover bill
        resetClassVariablesForAcceptHandoverBill();

        bundle = new ReportTemplateRowBundle();
        bundle.setUser(selectedBill.getFromWebUser());
        bundle.setStartBill(selectedBill.getReferenceBill());
        if (selectedBill.getReferenceBill() != null) {
            bundle.setEndBill(selectedBill.getReferenceBill().getReferenceBill());
        }

        Bill denoBill = billSearch.fetchReferredBill(BillTypeAtomic.FUND_SHIFT_DENOMINATION_HANDOVER_CREATE, selectedBill);
        List<DenominationTransaction> dts = billService.fetchDenominationTransactionFromBill(denoBill);
        if (dts != null && !dts.isEmpty()) {
            bundle.setDenominationTransactions(dts);
            bundle.setCashHandoverValue(denoBill.getNetTotal());
        }

        // Fetch and process shift handover component bills
        List<Bill> shiftHandoverCompletionBills = billSearch.fetchReferredBills(BillTypeAtomic.FUND_SHIFT_COMPONANT_HANDOVER_CREATE, selectedBill);
        if (shiftHandoverCompletionBills == null || shiftHandoverCompletionBills.isEmpty()) {
            JsfUtil.addErrorMessage("No component bills found.");
            return null;
        }

        // Recreate data holder objects from persisted objects
        for (Bill b : shiftHandoverCompletionBills) {
            ReportTemplateRowBundle childBundle = new ReportTemplateRowBundle();
            childBundle.setSelected(true);
            childBundle.setDenominations(sessionController.findDefaultDenominations());
            childBundle.setDepartment(b.getDepartment());
            childBundle.setUser(b.getFromWebUser());
            childBundle.setDate(b.getBillDate());
            childBundle.setBundleType(b.getReferenceNumber());

            List<Payment> payments = fetchPaymentsForSummaryHandoverCreation(b);
            if (payments != null && !payments.isEmpty()) {
                for (Payment p : payments) {
                    ReportTemplateRow row = new ReportTemplateRow();
                    row.setPayment(p);
                    row.setSelected(true);
                    childBundle.getReportTemplateRows().add(row);
                }
            }

            childBundle.calculateTotalsByPaymentsAndDenominations();
            bundle.getBundles().add(childBundle);
        }

        bundle.aggregateTotalsFromAllChildBundles();
        bundle.collectDepartments();

        // Create and configure the current bill
//        currentBill = new Bill();
//        currentBill.setBillType(BillType.CashHandoverAcceptBill);
//        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_HANDOVER_ACCEPT);
//        currentBill.setBillClassType(BillClassType.Bill);
//        currentBill.setReferenceBill(selectedBill);
//        currentBill.setFromDepartment(selectedBill.getFromDepartment());
//        currentBill.setFromDate(selectedBill.getFromDate());
//        currentBill.setDepartment(sessionController.getDepartment());
//        currentBill.setInstitution(sessionController.getInstitution());
//        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
//        currentBill.setWebUser(sessionController.getLoggedUser());
//        currentBill.setToWebUser(sessionController.getLoggedUser());
//        currentBill.setFromWebUser(selectedBill.getCreater());
//        currentBill.setCreatedAt(new Date());
//        currentBill.setCreater(sessionController.getLoggedUser());
//        currentBill.setBillDate(new Date());
//        currentBill.setBillTime(new Date());
//        currentBill.setTotal(bundle.getTotal());
//        currentBill.setNetTotal(bundle.getTotal());
//
//        // Save the current bill
//        billController.save(currentBill);
        return "/cashier/handover_accept?faces-redirect=true";
    }

    public String navigateToViewHandoverBill() {
        if (selectedBill == null) {
            JsfUtil.addErrorMessage("Please select a bill.");
            return null;
        }
        if (selectedBill.getBillTypeAtomic() != BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE) {
            JsfUtil.addErrorMessage("Wrong Bill Type.");
            return null;
        }
        resetClassVariablesForAcceptHandoverBill();
        bundle = new ReportTemplateRowBundle();
        bundle.setUser(selectedBill.getFromWebUser());
        bundle.setToUser(selectedBill.getToWebUser());
        bundle.setStartBill(selectedBill.getReferenceBill());
        if (selectedBill.getReferenceBill() != null) {
            bundle.setEndBill(selectedBill.getReferenceBill().getReferenceBill());
        }

        Bill denoBill = billSearch.fetchReferredBill(BillTypeAtomic.FUND_SHIFT_DENOMINATION_HANDOVER_CREATE, selectedBill);
        List<DenominationTransaction> dts = billService.fetchDenominationTransactionFromBill(denoBill);
        if (dts != null && !dts.isEmpty()) {
            bundle.setDenominationTransactions(dts);
            bundle.setCashHandoverValue(denoBill.getNetTotal());
        }

        // Fetch and process shift handover component bills
        List<Bill> shiftHandoverCompletionBills = billSearch.fetchReferredBills(BillTypeAtomic.FUND_SHIFT_COMPONANT_HANDOVER_CREATE, selectedBill);
        if (shiftHandoverCompletionBills == null || shiftHandoverCompletionBills.isEmpty()) {
            JsfUtil.addErrorMessage("No component bills found.");
            return null;
        }

        // Recreate data holder objects from persisted objects
        for (Bill b : shiftHandoverCompletionBills) {
            ReportTemplateRowBundle childBundle = new ReportTemplateRowBundle();
            childBundle.setSelected(true);
            childBundle.setDenominations(sessionController.findDefaultDenominations());
            childBundle.setDepartment(b.getDepartment());
            childBundle.setUser(b.getFromWebUser());
            childBundle.setDate(b.getBillDate());
            childBundle.setBundleType(b.getReferenceNumber());

            List<Payment> payments = fetchPaymentsForSummaryHandoverCreation(b);
            if (payments != null && !payments.isEmpty()) {
                for (Payment p : payments) {
                    ReportTemplateRow row = new ReportTemplateRow();
                    row.setPayment(p);
                    row.setSelected(true);
                    childBundle.getReportTemplateRows().add(row);
                }
            }

            childBundle.calculateTotalsByPaymentsAndDenominations();
            bundle.getBundles().add(childBundle);
        }

        bundle.aggregateTotalsFromAllChildBundles();
        bundle.collectDepartments();

        return "/cashier/handover_preview?faces-redirect=true";
    }

    public String rejectToReceiveHandoverBill() {
        if (selectedBill == null) {
            JsfUtil.addErrorMessage("Please select a bill.");
            return null;
        }
        if (selectedBill.getBillTypeAtomic() != BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE) {
            JsfUtil.addErrorMessage("Wrong Bill Type.");
            return null;
        }

        selectedBill.setCancelled(true);
        selectedBill.setCompleted(true);
        selectedBill.setCompletedAt(new Date());
        selectedBill.setCompletedBy(sessionController.getLoggedUser());
        billController.save(selectedBill);

        Bill denoBill = billSearch.fetchReferredBill(BillTypeAtomic.FUND_SHIFT_DENOMINATION_HANDOVER_CREATE, selectedBill);
        List<DenominationTransaction> dts = billService.fetchDenominationTransactionFromBill(denoBill);
        if (dts != null && !dts.isEmpty()) {
            for (DenominationTransaction dt : dts) {
                dt.setCancelled(true);
                dt.setCancelledAt(new Date());
                dt.setCancelledBy(sessionController.getLoggedUser());
                denominationTransactionController.save(dt);
            }
        }
        List<Bill> shiftHandoverCompletionBills = billSearch.fetchReferredBills(BillTypeAtomic.FUND_SHIFT_COMPONANT_HANDOVER_CREATE, selectedBill);
        if (shiftHandoverCompletionBills != null) {
            for (Bill b : shiftHandoverCompletionBills) {
                b.setCancelled(true);
                b.setCompleted(true);
                b.setCompletedAt(new Date());
                b.setCompletedBy(sessionController.getLoggedUser());
                billController.save(b);

                List<Payment> payments = fetchPaymentsForSummaryHandoverCreation(b);
                if (payments != null && !payments.isEmpty()) {
                    for (Payment p : payments) {
                        p.setHandingOverCompleted(false);
                        p.setHandingOverStarted(false);
                        paymentController.save(p);

                        PaymentHandoverItem phi = new PaymentHandoverItem(p);
                        phi.setHandoverCreatedBill(null);
                        phi.setHandoverShiftBill(null);
                        phi.setHandoverShiftComponantBill(null);
                        paymentHandoverItemController.save(phi);

                    }
                }
            }
        }
        return navigateToReceiveHandoverBillsForMe();
    }

    public String recallMyHandoverBill() {
        if (selectedBill == null) {
            JsfUtil.addErrorMessage("Please select a bill.");
            return null;
        }
        if (selectedBill.getBillTypeAtomic() != BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE) {
            JsfUtil.addErrorMessage("Wrong Bill Type.");
            return null;
        }

        selectedBill.setCancelled(true);
        selectedBill.setCompleted(true);
        selectedBill.setCompletedAt(new Date());
        selectedBill.setCompletedBy(sessionController.getLoggedUser());
        billController.save(selectedBill);

        Bill denoBill = billSearch.fetchReferredBill(BillTypeAtomic.FUND_SHIFT_DENOMINATION_HANDOVER_CREATE, selectedBill);
        List<DenominationTransaction> dts = billService.fetchDenominationTransactionFromBill(denoBill);
        if (dts != null && !dts.isEmpty()) {
            for (DenominationTransaction dt : dts) {
                dt.setCancelled(true);
                dt.setCancelledAt(new Date());
                dt.setCancelledBy(sessionController.getLoggedUser());
                denominationTransactionController.save(dt);
            }
        }
        List<Bill> shiftHandoverCompletionBills = billSearch.fetchReferredBills(BillTypeAtomic.FUND_SHIFT_COMPONANT_HANDOVER_CREATE, selectedBill);
        if (shiftHandoverCompletionBills != null) {
            for (Bill b : shiftHandoverCompletionBills) {
                b.setCancelled(true);
                b.setCompleted(true);
                b.setCompletedAt(new Date());
                b.setCompletedBy(sessionController.getLoggedUser());
                billController.save(b);

                List<Payment> payments = fetchPaymentsForSummaryHandoverCreation(b);
                if (payments != null && !payments.isEmpty()) {
                    for (Payment p : payments) {
                        p.setHandingOverCompleted(false);
                        p.setHandingOverStarted(false);
//                        p.setHandoverCreatedBill(null);
//                        p.setHandoverShiftBill(null);
//                        p.setHandoverShiftComponantBill(null);
                        paymentController.save(p);
                    }
                }
            }
        }
        return navigateToMyHandovers();
    }

    @Deprecated
    public String navigateToReceiveNewHandoverBill(boolean old) {
        if (selectedBill == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return "";
        }
        if (selectedBill.getBillTypeAtomic() != BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE) {
            JsfUtil.addErrorMessage("Wrong Bill Type");
            return "";
        }
        resetClassVariablesForAcceptHandoverBill();

        List<Payment> paymentsToAcceptForHandover = fillPaymentsFromViewHandoverAcceptBill();
        Boolean selectAllHandoverPayments = null;
        bundle = generatePaymentBundleForHandovers(selectedBill.getReferenceBill(),
                selectedBill.getReferenceBill().getReferenceBill(),
                paymentsToAcceptForHandover,
                PaymentSelectionMode.SELECT_FOR_HANDOVER_RECEIPT);
        bundle.aggregateTotalsFromAllChildBundles();
        bundle.collectDepartments();

//
//
//         fillPaymentsFromViewHandoverAcceptBill();
        currentBill = new Bill();
        currentBill.setBillType(BillType.CashHandoverAcceptBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_HANDOVER_ACCEPT);
        currentBill.setBillClassType(BillClassType.Bill);
        currentBill.setReferenceBill(selectedBill);
        currentBill.setFromDepartment(selectedBill.getFromDepartment());
        currentBill.setFromDate(selectedBill.getFromDate());
        return "/cashier/handover_accept?faces-redirect=true";
    }

    public String navigateToRejectNewHandoverBill() {
        if (selectedBill == null) {
            JsfUtil.addErrorMessage("Please select a bill");
            return "";
        }
        if (selectedBill.getBillTypeAtomic() != BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE) {
            JsfUtil.addErrorMessage("Wrong Bill Type");
            return "";
        }
//        resetClassVariablesForAcceptHandoverBill();
//        fillPaymentsFromViewHandoverAcceptBill();
//        currentBill = new Bill();
//        currentBill.setBillType(BillType.CashHandoverAcceptBill);
//        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_HANDOVER_ACCEPT);
//        currentBill.setBillClassType(BillClassType.Bill);
//        currentBill.setReferenceBill(selectedBill);
//        currentBill.setFromDepartment(selectedBill.getFromDepartment());
//        currentBill.setFromDate(selectedBill.getFromDate());
        return "/cashier/handover_bill_reject?faces-redirect=true";
    }

    public String navigateToReceiveFundTransferBillsForMe() {
        findNonClosedShiftStartFundBillIsAvailable();
        fillFundTransferBillsForMeToReceive();
        return "/cashier/fund_transfer_bills_for_me_to_receive?faces-redirect=true";
    }

    public String navigateToReceiveHandoverBillsForMe() {
        fillHandoverBillsForMeToReceive();
        return "/cashier/handover_bills_for_me_to_receive?faces-redirect=true";
    }

    public String navigateToMyHandovers() {
        fillMyHandovers();
        return "/cashier/handover_bills_from_me?faces-redirect=true";
    }

    public String navigateToUserHandovers() {
        return "/reports/cashier_reports/handovers?faces-redirect=true";
    }

    private void prepareToAddNewInitialFundBill() {
        currentBill = new Bill();
        currentBill.setBillType(BillType.ShiftStartFundBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_START_BILL);
        currentBill.setBillClassType(BillClassType.Bill);
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());

        if (configOptionApplicationController.getBooleanValueByKey("Allow to Denomination for shift Starting Process", false)) {
            denominationTransactions = new ArrayList<>();
            denominationTransactions = denominationTransactionController.createDefaultDenominationTransaction();
        }
    }

    public void calculateTotalCashDenomination() {
        totalCashFund = 0.0;
        if (denominationTransactions == null || denominationTransactions.isEmpty()) {
            return;
        }
        for (DenominationTransaction dt : denominationTransactions) {
            if (dt == null || dt.getDenomination() == null || dt.getDenomination().getDenominationValue() == null) {
                continue;
            }
            if (dt.getDenominationQty() == null) {
                dt.setDenominationQty(0l);
                dt.setDenominationValue(null);
            } else {
                Double dv = dt.getDenomination().getDenominationValue() * dt.getDenominationQty();
                dt.setDenominationValue(dv);
                totalCashFund += dv;
            }
        }
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
            Payment np = p.createNewPaymentByCopyingAttributes();
            currentBillPayments.add(np);

        }
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="Functional Methods">
    public void resetClassVariables() {
        paymentsSelected = null;
        currentBill = null;
        currentPayment = null;
        removingPayment = null;
        currentBillPayments = null;
        fundTransferBillsToReceive = null;
        fundBillsForClosureBills = null;
        selectedBill = null;
        nonClosedShiftStartFundBill = null;
        paymentsFromShiftSratToNow = null;
        department = null;
        searchController.setBills(null);
    }

    public void resetClassVariablesForAcceptHandoverBill() {
        currentPayment = null;
        removingPayment = null;
        currentBillPayments = null;
        fundTransferBillsToReceive = null;
        fundBillsForClosureBills = null;
        nonClosedShiftStartFundBill = null;
        paymentsFromShiftSratToNow = null;
        department = null;

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
        return paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
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

        if (configOptionApplicationController.getBooleanValueByKey("Allow to Denomination for shift Starting Process", false)) {
            for (DenominationTransaction dt : getDenominationTransactions()) {
                dt.setBill(currentBill);
                dt.setPayment(currentPayment);
                dt.setPaymentMethod(currentPayment.getPaymentMethod());
            }
        }

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
        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.FUND_SHIFT_START_BILL);
        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());
        currentBill.setDeptId(deptId);
        currentBill.setInsId(deptId);
        findNonClosedShiftStartFundBillIsAvailable();
        if (nonClosedShiftStartFundBill != null) {
            JsfUtil.addErrorMessage("A shift start fund bill is already available for closure.");
            return "";
        }

        List<Payment> payments = new ArrayList();

        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            // Serialize denominations before saving
            p.serializeDenominations();
            paymentController.save(p);
            payments.add(p);
        }

        drawerController.updateDrawerForIns(payments);

        if (configOptionApplicationController.getBooleanValueByKey("Allow to Denomination for shift Starting Process", false)) {
            for (DenominationTransaction dt : getDenominationTransactions()) {
                denominationTransactionController.save(dt);
            }
        }

        return "/cashier/initial_fund_bill_print?faces-redirect=true";
    }

    public String settleIncomeBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.SUPPLEMENTARY_INCOME) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.SUPPLEMENTARY_INCOME);
        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());
        currentBill.setDeptId(deptId);
        currentBill.setInsId(deptId);
        currentBill.setBillClassType(BillClassType.BilledBill);
        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            // Serialize denominations before saving
            p.serializeDenominations();
            paymentController.save(p);
        }
        drawerController.updateDrawerForIns(getCurrentBillPayments());
        return "/cashier/income_bill_print?faces-redirect=true";
    }

    public String settleExpensesBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.OPERATIONAL_EXPENSES) {
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.OPERATIONAL_EXPENSES);
        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());
        currentBill.setDeptId(deptId);
        currentBill.setInsId(deptId);
        currentBill.setBillClassType(BillClassType.BilledBill);
        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            // Serialize denominations before saving
            p.serializeDenominations();
            paymentController.save(p);
        }
        drawerController.updateDrawerForOuts(getCurrentBillPayments());
        return "/cashier/expense_bill_print?faces-redirect=true";
    }

    public String settleFundTransferBill() {
        if (floatTransferStarted) {
            JsfUtil.addErrorMessage("Already Started");
            return "";
        } else {
            floatTransferStarted = true;
        }
        if (currentBill == null) {
            floatTransferStarted = false;
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getBillType() != BillType.FundTransferBill) {
            floatTransferStarted = false;
            JsfUtil.addErrorMessage("Error");
            return "";
        }
        if (currentBill.getToWebUser() == null) {
            floatTransferStarted = false;
            JsfUtil.addErrorMessage("Select to whom to transfer");
            return "";
        }
        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.FUND_TRANSFER_BILL);
        currentBill.setToStaff(currentBill.getToWebUser().getStaff());
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setFromStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setFromWebUser(sessionController.getLoggedUser());
        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());
        currentBill.setDeptId(deptId);
        currentBill.setInsId(deptId);

        billController.save(currentBill);
        double billTotal = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setCreatedAt(new Date());
            p.setCreater(sessionController.getLoggedUser());
            p.setInstitution(null);
            p.setDepartment(null);
            p.setPaidValue(0 - Math.abs(p.getPaidValue()));
            billTotal += p.getPaidValue();
            paymentController.save(p);
            drawerController.updateDrawerForOuts(p);
        }

        currentBill.setTotal(billTotal);
        currentBill.setNetTotal(billTotal);
        currentBill.setDiscount(0.0);
        currentBill.setTax(0.0);
        currentBill.setBalance(billTotal);
        billController.save(currentBill);
        currentBill.getPayments().addAll(currentBillPayments);
        billController.save(currentBill);
        floatTransferStarted = false;
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

    public String navigateToCreateShiftEndSummaryBillForHandover() {
        resetClassVariables();
        Bill startBill = findNonClosedShiftStartFundBill(sessionController.getLoggedUser());
        nonClosedShiftStartFundBill = startBill;

        if (nonClosedShiftStartFundBill == null) {
            JsfUtil.addErrorMessage("No Shift to End");
            return null; // Early exit if no shift to end
        }

        // Initializing the current bill with relevant details
        currentBill = new Bill();
        currentBill.setBillType(BillType.ShiftEndFundBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_END_BILL);
        currentBill.setBillClassType(BillClassType.Bill);
        currentBill.setReferenceBill(nonClosedShiftStartFundBill);

        // Fetching payments from various sources
        List<Payment> shiftPayments = fetchPaymentsFromShiftStartToEndByDateAndDepartment(startBill, null);
        if (shiftPayments != null) {
            shiftPayments.stream()
                    .forEach(p -> p.setTransientPaymentHandover(PaymentHandover.USER_COLLECTED));
        }

        List<Payment> shiftFloats = fetchShiftFloatsFromShiftStartToEnd(startBill, null, sessionController.getLoggedUser());
        if (shiftFloats != null) {
            shiftFloats.stream()
                    .forEach(p -> p.setTransientPaymentHandover(PaymentHandover.FLOATS));
        }

        List<Payment> othersPayments = fetchAllPaymentInMyHold(startBill, sessionController.getLoggedUser());
        if (othersPayments != null) {
            othersPayments.stream()
                    .forEach(p -> p.setTransientPaymentHandover(PaymentHandover.OTHER_USERS_COLLECTED_AND_HANDED_OVER));
        }

        List<Payment> bankPayments = fetchBankPayments(startBill, null, sessionController.getLoggedUser());

        Set<Payment> uniquePaymentSet = new HashSet<>();
        if (shiftPayments != null) {
            uniquePaymentSet.addAll(shiftPayments);
        }
        if (shiftFloats != null) {
            uniquePaymentSet.addAll(shiftFloats);
        }
        if (othersPayments != null) {
            uniquePaymentSet.addAll(othersPayments);
        }
        if (bankPayments != null) {
            uniquePaymentSet.addAll(bankPayments);
        }

        List<Payment> allUniquePayments = new ArrayList<>(uniquePaymentSet);

        bundle = new ReportTemplateRowBundle();
        bundle.setDenominations(sessionController.findDefaultDenominations());
        boolean selectAllHandoverPayments = configOptionApplicationController.getBooleanValueByKey("Select All Payments for Handovers", handoverValuesCreated);
        if (selectAllHandoverPayments) {
            bundle = generatePaymentBundleForHandovers(nonClosedShiftStartFundBill,
                    null,
                    allUniquePayments,
                    PaymentSelectionMode.SELECT_ALL_FOR_HANDOVER_CREATION
            );
        } else {
            bundle = generatePaymentBundleForHandovers(nonClosedShiftStartFundBill,
                    null,
                    allUniquePayments,
                    PaymentSelectionMode.SELECT_NONE_FOR_HANDOVER_CREATION
            );
        }

        bundle.setUser(sessionController.getLoggedUser());
        bundle.aggregateTotalsFromAllChildBundles();
        bundle.setDenominationTransactions(denominationTransactionController.createDefaultDenominationTransaction());
        bundle.collectDepartments();

        return "/cashier/shift_end_for_handover?faces-redirect=true";
    }

    public String navigateToHandoverCreateBill() {
        resetClassVariables();
        handoverValuesCreated = false;
        findNonClosedShiftStartFundBillIsAvailable();
        fillPaymentsFromViewHandoverAcceptBillOld();
        currentBill = new Bill();
        currentBill.setBillType(BillType.CashHandoverCreateBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE);
        currentBill.setBillClassType(BillClassType.PreBill);
        currentBill.setReferenceBill(null);
        return "/cashier/handover_start?faces-redirect=true";
    }

    public String navigateToHandoverCreateBillForCurrentShift() {
        resetClassVariables();
        Bill startBill = fetchNonClosedShiftStartFundBill();
        bundle = new ReportTemplateRowBundle();
        if (startBill == null) {
            JsfUtil.addErrorMessage("Shift not yet started.");
            return null;
        }

        List<Payment> shiftPayments = fetchPaymentsFromShiftStartToEndByDateAndDepartment(startBill, startBill.getReferenceBill());
        if (shiftPayments != null) {
            shiftPayments.stream()
                    .forEach(p -> p.setTransientPaymentHandover(PaymentHandover.USER_COLLECTED));
        }

        List<Payment> shiftFloats = fetchShiftFloatsFromShiftStartToEnd(startBill, startBill.getReferenceBill(), sessionController.getLoggedUser());
        if (shiftFloats != null) {
            shiftFloats.stream()
                    .forEach(p -> p.setTransientPaymentHandover(PaymentHandover.FLOATS));
        }

        List<Payment> othersPayments = fetchAllPaymentInMyHold(startBill, sessionController.getLoggedUser());
        if (othersPayments != null) {
            othersPayments.stream()
                    .forEach(p -> p.setTransientPaymentHandover(PaymentHandover.OTHER_USERS_COLLECTED_AND_HANDED_OVER));
        }

        Set<Payment> uniquePaymentSet = new HashSet<>();

        if (shiftPayments != null) {
            uniquePaymentSet.addAll(shiftPayments);
        }
        if (shiftFloats != null) {
            uniquePaymentSet.addAll(shiftFloats);
        }
        if (othersPayments != null) {
            uniquePaymentSet.addAll(othersPayments);
        }

        List<Payment> allUniquePayments = new ArrayList<>(uniquePaymentSet);
        boolean selectAllHandoverPayments = configOptionApplicationController.getBooleanValueByKey("Select all payments by default for Handing over of the shift.", false);

        if (shiftPayments != null) {
            uniquePaymentSet.addAll(shiftPayments);
        }
        if (shiftFloats != null) {
            uniquePaymentSet.addAll(shiftFloats);
        }
        if (othersPayments != null) {
            uniquePaymentSet.addAll(othersPayments);
        }

        allUniquePayments = new ArrayList<>(uniquePaymentSet);

        if (selectAllHandoverPayments) {
            bundle = generatePaymentBundleForHandovers(nonClosedShiftStartFundBill,
                    null,
                    allUniquePayments,
                    PaymentSelectionMode.SELECT_ALL_FOR_HANDOVER_CREATION
            );
        } else {
            bundle = generatePaymentBundleForHandovers(nonClosedShiftStartFundBill,
                    null,
                    allUniquePayments,
                    PaymentSelectionMode.SELECT_NONE_FOR_HANDOVER_CREATION
            );
        }

        bundle.setUser(sessionController.getLoggedUser());
        bundle.setStartBill(startBill);
        bundle.selectAllChildBundles();
//        bundle.aggregateTotalsFromAllChildBundles();
        bundle.setDenominationTransactions(denominationTransactionController.createDefaultDenominationTransaction());
        bundle.sortByDateInstitutionSiteDepartmentType();

        bundle.setPatientDepositsAreConsideredInHandingover(getPatientDepositsAreConsideredInHandingover());
        bundle.calculateTotalsByChildBundlesForHandover();
        return "/cashier/handover_start_all?faces-redirect=true";
    }

    public String navigateToRecordShiftEndCash() {
        resetClassVariables();
        Bill startBill = fetchNonClosedShiftStartFundBill();
        bundle = new ReportTemplateRowBundle();
        if (startBill == null) {
            JsfUtil.addErrorMessage("Shift not yet started.");
            return null;
        }
        bundle.setUser(sessionController.getLoggedUser());
        bundle.setStartBill(startBill);
        bundle.setDenominationTransactions(denominationTransactionController.createDefaultDenominationTransaction());
        return "/cashier/shift_end_cash_in_hand?faces-redirect=true";
    }

    public String navigateToHandoverCreateBillForSelectedPeriod() {
        return "/cashier/handover_start_for_period?faces-redirect=true";
    }

    public void processToHandoverCreateBillForSelectedPeriod() {
        resetClassVariables();
        findNonClosedShiftStartFundBillIsAvailable();
        handoverValuesCreated = false;
        bundle = new ReportTemplateRowBundle();
        bundle.setDenominations(sessionController.findDefaultDenominations());

        List<Payment> shiftPayments = fetchPaymentsFromShiftStartToEndByDateAndDepartment(fromDate, toDate, sessionController.getLoggedUser());
        List<Payment> shiftFloats = fetchShiftFloatsFromShiftStartToEnd(fromDate, toDate, sessionController.getLoggedUser());
        List<Payment> othersPayments = fetchAllPaymentInMyHold(fromDate, toDate, sessionController.getLoggedUser());

        Set<Payment> uniquePaymentSet = new HashSet<>();
        if (shiftPayments != null) {
            uniquePaymentSet.addAll(shiftPayments);
        }
        if (shiftFloats != null) {
            uniquePaymentSet.addAll(shiftFloats);
        }
        if (othersPayments != null) {
            uniquePaymentSet.addAll(othersPayments);
        }

        List<Payment> allUniquePayments = new ArrayList<>(uniquePaymentSet);

        boolean selectAllHandoverPayments = configOptionApplicationController.getBooleanValueByKey("Select all payments by default for Handing over of the shift.", false);

        if (selectAllHandoverPayments) {
            bundle = generatePaymentBundleForHandovers(nonClosedShiftStartFundBill,
                    null,
                    allUniquePayments,
                    PaymentSelectionMode.SELECT_ALL_FOR_HANDOVER_CREATION
            );
        } else {
            bundle = generatePaymentBundleForHandovers(nonClosedShiftStartFundBill,
                    null,
                    allUniquePayments,
                    PaymentSelectionMode.SELECT_NONE_FOR_HANDOVER_CREATION
            );
        }
        bundle.setUser(sessionController.getLoggedUser());
        bundle.aggregateTotalsFromAllChildBundles();
        bundle.setDenominationTransactions(denominationTransactionController.createDefaultDenominationTransaction());
        bundle.setCashHandoverValue(0.0);
    }

    public String navigateToHandoverCreateBillForSelectedShift(Bill startBill) {
        resetClassVariables();
        handoverValuesCreated = false;
        bundle = new ReportTemplateRowBundle();
        bundle.setDenominations(sessionController.findDefaultDenominations());

        List<Payment> shiftPayments = fetchPaymentsFromShiftStartToEndByDateAndDepartment(startBill, startBill.getReferenceBill());
        if (shiftPayments != null) {
            shiftPayments.stream()
                    .forEach(p -> p.setTransientPaymentHandover(PaymentHandover.USER_COLLECTED));
        }

        List<Payment> shiftFloats = fetchShiftFloatsFromShiftStartToEnd(startBill, startBill.getReferenceBill(), sessionController.getLoggedUser());
        if (shiftFloats != null) {
            shiftFloats.stream()
                    .forEach(p -> p.setTransientPaymentHandover(PaymentHandover.FLOATS));
        }

        List<Payment> othersPayments = fetchAllPaymentInMyHold(startBill, sessionController.getLoggedUser());
        if (othersPayments != null) {
            othersPayments.stream()
                    .forEach(p -> p.setTransientPaymentHandover(PaymentHandover.OTHER_USERS_COLLECTED_AND_HANDED_OVER));
        }

        Set<Payment> uniquePaymentSet = new HashSet<>();

        if (shiftPayments != null) {
            uniquePaymentSet.addAll(shiftPayments);
        }
        if (shiftFloats != null) {
            uniquePaymentSet.addAll(shiftFloats);
        }
        if (othersPayments != null) {
            uniquePaymentSet.addAll(othersPayments);
        }

        List<Payment> allUniquePayments = new ArrayList<>(uniquePaymentSet);
        boolean selectAllHandoverPayments = configOptionApplicationController.getBooleanValueByKey("Select all payments by default for Handing over of the shift.", false);

        if (selectAllHandoverPayments) {
            bundle = generatePaymentBundleForHandovers(startBill,
                    startBill.getReferenceBill(),
                    allUniquePayments,
                    PaymentSelectionMode.SELECT_ALL_FOR_HANDOVER_CREATION
            );
        } else {
            bundle = generatePaymentBundleForHandovers(startBill,
                    startBill.getReferenceBill(),
                    allUniquePayments,
                    PaymentSelectionMode.SELECT_NONE_FOR_HANDOVER_CREATION
            );
        }
//        bundle = generatePaymentsFromShiftStartToEndToEnterToCashbookFilteredByDateAndDepartment(startBill, startBill.getReferenceBill());
        bundle.setUser(sessionController.getLoggedUser());

        bundle.setDenominations(sessionController.findDefaultDenominations());
        bundle.prepareDenominations();
        bundle.selectAllChildBundles();
        bundle.aggregateTotalsFromAllChildBundles();
//        currentBill = new Bill();
//        currentBill.setBillType(BillType.CashHandoverCreateBill);
//        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE);
//        currentBill.setBillClassType(BillClassType.PreBill);
//        currentBill.setReferenceBill(null);
        return "/cashier/handover_start_all?faces-redirect=true";
    }

    public String navigateToMyShifts() {
        bundle = new ReportTemplateRowBundle();
        fillMyUncompletedShifts(10);
        return "/cashier/my_shifts?faces-redirect=true";
    }

// Original method (kept as-is)
    public void fillMyShifts() {
        fillShifts(null, null, fromDate, toDate, sessionController.getLoggedUser());
    }

// New overloaded method with BillTypeAtomic parameter
    public void fillShifts(Integer count, Boolean completed, Date fromDate, Date toDate, WebUser paramUser, BillTypeAtomic billTypeAtomic) {
        bundle = new ReportTemplateRowBundle();
        String jpql = "Select new com.divudi.core.data.ReportTemplateRow(b) "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.billTypeAtomic=:bta ";

        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("bta", billTypeAtomic);

        if (completed != null) {
            jpql += " and b.referenceBill.completed=:completed ";
            params.put("completed", completed);
        }

        if (paramUser != null) {
            jpql += " and b.creater=:user ";
            params.put("user", paramUser);
        }

        if (fromDate != null && toDate != null) {
            jpql += " and b.createdAt between :fd and :td ";
            params.put("fd", fromDate);
            params.put("td", toDate);
        }

        jpql += " order by b.id ";
        List<ReportTemplateRow> rows;

        if (count != null) {
            rows = (List<ReportTemplateRow>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP, count);
        } else {
            rows = (List<ReportTemplateRow>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        }
        bundle.setReportTemplateRows(rows);
    }

// Existing method modified to call the new one
    public void fillShifts(Integer count, Boolean completed, Date fromDate, Date toDate, WebUser paramUser) {
        fillShifts(count, completed, fromDate, toDate, paramUser, BillTypeAtomic.FUND_SHIFT_START_BILL);
    }

    public void fillMyShifts(Integer count) {
        fillShifts(count, null, null, null, sessionController.getLoggedUser());
    }

    public void fillMyUncompletedShifts(Integer count) {
        fillShifts(count, false, null, null, sessionController.getLoggedUser());
    }

    public void fillMyCompletedShifts() {
        fillShifts(null, true, fromDate, toDate, sessionController.getLoggedUser());
    }

    public void fillMyUncompletedShifts() {
        fillShifts(null, false, fromDate, toDate, sessionController.getLoggedUser());
    }

    public void fillUserShifts() {
        fillShifts(null, null, null, null, user);
    }

    public void fillShiftEndCash() {
        fillShifts(null, null, null, null, user, BillTypeAtomic.FUND_SHIFT_END_CASH_RECORD);
    }

    public void fillUserCompletedShifts() {
        fillShifts(null, true, fromDate, toDate, user);
    }

    public void fillUserUncompletedShifts() {
        fillShifts(null, false, fromDate, toDate, user);
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

    @Deprecated
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
                + "AND p.cashbookEntryStated = :started "
                + "ORDER BY p.id DESC";
        Map<String, Object> m = new HashMap<>();
        m.put("started", false);
        m.put("cr", nonClosedShiftStartFundBill.getCreater());
        m.put("ret", false);
        m.put("cid", nonClosedShiftStartFundBill.getId());
        paymentsFromShiftSratToNow = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);

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

    public void fillPaymentsFromViewHandoverAcceptBillOld() {
        paymentsFromShiftSratToNow = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        String jpql = "SELECT p "
                + "FROM Payment p "
                + "WHERE p.retired = :ret "
                + "AND p.handoverCreatedBill = :hcb ";
        m.put("hcb", selectedBill);

        jpql += "ORDER BY p.id DESC";

        m.put("ret", false);
        paymentsFromShiftSratToNow = paymentFacade.findByJpql(jpql, m);

        atomicBillTypeTotalsByPayments = new AtomicBillTypeTotals();
        currentBillPayments = paymentsFromShiftSratToNow;
        Set<Department> uniqueDepartments = new HashSet<>();
        Set<LocalDate> uniqueDates = new HashSet<>();

        for (Payment p : paymentsFromShiftSratToNow) {
            Bill bill = p.getBill();
            if (bill == null) {
                continue;
            }

            if (bill.getBillTypeAtomic() == null) {
            } else {
                Department dept = bill.getDepartment();
                if (dept != null) {
                    uniqueDepartments.add(dept);
                } else {
                }

                if (p.getCreatedAt() != null) {
                    LocalDate createdDateOnly = p.getCreatedAt().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    uniqueDates.add(createdDateOnly);
                } else {
                }

                atomicBillTypeTotalsByPayments.addOrUpdateAtomicRecord(bill.getBillTypeAtomic(), p.getPaymentMethod(), p.getPaidValue());
            }
        }

        cashbookDepartments = new ArrayList<>(uniqueDepartments);

        // Convert Set<LocalDate> to List<Date>
        cashbookDates = new ArrayList<>();
        for (LocalDate localDate : uniqueDates) {
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            cashbookDates.add(date); // Add converted Date to the list
        }

        financialReportByPayments = new FinancialReport(atomicBillTypeTotalsByPayments);
    }

    public void fillPaymentsFromShiftStartToNowNotYetStartedToEntereToCashbook() {
        paymentsFromShiftSratToNow = new ArrayList<>();
        if (nonClosedShiftStartFundBill == null) {
            return;
        }
        Long shiftStartBillId = nonClosedShiftStartFundBill.getId();
        Map<String, Object> m = new HashMap<>();
        String jpql = "SELECT p "
                + "FROM Payment p "
                + "WHERE p.creater = :cr "
                + "AND p.retired = :ret "
                + "AND p.id > :cid "
                + "AND p.cashbookEntryStated = :started ";
        m.put("started", false);

        jpql += "ORDER BY p.id DESC";

        m.put("cr", nonClosedShiftStartFundBill.getCreater());
        m.put("ret", false);
        m.put("cid", shiftStartBillId);
        paymentsFromShiftSratToNow = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);

        atomicBillTypeTotalsByPayments = new AtomicBillTypeTotals();

        Set<Department> uniqueDepartments = new HashSet<>();
        Set<LocalDate> uniqueDates = new HashSet<>();

        for (Payment p : paymentsFromShiftSratToNow) {
            Bill bill = p.getBill();
            if (bill == null) {
                continue;
            }

            if (bill.getBillTypeAtomic() == null) {
            } else {
                Department dept = bill.getDepartment();
                if (dept != null) {
                    uniqueDepartments.add(dept);
                } else {
                }

                if (p.getCreatedAt() != null) {
                    LocalDate createdDateOnly = p.getCreatedAt().toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();
                    uniqueDates.add(createdDateOnly);
                } else {
                }

                atomicBillTypeTotalsByPayments.addOrUpdateAtomicRecord(bill.getBillTypeAtomic(), p.getPaymentMethod(), p.getPaidValue());
            }
        }

        cashbookDepartments = new ArrayList<>(uniqueDepartments);

        // Convert Set<LocalDate> to List<Date>
        cashbookDates = new ArrayList<>();
        for (LocalDate localDate : uniqueDates) {
            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
            cashbookDates.add(date); // Add converted Date to the list
        }

        financialReportByPayments = new FinancialReport(atomicBillTypeTotalsByPayments);
    }

    public List<Payment> fillPaymentsFromViewHandoverAcceptBill() {
        List<Payment> paymts = new ArrayList<>();
        Map<String, Object> m = new HashMap<>();
        String jpql = "SELECT p "
                + "FROM Payment p "
                + "WHERE p.retired = :ret "
                + "AND p.handoverCreatedBill = :hcb ";
        m.put("hcb", selectedBill);

        jpql += "ORDER BY p.id DESC";

        m.put("ret", false);
        paymts = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        return paymts;

//        atomicBillTypeTotalsByPayments = new AtomicBillTypeTotals();
//        currentBillPayments = paymts;
//        Set<Department> uniqueDepartments = new HashSet<>();
//        Set<LocalDate> uniqueDates = new HashSet<>();
//
//        for (Payment p : paymts) {
//            Bill bill = p.getBill();
//            if (bill == null) {
//                continue;
//            }
//
//            if (bill.getBillTypeAtomic() == null) {
//            } else {
//                Department dept = bill.getDepartment();
//                if (dept != null) {
//                    uniqueDepartments.add(dept);
//                } else {
//                }
//
//                if (p.getCreatedAt() != null) {
//                    LocalDate createdDateOnly = p.getCreatedAt().toInstant()
//                            .atZone(ZoneId.systemDefault())
//                            .toLocalDate();
//                    uniqueDates.add(createdDateOnly);
//                } else {
//                }
//
//                atomicBillTypeTotalsByPayments.addOrUpdateAtomicRecord(bill.getBillTypeAtomic(), p.getPaymentMethod(), p.getPaidValue());
//            }
//        }
//
//        cashbookDepartments = new ArrayList<>(uniqueDepartments);
//
//        // Convert Set<LocalDate> to List<Date>
//        cashbookDates = new ArrayList<>();
//        for (LocalDate localDate : uniqueDates) {
//            Date date = Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
//            cashbookDates.add(date); // Add converted Date to the list
//        }
//
//        financialReportByPayments = new FinancialReport(atomicBillTypeTotalsByPayments);
    }

    public void fillPaymentsFromShiftStartToNowNotYetStartedToEntereToCashbookFilteredByDateAndDepartment() {
        paymentsFromShiftSratToNow = new ArrayList<>();
        if (nonClosedShiftStartFundBill == null) {
            return;
        }
        Long shiftStartBillId = nonClosedShiftStartFundBill.getId();

        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));

        Map<String, Object> m = new HashMap<>();
        String jpql = "SELECT p FROM Payment p JOIN p.bill b "
                + "WHERE p.creater = :cr "
                + "AND p.retired = :ret "
                + "AND p.id > :cid "
                + "AND b.billTypeAtomic IN :btas "
                + "AND p.cashbookEntryStated = :started "
                + "AND FUNCTION('DATE', p.createdAt) = :createdDate "
                + "AND b.department = :dept "
                + "ORDER BY p.id DESC";

        m.put("dept", cashbookDepartment);
        m.put("btas", btas);
        m.put("createdDate", cashbookDate);
        m.put("started", false);
        m.put("cr", nonClosedShiftStartFundBill.getCreater());
        m.put("ret", false);
        m.put("cid", shiftStartBillId);
        paymentsFromShiftSratToNow = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);

// Filter and collect unique cancelled bills
        Set<Bill> cancelledBills = paymentsFromShiftSratToNow.stream()
                .map(Payment::getBill)
                .filter(bill -> bill.getBillClassType() == BillClassType.CancelledBill)
                .collect(Collectors.toSet()); // Using a Set to ensure uniqueness

// Filter and collect unique refunded bills
        Set<Bill> refundedBills = paymentsFromShiftSratToNow.stream()
                .map(Payment::getBill)
                .filter(bill -> bill.getBillClassType() == BillClassType.RefundBill)
                .collect(Collectors.toSet());

// Create bundles for cancelled and refunded bills
        cancelledBundle = new ReportTemplateRowBundle();
        refundBundle = new ReportTemplateRowBundle();

// Add unique cancelled bills to the cancelled bundle
        for (Bill bill : cancelledBills) {
            ReportTemplateRow row = new ReportTemplateRow();
            row.setBill(bill); // Assuming you have a method to set a Bill in ReportTemplateRow
            cancelledBundle.getReportTemplateRows().add(row);
        }

// Add unique refunded bills to the refund bundle
        for (Bill bill : refundedBills) {
            ReportTemplateRow row = new ReportTemplateRow();
            row.setBill(bill); // Assuming you have a method to set a Bill in ReportTemplateRow
            refundBundle.getReportTemplateRows().add(row);
        }

// Optionally, set other properties for the bundles
        cancelledBundle.setName("Cancelled Bills");
        cancelledBundle.setBundleType("cancelledBillBundle");

        refundBundle.setName("Refunded Bills");
        refundBundle.setBundleType("refundBillBundle");
//
        chequeFromShiftSratToNow = paymentsFromShiftSratToNow.stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.Cheque)
                .collect(Collectors.toList());

        cardPaymentsFromShiftSratToNow = paymentsFromShiftSratToNow.stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.Card)
                .collect(Collectors.toList());

        // Create bundles for cash and cheque transactions
        cardTransactionPaymentBundle = new ReportTemplateRowBundle();
        chequeTransactionPaymentBundle = new ReportTemplateRowBundle();

// Add filtered cheque payments to the cheque transaction bundle
        for (Payment currentPayment : chequeFromShiftSratToNow) {
            ReportTemplateRow row = new ReportTemplateRow();
            row.setPayment(currentPayment);
            chequeTransactionPaymentBundle.getReportTemplateRows().add(row);
        }

// Add filtered card payments to the cash transaction bundle
        for (Payment currentPayment : cardPaymentsFromShiftSratToNow) {
            ReportTemplateRow row = new ReportTemplateRow();
            row.setPayment(currentPayment);
            cardTransactionPaymentBundle.getReportTemplateRows().add(row);
        }

// Optionally, set other properties for the bundles
        cardTransactionPaymentBundle.setName("Card Payments");
        cardTransactionPaymentBundle.setBundleType("paymentReportCard");

        chequeTransactionPaymentBundle.setName("Cheque Payments");
        chequeTransactionPaymentBundle.setBundleType("paymentReportCheque");

        atomicBillTypeTotalsByPayments = new AtomicBillTypeTotals();

        for (Payment p : paymentsFromShiftSratToNow) {
            Bill bill = p.getBill();
            if (bill == null) {
                continue;
            }

            if (bill.getBillTypeAtomic() == null) {
            } else {
                atomicBillTypeTotalsByPayments.addOrUpdateAtomicRecord(bill.getBillTypeAtomic(), p.getPaymentMethod(), p.getPaidValue());
            }
        }
        financialReportByPayments = new FinancialReport(atomicBillTypeTotalsByPayments);
        financialReportByPayments.getRefundedCash();
    }

    public String navigateToViewIndividualShiftForHandover() {
        return null;
    }

    public String navigateToAddExcessForShiftForHandover() {
        return null;
    }

    public Double shortageBillTotal(ReportTemplateRowBundle paramBundle, PaymentMethod pm) {
        String jpql = "Select sum(p.paidValue) "
                + " from Payment p "
                + " where p.retired=:ret "
                + " and p.bill.retired=:br"
                + " and p.bill.cancelled=:can"
                + " and p.bill.webUser=:u"
                + " and p.paymentDate=:pd "
                + " and p.department=:dep "
                + " and p.paymentMethod=:pm "
                + " and p.bill.billTypeAtomic=:bta";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("br", false);
        m.put("can", false);
        m.put("u", paramBundle.getUser());
        m.put("pd", paramBundle.getDate());
        m.put("dep", paramBundle.getDepartment());
        m.put("pm", pm);
        m.put("bta", BillTypeAtomic.FUND_SHIFT_SHORTAGE_BILL);
        Double dbl = billFacade.findDoubleByJpql(jpql, m, TemporalType.DATE);
        return dbl;
    }

    public String navigateToMarkShortagesForShiftForHandover() {
        if (bundle == null) {
            JsfUtil.addErrorMessage("No Bundle available for processing.");
            return null;
        }

        prepareToAddNewShiftShortageRecord();
        currentBillPayments = new ArrayList<>();
        boolean hasShortages = false;
        double cashCollected = bundle.getCashValue();
        double cashHandover = bundle.getDenominatorValue();
        double cashDifference = cashCollected - cashHandover;

        // Handle cash shortage/surplus initially if there's a difference.
        if (Math.abs(cashDifference) > 0) {
            Payment pc = new Payment();
            pc.setPaymentMethod(PaymentMethod.Cash);
            pc.setBill(currentBill);
            pc.setPaidValue(0 - Math.abs(cashDifference));
            pc.setCreatedAt(new Date());
            currentBillPayments.add(pc);
        }

        // Process non-cash payments while avoiding null pointers and using the built-in clone method for payment.
        for (ReportTemplateRowBundle cb : bundle.getBundles()) {
            if (cb == null || cb.getPaymentMethod() == null || cb.getPaymentMethod() == PaymentMethod.Cash) {
                continue; // Skip null bundles or cash payments as it's already processed.
            }

            if (cb.getReportTemplateRows() != null) {
                for (ReportTemplateRow r : cb.getReportTemplateRows()) {
                    if (r == null || r.getPayment() == null || r.getPayment().isSelectedForHandover()) {
                        continue; // Skip null rows, payments, or those already selected for handover.
                    }
                    hasShortages = true;
                    Payment newPaymentForShortageBill = r.getPayment().clonePaymentForNewBill();
                    newPaymentForShortageBill.setPaidValue(0 - Math.abs(newPaymentForShortageBill.getPaidValue()));
                    newPaymentForShortageBill.setCreatedAt(new Date());
                    currentBillPayments.add(newPaymentForShortageBill);
                }
            }
        }

        calculateShortageBillTotal();
        return "/cashier/record_shift_shortage"; // Navigation case
    }

    public List<Payment> fetchPaymentsFromShiftStartToEndByDateAndDepartment(
            Bill startBill, Bill endBill) {
        if (startBill == null || startBill.getId() == null || startBill.getCreater() == null) {
            return null;
        }
        WebUser paymentUser = startBill.getCreater();
        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        Map<String, Object> m = new HashMap<>();

        StringBuilder jpqlBuilder = new StringBuilder("SELECT p ")
                .append("FROM Payment p ")
                .append("JOIN p.bill b ")
                .append("WHERE p.creater=:cr ")
                .append("AND p.retired=:ret AND p.id>:sid ")
                .append("AND b.billTypeAtomic IN :btas ");
        m.put("cr", paymentUser);
        m.put("ret", false);
        m.put("sid", startBill.getId());
        m.put("btas", btas);
        if (endBill != null && endBill.getId() != null) {
            jpqlBuilder.append("AND p.id<:eid ");
            m.put("eid", endBill.getId());
        }
        jpqlBuilder
                .append("AND p.cashbookEntryStated =:cbes ")
                .append("AND p.handingOverStarted =:hos ")
                .append("ORDER BY p.createdAt, b.department, p.creater");
        m.put("cbes", false);
        m.put("hos", false);

        String jpql = jpqlBuilder.toString();

        List<Payment> shiftPayments = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        List<Payment> shiftPaymentsToEnd = new ArrayList<>();
        for (Payment p : shiftPayments) {
            WebUser u = p.getCurrentHolder();
            if (u == null) {
                p.setCurrentHolder(paymentUser);
                shiftPaymentsToEnd.add(p);
            } else if (paymentUser.equals(u)) {
                shiftPaymentsToEnd.add(p);
            }
        }
        return shiftPaymentsToEnd;
    }

    public List<Payment> fetchPaymentsFromShiftStartToEndByDateAndDepartment(
            Long startBillId, Long endBillId, WebUser wu) {

        if (startBillId == null) {
            return null;
        }

        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        Map<String, Object> m = new HashMap<>();

        StringBuilder jpqlBuilder = new StringBuilder("SELECT p ")
                .append("FROM Payment p ")
                .append("JOIN p.bill b ")
                .append("WHERE p.creater=:cr ")
                .append("AND p.retired=:ret AND p.id>:sid ")
                .append("AND b.billTypeAtomic IN :btas ");
        m.put("cr", wu);
        m.put("ret", false);
        m.put("sid", startBillId);
        m.put("btas", btas);
        if (endBillId != null) {
            jpqlBuilder.append("AND p.id<:eid ");
            m.put("eid", endBillId);
        }
        jpqlBuilder
                .append("AND p.cashbookEntryStated =:cbes ")
                .append("AND p.handingOverStarted =:hos ")
                .append("ORDER BY p.createdAt, b.department, p.creater");
        m.put("cbes", false);
        m.put("hos", false);

        String jpql = jpqlBuilder.toString();

        List<Payment> shiftPayments = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        List<Payment> shiftPaymentsToEnd = new ArrayList<>();
        for (Payment p : shiftPayments) {
            WebUser u = p.getCurrentHolder();
            if (u == null) {
                p.setCurrentHolder(wu);
                shiftPaymentsToEnd.add(p);
            } else if (wu.equals(u)) {
                shiftPaymentsToEnd.add(p);
            }
        }
        return shiftPaymentsToEnd;
    }

    public List<Payment> fetchPaymentsFromShiftStartToEndByDateAndDepartment(
            Date fromDate, Date toDate, WebUser wu) {

        if (fromDate == null) {
            return null;
        }

        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        Map<String, Object> m = new HashMap<>();

        StringBuilder jpqlBuilder = new StringBuilder("SELECT p ")
                .append("FROM Payment p ")
                .append("JOIN p.bill b ")
                .append("WHERE p.creater=:cr ")
                .append("AND p.retired=:ret AND p.bill.createdAt>:sid ")
                .append("AND b.billTypeAtomic IN :btas ");
        m.put("cr", wu);
        m.put("ret", false);
        m.put("sid", fromDate);
        m.put("btas", btas);
        if (toDate != null) {
            jpqlBuilder.append("AND p.bill.createdAt<:eid ");
            m.put("eid", toDate);
        }
        jpqlBuilder
                .append("AND p.cashbookEntryStated =:cbes ")
                .append("AND p.handingOverStarted =:hos ")
                .append("ORDER BY p.createdAt, b.department, p.creater");
        m.put("cbes", false);
        m.put("hos", false);

        String jpql = jpqlBuilder.toString();

        List<Payment> shiftPayments = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        List<Payment> shiftPaymentsToEnd = new ArrayList<>();
        for (Payment p : shiftPayments) {
            WebUser u = p.getCurrentHolder();
            if (u == null) {
                p.setCurrentHolder(wu);
                shiftPaymentsToEnd.add(p);
            } else if (wu.equals(u)) {
                shiftPaymentsToEnd.add(p);
            }
        }
        return shiftPaymentsToEnd;
    }

    public List<Payment> fetchShiftFloatsFromShiftStartToEnd(
            Bill startBill, Bill endBill, WebUser wu) {
        if (startBill == null || startBill.getId() == null || startBill.getCreater() == null) {
            return null;
        }

        WebUser paymentUser = startBill.getCreater();

        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_CLOSING_BALANCE));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_DECREASE));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_INCREASE));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_CHANGE));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_STARTING_BALANCE));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.BANK_OUT));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.BANK_IN));

        Map<String, Object> m = new HashMap<>();

        StringBuilder jpqlBuilder = new StringBuilder("SELECT p ")
                .append("FROM Payment p JOIN p.bill b ")
                .append("WHERE p.retired=:pr ")
                .append("AND b.retired=:br ")
                .append("AND b.billTypeAtomic IN :btas  ")
                .append("AND p.creater=:cr ")
                .append("AND p.cancelled=:can ")
                .append("AND p.id > :sid ");
        m.put("btas", btas);
        m.put("cr", paymentUser);
        m.put("pr", false);
        m.put("br", false);
        m.put("can", false);
        m.put("sid", startBill.getId());

        jpqlBuilder
                .append("AND p.cashbookEntryStated =:cbes ")
                .append("AND p.handingOverStarted =:hos ");
        m.put("cbes", false);
        m.put("hos", false);

        if (endBill != null && endBill.getId() != null) {
            jpqlBuilder.append("AND p.id < :eid ");
            m.put("eid", endBill.getId());
        }
        jpqlBuilder.append("ORDER BY p.createdAt, b.department, p.creater");
        String jpql = jpqlBuilder.toString();
        List<Payment> allFloats = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        List<Payment> myFloats = new ArrayList<>();
        for (Payment p : allFloats) {
            WebUser u = p.getCurrentHolder();
            if (u == null) {
                p.setCurrentHolder(paymentUser);
                myFloats.add(p);
            } else if (paymentUser.equals(u)) {
                myFloats.add(p);
            }
        }
        return myFloats;
    }

    public List<Payment> fetchBankPayments(
            Bill startBill, Bill endBill, WebUser wu) {
        if (startBill == null || startBill.getId() == null || startBill.getCreater() == null) {
            return null;
        }

        WebUser paymentUser = startBill.getCreater();

        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.BANK_IN));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.BANK_OUT));

        Map<String, Object> m = new HashMap<>();

        StringBuilder jpqlBuilder = new StringBuilder("SELECT p ")
                .append("FROM Payment p JOIN p.bill b ")
                .append("WHERE p.retired=:pr ")
                .append("AND b.retired=:br ")
                .append("AND b.billTypeAtomic IN :btas  ")
                .append("AND p.creater=:cr ")
                .append("AND p.cancelled=:can ")
                .append("AND p.id > :sid ");
        m.put("btas", btas);
        m.put("cr", paymentUser);
        m.put("pr", false);
        m.put("br", false);
        m.put("can", false);
        m.put("sid", startBill.getId());

        if (endBill != null && endBill.getId() != null) {
            jpqlBuilder.append("AND p.id < :eid ");
            m.put("eid", endBill.getId());
        }
        jpqlBuilder.append("ORDER BY p.createdAt, b.department, p.creater");
        String jpql = jpqlBuilder.toString();
        List<Payment> bankPayments = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        List<Payment> myBankPayments = new ArrayList<>();
        for (Payment p : bankPayments) {
            WebUser u = p.getCurrentHolder();
            if (u == null) {
                p.setCurrentHolder(paymentUser);
                myBankPayments.add(p);
            } else if (paymentUser.equals(u)) {
                myBankPayments.add(p);
            }
        }
        return myBankPayments;
    }

    public List<Payment> fetchShiftFloatsFromShiftStartToEnd(
            Date fromDate, Date toDate, WebUser wu) {
        if (fromDate == null) {
            return null;
        }

        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_CLOSING_BALANCE));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_DECREASE));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_INCREASE));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_CHANGE));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.FLOAT_STARTING_BALANCE));

        Map<String, Object> m = new HashMap<>();

        StringBuilder jpqlBuilder = new StringBuilder("SELECT p ")
                .append("FROM Payment p JOIN p.bill b ")
                .append("WHERE p.retired=:pr ")
                .append("AND b.retired=:br ")
                .append("AND b.billTypeAtomic IN :btas  ")
                .append("AND p.creater=:cr ")
                .append("AND p.cancelled=:can ")
                .append("AND p.bill.createdAt > :sid ");
        m.put("btas", btas);
        m.put("cr", wu);
        m.put("pr", false);
        m.put("br", false);
        m.put("can", false);
        m.put("sid", fromDate);

        if (toDate != null) {
            jpqlBuilder.append("AND p.bill.createdAt < :eid ");
            m.put("eid", toDate);
        }
        jpqlBuilder.append("ORDER BY p.createdAt, b.department, p.creater");
        String jpql = jpqlBuilder.toString();
        List<Payment> allFloats = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        List<Payment> myFloats = new ArrayList<>();
        for (Payment p : allFloats) {
            WebUser u = p.getCurrentHolder();
            if (u == null) {
                p.setCurrentHolder(wu);
                myFloats.add(p);
            } else if (wu.equals(u)) {
                myFloats.add(p);
            }
        }
        return myFloats;
    }

    public List<Payment> fetchAllPaymentInMyHold(Bill shiftStartBill, WebUser wu) {
        WebUser paymentUser = wu;
        StringBuilder jpqlBuilder = new StringBuilder("SELECT p ")
                .append("FROM Payment p ")
                .append("JOIN p.bill b ")
                .append("WHERE p.retired=:pret ")
                .append("AND p.currentHolder=:cr ")
                .append("AND p.retired=:bret ")
                .append("AND p.cancelled=:can ")
                .append("AND p.handingOverStarted=:hs ")
                .append("ORDER BY p.createdAt, b.department, p.creater");
        Map<String, Object> params = new HashMap<>();
        params.put("cr", paymentUser);
        params.put("pret", false);
        params.put("bret", false);
        params.put("can", false);
        params.put("hs", false);
        String jpql = jpqlBuilder.toString();
        List<Payment> initialOtherPayments = paymentFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        List<Payment> finalOtherPayments = new ArrayList<>();
        if (initialOtherPayments != null) {
            for (Payment p : initialOtherPayments) {
                if (p.getBill() == null) {
                    continue;
                }
                if (p.getBill().getBillTypeAtomic() != null && p.getBill().getBillTypeAtomic() == BillTypeAtomic.FUND_TRANSFER_RECEIVED_BILL) {
                    if (p.getId() > shiftStartBill.getId()) {
                        finalOtherPayments.add(p);
                    }
                } else {
                    finalOtherPayments.add(p);
                }
            }
        }
        return finalOtherPayments;
    }

    public List<Payment> fetchAllPaymentInMyHold(Date paramFromDate, Date paramToDate, WebUser wu) {
        WebUser paymentUser = wu;
        StringBuilder jpqlBuilder = new StringBuilder("SELECT p ")
                .append("FROM Payment p ")
                .append("JOIN p.bill b ")
                .append("WHERE p.retired=:pret ")
                .append("AND p.currentHolder=:cr ")
                .append("AND p.retired=:bret ")
                .append("AND p.cancelled=:can ")
                .append("AND p.handingOverStarted=:hs ")
                .append("ORDER BY p.createdAt, b.department, p.creater");
        Map<String, Object> params = new HashMap<>();
        params.put("cr", paymentUser);
        params.put("pret", false);
        params.put("bret", false);
        params.put("can", false);
        params.put("hs", false);
        String jpql = jpqlBuilder.toString();
        List<Payment> initialOtherPayments = paymentFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
        List<Payment> finalOtherPayments = new ArrayList<>();
        if (initialOtherPayments != null) {
            for (Payment p : initialOtherPayments) {
                if (p.getBill() == null) {
                    continue;
                }
                if (p.getBill().getBillTypeAtomic() != null && p.getBill().getBillTypeAtomic() == BillTypeAtomic.FUND_TRANSFER_RECEIVED_BILL) {
                    if (p.getCreatedAt().getTime() > paramFromDate.getTime() && p.getCreatedAt().getTime() < paramToDate.getTime()) {
                        finalOtherPayments.add(p);
                    }
                } else {
                    finalOtherPayments.add(p);
                }
            }
        }
        return finalOtherPayments;
    }

    public ReportTemplateRowBundle generatePaymentBundleForHandovers(
            Bill startBill,
            Bill endBill,
            List<Payment> shiftPayments,
            PaymentSelectionMode selectionMode) {

        Map<String, ReportTemplateRowBundle> groupedBundles = new HashMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        if (shiftPayments != null) {
            for (Payment p : shiftPayments) {
                // Retrieve the payment handover type
                PaymentHandover ph = p.getTransientPaymentHandover(); // Assuming a getter that returns the enum

                // Generate key with fallback values
                String dateKey = (p.getCreatedAt() != null) ? sdf.format(p.getCreatedAt()) : "No Date";
                String deptKey = (p.getDepartment() != null && p.getDepartment().getId() != null) ? p.getDepartment().getId().toString() : "No Department";
                String userKey = (p.getCreater() != null && p.getCreater().getId() != null) ? p.getCreater().getId().toString() : "No User";
                String webUserKey = (p.getCurrentHolder() != null && p.getCurrentHolder().getId() != null) ? p.getCurrentHolder().getId().toString() : "No WebUser";
                String handoverKey = (ph != null) ? ph.name() : "No Handover"; // Use the enum name as part of the key

                String key = String.join("-", dateKey, deptKey, userKey, webUserKey, handoverKey);

                ReportTemplateRowBundle b = groupedBundles.getOrDefault(key, new ReportTemplateRowBundle());
                b.setDenominations(sessionController.findDefaultDenominations());

                b.setDate(p.getCreatedAt() != null ? p.getCreatedAt() : new Date());
                b.setDepartment(p.getDepartment() != null ? p.getDepartment() : new Department());
                b.setUser(p.getCreater() != null ? p.getCreater() : new WebUser());
                b.setPaymentHandover(ph);  // Set the payment handover attribute

                ReportTemplateRow r = new ReportTemplateRow();
                r.setPayment(p);
                Boolean selected = null;

                switch (selectionMode) {
                    case SELECT_ALL_FOR_HANDOVER_CREATION:
                        p.setSelectedForHandover(true);
                        selected = true;
                        break;
                    case SELECT_NONE_FOR_HANDOVER_CREATION:
                        selected = false;
                        p.setSelectedForHandover(false);
                        break;
                    case SELECT_FOR_HANDOVER_RECEIPT:
                        selected = p.isSelectedForHandover();
                        p.setSelectedForCashbookEntry(selected);
                        break;
                    case SELECT_FOR_HANDOVER_RECORD:
                        selected = p.isSelectedForRecording();
                        break;
                    case SELECT_FOR_HANDOVER_RECORD_CONFIRMATION:
                        selected = p.isSelectedForRecordingConfirmation();
                        break;
                    case NONE:
                        break;
                }

                if (p.getPaymentMethod() == PaymentMethod.Cash) {
                    r.setSelected(true);
                } else {
                    if (selected != null) {
                        r.setSelected(selected);
                    }
                }
                b.getReportTemplateRows().add(r);

                // Store the bundle in the map
                groupedBundles.put(key, b);
            }

            // Calculate totals for each bundle
            for (ReportTemplateRowBundle tmpBundle : groupedBundles.values()) {
                tmpBundle.calculateTotalsBySelectedPayments();
            }
        }

        ReportTemplateRowBundle bundleToHoldDeptUserDayBundle = new ReportTemplateRowBundle();
        bundleToHoldDeptUserDayBundle.setBundles(new ArrayList<>(groupedBundles.values()));
        bundleToHoldDeptUserDayBundle.setStartBill(startBill);
        bundleToHoldDeptUserDayBundle.setEndBill(endBill);
        if (startBill != null) {
            bundleToHoldDeptUserDayBundle.setUser(startBill.getCreater());
        } else {
            bundleToHoldDeptUserDayBundle.setUser(sessionController.getLoggedUser());
        }

        return bundleToHoldDeptUserDayBundle;
    }

    @Deprecated // Use fetchPaymentsFromShiftStartToEndByDateAndDepartment and generatePaymentBundleForHandovers
    public ReportTemplateRowBundle generatePaymentsFromShiftStartToEndToEnterToCashbookFilteredByDateAndDepartment(
            Bill startBill, Bill endBill) {
        if (startBill == null || startBill.getId() == null || startBill.getCreater() == null) {
            return null;
        }

        WebUser user = startBill.getCreater();

        List<BillTypeAtomic> btas = new ArrayList<>();
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        btas.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Map<String, Object> m = new HashMap<>();
        m.put("btas", btas);
        m.put("started", false);
        m.put("cr", user);
        m.put("ret", false);
        m.put("sid", startBill.getId());

        StringBuilder jpqlBuilder = new StringBuilder("SELECT p FROM Payment p JOIN p.bill b WHERE p.creater = :cr ")
                .append("AND p.retired = :ret AND p.id > :sid ");

        if (endBill != null && endBill.getId() != null) {
            jpqlBuilder.append("AND p.id < :eid ");
            m.put("eid", endBill.getId());
        }

        jpqlBuilder.append("AND b.billTypeAtomic IN :btas AND p.cashbookEntryStated = :started ")
                .append("ORDER BY p.createdAt, b.department, p.creater");

        String jpql = jpqlBuilder.toString();

        List<Payment> shiftPayments = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        // To hold grouped data
        Map<String, ReportTemplateRowBundle> groupedBundles = new HashMap<>();

        for (Payment p : shiftPayments) {
            String key = sdf.format(p.getCreatedAt()) + "-" + p.getDepartment().getId() + "-" + p.getCreater().getId();

            ReportTemplateRowBundle b = groupedBundles.getOrDefault(key, new ReportTemplateRowBundle());
            b.setUser(user);
            b.setDate(p.getCreatedAt());
            b.setDepartment(p.getDepartment());

            ReportTemplateRow r = new ReportTemplateRow();
            r.setPayment(p);
            b.getReportTemplateRows().add(r);

            // Temporarily store the bundle
            groupedBundles.put(key, b);
        }

        // Calculate totals once all payments have been grouped
        for (ReportTemplateRowBundle tmpBundle : groupedBundles.values()) {
            tmpBundle.calculateTotalsBySelectedPayments();
        }

        ReportTemplateRowBundle bundleToHoldDeptUserDayBundle = new ReportTemplateRowBundle();
        bundleToHoldDeptUserDayBundle.setBundles(new ArrayList<>(groupedBundles.values()));
        bundleToHoldDeptUserDayBundle.setStartBill(startBill);
        bundleToHoldDeptUserDayBundle.setEndBill(endBill);
        return bundleToHoldDeptUserDayBundle;
    }

    public void fillPaymentsForDateRange() {
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
        paymentsFromShiftSratToNow = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
        atomicBillTypeTotalsByPayments = new AtomicBillTypeTotals();
        for (Payment p : paymentsFromShiftSratToNow) {
            if (p.getBill().getBillTypeAtomic() == null) {
            } else {
                atomicBillTypeTotalsByPayments.addOrUpdateAtomicRecord(p.getBill().getBillTypeAtomic(), p.getPaymentMethod(), p.getPaidValue());
            }
        }
        financialReportByPayments = new FinancialReport(atomicBillTypeTotalsByPayments);
    }

    public void fillPayments(Date fromDateParam, Date toDateParam, WebUser user) {
        paymentsFromShiftSratToNow = new ArrayList<>();
        if (fromDateParam == null) {
            JsfUtil.addErrorMessage("No Start Date");
            return;
        }
        if (toDateParam == null) {
            JsfUtil.addErrorMessage("No End Date");
            return;
        }

        String jpql = "SELECT p "
                + "FROM Payment p "
                + "WHERE p.retired = :ret "
                + "AND p.createdAt > :sid "
                + "AND p.createdAt < :eid ";
        Map<String, Object> m = new HashMap<>();
        if (user != null) {
            jpql += " and p.creater = :cr ";
            m.put("cr", user);
        }

        m.put("ret", false);
        m.put("sid", fromDateParam);
        m.put("eid", toDateParam);

        paymentsFromShiftSratToNow = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
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

        paymentsFromShiftSratToNow = paymentFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
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
        currentBills = billFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);
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

    public Bill fetchNonClosedShiftStartFundBill() {
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
        return billFacade.findFirstByJpql(jpql, m);
    }

    public Bill findNonClosedShiftStartFundBill(WebUser user) {
        nonClosedShiftStartFundBill = null;
        String jpql = "select b "
                + " from Bill b "
                + " where b.creater=:user "
                + " and b.retired=:ret "
                + " and b.billType=:ofb "
                + " and b.referenceBill is null";
        Map m = new HashMap();
        m.put("user", user);
        m.put("ret", false);
        m.put("ofb", BillType.ShiftStartFundBill);
        return billFacade.findFirstByJpql(jpql, m);
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
        shiftStartFundBill = billFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP);

    }

    public String settleShiftEnd() {
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

        nonClosedShiftStartFundBill = currentBill.getReferenceBill();

        boolean mustReceiveAllFundTransfersBeforeClosingShift;
        boolean mustWaitUntilOtherUserAcceptsAllFundTransfersBeforeClosingShift;
        boolean mustReceiveAllHandoversBeforeClosingShift;
        boolean mustWaitUntilOtherUserAcceptsAllHandoversBeforeClosingShift;

        mustReceiveAllFundTransfersBeforeClosingShift = configOptionApplicationController
                .getBooleanValueByKey("Must Receive All Fund Transfers Before Closing Shift", false);

        mustWaitUntilOtherUserAcceptsAllFundTransfersBeforeClosingShift = configOptionApplicationController
                .getBooleanValueByKey("Must Wait Until Other User Accepts All Fund Transfers Before Closing Shift", false);

        mustReceiveAllHandoversBeforeClosingShift = configOptionApplicationController
                .getBooleanValueByKey("Must Receive All Handovers Before Closing Shift", false);

        mustWaitUntilOtherUserAcceptsAllHandoversBeforeClosingShift = configOptionApplicationController
                .getBooleanValueByKey("Must Wait Until Other User Accepts All Handovers Before Closing Shift", false);

        if (fundTransferBillsToReceive != null && !fundTransferBillsToReceive.isEmpty()) {
            fundTransferBillTocollect = true;
        }

        if (fundTransferBillTocollect) {
            JsfUtil.addErrorMessage("Please collect funds transferred to you before closing.");
            return "";
        }

        if (mustReceiveAllHandoversBeforeClosingShift) {
            boolean haveHandoversForMeToReceive = hasAtLeastOneHandoverBillToReceive(null, null, sessionController.getLoggedUser(), null);
            if (haveHandoversForMeToReceive) {
                JsfUtil.addErrorMessage("There are Handovers FOr You to Receive, Please accept them before closing the shift.");
                return null;
            }
        }

        if (mustWaitUntilOtherUserAcceptsAllFundTransfersBeforeClosingShift) {
            boolean haveHandoversToBeReceived = hasAtLeastOneHandoverBillToReceive(sessionController.getLoggedUser(), null, null, null);
            if (haveHandoversToBeReceived) {
                JsfUtil.addErrorMessage("There are Handovers you have created yet to be Received by another user, Please ask the other user to accept them. Until they accept your handovers, you can not close your shift.");
                return null;
            }
        }

        //ToDo: more checks for others . Will have individual issues 
        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.FUND_SHIFT_END_BILL);
        currentBill.setDeptId(deptId);
        currentBill.setInsId(deptId);
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setWebUser(sessionController.getLoggedUser());
        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());
        currentBill.setTotal(bundle.getTotal());
        currentBill.setNetTotal(bundle.getTotal());
        billController.save(currentBill);
        DetailedFinancialBill fb = new DetailedFinancialBill();
        fb.setBill(currentBill);

        fb.setAgentValue(bundle.getAgentValue());
        fb.setCashValue(bundle.getCashValue());
        fb.setCardValue(bundle.getCardValue());
        fb.setChequeValue(bundle.getChequeValue());
        fb.setSlipValue(bundle.getSlipValue());
        fb.setEwalletValue(bundle.getEwalletValue());
        fb.setOnCallValue(bundle.getOnCallValue());
        fb.setMultiplePaymentMethodsValue(bundle.getMultiplePaymentMethodsValue());
        fb.setStaffValue(bundle.getStaffValue());
        fb.setCreditValue(bundle.getCreditValue());
        fb.setStaffWelfareValue(bundle.getStaffWelfareValue());
        fb.setVoucherValue(bundle.getVoucherValue());
        fb.setIouValue(bundle.getIouValue());
        fb.setPatientDepositValue(bundle.getPatientDepositValue());
        fb.setPatientPointsValue(bundle.getPatientPointsValue());
        fb.setOnlineSettlementValue(bundle.getOnlineSettlementValue());

        detailedFinancialBillController.save(fb);

        currentDetailedFinancialBill = fb;

        nonClosedShiftStartFundBill.setReferenceBill(currentBill);
        nonClosedShiftStartFundBill.setCompleted(true);
        billController.save(nonClosedShiftStartFundBill);

        return "/cashier/shift_end_print?faces-redirect=true";
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

        BillComponent bcCollectedCash = new BillComponent();
        bcCollectedCash.setName("Collected Cash");
        bcCollectedCash.setComponentValue(financialReportByPayments.getCollectedCash());
        bcCollectedCash.setBill(currentBill);
        billComponentFacade.create(bcCollectedCash);

        BillComponent bcRefundedCash = new BillComponent();
        bcRefundedCash.setName("Refunded Cash");
        bcRefundedCash.setComponentValue(financialReportByPayments.getRefundedCash());
        bcRefundedCash.setBill(currentBill);
        billComponentFacade.create(bcRefundedCash);

        BillComponent bcNetCashTotal = new BillComponent();
        bcNetCashTotal.setName("Net Cash Total");
        bcNetCashTotal.setComponentValue(financialReportByPayments.getNetCashTotal());
        bcNetCashTotal.setBill(currentBill);
        billComponentFacade.create(bcNetCashTotal);

        BillComponent bcCollectedCreditCard = new BillComponent();
        bcCollectedCreditCard.setName("Collected Credit Card");
        bcCollectedCreditCard.setComponentValue(financialReportByPayments.getCollectedCreditCard());
        bcCollectedCreditCard.setBill(currentBill);
        billComponentFacade.create(bcCollectedCreditCard);

        BillComponent bcRefundedCreditCard = new BillComponent();
        bcRefundedCreditCard.setName("Refunded Credit Card");
        bcRefundedCreditCard.setComponentValue(financialReportByPayments.getRefundedCreditCard());
        bcRefundedCreditCard.setBill(currentBill);
        billComponentFacade.create(bcRefundedCreditCard);

        BillComponent bcNetCreditCardTotal = new BillComponent();
        bcNetCreditCardTotal.setName("Net Credit Card Total");
        bcNetCreditCardTotal.setComponentValue(financialReportByPayments.getNetCreditCardTotal());
        bcNetCreditCardTotal.setBill(currentBill);
        billComponentFacade.create(bcNetCreditCardTotal);

        BillComponent bcCollectedVoucher = new BillComponent();
        bcCollectedVoucher.setName("Collected Voucher");
        bcCollectedVoucher.setComponentValue(financialReportByPayments.getCollectedVoucher());
        bcCollectedVoucher.setBill(currentBill);
        billComponentFacade.create(bcCollectedVoucher);

        BillComponent bcRefundedVoucher = new BillComponent();
        bcRefundedVoucher.setName("Refunded Voucher");
        bcRefundedVoucher.setComponentValue(financialReportByPayments.getRefundedVoucher());
        bcRefundedVoucher.setBill(currentBill);
        billComponentFacade.create(bcRefundedVoucher);

        BillComponent bcNetVoucherTotal = new BillComponent();
        bcNetVoucherTotal.setName("Net Voucher Total");
        bcNetVoucherTotal.setComponentValue(financialReportByPayments.getNetVoucherTotal());
        bcNetVoucherTotal.setBill(currentBill);
        billComponentFacade.create(bcNetVoucherTotal);

        BillComponent bcCollectedOtherNonCredit = new BillComponent();
        bcCollectedOtherNonCredit.setName("Collected Other Non-Credit");
        bcCollectedOtherNonCredit.setComponentValue(financialReportByPayments.getCollectedOtherNonCredit());
        bcCollectedOtherNonCredit.setBill(currentBill);
        billComponentFacade.create(bcCollectedOtherNonCredit);

        BillComponent bcRefundedOtherNonCredit = new BillComponent();
        bcRefundedOtherNonCredit.setName("Refunded Other Non-Credit");
        bcRefundedOtherNonCredit.setComponentValue(financialReportByPayments.getRefundedOtherNonCredit());
        bcRefundedOtherNonCredit.setBill(currentBill);
        billComponentFacade.create(bcRefundedOtherNonCredit);

        BillComponent bcNetOtherNonCreditTotal = new BillComponent();
        bcNetOtherNonCreditTotal.setName("Net Other Non-Credit Total");
        bcNetOtherNonCreditTotal.setComponentValue(financialReportByPayments.getNetOtherNonCreditTotal());
        bcNetOtherNonCreditTotal.setBill(currentBill);
        billComponentFacade.create(bcNetOtherNonCreditTotal);

        BillComponent bcShiftStartFunds = new BillComponent();
        bcShiftStartFunds.setName("Shift Start Funds");
        bcShiftStartFunds.setComponentValue(financialReportByPayments.getShiftStartFunds());
        bcShiftStartFunds.setBill(currentBill);
        billComponentFacade.create(bcShiftStartFunds);

        BillComponent bcFloatReceived = new BillComponent();
        bcFloatReceived.setName("Float Received");
        bcFloatReceived.setComponentValue(financialReportByPayments.getFloatReceived());
        bcFloatReceived.setBill(currentBill);
        billComponentFacade.create(bcFloatReceived);

        BillComponent bcFloatHandover = new BillComponent();
        bcFloatHandover.setName("Float Handover");
        bcFloatHandover.setComponentValue(financialReportByPayments.getFloatHandover());
        bcFloatHandover.setBill(currentBill);
        billComponentFacade.create(bcFloatHandover);

        BillComponent bcBankWithdrawals = new BillComponent();
        bcBankWithdrawals.setName("Bank Withdrawals");
        bcBankWithdrawals.setComponentValue(financialReportByPayments.getBankWithdrawals());
        bcBankWithdrawals.setBill(currentBill);
        billComponentFacade.create(bcBankWithdrawals);

        BillComponent bcBankDeposits = new BillComponent();
        bcBankDeposits.setName("Bank Deposits");
        bcBankDeposits.setComponentValue(financialReportByPayments.getBankDeposits());
        bcBankDeposits.setBill(currentBill);
        billComponentFacade.create(bcBankDeposits);

        BillComponent bcCashCollectedTransferIn = new BillComponent();
        bcCashCollectedTransferIn.setName("Cash Collected Transfer In");
        bcCashCollectedTransferIn.setComponentValue(financialReportByPayments.getCashCollectedTransferIn());
        bcCashCollectedTransferIn.setBill(currentBill);
        billComponentFacade.create(bcCashCollectedTransferIn);

        BillComponent bcCashGivenOutTransferOut = new BillComponent();
        bcCashGivenOutTransferOut.setName("Cash Given Out Transfer Out");
        bcCashGivenOutTransferOut.setComponentValue(financialReportByPayments.getCashGivenOutTransferOut());
        bcCashGivenOutTransferOut.setBill(currentBill);
        billComponentFacade.create(bcCashGivenOutTransferOut);

        BillComponent bcTotal = new BillComponent();
        bcTotal.setName("Total");
        bcTotal.setComponentValue(financialReportByPayments.getTotal());
        bcTotal.setBill(currentBill);
        billComponentFacade.create(bcTotal);

        BillComponent bcShortExcess = new BillComponent();
        bcShortExcess.setName("Short/Excess");
        bcShortExcess.setComponentValue(currentBill.getNetTotal() - financialReportByPayments.getTotal());
        bcShortExcess.setBill(currentBill);
        billComponentFacade.create(bcShortExcess);

        currentBill.getBillComponents().add(bcCollectedCash);
        currentBill.getBillComponents().add(bcRefundedCash);
        currentBill.getBillComponents().add(bcNetCashTotal);
        currentBill.getBillComponents().add(bcCollectedCreditCard);
        currentBill.getBillComponents().add(bcRefundedCreditCard);
        currentBill.getBillComponents().add(bcNetCreditCardTotal);
        currentBill.getBillComponents().add(bcCollectedVoucher);
        currentBill.getBillComponents().add(bcRefundedVoucher);
        currentBill.getBillComponents().add(bcNetVoucherTotal);
        currentBill.getBillComponents().add(bcCollectedOtherNonCredit);
        currentBill.getBillComponents().add(bcRefundedOtherNonCredit);
        currentBill.getBillComponents().add(bcNetOtherNonCreditTotal);
        currentBill.getBillComponents().add(bcShiftStartFunds);
        currentBill.getBillComponents().add(bcFloatReceived);
        currentBill.getBillComponents().add(bcFloatHandover);
        currentBill.getBillComponents().add(bcBankWithdrawals);

        return "/cashier/shift_end_summery_bill_print?faces-redirect=true";
    }

    public String completeHandover() {
        if (bundle == null) {
            JsfUtil.addErrorMessage("Error - Null Bundle");
            return null;
        }
        if (bundle.getStartBill() == null) {
            JsfUtil.addErrorMessage("No Start");
            return null;
        }
        if (bundle.getStartBill().getReferenceBill() == null) {
            JsfUtil.addErrorMessage("Shift NOT ended. Can not complete Handover");
            return null;
        }
        if (bundle.getEndBill() == null) {
            JsfUtil.addErrorMessage("Shift NOT ended. Can not complete Handover");
            return null;
        }

        bundle.getStartBill().setCompleted(true);
        bundle.getStartBill().setCompletedAt(new Date());
        bundle.getStartBill().setCompletedBy(sessionController.getLoggedUser());

        bundle.getEndBill().setCompleted(true);
        bundle.getEndBill().setCompletedAt(new Date());
        bundle.getEndBill().setCompletedBy(sessionController.getLoggedUser());

        billController.save(bundle.getStartBill());
        billController.save(bundle.getEndBill());

        return navigateToMyShifts();
    }

    public String settleHandoverStartBill() {
        if (user == null) {
            JsfUtil.addErrorMessage("Please select a user to handover the shift.");
            return null;
        }
        if (bundle == null) {
            JsfUtil.addErrorMessage("Error - Null Bundle");
            return null;
        }
        if (bundle.getBundles() == null) {
            JsfUtil.addErrorMessage("No Payments");
            return null;
        }
        if (bundle.getBundles().isEmpty()) {
            JsfUtil.addErrorMessage("No Payments to Handover");
            return null;
        }
        Double maximumAllowedCashDifferenceForHandover = configOptionApplicationController.getDoubleValueByKey("Maximum Allowed Cash Difference for Handover", 1.0);
        if (Math.abs(bundle.getDenominatorValue() - bundle.getCashValue()) > maximumAllowedCashDifferenceForHandover) {
            JsfUtil.addErrorMessage("Cash Value Collected and the cash value Handing over are different. Cannot handover.");
            return null;
        }
        boolean shouldSelectAllCollectionsForHandover = configOptionApplicationController.getBooleanValueByKey("Should Select All Collections for Handover", false);
        boolean allBundlesSelected = true;
        boolean anyBundleSelected = false;

        for (ReportTemplateRowBundle b : bundle.getBundles()) {
            if (b.isSelected()) {
                anyBundleSelected = true; // At least one bundle is selected
            } else {
                allBundlesSelected = false; // Found an unselected bundle, not all are selected
            }
        }

        if (!anyBundleSelected) {
            JsfUtil.addErrorMessage("No Payments to Handover");
            return null; // Stop processing since no bundles are selected
        }

        if (shouldSelectAllCollectionsForHandover && !allBundlesSelected) {
            JsfUtil.addErrorMessage("All collections must be selected for handover");
            return null; // Stop processing since not all bundles are selected when they must be
        }

        bundle.setFromUser(sessionController.getLoggedUser());
        bundle.setToUser(user);

        currentBill = new Bill();

        String cbDeptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE);

        currentBill.setDeptId(cbDeptId);
        currentBill.setInsId(cbDeptId);

        currentBill.setBillType(BillType.CashHandoverCreateBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE);
        currentBill.setBillClassType(BillClassType.PreBill);

        currentBill.setReferenceBill(bundle.getStartBill());

        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setFromDate(cashbookDate);

        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setFromDepartment(cashbookDepartment);

        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setFromStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setToStaff(user.getStaff());

        currentBill.setToWebUser(user);
        currentBill.setFromWebUser(sessionController.getLoggedUser());
        currentBill.setWebUser(sessionController.getLoggedUser());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());
        currentBill.setTotal(bundle.getTotal());
        currentBill.setNetTotal(bundle.getTotalOut());

        currentBill.setCreatedAt(new Date());
        currentBill.setCreater(sessionController.getLoggedUser());

        billController.save(currentBill);

        Bill denos = new Bill();
        denos.setDepartment(sessionController.getDepartment());
        denos.setInstitution(sessionController.getDepartment().getInstitution());
        denos.setCreater(sessionController.getLoggedUser());
        denos.setBillDate(new Date());
        denos.setStaff(sessionController.getLoggedUser().getStaff());
        denos.setBillType(BillType.FUND_SHIFT_COMPONANT_HANDOVER_CREATE);
        denos.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_DENOMINATION_HANDOVER_CREATE);
        denos.setDeptId("");
        denos.setInsId("");
        denos.setFromWebUser(sessionController.getLoggedUser());
        denos.setToWebUser(user);
        denos.setReferenceBill(currentBill);
        denos.setCreatedAt(new Date());
        billFacade.create(denos);
        Double cashHandover = 0.0;
        if (bundle.getDenominationTransactions() != null) {
            for (DenominationTransaction dt : bundle.getDenominationTransactions()) {
                dt.setBill(denos);
                if (dt.getDenominationValue() != null) {
                    cashHandover += dt.getDenominationValue();
                }
                denominationTransactionController.save(dt);
            }
        }
        denos.setTotal(cashHandover);
        denos.setNetTotal(cashHandover);
        billFacade.edit(denos);

        drawerController.updateDrawerForOuts(currentBill, PaymentMethod.Cash, cashHandover, sessionController.getLoggedUser());

        for (ReportTemplateRowBundle shiftBundle : bundle.getBundles()) {
            if (shiftBundle.isSelected()) {
                String id = billNumberGenerator.departmentBillNumberGeneratorYearly(department, BillTypeAtomic.FUND_SHIFT_COMPONANT_HANDOVER_CREATE);
                Bill shiftHandoverComponantBill = new Bill();
                shiftHandoverComponantBill.setReferenceNumber(shiftBundle.getBundleType());
                shiftHandoverComponantBill.setDeptId(id);
                shiftHandoverComponantBill.setInsId(id);
                billController.save(shiftHandoverComponantBill);
                shiftHandoverComponantBill.setDepartment(shiftBundle.getDepartment());
                shiftHandoverComponantBill.setInstitution(shiftBundle.getDepartment().getInstitution());
                shiftHandoverComponantBill.setCreater(sessionController.getLoggedUser());
                shiftHandoverComponantBill.setBillDate(shiftBundle.getDate());
                billController.save(shiftHandoverComponantBill);
                shiftHandoverComponantBill.setStaff(shiftBundle.getUser().getStaff());
                shiftHandoverComponantBill.setBillType(BillType.FUND_SHIFT_COMPONANT_HANDOVER_CREATE);
                shiftHandoverComponantBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_COMPONANT_HANDOVER_CREATE);
                billController.save(shiftHandoverComponantBill);
                shiftHandoverComponantBill.setDeptId(id);
                shiftHandoverComponantBill.setInsId(id);
                shiftHandoverComponantBill.setFromWebUser(sessionController.getLoggedUser());
                shiftHandoverComponantBill.setToWebUser(user);
                billController.save(shiftHandoverComponantBill);
                shiftHandoverComponantBill.setReferenceBill(currentBill);
                billController.save(shiftHandoverComponantBill);
                shiftHandoverComponantBill.setCreatedAt(new Date());
                billController.save(shiftHandoverComponantBill);

                Double componantTotal = 0.0;

                for (ReportTemplateRow row : shiftBundle.getReportTemplateRows()) {
                    if (row.getPayment() == null) {
                        continue;
                    }
                    Payment p = row.getPayment();
                    if (p.getPaymentMethod() == null) {
                        continue;
                    }
                    if (p.getPaymentMethod() != PaymentMethod.Cash && p.isSelectedForHandover() == false) {
                        continue;
                    }
                    if (p.getPaymentMethod() == PaymentMethod.Cash && shiftBundle.getSelectAllCashToHandover() == false) {
                        continue;
                    }

                    p.setHandingOverStarted(true);
                    p.setHandingOverCompleted(false);
                    componantTotal += p.getPaidValue();
                    paymentController.save(p);
                    if (p.getPaymentMethod() != PaymentMethod.Cash) {
                        drawerController.updateDrawer(currentBill, 0 - p.getPaidValue(), p.getPaymentMethod(), sessionController.getLoggedUser());
                    }

                    PaymentHandoverItem phi = new PaymentHandoverItem(p);
                    phi.setHandoverCreatedBill(currentBill);
                    phi.setHandoverShiftComponantBill(shiftHandoverComponantBill);
                    phi.setHandoverShiftBill(shiftBundle.getStartBill());
                    paymentHandoverItemController.save(phi);

                }
                shiftHandoverComponantBill.setTotal(componantTotal);
                shiftHandoverComponantBill.setNetTotal(componantTotal);
                billFacade.edit(shiftHandoverComponantBill);
            }
        }

        billController.save(currentBill);
        bundle.setHandoverBill(currentBill);

        return "/cashier/handover_creation_bill_print?faces-redirect=true";
    }

    public String navigateToViewShiftEndCashInHandBill(Bill cashInHandBillToView) {
        if (cashInHandBillToView == null) {
            JsfUtil.addErrorMessage("No Bill");
            return null;
        }
        if (cashInHandBillToView.getBillTypeAtomic() == null) {
            JsfUtil.addErrorMessage("No Bill Type");
            return null;
        }
        if (cashInHandBillToView.getBillTypeAtomic() != BillTypeAtomic.FUND_SHIFT_END_CASH_RECORD) {
            JsfUtil.addErrorMessage("Wrong Bill Type");
            return null;
        }
        bundle = new ReportTemplateRowBundle();
        bundle.setHandoverBill(cashInHandBillToView);
        bundle.setFromUser(cashInHandBillToView.getCreater());
        bundle.setToUser(cashInHandBillToView.getCreater());
        bundle.setUser(cashInHandBillToView.getCreater());
        bundle.setStartBill(cashInHandBillToView.getReferenceBill());
        List<DenominationTransaction> denominationTransactionsInCashHandover = billService.fetchDenominationTransactionFromBill(cashInHandBillToView);
        bundle.setDenominationTransactions(denominationTransactionsInCashHandover);
        return "shift_end_cash_in_hand_print?faces-redirect=true";
    }

    public String settleRecordingShiftEndCashInHand() {
        if (bundle == null) {
            JsfUtil.addErrorMessage("Error - Null Bundle");
            return null;
        }

        bundle.setFromUser(sessionController.getLoggedUser());
        bundle.setToUser(user);

        currentBill = new Bill();

        String cbDeptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.FUND_SHIFT_END_CASH_RECORD);

        currentBill.setDeptId(cbDeptId);
        currentBill.setInsId(cbDeptId);

        currentBill.setBillType(BillType.RecordShiftEndCash);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_END_CASH_RECORD);
        currentBill.setBillClassType(BillClassType.PreBill);

        currentBill.setReferenceBill(bundle.getStartBill());

        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setFromDate(cashbookDate);

        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setFromDepartment(cashbookDepartment);

        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setFromStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setWebUser(sessionController.getLoggedUser());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        currentBill.setCreatedAt(new Date());
        currentBill.setCreater(sessionController.getLoggedUser());

        currentBill.setFromWebUser(sessionController.getLoggedUser());
        currentBill.setToWebUser(user);

        billController.save(currentBill);

        Double cashHandover = 0.0;
        if (bundle.getDenominationTransactions() != null) {
            for (DenominationTransaction dt : bundle.getDenominationTransactions()) {
                dt.setBill(currentBill);
                if (dt.getDenominationValue() != null) {
                    cashHandover += dt.getDenominationValue();
                }
                denominationTransactionController.save(dt);
            }
        }
        currentBill.setTotal(cashHandover);
        currentBill.setNetTotal(cashHandover);

        billController.save(currentBill);
        bundle.setHandoverBill(currentBill);

        return "/cashier/shift_end_cash_in_hand_print?faces-redirect=true";
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
        fundTransferBillsToReceive = fillFundTransferBillsToReceive(null, null, sessionController.getLoggedUser(), null);
        if (fundTransferBillsToReceive != null) {
            fundTransferBillsToReceiveCount = fundTransferBillsToReceive.size();
        } else {
            fundTransferBillsToReceiveCount = 0;
        }
    }

    public List<Bill> fillFundTransferBillsToReceive(WebUser fromUser,
            Staff fromStaff,
            WebUser toUser,
            Staff toStaff) {
        Map<String, Object> params = new HashMap<>();

        StringBuilder jpql = new StringBuilder(
                "select s from Bill s "
                + "where s.retired=:ret "
                + "and s.billTypeAtomic=:btype "
                + "and s.referenceBill is null "
        );

        params.put("ret", false);
        params.put("btype", BillTypeAtomic.FUND_TRANSFER_BILL);

        if (fromStaff != null) {
            jpql.append("and s.fromStaff=:fStaff ");
            params.put("fStaff", fromStaff);
        }
        if (fromUser != null) {
            jpql.append("and s.fromWebUser=:fUser ");
            params.put("fUser", fromUser);
        }
        if (toStaff != null) {
            jpql.append("and s.toStaff=:tStaff ");
            params.put("tStaff", toStaff);
        }
        if (toUser != null) {
            jpql.append("and s.toWebUser=:tUser ");
            params.put("tUser", toUser);
        }

        jpql.append("order by s.createdAt");

        return billFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    public boolean hasAtLeastOneFundTransferBillToReceive(WebUser fromUser,
            Staff fromStaff,
            WebUser toUser,
            Staff toStaff) {
        return hasAtLeastOneToReceived(BillTypeAtomic.FUND_TRANSFER_BILL, fromUser, fromStaff, toUser, toStaff);
    }

    public boolean hasAtLeastOneHandoverBillToReceive(WebUser fromUser,
            Staff fromStaff,
            WebUser toUser,
            Staff toStaff) {
        return hasAtLeastOneToReceived(BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE, fromUser, fromStaff, toUser, toStaff);
    }

    public boolean hasAtLeastOneToReceived(BillTypeAtomic billTypeAtomic,
            WebUser fromUser,
            Staff fromStaff,
            WebUser toUser,
            Staff toStaff) {
        Map<String, Object> params = new HashMap<>();

        StringBuilder jpql = new StringBuilder(
                "select s from Bill s "
                + "where s.retired=:ret "
                + "and s.billTypeAtomic=:btype "
        );

        switch (billTypeAtomic) {
            case FUND_SHIFT_HANDOVER_CREATE:
                jpql.append("and (s.completed = false or s.completed is null) ");
                break;
            case FUND_TRANSFER_BILL:
                jpql.append("and s.referenceBill is null ");
                break;

        }

        params.put("ret", false);
        params.put("btype", billTypeAtomic);

        if (fromStaff != null) {
            jpql.append("and s.fromStaff=:fStaff ");
            params.put("fStaff", fromStaff);
        }
        if (fromUser != null) {
            jpql.append("and s.fromWebUser=:fUser ");
            params.put("fUser", fromUser);
        }
        if (toStaff != null) {
            jpql.append("and s.toStaff=:tStaff ");
            params.put("tStaff", toStaff);
        }
        if (toUser != null) {
            jpql.append("and s.toWebUser=:tUser ");
            params.put("tUser", toUser);
        }

        // Use findFirstByJpql(...) to avoid counting all matching records:
        Bill firstMatch = billFacade.findFirstByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        // If there's at least one matching Bill, return true
        return firstMatch != null;
    }

    public void fillHandoverBillsForMeToReceive() {
        String sql;
        fundTransferBillsToReceive = new ArrayDeque<>();
        handoverBillsToReceiveCount = 0;
        Map tempMap = new HashMap();
        sql = "select s "
                + "from Bill s "
                + "where s.retired=:ret "
                + "and s.billTypeAtomic=:btype "
                + "and s.toWebUser=:user "
                + "and s.completed=:com "
                + "and s.cancelled=:can "
                + "order by s.createdAt ";
        tempMap.put("btype", BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE);
        tempMap.put("ret", false);
        tempMap.put("com", false);
        tempMap.put("can", false);
        tempMap.put("user", sessionController.getLoggedUser());
        handovertBillsToReceive = billFacade.findByJpql(sql, tempMap, TemporalType.TIMESTAMP);

        try {
            handoverBillsToReceiveCount = handovertBillsToReceive.size();
        } catch (Exception e) {
            handoverBillsToReceiveCount = 0;
        }

    }

    public void fillMyHandovers() {
        String jpql;
        currentBills = new ArrayDeque<>();
        Map params = new HashMap();
        jpql = "select s "
                + "from Bill s "
                + "where (s.retired=false or s.retired is null) "
                + "and s.billTypeAtomic=:btype "
                + "and s.fromWebUser=:user "
                + "and s.createdAt between :fd and :td "
                + "order by s.createdAt ";
        params.put("btype", BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE);
        params.put("fd", getFromDate());
        params.put("td", getToDate());
        params.put("user", sessionController.getLoggedUser());
        System.out.println("jpql = " + jpql);
        System.out.println("params = " + params);
        currentBills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public void fillHandovers() {
        String jpql;
        currentBills = new ArrayDeque<>();
        Map params = new HashMap();
        jpql = "select s "
                + "from Bill s "
                + "where (s.retired=false or s.retired is null) "
                + "and s.billTypeAtomic=:btype "
                + "and s.createdAt between :fd and :td ";

        params.put("btype", BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE);
        params.put("fd", getFromDate());
        params.put("td", getToDate());
        if (user != null) {
            jpql += "and s.fromWebUser=:user ";
            params.put("user", user);

        }
        jpql += "order by s.createdAt ";
        System.out.println("jpql = " + jpql);
        System.out.println("params = " + params);
        currentBills = billFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public List<Bill> findHandoverCompletionBills(ReportTemplateRow row) {
        String sql;
        Staff forStaff = row.getUser().getStaff();
        Date forDate = row.getDate();
        Department forDepartment = row.getDepartment();

        List<Bill> bills;
        Map tempMap = new HashMap();
        sql = "select s "
                + "from Bill s "
                + "where s.retired=:ret "
                + "and s.billType=:btype "
                + "and s.toStaff=:logStaff "
                + "and s.toStaff=:logStaff "
                + "and s.referenceBill is null "
                + "order by s.createdAt ";
        tempMap.put("btype", BillType.CashHandoverAcceptBill);
        tempMap.put("ret", false);
        tempMap.put("logStaff", sessionController.getLoggedUser().getStaff());
        bills = billFacade.findByJpql(sql, tempMap, TemporalType.TIMESTAMP);
        return bills;
    }

    public String settleFundTransferReceiveBill() {
        if (floatTransferStarted) {
            JsfUtil.addErrorMessage("Error");
            return "";
        } else {
            floatTransferStarted = true;
        }
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            floatTransferStarted = false;
            return "";
        }

        if (currentBill.getReferenceBill() == null) {
            JsfUtil.addErrorMessage("Error");
            floatTransferStarted = false;
            return "";
        }

        if (currentBill.getBillType() != BillType.FundTransferReceivedBill) {
            JsfUtil.addErrorMessage("Error - bill type");
            floatTransferStarted = false;
            return "";
        }

        if (currentBill.getReferenceBill().getBillType() != BillType.FundTransferBill) {
            floatTransferStarted = false;
            JsfUtil.addErrorMessage("Error - Reference bill type");
            return "";
        }

        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setToStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setFromStaff(currentBill.getReferenceBill().getFromStaff());
        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.FUND_TRANSFER_RECEIVED_BILL);
        currentBill.setDeptId(deptId);
        currentBill.setInsId(deptId);
        billController.save(currentBill);
        double totalValue = 0.0;
        for (Payment p : currentBillPayments) {
            p.setBill(currentBill);
            p.setCurrentHolder(sessionController.getLoggedUser());
            p.setDepartment(null);
            p.setInstitution(null);
            p.setPaidValue(Math.abs(p.getPaidValue()));
            totalValue += p.getPaidValue();
            paymentController.save(p);
            drawerController.updateDrawerForIns(p, sessionController.getLoggedUser());
        }

        currentBill.setTotal(totalValue);
        currentBill.setNetTotal(totalValue);
        currentBill.setDiscount(0);
        currentBill.getPayments().addAll(currentBillPayments);
        currentBill.getReferenceBill().setReferenceBill(currentBill);
        billController.save(currentBill.getReferenceBill());
        floatTransferStarted = false;
        return "/cashier/fund_transfer_receive_bill_print?faces-redirect=true";
    }

//    @Deprecated
//    public String acceptHandoverBill() {
//        if (bundle == null) {
//            JsfUtil.addErrorMessage("Error - Null Bundle");
//            return null;
//        }
//        if (bundle.getBundles() == null) {
//            JsfUtil.addErrorMessage("No Payments");
//            return null;
//        }
//        if (bundle.getBundles().isEmpty()) {
//            JsfUtil.addErrorMessage("No Payments to Handover");
//            return null;
//        }
//        if (selectedBill == null) {
//            JsfUtil.addErrorMessage("Please select a bill.");
//            return null;
//        }
//        if (selectedBill.getBillTypeAtomic() != BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE) {
//            JsfUtil.addErrorMessage("Wrong Bill Type.");
//            return null;
//        }
//        currentBill = new Bill();
//
//        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.FUND_SHIFT_HANDOVER_ACCEPT);
//
//        currentBill.setDeptId(deptId);
//        currentBill.setInsId(deptId);
//
//        currentBill.setBillType(BillType.CashHandoverAcceptBill);
//        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_HANDOVER_ACCEPT);
//        currentBill.setBillClassType(BillClassType.PreBill);
//        currentBill.setReferenceBill(selectedBill);
//
//        currentBill.setDepartment(sessionController.getDepartment());
//        currentBill.setFromDepartment(selectedBill.getDepartment());
//        currentBill.setToDepartment(sessionController.getDepartment());
//
//        currentBill.setInstitution(sessionController.getInstitution());
//        currentBill.setFromInstitution(selectedBill.getInstitution());
//        currentBill.setToInstitution(sessionController.getInstitution());
//
//        currentBill.setFromDate(cashbookDate);
//
//        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
//        currentBill.setFromStaff(selectedBill.getStaff());
//        currentBill.setToStaff(sessionController.getLoggedUser().getStaff());
//
//        currentBill.setToWebUser(sessionController.getLoggedUser());
//        currentBill.setFromWebUser(selectedBill.getCreater());
//        currentBill.setToWebUser(sessionController.getLoggedUser());
//
//        currentBill.setCreatedAt(new Date());
//        currentBill.setCreater(sessionController.getLoggedUser());
//        currentBill.setBillDate(new Date());
//        currentBill.setBillTime(new Date());
//        currentBill.setTotal(bundle.getTotal());
//        currentBill.setNetTotal(bundle.getTotal());
//        billController.save(currentBill);
//
//        List<Payment> payments = new ArrayList();
//        WebUser sender = selectedBill.getCreater();
//        WebUser reciver = sessionController.getLoggedUser();
//
////        System.out.println("Sender = " + sender);
////        System.out.println("Reciver = " + reciver);
//        for (ReportTemplateRowBundle shiftBundle : bundle.getBundles()) {
//            String id = billNumberGenerator.departmentBillNumberGeneratorYearly(department, BillTypeAtomic.FUND_SHIFT_COMPONANT_HANDOVER_CREATE);
//            Bill shiftHandoverComponantAcceptBill = new Bill();
//            shiftHandoverComponantAcceptBill.setDepartment(shiftBundle.getDepartment());
//            shiftHandoverComponantAcceptBill.setInstitution(shiftBundle.getDepartment().getInstitution());
//            shiftHandoverComponantAcceptBill.setCreater(sessionController.getLoggedUser());
//            shiftHandoverComponantAcceptBill.setBillDate(shiftBundle.getDate());
//            shiftHandoverComponantAcceptBill.setStaff(shiftBundle.getUser().getStaff());
//            shiftHandoverComponantAcceptBill.setBillType(BillType.FUND_SHIFT_COMPONANT_HANDOVER_ACCEPT);
//            shiftHandoverComponantAcceptBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_COMPONANT_HANDOVER_ACCEPT);
//            shiftHandoverComponantAcceptBill.setDeptId(id);
//            shiftHandoverComponantAcceptBill.setInsId(id);
//            shiftHandoverComponantAcceptBill.setFromWebUser(sessionController.getLoggedUser());
//            shiftHandoverComponantAcceptBill.setToWebUser(user);
//            shiftHandoverComponantAcceptBill.setReferenceBill(currentBill);
//            shiftHandoverComponantAcceptBill.setCreatedAt(new Date());
//            shiftHandoverComponantAcceptBill.setReferenceNumber(shiftBundle.getBundleType());
//            billFacade.create(shiftHandoverComponantAcceptBill);
////            System.out.println("shiftBundle = " + shiftBundle);
////            System.out.println("shiftBundle.getStartBill() = " + shiftBundle.getStartBill());
//
//            for (ReportTemplateRow row : shiftBundle.getReportTemplateRows()) {
//                //System.out.println("row = " + row);
//                if (row.getPayment() == null) {
//                    //System.out.println("row.getPayment() = " + row.getPayment());
//                    continue;
//                }
//
//                Payment p = row.getPayment();
//                p.setCashbookEntryCompleted(false);
//                p.setHandoverAcceptBill(currentBill);
//                p.setHandoverAcceptComponantBill(shiftHandoverComponantAcceptBill);
//                p.setCurrentHolder(sessionController.getLoggedUser());
//                p.setHandingOverCompleted(true);
//                p.setHandingOverStarted(false);
//                p.setCashbookEntryStated(false);
//                p.setCashbookEntryCompleted(false);
//
//                paymentController.save(p);
//                payments.add(p);
//            }
//        }
//
//        System.out.println("payments = " + payments.size());
//
//        updateDraverForHandoverAccept(payments, reciver, sender);
//
//        billController.save(currentBill);
//
//        selectedBill.setCompleted(true);
//        selectedBill.setCompletedAt(new Date());
//        selectedBill.setCompletedBy(sessionController.getLoggedUser());
//        billController.save(selectedBill);
//
//        return "/cashier/handover_creation_bill_print?faces-redirect=true";
//    }
    public void updateDraverForHandoverAccept(List<Payment> payments, WebUser reciver, WebUser sender) {

        for (Payment p : payments) {
        }

        //System.out.println("Update Resiver Drawer Start");//Accepted Cashier Dravr Update
        drawerController.updateDrawer(payments, reciver);
        //System.out.println("Update Resiver Drawer End");

        //System.out.println("*******************************************");
        //System.out.println("Update Sender Drawer Start");//Sended Cashier Dravr Update
//        drawerController.updateDrawerForOuts(payments, sender);
        //System.out.println("Update Sender Drawer End");
    }

    public String navigateToHandoverReprint() {
        return "/cashier/handover_reprint?faces-redirect=true";
    }

    public String acceptHandoverBillAndWriteToCashbook() {
        if (bundle == null) {
            JsfUtil.addErrorMessage("Error - Null Bundle");
            return null;
        }
        if (bundle.getBundles() == null) {
            JsfUtil.addErrorMessage("No Payments");
            return null;
        }
        if (bundle.getBundles().isEmpty()) {
            JsfUtil.addErrorMessage("No Payments to Handover");
            return null;
        }
        currentBill = new Bill();
        currentBill.setBillType(BillType.CashHandoverAcceptBill);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_HANDOVER_ACCEPT);
        currentBill.setBillClassType(BillClassType.PreBill);
        currentBill.setReferenceBill(selectedBill);

        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.FUND_SHIFT_HANDOVER_ACCEPT);

        currentBill.setDeptId(deptId);
        currentBill.setInsId(deptId);
        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setFromDepartment(cashbookDepartment);
        currentBill.setToDepartment(sessionController.getDepartment());

        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setFromInstitution(sessionController.getInstitution());
        currentBill.setToInstitution(sessionController.getInstitution());

        currentBill.setFromDate(cashbookDate);

        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setToWebUser(user);
        currentBill.setFromWebUser(sessionController.getLoggedUser());
        currentBill.setCreatedAt(new Date());
        currentBill.setCreater(sessionController.getLoggedUser());
        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());
        currentBill.setTotal(bundle.getTotal());
        currentBill.setNetTotal(bundle.getTotal());
        billController.save(currentBill);

        List<Payment> payments = new ArrayList();
        WebUser sender = selectedBill.getCreater();
        WebUser reciver = sessionController.getLoggedUser();

        for (ReportTemplateRowBundle shiftBundle : bundle.getBundles()) {

            CashBook bundleCb = cashBookController.findAndSaveCashBookBySite(shiftBundle.getDepartment().getSite(), shiftBundle.getDepartment().getInstitution(), shiftBundle.getDepartment());
            String id = billNumberGenerator.departmentBillNumberGeneratorYearly(department, BillTypeAtomic.FUND_SHIFT_COMPONANT_HANDOVER_CREATE);
            Bill shiftHandoverComponantAcceptBill = new Bill();
            shiftHandoverComponantAcceptBill.setDepartment(shiftBundle.getDepartment());
            shiftHandoverComponantAcceptBill.setInstitution(shiftBundle.getDepartment().getInstitution());
            shiftHandoverComponantAcceptBill.setCreater(sessionController.getLoggedUser());
            shiftHandoverComponantAcceptBill.setBillDate(shiftBundle.getDate());
            shiftHandoverComponantAcceptBill.setStaff(shiftBundle.getUser().getStaff());
            shiftHandoverComponantAcceptBill.setBillType(BillType.FUND_SHIFT_COMPONANT_HANDOVER_ACCEPT);
            shiftHandoverComponantAcceptBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_COMPONANT_HANDOVER_ACCEPT);
            shiftHandoverComponantAcceptBill.setDeptId(id);
            shiftHandoverComponantAcceptBill.setInsId(id);
            shiftHandoverComponantAcceptBill.setFromWebUser(sessionController.getLoggedUser());
            shiftHandoverComponantAcceptBill.setToWebUser(user);
            shiftHandoverComponantAcceptBill.setReferenceBill(currentBill);
            shiftHandoverComponantAcceptBill.setCreatedAt(new Date());
            billFacade.create(shiftHandoverComponantAcceptBill);

            List<CashBookEntry> cbEntries = new ArrayList<>();
            if (shiftBundle.getDepartment() != null) {
                cbEntries = cashBookEntryController.writeCashBookEntryAtHandover(shiftBundle, shiftHandoverComponantAcceptBill, bundleCb);
            }

            for (ReportTemplateRow row : shiftBundle.getReportTemplateRows()) {
                if (row.getPayment() == null) {
                    continue;
                }

//                if (!row.getPayment().isSelectedForCashbookEntry()) {
//                    continue;
//                }
                Payment p = row.getPayment();
                p.setCashbook(bundleCb);
                p.setCashbookEntry(findCashbookEntry(p, cbEntries));
                p.setCashbookEntryCompleted(true);

                p.setCurrentHolder(sessionController.getLoggedUser());
                p.setHandingOverCompleted(true);
                p.setHandingOverStarted(false);
                p.setCashbookEntryStated(true);
                p.setCashbookEntryCompleted(true);

                paymentController.save(p);

                payments.add(p);

                PaymentHandoverItem phi = new PaymentHandoverItem(p);
                phi.setHandoverAcceptBill(currentBill);
                phi.setHandoverAcceptComponantBill(shiftHandoverComponantAcceptBill);

                paymentHandoverItemController.save(phi);

            }
        }

        updateDraverForHandoverAccept(payments, reciver, sender);

        billController.save(currentBill);

        bundle.setHandoverBill(currentBill);

        selectedBill.setCompleted(true);
        selectedBill.setCompletedAt(new Date());
        selectedBill.setCompletedBy(sessionController.getLoggedUser());
        selectedBill.setBackwardReferenceBill(currentBill);
        billController.save(selectedBill);

        return "/cashier/handover_accept_bill_print?faces-redirect=true";
    }

    public void addMissingBackwordReferancesForShiftStartBills() {
        Bill firstBill = billService.fetchFirstBill();
        Bill lastBill = billService.fetchLastBill();
        Date fd = CommonFunctions.getStartOfDay();
        Date td = CommonFunctions.getEndOfDay();
        if (firstBill != null) {
            if (firstBill.getCreatedAt() != null) {
                fd = CommonFunctions.getStartOfDay(firstBill.getCreatedAt());
            }
        }

        if (lastBill != null) {
            if (lastBill.getCreatedAt() != null) {
                td = CommonFunctions.getEndOfDay(lastBill.getCreatedAt());
            }
        }

        List<Bill> handoverStarts = billService.fetchBills(fd, td, null, null, null, null, BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE);

        if (handoverStarts == null) {
            JsfUtil.addErrorMessage("No bills");
            return;
        }

        for (Bill handoverStartBill : handoverStarts) {
            if (handoverStartBill.getBillTypeAtomic() == null) {
                continue;
            }
            if (handoverStartBill.getBillTypeAtomic() != BillTypeAtomic.FUND_SHIFT_HANDOVER_CREATE) {
                continue;
            }
            if (handoverStartBill.getBackwardReferenceBill() != null) {
                continue;
            }
            Bill handoverAcceptBill = billService.fetchBillReferredAsReferenceBill(handoverStartBill, BillTypeAtomic.FUND_SHIFT_HANDOVER_ACCEPT);
            if (handoverAcceptBill == null) {
                continue;
            }
            if (handoverAcceptBill.getBillTypeAtomic() == null) {
                continue;
            }
            if (handoverAcceptBill.getBillTypeAtomic() != BillTypeAtomic.FUND_SHIFT_HANDOVER_ACCEPT) {
                continue;
            }

            handoverStartBill.setBackwardReferenceBill(handoverAcceptBill);
            billFacade.edit(handoverStartBill);
        }

    }

    public void addMissingBillTimeData() {
        String jpql;
        Map m = new HashMap();
        List<Bill> billsWithoutBillDateAndBillTime;
        jpql = "Select b "
                + "from Bill b "
                + " where b.retired=:ret "
                + " and (b.billDate is null or b.billTime is null) "
                + " and b.createdAt is not null";
        m.put("ret", false);
        billsWithoutBillDateAndBillTime = billFacade.findByJpql(jpql, m);
        for (Bill billWIthoutBillDateAndBillTime : billsWithoutBillDateAndBillTime) {
            billWIthoutBillDateAndBillTime.getBillTime();// Fixed in the getter
            billWIthoutBillDateAndBillTime.getBillDate();// Fixed in the getter
            billFacade.edit(billWIthoutBillDateAndBillTime);
        }

        m = new HashMap();
        List<Bill> billsWithoutCreatedAt;
        jpql = "Select b "
                + "from Bill b "
                + " where b.retired=:ret "
                + " and b.billDate is not null "
                + " and b.createdAt is null";
        m.put("ret", false);
        billsWithoutCreatedAt = billFacade.findByJpql(jpql, m);

        for (Bill billWithoutCreaetedAt : billsWithoutCreatedAt) {
            billWithoutCreaetedAt.getCreatedAt(); // Fixed in the getter
            billFacade.edit(billWithoutCreaetedAt);
        }

    }

    private CashBookEntry findCashbookEntry(Payment p, List<CashBookEntry> cbEntries) {
        if (p == null) {
            return null;
        }
        if (p.getPaymentMethod() == null) {
            return null;
        }
        if (cbEntries == null) {
            return null;
        }
        for (CashBookEntry cbe : cbEntries) {
            if (cbe.getPaymentMethod() == null) {
                continue;
            }
            if (cbe.getPaymentMethod().equals(p)) {
                return cbe;
            }
        }
        return null;
    }

//    public String acceptHandoverBill() {
//        if (currentBill == null) {
//            JsfUtil.addErrorMessage("Error");
//            return "";
//        }
//        if (currentBill.getReferenceBill() == null) {
//            JsfUtil.addErrorMessage("Error");
//            return "";
//        }
//
//        if (currentBill.getBillType() != BillType.CashHandoverAcceptBill) {
//            JsfUtil.addErrorMessage("Error - bill type");
//            return "";
//        }
//
//        if (currentBill.getReferenceBill().getBillType() != BillType.CashHandoverCreateBill) {
//            JsfUtil.addErrorMessage("Error - Reference bill type");
//            return "";
//        }
//
//        currentBill.setDepartment(sessionController.getDepartment());
//        currentBill.setInstitution(sessionController.getInstitution());
//        currentBill.setStaff(sessionController.getLoggedUser().getStaff());
//        currentBill.setToStaff(sessionController.getLoggedUser().getStaff());
//        currentBill.setFromStaff(currentBill.getReferenceBill().getFromStaff());
//        currentBill.setFromDate(currentBill.getReferenceBill().getFromDate());
//        currentBill.setFromDepartment(currentBill.getReferenceBill().getFromDepartment());
//        double total = 0.0;
//        billController.save(currentBill);
//
//        Map<PaymentMethod, Double> paymentMethodTotals = new HashMap<>();
//
//        for (Payment p : currentBillPayments) {
//            PaymentMethod method = p.getPaymentMethod();
//            Double amount = p.getPaidValue();
//            p.setCashbookEntryCompleted(true);
//            p.setHandoverAcceptBill(currentBill);
//            paymentController.save(p);
//            paymentMethodTotals.put(method, paymentMethodTotals.getOrDefault(method, 0.0) + amount);
//            cashBookEntryController.writeCashBookEntryAtHandover(p);
//        }
//
//        // Create the list of PaymentMethodValue
//        List<PaymentMethodValue> pmvs = new ArrayList<>();
//        for (Map.Entry<PaymentMethod, Double> entry : paymentMethodTotals.entrySet()) {
//            PaymentMethodValue pmv = new PaymentMethodValue();
//            pmv.setPaymentMethod(entry.getKey());
//            pmv.setAmount(entry.getValue());
//            pmv.setCreatedAt(new Date());
//            paymentMethodValueFacade.create(pmv);
//            pmvs.add(pmv);
//
//            BillComponent bc = new BillComponent();
//            bc.setName("Collected  " + pmv.getPaymentMethod().getLabel());
//            bc.setComponentValue(pmv.getAmount());
//            bc.setPaymentMethodValue(pmv);
//            bc.setBill(currentBill);
//            billComponentFacade.create(bc);
//
//            currentBill.getBillComponents().add(bc);
//
//        }
//
//        handingOverPaymentMethodValues = pmvs;
//
//        currentBill.getReferenceBill().setReferenceBill(currentBill);
//        currentBill.setNetTotal(total);
//        billController.save(currentBill.getReferenceBill());
//
//        return "/cashier/handover_receive_bill_print?faces-redirect=true";
//    }
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

    public void addPaymentToIncomeBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillTypeAtomic() != BillTypeAtomic.SUPPLEMENTARY_INCOME) {
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
        calculateIncometBillTotal();
        currentPayment = null;
    }

    public void addPaymentToExpenseBill() {
        if (currentBill == null) {
            JsfUtil.addErrorMessage("Error");
            return;
        }
        if (currentBill.getBillTypeAtomic() != BillTypeAtomic.OPERATIONAL_EXPENSES) {
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

        if (currentPayment.getPaymentMethod() == PaymentMethod.Cash) {
            double drawerBalance = getLoggedUserDrawer().getCashInHandValue();
            double paymentAmount = currentPayment.getPaidValue();
            if (configOptionApplicationController.getBooleanValueByKey("Enable Drawer Manegment", true)) {
                if (drawerBalance < paymentAmount) {
                    JsfUtil.addErrorMessage("Not enough cash in your drawer to make this payment");
                    return;
                }
            }
        }

        getCurrentBillPayments().add(currentPayment);
        calculateExpenseBillTotal();
        currentPayment = null;
    }

    public void addShortageRecord() {
        if (currentPayment == null) {
            JsfUtil.addErrorMessage("Please provide valid amount for the shortage.");
            return;
        }
        currentPayment.setPaidValue(0 - Math.abs(currentPayment.getPaidValue()));
        currentPayment.setCreatedAt(new Date()); // Set payment date to now
        currentBillPayments.add(currentPayment); // Add to the current bill's payments list
        calculateShortageBillTotal();
        JsfUtil.addSuccessMessage("Shortage recorded successfully.");
        currentPayment = new Payment(); // Reset currentPayment for the next entry
    }

    public void removeShortageRecord(Payment payment) {
        if (payment == null || !currentBillPayments.remove(payment)) {
            JsfUtil.addErrorMessage("Failed to remove the record or record not found.");
        } else {
            JsfUtil.addSuccessMessage("Record removed successfully.");
        }
    }

    // Method to confirm and settle all recorded shift excesses
    public String settleShiftExcesses() {
        if (currentBill == null || currentBillPayments.isEmpty()) {
            JsfUtil.addErrorMessage("No excess records to settle.");
            return "";  // Staying on the same page due to lack of records
        }

        currentBill.setDepartment(sessionController.getDepartment());
        currentBill.setInstitution(sessionController.getInstitution());
        currentBill.setStaff(sessionController.getLoggedUser().getStaff());

        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        // Assuming a method to save the bill and associated payments
        billController.save(currentBill);
        for (Payment p : currentBillPayments) {
            p.setBill(currentBill);
            paymentController.save(p);
        }
        JsfUtil.addSuccessMessage("All shift excess records have been successfully settled.");
        return "/cashier/record_shift_excess_print?faces-redirect=true";  // Redirect to a summary page or another relevant page
    }

// Method to add a new excess record to the current bill
    public void addExcessRecord() {
        if (currentPayment == null || currentPayment.getPaidValue() <= 0) {
            JsfUtil.addErrorMessage("Please enter a valid excess amount.");
            return;
        }

        currentPayment.setCreatedAt(new Date());
        currentPayment.setPaidValue(Math.abs(currentPayment.getPaidValue()));
        currentBillPayments.add(currentPayment); // Add to current bill's payments list
        calculateExcessBillTotal();
        JsfUtil.addSuccessMessage("Excess record added successfully.");

        currentPayment = new Payment(); // Reset currentPayment for next entry

    }

// Method to remove an excess record
    public void removeExcessRecord(Payment payment) {
        if (payment == null || !currentBillPayments.remove(payment)) {
            JsfUtil.addErrorMessage("Failed to remove the excess record or record not found.");
        } else {
            JsfUtil.addSuccessMessage("Excess record removed successfully.");
        }
    }

    public String settleShiftShortages() {
        if (currentBill == null || currentBillPayments.isEmpty()) {
            JsfUtil.addErrorMessage("No shortage records to settle or no current bill set.");
            return "";  // Stay on the same page due to lack of records or bill
        }

        // Set attributes specific to this kind of bill
        currentBill.setBillType(BillType.ShiftShortage);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_SHIFT_SHORTAGE_BILL);
        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());

        // Set the department, institution, and staff from session
        currentBill.setDepartment(null);
        currentBill.setInstitution(null);
        currentBill.setStaff(null);
        currentBill.setWebUser(null);

        currentBill.setFromDepartment(sessionController.getDepartment());
        currentBill.setFromInstitution(sessionController.getInstitution());
        currentBill.setFromStaff(sessionController.getLoggedUser().getStaff());
        currentBill.setFromWebUser(sessionController.getLoggedUser());

        try {
            billController.save(currentBill);  // Save the bill
            // Save each payment linked to this bill
            for (Payment p : currentBillPayments) {
                p.setBill(currentBill);
                p.setDepartment(null);
                p.setInstitution(null);
                p.setCurrentHolder(sessionController.getLoggedUser());
                paymentController.save(p);
            }

            JsfUtil.addSuccessMessage("All shift shortage records have been successfully recorded.");
            return "/cashier/record_shift_shortage_print?faces-redirect=true";  // Redirect to a summary page
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error settling shift shortages: " + e.getMessage());
            return "";  // Optionally, redirect to an error page
        }
    }

    private void calculateShortageBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    private void calculateExcessBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    private void calculateFundDepositBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    private void calculateIncometBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            total += p.getPaidValue();
        }
        currentBill.setTotal(total);
        currentBill.setNetTotal(total);
    }

    private void calculateExpenseBillTotal() {
        double total = 0.0;
        for (Payment p : getCurrentBillPayments()) {
            double absolutePaymentValue = Math.abs(p.getPaidValue());
            double expenseValue = 0 - absolutePaymentValue;
            p.setPaidValue(expenseValue);
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
        String deptId = billNumberGenerator.departmentBillNumberGeneratorYearly(sessionController.getDepartment(), BillTypeAtomic.FUND_DEPOSIT_BILL);
        currentBill.setBillTypeAtomic(BillTypeAtomic.FUND_DEPOSIT_BILL);
        currentBill.setBillDate(new Date());
        currentBill.setBillTime(new Date());
        currentBill.setInsId(deptId);
        currentBill.setDeptId(deptId);

        Double netTotal = currentBill.getNetTotal();

        boolean drawerManagementIsEnabled = configOptionApplicationController.getBooleanValueByKey("Enable Drawer Manegment", true);

        if (drawerManagementIsEnabled) {
            double drawerBalance = getLoggedUserDrawer().getCashInHandValue();
            double totalPaymentCash = 0.0;
            for (Payment p : getCurrentBillPayments()) {
                if (p.getPaymentMethod() == PaymentMethod.Cash) {
                    totalPaymentCash += p.getPaidValue();
                }
            }
            if (drawerBalance < totalPaymentCash) {
                JsfUtil.addErrorMessage("Not enough cash in your drawer to make this payment");
                return "";
            }
        }

        currentBill.setNetTotal(0 - Math.abs(netTotal));
        currentBill.setTotal(0 - Math.abs(netTotal));
        billController.save(currentBill);
        for (Payment p : getCurrentBillPayments()) {
            p.setBill(currentBill);
            p.setDepartment(sessionController.getDepartment());
            p.setInstitution(sessionController.getInstitution());
            p.setPaidValue(0 - Math.abs(p.getPaidValue()));
            paymentController.save(p);
            drawerController.updateDrawerForOuts(p);
        }
        return "/cashier/deposit_funds_print?faces-redirect=true";
    }

    // </editor-fold>
    // <editor-fold defaultstate="collapsed" desc="WithdrawalFundBill">
    public String navigateToCreateNewFundWithdrawalBill() {
        prepareToAddNewWithdrawalProcessingBill();
        return "/cashier/fund_withdrawal_bill?faces-redirect=true";
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
    public void calculateHandingOverValues() {
        // Map to store total amount for each payment method
        Map<PaymentMethod, Double> paymentMethodTotals = new HashMap<>();
        Double totalValue = 0.0;
        // Loop through selected payments
        for (Payment pmt : paymentsSelected) {
            PaymentMethod method = pmt.getPaymentMethod();
            Double amount = pmt.getPaidValue();
            totalValue += pmt.getPaidValue();

            // Add the amount to the corresponding payment method total
            paymentMethodTotals.put(method, paymentMethodTotals.getOrDefault(method, 0.0) + amount);
        }

        // Create the list of PaymentMethodValue
        List<com.divudi.core.data.PaymentMethodValue> pmvs = new ArrayList<>();
        for (Map.Entry<PaymentMethod, Double> entry : paymentMethodTotals.entrySet()) {
            com.divudi.core.data.PaymentMethodValue pmv = new com.divudi.core.data.PaymentMethodValue();
            pmv.setPaymentMethod(entry.getKey());
            pmv.setAmount(entry.getValue());
            pmv.setCreatedAt(new Date());
            pmvs.add(pmv);
//            paymentMethodValueFacade.create(pmv);
        }

        handingOverPaymentMethodValues = pmvs;
        currentBill.setNetTotal(totalValue);
        handoverValuesCreated = true;
        // Now pmvs contains the total amounts for each payment method
    }

    public void updateCashTaralFromDenomination() {
        double denominationTotal = reportTemplateRowBundle.getCashValue();
        currentPayment.setPaidValue(denominationTotal);
    }

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
//            currentPayment.setCurrencyDenominations(configOptionApplicationController.getDenominations());
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

    public List<BillComponent> getCurrentBillComponents() {
        return currentBillComponents;
    }

    public void setCurrentBillComponents(List<BillComponent> currentBillComponents) {
        this.currentBillComponents = currentBillComponents;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Date getCashbookDate() {
        return cashbookDate;
    }

    public void setCashbookDate(Date cashbookDate) {
        this.cashbookDate = cashbookDate;
    }

    public Department getCashbookDepartment() {
        return cashbookDepartment;
    }

    public void setCashbookDepartment(Department cashbookDepartment) {
        this.cashbookDepartment = cashbookDepartment;
    }

    public List<Date> getCashbookDates() {
        return cashbookDates;
    }

    public void setCashbookDates(List<Date> cashbookDates) {
        this.cashbookDates = cashbookDates;
    }

    public List<Department> getCashbookDepartments() {
        return cashbookDepartments;
    }

    public void setCashbookDepartments(List<Department> cashbookDepartments) {
        this.cashbookDepartments = cashbookDepartments;
    }

    public List<com.divudi.core.data.PaymentMethodValue> getHandingOverPaymentMethodValues() {
        return handingOverPaymentMethodValues;
    }

    public void setHandingOverPaymentMethodValues(List<com.divudi.core.data.PaymentMethodValue> handingOverPaymentMethodValues) {
        this.handingOverPaymentMethodValues = handingOverPaymentMethodValues;
    }

    public List<Bill> getHandovertBillsToReceive() {
        return handovertBillsToReceive;
    }

    public void setHandovertBillsToReceive(List<Bill> handovertBillsToReceive) {
        this.handovertBillsToReceive = handovertBillsToReceive;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }

    public PaymentMethodValueFacade getPaymentMethodValueFacade() {
        return paymentMethodValueFacade;
    }

    public void setPaymentMethodValueFacade(PaymentMethodValueFacade paymentMethodValueFacade) {
        this.paymentMethodValueFacade = paymentMethodValueFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public CashBookEntryController getCashBookEntryController() {
        return cashBookEntryController;
    }

    public void setCashBookEntryController(CashBookEntryController cashBookEntryController) {
        this.cashBookEntryController = cashBookEntryController;
    }

    public ReportTemplateController getReportTemplateController() {
        return reportTemplateController;
    }

    public void setReportTemplateController(ReportTemplateController reportTemplateController) {
        this.reportTemplateController = reportTemplateController;
    }

    public BillController getBillController() {
        return billController;
    }

    public void setBillController(BillController billController) {
        this.billController = billController;
    }

    public PaymentController getPaymentController() {
        return paymentController;
    }

    public void setPaymentController(PaymentController paymentController) {
        this.paymentController = paymentController;
    }

    public SearchController getSearchController() {
        return searchController;
    }

    public void setSearchController(SearchController searchController) {
        this.searchController = searchController;
    }

    public BillSearch getBillSearch() {
        return billSearch;
    }

    public void setBillSearch(BillSearch billSearch) {
        this.billSearch = billSearch;
    }

    public ConfigOptionApplicationController getConfigOptionApplicationController() {
        return configOptionApplicationController;
    }

    public void setConfigOptionApplicationController(ConfigOptionApplicationController configOptionApplicationController) {
        this.configOptionApplicationController = configOptionApplicationController;
    }

    public boolean isHandoverValuesCreated() {
        return handoverValuesCreated;
    }

    public void setHandoverValuesCreated(boolean handoverValuesCreated) {
        this.handoverValuesCreated = handoverValuesCreated;
    }

    public WebUser getUser() {
        return user;
    }

    public void setUser(WebUser user) {
        this.user = user;
    }

    public Staff getFromStaff() {
        return fromStaff;
    }

    public void setFromStaff(Staff fromStaff) {
        this.fromStaff = fromStaff;
    }

    public Staff getToStaff() {
        return toStaff;
    }

    public void setToStaff(Staff toStaff) {
        this.toStaff = toStaff;
    }

    public Department getForDepartment() {
        return forDepartment;
    }

    public void setForDepartment(Department forDepartment) {
        this.forDepartment = forDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public Date getForDate() {
        return forDate;
    }

    public void setForDate(Date forDate) {
        this.forDate = forDate;
    }

    public int getHandoverBillsToReceiveCount() {
        return handoverBillsToReceiveCount;
    }

    public void setHandoverBillsToReceiveCount(int handoverBillsToReceiveCount) {
        this.handoverBillsToReceiveCount = handoverBillsToReceiveCount;
    }

    public List<Payment> getCardPaymentsFromShiftSratToNow() {
        return cardPaymentsFromShiftSratToNow;
    }

    public void setCardPaymentsFromShiftSratToNow(List<Payment> cardPaymentsFromShiftSratToNow) {
        this.cardPaymentsFromShiftSratToNow = cardPaymentsFromShiftSratToNow;
    }

    public List<Payment> getChequeFromShiftSratToNow() {
        return chequeFromShiftSratToNow;
    }

    public void setChequeFromShiftSratToNow(List<Payment> chequeFromShiftSratToNow) {
        this.chequeFromShiftSratToNow = chequeFromShiftSratToNow;
    }

    public ReportTemplateRowBundle getChequeTransactionPaymentBundle() {
        return chequeTransactionPaymentBundle;
    }

    public void setChequeTransactionPaymentBundle(ReportTemplateRowBundle chequeTransactionPaymentBundle) {
        this.chequeTransactionPaymentBundle = chequeTransactionPaymentBundle;
    }

    public ReportTemplateRowBundle getCardTransactionPaymentBundle() {
        return cardTransactionPaymentBundle;
    }

    public void setCardTransactionPaymentBundle(ReportTemplateRowBundle cardTransactionPaymentBundle) {
        this.cardTransactionPaymentBundle = cardTransactionPaymentBundle;
    }

    public ReportTemplateRowBundle getRefundBundle() {
        return refundBundle;
    }

    public void setRefundBundle(ReportTemplateRowBundle refundBundle) {
        this.refundBundle = refundBundle;
    }

    public ReportTemplateRowBundle getCancelledBundle() {
        return cancelledBundle;
    }

    public void setCancelledBundle(ReportTemplateRowBundle cancelledBundle) {
        this.cancelledBundle = cancelledBundle;
    }

    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    public ReportTemplateRowBundle getBundle() {
        return bundle;
    }

    public void setBundle(ReportTemplateRowBundle bundle) {
        this.bundle = bundle;
    }

    public ReportTemplateRowBundle getSelectedBundle() {
        return selectedBundle;
    }

    public void setSelectedBundle(ReportTemplateRowBundle selectedBundle) {
        this.selectedBundle = selectedBundle;
    }

    public PaymentMethod getSelectedPaymentMethod() {
        return selectedPaymentMethod;
    }

    public void setSelectedPaymentMethod(PaymentMethod selectedPaymentMethod) {
        this.selectedPaymentMethod = selectedPaymentMethod;
    }

    public DetailedFinancialBill getCurrentDetailedFinancialBill() {
        return currentDetailedFinancialBill;
    }

    public void setCurrentDetailedFinancialBill(DetailedFinancialBill currentDetailedFinancialBill) {
        this.currentDetailedFinancialBill = currentDetailedFinancialBill;
    }

    private List<Payment> fetchPaymentsForSummaryHandoverCreation(Bill b) {
        if (b == null) {
            return Collections.emptyList();
        }

        List<Payment> payments = new ArrayList<>();
        String jpql = "SELECT p FROM PaymentHandoverItem p WHERE p.retired = :ret AND p.handoverShiftComponantBill = :b";

        Map<String, Object> m = new HashMap<>();
        m.put("ret", false);
        m.put("b", b);

        List<PaymentHandoverItem> phis = paymentHandoverItemFacade != null
                ? paymentHandoverItemFacade.findByJpql(jpql, m, TemporalType.TIMESTAMP) : Collections.emptyList();

        if (phis != null) {
            for (PaymentHandoverItem phi : phis) {
                if (phi != null && phi.getPayment() != null) {
                    payments.add(phi.getPayment());
                }
            }
        }

        return payments;
    }

    public Drawer getLoggedUserDrawer() {
        if (loggedUserDrawer == null) {
            if (sessionController.getLoggedUserDrawer() != null) {
                loggedUserDrawer = drawerFacade.find(sessionController.getLoggedUserDrawer().getId());
            } else {
                loggedUserDrawer = null;
            }
        }
        return loggedUserDrawer;
    }

    public void setLoggedUserDrawer(Drawer loggedUserDrawer) {
        this.loggedUserDrawer = loggedUserDrawer;
    }

    // <editor-fold defaultstate="collapsed" desc="Damitha's Edit">
    // </editor-fold>
    public List<DenominationTransaction> getDenominationTransactions() {
        return denominationTransactions;
    }

    public void setDenominationTransactions(List<DenominationTransaction> denominationTransactions) {
        this.denominationTransactions = denominationTransactions;
    }

    public DenominationTransaction getDt() {
        return dt;
    }

    public void setDt(DenominationTransaction dt) {
        this.dt = dt;
    }

    public double getTotalCashFund() {
        return totalCashFund;
    }

    public void setTotalCashFund(double totalCashFund) {
        this.totalCashFund = totalCashFund;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public BillItem getRemovingBillItem() {
        return removingBillItem;
    }

    public void setRemovingBillItem(BillItem removingBillItem) {
        this.removingBillItem = removingBillItem;
    }

    public Boolean getPatientDepositsAreConsideredInHandingover() {
        if (patientDepositsAreConsideredInHandingover == null) {
            patientDepositsAreConsideredInHandingover = configOptionApplicationController.getBooleanValueByKey("Patient Deposits are considered in handingover", false);
        }
        return patientDepositsAreConsideredInHandingover;
    }

}
