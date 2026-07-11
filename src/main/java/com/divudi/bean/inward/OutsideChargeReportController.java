package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.core.data.BillType;
import com.divudi.core.data.dto.OutsidePaymentReportDto;
import com.divudi.core.data.inward.InwardChargeType;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Doctor;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import javax.inject.Named;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.persistence.TemporalType;

/**
 *
 * @author Thisara Samuditha | github - thisarasamuditha |  thellamburavithanagethisarasam@gmail.com
 */
@Named(value = "outsideChargeReportController")
@SessionScoped
public class OutsideChargeReportController implements Serializable {

    @EJB
    private BillItemFacade billItemFacade;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;

    private Date fromDate;
    private Date toDate;
    private Date dischargeFromDate;
    private Date dischargeToDate;
    private Date invoiceApprovedFromDate;
    private Date invoiceApprovedToDate;
    private Institution institution;
    private Institution site;
    private Department department;
    private Doctor referringDoctor;
    private Institution creditCompany;
    private List<AdmissionType> admissionType;

    /** "Paid", "Not Paid", or null/"" for All. Bound to the Paid Type filter. */
    private String paidType;

    private List<OutsidePaymentReportDto> reportRows;

    public OutsideChargeReportController() {
    }
 
    public void processOutsidePaymentReport() {
        reportRows = null;

        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select both Admission From and To dates.");
            return;
        }
        if (fromDate.after(toDate)) {
            JsfUtil.addErrorMessage("Admission From date must not be after Admission To date.");
            return;
        }
        if (dischargeFromDate != null && dischargeToDate != null && dischargeFromDate.after(dischargeToDate)) {
            JsfUtil.addErrorMessage("Discharge From date must not be after Discharge To date.");
            return;
        }
        if (invoiceApprovedFromDate != null && invoiceApprovedToDate != null
                && invoiceApprovedFromDate.after(invoiceApprovedToDate)) {
            JsfUtil.addErrorMessage("Invoice Approved From date must not be after Invoice Approved To date.");
            return;
        }

        StringBuilder jpql = new StringBuilder(
                "SELECT new com.divudi.core.data.dto.OutsidePaymentReportDto("
                + "bi.id, "
                + "b.id, "
                + "COALESCE(b.deptId, ''), "
                + "COALESCE(enc.patient.phn, ''), "
                + "COALESCE(enc.patient.person.name, ''), "
                + "COALESCE(enc.bhtNo, ''), "
                + "COALESCE(item.name, ''), "
                + "enc.dateOfDischarge, "
                + "b.cancelled, "
                + "b.refunded, "
                + "bi.inwardChargeType, "
                + "COALESCE(createrPerson.name, ''), "
                + "bi.createdAt, "
                + "bi.netValue, "
                + "b.paidAt, "
                + "b.paidAmount, "
                + "b.netTotal, "
                + "COALESCE(bi.descreption, ''), "
                + "b.paid) "
                + "FROM BillItem bi "
                + "JOIN bi.bill b "
                + "JOIN b.patientEncounter enc "
                + "LEFT JOIN bi.item item "
                + "LEFT JOIN bi.creater creater "
                + "LEFT JOIN creater.webUserPerson createrPerson "
                + "WHERE bi.retired = false "
                + "AND b.retired = false "
                + "AND b.billType = :bt "
                + "AND enc.dateOfAdmission BETWEEN :fd AND :td ");

        Map<String, Object> params = new HashMap<>();
        params.put("bt", BillType.InwardOutSideBill);
        params.put("fd", fromDate);
        params.put("td", toDate);

        if (dischargeFromDate != null && dischargeToDate != null) {
            jpql.append("AND enc.dateOfDischarge BETWEEN :dfd AND :dtd ");
            params.put("dfd", dischargeFromDate);
            params.put("dtd", dischargeToDate);
        }

        if (invoiceApprovedFromDate != null && invoiceApprovedToDate != null) {
            jpql.append("AND b.createdAt BETWEEN :iafd AND :iatd ");
            params.put("iafd", invoiceApprovedFromDate);
            params.put("iatd", invoiceApprovedToDate);
        }

        if (institution != null) {
            jpql.append("AND b.institution = :ins ");
            params.put("ins", institution);
        }

        if (site != null) {
            jpql.append("AND b.department.site = :site ");
            params.put("site", site);
        }

        if (department != null) {
            jpql.append("AND b.department = :dept ");
            params.put("dept", department);
        }

        if (referringDoctor != null) {
            jpql.append("AND enc.referringDoctor = :doc ");
            params.put("doc", referringDoctor);
        }

        if (creditCompany != null) {
            jpql.append("AND b.creditCompany = :cc ");
            params.put("cc", creditCompany);
        }

        if (admissionType != null && !admissionType.isEmpty()) {
            jpql.append("AND enc.admissionType IN :ats ");
            params.put("ats", admissionType);
        }

        if ("Paid".equals(paidType)) {
            jpql.append("AND b.paid = true ");
        } else if ("Not Paid".equals(paidType)) {
            jpql.append("AND b.paid = false ");
        }

        jpql.append("ORDER BY enc.bhtNo, bi.createdAt");

        List<OutsidePaymentReportDto> result = (List<OutsidePaymentReportDto>) billItemFacade.findLightsByJpql(
                jpql.toString(), params, TemporalType.TIMESTAMP);
        reportRows = result != null ? result : new ArrayList<>();

        if (reportRows.isEmpty()) {
            JsfUtil.addErrorMessage("No records found for the selected criteria.");
        }
    }

    public String getChargeTypeLabel(InwardChargeType type) {
        if (type == null) {
            return "";
        }
        return configOptionApplicationController.getInwardChargeTypeLabel(type);
    }

    // getters and setters
    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay(new Date());
        }
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public Date getDischargeFromDate() {
        return dischargeFromDate;
    }

    public void setDischargeFromDate(Date dischargeFromDate) {
        this.dischargeFromDate = dischargeFromDate;
    }

    public Date getDischargeToDate() {
        return dischargeToDate;
    }

    public void setDischargeToDate(Date dischargeToDate) {
        this.dischargeToDate = dischargeToDate;
    }

    public Date getInvoiceApprovedFromDate() {
        return invoiceApprovedFromDate;
    }

    public void setInvoiceApprovedFromDate(Date InvoiceApprovedFromDate) {
        this.invoiceApprovedFromDate = InvoiceApprovedFromDate;
    }

    public Date getInvoiceApprovedToDate() {
        return invoiceApprovedToDate;
    }

    public void setInvoiceApprovedToDate(Date InvoiceApprovedToDate) {
        this.invoiceApprovedToDate = InvoiceApprovedToDate;
    }

    public Institution getInstitution() {
        return institution;
    }

    public void setInstitution(Institution institution) {
        this.institution = institution;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Doctor getReferringDoctor() {
        return referringDoctor;
    }

    public void setReferringDoctor(Doctor referringDoctor) {
        this.referringDoctor = referringDoctor;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public List<AdmissionType> getAdmissionType() {
        return admissionType;
    }

    public void setAdmissionType(List<AdmissionType> admissionType) {
        this.admissionType = admissionType;
    }

    public String getPaidType() {
        return paidType;
    }

    public void setPaidType(String paidType) {
        this.paidType = paidType;
    }

    public List<OutsidePaymentReportDto> getReportRows() {
        return reportRows;
    }
}
