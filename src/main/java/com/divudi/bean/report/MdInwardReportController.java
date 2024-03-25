/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.report;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.data.BillType;
import com.divudi.data.FeeType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.BillListWithTotals;
import com.divudi.data.dataStructure.BillsTotals;
import com.divudi.data.dataStructure.ItemWithFee;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.ejb.BillEjb;

import com.divudi.ejb.CreditBean;
import com.divudi.entity.Bill;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.RefundBill;
import com.divudi.entity.inward.Admission;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.facade.AdmissionTypeFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ServiceFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author safrin
 */
@Named
@RequestScoped
public class MdInwardReportController implements Serializable {

    private Date fromDate;
    private Date toDate;
    Department dept;
    Item item;
    Category category;
    List<Bill> bills;
    private List<Bill> fillterBill;
    private List<ItemWithFee> itemWithFees;
    private List<ItemWithFee> fillterItemWithFees;
    private PaymentMethod paymentMethod;
    List<BillItem> billItem;
    List<Bill> bil;
    List<Bill> cancel;
    List<Bill> refund;
    List<Bill> refundCancel;
    List<AdmissionType> admissionTy;
    BillsTotals biltot;
    Institution creditCompany;
    PatientEncounter patientEncounter;
    AdmissionType admissionType;
    Admission current;
    Institution institution;
    double total = 0.0;
    List<BillFee> billfees;
    Bill bill;
    ReportKeyWord reportKeyWord;
    private int managaeInwardReportIndex = -1;
    
    ////////////////////////////////////

    private CommonFunctions commonFunctions;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private ServiceFacade serviceFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillFeeFacade billFeeFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    AdmissionTypeFacade admissionTypeFacade;
    @EJB
    BillEjb billEjb;
    ///////////////////////////////
    @Inject
    private SessionController sessionController;
    @Inject
    CommonController commonController;

    //reporting purpuse
    boolean showCreatedDate = false;
    boolean showServiceDate = false;
    boolean showDischargeDate = false;
    boolean showDepartment = false;
    boolean showCategory = false;

    private double purchaseValue;

    public PaymentMethod[] getPaymentMethods() {

        return PaymentMethod.values();
    }
    
    

    public void makeNull() {
        fromDate = null;
        toDate = null;
        bills = null;
        fillterBill = null;
        itemWithFees = null;
        fillterItemWithFees = null;
        paymentMethod = null;
    }

    public BillsTotals getBiltot() {
        if (biltot == null) {
            biltot = new BillsTotals();
        }
        return biltot;
    }

    public void setBiltot(BillsTotals biltot) {
        this.biltot = biltot;
    }

    public double getHospitalTotal() {
        double tmp = 0.0;
        List<Bill> list;
        if (fillterBill == null) {
            list = bills;
        } else {
            list = fillterBill;
        }
        if (list != null) {
            for (Bill b : list) {
                tmp += b.getHospitalFee();
            }
        }
        return tmp;
    }
    
    public double getNetTotal() {
        double tmp = 0.0;
        List<Bill> list;
        if (fillterBill == null) {
            list = bills;
        } else {
            list = fillterBill;
        }
        if (list != null) {
            for (Bill b : list) {
                tmp += b.getNetTotal();
            }
        }  
        return tmp;
    }

    public double getItemHospitalTotal() {
        double tmp = 0.0;
        List<ItemWithFee> list;
        if (fillterItemWithFees == null) {
            list = itemWithFees;
        } else {
            list = fillterItemWithFees;
        }

        if (list != null) {
            for (ItemWithFee b : list) {
                tmp += b.getHospitalFee();

            }
        }
        return tmp;
    }

    public double getItemProfessionalTotal() {
        double tmp = 0.0;
        List<ItemWithFee> list;
        if (fillterItemWithFees == null) {
            list = itemWithFees;
        } else {
            list = fillterItemWithFees;
        }

        if (list != null) {
            for (ItemWithFee b : list) {
                tmp += b.getProFee();
            }
        }
        return tmp;
    }

    public double getProfessionalTotal() {
        double tmp = 0.0;
        List<Bill> list;
        if (fillterBill == null) {
            list = bills;
        } else {
            list = fillterBill;
        }

        if (list != null) {
            for (Bill b : list) {
                tmp += b.getProfessionalFee();
            }
        }
        return tmp;
    }

    public void makeBillNull() {
        bills = null;
        itemWithFees = null;
        fillterBill = null;
        fillterItemWithFees = null;
    }

    public void listBhtFinalBills() {

        String sql;
        Map m = new HashMap();

        sql = "select b from BilledBill b where"
                + " b.retired=false and"
                + " b.createdAt between :fd and :td and"
                + " b.billType=:bt";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("bt", BillType.InwardFinalBill);

        bills = getBillFacade().findByJpql(sql, m);

    }

    public void createServiceBillsByAddedDate() {
        Date startTime = new Date();

        makeListNull();
        String sql;
        Map temMap = new HashMap();
        sql = "select b from Bill b"
                + " where b.billType = :billType "
                + " and b.createdAt between :fromDate and :toDate"
                + "  and b.retired=false "
                + " order by b.insId desc";

        temMap.put("billType", BillType.InwardBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (bills == null) {
            bills = new ArrayList<>();

        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Inward Report/service report/Report by bill(/faces/inward/report_md_inward_bill.xhtml)");

    }

    public void createServiceBillsByDischargeDate() {
        Date startTime = new Date();

        makeListNull();
        String sql;
        Map temMap = new HashMap();
        sql = "select b from Bill b"
                + " where b.billType = :billType "
                + " and b.patientEncounter.dateOfDischarge between :fromDate and :toDate"
                + "  and b.retired=false "
                + " order by b.insId desc";

        temMap.put("billType", BillType.InwardBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (bills == null) {
            bills = new ArrayList<>();

        }
        commonController.printReportDetails(fromDate, toDate, startTime, "Inward Report/service report/Report by bill(/faces/inward/report_md_inward_bill.xhtml)");
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void createInwardBalancePaymentBills() {
        Date startTime = new Date();

        String sql;
        Map temMap = new HashMap();
        sql = "select b from Bill b where"
                + " b.billType = :billType "
                //                + " and b.patientEncounter.paymentFinalized=true "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad ";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.createdAt ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        total = 0.0;
        for (Bill b : bills) {
            total += b.getNetTotal();
        }

        temMap = new HashMap();
        sql = "select count(distinct(b.patientEncounter)) from Bill b where"
                + " b.billType = :billType "
                //                + " and b.patientEncounter.paymentFinalized=true "
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad ";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.createdAt ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        count = getBillFacade().findLongByJpql(sql, temMap, TemporalType.TIMESTAMP);

        commonController.printReportDetails(fromDate, toDate, startTime, "Balance payment report (D)(/faces/inward/inward_search_balance_payment.xhtml)");

    }

    private long count;

    public void createInwardBalancePaymentBills1() {
        Date startTime = new Date();

        String sql;
        Map temMap = new HashMap();
        sql = "select b from BillItem b where "
                + " b.bill.billType = :billType "
                + " and b.bill.institution=:ins "
                + " and b.bill.createdAt between :fromDate and :toDate "
                + " and b.bill.retired=false  ";

        if (reportKeyWord.getString().equals("0")) {
            if (admissionType != null) {
                sql += " and b.patientEncounter.admissionType =:ad ";
                temMap.put("ad", admissionType);
            }
            sql += " and b.patientEncounter is not null ";
        } else if (reportKeyWord.getString().equals("1")) {
            sql += " and b.referenceBill.billType=:refTp";
            temMap.put("refTp", BillType.OpdBill);

        } else if (reportKeyWord.getString().equals("2")) {
            sql += " and b.referenceBill.billType in :refTp ";
            temMap.put("refTp", Arrays.asList(new BillType[]{BillType.PharmacySale, BillType.PharmacyWholeSale}));
        }

        sql += " order by b.createdAt ";

        temMap.put("billType", BillType.CashRecieveBill);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        billItem = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//        bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        total = 0.0;
        for (BillItem b : billItem) {
            total += b.getNetValue();
        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Balance payment report (D)(/faces/inward/inward_search_balance_payment_1.xhtml)");
    }

    public void createCreditInwardOpdPharmacyBills() {
        BillListWithTotals billListWithTotals = new BillListWithTotals();
        if (reportKeyWord.getString().equals("3") && reportKeyWord.getInstitution() == null) {
            JsfUtil.addErrorMessage("Please Select Credit Company");
            return;
        }
        PaymentMethod[] pms = new PaymentMethod[]{PaymentMethod.Credit};
        total = 0.0;
        if (reportKeyWord.getString().equals("0")) {
            BillType[] bts = new BillType[]{BillType.InwardFinalBill};
            billListWithTotals = billEjb.findBillsAndTotals(fromDate, toDate, bts, null, null, null, null, null, null, null,
                    reportKeyWord.getInstitution(), pms, null, null, true, admissionType);
            if (!billListWithTotals.getBills().isEmpty()) {
                bills = new ArrayList<>();
                bills.addAll(billListWithTotals.getBills());
                total = billListWithTotals.getNetTotal();
            }
        }
        if (reportKeyWord.getString().equals("1")) {
            BillType[] bts = new BillType[]{BillType.OpdBill};
            billListWithTotals = billEjb.findBillsAndTotals(fromDate, toDate, bts, null, null, null, null, null, null, null,
                    reportKeyWord.getInstitution(), pms, null, null, false, null);
            if (!billListWithTotals.getBills().isEmpty()) {
                bills = new ArrayList<>();
                bills.addAll(billListWithTotals.getBills());
                total = billListWithTotals.getNetTotal();
            }
        }
        if (reportKeyWord.getString().equals("2")) {
            BillType[] bts = new BillType[]{BillType.PharmacySale, BillType.PharmacyWholeSale};
            billListWithTotals = billEjb.findBillsAndTotals(fromDate, toDate, bts, null, null, null, null, null, reportKeyWord.getInstitution(),
                    null, null, pms, null, null, false, null);
            if (!billListWithTotals.getBills().isEmpty()) {
                bills = new ArrayList<>();
                for (Bill b : billListWithTotals.getBills()) {
                    if (b.getToStaff() == null) {
                        bills.add(b);
                        total += b.getNetTotal();
                    }
                }
            }
        }
        if (reportKeyWord.getString().equals("3")) {
            bills = new ArrayList<>();
            BillType[] bts = new BillType[]{BillType.InwardFinalBill};
            billListWithTotals = billEjb.findBillsAndTotals(fromDate, toDate, bts, null, null, null, null, null, null, null,
                    reportKeyWord.getInstitution(), pms, null, null, true, null);
            if (!billListWithTotals.getBills().isEmpty()) {
                bills.addAll(billListWithTotals.getBills());
                total = billListWithTotals.getNetTotal();
            }

            bts = new BillType[]{BillType.OpdBill};
            billListWithTotals = billEjb.findBillsAndTotals(fromDate, toDate, bts, null, null, null, null, null, null, null,
                    reportKeyWord.getInstitution(), pms, null, null, false, null);
            if (!billListWithTotals.getBills().isEmpty()) {
                bills.addAll(billListWithTotals.getBills());
                total += billListWithTotals.getNetTotal();
            }

            bts = new BillType[]{BillType.PharmacySale, BillType.PharmacyWholeSale};
            billListWithTotals = billEjb.findBillsAndTotals(fromDate, toDate, bts, null, null, null, null, null, reportKeyWord.getInstitution(),
                    null, null, pms, null, null, false, null);
            if (!billListWithTotals.getBills().isEmpty()) {
                bills.addAll(billListWithTotals.getBills());
                total += billListWithTotals.getNetTotal();
            }

        }

    }

    public List<Bill> getBillsDischarged() {

        if (bills == null) {
            String sql;
            Map temMap = new HashMap();
            sql = "select b from Bill b where b.createdAt is not null and  b.billType = :billType and b.id in"
                    + "(select bi.bill.id from BillItem bi where bi.item is not null) and b.institution=:ins and b.patientEncounter.dateOfDischarge between :fromDate and :toDate and b.retired=false order by b.insId desc";

            temMap.put("billType", BillType.InwardBill);
            temMap.put("toDate", toDate);
            temMap.put("fromDate", fromDate);
            temMap.put("ins", getSessionController().getInstitution());

            bills = getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

            if (bills == null) {
                bills = new ArrayList<Bill>();

            }

            for (Bill b : bills) {
                sql = "Select b From BillFee b where b.retired=false and b.bill.id=" + b.getId();
                List<BillFee> bflist = getBillFeeFacade().findByJpql(sql);
                for (BillFee bf : bflist) {
                    if (bf.getFee().getFeeType() == FeeType.OwnInstitution) {
                        b.setHospitalFee(b.getHospitalFee() + bf.getFeeValue());
                    } else if (bf.getFee().getFeeType() == FeeType.Staff) {
                        b.setProfessionalFee(b.getProfessionalFee() + bf.getFeeValue());
                    }
                }
            }
        }

        return bills;
    }

    public MdInwardReportController() {
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        // makeNull();
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        //   makeNull();
        this.toDate = toDate;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public ServiceFacade getServiceFacade() {
        return serviceFacade;
    }

    public void setServiceFacade(ServiceFacade serviceFacade) {
        this.serviceFacade = serviceFacade;
    }

    public List<Bill> getFillterBill() {
        return fillterBill;
    }

    public void setFillterBill(List<Bill> fillterBill) {
        this.fillterBill = fillterBill;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    private void listInBhtBillItems(BillType billType) {

        Map m = new HashMap();
        String jpql;
        jpql = "select b from BillItem b"
                + " where b.bill.department =:dept"
                + " and  b.bill.billType=:biTy "
                + " and b.createdAt between :fd and :td";
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("dept", dept);
        m.put("biTy", billType);
        billItem = getBillItemFacade().findByJpql(jpql, m, TemporalType.TIMESTAMP);

    }

    public void listInBhtBillItems() {
        Date startTime = new Date();
        purchaseValue = 0.0;
        listInBhtBillItems(BillType.PharmacyBhtPre);
        for (BillItem bi : billItem) {
            purchaseValue += bi.getPharmaceuticalBillItem().getPurchaseRate() * bi.getPharmaceuticalBillItem().getQty();
        }
        commonController.printReportDetails(fromDate, toDate, startTime, " BHT Interim error correction(/faces/inward/report_bht_issue_by_bill_item.xhtml)");
    }

    public void listInBhtBillItemsStore() {
        listInBhtBillItems(BillType.StoreBhtPre);

    }

    public List<Bill> getBil() {
        return bil;
    }

    public void setBil(List<Bill> bil) {
        this.bil = bil;
    }

    public List<AdmissionType> getAdmissionTy() {
        admissionTy = getAdmissionTypeFacade().findAll("name", true);
        return admissionTy;
    }

    public void setAdmissionTy(List<AdmissionType> admissionTy) {
        this.admissionTy = admissionTy;
    }

    public AdmissionTypeFacade getAdmissionTypeFacade() {
        return admissionTypeFacade;
    }

    public void setAdmissionTypeFacade(AdmissionTypeFacade admissionTypeFacade) {
        this.admissionTypeFacade = admissionTypeFacade;
    }

    public Admission getCurrent() {
        if (current == null) {
            current = new Admission();
            current.setDateOfAdmission(new Date());
        }
        return current;
    }

    public void setCurrent(Admission current) {
        this.current = current;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

//    public void listInwardBillItems(){
//    
//        Map m=new HashMap();
//        String jpql;
//        jpql="select b from BillItem b where"
//                + " b.bill.department =:dept"
//                + " and  b.bill.billType=:biTy "
//                + " and b.createdAt between :fd and :td";
//        m.put("fd", fromDate);
//        m.put("td", toDate);
//        m.put("dept", dept);
//        m.put("biTy", BillType.InwardFinalBill);
//        billItem=getBillItemFacade().findByJpql(jpql, m,TemporalType.TIMESTAMP);
//        
//        
//    }
    double totalValue;

    public double getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(double totalValue) {
        this.totalValue = totalValue;
    }

    private double calTotal(Bill bill) {
        String sql;
        Map temMap = new HashMap();
        sql = "select sum(b.netTotal) from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private double calTotal(Bill bill, PaymentMethod paymentMethod) {
        String sql;
        Map temMap = new HashMap();
        sql = "select sum(b.netTotal) from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.paymentMethod=:pm"
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);
        temMap.put("pm", paymentMethod);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private List<Bill> calBills(Bill bill) {
        String sql;
        Map temMap = new HashMap();
        sql = "select b from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double getBilledCashValue() {
        return billedCashValue;
    }

    public void setBilledCashValue(double billedCashValue) {
        this.billedCashValue = billedCashValue;
    }

    public List<Bill> getRefund() {
        return refund;
    }

    public void setRefund(List<Bill> refund) {
        this.refund = refund;
    }

    public List<Bill> getCancel() {
        return cancel;
    }

    public void setCancel(List<Bill> cancel) {
        this.cancel = cancel;
    }

    double cancelledTotal;

    public double getCancelledTotal() {
        return cancelledTotal;
    }

    public void setCancelledTotal(double cancelledTotal) {
        this.cancelledTotal = cancelledTotal;
    }

    public List<Bill> getRefundCancel() {
        return refundCancel;
    }

    public void setRefundCancel(List<Bill> refundCancel) {
        this.refundCancel = refundCancel;
    }

    public double getRefundTotal() {
        return refundTotal;
    }

    public void setRefundTotal(double refundTotal) {
        this.refundTotal = refundTotal;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    double billedCashValue = 0;
    double billedCreditValue = 0;
    double billedCreditCardValue = 0;
    double billedSlipValue = 0;
    double billedChequeValue = 0;
    double billedAgentValue = 0;

    double cancelledCashValue = 0;
    double cancelledCreditValue = 0;
    double cancelledCreditCardValue = 0;
    double cancelledSlipValue = 0;
    double cancelledChequeValue = 0;
    double cancelledAgentValue = 0;
    double refundTotal;

    public void listInwardPaymentBill() {

        bil = calBills(new BilledBill());
        cancel = calBills(new CancelledBill());
        refund = calBills(new RefundBill());

        totalValue = calTotal(new BilledBill());
        cancelledTotal = calTotal(new CancelledBill());
        refundTotal = calTotal(new RefundBill());
        billedCashValue = calTotal(new BilledBill(), PaymentMethod.Cash);
        billedChequeValue = calTotal(new BilledBill(), PaymentMethod.Cheque);
        billedAgentValue = calTotal(new BilledBill(), PaymentMethod.Agent);
        billedCreditCardValue = calTotal(new BilledBill(), PaymentMethod.Card);
        billedCreditValue = calTotal(new BilledBill(), PaymentMethod.Credit);
        billedSlipValue = calTotal(new BilledBill(), PaymentMethod.Slip);
        cancelledCashValue = calTotal(new CancelledBill(), PaymentMethod.Cash);
        cancelledChequeValue = calTotal(new CancelledBill(), PaymentMethod.Cheque);
        cancelledAgentValue = calTotal(new CancelledBill(), PaymentMethod.Agent);
        cancelledCreditCardValue = calTotal(new CancelledBill(), PaymentMethod.Card);
        cancelledCreditValue = calTotal(new CancelledBill(), PaymentMethod.Credit);
        cancelledSlipValue = calTotal(new CancelledBill(), PaymentMethod.Slip);

//        Map m = new HashMap();
        //        String jpql;
        //        jpql = "select b from BilledBill b where"
        //                + " b.billType=:biTy "
        //                + " and b.createdAt between :fd and :td";
        //        m.put("fd", fromDate);
        //        m.put("td", toDate);
        //        m.put("biTy", BillType.InwardPaymentBill);
        //        bil = getBillFacade().findByJpql(jpql, m, TemporalType.TIMESTAMP);
    }

    private double calTotInwdPaymentBills(Bill bill) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.netTotal) from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.retired=false  ";

        sql += " and ((b.patientEncounter.dateOfDischarge between :fromDate and :toDate "
                //                + " and b.patientEncounter.paymentFinalized = true "
                + " and b.patientEncounter.discharged = true )";

        sql += " or (b.createdAt <= :toDate "
                + " and b.patientEncounter.dateOfDischarge > :toDate ))";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }
        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private List<Bill> inwdPaymentBills(Bill bill) {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class "
                + " and b.retired=false ";

        sql += " and ((b.patientEncounter.dateOfDischarge between :fromDate and :toDate "
                //                + " and b.patientEncounter.paymentFinalized = true "
                + " and b.patientEncounter.discharged = true )";

        sql += " or (b.createdAt <= :toDate "
                + " and b.patientEncounter.dateOfDischarge > :toDate ))";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public void allBhtTotPySummerries() {
        Date startTime = new Date();

        bil = inwdPaymentBills(new BilledBill());
        cancel = inwdPaymentBills(new CancelledBill());
        refund = inwdPaymentBills(new RefundBill());

        totalValue = calTotInwdPaymentBills(new BilledBill());
        cancelledTotal = calTotInwdPaymentBills(new CancelledBill());
        refundTotal = calTotInwdPaymentBills(new RefundBill());

        commonController.printReportDetails(fromDate, toDate, startTime, "Deposits of dicharged patient and complete payment of discharged patient(/faces/inward/bht_deposit_by_discharge_date_and_created_date.xhtml)");
    }

    private double calInwdPaymentBillsNotDischarge(Bill bill) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.netTotal) from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.createdAt <= :toDate "
                + " and b.retired=false  "
                + "  and b.patientEncounter.dateOfDischarge > :toDate ";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }
        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
//        temMap.put("fromDate", fromDate);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private List<Bill> inwdPaymentBillsNotDischarge(Bill bill) {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.createdAt <= :toDate "
                + " and b.retired=false  "
                + " and b.patientEncounter.dateOfDischarge > :toDate";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }
        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
//        temMap.put("fromDate", fromDate);

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private List<Bill> depositByCreatedDate(Bill bill, boolean disharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";
        if (disharged) {
            sql += " and b.patientEncounter.dateOfDischarge < :toDate";
        } else {
            sql += " and (b.patientEncounter.dateOfDischarge > :toDate "
                    + " or b.patientEncounter.dateOfDischarge is null )";
        }

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }

        if (patientEncounter != null) {
            sql += " and b.patientEncounter=pten ";
            temMap.put("pten", patientEncounter);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.patientEncounter.bhtNo,b.insId ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private List<Bill> allPaymentByCreatedDate(Bill bill, boolean disharged) {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.patientEncounter.dateOfDischarge between :fromDate and :toDate"
                + " and b.createdAt <= :toDate "
                + " and b.retired=false  ";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }

        if (patientEncounter != null) {
            sql += " and b.patientEncounter=pten ";
            temMap.put("pten", patientEncounter);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.patientEncounter.bhtNo,b.insId ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private List<Bill> depositByCreatedDate(Bill bill) {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }

        if (patientEncounter != null) {
            sql += " and b.patientEncounter=pten ";
            temMap.put("pten", patientEncounter);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.patientEncounter.dateOfDischarge,b.createdAt";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private double depositByCreatedDateValue(Bill bill, boolean discharge) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.netTotal) from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (discharge) {
            sql += " and b.patientEncounter.dateOfDischarge < :toDate";
        } else {
            sql += " and (b.patientEncounter.dateOfDischarge > :toDate "
                    + " or b.patientEncounter.dateOfDischarge is null )";
        }

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }

        if (patientEncounter != null) {
            sql += " and b.patientEncounter=pten ";
            temMap.put("pten", patientEncounter);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

//        sql += " order by b.patientEncounter.bhtNo,b.insId ";
        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private double allPaymentByCreatedDateValue(Bill bill, boolean discharge) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.netTotal) from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.patientEncounter.dateOfDischarge between :fromDate and :toDate "
                + " and b.createdAt <= :toDate"
                + " and b.retired=false  ";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }

        if (patientEncounter != null) {
            sql += " and b.patientEncounter=pten ";
            temMap.put("pten", patientEncounter);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

//        sql += " order by b.patientEncounter.bhtNo,b.insId ";
        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private double depositByCreatedDateValue(Bill bill) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.netTotal) from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.createdAt between :fromDate and :toDate "
                + " and b.retired=false  ";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }

        if (patientEncounter != null) {
            sql += " and b.patientEncounter=pten ";
            temMap.put("pten", patientEncounter);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

//        sql += " order by b.patientEncounter.bhtNo,b.insId ";
        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private List<Bill> inwdPaymentBillsAdmitted(Bill bill) {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.retired=false  "
                + " and b.patientEncounter.discharged=false";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }
        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private double calPaymentBillsAdmitted(Bill bill) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.netTotal) from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.retired=false  "
                + " and b.patientEncounter.discharged=false";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }
        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private List<Bill> fetchPaymentBills(String args) {
        String sql = "";
        Map temMap = new HashMap();
        sql = "select b from Bill b where "
                + " b.billType = :billType "
                + " and b.retired=false"
                + " and b.createdAt between :fromDate and :toDate ";

        sql += args;

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }

        if (patientEncounter != null) {
            sql += " and b.patientEncounter=pten ";
            temMap.put("pten", patientEncounter);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad ";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.patientEncounter.bhtNo,"
                + " b.patientEncounter.dateOfDischarge,"
                + " b.createdAt ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private List<Bill> fetchPaymentBillsNotDicharged() {
        String sql = "";
        Map temMap = new HashMap();
        sql = "select b from Bill b where "
                + " b.billType = :billType "
                + " and b.retired=false"
                + " and b.createdAt < :toDate"
                + " and b.patientEncounter.dateOfAdmission < :toDate "
                + " and (b.patientEncounter.dateOfDischarge > :toDate"
                + " or b.patientEncounter.dateOfDischarge is null )";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }

        if (patientEncounter != null) {
            sql += " and b.patientEncounter=pten ";
            temMap.put("pten", patientEncounter);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad ";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.patientEncounter.bhtNo,"
                + " b.createdAt ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private double calPaymentBillsNotDicharged() {
        String sql = "";
        Map temMap = new HashMap();
        sql = "select sum(b.netTotal) from Bill b where "
                + " b.billType = :billType "
                + " and b.retired=false"
                + " and b.createdAt < :toDate"
                + " and b.patientEncounter.dateOfAdmission < :toDate "
                + " and (b.patientEncounter.dateOfDischarge > :toDate"
                + " or b.patientEncounter.dateOfDischarge is null )";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }

        if (patientEncounter != null) {
            sql += " and b.patientEncounter=pten ";
            temMap.put("pten", patientEncounter);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad ";
            temMap.put("ad", admissionType);
        }

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private double calPaymentBills(String args) {
        String sql = "";
        Map temMap = new HashMap();
        sql = "select sum(b.netTotal) "
                + " from Bill b where "
                + " b.billType = :billType "
                + " and b.retired=false"
                + " and b.createdAt between :fromDate and :toDate ";

        sql += args;

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }

        if (patientEncounter != null) {
            sql += " and b.patientEncounter=pten ";
            temMap.put("pten", patientEncounter);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public void notDisBhtPySummerries() {

        bil = inwdPaymentBillsNotDischarge(new BilledBill());
        cancel = inwdPaymentBillsNotDischarge(new CancelledBill());
        refund = inwdPaymentBillsNotDischarge(new RefundBill());

        totalValue = calInwdPaymentBillsNotDischarge(new BilledBill());
        cancelledTotal = calInwdPaymentBillsNotDischarge(new CancelledBill());
        refundTotal = calInwdPaymentBillsNotDischarge(new RefundBill());

    }

    public void createDepositByCreatedDateNotDischarged() {

        Date startTime = new Date();

        bil = depositByCreatedDate(new BilledBill(), false);
        cancel = depositByCreatedDate(new CancelledBill(), false);
        refund = depositByCreatedDate(new RefundBill(), false);

        totalValue = depositByCreatedDateValue(new BilledBill(), false);
        cancelledTotal = depositByCreatedDateValue(new CancelledBill(), false);
        refundTotal = depositByCreatedDateValue(new RefundBill(), false);

        commonController.printReportDetails(fromDate, toDate, startTime, "Deposite of discharged patient (/faces/inward/bht_deposit_by_created_date_not_discharge.xhtml)");

    }

    public void sortByPatientDischargeDate() {
        String sql;
        Bill bill;
        sql = "select b from Bill b where "
                + " and order by b. ";
    }

    public void createDepositByCreatedDateDischarged() {
        Date startTime = new Date();

        bil = depositByCreatedDate(new BilledBill(), true);
        cancel = depositByCreatedDate(new CancelledBill(), true);
        refund = depositByCreatedDate(new RefundBill(), true);

        totalValue = depositByCreatedDateValue(new BilledBill(), true);
        cancelledTotal = depositByCreatedDateValue(new CancelledBill(), true);
        refundTotal = depositByCreatedDateValue(new RefundBill(), true);

        commonController.printReportDetails(fromDate, toDate, startTime, "Payment by created date discharged only(/faces/inward/bht_deposit_by_created_date_discharged.xhtml)");
    }

    public void createAllPaymentByCreatedDateDischarged() {
        Date startTime = new Date();

        bil = allPaymentByCreatedDate(new BilledBill(), true);
        cancel = allPaymentByCreatedDate(new CancelledBill(), true);
        refund = allPaymentByCreatedDate(new RefundBill(), true);

        totalValue = allPaymentByCreatedDateValue(new BilledBill(), true);
        cancelledTotal = allPaymentByCreatedDateValue(new CancelledBill(), true);
        refundTotal = allPaymentByCreatedDateValue(new RefundBill(), true);

        commonController.printReportDetails(fromDate, toDate, startTime, "All payment of discharged patient(/faces/inward/all_payment_by_created_date_discharged_only.xhtml)");
    }

    public void createDepositByCreatedDateDischargedAll() {
        Date startTime = new Date();

        bil = depositByCreatedDate(new BilledBill());
        cancel = depositByCreatedDate(new CancelledBill());
        refund = depositByCreatedDate(new RefundBill());

        totalValue = depositByCreatedDateValue(new BilledBill());
        cancelledTotal = depositByCreatedDateValue(new CancelledBill());
        refundTotal = depositByCreatedDateValue(new RefundBill());

        commonController.printReportDetails(fromDate, toDate, startTime, " Payment by created date(/faces/inward/bht_deposit_by_created_date_discharged_all.xhtml)");

    }

    public void admittedPatientSummerries() {

        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        bil = inwdPaymentBillsAdmitted(new BilledBill());
        cancel = inwdPaymentBillsAdmitted(new CancelledBill());
        refund = inwdPaymentBillsAdmitted(new RefundBill());

        totalValue = calPaymentBillsAdmitted(new BilledBill());
        cancelledTotal = calPaymentBillsAdmitted(new CancelledBill());
        refundTotal = calPaymentBillsAdmitted(new RefundBill());

        commonController.printReportDetails(fromDate, toDate, startTime, "Deposit of admitted patients(/faces/inward/bht_deposit_of_admitted_patient.xhtml)");

    }

    List<Bill> deposits;
    List<Bill> completePayments;

    double depositsTotal;
    double completePaymentsTotal;
    double grantTotal;

    public void allBhtPySummerriesByCreatedDate() {

        Date startTime = new Date();
//        String sql = " and b.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        String sql = "";
        completePayments = fetchPaymentBills(sql);
        completePaymentsTotal = calPaymentBills(sql);

//        sql = " and b.patientEncounter.dateOfDischarge not between :fromDate and :toDate ";
//        deposits = fetchPaymentBills(sql);
//        depositsTotal = calPaymentBills(sql);
//
        sql = "";
        grantTotal = calPaymentBills(sql);

        commonController.printReportDetails(fromDate, toDate, startTime, "All payment by created date (/faces/inward/bht_deposit_by_created_date_all.xhtml)");

    }

    public void dipositsOfNotDischarged() {
        Date startTime = new Date();
//        String sql = " and b.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        String sql = "";
        completePayments = fetchPaymentBillsNotDicharged();
        completePaymentsTotal = calPaymentBillsNotDicharged();

        commonController.printReportDetails(fromDate, toDate, startTime, "All deposite of not discharged patient(/faces/inward/bht_deposit_of_not_discharged_patient.xhtml)");

    }

    public void dipositsOfNotDischargedByBht() {
        Date startTime = new Date();

        String sql = "";
        Map temMap = new HashMap();
        sql = "select b.patientEncounter,sum(b.netTotal)"
                + " from Bill b where "
                + " b.billType = :billType "
                + " and b.retired=false "
                + " and b.createdAt < :toDate"
                + " and b.patientEncounter.dateOfAdmission < :toDate "
                + " and (b.patientEncounter.dateOfDischarge > :toDate"
                + " or b.patientEncounter.dateOfDischarge is null )";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }

        if (patientEncounter != null) {
            sql += " and b.patientEncounter=pten ";
            temMap.put("pten", patientEncounter);
        }

        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad ";
            temMap.put("ad", admissionType);
        }

        sql += "  group by b.patientEncounter.id "
                + " order  by b.patientEncounter.admissionType.name,b.patientEncounter.bhtNo";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("toDate", toDate);

        List<Object[]> list = getBillFacade().findAggregates(sql, temMap, TemporalType.TIMESTAMP);

        patientEncounterValues = new ArrayList<>();

        for (Object[] obj : list) {
            PatientEncounterValue row = new PatientEncounterValue();
            PatientEncounter pe = (PatientEncounter) obj[0];
            row.setPe(pe);
            row.setPaid((Double) obj[1]);
            double paidAmtByCreditCompany = Math.abs(creditBean.getPaidAmount(pe, BillType.CashRecieveBill, getToDate()));
            row.setPaidByCreditCompany(paidAmtByCreditCompany);
            if (row.getPaid() == 0.0 && paidAmtByCreditCompany == 0.0) {
                continue;
            }
            patientEncounterValues.add(row);

        }

        completePaymentsTotal = calPaymentBillsNotDicharged();

        commonController.printReportDetails(fromDate, toDate, startTime, "All deposite of not discharged patient by BHT(/faces/inward/bht_deposit_of_not_discharged_patient_by_bht.xhtml)");
    }

    @EJB
    CreditBean creditBean;

    private double calInwdPaymentBillsDischarge(Bill bill) {
        String sql;
        Map temMap = new HashMap();

        sql = "select sum(b.netTotal)"
                + " from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.patientEncounter.dateOfDischarge between :fromDate and :toDate "
                //                + " and b.createdAt between :fromDate and :toDate "
                //                + " and b.createdAt <= :toDate"
                + " and b.retired = false"
                //                + " and b.patientEncounter.paymentFinalized = true"
                + " and b.patientEncounter.discharged = true";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }
        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        return getBillFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    private List<Bill> inwdPaymentBillsDischarge(Bill bill) {
        String sql;
        Map temMap = new HashMap();

        sql = "select b from Bill b where"
                + " b.billType = :billType "
                + " and type(b)=:class"
                + " and b.patientEncounter.dateOfDischarge between :fromDate and :toDate "
                //                + " and b.createdAt between :fromDate and :toDate "
                //                + " and b.createdAt <= :toDate"
                + " and b.retired = false  "
                //                + " and b.patientEncounter.paymentFinalized = true"
                + " and b.patientEncounter.discharged = true";

        if (creditCompany != null) {
            sql += " and b.creditCompany=:cc ";
            temMap.put("cc", creditCompany);
        }
        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod =:pm";
            temMap.put("pm", paymentMethod);
        }

        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType =:ad";
            temMap.put("ad", admissionType);
        }

        sql += " order by b.insId desc  ";

        temMap.put("billType", BillType.InwardPaymentBill);
        temMap.put("class", bill.getClass());
        temMap.put("toDate", toDate);
        temMap.put("fromDate", fromDate);

        return getBillFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public void disBhtPySummerries() {
        Date startTime = new Date();

        bil = inwdPaymentBillsDischarge(new BilledBill());
        cancel = inwdPaymentBillsDischarge(new CancelledBill());
        refund = inwdPaymentBillsDischarge(new RefundBill());

        totalValue = calInwdPaymentBillsDischarge(new BilledBill());
        cancelledTotal = calInwdPaymentBillsDischarge(new CancelledBill());
        refundTotal = calInwdPaymentBillsDischarge(new RefundBill());

        commonController.printReportDetails(fromDate, toDate, startTime, "complete Payment of discharged payment(/faces/inward/bht_deposit_by_discharge_date.xhtml)");
    }

    public void makeListNull() {
        bills = null;
        fillterBill = null;
        itemWithFees = null;
        fillterItemWithFees = null;
    }

    public void createItemWithFeeByAddedDate() {
        Date startTime = new Date();

        makeListNull();
        String sql;
        List<Item> tmp;
        Map temMap = new HashMap();

        itemWithFees = new ArrayList<>();

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bTp", BillType.InwardBill);

        if (getPaymentMethod() == null) {
            sql = "select distinct(bi.item) "
                    + " FROM BillItem bi "
                    + " where bi.retired=false "
                    + " and bi.item.retired=false "
                    //                    + " and  bi.bill.institution=:ins "
                    + " and  bi.bill.billType= :bTp  "
                    + " and  bi.bill.createdAt between :fromDate and :toDate ";
            if (institution != null) {
                sql += " and  bi.item.institution=:ins ";
                temMap.put("ins", institution);
            }
            if (dept != null) {
                sql += " and  bi.item.department=:dept ";
                temMap.put("dept", dept);
            }
            if (category != null) {
                sql += " and  bi.item.category=:cat ";
                temMap.put("cat", category);
            }
            if (item != null) {
                sql += " and  bi.item=:item ";
                temMap.put("item", item);
            }
        } else {
            sql = "select distinct(bi.item) "
                    + " FROM BillItem bi"
                    + " where bi.bill.billType= :bTp  "
                    + " and  bi.bill.createdAt between :fromDate and :toDate "
                    + " and bi.bill.paymentMethod=:p ";
            if (institution != null) {
                sql += " and  bi.item.institution=:ins ";
                temMap.put("ins", institution);
            }
            if (dept != null) {
                sql += " and  bi.item.department=:dept ";
                temMap.put("dept", dept);
            }
            if (category != null) {
                sql += " and  bi.item.category=:cat ";
                temMap.put("cat", category);
            }
            if (item != null) {
                sql += " and  bi.item=:item ";
                temMap.put("item", item);
            }

            temMap.put("p", getPaymentMethod());
        }

        tmp = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        for (Item i : tmp) {
            ItemWithFee iwf = new ItemWithFee();
            iwf.setItem(i);
            setCount(iwf);
            setFee(iwf);
            ////// // System.out.println("ss " + itemWithFees.size());
            ////// // System.out.println("ss " + iwf.getItem());
            itemWithFees.add(iwf);
        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Inward Reports/Service Report/Report by item(Count)(/faces/inward/report_md_inward_item.xhtml)");
    }

    public void createItemWithFeeByDischargeDate() {
        makeListNull();
        String sql;
        List<Item> tmp;
        Map temMap = new HashMap();

        itemWithFees = new ArrayList<>();

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bTp", BillType.InwardBill);

        if (getPaymentMethod() == null) {
            sql = "select distinct(bi.item) "
                    + " FROM BillItem bi "
                    + " where bi.retired=false "
                    + " and bi.item.retired=false "
                    //                    + " and  bi.bill.institution=:ins "
                    + " and  bi.bill.billType= :bTp  "
                    + " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";

            if (institution != null) {
                sql += " and  bi.item.institution=:ins ";
                temMap.put("ins", institution);
            }
            if (dept != null) {
                sql += " and  bi.item.department=:dept ";
                temMap.put("dept", dept);
            }
            if (category != null) {
                sql += " and  bi.item.category=:cat ";
                temMap.put("cat", category);
            }
            if (item != null) {
                sql += " and  bi.item=:item ";
                temMap.put("item", item);
            }
        } else {
            sql = "select distinct(bi.item) "
                    + " FROM BillItem bi"
                    + " where bi.bill.billType= :bTp  "
                    + " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate "
                    + " and bi.bill.paymentMethod=:p ";

            if (institution != null) {
                sql += " and  bi.item.institution=:ins ";
                temMap.put("ins", institution);
            }
            if (dept != null) {
                sql += " and  bi.item.department=:dept ";
                temMap.put("dept", dept);
            }
            if (category != null) {
                sql += " and  bi.item.category=:cat ";
                temMap.put("cat", category);
            }
            if (item != null) {
                sql += " and  bi.item=:item ";
                temMap.put("item", item);
            }

            temMap.put("p", getPaymentMethod());
        }

        tmp = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        for (Item i : tmp) {
            ItemWithFee iwf = new ItemWithFee();
            iwf.setItem(i);
            setCount(iwf);
            setFee(iwf);
            //   //////// // System.out.println("ss " + itemWithFees.size());
            //      //////// // System.out.println("ss " + iwf.getItem());
            itemWithFees.add(iwf);
        }

    }

    public void createItemWithFeeByAddedDate1() {
        Date startTime = new Date();

        makeListNull();
        String sql;
        Map temMap = new HashMap();
        billItem = new ArrayList<>();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bTp", BillType.InwardBill);

        ////// // System.out.println("in");
        if (getPaymentMethod() == null) {
            sql = "select bi "
                    + " FROM BillFee bi "
                    + " where bi.retired=false "
                    + " and bi.bill.retired=false "
                    + " and bi.billItem.retired=false"
                    //                    + " and  bi.bill.institution=:ins "
                    + " and  bi.bill.billType= :bTp  "
                    + " and  bi.bill.createdAt between :fromDate and :toDate ";
            if (institution != null) {
                sql += " and  bi.billItem.item.institution=:ins ";
                temMap.put("ins", institution);
            }
            if (dept != null) {
                sql += " and  bi.billItem.item.department=:dept ";
                temMap.put("dept", dept);
            }
            if (category != null) {
                sql += " and  bi.billItem.item.category=:cat ";
                temMap.put("cat", category);
            }
            if (item != null) {
                sql += " and  bi.billItem.item=:item ";
                temMap.put("item", item);
            }
        } else {
            sql = "select bi "
                    + " FROM BillFee bi"
                    + " where bi.bill.billType= :bTp  "
                    + " and  bi.bill.createdAt between :fromDate and :toDate "
                    + " and bi.bill.paymentMethod=:p ";
            if (institution != null) {
                sql += " and  bi.billItem.item.institution=:ins ";
                temMap.put("ins", institution);
            }
            if (dept != null) {
                sql += " and  bi.billItem.item.department=:dept ";
                temMap.put("dept", dept);
            }
            if (category != null) {
                sql += " and  bi.billItem.item.category=:cat ";
                temMap.put("cat", category);
            }
            if (item != null) {
                sql += " and  bi.billItem.item=:item ";
                temMap.put("item", item);
            }

            temMap.put("p", getPaymentMethod());
        }

        sql += " order by bi.billItem.bill.patientEncounter.bhtNo ";

        billfees = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        ////// // System.out.println("out");

        total = 0.0;
        for (BillFee bf : billfees) {
            total += bf.getFee().getFee();
            ////// // System.out.println("total = " + total);
        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Inward Reports/Service Report/Report by item/process by bill date(/faces/inward/report_md_inward_item_1.xhtml)");

    }

    public void createItemWithFeeWithBillByAddedDate() {
        createBillWithBillFee(false, true, false);
    }

    public void createItemWithFeeWithBillByDischargeDate() {
        createBillWithBillFee(true, false, false);
    }

    public void createItemWithFeeWithBillByBillDate() {
        Date startTime = new Date();

        createBillWithBillFee(false, false, true);
        commonController.printReportDetails(fromDate, toDate, startTime, "Inward report/service report/Report by item with bill(/faces/inward/report_md_inward_item_with_bill.xhtml)");
    }

    //619
    public List<Bill> billsOutSide(boolean dis, boolean add, boolean bill, Bill billClass) {

        String sql;
        Map m = new HashMap();
        billItem = new ArrayList<>();
        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        m.put("bTp", BillType.InwardBill);
        m.put("billClass", billClass.getClass());
        ////// // System.out.println("in");

        sql = "select DISTINCT(bi.bill) FROM BillFee bi"
                + " where bi.bill.billType= :bTp "
                + " and type(bi.bill)=:billClass ";

        if (bill) {
            sql += " and bi.bill.createdAt between :fromDate and :toDate ";
        }
        if (dis) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        }

        if (add) {
            sql += " and bi.billItem.billTime between :fromDate and :toDate ";
        }

        if (institution != null) {
            sql += " and  bi.billItem.item.institution=:ins ";
            m.put("ins", institution);
        }
        if (dept != null) {
            sql += " and  bi.billItem.item.department=:dept ";
            m.put("dept", dept);
        }
        if (category != null) {
            sql += " and  bi.billItem.item.category=:cat ";
            m.put("cat", category);
        }
        if (item != null) {
            sql += " and  bi.billItem.item=:item ";
            m.put("item", item);
        }
        if (getPaymentMethod() != null) {
            sql += " and bi.bill.paymentMethod=:p ";
            m.put("p", getPaymentMethod());
        }

        sql += " order by bi.bill.insId ";

        return getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public double calBillHosTotals(List<BillFee> bfs) {
        double total = 0.0;
        for (BillFee bf : bfs) {
            total += bf.getFee().getFee();
        }
        ////// // System.out.println("total = " + total);
        return total;

    }

    public double calHosTotals(boolean dis, boolean add, boolean bill) {

        String sql;
        Map m = new HashMap();
        billItem = new ArrayList<>();
        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());
        m.put("bTp", BillType.InwardBill);

        ////// // System.out.println("in");
        sql = "select sum(bi.fee.fee) FROM BillFee bi"
                + " where bi.bill.billType= :bTp ";

        if (bill) {
            sql += " and bi.bill.createdAt between :fromDate and :toDate ";
        }
        if (dis) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        }

        if (add) {
            sql += " and bi.billItem.billTime between :fromDate and :toDate ";
        }

        if (institution != null) {
            sql += " and  bi.billItem.item.institution=:ins ";
            m.put("ins", institution);
        }
        if (dept != null) {
            sql += " and  bi.billItem.item.department=:dept ";
            m.put("dept", dept);
        }
        if (category != null) {
            sql += " and  bi.billItem.item.category=:cat ";
            m.put("cat", category);
        }
        if (item != null) {
            sql += " and  bi.billItem.item=:item ";
            m.put("item", item);
        }
        if (getPaymentMethod() != null) {
            sql += " and bi.bill.paymentMethod=:p ";
            m.put("p", getPaymentMethod());
        }

        return getBillFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    public List<BillFee> bilfees(Bill b, boolean dis, boolean add, boolean bill) {
        String sql;
        Map m = new HashMap();
        sql = "select bi FROM BillFee bi where "
                + " bi.bill.id=" + b.getId();

        if (bill) {
            sql += " and bi.bill.createdAt between :fromDate and :toDate ";
        }
        if (dis) {
            sql += " and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
        }

        if (add) {
            sql += " and bi.billItem.billTime between :fromDate and :toDate ";
        }

        sql += " order by bi.id ";

        m.put("toDate", getToDate());
        m.put("fromDate", getFromDate());

        return getBillFeeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

    }

    List<BillWithBillFees> billWithBillFeeses = new ArrayList<>();

    public void createBillWithBillFee(boolean dis, boolean add, boolean bill) {

        List<Bill> bills = billsOutSide(dis, add, bill, new BilledBill());

        for (Bill b : bills) {
            BillWithBillFees bf = new BillWithBillFees();

            bf.setBill(b);
            bf.setBillFees(bilfees(b, dis, add, bill));
            bf.setHosTotal(calBillHosTotals(bf.getBillFees()));
            if (bf.getHosTotal() > 0) {
                billWithBillFeeses.add(bf);
            }
        }

        total = calHosTotals(dis, add, bill);
    }

    public List<BillWithBillFees> getBillWithBillFeeses() {
        return billWithBillFeeses;
    }

    public void setBillWithBillFeeses(List<BillWithBillFees> billWithBillFeeses) {
        this.billWithBillFeeses = billWithBillFeeses;
    }

    //
    public void createItemWithFeeByAddedDate2() {
        Date startTime = new Date();

        makeListNull();
        String sql;
        Map temMap = new HashMap();
        billItem = new ArrayList<>();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bTp", BillType.InwardBill);

        ////// // System.out.println("in");
        if (getPaymentMethod() == null) {
            sql = "select bi "
                    + " FROM BillFee bi "
                    + " where bi.retired=false "
                    + " and bi.bill.retired=false "
                    + " and bi.billItem.retired=false"
                    //                    + " and  bi.bill.institution=:ins "
                    + " and  bi.bill.billType= :bTp  "
                    + " and  bi.billItem.billTime between :fromDate and :toDate ";
            if (institution != null) {
                sql += " and  bi.billItem.item.institution=:ins ";
                temMap.put("ins", institution);
            }
            if (dept != null) {
                sql += " and  bi.billItem.item.department=:dept ";
                temMap.put("dept", dept);
            }
            if (category != null) {
                sql += " and  bi.billItem.item.category=:cat ";
                temMap.put("cat", category);
            }
            if (item != null) {
                sql += " and  bi.billItem.item=:item ";
                temMap.put("item", item);
            }
        } else {
            sql = "select bi "
                    + " FROM BillFee bi"
                    + " where bi.bill.billType= :bTp  "
                    + " and  bi.billItem.billTime between :fromDate and :toDate "
                    + " and bi.bill.paymentMethod=:p ";
            if (institution != null) {
                sql += " and  bi.billItem.item.institution=:ins ";
                temMap.put("ins", institution);
            }
            if (dept != null) {
                sql += " and  bi.billItem.item.department=:dept ";
                temMap.put("dept", dept);
            }
            if (category != null) {
                sql += " and  bi.billItem.item.category=:cat ";
                temMap.put("cat", category);
            }
            if (item != null) {
                sql += " and  bi.billItem.item=:item ";
                temMap.put("item", item);
            }

            temMap.put("p", getPaymentMethod());
        }

        sql += " order by bi.billItem.bill.patientEncounter.bhtNo ";

        billfees = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        ////// // System.out.println("out");

        total = 0.0;
        for (BillFee bf : billfees) {
            total += bf.getFee().getFee();
            ////// // System.out.println("total = " + total);
        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Inward Reports/Service Report/Report by item/process by addeed date(/faces/inward/report_md_inward_item_1.xhtml)");

    }

    public void createItemWithFeeByDischargeDate1() {
        Date startTime = new Date();

        makeListNull();
        String sql;
        billItem = new ArrayList<>();
        Map temMap = new HashMap();

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bTp", BillType.InwardBill);
        ////// // System.out.println("in");
        if (getPaymentMethod() == null) {
            sql = "select bi "
                    + " FROM BillFee bi "
                    + " where bi.retired=false "
                    + " and bi.bill.retired=false "
                    + " and bi.billItem.retired=false"
                    //                    + " and  bi.bill.institution=:ins "
                    + " and  bi.bill.billType= :bTp  "
                    + " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";

            if (institution != null) {
                sql += " and  bi.billItem.item.institution=:ins ";
                temMap.put("ins", institution);
            }
            if (dept != null) {
                sql += " and  bi.billItem.item.department=:dept ";
                temMap.put("dept", dept);
            }
            if (category != null) {
                sql += " and  bi.billItem.item.category=:cat ";
                temMap.put("cat", category);
            }
            if (item != null) {
                sql += " and  bi.billItem.item=:item ";
                temMap.put("item", item);
            }
        } else {
            sql = "select bi "
                    + " FROM BillFee bi"
                    + " where bi.bill.billType= :bTp  "
                    + " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate "
                    + " and bi.bill.paymentMethod=:p ";
            if (institution != null) {
                sql += " and  bi.billItem.item.institution=:ins ";
                temMap.put("ins", institution);
            }
            if (dept != null) {
                sql += " and  bi.billItem.item.department=:dept ";
                temMap.put("dept", dept);
            }
            if (category != null) {
                sql += " and  bi.billItem.item.category=:cat ";
                temMap.put("cat", category);
            }
            if (item != null) {
                sql += " and  bi.billItem.item=:item ";
                temMap.put("item", item);
            }

            temMap.put("p", getPaymentMethod());
        }

        sql += " order by bi.billItem.bill.patientEncounter.bhtNo ";

        billfees = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        ////// // System.out.println("out");

        total = 0.0;
        for (BillFee bf : billfees) {
            total += bf.getFee().getFee();
            ////// // System.out.println("total = " + total);
        }

        commonController.printReportDetails(fromDate, toDate, startTime, "Inward Reports/Service Report/Report by item/process by dichraged date(/faces/inward/report_md_inward_item_1.xhtml)");

    }

    public List<ItemWithFee> getItemWithFees() {

        return itemWithFees;
    }

    public List<ItemWithFee> getItemWithFeesDischarged() {

        String sql;
        List<Item> tmp;
        Map temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bTp", BillType.InwardBill);
        temMap.put("ins", getSessionController().getInstitution());

        if (itemWithFees == null) {

            itemWithFees = new ArrayList<ItemWithFee>();

            if (getPaymentMethod() == null) {
                sql = "select distinct(bi.item) FROM BillItem bi where bi.retired=false and bi.item.retired=false and  bi.bill.institution=:ins and  bi.bill.billType= :bTp  "
                        + " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";

            } else {
                sql = "select distinct(bi.item) FROM BillItem bi where  bi.bill.institution=:ins and  bi.bill.billType= :bTp  "
                        + " and  bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate and bi.bill.paymentMethod=:p ";

                temMap.put("p", getPaymentMethod());

            }

            tmp = getItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

            for (Item i : tmp) {
                ItemWithFee iwf = new ItemWithFee();
                iwf.setItem(i);
                setCountDischarge(iwf);
                setFeeDischarge(iwf);
                //////// // System.out.println("ss " + itemWithFees.size());
                //////// // System.out.println("ss " + iwf.getItem());
                itemWithFees.add(iwf);
            }

        }

        return itemWithFees;
    }

    private List<BillItem> billItemForCount(Bill bill, Item i) {
        if (i == null) {
            return new ArrayList<BillItem>();
        }

        Map temMap = new HashMap();
        String sql;

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billClass", bill.getClass());
        temMap.put("btp", BillType.InwardBill);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("item", i);

        if (getPaymentMethod() == null) {
            sql = "select bi FROM BillItem bi where  bi.bill.institution=:ins and bi.item=:item"
                    + " and type(bi.bill)=:billClass and bi.bill.billType=:btp and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";

        } else {
            sql = "select bi FROM BillItem bi where  bi.bill.institution=:ins and bi.item=:item"
                    + " and bi.bill.paymentMethod = :pm and bi.bill.billType=:btp and type(bi.bill)=:billClass and bi.bill.createdAt between :fromDate and :toDate order by bi.item.name";

            temMap.put("pm", getPaymentMethod());
        }

        return getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private List<BillItem> billItemForCountDischarge(Bill bill, Item i) {
        if (i == null) {
            return new ArrayList<BillItem>();
        }

        Map temMap = new HashMap();
        String sql;
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("billClass", bill.getClass());
        temMap.put("btp", BillType.InwardBill);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("item", i);

        if (getPaymentMethod() == null) {
            sql = "select bi FROM BillItem bi where  bi.bill.institution=:ins and bi.item=:item"
                    + " and type(bi.bill)=:billClass and bi.bill.billType=:btp and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate order by bi.item.name";

        } else {
            sql = "select bi FROM BillItem bi where  bi.bill.institution=:ins and bi.item=:item"
                    + " and bi.bill.paymentMethod = :pm and bi.bill.billType=:btp and type(bi.bill)=:billClass and bi.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate order by bi.item.name";

            temMap.put("pm", getPaymentMethod());

        }

        return getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

    }

    private void setCount(ItemWithFee i) {
        double billed, cancelled, refunded;
        billed = cancelled = refunded = 0.0;

        List<BillItem> temps = billItemForCount(new BilledBill(), i.getItem());

        for (BillItem b : temps) {
            billed++;
        }

        temps = billItemForCount(new CancelledBill(), i.getItem());

        for (BillItem b : temps) {
            cancelled++;
        }

        temps = billItemForCount(new RefundBill(), i.getItem());

        for (BillItem b : temps) {
            refunded++;
        }

        i.setCount(billed - cancelled - refunded);

    }

    private void setCountDischarge(ItemWithFee i) {
        double billed, cancelled, refunded;
        billed = cancelled = refunded = 0.0;

        List<BillItem> temps = billItemForCountDischarge(new BilledBill(), i.getItem());

        for (BillItem b : temps) {
            billed++;
        }

        temps = billItemForCountDischarge(new CancelledBill(), i.getItem());

        for (BillItem b : temps) {
            cancelled++;
        }

        temps = billItemForCountDischarge(new RefundBill(), i.getItem());

        for (BillItem b : temps) {
            refunded++;
        }

        i.setCount(billed - cancelled - refunded);

    }

    private void setFee(ItemWithFee i) {
        if (i.getItem() == null) {
            return;
        }

        double hospiatalFee = 0.0;
        double staffFee = 0.0;
        String sql;
        HashMap temMap = new HashMap();

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bTp", BillType.InwardBill);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("item", i.getItem());

        if (getPaymentMethod() == null) {
            sql = "SELECT bf FROM BillFee bf WHERE   bf.billItem.id in"
                    + "(SELECT b.id from BillItem b where b.bill.billType=:bTp"
                    + " and b.bill.institution=:ins"
                    + " and b.bill.createdAt between :fromDate and :toDate  and b.item=:item)";

        } else {
            sql = "SELECT bf FROM BillFee bf WHERE   bf.billItem.id in"
                    + "(SELECT b.id from BillItem b where b.bill.billType=:bTp"
                    + " and b.bill.institution=:ins"
                    + " and b.bill.createdAt between :fromDate and :toDate  and b.item=:item"
                    + " and  b.bill.paymentMethod = :pm)";

            temMap.put("pm", getPaymentMethod());
        }

        List<BillFee> billFees = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        for (BillFee b : billFees) {
            if (b.getStaff() != null) {
                staffFee += b.getFeeValue();
            } else {
                hospiatalFee += b.getFeeValue();
            }
        }

        i.setHospitalFee(hospiatalFee);
        i.setProFee(staffFee);
        i.setTotal(hospiatalFee + staffFee);

    }

    private void setFeeDischarge(ItemWithFee i) {
        if (i.getItem() == null) {
            return;
        }

        double hospiatalFee = 0.0;
        double staffFee = 0.0;
        String sql;
        HashMap temMap = new HashMap();
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bTp", BillType.InwardBill);
        temMap.put("ins", getSessionController().getInstitution());
        temMap.put("item", i.getItem());

        if (getPaymentMethod() == null) {
            sql = "SELECT bf FROM BillFee bf WHERE   bf.billItem.id in"
                    + "(SELECT b.id from BillItem b where b.bill.billType=:bTp"
                    + " and b.bill.institution=:ins"
                    + " and b.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate  and b.item=:item)";

        } else {
            sql = "SELECT bf FROM BillFee bf WHERE   bf.billItem.id in"
                    + "(SELECT b.id from BillItem b where b.bill.billType=:bTp"
                    + " and b.bill.institution=:ins"
                    + " and b.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate  and b.item=:item"
                    + " and  b.bill.paymentMethod = :pm)";

            temMap.put("pm", getPaymentMethod());
        }

        List<BillFee> billFees = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        for (BillFee b : billFees) {
            if (b.getStaff() != null) {
                staffFee += b.getFeeValue();
            } else {
                hospiatalFee += b.getFeeValue();
            }
        }

        i.setHospitalFee(hospiatalFee);
        i.setProFee(staffFee);
        i.setTotal(hospiatalFee + staffFee);

    }

    public void setItemWithFees(List<ItemWithFee> itemWithFees) {
        this.itemWithFees = itemWithFees;
    }

    public double getBilledCreditValue() {
        return billedCreditValue;
    }

    public void setBilledCreditValue(double billedCreditValue) {
        this.billedCreditValue = billedCreditValue;
    }

    public double getBilledCreditCardValue() {
        return billedCreditCardValue;
    }

    public void setBilledCreditCardValue(double billedCreditCardValue) {
        this.billedCreditCardValue = billedCreditCardValue;
    }

    public double getBilledSlipValue() {
        return billedSlipValue;
    }

    public void setBilledSlipValue(double billedSlipValue) {
        this.billedSlipValue = billedSlipValue;
    }

    public double getBilledChequeValue() {
        return billedChequeValue;
    }

    public void setBilledChequeValue(double billedChequeValue) {
        this.billedChequeValue = billedChequeValue;
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

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        itemWithFees = null;
        fillterItemWithFees = null;
        this.paymentMethod = paymentMethod;
    }

    public List<ItemWithFee> getFillterItemWithFees() {
        return fillterItemWithFees;
    }

    public void setFillterItemWithFees(List<ItemWithFee> fillterItemWithFees) {
        this.fillterItemWithFees = fillterItemWithFees;
    }

    public List<BillItem> getbillItem() {
        return billItem;
    }

    public void setbillItem(List<BillItem> billItem) {
        this.billItem = billItem;
    }

    public Department getDept() {
        return dept;
    }

    public void setDept(Department dept) {
        this.dept = dept;
    }

    public List<BillItem> getBillItem() {
        return billItem;
    }

    public void setBillItem(List<BillItem> billItem) {
        this.billItem = billItem;
    }

    public double getBilledAgentValue() {
        return billedAgentValue;
    }

    public void setBilledAgentValue(double billedAgentValue) {
        this.billedAgentValue = billedAgentValue;
    }

    public double getCancelledCashValue() {
        return cancelledCashValue;
    }

    public void setCancelledCashValue(double cancelledCashValue) {
        this.cancelledCashValue = cancelledCashValue;
    }

    public double getCancelledCreditValue() {
        return cancelledCreditValue;
    }

    public void setCancelledCreditValue(double cancelledCreditValue) {
        this.cancelledCreditValue = cancelledCreditValue;
    }

    public double getCancelledCreditCardValue() {
        return cancelledCreditCardValue;
    }

    public void setCancelledCreditCardValue(double cancelledCreditCardValue) {
        this.cancelledCreditCardValue = cancelledCreditCardValue;
    }

    public double getCancelledSlipValue() {
        return cancelledSlipValue;
    }

    public void setCancelledSlipValue(double cancelledSlipValue) {
        this.cancelledSlipValue = cancelledSlipValue;
    }

    public double getCancelledChequeValue() {
        return cancelledChequeValue;
    }

    public void setCancelledChequeValue(double cancelledChequeValue) {
        this.cancelledChequeValue = cancelledChequeValue;
    }

    public double getCancelledAgentValue() {
        return cancelledAgentValue;
    }

    public void setCancelledAgentValue(double cancelledAgentValue) {
        this.cancelledAgentValue = cancelledAgentValue;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }

    public List<Bill> getDeposits() {
        return deposits;
    }

    public void setDeposits(List<Bill> deposits) {
        this.deposits = deposits;
    }

    public List<Bill> getCompletePayments() {
        return completePayments;
    }

    public void setCompletePayments(List<Bill> completePayments) {
        this.completePayments = completePayments;
    }

    public double getDepositsTotal() {
        return depositsTotal;
    }

    public void setDepositsTotal(double depositsTotal) {
        this.depositsTotal = depositsTotal;
    }

    public double getCompletePaymentsTotal() {
        return completePaymentsTotal;
    }

    public void setCompletePaymentsTotal(double completePaymentsTotal) {
        this.completePaymentsTotal = completePaymentsTotal;
    }

    public double getGrantTotal() {
        return grantTotal;
    }

    public void setGrantTotal(double grantTotal) {
        this.grantTotal = grantTotal;
    }

    List<PatientEncounterValue> patientEncounterValues;

    public List<PatientEncounterValue> getPatientEncounterValues() {
        return patientEncounterValues;
    }

    public void setPatientEncounterValues(List<PatientEncounterValue> patientEncounterValues) {
        this.patientEncounterValues = patientEncounterValues;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public List<BillFee> getBillfees() {
        return billfees;
    }

    public void setBillfees(List<BillFee> billfees) {
        this.billfees = billfees;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public boolean isShowCreatedDate() {
        return showCreatedDate;
    }

    public void setShowCreatedDate(boolean showCreatedDate) {
        this.showCreatedDate = showCreatedDate;
    }

    public boolean isShowServiceDate() {
        return showServiceDate;
    }

    public void setShowServiceDate(boolean showServiceDate) {
        this.showServiceDate = showServiceDate;
    }

    public boolean isShowDischargeDate() {
        return showDischargeDate;
    }

    public void setShowDischargeDate(boolean showDischargeDate) {
        this.showDischargeDate = showDischargeDate;
    }

    public boolean isShowDepartment() {
        return showDepartment;
    }

    public void setShowDepartment(boolean showDepartment) {
        this.showDepartment = showDepartment;
    }

    public boolean isShowCategory() {
        return showCategory;
    }

    public void setShowCategory(boolean showCategory) {
        this.showCategory = showCategory;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public double getPurchaseValue() {
        return purchaseValue;
    }

    public void setPurchaseValue(double purchaseValue) {
        this.purchaseValue = purchaseValue;
    }

    public int getManagaeInwardReportIndex() {
        return managaeInwardReportIndex;
    }

    public void setManagaeInwardReportIndex(int managaeInwardReportIndex) {
        this.managaeInwardReportIndex = managaeInwardReportIndex;
    }

    //619
    public class BillWithBillFees {

        Bill bill;
        List<BillFee> billFees;
        double hosTotal;

        public Bill getBill() {
            return bill;
        }

        public void setBill(Bill bill) {
            this.bill = bill;
        }

        public List<BillFee> getBillFees() {
            return billFees;
        }

        public void setBillFees(List<BillFee> billFees) {
            this.billFees = billFees;
        }

        public double getHosTotal() {
            return hosTotal;
        }

        public void setHosTotal(double hosTotal) {
            this.hosTotal = hosTotal;
        }

    }
    //

    public class PatientEncounterValue {

        PatientEncounter pe;
        Double paid;
        Double paidByCreditCompany;
        Double due;

        public Double getPaidByCreditCompany() {
            return paidByCreditCompany;
        }

        public void setPaidByCreditCompany(Double paidByCreditCompany) {
            this.paidByCreditCompany = paidByCreditCompany;
        }

        public PatientEncounter getPe() {
            return pe;
        }

        public void setPe(PatientEncounter pe) {
            this.pe = pe;
        }

        public Double getPaid() {
            return paid;
        }

        public void setPaid(Double paid) {
            this.paid = paid;
        }

        public Double getDue() {
            return due;
        }

        public void setDue(Double due) {
            this.due = due;
        }

    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

}
