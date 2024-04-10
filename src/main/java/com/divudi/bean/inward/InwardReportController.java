/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;

import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.data.inward.InwardChargeType;

import com.divudi.entity.Bill;
import com.divudi.entity.BillItem;
import com.divudi.entity.BilledBill;
import com.divudi.entity.CancelledBill;
import com.divudi.entity.Category;
import com.divudi.entity.Institution;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.RefundBill;
import com.divudi.entity.inward.Admission;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.entity.lab.PatientInvestigation;
import com.divudi.facade.AdmissionTypeFacade;
import com.divudi.facade.BillFacade;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.PatientEncounterFacade;
import com.divudi.facade.PatientInvestigationFacade;
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
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 *
 * @author pdhs
 */
@Named
@SessionScoped
public class InwardReportController implements Serializable {

    /**
     * Creates a new instance of InwardReportController
     */
    public InwardReportController() {
    }

    PaymentMethod paymentMethod;
    AdmissionType admissionType;
    Institution institution;
    Date fromDate;
    Date toDate;
    Admission patientEncounter;
    double grossTotals;
    double discounts;
    double netTotals;
    boolean withFooter;
    String invoceNo;
    String vatRegNo;

    List<IncomeByCategoryRecord> incomeByCategoryRecords;
    List<IndividualBhtIncomeByCategoryRecord> individualBhtIncomeByCategoryRecord;

    List<AdmissionType> admissionty;

    //////////////
    @Inject
    CommonController commonController;

    @EJB
    PatientEncounterFacade peFacade;
    @EJB
    AdmissionTypeFacade admissionTypeFacade;
    @EJB
    PatientInvestigationFacade patientInvestigationFacade;
    List<PatientEncounter> patientEncounters;

    Bill bill;
    List<BillItem> billedBill;
    List<BillItem> cancelledBill;
    List<BillItem> refundBill;
    List<PatientInvestigation> patientInvestigations;
    double totalBilledBill;
    double totalCancelledBill;
    double totalRefundBill;
    @Inject
    SessionController sessionController;
    List<BillItem> billItems;
    @EJB
    BillFacade billFacade;
    @EJB
    BillItemFacade billItemFacade;
    // for disscharge book
    boolean dischargeDate = true;
    boolean bhtNo = true;
    boolean paymentMethord = true;
    boolean creditCompany = true;
    boolean person = true;
    boolean guardian = true;
    boolean room = true;
    boolean refDoctor = true;
    boolean AddmitDetails = true;
    boolean billedBy = true;
    boolean finalBillTotal = true;
    boolean paidByPatient = true;
    boolean creditPaidAmount = true;
    boolean dueAmount = true;
    boolean calculatedAmount = true;
    boolean differentAmount = true;
    boolean developers = false;
    // for disscharge book
    boolean withoutCancelBHT = true;

    private ReportKeyWord reportKeyWord;

    public List<PatientEncounter> getPatientEncounters() {
        return patientEncounters;
    }

    public void setPatientEncounters(List<PatientEncounter> patientEncounters) {
        this.patientEncounters = patientEncounters;
    }
    double netTotal;
    double netPaid;

    public void fillAdmissionBook() {
        Date startTime = new Date();

        fillAdmissions(null, null);

        
    }

    public void fillAdmissionBookNew() {
        Date startTime = new Date();
        if (getReportKeyWord().getString().isEmpty() || getReportKeyWord().getString() == null) {
            JsfUtil.addErrorMessage("Select a Selection Methord");
            return;
        }
        if (getReportKeyWord().getString().equals("0")) {
            fillAdmissions(null, null);
        } else if (getReportKeyWord().getString().equals("1")) {
            fillAdmissions(false, false);
        } else if (getReportKeyWord().getString().equals("2")) {
            fillAdmissions(true, false);
        } else if (getReportKeyWord().getString().equals("3")) {
            fillAdmissions(true, true);
        }
        
    }

    public void fillAdmissionBookOnlyInward() {
        Date startTime = new Date();

        fillAdmissions(false, null);

        
    }

    public void fillAdmissionBookOnlyDischarged() {
        Date startTime = new Date();
        fillAdmissions(true, null);

        
    }

    public void fillAdmissionBookOnlyDischargedNotFinalized() {
        Date startTime = new Date();
        fillAdmissions(true, false);

        

    }

    public void fillAdmissionBookOnlyDischargedFinalized() {
        Date startTime = new Date();
        fillAdmissions(true, true);

        
    }

    public void fillAdmissions(Boolean discharged, Boolean finalized) {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.dateOfAdmission between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad";
            m.put("ad", admissionType);
        }

        if (withoutCancelBHT) {
            sql += " and b.retired=false ";
        }
        //// // System.out.println("discharged = " + discharged);
        if (discharged != null) {
            if (discharged) {
                sql += " and b.discharged=true ";
            } else {
                sql += " and b.discharged=false ";
            }
        }
        if (finalized != null) {
            if (finalized) {
                sql += " and b.paymentFinalized=true ";
            } else {
                sql += " and b.paymentFinalized=false ";
            }
        }

        sql += " order by b.dateOfAdmission,b.bhtNo ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
//        calTtoal();
        calTtoal(patientEncounters);
    }

    @Inject
    BhtSummeryController bhtSummeryController;

    private void calTtoal() {
        if (patientEncounters == null) {
            return;
        }

        netTotal = 0;
        netPaid = 0;
        for (PatientEncounter p : patientEncounters) {
            bhtSummeryController.setPatientEncounter((Admission) p);
            bhtSummeryController.createTables();
            p.setTransTotal(bhtSummeryController.getGrantTotal());
            p.setTransPaid(bhtSummeryController.getPaid());

            netTotal += p.getTransTotal();
            netPaid += p.getTransPaid();
        }
    }

    private void calTtoal(List<PatientEncounter> patientEncounters) {
        if (patientEncounters == null) {
            return;
        }
        netTotal = 0;
        netPaid = 0;
        for (PatientEncounter p : patientEncounters) {
            if (p.getFinalBill() != null) {
                netTotal += p.getFinalBill().getNetTotal();
                netPaid += p.getPaidByCreditCompany() + p.getFinalBill().getPaidAmount();
            }
        }
    }

    public void fillAdmissionBookOnlyInwardDeleted() {
        Date startTime = new Date();

        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=true "
                + " and b.dateOfAdmission between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad";
            m.put("ad", admissionType);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        
    }

    double total;
    double paid;
    double creditPaid;
    double creditUsed;
    double calTotal;
    double totalVat;
    double totalVatCalculatedValue;

    @Inject
    InwardReportControllerBht inwardReportControllerBht;

    public void fillDischargeBook() {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                //                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        if (reportKeyWord.getStaff() != null) {
            sql += " and b.referringDoctor =:refDoc ";
            m.put("refDoc", reportKeyWord.getStaff());
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);
        calTotalDischarged();
    }

    public void calTotalDischarged() {
        if (patientEncounters == null) {
            return;
        }
        setTotal(0);
        paid = 0;
        calTotal = 0;
        creditPaid = 0;
        creditUsed = 0;
        for (PatientEncounter p : patientEncounters) {
            inwardReportControllerBht.setPatientEncounter(p);
            inwardReportControllerBht.process();
            p.setTransTotal(inwardReportControllerBht.getNetTotal());

            if (p.getFinalBill() != null) {
                setTotal(getTotal() + p.getFinalBill().getNetTotal());
                paid += p.getFinalBill().getPaidAmount();
            }

            creditUsed += p.getCreditUsedAmount();
            creditPaid += p.getPaidByCreditCompany();
            calTotal += p.getTransTotal();
        }
    }

    public void calTotalDischargedNoChanges() {
        if (patientEncounters == null) {
            return;
        }

        total = 0.0;
        totalVat = 0.0;
        totalVatCalculatedValue = 0.0;
        paid = 0;
        calTotal = 0;
        creditPaid = 0;
        creditUsed = 0;
        for (PatientEncounter p : patientEncounters) {
            p.setTransPaidByPatient(calPaidByPatient(p));
            p.setTransPaidByCompany(calPaidByCompany(p));
            if (p.getFinalBill()==null) {
                continue;
            }
            for (BillItem bi : p.getFinalBill().getBillItems()) {
                if (bi.getInwardChargeType() == InwardChargeType.VAT) {
                    p.getFinalBill().setVat(bi.getNetValue() + p.getFinalBill().getVat());
                }
                if (bi.getInwardChargeType() != InwardChargeType.VAT && bi.getInwardChargeType() != InwardChargeType.Medicine) {
                    p.getFinalBill().setVatCalulatedAmount(bi.getNetValue() + p.getFinalBill().getVatCalulatedAmount());
                }
            }

            total += p.getFinalBill().getNetTotal();
            totalVat += p.getFinalBill().getVat();
            totalVatCalculatedValue += p.getFinalBill().getVatCalulatedAmount();
            paid += p.getTransPaidByPatient();
            creditPaid += p.getTransPaidByCompany();
        }
    }

    private double calPaidByPatient(PatientEncounter patientEncounter) {
        Map m = new HashMap();
        String sql = "select sum(b.netTotal) from Bill b "
                + " where b.patientEncounter=:pe"
                + " and b.billType=:btp "
                + " and b.createdAt <= :td ";

        m.put("btp", BillType.InwardPaymentBill);
        m.put("td", toDate);
        m.put("pe", patientEncounter);
        return getPeFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    private double calPaidByCompany(PatientEncounter patientEncounter) {
        Map m = new HashMap();
        String sql = "select sum(b.netValue) "
                + "  from BillItem b "
                + " where b.patientEncounter=:pe"
                + " and b.bill.billType=:btp "
                + " and b.bill.createdAt <= :td ";

        m.put("btp", BillType.CashRecieveBill);
        m.put("td", toDate);
        m.put("pe", patientEncounter);
        return getPeFacade().findDoubleByJpql(sql, m, TemporalType.TIMESTAMP);
    }

    public void fillDischargeBookPaymentNotFinalized() {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentFinalized=false "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        if (reportKeyWord.getStaff() != null) {
            sql += " and b.referringDoctor =:refDoc ";
            m.put("refDoc", reportKeyWord.getStaff());
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calTotalDischarged();
    }

    public void fillDischargeBookPaymentFinalized() {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        if (reportKeyWord.getStaff() != null) {
            sql += " and b.referringDoctor =:refDoc ";
            m.put("refDoc", reportKeyWord.getStaff());
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calTotalDischarged();
    }

    public void fillDischargeBookPaymentFinalizedNoChanges() {
        Date startTime = new Date();

        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                //                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calTotalDischargedNoChanges();

        
    }

    public void fillDischargeBookPaymentFinalizedNoChangesOnlyDue() {
        Date startTime = new Date();

        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentFinalized=true "
                + " and b.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad ";
            m.put("ad", admissionType);
        }

        if (institution != null) {
            sql += " and b.creditCompany =:ins ";
            m.put("ins", institution);
        }

        if (paymentMethod != null) {
            sql += " and b.paymentMethod =:pm ";
            m.put("pm", paymentMethod);
        }

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findByJpql(sql, m, TemporalType.TIMESTAMP);

        calTotalDischargedNoChanges();

        List<PatientEncounter> list = patientEncounters;
        patientEncounters = null;
        patientEncounters = new ArrayList<>();
        setTotal(0);
        paid = 0;
        calTotal = 0;
        creditPaid = 0;
        creditUsed = 0;
        for (PatientEncounter p : list) {
            if (p.getFinalBill()==null) {
                continue;
            }
            p.setTransPaidByPatient(calPaidByPatient(p));
            p.setTransPaidByCompany(calPaidByCompany(p));

            double paidValue = p.getTransPaidByPatient() + p.getTransPaidByCompany();
            double dueValue = p.getFinalBill().getNetTotal() - paidValue;

            if (Math.round(dueValue) != 0) {
                setTotal(getTotal() + p.getFinalBill().getNetTotal());
                paid += p.getTransPaidByPatient();
                creditPaid += p.getTransPaidByCompany();

                patientEncounters.add(p);
            }

        }

        

    }

    public void makeListNull() {
        billItems = null;
    }

    public void updateOutSideBill(BillItem bi) {
        if (bi.getBill().isPaid()) {
            if (bi.getDescreption() == null || bi.getDescreption().equals("")) {
                JsfUtil.addErrorMessage("Please Enter Memo");
                return;
            }
            if (bi.getBill().getEditedAt() == null && bi.getBill().getEditor() == null) {
                bi.getBill().setEditor(getSessionController().getLoggedUser());
                bi.getBill().setEditedAt(new Date());
                getBillFacade().edit(bi.getBill());
                getBillItemFacade().edit(bi);
                JsfUtil.addSuccessMessage("This Bill Mark as Paid");
            } else {
                JsfUtil.addErrorMessage("Alreddy Mark as Paid");
            }
        } else {
            bi.getBill().setEditor(null);
            bi.getBill().setEditedAt(null);
            getBillFacade().edit(bi.getBill());
            bi.setDescreption("");
            getBillItemFacade().edit(bi);
            JsfUtil.addSuccessMessage("This Bill Mark as Un Paid");
        }
    }

    public void createOutSideBills() {
        Date startTime = new Date();

        makeListNull();
        String sql;
        Map temMap = new HashMap();
        sql = "select b from BillItem b"
                + " where b.bill.billType = :billType "
                + " and b.retired=false "
                + " and b.bill.retired=false ";

        if (reportKeyWord.getString().equals("0")) {
            sql += " and b.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate ";
            temMap.put("toDate", toDate);
            temMap.put("fromDate", fromDate);
        }

        if (reportKeyWord.getString().equals("1")) {
            sql += " and b.bill.createdAt between :fromDate and :toDate ";
            temMap.put("toDate", toDate);
            temMap.put("fromDate", fromDate);
        }

        if (reportKeyWord.getString1().equals("0")) {
            sql += " and b.bill.paid!=true ";
        }

        if (reportKeyWord.getString1().equals("1")) {
            sql += " and b.bill.paid=true ";
        }

        if (institution != null) {
            sql += " and b.bill.fromInstitution=:ins ";
            temMap.put("ins", institution);
        }

        temMap.put("billType", BillType.InwardOutSideBill);

        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (billItems == null) {
            billItems = new ArrayList<>();
        }

        total = 0.0;
        for (BillItem b : billItems) {
            total += b.getBill().getNetTotal();
        }

        

    }

//    public void createOutSideBillsByAddedDate() {
//        Date startTime = new Date();
//
//        makeListNull();
//        String sql;
//        Map temMap = new HashMap();
//        sql = "select b from BillItem b"
//                + " where b.bill.billType = :billType "
//                + " and b.bill.createdAt between :fromDate and :toDate "
//                + " and b.retired=false "
//                + " and b.bill.retired=false ";
//
//        if (institution != null) {
//            sql += " and b.bill.fromInstitution=:ins ";
//            temMap.put("ins", institution);
//        }
//
//        temMap.put("billType", BillType.InwardOutSideBill);
//        temMap.put("toDate", toDate);
//        temMap.put("fromDate", fromDate);
//
//        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//        if (billItems == null) {
//            billItems = new ArrayList<>();
//
//        }
//
//        setTotal(0.0);
//        for (BillItem b : billItems) {
//            setTotal(getTotal() + b.getBill().getNetTotal());
//        }
//
//        
//
//    }
//
//    public void createOutSideBillsByDischargeDate() {
//
//        Date startTime = new Date();
//
//        makeListNull();
//        String sql;
//        Map temMap = new HashMap();
//        sql = "select b from BillItem b"
//                + " where b.bill.billType = :billType "
//                + " and b.bill.patientEncounter.dateOfDischarge between :fromDate and :toDate "
//                + " and b.retired=false "
//                + " and b.bill.retired=false ";
//
//        if (institution != null) {
//            sql += " and b.bill.fromInstitution=:ins ";
//            temMap.put("ins", institution);
//        }
//
//        temMap.put("billType", BillType.InwardOutSideBill);
//        temMap.put("toDate", toDate);
//        temMap.put("fromDate", fromDate);
//
//        billItems = getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
//
//        if (billItems == null) {
//            billItems = new ArrayList<>();
//
//        }
//
//        setTotal(0.0);
//        for (BillItem b : billItems) {
//            setTotal(getTotal() + b.getBill().getNetTotal());
//        }
//
//        
//
//    }
    public void createPatientInvestigationsTableAll() {
        Date startTime = new Date();

        String sql = "select pi from PatientInvestigation pi join pi.investigation  "
                + " i join pi.billItem.bill b join b.patient.person p where "
                + " b.createdAt between :fromDate and :toDate  "
                + "and pi.encounter is not null ";

        Map temMap = new HashMap();

        if (patientEncounter != null) {
            sql += "and pi.encounter=:en";
            temMap.put("en", patientEncounter);
        }
//       

        sql += " order by pi.id desc  ";
//    

        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());

        //System.err.println("Sql " + sql);
        patientInvestigations = getPatientInvestigationFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        

    }

    public Admission getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(Admission patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public PatientInvestigationFacade getPatientInvestigationFacade() {
        return patientInvestigationFacade;
    }

    public void setPatientInvestigationFacade(PatientInvestigationFacade patientInvestigationFacade) {
        this.patientInvestigationFacade = patientInvestigationFacade;
    }

    public List<PatientInvestigation> getPatientInvestigations() {
        return patientInvestigations;
    }

    public void setPatientInvestigations(List<PatientInvestigation> patientInvestigations) {
        this.patientInvestigations = patientInvestigations;
    }

    public BhtSummeryController getBhtSummeryController() {
        return bhtSummeryController;
    }

    public void setBhtSummeryController(BhtSummeryController bhtSummeryController) {
        this.bhtSummeryController = bhtSummeryController;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public double getPaid() {
        return paid;
    }

    public void setPaid(double paid) {
        this.paid = paid;
    }

    public List<IncomeByCategoryRecord> getIncomeByCategoryRecords() {
        return incomeByCategoryRecords;
    }

    public void setIncomeByCategoryRecords(List<IncomeByCategoryRecord> incomeByCategoryRecords) {
        this.incomeByCategoryRecords = incomeByCategoryRecords;
    }

    public void listBhtViceIncome() {
        Date startTime = new Date();

        String sql;
        individualBhtIncomeByCategoryRecord = new ArrayList<>();
        grossTotals = 0.0;
        netTotals = 0.0;
        discounts = 0.0;
        Map m = new HashMap();
        sql = "select pe, category,"
                + " bf.billItem.item.inwardChargeType, "
                + " sum(bf.feeGrossValue), sum(bf.feeDiscount),"
                + " sum(bf.feeValue) "
                + "from BillFee bf "
                + "join bf.billItem.item.category as category "
                + "join bf.bill.patientEncounter as pe "
                + "where "
                + "pe is not null and "
                + "bf.bill.billType=:billType and "
                + "pe.dateOfDischarge between :fd and :td ";
        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("billType", BillType.InwardBill);

        sql = sql + " group by pe.id, category.name, bf.billItem.item.inwardChargeType ";
        sql = sql + " order by pe.id, bf.billItem.item.inwardChargeType, category.name";

//        Item item;
//        item.getInwardChargeType()
        List<Object[]> results = getPeFacade().findAggregates(sql, m, TemporalType.DATE);

//        PatientEncounter pe = new PatientEncounter();
//        pe.getAdmissionType();
        if (results == null) {
            return;
        }

        for (Object[] objs : results) {
            IndividualBhtIncomeByCategoryRecord ibr = new IndividualBhtIncomeByCategoryRecord();
            PatientEncounter pe = (PatientEncounter) objs[0];
            Category cat = (Category) objs[1];
            InwardChargeType ict = (InwardChargeType) objs[2];
            ibr.setBht(pe);
            ibr.setFinalBill(pe.getFinalBill());
            ibr.setCategory(cat);
            ibr.setInwardChargeType(ict);
            ibr.setGrossValue((Double) objs[3]);
            ibr.setDiscount((Double) objs[4]);
            ibr.setNetValue((Double) objs[5]);

            grossTotals = grossTotals + ibr.getGrossValue();
            discounts = discounts + ibr.getDiscount();
            netTotals = netTotals + ibr.getNetValue();

            individualBhtIncomeByCategoryRecord.add(ibr);
        }

        
    }

    public void listDischargedBhtIncomeByCategories() {
        String sql;
        incomeByCategoryRecords = new ArrayList<>();
        grossTotals = 0.0;
        netTotals = 0.0;
        discounts = 0.0;
        Map m = new HashMap();
        sql = "select bf.billItem.item.category, "
                + " sum(bf.feeDiscount),"
                + " sum(bf.feeMargin),"
                + " sum(bf.feeGrossValue),"
                + " sum(bf.feeValue)"
                + " from BillFee bf where"
                + " bf.bill.patientEncounter is not null"
                + " and bf.bill.patientEncounter.discharged=true ";

        m.put("fd", fromDate);
        m.put("td", toDate);
        m.put("billType", BillType.InwardBill);
        sql = sql + " and bf.bill.billType=:billType and"
                + " bf.bill.patientEncounter.dateOfDischarge between :fd and :td ";

        if (admissionType != null) {
            sql = sql + " and bf.bill.patientEncounter.admissionType=:at ";
            m.put("at", admissionType);

        }

        if (paymentMethod != null) {
            sql = sql + " and bf.bill.patientEncounter.paymentMethod=:bt ";
            m.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and bf.bill.patientEncounter.creditCompany=:cc ";
            m.put("cc", institution);
        }

        sql = sql + " group by bf.billItem.item.category order by bf.billItem.item.category.name";
        List<Object[]> results = getPeFacade().findAggregates(sql, m, TemporalType.DATE);

//        PatientEncounter pe = new PatientEncounter();
//        pe.getAdmissionType();
        if (results == null) {
            return;
        }

        for (Object[] objs : results) {

            IncomeByCategoryRecord ibr = new IncomeByCategoryRecord();
            ibr.setCategory((Category) objs[0]);
            ibr.setDiscount((double) objs[1]);
            ibr.setMatrix((double) objs[2]);
            ibr.setGrossAmount((double) objs[3]);
            ibr.setNetAmount((double) objs[4]);

            grossTotals = grossTotals + ibr.getGrossAmount();
            discounts = discounts + ibr.getDiscount();
            netTotals = netTotals + ibr.getNetAmount();

            incomeByCategoryRecords.add(ibr);

        }

    }

    public void fillProfessionalPaymentDone() {
        Date startTime = new Date();

        billedBill = createBilledBillProfessionalPaymentTableInwardAll(new BilledBill());
        cancelledBill = createCancelBillRefundBillProfessionalPaymentTableInwardAll(new CancelledBill());
        refundBill = createCancelBillRefundBillProfessionalPaymentTableInwardAll(new RefundBill());

        totalBilledBill = calTotalCreateBilledBillProfessionalPaymentTableInwardAll(new BilledBill());
        totalCancelledBill = calTotalCreateCancelBillRefundBillProfessionalPaymentTableInwardAll(new CancelledBill());
        totalRefundBill = calTotalCreateCancelBillRefundBillProfessionalPaymentTableInwardAll(new RefundBill());

        
    }

    public void fillProfessionalPaymentDoneOPD() {
        Date startTime = new Date();

        BillType[] bts = {BillType.OpdBill};
        List<BillType> billTypes = Arrays.asList(bts);
        billedBill = createProfessionalPaymentTable(new BilledBill(), BillType.PaymentBill, billTypes);
        cancelledBill = createProfessionalPaymentTable(new CancelledBill(), BillType.PaymentBill, null);
        refundBill = createProfessionalPaymentTable(new RefundBill(), BillType.PaymentBill, null);

        totalBilledBill = createProfessionalPaymentTableTotals(new BilledBill(), BillType.PaymentBill, billTypes);
        totalCancelledBill = createProfessionalPaymentTableTotals(new CancelledBill(), BillType.PaymentBill, null);
        totalRefundBill = createProfessionalPaymentTableTotals(new RefundBill(), BillType.PaymentBill, null);

        
    }

    List<BillItem> createBilledBillProfessionalPaymentTableInwardAll(Bill bill) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                //                + " and b.bill.cancelled=false "
                + " and type(b.bill)=:bclass"
                + " and (b.referenceBill.billType=:refType "
                + " or b.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (admissionType != null) {
            sql = sql + " and (b.referenceBill.billType=:refType "
                    + " or b.referenceBill.billType=:refType2) ";
            temMap.put("at", admissionType);
        }

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            temMap.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            temMap.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            temMap.put("cc", institution);
        }


        return getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    List<BillItem> createCancelBillRefundBillProfessionalPaymentTableInwardAll(Bill bill) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
//        temMap.put("refType", BillType.InwardBill);
//        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.paidForBillFee.bill.patientEncounter is not null"
                + " and type(b.bill)=:bclass"
                //                + " and (b.bill.billedBill.referenceBill.billType=:refType "
                //                + " or b.bill.billedBill.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            temMap.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            temMap.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            temMap.put("cc", institution);
        }


        return getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    List<BillItem> createProfessionalPaymentTable(Bill bill, BillType bt, List<BillType> billTypes) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bt", bt);
        String sql = " Select b FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bt "
                + " and type(b.bill)=:bclass "
                + " and b.paidForBillFee.bill.patientEncounter is null "
                + " and b.createdAt between :fromDate and :toDate ";

        if (paymentMethod != null) {
            sql = sql + " and b.paidForBillFee.bill.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.paidForBillFee.bill.creditCompany=:cc ";
            temMap.put("cc", institution);
        }
        if (billTypes != null) {
            sql += " and b.referenceBill.billType in :bts ";
            temMap.put("bts", billTypes);
        }

        return getBillItemFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double createProfessionalPaymentTableTotals(Bill bill, BillType bt, List<BillType> billTypes) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bt", bt);
        String sql = " Select sum(b.netValue) FROM BillItem b  "
                + " where b.retired=false "
                + " and b.bill.billType=:bt "
                + " and type(b.bill)=:bclass"
                + " and b.paidForBillFee.bill.patientEncounter is null "
                + " and b.createdAt between :fromDate and :toDate ";

        if (paymentMethod != null) {
            sql = sql + " and b.paidForBillFee.bill.paymentMethod=:pm ";
            temMap.put("pm", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.paidForBillFee.bill.creditCompany=:cc ";
            temMap.put("cc", institution);
        }

        if (billTypes != null) {
            sql += " and b.referenceBill.billType in :bts ";
            temMap.put("bts", billTypes);
        }

        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double calTotalCreateBilledBillProfessionalPaymentTableInwardAll(Bill bill) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
        temMap.put("refType", BillType.InwardBill);
        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select sum(b.netValue) FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                //                + " and b.bill.cancelled=false "
                + " and type(b.bill)=:bclass"
                + " and (b.referenceBill.billType=:refType "
                + " or b.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            temMap.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            temMap.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            temMap.put("cc", institution);
        }

        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public double calTotalCreateCancelBillRefundBillProfessionalPaymentTableInwardAll(Bill bill) {
        billItems = null;
        HashMap temMap = new HashMap();
        temMap.put("bclass", bill.getClass());
        temMap.put("toDate", getToDate());
        temMap.put("fromDate", getFromDate());
        temMap.put("bType", BillType.PaymentBill);
//        temMap.put("refType", BillType.InwardBill);
//        temMap.put("refType2", BillType.InwardProfessional);
        String sql = "Select sum(b.netValue) FROM BillItem b "
                + " where b.retired=false "
                + " and b.bill.billType=:bType "
                + " and b.paidForBillFee.bill.patientEncounter is not null"
                + " and type(b.bill)=:bclass"
                //                + " and (b.bill.billedBill.referenceBill.billType=:refType "
                //                + " or b.bill.billedBill.referenceBill.billType=:refType2) "
                + " and b.createdAt between :fromDate and :toDate ";

        if (admissionType != null) {
            sql = sql + " and b.bill.patientEncounter.admissionType=:at ";
            temMap.put("at", admissionType);
        }

        if (paymentMethod != null) {
            sql = sql + " and b.bill.patientEncounter.paymentMethod=:bt ";
            temMap.put("bt", paymentMethod);
        }

        if (institution != null) {
            sql = sql + " and b.bill.patientEncounter.creditCompany=:cc ";
            temMap.put("cc", institution);
        }

        return getBillItemFacade().findDoubleByJpql(sql, temMap, TemporalType.TIMESTAMP);
    }

    public void listnerDeveloperCheckBox() {
        if (developers) {
            dischargeDate = false;
            bhtNo = false;
            paymentMethord = false;
            creditCompany = false;
            person = false;
            guardian = false;
            room = false;
            refDoctor = false;
            AddmitDetails = false;
            billedBy = false;
            finalBillTotal = false;
            paidByPatient = false;
            creditPaidAmount = false;
            dueAmount = false;
            calculatedAmount = false;
            differentAmount = false;
        } else {
            dischargeDate = true;
            bhtNo = true;
            paymentMethord = true;
            creditCompany = true;
            person = true;
            guardian = true;
            room = true;
            refDoctor = true;
            AddmitDetails = true;
            billedBy = true;
            finalBillTotal = true;
            paidByPatient = true;
            creditPaidAmount = true;
            dueAmount = true;
            calculatedAmount = true;
            differentAmount = true;
        }
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public AdmissionType getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(AdmissionType admissionType) {
        this.admissionType = admissionType;
    }

    public List<AdmissionType> getAdmissionty() {
        admissionty = getAdmissionTypeFacade().findAll("name", true);
        return admissionty;
    }

    public void setAdmissionty(List<AdmissionType> admissionty) {
        this.admissionty = admissionty;
    }

    public AdmissionTypeFacade getAdmissionTypeFacade() {
        return admissionTypeFacade;
    }

    public void setAdmissionTypeFacade(AdmissionTypeFacade admissionTypeFacade) {
        this.admissionTypeFacade = admissionTypeFacade;
    }


    CommonFunctions commonFunctions;

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = com.divudi.java.CommonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public List<BillItem> getBilledBill() {
        return billedBill;
    }

    public void setBilledBill(List<BillItem> billedBill) {
        this.billedBill = billedBill;
    }

    public List<BillItem> getCancelledBill() {
        return cancelledBill;
    }

    public void setCancelledBill(List<BillItem> cancelledBill) {
        this.cancelledBill = cancelledBill;
    }

    public List<BillItem> getRefundBill() {
        return refundBill;
    }

    public void setRefundBill(List<BillItem> refundBill) {
        this.refundBill = refundBill;
    }

    public double getTotalBilledBill() {
        return totalBilledBill;
    }

    public void setTotalBilledBill(double totalBilledBill) {
        this.totalBilledBill = totalBilledBill;
    }

    public double getTotalCancelledBill() {
        return totalCancelledBill;
    }

    public void setTotalCancelledBill(double totalCancelledBill) {
        this.totalCancelledBill = totalCancelledBill;
    }

    public double getTotalRefundBill() {
        return totalRefundBill;
    }

    public void setTotalRefundBill(double totalRefundBill) {
        this.totalRefundBill = totalRefundBill;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = com.divudi.java.CommonFunctions.getEndOfMonth(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PatientEncounterFacade getPeFacade() {
        return peFacade;
    }

    public void setPeFacade(PatientEncounterFacade peFacade) {
        this.peFacade = peFacade;
    }

    public double getGrossTotals() {
        return grossTotals;
    }

    public void setGrossTotals(double grossTotals) {
        this.grossTotals = grossTotals;
    }

    public double getDiscounts() {
        return discounts;
    }

    public void setDiscounts(double discounts) {
        this.discounts = discounts;
    }

    public double getNetTotals() {
        return netTotals;
    }

    public void setNetTotals(double netTotals) {
        this.netTotals = netTotals;
    }

    public List<IndividualBhtIncomeByCategoryRecord> getIndividualBhtIncomeByCategoryRecord() {
        return individualBhtIncomeByCategoryRecord;
    }

    public void setIndividualBhtIncomeByCategoryRecord(List<IndividualBhtIncomeByCategoryRecord> individualBhtIncomeByCategoryRecord) {
        this.individualBhtIncomeByCategoryRecord = individualBhtIncomeByCategoryRecord;
    }

    public boolean isWithFooter() {
        return withFooter;
    }

    public void setWithFooter(boolean withFooter) {
        this.withFooter = withFooter;
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

    public class IncomeByCategoryRecord {

        Category category;
        Category subCategory;
        double grossAmount;
        double discount;
        double matrix;
        double netAmount;

        public Category getCategory() {
            return category;
        }

        public void setCategory(Category category) {
            this.category = category;
        }

        public Category getSubCategory() {
            return subCategory;
        }

        public void setSubCategory(Category subCategory) {
            this.subCategory = subCategory;
        }

        public double getGrossAmount() {
            return grossAmount;
        }

        public void setGrossAmount(double grossAmount) {
            this.grossAmount = grossAmount;
        }

        public double getMatrix() {
            return matrix;
        }

        public void setMatrix(double matrix) {
            this.matrix = matrix;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

        public double getNetAmount() {
            return netAmount;
        }

        public void setNetAmount(double netAmount) {
            this.netAmount = netAmount;
        }

    }

    public class IndividualBhtIncomeByCategoryRecord {

        PatientEncounter bht;
        Bill finalBill;
        Category category;
        Category subCategory;
        InwardChargeType inwardChargeType;
        double grossValue;
        double discount;
        double inwardAddition;
        double netValue;

        public PatientEncounter getBht() {
            return bht;
        }

        public void setBht(PatientEncounter bht) {
            this.bht = bht;
        }

        public Bill getFinalBill() {
            return finalBill;
        }

        public void setFinalBill(Bill finalBill) {
            this.finalBill = finalBill;
        }

        public Category getCategory() {
            return category;
        }

        public void setCategory(Category category) {
            this.category = category;
        }

        public Category getSubCategory() {
            return subCategory;
        }

        public void setSubCategory(Category subCategory) {
            this.subCategory = subCategory;
        }

        public InwardChargeType getInwardChargeType() {
            return inwardChargeType;
        }

        public void setInwardChargeType(InwardChargeType inwardChargeType) {
            this.inwardChargeType = inwardChargeType;
        }

        public double getGrossValue() {
            return grossValue;
        }

        public void setGrossValue(double grossValue) {
            this.grossValue = grossValue;
        }

        public double getDiscount() {
            return discount;
        }

        public void setDiscount(double discount) {
            this.discount = discount;
        }

        public double getInwardAddition() {
            return inwardAddition;
        }

        public void setInwardAddition(double inwardAddition) {
            this.inwardAddition = inwardAddition;
        }

        public double getNetValue() {
            return netValue;
        }

        public void setNetValue(double netValue) {
            this.netValue = netValue;
        }

    }

    public double getNetTotal() {
        return netTotal;
    }

    public void setNetTotal(double netTotal) {
        this.netTotal = netTotal;
    }

    public double getNetPaid() {
        return netPaid;
    }

    public void setNetPaid(double netPaid) {
        this.netPaid = netPaid;
    }

    public double getCalTotal() {
        return calTotal;
    }

    public void setCalTotal(double calTotal) {
        this.calTotal = calTotal;
    }

    public double getCreditPaid() {
        return creditPaid;
    }

    public void setCreditPaid(double creditPaid) {
        this.creditPaid = creditPaid;
    }

    public double getCreditUsed() {
        return creditUsed;
    }

    public void setCreditUsed(double creditUsed) {
        this.creditUsed = creditUsed;
    }

    public InwardReportControllerBht getInwardReportControllerBht() {
        return inwardReportControllerBht;
    }

    public void setInwardReportControllerBht(InwardReportControllerBht inwardReportControllerBht) {
        this.inwardReportControllerBht = inwardReportControllerBht;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public Bill getBill() {
        return bill;
    }

    public void setBill(Bill bill) {
        this.bill = bill;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public List<BillItem> getBillItems() {
        return billItems;
    }

    public void setBillItems(List<BillItem> billItems) {
        this.billItems = billItems;
    }

    public BillFacade getBillFacade() {
        return billFacade;
    }

    public void setBillFacade(BillFacade billFacade) {
        this.billFacade = billFacade;
    }

    public BillItemFacade getBillItemFacade() {
        return billItemFacade;
    }

    public void setBillItemFacade(BillItemFacade billItemFacade) {
        this.billItemFacade = billItemFacade;
    }

    public boolean isDischargeDate() {
        return dischargeDate;
    }

    public void setDischargeDate(boolean dischargeDate) {
        this.dischargeDate = dischargeDate;
    }

    public boolean isBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(boolean bhtNo) {
        this.bhtNo = bhtNo;
    }

    public boolean isPaymentMethord() {
        return paymentMethord;
    }

    public void setPaymentMethord(boolean paymentMethord) {
        this.paymentMethord = paymentMethord;
    }

    public boolean isCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(boolean creditCompany) {
        this.creditCompany = creditCompany;
    }

    public boolean isPerson() {
        return person;
    }

    public void setPerson(boolean person) {
        this.person = person;
    }

    public boolean isRoom() {
        return room;
    }

    public void setRoom(boolean room) {
        this.room = room;
    }

    public boolean isRefDoctor() {
        return refDoctor;
    }

    public void setRefDoctor(boolean refDoctor) {
        this.refDoctor = refDoctor;
    }

    public boolean isAddmitDetails() {
        return AddmitDetails;
    }

    public void setAddmitDetails(boolean AddmitDetails) {
        this.AddmitDetails = AddmitDetails;
    }

    public boolean isBilledBy() {
        return billedBy;
    }

    public void setBilledBy(boolean billedBy) {
        this.billedBy = billedBy;
    }

    public boolean isFinalBillTotal() {
        return finalBillTotal;
    }

    public void setFinalBillTotal(boolean finalBillTotal) {
        this.finalBillTotal = finalBillTotal;
    }

    public boolean isPaidByPatient() {
        return paidByPatient;
    }

    public void setPaidByPatient(boolean paidByPatient) {
        this.paidByPatient = paidByPatient;
    }

    public boolean isCreditPaidAmount() {
        return creditPaidAmount;
    }

    public void setCreditPaidAmount(boolean creditPaidAmount) {
        this.creditPaidAmount = creditPaidAmount;
    }

    public boolean isDueAmount() {
        return dueAmount;
    }

    public void setDueAmount(boolean dueAmount) {
        this.dueAmount = dueAmount;
    }

    public boolean isCalculatedAmount() {
        return calculatedAmount;
    }

    public void setCalculatedAmount(boolean calculatedAmount) {
        this.calculatedAmount = calculatedAmount;
    }

    public boolean isDifferentAmount() {
        return differentAmount;
    }

    public void setDifferentAmount(boolean differentAmount) {
        this.differentAmount = differentAmount;
    }

    public boolean isGuardian() {
        return guardian;
    }

    public void setGuardian(boolean guardian) {
        this.guardian = guardian;
    }

    public boolean isDevelopers() {
        return developers;
    }

    public void setDevelopers(boolean developers) {
        this.developers = developers;
    }

    public boolean isWithoutCancelBHT() {
        return withoutCancelBHT;
    }

    public void setWithoutCancelBHT(boolean withoutCancelBHT) {
        this.withoutCancelBHT = withoutCancelBHT;
    }

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }

    public String getInvoceNo() {
        return invoceNo;
    }

    public void setInvoceNo(String invoceNo) {
        this.invoceNo = invoceNo;
    }

    public double getTotalVat() {
        return totalVat;
    }

    public void setTotalVat(double totalVat) {
        this.totalVat = totalVat;
    }

    public double getTotalVatCalculatedValue() {
        return totalVatCalculatedValue;
    }

    public void setTotalVatCalculatedValue(double totalVatCalculatedValue) {
        this.totalVatCalculatedValue = totalVatCalculatedValue;
    }

    public String getVatRegNo() {
        return vatRegNo;
    }

    public void setVatRegNo(String vatRegNo) {
        this.vatRegNo = vatRegNo;
    }

}
