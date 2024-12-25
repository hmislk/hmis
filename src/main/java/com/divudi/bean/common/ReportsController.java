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
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientReport;
import com.divudi.facade.*;
import com.divudi.java.CommonFunctions;
import com.divudi.light.common.BillLight;
import com.divudi.light.common.BillSummaryRow;
import com.divudi.service.BillService;
import com.divudi.service.PatientInvestigationService;
import com.itextpdf.text.Document;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.time.YearMonth;
import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
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

    private ItemLight investigationCode;

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
    private Route route;

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
    private Doctor referingDoctor;
    private Month month;
    private PaymentScheme paymentScheme;
    private String remarks;

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

    private String cashBookNumber;
    private String invoiceNumber;

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

    private Map<Integer, Map<String, Map<Integer, Double>>> groupedBillItemsWeekly;
    private Map<String, Map<Integer, Double>> groupedBillItemsWeeklyValues7to7;
    private Map<String, Map<Integer, Double>> groupedBillItemsWeeklyValues7to1;
    private Map<String, Map<Integer, Double>> groupedBillItemsWeeklyValues1to7;

    double total;
    double paid;
    double creditPaid;
    double creditUsed;
    double calTotal;
    double totalVat;
    double totalVatCalculatedValue;

    private Map<Institution, Map<YearMonth, Bill>> groupedCollectingCenterWiseBillsMonthly;
    private Map<Route, Map<YearMonth, Bill>> groupedRouteWiseBillsMonthly;
    private List<YearMonth> yearMonths;

    private String settlementStatus;
    private String dischargedStatus;

    private String selectedDateType = "invoice";

    private Investigation investigation;

    // Map<Week, Map<ItemName, Map<dayOfMonth, Count>>>
    Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap7to7;
    Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap7to1;
    Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap1to7;

    private boolean showChart;

    public String getDischargedStatus() {
        return dischargedStatus;
    }

    public void setDischargedStatus(String dischargedStatus) {
        this.dischargedStatus = dischargedStatus;
    }

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

    public PaymentScheme getPaymentScheme() {
        return paymentScheme;
    }

    public void setPaymentScheme(PaymentScheme paymentScheme) {
        this.paymentScheme = paymentScheme;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getSelectedDateType() {
        return selectedDateType;
    }

    public void setSelectedDateType(String selectedDateType) {
        this.selectedDateType = selectedDateType;
    }


    public Investigation getInvestigation() {
        return investigation;
    }

    public void setInvestigation(Investigation investigation) {
        this.investigation = investigation;
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

    public Map<Integer, Map<String, Map<Integer, Double>>> getWeeklyDailyBillItemMap7to7() {
        return weeklyDailyBillItemMap7to7;
    }

    public void setWeeklyDailyBillItemMap7to7(Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap7to7) {
        this.weeklyDailyBillItemMap7to7 = weeklyDailyBillItemMap7to7;
    }

    public Map<Integer, Map<String, Map<Integer, Double>>> getWeeklyDailyBillItemMap7to1() {
        return weeklyDailyBillItemMap7to1;
    }

    public void setWeeklyDailyBillItemMap7to1(Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap7to1) {
        this.weeklyDailyBillItemMap7to1 = weeklyDailyBillItemMap7to1;
    }

    public Map<Integer, Map<String, Map<Integer, Double>>> getWeeklyDailyBillItemMap1to7() {
        return weeklyDailyBillItemMap1to7;
    }

    public void setWeeklyDailyBillItemMap1to7(Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap1to7) {
        this.weeklyDailyBillItemMap1to7 = weeklyDailyBillItemMap1to7;
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

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
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

    public Doctor getReferingDoctor() {
        return referingDoctor;
    }

    public void setReferingDoctor(Doctor referingDoctor) {
        this.referingDoctor = referingDoctor;
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

    public Map<Integer, Map<String, Map<Integer, Double>>> getGroupedBillItemsWeekly() {
        return groupedBillItemsWeekly;
    }

    public void setGroupedBillItemsWeekly(Map<Integer, Map<String, Map<Integer, Double>>> groupedBillItemsWeekly) {
        this.groupedBillItemsWeekly = groupedBillItemsWeekly;
    }

    public Map<String, Map<Integer, Double>> getGroupedBillItemsWeeklyValues7to7() {
        return groupedBillItemsWeeklyValues7to7;
    }

    public void setGroupedBillItemsWeeklyValues7to7(Map<String, Map<Integer, Double>> groupedBillItemsWeeklyValues7to7) {
        this.groupedBillItemsWeeklyValues7to7 = groupedBillItemsWeeklyValues7to7;
    }

    public Map<String, Map<Integer, Double>> getGroupedBillItemsWeeklyValues7to1() {
        return groupedBillItemsWeeklyValues7to1;
    }

    public void setGroupedBillItemsWeeklyValues7to1(Map<String, Map<Integer, Double>> groupedBillItemsWeeklyValues7to1) {
        this.groupedBillItemsWeeklyValues7to1 = groupedBillItemsWeeklyValues7to1;
    }

    public Map<String, Map<Integer, Double>> getGroupedBillItemsWeeklyValues1to7() {
        return groupedBillItemsWeeklyValues1to7;
    }

    public void setGroupedBillItemsWeeklyValues1to7(Map<String, Map<Integer, Double>> groupedBillItemsWeeklyValues1to7) {
        this.groupedBillItemsWeeklyValues1to7 = groupedBillItemsWeeklyValues1to7;
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

    public String getCashBookNumber() {
        return cashBookNumber;
    }

    public void setCashBookNumber(String cashBookNumber) {
        this.cashBookNumber = cashBookNumber;
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

    public Month getMonth() {
        return month;
    }

    public void setMonth(Month month) {
        this.month = month;
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

    public List<Month> getMonths() {
        return Arrays.asList(Month.values());
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

    public Route getRoute() {
        return route;
    }

    public void setRoute(Route route) {
        this.route = route;
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

    public ItemLight getInvestigationCode() {
        return investigationCode;
    }

    public void setInvestigationCode(ItemLight investigationCode) {
        this.investigationCode = investigationCode;
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

    public String getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(String settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public Map<Institution, Map<YearMonth, Bill>> getGroupedCollectingCenterWiseBillsMonthly() {
        return groupedCollectingCenterWiseBillsMonthly;
    }

    public void setGroupedCollectingCenterWiseBillsMonthly(Map<Institution, Map<YearMonth, Bill>> groupedCollectingCenterWiseBillsMonthly) {
        this.groupedCollectingCenterWiseBillsMonthly = groupedCollectingCenterWiseBillsMonthly;
    }

    public List<YearMonth> getYearMonths() {
        return yearMonths;
    }

    public void setYearMonths(List<YearMonth> yearMonths) {
        this.yearMonths = yearMonths;
    }

    public Map<Route, Map<YearMonth, Bill>> getGroupedRouteWiseBillsMonthly() {
        return groupedRouteWiseBillsMonthly;
    }

    public void setGroupedRouteWiseBillsMonthly(Map<Route, Map<YearMonth, Bill>> groupedRouteWiseBillsMonthly) {
        this.groupedRouteWiseBillsMonthly = groupedRouteWiseBillsMonthly;
    }

    public boolean isShowChart() {
        return showChart;
    }

    public void setShowChart(boolean showChart) {
        this.showChart = showChart;
    }

    public void generateSampleCarrierReport() {
        System.out.println("generateSampleCarrierReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();

        if (visitType != null && visitType.equalsIgnoreCase("IP")) {
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
        } else {
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.OPD_BILL_REFUND);
        }

        System.out.println("bill items");

        bundle.setName("Sample Carrier Bill Items");
        bundle.setBundleType("billItemList");

        bundle = generateSampleCarrierBillItems(opdBts);
    }

    private ReportTemplateRowBundle generateSampleCarrierBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(pi) "
                + "FROM PatientInvestigation pi "
                + "JOIN pi.billItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE pi.retired=false "
                + " and billItem.retired=false "
                + " and bill.retired=false ";

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (visitType != null) {
            if (visitType.equalsIgnoreCase("IP") || visitType.equalsIgnoreCase("OP")) {
                jpql += "AND bill.ipOpOrCc = :type ";
                parameters.put("type", visitType);
            }
        }

        if (staff != null) {
            jpql += "AND pi.sampleTransportedToLabByStaff.person.name = :staff ";
            parameters.put("staff", staff.getPerson().getName());
        }

        if (item != null) {
            jpql += "AND billItem.patientInvestigation.investigation.name = :item ";
            parameters.put("item", item.getName());
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

        jpql += "GROUP BY pi";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        for (ReportTemplateRow row : rs) {
            BillItem billItem = row.getBillItem();
            PatientInvestigation investigation = row.getPatientInvestigation();

            if (investigation != null && investigation.getSampleSentAt() != null && investigation.getReceivedAt() != null) {
                long duration = investigation.getReceivedAt().getTime() - investigation.getSampleSentAt().getTime();
                row.setDuration(duration / (1000 * 60)); // in minutes
            } else {
                row.setDuration(0);
            }
        }

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
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

    public void generateOPDWeeklyReport() {
        System.out.println("generateOPDWeeklyReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();

        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
        opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);

        opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
        opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
        opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
        opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
        opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);

        System.out.println("bill items");

        bundle.setName("Bill Items");
        bundle.setBundleType("billItemList");

        bundle = generateWeeklyBillItems(opdBts);

        if (reportType.equalsIgnoreCase("summary")) {
            groupBillItemsWeekly();
        } else if (reportType.equalsIgnoreCase("detail")) {
            groupBillItemsDaily();
        }
    }

    private void groupBillItemsDaily() {
        // Map<Week, Map<ItemName, Map<dayOfMonth, Count>>>
        Map<Integer, Map<String, Map<Integer, Double>>> weeklyBillItemMap7to7 = new HashMap<>();
        Map<Integer, Map<String, Map<Integer, Double>>> weeklyBillItemMap7to1 = new HashMap<>();
        Map<Integer, Map<String, Map<Integer, Double>>> weeklyBillItemMap1to7 = new HashMap<>();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            final BillItem billItem = row.getBillItem();

            final Date billItemDate = billItem.getBill().getCreatedAt();

            if (billItemDate == null) {
                continue;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(billItemDate);

            final int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
            final int weekOfMonth = getWeekOfMonth(billItemDate);
            final int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

            if (hourOfDay >= 19 || hourOfDay < 7) {
                // Between 7 PM to 7 AM
                Map<String, Map<Integer, Double>> billItemMap = weeklyBillItemMap7to7.containsKey(weekOfMonth) ? weeklyBillItemMap7to7.get(weekOfMonth) : new HashMap<>();

                billItemMap.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(dayOfMonth, billItemMap.get(billItem.getItem().getName()).getOrDefault(dayOfMonth, 0.0) + 1.0);

                weeklyBillItemMap7to7.put(weekOfMonth, billItemMap);
            } else if (hourOfDay < 13) {
                // Between 7 AM to 1 PM
                Map<String, Map<Integer, Double>> billItemMap = weeklyBillItemMap7to1.containsKey(weekOfMonth) ? weeklyBillItemMap7to1.get(weekOfMonth) : new HashMap<>();

                billItemMap.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(dayOfMonth, billItemMap.get(billItem.getItem().getName()).getOrDefault(dayOfMonth, 0.0) + 1.0);

                weeklyBillItemMap7to1.put(weekOfMonth, billItemMap);
            } else {
                // Between 1 PM to 7 PM
                Map<String, Map<Integer, Double>> billItemMap = weeklyBillItemMap1to7.containsKey(weekOfMonth) ? weeklyBillItemMap1to7.get(weekOfMonth) : new HashMap<>();

                billItemMap.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(dayOfMonth, billItemMap.get(billItem.getItem().getName()).getOrDefault(dayOfMonth, 0.0) + 1.0);

                weeklyBillItemMap1to7.put(weekOfMonth, billItemMap);
            }
        }

        setWeeklyDailyBillItemMap7to7(weeklyBillItemMap7to7);
        setWeeklyDailyBillItemMap7to1(weeklyBillItemMap7to1);
        setWeeklyDailyBillItemMap1to7(weeklyBillItemMap1to7);
    }

    public List<String> getItemListByWeek(final int week, final Map<Integer, Map<String, Map<Integer, Double>>> weeklyBillItemMap) {
        if (weeklyBillItemMap == null) {
            return new ArrayList<>();
        }

        List<String> itemList = new ArrayList<>();

        if (weeklyBillItemMap.containsKey(week)) {
            itemList.addAll(weeklyBillItemMap.get(week).keySet());
        }

        return itemList;
    }

    public double getCountByWeekAndDay(final int week, final int day, final String itemName, final Map<Integer, Map<String, Map<Integer, Double>>> weeklyBillItemMap) {
        return Optional.ofNullable(weeklyBillItemMap)
                .map(map -> map.get(week))
                .map(weekMap -> weekMap.get(itemName))
                .map(itemMap -> itemMap.get(day))
                .orElse(0.0);
    }

    public double getSumByWeek(final int week, final String itemName, final Map<Integer, Map<String, Map<Integer, Double>>> weeklyBillItemMap) {
        return Optional.ofNullable(weeklyBillItemMap)
                .map(map -> map.get(week))
                .map(weekMap -> weekMap.get(itemName))
                .map(itemMap -> itemMap.values().stream().mapToDouble(Double::doubleValue).sum())
                .orElse(0.0);
    }

    public List<Integer> getDaysOfWeek(final int weekOfMonth) {
        if (month == null) {
            return new ArrayList<>();
        }

        final YearMonth yearMonth = YearMonth.of(LocalDate.now().getYear(), month);

        List<Integer> daysOfWeek = new ArrayList<>();

        LocalDate firstDayOfMonth = yearMonth.atDay(1);
        int firstDayOfTargetWeek = (weekOfMonth - 1) * 7 + 1;

        for (int i = 0; i < 7; i++) {
            LocalDate day = firstDayOfMonth.plusDays(firstDayOfTargetWeek - 1 + i);

            if (day.getMonth() != yearMonth.getMonth()) {
                break;
            }

            daysOfWeek.add(day.getDayOfMonth());
        }

        return daysOfWeek;
    }

    private void groupBillItemsWeekly() {
        Map<String, Map<Integer, Double>> billItemMap7to7 = new HashMap<>();
        Map<String, Map<Integer, Double>> billItemMap7to1 = new HashMap<>();
        Map<String, Map<Integer, Double>> billItemMap1to7 = new HashMap<>();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            final BillItem billItem = row.getBillItem();

            final Date billItemDate = billItem.getBill().getCreatedAt();

            if (billItemDate == null) {
                continue;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(billItemDate);

            final int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
            final int weekOfMonth = getWeekOfMonth(billItemDate);

            if (hourOfDay >= 19 || hourOfDay < 7) {
                // Between 7 PM to 7 AM
                billItemMap7to7.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(weekOfMonth, billItemMap7to7.get(billItem.getItem().getName()).getOrDefault(weekOfMonth, 0.0) + 1.0);
            } else if (hourOfDay < 13) {
                // Between 7 AM to 1 PM
                billItemMap7to1.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(weekOfMonth, billItemMap7to1.get(billItem.getItem().getName()).getOrDefault(weekOfMonth, 0.0) + 1.0);
            } else {
                // Between 1 PM to 7 PM
                billItemMap1to7.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(weekOfMonth, billItemMap1to7.get(billItem.getItem().getName()).getOrDefault(weekOfMonth, 0.0) + 1.0);
            }
        }

        setGroupedBillItemsWeeklyValues7to7(billItemMap7to7);
        setGroupedBillItemsWeeklyValues7to1(billItemMap7to1);
        setGroupedBillItemsWeeklyValues1to7(billItemMap1to7);

        Map<Integer, Map<String, Map<Integer, Double>>> billItemMap = new HashMap<>();
        billItemMap.put(1, billItemMap7to7);
        billItemMap.put(2, billItemMap7to1);
        billItemMap.put(3, billItemMap1to7);

        setGroupedBillItemsWeekly(billItemMap);
    }

    public double getWeeklyGroupedBillValues(final String billItemName, final int weekNumber, final int timeSlot) {
        Map<String, Map<Integer, Double>> billItemMap;

        if (timeSlot == 1) {
            billItemMap = groupedBillItemsWeeklyValues7to7;
        } else if (timeSlot == 2) {
            billItemMap = groupedBillItemsWeeklyValues7to1;
        } else if (timeSlot == 3) {
            billItemMap = groupedBillItemsWeeklyValues1to7;
        } else {
            return 0.0;
        }

        if (billItemMap.containsKey(billItemName)) {
            return billItemMap.get(billItemName).getOrDefault(weekNumber, 0.0);
        } else {
            return 0.0;
        }
    }

    public Double getTotalWeeklyGroupedBillValues(String key, Integer entryKey) {
        Double total = 0.0;
        for (int i = 1; i <= 6; i++) {
            Double value = getWeeklyGroupedBillValues(key, i, entryKey);
            if (value != null) {
                total += value;
            }
        }

        return total;
    }

    public static int getWeekOfMonth(final Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);

        return calendar.get(Calendar.WEEK_OF_MONTH);
    }

    private ReportTemplateRowBundle generateWeeklyBillItems(List<BillTypeAtomic> bts) {
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

        LocalDate firstDayOfMonth = LocalDate.of(LocalDate.now().getYear(), month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.withDayOfMonth(firstDayOfMonth.lengthOfMonth());
        Date fromDate = Date.from(firstDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(lastDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());

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

    public void generateRouteAnalysisReport() {
        System.out.println("generateRouteAnalysisReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();

        opdBts.add(BillTypeAtomic.CC_BILL);
        opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
        opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);

        System.out.println("bill items");

        bundle.setName("Route Analysis Bill Items");
        bundle.setBundleType("billItemList");

        bundle = generateCollectingCenterWiseBillItems(opdBts);

        if (reportType.equalsIgnoreCase("detail")) {
            groupCollectingCenterWiseBillsMonthly();
        } else {
            groupRouteWiseBillsMonthly();
        }
    }

    private void groupRouteWiseBillsMonthly() {
        Map<Route, Map<YearMonth, Bill>> map = new HashMap<>();
        List<YearMonth> yearMonths = new ArrayList<>();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            Bill bill = row.getBill();

            final Calendar cal = Calendar.getInstance();
            cal.setTime(bill.getCreatedAt());

            final YearMonth yearMonth = YearMonth.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));

            if (!yearMonths.contains(yearMonth)) {
                yearMonths.add(yearMonth);
            }

            Map<YearMonth, Bill> monthMap;
            if (map.containsKey(bill.getCollectingCentre().getRoute())) {
                monthMap = map.get(bill.getCollectingCentre().getRoute());

                if (monthMap.containsKey(yearMonth)) {
                    Bill existingBill = monthMap.get(yearMonth);
                    existingBill.setTotalHospitalFee(existingBill.getTotalHospitalFee() + bill.getTotalHospitalFee());
                    existingBill.setQty(existingBill.getQty() + bill.getQty());
                } else {
                    monthMap.put(yearMonth, bill);
                }
            } else {
                monthMap = new HashMap<>();
                monthMap.put(yearMonth, bill);
            }

            map.put(bill.getCollectingCentre().getRoute(), monthMap);
        }

        setGroupedRouteWiseBillsMonthly(map);
        setYearMonths(yearMonths);
    }

    private void groupCollectingCenterWiseBillsMonthly() {
        Map<Institution, Map<YearMonth, Bill>> map = new HashMap<>();
        List<YearMonth> yearMonths = new ArrayList<>();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            Bill bill = row.getBill();

            final Calendar cal = Calendar.getInstance();
            cal.setTime(bill.getCreatedAt());

            final YearMonth yearMonth = YearMonth.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));

            if (!yearMonths.contains(yearMonth)) {
                yearMonths.add(yearMonth);
            }

            Map<YearMonth, Bill> monthMap;
            if (map.containsKey(bill.getCollectingCentre())) {
                monthMap = map.get(bill.getCollectingCentre());

                if (monthMap.containsKey(yearMonth)) {
                    Bill existingBill = monthMap.get(yearMonth);
                    existingBill.setTotalHospitalFee(existingBill.getTotalHospitalFee() + bill.getTotalHospitalFee());
                    existingBill.setQty(existingBill.getQty() + bill.getQty());
                } else {
                    monthMap.put(yearMonth, bill);
                }

            } else {
                monthMap = new HashMap<>();
                monthMap.put(yearMonth, bill);

            }
            map.put(bill.getCollectingCentre(), monthMap);
        }

        setGroupedCollectingCenterWiseBillsMonthly(map);
        setYearMonths(yearMonths);
    }

    public double getCollectionCenterWiseTotalSampleCount(YearMonth yearmonth) {
        double total = 0;
        for (Map.Entry<Institution, Map<YearMonth, Bill>> entry : groupedCollectingCenterWiseBillsMonthly.entrySet()) {
            Bill summary = entry.getValue().get(yearmonth);
            if (summary != null) {
                total += summary.getQty();
            }
        }
        return total;
    }

    public double getCollectionCenterWiseTotalServiceAmount(YearMonth yearmonth) {
        double total = 0;
        for (Map.Entry<Institution, Map<YearMonth, Bill>> entry : groupedCollectingCenterWiseBillsMonthly.entrySet()) {
            Bill summary = entry.getValue().get(yearmonth);
            if (summary != null) {
                total += summary.getTotalHospitalFee();
            }
        }
        return total;
    }

    public double calculateCollectionCenterWiseBillCount(YearMonth yearmonth) {
        double total = 0;
        for (Map.Entry<Institution, Map<YearMonth, Bill>> entry : groupedCollectingCenterWiseBillsMonthly.entrySet()) {
            Bill summary = entry.getValue().get(yearmonth);
            if (summary != null) {
                total += 1;
            }
        }
        return total;
    }

    public double calculateRouteWiseBillCount(YearMonth yearmonth) {
        double total = 0;
        for (Map.Entry<Route, Map<YearMonth, Bill>> entry : groupedRouteWiseBillsMonthly.entrySet()) {
            Bill summary = entry.getValue().get(yearmonth);
            if (summary != null) {
                total += 1;
            }
        }
        return total;
    }

    public double calculateRouteWiseTotalSampleCount(YearMonth yearmonth) {
        double total = 0;
        for (Map.Entry<Route, Map<YearMonth, Bill>> entry : groupedRouteWiseBillsMonthly.entrySet()) {
            Bill summary = entry.getValue().get(yearmonth);
            if (summary != null) {
                total += summary.getQty();
            }
        }
        return total;
    }

    public double calculateRouteWiseTotalServiceAmount(YearMonth yearmonth) {
        double total = 0;
        for (Map.Entry<Route, Map<YearMonth, Bill>> entry : groupedRouteWiseBillsMonthly.entrySet()) {
            Bill summary = entry.getValue().get(yearmonth);
            if (summary != null) {
                total += summary.getTotalHospitalFee();
            }
        }
        return total;
    }

    public Map<YearMonth, Double> getSampleCountChartData() {
        Map<YearMonth, Double> data = new HashMap<>();

        if (reportType.equalsIgnoreCase("detail")) {
            for (YearMonth yearMonth : yearMonths) {
                data.put(yearMonth, getCollectionCenterWiseTotalSampleCount(yearMonth) / calculateCollectionCenterWiseBillCount(yearMonth));
            }
        } else {
            for (YearMonth yearMonth : yearMonths) {
                data.put(yearMonth, calculateRouteWiseTotalSampleCount(yearMonth) / calculateRouteWiseBillCount(yearMonth));
            }
        }

        return data;
    }

    public Map<YearMonth, Double> getServiceAmountChartData() {
        Map<YearMonth, Double> data = new HashMap<>();

        if (reportType.equalsIgnoreCase("detail")) {
            for (YearMonth yearMonth : yearMonths) {
                data.put(yearMonth, getCollectionCenterWiseTotalServiceAmount(yearMonth) / calculateCollectionCenterWiseBillCount(yearMonth));
            }
        } else {
            for (YearMonth yearMonth : yearMonths) {
                data.put(yearMonth, calculateRouteWiseTotalServiceAmount(yearMonth) / calculateRouteWiseBillCount(yearMonth));
            }
        }

        return data;
    }

    public ReportTemplateRowBundle generateCollectingCenterWiseBillItems(List<BillTypeAtomic> bts) {
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

        if (route != null) {
            jpql += "AND bill.collectingCentre.route = :route ";
            parameters.put("route", route);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY bill";

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

    public void generatePaymentSettlementReport() {
        if (visitType == null || visitType.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a visit type");
            return;
        }

        System.out.println("generatePaymentSettlementReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();
        bundle = new ReportTemplateRowBundle();

        if (visitType.equalsIgnoreCase("IP")) {
            opdBts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED);
            opdBts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION);
        } else if (visitType.equalsIgnoreCase("OP")) {
            opdBts.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
            opdBts.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_CANCELLATION);
        }

        bundle.setName("Bills");
        bundle.setBundleType("billList");

        bundle = generateReportBillItems(opdBts, null);

        bundle.calculateTotalByReferenceBills(visitType.equalsIgnoreCase("OP"));
    }

    public ReportTemplateRowBundle generateReportBillItems(List<BillTypeAtomic> bts, List<PaymentMethod> billPaymentMethods) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

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

        if (creditCompany != null) {
            if (visitType != null && visitType.equalsIgnoreCase("OP")) {
                jpql += "AND billItem.referenceBill.creditCompany = :creditC ";
            } else if (visitType != null && visitType.equalsIgnoreCase("IP")) {
                jpql += "AND billItem.referenceBill.patientEncounter.finalBill.creditCompany = :creditC ";
            }

            parameters.put("creditC", creditCompany);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateCollectionCenterBookWiseDetailReport() {
        System.out.println("generateCollectionCenterBookWiseDetailReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();
        bundle = new ReportTemplateRowBundle();

        opdBts.add(BillTypeAtomic.CC_BILL);
        opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);
        opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
        opdBts.add(BillTypeAtomic.CC_PAYMENT_RECEIVED_BILL);

        bundle.setName("Bills");
        bundle.setBundleType("billList");

        bundle = generateCollectionCenterBookWiseBills(opdBts);
    }

    public ReportTemplateRowBundle generateCollectionCenterBookWiseBills(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br ";

        parameters.put("br", true);
        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

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

        if (cashBookNumber != null && !cashBookNumber.trim().isEmpty()) {
            jpql += "AND bill.referenceNumber LIKE :cbn ";
            parameters.put("cbn", "%" + cashBookNumber + "%");
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

    public void generateCollectionCenterBillWiseDetailReport() {
        System.out.println("generateCollectionCenterBillWiseDetailReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();

        opdBts.add(BillTypeAtomic.CC_BILL);
        opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
        opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);

        System.out.println("bill items");

        bundle.setName("Route Analysis Bill Items");
        bundle.setBundleType("billItemList");

        bundle = generateCollectingCenterBillWiseBillItems(opdBts);

        if (reportType.equalsIgnoreCase("Report")) {
            groupBillItems();
        } else {
            bundle.calculateTotalHospitalFeeByBillItems();
            bundle.calculateTotalByBillItems();
            bundle.calculateTotalStaffFeeByBillItems();
        }
    }

    private void groupBillItems() {
        Map<String, List<BillItem>> billItemMap = new HashMap<>();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            BillItem billItem1 = row.getBillItem();

            if (billItemMap.containsKey(billItem1.getBill().getDeptId())) {
                billItemMap.get(billItem1.getBill().getDeptId()).add(billItem1);
            } else {
                List<BillItem> billItems = new ArrayList<>();
                billItems.add(billItem1);
                billItemMap.put(billItem1.getBill().getDeptId(), billItems);
            }
        }

        bundle.setGroupedBillItems(billItemMap);
    }

    public ReportTemplateRowBundle generateCollectingCenterBillWiseBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
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

        if (creditCompany != null) {
            jpql += "AND bill.creditCompany = :cc ";
            parameters.put("cc", creditCompany);
        }

        if (invoiceNumber != null && !invoiceNumber.trim().isEmpty()) {
            jpql += "AND bill.deptId = :in ";
            parameters.put("in", invoiceNumber);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateCreditInvoiceDispatchReport() {
        if (visitType == null || visitType.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a visit type");
            return;
        }

        System.out.println("generateCreditInvoiceDispatchReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();
        bundle = new ReportTemplateRowBundle();

        if (visitType.equalsIgnoreCase("IP")) {
            opdBts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED);
            opdBts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION);
        } else if (visitType.equalsIgnoreCase("OP")) {
            opdBts.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
            opdBts.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_CANCELLATION);
        } else {
            opdBts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED);
            opdBts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION);
            opdBts.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
            opdBts.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_CANCELLATION);
        }

        bundle.setName("Bills");
        bundle.setBundleType("billList");

        bundle = generateCreditInvoiceDispatchBillItems(opdBts, null);

        bundle.calculateTotalByBills(visitType.equalsIgnoreCase("OP"));
        bundle.calculateTotalBalance(visitType.equalsIgnoreCase("OP"));
        bundle.calculateTotalSettledAmountByPatients(visitType.equalsIgnoreCase("OP"));
        bundle.calculateTotalSettledAmountBySponsors(visitType.equalsIgnoreCase("OP"));
    }

    public ReportTemplateRowBundle generateCreditInvoiceDispatchBillItems(List<BillTypeAtomic> bts, List<PaymentMethod> billPaymentMethods) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

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

        if (creditCompany != null) {
            jpql += "AND bill.creditCompany = :cc ";
            parameters.put("cc", creditCompany);
        }

        if ("notSettled".equalsIgnoreCase(settlementStatus)) {
            jpql += "AND (billItem.referenceBill.netTotal + billItem.referenceBill.vat + billItem.referenceBill.paidAmount) <> 0 ";
        } else if ("settled".equalsIgnoreCase(settlementStatus)) {
            jpql += "AND (billItem.referenceBill.netTotal + billItem.referenceBill.vat + billItem.referenceBill.paidAmount) = 0 ";
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void externalLaboratoryWorkloadReport() {
        if (visitType == null || visitType.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a visit type");
            return;
        }

        System.out.println("generateCreditInvoiceDispatchReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();
        bundle = new ReportTemplateRowBundle();

        if (visitType != null && visitType.equalsIgnoreCase("IP")) {
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
        }
        if (visitType != null && visitType.equalsIgnoreCase("OP")) {
            opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
        }
        if (visitType != null && visitType.equalsIgnoreCase("CC")) {
            opdBts.add(BillTypeAtomic.CC_BILL);
            opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
            opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);
        }

        bundle.setName("Bills");
        bundle.setBundleType("billList");

        if (reportType.equalsIgnoreCase("detail")) {
            bundle = generateExternalLaboratoryWorkloadBillItems(opdBts);

            bundle.calculateTotalByBillItemsNetTotal();
        } else {
            bundle = generateExternalLaboratoryWorkloadSummaryBillItems(opdBts);
        }
    }

    private ReportTemplateRowBundle generateExternalLaboratoryWorkloadBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

//        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
//                + "FROM BillItem billItem "
//                + "JOIN billItem.bill bill "
//                + "LEFT JOIN PatientInvestigation pi ON pi.billItem = billItem "
//                + "WHERE bill.billTypeAtomic IN :bts "
//                + "AND bill.createdAt BETWEEN :fd AND :td ";
        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
                + "FROM PatientInvestigation pi "
                + "JOIN pi.billItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE pi.retired=false "
                + " and billItem.retired=false "
                + " and bill.retired=false ";

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (visitType != null) {
            if (visitType.equalsIgnoreCase("IP") || visitType.equalsIgnoreCase("OP") || visitType.equalsIgnoreCase("CC")) {
                jpql += "AND bill.ipOpOrCc = :type ";
                parameters.put("type", visitType);
            }
        }

        if (staff != null) {
            jpql += "AND billItem.patientInvestigation.barcodeGeneratedBy.webUserPerson.name = :staff ";
            parameters.put("staff", staff.getPerson().getName());
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

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (route != null) {
            jpql += "AND bill.collectingCentre.route = :route ";
            parameters.put("route", route);
        }

        if (referingDoctor != null) {
            jpql += "AND billItem.bill.referredBy = :rd ";
            parameters.put("rd", referingDoctor);
        }

        if (mrnNo != null && !mrnNo.trim().isEmpty()) {
            jpql += "AND billItem.bill.patient.phn LIKE :phn ";
            parameters.put("phn", mrnNo + "%");
        }

        if (category != null) {
            jpql += "AND billItem.item.department.id = :cat ";
            parameters.put("cat", category.getId());
        }

        if (investigation != null) {
            jpql += "AND billItem.item = :code ";
            parameters.put("code", investigation);
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

    private ReportTemplateRowBundle generateExternalLaboratoryWorkloadSummaryBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("bts", bts);
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem.item.name, SUM(billItem.qty)) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "LEFT JOIN PatientInvestigation pi ON pi.billItem = billItem "
                + "WHERE bill.billTypeAtomic IN :bts "
                + "AND bill.createdAt BETWEEN :fd AND :td ";

        if (visitType != null) {
            if (visitType.equalsIgnoreCase("IP") || visitType.equalsIgnoreCase("OP") || visitType.equalsIgnoreCase("CC")) {
                jpql += "AND bill.ipOpOrCc = :type ";
                parameters.put("type", visitType);
            }
        }

        if (staff != null) {
            jpql += "AND billItem.patientInvestigation.barcodeGeneratedBy.webUserPerson.name = :staff ";
            parameters.put("staff", staff.getPerson().getName());
        }

        if (item != null) {
            jpql += "AND billItem.patientInvestigation.investigation.name = :item ";
            parameters.put("item", item.getName());
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

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (route != null) {
            jpql += "AND bill.collectingCentre.route = :route ";
            parameters.put("route", route);
        }

        if (referingDoctor != null) {
            jpql += "AND billItem.bill.referredBy = :rd ";
            parameters.put("rd", referingDoctor);
        }

        if (mrnNo != null && !mrnNo.trim().isEmpty()) {
            jpql += "AND billItem.bill.patient.phn LIKE :phn ";
            parameters.put("phn", mrnNo + "%");
        }

        if (category != null) {
            jpql += "AND billItem.patientInvestigation.investigation.category.id = :cat ";
            parameters.put("cat", category.getId());
        }

        if (investigationCode != null) {
            jpql += "AND billItem.patientInvestigation.investigation.code = :code ";
            parameters.put("code", investigationCode.getCode());
        }

        jpql += "GROUP BY billItem.item.name";

        System.out.println("jpql = " + jpql);
        System.out.println("parameters = " + parameters);

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateOpdAndInwardDueReport() {
        if (visitType == null || visitType.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a visit type");
            return;
        }

        System.out.println("generatePaymentSettlementReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();
        bundle = new ReportTemplateRowBundle();

        if (visitType.equalsIgnoreCase("IP")) {
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
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

        bundle = generateOpdAndInwardDueBills(opdBts);

        groupBills();
    }

    public ReportTemplateRowBundle generateOpdAndInwardDueBills(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.creditCompany is not null ";

        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (paymentMethod != null) {
            List<PaymentMethod> billPaymentMethods = new ArrayList<>();
            billPaymentMethods.add(paymentMethod);

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

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && dischargedStatus != null && !dischargedStatus.trim().isEmpty())) {
            if (dischargedStatus.equalsIgnoreCase("notDischarged")) {
                jpql += "AND bill.patientEncounter.discharged = :status ";
                parameters.put("status", false);
            } else if (dischargedStatus.equalsIgnoreCase("discharged")) {
                jpql += "AND bill.patientEncounter.discharged = :status ";
                parameters.put("status", true);
            }
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

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (creditCompany != null) {
            jpql += "AND bill.creditCompany = :cc ";
            parameters.put("cc", creditCompany);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY bill, bill.creditCompany";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBill();
        b.calculateTotalsWithCredit();
        return b;
    }

    private void groupBills() {
        Map<Institution, List<Bill>> billMap = new HashMap<>();

        if (visitType != null && visitType.equalsIgnoreCase("OP")) {
            for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
                Bill bill1 = row.getBill();

                if (reportType != null && reportType.equalsIgnoreCase("paid")) {
                    if (bill1.getBalance() != 0) {
                        continue;
                    }
                }

                if (reportType != null && reportType.equalsIgnoreCase("due")) {
                    if (bill1.getBalance() == 0) {
                        continue;
                    }
                }

                if (billMap.containsKey(bill1.getCreditCompany())) {
                    billMap.get(bill1.getCreditCompany()).add(bill1);
                } else {
                    List<Bill> bills = new ArrayList<>();
                    bills.add(bill1);
                    billMap.put(bill1.getCreditCompany(), bills);
                }
            }
        } else if (visitType != null && visitType.equalsIgnoreCase("IP")) {
            for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
                Bill bill1 = row.getBill();

                if (reportType != null && reportType.equalsIgnoreCase("paid")) {
                    if (bill1.getBalance() != 0) {
                        continue;
                    }
                }

                if (reportType != null && reportType.equalsIgnoreCase("due")) {
                    if (bill1.getBalance() == 0) {
                        continue;
                    }
                }

                if (billMap.containsKey(bill1.getPatientEncounter().getFinalBill().getCreditCompany())) {
                    billMap.get(bill1.getPatientEncounter().getFinalBill().getCreditCompany()).add(bill1);
                } else {
                    List<Bill> bills = new ArrayList<>();
                    bills.add(bill1);
                    billMap.put(bill1.getPatientEncounter().getFinalBill().getCreditCompany(), bills);
                }
            }
        }

        bundle.setGroupedBillItemsByInstitution(billMap);
    }

    public Double calculateNetTotalByBills(List<Bill> bills) {
        Double netTotal = 0.0;

        for (Bill bill : bills) {
            netTotal += bill.getNetTotal();
        }

        return netTotal;
    }

    public Double calculateDiscountByBills(List<Bill> bills) {
        Double discount = 0.0;

        for (Bill bill : bills) {
            discount += bill.getDiscount();
        }

        return discount;
    }

    public Double calculateSponsorShareNetTotal() {
        double sponsorShareNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            sponsorShareNetTotal += calculateNetTotalByBills(bills);
        }

        return sponsorShareNetTotal;
    }

    public Double calculateNetAmountNetTotal() {
        double netAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            netAmountNetTotal += calculateNetAmountSubTotalByBills(bills);
        }

        return netAmountNetTotal;
    }

    public Double calculateNetAmountSubTotalByBills(List<Bill> bills) {
        Double netTotal = 0.0;

        for (Bill bill : bills) {
            netTotal += bill.getNetTotal();
        }

        return netTotal;
    }

    public Double calculateGrossAmountSubTotalByBills(List<Bill> bills) {
        Double billTotal = 0.0;

        for (Bill bill : bills) {
            billTotal += bill.getBillTotal();
        }

        return billTotal;
    }

    public Double calculatePatientShareSubTotalByBills(List<Bill> bills) {
        Double settledAmountByPatient = 0.0;

        for (Bill bill : bills) {
            settledAmountByPatient += bill.getSettledAmountByPatient();
        }

        return settledAmountByPatient;
    }

    public Double calculateSponsorShareSubTotalByBills(List<Bill> bills) {
        Double settledAmountBySponsor = 0.0;

        for (Bill bill : bills) {
            settledAmountBySponsor += bill.getSettledAmountBySponsor();
        }

        return settledAmountBySponsor;
    }

    public Double calculateDueAmountSubTotalByBills(List<Bill> bills) {
        Double balance = 0.0;

        for (Bill bill : bills) {
            balance += bill.getBalance();
        }

        return balance;
    }

    public Double calculateGrossAmountNetTotal() {
        double grossAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            grossAmountNetTotal += calculateGrossAmountSubTotalByBills(bills);
        }

        return grossAmountNetTotal;
    }

    public Double calculateDiscountNetTotal() {
        double discountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            discountNetTotal += calculateDiscountSubTotalByBills(bills);
        }

        return discountNetTotal;
    }

    public Double calculateDiscountSubTotalByBills(List<Bill> bills) {
        Double discount = 0.0;

        for (Bill bill : bills) {
            discount += bill.getDiscount();
        }

        return discount;
    }

    public Double calculatePatientShareNetTotal() {
        double patientShareNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            patientShareNetTotal += calculatePatientShareSubTotalByBills(bills);
        }

        return patientShareNetTotal;
    }

    public Double calculateDueAmountNetTotal() {
        double dueAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            dueAmountNetTotal += calculateDueAmountSubTotalByBills(bills);
        }

        return dueAmountNetTotal;
    }

    public void generateDiscountReport() {
        if (visitType == null || visitType.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a visit type");
            return;
        }

        System.out.println("generateDiscountReport = " + this);
        bundle = new ReportTemplateRowBundle();

        List<BillTypeAtomic> opdBts = new ArrayList<>();
        bundle = new ReportTemplateRowBundle();

        if (visitType.equalsIgnoreCase("IP")) {
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
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

        if (reportType.equalsIgnoreCase("detail")) {
            bundle.setName("BillItems");
            bundle.setBundleType("billItemList");

            bundle = generateDiscountBillItems(opdBts);
            bundle.calculateTotalDiscountByBillItems();
        } else if (reportType.equalsIgnoreCase("summary")) {
            bundle.setName("Bills");
            bundle.setBundleType("billList");

            bundle = generateDiscountBills(opdBts);
            bundle.calculateTotalDiscountByBills();
        }
    }

    public ReportTemplateRowBundle generateDiscountBills(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.paymentScheme is not null ";

        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (paymentMethod != null) {
            List<PaymentMethod> billPaymentMethods = new ArrayList<>();
            billPaymentMethods.add(paymentMethod);

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

        if (discount > 0) {
            jpql += "AND bill.discount = :dis ";
            parameters.put("dis", discount);
        }

        if (remarks != null && !remarks.trim().isEmpty()) {
            jpql += "AND bill.comments LIKE :rem ";
            parameters.put("rem", remarks + "%");
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

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :cc ";
            parameters.put("cc", collectingCentre);
        }

        if (creditCompany != null) {
            jpql += "AND bill.creditCompany = :cc ";
            parameters.put("cc", creditCompany);
        }

        if (paymentScheme != null) {
            jpql += "AND bill.paymentScheme = :ps ";
            parameters.put("ps", paymentScheme);
        }

        if (selectedDateType != null && !selectedDateType.trim().isEmpty() && selectedDateType.equalsIgnoreCase("admission")) {
            jpql += "AND bill.patientEncounter.dateOfAdmission BETWEEN :fd AND :td ";
        } else if (selectedDateType != null && !selectedDateType.trim().isEmpty() && selectedDateType.equalsIgnoreCase("discharge")) {
            jpql += "AND bill.patientEncounter.dateOfDischarge BETWEEN :fd AND :td ";
        } else {
            jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        }

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

    public ReportTemplateRowBundle generateDiscountBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br "
                + "AND billItem.bill.paymentScheme is not null ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (paymentMethod != null) {
            List<PaymentMethod> billPaymentMethods = new ArrayList<>();
            billPaymentMethods.add(paymentMethod);

            jpql += "AND bill.paymentMethod in :bpms ";
            parameters.put("bpms", billPaymentMethods);
        }

        if (discount > 0) {
            jpql += "AND billItem.bill.discount = :dis ";
            parameters.put("dis", discount);
        }

        if (remarks != null && !remarks.trim().isEmpty()) {
            jpql += "AND billItem.bill.comments LIKE :rem ";
            parameters.put("rem", remarks + "%");
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

        if (creditCompany != null) {
            jpql += "AND bill.creditCompany = :cc ";
            parameters.put("cc", creditCompany);
        }

        if (paymentScheme != null) {
            jpql += "AND bill.paymentScheme = :ps ";
            parameters.put("ps", paymentScheme);
        }

        if ("notSettled".equalsIgnoreCase(settlementStatus)) {
            jpql += "AND (billItem.referenceBill.netTotal + billItem.referenceBill.vat + billItem.referenceBill.paidAmount) <> 0 ";
        } else if ("settled".equalsIgnoreCase(settlementStatus)) {
            jpql += "AND (billItem.referenceBill.netTotal + billItem.referenceBill.vat + billItem.referenceBill.paidAmount) = 0 ";
        }

        if (selectedDateType != null && !selectedDateType.trim().isEmpty() && selectedDateType.equalsIgnoreCase("admission")) {
            jpql += "AND bill.patientEncounter.dateOfAdmission BETWEEN :fd AND :td ";
        } else if (selectedDateType != null && !selectedDateType.trim().isEmpty() && selectedDateType.equalsIgnoreCase("discharge")) {
            jpql += "AND bill.patientEncounter.dateOfDischarge BETWEEN :fd AND :td ";
        } else {
            jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        }

        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void exportCollectionCenterBillWiseDetailReportToPDF() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Collection_Center_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);

            document.open();

            document.add(new Paragraph("Collection Center Bill Wise Detail Report",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));

            PdfPTable table = new PdfPTable(7);
            float[] columnWidths = {1f, 1f, 1f, 1f, 1f, 1f, 3f};
            table.setWidths(columnWidths);
            table.setWidthPercentage(100);

            table.addCell("CC Code");
            table.addCell("Leaf No.");
            table.addCell("MRN");
            table.addCell("Patient Name");
            table.addCell("Invoice Date");
            table.addCell("Invoice No");
            table.addCell("Details");

            for (Map.Entry<String, List<BillItem>> entry : bundle.getGroupedBillItems().entrySet()) {
                List<BillItem> billItems = entry.getValue();

                if (billItems == null || billItems.isEmpty()) {
                    table.addCell("N/A");
                    table.addCell("N/A");
                    table.addCell("N/A");
                    table.addCell("N/A");
                    table.addCell("N/A");
                    table.addCell(entry.getKey());
                    PdfPCell emptyCell = new PdfPCell(new Phrase("No Details Available"));
                    emptyCell.setColspan(7);
                    table.addCell(emptyCell);
                    continue;
                }

                BillItem firstItem = billItems.get(0);

                table.addCell(firstItem.getBill().getCollectingCentre().getCode());
                table.addCell(firstItem.getBill().getReferenceNumber());
                table.addCell(firstItem.getBill().getPatient().getPhn());
                table.addCell(firstItem.getBill().getPatient().getPerson().getName());
                table.addCell(firstItem.getBill().getCreatedAt().toString());
                table.addCell(entry.getKey());

                PdfPTable detailsTable = new PdfPTable(5);
                float[] columnWidthsInner = {2f, 1f, 1f, 1f, 1f};
                detailsTable.setWidths(columnWidthsInner);

                detailsTable.addCell("Service Name");
                detailsTable.addCell("Hos Fee");
                detailsTable.addCell("Staff Fee");
                detailsTable.addCell("CC Fee");
                detailsTable.addCell("Net Amount");

                for (BillItem bi : billItems) {
                    detailsTable.addCell(bi.getItem().getName());
                    detailsTable.addCell(String.valueOf(bi.getHospitalFee()));
                    detailsTable.addCell(String.valueOf(bi.getStaffFee()));
                    detailsTable.addCell(String.valueOf(bi.getCollectingCentreFee()));
                    detailsTable.addCell(String.valueOf(bi.getNetValue()));
                }
                PdfPCell nestedCell = new PdfPCell(detailsTable);
                nestedCell.setColspan(7);
                table.addCell(nestedCell);
            }

            document.add(table);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportCollectionCenterBillWiseDetailReportToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Collection_Center_Report.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream out = response.getOutputStream()) {


            XSSFSheet sheet = workbook.createSheet("Report");
            int rowIndex = 0;

            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("CC Code");
            headerRow.createCell(1).setCellValue("Leaf No.");
            headerRow.createCell(2).setCellValue("MRN");
            headerRow.createCell(3).setCellValue("Patient Name");
            headerRow.createCell(4).setCellValue("Invoice Date");
            headerRow.createCell(5).setCellValue("Invoice No");
            headerRow.createCell(6).setCellValue("Details");

            for (Map.Entry<String, List<BillItem>> entry : bundle.getGroupedBillItems().entrySet()) {
                List<BillItem> billItems = entry.getValue();

                if (billItems == null || billItems.isEmpty()) {
                    Row emptyRow = sheet.createRow(rowIndex++);
                    emptyRow.createCell(0).setCellValue("N/A");
                    emptyRow.createCell(1).setCellValue("N/A");
                    emptyRow.createCell(2).setCellValue("N/A");
                    emptyRow.createCell(3).setCellValue("N/A");
                    emptyRow.createCell(4).setCellValue("N/A");
                    emptyRow.createCell(5).setCellValue(entry.getKey());
                    emptyRow.createCell(6).setCellValue("No Details Available");
                    continue;
                }

                BillItem firstItem = billItems.get(0);
                Row row = sheet.createRow(rowIndex++);

                row.createCell(0).setCellValue(firstItem.getBill().getCollectingCentre().getCode());
                row.createCell(1).setCellValue(firstItem.getBill().getReferenceNumber());
                row.createCell(2).setCellValue(firstItem.getBill().getPatient().getPhn());
                row.createCell(3).setCellValue(firstItem.getBill().getPatient().getPerson().getName());
                row.createCell(4).setCellValue(firstItem.getBill().getCreatedAt().toString());
                row.createCell(5).setCellValue(entry.getKey());

                for (BillItem bi : billItems) {
                    Row detailRow = sheet.createRow(rowIndex++);
                    detailRow.createCell(6).setCellValue(bi.getItem().getName());
                    detailRow.createCell(7).setCellValue(bi.getHospitalFee());
                    detailRow.createCell(8).setCellValue(bi.getStaffFee());
                    detailRow.createCell(9).setCellValue(bi.getCollectingCentreFee());
                    detailRow.createCell(10).setCellValue(bi.getNetValue());
                }
            }

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
