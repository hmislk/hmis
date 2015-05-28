/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.BillsTotals;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.PaymentFacade;
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

    @Inject
    SessionController sessionController;

    List<Bill> bills;

    WebUser webUser;
    Department department;
    Date fromDate;
    Date toDate;

    BillsTotals userBilledBills;
    BillsTotals userCancellededBills;
    BillsTotals userRefundedBills;

    List<PaymentMethod> getPaymentMethods = Arrays.asList(PaymentMethod.Cash, PaymentMethod.Credit, PaymentMethod.Cheque, PaymentMethod.Card, PaymentMethod.Slip);

    /**
     * Creates a new instance of OpdPreBillReportController
     */
    public OpdPreBillReportController() {
    }

    public void createCashierTableByUser() {
        if (getWebUser()==null) {
            JsfUtil.addErrorMessage("Please Select A User");
            return;
        }
        userBilledBills = createBillsTotals(new BilledBill());
        userCancellededBills = createBillsTotals(new CancelledBill());
        userRefundedBills = createBillsTotals(new RefundBill());
    }

    private double calValue(Bill b, PaymentMethod paymentMethod, WebUser wUser, Department department) {

        String sql;
        Map m = new HashMap();

        sql = "SELECT sum(bfp.amount) FROM BillFeePayment bfp "
                + " join bfp.payment p "
                + " join bfp.billFee.bill b "
                + " WHERE "
                + " bfp.retired=false "
                + " and type(b)=:b"
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
        m.put("ins", getSessionController().getInstitution());

        return getPaymentFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    private List<Object[]> getBillWithTotal(Bill b, PaymentMethod paymentMethod, WebUser wUser, Department department) {

        String sql;
        Map m = new HashMap();

        sql = "SELECT distinct(bfp.billFee.bill),sum(bfp.amount) FROM BillFeePayment bfp WHERE "
                + " bfp.retired=false "
                + " and type(bfp.billFee.bill)=:b"
                + " and bfp.payment.paymentMethod=:pm "
                + " and bfp.payment.institution=:ins "
                + " and bfp.payment.createdAt between :fromDate and :toDate ";
                

        if (department != null) {
            sql += " and bfp.payment.department=:dep ";
            m.put("dep", department);
        }

        if (wUser != null) {
            sql += " and bfp.payment.creater=:w ";
            m.put("w", wUser);
        }
        
        sql+= " group by bfp.billFee.bill "
                + " order by bfp.billFee.bill.createdAt ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("pm", paymentMethod);
        m.put("b", b.getClass());
        m.put("ins", getSessionController().getInstitution());

        System.out.println("paymentMethod = " + paymentMethod);
        System.out.println("sql = " + sql);
        System.out.println("getPaymentFacade().findAggregates(sql, m, TemporalType.TIMESTAMP) = " + getPaymentFacade().findAggregates(sql, m, TemporalType.TIMESTAMP));

        return getPaymentFacade().findAggregates(sql, m, TemporalType.TIMESTAMP);

    }

    public BillsTotals createBillsTotals(Bill b) {
        BillsTotals billsTotals = new BillsTotals();
        List<Bill> bs = new ArrayList<>();
        for (PaymentMethod pm : getPaymentMethods) {
            List<Object[]> objects = getBillWithTotal(b, pm, webUser, department);
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
                        if (bb!=null) {
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
                    System.out.println("1.calValue(paymentMethod, getWebUser(), getDepartment()) = " + calValue(b, paymentMethod, getWebUser(), getDepartment()));
                    billsTotals.setCash(calValue(b, paymentMethod, getWebUser(), getDepartment()));
                    break;
                case Credit:
                    System.out.println("2.calValue(paymentMethod, getWebUser(), getDepartment()) = " + calValue(b, paymentMethod, getWebUser(), getDepartment()));
                    billsTotals.setCredit(calValue(b, paymentMethod, getWebUser(), getDepartment()));
                    break;
                case Card:
                    System.out.println("3.calValue(paymentMethod, getWebUser(), getDepartment()) = " + calValue(b, paymentMethod, getWebUser(), getDepartment()));
                    billsTotals.setCard(calValue(b, paymentMethod, getWebUser(), getDepartment()));
                    break;
                case Slip:
                    System.out.println("4.calValue(paymentMethod, getWebUser(), getDepartment()) = " + calValue(b, paymentMethod, getWebUser(), getDepartment()));
                    billsTotals.setSlip(calValue(b, paymentMethod, getWebUser(), getDepartment()));
                    break;
                case Cheque:
                    System.out.println("5.calValue(paymentMethod, getWebUser(), getDepartment()) = " + calValue(b, paymentMethod, getWebUser(), getDepartment()));
                    billsTotals.setCheque(calValue(b, paymentMethod, getWebUser(), getDepartment()));
                    break;
            }
        }

        return billsTotals;
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

}
