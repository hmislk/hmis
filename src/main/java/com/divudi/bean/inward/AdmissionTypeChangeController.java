package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.inward.Admission;
import com.divudi.core.entity.inward.AdmissionType;
import com.divudi.core.facade.AdmissionTypeFacade;
import com.divudi.core.facade.PatientEncounterFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.inward.AdmissionTypeChangeException;
import com.divudi.service.inward.AdmissionTypeHistoryService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Change Admission Type page of the Inpatient Dashboard (#25814). The BHT
 * number does not change; charges added so far keep their stored values and the
 * bill-time calculations use the old type before the change and the new type
 * after it (see {@link AdmissionTypeHistoryService}).
 */
@Named
@SessionScoped
public class AdmissionTypeChangeController implements Serializable {

    private static final long serialVersionUID = 1L;
    static final String PRIVILEGE = "InwardChangeAdmissionType";

    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;
    @Inject
    private AdmissionController admissionController;

    @EJB
    private AdmissionTypeHistoryService admissionTypeHistoryService;
    @EJB
    private AdmissionTypeFacade admissionTypeFacade;
    @EJB
    private PatientEncounterFacade patientEncounterFacade;

    private PatientEncounter admission;
    private AdmissionType newType;
    private String reason;
    private long interimBillCount;
    private List<AdmissionType> choices = new ArrayList<>();

    public String navigateToChangeAdmissionType(PatientEncounter pe) {
        if (!webUserController.hasPrivilege(PRIVILEGE)) {
            JsfUtil.addErrorMessage("You are not authorized to change the admission type.");
            return "";
        }
        if (pe == null || pe.getId() == null) {
            JsfUtil.addErrorMessage("No admission selected.");
            return "";
        }
        PatientEncounter fresh = patientEncounterFacade.find(pe.getId());
        if (fresh == null) {
            JsfUtil.addErrorMessage("Admission not found.");
            return "";
        }
        if (Boolean.TRUE.equals(fresh.isDischarged())) {
            JsfUtil.addErrorMessage("The patient is already discharged. The admission type cannot be changed.");
            return "";
        }
        if (fresh.isPaymentFinalized()) {
            JsfUtil.addErrorMessage("The final bill is already settled. The admission type cannot be changed.");
            return "";
        }
        admission = fresh;
        newType = null;
        reason = null;
        interimBillCount = admissionTypeHistoryService.countInterimBills(fresh);
        fillChoices();
        return "/inward/inward_change_admission_type?faces-redirect=true";
    }

    private void fillChoices() {
        choices = new ArrayList<>();
        String jpql = "select t from AdmissionType t where t.retired=false order by t.name";
        List<AdmissionType> all = admissionTypeFacade.findByJpql(jpql);
        AdmissionType cur = admission != null ? admission.getAdmissionType() : null;
        if (all != null) {
            for (AdmissionType t : all) {
                if (cur == null || cur.getId() == null || !cur.getId().equals(t.getId())) {
                    choices.add(t);
                }
            }
        }
    }

    public String save() {
        if (!webUserController.hasPrivilege(PRIVILEGE)) {
            JsfUtil.addErrorMessage("You are not authorized to change the admission type.");
            return "";
        }
        if (admission == null) {
            JsfUtil.addErrorMessage("No admission selected.");
            return "";
        }
        try {
            admissionTypeHistoryService.changeAdmissionType(admission, newType, reason, sessionController.getLoggedUser());
        } catch (AdmissionTypeChangeException e) {
            JsfUtil.addErrorMessage(e.getMessage());
            return "";
        }
        JsfUtil.addSuccessMessage("Admission type changed. Charges added before the change keep the old type.");
        PatientEncounter fresh = patientEncounterFacade.find(admission.getId());
        if (fresh instanceof Admission) {
            admissionController.setCurrent((Admission) fresh);
        }
        admission = null;
        newType = null;
        reason = null;
        return admissionController.navigateToAdmissionProfilePage();
    }

    public String cancel() {
        if (admission instanceof Admission) {
            admissionController.setCurrent((Admission) admission);
        }
        return admissionController.navigateToAdmissionProfilePage();
    }

    public boolean isInterimBillWarning() {
        return interimBillCount > 0;
    }

    public PatientEncounter getAdmission() {
        return admission;
    }

    public AdmissionType getNewType() {
        return newType;
    }

    public void setNewType(AdmissionType newType) {
        this.newType = newType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public long getInterimBillCount() {
        return interimBillCount;
    }

    public List<AdmissionType> getChoices() {
        return choices;
    }
}
