/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.inward;

import com.divudi.data.BillType;
import com.divudi.data.PaymentMethod;
import com.divudi.data.inward.InwardChargeType;
import com.divudi.ejb.CommonFunctions;
import com.divudi.entity.Bill;
import com.divudi.entity.Category;
import com.divudi.entity.Institution;
import com.divudi.entity.PatientEncounter;
import com.divudi.entity.inward.Admission;
import com.divudi.entity.inward.AdmissionType;
import com.divudi.facade.AdmissionTypeFacade;
import com.divudi.facade.PatientEncounterFacade;
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
    double grossTotals;
    double discounts;
    double netTotals;
    List<IncomeByCategoryRecord> incomeByCategoryRecords;
    List<IndividualBhtIncomeByCategoryRecord> individualBhtIncomeByCategoryRecord;

    List<AdmissionType> admissionty;

    @EJB
    PatientEncounterFacade peFacade;
    @EJB
    AdmissionTypeFacade admissionTypeFacade;
    List<PatientEncounter> patientEncounters;

    public List<PatientEncounter> getPatientEncounters() {
        return patientEncounters;
    }

    public void setPatientEncounters(List<PatientEncounter> patientEncounters) {
        this.patientEncounters = patientEncounters;
    }
    double netTotal;
    double netPaid;

    public void fillAdmissionBook() {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.dateOfAdmission between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad";
            m.put("ad", admissionType);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        calTtoal();
    }

    public void fillAdmissionBookOnlyInward() {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=false "
                //                + " and b.paymentFinalized=false "
                + " and b.dateOfAdmission between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad";
            m.put("ad", admissionType);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        calTtoal();
    }

    public void fillAdmissionBookOnlyDischarged() {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                //                + " and b.paymentFinalized=false "
                + " and b.dateOfAdmission between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad";
            m.put("ad", admissionType);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        calTtoal();

    }

    public void fillAdmissionBookOnlyDischargedNotFinalized() {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentFinalized=false "
                + " and b.dateOfAdmission between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad";
            m.put("ad", admissionType);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        calTtoal();

    }

    public void fillAdmissionBookOnlyDischargedFinalized() {
        Map m = new HashMap();
        String sql = "select b from PatientEncounter b "
                + " where b.retired=false "
                + " and b.discharged=true "
                + " and b.paymentFinalized=true "
                + " and b.dateOfAdmission between :fd and :td ";

        if (admissionType != null) {
            sql += " and b.admissionType =:ad";
            m.put("ad", admissionType);
        }

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        calTtoal();
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

    public void fillAdmissionBookOnlyInwardDeleted() {
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
        patientEncounters = getPeFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

    }

    double total;
    double paid;
    double creditPaid;
    double creditUsed;
    double calTotal;

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

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);
        calTotalDischarged();
    }

    public void calTotalDischarged() {
        if (patientEncounters == null) {
            return;
        }
        total = 0;
        paid = 0;
        calTotal = 0;
        creditPaid = 0;
        creditUsed = 0;
        for (PatientEncounter p : patientEncounters) {
            inwardReportControllerBht.setPatientEncounter(p);
            inwardReportControllerBht.process();
            p.setTransTotal(inwardReportControllerBht.getNetTotal());

            if (p.getFinalBill() != null) {
                total += p.getFinalBill().getNetTotal();
                paid += p.getFinalBill().getPaidAmount();
            }

            creditUsed += p.getCreditUsedAmount();
            creditPaid += p.getPaidByCreditCompany();
            calTotal += p.getTransTotal();
        }
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

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

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

        sql += " order by  b.dateOfDischarge";

        m.put("fd", fromDate);
        m.put("td", toDate);
        patientEncounters = getPeFacade().findBySQL(sql, m, TemporalType.TIMESTAMP);

        calTotalDischarged();
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

    @EJB
    CommonFunctions commonFunctions;

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = commonFunctions.getStartOfMonth(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = new Date();
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

}
