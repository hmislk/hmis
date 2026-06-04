/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.data.web.ComponentPresentationType;
import com.divudi.core.entity.PatientEncounter;
import com.divudi.core.entity.web.CaptureComponent;
import com.divudi.core.entity.web.DesignComponent;
import com.divudi.core.entity.web.DesignComponentChoice;
import com.divudi.core.entity.inward.PatientFormEntry;
import com.divudi.core.facade.PatientFormEntryFacade;
import com.divudi.core.facade.web.CaptureComponentFacade;
import com.divudi.core.facade.web.DesignComponentChoiceFacade;
import com.divudi.core.facade.web.DesignComponentFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Lets inpatient staff list, fill, edit and delete the dynamic forms
 * ({@link PatientFormEntry}) of an admission.
 *
 * @author Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class InwardFormController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PatientFormEntryFacade patientFormEntryFacade;
    @EJB
    private CaptureComponentFacade captureComponentFacade;
    @EJB
    private DesignComponentFacade designComponentFacade;
    @EJB
    private DesignComponentChoiceFacade designComponentChoiceFacade;

    @Inject
    private SessionController sessionController;

    private PatientEncounter patientEncounter;
    private List<PatientFormEntry> formEntries;
    private PatientFormEntry currentEntry;
    private List<CaptureComponent> currentCaptureComponents;
    private List<DesignComponent> availableTemplates;
    private DesignComponent selectedTemplate;

    public String navigateToInwardFormsFromAdmissionProfile(PatientEncounter pe) {
        if (pe == null) {
            JsfUtil.addErrorMessage("No admission selected");
            return "";
        }
        patientEncounter = pe;
        loadFormEntries();
        loadAvailableTemplates();
        currentEntry = null;
        currentCaptureComponents = null;
        selectedTemplate = null;
        return "/inward/admission_forms?faces-redirect=true";
    }

    public void loadFormEntries() {
        if (patientEncounter == null) {
            formEntries = new ArrayList<>();
            return;
        }
        String jpql = "select pfe "
                + " from PatientFormEntry pfe "
                + " where pfe.patientEncounter=:pe "
                + " and pfe.retired=:ret "
                + " order by pfe.createdAt";
        Map<String, Object> m = new HashMap<>();
        m.put("pe", patientEncounter);
        m.put("ret", false);
        formEntries = patientFormEntryFacade.findByJpql(jpql, m);
    }

    public void loadAvailableTemplates() {
        String jpql = "select d "
                + " from DesignComponent d "
                + " where d.componentPresentationType=:pt "
                + " and d.retired=:ret "
                + " order by d.name";
        Map<String, Object> m = new HashMap<>();
        m.put("pt", ComponentPresentationType.DataEntryForm);
        m.put("ret", false);
        availableTemplates = designComponentFacade.findByJpql(jpql, m);
    }

    private List<DesignComponent> listTemplateFields(DesignComponent template) {
        String jpql = "select d "
                + " from DesignComponent d "
                + " where d.dataEntryForm=:def "
                + " and d.retired=:ret "
                + " order by d.orderNo";
        Map<String, Object> m = new HashMap<>();
        m.put("def", template);
        m.put("ret", false);
        return designComponentFacade.findByJpql(jpql, m);
    }

    public void startNewForm() {
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No admission selected");
            return;
        }
        if (selectedTemplate == null) {
            JsfUtil.addErrorMessage("Please select a form to fill");
            return;
        }
        currentEntry = new PatientFormEntry();
        currentEntry.setPatientEncounter(patientEncounter);
        currentEntry.setDesignComponent(selectedTemplate);

        currentCaptureComponents = new ArrayList<>();
        for (DesignComponent field : listTemplateFields(selectedTemplate)) {
            CaptureComponent cc = new CaptureComponent();
            cc.setName(field.getName());
            cc.setDesignComponent(field);
            cc.setComponentPresentationType(field.getComponentPresentationType());
            cc.setComponentDataType(field.getComponentDataType());
            cc.setPlaceholder(field.getPlaceholder());
            cc.setMinValue(field.getMinValue());
            cc.setMaxValue(field.getMaxValue());
            cc.setStepSize(field.getStepSize());
            cc.setMaxRating(field.getMaxRating());
            cc.setOnLabel(field.getOnLabel());
            cc.setOffLabel(field.getOffLabel());
            currentCaptureComponents.add(cc);
        }
    }

    public void saveForm() {
        if (currentEntry == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        if (patientEncounter == null) {
            JsfUtil.addErrorMessage("No admission selected");
            return;
        }
        currentEntry.setPatientEncounter(patientEncounter);

        if (currentEntry.getId() == null) {
            currentEntry.setCreater(sessionController.getLoggedUser());
            currentEntry.setCreatedAt(new Date());
            patientFormEntryFacade.create(currentEntry);
        } else {
            currentEntry.setEditor(sessionController.getLoggedUser());
            currentEntry.setEditedAt(new Date());
            patientFormEntryFacade.edit(currentEntry);
        }

        if (currentCaptureComponents != null) {
            for (CaptureComponent cc : currentCaptureComponents) {
                cc.setPatientFormEntry(currentEntry);
                if (cc.getId() == null) {
                    cc.setCreater(sessionController.getLoggedUser());
                    cc.setCreatedAt(new Date());
                    captureComponentFacade.create(cc);
                } else {
                    captureComponentFacade.edit(cc);
                }
            }
        }

        JsfUtil.addSuccessMessage("Form Saved");
        loadFormEntries();
        currentEntry = null;
        currentCaptureComponents = null;
        selectedTemplate = null;
    }

    public void editForm(PatientFormEntry entry) {
        if (entry == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        currentEntry = entry;
        selectedTemplate = entry.getDesignComponent();
        String jpql = "select cc "
                + " from CaptureComponent cc "
                + " where cc.patientFormEntry=:pfe "
                + " and cc.retired=:ret "
                + " order by cc.designComponent.orderNo";
        Map<String, Object> m = new HashMap<>();
        m.put("pfe", entry);
        m.put("ret", false);
        currentCaptureComponents = captureComponentFacade.findByJpql(jpql, m);
    }

    public void deleteForm(PatientFormEntry entry) {
        if (entry == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        entry.setRetired(true);
        entry.setRetirer(sessionController.getLoggedUser());
        entry.setRetiredAt(new Date());
        patientFormEntryFacade.edit(entry);

        String jpql = "select cc "
                + " from CaptureComponent cc "
                + " where cc.patientFormEntry=:pfe "
                + " and cc.retired=:ret ";
        Map<String, Object> m = new HashMap<>();
        m.put("pfe", entry);
        m.put("ret", false);
        List<CaptureComponent> children = captureComponentFacade.findByJpql(jpql, m);
        for (CaptureComponent cc : children) {
            cc.setRetired(true);
            cc.setRetirer(sessionController.getLoggedUser());
            cc.setRetiredAt(new Date());
            captureComponentFacade.edit(cc);
        }

        if (currentEntry != null && currentEntry.equals(entry)) {
            currentEntry = null;
            currentCaptureComponents = null;
        }
        JsfUtil.addSuccessMessage("Form Deleted");
        loadFormEntries();
    }

    public void cancelForm() {
        currentEntry = null;
        currentCaptureComponents = null;
        selectedTemplate = null;
    }

    public List<DesignComponentChoice> getChoicesFor(CaptureComponent cc) {
        if (cc == null || cc.getDesignComponent() == null) {
            return new ArrayList<>();
        }
        String jpql = "select c from DesignComponentChoice c"
                + " where c.designComponent=:dc"
                + " and c.retired=:ret"
                + " order by c.orderNo";
        Map<String, Object> m = new HashMap<>();
        m.put("dc", cc.getDesignComponent());
        m.put("ret", false);
        return designComponentChoiceFacade.findByJpql(jpql, m);
    }


    public String navigateBackToAdmissionProfile() {
        return "/inward/admission_profile?faces-redirect=true";
    }

    public PatientEncounter getPatientEncounter() {
        return patientEncounter;
    }

    public void setPatientEncounter(PatientEncounter patientEncounter) {
        this.patientEncounter = patientEncounter;
    }

    public List<PatientFormEntry> getFormEntries() {
        if (formEntries == null) {
            formEntries = new ArrayList<>();
        }
        return formEntries;
    }

    public void setFormEntries(List<PatientFormEntry> formEntries) {
        this.formEntries = formEntries;
    }

    public PatientFormEntry getCurrentEntry() {
        return currentEntry;
    }

    public void setCurrentEntry(PatientFormEntry currentEntry) {
        this.currentEntry = currentEntry;
    }

    public List<CaptureComponent> getCurrentCaptureComponents() {
        return currentCaptureComponents;
    }

    public void setCurrentCaptureComponents(List<CaptureComponent> currentCaptureComponents) {
        this.currentCaptureComponents = currentCaptureComponents;
    }

    public List<DesignComponent> getAvailableTemplates() {
        if (availableTemplates == null) {
            loadAvailableTemplates();
        }
        return availableTemplates;
    }

    public void setAvailableTemplates(List<DesignComponent> availableTemplates) {
        this.availableTemplates = availableTemplates;
    }

    public DesignComponent getSelectedTemplate() {
        return selectedTemplate;
    }

    public void setSelectedTemplate(DesignComponent selectedTemplate) {
        this.selectedTemplate = selectedTemplate;
    }
}
