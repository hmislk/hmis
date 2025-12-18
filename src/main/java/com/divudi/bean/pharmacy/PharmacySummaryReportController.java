package com.divudi.bean.pharmacy;

// <editor-fold defaultstate="collapsed" desc="Import Statements">
import com.divudi.bean.common.*;
import com.divudi.bean.cashTransaction.CashBookEntryController;
import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.DrawerEntryController;
import com.divudi.bean.channel.ChannelSearchController;
import com.divudi.bean.channel.analytics.ReportTemplateController;
import com.divudi.core.data.reports.CashierReports;
import com.divudi.core.data.reports.CollectionCenterReport;
import com.divudi.core.data.reports.SummaryReports;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.BillType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.data.hr.ReportKeyWord;

import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Staff;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.bean.opd.OpdBillController;
import com.divudi.core.data.BillCategory;
import static com.divudi.core.data.BillCategory.BILL;
import static com.divudi.core.data.BillCategory.CANCELLATION;
import static com.divudi.core.data.BillCategory.PAYMENTS;
import static com.divudi.core.data.BillCategory.PREBILL;
import static com.divudi.core.data.BillCategory.REFUND;
import com.divudi.core.data.BillClassType;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.IncomeBundle;
import com.divudi.core.data.IncomeRow;
import static com.divudi.core.data.PaymentMethod.Agent;
import static com.divudi.core.data.PaymentMethod.Card;
import static com.divudi.core.data.PaymentMethod.Cash;
import static com.divudi.core.data.PaymentMethod.Cheque;
import static com.divudi.core.data.PaymentMethod.Credit;
import static com.divudi.core.data.PaymentMethod.IOU;
import static com.divudi.core.data.PaymentMethod.MultiplePaymentMethods;
import static com.divudi.core.data.PaymentMethod.None;
import static com.divudi.core.data.PaymentMethod.OnCall;
import static com.divudi.core.data.PaymentMethod.OnlineSettlement;
import static com.divudi.core.data.PaymentMethod.PatientDeposit;
import static com.divudi.core.data.PaymentMethod.PatientPoints;
import static com.divudi.core.data.PaymentMethod.Slip;
import static com.divudi.core.data.PaymentMethod.Staff;
import static com.divudi.core.data.PaymentMethod.Staff_Welfare;
import static com.divudi.core.data.PaymentMethod.Voucher;
import static com.divudi.core.data.PaymentMethod.YouOweMe;
import static com.divudi.core.data.PaymentMethod.ewallet;
import com.divudi.core.data.PharmacyBundle;
import com.divudi.core.data.PharmacyRow;
import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.ReportTemplateRowBundle;
import com.divudi.core.data.ReportViewType;
import static com.divudi.core.data.ReportViewType.BY_BILL;
import static com.divudi.core.data.ReportViewType.BY_BILL_TYPE;
import static com.divudi.core.data.ReportViewType.BY_BILL_TYPE_AND_DISCOUNT_TYPE_AND_ADMISSION_TYPE;
import static com.divudi.core.data.ReportViewType.BY_DISCOUNT_TYPE_AND_ADMISSION_TYPE;
import static com.divudi.core.data.ReportViewType.BY_ITEM;
import com.divudi.core.data.dto.MovementOutStockReportByItemDto;
import com.divudi.core.data.dto.MovementOutStockReportDto;
import com.divudi.core.data.pharmacy.DailyStockBalanceReport;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.HistoricalRecord;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.DrawerFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.ejb.PharmacyService;
import com.divudi.service.BillService;
import com.divudi.service.HistoricalRecordService;
import com.divudi.service.StockHistoryService;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.model.file.UploadedFile;

import org.primefaces.model.StreamedContent;
// </editor-fold>

/**
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class PharmacySummaryReportController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    private ReportTimerController reportTimerController;

// <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private BillFacade billFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private StockFacade stockFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private DrawerFacade drawerFacade;
    @EJB
    private BillService billService;
    @EJB
    StockHistoryService stockHistoryService;
    @EJB
    HistoricalRecordService historicalRecordService;
    @EJB
    PharmacyService pharmacyService;

// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private BillBeanController billBean;
    @Inject
    StockController stockController;
    @Inject
    private SessionController sessionController;
    @Inject
    private TransferController transferController;
    @Inject
    private PharmacySaleBhtController pharmacySaleBhtController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private OpdPreSettleController opdPreSettleController;
    @Inject
    private PharmacyPreSettleController pharmacyPreSettleController;
    @Inject
    private TokenController tokenController;
    @Inject
    private DepartmentController departmentController;
    @Inject
    private BillSearch billSearch;
    @Inject
    private PharmacyBillSearch pharmacyBillSearch;
    @Inject
    private OpdBillController opdBillController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private ChannelSearchController channelSearchController;
    @Inject
    private ReportTemplateController reportTemplateController;
    @Inject
    private CashBookEntryController cashBookEntryController;
    @Inject
    private ExcelController excelController;
    @Inject
    private PdfController pdfController;
    @Inject
    private DrawerEntryController drawerEntryController;
    @Inject
    private DrawerController drawerController;
    @Inject
    private EnumController enumController;
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Class Variables">
    // Basic types
    private String visitType;
    private String methodType;
    private String searchType;
    private String reportType;

    Long rowsPerPageForScreen;
    Long rowsPerPageForPrinting;
    private String fontSizeForPrinting;
    private String fontSizeForScreen;

    // Date range
    private Date fromDate;
    private Date toDate;

    private List<Bill> bills;

    private List<ReportViewType> reportViewTypes;
    private ReportViewType reportViewType;

    private double total;
    private double discount;
    private double netTotal;

    // Enum and category types
    private Category category;
    private BillType billType;
    private BillTypeAtomic billTypeAtomic;
    private BillClassType billClassType;
    private PaymentMethod paymentMethod;
    private AdmissionType admissionType;
    private PaymentScheme paymentScheme;

    // Collections
    private List<PaymentMethod> paymentMethods;

    // User-related
    private WebUser webUser;
    private Staff staff;
    private Staff currentStaff;

    // Institutional and departmental entities
    private Institution institution;
    private Institution creditCompany;
    private Institution dealer;
    private Institution site;
    private Institution toSite;
    private Institution fromInstitution;
    private Institution toInstitution;

    private Department department;
    private Department fromDepartment;
    private Department toDepartment;

    // Healthcare-specific entities
    private Patient patient;
    private PatientEncounter patientEncounter;
    private Item item;

    // Reporting and files
    private SearchKeyword searchKeyword;
    private ReportKeyWord reportKeyWord;
    private IncomeBundle bundle;
    private PharmacyBundle pharmacyBundle;
    private ReportTemplateRowBundle bundleReport;

    private DailyStockBalanceReport dailyStockBalanceReport;

    private StreamedContent downloadingExcel;
    private UploadedFile file;

    // Numeric variables
    private int maxResult = 50;
    
    private MovementOutStockReportWrapperDto wrapperDto;
    private MovementOutStockReportByItemWrapperDto wrapperDtoByItem;
        
    @EJB
    private ItemFacade itemFacade;

    //transferOuts;
    //adjustments;
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Navigators">
    public String navigateToPharmacyIncomeReport() {
        return "/pharmacy/reports/summary_reports/pharmacy_income_report?faces-redirect=true";
    }
    
    private void clearData(){
        wrapperDtoByItem = null;
        wrapperDto = null;
        bundle = null;
        institution = null;
        site = null;
        department = null;
        admissionType = null;
        paymentScheme = null;
    }

    public String navigateToPharmacyMovementOutBySaleIssueAndConsumptionWithCurrentStockReport() {
        clearData();
        reportViewTypes = Arrays.asList(
                ReportViewType.BY_BILL,
                ReportViewType.BY_ITEM
        );
        reportViewType = ReportViewType.BY_ITEM;
        return "/pharmacy/reports/movement_reports/movement_out_by_sale_issue_and_consumption_with_current_stock_report?faces-redirect=true";
    }
    
    public String navigateToPharmacyMovementOutBySaleIssueAndConsumptionWithCurrentStockReportOptimized() {
        clearData();
        reportViewTypes = Arrays.asList(
                ReportViewType.BY_BILL,
                ReportViewType.BY_ITEM
        );
        reportViewType = ReportViewType.BY_BILL;
        return "/pharmacy/reports/movement_reports/movement_out_by_sale_issue_and_consumption_with_current_stock_report_optimized?faces-redirect=true";
    }

    public String navigateToPharmacyProcurementReport() {
        return "/pharmacy/reports/procurement_reports/pharmacy_procurement_report?faces-redirect=true";
    }

    public String navigateToPharmacyIncomeAndCostReport() {
        return "/pharmacy/reports/summary_reports/pharmacy_income_and_cost_report?faces-redirect=true";
    }

    public String navigateToDailyStockValuesReport() {
        return "/pharmacy/reports/summary_reports/daily_stock_values_report?faces-redirect=true";
    }

    public String navigateToBillTypeIncome() {
        return "/pharmacy/reports/summary_reports/bill_type_income?faces-redirect=true";
    }

    public String navigatToBillListByBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
        listBills();
        return "/pharmacy/reports/summary_reports/bills?faces-redirect=true";
    }
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Functions">

    public void processDailyStockBalanceReport() {
//        reportTimerController.trackReportExecution(() -> {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        if (fromDate == null) {
            JsfUtil.addErrorMessage("Please select a date");
            return;
        }
        Date today = new Date();
        if (!fromDate.before(today)) {
            JsfUtil.addErrorMessage("Selected date must be earlier than today");
            return;
        }

        dailyStockBalanceReport = new DailyStockBalanceReport();
        dailyStockBalanceReport.setDate(fromDate);
        dailyStockBalanceReport.setDepartment(department);

        HistoricalRecord openingBalance = historicalRecordService.findRecord("Pharmacy Stock Value at Retail Sale Rate", null, null, department, fromDate);
        if (openingBalance != null) {
            dailyStockBalanceReport.setOpeningStockValue(openingBalance.getRecordValue());
        }

        // Calculate toDate as fromDate + 1 day
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        cal.add(Calendar.DATE, 1);
        toDate = cal.getTime();

        Date startOfTheDay = CommonFunctions.getStartOfBeforeDay(fromDate);
        Date endOfTheDay = CommonFunctions.getEndOfDay(fromDate);

        PharmacyBundle saleBundle = pharmacyService.fetchPharmacyIncomeByBillTypeAndDiscountTypeAndAdmissionType(startOfTheDay, endOfTheDay, null, null, department, null, null, null);
        dailyStockBalanceReport.setPharmacySalesByAdmissionTypeAndDiscountSchemeBundle(saleBundle);

        PharmacyBundle purchaseBundle = pharmacyService.fetchPharmacyStockPurchaseValueByBillType(startOfTheDay, endOfTheDay, null, null, department, null, null, null);
        dailyStockBalanceReport.setPharmacyPurchaseByBillTypeBundle(purchaseBundle);

        PharmacyBundle transferBundle = pharmacyService.fetchPharmacyTransferValueByBillType(startOfTheDay, endOfTheDay, null, null, department, null, null, null);
        dailyStockBalanceReport.setPharmacyTransferByBillTypeBundle(transferBundle);

        PharmacyBundle adjustmentBundle = pharmacyService.fetchPharmacyAdjustmentValueByBillType(startOfTheDay, endOfTheDay, null, null, department, null, null, null);
        dailyStockBalanceReport.setPharmacyAdjustmentsByBillTypeBundle(adjustmentBundle);

        HistoricalRecord closingBalance = historicalRecordService.findRecord("Pharmacy Stock Value at Retail Sale Rate", null, null, department, toDate);
        if (closingBalance != null) {
            dailyStockBalanceReport.setClosingStockValue(closingBalance.getRecordValue());
        }
//        }, SummaryReports.DAILY_STOCK_BALANCE_REPORT, sessionController.getLoggedUser());
    }

    public void listBillTypes() {
        reportTimerController.trackReportExecution(() -> {
            bundleReport = new ReportTemplateRowBundle();
            Map<String, Object> params = new HashMap<>();
            List<BillTypeAtomic> btas = new ArrayList<>();
            StringBuilder jpql = new StringBuilder("select new com.divudi.core.data.ReportTemplateRow("
                    + "b.billType, b.billClassType, b.billTypeAtomic, count(b), sum(b.total), sum(b.discount), sum(b.netTotal))"
                    + " from Bill b where b.retired=:ret ");
            params.put("ret", false);
            btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE);
            btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED);
            btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND);

            btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS);
            btas.add(BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEM_PAYMENTS);
            btas.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE);
            btas.add(BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED);
            btas.add(BillTypeAtomic.ACCEPT_ISSUED_MEDICINE_INWARD);

            btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE);
            btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION);
            btas.add(BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN);

            btas.add(BillTypeAtomic.PHARMACY_GRN);
            btas.add(BillTypeAtomic.PHARMACY_GRN_CANCELLED);
            btas.add(BillTypeAtomic.PHARMACY_GRN_REFUND);
            btas.add(BillTypeAtomic.PHARMACY_GRN_RETURN);
            btas.add(BillTypeAtomic.PHARMACY_GRN_PAYMENT);
            btas.add(BillTypeAtomic.PHARMACY_GRN_PAYMENT_CANCELLED);

            params.put("btas", btas);
            jpql.append(" and b.billTypeAtomic in :btas ");

            if (toDate != null && fromDate != null) {
                jpql.append(" and b.createdAt between :fromDate and :toDate ");
                params.put("toDate", toDate);
                params.put("fromDate", fromDate);
            }

            if (institution != null) {
                params.put("ins", institution);
                jpql.append(" and b.department.institution = :ins ");
            }

            if (department != null) {
                params.put("dep", department);
                jpql.append(" and b.department = :dept ");
            }

            if (site != null) {
                params.put("site", site);
                jpql.append(" and b.department = :site ");
            }

            if (webUser != null) {
                jpql.append(" and b.creater=:wu ");
                params.put("wu", webUser);
            }

            jpql.append(" group by b.billType, b.billClassType, b.billTypeAtomic ");
            List<ReportTemplateRow> rows = (List<ReportTemplateRow>) getBillFacade().findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

            bundleReport.setReportTemplateRows(rows);
            bundleReport.calculateTotalByValues();
        }, SummaryReports.BILL_TYPE_LIST_REPORT, sessionController.getLoggedUser());
    }

    public void listBills() {
        bills = null;
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select b from Bill b where 1=1 ");
        if (toDate != null && fromDate != null) {
            jpql.append(" and b.createdAt between :fromDate and :toDate ");
            params.put("toDate", toDate);
            params.put("fromDate", fromDate);
        }

        if (institution != null) {
            params.put("ins", institution);
            jpql.append(" and b.department.institution = :ins ");
        }

        if (department != null) {
            params.put("dep", department);
            jpql.append(" and b.department = :dept ");
        }

        if (site != null) {
            params.put("site", site);
            jpql.append(" and b.department.site = :site ");
        }

        if (webUser != null) {
            jpql.append(" and b.creater=:wu ");
            params.put("wu", webUser);
        }

        if (billClassType != null) {
            jpql.append(" and type(b)=:billClassType ");
            switch (billClassType) {
                case Bill:
                case OtherBill:
                    params.put("billClassType", com.divudi.core.entity.Bill.class);
                    break;
                case BilledBill:
                    params.put("billClassType", com.divudi.core.entity.BilledBill.class);
                    break;
                case CancelledBill:
                    params.put("billClassType", com.divudi.core.entity.CancelledBill.class);
                    break;
                case PreBill:
                    params.put("billClassType", com.divudi.core.entity.PreBill.class);
                    break;
                case RefundBill:
                    params.put("billClassType", com.divudi.core.entity.RefundBill.class);
                    break;

            }
        }

        if (billType != null) {
            jpql.append(" and b.billType=:billType ");
            params.put("billType", billType);
        }

        if (billTypeAtomic != null) {
            jpql.append(" and b.billTypeAtomic=:billTypeAtomic ");
            params.put("billTypeAtomic", billTypeAtomic);
        }

        // Order by bill ID
        jpql.append(" order by b.id ");

        // Execute the query
        bills = getBillFacade().findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        if (bills != null) {
            for (Bill bill : bills) {
                if (bill != null) {
                    total += bill.getTotal();
                    netTotal += bill.getNetTotal();
                    discount += bill.getDiscount();
                }
            }
        }

    }

    public void resetAllFiltersExceptDateRange() {
        setInstitution(null);
        setDepartment(null);
        setWebUser(null);
        setSite(null);
        setPaymentMethod(null);
        setSearchKeyword(null);
        setCategory(null);
        setItem(null);
        setStaff(null);
        setCurrentStaff(null);
        setCreditCompany(null);
        setDealer(null);
        setToSite(null);
        setFromInstitution(null);
        setToInstitution(null);
        setFromDepartment(null);
        setToDepartment(null);
        setPatient(null);
        setPatientEncounter(null);
        setBillType(null);
        setBillTypeAtomic(null);
        setBillClassType(null);
        setPaymentMethods(null);
        setReportKeyWord(null);
        setBundle(new IncomeBundle());
        setDownloadingExcel(null);
        setFile(null);
        setSearchKeyword(new SearchKeyword());
    }

    public void resetAllFilters() {
        resetAllFiltersExceptDateRange();
        setFromDate(null);
        setToDate(null);
    }

    public void processPharmacyIncomeReport() {
        reportTimerController.trackReportExecution(() -> {
            if (reportViewType == null) {
                JsfUtil.addErrorMessage("Please select a report view type.");
                return;
            }

            switch (reportViewType) {
                case BY_BILL:
                    processPharmacyIncomeReportByBill();
                    break;
                case BY_BILL_TYPE:
                    processPharmacyIncomeReportByBillType();
                    break;
                case BY_DISCOUNT_TYPE_AND_ADMISSION_TYPE:
                    processPharmacyIncomeReportByDiscountTypeAndAdmissionType();
                    break;
                case BY_BILL_TYPE_AND_DISCOUNT_TYPE_AND_ADMISSION_TYPE:
                    processPharmacyIncomeReportByBillTypeAndDiscountTypeAndAdmissionType();
                    break;
                default:
                    JsfUtil.addErrorMessage("Unsupported report view type: " + reportViewType.getLabel());
                    break;
            }
        }, SummaryReports.PHARMACY_INCOME_REPORT, sessionController.getLoggedUser());
    }
    
    public void processMovementOutBySaleIssueAndConsumptionWithCurrentStockReportOptimized() {
      
            if (reportViewType == null) {
                JsfUtil.addErrorMessage("Please select a report view type.");
                return;
            }
            switch (reportViewType) {
                case BY_BILL:
                    processMovementOutWithStocksReportByBillDto();
                    break;
                case BY_ITEM:
                    processMovementOutWithStockReportByItemDto();
                    addCurrentItemStock(wrapperDtoByItem.getPbis());
                    break;
                default:
                    JsfUtil.addErrorMessage("Unsupported report view type: " + reportViewType.getLabel());
                    break;
            }
        
    }

    public void processMovementOutBySaleIssueAndConsumptionWithCurrentStockReport() {
        reportTimerController.trackReportExecution(() -> {
            if (reportViewType == null) {
                JsfUtil.addErrorMessage("Please select a report view type.");
                return;
            }
            switch (reportViewType) {
                case BY_BILL:
                    processMovementOutWithStocksReportByBill();
                    break;
                case BY_ITEM:
                    processMovementOutWithStockReportByItem();
                    addCurrentItemStock(pharmacyBundle);
                    break;
                default:
                    JsfUtil.addErrorMessage("Unsupported report view type: " + reportViewType.getLabel());
                    break;
            }
        }, SummaryReports.PHARMACY_INCOME_REPORT, sessionController.getLoggedUser());
    }

    public void processPharmacyProcurementReport() {
        if (reportViewType == null) {
            JsfUtil.addErrorMessage("Please select a report view type.");
            return;
        }

        switch (reportViewType) {
            case BY_BILL:
                processPharmacyProcurementReportByBill();
                break;
            default:
                JsfUtil.addErrorMessage("Unsupported report view type: " + reportViewType.getLabel());
                break;
        }
    }

    public void processPharmacyProcurementReportByBill() {
        List<BillTypeAtomic> billTypeAtomics = getPharmacyProcurementBillTypes();
        List<Bill> bills = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, null, null);
        pharmacyBundle = new PharmacyBundle(bills);
        pharmacyBundle.generateProcurementForBills();
    }

    public void processPharmacyIncomeReportByBillType() {
        reportTimerController.trackReportExecution(() -> {
            List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();

            List<Bill> incomeBills = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
            bundle = new IncomeBundle(incomeBills);
            bundle.fixDiscountsAndMarginsInRows();
            for (IncomeRow r : bundle.getRows()) {
                if (r.getBill() == null) {
                    continue;
                }
                if (r.getBill().getPaymentMethod() == null) {
                    continue;
                }
                if (r.getBill().getPaymentMethod().equals(PaymentMethod.MultiplePaymentMethods)) {
                    r.setPayments(billService.fetchBillPayments(r.getBill()));
                }
            }
            bundle.generatePaymentDetailsGroupedByBillType();
        }, SummaryReports.PHARMACY_INCOME_REPORT, sessionController.getLoggedUser());
    }

    public void processMovementOutWithStockReportByBillType() {
        reportTimerController.trackReportExecution(() -> {
            List<BillTypeAtomic> billTypeAtomics = getPharmacyMovementOutBillTypes();

            List<Bill> incomeBills = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
            bundle = new IncomeBundle(incomeBills);
            for (IncomeRow r : bundle.getRows()) {
                if (r.getBill() == null) {
                    continue;
                }
                if (r.getBill().getPaymentMethod() == null) {
                    continue;
                }
                if (r.getBill().getPaymentMethod().equals(PaymentMethod.MultiplePaymentMethods)) {
                    r.setPayments(billService.fetchBillPayments(r.getBill()));
                }
            }
            bundle.generatePaymentDetailsGroupedByBillType();
        }, SummaryReports.PHARMACY_INCOME_REPORT, sessionController.getLoggedUser());
    }

    public List<BillTypeAtomic> getPharmacyMovementOutBillTypes() {
        return Arrays.asList(
                BillTypeAtomic.PHARMACY_RETAIL_SALE,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY,
                BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK,
                BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK_CANCELLED,
                BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK_REFUND,
                BillTypeAtomic.PHARMACY_WHOLESALE,
                BillTypeAtomic.PHARMACY_WHOLESALE_CANCELLED,
                BillTypeAtomic.PHARMACY_WHOLESALE_PRE,
                BillTypeAtomic.PHARMACY_WHOLESALE_REFUND,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_THEATRE,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_THEATRE_CANCELLATION,
                BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD,
                BillTypeAtomic.ACCEPT_RETURN_MEDICINE_THEATRE,
                BillTypeAtomic.PHARMACY_DIRECT_ISSUE,
                BillTypeAtomic.PHARMACY_DIRECT_ISSUE_CANCELLED,
                BillTypeAtomic.PHARMACY_ISSUE,
                BillTypeAtomic.PHARMACY_ISSUE_CANCELLED,
                BillTypeAtomic.PHARMACY_ISSUE_RETURN
        );
    }

    public List<BillTypeAtomic> getPharmacyIncomeBillTypes() {
        return Arrays.asList(
                BillTypeAtomic.PHARMACY_RETAIL_SALE,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_CANCELLED,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_REFUND,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_PREBILL_SETTLED_AT_CASHIER,
                BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_ONLY,
                BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK,
                BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK_CANCELLED,
                BillTypeAtomic.PHARMACY_SALE_WITHOUT_STOCK_REFUND,
                BillTypeAtomic.PHARMACY_WHOLESALE,
                BillTypeAtomic.PHARMACY_WHOLESALE_CANCELLED,
                BillTypeAtomic.PHARMACY_WHOLESALE_PRE,
                BillTypeAtomic.PHARMACY_WHOLESALE_REFUND,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_THEATRE_MEDICINE_RETURN,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_CANCELLATION,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_INWARD_RETURN,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_THEATRE,
                BillTypeAtomic.ISSUE_MEDICINE_ON_REQUEST_THEATRE_CANCELLATION,
                BillTypeAtomic.ACCEPT_RETURN_MEDICINE_INWARD,
                BillTypeAtomic.ACCEPT_RETURN_MEDICINE_THEATRE
        );
    }

    public List<BillTypeAtomic> getPharmacyProcurementBillTypes() {
        return Arrays.asList(
                BillTypeAtomic.PHARMACY_GRN,
                BillTypeAtomic.PHARMACY_GRN_CANCELLED,
                BillTypeAtomic.PHARMACY_GRN_REFUND,
                BillTypeAtomic.PHARMACY_GRN_RETURN,
                BillTypeAtomic.PHARMACY_GRN_WHOLESALE,
                BillTypeAtomic.PHARMACY_DIRECT_PURCHASE,
                BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED,
                BillTypeAtomic.PHARMACY_DIRECT_PURCHASE_CANCELLED
        );
    }

    public void processPharmacyIncomeReportByDiscountTypeAndAdmissionType() {
        reportTimerController.trackReportExecution(() -> {
            List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();

            List<Bill> incomeBills = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
            bundle = new IncomeBundle(incomeBills);
            bundle.fixDiscountsAndMarginsInRows();
            for (IncomeRow r : bundle.getRows()) {
                if (r.getBill() == null) {
                    continue;
                }
                if (r.getBill().getPaymentMethod() == null) {
                    continue;
                }
                if (r.getBill().getPaymentMethod().equals(PaymentMethod.MultiplePaymentMethods)) {
                    r.setPayments(billService.fetchBillPayments(r.getBill()));
                }
            }
            bundle.generatePaymentDetailsGroupedDiscountSchemeAndAdmissionType();
        }, SummaryReports.PHARMACY_INCOME_REPORT, sessionController.getLoggedUser());
    }

    public void processPharmacyIncomeReportByBillTypeAndDiscountTypeAndAdmissionType() {
        reportTimerController.trackReportExecution(() -> {

            List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();

            List<Bill> incomeBills = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
            bundle = new IncomeBundle(incomeBills);
            bundle.fixDiscountsAndMarginsInRows();
            for (IncomeRow r : bundle.getRows()) {
                Bill b = r.getBill();
                if (b == null || b.getPaymentMethod() == null) {
                    continue;
                }
                if (b.getPaymentMethod().equals(PaymentMethod.MultiplePaymentMethods)) {
                    r.setPayments(billService.fetchBillPayments(b));
                }
            }

            bundle.generatePaymentDetailsGroupedByBillTypeAndDiscountSchemeAndAdmissionType();
        }, SummaryReports.PHARMACY_INCOME_REPORT, sessionController.getLoggedUser());
    }

    public void processPharmacyIncomeReportByBill() {
        reportTimerController.trackReportExecution(() -> {
            List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();
            List<Bill> bills = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
            bundle = new IncomeBundle(bills);
            bundle.fixDiscountsAndMarginsInRows();
            for (IncomeRow r : bundle.getRows()) {
                if (r.getBill() == null) {
                    continue;
                }
                if (r.getBill().getPaymentMethod() == null) {
                    continue;
                }
                if (r.getBill().getPaymentMethod().equals(PaymentMethod.MultiplePaymentMethods)) {
                    r.setPayments(billService.fetchBillPayments(r.getBill()));
                }
            }
            bundle.generatePaymentDetailsForBills();
        }, SummaryReports.PHARMACY_INCOME_REPORT, sessionController.getLoggedUser());
    }

    public static class MovementOutStockReportWrapperDto {

        private List<MovementOutStockReportDto> reportDataRows;

        private double sumOfCashValues = 0.0;
        private double sumOfCardValues = 0.0;
        private double sumOfMultiplePaymentMethodsValues = 0.0;
        private double sumOfStaffValues = 0.0;
        private double sumOfCreditValues = 0.0;
        private double sumOfStaffWelfareValues = 0.0;
        private double sumOfVoucherValues = 0.0;
        private double sumOfIouValues = 0.0;
        private double sumOfAgentValues = 0.0;
        private double sumOfChequeValues = 0.0;
        private double sumOfSlipValues = 0.0;
        private double sumOfEWalletValues = 0.0;
        private double sumOfPatientDepositValues = 0.0;
        private double sumOfPatientPointsValues = 0.0;
        private double sumOfOnlineSettlementValues = 0.0;
        private double sumOfNoneValues = 0.0;
        private double sumOfOpdCreditValues = 0.0;
        private double sumOfInpatientCreditValues = 0.0;

        private double sumOfGrossTotal = 0.0;
        private double sumOfDiscount = 0.0;
        private double sumOfServiceCharge = 0.0;
        private double sumOfTax = 0.0;
        private double sumOfActualTotal = 0.0;
        private double sumOfNetTotal = 0.0;

        public List<MovementOutStockReportDto> getReportDataRows() {
            return reportDataRows;
        }

        public void setReportDataRows(List<MovementOutStockReportDto> reportDataRows) {
            this.reportDataRows = reportDataRows;
        }

        public double getSumOfCashValues() {
            return sumOfCashValues;
        }

        public void setSumOfCashValues(double sumOfCashValues) {
            this.sumOfCashValues = sumOfCashValues;
        }

        public double getSumOfCardValues() {
            return sumOfCardValues;
        }

        public void setSumOfCardValues(double sumOfCardValues) {
            this.sumOfCardValues = sumOfCardValues;
        }

        public double getSumOfMultiplePaymentMethodsValues() {
            return sumOfMultiplePaymentMethodsValues;
        }

        public void setSumOfMultiplePaymentMethodsValues(double sumOfMultiplePaymentMethodsValues) {
            this.sumOfMultiplePaymentMethodsValues = sumOfMultiplePaymentMethodsValues;
        }

        public double getSumOfStaffValues() {
            return sumOfStaffValues;
        }

        public void setSumOfStaffValues(double sumOfStaffValues) {
            this.sumOfStaffValues = sumOfStaffValues;
        }

        public double getSumOfCreditValues() {
            return sumOfCreditValues;
        }

        public void setSumOfCreditValues(double sumOfCreditValues) {
            this.sumOfCreditValues = sumOfCreditValues;
        }

        public double getSumOfStaffWelfareValues() {
            return sumOfStaffWelfareValues;
        }

        public void setSumOfStaffWelfareValues(double sumOfStaffWelfareValues) {
            this.sumOfStaffWelfareValues = sumOfStaffWelfareValues;
        }

        public double getSumOfVoucherValues() {
            return sumOfVoucherValues;
        }

        public void setSumOfVoucherValues(double sumOfVoucherValues) {
            this.sumOfVoucherValues = sumOfVoucherValues;
        }

        public double getSumOfIouValues() {
            return sumOfIouValues;
        }

        public void setSumOfIouValues(double sumOfIouValues) {
            this.sumOfIouValues = sumOfIouValues;
        }

        public double getSumOfAgentValues() {
            return sumOfAgentValues;
        }

        public void setSumOfAgentValues(double sumOfAgentValues) {
            this.sumOfAgentValues = sumOfAgentValues;
        }

        public double getSumOfChequeValues() {
            return sumOfChequeValues;
        }

        public void setSumOfChequeValues(double sumOfChequeValues) {
            this.sumOfChequeValues = sumOfChequeValues;
        }

        public double getSumOfSlipValues() {
            return sumOfSlipValues;
        }

        public void setSumOfSlipValues(double sumOfSlipValues) {
            this.sumOfSlipValues = sumOfSlipValues;
        }

        public double getSumOfEWalletValues() {
            return sumOfEWalletValues;
        }

        public void setSumOfEWalletValues(double sumOfEWalletValues) {
            this.sumOfEWalletValues = sumOfEWalletValues;
        }

        public double getSumOfPatientDepositValues() {
            return sumOfPatientDepositValues;
        }

        public void setSumOfPatientDepositValues(double sumOfPatientDepositValues) {
            this.sumOfPatientDepositValues = sumOfPatientDepositValues;
        }

        public double getSumOfPatientPointsValues() {
            return sumOfPatientPointsValues;
        }

        public void setSumOfPatientPointsValues(double sumOfPatientPointsValues) {
            this.sumOfPatientPointsValues = sumOfPatientPointsValues;
        }

        public double getSumOfOnlineSettlementValues() {
            return sumOfOnlineSettlementValues;
        }

        public void setSumOfOnlineSettlementValues(double sumOfOnlineSettlementValues) {
            this.sumOfOnlineSettlementValues = sumOfOnlineSettlementValues;
        }

        public double getSumOfNoneValues() {
            return sumOfNoneValues;
        }

        public void setSumOfNoneValues(double sumOfNoneValues) {
            this.sumOfNoneValues = sumOfNoneValues;
        }

        public double getSumOfOpdCreditValues() {
            return sumOfOpdCreditValues;
        }

        public void setSumOfOpdCreditValues(double sumOfOpdCreditValues) {
            this.sumOfOpdCreditValues = sumOfOpdCreditValues;
        }

        public double getSumOfInpatientCreditValues() {
            return sumOfInpatientCreditValues;
        }

        public void setSumOfInpatientCreditValues(double sumOfInpatientCreditValues) {
            this.sumOfInpatientCreditValues = sumOfInpatientCreditValues;
        }

        public double getSumOfGrossTotal() {
            return sumOfGrossTotal;
        }

        public void setSumOfGrossTotal(double sumOfGrossTotal) {
            this.sumOfGrossTotal = sumOfGrossTotal;
        }

        public double getSumOfDiscount() {
            return sumOfDiscount;
        }

        public void setSumOfDiscount(double sumOfDiscount) {
            this.sumOfDiscount = sumOfDiscount;
        }

        public double getSumOfServiceCharge() {
            return sumOfServiceCharge;
        }

        public void setSumOfServiceCharge(double sumOfServiceCharge) {
            this.sumOfServiceCharge = sumOfServiceCharge;
        }

        public double getSumOfTax() {
            return sumOfTax;
        }

        public void setSumOfTax(double sumOfTax) {
            this.sumOfTax = sumOfTax;
        }

        public double getSumOfActualTotal() {
            return sumOfActualTotal;
        }

        public void setSumOfActualTotal(double sumOfActualTotal) {
            this.sumOfActualTotal = sumOfActualTotal;
        }

        public double getSumOfNetTotal() {
            return sumOfNetTotal;
        }

        public void setSumOfNetTotal(double sumOfNetTotal) {
            this.sumOfNetTotal = sumOfNetTotal;
        }
    }

    private void calculatePaymentsFromMultiplePayment(MovementOutStockReportDto dto, List<PaymentData> payments) {

        if (dto == null && payments == null) {
            return;
        }

        for (PaymentData p : payments) {
            if (p.getPaymentMethod() == null) {
                dto.setNoneValue(dto.getNoneValue() + p.getPaidValue());
            } else {
                double paidValue = p.getPaidValue();

                switch (p.getPaymentMethod()) {
                    case Agent:
                        dto.setAgentValue(dto.getAgentValue() + paidValue);
                        break;
                    case Card:
                        dto.setCardValue(dto.getCardValue() + paidValue);
                        break;
                    case Cash:
                        dto.setCashValue(dto.getCashValue() + paidValue);
                        break;
                    case Cheque:
                        dto.setChequeValue(dto.getChequeValue() + paidValue);
                        break;
                    case Credit:
                        dto.setCreditValue(dto.getCreditValue() + paidValue);
                        
                        Bill bill = billFacade.find(dto.getBillId());
                        
                        if (bill != null && bill.getPatientEncounter() != null) {
                            dto.setInpatientCreditValue(dto.getInpatientCreditValue() + paidValue);
                        } else {
                            dto.setOpdCreditValue(dto.getOpdCreditValue() + paidValue);
                        }
                        break;
                    case IOU:
                        dto.setIouValue(dto.getIouValue() + paidValue);
                        break;
                    case OnCall:
                        dto.setOnCallValue(dto.getOnCallValue() + paidValue);
                        break;
                    case OnlineSettlement:
                        dto.setOnlineSettlementValue(dto.getOnlineSettlementValue() + paidValue);
                        break;
                    case PatientDeposit:
                        dto.setPatientDepositValue(dto.getPatientDepositValue() + paidValue);
                        break;
                    case PatientPoints:
                        dto.setPatientPointsValue(dto.getPatientPointsValue() + paidValue);
                        break;
                    case Slip:
                        dto.setSlipValue(dto.getSlipValue() + paidValue);
                        break;
                    case Staff:
                        dto.setStaffValue(dto.getStaffValue() + paidValue);
                        break;
                    case Staff_Welfare:
                        dto.setStaffWelfareValue(dto.getStaffWelfareValue() + paidValue);
                        break;
                    case Voucher:
                        dto.setVoucherValue(dto.getVoucherValue() + paidValue);
                        break;
                    case ewallet:
                        dto.seteWalletValue(dto.geteWalletValue() + paidValue);
                        break;
                    case YouOweMe:
                        break;
                    case None:
                    case MultiplePaymentMethods:
                        break;
                    default:
                        dto.setNoneValue(dto.getNoneValue() + paidValue);
                }
            }
        }

    }
    
    public Bill fetchBillFromId(Long billId){
        System.out.println("line 1140");
        if(billId == null){
            return null;
        }
        
        return billFacade.find(billId);
    }
    
    public static class PaymentData{
        private PaymentMethod paymentMethod;
        private double paidValue;
        private Date createdAt;

        public PaymentData(PaymentMethod paymentMethod, double paidValue, Date createdAt) {
            this.paymentMethod = paymentMethod;
            this.paidValue = paidValue;
            this.createdAt = createdAt;
        }

        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public double getPaidValue() {
            return paidValue;
        }

        public void setPaidValue(double paidValue) {
            this.paidValue = paidValue;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(Date createdAt) {
            this.createdAt = createdAt;
        }
    }

    private void fetchMultiplePaymentMovementOutWithStocksReportByBillDto(List<MovementOutStockReportDto> reportDataRows) {

        for (MovementOutStockReportDto dto : reportDataRows) {
            if (dto.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
                List<PaymentData> payments = billService.fetchBillPaymentsFromBillId(dto.getBillId());

                if(payments != null && !payments.isEmpty()){
                    calculatePaymentsFromMultiplePayment(dto, payments);
                }
            }
        }

    }
    
    private void createSummeryForMovementOutWithStocksReportByBillDto(List<MovementOutStockReportDto> reportDataRows, MovementOutStockReportWrapperDto wrapperDto){
        
        for (MovementOutStockReportDto r : reportDataRows) {
            wrapperDto.sumOfCashValues += r.getCashValue();
            wrapperDto.sumOfCardValues += r.getCardValue();
            wrapperDto.sumOfMultiplePaymentMethodsValues += r.getMultiplePaymentMethodsValue();
            wrapperDto.sumOfStaffValues += r.getStaffValue();
            wrapperDto.sumOfCreditValues += r.getCreditValue();
            wrapperDto.sumOfStaffWelfareValues += r.getStaffWelfareValue();
            wrapperDto.sumOfVoucherValues += r.getVoucherValue();
            wrapperDto.sumOfIouValues += r.getIouValue();
            wrapperDto.sumOfAgentValues += r.getAgentValue();
            wrapperDto.sumOfChequeValues += r.getChequeValue();
            wrapperDto.sumOfSlipValues += r.getSlipValue();
            wrapperDto.sumOfEWalletValues += r.geteWalletValue();
            wrapperDto.sumOfPatientDepositValues += r.getPatientDepositValue();
            wrapperDto.sumOfPatientPointsValues += r.getPatientPointsValue();
            wrapperDto.sumOfOnlineSettlementValues += r.getOnlineSettlementValue();
            wrapperDto.sumOfNoneValues += r.getNoneValue();
            wrapperDto.sumOfOpdCreditValues += r.getOpdCreditValue();
            wrapperDto.sumOfInpatientCreditValues += r.getInpatientCreditValue();

            wrapperDto.sumOfGrossTotal += r.getGrossTotal();
            wrapperDto.sumOfDiscount += r.getDiscount();
            wrapperDto.sumOfServiceCharge += r.getServiceCharge();
            wrapperDto.sumOfTax += r.getTax();
            wrapperDto.sumOfActualTotal += r.getActualTotal();
            wrapperDto.sumOfNetTotal += r.getNetTotal();
        }
  
    }

    public void processMovementOutWithStocksReportByBillDto() {
        List<BillTypeAtomic> billTypeAtomics = getPharmacyMovementOutBillTypes();
        List<MovementOutStockReportDto> reportDataRows = billService.fetchMovementOutStockReportData(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
        
        fetchMultiplePaymentMovementOutWithStocksReportByBillDto(reportDataRows);
        
        wrapperDto = new MovementOutStockReportWrapperDto();
        
        wrapperDto.setReportDataRows(reportDataRows);
        
        createSummeryForMovementOutWithStocksReportByBillDto(reportDataRows, wrapperDto);
        
       
    }

    public MovementOutStockReportWrapperDto getWrapperDto() {
        return wrapperDto;
    }

    public void setWrapperDto(MovementOutStockReportWrapperDto wrapperDto) {
        this.wrapperDto = wrapperDto;
    }

    public void processMovementOutWithStocksReportByBill() {
        reportTimerController.trackReportExecution(() -> {
            List<BillTypeAtomic> billTypeAtomics = getPharmacyMovementOutBillTypes();

            List<Bill> bills = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
            bundle = new IncomeBundle(bills);
            for (IncomeRow r : bundle.getRows()) {
                if (r.getBill() == null) {
                    continue;
                }
                if (r.getBill().getPaymentMethod() == null) {
                    continue;
                }
                if (r.getBill().getPaymentMethod().equals(PaymentMethod.MultiplePaymentMethods)) {
                    r.setPayments(billService.fetchBillPayments(r.getBill()));
                }
            }
            bundle.generatePaymentDetailsForBills();
        }, SummaryReports.PHARMACY_INCOME_REPORT, sessionController.getLoggedUser());
    }

    public void processPharmacyIncomeAndCostReport() {
        reportTimerController.trackReportExecution(() -> {
            if (reportViewType == null) {
                JsfUtil.addErrorMessage("Please select a report view type.");
                return;
            }
            switch (reportViewType) {
                case BY_BILL_ITEM:
                    processPharmacyIncomeAndCostReportByBillItem();
                    break;
                case BY_BILL_TYPE:
                    processPharmacyIncomeAndCostReportByBillType();
                    break;
                case BY_BILL:
                    processPharmacyIncomeAndCostReportByBill();
                    break;
                default:
                    JsfUtil.addErrorMessage("Unsupported report view type.");
                    break;
            }
        }, SummaryReports.PHARMACY_INCOME_AND_COST_REPORT, sessionController.getLoggedUser());
    }

    public void processPharmacyIncomeAndCostReportByBillItem() {
        reportTimerController.trackReportExecution(() -> {
            List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();
            List<PharmaceuticalBillItem> pbis = billService.fetchPharmaceuticalBillItems(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
            bundle = new IncomeBundle(pbis);
            bundle.generateRetailAndCostDetailsForPharmaceuticalBillItems();
        }, SummaryReports.PHARMACY_INCOME_REPORT, sessionController.getLoggedUser());
    }

    public void processMovementOutWithStockReportByItem() {
        List<BillTypeAtomic> billTypeAtomics = getPharmacyMovementOutBillTypes();
        List<PharmaceuticalBillItem> pbis = billService.fetchPharmaceuticalBillItems(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
        pharmacyBundle = new PharmacyBundle(pbis);
        pharmacyBundle.groupSaleDetailsByItems();
    }
    
    public static class MovementOutStockReportByItemWrapperDto{
        
        private List<com.divudi.core.data.dto.MovementOutStockReportByItemDto> pbis;
        private double grossSaleValue;
        private double marginValue;
        private double discountValue;
        private double netSaleValue;
        private double quantity;

        public List<MovementOutStockReportByItemDto> getPbis() {
            return pbis;
        }

        public void setPbis(List<MovementOutStockReportByItemDto> pbis) {
            this.pbis = pbis;
        }

        public double getGrossSaleValue() {
            return grossSaleValue;
        }

        public void setGrossSaleValue(double grossSaleValue) {
            this.grossSaleValue = grossSaleValue;
        }

        public double getMarginValue() {
            return marginValue;
        }

        public void setMarginValue(double marginValue) {
            this.marginValue = marginValue;
        }

        public double getDiscountValue() {
            return discountValue;
        }

        public void setDiscountValue(double discountValue) {
            this.discountValue = discountValue;
        }

        public double getNetSaleValue() {
            return netSaleValue;
        }

        public void setNetSaleValue(double netSaleValue) {
            this.netSaleValue = netSaleValue;
        }

        public double getQuantity() {
            return quantity;
        }

        public void setQuantity(double quantity) {
            this.quantity = quantity;
        }
        
    }

    public MovementOutStockReportByItemWrapperDto getWrapperDtoByItem() {
        return wrapperDtoByItem;
    }

    public void setWrapperDtoByItem(MovementOutStockReportByItemWrapperDto wrapperDtoByItem) {
        this.wrapperDtoByItem = wrapperDtoByItem;
    }
    
    private void calculateTheDtoSummeryForMovementOutWithStockReportByItemDto(List<MovementOutStockReportByItemDto> pbis, MovementOutStockReportByItemWrapperDto wrapperDto){
        
        if(pbis == null || wrapperDto == null){
            return;
        }
        
        for(MovementOutStockReportByItemDto dto : pbis){
            wrapperDto.setGrossSaleValue(wrapperDto.getGrossSaleValue() + dto.getGrossSaleValue());
            wrapperDto.setDiscountValue(wrapperDto.getDiscountValue() + dto.getDiscountValue());
            wrapperDto.setMarginValue(wrapperDto.getMarginValue() + dto.getMarginValue());
            wrapperDto.setNetSaleValue(wrapperDto.getNetSaleValue() + dto.getNetSaleValue());
            
        }
        
    }
    
    public void processMovementOutWithStockReportByItemDto() {
        List<BillTypeAtomic> billTypeAtomics = getPharmacyMovementOutBillTypes();
        List<MovementOutStockReportByItemDto> pbis = billService.fetchPharmaceuticalBillItemsForMovementOutStockReportByItemDto(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
        
        wrapperDtoByItem = new MovementOutStockReportByItemWrapperDto();
        wrapperDtoByItem.setPbis(pbis);
        
        calculateTheDtoSummeryForMovementOutWithStockReportByItemDto(pbis, wrapperDtoByItem);
    }

    public void processPharmacyIncomeAndCostReportByBill() {
        reportTimerController.trackReportExecution(() -> {
            List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();
            List<Bill> pbis = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
            bundle = new IncomeBundle(pbis);
            bundle.generateRetailAndCostDetailsForPharmaceuticalBill();
        }, SummaryReports.PHARMACY_INCOME_REPORT, sessionController.getLoggedUser());
    }

    public void processPharmacyIncomeAndCostReportByBillType() {
        reportTimerController.trackReportExecution(() -> {
            List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();

            List<PharmaceuticalBillItem> pbis = billService.fetchPharmaceuticalBillItems(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
            bundle = new IncomeBundle(pbis);
            bundle.generateRetailAndCostDetailsForPharmaceuticalBillType();
        }, SummaryReports.PHARMACY_INCOME_REPORT, sessionController.getLoggedUser());
    }

    public void calPharmacyIncomeAndCostReportByBill() {
        List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();
        List<Bill> pbis = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
        for (Bill b : pbis) {
            if (b == null) {
                continue;
            }

            BillTypeAtomic bta = Optional
                    .ofNullable(b)
                    .map(Bill::getBillTypeAtomic)
                    .orElse(null);
            if (bta == null || bta.getBillCategory() == null) {
                continue; // unable to categorise safely
            }
            BillCategory bc = bta.getBillCategory();

            Double saleValue = 0.0;
            Double purchaseValue = 0.0;

            for (BillItem bi : b.getBillItems()) {

                if (bi == null || bi.getPharmaceuticalBillItem() == null) {
                    continue;
                }

                Double q = bi.getPharmaceuticalBillItem().getQty();
                Double rRate = bi.getPharmaceuticalBillItem().getRetailRate();
                if (bta == BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS) {
                    rRate = bi.getNetRate();
                }

                Double pRate = bi.getPharmaceuticalBillItem().getPurchaseRate();

                if (q == null || rRate == null || pRate == null) {
                    continue;
                }

                double qty = Math.abs(q);
                double retail = Math.abs(rRate);
                double purchase = Math.abs(pRate);

                double retailTotal = 0;
                double purchaseTotal = 0;

                switch (bc) {
                    case BILL:
                    case PAYMENTS:
                    case PREBILL:
                        retailTotal = retail * qty;
                        purchaseTotal = purchase * qty;
                        break;

                    case CANCELLATION:
                    case REFUND:
                        retailTotal = -retail * qty;
                        purchaseTotal = -purchase * qty;
                        break;

                    default:
                        break;
                }
                saleValue += retailTotal;
                purchaseValue += purchaseTotal;
            }
            if (b.getBillFinanceDetails() == null) {
                b.setBillFinanceDetails(new BillFinanceDetails());
            }

            b.getBillFinanceDetails().setTotalRetailSaleValue(BigDecimal.valueOf(saleValue));
            b.getBillFinanceDetails().setTotalPurchaseValue(BigDecimal.valueOf(purchaseValue));
            getBillFacade().edit(b);
        }
    }

// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Constructors">
    public PharmacySummaryReportController() {
    }
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Getters and Setters">

    /**
     * @return the billFacade
     */
    public BillFacade getBillFacade() {
        return billFacade;
    }

    /**
     * @param billFacade the billFacade to set
     */
    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    /**
     * @return the paymentFacade
     */
    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    /**
     * @param paymentFacade the paymentFacade to set
     */
    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    /**
     * @return the billFeeFacade
     */
    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    /**
     * @param billFeeFacade the billFeeFacade to set
     */
    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    /**
     * @return the billItemFacade
     */
    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    /**
     * @param billItemFacade the billItemFacade to set
     */
    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    /**
     * @return the pharmacyBean
     */
    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    /**
     * @param pharmacyBean the pharmacyBean to set
     */
    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    /**
     * @return the stockFacade
     */
    public StockFacade getStockFacade() {
        return stockFacade;
    }

    /**
     * @param stockFacade the stockFacade to set
     */
    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    /**
     * @return the patientFacade
     */
    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    /**
     * @param patientFacade the patientFacade to set
     */
    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    /**
     * @return the drawerFacade
     */
    public DrawerFacade getDrawerFacade() {
        return drawerFacade;
    }

    /**
     * @param drawerFacade the drawerFacade to set
     */
    public void setDrawerFacade(DrawerFacade drawerFacade) {
        this.drawerFacade = drawerFacade;
    }

    /**
     * @return the billBean
     */
    public BillBeanController getBillBean() {
        return billBean;
    }

    /**
     * @param billBean the billBean to set
     */
    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    /**
     * @return the sessionController
     */
    public SessionController getSessionController() {
        return sessionController;
    }

    /**
     * @param sessionController the sessionController to set
     */
    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    /**
     * @return the transferController
     */
    public TransferController getTransferController() {
        return transferController;
    }

    /**
     * @param transferController the transferController to set
     */
    public void setTransferController(TransferController transferController) {
        this.transferController = transferController;
    }

    /**
     * @return the pharmacySaleBhtController
     */
    public PharmacySaleBhtController getPharmacySaleBhtController() {
        return pharmacySaleBhtController;
    }

    /**
     * @param pharmacySaleBhtController the pharmacySaleBhtController to set
     */
    public void setPharmacySaleBhtController(PharmacySaleBhtController pharmacySaleBhtController) {
        this.pharmacySaleBhtController = pharmacySaleBhtController;
    }

    /**
     * @return the webUserController
     */
    public WebUserController getWebUserController() {
        return webUserController;
    }

    /**
     * @param webUserController the webUserController to set
     */
    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    /**
     * @return the opdPreSettleController
     */
    public OpdPreSettleController getOpdPreSettleController() {
        return opdPreSettleController;
    }

    /**
     * @param opdPreSettleController the opdPreSettleController to set
     */
    public void setOpdPreSettleController(OpdPreSettleController opdPreSettleController) {
        this.opdPreSettleController = opdPreSettleController;
    }

    /**
     * @return the pharmacyPreSettleController
     */
    public PharmacyPreSettleController getPharmacyPreSettleController() {
        return pharmacyPreSettleController;
    }

    /**
     * @param pharmacyPreSettleController the pharmacyPreSettleController to set
     */
    public void setPharmacyPreSettleController(PharmacyPreSettleController pharmacyPreSettleController) {
        this.pharmacyPreSettleController = pharmacyPreSettleController;
    }

    /**
     * @return the tokenController
     */
    public TokenController getTokenController() {
        return tokenController;
    }

    /**
     * @param tokenController the tokenController to set
     */
    public void setTokenController(TokenController tokenController) {
        this.tokenController = tokenController;
    }

    /**
     * @return the departmentController
     */
    public DepartmentController getDepartmentController() {
        return departmentController;
    }

    /**
     * @param departmentController the departmentController to set
     */
    public void setDepartmentController(DepartmentController departmentController) {
        this.departmentController = departmentController;
    }

    /**
     * @return the billSearch
     */
    public BillSearch getBillSearch() {
        return billSearch;
    }

    /**
     * @param billSearch the billSearch to set
     */
    public void setBillSearch(BillSearch billSearch) {
        this.billSearch = billSearch;
    }

    /**
     * @return the pharmacyBillSearch
     */
    public PharmacyBillSearch getPharmacyBillSearch() {
        return pharmacyBillSearch;
    }

    /**
     * @param pharmacyBillSearch the pharmacyBillSearch to set
     */
    public void setPharmacyBillSearch(PharmacyBillSearch pharmacyBillSearch) {
        this.pharmacyBillSearch = pharmacyBillSearch;
    }

    /**
     * @return the opdBillController
     */
    public OpdBillController getOpdBillController() {
        return opdBillController;
    }

    /**
     * @param opdBillController the opdBillController to set
     */
    public void setOpdBillController(OpdBillController opdBillController) {
        this.opdBillController = opdBillController;
    }

    /**
     * @return the configOptionApplicationController
     */
    public ConfigOptionApplicationController getConfigOptionApplicationController() {
        return configOptionApplicationController;
    }

    /**
     * @param configOptionApplicationController the
     * configOptionApplicationController to set
     */
    public void setConfigOptionApplicationController(ConfigOptionApplicationController configOptionApplicationController) {
        this.configOptionApplicationController = configOptionApplicationController;
    }

    /**
     * @return the channelSearchController
     */
    public ChannelSearchController getChannelSearchController() {
        return channelSearchController;
    }

    /**
     * @param channelSearchController the channelSearchController to set
     */
    public void setChannelSearchController(ChannelSearchController channelSearchController) {
        this.channelSearchController = channelSearchController;
    }

    /**
     * @return the reportTemplateController
     */
    public ReportTemplateController getReportTemplateController() {
        return reportTemplateController;
    }

    /**
     * @param reportTemplateController the reportTemplateController to set
     */
    public void setReportTemplateController(ReportTemplateController reportTemplateController) {
        this.reportTemplateController = reportTemplateController;
    }

    /**
     * @return the cashBookEntryController
     */
    public CashBookEntryController getCashBookEntryController() {
        return cashBookEntryController;
    }

    /**
     * @param cashBookEntryController the cashBookEntryController to set
     */
    public void setCashBookEntryController(CashBookEntryController cashBookEntryController) {
        this.cashBookEntryController = cashBookEntryController;
    }

    /**
     * @return the excelController
     */
    public ExcelController getExcelController() {
        return excelController;
    }

    /**
     * @param excelController the excelController to set
     */
    public void setExcelController(ExcelController excelController) {
        this.excelController = excelController;
    }

    /**
     * @return the pdfController
     */
    public PdfController getPdfController() {
        return pdfController;
    }

    /**
     * @param pdfController the pdfController to set
     */
    public void setPdfController(PdfController pdfController) {
        this.pdfController = pdfController;
    }

    /**
     * @return the drawerEntryController
     */
    public DrawerEntryController getDrawerEntryController() {
        return drawerEntryController;
    }

    /**
     * @param drawerEntryController the drawerEntryController to set
     */
    public void setDrawerEntryController(DrawerEntryController drawerEntryController) {
        this.drawerEntryController = drawerEntryController;
    }

    /**
     * @return the drawerController
     */
    public DrawerController getDrawerController() {
        return drawerController;
    }

    /**
     * @param drawerController the drawerController to set
     */
    public void setDrawerController(DrawerController drawerController) {
        this.drawerController = drawerController;
    }

    /**
     * @return the enumController
     */
    public EnumController getEnumController() {
        return enumController;
    }

    /**
     * @param enumController the enumController to set
     */
    public void setEnumController(EnumController enumController) {
        this.enumController = enumController;
    }

    /**
     * @return the visitType
     */
    public String getVisitType() {
        return visitType;
    }

    /**
     * @param visitType the visitType to set
     */
    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    /**
     * @return the methodType
     */
    public String getMethodType() {
        return methodType;
    }

    /**
     * @param methodType the methodType to set
     */
    public void setMethodType(String methodType) {
        this.methodType = methodType;
    }

    /**
     * @return the searchType
     */
    public String getSearchType() {
        return searchType;
    }

    /**
     * @param searchType the searchType to set
     */
    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    /**
     * @return the reportType
     */
    public String getReportType() {
        return reportType;
    }

    /**
     * @param reportType the reportType to set
     */
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    /**
     * @return the fromDate
     */
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    /**
     * @param fromDate the fromDate to set
     */
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    /**
     * @return the toDate
     */
    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    /**
     * @param toDate the toDate to set
     */
    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    /**
     * @return the category
     */
    public Category getCategory() {
        return category;
    }

    /**
     * @param category the category to set
     */
    public void setCategory(Category category) {
        this.category = category;
    }

    /**
     * @return the billType
     */
    public BillType getBillType() {
        return billType;
    }

    /**
     * @param billType the billType to set
     */
    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    /**
     * @return the billTypeAtomic
     */
    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    /**
     * @param billTypeAtomic the billTypeAtomic to set
     */
    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    /**
     * @return the billClassType
     */
    public BillClassType getBillClassType() {
        return billClassType;
    }

    /**
     * @param billClassType the billClassType to set
     */
    public void setBillClassType(BillClassType billClassType) {
        this.billClassType = billClassType;
    }

    /**
     * @return the paymentMethod
     */
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    /**
     * @param paymentMethod the paymentMethod to set
     */
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    /**
     * @return the paymentMethods
     */
    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    /**
     * @param paymentMethods the paymentMethods to set
     */
    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    /**
     * @return the webUser
     */
    public WebUser getWebUser() {
        return webUser;
    }

    /**
     * @param webUser the webUser to set
     */
    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    /**
     * @return the staff
     */
    public Staff getStaff() {
        return staff;
    }

    /**
     * @param staff the staff to set
     */
    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    /**
     * @return the currentStaff
     */
    public Staff getCurrentStaff() {
        return currentStaff;
    }

    /**
     * @param currentStaff the currentStaff to set
     */
    public void setCurrentStaff(Staff currentStaff) {
        this.currentStaff = currentStaff;
    }

    /**
     * @return the institution
     */
    public Institution getInstitution() {
        return institution;
    }

    /**
     * @param institution the institution to set
     */
    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    /**
     * @return the creditCompany
     */
    public Institution getCreditCompany() {
        return creditCompany;
    }

    /**
     * @param creditCompany the creditCompany to set
     */
    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    /**
     * @return the dealer
     */
    public Institution getDealer() {
        return dealer;
    }

    /**
     * @param dealer the dealer to set
     */
    public void setDealer(Institution dealer) {
        this.dealer = dealer;
    }

    /**
     * @return the site
     */
    public Institution getSite() {
        return site;
    }

    /**
     * @param site the site to set
     */
    public void setSite(Institution site) {
        this.site = site;
    }

    /**
     * @return the toSite
     */
    public Institution getToSite() {
        return toSite;
    }

    /**
     * @param toSite the toSite to set
     */
    public void setToSite(Institution toSite) {
        this.toSite = toSite;
    }

    /**
     * @return the fromInstitution
     */
    public Institution getFromInstitution() {
        return fromInstitution;
    }

    /**
     * @param fromInstitution the fromInstitution to set
     */
    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    /**
     * @return the toInstitution
     */
    public Institution getToInstitution() {
        return toInstitution;
    }

    /**
     * @param toInstitution the toInstitution to set
     */
    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    /**
     * @return the department
     */
    public Department getDepartment() {
        return department;
    }

    /**
     * @param department the department to set
     */
    public void setDepartment(Department department) {
        this.department = department;
    }

    /**
     * @return the fromDepartment
     */
    public Department getFromDepartment() {
        return fromDepartment;
    }

    /**
     * @param fromDepartment the fromDepartment to set
     */
    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    /**
     * @return the toDepartment
     */
    public Department getToDepartment() {
        return toDepartment;
    }

    /**
     * @param toDepartment the toDepartment to set
     */
    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    /**
     * @return the patient
     */
    public Patient getPatient() {
        return patient;
    }

    /**
     * @param patient the patient to set
     */
    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    /**
     * @return the patientEncounter
     */
    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    /**
     * @param patientEncounter the patientEncounter to set
     */
    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    /**
     * @return the item
     */
    public Item getItem() {
        return item;
    }

    /**
     * @param item the item to set
     */
    public void setItem(Item item) {
        this.item = item;
    }

    /**
     * @return the searchKeyword
     */
    public SearchKeyword getSearchKeyword() {
        return searchKeyword;
    }

    /**
     * @param searchKeyword the searchKeyword to set
     */
    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    /**
     * @return the reportKeyWord
     */
    public ReportKeyWord getReportKeyWord() {
        return reportKeyWord;
    }

    /**
     * @param reportKeyWord the reportKeyWord to set
     */
    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    /**
     * @return the bundle
     */
    public IncomeBundle getBundle() {
        return bundle;
    }

    /**
     * @param bundle the bundle to set
     */
    public void setBundle(IncomeBundle bundle) {
        this.bundle = bundle;
    }

    /**
     * @return the downloadingExcel
     */
    public StreamedContent getDownloadingExcel() {
        return downloadingExcel;
    }

    /**
     * @param downloadingExcel the downloadingExcel to set
     */
    public void setDownloadingExcel(StreamedContent downloadingExcel) {
        this.downloadingExcel = downloadingExcel;
    }

    /**
     * @return the file
     */
    public UploadedFile getFile() {
        return file;
    }

    /**
     * @param file the file to set
     */
    public void setFile(UploadedFile file) {
        this.file = file;
    }

    /**
     * @return the maxResult
     */
    public int getMaxResult() {
        return maxResult;
    }

    /**
     * @param maxResult the maxResult to set
     */
    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
    }

// </editor-fold>
    public BillService getBillService() {
        return billService;
    }

    public void setBillService(BillService billService) {
        this.billService = billService;
    }

    public ReportTemplateRowBundle getBundleReport() {
        return bundleReport;
    }

    public void setBundleReport(ReportTemplateRowBundle bundleReport) {
        this.bundleReport = bundleReport;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public Long getRowsPerPageForScreen() {
        rowsPerPageForScreen = configOptionApplicationController.getLongValueByKey("Pharmacy Analytics - Rows per Page for Printing", 20L);
        return rowsPerPageForScreen;
    }

    public void setRowsPerPageForScreen(Long rowsPerPageForScreen) {
        this.rowsPerPageForScreen = rowsPerPageForScreen;
    }

    public Long getRowsPerPageForPrinting() {
        rowsPerPageForPrinting = configOptionApplicationController.getLongValueByKey("Pharmacy Analytics - Rows per Page for Screen", 20L);
        return rowsPerPageForPrinting;
    }

    public void setRowsPerPageForPrinting(Long rowsPerPageForPrinting) {
        this.rowsPerPageForPrinting = rowsPerPageForPrinting;
    }

    public String getFontSizeForPrinting() {
        String value = configOptionApplicationController.getShortTextValueByKey("Pharmacy Analytics - Font Size for Printing", "10pt");
        if (value.matches("^\\d+$")) {
            value += "pt";
        }
        fontSizeForPrinting = value;
        return fontSizeForPrinting;
    }

    public String getFontSizeForScreen() {
        String value = configOptionApplicationController.getShortTextValueByKey("Pharmacy Analytics - Font Size for Screen", "1em");
        if (value.matches("^\\d+$")) {
            value += "em";
        }
        fontSizeForScreen = value;
        return fontSizeForScreen;
    }

    public void setFontSizeForPrinting(String fontSizeForPrinting) {
        this.fontSizeForPrinting = fontSizeForPrinting;
    }

    public void setFontSizeForScreen(String fontSizeForScreen) {
        this.fontSizeForScreen = fontSizeForScreen;
    }

    public DailyStockBalanceReport getDailyStockBalanceReport() {
        return dailyStockBalanceReport;
    }

    public void setDailyStockBalanceReport(DailyStockBalanceReport dailyStockBalanceReport) {
        this.dailyStockBalanceReport = dailyStockBalanceReport;
    }

    public ReportViewType getReportViewType() {
        return reportViewType;
    }

    public void setReportViewType(ReportViewType reportViewType) {
        this.reportViewType = reportViewType;
    }

    public PharmacyBundle getPharmacyBundle() {
        return pharmacyBundle;
    }

    public void setPharmacyBundle(PharmacyBundle pharmacyBundle) {
        this.pharmacyBundle = pharmacyBundle;
    }

    public List<ReportViewType> getReportViewTypes() {
        if (reportViewTypes == null) {
            reportViewTypes = new ArrayList<>(Arrays.asList(ReportViewType.values()));
        }
        return reportViewTypes;
    }

    public void setReportViewTypes(List<ReportViewType> reportViewTypes) {
        this.reportViewTypes = reportViewTypes;
    }

    private void addCurrentItemStock(PharmacyBundle pharmacyBundle) {
        if (pharmacyBundle == null) {
            return;
        }
        for (PharmacyRow pr : pharmacyBundle.getRows()) {
            if (pr.getItem() == null) {
                continue;
            }
            pr.setStockQty(stockController.findStock(institution, site, department, pr.getItem()));
        }
    }
    
    private void addCurrentItemStock(List<MovementOutStockReportByItemDto> dtoList) {
        if (dtoList == null) {
            return;
        }

        for (MovementOutStockReportByItemDto dto : dtoList) {
            if (dto.getItemId() == 0) {
                continue;
            }

            dto.setCurrentStock(stockController.findStock(institution, site, department, dto.getItemId()));
            
        }
    }

}
