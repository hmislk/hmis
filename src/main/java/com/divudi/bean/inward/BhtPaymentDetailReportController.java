package com.divudi.bean.inward;

import com.divudi.core.data.BillType;
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
 * One row per individual transaction: a "Make a Deposit" (INWARD_DEPOSIT)
 * payment, a "Make a Payment" (INWARD_PAYMENT) payment, a "Post Final Payment"
 * (BillType.PostFinalBillInwardPayment) payment, or a CC settlement item -
 * see {@link com.divudi.core.data.dto.BhtPaymentDetailDTO#getPaymentCategory()}.
 * The three payment kinds are kept as separate categories and separate
 * per-method footer totals (issue #23262).
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
    private double grandTotalPayments;
    private double grandTotalPostPayments;
    /**
     * Ordered map of payment method → "Make a Deposit" (INWARD_DEPOSIT) total;
     * only methods with non-zero totals.
     */
    private Map<PaymentMethod, Double> depositTotalByMethod = new LinkedHashMap<>();
    /** Deposit payment methods in the order they appeared, for UI iteration. */
    private List<PaymentMethod> usedDepositMethods = new ArrayList<>();
    /**
     * Ordered map of payment method → "Make a Payment" (INWARD_PAYMENT) total.
     * Kept separate from {@link #depositTotalByMethod} (deposits) and
     * {@link #postPaymentTotalByMethod} (post-final-bill payments) per issue
     * #23262 - deposit, payment and post-payment amounts must not be conflated
     * in a single per-method total.
     */
    private Map<PaymentMethod, Double> paymentTotalByMethod = new LinkedHashMap<>();
    private List<PaymentMethod> usedPaymentMethods = new ArrayList<>();
    /**
     * Ordered map of payment method → post-final-bill ("Post Final Payment")
     * total. Kept separate from deposits and payments per issue #23262.
     */
    private Map<PaymentMethod, Double> postPaymentTotalByMethod = new LinkedHashMap<>();
    private List<PaymentMethod> usedPostPaymentMethods = new ArrayList<>();

    public void generateReport() {
        reportRows = new ArrayList<>();
        grandTotal = 0;
        grandTotalCcSettlement = 0;
        grandTotalPayments = 0;
        grandTotalPostPayments = 0;
        depositTotalByMethod = new LinkedHashMap<>();
        usedDepositMethods = new ArrayList<>();
        paymentTotalByMethod = new LinkedHashMap<>();
        usedPaymentMethods = new ArrayList<>();
        postPaymentTotalByMethod = new LinkedHashMap<>();
        usedPostPaymentMethods = new ArrayList<>();

        List<PatientEncounter> encounters = fetchEncounters();
        if (encounters == null || encounters.isEmpty()) {
            return;
        }

        for (PatientEncounter enc : encounters) {
            String patientName = enc.getPatient() != null && enc.getPatient().getPerson() != null
                    ? enc.getPatient().getPerson().getNameWithTitle() : "";

            // "Make a Deposit" (INWARD_DEPOSIT) payments — one row per Payment record
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
                row.setPaymentCategory("Deposit");
                reportRows.add(row);
                double depositAmt = Math.abs(p.getPaidValue());
                grandTotal += depositAmt;
                if (p.getPaymentMethod() != null) {
                    depositTotalByMethod.merge(p.getPaymentMethod(), depositAmt, Double::sum);
                }
            }

            // "Make a Payment" (INWARD_PAYMENT) payments — one row per Payment
            // record. Issue #23262: kept a separate category from deposits and
            // from post-final-bill payments.
            List<Payment> payments = fetchPayments(enc);
            for (Payment p : payments) {
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
                row.setPaymentCategory("Payment");
                reportRows.add(row);
                double paymentAmt = Math.abs(p.getPaidValue());
                grandTotal += paymentAmt;
                grandTotalPayments += paymentAmt;
                if (p.getPaymentMethod() != null) {
                    paymentTotalByMethod.merge(p.getPaymentMethod(), paymentAmt, Double::sum);
                }
            }

            // Post-final-bill ("Post Final Payment") payments — one row per Payment record.
            // Issue #23263: these were never queried here, so a BHT whose only
            // recorded payment was a post-final settlement (no deposit, no CC
            // settlement) was silently absent from this report.
            List<Payment> postPayments = fetchPostFinalPayments(enc);
            for (Payment p : postPayments) {
                BhtPaymentDetailDTO row = new BhtPaymentDetailDTO();
                row.setBhtNo(enc.getBhtNo());
                row.setPatientName(patientName);
                row.setAdmissionType(enc.getAdmissionType());
                row.setDateOfAdmission(enc.getDateOfAdmission());
                row.setDateOfDischarge(enc.getDateOfDischarge());
                row.setBillNo(p.getBill() != null ? p.getBill().getDeptId() : "");
                row.setCreatedAt(p.getCreatedAt());
                row.setPaymentMethod(p.getPaymentMethod());
                // Signed, not abs() - see fetchPostFinalPayments() javadoc: a
                // cancelled post-final payment arrives as a separate
                // negative-amount row rather than a cancelled flag, so each row
                // must keep its real sign to show the full transaction trail.
                row.setAmount(p.getPaidValue());
                row.setReferenceNo(p.getReferenceNo());
                row.setCreditCompanyName("");
                row.setPaymentCategory("Post Payment");
                reportRows.add(row);
                grandTotal += p.getPaidValue();
                grandTotalPostPayments += p.getPaidValue();
                if (p.getPaymentMethod() != null) {
                    postPaymentTotalByMethod.merge(p.getPaymentMethod(), p.getPaidValue(), Double::sum);
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
                row.setPaymentCategory("CC Settlement");
                reportRows.add(row);
                grandTotal += bi.getNetValue();
                grandTotalCcSettlement += bi.getNetValue();
            }
        }

        // Build ordered lists of used payment methods for UI iteration
        usedDepositMethods = new ArrayList<>(depositTotalByMethod.keySet());
        usedPaymentMethods = new ArrayList<>(paymentTotalByMethod.keySet());
        usedPostPaymentMethods = new ArrayList<>(postPaymentTotalByMethod.keySet());
    }

    public double getTotalForDepositMethod(PaymentMethod pm) {
        return depositTotalByMethod.getOrDefault(pm, 0.0);
    }

    public double getTotalForPaymentMethod(PaymentMethod pm) {
        return paymentTotalByMethod.getOrDefault(pm, 0.0);
    }

    public double getTotalForPostPaymentMethod(PaymentMethod pm) {
        return postPaymentTotalByMethod.getOrDefault(pm, 0.0);
    }

    private List<PatientEncounter> fetchEncounters() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder(
                "select distinct c from PatientEncounter c where c.retired = false");

        if (fromDate != null && toDate != null) {
            boolean useAdmissionDate = "admissionDate".equals(dateBasis)
                    || admissionStatus == AdmissionStatus.ADMITTED_BUT_NOT_DISCHARGED;
            if (useAdmissionDate) {
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
                + " and p.cancelled = false"
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

    /**
     * Fetch "Make a Payment" (INWARD_PAYMENT) payments for this encounter -
     * payments toward the bill made any time during the stay. Kept separate
     * from deposits (INWARD_DEPOSIT) and post-final-bill payments
     * (BillType.PostFinalBillInwardPayment). Issue #23262.
     */
    private List<Payment> fetchPayments(PatientEncounter enc) {
        StringBuilder jpql = new StringBuilder("select p from Payment p"
                + " where p.retired = false"
                + " and p.cancelled = false"
                + " and p.bill.retired = false"
                + " and p.bill.cancelled = false"
                + " and p.bill.billTypeAtomic = :bta"
                + " and p.bill.patientEncounter = :enc");
        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.INWARD_PAYMENT);
        params.put("enc", enc);
        if (paymentMethod != null) {
            jpql.append(" and p.paymentMethod = :pm");
            params.put("pm", paymentMethod);
        }
        jpql.append(" order by p.createdAt");
        return paymentFacade.findByJpql(jpql.toString(), params);
    }

    /**
     * Fetch post-final-bill ("Make Payment") payments for this encounter.
     *
     * Deliberately does NOT filter on {@code p.cancelled} / {@code p.bill.cancelled}:
     * mirrors {@link BhtPaymentSummaryReportController#fetchPostFinalPayments}
     * - cancellation of a post-final-bill payment arrives as a separate
     * negative-amount row of the same bill type rather than a cancelled flag
     * on the original, so each row is kept as its own line (with its natural
     * sign) instead of being filtered or netted here. Issue #23263.
     */
    private List<Payment> fetchPostFinalPayments(PatientEncounter enc) {
        StringBuilder jpql = new StringBuilder("select p from Payment p"
                + " where p.retired = false"
                + " and p.bill.retired = false"
                + " and p.bill.billType = :bt"
                + " and p.bill.patientEncounter = :enc");
        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.PostFinalBillInwardPayment);
        params.put("enc", enc);
        if (paymentMethod != null) {
            jpql.append(" and p.paymentMethod = :pm");
            params.put("pm", paymentMethod);
        }
        jpql.append(" order by p.createdAt");
        return paymentFacade.findByJpql(jpql.toString(), params);
    }

    /**
     * Fetch BillItems from INPATIENT_CREDIT_COMPANY_PAYMENT_RECEIVED and
     * INPATIENT_CREDIT_COMPANY_PAYMENT_CANCELLATION bills that reference this encounter.
     * Deliberately does NOT filter on bi.bill.cancelled: cancelling a CC payment sets
     * cancelled=true on the original RECEIVED bill while its negative-value items live on
     * a separate CANCELLATION bill, so filtering by cancelled would drop the original
     * positive row and leave only the negative one. Including both rows with their signed
     * netValue preserves the audit trail and nets out correctly.
     */
    private List<BillItem> fetchCreditSettlementItems(PatientEncounter enc) {
        String jpql = "select bi from BillItem bi"
                + " where bi.retired = false"
                + " and bi.bill.retired = false"
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
        grandTotalPayments = 0;
        grandTotalPostPayments = 0;
        depositTotalByMethod = new LinkedHashMap<>();
        usedDepositMethods = new ArrayList<>();
        paymentTotalByMethod = new LinkedHashMap<>();
        usedPaymentMethods = new ArrayList<>();
        postPaymentTotalByMethod = new LinkedHashMap<>();
        usedPostPaymentMethods = new ArrayList<>();
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

    public double getGrandTotalPayments() { return grandTotalPayments; }

    public double getGrandTotalPostPayments() { return grandTotalPostPayments; }

    public List<PaymentMethod> getUsedDepositMethods() { return usedDepositMethods; }

    public Map<PaymentMethod, Double> getDepositTotalByMethod() { return depositTotalByMethod; }

    public List<PaymentMethod> getUsedPaymentMethods() { return usedPaymentMethods; }

    public Map<PaymentMethod, Double> getPaymentTotalByMethod() { return paymentTotalByMethod; }

    public List<PaymentMethod> getUsedPostPaymentMethods() { return usedPostPaymentMethods; }

    public Map<PaymentMethod, Double> getPostPaymentTotalByMethod() { return postPaymentTotalByMethod; }
}
