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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    @EJB
    private com.divudi.core.facade.EmailFacade emailFacade;
    @EJB
    private com.divudi.ejb.EmailManagerEjb emailManagerEjb;

    @Inject
    private SessionController sessionController;

    private PatientEncounter patientEncounter;
    private List<PatientFormEntry> formEntries;
    private PatientFormEntry currentEntry;
    private List<CaptureComponent> currentCaptureComponents;
    private List<DesignComponent> availableTemplates;
    private DesignComponent selectedTemplate;
    private boolean viewMode = false;
    private String emailRecipient;

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
        viewMode = false;
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
        viewMode = false;
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
            cc.setEditHtml(field.getEditHtml());
            cc.setViewHtml(field.getViewHtml());
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
        viewMode = false;
    }

    public void editForm(PatientFormEntry entry) {
        if (entry == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        viewMode = true;
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

    public void prepareFormEmailDialog(PatientFormEntry entry) {
        if (entry == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        editForm(entry);
        emailRecipient = resolveDefaultEmailRecipient();
    }

    private String resolveDefaultEmailRecipient() {
        if (patientEncounter != null && patientEncounter.getPatient() != null
                && patientEncounter.getPatient().getPerson() != null
                && patientEncounter.getPatient().getPerson().getEmail() != null
                && !patientEncounter.getPatient().getPerson().getEmail().trim().isEmpty()) {
            return patientEncounter.getPatient().getPerson().getEmail().trim();
        }
        if (patientEncounter != null && patientEncounter.getGuardian() != null
                && patientEncounter.getGuardian().getEmail() != null
                && !patientEncounter.getGuardian().getEmail().trim().isEmpty()) {
            return patientEncounter.getGuardian().getEmail().trim();
        }
        return "";
    }

    public void sendFormEmail() {
        if (currentEntry == null || currentCaptureComponents == null) {
            JsfUtil.addErrorMessage("No form selected");
            return;
        }
        if (emailRecipient == null || emailRecipient.trim().isEmpty()) {
            JsfUtil.addErrorMessage("Please enter recipient email");
            return;
        }
        String recipient = emailRecipient.trim();
        if (!com.divudi.core.util.CommonFunctions.isValidEmail(recipient)) {
            JsfUtil.addErrorMessage("Please enter a valid email address");
            return;
        }

        String formName = currentEntry.getDesignComponent() != null
                ? currentEntry.getDesignComponent().getName() : "Inpatient Form";
        String body = buildFormEmailHtml(formName);

        com.divudi.core.entity.AppEmail email = new com.divudi.core.entity.AppEmail();
        email.setCreatedAt(new Date());
        email.setCreater(sessionController.getLoggedUser());
        email.setReceipientEmail(recipient);
        email.setMessageSubject(formName);
        email.setMessageBody(body);
        email.setDepartment(sessionController.getLoggedUser().getDepartment());
        email.setInstitution(sessionController.getLoggedUser().getInstitution());
        email.setPatientEncounter(patientEncounter);
        email.setMessageType(com.divudi.core.data.MessageType.InpatientFilledForm);
        email.setSentSuccessfully(false);
        email.setPending(true);
        emailFacade.create(email);

        try {
            boolean success = emailManagerEjb.sendEmail(
                    java.util.Collections.singletonList(recipient),
                    body,
                    formName,
                    true
            );
            email.setSentSuccessfully(success);
            email.setPending(!success);
            if (success) {
                email.setSentAt(new Date());
                JsfUtil.addSuccessMessage("Email Sent Successfully");
            } else {
                JsfUtil.addErrorMessage("Sending Email Failed");
            }
            emailFacade.edit(email);
        } catch (Exception ex) {
            JsfUtil.addErrorMessage("Sending Email Failed");
        }
    }

    /**
     * Builds an EmailAttachment from a filled form entry's captured field
     * values, for use by any compose flow that needs to attach this
     * admission's saved documents (loads the entry's components directly
     * rather than relying on currentEntry/currentCaptureComponents, so it's
     * safe to call without disturbing this controller's edit-mode state).
     */
    public com.divudi.core.data.EmailAttachment toEmailAttachment(PatientFormEntry entry) {
        if (entry == null || entry.getId() == null) {
            return null;
        }
        String jpql = "select cc "
                + " from CaptureComponent cc "
                + " where cc.patientFormEntry=:pfe "
                + " and cc.retired=:ret "
                + " order by cc.designComponent.orderNo";
        Map<String, Object> m = new HashMap<>();
        m.put("pfe", entry);
        m.put("ret", false);
        List<CaptureComponent> components = captureComponentFacade.findByJpql(jpql, m);
        if (components == null || components.isEmpty()) {
            return null;
        }
        String formName = entry.getDesignComponent() != null ? entry.getDesignComponent().getName() : "Inpatient Form";
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h3>").append(escapeHtml(formName)).append("</h3>");
        sb.append("<div class=\"row\">");
        for (CaptureComponent cc : components) {
            sb.append(resolveViewWrapper(cc));
        }
        sb.append("</div></body></html>");
        return new com.divudi.core.data.EmailAttachment(
                formName + ".html",
                "text/html",
                java.util.Base64.getEncoder().encodeToString(sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private String buildFormEmailHtml(String formName) {
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>");
        sb.append("<h3>").append(escapeHtml(formName)).append("</h3>");
        sb.append("<div class=\"row\">");
        for (CaptureComponent cc : currentCaptureComponents) {
            sb.append(resolveViewWrapper(cc));
        }
        sb.append("</div></body></html>");
        return sb.toString();
    }

    public String getEmailRecipient() {
        return emailRecipient;
    }

    public void setEmailRecipient(String emailRecipient) {
        this.emailRecipient = emailRecipient;
    }

    public void cancelForm() {
        currentEntry = null;
        currentCaptureComponents = null;
        selectedTemplate = null;
        viewMode = false;
    }

    public void toggleViewMode() {
        viewMode = !viewMode;
    }

    /**
     * Returns a display string for the stored value of a CaptureComponent,
     * used as the {{VALUE}} substitution in viewHtml wrappers.
     */
    public String resolveViewValue(CaptureComponent cc) {
        if (cc == null || cc.getComponentPresentationType() == null) {
            return "";
        }
        switch (cc.getComponentPresentationType()) {
            case Calendar:
                if (cc.getDateValue() == null) return "";
                return new SimpleDateFormat("dd MMM yyyy").format(cc.getDateValue());
            case SelectBooleanCheckBox:
            case SelectBooleanButton:
            case ToggleSwitch:
            case TriStateCheckBox:
                if (cc.getBooleanValue() == null) return "";
                return Boolean.TRUE.equals(cc.getBooleanValue()) ? "Yes" : "No";
            case SelectCheckBoxMenu:
            case SelectManyButton:
            case MultiSelectListBox: {
                List<String> vals = cc.getSelectedValues();
                if (vals == null || vals.isEmpty()) return "";
                List<DesignComponentChoice> choices = getChoicesFor(cc);
                return vals.stream()
                        .map(v -> choices.stream()
                                .filter(ch -> v.equals(ch.getValue()))
                                .map(DesignComponentChoice::getLabel)
                                .findFirst().orElse(v))
                        .collect(Collectors.joining(", "));
            }
            case Input_Number:
            case Spinner:
            case Slider:
                if (cc.getDoubleValue() != null) return String.valueOf(cc.getDoubleValue());
                if (cc.getIntValue() != null) return String.valueOf(cc.getIntValue());
                return "";
            case Rating:
                return cc.getRatingIntValue() != null ? String.valueOf(cc.getRatingIntValue()) : "";
            case Input_text:
            case SelectOneMenu:
            case SelectOneRadio:
            case SelectOneListBox:
            case AutoComplete:
                return cc.getShortTextValue() != null ? cc.getShortTextValue() : "";
            case Input_text_Area:
            case TextEditor:
                return cc.getLongTextValue() != null ? cc.getLongTextValue() : "";
            case Signature:
                return cc.getLongTextValue() != null && !cc.getLongTextValue().isEmpty()
                        ? "[Signature captured]" : "";
            default:
                return "";
        }
    }

    /**
     * Returns the HTML fragment that goes BEFORE the JSF input component in edit mode.
     * Splits editHtml at the {{INPUT}} marker; if no marker or no editHtml, returns the
     * default opening div + label.
     */
    public String resolveEditWrapperOpen(CaptureComponent cc) {
        String label = cc.getName() != null ? cc.getName() : "";
        String html = cc.getEditHtml();
        if (html == null || html.trim().isEmpty()) {
            return "<div class=\"col-12 col-md-6 mb-2\">"
                    + "<label class=\"form-label d-block\">" + escapeHtml(label) + "</label>";
        }
        String resolved = html.replace("{{LABEL}}", escapeHtml(label));
        int idx = resolved.indexOf("{{INPUT}}");
        if (idx < 0) {
            return resolved;
        }
        return resolved.substring(0, idx);
    }

    /**
     * Returns the HTML fragment that goes AFTER the JSF input component in edit mode.
     * Splits editHtml at the {{INPUT}} marker; if no marker or no editHtml, returns
     * the default closing div.
     */
    public String resolveEditWrapperClose(CaptureComponent cc) {
        String html = cc.getEditHtml();
        if (html == null || html.trim().isEmpty()) {
            return "</div>";
        }
        String label = cc.getName() != null ? cc.getName() : "";
        String resolved = html.replace("{{LABEL}}", escapeHtml(label));
        int idx = resolved.indexOf("{{INPUT}}");
        if (idx < 0) {
            return "";
        }
        return resolved.substring(idx + "{{INPUT}}".length());
    }

    /**
     * Substitutes {{LABEL}} and {{VALUE}} into viewHtml.
     * Falls back to a simple label + value display when viewHtml is blank.
     */
    public String resolveViewWrapper(CaptureComponent cc) {
        String label = cc.getName() != null ? cc.getName() : "";
        String value = resolveViewValue(cc);
        String html = cc.getViewHtml();
        if (html == null || html.trim().isEmpty()) {
            return "<div class=\"col-12 col-md-6 mb-2\">"
                    + "<div class=\"text-muted small\">" + escapeHtml(label) + "</div>"
                    + "<div class=\"fw-semibold\">" + escapeHtml(value) + "</div>"
                    + "</div>";
        }
        return html.replace("{{LABEL}}", escapeHtml(label)).replace("{{VALUE}}", escapeHtml(value));
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
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

    /**
     * Column count for a SelectOneRadio field's PrimeFaces grid layout,
     * derived from how many choices the field actually has (clamped so a
     * 2-choice field doesn't get 1 column and a 10-choice field doesn't
     * get 10 columns).
     */
    static int clampRadioColumns(int choiceCount) {
        return Math.max(1, Math.min(4, choiceCount));
    }

    public int getRadioColumns(CaptureComponent cc) {
        return clampRadioColumns(getChoicesFor(cc).size());
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

    public boolean isViewMode() {
        return viewMode;
    }

    public void setViewMode(boolean viewMode) {
        this.viewMode = viewMode;
    }
}
