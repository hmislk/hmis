package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.DepartmentController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.lab.ListingEntity;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.lab.PatientReport;
import com.divudi.core.entity.lab.PatientSample;
import com.divudi.core.facade.BillFacade;
import com.divudi.core.facade.PatientReportFacade;
import com.divudi.core.facade.PatientSampleFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Inward Laboratory Dashboard controller for inward nurses.
 *
 * Provides a simplified entry point to lab bill search, barcode generation,
 * sample management (collect, send, reject, re-generate) and report search for
 * in-patients. Business logic is delegated to {@link PatientInvestigationController}
 * wherever a suitable method exists; the bill search is implemented here so it can
 * be restricted to in-patient (IP) bills that have a patient investigation, and so
 * it can filter by bill number, BHT number and investigation.
 *
 * @author Dr. M. H. B. Ariyaratne
 */
@Named
@SessionScoped
public class InwardLaboratoryController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final String PATIENT_TYPE_IP = "IP";

    @Inject
    private PatientInvestigationController patientInvestigationController;
    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private DepartmentController departmentController;
    @Inject
    private AdmissionController admissionController;
    @EJB
    private BillFacade billFacade;
    @EJB
    private PatientSampleFacade patientSampleFacade;
    @EJB
    private PatientReportFacade patientReportFacade;

    private String billNumber;
    private String bhtNumber;

    /**
     * Opens the inward laboratory dashboard. Resets the underlying search state,
     * scopes the search to in-patients and lists the bills that are eligible for
     * barcode generation.
     */
    public String navigateToInwardLaboratoryDashboard() {
        patientInvestigationController.makeNull();
        billNumber = null;
        bhtNumber = null;
        boolean searchByOrderedInstitution = configOptionApplicationController.getBooleanValueByKey("For Lab Sample Barcode Generation, Search by Ordered Institution", false);
        if (searchByOrderedInstitution) {
            patientInvestigationController.setOrderedInstitution(sessionController.getInstitution());
        }
        boolean searchByOrderedDepartment = configOptionApplicationController.getBooleanValueByKey("For Lab Sample Barcode Generation, Search by Ordered Department", false);
        if (searchByOrderedDepartment) {
            patientInvestigationController.setOrderedDepartment(sessionController.getDepartment());
        }
        patientInvestigationController.setType(PATIENT_TYPE_IP);
        patientInvestigationController.listBillsToGenerateBarcodes();
        return "/inward/inward_lab_dashboard?faces-redirect=true";
    }

    /**
     * Returns to the inward laboratory dashboard from the inward patient report
     * print view, preserving the current search results/filters instead of
     * resetting them like {@link #navigateToInwardLaboratoryDashboard()} does.
     */
    public String navigateToBackFromPatientReportPrintView() {
        return "/inward/inward_lab_dashboard?faces-redirect=true";
    }

    /**
     * Searches in-patient lab bills. Only IP bills that have at least one patient
     * investigation are returned. Supports filtering by bill number, BHT number,
     * investigation name, patient name, status and the selected date range.
     */
    public void searchInwardLabBills() {
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT pi.billItem.bill ")
                .append("FROM PatientInvestigation pi ")
                .append("WHERE pi.billItem.bill.retired = :ret ")
                .append("AND pi.billItem.bill.ipOpOrCc = :type ")
                .append("AND pi.billItem.bill.createdAt BETWEEN :fd AND :td ");
        params.put("ret", false);
        params.put("type", PATIENT_TYPE_IP);
        params.put("fd", patientInvestigationController.getFromDate());
        params.put("td", patientInvestigationController.getToDate());

        if (billNumber != null && !billNumber.trim().isEmpty()) {
            jpql.append("AND pi.billItem.bill.deptId LIKE :billNo ");
            params.put("billNo", "%" + billNumber.trim() + "%");
        }

        if (bhtNumber != null && !bhtNumber.trim().isEmpty()) {
            jpql.append("AND pi.billItem.bill.patientEncounter.bhtNo LIKE :bht ");
            params.put("bht", "%" + bhtNumber.trim() + "%");
        }

        String investigationName = patientInvestigationController.getInvestigationName();
        if (investigationName != null && !investigationName.trim().isEmpty()) {
            jpql.append("AND pi.investigation.name LIKE :inv ");
            params.put("inv", "%" + investigationName.trim() + "%");
        }

        String patientName = patientInvestigationController.getPatientName();
        if (patientName != null && !patientName.trim().isEmpty()) {
            jpql.append("AND pi.billItem.bill.patient.person.name LIKE :pn ");
            params.put("pn", "%" + patientName.trim() + "%");
        }

        if (patientInvestigationController.getPatientInvestigationStatus() != null) {
            jpql.append("AND pi.billItem.bill.status = :status ");
            params.put("status", patientInvestigationController.getPatientInvestigationStatus());
        }

        if (patientInvestigationController.getPriority() != null) {
            jpql.append("AND pi.billItem.priority = :priority ");
            params.put("priority", patientInvestigationController.getPriority());
        }

        if (patientInvestigationController.getSpecimen() != null) {
            jpql.append("AND pi.investigation.sample = :specimen ");
            params.put("specimen", patientInvestigationController.getSpecimen());
        }

        if (patientInvestigationController.getOrderedInstitution() != null) {
            jpql.append("AND pi.billItem.bill.institution = :orderedInstitution ");
            params.put("orderedInstitution", patientInvestigationController.getOrderedInstitution());
        }

        if (patientInvestigationController.getOrderedDepartment() != null) {
            jpql.append("AND pi.billItem.bill.department = :orderedDepartment ");
            params.put("orderedDepartment", patientInvestigationController.getOrderedDepartment());
        }

        jpql.append("GROUP BY pi.billItem.bill ");
        jpql.append("ORDER BY pi.billItem.bill.id DESC");

        List<Bill> result = billFacade.findByJpql(jpql.toString(), params, TemporalType.TIMESTAMP);
        patientInvestigationController.setBills(result);
        patientInvestigationController.setListingEntity(ListingEntity.BILLS);
    }

    public void searchPatientSamples() {
        patientInvestigationController.setType(PATIENT_TYPE_IP);
        patientInvestigationController.searchPatientSamples();
    }

    public void searchPatientReports() {
        patientInvestigationController.setType(PATIENT_TYPE_IP);
        patientInvestigationController.searchPatientReports();
    }

    /**
     * Clears the Bill Number and BHT Number filters kept on this controller,
     * in addition to the filters cleared by {@link PatientInvestigationController#clearFilters()}.
     */
    public void clearFilters() {
        billNumber = null;
        bhtNumber = null;
        patientInvestigationController.clearFilters();
    }

    /**
     * Opens the inward laboratory dashboard directly on the Bills / Barcode
     * Generation view, scoped to the BHT currently selected on the Nursing
     * Work Bench. Requires a BHT to be selected first.
     */
    public String navigateToInwardLaboratoryBills() {
        Admission admission = getSelectedAdmissionOrShowError();
        if (admission == null) {
            return null;
        }
        listBillsForAdmission(admission);
        return "/inward/inward_lab_dashboard?faces-redirect=true";
    }

    /**
     * Opens the inward laboratory dashboard directly on the Samples view,
     * scoped to the BHT currently selected on the Nursing Work Bench.
     * Requires a BHT to be selected first.
     */
    public String navigateToInwardLaboratorySamples() {
        Admission admission = getSelectedAdmissionOrShowError();
        if (admission == null) {
            return null;
        }
        listSamplesForAdmission(admission);
        return "/inward/inward_lab_dashboard?faces-redirect=true";
    }

    /**
     * Opens the inward laboratory dashboard directly on the Reports view,
     * scoped to the BHT currently selected on the Nursing Work Bench.
     * Requires a BHT to be selected first.
     */
    public String navigateToInwardLaboratoryReports() {
        Admission admission = getSelectedAdmissionOrShowError();
        if (admission == null) {
            return null;
        }
        listReportsForAdmission(admission);
        return "/inward/inward_lab_dashboard?faces-redirect=true";
    }

    private Admission getSelectedAdmissionOrShowError() {
        Admission admission = admissionController.getCurrent();
        if (admission == null || admission.getId() == null || admission.getBhtNo() == null || admission.getBhtNo().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please select a BHT before navigating to the Laboratory Dashboard.");
            return null;
        }
        return admission;
    }

    private void listBillsForAdmission(Admission admission) {
        patientInvestigationController.makeNull();
        billNumber = null;
        bhtNumber = admission.getBhtNo();

        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT pi.billItem.bill "
                + "FROM PatientInvestigation pi "
                + "WHERE pi.billItem.bill.retired = :ret "
                + "AND pi.billItem.bill.patientEncounter.bhtNo = :bht "
                + "GROUP BY pi.billItem.bill "
                + "ORDER BY pi.billItem.bill.id DESC";
        params.put("ret", false);
        params.put("bht", admission.getBhtNo());

        List<Bill> result = billFacade.findByJpql(jpql, params);
        patientInvestigationController.setBills(result);
        patientInvestigationController.setListingEntity(ListingEntity.BILLS);
    }

    private void listSamplesForAdmission(Admission admission) {
        patientInvestigationController.makeNull();
        billNumber = null;
        bhtNumber = admission.getBhtNo();

        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT ps "
                + "FROM PatientSample ps "
                + "WHERE ps.retired = :ret "
                + "AND ps.bill.patientEncounter.bhtNo = :bht "
                + "ORDER BY ps.id DESC";
        params.put("ret", false);
        params.put("bht", admission.getBhtNo());

        List<PatientSample> result = patientSampleFacade.findByJpql(jpql, params);
        patientInvestigationController.setPatientSamples(result);
        patientInvestigationController.setListingEntity(ListingEntity.PATIENT_SAMPLES);
    }

    private void listReportsForAdmission(Admission admission) {
        patientInvestigationController.makeNull();
        billNumber = null;
        bhtNumber = admission.getBhtNo();

        Map<String, Object> params = new HashMap<>();
        String jpql = "SELECT r "
                + "FROM PatientReport r "
                + "WHERE r.retired = :ret "
                + "AND r.patientInvestigation.billItem.bill.patientEncounter.bhtNo = :bht "
                + "ORDER BY r.id DESC";
        params.put("ret", false);
        params.put("bht", admission.getBhtNo());

        List<PatientReport> result = patientReportFacade.findByJpql(jpql, params);
        patientInvestigationController.setPatientReports(result);
        patientInvestigationController.setListingEntity(ListingEntity.PATIENT_REPORTS);
    }

    public void collectSamples() {
        patientInvestigationController.collectSamples();
    }

    public void sendSamplesToLab() {
        patientInvestigationController.sendSamplesToLab(true);
    }

    public void rejectSamples() {
        patientInvestigationController.rejectSamples();
    }

    public void reGenerateSampleForRejectSamples() {
        patientInvestigationController.reGenerateSampleForRejectSamples();
    }

    /**
     * Returns the Inward-type departments of the currently selected Ordered
     * Institution, for populating the Ordered Department filter.
     */
    public List<Department> getInwardTypeDepartments() {
        return departmentController.getInstitutionDepatrments(patientInvestigationController.getOrderedInstitution(), false, DepartmentType.Inward);
    }

    public PatientInvestigationController getPatientInvestigationController() {
        return patientInvestigationController;
    }

    public String getBillNumber() {
        return billNumber;
    }

    public void setBillNumber(String billNumber) {
        this.billNumber = billNumber;
    }

    public String getBhtNumber() {
        return bhtNumber;
    }

    public void setBhtNumber(String bhtNumber) {
        this.bhtNumber = bhtNumber;
    }

}
