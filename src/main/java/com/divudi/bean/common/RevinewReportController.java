/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.data.BillType;
import com.divudi.data.dataStructure.BillsTotals;

import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

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

    //Opd summery Renew List
    List<BillsTotals> userBilledBillsList;
    List<BillsTotals> userCancellededBillsList;
    List<BillsTotals> userRefundedBillsList;

    //Pharmacy summery Renew List
    List<BillsTotals> userBilledBillsPharmacyList;
    List<BillsTotals> userCancellededBillsPharmacyList;
    List<BillsTotals> userRefundedBillsPharmacyList;

    //all summery
    List<BillsTotals> reNewReportFinalTotal;

    @Inject
    DepartmentController departmentController;

    @Inject
    OpdPreBillReportController opdPreBillReportController;
    
    @Inject
    CommonController commonController;


    CommonFunctions commonFunctions;

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
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if(toDate==null){
            toDate=getCommonFunctions().getEndOfDay(new Date());
        }
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

    public List<BillsTotals> getReNewReportFinalTotal() {
        return reNewReportFinalTotal;
    }

    public void setReNewReportFinalTotal(List<BillsTotals> reNewReportFinalTotal) {
        this.reNewReportFinalTotal = reNewReportFinalTotal;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
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
        reNewReportFinalTotal = new ArrayList<>();
    }

    public void createCashierTableByDepartment() {
        Date startime = new Date();

        makNull();

        makeListNull();

        List<Department> departments = getDepartmentController().listAllDepatrments();
        for (Department dep : departments) {

            List<BillsTotals> bts = new ArrayList<>();

            userBilledBills = getOpdPreBillReportController().createBillsTotals(new BilledBill(), BillType.OpdBill, getWebUser(), getDepartment(), dep);
            if (userBilledBills.getCash() != 0 || userBilledBills.getCard() != 0 || userBilledBills.getAgent() != 0 || userBilledBills.getCheque() != 0
                    || userBilledBills.getSlip() != 0 || userBilledBills.getCredit() != 0) {
                userBilledBillsList.add(userBilledBills);
                bts.add(userBilledBills);
            }

            userCancellededBills = getOpdPreBillReportController().createBillsTotals(new CancelledBill(), BillType.OpdBill, getWebUser(), getDepartment(), dep);
            if (userCancellededBills.getCash() != 0 || userCancellededBills.getCard() != 0 || userCancellededBills.getAgent() != 0 || userCancellededBills.getCheque() != 0
                    || userCancellededBills.getSlip() != 0 || userCancellededBills.getCredit() != 0) {
                userCancellededBillsList.add(userCancellededBills);
                bts.add(userCancellededBills);
            }

            userRefundedBills = getOpdPreBillReportController().createBillsTotals(new RefundBill(), BillType.OpdBill, getWebUser(), getDepartment(), dep);
            if (userRefundedBills.getCash() != 0 || userRefundedBills.getCard() != 0 || userRefundedBills.getAgent() != 0 || userRefundedBills.getCheque() != 0
                    || userRefundedBills.getSlip() != 0 || userRefundedBills.getCredit() != 0) {
                userRefundedBillsList.add(userRefundedBills);
                bts.add(userRefundedBills);
            }

            userBilledBillsPharmacy = getOpdPreBillReportController().createBillsTotals(new BilledBill(), BillType.PharmacySale, getWebUser(), getDepartment(), dep);
            if (userBilledBillsPharmacy.getCash() != 0 || userBilledBillsPharmacy.getCard() != 0 || userBilledBillsPharmacy.getAgent() != 0 || userBilledBillsPharmacy.getCheque() != 0
                    || userBilledBillsPharmacy.getSlip() != 0 || userBilledBillsPharmacy.getCredit() != 0) {
                userBilledBillsPharmacyList.add(userBilledBillsPharmacy);
                bts.add(userBilledBillsPharmacy);
            }

            userCancellededBillsPharmacy = getOpdPreBillReportController().createBillsTotals(new CancelledBill(), BillType.PharmacySale, getWebUser(), getDepartment(), dep);
            if (userCancellededBillsPharmacy.getCash() != 0 || userCancellededBillsPharmacy.getCard() != 0 || userCancellededBillsPharmacy.getAgent() != 0 || userCancellededBillsPharmacy.getCheque() != 0
                    || userCancellededBillsPharmacy.getSlip() != 0 || userCancellededBillsPharmacy.getCredit() != 0) {
                userCancellededBillsPharmacyList.add(userCancellededBillsPharmacy);
                bts.add(userCancellededBillsPharmacy);
            }

            userRefundedBillsPharmacy = getOpdPreBillReportController().createBillsTotals(new RefundBill(), BillType.PharmacySale, getWebUser(), getDepartment(), dep);
            if (userRefundedBillsPharmacy.getCash() != 0 || userRefundedBillsPharmacy.getCard() != 0 || userRefundedBillsPharmacy.getAgent() != 0 || userRefundedBillsPharmacy.getCheque() != 0
                    || userRefundedBillsPharmacy.getSlip() != 0 || userRefundedBillsPharmacy.getCredit() != 0) {
                userRefundedBillsPharmacyList.add(userRefundedBillsPharmacy);
                bts.add(userRefundedBillsPharmacy);
            }
            BillsTotals bt = createReNewTotal(bts, dep);
            if (bt.getCash() != 0 || bt.getCard() != 0 || bt.getAgent() != 0 || bt.getCheque() != 0
                    || bt.getSlip() != 0 || bt.getCredit() != 0) {
                reNewReportFinalTotal.add(bt);
            }

        }
        userBilledBillsList.add(createReNewTotal(userBilledBillsList, null));
        userCancellededBillsList.add(createReNewTotal(userCancellededBillsList, null));
        userRefundedBillsList.add(createReNewTotal(userRefundedBillsList, null));
        userBilledBillsPharmacyList.add(createReNewTotal(userBilledBillsPharmacyList, null));
        userCancellededBillsPharmacyList.add(createReNewTotal(userCancellededBillsPharmacyList, null));
        userRefundedBillsPharmacyList.add(createReNewTotal(userRefundedBillsPharmacyList, null));
        reNewReportFinalTotal.add(createReNewTotal(reNewReportFinalTotal, null));

        
        
    }

    public BillsTotals createReNewTotal(List<BillsTotals> bts, Department dep) {
        BillsTotals bt = new BillsTotals();
        double cash = 0.0;
        double card = 0.0;
        double agent = 0.0;
        double cheque = 0.0;
        double slip = 0.0;
        double credit = 0.0;
        for (BillsTotals billsTotals : bts) {
            cash += billsTotals.getCash();
            card += billsTotals.getCard();
            agent += billsTotals.getAgent();
            cheque += billsTotals.getCheque();
            slip += billsTotals.getSlip();
            credit += billsTotals.getCredit();
        }
        if (dep != null) {
            bt.setName(dep.getName());
        } else {
            bt.setName("Total");
            bt.setBold(true);
        }
        bt.setCash(cash);
        bt.setCard(card);
        bt.setAgent(agent);
        bt.setCheque(cheque);
        bt.setSlip(slip);
        bt.setCredit(credit);
        return bt;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }
    
}
