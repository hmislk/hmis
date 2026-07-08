package com.divudi.bean.inward;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.DepartmentController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.lab.PatientInvestigationController;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.lab.ListingEntity;
import com.divudi.core.entity.Bill;
import com.divudi.core.entity.Department;
import com.divudi.core.facade.BillFacade;
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

    @Inject
    private PatientInvestigationController patientInvestigationController;
    @Inject
    private SessionController sessionController;
    @Inject
    private ConfigOptionApplicationController configOptionApplicationController;
    @Inject
    private DepartmentController departmentController;
    @EJB
    private BillFacade billFacade;

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
        patientInvestigationController.setType("IP");
        patientInvestigationController.listBillsToGenerateBarcodes();
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
        params.put("type", "IP");
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
        patientInvestigationController.setType("IP");
        patientInvestigationController.searchPatientSamples();
    }

    public void searchPatientReports() {
        patientInvestigationController.setType("IP");
        patientInvestigationController.searchPatientReports();
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
