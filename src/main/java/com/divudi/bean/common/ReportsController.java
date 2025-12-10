package com.divudi.bean.common;

import com.divudi.bean.collectingCentre.CollectingCentreBillController;
import com.divudi.core.data.dataStructure.InstitutionBillEncounter;
import com.divudi.core.data.reports.FinancialReport;
import com.divudi.core.data.reports.ManagementReport;
import com.divudi.core.entity.channel.AgentReferenceBook;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.data.*;
import com.divudi.core.data.analytics.ReportTemplateType;
import com.divudi.core.data.dataStructure.SearchKeyword;
import com.divudi.core.data.hr.ReportKeyWord;
import com.divudi.core.data.reports.CollectionCenterReport;
import com.divudi.core.data.reports.LaboratoryReport;
import com.divudi.ejb.PharmacyBean;
import com.divudi.core.entity.*;
import com.divudi.core.entity.cashTransaction.CashBookEntry;
import com.divudi.core.entity.cashTransaction.Drawer;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.entity.inward.RoomCategory;
import com.divudi.core.entity.lab.Investigation;
import com.divudi.core.entity.lab.PatientInvestigation;
import com.divudi.core.entity.lab.PatientReport;
import com.divudi.core.facade.*;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.light.common.BillLight;
import com.divudi.core.light.common.BillSummaryRow;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;

import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.text.DecimalFormat;

/**
 * @author safrin
 */
@Named
@SessionScoped
public class ReportsController implements Serializable {

    private static final long serialVersionUID = 1L;

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
    StockFacade stockFacade;
    @EJB
    PatientReportFacade patientReportFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private DrawerFacade drawerFacade;
    @EJB
    PatientEncounterFacade peFacade;
    @EJB
    PatientEncounterFacade patientEncounterFacade;
    @EJB
    private ReportTimerController reportTimerController;
    @EJB
    private EncounterCreditCompanyFacade encounterCreditCompanyFacade;
    @EJB
    private AgentReferenceBookFacade agentReferenceBookFacade;

    /**
     * Inject
     */
    @Inject
    private SessionController sessionController;
    @Inject
    TransferController transferController;
    @Inject
    private DepartmentController departmentController;
    @Inject
    CollectingCentreBillController collectingCentreBillController;
    @Inject
    private WebUserController webUserController;

    /**
     * Properties
     */
    private List<PatientEncounter> patientEncounters;
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
    private List<ReportTemplateRow> filteredReportTemplateRows;

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
    private double payableByPatient;
    private double payableByCompany;
    private double gopAmount;
    private double dueByCompany;
    double billed;
    double paidByPatient;
    double paidByCompany;

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

    private int rowCounter = 0;

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

    Map<PatientEncounter, List<Bill>> billPatientEncounterMap = new HashMap<>();

    private Map<Institution, List<InstitutionBillEncounter>> billInstitutionEncounterMap;
    private Map<PatientEncounter, List<InstitutionBillEncounter>> institutionBillPatientEncounterMap;

    private int globalIndex;

    Map<Institution, Double> instituteGopMap = new HashMap<>();
    Map<Institution, Double> institutPaidByCompanyMap = new HashMap<>();
    Map<Institution, Double> institutePayableByCompanyMap = new HashMap<>();
    Map<Institution, Double> instituteDueByCompanyMap = new HashMap<>();

    private Map<PatientEncounter, Double> patientEncounterGopMap;
    private Map<PatientEncounter, Double> patientEncounterPaidByCompanyMap;

    private Map<String, Map<String, EncounterCreditCompany>> encounterCreditCompanyMap;
    private Institution institutionOfDepartment;

    private boolean withInactiveBooks;
    private boolean withDeletedBooks;
    private boolean withoutDateRange;

    public boolean isWithoutDateRange() {
        return withoutDateRange;
    }

    public void setWithoutDateRange(boolean withoutDateRange) {
        this.withoutDateRange = withoutDateRange;
    }

    public boolean isWithInactiveBooks() {
        return withInactiveBooks;
    }

    public void setWithInactiveBooks(boolean withInactiveBooks) {
        this.withInactiveBooks = withInactiveBooks;
    }

    public boolean isWithDeletedBooks() {
        return withDeletedBooks;
    }

    public void setWithDeletedBooks(boolean withDeletedBooks) {
        this.withDeletedBooks = withDeletedBooks;
    }

    public Institution getInstitutionOfDepartment() {
        return institutionOfDepartment;
    }

    public Map<PatientEncounter, List<InstitutionBillEncounter>> getInstitutionBillPatientEncounterMap() {
        if (institutionBillPatientEncounterMap == null) {
            institutionBillPatientEncounterMap = new HashMap<>();
        }
        return institutionBillPatientEncounterMap;
    }

    public void setInstitutionBillPatientEncounterMap(Map<PatientEncounter, List<InstitutionBillEncounter>> institutionBillPatientEncounterMap) {
        if (institutionBillPatientEncounterMap == null) {
            institutionBillPatientEncounterMap = new HashMap<>();
        }
        this.institutionBillPatientEncounterMap = institutionBillPatientEncounterMap;
    }

    public Map<PatientEncounter, Double> getPatientEncounterGopMap() {
        if (patientEncounterGopMap == null) {
            patientEncounterGopMap = new HashMap<>();
        }
        return patientEncounterGopMap;
    }

    public void setPatientEncounterGopMap(Map<PatientEncounter, Double> patientEncounterGopMap) {
        this.patientEncounterGopMap = patientEncounterGopMap;
    }

    public Map<PatientEncounter, Double> getPatientEncounterPaidByCompanyMap() {
        if (patientEncounterPaidByCompanyMap == null) {
            patientEncounterPaidByCompanyMap = new HashMap<>();
        }
        return patientEncounterPaidByCompanyMap;
    }

    public void setPatientEncounterPaidByCompanyMap(Map<PatientEncounter, Double> patientEncounterPaidByCompanyMap) {
        this.patientEncounterPaidByCompanyMap = patientEncounterPaidByCompanyMap;
    }

    public int nextRowCounter() {
        return ++rowCounter;
    }

    public void resetCounter() {
        rowCounter = 0;
    }

    public void setInstitutionOfDepartment(Institution institutionOfDepartment) {
        this.institutionOfDepartment = institutionOfDepartment;
    }

    public Map<Institution, List<InstitutionBillEncounter>> getBillInstitutionEncounterMap() {
        return billInstitutionEncounterMap;
    }

    public void setBillInstitutionEncounterMap(Map<Institution, List<InstitutionBillEncounter>> billInstitutionEncounterMap) {
        if (billInstitutionEncounterMap == null) {
            billInstitutionEncounterMap = new HashMap<>();
        }

        this.billInstitutionEncounterMap = billInstitutionEncounterMap;
    }

    public Map<Institution, Double> getInstituteGopMap() {
        return instituteGopMap;
    }

    public void setInstituteGopMap(Map<Institution, Double> instituteGopMap) {
        this.instituteGopMap = instituteGopMap;
    }

    public Map<Institution, Double> getInstitutPaidByCompanyMap() {
        return institutPaidByCompanyMap;
    }

    public void setInstitutPaidByCompanyMap(Map<Institution, Double> institutPaidByCompanyMap) {
        this.institutPaidByCompanyMap = institutPaidByCompanyMap;
    }

    public Map<Institution, Double> getInstitutePayableByCompanyMap() {
        return institutePayableByCompanyMap;
    }

    public void setInstitutePayableByCompanyMap(Map<Institution, Double> institutePayableByCompanyMap) {
        this.institutePayableByCompanyMap = institutePayableByCompanyMap;
    }

    public Map<Institution, Double> getInstituteDueByCompanyMap() {
        return instituteDueByCompanyMap;
    }

    public void setInstituteDueByCompanyMap(Map<Institution, Double> instituteDueByCompanyMap) {
        this.instituteDueByCompanyMap = instituteDueByCompanyMap;
    }

    public PatientEncounterFacade getPatientEncounterFacade() {
        return patientEncounterFacade;
    }

    public void setPatientEncounterFacade(PatientEncounterFacade patientEncounterFacade) {
        this.patientEncounterFacade = patientEncounterFacade;
    }

    public int getGlobalIndex() {
        return globalIndex;
    }

    public void setGlobalIndex(int globalIndex) {
        this.globalIndex = globalIndex;
    }

    public void resetGlobalIndex() {
        globalIndex = 0;
    }

    public double getPayableByPatient() {
        return payableByPatient;
    }

    public double getPayableByCompany() {
        return payableByCompany;
    }

    public void setPayableByCompany(double payableByCompany) {
        this.payableByCompany = payableByCompany;
    }

    public double getDueByCompany() {
        return dueByCompany;
    }

    public void setDueByCompany(double dueByCompany) {
        this.dueByCompany = dueByCompany;
    }

    public double getGopAmount() {
        return gopAmount;
    }

    public void setGopAmount(double gopAmount) {
        this.gopAmount = gopAmount;
    }

    public void setPayableByPatient(double payableByPatient) {
        this.payableByPatient = payableByPatient;
    }

    public Map<String, Map<String, EncounterCreditCompany>> getEncounterCreditCompanyMap() {
        if (encounterCreditCompanyMap == null) {
            encounterCreditCompanyMap = new HashMap<>();
        }

        return encounterCreditCompanyMap;
    }

    public void setEncounterCreditCompanyMap(Map<String, Map<String, EncounterCreditCompany>> encounterCreditCompanyMap) {
        this.encounterCreditCompanyMap = encounterCreditCompanyMap;
    }

    public int getNextIndex() {
        return ++globalIndex;
    }

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

    public List<ReportTemplateRow> getFilteredReportTemplateRows() {
        if (filteredReportTemplateRows == null) {
            filteredReportTemplateRows = new ArrayList<>();
        }
        return filteredReportTemplateRows;
    }

    public void setFilteredReportTemplateRows(List<ReportTemplateRow> filteredReportTemplateRows) {
        this.filteredReportTemplateRows = filteredReportTemplateRows;
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
        maxDate = CommonFunctions.getEndOfDay(new Date());
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

    public Map<PatientEncounter, List<Bill>> getBillPatientEncounterMap() {
        return billPatientEncounterMap;
    }

    public void setBillPatientEncounterMap(Map<PatientEncounter, List<Bill>> billPatientEncounterMap) {
        this.billPatientEncounterMap = billPatientEncounterMap;
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
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
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

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
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

    public double getBilled() {
        return billed;
    }

    public void setBilled(double billed) {
        this.billed = billed;
    }

    public double getPaidByPatient() {
        return paidByPatient;
    }

    public void setPaidByPatient(double paidByPatient) {
        this.paidByPatient = paidByPatient;
    }

    public double getPaidByCompany() {
        return paidByCompany;
    }

    public void setPaidByCompany(double paidByCompany) {
        this.paidByCompany = paidByCompany;
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

    public Date getCurrentDate() {
        return new Date();
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
        reportTimerController.trackReportExecution(() -> {
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


            bundle.setName("Sample Carrier Bill Items");
            bundle.setBundleType("billItemList");

            bundle = generateSampleCarrierBillItems(opdBts);
        }, LaboratoryReport.SAMPLE_CARRIER_REPORT, sessionController.getLoggedUser());
    }

    private ReportTemplateRowBundle generateSampleCarrierBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(pi) "
                + "FROM PatientInvestigation pi "
                + "JOIN pi.billItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE pi.retired=false "
                + " and billItem.retired=false "
                + " and bill.retired=false "
                + "AND pi.sampleSentAt IS NOT NULL ";

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
        reportTimerController.trackReportExecution(() -> {
            bundle = new ReportTemplateRowBundle();

            List<BillTypeAtomic> opdBts = new ArrayList<>();

            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);


            bundle.setName("Package Bill Items");
            bundle.setBundleType("billItemList");

            bundle = generateBillItems(opdBts);
        }, FinancialReport.PACKAGE_REPORT, sessionController.getLoggedUser());
    }

    private ReportTemplateRowBundle generateBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(billItem) "
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


        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateOPDWeeklyReport() {
        reportTimerController.trackReportExecution(() -> {
            if (month == null) {
                JsfUtil.addErrorMessage("Please select a month");
                return;
            }

            bundle = new ReportTemplateRowBundle();

            List<BillTypeAtomic> opdBts = new ArrayList<>();

            if (visitType == null || visitType.equalsIgnoreCase("OP")) {
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
                opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
                opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.OPD_BILL_REFUND);
                opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
            }

            if (visitType == null || visitType.equalsIgnoreCase("IP")) {
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
                opdBts.add(BillTypeAtomic.CANCELLED_INWARD_FINAL_BILL);
                opdBts.add(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE_RETURN);
            }
//        if (visitType == null) {
//            opdBts.add(BillTypeAtomic.CC_BILL);
//            opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
//            opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);
//        }


            bundle.setName("Bill Items");
            bundle.setBundleType("billItemList");

            bundle = generateWeeklyBillItems(opdBts);

            if (reportType.equalsIgnoreCase("summary")) {
                groupBillItemsWeekly();
            } else if (reportType.equalsIgnoreCase("detail")) {
                groupBillItemsDaily();
            }
        }, ManagementReport.OPD_WEEKLY_REPORT, sessionController.getLoggedUser());
    }

    public int getNumberOfWeeksOfMonth() {
        if (month == null) {
            return 0;
        }

        LocalDate firstDayOfMonth = LocalDate.of(LocalDate.now().getYear(), month, 1);
        LocalDate lastDayOfMonth = firstDayOfMonth.with(TemporalAdjusters.lastDayOfMonth());

        Date lastDate = Date.from(lastDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant());

        return getWeekOfMonth(lastDate);
    }

    public String getDateRangeText(Integer week, boolean onlyDateAndMonth) {
        if (week == null) {
            return "-";
        }

        List<Integer> daysOfWeek = getDaysOfWeek(week);
        if (daysOfWeek.isEmpty()) {
            return "-";
        }

        int startDay = daysOfWeek.get(0);
        int endDay = daysOfWeek.get(daysOfWeek.size() - 1);

        int yearValue = LocalDate.now().getYear();
        int monthValue = month.getValue();

        LocalDate startDate = LocalDate.of(yearValue, monthValue, startDay);
        LocalDate endDate = LocalDate.of(yearValue, monthValue, endDay);

        String pattern = sessionController.getApplicationPreference().getLongDateTimeFormat();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);

        if (onlyDateAndMonth) {
            pattern = "dd/MM";
            formatter = DateTimeFormatter.ofPattern(pattern);

            return formatter.format(startDate) + " - " + formatter.format(endDate);
        } else {
            return formatter.format(startDate.atStartOfDay()) + " - " + formatter.format(endDate.atTime(LocalTime.MAX));
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

            if (billItemDate == null || billItem.getItem() == null) {
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
                        .put(dayOfMonth, billItemMap.get(billItem.getItem().getName()) != null
                                ? billItemMap.get(billItem.getItem().getName()).getOrDefault(dayOfMonth, 0.0) + billItem.getQty() : billItem.getQty());

                weeklyBillItemMap7to7.put(weekOfMonth, billItemMap);
            } else if (hourOfDay < 13) {
                // Between 7 AM to 1 PM
                Map<String, Map<Integer, Double>> billItemMap = weeklyBillItemMap7to1.containsKey(weekOfMonth) ? weeklyBillItemMap7to1.get(weekOfMonth) : new HashMap<>();

                billItemMap.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(dayOfMonth, billItemMap.get(billItem.getItem().getName()) != null
                                ? billItemMap.get(billItem.getItem().getName()).getOrDefault(dayOfMonth, 0.0) + billItem.getQty() : billItem.getQty());

                weeklyBillItemMap7to1.put(weekOfMonth, billItemMap);
            } else {
                // Between 1 PM to 7 PM
                Map<String, Map<Integer, Double>> billItemMap = weeklyBillItemMap1to7.containsKey(weekOfMonth) ? weeklyBillItemMap1to7.get(weekOfMonth) : new HashMap<>();

                billItemMap.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(dayOfMonth, billItemMap.get(billItem.getItem().getName()) != null
                                ? billItemMap.get(billItem.getItem().getName()).getOrDefault(dayOfMonth, 0.0) + billItem.getQty() : billItem.getQty());

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
        final LocalDate firstDayOfMonth = yearMonth.atDay(1);

        List<Integer> daysOfWeek = new ArrayList<>();

        LocalDate firstSunday = firstDayOfMonth;
        while (firstSunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
            firstSunday = firstSunday.plusDays(1);
        }

        if (weekOfMonth == 1) {
            LocalDate currentDay = firstDayOfMonth;
            while (currentDay.getDayOfWeek() != DayOfWeek.SUNDAY && currentDay.getMonthValue() == month.getValue()) {
                daysOfWeek.add(currentDay.getDayOfMonth());
                currentDay = currentDay.plusDays(1);
            }
        } else {
            LocalDate firstDayOfWeek = firstSunday.plusWeeks(weekOfMonth - 2);

            for (int i = 0; i < 7; i++) {
                LocalDate day = firstDayOfWeek.plusDays(i);

                if (day.getMonthValue() != month.getValue()) {
                    break;
                }

                daysOfWeek.add(day.getDayOfMonth());
            }
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

            if (billItemDate == null || billItem.getItem() == null) {
                continue;
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(billItemDate);

            final int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
            final int weekOfMonth = getWeekOfMonth(billItemDate);

            if (hourOfDay >= 19 || hourOfDay < 7) {
                // Between 7 PM to 7 AM
                billItemMap7to7.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(weekOfMonth, billItemMap7to7.get(billItem.getItem().getName()) != null
                                ? billItemMap7to7.get(billItem.getItem().getName()).getOrDefault(weekOfMonth, 0.0) + billItem.getQty() : billItem.getQty());
            } else if (hourOfDay < 13) {
                // Between 7 AM to 1 PM
                billItemMap7to1.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(weekOfMonth, billItemMap7to1.get(billItem.getItem().getName()) != null
                                ? billItemMap7to1.get(billItem.getItem().getName()).getOrDefault(weekOfMonth, 0.0) + billItem.getQty() : billItem.getQty());
            } else {
                // Between 1 PM to 7 PM
                billItemMap1to7.computeIfAbsent(billItem.getItem().getName(), k -> new HashMap<>())
                        .put(weekOfMonth, billItemMap1to7.get(billItem.getItem().getName()) != null
                                ? billItemMap1to7.get(billItem.getItem().getName()).getOrDefault(weekOfMonth, 0.0) + billItem.getQty() : billItem.getQty());

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
        String jpql = "SELECT billItem "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

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


        List<BillItem> bi = billItemFacade.findByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        List<ReportTemplateRow> rs = new ArrayList<>();
        for (BillItem billItem : bi) {
            ReportTemplateRow row = new ReportTemplateRow();

            boolean hasInvestigation = billItem.getItem() != null && billItem.getItem() instanceof Investigation;

            if (!hasInvestigation) {
                row.setBillItem(billItem);
                rs.add(row);
            }
        }

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateInvoiceAndReportSerialWiseReport() {
        reportTimerController.trackReportExecution(() -> {
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
        }, FinancialReport.INVOICE_AND_RECEIPT_REPORT, sessionController.getLoggedUser());
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
        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
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


        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateRouteAnalysisReport() {
        reportTimerController.trackReportExecution(() -> {
            bundle = new ReportTemplateRowBundle();

            List<BillTypeAtomic> opdBts = new ArrayList<>();

            opdBts.add(BillTypeAtomic.CC_BILL);
            opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
            opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);


            bundle.setName("Route Analysis Bill Items");
            bundle.setBundleType("billItemList");

            bundle = generateCollectingCenterWiseBillItems(opdBts);

            if (reportType.equalsIgnoreCase("detail")) {
                groupCollectingCenterWiseBillsMonthly();
            } else {
                groupRouteWiseBillsMonthly();
            }
        }, CollectionCenterReport.ROUTE_ANALYSIS_REPORT, sessionController.getLoggedUser());
    }

    private void groupRouteWiseBillsMonthly() {
        Map<Route, Map<YearMonth, Bill>> map = new HashMap<>();
        Set<YearMonth> yearMonthsSet = new HashSet<>();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            Bill bill = row.getBill();
            if (bill == null || bill.getCollectingCentre() == null || bill.getCreatedAt() == null) continue;

            double qtySum = 0;
            double feeSum = 0;
            for (BillItem bi : Optional.ofNullable(bill.getBillItems()).orElse(Collections.emptyList())) {
                qtySum += bi.getQty();
                feeSum += bi.getHospitalFee();
            }

            // Handle refund/cancellation
            if (bill.getBillTypeAtomic() == BillTypeAtomic.CC_BILL_REFUND ||
                    bill.getBillTypeAtomic() == BillTypeAtomic.CC_BILL_CANCELLATION) {
                qtySum = -Math.abs(qtySum);
                feeSum = -Math.abs(feeSum);
            }

            YearMonth ym = YearMonth.from(bill.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            yearMonthsSet.add(ym);

            Route route = bill.getCollectingCentre().getRoute();
            double finalQtySum = qtySum;
            double finalFeeSum = feeSum;
            map.computeIfAbsent(route, r -> new HashMap<>())
                    .compute(ym, (y, existingBill) -> {
                        if (existingBill == null) {
                            Bill newBill = new Bill();
                            newBill.clone(bill);
                            newBill.setQty(finalQtySum);
                            newBill.setTotalHospitalFee(finalFeeSum);
                            return (finalQtySum == 0 && finalFeeSum == 0) ? null : newBill;
                        } else {
                            existingBill.setQty(existingBill.getQty() + finalQtySum);
                            existingBill.setTotalHospitalFee(existingBill.getTotalHospitalFee() + finalFeeSum);
                            return (existingBill.getQty() == 0 && existingBill.getTotalHospitalFee() == 0) ? null : existingBill;
                        }
                    });

            // Clean up null entries
            map.get(route).values().removeIf(Objects::isNull);
            if (map.get(route).isEmpty()) {
                map.remove(route);
            }
        }

        setGroupedRouteWiseBillsMonthly(map);
        setYearMonths(new ArrayList<>(yearMonthsSet));
    }

    private void groupCollectingCenterWiseBillsMonthly() {
        Map<Institution, Map<YearMonth, Bill>> map = new HashMap<>();
        Set<YearMonth> yearMonthSet = new HashSet<>();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            Bill bill = row.getBill();
            if (bill == null || bill.getCollectingCentre() == null || bill.getCreatedAt() == null) continue;

            double qtySum = 0;
            double feeSum = 0;
            List<BillItem> billItems = bill.getBillItems() != null ? bill.getBillItems() : Collections.emptyList();

            for (BillItem bi : billItems) {
                qtySum += bi.getQty();
                feeSum += bi.getHospitalFee();
            }

            if (bill.getBillTypeAtomic() == BillTypeAtomic.CC_BILL_REFUND ||
                    bill.getBillTypeAtomic() == BillTypeAtomic.CC_BILL_CANCELLATION) {
                qtySum = -Math.abs(qtySum);
                feeSum = -Math.abs(feeSum);
            }

            final YearMonth ym = YearMonth.from(bill.getCreatedAt().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate());

            final Institution centre = bill.getCollectingCentre();
            yearMonthSet.add(ym);

            double finalQtySum = qtySum;
            double finalFeeSum = feeSum;
            map.computeIfAbsent(centre, c -> new HashMap<>())
                    .compute(ym, (y, existingBill) -> {
                        if (existingBill == null) {
                            Bill newBill = new Bill();
                            newBill.clone(bill);
                            newBill.setQty(finalQtySum);
                            newBill.setTotalHospitalFee(finalFeeSum);
                            return (finalQtySum == 0 && finalFeeSum == 0) ? null : newBill;
                        } else {
                            double newQty = existingBill.getQty() + finalQtySum;
                            double newFee = existingBill.getTotalHospitalFee() + finalFeeSum;
                            existingBill.setQty(newQty);
                            existingBill.setTotalHospitalFee(newFee);
                            return (newQty == 0 && newFee == 0) ? null : existingBill;
                        }
                    });

            map.computeIfPresent(centre, (c, monthMap) -> {
                monthMap.values().removeIf(Objects::isNull);
                return monthMap.isEmpty() ? null : monthMap;
            });
        }

        setGroupedCollectingCenterWiseBillsMonthly(map);
        setYearMonths(new ArrayList<>(yearMonthSet));
    }

//    private void groupRouteWiseBillsMonthly() {
//        Map<Route, Map<YearMonth, Bill>> map = new HashMap<>();
//        List<YearMonth> yearMonths = new ArrayList<>();
//
//        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
//            Bill bill = row.getBill();
//
//            double billItemQty = Optional.ofNullable(bill.getBillItems())
//                    .orElse(Collections.emptyList())
//                    .stream()
//                    .mapToDouble(BillItem::getQty)
//                    .sum();
//
//            double totalHospitalFee = Optional.ofNullable(bill.getBillItems())
//                    .orElse(Collections.emptyList())
//                    .stream()
//                    .mapToDouble(BillItem::getHospitalFee)
//                    .sum();
//
//            if (bill.getBillTypeAtomic().equals(BillTypeAtomic.CC_BILL_REFUND) || bill.getBillTypeAtomic().equals(BillTypeAtomic.CC_BILL_CANCELLATION)) {
//                if (billItemQty > 0) {
//                    billItemQty = -billItemQty;
//                }
//                if (totalHospitalFee > 0) {
//                    totalHospitalFee = -totalHospitalFee;
//                }
//            }
//
//            final Calendar cal = Calendar.getInstance();
//            cal.setTime(bill.getCreatedAt());
//
//            final YearMonth yearMonth = YearMonth.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
//
//            if (!yearMonths.contains(yearMonth)) {
//                yearMonths.add(yearMonth);
//            }
//
//            Map<YearMonth, Bill> monthMap;
//            if (map.containsKey(bill.getCollectingCentre().getRoute())) {
//                monthMap = map.get(bill.getCollectingCentre().getRoute());
//
//                if (monthMap.containsKey(yearMonth)) {
//                    Bill existingBill = monthMap.get(yearMonth);
//                    existingBill.setTotalHospitalFee(existingBill.getTotalHospitalFee() + totalHospitalFee);
//                    existingBill.setQty(existingBill.getQty() + billItemQty);
//
//                    if (existingBill.getTotalHospitalFee() == 0 && existingBill.getQty() == 0) {
//                        monthMap.remove(yearMonth);
//
//                        if (monthMap.isEmpty()) {
//                            map.remove(bill.getCollectingCentre().getRoute());
//                        }
//                    }
//                } else {
//                    Bill cloneBill = new Bill();
//                    cloneBill.clone(bill);
//                    cloneBill.setQty(billItemQty);
//                    cloneBill.setTotalHospitalFee(totalHospitalFee);
//
//                    if (cloneBill.getTotalHospitalFee() == 0 && cloneBill.getQty() == 0) {
//                        monthMap.remove(yearMonth);
//
//                        if (monthMap.isEmpty()) {
//                            map.remove(bill.getCollectingCentre().getRoute());
//                        }
//                    } else {
//                        monthMap.put(yearMonth, cloneBill);
//                    }
//                }
//            } else {
//                monthMap = new HashMap<>();
//                Bill cloneBill = new Bill();
//                cloneBill.clone(bill);
//                cloneBill.setQty(billItemQty);
//                cloneBill.setTotalHospitalFee(totalHospitalFee);
//
//                if (cloneBill.getTotalHospitalFee() != 0 || cloneBill.getQty() != 0) {
//                    monthMap.put(yearMonth, cloneBill);
//                }
//            }
//
//            if (!monthMap.isEmpty()) {
//                map.put(bill.getCollectingCentre().getRoute(), monthMap);
//            }
//        }
//
//        setGroupedRouteWiseBillsMonthly(map);
//        setYearMonths(yearMonths);
//    }
//
//    private void groupCollectingCenterWiseBillsMonthly() {
//        Map<Institution, Map<YearMonth, Bill>> map = new HashMap<>();
//        List<YearMonth> yearMonths = new ArrayList<>();
//
//        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
//            Bill bill = row.getBill();
//
//            double billItemQty = Optional.ofNullable(bill.getBillItems())
//                    .orElse(Collections.emptyList())
//                    .stream()
//                    .mapToDouble(BillItem::getQty)
//                    .sum();
//
//            double totalHospitalFee = Optional.ofNullable(bill.getBillItems())
//                    .orElse(Collections.emptyList())
//                    .stream()
//                    .mapToDouble(BillItem::getHospitalFee)
//                    .sum();
//
//            if (bill.getBillTypeAtomic().equals(BillTypeAtomic.CC_BILL_REFUND) || bill.getBillTypeAtomic().equals(BillTypeAtomic.CC_BILL_CANCELLATION)) {
//                if (billItemQty > 0) {
//                    billItemQty = -billItemQty;
//                }
//                if (totalHospitalFee > 0) {
//                    totalHospitalFee = -totalHospitalFee;
//                }
//            }
//
//            final Calendar cal = Calendar.getInstance();
//            cal.setTime(bill.getCreatedAt());
//
//            final YearMonth yearMonth = YearMonth.of(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
//
//            if (!yearMonths.contains(yearMonth)) {
//                yearMonths.add(yearMonth);
//            }
//
//            Map<YearMonth, Bill> monthMap;
//            if (map.containsKey(bill.getCollectingCentre())) {
//                monthMap = map.get(bill.getCollectingCentre());
//
//                if (monthMap.containsKey(yearMonth)) {
//                    Bill existingBill = monthMap.get(yearMonth);
//                    existingBill.setTotalHospitalFee(existingBill.getTotalHospitalFee() + totalHospitalFee);
//                    existingBill.setQty(existingBill.getQty() + billItemQty);
//
//                    if (existingBill.getTotalHospitalFee() == 0 && existingBill.getQty() == 0) {
//                        monthMap.remove(yearMonth);
//
//                        if (monthMap.isEmpty()) {
//                            map.remove(bill.getCollectingCentre());
//                        }
//                    }
//                } else {
//                    Bill cloneBill = new Bill();
//                    cloneBill.clone(bill);
//                    cloneBill.setQty(billItemQty);
//                    cloneBill.setTotalHospitalFee(totalHospitalFee);
//
//                    if (cloneBill.getTotalHospitalFee() == 0 && cloneBill.getQty() == 0) {
//                        monthMap.remove(yearMonth);
//
//                        if (monthMap.isEmpty()) {
//                            map.remove(bill.getCollectingCentre());
//                        }
//                    } else {
//                        monthMap.put(yearMonth, cloneBill);
//                    }
//                }
//            } else {
//                monthMap = new HashMap<>();
//                Bill cloneBill = new Bill();
//                cloneBill.clone(bill);
//                cloneBill.setQty(billItemQty);
//                cloneBill.setTotalHospitalFee(totalHospitalFee);
//
//                if (cloneBill.getTotalHospitalFee() != 0 || cloneBill.getQty() != 0) {
//                    monthMap.put(yearMonth, cloneBill);
//                }
//
//            }
//
//            if (!monthMap.isEmpty()) {
//                map.put(bill.getCollectingCentre(), monthMap);
//            }
//        }
//
//        setGroupedCollectingCenterWiseBillsMonthly(map);
//        setYearMonths(yearMonths);
//    }

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
        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
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

        List<?> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpqlWithoutCache(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows((List<ReportTemplateRow>) rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateCollectionCenterWiseInvoiceListReport() {
        reportTimerController.trackReportExecution(() -> {
            bundle = new ReportTemplateRowBundle();

            List<BillTypeAtomic> opdBts = new ArrayList<>();

            opdBts.add(BillTypeAtomic.CC_BILL);
            opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
            opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);
            // If transaction add other CC types

            bundle.setName("Package Bills");
            bundle.setBundleType("billList");

            bundle = generateCollectingCenterWiseBills(opdBts);
            bundle.calculateTotalByHospitalFee();
            bundle.calculateTotalCCFee();
        }, CollectionCenterReport.COLLECTION_CENTER_WISE_INVOICE_LIST_REPORT, sessionController.getLoggedUser());
    }

    public ReportTemplateRowBundle generateCollectingCenterWiseBills(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
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

//    public void generateDebtorBalanceReport(final boolean onlyDueBills) {
//        if (visitType == null || visitType.trim().isEmpty()) {
//            JsfUtil.addErrorMessage("Please select a visit type");
//            return;
//        }
//
//        List<PaymentMethod> paymentMethods = new ArrayList<>();
//        if (methodType.equalsIgnoreCase("Credit")) {
//            paymentMethods.add(PaymentMethod.Credit);
//        } else if (methodType.equalsIgnoreCase("NonCredit")) {
//            paymentMethods.add(PaymentMethod.Cash);
//        } else {
//            addAllPaymentMethods(paymentMethods);
//        }
//
//        bundle = new ReportTemplateRowBundle();
//
//        List<BillTypeAtomic> opdBts = new ArrayList<>();
//        bundle = new ReportTemplateRowBundle();
//
////        if (visitType.equalsIgnoreCase("IP")) {
//////            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
//////            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
//////            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
//////            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
//////            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
////            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
////            opdBts.add(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE_RETURN);
////            opdBts.add(BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION);
////            //TODO: Add other needed bill types
////
////        } else if (visitType.equalsIgnoreCase("OP")) {
//////            opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
////            opdBts.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
////            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
////            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
////            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
////            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
////            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
////            opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
////            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
////            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
////        }
////
////        bundle.setName("Bills");
////        bundle.setBundleType("billList");
////
////        bundle = generateDebtorBalanceReportBills(opdBts, paymentMethods, onlyDueBills);
//        if (visitType.equalsIgnoreCase("IP")) {
////            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
////            opdBts.add(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE_RETURN);
////            opdBts.add(BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION);
//
//            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
//            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
//            opdBts.add(BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION);
//            opdBts.add(BillTypeAtomic.CANCELLED_INWARD_FINAL_BILL);
//        } else if (visitType.equalsIgnoreCase("OP")) {
//            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
//            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
//            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
//            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
//            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
//            opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
//            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
//            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
//            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
//            opdBts.add(BillTypeAtomic.OPD_BILL_REFUND);
//        }
//
//        bundle.setName("Bills");
//        bundle.setBundleType("billList");
//
//        bundle = generateDebtorBalanceBills(opdBts, paymentMethods);
//
//        if (visitType.equalsIgnoreCase("IP")) {
//            updateSettledAmountsForIP();
//        } else if (visitType.equalsIgnoreCase("OP")) {
//            updateSettledAmountsForOP();
//        }
//
//        if (onlyDueBills) {
//            removeNonDues();
//        }
//
//        bundle.calculateTotalByBills(visitType.equalsIgnoreCase("OP"));
//        bundle.calculateTotalBalance(visitType.equalsIgnoreCase("OP"));
//        bundle.calculateTotalSettledAmountByPatients(visitType.equalsIgnoreCase("OP"));
//        bundle.calculateTotalSettledAmountBySponsors(visitType.equalsIgnoreCase("OP"));
//    }

    public void generateDebtorBalanceReport(final boolean onlyDueBills) {
        reportTimerController.trackReportExecution(() -> {
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

            bundle = new ReportTemplateRowBundle();

            bundle.setName("Bills");
            bundle.setBundleType("billList");

            if (visitType.equalsIgnoreCase("IP")) {
                generateDebtorBalanceIPBills(onlyDueBills, paymentMethods);
            } else if (visitType.equalsIgnoreCase("OP")) {
                List<BillTypeAtomic> opdBts = new ArrayList<>();

                opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
                opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
                opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
                opdBts.add(BillTypeAtomic.OPD_BILL_REFUND);

                bundle = generateDebtorBalanceBills(opdBts, paymentMethods);
                updateSettledAmountsForOP();

                if (onlyDueBills) {
                    removeNonDues();
                }

                bundle.calculateTotalByBills(visitType.equalsIgnoreCase("OP"));
                bundle.calculateTotalBalance(visitType.equalsIgnoreCase("OP"));
                bundle.calculateTotalSettledAmountByPatients(visitType.equalsIgnoreCase("OP"));
                bundle.calculateTotalSettledAmountBySponsors(visitType.equalsIgnoreCase("OP"));
            }
        }, FinancialReport.DEBTOR_BALANCE_REPORT, sessionController.getLoggedUser());
    }

    public void exportDebtorBalanceReportIPToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Debtor_Balance_Report_Report.xlsx");

        SimpleDateFormat sdf = new SimpleDateFormat("dd MM yyyy hh:mm:ss a");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Debtor Balance Report");
            int rowIndex = 0;

            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);

            CellStyle boldStyle = workbook.createCellStyle();
            boldStyle.setFont(boldFont);

            Row mainHeader = sheet.createRow(rowIndex++);
            Cell headerCell = mainHeader.createCell(0);
            headerCell.setCellValue("Debtor Balance Report");
            headerCell.setCellStyle(boldStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 15));

            Row columnHeader = sheet.createRow(rowIndex++);
            String[] headers = {
                    "", "BHT", "MRN No", "Phone", "Patient Name", "Admitted At", "Discharged At", "Final Total", "GOP by Patient", "Paid by Patient",
                    "Patient Due", "Paid by Companies", "Total Due", "Company Details"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = columnHeader.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(boldStyle);
            }

            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 13, 18));

            int counter = 1;
            for (Map.Entry<PatientEncounter, List<InstitutionBillEncounter>> entry : institutionBillPatientEncounterMap.entrySet()) {
                PatientEncounter pe = entry.getKey();
                List<InstitutionBillEncounter> bills = entry.getValue();

                if (bills == null || bills.isEmpty()) {
                    continue;
                }

                Row row = sheet.createRow(rowIndex++);
                int col = 0;

                row.createCell(col++).setCellValue(counter++);
                row.createCell(col++).setCellValue(pe.getBhtNo());
                row.createCell(col++).setCellValue(pe.getPatient().getPhn());
                row.createCell(col++).setCellValue(pe.getPatient().getPerson().getMobile());
                row.createCell(col++).setCellValue(pe.getPatient().getPerson().getName());
                row.createCell(col++).setCellValue(sdf.format(pe.getDateOfAdmission()));
                row.createCell(col++).setCellValue(sdf.format(pe.getDateOfDischarge()));
                row.createCell(col++).setCellValue(pe.getFinalBill().getNetTotal());
                row.createCell(col++).setCellValue(bills.get(0).getPatientGopAmount());
                row.createCell(col++).setCellValue(bills.get(0).getPaidByPatient());
                row.createCell(col++).setCellValue(bills.get(0).getPatientDue());
                row.createCell(col++).setCellValue(bills.get(0).getTotalPaidByCompanies());
                row.createCell(col++).setCellValue(bills.get(0).getTotalDue());

                Row subHeader = sheet.createRow(rowIndex++);
                String[] innerHeaders = {
                        "Company Name", "Policy Number", "Reference Number", "GOP by Company",
                        "Paid by Company", "Company Due"
                };
                for (int i = 0; i < innerHeaders.length; i++) {
                    Cell cell = subHeader.createCell(i + 13);
                    cell.setCellValue(innerHeaders[i]);
                    cell.setCellStyle(boldStyle);
                }

                for (InstitutionBillEncounter bill : bills) {
                    Row billRow = sheet.createRow(rowIndex++);
                    billRow.createCell(13).setCellValue(bill.getInstitution().getName());
                    billRow.createCell(14).setCellValue(getPolicyNumberFromEncounterCreditCompanyMap(pe.getBhtNo(), bill.getInstitution().getName()));
                    billRow.createCell(15).setCellValue(getReferenceNumberFromEncounterCreditCompanyMap(pe.getBhtNo(), bill.getInstitution().getName()));
                    billRow.createCell(16).setCellValue(bill.getGopAmount());
                    billRow.createCell(17).setCellValue(bill.getPaidByCompany());
                    billRow.createCell(18).setCellValue(bill.getCompanyDue() != 0 ? bill.getCompanyDue() : bill.getCompanyExcess());
                }

                Row totalsRow = sheet.createRow(rowIndex++);
                Cell totalLabelCell = totalsRow.createCell(13);
                totalLabelCell.setCellValue("Total");
                totalLabelCell.setCellStyle(boldStyle);

                Cell total1 = totalsRow.createCell(16);
                total1.setCellValue(patientEncounterGopMap.get(pe));
                total1.setCellStyle(boldStyle);

                Cell total2 = totalsRow.createCell(17);
                total2.setCellValue(patientEncounterPaidByCompanyMap.get(pe));
                total2.setCellStyle(boldStyle);

                Cell total3 = totalsRow.createCell(18);
                total3.setCellValue(patientEncounterGopMap.get(pe) - patientEncounterPaidByCompanyMap.get(pe));
                total3.setCellStyle(boldStyle);
            }

            Row totalFooter = sheet.createRow(rowIndex++);
            Cell footerLabel = totalFooter.createCell(0);
            footerLabel.setCellValue("Total");
            footerLabel.setCellStyle(boldStyle);
            sheet.addMergedRegion(new CellRangeAddress(rowIndex - 1, rowIndex - 1, 0, 3));

            int[] footerCols = {7, 8, 9, 10, 11, 12};
            double[] footerValues = {
                    getBilled(),
                    getPayableByPatient(),
                    getPaidByPatient(),
                    getPayableByPatient() - getPaidByPatient(),
                    getPaidByCompany(),
                    getBilled() - (getPaidByCompany() + getPaidByPatient())
            };

            for (int i = 0; i < footerCols.length; i++) {
                Cell cell = totalFooter.createCell(footerCols[i]);
                cell.setCellValue(footerValues[i]);
                cell.setCellStyle(boldStyle);
            }

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(ReportsController.class.getName()).log(java.util.logging.Level.SEVERE, e.getMessage());
        }
    }

    public void exportDebtorBalanceReportIPToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Debtor_Balance_Report_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);

            document.open();

            com.itextpdf.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            com.itextpdf.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Debtor Balance Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            SimpleDateFormat longSdf = new SimpleDateFormat(sessionController.getApplicationPreference().getLongDateTimeFormat());

            PdfPTable dateHeaderTable = new PdfPTable(2);
            dateHeaderTable.setWidthPercentage(50);
            dateHeaderTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell fromLabelCell = new PdfPCell(new Phrase("From  ", boldFont));
            fromLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            fromLabelCell.setBorder(Rectangle.NO_BORDER);
            dateHeaderTable.addCell(fromLabelCell);

            PdfPCell fromDateCell = new PdfPCell(new Phrase(longSdf.format(fromDate), normalFont));
            fromDateCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            fromDateCell.setBorder(Rectangle.NO_BORDER);
            dateHeaderTable.addCell(fromDateCell);

            PdfPCell toLabelCell = new PdfPCell(new Phrase("To  ", boldFont));
            toLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            toLabelCell.setBorder(Rectangle.NO_BORDER);
            dateHeaderTable.addCell(toLabelCell);

            PdfPCell toDateCell = new PdfPCell(new Phrase(longSdf.format(toDate), normalFont));
            toDateCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            toDateCell.setBorder(Rectangle.NO_BORDER);
            dateHeaderTable.addCell(toDateCell);

            document.add(dateHeaderTable);

            PdfPTable mainTable = new PdfPTable(14);
            mainTable.setWidthPercentage(100);

            mainTable.setWidths(new float[]{0.6f, 1.2f, 1.2f, 1.2f, 1.2f, 1.8f, 1.8f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 1.2f, 8.0f});


            String[] headers = {"", "BHT", "MRN No", "Phone", "Patient Name", "Admitted At", "Discharged At", "Final Total",
                    "GOP by Patient", "Paid by Patient", "Patient Due", "Paid by Companies", "Total Due", "Company Details"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                mainTable.addCell(cell);
            }

            int counter = 1;
            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss a");

            for (Map.Entry<PatientEncounter, List<InstitutionBillEncounter>> entry : institutionBillPatientEncounterMap.entrySet()) {
                PatientEncounter pe = entry.getKey();
                List<InstitutionBillEncounter> bills = entry.getValue();

                mainTable.addCell(new Phrase(String.valueOf(counter++), normalFont));
                mainTable.addCell(new Phrase(pe.getBhtNo(), normalFont));
                mainTable.addCell(new Phrase(pe.getPatient().getPhn(), normalFont));
                mainTable.addCell(new Phrase(pe.getPatient().getPerson().getMobile(), normalFont));
                mainTable.addCell(new Phrase(pe.getPatient().getPerson().getName(), normalFont));
                mainTable.addCell(new Phrase(sdf.format(pe.getDateOfAdmission()), normalFont));
                mainTable.addCell(new Phrase(sdf.format(pe.getDateOfDischarge()), normalFont));
                mainTable.addCell(new Phrase(String.valueOf(pe.getFinalBill().getNetTotal()), normalFont));
                mainTable.addCell(new Phrase(String.valueOf(bills.get(0).getPatientGopAmount()), normalFont));
                mainTable.addCell(new Phrase(String.valueOf(bills.get(0).getPaidByPatient()), normalFont));
                mainTable.addCell(new Phrase(String.valueOf(bills.get(0).getPatientDue()), normalFont));
                mainTable.addCell(new Phrase(String.valueOf(bills.get(0).getTotalPaidByCompanies()), normalFont));
                mainTable.addCell(new Phrase(String.valueOf(bills.get(0).getTotalDue()), normalFont));

                PdfPTable nestedTable = new PdfPTable(6);
                nestedTable.setWidthPercentage(100);
                nestedTable.setWidths(new float[]{3f, 2.5f, 2.5f, 2f, 2f, 2f});

                String[] subHeaders = {"Company Name", "Policy Number", "Reference Number", "GOP by Company", "Paid by Company", "Company Due"};
                for (String sh : subHeaders) {
                    PdfPCell shCell = new PdfPCell(new Phrase(sh, boldFont));
                    shCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    nestedTable.addCell(shCell);
                }

                for (InstitutionBillEncounter bill : bills) {
                    nestedTable.addCell(new Phrase(bill.getInstitution().getName(), normalFont));
                    nestedTable.addCell(new Phrase(getPolicyNumberFromEncounterCreditCompanyMap(pe.getBhtNo(), bill.getInstitution().getName()), normalFont));
                    nestedTable.addCell(new Phrase(getReferenceNumberFromEncounterCreditCompanyMap(pe.getBhtNo(), bill.getInstitution().getName()), normalFont));
                    nestedTable.addCell(new Phrase(String.valueOf(bill.getGopAmount()), normalFont));
                    nestedTable.addCell(new Phrase(String.valueOf(bill.getPaidByCompany()), normalFont));
                    nestedTable.addCell(new Phrase(String.valueOf(bill.getCompanyDue() != 0 ? bill.getCompanyDue() : bill.getCompanyExcess()), normalFont));
                }

                PdfPCell nestedTotalLabel = new PdfPCell(new Phrase("Total", boldFont));
                nestedTotalLabel.setColspan(3);
                nestedTotalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
                nestedTable.addCell(nestedTotalLabel);

                nestedTable.addCell(new Phrase(String.valueOf(patientEncounterGopMap.get(pe)), boldFont));
                nestedTable.addCell(new Phrase(String.valueOf(patientEncounterPaidByCompanyMap.get(pe)), boldFont));
                nestedTable.addCell(new Phrase(String.valueOf(patientEncounterGopMap.get(pe) - patientEncounterPaidByCompanyMap.get(pe)), boldFont));

                PdfPCell nestedCell = new PdfPCell(nestedTable);
                nestedCell.setColspan(1);
                mainTable.addCell(nestedCell);
            }

            PdfPCell footerLabel = new PdfPCell(new Phrase("Total", boldFont));
            footerLabel.setColspan(7);
            footerLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
            mainTable.addCell(footerLabel);

            mainTable.addCell(new Phrase(String.valueOf(getBilled()), boldFont));
            mainTable.addCell(new Phrase(String.valueOf(getPayableByPatient()), boldFont));
            mainTable.addCell(new Phrase(String.valueOf(getPaidByPatient()), boldFont));
            mainTable.addCell(new Phrase(String.valueOf(getPayableByPatient() - getPaidByPatient()), boldFont));
            mainTable.addCell(new Phrase(String.valueOf(getPaidByCompany()), boldFont));
            mainTable.addCell(new Phrase(String.valueOf(getBilled() - (getPaidByCompany() + getPaidByPatient())), boldFont));
            mainTable.addCell(new PdfPCell(new Phrase("")));

            document.add(mainTable);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(ReportsController.class.getName()).log(Level.SEVERE, e.getMessage());
        }
    }

    public void exportDebtorBalanceReportOPToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Debtor_Balance_Report_OP.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);

            document.open();

            com.itextpdf.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            com.itextpdf.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            Paragraph title = new Paragraph("Debtor Balance Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18));
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            SimpleDateFormat longSdf = new SimpleDateFormat(sessionController.getApplicationPreference().getLongDateTimeFormat());

            PdfPTable dateHeaderTable = new PdfPTable(2);
            dateHeaderTable.setWidthPercentage(50);
            dateHeaderTable.setHorizontalAlignment(Element.ALIGN_CENTER);

            PdfPCell fromLabelCell = new PdfPCell(new Phrase("From  ", boldFont));
            fromLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            fromLabelCell.setBorder(Rectangle.NO_BORDER);
            dateHeaderTable.addCell(fromLabelCell);

            PdfPCell fromDateCell = new PdfPCell(new Phrase(longSdf.format(fromDate), normalFont));
            fromDateCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            fromDateCell.setBorder(Rectangle.NO_BORDER);
            dateHeaderTable.addCell(fromDateCell);

            PdfPCell toLabelCell = new PdfPCell(new Phrase("To  ", boldFont));
            toLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            toLabelCell.setBorder(Rectangle.NO_BORDER);
            dateHeaderTable.addCell(toLabelCell);

            PdfPCell toDateCell = new PdfPCell(new Phrase(longSdf.format(toDate), normalFont));
            toDateCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            toDateCell.setBorder(Rectangle.NO_BORDER);
            dateHeaderTable.addCell(toDateCell);

            document.add(dateHeaderTable);

            PdfPTable mainTable = new PdfPTable(11);
            mainTable.setWidthPercentage(100);

            mainTable.setWidths(new float[]{0.5f, 1.5f, 1.0f, 1.5f, 2.5f, 1.2f, 2.5f, 1.2f, 1.2f, 1.2f, 1.2f});

            String[] headers = {
                    "No", "Bill Date", "MRN No", "Payment Method", "Credit Company Name",
                    "Phone", "Patient Name", "Final Bill Total", "Paid By Patient",
                    "Credit Paid Amount", "Due Amount"
            };
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                mainTable.addCell(cell);
            }

            int counter = 1;
            SimpleDateFormat shortSdf = new SimpleDateFormat(sessionController.getApplicationPreference().getShortDateFormat());
            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");


            for (ReportTemplateRow rowData : bundle.getReportTemplateRows()) {
                mainTable.addCell(new Phrase(String.valueOf(counter++), normalFont));
                mainTable.addCell(new Phrase(shortSdf.format(rowData.getBill().getCreatedAt()), normalFont));
                mainTable.addCell(new Phrase(rowData.getBill().getPatient().getPhn(), normalFont));
                mainTable.addCell(new Phrase(rowData.getBill().getPaymentMethod() != null ? rowData.getBill().getPaymentMethod().toString() : "", normalFont));
                mainTable.addCell(new Phrase(rowData.getBill().getCreditCompany() != null ? rowData.getBill().getCreditCompany().getName() : "", normalFont));
                mainTable.addCell(new Phrase(rowData.getBill().getPatient().getPerson().getMobile(), normalFont));
                mainTable.addCell(new Phrase(rowData.getBill().getPatient().getPerson().getName(), normalFont));

                mainTable.addCell(new Phrase(decimalFormat.format(rowData.getBill().getNetTotal()), normalFont));
                mainTable.addCell(new Phrase(decimalFormat.format(rowData.getBill().getSettledAmountByPatient()), normalFont));
                mainTable.addCell(new Phrase(decimalFormat.format(rowData.getBill().getSettledAmountBySponsor()), normalFont));

                double dueAmount = rowData.getBill().getNetTotal() - rowData.getBill().getSettledAmountByPatient() - rowData.getBill().getSettledAmountBySponsor();
                mainTable.addCell(new Phrase(decimalFormat.format(dueAmount), normalFont));
            }

            PdfPCell footerLabelColSpan = new PdfPCell(new Phrase("Total", boldFont));
            footerLabelColSpan.setColspan(7);
            footerLabelColSpan.setHorizontalAlignment(Element.ALIGN_RIGHT);
            mainTable.addCell(footerLabelColSpan);

            mainTable.addCell(new Phrase(decimalFormat.format(bundle.getTotal()), boldFont));
            mainTable.addCell(new Phrase(decimalFormat.format(bundle.getSettledAmountByPatientsTotal()), boldFont));
            mainTable.addCell(new Phrase(decimalFormat.format(bundle.getSettledAmountBySponsorsTotal()), boldFont));
            mainTable.addCell(new Phrase(decimalFormat.format(bundle.getTotalBalance()), boldFont));

            document.add(mainTable);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Error exporting Debtor Balance Report OP to PDF", e);
        }
    }

    private void removeNonDues() {
        List<ReportTemplateRow> removeList = new ArrayList<>();

        if (visitType.equalsIgnoreCase("IP")) {
            for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
                Bill bill = row.getBill();

                if (bill.getBillClassType().equals(BillClassType.CancelledBill)) {
                    if (bill.getNetTotal() - bill.getSettledAmountByPatient() - bill.getSettledAmountBySponsor() <= 0) {
                        removeList.add(row);
                    }
                } else {
                    if (bill.getPatientEncounter().getFinalBill().getNetTotal() - bill.getPatientEncounter().getFinalBill().getSettledAmountByPatient()
                            - bill.getPatientEncounter().getFinalBill().getSettledAmountBySponsor() <= 0) {
                        removeList.add(row);
                    }
                }

            }
        } else if (visitType.equalsIgnoreCase("OP")) {
            for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
                Bill bill = row.getBill();
                if (bill.getBillClassType().equals(BillClassType.CancelledBill) || bill.getBillClassType().equals(BillClassType.RefundBill)) {
                    if (bill.getNetTotal() - bill.getSettledAmountByPatient() - bill.getSettledAmountBySponsor() >= 0) {
                        removeList.add(row);
                    }
                } else {
                    if (bill.getNetTotal() - bill.getSettledAmountByPatient() - bill.getSettledAmountBySponsor() <= 0) {
                        removeList.add(row);
                    }
                }

            }
        }

        bundle.getReportTemplateRows().removeAll(removeList);
    }

    public ReportTemplateRowBundle generateDebtorBalanceReportBills(List<BillTypeAtomic> bts, List<PaymentMethod> billPaymentMethods,
                                                                    boolean onlyDueBills) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
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
        reportTimerController.trackReportExecution(() -> {
            if (visitType == null || visitType.trim().isEmpty()) {
                JsfUtil.addErrorMessage("Please select a visit type");
                return;
            }

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

            if (reportType.equalsIgnoreCase("summary")) {
                bundle = generateReportBill(opdBts, null);
                bundle.calculateTotalByRefBills(visitType.equalsIgnoreCase("OP"));
            } else {
                bundle = generateReportBillItems(opdBts, null);
                bundle.calculateTotalByReferenceBills(visitType.equalsIgnoreCase("OP"));
            }
        }, FinancialReport.PAYMENT_SETTLEMENT_REPORT, sessionController.getLoggedUser());
    }

    public ReportTemplateRowBundle generateReportBillItems(List<BillTypeAtomic> bts, List<PaymentMethod> billPaymentMethods) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(billItem) "
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
            jpql += "AND billItem.patientEncounter.admissionType = :type ";
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
                jpql += "AND billItem.bill.creditCompany = :creditC ";
            }

            parameters.put("creditC", creditCompany);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpqlWithoutCache(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public ReportTemplateRowBundle generateReportBill(List<BillTypeAtomic> bts, List<PaymentMethod> billPaymentMethods) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (billPaymentMethods != null) {
            jpql += "AND bill.paymentMethod in :bpms ";
            parameters.put("bpms", billPaymentMethods);
        }

//        if (visitType != null && (visitType.equalsIgnoreCase("IP") && admissionType != null)) {
//            jpql += "AND billItem.patientEncounter.admissionType = :type ";
//            parameters.put("type", admissionType);
//        }
//
//        if (visitType != null && (visitType.equalsIgnoreCase("IP") && roomCategory != null)) {
//            jpql += "AND bill.patientEncounter.currentPatientRoom.roomFacilityCharge.roomCategory = :category ";
//            parameters.put("category", roomCategory);
//        }
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
                jpql += "AND bill.creditCompany = :creditC ";
            } else if (visitType != null && visitType.equalsIgnoreCase("IP")) {
                jpql += "AND bill.creditCompany = :creditC ";
            }

            parameters.put("creditC", creditCompany);
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY bill";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpqlWithoutCache(jpql, parameters, TemporalType.TIMESTAMP);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(rs);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void generateCollectionCenterBookWiseDetailReport() {
        reportTimerController.trackReportExecution(() -> {
            bundle = new ReportTemplateRowBundle();
            if (isWithoutDateRange()) {
                if (collectingCentre == null) {
                    JsfUtil.addErrorMessage("Please select a collection center if processing without date range!");
                    return;
                }

                if (cashBookNumber == null || cashBookNumber.isEmpty()) {
                    JsfUtil.addErrorMessage("Please enter a book number if processing without date range!");
                    return;
                }
            }

            bundle = new ReportTemplateRowBundle();

            List<BillTypeAtomic> opdBts = new ArrayList<>();
            bundle = new ReportTemplateRowBundle();

            opdBts.add(BillTypeAtomic.CC_BILL);
            opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);
            opdBts.add(BillTypeAtomic.CC_BILL_REFUND);

            bundle.setName("Bills");
            bundle.setBundleType("billList");

            bundle = generateCollectionCenterBookWiseBills(opdBts);
        }, CollectionCenterReport.COLLECTION_CENTER_BOOK_WISE_DETAIL_REPORT, sessionController.getLoggedUser());
    }

    public void exportCollectionCenterBookWiseDetailToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Collection_center_book_wise_detail_report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            com.itextpdf.text.Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            Paragraph title = new Paragraph("Collection Center Book Wise Detail", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10f);
            document.add(title);

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");

            if (getCollectingCentre() != null) {
                Paragraph centerName = new Paragraph("Collection Center: " + getCollectingCentre().getName(), cellFont);
                centerName.setSpacingAfter(10f);
                document.add(centerName);
            }

            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 1f, 2f, 3f, 3f, 3f, 2f});

            String[] headers = {
                    "Bill No", "Book No", "Book Ref No", "Patient",
                    "Creator", "Created Date", "Bill Value"
            };

            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                table.addCell(cell);
            }

            DecimalFormat df = new DecimalFormat("#,##0.00");

            List<ReportTemplateRow> exportRows;
            if (filteredReportTemplateRows != null && !filteredReportTemplateRows.isEmpty()) {
                exportRows = filteredReportTemplateRows;
            } else {
                exportRows = getBundle().getReportTemplateRows();
            }

            for (ReportTemplateRow c : exportRows) {
                table.addCell(new PdfPCell(new Phrase(c.getBill().getDeptId(), cellFont)));
                String referenceNumber = c.getBill().getReferenceNumber() != null ? c.getBill().getReferenceNumber() :
                        c.getBill().getBilledBill().getReferenceNumber();
                String bookNumber = collectingCentreBillController.generateBookNumberFromReference(referenceNumber);
                table.addCell(new PdfPCell(new Phrase(bookNumber != null ? bookNumber : "N/A", cellFont)));
                table.addCell(new PdfPCell(new Phrase(referenceNumber != null ? referenceNumber : "N/A", cellFont)));
                table.addCell(new PdfPCell(new Phrase(
                        c.getBill().getPatient().getPerson().getNameWithTitle(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(
                        c.getBill().getCreater().getWebUserPerson().getName(), cellFont)));
                table.addCell(new PdfPCell(new Phrase(
                        dateFormat.format(c.getBill().getCreatedAt()), cellFont)));

                PdfPCell netTotalCell = new PdfPCell(new Phrase(
                        df.format(c.getBill().getNetTotal()), cellFont));
                netTotalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(netTotalCell);
            }

            PdfPCell totalLabel = new PdfPCell(new Phrase("Gross Total", headerFont));
            totalLabel.setColspan(6);
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalLabel);

            PdfPCell totalValue = new PdfPCell(new Phrase(
                    df.format(getBundle().getGrossTotal()), cellFont));
            totalValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totalValue);

            document.add(table);
            document.close();
            context.responseComplete();

        } catch (Exception e) {
            Logger.getLogger(CollectingCentreBillController.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public void exportCollectionCenterBookWiseDetailToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Collection_center_book_wise_detail_report.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Collection Center Report");
            int rowIndex = 0;

            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss");

            XSSFCellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            XSSFFont headerFont = workbook.createFont();
            headerFont.setBold(true);
            XSSFCellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(rowIndex++);
            String[] headers = {"Bill No", "Book No", "Book Ref No", "Patient", "Creator", "Created Date", "Bill Value"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            List<ReportTemplateRow> exportRows =
                    (filteredReportTemplateRows != null && !filteredReportTemplateRows.isEmpty())
                            ? filteredReportTemplateRows
                            : getBundle().getReportTemplateRows();

            DecimalFormat df = new DecimalFormat("#,##0.00");

            for (ReportTemplateRow c : exportRows) {
                Row dataRow = sheet.createRow(rowIndex++);

                dataRow.createCell(0).setCellValue(c.getBill().getDeptId());
                String referenceNumber = c.getBill().getReferenceNumber() != null ? c.getBill().getReferenceNumber() :
                        c.getBill().getBilledBill().getReferenceNumber();
                String bookNumber = collectingCentreBillController.generateBookNumberFromReference(referenceNumber);
                dataRow.createCell(1).setCellValue(bookNumber != null ? bookNumber : "N/A");
                dataRow.createCell(2).setCellValue(referenceNumber != null ? referenceNumber : "N/A");
                dataRow.createCell(3).setCellValue(c.getBill().getPatient().getPerson().getNameWithTitle());
                dataRow.createCell(4).setCellValue(c.getBill().getCreater().getWebUserPerson().getName());
                dataRow.createCell(5).setCellValue(dateFormat.format(c.getBill().getCreatedAt()));

                Cell billValueCell = dataRow.createCell(6);
                billValueCell.setCellValue(c.getBill().getNetTotal());
                billValueCell.setCellStyle(amountStyle);
            }

            Row totalRow = sheet.createRow(rowIndex++);
            Cell labelCell = totalRow.createCell(0);
            labelCell.setCellValue("Gross Total");
            labelCell.setCellStyle(headerStyle);
            sheet.addMergedRegion(new CellRangeAddress(totalRow.getRowNum(), totalRow.getRowNum(), 0, 5));

            Cell totalCell = totalRow.createCell(6);
            totalCell.setCellValue(getBundle().getGrossTotal());
            totalCell.setCellStyle(amountStyle);

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            context.responseComplete();

        } catch (Exception e) {
            Logger.getLogger(CollectingCentreBillController.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public ReportTemplateRowBundle generateCollectionCenterBookWiseBills(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();
        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
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

        if (!withoutDateRange) {
            if (fromDate != null && toDate != null) {
                jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
                parameters.put("fd", fromDate);
                parameters.put("td", toDate);
            }
        }

//        jpql += "GROUP BY bill";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

//        if (cashBookNumber != null && !cashBookNumber.trim().isEmpty()) {
//            rs = rs.stream()
//                    .filter(r -> {
//                        String bookNumber = collectingCentreBillController.generateBookNumberFromReference(r.getBill().getReferenceNumber());
//                        return bookNumber != null && bookNumber.contains(cashBookNumber);
//                    })
//                    .collect(Collectors.toList());
//        }

        Map<String, AgentReferenceBook> agentReferenceBooks = getAgentReferenceBookMapByReportTemplateRows(rs);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(filterReportTemplateRowsByEnabledStatus(agentReferenceBooks, rs));
        b.createRowValuesFromBill();
        b.calculateTotalsWithCredit();
        return b;
    }

    private List<ReportTemplateRow> filterReportTemplateRowsByEnabledStatus(final Map<String, AgentReferenceBook> agentReferenceBooks,
                                                                            final List<ReportTemplateRow> rows) {

        if (agentReferenceBooks == null || agentReferenceBooks.isEmpty() || rows == null || rows.isEmpty()) {
            return rows;
        }

        Map<ReportTemplateRow, String> rowToBookNumber = new HashMap<>(rows.size());

        for (ReportTemplateRow row : rows) {
            if (row == null || row.getBill() == null) continue;

            Bill bill = resolveEffectiveBill(row.getBill());
            String refNo = bill.getReferenceNumber();

            if (refNo == null || refNo.length() <= 2) continue;

            String bookNumber = collectingCentreBillController.generateBookNumberFromReference(refNo);
            if (bookNumber != null) {
                rowToBookNumber.put(row, bookNumber);
            }
        }

        List<ReportTemplateRow> filteredRows = new ArrayList<>();

        for (Map.Entry<ReportTemplateRow, String> entry : rowToBookNumber.entrySet()) {
            ReportTemplateRow row = entry.getKey();
            String bookNumber = entry.getValue();

            AgentReferenceBook arb = agentReferenceBooks.get(bookNumber);
            if (arb == null) continue;

            if (!withDeletedBooks && arb.isRetired()) continue;
            if (!withInactiveBooks && !arb.getActive() && !arb.isRetired()) continue;

            row.setAgentReferenceBook(arb);
            filteredRows.add(row);
        }

        return filteredRows;
    }

    private Bill resolveEffectiveBill(Bill bill) {
        if (bill.getBillClassType() == BillClassType.CancelledBill ||
                bill.getBillClassType() == BillClassType.RefundBill) {
            return bill.getBilledBill() != null ? bill.getBilledBill() : bill;
        }
        return bill;
    }

    private Map<String, AgentReferenceBook> getAgentReferenceBookMapByReportTemplateRows(List<ReportTemplateRow> rows) {
        Map<String, AgentReferenceBook> agentReferenceBookMap = new HashMap<>();

        if (rows != null && !rows.isEmpty()) {
            Set<String> strBookNumbers = rows.stream()
                    .map(r -> {
                        String refNo = r.getBill().getReferenceNumber();

                        if (refNo != null && refNo.length() > 2) {
                            return collectingCentreBillController.generateBookNumberFromReference(refNo);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (!strBookNumbers.isEmpty()) {
                String jpqlBook = "SELECT arb FROM AgentReferenceBook arb WHERE arb.strbookNumber IN :sbns";
                Map<String, Object> bookParams = new HashMap<>();
                bookParams.put("sbns", strBookNumbers);

                List<AgentReferenceBook> agentReferenceBooks = agentReferenceBookFacade.findByJpql(jpqlBook, bookParams);

                agentReferenceBookMap = agentReferenceBooks.stream()
                        .filter(arb -> arb.getStrbookNumber() != null)
                        .collect(Collectors.toMap(
                                AgentReferenceBook::getStrbookNumber,
                                Function.identity(),
                                (existing, replacement) -> existing
                        ));
            }
        }

        return agentReferenceBookMap;
    }

    public void generateCollectionCenterBillWiseDetailReport() {
        reportTimerController.trackReportExecution(() -> {
            if (collectingCentre == null && !getWebUserController().hasPrivilege("Developers")) {
                JsfUtil.addErrorMessage("Please select an Agent");
                return;
            }

            bundle = new ReportTemplateRowBundle();

            List<BillTypeAtomic> opdBts = new ArrayList<>();

            opdBts.add(BillTypeAtomic.CC_BILL);
            opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
            opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);


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
        }, CollectionCenterReport.COLLECTION_CENTER_BILL_WISE_DETAIL_REPORT, sessionController.getLoggedUser());
    }

    /**
     * This method is a temporary fix for incorrectly assigned Department and
     * Institution fields on Bills. Previously, there was an error in the
     * workflow that assigned the wrong Department and Institution values. That
     * workflow has been corrected, but older records remain invalid. This
     * method corrects those records for a given ReportBundle (bundle).
     * <p>
     * Usage Notes: 1. The method iterates over the groupedBillItems map (key:
     * String, value: List of BillItem). 2. For each map entry, we only look at
     * the first BillItem in the list to fix that Bill. 3. We derive the correct
     * Department and Institution from bill.getCreater() (the user who created
     * the bill). 4. The 'retireComments' field is set to log the update that
     * occurred, referencing the old Department details. 5. The Bill is then
     * persisted via billFacade.edit(bill). 6. Once all records are corrected,
     * this method should be removed from the codebase.
     * <p>
     * Limitations: - Assumes that the first BillItem in each list (entry.value)
     * is sufficient for determining which Bill to fix. - Only updates the Bill
     * at index 0; if there are multiple BillItems, they presumably share the
     * same Bill object anyway.
     */
    public void fixBillingDepartmentInCollectionCenterBillWiseDetailReport() {
        if (bundle == null) {
            JsfUtil.addErrorMessage("No bundle");
            return;
        }
        if (bundle.getGroupedBillItems() == null) {
            JsfUtil.addErrorMessage("Grouped Bill Items NULL");
            return;
        }
        if (bundle.getGroupedBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("Grouped Bill Items is empty");
            return;
        }

        // Iterate over each entry in the map. The map key is a String (often a grouping identifier),
        // and the map value is a list of BillItems. We only need the first BillItem per entry
        // because it references the Bill we want to fix.
        for (Map.Entry<String, List<BillItem>> entry : bundle.getGroupedBillItems().entrySet()) {
            String key = entry.getKey();
            List<BillItem> billItems = entry.getValue();

            // Skip any entry that doesn't have actual BillItems.
            if (billItems == null || billItems.isEmpty()) {
                continue;
            }

            // Retrieve the Bill from the first BillItem in the list.
            Bill bill = billItems.get(0).getBill();
            if (bill != null) {
                // Assign the correct Department and Institution from the Bill's creator.
                Department oldDept = bill.getDepartment();
                bill.setDepartment(bill.getCreater().getDepartment());
                bill.setInstitution(bill.getCreater().getInstitution());

                // Log the update in retireComments for future reference.
                bill.setRetireComments("Bill's Department changed from "
                        + (oldDept != null ? oldDept.toString() : "Unknown Dept")
                        + " to " + bill.getDepartment()
                        + " (Dept ID was " + (oldDept != null ? oldDept.getId() : "null") + ").");

                // Persist the changes in the database.
                billFacade.edit(bill);
            }
        }
    }

    private void groupBillItems() {
        Map<String, List<BillItem>> billItemMap = new HashMap<>();

        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            BillItem billItem1 = row.getBillItem();

            if (billItem1.getBill() == null || billItem1.getBill().getDeptId() == null) {
                continue;
            }

            if (billItemMap.containsKey(billItem1.getBill().getDeptId())) {
                billItemMap.get(billItem1.getBill().getDeptId()).add(billItem1);
            } else {
                List<BillItem> billItems = new ArrayList<>();
                billItems.add(billItem1);
                billItemMap.put(billItem1.getBill().getDeptId(), billItems);
            }
        }

        Map<String, List<BillItem>> sortedBillItemMap = billItemMap.entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().get(0).getBill().getCreatedAt()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));

        bundle.setGroupedBillItems(sortedBillItemMap);
    }

    // Added methods to calculate totals manually since total is not stored in db for old records correctly
    public Double calculateTotalHospitalFeeByBillItems(final List<BillItem> billItems) {
        double totalHospitalFee = 0;

        for (BillItem billItem : billItems) {
            totalHospitalFee += billItem.getHospitalFee();
        }

        return totalHospitalFee;
    }

    public Double calculateTotalStaffFeeByBillItems(final List<BillItem> billItems) {
        double totalStaffFee = 0;

        for (BillItem billItem : billItems) {
            totalStaffFee += billItem.getStaffFee();
        }

        return totalStaffFee;
    }

    public Double calculateTotalCollectionCenterFeeByBillItems(final List<BillItem> billItems) {
        double totalecectionCenterFee = 0;

        for (BillItem billItem : billItems) {
            totalecectionCenterFee += billItem.getCollectingCentreFee();
        }

        return totalecectionCenterFee;
    }

    public Double calculateTotalNetValueByBillItems(final List<BillItem> billItems) {
        double totalNetValue = 0;

        for (BillItem billItem : billItems) {
            totalNetValue += billItem.getNetValue();
        }

        return totalNetValue;
    }

    //Correct
    public ReportTemplateRowBundle generateCollectingCenterBillWiseBillItems(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br ";

        parameters.put("bfr", true);
        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (institution != null) {
            jpql += "AND bill.toInstitution = :ins ";
            parameters.put("ins", institution);
        }

        if (department != null) {
            jpql += "AND (bill.toDepartment = :dep or bill.department = :dep) ";
            parameters.put("dep", department);
        }

        if (site != null) {
            jpql += "AND (bill.toDepartment.site = :site or bill.department.site = :site)";
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
        reportTimerController.trackReportExecution(() -> {
            if (visitType == null || visitType.trim().isEmpty()) {
                JsfUtil.addErrorMessage("Please select a visit type");
                return;
            }

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
        }, FinancialReport.CREDIT_INVOICE_DISPATCH_REPORT, sessionController.getLoggedUser());
    }

    public ReportTemplateRowBundle generateCreditInvoiceDispatchBillItems(List<BillTypeAtomic> bts, List<PaymentMethod> billPaymentMethods) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(billItem) "
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
        reportTimerController.trackReportExecution(() -> {
            if (visitType == null || visitType.trim().isEmpty()) {
                JsfUtil.addErrorMessage("Please select a visit type");
                return;
            }

            bundle = new ReportTemplateRowBundle();

            List<BillTypeAtomic> opdBts = new ArrayList<>();
            bundle = new ReportTemplateRowBundle();

            if (visitType != null && visitType.equalsIgnoreCase("IP")) {
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
                opdBts.add(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE_RETURN);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);
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
                opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.OPD_BILL_REFUND);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
            }
            if (visitType != null && visitType.equalsIgnoreCase("CC")) {
                opdBts.add(BillTypeAtomic.CC_BILL);
                opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
                opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);
            }

            bundle.setName("Bills");
            bundle.setBundleType("billList");

            if (reportType.equalsIgnoreCase("detail")) {
                bundle = generateExternalLaboratoryWorkloadBillItems(opdBts, true);

                bundle.calculateTotalByBillItemsNetTotal();
            } else {
                bundle = generateExternalLaboratoryWorkloadSummaryBillItems(opdBts, true);

                bundle.calculateTotalByBillItemRowValues();
            }
        }, LaboratoryReport.EXTERNAL_LABORATORY_WORKLOAD_REPORT, sessionController.getLoggedUser());
    }

    public void laboratoryWorkloadReport() {
        reportTimerController.trackReportExecution(() -> {
            if (visitType == null || visitType.trim().isEmpty()) {
                JsfUtil.addErrorMessage("Please select a visit type");
                return;
            }

            bundle = new ReportTemplateRowBundle();

            List<BillTypeAtomic> opdBts = new ArrayList<>();
            bundle = new ReportTemplateRowBundle();

            if (visitType != null && visitType.equalsIgnoreCase("IP")) {
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
                opdBts.add(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE_RETURN);
                opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);
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
                opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
                opdBts.add(BillTypeAtomic.OPD_BILL_REFUND);
                opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
            }
            if (visitType != null && visitType.equalsIgnoreCase("CC")) {
                opdBts.add(BillTypeAtomic.CC_BILL);
                opdBts.add(BillTypeAtomic.CC_BILL_REFUND);
                opdBts.add(BillTypeAtomic.CC_BILL_CANCELLATION);
            }

            bundle.setName("Bills");
            bundle.setBundleType("billList");

            if (reportType.equalsIgnoreCase("detail")) {
                bundle = generateExternalLaboratoryWorkloadBillItems(opdBts, false);

                bundle.calculateTotalByBillItemsNetTotal();
            } else {
                bundle = generateExternalLaboratoryWorkloadSummaryBillItems(opdBts, false);

                bundle.calculateTotalByBillItemRowValues();
            }
        }, LaboratoryReport.LABORATORY_WORKLOAD_REPORT, sessionController.getLoggedUser());
    }

    private List<BillTypeAtomic> cancelAndRefundBillTypeAtomics() {
        return Arrays.asList(
                BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION,
                BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION,
                BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE_RETURN,
                BillTypeAtomic.INWARD_SERVICE_BILL_REFUND,
                BillTypeAtomic.OPD_BILL_CANCELLATION,
                BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION,
                BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION,
                BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION,
                BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION,
                BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION,
                BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION,
                BillTypeAtomic.OPD_BILL_REFUND,
                BillTypeAtomic.PACKAGE_OPD_BILL_REFUND,
                BillTypeAtomic.CC_BILL_CANCELLATION,
                BillTypeAtomic.CC_BILL_REFUND
        );
    }

    private ReportTemplateRowBundle generateExternalLaboratoryWorkloadBillItems(List<BillTypeAtomic> bts, boolean externalLaboratoryOnly) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "LEFT JOIN PatientInvestigation pi ON pi.billItem = billItem "
                + "WHERE bill.billTypeAtomic IN :bts "
                + "AND billItem.item is not null "
                + "AND billItem.bill.cancelled =:canceled "
                + "AND billItem.refunded =:ref "
                + "AND TYPE(billItem.item) = Investigation "
                + "AND (TYPE(bill) != RefundBill AND TYPE(bill) != CancelledBill) ";
//        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(billItem) "
//                + "FROM PatientInvestigation pi "
//                + "JOIN pi.billItem billItem "
//                + "JOIN billItem.bill bill "
//                + "WHERE pi.retired=false "
//                + " AND billItem.retired=false "
//                + " AND bill.retired=false "
//                + " AND bill.billTypeAtomic in :bts ";

        parameters.put("bts", bts);
        parameters.put("canceled", false);
        parameters.put("ref", false);

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
            jpql += "AND billItem.item.category = :cat ";
            parameters.put("cat", category);
        }

        if (investigation != null) {
            jpql += "AND billItem.item = :code ";
            parameters.put("code", investigation);
        }

        if (externalLaboratoryOnly) {
            jpql += "AND billItem.patientInvestigation.outsourced = true ";

            if (toInstitution != null) {
                jpql += "AND billItem.patientInvestigation.outsourcedInstitution = :oInst ";
                parameters.put("oInst", toInstitution);
            }

            if (toDepartment != null) {
                jpql += "AND billItem.patientInvestigation.outsourcedDepartment = :oDept ";
                parameters.put("oDept", toDepartment);
            }
        }

        jpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpqlWithoutCache(jpql, parameters, TemporalType.TIMESTAMP);

        Map<String, Object> cancelledParameters = new HashMap<>();

        String cancelledJpql = "SELECT new com.divudi.core.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "LEFT JOIN PatientInvestigation pi ON pi.billItem = billItem "
                + "WHERE bill.billTypeAtomic IN :bts "
                + "AND billItem.item is not null "
                + "AND billItem.bill.cancelled =:canceled "
                + "AND billItem.refunded =:ref "
                + "AND TYPE(billItem.item) = Investigation "
                + "AND (TYPE(bill) = RefundBill OR TYPE(bill) = CancelledBill) ";

        cancelledParameters.put("bts", bts);
        cancelledParameters.put("canceled", false);
        cancelledParameters.put("ref", false);

        if (staff != null) {
            cancelledJpql += "AND billItem.patientInvestigation.barcodeGeneratedBy.webUserPerson.name = :staff ";
            cancelledParameters.put("staff", staff.getPerson().getName());
        }

        if (item != null) {
            cancelledJpql += "AND billItem.item = :item ";
            cancelledParameters.put("item", item);
        }

        if (institution != null) {
            cancelledJpql += "AND bill.department.institution = :ins ";
            cancelledParameters.put("ins", institution);
        }

        if (department != null) {
            cancelledJpql += "AND bill.department = :dep ";
            cancelledParameters.put("dep", department);
        }
        if (site != null) {
            cancelledJpql += "AND bill.department.site = :site ";
            cancelledParameters.put("site", site);
        }
        if (webUser != null) {
            cancelledJpql += "AND bill.creater = :wu ";
            cancelledParameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            cancelledJpql += "AND bill.collectingCentre = :cc ";
            cancelledParameters.put("cc", collectingCentre);
        }

        if (route != null) {
            cancelledJpql += "AND bill.collectingCentre.route = :route ";
            cancelledParameters.put("route", route);
        }

        if (referingDoctor != null) {
            cancelledJpql += "AND billItem.bill.referredBy = :rd ";
            cancelledParameters.put("rd", referingDoctor);
        }

        if (mrnNo != null && !mrnNo.trim().isEmpty()) {
            cancelledJpql += "AND billItem.bill.patient.phn LIKE :phn ";
            cancelledParameters.put("phn", mrnNo + "%");
        }

        if (category != null) {
            cancelledJpql += "AND billItem.item.category = :cat ";
            cancelledParameters.put("cat", category);
        }

        if (investigation != null) {
            cancelledJpql += "AND billItem.item = :code ";
            cancelledParameters.put("code", investigation);
        }

//        if (externalLaboratoryOnly) {
//            cancelledJpql += "AND billItem.patientInvestigation.outsourced = true ";
//        }

        cancelledJpql += "AND bill.createdAt BETWEEN :fd AND :td ";
        cancelledParameters.put("fd", fromDate);
        cancelledParameters.put("td", toDate);

        cancelledJpql += "GROUP BY billItem";

        List<ReportTemplateRow> cancelledRs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpqlWithoutCache(cancelledJpql, cancelledParameters, TemporalType.TIMESTAMP);

        if (externalLaboratoryOnly) {
            removeCancelledNonInvestigationBillsWithSentInstitutionDepartmentFilters(cancelledRs);
        }

        List<ReportTemplateRow> allRows = new ArrayList<>(rs);
        allRows.addAll(cancelledRs);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(allRows);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    private void removeCancelledNonInvestigationBillsWithSentInstitutionDepartmentFilters(final List<ReportTemplateRow> rs) {
        Iterator<ReportTemplateRow> iterator = rs.iterator();

        while (iterator.hasNext()) {
            ReportTemplateRow row = iterator.next();
            BillItem item = row.getBillItem();
            Bill bill = item.getBill();

            if (isInternalInvestigation(item)) {
                iterator.remove();
                continue;
            }

            if (!isCancelledOrRefundBill(bill)) {
                continue;
            }

            item.setNetValue(-Math.abs(item.getNetValue()));

            if (!(item.getItem() instanceof Investigation)) {
                iterator.remove();
                continue;
            }

            if (isFromInternalReference(item, bill)) {
                iterator.remove();
            }
        }
    }

    public void resetToDepartment() {
        toDepartment = null;
    }

    private boolean isCancelledOrRefundBill(Bill bill) {
        BillClassType type = bill.getBillClassType();
        return type == BillClassType.CancelledBill || type == BillClassType.RefundBill;
    }

    private boolean isInternalInvestigation(BillItem item) {
        return item.getPatientInvestigation() != null &&
                Boolean.FALSE.equals(item.getPatientInvestigation().getOutsourced());
    }

    private boolean isOutsourcedInstitution(BillItem item) {
        if (toInstitution == null) {
            return true;
        }

        return item.getPatientInvestigation() != null &&
                item.getPatientInvestigation().getOutsourcedInstitution() != null
                && item.getPatientInvestigation().getOutsourcedInstitution().equals(toInstitution);
    }

    private boolean isOutsourcedDepartment(BillItem item) {
        if (toDepartment == null) {
            return true;
        }

        return item.getPatientInvestigation() != null &&
                item.getPatientInvestigation().getOutsourcedDepartment() != null
                && item.getPatientInvestigation().getOutsourcedDepartment().equals(toDepartment);
    }

    private boolean isFromInternalReference(BillItem item, Bill currentBill) {
        BillItem referenceItem = item.getReferanceBillItem();

        if (referenceItem != null) {
            if (isInternalInvestigation(referenceItem)) {
                return true;
            } else {
                return !(isOutsourcedInstitution(referenceItem) && isOutsourcedDepartment(referenceItem));
            }
        }

        Bill originalBill = currentBill.getBilledBill();
        if (originalBill == null) return false;

        return Optional.ofNullable(originalBill.getBillItems())
                .orElse(Collections.emptyList())
                .stream()
                .filter(oi -> oi.getItem() != null && oi.getItem().equals(item.getItem()))
                .anyMatch(oi -> {
                    if (isInternalInvestigation(oi)) {
                        return true;
                    } else {
                        return !(isOutsourcedInstitution(oi) && isOutsourcedDepartment(oi));
                    }
                });
    }

    private ReportTemplateRowBundle generateExternalLaboratoryWorkloadSummaryBillItems(List<BillTypeAtomic> bts,
                                                                                       boolean externalLaboratoryOnly) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("bts", bts);
        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(billItem.item.name, SUM(billItem.qty), billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "LEFT JOIN PatientInvestigation pi ON pi.billItem = billItem "
                + "WHERE bill.billTypeAtomic IN :bts "
                + "AND bill.createdAt BETWEEN :fd AND :td "
                + "AND bill.cancelled =:canceled "
                + "AND billItem.refunded =:ref "
                + "AND TYPE(billItem.item) = Investigation "
                + "AND (TYPE(bill) != RefundBill AND TYPE(bill) != CancelledBill) ";
        
        parameters.put("canceled", false);
        parameters.put("ref", false);

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
            jpql += "AND billItem.item.category = :cat ";
            parameters.put("cat", category);
        }

        if (investigationCode != null) {
            jpql += "AND billItem.patientInvestigation.investigation.code = :code ";
            parameters.put("code", investigationCode.getCode());
        }

        if (investigation != null) {
            jpql += "AND billItem.item = :inv ";
            parameters.put("inv", investigation);
        }

        if (externalLaboratoryOnly) {
            jpql += "AND billItem.patientInvestigation.outsourced = true ";

            if (toInstitution != null) {
                jpql += "AND billItem.patientInvestigation.outsourcedInstitution = :oInst ";
                parameters.put("oInst", toInstitution);
            }

            if (toDepartment != null) {
                jpql += "AND billItem.patientInvestigation.outsourcedDepartment = :oDept ";
                parameters.put("oDept", toDepartment);
            }
        }

        jpql += "GROUP BY billItem";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        Map<String, Object> cancelledParameters = new HashMap<>();
        cancelledParameters.put("bts", bts);
        cancelledParameters.put("fd", fromDate);
        cancelledParameters.put("td", toDate);

        String cancelledJpql = "SELECT new com.divudi.core.data.ReportTemplateRow(billItem.item.name, SUM(billItem.qty), billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "LEFT JOIN PatientInvestigation pi ON pi.billItem = billItem "
                + "WHERE bill.billTypeAtomic IN :bts "
                + "AND bill.createdAt BETWEEN :fd AND :td "
                + "AND billItem.bill.cancelled =:canceled "
                + "AND billItem.refunded =:ref "
                + "AND TYPE(billItem.item) = Investigation "
                + "AND (TYPE(bill) = RefundBill OR TYPE(bill) = CancelledBill) ";
        
        cancelledParameters.put("canceled", false);
        cancelledParameters.put("ref", false);

        if (staff != null) {
            cancelledJpql += "AND billItem.patientInvestigation.barcodeGeneratedBy.webUserPerson.name = :staff ";
            cancelledParameters.put("staff", staff.getPerson().getName());
        }

        if (item != null) {
            cancelledJpql += "AND billItem.patientInvestigation.investigation.name = :item ";
            cancelledParameters.put("item", item.getName());
        }

        if (institution != null) {
            cancelledJpql += "AND bill.department.institution = :ins ";
            cancelledParameters.put("ins", institution);
        }

        if (department != null) {
            cancelledJpql += "AND bill.department = :dep ";
            cancelledParameters.put("dep", department);
        }
        if (site != null) {
            cancelledJpql += "AND bill.department.site = :site ";
            cancelledParameters.put("site", site);
        }
        if (webUser != null) {
            cancelledJpql += "AND bill.creater = :wu ";
            cancelledParameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            cancelledJpql += "AND bill.collectingCentre = :cc ";
            cancelledParameters.put("cc", collectingCentre);
        }

        if (route != null) {
            cancelledJpql += "AND bill.collectingCentre.route = :route ";
            cancelledParameters.put("route", route);
        }

        if (referingDoctor != null) {
            cancelledJpql += "AND billItem.bill.referredBy = :rd ";
            cancelledParameters.put("rd", referingDoctor);
        }

        if (mrnNo != null && !mrnNo.trim().isEmpty()) {
            cancelledJpql += "AND billItem.bill.patient.phn LIKE :phn ";
            cancelledParameters.put("phn", mrnNo + "%");
        }

        if (category != null) {
            cancelledJpql += "AND billItem.item.category = :cat ";
            cancelledParameters.put("cat", category);
        }

        if (investigationCode != null) {
            cancelledJpql += "AND billItem.patientInvestigation.investigation.code = :code ";
            cancelledParameters.put("code", investigationCode.getCode());
        }

        if (investigation != null) {
            cancelledJpql += "AND billItem.item = :inv ";
            cancelledParameters.put("inv", investigation);
        }

//        if (externalLaboratoryOnly) {
//            jpql += "AND billItem.patientInvestigation.outsourced = true ";
//        }

        cancelledJpql += "GROUP BY billItem";

        List<ReportTemplateRow> cancelledRs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(cancelledJpql, cancelledParameters, TemporalType.TIMESTAMP);

        if (externalLaboratoryOnly) {
            removeCancelledNonInvestigationBillsWithSentInstitutionDepartmentFilters(cancelledRs);
        }

        List<ReportTemplateRow> allRows = new ArrayList<>(rs);
        allRows.addAll(cancelledRs);

        createSummaryRows(allRows);

        ReportTemplateRowBundle b = new ReportTemplateRowBundle();
        b.setReportTemplateRows(allRows);
        b.createRowValuesFromBillItems();
        b.calculateTotalsWithCredit();
        return b;
    }

    private void createSummaryRows(final List<ReportTemplateRow> rs) {
        Map<String, ReportTemplateRow> reportRowMap = new HashMap<>();

        for (ReportTemplateRow row : rs) {
            if (row.getRowValue() == 0.0) {
                continue;
            }

            if (row.getBillItem().getNetValue() < 0 && row.getRowValue() > 0) {
                row.setRowValue(-row.getRowValue());
            }

            String rowKey = row.getCategoryName();

            reportRowMap.merge(rowKey, row, (existingRow, newRow) -> {
                existingRow.setRowValue(existingRow.getRowValue() + newRow.getRowValue());
                return existingRow;
            });
        }

        rs.clear();
        rs.addAll(reportRowMap.values());
    }

//    public void generateOpdAndInwardDueReport() {
//        if (visitType == null || visitType.trim().isEmpty()) {
//            JsfUtil.addErrorMessage("Please select a visit type");
//            return;
//        }
//
//        bundle = new ReportTemplateRowBundle();
//
//        List<BillTypeAtomic> opdBts = new ArrayList<>();
//        bundle = new ReportTemplateRowBundle();
//
//        if (visitType.equalsIgnoreCase("IP")) {
////            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
////            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
////            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
////            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
////            opdBts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION);
//            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
//            opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
//            opdBts.add(BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION);
//            opdBts.add(BillTypeAtomic.CANCELLED_INWARD_FINAL_BILL);
////            opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
////            opdBts.add(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE_RETURN);
////            opdBts.add(BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION);
//        } else if (visitType.equalsIgnoreCase("OP")) {
////            opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
////            opdBts.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
//            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
//            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
//            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
//            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
//            opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
//            opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
//            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
//            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
//            opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
//            opdBts.add(BillTypeAtomic.OPD_BILL_REFUND);
//        }
//
//        bundle.setName("Bills");
//        bundle.setBundleType("billList");
//
//        bundle = generateOpdAndInwardDueBills(opdBts, null);
//
//        if (visitType.equalsIgnoreCase("IP")) {
//            updateSettledAmountsForIP();
//        } else if (visitType.equalsIgnoreCase("OP")) {
//            updateSettledAmountsForOP();
//        }
//
//        groupBills();
//    }

    public void generateOpdAndInwardDueReport() {
        reportTimerController.trackReportExecution(() -> {
            if (visitType == null || visitType.trim().isEmpty()) {
                JsfUtil.addErrorMessage("Please select a visit type");
                return;
            }

            bundle = new ReportTemplateRowBundle();
            bundle.setName("Bills");
            bundle.setBundleType("billList");

            if (visitType.equalsIgnoreCase("IP")) {
                generateOpdAndInwardDueIPBills();
            } else if (visitType.equalsIgnoreCase("OP")) {
                List<BillTypeAtomic> opdBts = new ArrayList<>();

                if (visitType.equalsIgnoreCase("OP")) {
//            opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
//            opdBts.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
                    opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
                    opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
                    opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
                    opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
                    opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
                    opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
                    opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
                    opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
                    opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
                    opdBts.add(BillTypeAtomic.OPD_BILL_REFUND);
                }

                bundle = generateOpdAndInwardDueBills(opdBts, null);

                updateSettledAmountsForOP();
                groupBills();
            }
        }, FinancialReport.OPD_AND_INWARD_DUE_REPORT, sessionController.getLoggedUser());
    }

    public void generateOpdAndInwardDueIPBills() {
        HashMap m = new HashMap();
        String sql = " Select b from PatientEncounter b"
                + " JOIN b.finalBill fb"
                + " where b.retired=false "
                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

//        if (paymentMethod != null) {
//            sql += " and b.paymentMethod =:pm ";
//            m.put("pm", paymentMethod);
//        }

        if (institutionOfDepartment != null) {
            sql += "AND b.institution = :insd ";
            m.put("insd", institutionOfDepartment);
        }

        if (department != null) {
            sql += "AND b.department = :dep ";
            m.put("dep", department);
        }

        if (site != null) {
            sql += "AND b.department.site = :site ";
            m.put("site", site);
        }

        if (visitType != null && (visitType.equalsIgnoreCase("IP") && dischargedStatus != null && !dischargedStatus.trim().isEmpty())) {
            if (dischargedStatus.equalsIgnoreCase("notDischarged")) {
                sql += "AND b.discharged = :status ";
                m.put("status", false);
            } else if (dischargedStatus.equalsIgnoreCase("discharged")) {
                sql += "AND b.discharged = :status ";
                m.put("status", true);
            }
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = patientEncounterFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

        if (patientEncounters == null) {
            return;
        }

        updateSettledAmountsForIPByInwardFinalBillPaymentForCreditCompany(patientEncounters);

        if (reportType == null) {
            reportType = "any";
        }

        setBillPatientEncounterMap(getCreditCompanyBills(patientEncounters, "any"));
        calculateCreditCompanyAmounts();

        List<InstitutionBillEncounter> institutionEncounters = new ArrayList<>(InstitutionBillEncounter.createInstitutionBillEncounter(getBillPatientEncounterMap(),
                reportType.equalsIgnoreCase("paid") ? "settled" : reportType.equalsIgnoreCase("due") ? "due" : "any",
                reportType.equalsIgnoreCase("paid") ? "all" : "any", creditCompany, paymentMethod));

        setBillInstitutionEncounterMap(InstitutionBillEncounter.createInstitutionBillEncounterMap(institutionEncounters));
        calculateCreditCompanyDueTotals();

        setEncounterCreditCompanyMap(getEncounterCreditCompanies());
    }

    public String getPolicyNumberFromEncounterCreditCompanyMap(final String bht, final String creditCompanyName) {
        Map<String, EncounterCreditCompany> creditCompanyMap = getEncounterCreditCompanyMap().get(bht);

        if (creditCompanyMap != null) {
            EncounterCreditCompany ecc = creditCompanyMap.get(creditCompanyName);
            if (ecc != null) {
                return ecc.getPolicyNo();
            }
        }
        return "";
    }

    public String getReferenceNumberFromEncounterCreditCompanyMap(final String bht, final String creditCompanyName) {
        Map<String, EncounterCreditCompany> creditCompanyMap = getEncounterCreditCompanyMap().get(bht);

        if (creditCompanyMap != null) {
            EncounterCreditCompany ecc = creditCompanyMap.get(creditCompanyName);
            if (ecc != null) {
                return ecc.getReferanceNo();
            }
        }
        return "";
    }

    // Map<bht, Map<Credit Company, Encounter Credit Company>>
    private Map<String, Map<String, EncounterCreditCompany>> getEncounterCreditCompanies() {
        List<Long> patientEncounterIds = getBillPatientEncounterMap().keySet().stream()
                .map(PatientEncounter::getId)
                .collect(Collectors.toList());

        if (patientEncounterIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String jpql = "SELECT ecc FROM EncounterCreditCompany ecc WHERE ecc.patientEncounter.id IN :patientEncounterIds";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("patientEncounterIds", patientEncounterIds);

        List<EncounterCreditCompany> results = encounterCreditCompanyFacade.findByJpql(jpql, parameters);

        Map<String, Map<String, EncounterCreditCompany>> encounterCreditCompanyMap = new HashMap<>();

        for (EncounterCreditCompany ecc : results) {
            String bhtNo = ecc.getPatientEncounter().getBhtNo();
            String institutionName = ecc.getInstitution().getName();

            encounterCreditCompanyMap
                    .computeIfAbsent(bhtNo, k -> new HashMap<>())
                    .putIfAbsent(institutionName, ecc);
        }

        return encounterCreditCompanyMap;
    }

    private Map<PatientEncounter, List<Bill>> getCreditCompanyBills(List<PatientEncounter> patientEncounters, String dueType) {
        if (dueType == null || (!dueType.equalsIgnoreCase("due") && !dueType.equalsIgnoreCase("any")
                && !dueType.equalsIgnoreCase("excess") && !dueType.equalsIgnoreCase("settled"))) {
            return Collections.emptyMap();
        }

        List<Long> patientEncounterIds = patientEncounters.stream()
                .map(PatientEncounter::getId)
                .collect(Collectors.toList());

        if (patientEncounterIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> parameters = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();

        bts.add(BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);

//        String jpql = "SELECT bill from Bill bill "
//                + "WHERE bill.retired <> :br "
//                + "AND bill.patientEncounter.id in :patientEncounterIds ";

        String jpql = "SELECT bill FROM Bill bill "
                + "LEFT JOIN FETCH bill.paidBill "
                + "WHERE bill.retired <> :br "
                + "AND bill.patientEncounter.id IN :patientEncounterIds ";

        parameters.put("br", true);
        parameters.put("patientEncounterIds", patientEncounterIds);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

//        if (creditCompany != null) {
//            jpql += " and bill.creditCompany =:ins ";
//            parameters.put("ins", creditCompany);
//        }

        List<Bill> rs = (List<Bill>) billFacade.findByJpql(jpql, parameters);

        List<Bill> detachedClones = rs.stream()
                .map(b -> {
                    Bill clonedBill = new Bill();
                    clonedBill.clone(b);
                    return clonedBill;
                })
                .collect(Collectors.toList());

        Map<Long, PatientEncounter> encounterMap = patientEncounters.stream()
                .collect(Collectors.toMap(PatientEncounter::getId, pe -> pe));

        if (dueType.equalsIgnoreCase("settled")) {
            return detachedClones.stream()
                    .filter(bill -> {
                        PatientEncounter pe = encounterMap.get(bill.getPatientEncounter().getId());
                        Bill referenceBill = bill.getReferenceBill();

                        if (pe == null || pe.getFinalBill() == null || referenceBill == null) {
                            return false;
                        }

                        if (referenceBill.isCancelled() || referenceBill.isRefunded()) {
                            bill.setNetTotal(0.0);
                        }

                        double netTotal = pe.getFinalBill().getNetTotal();
                        double settledByPatient = pe.getFinalBill().getSettledAmountByPatient();
                        double settledBySponsor = pe.getFinalBill().getSettledAmountBySponsor();

                        return (netTotal - settledByPatient - settledBySponsor) == 0;
                    })
                    .collect(Collectors.groupingBy(
                            bill -> encounterMap.get(bill.getPatientEncounter().getId())
                    ));
        }

        if (dueType.equalsIgnoreCase("excess")) {
            return detachedClones.stream()
                    .filter(bill -> {
                        PatientEncounter pe = encounterMap.get(bill.getPatientEncounter().getId());
                        Bill referenceBill = bill.getReferenceBill();

                        if (pe == null || pe.getFinalBill() == null || referenceBill == null) {
                            return false;
                        }

                        if (referenceBill.isCancelled() || referenceBill.isRefunded()) {
                            bill.setNetTotal(0.0);
                        }

                        double netTotal = pe.getFinalBill().getNetTotal();
                        double settledByPatient = pe.getFinalBill().getSettledAmountByPatient();
                        double settledBySponsor = pe.getFinalBill().getSettledAmountBySponsor();

                        return (netTotal - settledByPatient - settledBySponsor) < 0;
                    })
                    .collect(Collectors.groupingBy(
                            bill -> encounterMap.get(bill.getPatientEncounter().getId())
                    ));
        }

        if (dueType.equalsIgnoreCase("due")) {
            return detachedClones.stream()
                    .filter(bill -> {
                        PatientEncounter pe = encounterMap.get(bill.getPatientEncounter().getId());

                        Bill referenceBill = bill.getReferenceBill();

                        if (pe == null || pe.getFinalBill() == null || referenceBill == null) {
                            return false;
                        }

                        if (referenceBill.isCancelled() || referenceBill.isRefunded()) {
                            bill.setNetTotal(0.0);
                        }

                        double netTotal = pe.getFinalBill().getNetTotal();
                        double settledByPatient = pe.getFinalBill().getSettledAmountByPatient();
                        double settledBySponsor = pe.getFinalBill().getSettledAmountBySponsor();

                        return (netTotal - settledByPatient - settledBySponsor) > 0;
                    })
                    .collect(Collectors.groupingBy(
                            bill -> encounterMap.get(bill.getPatientEncounter().getId())
                    ));
        }

        return detachedClones.stream()
                .collect(Collectors.groupingBy(
                        bill -> encounterMap.get(bill.getPatientEncounter().getId())
                ));
    }

    private void calculateCreditCompanyDueTotals() {
        gopAmount = 0;
        paidByCompany = 0;
        payableByCompany = 0;
        dueByCompany = 0;

        for (Institution ins : getBillInstitutionEncounterMap().keySet()) {
            double gop = 0;
            double paidByComp = 0;
            double payableByComp = 0;
            double dueByComp = 0;

            List<InstitutionBillEncounter> encounters = getBillInstitutionEncounterMap().get(ins);

            for (InstitutionBillEncounter ibe : encounters) {
                gop += ibe.getGopAmount();
                paidByComp += ibe.getPaidByCompany();
                payableByComp += ibe.getCompanyDue();
                dueByComp += ibe.getCompanyDue() + ibe.getPatientDue();
            }

            instituteGopMap.put(ins, gop);
            institutPaidByCompanyMap.put(ins, paidByComp);
            institutePayableByCompanyMap.put(ins, payableByComp);
            instituteDueByCompanyMap.put(ins, dueByComp);

            gopAmount += gop;
            paidByCompany += paidByComp;
            payableByCompany += payableByComp;
            dueByCompany += dueByComp;
        }
    }

    private void calculateCreditCompanyAmounts() {
        for (Map.Entry<PatientEncounter, List<Bill>> entry : getBillPatientEncounterMap().entrySet()) {

            PatientEncounter patientEncounter = entry.getKey();
            List<Bill> bills = entry.getValue();

            double totalPayableByCompanies = bills.stream().mapToDouble(Bill::getNetTotal).sum();
            double totalPaidByCompanies = bills.stream().mapToDouble(Bill::getPaidAmount).sum();

            patientEncounter.setTransPaid(totalPayableByCompanies);
            patientEncounter.setTransPaidByCompany(totalPaidByCompanies);
        }
    }

    private void updateSettledAmountsForIPByInwardFinalBillPaymentForCreditCompany(List<PatientEncounter> patientEncounters) {
        if (patientEncounters == null || patientEncounters.isEmpty()) {
            return;
        }

        Map<Long, Double> settledAmounts = calculateSettledSponsorBillIP(patientEncounters);

        for (PatientEncounter patientEncounter : patientEncounters) {
            Bill finalBill = patientEncounter.getFinalBill();

            if (finalBill.isCancelled() || finalBill.isRefunded()) {
                continue;
            }

            List<Bill> bills = calculateSettledPatientBillIP(finalBill);
            double total = bills.stream().mapToDouble(Bill::getNetTotal).sum();

            synchronized (finalBill) {
                finalBill.setSettledAmountByPatient(total);
            }

            total = settledAmounts.getOrDefault(patientEncounter.getId(), 0.0);

            synchronized (finalBill) {
                finalBill.setSettledAmountBySponsor(total);
            }
        }
    }

    private Map<Long, Double> calculateSettledSponsorBillIP(List<PatientEncounter> patientEncounters) {
        List<Long> patientEncounterIds = patientEncounters.stream()
                .map(PatientEncounter::getId)
                .collect(Collectors.toList());

        Map<String, Object> parameters = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();

        bts.add(BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);

        String jpql = "SELECT bill from Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.patientEncounter.id in :patientEncounterIds ";

        parameters.put("br", true);
        parameters.put("patientEncounterIds", patientEncounterIds);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (institution != null) {
            jpql += " and bill.creditCompany =:ins ";
            parameters.put("ins", institution);
        }

        List<Bill> rs = (List<Bill>) billFacade.findByJpql(jpql, parameters);

        return rs.stream()
                .collect(Collectors.groupingBy(
                        bill -> bill.getPatientEncounter().getId(),
                        Collectors.summingDouble(Bill::getPaidAmount)
                ));
    }

    private List<ReportTemplateRow> filterByCreditCompanyPaymentMethod(List<ReportTemplateRow> rows) {
        List<ReportTemplateRow> filteredRows = new ArrayList<>();

        Set<Long> billIds = rows.stream()
                .map(ReportTemplateRow::getBill)
                .filter(Objects::nonNull)
                .map(Bill::getId)
                .collect(Collectors.toSet());

        Map<Long, Bill> billIdToCreditCompanyBill = new HashMap<>();

        if ("OP".equalsIgnoreCase(visitType)) {
            String jpql = "SELECT bi FROM BillItem bi WHERE bi.referenceBill.id IN :billIds";
            Map<String, Object> params = new HashMap<>();
            params.put("billIds", billIds);
            List<BillItem> billItems = billItemFacade.findByJpql(jpql, params);

            for (BillItem bi : billItems) {
                if (bi.getReferenceBill() != null && bi.getBill() != null) {
                    billIdToCreditCompanyBill.put(bi.getReferenceBill().getId(), bi.getBill());
                }
            }
        } else if ("IP".equalsIgnoreCase(visitType)) {
            String jpql = "SELECT b FROM Bill b WHERE b.forwardReferenceBill.id IN :billIds";
            Map<String, Object> params = new HashMap<>();
            params.put("billIds", billIds);
            List<Bill> bills = billFacade.findByJpql(jpql, params);

            for (Bill b : bills) {
                if (b.getForwardReferenceBill() != null) {
                    billIdToCreditCompanyBill.put(b.getForwardReferenceBill().getId(), b);
                }
            }
        } else {
            return filteredRows;
        }

        for (ReportTemplateRow row : rows) {
            Bill bill = row.getBill();
            if (bill == null) continue;

            Bill creditCompanyBill = billIdToCreditCompanyBill.get(bill.getId());
            if (creditCompanyBill == null || creditCompanyBill.getPaymentMethod() == null) continue;

            if (creditCompanyBill.getPaymentMethod().equals(paymentMethod)) {
                filteredRows.add(row);
            }
        }

        return filteredRows;
    }

    private List<InstitutionBillEncounter> filterByCreditCompanyPaymentMethodByInstitutionBillEncounter(List<InstitutionBillEncounter> rows) {
        if (paymentMethod == null) {
            return rows;
        }

        List<InstitutionBillEncounter> filteredRows = new ArrayList<>();

        Set<Long> billIds = rows.stream()
                .map(row -> row.getPatientEncounter().getFinalBill())
                .filter(Objects::nonNull)
                .map(Bill::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Bill> billIdToCreditCompanyBill = new HashMap<>();

        if ("OP".equalsIgnoreCase(visitType)) {
            String jpql = "SELECT bi FROM BillItem bi WHERE bi.referenceBill.id IN :billIds";
            Map<String, Object> params = new HashMap<>();
            params.put("billIds", billIds);
            List<BillItem> billItems = billItemFacade.findByJpql(jpql, params);

            for (BillItem bi : billItems) {
                if (bi.getReferenceBill() != null && bi.getBill() != null) {
                    billIdToCreditCompanyBill.put(bi.getReferenceBill().getId(), bi.getBill());
                }
            }
        } else if ("IP".equalsIgnoreCase(visitType)) {
            String jpql = "SELECT b FROM Bill b WHERE b.forwardReferenceBill.id IN :billIds";
            Map<String, Object> params = new HashMap<>();
            params.put("billIds", billIds);
            List<Bill> bills = billFacade.findByJpql(jpql, params);

            for (Bill b : bills) {
                if (b.getForwardReferenceBill() != null) {
                    billIdToCreditCompanyBill.put(b.getForwardReferenceBill().getId(), b);
                }
            }
        } else {
            return filteredRows;
        }

        for (InstitutionBillEncounter row : rows) {
            Bill bill = row.getPatientEncounter().getFinalBill();
            if (bill == null) continue;

            Bill creditCompanyBill = billIdToCreditCompanyBill.get(bill.getId());
            if (creditCompanyBill == null || creditCompanyBill.getPaymentMethod() == null) continue;

            if (creditCompanyBill.getPaymentMethod().equals(paymentMethod)) {
                filteredRows.add(row);
            }
        }

        return filteredRows;
    }

    public ReportTemplateRowBundle generateOpdAndInwardDueBills(List<BillTypeAtomic> bts, List<PaymentMethod> paymentMethods) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.creditCompany is not null ";

        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        if (paymentMethods != null) {
            jpql += "AND bill.paymentMethod in :bpms ";
            parameters.put("bpms", paymentMethods);
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
            if (visitType.equalsIgnoreCase("OP")) {
                jpql += "AND bill.department.institution = :ins ";
            } else if (visitType.equalsIgnoreCase("IP")) {
                jpql += "AND bill.patientEncounter.department.institution = :ins ";
            }

            parameters.put("ins", institution);
        }

        if (department != null) {
            if (visitType.equalsIgnoreCase("OP")) {
                jpql += "AND bill.department = :dep ";
            } else if (visitType.equalsIgnoreCase("IP")) {
                jpql += "AND bill.patientEncounter.department = :dep ";
            }

            parameters.put("dep", department);
        }
        if (site != null) {
            if (visitType.equalsIgnoreCase("OP")) {
                jpql += "AND bill.department.site = :site ";
            } else if (visitType.equalsIgnoreCase("IP")) {
                jpql += "AND bill.patientEncounter.department.site = :site ";
            }

            parameters.put("site", site);
        }
        if (webUser != null) {
            jpql += "AND bill.creater = :wu ";
            parameters.put("wu", webUser);
        }

        if (collectingCentre != null) {
            jpql += "AND bill.collectingCentre = :ccc ";
            parameters.put("ccc", collectingCentre);
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

        if (paymentMethod != null) {
            b.setReportTemplateRows(filterByCreditCompanyPaymentMethod(rs));
        } else {
            b.setReportTemplateRows(rs);
        }

        b.createRowValuesFromBill();
        b.calculateTotalsWithCredit();
        return b;
    }

    public void exportOpdAndInwardOPToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=OPD_AND_INWARD.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("OPD and Inward Report");
            int rowIndex = 0;

            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");

            XSSFCellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("S. No");
            headerRow.createCell(1).setCellValue("Invoice Date");
            headerRow.createCell(2).setCellValue("Invoice No");
            headerRow.createCell(3).setCellValue("Customer Reference No");
            headerRow.createCell(4).setCellValue("MRNO");
            headerRow.createCell(5).setCellValue("Patient Name");
            headerRow.createCell(6).setCellValue("Gross Amt");
            headerRow.createCell(7).setCellValue("Disc Amt");
            headerRow.createCell(8).setCellValue("Net Amt");
            headerRow.createCell(9).setCellValue("Patient Share");
            headerRow.createCell(10).setCellValue("Sponsor Share");
            headerRow.createCell(11).setCellValue("Due Amt");

            int serialNumber = 1;
            for (Map.Entry<Institution, List<Bill>> entrySet : getBundle().getGroupedBillItemsByInstitution().entrySet()) {
                Institution institution = entrySet.getKey();
                List<Bill> bills = entrySet.getValue();

                Row institutionRow = sheet.createRow(rowIndex++);
                institutionRow.createCell(0).setCellValue(institution.getName());

                for (Bill bill : bills) {
                    Row dataRow = sheet.createRow(rowIndex++);
                    dataRow.createCell(0).setCellValue(serialNumber++);
                    String formattedDate = dateFormatter.format(bill.getCreatedAt());
                    dataRow.createCell(1).setCellValue(formattedDate);
                    dataRow.createCell(2).setCellValue(bill.getDeptId());
                    dataRow.createCell(3).setCellValue(bill.getReferenceNumber());
                    dataRow.createCell(4).setCellValue(bill.getPatient().getPhn());
                    dataRow.createCell(5).setCellValue(bill.getPatient().getPerson().getName());
                    dataRow.createCell(6).setCellValue(bill.getBillTotal());
                    dataRow.createCell(7).setCellValue(bill.getDiscount());
                    dataRow.createCell(8).setCellValue(bill.getNetTotal());
                    dataRow.createCell(9).setCellValue(bill.getSettledAmountByPatient());
                    dataRow.createCell(10).setCellValue(bill.getSettledAmountBySponsor());
                    dataRow.createCell(11).setCellValue(bill.getNetTotal() - bill.getSettledAmountBySponsor() - bill.getSettledAmountByPatient());

                    dataRow.getCell(6).setCellStyle(amountStyle);
                    dataRow.getCell(7).setCellStyle(amountStyle);
                    dataRow.getCell(8).setCellStyle(amountStyle);
                    dataRow.getCell(9).setCellStyle(amountStyle);
                    dataRow.getCell(10).setCellStyle(amountStyle);
                    dataRow.getCell(11).setCellStyle(amountStyle);
                }

                Row institutionTotalRow = sheet.createRow(rowIndex++);
                institutionTotalRow.createCell(5).setCellValue("Sub Total");
                institutionTotalRow.createCell(6).setCellValue(calculateGrossAmountSubTotalByBills(bills));
                institutionTotalRow.createCell(7).setCellValue(calculateDiscountSubTotalByBills(bills));
                institutionTotalRow.createCell(8).setCellValue(calculateNetAmountSubTotalByBills(bills));
                institutionTotalRow.createCell(9).setCellValue(calculatePatientShareSubTotalByBills(bills));
                institutionTotalRow.createCell(10).setCellValue(calculateSponsorShareSubTotalByBills(bills));
                institutionTotalRow.createCell(11).setCellValue(calculateDueAmountSubTotalByBills(bills));

                institutionTotalRow.getCell(6).setCellStyle(amountStyle);
                institutionTotalRow.getCell(7).setCellStyle(amountStyle);
                institutionTotalRow.getCell(8).setCellStyle(amountStyle);
                institutionTotalRow.getCell(9).setCellStyle(amountStyle);
                institutionTotalRow.getCell(10).setCellStyle(amountStyle);
                institutionTotalRow.getCell(11).setCellStyle(amountStyle);
            }

            Row footerRow = sheet.createRow(rowIndex++);
            footerRow.createCell(5).setCellValue("Net Total");
            footerRow.createCell(6).setCellValue(calculateGrossAmountNetTotal());
            footerRow.createCell(7).setCellValue(calculateDiscountNetTotal());
            footerRow.createCell(8).setCellValue(calculateNetAmountNetTotal());
            footerRow.createCell(9).setCellValue(calculatePatientShareNetTotal());
            footerRow.createCell(10).setCellValue(calculateSponsorShareNetTotal());
            footerRow.createCell(11).setCellValue(calculateDueAmountNetTotal());

            footerRow.getCell(6).setCellStyle(amountStyle);
            footerRow.getCell(7).setCellStyle(amountStyle);
            footerRow.getCell(8).setCellStyle(amountStyle);
            footerRow.getCell(9).setCellStyle(amountStyle);
            footerRow.getCell(10).setCellStyle(amountStyle);
            footerRow.getCell(11).setCellStyle(amountStyle);

            workbook.write(out);
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportOpdAndInwardOPToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=OPD_AND_INWARD.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("OPD and Inward Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            com.itextpdf.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            com.itextpdf.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            PdfPTable table = new PdfPTable(12);
            table.setWidthPercentage(100);

            float[] columnWidths = {1.5f, 2.5f, 2.5f, 2.5f, 2.5f, 3.0f, 3.0f, 3.0f, 3.0f, 3.0f, 3.0f, 3.0f};
            table.setWidths(columnWidths);

            String[] headers = {"S. No", "Invoice Date", "Invoice No", "Customer Reference No", "MRNO", "Patient Name",
                    "Gross Amt", "Disc Amt", "Net Amt", "Patient Share", "Sponsor Share", "Due Amt"};
            for (String header : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(header, boldFont));
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                table.addCell(cell);
            }

            int serialNumber = 1;

            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMMM yyyy HH:mm:ss");

            for (Map.Entry<Institution, List<Bill>> entrySet : getBundle().getGroupedBillItemsByInstitution().entrySet()) {
                Institution institution = entrySet.getKey();
                List<Bill> bills = entrySet.getValue();

                PdfPCell institutionCell = new PdfPCell(new Phrase(institution.getName(), boldFont));
                institutionCell.setColspan(12);
                table.addCell(institutionCell);

                for (Bill bill : bills) {
                    table.addCell(new PdfPCell(new Phrase(String.valueOf(serialNumber++), normalFont)));
                    String formattedDate = dateFormatter.format(bill.getCreatedAt());
                    table.addCell(new PdfPCell(new Phrase(formattedDate, normalFont)));
                    table.addCell(new PdfPCell(new Phrase(bill.getDeptId(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(bill.getReferenceNumber(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(bill.getPatient().getPhn(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(bill.getPatient().getPerson().getName(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getBillTotal()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getDiscount()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getNetTotal()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getSettledAmountByPatient()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(bill.getSettledAmountBySponsor()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(
                            bill.getNetTotal() - bill.getSettledAmountBySponsor() - bill.getSettledAmountByPatient()), normalFont)));
                }

                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("", normalFont)));
                table.addCell(new PdfPCell(new Phrase("Sub Total", boldFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateGrossAmountSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateDiscountSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateNetAmountSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculatePatientShareSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateSponsorShareSubTotalByBills(bills)), normalFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateDueAmountSubTotalByBills(bills)), normalFont)));
            }

            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("", normalFont)));
            table.addCell(new PdfPCell(new Phrase("Net Total", boldFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateGrossAmountNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateDiscountNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateNetAmountNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculatePatientShareNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateSponsorShareNetTotal()), normalFont)));
            table.addCell(new PdfPCell(new Phrase(decimalFormat.format(calculateDueAmountNetTotal()), normalFont)));

            document.add(table);
            document.close();
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportOpdAndInwardIPToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=OPD_AND_INWARD.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("OPD and Inward Report");

            int rowIndex = 0;
            int serialNumber = 1;

            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy HH:mm");
            XSSFCellStyle amountStyle = workbook.createCellStyle();
            amountStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));

            Row headerRow = sheet.createRow(rowIndex++);
            String[] headers = {
                    "Sr.No", "BHT No", "Invoice Date", "Invoice No", "Customer Ref No", "MRNO", "Patient Name",
                    "Gross Amt", "Disc Amt", "Net Amt", "GOP (Pat)", "Paid (Pat)", "Due (Pat)",
                    "GOP (Spon)", "Paid (Spon)", "Due (Spon)", "Total Due"
            };

            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            for (Map.Entry<Institution, List<InstitutionBillEncounter>> entry : getBillInstitutionEncounterMap().entrySet()) {
                Institution institution = entry.getKey();
                List<InstitutionBillEncounter> bills = entry.getValue();

                Row groupRow = sheet.createRow(rowIndex++);
                groupRow.createCell(0).setCellValue(institution.getName());

                for (InstitutionBillEncounter b : bills) {
                    Row dataRow = sheet.createRow(rowIndex++);

                    dataRow.createCell(0).setCellValue(serialNumber++);
                    dataRow.createCell(1).setCellValue(b.getBhtNo());
                    dataRow.createCell(2).setCellValue(dateFormatter.format(b.getPatientEncounter().getFinalBill().getCreatedAt()));
                    dataRow.createCell(3).setCellValue(b.getPatientEncounter().getFinalBill().getDeptId());
                    dataRow.createCell(4).setCellValue(getReferenceNumberFromEncounterCreditCompanyMap(b.getBhtNo(), institution.getName()));
                    dataRow.createCell(5).setCellValue(b.getPatient().getPhn());
                    dataRow.createCell(6).setCellValue(b.getPatient().getPerson().getName());

                    Cell grossAmt = dataRow.createCell(7);
                    grossAmt.setCellValue(b.getPatientEncounter().getFinalBill().getGrantTotal());
                    grossAmt.setCellStyle(amountStyle);

                    Cell discAmt = dataRow.createCell(8);
                    discAmt.setCellValue(b.getPatientEncounter().getFinalBill().getDiscount());
                    discAmt.setCellStyle(amountStyle);

                    Cell netAmt = dataRow.createCell(9);
                    netAmt.setCellValue(b.getNetTotal());
                    netAmt.setCellStyle(amountStyle);

                    Cell gopPat = dataRow.createCell(10);
                    gopPat.setCellValue(b.getNetTotal() - b.getGopAmount());
                    gopPat.setCellStyle(amountStyle);

                    Cell paidPat = dataRow.createCell(11);
                    paidPat.setCellValue(b.getPaidByPatient());
                    paidPat.setCellStyle(amountStyle);

                    Cell duePat = dataRow.createCell(12);
                    duePat.setCellValue(b.getPatientDue());
                    duePat.setCellStyle(amountStyle);

                    Cell gopSpon = dataRow.createCell(13);
                    gopSpon.setCellValue(b.getGopAmount());
                    gopSpon.setCellStyle(amountStyle);

                    Cell paidSpon = dataRow.createCell(14);
                    paidSpon.setCellValue(b.getPaidByCompany());
                    paidSpon.setCellStyle(amountStyle);

                    Cell dueSpon = dataRow.createCell(15);
                    dueSpon.setCellValue(b.getCompanyDue());
                    dueSpon.setCellStyle(amountStyle);

                    Cell totalDue = dataRow.createCell(16);
                    totalDue.setCellValue(b.getTotalDue());
                    totalDue.setCellStyle(amountStyle);
                }

                Row subTotalRow = sheet.createRow(rowIndex++);
                subTotalRow.createCell(0).setCellValue("Sub Total:");
                subTotalRow.createCell(13).setCellValue(getInstituteGopMap().get(institution));
                subTotalRow.getCell(13).setCellStyle(amountStyle);
                subTotalRow.createCell(14).setCellValue(getInstitutPaidByCompanyMap().get(institution));
                subTotalRow.getCell(14).setCellStyle(amountStyle);
                subTotalRow.createCell(15).setCellValue(getInstitutePayableByCompanyMap().get(institution));
                subTotalRow.getCell(15).setCellStyle(amountStyle);
            }

            Row totalRow = sheet.createRow(rowIndex++);
            totalRow.createCell(0).setCellValue("Net Total:");
            totalRow.createCell(13).setCellValue(getGopAmount());
            totalRow.getCell(13).setCellStyle(amountStyle);
            totalRow.createCell(14).setCellValue(getPaidByCompany());
            totalRow.getCell(14).setCellStyle(amountStyle);
            totalRow.createCell(15).setCellValue(getPayableByCompany());
            totalRow.getCell(15).setCellStyle(amountStyle);

            for (int i = 0; i <= 16; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            context.responseComplete();

        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error exporting OPD and Inward IP to Excel", e);
        }
    }

    public void exportOpdAndInwardIPToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=OPD_AND_INWARD.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("OPD_AND_INWARD Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            com.itextpdf.text.Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            com.itextpdf.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
            SimpleDateFormat dateFormatter = new SimpleDateFormat("dd MMM yyyy HH:mm");

            int srNo = 1;

            for (Map.Entry<Institution, List<InstitutionBillEncounter>> entry : getBillInstitutionEncounterMap().entrySet()) {
                Institution institution = entry.getKey();
                List<InstitutionBillEncounter> bills = entry.getValue();

                Paragraph groupHeader = new Paragraph(institution.getName(), boldFont);
                groupHeader.setSpacingBefore(10);
                groupHeader.setSpacingAfter(5);
                document.add(groupHeader);

                PdfPTable table = new PdfPTable(17);
                table.setWidthPercentage(100);
                float[] widths = {1.2f, 2f, 3f, 2.5f, 3f, 2f, 3f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f};
                table.setWidths(widths);

                String[] headers = {"Sr.No", "BHT No", "Invoice Date", "Invoice No", "Customer Ref No", "MRNO", "Patient Name",
                        "Gross Amt", "Disc Amt", "Net Amt", "GOP (Pat)", "Paid (Pat)", "Due (Pat)",
                        "GOP (Spon)", "Paid (Spon)", "Due (Spon)", "Total Due"};

                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    table.addCell(cell);
                }

                for (InstitutionBillEncounter b : bills) {
                    table.addCell(new PdfPCell(new Phrase(String.valueOf(srNo++), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(b.getBhtNo(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(dateFormatter.format(b.getPatientEncounter().getFinalBill().getCreatedAt()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(b.getPatientEncounter().getFinalBill().getDeptId(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(getReferenceNumberFromEncounterCreditCompanyMap(b.getBhtNo(), institution.getName()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(b.getPatient().getPhn(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(b.getPatient().getPerson().getName(), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(b.getPatientEncounter().getFinalBill().getGrantTotal()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(b.getPatientEncounter().getFinalBill().getDiscount()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(b.getNetTotal()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(b.getNetTotal() - b.getGopAmount()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(b.getPaidByPatient()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(b.getPatientDue()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(b.getGopAmount()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(b.getPaidByCompany()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(b.getCompanyDue()), normalFont)));
                    table.addCell(new PdfPCell(new Phrase(decimalFormat.format(b.getTotalDue()), normalFont)));
                }

                PdfPCell subtotalLabelCell = new PdfPCell(new Phrase("Sub Total:", boldFont));
                subtotalLabelCell.setColspan(6);
                subtotalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                table.addCell(subtotalLabelCell);

                for (int i = 0; i < 7; i++) {
                    table.addCell(new PdfPCell(new Phrase("")));
                }

                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(getInstituteGopMap().get(institution)), boldFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(getInstitutPaidByCompanyMap().get(institution)), boldFont)));
                table.addCell(new PdfPCell(new Phrase(decimalFormat.format(getInstitutePayableByCompanyMap().get(institution)), boldFont)));
                table.addCell(new PdfPCell(new Phrase("")));

                document.add(table);
            }

            Paragraph spacer = new Paragraph("\n");
            document.add(spacer);

            PdfPTable totalTable = new PdfPTable(17);
            totalTable.setWidthPercentage(100);
            float[] totalWidths = {1.2f, 2f, 3f, 2.5f, 3f, 2f, 3f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f};
            totalTable.setWidths(totalWidths);

            PdfPCell totalLabel = new PdfPCell(new Phrase("Net Total:", boldFont));
            totalLabel.setColspan(6);
            totalLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalTable.addCell(totalLabel);

            for (int i = 0; i < 7; i++) {
                totalTable.addCell(new PdfPCell(new Phrase("")));
            }

            totalTable.addCell(new PdfPCell(new Phrase(decimalFormat.format(getGopAmount()), boldFont)));
            totalTable.addCell(new PdfPCell(new Phrase(decimalFormat.format(getPaidByCompany()), boldFont)));
            totalTable.addCell(new PdfPCell(new Phrase(decimalFormat.format(getPayableByCompany()), boldFont)));
            totalTable.addCell(new PdfPCell(new Phrase("")));

            document.add(totalTable);

            document.close();
            context.responseComplete();
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Error exporting OPD and Inward IP to PDF", e);
        }
    }

    public void generateDebtorBalanceIPBills(final boolean onlyDue, final List<PaymentMethod> paymentMethods) {
        Date startTime = new Date();

        HashMap m = new HashMap();
        String sql = " Select b from PatientEncounter b"
                + " JOIN b.finalBill fb"
                + " where b.retired=false "
                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (roomCategory != null) {
            sql += " AND b.currentPatientRoom.roomFacilityCharge.roomCategory = :category ";
            m.put("category", roomCategory);
        }

        if (paymentMethods != null) {
            sql += " AND b.finalBill.paymentMethod in :bpms ";
            m.put("bpms", paymentMethods);
        }

        if (institutionOfDepartment != null) {
            sql += "AND fb.institution = :insd ";
            m.put("insd", institutionOfDepartment);
        }

        if (department != null) {
            sql += "AND fb.department = :dep ";
            m.put("dep", department);
        }

        if (site != null) {
            sql += "AND fb.department.site = :site ";
            m.put("site", site);
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = patientEncounterFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);

        if (patientEncounters == null) {
            return;
        }

        updateSettledAmountsForIPByInwardFinalBillPaymentForCreditCompany(patientEncounters);

        setBillPatientEncounterMap(getCreditCompanyBills(patientEncounters, "any"));

        setInstitutionBillPatientEncounterMap(InstitutionBillEncounter.createPatientEncounterBillEncounterMap(
                InstitutionBillEncounter.createInstitutionBillEncounter(getBillPatientEncounterMap(),
                        onlyDue ? "due" : "any", "any", creditCompany, null)));

        calculateCreditCompanyAmounts();

        setEncounterCreditCompanyMap(getEncounterCreditCompanies());

        billed = 0;
        paidByPatient = 0;
        paidByCompany = 0;
        payableByPatient = 0;
        double peGop;
        double pePaidByCompany;

        Map<PatientEncounter, Double> billGopMap = new HashMap<>();
        Map<PatientEncounter, Double> billPaidByCompanyMap = new HashMap<>();

        for (PatientEncounter p : getInstitutionBillPatientEncounterMap().keySet()) {
            List<InstitutionBillEncounter> encounters = getInstitutionBillPatientEncounterMap().get(p);
            if (encounters == null || encounters.isEmpty()) {
                continue;
            }
            peGop = encounters.stream()
                    .mapToDouble(InstitutionBillEncounter::getGopAmount)
                    .sum();
            pePaidByCompany = encounters.stream()
                    .mapToDouble(InstitutionBillEncounter::getPaidByCompany)
                    .sum();

            billed += p.getFinalBill().getNetTotal();
            paidByPatient += getInstitutionBillPatientEncounterMap().get(p).get(0).getPaidByPatient();
            paidByCompany += getInstitutionBillPatientEncounterMap().get(p).get(0).getTotalPaidByCompanies();
            payableByPatient += getInstitutionBillPatientEncounterMap().get(p).get(0).getPatientGopAmount();

            billGopMap.put(p, peGop);
            billPaidByCompanyMap.put(p, pePaidByCompany);
        }

        setPatientEncounterGopMap(billGopMap);
        setPatientEncounterPaidByCompanyMap(billPaidByCompanyMap);
    }

    public ReportTemplateRowBundle generateDebtorBalanceBills(List<BillTypeAtomic> bts, List<PaymentMethod> paymentMethods) {
        Map<String, Object> parameters = new HashMap<>();

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
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

        if (paymentMethods != null) {
            jpql += "AND bill.paymentMethod in :bpms ";
            parameters.put("bpms", paymentMethods);
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
            if (visitType.equalsIgnoreCase("OP")) {
                jpql += "AND bill.department.institution = :ins ";
            } else if (visitType.equalsIgnoreCase("IP")) {
                jpql += "AND bill.patientEncounter.department.institution = :ins ";
            }

            parameters.put("ins", institution);
        }

        if (department != null) {
            if (visitType.equalsIgnoreCase("OP")) {
                jpql += "AND bill.department = :dep ";
            } else if (visitType.equalsIgnoreCase("IP")) {
                jpql += "AND bill.patientEncounter.department = :dep ";
            }

            parameters.put("dep", department);
        }
        if (site != null) {
            if (visitType.equalsIgnoreCase("OP")) {
                jpql += "AND bill.department.site = :site ";
            } else if (visitType.equalsIgnoreCase("IP")) {
                jpql += "AND bill.patientEncounter.department.site = :site ";
            }

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

    private void updateSettledAmountsForIP() {
        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            Bill bill = row.getBill();

            if (bill.isCancelled() || bill.isRefunded()) {
                continue;
            }

            PatientEncounter patientEncounter = bill.getPatientEncounter();
            Bill finalBill = patientEncounter.getFinalBill();

            List<Bill> bills = calculateSettledPatientBillIP(finalBill);
            double total = bills.stream().mapToDouble(Bill::getNetTotal).sum();

            synchronized (finalBill) {
                finalBill.setSettledAmountByPatient(total);
            }

            bills = calculateSettledSponsorBillIP(finalBill);
            total = bills.stream().mapToDouble(Bill::getNetTotal).sum();

            synchronized (finalBill) {
                finalBill.setSettledAmountBySponsor(total);
            }
        }
    }

    private List<Bill> calculateSettledPatientBillIP(Bill bill) {
        Map<String, Object> parameters = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();

        if (visitType.equalsIgnoreCase("IP")) {
            bts.add(BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION);
            bts.add(BillTypeAtomic.INWARD_DEPOSIT);
            bts.add(BillTypeAtomic.INWARD_DEPOSIT_REFUND);
        }

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br ";

        parameters.put("br", true);

        jpql += "AND bill.billTypeAtomic in :bts ";
        parameters.put("bts", bts);

        jpql += "AND bill.forwardReferenceBill.id = :rb ";
        parameters.put("rb", bill.getId());

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        return rs.stream().map(ReportTemplateRow::getBill).collect(Collectors.toList());
    }

    private List<Bill> calculateSettledSponsorBillIP(Bill bill) {
        Map<String, Object> parameters = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();

        if (visitType.equalsIgnoreCase("IP")) {
            bts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION);
            bts.add(BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED);
        }

        parameters.put("br", true);
        parameters.put("bts", bts);
        parameters.put("rb", bill.getId());

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.billTypeAtomic in :bts "
                + "AND bill.forwardReferenceBill.id = :rb ";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        List<Bill> bills = rs.stream().map(ReportTemplateRow::getBill).collect(Collectors.toList());

        String sql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.billTypeAtomic in :bts "
                + "AND bill.billedBill.forwardReferenceBill.id = :rb ";

        rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(sql, parameters, TemporalType.TIMESTAMP);

        bills.addAll(rs.stream().map(ReportTemplateRow::getBill).collect(Collectors.toList()));

        return bills;
    }

    private void updateSettledAmountsForOP() {
        for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
            Bill bill = row.getBill();

            if (bill.isCancelled() || bill.isRefunded()) {
                continue;
            }

            List<Bill> bills = calculateSettledSponsorBillOP(bill);
            total = bills.stream().mapToDouble(Bill::getNetTotal).sum();

            synchronized (bill) {
                bill.setSettledAmountBySponsor(total);
            }
        }
    }

    private List<Bill> calculateSettledSponsorBillOP(Bill bill) {
        Map<String, Object> parameters = new HashMap<>();
        List<BillTypeAtomic> bts = new ArrayList<>();

        if (visitType.equalsIgnoreCase("OP")) {
            bts.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_CANCELLATION);
            bts.add(BillTypeAtomic.OPD_CREDIT_COMPANY_PAYMENT_RECEIVED);
        }

        parameters.put("br", true);
        parameters.put("bts", bts);
        parameters.put("rb", bill);

        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "JOIN BillItem billItem ON bill.id = billItem.bill.id "
                + "WHERE bill.retired <> :br "
                + "AND bill.billTypeAtomic in :bts "
                + "AND billItem.referenceBill = :rb ";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        return rs.stream().map(ReportTemplateRow::getBill).collect(Collectors.toList());
    }

    private void groupBills() {
        Map<Institution, List<Bill>> billMap = new HashMap<>();

        if (visitType != null && visitType.equalsIgnoreCase("OP")) {
            for (ReportTemplateRow row : bundle.getReportTemplateRows()) {
                Bill bill1 = row.getBill();

                if (reportType != null && reportType.equalsIgnoreCase("paid")) {
                    if (bill.getBillClassType().equals(BillClassType.CancelledBill)) {
                        if (bill1.getNetTotal() - bill1.getSettledAmountByPatient() - bill1.getSettledAmountBySponsor() < 0) {
                            continue;
                        }
                    } else {
                        if (bill1.getNetTotal() - bill1.getSettledAmountByPatient() - bill1.getSettledAmountBySponsor() > 0) {
                            continue;
                        }
                    }
                }

                if (reportType != null && reportType.equalsIgnoreCase("due")) {
                    if (bill.getBillClassType().equals(BillClassType.CancelledBill)) {
                        if (bill1.getNetTotal() - bill1.getSettledAmountByPatient() - bill1.getSettledAmountBySponsor() >= 0) {
                            continue;
                        }
                    } else {
                        if (bill1.getNetTotal() - bill1.getSettledAmountByPatient() - bill1.getSettledAmountBySponsor() <= 0) {
                            continue;
                        }
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
                    if (bill1.getPatientEncounter() != null && bill1.getPatientEncounter().getFinalBill() != null) {
                        if ((bill1.getPatientEncounter().getFinalBill().getNetTotal() - bill1.getPatientEncounter().getFinalBill().getSettledAmountByPatient()
                                - bill1.getPatientEncounter().getFinalBill().getSettledAmountBySponsor()) > 0) {
                            continue;
                        }
                    } else {
                        if ((bill1.getNetTotal() - bill1.getSettledAmountByPatient() - bill1.getSettledAmountBySponsor()) > 0) {
                            continue;
                        }
                    }
                }

                if (reportType != null && reportType.equalsIgnoreCase("due")) {
                    if (bill1.getPatientEncounter() != null && bill1.getPatientEncounter().getFinalBill() != null) {
                        if ((bill1.getPatientEncounter().getFinalBill().getNetTotal() - bill1.getPatientEncounter().getFinalBill().getSettledAmountByPatient()
                                - bill1.getPatientEncounter().getFinalBill().getSettledAmountBySponsor()) <= 0) {
                            continue;
                        }
                    } else {
                        if ((bill1.getNetTotal() - bill1.getSettledAmountByPatient() - bill1.getSettledAmountBySponsor()) <= 0) {
                            continue;
                        }
                    }
                }

                if (bill1.getPatientEncounter() != null && bill1.getPatientEncounter().getFinalBill() != null) {
                    if (billMap.containsKey(bill1.getPatientEncounter().getFinalBill().getCreditCompany())) {
                        billMap.get(bill1.getPatientEncounter().getFinalBill().getCreditCompany()).add(bill1);
                    } else {
                        List<Bill> bills = new ArrayList<>();
                        bills.add(bill1);
                        billMap.put(bill1.getPatientEncounter().getFinalBill().getCreditCompany(), bills);
                    }
                } else if (bill1.getCreditCompany() != null) {
                    if (billMap.containsKey(bill1.getCreditCompany())) {
                        billMap.get(bill1.getCreditCompany()).add(bill1);
                    } else {
                        List<Bill> bills = new ArrayList<>();
                        bills.add(bill1);
                        billMap.put(bill1.getCreditCompany(), bills);
                    }
                }
            }
        }

        bundle.setGroupedBillItemsByInstitution(billMap);
    }

    public Double calculateSubTotal() {
        double subTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            subTotal += calculateNetAmountSubTotalByBills(bills);
        }

        return subTotal;
    }

    public Double calculateGrossAmountSubTotalByBills(List<Bill> bills) {
        Double total = 0.0;

        for (Bill bill : bills) {
            total += bill.getTotal();
        }

        return total;
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
            balance += bill.getNetTotal() - bill.getSettledAmountBySponsor() - bill.getSettledAmountByPatient();
        }

        return balance;
    }

    public Double calculateGrossAmountNetTotal() {
        double grossAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        if (billMap == null || billMap.isEmpty()) {
            return 0.0;
        }

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            grossAmountNetTotal += calculateGrossAmountSubTotalByBills(bills);
        }

        return grossAmountNetTotal;
    }

    public Double calculateDiscountNetTotal() {
        double discountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        if (billMap == null || billMap.isEmpty()) {
            return 0.0;
        }

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            discountNetTotal += calculateDiscountSubTotalByBills(bills);
        }

        return discountNetTotal;
    }

    public Double calculatePatientShareNetTotal() {
        double patientShareNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        if (billMap == null || billMap.isEmpty()) {
            return 0.0;
        }

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            patientShareNetTotal += calculatePatientShareSubTotalByBills(bills);
        }

        return patientShareNetTotal;
    }

    public Double calculateDueAmountNetTotal() {
        double dueAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        if (billMap == null || billMap.isEmpty()) {
            return 0.0;
        }

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            dueAmountNetTotal += calculateDueAmountSubTotalByBills(bills);
        }

        return dueAmountNetTotal;
    }

    public Double calculateSponsorShareNetTotal() {
        double sponsorShareNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        if (billMap == null || billMap.isEmpty()) {
            return 0.0;
        }

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            sponsorShareNetTotal += calculateSponsorShareSubTotalByBills(bills);

        }

        return sponsorShareNetTotal;
    }

    public Double calculateNetAmountSubTotalByBills(List<Bill> bills) {
        Double netTotal = 0.0;

        for (Bill bill : bills) {
            netTotal += bill.getNetTotal();
        }

        return netTotal;
    }

    public Double calculateDiscountSubTotalByBills(List<Bill> bills) {
        Double discount = 0.0;

        for (Bill bill : bills) {
            discount += bill.getDiscount();
        }

        return discount;
    }

    public Double calculateNetAmountNetTotal() {
        double netAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        if (billMap == null || billMap.isEmpty()) {
            return 0.0;
        }

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            netAmountNetTotal += calculateNetAmountSubTotalByBills(bills);
        }

        return netAmountNetTotal;
    }

    public void generateDiscountReport() {
        reportTimerController.trackReportExecution(() -> {
            if (visitType == null || visitType.trim().isEmpty()) {
                JsfUtil.addErrorMessage("Please select a visit type");
                return;
            }

            bundle = new ReportTemplateRowBundle();

            List<BillTypeAtomic> opdBts = new ArrayList<>();
            bundle = new ReportTemplateRowBundle();

            if (visitType.equalsIgnoreCase("IP")) {
                if (reportType.equalsIgnoreCase("detail")) {
                    opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL);
                    opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION);
                    opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);
                    opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
                } else if (reportType.equalsIgnoreCase("summary")) {
                    opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
                    opdBts.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION);
                    opdBts.add(BillTypeAtomic.INWARD_SERVICE_BILL_REFUND);
                    opdBts.add(BillTypeAtomic.INWARD_FINAL_BILL);
                }
            } else if (visitType.equalsIgnoreCase("OP")) {
                if (reportType.equalsIgnoreCase("detail")) {
                    opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
                    opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
                    opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION);
                    opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
                    opdBts.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
                    opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
                    opdBts.add(BillTypeAtomic.OPD_BILL_REFUND);
                    opdBts.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
                    opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
                    opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION);
                } else if (reportType.equalsIgnoreCase("summary")) {
                    opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_WITH_PAYMENT);
                    opdBts.add(BillTypeAtomic.PACKAGE_OPD_BATCH_BILL_CANCELLATION);
                    opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_WITH_PAYMENT);
                    opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_PAYMENT_COLLECTION_AT_CASHIER);
                    opdBts.add(BillTypeAtomic.OPD_BATCH_BILL_CANCELLATION);
                    opdBts.add(BillTypeAtomic.OPD_BILL_REFUND);
                    opdBts.add(BillTypeAtomic.OPD_BILL_CANCELLATION);
                    opdBts.add(BillTypeAtomic.PACKAGE_OPD_BILL_REFUND);
                }
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
        }, FinancialReport.DISCOUNT_REPORT, sessionController.getLoggedUser());
    }

    public double calculateDiscountForOP(final BillItem billItem, final List<ReportTemplateRow> reportTemplateRows) {
        if (!billItem.getItem().isDiscountAllowed()) {
            return 0.0;
        }

        double totalDiscount = billItem.getBill().getDiscount();
        int billItemCount = 0;

        for (ReportTemplateRow row : reportTemplateRows) {
            BillItem item = row.getBillItem();

            if (item.getBill().equals(billItem.getBill()) && item.getItem().isDiscountAllowed()) {
                billItemCount++;
            }
        }

        return billItemCount > 0 ? totalDiscount / billItemCount : 0.0;
    }

    public double calculateDiscountForIP(final BillItem billItem, final List<ReportTemplateRow> reportTemplateRows) {
        double totalDiscount = billItem.getBill().getDiscount();
        int billItemCount = 0;

        for (ReportTemplateRow row : reportTemplateRows) {
            BillItem item = row.getBillItem();

            if (item.getBill().equals(billItem.getBill()) && billItem.getNetValue() > 0.0) {
                billItemCount++;
            }
        }

        return billItemCount > 0 ? totalDiscount / billItemCount : 0.0;
    }

    public ReportTemplateRowBundle generateDiscountBills(List<BillTypeAtomic> bts) {
        Map<String, Object> parameters = new HashMap<>();

//        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
//                + "FROM Bill bill "
//                + "WHERE bill.retired <> :br "
//                + "AND bill.paymentScheme is not null ";
        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(bill) "
                + "FROM Bill bill "
                + "WHERE bill.retired <> :br "
                + "AND bill.discount > 0 ";

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

//        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(billItem) "
//                + "FROM BillItem billItem "
//                + "JOIN billItem.bill bill "
//                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br "
//                + "AND billItem.bill.paymentScheme is not null ";
        String jpql = "SELECT new com.divudi.core.data.ReportTemplateRow(billItem) "
                + "FROM BillItem billItem "
                + "JOIN billItem.bill bill "
                + "WHERE billItem.retired <> :bfr AND bill.retired <> :br "
                + "AND bill.discount > 0 ";

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

        if (visitType.equalsIgnoreCase("IP")) {
            jpql += "AND billItem.netValue > 0 ";
        }

        parameters.put("fd", fromDate);
        parameters.put("td", toDate);

        jpql += "GROUP BY billItem";

        List<ReportTemplateRow> rs = (List<ReportTemplateRow>) paymentFacade.findLightsByJpql(jpql, parameters, TemporalType.TIMESTAMP);

        for (ReportTemplateRow row : rs) {
            BillItem billItem = row.getBillItem();

            billItem.setDiscount(visitType.equalsIgnoreCase("OP") ? calculateDiscountForOP(billItem, rs) : calculateDiscountForIP(billItem, rs));
        }

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

                detailsTable.addCell("Total");
                detailsTable.addCell(String.valueOf(calculateTotalHospitalFeeByBillItems(billItems)));
                detailsTable.addCell(String.valueOf(calculateTotalStaffFeeByBillItems(billItems)));
                detailsTable.addCell(String.valueOf(calculateTotalCollectionCenterFeeByBillItems(billItems)));
                detailsTable.addCell(String.valueOf(calculateTotalNetValueByBillItems(billItems)));

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

        SimpleDateFormat sdf = new SimpleDateFormat("dd MM yyyy hh:mm:ss a");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {

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
                String formattedDate = sdf.format(firstItem.getBill().getCreatedAt());
                row.createCell(4).setCellValue(formattedDate);
                row.createCell(5).setCellValue(entry.getKey());

                Row detailHeaderRow = sheet.createRow(rowIndex++);
                detailHeaderRow.createCell(6).setCellValue("Service Name");
                detailHeaderRow.createCell(7).setCellValue("Hos Fee.");
                detailHeaderRow.createCell(8).setCellValue("Staff Fee.");
                detailHeaderRow.createCell(9).setCellValue("CC Fee.");
                detailHeaderRow.createCell(10).setCellValue("Net Amount");

                for (BillItem bi : billItems) {
                    Row detailRow = sheet.createRow(rowIndex++);
                    detailRow.createCell(6).setCellValue(bi.getItem().getName());
                    detailRow.createCell(7).setCellValue(bi.getHospitalFee());
                    detailRow.createCell(8).setCellValue(bi.getStaffFee());
                    detailRow.createCell(9).setCellValue(bi.getCollectingCentreFee());
                    detailRow.createCell(10).setCellValue(bi.getNetValue());
                }

                Row footerRow = sheet.createRow(rowIndex++);
                footerRow.createCell(6).setCellValue("Total");
                footerRow.createCell(7).setCellValue(calculateTotalHospitalFeeByBillItems(billItems));
                footerRow.createCell(8).setCellValue(calculateTotalStaffFeeByBillItems(billItems));
                footerRow.createCell(9).setCellValue(calculateTotalCollectionCenterFeeByBillItems(billItems));
                footerRow.createCell(10).setCellValue(calculateTotalNetValueByBillItems(billItems));
            }

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportWeeklyOPDSummaryReportToPDF() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Weekly_Summary_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);

            document.open();

            document.add(new Paragraph("Weekly Summary Report",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));

            document.add(new Paragraph("Month: " + getMonth(),
                    FontFactory.getFont(FontFactory.HELVETICA, 12)));
            document.add(new Paragraph("Report Type: Summary",
                    FontFactory.getFont(FontFactory.HELVETICA, 12)));
            document.add(new Paragraph(" "));

            for (Map.Entry<Integer, Map<String, Map<Integer, Double>>> entry : getGroupedBillItemsWeekly().entrySet()) {
                Integer key = entry.getKey();
                Map<String, Map<Integer, Double>> weeklyData = entry.getValue();

                PdfPTable table = new PdfPTable(8);
                table.setWidthPercentage(100);
                float[] columnWidths = {3f, 1f, 1f, 1f, 1f, 1f, 1f, 1.5f};
                table.setWidths(columnWidths);

                table.addCell(new PdfPCell(new Phrase(getShiftDescription(key),
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12))));

                for (int week = 1; week <= 6; week++) {
                    table.addCell(new PdfPCell(new Phrase(getDateRangeText(week, true),
                            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12))));
                }
                table.addCell(new PdfPCell(new Phrase("Total",
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12))));

                for (Map.Entry<String, Map<Integer, Double>> rowEntry : weeklyData.entrySet()) {
                    String name = rowEntry.getKey();
                    Map<Integer, Double> weekValues = rowEntry.getValue();

                    table.addCell(new PdfPCell(new Phrase(name)));

                    for (int week = 1; week <= 6; week++) {
                        table.addCell(String.valueOf(weekValues.getOrDefault(week, 0.0)));
                    }

                    double total = weekValues.values().stream().mapToDouble(Double::doubleValue).sum();
                    table.addCell(String.valueOf(total));
                }

                document.add(table);
                document.add(new Paragraph(" "));
            }

            document.close();
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportWeeklyOPDSummaryReportToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Weekly_Summary_Report.xlsx");

        try (OutputStream out = response.getOutputStream(); Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Weekly Summary Report");

            int rowIndex = 0;

            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Weekly Summary Report");
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 18);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

            Row metaRow1 = sheet.createRow(rowIndex++);
            metaRow1.createCell(0).setCellValue("Month: " + getMonth());

            Row metaRow2 = sheet.createRow(rowIndex++);
            metaRow2.createCell(0).setCellValue("Report Type: Summary");

            rowIndex++;

            for (Map.Entry<Integer, Map<String, Map<Integer, Double>>> entry : getGroupedBillItemsWeekly().entrySet()) {
                Integer key = entry.getKey();
                Map<String, Map<Integer, Double>> weeklyData = entry.getValue();

                Row shiftRow = sheet.createRow(rowIndex++);
                Cell shiftCell = shiftRow.createCell(0);
                shiftCell.setCellValue(getShiftDescription(key));
                CellStyle shiftStyle = workbook.createCellStyle();
                Font shiftFont = workbook.createFont();
                shiftFont.setBold(true);
                shiftStyle.setFont(shiftFont);
                shiftCell.setCellStyle(shiftStyle);

                Row headerRow = sheet.createRow(rowIndex++);
                String[] headers = {"Name", getDateRangeText(1, true), getDateRangeText(2, true),
                        getDateRangeText(3, true), getDateRangeText(4, true),
                        getDateRangeText(5, true), getDateRangeText(6, true), "Total"};
                for (int col = 0; col < headers.length; col++) {
                    Cell cell = headerRow.createCell(col);
                    cell.setCellValue(headers[col]);
                    CellStyle headerStyle = workbook.createCellStyle();
                    headerStyle.setFont(shiftFont);
                    headerStyle.setBorderBottom(BorderStyle.THIN);
                    cell.setCellStyle(headerStyle);
                }

                for (Map.Entry<String, Map<Integer, Double>> rowEntry : weeklyData.entrySet()) {
                    Row dataRow = sheet.createRow(rowIndex++);
                    String name = rowEntry.getKey();
                    Map<Integer, Double> weekValues = rowEntry.getValue();

                    dataRow.createCell(0).setCellValue(name);

                    double total = 0.0;
                    for (int week = 1; week <= 6; week++) {
                        double value = weekValues.getOrDefault(week, 0.0);
                        dataRow.createCell(week).setCellValue(value);
                        total += value;
                    }

                    dataRow.createCell(7).setCellValue(total);
                }

                rowIndex++;
            }

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getShiftDescription(Integer key) {
        switch (key) {
            case 1:
                return "7:00 PM - 7:00 AM (Night)";
            case 2:
                return "7:00 AM - 1:00 PM";
            case 3:
                return "1:00 PM - 7:00 PM";
            default:
                return "Unknown Shift";
        }
    }

    public void exportDetailedWeeklyOPDReportToPDF() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Detailed_Weekly_OPD_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, out);

            document.open();

            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            com.itextpdf.text.Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            com.itextpdf.text.Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            for (int week = 1; week <= 6; week++) {
                List<Integer> daysOfWeek = getDaysOfWeek(week);

                if (daysOfWeek.isEmpty()) {
                    continue;
                }

                Paragraph title = new Paragraph("Detailed Weekly OPD Report", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                document.add(title);

                document.add(new Paragraph("Week: " + week, headerFont));
                document.add(new Paragraph(getDateRangeText(week, false), headerFont));
                document.add(new Paragraph("Report Type: Detail", regularFont));
                document.add(Chunk.NEWLINE);

                addWeeklyReportSection(document, "Weekly Report OPD (7.00pm - 7.00am) Night",
                        getItemListByWeek(week, weeklyDailyBillItemMap7to7), daysOfWeek, weeklyDailyBillItemMap7to7, week, headerFont, regularFont);

                addWeeklyReportSection(document, "Weekly Report OPD (7.00pm - 1.00pm) Night",
                        getItemListByWeek(week, weeklyDailyBillItemMap7to1), daysOfWeek, weeklyDailyBillItemMap7to1, week, headerFont, regularFont);

                addWeeklyReportSection(document, "Weekly Report OPD (1.00pm - 7.00am) Night",
                        getItemListByWeek(week, weeklyDailyBillItemMap1to7), daysOfWeek, weeklyDailyBillItemMap1to7, week, headerFont, regularFont);

                document.add(Chunk.NEWLINE);
            }

            document.close();
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addWeeklyReportSection(Document document, String sectionTitle, List<String> itemList,
                                        List<Integer> daysOfWeek, Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap,
                                        int week, com.itextpdf.text.Font headerFont, com.itextpdf.text.Font regularFont) throws DocumentException {
        document.add(new com.itextpdf.text.Paragraph(sectionTitle, headerFont));
        document.add(com.itextpdf.text.Chunk.NEWLINE);

        com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(daysOfWeek.size() + 2); // +2 for Item and Total columns
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        table.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Item", headerFont)));
        for (int day : daysOfWeek) {
            table.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(String.valueOf(day), headerFont)));
        }
        table.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase("Total", headerFont)));

        for (String item : itemList) {
            table.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(item, regularFont)));
            for (int day : daysOfWeek) {
                double count = getCountByWeekAndDay(week, day, item, weeklyDailyBillItemMap);
                table.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(String.valueOf(count), regularFont)));
            }
            double total = getSumByWeek(week, item, weeklyDailyBillItemMap);
            table.addCell(new com.itextpdf.text.pdf.PdfPCell(new com.itextpdf.text.Phrase(String.valueOf(total), regularFont)));
        }

        document.add(table);
    }

    public void exportDetailedWeeklyOPDReportToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext externalContext = context.getExternalContext();
        HttpServletResponse response = (HttpServletResponse) externalContext.getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Detailed_Weekly_OPD_Report.xlsx");

        try (OutputStream out = response.getOutputStream(); Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Detailed Weekly OPD Report");

            int rowIndex = 0;

            for (int week = 1; week <= 6; week++) {
                List<Integer> daysOfWeek = getDaysOfWeek(week);

                if (daysOfWeek.isEmpty()) {
                    continue;
                }

                Row headerRow1 = sheet.createRow(rowIndex++);
                Cell headerCell1 = headerRow1.createCell(0);
                headerCell1.setCellValue("Report Type: Detail");
                CellStyle boldStyle = workbook.createCellStyle();
                Font boldFont = workbook.createFont();
                boldFont.setBold(true);
                boldStyle.setFont(boldFont);
                headerCell1.setCellStyle(boldStyle);

                Row headerRow2 = sheet.createRow(rowIndex++);
                headerRow2.createCell(0).setCellValue("Week: " + week);

                Row headerRow3 = sheet.createRow(rowIndex++);
                headerRow3.createCell(0).setCellValue(getDateRangeText(week, false));

                Row headerRow4 = sheet.createRow(rowIndex++);
                headerRow4.createCell(0).setCellValue("Weekly Report OPD (7.00pm - 7.00am) Night");

                rowIndex++;

                Row columnHeaderRow = sheet.createRow(rowIndex++);
                columnHeaderRow.createCell(0).setCellValue("Item");
                int colIndex = 1;
                for (int day : daysOfWeek) {
                    columnHeaderRow.createCell(colIndex++).setCellValue(day);
                }
                columnHeaderRow.createCell(colIndex).setCellValue("Total");

                // Data Rows for (7.00 PM - 7.00 AM)
                populateDataRows(sheet, rowIndex, getItemListByWeek(week, weeklyDailyBillItemMap7to7), daysOfWeek, weeklyDailyBillItemMap7to7, week);
                rowIndex += getItemListByWeek(week, weeklyDailyBillItemMap7to7).size();

                // Section for (7.00 PM - 1.00 PM)
                rowIndex++;
                Row sectionHeaderRow1 = sheet.createRow(rowIndex++);
                sectionHeaderRow1.createCell(0).setCellValue("Weekly Report OPD (7.00pm - 1.00pm) Night");

                populateDataRows(sheet, rowIndex, getItemListByWeek(week, weeklyDailyBillItemMap7to1), daysOfWeek, weeklyDailyBillItemMap7to1, week);
                rowIndex += getItemListByWeek(week, weeklyDailyBillItemMap7to1).size();

                // Section for (1.00 PM - 7.00 AM)
                rowIndex++;
                Row sectionHeaderRow2 = sheet.createRow(rowIndex++);
                sectionHeaderRow2.createCell(0).setCellValue("Weekly Report OPD (1.00pm - 7.00am) Night");

                populateDataRows(sheet, rowIndex, getItemListByWeek(week, weeklyDailyBillItemMap1to7), daysOfWeek, weeklyDailyBillItemMap1to7, week);
                rowIndex += getItemListByWeek(week, weeklyDailyBillItemMap1to7).size();

                rowIndex++;
            }

            workbook.write(out);
            context.responseComplete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void populateDataRows(Sheet sheet, int rowIndex, List<String> itemList, List<Integer> daysOfWeek, Map<Integer, Map<String, Map<Integer, Double>>> weeklyDailyBillItemMap, int week) {
        for (String item : itemList) {
            Row dataRow = sheet.createRow(rowIndex++);
            int colIndex = 0;

            dataRow.createCell(colIndex++).setCellValue(item);

            for (int day : daysOfWeek) {
                double count = getCountByWeekAndDay(week, day, item, weeklyDailyBillItemMap);
                dataRow.createCell(colIndex++).setCellValue(count);
            }

            double total = getSumByWeek(week, item, weeklyDailyBillItemMap);
            dataRow.createCell(colIndex).setCellValue(total);
        }
    }

    public void exportRouteAnalysisDetailReportToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Collecting_Center_Monthly_Report.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("Monthly Report");
            int rowIndex = 0;

            Row headerRow = sheet.createRow(rowIndex++);
            headerRow.createCell(0).setCellValue("S. No");
            headerRow.createCell(1).setCellValue("Collecting Center Code");
            headerRow.createCell(2).setCellValue("Collecting Center");

            List<YearMonth> yearMonths = getYearMonths();
            int dynamicColumnIndex = 3;
            for (YearMonth yearMonth : yearMonths) {
                headerRow.createCell(dynamicColumnIndex++).setCellValue(yearMonth.toString() + " - Sample Count");
                headerRow.createCell(dynamicColumnIndex++).setCellValue(yearMonth.toString() + " - Service Amount");
            }

            int serialNumber = 1;
            for (Map.Entry<Institution, Map<YearMonth, Bill>> entrySet : getGroupedCollectingCenterWiseBillsMonthly().entrySet()) {
                Institution center = entrySet.getKey();
                Map<YearMonth, Bill> monthlyData = entrySet.getValue();

                Row dataRow = sheet.createRow(rowIndex++);
                dataRow.createCell(0).setCellValue(serialNumber++);
                dataRow.createCell(1).setCellValue(center.getCode());
                dataRow.createCell(2).setCellValue(center.getName());

                dynamicColumnIndex = 3;
                for (YearMonth yearMonth : yearMonths) {
                    Bill bill = monthlyData.get(yearMonth);

                    if (bill != null) {
                        dataRow.createCell(dynamicColumnIndex++).setCellValue(bill.getQty());
                        dataRow.createCell(dynamicColumnIndex++).setCellValue(
                                BigDecimal.valueOf(bill.getTotalHospitalFee()).setScale(2, RoundingMode.HALF_UP).doubleValue()
                        );

                    } else {
                        dataRow.createCell(dynamicColumnIndex++).setCellValue(0);
                        dataRow.createCell(dynamicColumnIndex++).setCellValue(0.0);
                    }
                }
            }

            Row totalRow = sheet.createRow(rowIndex++);
            totalRow.createCell(0).setCellValue("Total");
            totalRow.createCell(1).setCellValue("");
            totalRow.createCell(2).setCellValue("");

            dynamicColumnIndex = 3;
            for (YearMonth yearMonth : yearMonths) {
                totalRow.createCell(dynamicColumnIndex++).setCellValue(String.format("%.2f", getCollectionCenterWiseTotalSampleCount(yearMonth)
                        / calculateCollectionCenterWiseBillCount(yearMonth)));
                totalRow.createCell(dynamicColumnIndex++).setCellValue(String.format("%.2f", getCollectionCenterWiseTotalServiceAmount(yearMonth)
                        / calculateCollectionCenterWiseBillCount(yearMonth)));
            }

            XSSFSheet countChartSheet = workbook.createSheet("Sample Count Chart");

            Row chartHeader = countChartSheet.createRow(0);
            chartHeader.createCell(0).setCellValue("Month");
            chartHeader.createCell(1).setCellValue("Sample Count");

            Map<YearMonth, Double> countData = getSampleCountChartData();
            int chartRowIndex = 1;
            for (Map.Entry<YearMonth, Double> entry : countData.entrySet()) {
                Row row = countChartSheet.createRow(chartRowIndex++);
                row.createCell(0).setCellValue(entry.getKey().toString());
                row.createCell(1).setCellValue(entry.getValue());
            }

            XSSFDrawing drawing = countChartSheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 3, 1, 15, 20);

            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText("Sample Count Over Months");

            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.BOTTOM);

            XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
            bottomAxis.setTitle("Month");
            XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
            leftAxis.setTitle("Sample Count");
            leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

            XDDFDataSource<String> months = XDDFDataSourcesFactory.fromStringCellRange(countChartSheet,
                    new CellRangeAddress(1, chartRowIndex - 1, 0, 0));
            XDDFNumericalDataSource<Double> counts = XDDFDataSourcesFactory.fromNumericCellRange(countChartSheet,
                    new CellRangeAddress(1, chartRowIndex - 1, 1, 1));

            XDDFLineChartData chartData = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
            XDDFLineChartData.Series series = (XDDFLineChartData.Series) chartData.addSeries(months, counts);
            series.setTitle("Sample Count", null);
            series.setSmooth(false);
            series.setMarkerStyle(MarkerStyle.CIRCLE);

            chart.plot(chartData);

            XSSFSheet serviceChartSheet = workbook.createSheet("Service Amount Chart");

            Row serviceHeader = serviceChartSheet.createRow(0);
            serviceHeader.createCell(0).setCellValue("Month");
            serviceHeader.createCell(1).setCellValue("Service Amount");

            Map<YearMonth, Double> serviceData = getServiceAmountChartData();
            int serviceRowIndex = 1;
            for (Map.Entry<YearMonth, Double> entry : serviceData.entrySet()) {
                Row row = serviceChartSheet.createRow(serviceRowIndex++);
                row.createCell(0).setCellValue(entry.getKey().toString());
                row.createCell(1).setCellValue(entry.getValue());
            }

            XSSFDrawing serviceDrawing = serviceChartSheet.createDrawingPatriarch();
            XSSFClientAnchor serviceAnchor = serviceDrawing.createAnchor(0, 0, 0, 0, 3, 1, 15, 20);

            XSSFChart serviceChart = serviceDrawing.createChart(serviceAnchor);
            serviceChart.setTitleText("Service Amount Over Months");

            XDDFChartLegend serviceLegend = serviceChart.getOrAddLegend();
            serviceLegend.setPosition(LegendPosition.BOTTOM);

            XDDFCategoryAxis serviceBottomAxis = serviceChart.createCategoryAxis(AxisPosition.BOTTOM);
            serviceBottomAxis.setTitle("Month");
            XDDFValueAxis serviceLeftAxis = serviceChart.createValueAxis(AxisPosition.LEFT);
            serviceLeftAxis.setTitle("Service Amount");
            serviceLeftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

            XDDFDataSource<String> serviceMonths = XDDFDataSourcesFactory.fromStringCellRange(serviceChartSheet,
                    new CellRangeAddress(1, serviceRowIndex - 1, 0, 0));
            XDDFNumericalDataSource<Double> serviceValues = XDDFDataSourcesFactory.fromNumericCellRange(serviceChartSheet,
                    new CellRangeAddress(1, serviceRowIndex - 1, 1, 1));

            XDDFLineChartData serviceChartData = (XDDFLineChartData) serviceChart.createData(ChartTypes.LINE, serviceBottomAxis, serviceLeftAxis);
            XDDFLineChartData.Series serviceSeries = (XDDFLineChartData.Series) serviceChartData.addSeries(serviceMonths, serviceValues);
            serviceSeries.setTitle("Service Amount", null);
            serviceSeries.setSmooth(false);
            serviceSeries.setMarkerStyle(MarkerStyle.SQUARE);
            serviceChart.plot(serviceChartData);

            workbook.write(out);
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportRouteAnalysisDetailReportToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Collecting_Center_Monthly_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Collecting Center Monthly Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            document.add(new Paragraph("Sample Count Over Months", titleFont));
            Image countChartImage = Image.getInstance(generateChartAsBytes("Sample Count Over Months",
                    getSampleCountChartData(), "Month", "Sample Count"));
            countChartImage.scaleToFit(500, 300);
            countChartImage.setAlignment(Element.ALIGN_CENTER);
            document.add(countChartImage);

            document.add(new Paragraph("\n\nService Amount Over Months", titleFont));
            Image amountChartImage = Image.getInstance(generateChartAsBytes("Service Amount Over Months",
                    getServiceAmountChartData(), "Month", "Service Amount"));
            amountChartImage.scaleToFit(500, 300);
            amountChartImage.setAlignment(Element.ALIGN_CENTER);
            document.add(amountChartImage);

            document.newPage();

            PdfPTable table = new PdfPTable(3 + getYearMonths().size() * 2);
            table.setWidthPercentage(100);

            table.addCell(new PdfPCell(new Phrase("S. No")));
            table.addCell(new PdfPCell(new Phrase("Collecting Center Code")));
            table.addCell(new PdfPCell(new Phrase("Collecting Center")));

            List<YearMonth> yearMonths = getYearMonths();
            for (YearMonth yearMonth : yearMonths) {
                table.addCell(new PdfPCell(new Phrase(yearMonth.toString() + " - Sample Count")));
                table.addCell(new PdfPCell(new Phrase(yearMonth.toString() + " - Service Amount")));
            }

            int serialNumber = 1;
            for (Map.Entry<Institution, Map<YearMonth, Bill>> entrySet : getGroupedCollectingCenterWiseBillsMonthly().entrySet()) {
                Institution center = entrySet.getKey();
                Map<YearMonth, Bill> monthlyData = entrySet.getValue();

                table.addCell(new PdfPCell(new Phrase(String.valueOf(serialNumber++))));
                table.addCell(new PdfPCell(new Phrase(center.getCode())));
                table.addCell(new PdfPCell(new Phrase(center.getName())));

                for (YearMonth yearMonth : yearMonths) {
                    Bill bill = monthlyData.get(yearMonth);
                    table.addCell(new PdfPCell(new Phrase(bill != null ? String.valueOf(bill.getQty()) : "0")));
                    table.addCell(new PdfPCell(new Phrase(
                            bill != null ? String.format("%.2f", bill.getTotalHospitalFee()) : "0.00"
                    )));

                }
            }

            PdfPCell totalCell = new PdfPCell(new Phrase("Total"));
            totalCell.setColspan(3);
            totalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(totalCell);

            for (YearMonth yearMonth : yearMonths) {
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f", getCollectionCenterWiseTotalSampleCount(yearMonth)
                        / calculateCollectionCenterWiseBillCount(yearMonth)))));
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f", getCollectionCenterWiseTotalServiceAmount(yearMonth)
                        / calculateCollectionCenterWiseBillCount(yearMonth)))));
            }

            document.add(table);
            document.close();
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void exportRouteAnalysisSummaryReportToPdf() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=Route_Wise_Monthly_Report.pdf");

        try (OutputStream out = response.getOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            com.itextpdf.text.Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Paragraph title = new Paragraph("Route Wise Monthly Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            document.add(new Paragraph("Sample Count Over Months", titleFont));
            Image countChartImage = Image.getInstance(generateChartAsBytes("Sample Count Over Months", getSampleCountChartData(), "Month", "Sample Count"));
            countChartImage.scaleToFit(500, 300);
            countChartImage.setAlignment(Element.ALIGN_CENTER);
            document.add(countChartImage);

            document.add(new Paragraph("\n\nService Amount Over Months", titleFont));
            Image serviceChartImage = Image.getInstance(generateChartAsBytes("Service Amount Over Months", getServiceAmountChartData(), "Month", "Service Amount"));
            serviceChartImage.scaleToFit(500, 300);
            serviceChartImage.setAlignment(Element.ALIGN_CENTER);
            document.add(serviceChartImage);

            document.newPage();

            PdfPTable table = new PdfPTable(3 + getYearMonths().size() * 2);
            table.setWidthPercentage(100);

            table.addCell(new PdfPCell(new Phrase("S. No")));
            table.addCell(new PdfPCell(new Phrase("Route Code")));
            table.addCell(new PdfPCell(new Phrase("Route")));

            List<YearMonth> yearMonths = getYearMonths();
            for (YearMonth yearMonth : yearMonths) {
                table.addCell(new PdfPCell(new Phrase(yearMonth.toString() + " - Sample Count")));
                table.addCell(new PdfPCell(new Phrase(yearMonth.toString() + " - Service Amount")));
            }

            int serialNumber = 1;
            for (Map.Entry<Route, Map<YearMonth, Bill>> entrySet : getGroupedRouteWiseBillsMonthly().entrySet()) {
                Route route = entrySet.getKey();
                Map<YearMonth, Bill> monthlyData = entrySet.getValue();

                table.addCell(new PdfPCell(new Phrase(String.valueOf(serialNumber++))));
                table.addCell(new PdfPCell(new Phrase(route.getCode())));
                table.addCell(new PdfPCell(new Phrase(route.getName())));

                for (YearMonth yearMonth : yearMonths) {
                    Bill billData = monthlyData.get(yearMonth);
                    table.addCell(new PdfPCell(new Phrase(billData != null ? String.valueOf(billData.getQty()) : "0")));
                    table.addCell(new PdfPCell(new Phrase(
                            billData != null ? String.format("%.2f", billData.getTotalHospitalFee()) : "0.00"
                    )));
                }
            }

            PdfPCell totalCell = new PdfPCell(new Phrase("Total"));
            totalCell.setColspan(3);
            totalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(totalCell);

            for (YearMonth yearMonth : yearMonths) {
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f", calculateRouteWiseTotalSampleCount(yearMonth) / calculateRouteWiseBillCount(yearMonth)))));
                table.addCell(new PdfPCell(new Phrase(String.format("%.2f", calculateRouteWiseTotalServiceAmount(yearMonth) / calculateRouteWiseBillCount(yearMonth)))));
            }

            document.add(table);
            document.close();
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] generateChartAsBytes(String title, Map<YearMonth, Double> data, String categoryLabel, String valueLabel) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (Map.Entry<YearMonth, Double> entry : data.entrySet()) {
            dataset.addValue(entry.getValue(), valueLabel, entry.getKey().toString());
        }

        JFreeChart chart = ChartFactory.createLineChart(
                title,
                categoryLabel,
                valueLabel,
                dataset
        );

        try (ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
            ChartUtils.writeChartAsPNG(bao, chart, 600, 300);
            return bao.toByteArray();
        }
    }

    public void exportRouteAnalysisSummaryReportToExcel() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletResponse response = (HttpServletResponse) context.getExternalContext().getResponse();

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Route_Wise_Monthly_Report.xlsx");

        try (XSSFWorkbook workbook = new XSSFWorkbook(); OutputStream out = response.getOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("Route Wise Monthly Report");
            int rowIndex = 0;

            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Route Wise Monthly Report");
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2 + getYearMonths().size() * 2 - 1));

            Row headerRow = sheet.createRow(rowIndex++);
            int cellIndex = 0;
            headerRow.createCell(cellIndex++).setCellValue("S. No");
            headerRow.createCell(cellIndex++).setCellValue("Route Code");
            headerRow.createCell(cellIndex++).setCellValue("Route");

            List<YearMonth> yearMonths = getYearMonths();
            for (YearMonth yearMonth : yearMonths) {
                headerRow.createCell(cellIndex++).setCellValue(yearMonth.toString() + " - Sample Count");
                headerRow.createCell(cellIndex++).setCellValue(yearMonth.toString() + " - Service Amount");
            }

            int serialNumber = 1;
            for (Map.Entry<Route, Map<YearMonth, Bill>> entrySet : getGroupedRouteWiseBillsMonthly().entrySet()) {
                Route route = entrySet.getKey();
                Map<YearMonth, Bill> monthlyData = entrySet.getValue();

                Row row = sheet.createRow(rowIndex++);
                cellIndex = 0;

                row.createCell(cellIndex++).setCellValue(serialNumber++);
                row.createCell(cellIndex++).setCellValue(route.getCode());
                row.createCell(cellIndex++).setCellValue(route.getName());

                for (YearMonth yearMonth : yearMonths) {
                    Bill billData = monthlyData.get(yearMonth);
                    if (billData != null) {
                        row.createCell(cellIndex++).setCellValue(billData.getQty());
                        row.createCell(cellIndex++).setCellValue(
                                BigDecimal.valueOf(billData.getTotalHospitalFee()).setScale(2, RoundingMode.HALF_UP).doubleValue()
                        );

                    } else {
                        row.createCell(cellIndex++).setCellValue(0);
                        row.createCell(cellIndex++).setCellValue(0.0);
                    }
                }
            }

            Row totalRow = sheet.createRow(rowIndex++);
            totalRow.createCell(0).setCellValue("Total");
            totalRow.createCell(1).setCellValue("");
            totalRow.createCell(2).setCellValue("");

            cellIndex = 3;
            for (YearMonth yearMonth : yearMonths) {
                totalRow.createCell(cellIndex++).setCellValue(
                        String.format("%.2f", calculateRouteWiseTotalSampleCount(yearMonth)
                                / calculateRouteWiseBillCount(yearMonth)));
                totalRow.createCell(cellIndex++).setCellValue(
                        String.format("%.2f", calculateRouteWiseTotalServiceAmount(yearMonth)
                                / calculateRouteWiseBillCount(yearMonth)));
            }

            XSSFSheet countChartSheet = workbook.createSheet("Sample Count Chart");

            Row chartHeader = countChartSheet.createRow(0);
            chartHeader.createCell(0).setCellValue("Month");
            chartHeader.createCell(1).setCellValue("Sample Count");

            Map<YearMonth, Double> countData = getSampleCountChartData();
            int chartRowIndex = 1;
            for (Map.Entry<YearMonth, Double> entry : countData.entrySet()) {
                Row row = countChartSheet.createRow(chartRowIndex++);
                row.createCell(0).setCellValue(entry.getKey().toString());
                row.createCell(1).setCellValue(entry.getValue());
            }

            XSSFDrawing drawing = countChartSheet.createDrawingPatriarch();
            XSSFClientAnchor anchor = drawing.createAnchor(0, 0, 0, 0, 3, 1, 15, 20);

            XSSFChart chart = drawing.createChart(anchor);
            chart.setTitleText("Sample Count Over Months");

            XDDFChartLegend legend = chart.getOrAddLegend();
            legend.setPosition(LegendPosition.BOTTOM);

            XDDFCategoryAxis bottomAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
            bottomAxis.setTitle("Month");
            XDDFValueAxis leftAxis = chart.createValueAxis(AxisPosition.LEFT);
            leftAxis.setTitle("Sample Count");
            leftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

            XDDFDataSource<String> months = XDDFDataSourcesFactory.fromStringCellRange(countChartSheet,
                    new CellRangeAddress(1, chartRowIndex - 1, 0, 0));
            XDDFNumericalDataSource<Double> counts = XDDFDataSourcesFactory.fromNumericCellRange(countChartSheet,
                    new CellRangeAddress(1, chartRowIndex - 1, 1, 1));

            XDDFLineChartData chartData = (XDDFLineChartData) chart.createData(ChartTypes.LINE, bottomAxis, leftAxis);
            XDDFLineChartData.Series series = (XDDFLineChartData.Series) chartData.addSeries(months, counts);
            series.setTitle("Sample Count", null);
            series.setSmooth(false);
            series.setMarkerStyle(MarkerStyle.CIRCLE);

            chart.plot(chartData);

            XSSFSheet serviceChartSheet = workbook.createSheet("Service Amount Chart");

            Row serviceHeader = serviceChartSheet.createRow(0);
            serviceHeader.createCell(0).setCellValue("Month");
            serviceHeader.createCell(1).setCellValue("Service Amount");

            Map<YearMonth, Double> serviceData = getServiceAmountChartData();
            int serviceRowIndex = 1;
            for (Map.Entry<YearMonth, Double> entry : serviceData.entrySet()) {
                Row row = serviceChartSheet.createRow(serviceRowIndex++);
                row.createCell(0).setCellValue(entry.getKey().toString());
                row.createCell(1).setCellValue(entry.getValue());
            }

            XSSFDrawing serviceDrawing = serviceChartSheet.createDrawingPatriarch();
            XSSFClientAnchor serviceAnchor = serviceDrawing.createAnchor(0, 0, 0, 0, 3, 1, 15, 20);

            XSSFChart serviceChart = serviceDrawing.createChart(serviceAnchor);
            serviceChart.setTitleText("Service Amount Over Months");

            XDDFChartLegend serviceLegend = serviceChart.getOrAddLegend();
            serviceLegend.setPosition(LegendPosition.BOTTOM);

            XDDFCategoryAxis serviceBottomAxis = serviceChart.createCategoryAxis(AxisPosition.BOTTOM);
            serviceBottomAxis.setTitle("Month");
            XDDFValueAxis serviceLeftAxis = serviceChart.createValueAxis(AxisPosition.LEFT);
            serviceLeftAxis.setTitle("Service Amount");
            serviceLeftAxis.setCrosses(AxisCrosses.AUTO_ZERO);

            XDDFDataSource<String> serviceMonths = XDDFDataSourcesFactory.fromStringCellRange(serviceChartSheet,
                    new CellRangeAddress(1, serviceRowIndex - 1, 0, 0));
            XDDFNumericalDataSource<Double> serviceValues = XDDFDataSourcesFactory.fromNumericCellRange(serviceChartSheet,
                    new CellRangeAddress(1, serviceRowIndex - 1, 1, 1));

            XDDFLineChartData serviceChartData = (XDDFLineChartData) serviceChart.createData(ChartTypes.LINE, serviceBottomAxis, serviceLeftAxis);
            XDDFLineChartData.Series serviceSeries = (XDDFLineChartData.Series) serviceChartData.addSeries(serviceMonths, serviceValues);
            serviceSeries.setTitle("Service Amount", null);
            serviceSeries.setSmooth(false);
            serviceSeries.setMarkerStyle(MarkerStyle.SQUARE);
            serviceChart.plot(serviceChartData);

            workbook.write(out);
            context.responseComplete();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Double calculateIpGrossAmountSubTotalByBills(List<Bill> bills) {
        Double billTotal = 0.0;

        for (Bill bill : bills) {
            if (bill.getPatientEncounter() != null && bill.getPatientEncounter().getFinalBill() != null && !bill.getBillClassType().equals(BillClassType.CancelledBill)) {
                billTotal += bill.getPatientEncounter().getFinalBill().getGrantTotal();
            } else {
                billTotal += bill.getGrantTotal();
            }
        }

        return billTotal;
    }

    public Double calculateIpDiscountSubTotalByBills(List<Bill> bills) {
        Double discount = 0.0;

        for (Bill bill : bills) {
            if (bill.getPatientEncounter() != null && bill.getPatientEncounter().getFinalBill() != null && !bill.getBillClassType().equals(BillClassType.CancelledBill)) {
                discount += bill.getPatientEncounter().getFinalBill().getDiscount();
            } else {
                discount += bill.getDiscount();
            }
        }

        return discount;
    }

    public Double calculateIpNetAmountSubTotalByBills(List<Bill> bills) {
        Double netTotal = 0.0;

        for (Bill bill : bills) {
            if (bill.getPatientEncounter() != null && bill.getPatientEncounter().getFinalBill() != null && !bill.getBillClassType().equals(BillClassType.CancelledBill)) {
                netTotal += bill.getPatientEncounter().getFinalBill().getNetTotal();
            } else {
                netTotal += bill.getNetTotal();
            }
        }

        return netTotal;
    }

    public Double calculateIpPatientShareSubTotalByBills(List<Bill> bills) {
        Double settledAmountByPatient = 0.0;

        for (Bill bill : bills) {
            if (bill.getPatientEncounter() != null && bill.getPatientEncounter().getFinalBill() != null && !bill.getBillClassType().equals(BillClassType.CancelledBill)) {
                settledAmountByPatient += bill.getPatientEncounter().getFinalBill().getSettledAmountByPatient();
            } else {
                settledAmountByPatient += bill.getSettledAmountByPatient();
            }
        }

        return settledAmountByPatient;
    }

    public Double calculateIpSponsorShareSubTotalByBills(List<Bill> bills) {
        Double settledAmountBySponsor = 0.0;

        for (Bill bill : bills) {
            if (bill.getPatientEncounter() != null && bill.getPatientEncounter().getFinalBill() != null && !bill.getBillClassType().equals(BillClassType.CancelledBill)) {
                settledAmountBySponsor += bill.getPatientEncounter().getFinalBill().getSettledAmountBySponsor();
            } else {
                settledAmountBySponsor += bill.getSettledAmountBySponsor();
            }
        }

        return settledAmountBySponsor;
    }

    public Double calculateIpDueAmountSubTotalByBills(List<Bill> bills) {
        Double balance = 0.0;

        for (Bill bill : bills) {
            if (bill.getPatientEncounter() != null && bill.getPatientEncounter().getFinalBill() != null && !bill.getBillClassType().equals(BillClassType.CancelledBill)) {
                balance += bill.getPatientEncounter().getFinalBill().getNetTotal() - bill.getPatientEncounter().getFinalBill().getSettledAmountBySponsor()
                        - bill.getPatientEncounter().getFinalBill().getSettledAmountByPatient();

            } else {
                balance += bill.getNetTotal() - bill.getSettledAmountBySponsor() - bill.getSettledAmountByPatient();
            }
        }

        return balance;
    }

    public Double calculateIpGrossAmountNetTotal() {
        double grossAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        if (billMap == null || billMap.isEmpty()) {
            return grossAmountNetTotal;
        }

        for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
            List<Bill> bills = entry.getValue();

            grossAmountNetTotal += calculateIpGrossAmountSubTotalByBills(bills);
        }

        return grossAmountNetTotal;
    }

    public Double calculateIpDiscountNetTotal() {
        double discountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        if (billMap != null) {
            for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
                List<Bill> bills = entry.getValue();
                discountNetTotal += calculateIpDiscountSubTotalByBills(bills);
            }
        }

        return discountNetTotal;
    }

    public Double calculateIpNetAmountNetTotal() {
        double netAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        if (billMap != null) {
            for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
                List<Bill> bills = entry.getValue();

                netAmountNetTotal += calculateIpNetAmountSubTotalByBills(bills);
            }
        }

        return netAmountNetTotal;
    }

    public Double calculateIpPatientShareNetTotal() {
        double patientShareNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        if (billMap != null) {
            for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
                List<Bill> bills = entry.getValue();

                patientShareNetTotal += calculateIpPatientShareSubTotalByBills(bills);
            }
        }

        return patientShareNetTotal;
    }

    public Double calculateIpSponsorShareNetTotal() {
        double sponsorShareNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        if (billMap != null) {
            for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
                List<Bill> bills = entry.getValue();

                sponsorShareNetTotal += calculateIpSponsorShareSubTotalByBills(bills);
            }
        }

        return sponsorShareNetTotal;
    }

    public Double calculateIpDueAmountNetTotal() {
        double dueAmountNetTotal = 0.0;
        Map<Institution, List<Bill>> billMap = bundle.getGroupedBillItemsByInstitution();

        if (billMap != null) {
            for (Map.Entry<Institution, List<Bill>> entry : billMap.entrySet()) {
                List<Bill> bills = entry.getValue();

                dueAmountNetTotal += calculateIpDueAmountSubTotalByBills(bills);
            }
        }

        return dueAmountNetTotal;
    }

}
