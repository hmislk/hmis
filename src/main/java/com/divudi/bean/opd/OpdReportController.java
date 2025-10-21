package com.divudi.bean.opd;

// <editor-fold defaultstate="collapsed" desc="Import Statements">
import com.divudi.bean.pharmacy.*;
import com.divudi.bean.common.*;
import com.divudi.bean.cashTransaction.CashBookEntryController;
import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.DrawerEntryController;
import com.divudi.bean.channel.ChannelSearchController;
import com.divudi.bean.channel.analytics.ReportTemplateController;
import com.divudi.bean.inward.AdmissionTypeController;
import com.divudi.bean.membership.PaymentSchemeController;
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
import com.divudi.core.data.BillClassType;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.IncomeBundle;
import com.divudi.core.data.IncomeRow;
import static com.divudi.core.data.PaymentMethod.Card;
import static com.divudi.core.data.PaymentMethod.Cash;
import static com.divudi.core.data.PaymentMethod.Credit;
import static com.divudi.core.data.PaymentMethod.MultiplePaymentMethods;
import static com.divudi.core.data.PaymentMethod.OnlineSettlement;
import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.ReportTemplateRowBundle;
import com.divudi.core.data.dto.BillItemDTO;
import com.divudi.core.data.pharmacy.DailyStockBalanceReport;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.facade.DrawerFacade;
import com.divudi.core.facade.PaymentFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.BillService;
import com.divudi.service.StockHistoryService;
import com.divudi.core.data.dto.LabDailySummaryDTO;
import com.divudi.core.data.dto.OpdIncomeReportDTO;
import com.divudi.core.data.reports.CommonReports;
import com.divudi.core.data.reports.LaboratoryReport;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
public class OpdReportController implements Serializable {

    private static final long serialVersionUID = 1L;

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

// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private BillBeanController billBean;
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
    private ReportTimerController reportTimerController;
    @Inject
    AdmissionTypeController admissionTypeController;
    @Inject
    PaymentSchemeController paymentSchemeController;
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
    private ReportTemplateRowBundle bundleReport;
    private List<LabDailySummaryDTO> labDailySummaryDtos;
    private List<OpdIncomeReportDTO> opdIncomeReportDtos;

    private DailyStockBalanceReport dailyStockBalanceReport;

    private StreamedContent downloadingExcel;
    private UploadedFile file;

    // Numeric variables
    private int maxResult = 50;
    private int tabIndex;

    private double totalAdditionCashValue = 0.0;
    private double totalAdditionCardValue = 0.0;
    private double totalAdditionFundTransferValue = 0.0;
    private double totalAdditionCreditValue = 0.0;
    private double totalAdditionInwardCreditValue = 0.0;
    private double totalAdditionOtherValue = 0.0;
    private double totalAdditionTotalValue = 0.0;
    private double totalAdditionDiscountValue = 0.0;
    private double totalAdditionServiceChargeValue = 0.0;

    private double totalDeductionCashValue = 0.0;
    private double totalDeductionCardValue = 0.0;
    private double totalDeductionFundTransferValue = 0.0;
    private double totalDeductionCreditValue = 0.0;
    private double totalDeductionInwardCreditValue = 0.0;
    private double totalDeductionOtherValue = 0.0;
    private double totalDeductionTotalValue = 0.0;
    private double totalDeductionDiscountValue = 0.0;
    private double totalDeductionServiceChargeValue = 0.0;

    //transferOuts;
    //adjustments;
// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Navigators">
    public String navigateToOpdIncomeReport() {
        return "/opd/analytics/summary_reports/opd_income_report?faces-redirect=true";
    }

    public String navigateToOpdIncomeReportDto() {
        return "/opd/analytics/summary_reports/opd_income_report_dto.xhtml?faces-redirect=true";
    }

    public String navigateToOptimizedOpdIncomeReport() {
        return "/opd/analytics/summary_reports/opd_income_report_dto.xhtml?faces-redirect=true";
    }

    public String navigateToLegacyOpdIncomeReport() {
        return "/opd/analytics/summary_reports/opd_income_report.xhtml?faces-redirect=true";
    }

    public String navigateToOpdIncomeDailySummary() {
        return "/opd/analytics/summary_reports/opd_income_daily_summary?faces-redirect=true";
    }

    public String navigateToPatientIndicationsReport() {
        return "/reports/opd/patient_indications_report?faces-redirect=true";
    }

// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Functions">
    public void processDailyStockBalanceReport() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please select a department");
            return;
        }
        if (fromDate == null) {
            JsfUtil.addErrorMessage("Please select a date");
            return;
        }
        dailyStockBalanceReport = new DailyStockBalanceReport();
        dailyStockBalanceReport.setDate(fromDate);
        dailyStockBalanceReport.setDepartment(department);

        dailyStockBalanceReport.setOpeningStockValue(stockHistoryService.fetchOpeningStockQuantity(department, toDate));
        dailyStockBalanceReport.setClosingStockValue(stockHistoryService.fetchClosingStockQuantity(department, toDate));

    }

    public void listBillTypes() {
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

        // Execute the query
        List<ReportTemplateRow> rows = (List<ReportTemplateRow>) getBillFacade().findLightsByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);

        bundleReport.setReportTemplateRows(rows);
        bundleReport.calculateTotalByValues();

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

    public void processOpdIncomeReport() {
        reportTimerController.trackReportExecution(() -> {
            List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_REFUND);

            billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL);
            billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
            billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
            billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);

            List<Bill> incomeBills = billService.fetchBillsWithToInstitution(
                    fromDate,
                    toDate,
                    institution,
                    site,
                    department,
                    toInstitution,
                    toDepartment,
                    toSite,
                    webUser,
                    billTypeAtomics,
                    admissionType,
                    paymentScheme,
                    paymentMethod
            );

            bundle = new IncomeBundle(incomeBills);
            for (IncomeRow r : bundle.getRows()) {
                if (r.getBill() == null) {
                    continue;
                }
                if (r.getBill().getBillTypeAtomic() == null) {
                    continue;
                }
                Bill batchBill = null;
                switch (r.getBill().getBillTypeAtomic()) {
                    case OPD_BILL_WITH_PAYMENT:
                        batchBill = billService.fetchBatchBillOfIndividualBill(r.getBill());
                        r.setBatchBill(batchBill);
                    case OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER:
                        batchBill = billService.fetchBatchBillOfIndividualBill(r.getBill());
                        r.setBatchBill(batchBill);
                        r.setReferanceBill(r.getBill().getReferenceBill());
                    case OPD_BILL_CANCELLATION:
                    case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
                    case OPD_BILL_REFUND:

                }

                if (r.getBill().getPaymentMethod() == null) {
                    continue;
                }

                if (r.getBill().getPaymentMethod().equals(PaymentMethod.MultiplePaymentMethods)) {
                    r.setPayments(billService.fetchBillPayments(r.getBill(), r.getBatchBill()));
                }
            }
            bundle.generatePaymentDetailsForBillsAndBatchBills();
        }, CommonReports.LAB_REPORTS, "OpdReportController.processOpdIncomeReport", sessionController.getLoggedUser());
    }

    public void generateOpdIncomeReportDto() {
        processOpdIncomeReport();
        reportTimerController.trackReportExecution(() -> {
            List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_REFUND);

            billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL);
            billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
            billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
            billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);

            opdIncomeReportDtos = billService.fetchOpdIncomeReportDTOs(
                    fromDate, toDate, institution, site, department, webUser,
                    billTypeAtomics, admissionType, paymentScheme);

            System.out.println("Results returned: " + (opdIncomeReportDtos != null ? opdIncomeReportDtos.size() : 0));
            System.out.println("=================================");

            bundle = new IncomeBundle(opdIncomeReportDtos);
            bundle.generatePaymentDetailsForBillsAndBatchBills();
        }, CommonReports.LAB_REPORTS, "OpdReportController.generateOpdIncomeReportDto", sessionController.getLoggedUser());
    }

    public void processOpdIncomeSummaryByDateDTO() {
        reportTimerController.trackReportExecution(() -> {
            List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
            //Add All OPD BillTypes
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
            billTypeAtomics.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);

            //Add All Inward BillTypes
            billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL);

            //Add All Package BillTypes
            billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);

            //Add All CC BillTypes
            billTypeAtomics.add(BillTypeAtomic.CC_BILL);

            String jpql = "select new com.divudi.core.data.dto.BillItemDTO( "
                    + " bi.bill.id, "
                    + " bi.bill.billDate, "
                    + " bi.bill.discount, "
                    + " bi.bill.netTotal,"
                    + " bi.bill.paymentMethod "
                    + " ) "
                    + " from BillItem bi "
                    + " where bi.bill.retired=:ret "
                    + " and bi.bill.cancelled =:can "
                    + " and bi.refunded =:ref "
                    + " and type(bi.item) =:type"
                    + " and bi.bill.billTypeAtomic in :billTypesAtomics "
                    + " and bi.bill.createdAt between :fromDate and :toDate ";
            Map<String, Object> params = new HashMap<>();

            params.put("ret", false);
            params.put("can", false);
            params.put("ref", false);
            params.put("type", Investigation.class);
            params.put("billTypesAtomics", billTypeAtomics);
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);

            if (institution != null) {
                jpql += " and bi.bill.institution=:ins ";
                params.put("ins", institution);
            }

            if (department != null) {
                jpql += " and bi.bill.department=:dep ";
                params.put("dep", department);
            }

            if (site != null) {
                jpql += " and bi.bill.department.site=:site ";
                params.put("site", site);
            }

            if (toInstitution != null) {
                jpql += " and bi.bill.toInstitution=:toIns ";
                params.put("toIns", toInstitution);
            }

            if (toDepartment != null) {
                jpql += " and bi.bill.toDepartment=:toDep ";
                params.put("toDep", toDepartment);
            }

            if (toSite != null) {
                jpql += " and bi.bill.toDepartment.site=:toSite ";
                params.put("toSite", toSite);
            }

            if (webUser != null) {
                jpql += " and bi.bill.creater=:user ";
                params.put("user", webUser);
            }

            if (admissionType != null) {
                jpql += " and bi.bill.patientEncounter.admissionType=:admissionType ";
                params.put("admissionType", admissionType);
            }

            if (paymentScheme != null) {
                jpql += " and bi.bill.paymentScheme=:paymentScheme ";
                params.put("paymentScheme", paymentScheme);
            }

            if (paymentMethod != null) {
                jpql += " and bi.bill.paymentMethod=:paymentMethod ";
                params.put("paymentMethod", paymentMethod);
            }

            jpql += " order by bi.bill.createdAt desc";

            List<BillItemDTO> tempBillItems = (List<BillItemDTO>) billItemFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

            List<BillItemDTO> uniqueBills = tempBillItems.stream()
                    .collect(Collectors.toMap(
                            BillItemDTO::getId,
                            dto -> dto,
                            (existing, replacement) -> existing // keep the first occurrence
                    ))
                    .values()
                    .stream()
                    .collect(Collectors.toList());

            bundle = generateDailyIncomeSummary(uniqueBills);

            populateSummaryRow();
        }, LaboratoryReport.LABORATORY_SUMMARY, "Laboratory Summary - DTO", sessionController.getLoggedUser());
    }

    public IncomeBundle generateDailyIncomeSummary(List<BillItemDTO> uniqueBills) {
        IncomeBundle bundle = new IncomeBundle();

        // Group bills by day
        Map<Date, List<BillItemDTO>> billsByDay = uniqueBills.stream()
                .collect(Collectors.groupingBy(
                        bi -> bi.getBillCreatedAt()
                ));

        // Process each day's bills
        for (Map.Entry<Date, List<BillItemDTO>> entry : billsByDay.entrySet()) {
            Date day = entry.getKey();
            List<BillItemDTO> dailyBills = entry.getValue();

            IncomeRow dayRow = new IncomeRow();
            dayRow.setDate(day);  // Set the date for the row

            // Initialize daily totals
            double dailyCash = 0;
            double dailyCard = 0;
            double dailyCredit = 0;
            double dailyDiscount = 0;
            double dailyNetTotal = 0;

            // Calculate totals for each payment method
            for (BillItemDTO bi : dailyBills) {
                if (bi.getPaymentMethod() == null) {
                    dailyCredit += bi.getBillNetTotal();
                } else {
                    switch (bi.getPaymentMethod()) {
                        case Card:
                            dailyCard += bi.getBillNetTotal();
                            break;
                        case Cash:
                            dailyCash += bi.getBillNetTotal();
                            break;
                        case Credit:
                            dailyCredit += bi.getBillNetTotal();
                            break;
                        case MultiplePaymentMethods:
                            Bill bill = billFacade.find(bi.getId());
                            Bill batchBill = bill.getBackwardReferenceBill();
                            analyzeMultiplePayments(dayRow, batchBill, dailyCard, dailyCash, dailyCredit);
                            break;
                    }
                }

                // Sum discounts and net totals
                dailyDiscount += bi.getDiscount() != null ? bi.getDiscount() : 0;
                dailyNetTotal += bi.getBillNetTotal() != null ? bi.getBillNetTotal() : 0;
            }

            // Set the calculated values
            dayRow.setCashValue(dailyCash);
            dayRow.setCardValue(dailyCard);
            dayRow.setCreditValue(dailyCredit);
            dayRow.setDiscount(dailyDiscount);
            dayRow.setNetTotal(dailyNetTotal);

            bundle.getRows().add(dayRow);
        }
        // Sort rows by date
        bundle.getRows().sort(Comparator.comparing(IncomeRow::getDate));

        return bundle;
    }

    public void populateSummaryRow() {
        // Initialize all sums to zero
        double sumOfCashValues = 0.0;
        double sumOfCardValues = 0.0;
        double sumOfCreditValues = 0.0;

        double sumOfDiscount = 0.0;
        double sumOfNetTotal = 0.0;

        // Aggregate all rows
        for (IncomeRow r : bundle.getRows()) {
            sumOfCashValues += r.getCashValue();
            sumOfCardValues += r.getCardValue();
            sumOfCreditValues += r.getCreditValue();
            sumOfDiscount += r.getDiscount();
            sumOfNetTotal += r.getNetTotal();
        }

        IncomeRow summaryRow = new IncomeRow();
        // Set summary row values
        summaryRow.setCashValue(sumOfCashValues);
        summaryRow.setCardValue(sumOfCardValues);
        summaryRow.setCreditValue(sumOfCreditValues);

        summaryRow.setDiscount(sumOfDiscount);
        summaryRow.setNetTotal(sumOfNetTotal);

        bundle.setSummaryRow(summaryRow);
    }

    public void analyzeMultiplePayments(IncomeRow row, Bill batchBill, double dailyCard, double dailyCash, double dailyCredit) {
        List<Payment> billPaymnets = billSearch.fetchBillPayments(batchBill);

        for (Payment p : billPaymnets) {
            if (p.getPaymentMethod() == null) {

            } else {
                switch (p.getPaymentMethod()) {
                    case Card:
                        dailyCard += p.getPaidValue();
                        break;
                    case Cash:
                        dailyCash += p.getPaidValue();
                        break;
                    case Credit:
                        dailyCredit += p.getPaidValue();
                        break;
                }
            }
        }

    }

    public List<BillTypeAtomic> getOpdAndPackageBillTypeAtomics() {
        List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_REFUND);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);

        return billTypeAtomics;
    }

    public List<BillTypeAtomic> getOutPatientBillTypeAtomics() {
        List<BillTypeAtomic> ccBillTypeAtomics = new ArrayList<>();
        ccBillTypeAtomics.add(BillTypeAtomic.CC_BILL);
        ccBillTypeAtomics.add(BillTypeAtomic.CC_BILL_CANCELLATION);
        ccBillTypeAtomics.add(BillTypeAtomic.CC_BILL_REFUND);

        return ccBillTypeAtomics;
    }

    public List<BillTypeAtomic> getInwardBillTypeAtomics() {
        List<BillTypeAtomic> inwardBillTypeAtomics = new ArrayList<>();
        inwardBillTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL);
        inwardBillTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
        inwardBillTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        inwardBillTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);

        return inwardBillTypeAtomics;
    }

    public void generateDailyLabSummaryByDepartment() {

        bundleReport = new ReportTemplateRowBundle();

        //create Normal,Membership,Staff Incomes
        List<Bill> fetchedBills = billService.fetchBills(fromDate, toDate, institution, site, department, null, getOpdAndPackageBillTypeAtomics(), null, null, null, null, null);
        List<Bill> normalIncomeList = new ArrayList<>();
        List<Bill> paymentSchemeIncomeList = new ArrayList<>();
        ReportTemplateRow normalIncomeRow = new ReportTemplateRow();
        normalIncomeRow.setItemName("Normal Income");
        initializeRows(normalIncomeRow);

        for (Bill b : fetchedBills) {
            if (b.getPaymentScheme() != null) {
                paymentSchemeIncomeList.add(b);
            } else {
                normalIncomeList.add(b);
            }
        }

        normalIncomeRow = genarateRowBundle(normalIncomeList, normalIncomeRow);
        bundleReport.getReportTemplateRows().add(normalIncomeRow);

        List<PaymentScheme> pss = paymentSchemeController.getPaymentSchemesForOPD();
        Map<PaymentScheme, List<Bill>> billsByPaymentScheme = paymentSchemeIncomeList.stream().collect(Collectors.groupingBy(b -> b.getPaymentScheme()));

        for (PaymentScheme ps : pss) {
            ReportTemplateRow paymentSchemeIncomeRow = new ReportTemplateRow();
            paymentSchemeIncomeRow.setItemName(ps.getName() + " Income");
            initializeRows(paymentSchemeIncomeRow);
            paymentSchemeIncomeRow = genarateRowBundle(billsByPaymentScheme.getOrDefault(ps, new ArrayList<>()), paymentSchemeIncomeRow);
            bundleReport.getReportTemplateRows().add(paymentSchemeIncomeRow);
        }

        //Create Inward rows
        fetchedBills = billService.fetchBills(fromDate, toDate, institution, site, department, null, getInwardBillTypeAtomics(), null, null, null, null, null);
        List<AdmissionType> ats = admissionTypeController.getItems();
        Map<AdmissionType, List<Bill>> billsByAdmissionType = fetchedBills.stream().collect(Collectors.groupingBy(b -> b.getPatientEncounter().getAdmissionType()));

        for (AdmissionType at : ats) {
            ReportTemplateRow inwardIncomeRow = new ReportTemplateRow();
            inwardIncomeRow.setItemName(at + " Income");
            initializeRows(inwardIncomeRow);
            inwardIncomeRow = genarateRowBundleInward(billsByAdmissionType.getOrDefault(at, new ArrayList<>()), inwardIncomeRow);
            bundleReport.getReportTemplateRows().add(inwardIncomeRow);
        }

        //outpatientIncome (CC)
        fetchedBills = billService.fetchBills(fromDate, toDate, institution, site, department, null, getOutPatientBillTypeAtomics(), null, null, null, null, null);
        ReportTemplateRow outPatientIncomeRow = new ReportTemplateRow();
        outPatientIncomeRow.setItemName("Outpatient Income");
        initializeRows(outPatientIncomeRow);
        outPatientIncomeRow = genarateRowBundle(fetchedBills, outPatientIncomeRow);
        bundleReport.getReportTemplateRows().add(outPatientIncomeRow);

        //Other
        List<BillTypeAtomic> otherbillTypeAtomics = new ArrayList<>();
        otherbillTypeAtomics.add(BillTypeAtomic.FUND_TRANSFER_RECEIVED_BILL);
        otherbillTypeAtomics.add(BillTypeAtomic.FUND_TRANSFER_RECEIVED_BILL_CANCELLED);

        fetchedBills = billService.fetchBills(fromDate, toDate, institution, site, department, null, otherbillTypeAtomics, null, null, null, null, null);
        ReportTemplateRow floatReceiveIncomeRow = new ReportTemplateRow();
        floatReceiveIncomeRow.setItemName("Float Receive");
        initializeRows(floatReceiveIncomeRow);
        floatReceiveIncomeRow = genarateRowBundleOther(fetchedBills, floatReceiveIncomeRow);
        bundleReport.getReportTemplateRows().add(floatReceiveIncomeRow);

        ReportTemplateRow otherIncomeRow = new ReportTemplateRow();
        otherIncomeRow.setItemName("Other Income");
        initializeRows(otherIncomeRow);
        bundleReport.getReportTemplateRows().add(otherIncomeRow);

        //calculate Addition Totals
        calculateTotalsFromRows(bundleReport.getReportTemplateRows(), true);

        //Deductions
        bundle = new IncomeBundle();

        //Deducations Voucher
        List<BillTypeAtomic> voucherDeductionBillTypeAtomics = new ArrayList<>();
        voucherDeductionBillTypeAtomics.add(BillTypeAtomic.FUND_TRANSFER_BILL);
        voucherDeductionBillTypeAtomics.add(BillTypeAtomic.FUND_TRANSFER_BILL_CANCELLED);

        fetchedBills = billService.fetchBills(fromDate, toDate, institution, site, department, null, voucherDeductionBillTypeAtomics, null, null, null, null, null);
        IncomeRow floatTransferDeductionRow = new IncomeRow();
        floatTransferDeductionRow.setItemName("Float Transfer");
        initializeDeductionRows(floatTransferDeductionRow);
        floatTransferDeductionRow = genarateDeductionRowBundleOther(fetchedBills, floatTransferDeductionRow);
        bundle.getRows().add(floatTransferDeductionRow);

        //Deducations Voucher
        IncomeRow voucherDeductionRow = new IncomeRow();
        voucherDeductionRow.setItemName("Voucher");
        initializeDeductionRows(voucherDeductionRow);
        //otherDeductionRow = genarateDeductionRowBundleOther(fetchedBills,otherDeductionRow);
        bundle.getRows().add(voucherDeductionRow);

        //Deducations Other
        IncomeRow otherDeductionRow = new IncomeRow();
        otherDeductionRow.setItemName("Other");
        initializeDeductionRows(otherDeductionRow);
        //otherDeductionRow = genarateDeductionRowBundleOther(fetchedBills,otherDeductionRow);
        bundle.getRows().add(otherDeductionRow);

        //calculate Deducations Totals
        calculateTotalsFromRows(bundle.getRows(), false);

    }

    public void generateDailyLabSummaryByDepartmentDto() {
        if (!configOptionApplicationController.getBooleanValueByKey("Lab Daily Summary Report - Optimized Method")) {
            generateDailyLabSummaryByDepartment();
            return;
        }

        labDailySummaryDtos = billService.fetchLabDailySummaryDtos(fromDate, toDate, institution, site, department);
        bundle = new IncomeBundle();

        totalAdditionCashValue = 0;
        totalAdditionCardValue = 0;
        totalAdditionFundTransferValue = 0;
        totalAdditionCreditValue = 0;
        totalAdditionInwardCreditValue = 0;
        totalAdditionOtherValue = 0;
        totalAdditionTotalValue = 0;
        totalAdditionDiscountValue = 0;
        totalAdditionServiceChargeValue = 0;

        if (labDailySummaryDtos != null) {
            for (LabDailySummaryDTO dto : labDailySummaryDtos) {
                totalAdditionCashValue += dto.getCashValue() != null ? dto.getCashValue() : 0;
                totalAdditionCardValue += dto.getCardValue() != null ? dto.getCardValue() : 0;
                totalAdditionFundTransferValue += dto.getOnlineSettlementValue() != null ? dto.getOnlineSettlementValue() : 0;
                totalAdditionCreditValue += dto.getCreditValue() != null ? dto.getCreditValue() : 0;
                totalAdditionInwardCreditValue += dto.getInwardCreditValue() != null ? dto.getInwardCreditValue() : 0;
                totalAdditionOtherValue += dto.getOtherValue() != null ? dto.getOtherValue() : 0;
                totalAdditionTotalValue += dto.getTotal() != null ? dto.getTotal() : 0;
                totalAdditionDiscountValue += dto.getDiscount() != null ? dto.getDiscount() : 0;
                totalAdditionServiceChargeValue += dto.getServiceCharge() != null ? dto.getServiceCharge() : 0;
            }
        }

        // Initialize deduction totals
        totalDeductionCashValue = 0;
        totalDeductionCardValue = 0;
        totalDeductionFundTransferValue = 0;
        totalDeductionCreditValue = 0;
        totalDeductionInwardCreditValue = 0;
        totalDeductionOtherValue = 0;
        totalDeductionTotalValue = 0;
        totalDeductionDiscountValue = 0;
        totalDeductionServiceChargeValue = 0;

        // Add deduction logic similar to legacy method
        List<BillTypeAtomic> voucherDeductionBillTypeAtomics = new ArrayList<>();
        voucherDeductionBillTypeAtomics.add(BillTypeAtomic.FUND_TRANSFER_BILL);
        voucherDeductionBillTypeAtomics.add(BillTypeAtomic.FUND_TRANSFER_BILL_CANCELLED);

        List<Bill> fetchedBills = billService.fetchBills(fromDate, toDate, institution, site, department, null, voucherDeductionBillTypeAtomics, null, null, null, null, null);
        IncomeRow floatTransferDeductionRow = new IncomeRow();
        floatTransferDeductionRow.setItemName("Float Transfer");
        initializeDeductionRows(floatTransferDeductionRow);
        floatTransferDeductionRow = genarateDeductionRowBundleOther(fetchedBills, floatTransferDeductionRow);
        bundle.getRows().add(floatTransferDeductionRow);

        // Deductions Voucher
        IncomeRow voucherDeductionRow = new IncomeRow();
        voucherDeductionRow.setItemName("Voucher");
        initializeDeductionRows(voucherDeductionRow);
        bundle.getRows().add(voucherDeductionRow);

        // Deductions Other
        IncomeRow otherDeductionRow = new IncomeRow();
        otherDeductionRow.setItemName("Other");
        initializeDeductionRows(otherDeductionRow);
        bundle.getRows().add(otherDeductionRow);

        // Calculate deduction totals
        calculateTotalsFromRows(bundle.getRows(), false);
    }

    public ReportTemplateRow genarateRowBundle(List<Bill> bills, ReportTemplateRow row) {
        List<BillItem> billItems = new ArrayList<>();
        Set<Bill> processedMultipleBills = new HashSet<>();
        for (Bill b : bills) {
            billItems.addAll(billBean.fillBillItems(b));
        }
        for (BillItem bi : billItems) {
            if (!(bi.getItem() instanceof Investigation)) {
                continue;
            }
            if (null == bi.getBill().getPaymentMethod()) {
                continue;
            } else {
                switch (bi.getBill().getPaymentMethod()) {
                    case Cash:
                        row.setCashValue(row.getCashValue() + bi.getNetValue());
                        row.setTotal(row.getTotal() + bi.getNetValue());
                        row.setDiscount(row.getDiscount() + bi.getDiscount());
                        row.setServiceCharge(row.getServiceCharge() + bi.getMarginValue());
                        break;
                    case Card:
                        row.setCardValue(row.getCardValue() + bi.getNetValue());
                        row.setTotal(row.getTotal() + bi.getNetValue());
                        row.setDiscount(row.getDiscount() + bi.getDiscount());
                        row.setServiceCharge(row.getServiceCharge() + bi.getMarginValue());
                        break;
                    case Credit:
                        row.setCreditValue(row.getCreditValue() + bi.getNetValue());
                        row.setTotal(row.getTotal() + bi.getNetValue());
                        row.setDiscount(row.getDiscount() + bi.getDiscount());
                        row.setServiceCharge(row.getServiceCharge() + bi.getMarginValue());
                        break;
                    case OnlineSettlement:
                        row.setLong1(row.getLong1() + Math.round(bi.getNetValue()));
                        row.setTotal(row.getTotal() + bi.getNetValue());
                        row.setDiscount(row.getDiscount() + bi.getDiscount());
                        row.setServiceCharge(row.getServiceCharge() + bi.getMarginValue());
                        break;
                    case MultiplePaymentMethods:
                        if (!processedMultipleBills.contains(bi.getBill())) {
                            processMultiplePaymentBill(bi.getBill(), row);
                            processedMultipleBills.add(bi.getBill());
                        }
                        break;
                    default:
                        row.setLong3(row.getLong3() + Math.round(bi.getNetValue()));
                        row.setTotal(row.getTotal() + bi.getNetValue());
                        row.setDiscount(row.getDiscount() + bi.getDiscount());
                        row.setServiceCharge(row.getServiceCharge() + bi.getMarginValue());
                        break;
                }
            }
        }
        return row;
    }

    public ReportTemplateRow genarateRowBundleInward(List<Bill> bills, ReportTemplateRow row) {
        List<BillItem> billItems = new ArrayList<>();
        for (Bill b : bills) {
            billItems.addAll(billBean.fillBillItems(b));
        }
        for (BillItem bi : billItems) {
            if (!(bi.getItem() instanceof Investigation)) {
                continue;
            }
            row.setLong2(row.getLong2() + Math.round(bi.getNetValue()));
            row.setTotal(row.getTotal() + bi.getNetValue());
            row.setDiscount(row.getDiscount() + bi.getDiscount());
            row.setServiceCharge(row.getServiceCharge() + bi.getMarginValue());
        }
        return row;
    }

    public ReportTemplateRow genarateRowBundleOther(List<Bill> bills, ReportTemplateRow row) {
        for (Bill b : bills) {
            List<Payment> payments = new ArrayList<>();
            payments = billService.fetchBillPayments(b);
            for (Payment p : payments) {
                if (null == p.getPaymentMethod()) {
                    continue;
                } else {
                    switch (p.getPaymentMethod()) {
                        case Cash:
                            row.setCashValue(row.getCashValue() + p.getPaidValue());
                            row.setTotal(row.getTotal() + p.getPaidValue());
                            break;
                        case Card:
                            row.setCardValue(row.getCardValue() + p.getPaidValue());
                            row.setTotal(row.getTotal() + p.getPaidValue());
                            break;
                        case Credit:
                            row.setCreditValue(row.getCreditValue() + p.getPaidValue());
                            row.setTotal(row.getTotal() + p.getPaidValue());
                            break;
                        case OnlineSettlement:
                            row.setLong1(row.getLong1() + Math.round(p.getPaidValue()));
                            row.setTotal(row.getTotal() + p.getPaidValue());
                            break;
                        default:
                            row.setLong3(row.getLong3() + Math.round(p.getPaidValue()));
                            row.setTotal(row.getTotal() + p.getPaidValue());
                            break;
                    }
                }
            }
        }
        return row;
    }

    public IncomeRow genarateDeductionRowBundleOther(List<Bill> bills, IncomeRow row) {
        for (Bill b : bills) {
            List<Payment> payments = new ArrayList<>();
            payments = billService.fetchBillPayments(b);
            for (Payment p : payments) {
                if (null == p.getPaymentMethod()) {
                    continue;
                } else {
                    switch (p.getPaymentMethod()) {
                        case Cash:
                            row.setCashValue(row.getCashValue() + p.getPaidValue());
                            row.setNetTotal(row.getNetTotal() + p.getPaidValue());
                            break;
                        case Card:
                            row.setCardValue(row.getCardValue() + p.getPaidValue());
                            row.setNetTotal(row.getNetTotal() + p.getPaidValue());
                            break;
                        case Credit:
                            row.setCreditValue(row.getCreditValue() + p.getPaidValue());
                            row.setNetTotal(row.getNetTotal() + p.getPaidValue());
                            break;
                        case OnlineSettlement:
                            row.setLong1(row.getLong1() + Math.round(p.getPaidValue()));
                            row.setNetTotal(row.getNetTotal() + p.getPaidValue());
                            break;
                        default:
                            row.setLong3(row.getLong3() + Math.round(p.getPaidValue()));
                            row.setNetTotal(row.getNetTotal() + p.getPaidValue());
                            break;
                    }
                }
            }
        }
        return row;
    }

    private void processMultiplePaymentBill(Bill bill, ReportTemplateRow row) {
        List<Payment> payments = new ArrayList<>();
        if (getOpdAndPackageBillTypeAtomics().contains(bill.getBillTypeAtomic())) {
            bill = bill.getBackwardReferenceBill();
        }
        payments = billService.fetchBillPayments(bill);

        if (!payments.isEmpty()) {
            for (Payment p : payments) {
                if (null == p.getPaymentMethod()) {
                    continue;
                } else {
                    switch (p.getPaymentMethod()) {
                        case Cash:
                            row.setCashValue(row.getCashValue() + p.getPaidValue());
                            row.setTotal(row.getTotal() + p.getPaidValue());
                            break;
                        case Card:
                            row.setCardValue(row.getCardValue() + p.getPaidValue());
                            row.setTotal(row.getTotal() + p.getPaidValue());
                            break;
                        case Credit:
                            row.setCreditValue(row.getCreditValue() + p.getPaidValue());
                            row.setTotal(row.getTotal() + p.getPaidValue());
                            break;
                        case OnlineSettlement:
                            row.setLong1(row.getLong1() + Math.round(p.getPaidValue()));
                            row.setTotal(row.getTotal() + p.getPaidValue());
                            break;
                        default:
                            row.setLong3(row.getLong3() + Math.round(p.getPaidValue()));
                            row.setTotal(row.getTotal() + p.getPaidValue());
                            break;
                    }
                }
            }
        }
    }

    private void processMultiplePaymentBill(Bill bill, IncomeRow row) {
        List<Payment> payments = new ArrayList<>();
        if (getOpdAndPackageBillTypeAtomics().contains(bill.getBillTypeAtomic())) {
            bill = bill.getBackwardReferenceBill();
        }
        payments = billService.fetchBillPayments(bill);

        if (!payments.isEmpty()) {
            for (Payment p : payments) {
                if (null == p.getPaymentMethod()) {
                    continue;
                } else {
                    switch (p.getPaymentMethod()) {
                        case Cash:
                            row.setCashValue(row.getCashValue() + p.getPaidValue());
                            row.setNetTotal(row.getNetTotal() + p.getPaidValue());
                            break;
                        case Card:
                            row.setCardValue(row.getCardValue() + p.getPaidValue());
                            row.setNetTotal(row.getNetTotal() + p.getPaidValue());
                            break;
                        case Credit:
                            row.setCreditValue(row.getCreditValue() + p.getPaidValue());
                            row.setNetTotal(row.getNetTotal() + p.getPaidValue());
                            break;
                        case OnlineSettlement:
                            row.setLong1(row.getLong1() + Math.round(p.getPaidValue()));
                            row.setNetTotal(row.getNetTotal() + p.getPaidValue());
                            break;
                        default:
                            row.setLong3(row.getLong3() + Math.round(p.getPaidValue()));
                            row.setNetTotal(row.getNetTotal() + p.getPaidValue());
                            break;
                    }
                }
            }
        }
    }

    public void initializeRows(ReportTemplateRow row) {
        row.setCashValue(0.0);
        row.setCardValue(0.0);
        row.setLong1(0L);
        row.setCreditValue(0.0);
        row.setLong2(0L);
        row.setLong3(0L);
        row.setTotal(0.0);
        row.setDiscount(0.0);
        row.setServiceCharge(0.0);
    }

    public void initializeDeductionRows(IncomeRow row) {
        row.setCashValue(0.0);
        row.setCardValue(0.0);
        row.setLong1(0L);
        row.setCreditValue(0.0);
        row.setLong2(0L);
        row.setLong3(0L);
        row.setNetTotal(0.0);
        row.setDiscount(0.0);
        row.setServiceCharge(0.0);
    }

    private void calculateTotalsFromRows(List<? extends Object> rows, boolean isAddition) {
        double cashValue = 0.0;
        double cardValue = 0.0;
        double fundTransferValue = 0.0;
        double creditValue = 0.0;
        double inwardCreditValue = 0.0;
        double otherValue = 0.0;
        double totalValue = 0.0;
        double discountValue = 0.0;
        double serviceChargeValue = 0.0;

        for (Object row : rows) {
            if (row instanceof ReportTemplateRow) {
                ReportTemplateRow rtr = (ReportTemplateRow) row;
                cashValue += rtr.getCashValue();
                cardValue += rtr.getCardValue();
                fundTransferValue += rtr.getLong1() != null ? rtr.getLong1().doubleValue() : 0.0;
                creditValue += rtr.getCreditValue();
                inwardCreditValue += rtr.getLong2() != null ? rtr.getLong2().doubleValue() : 0.0;
                otherValue += rtr.getLong3() != null ? rtr.getLong3().doubleValue() : 0.0;
                totalValue += rtr.getTotal();
                discountValue += rtr.getDiscount();
                serviceChargeValue += rtr.getServiceCharge();
            } else if (row instanceof IncomeRow) {
                IncomeRow ir = (IncomeRow) row;
                cashValue += ir.getCashValue();
                cardValue += ir.getCardValue();
                fundTransferValue += ir.getLong1() != null ? ir.getLong1().doubleValue() : 0.0;
                creditValue += ir.getCreditValue();
                inwardCreditValue += ir.getLong2() != null ? ir.getLong2().doubleValue() : 0.0;
                otherValue += ir.getLong3() != null ? ir.getLong3().doubleValue() : 0.0;
                totalValue += ir.getNetTotal();
                discountValue += ir.getDiscount();
                serviceChargeValue += ir.getServiceCharge();
            }
        }

        if (isAddition) {
            totalAdditionCashValue = cashValue;
            totalAdditionCardValue = cardValue;
            totalAdditionFundTransferValue = fundTransferValue;
            totalAdditionCreditValue = creditValue;
            totalAdditionInwardCreditValue = inwardCreditValue;
            totalAdditionOtherValue = otherValue;
            totalAdditionTotalValue = totalValue;
            totalAdditionDiscountValue = discountValue;
            totalAdditionServiceChargeValue = serviceChargeValue;
        } else {
            totalDeductionCashValue = cashValue;
            totalDeductionCardValue = cardValue;
            totalDeductionFundTransferValue = fundTransferValue;
            totalDeductionCreditValue = creditValue;
            totalDeductionInwardCreditValue = inwardCreditValue;
            totalDeductionOtherValue = otherValue;
            totalDeductionTotalValue = totalValue;
            totalDeductionDiscountValue = discountValue;
            totalDeductionServiceChargeValue = serviceChargeValue;
        }
    }

// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Constructors">
    public OpdReportController() {
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

    public int getTabIndex() {
        return tabIndex;
    }

    public void setTabIndex(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    public double getTotalAdditionCashValue() {
        return totalAdditionCashValue;
    }

    public void setTotalAdditionCashValue(double totalAdditionCashValue) {
        this.totalAdditionCashValue = totalAdditionCashValue;
    }

    public double getTotalAdditionCardValue() {
        return totalAdditionCardValue;
    }

    public void setTotalAdditionCardValue(double totalAdditionCardValue) {
        this.totalAdditionCardValue = totalAdditionCardValue;
    }

    public double getTotalAdditionFundTransferValue() {
        return totalAdditionFundTransferValue;
    }

    public void setTotalAdditionFundTransferValue(double totalAdditionFundTransferValue) {
        this.totalAdditionFundTransferValue = totalAdditionFundTransferValue;
    }

    public double getTotalAdditionCreditValue() {
        return totalAdditionCreditValue;
    }

    public void setTotalAdditionCreditValue(double totalAdditionCreditValue) {
        this.totalAdditionCreditValue = totalAdditionCreditValue;
    }

    public double getTotalAdditionInwardCreditValue() {
        return totalAdditionInwardCreditValue;
    }

    public void setTotalAdditionInwardCreditValue(double totalAdditionInwardCreditValue) {
        this.totalAdditionInwardCreditValue = totalAdditionInwardCreditValue;
    }

    public double getTotalAdditionOtherValue() {
        return totalAdditionOtherValue;
    }

    public void setTotalAdditionOtherValue(double totalAdditionOtherValue) {
        this.totalAdditionOtherValue = totalAdditionOtherValue;
    }

    public double getTotalAdditionTotalValue() {
        return totalAdditionTotalValue;
    }

    public void setTotalAdditionTotalValue(double totalAdditionTotalValue) {
        this.totalAdditionTotalValue = totalAdditionTotalValue;
    }

    public double getTotalAdditionDiscountValue() {
        return totalAdditionDiscountValue;
    }

    public void setTotalAdditionDiscountValue(double totalAdditionDiscountValue) {
        this.totalAdditionDiscountValue = totalAdditionDiscountValue;
    }

    public double getTotalAdditionServiceChargeValue() {
        return totalAdditionServiceChargeValue;
    }

    public void setTotalAdditionServiceChargeValue(double totalAdditionServiceChargeValue) {
        this.totalAdditionServiceChargeValue = totalAdditionServiceChargeValue;
    }

    public double getTotalDeductionCashValue() {
        return totalDeductionCashValue;
    }

    public void setTotalDeductionCashValue(double totalDeductionCashValue) {
        this.totalDeductionCashValue = totalDeductionCashValue;
    }

    public double getTotalDeductionCardValue() {
        return totalDeductionCardValue;
    }

    public void setTotalDeductionCardValue(double totalDeductionCardValue) {
        this.totalDeductionCardValue = totalDeductionCardValue;
    }

    public double getTotalDeductionFundTransferValue() {
        return totalDeductionFundTransferValue;
    }

    public void setTotalDeductionFundTransferValue(double totalDeductionFundTransferValue) {
        this.totalDeductionFundTransferValue = totalDeductionFundTransferValue;
    }

    public double getTotalDeductionCreditValue() {
        return totalDeductionCreditValue;
    }

    public void setTotalDeductionCreditValue(double totalDeductionCreditValue) {
        this.totalDeductionCreditValue = totalDeductionCreditValue;
    }

    public double getTotalDeductionInwardCreditValue() {
        return totalDeductionInwardCreditValue;
    }

    public void setTotalDeductionInwardCreditValue(double totalDeductionInwardCreditValue) {
        this.totalDeductionInwardCreditValue = totalDeductionInwardCreditValue;
    }

    public double getTotalDeductionOtherValue() {
        return totalDeductionOtherValue;
    }

    public void setTotalDeductionOtherValue(double totalDeductionOtherValue) {
        this.totalDeductionOtherValue = totalDeductionOtherValue;
    }

    public double getTotalDeductionTotalValue() {
        return totalDeductionTotalValue;
    }

    public void setTotalDeductionTotalValue(double totalDeductionTotalValue) {
        this.totalDeductionTotalValue = totalDeductionTotalValue;
    }

    public double getTotalDeductionDiscountValue() {
        return totalDeductionDiscountValue;
    }

    public void setTotalDeductionDiscountValue(double totalDeductionDiscountValue) {
        this.totalDeductionDiscountValue = totalDeductionDiscountValue;
    }

    public double getTotalDeductionServiceChargeValue() {
        return totalDeductionServiceChargeValue;
    }

    public void setTotalDeductionServiceChargeValue(double totalDeductionServiceChargeValue) {
        this.totalDeductionServiceChargeValue = totalDeductionServiceChargeValue;
    }

    public List<LabDailySummaryDTO> getLabDailySummaryDtos() {
        return labDailySummaryDtos;
    }

    public void setLabDailySummaryDtos(List<LabDailySummaryDTO> labDailySummaryDtos) {
        this.labDailySummaryDtos = labDailySummaryDtos;
    }

    public List<OpdIncomeReportDTO> getOpdIncomeReportDtos() {
        return opdIncomeReportDtos;
    }

    public void setOpdIncomeReportDtos(List<OpdIncomeReportDTO> opdIncomeReportDtos) {
        this.opdIncomeReportDtos = opdIncomeReportDtos;
    }

}
