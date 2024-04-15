/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.bean.report.CashierReportController;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.BillsTotals;
import com.divudi.data.dataStructure.WebUserBillsTotal;

import com.divudi.entity.AuditEvent;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
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

/**
 *
 * @author Sniper 619
 */
@Named(value = "opdPreBillReportController")
@SessionScoped
public class OpdPreBillReportController implements Serializable {

    @EJB
    PaymentFacade paymentFacade;
    CommonFunctions commonFunctions;
    @EJB
    WebUserFacade webUserFacade;

    @Inject
    SessionController sessionController;
    @Inject
    CommonController commonController;
    @Inject
    CashierReportController cashierReportController;
    @Inject
    EnumController enumController;
    @Inject
    CommonController commonController1; 
    @Inject
    AuditEventApplicationController auditEventApplicationController;

            

    List<Bill> bills;
    List<WebUserBillsTotal> webUserBillsTotals;

    WebUser webUser;
    Department department;//cash getting dept - cashier
    Department toDepartment;// lab/channel/opd
    Date fromDate;
    Date toDate;

    //final totals
    double finalCashTot;
    double finalCreditTot;
    double finalCardTot;
    double finalSlipTot;
    double finalChequeTot;

    //Opd summery
    BillsTotals userBilledBills;
    BillsTotals userBilledBillsPatcial;
    BillsTotals userBilledBillsForCashier;
    BillsTotals userCancellededBills;
    BillsTotals userRefundedBills;

    //Pharmacy summery
    BillsTotals userBilledBillsPharmacy;
    BillsTotals userCancellededBillsPharmacy;
    BillsTotals userRefundedBillsPharmacy;

    //Pharmacy Purchase summery
    BillsTotals userBilledBillsPharmacyPurchase;
    BillsTotals userCancellededBillsPharmacyPurchase;
    BillsTotals userRefundedBillsPharmacyPurchase;
    BillsTotals userRefundedBillsPharmacyPurchaseCancel;

    //Pharmacy GRN
    BillsTotals userBilledBillsPharmacyGRN;
    BillsTotals userCancellededBillsPharmacyGRN;
    BillsTotals userRefundedBillsPharmacyGRN;
    BillsTotals userRefundedBillsPharmacyGRNCancel;

    //Pharmacy GRN
    BillsTotals userBilledBillsPharmacyGRNPayment;
    BillsTotals userCancellededBillsPharmacyGRNPayment;
    BillsTotals userRefundedBillsPharmacyGRNPayment;
    
    //Doc Pay
    BillsTotals userBilledBillsDocPay;
    BillsTotals userCancellededBillsDocPay;
    
    //For show totals
    List<BillsTotals>totalRow = new ArrayList<>();

    List<PaymentMethod> getPaymentMethods = Arrays.asList(PaymentMethod.Cash, PaymentMethod.Credit, PaymentMethod.Cheque, PaymentMethod.Card, PaymentMethod.Slip);
    List<Bill> getBillClassTypes = Arrays.asList(new BilledBill(), new CancelledBill(), new RefundBill());
    List<BillsTotals> billsTotalses = new ArrayList<>();

    /**
     * Creates a new instance of OpdPreBillReportController
     */
    public OpdPreBillReportController() {
    }

    public void makeNull() {
        bills = new ArrayList<>();
        webUserBillsTotals = new ArrayList<>();
        webUser = null;
        department = null;
        toDepartment = null;
    }

    public void createCashierTableByUser() {
        
        Date startTime = new Date();

        if (getWebUser() == null) {
            JsfUtil.addErrorMessage("Please Select A User");
            return;
        }

        userBilledBills = createBillsTotals(new BilledBill(), BillType.OpdBill, getWebUser(), getDepartment(), getToDepartment());
        userCancellededBills = createBillsTotals(new CancelledBill(), BillType.OpdBill, getWebUser(), getDepartment(), getToDepartment());
        userRefundedBills = createBillsTotals(new RefundBill(), BillType.OpdBill, getWebUser(), getDepartment(), getToDepartment());

        userBilledBillsPharmacy = createBillsTotals(new BilledBill(), BillType.PharmacySale, getWebUser(), getDepartment(), getToDepartment());
        userCancellededBillsPharmacy = createBillsTotals(new CancelledBill(), BillType.PharmacySale, getWebUser(), getDepartment(), getToDepartment());
        userRefundedBillsPharmacy = createBillsTotals(new RefundBill(), BillType.PharmacySale, getWebUser(), getDepartment(), getToDepartment());

        
        
    }

    public void createCashierTableByUserPayment() {
        Date startTime = new Date();
        
        if (getWebUser() == null) {
            JsfUtil.addErrorMessage("Please Select A User");
            return;
        }
        billsTotalses=new ArrayList<>();

        userBilledBills = createBillsTotalsPayment(new BilledBill(), BillType.OpdBill, getWebUser(), getDepartment());
        billsTotalses.add(userBilledBills);
        userBilledBillsPatcial = createBillsTotalsPayment(new BilledBill(), BillType.CashRecieveBill, getWebUser(), getDepartment());
        billsTotalses.add(userBilledBillsPatcial);
        userBilledBillsForCashier = createBillsTotalsPayment(new BilledBill(), BillType.OpdBathcBill, getWebUser(), getDepartment());
        billsTotalses.add(userBilledBillsForCashier);
        userCancellededBills = createBillsTotalsPayment(new CancelledBill(), BillType.OpdBill, getWebUser(), getDepartment());
        billsTotalses.add(userCancellededBills);
        userRefundedBills = createBillsTotalsPayment(new RefundBill(), BillType.OpdBill, getWebUser(), getDepartment());
        billsTotalses.add(userRefundedBills);

        userBilledBillsPharmacy = createBillsTotalsPayment(new BilledBill(), BillType.PharmacySale, getWebUser(), getDepartment());
        billsTotalses.add(userBilledBillsPharmacy);
        userCancellededBillsPharmacy = createBillsTotalsPayment(new CancelledBill(), BillType.PharmacySale, getWebUser(), getDepartment());
        billsTotalses.add(userCancellededBillsPharmacy);
        userRefundedBillsPharmacy = createBillsTotalsPayment(new RefundBill(), BillType.PharmacySale, getWebUser(), getDepartment());
        billsTotalses.add(userRefundedBillsPharmacy);

        userBilledBillsPharmacyPurchase = createBillsTotalsPayment(new BilledBill(), BillType.PharmacyPurchaseBill, getWebUser(), getDepartment());
        billsTotalses.add(userBilledBillsPharmacyPurchase);
        userCancellededBillsPharmacyPurchase = createBillsTotalsPayment(new CancelledBill(), BillType.PharmacyPurchaseBill, getWebUser(), getDepartment());
        billsTotalses.add(userCancellededBillsPharmacyPurchase);
        //purchase bill return as billed bill and bill type purchase return
        userRefundedBillsPharmacyPurchase = createBillsTotalsPayment(new BilledBill(), BillType.PurchaseReturn, getWebUser(), getDepartment());
        billsTotalses.add(userRefundedBillsPharmacyPurchase);
        //purchase retrn bills
        userRefundedBillsPharmacyPurchaseCancel = createBillsTotalsPayment(new CancelledBill(), BillType.PurchaseReturn, getWebUser(), getDepartment());
        billsTotalses.add(userRefundedBillsPharmacyPurchaseCancel);

        userBilledBillsPharmacyGRN = createBillsTotalsPayment(new BilledBill(), BillType.PharmacyGrnBill, getWebUser(), getDepartment());
        billsTotalses.add(userBilledBillsPharmacyGRN);
        userCancellededBillsPharmacyGRN = createBillsTotalsPayment(new CancelledBill(), BillType.PharmacyGrnBill, getWebUser(), getDepartment());
        billsTotalses.add(userCancellededBillsPharmacyGRN);
        userRefundedBillsPharmacyGRN = createBillsTotalsPayment(new BilledBill(), BillType.PharmacyGrnReturn, getWebUser(), getDepartment());
        billsTotalses.add(userRefundedBillsPharmacyGRN);
        userRefundedBillsPharmacyGRNCancel = createBillsTotalsPayment(new CancelledBill(), BillType.PharmacyGrnReturn, getWebUser(), getDepartment());
        billsTotalses.add(userRefundedBillsPharmacyGRNCancel);

        userBilledBillsPharmacyGRNPayment = createBillsTotalsPayment(new BilledBill(), BillType.GrnPaymentPre, getWebUser(), getDepartment());
        billsTotalses.add(userBilledBillsPharmacyGRNPayment);
        userCancellededBillsPharmacyGRNPayment = createBillsTotalsPayment(new CancelledBill(), BillType.GrnPayment, getWebUser(), getDepartment());
        billsTotalses.add(userCancellededBillsPharmacyGRNPayment);
        userRefundedBillsPharmacyGRNPayment = createBillsTotalsPayment(new RefundBill(), BillType.GrnPayment, getWebUser(), getDepartment());
        billsTotalses.add(userRefundedBillsPharmacyGRNPayment);
        
        userBilledBillsDocPay = createBillsTotalsPayment(new BilledBill(), BillType.PaymentBill, getWebUser(), getDepartment());
        billsTotalses.add(userBilledBillsDocPay);
        userCancellededBillsDocPay = createBillsTotalsPayment(new CancelledBill(), BillType.PaymentBill, getWebUser(), getDepartment());
        billsTotalses.add(userCancellededBillsDocPay);

        calTotals(billsTotalses);

        
    }

    public String createCashierTableByUserPaymentForDetail() {

        createCashierTableByUserPayment();

        return "/reportCashierBillFeePayment/report_cashier_detailed_by_user_payment";

    }
    
    public String navigateToReportCashierDetailedByUserPayment(){
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
        auditEvent.setEventTrigger("navigateToReportCashierDetailedByUserPayment()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        
        
        return "/reportCashierBillFeePayment/report_cashier_detailed_by_user_payment.xhtml?faces-redirect=true";
    }
    
    public String navigateToReportCashierSummeryByUserPayment(){
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
        auditEvent.setEventTrigger("navigateToReportCashierSummeryByUserPayment()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        
        
        return "/reportCashierBillFeePayment/report_cashier_summery_by_user_payment.xhtml?faces-redirect=true";
    }
    
    
    
            
    public String navigateToReportCashierSummeryAllTotalOnly(){
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
        auditEvent.setEventTrigger("navigateToReportCashierSummeryAllTotalOnly()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        
        
        return "/reportCashierBillFeePayment/report_cashier_summery_all_total_only.xhtml?faces-redirect=true";
    }
    
    public String navigateToReportCashierSummeryAll(){
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
        auditEvent.setEventTrigger("navigateToReportCashierSummeryAll()");
        auditEventApplicationController.logAuditEvent(auditEvent);

        
        Date endTime = new Date();
        duration = endTime.getTime() - startTime.getTime();
        auditEvent.setEventDuration(duration);
        auditEvent.setEventStatus("Completed");
        auditEventApplicationController.logAuditEvent(auditEvent);
        
        
        return "/reportCashierBillFeePayment/report_cashier_summery_all.xhtml?faces-redirect=true";
    }
    
            

    public void createCashierTableByAllUserPaymentDetail() {
        Date startTime = new Date();
        createCashierTableByAllUserPayment(true);
        
        
    }

    public void createCashierTableByAllUserPaymentSummery() {
        Date startTime  = new Date();
        createCashierTableByAllUserPayment(false);
        
        
    }

    public void createCashierTableByAllUserPayment(boolean detail) {
        webUserBillsTotals = new ArrayList<>();

        finalCashTot = 0.0;
        finalCardTot = 0.0;
        finalChequeTot = 0.0;
        finalCreditTot = 0.0;
        finalSlipTot = 0.0;

        for (WebUser wu : getCashiers()) {
            WebUserBillsTotal tmp = new WebUserBillsTotal();
            tmp.setWebUser(wu);
            List<BillsTotals> billls = new ArrayList<>();
            double uCard = 0;
            double uCash = 0;
            double uCheque = 0;
            double uCredit = 0;
            double uSlip = 0;
            for (BillType btp : getCashFlowBillTypes()) {
                for (Bill b : getBillClassTypes) {
                    BillsTotals billsTotals = createTotalsPayment(b, btp, wu, getDepartment());
                    if (billsTotals.getCard() != 0
                            || billsTotals.getCash() != 0
                            || billsTotals.getCheque() != 0
                            || billsTotals.getCredit() != 0
                            || billsTotals.getSlip() != 0) {

                        if (detail) {
                            billls.add(billsTotals);
                        }
                        uCard += billsTotals.getCard();
                        uCash += billsTotals.getCash();
                        uCheque += billsTotals.getCheque();
                        uCredit += billsTotals.getCredit();
                        uSlip += billsTotals.getSlip();

                    }
                }

            }
            BillsTotals newSum = new BillsTotals();
            newSum.setName("Total ");
            newSum.setBold(true);
            newSum.setCard(uCard);
            newSum.setCash(uCash);
            newSum.setCheque(uCheque);
            newSum.setCredit(uCredit);
            newSum.setSlip(uSlip);

            if (newSum.getCard() != 0 || newSum.getCash() != 0 || newSum.getCheque() != 0 || newSum.getCredit() != 0 || newSum.getSlip() != 0) {
                billls.add(newSum);
            }

            finalCashTot += newSum.getCash();
            finalCardTot += newSum.getCard();
            finalChequeTot += newSum.getCheque();
            finalCreditTot += newSum.getCredit();
            finalSlipTot += newSum.getSlip();

            tmp.setBillsTotals(billls);
            webUserBillsTotals.add(tmp);
        }

    }

    private double calValue(Bill b, PaymentMethod paymentMethod, WebUser wUser, Department d, Department td, BillType bt) {

        String sql;
        Map m = new HashMap();

        sql = "SELECT sum(bfp.amount) FROM BillFeePayment bfp "
                + " join bfp.payment p "
                + " join bfp.billFee.bill b "
                + " WHERE "
                + " bfp.retired=false "
                + " and type(b)=:b "
                + " and b.billType=:bt "
                + " and p.paymentMethod=:pm "
                + " and p.institution=:ins "
                + " and p.createdAt between :fromDate and :toDate";

        if (d != null) {
            sql += " and p.department=:dep ";
            m.put("dep", d);
        }

        if (td != null) {
            sql += " and bfp.department=:tdep ";
            m.put("tdep", td);
        }

        if (wUser != null) {
            sql += " and p.creater=:w ";
            m.put("w", wUser);
        }

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("pm", paymentMethod);
        m.put("b", b.getClass());
        m.put("bt", bt);
        m.put("ins", getSessionController().getInstitution());

        return getPaymentFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private double calValuePayment(Bill b, PaymentMethod paymentMethod, WebUser wUser, Department department, BillType bt) {

        String sql;
        Map m = new HashMap();

        sql = "SELECT sum(p.paidValue) FROM Payment p WHERE "
                + " p.retired=false "
                + " and type(p.bill)=:b "
                + " and p.bill.billType=:bt "
                + " and p.paymentMethod=:pm "
                + " and p.institution=:ins "
                + " and p.createdAt between :fromDate and :toDate";

        if (department != null) {
            sql += " and p.department=:dep ";
            m.put("dep", department);
        }

        if (wUser != null) {
            sql += " and p.creater=:w ";
            m.put("w", wUser);
        }

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("pm", paymentMethod);
        m.put("b", b.getClass());
        m.put("bt", bt);
        m.put("ins", getSessionController().getInstitution());

        if (getPaymentFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP) > 0) {
        }

        return getPaymentFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private List<Object[]> getBillWithTotal(Bill b, PaymentMethod paymentMethod, WebUser wUser, Department department, Department tDepartment, BillType bt) {

        String sql;
        Map m = new HashMap();

        sql = "SELECT distinct(bfp.billFee.bill),sum(bfp.amount) FROM BillFeePayment bfp WHERE "
                + " bfp.retired=false "
                + " and type(bfp.billFee.bill)=:b "
                + " and bfp.billFee.bill.billType=:bt "
                + " and bfp.payment.paymentMethod=:pm "
                + " and bfp.payment.institution=:ins "
                + " and bfp.payment.createdAt between :fromDate and :toDate ";
        //Payment done deparment-cashier
        if (department != null) {
            sql += " and bfp.payment.department=:dep ";
            m.put("dep", department);
        }

        if (tDepartment != null) {
            sql += " and bfp.department=:toDep ";
            m.put("toDep", tDepartment);
        }

        if (wUser != null) {
            sql += " and bfp.payment.creater=:w ";
            m.put("w", wUser);
        }

        sql += " group by bfp.billFee.bill "
                + " order by bfp.billFee.bill.createdAt ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("pm", paymentMethod);
        m.put("b", b.getClass());
        m.put("bt", bt);
        m.put("ins", getSessionController().getInstitution());


        return getPaymentFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    private List<Object[]> getBillWithTotalPayment(Bill b, PaymentMethod paymentMethod, WebUser wUser, Department department, BillType bt) {

        String sql;
        Map m = new HashMap();

        sql = "SELECT distinct(p.bill),sum(p.paidValue) FROM Payment p WHERE "
                + " p.retired=false "
                + " and type(p.bill)=:b "
                + " and p.bill.billType=:bt "
                + " and p.paymentMethod=:pm "
                + " and p.institution=:ins "
                + " and p.createdAt between :fromDate and :toDate ";
        //Payment done deparment-cashier
        if (department != null) {
            sql += " and p.department=:dep ";
            m.put("dep", department);
        }

        if (wUser != null) {
            sql += " and p.creater=:w ";
            m.put("w", wUser);
        }

        sql += " group by p.bill "
                + " order by p.bill.createdAt ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("pm", paymentMethod);
        m.put("b", b.getClass());
        m.put("bt", bt);
        m.put("ins", getSessionController().getInstitution());

        if (getPaymentFacade().findAggregates(sql, m, TemporalType.TIMESTAMP) != null) {
        }

        return getPaymentFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public BillsTotals createBillsTotals(Bill b, BillType billType, WebUser wu, Department d, Department td) {
        BillsTotals billsTotals = new BillsTotals();
        List<Bill> bs = new ArrayList<>();
        for (PaymentMethod pm : getPaymentMethods) {
            List<Object[]> objects = getBillWithTotal(b, pm, wu, d, td, billType);
            if (objects != null) {
                for (Object[] obj : objects) {
                    if (obj[0] != null) {
                        Bill bb = new Bill();
                        bb = (Bill) obj[0];
                        bb.setNetTotal((double) obj[1]);
                        bb.setPaymentMethod(pm);
                        if (bb != null) {
                            bs.add(bb);
                        }
                    }
                }
            }
        }
        billsTotals.setBills(bs);
        if ((d == null && td != null) || (d != null && td == null)) {
            if (d != null) {
                billsTotals.setName(d.getName());
            } else {
                billsTotals.setName(td.getName());
            }
        }
        for (PaymentMethod paymentMethod : getPaymentMethods) {
        }

        return billsTotals;
    }

    public BillsTotals createBillsTotalsPayment(Bill b, BillType billType, WebUser wu, Department d) {
        BillsTotals billsTotals = new BillsTotals();
        List<Bill> bs = new ArrayList<>();
        for (PaymentMethod pm : getPaymentMethods) {
            List<Object[]> objects = getBillWithTotalPayment(b, pm, wu, d, billType);
            if (objects != null) {
                for (Object[] obj : objects) {
                    if (obj[0] != null) {
                        Bill bb = new Bill();
                        bb = (Bill) obj[0];
                        bb.setNetTotal((double) obj[1]);
                        bb.setPaymentMethod(pm);
                        if (bb != null) {
                            bs.add(bb);
                        }
                    }
                }
            }
        }
        billsTotals.setBills(bs);
        for (PaymentMethod paymentMethod : getPaymentMethods) {
            if (calValuePayment(b, paymentMethod, getWebUser(), d, billType) > 0) {
            }
            switch (paymentMethod) {
                case Cash:
                    billsTotals.setCash(calValuePayment(b, paymentMethod, getWebUser(), d, billType));
                    break;
                case Credit:
                    billsTotals.setCredit(calValuePayment(b, paymentMethod, getWebUser(), d, billType));
                    break;
                case Card:
                    billsTotals.setCard(calValuePayment(b, paymentMethod, getWebUser(), d, billType));
                    break;
                case Slip:
                    billsTotals.setSlip(calValuePayment(b, paymentMethod, getWebUser(), d, billType));
                    break;
                case Cheque:
                    billsTotals.setCheque(calValuePayment(b, paymentMethod, getWebUser(), d, billType));
                    break;
            }
        }
        return billsTotals;
    }

    public BillsTotals createTotalsPayment(Bill b, BillType billType, WebUser wu, Department d) {
        BillsTotals billsTotals = new BillsTotals();
        List<Bill> bs = new ArrayList<>();
        String name = billType.getLabel() + " ";

        if (b.getClass().equals(BilledBill.class)) {
            name += "Billed";
        }
        if (b.getClass().equals(RefundBill.class)) {
            name += "Refunded";
        }
        if (b.getClass().equals(CancelledBill.class)) {
            name += "Cancelled";
        }
        billsTotals.setName(name);
        for (PaymentMethod paymentMethod : getPaymentMethods) {
            if (calValuePayment(b, paymentMethod, getWebUser(), d, billType) > 0) {
            }
            switch (paymentMethod) {
                case Cash:
                    billsTotals.setCash(calValuePayment(b, paymentMethod, wu, d, billType));
                    break;
                case Credit:
                    billsTotals.setCredit(calValuePayment(b, paymentMethod, wu, d, billType));
                    break;
                case Card:
                    billsTotals.setCard(calValuePayment(b, paymentMethod, wu, d, billType));
                    break;
                case Slip:
                    billsTotals.setSlip(calValuePayment(b, paymentMethod, wu, d, billType));
                    break;
                case Cheque:
                    billsTotals.setCheque(calValuePayment(b, paymentMethod, wu, d, billType));
                    break;
            }
        }

        return billsTotals;
    }

    public List<WebUser> getCashiers() {
        String sql;
        Map temMap = new HashMap();
        List<WebUser> cashiers = new ArrayList<>();
        BillType[] btpArr = getCashFlowBillTypes();
        List<BillType> btpList = Arrays.asList(btpArr);
        sql = "select us from "
                + " Payment p"
                + " join p.bill b "
                + " join p.creater us "
                + " where p.retired=false "
                + " and p.institution=:ins "
                + " and b.billType in :btp "
                + " and p.createdAt between :fromDate and :toDate "
                + " group by us ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", btpList);
        temMap.put("ins", sessionController.getInstitution());
        cashiers = getWebUserFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        if (cashiers == null) {
            cashiers = new ArrayList<>();
        }

        return cashiers;
    }

    public BillType[] getCashFlowBillTypes() {
        BillType[] b = {
            BillType.OpdBathcBill,
            BillType.OpdBill,
            BillType.PaymentBill,
            BillType.PettyCash,
            BillType.CashRecieveBill,
            BillType.AgentPaymentReceiveBill,
            BillType.InwardPaymentBill,
            BillType.PharmacySale,
            BillType.ChannelCash,
            BillType.ChannelPaid,
            BillType.PharmacyPurchaseBill,
            BillType.PurchaseReturn,
            BillType.PharmacyGrnBill,
            BillType.PharmacyGrnReturn,
            BillType.GrnPayment,
            BillType.GrnPaymentPre,};

        return b;
    }

    public void calTotals(List<BillsTotals> bts) {
        double cash=0.0;
        double credit=0.0;
        double card=0.0;
        double cheque=0.0;
        double slip=0.0;
        for (BillsTotals bt : bts) {
            cash+=bt.getCash();
            card+=bt.getCard();
            credit+=bt.getCredit();
            cheque+=bt.getCheque();
            slip+=bt.getSlip();
        }
        BillsTotals tr=new BillsTotals();
        totalRow=new ArrayList<>();
        tr.setName("Final Total");
        tr.setCard(card);
        tr.setCash(cash);
        tr.setCheque(cheque);
        tr.setCredit(credit);
        tr.setSlip(slip);
        tr.setBold(true);
        totalRow.add(tr);
    }

    //getters and Setters
    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public WebUser getWebUser() {
        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public BillsTotals getUserBilledBills() {
        return userBilledBills;
    }

    public void setUserBilledBills(BillsTotals userBilledBills) {
        this.userBilledBills = userBilledBills;
    }

    public BillsTotals getUserCancellededBills() {
        return userCancellededBills;
    }

    public void setUserCancellededBills(BillsTotals userCancellededBills) {
        this.userCancellededBills = userCancellededBills;
    }

    public BillsTotals getUserRefundedBills() {
        return userRefundedBills;
    }

    public void setUserRefundedBills(BillsTotals userRefundedBills) {
        this.userRefundedBills = userRefundedBills;
    }

    public PaymentFacade getPaymentFacade() {
        return paymentFacade;
    }

    public void setPaymentFacade(PaymentFacade paymentFacade) {
        this.paymentFacade = paymentFacade;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<Bill> getBills() {
        if (bills == null) {
            bills = new ArrayList<>();
        }
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public BillsTotals getUserBilledBillsPharmacy() {
        return userBilledBillsPharmacy;
    }

    public void setUserBilledBillsPharmacy(BillsTotals userBilledBillsPharmacy) {
        this.userBilledBillsPharmacy = userBilledBillsPharmacy;
    }

    public BillsTotals getUserCancellededBillsPharmacy() {
        return userCancellededBillsPharmacy;
    }

    public void setUserCancellededBillsPharmacy(BillsTotals userCancellededBillsPharmacy) {
        this.userCancellededBillsPharmacy = userCancellededBillsPharmacy;
    }

    public BillsTotals getUserRefundedBillsPharmacy() {
        return userRefundedBillsPharmacy;
    }

    public void setUserRefundedBillsPharmacy(BillsTotals userRefundedBillsPharmacy) {
        this.userRefundedBillsPharmacy = userRefundedBillsPharmacy;
    }

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public CashierReportController getCashierReportController() {
        return cashierReportController;
    }

    public void setCashierReportController(CashierReportController cashierReportController) {
        this.cashierReportController = cashierReportController;
    }

    public List<WebUserBillsTotal> getWebUserBillsTotals() {
        return webUserBillsTotals;
    }

    public void setWebUserBillsTotals(List<WebUserBillsTotal> webUserBillsTotals) {
        this.webUserBillsTotals = webUserBillsTotals;
    }

    public EnumController getEnumController() {
        return enumController;
    }

    public void setEnumController(EnumController enumController) {
        this.enumController = enumController;
    }

    public WebUserFacade getWebUserFacade() {
        return webUserFacade;
    }

    public void setWebUserFacade(WebUserFacade webUserFacade) {
        this.webUserFacade = webUserFacade;
    }

    public double getFinalCashTot() {
        return finalCashTot;
    }

    public void setFinalCashTot(double finalCashTot) {
        this.finalCashTot = finalCashTot;
    }

    public double getFinalCreditTot() {
        return finalCreditTot;
    }

    public void setFinalCreditTot(double finalCreditTot) {
        this.finalCreditTot = finalCreditTot;
    }

    public double getFinalCardTot() {
        return finalCardTot;
    }

    public void setFinalCardTot(double finalCardTot) {
        this.finalCardTot = finalCardTot;
    }

    public double getFinalSlipTot() {
        return finalSlipTot;
    }

    public void setFinalSlipTot(double finalSlipTot) {
        this.finalSlipTot = finalSlipTot;
    }

    public double getFinalChequeTot() {
        return finalChequeTot;
    }

    public void setFinalChequeTot(double finalChequeTot) {
        this.finalChequeTot = finalChequeTot;
    }

//    public boolean isBack() {
//        return back;
//    }
//
//    public void setBack(boolean back) {
//        this.back = back;
//    }
    public BillsTotals getUserBilledBillsPharmacyPurchase() {
        return userBilledBillsPharmacyPurchase;
    }

    public void setUserBilledBillsPharmacyPurchase(BillsTotals userBilledBillsPharmacyPurchase) {
        this.userBilledBillsPharmacyPurchase = userBilledBillsPharmacyPurchase;
    }

    public BillsTotals getUserCancellededBillsPharmacyPurchase() {
        return userCancellededBillsPharmacyPurchase;
    }

    public void setUserCancellededBillsPharmacyPurchase(BillsTotals userCancellededBillsPharmacyPurchase) {
        this.userCancellededBillsPharmacyPurchase = userCancellededBillsPharmacyPurchase;
    }

    public BillsTotals getUserRefundedBillsPharmacyPurchase() {
        return userRefundedBillsPharmacyPurchase;
    }

    public void setUserRefundedBillsPharmacyPurchase(BillsTotals userRefundedBillsPharmacyPurchase) {
        this.userRefundedBillsPharmacyPurchase = userRefundedBillsPharmacyPurchase;
    }

    public BillsTotals getUserRefundedBillsPharmacyPurchaseCancel() {
        return userRefundedBillsPharmacyPurchaseCancel;
    }

    public void setUserRefundedBillsPharmacyPurchaseCancel(BillsTotals userRefundedBillsPharmacyPurchaseCancel) {
        this.userRefundedBillsPharmacyPurchaseCancel = userRefundedBillsPharmacyPurchaseCancel;
    }

    public BillsTotals getUserBilledBillsPharmacyGRN() {
        return userBilledBillsPharmacyGRN;
    }

    public void setUserBilledBillsPharmacyGRN(BillsTotals userBilledBillsPharmacyGRN) {
        this.userBilledBillsPharmacyGRN = userBilledBillsPharmacyGRN;
    }

    public BillsTotals getUserCancellededBillsPharmacyGRN() {
        return userCancellededBillsPharmacyGRN;
    }

    public void setUserCancellededBillsPharmacyGRN(BillsTotals userCancellededBillsPharmacyGRN) {
        this.userCancellededBillsPharmacyGRN = userCancellededBillsPharmacyGRN;
    }

    public BillsTotals getUserRefundedBillsPharmacyGRN() {
        return userRefundedBillsPharmacyGRN;
    }

    public void setUserRefundedBillsPharmacyGRN(BillsTotals userRefundedBillsPharmacyGRN) {
        this.userRefundedBillsPharmacyGRN = userRefundedBillsPharmacyGRN;
    }

    public BillsTotals getUserRefundedBillsPharmacyGRNCancel() {
        return userRefundedBillsPharmacyGRNCancel;
    }

    public void setUserRefundedBillsPharmacyGRNCancel(BillsTotals userRefundedBillsPharmacyGRNCancel) {
        this.userRefundedBillsPharmacyGRNCancel = userRefundedBillsPharmacyGRNCancel;
    }

    public BillsTotals getUserBilledBillsPharmacyGRNPayment() {
        return userBilledBillsPharmacyGRNPayment;
    }

    public void setUserBilledBillsPharmacyGRNPayment(BillsTotals userBilledBillsPharmacyGRNPayment) {
        this.userBilledBillsPharmacyGRNPayment = userBilledBillsPharmacyGRNPayment;
    }

    public BillsTotals getUserCancellededBillsPharmacyGRNPayment() {
        return userCancellededBillsPharmacyGRNPayment;
    }

    public void setUserCancellededBillsPharmacyGRNPayment(BillsTotals userCancellededBillsPharmacyGRNPayment) {
        this.userCancellededBillsPharmacyGRNPayment = userCancellededBillsPharmacyGRNPayment;
    }

    public BillsTotals getUserRefundedBillsPharmacyGRNPayment() {
        return userRefundedBillsPharmacyGRNPayment;
    }

    public void setUserRefundedBillsPharmacyGRNPayment(BillsTotals userRefundedBillsPharmacyGRNPayment) {
        this.userRefundedBillsPharmacyGRNPayment = userRefundedBillsPharmacyGRNPayment;
    }

    public BillsTotals getUserBilledBillsForCashier() {
        return userBilledBillsForCashier;
    }

    public void setUserBilledBillsForCashier(BillsTotals userBilledBillsForCashier) {
        this.userBilledBillsForCashier = userBilledBillsForCashier;
    }

    public BillsTotals getUserBilledBillsPatcial() {
        return userBilledBillsPatcial;
    }

    public void setUserBilledBillsPatcial(BillsTotals userBilledBillsPatcial) {
        this.userBilledBillsPatcial = userBilledBillsPatcial;
    }

    public List<BillsTotals> getTotalRow() {
        return totalRow;
    }

    public void setTotalRow(List<BillsTotals> totalRow) {
        this.totalRow = totalRow;
    }

    public BillsTotals getUserBilledBillsDocPay() {
        return userBilledBillsDocPay;
    }

    public void setUserBilledBillsDocPay(BillsTotals userBilledBillsDocPay) {
        this.userBilledBillsDocPay = userBilledBillsDocPay;
    }

    public BillsTotals getUserCancellededBillsDocPay() {
        return userCancellededBillsDocPay;
    }

    public void setUserCancellededBillsDocPay(BillsTotals userCancellededBillsDocPay) {
        this.userCancellededBillsDocPay = userCancellededBillsDocPay;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public CommonController getCommonController1() {
        return commonController1;
    }

    public void setCommonController1(CommonController commonController1) {
        this.commonController1 = commonController1;
    }
    
}
