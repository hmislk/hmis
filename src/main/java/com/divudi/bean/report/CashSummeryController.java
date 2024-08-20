/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.report;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.inward.AdmissionTypeController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.AdmissionTypeBills;
import com.divudi.data.dataStructure.BillItemWithFee;
import com.divudi.data.dataStructure.BillsItems;
import com.divudi.data.dataStructure.CategoryWithItem;
import com.divudi.data.dataStructure.DailyCash;
import com.divudi.data.dataStructure.DepartmentPayment;
import com.divudi.data.dataStructure.ItemWithFee;
import com.divudi.data.table.String1Value1;
import com.divudi.data.table.String1Value3;

import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.CategoryFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Buddhika
 */
@Named
@RequestScoped
public class CashSummeryController implements Serializable {

    @Inject
    private SessionController sessionController;
    // private List<DailyCash> dailyCashs;
    @Temporal(TemporalType.TIMESTAMP)
    private Date fromDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date toDate;

    private CommonFunctions commonFunctions;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private CategoryFacade categoryFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    private List<DailyCash> dailyCash;
    private List<Bill> inwardPayments;
    List<String1Value3> otherInstitution;
    @Inject
    private BillBeanController billBean;
    private double creditCompanyTotal = 0.0;
    private double grantTotal;
    private double cardTot;
    private double chequeTot;
    private double slipTot;
    private double inwardTot;
    private double inwardProfTot;
    @Inject
    private AdmissionTypeController admissionTypeController;
    boolean paginator;
    
    public void makePaginatorFalse(){
        paginator=false;
    }
    

    public long getCountTotal() {
        long countTotal = 0;

        long billed = getCount(new BilledBill());
        long cancelled = getCount(new CancelledBill());
        long refunded = getCount(new RefundBill());

        countTotal = billed - (refunded + cancelled);

        //  //System.err.println("Billed : " + billed);
        //   //System.err.println("Cancelled : " + cancelled);
        //   //System.err.println("Refunded : " + refunded);
        //   //System.err.println("Gross Tot : " + countTotal);
        return countTotal;
    }

    private long getCount(Bill bill) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi where bi.bill.billType=:bType and bi.item=:itm "
                + " and type(bi.bill)=:billClass and bi.bill.toInstitution=:ins "
                + " and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", getService());
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", BillType.OpdBill);
        temMap.put("ins", getSessionController().getInstitution());
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<String1Value1> getSummerizeTable() {
        List<String1Value1> tmp = new ArrayList<>();
        String1Value1 ttt;

        ttt = new String1Value1();
        getPathologyFees();
        ttt.setString("Pathology Colection");
        ttt.setValue(getPathTotal());
        tmp.add(ttt);

        ttt = new String1Value1();
        getPharmacySale();
        ttt.setString("Pharmacy Colection");
        ttt.setValue(getPharmacyTotal());
        tmp.add(ttt);

        for (AdmissionTypeBills adB : getInwardCollection()) {
            if (adB.getTotal() != 0) {
                ttt = new String1Value1();
                ttt.setString(adB.getAdmissionType().getName());
                ttt.setValue(adB.getTotal());
                tmp.add(ttt);
            }
        }

        ttt = new String1Value1();
        getAgentCollection();
        ttt.setString("Agent Collection");
        ttt.setValue(getAgentCollectionTot());
        tmp.add(ttt);

        ttt = new String1Value1();
        getCreditCompanyCollection();
        ttt.setString("Credit Company Collection");
        ttt.setValue(getCreditCompanyTotal());
        tmp.add(ttt);

        return tmp;

    }

    public List<String1Value1> getSummerizeTableAfter() {
        List<String1Value1> tmp = new ArrayList<>();
        String1Value1 ttt;

        ttt = new String1Value1();
        getDepartmentPayment();
        ttt.setString("Opd Professional Payment");
        ttt.setValue(getDoctorPaymentTot());
        tmp.add(ttt);

        for (String1Value1 dtd : getInwardProfessions()) {
            ttt = new String1Value1();
            ttt.setString(dtd.getString());
            ttt.setValue(dtd.getValue());
            tmp.add(ttt);
        }

        ttt = new String1Value1();
        getCardBill();
        ttt.setString("Credit Card Collection");
        ttt.setValue(getCardTot());
        tmp.add(ttt);

        ttt = new String1Value1();
        getSlipBill();
        ttt.setString("Slip Collection");
        ttt.setValue(getSlipTot());
        tmp.add(ttt);

        ttt = new String1Value1();
        getChequeBill();
        ttt.setString("Cheque Collection");
        ttt.setValue(getChequeTot());
        tmp.add(ttt);

        return tmp;

    }

    public void makeNull() {
        dailyCash = null;
        categoryWithItem = null;
    }

    public List<String1Value1> getFinalSummery() {
        List<String1Value1> tmp = new ArrayList<>();
        String1Value1 dd;
        dd = new String1Value1();
        dd.setString("Net Cash");
        dd.setValue(getNetCash());
        tmp.add(dd);

        dd = new String1Value1();
        dd.setString("Lab Handover Total");

        tmp.add(dd);

        dd = new String1Value1();
        dd.setString("Final Cash");

        tmp.add(dd);

        return tmp;
    }

    public List<String1Value1> getBankingData() {
        List<String1Value1> tmp = new ArrayList<>();
        String1Value1 dd;
        dd = new String1Value1();
        dd.setString("People Bank");
        tmp.add(dd);

        dd = new String1Value1();
        dd.setString("HNB 1");
        tmp.add(dd);

        dd = new String1Value1();
        dd.setString("HNB 2");
        tmp.add(dd);

        dd = new String1Value1();
        dd.setString("BOC");
        tmp.add(dd);

        dd = new String1Value1();
        dd.setString("NSB");
        tmp.add(dd);

        dd = new String1Value1();
        dd.setString("B/F Cash Balance");
        tmp.add(dd);

        return tmp;
    }

    public List<String1Value1> getCollections() {
        List<String1Value1> tmp = new ArrayList<>();
        String1Value1 dd;
        dd = new String1Value1();
        dd.setString("Collection For the Day");
        dd.setValue(getGrantTotal());
        tmp.add(dd);

        dd = new String1Value1();
        dd.setString("Petty cash Payments");
        dd.setValue(getPettyTot());
        tmp.add(dd);

        return tmp;
    }

    public List<String1Value1> getCollections2() {
        List<String1Value1> tmp = new ArrayList<>();
        String1Value1 dd;
        dd = new String1Value1();
        dd.setString("Collection For the Day");
        dd.setValue(getGrantTotal2());
        tmp.add(dd);

        dd = new String1Value1();
        dd.setString("Petty cash Payments");
        dd.setValue(getPettyTot());
        tmp.add(dd);

        return tmp;
    }

    public List<String1Value1> getCollections2Hos() {
        List<String1Value1> tmp = new ArrayList<>();
        String1Value1 dd;
        dd = new String1Value1();
        dd.setString("Collection For the Day");
        dd.setValue(getGrantTotal2Hos());
        tmp.add(dd);

        dd = new String1Value1();
        dd.setString("Petty cash Payments");
        dd.setValue(getPettyTot());
        tmp.add(dd);

        return tmp;
    }

    public List<String1Value1> getInwardProfessions() {
        inwardProfTot = 0.0;
        List<String1Value1> tmp = new ArrayList<>();
        List<AdmissionTypeBills> lst = new ArrayList<>();
        for (AdmissionType at : getAdmissionTypeController().getItems()) {
            AdmissionTypeBills admB = new AdmissionTypeBills();
            admB.setAdmissionType(at);
            admB.setTotal(getInwardProfTot(at));
            inwardProfTot += admB.getTotal();
            lst.add(admB);
        }

        for (AdmissionTypeBills atb : lst) {
            if (atb.getTotal() != 0) {
                String1Value1 dd;
                dd = new String1Value1();
                dd.setString(atb.getAdmissionType().getName());
                dd.setValue(atb.getTotal());
                tmp.add(dd);
            }
        }

        return tmp;
    }

    private List<Department> getDepartmentOfInstitution() {
        String sql = "select d from Department d where d.retired=false and d.institution=:ins";
        HashMap hm = new HashMap();
        hm.put("ins", getSessionController().getInstitution());

        return getDepartmentFacade().findByJpql(sql, hm);
    }

    private double pharmacyTotal;

    public List<DailyCash> getPharmacySale() {
        //System.err.println("GETTING PHARMACY SALE");
        List<DailyCash> list = new ArrayList<>();
        pharmacyTotal = 0;
        for (Department d : getDepartmentOfInstitution()) {
            //System.err.println("DEP " + d.getName());
            String sql = "Select sum(b.netTotal) from Bill b where b.retired=false and  b.billType=:bType and b.referenceBill.department=:dep "
                    + " and b.createdAt between :fromDate and :toDate and (b.paymentMethod = :pm1 or  b.paymentMethod = :pm2 or "
                    + " b.paymentMethod = :pm3 or  b.paymentMethod = :pm4)";
            HashMap hm = new HashMap();
            hm.put("bType", BillType.PharmacySale);
            hm.put("dep", d);
            hm.put("fromDate", getFromDate());
            hm.put("toDate", getToDate());
            hm.put("pm1", PaymentMethod.Cash);
            hm.put("pm2", PaymentMethod.Card);
            hm.put("pm3", PaymentMethod.Cheque);
            hm.put("pm4", PaymentMethod.Slip);
            double netTotal = getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

            if (netTotal != 0) {
                //System.err.println("NET " + netTotal);
                pharmacyTotal += netTotal;
                DailyCash dl = new DailyCash();
                dl.setDepartment(d);
                dl.setDepartmentTotal(netTotal);
                list.add(dl);
            }

        }

        return list;
    }

    public List<String1Value3> getOtherInstitution() {
        otherInstitution = new ArrayList<>();
        if (getPathTotal() != 0) {
            String1Value3 tmp = new String1Value3();
            tmp.setString("Pathology Lab");
            tmp.setValue2(getPathProf());
            tmp.setValue3(getPathTotal());
            tmp.setValue1(tmp.getValue3() - tmp.getValue2());
            otherInstitution.add(tmp);
        }
        return otherInstitution;
    }

    public void setOtherInstitution(List<String1Value3> otherInstitution) {
        this.otherInstitution = otherInstitution;
    }

    public List<CategoryWithItem> getCategoryWithItem() {
        if (categoryWithItem == null) {
            categoryWithItem = new ArrayList<>();
        }
        return categoryWithItem;
    }

    public void setCategoryWithItem(List<CategoryWithItem> categoryWithItem) {
        this.categoryWithItem = categoryWithItem;
    }

    public List<ItemWithFee> getItemWithFees() {
        return itemWithFees;
    }

    public void setItemWithFees(List<ItemWithFee> itemWithFees) {
        this.itemWithFees = itemWithFees;
    }

    public double getHosTotal() {
        double tmp = 0.0;
        for (ItemWithFee i : itemWithFees) {
            tmp += i.getHospitalFee();
        }
        return tmp;
    }

    public double getProfTotal() {
        double tmp = 0.0;
        for (ItemWithFee i : itemWithFees) {
            tmp += i.getProFee();
        }
        return tmp;
    }

    public double getNetCash() {
        double tmp = grantTotal - doctorPaymentTot + getPettyTot() - cardTot - slipTot - chequeTot - inwardProfTot;

        return tmp;
    }

    public List<Bill> getCardBill() {
        cardTot = 0.0;
        List<Bill> tmp = bills(PaymentMethod.Card);
        for (Bill b : tmp) {
            cardTot += b.getNetTotal();
        }

        return tmp;
    }

    private List<Bill> bills(PaymentMethod paymentMethod) {
        List<Bill> lstBills;
        String sql;
        Map temMap = new HashMap();
        sql = "select b from Bill b where type(b)!=:type and b.institution=:ins "
                + " and b.paymentMethod = :bTp and "
                + " b.createdAt between :fromDate and :toDate and b.retired=false order by b.id desc  ";

        temMap.put("bTp", paymentMethod);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("type", PreBill.class);
        temMap.put("ins", getSessionController().getInstitution());
        lstBills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return lstBills;

    }

    public List<Bill> getSlipBill() {
        slipTot = 0.0;
        List<Bill> tmp = bills(PaymentMethod.Slip);
        for (Bill b : tmp) {
            slipTot += b.getNetTotal();
        }

        return tmp;
    }

    public List<Bill> getChequeBill() {
        chequeTot = 0.0;
        List<Bill> tmp = bills(PaymentMethod.Cheque);
        for (Bill b : tmp) {
            chequeTot += b.getNetTotal();
        }
        return tmp;
    }

    public List<BillFee> getPathologyFees() {
        String sql = "SELECT bf FROM BillFee bf WHERE bf.billItem.bill.institution=:ins and bf.billItem.item.institution!=:ins and  bf.createdAt between :fromDate and :toDate  and "
                + "( bf.billItem.bill.paymentMethod = :pm1 or  bf.billItem.bill.paymentMethod = :pm2 or "
                + " bf.billItem.bill.paymentMethod = :pm3 or  bf.billItem.bill.paymentMethod = :pm4)";

        HashMap temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        List<BillFee> billFees = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return billFees;
    }

    public double getPathTotal() {
        double tmp = 0.0;
        for (BillFee b : getPathologyFees()) {
            tmp += b.getFeeValue();
        }
        return tmp;
    }

    public double getPathProf() {
        double tmp = 0.0;
        for (BillFee b : getPathologyFees()) {
            if (b.getStaff() != null) {
                tmp += b.getFeeValue();
            }
        }
        return tmp;
    }

    private List<Bill> bills(BillType billType) {
        String sql;
        sql = "SELECT b FROM Bill b WHERE b.retired=false and b.billType = :bTp and b.institution=:ins and b.createdAt between :fromDate and :toDate order by b.id";

        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bTp", billType);
        temMap.put("ins", getSessionController().getInstitution());

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public List<BillsItems> getCreditCompanyCollection() {
        creditCompanyTotal = 0.0;
        List<BillsItems> billsItems = new ArrayList<>();
        List<Bill> tmp = bills(BillType.CashRecieveBill);

        for (Bill b : tmp) {
            creditCompanyTotal += b.getNetTotal();

            BillsItems newB = new BillsItems();
            newB.setBill(b);

            String sql;
            HashMap temMap = new HashMap();

            sql = "SELECT b FROM BillItem b WHERE b.bill.institution=:ins and b.retired=false and b.bill.id=" + b.getId() + " and b.createdAt between :fromDate and :toDate order by b.id";

            temMap.put("fromDate", getFromDate());
            temMap.put("toDate", getToDate());
            temMap.put("ins", getSessionController().getInstitution());
            newB.setBillItems(getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP));

            billsItems.add(newB);
        }

        return billsItems;
    }
    private double agentCollectionTot;

    public List<Bill> getAgentCollection() {
        agentCollectionTot = 0.0;

        List<Bill> tmp = bills(BillType.AgentPaymentReceiveBill);

        for (Bill b : tmp) {
            agentCollectionTot += b.getNetTotal();
        }

        return tmp;
    }

    public List<AdmissionTypeBills> getInwardCollection() {
        inwardTot = 0.0;
        List<AdmissionTypeBills> tmp = new ArrayList<>();
        for (AdmissionType at : getAdmissionTypeController().getItems()) {
            AdmissionTypeBills admB = new AdmissionTypeBills();
            admB.setAdmissionType(at);
            admB.setBills(getInwardBills(at));
            admB.setTotal(calTotal(admB.getBills()));
            inwardTot += admB.getTotal();
            tmp.add(admB);
        }

        return tmp;
    }

    private double calTotal(List<Bill> lst) {
        double tmp = 0.0;
        for (Bill b : lst) {
            tmp += b.getNetTotal();
        }
        return tmp;
    }

    private List<Bill> getInwardBills(AdmissionType admissionType) {
        String sql;
        sql = "SELECT b FROM Bill b WHERE b.retired=false and b.billType = :bTp and b.patientEncounter.admissionType=:adm  and b.institution=:ins and b.createdAt between :fromDate and :toDate order by b.id";
        Map temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("bTp", BillType.InwardPaymentBill);
        temMap.put("adm", admissionType);
        temMap.put("ins", getSessionController().getInstitution());
        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double getInwardProfTot(AdmissionType adt) {
        double tmp = 0.0;
        HashMap temMap = new HashMap();

        String sql = "SELECT b FROM BillItem b WHERE b.referanceBillItem is null and "
                + " b.referenceBill.billType=:btp and b.referenceBill.patientEncounter.admissionType=:admis"
                + "  and b.retired=false and b.bill.createdAt between :fromDate and :toDate";

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", BillType.InwardBill);
        temMap.put("admis", adt);
        List<BillItem> tmp2 = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        for (BillItem b : tmp2) {
            tmp += b.getNetValue();
        }

        return tmp;
    }

    private double doctorPaymentTot = 0.0;

    public List<DepartmentPayment> getDepartmentPayment() {
        doctorPaymentTot = 0.0;
        HashMap temMap = new HashMap();

        String sql = "SELECT b FROM BillItem b "
                + " WHERE b.bill.institution=:ins "
                + " and b.retired=false "
                + " and b.bill.billType= :btp"
                + " and b.createdAt between :fromDate and :toDate";

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", BillType.PaymentBill);
        temMap.put("ins", getSessionController().getInstitution());
        List<BillItem> billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        Set<Department> depSet;
        depSet = new HashSet();

        for (BillItem b : billItems) {
            if (b.getReferanceBillItem() != null && b.getReferanceBillItem().getItem() != null && b.getReferanceBillItem().getItem().getDepartment() != null) {
                depSet.add(b.getReferanceBillItem().getItem().getDepartment());
            }
        }

        List<DepartmentPayment> depP = new ArrayList<>();

        for (Department dep : depSet) {
            DepartmentPayment dp = new DepartmentPayment();
            dp.setDepartment(dep);

            double tot = 0.0;

            for (BillItem bb : billItems) {
                if (bb.getReferanceBillItem() != null && bb.getReferanceBillItem().getItem() != null && bb.getReferanceBillItem().getItem().getDepartment() != null) {
                    if (bb.getReferanceBillItem().getItem().getDepartment().getId() == dep.getId()) {
                        tot += bb.getNetValue();
                        doctorPaymentTot += bb.getNetValue();
                    }
                }
            }

            if (tot != 0) {
                dp.setTotalPayment(tot);
                depP.add(dp);
            }
        }

        return depP;
    }

    public List<BillsItems> getDoctorPayment() {
        List<BillsItems> billsItems = new ArrayList<>();

        List<Bill> tmp = bills(BillType.PaymentBill);

        for (Bill b : tmp) {

            BillsItems nB = new BillsItems();
            nB.setBill(b);

            String sql = "SELECT b FROM BillItem b WHERE b.bill.institution=:ins and b.retired=false and b.bill.id=" + b.getId() + " and b.createdAt between :fromDate and :toDate order by b.id";
            HashMap temMap = new HashMap();
            temMap.put("fromDate", getFromDate());
            temMap.put("toDate", getToDate());
            temMap.put("ins", getSessionController().getInstitution());
            nB.setBillItems(getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP));

            billsItems.add(nB);
        }

        return billsItems;
    }

    public List<Bill> getRecieveBill() {
        String sql = "Select b From BilledBill b where b.institution=:ins and b.retired=false and b.billType= :bTp order by :bTp";
        HashMap h = new HashMap();
        h.put("bTp", BillType.CashRecieveBill);
        h.put("ins", getSessionController().getInstitution());

        return getBillFacade().findByJpql(sql, h, TemporalType.DATE);
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    private List<Department> findDepartment() {
        if (getSessionController().getInstitution() == null) {
            return new ArrayList<>();
        }

        String sql;
        Map temMap = new HashMap();
        sql = "select distinct(bi.item.department) FROM BillItem bi where bi.bill.institution=:ins and bi.bill.billType= :bTp and bi.item.institution=:ins "
                + " and  bi.bill.createdAt between :fromDate and :toDate "
                + " and ( bi.bill.paymentMethod = :pm1 or  bi.bill.paymentMethod = :pm2 or  bi.bill.paymentMethod = :pm3 or  bi.bill.paymentMethod = :pm4) ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);

        List<Department> tmp = getDepartmentFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        return tmp;
    }

    private List<Category> findCategory(Department d) {
        String sql;
        Map temMap = new HashMap();
        if (d == null) {
            return new ArrayList<>();
        }
        sql = "select distinct(bi.item.category) FROM BillItem bi where bi.bill.institution=:ins and bi.bill.billType= :bTp "
                + " and bi.item.department=:dep and  bi.bill.createdAt between :fromDate and :toDate "
                + " and ( bi.bill.paymentMethod = :pm1 or  bi.bill.paymentMethod = :pm2 or  bi.bill.paymentMethod = :pm3 or  bi.bill.paymentMethod = :pm4)";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("dep", d);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        List<Category> tmp = getCategoryFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        return tmp;

    }

    private List<Category> findCategory() {
        String sql;
        Map temMap = new HashMap();

        sql = "select distinct(bi.item.category) FROM BillItem bi where bi.bill.institution=:ins and bi.bill.billType= :bTp "
                + " and bi.bill.createdAt between :fromDate and :toDate and bi.item.department.institution=:ins2 "
                + " and ( bi.bill.paymentMethod = :pm1 or  bi.bill.paymentMethod = :pm2 or  bi.bill.paymentMethod = :pm3 or  bi.bill.paymentMethod = :pm4) order by bi.item.category.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("ins2", getSessionController().getInstitution());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        List<Category> tmp = getCategoryFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        return tmp;

    }

    private List<Item> findItem(Category d, Department dep) {
        String sql;
        Map temMap = new HashMap();
        if (d == null) {
            return new ArrayList<>();
        }
        sql = "select distinct(bi.item) FROM BillItem bi where bi.item.department=:dep and bi.bill.institution=:ins and  bi.bill.billType= :bTp  "
                + " and bi.item.category=:cat and  bi.bill.createdAt between :fromDate and :toDate "
                + "and ( bi.bill.paymentMethod = :pm1 or  bi.bill.paymentMethod = :pm2 "
                + " or  bi.bill.paymentMethod = :pm3 or  bi.bill.paymentMethod = :pm4)";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("dep", dep);
        temMap.put("cat", d);
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        List<Item> tmp = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    private List<Item> findItem(Category d) {
        String sql;
        Map temMap = new HashMap();
        if (d == null) {
            return new ArrayList<>();
        }
        sql = "select distinct(bi.item) FROM BillItem bi where  bi.bill.institution=:ins and  bi.bill.billType= :bTp  "
                + " and bi.item.category.id=" + d.getId() + " and  bi.bill.createdAt between :fromDate and :toDate "
                + "and ( bi.bill.paymentMethod = :pm1 or  bi.bill.paymentMethod = :pm2 "
                + " or  bi.bill.paymentMethod = :pm3 or  bi.bill.paymentMethod = :pm4)";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        List<Item> tmp = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    private List<Item> findItem() {
        String sql;
        Map temMap = new HashMap();

        sql = "select distinct(bi.item) FROM BillItem bi where  bi.item.institution=:ins and  bi.bill.billType= :bTp  "
                + " and bi.bill.createdAt between :fromDate and :toDate "
                + "and ( bi.bill.paymentMethod = :pm1 or  bi.bill.paymentMethod = :pm2 or  bi.bill.paymentMethod = :pm3 or  bi.bill.paymentMethod = :pm4)";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        List<Item> tmp = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    private long getCount(ItemWithFee i) {
        long billed, cancelled, refunded;
        billed = cancelled = refunded = 0l;

        billed = billItemForCount(new BilledBill(), i.getItem());
        cancelled = billItemForCount(new CancelledBill(), i.getItem());
        refunded = billItemForCount(new RefundBill(), i.getItem());

        return billed - (cancelled + refunded);

    }

    private long billItemForCount(Bill bill, Item i) {

        Map temMap = new HashMap();
        String sql;

        sql = "select count(bi) FROM BillItem bi where  bi.bill.institution=:ins and bi.item=:itm"
                + " and (bi.bill.paymentMethod = :pm1 or bi.bill.paymentMethod = :pm2 or bi.bill.paymentMethod = :pm3 or bi.bill.paymentMethod = :pm4) "
                + "and bi.bill.billType=:btp and type(bi.bill)=:billClass "
                + "and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("itm", i);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        temMap.put("billClass", bill.getClass());
        temMap.put("btp", BillType.OpdBill);

        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

//    private void setCount(ItemWithFee i) {
//        if (i.getItem() == null) {
//            return;
//        }
//        String sql;
//        double billed, cancelled, refunded;
//        billed = cancelled = refunded = 0.0;
//        Map temMap = new HashMap();
//        sql = "select bi FROM BillItem bi where  bi.bill.institution.id=" + getSessionController().getInstitution().getId() + " and bi.item.id=" + i.getItem().getId()
//                + " and (bi.bill.paymentMethod = :pm1 or bi.bill.paymentMethod = :pm2 or bi.bill.paymentMethod = :pm3 )    and type(bi.bill)=:bTp and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
//        temMap.put("toDate", getToDate());
//        temMap.put("fromDate", getFromDate());
//        temMap.put("pm1", PaymentMethod.Cash);
//        temMap.put("pm2", PaymentMethod.Card);
//        temMap.put("pm3", PaymentMethod.Cheque);
//        temMap.put("bTp", BilledBill.class);
//        List<BillItem> temps = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//        for (BillItem b : temps) {
//            billed++;
//        }
//
//        temMap.clear();
//
//
//        sql = "select bi FROM BillItem bi where  bi.bill.institution.id=" + getSessionController().getInstitution().getId() + " and bi.item.id=" + i.getItem().getId()
//                + " and (bi.bill.paymentMethod = :pm1 or bi.bill.paymentMethod = :pm2 or bi.bill.paymentMethod = :pm3 )    and type(bi.bill)=:bTp and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
//        temMap.put("toDate", getToDate());
//        temMap.put("fromDate", getFromDate());
//        temMap.put("pm1", PaymentMethod.Cash);
//        temMap.put("pm2", PaymentMethod.Card);
//        temMap.put("pm3", PaymentMethod.Cheque);
//        temMap.put("bTp", CancelledBill.class);
//        temps = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//        for (BillItem b : temps) {
//            cancelled++;
//        }
//
//        temMap.clear();
//
//        sql = "select bi FROM BillItem bi where  bi.bill.institution.id=" + getSessionController().getInstitution().getId() + " and bi.item.id=" + i.getItem().getId()
//                + " and (bi.bill.paymentMethod = :pm1 or bi.bill.paymentMethod = :pm2 or bi.bill.paymentMethod = :pm3 )    and type(bi.bill)=:bTp and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";
//        temMap.put("toDate", getToDate());
//        temMap.put("fromDate", getFromDate());
//        temMap.put("pm1", PaymentMethod.Cash);
//        temMap.put("pm2", PaymentMethod.Card);
//        temMap.put("pm3", PaymentMethod.Cheque);
//        temMap.put("bTp", RefundBill.class);
//        temps = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//        for (BillItem b : temps) {
//            refunded++;
//        }
//
//
//        i.setCount(billed - cancelled - refunded);
//
//
//    }
    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;

    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    private double getFee(ItemWithFee i, FeeType feeType) {
        String sql = "SELECT sum(bf.feeValue) FROM BillFee bf WHERE "
                + " bf.bill.billType=:bTp and bf.fee.feeType=:ftp "
                + " and bf.bill.institution=:ins and bf.bill.createdAt between :fromDate and :toDate "
                + "  and bf.billItem.item=:itm"
                + " and ( bf.bill.paymentMethod = :pm1 or  bf.bill.paymentMethod = :pm2"
                + " or  bf.bill.paymentMethod = :pm3 or  bf.bill.paymentMethod = :pm4)";

        HashMap temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("itm", i.getItem());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("ftp", feeType);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public double getProTotal() {
        double tmp = 0.0;
        for (DailyCash d : dailyCash) {
            tmp += d.getProfFee();
        }
        tmp += getPathProf();
        return tmp;
    }

    public double getPettyTot() {
        double tmp = 0.0;
        List<Bill> lstBills;

        lstBills = bills(BillType.PettyCash);

        for (Bill d : lstBills) {
            tmp += d.getNetTotal();
        }

        return tmp;
    }

    public List<DailyCash> getDailyCash() {
        // //////System.out.println("Starting : ");
        if (dailyCash == null) {
            dailyCash = new ArrayList<>();

            for (Department d : findDepartment()) {
                DailyCash tmp = new DailyCash();
                tmp.setDepartment(d);
                dailyCash.add(tmp);
            }

            for (DailyCash d : dailyCash) {
                List<CategoryWithItem> tmpCatList = new ArrayList<>();

                for (Category cat : findCategory(d.getDepartment())) {
                    CategoryWithItem n = new CategoryWithItem();
                    n.setCategory(cat);

                    List<ItemWithFee> tmpItemList = new ArrayList<>();

                    for (Item i : findItem(cat, d.getDepartment())) {
                        ItemWithFee iwf = new ItemWithFee();
                        iwf.setItem(i);
                        iwf.setCount(getCount(iwf));
                        // setCount(iwf);
                        iwf.setHospitalFee(getFee(iwf, FeeType.OwnInstitution));
                        iwf.setProFee(getFee(iwf, FeeType.Staff));

                        tmpItemList.add(iwf);

                    }

                    n.setItemWithFees(tmpItemList);
                    tmpCatList.add(n);
                }

                d.setCategoryWitmItems(tmpCatList);

            }

            calNonCashTot();
        }

        return dailyCash;
    }
    List<CategoryWithItem> categoryWithItem;

    public List<CategoryWithItem> getDailyCash2() {

        categoryWithItem = new ArrayList<>();

        for (Category cat : findCategory()) {
            CategoryWithItem n = new CategoryWithItem();
            n.setCategory(cat);

            List<ItemWithFee> tmpItemList = new ArrayList<>();

            for (Item i : findItem(cat)) {
                ItemWithFee iwf = new ItemWithFee();
                iwf.setItem(i);
                iwf.setCount(getCount(iwf));
                iwf.setHospitalFee(getFee(iwf, FeeType.OwnInstitution));
                iwf.setProFee(getFee(iwf, FeeType.Staff));
                tmpItemList.add(iwf);
            }
            if (!tmpItemList.isEmpty() && tmpItemList.get(0).getItem() != null && tmpItemList.get(0).getItem().getDepartment() != null) {
                n.setDepartment(tmpItemList.get(0).getItem().getDepartment());
                
            }
            n.setItemWithFees(tmpItemList);
            categoryWithItem.add(n);
        }

        calNonCashTot();

        return categoryWithItem;
    }

    private Item service;

    private List<BillItem> getBillItem() {
        String sql;
        Map temMap = new HashMap();

        sql = "select bi FROM BillItem bi where  bi.bill.institution=:ins and  bi.bill.billType= :bTp  "
                + " and  bi.bill.createdAt between :fromDate and :toDate and bi.item=:itm"
                + " and ( bi.bill.paymentMethod = :pm1 or  bi.bill.paymentMethod = :pm2 "
                + " or  bi.bill.paymentMethod = :pm3 or  bi.bill.paymentMethod = :pm4)";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        temMap.put("itm", getService());
        List<BillItem> tmp = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    private double calHospitalFee(BillItem bi) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeValue) from BillFee f where f.retired=false and f.billItem=:b and "
                + " f.fee.feeType=:ftp";
        hm.put("b", bi);
        hm.put("ftp", FeeType.OwnInstitution);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    public double getServiceTot() {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(bi.feeValue) FROM BillFee bi where  bi.bill.institution=:ins and  bi.bill.billType= :bTp  "
                + " and  bi.bill.createdAt between :fromDate and :toDate and bi.billItem.item=:itm"
                + " and ( bi.bill.paymentMethod = :pm1 or  bi.bill.paymentMethod = :pm2 "
                + " or  bi.bill.paymentMethod = :pm3 or  bi.bill.paymentMethod = :pm4)";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", BillType.OpdBill);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        temMap.put("itm", getService());
        //     List<BillItem> tmp = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public List<BillItemWithFee> getServiceSummery() {

        List<BillItemWithFee> tmp = new ArrayList<>();

        for (BillItem i : getBillItem()) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setHospitalFee(calHospitalFee(i));
            tmp.add(bi);
        }

        return tmp;
    }

    List<ItemWithFee> itemWithFees;

    public List<ItemWithFee> getDailyCash3() {

        itemWithFees = new ArrayList<>();

        for (Item i : findItem()) {
            ItemWithFee iwf = new ItemWithFee();
            iwf.setItem(i);
            iwf.setCount(getCount(iwf));
            iwf.setHospitalFee(getFee(iwf, FeeType.OwnInstitution));
            iwf.setProFee(getFee(iwf, FeeType.Staff));
            itemWithFees.add(iwf);
        }

        calNonCashTot();

        return itemWithFees;
    }

    private double getSumByFee(BillType billType, PaymentMethod paymentMethod) {
        String sql = "SELECT sum(bf.feeValue) FROM BillFee bf WHERE bf.billItem.bill.institution=:ins and  bf.billItem.bill.createdAt between :fromDate and :toDate  and "
                + " bf.billItem.bill.paymentMethod = :pm and bf.bill.billType=:btp";

        HashMap temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("pm", paymentMethod);
        temMap.put("btp", billType);
        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private double getSumByBill(BillType billType, PaymentMethod paymentMethod) {
        String sql = "SELECT sum(b.netTotal) FROM Bill b WHERE b.institution=:ins and  b.createdAt between :fromDate and :toDate  and "
                + " b.paymentMethod = :pm and b.billType=:btp";

        HashMap temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("pm", paymentMethod);
        temMap.put("btp", billType);
        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private void calNonCashTot() {
        cardTot = getSumByFee(BillType.OpdBill, PaymentMethod.Card);
        chequeTot = getSumByFee(BillType.OpdBill, PaymentMethod.Cheque);
        slipTot = getSumByFee(BillType.OpdBill, PaymentMethod.Slip);

        cardTot += getSumByFee(BillType.PaymentBill, PaymentMethod.Card);
        chequeTot += getSumByFee(BillType.PaymentBill, PaymentMethod.Cheque);
        slipTot += getSumByFee(BillType.PaymentBill, PaymentMethod.Slip);

        cardTot += getSumByBill(BillType.AgentPaymentReceiveBill, PaymentMethod.Card);
        chequeTot += getSumByBill(BillType.AgentPaymentReceiveBill, PaymentMethod.Cheque);
        slipTot += getSumByBill(BillType.AgentPaymentReceiveBill, PaymentMethod.Slip);

        cardTot += getSumByBill(BillType.CashRecieveBill, PaymentMethod.Card);
        chequeTot += getSumByBill(BillType.CashRecieveBill, PaymentMethod.Cheque);
        slipTot += getSumByBill(BillType.CashRecieveBill, PaymentMethod.Slip);

        cardTot += getSumByBill(BillType.PettyCash, PaymentMethod.Card);
        chequeTot += getSumByBill(BillType.PettyCash, PaymentMethod.Cheque);
        slipTot += getSumByBill(BillType.PettyCash, PaymentMethod.Slip);

    }

    public void setDailyCash(List<DailyCash> dailyCash) {
        this.dailyCash = dailyCash;
    }

    public CategoryFacade getCategoryFacade() {
        return categoryFacade;
    }

    public void setCategoryFacade(CategoryFacade categoryFacade) {
        this.categoryFacade = categoryFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public List<Bill> getInwardPayments() {
        inwardPayments = getBillBean().billsForTheDay(getFromDate(), getToDate(), BillType.InwardPaymentBill);

        return inwardPayments;
    }

    public void setInwardPayments(List<Bill> inwardPayments) {
        this.inwardPayments = inwardPayments;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public double getGrantTotal() {
        grantTotal = 0.0;
        for (DailyCash d : dailyCash) {
            grantTotal += d.getDepartmentTotal();
        }
        grantTotal += getPathTotal() + getCreditCompanyTotal() + getAgentCollectionTot() + inwardTot + getPharmacyTotal();

        return grantTotal;
    }

    public double getGrantTotal2() {
        grantTotal = 0.0;
        for (CategoryWithItem d : categoryWithItem) {
            grantTotal += d.getSubTotal();
        }
        grantTotal += getPathTotal() + getCreditCompanyTotal() + getAgentCollectionTot() + inwardTot + getPharmacyTotal();

        return grantTotal;
    }

    public double getGrantTotal2Hos() {
        grantTotal = 0.0;
        for (CategoryWithItem d : categoryWithItem) {
            grantTotal += d.getSubHosTotal();
        }
        grantTotal += getPathTotal() + getCreditCompanyTotal() + getAgentCollectionTot() + inwardTot + getPharmacyTotal();

        return grantTotal;
    }

    public double getGrantTotal3() {
        grantTotal = 0.0;
        for (ItemWithFee d : itemWithFees) {
            grantTotal += d.getTotal();
        }
        grantTotal += getPathTotal() + getCreditCompanyTotal() + getAgentCollectionTot() + inwardTot + getPharmacyTotal();

        return grantTotal;
    }

    public double getCategoryTotal() {
        double tmp = 0.0;
        for (CategoryWithItem d : getDailyCash2()) {
            tmp += d.getSubTotal();
        }

        return tmp;
    }

    public double getOpdHospitalTotal() {
        double tmp = 0.0;
        for (CategoryWithItem d : getDailyCash2()) {
            tmp += d.getSubHosTotal();
        }

        return tmp;
    }

    public double getDepartmentTotal() {
        double tmp = 0.0;
        for (DailyCash d : getDailyCash()) {
            tmp += d.getDepartmentTotal();
        }

        return tmp;
    }

    public void setGrantTotal(double grantTotal) {
        this.grantTotal = grantTotal;
    }

    public double getCreditCompanyTotal() {
        return creditCompanyTotal;
    }

    public void setCreditCompanyTotal(double creditCompanyTotal) {
        this.creditCompanyTotal = creditCompanyTotal;
    }

    public double getCardTot() {
        return cardTot;
    }

    public void setCardTot(double cardTot) {
        this.cardTot = cardTot;
    }

    public double getChequeTot() {
        return chequeTot;
    }

    public void setChequeTot(double chequeTot) {
        this.chequeTot = chequeTot;
    }

    public double getSlipTot() {
        return slipTot;
    }

    public void setSlipTot(double slipTot) {
        this.slipTot = slipTot;
    }

    public double getAgentCollectionTot() {
        return agentCollectionTot;
    }

    public void setAgentCollectionTot(double agentCollectionTot) {
        this.agentCollectionTot = agentCollectionTot;
    }

    public double getDoctorPaymentTot() {
        return doctorPaymentTot;
    }

    public void setDoctorPaymentTot(double doctorPaymentTot) {
        this.doctorPaymentTot = doctorPaymentTot;
    }

    public AdmissionTypeController getAdmissionTypeController() {
        return admissionTypeController;
    }

    public void setAdmissionTypeController(AdmissionTypeController admissionTypeController) {
        this.admissionTypeController = admissionTypeController;
    }

    public double getInwardTot() {
        return inwardTot;
    }

    public void setInwardTot(double inwardTot) {
        this.inwardTot = inwardTot;
    }

    public double getInwardProfTot() {
        return inwardProfTot;
    }

    public void setInwardProfTot(double inwardProfTot) {
        this.inwardProfTot = inwardProfTot;
    }

    public Item getService() {
        return service;
    }

    public void setService(Item service) {
        this.service = service;
    }

    public double getPharmacyTotal() {
        return pharmacyTotal;
    }

    public void setPharmacyTotal(double pharmacyTotal) {
        this.pharmacyTotal = pharmacyTotal;
    }

}
