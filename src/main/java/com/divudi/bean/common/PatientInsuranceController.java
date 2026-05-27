package com.divudi.bean.common;

import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.PatientInsurance;
import com.divudi.core.facade.PatientInsuranceFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Manages PatientInsurance profiles for a patient.
 * Used on the patient profile and registration pages.
 *
 * @author Dr M H B Ariyaratne <buddhika.ari at gmail.com>
 */
@Named
@SessionScoped
public class PatientInsuranceController implements Serializable {

    @EJB
    private PatientInsuranceFacade patientInsuranceFacade;

    @Inject
    private SessionController sessionController;

    private Patient patient;
    private List<PatientInsurance> insuranceProfiles;
    private PatientInsurance newInsurance;

    public void initForPatient(Patient p) {
        this.patient = p;
        newInsurance = new PatientInsurance();
        loadProfiles();
    }

    private void loadProfiles() {
        if (patient == null || patient.getId() == null) {
            insuranceProfiles = new ArrayList<>();
            return;
        }
        String jpql = "select pi from PatientInsurance pi "
                + "where pi.patient = :pt "
                + "and pi.retired = false "
                + "order by pi.primary desc, pi.id desc";
        HashMap<String, Object> params = new HashMap<>();
        params.put("pt", patient);
        insuranceProfiles = patientInsuranceFacade.findByJpql(jpql, params);
    }

    public void addInsurance() {
        if (patient == null) {
            JsfUtil.addErrorMessage("No patient selected");
            return;
        }
        if (newInsurance.getCreditCompany() == null) {
            JsfUtil.addErrorMessage("Please select a credit company / insurer");
            return;
        }
        newInsurance.setPatient(patient);
        newInsurance.setActive(true);
        newInsurance.setCreatedAt(new Date());
        newInsurance.setCreater(sessionController.getLoggedUser());
        patientInsuranceFacade.create(newInsurance);
        newInsurance = new PatientInsurance();
        loadProfiles();
        JsfUtil.addSuccessMessage("Insurance profile added");
    }

    public void saveInsurance(PatientInsurance pi) {
        pi.setEditedAt(new Date());
        pi.setEditer(sessionController.getLoggedUser());
        patientInsuranceFacade.edit(pi);
        JsfUtil.addSuccessMessage("Insurance profile saved");
    }

    public void deactivateInsurance(PatientInsurance pi) {
        pi.setActive(false);
        pi.setEditedAt(new Date());
        pi.setEditer(sessionController.getLoggedUser());
        patientInsuranceFacade.edit(pi);
        loadProfiles();
        JsfUtil.addSuccessMessage("Insurance profile deactivated");
    }

    public void retireInsurance(PatientInsurance pi) {
        pi.setRetired(true);
        pi.setRetiredAt(new Date());
        pi.setRetirer(sessionController.getLoggedUser());
        patientInsuranceFacade.edit(pi);
        loadProfiles();
    }

    /**
     * Returns active, non-retired profiles for the given patient.
     * Used by AdmissionController to auto-populate credit companies.
     */
    public List<PatientInsurance> findActiveProfiles(Patient p) {
        if (p == null || p.getId() == null) {
            return new ArrayList<>();
        }
        String jpql = "select pi from PatientInsurance pi "
                + "where pi.patient = :pt "
                + "and pi.retired = false "
                + "and pi.active = true "
                + "order by pi.primary desc, pi.id desc";
        HashMap<String, Object> params = new HashMap<>();
        params.put("pt", p);
        return patientInsuranceFacade.findByJpql(jpql, params);
    }

    /**
     * Upserts a PatientInsurance record from an EncounterCreditCompany snapshot.
     * Matches on patient + institution. Creates a new profile if none exists.
     */
    public void upsertFromEncounter(Patient p, Institution institution,
            String policyNo, String referenceNo, double creditLimit) {
        if (p == null || institution == null) {
            return;
        }
        String jpql = "select pi from PatientInsurance pi "
                + "where pi.patient = :pt "
                + "and pi.creditCompany = :cc "
                + "and pi.retired = false";
        HashMap<String, Object> params = new HashMap<>();
        params.put("pt", p);
        params.put("cc", institution);
        PatientInsurance existing = patientInsuranceFacade.findFirstByJpql(jpql, params);
        if (existing != null) {
            existing.setPolicyNo(policyNo);
            existing.setReferenceNo(referenceNo);
            existing.setCreditLimit(creditLimit);
            existing.setEditedAt(new Date());
            existing.setEditer(sessionController.getLoggedUser());
            patientInsuranceFacade.edit(existing);
        } else {
            PatientInsurance pi = new PatientInsurance();
            pi.setPatient(p);
            pi.setCreditCompany(institution);
            pi.setPolicyNo(policyNo);
            pi.setReferenceNo(referenceNo);
            pi.setCreditLimit(creditLimit);
            pi.setActive(true);
            pi.setCreatedAt(new Date());
            pi.setCreater(sessionController.getLoggedUser());
            patientInsuranceFacade.create(pi);
        }
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public List<PatientInsurance> getInsuranceProfiles() {
        return insuranceProfiles;
    }

    public void setInsuranceProfiles(List<PatientInsurance> insuranceProfiles) {
        this.insuranceProfiles = insuranceProfiles;
    }

    public PatientInsurance getNewInsurance() {
        if (newInsurance == null) {
            newInsurance = new PatientInsurance();
        }
        return newInsurance;
    }

    public void setNewInsurance(PatientInsurance newInsurance) {
        this.newInsurance = newInsurance;
    }
}
