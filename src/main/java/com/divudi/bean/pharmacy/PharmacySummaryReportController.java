package com.divudi.bean.pharmacy;

// <editor-fold defaultstate="collapsed" desc="Import Statements">
import com.divudi.bean.common.*;
import com.divudi.bean.cashTransaction.CashBookEntryController;
import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.DrawerEntryController;
import com.divudi.bean.channel.ChannelSearchController;
import com.divudi.bean.channel.analytics.ReportTemplateController;
import com.divudi.core.data.dto.PharmacyIncomeBillDTO;
import com.divudi.core.data.dto.PharmacyIncomeBillItemDTO;
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
import com.divudi.core.facade.StockHistoryFacade;
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
import com.divudi.core.data.PharmacyBundle;
import com.divudi.core.data.PharmacyRow;
import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.ReportTemplateRowBundle;
import com.divudi.core.data.ReportViewType;

import static com.divudi.core.data.ReportViewType.BY_BILL;
import static com.divudi.core.data.ReportViewType.BY_BILL_TYPE;
import static com.divudi.core.data.ReportViewType.BY_BILL_TYPE_AND_DISCOUNT_TYPE_AND_ADMISSION_TYPE;
import static com.divudi.core.data.ReportViewType.BY_DISCOUNT_TYPE_AND_ADMISSION_TYPE;

import com.divudi.core.data.dto.PharmacyIncomeCostBillDTO;
import com.divudi.core.data.pharmacy.DailyStockBalanceReport;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillFinanceDetails;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.HistoricalRecord;
import com.divudi.core.data.HistoricalRecordType;

import static com.divudi.core.data.ReportViewType.BY_BILL_ITEM;

import com.divudi.core.entity.BillItemFinanceDetails;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.StockBill;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.core.facade.DrawerFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.ejb.PharmacyService;
import com.divudi.service.BillService;
import com.divudi.service.HistoricalRecordService;
import com.divudi.core.facade.HistoricalRecordFacade;
import com.divudi.service.PharmacyAsyncReportService;
import com.divudi.core.data.HistoricalRecordType;
import com.divudi.core.facade.PharmaceuticalBillItemFacade;
import com.divudi.service.StockHistoryService;

import java.io.Serializable;
import java.math.BigDecimal;
import com.divudi.core.util.BigDecimalUtil;
import java.math.RoundingMode;
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
import com.divudi.core.entity.Upload;
import com.divudi.core.facade.UploadFacade;
import java.io.ByteArrayInputStream;
import org.primefaces.model.DefaultStreamedContent;

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
    private StockHistoryFacade stockHistoryFacade;
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
    HistoricalRecordFacade historicalRecordFacade;
    @EJB
    PharmacyAsyncReportService pharmacyAsyncReportService;
    @EJB
    PharmacyService pharmacyService;
    @EJB
    UploadFacade uploadFacade;

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
    @Inject
    BillController billController;
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

    private List<HistoricalRecord> historicalRecords;

    //transferOuts;
    //adjustments;
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Navigators">
    public String navigateToPharmacyIncomeReport() {
        return "/pharmacy/reports/summary_reports/pharmacy_income_report?faces-redirect=true";
    }

    public String navigateToPharmacyMovementOutBySaleIssueAndConsumptionWithCurrentStockReport() {
        reportViewTypes = Arrays.asList(
                ReportViewType.BY_BILL,
                ReportViewType.BY_BILL_TYPE,
                ReportViewType.BY_ITEM
        );
        reportViewType = ReportViewType.BY_ITEM;
        return "/pharmacy/reports/movement_reports/movement_out_by_sale_issue_and_consumption_with_current_stock_report?faces-redirect=true";
    }

    public String navigateToPharmacyProcurementReport() {
        return "/pharmacy/reports/procurement_reports/pharmacy_procurement_report?faces-redirect=true";
    }

    public String navigateToPharmacyIncomeAndCostReport() {
        reportViewTypes = Arrays.asList(
                ReportViewType.BY_BILL,
                ReportViewType.BY_BILL_TYPE,
                ReportViewType.BY_BILL_ITEM
        );
        reportViewType = ReportViewType.BY_BILL;
        return "/pharmacy/reports/summary_reports/pharmacy_income_and_cost_report?faces-redirect=true";
    }

    public String navigateToDailyStockValuesReport() {
        if(institution==null){
            institution = sessionController.getInstitution();
        }
        if(site==null){
            site = sessionController.getLoggedSite();
        }
        if(department==null){
            department=sessionController.getDepartment();
        }
        return "/pharmacy/reports/summary_reports/daily_stock_values_report?faces-redirect=true";
    }

    public String navigateToBillTypeIncome() {
        return "/pharmacy/reports/summary_reports/bill_type_income?faces-redirect=true";
    }

    public String navigateToAllItemMovementSummary() {
        return "/pharmacy/reports/summary_reports/all_item_movement_summary?faces-redirect=true";
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
        
//        Report can be generated for today as well
//        Date today = new Date();
//        if (!fromDate.before(today)) {
//            JsfUtil.addErrorMessage("Selected date must be earlier than today");
//            return;
//        }

        dailyStockBalanceReport = new DailyStockBalanceReport();
        dailyStockBalanceReport.setDate(fromDate);
        dailyStockBalanceReport.setDepartment(department);

        // Calculate Opening Stock Value at Retail Rate
        double openingStockValueAtRetailRate = calculateStockValueAtRetailRate(fromDate, department);
        dailyStockBalanceReport.setOpeningStockValue(openingStockValueAtRetailRate);

        // Calculate toDate as fromDate + 1 day
        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        cal.add(Calendar.DATE, 1);
        toDate = cal.getTime();

        Date startOfTheDay = CommonFunctions.getStartOfDay(fromDate);
        Date endOfTheDay = CommonFunctions.getEndOfDay(fromDate);

        PharmacyBundle saleBundle = pharmacyService.fetchPharmacyIncomeByBillTypeAndDiscountTypeAndAdmissionType(startOfTheDay, endOfTheDay, null, null, department, null, null, null);
        dailyStockBalanceReport.setPharmacySalesByAdmissionTypeAndDiscountSchemeBundle(saleBundle);

        PharmacyBundle purchaseBundle = pharmacyService.fetchPharmacyStockPurchaseValueByBillTypeDto(startOfTheDay, endOfTheDay, null, null, department, null, null, null);
        dailyStockBalanceReport.setPharmacyPurchaseByBillTypeBundle(purchaseBundle);

        PharmacyBundle transferBundle = pharmacyService.fetchPharmacyTransferValueByBillTypeDto(startOfTheDay, endOfTheDay, null, null, department, null, null, null);
        dailyStockBalanceReport.setPharmacyTransferByBillTypeBundle(transferBundle);

        PharmacyBundle adjustmentBundle = pharmacyService.fetchPharmacyAdjustmentValueByBillTypeDto(startOfTheDay, endOfTheDay, null, null, department, null, null, null);
        dailyStockBalanceReport.setPharmacyAdjustmentsByBillTypeBundle(adjustmentBundle);

        // Calculate Closing Stock Value at Retail Rate
        double closingStockValueAtRetailRate = calculateStockValueAtRetailRate(toDate, department);
        dailyStockBalanceReport.setClosingStockValue(closingStockValueAtRetailRate);
//        }, SummaryReports.DAILY_STOCK_BALANCE_REPORT, sessionController.getLoggedUser());
    }

    /**
     * Calculates the stock value at retail rate for a given date and department.
     * This method queries the StockHistory to find the latest stock quantities
     * before the specified date and multiplies them by the retail sale rate.
     *
     * @param date The date for which to calculate stock value
     * @param dept The department for which to calculate stock value
     * @return The total stock value at retail rate, or 0.0 if calculation fails
     */
    private double calculateStockValueAtRetailRate(Date date, Department dept) {
        try {
            Map<String, Object> params = new HashMap<>();
            StringBuilder jpql = new StringBuilder();

            // Query to calculate total retail value of stock
            jpql.append("SELECT SUM(sh.stockQty * COALESCE(sh.itemBatch.retailsaleRate, 0.0)) ")
                .append("FROM StockHistory sh ")
                .append("WHERE sh.retired = :ret ")
                .append("AND sh.id IN (")
                .append("SELECT MAX(sh2.id) FROM StockHistory sh2 ")
                .append("WHERE sh2.retired = :ret ")
                .append("AND sh2.createdAt < :et ");

            params.put("ret", false);
            params.put("et", date);

            // Add department filter to subquery
            if (dept != null) {
                jpql.append("AND sh2.department = :dep ");
                params.put("dep", dept);
            }

            jpql.append("GROUP BY sh2.department, sh2.itemBatch ")
                .append("HAVING MAX(sh2.id) IN (")
                .append("SELECT sh3.id FROM StockHistory sh3 ")
                .append("WHERE sh3.retired = :ret ");

            // Add department filter to innermost query
            if (dept != null) {
                jpql.append("AND sh3.department = :dep2 ");
                params.put("dep2", dept);
            }

            jpql.append("AND sh3.createdAt < :et2)) ");
            params.put("et2", date);

            // Add department filter to main query
            if (dept != null) {
                jpql.append("AND sh.department = :dep3 ");
                params.put("dep3", dept);
            }

            // Filter to include only items with positive stock quantities
            jpql.append("AND sh.itemBatch.item.id IN (")
                .append("SELECT sh4.itemBatch.item.id FROM StockHistory sh4 ")
                .append("WHERE sh4.retired = :ret ")
                .append("AND sh4.id IN (")
                .append("SELECT MAX(sh5.id) FROM StockHistory sh5 ")
                .append("WHERE sh5.retired = :ret ")
                .append("AND sh5.createdAt < :et3 ");

            params.put("et3", date);

            // Add department filter to item filtering subqueries
            if (dept != null) {
                jpql.append("AND sh5.department = :dep4 ");
                params.put("dep4", dept);
            }

            jpql.append("GROUP BY sh5.department, sh5.itemBatch) ");

            if (dept != null) {
                jpql.append("AND sh4.department = :dep5 ");
                params.put("dep5", dept);
            }

            jpql.append("GROUP BY sh4.itemBatch.item.id ")
                .append("HAVING SUM(sh4.stockQty) > 0)");

            // Execute the query
            List<Object[]> results = stockHistoryFacade.findRawResultsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

            if (results != null && !results.isEmpty() && results.get(0) != null) {
                Object result = results.get(0);
                // Since we're selecting a single SUM value, it comes as a single Object, not an array
                if (result instanceof Object[]) {
                    Object[] resultArray = (Object[]) result;
                    return resultArray[0] != null ? ((Number) resultArray[0]).doubleValue() : 0.0;
                } else {
                    return result != null ? ((Number) result).doubleValue() : 0.0;
                }
            } else {
                return 0.0;
            }

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error calculating stock value at retail rate for date: " + date);
            return 0.0;
        }
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
                jpql.append(" and b.department.site = :site ");
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
                case BY_BILL_TYPE:
                    processMovementOutWithStockReportByBillType();
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

    public void generateAllItemMovementReport() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both from and to dates");
            return;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("From date must be before or equal to to date");
            return;
        }

        HistoricalRecord hr = new HistoricalRecord();
        hr.setHistoricalRecordType(HistoricalRecordType.ASYNC_REPORT);
        hr.setVariableName("AllItemMovementSummary");
        hr.setFromDateTime(fromDate);
        hr.setToDateTime(toDate);
        hr.setInstitution(institution);
        hr.setSite(site);
        hr.setDepartment(department);
        hr.setCreatedAt(new Date());
        hr.setCreatedBy(sessionController.getLoggedUser());
        historicalRecordFacade.create(hr);
        pharmacyAsyncReportService.generateAllItemMovementReport(hr, sessionController.getApplicationPreference().getLongDateTimeFormat());

        JsfUtil.addSuccessMessage("Async report generation request added");
        viewAlreadyAvailableAllItemMovementSummaryReports();
    }

    public void viewAlreadyAvailableAllItemMovementSummaryReports() {
        StringBuilder jpql = new StringBuilder("select hr from HistoricalRecord hr "
                + "where hr.retired=false "
                + "and hr.historicalRecordType=:type "
                + "and hr.variableName=:vn ");

        Map<String, Object> params = new HashMap<>();
        params.put("type", HistoricalRecordType.ASYNC_REPORT);
        params.put("vn", "AllItemMovementSummary");

        if (institution != null) {
            jpql.append(" and hr.institution=:ins ");
            params.put("ins", institution);
        } else {
            jpql.append(" and hr.institution is null ");
        }

        if (site != null) {
            jpql.append(" and hr.site=:site ");
            params.put("site", site);
        } else {
            jpql.append(" and hr.site is null ");
        }

        if (department != null) {
            jpql.append(" and hr.department=:dep ");
            params.put("dep", department);
        } else {
            jpql.append(" and hr.department is null ");
        }

        if (fromDate != null) {
            jpql.append(" and hr.fromDateTime = :fd ");
            params.put("fd", fromDate);
        }

        if (toDate != null) {
            jpql.append(" and hr.toDateTime = :td ");
            params.put("td", toDate);
        }

        jpql.append(" order by hr.fromDateTime desc");

        historicalRecords = historicalRecordFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    public void processPharmacyIncomeReportByBillType() {
        List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();

        List<PharmacyIncomeBillDTO> dtos = billService.fetchBillsAsPharmacyIncomeBillDTOs(
                fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
        bundle = new IncomeBundle(dtos);
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
        List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();

        List<PharmacyIncomeBillDTO> dtos = billService.fetchBillsAsPharmacyIncomeBillDTOs(
                fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
        bundle = new IncomeBundle(dtos);
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
    }

    public void processPharmacyIncomeReportByBillTypeAndDiscountTypeAndAdmissionType() {
        List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();
        List<PharmacyIncomeBillDTO> dtos = billService.fetchBillsAsPharmacyIncomeBillDTOs(
                fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
        bundle = new IncomeBundle(dtos);
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
    }

    public void processPharmacyIncomeReportByBill() {
        List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();
        List<PharmacyIncomeBillDTO> dtos = billService.fetchBillsAsPharmacyIncomeBillDTOs(
                fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);

        bundle = new IncomeBundle(dtos);
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
//                    processPharmacyIncomeAndCostReportByBill();
                    processPharmacyIncomeAndCostReportByBillDto();
                    break;
                default:
                    JsfUtil.addErrorMessage("Unsupported report view type.");
                    break;
            }
        }, SummaryReports.PHARMACY_INCOME_AND_COST_REPORT, sessionController.getLoggedUser());
    }

    public void processPharmacyIncomeAndCostReportByBillItem() {
        List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();
        List<PharmacyIncomeBillItemDTO> dtos = billService.fetchPharmacyIncomeBillItemWithCostRateDTOs(
                fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
        bundle = new IncomeBundle(dtos);
        bundle.generateRetailAndCostDetailsForPharmaceuticalBillItems();
    }

    public void processMovementOutWithStockReportByItem() {
        reportTimerController.trackReportExecution(() -> {
            List<BillTypeAtomic> billTypeAtomics = getPharmacyMovementOutBillTypes();
            List<PharmaceuticalBillItem> pbis = billService.fetchPharmaceuticalBillItems(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
            pharmacyBundle = new PharmacyBundle(pbis);
            pharmacyBundle.groupSaleDetailsByItems();
        }, SummaryReports.PHARMACY_MOVEMENT_OUT_REPORT, sessionController.getLoggedUser());
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
        List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();
        List<PharmacyIncomeBillItemDTO> dtos = billService.fetchPharmacyIncomeBillItemDTOs(
                fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);
        bundle = new IncomeBundle(dtos);
        bundle.generateRetailAndCostDetailsForPharmaceuticalBillType();
    }

    public void processPharmacyIncomeAndCostReportByBillDto() {

        List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();

        List<PharmacyIncomeBillDTO> dtos = billService.fetchBillsAsPharmacyIncomeBillDTOs(
                fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);


        bundle = new IncomeBundle(dtos);
        bundle.generateRetailAndCostDetailsForPharmaceuticalBill();

    }

    /**
     * Calculates and assigns financial details for the given {@code BillItem}'s
     * {@code BillItemFinanceDetails} during a **pharmacy sale**.
     * <p>
     * This method must only modify the {@code BillItemFinanceDetails} of the
     * provided {@code BillItem}. No other data related to the {@code BillItem},
     * its parent {@code Bill}, or the associated {@code PharmaceuticalBillItem}
     * should be altered.
     * <p>
     * The calculation logic differs from purchase-related calculations (e.g.,
     * direct purchases) as this method is designed exclusively for sales
     * scenarios. All rates and totals computed here are based on **sale
     * values**, not cost or purchase values.
     * <p>
     * Key steps: - Compute line-level values (rates, amounts) using available
     * data from {@code BillItem} and {@code PharmaceuticalBillItem}. - Derive a
     * proportion using the {@code BillItem}'s net value as a ratio of the total
     * bill net value. - Use this proportion to allocate the {@code Bill}'s
     * total discount, tax, and expense values to the current item. - Convert
     * all quantities into atomic units based on the item's unit-per-pack
     * conversion factor (1 for AMP, specific value for AMPP). - Assign all
     * computed line-level, bill-level, and combined totals and rates to the
     * corresponding fields in {@code BillItemFinanceDetails}. - Avoid any
     * potential {@code NullPointerException} by ensuring all values are
     * non-null and safely handled.
     * <p>
     * Reference Logic: - lineGrossTotal = bi.getGrossValue() - billGrossTotal =
     * 0 (reserved for future use) - grossTotal = lineGrossTotal +
     * billGrossTotal
     * <p>
     * - lineNetTotal = bi.getNetValue() - billNetTotal = (bill.tax +
     * bill.expenses - bill.discount) * proportion - netTotal = lineNetTotal +
     * billNetTotal
     * <p>
     * - Per-unit bill rates (discount/tax/expense) are calculated by dividing
     * the proportional value by quantity in units.
     * <p>
     * NOTE: - All input data used here (from {@code Bill}, {@code BillItem},
     * and {@code PharmaceuticalBillItem}) must not be changed within this
     * method. - All primitives (double) are assumed non-null and directly
     * accessible.
     * <p>
     * Available Input Data (read-only during this method): - Bill:
     * getDiscount(), getTax(), getExpenseTotal(), getNetTotal() - BillItem:
     * getItem(), getRate(), getGrossValue(), getNetRate(), getNetValue(),
     * getDiscount(), getDiscountRate() - PharmaceuticalBillItem: getQty(),
     * getFreeQty(), getItemBatch().getCostRate() - Item: getDblValue() (used as
     * unitsPerPack if AMPP)
     *
     * @param bi
     */
    public void addMissingDataToBillItemFinanceDetailsWhenPharmaceuticalBillItemsAreAvailableForPharmacySale(BillItem bi) {
        if (bi == null || bi.getPharmaceuticalBillItem() == null) {
            return; // Exit if bill item or pharmaceutical bill item is null
        }

        Bill bill = bi.getBill(); // Get the associated bill
        PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem(); // Get pharmaceutical bill item

        // Create finance detail object if not already created
        BillItemFinanceDetails bifd = bi.getBillItemFinanceDetails();
        if (bifd == null) {
            bifd = new BillItemFinanceDetails(bi);
            bi.setBillItemFinanceDetails(bifd);
        }

        Item itemOfInputBillItem = bi.getItem(); // Get the item
        BigDecimal unitsPerPack = BigDecimal.ONE; // Default unitsPerPack

        // Calculate units per pack if item is AMPP
        if (itemOfInputBillItem instanceof Ampp) {
            double dblVal = itemOfInputBillItem.getDblValue();
            unitsPerPack = dblVal > 0.0 ? BigDecimal.valueOf(dblVal) : BigDecimal.ONE;
        }

        // Calculate qty and free qty based on whether item is AMPP
        BigDecimal qty = itemOfInputBillItem instanceof Ampp
                ? BigDecimal.valueOf(pbi.getQtyPacks())
                : BigDecimal.valueOf(pbi.getQty());

        BigDecimal freeQty = itemOfInputBillItem instanceof Ampp
                ? BigDecimal.valueOf(pbi.getFreeQtyPacks())
                : BigDecimal.valueOf(pbi.getFreeQty());

        // Total quantity = qty + free (using null-safe operations)
        BigDecimal totalQty = BigDecimalUtil.add(qty, freeQty);

        // Convert quantities to atomic units (using null-safe operations)
        BigDecimal qtyInUnits = BigDecimalUtil.multiply(qty, unitsPerPack);
        BigDecimal freeQtyInUnits = BigDecimalUtil.multiply(freeQty, unitsPerPack);
        BigDecimal totalQtyInUnits = BigDecimalUtil.add(qtyInUnits, freeQtyInUnits);

        // Assign quantity values to bifd
        bifd.setUnitsPerPack(unitsPerPack);
        bifd.setQuantity(qty);
        bifd.setFreeQuantity(freeQty);
        bifd.setTotalQuantity(totalQty);
        bifd.setQuantityByUnits(qtyInUnits);
        bifd.setFreeQuantityByUnits(freeQtyInUnits);
        bifd.setTotalQuantityByUnits(totalQtyInUnits);

        // Calculate proportion = line value / total net bill
        BigDecimal proportion = BigDecimal.ZERO;
        if (bill.getNetTotal() != 0) {
            proportion = BigDecimal.valueOf(bi.getNetValue())
                    .divide(BigDecimal.valueOf(bill.getNetTotal()), 10, RoundingMode.HALF_EVEN);
        }

        // Retrieve cost rate from item batch
        Double costRate = null;
        if (pbi.getItemBatch() != null) {
            costRate = pbi.getItemBatch().getCostRate();
        }

        // Assign cost rate values
        BigDecimal lineCostRate = costRate == null ? BigDecimal.ZERO : BigDecimal.valueOf(costRate);
        bifd.setLineCostRate(lineCostRate);
        bifd.setBillCostRate(BigDecimal.ZERO);
        bifd.setTotalCostRate(lineCostRate);

        // Assign cost values
        bifd.setLineCost(BigDecimalUtil.multiply(lineCostRate, totalQtyInUnits));
        bifd.setBillCost(BigDecimal.ZERO);
        bifd.setTotalCost(bifd.getLineCost());

        // Assign gross rate (sale rate)
        BigDecimal lineGrossRate = BigDecimal.valueOf(bi.getRate());
        bifd.setRetailSaleRate(lineGrossRate);
        bifd.setRetailSaleRatePerUnit(BigDecimalUtil.multiply(lineGrossRate, unitsPerPack));

        // Calculate discount, tax, and expense from bill using proportion
        BigDecimal discountPortionFromBill = BigDecimalUtil.multiply(BigDecimal.valueOf(bill.getDiscount()), proportion);
        BigDecimal taxPortionFromBill = BigDecimalUtil.multiply(BigDecimal.valueOf(bill.getTax()), proportion);
        BigDecimal expensePortionFromBill = BigDecimalUtil.multiply(BigDecimal.valueOf(bill.getExpenseTotal()), proportion);

        // Calculate the difference between gross and net as gap (using null-safe operations)
        BigDecimal gapPortionFromBillNetTotalFromBillGrossTotal = BigDecimalUtil.subtract(
                BigDecimalUtil.add(taxPortionFromBill, expensePortionFromBill),
                discountPortionFromBill);

        // Assign gross and net totals
        BigDecimal lineGrossTotal = BigDecimal.valueOf(bi.getGrossValue());
        BigDecimal billGrossTotal = BigDecimal.ZERO;
        BigDecimal grossTotal = BigDecimalUtil.add(lineGrossTotal, billGrossTotal);

        BigDecimal lineNetTotal = BigDecimal.valueOf(bi.getNetValue());
        BigDecimal billNetTotal = gapPortionFromBillNetTotalFromBillGrossTotal;
        BigDecimal netTotal = BigDecimalUtil.add(lineNetTotal, billNetTotal);

        BigDecimal lineDiscountTotal = BigDecimal.valueOf(bi.getDiscount());
        BigDecimal billDiscountTotal = discountPortionFromBill;
        BigDecimal discountTotal = BigDecimalUtil.add(lineDiscountTotal, billDiscountTotal);

        BigDecimal lineExpenseTotal = BigDecimal.ZERO;
        BigDecimal billExpenseTotal = expensePortionFromBill;
        BigDecimal expenseTotal = BigDecimalUtil.add(lineExpenseTotal, billExpenseTotal);

        BigDecimal lineTaxTotal = BigDecimal.ZERO;
        BigDecimal billTaxTotal = taxPortionFromBill;
        BigDecimal taxTotal = BigDecimalUtil.add(lineTaxTotal, billTaxTotal);

        // Assign gross totals
        bifd.setLineGrossTotal(lineGrossTotal);
        bifd.setBillGrossTotal(billGrossTotal);
        bifd.setGrossTotal(grossTotal);

        // Assign discount values
        bifd.setLineDiscount(lineDiscountTotal);
        bifd.setBillDiscount(billDiscountTotal);
        bifd.setTotalDiscount(discountTotal);

        // Assign tax values
        bifd.setLineTax(lineTaxTotal);
        bifd.setBillTax(billTaxTotal);
        bifd.setTotalTax(taxTotal);

        // Assign expense values
        bifd.setLineExpense(lineExpenseTotal);
        bifd.setBillExpense(billExpenseTotal);
        bifd.setTotalExpense(expenseTotal);

        // Assign net totals
        bifd.setLineNetTotal(lineNetTotal);
        bifd.setBillNetTotal(billNetTotal);
        bifd.setNetTotal(netTotal);

        // Calculate per-unit adjustments for rates
        BigDecimal rateOfDiscountPortionFromBill = BigDecimal.ZERO;
        BigDecimal rateOfExpensePortionFromBill = BigDecimal.ZERO;
        BigDecimal rateOfTaxPortionFromBill = BigDecimal.ZERO;

        if (qtyInUnits.compareTo(BigDecimal.ZERO) != 0) {
            rateOfDiscountPortionFromBill = discountPortionFromBill.divide(qtyInUnits, 10, RoundingMode.HALF_EVEN);
            rateOfTaxPortionFromBill = taxPortionFromBill.divide(qtyInUnits, 10, RoundingMode.HALF_EVEN);
            rateOfExpensePortionFromBill = expensePortionFromBill.divide(qtyInUnits, 10, RoundingMode.HALF_EVEN);
        }

        // Assign rates
        bifd.setLineGrossRate(lineGrossRate);
        bifd.setBillGrossRate(BigDecimal.ZERO);
        bifd.setGrossRate(lineGrossRate);

        BigDecimal lineNetRate = BigDecimal.valueOf(bi.getNetRate());
        bifd.setLineNetRate(lineNetRate);
        bifd.setBillNetRate(rateOfTaxPortionFromBill.add(rateOfExpensePortionFromBill).subtract(rateOfDiscountPortionFromBill));
        bifd.setNetRate(bifd.getLineNetRate().add(bifd.getBillNetRate()));

        BigDecimal lineDiscountRate = Optional.ofNullable(bifd.getLineDiscountRate())
                .filter(r -> r.compareTo(BigDecimal.ZERO) != 0)
                .orElse(BigDecimal.valueOf(bi.getDiscountRate()));
        bifd.setLineDiscountRate(lineDiscountRate);
        bifd.setBillDiscountRate(rateOfDiscountPortionFromBill);
        bifd.setTotalDiscountRate(lineDiscountRate.add(rateOfDiscountPortionFromBill));

        bifd.setLineExpenseRate(BigDecimal.ZERO);
        bifd.setBillExpenseRate(rateOfExpensePortionFromBill);
        bifd.setTotalExpenseRate(rateOfExpensePortionFromBill);

        bifd.setLineTaxRate(BigDecimal.valueOf(bi.getVat()));
        bifd.setBillTaxRate(rateOfTaxPortionFromBill);
        bifd.setTotalTaxRate(BigDecimal.valueOf(bi.getVat()).add(rateOfTaxPortionFromBill));
    }

    @Deprecated
    // WIll be deleted soon. Use addMissingDataToBillItemFinanceDetailsWhenPharmaceuticalBillItemsAreAvailableForPharmacySale
    public void addMissingDataToBillItemFinanceDetailsWhenPharmaceuticalBillItemsAreAvailableForPharmacySaleOld(BillItem bi) {
        if (bi == null || bi.getPharmaceuticalBillItem() == null) {
            return;
        }

        Bill bill = bi.getBill();

        PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();

        //Available data for calculations, should not be altered dueing this method - Start
        //from bill
        bill.getDiscount();  // double values , NOT Double or BigDecimal, so never null
        bill.getTax(); // double values , NOT Double or BigDecimal, so never null
        bill.getExpenseTotal(); // double values , NOT Double or BigDecimal, so never null
        bill.getNetTotal(); // double values , NOT Double or BigDecimal, so never null
        // from bill Item
        bi.getItem();
        bi.getItem().getDblValue(); // This is Units per pack in AMPPs, THis should be one for AMPs
        bi.getRate(); // double values , NOT Double or BigDecimal, so never null
        bi.getGrossValue(); // double values , NOT Double or BigDecimal, so never null
        bi.getNetRate(); // double values , NOT Double or BigDecimal, so never null
        bi.getNetValue(); // Net Total for Bill Item.  // double values , NOT Double or BigDecimal, so never null
        bi.getDiscount(); // double values , NOT Double or BigDecimal, so never null
        bi.getDiscountRate(); // double values , NOT Double or BigDecimal, so never null
        // from Pharmaceutical bill item
        pbi.getQty();  // double values , NOT Double or BigDecimal, so never null
        pbi.getFreeQty();  // double values , NOT Double or BigDecimal, so never null
        pbi.getItemBatch().getCostRate(); //Always Cost is by Units , NOT packs
        //Available data for calculations, should not be altered dueing this method - End

        BillItemFinanceDetails bifd = bi.getBillItemFinanceDetails();
        if (bifd == null) {
            bifd = new BillItemFinanceDetails(bi);
            bi.setBillItemFinanceDetails(bifd);
        }

        Item itemOfInputBillItem = bi.getItem();
        BigDecimal unitsPerPack = BigDecimal.ONE;
        if (itemOfInputBillItem instanceof Ampp) {
            double dblVal = itemOfInputBillItem.getDblValue();
            unitsPerPack = dblVal > 0.0 ? BigDecimal.valueOf(dblVal) : BigDecimal.ONE;
        }

        BigDecimal qty = itemOfInputBillItem instanceof Ampp
                ? BigDecimal.valueOf(pbi.getQtyPacks())
                : BigDecimal.valueOf(pbi.getQty());
        BigDecimal freeQty = itemOfInputBillItem instanceof Ampp
                ? BigDecimal.valueOf(pbi.getFreeQtyPacks())
                : BigDecimal.valueOf(pbi.getFreeQty());

        BigDecimal qtyInUnits = qty.multiply(unitsPerPack);
        BigDecimal freeQtyInUnits = freeQty.multiply(unitsPerPack);

        if (bifd.getUnitsPerPack() == null || bifd.getUnitsPerPack().compareTo(BigDecimal.ZERO) == 0) {
            bifd.setUnitsPerPack(unitsPerPack);
        }
        if (bifd.getQuantity() == null || bifd.getQuantity().compareTo(BigDecimal.ZERO) == 0) {
            bifd.setQuantity(qty);
        }
        if (bifd.getFreeQuantity() == null || bifd.getFreeQuantity().compareTo(BigDecimal.ZERO) == 0) {
            bifd.setFreeQuantity(freeQty);
        }
        if (bifd.getTotalQuantity() == null || bifd.getTotalQuantity().compareTo(BigDecimal.ZERO) == 0) {
            bifd.setTotalQuantity(qty.add(freeQty));
        }
        if (bifd.getQuantityByUnits() == null || bifd.getQuantityByUnits().compareTo(BigDecimal.ZERO) == 0) {
            bifd.setQuantityByUnits(qtyInUnits);
        }
        if (bifd.getFreeQuantityByUnits() == null || bifd.getFreeQuantityByUnits().compareTo(BigDecimal.ZERO) == 0) {
            bifd.setFreeQuantityByUnits(freeQtyInUnits);
        }
        if (bifd.getTotalQuantityByUnits() == null || bifd.getTotalQuantityByUnits().compareTo(BigDecimal.ZERO) == 0) {
            bifd.setTotalQuantityByUnits(qtyInUnits.add(freeQtyInUnits));
        }

        Double costRate = null;
        if (pbi.getItemBatch() != null) {
            costRate = pbi.getItemBatch().getCostRate();
        }
        BigDecimal lineCostRate = costRate == null ? BigDecimal.ZERO : BigDecimal.valueOf(costRate);
        bifd.setLineCostRate(lineCostRate);
        bifd.setBillCostRate(BigDecimal.ZERO);
        bifd.setTotalCostRate(BigDecimal.valueOf(costRate));

        double retailRate = bi.getRate();

        if (bifd.getRetailSaleRate() == null || bifd.getRetailSaleRate().compareTo(BigDecimal.ZERO) == 0) {
            if (itemOfInputBillItem instanceof Ampp) {
                bifd.setRetailSaleRate(BigDecimal.valueOf(retailRate));
            } else {
                bifd.setRetailSaleRate(BigDecimal.valueOf(retailRate));
            }

        }
        if (bifd.getRetailSaleRatePerUnit() == null || bifd.getRetailSaleRatePerUnit().compareTo(BigDecimal.ZERO) == 0) {
            if (itemOfInputBillItem instanceof Ampp) {
                bifd.setRetailSaleRatePerUnit(bifd.getRetailSaleRate().multiply(bifd.getUnitsPerPack()));
            } else {
                bifd.setRetailSaleRatePerUnit(bifd.getRetailSaleRate());
            }
        }

        bifd.setBillCost(BigDecimal.ZERO);
        bifd.setLineCost(bifd.getLineCostRate().multiply(bifd.getTotalQuantityByUnits()));
        bifd.setTotalCost(bifd.getLineCost().add(bifd.getBillCost()));

        bifd.setLineGrossTotal(bifd.getLineGrossRate().multiply(bifd.getQuantity()));
        bifd.setLineNetTotal(bifd.getLineNetRate().multiply(bifd.getQuantity()));

        bifd.setLineDiscount(BigDecimal.ZERO);
        bifd.setBillDiscount(BigDecimal.ZERO);

    }

// Contributed with ChatGPT assistance
    // Contributed with ChatGPT assistance
    public void addFinancialDetailsForPharmacySaleBillsFromBillItemData() {
        List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();
        List<Bill> pbis = billService.fetchBills(
                billController.getFromDate(),
                billController.getToDate(),
                null, null, null, null,
                billTypeAtomics,
                null, null
        );

        for (Bill b : pbis) {
            if (b == null) {
                continue;
            }

            BillTypeAtomic bta = Optional
                    .ofNullable(b)
                    .map(Bill::getBillTypeAtomic)
                    .orElse(null);
            if (bta == null || bta.getBillCategory() == null) {
                continue;
            }
            BillCategory bc = bta.getBillCategory();

            double saleValue = 0.0;
            double purchaseValue = 0.0;
            double costValue = 0.0;

            for (BillItem bi : b.getBillItems()) {
                addMissingDataToBillItemFinanceDetailsWhenPharmaceuticalBillItemsAreAvailableForPharmacySale(bi);
                

                PharmaceuticalBillItem pbi = bi.getPharmaceuticalBillItem();
                if (pbi == null || pbi.getItemBatch() == null) {
                    continue;
                }

                double qty = Math.abs(pbi.getQty());
                double retailRate = Math.abs(pbi.getRetailRate());
                double purchaseRate = Math.abs(pbi.getItemBatch().getPurcahseRate());
                double cRate = Math.abs(pbi.getItemBatch().getCostRate());

                if (bta == BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS) {
                    retailRate = Math.abs(bi.getNetRate());
                }

                double factor = (bc == BillCategory.CANCELLATION || bc == BillCategory.REFUND) ? -1 : 1;

                double itemSaleValue = factor * retailRate * qty;
                double itemPurchaseValue = factor * purchaseRate * qty;
                double itemCostValue = factor * cRate * qty;

                BillItemFinanceDetails bifd = bi.getBillItemFinanceDetails();
                if (bifd == null) {
                    bifd = new BillItemFinanceDetails();
                    bifd.setBillItem(bi);
                    bi.setBillItemFinanceDetails(bifd);
                }

                // Fill basic values if missing
                if (bifd.getGrossRate() == null) {
                    bifd.setGrossRate(BigDecimal.valueOf(retailRate));
                }
                if (bifd.getQuantityByUnits() == null) {
                    bifd.setQuantityByUnits(BigDecimal.valueOf(qty));
                }
                if (bifd.getValueAtCostRate() == null) {
                    bifd.setValueAtCostRate(BigDecimal.valueOf(itemCostValue));
                }
                if (bifd.getValueAtPurchaseRate() == null) {
                    bifd.setValueAtPurchaseRate(BigDecimal.valueOf(itemPurchaseValue));
                }
                if (bifd.getValueAtRetailRate() == null) {
                    bifd.setValueAtRetailRate(BigDecimal.valueOf(itemSaleValue));
                }

                // Set Line Gross Rate & Total based on config
                boolean txByCr = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Cost Rate");
                boolean txByPr = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Purchase Rate");

                double txRate;
                if (txByCr) {
                    txRate = cRate;
                } else if (txByPr) {
                    txRate = purchaseRate;
                } else {
                    txRate = retailRate;
                }

                // Assign lineGrossRate and lineGrossTotal using the chosen transfer rate
                bifd.setLineGrossRate(BigDecimal.valueOf(txRate));
                bifd.setLineGrossTotal(BigDecimal.valueOf(txRate * qty * factor));

                // Totals
                saleValue += itemSaleValue;
                purchaseValue += itemPurchaseValue;
                costValue += itemCostValue;
                billItemFacade.edit(bi);
            }

            BillFinanceDetails bfd = b.getBillFinanceDetails();
            if (bfd == null) {
                bfd = new BillFinanceDetails(b);
                b.setBillFinanceDetails(bfd);
            }

            bfd.setTotalCostValue(BigDecimal.valueOf(costValue));
            bfd.setTotalRetailSaleValue(BigDecimal.valueOf(saleValue));
            bfd.setTotalPurchaseValue(BigDecimal.valueOf(purchaseValue));

            StockBill sb = b.getStockBill();
            if (sb != null) {
                sb.setStockValueAsSaleRate(saleValue);
                sb.setStockValueAtPurchaseRates(purchaseValue);
                sb.setStockValueAsCostRate(costValue);
            }

            // Bill Gross Total based on config
            boolean txByCr = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Cost Rate");
            boolean txByPr = configOptionApplicationController.getBooleanValueByKey("Pharmacy Transfer is by Purchase Rate");

            if (txByCr) {
                bfd.setBillGrossTotal(BigDecimal.valueOf(costValue));
            } else if (txByPr) {
                bfd.setBillGrossTotal(BigDecimal.valueOf(purchaseValue));
            } else {
                bfd.setBillGrossTotal(BigDecimal.valueOf(saleValue));
            }

            getBillFacade().edit(b);
        }
    }

    public void calPharmacyIncomeAndCostReportByBill() {
        List<BillTypeAtomic> billTypeAtomics = getPharmacyIncomeBillTypes();
        List<PharmacyIncomeBillItemDTO> dtos = billService.fetchPharmacyIncomeBillItemDTOs(
                fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme);

        Map<Long, Double> saleTotals = new HashMap<>();
        Map<Long, Double> purchaseTotals = new HashMap<>();

        for (PharmacyIncomeBillItemDTO b : dtos) {
            if (b == null || b.getBillId() == null || b.getBillTypeAtomic() == null) {
                continue;
            }
            BillCategory bc = b.getBillTypeAtomic().getBillCategory();
            if (bc == null || b.getQty() == null || b.getRetailRate() == null || b.getPurchaseRate() == null) {
                continue;
            }

            double qty = Math.abs(b.getQty());
            double retail = Math.abs(b.getBillTypeAtomic() == BillTypeAtomic.PHARMACY_RETAIL_SALE_RETURN_ITEMS_AND_PAYMENTS ? b.getNetRate() : b.getRetailRate());
            double purchase = Math.abs(b.getPurchaseRate());

            double saleValue = 0.0;
            double purchaseValue = 0.0;

            switch (bc) {
                case BILL:
                case PAYMENTS:
                case PREBILL:
                    saleValue = retail * qty;
                    purchaseValue = purchase * qty;
                    break;
                case CANCELLATION:
                case REFUND:
                    saleValue = -retail * qty;
                    purchaseValue = -purchase * qty;
                    break;
                default:
                    continue;
            }

            saleTotals.merge(b.getBillId(), saleValue, Double::sum);
            purchaseTotals.merge(b.getBillId(), purchaseValue, Double::sum);
        }

        for (IncomeRow bundle : this.bundle.getRows()) {
            Bill bill = bundle.getBill();
            if (bill == null || bill.getId() == null) {
                continue;
            }

            double sale = saleTotals.getOrDefault(bill.getId(), 0.0);
            double purchase = purchaseTotals.getOrDefault(bill.getId(), 0.0);

            BillFinanceDetails bfd = bill.getBillFinanceDetails();
            if (bfd == null) {
                bfd = new BillFinanceDetails();
                bill.setBillFinanceDetails(bfd);
            }

            bfd.setTotalRetailSaleValue(BigDecimal.valueOf(sale));
            bfd.setTotalPurchaseValue(BigDecimal.valueOf(purchase));
        }

        bundle.generateRetailAndCostDetailsForPharmaceuticalBill();
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

    public StreamedContent downloadHistoricalRecordFile(HistoricalRecord hr) {
        if (hr == null) {
            return null;
        }
        String jpql = "select u from Upload u where u.retired=false and u.historicalRecord=:hr order by u.id desc";
        Map<String, Object> params = new HashMap<>();
        params.put("hr", hr);
        Upload u = uploadFacade.findFirstByJpql(jpql, params);
        if (u == null || u.getBaImage() == null) {
            return null;
        }
        return DefaultStreamedContent.builder()
                .name(u.getFileName())
                .contentType(u.getFileType())
                .stream(() -> new ByteArrayInputStream(u.getBaImage()))
                .build();
    }

    public List<HistoricalRecord> getHistoricalRecords() {
        return historicalRecords;
    }

    public void setHistoricalRecords(List<HistoricalRecord> historicalRecords) {
        this.historicalRecords = historicalRecords;
    }

    public void retireHistoricalRecord(HistoricalRecord hr) {
        if (hr == null) {
            JsfUtil.addErrorMessage("Nothing to delete");
            return;
        }
        hr.setRetired(true);
        hr.setRetiredAt(new Date());
        hr.setRetiredBy(sessionController.getLoggedUser());
        historicalRecordFacade.edit(hr);

        String jpql = "select u from Upload u where u.retired=false and u.historicalRecord=:hr";
        Map<String, Object> params = new HashMap<>();
        params.put("hr", hr);
        List<Upload> uploads = uploadFacade.findByJpql(jpql, params);
        if (uploads != null) {
            for (Upload u : uploads) {
                u.setRetired(true);
                u.setRetiredAt(new Date());
                u.setRetirer(sessionController.getLoggedUser());
                uploadFacade.edit(u);
            }
        }

        JsfUtil.addSuccessMessage("Deleted");
        viewAlreadyAvailableAllItemMovementSummaryReports();
    }

}
