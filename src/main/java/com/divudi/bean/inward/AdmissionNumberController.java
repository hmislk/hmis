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
import java.io.Serializable;
import java.util.List;
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

    private List<AdmissionNumber> items;
    private AdmissionNumber selected;

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
        ejbFacade.edit(selected);
        JsfUtil.addSuccessMessage("BHT counter updated. Next BHT will be " + getNextBht(selected) + ".");
        items = null;
    }

    public AdmissionNumber getSelected() {
        return selected;
    }

    public void setSelected(AdmissionNumber selected) {
        this.selected = selected;
    }
}
