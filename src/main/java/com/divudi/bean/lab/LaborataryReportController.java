package com.divudi.bean.lab;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.BillSearch;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.BillClassType;
import com.divudi.core.data.BillType;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.IncomeBundle;
import com.divudi.core.data.IncomeRow;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.ReportTemplateRowBundle;
import com.divudi.core.data.TestWiseCountReport;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.data.hr.ReportKeyWord;
import com.divudi.core.data.pharmacy.DailyStockBalanceReport;
import com.divudi.core.data.dto.TestCountDTO;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Category;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PaymentScheme;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.data.dto.LabIncomeReportDTO;
import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.ReportTimerController;
import com.divudi.bean.inward.AdmissionTypeController;
import com.divudi.bean.membership.PaymentSchemeController;
import com.divudi.core.data.FeeType;
import static com.divudi.core.data.PaymentMethod.Card;
import static com.divudi.core.data.PaymentMethod.Cash;
import static com.divudi.core.data.PaymentMethod.Credit;
import static com.divudi.core.data.PaymentMethod.MultiplePaymentMethods;
import static com.divudi.core.data.PaymentMethod.OnlineSettlement;
import com.divudi.core.data.ReportTemplateRow;
import com.divudi.core.data.dto.BillItemDTO;
import com.divudi.core.data.dto.PatientInvestigationDTO;
import com.divudi.core.data.reports.LaboratoryReport;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.facade.InvestigationFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PatientInvestigationFacade;
import com.divudi.core.light.common.BillLight;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.BillService;
import com.divudi.service.PaymentService;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author H.K. Damith Deshan
 */
@Named
@SessionScoped
public class LaborataryReportController implements Serializable {

    public LaborataryReportController() {
    }

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    SessionController sessionController;
    @Inject
    private BillSearch billSearch;
    @Inject
    ConfigOptionApplicationController configController;
    @Inject
    ReportTimerController reportTimerController;

    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private BillService billService;
    @EJB
    BillItemFacade billItemFacade;
    @EJB
    PaymentService paymentService;
    @EJB
    PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    PatientFacade patientFacade;
    @EJB
    InvestigationFacade investigationFacade;
    @EJB
    BillFeeFacade billFeeFacade;

    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Class Variables">
    // Basic types
    private String visitType;
    private String methodType;
    private String searchType;
    private String reportType;

    private Long rowsPerPageForScreen;
    private Long rowsPerPageForPrinting;
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

    private DailyStockBalanceReport dailyStockBalanceReport;

    private StreamedContent downloadingExcel;
    private UploadedFile file;
    private List<LabIncomeReportDTO> labIncomeReportDtos;

    // Numeric variables
    private int maxResult = 50;

    private String viewTemplate;

    private double totalHospitalFee;
    private double totalReagentFee;
    private double totalOtherFee;
    private double totalNetTotal;
    private double totalDiscount;
    private double totalServiceCharge;

    private int activeIndex;
    private String strActiveIndex = "0";

    private List<TestWiseCountReport> testWiseCounts;
    private List<TestCountDTO> testWiseCountDtos;
    private double totalNetHosFee;
    private double totalAdditionalFee;
    private double totalCCFee;
    private double totalProFee;
    private double totalHosFee;
    private double totalCount;

    private List<Payment> payments;

    private Investigation investigation;
    private List<PatientInvestigationDTO> itemList;

    //for 9B
    private double totalAdditionCashValue;
    private double totalAdditionCardValue;
    private double totalAdditionOnlineSettlementValue;
    private double totalAdditionCreditValue;
    private double totalAdditionInwardCreditValue;
    private double totalAdditionOtherValue;
    private double totalAdditionTotalValue;
    private double totalAdditionDiscountValue;
    private double totalAdditionServiceChargeValue;

    private double totalDeductionCashValue;
    private double totalDeductionCardValue;
    private double totalDeductionOnlineSettlementValue;
    private double totalDeductionCreditValue;
    private double totalDeductionInwardCreditValue;
    private double totalDeductionOtherValue;
    private double totalDeductionTotalValue;
    private double totalDeductionDiscountValue;
    private double totalDeductionServiceChargeValue;

// </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Navigators">
    public String navigateToLaborataryInwardOrderReportFromLabAnalytics() {
        resetAllFiltersExceptDateRange();
        return "/reportLab/lab_inward_order_report?faces-redirect=true";
    }

    public String navigateToLaborataryIncomeReportFromLabAnalytics() {
        resetAllFiltersExceptDateRange();
        return "/reportLab/laboratary_income_report?faces-redirect=true";
    }

    public String navigateToLaborataryIncomeReportFromLabAnalyticsDto() {
        resetAllFiltersExceptDateRange();
        return "/reportLab/laboratary_income_report_dto?faces-redirect=true";
    }

    public String navigateToLaborataryCardIncomeReportFromLabAnalytics() {
        resetAllFiltersExceptDateRange();
        return "/reportLab/laboratary_card_income_report?faces-redirect=true";
    }

    public String navigateToLaboratarySummaryFromLabAnalytics() {
        resetAllFiltersExceptDateRange();
        return "/reportLab/laboratary_summary?faces-redirect=true";
    }

    public String navigateToLaborataryTestWiseCountReportFromLabAnalytics() {
        resetAllFiltersExceptDateRange();
        return "/reportLab/test_wise_count?faces-redirect=true";
    }

    public String navigateToLaborataryTestWiseRegentCostReportFromLabAnalytics() {
        resetAllFiltersExceptDateRange();
        return "/reportLab/test_wise_regent_cost_report?faces-redirect=true";
    }

    public boolean isOptimizedMethodEnabled() {
        return configController.getBooleanValueByKey("Laboratory Income Report - Optimized Method", false);
    }

    public boolean isLegacyMethodEnabled() {
        return configController.getBooleanValueByKey("Laboratory Income Report - Legacy Method", true);
    }

    public String navigateToLaborataryTestWiseCountReportDtoFromLabAnalytics() {
        resetAllFiltersExceptDateRange();
        setToInstitution(sessionController.getInstitution());
        setToDepartment(sessionController.getDepartment());
        return "/reportLab/test_wise_count_dto?faces-redirect=true";
    }

    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Functions">
// Simple tab persistence - no complex event handling needed
    // The activeIndex binding handles persistence automatically
    public void resetAllFiltersExceptDateRange() {
        setViewTemplate(null);
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
        setFromDate(null);
        setToDate(null);
        setVisitType(null);
        totalHospitalFee = 0.0;
        totalReagentFee = 0.0;
        totalOtherFee = 0.0;
        totalNetTotal = 0.0;
        totalDiscount = 0.0;
        totalServiceCharge = 0.0;

        testWiseCounts = new ArrayList<>();;
        totalNetHosFee = 0.0;
        totalAdditionalFee = 0.0;
        totalCCFee = 0.0;
        totalProFee = 0.0;
        totalHosFee = 0.0;
        totalCount = 0.0;
        investigation = null;

    }

    public void processLaboratoryCardIncomeReport() {
        List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
        payments = new ArrayList<>();

        //Add All OPD BillTypes
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_REFUND);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        billTypeAtomics.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
        billTypeAtomics.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);

        //Add All Inward BillTypes
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);

        //Add All Package BillTypes
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);

        //Add All CC BillTypes
        billTypeAtomics.add(BillTypeAtomic.CC_BILL);
        billTypeAtomics.add(BillTypeAtomic.CC_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.CC_BILL_REFUND);

        List<Payment> allPayments = paymentService.fetchPayments(fromDate, toDate, institution, site, department, null, billTypeAtomics, null, null, null, null, visitType);

        payments = allPayments.stream().filter(p -> p.getPaymentMethod() == PaymentMethod.Card).collect(Collectors.toList());
        totalNetTotal = allPayments.stream().filter(p -> p.getPaymentMethod() == PaymentMethod.Card).mapToDouble(Payment::getPaidValue).sum();
    }

    public void processLaboratoryIncomeReport() {
        List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();

        //Add All OPD BillTypes
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_REFUND);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);

        //Add All Inward BillTypes
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);

        //Add All Package BillTypes
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);

        //Add All CC BillTypes
        billTypeAtomics.add(BillTypeAtomic.CC_BILL);
        billTypeAtomics.add(BillTypeAtomic.CC_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.CC_BILL_REFUND);

        List<Bill> bills = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme, toInstitution, toDepartment, visitType);
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
    }

    public void processLaboratoryIncomeReportDto() {
        List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_REFUND);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);

        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);

        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);

        billTypeAtomics.add(BillTypeAtomic.CC_BILL);
        billTypeAtomics.add(BillTypeAtomic.CC_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.CC_BILL_REFUND);

        labIncomeReportDtos = billService.fetchBillsAsLabIncomeReportDTOs(fromDate,
                toDate, institution, site, department, webUser, billTypeAtomics,
                admissionType, paymentScheme, toInstitution, toDepartment, visitType);

        // Create IncomeBundle from DTOs to calculate payment breakdowns
        bundle = new IncomeBundle(labIncomeReportDtos);
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

    public void processLaboratoryOrderReport() {
        List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();

        //Add All OPD BillTypes
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_REFUND);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);

        //Add All Inward BillTypes
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);

        //Add All Package BillTypes
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);

        //Add All CC BillTypes
        billTypeAtomics.add(BillTypeAtomic.CC_BILL);
        billTypeAtomics.add(BillTypeAtomic.CC_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.CC_BILL_REFUND);

        List<Bill> fetchedBills = billService.fetchBills(fromDate, toDate, institution, site, department, webUser, billTypeAtomics, admissionType, paymentScheme, toInstitution, toDepartment, visitType);

        bundle = new IncomeBundle();

        totalHospitalFee = 0.0;
        totalReagentFee = 0.0;
        totalOtherFee = 0.0;
        totalNetTotal = 0.0;
        totalDiscount = 0.0;
        totalServiceCharge = 0.0;

        for (Bill bill : fetchedBills) {
            IncomeRow billIncomeRow = new IncomeRow(bill);
            bundle.getRows().add(billIncomeRow);
            boolean checkInInvestigationBillItem = false;
            billService.reloadBill(bill);
            for (BillItem billItem : bill.getBillItems()) {
                if (billItem.getItem() instanceof Investigation) {
                    IncomeRow billItemIncomeRow = new IncomeRow(billItem);
                    bundle.getRows().add(billItemIncomeRow);
                    checkInInvestigationBillItem = true;
                    totalHospitalFee += (billItem.getHospitalFee() - billItem.getReagentFee() - billItem.getOtherFee());
                    totalReagentFee += billItem.getReagentFee();
                    totalOtherFee += billItem.getOtherFee();
                    totalNetTotal += billItem.getNetValue();
                }
            }
            if (checkInInvestigationBillItem == true) {
                totalDiscount += bill.getDiscount();
                totalServiceCharge += bill.getServiceCharge();
            } else {
                bundle.getRows().remove(billIncomeRow);
            }
        }
    }

    public void processLaboratorySummary() {
        System.out.println("processLaboratorySummary");

        bundle = new IncomeBundle(bills);

    }

    public void processLabTestWiseCountReport() {

        Map<String, Object> params = new HashMap<>();

        String jpql = "select new com.divudi.core.data.TestWiseCountReport("
                + "bi.item.name, "
                + "count(bi.item.name), "
                + "sum(bi.hospitalFee), "
                + "sum(bi.collectingCentreFee), "
                + "sum(bi.staffFee), "
                + "sum(bi.reagentFee), "
                + "sum(bi.otherFee), "
                + "sum(bi.discount), "
                + "sum(bi.netValue)"
                + ") "
                + " from BillItem bi "
                + " where bi.retired = :ret "
                + " and bi.bill.createdAt between :fd and :td "
                + " and bi.bill.billTypeAtomic IN :bType " // Corrected IN clause
                + " and type(bi.item) = :invType ";

        // Adding filters for institution, department, site
        if (institution != null) {
            jpql += " and bi.bill.institution=:ins ";
            params.put("ins", institution);
        }

        if (webUser != null) {
            jpql += " and bi.bill.creater=:user ";
            params.put("user", webUser);
        }

        if (department != null) {
            jpql += " and bi.bill.department=:dep ";
            params.put("dep", department);
        }

        if (admissionType != null) {
            jpql += " and bi.bill.patientEncounter.admissionType=:admissionType ";
            params.put("admissionType", admissionType);
        }

        if (paymentScheme != null) {
            jpql += " and bi.bill.paymentScheme=:paymentScheme ";
            params.put("paymentScheme", paymentScheme);
        }

        if (toInstitution != null) {
            jpql += " and bi.bill.toInstitution=:toIns ";
            params.put("toIns", toInstitution);
        }

        if (toDepartment != null) {
            jpql += " and bi.bill.toDepartment=:toDep ";
            params.put("toDep", toDepartment);
        }

        if (visitType != null && !visitType.trim().isEmpty()) {
            jpql += " AND bi.bill.ipOpOrCc = :type ";
            params.put("type", visitType.trim());
        }
        jpql += " group by bi.item.name ";

        params.put("ret", false);
        params.put("fd", fromDate);
        params.put("td", toDate);

        params.put("invType", Investigation.class);

        // Handle multiple bill types
        List<BillTypeAtomic> bTypes = Arrays.asList(
                BillTypeAtomic.OPD_BILL_WITH_PAYMENT,
                BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER,
                BillTypeAtomic.CC_BILL,
                BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT,
                BillTypeAtomic.INWARD_SERVICE_BILL);

        params.put("bType", bTypes);  // Use 'bType' for IN clause

        // Fetch results for OpdBill
        List<TestWiseCountReport> positiveResults = (List<TestWiseCountReport>) billItemFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        // Now fetch results for OpdBillCancel (use a list for single bType)
        params.put("bType", Arrays.asList(BillTypeAtomic.OPD_BILL_CANCELLATION, BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION, BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION, BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION, BillTypeAtomic.CC_BILL_CANCELLATION, BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION, BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION));
        List<TestWiseCountReport> cancelResults = (List<TestWiseCountReport>) billItemFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        // Now fetch results for OpdBillRefund (use a list for single bType)
        params.put("bType", Arrays.asList(BillTypeAtomic.OPD_BILL_REFUND, BillTypeAtomic.PACKAGE_OPD_BILL_REFUND, BillTypeAtomic.CC_BILL_REFUND, BillTypeAtomic.INWARD_SERVICE_BILL_REFUND));

        List<TestWiseCountReport> refundResults = (List<TestWiseCountReport>) billItemFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        // Subtract cancel and refund results from the main results
        Map<String, TestWiseCountReport> resultMap = new HashMap<>();

        for (TestWiseCountReport posResult : positiveResults) {
            resultMap.put(posResult.getTestName(), posResult);
        }

        // Subtract cancel results
        for (TestWiseCountReport cancelResult : cancelResults) {
            TestWiseCountReport posResult = resultMap.get(cancelResult.getTestName());
            if (posResult != null) {
                posResult.setCount(posResult.getCount() - Math.abs(cancelResult.getCount()));
                posResult.setHosFee(posResult.getHosFee() - Math.abs(cancelResult.getHosFee()));
                posResult.setCcFee(posResult.getCcFee() - Math.abs(cancelResult.getCcFee()));
                posResult.setProFee(posResult.getProFee() - Math.abs(cancelResult.getProFee()));
                posResult.setReagentFee(posResult.getReagentFee() - Math.abs(cancelResult.getReagentFee()));
                posResult.setOtherFee(posResult.getOtherFee() - Math.abs(cancelResult.getOtherFee()));
                posResult.setTotal(posResult.getTotal() - Math.abs(cancelResult.getTotal()));
                posResult.setDiscount(posResult.getDiscount() - Math.abs(cancelResult.getDiscount()));
            }
        }

        // Subtract refund results
        for (TestWiseCountReport refundResult : refundResults) {
            TestWiseCountReport posResult = resultMap.get(refundResult.getTestName());
            if (posResult != null) {
                posResult.setHosFee(posResult.getHosFee() - Math.abs(refundResult.getHosFee()));
                posResult.setCcFee(posResult.getCcFee() - Math.abs(refundResult.getCcFee()));
                posResult.setProFee(posResult.getProFee() - Math.abs(refundResult.getProFee()));
                posResult.setReagentFee(posResult.getReagentFee() - Math.abs(refundResult.getReagentFee()));
                posResult.setOtherFee(posResult.getOtherFee() - Math.abs(refundResult.getOtherFee()));
                posResult.setTotal(posResult.getTotal() - Math.abs(refundResult.getTotal()));
                posResult.setDiscount(posResult.getDiscount() - Math.abs(refundResult.getDiscount()));
            }
        }

        List<TestWiseCountReport> tempTestWiseCounts = new ArrayList<>(resultMap.values());

        List<TestWiseCountReport> testWiseCount = new ArrayList<>();

        totalCount = 0.0;
        totalHosFee = 0.0;
        totalCCFee = 0.0;
        totalProFee = 0.0;
        totalReagentFee = 0.0;
        totalAdditionalFee = 0.0;
        totalNetTotal = 0.0;
        totalDiscount = 0.0;
        totalNetHosFee = 0.0;

        for (TestWiseCountReport twc : tempTestWiseCounts) {
            if (twc.getCount() > 0.0) {
                testWiseCount.add(twc);

                totalCount += twc.getCount();
                totalHosFee += (twc.getHosFee());
                totalCCFee += twc.getCcFee();
                totalProFee += twc.getProFee();
                totalReagentFee += twc.getReagentFee();
                totalAdditionalFee += twc.getOtherFee();
                totalNetTotal += twc.getTotal();
                totalDiscount += twc.getDiscount();
                totalNetHosFee += twc.getHosFee() - twc.getDiscount();
            }
        }

        testWiseCounts = alphabetList(testWiseCount);

    }

    public void processLabTestWiseCountReportDto() {
        Map<String, Object> params = new HashMap<>();

        String jpql = "select new com.divudi.core.data.dto.TestCountDTO("
                + "bi.item.id, bi.item.code, bi.item.name, "
                + "count(bi), "
                + "sum(bi.hospitalFee), "
                + "sum(bi.collectingCentreFee), "
                + "sum(bi.staffFee), "
                + "sum(bi.reagentFee), "
                + "sum(bi.otherFee), "
                + "sum(bi.discount), "
                + "sum(bi.netValue)) "
                + " from BillItem bi "
                + " where bi.retired = :ret "
                + " and bi.bill.createdAt between :fd and :td "
                + " and bi.bill.billTypeAtomic IN :bType "
                + " and type(bi.item) = :invType ";

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

        jpql += " group by bi.item.id, bi.item.code, bi.item.name";

        params.put("ret", false);
        params.put("fd", fromDate);
        params.put("td", toDate);
        params.put("invType", Investigation.class);

        List<BillTypeAtomic> bTypes = Arrays.asList(
                BillTypeAtomic.OPD_BILL_WITH_PAYMENT,
                BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER,
                BillTypeAtomic.CC_BILL,
                BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT,
                BillTypeAtomic.INWARD_SERVICE_BILL);
        params.put("bType", bTypes);

        testWiseCountDtos = (List<TestCountDTO>) billItemFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        totalCount = 0.0;
        totalHosFee = 0.0;
        totalCCFee = 0.0;
        totalProFee = 0.0;
        totalReagentFee = 0.0;
        totalAdditionalFee = 0.0;
        totalNetTotal = 0.0;
        totalDiscount = 0.0;
        totalNetHosFee = 0.0;

        if (testWiseCountDtos != null) {
            for (TestCountDTO dto : testWiseCountDtos) {
                totalCount += dto.getCount();
                totalHosFee += dto.getHosFee();
                totalCCFee += dto.getCcFee();
                totalProFee += dto.getProFee();
                totalReagentFee += dto.getReagentFee();
                totalAdditionalFee += dto.getOtherFee();
                totalNetTotal += dto.getTotal();
                totalDiscount += dto.getDiscount();
                totalNetHosFee += dto.getHosFee() - dto.getDiscount();
            }
        }
    }

    public void processLabTestWiseReagentCostReportDto() {
        Map<String, Object> params = new HashMap<>();

        String jpql = "select new com.divudi.core.data.dto.TestCountDTO("
                + " bf.billItem.item.id, "
                + " bf.billItem.item.name, "
                + " count(bf.billItem), "
                + " bf.feeGrossValue ) "
                + " from BillFee bf "
                + " where bf.retired =:bfRet"
                + " and bf.billItem.retired =:ret "
                + " and bf.billItem.refunded =:ref "
                + " and bf.billItem.bill.cancelled =:can "
                + " and bf.billItem.bill.retired =:bRet "
                + " and bf.fee.feeType =:feeType "
                + " and bf.billItem.bill.createdAt between :fd and :td "
                + " and bf.billItem.bill.billTypeAtomic IN :bType "
                + " and type(bf.billItem.item) = :invType ";

        if (institution != null) {
            jpql += " and bf.billItem.bill.institution=:ins ";
            params.put("ins", institution);
        }

        if (webUser != null) {
            jpql += " and bf.billItem.bill.creater=:user ";
            params.put("user", webUser);
        }

        if (department != null) {
            jpql += " and bf.billItem.bill.department=:dep ";
            params.put("dep", department);
        }

        if (admissionType != null) {
            jpql += " and bf.billItem.bill.patientEncounter.admissionType=:admissionType ";
            params.put("admissionType", admissionType);
        }

        if (paymentScheme != null) {
            jpql += " and bf.billItem.bill.paymentScheme=:paymentScheme ";
            params.put("paymentScheme", paymentScheme);
        }

        if (toInstitution != null) {
            jpql += " and bf.billItem.bill.toInstitution=:toIns ";
            params.put("toIns", toInstitution);
        }

        if (toDepartment != null) {
            jpql += " and bf.billItem.bill.toDepartment=:toDep ";
            params.put("toDep", toDepartment);
        }

        if (visitType != null && !visitType.trim().isEmpty()) {
            jpql += " AND bf.billItem.bill.ipOpOrCc = :type ";
            params.put("type", visitType.trim());
        }

        jpql += " group by bf.billItem.item.id, bf.billItem.item.name, bf.feeGrossValue ";

        params.put("ret", false);
        params.put("ref", false);
        params.put("bRet", false);
        params.put("bfRet", false);
        params.put("can", false);
        params.put("fd", getFromDate());
        params.put("feeType", FeeType.Chemical);
        params.put("td", getToDate());
        params.put("invType", Investigation.class);

        List<BillTypeAtomic> bTypes = Arrays.asList(
                BillTypeAtomic.OPD_BILL_WITH_PAYMENT,
                BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER,
                BillTypeAtomic.CC_BILL,
                BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT,
                BillTypeAtomic.INWARD_SERVICE_BILL);
        params.put("bType", bTypes);

        List<TestCountDTO> testWiseReagentCount = (List<TestCountDTO>) billFeeFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        totalCount = 0.0;
        totalNetTotal = 0.0;

        if (testWiseReagentCount != null) {
            for (TestCountDTO dto : testWiseReagentCount) {
                double itemNetTotal = 0.0;
                double reagentFee = dto.getReagentFee() == null ? 0.0 : dto.getReagentFee();
                itemNetTotal = dto.getCount() * reagentFee;
                dto.setTotal(itemNetTotal);
                totalCount += dto.getCount();
                totalNetTotal += itemNetTotal;
            }
        }
        List<TestCountDTO> safeList = testWiseReagentCount == null ? java.util.Collections.emptyList() : testWiseReagentCount;
        testWiseCountDtos = setAlphabetList(safeList);
    }

    public List<TestWiseCountReport> alphabetList(List<TestWiseCountReport> testWiseCounts) {

        List<TestWiseCountReport> reportList = testWiseCounts.stream()
                .sorted(Comparator.comparing(TestWiseCountReport::getTestName))
                .collect(Collectors.toList());

        return reportList;
    }

    public List<TestCountDTO> setAlphabetList(List<TestCountDTO> testCountDTO) {

        List<TestCountDTO> reportList = testCountDTO.stream()
                .sorted(Comparator.comparing(TestCountDTO::getTestName))
                .collect(Collectors.toList());

        return reportList;
    }

    public void processLaborataryBillItemReportDto() {
        reportTimerController.trackReportExecution(() -> {
            if (investigation == null) {
                JsfUtil.addErrorMessage("Investigation Missing.");
                return;
            }

            Map<String, Object> params = new HashMap<>();

            String jpql = "select new com.divudi.core.data.dto.PatientInvestigationDTO("
                    + " pi.id,"
                    + " pi.billItem.item.name, "
                    + " pi.billItem.bill.createdAt, "
                    + " pi.billItem.bill.deptId, "
                    + " pi.billItem.bill.patient.person.title, "
                    + " pi.billItem.bill.patient.person.name, "
                    + " pi.billItem.bill.patient.id,"
                    + " pi.billItem.bill.patient.person.sex) "
                    + " from PatientInvestigation pi "
                    + " where pi.billItem.retired = :ret "
                    + " and pi.billItem.bill.cancelled =:can "
                    + " and pi.billItem.refunded =:ref "
                    + " and pi.billItem.bill.createdAt between :fd and :td "
                    + " and pi.billItem.bill.billTypeAtomic IN :bType "
                    + " and type(pi.billItem.item) = :invType ";

            if (investigation != null) {
                jpql += " and pi.investigation =:investigation ";
                params.put("investigation", investigation);
            }

            if (institution != null) {
                jpql += " and pi.billItem.bill.institution=:ins ";
                params.put("ins", institution);
            }
            if (department != null) {
                jpql += " and pi.billItem.bill.department=:dep ";
                params.put("dep", department);
            }
            if (site != null) {
                jpql += " and pi.billItem.bill.department.site=:site ";
                params.put("site", site);
            }

            jpql += " order by pi.billItem.bill.createdAt asc ";

            params.put("ret", false);
            params.put("fd", fromDate);
            params.put("td", toDate);
            params.put("invType", Investigation.class);
            params.put("can", false);
            params.put("ref", false);

            List<BillTypeAtomic> bTypes = Arrays.asList(
                    BillTypeAtomic.OPD_BILL_WITH_PAYMENT,
                    BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER,
                    BillTypeAtomic.CC_BILL,
                    BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT,
                    BillTypeAtomic.INWARD_SERVICE_BILL);
            params.put("bType", bTypes);

            itemList = (List<PatientInvestigationDTO>) patientInvestigationFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

            for (PatientInvestigationDTO pi : itemList) {
                Patient pt = patientFacade.find(pi.getPatientid());
                String age = pt.getAgeOnBilledDate(pi.getBillDate());
                pi.setPatientAge(age);
            }
        }, LaboratoryReport.INVESTIGATION_BILL_LIST_REPORT, sessionController.getLoggedUser());
    }

    public List<Investigation> completeInvestigations(String qry) {

        String jpql = "SELECT c "
                + " FROM Item c "
                + " WHERE TYPE(c) = Investigation "
                + " AND c.retired = :ret "
                + " AND (UPPER(c.name) LIKE :nameQuery  OR c.code LIKE :codeQuery) "
                + " ORDER BY c.name";

        Map parameters = new HashMap<>();
        parameters.put("nameQuery", "%" + qry.toUpperCase() + "%");
        parameters.put("codeQuery", "%" + qry + "%");
        parameters.put("ret", false);

        List<Investigation> completeItems = investigationFacade.findByJpql(jpql, parameters, 15);
        return completeItems;
    }

    private void processPayment(Bill b, IncomeRow incomeRow) {
        switch (b.getPaymentMethod()) {
            case Cash:
                incomeRow.setCashValue(incomeRow.getCashValue() + b.getNetTotal());
                break;
            case Card:
                incomeRow.setCardValue(incomeRow.getCardValue() + b.getNetTotal());
                break;
            case Agent:
                incomeRow.setAgentValue(incomeRow.getAgentValue() + b.getNetTotal());
                break;
            case OnlineSettlement:
                incomeRow.setOnlineSettlementValue(incomeRow.getOnlineSettlementValue() + b.getNetTotal());
                break;
            case Credit:
                incomeRow.setCreditValue(incomeRow.getCreditValue() + b.getNetTotal());
                break;
            case MultiplePaymentMethods:
                calculateBillAndBatchBillPaymentValuesFromPayments(b, incomeRow);
                break;
        }
        incomeRow.setTotalBillsDiscount(incomeRow.getTotalBillsDiscount() + b.getDiscount());
        incomeRow.setGrossTotal(incomeRow.getGrossTotal() + b.getNetTotal());
    }

    public void processLaboratoryAllIncomeSummary() {
        // 1. Initialize bill types
        List<BillTypeAtomic> billTypeAtomics = Arrays.asList(
                BillTypeAtomic.OPD_BILL_WITH_PAYMENT,
                BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT,
                BillTypeAtomic.INWARD_SERVICE_BILL,
                BillTypeAtomic.CC_BILL,
                BillTypeAtomic.OPD_BILL_REFUND,
                BillTypeAtomic.OPD_BILL_CANCELLATION,
                BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION,
                BillTypeAtomic.INWARD_SERVICE_BILL_REFUND,
                BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION,
                BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION,
                BillTypeAtomic.CC_BILL_REFUND,
                BillTypeAtomic.CC_BILL_CANCELLATION
        );

        // 2. Fetch bills
        List<Bill> incomeBills = getLaboratoryBills(billTypeAtomics);
        if (incomeBills == null || incomeBills.isEmpty()) {
            return;
        }

        // 3. Initialize income categories
        bundle = new IncomeBundle();
        Map<String, IncomeRow> incomeRows = initializeIncomeCategories();

        // 4. Process each bill
        processAllBills(incomeBills, incomeRows);

        // 5. Add rows to bundle
        addRowsToBundle(incomeRows);

        // 6. Calculate net totals for each category
        calculateNetTotals();

        // 7. Calculate and add summary row
        addSummaryRow();

    }

// Helper methods:
    private Map<String, IncomeRow> initializeIncomeCategories() {
        Map<String, IncomeRow> incomeRows = new LinkedHashMap<>();

        String[] categories = {"OPD", "Inpatient", "Home Visit", "Collection", "Total"};

        for (String category : categories) {
            IncomeRow row = new IncomeRow();
            row.setCategoryName(category);
            incomeRows.put(category, row);
        }

        return incomeRows;
    }

    private void processAllBills(List<Bill> incomeBills, Map<String, IncomeRow> incomeRows) {
        for (Bill bill : incomeBills) {
            switch (bill.getBillTypeAtomic()) {
                // OPD Section
                case OPD_BILL_WITH_PAYMENT:
                case PACKAGE_OPD_BILL_WITH_PAYMENT:
                    processPayment(bill, incomeRows.get("OPD"));
                    break;

                case OPD_BILL_CANCELLATION:
                case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
                    addToCancellation(incomeRows.get("OPD"), bill.getNetTotal());
                    break;

                case OPD_BILL_REFUND:
                    addToRefund(incomeRows.get("OPD"), bill.getNetTotal());
                    break;

                // CC Section    
                case CC_BILL:
                    addToAgentValue(incomeRows.get("Collection"), bill.getNetTotal());
                    break;

                case CC_BILL_REFUND:
                    addToRefund(incomeRows.get("Collection"), bill.getNetTotal());
                    break;

                case CC_BILL_CANCELLATION:
                    addToCancellation(incomeRows.get("Collection"), bill.getNetTotal());
                    break;

                // Inward Section
                case INWARD_SERVICE_BILL:
                    addToCreditValue(incomeRows.get("Inpatient"), bill.getNetTotal());
                    break;

                case INWARD_SERVICE_BILL_REFUND:
                    addToRefund(incomeRows.get("Inpatient"), bill.getNetTotal());
                    break;

                case INWARD_SERVICE_BILL_CANCELLATION:
                case INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
                    addToCancellation(incomeRows.get("Inpatient"), bill.getNetTotal());
                    break;
            }
        }
    }

    private void addRowsToBundle(Map<String, IncomeRow> incomeRows) {
        // Add all rows except "Total" to bundle
        incomeRows.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("Total"))
                .forEach(entry -> bundle.getRows().add(entry.getValue()));
    }

    private void calculateNetTotals() {
        bundle.getRows().forEach(row -> {
            double totalDeduction = Math.abs(
                    row.getTotalBillsRefund()
                    + row.getTotalBillsCancel()
            );
            row.setNetTotal(row.getGrossTotal() - totalDeduction);
        });
    }

    private void addSummaryRow() {
        IncomeRow totalRow = new IncomeRow();
        totalRow.setCategoryName("Total");

        bundle.getRows().forEach(row -> {
            totalRow.setCashValue(totalRow.getCashValue() + row.getCashValue());
            totalRow.setCardValue(totalRow.getCardValue() + row.getCardValue());
            totalRow.setAgentValue(totalRow.getAgentValue() + row.getAgentValue());
            totalRow.setOnlineSettlementValue(totalRow.getOnlineSettlementValue() + row.getOnlineSettlementValue());
            totalRow.setCreditValue(totalRow.getCreditValue() + row.getCreditValue());
            totalRow.setGrossTotal(totalRow.getGrossTotal() + row.getGrossTotal());
            totalRow.setTotalBillsRefund(totalRow.getTotalBillsRefund() + row.getTotalBillsRefund());
            totalRow.setTotalBillsCancel(totalRow.getTotalBillsCancel() + row.getTotalBillsCancel());
            totalRow.setTotalBillsDiscount(totalRow.getTotalBillsDiscount() + row.getTotalBillsDiscount());
            totalRow.setNetTotal(totalRow.getNetTotal() + row.getNetTotal());
        });

        bundle.getRows().add(totalRow);
    }

// Small helper methods for specific operations
    private void addToAgentValue(IncomeRow row, double amount) {
        row.setAgentValue(row.getAgentValue() + amount);
        row.setGrossTotal(row.getGrossTotal() + amount);
    }

    private void addToCreditValue(IncomeRow row, double amount) {
        row.setCreditValue(row.getCreditValue() + amount);
        row.setGrossTotal(row.getGrossTotal() + amount);
    }

    private void addToRefund(IncomeRow row, double amount) {
        row.setTotalBillsRefund(row.getTotalBillsRefund() + amount);
    }

    private void addToCancellation(IncomeRow row, double amount) {
        row.setTotalBillsCancel(row.getTotalBillsCancel() + amount);
    }

    private void calculateBillAndBatchBillPaymentValuesFromPayments(Bill bill, IncomeRow incomeRow) {
        if (bill == null || bill.getPaymentMethod() == null || bill.getPaymentMethod() != PaymentMethod.MultiplePaymentMethods) {
            return;
        }

        // Identify the batch bill and the individual bill.
        Bill individualBill = bill;
        Bill batchBill = bill.getBackwardReferenceBill();

        // Net totals for ratio calculation (if needed).
        double netTotalOfBatchBill = batchBill != null ? batchBill.getNetTotal() : 0.0;
        double netTotalOfIndividualBill = individualBill.getNetTotal();

        // Determine the ratio for allocating batch-bill payments to the individual bill.
        // If there's no batch bill or the batch bill total is zero, we use a ratio of 1.0 (i.e., full amount).
        double ratio = 1.0;
        if (batchBill != null && netTotalOfBatchBill != 0.0) {
            ratio = netTotalOfIndividualBill / netTotalOfBatchBill;
        }

        List<Payment> payments = billSearch.fetchBillPayments(batchBill);
        if (payments == null || payments.isEmpty()) {
            return;
        }

        for (Payment p : payments) {

            double allocatedValue = p.getPaidValue() * ratio;

            switch (p.getPaymentMethod()) {
                case Cash:
                    double cash = incomeRow.getCashValue() + allocatedValue;
                    incomeRow.setCashValue(cash);
                    break;
                case Card:
                    double card = incomeRow.getCardValue() + allocatedValue;
                    incomeRow.setCardValue(card);
                    break;
                case Agent:
                    double agent = incomeRow.getAgentValue() + allocatedValue;
                    incomeRow.setAgentValue(agent);
                    break;
                case OnlineSettlement:
                    double online = incomeRow.getOnlineSettlementValue() + allocatedValue;
                    incomeRow.setOnlineSettlementValue(online);
                    break;
                case Credit:
                    double credit = incomeRow.getCreditValue() + allocatedValue;
                    incomeRow.setCreditValue(credit);
                    break;
            }
        }
    }

    public List<Bill> getLaboratoryBills(List<BillTypeAtomic> billTypeAtomics) {
        List<Bill> incomeBills = billService.fetchBills(fromDate, toDate, null, null, null, null, billTypeAtomics, null, null, null, null, DepartmentType.Lab, null);
        return incomeBills;
    }

    // <editor-fold defaultstate="collapsed" desc="9B Report">
    @Inject
    BillBeanController billBean;
    @Inject
    PaymentSchemeController paymentSchemeController;
    @Inject
    AdmissionTypeController admissionTypeController;
    
    //9B Process Method
    public void generateDailyLabSummaryByDepartment() {

        if (getDepartment() == null) {
            JsfUtil.addErrorMessage("Please Select the Department");
            return;
        }

        bundleReport = new ReportTemplateRowBundle();

        //Normal Income
        List<BillLight> normalIncomeList = billService.fetchBillDtos(fromDate, toDate, null, null, department, getOpdAndPackageBillTypeAtomics(), null, null);
        ReportTemplateRow normalIncomeRow = new ReportTemplateRow();
        normalIncomeRow.setItemName("Normal Income");
        initializeRows(normalIncomeRow);
        normalIncomeRow = genarateRowBundle(normalIncomeList, normalIncomeRow);
        bundleReport.getReportTemplateRows().add(normalIncomeRow);

        //Member Schemes
        List<PaymentScheme> pss = paymentSchemeController.getPaymentSchemesForOPD();
        for (PaymentScheme ps : pss) {
            ReportTemplateRow paymentSchemeIncomeRow = new ReportTemplateRow();
            paymentSchemeIncomeRow.setItemName(ps.getName() + " Income");
            initializeRows(paymentSchemeIncomeRow);
            List<BillLight> paymentSchemeIncome1 = billService.fetchBillDtos(fromDate, toDate, null, null, department, getOpdAndPackageBillTypeAtomics(), null, ps);
            paymentSchemeIncomeRow = genarateRowBundle(paymentSchemeIncome1, paymentSchemeIncomeRow);
            bundleReport.getReportTemplateRows().add(paymentSchemeIncomeRow);
        }

        //Inward Income
        List<AdmissionType> ats = admissionTypeController.getItems();

        for (AdmissionType at : ats) {
            ReportTemplateRow inwardIncomeRow = new ReportTemplateRow();
            inwardIncomeRow.setItemName(at + " Income");
            initializeRows(inwardIncomeRow);
            List<BillLight> inpatientIncome = billService.fetchBillDtos(fromDate, toDate, institution, site, department, null, getInwardBillTypeAtomics(), at,null);
            inwardIncomeRow = genarateRowBundleInward(inpatientIncome, inwardIncomeRow);
            bundleReport.getReportTemplateRows().add(inwardIncomeRow);
        }

        //outpatientIncome (CC)
        ReportTemplateRow outPatientIncomeRow = new ReportTemplateRow();
        outPatientIncomeRow.setItemName("Outpatient Income");
        initializeRows(outPatientIncomeRow);
        List<BillLight> outPatientIncome = billService.fetchBillDtos(fromDate, toDate, institution, site, department, null, getOutPatientBillTypeAtomics(), null,null);
        outPatientIncomeRow = genarateRowBundle(outPatientIncome, outPatientIncomeRow);
        bundleReport.getReportTemplateRows().add(outPatientIncomeRow);
        
        //Float Income
        ReportTemplateRow floatIncomeRow = new ReportTemplateRow();
        floatIncomeRow.setItemName("Float Income");
        initializeRows(floatIncomeRow);
        List<BillLight> floatIncome = billService.fetchBillDtos(fromDate, toDate, institution, site, department, null, getFloatInconeBillTypeAtomics(), null,null);
        floatIncomeRow = genarateRowBundleOther(floatIncome, floatIncomeRow);
        bundleReport.getReportTemplateRows().add(floatIncomeRow);
        
        //Outher Income
        ReportTemplateRow otherIncomeRow = new ReportTemplateRow();
        otherIncomeRow.setItemName("Other Income");
        initializeRows(otherIncomeRow);
        bundleReport.getReportTemplateRows().add(otherIncomeRow);
        
        //calculate Addition Totals
        calculateTotalsFromRows(bundleReport.getReportTemplateRows(), true);

//        //Deductions
        bundle = new IncomeBundle();

        //Deducations Voucher
        IncomeRow floatTransferDeductionRow = new IncomeRow();
        floatTransferDeductionRow.setItemName("Float Transfer");
        initializeDeductionRows(floatTransferDeductionRow);
        List<BillLight> floatTransfer = billService.fetchBillDtos(fromDate, toDate, institution, site, department, null, getFloatTransferBillTypeAtomics(), null,null);
        floatTransferDeductionRow = genarateDeductionRowBundleOther(floatTransfer, floatTransferDeductionRow);
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

    public void initializeRows(ReportTemplateRow row) {
        row.setCashValue(0.0);
        row.setCardValue(0.0);
        row.setOnlineSettlementValue(0.0);
        row.setCreditValue(0.0);
        row.setInpatientTotal(0.0);
        row.setOtherIncomeValue(0.0);
        row.setTotal(0.0);
        row.setDiscount(0.0);
        row.setServiceCharge(0.0);
    }

    public ReportTemplateRow genarateRowBundle(List<BillLight> billLights, ReportTemplateRow row) {
        List<BillItemDTO> billItems = new ArrayList<>();
        Set<Bill> processedMultipleBills = new HashSet<>();
        for (BillLight bl : billLights) {
            List<BillItemDTO> bidtos = billBean.fillBillItemDTOs(bl.getId());
            billItems.addAll(bidtos);
        }

        for (BillItemDTO bi : billItems) {
            if (!(bi.getItemClass().equals("Investigation"))) {
                continue;
            }
            if (null == bi.getPaymentMethod()) {
                continue;
            } else {
                switch (bi.getPaymentMethod()) {
                    case Cash:
                        row.setCashValue(row.getCashValue() + bi.getBillNetTotal());
                        row.setTotal(row.getTotal() + bi.getBillNetTotal());
                        row.setDiscount(row.getDiscount() + bi.getDiscount());
                        row.setServiceCharge(row.getServiceCharge() + bi.getMarginValue());
                        break;
                    case Card:
                        row.setCardValue(row.getCardValue() + bi.getBillNetTotal());
                        row.setTotal(row.getTotal() + bi.getBillNetTotal());
                        row.setDiscount(row.getDiscount() + bi.getDiscount());
                        row.setServiceCharge(row.getServiceCharge() + bi.getMarginValue());
                        break;
                    case Credit:
                        row.setCreditValue(row.getCreditValue() + bi.getBillNetTotal());
                        row.setTotal(row.getTotal() + bi.getBillNetTotal());
                        row.setDiscount(row.getDiscount() + bi.getDiscount());
                        row.setServiceCharge(row.getServiceCharge() + bi.getMarginValue());
                        break;
                    case OnlineSettlement:
                        row.setOnlineSettlementValue(row.getOnlineSettlementValue() + bi.getBillNetTotal());
                        row.setTotal(row.getTotal() + bi.getBillNetTotal());
                        row.setDiscount(row.getDiscount() + bi.getDiscount());
                        row.setServiceCharge(row.getServiceCharge() + bi.getMarginValue());
                        break;
                    case MultiplePaymentMethods:
                        Bill b = billBean.fetchBill(bi.getBillId());
                        if (!processedMultipleBills.contains(b)) {
                            processMultiplePaymentBill(b, row);
                            processedMultipleBills.add(b);
                        }
                        break;
                    default:
                        row.setOtherIncomeValue(row.getOtherIncomeValue() + bi.getBillNetTotal());
                        row.setTotal(row.getTotal() + bi.getBillNetTotal());
                        row.setDiscount(row.getDiscount() + bi.getDiscount());
                        row.setServiceCharge(row.getServiceCharge() + bi.getMarginValue());
                        break;
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
                            row.setOnlineSettlementValue(row.getOnlineSettlementValue() + p.getPaidValue());
                            row.setTotal(row.getTotal() + p.getPaidValue());
                            break;
                        default:
                            row.setOtherIncomeValue(row.getOtherIncomeValue() + p.getPaidValue());
                            row.setTotal(row.getTotal() + p.getPaidValue());
                            break;
                    }
                }
            }
        }
    }

    public ReportTemplateRow genarateRowBundleInward(List<BillLight> bills, ReportTemplateRow row) {
        List<BillItemDTO> billItems = new ArrayList<>();
        for (BillLight bl : bills) {
            List<BillItemDTO> inpatientBiDtos = billBean.fillBillItemDTOs(bl.getId());
            billItems.addAll(inpatientBiDtos);
        }
        for (BillItemDTO bi : billItems) {
            if (!(bi.getItemClass().equals("Investigation"))) {
                continue;
            }
            row.setInpatientTotal(row.getInpatientTotal() + bi.getBillNetTotal());
            row.setTotal(row.getTotal() + bi.getBillNetTotal());
            row.setDiscount(row.getDiscount() + bi.getDiscount());
            row.setServiceCharge(row.getServiceCharge() + bi.getMarginValue());
        }
        return row;
    }

    public ReportTemplateRow genarateRowBundleOther(List<BillLight> bills, ReportTemplateRow row) {
        
        for (BillLight bl : bills) {
            List<Payment> payments = billService.fetchBillPaymentsFromBillId(bl.getId());
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
                            row.setOnlineSettlementValue(row.getOnlineSettlementValue() + p.getPaidValue());
                            row.setTotal(row.getTotal() + p.getPaidValue());
                            break;
                        default:
                            row.setOtherIncomeValue(row.getOtherIncomeValue() + p.getPaidValue());
                            row.setTotal(row.getTotal() + p.getPaidValue());
                            break;
                    }
                }
            }
        }
        return row;
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
    
    public List<BillTypeAtomic> getFloatInconeBillTypeAtomics() {
        List<BillTypeAtomic> otherbillTypeAtomics = new ArrayList<>();
        otherbillTypeAtomics.add(BillTypeAtomic.FUND_TRANSFER_RECEIVED_BILL);
        otherbillTypeAtomics.add(BillTypeAtomic.FUND_TRANSFER_RECEIVED_BILL_CANCELLED);

        return otherbillTypeAtomics;
    }
    
    public List<BillTypeAtomic> getFloatTransferBillTypeAtomics() {
        List<BillTypeAtomic> otherbillTypeAtomics = new ArrayList<>();
        otherbillTypeAtomics.add(BillTypeAtomic.FUND_TRANSFER_BILL);
        otherbillTypeAtomics.add(BillTypeAtomic.FUND_TRANSFER_BILL_CANCELLED);

        return otherbillTypeAtomics;
    }

    private void calculateTotalsFromRows(List<? extends Object> rows, boolean isAddition) {
        double cashValue = 0.0;
        double cardValue = 0.0;
        double onlineSettlementValue = 0.0;
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
                onlineSettlementValue += rtr.getOnlineSettlementValue();
                creditValue += rtr.getCreditValue();
                inwardCreditValue += rtr.getInpatientTotal();
                otherValue += rtr.getOtherIncomeValue();
                totalValue += rtr.getTotal();
                discountValue += rtr.getDiscount();
                serviceChargeValue += rtr.getServiceCharge();
            } else if (row instanceof IncomeRow) {
                IncomeRow ir = (IncomeRow) row;
                cashValue += ir.getCashValue();
                cardValue += ir.getCardValue();
                onlineSettlementValue += ir.getOnlineSettlementValue();
                creditValue += ir.getCreditValue();
                inwardCreditValue += ir.getInpatientCreditValue();
                otherValue += ir.getOtherValue();
                totalValue += ir.getNetTotal();
                discountValue += ir.getDiscount();
                serviceChargeValue += ir.getServiceCharge();
            }
        }

        if (isAddition) {
            totalAdditionCashValue = cashValue;
            totalAdditionCardValue = cardValue;
            totalAdditionOnlineSettlementValue = onlineSettlementValue;
            totalAdditionCreditValue = creditValue;
            totalAdditionInwardCreditValue = inwardCreditValue;
            totalAdditionOtherValue = otherValue;
            totalAdditionTotalValue = totalValue;
            totalAdditionDiscountValue = discountValue;
            totalAdditionServiceChargeValue = serviceChargeValue;
        } else {
            totalDeductionCashValue = cashValue;
            totalDeductionCardValue = cardValue;
            totalDeductionOnlineSettlementValue = onlineSettlementValue;
            totalDeductionCreditValue = creditValue;
            totalDeductionInwardCreditValue = inwardCreditValue;
            totalDeductionOtherValue = otherValue;
            totalDeductionTotalValue = totalValue;
            totalDeductionDiscountValue = discountValue;
            totalDeductionServiceChargeValue = serviceChargeValue;
        }
    }

    public void initializeDeductionRows(IncomeRow row) {
        row.setCashValue(0.0);
        row.setCardValue(0.0);
        row.setOnlineSettlementValue(0.0);
        row.setCreditValue(0.0);
        row.setInpatientCreditValue(0.0);
        row.setOtherValue(0.0);
        row.setNetTotal(0.0);
        row.setDiscount(0.0);
        row.setServiceCharge(0.0);
    }
    
    public IncomeRow genarateDeductionRowBundleOther(List<BillLight> bills, IncomeRow row) {
        for (BillLight bl : bills) {
            List<Payment> payments = billService.fetchBillPaymentsFromBillId(bl.getId());
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
                            row.setOnlineSettlementValue(row.getOnlineSettlementValue() + p.getPaidValue());
                            row.setNetTotal(row.getNetTotal() + p.getPaidValue());
                            break;
                        default:
                            row.setOtherValue(row.getOtherValue() + p.getPaidValue());
                            row.setNetTotal(row.getNetTotal() + p.getPaidValue());
                            break;
                    }
                }
            }
        }
        return row;
    }

    public void reset9BData() {
        totalAdditionCashValue = 0.0;
        totalAdditionCardValue = 0.0;
        totalAdditionOnlineSettlementValue = 0.0;
        totalAdditionCreditValue = 0.0;
        totalAdditionInwardCreditValue = 0.0;
        totalAdditionOtherValue = 0.0;
        totalAdditionTotalValue = 0.0;
        totalAdditionDiscountValue = 0.0;
        totalAdditionServiceChargeValue = 0.0;

        totalDeductionCashValue = 0.0;
        totalDeductionCardValue = 0.0;
        totalDeductionOnlineSettlementValue = 0.0;
        totalDeductionCreditValue = 0.0;
        totalDeductionInwardCreditValue = 0.0;
        totalDeductionOtherValue = 0.0;
        totalDeductionTotalValue = 0.0;
        totalDeductionDiscountValue = 0.0;
        totalDeductionServiceChargeValue = 0.0;
        
        bundleReport = new ReportTemplateRowBundle();
        bundle = new IncomeBundle();
    }

    // </editor-fold>
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public Long getRowsPerPageForScreen() {
        return rowsPerPageForScreen;
    }

    public void setRowsPerPageForScreen(Long rowsPerPageForScreen) {
        this.rowsPerPageForScreen = rowsPerPageForScreen;
    }

    public Long getRowsPerPageForPrinting() {
        return rowsPerPageForPrinting;
    }

    public void setRowsPerPageForPrinting(Long rowsPerPageForPrinting) {
        this.rowsPerPageForPrinting = rowsPerPageForPrinting;
    }

    public String getVisitType() {
        return visitType;
    }

    public void setVisitType(String visitType) {
        this.visitType = visitType;
    }

    public String getMethodType() {
        return methodType;
    }

    public void setMethodType(String methodType) {
        this.methodType = methodType;
    }

    public String getSearchType() {
        return searchType;
    }

    public void setSearchType(String searchType) {
        this.searchType = searchType;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public String getFontSizeForPrinting() {
        return fontSizeForPrinting;
    }

    public void setFontSizeForPrinting(String fontSizeForPrinting) {
        this.fontSizeForPrinting = fontSizeForPrinting;
    }

    public String getFontSizeForScreen() {
        return fontSizeForScreen;
    }

    public void setFontSizeForScreen(String fontSizeForScreen) {
        this.fontSizeForScreen = fontSizeForScreen;
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
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public BillClassType getBillClassType() {
        return billClassType;
    }

    public void setBillClassType(BillClassType billClassType) {
        this.billClassType = billClassType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
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

    public List<PaymentMethod> getPaymentMethods() {
        return paymentMethods;
    }

    public void setPaymentMethods(List<PaymentMethod> paymentMethods) {
        this.paymentMethods = paymentMethods;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Staff getCurrentStaff() {
        return currentStaff;
    }

    public void setCurrentStaff(Staff currentStaff) {
        this.currentStaff = currentStaff;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public Institution getDealer() {
        return dealer;
    }

    public void setDealer(Institution dealer) {
        this.dealer = dealer;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Institution getToSite() {
        return toSite;
    }

    public void setToSite(Institution toSite) {
        this.toSite = toSite;
    }

    public Institution getFromInstitution() {
        return fromInstitution;
    }

    public void setFromInstitution(Institution fromInstitution) {
        this.fromInstitution = fromInstitution;
    }

    public Institution getToInstitution() {
        return toInstitution;
    }

    public void setToInstitution(Institution toInstitution) {
        this.toInstitution = toInstitution;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Department getFromDepartment() {
        return fromDepartment;
    }

    public void setFromDepartment(Department fromDepartment) {
        this.fromDepartment = fromDepartment;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public SearchKeyword getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public ReportKeyWord getReportKeyWord() {
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public IncomeBundle getBundle() {
        return bundle;
    }

    public void setBundle(IncomeBundle bundle) {
        this.bundle = bundle;
    }

    public ReportTemplateRowBundle getBundleReport() {
        return bundleReport;
    }

    public void setBundleReport(ReportTemplateRowBundle bundleReport) {
        this.bundleReport = bundleReport;
    }

    public DailyStockBalanceReport getDailyStockBalanceReport() {
        return dailyStockBalanceReport;
    }

    public void setDailyStockBalanceReport(DailyStockBalanceReport dailyStockBalanceReport) {
        this.dailyStockBalanceReport = dailyStockBalanceReport;
    }

    public StreamedContent getDownloadingExcel() {
        return downloadingExcel;
    }

    public void setDownloadingExcel(StreamedContent downloadingExcel) {
        this.downloadingExcel = downloadingExcel;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public int getMaxResult() {
        return maxResult;
    }

    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
    }

    public String getViewTemplate() {
        return viewTemplate;
    }

    public void setViewTemplate(String viewTemplate) {
        this.viewTemplate = viewTemplate;
    }

    public double getTotalHospitalFee() {
        return totalHospitalFee;
    }

    public void setTotalHospitalFee(double totalHospitalFee) {
        this.totalHospitalFee = totalHospitalFee;
    }

    public double getTotalReagentFee() {
        return totalReagentFee;
    }

    public void setTotalReagentFee(double totalReagentFee) {
        this.totalReagentFee = totalReagentFee;
    }

    public double getTotalOtherFee() {
        return totalOtherFee;
    }

    public void setTotalOtherFee(double totalOtherFee) {
        this.totalOtherFee = totalOtherFee;
    }

    public double getTotalNetTotal() {
        return totalNetTotal;
    }

    public void setTotalNetTotal(double totalNetTotal) {
        this.totalNetTotal = totalNetTotal;
    }

    public double getTotalDiscount() {
        return totalDiscount;
    }

    public void setTotalDiscount(double totalDiscount) {
        this.totalDiscount = totalDiscount;
    }

    public double getTotalServiceCharge() {
        return totalServiceCharge;
    }

    public void setTotalServiceCharge(double totalServiceCharge) {
        this.totalServiceCharge = totalServiceCharge;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    public void setActiveIndex(int activeIndex) {
        this.activeIndex = activeIndex;
    }

    public List<TestWiseCountReport> getTestWiseCounts() {
        return testWiseCounts;
    }

    public void setTestWiseCounts(List<TestWiseCountReport> testWiseCounts) {
        this.testWiseCounts = testWiseCounts;
    }

    public List<TestCountDTO> getTestWiseCountDtos() {
        return testWiseCountDtos;
    }

    public void setTestWiseCountDtos(List<TestCountDTO> testWiseCountDtos) {
        this.testWiseCountDtos = testWiseCountDtos;
    }

    public boolean isTestWiseCountOptimizedEnabled() {
        return configController.getBooleanValueByKey("Test Wise Count Report - Optimized Method", false);
    }

    public boolean isTestWiseCountLegacyEnabled() {
        return configController.getBooleanValueByKey("Test Wise Count Report - Legacy Method", true);
    }

    public double getTotalNetHosFee() {
        return totalNetHosFee;
    }

    public void setTotalNetHosFee(double totalNetHosFee) {
        this.totalNetHosFee = totalNetHosFee;
    }

    public double getTotalAdditionalFee() {
        return totalAdditionalFee;
    }

    public void setTotalAdditionalFee(double totalAdditionalFee) {
        this.totalAdditionalFee = totalAdditionalFee;
    }

    public double getTotalCCFee() {
        return totalCCFee;
    }

    public void setTotalCCFee(double totalCCFee) {
        this.totalCCFee = totalCCFee;
    }

    public double getTotalProFee() {
        return totalProFee;
    }

    public void setTotalProFee(double totalProFee) {
        this.totalProFee = totalProFee;
    }

    public double getTotalHosFee() {
        return totalHosFee;
    }

    public void setTotalHosFee(double totalHosFee) {
        this.totalHosFee = totalHosFee;
    }

    public double getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(double totalCount) {
        this.totalCount = totalCount;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public List<LabIncomeReportDTO> getLabIncomeReportDtos() {
        return labIncomeReportDtos;
    }

    public void setLabIncomeReportDtos(List<LabIncomeReportDTO> labIncomeReportDtos) {
        this.labIncomeReportDtos = labIncomeReportDtos;
    }

    public String getStrActiveIndex() {
        return strActiveIndex;
    }

    public void setStrActiveIndex(String strActiveIndex) {
        this.strActiveIndex = strActiveIndex;
    }

    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
    }

    public List<PatientInvestigationDTO> getItemList() {
        return itemList;
    }

    public void setItemList(List<PatientInvestigationDTO> itemList) {
        this.itemList = itemList;
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

    public double getTotalAdditionOnlineSettlementValue() {
        return totalAdditionOnlineSettlementValue;
    }

    public void setTotalAdditionOnlineSettlementValue(double totalAdditionOnlineSettlementValue) {
        this.totalAdditionOnlineSettlementValue = totalAdditionOnlineSettlementValue;
    }

    public double getTotalDeductionOnlineSettlementValue() {
        return totalDeductionOnlineSettlementValue;
    }

    public void setTotalDeductionOnlineSettlementValue(double totalDeductionOnlineSettlementValue) {
        this.totalDeductionOnlineSettlementValue = totalDeductionOnlineSettlementValue;
    }
    
    // </editor-fold>
}
