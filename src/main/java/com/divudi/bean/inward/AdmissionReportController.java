package com.divudi.bean.inward;

import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.dto.AdmissionReportDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.AdmissionFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Controller for the enhanced Admission Report page (Issue #19640).
 *
 * Replaces the entity-based query in MdInwardReportController.fillAdmissions()
 * with a DTO-based JPQL SELECT NEW approach so that all columns are populated
 * in one query and there are no lazy-load round-trips during rendering.
 */
@Named
@SessionScoped
public class AdmissionReportController implements Serializable {

    @EJB
    private AdmissionFacade admissionFacade;

    // -------------------------------------------------------------------------
    // Filter fields
    // -------------------------------------------------------------------------
    private Date fromDate = startOfCurrentMonth();
    private Date toDate = new Date();

    /**
     * "createdAt" (default) or "admissionDate"
     */
    private String dateBasis = "createdAt";

    private AdmissionType admissionType;
    private PaymentMethod paymentMethod;
    private Institution institution;
    private Institution site;
    private Department department;

    // -------------------------------------------------------------------------
    // Report output
    // -------------------------------------------------------------------------
    private List<AdmissionReportDTO> admissions;

    // -------------------------------------------------------------------------
    // Main generate method
    // -------------------------------------------------------------------------

    public void fillAdmissions() {
        Map<String, Object> params = new HashMap<>();

        StringBuilder jpql = new StringBuilder(
                "select new com.divudi.core.data.dto.AdmissionReportDTO("
                + "ad.id,"
                + "ad.bhtNo,"
                + "ad.patient.person.nameWithTitle,"
                + "ad.patient.person.mobile,"
                + "ad.patient.person.address,"
                + "ad.dateOfAdmission,"
                + "ad.dateOfDischarge,"
                + "ad.createdAt,"
                + "ad.paymentMethod,"
                + "ad.creditCompany,"
                + "ad.referringConsultant,"
                + "ad.opdDoctor,"
                + "rfc.name,"
                + "ad.discharged,"
                + "ad.paymentFinalized,"
                + "ad.admissionType"
                + ") from Admission ad"
                + " left join ad.currentPatientRoom pr"
                + " left join pr.roomFacilityCharge rfc"
                + " where ad.retired = false");

        // --- date basis ---
        if (fromDate != null && toDate != null) {
            if ("admissionDate".equals(dateBasis)) {
                jpql.append(" and ad.dateOfAdmission between :fromDate and :toDate");
            } else {
                jpql.append(" and ad.createdAt between :fromDate and :toDate");
            }
            params.put("fromDate", fromDate);
            params.put("toDate", toDate);
        }

        // --- admission type ---
        if (admissionType != null) {
            jpql.append(" and ad.admissionType = :admType");
            params.put("admType", admissionType);
        }

        // --- payment method ---
        if (paymentMethod != null) {
            jpql.append(" and ad.paymentMethod = :pm");
            params.put("pm", paymentMethod);
        }

        // --- institution ---
        if (institution != null) {
            jpql.append(" and ad.institution = :ins");
            params.put("ins", institution);
        }

        // --- site ---
        if (site != null) {
            jpql.append(" and ad.department.site = :site");
            params.put("site", site);
        }

        // --- department ---
        if (department != null) {
            jpql.append(" and ad.department = :dept");
            params.put("dept", department);
        }

        jpql.append(" order by ad.createdAt asc");

        List<Object> results = admissionFacade.findObjectByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        admissions = new ArrayList<>();
        for (Object o : results) {
            admissions.add((AdmissionReportDTO) o);
        }
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    /** Called when Institution or Site selection changes to prevent stale department. */
    public void clearDepartment() {
        department = null;
    }

    // -------------------------------------------------------------------------
    // Reset
    // -------------------------------------------------------------------------

    public void makeNull() {
        fromDate = startOfCurrentMonth();
        toDate = new Date();
        dateBasis = "createdAt";
        admissionType = null;
        paymentMethod = null;
        institution = null;
        site = null;
        department = null;
        admissions = null;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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

    public List<AdmissionReportDTO> getAdmissions() { return admissions; }
}
