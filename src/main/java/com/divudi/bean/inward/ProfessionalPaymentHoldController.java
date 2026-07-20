package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
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
        currentEncounter.setProfessionalPaymentsOnHold(Boolean.TRUE);
        currentEncounter.setProfessionalPaymentsHoldDateTime(new Date());
        currentEncounter.setProfessionalPaymentsHoldBy(sessionController.getLoggedUser());
        patientEncounterFacade.edit(currentEncounter);
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
        currentEncounter.setProfessionalPaymentsOnHold(Boolean.FALSE);
        currentEncounter.setProfessionalPaymentsHoldDateTime(null);
        currentEncounter.setProfessionalPaymentsHoldBy(null);
        currentEncounter.setProfessionalPaymentsHoldNotes(null);
        patientEncounterFacade.edit(currentEncounter);
        JsfUtil.addSuccessMessage("Professional payments hold released for BHT " + currentEncounter.getBhtNo() + ".");
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
