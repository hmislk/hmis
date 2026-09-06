package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.data.BillTypeAtomic;
import com.divudi.core.entity.BillFee;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.facade.BillFeeFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.service.AuditService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

/**
 * Report of professional payments (ward + surgery {@code BillFee} rows) for a
 * single admission, with multi-select hold/unhold of individual professional
 * payments — a finer-grained complement to the whole-BHT hold in
 * {@link ProfessionalPaymentHoldController}. (Issue #22383)
 */
@Named
@SessionScoped
public class ProfessionalFeeHoldController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private BillFeeFacade billFeeFacade;

    @EJB
    private AuditService auditService;

    @Inject
    private SessionController sessionController;

    @Inject
    private WebUserController webUserController;

    private PatientEncounter currentEncounter;
    private List<BillFee> professionalFees;
    private List<BillFee> selectedFees;
    private String holdNotes;

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    public String navigateToProfessionalFeeHold(PatientEncounter pe) {
        this.currentEncounter = pe;
        this.selectedFees = new ArrayList<>();
        this.holdNotes = null;
        loadProfessionalFees();
        return "/inward/inward_professional_fee_hold?faces-redirect=true";
    }

    // -------------------------------------------------------------------------
    // Loading
    // -------------------------------------------------------------------------

    public void loadProfessionalFees() {
        professionalFees = new ArrayList<>();
        if (currentEncounter == null || currentEncounter.getId() == null) {
            return;
        }
        List<BillTypeAtomic> btcs = new ArrayList<>();
        btcs.add(BillTypeAtomic.INWARD_PROFESSIONAL_FEE_BILL);
        btcs.add(BillTypeAtomic.INWARD_THEATRE_PROFESSIONAL_FEE_BILL);
        btcs.add(BillTypeAtomic.INWARD_SERVICE_BILL);
        btcs.add(BillTypeAtomic.INWARD_SERVICE_BATCH_BILL);
        String jpql = "select bf from BillFee bf where "
                + " bf.retired=false "
                + " and bf.bill.cancelled=false "
                + " and bf.patienEncounter=:pe "
                + " and bf.staff is not null "
                + " and bf.bill.billTypeAtomic in :btcs "
                + " order by bf.staff.person.name, bf.createdAt";
        Map<String, Object> params = new HashMap<>();
        params.put("pe", currentEncounter);
        params.put("btcs", btcs);
        professionalFees = billFeeFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    // -------------------------------------------------------------------------
    // Hold / Release (multi-select bulk actions)
    // -------------------------------------------------------------------------

    public void holdSelected() {
        if (!webUserController.hasPrivilege("InwardHoldProfessionalPayments")) {
            JsfUtil.addErrorMessage("You do not have privileges to hold professional payments.");
            return;
        }
        if (selectedFees == null || selectedFees.isEmpty()) {
            JsfUtil.addErrorMessage("Please select at least one professional payment to hold.");
            return;
        }
        int held = 0;
        List<String> alreadyPaidStaffNames = new ArrayList<>();
        for (BillFee bf : selectedFees) {
            if (Boolean.TRUE.equals(bf.getCompletedPayment())) {
                alreadyPaidStaffNames.add(bf.getStaff() != null ? bf.getStaff().getPerson().getNameWithTitle() : "Unknown");
                continue;
            }
            if (bf.isFeePaymentOnHold()) {
                continue;
            }
            Map<String, Object> before = feeHoldStateMap(bf);
            bf.setFeePaymentOnHold(Boolean.TRUE);
            bf.setFeePaymentHoldDateTime(new Date());
            bf.setFeePaymentHoldBy(sessionController.getLoggedUser());
            bf.setFeePaymentHoldNotes(holdNotes);
            billFeeFacade.edit(bf);
            auditService.logEncounterAudit(currentEncounter, "Professional Fee Payment Hold",
                    before, feeHoldStateMap(bf), sessionController.getLoggedUser(), "BillFee", bf.getId());
            held++;
        }
        selectedFees = new ArrayList<>();
        holdNotes = null;
        loadProfessionalFees();
        if (held > 0) {
            JsfUtil.addSuccessMessage(held + " professional payment(s) held.");
        }
        if (!alreadyPaidStaffNames.isEmpty()) {
            JsfUtil.addErrorMessage("Cannot hold payment(s) for " + String.join(", ", alreadyPaidStaffNames)
                    + " because payment has already been completed.");
        } else if (held == 0) {
            JsfUtil.addErrorMessage("Selected payment(s) are already on hold.");
        }
    }

    public void releaseHoldSelected() {
        if (!webUserController.hasPrivilege("InwardHoldProfessionalPayments")) {
            JsfUtil.addErrorMessage("You do not have privileges to release the hold on professional payments.");
            return;
        }
        if (selectedFees == null || selectedFees.isEmpty()) {
            JsfUtil.addErrorMessage("Please select at least one professional payment to release.");
            return;
        }
        int released = 0;
        for (BillFee bf : selectedFees) {
            if (!bf.isFeePaymentOnHold()) {
                continue;
            }
            Map<String, Object> before = feeHoldStateMap(bf);
            bf.setFeePaymentOnHold(Boolean.FALSE);
            bf.setFeePaymentHoldDateTime(null);
            bf.setFeePaymentHoldBy(null);
            bf.setFeePaymentHoldNotes(null);
            billFeeFacade.edit(bf);
            auditService.logEncounterAudit(currentEncounter, "Professional Fee Payment Hold Released",
                    before, feeHoldStateMap(bf), sessionController.getLoggedUser(), "BillFee", bf.getId());
            released++;
        }
        selectedFees = new ArrayList<>();
        loadProfessionalFees();
        if (released > 0) {
            JsfUtil.addSuccessMessage(released + " professional payment(s) released from hold.");
        } else {
            JsfUtil.addErrorMessage("Selected payment(s) are not currently on hold.");
        }
    }

    /**
     * Snapshot of a BillFee's hold fields for audit before/after events.
     */
    private Map<String, Object> feeHoldStateMap(BillFee bf) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (bf == null) {
            return m;
        }
        m.put("billFeeId", bf.getId());
        m.put("staff", bf.getStaff() != null ? bf.getStaff().getName() : null);
        m.put("feeValue", bf.getFeeValue());
        m.put("feePaymentOnHold", bf.isFeePaymentOnHold());
        m.put("feePaymentHoldDateTime", bf.getFeePaymentHoldDateTime());
        m.put("feePaymentHoldBy", bf.getFeePaymentHoldBy() != null ? bf.getFeePaymentHoldBy().getName() : null);
        m.put("feePaymentHoldNotes", bf.getFeePaymentHoldNotes());
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

    public List<BillFee> getProfessionalFees() {
        if (professionalFees == null) {
            professionalFees = new ArrayList<>();
        }
        return professionalFees;
    }

    public void setProfessionalFees(List<BillFee> professionalFees) {
        this.professionalFees = professionalFees;
    }

    public List<BillFee> getSelectedFees() {
        if (selectedFees == null) {
            selectedFees = new ArrayList<>();
        }
        return selectedFees;
    }

    public void setSelectedFees(List<BillFee> selectedFees) {
        this.selectedFees = selectedFees;
    }

    public String getHoldNotes() {
        return holdNotes;
    }

    public void setHoldNotes(String holdNotes) {
        this.holdNotes = holdNotes;
    }
}
