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
import com.divudi.entity.Department;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.PaymentFacade;
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
        userBilledBills=createBillsTotals();
    }

    private double calValue(PaymentMethod paymentMethod, WebUser wUser, Department department) {

        String sql;
        Map m = new HashMap();

        sql = "SELECT sum(p.paidValue) FROM Payment p WHERE"
                + " p.retired=false  "
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
        m.put("ins", getSessionController().getInstitution());

        return getPaymentFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public BillsTotals createBillsTotals() {
        BillsTotals billsTotals = new BillsTotals();

        for (PaymentMethod paymentMethod : getPaymentMethods) {
            switch (paymentMethod) {
                case Cash:
                    System.out.println("1.calValue(paymentMethod, getWebUser(), getDepartment()) = " + calValue(paymentMethod, getWebUser(), getDepartment()));
                    billsTotals.setCash(calValue(paymentMethod, getWebUser(), getDepartment()));
                    break;
                case Credit:
                    System.out.println("2.calValue(paymentMethod, getWebUser(), getDepartment()) = " + calValue(paymentMethod, getWebUser(), getDepartment()));
                    billsTotals.setCredit(calValue(paymentMethod, getWebUser(), getDepartment()));
                    break;
                case Card:
                    System.out.println("3.calValue(paymentMethod, getWebUser(), getDepartment()) = " + calValue(paymentMethod, getWebUser(), getDepartment()));
                    billsTotals.setCard(calValue(paymentMethod, getWebUser(), getDepartment()));
                    break;
                case Slip:
                    System.out.println("4.calValue(paymentMethod, getWebUser(), getDepartment()) = " + calValue(paymentMethod, getWebUser(), getDepartment()));
                    billsTotals.setSlip(calValue(paymentMethod, getWebUser(), getDepartment()));
                    break;
                case Cheque:
                    System.out.println("5.calValue(paymentMethod, getWebUser(), getDepartment()) = " + calValue(paymentMethod, getWebUser(), getDepartment()));
                    billsTotals.setCheque(calValue(paymentMethod, getWebUser(), getDepartment()));
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
        if (fromDate==null) {
            fromDate=getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate==null) {
            toDate=getCommonFunctions().getEndOfDay(new Date());
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

}
