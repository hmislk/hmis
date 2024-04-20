package com.divudi.bean.inward;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;

import com.divudi.data.BillClassType;
import com.divudi.data.BillNumberSuffix;
import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.data.table.String1Value1;
import com.divudi.data.table.String2Value1;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.ejb.CashTransactionBean;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.BillTypeAtomic;
import com.divudi.entity.Bill;
import com.divudi.entity.BillComponent;
import com.divudi.entity.BillFee;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.Institution;
import com.divudi.entity.Speciality;
import com.divudi.entity.Staff;
import com.divudi.entity.WebUser;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.facade.BillComponentFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillFeeFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.CancelledBillFacade;
import com.divudi.facade.RefundBillFacade;
import com.divudi.facade.StaffFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class InwardStaffPaymentBillController implements Serializable {

    @Inject
    CommonController commonController;

    @EJB
    private RefundBillFacade refundBillFacade;
    private List<BillComponent> billComponents;
    @EJB
    private CancelledBillFacade cancelledBillFacade;
    @EJB
    private BillComponentFacade billComponentFacade;
    @EJB
    BillFeeFacade billFeeFacade;
    private List<BillItem> billItems;
    private List<BillItem> docPayDischarged;
    private List<BillItem> docPayNotDischarged;
    List<Speciality> selectedItems;
    private static final long serialVersionUID = 1L;
    private Date fromDate;
    private Date toDate;
    @Inject
    SessionController sessionController;

    private CommonFunctions commonFunctions;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;
    private Bill current;
    private List<Bill> items = null;
    List<Bill> bills;
    List<Bill> billsCan;

    Staff currentStaff;
    List<BillFee> dueBillFees;
    List<BillFee> payingBillFees;
    double totalDue;
    double totalPaying;
    double totalPayingCan;
    @EJB
    BillNumberGenerator billNumberBean;
    private Boolean printPreview = false;
    PaymentMethod paymentMethod;
    Speciality speciality;
    Speciality referringDoctorSpeciality;
    @EJB
    StaffFacade staffFacade;
    List<String1Value1> list;
    List<String2Value1> list1;
    List<BillFee> docPayingBillFee;
    List<BillItem> billItems1;

    List<String2Value1> docPayListDischarged;
    List<String2Value1> docPayListNotDischarged;

    List<BillItem> docFeePayDischarged;
    List<BillItem> docFeePayNotDischarged;

    List<BillFee> docFeeDueDischarged;
    List<BillFee> docFeeDueNotDischarged;

    double totalDocFeePayDischarged;
    double totalDocFeePayNotDischarged;

    double totalDocFeeDueDischarged;
    double totalDocFeeDueNotDischarged;

    double totalPaid = 0.0;
    double totalVal = 0.0;

    List<BillFee> bhtBillItemList;
    List<BillFee> bhtDueList;

    SearchKeyword searchKeyword;

    public void makenull() {
        currentStaff = null;
        speciality = null;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String navigateToInwardPayStaff() {
        speciality = null;
        currentStaff = null;
        paymentMethod = null;
        dueBillFees = null;
        totalDue = 0.0;
        totalPaying = 0.0;
        printPreview = false;
        return "/inward/inward_bill_staff_payment?faces-redirect=true";
    }

    AdmissionType admissionType;
    Institution institution;

    public void fillDocPayingBillFeeByCreatedDate() {
        Date startTime = new Date();

        fillDocPayingBillFee(false);

        

    }

    public void fillDocPayingBillFeeByDischargeDate() {
        fillDocPayingBillFee(true);
    }

    public void fillDocPayingBillFee(boolean dischargeDate) {

        String sql;
        Map m = new HashMap();

        sql = "select bf from BillItem bf "
                + " where bf.retired=false "
                + " and bf.bill.billType=:btp"
                + " and (bf.paidForBillFee.bill.billType=:refBtp1"
                + " or bf.paidForBillFee.bill.billType=:refBtp2)";

        if (dischargeDate) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.dateOfDischarge between :fd and :td ";
        } else {
            sql += " and bf.createdAt between :fd and :td ";
        }

        if (speciality != null) {
            sql += " and bf.paidForBillFee.staff.speciality=:s ";
            m.put("s", speciality);
        }

        if (currentStaff != null) {
            sql += " and bf.paidForBillFee.staff=:cs";
            m.put("cs", currentStaff);
        }

        if (admissionType != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.admissionType=:admTp ";
            m.put("admTp", admissionType);
        }
        if (paymentMethod != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.paymentMethod=:pm";
            m.put("pm", paymentMethod);
        }
        if (institution != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.creditCompany=:cd";
            m.put("cd", institution);
        }

        sql += " order by bf.bill.insId ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("btp", BillType.PaymentBill);
        m.put("refBtp1", BillType.InwardBill);
        m.put("refBtp2", BillType.InwardProfessional);

        billItems1 = getBillItemFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        totalPaying = 0.0;
        if (billItems1 == null) {
            return;
        }
        for (BillItem dFee : billItems1) {
            totalPaying += dFee.getPaidForBillFee().getFeeValue();
        }

    }

    public void fillDocPayingBillByCreatedDate() {
        Date startTime = new Date();

        fillDocPayingBill(false);
        fillDocPayingBillCancel(false);

        
    }

    public void fillDocPayingBillByDischargeDate() {
        Date startTime = new Date();

        fillDocPayingBill(true);
        fillDocPayingBillCancel(true);

        
    }

    public void fillDocPayingBill(boolean dischargeDate) {

        String sql;
        Map m = new HashMap();

        sql = "select distinct(bf.bill) from BillItem bf "
                + " where bf.retired=false "
                + " and bf.bill.billType=:btp"
                + " and (bf.paidForBillFee.bill.billType=:refBtp1"
                + " or bf.paidForBillFee.bill.billType=:refBtp2)";

        if (dischargeDate) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.dateOfDischarge between :fd and :td ";
        } else {
            sql += " and bf.createdAt between :fd and :td ";
        }

        if (speciality != null) {
            sql += " and bf.paidForBillFee.staff.speciality=:s ";
            m.put("s", speciality);
        }

        if (currentStaff != null) {
            sql += " and bf.paidForBillFee.staff=:cs";
            m.put("cs", currentStaff);
        }

        if (admissionType != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.admissionType=:admTp ";
            m.put("admTp", admissionType);
        }
        if (paymentMethod != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.paymentMethod=:pm";
            m.put("pm", paymentMethod);
        }
        if (institution != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.creditCompany=:cd";
            m.put("cd", institution);
        }

        sql += " order by bf.bill.insId ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("btp", BillType.PaymentBill);
        m.put("refBtp1", BillType.InwardBill);
        m.put("refBtp2", BillType.InwardProfessional);

        bills = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        totalPaying = 0.0;
        if (bills == null) {
            return;
        }
        for (Bill b : bills) {
            totalPaying += b.getNetTotal();
        }

    }

    public void fillDocPayingBillCancel(boolean dischargeDate) {

        String sql;
        Map m = new HashMap();

        sql = "select distinct(bf.bill.cancelledBill) from BillItem bf "
                + " where bf.retired=false "
                + " and bf.bill.billType=:btp "
                + " and bf.bill.cancelled=true "
                + " and (bf.paidForBillFee.bill.billType=:refBtp1 "
                + " or bf.paidForBillFee.bill.billType=:refBtp2)";

        if (dischargeDate) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.dateOfDischarge between :fd and :td ";
        } else {
            sql += " and bf.createdAt between :fd and :td ";
        }

        if (speciality != null) {
            sql += " and bf.paidForBillFee.staff.speciality=:s ";
            m.put("s", speciality);
        }

        if (currentStaff != null) {
            sql += " and bf.paidForBillFee.staff=:cs";
            m.put("cs", currentStaff);
        }

        if (admissionType != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.admissionType=:admTp ";
            m.put("admTp", admissionType);
        }
        if (paymentMethod != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.paymentMethod=:pm";
            m.put("pm", paymentMethod);
        }
        if (institution != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.creditCompany=:cd";
            m.put("cd", institution);
        }

        sql += " order by bf.bill.insId ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("btp", BillType.PaymentBill);
        m.put("refBtp1", BillType.InwardBill);
        m.put("refBtp2", BillType.InwardProfessional);

        billsCan = getBillFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        totalPayingCan = 0.0;
        if (billsCan == null) {
            billsCan = new ArrayList<>();
            return;
        }
        for (Bill b : billsCan) {
            totalPayingCan += b.getNetTotal();
        }

    }

    public void fillDocPayingBillFeeSummeryCreatedDate() {
        fillDocPayingBillFeeSummery(false);
    }

    public void fillDocPayingBillFeeSummeryDischargeDate() {
        fillDocPayingBillFeeSummery(true);
    }

    private void fillDocPayingBillFeeSummery(boolean dischargeDate) {

        String sql;
        Map m = new HashMap();

        sql = "select bf.paidForBillFee.staff,"
                + " sum(bf.paidForBillFee.feeValue) "
                + " from BillItem bf"
                + " where bf.retired=false "
                + " and bf.bill.billType=:btp"
                + " and (bf.paidForBillFee.bill.billType=:refBtp1"
                + " or bf.paidForBillFee.bill.billType=:refBtp2)";

        if (dischargeDate) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.dateOfDischarge between :fd and :td ";
        } else {
            sql += " and bf.createdAt between :fd and :td ";
        }

        if (speciality != null) {
            sql += " and bf.paidForBillFee.staff.speciality=:s ";
            m.put("s", speciality);
        }

        if (admissionType != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.admissionType=:admTp ";
            m.put("admTp", admissionType);
        }
        if (paymentMethod != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.paymentMethod=:pm";
            m.put("pm", paymentMethod);
        }
        if (institution != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.creditCompany=:cd";
            m.put("cd", institution);
        }

        sql += " group by bf.paidForBillFee.staff "
                + " order by bf.paidForBillFee.staff.person.name ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("btp", BillType.PaymentBill);
        m.put("refBtp1", BillType.InwardBill);
        m.put("refBtp2", BillType.InwardProfessional);

        list1 = new ArrayList<>();
        List<Object[]> objs = getBillFeeFacade().findAggregates(sql, m);
        for (Object[] objc : objs) {
//            String1Value1 roe = new String1Value1();
            String2Value1 roe1 = new String2Value1();
            Staff st = (Staff) objc[0];
//            Staff stc = (Staff) objc[0];
            double val = (double) objc[1];
            roe1.setString1(st.getPerson().getNameWithTitle());
            roe1.setString2(st.getCode());
            roe1.setValue(val);
            list1.add(roe1);

        }

    }

    public void fillDocPayDischargeAndNotDischarge() {
        docPayListDischarged = inwardDoctorPaySummery(true);
        docPayListNotDischarged = inwardDoctorPaySummery(false);

//        totalDocPayListDischarged = calTotal(docPayListDischarged);
//        totalDocPayListNotDischarged = calTotal(docPayListNotDischarged);
    }

    public void fillDocPayDischargeAndNotDischargeWithBHT() {
//        docBhtPayListDischarged = createDoctorPaymentTableInwardDischarged();
//        docBhtPayListnotDischarged = createDoctorPaymentTableInwardNotDischarged();
//        createDueFeeTableAll();
//
//        docFeeListDischarged = createDocFeeTableDischarged();
//        docFeeListNotDischarged = createDocFeeTableNotDischarged();

//        totalDocFeeListDischarged = calBhtTotal(docBhtPayListDischarged);
//        totalDocFeeListNotDischarged = calBhtTotal(docBhtPayListnotDischarged);
//        totalDocFeeDueList = calDueBhtTotal(billFeeDue);
//
//        totalDocFeeListDischarged = calDueBhtTotal(docFeeListDischarged);
//        totalDocFeeListNotDischarged = calDueBhtTotal(docFeeListNotDischarged);
//        //        totalPaidDocFeeListDischarged = calBhtPaidTotal(docBhtPayListDischarged);
        //        totalPadDocFeeListNotDischarged = calBhtPaidTotal(docBhtPayListnotDischarged);
        //        totalPaid = totalDocFeeListDischarged + totalPadDocFeeListNotDischarged;
        //        totalVal = totalPaidDocFeeListDischarged + totalPadDocFeeListNotDischarged;
        docFeePayDischarged = createDoctorPaymentTableInwardDischarged();
        docFeePayNotDischarged = createDoctorPaymentTableInwardNotDischarged();
        docFeeDueDischarged = createDocDueFeeTableDischarged();
        docFeeDueNotDischarged = createDocDueFeeTableNotDischarged();
        totalDocFeePayDischarged = calPaidTotal(docFeePayDischarged);
        totalDocFeePayNotDischarged = calPaidTotal(docFeePayNotDischarged);
        totalDocFeeDueDischarged = calDueTotal(docFeeDueDischarged);
        totalDocFeeDueNotDischarged = calDueTotal(docFeeDueNotDischarged);
    }

    public List<BillItem> createDoctorPaymentTableInwardDischarged() {
        docPayDischarged = null;
        HashMap temMap = new HashMap();
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.paidForBillFee.bill.patientEncounter.discharged=true "
                + " and (b.referenceBill.billType=:refType "
                + " or b.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate "
                + " order by b.createdAt desc ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);

        docPayDischarged = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        ////// // System.out.println("docPayDischarged = " + docPayDischarged);
        return docPayDischarged;
    }

    public List<BillItem> createDoctorPaymentTableInwardNotDischarged() {
        docPayNotDischarged = null;
        HashMap temMap = new HashMap();
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.paidForBillFee.bill.patientEncounter.discharged=false"
                + " and (b.referenceBill.billType=:refType "
                + " or b.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate "
                + " order by b.createdAt desc ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);

        docPayNotDischarged = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
        ////// // System.out.println("docPayNotDischarged = " + docPayNotDischarged);
        return docPayNotDischarged;

    }

    public double calTotal(List<String2Value1> string2Value1s) {
        double total = 0.0;
        for (String2Value1 s2v1 : string2Value1s) {
            total += s2v1.getValue();
        }
        return total;
    }

    public double calPaidTotal(List<BillItem> bhtbillItems) {
        double bhtTotal = 0.0;
        ////// // System.out.println("Items = " + bhtbillItems);
        for (BillItem bhtb : bhtbillItems) {
            bhtTotal += bhtb.getPaidForBillFee().getFeeValue();
        }
        return bhtTotal;
    }

//    public double calBhtPaidTotal(List<BillFee> bhtbillItems) {
//
//        double bhtPaidTotal = 0.0;
//        ////// // System.out.println("Bill Items = " + bhtbillItems);
//        for (BillFee bhtb : bhtbillItems) {
//            bhtPaidTotal += bhtb.getBillItem().getPaidForBillFee().getFeeValue();
//        }
//        return bhtPaidTotal;
//    }
    List<BillFee> billFeeDueDischarged;

    public List<BillFee> createDocDueFeeTableDischarged() {

        String sql;
        Map temMap = new HashMap();
        billFeeDueDischarged = new ArrayList<>();

        sql = "select b from BillFee b where "
                + " b.retired=false "
                + " and (b.bill.billType=:btp or b.bill.billType=:btp2 )"
                + " and b.bill.cancelled=false "
                + " and b.billItem.bill.patientEncounter.discharged=true "
                + " and (b.feeValue - b.paidValue) > 0"
                + " and b.bill.billDate between :fromDate and :toDate "
                + " order by b.staff.id ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.InwardBill);
        temMap.put("btp2", BillType.InwardProfessional);

        billFeeDueDischarged = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return billFeeDueDischarged;

    }

    List<BillFee> billFeeDueNotDischarged;

    public List<BillFee> createDocDueFeeTableNotDischarged() {

        String sql;
        Map temMap = new HashMap();
        billFeeDueNotDischarged = new ArrayList<>();

        sql = "select b from BillFee b where "
                + " b.retired=false "
                + " and (b.bill.billType=:btp or b.bill.billType=:btp2 )"
                + " and b.bill.cancelled=false "
                + " and b.billItem.bill.patientEncounter.discharged=false "
                + " and (b.feeValue - b.paidValue) > 0"
                + " and  b.bill.billDate between :fromDate and :toDate "
                + " order by b.staff.id ";

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("btp", BillType.InwardBill);
        temMap.put("btp2", BillType.InwardProfessional);

        billFeeDueNotDischarged = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        return billFeeDueNotDischarged;

    }

    public double calDueTotal(List<BillFee> bhtbillItems) {
        double bhtDueTotal = 0.0;
        ////// // System.out.println("Due Items = " + bhtbillItems);
        for (BillFee bhtb : bhtbillItems) {
            bhtDueTotal += bhtb.getFeeValue();
        }
        return bhtDueTotal;
    }

//    public List<BillFee> createDocFeeTableDischarged() {
//
//        String sql;
//        Map temMap = new HashMap();
//
////        bf.getBillItem().getBill().getPatientEncounter().getDischarged();
//        sql = "select bf from BillFee bf where bf.retired=false "
//                + " and bf.bill.billType=:btp "
//                + " and bf.bill.cancelled=false "
//                + " and bf.billItem.bill.patientEncounter.discharged=true"
//                + " and bf.paidValue > 0 "
//                + " and bf.bill.createdAt between :fromDate and :toDate ";
////                + " and bf.billItem.paidForBillFee.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
//
//        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
//            sql += " and  ((bf.bill.patient.person.name) like :patientName )";
//            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
//        }
//
//        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
//            sql += " and  ((bf.bill.insId) like :billNo )";
//            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
//        }
//
//        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
//            sql += " and  ((bf.feeValue) like :total )";
//            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
//        }
//
//        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
//            sql += " and  ((bf.staff.speciality.name) like :special )";
//            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
//        }
//
//        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
//            sql += " and  ((bf.staff.person.name) like :staff )";
//            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
//        }
//
//        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
//            sql += " and  ((bf.billItem.item.name) like :staff )";
//            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
//        }
//
//        sql += " order by bf.staff.id ";
//
//        temMap.put("toDate", getToDate());
//        temMap.put("fromDate", getFromDate());
//        temMap.put("btp", BillType.InwardBill);
//
//        bhtBillItemList = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//        return bhtBillItemList;
//
//    }
//    public List<BillFee> createDocFeeTableNotDischarged() {
//
//        String sql;
//        Map temMap = new HashMap();
//
////        BillFee bf;
////        bf.getBillItem().getBill().getPatientEncounter().getDischarged();
//        sql = "select bf from BillFee bf where bf.retired=false "
//                + " and bf.bill.billType=:btp "
//                + " and bf.bill.cancelled=false "
//                + " and bf.paidValue > 0 "
//                + " and bf.bill.createdAt between :fromDate and :toDate ";
//
////        if (dischargedDate) {
////            sql += " and bf.billItem.paidForBillFee.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
////        } else {
////            sql += " and bf.bill.createdAt between :fromDate and :toDate";
////        }
//        if (getSearchKeyword().getPatientName() != null && !getSearchKeyword().getPatientName().trim().equals("")) {
//            sql += " and  ((bf.bill.patient.person.name) like :patientName )";
//            temMap.put("patientName", "%" + getSearchKeyword().getPatientName().trim().toUpperCase() + "%");
//        }
//
//        if (getSearchKeyword().getBillNo() != null && !getSearchKeyword().getBillNo().trim().equals("")) {
//            sql += " and  ((bf.bill.insId) like :billNo )";
//            temMap.put("billNo", "%" + getSearchKeyword().getBillNo().trim().toUpperCase() + "%");
//        }
//
//        if (getSearchKeyword().getTotal() != null && !getSearchKeyword().getTotal().trim().equals("")) {
//            sql += " and  ((bf.feeValue) like :total )";
//            temMap.put("total", "%" + getSearchKeyword().getTotal().trim().toUpperCase() + "%");
//        }
//
//        if (getSearchKeyword().getSpeciality() != null && !getSearchKeyword().getSpeciality().trim().equals("")) {
//            sql += " and  ((bf.staff.speciality.name) like :special )";
//            temMap.put("special", "%" + getSearchKeyword().getSpeciality().trim().toUpperCase() + "%");
//        }
//
//        if (getSearchKeyword().getStaffName() != null && !getSearchKeyword().getStaffName().trim().equals("")) {
//            sql += " and  ((bf.staff.person.name) like :staff )";
//            temMap.put("staff", "%" + getSearchKeyword().getStaffName().trim().toUpperCase() + "%");
//        }
//
//        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
//            sql += " and  ((bf.billItem.item.name) like :staff )";
//            temMap.put("staff", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
//        }
//
//        sql += " order by bf.staff.id ";
//
//        temMap.put("toDate", getToDate());
//        temMap.put("fromDate", getFromDate());
//        temMap.put("btp", BillType.InwardBill);
//
//        bhtBillItemList = getBillFeeFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//        return bhtBillItemList;
//
//    }
    public List<String2Value1> inwardDoctorPaySummery(boolean dischargeDate) {

        String sql;
        Map m = new HashMap();

        sql = "select bf.paidForBillFee.staff,"
                + " sum(bf.paidForBillFee.feeValue) "
                + " from BillItem bf"
                + " where bf.retired=false "
                + " and bf.bill.billType=:btp"
                + " and (bf.paidForBillFee.bill.billType=:refBtp1"
                + " or bf.paidForBillFee.bill.billType=:refBtp2)";

        if (dischargeDate) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.dateOfDischarge between :fd and :td ";
        }

        if (speciality != null) {
            sql += " and bf.paidForBillFee.staff.speciality=:s ";
            m.put("s", speciality);
        }

        if (admissionType != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.admissionType=:admTp ";
            m.put("admTp", admissionType);
        }
        if (paymentMethod != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.paymentMethod=:pm";
            m.put("pm", paymentMethod);
        }
        if (institution != null) {
            sql += " and bf.paidForBillFee.bill.patientEncounter.creditCompany=:cd";
            m.put("cd", institution);
        }
        sql += " and bf.createdAt between :fd and :td ";

        sql += " group by bf.paidForBillFee.staff "
                + " order by bf.paidForBillFee.staff.person.name ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("btp", BillType.PaymentBill);
        m.put("refBtp1", BillType.InwardBill);
        m.put("refBtp2", BillType.InwardProfessional);

        List<String2Value1> st2val1s = new ArrayList<>();
        List<Object[]> objs = getBillFeeFacade().findAggregates(sql, m);
        for (Object[] objc : objs) {
//            String1Value1 roe = new String1Value1();
            String2Value1 roe1 = new String2Value1();
            Staff st = (Staff) objc[0];
//            Staff stc = (Staff) objc[0];
            double val = (double) objc[1];
            roe1.setString1(st.getPerson().getNameWithTitle());
            roe1.setString2(st.getCode());
            roe1.setValue(val);
            st2val1s.add(roe1);

        }

        return st2val1s;
    }

    public List<BillComponent> getBillComponents() {
        if (getCurrent() != null) {
            String sql = "SELECT b FROM BillComponent b WHERE b.retired=false and b.bill.id=" + getCurrent().getId();
            billComponents = getBillComponentFacade().findByJpql(sql);
            if (billComponents == null) {
                billComponents = new ArrayList<>();
            }
        }
        return billComponents;
    }
    private List<BillFee> billFees;

    public List<BillFee> getBillFees() {
        if (getCurrent() != null) {
            if (billFees == null) {
                String sql = "SELECT b FROM BillFee b WHERE b.retired=false and b.bill.id=" + getCurrent().getId();
                billFees = getBillFeeFacade().findByJpql(sql);
                if (billFees == null) {
                    billFees = new ArrayList<>();
                }
            }
        }

        return billFees;
    }

    public void recreateModel() {

        billFees = null;
        billItems = null;
        printPreview = false;
        billItems = null;
        items = null;
        dueBillFees = null;
        payingBillFees = null;
        billFees = null;
        /////////////////////    
        fromDate = null;
        toDate = null;
        current = null;
        currentStaff = null;
        totalDue = 0.0;
        totalPaying = 0.0;
        printPreview = false;
        paymentMethod = null;
        speciality = null;

    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
        currentStaff = null;
        dueBillFees = new ArrayList<BillFee>();
        payingBillFees = new ArrayList<BillFee>();
        totalPaying = 0.0;
        totalDue = 0.0;

    }

    @Deprecated
    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<Staff>();
        } else {
            if (speciality != null) {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality.id = " + getSpeciality().getId() + " order by p.person.name";
            } else {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            }
            //   ////// // System.out.println(sql);
            suggestions = getStaffFacade().findByJpql(sql);
        }
        return suggestions;
    }

    @Deprecated
    public List<Staff> completeReferringDoctor(String query) {
        List<Staff> suggestions;
        String sql;
        Map m = new HashMap();
        m.put("rd", getReferringDoctorSpeciality());
        if (query == null) {
            suggestions = new ArrayList<Staff>();
        } else {
            if (getReferringDoctorSpeciality() != null) {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) and p.speciality=:rd order by p.person.name";
                suggestions = getStaffFacade().findByJpql(sql, m);
            } else {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
                suggestions = getStaffFacade().findByJpql(sql);
            }
        }
        return suggestions;
    }

    public BillNumberGenerator getBillNumberBean() {
        return billNumberBean;
    }

    public void setBillNumberBean(BillNumberGenerator billNumberBean) {
        this.billNumberBean = billNumberBean;
    }

    public double getTotalDue() {
        return totalDue;
    }

    public void setTotalDue(double totalDue) {
        this.totalDue = totalDue;
    }

    public double getTotalPaying() {
        return totalPaying;
    }

    public void setTotalPaying(double totalPaying) {
        this.totalPaying = totalPaying;
    }

    public void calculateDueFees() {

        dueBillFees = new ArrayList<>();
        payingBillFees = new ArrayList<>();
        totalPaying = 0.0;
        totalDue = 0.0;
        printPreview = false;

        String sql;
        HashMap h = new HashMap();
        sql = "select b from BillFee b where "
                + " b.retired=false "
                + " and (b.bill.billType=:btp"
                + " or b.bill.billType=:btp2 )"
                + " and b.bill.cancelled=false "
                //                + " and b.bill.refunded=false "
                + " and (b.feeValue - b.paidValue) > 0 "
                + " and b.staff=:stf ";
//            h.put("btp", BillType.ChannelPaid);
        h.put("stf", currentStaff);
        h.put("btp", BillType.InwardBill);
        h.put("btp2", BillType.InwardProfessional);
        dueBillFees = getBillFeeFacade().findByJpql(sql, h, TemporalType.TIMESTAMP);
        List<BillFee> removeingBillFees = new ArrayList<>();
        for (BillFee bf : dueBillFees) {
            h = new HashMap();
            h.put("btp", BillType.InwardBill);
            sql = "SELECT bi FROM BillItem bi where bi.retired=false "
                    + " and bi.bill.cancelled=false "
                    + " and bi.bill.billType=:btp "
                    //                    + " and bi.bill.toStaff=:stf "
                    + " and bi.referanceBillItem.id=" + bf.getBillItem().getId();
            BillItem rbi = getBillItemFacade().findFirstByJpql(sql, h);

            if (rbi != null) {
                removeingBillFees.add(bf);
            }

        }
        dueBillFees.removeAll(removeingBillFees);
        calculateTotalPay();
    }

    public void calculateTotalDue() {
        totalDue = 0;
        for (BillFee f : dueBillFees) {
            totalDue = totalDue + f.getFeeValue() - f.getPaidValue();
        }
    }

    public void performCalculations() {
        calculateTotalDue();
        calculateTotalPay();
    }

    public void calculateTotalPay() {
        System.out.println("calculateTotalPay");
        totalPaying = 0;
        for (BillFee f : payingBillFees) {
            totalPaying = totalPaying + (f.getFeeValue() - f.getPaidValue());
        }
    }

    public BillFeeFacade getBillFeeFacade() {
        return billFeeFacade;
    }

    public void setBillFeeFacade(BillFeeFacade billFeeFacade) {
        this.billFeeFacade = billFeeFacade;
    }

    public List<BillFee> getDueBillFees() {
        return dueBillFees;
    }

    public void setDueBillFees(List<BillFee> dueBillFees) {
        this.dueBillFees = dueBillFees;
    }

    public List<BillFee> getPayingBillFees() {
        return payingBillFees;
    }

    public void setPayingBillFees(List<BillFee> payingBillFees) {
        this.payingBillFees = payingBillFees;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public Staff getCurrentStaff() {
        return currentStaff;

    }

    public void setCurrentStaff(Staff currentStaff) {
        this.currentStaff = currentStaff;
    }

    public void prepareAdd() {
        current = new BilledBill();
    }

    private Bill createPaymentBill() {
        BilledBill tmp = new BilledBill();
        tmp.setBillDate(Calendar.getInstance().getTime());
        tmp.setBillTime(Calendar.getInstance().getTime());
        tmp.setBillType(BillType.PaymentBill);
        tmp.setBillTypeAtomic(BillTypeAtomic.PROFESSIONAL_PAYMENT_FOR_STAFF_FOR_INWARD_SERVICE);
        tmp.setCreatedAt(Calendar.getInstance().getTime());
        tmp.setCreater(getSessionController().getLoggedUser());
        tmp.setDepartment(getSessionController().getLoggedUser().getDepartment());

        tmp.setDeptId(getBillNumberBean().departmentBillNumberGenerator(getSessionController().getDepartment(), BillType.PaymentBill, BillClassType.BilledBill, BillNumberSuffix.PROPAY));
        tmp.setInsId(getBillNumberBean().institutionBillNumberGenerator(getSessionController().getInstitution(), BillType.PaymentBill, BillClassType.BilledBill, BillNumberSuffix.PROPAY));

        tmp.setDiscount(0.0);
        tmp.setDiscountPercent(0.0);

        tmp.setInstitution(getSessionController().getLoggedUser().getInstitution());
        tmp.setNetTotal(0 - totalPaying);
        tmp.setPaymentMethod(paymentMethod);
        tmp.setStaff(currentStaff);
        tmp.setToStaff(currentStaff);
        tmp.setTotal(0 - totalPaying);

        return tmp;
    }

    private boolean errorCheck() {
        if (currentStaff == null) {
            JsfUtil.addErrorMessage("Please select a Staff Memeber");
            return true;
        }
//        performCalculations();
        if (totalPaying == 0) {
            JsfUtil.addErrorMessage("Please select payments to update");
            return true;
        }
        if (paymentMethod == null) {
            JsfUtil.addErrorMessage("Please select a payment method");
            return true;
        }

        return false;
    }

    @EJB
    private CashTransactionBean cashTransactionBean;

    public void settleBill() {
        if (errorCheck()) {
            return;
        }

//        calculateTotalPay();
        Bill b = createPaymentBill();
        current = b;

        if (b.getId() == null) {
            getBillFacade().create(b);
        }

        saveBillCompo(b);
        getBillFacade().edit(b);
        printPreview = true;

        WebUser wb = getCashTransactionBean().saveBillCashOutTransaction(b, getSessionController().getLoggedUser());
        getSessionController().setLoggedUser(wb);
        JsfUtil.addSuccessMessage("Successfully Paid");
        //   ////// // System.out.println("Paid");
    }

    private void saveBillCompo(Bill b) {
        for (BillFee bf : getPayingBillFees()) {
            saveBillItemForPaymentBill(b, bf);
//            saveBillFeeForPaymentBill(b,bf); No need to add fees for this bill

            bf.setPaidValue(bf.getFeeValue());
            getBillFeeFacade().edit(bf);
            //   ////// // System.out.println("marking as paid");
            b.getBillFees().add(bf);
        }
    }

    private void saveBillItemForPaymentBill(Bill b, BillFee bf) {
        BillItem i = new BillItem();
        i.setReferanceBillItem(bf.getBillItem());
        i.setReferenceBill(bf.getBill());
//        System.err.println("SS : " + bf.getPatienEncounter().getName());
//        System.err.println("SS : " + bf.getPatienEncounter().getDateTime());
//        System.err.println("SS : " + bf.getPatienEncounter().getFromTime());
//        System.err.println("SS : " + bf.getPatienEncounter().getToTime());
//        System.err.println("SS : " + bf.getPatienEncounter().getId());
        i.setPaidForBillFee(bf);
        i.setBill(b);
        i.setCreatedAt(Calendar.getInstance().getTime());
        i.setCreater(getSessionController().getLoggedUser());
        i.setDiscount(0.0);
        if (bf.getFeeGrossValue() != null) {
            i.setGrossValue(bf.getFeeGrossValue());
        }
//        if (bf.getBillItem() != null && bf.getBillItem().getItem() != null) {
//            i.setItem(bf.getBillItem().getItem());
//        }
        i.setNetValue(bf.getFeeValue());
        i.setQty(1.0);
        i.setRate(bf.getFeeValue());
        if (i.getId() == null) {
            getBillItemFacade().create(i);
        }
        b.getBillItems().add(i);
    }

    public BillFacade getEjbFacade() {
        return billFacade;
    }

    public void setEjbFacade(BillFacade ejbFacade) {
        this.billFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public InwardStaffPaymentBillController() {
    }

    public Bill getCurrent() {
        if (current == null) {
            current = new BilledBill();
        }
        return current;
    }

    public void setCurrent(Bill current) {
        currentStaff = null;
        dueBillFees = new ArrayList<BillFee>();
        payingBillFees = new ArrayList<BillFee>();
        totalPaying = 0.0;
        totalDue = 0.0;
        recreateModel();
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(sessionController.getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private BillFacade getFacade() {
        return billFacade;
    }

    public List<Bill> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public Boolean getPrintPreview() {
        return printPreview;
    }

    public void setPrintPreview(Boolean printPreview) {
        this.printPreview = printPreview;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = getCommonFunctions().getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
        //  resetLists();
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = getCommonFunctions().getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
        //  resetLists();
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public List<BillItem> getBillItems() {
        if (getCurrent() != null) {
            String sql = "SELECT b FROM BillItem b WHERE b.retired=false and b.bill.id=" + getCurrent().getId();
            billItems = getBillItemFacade().findByJpql(sql);
            if (billItems == null) {
                billItems = new ArrayList<BillItem>();
            }
        }

        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public CancelledBillFacade getCancelledBillFacade() {
        return cancelledBillFacade;
    }

    public void setCancelledBillFacade(CancelledBillFacade cancelledBillFacade) {
        this.cancelledBillFacade = cancelledBillFacade;
    }

    public BillComponentFacade getBillComponentFacade() {
        return billComponentFacade;
    }

    public void setBillComponentFacade(BillComponentFacade billComponentFacade) {
        this.billComponentFacade = billComponentFacade;
    }

    public RefundBillFacade getRefundBillFacade() {
        return refundBillFacade;
    }

    public void setRefundBillFacade(RefundBillFacade refundBillFacade) {
        this.refundBillFacade = refundBillFacade;
    }

    public void setBillComponents(List<BillComponent> billComponents) {
        this.billComponents = billComponents;
    }

    public void setBillFees(List<BillFee> billFees) {
        this.billFees = billFees;
    }

    public void setItems(List<Bill> items) {
        this.items = items;
    }

    public Speciality getReferringDoctorSpeciality() {
        return referringDoctorSpeciality;
    }

    public void setReferringDoctorSpeciality(Speciality referringDoctorSpeciality) {
        this.referringDoctorSpeciality = referringDoctorSpeciality;
    }

    public List<BillFee> getDocPayingBillFee() {
        return docPayingBillFee;
    }

    public void setDocPayingBillFee(List<BillFee> docPayingBillFee) {
        this.docPayingBillFee = docPayingBillFee;
    }

    public CashTransactionBean getCashTransactionBean() {
        return cashTransactionBean;
    }

    public void setCashTransactionBean(CashTransactionBean cashTransactionBean) {
        this.cashTransactionBean = cashTransactionBean;
    }

    public List<String1Value1> getList() {
        return list;
    }

    public void setList(List<String1Value1> list) {
        this.list = list;
    }

    public List<String2Value1> getList1() {
        return list1;
    }

    public void setList1(List<String2Value1> list1) {
        this.list1 = list1;
    }

    public List<Speciality> getSelectedItems() {
        return selectedItems;
    }

    public void setSelectedItems(List<Speciality> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public List<BillItem> getBillItems1() {
        return billItems1;
    }

    public void setBillItems1(List<BillItem> billItems1) {
        this.billItems1 = billItems1;
    }

    public List<Bill> getBills() {
        return bills;
    }

    public void setBills(List<Bill> bills) {
        this.bills = bills;
    }

    public List<Bill> getBillsCan() {
        return billsCan;
    }

    public void setBillsCan(List<Bill> billsCan) {
        this.billsCan = billsCan;
    }

    public double getTotalPayingCan() {
        return totalPayingCan;
    }

    public void setTotalPayingCan(double totalPayingCan) {
        this.totalPayingCan = totalPayingCan;
    }

    public List<String2Value1> getDocPayListDischarged() {
        return docPayListDischarged;
    }

    public void setDocPayListDischarged(List<String2Value1> docPayListDischarged) {
        this.docPayListDischarged = docPayListDischarged;
    }

    public List<String2Value1> getDocPayListNotDischarged() {
        return docPayListNotDischarged;
    }

    public void setDocPayListNotDischarged(List<String2Value1> docPayListNotDischarged) {
        this.docPayListNotDischarged = docPayListNotDischarged;
    }

    public List<BillFee> getBhtBillItemList() {
        return bhtBillItemList;
    }

    public void setBhtBillItemList(List<BillFee> bhtBillItemList) {
        this.bhtBillItemList = bhtBillItemList;
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

    public double getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(double totalPaid) {
        this.totalPaid = totalPaid;
    }

    public double getTotalVal() {
        return totalVal;
    }

    public void setTotalVal(double totalVal) {
        this.totalVal = totalVal;
    }

    public List<BillFee> getBhtDueList() {
        return bhtDueList;
    }

    public void setBhtDueList(List<BillFee> bhtDueList) {
        this.bhtDueList = bhtDueList;
    }

    public List<BillFee> getBillFeeDueDischarged() {
        return billFeeDueDischarged;
    }

    public void setBillFeeDueDischarged(List<BillFee> billFeeDueDischarged) {
        this.billFeeDueDischarged = billFeeDueDischarged;
    }

    public List<BillFee> getBillFeeDueNotDischarged() {
        return billFeeDueNotDischarged;
    }

    public void setBillFeeDueNotDischarged(List<BillFee> billFeeDueNotDischarged) {
        this.billFeeDueNotDischarged = billFeeDueNotDischarged;
    }

    public List<BillItem> getDocPayDischarged() {
        Date startTime = new Date();
        
        return docPayDischarged;

    }

    public void setDocPayDischarged(List<BillItem> docPayDischarged) {
        this.docPayDischarged = docPayDischarged;
    }

    public List<BillItem> getDocPayNotDischarged() {
        return docPayNotDischarged;
    }

    public void setDocPayNotDischarged(List<BillItem> docPayNotDischarged) {
        this.docPayNotDischarged = docPayNotDischarged;
    }

    public List<BillItem> getDocFeePayDischarged() {
        if (docFeePayDischarged == null) {
            docFeePayDischarged = new ArrayList<>();
        }
        return docFeePayDischarged;
    }

    public void setDocFeePayDischarged(List<BillItem> docFeePayDischarged) {
        this.docFeePayDischarged = docFeePayDischarged;
    }

    public List<BillItem> getDocFeePayNotDischarged() {
        if (docFeePayNotDischarged == null) {
            docFeePayNotDischarged = new ArrayList<>();
        }
        return docFeePayNotDischarged;
    }

    public void setDocFeePayNotDischarged(List<BillItem> docFeePayNotDischarged) {
        this.docFeePayNotDischarged = docFeePayNotDischarged;
    }

    public List<BillFee> getDocFeeDueDischarged() {
        if (docFeeDueDischarged == null) {
            docFeeDueDischarged = new ArrayList<>();
        }
        return docFeeDueDischarged;
    }

    public void setDocFeeDueDischarged(List<BillFee> docFeeDueDischarged) {
        this.docFeeDueDischarged = docFeeDueDischarged;
    }

    public List<BillFee> getDocFeeDueNotDischarged() {
        if (docFeeDueNotDischarged == null) {
            docFeeDueNotDischarged = new ArrayList<>();
        }
        return docFeeDueNotDischarged;
    }

    public void setDocFeeDueNotDischarged(List<BillFee> docFeeDueNotDischarged) {
        this.docFeeDueNotDischarged = docFeeDueNotDischarged;
    }

    public double getTotalDocFeePayDischarged() {
        return totalDocFeePayDischarged;
    }

    public void setTotalDocFeePayDischarged(double totalDocFeePayDischarged) {
        this.totalDocFeePayDischarged = totalDocFeePayDischarged;
    }

    public double getTotalDocFeePayNotDischarged() {
        return totalDocFeePayNotDischarged;
    }

    public void setTotalDocFeePayNotDischarged(double totalDocFeePayNotDischarged) {
        this.totalDocFeePayNotDischarged = totalDocFeePayNotDischarged;
    }

    public double getTotalDocFeeDueDischarged() {
        return totalDocFeeDueDischarged;
    }

    public void setTotalDocFeeDueDischarged(double totalDocFeeDueDischarged) {
        this.totalDocFeeDueDischarged = totalDocFeeDueDischarged;
    }

    public double getTotalDocFeeDueNotDischarged() {
        return totalDocFeeDueNotDischarged;
    }

    public void setTotalDocFeeDueNotDischarged(double totalDocFeeDueNotDischarged) {
        this.totalDocFeeDueNotDischarged = totalDocFeeDueNotDischarged;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

}
