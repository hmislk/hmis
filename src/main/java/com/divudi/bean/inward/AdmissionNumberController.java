/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.inward.AdmissionNumber;
import com.divudi.core.facade.AdmissionNumberFacade;
import com.divudi.ejb.BillNumberGenerator;
import com.divudi.service.AuditService;
import java.io.Serializable;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Manages BHT (admission) number counters per institution and admission type.
 * Allows administrators to adjust the lastAdmissionNumber to match the physical
 * BHT register. The next admission issued will be lastAdmissionNumber + 1.
 *
 * @author Dr. M H B Ariyaratne
 */
@Named
@SessionScoped
public class AdmissionNumberController implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    SessionController sessionController;

    @EJB
    AdmissionNumberFacade ejbFacade;
    @EJB
    BillNumberGenerator billNumberGenerator;
    @EJB
    AuditService auditService;

    private List<AdmissionNumber> items;
    private AdmissionNumber selected;
    /** Counter value when Edit was clicked; the save is rejected if it has moved since. */
    private Long lastAdmissionNumberAtEdit;

    public String navigateToManageAdmissionNumbers() {
        selected = null;
        items = null;
        return "/inward/inward_admission_number_management?faces-redirect=true";
    }

    public List<AdmissionNumber> getItems() {
        if (items == null) {
            items = ejbFacade.findByJpql(
                    "SELECT an FROM AdmissionNumber an WHERE an.retired = false ORDER BY an.id");
        }
        return items;
    }

    public String getTypeLabel(AdmissionNumber an) {
        if (an.getAdmissionType() != null) {
            return an.getAdmissionType().getName();
        }
        if (an.getAdmissionTypeEnum() != null) {
            return an.getAdmissionTypeEnum().name();
        }
        return "General";
    }

    public long getNextBht(AdmissionNumber an) {
        if (an.getLastAdmissionNumber() == null) {
            return 1L;
        }
        return an.getLastAdmissionNumber() + 1;
    }

    public void prepareEdit(AdmissionNumber an) {
        selected = an;
        lastAdmissionNumberAtEdit = an == null ? null : an.getLastAdmissionNumber();
    }

    public void saveSelected() {
        if (selected == null) {
            JsfUtil.addErrorMessage("No record selected.");
            return;
        }
        if (selected.getLastAdmissionNumber() == null || selected.getLastAdmissionNumber() < 0) {
            JsfUtil.addErrorMessage("Last used BHT number must be zero or a positive number.");
            return;
        }
        Long newLast = selected.getLastAdmissionNumber();
        Long previous;
        try {
            // Locked compare-and-set, so a save never rewinds past a BHT number
            // another user drew while this form was open. (#24672)
            previous = billNumberGenerator.resetAdmissionNumberRecord(selected, newLast, lastAdmissionNumberAtEdit);
        } catch (Exception e) {
            Throwable cause = e;
            while (cause != null && !(cause instanceof ConcurrentModificationException)) {
                cause = cause.getCause();
            }
            if (cause == null) {
                throw e;
            }
            JsfUtil.addErrorMessage("A BHT number was issued from this sequence while you were editing, "
                    + "so the change was not saved. Check the current value and edit again.");
            selected = null;
            items = null;
            return;
        }

        Map<String, Object> before = new HashMap<>();
        before.put("lastAdmissionNumber", previous);
        Map<String, Object> after = new HashMap<>();
        after.put("lastAdmissionNumber", newLast);
        auditService.logAudit(before, after, sessionController.getLoggedUser(),
                "AdmissionNumber", "Admission Number Counter Reset", selected.getId());

        JsfUtil.addSuccessMessage("BHT counter updated. Next BHT will be " + getNextBht(selected) + ".");
        lastAdmissionNumberAtEdit = newLast;
        items = null;
    }

    public AdmissionNumber getSelected() {
        return selected;
    }

    public void setSelected(AdmissionNumber selected) {
        this.selected = selected;
    }
}
