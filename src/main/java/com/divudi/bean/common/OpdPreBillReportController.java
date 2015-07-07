/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.bean.report.CashierReportController;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.BillsTotals;
import com.divudi.data.dataStructure.WebUserBillsTotal;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Payment;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.PaymentFacade;
import com.divudi.facade.WebUserFacade;
import com.divudi.facade.util.JsfUtil;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 *
 * @author Sniper 619
 */
@Named(value = "opdPreBillReportController")
@SessionScoped
public class OpdPreBillReportController implements Serializable {

    @EJB
    PaymentFacade paymentFacade;
    @EJB
    CommonFunctions commonFunctions;
    @EJB
    WebUserFacade webUserFacade;

    @Inject
    SessionController sessionController;
    @Inject
    CashierReportController cashierReportController;
    @Inject
    EnumController enumController;

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

    List<PaymentMethod> getPaymentMethods = Arrays.asList(PaymentMethod.Cash, PaymentMethod.Credit, PaymentMethod.Cheque, PaymentMethod.Card, PaymentMethod.Slip);
    List<Bill> getBillClassTypes = Arrays.asList(new BilledBill(), new CancelledBill(), new RefundBill());

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
        System.err.println("getWebUser() = " + getWebUser());
        System.err.println("Date F = " + getFromDate());
        System.err.println("Date T = " + getToDate());
        if (getWebUser() == null) {
            JsfUtil.addErrorMessage("Please Select A User");
            return;
        }

        userBilledBills = createBillsTotalsPayment(new BilledBill(), BillType.OpdBathcBill, getWebUser(), getDepartment());
        userCancellededBills = createBillsTotalsPayment(new CancelledBill(), BillType.OpdBill, getWebUser(), getDepartment());
        userRefundedBills = createBillsTotalsPayment(new RefundBill(), BillType.OpdBill, getWebUser(), getDepartment());

        userBilledBillsPharmacy = createBillsTotalsPayment(new BilledBill(), BillType.PharmacySale, getWebUser(), getDepartment());
        userCancellededBillsPharmacy = createBillsTotalsPayment(new CancelledBill(), BillType.PharmacySale, getWebUser(), getDepartment());
        userRefundedBillsPharmacy = createBillsTotalsPayment(new RefundBill(), BillType.PharmacySale, getWebUser(), getDepartment());

        userBilledBillsPharmacyPurchase = createBillsTotalsPayment(new BilledBill(), BillType.PharmacyPurchaseBill, getWebUser(), getDepartment());
        userCancellededBillsPharmacyPurchase = createBillsTotalsPayment(new CancelledBill(), BillType.PharmacyPurchaseBill, getWebUser(), getDepartment());
        //purchase bill return as billed bill and bill type purchase return
        userRefundedBillsPharmacyPurchase = createBillsTotalsPayment(new BilledBill(), BillType.PurchaseReturn, getWebUser(), getDepartment());
        //purchase retrn bills
        userRefundedBillsPharmacyPurchaseCancel= createBillsTotalsPayment(new CancelledBill(), BillType.PurchaseReturn, getWebUser(), getDepartment());

    }

    public String createCashierTableByUserPaymentForDetail() {
        System.err.println("getWebUser() = " + getWebUser());
        System.err.println("Date F = " + getFromDate());
        System.err.println("Date T = " + getToDate());

        createCashierTableByUserPayment();

        return "/reportCashierBillFeePayment/report_cashier_detailed_by_user_payment";

    }

    public void createCashierTableByAllUserPaymentDetail() {
        createCashierTableByAllUserPayment(true);
    }

    public void createCashierTableByAllUserPaymentSummery() {
        createCashierTableByAllUserPayment(false);
    }

    public void createCashierTableByAllUserPayment(boolean detail) {
        System.out.println("in");
        webUserBillsTotals = new ArrayList<>();
        System.out.println("getCashiers() = " + getCashiers());

        finalCashTot = 0.0;
        finalCardTot = 0.0;
        finalChequeTot = 0.0;
        finalCreditTot = 0.0;
        finalSlipTot = 0.0;

        for (WebUser wu : getCashiers()) {
            System.out.println("in 2");
            WebUserBillsTotal tmp = new WebUserBillsTotal();
            tmp.setWebUser(wu);
            List<BillsTotals> billls = new ArrayList<>();
            double uCard = 0;
            double uCash = 0;
            double uCheque = 0;
            double uCredit = 0;
            double uSlip = 0;
            for (BillType btp : getCashFlowBillTypes()) {
                System.out.println("in 3");
                for (Bill b : getBillClassTypes) {
                    System.out.println("in 4");
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
                System.err.println("SUNN ");
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

        System.out.println("paymentMethod = " + paymentMethod);
        System.out.println("sql = " + sql);
        System.out.println("getPaymentFacade().findAggregates(sql, m, TemporalType.TIMESTAMP) = " + getPaymentFacade().findAggregates(sql, m, TemporalType.TIMESTAMP));

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

        System.out.println("paymentMethod = " + paymentMethod);
        System.out.println("sql = " + sql);
        System.out.println("getPaymentFacade().findAggregates(sql, m, TemporalType.TIMESTAMP) = " + getPaymentFacade().findAggregates(sql, m, TemporalType.TIMESTAMP));

        return getPaymentFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public BillsTotals createBillsTotals(Bill b, BillType billType, WebUser wu, Department d, Department td) {
        BillsTotals billsTotals = new BillsTotals();
        List<Bill> bs = new ArrayList<>();
        for (PaymentMethod pm : getPaymentMethods) {
            List<Object[]> objects = getBillWithTotal(b, pm, wu, d, td, billType);
            System.out.println("objects = " + objects);
            if (objects != null) {
                for (Object[] obj : objects) {
                    if (obj[0] != null) {
                        Bill bb = new Bill();
                        bb = (Bill) obj[0];
                        System.out.println("bb = " + bb);
                        bb.setNetTotal((double) obj[1]);
                        System.out.println("bb = " + bb.getNetTotal());
                        bb.setPaymentMethod(pm);
                        System.out.println("bb = " + bb.getPaymentMethod());
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
            switch (paymentMethod) {
                case Cash:
                    System.out.println("1.calValue Cash = " + calValue(b, paymentMethod, getWebUser(), d, td, billType));
                    billsTotals.setCash(calValue(b, paymentMethod, getWebUser(), d, td, billType));
                    break;
                case Credit:
                    System.out.println("2.calValue Credit = " + calValue(b, paymentMethod, getWebUser(), d, td, billType));
                    billsTotals.setCredit(calValue(b, paymentMethod, getWebUser(), d, td, billType));
                    break;
                case Card:
                    System.out.println("3.calValue Card = " + calValue(b, paymentMethod, getWebUser(), d, td, billType));
                    billsTotals.setCard(calValue(b, paymentMethod, getWebUser(), d, td, billType));
                    break;
                case Slip:
                    System.out.println("4.calValue Slip = " + calValue(b, paymentMethod, getWebUser(), d, td, billType));
                    billsTotals.setSlip(calValue(b, paymentMethod, getWebUser(), d, td, billType));
                    break;
                case Cheque:
                    System.out.println("5.calValue Cheque= " + calValue(b, paymentMethod, getWebUser(), d, td, billType));
                    billsTotals.setCheque(calValue(b, paymentMethod, getWebUser(), d, td, billType));
                    break;
            }
        }

        return billsTotals;
    }

    public BillsTotals createBillsTotalsPayment(Bill b, BillType billType, WebUser wu, Department d) {
        BillsTotals billsTotals = new BillsTotals();
        List<Bill> bs = new ArrayList<>();
        for (PaymentMethod pm : getPaymentMethods) {
            List<Object[]> objects = getBillWithTotalPayment(b, pm, wu, d, billType);
            System.out.println("objects = " + objects);
            if (objects != null) {
                for (Object[] obj : objects) {
                    if (obj[0] != null) {
                        Bill bb = new Bill();
                        bb = (Bill) obj[0];
                        System.out.println("bb = " + bb);
                        bb.setNetTotal((double) obj[1]);
                        System.out.println("bb = " + bb.getNetTotal());
                        bb.setPaymentMethod(pm);
                        System.out.println("bb = " + bb.getPaymentMethod());
                        if (bb != null) {
                            bs.add(bb);
                        }
                    }
                }
            }
        }
        billsTotals.setBills(bs);
        for (PaymentMethod paymentMethod : getPaymentMethods) {
            switch (paymentMethod) {
                case Cash:
                    System.out.println("1.calValue Cash = " + calValuePayment(b, paymentMethod, getWebUser(), d, billType));
                    billsTotals.setCash(calValuePayment(b, paymentMethod, getWebUser(), d, billType));
                    break;
                case Credit:
                    System.out.println("2.calValue Credit = " + calValuePayment(b, paymentMethod, getWebUser(), d, billType));
                    billsTotals.setCredit(calValuePayment(b, paymentMethod, getWebUser(), d, billType));
                    break;
                case Card:
                    System.out.println("3.calValue Card = " + calValuePayment(b, paymentMethod, getWebUser(), d, billType));
                    billsTotals.setCard(calValuePayment(b, paymentMethod, getWebUser(), d, billType));
                    break;
                case Slip:
                    System.out.println("4.calValue Slip = " + calValuePayment(b, paymentMethod, getWebUser(), d, billType));
                    billsTotals.setSlip(calValuePayment(b, paymentMethod, getWebUser(), d, billType));
                    break;
                case Cheque:
                    System.out.println("5.calValue Cheque = " + calValuePayment(b, paymentMethod, getWebUser(), d, billType));
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
        System.out.println("name = " + name);
        for (PaymentMethod paymentMethod : getPaymentMethods) {
            switch (paymentMethod) {
                case Cash:
                    System.out.println("1.calValuePayment Cash = " + calValuePayment(b, paymentMethod, wu, d, billType));
                    billsTotals.setCash(calValuePayment(b, paymentMethod, wu, d, billType));
                    break;
                case Credit:
                    System.out.println("2.calValuePayment Credit = " + calValuePayment(b, paymentMethod, wu, d, billType));
                    billsTotals.setCredit(calValuePayment(b, paymentMethod, wu, d, billType));
                    break;
                case Card:
                    System.out.println("3.calValuePayment Card = " + calValuePayment(b, paymentMethod, wu, d, billType));
                    billsTotals.setCard(calValuePayment(b, paymentMethod, wu, d, billType));
                    break;
                case Slip:
                    System.out.println("4.calValuePayment Slip = " + calValuePayment(b, paymentMethod, wu, d, billType));
                    billsTotals.setSlip(calValuePayment(b, paymentMethod, wu, d, billType));
                    break;
                case Cheque:
                    System.out.println("5.calValuePayment Cheque = " + calValuePayment(b, paymentMethod, wu, d, billType));
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
        cashiers = getWebUserFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
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
            BillType.GrnPayment,};

        return b;
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
}
