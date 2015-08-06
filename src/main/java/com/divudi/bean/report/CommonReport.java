/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.report;

import com.divudi.bean.common.SessionController;
import com.divudi.data.BillType;
import com.divudi.data.DepartmentType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.BillsTotals;
import com.divudi.data.table.String1Value1;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PriceMatrixFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author www.divudi.com
 */
@Named
@RequestScoped
public class CommonReport implements Serializable {

    @Inject
    SessionController sessionController;
    ///////////////////
    @EJB
    private BillFacade billFacade;
    @EJB
    CommonFunctions commonFunctions;
    @EJB
    private PriceMatrixFacade inwdPriceAdjFacade;
    @EJB
    BillItemFacade billItemFac;
    @EJB
    BillFeeFacade billFeeFacade;
    ////////////////////
    List<BillFee> billFees;
    List<Bill> referralBills;
    List<BillItem> referralBillItems;

    List<Bill> pharmacyCashBilledBills;
    List<Bill> pharmacyCashCancelBills;
    List<Bill> pharmacyCashReturnbill;

    List<Bill> pharmacyCreditBilledBills;
    List<Bill> pharmacyCreditCancelBills;
    List<Bill> pharmacyCreditReturnbill;

    List<Bill> pharmacyBhtIssueBilledBills;
    List<Bill> pharmacyBhtIssueCancelBills;
    List<Bill> pharmacyBhtIssueReturnbill;

    List<Bill> pharmacyUnitIssueBilledBills;
    List<Bill> pharmacyUnitIssueCancelBills;
    List<Bill> pharmacyUnitIssueReturnbill;

    double pharmacyCashBilledBillTotals;
    double pharmacyCashCancelBillTotals;
    double pharmacyCashReturnbillTotals;

    double pharmacyCreditBilledBillTotals;
    double pharmacyCreditCancelBillTotals;
    double pharmacyCreditReturnbillTotals;

    double pharmacyBhtIssueBilledBillTotals;
    double pharmacyBhtIssueCancelBillTotals;
    double pharmacyBhtIssueReturnbillTotals;

    double pharmacyUnitIssueBilledBillTotals;
    double pharmacyUnitIssueCancelBillTotals;
    double pharmacyUnitIssueReturnbillTotals;

    ////////////////
    private Institution collectingIns;
    Institution institution;
    Institution ins;
    private Date fromDate;
    private Date toDate;
    private WebUser webUser;
    private Department department;
    private BillType billType;
    private Institution creditCompany;
    private Institution referenceInstitution;
    private Item referenceItem;
    /////////////////////
    private BillsTotals billedBills;
    private BillsTotals cancellededBills;
    private BillsTotals refundedBills;
    private BillsTotals billedBillsPh;
    
    /////pharmacy whole sale
    private BillsTotals cancelledPhWholeSale;
    private BillsTotals refundedPhWholeSale;
    private BillsTotals billedPhWholeSale;
    
    private BillsTotals billedBillsCh;
    
    private BillsTotals billedBillsPh2;
    private BillsTotals cancellededBillsPh;
    private BillsTotals cancellededBillsPh2;
    private BillsTotals refundedBillsPh;
    private BillsTotals refundedBillsPh2;
    private BillsTotals paymentBills;
    private BillsTotals paymentCancelBills;
    private BillsTotals pettyPayments;
    private BillsTotals pettyPaymentsCancel;
    private BillsTotals cashRecieves;
    private BillsTotals cashRecieveCancel;
    private BillsTotals agentRecieves;
    private BillsTotals agentCancelBill;
    private BillsTotals inwardPayments;
    private BillsTotals inwardPaymentCancel;
    private BillsTotals inwardRefunds;
    private BillsTotals grnBilled;
    private BillsTotals grnCancelled;
    private BillsTotals grnReturn;
    private BillsTotals grnReturnCancel;
    private BillsTotals purchaseBilled;
    private BillsTotals purchaseCancelled;
    private BillsTotals purchaseReturn;
    private BillsTotals purchaseReturnCancel;
    private BillsTotals GrnPaymentBill;
    private BillsTotals GrnPaymentReturn;
    private BillsTotals GrnPaymentCancell;
    private BillsTotals GrnPaymentCancellReturn;
    private BillsTotals PharmacyBhtPreBilled;
    private BillsTotals PharmacyBhtPreCancelled;
    private BillsTotals PharmacyBhtPreRefunded;
    private BillsTotals StoreBhtPreBilled;
    private BillsTotals StoreBhtPreCancelled;
    private BillsTotals StoreBhtPreRefunded;
    BillsTotals cashInBills;
    BillsTotals cashInBillsCancel;
    BillsTotals cashOutBills;
    BillsTotals cashOutBillsCancel;
    BillsTotals cashAdjustmentBills;
    BillsTotals InwardPaymentBill;
    BillsTotals channelCancells;
    BillsTotals channelBilled;
    BillsTotals channelRefunds;
    BillsTotals channelCancellProPayment;
    BillsTotals channelBilledProPayment;
    BillsTotals channelRefundsProPayment;
    BillsTotals channelCancellAgnPayment;
    BillsTotals channelBilledAgnPayment;
    BillsTotals channelRefundAgnPayment;
    List<Bill> bills;

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public double displayOutsideCalBillFees() {
        String jpql;
        jpql = "Select sum(bf.feeValue) from BillFee bf where bf.fee.feeType=:bft and bf.fee.institution=:ins "
                + "and bf.bill.createdAt between :fd and :td "
                + "order by bf.id";
        Map m = new HashMap();
        m.put("bft", FeeType.OtherInstitution);
        m.put("ins", institution);
        m.put("fd", fromDate);
        m.put("td", toDate);
        return getBillFeeFacade().findDoubleByJpql(jpql, m, TemporalType.TIMESTAMP);
    }

    public double displayOutsideBillFeeBillTotals() {
        String jpql;
        jpql = "Select sum(bf.billItem.netValue) from BillFee bf where bf.fee.feeType=:bft and bf.fee.institution=:ins "
                + "and bf.bill.createdAt between :fd and :td "
                + "order by bf.id";
        Map m = new HashMap();
        m.put("bft", FeeType.OtherInstitution);
        m.put("ins", institution);
        m.put("fd", fromDate);
        m.put("td", toDate);
        return getBillFeeFacade().findDoubleByJpql(jpql, m, TemporalType.TIMESTAMP);
    }

    double totalFee;
    double billTotal;

    public double getBillTotal() {
        return billTotal;
    }

    public void setBillTotal(double billTotal) {
        this.billTotal = billTotal;
    }

    public double getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(double totalFee) {
        this.totalFee = totalFee;
    }

    public String displayOutsideBillFees() {
        String jpql;
        BillFee bf = new BillFee();

        //bf.getFee().getInstitution();
        //bf.getFee().getFeeType()==FeeType.OtherInstitution;
        jpql = "Select bf from BillFee bf where bf.fee.feeType=:bft and bf.fee.institution=:ins "
                + "and bf.bill.createdAt between :fd and :td "
                + "order by bf.id";
        Map m = new HashMap();
        m.put("bft", FeeType.OtherInstitution);
        m.put("ins", institution);
        m.put("fd", fromDate);
        m.put("td", toDate);
        billFees = getBillFeeFacade().findBySQL(jpql, m, TemporalType.TIMESTAMP);
        totalFee = displayOutsideCalBillFees();
        billTotal = displayOutsideBillFeeBillTotals();
        return "/lab/lab_report_by_outside_institution";
    }

    public BillsTotals getChannelCancells() {
        if (channelCancells == null) {
            channelCancells = new BillsTotals();
        }
        return channelCancells;
    }

    public void setChannelCancells(BillsTotals channelCancells) {
        this.channelCancells = channelCancells;
    }

    public BillsTotals getChannelBilled() {
        if (channelBilled == null) {
            channelBilled = new BillsTotals();
        }
        return channelBilled;
    }

    public void setChannelBilled(BillsTotals channelBilled) {
        this.channelBilled = channelBilled;
    }

    public BillsTotals getChannelRefunds() {
        if (channelRefunds == null) {
            channelRefunds = new BillsTotals();
        }
        return channelRefunds;
    }

    public void setChannelRefunds(BillsTotals channelRefunds) {
        this.channelRefunds = channelRefunds;
    }

    public BillsTotals getInwardPaymentBill() {
        return InwardPaymentBill;
    }

    public void setInwardPaymentBill(BillsTotals InwardPaymentBill) {
        this.InwardPaymentBill = InwardPaymentBill;
    }

    public BillsTotals getCashAdjustmentBills() {
        if (cashAdjustmentBills == null) {
            cashAdjustmentBills = new BillsTotals();
        }
        return cashAdjustmentBills;
    }

    public void setCashAdjustmentBills(BillsTotals cashAdjustmentBills) {
        this.cashAdjustmentBills = cashAdjustmentBills;
    }

    public BillsTotals getCashInBillsCancel() {
        if (cashInBillsCancel == null) {
            cashInBillsCancel = new BillsTotals();
        }
        return cashInBillsCancel;
    }

    public void setCashInBillsCancel(BillsTotals cashInBillsCancel) {
        this.cashInBillsCancel = cashInBillsCancel;
    }

    public BillsTotals getCashOutBillsCancel() {
        if (cashOutBillsCancel == null) {
            cashOutBillsCancel = new BillsTotals();
        }
        return cashOutBillsCancel;
    }

    public void setCashOutBillsCancel(BillsTotals cashOutBillsCancel) {
        this.cashOutBillsCancel = cashOutBillsCancel;
    }

    public BillsTotals getCashInBills() {
        if (cashInBills == null) {
            cashInBills = new BillsTotals();
        }
        return cashInBills;
    }

    public void setCashInBills(BillsTotals cashInBills) {
        this.cashInBills = cashInBills;
    }

    public BillsTotals getCashOutBills() {
        if (cashOutBills == null) {
            cashOutBills = new BillsTotals();
        }
        return cashOutBills;
    }

    public void setCashOutBills(BillsTotals cashOutBills) {
        this.cashOutBills = cashOutBills;
    }

    //////////////////    
    private List<String1Value1> dataTableData;
    private List<PriceMatrix> items = null;

    /**
     * Creates a new instance of CommonReport
     */
    public CommonReport() {
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        recreteModal();
        this.institution = institution;

    }

    public BillType[] getBillTypes() {
        return BillType.values();
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
        recreteModal();
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
        recreteModal();
    }

    public WebUser getWebUser() {

        return webUser;
    }

    public void setWebUser(WebUser webUser) {
        this.webUser = webUser;
        recreteModal();
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
        recreteModal();
    }

    public List<Bill> getPharmacyCashBilledBills() {
        return pharmacyCashBilledBills;
    }

    public void setPharmacyCashBilledBills(List<Bill> pharmacyCashBilledBills) {
        this.pharmacyCashBilledBills = pharmacyCashBilledBills;
    }

    public List<Bill> getPharmacyCashCancelBills() {
        return pharmacyCashCancelBills;
    }

    public void setPharmacyCashCancelBills(List<Bill> pharmacyCashCancelBills) {
        this.pharmacyCashCancelBills = pharmacyCashCancelBills;
    }

    public List<Bill> getPharmacyCashReturnbill() {
        return pharmacyCashReturnbill;
    }

    public void setPharmacyCashReturnbill(List<Bill> pharmacyCashReturnbill) {
        this.pharmacyCashReturnbill = pharmacyCashReturnbill;
    }

    public List<Bill> getPharmacyCreditBilledBills() {
        return pharmacyCreditBilledBills;
    }

    public void setPharmacyCreditBilledBills(List<Bill> pharmacyCreditBilledBills) {
        this.pharmacyCreditBilledBills = pharmacyCreditBilledBills;
    }

    public List<Bill> getPharmacyCreditCancelBills() {
        return pharmacyCreditCancelBills;
    }

    public void setPharmacyCreditCancelBills(List<Bill> pharmacyCreditCancelBills) {
        this.pharmacyCreditCancelBills = pharmacyCreditCancelBills;
    }

    public List<Bill> getPharmacyCreditReturnbill() {
        return pharmacyCreditReturnbill;
    }

    public void setPharmacyCreditReturnbill(List<Bill> pharmacyCreditReturnbill) {
        this.pharmacyCreditReturnbill = pharmacyCreditReturnbill;
    }

    public List<Bill> getPharmacyBhtIssueBilledBills() {
        return pharmacyBhtIssueBilledBills;
    }

    public void setPharmacyBhtIssueBilledBills(List<Bill> pharmacyBhtIssueBilledBills) {
        this.pharmacyBhtIssueBilledBills = pharmacyBhtIssueBilledBills;
    }

    public List<Bill> getPharmacyBhtIssueCancelBills() {
        return pharmacyBhtIssueCancelBills;
    }

    public void setPharmacyBhtIssueCancelBills(List<Bill> pharmacyBhtIssueCancelBills) {
        this.pharmacyBhtIssueCancelBills = pharmacyBhtIssueCancelBills;
    }

    public List<Bill> getPharmacyBhtIssueReturnbill() {
        return pharmacyBhtIssueReturnbill;
    }

    public void setPharmacyBhtIssueReturnbill(List<Bill> pharmacyBhtIssueReturnbill) {
        this.pharmacyBhtIssueReturnbill = pharmacyBhtIssueReturnbill;
    }

    public List<Bill> getPharmacyUnitIssueBilledBills() {
        return pharmacyUnitIssueBilledBills;
    }

    public void setPharmacyUnitIssueBilledBills(List<Bill> pharmacyUnitIssueBilledBills) {
        this.pharmacyUnitIssueBilledBills = pharmacyUnitIssueBilledBills;
    }

    public List<Bill> getPharmacyUnitIssueCancelBills() {
        return pharmacyUnitIssueCancelBills;
    }

    public void setPharmacyUnitIssueCancelBills(List<Bill> pharmacyUnitIssueCancelBills) {
        this.pharmacyUnitIssueCancelBills = pharmacyUnitIssueCancelBills;
    }

    public List<Bill> getPharmacyUnitIssueReturnbill() {
        return pharmacyUnitIssueReturnbill;
    }

    public void setPharmacyUnitIssueReturnbill(List<Bill> pharmacyUnitIssueReturnbill) {
        this.pharmacyUnitIssueReturnbill = pharmacyUnitIssueReturnbill;
    }

    public double getPharmacyCashBilledBillTotals() {
        return pharmacyCashBilledBillTotals;
    }

    public void setPharmacyCashBilledBillTotals(double pharmacyCashBilledBillTotals) {
        this.pharmacyCashBilledBillTotals = pharmacyCashBilledBillTotals;
    }

    public double getPharmacyCashCancelBillTotals() {
        return pharmacyCashCancelBillTotals;
    }

    public void setPharmacyCashCancelBillTotals(double pharmacyCashCancelBillTotals) {
        this.pharmacyCashCancelBillTotals = pharmacyCashCancelBillTotals;
    }

    public double getPharmacyCashReturnbillTotals() {
        return pharmacyCashReturnbillTotals;
    }

    public void setPharmacyCashReturnbillTotals(double pharmacyCashReturnbillTotals) {
        this.pharmacyCashReturnbillTotals = pharmacyCashReturnbillTotals;
    }

    public double getPharmacyCreditBilledBillTotals() {
        return pharmacyCreditBilledBillTotals;
    }

    public void setPharmacyCreditBilledBillTotals(double pharmacyCreditBilledBillTotals) {
        this.pharmacyCreditBilledBillTotals = pharmacyCreditBilledBillTotals;
    }

    public double getPharmacyCreditCancelBillTotals() {
        return pharmacyCreditCancelBillTotals;
    }

    public void setPharmacyCreditCancelBillTotals(double pharmacyCreditCancelBillTotals) {
        this.pharmacyCreditCancelBillTotals = pharmacyCreditCancelBillTotals;
    }

    public double getPharmacyCreditReturnbillTotals() {
        return pharmacyCreditReturnbillTotals;
    }

    public void setPharmacyCreditReturnbillTotals(double pharmacyCreditReturnbillTotals) {
        this.pharmacyCreditReturnbillTotals = pharmacyCreditReturnbillTotals;
    }

    public double getPharmacyBhtIssueBilledBillTotals() {
        return pharmacyBhtIssueBilledBillTotals;
    }

    public void setPharmacyBhtIssueBilledBillTotals(double pharmacyBhtIssueBilledBillTotals) {
        this.pharmacyBhtIssueBilledBillTotals = pharmacyBhtIssueBilledBillTotals;
    }

    public double getPharmacyBhtIssueCancelBillTotals() {
        return pharmacyBhtIssueCancelBillTotals;
    }

    public void setPharmacyBhtIssueCancelBillTotals(double pharmacyBhtIssueCancelBillTotals) {
        this.pharmacyBhtIssueCancelBillTotals = pharmacyBhtIssueCancelBillTotals;
    }

    public double getPharmacyBhtIssueReturnbillTotals() {
        return pharmacyBhtIssueReturnbillTotals;
    }

    public void setPharmacyBhtIssueReturnbillTotals(double pharmacyBhtIssueReturnbillTotals) {
        this.pharmacyBhtIssueReturnbillTotals = pharmacyBhtIssueReturnbillTotals;
    }

    public double getPharmacyUnitIssueBilledBillTotals() {
        return pharmacyUnitIssueBilledBillTotals;
    }

    public void setPharmacyUnitIssueBilledBillTotals(double pharmacyUnitIssueBilledBillTotals) {
        this.pharmacyUnitIssueBilledBillTotals = pharmacyUnitIssueBilledBillTotals;
    }

    public double getPharmacyUnitIssueCancelBillTotals() {
        return pharmacyUnitIssueCancelBillTotals;
    }

    public void setPharmacyUnitIssueCancelBillTotals(double pharmacyUnitIssueCancelBillTotals) {
        this.pharmacyUnitIssueCancelBillTotals = pharmacyUnitIssueCancelBillTotals;
    }

    public double getPharmacyUnitIssueReturnbillTotals() {
        return pharmacyUnitIssueReturnbillTotals;
    }

    public void setPharmacyUnitIssueReturnbillTotals(double pharmacyUnitIssueReturnbillTotals) {
        this.pharmacyUnitIssueReturnbillTotals = pharmacyUnitIssueReturnbillTotals;
    }

    public Institution getIns() {
        return ins;
    }

    public void setIns(Institution ins) {
        this.ins = ins;
    }

    public BillsTotals getChannelCancellProPayment() {
        if(channelCancellProPayment==null){
            channelCancellProPayment=new BillsTotals();
        }
        return channelCancellProPayment;
    }

    public void setChannelCancellProPayment(BillsTotals channelCancellProPayment) {
        this.channelCancellProPayment = channelCancellProPayment;
    }

    public BillsTotals getChannelBilledProPayment() {
        if(channelBilledProPayment==null){
            channelBilledProPayment=new BillsTotals();
        }
        return channelBilledProPayment;
    }

    public void setChannelBilledProPayment(BillsTotals channelBilledProPayment) {
        this.channelBilledProPayment = channelBilledProPayment;
    }

    public BillsTotals getChannelRefundsProPayment() {
        if(channelRefundsProPayment==null){
            channelRefundsProPayment=new BillsTotals();
        }
        return channelRefundsProPayment;
    }

    public void setChannelRefundsProPayment(BillsTotals channelRefundsProPayment) {
        this.channelRefundsProPayment = channelRefundsProPayment;
    }

    public BillsTotals getChannelCancellAgnPayment() {
        if(channelCancellAgnPayment==null){
            channelCancellAgnPayment=new BillsTotals();
        }
        return channelCancellAgnPayment;
    }

    public void setChannelCancellAgnPayment(BillsTotals channelCancellAgnPayment) {
        this.channelCancellAgnPayment = channelCancellAgnPayment;
    }

    public BillsTotals getChannelBilledAgnPayment() {
        if(channelBilledAgnPayment==null){
            channelBilledAgnPayment=new BillsTotals();
        }
        return channelBilledAgnPayment;
    }

    public void setChannelBilledAgnPayment(BillsTotals channelBilledAgnPayment) {
        this.channelBilledAgnPayment = channelBilledAgnPayment;
    }

    public BillsTotals getChannelRefundAgnPayment() {
        if(channelRefundAgnPayment==null){
            channelRefundAgnPayment=new BillsTotals();
        }
        return channelRefundAgnPayment;
    }

    public void setChannelRefundAgnPayment(BillsTotals channelRefundAgnPayment) {
        this.channelRefundAgnPayment = channelRefundAgnPayment;
    }
    
    
    

    public List<Bill> getBillsByReferingDoc() {

        Map temMap = new HashMap();
        List<Bill> tmp;

        String sql = "SELECT b FROM BilledBill b WHERE b.retired=false "
                + " and b.billType = :bTp "
                + " and b.createdAt between :fromDate"
                + " and :toDate  order by b.referredBy.person.name";
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bTp", BillType.OpdBill);

        tmp = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        if (tmp == null) {
            tmp = new ArrayList<Bill>();
        }

        return tmp;

    }

    public List<Bill> getBillsByCollecting() {

        Map temMap = new HashMap();
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        String sql;
        List<Bill> tmp;

        if (collectingIns == null) {
            //sql = "SELECT b FROM BilledBill b WHERE b.retired=false  and b.createdAt between :fromDate and :toDate  order by b.collectingCentre.name";
            sql = "SELECT b FROM BilledBill b WHERE b.retired=false and  b.createdAt between :fromDate and :toDate  order by b.collectingCentre.name";
        } else {
            sql = "SELECT b FROM BilledBill b WHERE b.retired=false   and  b.collectingCentre=:col  and b.createdAt between :fromDate and :toDate  order by b.collectingCentre.name";
            temMap.put("col", getCollectingIns());
        }

        tmp = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        if (tmp == null) {
            tmp = new ArrayList<Bill>();
        }

        return tmp;

    }

    public BillsTotals getUserBills() {
        if (billedBills == null) {
            getBilledBills().setBills(userBills(new BilledBill(), BillType.OpdBill, getWebUser()));
            // calTot(getBilledBills());
        }

        return billedBills;
    }

    public BillsTotals getUserCancelledBills() {
        if (cancellededBills == null) {
            getCancellededBills().setBills(userBills(new CancelledBill(), BillType.OpdBill, getWebUser()));
            //   calTot(getCancellededBills());
        }
        return cancellededBills;
    }

    public BillsTotals getUserRefundedBills() {
        if (refundedBills == null) {
            getRefundedBills().setBills(userBills(new RefundBill(), BillType.OpdBill, getWebUser()));
            //  calTot(getRefundedBills());
        }
        return refundedBills;
    }

    public BillsTotals getUserPaymentBills() {
        if (paymentBills == null) {
            getPaymentBills().setBills(userBills(new BilledBill(), BillType.PaymentBill, getWebUser()));
            //  calTot(getPaymentBills());
        }
        return paymentBills;
    }

    public BillsTotals getUserPaymentCancelBills() {
        if (paymentCancelBills == null) {
            getPaymentCancelBills().setBills(userBills(new CancelledBill(), BillType.PaymentBill, getWebUser()));
            //   calTot(getPaymentCancelBills());
        }
        return paymentCancelBills;
    }

    public BillsTotals getInstitutionBilledBills() {
        if (billedBills == null) {
            getBilledBills().setBills(institutionBill(getInstitution(), new BilledBill(), BillType.OpdBill));
            //  calTot(getBilledBills());
        }
        return billedBills;
    }

    public BillsTotals getInstitutionCancelledBills() {
        if (cancellededBills == null) {
            getCancellededBills().setBills(institutionBill(getInstitution(), new CancelledBill(), BillType.OpdBill));
            //   calTot(getCancellededBills());
        }
        return cancellededBills;
    }

    public BillsTotals getInstitutionRefundedBills() {
        if (refundedBills == null) {
            getRefundedBills().setBills(institutionBill(getInstitution(), new RefundBill(), BillType.OpdBill));
            //  calTot(getRefundedBills());
        }
        return refundedBills;
    }

    public BillsTotals getInstitutionPaymentBills() {
        if (paymentBills == null) {
            getPaymentBills().setBills(institutionBill(getInstitution(), new BilledBill(), BillType.PaymentBill));
            // calTot(getPaymentBills());
        }
        return paymentBills;
    }

    public BillsTotals getInstitutionPaymentCancelBills() {
        if (paymentCancelBills == null) {
            getPaymentCancelBills().setBills(institutionBill(getInstitution(), new CancelledBill(), BillType.PaymentBill));
            // calTot(getPaymentCancelBills());
        }
        return paymentCancelBills;
    }

    public BillsTotals getUserPaymentBillsOwn() {

        return paymentBills;
    }

    public BillsTotals getUserInwardPaymentBillsOwn() {

        return inwardPayments;
    }

    public BillsTotals getInstitutionInwardPaymentBillsOwn() {
        if (inwardPayments == null) {
            getInwardPayments().setBills(billsOwn(new BilledBill(), BillType.InwardPaymentBill));
        }
        //  calTot(getInwardPayments());
        return inwardPayments;
    }

    public List<Bill> getBillsByReferingDocOwn() {
        Map temMap = new HashMap();
        List<Bill> tmp;
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", BillType.OpdBill);
        String sql = "SELECT b FROM Bill b "
                + "WHERE b.retired=false "
                + "and b.toInstitution=:ins "
                + "and b.billType =:bTp "
                + "and b.createdAt between :fromDate and :toDate  ";

        sql = sql + "order by b.referredBy.person.name ";
        tmp = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        if (tmp == null) {
            tmp = new ArrayList<>();
        }
        return tmp;
    }

    public List<Bill> getBillsByCollectingOwn() {

        Map temMap = new HashMap();
        List<Bill> tmp;
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bType", BillType.LabBill);
        String sql;

        if (collectingIns == null) {
            sql = "SELECT b FROM BilledBill b WHERE b.retired=false and  b.billType=:bType and  b.institution=:ins and b.createdAt between :fromDate and :toDate  order by b.collectingCentre.name";
        } else {
            sql = "SELECT b FROM BilledBill b WHERE b.retired=false and  b.billType =:bType and b.institution=:ins and b.collectingCentre=:col and b.createdAt between :fromDate and :toDate  order by b.collectingCentre.name";
            temMap.put("col", getCollectingIns());
        }
        tmp = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        if (tmp == null) {
            tmp = new ArrayList<Bill>();
        }

        total = 0.0;

        for (Bill b : tmp) {
            total += b.getNetTotal();
        }

        return tmp;

    }

    public List<Bill> getBillsByLabCreditOwn() {

        Map temMap = new HashMap();
        List<Bill> tmp;
        temMap.put("fromDate", fromDate);
        temMap.put("toDate", toDate);
        //   temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bType", BillType.LabBill);
        temMap.put("col", getCreditCompany());
        String sql;

        sql = "SELECT b FROM BilledBill b WHERE b.retired=false and  b.billType =:bType "
                + "  and b.creditCompany=:col and b.createdAt between :fromDate and :toDate "
                + "order by b.creditCompany.name";

        tmp = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        if (tmp == null) {
            tmp = new ArrayList<>();
        }

        total = 0.0;

        for (Bill b : tmp) {
            total += b.getNetTotal();
        }

        return tmp;

    }

    private double total;

    public BillsTotals getUserRefundedBillsOwn() {

        return refundedBills;
    }

    public BillsTotals getUserRefundedBillsOwnPh() {

        return refundedBillsPh;
    }

    public BillsTotals getUserRefundedBillsPhOther() {
        if (refundedBillsPh2 == null) {
            getRefundedBillsPh2().setBills(userPharmacyBillsOther(new RefundBill(), BillType.PharmacySale, getWebUser()));
        }
        // calTot(getRefundedBills());
        return refundedBillsPh2;
    }

    public BillsTotals getUserCancelledBillsOwn() {

        return cancellededBills;
    }

    public BillsTotals getUserCancelledBillsOwnPh() {

        return cancellededBillsPh;
    }

    public BillsTotals getUserCancelledBillsPhOther() {

        return cancellededBillsPh2;
    }

    private List<Bill> userBillsOwn(Bill billClass, BillType billType, WebUser webUser, Department department) {
        String sql = "SELECT b FROM Bill b WHERE type(b)=:bill "
                + " and b.retired=false"
                + " and b.billType = :btp"
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            temMap.put("dep", department);
        }

        if (webUser != null) {
            sql += " and b.creater=:web ";
            temMap.put("web", webUser);
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);

        temMap.put("ins", getSessionController().getInstitution());

        sql += " order by b.insId ";

        return getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<Bill> userBillsOwn(Bill billClass, List<BillType> billType, WebUser webUser, Department department) {
        String sql = "SELECT b FROM Bill b WHERE type(b)=:bill "
                + " and b.retired=false"
                + " and b.billType in :btp"
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            temMap.put("dep", department);
        }

        if (webUser != null) {
            sql += " and b.creater=:web ";
            temMap.put("web", webUser);
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);

        temMap.put("ins", getSessionController().getInstitution());

        sql += " order by b.insId ";

        return getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<Bill> getBills(Bill billClass, BillType billType, Department dep) {
        String sql;
        Map temMap = new HashMap();

        sql = "SELECT b FROM Bill b WHERE type(b)=:bill"
                + " and b.retired=false and "
                + " b.billType = :btp "
                + " and b.department=:d "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getReferenceInstitution() != null) {
            sql += " and b.referenceInstitution=:ins ";
            temMap.put("ins", getReferenceInstitution());
        }

        sql += " order by b.id  ";

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);
        temMap.put("d", dep);

        return getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<Bill> grnBills(Bill billClass, BillType billType, Department dep, Institution ins) {
        String sql;
        Map temMap = new HashMap();

        sql = "SELECT b FROM Bill b WHERE type(b)=:bill and b.retired=false and "
                + " b.billType = :btp and (b.fromInstitution=:ins or b.toInstitution=:ins ) "
                + " and b.department=:d "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getReferenceInstitution() != null) {
            sql += " and b.referenceInstitution=:inst ";
            temMap.put("inst", getReferenceInstitution());
            //System.out.println("getReferenceInstitution().getName() = " + getReferenceInstitution().getName());
        }
        sql += "order by b.deptId,b.fromInstitution.name ";

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);
        temMap.put("d", dep);
        temMap.put("ins", ins);

        return getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<Bill> userPharmacyBillsOwn(Bill billClass, BillType billType, WebUser webUser) {
        String sql = "SELECT b FROM Bill b WHERE "
                + " type(b)=:bill "
                + " and b.retired=false "
                + " and b.billType = :btp"
                + " and b.creater=:web "
                + " and b.referenceBill.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " order by b.deptId ";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);
        temMap.put("web", webUser);
        temMap.put("ins", getSessionController().getInstitution());

//        checkOtherInstiution
        return getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<Bill> userPharmacyBillsOther(Bill billClass, BillType billType, WebUser webUser) {
        String sql = "SELECT b FROM Bill b WHERE type(b)=:bill and b.retired=false and "
                + "  b.billType = :btp"
                + " and b.creater=:web and b.referenceBill.institution!=:ins "
                + " and b.createdAt between :fromDate"
                + " and :toDate order by b.deptId ";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);
        temMap.put("web", webUser);
        temMap.put("ins", getSessionController().getInstitution());

        Bill b = getBillFacade().findFirstBySQL(sql, temMap, TemporalType.DATE);

        if (b != null && institution == null) {
            //System.err.println("SYS " + b.getInstitution().getName());
            institution = b.getInstitution();
        }

        return getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<Bill> institutionBill(Institution ins, Bill billClass, BillType billType) {
        String sql;
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);

        if (institution == null) {
            sql = "SELECT b FROM Bill b WHERE type(b)=:bill and b.retired=false and b.billType = :btp"
                    + " and  b.createdAt between :fromDate and :toDate ";
        } else {
            sql = "SELECT b FROM Bill b WHERE type(b)=:bill and b.retired=false and b.billType = :btp"
                    + " and b.institution=:ins and b.createdAt between :fromDate and :toDate";

            temMap.put("ins", ins);
        }

        return getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<Bill> userBills(Bill billClass, BillType billType, WebUser webUser) {
        String sql = "SELECT b FROM Bill b WHERE type(b)=:bill and b.retired=false and b.billType = :btp"
                + " and b.creater=:web and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bill", billClass.getClass());
        temMap.put("btp", billType);
        temMap.put("web", webUser);

        return getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    List<BillItem> billItems;

    public void createGRNBillItemForAsset() {
        billItems = new ArrayList<>();
        billItems=createStoreGRNBillItem(DepartmentType.Inventry);
        System.out.println("billItems Size = " + billItems.size());
    }
    
    public void createGRNBillItemForStore() {
        billItems = new ArrayList<>();
        billItems=createStoreGRNBillItem(DepartmentType.Store);
        System.out.println("billItems Size = " + billItems.size());
    }
    
    public List<BillItem> createStoreGRNBillItem(DepartmentType dt) {
        String sql;
        Map m = new HashMap();
        List<BillItem> bs=new ArrayList<>();

        sql = " SELECT bi FROM BillItem bi WHERE "
                + " type(bi.bill)=:bill "
                + " and bi.bill.retired=false "
                + " and bi.bill.billType = :btp "
                + " and bi.item.departmentType=:dt "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + " order by bi.bill.createdAt  ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("bill", BilledBill.class);
        m.put("btp", BillType.StoreGrnBill);
        m.put("dt", dt);

        bs = getBillItemFac().findBySQL(sql, m, TemporalType.TIMESTAMP);
        //System.out.println("billItems = " + billItems);
        System.out.println("billItems Size = " + bs.size());
        
        return bs;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public BillsTotals getUserBillsOwn() {

        return billedBills;
    }

    public BillsTotals getUserBillsOwnPh() {

        return billedBillsPh;
    }
        public BillsTotals getUserBillsOwnCh() {

        return billedBillsCh;
    }


    public BillsTotals getUserBillsPhOther() {
        return billedBillsPh2;
    }

    public BillsTotals getInstitutionPaymentBillsOwn() {
        if (paymentBills == null) {
            getPaymentBills().setBills(billsOwn(new BilledBill(), BillType.PaymentBill));
            //   calTot(getPaymentBills());
        }
        return paymentBills;
    }

    public BillsTotals getInstitutionRefundedBillsOwn() {
        if (refundedBills == null) {
            getRefundedBills().setBills(billsOwn(new RefundBill(), BillType.OpdBill));
        }
        //  calTot(getRefundedBills());
        return refundedBills;
    }

    public BillsTotals getInstitutionRefundedBillsOwnPh() {
        if (refundedBillsPh == null) {
            getRefundedBillsPh().setBills(billsOwn(new RefundBill(), BillType.PharmacySale));
        }
        //  calTot(getRefundedBills());
        return refundedBillsPh;
    }

    public List<BillItem> getReferralBillItems() {
        return referralBillItems;
    }

    public void setReferralBillItems(List<BillItem> referralBillItems) {
        this.referralBillItems = referralBillItems;
    }

    public BillsTotals getInstitutionCancelledBillsOwn() {
        if (cancellededBills == null) {
            getCancellededBills().setBills(billsOwn(new CancelledBill(), BillType.OpdBill));
        }
        ///   calTot(getCancellededBills());
        return cancellededBills;
    }

    public BillsTotals getInstitutionCancelledBillsOwnPh() {
        if (cancellededBillsPh == null) {
            getCancellededBillsPh().setBills(billsOwn(new CancelledBill(), BillType.PharmacySale));
        }
        ///   calTot(getCancellededBills());
        return cancellededBillsPh;
    }

    public BillsTotals getUserAgentRecieveBills() {

        return agentRecieves;
    }

    public BillsTotals getUserCashRecieveBills() {

        return cashRecieves;
    }

    public BillsTotals getUserAgentRecieveBillCancel() {

        return agentCancelBill;
    }

    public BillsTotals getUserCashRecieveBillCancel() {

        return cashRecieveCancel;
    }

    public BillsTotals getUserPettyPaymentBills() {

        return pettyPayments;
    }

    public BillsTotals getUserPettyPaymentCancelBills() {

        return pettyPaymentsCancel;
    }

    public BillsTotals getUserPaymentCancelBillsOwn() {

        return paymentCancelBills;
    }

    public BillsTotals getUserInwardPaymentCancelBillsOwn() {
        if (inwardPaymentCancel == null) {
            inwardPaymentCancel = new BillsTotals();
        }
        return inwardPaymentCancel;
    }

    public BillsTotals getInstitutionInwardPaymentCancelBillsOwn() {
        if (inwardPaymentCancel == null) {
            List<Bill> tmp = billsOwn(new CancelledBill(), BillType.InwardPaymentBill);
            getInwardPaymentCancel().setBills(tmp);
        }
        //    calTot(getInwardPaymentCancel());
        return inwardPaymentCancel;
    }

    public BillsTotals getInstitutionPaymentCancelBillsOwn() {
        if (paymentCancelBills == null) {
            List<Bill> tmp = billsOwn(new CancelledBill(), BillType.PaymentBill);

            getPaymentCancelBills().setBills(tmp);
        }
        //   calTot(getPaymentCancelBills());
        return paymentCancelBills;
    }

    public BillsTotals getInstitutionPettyPaymentBillsOwn() {
        if (pettyPayments == null) {
            List<Bill> tmp = billsOwn(new BilledBill(), BillType.PettyCash);

            getPettyPayments().setBills(tmp);
            //   calTot(getPettyPayments());
        }
        return pettyPayments;
    }

//    public List<Bill> getInstitutionPettyPaymentBills() {
//        if (pettyPayments == null) {
//            String sql;
//            sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.billType = :bTp and  b.createdAt between :fromDate and :toDate order by b.id";
//
//            Map temMap = new HashMap();
//            temMap.put("fromDate", getFromDate());
//            temMap.put("toDate", getToDate());
//            temMap.put("bTp", BillType.PettyCash);
//            pettyPayments = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
//
//            if (pettyPayments == null) {
//                pettyPayments = new ArrayList<Bill>();
//            }
//
//        }
//
//        calPettyTotal();
//        return pettyPayments;
//    }
    public BillsTotals getInstitutionPettyCancellBillsOwn() {
        if (pettyPaymentsCancel == null) {
            List<Bill> tmp = billsOwn(new CancelledBill(), BillType.PettyCash);
            getPettyPaymentsCancel().setBills(tmp);
//        calTot(getPettyPaymentsCancel());
        }
        return pettyPaymentsCancel;
    }

//    public List<Bill> getInstitutionPettyCancellBills() {
//        if (pettyPaymentsCancel == null) {
//            String sql;
//
//            sql = "SELECT b FROM CancelledBill b WHERE b.retired=false and b.billType = :bTp and b.createdAt between :fromDate and :toDate order by b.id";
//
//            Map temMap = new HashMap();
//            temMap.put("fromDate", getFromDate());
//            temMap.put("toDate", getToDate());
//            temMap.put("bTp", BillType.PettyCash);
//            pettyPaymentsCancel = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
//
//            if (pettyPaymentsCancel == null) {
//                pettyPaymentsCancel = new ArrayList<Bill>();
//            }
//
//        }
//        calPettyCancelTotal();
//        return pettyPaymentsCancel;
//    }
    public BillsTotals getInstitutionCashRecieveBillsOwn() {
        if (cashRecieves == null) {
            getCashRecieves().setBills(billsOwn(new BilledBill(), BillType.CashRecieveBill));
            //      calTot(getCashRecieves());
        }
        return cashRecieves;
    }

//    public List<Bill> getInstitutionCashRecieveBills() {
//        if (cashRecieves == null) {
//            String sql;
//
//            sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.billType = :bTp and  b.createdAt between :fromDate and :toDate order by b.id";
//
//            Map temMap = new HashMap();
//            temMap.put("fromDate", getFromDate());
//            temMap.put("toDate", getToDate());
//            temMap.put("bTp", BillType.CashRecieveBill);
//            cashRecieves = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
//
//            if (cashRecieves == null) {
//                cashRecieves = new ArrayList<Bill>();
//            }
//
//        }
//        calCashRecieveTot();
//        return cashRecieves;
//    }
    public BillsTotals getInstitutionAgentBillsOwn() {
        if (agentRecieves == null) {
            getAgentRecieves().setBills(billsOwn(new BilledBill(), BillType.AgentPaymentReceiveBill));
        }
        //    calTot(getAgentRecieves());
        return agentRecieves;
    }

//    public List<Bill> getInstitutionAgentBills() {
//        if (agentRecieves == null) {
//            String sql;
//
//            sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.billType = :bTp and  b.createdAt between :fromDate and :toDate order by b.id";
//
//            Map temMap = new HashMap();
//            temMap.put("fromDate", getFromDate());
//            temMap.put("toDate", getToDate());
//            temMap.put("bTp", BillType.AgentPaymentReceiveBill);
//            agentRecieves = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
//
//            if (agentRecieves == null) {
//                agentRecieves = new ArrayList<Bill>();
//            }
//
//        }
//        calAgentTotal();
//        return agentRecieves;
//    }
    public BillsTotals getInstitutionCashRecieveCancellBillsOwn() {
        if (cashRecieveCancel == null) {
            getCashRecieveCancel().setBills(billsOwn(new CancelledBill(), BillType.CashRecieveBill));
            //    calTot(getCashRecieveCancel());
        }
        return cashRecieveCancel;
    }

//    public List<Bill> getInstitutionCashRecieveCancellBills() {
//        if (cashRecieveCancel == null) {
//            String sql;
//
//            sql = "SELECT b FROM CancelledBill b WHERE b.retired=false and b.billType = :bTp and b.createdAt between :fromDate and :toDate order by b.id";
//
//            Map temMap = new HashMap();
//            temMap.put("fromDate", getFromDate());
//            temMap.put("toDate", getToDate());
//            temMap.put("bTp", BillType.CashRecieveBill);
//            cashRecieveCancel = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
//
//            if (cashRecieveCancel == null) {
//                cashRecieveCancel = new ArrayList<Bill>();
//            }
//
//        }
//        calCashRecieveCancelTot();
//        return cashRecieveCancel;
//    }
    public BillsTotals getInstitutionAgentCancellBillsOwn() {
        if (agentCancelBill == null) {
            getAgentCancelBill().setBills(billsOwn(new CancelledBill(), BillType.AgentPaymentReceiveBill));
            //   calTot(getAgentCancelBill());
        }
        return agentCancelBill;
    }

//    public List<Bill> getInstitutionAgentCancellBills() {
//        if (agentCancelBill == null) {
//            String sql;
//
//            sql = "SELECT b FROM CancelledBill b WHERE b.retired=false and b.billType = :bTp  and b.createdAt between :fromDate and :toDate order by b.id";
//
//            Map temMap = new HashMap();
//            temMap.put("fromDate", getFromDate());
//            temMap.put("toDate", getToDate());
//            temMap.put("bTp", BillType.AgentPaymentReceiveBill);
//            agentCancelBill = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
//
//            if (agentCancelBill == null) {
//                agentCancelBill = new ArrayList<Bill>();
//            }
//
//        }
//        calAgentCancelTot();
//        return agentCancelBill;
//    }
    private List<Bill> billsOwn(Bill billClass, BillType billType) {
        String sql = "SELECT b FROM Bill b WHERE type(b)=:bill and b.retired=false and b.billType=:btp and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bill", billClass.getClass());

        return getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calValue(Bill billClass, BillType billType, PaymentMethod paymentMethod) {
        String sql = "SELECT sum(b.netTotal) FROM Bill b WHERE"
                + " type(b)=:bill and b.retired=false and "
                + " b.billType=:btp "
                + " and (b.paymentMethod=:pm or b.paymentMethod=:pm)"
                + "  and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        temMap.put("pm", paymentMethod);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bill", billClass.getClass());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calValue(Bill billClass, BillType billType, PaymentMethod paymentMethod, WebUser wUser, Department department) {
        String sql = "SELECT sum(b.netTotal) FROM Bill b WHERE"
                + " type(b)=:bill and b.retired=false  "
                + " and b.billType=:btp "
                + " and (b.paymentMethod=:pm )"
                + " and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            temMap.put("dep", department);
        }

        if (webUser != null) {
            sql += " and b.creater=:w";
            temMap.put("w", wUser);
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        temMap.put("pm", paymentMethod);

        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bill", billClass.getClass());

        sql += " order by b.insId ";

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }
    
    private double calValue(Bill billClass, List<BillType> billTypes, PaymentMethod paymentMethod, WebUser wUser, Department department) {
        String sql = "SELECT sum(b.netTotal) FROM Bill b WHERE"
                + " type(b)=:bill and b.retired=false  "
                + " and b.billType in :btps "
                + " and (b.paymentMethod=:pm )"
                + " and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();

        if (department != null) {
            sql += " and b.department=:dep ";
            temMap.put("dep", department);
        }

        if (webUser != null) {
            sql += " and b.creater=:w";
            temMap.put("w", wUser);
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btps", billTypes);
        temMap.put("pm", paymentMethod);

        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bill", billClass.getClass());

        sql += " order by b.insId ";

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calValueCash(Bill billClass, BillType billType, WebUser wUser) {
        String sql = "SELECT sum(b.cashTransaction.cashValue) FROM Bill b "
                + " WHERE type(b)=:bill "
                + " and b.retired=false "
                + " and b.billType=:btp "
                + " and b.creater=:w "
                + "  and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        temMap.put("w", wUser);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bill", billClass.getClass());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calValueCreditCard(Bill billClass, BillType billType, WebUser wUser) {
        String sql = "SELECT sum(b.cashTransaction.creditCardValue) FROM Bill b"
                + "  WHERE type(b)=:bill "
                + " and b.retired=false "
                + " and b.billType=:btp"
                + " and b.creater=:w "
                + "  and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        temMap.put("w", wUser);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bill", billClass.getClass());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calValueCheque(Bill billClass, BillType billType, WebUser wUser) {
        String sql = "SELECT sum(b.cashTransaction.chequeValue) FROM Bill b "
                + " WHERE type(b)=:bill "
                + " and b.retired=false "
                + " and b.billType=:btp "
                + " and b.creater=:w "
                + "  and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        temMap.put("w", wUser);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bill", billClass.getClass());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calValueSlip(Bill billClass, BillType billType, WebUser wUser) {
        String sql = "SELECT sum(b.cashTransaction.slipValue) FROM Bill b "
                + " WHERE type(b)=:bill "
                + " and b.retired=false "
                + " and  b.billType=:btp "
                + " and b.creater=:w "
                + "  and b.institution=:ins"
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        temMap.put("w", wUser);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bill", billClass.getClass());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calValue(Bill billClass, BillType billType, PaymentMethod paymentMethod, Department dep) {
        String sql;
        Map temMap = new HashMap();

        sql = "SELECT sum(b.grnNetTotal) FROM Bill b WHERE"
                + " type(b)=:bill and b.retired=false and "
                + " b.billType=:btp and b.department=:d "
                + " and b.paymentMethod=:pm "
                + "  and b.createdAt between :fromDate and :toDate";

        if (getReferenceInstitution() != null) {
            sql += " and b.referenceInstitution=:ins ";
            temMap.put("ins", getReferenceInstitution());
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        temMap.put("pm", paymentMethod);
        temMap.put("d", dep);
        temMap.put("bill", billClass.getClass());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calValueNetTotal(Bill billClass, BillType billType, PaymentMethod paymentMethod, Department dep) {
        String sql;
        Map temMap = new HashMap();

        sql = "SELECT sum(b.netTotal) FROM Bill b WHERE"
                + " type(b)=:bill and b.retired=false and "
                + " b.billType=:btp and b.department=:d "
                + " and b.paymentMethod=:pm "
                + "  and b.createdAt between :fromDate and :toDate";

        if (getReferenceInstitution() != null) {
            sql += " and b.referenceInstitution=:ins ";
            temMap.put("ins", getReferenceInstitution());
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        temMap.put("pm", paymentMethod);
        temMap.put("d", dep);
        temMap.put("bill", billClass.getClass());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calValueNetTotal(Bill billClass, BillType billType, Department dep) {
        String sql;
        Map temMap = new HashMap();

        sql = "SELECT sum(b.netTotal) FROM Bill b WHERE"
                + " type(b)=:bill and b.retired=false and "
                + " b.billType=:btp and b.department=:d "
                //+ " and b.paymentMethod=:pm "
                + "  and b.createdAt between :fromDate and :toDate";

        if (getReferenceInstitution() != null) {
            sql += " and b.referenceInstitution=:ins ";
            temMap.put("ins", getReferenceInstitution());
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        //temMap.put("pm", paymentMethod);
        temMap.put("d", dep);
        temMap.put("bill", billClass.getClass());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calValueExpenseTotal(Bill billClass, BillType billType, Department dep) {
        String sql;
        Map temMap = new HashMap();

        sql = "SELECT sum(b.expenseTotal) FROM Bill b WHERE"
                + " type(b)=:bill and b.retired=false and "
                + " b.billType=:btp and b.department=:d "
                // + " and b.paymentMethod=:pm "
                + "  and b.createdAt between :fromDate and :toDate";

        if (getReferenceInstitution() != null) {
            sql += " and b.referenceInstitution=:ins ";
            temMap.put("ins", getReferenceInstitution());
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        //temMap.put("pm", paymentMethod);
        temMap.put("d", dep);
        temMap.put("bill", billClass.getClass());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calValue(Bill billClass, BillType billType, Department dep) {
        String sql = "SELECT sum(b.netTotal) FROM Bill b WHERE"
                + " type(b)=:bill and b.retired=false and "
                + " b.billType=:btp and b.department=:d "
                + "  and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        temMap.put("d", dep);
        temMap.put("bill", billClass.getClass());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calValue(Bill billClass, BillType billType, PaymentMethod paymentMethod, Department dep, Institution ins) {
        String sql;
        Map temMap = new HashMap();

        sql = "SELECT sum(b.netTotal) FROM Bill b WHERE"
                + " type(b)=:bill and b.retired=false and "
                + " b.billType=:btp and b.department=:d "
                + " and b.paymentMethod=:pm and "
                + " (b.fromInstitution=:ins or b.toInstitution=:ins) "
                + "  and b.createdAt between :fromDate and :toDate ";

        if (getReferenceInstitution() != null) {
            sql += " and b.referenceInstitution=:inst ";
            temMap.put("inst", getReferenceInstitution());
            //System.out.println("getReferenceInstitution().getName() = " + getReferenceInstitution().getName());
        }

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        temMap.put("pm", paymentMethod);
        temMap.put("d", dep);
        temMap.put("ins", ins);
        temMap.put("bill", billClass.getClass());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double calValueOther(Bill billClass, BillType billType, PaymentMethod paymentMethod, WebUser wUser) {
        String sql = "SELECT sum(b.netTotal) FROM Bill b WHERE"
                + " type(b)=:bill and b.retired=false and "
                + " b.billType=:btp and b.creater=:w "
                + " and (b.paymentMethod=:pm or b.paymentMethod=:pm)"
                + "  and b.institution!=:ins"
                + " and b.createdAt between :fromDate and :toDate";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", billType);
        temMap.put("pm", paymentMethod);
        temMap.put("w", wUser);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bill", billClass.getClass());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public BillsTotals getInstitutionBilledBillsOwn() {
        if (billedBills == null) {
            List<Bill> tmp = billsOwn(new BilledBill(), BillType.OpdBill);

            getBilledBills().setBills(tmp);
        }
        //    calTot(getBilledBills());
        return billedBills;
    }

    public BillsTotals getInstitutionBilledBillsOwnPh() {
        if (billedBillsPh == null) {
            List<Bill> tmp = billsOwn(new BilledBill(), BillType.PharmacySale);

            getBilledBillsPh().setBills(tmp);
        }
        //    calTot(getBilledBills());
        return billedBillsPh;
    }

    public double getFinalCreditTotal(List<BillsTotals> list) {

        double tmp = 0.0;
        for (BillsTotals bt : list) {
            if (bt != null) {
                System.err.println("CRDIT " + bt.getCredit());
                System.err.println("CASH " + bt.getCash());
                //   System.err.println("Size " + bt.getBills().size());
                tmp += bt.getCredit();
            }
        }

        return tmp;
    }

    public double getFinalCreditCardTotal(List<BillsTotals> list) {

        double tmp = 0.0;
        for (BillsTotals bt : list) {
            if (bt != null) {
                tmp += bt.getCard();
            }
        }

        return tmp;
    }

    public void createTableByBillType() {
        billedBills = null;
        cancellededBills = null;
        refundedBills = null;
        List<Bill> list = null;
        getBilledBills().setBills(billsOwn(new BilledBill(), billType));
        getBilledBills().setCard(calValue(new BilledBill(), billType, PaymentMethod.Card));
        getBilledBills().setCash(calValue(new BilledBill(), billType, PaymentMethod.Cash));
        getBilledBills().setCheque(calValue(new BilledBill(), billType, PaymentMethod.Cheque));
        getBilledBills().setCredit(calValue(new BilledBill(), billType, PaymentMethod.Credit));
        getBilledBills().setSlip(calValue(new BilledBill(), billType, PaymentMethod.Slip));
        ////////////
        getCancellededBills().setBills(billsOwn(new CancelledBill(), billType));
        getCancellededBills().setCard(calValue(new CancelledBill(), billType, PaymentMethod.Card));
        getCancellededBills().setCash(calValue(new CancelledBill(), billType, PaymentMethod.Cash));
        getCancellededBills().setCheque(calValue(new CancelledBill(), billType, PaymentMethod.Cheque));
        getCancellededBills().setCredit(calValue(new CancelledBill(), billType, PaymentMethod.Credit));
        getCancellededBills().setSlip(calValue(new CancelledBill(), billType, PaymentMethod.Slip));
        /////////////
        getRefundedBills().setBills(billsOwn(new RefundBill(), billType));
        getRefundedBills().setCard(calValue(new RefundBill(), billType, PaymentMethod.Card));
        getRefundedBills().setCash(calValue(new RefundBill(), billType, PaymentMethod.Cash));
        getRefundedBills().setCheque(calValue(new RefundBill(), billType, PaymentMethod.Cheque));
        getRefundedBills().setCredit(calValue(new RefundBill(), billType, PaymentMethod.Credit));
        getRefundedBills().setSlip(calValue(new RefundBill(), billType, PaymentMethod.Slip));

    }

    public void createTableByBillTypeWebUser() {
        billedBills = null;
        cancellededBills = null;
        refundedBills = null;
        List<Bill> list = null;
        getBilledBills().setBills(userBillsOwn(new BilledBill(), billType, webUser, department));
        getBilledBills().setCard(calValue(new BilledBill(), billType, PaymentMethod.Card, webUser, department));
        getBilledBills().setCash(calValue(new BilledBill(), billType, PaymentMethod.Cash, webUser, department));
        getBilledBills().setCheque(calValue(new BilledBill(), billType, PaymentMethod.Cheque, webUser, department));
        getBilledBills().setCredit(calValue(new BilledBill(), billType, PaymentMethod.Credit, webUser, department));
        getBilledBills().setSlip(calValue(new BilledBill(), billType, PaymentMethod.Slip, webUser, department));
        ////////////
        getCancellededBills().setBills(userBillsOwn(new CancelledBill(), billType, webUser, department));
        getCancellededBills().setCard(calValue(new CancelledBill(), billType, PaymentMethod.Card, webUser, department));
        getCancellededBills().setCash(calValue(new CancelledBill(), billType, PaymentMethod.Cash, webUser, department));
        getCancellededBills().setCheque(calValue(new CancelledBill(), billType, PaymentMethod.Cheque, webUser, department));
        getCancellededBills().setCredit(calValue(new CancelledBill(), billType, PaymentMethod.Credit, webUser, department));
        getCancellededBills().setSlip(calValue(new CancelledBill(), billType, PaymentMethod.Slip, webUser, department));
        /////////////
        getRefundedBills().setBills(userBillsOwn(new RefundBill(), billType, webUser, department));
        getRefundedBills().setCard(calValue(new RefundBill(), billType, PaymentMethod.Card, webUser, department));
        getRefundedBills().setCash(calValue(new RefundBill(), billType, PaymentMethod.Cash, webUser, department));
        getRefundedBills().setCheque(calValue(new RefundBill(), billType, PaymentMethod.Cheque, webUser, department));
        getRefundedBills().setCredit(calValue(new RefundBill(), billType, PaymentMethod.Credit, webUser, department));
        getRefundedBills().setSlip(calValue(new RefundBill(), billType, PaymentMethod.Slip, webUser, department));

    }

    public void fetchPharmacySummeryAll() {
        pharmacyCashBilledBills = getPharmacyBills(PaymentMethod.Cash, BillType.PharmacySale, new BilledBill());
        pharmacyCashCancelBills = getPharmacyBills(PaymentMethod.Cash, BillType.PharmacySale, new CancelledBill());
        pharmacyCashReturnbill = getPharmacyBills(PaymentMethod.Cash, BillType.PharmacySale, new RefundBill());

        pharmacyCreditBilledBills = getPharmacyBills(PaymentMethod.Credit, BillType.PharmacySale, new BilledBill());
        pharmacyCreditCancelBills = getPharmacyBills(PaymentMethod.Credit, BillType.PharmacySale, new CancelledBill());
        pharmacyCreditReturnbill = getPharmacyBills(PaymentMethod.Credit, BillType.PharmacySale, new RefundBill());

        pharmacyBhtIssueBilledBills = getPharmacyBills(BillType.PharmacyBhtPre, new PreBill());
        //pharmacyBhtIssueCancelBills = getPharmacyBills(BillType.PharmacyBhtPre, new CancelledBill());
        //pharmacyBhtIssueReturnbill = getPharmacyBills(BillType.PharmacyBhtPre, new RefundBill());

        pharmacyUnitIssueBilledBills = getPharmacyBills(BillType.PharmacyIssue, new PreBill());
       //pharmacyUnitIssueCancelBills = getPharmacyBills(BillType.PharmacyIssue, new CancelledBill());
        //pharmacyUnitIssueReturnbill = getPharmacyBills(BillType.PharmacyIssue, new RefundBill());

        //totals
        pharmacyCashBilledBillTotals = getPharmacyBillTotal(PaymentMethod.Cash, BillType.PharmacySale, new BilledBill());
        pharmacyCashCancelBillTotals = getPharmacyBillTotal(PaymentMethod.Cash, BillType.PharmacySale, new CancelledBill());
        pharmacyCashReturnbillTotals = getPharmacyBillTotal(PaymentMethod.Cash, BillType.PharmacySale, new RefundBill());

        pharmacyCreditBilledBillTotals = getPharmacyBillTotal(PaymentMethod.Credit, BillType.PharmacySale, new BilledBill());
        pharmacyCreditCancelBillTotals = getPharmacyBillTotal(PaymentMethod.Credit, BillType.PharmacySale, new CancelledBill());
        pharmacyCreditReturnbillTotals = getPharmacyBillTotal(PaymentMethod.Credit, BillType.PharmacySale, new RefundBill());

        pharmacyBhtIssueBilledBillTotals = getPharmacyBillTotal(BillType.PharmacyBhtPre, new PreBill());
        //pharmacyBhtIssueCancelBillTotals = getPharmacyBillTotal(BillType.PharmacyBhtPre, new CancelledBill());
        //pharmacyBhtIssueReturnbillTotals = getPharmacyBillTotal(BillType.PharmacyBhtPre, new RefundBill());

        pharmacyUnitIssueBilledBillTotals = getPharmacyBillTotal(BillType.PharmacyIssue, new PreBill());
        //pharmacyUnitIssueCancelBillTotals = getPharmacyBillTotal(BillType.PharmacyIssue, new CancelledBill());
        //pharmacyUnitIssueReturnbillTotals = getPharmacyBillTotal(BillType.PharmacyIssue, new RefundBill());

    }

    List<Bill> getPharmacyBills(PaymentMethod paymentMethod, BillType billType, Bill bill) {
        Map hm = new HashMap();
        String sql;

        sql = "SELECT b FROM Bill b "
                + " WHERE b.createdAt between :fromDate and :toDate "
                + " and type(b)=:bill "
                + " and b.retired=false "
                + " and b.billType=:btp "
                + " and b.paymentMethod=:pm "
                + " and b.institution=:ins "
                + " and b.department=:dep ";

        hm.put("btp", billType);
        hm.put("bill", bill.getClass());
        hm.put("pm", paymentMethod);
        hm.put("fromDate", getFromDate());
        hm.put("toDate", getToDate());
        hm.put("ins", getIns());
        hm.put("dep", getDepartment());

        return getBillFacade().findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }

    List<Bill> getPharmacyBills(BillType billType, Bill bill) {
        Map hm = new HashMap();
        String sql;

        sql = "SELECT b FROM Bill b "
                + " WHERE b.createdAt between :fromDate and :toDate "
                + " and type(b)=:bill "
                + " and b.retired=false "
                + " and b.billType=:btp "
                + " and b.institution=:ins "
                + " and b.department=:dep ";

        hm.put("btp", billType);
        hm.put("bill", bill.getClass());
        hm.put("fromDate", getFromDate());
        hm.put("toDate", getToDate());
        hm.put("ins", getIns());
        hm.put("dep", getDepartment());

        return getBillFacade().findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }

    public double getPharmacyBillTotal(PaymentMethod paymentMethod, BillType billType, Bill bill) {
        Map hm = new HashMap();
        String sql;

        sql = "SELECT sum(b.netTotal) FROM Bill b "
                + " WHERE b.createdAt between :fromDate and :toDate "
                + " and type(b)=:bill "
                + " and b.retired=false "
                + " and b.billType=:btp "
                + " and b.paymentMethod=:pm "
                + " and b.institution=:ins "
                + " and b.department=:dep ";

        hm.put("btp", billType);
        hm.put("bill", bill.getClass());
        hm.put("pm", paymentMethod);
        hm.put("fromDate", getFromDate());
        hm.put("toDate", getToDate());
        hm.put("ins", getIns());
        hm.put("dep", getDepartment());

        return getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public double getPharmacyBillTotal(BillType billType, Bill bill) {
        Map hm = new HashMap();
        String sql;

        sql = "SELECT sum(b.netTotal) FROM Bill b "
                + " WHERE b.createdAt between :fromDate and :toDate "
                + " and type(b)=:bill "
                + " and b.retired=false "
                + " and b.billType=:btp "
                + " and b.institution=:ins "
                + " and b.department=:dep ";

        hm.put("btp", billType);
        hm.put("bill", bill.getClass());
        hm.put("fromDate", getFromDate());
        hm.put("toDate", getToDate());
        hm.put("ins", getIns());
        hm.put("dep", getDepartment());

        return getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    public boolean checkLabCashier() {
        if (department == null) {
            JsfUtil.addErrorMessage("Please Select a Department");
            return true;
        }
        return false;
    }

    public void createLabCashierSummeryReport() {

        recreteModal();

        if (checkLabCashier()) {
            return;
        }

        //Opd Billed Bills
        getBilledBills().setBills(userBillsOwn(new BilledBill(), BillType.OpdBill, getWebUser(), getDepartment()));
        getBilledBills().setCard(calValue(new BilledBill(), BillType.OpdBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getBilledBills().setCash(calValue(new BilledBill(), BillType.OpdBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getBilledBills().setCheque(calValue(new BilledBill(), BillType.OpdBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getBilledBills().setCredit(calValue(new BilledBill(), BillType.OpdBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getBilledBills().setSlip(calValue(new BilledBill(), BillType.OpdBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Opd Cancelled Bill
        getCancellededBills().setBills(userBillsOwn(new CancelledBill(), BillType.OpdBill, getWebUser(), getDepartment()));
        getCancellededBills().setCard(calValue(new CancelledBill(), BillType.OpdBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getCancellededBills().setCash(calValue(new CancelledBill(), BillType.OpdBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getCancellededBills().setCheque(calValue(new CancelledBill(), BillType.OpdBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getCancellededBills().setCredit(calValue(new CancelledBill(), BillType.OpdBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getCancellededBills().setSlip(calValue(new CancelledBill(), BillType.OpdBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Opd Refunded Bill
        getRefundedBills().setBills(userBillsOwn(new RefundBill(), BillType.OpdBill, getWebUser(), getDepartment()));
        getRefundedBills().setCard(calValue(new RefundBill(), BillType.OpdBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getRefundedBills().setCash(calValue(new RefundBill(), BillType.OpdBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getRefundedBills().setCheque(calValue(new RefundBill(), BillType.OpdBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getRefundedBills().setCredit(calValue(new RefundBill(), BillType.OpdBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getRefundedBills().setSlip(calValue(new RefundBill(), BillType.OpdBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        createSum();

        //Cash IN Billed
        getCashInBills().setBills(userBillsOwn(new BilledBill(), BillType.CashIn, getWebUser(), getDepartment()));
        getCashInBills().setCard(calValueCreditCard(new BilledBill(), BillType.CashIn, getWebUser()));
        getCashInBills().setCash(calValueCash(new BilledBill(), BillType.CashIn, getWebUser()));
        getCashInBills().setCheque(calValueCheque(new BilledBill(), BillType.CashIn, getWebUser()));
        getCashInBills().setSlip(calValueSlip(new BilledBill(), BillType.CashIn, getWebUser()));

        //Cash IN Canceled
        getCashInBillsCancel().setBills(userBillsOwn(new CancelledBill(), BillType.CashIn, getWebUser(), getDepartment()));
        getCashInBillsCancel().setCard(calValueCreditCard(new CancelledBill(), BillType.CashIn, getWebUser()));
        getCashInBillsCancel().setCash(calValueCash(new CancelledBill(), BillType.CashIn, getWebUser()));
        getCashInBillsCancel().setCheque(calValueCheque(new CancelledBill(), BillType.CashIn, getWebUser()));
        getCashInBillsCancel().setSlip(calValueSlip(new CancelledBill(), BillType.CashIn, getWebUser()));

        //Cash Out Billled
        getCashOutBills().setBills(userBillsOwn(new BilledBill(), BillType.CashOut, getWebUser(), getDepartment()));
        getCashOutBills().setCard(calValueCreditCard(new BilledBill(), BillType.CashOut, getWebUser()));
        getCashOutBills().setCash(calValueCash(new BilledBill(), BillType.CashOut, getWebUser()));
        getCashOutBills().setCheque(calValueCheque(new BilledBill(), BillType.CashOut, getWebUser()));
        getCashOutBills().setSlip(calValueSlip(new BilledBill(), BillType.CashOut, getWebUser()));

        //Cash Out Cancelled
        getCashOutBillsCancel().setBills(userBillsOwn(new CancelledBill(), BillType.CashOut, getWebUser(), getDepartment()));
        getCashOutBillsCancel().setCard(calValueCreditCard(new CancelledBill(), BillType.CashOut, getWebUser()));
        getCashOutBillsCancel().setCash(calValueCash(new CancelledBill(), BillType.CashOut, getWebUser()));
        getCashOutBillsCancel().setCheque(calValueCheque(new CancelledBill(), BillType.CashOut, getWebUser()));
        getCashOutBillsCancel().setSlip(calValueSlip(new CancelledBill(), BillType.CashOut, getWebUser()));

        //Cash Adjustement
        getCashAdjustmentBills().setBills(userBillsOwn(new BilledBill(), BillType.DrawerAdjustment, getWebUser(), getDepartment()));
        getCashAdjustmentBills().setCard(calValueCreditCard(new BilledBill(), BillType.DrawerAdjustment, getWebUser()));
        getCashAdjustmentBills().setCash(calValueCash(new BilledBill(), BillType.DrawerAdjustment, getWebUser()));
        getCashAdjustmentBills().setCheque(calValueCheque(new BilledBill(), BillType.DrawerAdjustment, getWebUser()));
        getCashAdjustmentBills().setSlip(calValueSlip(new BilledBill(), BillType.DrawerAdjustment, getWebUser()));

        //////////
        createSumAfterCash();

    }

    public void createCashierTableByUser() {
        recreteModal();
        //Opd Billed Bills
        getBilledBills().setBills(userBillsOwn(new BilledBill(), BillType.OpdBill, getWebUser(), getDepartment()));
        getBilledBills().setCard(calValue(new BilledBill(), BillType.OpdBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getBilledBills().setCash(calValue(new BilledBill(), BillType.OpdBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getBilledBills().setCheque(calValue(new BilledBill(), BillType.OpdBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getBilledBills().setCredit(calValue(new BilledBill(), BillType.OpdBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getBilledBills().setSlip(calValue(new BilledBill(), BillType.OpdBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Opd Cancelled Bill
        getCancellededBills().setBills(userBillsOwn(new CancelledBill(), BillType.OpdBill, getWebUser(), getDepartment()));
        getCancellededBills().setCard(calValue(new CancelledBill(), BillType.OpdBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getCancellededBills().setCash(calValue(new CancelledBill(), BillType.OpdBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getCancellededBills().setCheque(calValue(new CancelledBill(), BillType.OpdBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getCancellededBills().setCredit(calValue(new CancelledBill(), BillType.OpdBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getCancellededBills().setSlip(calValue(new CancelledBill(), BillType.OpdBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Opd Refunded Bill
        getRefundedBills().setBills(userBillsOwn(new RefundBill(), BillType.OpdBill, getWebUser(), getDepartment()));
        getRefundedBills().setCard(calValue(new RefundBill(), BillType.OpdBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getRefundedBills().setCash(calValue(new RefundBill(), BillType.OpdBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getRefundedBills().setCheque(calValue(new RefundBill(), BillType.OpdBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getRefundedBills().setCredit(calValue(new RefundBill(), BillType.OpdBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getRefundedBills().setSlip(calValue(new RefundBill(), BillType.OpdBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Pharmacy Billed
        getBilledBillsPh().setBills(userPharmacyBillsOwn(new BilledBill(), BillType.PharmacySale, getWebUser()));
        getBilledBillsPh().setCard(calValue(new BilledBill(), BillType.PharmacySale, PaymentMethod.Card, getWebUser(), getDepartment()));
        getBilledBillsPh().setCash(calValue(new BilledBill(), BillType.PharmacySale, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getBilledBillsPh().setCheque(calValue(new BilledBill(), BillType.PharmacySale, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getBilledBillsPh().setCredit(calValue(new BilledBill(), BillType.PharmacySale, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getBilledBillsPh().setSlip(calValue(new BilledBill(), BillType.PharmacySale, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Pharmacy Cancelled       
        getCancellededBillsPh().setBills(userPharmacyBillsOwn(new CancelledBill(), BillType.PharmacySale, getWebUser()));
        getCancellededBillsPh().setCard(calValue(new CancelledBill(), BillType.PharmacySale, PaymentMethod.Card, getWebUser(), getDepartment()));
        getCancellededBillsPh().setCash(calValue(new CancelledBill(), BillType.PharmacySale, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getCancellededBillsPh().setCheque(calValue(new CancelledBill(), BillType.PharmacySale, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getCancellededBillsPh().setCredit(calValue(new CancelledBill(), BillType.PharmacySale, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getCancellededBillsPh().setSlip(calValue(new CancelledBill(), BillType.PharmacySale, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Pharmacy Refunded      
        getRefundedBillsPh().setBills(userPharmacyBillsOwn(new RefundBill(), BillType.PharmacySale, getWebUser()));
        getRefundedBillsPh().setCard(calValue(new RefundBill(), BillType.PharmacySale, PaymentMethod.Card, getWebUser(), getDepartment()));
        getRefundedBillsPh().setCash(calValue(new RefundBill(), BillType.PharmacySale, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getRefundedBillsPh().setCheque(calValue(new RefundBill(), BillType.PharmacySale, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getRefundedBillsPh().setCredit(calValue(new RefundBill(), BillType.PharmacySale, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getRefundedBillsPh().setSlip(calValue(new RefundBill(), BillType.PharmacySale, PaymentMethod.Slip, getWebUser(), getDepartment()));
        
        //Pharmacy Whole Billed
        getBilledPhWholeSale().setBills(userPharmacyBillsOwn(new BilledBill(), BillType.PharmacyWholeSale, getWebUser()));
        getBilledPhWholeSale().setCard(calValue(new BilledBill(), BillType.PharmacyWholeSale, PaymentMethod.Card, getWebUser(), getDepartment()));
        getBilledPhWholeSale().setCash(calValue(new BilledBill(), BillType.PharmacyWholeSale, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getBilledPhWholeSale().setCheque(calValue(new BilledBill(), BillType.PharmacyWholeSale, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getBilledPhWholeSale().setCredit(calValue(new BilledBill(), BillType.PharmacyWholeSale, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getBilledPhWholeSale().setSlip(calValue(new BilledBill(), BillType.PharmacyWholeSale, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Pharmacy Whole Cancelled       
        getCancelledPhWholeSale().setBills(userPharmacyBillsOwn(new CancelledBill(), BillType.PharmacyWholeSale, getWebUser()));
        getCancelledPhWholeSale().setCard(calValue(new CancelledBill(), BillType.PharmacyWholeSale, PaymentMethod.Card, getWebUser(), getDepartment()));
        getCancelledPhWholeSale().setCash(calValue(new CancelledBill(), BillType.PharmacyWholeSale, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getCancelledPhWholeSale().setCheque(calValue(new CancelledBill(), BillType.PharmacyWholeSale, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getCancelledPhWholeSale().setCredit(calValue(new CancelledBill(), BillType.PharmacyWholeSale, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getCancelledPhWholeSale().setSlip(calValue(new CancelledBill(), BillType.PharmacyWholeSale, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Pharmacy Whole Refunded      
        getRefundedPhWholeSale().setBills(userPharmacyBillsOwn(new RefundBill(), BillType.PharmacyWholeSale, getWebUser()));
        getRefundedPhWholeSale().setCard(calValue(new RefundBill(), BillType.PharmacyWholeSale, PaymentMethod.Card, getWebUser(), getDepartment()));
        getRefundedPhWholeSale().setCash(calValue(new RefundBill(), BillType.PharmacyWholeSale, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getRefundedPhWholeSale().setCheque(calValue(new RefundBill(), BillType.PharmacyWholeSale, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getRefundedPhWholeSale().setCredit(calValue(new RefundBill(), BillType.PharmacyWholeSale, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getRefundedPhWholeSale().setSlip(calValue(new RefundBill(), BillType.PharmacyWholeSale, PaymentMethod.Slip, getWebUser(), getDepartment()));
        
        //Pharmacy GRN Payment Billed
        getGrnPaymentBill().setBills(userBillsOwn(new BilledBill(), BillType.GrnPaymentPre, getWebUser(),getDepartment()));
        getGrnPaymentBill().setCard(calValue(new BilledBill(), BillType.GrnPaymentPre, PaymentMethod.Card, getWebUser(), getDepartment()));
        getGrnPaymentBill().setCash(calValue(new BilledBill(), BillType.GrnPaymentPre, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getGrnPaymentBill().setCheque(calValue(new BilledBill(), BillType.GrnPaymentPre, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getGrnPaymentBill().setCredit(calValue(new BilledBill(), BillType.GrnPaymentPre, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getGrnPaymentBill().setSlip(calValue(new BilledBill(), BillType.GrnPaymentPre, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Pharmacy GRN Payment Cancelled       
        getGrnPaymentCancell().setBills(userBillsOwn(new CancelledBill(), BillType.GrnPaymentPre, getWebUser(),getDepartment()));
        getGrnPaymentCancell().setCard(calValue(new CancelledBill(), BillType.GrnPaymentPre, PaymentMethod.Card, getWebUser(), getDepartment()));
        getGrnPaymentCancell().setCash(calValue(new CancelledBill(), BillType.GrnPaymentPre, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getGrnPaymentCancell().setCheque(calValue(new CancelledBill(), BillType.GrnPaymentPre, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getGrnPaymentCancell().setCredit(calValue(new CancelledBill(), BillType.GrnPaymentPre, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getGrnPaymentCancell().setSlip(calValue(new CancelledBill(), BillType.GrnPaymentPre, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Pharmacy GRN Payment Refunded      
        getGrnPaymentReturn().setBills(userBillsOwn(new RefundBill(), BillType.GrnPaymentPre, getWebUser(),getDepartment()));
        getGrnPaymentReturn().setCard(calValue(new RefundBill(), BillType.GrnPaymentPre, PaymentMethod.Card, getWebUser(), getDepartment()));
        getGrnPaymentReturn().setCash(calValue(new RefundBill(), BillType.GrnPaymentPre, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getGrnPaymentReturn().setCheque(calValue(new RefundBill(), BillType.GrnPaymentPre, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getGrnPaymentReturn().setCredit(calValue(new RefundBill(), BillType.GrnPaymentPre, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getGrnPaymentReturn().setSlip(calValue(new RefundBill(), BillType.GrnPaymentPre, PaymentMethod.Slip, getWebUser(), getDepartment()));
        
        //Payment Billed Bill
        getPaymentBills().setBills(userBillsOwn(new BilledBill(), BillType.PaymentBill, getWebUser(), getDepartment()));
        getPaymentBills().setCard(calValue(new BilledBill(), BillType.PaymentBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getPaymentBills().setCash(calValue(new BilledBill(), BillType.PaymentBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getPaymentBills().setCheque(calValue(new BilledBill(), BillType.PaymentBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getPaymentBills().setCredit(calValue(new BilledBill(), BillType.PaymentBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getPaymentBills().setSlip(calValue(new BilledBill(), BillType.PaymentBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Payment Cancelled Bill
        getPaymentCancelBills().setBills(userBillsOwn(new CancelledBill(), BillType.PaymentBill, getWebUser(), getDepartment()));
        getPaymentCancelBills().setCard(calValue(new CancelledBill(), BillType.PaymentBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getPaymentCancelBills().setCash(calValue(new CancelledBill(), BillType.PaymentBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getPaymentCancelBills().setCheque(calValue(new CancelledBill(), BillType.PaymentBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getPaymentCancelBills().setCredit(calValue(new CancelledBill(), BillType.PaymentBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getPaymentCancelBills().setSlip(calValue(new CancelledBill(), BillType.PaymentBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Petty Cash Paymennt       
        getPettyPayments().setBills(userBillsOwn(new BilledBill(), BillType.PettyCash, getWebUser(), getDepartment()));
        getPettyPayments().setCard(calValue(new BilledBill(), BillType.PettyCash, PaymentMethod.Card, getWebUser(), getDepartment()));
        getPettyPayments().setCash(calValue(new BilledBill(), BillType.PettyCash, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getPettyPayments().setCheque(calValue(new BilledBill(), BillType.PettyCash, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getPettyPayments().setCredit(calValue(new BilledBill(), BillType.PettyCash, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getPettyPayments().setSlip(calValue(new BilledBill(), BillType.PettyCash, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Petty Cash Paymennt Cancell       
        getPettyPaymentsCancel().setBills(userBillsOwn(new CancelledBill(), BillType.PettyCash, getWebUser(), getDepartment()));
        getPettyPaymentsCancel().setCard(calValue(new CancelledBill(), BillType.PettyCash, PaymentMethod.Card, getWebUser(), getDepartment()));
        getPettyPaymentsCancel().setCash(calValue(new CancelledBill(), BillType.PettyCash, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getPettyPaymentsCancel().setCheque(calValue(new CancelledBill(), BillType.PettyCash, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getPettyPaymentsCancel().setCredit(calValue(new CancelledBill(), BillType.PettyCash, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getPettyPaymentsCancel().setSlip(calValue(new CancelledBill(), BillType.PettyCash, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Cash Receive Bill       
        getCashRecieves().setBills(userBillsOwn(new BilledBill(), BillType.CashRecieveBill, getWebUser(), getDepartment()));
        getCashRecieves().setCard(calValue(new BilledBill(), BillType.CashRecieveBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getCashRecieves().setCash(calValue(new BilledBill(), BillType.CashRecieveBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getCashRecieves().setCheque(calValue(new BilledBill(), BillType.CashRecieveBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getCashRecieves().setCredit(calValue(new BilledBill(), BillType.CashRecieveBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getCashRecieves().setSlip(calValue(new BilledBill(), BillType.CashRecieveBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Cash Recieve Cancel      
        getCashRecieveCancel().setBills(userBillsOwn(new CancelledBill(), BillType.CashRecieveBill, getWebUser(), getDepartment()));
        getCashRecieveCancel().setCard(calValue(new CancelledBill(), BillType.CashRecieveBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getCashRecieveCancel().setCash(calValue(new CancelledBill(), BillType.CashRecieveBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getCashRecieveCancel().setCheque(calValue(new CancelledBill(), BillType.CashRecieveBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getCashRecieveCancel().setCredit(calValue(new CancelledBill(), BillType.CashRecieveBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getCashRecieveCancel().setSlip(calValue(new CancelledBill(), BillType.CashRecieveBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Agent Recieve
        getAgentRecieves().setBills(userBillsOwn(new BilledBill(), BillType.AgentPaymentReceiveBill, getWebUser(), getDepartment()));
        getAgentRecieves().setCard(calValue(new BilledBill(), BillType.AgentPaymentReceiveBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getAgentRecieves().setCash(calValue(new BilledBill(), BillType.AgentPaymentReceiveBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getAgentRecieves().setCheque(calValue(new BilledBill(), BillType.AgentPaymentReceiveBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getAgentRecieves().setCredit(calValue(new BilledBill(), BillType.AgentPaymentReceiveBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getAgentRecieves().setSlip(calValue(new BilledBill(), BillType.AgentPaymentReceiveBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Agent Receive Cancel
        getAgentCancelBill().setBills(userBillsOwn(new CancelledBill(), BillType.AgentPaymentReceiveBill, getWebUser(), getDepartment()));
        getAgentCancelBill().setCard(calValue(new CancelledBill(), BillType.AgentPaymentReceiveBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getAgentCancelBill().setCash(calValue(new CancelledBill(), BillType.AgentPaymentReceiveBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getAgentCancelBill().setCheque(calValue(new CancelledBill(), BillType.AgentPaymentReceiveBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getAgentCancelBill().setCredit(calValue(new CancelledBill(), BillType.AgentPaymentReceiveBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getAgentCancelBill().setSlip(calValue(new CancelledBill(), BillType.AgentPaymentReceiveBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Inward Payment
        getInwardPayments().setBills(userBillsOwn(new BilledBill(), BillType.InwardPaymentBill, getWebUser(), getDepartment()));
        getInwardPayments().setCard(calValue(new BilledBill(), BillType.InwardPaymentBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getInwardPayments().setCash(calValue(new BilledBill(), BillType.InwardPaymentBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getInwardPayments().setCheque(calValue(new BilledBill(), BillType.InwardPaymentBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getInwardPayments().setCredit(calValue(new BilledBill(), BillType.InwardPaymentBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getInwardPayments().setSlip(calValue(new BilledBill(), BillType.InwardPaymentBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Inward Payment Cancel
        getInwardPaymentCancel().setBills(userBillsOwn(new CancelledBill(), BillType.InwardPaymentBill, getWebUser(), getDepartment()));
        getInwardPaymentCancel().setCard(calValue(new CancelledBill(), BillType.InwardPaymentBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getInwardPaymentCancel().setCash(calValue(new CancelledBill(), BillType.InwardPaymentBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getInwardPaymentCancel().setCheque(calValue(new CancelledBill(), BillType.InwardPaymentBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getInwardPaymentCancel().setCredit(calValue(new CancelledBill(), BillType.InwardPaymentBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getInwardPaymentCancel().setSlip(calValue(new CancelledBill(), BillType.InwardPaymentBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //Inward Refund 
        getInwardRefunds().setBills(userBillsOwn(new RefundBill(), BillType.InwardPaymentBill, getWebUser(), getDepartment()));
        getInwardRefunds().setCard(calValue(new RefundBill(), BillType.InwardPaymentBill, PaymentMethod.Card, getWebUser(), getDepartment()));
        getInwardRefunds().setCash(calValue(new RefundBill(), BillType.InwardPaymentBill, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getInwardRefunds().setCheque(calValue(new RefundBill(), BillType.InwardPaymentBill, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getInwardRefunds().setCredit(calValue(new RefundBill(), BillType.InwardPaymentBill, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getInwardRefunds().setSlip(calValue(new RefundBill(), BillType.InwardPaymentBill, PaymentMethod.Slip, getWebUser(), getDepartment()));

        //channell bills
        BillType bty[]={BillType.ChannelCash,BillType.ChannelPaid};
        List<BillType> btys=Arrays.asList(bty);
        getChannelBilled().setBills(userBillsOwn(new BilledBill(), btys, getWebUser(), getDepartment()));
        getChannelBilled().setCard(calValue(new BilledBill(), btys, PaymentMethod.Card, getWebUser(), getDepartment()));
        getChannelBilled().setCash(calValue(new BilledBill(), btys, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getChannelBilled().setCheque(calValue(new BilledBill(), btys, PaymentMethod.Cheque, getWebUser(), getDepartment()));
//        getChannelBilled().setCredit(calValue(new BilledBill(), btys, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getChannelBilled().setSlip(calValue(new BilledBill(), btys, PaymentMethod.Slip, getWebUser(), getDepartment()));

        getChannelCancells().setBills(userBillsOwn(new CancelledBill(), btys, getWebUser(), getDepartment()));
        getChannelCancells().setCard(calValue(new CancelledBill(), btys, PaymentMethod.Card, getWebUser(), getDepartment()));
        getChannelCancells().setCash(calValue(new CancelledBill(), btys, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getChannelCancells().setCheque(calValue(new CancelledBill(), btys, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        //getChannelCancells().setCredit(calValue(new CancelledBill(), BillType.ChannelCash, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getChannelCancells().setSlip(calValue(new CancelledBill(), btys, PaymentMethod.Slip, getWebUser(), getDepartment()));

        getChannelRefunds().setBills(userBillsOwn(new RefundBill(), btys, getWebUser(), getDepartment()));
        getChannelRefunds().setCard(calValue(new RefundBill(), btys, PaymentMethod.Card, getWebUser(), getDepartment()));
        getChannelRefunds().setCash(calValue(new RefundBill(), btys, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getChannelRefunds().setCheque(calValue(new RefundBill(), btys, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        //getChannelRefunds().setCredit(calValue(new RefundBill(), BillType.ChannelCash, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getChannelRefunds().setSlip(calValue(new RefundBill(), btys, PaymentMethod.Slip, getWebUser(), getDepartment()));

        
        //channel professional payment        
        getChannelBilledProPayment().setBills(userBillsOwn(new BilledBill(), BillType.ChannelProPayment, getWebUser(), getDepartment()));
        getChannelBilledProPayment().setCard(calValue(new BilledBill(), BillType.ChannelProPayment, PaymentMethod.Card, getWebUser(), getDepartment()));
        getChannelBilledProPayment().setCash(calValue(new BilledBill(), BillType.ChannelProPayment, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getChannelBilledProPayment().setCheque(calValue(new BilledBill(), BillType.ChannelProPayment, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getChannelBilledProPayment().setCredit(calValue(new BilledBill(), BillType.ChannelProPayment, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getChannelBilledProPayment().setSlip(calValue(new BilledBill(), BillType.ChannelProPayment, PaymentMethod.Slip, getWebUser(), getDepartment()));

        getChannelCancellProPayment().setBills(userBillsOwn(new CancelledBill(), BillType.ChannelProPayment, getWebUser(), getDepartment()));
        getChannelCancellProPayment().setCard(calValue(new CancelledBill(), BillType.ChannelProPayment, PaymentMethod.Card, getWebUser(), getDepartment()));
        getChannelCancellProPayment().setCash(calValue(new CancelledBill(), BillType.ChannelProPayment, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getChannelCancellProPayment().setCheque(calValue(new CancelledBill(), BillType.ChannelProPayment, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getChannelCancellProPayment().setCredit(calValue(new CancelledBill(), BillType.ChannelProPayment, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getChannelCancellProPayment().setSlip(calValue(new CancelledBill(), BillType.ChannelProPayment, PaymentMethod.Slip, getWebUser(), getDepartment()));

        getChannelRefundsProPayment().setBills(userBillsOwn(new RefundBill(), BillType.ChannelProPayment, getWebUser(), getDepartment()));
        getChannelRefundsProPayment().setCard(calValue(new RefundBill(), BillType.ChannelProPayment, PaymentMethod.Card, getWebUser(), getDepartment()));
        getChannelRefundsProPayment().setCash(calValue(new RefundBill(), BillType.ChannelProPayment, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getChannelRefundsProPayment().setCheque(calValue(new RefundBill(), BillType.ChannelProPayment, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getChannelRefundsProPayment().setCredit(calValue(new RefundBill(), BillType.ChannelProPayment, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getChannelRefundsProPayment().setSlip(calValue(new RefundBill(), BillType.ChannelProPayment, PaymentMethod.Slip, getWebUser(), getDepartment()));
        
        //channel agent payment
        getChannelBilledAgnPayment().setBills(userBillsOwn(new BilledBill(), BillType.ChannelAgencyCommission, getWebUser(), getDepartment()));
        getChannelBilledAgnPayment().setCard(calValue(new BilledBill(), BillType.ChannelAgencyCommission, PaymentMethod.Card, getWebUser(), getDepartment()));
        getChannelBilledAgnPayment().setCash(calValue(new BilledBill(), BillType.ChannelAgencyCommission, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getChannelBilledAgnPayment().setCheque(calValue(new BilledBill(), BillType.ChannelAgencyCommission, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getChannelBilledAgnPayment().setCredit(calValue(new BilledBill(), BillType.ChannelAgencyCommission, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getChannelBilledAgnPayment().setSlip(calValue(new BilledBill(), BillType.ChannelAgencyCommission, PaymentMethod.Slip, getWebUser(), getDepartment()));

        getChannelCancellAgnPayment().setBills(userBillsOwn(new CancelledBill(), BillType.ChannelAgencyCommission, getWebUser(), getDepartment()));
        getChannelCancellAgnPayment().setCard(calValue(new CancelledBill(), BillType.ChannelAgencyCommission, PaymentMethod.Card, getWebUser(), getDepartment()));
        getChannelCancellAgnPayment().setCash(calValue(new CancelledBill(), BillType.ChannelAgencyCommission, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getChannelCancellAgnPayment().setCheque(calValue(new CancelledBill(), BillType.ChannelAgencyCommission, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getChannelCancellAgnPayment().setCredit(calValue(new CancelledBill(), BillType.ChannelAgencyCommission, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getChannelCancellAgnPayment().setSlip(calValue(new CancelledBill(), BillType.ChannelAgencyCommission, PaymentMethod.Slip, getWebUser(), getDepartment()));

        getChannelRefundAgnPayment().setBills(userBillsOwn(new RefundBill(), BillType.ChannelAgencyCommission, getWebUser(), getDepartment()));
        getChannelRefundAgnPayment().setCard(calValue(new RefundBill(), BillType.ChannelAgencyCommission, PaymentMethod.Card, getWebUser(), getDepartment()));
        getChannelRefundAgnPayment().setCash(calValue(new RefundBill(), BillType.ChannelAgencyCommission, PaymentMethod.Cash, getWebUser(), getDepartment()));
        getChannelRefundAgnPayment().setCheque(calValue(new RefundBill(), BillType.ChannelAgencyCommission, PaymentMethod.Cheque, getWebUser(), getDepartment()));
        getChannelRefundAgnPayment().setCredit(calValue(new RefundBill(), BillType.ChannelAgencyCommission, PaymentMethod.Credit, getWebUser(), getDepartment()));
        getChannelRefundAgnPayment().setSlip(calValue(new RefundBill(), BillType.ChannelAgencyCommission, PaymentMethod.Slip, getWebUser(), getDepartment()));
        //////////
        createSum();

        //Cash IN Billed
        getCashInBills().setBills(userBillsOwn(new BilledBill(), BillType.CashIn, getWebUser(), getDepartment()));
        getCashInBills().setCard(calValueCreditCard(new BilledBill(), BillType.CashIn, getWebUser()));
        getCashInBills().setCash(calValueCash(new BilledBill(), BillType.CashIn, getWebUser()));
        getCashInBills().setCheque(calValueCheque(new BilledBill(), BillType.CashIn, getWebUser()));
        getCashInBills().setSlip(calValueSlip(new BilledBill(), BillType.CashIn, getWebUser()));

        //Cash IN Canceled
        getCashInBillsCancel().setBills(userBillsOwn(new CancelledBill(), BillType.CashIn, getWebUser(), getDepartment()));
        getCashInBillsCancel().setCard(calValueCreditCard(new CancelledBill(), BillType.CashIn, getWebUser()));
        getCashInBillsCancel().setCash(calValueCash(new CancelledBill(), BillType.CashIn, getWebUser()));
        getCashInBillsCancel().setCheque(calValueCheque(new CancelledBill(), BillType.CashIn, getWebUser()));
        getCashInBillsCancel().setSlip(calValueSlip(new CancelledBill(), BillType.CashIn, getWebUser()));

        //Cash Out Billled
        getCashOutBills().setBills(userBillsOwn(new BilledBill(), BillType.CashOut, getWebUser(), getDepartment()));
        getCashOutBills().setCard(calValueCreditCard(new BilledBill(), BillType.CashOut, getWebUser()));
        getCashOutBills().setCash(calValueCash(new BilledBill(), BillType.CashOut, getWebUser()));
        getCashOutBills().setCheque(calValueCheque(new BilledBill(), BillType.CashOut, getWebUser()));
        getCashOutBills().setSlip(calValueSlip(new BilledBill(), BillType.CashOut, getWebUser()));

        //Cash Out Cancelled
        getCashOutBillsCancel().setBills(userBillsOwn(new CancelledBill(), BillType.CashOut, getWebUser(), getDepartment()));
        getCashOutBillsCancel().setCard(calValueCreditCard(new CancelledBill(), BillType.CashOut, getWebUser()));
        getCashOutBillsCancel().setCash(calValueCash(new CancelledBill(), BillType.CashOut, getWebUser()));
        getCashOutBillsCancel().setCheque(calValueCheque(new CancelledBill(), BillType.CashOut, getWebUser()));
        getCashOutBillsCancel().setSlip(calValueSlip(new CancelledBill(), BillType.CashOut, getWebUser()));

        //Cash Adjustement
        getCashAdjustmentBills().setBills(userBillsOwn(new BilledBill(), BillType.DrawerAdjustment, getWebUser(), getDepartment()));
        getCashAdjustmentBills().setCard(calValueCreditCard(new BilledBill(), BillType.DrawerAdjustment, getWebUser()));
        getCashAdjustmentBills().setCash(calValueCash(new BilledBill(), BillType.DrawerAdjustment, getWebUser()));
        getCashAdjustmentBills().setCheque(calValueCheque(new BilledBill(), BillType.DrawerAdjustment, getWebUser()));
        getCashAdjustmentBills().setSlip(calValueSlip(new BilledBill(), BillType.DrawerAdjustment, getWebUser()));

        //////////
        createSumAfterCash();

    }

    public List<PriceMatrix> createMatrxTabl() {
        String sql;
        sql = "select a from InwardPriceAdjustment a where a.retired=false order by a.department.name,a.category.name,a.fromPrice";
        items = getInwdPriceAdjFacade().findBySQL(sql);
        return items;
    }

    public void createGrnDetailTable() {
        recreteModal();

        grnBilled = new BillsTotals();
        grnCancelled = new BillsTotals();
        grnReturn = new BillsTotals();
        grnReturnCancel = new BillsTotals();

        if (getDepartment() == null) {
            return;
        }

        //GRN Billed Bills
        getGrnBilled().setBills(getBills(new BilledBill(), BillType.PharmacyGrnBill, getDepartment()));
        getGrnBilled().setCash(calValueNetTotal(new BilledBill(), BillType.PharmacyGrnBill, PaymentMethod.Cash, getDepartment()));
        getGrnBilled().setCredit(calValueNetTotal(new BilledBill(), BillType.PharmacyGrnBill, PaymentMethod.Credit, getDepartment()));

        //GRN Cancelled Bill
        getGrnCancelled().setBills(getBills(new CancelledBill(), BillType.PharmacyGrnBill, getDepartment()));
        getGrnCancelled().setCash(calValueNetTotal(new CancelledBill(), BillType.PharmacyGrnBill, PaymentMethod.Cash, getDepartment()));
        getGrnCancelled().setCredit(calValueNetTotal(new CancelledBill(), BillType.PharmacyGrnBill, PaymentMethod.Credit, getDepartment()));

        //GRN Refunded Bill
        getGrnReturn().setBills(getBills(new BilledBill(), BillType.PharmacyGrnReturn, getDepartment()));
        getGrnReturn().setCash(calValueNetTotal(new BilledBill(), BillType.PharmacyGrnReturn, PaymentMethod.Cash, getDepartment()));
        getGrnReturn().setCredit(calValueNetTotal(new BilledBill(), BillType.PharmacyGrnReturn, PaymentMethod.Credit, getDepartment()));

        //GRN Refunded Bill Cancel
        getGrnReturnCancel().setBills(getBills(new CancelledBill(), BillType.PharmacyGrnReturn, getDepartment()));
        getGrnReturnCancel().setCash(calValueNetTotal(new CancelledBill(), BillType.PharmacyGrnReturn, PaymentMethod.Cash, getDepartment()));
        getGrnReturnCancel().setCredit(calValueNetTotal(new CancelledBill(), BillType.PharmacyGrnReturn, PaymentMethod.Credit, getDepartment()));

    }

    public void createGrnDetailTableStore() {
        recreteModal();

        grnBilled = new BillsTotals();
        grnCancelled = new BillsTotals();
        grnReturn = new BillsTotals();
        grnReturnCancel = new BillsTotals();

        if (getDepartment() == null) {
            return;
        }

        //GRN Billed Bills
        getGrnBilled().setBills(getBills(new BilledBill(), BillType.StoreGrnBill, getDepartment()));
        getGrnBilled().setCash(calValue(new BilledBill(), BillType.StoreGrnBill, PaymentMethod.Cash, getDepartment()));
        getGrnBilled().setCredit(calValue(new BilledBill(), BillType.StoreGrnBill, PaymentMethod.Credit, getDepartment()));
        getGrnBilled().setExpense(calValueExpenseTotal(new BilledBill(), BillType.StoreGrnBill, getDepartment()));
        getGrnBilled().setGrnNetTotalWithExpenses(calValueNetTotal(new BilledBill(), BillType.StoreGrnBill, getDepartment()));

        //GRN Cancelled Bill
        getGrnCancelled().setBills(getBills(new CancelledBill(), BillType.StoreGrnBill, getDepartment()));
        getGrnCancelled().setCash(calValue(new CancelledBill(), BillType.StoreGrnBill, PaymentMethod.Cash, getDepartment()));
        getGrnCancelled().setCredit(calValue(new CancelledBill(), BillType.StoreGrnBill, PaymentMethod.Credit, getDepartment()));
        getGrnCancelled().setExpense(calValueExpenseTotal(new BilledBill(), BillType.StoreGrnBill, getDepartment()));
        getGrnCancelled().setGrnNetTotalWithExpenses(calValueNetTotal(new BilledBill(), BillType.StoreGrnBill, getDepartment()));

        //GRN Refunded Bill GRN Total
        getGrnReturn().setBills(getBills(new BilledBill(), BillType.StoreGrnReturn, getDepartment()));
        getGrnReturn().setCash(calValueNetTotal(new BilledBill(), BillType.StoreGrnReturn, PaymentMethod.Cash, getDepartment()));
        getGrnReturn().setCredit(calValueNetTotal(new BilledBill(), BillType.StoreGrnReturn, PaymentMethod.Credit, getDepartment()));

//        //GRN Refunded Bill Net Total
//        getGrnReturn().setBills(getBills(new BilledBill(), BillType.StoreGrnReturn, getDepartment()));
//        getGrnReturn().setCash(calValueNetTotal(new BilledBill(), BillType.StoreGrnReturn, PaymentMethod.Cash, getDepartment()));
//        getGrnReturn().setCredit(calValueNetTotal(new BilledBill(), BillType.StoreGrnReturn, PaymentMethod.Credit, getDepartment()));
        //GRN Refunded Bill Cancel
        getGrnReturnCancel().setBills(getBills(new CancelledBill(), BillType.StoreGrnReturn, getDepartment()));
        getGrnReturnCancel().setCash(calValueNetTotal(new CancelledBill(), BillType.StoreGrnReturn, PaymentMethod.Cash, getDepartment()));
        getGrnReturnCancel().setCredit(calValueNetTotal(new CancelledBill(), BillType.StoreGrnReturn, PaymentMethod.Credit, getDepartment()));

    }

//    public void createGrnDetailTableStore() {
//        recreteModal();
//
//        grnBilled = new BillsTotals();
//        grnCancelled = new BillsTotals();
//        grnReturn = new BillsTotals();
//        grnReturnCancel = new BillsTotals();
//
//        if (getDepartment() == null) {
//            return;
//        }
//
//        //GRN Billed Bills
//        getGrnBilled().setBills(getBills(new BilledBill(), BillType.StoreGrnBill, getDepartment()));
//        getGrnBilled().setCash(calValue(new BilledBill(), BillType.StoreGrnBill, PaymentMethod.Cash, getDepartment()));
//        getGrnBilled().setCredit(calValue(new BilledBill(), BillType.StoreGrnBill, PaymentMethod.Credit, getDepartment()));
//
//        //GRN Cancelled Bill
//        getGrnCancelled().setBills(getBills(new CancelledBill(), BillType.StoreGrnBill, getDepartment()));
//        getGrnCancelled().setCash(calValue(new CancelledBill(), BillType.StoreGrnBill, PaymentMethod.Cash, getDepartment()));
//        getGrnCancelled().setCredit(calValue(new CancelledBill(), BillType.StoreGrnBill, PaymentMethod.Credit, getDepartment()));
//
//        //GRN Refunded Bill
//        getGrnReturn().setBills(getBills(new BilledBill(), BillType.StoreGrnReturn, getDepartment()));
//        getGrnReturn().setCash(calValue(new BilledBill(), BillType.StoreGrnReturn, PaymentMethod.Cash, getDepartment()));
//        getGrnReturn().setCredit(calValue(new BilledBill(), BillType.StoreGrnReturn, PaymentMethod.Credit, getDepartment()));
//
//        //GRN Refunded Bill Cancel
//        getGrnReturnCancel().setBills(getBills(new CancelledBill(), BillType.StoreGrnReturn, getDepartment()));
//        getGrnReturnCancel().setCash(calValue(new CancelledBill(), BillType.StoreGrnReturn, PaymentMethod.Cash, getDepartment()));
//        getGrnReturnCancel().setCredit(calValue(new CancelledBill(), BillType.StoreGrnReturn, PaymentMethod.Credit, getDepartment()));
//
//    }
    public void createBhtIssueTable() {
        recreteModal();

        PharmacyBhtPreBilled = new BillsTotals();
        PharmacyBhtPreCancelled = new BillsTotals();
        PharmacyBhtPreRefunded = new BillsTotals();

        if (getDepartment() == null) {
            return;
        }

        //Pharmacy Bht Billed Bills
        getPharmacyBhtPreBilled().setBills(getBills(new PreBill(), BillType.PharmacyBhtPre, getDepartment()));
        getPharmacyBhtPreBilled().setCash(calValue(new PreBill(), BillType.PharmacyBhtPre, getDepartment()));

        //Pharmacy Bht Cancelled Bill
        getPharmacyBhtPreCancelled().setBills(getBills(new CancelledBill(), BillType.PharmacyBhtPre, getDepartment()));
        getPharmacyBhtPreCancelled().setCash(calValue(new CancelledBill(), BillType.PharmacyBhtPre, getDepartment()));

        //Pharmacy Bht Refunded Bill
        getPharmacyBhtPreRefunded().setBills(getBills(new RefundBill(), BillType.PharmacyBhtPre, getDepartment()));
        getPharmacyBhtPreRefunded().setCash(calValue(new RefundBill(), BillType.PharmacyBhtPre, getDepartment()));

    }

    public void createBhtIssueTableStore() {
        recreteModal();

        StoreBhtPreBilled = new BillsTotals();
        StoreBhtPreCancelled = new BillsTotals();
        StoreBhtPreRefunded = new BillsTotals();

        if (getDepartment() == null) {
            return;
        }

        //Pharmacy Bht Billed Bills
        getPharmacyBhtPreBilled().setBills(getBills(new PreBill(), BillType.StoreBhtPre, getDepartment()));
        getPharmacyBhtPreBilled().setCash(calValue(new PreBill(), BillType.StoreBhtPre, getDepartment()));

        //Pharmacy Bht Cancelled Bill
        getPharmacyBhtPreCancelled().setBills(getBills(new CancelledBill(), BillType.StoreBhtPre, getDepartment()));
        getPharmacyBhtPreCancelled().setCash(calValue(new CancelledBill(), BillType.StoreBhtPre, getDepartment()));

        //Pharmacy Bht Refunded Bill
        getPharmacyBhtPreRefunded().setBills(getBills(new RefundBill(), BillType.StoreBhtPre, getDepartment()));
        getPharmacyBhtPreRefunded().setCash(calValue(new RefundBill(), BillType.StoreBhtPre, getDepartment()));

    }

//    public void createBhtIssueBillItemTable() {
//        recreteModal();
//
//        PharmacyBhtPreBilled = new BillsTotals();
//        PharmacyBhtPreCancelled = new BillsTotals();
//        PharmacyBhtPreRefunded = new BillsTotals();
//        
//        if (getDepartment() == null) {
//            return;
//        }
//
//        //GRN Billed Bills
//        getPharmacyBhtPreBilled().setBills(getBills(new PreBill(), BillType.PharmacyBhtPre, getDepartment()));
//        getPharmacyBhtPreBilled().setCash(calValue(new PreBill(), BillType.PharmacyBhtPre, PaymentMethod.Cash, getDepartment()));
//        getPharmacyBhtPreBilled().setCredit(calValue(new PreBill(), BillType.PharmacyBhtPre, PaymentMethod.Credit, getDepartment()));
//
//        //GRN Cancelled Bill
//        getPharmacyBhtPreCancelled().setBills(getBills(new CancelledBill(), BillType.PharmacyBhtPre, getDepartment()));
//        getPharmacyBhtPreCancelled().setCash(calValue(new CancelledBill(), BillType.PharmacyBhtPre, PaymentMethod.Cash, getDepartment()));
//        getPharmacyBhtPreCancelled().setCredit(calValue(new CancelledBill(), BillType.PharmacyBhtPre, PaymentMethod.Credit, getDepartment()));
//
//        //GRN Refunded Bill
//        getPharmacyBhtPreRefunded().setBills(getBills(new RefundBill(), BillType.PharmacyBhtPre, getDepartment()));
//        getPharmacyBhtPreRefunded().setCash(calValue(new RefundBill(), BillType.PharmacyBhtPre, PaymentMethod.Cash, getDepartment()));
//        getPharmacyBhtPreRefunded().setCredit(calValue(new RefundBill(), BillType.PharmacyBhtPre, PaymentMethod.Credit, getDepartment()));
//
//     
//
//    }
    public void createGrnPaymentTable() {
        recreteModal();

        GrnPaymentBill = new BillsTotals();
        GrnPaymentCancell = new BillsTotals();
        GrnPaymentReturn = new BillsTotals();
        GrnPaymentCancellReturn = new BillsTotals();

        if (getDepartment() == null) {
            return;
        }

        //GRN Payment Billed Bills
        getGrnPaymentBill().setBills(getBills(new BilledBill(), BillType.GrnPaymentBill, getDepartment()));
        getGrnPaymentBill().setCash(calValue(new BilledBill(), BillType.GrnPaymentBill, PaymentMethod.Cash, getDepartment()));
        getGrnPaymentBill().setCredit(calValue(new BilledBill(), BillType.GrnPaymentBill, PaymentMethod.Credit, getDepartment()));

        //GRN Payment Cancelled Bill
        getGrnPaymentCancell().setBills(getBills(new CancelledBill(), BillType.GrnPaymentBill, getDepartment()));
        getGrnPaymentCancell().setCash(calValue(new CancelledBill(), BillType.GrnPaymentBill, PaymentMethod.Cash, getDepartment()));
        getGrnPaymentCancell().setCredit(calValue(new CancelledBill(), BillType.GrnPaymentBill, PaymentMethod.Credit, getDepartment()));

        //GRN Payment Refunded Bill
        getGrnPaymentReturn().setBills(getBills(new BilledBill(), BillType.GrnPaymentReturn, getDepartment()));
        getGrnPaymentReturn().setCash(calValue(new BilledBill(), BillType.GrnPaymentReturn, PaymentMethod.Cash, getDepartment()));
        getGrnPaymentReturn().setCredit(calValue(new BilledBill(), BillType.GrnPaymentReturn, PaymentMethod.Credit, getDepartment()));

        //GRN Payment Refunded Bill Cancel
        getGrnPaymentCancellReturn().setBills(getBills(new CancelledBill(), BillType.GrnPaymentReturn, getDepartment()));
        getGrnPaymentCancellReturn().setCash(calValue(new CancelledBill(), BillType.GrnPaymentReturn, PaymentMethod.Cash, getDepartment()));
        getGrnPaymentCancellReturn().setCredit(calValue(new CancelledBill(), BillType.GrnPaymentReturn, PaymentMethod.Credit, getDepartment()));

    }

    public void createPurchaseDetailTable() {
        recreteModal();

        purchaseBilled = new BillsTotals();
        purchaseCancelled = new BillsTotals();
        purchaseReturn = new BillsTotals();
        purchaseReturnCancel = new BillsTotals();

        if (getDepartment() == null) {
            return;
        }

        //Purchase Billed Bills
        getPurchaseBilled().setBills(getBills(new BilledBill(), BillType.PharmacyPurchaseBill, getDepartment()));
        getPurchaseBilled().setCash(calValueNetTotal(new BilledBill(), BillType.PharmacyPurchaseBill, PaymentMethod.Cash, getDepartment()));
        getPurchaseBilled().setCredit(calValueNetTotal(new BilledBill(), BillType.PharmacyPurchaseBill, PaymentMethod.Credit, getDepartment()));

        //Purchase Cancelled Bill
        getPurchaseCancelled().setBills(getBills(new CancelledBill(), BillType.PharmacyPurchaseBill, getDepartment()));
        getPurchaseCancelled().setCash(calValueNetTotal(new CancelledBill(), BillType.PharmacyPurchaseBill, PaymentMethod.Cash, getDepartment()));
        getPurchaseCancelled().setCredit(calValueNetTotal(new CancelledBill(), BillType.PharmacyPurchaseBill, PaymentMethod.Credit, getDepartment()));

        //Purchase Refunded Bill
        getPurchaseReturn().setBills(getBills(new BilledBill(), BillType.PurchaseReturn, getDepartment()));
        getPurchaseReturn().setCash(calValueNetTotal(new BilledBill(), BillType.PurchaseReturn, PaymentMethod.Cash, getDepartment()));
        getPurchaseReturn().setCredit(calValueNetTotal(new BilledBill(), BillType.PurchaseReturn, PaymentMethod.Credit, getDepartment()));

        //Purchase Refunded Bill Cancel
        getPurchaseReturnCancel().setBills(getBills(new CancelledBill(), BillType.PurchaseReturn, getDepartment()));
        getPurchaseReturnCancel().setCash(calValueNetTotal(new CancelledBill(), BillType.PurchaseReturn, PaymentMethod.Cash, getDepartment()));
        getPurchaseReturnCancel().setCredit(calValueNetTotal(new CancelledBill(), BillType.PurchaseReturn, PaymentMethod.Credit, getDepartment()));

    }

    public void createPurchaseDetailTableStore() {
        recreteModal();

        purchaseBilled = new BillsTotals();
        purchaseCancelled = new BillsTotals();
        purchaseReturn = new BillsTotals();
        purchaseReturnCancel = new BillsTotals();

        if (getDepartment() == null) {
            return;
        }

        //Purchase Billed Bills
        getPurchaseBilled().setBills(getBills(new BilledBill(), BillType.StorePurchase, getDepartment()));
        getPurchaseBilled().setCash(calValue(new BilledBill(), BillType.StorePurchase, PaymentMethod.Cash, getDepartment()));
        getPurchaseBilled().setCredit(calValue(new BilledBill(), BillType.StorePurchase, PaymentMethod.Credit, getDepartment()));

        //Purchase Cancelled Bill
        getPurchaseCancelled().setBills(getBills(new CancelledBill(), BillType.StorePurchase, getDepartment()));
        getPurchaseCancelled().setCash(calValue(new CancelledBill(), BillType.StorePurchase, PaymentMethod.Cash, getDepartment()));
        getPurchaseCancelled().setCredit(calValue(new CancelledBill(), BillType.StorePurchase, PaymentMethod.Credit, getDepartment()));

        //Purchase Refunded Bill
        getPurchaseReturn().setBills(getBills(new BilledBill(), BillType.PurchaseReturn, getDepartment()));
        getPurchaseReturn().setCash(calValue(new BilledBill(), BillType.PurchaseReturn, PaymentMethod.Cash, getDepartment()));
        getPurchaseReturn().setCredit(calValue(new BilledBill(), BillType.PurchaseReturn, PaymentMethod.Credit, getDepartment()));

        //Purchase Refunded Bill Cancel
        getPurchaseReturnCancel().setBills(getBills(new CancelledBill(), BillType.PurchaseReturn, getDepartment()));
        getPurchaseReturnCancel().setCash(calValue(new CancelledBill(), BillType.PurchaseReturn, PaymentMethod.Cash, getDepartment()));
        getPurchaseReturnCancel().setCredit(calValue(new CancelledBill(), BillType.PurchaseReturn, PaymentMethod.Credit, getDepartment()));

    }

//    public void createPurchaseDetailTableStore() {
//        recreteModal();
//
//        purchaseBilled = new BillsTotals();
//        purchaseCancelled = new BillsTotals();
//        purchaseReturn = new BillsTotals();
//        purchaseReturnCancel = new BillsTotals();
//
//        if (getDepartment() == null) {
//            return;
//        }
//
//        //Purchase Billed Bills
//        getPurchaseBilled().setBills(getBills(new BilledBill(), BillType.StorePurchase, getDepartment()));
//        getPurchaseBilled().setCash(calValue(new BilledBill(), BillType.StorePurchase, PaymentMethod.Cash, getDepartment()));
//        getPurchaseBilled().setCredit(calValue(new BilledBill(), BillType.StorePurchase, PaymentMethod.Credit, getDepartment()));
//
//        //Purchase Cancelled Bill
//        getPurchaseCancelled().setBills(getBills(new CancelledBill(), BillType.StorePurchase, getDepartment()));
//        getPurchaseCancelled().setCash(calValue(new CancelledBill(), BillType.StorePurchase, PaymentMethod.Cash, getDepartment()));
//        getPurchaseCancelled().setCredit(calValue(new CancelledBill(), BillType.StorePurchase, PaymentMethod.Credit, getDepartment()));
//
//        //Purchase Refunded Bill
//        getPurchaseReturn().setBills(getBills(new BilledBill(), BillType.PurchaseReturn, getDepartment()));
//        getPurchaseReturn().setCash(calValue(new BilledBill(), BillType.PurchaseReturn, PaymentMethod.Cash, getDepartment()));
//        getPurchaseReturn().setCredit(calValue(new BilledBill(), BillType.PurchaseReturn, PaymentMethod.Credit, getDepartment()));
//
//        //Purchase Refunded Bill Cancel
//        getPurchaseReturnCancel().setBills(getBills(new CancelledBill(), BillType.PurchaseReturn, getDepartment()));
//        getPurchaseReturnCancel().setCash(calValue(new CancelledBill(), BillType.PurchaseReturn, PaymentMethod.Cash, getDepartment()));
//        getPurchaseReturnCancel().setCredit(calValue(new CancelledBill(), BillType.PurchaseReturn, PaymentMethod.Credit, getDepartment()));
//
//    }
    public void createGrnDetailTableByDealor() {
        recreateList();

        grnBilled = new BillsTotals();
        grnCancelled = new BillsTotals();
        grnReturn = new BillsTotals();
        grnReturnCancel = new BillsTotals();

        if (getDepartment() == null || getInstitution() == null) {
            return;
        }

        //GRN Billed Bills
        getGrnBilled().setBills(grnBills(new BilledBill(), BillType.PharmacyGrnBill, getDepartment(), getInstitution()));
        getGrnBilled().setCash(calValue(new BilledBill(), BillType.PharmacyGrnBill, PaymentMethod.Cash, getDepartment(), getInstitution()));
        getGrnBilled().setCredit(calValue(new BilledBill(), BillType.PharmacyGrnBill, PaymentMethod.Credit, getDepartment(), getInstitution()));

        //GRN Cancelled Bill
        getGrnCancelled().setBills(grnBills(new CancelledBill(), BillType.PharmacyGrnBill, getDepartment(), getInstitution()));
        getGrnCancelled().setCash(calValue(new CancelledBill(), BillType.PharmacyGrnBill, PaymentMethod.Cash, getDepartment(), getInstitution()));
        getGrnCancelled().setCredit(calValue(new CancelledBill(), BillType.PharmacyGrnBill, PaymentMethod.Credit, getDepartment(), getInstitution()));

        //GRN Refunded Bill
        getGrnReturn().setBills(grnBills(new BilledBill(), BillType.PharmacyGrnReturn, getDepartment(), getInstitution()));
        getGrnReturn().setCash(calValue(new BilledBill(), BillType.PharmacyGrnReturn, PaymentMethod.Cash, getDepartment(), getInstitution()));
        getGrnReturn().setCredit(calValue(new BilledBill(), BillType.PharmacyGrnReturn, PaymentMethod.Credit, getDepartment(), getInstitution()));

        //GRN Refunded Bill Cancel
        getGrnReturnCancel().setBills(grnBills(new CancelledBill(), BillType.PharmacyGrnReturn, getDepartment(), getInstitution()));
        getGrnReturnCancel().setCash(calValue(new CancelledBill(), BillType.PharmacyGrnReturn, PaymentMethod.Cash, getDepartment(), getInstitution()));
        getGrnReturnCancel().setCredit(calValue(new CancelledBill(), BillType.PharmacyGrnReturn, PaymentMethod.Credit, getDepartment(), getInstitution()));

    }

    public void createGrnDetailTableByDealorStore() {
        recreateList();

        grnBilled = new BillsTotals();
        grnCancelled = new BillsTotals();
        grnReturn = new BillsTotals();
        grnReturnCancel = new BillsTotals();

        if (getDepartment() == null || getInstitution() == null) {
            return;
        }

        //GRN Billed Bills
        getGrnBilled().setBills(grnBills(new BilledBill(), BillType.StoreGrnBill, getDepartment(), getInstitution()));
        getGrnBilled().setCash(calValue(new BilledBill(), BillType.StoreGrnBill, PaymentMethod.Cash, getDepartment(), getInstitution()));
        getGrnBilled().setCredit(calValue(new BilledBill(), BillType.StoreGrnBill, PaymentMethod.Credit, getDepartment(), getInstitution()));

        //GRN Cancelled Bill
        getGrnCancelled().setBills(grnBills(new CancelledBill(), BillType.StoreGrnBill, getDepartment(), getInstitution()));
        getGrnCancelled().setCash(calValue(new CancelledBill(), BillType.StoreGrnBill, PaymentMethod.Cash, getDepartment(), getInstitution()));
        getGrnCancelled().setCredit(calValue(new CancelledBill(), BillType.StoreGrnBill, PaymentMethod.Credit, getDepartment(), getInstitution()));

        //GRN Refunded Bill
        getGrnReturn().setBills(grnBills(new BilledBill(), BillType.StoreGrnReturn, getDepartment(), getInstitution()));
        getGrnReturn().setCash(calValue(new BilledBill(), BillType.StoreGrnReturn, PaymentMethod.Cash, getDepartment(), getInstitution()));
        getGrnReturn().setCredit(calValue(new BilledBill(), BillType.StoreGrnReturn, PaymentMethod.Credit, getDepartment(), getInstitution()));

        //GRN Refunded Bill Cancel
        getGrnReturnCancel().setBills(grnBills(new CancelledBill(), BillType.StoreGrnReturn, getDepartment(), getInstitution()));
        getGrnReturnCancel().setCash(calValue(new CancelledBill(), BillType.StoreGrnReturn, PaymentMethod.Cash, getDepartment(), getInstitution()));
        getGrnReturnCancel().setCredit(calValue(new CancelledBill(), BillType.StoreGrnReturn, PaymentMethod.Credit, getDepartment(), getInstitution()));

    }

//    public void createGrnDetailTableByDealorStore() {
//        recreateList();
//
//        grnBilled = new BillsTotals();
//        grnCancelled = new BillsTotals();
//        grnReturn = new BillsTotals();
//        grnReturnCancel = new BillsTotals();
//
//        if (getDepartment() == null || getInstitution() == null) {
//            return;
//        }
//
//        //GRN Billed Bills
//        getGrnBilled().setBills(grnBills(new BilledBill(), BillType.StoreGrnBill, getDepartment(), getInstitution()));
//        getGrnBilled().setCash(calValue(new BilledBill(), BillType.StoreGrnBill, PaymentMethod.Cash, getDepartment(), getInstitution()));
//        getGrnBilled().setCredit(calValue(new BilledBill(), BillType.StoreGrnBill, PaymentMethod.Credit, getDepartment(), getInstitution()));
//
//        //GRN Cancelled Bill
//        getGrnCancelled().setBills(grnBills(new CancelledBill(), BillType.StoreGrnBill, getDepartment(), getInstitution()));
//        getGrnCancelled().setCash(calValue(new CancelledBill(), BillType.StoreGrnBill, PaymentMethod.Cash, getDepartment(), getInstitution()));
//        getGrnCancelled().setCredit(calValue(new CancelledBill(), BillType.StoreGrnBill, PaymentMethod.Credit, getDepartment(), getInstitution()));
//
//        //GRN Refunded Bill
//        getGrnReturn().setBills(grnBills(new BilledBill(), BillType.StoreGrnReturn, getDepartment(), getInstitution()));
//        getGrnReturn().setCash(calValue(new BilledBill(), BillType.StoreGrnReturn, PaymentMethod.Cash, getDepartment(), getInstitution()));
//        getGrnReturn().setCredit(calValue(new BilledBill(), BillType.StoreGrnReturn, PaymentMethod.Credit, getDepartment(), getInstitution()));
//
//        //GRN Refunded Bill Cancel
//        getGrnReturnCancel().setBills(grnBills(new CancelledBill(), BillType.StoreGrnReturn, getDepartment(), getInstitution()));
//        getGrnReturnCancel().setCash(calValue(new CancelledBill(), BillType.StoreGrnReturn, PaymentMethod.Cash, getDepartment(), getInstitution()));
//        getGrnReturnCancel().setCredit(calValue(new CancelledBill(), BillType.StoreGrnReturn, PaymentMethod.Credit, getDepartment(), getInstitution()));
//
//    }
    private void recreateList() {
        billedBills = null;
        cancellededBills = null;
        refundedBills = null;
        billedBillsPh = null;
        cancellededBillsPh = null;
        refundedBillsPh = null;
        billedBillsPh2 = null;
        cancellededBillsPh2 = null;
        refundedBillsPh2 = null;
        pettyPayments = null;
        pettyPaymentsCancel = null;
        agentCancelBill = null;
        agentRecieves = null;
        cashRecieves = null;
        cashRecieveCancel = null;
        paymentBills = null;
        paymentCancelBills = null;
        inwardPayments = null;
        inwardPaymentCancel = null;
        inwardRefunds = null;
        cashInBills = null;
        cashInBillsCancel = null;
        cashOutBills = null;
        cashOutBillsCancel = null;
        cashAdjustmentBills = null;
        grnBilled = null;
        grnCancelled = null;
        grnReturn = null;
        grnReturnCancel = null;
    }

    public void fillInstitutionReferralBills() {
        //Done By Pasan
//        String jpql;
//        Map m = new HashMap();
//        if (institution != null) {
//            jpql = "select b from Bill b "
//                    + "where b.retired=false "
//                    + "and b.referredByInstitution=:refIns "
//                    + "and b.createdAt between :fd and :td "
//                    + " order by b.id";
//            m.put("refIns", institution);
//            m.put("fd", fromDate);
//            m.put("td", toDate);
//            referralBills = getBillFacade().findBySQL(jpql, m);
//        }else{
//            jpql = "select b from Bill b "
//                    + " where b.retired=false "
//                    + " and b.referredByInstitution is not null "
//                    + "and b.createdAt between :fd and :td "
//                    + " order by b.id";
//            m.put("fd", fromDate);
//            m.put("td", toDate);
//            referralBills = getBillFacade().findBySQL(jpql, m, TemporalType.TIMESTAMP);
//        }

        //Done By Safrin
        String jpql;
        Map m = new HashMap();

        jpql = "select b from Bill b "
                + "where b.retired=false ";

        if (referenceInstitution != null) {
            jpql += "and b.referredByInstitution=:refIns ";
            m.put("refIns", referenceInstitution);
        } else {
            jpql += " and b.referredByInstitution is not null ";
        }

        jpql += "and b.createdAt between :fd and :td "
                + " order by b.id";
        m.put("fd", fromDate);
        m.put("td", toDate);
        referralBills = getBillFacade().findBySQL(jpql, m, TemporalType.TIMESTAMP);

    }

    public void fillInstitutionReferralBillItems() {

        String jpql;
        Map m = new HashMap();

        jpql = "select bi from BillItem bi "
                + "where bi.retired=false "
                + " and bi.bill.referredByInstitution is not null ";

        if (referenceInstitution != null) {
            jpql += "and bi.bill.referredByInstitution=:refIns ";
            m.put("refIns", referenceInstitution);
        }

        if (referenceItem != null) {
            jpql += "and bi.item=:billItem ";
            m.put("billItem", referenceItem);
        }

        jpql += "and bi.bill.createdAt between :fd and :td "
                + " order by bi.id";
        m.put("fd", fromDate);
        m.put("td", toDate);
        referralBillItems = getBillItemFac().findBySQL(jpql, m, TemporalType.TIMESTAMP);

    }

    public void recreteModal() {
        collectingIns = null;
        dataTableData = null;
        institution = null;
        //  department=null;
        recreateList();
    }

    public Institution getCollectingIns() {
        return collectingIns;
    }

    public void setCollectingIns(Institution collectingIns) {
        //recreteModal();
        this.collectingIns = collectingIns;
    }

    public double getFinalChequeTot(List<BillsTotals> list) {

        double tmp = 0.0;
        for (BillsTotals bt : list) {
            if (bt != null) {
                tmp += bt.getCheque();
            }
        }

        return tmp;
    }

    public double getFinalSlipTot(List<BillsTotals> list) {

        double tmp = 0.0;
        for (BillsTotals bt : list) {
            if (bt != null) {
                tmp += bt.getSlip();
            }
        }

        return tmp;
    }

    public double getFinalCashTotal(List<BillsTotals> list) {

        double tmp = 0.0;
        for (BillsTotals bt : list) {
            if (bt != null) {
                tmp += bt.getCash();
            }
        }

        return tmp;
    }

    public BillsTotals getBilledBills() {
        if (billedBills == null) {
            billedBills = new BillsTotals();
            //  billedBills.setBillType(BillType.OpdBill);
        }
        return billedBills;
    }

    public void setBilledBills(BillsTotals billedBills) {
        this.billedBills = billedBills;
    }

    public BillsTotals getCancellededBills() {
        if (cancellededBills == null) {
            cancellededBills = new BillsTotals();
            //   cancellededBills.setBillType(BillType.OpdBill);
        }
        return cancellededBills;
    }

    public void setCancellededBills(BillsTotals cancellededBills) {
        this.cancellededBills = cancellededBills;
    }

    public BillsTotals getRefundedBills() {
        if (refundedBills == null) {
            refundedBills = new BillsTotals();
            //    refundedBills.setBillType(BillType.OpdBill);
        }
        return refundedBills;
    }

    public void setRefundedBills(BillsTotals refundedBills) {
        this.refundedBills = refundedBills;
    }

    public BillsTotals getPaymentBills() {
        if (paymentBills == null) {
            paymentBills = new BillsTotals();
            //    paymentBills.setBillType(BillType.PaymentBill);
        }
        return paymentBills;
    }

    public void setPaymentBills(BillsTotals paymentBills) {
        this.paymentBills = paymentBills;
    }

    public BillsTotals getPaymentCancelBills() {
        if (paymentCancelBills == null) {
            paymentCancelBills = new BillsTotals();
            //    paymentCancelBills.setBillType(BillType.PaymentBill);
        }
        return paymentCancelBills;
    }

    public void setPaymentCancelBills(BillsTotals paymentCancelBills) {
        this.paymentCancelBills = paymentCancelBills;
    }

    public BillsTotals getPettyPayments() {
        if (pettyPayments == null) {
            pettyPayments = new BillsTotals();
            //    pettyPayments.setBillType(BillType.PettyCash);
        }
        return pettyPayments;
    }

    public void setPettyPayments(BillsTotals pettyPayments) {
        this.pettyPayments = pettyPayments;
    }

    public BillsTotals getPettyPaymentsCancel() {
        if (pettyPaymentsCancel == null) {
            pettyPaymentsCancel = new BillsTotals();
            //     pettyPaymentsCancel.setBillType(BillType.PettyCash);
        }
        return pettyPaymentsCancel;
    }

    public void setPettyPaymentsCancel(BillsTotals pettyPaymentsCancel) {
        this.pettyPaymentsCancel = pettyPaymentsCancel;
    }

    public BillsTotals getCashRecieves() {
        if (cashRecieves == null) {
            cashRecieves = new BillsTotals();
            //    cashRecieves.setBillType(BillType.CashRecieveBill);
        }
        return cashRecieves;
    }

    public void setCashRecieves(BillsTotals cashRecieves) {
        this.cashRecieves = cashRecieves;
    }

    public BillsTotals getCashRecieveCancel() {
        if (cashRecieveCancel == null) {
            cashRecieveCancel = new BillsTotals();
            //   cashRecieveCancel.setBillType(BillType.CashRecieveBill);
        }
        return cashRecieveCancel;
    }

    public void setCashRecieveCancel(BillsTotals cashRecieveCancel) {
        this.cashRecieveCancel = cashRecieveCancel;
    }

    public BillsTotals getAgentRecieves() {
        if (agentRecieves == null) {
            agentRecieves = new BillsTotals();
            //  agentRecieves.setBillType(BillType.AgentPaymentReceiveBill);
        }
        return agentRecieves;
    }

    public void setAgentRecieves(BillsTotals agentRecieves) {
        this.agentRecieves = agentRecieves;
    }

    public BillsTotals getAgentCancelBill() {
        if (agentCancelBill == null) {
            agentCancelBill = new BillsTotals();
            //    agentCancelBill.setBillType(BillType.AgentPaymentReceiveBill);
        }
        return agentCancelBill;
    }

    public void setAgentCancelBill(BillsTotals agentCancelBill) {
        this.agentCancelBill = agentCancelBill;
    }

    public BillsTotals getInwardPayments() {
        if (inwardPayments == null) {
            inwardPayments = new BillsTotals();
            //   inwardPayments.setBillType(BillType.InwardPaymentBill);
        }
        return inwardPayments;
    }

    public void setInwardPayments(BillsTotals inwardPayments) {
        this.inwardPayments = inwardPayments;
    }

    public BillsTotals getInwardPaymentCancel() {
        if (inwardPaymentCancel == null) {
            inwardPaymentCancel = new BillsTotals();
            //    inwardPaymentCancel.setBillType(BillType.InwardPaymentBill);
        }
        return inwardPaymentCancel;
    }

    public void setInwardPaymentCancel(BillsTotals inwardPaymentCancel) {
        this.inwardPaymentCancel = inwardPaymentCancel;
    }

    private List<String1Value1> creditSlipSum;
    private List<String1Value1> creditSlipSumAfter;

    public void createSum() {
        List<BillsTotals> list2 = new ArrayList<>();
        list2.add(billedBills);
        list2.add(cancellededBills);
        list2.add(refundedBills);
        list2.add(billedBillsPh);
        list2.add(cancellededBillsPh);
        list2.add(refundedBillsPh);
        list2.add(billedPhWholeSale);
        list2.add(cancelledPhWholeSale);
        list2.add(refundedPhWholeSale);
        list2.add(GrnPaymentBill);
        list2.add(GrnPaymentCancell);
        list2.add(GrnPaymentReturn);
        list2.add(paymentBills);
        list2.add(paymentCancelBills);
        list2.add(pettyPayments);
        list2.add(pettyPaymentsCancel);
        list2.add(agentRecieves);
        list2.add(agentCancelBill);
        list2.add(inwardPayments);
        list2.add(inwardPaymentCancel);
        list2.add(inwardRefunds);
        list2.add(cashRecieves);
        list2.add(cashRecieveCancel);

        double credit = 0.0;
        double slip = 0;
        double creditCard = 0.0;
        double cheque = 0.0;
        double cash = 0.0;
        for (BillsTotals bt : list2) {
            if (bt != null) {
                credit += bt.getCredit();
                slip += bt.getSlip();
                creditCard += bt.getCard();
                cheque += bt.getCheque();
                cash += bt.getCash();
            }
        }

        creditSlipSum = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Total");
        tmp1.setValue(credit);
        creditSlipSum.add(tmp1);

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Slip Total");
        tmp2.setValue(slip);
        creditSlipSum.add(tmp2);

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Total");
        tmp3.setValue(credit + slip);
        creditSlipSum.add(tmp3);

        ///////////////////
        cashChequeSum = new ArrayList<>();

        tmp1 = new String1Value1();
        tmp1.setString("Final Credit Card Total");
        tmp1.setValue(creditCard);

        tmp2 = new String1Value1();
        tmp2.setString("Final Cheque Total");
        tmp2.setValue(cheque);

        tmp3 = new String1Value1();
        tmp3.setString("Final Cash Total");
        tmp3.setValue(cash);

        String1Value1 tmp4 = new String1Value1();
        tmp4.setString("Total");
        tmp4.setValue(creditCard + cheque + cash);

        cashChequeSum.add(tmp1);
        cashChequeSum.add(tmp2);
        cashChequeSum.add(tmp3);
        cashChequeSum.add(tmp4);

    }

    public void createSumAfterCash() {
        List<BillsTotals> list2 = new ArrayList<>();
        list2.add(billedBills);
        list2.add(cancellededBills);
        list2.add(refundedBills);
        list2.add(billedBillsPh);
        list2.add(cancellededBillsPh);
        list2.add(refundedBillsPh);
        list2.add(billedPhWholeSale);
        list2.add(cancelledPhWholeSale);
        list2.add(refundedPhWholeSale);
        list2.add(paymentBills);
        list2.add(paymentCancelBills);
        list2.add(pettyPayments);
        list2.add(pettyPaymentsCancel);
        list2.add(GrnPaymentBill);
        list2.add(GrnPaymentCancell);
        list2.add(GrnPaymentReturn);
        list2.add(agentRecieves);
        list2.add(agentCancelBill);
        list2.add(inwardPayments);
        list2.add(inwardPaymentCancel);
        list2.add(inwardRefunds);
        list2.add(channelBilled);
        list2.add(channelCancells);
        list2.add(channelRefunds);
        list2.add(channelBilledProPayment);
        list2.add(channelCancellProPayment);
        list2.add(channelRefundsProPayment);
        list2.add(channelBilledAgnPayment);
        list2.add(channelCancellAgnPayment);
        list2.add(channelRefundAgnPayment);
        list2.add(cashRecieves);
        list2.add(cashRecieveCancel);
        list2.add(cashInBills);
        list2.add(cashInBillsCancel);
        list2.add(cashOutBills);
        list2.add(cashOutBillsCancel);
        list2.add(cashAdjustmentBills);

        double credit = 0.0;
        double slip = 0;
        double creditCard = 0.0;
        double cheque = 0.0;
        double cash = 0.0;
        for (BillsTotals bt : list2) {
            if (bt != null) {
                credit += bt.getCredit();
                slip += bt.getSlip();
                creditCard += bt.getCard();
                cheque += bt.getCheque();
                cash += bt.getCash();
            }
        }

        creditSlipSumAfter = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Total");
        tmp1.setValue(credit);
        creditSlipSumAfter.add(tmp1);

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Slip Total");
        tmp2.setValue(slip);
        creditSlipSumAfter.add(tmp2);

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Total");
        tmp3.setValue(credit + slip);
        creditSlipSumAfter.add(tmp3);

        ////////////////////////////////////////
        cashChequeSumAfter = new ArrayList<>();

        tmp1 = new String1Value1();
        tmp1.setString("Final Credit Card Total");
        tmp1.setValue(creditCard);

        tmp2 = new String1Value1();
        tmp2.setString("Final Cheque Total");
        tmp2.setValue(cheque);

        tmp3 = new String1Value1();
        tmp3.setString("Final Cash Total");
        tmp3.setValue(cash);

        String1Value1 tmp4 = new String1Value1();
        tmp4.setString("Total");
        tmp4.setValue(creditCard + cheque + cash);

        cashChequeSumAfter.add(tmp1);
        cashChequeSumAfter.add(tmp2);
        cashChequeSumAfter.add(tmp3);
        cashChequeSumAfter.add(tmp4);

    }

    public List<String1Value1> getCreditSlipSum2() {
        List<BillsTotals> list2 = new ArrayList<>();
        list2.add(billedBillsPh2);
        list2.add(cancellededBillsPh2);
        list2.add(refundedBillsPh2);

        List<String1Value1> list = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Total");
        tmp1.setValue(getFinalCreditTotal(list2));
        list.add(tmp1);

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Slip Total");
        tmp2.setValue(getFinalSlipTot(list2));
        list.add(tmp2);

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Total");
        tmp3.setValue(tmp1.getValue() + tmp2.getValue());
        list.add(tmp3);

        return list;
    }

    public List<String1Value1> getDataTableData() {
        List<BillsTotals> list = new ArrayList<>();
        list.add(getBilledBills());
        list.add(getCancellededBills());
        list.add(getRefundedBills());
        list.add(getBilledBillsPh());
        list.add(getCancellededBillsPh());
        list.add(getRefundedBillsPh());
        list.add(getPaymentBills());
        list.add(getPaymentCancelBills());
        list.add(getPettyPayments());
        list.add(getPettyPaymentsCancel());
        list.add(getAgentRecieves());
        list.add(getAgentCancelBill());
        list.add(getInwardPayments());
        list.add(getInwardPaymentCancel());
        list.add(getCashRecieves());
        list.add(getCashRecieveCancel());

        dataTableData = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Total");
        tmp1.setValue(getFinalCreditTotal(list));

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Credit Card Total");
        tmp2.setValue(getFinalCreditCardTotal(list));

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Final Cheque Total");
        tmp3.setValue(getFinalChequeTot(list));

        String1Value1 tmp4 = new String1Value1();
        tmp4.setString("Final Slip Total");
        tmp4.setValue(getFinalSlipTot(list));

        String1Value1 tmp5 = new String1Value1();
        tmp5.setString("Final Cash Total");
        tmp5.setValue(getFinalCashTotal(list));

        dataTableData.add(tmp1);
        dataTableData.add(tmp2);
        dataTableData.add(tmp3);
        dataTableData.add(tmp4);
        dataTableData.add(tmp5);

        return dataTableData;
    }

    public List<String1Value1> getGrnTotal() {
        List<BillsTotals> list = new ArrayList<>();
        list.add(getGrnBilled());
        list.add(getGrnCancelled());
        list.add(getGrnReturn());
        list.add(getGrnReturnCancel());

        dataTableData = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Total");
        tmp1.setValue(getFinalCreditTotal(list));

        String1Value1 tmp5 = new String1Value1();
        tmp5.setString("Final Cash Total");
        tmp5.setValue(getFinalCashTotal(list));

        String1Value1 tmp6 = new String1Value1();
        tmp6.setString("Final Credit & Cash Total");
        tmp6.setValue(getFinalCashTotal(list) + getFinalCreditTotal(list));

        dataTableData.add(tmp1);
        dataTableData.add(tmp5);
        dataTableData.add(tmp6);

        return dataTableData;
    }

    public List<String1Value1> getPurchaseTotal() {
        List<BillsTotals> list = new ArrayList<>();
        list.add(getPurchaseBilled());
        list.add(getPurchaseCancelled());
        list.add(getPurchaseReturn());
        list.add(getPurchaseReturnCancel());

        dataTableData = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Total");
        tmp1.setValue(getFinalCreditTotal(list));

        String1Value1 tmp5 = new String1Value1();
        tmp5.setString("Final Cash Total");
        tmp5.setValue(getFinalCashTotal(list));

        String1Value1 tmp6 = new String1Value1();
        tmp6.setString("Final Credit & Cash Total");
        tmp6.setValue(getFinalCashTotal(list) + getFinalCreditTotal(list));

        dataTableData.add(tmp1);
        dataTableData.add(tmp5);
        dataTableData.add(tmp6);

        return dataTableData;
    }

    public List<String1Value1> getGRNPaymentTotal() {
        List<BillsTotals> list = new ArrayList<>();
        list.add(getGrnPaymentBill());
        list.add(getGrnPaymentCancell());
        list.add(getGrnPaymentReturn());
        list.add(getGrnPaymentCancellReturn());

        dataTableData = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Total");
        tmp1.setValue(getFinalCreditTotal(list));

        String1Value1 tmp5 = new String1Value1();
        tmp5.setString("Final Cash Total");
        tmp5.setValue(getFinalCashTotal(list));

        String1Value1 tmp6 = new String1Value1();
        tmp6.setString("Final Credit & Cash Total");
        tmp6.setValue(getFinalCashTotal(list) + getFinalCreditTotal(list));

        dataTableData.add(tmp1);
        dataTableData.add(tmp5);
        dataTableData.add(tmp6);

        return dataTableData;
    }

    public List<String1Value1> getDataTableDataByType() {
        List<BillsTotals> list = new ArrayList<>();
        if (billType == BillType.OpdBill) {
            list.add(getBilledBills());
            list.add(getCancellededBills());
            list.add(getRefundedBills());
        }
        if (billType == BillType.PharmacySale) {
            list.add(getBilledBillsPh());
            list.add(getCancellededBillsPh());
            list.add(getRefundedBillsPh());
        }

        if (billType == BillType.PaymentBill) {
            list.add(getPaymentBills());
            list.add(getPaymentCancelBills());
        }
        if (billType == BillType.PettyCash) {
            list.add(getPettyPayments());
            list.add(getPettyPaymentsCancel());
        }
        if (billType == BillType.AgentPaymentReceiveBill) {
            list.add(getAgentRecieves());
            list.add(getAgentCancelBill());
        }
        if (billType == BillType.InwardPaymentBill) {
            list.add(getInwardPayments());
            list.add(getInwardPaymentCancel());
        }
        if (billType == BillType.CashRecieveBill) {
            list.add(getCashRecieves());
            list.add(getCashRecieveCancel());
        }

        List< String1Value1> data = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Total");
        tmp1.setValue(getFinalCreditTotal(list));

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Credit Card Total");
        tmp2.setValue(getFinalCreditCardTotal(list));

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Final Cheque Total");
        tmp3.setValue(getFinalChequeTot(list));

        String1Value1 tmp4 = new String1Value1();
        tmp4.setString("Final Slip Total");
        tmp4.setValue(getFinalSlipTot(list));

        String1Value1 tmp5 = new String1Value1();
        tmp5.setString("Final Cash Total");
        tmp5.setValue(getFinalCashTotal(list));

        data.add(tmp1);
        data.add(tmp2);
        data.add(tmp3);
        data.add(tmp4);
        data.add(tmp5);

        return data;
    }

    public List<String1Value1> getDataTableDataByType2() {
        List<BillsTotals> list = new ArrayList<>();

        list.add(getBilledBills());
        list.add(getCancellededBills());
        list.add(getRefundedBills());

        List< String1Value1> data = new ArrayList<>();
        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Total");
        tmp1.setValue(getFinalCreditTotal(list));

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Credit Card Total");
        tmp2.setValue(getFinalCreditCardTotal(list));

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Final Cheque Total");
        tmp3.setValue(getFinalChequeTot(list));

        String1Value1 tmp4 = new String1Value1();
        tmp4.setString("Final Slip Total");
        tmp4.setValue(getFinalSlipTot(list));

        String1Value1 tmp5 = new String1Value1();
        tmp5.setString("Final Cash Total");
        tmp5.setValue(getFinalCashTotal(list));

        String1Value1 tmp6 = new String1Value1();
        tmp6.setString("Grant Total");
        tmp6.setValue(getFinalCreditTotal(list) + getFinalCreditCardTotal(list) + getFinalChequeTot(list) + getFinalSlipTot(list) + getFinalCashTotal(list));

        data.add(tmp1);
        data.add(tmp2);
        data.add(tmp3);
        data.add(tmp4);
        data.add(tmp5);
        data.add(tmp6);

        return data;
    }

    private List<String1Value1> cashChequeSum;
    private List<String1Value1> cashChequeSumAfter;

    public List<String1Value1> getCashChequeSum2() {
        List<BillsTotals> list2 = new ArrayList<>();
        list2.add(billedBillsPh2);
        list2.add(cancellededBillsPh2);
        list2.add(refundedBillsPh2);

        List<String1Value1> list = new ArrayList<>();

        String1Value1 tmp1 = new String1Value1();
        tmp1.setString("Final Credit Card Total");
        tmp1.setValue(getFinalCreditCardTotal(list2));

        String1Value1 tmp2 = new String1Value1();
        tmp2.setString("Final Cheque Total");
        tmp2.setValue(getFinalChequeTot(list2));

        String1Value1 tmp3 = new String1Value1();
        tmp3.setString("Final Cash Total");
        tmp3.setValue(getFinalCashTotal(list2));

        String1Value1 tmp4 = new String1Value1();
        tmp4.setString("Total");
        tmp4.setValue(tmp1.getValue() + tmp2.getValue() + tmp3.getValue());

        list.add(tmp1);
        list.add(tmp2);
        list.add(tmp3);
        list.add(tmp4);
        return list;
    }

    public void setDataTableData(List<String1Value1> dataTableData) {
        this.dataTableData = dataTableData;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public BillsTotals getBilledBillsPh() {
        if (billedBillsPh == null) {
            billedBillsPh = new BillsTotals();
        }
        return billedBillsPh;
    }

    public void setBilledBillsPh(BillsTotals billedBillsPh) {
        this.billedBillsPh = billedBillsPh;
    }

    public BillsTotals getCancellededBillsPh() {
        if (cancellededBillsPh == null) {
            cancellededBillsPh = new BillsTotals();
        }
        return cancellededBillsPh;
    }

    public void setCancellededBillsPh(BillsTotals cancellededBillsPh) {
        this.cancellededBillsPh = cancellededBillsPh;
    }

    public BillsTotals getRefundedBillsPh() {
        if (refundedBillsPh == null) {
            refundedBillsPh = new BillsTotals();
        }
        return refundedBillsPh;
    }

    public void setRefundedBillsPh(BillsTotals refundedBillsPh) {
        this.refundedBillsPh = refundedBillsPh;
    }

    
    public BillsTotals getBilledBillsPh2() {
        if (billedBillsPh2 == null) {
            billedBillsPh2 = new BillsTotals();
        }
        return billedBillsPh2;
    }

    public void setBilledBillsPh2(BillsTotals billedBillsPh2) {
        this.billedBillsPh2 = billedBillsPh2;
    }

    public BillsTotals getCancellededBillsPh2() {
        if (cancellededBillsPh2 == null) {
            cancellededBillsPh2 = new BillsTotals();
        }
        return cancellededBillsPh2;
    }

    public void setCancellededBillsPh2(BillsTotals cancellededBillsPh2) {
        this.cancellededBillsPh2 = cancellededBillsPh2;
    }

    public BillsTotals getRefundedBillsPh2() {
        if (refundedBillsPh2 == null) {
            refundedBillsPh2 = new BillsTotals();
        }
        return refundedBillsPh2;
    }

    public void setRefundedBillsPh2(BillsTotals refundedBillsPh2) {
        this.refundedBillsPh2 = refundedBillsPh2;
    }
    
    

    public BillsTotals getGrnBilled() {
        return grnBilled;
    }

    public void setGrnBilled(BillsTotals grnBilled) {
        this.grnBilled = grnBilled;
    }

    public BillsTotals getGrnCancelled() {
        return grnCancelled;
    }

    public void setGrnCancelled(BillsTotals grnCancelled) {
        this.grnCancelled = grnCancelled;
    }

    public BillsTotals getGrnReturn() {
        return grnReturn;
    }

    public void setGrnReturn(BillsTotals grnReturn) {
        this.grnReturn = grnReturn;
    }

    public BillsTotals getGrnReturnCancel() {
        return grnReturnCancel;
    }

    public void setGrnReturnCancel(BillsTotals grnReturnCancel) {
        this.grnReturnCancel = grnReturnCancel;
    }

    public List<PriceMatrix> getItems() {
        return items;
    }

    public void setItems(List<PriceMatrix> items) {
        this.items = items;
    }

    public PriceMatrixFacade getInwdPriceAdjFacade() {
        return inwdPriceAdjFacade;
    }

    public void setInwdPriceAdjFacade(PriceMatrixFacade inwdPriceAdjFacade) {
        this.inwdPriceAdjFacade = inwdPriceAdjFacade;
    }

    public BillsTotals getPurchaseBilled() {
        return purchaseBilled;
    }

    public void setPurchaseBilled(BillsTotals purchaseBilled) {
        this.purchaseBilled = purchaseBilled;
    }

    public BillsTotals getPurchaseCancelled() {
        return purchaseCancelled;
    }

    public void setPurchaseCancelled(BillsTotals purchaseCancelled) {
        this.purchaseCancelled = purchaseCancelled;
    }

    public BillsTotals getPurchaseReturn() {
        return purchaseReturn;
    }

    public void setPurchaseReturn(BillsTotals purchaseReturn) {
        this.purchaseReturn = purchaseReturn;
    }

    public BillsTotals getPurchaseReturnCancel() {
        return purchaseReturnCancel;
    }

    public void setPurchaseReturnCancel(BillsTotals purchaseReturnCancel) {
        this.purchaseReturnCancel = purchaseReturnCancel;
    }

    public BillsTotals getGrnPaymentBill() {
        if(GrnPaymentBill==null){
            GrnPaymentBill=new BillsTotals();
        }
        return GrnPaymentBill;
    }

    public void setGrnPaymentBill(BillsTotals GrnPaymentBill) {
        this.GrnPaymentBill = GrnPaymentBill;
    }

    public BillsTotals getGrnPaymentReturn() {
        if(GrnPaymentReturn==null){
            GrnPaymentReturn=new BillsTotals();
        }
        return GrnPaymentReturn;
    }

    public void setGrnPaymentReturn(BillsTotals GrnPaymentReturn) {
        this.GrnPaymentReturn = GrnPaymentReturn;
    }

    public BillsTotals getGrnPaymentCancell() {
        if(GrnPaymentCancell==null){
            GrnPaymentCancell=new BillsTotals();
        }
        return GrnPaymentCancell;
    }

    public void setGrnPaymentCancell(BillsTotals GrnPaymentCancell) {
        this.GrnPaymentCancell = GrnPaymentCancell;
    }

    public BillsTotals getGrnPaymentCancellReturn() {
        return GrnPaymentCancellReturn;
    }

    public void setGrnPaymentCancellReturn(BillsTotals GrnPaymentCancellReturn) {
        this.GrnPaymentCancellReturn = GrnPaymentCancellReturn;
    }

    public BillsTotals getInwardRefunds() {
        if (inwardRefunds == null) {
            inwardRefunds = new BillsTotals();
        }
        return inwardRefunds;
    }

    public void setInwardRefunds(BillsTotals inwardRefunds) {
        this.inwardRefunds = inwardRefunds;
    }

    public BillsTotals getPharmacyBhtPreBilled() {
        return PharmacyBhtPreBilled;
    }

    public void setPharmacyBhtPreBilled(BillsTotals PharmacyBhtPreBilled) {
        this.PharmacyBhtPreBilled = PharmacyBhtPreBilled;
    }

    public BillsTotals getPharmacyBhtPreCancelled() {
        return PharmacyBhtPreCancelled;
    }

    public void setPharmacyBhtPreCancelled(BillsTotals PharmacyBhtPreCancelled) {
        this.PharmacyBhtPreCancelled = PharmacyBhtPreCancelled;
    }

    public BillsTotals getPharmacyBhtPreRefunded() {
        return PharmacyBhtPreRefunded;
    }

    public void setPharmacyBhtPreRefunded(BillsTotals PharmacyBhtPreRefunded) {
        this.PharmacyBhtPreRefunded = PharmacyBhtPreRefunded;
    }

    public BillItemFacade getBillItemFac() {
        return billItemFac;
    }

    public void setBillItemFac(BillItemFacade billItemFac) {
        this.billItemFac = billItemFac;
    }

    public List<String1Value1> getCreditSlipSum() {
        return creditSlipSum;
    }

    public void setCreditSlipSum(List<String1Value1> creditSlipSum) {
        this.creditSlipSum = creditSlipSum;
    }

    public List<String1Value1> getCashChequeSum() {
        return cashChequeSum;
    }

    public void setCashChequeSum(List<String1Value1> cashChequeSum) {
        this.cashChequeSum = cashChequeSum;
    }

    public List<String1Value1> getCreditSlipSumAfter() {
        return creditSlipSumAfter;
    }

    public void setCreditSlipSumAfter(List<String1Value1> creditSlipSumAfter) {
        this.creditSlipSumAfter = creditSlipSumAfter;
    }

    public List<String1Value1> getCashChequeSumAfter() {
        return cashChequeSumAfter;
    }

    public void setCashChequeSumAfter(List<String1Value1> cashChequeSumAfter) {
        this.cashChequeSumAfter = cashChequeSumAfter;
    }

    public List<BillFee> getBillFees() {
        return billFees;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public Institution getReferenceInstitution() {
        return referenceInstitution;
    }

    public void setReferenceInstitution(Institution referenceInstitution) {
        this.referenceInstitution = referenceInstitution;
    }

    public BillsTotals getStoreBhtPreBilled() {
        return StoreBhtPreBilled;
    }

    public void setStoreBhtPreBilled(BillsTotals StoreBhtPreBilled) {
        this.StoreBhtPreBilled = StoreBhtPreBilled;
    }

    public BillsTotals getStoreBhtPreCancelled() {
        return StoreBhtPreCancelled;
    }

    public void setStoreBhtPreCancelled(BillsTotals StoreBhtPreCancelled) {
        this.StoreBhtPreCancelled = StoreBhtPreCancelled;
    }

    public BillsTotals getStoreBhtPreRefunded() {
        return StoreBhtPreRefunded;
    }

    public void setStoreBhtPreRefunded(BillsTotals StoreBhtPreRefunded) {
        this.StoreBhtPreRefunded = StoreBhtPreRefunded;
    }

    public List<Bill> getReferralBills() {
        return referralBills;
    }

    public void setReferralBills(List<Bill> referralBills) {
        this.referralBills = referralBills;
    }

    public Item getReferenceItem() {
        return referenceItem;
    }

    public void setReferenceItem(Item referenceItem) {
        this.referenceItem = referenceItem;
    }

    public BillsTotals getBilledBillsCh() {
        return billedBillsCh;
    }

    public void setBilledBillsCh(BillsTotals billedBillsCh) {
        this.billedBillsCh = billedBillsCh;
    }

    public BillsTotals getCancelledPhWholeSale() {
        if(cancelledPhWholeSale==null){
            cancelledPhWholeSale = new BillsTotals();
        }
        return cancelledPhWholeSale;
    }

    public void setCancelledPhWholeSale(BillsTotals cancelledPhWholeSale) {
        this.cancelledPhWholeSale = cancelledPhWholeSale;
    }

    public BillsTotals getRefundedPhWholeSale() {
        if(refundedPhWholeSale==null){
            refundedPhWholeSale=new BillsTotals();
        }
        return refundedPhWholeSale;
    }

    public void setRefundedPhWholeSale(BillsTotals refundedPhWholeSale) {
        this.refundedPhWholeSale = refundedPhWholeSale;
    }

    public BillsTotals getBilledPhWholeSale() {
        if(billedPhWholeSale==null){
            billedPhWholeSale= new BillsTotals();
        }
        return billedPhWholeSale;
    }

    public void setBilledPhWholeSale(BillsTotals billedPhWholeSale) {
        this.billedPhWholeSale = billedPhWholeSale;
    }

}
