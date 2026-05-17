/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.common;

import com.divudi.core.data.clinical.PersonRelationship;
import com.divudi.core.data.hr.Relationship;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PersonRelationshipFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Manages PersonRelationship records — next-of-kin, guardian, dependent, etc.
 * — for a given patient on the patient profile page.
 */
@Named
@SessionScoped
public class PersonRelationshipController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PersonRelationshipFacade ejbFacade;
    @EJB
    private PatientFacade patientFacade;
    @Inject
    SessionController sessionController;

    private PersonRelationship current;
    private List<PersonRelationship> patientRelationships;
    private Patient currentPatient;
    private Patient relatedPatient;

    public PersonRelationshipController() {
    }

    public void loadRelationshipsForPatient(Patient p) {
        if (p == null || p.getPerson() == null) {
            patientRelationships = new ArrayList<>();
            return;
        }
        String jpql = "SELECT pr FROM PersonRelationship pr "
                + "WHERE (pr.aperson = :person OR pr.bperson = :person) "
                + "AND pr.retired = false "
                + "ORDER BY pr.createdAt DESC";
        Map<String, Object> params = new HashMap<>();
        params.put("person", p.getPerson());
        patientRelationships = ejbFacade.findByJpql(jpql, params);
    }

    public void prepareAdd(Patient p) {
        currentPatient = p;
        current = new PersonRelationship();
        relatedPatient = null;
    }

    public void saveRelationship() {
        if (currentPatient == null || currentPatient.getPerson() == null) {
            JsfUtil.addErrorMessage("No patient selected.");
            return;
        }
        if (relatedPatient == null || relatedPatient.getPerson() == null) {
            JsfUtil.addErrorMessage("Please select the related person.");
            return;
        }
        if (current.getApToBpRelationship() == null) {
            JsfUtil.addErrorMessage("Please select the relationship type.");
            return;
        }
        current.setAperson(currentPatient.getPerson());
        current.setBperson(relatedPatient.getPerson());
        current.setCreatedAt(new Date());
        current.setCreater(sessionController.getLoggedUser());
        current.setRetired(false);
        ejbFacade.create(current);
        JsfUtil.addSuccessMessage("Relationship saved.");
        loadRelationshipsForPatient(currentPatient);
        current = new PersonRelationship();
        relatedPatient = null;
    }

    public void deleteRelationship(PersonRelationship pr) {
        if (pr == null) {
            return;
        }
        pr.setRetired(true);
        pr.setRetiredAt(new Date());
        pr.setRetirer(sessionController.getLoggedUser());
        ejbFacade.edit(pr);
        JsfUtil.addSuccessMessage("Relationship removed.");
        loadRelationshipsForPatient(currentPatient);
    }

    /**
     * Returns the label describing how the current patient is related to the
     * other person in the given PersonRelationship record.
     */
    public String getRelationshipLabel(PersonRelationship pr) {
        if (pr == null || pr.getApToBpRelationship() == null) {
            return "";
        }
        return pr.getApToBpRelationship().getLabel();
    }

    /**
     * Returns the Person in the relationship that is NOT the current patient's
     * person — i.e. the related person.
     */
    public Person getOtherPerson(PersonRelationship pr) {
        if (pr == null || currentPatient == null || currentPatient.getPerson() == null) {
            return null;
        }
        Person mine = currentPatient.getPerson();
        if (mine.equals(pr.getAperson())) {
            return pr.getBperson();
        }
        return pr.getAperson();
    }

    public List<Patient> completeRelatedPatient(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String jpql = "SELECT p FROM Patient p WHERE p.retired = false "
                + "AND (UPPER(p.person.name) LIKE :q "
                + "OR p.code LIKE :q "
                + "OR p.person.nic LIKE :q "
                + "OR p.person.mobile LIKE :q) "
                + "ORDER BY p.person.name";
        Map<String, Object> params = new HashMap<>();
        params.put("q", "%" + query.toUpperCase() + "%");
        return patientFacade.findByJpql(jpql, params, 20);
    }

    public List<Relationship> getAllRelationshipTypes() {
        return Arrays.asList(Relationship.values());
    }

    // Getters and setters

    public PersonRelationship getCurrent() {
        return current;
    }

    public void setCurrent(PersonRelationship current) {
        this.current = current;
    }

    public List<PersonRelationship> getPatientRelationships() {
        return patientRelationships;
    }

    public void setPatientRelationships(List<PersonRelationship> patientRelationships) {
        this.patientRelationships = patientRelationships;
    }

    public Patient getCurrentPatient() {
        return currentPatient;
    }

    public void setCurrentPatient(Patient currentPatient) {
        this.currentPatient = currentPatient;
    }

    public Patient getRelatedPatient() {
        return relatedPatient;
    }

    public void setRelatedPatient(Patient relatedPatient) {
        this.relatedPatient = relatedPatient;
    }
}
