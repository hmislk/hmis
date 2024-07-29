/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.collectingCentre.CollectingCentreBillController;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.bean.pharmacy.PharmacyPreSettleController;
import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillSummery;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.HistoryType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;

import com.divudi.ejb.EjbApplication;
import com.divudi.ejb.PharmacyBean;
import com.divudi.ejb.StaffBean;
import com.divudi.entity.AgentHistory;
import com.divudi.entity.AppEmail;
import com.divudi.entity.AuditEvent;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillEntry;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Payment;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.entity.cashTransaction.CashTransaction;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.entity.pharmacy.PharmaceuticalBillItem;
import com.divudi.facade.AgentHistoryFacade;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.EmailFacade;
import com.divudi.facade.ItemBatchFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.PharmaceuticalBillItemFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.pharmacy.PharmacyBillSearch;
import com.divudi.data.BillTypeAtomic;
import com.divudi.data.OptionScope;
import com.divudi.entity.Doctor;
import com.divudi.facade.FeeFacade;
import com.divudi.java.CommonFunctions;
import com.divudi.light.common.BillLight;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import kotlin.collections.ArrayDeque;
import org.apache.commons.beanutils.BeanUtils;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.LazyDataModel;

/**
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class BillSearch implements Serializable {

    /**
     * EJBs
     */
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillItemFacade billItemFacede;
    @EJB
    private BillComponentFacade billCommponentFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade;
    @EJB
    private PaymentFacade paymentFacade;

    private CommonFunctions commonFunctions;
    @EJB
    private BillNumberGenerator billNumberBean;
    @EJB
    private PharmacyBean pharmacyBean;
    @EJB
    private EjbApplication ejbApplication;
    @EJB
    private AgentHistoryFacade agentHistoryFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private ItemBatchFacade itemBatchFacade;
    @EJB
    private StaffBean staffBean;
    @EJB
    private WebUserFacade webUserFacade;
    @EJB
    CashTransactionBean cashTransactionBean;
    @EJB
    private EmailFacade emailFacade;
    @EJB
    FeeFacade feeFacade;
    /**
     * Controllers
     */
    @Inject
    private CollectingCentreBillController collectingCentreBillController;
    @Inject
    private SessionController sessionController;
    @Inject
    private CommonController commonController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private PharmacyPreSettleController pharmacyPreSettleController;
    @Inject
    private OpdPreSettleController opdPreSettleController;
    @Inject
    private PatientInvestigationController patientInvestigationController;
    @Inject
    private BillController billController;
    @Inject
    private BillBeanController billBean;
    @Inject
    private SecurityController securityController;
    @Inject
    NotificationController notificationController;
    @Inject
    ConfigOptionController configOptionController;
    @Inject
    private AuditEventApplicationController auditEventApplicationController;
    @Inject
    CommonFunctionsController commonFunctionsController;
    @Inject
    PharmacyBillSearch pharmacyBillSearch;

    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    /**
     * Class Variables
     */
    private Bill billForCancel;
    boolean showAllBills;
    private boolean printPreview = false;
    private double refundAmount;
    private String txtSearch;
    private Bill bill;
    private BillLight billLight;
    private Bill printingBill;
    private PaymentMethod paymentMethod;
    private RefundBill billForRefund;
    @Temporal(TemporalType.TIME)
    private Date fromDate;
    @Temporal(TemporalType.TIME)
    private Date toDate;
    private String comment;
    private WebUser user;
    private BillType billType;
    private BillClassType billClassType;
    ////////////////
    private List<Bill> billsToApproveCancellation;
    private List<Bill> billsApproving;
    private List<BillItem> refundingItems;
    private List<BillFee> refundingFees;
    private Bill refundingBill;
    private List<Bill> bills;
    private List<Bill> filteredBill;
    private List<BillEntry> billEntrys;
    private List<BillItem> billItems;
    private List<BillComponent> billComponents;
    private List<BillFee> billFees;
    private List<BillItem> tempbillItems;
    private LazyDataModel<BillItem> searchBillItems;
    private LazyDataModel<Bill> lazyBills;
    List<BillSummery> billSummeries;
    private BillSummery billSummery;
    ////////////////////
    ///////////////////
    private SearchKeyword searchKeyword;
    private Institution creditCompany;
    private PatientInvestigation patientInvestigation;
    private Institution institution;
    private Department department;
    private double refundTotal = 0;
    private double refundDiscount = 0;
    private double refundMargin = 0;
    private double refundVat = 0;
    private double refundVatPlusTotal = 0;
    String encryptedPatientReportId;
    String encryptedExpiary;

    private double cashTotal;
    private double slipTotal;
    private double creditTotal;
    private double creditCardTotal;
    private double multiplePaymentsTotal;
    private double patientDepositsTotal;
    private OverallSummary overallSummary;

    private Bill currentRefundBill;

    private boolean opdBillCancellationSameDay = false;
    private boolean opdBillRefundAllowedSameDay = false;

    //Edit Bill details
    private Doctor referredBy;

    public String navigateToBillPaymentOpdBill() {
        return "bill_payment_opd?faces-redirect=true";
    }

    public void fillBillFees() {
        for (BillItem bi : getRefundingBill().getBillItems()) {
            for (BillFee bfee : bi.getBillFees()) {
                bfee.setFeeValue(bfee.getReferenceBillFee().getFeeValue());
            }
        }
    }

    public void editBillDetails() {
        Bill editedBill = bill;
        if (bill == null) {
            JsfUtil.addErrorMessage("Bill Error !");
            return;
        }
        if (referredBy == null) {
            JsfUtil.addErrorMessage("Pleace Select Reffering Doctor !");
            return;
        }
        editedBill.setReferredBy(referredBy);
        if (bill.getId() == null) {
            billFacade.create(editedBill);
        }
        billFacade.edit(editedBill);
        JsfUtil.addSuccessMessage("Saved");
        referredBy = null;
    }

    public void preparePatientReportByIdForRequests() {
        bill = null;
        if (encryptedPatientReportId == null) {
            return;
        }
        if (encryptedExpiary != null) {
            Date expiaryDate;
            try {
                String ed = encryptedExpiary;
                ed = securityController.decrypt(ed);
                if (ed == null) {
                    return;
                }
                expiaryDate = new SimpleDateFormat("ddMMMMyyyyhhmmss").parse(ed);
            } catch (ParseException ex) {
                return;
            }
            if (expiaryDate.before(new Date())) {
                return;
            }
        }
        String idStr = getSecurityController().decrypt(encryptedPatientReportId);
        Long id = 0l;
        try {
            id = Long.parseLong(idStr);
        } catch (Exception e) {
            return;
        }
        Bill pr = getBillFacade().find(id);
        if (pr == null) {
            return;
        }
        bill = pr;
    }

    public void fillBillTypeSummery() {
        Map m = new HashMap();
        String j;
        if (billClassType == null) {
            j = "select new com.divudi.data.BillSummery(b.paymentMethod, sum(b.total), sum(b.discount), sum(b.netTotal), sum(b.vat), count(b), b.billType) "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        } else {
            j = "select new com.divudi.data.BillSummery(b.paymentMethod, b.billClassType, sum(b.total), sum(b.discount), sum(b.netTotal), sum(b.vat), count(b), b.billType) "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        }

        if (department == null) {
            j += " and b.institution=:ins ";
            m.put("ins", sessionController.getLoggedUser().getInstitution());
        } else {
            j += " and b.department=:dep ";
            m.put("dep", department);
        }
        if (user != null) {
            j += " and b.creater=:wu ";
            m.put("wu", user);
        }
        if (billType != null) {
            j += " and b.billType=:bt ";
            m.put("bt", billType);
        }
        if (billClassType != null) {
            j += " and b.billClassType=:bct ";
            m.put("bct", billClassType);
        }

        if (billClassType == null) {
            j += " group by b.paymentMethod,  b.billType";
        } else {
            j += " group by b.paymentMethod, b.billClassType, b.billType";
        }
        Boolean bf = false;
        if (bf) {
            Bill b = new Bill();
            b.getPaymentMethod();
            b.getTotal();
            b.getDiscount();
            b.getNetTotal();
            b.getVat();
            b.getBillType();
            b.getBillTime();
            b.getInstitution();
            b.getCreater();
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        List<Object> objs = billFacade.findObjectByJpql(j, m, TemporalType.TIMESTAMP);
        billSummeries = new ArrayList<>();
        Long i = 1l;
        for (Object o : objs) {
            BillSummery tbs = (BillSummery) o;
            tbs.setKey(i);
            billSummeries.add(tbs);
            i++;
        }
    }

    public void processCashierBillSummery() {
        //For Auditing Purposes
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
        auditEvent.setEventTrigger("fillTransactionTypeSummery()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Map m = new HashMap();
        String j;
        if (billClassType == null) {
            j = "select new com.divudi.data.BillSummery(b.paymentMethod, sum(b.total), sum(b.discount), sum(b.netTotal), sum(b.vat), count(b), b.billType) "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        } else {
            j = "select new com.divudi.data.BillSummery(b.paymentMethod, b.billClassType, sum(b.total), sum(b.discount), sum(b.netTotal), sum(b.vat), count(b), b.billType) "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        }

        if (institution != null) {
            j += " and b.institution=:ins ";
            m.put("ins", institution);
        }

        if (department != null) {
            j += " and b.department=:dep ";
            m.put("dep", department);
        }
        if (user != null) {
            j += " and b.creater=:wu ";
            m.put("wu", user);
        }
        if (billType != null) {
            j += " and b.billType=:bt ";
            m.put("bt", billType);
        }
        if (billClassType != null) {
            j += " and b.billClassType=:bct ";
            m.put("bct", billClassType);
        }

        if (billClassType == null) {
            j += " group by b.paymentMethod,  b.billType";
        } else {
            j += " group by b.paymentMethod, b.billClassType, b.billType";
        }
        Boolean bf = false;
        if (bf) {
            Bill b = new Bill();
            b.getPaymentMethod();
            b.getTotal();
            b.getDiscount();
            b.getNetTotal();
            b.getVat();
            b.getBillType();
            b.getBillTime();
            b.getInstitution();
            b.getCreater();
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        List<Object> objs = billFacade.findObjectByJpql(j, m, TemporalType.TIMESTAMP);
        billSummeries = new ArrayList<>();
        Long i = 1l;
        for (Object o : objs) {
            BillSummery tbs = (BillSummery) o;
            tbs.setKey(i);
            billSummeries.add(tbs);
            i++;
        }

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void processCashierPharmacySaleBillSummery() {
        //For Auditing Purposes
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
        auditEvent.setEventTrigger("fillTransactionTypeSummery()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        Map m = new HashMap();
        String j;
        j = "select new com.divudi.data.BillSummery(b.paymentMethod, b.billClassType, sum(b.total), sum(b.discount), sum(b.netTotal), sum(b.vat), count(b), b.billTypeAtomic, b.billType, b.creater) "
                + " from Bill b "
                + " where b.retired=false "
                + " and b.billTime between :fd and :td ";

        if (institution != null) {
            j += " and b.institution=:ins ";
            m.put("ins", institution);
        }

        if (department != null) {
            j += " and b.department=:dep ";
            m.put("dep", department);
        }
        if (user != null) {
            j += " and b.creater=:wu ";
            m.put("wu", user);
        }
        List<BillType> billTypes = new ArrayList<>();
        billTypes.add(BillType.PharmacySale);
        billTypes.add(BillType.PharmacySaleWithoutStock);
        billTypes.add(BillType.PharmacyWholeSale);
        j += " and b.billType in :bts ";
        m.put("bts", billTypes);

        j += " group by b.paymentMethod, b.billClassType, b.billType, b.creater, b.billTypeAtomic";

        Boolean bf = false;
        if (bf) {
            Bill b = new Bill();
            b.getPaymentMethod();
            b.getTotal();
            b.getDiscount();
            b.getNetTotal();
            b.getVat();
            b.getBillType();
            b.getBillTime();
            b.getInstitution();
            b.getCreater();
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        List<Object> objs = billFacade.findObjectByJpql(j, m, TemporalType.TIMESTAMP);
        billSummeries = new ArrayList<>();
        Long i = 1l;
        for (Object o : objs) {
            BillSummery tbs = (BillSummery) o;
            tbs.setKey(i);
            billSummeries.add(tbs);
            i++;
        }
        calculateTotalForBillSummaries();

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public void processCashierPharmacySaleBillTotalDateWise() {
        //For Auditing Purposes

        Map m = new HashMap();
        String j;
        j = "select new com.divudi.data.BillSummery(Function('DATE',(b.createdAt)),sum(b.netTotal), count(b)) "
                + " from Bill b "
                + " where b.retired=false "
                + " and b.billTime between :fd and :td ";

        if (institution != null) {
            j += " and b.institution=:ins ";
            m.put("ins", institution);
        }

        if (department != null) {
            j += " and b.department=:dep ";
            m.put("dep", department);
        }

        j += " group by Function('DATE',(b.createdAt))";

        m.put("fd", fromDate);
        m.put("td", toDate);

        List<Object> objs = billFacade.findObjectByJpql(j, m, TemporalType.TIMESTAMP);
        billSummeries = new ArrayList<>();
        Long i = 1l;
        for (Object o : objs) {
            BillSummery tbs = (BillSummery) o;
            tbs.setKey(i);
            billSummeries.add(tbs);
            i++;
        }

    }

    public void fillCashierSummery() {
        //For Auditing Purposes
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
        auditEvent.setEventTrigger("fillTransactionTypeSummery()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        List<BillType> bts = new ArrayList<>();
        bts.add(BillType.OpdBill);
        bts.add(BillType.PharmacySale);
        bts.add(BillType.PharmacyWholeSale);
        bts.add(BillType.InwardPaymentBill);
        bts.add(BillType.CollectingCentrePaymentReceiveBill);
        bts.add(BillType.PaymentBill);
        bts.add(BillType.PatientPaymentReceiveBill);
        bts.add(BillType.CollectingCentreBill);
        bts.add(BillType.PaymentBill);
        bts.add(BillType.ChannelCash);
        bts.add(BillType.ChannelPaid);
        bts.add(BillType.ChannelAgent);
        bts.add(BillType.ChannelProPayment);
        bts.add(BillType.ChannelAgencyCommission);
        bts.add(BillType.PettyCash);

        billSummeries = generateBillSummaries(institution, department, user, bts, billClassType, fromDate, toDate);

        overallSummary = aggregateBillSummaries(billSummeries);

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);

    }

    public List<BillSummery> generateBillSummaries(Institution ins, Department dep, WebUser u, List<BillType> bts, BillClassType bct, Date fd, Date td) {
        Map<String, Object> parameters = new HashMap<>();
        StringBuilder queryString = new StringBuilder("select new com.divudi.data.BillSummery(b.paymentMethod, ");
//        if (bct != null) {
//            queryString.append("b.billClassType, ");
//        }
        queryString.append("b.billClassType, ");
        queryString.append("sum(b.total), sum(b.discount), sum(b.netTotal), sum(b.vat), count(b), b.billType) from Bill b where b.retired=false and b.billTime between :fd and :td");
        parameters.put("fd", fd);
        parameters.put("td", td);
        if (ins != null) {
            queryString.append(" and b.institution=:ins");
            parameters.put("ins", ins);
        }
        if (dep != null) {
            queryString.append(" and b.department=:dep");
            parameters.put("dep", dep);
        }
        if (u != null) {
            queryString.append(" and b.creater=:wu");
            parameters.put("wu", u);
        }
        if (bts != null) {
            queryString.append(" and b.billType in :bts");
            parameters.put("bts", bts);
        }
        if (bct != null) {
            queryString.append(" and b.billClassType=:bct");
            parameters.put("bct", bct);
        }

        queryString.append(" group by b.paymentMethod, b.billClassType, b.billType");

        // queryString.append(bct == null ? " group by b.paymentMethod, b.billType" : " group by b.paymentMethod, b.billClassType, b.billType");
        List<BillSummery> bss = (List<BillSummery>) billFacade.findLightsByJpql(queryString.toString(), parameters, TemporalType.TIMESTAMP);
        if (bss == null || bss.isEmpty()) {
            return new ArrayList<>();
        }
        long key = 1;
        for (BillSummery result : bss) {

            result.setKey(key++);
        }
        return bss;
    }

    public String listBillsFromBillTypeSummery() {
        if (billSummery == null) {
            JsfUtil.addErrorMessage("No Summary Selected");
            return "";
        }
        String directTo;
        Map m = new HashMap();
        String j;

        BillClassType filteredBillClassType = billSummery.getBillClassType();
        BillType filteredBillType = billSummery.getBillType();
        PaymentMethod filteredPaymentMethod = billSummery.getPaymentMethod();

        if (filteredBillClassType == null) {
            j = "select b "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        } else {
            j = "select b "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        }

        if (department != null) {
            j += " and b.department=:dep ";
            m.put("dep", department);
        }
        if (institution != null) {
            j += " and b.institution=:ins ";
            m.put("ins", institution);
        }
        if (user != null) {
            j += " and b.creater=:wu ";
            m.put("wu", user);
        }

        if (filteredPaymentMethod != null) {
            j += " and b.paymentMethod=:pm ";
            m.put("pm", filteredPaymentMethod);
        }

        if (filteredBillType != null) {
            j += " and b.billType=:bt ";
            m.put("bt", filteredBillType);
        }
        if (filteredBillClassType != null) {
            j += " and b.billClassType=:bct ";
            m.put("bct", filteredBillClassType);
        }
        m.put("fd", fromDate);
        m.put("td", toDate);

        bills = billFacade.findByJpql(j, m, TemporalType.TIMESTAMP);

        if (bills != null) {
        } else {
        }

        if (filteredBillClassType == BillClassType.CancelledBill || filteredBillClassType == BillClassType.RefundBill) {
            directTo = "/reportIncome/bill_list_cancelled";
        } else {
            directTo = "/reportIncome/bill_list";
        }

        return directTo;
    }

    public String listBillsFromBillTransactionTypeSummery() {
        if (billSummery == null) {
            JsfUtil.addErrorMessage("No Summary Selected");
            return "";
        }
        String directTo;
        Map m = new HashMap();
        String j;

        BillClassType tmpBillClassType = billSummery.getBillClassType();
        BillType tmpBllType = billSummery.getBillType();

        if (tmpBillClassType == null) {
            j = "select b "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        } else {
            j = "select b "
                    + " from Bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        }

        if (institution != null) {
            j += " and b.institution=:ins ";
            m.put("ins", institution);
        }

        if (department != null) {
            j += " and b.department=:dep ";
            m.put("dep", department);
        }

        if (user != null) {
            j += " and b.creater=:wu ";
            m.put("wu", user);
        }
        if (tmpBllType != null) {
            j += " and b.billType=:bt ";
            m.put("bt", tmpBllType);
        }
        if (tmpBillClassType != null) {
            j += " and b.billClassType=:bct ";
            m.put("bct", tmpBillClassType);
        }
        m.put("fd", fromDate);
        m.put("td", toDate);

        bills = billFacade.findByJpql(j, m, TemporalType.TIMESTAMP);

        if (tmpBillClassType == BillClassType.CancelledBill || tmpBillClassType == BillClassType.RefundBill) {
            directTo = "/reportInstitution/bill_list_cancelled";
        } else {
            directTo = "/reportInstitution/bill_list";
        }
        return directTo;
    }

    public void fillBillFeeTypeSummery() {
        Map m = new HashMap();
        String j;
        if (billClassType == null) {
            j = "select new com.divudi.data.BillSummery(b.paymentMethod, sum(bf.feeGrossValue), sum(bf.feeDiscount), sum(bf.feeValue), sum(bf.feeVat), count(b), b.billType) "
                    + " from BillFee bf inner join bf.bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        } else {
            j = "select new com.divudi.data.BillSummery(b.paymentMethod, b.billClassType, sum(bf.feeGrossValue), sum(bf.feeDiscount), sum(bf.feeValue), sum(bf.feeVat), count(b), b.billType) "
                    + " from BillFee bf inner join bf.bill b "
                    + " where b.retired=false "
                    + " and b.billTime between :fd and :td ";
        }

        if (department == null) {
            j += " and b.institution=:ins ";
            m.put("ins", sessionController.getLoggedUser().getInstitution());
        } else {
            j += " and b.toDepartment=:dep ";
            m.put("dep", department);
        }
        if (user != null) {
            j += " and b.creater=:wu ";
            m.put("wu", user);
        }
        if (billType != null) {
            j += " and b.billType=:bt ";
            m.put("bt", billType);
        }
        if (billClassType != null) {
            j += " and b.billClassType=:bct ";
            m.put("bct", billClassType);
        }

        if (billClassType == null) {
            j += " group by b.paymentMethod,  b.billType";
        } else {
            j += " group by b.paymentMethod, b.billClassType, b.billType";
        }
        Boolean bf = false;
        if (bf) {
            Bill b = new Bill();
            b.getPaymentMethod();
            b.getTotal();
            b.getDiscount();
            b.getNetTotal();
            b.getVat();
            b.getBillType();
            b.getBillTime();
            b.getInstitution();
            b.getCreater();
        }

        m.put("fd", fromDate);
        m.put("td", toDate);

        List<Object> objs = billFacade.findObjectByJpql(j, m, TemporalType.TIMESTAMP);
        billSummeries = new ArrayList<>();
        Long i = 1l;
        for (Object o : objs) {
            BillSummery tbs = (BillSummery) o;
            tbs.setKey(i);
            billSummeries.add(tbs);
            i++;
        }
    }

    public void clearSearchFIelds() {
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
        auditEvent.setEventTrigger("clearSearchFIelds()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        auditEventApplicationController.logAuditEvent(auditEvent);
        department = null;
        fromDate = null;
        toDate = null;
        institution = null;
        user = null;
        billType = null;
        billClassType = null;

        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
    }

    public BillSearch() {
    }

    public void updateBill() {

        if (bill.getPatientEncounter() == null) {
            bill.setCreditCompany(creditCompany);
        } else {
            bill.getPatientEncounter().setCreditCompany(creditCompany);
        }
        bill.setEditedAt(new Date());
        bill.setEditor(sessionController.getLoggedUser());
        billFacade.edit(bill);
        JsfUtil.addSuccessMessage("Bill Upadted");

    }

    private double roundOff(double d, int position) {
        DecimalFormat newFormat = new DecimalFormat("#.##");
        return Double.valueOf(newFormat.format(d));
    }

    public void updateValue() {

        for (BillFee bf : billFeesList) {
            bf.setFeeGrossValue(roundOff(bf.getFeeGrossValue(), 2));
            bf.setFeeDiscount(roundOff(bf.getFeeDiscount(), 2));
            bf.setFeeValue(roundOff(bf.getFeeValue(), 2));
            billFeeFacade.edit(bf);
        }

        for (BillItem bt : billItemList) {
            String sql = "select sum(b.feeGrossValue)  "
                    + " from BillFee b "
                    + " where b.retired=false"
                    + " and b.billItem.id=" + bt.getId();
            bt.setGrossValue(billItemFacade.findDoubleByJpql(sql));

            sql = "select sum(b.feeDiscount)  "
                    + " from BillFee b "
                    + " where b.retired=false"
                    + " and b.billItem.id=" + bt.getId();

            bt.setDiscount(billItemFacade.findDoubleByJpql(sql));

            sql = "select sum(b.feeValue)  "
                    + " from BillFee b "
                    + " where b.retired=false"
                    + " and b.billItem.id=" + bt.getId();
            bt.setNetValue(billItemFacade.findDoubleByJpql(sql));
        }

        String sql = "select sum(b.grossValue)  "
                + " from BillItem b "
                + " where b.retired=false"
                + " and b.bill.id=" + bill.getId();
        bill.setTotal(billItemFacade.findDoubleByJpql(sql));

        sql = "select sum(b.discount)  "
                + " from BillItem b "
                + " where b.retired=false"
                + " and b.bill.id=" + bill.getId();

        bill.setDiscount(billItemFacade.findDoubleByJpql(sql));

        sql = "select sum(b.netValue)  "
                + " from BillItem b "
                + " where b.retired=false"
                + " and b.bill.id=" + bill.getId();
        bill.setNetTotal(billItemFacade.findDoubleByJpql(sql));
        billFacade.edit(bill);

        JsfUtil.addSuccessMessage("Bill Upadted");

    }

    public void updateBillFeeRetierd(BillFee bf) {
        if (bf.isRetired()) {
            bf.setRetiredAt(new Date());
            bf.setRetirer(sessionController.getLoggedUser());
            getBillFeeFacade().edit(bf);
            JsfUtil.addSuccessMessage("Bill Fee Retired");
        }
    }

    public void updateBillItemRetierd(BillItem bi) {

        if (bi.isRetired()) {
            bi.setRetiredAt(new Date());
            bi.setRetirer(sessionController.getLoggedUser());
            getBillItemFacade().edit(bi);
            JsfUtil.addSuccessMessage("Bill Item Retired");
        }
    }

    public void updateBillfee(BillFee bf) {

        getBillFeeFacade().edit(bf);
        JsfUtil.addSuccessMessage("Bill Item Retired");
    }

    private void createBillFees() {
        String sql = "SELECT b FROM BillFee b WHERE b.bill.id=" + getBillSearch().getId();
        billFeesList = getBillFeeFacade().findByJpql(sql);
    }

    private List<BillItem> billItemList;

    private void createBillItemsAll() {
        String sql = "SELECT b FROM BillItem b WHERE b.bill.id=" + getBillSearch().getId();
        billItemList = billItemFacade.findByJpql(sql);
    }

    public void createCashReturnBills() {
        bills = null;
        Map m = new HashMap();
        m.put("bt", BillType.PharmacySale);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from RefundBill b where  b.retired=false and b.institution=:ins and"
                + " b.createdAt between :fd and :td and b.billType=:bt ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.id desc  ";

        bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void makeNull() {
        printPreview = false;
        refundAmount = 0;
        txtSearch = "";
        bill = null;
        paymentMethod = null;
        billForRefund = null;
        comment = "";
        user = null;
        refundingItems = null;
        bills = null;
        filteredBill = null;
        billEntrys = null;
        billItems = null;
        billComponents = null;
        billFees = null;
        tempbillItems = null;
        lazyBills = null;
        searchKeyword = null;
    }

    public void update() {
        getBillFacade().edit(getBill());
    }

    public WebUser getUser() {
        return user;
    }

    public PatientInvestigation getPatientInvestigation() {
        return patientInvestigation;
    }

    public void setPatientInvestigation(PatientInvestigation patientInvestigation) {
        this.patientInvestigation = patientInvestigation;
    }

    public void onEdit(RowEditEvent event) {

        BillFee tmp = (BillFee) event.getObject();

        tmp.setEditedAt(new Date());
        tmp.setEditor(sessionController.getLoggedUser());

//        if (tmp.getPaidValue() != 0.0) {
//            JsfUtil.addErrorMessage("Already Staff FeePaid");
//            return;
//        }
        getBillFeeFacade().edit(tmp);

    }

    public void onEditItem(RowEditEvent event) {

        BillItem tmp = (BillItem) event.getObject();
        tmp.setEditedAt(new Date());
        tmp.setEditor(sessionController.getLoggedUser());
        getBillItemFacade().edit(tmp);
        ////// // System.out.println("1.tmp = " + tmp.getPaidForBillFee().getPaidValue());
        if (tmp.getPaidForBillFee() != null) {
            getBillFeeFacade().edit(tmp.getPaidForBillFee());
        }
        ////// // System.out.println("2.tmp = " + tmp.getPaidForBillFee().getPaidValue());
//        if (tmp.getPaidValue() != 0.0) {
//            JsfUtil.addErrorMessage("Already Staff FeePaid");
//            return;
//        }

    }

    public void setUser(WebUser user) {
        // recreateModel();
        this.user = user;
        recreateModel();
    }

    public EjbApplication getEjbApplication() {
        return ejbApplication;
    }

    public void setEjbApplication(EjbApplication ejbApplication) {
        this.ejbApplication = ejbApplication;
    }

    public Bill getPrintingBill() {
        return printingBill;
    }

    public void setPrintingBill(Bill printingBill) {
        this.printingBill = printingBill;
    }

    public boolean calculateRefundTotal() {
        refundAmount = 0;
        refundDiscount = 0;
        refundTotal = 0;
        refundMargin = 0;
        refundVat = 0;
        refundVatPlusTotal = 0;
        //billItems=null;
        tempbillItems = null;
        for (BillItem i : getRefundingItems()) {
            if (checkPaidIndividual(i)) {
                JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Refund Bill");
                return false;
            }
            //Add for check refund is already done
            String sql = "SELECT bi FROM BillItem bi where bi.retired=false and bi.referanceBillItem.id=" + i.getId();
            BillItem rbi = getBillItemFacade().findFirstByJpql(sql);

            if (rbi != null) {
                JsfUtil.addErrorMessage("This Bill Item Already Refunded");
                return false;
            }
            //

//            if (!i.isRefunded()) {
            refundTotal += i.getGrossValue();
            refundAmount += i.getNetValue();
            refundMargin += i.getMarginValue();
            refundDiscount += i.getDiscount();
            refundVat += i.getVat();
            refundVatPlusTotal += i.getVatPlusNetValue();
            getTempbillItems().add(i);
//            }

        }

        return true;
    }

    public void calculateRefundTotalForOpdBillForAjex() {
        calculateRefundTotalForOpdBill();
    }

    public boolean calculateRefundTotalForOpdBill() {

        refundAmount = 0;
        refundDiscount = 0;
        refundTotal = 0;
        refundMargin = 0;
        refundVat = 0;
        refundVatPlusTotal = 0;
        if (getRefundingBill() == null) {
            return false;
        }
        if (getRefundingBill().getBillItems() == null) {
            return false;
        }
        for (BillItem i : getRefundingBill().getBillItems()) {

            if (i.getBillFees() == null) {
                return false;
            }

            double refundingValue = 0;
            for (BillFee rbf : i.getBillFees()) {
                System.out.println("rbf.getFeeValue() name = " + rbf.getFee().getName());
                System.out.println("rbf.getFeeValue() = " + rbf.getFeeValue());
                System.out.println("rbf.getFeeValue() fee = " + rbf.getFee().getFee());
                refundingValue += rbf.getFeeValue();
            }
            i.setNetValue(refundingValue);
            i.setGrossValue(refundingValue);
            i.setDiscount(0.0);
            refundTotal += i.getGrossValue();
            refundAmount += i.getNetValue();
            refundMargin += i.getMarginValue();
            refundDiscount += i.getDiscount();
            refundVat += i.getVat();
            refundVatPlusTotal += i.getVatPlusNetValue();
            getTempbillItems().add(i);

        }

        getRefundingBill().setNetTotal(refundAmount);
        getRefundingBill().setNetTotal(refundTotal);
        getRefundingBill().setMargin(refundMargin);
        getRefundingBill().setDiscount(refundDiscount);
        getRefundingBill().setVat(refundVat);

        return true;
    }

    public List<Bill> getUserBillsOwn() {
        Date startTime = new Date();
        List<Bill> userBills;
        if (getUser() == null) {
            userBills = new ArrayList<>();
            //////// // System.out.println("user is null");
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), getSessionController().getInstitution(), BillType.OpdBill);

            //////// // System.out.println("user ok");
        }
        if (userBills == null) {
            userBills = new ArrayList<>();

        }

        return userBills;

    }

    public List<Bill> getBillsOwn() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), getSessionController().getInstitution(), BillType.OpdBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), getSessionController().getInstitution(), BillType.OpdBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public void recreateModel2() {
        billForRefund = null;
        refundAmount = 0.0;
        billFees = null;
//        billFees
        fromDate = null;
        toDate = null;
        billComponents = null;
        billForRefund = null;
        billItems = null;
        bills = null;
        printPreview = false;
        tempbillItems = null;
        comment = null;
        lazyBills = null;
    }

    public LazyDataModel<Bill> getSearchBills() {
        return lazyBills;
    }

    public void createDealorPaymentTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  ((b.toInstitution.name) like :ins )";
            temMap.put("ins", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  ((b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBank() != null && !getSearchKeyword().getBank().trim().equals("")) {
            sql += " and  ((b.bank.name) like :bnk )";
            temMap.put("bnk", "%" + getSearchKeyword().getBank().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  ((b.chequeRefNo) like :chck )";
            temMap.put("chck", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.id desc  ";

        temMap.put("billType", BillType.GrnPayment);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("dept", getSessionController().getInstitution());
        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP, 50);
        //     //System.err.println("SIZE : " + lst.size());

    }

    public void makeKeywodNull() {
        searchKeyword = null;
    }

    public List<BillItem> getRefundingItems() {
        return refundingItems;
    }

    public void setRefundingItems(List<BillItem> refundingItems) {
        this.refundingItems = refundingItems;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String refundBill() {
        if (refundingItems.isEmpty()) {
            JsfUtil.addErrorMessage("There is no item to Refund");
            return "";

        }
        if (getBill().getPatientEncounter() != null) {
            if (getBill().getPatientEncounter().isPaymentFinalized()) {
                JsfUtil.addErrorMessage("Final Payment is Finalized You can't Return");
                return "";
            }
        }
//        if (refundAmount == 0.0) {
//            JsfUtil.addErrorMessage("There is no item to Refund");
//            return "";
//        }
        if (comment == null || comment.trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return "";
        }

        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (getBill().isCancelled()) {
                JsfUtil.addErrorMessage("Already Cancelled. Can not Refund again");
                return "";
            }

            if (getBill().getBillType() == BillType.InwardBill) {
                if (getBill().getCheckedBy() != null) {
                    JsfUtil.addErrorMessage("Please Uncheck Bill");
                    return "";
                }
            }

            if (!calculateRefundTotal()) {
                return "";
            }

            if (!getWebUserController().hasPrivilege("LabBillRefundSpecial")) {
                for (BillItem trbi : refundingItems) {
                    if (patientInvestigationController.sampledForBillItem(trbi)) {
                        JsfUtil.addErrorMessage("One or more bill Item has been already undersone process at the Lab. Can not return.");
                        return "";
                    }
                }
            }

            RefundBill rb = (RefundBill) createRefundBill();
            Payment p = getOpdPreSettleController().createPaymentForCancellationsAndRefunds(rb, paymentMethod);
            refundBillItems(rb, p);
            p.setPaidValue(getOpdPreSettleController().calBillPaidValue(rb));

            paymentFacade.edit(p);

            calculateRefundBillFees(rb);

            getBill().setRefunded(true);
            getBill().setRefundedBill(rb);
            getBillFacade().edit(getBill());
            double feeTotalExceptCcfs = 0.0;
            if (getBill().getBillType() == BillType.CollectingCentreBill) {
                for (BillItem bi : refundingItems) {
                    String sql = "select c from BillFee c where c.billItem.id = " + bi.getId();
                    List<BillFee> rbf = getBillFeeFacade().findByJpql(sql);
                    for (BillFee bf : rbf) {
                        if (bf.getFee().getFeeType() != FeeType.CollectingCentre) {
                            feeTotalExceptCcfs += (bf.getFeeValue() + bf.getFeeVat());
                        }
                    }
                }

                collectingCentreBillController.updateBallance(getBill().getInstitution(), Math.abs(feeTotalExceptCcfs), HistoryType.CollectingCentreBilling, getBill().getRefundedBill(), getBill().getReferenceNumber());
            }

            if (getBill().getPaymentMethod() == PaymentMethod.Credit) {
                //   ////// // System.out.println("getBill().getPaymentMethod() = " + getBill().getPaymentMethod());
                //   ////// // System.out.println("getBill().getToStaff() = " + getBill().getToStaff());
                if (getBill().getToStaff() != null) {
                    //   ////// // System.out.println("getBill().getNetTotal() = " + getBill().getNetTotal());
                    staffBean.updateStaffCredit(getBill().getToStaff(), (rb.getNetTotal() + rb.getVat()));
                    JsfUtil.addSuccessMessage("Staff Credit Updated");
                }
            }

            WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(rb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);

            bill = billFacade.find(rb.getId());
            createCollectingCenterfees(bill);
            printPreview = true;
            //JsfUtil.addSuccessMessage("Refunded");

        } else {
            JsfUtil.addErrorMessage("No Bill to refund");
            return "";
        }
        //  recreateModel();
        return "";
    }

    public String refundOpdBill() {
        if (getRefundingBill() == null) {
            JsfUtil.addErrorMessage("There is no Bill to Refund");
            return "";
        }
        if (refundingBill.getBillItems().isEmpty()) {
            JsfUtil.addErrorMessage("There is no item to Refund");
            return "";
        }

        if (comment == null || comment.trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return "";
        }

        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not Refund again");
            return "";
        }

        if (!getWebUserController().hasPrivilege("LabBillRefundSpecial")) {
            if (sampleHasBeenCollected(refundingBill)) {
                JsfUtil.addErrorMessage("One or more bill Item you are refunding has been already undersone process at the Lab. Can not return.");
                return "";
            }
        }

        if (billFeeIsAlreadyRefunded(refundingBill)) {
            JsfUtil.addErrorMessage("One or more bill Item you are refunding has been already refunded. Can not refund again.");
            return "";
        }

        if (billFeeIsAlreadyPaidToStaff(refundingBill)) {
            JsfUtil.addErrorMessage("One or more bill Item you are refunding has been already paid to Service Provider. Can not refund again.");
            return "";
        }

        if (refundingBill.getBillItems() != null) {
            for (BillItem bi : refundingBill.getBillItems()) {
                for (BillFee bf : bi.getBillFees()) {
                    if (bf.getReferenceBillFee().getFeeValue() < bf.getFeeValue()) {
                        JsfUtil.addErrorMessage("Pleace Enter Correct Value");
                        return "";
                    }
                }
            }
        }

        boolean reundTotalCalculationsSuccess = calculateRefundTotalForOpdBill();
        if (!reundTotalCalculationsSuccess) {
            JsfUtil.addErrorMessage("Error in calculations. Please Check Bill");
            return "";
        }

        if (refundingBill.getNetTotal() == 0.0) {
            JsfUtil.addErrorMessage("There is no item to Refund");
            return "";
        }
        boolean billValueInversionIsSuccess = invertBillValuesAndCalculate(refundingBill);
        if (!billValueInversionIsSuccess) {
            JsfUtil.addErrorMessage("Error in Bill Value Inversion");
            return "";
        }

        saveRefundBill(refundingBill);

        Payment p = getOpdPreSettleController().createPaymentForCancellationsAndRefunds(refundingBill, paymentMethod);

        //TODO: Create Payments for Bill Items
        if (getBill().getPaymentMethod() == PaymentMethod.Credit) {
            if (getBill().getToStaff() != null) {
                staffBean.updateStaffCredit(getBill().getToStaff(), 0 - (getBill().getNetTotal() + getBill().getVat()));
                JsfUtil.addSuccessMessage("Staff Credit Updated");
            } else if (getBill().getCreditCompany() != null) {
                //TODO : Update Credit COmpany Bill
            }
        } else if (getBill().getPaymentMethod() == PaymentMethod.PatientDeposit) {
            //TODO: Update Patient Deposit
        }

        //TODO - Update Bill Payment Values
        //TODO - Update Bill Paid Value
        printPreview = true;
        return "";
    }

    public boolean sampleHasBeenCollected(Bill rf) {
        boolean oneItemIsSampled = false;
        for (BillItem bi : rf.getBillItems()) {
            double refVal = bi.getNetValue();
            refVal = Math.abs(refVal);
            if (refVal > 0.0) {
                boolean sampled = patientInvestigationController.sampledForBillItem(bi.getReferanceBillItem());
                if (sampled) {
                    oneItemIsSampled = true;
                }
            }
        }
        return oneItemIsSampled;
    }

    public boolean billFeeIsAlreadyRefunded(Bill rf) {
        boolean oneItemIsAlreadyRefunded = false;
        for (BillItem bi : rf.getBillItems()) {
            for (BillFee bf : bi.getBillFees()) {
                double refVal = bi.getNetValue();
                refVal = Math.abs(refVal);
                if (refVal > 0.0) {
                    boolean refunded = billController.hasRefunded(bf.getReferenceBillFee());
                    if (refunded) {
                        oneItemIsAlreadyRefunded = true;
                    }
                }
            }
        }
        return oneItemIsAlreadyRefunded;
    }

    public boolean billFeeIsAlreadyPaidToStaff(Bill rf) {
        boolean oneItemIsAlreadyPaid = false;
        for (BillItem bi : rf.getBillItems()) {
            for (BillFee bf : bi.getBillFees()) {
                double refVal = bi.getNetValue();
                refVal = Math.abs(refVal);
                if (refVal > 0.0) {
                    boolean refunded = billController.hasPaidToStaff(bf.getReferenceBillFee());
                    if (refunded) {
                        oneItemIsAlreadyPaid = true;
                    }
                }
            }
        }
        return oneItemIsAlreadyPaid;
    }

    private boolean saveRefundBill(Bill rb) {
        if (rb == null) {
            JsfUtil.addErrorMessage("No bill");
            return false;
        }
        if (rb.getBilledBill() == null) {
            rb.setBilledBill(getBill());
        }
        Date bd = Calendar.getInstance().getTime();
        rb.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_REFUND);
        rb.setBillType(billType.PaymentBill);
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setDepartment(getSessionController().getDepartment());
        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        rb.setComments(comment);
        rb.setPaymentMethod(paymentMethod);
        rb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill().getToDepartment(), BillType.OpdBill, BillClassType.RefundBill, BillNumberSuffix.RF));
        rb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getBill().getToDepartment(), BillType.OpdBill, BillClassType.RefundBill, BillNumberSuffix.RF));
        rb.setRefunded(Boolean.TRUE);
        rb.setReferenceBill(bill);
        rb.setBilledBill(bill);
        billController.save(rb);
        currentRefundBill = rb;
        for (BillItem bi : rb.getBillItems()) {
            for (BillFee bf : bi.getBillFees()) {
                billController.saveBillFee(bf);
            }
            billController.saveBillItem(bi);
        }

        List<Bill> refundBills = new ArrayDeque<>();
        refundBills.addAll(bill.getRefundBills());
        refundBills.add(rb);

        bill.getForwardReferenceBills().add(rb);
        bill.setRefunded(true);
        bill.setRefundBills(refundBills);
        bill.setRefundedBill(rb);
        billController.save(bill);
        return true;
    }

    private Bill createRefundBill() {
        RefundBill rb = new RefundBill();
        rb.copy(getBill());
        rb.invertValue(getBill());

        rb.setBilledBill(getBill());
        Date bd = Calendar.getInstance().getTime();
        rb.setBillDate(bd);
        rb.setBillTime(bd);
        rb.setCreatedAt(bd);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setDepartment(getSessionController().getDepartment());
        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        rb.setDiscount(0.00);
        rb.setDiscountPercent(0.0);
        rb.setComments(comment);
        rb.setPaymentMethod(paymentMethod);

        rb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill().getToDepartment(), BillType.OpdBill, BillClassType.RefundBill, BillNumberSuffix.RF));
        rb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getBill().getToDepartment(), BillType.OpdBill, BillClassType.RefundBill, BillNumberSuffix.RF));

        rb.setTotal(0 - refundTotal);
        rb.setDiscount(0 - refundDiscount);
        rb.setNetTotal(0 - refundAmount);
        rb.setVat(0 - refundVat);
        rb.setVatPlusNetTotal(0 - refundVatPlusTotal);

        getBillFacade().create(rb);

        return rb;

    }

    public String returnBill() {
        if (refundingItems.isEmpty()) {
            JsfUtil.addErrorMessage("There is no item to Refund");
            return "";

        }
        if (refundAmount == 0.0) {
            JsfUtil.addErrorMessage("There is no item to Refund");
            return "";
        }
        if (comment == null || comment.trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return "";
        }

        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (getBill().isCancelled()) {
                JsfUtil.addErrorMessage("Already Cancelled. Can not Refund again");
                return "";
            }
            if (!calculateRefundTotal()) {
                return "";
            }

            RefundBill rb = (RefundBill) createReturnBill();

            refundBillItems(rb);

            getBill().setRefunded(true);
            getBill().setRefundedBill(rb);
            getBillFacade().edit(getBill());

            printPreview = true;
            //JsfUtil.addSuccessMessage("Refunded");

        } else {
            JsfUtil.addErrorMessage("No Bill to refund");
            return "";
        }
        //  recreateModel();
        return "";
    }

    private Bill createReturnBill() {
        RefundBill rb = new RefundBill();
        rb.setBilledBill(getBill());
        Date bd = Calendar.getInstance().getTime();
        rb.setBillType(getBill().getBillType());
        rb.setBilledBill(getBill());
        rb.setCreatedAt(bd);
        rb.setComments(comment);
        rb.setCreater(getSessionController().getLoggedUser());
        rb.setCreditCompany(getBill().getCreditCompany());
        rb.setDepartment(getSessionController().getLoggedUser().getDepartment());

        rb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), getBill().getBillType(), BillClassType.RefundBill, BillNumberSuffix.GRNRET));
        rb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), getBill().getBillType(), BillClassType.RefundBill, BillNumberSuffix.GRNRET));

        rb.setToDepartment(getBill().getToDepartment());
        rb.setToInstitution(getBill().getToInstitution());

        rb.setFromDepartment(getBill().getFromDepartment());
        rb.setFromInstitution(getBill().getFromInstitution());

        rb.setInstitution(getSessionController().getLoggedUser().getInstitution());
        rb.setDepartment(getSessionController().getDepartment());

        rb.setNetTotal(refundAmount);
        rb.setPatient(getBill().getPatient());
        rb.setPatientEncounter(getBill().getPatientEncounter());
        rb.setPaymentMethod(paymentMethod);
        rb.setReferredBy(getBill().getReferredBy());
        rb.setTotal(0 - refundAmount);
        rb.setNetTotal(0 - refundAmount);

        getBillFacade().create(rb);

        return rb;

    }

    public void calculateRefundBillFees(RefundBill rb) {
        double s = 0.0;
        double b = 0.0;
        double p = 0.0;
        for (BillItem bi : refundingItems) {
            String sql = "select c from BillFee c where c.billItem.id = " + bi.getId();
            List<BillFee> rbf = getBillFeeFacade().findByJpql(sql);
            for (BillFee bf : rbf) {

                if (bf.getFee().getStaff() == null) {
                    p = p + bf.getFeeValue();
                } else {
                    s = s + bf.getFeeValue();
                }
            }

        }
        rb.setStaffFee(0 - s);
        rb.setPerformInstitutionFee(0 - p);
        getBillFacade().edit(rb);
    }

    public void refundBillItems(RefundBill rb) {
        for (BillItem bi : refundingItems) {
            //set Bill Item as Refunded

            BillItem rbi = new BillItem();
            rbi.copy(bi);
            rbi.invertValue(bi);
            rbi.setBill(rb);
            rbi.setCreatedAt(Calendar.getInstance().getTime());
            rbi.setCreater(getSessionController().getLoggedUser());
            rbi.setReferanceBillItem(bi);
            getBillItemFacede().create(rbi);

            bi.setRefunded(Boolean.TRUE);
            getBillItemFacede().edit(bi);
            BillItem bbb = getBillItemFacade().find(bi.getId());

            String sql = "Select bf From BillFee bf where "
                    + " bf.retired=false and bf.billItem.id=" + bi.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);

            returnBillFee(rb, rbi, tmp);

        }
    }

    public void refundBillItems(RefundBill rb, Payment p) {
        for (BillItem bi : refundingItems) { //set Bill Item as Refunded //set Bill Item as Refunded
            //set Bill Item as Refunded
            //set Bill Item as Refunded //set Bill Item as Refunded
            //set Bill Item as Refunded

            BillItem rbi = new BillItem();
            rbi.copy(bi);
            rbi.invertValue(bi);
            rbi.setBill(rb);
            rbi.setCreatedAt(Calendar.getInstance().getTime());
            rbi.setCreater(getSessionController().getLoggedUser());
            rbi.setReferanceBillItem(bi);
            getBillItemFacede().create(rbi);

            bi.setRefunded(Boolean.TRUE);
            getBillItemFacede().edit(bi);
            BillItem bbb = getBillItemFacade().find(bi.getId());

            String sql = "Select bf From BillFee bf where "
                    + " bf.retired=false and bf.billItem.id=" + bi.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);

            returnBillFee(rb, rbi, tmp);

            //create BillFeePayments For Refund
            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + rbi.getId();
            List<BillFee> tmpC = getBillFeeFacade().findByJpql(sql);
            getOpdPreSettleController().createOpdCancelRefundBillFeePayment(rb, tmpC, p);
            //

            rb.getBillItems().add(rbi);

        }
    }

    public void recreateModel() {
        billForRefund = null;
        refundAmount = 0.0;
        billFees = null;
        refundingItems = null;
//        billFees
        billComponents = null;
        billForRefund = null;
        billItems = null;
        bills = null;
        printPreview = false;
        tempbillItems = null;
        comment = null;
        lazyBills = null;
        searchKeyword = null;
    }

    private void cancelBillComponents(Bill can, BillItem bt) {
        for (BillComponent nB : getBillComponents()) {
            BillComponent bC = new BillComponent();
            bC.setCatId(nB.getCatId());
            bC.setDeptId(nB.getDeptId());
            bC.setInsId(nB.getInsId());
            bC.setDepartment(nB.getDepartment());
            bC.setDeptId(nB.getDeptId());
            bC.setInstitution(nB.getInstitution());
            bC.setItem(nB.getItem());
            bC.setName(nB.getName());
            bC.setPackege(nB.getPackege());
            bC.setSpeciality(nB.getSpeciality());
            bC.setStaff(nB.getStaff());

            bC.setBill(can);
            bC.setBillItem(bt);
            bC.setCreatedAt(new Date());
            bC.setCreater(getSessionController().getLoggedUser());
            getBillCommponentFacade().create(bC);
        }

    }

    private boolean checkPaid() {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.bill.id=" + getBill().getId();
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    private boolean checkPaidIndividual(BillItem bi) {
        String sql = "SELECT bf FROM BillFee bf where bf.retired=false and bf.billItem.id=" + bi.getId();
        List<BillFee> tempFe = getBillFeeFacade().findByJpql(sql);

        for (BillFee f : tempFe) {
            if (f.getPaidValue() != 0.0) {
                return true;
            }

        }
        return false;
    }

    private CancelledBill createOpdCancelBill(Bill originalBill) {
        CancelledBill cb = new CancelledBill();
        if (originalBill == null) {
            return null;
        }

        cb.copy(originalBill);
        cb.copyValue(originalBill);
        cb.invertValue(originalBill);
        cb.setBillType(BillType.OpdBill);
        cb.setBillTypeAtomic(BillTypeAtomic.OPD_BILL_CANCELLATION);

        cb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), originalBill.getToDepartment(), originalBill.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.CAN));
        cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), originalBill.getToDepartment(), originalBill.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.CAN));

        cb.setBalance(0.0);
        cb.setPaymentMethod(paymentMethod);
        cb.setBilledBill(originalBill);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

        return cb;
    }

    private CancelledBill createProfessionalPaymentCancelBill(Bill originalBill) {
        CancelledBill cb = new CancelledBill();
        if (originalBill != null) {
            cb.copy(originalBill);
            cb.copyValue(originalBill);
            cb.invertValue(originalBill);
            cb.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), originalBill.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PROCAN));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), originalBill.getBillType(), BillClassType.CancelledBill, BillNumberSuffix.PROCAN));
        }
        cb.setBalance(0.0);
        cb.setPaymentMethod(paymentMethod);
        cb.setBilledBill(originalBill);
        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());
        cb.setComments(comment);

        return cb;
    }

    private CancelledBill createCahsInOutCancelBill(Bill originalBill, BillNumberSuffix billNumberSuffix) {
        CancelledBill cb = new CancelledBill();
        if (originalBill != null) {
            cb.copy(originalBill);
            cb.invertValue(originalBill);

            cb.setBilledBill(originalBill);

            cb.setDeptId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getDepartment(), cb.getBillType(), BillClassType.CancelledBill, billNumberSuffix));
            cb.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), cb.getBillType(), BillClassType.CancelledBill, billNumberSuffix));

        }

        cb.setBillDate(new Date());
        cb.setBillTime(new Date());
        cb.setCreatedAt(new Date());
        cb.setCreater(getSessionController().getLoggedUser());
        cb.setDepartment(getSessionController().getLoggedUser().getDepartment());
        cb.setInstitution(getSessionController().getInstitution());

        cb.setPaymentMethod(paymentMethod);
        cb.setComments(comment);

        return cb;
    }

    private boolean errorsPresentOnOpdBillCancellation() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (getPaymentMethod() == PaymentMethod.Credit && getBill().getPaidAmount() != 0.0) {
            JsfUtil.addErrorMessage("Already Credit Company Paid For This Bill. Can not cancel.");
            return true;
        }

        if (checkPaid()) {
            JsfUtil.addErrorMessage("Doctor Payment Already Paid So Cant Cancel Bill");
            return true;
        }

        if (getBill().getBillType() == BillType.LabBill) {
            if (patientInvestigation.getCollected()) {
                JsfUtil.addErrorMessage("You can't cancell this bill. Sample is already taken");
                return true;
            }
            if (patientInvestigation.getPrinted()) {
                JsfUtil.addErrorMessage("You can't cancell this bill. Report is already printed");
                return true;
            }

        }
        if (!getWebUserController().hasPrivilege("LabBillCancelSpecial")) {

            ////// // System.out.println("patientInvestigationController.sampledForAnyItemInTheBill(bill) = " + patientInvestigationController.sampledForAnyItemInTheBill(bill));
            if (patientInvestigationController.sampledForAnyItemInTheBill(bill)) {
                JsfUtil.addErrorMessage("Sample Already collected can't cancel");
                return true;
            }
        }

        if (getPaymentMethod() == null) {
            JsfUtil.addErrorMessage("Please select a payment scheme for Cancellation.");
            return true;
        }

        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }

    private boolean errorsPresentOnProfessionalPaymentBillCancellation() {
        if (getBill().isCancelled()) {
            JsfUtil.addErrorMessage("Already Cancelled. Can not cancel again");
            return true;
        }

        if (getBill().isRefunded()) {
            JsfUtil.addErrorMessage("Already Returned. Can not cancel.");
            return true;
        }

        if (getComment() == null || getComment().trim().equals("")) {
            JsfUtil.addErrorMessage("Please enter a comment");
            return true;
        }

        return false;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public void cancelOpdBill() {
        if (getBill() == null) {
            JsfUtil.addErrorMessage("No bill");
            return;
        }
        if (getBill().getId() == null) {
            JsfUtil.addErrorMessage("No Saved bill");
            return;
        }

        if (errorsPresentOnOpdBillCancellation()) {
            return;
        }
        
        if(paymentMethod == PaymentMethod.PatientDeposit){
            if(getBill().getPatient().getHasAnAccount() == null){
                JsfUtil.addErrorMessage("Create Patient Account First");
                return;
            }
            if(!getBill().getPatient().getHasAnAccount()){
                JsfUtil.addErrorMessage("Create Patient Account First");
                return;
            }
        }

        if (!getWebUserController().hasPrivilege("OpdCancel")) {
            JsfUtil.addErrorMessage("You have no privilege to cancel OPD bills. Please contact System Administrator.");
            return;
        }

        CancelledBill cancellationBill = createOpdCancelBill(bill);
        billController.save(cancellationBill);
        System.out.println("cancellationBill.getDepartment().getName() = " + cancellationBill.getDepartment().getName());
        Payment p = getOpdPreSettleController().createPaymentForCancellationsforOPDBill(cancellationBill, paymentMethod);
        List<BillItem> list = cancelBillItems(getBill(), cancellationBill, p);
        cancellationBill.setBillItems(list);
        billFacade.edit(cancellationBill);

        getBill().setCancelled(true);
        getBill().setCancelledBill(cancellationBill);

        billController.save(getBill());
        JsfUtil.addSuccessMessage("Cancelled");

        if (getBill().getPaymentMethod() == PaymentMethod.Credit) {
            //TODO: Manage Credit Balances for Company, Staff
            if (getBill().getToStaff() != null) {
                staffBean.updateStaffCredit(getBill().getToStaff(), 0 - (getBill().getNetTotal() + getBill().getVat()));
                JsfUtil.addSuccessMessage("Staff Credit Updated");
                cancellationBill.setFromStaff(getBill().getToStaff());
                getBillFacade().edit(cancellationBill);
            }
        }

        notificationController.createNotification(cancellationBill);

        bill = billFacade.find(bill.getId());
        printPreview = true;
        comment = null;
//          TODO:         To Implement cancellation approval
//            getEjbApplication().getBillsToCancel().add(cb);
//            JsfUtil.addSuccessMessage("Awaiting Cancellation");
    }

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    public void cancelCashInBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            CancelledBill cb = createCahsInOutCancelBill(bill, BillNumberSuffix.CSINCAN);
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());
            Calendar created = Calendar.getInstance();
            created.setTime(cb.getBilledBill().getCreatedAt());

            //Copy & paste
            if ((now.get(Calendar.DATE) == created.get(Calendar.DATE))
                    || (getBill().getBillType() == BillType.LabBill && getWebUserController().hasPrivilege("LabBillCancelling"))
                    || (getBill().getBillType() == BillType.OpdBill && getWebUserController().hasPrivilege("OpdCancel"))) {

                getBillFacade().create(cb);
                cancelBillItems(cb);
                getBill().setCancelled(true);
                getBill().setCancelledBill(cb);
                getBillFacade().edit(getBill());
                JsfUtil.addSuccessMessage("Cancelled");

                CashTransaction newCt = new CashTransaction();
                newCt.invertQty(getBill().getCashTransaction());

                CashTransaction tmp = getCashTransactionBean().saveCashOutTransaction(newCt, cb, getSessionController().getLoggedUser());
                cb.setCashTransaction(tmp);
                getBillFacade().edit(cb);

//                getCashTransactionBean().deductFromBallance(getSessionController().getLoggedUser().getDrawer(), tmp);
                WebUser wb = getWebUserFacade().find(getSessionController().getLoggedUser().getId());
                getSessionController().setLoggedUser(wb);
                printPreview = true;
            } else {
                getEjbApplication().getBillsToCancel().add(cb);
                JsfUtil.addSuccessMessage("Awaiting Cancellation");
            }

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }

    }

    public void cancelCashOutBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {

            CancelledBill cb = createCahsInOutCancelBill(bill, BillNumberSuffix.CSOUTCAN);
            Calendar now = Calendar.getInstance();
            now.setTime(new Date());
            Calendar created = Calendar.getInstance();
            created.setTime(cb.getBilledBill().getCreatedAt());

            //Copy & paste
            if ((now.get(Calendar.DATE) == created.get(Calendar.DATE))
                    || (getBill().getBillType() == BillType.LabBill && getWebUserController().hasPrivilege("LabBillCancelling"))
                    || (getBill().getBillType() == BillType.OpdBill && getWebUserController().hasPrivilege("OpdCancel"))) {

                getBillFacade().create(cb);
                cancelBillItems(cb);
                getBill().setCancelled(true);
                getBill().setCancelledBill(cb);
                getBillFacade().edit(getBill());
                JsfUtil.addSuccessMessage("Cancelled");

                CashTransaction newCt = new CashTransaction();
                newCt.invertQty(getBill().getCashTransaction());

                CashTransaction tmp = getCashTransactionBean().saveCashInTransaction(newCt, cb, getSessionController().getLoggedUser());
                cb.setCashTransaction(tmp);
                getBillFacade().edit(cb);

//                getCashTransactionBean().addToBallance(getSessionController().getLoggedUser().getDrawer(), tmp);
                WebUser wb = getWebUserFacade().find(getSessionController().getLoggedUser().getId());
                getSessionController().setLoggedUser(wb);

                printPreview = true;
            } else {
                getEjbApplication().getBillsToCancel().add(cb);
                JsfUtil.addSuccessMessage("Awaiting Cancellation");
            }

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
        }

    }

    private void returnBillFee(Bill rb, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.copy(nB);
            bf.invertValue(nB);
            bf.setBill(rb);
            bf.setBillItem(bt);
            bf.setSettleValue(0 - nB.getSettleValue());
            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());

            getBillFeeFacade().create(bf);
        }
    }

    private void returnBillFeeForOpd(List<BillFee> tmp) {
        for (BillFee rbf : tmp) {
            rbf.invertValue(rbf);
            rbf.setSettleValue(0 - rbf.getSettleValue());
            rbf.setCreatedAt(new Date());
            rbf.setCreater(getSessionController().getLoggedUser());
            if (rbf.getId() == null) {
                getBillFeeFacade().create(rbf);
            } else {
                getBillFeeFacade().edit(rbf);
            }
        }
    }

    public void cancelPaymentBill() {
        if (getBill() != null && getBill().getId() != null && getBill().getId() != 0) {
            if (errorsPresentOnProfessionalPaymentBillCancellation()) {
                return;
            }
            CancelledBill cb = createProfessionalPaymentCancelBill(bill);
            //Copy & paste

            getBillFacade().create(cb);
            Payment p = getOpdPreSettleController().createPaymentForCancellationsAndRefunds(cb, paymentMethod);
//            cancelBillItems(cb);
            cancelBillItems(cb, p);
            cancelPaymentItems(bill);
            getBill().setCancelled(true);
            getBill().setCancelledBill(cb);
            getBillFacade().edit(getBill());
            JsfUtil.addSuccessMessage("Cancelled");

            WebUser wb = getCashTransactionBean().saveBillCashInTransaction(cb, getSessionController().getLoggedUser());
            getSessionController().setLoggedUser(wb);
            printPreview = true;

        } else {
            JsfUtil.addErrorMessage("No Bill to cancel");
            return;
        }
    }

    private void cancelPaymentItems(Bill pb) {
        List<BillItem> pbis;
        pbis = getBillItemFacede().findByJpql("SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + pb.getId());
        for (BillItem pbi : pbis) {
            if (pbi.getPaidForBillFee() != null) {
                pbi.getPaidForBillFee().setPaidValue(0.0);
                getBillFeeFacade().edit(pbi.getPaidForBillFee());
            }
        }
    }

    public void approveCancellation() {

        if (billsApproving == null) {
            JsfUtil.addErrorMessage("Select Bill to Approve Cancell");
            return;
        }
        for (Bill b : billsApproving) {

            b.setApproveUser(getSessionController().getCurrent());
            b.setApproveAt(Calendar.getInstance().getTime());
            getBillFacade().create(b);

            cancelBillItems(b);
            b.getBilledBill().setCancelled(true);
            b.getBilledBill().setCancelledBill(b);

            getBillFacade().edit(b);

            ejbApplication.getBillsToCancel().remove(b);

            JsfUtil.addSuccessMessage("Cancelled");

        }

        billForCancel = null;
    }

    public List<Bill> getOpdBillsToApproveCancellation() {
        //////// // System.out.println("1");
        billsToApproveCancellation = ejbApplication.getOpdBillsToCancel();
        return billsToApproveCancellation;
    }

    public List<Bill> getBillsToApproveCancellation() {
        //////// // System.out.println("1");
        billsToApproveCancellation = ejbApplication.getBillsToCancel();
        return billsToApproveCancellation;
    }

    public void setBillsToApproveCancellation(List<Bill> billsToApproveCancellation) {
        this.billsToApproveCancellation = billsToApproveCancellation;
    }

    public List<Bill> getBillsApproving() {
        return billsApproving;
    }

    public void setBillsApproving(List<Bill> billsApproving) {
        this.billsApproving = billsApproving;
    }

    private List<BillItem> cancelBillItems(Bill can) {
        List<BillItem> list = new ArrayList<>();
        for (BillItem nB : getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);

            if (can.getBillType() != BillType.PaymentBill) {
                b.setItem(nB.getItem());
            } else {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            }

            b.setNetValue(0 - nB.getNetValue());
            b.setGrossValue(0 - nB.getGrossValue());
            b.setRate(0 - nB.getRate());

            b.setCatId(nB.getCatId());
            b.setDeptId(nB.getDeptId());
            b.setInsId(nB.getInsId());
            b.setDiscount(nB.getDiscount());
            b.setQty(1.0);
            b.setRate(nB.getRate());

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            b.setPaidForBillFee(nB.getPaidForBillFee());

            getBillItemFacede().create(b);

            cancelBillComponents(can, b);

            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
////////////////////////

            cancelBillFee(can, b, tmp);

            list.add(b);

        }

        return list;
    }

    private List<BillItem> cancelBillItems(Bill can, Payment p) {
        List<BillItem> list = new ArrayList<>();
        for (BillItem nB : getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(can);

            if (can.getBillType() != BillType.PaymentBill) {
                b.setItem(nB.getItem());
            } else {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            }

            b.setNetValue(0 - nB.getNetValue());
            b.setGrossValue(0 - nB.getGrossValue());
            b.setRate(0 - nB.getRate());
            b.setVat(0 - nB.getVat());
            b.setVatPlusNetValue(0 - nB.getVatPlusNetValue());

            b.setCatId(nB.getCatId());
            b.setDeptId(nB.getDeptId());
            b.setInsId(nB.getInsId());
            b.setDiscount(0 - nB.getDiscount());
            b.setQty(1.0);
            b.setRate(nB.getRate());

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            b.setPaidForBillFee(nB.getPaidForBillFee());

            getBillItemFacede().create(b);

            cancelBillComponents(can, b);

            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
            cancelBillFee(can, b, tmp);

            //create BillFeePayments For cancel
            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + b.getId();
            List<BillFee> tmpC = getBillFeeFacade().findByJpql(sql);
            getOpdPreSettleController().createOpdCancelRefundBillFeePayment(can, tmpC, p);
            //

            list.add(b);

        }

        return list;
    }

    private List<BillItem> cancelBillItems(Bill originalBill, Bill cancellationBill, Payment p) {
        List<BillItem> list = new ArrayList<>();
        for (BillItem nB : originalBill.getBillItems()) {
            BillItem b = new BillItem();
            b.setBill(cancellationBill);

            if (cancellationBill.getBillType() != BillType.PaymentBill) {
                b.setItem(nB.getItem());
            } else {
                b.setReferanceBillItem(nB.getReferanceBillItem());
            }

            b.setNetValue(0 - nB.getNetValue());
            b.setGrossValue(0 - nB.getGrossValue());
            b.setRate(0 - nB.getRate());
            b.setVat(0 - nB.getVat());
            b.setVatPlusNetValue(0 - nB.getVatPlusNetValue());

            b.setCatId(nB.getCatId());
            b.setDeptId(nB.getDeptId());
            b.setInsId(nB.getInsId());
            b.setDiscount(0 - nB.getDiscount());
            b.setQty(1.0);
            b.setRate(nB.getRate());

            b.setCreatedAt(new Date());
            b.setCreater(getSessionController().getLoggedUser());

            b.setPaidForBillFee(nB.getPaidForBillFee());

            getBillItemFacede().create(b);

            cancelBillComponents(cancellationBill, b);

            String sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + nB.getId();
            List<BillFee> tmp = getBillFeeFacade().findByJpql(sql);
            cancelBillFee(cancellationBill, b, tmp);

            //create BillFeePayments For cancel
            sql = "Select bf From BillFee bf where bf.retired=false and bf.billItem.id=" + b.getId();
            List<BillFee> tmpC = getBillFeeFacade().findByJpql(sql);
            getOpdPreSettleController().createOpdCancelRefundBillFeePayment(cancellationBill, tmpC, p);
            //

            list.add(b);

        }

        return list;
    }

    public void cancelBillFee(Bill can, BillItem bt, List<BillFee> tmp) {
        for (BillFee nB : tmp) {
            BillFee bf = new BillFee();
            bf.setFee(nB.getFee());
            bf.setPatienEncounter(nB.getPatienEncounter());
            bf.setPatient(nB.getPatient());
            bf.setDepartment(nB.getDepartment());
            bf.setInstitution(nB.getInstitution());
            bf.setSpeciality(nB.getSpeciality());
            bf.setStaff(nB.getStaff());

            bf.setBill(can);
            bf.setBillItem(bt);
            bf.setFeeValue(0 - nB.getFeeValue());
            bf.setFeeGrossValue(0 - nB.getFeeGrossValue());
            bf.setFeeDiscount(0 - nB.getFeeDiscount());
            bf.setSettleValue(0 - nB.getSettleValue());
            bf.setFeeVat(0 - nB.getFeeVat());
            bf.setFeeVatPlusValue(0 - nB.getFeeVatPlusValue());

            bf.setCreatedAt(new Date());
            bf.setCreater(getSessionController().getLoggedUser());

            getBillFeeFacade().create(bf);
        }
    }

    public boolean isShowAllBills() {
        return showAllBills;
    }

    public void setShowAllBills(boolean showAllBills) {
        this.showAllBills = showAllBills;
    }

    public void allBills() {
        showAllBills = true;
    }

    public List<Bill> getBills() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.OpdBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.OpdBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        showAllBills = false;
        return bills;
    }

    public List<Bill> getPos() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyOrder);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyOrder);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getRequests() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyTransferRequest);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyTransferRequest);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getGrns() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyGrnBill);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyGrnBill);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getGrnReturns() {
        if (bills == null) {
            if (txtSearch == null || txtSearch.trim().equals("")) {
                bills = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.PharmacyGrnReturn);
            } else {
                bills = getBillBean().billsFromSearch(txtSearch, getFromDate(), getToDate(), BillType.PharmacyGrnReturn);
            }
            if (bills == null) {
                bills = new ArrayList<>();
            }
        }
        return bills;
    }

    public List<Bill> getInstitutionPaymentBills() {
        if (bills == null) {
            String sql;
            Map temMap = new HashMap();
            if (bills == null) {
                if (txtSearch == null || txtSearch.trim().equals("")) {
                    sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.id in"
                            + "(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType!=:btp and bt.referenceBill.billType!=:btp2) and b.billType=:type and b.createdAt between :fromDate and :toDate order by b.id";

                } else {
                    sql = "select b from BilledBill b where b.retired=false and"
                            + " b.id in(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType!=:btp and bt.referenceBill.billType!=:btp2) "
                            + "and b.billType=:type and b.createdAt between :fromDate and :toDate and ((b.staff.person.name) like '%" + txtSearch.toUpperCase() + "%'  or (b.staff.person.phone) like '%" + txtSearch.toUpperCase() + "%'  or (b.insId) like '%" + txtSearch.toUpperCase() + "%') order by b.id desc  ";
                }

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

    public List<Bill> getUserBills() {
        List<Bill> userBills;
        //////// // System.out.println("getting user bills");
        if (getUser() == null) {
            userBills = new ArrayList<>();
            //////// // System.out.println("user is null");
        } else {
            userBills = getBillBean().billsFromSearchForUser(txtSearch, getFromDate(), getToDate(), getUser(), BillType.OpdBill);
            //////// // System.out.println("user ok");
        }
        if (userBills == null) {
            userBills = new ArrayList<>();
        }
        return userBills;
    }

    public List<Bill> getOpdBills() {
        if (txtSearch == null || txtSearch.trim().equals("")) {
            bills = getBillBean().billsForTheDay(fromDate, toDate, BillType.OpdBill);
        } else {
            bills = getBillBean().billsFromSearch(txtSearch, fromDate, toDate, BillType.OpdBill);
        }

        if (bills == null) {
            bills = new ArrayList<>();
        }
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public String getTxtSearch() {
        return txtSearch;
    }

    public void setTxtSearch(String txtSearch) {
        this.txtSearch = txtSearch;
        //    recreateModel();
    }

    public Bill getBill() {
//        if (bill == null) {
//            bill = new Bill();
//        }
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
//        paymentMethod = bill.getPaymentMethod();
//        createBillItems();
//
//        boolean flag = billController.checkBillValues(bill);
//        bill.setTransError(flag);
    }

    public String navigateToCancelOpdBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        //System.out.println("bill = " + bill.getIdStr());
        
        if (configOptionApplicationController.getBooleanValueByKey("Set the Original Bill PaymentMethod to Cancelation Bill")) {
            paymentMethod = bill.getPaymentMethod();
        } else {
            paymentMethod = PaymentMethod.Cash;
        }
        //System.out.println("Cencel = " + paymentMethod);
        createBillItemsAndBillFees();
        boolean flag = billController.checkBillValues(bill);
        bill.setTransError(flag);
        printPreview = false;
        return "/opd/bill_cancel?faces-redirect=true;";
    }

    public boolean chackRefundORCancelBill(Bill bill) {
        boolean result = false;
        //System.out.println("bill.getCreatedAt = " + bill.getCreatedAt());
        //System.out.println("After 24H Date = " + commonFunctionsController.dateAfter24Hours(bill.getCreatedAt()));
        //System.out.println("curret Date = " + new Date());
        if (commonFunctionsController.dateAfter24Hours(bill.getCreatedAt()).after(new Date())) {
            result = true;
            //System.out.println("Can Refund or Cancel");
        } else {
            result = false;
            //System.out.println("Can not Refund Or Cancel");
        }
        return result;
    }

    public String navigateToViewSingleOpdBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        loadBillItemsAndBillFees(bill);
        if (configOptionController.getBooleanValueByKey("OPD Bill Cancelation is Limited to the Last 24 hours", OptionScope.APPLICATION, null, null, null)) {
            opdBillCancellationSameDay = chackRefundORCancelBill(bill);
        } else {
            opdBillCancellationSameDay = true;
        }

        if (configOptionController.getBooleanValueByKey("OPD Bill Refund Allowed to the Last 24 hours", OptionScope.APPLICATION, null, null, null)) {
            opdBillRefundAllowedSameDay = chackRefundORCancelBill(bill);
        } else {
            opdBillRefundAllowedSameDay = true;
        }
        paymentMethod = bill.getPaymentMethod();
        printPreview = false;
        return "/opd/bill_reprint?faces-redirect=true;";
    }

    public String navigateToViewOpdBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        if (configOptionController.getBooleanValueByKey("OPD Bill Cancelation is Limited to the Last 24 hours", OptionScope.APPLICATION, null, null, null)) {
            opdBillCancellationSameDay = chackRefundORCancelBill(bill);
            //System.out.println("opdBillCancellationSameDay = " + opdBillCancellationSameDay);
//            if (opdBillCancellationSameDay) {
//                System.out.println("Can Cancel");
//            } else {
//                System.out.println("Can not Cancel");
//            }
        } else {
            opdBillCancellationSameDay = true;
            //System.out.println("***Can Cancel***");
        }

        if (configOptionController.getBooleanValueByKey("OPD Bill Refund Allowed to the Last 24 hours", OptionScope.APPLICATION, null, null, null)) {
            opdBillRefundAllowedSameDay = chackRefundORCancelBill(bill);
            //System.out.println("opdBillRefundAllowedSameDay = " + opdBillRefundAllowedSameDay);
//            if (opdBillRefundAllowedSameDay) {
//                System.out.println("Can Refund");
//            } else {
//                System.out.println("Can not Refund");
//            }
        } else {
            opdBillRefundAllowedSameDay = true;
            //System.out.println("***Can Refund***");
        }

        paymentMethod = bill.getPaymentMethod();
        createBillItemsAndBillFees();
        billBean.checkBillItemFeesInitiated(bill);

        boolean flag = billController.checkBillValues(bill);
        bill.setTransError(flag);
        printPreview = false;

        return "/opd/bill_reprint?faces-redirect=true;";
    }

    public String navigateViewBillByBillTypeAtomic() {
        BillTypeAtomic billTypeAtomic = bill.getBillTypeAtomic();
        switch (billTypeAtomic) {
            case PHARMACY_RETAIL_SALE_CANCELLED:
                pharmacyBillSearch.setBill(bill);
                return pharmacyBillSearch.navigateToViewPharmacyGrn();
            case OPD_BILL_REFUND:
                return navigateToViewOpdBill();

            case OPD_BILL_CANCELLATION:
                return navigateToViewOpdBill();

            case OPD_BILL_PAYMENT_COLLECTION_AT_CASHIER:
                return navigateToViewOpdBill();

            case OPD_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION:
                return navigateToViewOpdBill();

            case OPD_PROFESSIONAL_PAYMENT_BILL:
                return navigateToViewOpdBill();

            case OPD_BILL_WITH_PAYMENT:
                return navigateToViewOpdBill();

            case OPD_BATCH_BILL_WITH_PAYMENT:
                return navigateToViewOpdBill();

            case CHANNEL_BOOKING_WITH_PAYMENT:
                return "";

            case CHANNEL_REFUND:
                return "";

            case CHANNEL_PAYMENT_FOR_BOOKING_BILL:

        }

        return "";
    }

    public String navigateToViewOpdBillNewWindow() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        createBillItemsAndBillFees();
        billBean.checkBillItemFeesInitiated(bill);
        return "/opd/bill_view?faces-redirect=true;";
    }

    public String navigateToViewCollectingCentreBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        paymentMethod = bill.getPaymentMethod();
        createBillItemsAndBillFees();
        billBean.checkBillItemFeesInitiated(bill);

        boolean flag = billController.checkBillValues(bill);
        bill.setTransError(flag);
        printPreview = false;
        return "/collecting_centre/bill_reprint?faces-redirect=true;";
    }

    public String navigateToRefundOpdBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        
        if(configOptionApplicationController.getBooleanValueByKey("Set the Original Bill PaymentMethod to Refunded Bill")){
            paymentMethod = getBill().getPaymentMethod();
        }else{
            paymentMethod = PaymentMethod.Cash;
        }
        //System.out.println("Refund"+ paymentMethod);
        try {
            createBillItemsAndBillFeesForOpdRefund();
        } catch (IllegalAccessException ex) {
            Logger.getLogger(BillSearch.class
                    .getName()).log(Level.SEVERE, null, ex);
            JsfUtil.addErrorMessage(ex.getMessage());
            return "";

        } catch (InvocationTargetException ex) {
            Logger.getLogger(BillSearch.class
                    .getName()).log(Level.SEVERE, null, ex);
            JsfUtil.addErrorMessage(ex.getMessage());
            return "";
        }
        if (configOptionApplicationController.getBooleanValueByKey("To Refunded the Full Value of the Bill")) {
            fillBillFees();
        }
        printPreview = false;
        return "/opd/bill_refund?faces-redirect=true;";
    }

    public String navigateToRefundCollectingCentreBill() {
        if (bill == null) {
            JsfUtil.addErrorMessage("Nothing to cancel");
            return "";
        }
        paymentMethod = bill.getPaymentMethod();
        createBillItemsAndBillFees();
        boolean flag = billController.checkBillValues(bill);
        bill.setTransError(flag);
        printPreview = false;
        return "/collecting_centre/bill_refund?faces-redirect=true;";
    }

    public List<BillEntry> getBillEntrys() {
        return billEntrys;
    }

    public void setBillEntrys(List<BillEntry> billEntrys) {
        this.billEntrys = billEntrys;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    private void createBillItemsAndBillFeesForOpdRefund() throws IllegalAccessException, InvocationTargetException {
        refundingBill = new RefundBill();
        BeanUtils.copyProperties(refundingBill, bill);

        // Set unique properties for refundingBill
        refundingBill.setBillClassType(BillClassType.RefundBill);
        refundingBill.setBillDate(new Date());
        refundingBill.setBillTime(new Date());
        refundingBill.setCreatedAt(new Date());
        refundingBill.setCreater(sessionController.getLoggedUser());
        refundingBill.setDepartment(sessionController.getLoggedUser().getDepartment());
        refundingBill.setInstitution(sessionController.getLoggedUser().getInstitution());

        refundingBill.setPatient(bill.getPatient());
        refundingBill.setReferenceInstitution(bill.getReferenceInstitution());
        refundingBill.setReferredBy(bill.getReferredBy());
        refundingBill.setReferredByInstitution(bill.getReferredByInstitution());
        refundingBill.setBillClassType(BillClassType.RefundBill);
        refundingBill.setBillDate(new Date());
        refundingBill.setBillTime(new Date());
        refundingBill.setBilledBill(bill);
        refundingBill.setReferenceBill(bill);
        refundingBill.setForwardReferenceBill(bill);
        refundingBill.setId(null);
        refundingBill.setBillItems(new ArrayList<>());
        refundingBill.setBillFees(null);
        refundingBill.setBackwardReferenceBills(null);

        billItems = new ArrayList<>();
        refundingFees = new ArrayList<>();
        refundingItems = new ArrayList<>();

        List<BillItem> billedBillItems = billController.billItemsOfBill(bill);

        for (BillItem bi : billedBillItems) {

            BillItem nbi = new BillItem();
            BeanUtils.copyProperties(nbi, bi);
            nbi.setBill(refundingBill);
            nbi.setReferanceBillItem(bi);
            nbi.setReferenceBill(bill);
            nbi.setId(null);
            nbi.setBillFees(new ArrayList<>());

            List<BillFee> billBillFees = billController.billFeesOfBillItem(bi);
            for (BillFee bf : billBillFees) {

                BillFee nbf = new BillFee();
                BeanUtils.copyProperties(nbf, bf);
                nbf.setBill(refundingBill);
                nbf.setBillItem(nbi);
                nbf.setId(null);
                nbf.setReferenceBillFee(bf);
                nbf.setReferenceBillItem(bi);
                nbf.setFeeValue(0.0);
                nbi.getBillFees().add(nbf);
                refundingFees.add(nbf);

            }
            refundingBill.getBillItems().add(nbi);
            refundingItems.add(nbi);

        }

    }

    private void createBillItemsAndBillFees() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "SELECT b FROM BillItem b"
                + "  WHERE b.retired=false "
                + " and b.bill=:b";
        hm.put("b", getBillSearch());
        billItems = getBillItemFacede().findByJpql(sql, hm);

        for (BillItem bi : billItems) {
            sql = "SELECT bi FROM BillItem bi where bi.retired=false and bi.referanceBillItem.id=" + bi.getId();
            BillItem rbi = getBillItemFacade().findFirstByJpql(sql);

            if (rbi != null) {
                bi.setTransRefund(true);
            } else {
                bi.setTransRefund(false);
            }
        }

    }

    private void loadBillItemsAndBillFees(Bill billToLoad) {
        if (billToLoad == null) {
            return;
        }
        if (billToLoad.getBillItems() == null || billToLoad.getBillItems().isEmpty()) {
            billToLoad.setBillItems(billBean.fillBillItems(billToLoad));
        }
        for (BillItem bi : billToLoad.getBillItems()) {
            if (bi.getBillFees() == null || bi.getBillFees().isEmpty()) {
                bi.setBillFees(billBean.fillBillItemFees(bi));
            }
        }
    }

    private void createBillItemsForRetire() {
        String sql = "";
        HashMap hm = new HashMap();
        sql = "SELECT b FROM BillItem b WHERE "
                + " b.bill=:b";
        hm.put("b", getBillSearch());
        billItems = getBillItemFacede().findByJpql(sql, hm);

    }

    public List<PharmaceuticalBillItem> getPharmacyBillItems() {
        List<PharmaceuticalBillItem> tmp = new ArrayList<>();
        if (getBill() != null) {
            String sql = "SELECT b FROM PharmaceuticalBillItem b WHERE b.billItem.retired=false and b.billItem.bill.id=" + getBill().getId();
            tmp = getPharmaceuticalBillItemFacade().findByJpql(sql);
        }

        return tmp;
    }

    public List<BillComponent> getBillComponents() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillComponent b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billComponents = getBillCommponentFacade().findByJpql(sql);
            if (billComponents == null) {
                billComponents = new ArrayList<>();
            }
        }
        return billComponents;
    }

    private List<BillFee> billFeesList;

    public List<BillFee> getBillFees() {
        if (getBill() != null) {
            if (billFees == null || billForRefund == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
            }
        }

        if (billFees == null) {
            billFees = new ArrayList<>();
        }

        return billFees;
    }

    public List<BillFee> getBillFees2() {
        System.out.println("getBill.getId() = " + getBill().getId());
        if (billFees == null) {
            if (getBill() != null) {
                Map m = new HashMap();
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill=:cb";
                m.put("cb", getBill());
                billFees = getBillFeeFacade().findByJpql(sql, m);
                System.out.println("billFees = " + billFees.size());
            }

            if (getBillSearch() != null) {
                Map m = new HashMap();
                String sql = "SELECT b FROM BillFee b WHERE b.bill=:cb";
                m.put("cb", getBill());
                billFees = getBillFeeFacade().findByJpql(sql, m);
                System.out.println("billFees2 = " + billFees.size());
            }

            if (billFees == null) {
                billFees = new ArrayList<>();
                System.out.println("this = " + this);
            }
        }

        return billFees;
    }

    public List<BillFee> getPayingBillFees() {
        if (getBill() != null) {
            String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getBill().getId();
            billFees = getBillFeeFacade().findByJpql(sql);
            if (billFees == null) {
                billFees = new ArrayList<>();
            }

        }

        return billFees;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public void setBillComponents(List<BillComponent> billComponents) {
        this.billComponents = billComponents;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public BillItemFacade getBillItemFacede() {
        return billItemFacede;
    }

    public void setBillItemFacede(BillItemFacade billItemFacede) {
        this.billItemFacede = billItemFacede;
    }

    public BillComponentFacade getBillCommponentFacade() {
        return billCommponentFacade;
    }

    public void setBillCommponentFacade(BillComponentFacade billCommponentFacade) {
        this.billCommponentFacade = billCommponentFacade;
    }

    private void setRefundAttribute() {
        billForRefund.setBalance(getBill().getBalance());

        billForRefund.setBillDate(Calendar.getInstance().getTime());
        billForRefund.setBillTime(Calendar.getInstance().getTime());
        billForRefund.setCreater(getSessionController().getLoggedUser());
        billForRefund.setCreatedAt(Calendar.getInstance().getTime());

        billForRefund.setBillType(getBill().getBillType());
        billForRefund.setBilledBill(getBill());

        billForRefund.setCatId(getBill().getCatId());
        billForRefund.setCollectingCentre(getBill().getCollectingCentre());
        billForRefund.setCreditCardRefNo(getBill().getCreditCardRefNo());
        billForRefund.setCreditCompany(getBill().getCreditCompany());

        billForRefund.setDepartment(getBill().getDepartment());
        billForRefund.setDeptId(getBill().getDeptId());
        billForRefund.setDiscount(getBill().getDiscount());

        billForRefund.setDiscountPercent(getBill().getDiscountPercent());
        billForRefund.setFromDepartment(getBill().getFromDepartment());
        billForRefund.setFromInstitution(getBill().getFromInstitution());
        billForRefund.setFromStaff(getBill().getFromStaff());

        billForRefund.setInsId(getBill().getInsId());
        billForRefund.setInstitution(getBill().getInstitution());

        billForRefund.setPatient(getBill().getPatient());
        billForRefund.setPatientEncounter(getBill().getPatientEncounter());
        billForRefund.setPaymentScheme(getBill().getPaymentScheme());
        billForRefund.setPaymentMethod(getBill().getPaymentMethod());
        billForRefund.setPaymentSchemeInstitution(getBill().getPaymentSchemeInstitution());

        billForRefund.setReferredBy(getBill().getReferredBy());
        billForRefund.setReferringDepartment(getBill().getReferringDepartment());

        billForRefund.setStaff(getBill().getStaff());

        billForRefund.setToDepartment(getBill().getToDepartment());
        billForRefund.setToInstitution(getBill().getToInstitution());
        billForRefund.setToStaff(getBill().getToStaff());
        billForRefund.setTotal(calTot());
        //Need To Add Net Total Logic
        billForRefund.setNetTotal(billForRefund.getTotal());
    }

    public double calTot() {
        if (getBillFees() == null) {
            return 0.0;
        }
        double tot = 0.0;
        for (BillFee f : getBillFees()) {
            //////// // System.out.println("Tot" + f.getFeeValue());
            tot += f.getFeeValue();
        }
        getBillForRefund().setTotal(tot);
        return tot;
    }

    public RefundBill getBillForRefund() {

        if (billForRefund == null) {
            billForRefund = new RefundBill();
            setRefundAttribute();
        }

        return billForRefund;
    }

    public void setBillForRefund(RefundBill billForRefund) {
        this.billForRefund = billForRefund;
    }

    public double getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(double refundAmount) {
        this.refundAmount = refundAmount;
    }

    public WebUserController getWebUserController() {
        return webUserController;
    }

    public void setWebUserController(WebUserController webUserController) {
        this.webUserController = webUserController;
    }

    public Bill getBillForCancel() {
        return billForCancel;
    }

    public void setBillForCancel(Bill billForCancel) {
        this.billForCancel = billForCancel;
    }

    public boolean isPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(boolean printPreview) {
        this.printPreview = printPreview;
    }

    public List<BillItem> getTempbillItems() {
        if (tempbillItems == null) {
            tempbillItems = new ArrayList<>();
        }
        return tempbillItems;
    }

    public void setTempbillItems(List<BillItem> tempbillItems) {
        this.tempbillItems = tempbillItems;
    }

    public void resetLists() {
        recreateModel();
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        //  resetLists();
        this.toDate = toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        //resetLists();
        this.fromDate = fromDate;

    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public PharmaceuticalBillItemFacade getPharmaceuticalBillItemFacade() {
        return pharmaceuticalBillItemFacade;
    }

    public void setPharmaceuticalBillItemFacade(PharmaceuticalBillItemFacade pharmaceuticalBillItemFacade) {
        this.pharmaceuticalBillItemFacade = pharmaceuticalBillItemFacade;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public ItemBatchFacade getItemBatchFacade() {
        return itemBatchFacade;
    }

    public void setItemBatchFacade(ItemBatchFacade itemBatchFacade) {
        this.itemBatchFacade = itemBatchFacade;
    }

    public List<Bill> getFilteredBill() {
        return filteredBill;
    }

    public void setFilteredBill(List<Bill> filteredBill) {
        this.filteredBill = filteredBill;
    }

    public PharmacyPreSettleController getPharmacyPreSettleController() {
        return pharmacyPreSettleController;
    }

    public void setPharmacyPreSettleController(PharmacyPreSettleController pharmacyPreSettleController) {
        this.pharmacyPreSettleController = pharmacyPreSettleController;
    }

    public LazyDataModel<Bill> getLazyBills() {
        return lazyBills;
    }

    public void setLazyBills(LazyDataModel<Bill> lazyBills) {
        this.lazyBills = lazyBills;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public LazyDataModel<BillItem> getSearchBillItems() {
        return searchBillItems;
    }

    public void setSearchBillItems(LazyDataModel<BillItem> searchBillItems) {
        this.searchBillItems = searchBillItems;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
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

    public Bill getBillSearch() {
        return bill;
    }

    public Institution getCreditCompany() {
        if (getBillSearch().getPatientEncounter() == null) {
            creditCompany = getBillSearch().getCreditCompany();
        } else {
            creditCompany = getBillSearch().getPatientEncounter().getCreditCompany();
        }

        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public void setBillSearch(Bill bill) {
        recreateModel();
        this.bill = bill;
        paymentMethod = bill.getPaymentMethod();
        createBillItemsForRetire();
        createBillFees();
        createBillItemsAll();
        if (getBill().getBillType() == BillType.CollectingCentreBill) {
            createCollectingCenterfees(getBill());

        }
        if (getBill().getRefundedBill() != null) {
            bills = new ArrayList<>();
            String sql;
            Map m = new HashMap();
            sql = "Select b from Bill b where "
                    + " b.billedBill.id=:bid";
            m.put("bid", getBill().getId());
            bills = getBillFacade().findByJpql(sql, m);
            for (Bill b : bills) {
                createCollectingCenterfees(b);
            }
        }
    }

    public void createCollectingCenterfees(Bill b) {
        AgentHistory ah = new AgentHistory();
        if (b.getCancelledBill() != null) {
            b.getCancelledBill().setTransTotalCCFee(0.0);
            b.getCancelledBill().setTransTotalWithOutCCFee(0.0);
            for (BillItem bi : b.getCancelledBill().getBillItems()) {
                bi.setTransCCFee(0.0);
                bi.setTransWithOutCCFee(0.0);
                for (BillFee bf : createBillFees(bi)) {
                    if (bf.getFee().getFeeType() == FeeType.CollectingCentre) {
                        bi.setTransCCFee(bi.getTransCCFee() + bf.getFeeValue());
                    } else {
                        bi.setTransWithOutCCFee(bi.getTransWithOutCCFee() + bf.getFeeValue() + bf.getFeeVat());
//                        bi.setTransWithOutCCFee(bi.getTransWithOutCCFee() + bf.getFeeValue());  add vat to hos fee
                    }
                }
                b.getCancelledBill().setTransTotalCCFee(b.getCancelledBill().getTransTotalCCFee() + bi.getTransCCFee());
                b.getCancelledBill().setTransTotalWithOutCCFee(b.getCancelledBill().getTransTotalWithOutCCFee() + bi.getTransWithOutCCFee());
            }
            ah = fetchCCHistory(b.getCancelledBill());
            if (ah != null) {
                b.getCancelledBill().setTransCurrentCCBalance(ah.getBeforeBallance() + ah.getTransactionValue());
            }

        } else if (b.getRefundedBill() != null) {
            b.getRefundedBill().setTransTotalCCFee(0.0);
            b.getRefundedBill().setTransTotalWithOutCCFee(0.0);
            for (BillItem bi : b.getRefundedBill().getBillItems()) {
                bi.setTransCCFee(0.0);
                bi.setTransWithOutCCFee(0.0);
                for (BillFee bf : createBillFees(bi)) {
                    if (bf.getFee().getFeeType() == FeeType.CollectingCentre) {
                        bi.setTransCCFee(bi.getTransCCFee() + bf.getFeeValue());
                    } else {
                        bi.setTransWithOutCCFee(bi.getTransWithOutCCFee() + bf.getFeeValue() + bf.getFeeVat());
//                        bi.setTransWithOutCCFee(bi.getTransWithOutCCFee() + bf.getFeeValue()); add vat for hos fee
                    }
                }
                b.getRefundedBill().setTransTotalCCFee(b.getRefundedBill().getTransTotalCCFee() + bi.getTransCCFee());
                b.getRefundedBill().setTransTotalWithOutCCFee(b.getRefundedBill().getTransTotalWithOutCCFee() + bi.getTransWithOutCCFee());
            }
            ah = fetchCCHistory(b.getRefundedBill());
            if (ah != null) {
                b.getRefundedBill().setTransCurrentCCBalance(ah.getBeforeBallance() + ah.getTransactionValue());
            }
        } else {
            b.setTransTotalCCFee(0.0);
            b.setTransTotalWithOutCCFee(0.0);
            for (BillItem bi : b.getBillItems()) {
                bi.setTransCCFee(0.0);
                bi.setTransWithOutCCFee(0.0);
                for (BillFee bf : createBillFees(bi)) {
                    if (bf.getFee().getFeeType() == FeeType.CollectingCentre) {
                        bi.setTransCCFee(bi.getTransCCFee() + bf.getFeeValue());
                    } else {
                        bi.setTransWithOutCCFee(bi.getTransWithOutCCFee() + bf.getFeeValue() + bf.getFeeVat());
//                        bi.setTransWithOutCCFee(bi.getTransWithOutCCFee() + bf.getFeeValue());add vat for hos fee
                    }
                }
                b.setTransTotalCCFee(b.getTransTotalCCFee() + bi.getTransCCFee());
                b.setTransTotalWithOutCCFee(b.getTransTotalWithOutCCFee() + bi.getTransWithOutCCFee());
            }
            ah = fetchCCHistory(b);
            if (ah != null) {
                b.setTransCurrentCCBalance(ah.getBeforeBallance() + ah.getTransactionValue());
            }
        }

    }

    public List<BillFee> createBillFees(BillItem bi) {
        List<BillFee> bfs = new ArrayList<>();
        String sql = "SELECT b FROM BillFee b WHERE b.billItem.id=" + bi.getId();
        bfs = getBillFeeFacade().findByJpql(sql);
        return bfs;
    }

    public AgentHistory fetchCCHistory(Bill b) {
        String sql;
        Map m = new HashMap();

        sql = " select ah from AgentHistory ah where ah.retired=false "
                + " and ah.bill.id=" + b.getId();
        AgentHistory ah = agentHistoryFacade.findFirstByJpql(sql);

        return ah;
    }

    public double getRefundTotal() {
        return refundTotal;
    }

    public void setRefundTotal(double refundTotal) {
        this.refundTotal = refundTotal;
    }

    public double getRefundDiscount() {
        return refundDiscount;
    }

    public void setRefundDiscount(double refundDiscount) {
        this.refundDiscount = refundDiscount;
    }

    public double getRefundMargin() {
        return refundMargin;
    }

    public void setRefundMargin(double refundMargin) {
        this.refundMargin = refundMargin;
    }

    public StaffBean getStaffBean() {
        return staffBean;
    }

    public void setStaffBean(StaffBean staffBean) {
        this.staffBean = staffBean;
    }

    public BillController getBillController() {
        return billController;
    }

    public void setBillController(BillController billController) {
        this.billController = billController;
    }

    public List<BillFee> getBillFeesList() {
        return billFeesList;
    }

    public void setBillFeesList(List<BillFee> billFeesList) {
        this.billFeesList = billFeesList;
    }

    public PatientInvestigationController getPatientInvestigationController() {
        return patientInvestigationController;
    }

    public void setPatientInvestigationController(PatientInvestigationController patientInvestigationController) {
        this.patientInvestigationController = patientInvestigationController;
    }

    public List<BillItem> getBillItemList() {
        return billItemList;
    }

    public void setBillItemList(List<BillItem> billItemList) {
        this.billItemList = billItemList;
    }

    public OpdPreSettleController getOpdPreSettleController() {
        return opdPreSettleController;
    }

    public void setOpdPreSettleController(OpdPreSettleController opdPreSettleController) {
        this.opdPreSettleController = opdPreSettleController;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public List<BillSummery> getBillSummeries() {
        return billSummeries;
    }

    public void setBillSummeries(List<BillSummery> billSummeries) {
        this.billSummeries = billSummeries;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void calculateTotalForBillSummaries() {
        cashTotal = 0;
        slipTotal = 0;
        creditCardTotal = 0;
        creditTotal = 0;
        multiplePaymentsTotal = 0;
        patientDepositsTotal = 0;

        if (getBillSummeries() == null) {
            return;
        }
        if (getBillSummeries().isEmpty()) {
            return;
        }
        for (BillSummery bs : getBillSummeries()) {
            if (bs.getPaymentMethod() == PaymentMethod.Cash) {
                cashTotal += bs.getNetTotal();
            } else if (bs.getPaymentMethod() == PaymentMethod.Slip) {
                slipTotal += bs.getNetTotal();
            } else if (bs.getPaymentMethod() == PaymentMethod.Card) {
                creditCardTotal += bs.getNetTotal();
            } else if (bs.getPaymentMethod() == PaymentMethod.Credit) {
                creditTotal += bs.getNetTotal();
            } else if (bs.getPaymentMethod() == PaymentMethod.MultiplePaymentMethods) {
                multiplePaymentsTotal += bs.getNetTotal();
            } else if (bs.getPaymentMethod() == PaymentMethod.PatientDeposit) {
                patientDepositsTotal += bs.getNetTotal();
            }
        }
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Department getDepartment() {
        if (department == null) {
            sessionController.getLoggedUser().getDepartment();
        }
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public BillClassType getBillClassType() {
        return billClassType;
    }

    public void setBillClassType(BillClassType billClassType) {
        this.billClassType = billClassType;
    }

    public EmailFacade getEmailFacade() {
        return emailFacade;
    }

    public String getEncryptedPatientReportId() {
        return encryptedPatientReportId;
    }

    public void setEncryptedPatientReportId(String encryptedPatientReportId) {
        this.encryptedPatientReportId = encryptedPatientReportId;
    }

    public String getEncryptedExpiary() {
        return encryptedExpiary;
    }

    public void setEncryptedExpiary(String encryptedExpiary) {
        this.encryptedExpiary = encryptedExpiary;
    }

    public SecurityController getSecurityController() {
        return securityController;
    }

    public BillSummery getBillSummery() {
        return billSummery;
    }

    public void setBillSummery(BillSummery billSummery) {
        this.billSummery = billSummery;
    }

    public BillLight getBillLight() {
        return billLight;
    }

    public void setBillLight(BillLight billLight) {
        bill = billFacade.find(billLight.getId());
        this.billLight = billLight;
    }

    private long billTypeSummaryId = 0; // Counter for BillTypeSummary IDs
    private long paymentSummaryId = 0; // Counter for PaymentSummary IDs

    public OverallSummary aggregateBillSummaries(List<BillSummery> billSummaries) {
        Map<String, BillTypeSummary> summaryMap = new HashMap<>();
        double billPaymentTotal = 0;
        for (BillSummery bs : billSummaries) {
            String billType = (bs.getBillType() != null) ? bs.getBillType().toString() : "UnknownBillType";
            String billClassType = (bs.getBillClassType() != null) ? bs.getBillClassType().toString() : "UnknownBillClassType";
            String key = billType + ":" + billClassType;

            BillTypeSummary billTypeSummary = summaryMap.get(key);
            if (billTypeSummary == null) {
                billTypeSummary = new BillTypeSummary(bs.getBillType(), bs.getBillClassType(), new ArrayList<>(), billPaymentTotal);
                summaryMap.put(key, billTypeSummary);
            }

            // Aggregate or update the payment summary for the billTypeSummary
            updatePaymentSummary(billTypeSummary, bs);
        }

        return new OverallSummary(new ArrayList<>(summaryMap.values()));
    }

    private void updatePaymentSummary(BillTypeSummary billTypeSummary, BillSummery bs) {
        boolean found = false;

        for (PaymentSummary ps : billTypeSummary.getPaymentSummaries()) {
            if (ps.getPaymentMethod() == paymentMethod.MultiplePaymentMethods && bs.getPaymentMethod() == paymentMethod.MultiplePaymentMethods) {
                return;
            }
            if (ps.getPaymentMethod().equals(bs.getPaymentMethod())) {
                // Aggregate existing payment summary
                ps.setTotal(ps.getTotal() + bs.getTotal());
                ps.setDiscount(ps.getDiscount() + bs.getDiscount());
                ps.setNetTotal(ps.getNetTotal() + bs.getNetTotal());
                ps.setTax(ps.getTax() + bs.getTax());
                ps.setCount(ps.getCount() + bs.getCount());
                break;
            }
        }
        if (!found) {

            // Create a new payment summary and add it
            double billPaymentTotal;
            PaymentSummary newPs = new PaymentSummary(bs.getPaymentMethod(), bs.getTotal(), bs.getDiscount(), bs.getNetTotal(), bs.getTax(), bs.getCount());
            if (newPs.getPaymentMethod() == paymentMethod.MultiplePaymentMethods) {
                return;
            }
            billTypeSummary.getPaymentSummaries().add(newPs);
            billPaymentTotal = bs.getNetTotal();
            double biltypeSum = billTypeSummary.getBillTypeTotal() + billPaymentTotal;
            billTypeSummary.setBillTypeTotal(biltypeSum);
        }
    }

    public OverallSummary getOverallSummary() {
        return overallSummary;
    }

    public void setOverallSummary(OverallSummary overallSummary) {
        this.overallSummary = overallSummary;
    }

    public List<BillFee> getRefundingFees() {
        return refundingFees;
    }

    public void setRefundingFees(List<BillFee> refundingFees) {
        this.refundingFees = refundingFees;
    }

    public Bill getRefundingBill() {
        return refundingBill;
    }

    public void setRefundingBill(Bill refundingBill) {
        this.refundingBill = refundingBill;
    }

    private boolean invertBillValuesAndCalculate(Bill b) {
        if (b == null) {
            return false;
        }
        if (b.getBillItems() == null) {
            return false;
        }
        if (b.getBillItems().isEmpty()) {
            return false;
        }
        double billTotal = 0.0;

        refundAmount = 0;
        refundDiscount = 0;
        refundTotal = 0;
        refundMargin = 0;
        refundVat = 0;
        refundVatPlusTotal = 0;

        for (BillItem bi : b.getBillItems()) {
            double billItemTotal = 0.0;
            for (BillFee bf : bi.getBillFees()) {
                System.out.println("bf = " + bf.getFee().getName());
                if (bf.getFeeValue() != 0.0) {
                    System.out.println(bf.getFee().getName() + "  " + bf.getFeeValue());
                    double bfv = bf.getFeeValue();
                    bfv = Math.abs(bfv);
                    bf.setFeeValue(0 - bfv);
                    billItemTotal += bfv;
                }
            }
            bi.setNetValue(billItemTotal);
            bi.setGrossValue(billItemTotal);
            bi.setMarginValue(0);
            bi.setDiscount(0);
            bi.setVat(0);
            bi.setVatPlusNetValue(billItemTotal);
            refundTotal += bi.getGrossValue();
            refundAmount += bi.getNetValue();
            refundMargin += bi.getMarginValue();
            refundDiscount += bi.getDiscount();
            refundVat += bi.getVat();
            refundVatPlusTotal += bi.getVatPlusNetValue();
        }
        b.setTotal(0 - Math.abs(billTotal));
        b.setNetTotal(0 - Math.abs(billTotal));
        b.setTotal(0 - Math.abs(refundTotal));
        b.setDiscount(0 - Math.abs(refundDiscount));
        b.setNetTotal(0 - Math.abs(refundAmount));
        b.setVat(0 - Math.abs(refundVat));
        b.setVatPlusNetTotal(0 - Math.abs(refundVatPlusTotal));
        return true;

    }

    public Bill getCurrentRefundBill() {
        if (currentRefundBill == null && bill != null) {
            currentRefundBill = bill;
        }
        return currentRefundBill;
    }

    public void setCurrentRefundBill(Bill currentRefundBill) {
        this.currentRefundBill = currentRefundBill;
    }

    public double getCashTotal() {
        return cashTotal;
    }

    public void setCashTotal(double cashTotal) {
        this.cashTotal = cashTotal;
    }

    public double getSlipTotal() {
        return slipTotal;
    }

    public void setSlipTotal(double slipTotal) {
        this.slipTotal = slipTotal;
    }

    public double getCreditTotal() {
        return creditTotal;
    }

    public void setCreditTotal(double creditTotal) {
        this.creditTotal = creditTotal;
    }

    public double getCreditCardTotal() {
        return creditCardTotal;
    }

    public void setCreditCardTotal(double creditCardTotal) {
        this.creditCardTotal = creditCardTotal;
    }

    public double getMultiplePaymentsTotal() {
        return multiplePaymentsTotal;
    }

    public void setMultiplePaymentsTotal(double multiplePaymentsTotal) {
        this.multiplePaymentsTotal = multiplePaymentsTotal;
    }

    public double getPatientDepositsTotal() {
        return patientDepositsTotal;
    }

    public void setPatientDepositsTotal(double patientDepositsTotal) {
        this.patientDepositsTotal = patientDepositsTotal;
    }

    public boolean isOpdBillCancellationSameDay() {
        return opdBillCancellationSameDay;
    }

    public void setOpdBillCancellationSameDay(boolean opdBillCancellationSameDay) {
        this.opdBillCancellationSameDay = opdBillCancellationSameDay;
    }

    public boolean isOpdBillRefundAllowedSameDay() {
        return opdBillRefundAllowedSameDay;
    }

    public void setOpdBillRefundAllowedSameDay(boolean opdBillRefundAllowedSameDay) {
        this.opdBillRefundAllowedSameDay = opdBillRefundAllowedSameDay;
    }

    public Doctor getReferredBy() {
        return referredBy;
    }

    public void setReferredBy(Doctor referredBy) {
        this.referredBy = referredBy;
    }

    public class PaymentSummary {

        private long idCounter = 0;

        private long id; // Unique identifier
        private PaymentMethod paymentMethod;
        private Double total;
        private Double discount;
        private Double netTotal;
        private Double tax;
        private Long count;

        private Boolean canceld;
        private Boolean refund;

        // Constructors, getters, and setters
        public PaymentSummary(PaymentMethod paymentMethod, Double total, Double discount, Double netTotal, Double tax, Long count) {
            this.id = ++idCounter; // Increment and assign a unique ID
            this.paymentMethod = paymentMethod;
            this.total = total;
            this.discount = discount;
            this.netTotal = netTotal;
            this.tax = tax;
            this.count = count;
        }

        // Unique ID getter
        public long getId() {
            return id;
        }

        // Add methods to aggregate (sum up) totals, discounts, netTotals, and tax values from BillSummery instances
        public PaymentMethod getPaymentMethod() {
            return paymentMethod;
        }

        public void setPaymentMethod(PaymentMethod paymentMethod) {
            this.paymentMethod = paymentMethod;
        }

        public Double getTotal() {
            return total;
        }

        public void setTotal(Double total) {
            this.total = total;
        }

        public Double getDiscount() {
            return discount;
        }

        public void setDiscount(Double discount) {
            this.discount = discount;
        }

        public Double getNetTotal() {
            return netTotal;
        }

        public void setNetTotal(Double netTotal) {
            this.netTotal = netTotal;
        }

        public Double getTax() {
            return tax;
        }

        public void setTax(Double tax) {
            this.tax = tax;
        }

        public Long getCount() {
            return count;
        }

        public void setCount(Long count) {
            this.count = count;
        }

        public Boolean getCanceld() {
            return canceld;
        }

        public void setCanceld(Boolean canceld) {
            this.canceld = canceld;
        }

        public Boolean getRefund() {
            return refund;
        }

        public void setRefund(Boolean refund) {
            this.refund = refund;
        }

    }

    public class BillTypeSummary {

        private long idCounter = 0;

        private long id; // Unique identifier
        private BillType billType;
        private BillClassType billClassType;
        private List<PaymentSummary> paymentSummaries;
        private double billTypeTotal = 0;

        // Constructors, getters, and setters
        public BillTypeSummary(BillType billType, BillClassType billClassType, List<PaymentSummary> paymentSummaries, double billTypeTotal) {
            this.id = ++idCounter; // Increment and assign a unique ID
            this.billType = billType;
            this.billClassType = billClassType;
            this.paymentSummaries = paymentSummaries;
            this.billTypeTotal = billTypeTotal;
        }

        // Unique ID getter
        public long getId() {
            return id;
        }

        // Methods to add or update payment summaries based on new BillSummery instances
        public BillType getBillType() {
            return billType;
        }

        public void setBillType(BillType billType) {
            this.billType = billType;
        }

        public BillClassType getBillClassType() {
            return billClassType;
        }

        public void setBillClassType(BillClassType billClassType) {
            this.billClassType = billClassType;
        }

        public List<PaymentSummary> getPaymentSummaries() {
            return paymentSummaries;
        }

        public void setPaymentSummaries(List<PaymentSummary> paymentSummaries) {
            this.paymentSummaries = paymentSummaries;
        }

        public double getBillTypeTotal() {
            return billTypeTotal;
        }

        public void setBillTypeTotal(double billTypeTotal) {
            this.billTypeTotal = billTypeTotal;
        }

    }

    public class OverallSummary {

        private List<BillTypeSummary> billTypeSummaries;

        // Constructors, getters, and setters
        public OverallSummary(List<BillTypeSummary> billTypeSummaries) {
            this.billTypeSummaries = billTypeSummaries;
        }

        // Methods to add or update bill type summaries based on new BillSummery instances
        public List<BillTypeSummary> getBillTypeSummaries() {
            return billTypeSummaries;
        }

        public void setBillTypeSummaries(List<BillTypeSummary> billTypeSummaries) {
            this.billTypeSummaries = billTypeSummaries;
        }

    }

}
