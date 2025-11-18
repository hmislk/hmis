/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.ControllerWithPatient;
import com.divudi.bean.common.SessionController;
import com.divudi.core.data.PaymentMethod;
import com.divudi.core.data.Sex;
import com.divudi.core.data.Title;
import com.divudi.core.entity.Patient;
import com.divudi.core.entity.Person;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.facade.AdmissionFacade;
import com.divudi.core.facade.PatientFacade;
import com.divudi.core.facade.PersonFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.AuditService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Controller for changing the patient associated with an admission
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class AdmissionPatientChangeController implements Serializable, ControllerWithPatient {

    private static final long serialVersionUID = 1L;

    // <editor-fold defaultstate="collapsed" desc="Controller">
    @Inject
    SessionController sessionController;
    @Inject
    com.divudi.bean.common.PatientController patientController;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="EJBs">
    @EJB
    private AdmissionFacade admissionFacade;
    @EJB
    private PersonFacade personFacade;
    @EJB
    private PatientFacade patientFacade;
    @EJB
    private AuditService auditService;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Variables">
    private Admission current;
    private Patient originalPatient;
    private Patient newPatient;
    private boolean patientDetailsEditable;
    private PaymentMethod paymentMethod;
    private String selectText = "";
    private boolean showConfirmation;
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Functions">

    /**
     * Navigate to the Change Patient for Admission page
     */
    public String navigateToChangeAdmissionPatient() {
        prepareForNew();
        return "/inward/inward_change_patient?faces-redirect=true";
    }

    /**
     * Auto-complete method for searching admissions
     */
    public List<Admission> completeAdmission(String query) {
        List<Admission> suggestions;
        String sql;
        HashMap h = new HashMap();

        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Admission c "
                    + " where c.retired=false "
                    + " and c.discharged=false "
                    + " and (upper(c.bhtNo) like :qLike "
                    + " or upper(c.patient.person.name) like :qLike "
                    + " or upper(c.patient.phn) = :qExact )"
                    + " order by c.bhtNo";

            h.put("qLike", "%" + query.toUpperCase() + "%");
            h.put("qExact", query.toUpperCase());
            suggestions = admissionFacade.findByJpql(sql, h, 20);
        }
        return suggestions;
    }

    /**
     * Load the selected admission and display current patient details
     */
    public void loadAdmission() {
        if (current == null) {
            JsfUtil.addErrorMessage("Please select an admission");
            return;
        }

        // Store the original patient
        originalPatient = current.getPatient();

        // Initialize new patient as null (will be selected by user)
        newPatient = null;
        patientDetailsEditable = true;
        showConfirmation = false;

        JsfUtil.addSuccessMessage("Admission loaded. Current patient: " + originalPatient.getPerson().getName());
    }

    /**
     * Prepare to confirm the patient change
     */
    public void prepareConfirmation() {
        if (current == null) {
            JsfUtil.addErrorMessage("No admission selected");
            return;
        }

        if (newPatient == null || newPatient.getId() == null) {
            JsfUtil.addErrorMessage("Please select a new patient");
            return;
        }

        if (originalPatient.getId().equals(newPatient.getId())) {
            JsfUtil.addErrorMessage("The selected patient is the same as the current patient. Please select a different patient.");
            return;
        }

        showConfirmation = true;
        JsfUtil.addSuccessMessage("Please confirm the patient change");
    }

    /**
     * Confirm and save the patient change
     */
    public void confirmPatientChange() {
        if (current == null || newPatient == null) {
            JsfUtil.addErrorMessage("Invalid operation");
            return;
        }

        if (originalPatient.getId().equals(newPatient.getId())) {
            JsfUtil.addErrorMessage("Cannot change to the same patient");
            return;
        }

        try {
            // Create audit record BEFORE making the change
            auditService.logAudit(
                originalPatient,
                newPatient,
                sessionController.getLoggedUser(),
                "Admission Patient Change",
                "Patient changed for BHT: " + current.getBhtNo() +
                " from " + originalPatient.getPerson().getName() +
                " (ID: " + originalPatient.getId() + ")" +
                " to " + newPatient.getPerson().getName() +
                " (ID: " + newPatient.getId() + ")"
            );

            // Update the admission's patient reference
            current.setPatient(newPatient);
            admissionFacade.edit(current);

            JsfUtil.addSuccessMessage("Patient changed successfully. " +
                "BHT: " + current.getBhtNo() +
                " is now assigned to " + newPatient.getPerson().getName());

            // Reset for next operation
            prepareForNew();

        } catch (Exception e) {
            JsfUtil.addErrorMessage("Error changing patient: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cancel the patient change operation
     */
    public void cancelChange() {
        showConfirmation = false;
        newPatient = null;
        patientDetailsEditable = true;
        JsfUtil.addSuccessMessage("Operation cancelled");
    }

    /**
     * Prepare for a new patient change operation
     */
    public void prepareForNew() {
        current = null;
        originalPatient = null;
        newPatient = null;
        patientDetailsEditable = false;
        selectText = "";
        showConfirmation = false;
    }

    public Title[] getTitle() {
        return Title.values();
    }

    public Sex[] getSex() {
        return Sex.values();
    }

    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Getter & Setters">

    public Admission getCurrent() {
        return current;
    }

    public void setCurrent(Admission current) {
        this.current = current;
    }

    public Patient getOriginalPatient() {
        return originalPatient;
    }

    public void setOriginalPatient(Patient originalPatient) {
        this.originalPatient = originalPatient;
    }

    @Override
    public Patient getPatient() {
        if (newPatient == null) {
            Person p = new Person();
            newPatient = new Patient();
            newPatient.setPerson(p);
        }
        return newPatient;
    }

    @Override
    public void setPatient(Patient patient) {
        this.newPatient = patient;
    }

    @Override
    public boolean isPatientDetailsEditable() {
        return patientDetailsEditable;
    }

    @Override
    public void setPatientDetailsEditable(boolean patientDetailsEditable) {
        this.patientDetailsEditable = patientDetailsEditable;
    }

    @Override
    public void toggalePatientEditable() {
        patientDetailsEditable = !patientDetailsEditable;
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    @Override
    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    @Override
    public void listnerForPaymentMethodChange() {
        // Not used in this context
    }

    public String getSelectText() {
        return selectText;
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public boolean isShowConfirmation() {
        return showConfirmation;
    }

    public void setShowConfirmation(boolean showConfirmation) {
        this.showConfirmation = showConfirmation;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public AdmissionFacade getAdmissionFacade() {
        return admissionFacade;
    }

    public void setAdmissionFacade(AdmissionFacade admissionFacade) {
        this.admissionFacade = admissionFacade;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public PatientFacade getPatientFacade() {
        return patientFacade;
    }

    public void setPatientFacade(PatientFacade patientFacade) {
        this.patientFacade = patientFacade;
    }

    public AuditService getAuditService() {
        return auditService;
    }

    public void setAuditService(AuditService auditService) {
        this.auditService = auditService;
    }

    // </editor-fold>
}
