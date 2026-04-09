package com.divudi.bean.inward;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.BhtPaymentDetailDTO;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.entity.BillItem;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PaymentFacade;
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
 * Controller for BHT Deposit and Credit Settlement Detail Report.
 * One row per individual payment (deposit payment or CC settlement item).
 */
@Named
@SessionScoped
public class BhtPaymentDetailReportController implements Serializable {

    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private BillItemFacade billItemFacade;
    @EJB
    private PaymentFacade paymentFacade;

    private Date fromDate = startOfCurrentMonth();
    private Date toDate = new Date();
    private String dateBasis = "dischargeDate";
    private AdmissionStatus admissionStatus = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    private Institution institution;
    private Institution site;
    private Department department;

    private List<BhtPaymentDetailDTO> reportRows;
    private double grandTotal;
    private double grandTotalCcSettlement;
    /** Ordered map of payment method → deposit total; only methods with non-zero totals. */
    private Map<PaymentMethod, Double> totalByMethod = new LinkedHashMap<>();
    /** Payment methods in the order they appeared, for UI iteration. */
    private List<PaymentMethod> usedPaymentMethods = new ArrayList<>();

    public void generateReport() {
        reportRows = new ArrayList<>();
        grandTotal = 0;
        grandTotalCcSettlement = 0;
        totalByMethod = new LinkedHashMap<>();
        usedPaymentMethods = new ArrayList<>();

        List<PatientEncounter> encounters = fetchEncounters();
        if (encounters == null || encounters.isEmpty()) {
            return;
        }

        for (PatientEncounter enc : encounters) {
            String patientName = enc.getPatient() != null && enc.getPatient().getPerson() != null
                    ? enc.getPatient().getPerson().getNameWithTitle() : "";

            // Deposit payments — one row per Payment record
            List<Payment> deposits = fetchDepositPayments(enc);
            for (Payment p : deposits) {
                BhtPaymentDetailDTO row = new BhtPaymentDetailDTO();
                row.setBhtNo(enc.getBhtNo());
                row.setPatientName(patientName);
                row.setAdmissionType(enc.getAdmissionType());
                row.setDateOfAdmission(enc.getDateOfAdmission());
                row.setDateOfDischarge(enc.getDateOfDischarge());
                row.setBillNo(p.getBill() != null ? p.getBill().getDeptId() : "");
                row.setCreatedAt(p.getCreatedAt());
                row.setPaymentMethod(p.getPaymentMethod());
                row.setAmount(Math.abs(p.getPaidValue()));
                row.setReferenceNo(p.getReferenceNo());
                row.setCreditCompanyName("");
                reportRows.add(row);
                double depositAmt = Math.abs(p.getPaidValue());
                grandTotal += depositAmt;
                if (p.getPaymentMethod() != null) {
                    totalByMethod.merge(p.getPaymentMethod(), depositAmt, Double::sum);
                }
            }

            // CC settlement items — one row per BillItem
            List<BillItem> ccItems = fetchCreditSettlementItems(enc);
            for (BillItem bi : ccItems) {
                String companyName = "";
                if (bi.getReferenceBill() != null && bi.getReferenceBill().getCreditCompany() != null) {
                    companyName = bi.getReferenceBill().getCreditCompany().getName();
                }
                BhtPaymentDetailDTO row = new BhtPaymentDetailDTO();
                row.setBhtNo(enc.getBhtNo());
                row.setPatientName(patientName);
                row.setAdmissionType(enc.getAdmissionType());
                row.setDateOfAdmission(enc.getDateOfAdmission());
                row.setDateOfDischarge(enc.getDateOfDischarge());
                row.setBillNo(bi.getBill() != null ? bi.getBill().getDeptId() : "");
                row.setCreatedAt(bi.getCreatedAt());
                row.setPaymentMethod(null);
                row.setAmount(bi.getNetValue());
                row.setReferenceNo("");
                row.setCreditCompanyName(companyName);
                reportRows.add(row);
                grandTotal += bi.getNetValue();
                grandTotalCcSettlement += bi.getNetValue();
            }
        }

        // Build ordered list of used payment methods for UI iteration
        usedPaymentMethods = new ArrayList<>(totalByMethod.keySet());
    }

    public double getTotalForMethod(PaymentMethod pm) {
        return totalByMethod.getOrDefault(pm, 0.0);
    }

    private List<PatientEncounter> fetchEncounters() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "select distinct c from PatientEncounter c where c.retired = false");

        if (fromDate != null && toDate != null) {
            if ("admissionDate".equals(dateBasis)) {
                jpql.append(" and c.dateOfAdmission between :fromDate and :toDate");
            } else {
                jpql.append(" and c.dateOfDischarge between :fromDate and :toDate");
            }
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);
        }

        if (admissionStatus != null && admissionStatus != AdmissionStatus.ANY_STATUS) {
            switch (admissionStatus) {
                case ADMITTED_BUT_NOT_DISCHARGED:
                    jpql.append(" and c.discharged = :dis");
                    params.put("dis", false);
                    break;
                case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                    jpql.append(" and c.discharged = :dis and c.paymentFinalized = :pf");
                    params.put("dis", true);
                    params.put("pf", false);
                    break;
                case DISCHARGED_AND_FINAL_BILL_COMPLETED:
                    jpql.append(" and c.discharged = :dis and c.paymentFinalized = :pf");
                    params.put("dis", true);
                    params.put("pf", true);
                    break;
                default:
                    break;
            }
        }

        if (admissionType != null) {
            jpql.append(" and c.admissionType = :admType");
            params.put("admType", admissionType);
        }
        if (institution != null) {
            jpql.append(" and c.institution = :ins");
            params.put("ins", institution);
        }
        if (site != null) {
            jpql.append(" and c.department.site = :site");
            params.put("site", site);
        }
        if (department != null) {
            jpql.append(" and c.department = :dept");
            params.put("dept", department);
        }
        jpql.append(" order by c.bhtNo");
        return patientEncounterFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
    }

    private List<Payment> fetchDepositPayments(PatientEncounter enc) {
        StringBuilder jpql = new StringBuilder("select p from Payment p"
                + " where p.retired = false"
                + " and p.bill.retired = false"
                + " and p.bill.cancelled = false"
                + " and p.bill.billTypeAtomic = :bta"
                + " and p.bill.patientEncounter = :enc");
        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.INWARD_DEPOSIT);
        params.put("enc", enc);
        if (paymentMethod != null) {
            jpql.append(" and p.paymentMethod = :pm");
            params.put("pm", paymentMethod);
        }
        jpql.append(" order by p.createdAt");
        return paymentFacade.findByJpql(jpql.toString(), params);
    }

    private List<BillItem> fetchCreditSettlementItems(PatientEncounter enc) {
        String jpql = "select bi from BillItem bi"
                + " where bi.retired = false"
                + " and bi.bill.retired = false"
                + " and bi.bill.cancelled = false"
                + " and bi.bill.billTypeAtomic in :btas"
                + " and bi.patientEncounter = :enc"
                + " order by bi.createdAt";
        Map<String, Object> params = new HashMap<>();
        params.put("btas", Arrays.asList(
                BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED,
                BillTypeAtomic.INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION));
        params.put("enc", enc);
        return billItemFacade.findByJpql(jpql, params);
    }

    public void makeNull() {
        fromDate = startOfCurrentMonth();
        toDate = new Date();
        dateBasis = "dischargeDate";
        admissionStatus = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
        admissionType = null;
        paymentMethod = null;
        institution = null;
        site = null;
        department = null;
        reportRows = null;
        grandTotal = 0;
        grandTotalCcSettlement = 0;
        totalByMethod = new LinkedHashMap<>();
        usedPaymentMethods = new ArrayList<>();
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

    // Getters / setters

    public Date getFromDate() { return fromDate; }
    public void setFromDate(Date fromDate) { this.fromDate = fromDate; }

    public Date getToDate() { return toDate; }
    public void setToDate(Date toDate) { this.toDate = toDate; }

    public String getDateBasis() { return dateBasis; }
    public void setDateBasis(String dateBasis) { this.dateBasis = dateBasis; }

    public AdmissionStatus getAdmissionStatus() { return admissionStatus; }
    public void setAdmissionStatus(AdmissionStatus admissionStatus) { this.admissionStatus = admissionStatus; }

    public AdmissionType getAdmissionType() { return admissionType; }
    public void setAdmissionType(AdmissionType admissionType) { this.admissionType = admissionType; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution institution) { this.institution = institution; }

    public Institution getSite() { return site; }
    public void setSite(Institution site) { this.site = site; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public List<BhtPaymentDetailDTO> getReportRows() { return reportRows; }

    public double getGrandTotal() { return grandTotal; }

    public double getGrandTotalCcSettlement() { return grandTotalCcSettlement; }

    public List<PaymentMethod> getUsedPaymentMethods() { return usedPaymentMethods; }

    public Map<PaymentMethod, Double> getTotalByMethod() { return totalByMethod; }
}
