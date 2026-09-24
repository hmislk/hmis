package com.divudi.bean.inward;

import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.data.CountedServiceType;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.BhtPaymentDetailDTO;
import com.divudi.core.data.dto.BhtPaymentDetailDTO.CreditCompanySettlement;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Payment;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.facade.PaymentFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.persistence.TemporalType;
import org.primefaces.component.api.UIColumn;

/**
 * Controller for the Coop-specific sibling of the BHT Deposit Detail report
 * (issue #23770). Deliberately duplicates
 * {@link BhtDepositDetailReportController}'s query logic rather than
 * extending/parametrizing it - several other hospitals use the original
 * report and explicitly do not want it changed, so this report must be able
 * to evolve independently without any risk to that one.
 *
 * <p>One row per individual payment, same as the original report. Each row
 * additionally carries every credit company on that admission's Final Bill,
 * with its Due/Paid/Balance recomputed via the CREDIT_SETTLE_BY_COMPANY
 * settlement pattern - see
 * developer_docs/billing/inward-cc-settlement-tracking.md.</p>
 *
 * <p>Refunds and cancellations are listed as their own rows with their signed
 * (negative) amount and counted in the totals, and an original bill stays
 * listed after it is cancelled or refunded (issue #23980).</p>
 */
@Named
@SessionScoped
public class BhtDepositDetailWithCreditCompaniesReportController implements Serializable {

    @EJB
    private PatientEncounterFacade patientEncounterFacade;
    @EJB
    private PaymentFacade paymentFacade;
    @EJB
    private BillFacade billFacade;
    @EJB
    private BillItemFacade billItemFacade;

    private Date fromDate = startOfCurrentMonth();
    private Date toDate = new Date();
    private String dateBasis = "dischargeDate";
    private String reportType = "DEPOSIT";
    private AdmissionStatus admissionStatus = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    private Institution institution;
    private Institution site;
    private Department department;

    private List<BhtPaymentDetailDTO> reportRows;
    private double grandTotal;
    private Map<PaymentMethod, Double> totalByMethod = new LinkedHashMap<>();
    private List<PaymentMethod> usedPaymentMethods = new ArrayList<>();

    public void generateReport() {
        reportRows = new ArrayList<>();
        grandTotal = 0;
        totalByMethod = new LinkedHashMap<>();
        usedPaymentMethods = new ArrayList<>();

        List<PatientEncounter> encounters = fetchEncounters();
        if (encounters == null || encounters.isEmpty()) {
            return;
        }

        Map<Long, List<CreditCompanySettlement>> settlementsByEncounter = new HashMap<>();

        for (PatientEncounter enc : encounters) {
            String patientName = enc.getPatient() != null && enc.getPatient().getPerson() != null
                    ? enc.getPatient().getPerson().getNameWithTitle() : "";
            List<CreditCompanySettlement> settlements = settlementsByEncounter
                    .computeIfAbsent(enc.getId(), id -> fetchCreditCompanySettlements(enc));

            List<Payment> deposits = fetchDepositPayments(enc);
            for (Payment p : deposits) {
                BhtPaymentDetailDTO row = new BhtPaymentDetailDTO();
                row.setBhtNo(enc.getBhtNo());
                row.setPatientName(patientName);
                row.setAdmissionType(enc.getAdmissionType());
                row.setDateOfAdmission(enc.getDateOfAdmission());
                row.setDateOfDischarge(enc.getDateOfDischarge());
                row.setBillNo(p.getBill() != null ? p.getBill().getDeptId() : "");
                row.setBillType(p.getBill() != null ? billTypeLabel(p.getBill().getBillTypeAtomic()) : "");
                row.setCreatedAt(p.getCreatedAt());
                row.setPaymentMethod(p.getPaymentMethod());
                // Signed, not abs(): cancellation and refund Payments are stored
                // negative and must reduce the totals (issue #23980).
                double amt = p.getPaidValue();
                row.setAmount(amt);
                row.setReferenceNo(p.getReferenceNo());
                row.setCreditCompanySettlements(settlements);
                reportRows.add(row);

                grandTotal += amt;
                if (p.getPaymentMethod() != null) {
                    totalByMethod.merge(p.getPaymentMethod(), amt, Double::sum);
                }
            }
        }

        usedPaymentMethods = new ArrayList<>(totalByMethod.keySet());
    }

    /**
     * Fetches every credit company commitment on this encounter's Final
     * Bill - one CC commitment bill (INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY)
     * per company - with Due/Paid/Balance recomputed per
     * InwardReportController1.inwardCreditCompanyDebtors(). Returns an empty
     * list for a self-paying admission with no Final Bill or no credit
     * company allocations.
     */
    private List<CreditCompanySettlement> fetchCreditCompanySettlements(PatientEncounter enc) {
        Bill finalBill = enc.getFinalBill();
        if (finalBill == null) {
            return Collections.emptyList();
        }

        String jpql = "select b from Bill b"
                + " where b.retired = false"
                + " and (b.cancelled = false or b.cancelled is null)"
                + " and b.billTypeAtomic = :bta"
                + " and b.referenceBill = :finalBill";
        Map<String, Object> params = new HashMap<>();
        params.put("bta", BillTypeAtomic.INWARD_FINAL_BILL_PAYMENT_BY_CREDIT_COMPANY);
        params.put("finalBill", finalBill);
        List<Bill> ccCommitmentBills = billFacade.findByJpql(jpql, params);
        if (ccCommitmentBills == null || ccCommitmentBills.isEmpty()) {
            return Collections.emptyList();
        }

        List<BillTypeAtomic> settlementTypes =
                BillTypeAtomic.findByCountedServiceType(CountedServiceType.CREDIT_SETTLE_BY_COMPANY);

        List<CreditCompanySettlement> settlements = new ArrayList<>();
        for (Bill ccBill : ccCommitmentBills) {
            double due = ccBill.getNetTotal();

            String settledJpql = "select sum(bi.netValue) from BillItem bi"
                    + " where bi.retired = false"
                    + " and bi.referenceBill = :bill"
                    + " and bi.bill.billTypeAtomic in :types";
            Map<String, Object> settledParams = new HashMap<>();
            settledParams.put("bill", ccBill);
            settledParams.put("types", settlementTypes);
            double paid = billItemFacade.findDoubleByJpql(settledJpql, settledParams);

            String companyName = ccBill.getCreditCompany() != null ? ccBill.getCreditCompany().getName() : "";
            settlements.add(new CreditCompanySettlement(companyName, due, paid, due - paid));
        }
        return settlements;
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

    private static final List<BillTypeAtomic> DEPOSIT_BILL_TYPE_ATOMICS = Arrays.asList(
            BillTypeAtomic.INWARD_DEPOSIT,
            BillTypeAtomic.INWARD_DEPOSIT_CANCELLATION,
            BillTypeAtomic.INWARD_DEPOSIT_REFUND,
            BillTypeAtomic.INWARD_DEPOSIT_REFUND_CANCELLATION);

    private static final List<BillTypeAtomic> PAYMENT_BILL_TYPE_ATOMICS = Arrays.asList(
            BillTypeAtomic.INWARD_PAYMENT,
            BillTypeAtomic.INWARD_PAYMENT_CANCELLATION,
            BillTypeAtomic.INWARD_PAYMENT_REFUND,
            BillTypeAtomic.INWARD_PAYMENT_REFUND_CANCELLATION);

    private static final List<BillTypeAtomic> POST_FINAL_BILL_TYPE_ATOMICS = Arrays.asList(
            BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT,
            BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT_CANCELLATION,
            BillTypeAtomic.POST_FINAL_BILL_INWARD_PAYMENT_REFUND);

    /**
     * The original bill type of the selected report type plus its contra
     * (cancellation, refund, refund cancellation) bill types, so contra bills
     * are listed as their own signed rows (issue #23980).
     */
    private List<BillTypeAtomic> reportTypeBillTypeAtomics() {
        if ("DEPOSIT".equals(reportType)) {
            return DEPOSIT_BILL_TYPE_ATOMICS;
        } else if ("PAYMENT".equals(reportType)) {
            return PAYMENT_BILL_TYPE_ATOMICS;
        } else if ("POST_FINAL".equals(reportType)) {
            return POST_FINAL_BILL_TYPE_ATOMICS;
        }
        List<BillTypeAtomic> all = new ArrayList<>(DEPOSIT_BILL_TYPE_ATOMICS);
        all.addAll(PAYMENT_BILL_TYPE_ATOMICS);
        all.addAll(POST_FINAL_BILL_TYPE_ATOMICS);
        return all;
    }

    /**
     * Short label for the Bill Type column, so a negative contra row reads as
     * e.g. "Deposit Refund" or "Payment Cancellation".
     */
    private String billTypeLabel(BillTypeAtomic bta) {
        if (bta == null) {
            return "";
        }
        switch (bta) {
            case INWARD_DEPOSIT:
                return "Deposit";
            case INWARD_DEPOSIT_CANCELLATION:
                return "Deposit Cancellation";
            case INWARD_DEPOSIT_REFUND:
                return "Deposit Refund";
            case INWARD_DEPOSIT_REFUND_CANCELLATION:
                return "Deposit Refund Cancellation";
            case INWARD_PAYMENT:
                return "Payment";
            case INWARD_PAYMENT_CANCELLATION:
                return "Payment Cancellation";
            case INWARD_PAYMENT_REFUND:
                return "Payment Refund";
            case INWARD_PAYMENT_REFUND_CANCELLATION:
                return "Payment Refund Cancellation";
            case POST_FINAL_BILL_INWARD_PAYMENT:
                return "Post Discharge Payment";
            case POST_FINAL_BILL_INWARD_PAYMENT_CANCELLATION:
                return "Post Discharge Payment Cancellation";
            case POST_FINAL_BILL_INWARD_PAYMENT_REFUND:
                return "Post Discharge Payment Refund";
            default:
                return bta.getLabel();
        }
    }

    /**
     * Deliberately does NOT filter on {@code p.bill.cancelled}: cancelling a
     * bill sets {@code cancelled=true} on the original and records the
     * reversal as a separate negative contra bill, so filtering would drop the
     * original and leave the cancellation unbalanced (issue #23980).
     */
    private List<Payment> fetchDepositPayments(PatientEncounter enc) {
        StringBuilder jpql = new StringBuilder("select p from Payment p"
                + " where p.retired = false"
                + " and p.bill.retired = false"
                + " and p.bill.billTypeAtomic in :btas"
                + " and p.bill.patientEncounter = :enc");
        Map<String, Object> params = new HashMap<>();
        params.put("btas", reportTypeBillTypeAtomics());
        params.put("enc", enc);
        if (paymentMethod != null) {
            jpql.append(" and p.paymentMethod = :pm");
            params.put("pm", paymentMethod);
        }
        jpql.append(" order by p.createdAt");
        return paymentFacade.findByJpql(jpql.toString(), params);
    }

    public double getTotalForMethod(PaymentMethod pm) {
        return totalByMethod.getOrDefault(pm, 0.0);
    }

    /**
     * p:column exportFunction for the Credit Companies column - PrimeFaces'
     * exporter cannot resolve a nested ui:repeat's content on its own (it
     * falls back to the component's toString()), so this flattens the
     * current row's settlements into the same text shown on screen. Invoked
     * with the row variable ("row") still bound to the row being exported.
     */
    public String exportCreditCompanySettlements(UIColumn column) {
        Object rowValue = FacesContext.getCurrentInstance().getELContext()
                .getELResolver().getValue(FacesContext.getCurrentInstance().getELContext(), null, "row");
        if (!(rowValue instanceof BhtPaymentDetailDTO)) {
            return "";
        }
        List<CreditCompanySettlement> settlements = ((BhtPaymentDetailDTO) rowValue).getCreditCompanySettlements();
        if (settlements == null || settlements.isEmpty()) {
            return "-";
        }
        StringBuilder sb = new StringBuilder();
        for (CreditCompanySettlement s : settlements) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(s.getCreditCompanyName()).append(": Due ")
                    .append(String.format("%,.2f", s.getDue())).append(" / Paid ")
                    .append(String.format("%,.2f", s.getPaid())).append(" / Balance ")
                    .append(String.format("%,.2f", s.getBalance()));
        }
        return sb.toString();
    }

    public void makeNull() {
        fromDate = startOfCurrentMonth();
        toDate = new Date();
        dateBasis = "dischargeDate";
        reportType = "DEPOSIT";
        admissionStatus = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
        admissionType = null;
        paymentMethod = null;
        institution = null;
        site = null;
        department = null;
        reportRows = null;
        grandTotal = 0;
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

    public Date getFromDate() { return fromDate; }
    public void setFromDate(Date fromDate) { this.fromDate = fromDate; }

    public Date getToDate() { return toDate; }
    public void setToDate(Date toDate) { this.toDate = toDate; }

    public String getDateBasis() { return dateBasis; }
    public void setDateBasis(String dateBasis) { this.dateBasis = dateBasis; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

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
    public List<PaymentMethod> getUsedPaymentMethods() { return usedPaymentMethods; }
    public Map<PaymentMethod, Double> getTotalByMethod() { return totalByMethod; }
}
