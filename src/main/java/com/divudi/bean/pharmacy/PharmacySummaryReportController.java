package com.divudi.bean.pharmacy;

// <editor-fold defaultstate="collapsed" desc="Import Statements">
import com.divudi.bean.common.*;
import com.divudi.bean.cashTransaction.CashBookEntryController;
import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.DrawerEntryController;
import com.divudi.bean.channel.ChannelSearchController;
import com.divudi.bean.channel.analytics.ReportTemplateController;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.data.hr.ReportKeyWord;

import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.Staff;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.StockFacade;
import com.divudi.bean.opd.OpdBillController;
import com.divudi.data.BillClassType;

import com.divudi.data.BillTypeAtomic;
import com.divudi.data.IncomeBundle;
import com.divudi.entity.Bill;
import com.divudi.entity.Category;
import com.divudi.entity.WebUser;
import com.divudi.facade.DrawerFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.java.CommonFunctions;
import com.divudi.service.BillService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
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

// <editor-fold defaultstate="collapsed" desc="EJBs">
    private CommonFunctions commonFunctions;
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
    BillService billService;

// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="Controllers">
    @Inject
    private BillBeanController billBean;
    @Inject
    private SessionController sessionController;
    @Inject
    private TransferController transferController;
    @Inject
    private CommonController commonController;
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

    // Date range
    private Date fromDate;
    private Date toDate;

    // Enum and category types
    private Category category;
    private BillType billType;
    private BillTypeAtomic billTypeAtomic;
    private BillClassType billClassType;
    private PaymentMethod paymentMethod;

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

    private StreamedContent downloadingExcel;
    private UploadedFile file;

    // Numeric variables
    private int maxResult = 50;

// </editor-fold>
// <editor-fold defaultstate="collapsed" desc="Navigators">
    public String navigateToPharmacyIncomeReport() {
        return "/pharmacy/reports/pharmacy_income_report?faces-redirect=true";
    }

    public String navigateToDailyStockBalanceReport() {
        return "/pharmacy/reports/daily_stock_balance_report?faces-redirect=true";
    }
// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="Functions">

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

    public void processPharmacyIncomeReport(){
        List<BillTypeAtomic> billTypeAtomics = new ArrayList<>();
        List<Bill> bills = billService.fetchBills(fromDate,toDate,institution,site,department,webUser, billTypeAtomics);
        bundle = new IncomeBundle(bills);
        bundle.generatePaymentDetailsForBills();
    }
    
// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="Constructors">
    public PharmacySummaryReportController() {
    }
// </editor-fold>  
// <editor-fold defaultstate="collapsed" desc="Getters and Setters">

     /**
     * @return the commonFunctions
     */
    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    /**
     * @param commonFunctions the commonFunctions to set
     */
    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

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
     * @return the commonController
     */
    public CommonController getCommonController() {
        return commonController;
    }

    /**
     * @param commonController the commonController to set
     */
    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
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
     * @param configOptionApplicationController the configOptionApplicationController to set
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
}
