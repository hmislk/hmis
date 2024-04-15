/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.report;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.CategoryController;
import com.divudi.bean.common.DepartmentController;
import com.divudi.bean.inward.AdmissionTypeController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.AdmissionTypeBills;
import com.divudi.data.dataStructure.BillsItems;
import com.divudi.data.dataStructure.DailyCash;
import com.divudi.data.dataStructure.DepartmentPayment;
import com.divudi.data.dataStructure.ItemWithFee;
import com.divudi.data.table.String1Value1;
import com.divudi.data.table.String1Value2;
import com.divudi.data.table.String1Value3;
import com.divudi.data.table.String6;

import com.divudi.entity.Bill;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
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
import java.util.List;
import java.util.Map;
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
public class CashSummeryControllerExcel1 implements Serializable {

    private Institution institution;
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
    private List<Bill> inwardPayments;
    private List<ItemWithFee> itemWithFees;
    List<String1Value3> otherInstitution;
    @Inject
    private BillBeanController billBean;
    private Item service;
    private double doctorPaymentTot = 0.0;
    private List<DepartmentPayment> departmentPayments;
    private double agentCollectionTot;
    private List<Bill> agentCollections;
    private List<BillsItems> creditCompanyCollections;
    private double creditCompanyTotal = 0.0;
    private double grantTotal;
    private double cardTot;
    private double chequeTot;
    private double slipTot;
    private double inwardTot;
    private double inwardProfTot;
    private double otherProfessionalTotal;
    private double otherHospitalTotal;
    private List<Bill> cardBill;
    private List<Bill> slipBill;
    private List<Bill> chequeBill;
    private List<DailyCash> pharmacySales;
    List<String6> string6s;
    private double pharmacyTotal;
    @Inject
    private AdmissionTypeController admissionTypeController;
    private List<String1Value1> finalSumery;
    private List<String1Value2> string1Value2s;
    private List<String1Value1> collections2Hos;
    private List<String1Value1> inwardProfessions;
    private List<AdmissionTypeBills> admissionTypeBillses;
    @Inject
    private CategoryController categoryController;

    public List<String6> getString6s() {
        if (string6s == null) {
            string6s = new ArrayList<>();
        }
        return string6s;
    }

    public void setString6s(List<String6> string6s) {
        this.string6s = string6s;
    }

    public long getCountTotal() {
        long countTotal = 0;

        long billed = getCount(new BilledBill());
        long cancelled = getCount(new CancelledBill());
        long refunded = getCount(new RefundBill());

        countTotal = billed - (refunded + cancelled);
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
        temMap.put("ins", getInstitution());
        return getBillItemFacade().countByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void makeNull() {

    }

    private void createFinalSummery() {
        finalSumery = new ArrayList<>();
        String1Value1 dd;
        dd = new String1Value1();
        dd.setString("Net Cash");
        dd.setValue(getNetCash());
        finalSumery.add(dd);

        dd = new String1Value1();
        dd.setString("Lab Handover Total");

        finalSumery.add(dd);

        dd = new String1Value1();
        dd.setString("Final Cash");

        finalSumery.add(dd);

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

    private void createCollections2Hos() {
        collections2Hos = new ArrayList<>();
        String1Value1 dd;
        dd = new String1Value1();
        dd.setString("Collection For the Day");
        dd.setValue(getGrantTotal2Hos());
        collections2Hos.add(dd);

        dd = new String1Value1();
        dd.setString("Petty cash Payments");
        dd.setValue(getPettyTot());
        collections2Hos.add(dd);

    }

    private void createInwardProfessions() {
        inwardProfTot = 0.0;
        inwardProfessions = new ArrayList<>();

        for (AdmissionType at : getAdmissionTypeController().getItems()) {
            AdmissionTypeBills admB = new AdmissionTypeBills();
            admB.setAdmissionType(at);
            admB.setTotal(getInwardProfTot(at));
            inwardProfTot += admB.getTotal();

            if (admB.getTotal() != 0) {
                String1Value1 dd;
                dd = new String1Value1();
                dd.setString(admB.getAdmissionType().getName());
                dd.setValue(admB.getTotal());
                inwardProfessions.add(dd);
            }
        }

    }

    private void createPharmacySale() {
        pharmacySales = new ArrayList<>();
        pharmacyTotal = 0;

        getDepartmentController().setInstitution(institution);

        for (Department d : getDepartmentController().getInstitutionDepatrments()) {
            //System.err.println("DEP " + d.getName());
            double netTotal = getBillBean().calDepartmentSale(d, getFromDate(), getToDate());
            if (netTotal != 0) {
                //System.err.println("NET " + netTotal);
                pharmacyTotal += netTotal;
                DailyCash dl = new DailyCash();
                dl.setDepartment(d);
                dl.setDepartmentTotal(netTotal);
                pharmacySales.add(dl);
            }

        }

    }

    private void createOtherInstituion() {
        otherHospitalTotal = 0;
        otherProfessionalTotal = 0;
        otherInstitution = new ArrayList<>();
        double totaltFee = getBillBean().calOutSideInstitutionFees(fromDate, toDate, institution);
        double proTotal = getOtherInstitutionFees(FeeType.Staff);
        if ((totaltFee) != 0) {
            String1Value3 tmp = new String1Value3();
            tmp.setString("Outer Institution");
            tmp.setValue1(totaltFee - proTotal);
            tmp.setValue2(proTotal);
            tmp.setValue3(totaltFee);

            otherInstitution.add(tmp);

            otherHospitalTotal = totaltFee - proTotal;
            otherProfessionalTotal = proTotal;
        }
    }

    public List<String1Value3> getOtherInstitution() {
        return otherInstitution;
    }

    public void setOtherInstitution(List<String1Value3> otherInstitution) {
        this.otherInstitution = otherInstitution;
    }

    public List<ItemWithFee> getItemWithFees() {
        return itemWithFees;
    }

    public void setItemWithFees(List<ItemWithFee> itemWithFees) {
        this.itemWithFees = itemWithFees;
    }

    public double getNetCash() {
        double tmp = grantTotal - doctorPaymentTot + getPettyTot() - cardTot - slipTot - chequeTot - inwardProfTot;

        return tmp;
    }

    private void createCardBill() {
        cardTot = getBillBean().calBillTotal(PaymentMethod.Card, getFromDate(), getToDate(), getInstitution());
        cardBill = getBillBean().fetchBills(PaymentMethod.Card, getFromDate(), getToDate(), getInstitution());

    }

    private void createSlipBill() {
        slipTot = getBillBean().calBillTotal(PaymentMethod.Slip, getFromDate(), getToDate(), getInstitution());
        slipBill = getBillBean().fetchBills(PaymentMethod.Slip, getFromDate(), getToDate(), getInstitution());

    }

    private void createChequeBill() {
        chequeTot = getBillBean().calBillTotal(PaymentMethod.Cheque, getFromDate(), getToDate(), getInstitution());
        chequeBill = getBillBean().fetchBills(PaymentMethod.Cheque, getFromDate(), getToDate(), getInstitution());

    }

    public double getOtherInstitutionFees(FeeType feeType) {
        String sql = "SELECT sum(bf.feeValue) "
                + " FROM BillFee bf"
                + " WHERE bf.billItem.bill.institution=:ins "
                + " and bf.billItem.item.institution!=:ins "
                + " and bf.fee.feeType=:ftp "
                + " and bf.createdAt between :fromDate and :toDate "
                + " and (bf.billItem.bill.paymentMethod = :pm1 "
                + " or bf.billItem.bill.paymentMethod = :pm2 "
                + " or bf.billItem.bill.paymentMethod = :pm3 "
                + " or bf.billItem.bill.paymentMethod = :pm4)";

        HashMap temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getInstitution());
        temMap.put("ftp", feeType);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        double val = getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return val;
    }

    public double getOtherInstitutionNotStaffFee(FeeType feeType) {
        String sql = "SELECT sum(bf.feeValue) "
                + " FROM BillFee bf"
                + " WHERE bf.billItem.bill.institution=:ins "
                + " and bf.billItem.item.institution!=:ins "
                + " and bf.fee.feeType!=:ftp "
                + " and bf.createdAt between :fromDate and :toDate "
                + " and (bf.billItem.bill.paymentMethod = :pm1 "
                + " or bf.billItem.bill.paymentMethod = :pm2 "
                + " or bf.billItem.bill.paymentMethod = :pm3 "
                + " or bf.billItem.bill.paymentMethod = :pm4)";

        HashMap temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getInstitution());
        temMap.put("ftp", feeType);
        temMap.put("pm1", PaymentMethod.Cash);
        temMap.put("pm2", PaymentMethod.Card);
        temMap.put("pm3", PaymentMethod.Cheque);
        temMap.put("pm4", PaymentMethod.Slip);
        double val = getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return val;
    }

    private void createCreditCompanyCollection() {
        creditCompanyTotal = 0.0;
        creditCompanyCollections = new ArrayList<>();
        List<Bill> tmp = getBillBean().fetchBills(BillType.CashRecieveBill, getFromDate(), getToDate(), getInstitution());
        creditCompanyTotal = getBillBean().calBillTotal(BillType.CashRecieveBill, getFromDate(), getToDate(), getInstitution());
        for (Bill b : tmp) {

            BillsItems newB = new BillsItems();
            newB.setBill(b);
            newB.setBillItems(getBillBean().fetchBillItems(b, getFromDate(), getToDate(), getInstitution()));

            creditCompanyCollections.add(newB);
        }

    }

    private void createAgentCollection() {
        agentCollectionTot = 0.0;

        agentCollections = getBillBean().fetchBills(BillType.AgentPaymentReceiveBill, getFromDate(), getToDate(), getInstitution());

        agentCollectionTot = getBillBean().calBillTotal(BillType.AgentPaymentReceiveBill, getFromDate(), getToDate(), getInstitution());

    }

//    public void createInwardCollection() {
//        System.err.println("createInwardCollection");
//        inwardTot = 0.0;
//        admissionTypeBillses = new ArrayList<>();
//        for (AdmissionType at : getAdmissionTypeController().getItems()) {
//            AdmissionTypeBills admB = new AdmissionTypeBills();
//            admB.setAdmissionType(at);
//            admB.setBills(getInwardBills(at));
//            admB.setTotal(getInwardBillTotal(at));
//            inwardTot += admB.getTotal();
//            admissionTypeBillses.add(admB);
//        }
//    }
//    public void createInwardCollection() {
//        System.err.println("createInwardCollection");
//        inwardTot = 0.0;
//        admissionTypeBillses = new ArrayList<>();
//        for (AdmissionType at : getAdmissionTypeController().getItems()) {
//            AdmissionTypeBills admB = new AdmissionTypeBills();
//            admB.setAdmissionType(at);
//            admB.setBills(getBillBean().fetchInwardPaymentBills(at, fromDate, toDate, institution));
//            admB.setTotal(getBillBean().calInwardPaymentTotal1(at, fromDate, toDate, institution));
//            inwardTot += admB.getTotal();
//            admissionTypeBillses.add(admB);
//        }
//    }

    public List<AdmissionTypeBills> getInwardCollection() {

        return admissionTypeBillses;
    }

//    public List<String2Value1> getInwardCollection(){
//        for(AdmissionTypeBills adm:)
//    
//    }
    public double getInwardProfTot(AdmissionType adt) {

        HashMap temMap = new HashMap();

        String sql = "SELECT sum(b.netValue)"
                + " FROM BillItem b WHERE "
                + " b.bill.billType=:btp"
                + " and (b.referenceBill.billType=:refBtp1 "
                + " or b.referenceBill.billType=:refBtp2 )"
                + " and b.referenceBill.patientEncounter.admissionType=:admis"
                + " and b.retired=false "
                + " and b.bill.createdAt between :fromDate and :toDate";

        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", BillType.PaymentBill);
        temMap.put("refBtp1", BillType.InwardBill);
        temMap.put("refBtp2", BillType.InwardProfessional);
        temMap.put("admis", adt);
        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<Department> getDepartmentList() {
        String sql = "SELECT distinct(b.referanceBillItem.item.department)"
                + " FROM BillItem b WHERE "
                + " b.bill.institution=:ins"
                + "  and b.retired=false "
                + " and b.bill.billType= :btp "
                + " and b.createdAt between :fromDate and :toDate";
        HashMap temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", BillType.PaymentBill);
        temMap.put("ins", getInstitution());

        return getDepartmentFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private double getDepartmentPaymentTotal(Department department) {
        String sql = "SELECT sum(b.netValue)"
                + " FROM BillItem b WHERE "
                + " b.bill.institution=:ins"
                + "  and b.retired=false "
                + "  and b.referanceBillItem.item.department=:dep "
                + " and b.bill.billType= :btp "
                + " and b.createdAt between :fromDate and :toDate";
        HashMap temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("dep", department);
        temMap.put("btp", BillType.PaymentBill);
        temMap.put("ins", getInstitution());

        return getDepartmentFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private double getDepartmentPaymentTotal() {
        String sql = "SELECT sum(b.netValue)"
                + " FROM BillItem b WHERE "
                + " b.bill.institution=:ins"
                + "  and b.retired=false "
                + " and b.bill.billType= :btp "
                + " and b.createdAt between :fromDate and :toDate";
        HashMap temMap = new HashMap();
        temMap.put("fromDate", getFromDate());
        temMap.put("toDate", getToDate());
        temMap.put("btp", BillType.PaymentBill);
        temMap.put("ins", getInstitution());

        return getDepartmentFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public void createDepartmentPayment() {
        doctorPaymentTot = 0.0;

        List<Department> depList = getDepartmentList();

        departmentPayments = new ArrayList<>();

        for (Department dep : depList) {
            DepartmentPayment dp = new DepartmentPayment();
            dp.setDepartment(dep);

            double tot = 0.0;

            dp.setTotalPayment(getDepartmentPaymentTotal(dep));
            departmentPayments.add(dp);
        }

        doctorPaymentTot = getDepartmentPaymentTotal();
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

    public double getPettyTot() {

        double tmp = getBillBean().calBillTotal(BillType.PettyCash, getFromDate(), getToDate(), getInstitution());

        return tmp;
    }

    @Inject
    DepartmentController departmentController;

    public DepartmentController getDepartmentController() {
        return departmentController;
    }

    public void setDepartmentController(DepartmentController departmentController) {
        this.departmentController = departmentController;
    }

//    public void createCashCategoryWithoutPro() {
//        long lng = getCommonFunctions().getDayCount(getFromDate(), getToDate());
//
//        if (Math.abs(lng) > 2) {
//            JsfUtil.addErrorMessage("Date Range is too Long");
//            return;
//        }
//
//        createOPdCategoryTable();
//        createOtherInstituion();
//        createPharmacySale();
//        createInwardCollection();
//        createAgentCollection();
//        createCreditCompanyCollection();
//        createCollections2Hos();
//        createDepartmentPayment();
//        createInwardProfessions();
//        createCardBill();
//        createChequeBill();
//        createSlipBill();
//        createFinalSummery();
//
//    }

    public void createOPdCategoryTable() {
        string1Value2s = new ArrayList<>();
        for (Category cat : getBillBean().fetchBilledOpdCategory(fromDate, toDate, institution)) {
            for (Item i : getBillBean().fetchBilledOpdItem(cat, fromDate, toDate, institution)) {
                //   System.err.println("Item " + i.getName() + " TIME " + new Date());
                double count = getBillBean().calBilledItemCount(i, getFromDate(), getToDate(), getInstitution());
                double hos = getBillBean().calFeeValue(i, FeeType.OwnInstitution, getFromDate(), getToDate(), getInstitution());
                //     double pro = getFee(i, FeeType.Staff);

                if (count != 0) {
                    String1Value2 newD = new String1Value2();
                    newD.setString(i.getName());
                    newD.setValue1(count);
                    newD.setValue2(hos);
                    newD.setSummery(false);
                    string1Value2s.add(newD);
                }
            }

            double catDbl = getBillBean().calFeeValue(cat, FeeType.OwnInstitution, getFromDate(), getToDate(), getInstitution());

            if (catDbl != 0) {
                String1Value2 newD = new String1Value2();
                newD.setString(cat.getName() + " Total : ");
                newD.setValue2(catDbl);
                newD.setSummery(true);
                string1Value2s.add(newD);
            }

        }

    }

    public List<String1Value2> getDailyCashExcel() {
        return string1Value2s;
    }

    private double getSumByBill(PaymentMethod paymentMethod) {
        String sql = "SELECT sum(b.netTotal) FROM Bill b WHERE"
                + "  b.institution=:ins "
                + " and  b.createdAt between :fromDate and :toDate"
                + " and  b.paymentMethod = :pm "
                + " and b.billType=:btp1 "
                + " and b.billType=:btp2"
                + " and b.billType=:btp3"
                + " and b.billType=:btp4"
                + " and b.billType=:btp5";

        HashMap temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getInstitution());
        temMap.put("pm", paymentMethod);
        temMap.put("btp1", BillType.OpdBill);
        temMap.put("btp2", BillType.PaymentBill);
        temMap.put("btp3", BillType.AgentPaymentReceiveBill);
        temMap.put("btp4", BillType.CashRecieveBill);
        temMap.put("btp5", BillType.PettyCash);
        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private void calNonCashTot() {
        cardTot = getSumByBill(PaymentMethod.Card);
        chequeTot = getSumByBill(PaymentMethod.Cheque);
        slipTot = getSumByBill(PaymentMethod.Slip);

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

    public void setInwardPayments(List<Bill> inwardPayments) {
        this.inwardPayments = inwardPayments;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public double getGrantTotal2Hos() {
        grantTotal = 0.0;

        grantTotal = getOtherProfessionalTotal()
                + getOtherHospitalTotal()
                + getOpdHospitalTotal()
                + getCreditCompanyTotal()
                + getAgentCollectionTot()
                + getInwardTot()
                + getPharmacyTotal();

        return grantTotal;
    }

    public double getOpdHospitalTotal() {
        double tmp = getBillBean().calFeeValue(FeeType.OwnInstitution, getFromDate(), getToDate(), getInstitution());

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

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<String1Value2> getString1Value2s() {
        return string1Value2s;
    }

    public void setString1Value2s(List<String1Value2> string1Value2s) {
        this.string1Value2s = string1Value2s;
    }

    public List<AdmissionTypeBills> getAdmissionTypeBillses() {
        return admissionTypeBillses;
    }

    public void setAdmissionTypeBillses(List<AdmissionTypeBills> admissionTypeBillses) {
        this.admissionTypeBillses = admissionTypeBillses;
    }

    public List<DailyCash> getPharmacySales() {
        return pharmacySales;
    }

    public void setPharmacySales(List<DailyCash> pharmacySales) {
        this.pharmacySales = pharmacySales;
    }

    public List<BillsItems> getCreditCompanyCollections() {
        return creditCompanyCollections;
    }

    public void setCreditCompanyCollections(List<BillsItems> creditCompanyCollections) {
        this.creditCompanyCollections = creditCompanyCollections;
    }

    public List<Bill> getAgentCollections() {
        return agentCollections;
    }

    public void setAgentCollections(List<Bill> agentCollections) {
        this.agentCollections = agentCollections;
    }

    public List<String1Value1> getCollections2Hos() {
        return collections2Hos;
    }

    public void setCollections2Hos(List<String1Value1> collections2Hos) {
        this.collections2Hos = collections2Hos;
    }

    public List<DepartmentPayment> getDepartmentPayments() {
        return departmentPayments;
    }

    public void setDepartmentPayments(List<DepartmentPayment> departmentPayments) {
        this.departmentPayments = departmentPayments;
    }

    public List<String1Value1> getInwardProfessions() {
        return inwardProfessions;
    }

    public void setInwardProfessions(List<String1Value1> inwardProfessions) {
        this.inwardProfessions = inwardProfessions;
    }

    public List<String1Value1> getFinalSumery() {
        return finalSumery;
    }

    public void setFinalSumery(List<String1Value1> finalSumery) {
        this.finalSumery = finalSumery;
    }

    public List<Bill> getCardBill() {
        return cardBill;
    }

    public void setCardBill(List<Bill> cardBill) {
        this.cardBill = cardBill;
    }

    public List<Bill> getSlipBill() {
        return slipBill;
    }

    public void setSlipBill(List<Bill> slipBill) {
        this.slipBill = slipBill;
    }

    public List<Bill> getChequeBill() {
        return chequeBill;
    }

    public void setChequeBill(List<Bill> chequeBill) {
        this.chequeBill = chequeBill;
    }

    public List<Bill> getInwardPayments() {
        return inwardPayments;
    }

    public double getGrantTotal() {
        return grantTotal;
    }

    public double getOtherProfessionalTotal() {
        return otherProfessionalTotal;
    }

    public void setOtherProfessionalTotal(double otherProfessionalTotal) {
        this.otherProfessionalTotal = otherProfessionalTotal;
    }

    public double getOtherHospitalTotal() {
        return otherHospitalTotal;
    }

    public void setOtherHospitalTotal(double otherHospitalTotal) {
        this.otherHospitalTotal = otherHospitalTotal;
    }

    public CategoryController getCategoryController() {
        return categoryController;
    }

    public void setCategoryController(CategoryController categoryController) {
        this.categoryController = categoryController;
    }

}
