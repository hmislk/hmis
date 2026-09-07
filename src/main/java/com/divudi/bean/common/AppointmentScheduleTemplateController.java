package com.divudi.bean.common;

import com.divudi.core.data.AppointmentScheduleDateOverrideType;
import com.divudi.core.data.AppointmentScheduleType;
import com.divudi.core.data.AppointmentStatus;
import com.divudi.core.entity.AppointmentScheduleDateOverride;
import com.divudi.core.entity.AppointmentScheduleInstance;
import com.divudi.core.entity.AppointmentScheduleTemplate;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.Staff;
import com.divudi.core.entity.inward.RoomFacilityCharge;
import com.divudi.core.facade.AppointmentScheduleDateOverrideFacade;
import com.divudi.core.facade.AppointmentScheduleInstanceFacade;
import com.divudi.core.facade.AppointmentScheduleTemplateFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

@Named
@SessionScoped
public class AppointmentScheduleTemplateController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private AppointmentScheduleTemplateFacade templateFacade;
    @EJB
    private AppointmentScheduleInstanceFacade instanceFacade;
    @EJB
    private AppointmentScheduleDateOverrideFacade overrideFacade;

    @Inject
    private SessionController sessionController;

    private AppointmentScheduleTemplate currentTemplate;
    private AppointmentScheduleDateOverride currentOverride;
    private AppointmentScheduleTemplate selectedTemplateForOverrides;
    private List<AppointmentScheduleTemplate> templates;
    private List<AppointmentScheduleDateOverride> dateOverrides;

    // Navigation methods
    public String navigateToManageTemplates() {
        currentTemplate = new AppointmentScheduleTemplate();
        loadTemplates();
        return "/inward/appointment_schedule_templates?faces-redirect=true";
    }

    public String navigateToManageDateOverrides() {
        currentOverride = new AppointmentScheduleDateOverride();
        selectedTemplateForOverrides = null;
        dateOverrides = null;
        return "/inward/appointment_schedule_date_overrides?faces-redirect=true";
    }

    // Template CRUD
    public void saveTemplate() {
        if (currentTemplate == null) {
            JsfUtil.addErrorMessage("No template to save.");
            return;
        }
        if (currentTemplate.getName() == null || currentTemplate.getName().trim().isEmpty()) {
            JsfUtil.addErrorMessage("Template name is required.");
            return;
        }
        if (currentTemplate.getScheduleType() == null) {
            JsfUtil.addErrorMessage("Schedule type is required.");
            return;
        }
        if (currentTemplate.getMaxCount() <= 0) {
            JsfUtil.addErrorMessage("Max count must be greater than zero.");
            return;
        }

        if (currentTemplate.getId() == null) {
            currentTemplate.setCreater(sessionController.getLoggedUser());
            currentTemplate.setCreatedAt(new Date());
            currentTemplate.setActive(true);
            templateFacade.create(currentTemplate);
            JsfUtil.addSuccessMessage("Template saved.");
        } else {
            templateFacade.edit(currentTemplate);
            JsfUtil.addSuccessMessage("Template updated.");
        }
        currentTemplate = new AppointmentScheduleTemplate();
        loadTemplates();
    }

    public void editTemplate(AppointmentScheduleTemplate template) {
        this.currentTemplate = template;
    }

    public void retireTemplate(AppointmentScheduleTemplate template) {
        if (template == null) {
            return;
        }
        template.setRetired(true);
        template.setRetirer(sessionController.getLoggedUser());
        template.setRetiredAt(new Date());
        templateFacade.edit(template);
        JsfUtil.addSuccessMessage("Template retired.");
        loadTemplates();
    }

    private void loadTemplates() {
        String jpql = "SELECT t FROM AppointmentScheduleTemplate t WHERE t.retired = false ORDER BY t.name";
        templates = templateFacade.findByJpql(jpql);
    }

    // Date Override CRUD
    public void saveDateOverride() {
        if (currentOverride == null) {
            JsfUtil.addErrorMessage("No override to save.");
            return;
        }
        if (currentOverride.getTemplate() == null) {
            JsfUtil.addErrorMessage("Please select a template.");
            return;
        }
        if (currentOverride.getOverrideDate() == null) {
            JsfUtil.addErrorMessage("Override date is required.");
            return;
        }
        if (currentOverride.getOverrideType() == null) {
            JsfUtil.addErrorMessage("Override type is required.");
            return;
        }

        if (currentOverride.getId() == null) {
            currentOverride.setCreater(sessionController.getLoggedUser());
            currentOverride.setCreatedAt(new Date());
            overrideFacade.create(currentOverride);
            JsfUtil.addSuccessMessage("Date override saved.");
        } else {
            overrideFacade.edit(currentOverride);
            JsfUtil.addSuccessMessage("Date override updated.");
        }
        currentOverride = new AppointmentScheduleDateOverride();
        if (selectedTemplateForOverrides != null) {
            currentOverride.setTemplate(selectedTemplateForOverrides);
        }
        loadDateOverrides();
    }

    public void editDateOverride(AppointmentScheduleDateOverride override) {
        this.currentOverride = override;
    }

    public void retireDateOverride(AppointmentScheduleDateOverride override) {
        if (override == null) {
            return;
        }
        override.setRetired(true);
        override.setRetirer(sessionController.getLoggedUser());
        override.setRetiredAt(new Date());
        overrideFacade.edit(override);
        JsfUtil.addSuccessMessage("Date override retired.");
        loadDateOverrides();
    }

    public void onTemplateSelectedForOverrides() {
        if (selectedTemplateForOverrides != null) {
            currentOverride = new AppointmentScheduleDateOverride();
            currentOverride.setTemplate(selectedTemplateForOverrides);
            loadDateOverrides();
        } else {
            dateOverrides = null;
        }
    }

    private void loadDateOverrides() {
        if (selectedTemplateForOverrides == null) {
            dateOverrides = null;
            return;
        }
        String jpql = "SELECT o FROM AppointmentScheduleDateOverride o "
                + "WHERE o.template = :template AND o.retired = false "
                + "ORDER BY o.overrideDate";
        Map<String, Object> params = new HashMap<>();
        params.put("template", selectedTemplateForOverrides);
        dateOverrides = overrideFacade.findByJpql(jpql, params);
    }

    // Instance management
    public AppointmentScheduleInstance findOrCreateInstance(AppointmentScheduleTemplate template, Date date) {
        if (template == null || date == null) {
            return null;
        }

        // Check for existing instance
        String jpql = "SELECT i FROM AppointmentScheduleInstance i "
                + "WHERE i.template = :template AND i.instanceDate = :date "
                + "AND i.retired = false AND i.cancelled = false";
        Map<String, Object> params = new HashMap<>();
        params.put("template", template);
        params.put("date", date);
        AppointmentScheduleInstance existing = instanceFacade.findFirstByJpql(jpql, params);
        if (existing != null) {
            return existing;
        }

        // Check for date override
        AppointmentScheduleDateOverride override = findDateOverride(template, date);
        if (override != null && override.getOverrideType() == AppointmentScheduleDateOverrideType.BLOCKED) {
            return null; // Blocked date
        }

        // Create new instance
        AppointmentScheduleInstance instance = new AppointmentScheduleInstance();
        instance.setTemplate(template);
        instance.setInstanceDate(date);
        instance.setCreater(sessionController.getLoggedUser());
        instance.setCreatedAt(new Date());

        if (override != null) {
            instance.setMaxCount(override.getOverriddenMaxCount() != null
                    ? override.getOverriddenMaxCount() : template.getMaxCount());
            instance.setStartTime(override.getOverriddenStartTime() != null
                    ? override.getOverriddenStartTime() : template.getStartTime());
            instance.setEndTime(override.getOverriddenEndTime() != null
                    ? override.getOverriddenEndTime() : template.getEndTime());
        } else {
            instance.setMaxCount(template.getMaxCount());
            instance.setStartTime(template.getStartTime());
            instance.setEndTime(template.getEndTime());
        }

        instance.setBookedCount(0);
        instanceFacade.create(instance);
        return instance;
    }

    private AppointmentScheduleDateOverride findDateOverride(AppointmentScheduleTemplate template, Date date) {
        String jpql = "SELECT o FROM AppointmentScheduleDateOverride o "
                + "WHERE o.template = :template AND o.overrideDate = :date AND o.retired = false";
        Map<String, Object> params = new HashMap<>();
        params.put("template", template);
        params.put("date", date);
        return overrideFacade.findFirstByJpql(jpql, params);
    }

    // Query methods for booking flow
    public List<AppointmentScheduleInstance> findAvailableInstances(Date date,
            Item procedure, Staff consultant, Department dept, RoomFacilityCharge room) {
        if (date == null) {
            return new ArrayList<>();
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int weekday = cal.get(Calendar.DAY_OF_WEEK);

        // Find matching templates
        List<AppointmentScheduleTemplate> matchingTemplates = findMatchingTemplates(weekday, date, procedure, consultant, dept, room);

        List<AppointmentScheduleInstance> instances = new ArrayList<>();
        for (AppointmentScheduleTemplate t : matchingTemplates) {
            AppointmentScheduleInstance inst = findOrCreateInstance(t, date);
            if (inst != null) {
                // Refresh booked count transiently (no DB write on read path)
                inst.setBookedCount(countActiveAppointments(inst));
                instances.add(inst);
            }
        }
        return instances;
    }

    private List<AppointmentScheduleTemplate> findMatchingTemplates(int weekday, Date date,
            Item procedure, Staff consultant, Department dept, RoomFacilityCharge room) {
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT t FROM AppointmentScheduleTemplate t ");
        jpql.append("WHERE t.retired = false AND t.active = true ");
        jpql.append("AND t.sessionWeekday = :weekday ");

        Map<String, Object> params = new HashMap<>();
        params.put("weekday", weekday);

        if (procedure != null) {
            jpql.append("AND t.procedure = :procedure ");
            params.put("procedure", procedure);
        }
        if (consultant != null) {
            jpql.append("AND t.consultant = :consultant ");
            params.put("consultant", consultant);
        }
        if (dept != null) {
            jpql.append("AND t.department = :dept ");
            params.put("dept", dept);
        }
        if (room != null) {
            jpql.append("AND t.room = :room ");
            params.put("room", room);
        }

        jpql.append("AND (t.effectiveFrom IS NULL OR t.effectiveFrom <= :date) ");
        jpql.append("AND (t.effectiveTo IS NULL OR t.effectiveTo >= :date) ");
        params.put("date", date);

        return templateFacade.findByJpql(jpql.toString(), params);
    }

    public int countActiveAppointments(AppointmentScheduleInstance instance) {
        if (instance == null || instance.getId() == null) {
            return 0;
        }
        String jpql = "SELECT COUNT(a) FROM Appointment a "
                + "WHERE a.scheduleInstance = :si AND a.retired = false "
                + "AND a.status = :status";
        Map<String, Object> params = new HashMap<>();
        params.put("si", instance);
        params.put("status", AppointmentStatus.PENDING);
        Long count = instanceFacade.countByJpql(jpql, params);
        return count != null ? count.intValue() : 0;
    }

    public boolean hasTimeOverlap(AppointmentScheduleInstance instance, Date fromTime, Date toTime, Long excludeAppointmentId) {
        if (instance == null || instance.getId() == null || fromTime == null || toTime == null) {
            return false;
        }
        StringBuilder jpql = new StringBuilder();
        jpql.append("SELECT COUNT(a) FROM Appointment a ");
        jpql.append("WHERE a.scheduleInstance = :si AND a.retired = false ");
        jpql.append("AND a.status = :status ");
        jpql.append("AND a.appointmentTimeFrom < :toTime AND a.appointmentTimeTo > :fromTime ");
        Map<String, Object> params = new HashMap<>();
        params.put("si", instance);
        params.put("status", AppointmentStatus.PENDING);
        params.put("toTime", toTime);
        params.put("fromTime", fromTime);
        if (excludeAppointmentId != null) {
            jpql.append("AND a.id != :excludeId ");
            params.put("excludeId", excludeAppointmentId);
        }
        Long count = instanceFacade.countByJpql(jpql.toString(), params);
        return count != null && count > 0;
    }

    // Autocomplete for templates
    public List<AppointmentScheduleTemplate> completeTemplate(String query) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String jpql = "SELECT t FROM AppointmentScheduleTemplate t "
                + "WHERE t.retired = false AND t.active = true "
                + "AND t.name LIKE :qry ORDER BY t.name";
        Map<String, Object> params = new HashMap<>();
        params.put("qry", "%" + query.trim() + "%");
        return templateFacade.findByJpql(jpql, params);
    }

    // Enum value providers for XHTML
    public AppointmentScheduleType[] getScheduleTypes() {
        return AppointmentScheduleType.values();
    }

    public AppointmentScheduleDateOverrideType[] getOverrideTypes() {
        return AppointmentScheduleDateOverrideType.values();
    }

    // Getters and setters
    public AppointmentScheduleTemplate getCurrentTemplate() {
        if (currentTemplate == null) {
            currentTemplate = new AppointmentScheduleTemplate();
        }
        return currentTemplate;
    }

    public void setCurrentTemplate(AppointmentScheduleTemplate currentTemplate) {
        this.currentTemplate = currentTemplate;
    }

    public AppointmentScheduleDateOverride getCurrentOverride() {
        if (currentOverride == null) {
            currentOverride = new AppointmentScheduleDateOverride();
        }
        return currentOverride;
    }

    public void setCurrentOverride(AppointmentScheduleDateOverride currentOverride) {
        this.currentOverride = currentOverride;
    }

    public AppointmentScheduleTemplate getSelectedTemplateForOverrides() {
        return selectedTemplateForOverrides;
    }

    public void setSelectedTemplateForOverrides(AppointmentScheduleTemplate selectedTemplateForOverrides) {
        this.selectedTemplateForOverrides = selectedTemplateForOverrides;
    }

    public List<AppointmentScheduleTemplate> getTemplates() {
        return templates;
    }

    public void setTemplates(List<AppointmentScheduleTemplate> templates) {
        this.templates = templates;
    }

    public List<AppointmentScheduleDateOverride> getDateOverrides() {
        return dateOverrides;
    }

    public void setDateOverrides(List<AppointmentScheduleDateOverride> dateOverrides) {
        this.dateOverrides = dateOverrides;
    }

    public AppointmentScheduleInstanceFacade getInstanceFacade() {
        return instanceFacade;
    }

    @FacesConverter(forClass = AppointmentScheduleTemplate.class)
    public static class AppointmentScheduleTemplateConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                AppointmentScheduleTemplateController controller = (AppointmentScheduleTemplateController) facesContext.getApplication().getELResolver()
                        .getValue(facesContext.getELContext(), null, "appointmentScheduleTemplateController");
                return controller.templateFacade.find(Long.valueOf(value));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return "";
            }
            if (object instanceof AppointmentScheduleTemplate) {
                AppointmentScheduleTemplate t = (AppointmentScheduleTemplate) object;
                return t.getId() != null ? t.getId().toString() : "";
            }
            return "";
        }
    }

    @FacesConverter(forClass = AppointmentScheduleInstance.class)
    public static class AppointmentScheduleInstanceConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                AppointmentScheduleTemplateController controller = (AppointmentScheduleTemplateController) facesContext.getApplication().getELResolver()
                        .getValue(facesContext.getELContext(), null, "appointmentScheduleTemplateController");
                return controller.instanceFacade.find(Long.valueOf(value));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return "";
            }
            if (object instanceof AppointmentScheduleInstance) {
                AppointmentScheduleInstance i = (AppointmentScheduleInstance) object;
                return i.getId() != null ? i.getId().toString() : "";
            }
            return "";
        }
    }
}
