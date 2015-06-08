/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.data.BillType;
import com.divudi.data.dataStructure.BillsTotals;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;

/**
 *
 * @author neo
 */
@Named(value = "revinewReportController")
@SessionScoped
public class RevinewReportController implements Serializable {

    /**
     * Creates a new instance of RevinewReportController
     */
    WebUser webUser;
    Department department;//cash getting dept - cashier
    Department toDepartment;// lab/channel/opd
    Date fromDate;
    Date toDate;

    //Opd summery
    BillsTotals userBilledBills;
    BillsTotals userCancellededBills;
    BillsTotals userRefundedBills;

    //Pharmacy summery
    BillsTotals userBilledBillsPharmacy;
    BillsTotals userCancellededBillsPharmacy;
    BillsTotals userRefundedBillsPharmacy;

    //Opd summery List
    List<BillsTotals> userBilledBillsList;
    List<BillsTotals> userCancellededBillsList;
    List<BillsTotals> userRefundedBillsList;

    //Pharmacy summery List
    List<BillsTotals> userBilledBillsPharmacyList;
    List<BillsTotals> userCancellededBillsPharmacyList;
    List<BillsTotals> userRefundedBillsPharmacyList;

    //all summery
    List<BillsTotals> reNewReportFinalTotal;

    //Opd summery
    double userBilledBillsTotal;
    double userCancellededBillsTotal;
    double userRefundedBillsTotal;

    //Pharmacy summery
    double userBilledBillsPharmacyTotal;
    double userCancellededBillsPharmacyTotal;
    double userRefundedBillsPharmacyTotal;

    @Inject
    DepartmentController departmentController;

    @Inject
    OpdPreBillReportController opdPreBillReportController;

    public RevinewReportController() {
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

    public Department getToDepartment() {
        return toDepartment;
    }

    public void setToDepartment(Department toDepartment) {
        this.toDepartment = toDepartment;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public BillsTotals getUserBilledBills() {
        if (userBilledBills == null) {
            userBilledBills = new BillsTotals();
        }
        return userBilledBills;
    }

    public void setUserBilledBills(BillsTotals userBilledBills) {
        this.userBilledBills = userBilledBills;
    }

    public BillsTotals getUserCancellededBills() {
        if (userCancellededBills == null) {
            userCancellededBills = new BillsTotals();
        }
        return userCancellededBills;
    }

    public void setUserCancellededBills(BillsTotals userCancellededBills) {
        this.userCancellededBills = userCancellededBills;
    }

    public BillsTotals getUserRefundedBills() {
        if (userRefundedBills == null) {
            userRefundedBills = new BillsTotals();
        }
        return userRefundedBills;
    }

    public void setUserRefundedBills(BillsTotals userRefundedBills) {
        this.userRefundedBills = userRefundedBills;
    }

    public BillsTotals getUserBilledBillsPharmacy() {
        if (userBilledBillsPharmacy == null) {
            userBilledBillsPharmacy = new BillsTotals();
        }
        return userBilledBillsPharmacy;
    }

    public void setUserBilledBillsPharmacy(BillsTotals userBilledBillsPharmacy) {
        this.userBilledBillsPharmacy = userBilledBillsPharmacy;
    }

    public BillsTotals getUserCancellededBillsPharmacy() {
        if (userCancellededBillsPharmacy == null) {
            userCancellededBillsPharmacy = new BillsTotals();
        }
        return userCancellededBillsPharmacy;
    }

    public void setUserCancellededBillsPharmacy(BillsTotals userCancellededBillsPharmacy) {
        this.userCancellededBillsPharmacy = userCancellededBillsPharmacy;
    }

    public BillsTotals getUserRefundedBillsPharmacy() {
        if (userRefundedBillsPharmacy == null) {
            userRefundedBillsPharmacy = new BillsTotals();
        }
        return userRefundedBillsPharmacy;
    }

    public void setUserRefundedBillsPharmacy(BillsTotals userRefundedBillsPharmacy) {
        this.userRefundedBillsPharmacy = userRefundedBillsPharmacy;
    }

    public DepartmentController getDepartmentController() {
        return departmentController;
    }

    public void setDepartmentController(DepartmentController departmentController) {
        this.departmentController = departmentController;
    }

    public OpdPreBillReportController getOpdPreBillReportController() {
        return opdPreBillReportController;
    }

    public void setOpdPreBillReportController(OpdPreBillReportController opdPreBillReportController) {
        this.opdPreBillReportController = opdPreBillReportController;
    }

    public List<BillsTotals> getUserBilledBillsList() {
        return userBilledBillsList;
    }

    public void setUserBilledBillsList(List<BillsTotals> userBilledBillsList) {
        this.userBilledBillsList = userBilledBillsList;
    }

    public List<BillsTotals> getUserCancellededBillsList() {
        return userCancellededBillsList;
    }

    public void setUserCancellededBillsList(List<BillsTotals> userCancellededBillsList) {
        this.userCancellededBillsList = userCancellededBillsList;
    }

    public List<BillsTotals> getUserRefundedBillsList() {
        return userRefundedBillsList;
    }

    public void setUserRefundedBillsList(List<BillsTotals> userRefundedBillsList) {
        this.userRefundedBillsList = userRefundedBillsList;
    }

    public List<BillsTotals> getUserBilledBillsPharmacyList() {
        return userBilledBillsPharmacyList;
    }

    public void setUserBilledBillsPharmacyList(List<BillsTotals> userBilledBillsPharmacyList) {
        this.userBilledBillsPharmacyList = userBilledBillsPharmacyList;
    }

    public List<BillsTotals> getUserCancellededBillsPharmacyList() {
        return userCancellededBillsPharmacyList;
    }

    public void setUserCancellededBillsPharmacyList(List<BillsTotals> userCancellededBillsPharmacyList) {
        this.userCancellededBillsPharmacyList = userCancellededBillsPharmacyList;
    }

    public List<BillsTotals> getUserRefundedBillsPharmacyList() {
        return userRefundedBillsPharmacyList;
    }

    public void setUserRefundedBillsPharmacyList(List<BillsTotals> userRefundedBillsPharmacyList) {
        this.userRefundedBillsPharmacyList = userRefundedBillsPharmacyList;
    }

    public double getUserBilledBillsTotal() {
        return userBilledBillsTotal;
    }

    public void setUserBilledBillsTotal(double userBilledBillsTotal) {
        this.userBilledBillsTotal = userBilledBillsTotal;
    }

    public double getUserCancellededBillsTotal() {
        return userCancellededBillsTotal;
    }

    public void setUserCancellededBillsTotal(double userCancellededBillsTotal) {
        this.userCancellededBillsTotal = userCancellededBillsTotal;
    }

    public double getUserRefundedBillsTotal() {
        return userRefundedBillsTotal;
    }

    public void setUserRefundedBillsTotal(double userRefundedBillsTotal) {
        this.userRefundedBillsTotal = userRefundedBillsTotal;
    }

    public double getUserBilledBillsPharmacyTotal() {
        return userBilledBillsPharmacyTotal;
    }

    public void setUserBilledBillsPharmacyTotal(double userBilledBillsPharmacyTotal) {
        this.userBilledBillsPharmacyTotal = userBilledBillsPharmacyTotal;
    }

    public double getUserCancellededBillsPharmacyTotal() {
        return userCancellededBillsPharmacyTotal;
    }

    public void setUserCancellededBillsPharmacyTotal(double userCancellededBillsPharmacyTotal) {
        this.userCancellededBillsPharmacyTotal = userCancellededBillsPharmacyTotal;
    }

    public double getUserRefundedBillsPharmacyTotal() {
        return userRefundedBillsPharmacyTotal;
    }

    public void setUserRefundedBillsPharmacyTotal(double userRefundedBillsPharmacyTotal) {
        this.userRefundedBillsPharmacyTotal = userRefundedBillsPharmacyTotal;
    }

    public List<BillsTotals> getReNewReportFinalTotal() {
        return reNewReportFinalTotal;
    }

    public void setReNewReportFinalTotal(List<BillsTotals> reNewReportFinalTotal) {
        this.reNewReportFinalTotal = reNewReportFinalTotal;
    }

    public void makNull() {
        webUser = null;
        department = null;
        toDepartment = null;
    }

    public void makeListNull() {
        userBilledBillsList = new ArrayList<>();
        userCancellededBillsList = new ArrayList<>();
        userRefundedBillsList = new ArrayList<>();
        userBilledBillsPharmacyList = new ArrayList<>();
        userCancellededBillsPharmacyList = new ArrayList<>();
        userRefundedBillsPharmacyList = new ArrayList<>();
        reNewReportFinalTotal=new ArrayList<>();
    }

    public void makeZero() {
        userBilledBillsTotal = 0.0;
        userCancellededBillsTotal = 0.0;
        userRefundedBillsTotal = 0.0;
        userBilledBillsPharmacyTotal = 0.0;
        userCancellededBillsPharmacyTotal = 0.0;
        userRefundedBillsPharmacyTotal = 0.0;
    }

    public void createCashierTableByDepartment() {

        makNull();

        makeListNull();

        makeZero();

        List<Department> departments = getDepartmentController().listAllDepatrments();

        for (Department dep : departments) {
            BillsTotals bt = new BillsTotals();

            System.out.println("dep" + dep.getName());

            userBilledBills = getOpdPreBillReportController().createBillsTotals(new BilledBill(), BillType.OpdBill, getWebUser(), getDepartment(), dep);
            if (userBilledBills.getCash() != 0 || userBilledBills.getCard() != 0 || userBilledBills.getAgent() != 0 || userBilledBills.getCheque() != 0
                    || userBilledBills.getSlip() != 0 || userBilledBills.getCredit() != 0) {
                System.out.println("userBilledBills.getCash()" + userBilledBills.getCash());
                userBilledBillsList.add(userBilledBills);
            }

            userCancellededBills = getOpdPreBillReportController().createBillsTotals(new CancelledBill(), BillType.OpdBill, getWebUser(), getDepartment(), dep);
            if (userCancellededBills.getCash() != 0 || userCancellededBills.getCard() != 0 || userCancellededBills.getAgent() != 0 || userCancellededBills.getCheque() != 0
                    || userCancellededBills.getSlip() != 0 || userCancellededBills.getCredit() != 0) {
                System.out.println("userCancellededBills.getCash()" + userCancellededBills.getCash());
                userCancellededBillsList.add(userCancellededBills);
            }

            userRefundedBills = getOpdPreBillReportController().createBillsTotals(new RefundBill(), BillType.OpdBill, getWebUser(), getDepartment(), dep);
            if (userRefundedBills.getCash() != 0 || userRefundedBills.getCard() != 0 || userRefundedBills.getAgent() != 0 || userRefundedBills.getCheque() != 0
                    || userRefundedBills.getSlip() != 0 || userRefundedBills.getCredit() != 0) {
                System.out.println("userRefundedBills.getCash()" + userRefundedBills.getCash());
                userRefundedBillsList.add(userRefundedBills);
            }

            userBilledBillsPharmacy = getOpdPreBillReportController().createBillsTotals(new BilledBill(), BillType.PharmacySale, getWebUser(), getDepartment(), dep);
            if (userBilledBillsPharmacy.getCash() != 0 || userBilledBillsPharmacy.getCard() != 0 || userBilledBillsPharmacy.getAgent() != 0 || userBilledBillsPharmacy.getCheque() != 0
                    || userBilledBillsPharmacy.getSlip() != 0 || userBilledBillsPharmacy.getCredit() != 0) {
                System.out.println("userBilledBillsPharmacy.getCash()" + userBilledBillsPharmacy.getCash());
                userBilledBillsPharmacyList.add(userBilledBillsPharmacy);
            }

            userCancellededBillsPharmacy = getOpdPreBillReportController().createBillsTotals(new CancelledBill(), BillType.PharmacySale, getWebUser(), getDepartment(), dep);
            if (userCancellededBillsPharmacy.getCash() != 0 || userCancellededBillsPharmacy.getCard() != 0 || userCancellededBillsPharmacy.getAgent() != 0 || userCancellededBillsPharmacy.getCheque() != 0
                    || userCancellededBillsPharmacy.getSlip() != 0 || userCancellededBillsPharmacy.getCredit() != 0) {
                System.out.println("userCancellededBillsPharmacy.getCash()" + userCancellededBillsPharmacy.getCash());
                userCancellededBillsPharmacyList.add(userCancellededBillsPharmacy);
            }

            userRefundedBillsPharmacy = getOpdPreBillReportController().createBillsTotals(new RefundBill(), BillType.PharmacySale, getWebUser(), getDepartment(), dep);
            if (userRefundedBillsPharmacy.getCash() != 0 || userRefundedBillsPharmacy.getCard() != 0 || userRefundedBillsPharmacy.getAgent() != 0 || userRefundedBillsPharmacy.getCheque() != 0
                    || userRefundedBillsPharmacy.getSlip() != 0 || userRefundedBillsPharmacy.getCredit() != 0) {
                System.out.println("userRefundedBillsPharmacy.getCash()" + userRefundedBillsPharmacy.getCash());
                userRefundedBillsPharmacyList.add(userRefundedBillsPharmacy);
            }

            bt.setName(dep.getName());
            bt.setCash(userBilledBills.getCash() + userCancellededBills.getCash() + userRefundedBills.getCash()
                    + userBilledBillsPharmacy.getCash() + userCancellededBillsPharmacy.getCash() + userRefundedBillsPharmacy.getCash());
            bt.setAgent(userBilledBills.getAgent() + userCancellededBills.getAgent() + userRefundedBills.getAgent()
                    + userBilledBillsPharmacy.getAgent() + userCancellededBillsPharmacy.getAgent() + userRefundedBillsPharmacy.getAgent());
            bt.setCard(userBilledBills.getCard() + userCancellededBills.getCard() + userRefundedBills.getCard()
                    + userBilledBillsPharmacy.getCard() + userCancellededBillsPharmacy.getCard() + userRefundedBillsPharmacy.getCard());
            bt.setCheque(userBilledBills.getCheque() + userCancellededBills.getCheque() + userRefundedBills.getCheque()
                    + userBilledBillsPharmacy.getCheque() + userCancellededBillsPharmacy.getCheque() + userRefundedBillsPharmacy.getCheque());
            bt.setCredit(userBilledBills.getCredit() + userCancellededBills.getCredit() + userRefundedBills.getCredit()
                    + userBilledBillsPharmacy.getCredit() + userCancellededBillsPharmacy.getCredit() + userRefundedBillsPharmacy.getCredit());
            bt.setSlip(userBilledBills.getSlip() + userCancellededBills.getSlip() + userRefundedBills.getSlip()
                    + userBilledBillsPharmacy.getSlip() + userCancellededBillsPharmacy.getSlip() + userRefundedBillsPharmacy.getSlip());
            
            if (bt.getCash() != 0 || bt.getCard() != 0 || bt.getAgent() != 0 || bt.getCheque() != 0
                    || bt.getSlip() != 0 || bt.getCredit() != 0) {
                reNewReportFinalTotal.add(bt);
            }

        }

    }

    public void getTotal() {
        double cash;
        double card;
        double agent;
        double cheque;
        double slip;
        double credit;

    }

}
