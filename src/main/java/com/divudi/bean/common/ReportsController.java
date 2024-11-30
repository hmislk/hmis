package com.divudi.bean.common;

import com.divudi.bean.cashTransaction.CashBookEntryController;
import com.divudi.bean.cashTransaction.DrawerController;
import com.divudi.bean.cashTransaction.DrawerEntryController;
import com.divudi.bean.channel.ChannelSearchController;
import com.divudi.bean.channel.analytics.ReportTemplateController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.opd.OpdBillController;
import com.divudi.bean.pharmacy.PharmacyBillSearch;
import com.divudi.bean.pharmacy.PharmacyPreSettleController;
import com.divudi.bean.pharmacy.PharmacySaleBhtController;
import com.divudi.data.*;
import com.divudi.data.analytics.ReportTemplateType;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.*;
import com.divudi.entity.cashTransaction.CashBookEntry;
import com.divudi.entity.cashTransaction.Drawer;
import com.divudi.entity.inward.Admission;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.inward.RoomCategory;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientReport;
import com.divudi.facade.*;
import com.divudi.java.CommonFunctions;
import com.divudi.light.common.BillLight;
import com.divudi.light.common.BillSummaryRow;
import com.divudi.service.BillService;
import com.divudi.service.PatientInvestigationService;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import java.io.*;
import java.util.*;

/**
 * @author safrin
 */
@Named
@SessionScoped
public class ReportsController implements Serializable {

    private static final long serialVersionUID = 1L;
    /**
     * EJBs
     */
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
    private PatientInvestigationFacade patientInvestigationFacade;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    BillSessionFacade billSessionFacade;
    @EJB
    StockFacade stockFacade;
    @EJB
    PatientReportFacade patientReportFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    TokenFacade tokenFacade;
    @EJB
    private DrawerFacade drawerFacade;
    @EJB
    BillService billService;
    @EJB
    PatientInvestigationService patientInvestigationService;
    @EJB
    PatientEncounterFacade peFacade;
    List<PatientEncounter> patientEncounters;

    /**
     * Inject
     */
    @Inject
    private BillBeanController billBean;
    @Inject
    private SessionController sessionController;
    @Inject
    TransferController transferController;
    @Inject
    private CommonController commonController;
    @Inject
    PharmacySaleBhtController pharmacySaleBhtController;
    @Inject
    SmsController smsController;
    @Inject
    AuditEventApplicationController auditEventApplicationController;
    @Inject
    WebUserController webUserController;
    @Inject
    OpdPreSettleController opdPreSettleController;
    @Inject
    PharmacyPreSettleController pharmacyPreSettleController;
    @Inject
    TokenController tokenController;
    @Inject
    private DepartmentController departmentController;
    @Inject
    BillSearch billSearch;
    @Inject
    PharmacyBillSearch pharmacyBillSearch;
    @Inject
    OpdBillController opdBillController;
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    ChannelSearchController channelSearchController;
    @Inject
    ReportTemplateController reportTemplateController;
    @Inject
    CashBookEntryController cashBookEntryController;
    @Inject
    ExcelController excelController;
    @Inject
    PdfController pdfController;
    @Inject
    DrawerEntryController drawerEntryController;
    @Inject
    DrawerController drawerController;
    @Inject
    EnumController enumController;

    /**
     * Properties
     */
    private String visitType;
    private String methodType;

    private Category category;
    private ReportTemplateType reportTemplateType;
    private SearchKeyword searchKeyword;
    private Date fromDate;
    private Date toDate;
    private Long startBillId;
    private Long endBillId;
    private WebUser webUser;
    private String backLink;
    private int maxResult = 50;
    private BillType billType;
    private PaymentMethod paymentMethod;
    private List<PaymentMethod> paymentMethods;
    private List<Bill> bills;
    private List<Payment> payments;
    private List<BillLight> billLights;
    private List<BillSummaryRow> billSummaryRows;
    private List<Bill> selectedBills;
    private List<Bill> aceptPaymentBills;
    private List<BillFee> billFees;
    private List<BillFee> billFeesDone;
    private List<BillItem> billItems;
    private List<PatientInvestigation> patientInvestigations;
    private List<PatientReport> patientReports;
    private List<PatientInvestigation> patientInvestigationsSigle;
    private BillTypeAtomic billTypeAtomic;
    private BillClassType billClassType;
    private Institution collectingCentre;
    private RoomCategory roomCategory;

    private StreamedContent downloadingExcel;

    BillSummaryRow billSummaryRow;
    Bill cancellingIssueBill;
    Bill bill;
    Speciality speciality;
    PatientEncounter patientEncounter;
    Staff staff;
    Item item;
    double dueTotal;
    double doneTotal;
    double netTotal;
    private double totalBillCount;
    private double grossTotal;
    private double discount;
    ServiceSession selectedServiceSession;
    Staff currentStaff;
    private String mrnNo;
    List<BillItem> billItem;
    List<PatientInvestigation> userPatientInvestigations;
    double netTotalValue;

    String menuBarSearchText;
    String smsText;
    String uniqueSmsText;
    boolean channelingPanelVisible;
    boolean pharmacyPanelVisible;
    boolean opdPanelVisible;
    boolean inwardPanelVisible;
    boolean labPanelVisile;
    boolean patientPanelVisible;
    ReportKeyWord reportKeyWord;

    List<Bill> channellingBills;
    List<BillSession> billSessions;
    List<Bill> opdBills;
    List<Bill> pharmacyBills;
    List<Admission> admissions;
    List<PatientInvestigation> pis;
    List<Patient> patients;
    List<String> telephoneNumbers;
    List<String> selectedTelephoneNumbers;

    BillSession selectedBillSession;
    UploadedFile file;
    private Institution creditCompany;

    private Institution otherInstitution;

    private Institution institution;
    private Department department;
    List<Bill> prescreptionBills;
    private Department fromDepartment;
    private Department toDepartment;
    private Institution fromInstitution;
    private Institution toInstitution;
    private int manageListIndex;
    private Patient patient;
    private Institution dealer;
    private List<Bill> grnBills;
    Bill currentBill;
    private Long currentBillId;
    private Bill preBill;
    boolean billPreview;
    private Long barcodeIdLong;
    private Date maxDate;

    private double cashTotal;
    private double cardTotal;
    private double chequeTotal;
    private double slipTotal;
    private double totalOfOtherPayments;
    private double billCount;
    private Token token;
    private int managePaymentIndex = -1;

    private double hosTotal;
    private double staffTotal;
    private double discountTotal;
    private double amountTotal;
    double totalPaying;

    private boolean duplicateBillView;

    private ReportTemplateRowBundle bundle;
    private ReportTemplateRowBundle bundleBillItems;

    private List<CashBookEntry> cashBookEntries;
    private Institution site;
    private Institution toSite;
    private List<Drawer> drawerList;
    private Drawer selectedDrawer;
    private int opdAnalyticsIndex;
    private AdmissionType admissionType;
    private List<AgentHistory> agentHistories;

    private String searchType;
    private String reportType;
    private String serviceGroup;
    private boolean withProfessionalFee;

    private Drawer drawer;

    private Department serviceDepartment;
    private Department billedDepartment;
    private List<Department> departments;

    double total;
    double paid;
    double creditPaid;
    double creditUsed;
    double calTotal;
    double totalVat;
    double totalVatCalculatedValue;

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public UploadedFile getFile() {
        return file;
    }

    public void setFile(UploadedFile file) {
        this.file = file;
    }

    public PatientReportFacade getPatientReportFacade() {
        return patientReportFacade;
    }

    public void setPatientReportFacade(PatientReportFacade patientReportFacade) {
        this.patientReportFacade = patientReportFacade;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
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

    public String getServiceGroup() {
        return serviceGroup;
    }

    public void setServiceGroup(String serviceGroup) {
        this.serviceGroup = serviceGroup;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public List<PatientReport> getPatientReports() {
        return patientReports;
    }

    public void setPatientReports(List<PatientReport> patientReports) {
        this.patientReports = patientReports;
    }

    public Institution getOtherInstitution() {
        return otherInstitution;
    }

    public void setOtherInstitution(Institution otherInstitution) {
        this.otherInstitution = otherInstitution;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public int getManageListIndex() {
        return manageListIndex;
    }

    public void setManageListIndex(int manageListIndex) {
        this.manageListIndex = manageListIndex;
    }

    public List<BillLight> getBillLights() {
        return billLights;
    }

    public void setBillLights(List<BillLight> billLights) {
        this.billLights = billLights;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
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

    public Institution getDealer() {
        return dealer;
    }

    public void setDealer(Institution dealer) {
        this.dealer = dealer;
    }

    public List<Bill> getGrnBills() {
        return grnBills;
    }

    public void setGrnBills(List<Bill> grnBills) {
        this.grnBills = grnBills;
    }

    public Long getCurrentBillId() {
        return currentBillId;
    }

    public void setCurrentBillId(Long currentBillId) {
        this.currentBillId = currentBillId;
    }

    public Long getBarcodeIdLong() {
        return barcodeIdLong;
    }

    public void setBarcodeIdLong(Long barcodeIdLong) {
        this.barcodeIdLong = barcodeIdLong;
    }

    public List<Payment> getPayments() {
        return payments;
    }

    public void setPayments(List<Payment> payments) {
        this.payments = payments;
    }

    public List<BillSummaryRow> getBillSummaryRows() {
        return billSummaryRows;
    }

    public void setBillSummaryRows(List<BillSummaryRow> billSummaryRows) {
        this.billSummaryRows = billSummaryRows;
    }

    public double getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public double getDiscount() {
        return discount;
    }

    public void setDiscount(double discount) {
        this.discount = discount;
    }

    public DepartmentController getDepartmentController() {
        return departmentController;
    }

    public void setDepartmentController(DepartmentController departmentController) {
        this.departmentController = departmentController;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public double getTotalBillCount() {
        return totalBillCount;
    }

    public void setTotalBillCount(double totalBillCount) {
        this.totalBillCount = totalBillCount;
    }

    public Long getStartBillId() {
        return startBillId;
    }

    public void setStartBillId(Long startBillId) {
        this.startBillId = startBillId;
    }

    public Long getEndBillId() {
        return endBillId;
    }

    public void setEndBillId(Long endBillId) {
        this.endBillId = endBillId;
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

    public BillTypeAtomic getBillTypeAtomic() {
        return billTypeAtomic;
    }

    public void setBillTypeAtomic(BillTypeAtomic billTypeAtomic) {
        this.billTypeAtomic = billTypeAtomic;
    }

    public Date getMaxDate() {
        maxDate = commonFunctions.getEndOfDay(new Date());
        return maxDate;
    }

    public void setMaxDate(Date maxDate) {
        this.maxDate = maxDate;
    }

    public double getCashTotal() {
        return cashTotal;
    }

    public void setCashTotal(double cashTotal) {
        this.cashTotal = cashTotal;
    }

    public double getCardTotal() {
        return cardTotal;
    }

    public void setCardTotal(double cardTotal) {
        this.cardTotal = cardTotal;
    }

    public double getChequeTotal() {
        return chequeTotal;
    }

    public void setChequeTotal(double chequeTotal) {
        this.chequeTotal = chequeTotal;
    }

    public double getSlipTotal() {
        return slipTotal;
    }

    public void setSlipTotal(double slipTotal) {
        this.slipTotal = slipTotal;
    }

    public double getTotalOfOtherPayments() {
        return totalOfOtherPayments;
    }

    public void setTotalOfOtherPayments(double totalOfOtherPayments) {
        this.totalOfOtherPayments = totalOfOtherPayments;
    }

    public double getBillCount() {
        return billCount;
    }

    public void setBillCount(double billCount) {
        this.billCount = billCount;
    }

    public Token getToken() {
        return token;
    }

    public void setToken(Token token) {
        this.token = token;
    }

    public boolean isDuplicateBillView() {
        return duplicateBillView;
    }

    public void setDuplicateBillView(boolean duplicateBillView) {
        this.duplicateBillView = duplicateBillView;
    }

    public int getManagePaymentIndex() {
        return managePaymentIndex;
    }

    public void setManagePaymentIndex(int managePaymentIndex) {
        this.managePaymentIndex = managePaymentIndex;
    }

    public ReportTemplateType getReportTemplateType() {
        return reportTemplateType;
    }

    public void setReportTemplateType(ReportTemplateType reportTemplateType) {
        this.reportTemplateType = reportTemplateType;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<CashBookEntry> getCashBookEntries() {
        return cashBookEntries;
    }

    public void setCashBookEntries(List<CashBookEntry> cashBookEntries) {
        this.cashBookEntries = cashBookEntries;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public ReportTemplateRowBundle getBundle() {
        return bundle;
    }

    public void setBundle(ReportTemplateRowBundle bundle) {
        this.bundle = bundle;
    }

    public String getBackLink() {
        return backLink;
    }

    public void setBackLink(String backLink) {
        this.backLink = backLink;
    }

    public ReportTemplateRowBundle getBundleBillItems() {
        return bundleBillItems;
    }

    public void setBundleBillItems(ReportTemplateRowBundle bundleBillItems) {
        this.bundleBillItems = bundleBillItems;
    }

    public int getOpdAnalyticsIndex() {
        return opdAnalyticsIndex;
    }

    public void setOpdAnalyticsIndex(int opdAnalyticsIndex) {
        this.opdAnalyticsIndex = opdAnalyticsIndex;
    }

    public List<Drawer> getDrawerList() {
        return drawerList;
    }

    public void setDrawerList(List<Drawer> drawerList) {
        this.drawerList = drawerList;
    }

    public DrawerFacade getDrawerFacade() {
        return drawerFacade;
    }

    public void setDrawerFacade(DrawerFacade drawerFacade) {
        this.drawerFacade = drawerFacade;
    }

    public Drawer getSelectedDrawer() {
        return selectedDrawer;
    }

    public void setSelectedDrawer(Drawer selectedDrawer) {
        this.selectedDrawer = selectedDrawer;
    }

    public BillClassType getBillClassType() {
        return billClassType;
    }

    public void setBillClassType(BillClassType billClassType) {
        this.billClassType = billClassType;
    }

    public StreamedContent getDownloadingExcel() {
        return downloadingExcel;
    }

    public void setDownloadingExcel(StreamedContent downloadingExcel) {
        this.downloadingExcel = downloadingExcel;
    }

    public double getHosTotal() {
        return hosTotal;
    }

    public void setHosTotal(double hosTotal) {
        this.hosTotal = hosTotal;
    }

    public double getStaffTotal() {
        return staffTotal;
    }

    public void setStaffTotal(double staffTotal) {
        this.staffTotal = staffTotal;
    }

    public double getDiscountTotal() {
        return discountTotal;
    }

    public void setDiscountTotal(double discountTotal) {
        this.discountTotal = discountTotal;
    }

    public double getAmountTotal() {
        return amountTotal;
    }

    public void setAmountTotal(double amountTotal) {
        this.amountTotal = amountTotal;
    }

    public Institution getToSite() {
        return toSite;
    }

    public void setToSite(Institution toSite) {
        this.toSite = toSite;
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

    public String getMrnNo() {
        return mrnNo;
    }

    public void setMrnNo(String mrnNo) {
        this.mrnNo = mrnNo;
    }

    public boolean isWithProfessionalFee() {
        return withProfessionalFee;
    }

    public void setWithProfessionalFee(boolean withProfessionalFee) {
        this.withProfessionalFee = withProfessionalFee;
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

    public Drawer getDrawer() {
        return drawer;
    }

    public void setDrawer(Drawer drawer) {
        this.drawer = drawer;
    }

    public Department getServiceDepartment() {
        return serviceDepartment;
    }

    public void setServiceDepartment(Department serviceDepartment) {
        this.serviceDepartment = serviceDepartment;
    }

    public Department getBilledDepartment() {
        return billedDepartment;
    }

    public void setBilledDepartment(Department billedDepartment) {
        this.billedDepartment = billedDepartment;
    }

    public ReportsController() {
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

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = commonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public List<BillFee> getBillFees() {
        return billFees;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public List<PatientInvestigation> getPatientInvestigations() {
        return patientInvestigations;
    }

    public void setPatientInvestigations(List<PatientInvestigation> patientInvestigations) {
        this.patientInvestigations = patientInvestigations;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public int getMaxResult() {

        return maxResult;
    }

    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
    }

    public List<Bill> getSelectedBills() {
        if (selectedBills == null) {
            selectedBills = new ArrayList<>();
        }
        return selectedBills;
    }

    public void setSelectedBills(List<Bill> selectedBills) {
        this.selectedBills = selectedBills;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public TransferController getTransferController() {
        return transferController;
    }

    public void setTransferController(TransferController transferController) {
        this.transferController = transferController;
    }

    public void setPatientInvestigationsSigle(List<PatientInvestigation> patientInvestigationsSigle) {
        this.patientInvestigationsSigle = patientInvestigationsSigle;
    }

    public Bill getCancellingIssueBill() {
        return cancellingIssueBill;
    }

    public void setCancellingIssueBill(Bill cancellingIssueBill) {
        this.cancellingIssueBill = cancellingIssueBill;
    }

    public double getNetTotalValue() {
        return netTotalValue;
    }

    public void setNetTotalValue(double netTotalValue) {
        this.netTotalValue = netTotalValue;
    }

    public List<BillFee> getBillFeesDone() {
        return billFeesDone;
    }

    public void setBillFeesDone(List<BillFee> billFeesDone) {
        this.billFeesDone = billFeesDone;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public double getDueTotal() {
        return dueTotal;
    }

    public void setDueTotal(double dueTotal) {
        this.dueTotal = dueTotal;
    }

    public double getDoneTotal() {
        return doneTotal;
    }

    public void setDoneTotal(double doneTotal) {
        this.doneTotal = doneTotal;
    }

    public double getTotalPaying() {
        return totalPaying;
    }

    public void setTotalPaying(double totalPaying) {
        this.totalPaying = totalPaying;
    }

    public List<AgentHistory> getAgentHistories() {
        return agentHistories;
    }

    public void setAgentHistories(List<AgentHistory> agentHistories) {
        this.agentHistories = agentHistories;
    }

    public List<PatientInvestigation> getUserPatientInvestigations() {
        return userPatientInvestigations;
    }

    public void setUserPatientInvestigations(List<PatientInvestigation> userPatientInvestigations) {
        this.userPatientInvestigations = userPatientInvestigations;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public List<Bill> getAceptPaymentBills() {
        if (aceptPaymentBills == null) {
            aceptPaymentBills = new ArrayList<>();
        }
        return aceptPaymentBills;
    }

    public void setAceptPaymentBills(List<Bill> aceptPaymentBills) {
        this.aceptPaymentBills = aceptPaymentBills;
    }

    public String getMenuBarSearchText() {
        return menuBarSearchText;
    }

    public void setMenuBarSearchText(String menuBarSearchText) {
        this.menuBarSearchText = menuBarSearchText;
    }

    public boolean isChannelingPanelVisible() {
        return channelingPanelVisible;
    }

    public void setChannelingPanelVisible(boolean channelingPanelVisible) {
        this.channelingPanelVisible = channelingPanelVisible;
    }

    public boolean isPharmacyPanelVisible() {
        return pharmacyPanelVisible;
    }

    public void setPharmacyPanelVisible(boolean pharmacyPanelVisible) {
        this.pharmacyPanelVisible = pharmacyPanelVisible;
    }

    public boolean isOpdPanelVisible() {
        return opdPanelVisible;
    }

    public void setOpdPanelVisible(boolean opdPanelVisible) {
        this.opdPanelVisible = opdPanelVisible;
    }

    public boolean isInwardPanelVisible() {
        return inwardPanelVisible;
    }

    public void setInwardPanelVisible(boolean inwardPanelVisible) {
        this.inwardPanelVisible = inwardPanelVisible;
    }

    public boolean isLabPanelVisile() {
        return labPanelVisile;
    }

    public void setLabPanelVisile(boolean labPanelVisile) {
        this.labPanelVisile = labPanelVisile;
    }

    public boolean isPatientPanelVisible() {
        return patientPanelVisible;
    }

    public void setPatientPanelVisible(boolean patientPanelVisible) {
        this.patientPanelVisible = patientPanelVisible;
    }

    public List<Bill> getChannellingBills() {
        return channellingBills;
    }

    public void setChannellingBills(List<Bill> channellingBills) {
        this.channellingBills = channellingBills;
    }

    public List<Bill> getOpdBills() {
        return opdBills;
    }

    public void setOpdBills(List<Bill> opdBills) {
        this.opdBills = opdBills;
    }

    public List<Bill> getPharmacyBills() {
        return pharmacyBills;
    }

    public void setPharmacyBills(List<Bill> pharmacyBills) {
        this.pharmacyBills = pharmacyBills;
    }

    public List<Admission> getAdmissions() {
        return admissions;
    }

    public void setAdmissions(List<Admission> admissions) {
        this.admissions = admissions;
    }

    public List<PatientInvestigation> getPis() {
        return pis;
    }

    public void setPis(List<PatientInvestigation> pis) {
        this.pis = pis;
    }

    public List<Patient> getPatients() {
        return patients;
    }

    public void setPatients(List<Patient> patients) {
        this.patients = patients;
    }

    public List<BillSession> getBillSessions() {
        return billSessions;
    }

    public void setBillSessions(List<BillSession> billSessions) {
        this.billSessions = billSessions;
    }

    public BillSession getSelectedBillSession() {
        return selectedBillSession;
    }

    public void setSelectedBillSession(BillSession selectedBillSession) {
        this.selectedBillSession = selectedBillSession;
    }

    public List<String> getTelephoneNumbers() {
        return telephoneNumbers;
    }

    public void setTelephoneNumbers(List<String> telephoneNumbers) {
        this.telephoneNumbers = telephoneNumbers;
    }

    public List<String> getSelectedTelephoneNumbers() {
        return selectedTelephoneNumbers;
    }

    public void setSelectedTelephoneNumbers(List<String> selectedTelephoneNumbers) {
        this.selectedTelephoneNumbers = selectedTelephoneNumbers;
    }

    public Institution getCollectingCentre() {
        return collectingCentre;
    }

    public void setCollectingCentre(Institution collectingCentre) {
        this.collectingCentre = collectingCentre;
    }

    boolean paginator = true;
    int rows = 20;

    public boolean isPaginator() {
        return paginator;
    }

    public void setPaginator(boolean paginator) {
        this.paginator = paginator;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public String getSmsText() {
        return smsText;
    }

    public void setSmsText(String smsText) {
        this.smsText = smsText;
    }

    public String getUniqueSmsText() {
        return uniqueSmsText;
    }

    public void setUniqueSmsText(String uniqueSmsText) {
        this.uniqueSmsText = uniqueSmsText;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public PatientEncounterFacade getPeFacade() {
        return peFacade;
    }

    public void setPeFacade(PatientEncounterFacade peFacade) {
        this.peFacade = peFacade;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public List<PatientEncounter> getPatientEncounters() {
        return patientEncounters;
    }

    public void setPatientEncounters(List<PatientEncounter> patientEncounters) {
        this.patientEncounters = patientEncounters;
    }

    public double getPaid() {
        return paid;
    }

    public void setPaid(double paid) {
        this.paid = paid;
    }

    public RoomCategory getRoomCategory() {
        return roomCategory;
    }

    public void setRoomCategory(RoomCategory roomCategory) {
        this.roomCategory = roomCategory;
    }

    public double getCreditPaid() {
        return creditPaid;
    }

    public void setCreditPaid(double creditPaid) {
        this.creditPaid = creditPaid;
    }

    public void generatePackageReport() {
        System.out.println("generatePackageReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();

        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);

        System.out.println("bill items");

        bundle.setName("Package Bill Items");
        bundle.setBundleType("billItemList");

        bundle = generateBillItems(opdBts);
    }

    private ReportTemplateRowBundle generateBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (visitType != null) {
            if (visitType.equalsIgnoreCase("IP") || visitType.equalsIgnoreCase("OP")) {
                jpql += "AND bill.ipOpOrCc = :type ";
                parameters.put("type", visitType);
            }
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().isEmpty()) {
            jpql += "AND ((bill.billPackege.name) like :itemName ) ";
            parameters.put("itemName", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().isEmpty()) {
            jpql += "AND ((bill.deptId) like :billNo ) ";
            parameters.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (item != null) {
            jpql += "AND billItem.item = :item ";
            parameters.put("item", item);
        }

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateInvoiceAndReportSerialWiseReport() {
        System.out.println("generateInvoiceAndReportSerialWiseReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<PaymentMethod> paymentMethods = new ArrayList<>();
        if (paymentMethod != null) {
            paymentMethods.add(paymentMethod);
        } else {
            addAllPaymentMethods(paymentMethods);
        }

        List<BillTypeAtomic> opdBts = new ArrayList<>();

        opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        opdBts.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
        opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
        opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);

        bundle.setName("Invoice and Receipt Report Bill Items");
        bundle.setBundleType("billItemList");
        bundle = generateBillItems(opdBts, paymentMethods);
        bundle.calculateTotalByBillItems();
    }

    private void addAllPaymentMethods(List<PaymentMethod> paymentMethods) {
        paymentMethods.add(PaymentMethod.Cash);
        paymentMethods.add(PaymentMethod.Card);
        paymentMethods.add(PaymentMethod.Agent);
        paymentMethods.add(PaymentMethod.Cheque);
        paymentMethods.add(PaymentMethod.Slip);
        paymentMethods.add(PaymentMethod.Credit);
        paymentMethods.add(PaymentMethod.PatientDeposit);
        paymentMethods.add(PaymentMethod.MultiplePaymentMethods);
    }

    private ReportTemplateRowBundle generateBillItems(List<BillTypeAtomic> bts, List<PaymentMethod> billPaymentMethods) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (visitType != null && (visitType.equalsIgnoreCase("IP") || visitType.equalsIgnoreCase("OP"))) {
            jpql += "AND bill.ipOpOrCc = :type ";
            parameters.put("type", visitType);
        }

        if (billPaymentMethods != null) {
            jpql += "AND bill.paymentMethod in :bpms ";
            parameters.put("bpms", billPaymentMethods);
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().isEmpty()) {
            jpql += "AND ((bill.billPackege.name) like :itemName ) ";
            parameters.put("itemName", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().isEmpty()) {
            jpql += "AND ((bill.deptId) like :billNo ) ";
            parameters.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (item != null) {
            jpql += "AND billItem.item = :item ";
            parameters.put("item", item);
        }

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY bill";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateCollectionCenterWiseInvoiceListReport() {
        System.out.println("generateCollectionCenterWiseInvoiceListReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();

        opdBts.add(BillTypeAtomic.CC_BILL);
        opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
        opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);
        // If transaction add other CC types

        System.out.println("bills");

        bundle.setName("Package Bills");
        bundle.setBundleType("billList");

        bundle = generateCollectingCenterWiseBills(opdBts);
        bundle.calculateTotalByHospitalFee();
        bundle.calculateTotalCCFee();
    }

    public ReportTemplateRowBundle generateCollectingCenterWiseBills(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br ";
        parameters.put("br", true);
        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (institution != null) {
            jpql += "AND bill.department.institution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY bill";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBill();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateDebtorBalanceReport(final boolean onlyDueBills) {
        if (visitType == null || visitType.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a visit type");
            return;
        }

        List<PaymentMethod> paymentMethods = new ArrayList<>();
        if (methodType.equalsIgnoreCase("Credit")) {
            paymentMethods.add(PaymentMethod.Credit);
        } else if (methodType.equalsIgnoreCase("NonCredit")) {
            paymentMethods.add(PaymentMethod.Cash);
        } else {
            addAllPaymentMethods(paymentMethods);
        }

        System.out.println("generateDebtorBalanceReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();
        bundle = new ReportTemplateRowBundle();

        if (visitType.equalsIgnoreCase("IP")) {
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
            //TODO: Add other needed bill types

        } else if (visitType.equalsIgnoreCase("OP")) {
            opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
        }

        bundle.setName("Bills");
        bundle.setBundleType("billList");

        bundle = generateDebtorBalanceReportBills(opdBts, paymentMethods, onlyDueBills);

        bundle.calculateTotalByBills(visitType.equalsIgnoreCase("OP"));
        bundle.calculateTotalBalance(visitType.equalsIgnoreCase("OP"));
        bundle.calculateTotalSettledAmountByPatients(visitType.equalsIgnoreCase("OP"));
        bundle.calculateTotalSettledAmountBySponsors(visitType.equalsIgnoreCase("OP"));
    }

    public ReportTemplateRowBundle generateDebtorBalanceReportBills(List<BillTypeAtomic> bts, List<PaymentMethod> billPaymentMethods,
                                                                    boolean onlyDueBills) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.creditCompany is not null ";
        parameters.put("br", true);
        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (onlyDueBills) {
            jpql += "AND bill.balance > 0 ";
        }

        if (billPaymentMethods != null) {
            jpql += "AND bill.paymentMethod in :bpms ";
            parameters.put("bpms", billPaymentMethods);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && admissionType != null)) {
            jpql += "AND bill.patientEncounter.admissionType = :type ";
            parameters.put("type", admissionType);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && roomCategory != null)) {
            jpql += "AND bill.patientEncounter.currentPatientRoom.roomFacilityCharge.roomCategory = :category ";
            parameters.put("category", roomCategory);
        }

        if (institution != null) {
            jpql += "AND bill.creditCompany = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND bill.department = :dep ";
            parameters.put("dep", department);
        }
        if (site != null) {
            jpql += "AND bill.department.site = :site ";
            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY bill";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBill();
        b.calculateTotalsWithCredit();
        return b;
    }
}
