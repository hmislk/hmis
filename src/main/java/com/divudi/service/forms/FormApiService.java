package com.divudi.service.forms;

import com.divudi.core.data.dto.forms.FormChoiceDto;
import com.divudi.core.data.dto.forms.FormEntryDto;
import com.divudi.core.data.dto.forms.FormFieldDto;
import com.divudi.core.data.dto.forms.FormTemplateDto;
import com.divudi.core.data.dto.forms.FormValueDto;
import com.divudi.core.data.web.ComponentDataType;
import com.divudi.core.data.web.ComponentPresentationType;
import com.divudi.core.entity.WebUser;
import com.divudi.core.entity.inward.PatientFormEntry;
import com.divudi.core.entity.web.CaptureComponent;
import com.divudi.core.entity.web.DesignComponent;
import com.divudi.core.entity.web.DesignComponentChoice;
import com.divudi.core.facade.PatientFormEntryFacade;
import com.divudi.core.facade.web.CaptureComponentFacade;
import com.divudi.core.facade.web.DesignComponentChoiceFacade;
import com.divudi.core.facade.web.DesignComponentFacade;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Stateless
public class FormApiService {

    @EJB
    private DesignComponentFacade designComponentFacade;

    @EJB
    private DesignComponentChoiceFacade designComponentChoiceFacade;

    @EJB
    private PatientFormEntryFacade patientFormEntryFacade;

    @EJB
    private CaptureComponentFacade captureComponentFacade;

    // -------------------------------------------------------------------------
    // Form Templates
    // -------------------------------------------------------------------------

    public List<FormTemplateDto> listFormTemplates() {
        String jpql = "SELECT d FROM DesignComponent d WHERE d.retired = false AND d.componentPresentationType = :cpt AND d.dataEntryForm IS NULL ORDER BY d.name";
        Map<String, Object> params = new HashMap<>();
        params.put("cpt", ComponentPresentationType.DataEntryForm);
        List<DesignComponent> forms = designComponentFacade.findByJpql(jpql, params);
        List<FormTemplateDto> result = new ArrayList<>();
        for (DesignComponent dc : forms) {
            int fieldCount = countFields(dc);
            result.add(toTemplateDto(dc, fieldCount));
        }
        return result;
    }

    public FormTemplateDto getFormTemplateWithFields(Long id) {
        DesignComponent dc = designComponentFacade.find(id);
        if (dc == null || dc.isRetired()) {
            throw new IllegalArgumentException("Form template not found: " + id);
        }
        FormTemplateDto dto = toTemplateDto(dc, countFields(dc));
        return dto;
    }

    public FormTemplateDto createFormTemplate(String name, String description, String formCssClass, WebUser user) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        DesignComponent dc = new DesignComponent();
        dc.setName(name.trim());
        dc.setDescription(description);
        dc.setFormCssClass(formCssClass);
        dc.setComponentPresentationType(ComponentPresentationType.DataEntryForm);
        dc.setRetired(false);
        designComponentFacade.create(dc);
        designComponentFacade.flush();
        return toTemplateDto(dc, 0);
    }

    public FormTemplateDto updateFormTemplate(Long id, String name, String description, String formCssClass, WebUser user) {
        DesignComponent dc = designComponentFacade.find(id);
        if (dc == null || dc.isRetired()) {
            throw new IllegalArgumentException("Form template not found: " + id);
        }
        if (name != null && !name.trim().isEmpty()) {
            dc.setName(name.trim());
        }
        if (description != null) {
            dc.setDescription(description);
        }
        if (formCssClass != null) {
            dc.setFormCssClass(formCssClass);
        }
        designComponentFacade.edit(dc);
        return toTemplateDto(dc, countFields(dc));
    }

    public void retireFormTemplate(Long id, String retireComments, WebUser user) {
        DesignComponent dc = designComponentFacade.find(id);
        if (dc == null || dc.isRetired()) {
            throw new IllegalArgumentException("Form template not found: " + id);
        }
        dc.setRetired(true);
        designComponentFacade.edit(dc);
    }

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    public List<FormFieldDto> listFormFields(Long formId) {
        DesignComponent form = designComponentFacade.find(formId);
        if (form == null || form.isRetired()) {
            throw new IllegalArgumentException("Form template not found: " + formId);
        }
        String jpql = "SELECT d FROM DesignComponent d WHERE d.retired = false AND d.dataEntryForm = :form ORDER BY d.orderNo";
        Map<String, Object> params = new HashMap<>();
        params.put("form", form);
        List<DesignComponent> fields = designComponentFacade.findByJpql(jpql, params);
        List<FormFieldDto> result = new ArrayList<>();
        for (DesignComponent f : fields) {
            result.add(toFieldDto(f));
        }
        return result;
    }

    public FormFieldDto addFormField(Long formId, Map<String, Object> body, WebUser user) {
        DesignComponent form = designComponentFacade.find(formId);
        if (form == null || form.isRetired()) {
            throw new IllegalArgumentException("Form template not found: " + formId);
        }
        String name = (String) body.get("name");
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }
        DesignComponent field = new DesignComponent();
        field.setName(name.trim());
        field.setCode((String) body.get("code"));
        field.setDescription((String) body.get("description"));
        field.setDataEntryForm(form);
        field.setRetired(false);

        String cpt = (String) body.get("componentPresentationType");
        if (cpt != null && !cpt.trim().isEmpty()) {
            try {
                field.setComponentPresentationType(ComponentPresentationType.valueOf(cpt.trim()));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid componentPresentationType: " + cpt.trim());
            }
        }
        String cdt = (String) body.get("componentDataType");
        if (cdt != null && !cdt.trim().isEmpty()) {
            try {
                field.setComponentDataType(ComponentDataType.valueOf(cdt.trim()));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid componentDataType: " + cdt.trim());
            }
        }
        if (body.get("orderNo") != null) {
            field.setOrderNo(toInteger(body.get("orderNo")));
        }
        if (body.get("required") != null) {
            field.setRequired(Boolean.parseBoolean(body.get("required").toString()));
        }
        field.setPlaceholder((String) body.get("placeholder"));
        field.setMinValue(toDouble(body.get("minValue")));
        field.setMaxValue(toDouble(body.get("maxValue")));
        field.setStepSize(toDouble(body.get("stepSize")));
        field.setMaxRating(toInteger(body.get("maxRating")));
        field.setOnLabel((String) body.get("onLabel"));
        field.setOffLabel((String) body.get("offLabel"));
        field.setEditHtml((String) body.get("editHtml"));
        field.setViewHtml((String) body.get("viewHtml"));
        designComponentFacade.create(field);
        designComponentFacade.flush();
        return toFieldDto(field);
    }

    public FormFieldDto updateFormField(Long fieldId, Map<String, Object> body, WebUser user) {
        DesignComponent field = designComponentFacade.find(fieldId);
        if (field == null || field.isRetired()) {
            throw new IllegalArgumentException("Field not found: " + fieldId);
        }
        if (body.containsKey("name") && body.get("name") != null) {
            field.setName(body.get("name").toString().trim());
        }
        if (body.containsKey("code")) {
            field.setCode((String) body.get("code"));
        }
        if (body.containsKey("description")) {
            field.setDescription((String) body.get("description"));
        }
        if (body.containsKey("componentPresentationType") && body.get("componentPresentationType") != null) {
            try {
                field.setComponentPresentationType(ComponentPresentationType.valueOf(body.get("componentPresentationType").toString().trim()));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid componentPresentationType: " + body.get("componentPresentationType"));
            }
        }
        if (body.containsKey("componentDataType") && body.get("componentDataType") != null) {
            try {
                field.setComponentDataType(ComponentDataType.valueOf(body.get("componentDataType").toString().trim()));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Invalid componentDataType: " + body.get("componentDataType"));
            }
        }
        if (body.containsKey("orderNo")) {
            field.setOrderNo(toInteger(body.get("orderNo")));
        }
        if (body.containsKey("required")) {
            field.setRequired(Boolean.parseBoolean(body.get("required").toString()));
        }
        if (body.containsKey("placeholder")) {
            field.setPlaceholder((String) body.get("placeholder"));
        }
        if (body.containsKey("minValue")) {
            field.setMinValue(toDouble(body.get("minValue")));
        }
        if (body.containsKey("maxValue")) {
            field.setMaxValue(toDouble(body.get("maxValue")));
        }
        if (body.containsKey("stepSize")) {
            field.setStepSize(toDouble(body.get("stepSize")));
        }
        if (body.containsKey("maxRating")) {
            field.setMaxRating(toInteger(body.get("maxRating")));
        }
        if (body.containsKey("onLabel")) {
            field.setOnLabel((String) body.get("onLabel"));
        }
        if (body.containsKey("offLabel")) {
            field.setOffLabel((String) body.get("offLabel"));
        }
        if (body.containsKey("editHtml")) {
            field.setEditHtml((String) body.get("editHtml"));
        }
        if (body.containsKey("viewHtml")) {
            field.setViewHtml((String) body.get("viewHtml"));
        }
        designComponentFacade.edit(field);
        return toFieldDto(field);
    }

    public void retireFormField(Long fieldId, WebUser user) {
        DesignComponent field = designComponentFacade.find(fieldId);
        if (field == null || field.isRetired()) {
            throw new IllegalArgumentException("Field not found: " + fieldId);
        }
        field.setRetired(true);
        designComponentFacade.edit(field);
    }

    // -------------------------------------------------------------------------
    // Choices
    // -------------------------------------------------------------------------

    public List<FormChoiceDto> listChoices(Long fieldId) {
        DesignComponent field = designComponentFacade.find(fieldId);
        if (field == null || field.isRetired()) {
            throw new IllegalArgumentException("Field not found: " + fieldId);
        }
        String jpql = "SELECT c FROM DesignComponentChoice c WHERE c.retired = false AND c.designComponent = :field ORDER BY c.orderNo";
        Map<String, Object> params = new HashMap<>();
        params.put("field", field);
        List<DesignComponentChoice> choices = designComponentChoiceFacade.findByJpql(jpql, params);
        List<FormChoiceDto> result = new ArrayList<>();
        for (DesignComponentChoice ch : choices) {
            result.add(new FormChoiceDto(ch.getId(), ch.getLabel(), ch.getValue(), ch.getOrderNo()));
        }
        return result;
    }

    public FormChoiceDto addChoice(Long fieldId, String label, String value, Integer orderNo, WebUser user) {
        DesignComponent field = designComponentFacade.find(fieldId);
        if (field == null || field.isRetired()) {
            throw new IllegalArgumentException("Field not found: " + fieldId);
        }
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("label is required");
        }
        DesignComponentChoice ch = new DesignComponentChoice();
        ch.setDesignComponent(field);
        ch.setLabel(label.trim());
        ch.setValue(value != null ? value : label.trim());
        ch.setOrderNo(orderNo);
        ch.setRetired(false);
        designComponentChoiceFacade.create(ch);
        designComponentChoiceFacade.flush();
        return new FormChoiceDto(ch.getId(), ch.getLabel(), ch.getValue(), ch.getOrderNo());
    }

    public FormChoiceDto updateChoice(Long choiceId, String label, String value, Integer orderNo, WebUser user) {
        DesignComponentChoice ch = designComponentChoiceFacade.find(choiceId);
        if (ch == null || ch.isRetired()) {
            throw new IllegalArgumentException("Choice not found: " + choiceId);
        }
        if (label != null && !label.trim().isEmpty()) {
            ch.setLabel(label.trim());
        }
        if (value != null) {
            ch.setValue(value);
        }
        if (orderNo != null) {
            ch.setOrderNo(orderNo);
        }
        designComponentChoiceFacade.edit(ch);
        return new FormChoiceDto(ch.getId(), ch.getLabel(), ch.getValue(), ch.getOrderNo());
    }

    public void retireChoice(Long choiceId, WebUser user) {
        DesignComponentChoice ch = designComponentChoiceFacade.find(choiceId);
        if (ch == null || ch.isRetired()) {
            throw new IllegalArgumentException("Choice not found: " + choiceId);
        }
        ch.setRetired(true);
        designComponentChoiceFacade.edit(ch);
    }

    // -------------------------------------------------------------------------
    // Entries and Values
    // -------------------------------------------------------------------------

    public List<FormEntryDto> listEntriesForAdmission(Long admissionId) {
        String jpql = "SELECT e FROM PatientFormEntry e WHERE e.retired = false AND e.patientEncounter.id = :admId ORDER BY e.createdAt DESC";
        Map<String, Object> params = new HashMap<>();
        params.put("admId", admissionId);
        List<PatientFormEntry> entries = patientFormEntryFacade.findByJpql(jpql, params);
        List<FormEntryDto> result = new ArrayList<>();
        for (PatientFormEntry e : entries) {
            result.add(toEntryDto(e));
        }
        return result;
    }

    public List<FormValueDto> listValuesForEntry(Long entryId) {
        PatientFormEntry entry = patientFormEntryFacade.find(entryId);
        if (entry == null || entry.isRetired()) {
            throw new IllegalArgumentException("Entry not found: " + entryId);
        }
        String jpql = "SELECT cc FROM CaptureComponent cc WHERE cc.retired = false AND cc.patientFormEntry = :entry ORDER BY cc.id";
        Map<String, Object> params = new HashMap<>();
        params.put("entry", entry);
        List<CaptureComponent> components = captureComponentFacade.findByJpql(jpql, params);
        List<FormValueDto> result = new ArrayList<>();
        for (CaptureComponent cc : components) {
            result.add(toValueDto(cc));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private int countFields(DesignComponent form) {
        String jpql = "SELECT COUNT(d) FROM DesignComponent d WHERE d.retired = false AND d.dataEntryForm = :form";
        Map<String, Object> params = new HashMap<>();
        params.put("form", form);
        Long count = designComponentFacade.findLongByJpql(jpql, params);
        return count != null ? count.intValue() : 0;
    }

    private FormTemplateDto toTemplateDto(DesignComponent dc, int fieldCount) {
        return new FormTemplateDto(
                dc.getId(),
                dc.getName(),
                dc.getDescription(),
                dc.getFormCssClass(),
                fieldCount);
    }

    private FormFieldDto toFieldDto(DesignComponent dc) {
        FormFieldDto dto = new FormFieldDto();
        dto.setId(dc.getId());
        dto.setName(dc.getName());
        dto.setCode(dc.getCode());
        dto.setDescription(dc.getDescription());
        dto.setComponentPresentationType(dc.getComponentPresentationType() != null ? dc.getComponentPresentationType().name() : null);
        dto.setComponentDataType(dc.getComponentDataType() != null ? dc.getComponentDataType().name() : null);
        dto.setOrderNo(dc.getOrderNo());
        dto.setRequired(dc.isRequired());
        dto.setPlaceholder(dc.getPlaceholder());
        dto.setMinValue(dc.getMinValue());
        dto.setMaxValue(dc.getMaxValue());
        dto.setStepSize(dc.getStepSize());
        dto.setMaxRating(dc.getMaxRating());
        dto.setOnLabel(dc.getOnLabel());
        dto.setOffLabel(dc.getOffLabel());
        dto.setEditHtml(dc.getEditHtml());
        dto.setViewHtml(dc.getViewHtml());
        return dto;
    }

    private FormEntryDto toEntryDto(PatientFormEntry e) {
        FormEntryDto dto = new FormEntryDto();
        dto.setId(e.getId());
        if (e.getDesignComponent() != null) {
            dto.setFormTemplateId(e.getDesignComponent().getId());
            dto.setFormTemplateName(e.getDesignComponent().getName());
        }
        dto.setComments(e.getComments());
        dto.setCreatedAt(e.getCreatedAt());
        if (e.getCreater() != null && e.getCreater().getWebUserPerson() != null) {
            dto.setCreatedBy(e.getCreater().getWebUserPerson().getName());
        }
        return dto;
    }

    private FormValueDto toValueDto(CaptureComponent cc) {
        FormValueDto dto = new FormValueDto();
        dto.setId(cc.getId());
        if (cc.getDesignComponent() != null) {
            dto.setFieldId(cc.getDesignComponent().getId());
            dto.setFieldName(cc.getDesignComponent().getName());
        }
        dto.setComponentPresentationType(cc.getComponentPresentationType() != null ? cc.getComponentPresentationType().name() : null);
        dto.setShortTextValue(cc.getShortTextValue());
        dto.setLongTextValue(cc.getLongTextValue());
        dto.setDoubleValue(cc.getDoubleValue());
        dto.setIntValue(cc.getIntValue());
        dto.setBooleanValue(cc.getBooleanValue());
        dto.setDateValue(cc.getDateValue());
        dto.setRatingIntValue(cc.getRatingIntValue());
        if (cc.getLongTextValue() != null && !cc.getLongTextValue().trim().isEmpty()) {
            dto.setSelectedValues(Arrays.asList(cc.getLongTextValue().split("\\|")));
        }
        return dto;
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer toInteger(Object val) {
        if (val == null) return null;
        try {
            return (int) Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
