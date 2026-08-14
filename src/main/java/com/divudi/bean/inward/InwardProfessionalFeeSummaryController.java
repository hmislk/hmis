package com.divudi.bean.inward;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.InwardProfessionalFeeSummaryRowDto;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.service.inward.InwardBhtChargeAggregationService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

/**
 * Controller for the Professional &amp; Other Fee Summary report (2026-08-15).
 *
 * One row per PatientEncounter. Professional Fee Total = InwardChargeType.ProfessionalCharge
 * (Consultant fees) only, net of professional-bill discount/service charge.
 * Other Fee Total = every other InwardChargeType, net of non-professional-bill
 * discount/service charge. Net Total = Professional Fee Total + Other Fee Total —
 * always reconciles since each bucket is independently netted.
 *
 * Reuses InwardBhtChargeAggregationService for encounter search and charge
 * aggregation (shared with InwardInvoiceJournalController) so the bulk-query
 * JPQL lives in exactly one place.
 */
@Named
@SessionScoped
public class InwardProfessionalFeeSummaryController implements Serializable {

    @EJB
    private InwardBhtChargeAggregationService chargeAggregationService;

    // -------------------------------------------------------------------------
    // Filter fields
    // -------------------------------------------------------------------------
    private Date fromDate = startOfCurrentMonth();
    private Date toDate   = new Date();

    /** "admissionDate" or "dischargeDate" */
    private String dateBasis = "dischargeDate";

    private AdmissionStatus admissionStatus = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
    private AdmissionType   admissionType;
    private PaymentMethod   paymentMethod;
    private Institution     institution;
    private Institution     site;
    private Department      department;

    // -------------------------------------------------------------------------
    // Report output
    // -------------------------------------------------------------------------
    private List<InwardProfessionalFeeSummaryRowDto> reportRows;

    private double grandTotalProfessionalFee;
    private double grandTotalOtherFee;
    private double grandTotalNet;

    // -------------------------------------------------------------------------
    // Main generate method
    // -------------------------------------------------------------------------

    public void generateReport() {
        reportRows                = new ArrayList<>();
        grandTotalProfessionalFee = 0;
        grandTotalOtherFee        = 0;
        grandTotalNet             = 0;

        List<PatientEncounter> encounters = chargeAggregationService.fetchEncounters(buildEncounterFilter());
        if (encounters == null || encounters.isEmpty()) {
            return;
        }

        Map<Long, Map<InwardChargeType, Double>> chargeMap =
                chargeAggregationService.fetchChargesByEncounter(encounters);
        Map<Long, double[]> splitDiscountMarginMap =
                chargeAggregationService.fetchDiscountAndMarginSplitByProfessional(encounters);

        for (PatientEncounter enc : encounters) {
            InwardProfessionalFeeSummaryRowDto row = buildRow(enc, chargeMap, splitDiscountMarginMap);
            reportRows.add(row);

            grandTotalProfessionalFee += row.getProfessionalFeeTotal();
            grandTotalOtherFee        += row.getOtherFeeTotal();
            grandTotalNet             += row.getNetTotal();
        }
    }

    private InwardBhtChargeAggregationService.EncounterFilter buildEncounterFilter() {
        InwardBhtChargeAggregationService.EncounterFilter filter =
                new InwardBhtChargeAggregationService.EncounterFilter();
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        filter.setDateBasis(dateBasis);
        filter.setAdmissionStatus(admissionStatus);
        filter.setAdmissionType(admissionType);
        filter.setPaymentMethod(paymentMethod);
        filter.setInstitution(institution);
        filter.setSite(site);
        filter.setDepartment(department);
        return filter;
    }

    private InwardProfessionalFeeSummaryRowDto buildRow(
            PatientEncounter enc,
            Map<Long, Map<InwardChargeType, Double>> chargeMap,
            Map<Long, double[]> splitDiscountMarginMap) {

        InwardProfessionalFeeSummaryRowDto row = new InwardProfessionalFeeSummaryRowDto();
        row.setEncounterDatabaseId(enc.getId());
        row.setBhtNo(enc.getBhtNo());
        row.setPatientName(enc.getPatient() != null && enc.getPatient().getPerson() != null
                ? enc.getPatient().getPerson().getNameWithTitle() : "");
        row.setDateOfAdmission(enc.getDateOfAdmission());
        row.setDateOfDischarge(enc.getDateOfDischarge());
        row.setAdmissionType(enc.getAdmissionType());

        if (enc.getFinalBill() != null) {
            row.setFinalBillNo(enc.getFinalBill().getIdStr());
        }

        Map<InwardChargeType, Double> charges = chargeMap.get(enc.getId());
        double professionalGross = 0;
        double otherGross = 0;
        if (charges != null) {
            for (Map.Entry<InwardChargeType, Double> e : charges.entrySet()) {
                double value = e.getValue() == null ? 0.0 : e.getValue();
                if (e.getKey() == InwardChargeType.ProfessionalCharge) {
                    professionalGross += value;
                } else {
                    otherGross += value;
                }
            }
        }

        double[] splitDm = splitDiscountMarginMap.get(enc.getId());
        double professionalDiscount = splitDm != null ? splitDm[0] : 0.0;
        double professionalMargin   = splitDm != null ? splitDm[1] : 0.0;
        double otherDiscount        = splitDm != null ? splitDm[2] : 0.0;
        double otherMargin          = splitDm != null ? splitDm[3] : 0.0;

        row.setProfessionalFeeTotal(professionalGross - professionalDiscount + professionalMargin);
        row.setOtherFeeTotal(otherGross - otherDiscount + otherMargin);

        return row;
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    public void onInstitutionChange() {
        site       = null;
        department = null;
    }

    public void onSiteChange() {
        department = null;
    }

    public String navigateToReport() {
        makeNull();
        return "/inward/inward_report_professional_fee_summary?faces-redirect=true";
    }

    public void makeNull() {
        fromDate        = startOfCurrentMonth();
        toDate          = new Date();
        dateBasis       = "dischargeDate";
        admissionStatus = AdmissionStatus.DISCHARGED_AND_FINAL_BILL_COMPLETED;
        admissionType   = null;
        paymentMethod   = null;
        institution     = null;
        site            = null;
        department      = null;
        reportRows      = null;
        grandTotalProfessionalFee = 0;
        grandTotalOtherFee        = 0;
        grandTotalNet             = 0;
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

    // -------------------------------------------------------------------------
    // Getters / setters
    // -------------------------------------------------------------------------

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

    public List<InwardProfessionalFeeSummaryRowDto> getReportRows() { return reportRows; }

    public double getGrandTotalProfessionalFee() { return grandTotalProfessionalFee; }
    public double getGrandTotalOtherFee() { return grandTotalOtherFee; }
    public double getGrandTotalNet() { return grandTotalNet; }
}
