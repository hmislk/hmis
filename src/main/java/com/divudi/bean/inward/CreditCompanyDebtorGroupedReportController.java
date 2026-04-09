package com.divudi.bean.inward;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.CountedServiceType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.CreditCompanyDebtorGroupDTO;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Controller for the Credit Company Debtor Grouped Report.
 * Produces the same data as the flat debtor report but groups rows by credit
 * company, with per-company subtotals and a grand total.
 */
@Named
@SessionScoped
public class CreditCompanyDebtorGroupedReportController implements Serializable {

    @EJB
    private BillFacade billFacade;
    @EJB
    BillItemFacade BillItemFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;

    private Date fromDate = startOfCurrentMonth();
    private Date toDate = new Date();
    private String dateBasis = "createdAt";
    private Institution institution;
    private Institution admittingInstitution;
    private Institution site;
    private Department department;
    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    private boolean outstandingOnly = false;

    private List<CreditCompanyDebtorGroupDTO> groups;
    private double grandBillTotal;
    private double grandSettledByCompany;
    private double grandSettledByPatient;
    private double grandPaidTotal;
    private double grandOutstandingTotal;

    public void generateReport() {
        groups = new ArrayList<>();
        grandBillTotal = 0;
        grandSettledByCompany = 0;
        grandSettledByPatient = 0;
        grandPaidTotal = 0;
        grandOutstandingTotal = 0;

        List<Bill> allBills = new ArrayList<>();

        // --- Discharged patients: query INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY ---
        HashMap<String, Object> hm = new HashMap<>();
        String dateField = resolveDateField(dateBasis, "b.billDate", "b.patientEncounter");
        String sql = "Select b from Bill b"
                + " where b.retired=false"
                + " and (b.cancelled=false or b.cancelled is null)"
                + " and b.billTypeAtomic=:bta"
                + " and " + dateField + " between :frm and :to";

        if (institution != null) {
            sql += " and b.creditCompany=:cc";
            hm.put("cc", institution);
        }
        if (admittingInstitution != null) {
            sql += " and b.patientEncounter.institution=:ins";
            hm.put("ins", admittingInstitution);
        }
        if (site != null) {
            sql += " and b.patientEncounter.department.site=:site";
            hm.put("site", site);
        }
        if (department != null) {
            sql += " and b.patientEncounter.department=:dept";
            hm.put("dept", department);
        }
        if (admissionType != null) {
            sql += " and b.patientEncounter.admissionType=:at";
            hm.put("at", admissionType);
        }
        if (paymentMethod != null) {
            sql += " and b.patientEncounter.paymentMethod=:pm";
            hm.put("pm", paymentMethod);
        }
        sql += " order by b.creditCompany.name, b.billDate";
        hm.put("bta", BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
        hm.put("frm", fromDate);
        hm.put("to", toDate);

        List<Bill> dischargedBills = billFacade.findByJpql(sql, hm, TemporalType.TIMESTAMP);
        List<BillTypeAtomic> settlementTypes =
                BillTypeAtomic.findByCountedServiceType(CountedServiceType.CREDIT_SETTLE_BY_COMPANY);

        for (Bill b : dischargedBills) {
            String settledSql = "Select sum(bi.netValue) from BillItem bi"
                    + " where bi.retired=false"
                    + " and bi.referenceBill=:bill"
                    + " and bi.bill.billTypeAtomic in :types";
            HashMap<String, Object> settledParams = new HashMap<>();
            settledParams.put("bill", b);
            settledParams.put("types", settlementTypes);
            double settled = BillItemFacade.findDoubleByJpql(settledSql, settledParams);
            b.setPaidAmount(settled);
            b.setSettledAmountBySponsor(settled);

            double outstanding = b.getNetTotal() - settled;
            if (outstandingOnly && outstanding <= 0.01) {
                continue;
            }
            allBills.add(b);
        }

        // --- Non-discharged patients ---
        List<BillTypeAtomic> chargeTypes = Arrays.asList(
                BillTypeAtomic.INWARD_SERVICE_BILL,
                BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION,
                BillTypeAtomic.INWARD_SERVICE_BILL_CANCELLATION_DURING_BATCH_BILL_CANCELLATION,
                BillTypeAtomic.INWARD_SERVICE_BATCH_BILL,
                BillTypeAtomic.INWARD_SERVICE_BATCH_BILL_CANCELLATION,
                BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL,
                BillTypeAtomic.INWARD_OUTSIDE_CHARGES_BILL_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_CANCELLATION,
                BillTypeAtomic.DIRECT_ISSUE_INWARD_MEDICINE_RETURN
        );
        List<BillTypeAtomic> ccPaymentTypes = Arrays.asList(
                BillTypeAtomic.CREDIT_COMPANY_INPATIENT_PAYMENT,
                BillTypeAtomic.CREDIT_COMPANY_INPATIENT_PAYMENT_CANCELLATION,
                BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED,
                BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION
        );
        List<BillTypeAtomic> depositTypes = Arrays.asList(
                BillTypeAtomic.INWARD_DEPOSIT,
                BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION,
                BillTypeAtomic.INWARD_DEPOSIT_REFUND,
                BillTypeAtomic.INWARD_DEPOSIT_REFUND_CANCELLATION
        );

        String encSql = "Select pe from PatientEncounter pe"
                + " where pe.retired=false"
                + " and (pe.discharged=false or pe.discharged is null)"
                + " and pe.creditCompany is not null"
                + " and pe.dateOfAdmission between :frm and :to";
        HashMap<String, Object> encHm = new HashMap<>();
        if (institution != null) {
            encSql += " and pe.creditCompany=:cc";
            encHm.put("cc", institution);
        }
        if (admittingInstitution != null) {
            encSql += " and pe.institution=:ins";
            encHm.put("ins", admittingInstitution);
        }
        if (site != null) {
            encSql += " and pe.department.site=:site";
            encHm.put("site", site);
        }
        if (department != null) {
            encSql += " and pe.department=:dept";
            encHm.put("dept", department);
        }
        if (admissionType != null) {
            encSql += " and pe.admissionType=:at";
            encHm.put("at", admissionType);
        }
        if (paymentMethod != null) {
            encSql += " and pe.paymentMethod=:pm";
            encHm.put("pm", paymentMethod);
        }
        encSql += " order by pe.creditCompany.name, pe.dateOfAdmission";
        encHm.put("frm", fromDate);
        encHm.put("to", toDate);

        List<PatientEncounter> nonDischargedEncounters =
                patientEncounterFacade.findByJpql(encSql, encHm, TemporalType.TIMESTAMP);

        for (PatientEncounter pe : nonDischargedEncounters) {
            HashMap<String, Object> chargeParams = new HashMap<>();
            chargeParams.put("pe", pe);
            chargeParams.put("chargeTypes", chargeTypes);
            double chargeTotal = billFacade.findDoubleByJpql(
                    "Select sum(b.netTotal) from Bill b where b.retired=false and b.patientEncounter=:pe and b.billTypeAtomic in :chargeTypes",
                    chargeParams);

            HashMap<String, Object> ccPaidParams = new HashMap<>();
            ccPaidParams.put("pe", pe);
            ccPaidParams.put("ccPaymentTypes", ccPaymentTypes);
            double ccPaid = billFacade.findDoubleByJpql(
                    "Select sum(b.netTotal) from Bill b where b.retired=false and b.patientEncounter=:pe and b.billTypeAtomic in :ccPaymentTypes",
                    ccPaidParams);

            HashMap<String, Object> depositParams = new HashMap<>();
            depositParams.put("pe", pe);
            depositParams.put("depositTypes", depositTypes);
            double deposited = billFacade.findDoubleByJpql(
                    "Select sum(b.netTotal) from Bill b where b.retired=false and b.patientEncounter=:pe and b.billTypeAtomic in :depositTypes",
                    depositParams);

            double totalPaid = ccPaid + deposited;
            double outstanding = chargeTotal - totalPaid;

            if (outstandingOnly && outstanding <= 0.01) {
                continue;
            }

            Bill syntheticBill = new Bill();
            syntheticBill.setPatientEncounter(pe);
            syntheticBill.setPatient(pe.getPatient());
            syntheticBill.setCreditCompany(pe.getCreditCompany());
            syntheticBill.setDeptId("(Active)");
            syntheticBill.setBillDate(pe.getDateOfAdmission());
            syntheticBill.setNetTotal(chargeTotal);
            syntheticBill.setSettledAmountBySponsor(ccPaid);
            syntheticBill.setSettledAmountByPatient(deposited);
            syntheticBill.setPaidAmount(totalPaid);
            allBills.add(syntheticBill);
        }

        // --- Group bills by credit company ---
        LinkedHashMap<String, CreditCompanyDebtorGroupDTO> groupMap = new LinkedHashMap<>();
        for (Bill b : allBills) {
            String companyName = b.getCreditCompany() != null ? b.getCreditCompany().getName() : "(Unknown)";
            CreditCompanyDebtorGroupDTO group = groupMap.computeIfAbsent(companyName,
                    CreditCompanyDebtorGroupDTO::new);
            group.addBill(b);
        }

        groups = new ArrayList<>(groupMap.values());
        for (CreditCompanyDebtorGroupDTO g : groups) {
            grandBillTotal += g.getSubBillTotal();
            grandSettledByCompany += g.getSubSettledByCompany();
            grandSettledByPatient += g.getSubSettledByPatient();
            grandPaidTotal += g.getSubTotalPaid();
            grandOutstandingTotal += g.getSubOutstandingTotal();
        }
    }

    private String resolveDateField(String basis, String defaultField, String encounterAlias) {
        if ("dischargeDate".equals(basis)) {
            return encounterAlias + ".dateOfDischarge";
        } else if ("admissionDate".equals(basis)) {
            return encounterAlias + ".dateOfAdmission";
        }
        return defaultField;
    }

    public void makeNull() {
        fromDate = startOfCurrentMonth();
        toDate = new Date();
        dateBasis = "createdAt";
        institution = null;
        admittingInstitution = null;
        site = null;
        department = null;
        admissionType = null;
        paymentMethod = null;
        outstandingOnly = false;
        groups = null;
        grandBillTotal = 0;
        grandSettledByCompany = 0;
        grandSettledByPatient = 0;
        grandPaidTotal = 0;
        grandOutstandingTotal = 0;
    }

    private static Date startOfCurrentMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    // Getters and setters

    public Date getFromDate() { return fromDate; }
    public void setFromDate(Date fromDate) { this.fromDate = fromDate; }

    public Date getToDate() { return toDate; }
    public void setToDate(Date toDate) { this.toDate = toDate; }

    public String getDateBasis() { return dateBasis; }
    public void setDateBasis(String dateBasis) { this.dateBasis = dateBasis; }

    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }

    public Institution getAdmittingInstitution() { return admittingInstitution; }
    public void setAdmittingInstitution(Institution admittingInstitution) { this.admittingInstitution = admittingInstitution; }

    public Institution getSite() { return site; }
    public void setSite(Institution site) { this.site = site; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public AdmissionType getAdmissionType() { return admissionType; }
    public void setAdmissionType(AdmissionType admissionType) { this.admissionType = admissionType; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public boolean isOutstandingOnly() { return outstandingOnly; }
    public void setOutstandingOnly(boolean outstandingOnly) { this.outstandingOnly = outstandingOnly; }

    public List<CreditCompanyDebtorGroupDTO> getGroups() { return groups; }
    public double getGrandBillTotal() { return grandBillTotal; }
    public double getGrandSettledByCompany() { return grandSettledByCompany; }
    public double getGrandSettledByPatient() { return grandSettledByPatient; }
    public double getGrandPaidTotal() { return grandPaidTotal; }
    public double getGrandOutstandingTotal() { return grandOutstandingTotal; }
}
