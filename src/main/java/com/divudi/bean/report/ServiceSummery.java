/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.report;

import com.divudi.bean.common.ServiceSubCategoryController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.BillItemWithFee;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceCategory;
import com.divudi.entity.ServiceSubCategory;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class ServiceSummery implements Serializable {

    @Inject
    private SessionController sessionController;
    // private List<DailyCash> dailyCashs;
    @Temporal(TemporalType.TIMESTAMP)
    private Date fromDate;
    @Temporal(TemporalType.TIMESTAMP)
    private Date toDate;
    private Item service;
    private Category category;
    double count;
    double value;
    private double proFeeTotal;
    private double hosFeeTotal;
    double hosFeeGrossValueTotal;
    double hosFeeDisTotal;
    double hosFeeMarginTotal;
    private double outSideFeeTotoal;
    double outSideFeeGrossTotal;
    double outSideFeeDiscountTotal;
    double outSideFeeMarginTotal;
    double reagentFeeTotal;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    List<BillItem> billItems;

    public double getCount() {
        return count;
    }

    public void setCount(double count) {
        this.count = count;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    /**
     * Creates a new instance of ServiceSummery
     */
    public ServiceSummery() {
    }

    public double calServiceTot(BillType billType, FeeType feeType, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(bi.feeValue) FROM BillFee bi "
                + " where  bi.bill.institution=:ins"
                + " and  bi.bill.billType= :bTp "
                + " and bi.fee.feeType=:ftp "
                + " and bi.billItem.item=:itm ";

        if (discharged) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and bi.bill.createdAt between :fromDate and :toDate ";
        }

        if (billType != BillType.InwardBill) {
            sql += " and ( bi.bill.paymentMethod = :pm1 "
                    + " or  bi.bill.paymentMethod = :pm2 "
                    + " or  bi.bill.paymentMethod = :pm3"
                    + " or  bi.bill.paymentMethod = :pm4) ";
            temMap.put("pm1", PaymentMethod.Cash);
            temMap.put("pm2", PaymentMethod.Card);
            temMap.put("pm3", PaymentMethod.Cheque);
            temMap.put("pm4", PaymentMethod.Slip);

        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);
        temMap.put("ftp", feeType);
        temMap.put("itm", getService());
        //     List<BillItem> tmp = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }
    
    public double calServiceTot(BillType billType,Item item, FeeType feeType,Department department,PaymentMethod paymentMethod, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(bi.feeValue) FROM BillFee bi "
                + " where  bi.bill.institution=:ins"
                + " and  bi.bill.billType= :bTp "
                + " and bi.fee.feeType=:ftp ";
        
        if (item != null) {
            sql += " and bi.item=:itm ";
            temMap.put("itm", item);
        }
        
        if (department != null) {
            sql += " and bi.bill.department=:dep ";
            temMap.put("dep", department);
        }

        if (paymentMethod != null) {
            sql += " and bi.bill.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (discharged) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and bi.bill.createdAt between :fromDate and :toDate ";
        }

        

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);
        temMap.put("ftp", feeType);
        //     List<BillItem> tmp = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        
        System.out.println("sql = " + sql);

        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }
    
    public List<BillFee> createBillFees(BillType billType,Item item, FeeType feeType,Department department,PaymentMethod paymentMethod, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bi FROM BillFee bi "
                + " where  bi.bill.institution=:ins"
                + " and  bi.bill.billType= :bTp "
                + " and bi.fee.feeType=:ftp ";
        
        if (item != null) {
            sql += " and bi.item=:itm ";
            temMap.put("itm", item);
        }
        
        if (department != null) {
            sql += " and bi.bill.department=:dep ";
            temMap.put("dep", department);
        }

        if (paymentMethod != null) {
            sql += " and bi.bill.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (discharged) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and bi.bill.createdAt between :fromDate and :toDate ";
        }

        

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);
        temMap.put("ftp", feeType);
        //     List<BillItem> tmp = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        
        System.out.println("sql = " + sql);

        return getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    private long calCount(Bill bill, BillType billType, boolean discharged) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi "
                + " where bi.bill.billType=:bType "
                + " and bi.item=:itm "
                + " and type(bi.bill)=:billClass "
                + " and bi.bill.toInstitution=:ins ";

        if (billType != BillType.InwardBill) {
            sql += " and ( bi.bill.paymentMethod = :pm1 "
                    + " or  bi.bill.paymentMethod = :pm2 "
                    + " or  bi.bill.paymentMethod = :pm3"
                    + " or  bi.bill.paymentMethod = :pm4) ";
            temMap.put("pm1", PaymentMethod.Cash);
            temMap.put("pm2", PaymentMethod.Card);
            temMap.put("pm3", PaymentMethod.Cheque);
            temMap.put("pm4", PaymentMethod.Slip);

        }

        if (discharged) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and bi.bill.createdAt between :fromDate and :toDate ";
        }

        sql += " order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("itm", getService());
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", billType);

        temMap.put("ins", getSessionController().getInstitution());
        return getBillItemFacade().countBySql(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void calCountTotalItem(BillType billType, boolean discharged) {
        count = 0;

        long billed = calCount(new BilledBill(), billType, discharged);
        long cancelled = calCount(new CancelledBill(), billType, discharged);
        long refunded = calCount(new RefundBill(), billType, discharged);

        count = billed - (refunded + cancelled);

    }

    private List<BillItem> getBillItem(BillType billType, Item item, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bi FROM BillItem bi "
                + " where  bi.bill.institution=:ins "
                + " and  bi.bill.billType= :bTp  "
                + " and bi.item=:itm ";

        if (billType != BillType.InwardBill) {
            sql += " and ( bi.bill.paymentMethod = :pm1 "
                    + " or  bi.bill.paymentMethod = :pm2 "
                    + " or  bi.bill.paymentMethod = :pm3"
                    + " or  bi.bill.paymentMethod = :pm4) ";

            temMap.put("pm1", PaymentMethod.Cash);
            temMap.put("pm2", PaymentMethod.Card);
            temMap.put("pm3", PaymentMethod.Cheque);
            temMap.put("pm4", PaymentMethod.Slip);

        }

        if (discharged) {
            sql += " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and  bi.bill.createdAt between :fromDate and :toDate ";
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);
        temMap.put("itm", item);
        List<BillItem> tmp = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    private List<BillItem> getBillItem(BillType billType, Item item, Department department, PaymentMethod paymentMethod, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bi FROM BillItem bi "
                + " where  bi.bill.institution=:ins "
                + " and  bi.bill.billType= :bTp  ";

        if (item != null) {
            sql += " and bi.item=:itm ";
            temMap.put("itm", item);
        }

        if (department != null) {
            sql += " and bi.bill.department=:dep ";
            temMap.put("dep", department);
        }

        if (paymentMethod != null) {
            sql += " and bi.bill.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (discharged) {
            sql += " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and  bi.bill.createdAt between :fromDate and :toDate ";
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);

        List<BillItem> tmp = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        return tmp;

    }

    public void createServiceSummery() {
        serviceSummery = new ArrayList<>();
        for (BillItem i : getBillItem(BillType.OpdBill, service, false)) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            serviceSummery.add(bi);
        }

        calCountTotalItem(BillType.OpdBill, false);
        proFeeTotal = calServiceTot(BillType.OpdBill, FeeType.Staff, false);
        hosFeeTotal = calServiceTot(BillType.OpdBill, FeeType.OwnInstitution, false);
        outSideFeeTotoal = calServiceTot(BillType.OpdBill, FeeType.OtherInstitution, false);

    }

    Department department;
    PaymentMethod paymentMethod;
    
    @EJB
    CommonFunctions commonFunctions;

    public void createServiceSummeryLab() {
        
         long lng = commonFunctions.getDayCount(getFromDate(), getToDate());

        if (Math.abs(lng) > 2) {
            UtilityController.addErrorMessage("Date Range is too Long");
            return;
        }
        
        
        serviceSummery = new ArrayList<>();
        for (BillItem i : getBillItem(BillType.OpdBill, service, department, paymentMethod, false)) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setReagentFee(calFee(i, FeeType.Chemical));
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            System.out.println("bi = " + bi);
            serviceSummery.add(bi);
        }

//        calCountTotalItem(BillType.OpdBill, false);
//        System.out.println("proFeeTotal = " + proFeeTotal);
//        System.out.println("hosFeeTotal = " + hosFeeTotal);
//        System.out.println("outSideFeeTotoal = " + outSideFeeTotoal);
//        System.out.println("reagentFeeTotal = " + reagentFeeTotal);
        proFeeTotal = calServiceTot(BillType.OpdBill, service, FeeType.Staff, department, paymentMethod, false);
        hosFeeTotal = calServiceTot(BillType.OpdBill, service, FeeType.OwnInstitution, department, paymentMethod, false);
        outSideFeeTotoal = calServiceTot(BillType.OpdBill, service, FeeType.OtherInstitution, department, paymentMethod, false);
        reagentFeeTotal=calServiceTot(BillType.OpdBill, service, FeeType.Chemical, department, paymentMethod, false);
//        List<BillFee> billfees =new ArrayList<>();
//        billfees=createBillFees(BillType.OpdBill, service, FeeType.Staff, department, paymentMethod, false);
//        for (BillFee bf : billfees) {
//            proFeeTotal+=bf.getFeeValue();
//            System.out.println("bf.getFeeValue = " + bf.getFeeValue());
//            System.out.println("proFeeTotal = " + proFeeTotal);
//            System.out.println("date = " + bf.getBill().getCreatedAt());
//        }
//        billfees=createBillFees(BillType.OpdBill, service, FeeType.OwnInstitution, department, paymentMethod, false);
//        for (BillFee bf : billfees) {
//            hosFeeTotal+=bf.getFeeValue();
//            System.out.println("hosFeeTotal = " + hosFeeTotal);
//        }
//        billfees=createBillFees(BillType.OpdBill, service, FeeType.OtherInstitution, department, paymentMethod, false);
//        for (BillFee bf : billfees) {
//            outSideFeeTotoal+=bf.getFeeValue();
//            System.out.println("outSideFeeTotoal = " + outSideFeeTotoal);
//        }
//        billfees=createBillFees(BillType.OpdBill, service, FeeType.Chemical, department, paymentMethod, false);
//        for (BillFee bf : billfees) {
//            reagentFeeTotal+=bf.getFeeValue();
//            System.out.println("reagentFeeTotal = " + reagentFeeTotal);
//        }
        
    }

    public void createServiceSummeryLabNew() {
        
         long lng = commonFunctions.getDayCount(getFromDate(), getToDate());

        if (Math.abs(lng) > 2) {
            UtilityController.addErrorMessage("Date Range is too Long");
            return;
        }
        
        
        serviceSummery = new ArrayList<>();
        for (BillItem i : getBillItem(BillType.OpdBill, service, department, paymentMethod, false)) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setReagentFee(calFee(i, FeeType.Chemical));
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            System.out.println("bi = " + bi);
            serviceSummery.add(bi);
        }

//        calCountTotalItem(BillType.OpdBill, false);
//        System.out.println("proFeeTotal = " + proFeeTotal);
//        System.out.println("hosFeeTotal = " + hosFeeTotal);
//        System.out.println("outSideFeeTotoal = " + outSideFeeTotoal);
//        System.out.println("reagentFeeTotal = " + reagentFeeTotal);
        proFeeTotal = calServiceTot(BillType.OpdBill, service, FeeType.Staff, department, paymentMethod, false);
        hosFeeTotal = calServiceTot(BillType.OpdBill, service, FeeType.OwnInstitution, department, paymentMethod, false);
        outSideFeeTotoal = calServiceTot(BillType.OpdBill, service, FeeType.OtherInstitution, department, paymentMethod, false);
        reagentFeeTotal=calServiceTot(BillType.OpdBill, service, FeeType.Chemical, department, paymentMethod, false);
//        List<BillFee> billfees =new ArrayList<>();
//        billfees=createBillFees(BillType.OpdBill, service, FeeType.Staff, department, paymentMethod, false);
//        for (BillFee bf : billfees) {
//            proFeeTotal+=bf.getFeeValue();
//            System.out.println("bf.getFeeValue = " + bf.getFeeValue());
//            System.out.println("proFeeTotal = " + proFeeTotal);
//            System.out.println("date = " + bf.getBill().getCreatedAt());
//        }
//        billfees=createBillFees(BillType.OpdBill, service, FeeType.OwnInstitution, department, paymentMethod, false);
//        for (BillFee bf : billfees) {
//            hosFeeTotal+=bf.getFeeValue();
//            System.out.println("hosFeeTotal = " + hosFeeTotal);
//        }
//        billfees=createBillFees(BillType.OpdBill, service, FeeType.OtherInstitution, department, paymentMethod, false);
//        for (BillFee bf : billfees) {
//            outSideFeeTotoal+=bf.getFeeValue();
//            System.out.println("outSideFeeTotoal = " + outSideFeeTotoal);
//        }
//        billfees=createBillFees(BillType.OpdBill, service, FeeType.Chemical, department, paymentMethod, false);
//        for (BillFee bf : billfees) {
//            reagentFeeTotal+=bf.getFeeValue();
//            System.out.println("reagentFeeTotal = " + reagentFeeTotal);
//        }
        
    }

    
    public void createServiceSummeryInwardAdded() {
        serviceSummery = new ArrayList<>();
        for (BillItem i : getBillItem(BillType.InwardBill, service, false)) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            serviceSummery.add(bi);
        }

        calCountTotalItem(BillType.InwardBill, false);
        proFeeTotal = calServiceTot(BillType.InwardBill, FeeType.Staff, false);
        hosFeeTotal = calServiceTot(BillType.InwardBill, FeeType.OwnInstitution, false);
        outSideFeeTotoal = calServiceTot(BillType.InwardBill, FeeType.OtherInstitution, false);

    }

    public void createServiceSummeryInwardDischarged() {
        serviceSummery = new ArrayList<>();
        for (BillItem i : getBillItem(BillType.InwardBill, service, true)) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            serviceSummery.add(bi);
        }

        calCountTotalItem(BillType.InwardBill, true);
        proFeeTotal = calServiceTot(BillType.InwardBill, FeeType.Staff, true);
        hosFeeTotal = calServiceTot(BillType.InwardBill, FeeType.OwnInstitution, true);
        outSideFeeTotoal = calServiceTot(BillType.InwardBill, FeeType.OtherInstitution, true);
    }

    List<BillItemWithFee> serviceSummery;

    public List<BillItemWithFee> getServiceSummery() {
        return serviceSummery;
    }

    public void setServiceSummery(List<BillItemWithFee> serviceSummery) {
        this.serviceSummery = serviceSummery;
    }

    private double calFee(BillItem bi, FeeType feeType) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeValue) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b and "
                + " f.fee.feeType=:ftp";
        hm.put("b", bi);
        hm.put("ftp", feeType);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

    }

    private double calFeeFeeValue(BillItem bi, FeeType feeType) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeValue) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b and "
                + " f.fee.feeType=:ftp ";
        hm.put("b", bi);
        hm.put("ftp", feeType);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    private double calFeeFeeGrossValue(BillItem bi, FeeType feeType) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeGrossValue) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b and "
                + " f.fee.feeType=:ftp ";
        hm.put("b", bi);
        hm.put("ftp", feeType);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    private double calFeeFeeGrossValueFeeDiscount(BillItem bi, FeeType feeType) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeDiscount) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b and "
                + " f.fee.feeType=:ftp ";
        hm.put("b", bi);
        hm.put("ftp", feeType);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    private double calFeeFeeGrossValueFeeMargin(BillItem bi, FeeType feeType) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeMargin) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b and "
                + " f.fee.feeType=:ftp ";
        hm.put("b", bi);
        hm.put("ftp", feeType);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    private double calFeeOutSideFeeGrossValueFeeMargin(BillItem bi, FeeType feeType, FeeType feeType2) {
        HashMap hm = new HashMap();
        String sql = "Select sum(f.feeMargin) from "
                + " BillFee f where "
                + " f.retired=false "
                + " and f.billItem=:b and "
                + " (f.fee.feeType=:ftp "
                + "or f.fee.feeType=:ftp2 )";
        hm.put("b", bi);
        hm.put("ftp", feeType);
        hm.put("ftp2", feeType2);

        return getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.DATE);

    }

    List<BillItemWithFee> billItemWithFees;
   
    public List<BillItemWithFee> getBillItemWithFees() {
        return billItemWithFees;
    }

    public void setBillItemWithFees(List<BillItemWithFee> billItemWithFees) {
        this.billItemWithFees = billItemWithFees;
    }

    public void createServiceCategorySummery() {
        if (getCategory() == null) {
            return;
        }
        if (getToDate() == null || getFromDate() == null) {
            return;
        }

        billItemWithFees = new ArrayList<>();

        List<BillItem> list = calBillItems(BillType.OpdBill, false);

        for (BillItem i : list) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            billItemWithFees.add(bi);
        }

        calCountTotalCategory(BillType.OpdBill, false);
        calServiceTot1(BillType.OpdBill, false);

    }

    private void calServiceTot1(BillType billType, boolean discharged) {

        if (getCategory() instanceof ServiceSubCategory) {
            value = getServiceValue(getCategory(), billType, discharged);
        }

        if (getCategory() instanceof ServiceCategory) {
            getServiceSubCategoryController().setParentCategory(getCategory());
            List<ServiceSubCategory> subCategorys = getServiceSubCategoryController().getItems();
            if (subCategorys.isEmpty()) {
                value = getServiceValue(getCategory(), billType, discharged);
            } else {
                value = 0;
                for (ServiceSubCategory ssc : subCategorys) {
                    value += getServiceValue(ssc, billType, discharged);
                }
            }
        }

    }

    public void createServiceCategorySummeryInwardAdded() {
        if (getCategory() == null) {
            return;
        }
        if (getToDate() == null || getFromDate() == null) {
            return;
        }

        billItemWithFees = new ArrayList<>();

        List<BillItem> list = calBillItems(BillType.InwardBill, false);

        for (BillItem i : list) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            billItemWithFees.add(bi);
        }

        calCountTotalCategory(BillType.InwardBill, false);
        calServiceTot(BillType.InwardBill, false);

    }

    public void createServiceCategorySummeryInwardDischarged() {
        if (getCategory() == null) {
            return;
        }
        if (getToDate() == null || getFromDate() == null) {
            return;
        }

        billItemWithFees = new ArrayList<>();

        List<BillItem> list = calBillItems(BillType.InwardBill, true);

        for (BillItem i : list) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFee(i, FeeType.OwnInstitution));
            bi.setOutSideFee(calFee(i, FeeType.OtherInstitution));
            billItemWithFees.add(bi);
        }

        calCountTotalCategory(BillType.InwardBill, true);
        calServiceTot(BillType.InwardBill, true);

    }

    public void createServiceCategorySummeryInwardDischargedDetail() {
        if (getCategory() == null) {
            return;
        }
        if (getToDate() == null || getFromDate() == null) {
            return;
        }

        billItemWithFees = new ArrayList<>();
        hosFeeDisTotal = 0;
        hosFeeGrossValueTotal = 0;
        hosFeeMarginTotal = 0;
        hosFeeTotal = 0;
        proFeeTotal = 0;
        outSideFeeDiscountTotal = 0;
        outSideFeeGrossTotal = 0;
        outSideFeeMarginTotal = 0;
        outSideFeeTotoal = 0;

        List<BillItem> list = calBillItems(BillType.InwardBill, true);

        for (BillItem i : list) {
            BillItemWithFee bi = new BillItemWithFee();
            bi.setBillItem(i);
            bi.setProFee(calFee(i, FeeType.Staff));
            bi.setHospitalFee(calFeeFeeValue(i, FeeType.OwnInstitution));
            bi.setHospitalFeeGross(calFeeFeeGrossValue(i, FeeType.OwnInstitution));
            bi.setHospitalFeeDiscount(calFeeFeeGrossValueFeeDiscount(i, FeeType.OwnInstitution));
            bi.setHospitalFeeMargin(calFeeFeeGrossValueFeeMargin(i, FeeType.OwnInstitution));
            bi.setOutSideFee(calFeeFeeValue(i, FeeType.OtherInstitution));
            bi.setOutSideFeeGross(calFeeFeeGrossValue(i, FeeType.OtherInstitution));
            bi.setOutSideFeeDiscount(calFeeFeeGrossValueFeeDiscount(i, FeeType.OtherInstitution));
            bi.setOutSideFeeMargin(calFeeFeeGrossValueFeeMargin(i, FeeType.OtherInstitution));
            hosFeeDisTotal += bi.getHospitalFeeDiscount();
            hosFeeGrossValueTotal += bi.getHospitalFeeGross();
            hosFeeMarginTotal += bi.getHospitalFeeMargin();
            hosFeeTotal += bi.getHospitalFee();
            proFeeTotal += bi.getProFee();

            outSideFeeGrossTotal += bi.getOutSideFeeGross();
            outSideFeeDiscountTotal += bi.getOutSideFeeDiscount();
            outSideFeeMarginTotal += bi.getOutSideFeeMargin();
            outSideFeeTotoal += bi.getOutSideFee();

            //bi.setOutSideFee(calFee(i, FeeType.OtherInstitution));
            billItemWithFees.add(bi);
        }

        calCountTotalCategory(BillType.InwardBill, true);
        //calServiceTot(BillType.InwardBill, true);

    }

    public void createServiceInwardCategorySummery() {
        Map m = new HashMap();
        String sql = "SELECT bi FROM BillItem bi WHERE bi.retired=false and bi.createdAt between :fd and :td and bi.bill.billType=:bt";
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.InwardBill);

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.DATE);

    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    @Inject
    ServiceSubCategoryController serviceSubCategoryController;

    public ServiceSubCategoryController getServiceSubCategoryController() {
        return serviceSubCategoryController;
    }

    public void setServiceSubCategoryController(ServiceSubCategoryController serviceSubCategoryController) {
        this.serviceSubCategoryController = serviceSubCategoryController;
    }

    private List<BillItem> calBillItems(BillType billType, boolean discharged) {
        if (getCategory() instanceof ServiceSubCategory) {
            return getBillItemByCategory(category, billType, discharged);
        }

        if (getCategory() instanceof ServiceCategory) {
            getServiceSubCategoryController().setParentCategory(getCategory());
            List<ServiceSubCategory> subCategorys = getServiceSubCategoryController().getItems();
            if (subCategorys.isEmpty()) {
                return getBillItemByCategory(getCategory(), billType, discharged);
            } else {
                Set<BillItem> setBillItem = new HashSet<>();
                for (ServiceSubCategory ssc : subCategorys) {
                    setBillItem.addAll(getBillItemByCategory(ssc, billType, discharged));
                }

                List<BillItem> tmpBillItems = new ArrayList<>();
                tmpBillItems.addAll(setBillItem);
                return tmpBillItems;
            }
        }

        return null;
    }

    private List<BillItem> getBillItemByCategory(Category cat, BillType billType, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select bi FROM BillItem bi "
                + " where  bi.bill.institution=:ins"
                + " and  bi.bill.billType= :bTp  "
                + " and bi.item.category=:cat";

        if (billType != BillType.InwardBill) {
            sql += " and ( bi.bill.paymentMethod = :pm1 "
                    + " or  bi.bill.paymentMethod = :pm2 "
                    + " or  bi.bill.paymentMethod = :pm3"
                    + " or  bi.bill.paymentMethod = :pm4) ";

            temMap.put("pm1", PaymentMethod.Cash);
            temMap.put("pm2", PaymentMethod.Card);
            temMap.put("pm3", PaymentMethod.Cheque);
            temMap.put("pm4", PaymentMethod.Slip);

        }

        if (discharged) {
            sql += " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        } else {
            sql += " and  bi.bill.createdAt between :fromDate and :toDate ";
        }

        sql += " order by bi.item.name";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);

        temMap.put("cat", cat);
        List<BillItem> tmp = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        System.err.println("BILL " + tmp);
        return tmp;

    }

    public void calCountTotalCategory(BillType billType, boolean discharged) {
        long countTotal = 0;
        long billed = 0l;
        long cancelled = 0l;
        long refunded = 0l;

        if (getCategory() instanceof ServiceSubCategory) {
            billed = getCount(new BilledBill(), billType, getCategory(), discharged);
            cancelled = getCount(new CancelledBill(), billType, getCategory(), discharged);
            refunded = getCount(new RefundBill(), billType, getCategory(), discharged);
        }

        if (getCategory() instanceof ServiceCategory) {
            getServiceSubCategoryController().setParentCategory(getCategory());
            List<ServiceSubCategory> subCategorys = getServiceSubCategoryController().getItems();
            if (subCategorys.isEmpty()) {
                billed = getCount(new BilledBill(), billType, getCategory(), discharged);
                cancelled = getCount(new CancelledBill(), billType, getCategory(), discharged);
                refunded = getCount(new RefundBill(), billType, getCategory(), discharged);
            } else {
                billed = 0l;
                cancelled = 0l;
                refunded = 0l;
                for (ServiceSubCategory ssc : subCategorys) {
                    billed += getCount(new BilledBill(), billType, ssc, discharged);
                    cancelled += getCount(new CancelledBill(), billType, ssc, discharged);
                    refunded += getCount(new RefundBill(), billType, ssc, discharged);
                }
            }
        }

        countTotal = billed - (refunded + cancelled);

        count = countTotal;
    }

    private long getCount(Bill bill, BillType billType, Category cat, boolean discharged) {
        String sql;
        Map temMap = new HashMap();
        sql = "select count(bi) FROM BillItem bi "
                + " where bi.bill.billType=:bType "
                + " and bi.item.category=:cat "
                + " and type(bi.bill)=:billClass "
                + " and bi.bill.toInstitution=:ins ";

        if (billType != BillType.InwardBill) {
            sql += " and ( bi.bill.paymentMethod = :pm1 "
                    + " or  bi.bill.paymentMethod = :pm2 "
                    + " or  bi.bill.paymentMethod = :pm3"
                    + " or  bi.bill.paymentMethod = :pm4) ";

            temMap.put("pm1", PaymentMethod.Cash);
            temMap.put("pm2", PaymentMethod.Card);
            temMap.put("pm3", PaymentMethod.Cheque);
            temMap.put("pm4", PaymentMethod.Slip);

        }

        if (discharged) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate";
        } else {
            sql += " and bi.bill.createdAt between :fromDate and :toDate";
        }

        sql += " order by bi.item.name";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("cat", cat);
        temMap.put("billClass", bill.getClass());
        temMap.put("bType", billType);
        temMap.put("ins", getSessionController().getInstitution());
        return getBillItemFacade().countBySql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getServiceValue(Category cat, BillType billType, FeeType feeType, boolean discharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(bi.feeValue) FROM BillFee bi "
                + " where  bi.bill.institution=:ins"
                + " and  bi.bill.billType= :bTp "
                + " and bi.fee.feeType=:ftp "
                + " and bi.billItem.item.category=:cat";

        if (billType != BillType.InwardBill) {
            sql += " and ( bi.bill.paymentMethod = :pm1 "
                    + " or  bi.bill.paymentMethod = :pm2 "
                    + " or  bi.bill.paymentMethod = :pm3"
                    + " or  bi.bill.paymentMethod = :pm4) ";

            temMap.put("pm1", PaymentMethod.Cash);
            temMap.put("pm2", PaymentMethod.Card);
            temMap.put("pm3", PaymentMethod.Cheque);
            temMap.put("pm4", PaymentMethod.Slip);

        }

        if (discharged) {
            sql += " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate";
        } else {
            sql += " and  bi.bill.createdAt between :fromDate and :toDate";
        }

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("bTp", billType);
        temMap.put("ftp", feeType);
        temMap.put("cat", cat);
        //     List<BillItem> tmp = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        return getBillFeeFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private double getServiceValue(Category cat, BillType billType, boolean discharged) {
        double value = getServiceValue(cat, billType, FeeType.OwnInstitution, discharged);
        value += getServiceValue(cat, billType, FeeType.OtherInstitution, discharged);

        return value;
    }

    private void calServiceTot(BillType billType, boolean discharged) {

        if (getCategory() instanceof ServiceSubCategory) {
            proFeeTotal = getServiceValue(getCategory(), billType, FeeType.Staff, discharged);
            hosFeeTotal = getServiceValue(getCategory(), billType, FeeType.OwnInstitution, discharged);
            outSideFeeTotoal = getServiceValue(getCategory(), billType, FeeType.OtherInstitution, discharged);
        }

        if (getCategory() instanceof ServiceCategory) {
            getServiceSubCategoryController().setParentCategory(getCategory());
            List<ServiceSubCategory> subCategorys = getServiceSubCategoryController().getItems();
            if (subCategorys.isEmpty()) {
                proFeeTotal = getServiceValue(getCategory(), billType, FeeType.Staff, discharged);
                hosFeeTotal = getServiceValue(getCategory(), billType, FeeType.OwnInstitution, discharged);
                outSideFeeTotoal = getServiceValue(getCategory(), billType, FeeType.OtherInstitution, discharged);
            } else {
                proFeeTotal = 0;
                hosFeeTotal = 0;
                outSideFeeTotoal = 0;
                for (ServiceSubCategory ssc : subCategorys) {
                    proFeeTotal += getServiceValue(ssc, billType, FeeType.Staff, discharged);
                    hosFeeTotal += getServiceValue(ssc, billType, FeeType.OwnInstitution, discharged);
                    outSideFeeTotoal += getServiceValue(ssc, billType, FeeType.OtherInstitution, discharged);
                }
            }
        }

    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public Date getFromDate() {
        if (fromDate==null) {
            fromDate=commonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate==null) {
            toDate=commonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Item getService() {
        return service;
    }

    public void setService(Item service) {
        this.service = service;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public double getProFeeTotal() {
        return proFeeTotal;
    }

    public void setProFeeTotal(double proFeeTotal) {
        this.proFeeTotal = proFeeTotal;
    }

    public double getHosFeeGrossValueTotal() {
        return hosFeeGrossValueTotal;
    }

    public void setHosFeeGrossValueTotal(double hosFeeGrossValueTotal) {
        this.hosFeeGrossValueTotal = hosFeeGrossValueTotal;
    }

    public double getHosFeeDisTotal() {
        return hosFeeDisTotal;
    }

    public void setHosFeeDisTotal(double hosFeeDisTotal) {
        this.hosFeeDisTotal = hosFeeDisTotal;
    }

    public double getHosFeeMarginTotal() {
        return hosFeeMarginTotal;
    }

    public void setHosFeeMarginTotal(double hosFeeMarginTotal) {
        this.hosFeeMarginTotal = hosFeeMarginTotal;
    }

    public double getHosFeeTotal() {
        return hosFeeTotal;
    }

    public void setHosFeeTotal(double hosFeeTotal) {
        this.hosFeeTotal = hosFeeTotal;
    }

    public double getOutSideFeeTotoal() {
        return outSideFeeTotoal;
    }

    public void setOutSideFeeTotoal(double outSideFeeTotoal) {
        this.outSideFeeTotoal = outSideFeeTotoal;
    }

    public double getOutSideFeeGrossTotal() {
        return outSideFeeGrossTotal;
    }

    public void setOutSideFeeGrossTotal(double outSideFeeGrossTotal) {
        this.outSideFeeGrossTotal = outSideFeeGrossTotal;
    }

    public double getOutSideFeeDiscountTotal() {
        return outSideFeeDiscountTotal;
    }

    public void setOutSideFeeDiscountTotal(double outSideFeeDiscountTotal) {
        this.outSideFeeDiscountTotal = outSideFeeDiscountTotal;
    }

    public double getOutSideFeeMarginTotal() {
        return outSideFeeMarginTotal;
    }

    public void setOutSideFeeMarginTotal(double outSideFeeMarginTotal) {
        this.outSideFeeMarginTotal = outSideFeeMarginTotal;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getReagentFeeTotal() {
        return reagentFeeTotal;
    }

    public void setReagentFeeTotal(double reagentFeeTotal) {
        this.reagentFeeTotal = reagentFeeTotal;
    }
    
}
