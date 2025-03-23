package com.divudi.bean.lab;

import com.divudi.data.BillClassType;
import com.divudi.data.BillType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.IncomeBundle;
import com.divudi.data.IncomeRow;
import com.divudi.data.PaymentMethod;
import com.divudi.data.ReportTemplateRowBundle;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.data.pharmacy.DailyStockBalanceReport;
import com.divudi.entity.Bill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.java.CommonFunctions;
import com.divudi.service.BillService;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.inject.Named;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author H.K. Damith Deshan
 */
@Named
@SessionScoped
public class LaboratoryReportController implements Serializable {

    public LaboratoryReportController() {
    }
    
    private CommonFunctions commonFunctions;

    // <editor-fold defaultstate="collapsed" desc="Controllers">
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private BillService billService;

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

    // Numeric variables
    private int maxResult = 50;
    
    private String viewTemplate;

// </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Navigators">
    
    public String navigateToLaboratoryIncomeReportFromLabAnalytics() {
        resetAllFiltersExceptDateRange();
        viewTemplate ="/reportLab/lab_summeries_index.xhtml";
        return "/reportLab/laboratory_income_report?faces-redirect=true;";
    }
    
    public String navigateToLaboratoryIncomeReportFromReport() {
        resetAllFiltersExceptDateRange();
        viewTemplate ="/reports/index.xhtml";
        return "/reportLab/laboratory_income_report?faces-redirect=true;";
    }
    
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="Functions">
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
    }

    public void processLaboratoryIncomeReport() {
        System.out.println("processLaboratoryIncomeReport");
        List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
        //Add All OPD BillTypes
//        billTypeAtomics.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
//        billTypeAtomics.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
//        billTypeAtomics.add(BillTypeAtomic.OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
//        billTypeAtomics.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_REFUND);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        billTypeAtomics.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        //Add All Inward BillTypes
//        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
//        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
        billTypeAtomics.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);
        //Add All Package BillTypes
//        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
//        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
//        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
//        billTypeAtomics.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
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
    }

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
            fromDate = commonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = commonFunctions.getEndOfDay(new Date());
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

    // </editor-fold>

    public String getViewTemplate() {
        return viewTemplate;
    }

    public void setViewTemplate(String viewTemplate) {
        this.viewTemplate = viewTemplate;
    }
}
