package com.divudi.bean.inward;

import com.divudi.bean.common.NotificationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.Date;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Handles Nursing Discharge (stage 4) and Physical Discharge (stage 5) of the
 * inpatient workflow.
 *
 * Flow: Clinical → Room → Nursing → Administrative → Physical
 */
@Named
@SessionScoped
public class NursingDischargeController implements Serializable {

    @EJB
    private PatientEncounterFacade patientEncounterFacade;

    @Inject
    private SessionController sessionController;

    @Inject
    private NotificationController notificationController;

    private PatientEncounter currentEncounter;

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    public String navigateToNursingDischarge(PatientEncounter pe) {
        this.currentEncounter = pe;
        return "/inward/inward_nursing_discharge?faces-redirect=true";
    }

    public String navigateToPhysicalDischarge(PatientEncounter pe) {
        this.currentEncounter = pe;
        return "/inward/inward_physical_discharge?faces-redirect=true";
    }

    // -------------------------------------------------------------------------
    // Nursing Discharge
    // -------------------------------------------------------------------------

    public void confirmNursingDischarge() {
        if (currentEncounter == null) {
            JsfUtil.addErrorMessage("No patient encounter loaded.");
            return;
        }
        if (currentEncounter.isNursingDischarged()) {
            JsfUtil.addErrorMessage("Nursing discharge already confirmed.");
            return;
        }
        if (currentEncounter.getRoomDischargeDateTime() == null) {
            JsfUtil.addErrorMessage("Cannot confirm nursing discharge: room discharge has not been completed.");
            return;
        }
        currentEncounter.setNursingDischarged(Boolean.TRUE);
        currentEncounter.setNursingDischargeDateTime(new Date());
        currentEncounter.setNursingDischargedBy(sessionController.getLoggedUser());
        patientEncounterFacade.edit(currentEncounter);
        notificationController.createNotification(currentEncounter, "NursingDischarge");
        JsfUtil.addSuccessMessage("Nursing discharge confirmed.");
    }

    public void cancelNursingDischarge() {
        if (currentEncounter == null) {
            JsfUtil.addErrorMessage("No patient encounter loaded.");
            return;
        }
        if (!currentEncounter.isNursingDischarged()) {
            JsfUtil.addErrorMessage("Nursing discharge has not been confirmed.");
            return;
        }
        if (currentEncounter.isPhysicalDischarged()) {
            JsfUtil.addErrorMessage("Cannot cancel nursing discharge: physical discharge has already been confirmed. Cancel physical discharge first.");
            return;
        }
        currentEncounter.setNursingDischarged(Boolean.FALSE);
        currentEncounter.setNursingDischargeDateTime(null);
        currentEncounter.setNursingDischargedBy(null);
        patientEncounterFacade.edit(currentEncounter);
        JsfUtil.addSuccessMessage("Nursing discharge cancelled.");
    }

    // -------------------------------------------------------------------------
    // Physical Discharge
    // -------------------------------------------------------------------------

    public void confirmPhysicalDischarge() {
        if (currentEncounter == null) {
            JsfUtil.addErrorMessage("No patient encounter loaded.");
            return;
        }
        if (currentEncounter.isPhysicalDischarged()) {
            JsfUtil.addErrorMessage("Physical discharge already confirmed.");
            return;
        }
        if (!currentEncounter.isNursingDischarged()) {
            JsfUtil.addErrorMessage("Cannot confirm physical discharge: nursing discharge has not been completed.");
            return;
        }
        if (!Boolean.TRUE.equals(currentEncounter.getDischarged())) {
            JsfUtil.addErrorMessage("Cannot confirm physical discharge: administrative discharge (final bill) has not been completed.");
            return;
        }
        currentEncounter.setPhysicalDischarged(Boolean.TRUE);
        currentEncounter.setPhysicalDischargeDateTime(new Date());
        currentEncounter.setPhysicalDischargedBy(sessionController.getLoggedUser());
        patientEncounterFacade.edit(currentEncounter);
        notificationController.createNotification(currentEncounter, "PhysicalDischarge");
        JsfUtil.addSuccessMessage("Physical discharge confirmed. Patient has left the hospital.");
    }

    public void cancelPhysicalDischarge() {
        if (currentEncounter == null) {
            JsfUtil.addErrorMessage("No patient encounter loaded.");
            return;
        }
        if (!currentEncounter.isPhysicalDischarged()) {
            JsfUtil.addErrorMessage("Physical discharge has not been confirmed.");
            return;
        }
        currentEncounter.setPhysicalDischarged(Boolean.FALSE);
        currentEncounter.setPhysicalDischargeDateTime(null);
        currentEncounter.setPhysicalDischargedBy(null);
        patientEncounterFacade.edit(currentEncounter);
        JsfUtil.addSuccessMessage("Physical discharge cancelled.");
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public PatientEncounter getCurrentEncounter() {
        return currentEncounter;
    }

    public void setCurrentEncounter(PatientEncounter currentEncounter) {
        this.currentEncounter = currentEncounter;
    }
}
