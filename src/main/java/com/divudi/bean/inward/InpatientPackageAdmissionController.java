package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.InpatientPackage;
import com.divudi.core.entity.inward.PatientRoom;
import com.divudi.core.facade.InpatientPackageFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@SessionScoped
public class InpatientPackageAdmissionController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private SessionController sessionController;
    @Inject
    private AdmissionController admissionController;

    @EJB
    private PatientFacade patientFacade;
    @EJB
    private InpatientPackageFacade inpatientPackageFacade;

    private Patient patient;
    private InpatientPackage inpatientPackage;

    public List<Patient> completePatient(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        Map<String, Object> m = new HashMap<>();
        m.put("name", "%" + query.toUpperCase() + "%");
        String jpql = "SELECT p FROM Patient p"
                + " WHERE p.retired = false"
                + " AND UPPER(p.person.name) LIKE :name"
                + " ORDER BY p.person.name";
        return patientFacade.findByJpql(jpql, m, 15);
    }

    public List<InpatientPackage> completeInpatientPackage(String query) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", "%" + (query == null ? "" : query.toUpperCase()) + "%");
        String jpql = "SELECT p FROM InpatientPackage p"
                + " WHERE p.retired = false"
                + " AND UPPER(p.name) LIKE :name"
                + " ORDER BY p.name";
        return inpatientPackageFacade.findByJpql(jpql, m, 15);
    }

    public String navigatePackageAdmit() {
        if (patient == null || patient.getId() == null) {
            JsfUtil.addErrorMessage("Please select a Patient");
            return "";
        }
        if (inpatientPackage == null || inpatientPackage.getId() == null) {
            JsfUtil.addErrorMessage("Please select an Inpatient Package");
            return "";
        }

        Admission ad = new Admission();
        ad.setDateOfAdmission(CommonFunctions.getCurrentDateTime());
        ad.setPatient(patient);
        ad.setAdmissionType(inpatientPackage.getAdmissionType());
        ad.setInpatientPackage(inpatientPackage);

        admissionController.setCurrent(ad);
        admissionController.setPrintPreview(false);
        admissionController.setAdmittingProcessStarted(false);
        admissionController.setPatientRoom(new PatientRoom());
        admissionController.setPatientAllergies(null);
        admissionController.setCurrentReservation(null);
        admissionController.setBhtText("");

        patient = null;
        inpatientPackage = null;

        return "/inward/inward_admission?faces-redirect=true";
    }

    public String navigateToPackageAdmitFromMenu() {
        patient = null;
        inpatientPackage = null;
        return "/inward/package_admit?faces-redirect=true";
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public InpatientPackage getInpatientPackage() {
        return inpatientPackage;
    }

    public void setInpatientPackage(InpatientPackage inpatientPackage) {
        this.inpatientPackage = inpatientPackage;
    }
}
