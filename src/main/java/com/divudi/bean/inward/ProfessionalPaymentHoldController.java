package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.AuditService;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Date;

/**
 * Handles holding and releasing the hold on professional fee payments for a
 * BHT (PatientEncounter). This is BHT-level only (no per-fee hold).
 * <p>
 * Holding a BHT does NOT block charge entry — {@code BillBhtController} and
 * {@code InwardProfessionalBillController} continue to add charges normally
 * regardless of hold state. Only the payment pages
 * ({@code InwardStaffPaymentBillController},
 * {@code InwardSurgeryPaymentBillController}) check this flag before allowing
 * professional fees to be paid out. (Issue #16473)
 * <p>
 * Mirrors the shape of {@link NursingDischargeController}, applying the guard
 * at payment time instead of charge-entry time.
 */
@Named
@SessionScoped
public class ProfessionalPaymentHoldController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PatientEncounterFacade patientEncounterFacade;

    @EJB
    private AuditService auditService;

    @Inject
    private SessionController sessionController;

    @Inject
    private WebUserController webUserController;

    private PatientEncounter currentEncounter;

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    public String navigateToProfessionalPaymentHold(PatientEncounter pe) {
        this.currentEncounter = pe;
        return "/inward/inward_professional_payment_hold?faces-redirect=true";
    }

    // -------------------------------------------------------------------------
    // Hold / Release
    // -------------------------------------------------------------------------

    public void confirmHold() {
        if (currentEncounter == null) {
            JsfUtil.addErrorMessage("No patient encounter loaded.");
            return;
        }
        if (!webUserController.hasPrivilege("InwardHoldProfessionalPayments")) {
            JsfUtil.addErrorMessage("You do not have privileges to hold professional payments.");
            return;
        }
        if (currentEncounter.isProfessionalPaymentsOnHold()) {
            JsfUtil.addErrorMessage("Professional payments are already on hold for this BHT.");
            return;
        }
        Map<String, Object> before = holdStateMap();
        currentEncounter.setProfessionalPaymentsOnHold(Boolean.TRUE);
        currentEncounter.setProfessionalPaymentsHoldDateTime(new Date());
        currentEncounter.setProfessionalPaymentsHoldBy(sessionController.getLoggedUser());
        patientEncounterFacade.edit(currentEncounter);
        auditService.logEncounterAudit(currentEncounter, "Professional Payments Hold (BHT)",
                before, holdStateMap(), sessionController.getLoggedUser());
        JsfUtil.addSuccessMessage("Professional payments held for BHT " + currentEncounter.getBhtNo() + ".");
    }

    public void releaseHold() {
        if (currentEncounter == null) {
            JsfUtil.addErrorMessage("No patient encounter loaded.");
            return;
        }
        if (!webUserController.hasPrivilege("InwardHoldProfessionalPayments")) {
            JsfUtil.addErrorMessage("You do not have privileges to release the hold on professional payments.");
            return;
        }
        if (!currentEncounter.isProfessionalPaymentsOnHold()) {
            JsfUtil.addErrorMessage("Professional payments are not currently on hold for this BHT.");
            return;
        }
        Map<String, Object> before = holdStateMap();
        currentEncounter.setProfessionalPaymentsOnHold(Boolean.FALSE);
        currentEncounter.setProfessionalPaymentsHoldDateTime(null);
        currentEncounter.setProfessionalPaymentsHoldBy(null);
        currentEncounter.setProfessionalPaymentsHoldNotes(null);
        patientEncounterFacade.edit(currentEncounter);
        auditService.logEncounterAudit(currentEncounter, "Professional Payments Hold Released (BHT)",
                before, holdStateMap(), sessionController.getLoggedUser());
        JsfUtil.addSuccessMessage("Professional payments hold released for BHT " + currentEncounter.getBhtNo() + ".");
    }

    /**
     * Snapshot of the BHT-level hold fields for audit before/after events (#22383).
     */
    private Map<String, Object> holdStateMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (currentEncounter == null) {
            return m;
        }
        m.put("professionalPaymentsOnHold", currentEncounter.isProfessionalPaymentsOnHold());
        m.put("professionalPaymentsHoldDateTime", currentEncounter.getProfessionalPaymentsHoldDateTime());
        m.put("professionalPaymentsHoldBy", currentEncounter.getProfessionalPaymentsHoldBy() != null
                ? currentEncounter.getProfessionalPaymentsHoldBy().getName() : null);
        m.put("professionalPaymentsHoldNotes", currentEncounter.getProfessionalPaymentsHoldNotes());
        return m;
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
