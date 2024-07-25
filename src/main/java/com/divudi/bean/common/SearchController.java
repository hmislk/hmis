/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.pharmacy.PharmacyPreSettleController;
import com.divudi.bean.pharmacy.PharmacySaleBhtController;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.FeeType;
import com.divudi.data.InstitutionType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.MessageType;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.data.hr.ReportKeyWord;

import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.AuditEvent;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BillSession;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Doctor;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.Patient;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.Token;
import com.divudi.entity.inward.Admission;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.lab.PatientReport;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.BillSessionFacade;
import com.divudi.facade.PatientFacade;
import com.divudi.facade.PatientInvestigationFacade;
import com.divudi.facade.PatientReportFacade;
import com.divudi.facade.StockFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.opd.OpdBillController;
import com.divudi.bean.pharmacy.PharmacyBillSearch;
import com.divudi.data.BillClassType;
import com.divudi.data.BillFinanceType;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.analytics.ReportTemplateType;
import com.divudi.entity.Payment;
import com.divudi.entity.WebUser;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.entity.pharmacy.PharmaceuticalItem;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.TokenFacade;
import com.divudi.java.CommonFunctions;
import com.divudi.light.common.BillLight;
import com.divudi.light.common.BillSummaryRow;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.primefaces.model.file.UploadedFile;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class SearchController implements Serializable {

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

    /**
     * Properties
     */
    private ReportTemplateType reportTemplateType;
    private SearchKeyword searchKeyword;
    Date fromDate;
    Date toDate;
    private Long startBillId;
    private Long endBillId;
    private WebUser webUser;

    private int maxResult = 50;
    private BillType billType;
    private PaymentMethod paymentMethod;
    private List<PaymentMethod> paymentMethods;
    private List<Bill> bills;
    private List<Payment> payments;
    private List<BillLight> billLights;
    private List<BillSummaryRow> billSummaryRows;
    private List<Bill> selectedBills;
    List<Bill> aceptPaymentBills;
    private List<BillFee> billFees;
    private List<BillFee> billFeesDone;
    private List<BillItem> billItems;
    private List<PatientInvestigation> patientInvestigations;
    private List<PatientReport> patientReports;
    private List<PatientInvestigation> patientInvestigationsSigle;
    private BillTypeAtomic billTypeAtomic;

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
    List<BillItem> billItem;
    List<PatientInvestigation> userPatientInvestigations;

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
    List<PharmacyAdjustmentRow> pharmacyAdjustmentRows;

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

    private boolean duplicateBillView;

    public String navigateTobill(Bill bill) {
        String navigateTo = "";
        if (bill == null) {
            return "";
        }
        switch (bill.getBillTypeAtomic()) {
            //Opd Bill Navigation
            case OPD_BILL_WITH_PAYMENT:
                billSearch.setBill(bill);
                navigateTo = "/opd/bill_reprint.xhtml";
                break;
            case OPD_BILL_REFUND:
                billSearch.setBill(bill);
                navigateTo = "/opd/bill_reprint.xhtml";
                break;
            case OPD_BILL_CANCELLATION:
                billSearch.setBill(bill);
                navigateTo = "/opd/bill_reprint.xhtml";
                break;
            case OPD_BATCH_BILL_WITH_PAYMENT:
                System.out.println("bill = " + bill);
                opdBillController.setBatchBill(bill);
                navigateTo = "/opd/opd_batch_bill_print.xhtml";
                break;
            //Pharmacy Bill Navigation    
            case PHARMACY_RETAIL_SALE:
                pharmacyBillSearch.setBill(bill);
                navigateTo = "/pharmacy/pharmacy_reprint_bill_sale.xhtml";
                break;
            default:
                return navigateTo;
        }
        return navigateTo;
    }

    public String navigateToAllFinancialTransactionSummary() {
        billSummaryRows = null;
        return "/analytics/all_financial_transaction_summary?faces-redirect=true";
    }

    public String navigateToUserFinancialTransactionSummaryByBill() {
        billSummaryRows = null;
        return "/analytics/user_financial_transaction_summary_by_bill?faces-redirect=true";
    }

    public String navigateToAllFinancialTransactionSummaryCashier() {
        billSummaryRows = null;
        return "/analytics/all_financial_transaction_summary_by_user?faces-redirect=true";
    }

    public String navigateToFinancialTransactionSummaryPaymentMethod() {
        institution = sessionController.getInstitution();
        department = null;
        billSummaryRows = null;
        return "/analytics/financial_transaction_summary_PaymentMethod?faces-redirect=true";
    }

    public String navigateToAddToStockBillList() {
        printPreview = false;
        bills = null;
        return "/pharmacy/pharmacy_add_to_stock_search_bill?faces-redirect=true";
    }

    public String navigateToFinancialTransactionSummaryByUsers() {
        institution = sessionController.getInstitution();
        department = null;
        billSummaryRows = null;
        return "/analytics/financial_transaction_summary_Users?faces-redirect=true";
    }

    public String navigateToFinancialTransactionSummaryByUserPayment() {
        departments = departmentController.getInstitutionDepatrments(sessionController.getInstitution());
        //System.out.println("departments = " + departments);
        billSummaryRows = null;
        return "/analytics/financial_transaction_summary_Users_PaymentMethod?faces-redirect=true";
    }

    public String navigateToFinancialTransactionSummaryByDepartment() {
        department = sessionController.getDepartment();
        billSummaryRows = null;
        departments = departmentController.getInstitutionDepatrments(sessionController.getInstitution());
        return "/analytics/financial_transaction_summary_Department?faces-redirect=true";
    }

    public void clearBillList() {
        if (bills == null) {
            return;
        } else {
            bills = new ArrayList<>();
        }
    }

    public Bill searchBillFromTokenId(Long currentTokenId) {
        if (currentTokenId == null) {
            JsfUtil.addErrorMessage("Enter Correct Bill Number !");
            return null; // Return null if the token ID is null
        }
        String sql = "SELECT t.bill FROM Token t "
                + "WHERE t.retired = false "
                + "AND t.id = :tid";
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("tid", currentTokenId);
        return getBillFacade().findFirstByJpql(sql, hm);
    }

    public Token searchTokenFromTokenId(Long currentTokenId) {
        if (currentTokenId == null) {
            JsfUtil.addErrorMessage("Enter Correct Bill Number !");
            return null; // Return null if the token ID is null
        }
        String sql = "SELECT t FROM Token t "
                + "WHERE t.retired = false "
                + "AND t.id = :tid";
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("tid", currentTokenId);
        return tokenFacade.findFirstByJpql(sql, hm);
    }

    public Bill searchBillFromBillId(Long currentTokenId) {
        if (currentTokenId == null) {
            JsfUtil.addErrorMessage("Enter Correct Bill Number !");
            return null; // Return null if the token ID is null
        }
        String sql = " SELECT b "
                + " FROM Bill b "
                + " WHERE b.retired = false "
                + "AND b.id = :bid";
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("bid", currentTokenId);
        return getBillFacade().findFirstByJpql(sql, hm);
    }

    public String settleBillByBarcode() {
        System.out.println("settleBillByBarcode");
        currentBill = searchBillFromBillId(barcodeIdLong);
        System.out.println("currentBill by bill id= " + currentBill);
        if (currentBill == null) {
            currentBill = searchBillFromTokenId(barcodeIdLong);
            opdPreSettleController.setToken(searchTokenFromTokenId(barcodeIdLong));
            System.out.println("currentBill by token id = " + currentBill);
        }
        String action;
        if (currentBill == null) {
            JsfUtil.addErrorMessage("No Bill Found");
            return "";
        }
        if (currentBill.isPaid()) {
            JsfUtil.addErrorMessage("Error : Bill is Already Paid");
            return " ";
        }
        action = toSettle(currentBill);
        return action;

    }

    public String toSettle(Bill preBill) {
        System.out.println("preBill = " + preBill);
        System.out.println("preBill ATOMIC BILL TYPE= " + preBill.getBillTypeAtomic());

        if (preBill == null) {
            JsfUtil.addErrorMessage("No Such Prebill");
            return "";
        }
        String sql = "Select b from "
                + " BilledBill b"
                + " where b.referenceBill=:bil"
                + " and b.retired=false "
                + " and b.cancelled=false ";
        HashMap hm = new HashMap();
        hm.put("bil", preBill);
        Bill b = getBillFacade().findFirstByJpql(sql, hm);

        if (b != null) {
            JsfUtil.addErrorMessage("Allready Paid");
            return "";
        }

        if (preBill.getBillTypeAtomic() != null) {
            BillTypeAtomic bta = preBill.getBillTypeAtomic();
            switch (bta) {
                case OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER:
                    return opdPreSettleController.toSettle(preBill);
                case PHARMACY_RETAIL_SALE_PRE:
                case PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER:
                    setPreBillForPharmecy(preBill);
                    return "/pharmacy/pharmacy_bill_pre_settle?faces-redirect=true";
                default:
                    JsfUtil.addErrorMessage("Other Bill Type Error");
                    System.out.println("No Adomic bill type for = " + b);
                    return null;
            }
        }
//        if (preBill.getBillType() != null) {
//            BillType btype = preBill.getBillType();
//            switch (btype) {
//                case OpdPreBill:
//                    setPreBillForOpd(preBill);
//
//                case PharmacyPre:
//                    setPreBillForPharmecy(preBill);
//                    return "/pharmacy/pharmacy_bill_pre_settle?faces-redirect=true";
//
////                case OpdBathcBillPre:
////                    opdPreBatchBillSettleController.setPreBill(preBill);
////                    return "/opd/opd_bill_pre_settle?faces-redirect=true";
//                default:
//                    throw new AssertionError();
//            }
//
//        }
        JsfUtil.addErrorMessage("No bill error");
        return null;
    }

    public void setPreBillForOpd(Bill preBill) {
        makeNull();
        opdPreSettleController.setPreBill(preBill);
        System.out.println("preBill = " + preBill.getBillItems().size());
        opdPreSettleController.toSettle(preBill);
        //System.err.println("Setting Bill " + preBill);
        opdPreSettleController.setBillPreview(false);

    }

    public void setPreBillForPharmecy(Bill preBill) {
        makeNull();
        pharmacyPreSettleController.setPreBill(preBill);
        //System.err.println("Setting Bill " + preBill);
        pharmacyPreSettleController.setBillPreview(false);

    }

    public void createGrnWithDealerTable() {
        Map m = new HashMap();
        String sql = "select b from Bill b where b.retired=false and "
                + " b.billType = :billType and b.institution = :del "
                + "and b.createdAt between :fromDate and :toDate ";

        m.put("billType", BillType.PharmacyGrnBill);
        m.put("del", getDealer());
        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());

        grnBills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public String navigateToPatientLabReports() {
        fillPatientLabReports(patient);
        return "/lab/patient_lab_reports";
    }

    public String navigateToPatientAcceptPayment() {
        fillPatientPreBills(null, patient, null, true);
        return "/opd/patient_accept_payment";
    }

    public String menuBarSearch() {
        JsfUtil.addSuccessMessage("Sarched From Menubar" + "\n" + menuBarSearchText);
        return "/index";
    }

    public String navigateToSmsList() {
        return "/analytics/sms_list?faces-redirect=true";
    }

    public String navigateToFailedSmsList() {
        return "/analytics/sms_faild?faces-redirect=true";
    }

    public String navigateToSendSms() {
        return "/analytics/sms_send?faces-redirect=true";
    }

    public String navigateToListOtherInstitutionBills() {
        bills = null;
        return "/analytics/other_institution_bills?faces-redirect=true";
    }

    public String navigateToAnalytics() {
        bills = null;
        return "/analytics/index?faces-redirect=true";
    }

    public String navigateToOpdBillList() {
        bills = null;
        return "/analytics/opd_bill_list?faces-redirect=true";
    }

    public String toSearchBills() {
        bills = null;
        return "/dataAdmin/search_bill?faces-redirect=true";
    }

    public String toListAllBills() {
        bills = null;
        return "/dataAdmin/list_bills?faces-redirect=true";
    }

    public String toListAllPayments() {
        bills = null;
        return "/dataAdmin/list_payments?faces-redirect=true";
    }

    public void listAllBills() {
        String sql;
        Map temMap = new HashMap();
        sql = "select b from Bill b where "
                + " b.createdAt between :fromDate and :toDate "
                + "order by b.createdAt desc ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public void retireAlllistedBills() {
        for (Bill b : bills) {
            b.setRetired(true);
            b.setRetiredAt(new Date());
            b.setRetirer(sessionController.getLoggedUser());
            getBillFacade().edit(b);
        }
    }

    public void listAllPayments() {
        String sql;
        Map temMap = new HashMap();
        sql = "select p from Payment"
                + " p where "
                + " p.createdAt between :fromDate and :toDate "
                + " order by p.createdAt desc ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        payments = paymentFacade.findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public String toViewBillSummery() {
        if (bill == null) {
            return "";
        }
        return "/bill_summery?faces-redirect=true";
    }

    public void fillBillSessions() {
        selectedBillSession = null;
        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        if (false) {
            BillSession bs = new BillSession();
            bs.getBill().getInsId();
            bs.getBill().getDeptId();
            bs.getBill().getReferenceNumber();
            bs.getReferenceBillSession().getBill().getInsId();
            bs.getReferenceBillSession().getBill().getDeptId();
            bs.getReferenceBillSession().getBill().getReferenceNumber();
            bs.getBill().getReferenceBill().getInsId();
            bs.getBill().getReferenceBill().getDeptId();
            bs.getBill().getReferenceBill().getReferenceNumber();
        }
        String sql = "Select bs From BillSession bs "
                + " where bs.retired=false"
                + " and bs.bill.billType in :bt"
                + " and type(bs.bill)=:class "
                + " and ("
                + "    (bs.bill.insId) like :txt "
                + " or (bs.bill.deptId) like :txt "
                + " or (bs.bill.referenceNumber) like :txt "
                + " or (bs.referenceBillSession.bill.insId) like :txt "
                + " or (bs.referenceBillSession.bill.deptId) like :txt "
                + " or (bs.referenceBillSession.bill.referenceNumber) like :txt "
                + " or (bs.bill.referenceBill.insId) like :txt "
                + " or (bs.bill.referenceBill.deptId) like :txt "
                + " or (bs.bill.referenceBill.referenceNumber) like :txt "
                + " )"
                + " order by bs.sessionDate, bs.serialNo ";
        HashMap hh = new HashMap();
        hh.put("bt", bts);
        hh.put("class", BilledBill.class);
        hh.put("txt", "%" + menuBarSearchText.toLowerCase().trim() + "%");
        billSessions = billSessionFacade.findByJpql(sql, hh, TemporalType.DATE);

    }

    public void makeListNull() {
        maxResult = 50;
        bills = null;
        aceptPaymentBills = null;
        selectedBills = null;
        billFees = null;
        billItems = null;
        patientInvestigations = null;
        searchKeyword = null;
        printPreview = false;
    }

    public String navigateToSearchOpdBillsOfLoggedDepartment() {
        maxResult = 50;
        bills = null;
        aceptPaymentBills = null;
        selectedBills = null;
        billFees = null;
        billItems = null;
        patientInvestigations = null;
        searchKeyword = null;
        return "/opd/search_opd_billd_of_logged_department?faces-redirect=true";
    }

    public void makeListNull2() {
        billFeesDone = null;
        searchKeyword = null;
        speciality = null;
        staff = null;
        item = null;
        makeListNull();
    }

    public void createPatientInvestigationsTableAllTest() {
        Date startTime = new Date();

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p";

        Map temMap = new HashMap();

        patientInvestigations = getPatientInvestigationFacade().findByJpql(sql, 10);
        checkRefundBillItems(patientInvestigations);

    }

    public String navigatToopdSearchProfessionalPaymentDue1() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigatToopdSearchProfessionalPaymentDue1()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/opd_search_professional_payment_due_1.xhtml?faces-redirect=true";
    }

    public String navigatToReportDoctorPaymentOpd() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigatToReportDoctorPaymentOpd()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_doctor_payment_opd.xhtml?faces-redirect=true";

    }

    public String navigatToReportDoctorPaymentOpdByBill() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("navigatToReportDoctorPaymentOpdByBill()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        return "/reportCashier/report_doctor_payment_opd_by_bill.xhtml?faces-redirect=true";

    }

    public void fillToMyDepartmentPatientInvestigations() {
        Date startTime = new Date();

        String jpql = "select pi "
                + " from PatientInvestigation pi "
                + " join pi.investigation i "
                + " join pi.billItem.bill b "
                + " join b.patient.person p "
                + " where "
                + " b.createdAt between :fromDate and :toDate  "
                + " and b.toDepartment=:dep ";

        Map temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("dep", getSessionController().getLoggedUser().getDepartment());

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            jpql += " and  ((p.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            jpql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            jpql += " and  ((p.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            jpql += " and  ((i.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (patientEncounter != null) {
            jpql += "and pi.encounter=:en";
            temMap.put("en", patientEncounter);
        }

        jpql += " order by pi.id desc  ";
//    

        patientInvestigations = getPatientInvestigationFacade().findByJpql(jpql, temMap, TemporalType.TIMESTAMP, 50);
        checkRefundBillItems(patientInvestigations);

    }

    public void fillToDepartmentPatientInvestigations() {
        Date startTime = new Date();

        String jpql = "select pi "
                + " from PatientInvestigation pi "
                + " join pi.investigation i "
                + " join pi.billItem.bill b "
                + " join b.patient.person p "
                + " where "
                + " b.createdAt between :fromDate and :toDate  "
                + " and b.toDepartment=:dep ";

        Map temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("dep", getReportKeyWord().getDepartment());

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            jpql += " and  ((p.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            jpql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            jpql += " and  ((p.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            jpql += " and  ((i.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (patientEncounter != null) {
            jpql += "and pi.encounter=:en";
            temMap.put("en", patientEncounter);
        }

        jpql += " order by pi.id desc  ";
//    

        patientInvestigations = getPatientInvestigationFacade().findByJpql(jpql, temMap, TemporalType.TIMESTAMP, 50);
        checkRefundBillItems(patientInvestigations);

    }

    public void createPreRefundTable() {

        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from RefundBill b where b.billType = :billType "
                + " and b.institution=:ins and "
                + " (b.billedBill is null  or type(b.billedBill)=:billedClass ) "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false and b.deptId is not null ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billedClass", PreBill.class);
        temMap.put("billType", BillType.PharmacyPre);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpqlWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 50);

        Date startTime = new Date();

    }

    public void createPreRefundOpdTable() {

        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from RefundBill b where "
                + " b.createdAt between :fromDate and :toDate"
                + " and b.retired=false and b.deptId is not null "
                + " and b.billType = :billType"
                + " and b.institution = :ins";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
//        temMap.put("billedClass", PreBill.class);
//        temMap.put("billType", BillType.OpdBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billType", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpqlWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 50);

        Date startTime = new Date();

    }

    public void reportSettledOPDBills() {
        Date startTime = new Date();
        settledBills(billType.OpdBill);

    }

    public void reportSettledPharmacyBills() {
        Date startTime = new Date();
        settledBills(billType.PharmacyWholeSale);

    }

    public void settledBills(BillType bt) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where "
                + " b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.referenceBill.billType=:bt "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                //                + " and b.balance=0 "
                + "order by b.createdAt desc ";

//    
        temMap.put("billType", BillType.CashRecieveBill);
        temMap.put("bt", bt);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    List<billsWithbill> withbills;

    public List<billsWithbill> getWithbills() {
        return withbills;
    }

    public void setWithbills(List<billsWithbill> withbills) {
        this.withbills = withbills;
    }

    public void createCreditBillsWithOPDBill() {
        Date startTime = new Date();
        createCreditBillsWithBill(billType.OpdBill);

    }

    public void createCreditBillsWithPharmacyBill() {
        Date startTime = new Date();

        createCreditBillsWithBill(billType.PharmacyWholeSale);

    }

    public void createCreditBillsWithBill(BillType refBillType) {
        bills = fetchBills(BillType.CashRecieveBill, refBillType);
        ////System.out.println("bills = " + bills);
        withbills = new ArrayList<>();

        for (Bill b : bills) {
            billsWithbill bWithbill = new billsWithbill();
            bWithbill.setB(b);
            bWithbill.setCaBills(fetchCreditBills(BillType.CashRecieveBill, b));
            ////System.out.println("bWithbill.getCaBills() = " + bWithbill.getCaBills());
            withbills.add(bWithbill);
        }
        ////System.out.println("withbills = " + withbills);

    }

    public List<Bill> fetchBills(BillType bt, BillType rbt) {

        String sql;
        Map temMap = new HashMap();

        sql = "select DISTINCT(b.referenceBill) from Bill b where "
                + " b.billType =:billType "
                + " and b.referenceBill.billType =:billTypeRef "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        temMap.put("billType", bt);
        temMap.put("billTypeRef", rbt);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public List<Bill> fetchCreditBills(BillType bt, Bill b) {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where "
                + " b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.referenceBill=:bid ";

        temMap.put("billType", bt);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bid", b);
        temMap.put("ins", getSessionController().getInstitution());

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
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

    @Deprecated
    private void checkLabReportsApproved(List<Bill> bills) {
        if (!bills.isEmpty()) {
            for (Bill b : bills) {
                String sql;
                Map m = new HashMap();
                sql = " select pr from PatientReport pr where "
                        + " pr.retired=false "
                        + " and pr.patientInvestigation.billItem.bill=:b "
                        + " and pr.approved=true ";

                m.put("b", b);
                List<PatientReport> list = getPatientReportFacade().findByJpql(sql, m);
                if (!list.isEmpty()) {
                    b.setApprovedAnyTest(true);
                }
            }
        }
    }

    private void checkLabReportsApprovedBillItem(List<BillItem> billItems) {
        for (BillItem bi : billItems) {
            String sql;
            Map m = new HashMap();
            sql = " select pr from PatientReport pr where "
                    + " pr.retired=false "
                    + " and pr.patientInvestigation.billItem=:bi "
                    + " and pr.approved=true ";

            m.put("bi", bi);
            List<PatientReport> list = getPatientReportFacade().findByJpql(sql, m);
            if (!list.isEmpty()) {
                bi.getBill().setApprovedAnyTest(true);
            }
        }
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

    public BillController getBillController() {
        return billController;
    }

    public void setBillController(BillController billController) {
        this.billController = billController;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
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

    public class billsWithbill {

        Bill b;
        List<Bill> caBills;

        public Bill getB() {
            return b;
        }

        public void setB(Bill b) {
            this.b = b;
        }

        public List<Bill> getCaBills() {
            if (caBills == null) {
                caBills = new ArrayList<>();
            }
            return caBills;
        }

        public void setCaBills(List<Bill> caBills) {
            this.caBills = caBills;
        }

    }

    public void createReturnSaleBills() {
        Date startTime = new Date();

        createReturnSaleBills(BillType.PharmacyPre, true);

    }

    public void createReturnSaleAllBills() {
        Date startTime = new Date();

        createReturnSaleBills(BillType.PharmacyPre, false);

    }

    public void createReturnWholeSaleBills() {
        createReturnSaleBills(BillType.PharmacyWholesalePre, true);
    }

    public void createReturnWholeSaleAllBills() {
        createReturnSaleBills(BillType.PharmacyWholesalePre, false);
    }

    public void createReturnSaleBills(BillType billType, boolean maxNum) {

        Map m = new HashMap();
        m.put("bt", billType);
        m.put("billedClass", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from RefundBill b where  b.retired=false "
                + " and b.institution=:ins and "
                + " (b.billedBill is null  or type(b.billedBill)=:billedClass ) "
                + " and b.createdAt between :fd and :td"
                + " and b.billType=:bt ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  ((b.billedBill.deptId) like :rNo )";
            m.put("rNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
        if (maxNum == true) {
            bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 25);
        } else {
            bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        }

    }

    public void createTableByKeywordToPayBills() {
        Date startTime = new Date();
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where "
                + " b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.balance>0 ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (((b.deptId) like :billNo )or((b.insId) like :billNo ))";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.OpdBathcBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createTablePharmacyCreditToPayBills() {
        Date startTime = new Date();

        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where "
                + " b.billType in :billTypes "
                + " and b.institution=:ins "
                + " and b.department=:dep "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and (b.netTotal-b.paidAmount)>0 "
                + " and b.paymentMethod=:pm ";

        if (getSearchKeyword().getInstitution() != null && !getSearchKeyword().getInstitution().trim().equals("")) {
            sql += " and  ((b.toInstitution.name) like :comp )";
            temMap.put("comp", "%" + getSearchKeyword().getInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.toStaff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (((b.deptId) like :billNo )or((b.insId) like :billNo ))";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billTypes", Arrays.asList(new BillType[]{BillType.PharmacyWholeSale, BillType.PharmacySale}));
        temMap.put("pm", PaymentMethod.Credit);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("dep", getSessionController().getDepartment());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createReturnBhtBills() {
        Date startTime = new Date();

        createReturnBhtBills(BillType.PharmacyBhtPre);

    }

    public void createReturnBhtBillsStore() {
        createReturnBhtBills(BillType.StoreBhtPre);
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    private void createReturnBhtBills(BillType billType) {

        Map m = new HashMap();
        m.put("bt", billType);
        m.put("billedClass", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from RefundBill b where  b.retired=false "
                + " and b.institution=:ins and "
                + " (b.billedBill is null  or type(b.billedBill)=:billedClass ) "
                + " and b.createdAt between :fd and :td"
                + " and b.billType=:bt ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  ((b.billedBill.deptId) like :rNo )";
            m.put("rNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            m.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createVariantReportSearch() {
        Date startTime = new Date();

        String sql = "";
        HashMap tmp = new HashMap();

        sql = "Select b From PreBill b where b.cancelledBill is null  "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false and b.billType= :bTp ";

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  ((b.department.name) like :dep )";
            tmp.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCategory() != null && !getSearchKeyword().getCategory().trim().equals("")) {
            sql += " and  ((b.category.name) like :cat )";
            tmp.put("cat", "%" + getSearchKeyword().getCategory().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("bTp", BillType.PharmacyMajorAdjustment);
        bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP);

    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<Bill> getPrescreptionBills() {
        return prescreptionBills;
    }

    public void setPrescreptionBills(List<Bill> prescreptionBills) {
        this.prescreptionBills = prescreptionBills;
    }

    public void createPharmacyPrescriptionBillTable() {
        Date startTime = new Date();

        Map m = new HashMap();
        m.put("bt", BillType.PharmacyPre);
        m.put("rBt", BillType.PharmacySale);
        m.put("class", PreBill.class);
        m.put("rClass", BilledBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;
        sql = "Select b from Bill b "
                + " where b.retired=false and b.createdAt between :fd and :td and b.billType=:bt "
                + " and b.referredBy is not null "
                + " and b.institution=:ins "
                + " and b.referenceBill.billType=:rBt "
                + " and type(b)=:class "
                + " and type(b.referenceBill)=:rClass ";

        if (department != null) {
            sql += " and b.department=:dept ";
            m.put("dept", department);
        }

        sql += " order by b.createdAt ";

        prescreptionBills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void createPharmacyRetailBills() {
        Date startTime = new Date();

        createPharmacyRetailBills(BillType.PharmacyPre, true);

    }

    public void createPharmacyAddToStockBills() {
        Date startTime = new Date();
        createPharmacyAddToBills(BillType.PharmacyAddtoStock, true);
        //System.out.println("bills = " + bills);
    }

    public void createPharmacyAddToStockAllBills() {
        Date startTime = new Date();
        createPharmacyAddToBills(BillType.PharmacyAddtoStock, false);
        //System.out.println("Allbills = " + bills);
    }

    public void createPharmacyAddToBills(BillType billtype, boolean maxNum) {

        String sql;
        Map m = new HashMap();

        sql = "Select b from Bill b where "
                + " b.createdAt between :fd and :td "
                + " and b.billType=:bt"
                + " and b.institution=:ins "
                + " and b.department=:ldep";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  ((b.department.name) like :dep )";
            m.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        m.put("bt", billtype);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        m.put("ldep", getSessionController().getLoggedUser().getDepartment());
//    
        //System.out.println("sql = " + sql);

        if (maxNum == true) {
            bills = billFacade.findByJpql(sql, m, TemporalType.TIMESTAMP, 25);
        } else {
            bills = billFacade.findByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        netTotal = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
        }
        //System.out.println("Method_bills = " + bills);
    }

    public void createPharmacyWholesaleBills() {
        createPharmacyRetailBills(BillType.PharmacyWholesalePre, true);
    }

    public void createPharmacyWholeTableRe() {
        Date startTime = new Date();

        Map m = new HashMap();
        m.put("bt", BillType.PharmacyWholesalePre);
        //     m.put("class", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ret", false);
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from Bill b where "
                + " b.createdAt between :fd and :td "
                + " and b.billType=:bt "
                + " and b.retired=:ret "
                + " and b.institution=:ins";
        //+ " and type(b)=:class ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  ((b.department.name) like :dep )";
            m.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        //     //////System.out.println("sql = " + sql);
        bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyRetailAllBills() {
        Date startTime = new Date();

        createPharmacyRetailBills(BillType.PharmacyPre, false);

    }

    public void createPharmacyWholesaleAllBills() {
        createPharmacyRetailBills(BillType.PharmacyWholesalePre, false);
    }

    public void createPharmacyRetailBills(BillType billtype, boolean maxNum) {

        Map m = new HashMap();
        m.put("bt", billtype);
        //   m.put("class", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        m.put("ldep", getSessionController().getLoggedUser().getDepartment());
        String sql;

        sql = "Select b from PreBill b where "
                + " b.createdAt between :fd and :td "
                + " and b.billType=:bt"
                + " and b.billedBill is null "
                + " and b.institution=:ins "
                + " and b.department=:ldep";
        //  + " and type(b)=:class ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  ((b.department.name) like :dep )";
            m.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }
        if (getPaymentMethod() != null) {
            sql += " and b.paymentMethod=:pay ";
            m.put("pay", paymentMethod);
        }
        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :phone )";
            m.put("phone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        //     //////System.out.println("sql = " + sql);

        if (maxNum == true) {
            bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 25);
        } else {
            bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        netTotal = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
        }
    }

    public String searchMyPharmacyBills() {
        BillType billtype = BillType.PharmacyPre;
        String sql;
        if (false) {
            Bill b = new Bill();
            b.getPatient().getPerson();
            sessionController.getLoggedUser().getWebUserPerson();
        }
        sql = "Select b from PreBill b where "
                + " b.billType=:bt"
                + " and b.billedBill is null "
                + " and b.patient.person=:person";
        sql += " order by b.createdAt desc  ";
        Map m = new HashMap();
        m.put("bt", billtype);
        m.put("person", sessionController.getLoggedUser().getWebUserPerson());

        boolean maxNum = true;
        if (maxNum == true) {
            bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 25);
        } else {
            bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        }
        netTotal = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
        }

        return "/mobile/my_pharmacy_bills";
    }

    double netTotalValue;

    public void createPharmacyStaffBill() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("createPharmacyStaffBill()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Map m = new HashMap();
        m.put("bt", BillType.PharmacyPre);
        //   m.put("class", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from PreBill b where "
                + " b.createdAt between :fd and :td "
                + " and b.billType=:bt"
                + " and b.billedBill is null "
                + " and b.institution=:ins "
                + " and b.toStaff is not null "
                + " order by b.createdAt ";
//    
        //     //////System.out.println("sql = " + sql);
        bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        netTotalValue = 0.0;
        for (Bill b : bills) {
            netTotalValue += b.getNetTotal();
        }
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void createPharmacyTableRe() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billTypeAtomic = :billTypeAtomic "
                + " and b.institution=:ins "
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false "
                + " and b.deptId is not null "
                + " and b.cancelled=false";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  ((b.department.name) like :dep )";
            temMap.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :phone )";
            temMap.put("phone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billTypeAtomic", BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpqlWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 25);

    }

    public void listPharmacyIssue() {
        Date startTime = new Date();

        listPharmacyPreBills(BillType.PharmacyIssue);

    }

    public void listStoreIssue() {
        Date startTime = new Date();
        listPharmacyPreBills(BillType.StoreIssue);

    }

    public void listPharmacyCancelled() {
        listPharmacyCancelledBills(BillType.PharmacyIssue);
    }

    public void listPharmacyReturns() {
        Date startTime = new Date();

        listPharmacyStoreReturnedBills(BillType.PharmacyIssue);

    }

    public void listStoreReturns() {
        Date startTime = new Date();
        listPharmacyStoreReturnedBills(BillType.StoreIssue);

    }

    public void listPharmacyBilledBills(BillType bt) {
        listPharmacyBills(bt, BilledBill.class);
    }

    public void listPharmacyPreBills(BillType bt) {
        listPharmacyBills(bt, PreBill.class);
    }

    public void listPharmacyCancelledBills(BillType bt) {
        listPharmacyBills(bt, CancelledBill.class);
    }

    public void listPharmacyStoreReturnedBills(BillType bt) {
        listReturnBills(bt, RefundBill.class);
    }

    public ServiceSession getSelectedServiceSession() {
        return selectedServiceSession;
    }

    public void setSelectedServiceSession(ServiceSession selectedServiceSession) {
        this.selectedServiceSession = selectedServiceSession;
    }

    public Staff getCurrentStaff() {
        return currentStaff;
    }

    public void setCurrentStaff(Staff currentStaff) {
        this.currentStaff = currentStaff;
    }

    public void listPharmacyBills(BillType bt, Class bc) {

        Map m = new HashMap();
        m.put("bt", bt);
        m.put("class", bc);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from Bill b where "
                + " b.createdAt between :fd and :td "
                + " and b.billType=:bt "
                + " and b.institution=:ins "
                + " and type(b)=:class "
                + " and b.billedBill is null ";

        if (getSearchKeyword().getRequestNo() != null && !getSearchKeyword().getRequestNo().trim().equals("")) {
            sql += " and  ((b.invoiceNumber) like :requestNo )";
            m.put("requestNo", "%" + getSearchKeyword().getRequestNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  ((b.department.name) like :dep )";
            m.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        //     //////System.out.println("sql = " + sql);
        bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void listReturnBills(BillType bt, Class bc) {

        Map m = new HashMap();
        m.put("bt", bt);
        m.put("class", bc);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        m.put("dept", getSessionController().getDepartment());
        String sql;

        sql = "Select b from Bill b where "
                + " b.createdAt between :fd and :td "
                + " and b.billType=:bt "
                + " and b.institution=:ins "
                + " and b.department=:dept "
                + " and type(b)=:class ";

        if (getSearchKeyword().getRequestNo() != null && !getSearchKeyword().getRequestNo().trim().equals("")) {
            sql += " and  ((b.invoiceNumber) like :requestNo )";
            m.put("requestNo", "%" + getSearchKeyword().getRequestNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  ((b.department.name) like :dep )";
            m.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        //     //////System.out.println("sql = " + sql);
        bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyTableBht() {
        Date startTime = new Date();

        createTableBht(BillType.PharmacyBhtPre);

    }

    public void createStoreTableIssue() {
        createTableBht(BillType.StoreIssue);
    }

    public void createStoreTableBht() {
        Date startTime = new Date();
        createTableBht(BillType.StoreBhtPre);

    }

    public void createTableBht(BillType btp) {

        Map m = new HashMap();
        m.put("bt", btp);
        m.put("class", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        m.put("dep", getSessionController().getDepartment());
        String sql;

        sql = "Select b from Bill b "
                + " where b.retired=false "
                + " and b.billedBill is null "
                + " and b.createdAt between :fd and :td "
                + " and b.billType=:bt"
                + " and b.institution=:ins "
                + " and b.department=:dep "
                + " and type(b)=:class ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            m.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  ((b.department.name) like :dep )";
            m.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        //     //////System.out.println("sql = " + sql);
        bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createIssueTable() {
        Date startTime = new Date();

        String sql;
        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("dep", getSessionController().getDepartment());
        tmp.put("bTp", BillType.PharmacyTransferIssue);
        sql = "Select b From BilledBill b where b.retired=false and "
                + " b.toDepartment=:dep and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.toStaff.person.name) like :stf )";
            tmp.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  ((b.department.name) like :fDep )";
            tmp.put("fDep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setTmpRefBill(getRefBill(b));

        }

    }

    public void createIssueReport1() {
        String sql;
        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        //tmp.put("dep", getSessionController().getDepartment());
        tmp.put("bTp", BillType.PharmacyTransferIssue);
        sql = "Select b From BilledBill b "
                + " where b.retired=false "
                //+ " and b.toDepartment=:dep "
                + " and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.toStaff.person.name) like :stf )";
            tmp.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFrmDepartment() != null) {
            sql += " and b.department=:frmdep";
            tmp.put("frmdep", getSearchKeyword().getFrmDepartment());
        }

        if (getSearchKeyword().getTooDepartment() != null) {
            sql += " and b.toDepartment=:tdep";
            tmp.put("tdep", getSearchKeyword().getTooDepartment());
        }

        sql += " order by b.createdAt desc  ";

        List<Bill> list = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP);
        bills = new ArrayList<>();
        netTotalValue = 0.0;
        for (Bill b : list) {
//            ////System.out.println("b = ");

            Bill refBill = getActiveRefBill(b);
            if (refBill == null) {
                ////System.out.println("b = " + refBill);
                netTotalValue += b.getNetTotal();
                bills.add(b);
            }
        }

    }

    public void createIssuePharmacyReport() {
        Date startTime = new Date();
//        fetchPharmacyBills(BillType.PharmacyTransferIssue, BillType.PharmacyTransferReceive);
        fetchPharmacyBillsNew(BillType.PharmacyTransferIssue, BillType.PharmacyTransferReceive);

    }

    public void createIssueStoreReport() {
        fetchPharmacyBills(BillType.StoreTransferIssue, BillType.StoreTransferReceive);
    }

    public void createPoNotPharmacyApproveReport() {
        Date startTime = new Date();

        fetchPharmacyBills(BillType.PharmacyOrder, BillType.PharmacyAdjustment);

    }

    public void createPoNotStoreApproveReport() {
        fetchPharmacyBills(BillType.StoreOrder, BillType.StoreOrderApprove);
    }

    public void fetchPharmacyBills(BillType billType, BillType billType2) {
        String sql;
        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        //tmp.put("dep", getSessionController().getDepartment());
        tmp.put("bTp", billType);
        sql = "Select b From BilledBill b "
                + " where b.retired=false "
                //+ " and b.toDepartment=:dep "
                + " and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.toStaff.person.name) like :stf )";
            tmp.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFrmDepartment() != null) {
            sql += " and b.department=:frmdep";
            tmp.put("frmdep", getSearchKeyword().getFrmDepartment());
        }

        if (getSearchKeyword().getTooDepartment() != null) {
            sql += " and b.toDepartment=:tdep";
            tmp.put("tdep", getSearchKeyword().getTooDepartment());
        }

        sql += " order by b.createdAt desc  ";

        List<Bill> list = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP);
        bills = new ArrayList<>();
        netTotalValue = 0.0;
        for (Bill b : list) {
//            ////System.out.println("b = ");

            Bill refBill = getActiveRefBillnotApprove(b, billType2);
            if (refBill == null) {
                ////System.out.println("b = " + refBill);
                netTotalValue += b.getNetTotal();
                bills.add(b);
            }
        }

    }

    public void fetchPharmacyBillsNew(BillType billType, BillType billType2) {
        String sql;
        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        //tmp.put("dep", getSessionController().getDepartment());
        tmp.put("bTp", billType);
        sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.toStaff.person.name) like :stf )";
            tmp.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFrmDepartment() != null) {
            sql += " and b.department=:frmdep";
            tmp.put("frmdep", getSearchKeyword().getFrmDepartment());
        }

        if (getSearchKeyword().getTooDepartment() != null) {
            sql += " and b.toDepartment=:tdep";
            tmp.put("tdep", getSearchKeyword().getTooDepartment());
        }

        sql += " order by b.createdAt desc  ";

        List<Bill> list = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP);
        bills = new ArrayList<>();
        netTotalValue = 0.0;
        for (Bill b : list) {
//            ////System.out.println("b = ");

            Bill refBill = getActiveRefBillnotApprove(b, billType2);
            if (refBill == null) {
                ////System.out.println("b = " + refBill);
                netTotalValue += b.getNetTotal();
                bills.add(b);
            }
        }

    }

    public void createIssueTableStore() {
        Date startTime = new Date();

        String sql;
        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("dep", getSessionController().getDepartment());
        tmp.put("bTp", BillType.StoreTransferIssue);
        sql = "Select b From BilledBill b where b.retired=false and "
                + " b.toDepartment=:dep and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.toStaff.person.name) like :stf )";
            tmp.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  ((b.department.name) like :fDep )";
            tmp.put("fDep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setTmpRefBill(getRefBill(b));

        }

    }

    private Bill getRefBill(Bill b) {
        String sql = "Select b From Bill b where b.retired=false "
                + " and b.cancelled=false and b.billType=:btp and "
                + " b.referenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyTransferReceive);
        return getBillFacade().findFirstByJpql(sql, hm);
    }

    private Bill getActiveRefBill(Bill b) {
        String sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false"
                + "  and b.billType=:btp"
                + " and b.backwardReferenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyTransferReceive);
        return getBillFacade().findFirstByJpql(sql, hm);
    }

    private Bill getActiveRefBillnotApprove(Bill b, BillType billType) {
        String sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false"
                + "  and b.billType=:btp"
                + " and b.backwardReferenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", billType);
        return getBillFacade().findFirstByJpql(sql, hm);
    }

    public void makeNull() {
        fromDate = null;
        toDate = null;
        department = null;
        departments = null;
        institution = null;
        paymentMethod = null;
        searchKeyword = null;
        bills = null;
        billSummaryRows = null;
        netTotal = 0.0;
        discount = 0.0;
        grossTotal = 0.0;

        cashTotal = 0.0;
        cardTotal = 0.0;
        chequeTotal = 0.0;
        slipTotal = 0.0;
        totalOfOtherPayments = 0.0;
        billCount = 0.0;
        totalPaying = 0.0;
    }

    public void createTableByBillType() {
        Date startTime = new Date();

        if (billType == null) {
            JsfUtil.addErrorMessage("Please Select Bill Type");
            return;

        }

        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where b.retired=false and "
                + " (type(b)=:class1 or type(b)=:class2) "
                + " and b.department=:dep and b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRequestNo() != null && !getSearchKeyword().getRequestNo().trim().equals("")) {
            sql += " and  ((b.insId) like :requestNo )";
            temMap.put("requestNo", "%" + getSearchKeyword().getRequestNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  ((b.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromDepartment() != null && !getSearchKeyword().getFromDepartment().trim().equals("")) {
            sql += " and  ((b.fromDepartment.name) like :frmDept )";
            temMap.put("frmDept", "%" + getSearchKeyword().getFromDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  ((b.toInstitution.name) like :toIns )";
            temMap.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToDepartment() != null && !getSearchKeyword().getToDepartment().trim().equals("")) {
            sql += " and  ((b.toDepartment.name) like :toDept )";
            temMap.put("toDept", "%" + getSearchKeyword().getToDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  ((b.referenceBill.deptId) like :refId )";
            temMap.put("refId", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  ((b.invoiceNumber) like :inv )";
            temMap.put("inv", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and b.id in (select bItem.bill.id  "
                    + " from BillItem bItem where bItem.retired=false and  "
                    + " ((bItem.item.name) like :itm ))";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and b.id in (select bItem.bill.id  "
                    + " from BillItem bItem where bItem.retired=false and  "
                    + " ((bItem.item.code) like :cde ))";
            temMap.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("class1", BilledBill.class);
        temMap.put("class2", PreBill.class);
        temMap.put("billType", billType);
        temMap.put("dep", getSessionController().getDepartment());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //temMap.put("dep", getSessionController().getDepartment());
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, maxResult);
        //     //System.err.println("SIZE : " + lst.size());

    }

    public void createGRNRegistory() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) context.getExternalContext().getContext();

        String url = request.getRequestURL().toString();

        String ipAddress = request.getRemoteAddr();

        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventStatus("Started");
        long duration;
        Date startTime = new Date();
        auditEvent.setEventDataTime(startTime);
        if (sessionController != null && sessionController.getDepartment() != null) {
            auditEvent.setDepartmentId(sessionController.getDepartment().getId());
        }

        if (sessionController != null && sessionController.getInstitution() != null) {
            auditEvent.setInstitutionId(sessionController.getInstitution().getId());
        }
        if (sessionController != null && sessionController.getLoggedUser() != null) {
            auditEvent.setWebUserId(sessionController.getLoggedUser().getId());
        }
        auditEvent.setUrl(url);
        auditEvent.setIpAddress(ipAddress);
        auditEvent.setEventTrigger("createGRNRegistory()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        if (getReportKeyWord().getDepartment() == null) {
            JsfUtil.addErrorMessage("Select Departmrnt.");
            return;
        }
        String sql;
        Map m = new HashMap();

        sql = "select b from Bill b where b.retired=false and "
                + " (type(b)=:class1 or type(b)=:class2) "
                + " and b.department=:dep and b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getReportKeyWord().getInstitution() != null) {
            sql += " and  ((b.fromInstitution.name) like :frmIns )";
            m.put("frmIns", getReportKeyWord().getInstitution());
        }

        if (getReportKeyWord().getItem() != null) {
            sql += " and b.id in (select bItem.bill.id  "
                    + " from BillItem bItem where bItem.retired=false "
                    + " and bItem.item.name=:itm ))";
            m.put("itm", getReportKeyWord().getItem());
        }

        sql += " order by b.createdAt desc  ";

        m.put("class1", BilledBill.class);
        m.put("class2", PreBill.class);
        m.put("billType", BillType.PharmacyGrnBill);
        m.put("dep", getReportKeyWord().getDepartment());
        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        //temMap.put("dep", getSessionController().getDepartment());
        bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        //     //System.err.println("SIZE : " + lst.size());
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void createTableByBillTypeAllDepartment() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where b.retired=false and "
                + " (type(b)=:class1 or type(b)=:class2) "
                + " and  b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRequestNo() != null && !getSearchKeyword().getRequestNo().trim().equals("")) {
            sql += " and ((b.insId) like :requestNo)";
            temMap.put("requestNo", "%" + getSearchKeyword().getRequestNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  ((b.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  ((b.toInstitution.name) like :toIns )";
            temMap.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  ((b.referenceBill.deptId) like :refId )";
            temMap.put("refId", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  ((b.invoiceNumber) like :inv )";
            temMap.put("inv", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and b.id in (select bItem.bill.id  "
                    + " from BillItem bItem where bItem.retired=false and  "
                    + " ((bItem.item.name) like :itm ))";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and b.id in (select bItem.bill.id  "
                    + " from BillItem bItem where bItem.retired=false and  "
                    + " ((bItem.item.code) like :cde ))";
            temMap.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("class1", BilledBill.class);
        temMap.put("class2", PreBill.class);
        temMap.put("billType", billType);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //temMap.put("dep", getSessionController().getDepartment());
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, maxResult);
        //     //System.err.println("SIZE : " + lst.size());

    }

    public void createRequestTable() {
        Date startTime = new Date();
        BillClassType[] billClassTypes = {BillClassType.CancelledBill, BillClassType.RefundBill};
        List<BillClassType> bct = Arrays.asList(billClassTypes);

        String sql;

        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("toDep", getSessionController().getDepartment());
        tmp.put("bct", bct);
        tmp.put("bTp", BillType.PharmacyTransferRequest);

        sql = "Select b From Bill b where "
                + " b.retired=false and  b.toDepartment=:toDep"
                + " and b.billClassType not in :bct"
                + " and b.billType= :bTp and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  ((b.department.name) like :dep )";
            tmp.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getIssudBills(b));
        }

    }

    public void createInwardBHTRequestTable() {
        Date startTime = new Date();
        BillClassType[] billClassTypes = {BillClassType.CancelledBill, BillClassType.RefundBill};
        List<BillClassType> bct = Arrays.asList(billClassTypes);

        String sql;

        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("dep", getSessionController().getDepartment());
        tmp.put("bct", bct);
        tmp.put("bTp", BillType.InwardPharmacyRequest);

        sql = "Select b From Bill b where "
                + " b.retired=false and  b.department=:dep "
                + " and b.billClassType not in :bct"
                + " and b.billType= :bTp and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and (((b.insId) like :billNo ) or ((b.deptId) like :billNo )) ";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            tmp.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getBHTIssudBills(b));
        }

    }

    public void createInwardBHTForIssueTableAll() {
        createInwardBHTForIssueTable(null);
    }

    public void createInwardBHTForNotIssueTable() {
        createInwardBHTForIssueTable(true);
    }

    public String navigateToIssueForBhtRequests() {
        bills = createInwardPharmacyRequests();
        return "/ward/issue_for_bht_request_list";
    }

    public List<Bill> createInwardPharmacyRequests() {
        String sql;
        HashMap tmp = new HashMap();
        tmp.put("admission", getPatientEncounter());
        tmp.put("bTp", BillType.InwardPharmacyRequest);
        sql = "Select b "
                + " From Bill b "
                + " where b.retired=false "
                + " and  b.toDepartment=:toDep"
                + " and b.billType=:bTp "
                + " and b.patientEncounter=:admission ";
        sql += " order by b.createdAt desc  ";
        return getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 100);
    }

    public void createInwardBHTForIssueOnlyTable() {
        createInwardBHTForIssueTable(false);
    }

    public void createInwardBHTForIssueTable(Boolean bool) {
        Date startTime = new Date();
        BillClassType[] billClassTypes = {BillClassType.CancelledBill, BillClassType.RefundBill};
        List<BillClassType> bct = Arrays.asList(billClassTypes);

        String sql;

        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("toDep", getSessionController().getDepartment());
        tmp.put("bTp", BillType.InwardPharmacyRequest);
        tmp.put("bct", bct);

        sql = "Select b From Bill b where "
                + " b.retired=false and  b.toDepartment=:toDep"
                + " and b.billClassType not in :bct"
                + " and b.billType= :bTp and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and (((b.insId) like :billNo ) or ((b.deptId) like :billNo )) ";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            tmp.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 100);

        for (Bill b : bills) {
            b.setListOfBill(getBHTIssudBills(b));
        }

        if (bool != null) {
            List<Bill> bs = new ArrayList<>();
            for (Bill b : bills) {
                if (pharmacySaleBhtController.checkBillComponent(b)) {
                    bs.add(b);
                }
            }
            if (bool) {
                bills = bs;
            } else {
                bills.removeAll(bs);
            }
        }

    }

    public long createInwardBHTForIssueBillCount() {
        String sql;

        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("toDep", getSessionController().getDepartment());
        tmp.put("bTp", BillType.InwardPharmacyRequest);

        sql = "Select COUNT(b) From Bill b "
                + " where b.retired=false "
                + " and b.toDepartment=:toDep "
                + " and b.cancelled=false "
                + " and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        long count = 0l;
        count = getBillFacade().countByJpql(sql, tmp, TemporalType.TIMESTAMP);

        return count;

    }

    public void createRequestTableStore() {
        Date startTime = new Date();

        String sql;

        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("toDep", getSessionController().getDepartment());
        tmp.put("bTp", BillType.StoreTransferRequest);

        sql = "Select b From Bill b where "
                + " b.retired=false and  b.toDepartment=:toDep"
                + " and b.billType= :bTp and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  ((b.department.name) like :dep )";
            tmp.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP);

        for (Bill b : bills) {
            b.setListOfBill(getIssudBills(b));
        }

    }

    public void createListToCashRecieve() {
        String sql;

        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("toWeb", getSessionController().getLoggedUser());
        tmp.put("bTp", BillType.CashOut);

        sql = "Select b From Bill b where "
                + " b.retired=false "
                + " and  b.toWebUser=:toWeb"
                + " and b.billType= :bTp"
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :net )";
            tmp.put("net", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  ((b.fromWebUser.webUserPerson.name) like :dep )";
            tmp.put("dep", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyBillItemTable() {
        Date startTime = new Date();

        createPharmacyBillItemTable(BillType.PharmacyPre, BillType.PharmacySale);

    }

    public void createPharmacyWholeBillItemTable() {
        createPharmacyBillItemTable(BillType.PharmacyWholesalePre, BillType.PharmacyWholeSale);
    }

    public void createPharmacyBillItemTable(BillType billType, BillType refBillType) {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", billType);
        m.put("rBType", refBillType);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);
        m.put("rClass", BilledBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class and type(bi.bill.referenceBill)=:rClass"
                + " and bi.bill.institution=:ins"
                + " and bi.bill.billType=:bType and "
                + " bi.bill.referenceBill.billType=:rBType "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((bi.bill.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((bi.netValue) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  ((bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyIssueBillItemTable() {
        Date startTime = new Date();
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.PharmacyIssue);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class "
                + " and bi.bill.institution=:ins"
                + " and bi.bill.billType=:bType "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((bi.bill.toDepartment.name) like :deptName )";
            m.put("deptName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((bi.netValue) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  ((bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void createStoreIssueBillItemTable() {
        Date startTime = new Date();
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.StoreIssue);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class "
                + " and bi.bill.institution=:ins"
                + " and bi.bill.billType=:bType "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((bi.bill.toDepartment.name) like :deptName )";
            m.put("deptName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((bi.netValue) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  ((bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createErronousStoreIssueBillItemTable() {

        Date startTime = new Date();
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.StoreIssue);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class "
                + " and bi.bill.institution=:ins "
                + " and bi.bill.billType=:bType "
                + " and bi.item.retired=true "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((bi.bill.toDepartment.name) like :deptName )";
            m.put("deptName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((bi.netValue) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  ((bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyAdjustmentBillItemTable() {
        Date startTime = new Date();
        String sql;
        Map<String, Object> m = new HashMap<>();

        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);

        // Set bill types individually
        m.put("bType1", BillType.PharmacyAdjustment);
        m.put("bType2", BillType.PharmacyAdjustmentDepartmentStock);
        m.put("bType3", BillType.PharmacyAdjustmentDepartmentSingleStock);
        m.put("bType4", BillType.PharmacyAdjustmentStaffStock);
        m.put("bType5", BillType.PharmacyAdjustmentSaleRate);
        m.put("bType6", BillType.PharmacyAdjustmentWholeSaleRate);
        m.put("bType7", BillType.PharmacyAdjustmentPurchaseRate);
        m.put("bType8", BillType.PharmacyAdjustmentExpiryDate);

        sql = "select bi from BillItem bi"
                + " where type(bi.bill) = :class "
                + " and bi.bill.institution = :ins"
                + " and (bi.bill.billType = :bType1"
                + " or bi.bill.billType = :bType2"
                + " or bi.bill.billType = :bType3"
                + " or bi.bill.billType = :bType4"
                + " or bi.bill.billType = :bType5"
                + " or bi.bill.billType = :bType6"
                + " or bi.bill.billType = :bType7"
                + " or bi.bill.billType = :bType8)"
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().isEmpty()) {
            sql += " and (bi.bill.deptId) like :billNo ";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().isEmpty()) {
            sql += " and (bi.item.name) like :itm ";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().isEmpty()) {
            sql += " and (bi.item.code) like :cde ";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc";

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyAdjustmentBillItemTableForStockTaking() {
        fetchAdjustmentBillItemTableForStockTaking(BillType.PharmacyAdjustment);
    }

    public void createStoreAdjustmentBillItemTableForStockTaking() {
        fetchAdjustmentBillItemTableForStockTaking(BillType.StoreAdjustment);
    }

    private void fetchAdjustmentBillItemTableForStockTaking(BillType billType) {
        pharmacyAdjustmentRows = new ArrayList<>();

        if (getReportKeyWord().getDepartment() == null) {
            JsfUtil.addErrorMessage("Please Select Department");
            return;
        }
        String sql;
        Map m = new HashMap();

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class "
                + " and bi.bill.institution=:ins "
                + " and bi.bill.department=:dep "
                + " and bi.bill.billType=:bType  "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getReportKeyWord().getCategory() != null) {
            sql += " and bi.item.category=:cat ";
            m.put("cat", getReportKeyWord().getCategory());
        }

        sql += " order by bi.item.name, bi.pharmaceuticalBillItem.stock.itemBatch.id ";

        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", billType);
        m.put("ins", getSessionController().getInstitution());
        m.put("dep", getReportKeyWord().getDepartment());
        m.put("class", PreBill.class);

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        dueTotal = 0.0;
        doneTotal = 0.0;
        netTotal = 0.0;

        if (getReportKeyWord().isAdditionalDetails()) {
            createAdjustmentRow(null, null, billItems);
        } else {
            m = new HashMap();
            sql = "select s from Stock s "
                    + " where s.department=:d "
                    + " and s.stock>=0 "
                    + " and s.itemBatch.item.departmentType!=:depty ";

            if (getReportKeyWord().getCategory() != null) {
                sql += " and s.itemBatch.item.category=:cat ";
                m.put("cat", getReportKeyWord().getCategory());
            }

            sql += " order by s.itemBatch.item.name,s.itemBatch.id ";
            m.put("d", getReportKeyWord().getDepartment());
            m.put("depty", DepartmentType.Inventry);
            List<Stock> stocks = getStockFacade().findByJpql(sql, m);
            for (Stock s : stocks) {
                boolean flag = true;
                int i = 1;
                for (BillItem bi : billItems) {
                    if (s.equals(bi.getPharmaceuticalBillItem().getStock())) {
                        createAdjustmentRow(null, bi, null);
                        flag = false;
                        i++;
                        break;
                    }
                    i++;
                }
                if (flag) {
                    if (s.getStock() > 0) {
                        createAdjustmentRow(s, null, null);
                    } else {
                    }
                }
            }

        }

    }

    public void createAdjustmentRow(Stock s, BillItem bi, List<BillItem> billItems) {
        PharmacyAdjustmentRow row;
        if (s != null) {
            row = new PharmacyAdjustmentRow(s.getItemBatch().getItem(),
                    s.getItemBatch().getPurcahseRate(),
                    s.getItemBatch().getRetailsaleRate(),
                    s.getStock(),
                    s.getStock(),
                    0,
                    s.getItemBatch().getBatchNo(),
                    s.getItemBatch().getDateOfExpire());
            dueTotal += row.getBefoerVal();
            doneTotal += row.getAfterVal();
            netTotal += row.getAdjusetedVal();
            pharmacyAdjustmentRows.add(row);
        }
        if (bi != null) {
            row = new PharmacyAdjustmentRow(bi.getItem(),
                    bi.getPharmaceuticalBillItem().getStock().getItemBatch().getPurcahseRate(),
                    bi.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate(),
                    bi.getPharmaceuticalBillItem().getStockHistory().getStockQty(),
                    bi.getQty(),
                    bi.getQty() - bi.getPharmaceuticalBillItem().getStockHistory().getStockQty(),
                    bi.getPharmaceuticalBillItem().getStock().getItemBatch().getBatchNo(),
                    bi.getPharmaceuticalBillItem().getStock().getItemBatch().getDateOfExpire());
            dueTotal += row.getBefoerVal();
            doneTotal += row.getAfterVal();
            netTotal += row.getAdjusetedVal();
            pharmacyAdjustmentRows.add(row);
        }
        if (billItems != null) {
            for (BillItem bii : billItems) {
                try {
                    row = new PharmacyAdjustmentRow(bii.getItem(),
                            bii.getPharmaceuticalBillItem().getStock().getItemBatch().getPurcahseRate(),
                            bii.getPharmaceuticalBillItem().getStock().getItemBatch().getRetailsaleRate(),
                            bii.getPharmaceuticalBillItem().getStockHistory().getStockQty(),
                            bii.getQty(),
                            bii.getQty() - bii.getPharmaceuticalBillItem().getStockHistory().getStockQty(),
                            bii.getPharmaceuticalBillItem().getStock().getItemBatch().getBatchNo(),
                            bii.getPharmaceuticalBillItem().getStock().getItemBatch().getDateOfExpire());
                    dueTotal += row.getBefoerVal();
                    doneTotal += row.getAfterVal();
                    netTotal += row.getAdjusetedVal();
                    pharmacyAdjustmentRows.add(row);
                } catch (Exception e) {
                }
            }
        }
    }

    public void createStoreAdjustmentBillItemTable() {
        Date startTime = new Date();
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.StoreAdjustment);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class "
                + " and bi.bill.institution=:ins"
                + " and bi.bill.billType=:bType  "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  ((bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createDrawerAdjustmentTable() {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.DrawerAdjustment);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", BilledBill.class);

        sql = "select bi from Bill bi"
                + " where  type(bi)=:class "
                + " and bi.institution=:ins"
                + " and bi.billType=:bType  "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((bi.insId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyBillItemTableIssue() {
        createBillItemTableBht(BillType.StoreIssue);
    }

    public void createPharmacyBillItemTableBht() {
        Date startTime = new Date();

        createBillItemTableBht(BillType.PharmacyBhtPre);

    }

    public void createStoreBillItemTableBht() {
        createBillItemTableBht(BillType.StoreBhtPre);
    }

    public List<BillItem> getBillItem() {
        return billItem;
    }

    public void setBillItem(List<BillItem> billItem) {
        this.billItem = billItem;
    }

    public void createBillItemTableBht(BillType btp) {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", btp);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class "
                + " and bi.bill.institution=:ins"
                + " and bi.bill.billType=:bType and "
                + " bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((bi.bill.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((bi.netValue) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  ((bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((bi.bill.patientEncounter.bhtNo) like :bht )";
            m.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPoRequestedAndApprovedPharmacy() {
        Date startTime = new Date();

        createPoRequestedAndApproved(InstitutionType.Dealer, BillType.PharmacyOrder);

    }

    public void createPoRequestedAndApprovedStore() {
        Date startTime = new Date();
        createPoRequestedAndApproved(InstitutionType.StoreDealor, BillType.StoreOrder);

    }

    public void createPoRequestedAndApproved(InstitutionType institutionType, BillType bt) {
        bills = null;
        String sql = "";
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where "
                + " b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.billType= :bTp  ";

        sql += createKeySql(tmp);

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("insTp", institutionType);
        tmp.put("bTp", bt);
        bills = getBillFacade().findByJpqlWithoutCache(sql, tmp, TemporalType.TIMESTAMP, maxResult);

    }

    public void createApprovedPharmacy() {
        Date startTime = new Date();

        createApproved(InstitutionType.Dealer, BillType.PharmacyOrder);

    }

    public void createApprovedStore() {
        Date startTime = new Date();
        createApproved(InstitutionType.StoreDealor, BillType.StoreOrder);

    }

    public void createApproved(InstitutionType institutionType, BillType bt) {
        bills = null;
        String sql = "";
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where"
                + " b.referenceBill.creater is not null "
                + " and b.referenceBill.cancelled=false "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.billType= :bTp  ";

        sql += createKeySql(tmp);

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("insTp", institutionType);
        tmp.put("bTp", bt);
        bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, maxResult);

    }

    public void createNotApprovedPharmacy() {
        Date startTime = new Date();
        createNotApproved(InstitutionType.Dealer, BillType.PharmacyOrder);

    }

    public void fillOnlySavedPharmacyPo() {
        bills = null;
        HashMap tmp = new HashMap();
        String sql;
        sql = "Select b From BilledBill b where "
                + " b.referenceBill is null "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.billType= :bTp  "
                + " and b.checkedBy is null";

        sql += createKeySqlSearchForPoCancel(tmp);

        sql += " order by b.createdAt desc  ";
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("insTp", InstitutionType.Dealer);
        tmp.put("bTp", BillType.PharmacyOrder);

        bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, maxResult);

    }

    public void fillOnlyFinalizedPharmacyPo() {
        bills = null;
        HashMap tmp = new HashMap();
        String sql;
        sql = "Select b From BilledBill b where "
                + " b.referenceBill is null "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.billType= :bTp  "
                + " and b.checkedBy is not null";
        sql += createKeySqlSearchForPoCancel(tmp);

        sql += " order by b.createdAt desc  ";
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("insTp", InstitutionType.Dealer);
        tmp.put("bTp", BillType.PharmacyOrder);

        bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, maxResult);

    }

    public void createNotApprovedStore() {
        Date startTime = new Date();

        createNotApproved(InstitutionType.StoreDealor, BillType.StoreOrder);

    }

    public void createNotApproved(InstitutionType institutionType, BillType bt) {
        bills = null;
        String sql = "";
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where"
                + "  b.referenceBill is null  "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false"
                + " and b.billType= :bTp ";

        sql += createKeySql(tmp);
        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("insTp", institutionType);
        tmp.put("bTp", bt);
        List<Bill> lst1 = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, maxResult);

        sql = "Select b From BilledBill b where "
                + " b.referenceBill.creater is null "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.billType= :bTp  ";

        sql += createKeySql(tmp);
        sql += " order by b.createdAt desc  ";

        List<Bill> lst2 = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, maxResult);

        sql = "Select b From BilledBill b where "
                + " b.referenceBill.creater is not null "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.referenceBill.cancelled=true "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false"
                + " and b.billType= :bTp ";

        sql += createKeySql(tmp);
        sql += " order by b.createdAt desc  ";

        List<Bill> lst3 = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, maxResult);

        lst1.addAll(lst2);
        lst1.addAll(lst3);

        bills = lst1;

    }

    private String createKeySqlSearchForPoCancel(HashMap tmp) {
        String sql = "";
        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  ((b.toInstitution.name) like :toIns )";
            tmp.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCreator() != null && !getSearchKeyword().getCreator().trim().equals("")) {
            sql += " and  ((b.creater.webUserPerson.name) like :crt )";
            tmp.put("crt", "%" + getSearchKeyword().getCreator().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  ((b.department.name) like :crt )";
            tmp.put("crt", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :reqTotal )";
            tmp.put("reqTotal", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.referenceBill.netTotal) like :appTotal )";
            tmp.put("appTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        return sql;

    }

    private String createKeySql(HashMap tmp) {
        String sql = "";
        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  ((b.toInstitution.name) like :toIns )";
            tmp.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCreator() != null && !getSearchKeyword().getCreator().trim().equals("")) {
            sql += " and  ((b.creater.webUserPerson.name) like :crt )";
            tmp.put("crt", "%" + getSearchKeyword().getCreator().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  ((b.department.name) like :crt )";
            tmp.put("crt", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  ((b.referenceBill.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :reqTotal )";
            tmp.put("reqTotal", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.referenceBill.netTotal) like :appTotal )";
            tmp.put("appTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        return sql;

    }

    private String keysForGrnReturnByBillItem(HashMap tmp) {
        String sql = "";
        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((bi.bill.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  ((bi.bill.invoiceNumber) like :invoice )";
            tmp.put("invoice", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  ((bi.bill.referenceBill.deptId) like :refNo )";
            tmp.put("refNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  ((bi.bill.fromInstitution.name) like :frmIns )";
            tmp.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((bi.bill.referenceBill.netTotal) like :total )";
            tmp.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((bi.bill.netTotal) like :netTotal )";
            tmp.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        return sql;
    }

    private String keysForGrnReturn(HashMap tmp) {
        String sql = "";
        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  ((b.invoiceNumber) like :invoice )";
            tmp.put("invoice", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  ((b.referenceBill.deptId) like :refNo )";
            tmp.put("refNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  ((b.fromInstitution.name) like :frmIns )";
            tmp.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.referenceBill.netTotal) like :total )";
            tmp.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            tmp.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        return sql;
    }

    public void createGrnTable() {
        Date startTime = new Date();

        bills = null;
        String sql;
        HashMap tmp = new HashMap();

        if (getSearchKeyword().getItem() == null) {
            sql = "Select b From BilledBill b where  b.retired=false and b.billType= :bTp "
                    + " and b.institution=:ins "
                    + " and b.createdAt between :fromDate and :toDate ";

            sql += keysForGrnReturn(tmp);

            sql += " order by b.createdAt desc  ";

            tmp.put("toDate", getToDate());
            tmp.put("fromDate", getFromDate());
            tmp.put("ins", getSessionController().getInstitution());
            // tmp.put("ins", getSessionController().getInstitution());
            tmp.put("bTp", BillType.PharmacyGrnBill);
            bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 50);

        } else {
            sql = "Select DISTINCT(bi.bill) From BillItem bi"
                    + " where bi.retired=false and bi.bill.billType= :bTp "
                    + " and bi.bill.institution=:ins "
                    + " and bi.createdAt between :fromDate and :toDate "
                    + " and bi.item=:item ";
            sql += keysForGrnReturnByBillItem(tmp);

            sql += " order by bi.createdAt desc  ";

            tmp.put("toDate", getToDate());
            tmp.put("fromDate", getFromDate());
            tmp.put("ins", getSessionController().getInstitution());
            tmp.put("item", getSearchKeyword().getItem());
            // tmp.put("ins", getSessionController().getInstitution());
            tmp.put("bTp", BillType.PharmacyGrnBill);
            bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 50);
        }
        for (Bill b : bills) {
            b.setListOfBill(getReturnBill(b, BillType.PharmacyGrnReturn));
        }

    }

    public void createGrnTableStore() {
        Date startTime = new Date();

        bills = null;
        String sql;
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where  b.retired=false and b.billType= :bTp "
                + " and b.institution=:ins and"
                + " b.createdAt between :fromDate and :toDate ";

        sql += keysForGrnReturn(tmp);

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("ins", getSessionController().getInstitution());
        tmp.put("bTp", BillType.StoreGrnBill);
        bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getReturnBill(b, BillType.StoreGrnReturn));
        }

    }

    public void createGrnTableAllIns() {
        Date startTime = new Date();

        bills = null;
        String sql;
        HashMap tmp = new HashMap();

        if (getSearchKeyword().getItem() == null) {
            sql = "Select b From BilledBill b where  b.retired=false and b.billType= :bTp "
                    + " and b.createdAt between :fromDate and :toDate ";

            sql += keysForGrnReturn(tmp);

            sql += " order by b.createdAt desc  ";

            tmp.put("toDate", getToDate());
            tmp.put("fromDate", getFromDate());
            // tmp.put("ins", getSessionController().getInstitution());
            tmp.put("bTp", BillType.PharmacyGrnBill);
            bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 50);

        } else {
            sql = "Select DISTINCT(bi.bill) From BillItem bi"
                    + " where bi.retired=false and bi.bill.billType= :bTp "
                    + " and bi.createdAt between :fromDate and :toDate "
                    + " and bi.item=:item";
            sql += keysForGrnReturnByBillItem(tmp);

            sql += " order by bi.createdAt desc  ";

            tmp.put("toDate", getToDate());
            tmp.put("fromDate", getFromDate());
            tmp.put("item", getSearchKeyword().getItem());
            // tmp.put("ins", getSessionController().getInstitution());
            tmp.put("bTp", BillType.PharmacyGrnBill);
            bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 50);
        }

        for (Bill b : bills) {
            b.setListOfBill(getReturnBill(b, BillType.PharmacyGrnReturn));
        }

    }

    public void createGrnTableAllInsStore() {
        Date startTime = new Date();

        bills = null;
        String sql;
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where  b.retired=false and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        sql += keysForGrnReturn(tmp);

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        // tmp.put("ins", getSessionController().getInstitution());
        tmp.put("bTp", BillType.StoreGrnBill);
        bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getReturnBill(b, BillType.StoreGrnReturn));
        }

    }

    private List<Bill> getReturnBill(Bill b, BillType bt) {
        String sql = "Select b From BilledBill b where b.retired=false and b.creater is not null"
                + " and  b.billType=:btp and "
                + " b.referenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", bt);
        return getBillFacade().findByJpql(sql, hm);
    }

    public void createPoTablePharmacy() {
        Date startTime = new Date();

        createPoTable(InstitutionType.Dealer, BillType.PharmacyOrderApprove, BillType.PharmacyGrnBill);

    }

    public void createPoTableStore() {
        Date startTime = new Date();

        createPoTable(InstitutionType.StoreDealor, BillType.StoreOrderApprove, BillType.StoreGrnBill);

    }

    public void createPoTable(InstitutionType institutionType, BillType bt, BillType referenceBillType) {
        bills = null;
        String sql;
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b "
                + " where  b.retired=false"
                + " and b.billType= :bTp"
                + " and b.toInstitution.institutionType=:insTp "
                + " and  b.referenceBill.institution=:ins "
                + " and  b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  ((b.toInstitution.name) like :toIns )";
            tmp.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            tmp.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("insTp", institutionType);
        tmp.put("ins", getSessionController().getInstitution());
        tmp.put("bTp", bt);
        if (getReportKeyWord() != null) {
            if (getReportKeyWord().isAdditionalDetails()) {
                bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP);
            } else {
                bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 50);
            }
        } else {
            bills = getBillFacade().findByJpql(sql, tmp, TemporalType.TIMESTAMP, 50);
        }
        for (Bill b : bills) {
            b.setListOfBill(getGrns(b, referenceBillType));
        }

    }

    private List<Bill> getGrns(Bill b, BillType billType) {
        String sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.creater is not null"
                + " and b.billType=:btp"
                + " and b.referenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", billType);
        return getBillFacade().findByJpql(sql, hm);
    }

    private List<Bill> getIssudBills(Bill b) {
        String sql = "Select b From Bill b where b.retired=false and b.creater is not null"
                + " and b.billType=:btp and "
                + " b.referenceBill=:ref or b.backwardReferenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyTransferIssue);
        return getBillFacade().findByJpql(sql, hm);
    }

    private List<Bill> getBHTIssudBills(Bill b) {
        String sql = "Select b From Bill b where b.retired=false "
                + " and b.billType=:btp "
                + " and b.referenceBill=:ref ";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyBhtPre);
        return getBillFacade().findByJpql(sql, hm);
    }

    private List<Bill> getIssuedBills(Bill b) {
        String sql = "Select b From Bill b where b.retired=false and b.creater is not null"
                + " and b.billType=:btp and "
                + " b.referenceBill=:ref and b.backwardReferenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyTransferIssue);
        return getBillFacade().findByJpql(sql, hm);
    }

    public void createDueFeeTable() {
        Date startTime = new Date();

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where b.retired=false and "
                + " (b.bill.billType=:btp or b.bill.billType=:btpc) "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 and"
                + "  b.bill.createdAt between :fromDate"
                + " and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  ((b.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.billItem.item.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += "  order by b.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.OpdBill);
        temMap.put("btpc", BillType.CollectingCentreBill);

        billFees = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);
        List<BillFee> removeingBillFees = new ArrayList<>();
        if (configOptionApplicationController.getBooleanValueByKey("Remove Refunded Bill From OPD Staff Payment")) {
            for (BillFee bf : billFees) {
                sql = "SELECT bi FROM BillItem bi where bi.retired=false and bi.referanceBillItem.id=" + bf.getBillItem().getId();
                BillItem rbi = getBillItemFacade().findFirstByJpql(sql);

                if (rbi != null) {
                    removeingBillFees.add(bf);
                }

            }
            billFees.removeAll(removeingBillFees);
        }
        calTotal();

    }

    public void createDueFeeTableAll() {
        Date sartTime = new Date();

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where b.retired=false and "
                + " (b.bill.billType=:btp or b.bill.billType=:btpc) "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 and"
                + "  b.bill.createdAt between :fromDate"
                + " and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  ((b.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.billItem.item.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += "  order by b.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.OpdBill);
        temMap.put("btpc", BillType.CollectingCentreBill);

        billFees = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        List<BillFee> removeingBillFees = new ArrayList<>();
        for (BillFee bf : billFees) {
            sql = "SELECT bi FROM BillItem bi where bi.retired=false and bi.referanceBillItem.id=" + bf.getBillItem().getId();
            BillItem rbi = getBillItemFacade().findFirstByJpql(sql);

            if (rbi != null) {
                removeingBillFees.add(bf);
            }

        }
        billFees.removeAll(removeingBillFees);
        calTotal();

    }

    public void createDueFeeTableAndPaidFeeTable() {
        Date startTime = new Date();

        dueTotal = 0.0;
        doneTotal = 0.0;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where b.retired=false and "
                + " b.bill.billType=:btp "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 and"
                + " b.bill.createdAt between :fromDate"
                + " and :toDate ";

        if (speciality != null) {
            sql += " and b.staff.speciality=:special ";
            temMap.put("special", speciality);
            ////System.out.println(speciality);
        }

        if (staff != null) {
            sql += " and b.staff=:staff ";
            temMap.put("staff", staff);
            ////System.out.println(staff);
        }

        if (item != null) {
            sql += " and b.billItem.item=:item ";
            temMap.put("item", item);
            ////System.out.println(item);
        }

        sql += "  order by b.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.OpdBill);

        billFees = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        for (BillFee bf : billFees) {
            dueTotal += bf.getFeeValue();
        }

        temMap.clear();
//        BillFee bf=new BillFee();
//        bf.getBillItem().getCreatedAt();
        sql = "select b.paidForBillFee from BillItem b where b.retired=false and "
                + " b.bill.billType=:btp "
                + " and b.referenceBill.billType=:refType "
                + " and b.paidForBillFee.bill.cancelled=false "
                //                + " and b.feeValue > 0 "
                + " and b.createdAt between :fromDate"
                + " and :toDate ";

//        sql = "Select b FROM BillItem b "
//                + " where b.retired=false "
//                + " and b.bill.billType=:bType "
//                + " and b.referenceBill.billType=:refType "
//                + " and b.createdAt between :fromDate and :toDate ";
        if (speciality != null) {
            sql += " and b.paidForBillFee.staff.speciality=:special ";
            temMap.put("special", speciality);
            ////System.out.println(speciality);
        }

        if (staff != null) {
            sql += " and b.paidForBillFee.staff=:staff ";
            temMap.put("staff", staff);
            ////System.out.println(staff);
        }

        if (item != null) {
            sql += " and b.paidForBillFee.billItem.item=:item ";
            temMap.put("item", item);
            ////System.out.println(item);
        }

        sql += "  order by b.paidForBillFee.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.PaymentBill);
        temMap.put("refType", BillType.OpdBill);

        billFeesDone = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        for (BillFee bf2 : billFeesDone) {
            doneTotal += bf2.getFeeValue();
        }

    }

    double totalPaying;

    public void fillDocPayingBillFee() {
        Date startTime = new Date();

        String sql;
        Map m = new HashMap();

        sql = "select b from BillItem b where b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.referenceBill.billType=:refType "
                //                + " and b.paidForBillFee.bill.cancelled=false "
                + " and b.createdAt between :fromDate and :toDate ";

        if (speciality != null) {
            sql += " and b.paidForBillFee.staff.speciality=:s ";
            m.put("s", speciality);
        }

        if (currentStaff != null) {
            sql += " and b.paidForBillFee.staff=:cs";
            m.put("cs", currentStaff);
        }

        sql += " order by b.bill.insId ";

        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        m.put("btp", BillType.PaymentBill);
        m.put("refType", BillType.OpdBill);

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        totalPaying = 0.0;
        if (billItems == null) {
            return;
        }
        for (BillItem dFee : billItems) {
            totalPaying += dFee.getPaidForBillFee().getFeeValue();
        }

    }

    public void fillDocPayingBill() {

        String sql;
        Map m = new HashMap();

        sql = "select distinct(b.bill) from BillItem b where b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.referenceBill.billType=:refType "
                //                + " and b.paidForBillFee.bill.cancelled=false "
                + " and b.createdAt between :fromDate and :toDate ";

        if (speciality != null) {
            sql += " and b.paidForBillFee.staff.speciality=:s ";
            m.put("s", speciality);
        }

        if (currentStaff != null) {
            sql += " and b.paidForBillFee.staff=:cs";
            m.put("cs", currentStaff);
        }

        sql += " order by b.bill.insId ";

        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        m.put("btp", BillType.PaymentBill);
        m.put("refType", BillType.OpdBill);

//        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        totalPaying = 0.0;
        if (bills == null) {
            return;
        }
        for (Bill b : bills) {
            totalPaying += b.getNetTotal();
        }

    }

    public void createDueFeeTableInward() {
        Date startTime = new Date();

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where "
                + " b.retired=false "
                + " and (b.bill.billType=:btp or b.bill.billType=:btp2 )"
                + " and b.bill.cancelled=false "
                //Starting of newly added code 
                //                + " and b.bill.refunded=false "
                //Ending of newly added code 
                + " and (b.feeValue - b.paidValue) > 0"
                //                + " and  b.bill.billTime between :fromDate and :toDate ";
                + " and  b.bill.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  ((b.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.billItem.item.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += "  order by b.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.InwardBill);
        temMap.put("btp2", BillType.InwardProfessional);

        billFees = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);
        List<BillFee> removeingBillFees = new ArrayList<>();
        for (BillFee bf : billFees) {
            temMap = new HashMap();
            temMap.put("btp", BillType.InwardBill);
            sql = "SELECT bi FROM BillItem bi where bi.retired=false "
                    + " and bi.bill.cancelled=false "
                    + " and bi.bill.billType=:btp "
                    //                    + " and bi.bill.toStaff=:stf "
                    + " and bi.referanceBillItem.id=" + bf.getBillItem().getId();
            BillItem rbi = getBillItemFacade().findFirstByJpql(sql, temMap);

            if (rbi != null) {
                removeingBillFees.add(bf);
            }
        }
        billFees.removeAll(removeingBillFees);
        calTotal();

    }

    public void createDueFeeTableInwardAll() {
        Date startTime = new Date();

        String sql;
        Map temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billClass", BilledBill.class);
        temMap.put("btp", BillType.InwardBill);
        temMap.put("btp2", BillType.InwardProfessional);

        sql = "select b from BillFee b where "
                + " b.retired=false "
                + " and (b.bill.billType=:btp or b.bill.billType=:btp2 )"
                + " and b.bill.cancelled=false "
                + " and type(b.bill)=:billClass "
                //Starting of newly added code 
                //                + " and b.bill.refunded=false "
                //Ending of newly added code 
                + " and (b.feeValue - b.paidValue) > 0"
                //                + " and  b.bill.billTime between :fromDate and :toDate ";
                + " and  b.bill.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  ((b.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.billItem.item.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += "  order by b.staff.id    ";

        ////System.out.println("temMap = " + temMap);
        ////System.out.println("sql = " + sql);
        billFees = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        List<BillFee> removeingBillFees = new ArrayList<>();
        for (BillFee bf : billFees) {
            temMap = new HashMap();
            temMap.put("btp", BillType.InwardBill);
            sql = "SELECT bi FROM BillItem bi where bi.retired=false "
                    + " and bi.bill.cancelled=false "
                    + " and bi.bill.billType=:btp "
                    //                    + " and bi.bill.toStaff=:stf "
                    + " and bi.referanceBillItem.id=" + bf.getBillItem().getId();
            BillItem rbi = getBillItemFacade().findFirstByJpql(sql, temMap);

            if (rbi != null) {
                removeingBillFees.add(bf);
            }
        }
        billFees.removeAll(removeingBillFees);
        calTotal();

    }

    public void createDueFeeTableInwardAllWithCancelled() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where "
                + " b.retired=false "
                + " and (b.bill.billType=:btp or b.bill.billType=:btp2 )"
                + " and (b.feeValue - b.paidValue) > 0"
                + " and  b.bill.billDate between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  ((b.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.billItem.item.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += "  order by b.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.InwardBill);
        temMap.put("btp2", BillType.InwardProfessional);
        ////System.out.println("temMap = " + temMap);
        ////System.out.println("sql = " + sql);

        billFees = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        calTotal();
    }

    double total;

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    private void calTotal() {
        total = 0;
        if (billFees == null) {
            return;
        }

        for (BillFee billFee : billFees) {
            total += billFee.getFeeValue();
        }
    }

    private void calTotalBillItem() {
        total = 0;
        if (billItems == null) {
            return;
        }

        for (BillItem billFee : billItems) {
            total += billFee.getNetValue();
        }
    }

    public void createDueFeeReportInward() {

        Date startTime = new Date();

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where "
                + " b.retired=false "
                + " and (b.bill.billType=:btp or b.bill.billType=:btp2 )"
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0"
                + " and  b.bill.billDate between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  ((b.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.billItem.item.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientEncounter() != null) {
            sql += " and  b.bill.patientEncounter =:pe";
            temMap.put("pe", getSearchKeyword().getPatientEncounter());
        }

        if (getSearchKeyword().getAdmissionType() != null) {
            sql += " and  b.bill.patientEncounter.admissionType =:adty";
            temMap.put("adty", getSearchKeyword().getAdmissionType());
        }

        if (getSearchKeyword().getPaymentMethod() != null) {
            sql += " and  b.bill.patientEncounter.paymentMethod =:payme";
            temMap.put("payme", getSearchKeyword().getPaymentMethod());
        }

        if (getSearchKeyword().getIns() != null) {
            sql += " and  b.bill.patientEncounter.creditCompany=:is";
            temMap.put("is", getSearchKeyword().getIns());
        }

        sql += "  order by b.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.InwardBill);
        temMap.put("btp2", BillType.InwardProfessional);

        billFees = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createPaymentTable() {
        Date startTime = new Date();

        billItems = null;
        HashMap temMap = new HashMap();
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.referenceBill.billType=:refType "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPaymentMethod() != null) {
            sql += " and  b.paidForBillFee.bill.paymentMethod=:pm ";
            temMap.put("pm", getSearchKeyword().getPaymentMethod());
        }

        if (getSearchKeyword().getInsId() != null && !getSearchKeyword().getInsId().trim().equals("")) {
            sql += " and  ((b.bill.insId) like :insId )";
            temMap.put("insId", "%" + getSearchKeyword().getInsId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.billItem.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }
        if (getReportKeyWord().getInstitution() != null) {
            sql += " and  b.paidForBillFee.bill.creditCompany=:cc ";
            temMap.put("cc", getReportKeyWord().getInstitution());
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.OpdBill);

        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createPaymentTableAll() {
        billItems = null;
        HashMap temMap = new HashMap();
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.referenceBill.billType=:refType "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPaymentMethod() != null) {
            sql += " and  b.paidForBillFee.bill.paymentMethod=:pm ";
            temMap.put("pm", getSearchKeyword().getPaymentMethod());
        }

        if (getSearchKeyword().getInsId() != null && !getSearchKeyword().getInsId().trim().equals("")) {
            sql += " and  ((b.bill.insId) like :insId )";
            temMap.put("insId", "%" + getSearchKeyword().getInsId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.billItem.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }
        if (getReportKeyWord().getInstitution() != null) {
            sql += " and  b.paidForBillFee.bill.creditCompany=:cc ";
            temMap.put("cc", getReportKeyWord().getInstitution());
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.OpdBill);

        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createProfessionalPaymentTableInward() {
        Date startTime = new Date();

        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", BilledBill.class);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                //                + " and b.bill.cancelled=false "
                + " and type(b.bill)=:bclass"
                + " and b.bill.billType=:bType "
                + " and (b.referenceBill.billType=:refType "
                + " or b.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getInsId() != null && !getSearchKeyword().getInsId().trim().equals("")) {
            sql += " and  ((b.bill.insId) like :insId )";
            temMap.put("insId", "%" + getSearchKeyword().getInsId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.billItem.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);

        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createProfessionalPaymentTableInwardAll() {
        billItems = null;
        HashMap temMap = new HashMap();
//        temMap.put("bclass", BilledBill.class);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                //                + " and b.bill.cancelled=false "
                //                + " and type(b.bill)=:bclass"
                + " and (b.referenceBill.billType=:refType "
                + " or b.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getInsId() != null && !getSearchKeyword().getInsId().trim().equals("")) {
            sql += " and  ((b.bill.insId) like :insId )";
            temMap.put("insId", "%" + getSearchKeyword().getInsId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.billItem.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.paidForBillFee.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        calTotalBillItem();
    }

    public void createBillItemTableByKeyword() {
        Date startTime = new Date();
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.OpdBill);
        m.put("dep", getSessionController().getDepartment());

        sql = "select bi "
                + " from BillItem bi "
                + " where bi.bill.department=:dep "
                + " and bi.bill.billType=:bType "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (searchKeyword.getPatientName() != null && !searchKeyword.getPatientName().trim().equals("")) {
            sql += " and  ((bi.bill.patient.person.name) like :patientName )";
            m.put("patientName", "%" + searchKeyword.getPatientName().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getPatientPhone() != null && !searchKeyword.getPatientPhone().trim().equals("")) {
            sql += " and  ((bi.bill.patient.person.phone) like :patientPhone )";
            m.put("patientPhone", "%" + searchKeyword.getPatientPhone().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getBillNo() != null && !searchKeyword.getBillNo().trim().equals("")) {
            sql += " and  ((bi.bill.insId) like :billNo )";
            m.put("billNo", "%" + searchKeyword.getBillNo().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getItemName() != null && !searchKeyword.getItemName().trim().equals("")) {
            sql += " and  ((bi.item.name) like :itemName )";
            m.put("itemName", "%" + searchKeyword.getItemName().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getToInstitution() != null && !searchKeyword.getToInstitution().trim().equals("")) {
            sql += " and  ((bi.bill.toInstitution.name) like :toIns )";
            m.put("toIns", "%" + searchKeyword.getToInstitution().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";
        //System.err.println("Sql " + sql);

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);

        checkLabReportsApprovedBillItem(billItems);

        //   searchBillItems = new LazyBillItem(tmp);
    }

    public String toCreateBillItemListForCreditCompany() {
        billItems = new ArrayList<>();
        return "/reportLab/credit_company_bill_item_list";
    }

    public void createBillItemListForCreditCompany() {
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.OpdBill);
        m.put("ins", getCreditCompany());
        sql = "select bi from BillItem bi where bi.bill.creditCompany=:ins "
                + " and bi.bill.billType=:bType "
                + " and bi.createdAt between :fromDate and :toDate ";
        sql += " order by bi.id ";
        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
//        checkLabReportsApprovedBillItem(billItems);
//        
    }

    public void createBillItemTableByKeywordAll() {
        Date startTime = new Date();

        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.OpdBill);
        m.put("ins", getSessionController().getInstitution());

        sql = "select bi from BillItem bi where bi.bill.institution=:ins "
                + " and bi.bill.billType=:bType "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (searchKeyword.getPatientName() != null && !searchKeyword.getPatientName().trim().equals("")) {
            sql += " and  ((bi.bill.patient.person.name) like :patientName )";
            m.put("patientName", "%" + searchKeyword.getPatientName().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getPatientPhone() != null && !searchKeyword.getPatientPhone().trim().equals("")) {
            sql += " and  ((bi.bill.patient.person.phone) like :patientPhone )";
            m.put("patientPhone", "%" + searchKeyword.getPatientPhone().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getBillNo() != null && !searchKeyword.getBillNo().trim().equals("")) {
            sql += " and  ((bi.bill.insId) like :billNo )";
            m.put("billNo", "%" + searchKeyword.getBillNo().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getItemName() != null && !searchKeyword.getItemName().trim().equals("")) {
            sql += " and  ((bi.item.name) like :itemName )";
            m.put("itemName", "%" + searchKeyword.getItemName().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getToInstitution() != null && !searchKeyword.getToInstitution().trim().equals("")) {
            sql += " and  ((bi.bill.toInstitution.name) like :toIns )";
            m.put("toIns", "%" + searchKeyword.getToInstitution().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";
        //System.err.println("Sql " + sql);

        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        checkLabReportsApprovedBillItem(billItems);

        //   searchBillItems = new LazyBillItem(tmp);
    }

    public void createPatientInvestigationsTable() {
        Date startTime = new Date();

        String sql = "select pi "
                + " from PatientInvestigation pi "
                + " join pi.investigation  i "
                + " join pi.billItem.bill b "
                + " join b.patient.person p "
                + " where "
                + " b.createdAt between :fromDate and :toDate  ";

        Map temMap = new HashMap();

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (p.name like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (b.insId like :billNo or b.deptId like :billNo)";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (p.phone like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (i.name like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (patientEncounter != null) {
            sql += "and pi.encounter=:en";
            temMap.put("en", patientEncounter);
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and b.toDepartment=:dep ";
            temMap.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getDepartmentFrom() != null) {
            sql += " and b.fromDepartment=:depFrom ";
            temMap.put("depFrom", getReportKeyWord().getDepartmentFrom());
        }

        sql += " order by pi.id desc  ";
//    

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        patientInvestigations = getPatientInvestigationFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);
        checkRefundBillItems(patientInvestigations);

    }

    public void createPatientInvestigationsTableByLoggedInstitution() {

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  "
                + " and b.institution =:ins ";

//        String sql = "select pi from PatientInvestigation pi where "
//                + " pi.billItem.bill.createdAt between :fromDate and :toDate  ";
        Map temMap = new HashMap();
        temMap.put("ins", getSessionController().getInstitution());

//        if(webUserController.hasPrivilege("LabSearchBillLoggedInstitution")){
//            //System.out.println("inside ins");
//            sql+="and b.institution =:ins ";
//            temMap.put("ins", getSessionController().getInstitution());
//        }
        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((p.name) like :patientName )";
//            sql += " and  ((pi.billItem.bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((p.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((i.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by pi.id desc  ";
//    

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        patientInvestigations = getPatientInvestigationFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createPatientInvestigationsTableByLoggedDepartment() {

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  "
                + " and b.department =:dep ";

//        String sql = "select pi from PatientInvestigation pi where "
//                + " pi.billItem.bill.createdAt between :fromDate and :toDate  ";
        Map temMap = new HashMap();
        temMap.put("dep", getSessionController().getDepartment());

//        if(webUserController.hasPrivilege("LabSearchBillLoggedInstitution")){
//            //System.out.println("inside ins");
//            sql+="and b.institution =:ins ";
//            temMap.put("ins", getSessionController().getInstitution());
//        }
        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((p.name) like :patientName )";
//            sql += " and  ((pi.billItem.bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((p.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((i.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by pi.id desc  ";
//    

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        patientInvestigations = getPatientInvestigationFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void searchPatientInvestigations() {
        Date startTime = new Date();

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  ";
        Map temMap = new HashMap();
        sql += " order by pi.id ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        patientInvestigations = getPatientInvestigationFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public String fillUserPatientReportMobile() {
        return fillUserPatientReport(false);
    }

    public String fillUserPatientReportWeb() {
        return fillUserPatientReport(true);
    }

    public String toSearchReportsByBillNumber() {
        return "/report_search_by_bill_number";
    }

    private String fillUserPatientReport(boolean web) {
        String jpql;
        Map m = new HashMap();
        m.put("pn", getSessionController().getPhoneNo());
        m.put("bn", getSessionController().getBillNo());
        //System.out.println("getSessionController().getPhoneNo() = " + getSessionController().getPhoneNo());
        if (getSessionController().getPhoneNo() == null || getSessionController().getPhoneNo().equals("")) {
            getReportKeyWord().setString("Please Enter Phone Number");
            JsfUtil.addErrorMessage("Please Enter Phone Number");
            return "";
        }
        if (getSessionController().getBillNo() == null || getSessionController().getBillNo().equals("")) {
            getReportKeyWord().setString("Please Enter Bill Number");
            JsfUtil.addErrorMessage("Please Enter Bill Number");
            return "";
        }

        jpql = " select pr from PatientInvestigation pr where pr.retired=false and "
                + " (pr.billItem.bill.patient.person.phone)=:pn and "
                + " ((pr.billItem.bill.insId)=:bn or (pr.billItem.bill.deptId)=:bn) "
                + " order by pr.id desc ";
        if (web) {
            m.put("pn", getSessionController().getPhoneNo());
        } else {
            m.put("pn", getSessionController().getPhoneNo().substring(0, 3) + "-" + getSessionController().getPhoneNo().substring(3));
        }
        m.put("bn", getSessionController().getBillNo());

        userPatientInvestigations = patientInvestigationFacade.findByJpql(jpql, m, 20);

//        return "/reports_list";
        if (!userPatientInvestigations.isEmpty()) {
            if (web) {
                return "/reports_list_new";
            } else {
                getReportKeyWord().setString("0");
                getSessionController().setPhoneNo("");
                getSessionController().setBillNo("");
                return "/reports_list_new_1";
            }
        } else {
            getReportKeyWord().setString("Please Enter Correct Phone Number and Bill Number");
            JsfUtil.addErrorMessage("Please Enter Correct Phone Number and Bill Number");
            return "";
        }
    }

    public void createPatientInvestigationsTableSingle() {
        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient p where "
                + " p=:pt ";
        Map temMap = new HashMap();
        sql += " order by pi.id desc  ";
        temMap.put("pt", getTransferController().getPatient());
//        //////System.out.println("temMap = " + temMap);
//        //////System.out.println("sql = " + sql);
        patientInvestigationsSigle = getPatientInvestigationFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);
//        //////System.out.println("patientInvestigations.size() = " + patientInvestigations.size());
    }

    public void createPatientInvestigationsTableAll() {
        Date startTime = new Date();

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  ";

        Map temMap = new HashMap();

//        if(webUserController.hasPrivilege("LabSearchBillLoggedInstitution")){
//            //System.out.println("inside ins");
//            sql+="and b.institution =:ins ";
//            temMap.put("ins", getSessionController().getInstitution());
//        }
        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((p.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (b.insId like :billNo or b.deptId like :billNo)";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((p.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((i.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (patientEncounter != null) {
            sql += "and pi.encounter=:en";
            temMap.put("en", patientEncounter);
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and b.toDepartment=:dep ";
            temMap.put("dep", getReportKeyWord().getDepartment());
        }

        if (getReportKeyWord().getDepartmentFrom() != null) {
            sql += " and b.fromDepartment=:depFrom ";
            temMap.put("depFrom", getReportKeyWord().getDepartmentFrom());
        }

        sql += " order by pi.id desc  ";
//    

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //System.err.println("Sql " + sql);
        //System.err.println("Sql " + sql);
        //System.err.println("Sql " + sql);
        patientInvestigations = getPatientInvestigationFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        checkRefundBillItems(patientInvestigations);

    }

    public void searchApprovedReportsWithCancelledOrReturnedBills() {
        String sql = "select pr "
                + " from PatientReport pr"
                + " join pr.patientInvestigation pi "
                + " join pi.billItem.bill b "
                + " where "
                + " b.createdAt between :fromDate and :toDate "
                //                + " and b.cancelled=:c "
                //                + " and pr.approved=:a "
                + " order by b.id";

        Map temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("c", true);
        temMap.put("a", true);
        patientReports = getPatientReportFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public void checkRefundBillItems(List<PatientInvestigation> pis) {
        for (PatientInvestigation pi : pis) {
            for (PatientReport pr : pi.getPatientReports()) {
                if (pr.getApproved()) {
                    pi.getBillItem().getBill().setApprovedAnyTest(true);
                    break;
                }
            }
            markRefundBillItem(pi);
        }
    }

    public void markRefundBillItem(PatientInvestigation pi) {
        if (pi == null) {
            return;
        }
        if (pi.getBillItem() == null) {
            return;
        }
        if (pi.getBillItem().getId() == null) {
            return;
        }
        String sql;
        Map m = new HashMap();
        sql = "select bi from BillItem bi "
                + " where bi.referanceBillItem.id=:bi ";
        m.put("bi", pi.getBillItem().getId());
        List<BillItem> bis = getBillItemFacade().findByJpql(sql, m);
        if (bis.isEmpty()) {
            pi.getBillItem().setTransRefund(false);
        } else {
            pi.getBillItem().setTransRefund(true);
        }
    }

    public void fillPatientLabReports(Patient pt) {

        String sql = "select pi "
                + "from PatientInvestigation pi "
                + "join pi.billItem.bill b "
                + "where b.patient=:cp "
                + "and b.createdAt between :fromDate and :toDate ";

        Map temMap = new HashMap();
        temMap.put("cp", pt);

        sql += " order by pi.approveAt desc  ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        patientInvestigations = getPatientInvestigationFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void fillPatientLabreportTable() {
        fillPatientLabReports(patient);
    }

    public void createPatientInvestigationsTableAllByLoggedInstitution() {

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  "
                + " and b.institution =:ins ";

        Map temMap = new HashMap();
        temMap.put("ins", getSessionController().getInstitution());

//        if(webUserController.hasPrivilege("LabSearchBillLoggedInstitution")){
//            //System.out.println("inside ins");
//            sql+="and b.institution =:ins ";
//            temMap.put("ins", getSessionController().getInstitution());
//        }
        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((p.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((p.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((i.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by pi.approveAt desc  ";
//    

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        patientInvestigations = getPatientInvestigationFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createPreBillsForReturn() {
        Date startTime = new Date();

        createPreBillsForReturn(BillType.PharmacyPre, BillType.PharmacySale);

    }

    public void createWholePreBillsForReturn() {
        createPreBillsForReturn(BillType.PharmacyWholesalePre, BillType.PharmacyWholeSale);
    }

    public void createPreBillsForReturn(BillType billType, BillType refBillType) {
        bills = null;
        String sql;
        Map<String, Object> temMap = new HashMap<>();
        if (getSearchKeyword().getItem() == null) {
            sql = "select b from PreBill b where b.billType = :billType and "
                    + "b.institution = :ins and b.billedBill is null and "
                    + "b.department = :dept and b.referenceBill.billType = :refBillType and "
                    + "b.createdAt between :fromDate and :toDate and b.retired = false and "
                    + "b.referenceBill.cancelled = false ";

            if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
                sql += "and b.deptId like :billNo ";
                temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
            }

            if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
                sql += "and b.netTotal like :netTotal ";
                temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
            }

            sql += "order by b.createdAt desc ";

            temMap.put("billType", billType);
            temMap.put("refBillType", refBillType);
            temMap.put("toDate", toDate);
            temMap.put("fromDate", fromDate);
            temMap.put("ins", getSessionController().getInstitution());
            temMap.put("dept", getSessionController().getLoggedUser().getDepartment());

            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

        } else {

            sql = "select DISTINCT(bi.bill) from BillItem bi where bi.bill.billType = :billType and "
                    + " bi.bill.institution=:ins and (bi.bill.billedBill is null) and "
                    + " bi.item=:item and "
                    + " bi.bill.department=:dept and "
                    + " bi.bill.referenceBill.billType=:refBillType "
                    + " and bi.createdAt between :fromDate and :toDate and bi.bill.retired=false "
                    // for remove cancel bills
                    + " and bi.bill.referenceBill.cancelled=false ";

            if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
                sql += " and  ((bi.bill.deptId) like :billNo )";
                temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
            }

            if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
                sql += " and  ((bi.bill.netTotal) like :netTotal )";
                temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
            }

            sql += " order by bi.createdAt desc  ";

            temMap.put("billType", billType);
            temMap.put("refBillType", refBillType);
            temMap.put("toDate", toDate);
            temMap.put("fromDate", fromDate);
            temMap.put("item", getSearchKeyword().getItem());
            temMap.put("ins", getSessionController().getInstitution());
            temMap.put("dept", getSessionController().getLoggedUser().getDepartment());

            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

        }
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public void createPreBillsNotPaid() {
        bills = getBillBean().billsForTheDayNotPaid(BillType.PharmacyPre, getSessionController().getDepartment());
    }

    public void createWholePreBillsNotPaid() {

        bills = getBillBean().billsForTheDayNotPaid(BillType.PharmacyWholesalePre, getSessionController().getDepartment());

    }

    @Inject
    private BillController billController;

    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;

    private boolean printPreview = false;

    public String navigateToAddToStockBillPrint() {
        printPreview = true;
        duplicateBillView = true;

        return "/pharmacy/pharmacy_search_pre_bill_not_paid?faces-redirect=true";
    }

    public void addToStock() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        bill = new Bill();

        bill.setBillDate(new Date());
        bill.setBillTime(new Date());
        bill.setCreatedAt(new Date());

        bill.setDeptId(billController.getBillNumberGenerator().departmentBillNumberGenerator(getSessionController().getDepartment(), getSessionController().getDepartment(), BillType.PharmacyAddtoStock, BillClassType.BilledBill));
        bill.setInsId(billController.getBillNumberGenerator().institutionBillNumberGenerator(getSessionController().getInstitution(), getSessionController().getDepartment(), BillType.PharmacyAddtoStock, BillClassType.BilledBill, BillNumberSuffix.NONE));

        bill.setBillClassType(BillClassType.Bill);
        bill.setBillType(BillType.PharmacyAddtoStock);
        bill.setBillTypeAtomic(BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_ADD_TO_STOCK);

        bill.setCreater(getSessionController().getLoggedUser());
        bill.setDepartment(getSessionController().getDepartment());
        bill.setInstitution(sessionController.getInstitution());

        getBillFacade().create(bill);

        List<BillItem> billItems = new ArrayList<>();

        double totalValue = 0.0;

        for (Bill b : getSelectedBills()) {
            b = getBillFacade().find(b.getId());
            if (b.getReferenceBill() != null) {
                JsfUtil.addErrorMessage("This Bill " + b.getDeptId() + " alrady Paid can't add to stock.");
                continue;
            }
            if (b.checkActiveCashPreBill()) {
                continue;
            }

            totalValue += b.getNetTotal();

            for (BillItem bi : b.getBillItems()) {
                BillItem nbi = new BillItem();
                nbi.copy(bi);
                nbi.invertValue();
                nbi.setBill(bill);

                billItemFacade.create(nbi);

                PharmaceuticalBillItem npi = new PharmaceuticalBillItem();
                npi.copy(bi.getPharmaceuticalBillItem());
                npi.invertValue(npi);
                npi.setBillItem(nbi);

                pharmaceuticalBillItemFacade.create(npi);

                nbi.setPharmaceuticalBillItem(npi);

                billItemFacade.edit(nbi);
                billItems.add(nbi);
            }

            Bill prebill = getPharmacyBean().reAddToStock(b, getSessionController().getLoggedUser(),
                    getSessionController().getDepartment(), BillNumberSuffix.PRECAN);

            if (prebill != null) {
                b.setCancelled(true);
                b.setCancelledBill(prebill);
                getBillFacade().edit(b);
            }
        }
        bill.setNetTotal(totalValue);
        bill.setBillItems(billItems);
        getBillFacade().edit(bill);

        createPreBillsNotPaid();
        printPreview = true;
        duplicateBillView = false;
    }

    public void cancelIssueToUnitBills() {
        if (cancellingIssueBill == null) {
            JsfUtil.addErrorMessage("Select a bill to cancel");
            return;
        }
        Bill b = cancellingIssueBill;

        if (b.isCancelled() || b.isRefunded()) {
            JsfUtil.addErrorMessage("Can not cancel already cancelled or returned bills");
            return;
        }
        if (b instanceof PreBill) {
            Bill prebill = getPharmacyBean().readdStockForIssueBills((PreBill) b, getSessionController().getLoggedUser(),
                    getSessionController().getDepartment(), BillNumberSuffix.DIC);
            b.setCancelled(true);
            b.setCancelledBill(prebill);
            getBillFacade().edit(b);
            JsfUtil.addSuccessMessage("Cancelled");
        } else {
            JsfUtil.addErrorMessage("Not an Issue Bill. Can not cancell");
        }

    }

    private String createPharmacyPayKeyword(Map temMap) {
        String sql = "";
        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getReportKeyWord().getDepartment() != null) {
            sql += " and b.department=:dep ";
            temMap.put("dep", getReportKeyWord().getDepartment());
        }

        return sql;

    }

    public void createOpdBathcBillPreTable() {
        Date startTime = new Date();
        aceptPaymentBills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billType = :billType "
                + " and b.institution=:ins"
                //                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";
//                + " and b.deptId is not null ";

        sql += createPharmacyPayKeyword(temMap);
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.OpdBathcBillPre);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        aceptPaymentBills = getBillFacade().findByJpqlWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 25);

    }

    public void createOpdBillSearch() {
        Date startTime = new Date();
        createTableByKeyword(BillType.OpdBill);
//        checkLabReportsApproved(bills);

    }

    public void createOpdBathcBillPreTablePaidOnly() {
        Date startTime = new Date();
        aceptPaymentBills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billType = :billType "
                + " and b.institution=:ins"
                + " and b.referenceBill.balance=0 "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";
//                + " and b.deptId is not null ";

        sql += createPharmacyPayKeyword(temMap);
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.OpdBathcBillPre);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        aceptPaymentBills = getBillFacade().findByJpqlWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 25);

    }

    public void createOpdBathcBillPreTableNotPaidOly() {
        Date startTime = new Date();
        aceptPaymentBills = new ArrayList<>();
        String sql;
        Map temMap = new HashMap();

        List<Bill> abs = new ArrayList<>();

        sql = "select b from PreBill b "
                + " where b.billType = :billType "
                + " and b.institution=:ins"
                //                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";
//                + " and b.deptId is not null ";

        sql += createPharmacyPayKeyword(temMap);
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.OpdBathcBillPre);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        abs = getBillFacade().findByJpqlWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 50);

        List<Bill> pbs = new ArrayList<>();
        Map temMap2 = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billType = :billType "
                + " and b.institution=:ins"
                + " and b.referenceBill.balance=0 "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";
//                + " and b.deptId is not null ";

        sql += createPharmacyPayKeyword(temMap2);
        sql += " order by b.createdAt desc  ";
//    
        temMap2.put("billType", BillType.OpdBathcBillPre);
        temMap2.put("toDate", getToDate());
        temMap2.put("fromDate", getFromDate());
        temMap2.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        pbs = getBillFacade().findByJpqlWithoutCache(sql, temMap2, TemporalType.TIMESTAMP, 50);

        abs.removeAll(pbs);
        aceptPaymentBills.addAll(abs);

    }

    public String searchOpdBatchBillsSettledAtCashier() {
        bills = null;
        String jpql;
        Map m = new HashMap();
        jpql = "select b "
                + " from Bill b "
                + " where b.retired=false "
                + " and b.institution=:ins"
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.deptId is not null "
                + " and b.cancelled=false"
                + " and b.referenceBill is not null ";
        jpql += " and b.billTypeAtomic = :billType ";
        m.put("billType", BillTypeAtomic.OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        jpql += " order by b.createdAt desc  ";
        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        m.put("ins", getSessionController().getInstitution());
        bills = getBillFacade().findByJpqlWithoutCache(jpql, m, TemporalType.TIMESTAMP, 25);
        return "/opd/opd_search_pre_bill?faces-redirect=true";
    }

    public String searchOpdBatchBillsToSettleAtCashier() {
        bills = null;
        String jpql;
        Map m = new HashMap();
        jpql = "select b "
                + " from Bill b "
                + " where b.retired=false "
                + " and b.institution=:ins"
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.deptId is not null "
                + " and b.cancelled=false"
                + " and b.referenceBill is null ";
        jpql += " and b.billTypeAtomic = :billType ";
        m.put("billType", BillTypeAtomic.OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        jpql += " order by b.createdAt desc  ";
        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        m.put("ins", getSessionController().getInstitution());
        bills = getBillFacade().findByJpqlWithoutCache(jpql, m, TemporalType.TIMESTAMP, 25);
        return "/opd/opd_search_pre_bill?faces-redirect=true";
    }

    public String searchOpdBatchBillsSettledOrNotSettled() {
        bills = null;
        String jpql;
        Map m = new HashMap();
        jpql = "select b "
                + " from Bill b "
                + " where b.retired=false "
                + " and b.institution=:ins"
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.deptId is not null ";
        jpql += " and b.billTypeAtomic = :billType ";
        m.put("billType", BillTypeAtomic.OPD_BATCH_BILL_TO_COLLECT_PAYMENT_AT_CASHIER);
        jpql += " order by b.createdAt desc  ";
        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        m.put("ins", getSessionController().getInstitution());
        bills = getBillFacade().findByJpqlWithoutCache(jpql, m, TemporalType.TIMESTAMP, 25);
        return "/opd/opd_search_pre_bill?faces-redirect=true";
    }

    public String createOpdPreTable() {
        createPreTable(BillType.OpdPreBill);
        return "/opd/opd_search_pre_bill?faces-redirect=true";
    }

    public void fillPatientBillsToPay() {
        if (patient == null) {
            JsfUtil.addErrorMessage("Please select a Patient");
            return;
        }
        fillPatientPreBills(BillType.OpdPreBill, patient, true, null);
    }

    public void fillPatientBillsPaid() {
        if (patient == null) {
            JsfUtil.addErrorMessage("Please select a Patient");
            return;
        }
        fillPatientPreBills(BillType.OpdPreBill, patient, null, false);
    }

    public void fillPatientBillsPaidAndToPay() {
        if (patient == null) {
            JsfUtil.addErrorMessage("Please select a Patient");
            return;
        }
        fillPatientPreBills(BillType.OpdPreBill, patient, null, null);
    }

    public String createOpdPreTableNotPaid() {
        createPreTableNotPaid(BillType.OpdPreBill);
        return "/opd/opd_search_pre_bill?faces-redirect=true";
    }

    public String createOpdPreTablePaid() {
        createPreTablePaid(BillType.OpdPreBill);
        return "/opd/opd_search_pre_bill?faces-redirect=true";
    }

    public void fillPharmacyPreBillsToAcceptAtCashierGetPaid() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billTypeAtomic = :billTypeAtomic "
                + " and b.institution=:ins "
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false "
                + " and b.deptId is not null "
                + " and b.cancelled=false"
                + " and b.referenceBill is not null ";

        sql += createPharmacyPayKeyword(temMap);
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billTypeAtomic", BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpqlWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 25);
    }

    public void fillPharmacyPreBillsToAcceptAtCashierNotPaid() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billTypeAtomic = :billTypeAtomic "
                + " and b.institution=:ins "
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false "
                + " and b.billTypeAtomic is not null "
                + " and b.deptId is not null "
                + " and b.cancelled=false"
                + " and b.referenceBill is null ";

        sql += createPharmacyPayKeyword(temMap);
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billTypeAtomic", BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpqlWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 25);
    }

    public void fillPharmacyPreBillsToAcceptAtCashier() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billTypeAtomic = :billTypeAtomic "
                + " and b.institution=:ins "
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false "
                + " and b.deptId is not null "
                + " and b.cancelled=false";

        sql += createPharmacyPayKeyword(temMap);
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billTypeAtomic", BillTypeAtomic.PHARMACY_RETAIL_SALE_PRE_TO_SETTLE_AT_CASHIER);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpqlWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 25);
    }

    public void createPharmacyPreTable() {
        Date startTime = new Date();
        createPreTable(BillType.PharmacyPre);

    }

    public void createPharmacyPreTableNotPaid() {
        Date startTime = new Date();
        createPreTableNotPaid(BillType.PharmacyPre);

    }

    public void createPharmacyPreTablePaid() {
        Date startTime = new Date();
        createPreTablePaid(BillType.PharmacyPre);

    }

    public void createPreTable(BillType bt) {
        createPreTable(bt, null);
    }

    public void createPreTable(BillType bt, Patient pt) {
        bills = null;
        String jpql;
        Map m = new HashMap();

        jpql = "select b "
                + " from PreBill b "
                + " where b.retired=false "
                + " and b.institution=:ins"
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.deptId is not null ";

        if (bt != null) {
            jpql += " and b.billType = :billType ";
            m.put("billType", bt);
        }

        if (pt != null) {
            jpql += " and b.patient=:pt ";
            m.put("pt", pt);
        }

        jpql += createPharmacyPayKeyword(m);
        jpql += " order by b.createdAt desc  ";
//    

        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        m.put("ins", getSessionController().getInstitution());

        bills = getBillFacade().findByJpqlWithoutCache(jpql, m, TemporalType.TIMESTAMP, 25);

    }

    public void fillPatientPreBills(BillType bt, Patient pt, Boolean paidOnly, Boolean toPayOnly) {
        bills = null;
        String jpql;
        Map m = new HashMap();

        jpql = "select b "
                + " from PreBill b "
                + " where b.retired=false "
                + " and b.institution=:ins"
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.deptId is not null ";

        if (bt != null) {
            jpql += " and b.billType = :billType ";
            m.put("billType", bt);
        }

        if (pt != null) {
            jpql += " and b.patient=:pt ";
            m.put("pt", pt);
        }

        if (paidOnly != null) {
            jpql += " and b.referenceBill is not null ";
        }

        if (toPayOnly != null) {
            jpql += " and b.referenceBill is null ";
        }

        jpql += " order by b.createdAt desc  ";

        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        m.put("ins", getSessionController().getInstitution());
        bills = getBillFacade().findByJpql(jpql, m, TemporalType.TIMESTAMP, 25);

    }

    public void createPreTableNotPaid(BillType bt) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false "
                + " and b.deptId is not null "
                + " and b.cancelled=false"
                + " and b.referenceBill is null ";

        sql += createPharmacyPayKeyword(temMap);
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", bt);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpqlWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 25);

    }

    public void createPreTablePaid(BillType bt) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false "
                + " and b.deptId is not null "
                + " and b.cancelled=false"
                + " and b.referenceBill is not null ";

        sql += createPharmacyPayKeyword(temMap);
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", bt);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpqlWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 25);

    }

    public void createGrnPaymentTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b"
                + " where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.GrnPayment);
        temMap.put("insTp", InstitutionType.Dealer);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createGrnPaymentTableStore() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b"
                + " where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.GrnPayment);
        temMap.put("insTp", InstitutionType.StoreDealor);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyPayment() {
        Date startTime = new Date();

        InstitutionType[] institutionTypes = {InstitutionType.Dealer};
        createGrnPaymentTable(Arrays.asList(institutionTypes), BillType.GrnPayment);

    }

    public void createStorePayment() {
        Date startTime = new Date();

        InstitutionType[] institutionTypes = {InstitutionType.StoreDealor};
        createGrnPaymentTable(Arrays.asList(institutionTypes), BillType.GrnPayment);

    }

    public void createStorePaharmacyPayment() {
        Date startTime = new Date();

        InstitutionType[] institutionTypes = {InstitutionType.Dealer, InstitutionType.StoreDealor};
        createGrnPaymentTable(Arrays.asList(institutionTypes), BillType.GrnPayment);

    }

    public void createPharmacyPaymentPre() {
        Date startTime = new Date();

        InstitutionType[] institutionTypes = {InstitutionType.Dealer};
        createGrnPaymentTable(Arrays.asList(institutionTypes), BillType.GrnPaymentPre);

    }

    public void createStorePaymentPre() {
        Date startTime = new Date();

        InstitutionType[] institutionTypes = {InstitutionType.StoreDealor};
        createGrnPaymentTable(Arrays.asList(institutionTypes), BillType.GrnPaymentPre);

    }

    public void createStorePaharmacyPaymentPre() {
        Date startTime = new Date();

        InstitutionType[] institutionTypes = {InstitutionType.Dealer, InstitutionType.StoreDealor};
        createGrnPaymentTable(Arrays.asList(institutionTypes), BillType.GrnPaymentPre);

    }

    private void createGrnPaymentTable(List<InstitutionType> institutionTypes, BillType billType) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b "
                + " where b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.toInstitution.institutionType in :insTp "
                + " and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", billType);
        temMap.put("insTp", institutionTypes);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //  temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createGrnPaymentTableAllStore() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b "
                + " where b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.GrnPayment);
        temMap.put("insTp", InstitutionType.StoreDealor);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //  temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    @Deprecated
    public void searchDepartmentOpdBills() {
        Date startTime = new Date();

        if (fromDate != null && toDate != null) {

            long timeGapInMillis = toDate.getTime() - fromDate.getTime();

            long daysGap = timeGapInMillis / (1000 * 60 * 60 * 24);

            if (daysGap > 3) {
//                if (searchKeyword.getBillNo() == null && searchKeyword.getBillNo().trim().equals("")) {
//                    JsfUtil.addErrorMessage("Please select upto 3 days or Use filtering data option");
//                    return;
//                } else if (searchKeyword.getPatientName() == null && searchKeyword.getPatientName().trim().equals("")) {
//                    JsfUtil.addErrorMessage("Please select upto 3 days or Use filtering data option");
//                    return;
//                } else if (searchKeyword.getPatientPhone() == null && searchKeyword.getPatientPhone().trim().equals("")) {
//                    JsfUtil.addErrorMessage("Please select upto 3 days or Use filtering data option");
//                    return;
//                }
                JsfUtil.addErrorMessage("Please select upto 3 days");
                return;
            }
        }

        createTableByKeyword(BillType.OpdBill, null, sessionController.getDepartment());
        checkLabReportsApproved(bills);

    }

    public void searchDepartmentOpdBillLights() {
        Date startTime = new Date();
        fillBills(BillType.OpdBill, null, sessionController.getDepartment());

    }

    public void searchOpdBatchBills() {
        Date startTime = new Date();
        createTableByKeyword(BillType.OpdBathcBill, institution, department, fromInstitution, fromDepartment, toInstitution, toDepartment);
        checkLabReportsApproved(bills);

    }

    public void searchOpdBills() {
        Date startTime = new Date();
        List<BillTypeAtomic> billTypesAtomics = new ArrayList<>();
        billTypesAtomics.add(BillTypeAtomic.OPD_BILL_WITH_PAYMENT);
        billTypesAtomics.add(BillTypeAtomic.PACKAGE_OPD_BILL_WITH_PAYMENT);
        billTypesAtomics.add(BillTypeAtomic.OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER);
        createTableByKeyword(billTypesAtomics, institution, department, fromInstitution, fromDepartment, toInstitution, toDepartment);
        checkLabReportsApproved(bills);

    }

    public void listOpdBatcuBills() {
        Date startTime = new Date();
        createTableByKeyword(BillType.OpdBathcBill, institution, department);
        checkLabReportsApproved(bills);
    }

    public void listOpdBills() {
        Date startTime = new Date();
        createTableByKeyword(BillType.OpdBill);
        checkLabReportsApproved(bills);

    }

    public void createCollectingCentreBillSearch() {
        Date startTime = new Date();
        createCCBillTableByKeyword(BillType.CollectingCentreBill, null, null);
        //checkLabReportsApproved(bills);

    }

    public void createCCBillTableByKeyword(BillType billType, Institution ins, Department dep) {
        billLights = null;
        String sql;
        Map temMap = new HashMap();
        sql = "select new com.divudi.light.common.BillLight(bill.id, bill.insId, bill.createdAt, bill.fromInstitution.name, bill.institution.name, bill.toDepartment.name, bill.creater.name, bill.patient.person.name, bill.patient.person.phone, bill.total, bill.discount, bill.netTotal, bill.patient.id) "
                + " from BilledBill bill "
                + " where bill.billType = :billType "
                + " and bill.createdAt between :fromDate and :toDate "
                + " and bill.retired=false ";

        if (ins != null) {
            sql += " and bill.institution=:ins ";
            temMap.put("ins", ins);
        }

        if (dep != null) {
            sql += " and bill.department=:dep ";
            temMap.put("dep", dep);
        }

        if (getInstitution() != null) {
            sql += " and  ((bill.fromInstitution) =:ccName )";
            temMap.put("ccName", getInstitution());
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  ((bill.fromInstitution.institutionCode) like :code )";
            temMap.put("code", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((bill.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (((bill.insId) like :billNo )or((bill.deptId) like :billNo ))";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((bill.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((bill.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }
        sql += " order by bill.createdAt desc  ";

        temMap.put("billType", billType);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        billLights = getBillFacade().findLightsByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public void listOpdBilledBills() {
        listBills(BillType.OpdBill, BilledBill.class, false, false, null, null, null, null, null, null);
    }

    public void listBills(BillType billType, Class billClass, Boolean onlyCancelledBills, Boolean onlyReturnedBills,
            Institution fromInstitution, Department fromDepartment,
            Institution toInstitution, Department toDepartment,
            Doctor referredDoctor, Institution referredInstitution) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (billClass != null) {
            sql += " and type(b.bill)=:class ";
            temMap.put("class", billClass);
        }

        if (onlyCancelledBills == true) {
            sql += " and b.cancelled=:cancelled ";
            temMap.put("cancelled", true);
        }
        if (onlyReturnedBills == true) {
            sql += " and b.refunded=:refunded ";
            temMap.put("refunded", true);
        }
        if (fromInstitution != null) {
            sql += " and b.fromInstitution=:fromIns ";
            temMap.put("fromIns", fromInstitution);
        }
        if (fromDepartment != null) {
            sql += " and b.fromDepartment=:fromDepartment ";
            temMap.put("fromDepartment", fromDepartment);
        }
        if (toInstitution != null) {
            sql += " and b.toInstitution=:toIns ";
            temMap.put("toIns", toInstitution);
        }
        if (toDepartment != null) {
            sql += " and b.toDepartment=:toDepartment ";
            temMap.put("toDepartment", toDepartment);
        }
        if (referredDoctor != null) {
            sql += " and b.referredBy=:referredDoctor ";
            temMap.put("fromIns", referredDoctor);
        }
        if (referredInstitution != null) {
            sql += " and b.referredByInstitution=:referredInstitution ";
            temMap.put("fromDepartment", referredInstitution);
        }

        /**
         *
         *
         *
         *
         * temp.setStaff(staff); temp.setToStaff(toStaff);
         * temp.setReferredBy(referredBy); temp.setReferenceNumber(referralId);
         * temp.setReferredByInstitution(referredByInstitution);
         * temp.setCreditCompany(creditCompany);
         * temp.setCollectingCentre(collectingCentre);
         *
         */
        sql += " order by b.createdAt desc  ";

        temMap.put("billType", billType);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void fillOpdClients() {
        fillClients(BillType.OpdBill);
    }

    public void fillClients(BillType billType) {
        patients = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select distinct(b.patient) from BilledBill b where b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (fromDepartment != null) {
            sql += " and b.fromDepartment=:fdep ";
            temMap.put("fdep", fromDepartment);
        }
        if (toDepartment != null) {
            sql += " and b.toDepartment=:tdep ";
            temMap.put("tdep", toDepartment);
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", billType);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        patients = getPatientFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createTableByKeyword(BillType billType) {
        fillBills(billType, null, null);
    }

    public void fillBills(BillType billType, Institution ins, Department dep) {
        bills = null;
        String sql;
        Map temMap = new HashMap();
        sql = "select new com.divudi.light.common.BillLight(bill.id, bill.insId, bill.createdAt, bill.institution.name, bill.toDepartment.name, bill.creater.name, bill.patient.person.name, bill.patient.person.phone, bill.total, bill.discount, bill.netTotal, bill.patient.id) "
                + " from BilledBill bill "
                + " where bill.billType = :billType "
                + " and bill.createdAt between :fromDate and :toDate "
                + " and bill.retired=false ";
        if (ins != null) {
            sql += " and bill.institution=:ins ";
            temMap.put("ins", ins);
        }

        if (dep != null) {
            sql += " and bill.department=:dep ";
            temMap.put("dep", dep);
        }

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((bill.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (((bill.insId) like :billNo )or((bill.deptId) like :billNo ))";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((bill.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((bill.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }
        sql += " order by bill.createdAt desc  ";

        temMap.put("billType", billType);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        billLights = getBillFacade().findLightsByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<Bill> fillBills(BillType billType, Institution ins, Department dep, Patient patient) {
        return fillBills(billType, ins, dep, patient, 10);
    }

    public List<Bill> fillBills(BillType billType, Institution ins, Department dep, Patient patient, Integer maxCount) {
        List<Bill> bs;
        String jpql;
        Map m = new HashMap();
        jpql = "select bill"
                + " from BilledBill bill "
                + " where bill.retired=:ret ";
        m.put("ret", false);
        if (billType != null) {
            jpql += " and bill.billType=:billType ";
            m.put("billType", billType);
        }

        if (ins != null) {
            jpql += " and bill.institution=:ins ";
            m.put("ins", ins);
        }

        if (dep != null) {
            jpql += " and bill.department=:dep ";
            m.put("dep", dep);
        }
        if (patient != null) {
            jpql += " and  bill.patient=:pt";
            m.put("pt", patient);
        }

        jpql += " order by bill.id desc  ";

        bs = getBillFacade().findByJpql(jpql, m, TemporalType.TIMESTAMP, maxCount);

        return bs;
    }

    public List<BillLight> listBillsLights(
            BillType billType,
            Institution ins,
            Department dep,
            SearchKeyword searchKw,
            Date fd,
            Date td) {
        String sql;
        Map temMap = new HashMap();
        sql = "select new com.divudi.light.common.BillLight(bill.id, bill.insId, bill.createdAt, bill.institution.name, bill.toDepartment.name, bill.creater.name, bill.patient.person.name, bill.patient.person.phone, bill.total, bill.discount, bill.netTotal, bill.patient.id) "
                + " from BilledBill bill "
                + " where bill.billType = :billType "
                + " and bill.createdAt between :fromDate and :toDate "
                + " and bill.retired=false ";
        if (ins != null) {
            sql += " and bill.institution=:ins ";
            temMap.put("ins", ins);
        }
        if (dep != null) {
            sql += " and bill.department=:dep ";
            temMap.put("dep", dep);
        }
        if (searchKw != null && !searchKw.getPatientName().trim().equals("")) {
            sql += " and  ((bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + searchKw.getPatientName().trim().toUpperCase() + "%");
        }
        if (searchKw != null && searchKw.getPatientPhone() != null && !searchKw.getPatientPhone().trim().equals("")) {
            sql += " and  ((bill.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + searchKw.getPatientPhone().trim().toUpperCase() + "%");
        }
        if (searchKw != null && searchKw.getBillNo() != null && !searchKw.getBillNo().trim().equals("")) {
            sql += " and  (((bill.insId) like :billNo )or((bill.deptId) like :billNo ))";
            temMap.put("billNo", "%" + searchKw.getBillNo().trim().toUpperCase() + "%");
        }
        if (searchKw != null && searchKw.getNetTotal() != null && !searchKw.getNetTotal().trim().equals("")) {
            sql += " and  ((bill.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + searchKw.getNetTotal().trim().toUpperCase() + "%");
        }
        if (searchKw != null && searchKw.getTotal() != null && !searchKw.getTotal().trim().equals("")) {
            sql += " and  ((bill.total) like :total )";
            temMap.put("total", "%" + searchKw.getTotal().trim().toUpperCase() + "%");
        }
        sql += " order by bill.createdAt desc  ";
        temMap.put("billType", billType);
        temMap.put("toDate", td);
        temMap.put("fromDate", fd);
        return (List<BillLight>) getBillFacade().findLightsByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    @Deprecated
    public void createTableByKeyword(BillType billType, Institution ins, Department dep) {

        createTableByKeyword(billType, ins, dep, null, null, null, null);

    }

    public void createTableByKeyword(List<BillTypeAtomic> billTypesAtomics,
            Institution ins, Department dep,
            Institution fromIns,
            Department fromDep,
            Institution toIns,
            Department toDep) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b "
                + " from Bill b "
                + " where b.billTypeAtomic in :billTypesAtomics "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        if (ins != null) {
            sql += " and b.institution=:ins ";
            temMap.put("ins", ins);
        }

        if (dep != null) {
            sql += " and b.department=:dep ";
            temMap.put("dep", dep);
        }

        if (toDep != null) {
            sql += " and b.toDepartment=:todep ";
            temMap.put("todep", toDep);
        }

        if (fromDep != null) {
            sql += " and b.fromDepartment=:fromdep ";
            temMap.put("fromdep", fromDep);
        }

        if (fromIns != null) {
            sql += " and b.fromInstitution=:fromins ";
            temMap.put("fromins", fromIns);
        }

        if (toIns != null) {
            sql += " and b.toInstitution=:toins ";
            temMap.put("toins", toIns);
        }

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  b.deptId like :billNo";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billTypesAtomics", billTypesAtomics);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createTableByKeyword(BillType billType,
            Institution ins, Department dep,
            Institution fromIns,
            Department fromDep,
            Institution toIns,
            Department toDep) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b "
                + " from BilledBill b "
                + " where b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        if (ins != null) {
            sql += " and b.institution=:ins ";
            temMap.put("ins", ins);
        }

        if (dep != null) {
            sql += " and b.department=:dep ";
            temMap.put("dep", dep);
        }

        if (toDep != null) {
            sql += " and b.toDepartment=:todep ";
            temMap.put("todep", toDep);
        }

        if (fromDep != null) {
            sql += " and b.fromDepartment=:fromdep ";
            temMap.put("fromdep", fromDep);
        }

        if (fromIns != null) {
            sql += " and b.fromInstitution=:fromins ";
            temMap.put("fromins", fromIns);
        }

        if (toIns != null) {
            sql += " and b.toInstitution=:toins ";
            temMap.put("toins", toIns);
        }

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  b.deptId like :billNo";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", billType);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void clearOpdBillSearchData() {
        getSearchKeyword().setPatientName(null);
        getSearchKeyword().setPatientPhone(null);
        getSearchKeyword().setTotal(null);
        getSearchKeyword().setNetTotal(null);
        getSearchKeyword().setBillNo(null);
        fromInstitution = null;
        fromDepartment = null;
        toInstitution = null;
        toDepartment = null;

    }

    public void processAllFinancialTransactionalSummary() {
        billSummaryRows = null;
        grossTotal = 0.0;
        discount = 0.0;
        netTotal = 0.0;
        String jpql;
        Map params = new HashMap();
        List<BillTypeAtomic> billTypesToFilter = new ArrayList<>();
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT_REVERSE));
        jpql = "select new com.divudi.light.common.BillSummaryRow("
                + "b.billTypeAtomic, "
                + "sum(b.total), "
                + "sum(b.discount), "
                + "sum(b.netTotal),"
                + "count(b)) "
                + " from Bill b "
                + " where b.retired=:ret"
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.billTypeAtomic in :abts ";

        Bill b = new Bill();
        b.getTotal();
        b.getDiscount();
        b.getNetTotal();
        b.getBillTypeAtomic();
        b.getCreater();
        if (institution == null && department == null) {
            jpql += " and b.institution=:ins";
            params.put("ins", sessionController.getInstitution());

        } else if (institution != null && department == null) {
            jpql += " and b.institution=:ins";
            params.put("ins", getInstitution());

            jpql += " and b.department=:dept";
            params.put("dept", sessionController.getDepartment());

        } else if (institution != null && department != null) {
            jpql += " and b.institution=:ins";
            params.put("ins", getInstitution());

            jpql += " and b.department=:dept";
            params.put("dept", getDepartment());
        }

        if (webUser != null) {
            jpql += " and b.creater=:wu";
            params.put("wu", webUser);
        }

        jpql += " group by b.billTypeAtomic order by b.billTypeAtomic";

        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());
        params.put("ret", false);
        params.put("abts", billTypesToFilter);

        billSummaryRows = getBillFacade().findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        for (BillSummaryRow bss : billSummaryRows) {
            grossTotal += bss.getGrossTotal();
            discount += bss.getDiscount();
            netTotal += bss.getNetTotal();
        }
    }

    public void processUserFinancialTransactionalSummaryByBillForBillTypeAtomic() {
        reportTemplateType = ReportTemplateType.BILL_TYPE_ATOMIC_SUMMARY_USING_BILLS;
        billSummaryRows = null;
        grossTotal = 0.0;
        discount = 0.0;
        netTotal = 0.0;
        String jpql;
        Map params = new HashMap();
        List<BillTypeAtomic> billTypesToFilter = new ArrayList<>();
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT_REVERSE));
        jpql = "select new com.divudi.light.common.BillSummaryRow("
                + "b.billTypeAtomic, "
                + "sum(b.total), "
                + "sum(b.discount), "
                + "sum(b.netTotal),"
                + "count(b)) "
                + " from Bill b "
                + " where b.retired=:ret"
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.billTypeAtomic in :abts "
                + " and b.creater=:user";

//        Bill b = new Bill();
//        b.getTotal();
//        b.getDiscount();
//        b.getNetTotal();
//        b.getBillTypeAtomic();
//        b.getCreater();
        jpql += " group by b.billTypeAtomic order by b.billTypeAtomic";

        params.put("user", getWebUser());
        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());
        params.put("ret", false);
        params.put("abts", billTypesToFilter);

        billSummaryRows = getBillFacade().findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        for (BillSummaryRow bss : billSummaryRows) {
            grossTotal += bss.getGrossTotal();
            discount += bss.getDiscount();
            netTotal += bss.getNetTotal();
        }
    }

    public void processUserFinancialTransactionalSummarybyBillForPaymentMethodAndBillTypeAtomic() {
        reportTemplateType = ReportTemplateType.BILLT_TYPE_AND_PAYMENT_METHOD_SUMMARY_PAYMENTS;
        System.out.println("institution = " + institution);
        if (institution == null) {
            setDepartments(null);
        }
        billSummaryRows = null;
        totalPaying = 0.0;
        cashTotal = 0.0;
        cardTotal = 0.0;
        chequeTotal = 0.0;
        slipTotal = 0.0;
        totalOfOtherPayments = 0.0;

        String jpql;
        Map params = new HashMap();
        List<BillTypeAtomic> billTypesToFilter = new ArrayList<>();
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT_REVERSE));

        jpql = "select new com.divudi.light.common.BillSummaryRow("
                + "p.bill.billTypeAtomic, "
                + "sum(p.paidValue), "
                + "count(p.bill), "
                + "p.paymentMethod ) "
                + " from Payment p "
                + " where p.bill.retired=:ret"
                + " and p.createdAt between :fromDate and :toDate "
                + " and p.bill.billTypeAtomic in :abts ";

        if (webUser != null) {
            jpql += " and p.bill.creater=:wu";
            params.put("wu", webUser);
        }

//        if (institution == null) {
//            jpql += " and p.bill.institution=:ins";
//            params.put("ins", sessionController.getInstitution());
//        } else {
//            jpql += " and p.bill.institution=:ins";
//            params.put("ins", institution);
//        }
//
//        if (department == null) {
////            jpql += " and p.bill.department=:dept";
////            params.put("dept", sessionController.getLoggedUser().getDepartment());
//        } else {
//            jpql += " and p.bill.department=:dept";
//            params.put("dept", department);
//        }
        jpql += " group by p.paymentMethod, p.bill.billTypeAtomic "
                + " order by p.bill.billTypeAtomic";

        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());
        params.put("ret", false);
        params.put("abts", billTypesToFilter);
        billSummaryRows = paymentFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        for (BillSummaryRow bss : billSummaryRows) {
            if (bss.getPaymentMethod() == PaymentMethod.Cash) {
                cashTotal += bss.getPaidValue();
            } else if (bss.getPaymentMethod() == PaymentMethod.Card) {
                cardTotal += bss.getPaidValue();
            } else if (bss.getPaymentMethod() == PaymentMethod.Cheque) {
                chequeTotal += bss.getPaidValue();
            } else if (bss.getPaymentMethod() == PaymentMethod.Slip) {
                slipTotal += bss.getPaidValue();
            } else {

                totalOfOtherPayments += bss.getPaidValue();
            }
            totalPaying += bss.getPaidValue();
        }

    }

    public void processUserFinancialTransactionalSummaryByPaymentMethod() {
        reportTemplateType = ReportTemplateType.PAYMENT_METHOD_SUMMARY_USING_BILLS;
        System.out.println("institution = " + institution);
        if (institution == null) {
            setDepartments(null);
        }
        billSummaryRows = null;
        totalPaying = 0.0;
        cashTotal = 0.0;
        cardTotal = 0.0;
        chequeTotal = 0.0;
        slipTotal = 0.0;
        totalOfOtherPayments = 0.0;

        String jpql;
        Map<String, Object> params = new HashMap<>();

        jpql = "select new com.divudi.light.common.BillSummaryRow("
                + "p.paymentMethod, "
                + "sum(p.paidValue), "
                + "count(p.bill)) "
                + "from Payment p "
                + "where p.bill.retired = :ret "
                + "and p.createdAt between :fromDate and :toDate ";

        if (webUser != null) {
            jpql += " and p.bill.creater = :wu";
            params.put("wu", webUser);
        }

        jpql += " group by p.paymentMethod "
                + "order by p.paymentMethod";

        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());
        params.put("ret", false);

        List<?> result = paymentFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        billSummaryRows = new ArrayList<>();
        for (Object obj : result) {
            if (obj instanceof BillSummaryRow) {
                billSummaryRows.add((BillSummaryRow) obj);
            } else {
                System.err.println("Unexpected result type: " + obj.getClass().getName());
            }
        }

        for (BillSummaryRow bss : billSummaryRows) {
            if (bss.getPaymentMethod() == PaymentMethod.Cash) {
                cashTotal += bss.getPaidValue();
            } else if (bss.getPaymentMethod() == PaymentMethod.Card) {
                cardTotal += bss.getPaidValue();
            } else if (bss.getPaymentMethod() == PaymentMethod.Cheque) {
                chequeTotal += bss.getPaidValue();
            } else if (bss.getPaymentMethod() == PaymentMethod.Slip) {
                slipTotal += bss.getPaidValue();
            } else {
                totalOfOtherPayments += bss.getPaidValue();
            }
            totalPaying += bss.getPaidValue();
        }
    }

    private List<Department> departments;

    public void fillInstitutionInDepartment(Institution ins) {
        if (ins == null) {
            setDepartments(null);
        } else {
            setDepartments(getDepartmentController().getInstitutionDepatrments(ins));
        }
    }

    public void processAllFinancialTransactionalSummarybyPaymentMethod() {
        System.out.println("institution = " + institution);
        if (institution == null) {
            setDepartments(null);
        }
        billSummaryRows = null;
        totalPaying = 0.0;
        cashTotal = 0.0;
        cardTotal = 0.0;
        chequeTotal = 0.0;
        slipTotal = 0.0;
        totalOfOtherPayments = 0.0;

        String jpql;
        Map params = new HashMap();
        List<BillTypeAtomic> billTypesToFilter = new ArrayList<>();
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT_REVERSE));

        jpql = "select new com.divudi.light.common.BillSummaryRow("
                + "p.bill.billTypeAtomic, "
                + "sum(p.paidValue), "
                + "count(p.bill), "
                + "p.paymentMethod ) "
                + " from Payment p "
                + " where p.bill.retired=:ret"
                + " and p.createdAt between :fromDate and :toDate "
                + " and p.bill.billTypeAtomic in :abts ";

        if (institution == null) {
            jpql += " and p.bill.institution=:ins";
            params.put("ins", sessionController.getInstitution());
        } else {
            jpql += " and p.bill.institution=:ins";
            params.put("ins", institution);
        }

        if (department == null) {
//            jpql += " and p.bill.department=:dept";
//            params.put("dept", sessionController.getLoggedUser().getDepartment());
        } else {
            jpql += " and p.bill.department=:dept";
            params.put("dept", department);
        }

        jpql += " group by p.paymentMethod, p.bill.billTypeAtomic "
                + " order by p.bill.billTypeAtomic";

        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());
        params.put("ret", false);
        params.put("abts", billTypesToFilter);
        billSummaryRows = paymentFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        for (BillSummaryRow bss : billSummaryRows) {
            if (bss.getPaymentMethod() == PaymentMethod.Cash) {
                cashTotal += bss.getPaidValue();
            } else if (bss.getPaymentMethod() == PaymentMethod.Card) {
                cardTotal += bss.getPaidValue();
            } else if (bss.getPaymentMethod() == PaymentMethod.Cheque) {
                chequeTotal += bss.getPaidValue();
            } else if (bss.getPaymentMethod() == PaymentMethod.Slip) {
                slipTotal += bss.getPaidValue();
            } else {

                totalOfOtherPayments += bss.getPaidValue();
            }
            totalPaying += bss.getPaidValue();
        }

    }

//    public void processAllFinancialTransactionalSummarybyPaymentMethod() {
//        System.out.println("institution = " + institution);
//        if (institution == null) {
//            setDepartments(null);
//        }
//        billSummaryRows = null;
//        grossTotal = 0.0;
//        discount = 0.0;
//        netTotal = 0.0;
//        String jpql;
//        Map params = new HashMap();
//        List<BillTypeAtomic> billTypesToFilter = new ArrayList<>();
//        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
//        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
//        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT));
//        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT_REVERSE));
//
//        jpql = "select new com.divudi.light.common.BillSummaryRow("
//                + "b.billTypeAtomic, "
//                + "sum(b.total), "
//                + "sum(b.discount), "
//                + "sum(b.netTotal), "
//                + "count(b), "
//                + "b.paymentMethod ) "
//                + " from Bill b "
//                + " where b.retired=:ret"
//                + " and b.createdAt between :fromDate and :toDate "
//                + " and b.billTypeAtomic in :abts ";
//
//        if (institution == null) {
//            jpql += " and b.institution=:ins";
//            params.put("ins", sessionController.getInstitution());
//        } else {
//            jpql += " and b.institution=:ins";
//            params.put("ins", institution);
//        }
//
//        if (department == null) {
//            jpql += " and b.department=:dept";
//            params.put("dept", sessionController.getLoggedUser().getDepartment());
//        } else {
//            jpql += " and b.department=:dept";
//            params.put("dept", department);
//        }
//
//        jpql += " group by b.paymentMethod, b.billTypeAtomic "
//                + " order by b.billTypeAtomic";
//
//        params.put("toDate", getToDate());
//        params.put("fromDate", getFromDate());
//        params.put("ret", false);
//        params.put("abts", billTypesToFilter);
//        billSummaryRows = getBillFacade().findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
//
//        for (BillSummaryRow bss : billSummaryRows) {
//            grossTotal += bss.getGrossTotal();
//            discount += bss.getDiscount();
//            netTotal += bss.getNetTotal();
//        }
//
//    }
    public void processAllFinancialTransactionalSummarybyUsers() {
        //System.out.println("institution = " + institution);
        //System.out.println("department = " + department);
        if (institution == null) {
            setDepartments(null);
        }
        billSummaryRows = null;
        grossTotal = 0.0;
        discount = 0.0;
        netTotal = 0.0;
        totalBillCount = 0.0;
        String jpql;
        Map params = new HashMap();
        List<BillTypeAtomic> billTypesToFilter = new ArrayList<>();
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT_REVERSE));

        jpql = "select new com.divudi.light.common.BillSummaryRow("
                + "sum(b.total), "
                + "sum(b.discount), "
                + "sum(b.netTotal), "
                + "count(b), "
                + "b.creater.webUserPerson ) "
                + " from Bill b "
                + " where b.retired=:ret"
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.billTypeAtomic in :abts ";

        if (institution != null) {
            jpql += " and b.institution=:ins";
            params.put("ins", getInstitution());

            if (department != null) {
                jpql += " and b.department=:dept";
                params.put("dept", getDepartment());
            }
        }

        jpql += " group by b.creater "
                + " order by  b.creater";

        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());
        params.put("ret", false);
        params.put("abts", billTypesToFilter);
        billSummaryRows = getBillFacade().findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
        //System.out.println("billSummaryRows = " + billSummaryRows);

        for (BillSummaryRow bss : billSummaryRows) {
            grossTotal += bss.getGrossTotal();
            discount += bss.getDiscount();
            netTotal += bss.getNetTotal();
            totalBillCount += bss.getBillCount();
        }
    }

    public void processAllFinancialTransactionalSummarybyUserPayment() {
        billSummaryRows = null;
        grossTotal = 0.0;
        discount = 0.0;
        netTotal = 0.0;
        totalBillCount = 0.0;
        String jpql;
        Map params = new HashMap();
        List<BillTypeAtomic> billTypesToFilter = new ArrayList<>();
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT_REVERSE));

        jpql = "select new com.divudi.light.common.BillSummaryRow("
                + "sum(b.total), "
                + "sum(b.discount), "
                + "sum(b.netTotal), "
                + "count(b), "
                + "b.paymentMethod,"
                + "b.creater.webUserPerson ) "
                + " from Bill b "
                + " where b.retired=:ret"
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.billTypeAtomic in :abts ";

        if (department != null) {
            jpql += " and b.department=:dept";
            params.put("dept", getDepartment());
        }

        jpql += " group by b.paymentMethod, b.creater.webUserPerson"
                + " order by b.creater.webUserPerson";

        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());
        params.put("ret", false);
        params.put("abts", billTypesToFilter);

        billSummaryRows = getBillFacade().findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        for (BillSummaryRow bss : billSummaryRows) {
            grossTotal += bss.getGrossTotal();
            discount += bss.getDiscount();
            netTotal += bss.getNetTotal();
            totalBillCount += bss.getBillCount();
        }
    }

    public void processAllFinancialTransactionalBillListBySingleUserByIds() {
        if (startBillId == null && endBillId == null) {
            JsfUtil.addErrorMessage("Enter at leat on bill number");
            return;
        }
        if (webUser == null) {
            webUser = sessionController.getLoggedUser();
        }
        billSummaryRows = null;
        grossTotal = 0.0;
        discount = 0.0;
        netTotal = 0.0;
        totalBillCount = 0.0;
        String jpql;
        Map params = new HashMap();
        List<BillTypeAtomic> billTypesToFilter = new ArrayList<>();
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT_REVERSE));

        jpql = "select b "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.creater=:wu ";

        if (billTypeAtomic == null) {
            jpql += "  and b.billTypeAtomic in :abts  ";
            params.put("abts", billTypesToFilter);
        } else {
            jpql += "  and b.billTypeAtomic=:abt  ";
            params.put("abt", billTypeAtomic);
        }

        if (startBillId != null) {
            jpql += " and b.id > :sid ";
            params.put("sid", startBillId);
        }
        if (endBillId != null) {
            jpql += " and b.id < :eid ";
            params.put("eid", endBillId);
        }

        if (paymentMethod != null) {
            jpql += " and b.paymentMethod=:pm ";
            params.put("pm", paymentMethod);
        }
        if (paymentMethods != null) {
            jpql += " and b.paymentMethod in :pms ";
            params.put("pms", paymentMethods);
        }
        jpql += " order by b.paymentMethod, b.billTypeAtomic ";

        params.put("wu", webUser);
        params.put("ret", false);

        System.out.println("jpql = " + jpql);
        System.out.println("params = " + params);

        bills = getBillFacade().findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        System.out.println("billSummaryRows = " + bills);

        for (Bill bss : bills) {
            grossTotal += bss.getTotal();
            discount += bss.getDiscount();
            netTotal += bss.getNetTotal();
            totalBillCount++;
        }
    }

    public void processAllFinancialTransactionalSummarybySingleUserByIds() {
        if (startBillId == null && endBillId == null) {
            JsfUtil.addErrorMessage("Enter at leat on bill number");
            return;
        }
        if (webUser == null) {
            webUser = sessionController.getLoggedUser();
        }
        billSummaryRows = null;
        grossTotal = 0.0;
        discount = 0.0;
        netTotal = 0.0;
        totalBillCount = 0.0;
        String jpql;
        Map params = new HashMap();
        List<BillTypeAtomic> billTypesToFilter = new ArrayList<>();
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT_REVERSE));

        jpql = "select new com.divudi.light.common.BillSummaryRow("
                + "b.billTypeAtomic, "
                + "sum(b.total), "
                + "sum(b.discount), "
                + "sum(b.netTotal), "
                + "count(b),"
                + "b.paymentMethod "
                + ") "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.creater=:wu "
                + " and b.billTypeAtomic in :abts ";

        if (startBillId != null) {
            jpql += " and b.id > :sid ";
            params.put("sid", startBillId);
        }
        if (endBillId != null) {
            jpql += " and b.id < :eid ";
            params.put("eid", endBillId);
        }

        if (paymentMethod != null) {
            jpql += " and b.paymentMethod=:pm ";
            params.put("pm", paymentMethod);
        }
        if (paymentMethods != null) {
            jpql += " and b.paymentMethod in :pms ";
            params.put("pms", paymentMethods);
        }
        jpql += " group by b.paymentMethod, b.billTypeAtomic "
                + " order by b.creater.webUserPerson";

        params.put("wu", webUser);
        params.put("ret", false);
        params.put("abts", billTypesToFilter);

        System.out.println("jpql = " + jpql);
        System.out.println("params = " + params);

        billSummaryRows = getBillFacade().findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        System.out.println("billSummaryRows = " + billSummaryRows);

        for (BillSummaryRow bss : billSummaryRows) {
            grossTotal += bss.getGrossTotal();
            discount += bss.getDiscount();
            netTotal += bss.getNetTotal();
            totalBillCount += bss.getBillCount();
        }
    }
    
    
    public void processAllFinancialTransactionalSummarybySingleUserByIdsAdmin() {
        if (startBillId == null && endBillId == null) {
            JsfUtil.addErrorMessage("Enter at leat on bill number");
            return;
        }
        if (webUser == null) {
            webUser = sessionController.getLoggedUser();
        }
        billSummaryRows = null;
        grossTotal = 0.0;
        discount = 0.0;
        netTotal = 0.0;
        totalBillCount = 0.0;
        String jpql;
        Map params = new HashMap();
        List<BillTypeAtomic> billTypesToFilter = new ArrayList<>();
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT_REVERSE));

        jpql = "select new com.divudi.light.common.BillSummaryRow("
                + "b.billTypeAtomic, "
                + "sum(b.total), "
                + "sum(b.discount), "
                + "sum(b.netTotal), "
                + "count(b),"
                + "b.paymentMethod "
                + ") "
                + " from Bill b "
                + " where b.retired=:ret "
                + " and b.creater=:wu "
                + " and b.billTypeAtomic in :abts ";

        if (startBillId != null) {
            jpql += " and b.id > :sid ";
            params.put("sid", startBillId);
        }
        if (endBillId != null) {
            jpql += " and b.id < :eid ";
            params.put("eid", endBillId);
        }

        if (paymentMethod != null) {
            jpql += " and b.paymentMethod=:pm ";
            params.put("pm", paymentMethod);
        }
        if (paymentMethods != null) {
            jpql += " and b.paymentMethod in :pms ";
            params.put("pms", paymentMethods);
        }
        jpql += " group by b.paymentMethod, b.billTypeAtomic "
                + " order by b.creater.webUserPerson";

        params.put("wu", webUser);
        params.put("ret", false);
        params.put("abts", billTypesToFilter);

        System.out.println("jpql = " + jpql);
        System.out.println("params = " + params);

        billSummaryRows = getBillFacade().findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        System.out.println("billSummaryRows = " + billSummaryRows);

        for (BillSummaryRow bss : billSummaryRows) {
            grossTotal += bss.getGrossTotal();
            discount += bss.getDiscount();
            netTotal += bss.getNetTotal();
            totalBillCount += bss.getBillCount();
        }
    }

    public void processAllFinancialTransactionalSummarybyDepartment() {
        billSummaryRows = null;
        grossTotal = 0.0;
        discount = 0.0;
        netTotal = 0.0;
        totalBillCount = 0.0;

        String jpql;
        Map params = new HashMap();
        List<BillTypeAtomic> billTypesToFilter = new ArrayList<>();
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT_REVERSE));

        jpql = "select new com.divudi.light.common.BillSummaryRow("
                + "b.billTypeAtomic, "
                + "sum(b.total), "
                + "sum(b.discount), "
                + "sum(b.netTotal),"
                + "count(b)) "
                + " from Bill b "
                + " where b.retired=:ret"
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.billTypeAtomic in :abts "
                + " and b.department=:dept"
                + " group by b.billTypeAtomic"
                + " order by b.billTypeAtomic ";
//        Bill b = new Bill();
//        b.getTotal();
//        b.getDiscount();
//        b.getNetTotal();
//        b.getBillTypeAtomic();
        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());
        params.put("ret", false);
        params.put("abts", billTypesToFilter);
        params.put("dept", getDepartment());
        billSummaryRows = getBillFacade().findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);

        for (BillSummaryRow bss : billSummaryRows) {
            grossTotal += bss.getGrossTotal();
            discount += bss.getDiscount();
            netTotal += bss.getNetTotal();
            totalBillCount += bss.getBillCount();
        }

    }

    public String fillAllBills(Date fromDate, Date toDate, Institution institution, Department department, PaymentMethod paymentMethod, BillTypeAtomic billtypeAtomic) {
        bills = null;
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder("select b from Bill b where 1=1 ");

        // Handle date filtering
        if (toDate != null && fromDate != null) {
            jpql.append(" and b.createdAt between :fromDate and :toDate ");
            params.put("toDate", toDate);
            params.put("fromDate", fromDate);
        }

        // Include BillTypeAtomic filtering
        List<BillTypeAtomic> billTypesToFilter = new ArrayList<>();
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_IN));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CASH_OUT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT));
        billTypesToFilter.addAll(BillTypeAtomic.findByFinanceType(BillFinanceType.CREDIT_SETTLEMENT_REVERSE));
        if (!billTypesToFilter.isEmpty()) {
            jpql.append(" and b.billTypeAtomic in :abts ");
            params.put("abts", billTypesToFilter);
        }

        // Institution and department filtering
        if (institution != null) {
            jpql.append(" and b.institution = :ins ");
            params.put("ins", institution);
            if (department != null) {
                jpql.append(" and b.department = :dept ");
                params.put("dept", department);
            }
        }

        if (paymentMethod != null) {
            jpql.append(" and b.paymentMethod=:pm ");
            params.put("pm", paymentMethod);
        }

        if (billtypeAtomic != null) {
            jpql.append(" and b.billTypeAtomic=:ba ");
            params.put("ba", billtypeAtomic);
        }

        if (webUser != null) {
            jpql.append(" and b.creater=:wu ");
            params.put("wu", webUser);
        }

        // Order by bill ID
        jpql.append(" order by b.id ");

        System.out.println("jpql.toString() = " + jpql.toString());
        System.out.println("params = " + params);

        // Execute the query
        bills = getBillFacade().findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        System.out.println("bills = " + bills);
        return "/analytics/all_bills";
    }

    public void fillAllBills() {
        bills = null;
        String jpql;
        Map params = new HashMap();
        jpql = "select b from "
                + " Bill b "
                + " where b.createdAt between :fromDate and :toDate "
                + " order by b.id ";
        params.put("toDate", getToDate());
        params.put("fromDate", getFromDate());
        bills = getBillFacade().findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public void fillOtherInstitutionBills() {
        bills = null;
        if (otherInstitution == null) {
            JsfUtil.addErrorMessage("Select other Institution");
            return;
        }
        String jpql;
        Map m = new HashMap();

        jpql = "select b from "
                + " Bill b "
                + " where b.createdAt between :fromDate and :toDate ";
        jpql += " and b.institution in :ins ";
        m.put("ins", sessionController.getLoggableInstitutions());

        jpql += " and (b.fromInstitution=:oi or b.toInstitution=:oi or b.bank=:oi or b.referenceInstitution=:oi or b.creditCompany=:oi and b.collectingCentre=:oi or b.paymentSchemeInstitution=:oi or b.referredByInstitution=:oi ) ";
        m.put("oi", otherInstitution);

        jpql += " order by b.id ";
        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        bills = getBillFacade().findByJpql(jpql, m, TemporalType.TIMESTAMP);
    }

    public String getInstitutionType(Bill bill) {
        if (bill == null || otherInstitution == null) {
            return "";
        }
        if (bill.getFromInstitution() != null && bill.getFromInstitution().equals(otherInstitution)) {
            return "From Institution";
        } else if (bill.getToInstitution() != null && bill.getToInstitution().equals(otherInstitution)) {
            return "To Institution";
        } else if (bill.getBank() != null && bill.getBank().equals(otherInstitution)) {
            return "Bank";
        } else if (bill.getReferenceInstitution() != null && bill.getReferenceInstitution().equals(otherInstitution)) {
            return "Reference Institution";
        } else if (bill.getCreditCompany() != null && bill.getCreditCompany().equals(otherInstitution)) {
            return "Credit Company";
        } else if (bill.getCollectingCentre() != null && bill.getCollectingCentre().equals(otherInstitution)) {
            return "Collecting Centre";
        } else if (bill.getPaymentSchemeInstitution() != null && bill.getPaymentSchemeInstitution().equals(otherInstitution)) {
            return "Payment Scheme Institution";
        } else if (bill.getReferredByInstitution() != null && bill.getReferredByInstitution().equals(otherInstitution)) {
            return "Referred By Institution";
        }
        return "";
    }

    public String viewOPD(Bill b) {
        if (b.getBillType() == BillType.OpdBill) {
            return "/opd/bill_reprint?faces-redirect=true;";
        } else {
            JsfUtil.addErrorMessage("Please Search Again and View Bill");
            bills = new ArrayList<>();
            return "";
        }
    }

    public String viewOPDBillById(Long billId) {
        if (billId == null) {
            return null;
        }
        Bill tb = getBillFacade().find(billId);
        if (tb == null) {
            return null;
        }
        //System.out.println("tb = " + tb);
        bill = tb;
        //System.out.println("bill = " + bill);
        return "/opd/bill_reprint?faces-redirect=true";
    }

    public void createTableCashIn() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false"
                + " and b.creater=:w ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  ((b.fromWebUser.webUserPerson.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.CashIn);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("w", getSessionController().getLoggedUser());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createTableCashOut() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false"
                + " and b.creater=:w ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  ((b.toWebUser.webUserPerson.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.CashOut);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("w", getSessionController().getLoggedUser());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createSearchBill() {
        if (getSearchKeyword().getInsId() == null && getSearchKeyword().getDeptId() == null
                && getSearchKeyword().getBhtNo() == null && getSearchKeyword().getRefBillNo() == null) {
            JsfUtil.addErrorMessage("Enter BHT No or Bill No");
            return;
        }
        bills = null;
        String sql;
        Map m = new HashMap();

        sql = "select b from Bill b where "
                + " b.id is not null ";

        if (!getSearchKeyword().getInsId().isEmpty()) {
            sql += " and (b.insId=:insId or b.deptId=:insId)  ";
            m.put("insId", getSearchKeyword().getInsId());
        }

        if (!getSearchKeyword().getDeptId().isEmpty()) {
            sql += " and (b.insId=:deptId or b.deptId=:deptId)  ";
            m.put("deptId", getSearchKeyword().getDeptId());
        }

        if (!getSearchKeyword().getBhtNo().trim().isEmpty()) {
            sql += " and b.patientEncounter.bhtNo=:bht";
            m.put("bht", getSearchKeyword().getBhtNo());
        }
        if (getSearchKeyword().getRefBillNo() != null) {
            try {
                long l = Long.parseLong(getSearchKeyword().getRefBillNo());
                sql += " and b.id=:id";
                m.put("id", l);
            } catch (Exception e) {
            }

        }
        sql += " order by b.insId ";
//        m.put("class", PreBill.class);
        bills = getBillFacade().findByJpql(sql, m, 5000);
    }

    public void createSearchAll() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where "
                + " b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getInstitution() != null && !getSearchKeyword().getInstitution().trim().equals("")) {
            sql += " and  ((b.institution.name) like :ins )";
            temMap.put("ins", "%" + getSearchKeyword().getInstitution().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  ((b.toInstitution.name) like :toIns )";
            temMap.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getToDepartment() != null && !getSearchKeyword().getToDepartment().trim().equals("")) {
            sql += " and  ((b.toDepartment.name) like :toDept )";
            temMap.put("toDept", "%" + getSearchKeyword().getToDepartment().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  ((b.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getFromDepartment() != null && !getSearchKeyword().getFromDepartment().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getFromDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPaymentScheme() != null && !getSearchKeyword().getPaymentScheme().trim().equals("")) {
            sql += " and  ((b.paymentScheme.name) like :pScheme )";
            temMap.put("pScheme", "%" + getSearchKeyword().getPaymentScheme().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPaymentmethod() != null && !getSearchKeyword().getPaymentmethod().trim().equals("")) {
            sql += " and  ((b.paymentMethod) like :pm )";
            temMap.put("pm", "%" + getSearchKeyword().getPaymentmethod().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getInsId() != null && !getSearchKeyword().getInsId().trim().equals("")) {
            sql += " and  ((b.insId) like :insId )";
            temMap.put("insId", "%" + getSearchKeyword().getInsId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDeptId() != null && !getSearchKeyword().getDeptId().trim().equals("")) {
            sql += " and  ((b.insId) like :deptId )";
            temMap.put("deptId", "%" + getSearchKeyword().getDeptId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createCollectingTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where "
                + " b.billType = :billType and "
                + " b.institution=:ins "
                + " and b.createdAt between :fromDate "
                + " and :toDate and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.LabBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createCollectingTableAll() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.LabBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createCollectingBillItemTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select bi from BillItem bi join bi.bill b where "
                + " b.billType = :billType and "
                + " b.institution=:ins "
                + " and b.createdAt between :fromDate "
                + " and :toDate and b.retired=false "
                + " and type(b)=:class ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((bi.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.LabBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap
                .put("class", BilledBill.class
                );

        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);
        ////System.out.println("billItems = " + billItems);

    }

    public void createCollectingBillItemTableAll() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select bi from BillItem bi join bi.bill b where "
                + " b.billType = :billType and "
                + " b.institution=:ins "
                + " and b.createdAt between :fromDate "
                + " and :toDate and b.retired=false "
                + " and type(b)=:class ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((bi.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.LabBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap
                .put("class", BilledBill.class
                );

        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        ////System.out.println("billItems = " + billItems);

    }

    public void createCreditTable() {
        Date startTime = new Date();

        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b "
                + " where b.billType = :billType"
                + "  and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  ((b.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBank() != null && !getSearchKeyword().getBank().trim().equals("")) {
            sql += " and  ((b.bank.name) like :bank )";
            temMap.put("bank", "%" + getSearchKeyword().getBank().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  ((b.chequeRefNo) like :num )";
            temMap.put("num", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.CashRecieveBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createCreditTableBillItemAll() {
        Date startTime = new Date();

        createCreditTableBillItem(null, true);

    }

    public void createCreditTableBillItemOpd() {
        Date startTime = new Date();
        createCreditTableBillItem(BillType.OpdBill, false);

    }

    public void createCreditTableBillItemBht() {
        Date startTime = new Date();
        createCreditTableBillItem(null, false);

    }

    public void createCreditTableBillItem(BillType billType, boolean all) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillItem b "
                + " where b.bill.billType = :billType"
                + "  and b.bill.institution=:ins "
                + " and b.bill.createdAt between :fromDate and :toDate "
                + " and b.bill.retired=false ";

        if (!all) {
            if (billType != null) {
                sql += " and b.referenceBill.billType=:refBtp";
                temMap.put("refBtp", billType);
            } else {
                sql += " and b.patientEncounter is not null ";
            }

        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }
        if (billType != null) {
            if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
                sql += " and  ((b.referenceBill.insId) like :reBillNo )";
                temMap.put("reBillNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
            }
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.bill.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  ((b.bill.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBank() != null && !getSearchKeyword().getBank().trim().equals("")) {
            sql += " and  ((b.bill.bank.name) like :bank )";
            temMap.put("bank", "%" + getSearchKeyword().getBank().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  ((b.bill.chequeRefNo) like :num )";
            temMap.put("num", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.bill.createdAt desc  ";
//    
        temMap.put("billType", BillType.CashRecieveBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public List<Bill> getChannelPaymentBillsOld() {
        if (bills == null) {
            String sql;
            Map temMap = new HashMap();
            if (bills == null) {
                sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.id in"
                        + "(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType=:btp"
                        + " or bt.referenceBill.billType=:btp2) and b.billType=:type and b.createdAt "
                        + "between :fromDate and :toDate order by b.id";

                temMap.put("toDate", getToDate());
                temMap.put("fromDate", getFromDate());
                temMap.put("type", BillType.PaymentBill);
                temMap.put("btp", BillType.ChannelPaid);
                temMap.put("btp2", BillType.ChannelCredit);
                bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);

                if (bills == null) {
                    bills = new ArrayList<>();
                }
            }
        }
        return bills;

    }

    public void channelPaymentBills() {
        Date startTime = new Date();

        String sql;
        Map m = new HashMap();

        BillType[] bt = {
            BillType.ChannelOnCall,
            BillType.ChannelCash,
            BillType.ChannelAgent,
            BillType.ChannelStaff,
            BillType.ChannelCredit,};

        List<BillType> bts = Arrays.asList(bt);

        sql = "SELECT bi FROM BillItem bi WHERE bi.retired = false "
                + " and bi.bill.billType=:bt "
                + " and type(bi.bill)=:class "
                + " and bi.paidForBillFee.bill.billType in :bts "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((bi.paidForBillFee.bill.patientEncounter.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((bi.paidForBillFee.bill.insId) like :billNo or (bi.paidForBillFee.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getInsId() != null && !getSearchKeyword().getInsId().trim().equals("")) {
            sql += " and  ((bi.bill.insId) like :insId )";
            m.put("insId", "%" + getSearchKeyword().getInsId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  ((bi.paidForBillFee.staff.speciality.name) like :special )";
            m.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((bi.paidForBillFee.staff.person.name) like :staff )";
            m.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((bi.paidForBillFee.feeValue) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by bi.bill.createdAt desc  ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("bt", BillType.PaymentBill);
        m.put("bts", bts);
        m
                .put("class", BilledBill.class
                );

        if (getReportKeyWord().isAdditionalDetails()) {
            billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        } else {
            billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);
        }

    }

    public void channelPaymentBillsNew() {
        Date startTime = new Date();

        String sql;
        Map m = new HashMap();

        BillType[] bt = {
            BillType.ChannelOnCall,
            BillType.ChannelCash,
            BillType.ChannelAgent,
            BillType.ChannelStaff,
            BillType.ChannelPaid,
            BillType.ChannelCredit,};

        List<BillType> bts = Arrays.asList(bt);

        sql = "SELECT bi FROM BillItem bi WHERE bi.retired = false "
                + " and bi.bill.billType=:bt "
                + " and type(bi.bill)=:class "
                + " and bi.paidForBillFee.bill.billType in :bts "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((bi.paidForBillFee.bill.patientEncounter.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((bi.paidForBillFee.bill.insId) like :billNo or (bi.paidForBillFee.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getInsId() != null && !getSearchKeyword().getInsId().trim().equals("")) {
            sql += " and  ((bi.bill.insId) like :insId )";
            m.put("insId", "%" + getSearchKeyword().getInsId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  ((bi.paidForBillFee.staff.speciality.name) like :special )";
            m.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((bi.paidForBillFee.staff.person.name) like :staff )";
            m.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((bi.paidForBillFee.feeValue) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by bi.bill.createdAt desc  ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("bt", BillType.ChannelProPayment);
        m.put("bts", bts);
        m
                .put("class", BilledBill.class
                );
        if (getReportKeyWord().isAdditionalDetails()) {
            billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        } else {
            billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);
        }

    }

    public void channelAgentPaymentBills() {
        Date startTime = new Date();

        String sql;
        Map m = new HashMap();

        sql = "SELECT bi FROM BillItem bi WHERE bi.retired = false "
                + " and bi.bill.billType=:bt"
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getIns() != null) {
            sql += " and bi.bill.toInstitution=:ins";
            m.put("ins", getSearchKeyword().getIns());
        }

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("bt", BillType.ChannelAgencyPayment);
        billItems = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public void createChannelDueBillFeeOld() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where b.retired=false and (b.bill.billType=:btp or b.bill.billType=:btp2) "
                + " and b.bill.id in(Select bs.bill.id From BillSession bs where bs.retired=false ) "
                + "and b.bill.cancelled=false and (b.feeValue - b.paidValue) > 0 and  "
                + "b.bill.createdAt between :fromDate and :toDate order by b.staff.id  ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.ChannelPaid);
        temMap.put("btp2", BillType.ChannelCredit);

        billFees = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createChannelDueBillFee() {
        Date startTime = new Date();

        selectedServiceSession = null;

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        String sql = " SELECT b FROM BillFee b "
                + "  where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.bill.refunded=false "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt ";

        HashMap hm = new HashMap();
        if (getFromDate() != null && getToDate() != null) {
            sql += " and b.bill.appointmentAt between :frm and  :to";
            hm.put("frm", getFromDate());
            hm.put("to", getToDate());
        }

        if (getSelectedServiceSession() != null) {
            sql += " and bs.serviceSession=:ss";
            hm.put("ss", getSelectedServiceSession());
        }

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.bill.patient.person.name) like :patientName )";
            hm.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.bill.insId) like :billNo )";
            hm.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.feeValue) like :total )";
            hm.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  ((b.staff.speciality.name) like :special )";
            hm.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.staff.person.name) like :staff )";
            hm.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        sql += " order by b.speciality.name ";

        //hm.put("ins", sessionController.getInstitution());
        hm.put("bt", bts);
        hm.put("ftp", FeeType.Staff);
        hm
                .put("class", BilledBill.class
                );
        if (getReportKeyWord().isAdditionalDetails()) {
            billFees = billFeeFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
        } else {
            billFees = billFeeFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP, 50);
        }

    }

    public void createChannelDueBillFeeByAgent() {
        selectedServiceSession = null;

        //BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        // List<BillType> bts = Arrays.asList(billTypes);
        String sql = " SELECT b FROM BillFee b "
                + "  where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.bill.refunded=false "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt ";

        HashMap hm = new HashMap();
        if (getFromDate() != null && getToDate() != null) {
            sql += " and b.bill.appointmentAt between :frm and  :to";
            hm.put("frm", getFromDate());
            hm.put("to", getToDate());
        }

        if (getSelectedServiceSession() != null) {
            sql += " and bs.serviceSession=:ss";
            hm.put("ss", getSelectedServiceSession());
        }

//        if (getCurrentStaff() != null) {
//            sql += " and b.staff=:stf ";
//            hm.put("stf", getCurrentStaff());
//        }
        //hm.put("ins", sessionController.getInstitution());
        hm.put("bt", BillType.ChannelAgent);
        hm.put("ftp", FeeType.OtherInstitution);
        hm
                .put("class", BilledBill.class
                );
        billFees = billFeeFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public void createChannelAgencyPaymentTable() {
        Date startTime = new Date();

        createAgentPaymentTable(BillType.AgentPaymentReceiveBill);

    }

    public void createChannelAgencyCreditNoteTable() {

        createAgentPaymentTable(BillType.AgentCreditNoteBill);

    }

    public void createChannelAgencyDebitNoteTable() {

        createAgentPaymentTable(BillType.AgentDebitNoteBill);

    }

    public void createCollectingCenterCreditNoteTable() {

        createAgentPaymentTable(BillType.CollectingCentreCreditNoteBill);

    }

    public void createCollectingCenterDebitNoteTable() {

        createAgentPaymentTable(BillType.CollectingCentreDebitNoteBill);

    }

    public void createCollectingCentrePaymentTable() {
        Date startTime = new Date();
        createAgentPaymentTable(BillType.CollectingCentrePaymentReceiveBill);

    }

    public void createAgentPaymentTable(BillType billType) {
        bills = new ArrayList<>();
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType "
                + " and b.institution=:ins and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  ((b.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  ((b.fromInstitution.institutionCode) like :num )";
            temMap.put("num", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("billType", billType);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardServiceTable() {
        Date startTime = new Date();

        String sql;
        Map temMap = new HashMap();
        sql = "select (b.bill) from BillItem b where "
                + " b.bill.billType = :billType "
                + " and type(b.bill)=:class "
                + " and b.bill.createdAt between :fromDate and :toDate"
                + " and b.bill.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.bill.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.bill.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by b.bill.insId desc ";
        temMap.put("billType", BillType.InwardBill);
        temMap
                .put("class", BilledBill.class
                );
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createInwardServiceTablebyLoggedDepartment() {
        Date startTime = new Date();

        String sql;
        Map temMap = new HashMap();
        sql = "select (b.bill) from BillItem b where "
                + " b.bill.billType = :billType "
                + " and b.bill.createdAt between :fromDate and :toDate"
                + " and b.bill.retired=false  "
                + " and b.bill.department = :dep";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.bill.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.bill.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by b.bill.insId desc ";
        temMap.put("dep", getSessionController().getDepartment());
        temMap.put("billType", BillType.InwardBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createInwardServiceTableDischarged() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.createdAt is not null "
                + " and b.patientEncounter.discharged=true and"
                + " b.billType = :billType and b.createdAt between :fromDate and :toDate "
                + "and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += "order by b.insId desc";

        temMap.put("billType", BillType.InwardBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardFinalBillsCheck() {
        Date startTime = new Date();

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where"
                + " b.billType = :billType and "
                + " b.patientEncounter.dateOfDischarge between :fromDate and :toDate "
                + "and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardFinalBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardFinalBills() {
//        double d = commonController.dateDifferenceInMinutes(fromDate, toDate) / (60 * 24);
//        if (d > 32 && getReportKeyWord().isBool1()) {
//            JsfUtil.addErrorMessage("Date Range To Long");
//            return;
//        }
        Date startTime = new Date();

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where"
                + " b.billType = :billType and "
                + " b.createdAt between :fromDate and :toDate "
                + "and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardFinalBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

//        if (getReportKeyWord().isBool1()) {
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//        } else {
//            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);
//        }

    }

    public void createCancelledInwardFinalBills() {
        Date startTime = new Date();
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where"
                + " b.billType = :billType and "
                + " b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.cancelled=true ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardFinalBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

//        if (getReportKeyWord().isBool1()) {
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//        } else {
//            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);
//        }

    }

    public void createInwardIntrimBills() {
        Date startTime = new Date();

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where"
                + " b.billType = :billType and "
                + " b.createdAt between :fromDate and :toDate "
                + "and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardIntrimBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardPaymentBills() {
        Date startTime = new Date();

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where"
                + " b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardRefundBills() {
        Date startTime = new Date();

        String sql;
        Map temMap = new HashMap();
        sql = "select b from RefundBill b where "
                + " b.billType = :billType "
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void searchSurgery() {
        Date startTime = new Date();

        if (searchKeyword.isActiveAdvanceOption() && searchKeyword.getItem() == null && searchKeyword.getItemName().equals("")) {
            JsfUtil.addErrorMessage("You Need To select Surgury to Search All");
            return;
        }

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where "
                + " b.billType = :billType and "
                + " b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.staff.person.name) like :staffName )";
            temMap.put("staffName", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null
                && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.procedure.item.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItem() != null) {
            sql += " and b.procedure.item=:item ";
            temMap.put("item", getSearchKeyword().getItem());
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("billType", BillType.SurgeryBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        if (searchKeyword.isActiveAdvanceOption()) {
            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        } else {
            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);
        }

    }

    public void createInwardSurgeryBills() {
        Date startTime = new Date();

        if (searchKeyword.isActiveAdvanceOption() && searchKeyword.getItem() == null && searchKeyword.getItemName().equals("")) {
            JsfUtil.addErrorMessage("You Need To select Surgury to Search All");
            return;
        }

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where "
                + " b.billType = :billType and "
                + " b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getStaffName() != null
                && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.staff.person.name) like :staffName )";
            temMap.put("staffName", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientName() != null
                && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null
                && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null
                && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null
                && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null
                && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.procedure.item.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItem() != null) {
            sql += " and b.procedure.item=:item ";
            temMap.put("item", getSearchKeyword().getItem());
        }

        if (getSearchKeyword().getTotal() != null
                && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.SurgeryBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        if (searchKeyword.isActiveAdvanceOption()) {
            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        } else {
            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);
        }

    }

    public void createInwardSurgeryBillsReport() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where "
                + " b.billType = :billType and "
                + " b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getStaffName() != null
                && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.staff.person.name) like :staffName )";
            temMap.put("staffName", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientName() != null
                && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null
                && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null
                && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null
                && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null
                && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.procedure.item.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null
                && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  ((b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.SurgeryBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createInwardPaymentBillsDischarged() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where  "
                + " and b.patientEncounter.discharged=true"
                + " and b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += "order by b.insId desc";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardProBills() {
        Date startTime = new Date();

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where "
                + " b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc";

        temMap.put("billType", BillType.InwardProfessional);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createInwardProBillsDischarged() {

        String sql;
        Map temMap = new HashMap();
//        sql = "select b from BilledBill b where b.createdAt is not null and b.billType = :billType and b.patientEncounter.discharged=true and "
//                + " b.id in(Select bf.bill.id From BillFee bf where bf.retired=false and bf.createdAt between :fromDate and :toDate and bf.billItem is null)"
//                + " and b.createdAt between :fromDate and :toDate and b.retired=false";

        sql = "select b from BilledBill b where b.createdAt is not null "
                + " and b.patientEncounter.discharged=true and"
                + " b.billType = :billType and b.createdAt between :fromDate and :toDate "
                + "and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  ((b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  ((b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += "order by b.insId desc";

        temMap.put("billType", BillType.InwardBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createPettyTable() {
        List<BillType> billTypes = new ArrayList<>();
        billTypes.add(BillType.PettyCash);
        billTypes.add(BillType.IouIssue);

        Date startTime = new Date();

        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType IN :billTypes and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.staff.person.name) like :stf )";
            temMap.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  ((b.person.name) like :per )";
            temMap.put("per", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  ((b.invoiceNumber) like :num )";
            temMap.put("num", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    

        temMap.put("billTypes", billTypes);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createIncomeBillTable() {
        fetchBillTable(BillType.ChannelIncomeBill);
    }

    public void createExpensesBillTable() {
        fetchBillTable(BillType.ChannelExpenesBill);
    }

    public void fetchBillTable(BillType billType) {
        Date startTime = new Date();

        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType =:billType "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  ((b.staff.person.name) like :stf )";
            temMap.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  ((b.person.name) like :per )";
            temMap.put("per", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  ((b.invoiceNumber) like :num )";
            temMap.put("num", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", billType);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createAllBillContacts() {
        String sql;
        Map temMap = new HashMap();
        telephoneNumbers = new ArrayList<>();

        if (getReportKeyWord().getString1().equals("0")) {
            sql = "select b.patient.person.phone from Bill b where ";
        } else {
            sql = "select b from Bill b where ";
        }

        sql += " b.retired = false "
                + " and b.cancelled=false "
                + " and b.refunded=false "
                + " and (b.patient.person.phone is not null "
                + " or b.patient.person.phone!=:em) "
                + " and b.createdAt between :fd and :td  ";

        if (getReportKeyWord().getString().equals("0")) {
        }
        if (getReportKeyWord().getString().equals("1")) {
            BillType[] billTypes = {BillType.ChannelCash, BillType.ChannelPaid};
            sql += " and b.billType in :bts ";
            temMap.put("bts", Arrays.asList(billTypes));
        }
        if (getReportKeyWord().getString().equals("2")) {
            BillType[] billTypes = {BillType.OpdBill};
            sql += " and b.billType in :bts ";
            temMap.put("bts", Arrays.asList(billTypes));
        }
        if (getReportKeyWord().getString().equals("3")) {
            BillType[] billTypes = {BillType.PharmacySale};
            sql += " and b.billType in :bts ";
            temMap.put("bts", Arrays.asList(billTypes));
        }
        if (getReportKeyWord().getArea() != null) {
            sql += " and b.patient.person.area=:a ";
            temMap.put("a", getReportKeyWord().getArea());
        }

        if (getReportKeyWord().getString1().equals("0")) {
            sql += " group by b.patient.person.phone ";
        }
        sql += " order by b.patient.person.phone ";

        temMap.put("em", "");
        temMap.put("fd", fromDate);
        temMap.put("td", toDate);

        if (getReportKeyWord().getString1().equals("0")) {
            List<Object> objs = getBillFacade().findObjectByJpql(sql, temMap, TemporalType.TIMESTAMP);

            for (Object o : objs) {
                String s = (String) o;
                if (s != null && !"".equals(s)) {
                    String ss = s.substring(0, 3);
//                //System.out.println("ss = " + ss);
                    if (ss.equals("077") || ss.equals("076")
                            || ss.equals("071") || ss.equals("072")
                            || ss.equals("075") || ss.equals("078")) {
                        telephoneNumbers.add(s);
                    }

                }
            }
        } else {
            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
            for (Bill b : bills) {
                if (b.getPatient().getPerson().getPhone() != null && !"".equals(b.getPatient().getPerson().getPhone())) {
                    String ss = b.getPatient().getPerson().getPhone().substring(0, 3);
                    if (getReportKeyWord().getString1().equals("1")) {
                        if (b.getPatient().getAgeYears() <= getReportKeyWord().getFrom()) {
                            if (ss.equals("077") || ss.equals("076")
                                    || ss.equals("071") || ss.equals("072")
                                    || ss.equals("075") || ss.equals("078")) {
                                telephoneNumbers.add(b.getPatient().getPerson().getPhone());
                            }
                        }
                    }
                    if (getReportKeyWord().getString1().equals("2")) {
                        if (b.getPatient().getAgeYears() >= getReportKeyWord().getTo()) {
                            if (b.getPatient().getAgeYears() <= getReportKeyWord().getFrom()) {
                                if (ss.equals("077") || ss.equals("076")
                                        || ss.equals("071") || ss.equals("072")
                                        || ss.equals("075") || ss.equals("078")) {
                                    telephoneNumbers.add(b.getPatient().getPerson().getPhone());
                                }
                            }
                        }
                    }
                    if (getReportKeyWord().getString1().equals("3")) {
                        if (b.getPatient().getAgeYears() >= getReportKeyWord().getFrom()
                                && b.getPatient().getAgeYears() <= getReportKeyWord().getTo()) {
                            if (b.getPatient().getAgeYears() <= getReportKeyWord().getFrom()) {
                                if (ss.equals("077") || ss.equals("076")
                                        || ss.equals("071") || ss.equals("072")
                                        || ss.equals("075") || ss.equals("078")) {
                                    telephoneNumbers.add(b.getPatient().getPerson().getPhone());
                                }
                            }
                        }
                    }
                }
            }
        }

    }

//    public void sendSms() {
//        smsController.sendSmsToNumberList(uniqueSmsText, getSessionController().getApplicationPreference().getApplicationInstitution(), smsText, null, MessageType.Marketing);
//    }
//    public void sendSmsAll() {
//        if (selectedTelephoneNumbers == null) {
//            JsfUtil.addErrorMessage("Please Select Numbers");
//            return;
//        }
//        if (selectedTelephoneNumbers.size() > 10000) {
//            JsfUtil.addErrorMessage("Please Contact System Development Team.You are trying to send more than 10,000 sms.");
//            return;
//        }
//        if (smsText.equals("") || smsText == null) {
//            JsfUtil.addErrorMessage("Enter Message");
//            return;
//        }
//        for (String stn : selectedTelephoneNumbers) {
//
//            smsController.sendSmsToNumberList(stn, getSessionController().getApplicationPreference().getApplicationInstitution(), smsText, null, MessageType.Marketing);
//            JsfUtil.addSuccessMessage("Done.");
//        }
//
//    }
    public String navigateToCancelPurchaseOrder() {
        makeNull();
        Calendar c1 = Calendar.getInstance();
        c1.set(Calendar.MONTH, c1.get(Calendar.MONTH) - 2);
        fromDate = c1.getTime();
        Calendar c2 = Calendar.getInstance();
        c2.set(Calendar.MONTH, c2.get(Calendar.MONTH) - 1);
        toDate = c2.getTime();
        return "/pharmacy/pharmacy_purhcase_order_list_to_cancel?faces-redirect=true";
    }

    public void createDocPaymentDue() {
        if (getReportKeyWord().getString().equals("0")) {
            fetchDueFeeTable(new BillType[]{BillType.OpdBill, BillType.CollectingCentreBill}, false);
        } else {
            fetchDueFeeTable(new BillType[]{BillType.InwardBill, BillType.InwardProfessional}, true);
        }
    }

    private void fetchDueFeeTable(BillType[] billTypes, boolean isInward) {
        String sql;
        Map m = new HashMap();

        sql = "select b from BillFee b where"
                + " b.retired=false "
                + " and b.bill.billType in :bts "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.createdAt between :fromDate and :toDate ";

        sql += " order by b.staff.person.name ";

        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        //System.out.println("1.cal.getTime() = " + cal.getTime());
        cal.set(2013, 00, 01, 00, 00, 00);
        //System.out.println("2.cal.getTime() = " + cal.getTime());
        m.put("fromDate", cal.getTime());
        m.put("toDate", getToDate());
        m.put("bts", Arrays.asList(billTypes));
//        temMap.put("btp", BillType.OpdBill);
//        temMap.put("btpc", BillType.CollectingCentreBill);
//        temMap.put("btp", BillType.InwardBill);
//        temMap.put("btp2", BillType.InwardProfessional);

        billFees = getBillFeeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        //System.out.println("billFees.size() = " + billFees.size());

        List<BillFee> afterPaid = new ArrayList<>();

        sql = "Select bi.paidForBillFee FROM BillItem bi "
                + " where bi.retired=false "
                + " and bi.bill.billType=:bType "
                + " and bi.referenceBill.billType in :refType "
                + " and bi.bill.createdAt > :toDate "
                + " and bi.paidForBillFee.bill.createdAt <= :toDate";

//        sql += " order by b.createdAt desc  ";
        m = new HashMap();
        m.put("toDate", getToDate());
        m.put("bType", BillType.PaymentBill);
        m.put("refType", Arrays.asList(billTypes));
//        temMap.put("refType", BillType.OpdBill);

        afterPaid = getBillFeeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        billFees.addAll(afterPaid);
        List<BillFee> removeingBillFees = new ArrayList<>();

        for (BillFee bf : billFees) {
            sql = "SELECT bi FROM BillItem bi where bi.retired=false "
                    + " and bi.bill.cancelled=false "
                    + " and bi.referanceBillItem.id=" + bf.getBillItem().getId();

            BillItem rbi = null;
            if (isInward) {
                m = new HashMap();
                m.put("btp", BillType.InwardBill);
                sql = "SELECT bi FROM BillItem bi where bi.retired=false "
                        + " and bi.bill.cancelled=false "
                        + " and bi.bill.billType=:btp "
                        + " and bi.referanceBillItem.id=" + bf.getBillItem().getId();
            } else {
                m = new HashMap();
//                m.put("class", RefundBill.class);
                sql = "SELECT bi FROM BillItem bi where "
                        + " bi.retired=false"
                        + " and bi.bill.cancelled=false "
                        //                        + " and type(bi.bill)=:class "
                        + " and bi.referanceBillItem.id=" + bf.getBillItem().getId();
            }
            rbi = getBillItemFacade().findFirstByJpql(sql, m);

            if (rbi != null) {
                removeingBillFees.add(bf);
            }
        }
        billFees.removeAll(removeingBillFees);
        total = 0.0;
        for (BillFee bf : billFees) {
            total += bf.getFeeValue();
        }
    }

//    public void createAllBillContacts() {
//        Map temMap = new HashMap();
//        bills=new ArrayList<>();
//
//        String sql = "select  b from Bill b where "
//                + " b.retired = false "
//                + " and b.cancelled=false "
//                + " and b.refunded=false "
//                + " and (b.patient.person.phone is not null "
//                + " or b.patient.person.phone=:em) "
//                + " and b.createdAt between :fd and :td  "
//                + " group by b.patient.person.phone  ";
//        
//        temMap.put("fd", fromDate);
//        temMap.put("td", toDate);
//        temMap.put("em", "");
//        
//        bills=getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//        //System.out.println("sql = " + sql);
//        //System.out.println("temMap = " + temMap);
//        //System.out.println("bills.size() = " + bills.size());
//
//    }
//     public List<Bill> getInstitutionPaymentBills() {
//        if (bills == null) {
//            String sql;
//            Map temMap = new HashMap();
//            if (bills == null) {
//                if (txtSearch == null || txtSearch.trim().equals("")) {
//                    sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.billType=:type and b.createdAt between :fromDate and :toDate order by b.id";
//                    temMap.put("toDate", getToDate());
//                    temMap.put("fromDate", getFromDate());
//                    temMap.put("type", BillType.PaymentBill);
//                    bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);
//
//                } else {
//                    sql = "select b from BilledBill b where b.retired=false and b.billType=:type and b.createdAt between :fromDate and :toDate and ((b.staff.person.name) like '%" + txtSearch.toUpperCase() + "%'  or (b.staff.person.phone) like '%" + txtSearch.toUpperCase() + "%'  or (b.insId) like '%" + txtSearch.toUpperCase() + "%') order by b.createdAt desc  ";
//                    temMap.put("toDate", getToDate());
//                    temMap.put("fromDate", getFromDate());
//                    temMap.put("type", BillType.PaymentBill);
//                    bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 100);
//                }
//                if (bills == null) {
//                    bills = new ArrayList<Bill>();
//                }
//            }
//        }
//        return bills;
//
//    }
    public void listnerBillTypeChange() {
        reportKeyWord.setArea(null);
    }

    public String navigateToLabReportSearch() {
        patientInvestigations = new ArrayList<>();
        getReportKeyWord().setDepartment(getSessionController().getLoggedUser().getDepartment());
        return "/lab/search_for_reporting_ondemand";
    }

    public String navigateToListSingleUserBills() {
        processAllFinancialTransactionalBillListBySingleUserByIds();
        return "/cashier/shift_end_summary_bill_list";
    }

    public SearchController() {
    }

    public class PharmacyAdjustmentRow {

        Item itm;
        double purchaseRate;
        double saleRate;
        double befoerQty;
        double afterQty;
        double adjusetedQty;
        double befoerVal;
        double afterVal;
        double adjusetedVal;
        String batchNo;
        Date expiry;

        public PharmacyAdjustmentRow() {
        }

        public PharmacyAdjustmentRow(Item itm, double purchaseRate, double saleRate, double befoerQty, double afterQty, double adjusetedQty, String batchNo, Date expiry) {
            this.itm = itm;
            this.purchaseRate = purchaseRate;
            this.saleRate = saleRate;
            this.befoerQty = befoerQty;
            this.afterQty = afterQty;
            this.adjusetedQty = adjusetedQty;
            this.batchNo = batchNo;
            this.expiry = expiry;
            this.adjusetedVal = adjusetedQty * purchaseRate;
            this.befoerVal = befoerQty * purchaseRate;
            this.afterVal = afterQty * purchaseRate;
        }

        public Item getItm() {
            return itm;
        }

        public void setItm(Item itm) {
            this.itm = itm;
        }

        public double getPurchaseRate() {
            return purchaseRate;
        }

        public void setPurchaseRate(double purchaseRate) {
            this.purchaseRate = purchaseRate;
        }

        public double getSaleRate() {
            return saleRate;
        }

        public void setSaleRate(double saleRate) {
            this.saleRate = saleRate;
        }

        public double getBefoerQty() {
            return befoerQty;
        }

        public void setBefoerQty(double befoerQty) {
            this.befoerQty = befoerQty;
        }

        public double getAfterQty() {
            return afterQty;
        }

        public void setAfterQty(double afterQty) {
            this.afterQty = afterQty;
        }

        public double getAdjusetedQty() {
            return adjusetedQty;
        }

        public void setAdjusetedQty(double adjusetedQty) {
            this.adjusetedQty = adjusetedQty;
        }

        public String getBatchNo() {
            return batchNo;
        }

        public void setBatchNo(String batchNo) {
            this.batchNo = batchNo;
        }

        public Date getExpiry() {
            return expiry;
        }

        public void setExpiry(Date expiry) {
            this.expiry = expiry;
        }

        public double getBefoerVal() {
            return befoerVal;
        }

        public void setBefoerVal(double befoerVal) {
            this.befoerVal = befoerVal;
        }

        public double getAfterVal() {
            return afterVal;
        }

        public void setAfterVal(double afterVal) {
            this.afterVal = afterVal;
        }

        public double getAdjusetedVal() {
            return adjusetedVal;
        }

        public void setAdjusetedVal(double adjusetedVal) {
            this.adjusetedVal = adjusetedVal;
        }
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

    public List<PatientInvestigation> getPatientInvestigationsSigle() {
        if (patientInvestigationsSigle == null) {
            createPatientInvestigationsTableSingle();
        }
        return patientInvestigationsSigle;
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

    public List<PharmacyAdjustmentRow> getPharmacyAdjustmentRows() {
        return pharmacyAdjustmentRows;
    }

    public void setPharmacyAdjustmentRows(List<PharmacyAdjustmentRow> pharmacyAdjustmentRows) {
        this.pharmacyAdjustmentRows = pharmacyAdjustmentRows;
    }

}
