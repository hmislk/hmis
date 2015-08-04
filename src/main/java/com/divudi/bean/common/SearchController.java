/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.InstitutionType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.ejb.CommonFunctions;
import com.divudi.ejb.PharmacyBean;
import com.divudi.entity.BatchBill;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PreBill;
import com.divudi.entity.RefundBill;
import com.divudi.entity.ServiceSession;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.facade.BatchBillFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PatientInvestigationFacade;
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
public class SearchController implements Serializable {

    private SearchKeyword searchKeyword;
    Date fromDate;
    Date toDate;
    private int maxResult = 50;
    private BillType billType;
    private PaymentMethod paymentMethod;

    ////////////
    private List<Bill> bills;
    private List<Bill> selectedBills;
    List<Bill> aceptPaymentBills;
    private List<BillFee> billFees;
    private List<BillFee> billFeesDone;
    private List<BillItem> billItems;
    private List<PatientInvestigation> patientInvestigations;
    private List<PatientInvestigation> patientInvestigationsSigle;
    Bill cancellingIssueBill;
    Bill bill;
    ////////////
    Speciality speciality;
    Staff staff;
    Item item;
    double dueTotal;
    double doneTotal;
    double netTotal;
    ////////////
    @EJB
    private CommonFunctions commonFunctions;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PatientInvestigationFacade patientInvestigationFacade;
    @Inject
    private BillBeanController billBean;
    @EJB
    private PharmacyBean pharmacyBean;
    ServiceSession selectedServiceSession;
    Staff currentStaff;
    List<BillItem> billItem;
    //////////
    @Inject
    private SessionController sessionController;
    @Inject
    TransferController transferController;
    List<PatientInvestigation> userPatientInvestigations;

    public void makeListNull() {
        maxResult = 50;
        bills = null;
        aceptPaymentBills = null;
        selectedBills = null;
        billFees = null;
        billItems = null;
        patientInvestigations = null;
        searchKeyword = null;
    }

    public void makeListNull2() {
        billFeesDone = null;
        searchKeyword = null;
        speciality = null;
        staff = null;
        item = null;
        makeListNull();
    }

    public void createPreRefundTable() {

        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from RefundBill b where b.billType = :billType "
                + " and b.institution=:ins and "
                + " (b.billedBill is null  or type(b.billedBill)=:billedClass ) "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false and b.deptId is not null ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billedClass", PreBill.class);
        temMap.put("billType", BillType.PharmacyPre);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQLWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void reportSettledOPDBills() {
        settledBills(billType.OpdBill);
    }

    public void reportSettledPharmacyBills() {
        settledBills(billType.PharmacyWholeSale);
    }

    public void settledBills(BillType bt) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where "
                + " b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.referenceBill.billType=:bt "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                //                + " and b.balance=0 "
                + "order by b.createdAt desc ";

//    
        temMap.put("billType", BillType.CashRecieveBill);
        temMap.put("bt", bt);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    List<billsWithbill> withbills;

    public List<billsWithbill> getWithbills() {
        return withbills;
    }

    public void setWithbills(List<billsWithbill> withbills) {
        this.withbills = withbills;
    }

    public void createCreditBillsWithOPDBill() {
        createCreditBillsWithBill(billType.OpdBill);
    }

    public void createCreditBillsWithPharmacyBill() {
        createCreditBillsWithBill(billType.PharmacyWholeSale);
    }

    public void createCreditBillsWithBill(BillType refBillType) {
        bills = fetchBills(BillType.CashRecieveBill, refBillType);
        //System.out.println("bills = " + bills);
        withbills = new ArrayList<>();

        for (Bill b : bills) {
            billsWithbill bWithbill = new billsWithbill();
            bWithbill.setB(b);
            bWithbill.setCaBills(fetchCreditBills(BillType.CashRecieveBill, b));
            //System.out.println("bWithbill.getCaBills() = " + bWithbill.getCaBills());
            withbills.add(bWithbill);
        }
        //System.out.println("withbills = " + withbills);

    }

    public List<Bill> fetchBills(BillType bt, BillType rbt) {

        String sql;
        Map temMap = new HashMap();

        sql = "select DISTINCT(b.referenceBill) from Bill b where "
                + " b.billType =:billType "
                + " and b.referenceBill.billType =:billTypeRef "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        temMap.put("billType", bt);
        temMap.put("billTypeRef", rbt);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        return getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
    }

    public List<Bill> fetchCreditBills(BillType bt, Bill b) {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where "
                + " b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.referenceBill=:bid ";

        temMap.put("billType", bt);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bid", b);
        temMap.put("ins", getSessionController().getInstitution());

        return getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public class billsWithbill {

        Bill b;
        List<Bill> caBills;

        public Bill getB() {
            return b;
        }

        public void setB(Bill b) {
            this.b = b;
        }

        public List<Bill> getCaBills() {
            if (caBills == null) {
                caBills = new ArrayList<>();
            }
            return caBills;
        }

        public void setCaBills(List<Bill> caBills) {
            this.caBills = caBills;
        }

    }

    public void createReturnSaleBills() {
        createReturnSaleBills(BillType.PharmacyPre, true);
    }

    public void createReturnSaleAllBills() {
        createReturnSaleBills(BillType.PharmacyPre, false);
    }

    public void createReturnWholeSaleBills() {
        createReturnSaleBills(BillType.PharmacyWholesalePre, true);
    }

    public void createReturnWholeSaleAllBills() {
        createReturnSaleBills(BillType.PharmacyWholesalePre, false);
    }

    public void createReturnSaleBills(BillType billType, boolean maxNum) {

        Map m = new HashMap();
        m.put("bt", billType);
        m.put("billedClass", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from RefundBill b where  b.retired=false "
                + " and b.institution=:ins and "
                + " (b.billedBill is null  or type(b.billedBill)=:billedClass ) "
                + " and b.createdAt between :fd and :td"
                + " and b.billType=:bt ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  (upper(b.billedBill.deptId) like :rNo )";
            m.put("rNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
        if (maxNum == true) {
            bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 25);
        } else {
            bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        }

    }

    public void createTableByKeywordToPayBills() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where "
                + " b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.balance>0 ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((upper(b.deptId) like :billNo )or(upper(b.insId) like :billNo ))";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.OpdBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
    }

    public void createTablePharmacyCreditToPayBills() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where "
                + " b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.department=:dep "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and (b.netTotal-b.paidAmount)>0 "
                + " and b.paymentMethod=:pm ";

        if (getSearchKeyword().getInstitution() != null && !getSearchKeyword().getInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :comp )";
            temMap.put("comp", "%" + getSearchKeyword().getInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.toStaff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((upper(b.deptId) like :billNo )or(upper(b.insId) like :billNo ))";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.PharmacyWholeSale);
        temMap.put("pm", PaymentMethod.Credit);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("dep", getSessionController().getDepartment());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createReturnBhtBills() {
        createReturnBhtBills(BillType.PharmacyBhtPre);
    }

    public void createReturnBhtBillsStore() {
        createReturnBhtBills(BillType.StoreBhtPre);
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    private void createReturnBhtBills(BillType billType) {

        Map m = new HashMap();
        m.put("bt", billType);
        m.put("billedClass", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from RefundBill b where  b.retired=false "
                + " and b.institution=:ins and "
                + " (b.billedBill is null  or type(b.billedBill)=:billedClass ) "
                + " and b.createdAt between :fd and :td"
                + " and b.billType=:bt ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  (upper(b.billedBill.deptId) like :rNo )";
            m.put("rNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            m.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createVariantReportSearch() {
        String sql = "";
        HashMap tmp = new HashMap();

        sql = "Select b From PreBill b where b.cancelledBill is null  "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false and b.billType= :bTp ";

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :dep )";
            tmp.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCategory() != null && !getSearchKeyword().getCategory().trim().equals("")) {
            sql += " and  (upper(b.category.name) like :cat )";
            tmp.put("cat", "%" + getSearchKeyword().getCategory().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("bTp", BillType.PharmacyMajorAdjustment);
        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);
    }

    List<Bill> prescreptionBills;
    Department department;

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public List<Bill> getPrescreptionBills() {
        return prescreptionBills;
    }

    public void setPrescreptionBills(List<Bill> prescreptionBills) {
        this.prescreptionBills = prescreptionBills;
    }

    public void createPharmacyPrescriptionBillTable() {
        Map m = new HashMap();
        m.put("bt", BillType.PharmacyPre);
        m.put("rBt", BillType.PharmacySale);
        m.put("class", PreBill.class);
        m.put("rClass", BilledBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;
        sql = "Select b from Bill b "
                + " where b.retired=false and b.createdAt between :fd and :td and b.billType=:bt "
                + " and b.referredBy is not null "
                + " and b.institution=:ins "
                + " and b.referenceBill.billType=:rBt "
                + " and type(b)=:class "
                + " and type(b.referenceBill)=:rClass ";

        if (department != null) {
            sql += " and b.department=:dept ";
            m.put("dept", department);
        }

        sql += " order by b.createdAt ";

        prescreptionBills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

    }

    public void createPharmacyRetailBills() {
        createPharmacyRetailBills(BillType.PharmacyPre, true);
    }

    public void createPharmacyWholesaleBills() {
        createPharmacyRetailBills(BillType.PharmacyWholesalePre, true);
    }

    public void createPharmacyRetailAllBills() {
        createPharmacyRetailBills(BillType.PharmacyPre, false);
    }

    public void createPharmacyWholesaleAllBills() {
        createPharmacyRetailBills(BillType.PharmacyWholesalePre, false);
    }

    public void createPharmacyRetailBills(BillType billtype, boolean maxNum) {

        Map m = new HashMap();
        m.put("bt", billtype);
        //   m.put("class", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        m.put("ldep", getSessionController().getLoggedUser().getDepartment());
        String sql;

        sql = "Select b from PreBill b where "
                + " b.createdAt between :fd and :td "
                + " and b.billType=:bt"
                + " and b.billedBill is null "
                + " and b.institution=:ins "
                + " and b.department=:ldep";
        //  + " and type(b)=:class ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :dep )";
            m.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }
        if (getPaymentMethod() != null) {
            sql += " and b.paymentMethod=:pay ";
            m.put("pay", paymentMethod);
        }

        sql += " order by b.createdAt desc  ";
//    
        //     ////System.out.println("sql = " + sql);

        if (maxNum == true) {
            bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 25);
        } else {
            bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        }
        netTotal = 0.0;
        for (Bill b : bills) {
            netTotal += b.getNetTotal();
        }
    }

    double netTotalValue;

    public void createPharmacyStaffBill() {

        Map m = new HashMap();
        m.put("bt", BillType.PharmacyPre);
        //   m.put("class", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from PreBill b where "
                + " b.createdAt between :fd and :td "
                + " and b.billType=:bt"
                + " and b.billedBill is null "
                + " and b.institution=:ins "
                + " and b.toStaff is not null "
                + " order by b.createdAt ";
//    
        //     ////System.out.println("sql = " + sql);
        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

        netTotalValue = 0.0;
        for (Bill b : bills) {
            netTotalValue += b.getNetTotal();
        }
    }

    public void createPharmacyTableRe() {

        Map m = new HashMap();
        m.put("bt", BillType.PharmacyPre);
        //     m.put("class", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from PreBill b where "
                + " b.createdAt between :fd and :td "
                + " and b.billType=:bt "
                + " and b.cancelled=true "
                + " and b.institution=:ins";
        //+ " and type(b)=:class ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :dep )";
            m.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        //     ////System.out.println("sql = " + sql);
        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void listPharmacyIssue() {
        listPharmacyPreBills(BillType.PharmacyIssue);
    }

    public void listStoreIssue() {
        listPharmacyPreBills(BillType.StoreIssue);
    }

    public void listPharmacyCancelled() {
        listPharmacyCancelledBills(BillType.PharmacyIssue);
    }

    public void listPharmacyReturns() {
        listPharmacyStoreReturnedBills(BillType.PharmacyIssue);
    }

    public void listStoreReturns() {
        listPharmacyStoreReturnedBills(BillType.StoreIssue);
    }

    public void listPharmacyBilledBills(BillType bt) {
        listPharmacyBills(bt, BilledBill.class);
    }

    public void listPharmacyPreBills(BillType bt) {
        listPharmacyBills(bt, PreBill.class);
    }

    public void listPharmacyCancelledBills(BillType bt) {
        listPharmacyBills(bt, CancelledBill.class);
    }

    public void listPharmacyStoreReturnedBills(BillType bt) {
        listReturnBills(bt, RefundBill.class);
    }

    public ServiceSession getSelectedServiceSession() {
        return selectedServiceSession;
    }

    public void setSelectedServiceSession(ServiceSession selectedServiceSession) {
        this.selectedServiceSession = selectedServiceSession;
    }

    public Staff getCurrentStaff() {
        return currentStaff;
    }

    public void setCurrentStaff(Staff currentStaff) {
        this.currentStaff = currentStaff;
    }

    public void listPharmacyBills(BillType bt, Class bc) {

        Map m = new HashMap();
        m.put("bt", bt);
        m.put("class", bc);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from Bill b where "
                + " b.createdAt between :fd and :td "
                + " and b.billType=:bt "
                + " and b.institution=:ins "
                + " and type(b)=:class "
                + " and b.billedBill is null ";

        if (getSearchKeyword().getRequestNo() != null && !getSearchKeyword().getRequestNo().trim().equals("")) {
            sql += " and  (upper(b.invoiceNumber) like :requestNo )";
            m.put("requestNo", "%" + getSearchKeyword().getRequestNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :dep )";
            m.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        //     ////System.out.println("sql = " + sql);
        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }
    
    public void listReturnBills(BillType bt, Class bc) {

        Map m = new HashMap();
        m.put("bt", bt);
        m.put("class", bc);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        m.put("dept", getSessionController().getDepartment());
        String sql;

        sql = "Select b from Bill b where "
                + " b.createdAt between :fd and :td "
                + " and b.billType=:bt "
                + " and b.institution=:ins "
                + " and b.department=:dept "
                + " and type(b)=:class ";

        if (getSearchKeyword().getRequestNo() != null && !getSearchKeyword().getRequestNo().trim().equals("")) {
            sql += " and  (upper(b.invoiceNumber) like :requestNo )";
            m.put("requestNo", "%" + getSearchKeyword().getRequestNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :dep )";
            m.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        //     ////System.out.println("sql = " + sql);
        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyTableBht() {
        createTableBht(BillType.PharmacyBhtPre);
    }

    public void createStoreTableIssue() {
        createTableBht(BillType.StoreIssue);
    }

    public void createStoreTableBht() {
        createTableBht(BillType.StoreBhtPre);
    }

    public void createTableBht(BillType btp) {

        Map m = new HashMap();
        m.put("bt", btp);
        m.put("class", PreBill.class);
        m.put("fd", getFromDate());
        m.put("td", getToDate());
        m.put("ins", getSessionController().getInstitution());
        String sql;

        sql = "Select b from Bill b "
                + " where b.retired=false "
                + "  and b.billedBill is null "
                + " and b.createdAt between :fd and :td "
                + " and b.billType=:bt"
                + " and b.institution=:ins "
                + " and type(b)=:class ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            m.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :dep )";
            m.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            m.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        //     ////System.out.println("sql = " + sql);
        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createIssueTable() {
        String sql;
        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("dep", getSessionController().getDepartment());
        tmp.put("bTp", BillType.PharmacyTransferIssue);
        sql = "Select b From BilledBill b where b.retired=false and "
                + " b.toDepartment=:dep and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.toStaff.person.name) like :stf )";
            tmp.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :fDep )";
            tmp.put("fDep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setTmpRefBill(getRefBill(b));

        }

    }

    public void createIssueReport1() {
        String sql;
        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        //tmp.put("dep", getSessionController().getDepartment());
        tmp.put("bTp", BillType.PharmacyTransferIssue);
        sql = "Select b From BilledBill b "
                + " where b.retired=false "
                //+ " and b.toDepartment=:dep "
                + " and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.toStaff.person.name) like :stf )";
            tmp.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFrmDepartment() != null) {
            sql += " and b.department=:frmdep";
            tmp.put("frmdep", getSearchKeyword().getFrmDepartment());
        }

        if (getSearchKeyword().getTooDepartment() != null) {
            sql += " and b.toDepartment=:tdep";
            tmp.put("tdep", getSearchKeyword().getTooDepartment());
        }

        sql += " order by b.createdAt desc  ";

        List<Bill> list = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);
        bills = new ArrayList<>();
        netTotalValue = 0.0;
        for (Bill b : list) {
//            //System.out.println("b = ");

            Bill refBill = getActiveRefBill(b);
            if (refBill == null) {
                //System.out.println("b = " + refBill);
                netTotalValue += b.getNetTotal();
                bills.add(b);
            }
        }

    }

    public void createIssuePharmacyReport() {
//        fetchPharmacyBills(BillType.PharmacyTransferIssue, BillType.PharmacyTransferReceive);
        fetchPharmacyBillsNew(BillType.PharmacyTransferIssue, BillType.PharmacyTransferReceive);
    }

    public void createIssueStoreReport() {
        fetchPharmacyBills(BillType.StoreTransferIssue, BillType.StoreTransferReceive);
    }

    public void createPoNotPharmacyApproveReport() {
        fetchPharmacyBills(BillType.PharmacyOrder, BillType.PharmacyAdjustment);
    }

    public void createPoNotStoreApproveReport() {
        fetchPharmacyBills(BillType.StoreOrder, BillType.StoreOrderApprove);
    }

    public void fetchPharmacyBills(BillType billType, BillType billType2) {
        String sql;
        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        //tmp.put("dep", getSessionController().getDepartment());
        tmp.put("bTp", billType);
        sql = "Select b From BilledBill b "
                + " where b.retired=false "
                //+ " and b.toDepartment=:dep "
                + " and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.toStaff.person.name) like :stf )";
            tmp.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFrmDepartment() != null) {
            sql += " and b.department=:frmdep";
            tmp.put("frmdep", getSearchKeyword().getFrmDepartment());
        }

        if (getSearchKeyword().getTooDepartment() != null) {
            sql += " and b.toDepartment=:tdep";
            tmp.put("tdep", getSearchKeyword().getTooDepartment());
        }

        sql += " order by b.createdAt desc  ";

        List<Bill> list = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);
        bills = new ArrayList<>();
        netTotalValue = 0.0;
        for (Bill b : list) {
//            //System.out.println("b = ");

            Bill refBill = getActiveRefBillnotApprove(b, billType2);
            if (refBill == null) {
                //System.out.println("b = " + refBill);
                netTotalValue += b.getNetTotal();
                bills.add(b);
            }
        }

    }

    public void fetchPharmacyBillsNew(BillType billType, BillType billType2) {
        String sql;
        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        //tmp.put("dep", getSessionController().getDepartment());
        tmp.put("bTp", billType);
        sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false "
                + " and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.toStaff.person.name) like :stf )";
            tmp.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFrmDepartment() != null) {
            sql += " and b.department=:frmdep";
            tmp.put("frmdep", getSearchKeyword().getFrmDepartment());
        }

        if (getSearchKeyword().getTooDepartment() != null) {
            sql += " and b.toDepartment=:tdep";
            tmp.put("tdep", getSearchKeyword().getTooDepartment());
        }

        sql += " order by b.createdAt desc  ";

        List<Bill> list = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP);
        bills = new ArrayList<>();
        netTotalValue = 0.0;
        for (Bill b : list) {
//            //System.out.println("b = ");

            Bill refBill = getActiveRefBillnotApprove(b, billType2);
            if (refBill == null) {
                //System.out.println("b = " + refBill);
                netTotalValue += b.getNetTotal();
                bills.add(b);
            }
        }

    }

    public void createIssueTableStore() {
        String sql;
        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("dep", getSessionController().getDepartment());
        tmp.put("bTp", BillType.StoreTransferIssue);
        sql = "Select b From BilledBill b where b.retired=false and "
                + " b.toDepartment=:dep and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.toStaff.person.name) like :stf )";
            tmp.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :fDep )";
            tmp.put("fDep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setTmpRefBill(getRefBill(b));

        }

    }

    private Bill getRefBill(Bill b) {
        String sql = "Select b From Bill b where b.retired=false "
                + " and b.cancelled=false and b.billType=:btp and "
                + " b.referenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyTransferReceive);
        return getBillFacade().findFirstBySQL(sql, hm);
    }

    private Bill getActiveRefBill(Bill b) {
        String sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false"
                + "  and b.billType=:btp"
                + " and b.backwardReferenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyTransferReceive);
        return getBillFacade().findFirstBySQL(sql, hm);
    }

    private Bill getActiveRefBillnotApprove(Bill b, BillType billType) {
        String sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.cancelled=false"
                + "  and b.billType=:btp"
                + " and b.backwardReferenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", billType);
        return getBillFacade().findFirstBySQL(sql, hm);
    }

    public void makeNull() {
        searchKeyword = null;
        bills = null;

    }

    public void createTableByBillType() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where b.retired=false and "
                + " (type(b)=:class1 or type(b)=:class2) "
                + " and b.department=:dep and b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRequestNo() != null && !getSearchKeyword().getRequestNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :requestNo )";
            temMap.put("requestNo", "%" + getSearchKeyword().getRequestNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  (upper(b.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromDepartment() != null && !getSearchKeyword().getFromDepartment().trim().equals("")) {
            sql += " and  (upper(b.fromDepartment.name) like :frmDept )";
            temMap.put("frmDept", "%" + getSearchKeyword().getFromDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :toIns )";
            temMap.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToDepartment() != null && !getSearchKeyword().getToDepartment().trim().equals("")) {
            sql += " and  (upper(b.toDepartment.name) like :toDept )";
            temMap.put("toDept", "%" + getSearchKeyword().getToDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  (upper(b.referenceBill.deptId) like :refId )";
            temMap.put("refId", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.invoiceNumber) like :inv )";
            temMap.put("inv", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and b.id in (select bItem.bill.id  "
                    + " from BillItem bItem where bItem.retired=false and  "
                    + " (upper(bItem.item.name) like :itm ))";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and b.id in (select bItem.bill.id  "
                    + " from BillItem bItem where bItem.retired=false and  "
                    + " (upper(bItem.item.code) like :cde ))";
            temMap.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("class1", BilledBill.class);
        temMap.put("class2", PreBill.class);
        temMap.put("billType", billType);
        temMap.put("dep", getSessionController().getDepartment());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //temMap.put("dep", getSessionController().getDepartment());
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, maxResult);
        //     //System.err.println("SIZE : " + lst.size());

    }

    public void createTableByBillTypeAllDepartment() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where b.retired=false and "
                + " (type(b)=:class1 or type(b)=:class2) "
                + " and  b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRequestNo() != null && !getSearchKeyword().getRequestNo().trim().equals("")) {
            sql += " and (upper(b.insId) like :requestNo)";
            temMap.put("requestNo", "%" + getSearchKeyword().getRequestNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  (upper(b.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :toIns )";
            temMap.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  (upper(b.referenceBill.deptId) like :refId )";
            temMap.put("refId", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.invoiceNumber) like :inv )";
            temMap.put("inv", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and b.id in (select bItem.bill.id  "
                    + " from BillItem bItem where bItem.retired=false and  "
                    + " (upper(bItem.item.name) like :itm ))";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and b.id in (select bItem.bill.id  "
                    + " from BillItem bItem where bItem.retired=false and  "
                    + " (upper(bItem.item.code) like :cde ))";
            temMap.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("class1", BilledBill.class);
        temMap.put("class2", PreBill.class);
        temMap.put("billType", billType);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        //temMap.put("dep", getSessionController().getDepartment());
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, maxResult);
        //     //System.err.println("SIZE : " + lst.size());

    }

    public void createRequestTable() {
        String sql;

        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("toDep", getSessionController().getDepartment());
        tmp.put("bTp", BillType.PharmacyTransferRequest);

        sql = "Select b From Bill b where "
                + " b.retired=false and  b.toDepartment=:toDep"
                + " and b.billType= :bTp and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :dep )";
            tmp.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getIssudBills(b));
        }

    }

    public void createRequestTableStore() {
        String sql;

        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("toDep", getSessionController().getDepartment());
        tmp.put("bTp", BillType.StoreTransferRequest);

        sql = "Select b From Bill b where "
                + " b.retired=false and  b.toDepartment=:toDep"
                + " and b.billType= :bTp and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :dep )";
            tmp.put("dep", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getIssudBills(b));
        }

    }

    public void createListToCashRecieve() {
        String sql;

        HashMap tmp = new HashMap();
        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("toWeb", getSessionController().getLoggedUser());
        tmp.put("bTp", BillType.CashOut);

        sql = "Select b From Bill b where "
                + " b.retired=false "
                + " and  b.toWebUser=:toWeb"
                + " and b.billType= :bTp"
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :net )";
            tmp.put("net", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  (upper(b.fromWebUser.webUserPerson.name) like :dep )";
            tmp.put("dep", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyBillItemTable() {
        createPharmacyBillItemTable(BillType.PharmacyPre, BillType.PharmacySale);
    }

    public void createPharmacyWholeBillItemTable() {
        createPharmacyBillItemTable(BillType.PharmacyWholesalePre, BillType.PharmacyWholeSale);
    }

    public void createPharmacyBillItemTable(BillType billType, BillType refBillType) {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", billType);
        m.put("rBType", refBillType);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);
        m.put("rClass", BilledBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class and type(bi.bill.referenceBill)=:rClass"
                + " and bi.bill.institution=:ins"
                + " and bi.bill.billType=:bType and "
                + " bi.bill.referenceBill.billType=:rBType "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(bi.bill.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(bi.netValue) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  (upper(bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyIssueBillItemTable() {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.PharmacyIssue);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class "
                + " and bi.bill.institution=:ins"
                + " and bi.bill.billType=:bType "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(bi.bill.toDepartment.name) like :deptName )";
            m.put("deptName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(bi.netValue) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  (upper(bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createStoreIssueBillItemTable() {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.StoreIssue);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class "
                + " and bi.bill.institution=:ins"
                + " and bi.bill.billType=:bType "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(bi.bill.toDepartment.name) like :deptName )";
            m.put("deptName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(bi.netValue) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  (upper(bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createErronousStoreIssueBillItemTable() {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.StoreIssue);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class "
                + " and bi.bill.institution=:ins "
                + " and bi.bill.billType=:bType "
                + " and bi.item.retired=true "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(bi.bill.toDepartment.name) like :deptName )";
            m.put("deptName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(bi.netValue) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  (upper(bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyAdjustmentBillItemTable() {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.PharmacyAdjustment);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class "
                + " and bi.bill.institution=:ins"
                + " and bi.bill.billType=:bType  "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  (upper(bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createStoreAdjustmentBillItemTable() {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.StoreAdjustment);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class "
                + " and bi.bill.institution=:ins"
                + " and bi.bill.billType=:bType  "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  (upper(bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createDrawerAdjustmentTable() {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.DrawerAdjustment);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", BilledBill.class);

        sql = "select bi from Bill bi"
                + " where  type(bi)=:class "
                + " and bi.institution=:ins"
                + " and bi.billType=:bType  "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.insId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyBillItemTableIssue() {
        createBillItemTableBht(BillType.StoreIssue);
    }

    public void createPharmacyBillItemTableBht() {
        createBillItemTableBht(BillType.PharmacyBhtPre);
    }

    public void createStoreBillItemTableBht() {
        createBillItemTableBht(BillType.StoreBhtPre);
    }

    public List<BillItem> getBillItem() {
        return billItem;
    }

    public void setBillItem(List<BillItem> billItem) {
        this.billItem = billItem;
    }

    public void createBillItemTableBht(BillType btp) {
        //  searchBillItems = null;
        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", btp);
        m.put("ins", getSessionController().getInstitution());
        m.put("class", PreBill.class);

        sql = "select bi from BillItem bi"
                + " where  type(bi.bill)=:class "
                + " and bi.bill.institution=:ins"
                + " and bi.bill.billType=:bType and "
                + " bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(bi.bill.patient.person.name) like :patientName )";
            m.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.deptId) like :billNo )";
            m.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(bi.netValue) like :total )";
            m.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :itm )";
            m.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  (upper(bi.item.code) like :cde )";
            m.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.patientEncounter.bhtNo) like :bht )";
            m.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

    }

    public void createPoRequestedAndApprovedPharmacy() {
        createPoRequestedAndApproved(InstitutionType.Dealer, BillType.PharmacyOrder);
    }

    public void createPoRequestedAndApprovedStore() {
        createPoRequestedAndApproved(InstitutionType.StoreDealor, BillType.StoreOrder);
    }

    public void createPoRequestedAndApproved(InstitutionType institutionType, BillType bt) {
        bills = null;
        String sql = "";
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where "
                + " b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.billType= :bTp  ";

        sql += createKeySql(tmp);

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("insTp", institutionType);
        tmp.put("bTp", bt);
        bills = getBillFacade().findBySQLWithoutCache(sql, tmp, TemporalType.TIMESTAMP, maxResult);

    }

    public void createApprovedPharmacy() {
        createApproved(InstitutionType.Dealer, BillType.PharmacyOrder);
    }

    public void createApprovedStore() {
        createApproved(InstitutionType.StoreDealor, BillType.StoreOrder);
    }

    public void createApproved(InstitutionType institutionType, BillType bt) {
        bills = null;
        String sql = "";
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where"
                + " b.referenceBill.creater is not null "
                + " and b.referenceBill.cancelled=false "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.billType= :bTp  ";

        sql += createKeySql(tmp);

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("insTp", institutionType);
        tmp.put("bTp", bt);
        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, maxResult);

    }

    public void createNotApprovedPharmacy() {
        createNotApproved(InstitutionType.Dealer, BillType.PharmacyOrder);
    }

    public void createNotApprovedStore() {
        createNotApproved(InstitutionType.StoreDealor, BillType.StoreOrder);
    }

    public void createNotApproved(InstitutionType institutionType, BillType bt) {
        bills = null;
        String sql = "";
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where"
                + "  b.referenceBill is null  "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false"
                + " and b.billType= :bTp ";

        sql += createKeySql(tmp);
        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("insTp", institutionType);
        tmp.put("bTp", bt);
        List<Bill> lst1 = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, maxResult);

        sql = "Select b From BilledBill b where "
                + " b.referenceBill.creater is null "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false "
                + " and b.billType= :bTp  ";

        sql += createKeySql(tmp);
        sql += " order by b.createdAt desc  ";

        List<Bill> lst2 = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, maxResult);

        sql = "Select b From BilledBill b where "
                + " b.referenceBill.creater is not null "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.referenceBill.cancelled=true "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false"
                + " and b.billType= :bTp ";

        sql += createKeySql(tmp);
        sql += " order by b.createdAt desc  ";

        List<Bill> lst3 = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, maxResult);

        lst1.addAll(lst2);
        lst1.addAll(lst3);

        bills = lst1;

    }

    private String createKeySql(HashMap tmp) {
        String sql = "";
        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :toIns )";
            tmp.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCreator() != null && !getSearchKeyword().getCreator().trim().equals("")) {
            sql += " and  (upper(b.creater.webUserPerson.name) like :crt )";
            tmp.put("crt", "%" + getSearchKeyword().getCreator().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDepartment() != null && !getSearchKeyword().getDepartment().trim().equals("")) {
            sql += " and  (upper(b.department.name) like :crt )";
            tmp.put("crt", "%" + getSearchKeyword().getDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  (upper(b.referenceBill.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :reqTotal )";
            tmp.put("reqTotal", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.referenceBill.netTotal) like :appTotal )";
            tmp.put("appTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        return sql;

    }

    private String keysForGrnReturn(HashMap tmp) {
        String sql = "";
        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.invoiceNumber) like :invoice )";
            tmp.put("invoice", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getRefBillNo() != null && !getSearchKeyword().getRefBillNo().trim().equals("")) {
            sql += " and  (upper(b.referenceBill.deptId) like :refNo )";
            tmp.put("refNo", "%" + getSearchKeyword().getRefBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  (upper(b.fromInstitution.name) like :frmIns )";
            tmp.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.referenceBill.netTotal) like :total )";
            tmp.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            tmp.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        return sql;
    }

    public void createGrnTable() {
        bills = null;
        String sql;
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where  b.retired=false and b.billType= :bTp "
                + " and b.institution=:ins and"
                + " b.createdAt between :fromDate and :toDate ";

        sql += keysForGrnReturn(tmp);

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("ins", getSessionController().getInstitution());
        tmp.put("bTp", BillType.PharmacyGrnBill);
        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getReturnBill(b, BillType.PharmacyGrnReturn));
        }

    }

    public void createGrnTableStore() {
        bills = null;
        String sql;
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where  b.retired=false and b.billType= :bTp "
                + " and b.institution=:ins and"
                + " b.createdAt between :fromDate and :toDate ";

        sql += keysForGrnReturn(tmp);

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("ins", getSessionController().getInstitution());
        tmp.put("bTp", BillType.StoreGrnBill);
        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getReturnBill(b, BillType.StoreGrnReturn));
        }

    }

    public void createGrnTableAllIns() {
        bills = null;
        String sql;
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where  b.retired=false and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        sql += keysForGrnReturn(tmp);

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        // tmp.put("ins", getSessionController().getInstitution());
        tmp.put("bTp", BillType.PharmacyGrnBill);
        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getReturnBill(b, BillType.PharmacyGrnReturn));
        }

    }

    public void createGrnTableAllInsStore() {
        bills = null;
        String sql;
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b where  b.retired=false and b.billType= :bTp "
                + " and b.createdAt between :fromDate and :toDate ";

        sql += keysForGrnReturn(tmp);

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        // tmp.put("ins", getSessionController().getInstitution());
        tmp.put("bTp", BillType.StoreGrnBill);
        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getReturnBill(b, BillType.StoreGrnReturn));
        }

    }

    private List<Bill> getReturnBill(Bill b, BillType bt) {
        String sql = "Select b From BilledBill b where b.retired=false and b.creater is not null"
                + " and  b.billType=:btp and "
                + " b.referenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", bt);
        return getBillFacade().findBySQL(sql, hm);
    }

    public void createPoTablePharmacy() {
        createPoTable(InstitutionType.Dealer, BillType.PharmacyOrderApprove, BillType.PharmacyGrnBill);
    }

    public void createPoTableStore() {
        createPoTable(InstitutionType.StoreDealor, BillType.StoreOrderApprove, BillType.StoreGrnBill);
    }

    public void createPoTable(InstitutionType institutionType, BillType bt, BillType referenceBillType) {
        bills = null;
        String sql;
        HashMap tmp = new HashMap();

        sql = "Select b From BilledBill b "
                + " where  b.retired=false"
                + " and b.billType= :bTp"
                + " and b.toInstitution.institutionType=:insTp "
                + " and  b.referenceBill.institution=:ins "
                + " and  b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            tmp.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :toIns )";
            tmp.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            tmp.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        tmp.put("toDate", getToDate());
        tmp.put("fromDate", getFromDate());
        tmp.put("insTp", institutionType);
        tmp.put("ins", getSessionController().getInstitution());
        tmp.put("bTp", bt);
        bills = getBillFacade().findBySQL(sql, tmp, TemporalType.TIMESTAMP, 50);

        for (Bill b : bills) {
            b.setListOfBill(getGrns(b, referenceBillType));
        }

    }

    private List<Bill> getGrns(Bill b, BillType billType) {
        String sql = "Select b From BilledBill b "
                + " where b.retired=false "
                + " and b.creater is not null"
                + " and b.billType=:btp"
                + " and b.referenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", billType);
        return getBillFacade().findBySQL(sql, hm);
    }

    private List<Bill> getIssudBills(Bill b) {
        String sql = "Select b From Bill b where b.retired=false and b.creater is not null"
                + " and b.billType=:btp and "
                + " b.referenceBill=:ref or b.backwardReferenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyTransferIssue);
        return getBillFacade().findBySQL(sql, hm);
    }

    private List<Bill> getIssuedBills(Bill b) {
        String sql = "Select b From Bill b where b.retired=false and b.creater is not null"
                + " and b.billType=:btp and "
                + " b.referenceBill=:ref and b.backwardReferenceBill=:ref";
        HashMap hm = new HashMap();
        hm.put("ref", b);
        hm.put("btp", BillType.PharmacyTransferIssue);
        return getBillFacade().findBySQL(sql, hm);
    }

    public void createDueFeeTable() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where b.retired=false and "
                + " b.bill.billType=:btp "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 and"
                + "  b.bill.createdAt between :fromDate"
                + " and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  (upper(b.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.billItem.item.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += "  order by b.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.OpdBill);

        billFees = getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);
        calTotal();
    }

    public void createDueFeeTableAll() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where b.retired=false and "
                + " b.bill.billType=:btp "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 and"
                + "  b.bill.createdAt between :fromDate"
                + " and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  (upper(b.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.billItem.item.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += "  order by b.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.OpdBill);

        billFees = getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        calTotal();
    }

    public void createDueFeeTableAndPaidFeeTable() {

        dueTotal = 0.0;
        doneTotal = 0.0;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where b.retired=false and "
                + " b.bill.billType=:btp "
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 and"
                + " b.bill.createdAt between :fromDate"
                + " and :toDate ";

        if (speciality != null) {
            sql += " and b.staff.speciality=:special ";
            temMap.put("special", speciality);
            //System.out.println(speciality);
        }

        if (staff != null) {
            sql += " and b.staff=:staff ";
            temMap.put("staff", staff);
            //System.out.println(staff);
        }

        if (item != null) {
            sql += " and b.billItem.item=:item ";
            temMap.put("item", item);
            //System.out.println(item);
        }

        sql += "  order by b.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.OpdBill);

        billFees = getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        for (BillFee bf : billFees) {
            dueTotal += bf.getFeeValue();
        }

        temMap.clear();
//        BillFee bf=new BillFee();
//        bf.getBillItem().getCreatedAt();
        sql = "select b.paidForBillFee from BillItem b where b.retired=false and "
                + " b.bill.billType=:btp "
                + " and b.referenceBill.billType=:refType "
                + " and b.paidForBillFee.bill.cancelled=false "
                //                + " and b.feeValue > 0 "
                + " and b.createdAt between :fromDate"
                + " and :toDate ";

//        sql = "Select b FROM BillItem b "
//                + " where b.retired=false "
//                + " and b.bill.billType=:bType "
//                + " and b.referenceBill.billType=:refType "
//                + " and b.createdAt between :fromDate and :toDate ";
        if (speciality != null) {
            sql += " and b.paidForBillFee.staff.speciality=:special ";
            temMap.put("special", speciality);
            //System.out.println(speciality);
        }

        if (staff != null) {
            sql += " and b.paidForBillFee.staff=:staff ";
            temMap.put("staff", staff);
            //System.out.println(staff);
        }

        if (item != null) {
            sql += " and b.paidForBillFee.billItem.item=:item ";
            temMap.put("item", item);
            //System.out.println(item);
        }

        sql += "  order by b.paidForBillFee.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.PaymentBill);
        temMap.put("refType", BillType.OpdBill);

        billFeesDone = getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        for (BillFee bf2 : billFeesDone) {
            doneTotal += bf2.getFeeValue();
        }

    }

    double totalPaying;

    public void fillDocPayingBillFee() {

        String sql;
        Map m = new HashMap();

        sql = "select b from BillItem b where b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.referenceBill.billType=:refType "
                //                + " and b.paidForBillFee.bill.cancelled=false "
                + " and b.createdAt between :fromDate and :toDate ";

        if (speciality != null) {
            sql += " and b.paidForBillFee.staff.speciality=:s ";
            m.put("s", speciality);
        }

        if (currentStaff != null) {
            sql += " and b.paidForBillFee.staff=:cs";
            m.put("cs", currentStaff);
        }

        sql += " order by b.bill.insId ";

        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        m.put("btp", BillType.PaymentBill);
        m.put("refType", BillType.OpdBill);

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

        totalPaying = 0.0;
        if (billItems == null) {
            return;
        }
        for (BillItem dFee : billItems) {
            totalPaying += dFee.getPaidForBillFee().getFeeValue();
        }

    }

    public void fillDocPayingBill() {

        String sql;
        Map m = new HashMap();

        sql = "select distinct(b.bill) from BillItem b where b.retired=false "
                + " and b.bill.billType=:btp "
                + " and b.referenceBill.billType=:refType "
                //                + " and b.paidForBillFee.bill.cancelled=false "
                + " and b.createdAt between :fromDate and :toDate ";

        if (speciality != null) {
            sql += " and b.paidForBillFee.staff.speciality=:s ";
            m.put("s", speciality);
        }

        if (currentStaff != null) {
            sql += " and b.paidForBillFee.staff=:cs";
            m.put("cs", currentStaff);
        }

        sql += " order by b.bill.insId ";

        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        m.put("btp", BillType.PaymentBill);
        m.put("refType", BillType.OpdBill);

//        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        bills = getBillFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

        totalPaying = 0.0;
        if (bills == null) {
            return;
        }
        for (Bill b : bills) {
            totalPaying += b.getNetTotal();
        }

    }

    public void createDueFeeTableInward() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where "
                + " b.retired=false "
                + " and (b.bill.billType=:btp or b.bill.billType=:btp2 )"
                + " and b.bill.cancelled=false "
                //Starting of newly added code 
                + " and b.bill.refunded=false "
                //Ending of newly added code 
                + " and (b.feeValue - b.paidValue) > 0"
                //                + " and  b.bill.billTime between :fromDate and :toDate ";
                + " and  b.bill.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  (upper(b.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.billItem.item.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += "  order by b.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.InwardBill);
        temMap.put("btp2", BillType.InwardProfessional);

        billFees = getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);
        calTotal();
    }

    public void createDueFeeTableInwardAll() {

        String sql;
        Map temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billClass", BilledBill.class);
        temMap.put("btp", BillType.InwardBill);
        temMap.put("btp2", BillType.InwardProfessional);

        sql = "select b from BillFee b where "
                + " b.retired=false "
                + " and (b.bill.billType=:btp or b.bill.billType=:btp2 )"
                + " and b.bill.cancelled=false "
                + " and type(b.bill)=:billClass "
                //Starting of newly added code 
                + " and b.bill.refunded=false "
                //Ending of newly added code 
                + " and (b.feeValue - b.paidValue) > 0"
                //                + " and  b.bill.billTime between :fromDate and :toDate ";
                + " and  b.bill.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  (upper(b.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.billItem.item.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += "  order by b.staff.id    ";

        //System.out.println("temMap = " + temMap);
        //System.out.println("sql = " + sql);
        billFees = getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        calTotal();
    }

    public void createDueFeeTableInwardAllWithCancelled() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where "
                + " b.retired=false "
                + " and (b.bill.billType=:btp or b.bill.billType=:btp2 )"
                + " and (b.feeValue - b.paidValue) > 0"
                + " and  b.bill.billDate between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  (upper(b.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.billItem.item.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += "  order by b.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.InwardBill);
        temMap.put("btp2", BillType.InwardProfessional);
        //System.out.println("temMap = " + temMap);
        //System.out.println("sql = " + sql);

        billFees = getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        calTotal();
    }

    double total;

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    private void calTotal() {
        total = 0;
        if (billFees == null) {
            return;
        }

        for (BillFee billFee : billFees) {
            total += billFee.getFeeValue();
        }
    }

    private void calTotalBillItem() {
        total = 0;
        if (billItems == null) {
            return;
        }

        for (BillItem billFee : billItems) {
            total += billFee.getNetValue();
        }
    }

    public void createDueFeeReportInward() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where "
                + " b.retired=false "
                + " and (b.bill.billType=:btp or b.bill.billType=:btp2 )"
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0"
                + " and  b.bill.billDate between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  (upper(b.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.billItem.item.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientEncounter() != null) {
            sql += " and  b.bill.patientEncounter =:pe";
            temMap.put("pe", getSearchKeyword().getPatientEncounter());
        }

        if (getSearchKeyword().getAdmissionType() != null) {
            sql += " and  b.bill.patientEncounter.admissionType =:adty";
            temMap.put("adty", getSearchKeyword().getAdmissionType());
        }

        if (getSearchKeyword().getPaymentMethod() != null) {
            sql += " and  b.bill.patientEncounter.paymentMethod =:payme";
            temMap.put("payme", getSearchKeyword().getPaymentMethod());
        }

        if (getSearchKeyword().getIns() != null) {
            sql += " and  b.bill.patientEncounter.creditCompany=:is";
            temMap.put("is", getSearchKeyword().getIns());
        }

        sql += "  order by b.staff.id    ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.InwardBill);
        temMap.put("btp2", BillType.InwardProfessional);

        billFees = getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createPaymentTable() {
        billItems = null;
        HashMap temMap = new HashMap();
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.referenceBill.billType=:refType "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getInsId() != null && !getSearchKeyword().getInsId().trim().equals("")) {
            sql += " and  (upper(b.bill.insId) like :insId )";
            temMap.put("insId", "%" + getSearchKeyword().getInsId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.billItem.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.OpdBill);

        billItems = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createProfessionalPaymentTableInward() {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", BilledBill.class);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                //                + " and b.bill.cancelled=false "
                + " and type(b.bill)=:bclass"
                + " and b.bill.billType=:bType "
                + " and (b.referenceBill.billType=:refType "
                + " or b.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getInsId() != null && !getSearchKeyword().getInsId().trim().equals("")) {
            sql += " and  (upper(b.bill.insId) like :insId )";
            temMap.put("insId", "%" + getSearchKeyword().getInsId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.billItem.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);

        billItems = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createProfessionalPaymentTableInwardAll() {
        billItems = null;
        HashMap temMap = new HashMap();
//        temMap.put("bclass", BilledBill.class);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                //                + " and b.bill.cancelled=false "
                //                + " and type(b.bill)=:bclass"
                + " and (b.referenceBill.billType=:refType "
                + " or b.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getInsId() != null && !getSearchKeyword().getInsId().trim().equals("")) {
            sql += " and  (upper(b.bill.insId) like :insId )";
            temMap.put("insId", "%" + getSearchKeyword().getInsId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.staff.speciality.name) like :special )";
            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.staff.person.name) like :staff )";
            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.billItem.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.paidForBillFee.feeValue) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        billItems = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        calTotalBillItem();
    }

    public void createBillItemTableByKeyword() {

        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.OpdBill);
        m.put("ins", getSessionController().getInstitution());

        sql = "select bi from BillItem bi where bi.bill.institution=:ins "
                + " and bi.bill.billType=:bType "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (searchKeyword.getPatientName() != null && !searchKeyword.getPatientName().trim().equals("")) {
            sql += " and  (upper(bi.bill.patient.person.name) like :patientName )";
            m.put("patientName", "%" + searchKeyword.getPatientName().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getPatientPhone() != null && !searchKeyword.getPatientPhone().trim().equals("")) {
            sql += " and  (upper(bi.bill.patient.person.phone) like :patientPhone )";
            m.put("patientPhone", "%" + searchKeyword.getPatientPhone().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getBillNo() != null && !searchKeyword.getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.insId) like :billNo )";
            m.put("billNo", "%" + searchKeyword.getBillNo().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getItemName() != null && !searchKeyword.getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :itemName )";
            m.put("itemName", "%" + searchKeyword.getItemName().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getToInstitution() != null && !searchKeyword.getToInstitution().trim().equals("")) {
            sql += " and  (upper(bi.bill.toInstitution.name) like :toIns )";
            m.put("toIns", "%" + searchKeyword.getToInstitution().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";
        //System.err.println("Sql " + sql);

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP, 50);

        //   searchBillItems = new LazyBillItem(tmp);
    }

    public void createBillItemTableByKeywordAll() {

        String sql;
        Map m = new HashMap();
        m.put("toDate", toDate);
        m.put("fromDate", fromDate);
        m.put("bType", BillType.OpdBill);
        m.put("ins", getSessionController().getInstitution());

        sql = "select bi from BillItem bi where bi.bill.institution=:ins "
                + " and bi.bill.billType=:bType "
                + " and bi.createdAt between :fromDate and :toDate ";

        if (searchKeyword.getPatientName() != null && !searchKeyword.getPatientName().trim().equals("")) {
            sql += " and  (upper(bi.bill.patient.person.name) like :patientName )";
            m.put("patientName", "%" + searchKeyword.getPatientName().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getPatientPhone() != null && !searchKeyword.getPatientPhone().trim().equals("")) {
            sql += " and  (upper(bi.bill.patient.person.phone) like :patientPhone )";
            m.put("patientPhone", "%" + searchKeyword.getPatientPhone().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getBillNo() != null && !searchKeyword.getBillNo().trim().equals("")) {
            sql += " and  (upper(bi.bill.insId) like :billNo )";
            m.put("billNo", "%" + searchKeyword.getBillNo().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getItemName() != null && !searchKeyword.getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :itemName )";
            m.put("itemName", "%" + searchKeyword.getItemName().trim().toUpperCase() + "%");
        }

        if (searchKeyword.getToInstitution() != null && !searchKeyword.getToInstitution().trim().equals("")) {
            sql += " and  (upper(bi.bill.toInstitution.name) like :toIns )";
            m.put("toIns", "%" + searchKeyword.getToInstitution().trim().toUpperCase() + "%");
        }

        sql += " order by bi.id desc  ";
        //System.err.println("Sql " + sql);

        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

        //   searchBillItems = new LazyBillItem(tmp);
    }

    public void createPatientInvestigationsTable() {

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  ";

//        String sql = "select pi from PatientInvestigation pi where "
//                + " pi.billItem.bill.createdAt between :fromDate and :toDate  ";
        Map temMap = new HashMap();

//        if(webUserController.hasPrivilege("LabSearchBillLoggedInstitution")){
//            System.out.println("inside ins");
//            sql+="and b.institution =:ins ";
//            temMap.put("ins", getSessionController().getInstitution());
//        }
        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(p.name) like :patientName )";
//            sql += " and  (upper(pi.billItem.bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(p.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(i.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by pi.id desc  ";
//    

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        patientInvestigations = getPatientInvestigationFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createPatientInvestigationsTableByLoggedInstitution() {

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  "
                + " and b.institution =:ins ";

//        String sql = "select pi from PatientInvestigation pi where "
//                + " pi.billItem.bill.createdAt between :fromDate and :toDate  ";
        Map temMap = new HashMap();
        temMap.put("ins", getSessionController().getInstitution());

//        if(webUserController.hasPrivilege("LabSearchBillLoggedInstitution")){
//            System.out.println("inside ins");
//            sql+="and b.institution =:ins ";
//            temMap.put("ins", getSessionController().getInstitution());
//        }
        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(p.name) like :patientName )";
//            sql += " and  (upper(pi.billItem.bill.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(p.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(i.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by pi.id desc  ";
//    

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        patientInvestigations = getPatientInvestigationFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void searchPatientInvestigations() {
        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  ";
        Map temMap = new HashMap();
        sql += " order by pi.id ";
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        patientInvestigations = getPatientInvestigationFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
    }

    public String fillUserPatientReport() {
        String jpql;
        Map m = new HashMap();
        m.put("pn", getSessionController().getPhoneNo());
        m.put("bn", getSessionController().getBillNo());
        //System.out.println("getSessionController().getPhoneNo() = " + getSessionController().getPhoneNo());
        //System.out.println("getSessionController().getBillNo() = " + getSessionController().getBillNo());
        jpql = " select pr from PatientInvestigation pr where pr.retired=false and "
                + " upper(pr.billItem.bill.patient.person.phone)=:pn and "
                + " (upper(pr.billItem.bill.insId)=:bn or upper(pr.billItem.bill.deptId)=:bn) "
                + " order by pr.id desc ";
        userPatientInvestigations = patientInvestigationFacade.findBySQL(jpql, m, 20);
        //System.out.println("m = " + m);
        //System.out.println("userPatientInvestigations = " + userPatientInvestigations);

        return "/reports_list";
    }

    public void createPatientInvestigationsTableSingle() {
        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient p where "
                + " p=:pt ";
        Map temMap = new HashMap();
        sql += " order by pi.id desc  ";
        temMap.put("pt", getTransferController().getPatient());
//        ////System.out.println("temMap = " + temMap);
//        ////System.out.println("sql = " + sql);
        patientInvestigationsSigle = getPatientInvestigationFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);
//        ////System.out.println("patientInvestigations.size() = " + patientInvestigations.size());
    }

    public void createPatientInvestigationsTableAll() {

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  ";

        Map temMap = new HashMap();

//        if(webUserController.hasPrivilege("LabSearchBillLoggedInstitution")){
//            System.out.println("inside ins");
//            sql+="and b.institution =:ins ";
//            temMap.put("ins", getSessionController().getInstitution());
//        }
        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(p.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(p.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(i.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by pi.id desc  ";
//    

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        patientInvestigations = getPatientInvestigationFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createPatientInvestigationsTableAllByLoggedInstitution() {

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  "
                + " and b.institution =:ins ";

        Map temMap = new HashMap();
        temMap.put("ins", getSessionController().getInstitution());

//        if(webUserController.hasPrivilege("LabSearchBillLoggedInstitution")){
//            System.out.println("inside ins");
//            sql+="and b.institution =:ins ";
//            temMap.put("ins", getSessionController().getInstitution());
//        }
        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(p.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(p.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(i.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by pi.id desc  ";
//    

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        patientInvestigations = getPatientInvestigationFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createPreBillsForReturn() {
        createPreBillsForReturn(BillType.PharmacyPre, BillType.PharmacySale);
    }

    public void createWholePreBillsForReturn() {
        createPreBillsForReturn(BillType.PharmacyWholesalePre, BillType.PharmacyWholeSale);
    }

    public void createPreBillsForReturn(BillType billType, BillType refBillType) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b where b.billType = :billType and "
                + " b.institution=:ins and (b.billedBill is null) and "
                + " b.referenceBill.billType=:refBillType "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false "
                // for remove cancel bills
                + " and b.referenceBill.cancelled=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("billType", billType);
        temMap.put("refBillType", refBillType);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("ins", getSessionController().getInstitution());

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public void createPreBillsNotPaid() {

        bills = getBillBean().billsForTheDayNotPaid(BillType.PharmacyPre, getSessionController().getDepartment());

    }

    public void createWholePreBillsNotPaid() {

        bills = getBillBean().billsForTheDayNotPaid(BillType.PharmacyWholesalePre, getSessionController().getDepartment());

    }

    public void addToStock() {
        for (Bill b : getSelectedBills()) {
            if (b.checkActiveCashPreBill()) {

                continue;
            }

            Bill prebill = getPharmacyBean().reAddToStock(b, getSessionController().getLoggedUser(),
                    getSessionController().getDepartment(), BillNumberSuffix.PRECAN);

            if (prebill != null) {
                b.setCancelled(true);
                b.setCancelledBill(prebill);
                getBillFacade().edit(b);
            }
        }

        createPreBillsNotPaid();

    }

    public void cancelIssueToUnitBills() {
        if (cancellingIssueBill == null) {
            JsfUtil.addErrorMessage("Select a bill to cancel");
            return;
        }
        Bill b = cancellingIssueBill;

        if (b.isCancelled() || b.isRefunded()) {
            JsfUtil.addErrorMessage("Can not cancel already cancelled or returned bills");
            return;
        }
        if (b instanceof PreBill) {
            Bill prebill = getPharmacyBean().readdStockForIssueBills((PreBill) b, getSessionController().getLoggedUser(),
                    getSessionController().getDepartment(), BillNumberSuffix.DIC);
            b.setCancelled(true);
            b.setCancelledBill(prebill);
            getBillFacade().edit(b);
            JsfUtil.addSuccessMessage("Cancelled");
        } else {
            JsfUtil.addErrorMessage("Not an Issue Bill. Can not cancell");
        }

    }

    private String createPharmacyPayKeyword(Map temMap) {
        String sql = "";
        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.deptId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        return sql;

    }

    public void createOpdBathcBillPreTable() {
        aceptPaymentBills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billType = :billType "
                + " and b.institution=:ins"
                //                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";
//                + " and b.deptId is not null ";

        sql += createPharmacyPayKeyword(temMap);
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.OpdBathcBillPre);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        aceptPaymentBills = getBillFacade().findBySQLWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 25);
        System.out.println("aceptPaymentBills = " + aceptPaymentBills);
    }

    public void createOpdBathcBillPreTablePaidOnly() {
        aceptPaymentBills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billType = :billType "
                + " and b.institution=:ins"
                + " and b.referenceBill.balance=0 "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";
//                + " and b.deptId is not null ";

        sql += createPharmacyPayKeyword(temMap);
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.OpdBathcBillPre);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        aceptPaymentBills = getBillFacade().findBySQLWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 25);
        System.out.println("aceptPaymentBills = " + aceptPaymentBills);
    }

    public void createOpdBathcBillPreTableNotPaidOly() {
        aceptPaymentBills = new ArrayList<>();
        String sql;
        Map temMap = new HashMap();

        List<Bill> abs = new ArrayList<>();

        sql = "select b from PreBill b "
                + " where b.billType = :billType "
                + " and b.institution=:ins"
                //                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";
//                + " and b.deptId is not null ";

        sql += createPharmacyPayKeyword(temMap);
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.OpdBathcBillPre);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        abs = getBillFacade().findBySQLWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 50);

        List<Bill> pbs = new ArrayList<>();
        Map temMap2 = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billType = :billType "
                + " and b.institution=:ins"
                + " and b.referenceBill.balance=0 "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";
//                + " and b.deptId is not null ";

        sql += createPharmacyPayKeyword(temMap2);
        sql += " order by b.createdAt desc  ";
//    
        temMap2.put("billType", BillType.OpdBathcBillPre);
        temMap2.put("toDate", getToDate());
        temMap2.put("fromDate", getFromDate());
        temMap2.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        pbs = getBillFacade().findBySQLWithoutCache(sql, temMap2, TemporalType.TIMESTAMP, 50);

        System.out.println("pbs = " + pbs);
        System.out.println("abs = " + abs);
        System.out.println("aceptPaymentBills = " + aceptPaymentBills);
        abs.removeAll(pbs);
        aceptPaymentBills.addAll(abs);
        System.out.println("aceptPaymentBills = " + aceptPaymentBills);
    }

    public void createOpdPreTable() {
        createPreTable(BillType.OpdPreBill);
    }

    public void createOpdPreTableNotPaid() {
        createPreTableNotPaid(BillType.OpdPreBill);
    }

    public void createOpdPreTablePaid() {
        createPreTablePaid(BillType.OpdPreBill);
    }

    public void createPharmacyPreTable() {
        createPreTable(BillType.PharmacyPre);
    }

    public void createPharmacyPreTableNotPaid() {
        createPreTableNotPaid(BillType.PharmacyPre);
    }

    public void createPharmacyPreTablePaid() {
        createPreTablePaid(BillType.PharmacyPre);
    }

    public void createPreTable(BillType bt) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billType = :billType "
                + " and b.institution=:ins"
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false "
                + " and b.deptId is not null ";

        sql += createPharmacyPayKeyword(temMap);
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", bt);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQLWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 25);

    }

    public void createPreTableNotPaid(BillType bt) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false "
                + " and b.deptId is not null "
                + " and b.cancelled=false"
                + " and b.referenceBill is null ";

        sql += createPharmacyPayKeyword(temMap);
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", bt);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQLWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 25);

    }

    public void createPreTablePaid(BillType bt) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from PreBill b "
                + " where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false "
                + " and b.deptId is not null "
                + " and b.cancelled=false"
                + " and b.referenceBill is not null ";

        sql += createPharmacyPayKeyword(temMap);
        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", bt);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQLWithoutCache(sql, temMap, TemporalType.TIMESTAMP, 25);

    }

    public void createGrnPaymentTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b"
                + " where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.GrnPayment);
        temMap.put("insTp", InstitutionType.Dealer);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createGrnPaymentTableStore() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b"
                + " where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.GrnPayment);
        temMap.put("insTp", InstitutionType.StoreDealor);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createPharmacyPayment() {
        InstitutionType[] institutionTypes = {InstitutionType.Dealer};
        createGrnPaymentTable(Arrays.asList(institutionTypes), BillType.GrnPayment);
    }

    public void createStorePayment() {
        InstitutionType[] institutionTypes = {InstitutionType.StoreDealor};
        createGrnPaymentTable(Arrays.asList(institutionTypes), BillType.GrnPayment);
    }

    public void createStorePaharmacyPayment() {
        InstitutionType[] institutionTypes = {InstitutionType.Dealer, InstitutionType.StoreDealor};
        createGrnPaymentTable(Arrays.asList(institutionTypes), BillType.GrnPayment);
    }

    public void createPharmacyPaymentPre() {
        InstitutionType[] institutionTypes = {InstitutionType.Dealer};
        createGrnPaymentTable(Arrays.asList(institutionTypes), BillType.GrnPaymentPre);
    }

    public void createStorePaymentPre() {
        InstitutionType[] institutionTypes = {InstitutionType.StoreDealor};
        createGrnPaymentTable(Arrays.asList(institutionTypes), BillType.GrnPaymentPre);
    }

    public void createStorePaharmacyPaymentPre() {
        InstitutionType[] institutionTypes = {InstitutionType.Dealer, InstitutionType.StoreDealor};
        createGrnPaymentTable(Arrays.asList(institutionTypes), BillType.GrnPaymentPre);
    }

    private void createGrnPaymentTable(List<InstitutionType> institutionTypes, BillType billType) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b "
                + " where b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.toInstitution.institutionType in :insTp "
                + " and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", billType);
        temMap.put("insTp", institutionTypes);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
      //  temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createGrnPaymentTableAllStore() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b "
                + " where b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.toInstitution.institutionType=:insTp "
                + " and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.GrnPayment);
        temMap.put("insTp", InstitutionType.StoreDealor);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
      //  temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    @Inject
    WebUserController webUserController;

    public void createTableByKeyword() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (!webUserController.hasPrivilege("AdminFilterWithoutDepartment")) {
            sql += " and b.institution=:ins ";
            temMap.put("ins", getSessionController().getInstitution());
        }

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  ((upper(b.insId) like :billNo )or(upper(b.deptId) like :billNo ))";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.OpdBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createTableCashIn() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false"
                + " and b.creater=:w ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  (upper(b.fromWebUser.webUserPerson.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.CashIn);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("w", getSessionController().getLoggedUser());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createTableCashOut() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType "
                + " and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false"
                + " and b.creater=:w ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  (upper(b.toWebUser.webUserPerson.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.CashOut);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("w", getSessionController().getLoggedUser());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createSearchBill() {
        bills = null;
        String sql;
        Map m = new HashMap();

        sql = "select b from Bill b where "
                + " b.id is not null ";

        if (!getSearchKeyword().getInsId().isEmpty()) {
            sql += " and b.insId=:insId  ";
            m.put("insId", getSearchKeyword().getInsId());
        }

        if (!getSearchKeyword().getDeptId().isEmpty()) {
            sql += " and b.deptId=:deptId  ";
            m.put("deptId", getSearchKeyword().getDeptId());
        }

        if (!getSearchKeyword().getBhtNo().trim().isEmpty()) {
            sql += " and b.patientEncounter.bhtNo=:bht";
            m.put("bht", getSearchKeyword().getBhtNo());
        }
        sql += " order by b.insId ";

//        m.put("class", PreBill.class);
        bills = getBillFacade().findBySQL(sql, m);
    }

    public void createSearchAll() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where "
                + " b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getInstitution() != null && !getSearchKeyword().getInstitution().trim().equals("")) {
            sql += " and  (upper(b.institution.name) like :ins )";
            temMap.put("ins", "%" + getSearchKeyword().getInstitution().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getToInstitution() != null && !getSearchKeyword().getToInstitution().trim().equals("")) {
            sql += " and  (upper(b.toInstitution.name) like :toIns )";
            temMap.put("toIns", "%" + getSearchKeyword().getToInstitution().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getToDepartment() != null && !getSearchKeyword().getToDepartment().trim().equals("")) {
            sql += " and  (upper(b.toDepartment.name) like :toDept )";
            temMap.put("toDept", "%" + getSearchKeyword().getToDepartment().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  (upper(b.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }
        if (getSearchKeyword().getFromDepartment() != null && !getSearchKeyword().getFromDepartment().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getFromDepartment().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPaymentScheme() != null && !getSearchKeyword().getPaymentScheme().trim().equals("")) {
            sql += " and  (upper(b.paymentScheme.name) like :pScheme )";
            temMap.put("pScheme", "%" + getSearchKeyword().getPaymentScheme().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPaymentmethod() != null && !getSearchKeyword().getPaymentmethod().trim().equals("")) {
            sql += " and  (upper(b.paymentMethod) like :pm )";
            temMap.put("pm", "%" + getSearchKeyword().getPaymentmethod().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getInsId() != null && !getSearchKeyword().getInsId().trim().equals("")) {
            sql += " and  (upper(b.insId) like :insId )";
            temMap.put("insId", "%" + getSearchKeyword().getInsId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getDeptId() != null && !getSearchKeyword().getDeptId().trim().equals("")) {
            sql += " and  (upper(b.insId) like :deptId )";
            temMap.put("deptId", "%" + getSearchKeyword().getDeptId().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createCollectingTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where "
                + " b.billType = :billType and "
                + " b.institution=:ins "
                + " and b.createdAt between :fromDate "
                + " and :toDate and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.LabBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createCollectingTableAll() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.LabBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createCollectingBillItemTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select bi from BillItem bi join bi.bill b where "
                + " b.billType = :billType and "
                + " b.institution=:ins "
                + " and b.createdAt between :fromDate "
                + " and :toDate and b.retired=false "
                + " and type(b)=:class ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.LabBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("class", BilledBill.class);

        System.err.println("Sql " + sql);
        billItems = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);
        //System.out.println("billItems = " + billItems);

    }

    public void createCollectingBillItemTableAll() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select bi from BillItem bi join bi.bill b where "
                + " b.billType = :billType and "
                + " b.institution=:ins "
                + " and b.createdAt between :fromDate "
                + " and :toDate and b.retired=false "
                + " and type(b)=:class ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(bi.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.LabBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("class", BilledBill.class);

        System.err.println("Sql " + sql);
        billItems = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);
        //System.out.println("billItems = " + billItems);

    }

    public void createCreditTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b "
                + " where b.billType = :billType"
                + "  and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  (upper(b.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBank() != null && !getSearchKeyword().getBank().trim().equals("")) {
            sql += " and  (upper(b.bank.name) like :bank )";
            temMap.put("bank", "%" + getSearchKeyword().getBank().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.chequeRefNo) like :num )";
            temMap.put("num", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.CashRecieveBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createCreditTableBillItemAll() {
        createCreditTableBillItem(null, true);
    }

    public void createCreditTableBillItemOpd() {
        createCreditTableBillItem(BillType.OpdBill, false);
    }

    public void createCreditTableBillItemBht() {
        createCreditTableBillItem(null, false);
    }

    public void createCreditTableBillItem(BillType billType, boolean all) {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillItem b "
                + " where b.bill.billType = :billType"
                + "  and b.bill.institution=:ins "
                + " and b.bill.createdAt between :fromDate and :toDate "
                + " and b.bill.retired=false ";

        if (!all) {
            if (billType != null) {
                sql += " and b.referenceBill.billType=:refBtp";
                temMap.put("refBtp", billType);
            } else {
                sql += " and b.patientEncounter is not null ";
            }

        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.bill.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  (upper(b.bill.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBank() != null && !getSearchKeyword().getBank().trim().equals("")) {
            sql += " and  (upper(b.bill.bank.name) like :bank )";
            temMap.put("bank", "%" + getSearchKeyword().getBank().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.bill.chequeRefNo) like :num )";
            temMap.put("num", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.bill.createdAt desc  ";
//    
        temMap.put("billType", BillType.CashRecieveBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        billItems = getBillItemFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public List<Bill> getChannelPaymentBillsOld() {
        if (bills == null) {
            String sql;
            Map temMap = new HashMap();
            if (bills == null) {
                sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.id in"
                        + "(Select bt.bill.id From BillItem bt Where bt.referenceBill.billType=:btp"
                        + " or bt.referenceBill.billType=:btp2) and b.billType=:type and b.createdAt "
                        + "between :fromDate and :toDate order by b.id";

                temMap.put("toDate", getToDate());
                temMap.put("fromDate", getFromDate());
                temMap.put("type", BillType.PaymentBill);
                temMap.put("btp", BillType.ChannelPaid);
                temMap.put("btp2", BillType.ChannelCredit);
                bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 100);

                if (bills == null) {
                    bills = new ArrayList<>();
                }
            }
        }
        return bills;

    }

    public void channelPaymentBills() {
        String sql;
        Map m = new HashMap();

        BillType[] bt = {
            BillType.ChannelOnCall,
            BillType.ChannelCash,
            BillType.ChannelAgent,
            BillType.ChannelStaff,
            BillType.ChannelCredit,};

        List<BillType> bts = Arrays.asList(bt);

        sql = "SELECT bi FROM BillItem bi WHERE bi.retired = false "
                + " and bi.bill.billType=:bt "
                + " and bi.paidForBillFee.bill.billType in :bts "
                + " and bi.createdAt between :fromDate and :toDate ";

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("bt", BillType.PaymentBill);
        m.put("bts", bts);
        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

    }

    public void channelAgentPaymentBills() {
        String sql;
        Map m = new HashMap();

        sql = "SELECT bi FROM BillItem bi WHERE bi.retired = false "
                + " and bi.bill.billType=:bt"
                + " and bi.createdAt between :fromDate and :toDate ";

        if (getSearchKeyword().getIns() != null) {
            sql += " and bi.bill.toInstitution=:ins";
            m.put("ins", getSearchKeyword().getIns());
        }

        m.put("fromDate", getFromDate());
        m.put("toDate", getToDate());
        m.put("bt", BillType.ChannelAgencyPayment);
        billItems = getBillItemFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

    }

    public void createChannelDueBillFeeOld() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BillFee b where b.retired=false and (b.bill.billType=:btp or b.bill.billType=:btp2) "
                + " and b.bill.id in(Select bs.bill.id From BillSession bs where bs.retired=false ) "
                + "and b.bill.cancelled=false and (b.feeValue - b.paidValue) > 0 and  "
                + "b.bill.createdAt between :fromDate and :toDate order by b.staff.id  ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.ChannelPaid);
        temMap.put("btp2", BillType.ChannelCredit);

        billFees = getBillFeeFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createChannelDueBillFee() {
        selectedServiceSession = null;

        BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        List<BillType> bts = Arrays.asList(billTypes);
        String sql = " SELECT b FROM BillFee b "
                + "  where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.bill.refunded=false "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt ";

        HashMap hm = new HashMap();
        if (getFromDate() != null && getToDate() != null) {
            sql += " and b.bill.appointmentAt between :frm and  :to";
            hm.put("frm", getFromDate());
            hm.put("to", getToDate());
        }

        if (getSelectedServiceSession() != null) {
            sql += " and bs.serviceSession=:ss";
            hm.put("ss", getSelectedServiceSession());
        }

        if (getCurrentStaff() != null) {
            sql += " and b.staff=:stf ";
            hm.put("stf", getCurrentStaff());
        }

        //hm.put("ins", sessionController.getInstitution());
        hm.put("bt", bts);
        hm.put("ftp", FeeType.Staff);
        hm.put("class", BilledBill.class);
        billFees = billFeeFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }

    public void createChannelDueBillFeeByAgent() {
        selectedServiceSession = null;

        //BillType[] billTypes = {BillType.ChannelAgent, BillType.ChannelCash, BillType.ChannelOnCall, BillType.ChannelStaff};
        // List<BillType> bts = Arrays.asList(billTypes);
        String sql = " SELECT b FROM BillFee b "
                + "  where type(b.bill)=:class "
                + " and b.bill.retired=false "
                + " and b.bill.paidAmount!=0 "
                + " and b.bill.refunded=false "
                + " and b.fee.feeType=:ftp"
                + " and b.bill.cancelled=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.bill.billType in :bt ";

        HashMap hm = new HashMap();
        if (getFromDate() != null && getToDate() != null) {
            sql += " and b.bill.appointmentAt between :frm and  :to";
            hm.put("frm", getFromDate());
            hm.put("to", getToDate());
        }

        if (getSelectedServiceSession() != null) {
            sql += " and bs.serviceSession=:ss";
            hm.put("ss", getSelectedServiceSession());
        }

//        if (getCurrentStaff() != null) {
//            sql += " and b.staff=:stf ";
//            hm.put("stf", getCurrentStaff());
//        }
        //hm.put("ins", sessionController.getInstitution());
        hm.put("bt", BillType.ChannelAgent);
        hm.put("ftp", FeeType.OtherInstitution);
        hm.put("class", BilledBill.class);
        billFees = billFeeFacade.findBySQL(sql, hm, TemporalType.TIMESTAMP);

    }

    public void createAgentPaymentTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType "
                + " and b.institution=:ins and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getFromInstitution() != null && !getSearchKeyword().getFromInstitution().trim().equals("")) {
            sql += " and  (upper(b.fromInstitution.name) like :frmIns )";
            temMap.put("frmIns", "%" + getSearchKeyword().getFromInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.fromInstitution.institutionCode) like :num )";
            temMap.put("num", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";

        temMap.put("billType", BillType.AgentPaymentReceiveBill);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardServiceTable() {

        String sql;
        Map temMap = new HashMap();
        sql = "select (b.bill) from BillItem b where "
                + " b.bill.billType = :billType "
                + " and b.bill.createdAt between :fromDate and :toDate"
                + " and b.bill.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.bill.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by b.bill.insId desc ";
        temMap.put("billType", BillType.InwardBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createInwardServiceTablebyLoggedDepartment() {

        String sql;
        Map temMap = new HashMap();
        sql = "select (b.bill) from BillItem b where "
                + " b.bill.billType = :billType "
                + " and b.bill.createdAt between :fromDate and :toDate"
                + " and b.bill.retired=false  "
                + " and b.bill.department = :dep";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.bill.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.bill.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.bill.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.item.name) like :item )";
            temMap.put("item", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        sql += " order by b.bill.insId desc ";
        temMap.put("dep", getSessionController().getDepartment());
        temMap.put("billType", BillType.InwardBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createInwardServiceTableDischarged() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.createdAt is not null "
                + " and b.patientEncounter.discharged=true and"
                + " b.billType = :billType and b.createdAt between :fromDate and :toDate "
                + "and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += "order by b.insId desc";

        temMap.put("billType", BillType.InwardBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardFinalBillsCheck() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where"
                + " b.billType = :billType and "
                + " b.patientEncounter.dateOfDischarge between :fromDate and :toDate "
                + "and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardFinalBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardFinalBills() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where"
                + " b.billType = :billType and "
                + " b.createdAt between :fromDate and :toDate "
                + "and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardFinalBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardIntrimBills() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where"
                + " b.billType = :billType and "
                + " b.createdAt between :fromDate and :toDate "
                + "and b.retired=false ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardIntrimBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardPaymentBills() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where"
                + " b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardRefundBills() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from RefundBill b where "
                + " b.billType = :billType "
                + " and b.billedBill is null "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardSurgeryBills() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where "
                + " b.billType = :billType and "
                + " b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getStaffName() != null
                && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.staff.person.name) like :staffName )";
            temMap.put("staffName", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientName() != null
                && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null
                && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null
                && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null
                && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null
                && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.procedure.item.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null
                && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.SurgeryBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardSurgeryBillsReport() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where "
                + " b.billType = :billType and "
                + " b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getStaffName() != null
                && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.staff.person.name) like :staffName )";
            temMap.put("staffName", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientName() != null
                && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null
                && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null
                && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null
                && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null
                && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.procedure.item.name) like :itm )";
            temMap.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getTotal() != null
                && !getSearchKeyword().getTotal().trim().equals("")) {
            sql += " and  (upper(b.total) like :total )";
            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.SurgeryBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createInwardPaymentBillsDischarged() {

        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where  "
                + " and b.patientEncounter.discharged=true"
                + " and b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += "order by b.insId desc";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createInwardProBills() {

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BilledBill b where "
                + " b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate"
                + " and b.retired=false";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += " order by b.insId desc";

        temMap.put("billType", BillType.InwardProfessional);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

    }

    public void createInwardProBillsDischarged() {

        String sql;
        Map temMap = new HashMap();
//        sql = "select b from BilledBill b where b.createdAt is not null and b.billType = :billType and b.patientEncounter.discharged=true and "
//                + " b.id in(Select bf.bill.id From BillFee bf where bf.retired=false and bf.createdAt between :fromDate and :toDate and bf.billItem is null)"
//                + " and b.createdAt between :fromDate and :toDate and b.retired=false";

        sql = "select b from BilledBill b where b.createdAt is not null "
                + " and b.patientEncounter.discharged=true and"
                + " b.billType = :billType and b.createdAt between :fromDate and :toDate "
                + "and b.retired=false  ";

        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.name) like :patientName )";
            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPatientPhone() != null && !getSearchKeyword().getPatientPhone().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.patient.person.phone) like :patientPhone )";
            temMap.put("patientPhone", "%" + getSearchKeyword().getPatientPhone().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBhtNo() != null && !getSearchKeyword().getBhtNo().trim().equals("")) {
            sql += " and  (upper(b.patientEncounter.bhtNo) like :bht )";
            temMap.put("bht", "%" + getSearchKeyword().getBhtNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        sql += "order by b.insId desc";

        temMap.put("billType", BillType.InwardBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

    public void createPettyTable() {
        bills = null;
        String sql;
        Map temMap = new HashMap();

        sql = "select b from BilledBill b where b.billType = :billType and b.institution=:ins "
                + " and b.createdAt between :fromDate and :toDate and b.retired=false ";

        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
            sql += " and  (upper(b.insId) like :billNo )";
            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
            sql += " and  (upper(b.staff.person.name) like :stf )";
            temMap.put("stf", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getPersonName() != null && !getSearchKeyword().getPersonName().trim().equals("")) {
            sql += " and  (upper(b.person.name) like :per )";
            temMap.put("per", "%" + getSearchKeyword().getPersonName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNetTotal() != null && !getSearchKeyword().getNetTotal().trim().equals("")) {
            sql += " and  (upper(b.netTotal) like :netTotal )";
            temMap.put("netTotal", "%" + getSearchKeyword().getNetTotal().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getNumber() != null && !getSearchKeyword().getNumber().trim().equals("")) {
            sql += " and  (upper(b.invoiceNumber) like :num )";
            temMap.put("num", "%" + getSearchKeyword().getNumber().trim().toUpperCase() + "%");
        }

        sql += " order by b.createdAt desc  ";
//    
        temMap.put("billType", BillType.PettyCash);
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("ins", getSessionController().getInstitution());

        //System.err.println("Sql " + sql);
        bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 50);

    }

//     public List<Bill> getInstitutionPaymentBills() {
//        if (bills == null) {
//            String sql;
//            Map temMap = new HashMap();
//            if (bills == null) {
//                if (txtSearch == null || txtSearch.trim().equals("")) {
//                    sql = "SELECT b FROM BilledBill b WHERE b.retired=false and b.billType=:type and b.createdAt between :fromDate and :toDate order by b.id";
//                    temMap.put("toDate", getToDate());
//                    temMap.put("fromDate", getFromDate());
//                    temMap.put("type", BillType.PaymentBill);
//                    bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 100);
//
//                } else {
//                    sql = "select b from BilledBill b where b.retired=false and b.billType=:type and b.createdAt between :fromDate and :toDate and (upper(b.staff.person.name) like '%" + txtSearch.toUpperCase() + "%'  or upper(b.staff.person.phone) like '%" + txtSearch.toUpperCase() + "%'  or upper(b.insId) like '%" + txtSearch.toUpperCase() + "%') order by b.createdAt desc  ";
//                    temMap.put("toDate", getToDate());
//                    temMap.put("fromDate", getFromDate());
//                    temMap.put("type", BillType.PaymentBill);
//                    bills = getBillFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP, 100);
//                }
//                if (bills == null) {
//                    bills = new ArrayList<Bill>();
//                }
//            }
//        }
//        return bills;
//
//    }
    public SearchController() {
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public SearchKeyword getSearchKeyword() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public List<BillFee> getBillFees() {
        return billFees;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public List<PatientInvestigation> getPatientInvestigations() {
        return patientInvestigations;
    }

    public void setPatientInvestigations(List<PatientInvestigation> patientInvestigations) {
        this.patientInvestigations = patientInvestigations;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public int getMaxResult() {

        return maxResult;
    }

    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
    }

    public List<Bill> getSelectedBills() {
        return selectedBills;
    }

    public void setSelectedBills(List<Bill> selectedBills) {
        this.selectedBills = selectedBills;
    }

    public PharmacyBean getPharmacyBean() {
        return pharmacyBean;
    }

    public void setPharmacyBean(PharmacyBean pharmacyBean) {
        this.pharmacyBean = pharmacyBean;
    }

    public BillType getBillType() {
        return billType;
    }

    public void setBillType(BillType billType) {
        this.billType = billType;
    }

    public TransferController getTransferController() {
        return transferController;
    }

    public void setTransferController(TransferController transferController) {
        this.transferController = transferController;
    }

    public List<PatientInvestigation> getPatientInvestigationsSigle() {
        if (patientInvestigationsSigle == null) {
            createPatientInvestigationsTableSingle();
        }
        return patientInvestigationsSigle;
    }

    public void setPatientInvestigationsSigle(List<PatientInvestigation> patientInvestigationsSigle) {
        this.patientInvestigationsSigle = patientInvestigationsSigle;
    }

    public Bill getCancellingIssueBill() {
        return cancellingIssueBill;
    }

    public void setCancellingIssueBill(Bill cancellingIssueBill) {
        this.cancellingIssueBill = cancellingIssueBill;
    }

    public double getNetTotalValue() {
        return netTotalValue;
    }

    public void setNetTotalValue(double netTotalValue) {
        this.netTotalValue = netTotalValue;
    }

    public List<BillFee> getBillFeesDone() {
        return billFeesDone;
    }

    public void setBillFeesDone(List<BillFee> billFeesDone) {
        this.billFeesDone = billFeesDone;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    public Staff getStaff() {
        return staff;
    }

    public void setStaff(Staff staff) {
        this.staff = staff;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public double getDueTotal() {
        return dueTotal;
    }

    public void setDueTotal(double dueTotal) {
        this.dueTotal = dueTotal;
    }

    public double getDoneTotal() {
        return doneTotal;
    }

    public void setDoneTotal(double doneTotal) {
        this.doneTotal = doneTotal;
    }

    public double getTotalPaying() {
        return totalPaying;
    }

    public void setTotalPaying(double totalPaying) {
        this.totalPaying = totalPaying;
    }

    public List<PatientInvestigation> getUserPatientInvestigations() {
        return userPatientInvestigations;
    }

    public void setUserPatientInvestigations(List<PatientInvestigation> userPatientInvestigations) {
        this.userPatientInvestigations = userPatientInvestigations;
    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public List<Bill> getAceptPaymentBills() {
        if (aceptPaymentBills == null) {
            aceptPaymentBills = new ArrayList<>();
        }
        return aceptPaymentBills;
    }

    public void setAceptPaymentBills(List<Bill> aceptPaymentBills) {
        this.aceptPaymentBills = aceptPaymentBills;
    }

}
