/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.BillBeanController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.dataStructure.DepartmentBillItems;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Consultant;
import com.divudi.entity.Department;
import com.divudi.entity.Fee;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.PatientItem;
import com.divudi.entity.PreBill;
import com.divudi.entity.PriceMatrix;
import com.divudi.entity.RefundBill;
import com.divudi.entity.WebUser;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.inward.InwardFee;
import com.divudi.entity.inward.PatientRoom;
import com.divudi.entity.inward.Room;
import com.divudi.entity.inward.RoomFacilityCharge;
import com.divudi.entity.inward.TimedItem;
import com.divudi.entity.inward.TimedItemFee;
import com.divudi.facade.AdmissionFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.FeeFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientItemFacade;
import com.divudi.facade.PatientRoomFacade;
import com.divudi.facade.PriceMatrixFacade;
import com.divudi.facade.RoomFacade;
import com.divudi.facade.TimedItemFeeFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class InwardBeanController implements Serializable {

    @EJB
    private PatientRoomFacade patientRoomFacade;
    @EJB
    PatientEncounterFacade patientEncounterFacade;
    @EJB
    private RoomFacade roomFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private FeeFacade feeFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    PatientItemFacade patientItemFacade;
    @EJB
    private TimedItemFeeFacade timedItemFeeFacade;
    @EJB
    private CommonFunctions commonFunctions;
    @EJB
    private ItemFeeFacade itemFeeFacade;
    @EJB
    private PriceMatrixFacade priceMatrixFacade;
    @EJB
    private AdmissionFacade admissionFacade;
    @Inject
    BillBeanController billBean;
    @Inject
    InwardReportControllerBht inwardReportControllerBht;
    @Inject
    SessionController sessionController;

    public List<BillItem> createBillItems(Item item, PatientEncounter patientEncounter) {
        String sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.item=:itm"
                + " and b.bill.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("itm", item);
        return getBillItemFacade().findBySQL(sql, hm, TemporalType.TIME);
    }

    public List<BillItem> fetchBillItems(PatientEncounter patientEncounter) {
        String sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        return getBillItemFacade().findBySQL(sql, hm);
    }

    public List<BillItem> fetchEagerBillItems(PatientEncounter patientEncounter) {
        String sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        return getBillItemFacade().findBySQLWithoutCache(sql, hm);
    }

    public List<BillItem> fetchBillItems(BillType billType, Bill bill) {
        String sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and type(b.bill)=:class ";
        HashMap hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", bill.getClass());
        return getBillItemFacade().findBySQL(sql, hm);
    }

    public List<BillItem> fetchBillItems(PatientEncounter patientEncounter, InwardChargeType inwardChargeType) {
        String sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.item.inwardChargeType=:inw"
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("inw", inwardChargeType);
        hm.put("pe", patientEncounter);
        return getBillItemFacade().findBySQL(sql, hm, TemporalType.TIME);
    }

    public List<BillFee> fetchBillFees(PatientEncounter patientEncounter) {
        String sql = "SELECT  b FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        return billFeeFacade.findBySQL(sql, hm, TemporalType.TIME);
    }

    public List<BillFee> fetchBillFees(PatientEncounter patientEncounter, InwardChargeType inwardChargeType) {
        String sql = "SELECT  b FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.billItem.item.inwardChargeType=:inw "
                + " and b.bill.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("inw", inwardChargeType);
        hm.put("pe", patientEncounter);
        return billFeeFacade.findBySQL(sql, hm, TemporalType.TIME);
    }

    public boolean checkRoomDischarge(Date date, PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT pr FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.dischargedAt>:dt "
                + " and pr.patientEncounter=:pe ";
        hm.put("pe", patientEncounter);
        hm.put("dt", date);
        PatientRoom tmp = getPatientRoomFacade().findFirstBySQL(sql, hm, TemporalType.TIMESTAMP);

        if (tmp != null) {
            return true;
        }

        return false;
    }

    public double calTimedPatientItemByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {

        HashMap hm = new HashMap();
        String sql = " SELECT sum(i.serviceValue) "
                + " FROM PatientItem i where "
                + " type(i.item)=:cls "
                + " and i.retired=false "
                + " and i.patientEncounter=:pe "
                + " and i.item.inwardChargeType=:inw ";
        hm.put("pe", patientEncounter);
        hm.put("cls", TimedItem.class);
        hm.put("inw", inwardChargeType);
        return getPatientItemFacade().findDoubleByJpql(sql, hm);

    }

    public List<BillItem> getIssueBillItemByInwardChargeType(PatientEncounter patientEncounter, BillType billType) {
        String sql = "Select s From BillItem s"
                + " where s.retired=false"
                + " and s.bill.billType=:btp"
                + " and s.bill.patientEncounter=:pe ";

        HashMap hm = new HashMap();
        hm.put("btp", billType);
        hm.put("pe", patientEncounter);

        return getBillItemFacade().findBySQL(sql, hm);

    }

    public Double[] fetchDiscountAndNetTotalByBillItem(Bill b) {
        String sql = "Select sum(s.discount),"
                + " sum(s.netValue) "
                + "  From BillItem s"
                + " where s.retired=false"
                + " and s.bill=:bill";

        HashMap hm = new HashMap();
        hm.put("bill", b);

        Object[] obj = getBillItemFacade().findAggregateModified(sql, hm, TemporalType.DATE);

        if (obj == null) {
            Double[] dbl = new Double[2];
            dbl[0] = 0.0;
            dbl[0] = 0.0;

            return dbl;
        }

        return Arrays.copyOf(obj, obj.length, Double[].class);

    }

    public List<Bill> fetchIssueBills(PatientEncounter patientEncounter, BillType billType) {
        String sql = "Select distinct(s.bill)"
                + "  From BillItem s"
                + " where s.retired=false"
                + " and s.bill.billType=:btp"
                + " and s.bill.patientEncounter=:pe ";

        HashMap hm = new HashMap();
        hm.put("btp", billType);
        hm.put("pe", patientEncounter);

        return billFacade.findBySQL(sql, hm);

    }

    public double calIssueBillItemDiscountByInwardChargeType(PatientEncounter patientEncounter, BillType billType) {
        String sql = "Select sum(s.discount) From BillItem s"
                + " where s.retired=false"
                + " and s.bill.billType=:btp"
                + " and s.bill.patientEncounter=:pe ";

        HashMap hm = new HashMap();
        hm.put("btp", billType);
        hm.put("pe", patientEncounter);

        return getBillItemFacade().findDoubleByJpql(sql, hm);

    }

    public double getTimedItemFeeTotalByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {

        HashMap hm = new HashMap();
        String sql = " SELECT sum(i.serviceValue) "
                + " FROM PatientItem i where "
                + " type(i.item)=:cls "
                + " and i.retired=false "
                + " and i.patientEncounter=:pe "
                + " and i.item.inwardChargeType=:inw ";
        hm.put("pe", patientEncounter);
        hm.put("cls", TimedItem.class);
        hm.put("inw", inwardChargeType);
        double dbl = getPatientItemFacade().findDoubleByJpql(sql, hm);

        return dbl;
    }

    public List<PatientItem> fetchTimedPatientItemByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {

        HashMap hm = new HashMap();
        String sql = " SELECT i "
                + " FROM PatientItem i where "
                + " type(i.item)=:cls "
                + " and i.retired=false "
                + " and i.patientEncounter=:pe "
                + " and i.item.inwardChargeType=:inw ";
        hm.put("pe", patientEncounter);
        hm.put("cls", TimedItem.class);
        hm.put("inw", inwardChargeType);
        return getPatientItemFacade().findBySQL(sql, hm);

    }

    public List<PatientRoom> fetchPatientRoomAll(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT pr FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findBySQL(sql, hm);

    }

    public double calCostOfIssue(PatientEncounter patientEncounter, BillType billType) {
        String sql;
        HashMap hm;
        sql = "SELECT  sum(b.grossValue+b.marginValue)"
                + " FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and  b.bill.patientEncounter=:pe";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("pe", patientEncounter);
        return getBillItemFacade().findDoubleByJpql(sql, hm);

    }

    public double calServiceBillItemsTotalByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "Select sum(s.grossValue+s.marginValue) "
                + " From BillItem s"
                + " where s.retired=false "
                + " and s.bill.billType=:btp "
                + " and s.bill.patientEncounter=:pe"
                + " and s.item.inwardChargeType=:inw ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("inw", inwardChargeType);

        double dbl = getBillFeeFacade().findDoubleByJpql(sql, hm);

        return dbl;

    }

    public double calculateProfessionalCharges(PatientEncounter patientEncounter) {

        HashMap hm = new HashMap();
        String sql = "SELECT sum(bt.feeValue)"
                + " FROM BillFee bt"
                + " WHERE bt.retired=false"
                + " and type(bt.staff)=:class "
                + " and bt.fee.feeType=:ftp  "
                + " and (bt.bill.billType=:btp2) "
                + " and bt.bill.patientEncounter=:pe";
        hm.put("class", Consultant.class);
        hm.put("ftp", FeeType.Staff);
        //  hm.put("btp", BillType.InwardBill);
        hm.put("btp2", BillType.InwardProfessional);
        hm.put("pe", patientEncounter);

        double val = getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.TIME);

        return val;
    }

    public double calOutSideBillItemsTotalByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "Select sum(s.feeValue) From BillFee s"
                + " where s.retired=false "
                + " and s.billItem.bill.billType=:btp "
                + " and s.billItem.bill.patientEncounter=:pe"
                + " and s.billItem.inwardChargeType=:inw ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardOutSideBill);
        hm.put("pe", patientEncounter);
        hm.put("inw", inwardChargeType);

        double dbl = getBillFeeFacade().findDoubleByJpql(sql, hm);

        return dbl;

    }

    public List<BillFee> getServiceBillFeesByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "Select s From BillFee s"
                + " where s.retired=false"
                //  + " and s.bill.cancelled=false"
                // + " and type(s.bill)=:billedClass "
                + " and s.billItem.bill.billType=:btp "
                + " and s.billItem.bill.patientEncounter=:pe"
                + " and s.billItem.item.inwardChargeType=:inw "
                + " and s.fee.feeType!=:st ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("inw", inwardChargeType);
        //  hm.put("billedClass", BilledBill.class);
        hm.put("st", FeeType.Staff);

        return getBillFeeFacade().findBySQL(sql, hm);

    }

    public List<BillItem> getServiceBillItemByInwardChargeType(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        String sql = "Select s From BillItem s"
                + " where s.retired=false "
                + " and s.bill.billType=:btp "
                + " and s.bill.patientEncounter=:pe"
                + " and s.item.inwardChargeType=:inw ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("inw", inwardChargeType);

        return getBillItemFacade().findBySQL(sql, hm);

    }

    public List<BillFee> createDoctorAndNurseFee(PatientEncounter patientEncounter) {

        HashMap hm = new HashMap();
        String sql = "SELECT bt FROM BillFee bt WHERE "
                + " bt.retired=false "
                + " and type(bt.staff)!=:class "
                + " and bt.fee.feeType=:ftp "
                + " and (bt.bill.billType=:btp)"
                + " and bt.bill.patientEncounter=:pe ";
        hm.put("class", Consultant.class);
        hm.put("ftp", FeeType.Staff);
        hm.put("btp", BillType.InwardProfessional);
        hm.put("pe", patientEncounter);

        return getBillFeeFacade().findBySQL(sql, hm, TemporalType.TIME);

    }

    public List<BillFee> createProfesionallFee(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT bt FROM BillFee bt WHERE "
                + " bt.retired=false "
                + " and type(bt.staff)=:class "
                + " and bt.fee.feeType=:ftp "
                + " and (bt.bill.billType=:btp)"
                + " and bt.bill.patientEncounter=:pe "
                + " order by bt.feeAdjusted desc ";
        hm.put("class", Consultant.class);
        hm.put("ftp", FeeType.Staff);
        hm.put("btp", BillType.InwardProfessional);
        hm.put("pe", patientEncounter);

        return getBillFeeFacade().findBySQL(sql, hm, TemporalType.TIME);
        ////System.out.println("Size : " + profesionallFee.size());

    }

    
    public List<BillFee> createProfesionallFeeEstimated(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT bt FROM BillFee bt WHERE "
                + " bt.retired=false "
                + " and type(bt.staff)=:class "
                + " and bt.fee.feeType=:ftp "
                + " and (bt.bill.billType=:btp)"
                + " and bt.bill.patientEncounter=:pe "
                + " order by bt.feeAdjusted desc ";
        hm.put("class", Consultant.class);
        hm.put("ftp", FeeType.Staff);
        hm.put("btp", BillType.InwardProfessionalEstimates);
        hm.put("pe", patientEncounter);

        return getBillFeeFacade().findBySQL(sql, hm, TemporalType.TIME);
        ////System.out.println("Size : " + profesionallFee.size());

    }

    
    public void setProfesionallFeeAdjusted(PatientEncounter patientEncounter) {

        HashMap hm = new HashMap();
        String sql = "SELECT bt FROM BillFee bt"
                + " WHERE bt.retired=false "
                + " and type(bt.staff)=:class "
                + " and bt.fee.feeType=:ftp "
                + " and (bt.bill.billType=:btp)"
                + " and bt.bill.patientEncounter=:pe ";
        hm.put("class", Consultant.class);
        hm.put("ftp", FeeType.Staff);
        hm.put("btp", BillType.InwardProfessional);
        hm.put("pe", patientEncounter);

        List<BillFee> list = getBillFeeFacade().findBySQL(sql, hm);

        for (BillFee bf : list) {
            bf.setFeeAdjusted(bf.getFeeValue());
            getBillFeeFacade().edit(bf);
        }

        ////System.out.println("Size : " + profesionallFee.size());
    }

    public List<Bill> fetchIssueTable(PatientEncounter patientEncounter, BillType billType) {
        List<Bill> list = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:btp "
                + " and (b.billedBill is null )  "
                + " and  b.patientEncounter=:pe"
                + " and (type(b)=:class) ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", PreBill.class);
        hm.put("pe", patientEncounter);

        List<Bill> bills = getBillFacade().findBySQL(sql, hm);

        hm.clear();
        sql = "SELECT  b FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:btp"
                + " and type(b.billedBill)=:billedClass "
                + " and  b.patientEncounter=:pe"
                + " and (type(b)=:class) ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", RefundBill.class);
        hm.put("billedClass", PreBill.class);
        hm.put("pe", patientEncounter);

        List<Bill> bills2 = getBillFacade().findBySQL(sql, hm);

        list.addAll(bills);
        list.addAll(bills2);

        return list;
    }

    public List<BillItem> fetchPharmacyIssueBillItem(PatientEncounter patientEncounter, BillType billType) {
        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and  b.bill.patientEncounter=:pe"
                + " and (type(b.bill)=:class) ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", PreBill.class);
        hm.put("pe", patientEncounter);

        List<BillItem> list = getBillItemFacade().findBySQL(sql, hm);

        hm.clear();
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp"
                + " and  b.bill.patientEncounter=:pe"
                + " and (type(b.bill)=:class) ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", RefundBill.class);
        hm.put("pe", patientEncounter);

        List<BillItem> list2 = getBillItemFacade().findBySQL(sql, hm);

        grantList.addAll(list);
        grantList.addAll(list2);

        return grantList;

    }

    public List<BillItem> fetchBillItem1(BillType billType) {
//        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.marginValue=0 "
                + " and b.discount=0 "
                + " and b.grossValue!=b.netValue";
        hm = new HashMap();
        hm.put("btp", billType);

        List<BillItem> list = getBillItemFacade().findBySQL(sql, hm);
        System.err.println("**************************** List 1 :  " + list.size());
        return list;

    }

    public List<BillFee> fetchBillFee1(BillType billType) {
//        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.feeMargin=0 "
                + " and b.feeDiscount=0 "
                + " and b.feeGrossValue!=b.feeValue";
        hm = new HashMap();
        hm.put("btp", billType);

        List<BillFee> list = billFeeFacade.findBySQL(sql, hm);
        System.err.println("**************************** List BillFee  1 : " + list.size());
        return list;

    }

    public List<BillItem> fetchBillItem2(BillType billType) {
//        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.marginValue!=0 "
                + " and b.discount=0 "
                + " and (b.grossValue+b.marginValue)!=b.netValue";
        hm = new HashMap();
        hm.put("btp", billType);

        List<BillItem> list = getBillItemFacade().findBySQL(sql, hm);
        System.err.println("****************************** BillItems 2 : " + list.size());
        return list;

    }

    public List<BillFee> fetchBillFee2(BillType billType) {
//        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.feeMargin!=0 "
                + " and b.feeDiscount=0 "
                + " and (b.feeGrossValue+b.feeMargin)!=b.feeValue";
        hm = new HashMap();
        hm.put("btp", billType);

        List<BillFee> list = billFeeFacade.findBySQL(sql, hm);
        System.err.println("****************************** BillFee 2 : " + list.size());
        return list;

    }

    public List<BillItem> fetchBillItem3(BillType billType) {
//        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.marginValue!=0 "
                + " and b.discount!=0 "
                + " and (b.grossValue+b.marginValue-b.discount)!=b.netValue";
        hm = new HashMap();
        hm.put("btp", billType);

        List<BillItem> list = getBillItemFacade().findBySQL(sql, hm);
        System.err.println("******************************** BillItems 3 : " + list.size());
        return list;

    }

    public List<BillFee> fetchBillFee3(BillType billType) {
//        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillFee b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.feeMargin!=0 "
                + " and b.feeDiscount!=0 "
                + " and (b.feeGrossValue+b.feeMargin-b.feeDiscount)!=b.feeValue";
        hm = new HashMap();
        hm.put("btp", billType);

        List<BillFee> list = billFeeFacade.findBySQL(sql, hm);
        System.err.println("******************************** BillFee 3 : " + list.size());
        return list;

    }

    public List<BillItem> createIssueItemTable(PatientEncounter patientEncounter, BillType billType) {
        List<BillItem> grantList = new ArrayList<>();
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp"
                + " and (b.bill.billedBill is null ) "
                + " and  b.bill.patientEncounter=:pe"
                + " and (type(b.bill)=:class) ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", PreBill.class);
        hm.put("pe", patientEncounter);

        List<BillItem> list = getBillItemFacade().findBySQL(sql, hm);

        hm.clear();
        sql = "SELECT  b FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp"
                + " and type(b.bill.billedBill)=:billedClass "
                + " and  b.bill.patientEncounter=:pe"
                + " and (type(b.bill)=:class) ";
        hm = new HashMap();
        hm.put("btp", billType);
        hm.put("class", RefundBill.class);
        hm.put("billedClass", PreBill.class);
        hm.put("pe", patientEncounter);

        List<BillItem> list2 = getBillItemFacade().findBySQL(sql, hm);

        grantList.addAll(list);
        grantList.addAll(list2);

        return grantList;
    }

    public List<Bill> createStoreTable(PatientEncounter patientEncounter) {
        String sql;
        HashMap hm;
        sql = "SELECT  b FROM Bill b"
                + " WHERE b.retired=false "
                + " and b.billType=:btp  "
                + " and  b.patientEncounter=:pe"
                + " and type(b)=:class ";
        hm = new HashMap();
        hm.put("btp", BillType.StoreBhtPre);
        hm.put("class", PreBill.class);
        hm.put("pe", patientEncounter);
        return getBillFacade().findBySQL(sql, hm);

    }

    public List<BillItem> getService(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {

        String sql = "SELECT  b FROM BillItem b"
                + "  WHERE b.retired=false  "
                + " and b.bill.billType=:btp"
                + " and Type(b.item)!=TimedItem  "
                + " and b.bill.patientEncounter=:pe "
                + " and b.bill.cancelled=false"
                + " and b.item.inwardChargeType=:inw ";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("inw", inwardChargeType);
        return getBillItemFacade().findBySQL(sql, hm, TemporalType.TIME);

    }

    public double calculateDoctorAndNurseCharges(PatientEncounter patientEncounter) {

        HashMap hm = new HashMap();
        String sql = "SELECT sum(bt.feeValue)"
                + " FROM BillFee bt"
                + " WHERE bt.retired=false"
                + " and type(bt.staff)!=:class "
                + " and bt.fee.feeType=:ftp  "
                + " and (bt.bill.billType=:btp2) "
                + " and bt.bill.patientEncounter=:pe";
        hm.put("class", Consultant.class);
        hm.put("ftp", FeeType.Staff);
        //     hm.put("btp", BillType.InwardBill);
        hm.put("btp2", BillType.InwardProfessional);
        hm.put("pe", patientEncounter);

        double val = getBillFeeFacade().findDoubleByJpql(sql, hm, TemporalType.TIME);

        //   System.err.println("NURSE " + val);
        return val;
    }

    public boolean isRoomFilled(Room room) {
        String sql = "select p from PatientRoom p "
                + " where p.retired=false "
                + " and p.roomFacilityCharge.room=:rm "
                + " and p.discharged=false ";
        HashMap hm = new HashMap();
        hm.put("rm", room);
        PatientRoom patientRoom = getPatientRoomFacade().findFirstBySQL(sql, hm);

        if (patientRoom != null) {
            return true;
        } else {
            return false;
        }
    }

    public double getRoomCharge(PatientEncounter patientEncounter) {
        String sql = "select sum(p.calculatedRoomCharge) "
                + " from PatientRoom p "
                + " where p.retired=false "
                + " and p.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("pe", patientEncounter);

        return getPatientRoomFacade().findDoubleByJpql(sql, hm);
    }

    public double getMoCharge(PatientEncounter patientEncounter) {
        String sql = "select sum(p.calculatedMoCharge) "
                + " from PatientRoom p "
                + " where p.retired=false "
                + " and p.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("pe", patientEncounter);

        return getPatientRoomFacade().findDoubleByJpql(sql, hm);
    }

    public double getProfessionalCharge(PatientEncounter patientEncounter) {
        String sql = "select sum(p.feeAdjusted) "
                + " from BillFee p "
                + " where p.retired=false "
                + " and p.bill.patientEncounter=:pe"
                + " and p.bill.billType=:bilTp ";
        HashMap hm = new HashMap();
        hm.put("pe", patientEncounter);
        hm.put("bilTp", BillType.InwardProfessional);
        return getBillFeeFacade().findDoubleByJpql(sql, hm);
    }

    public double getNursingCharge(PatientEncounter patientEncounter) {
        String sql = "select sum(p.calculatedNursingCharge) "
                + " from PatientRoom p "
                + " where p.retired=false "
                + " and p.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("pe", patientEncounter);

        return getPatientRoomFacade().findDoubleByJpql(sql, hm);
    }

    public double getMaintainCharge(PatientEncounter patientEncounter) {
        String sql = "select sum(p.calculatedMaintainCharge) "
                + " from PatientRoom p "
                + " where p.retired=false "
                + " and p.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("pe", patientEncounter);

        return getPatientRoomFacade().findDoubleByJpql(sql, hm);
    }

    public double getMedicalCareIcuCharge(PatientEncounter patientEncounter) {
        String sql = "select sum(p.calculatedMedicalCareCharge) "
                + " from PatientRoom p "
                + " where p.retired=false "
                + " and p.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("pe", patientEncounter);

        return getPatientRoomFacade().findDoubleByJpql(sql, hm);
    }

    public double getAdminCharge(PatientEncounter patientEncounter) {
        String sql = "select sum(p.calculatedAdministrationCharge) "
                + " from PatientRoom p "
                + " where p.retired=false "
                + " and p.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("pe", patientEncounter);

        return getPatientRoomFacade().findDoubleByJpql(sql, hm);
    }

    public double getLinenCharge(PatientEncounter patientEncounter) {
        String sql = "select sum(p.calculatedLinenCharge) "
                + " from PatientRoom p "
                + " where p.retired=false "
                + " and p.patientEncounter=:pe ";
        HashMap hm = new HashMap();
        hm.put("pe", patientEncounter);

        return getPatientRoomFacade().findDoubleByJpql(sql, hm);
    }

    public PatientItemFacade getPatientItemFacade() {
        return patientItemFacade;
    }

    public void setPatientItemFacade(PatientItemFacade patientItemFacade) {
        this.patientItemFacade = patientItemFacade;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    private List<Department> getToDepartmentList(PatientEncounter patientEncounter, Bill forwardRefBill) {
        String sql;
        HashMap hm = new HashMap();

        sql = "SELECT  distinct(b.bill.toDepartment) FROM BillItem b "
                + " WHERE   b.retired=false "
                + " and b.bill.billType=:btp ";

        if (forwardRefBill != null) {
            sql += " and b.bill.forwardReferenceBill=:fB";
            hm.put("fB", forwardRefBill);
        }

        sql += " and Type(b.item)!=TimedItem "
                + " and b.bill.patientEncounter=:pe ";

        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);

        return getDepartmentFacade().findBySQL(sql, hm, TemporalType.TIME);
    }

    private List<Item> getToDepartmentItems(PatientEncounter patientEncounter, Department department, Bill forwardBill) {
        HashMap hm = new HashMap();
        String sql = "SELECT  distinct(b.item) FROM BillItem b "
                + " WHERE b.retired=false"
                + " and b.bill.billType=:btp";

        if (forwardBill != null) {
            sql += " and b.bill.forwardReferenceBill=:fB";
            hm.put("fB", forwardBill);
        }

        sql += " and Type(b.item)!=TimedItem"
                + "  and b.bill.patientEncounter=:pe"
                + " and b.bill.toDepartment=:dep "
                + "  order by b.item.name ";

        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("dep", department);

        return getItemFacade().findBySQL(sql, hm, TemporalType.TIME);
    }

    public boolean checkByBillItem(PatientEncounter patientEncounter, Bill billClass, BillType billType) {
        String sql = "Select b.bill From BillItem b"
                + " where b.bill.retired=false "
                + " and b.retired=false"
                + " and b.bill.cancelled=false "
                + " and (b.refunded is null "
                + " or b.refunded=false) ";

        if (billClass instanceof PreBill) {
            sql += " and b.bill.billedBill is null ";
        }
        sql += " and b.bill.refundedBill is null "
                + " and b.bill.checkedBy is null"
                + " and b.bill.billType=:bt"
                + " and b.bill.patientEncounter=:pe "
                + " and b.bill.netTotal !=0 "
                + " and type(b.bill)=:class";
        HashMap hm = new HashMap();
        hm.put("bt", billType);
        hm.put("pe", patientEncounter);
        hm.put("class", billClass.getClass());
        Bill bill = getBillFacade().findFirstBySQL(sql, hm);

        if (bill != null) {
            return true;
        }

        return false;

    }

    public boolean checkByBillFee(PatientEncounter patientEncounter, Bill billClass, BillType billType) {
        String sql = "Select b.bill From BillFee b"
                + " where b.bill.retired=false"
                + " and b.retired=false "
                + " and b.bill.cancelled=false "
                + " and (b.billItem.refunded is null "
                + " or b.billItem.refunded=false) "
                + " and b.bill.billedBill is null"
                + " and b.bill.checkedBy is null"
                + " and b.bill.billType=:bt"
                + " and b.bill.patientEncounter=:pe "
                + " and b.bill.netTotal !=0 "
                + " and type(b.bill)=:class";
        HashMap hm = new HashMap();
        hm.put("bt", billType);
        hm.put("pe", patientEncounter);
        hm.put("class", billClass.getClass());
        Bill bill = getBillFacade().findFirstBySQL(sql, hm);

        if (bill != null) {
            return true;
        }

        return false;

    }

//    public boolean checkRefundedBill(PatientEncounter patientEncounter, BillType billType) {
//        String sql = "Select b From RefundBill b"
//                + " where b.retired=false "
//                + " and b.cancelled=false "
//                + " and b.billedBill is null "
//                + " and b.checkedBy is null "
//                + " and b.netTotal!=0"
//                + " and b.billType=:bt "
//                + " and b.patientEncounter=:pe ";
//        HashMap hm = new HashMap();
//        hm.put("bt", billType);
//        hm.put("pe", patientEncounter);
//        Bill bill = getBillFacade().findFirstBySQL(sql, hm);
//
//        if (bill != null) {
//            return true;
//        }
//
//        return false;
//
//    }
    private double calBillItemCount(Bill bill, Item item, PatientEncounter patientEncounter, Bill forwardBill) {
        HashMap hm = new HashMap();
        String sql = "SELECT  count(b) FROM BillItem b "
                + " WHERE b.retired=false "
                + "  and b.bill.billType=:btp ";

        if (forwardBill != null) {
            sql += " and b.bill.forwardReferenceBill=:fB";
            hm.put("fB", forwardBill);
        }

        sql += " and b.bill.patientEncounter=:pe "
                + " and b.item=:itm "
                + " and type(b.bill)=:cls";

        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("itm", item);
        hm.put("cls", bill.getClass());
        double dbl = getBillItemFacade().countBySql(sql, hm, TemporalType.TIME);

        return dbl;
    }

    private double calCheckedBillItemCount(Item item, PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT  count(b) FROM BillItem b "
                + " WHERE b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.bill.patientEncounter=:pe "
                + " and b.item=:itm "
                + " and type(b.bill)=:cls"
                + " and b.bill.checkedBy is not null "
                + " and b.bill.checkeAt is not null "
                + " and b.bill.cancelled=false ";

        hm.put("btp", BillType.InwardBill);
        hm.put("pe", patientEncounter);
        hm.put("itm", item);
        hm.put("cls", BilledBill.class);

        double dbl = getBillItemFacade().countBySql(sql, hm);

        return dbl;
    }

    public List<Bill> fetchOutSideBill(PatientEncounter patientEncounter) {

        String sql = "Select i From BilledBill i "
                + " where i.retired=false"
                + "  and i.billType=:btp "
                + " and i.patientEncounter=:pe ";

        HashMap m = new HashMap();
        m.put("btp", BillType.InwardOutSideBill);
        m.put("pe", patientEncounter);
        return getBillFacade().findBySQL(sql, m, TemporalType.DATE);

        //return additionalChargeBill;
    }

    public List<Bill> fetchOutSideBill2(PatientEncounter patientEncounter) {

        String sql = "Select i From Bill i "
                + " where i.retired=false"
                + "  and i.billType=:btp "
                + " and i.patientEncounter=:pe ";

        HashMap m = new HashMap();
        m.put("btp", BillType.InwardOutSideBill);
        m.put("pe", patientEncounter);
        return getBillFacade().findBySQL(sql, m, TemporalType.DATE);

        //return additionalChargeBill;
    }

    public double caltValueFromAdditionalCharge(InwardChargeType inwardChargeType, PatientEncounter patientEncounter) {
        //   additionalChargeBill = new ArrayList<>();
        String sql = "Select sum(i.netValue)"
                + " From BillItem i "
                + " where i.retired=false "
                + " and i.bill.billType=:btp "
                + "and i.bill.patientEncounter=:pe "
                + " and i.inwardChargeType=:inwCh ";
        HashMap m = new HashMap();
        m.put("btp", BillType.InwardOutSideBill);
        m.put("pe", patientEncounter);
        m.put("inwCh", inwardChargeType);
        double val = getBillFacade().findDoubleByJpql(sql, m, TemporalType.DATE);

        return val;
    }

    public List<PatientItem> fetchPatientItem(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT i FROM PatientItem i "
                + " where Type(i.item)=TimedItem "
                + " and i.retired=false "
                + " and i.patientEncounter=:pe";
        hm.put("pe", patientEncounter);
        return getPatientItemFacade().findBySQL(sql, hm);
    }

    public List<Bill> fetchPaymentBill(PatientEncounter patientEncounter) {

        HashMap hm = new HashMap();
        String sql = "SELECT  b FROM Bill b "
                + " WHERE b.retired=false "
                + " and b.billType=:btp "
                + " and b.patientEncounter=:pe ";
        hm.put("btp", BillType.InwardPaymentBill);
        hm.put("pe", patientEncounter);
        return getBillFacade().findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }

    public double calPatientRoomChargeDiscount(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.discountRoomCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientRoomAdminChargeDiscount(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.discountAdministrationCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientRoomMadicalCareChargeDiscount(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.discountMedicalCareCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientRoomLinenChargeDiscount(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.discountLinenCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientMoChargeDiscount(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.discountMoCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientMaintananceChargeDiscount(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.discountMaintainCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientNursingChargeDiscount(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.discountNursingCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientRoomChargeAdjusted(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.adjustedRoomCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientRoomLinenChargeAdjusted(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.ajdustedLinenCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientRoomAdminAdjusted(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.ajdustedAdministrationCharge) "
                + " FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientRoomMadicalCareAdjusted(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.ajdustedMedicalCareCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientMoChargeAdjusted(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.adjustedMoCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientMaintananceChargeAdjusted(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.adjustedMaintainCharge) FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public double calPatientNursingChargeAdjusted(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT sum(pr.ajdustedNursingCharge) "
                + " FROM PatientRoom pr "
                + " where pr.retired=false"
                + " and pr.patientEncounter=:pe "
                + " order by pr.createdAt";
        hm.put("pe", patientEncounter);
        return getPatientRoomFacade().findDoubleByJpql(sql, hm);

    }

    public List<DepartmentBillItems> createDepartmentBillItems(PatientEncounter patientEncounter, Bill forwardRefBill) {
        List<DepartmentBillItems> list = new ArrayList<>();

        List<Department> deptList = getToDepartmentList(patientEncounter, forwardRefBill);

        for (Department dep : deptList) {
            DepartmentBillItems table = new DepartmentBillItems();

            List<Item> items = getToDepartmentItems(patientEncounter, dep, forwardRefBill);

            for (Item itm : items) {
                double billed = calBillItemCount(new BilledBill(), itm, patientEncounter, forwardRefBill);
                double cancelld = calBillItemCount(new CancelledBill(), itm, patientEncounter, forwardRefBill);
                double refund = calBillItemCount(new RefundBill(), itm, patientEncounter, forwardRefBill);
//                System.err.println("Billed " + billed);
//                System.err.println("Cancelled " + cancelld);
//                System.err.println("Refun " + refund);

                itm.setTransCheckedCount(calCheckedBillItemCount(itm, patientEncounter));
                itm.setTransBillItemCount(billed - (cancelld + refund));
            }

            table.setDepartment(dep);
            table.setItems(items);

            list.add(table);

        }

//        calServiceTot(departmentBillItems);
        return list;

    }

    public Fee getStaffFeeForInward(WebUser webUser) {
        String sql = "Select f From InwardFee f "
                + " where f.retired=false "
                + " and f.feeType=:st ";

        HashMap hm = new HashMap();
        hm.put("st", FeeType.Staff);

        Fee fee = getFeeFacade().findFirstBySQL(sql, hm);
        if (fee == null) {
            fee = new InwardFee();
            fee.setCreatedAt(new Date());
            fee.setCreater(webUser);
            fee.setFeeType(FeeType.Staff);

            if (fee.getId() == null) {
                getFeeFacade().create(fee);
            }
        }

        return fee;

    }

    public Bill fetchFinalBill(PatientEncounter patientEncounter) {
        String sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.billType=:btp "
                + " and b.patientEncounter=:pe"
                + " order by b.id desc";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardFinalBill);
        hm.put("pe", patientEncounter);

        return getBillFacade().findFirstBySQL(sql, hm);
    }

    public List<Bill> fetchFinalBills() {
        String sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.billType=:btp "
                + " and b.patientEncounter.paymentFinalized=true";
        HashMap hm = new HashMap();
        hm.put("btp", BillType.InwardFinalBill);

        return getBillFacade().findBySQL(sql, hm);
    }

    public void updateFinalFill(PatientEncounter patientEncounter) {
        Bill b = fetchFinalBill(patientEncounter);
        if (b == null) {
            return;
        }

        double paid = getPaidValue(patientEncounter);
//        System.err.println("NET " + b.getNetTotal());
//        System.err.println("PAID " + paid);

        b.setPaidAmount(paid);
        getBillFacade().edit(b);

    }

    public double getPaidValue(PatientEncounter patientEncounter) {

        HashMap hm = new HashMap();
        String sql = "SELECT  sum(b.netTotal) FROM Bill b "
                + " WHERE b.retired=false "
                + "  and b.billType=:btp "
                + " and b.patientEncounter=:pe ";
        hm.put("btp", BillType.InwardPaymentBill);
        hm.put("pe", patientEncounter);
        double dbl = getBillFacade().findDoubleByJpql(sql, hm, TemporalType.TIMESTAMP);

        return dbl;

    }

    public void updateCreditDetail(PatientEncounter patientEncounter, double netTotal) {
        if (patientEncounter == null) {
            return;
        }

        if (patientEncounter.getCreditLimit() == 0) {
            patientEncounter.setCreditUsedAmount(netTotal);
            patientEncounterFacade.edit(patientEncounter);
            return;
        }

        if (patientEncounter.getCreditLimit() <= netTotal) {
            patientEncounter.setCreditUsedAmount(patientEncounter.getCreditLimit());
        } else {
            patientEncounter.setCreditUsedAmount(netTotal);
        }

        patientEncounterFacade.edit(patientEncounter);
    }

    public PatientRoom savePatientRoom(PatientRoom patientRoom, PatientRoom previousRoom, RoomFacilityCharge newRoomFacilityCharge, PatientEncounter patientEncounter, Date admittedAt, WebUser webUser) {
        //     patientRoom.setCurrentLinenCharge(patientRoom.getRoomFacilityCharge().getLinenCharge());

        System.err.println("Mill " + patientRoom);
        System.err.println("Pre " + previousRoom);
        System.err.println("new " + newRoomFacilityCharge);
        if (patientRoom == null) {
            return null;
        }

        if (sessionController.getInstitutionPreference().isInwardMoChargeCalculateInitialTime()) {
            patientRoom.setCurrentMoChargeForAfterDuration(newRoomFacilityCharge.getMoChargeForAfterDuration());
        }

        patientRoom.setCurrentMaintananceCharge(newRoomFacilityCharge.getMaintananceCharge());
        patientRoom.setCurrentMoCharge(newRoomFacilityCharge.getMoCharge());
        patientRoom.setCurrentNursingCharge(newRoomFacilityCharge.getNursingCharge());
        patientRoom.setCurrentRoomCharge(newRoomFacilityCharge.getRoomCharge());
        patientRoom.setCurrentLinenCharge(newRoomFacilityCharge.getLinenCharge());
        patientRoom.setCurrentMedicalCareCharge(newRoomFacilityCharge.getMedicalCareCharge());
        patientRoom.setCurrentAdministrationCharge(newRoomFacilityCharge.getAdminstrationCharge());

        patientRoom.setPreviousRoom(previousRoom);
        patientRoom.setCreatedAt(Calendar.getInstance().getTime());
        patientRoom.setCreater(webUser);
        patientRoom.setAdmittedAt(admittedAt);
        patientRoom.setAddmittedBy(webUser);
        patientRoom.setPatientEncounter(patientEncounter);
        patientRoom.setRoomFacilityCharge(newRoomFacilityCharge);

//        if (patientEncounter.getAdmissionType().isRoomChargesAllowed() == false) {
//            patientRoom.setDischarged(true);
//        }
        if (patientRoom.getId() == null || patientRoom.getId() == 0) {
            getPatientRoomFacade().create(patientRoom);
        } else {
            getPatientRoomFacade().edit(patientRoom);
        }

        return patientRoom;
    }

    public PatientRoom savePatientRoom(PatientRoom patientRoom, RoomFacilityCharge newRoomFacilityCharge, PatientEncounter patientEncounter, Date admittedAt, WebUser webUser) {
        //     patientRoom.setCurrentLinenCharge(patientRoom.getRoomFacilityCharge().getLinenCharge());

        System.err.println("Mill " + patientRoom);

        System.err.println("new " + newRoomFacilityCharge);
        if (patientRoom == null) {
            return null;
        }

        patientRoom.setCurrentMaintananceCharge(newRoomFacilityCharge.getMaintananceCharge());
        patientRoom.setCurrentMoCharge(newRoomFacilityCharge.getMoCharge());
        
        if (sessionController.getInstitutionPreference().isInwardMoChargeCalculateInitialTime()) {
            patientRoom.setCurrentMoChargeForAfterDuration(newRoomFacilityCharge.getMoChargeForAfterDuration());
        }

        patientRoom.setCurrentNursingCharge(newRoomFacilityCharge.getNursingCharge());
        patientRoom.setCurrentRoomCharge(newRoomFacilityCharge.getRoomCharge());
        patientRoom.setCurrentLinenCharge(newRoomFacilityCharge.getLinenCharge());
        patientRoom.setCurrentMedicalCareCharge(newRoomFacilityCharge.getMedicalCareCharge());
        patientRoom.setCurrentAdministrationCharge(newRoomFacilityCharge.getAdminstrationCharge());

        patientRoom.setCreatedAt(Calendar.getInstance().getTime());
        patientRoom.setCreater(webUser);
        patientRoom.setAdmittedAt(admittedAt);
        patientRoom.setAddmittedBy(webUser);
        patientRoom.setPatientEncounter(patientEncounter);
        patientRoom.setRoomFacilityCharge(newRoomFacilityCharge);

//        if (patientEncounter.getAdmissionType().isRoomChargesAllowed() == false) {
//            patientRoom.setDischarged(true);
//        }
        if (patientRoom.getId() == null || patientRoom.getId() == 0) {
            getPatientRoomFacade().create(patientRoom);
        } else {
            getPatientRoomFacade().edit(patientRoom);
        }

        return patientRoom;
    }

    // Add business logic below. (Right-click in editor and choose
    // "Insert Code > Add Business Method")
    public PatientRoomFacade getPatientRoomFacade() {
        return patientRoomFacade;
    }

    public void setPatientRoomFacade(PatientRoomFacade patientRoomFacade) {
        this.patientRoomFacade = patientRoomFacade;
    }

    public RoomFacade getRoomFacade() {
        return roomFacade;
    }

    public void setRoomFacade(RoomFacade roomFacade) {
        this.roomFacade = roomFacade;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public FeeFacade getFeeFacade() {
        return feeFacade;
    }

    public void setFeeFacade(FeeFacade feeFacade) {
        this.feeFacade = feeFacade;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public InwardReportControllerBht getInwardReportControllerBht() {
        return inwardReportControllerBht;
    }

    public void setInwardReportControllerBht(InwardReportControllerBht inwardReportControllerBht) {
        this.inwardReportControllerBht = inwardReportControllerBht;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public List<PatientRoom> getPatientRooms(PatientEncounter patientEncounter) {
        HashMap hm = new HashMap();
        String sql = "SELECT pr FROM PatientRoom pr where pr.retired=false"
                + " and pr.patientEncounter=:pe order by pr.createdAt";
        hm.put("pe", patientEncounter);
        List<PatientRoom> tmp = getPatientRoomFacade().findBySQL(sql, hm);

        if (tmp == null) {
            tmp = new ArrayList<>();
        }

        return tmp;
    }

    public String getBhtText(AdmissionType admissionType) {
        String bhtText;
        String sql = "SELECT count(a.id) FROM Admission a "
                + " where a.admissionType.admissionTypeEnum=:adType ";

        HashMap hm = new HashMap();
        hm.put("adType", admissionType.getAdmissionTypeEnum());
        long temp = getAdmissionFacade().countBySql(sql, hm);

        temp = temp + admissionType.getAdditionToCount();

        bhtText = admissionType.getCode().trim() + Long.toString(temp);

        if (getSessionController().getInstitutionPreference().isBhtNumberWithYear()) {
            Calendar c = Calendar.getInstance();

            bhtText = bhtText + "/" + c.get(Calendar.YEAR);
        }
        return bhtText;
    }

    public Fee createAdditionalFee() {
        String sql = "Select f from Fee f where f.retired=false and f.feeType=:nm";
        HashMap hm = new HashMap();
        hm.put("nm", FeeType.Additional);
        List<Fee> fee = getFeeFacade().findBySQL(sql, hm, TemporalType.TIMESTAMP);
        Fee additional;

        if (fee.isEmpty()) {
            additional = new Fee();
            additional.setName("Additional");
            additional.setFeeType(FeeType.Additional);
            additional.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            if (additional.getId() == null) {
                getFeeFacade().create(additional);
            }
            return additional;
        } else {
            return fee.get(0);
        }
    }

    public void setBillFeeMargin(BillFee billFee, Item item, PriceMatrix priceMatrix) {
        double margin = 0;

        if (billFee == null || item.isMarginNotAllowed()) {
            return;
        }

        if (billFee.getFee().getFeeType() != FeeType.Staff
                && priceMatrix != null) {

            margin = (billFee.getFeeGrossValue() * priceMatrix.getMargin()) / 100;
            billFee.setFeeMargin(margin);
        }

        double net = (billFee.getFeeGrossValue() + margin) - billFee.getFeeDiscount();

        billFee.setFeeValue(net);
    }

    public void updateBillItemMargin(BillItem billItem, double serviceValue, PatientEncounter patientEncounter, Department matrixDepartment, PriceMatrix priceMatrix) {

        List<BillFee> billFees = getBillBean().getBillFee(billItem);

        for (BillFee billFee : billFees) {
            setBillFeeMargin(billFee, billItem.getItem(), priceMatrix);

            if (billFee.getId() != null) {
                getBillFeeFacade().edit(billFee);
            }
        }

    }

    public void updateBillItemMargin(BillFee billFee, double serviceValue, PatientEncounter patientEncounter, Department matrixDepartment, PriceMatrix priceMatrix) {

        setBillFeeMargin(billFee, billFee.getBillItem().getItem(), priceMatrix);

        if (billFee.getId() != null) {
            getBillFeeFacade().edit(billFee);
        }

    }

    public void saveBillFee(BillFee bf, BillItem billItem, Bill b, WebUser wu) {

        bf.setCreatedAt(Calendar.getInstance().getTime());
        bf.setCreater(wu);
        bf.setBillItem(billItem);
        bf.setPatienEncounter(b.getPatientEncounter());
        bf.setPatient(b.getPatient());

        bf.setBill(b);
        if (bf.getId() == null) {
            getBillFeeFacade().create(bf);
        }

    }

//    public void updateBillItemMargin(BillItem billItem, List<BillFee> billFees, PriceMatrix priceMatrix, PatientEncounter patientEncounter) {
//        System.err.println("///////////////////////");
//        System.err.println("Margin " + priceMatrix.getMargin());
//
//        for (BillFee billFee : billFees) {
//            updateBillFeeMargin(billFee, priceMatrix);
//            getBillFeeFacade().edit(billFee);
//        }
//
//    }
    @EJB
    private BillFeeFacade billFeeFacade;

    public BillFee getIssueBillFee(BillItem billItem, Institution institution) {
        String sql = "Select bf from BillFee bf where bf.retired=false and "
                + " bf.billItem=:bItem and bf.fee.feeType=:ftp ";
        HashMap hm = new HashMap();
        hm.put("bItem", billItem);
        hm.put("ftp", FeeType.Issue);

        BillFee billtItemFee = getBillFeeFacade().findFirstBySQL(sql, hm);

        if (billtItemFee == null) {
            billtItemFee = new BillFee();

            Fee issueFee = getIssueFee();

            billtItemFee.setBillItem(billItem);
            billtItemFee.setFee(issueFee);
            billtItemFee.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            billtItemFee.setInstitution(institution);
//            getBillFeeFacade().create(billFee);
        }

        return billtItemFee;
    }

    private Fee getIssueFee() {
        String sql = "Select f from Fee f where f.retired=false and f.feeType=:nm";
        HashMap hm = new HashMap();
        hm.put("nm", FeeType.Issue);
        Fee issue = getFeeFacade().findFirstBySQL(sql, hm);

        if (issue == null) {
            issue = new Fee();
            issue.setName("Issue");
            issue.setFeeType(FeeType.Issue);
            issue.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());

            if (issue.getId() == null) {
                getFeeFacade().create(issue);
            }

        }

        return issue;
    }

    public TimedItemFee getTimedItemFee(TimedItem ti) {
        TimedItemFee tmp = new TimedItemFee();
        if (ti.getId() != null) {
            String sql = "SELECT tif FROM TimedItemFee tif where tif.retired=false AND tif.item.id=" + ti.getId();
            tmp = getTimedItemFeeFacade().findFirstBySQL(sql);
        }

        if (tmp == null) {
            tmp = new TimedItemFee();
            tmp.setDurationHours(0);
            tmp.setOverShootHours(0);
        }
        return tmp;
    }

    public TimedItemFeeFacade getTimedItemFeeFacade() {
        return timedItemFeeFacade;
    }

    public void setTimedItemFeeFacade(TimedItemFeeFacade timedItemFeeFacade) {
        this.timedItemFeeFacade = timedItemFeeFacade;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public double calTotalLinen(PatientEncounter patientEncounter) {

        if (patientEncounter == null || patientEncounter.getAdmissionType() == null) {
            return 0;
        }

        double linen = 0.0;

        Long dayCount = getCommonFunctions().getDayCount(patientEncounter.getDateOfAdmission(), patientEncounter.getDateOfDischarge());

        for (PatientRoom pr : getPatientRooms(patientEncounter)) {
            linen += pr.getAddedLinenCharge();
        }

        if (patientEncounter.getAdmissionType().getDblValue() != null) {
            if (dayCount != 0) {
                linen += (patientEncounter.getAdmissionType().getDblValue() * dayCount);
            } else {
                linen += (patientEncounter.getAdmissionType().getDblValue() * 1);
            }
        }

        return linen;
    }

    public double calCountWithoutOverShoot(TimedItemFee tif, Date admittedAt, Date dischargedAt) {

        double duration = tif.getDurationHours() * 60;
        double consumeTimeM = 0L;

        consumeTimeM = getCommonFunctions().calculateDurationMin(admittedAt, dischargedAt);

        double count = 0;

        if (tif.isBooleanValue()) {
            //For Minute Calculation
            count = (consumeTimeM / duration);
        } else {
            //For Hour Calculation
            count = (long) (consumeTimeM / duration);
        }

        //  System.err.println("Min " + duration);
        //     System.err.println("Consume " + consumeTimeM);
        //   System.err.println("Count " + count);
        if (0 != (consumeTimeM % duration)) {
            count++;
        }

        return count;
    }

    public double calCount(TimedItemFee tif, Date admittedDate, Date dischargedDate) {

        double duration = tif.getDurationHours() * 60;
        double overShoot = tif.getOverShootHours() * 60;
        //  double tempFee = tif.getFee();
        double consumeTime = 0;

        if (dischargedDate == null) {
            dischargedDate = new Date();
        }

        consumeTime = getCommonFunctions().calculateDurationMin(admittedDate, dischargedDate);

        double count = 0;
        double calculation = 0;

        if (consumeTime != 0 && duration != 0) {
            if (tif.isBooleanValue()) {
                //For Minut Calculation (Theatre Charges)
                count = (consumeTime / duration);
            } else {
                //For Room Calculation Hour(For Room Charges)
                count = (long) (consumeTime / duration);
            }

            calculation = (consumeTime - (count * duration));
            if (overShoot != 0 && overShoot <= (calculation)) {
                count++;
            }
        }

        return count;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

    public AdmissionFacade getAdmissionFacade() {
        return admissionFacade;
    }

    public void setAdmissionFacade(AdmissionFacade admissionFacade) {
        this.admissionFacade = admissionFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public PriceMatrixFacade getPriceMatrixFacade() {
        return priceMatrixFacade;
    }

    public void setPriceMatrixFacade(PriceMatrixFacade priceMatrixFacade) {
        this.priceMatrixFacade = priceMatrixFacade;
    }

}
