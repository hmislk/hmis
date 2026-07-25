/*
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.core.data.BillType;
import com.divudi.core.data.dto.InwardIntrimBillSearchDTO;
import com.divudi.core.data.inward.AdmissionStatus;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.BillService;
import java.io.Serializable;
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
 * Search + view controller for inward interim bills (BillType.InwardIntrimBill),
 * DTO-driven (issue #1009). Replaces SearchController#createInwardIntrimBills(),
 * whose "Total"/"Net Total" filters were broken (Total was never read; Net Total
 * put a wildcarded String into a double-typed JPQL parameter) and which returned
 * full entity graphs instead of a lightweight DTO.
 *
 * Every InwardIntrimBill is tied 1:1 to an Admission/PatientEncounter via
 * {@code Bill.patientEncounter}, so the filters here mirror
 * {@link AdmissionController#searchAdmissions()} / inward/inpatient_search.xhtml
 * instead of reintroducing a Total/Net Total search on a running (in-progress)
 * total that is not meaningful to exact-match.
 *
 * @author Buddhika
 */
@Named
@SessionScoped
public class InwardIntrimBillSearchController implements Serializable {

    @EJB
    private BillFacade billFacade;
    @EJB
    private BillService billService;

    @Inject
    private InwardSearch inwardSearch;

    // <editor-fold defaultstate="collapsed" desc="Search Filters">
    private Date fromDate;
    private Date toDate;
    private String billNo;
    private String bhtNo;
    private String patientName;
    private String mrnOrPhn;
    private String patientPhone;
    private String patientIdentityNumber;
    private Staff referringDoctor;
    private String occupation;
    private Institution creditCompany;
    private AdmissionStatus admissionStatus;
    private AdmissionType admissionType;
    private Institution institution;
    private Department department;
    /**
     * UI-only narrowing for the Department dropdown (mirrors
     * AdmissionController#site) - not used as a JPQL predicate here.
     */
    private Institution site;
    // </editor-fold>

    private List<InwardIntrimBillSearchDTO> results;
    private Long billId;

    public void search() {
        if (fromDate == null || toDate == null) {
            JsfUtil.addErrorMessage("Please select date");
            return;
        }

        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT NEW com.divudi.core.data.dto.InwardIntrimBillSearchDTO("
                + "b.id, b.deptId, pe.bhtNo, b.createdAt, b.cancelled, cb.createdAt, cbcp.name, "
                + "bcrp.name, per.name, b.paymentMethod, b.total, cb.comments, rb.comments) "
                + "FROM BilledBill b "
                + "LEFT JOIN b.patientEncounter pe "
                + "LEFT JOIN pe.patient p "
                + "LEFT JOIN p.person per "
                + "LEFT JOIN per.occupation occ "
                + "LEFT JOIN b.creater bcr "
                + "LEFT JOIN bcr.webUserPerson bcrp "
                + "LEFT JOIN b.cancelledBill cb "
                + "LEFT JOIN cb.creater cbc "
                + "LEFT JOIN cbc.webUserPerson cbcp "
                + "LEFT JOIN b.refundedBill rb "
                + "WHERE b.billType = :billType AND b.retired = false "
                + "AND b.createdAt BETWEEN :fromDate AND :toDate ";
        params.put("billType", BillType.InwardIntrimBill);
        params.put("fromDate", fromDate);
        params.put("toDate", toDate);

        if (billNo != null && !billNo.trim().isEmpty()) {
            jpql += " AND b.deptId like :billNo ";
            params.put("billNo", "%" + billNo.trim().toUpperCase() + "%");
        }

        if (bhtNo != null && !bhtNo.trim().isEmpty()) {
            jpql += " AND pe.bhtNo like :bhtNo ";
            params.put("bhtNo", "%" + bhtNo.trim().toUpperCase() + "%");
        }

        if (patientName != null && !patientName.trim().isEmpty()) {
            jpql += " AND per.name like :patientName ";
            params.put("patientName", "%" + patientName.trim().toUpperCase() + "%");
        }

        if (mrnOrPhn != null && !mrnOrPhn.trim().isEmpty()) {
            jpql += " AND (p.code = :mrnOrPhn OR p.phn = :mrnOrPhn) ";
            params.put("mrnOrPhn", mrnOrPhn.trim().toUpperCase());
        }

        if (patientPhone != null && !patientPhone.trim().isEmpty()) {
            jpql += " AND per.phone like :patientPhone ";
            params.put("patientPhone", "%" + patientPhone.trim().toUpperCase() + "%");
        }

        if (patientIdentityNumber != null && !patientIdentityNumber.trim().isEmpty()) {
            jpql += " AND per.nic = :nic ";
            params.put("nic", patientIdentityNumber.trim());
        }

        if (referringDoctor != null) {
            jpql += " AND pe.referringDoctor = :refDoc ";
            params.put("refDoc", referringDoctor);
        }

        if (occupation != null && !occupation.trim().isEmpty()) {
            jpql += " AND occ.name like :occ ";
            params.put("occ", "%" + occupation.trim().toUpperCase() + "%");
        }

        if (creditCompany != null) {
            jpql += " AND pe.creditCompany = :cc ";
            params.put("cc", creditCompany);
        }

        if (admissionStatus != null) {
            switch (admissionStatus) {
                case ADMITTED_BUT_NOT_DISCHARGED:
                    jpql += " AND pe.discharged = :dis ";
                    params.put("dis", false);
                    break;
                case DISCHARGED_BUT_FINAL_BILL_NOT_COMPLETED:
                    jpql += " AND pe.discharged = :dis AND pe.paymentFinalized = :bf ";
                    params.put("dis", true);
                    params.put("bf", false);
                    break;
                case DISCHARGED_AND_FINAL_BILL_COMPLETED:
                    jpql += " AND pe.discharged = :dis AND pe.paymentFinalized = :bf ";
                    params.put("dis", true);
                    params.put("bf", true);
                    break;
                case ANY_STATUS:
                default:
                    break;
            }
        }

        if (institution != null) {
            jpql += " AND pe.institution = :ins ";
            params.put("ins", institution);
        }

        if (department != null) {
            jpql += " AND pe.department = :dept ";
            params.put("dept", department);
        }

        if (admissionType != null) {
            jpql += " AND pe.admissionType = :at ";
            params.put("at", admissionType);
        }

        jpql += " order by b.createdAt desc ";

        results = (List<InwardIntrimBillSearchDTO>) billFacade.findLightsByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    public String viewInterimBill() {
        if (billId == null) {
            JsfUtil.addErrorMessage("No Bill Selected");
            return null;
        }
        Bill b = billService.reloadBill(billId);
        if (b == null) {
            JsfUtil.addErrorMessage("Bill not found");
            return null;
        }
        inwardSearch.setBill(b);
        return "/inward/inward_reprint_bill_intrim?faces-redirect=true";
    }

    // <editor-fold defaultstate="collapsed" desc="Getters and Setters">
    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public String getBillNo() {
        return billNo;
    }

    public void setBillNo(String billNo) {
        this.billNo = billNo;
    }

    public String getBhtNo() {
        return bhtNo;
    }

    public void setBhtNo(String bhtNo) {
        this.bhtNo = bhtNo;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getMrnOrPhn() {
        return mrnOrPhn;
    }

    public void setMrnOrPhn(String mrnOrPhn) {
        this.mrnOrPhn = mrnOrPhn;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public String getPatientIdentityNumber() {
        return patientIdentityNumber;
    }

    public void setPatientIdentityNumber(String patientIdentityNumber) {
        this.patientIdentityNumber = patientIdentityNumber;
    }

    public Staff getReferringDoctor() {
        return referringDoctor;
    }

    public void setReferringDoctor(Staff referringDoctor) {
        this.referringDoctor = referringDoctor;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public Institution getCreditCompany() {
        return creditCompany;
    }

    public void setCreditCompany(Institution creditCompany) {
        this.creditCompany = creditCompany;
    }

    public AdmissionStatus getAdmissionStatus() {
        return admissionStatus;
    }

    public void setAdmissionStatus(AdmissionStatus admissionStatus) {
        this.admissionStatus = admissionStatus;
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

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public Institution getSite() {
        return site;
    }

    public void setSite(Institution site) {
        this.site = site;
    }

    public List<InwardIntrimBillSearchDTO> getResults() {
        return results;
    }

    public void setResults(List<InwardIntrimBillSearchDTO> results) {
        this.results = results;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }
    // </editor-fold>
}
