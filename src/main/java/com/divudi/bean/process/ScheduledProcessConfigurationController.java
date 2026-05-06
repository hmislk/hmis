package com.divudi.bean.process;

import com.divudi.core.entity.ScheduledProcessConfiguration;
import com.divudi.core.facade.ScheduledProcessConfigurationFacade;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.common.SessionController;
import com.divudi.service.ScheduledProcessService;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.TemporalType;

@Named
@SessionScoped
public class ScheduledProcessConfigurationController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ScheduledProcessConfigurationFacade scheduledProcessConfigurationFacade;

    @Inject
    private SessionController sessionController;

    @EJB
    private ScheduledProcessService scheduledProcessService;

    private List<ScheduledProcessConfiguration> items;
    private ScheduledProcessConfiguration current;
    private boolean editable;

    public String navigateToManageScheduledProcessConfigurations() {
        fillItems();
        return "/process/admin/scheduled_process_configurations?faces-redirect=true";
    }

    public void fillItems() {
        String jpql = "select c from ScheduledProcessConfiguration c where c.retired=false order by c.scheduledProcess";
        items = scheduledProcessConfigurationFacade.findByJpql(jpql, new HashMap<>(), TemporalType.TIMESTAMP);
    }

    public void prepareAdd() {
        current = new ScheduledProcessConfiguration();
        editable = true;
    }

    public void edit() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select one to edit");
            return;
        }
        editable = true;
    }

    public void cancel() {
        current = null;
        editable = false;
    }

    public void save() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        if (current.getId() == null) {
            current.setCreatedAt(new Date());
            current.setCreatedBy(sessionController.getLoggedUser());
            scheduledProcessConfigurationFacade.create(current);
            JsfUtil.addSuccessMessage("Saved");
        } else {
            scheduledProcessConfigurationFacade.edit(current);
            JsfUtil.addSuccessMessage("Updated");
        }
        fillItems();
        editable = false;
    }

    public void delete() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to delete");
            return;
        }
        current.setRetired(true);
        current.setRetiredAt(new Date());
        current.setRetiredBy(sessionController.getLoggedUser());
        scheduledProcessConfigurationFacade.edit(current);
        JsfUtil.addSuccessMessage("Deleted");
        fillItems();
        current = null;
        editable = false;
    }

    public void runLastScheduled() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        scheduledProcessService.executeScheduledProcessManual(current, false);
        JsfUtil.addSuccessMessage("Process executed for last schedule");
    }

    public void runNextScheduled() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return;
        }
        scheduledProcessService.executeScheduledProcessManual(current, true);
        JsfUtil.addSuccessMessage("Process executed for next schedule");
    }

    public List<ScheduledProcessConfiguration> getItems() {
        if (items == null) {
            fillItems();
        }
        return items;
    }

    public ScheduledProcessConfiguration getCurrent() {
        return current;
    }

    public void setCurrent(ScheduledProcessConfiguration current) {
        this.current = current;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public ScheduledProcessConfigurationFacade getScheduledProcessConfigurationFacade() {
        return scheduledProcessConfigurationFacade;
    }

    @FacesConverter(forClass = ScheduledProcessConfiguration.class)
    public static class ScheduledProcessConfigurationConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            ScheduledProcessConfigurationController controller = (ScheduledProcessConfigurationController) facesContext.getApplication().getELResolver()
                    .getValue(facesContext.getELContext(), null, "scheduledProcessConfigurationController");
            return controller.getScheduledProcessConfigurationFacade().find(Long.valueOf(value));
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return "";
            }
            if (object instanceof ScheduledProcessConfiguration) {
                return String.valueOf(((ScheduledProcessConfiguration) object).getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + ScheduledProcessConfiguration.class.getName());
            }
        }
    }
}
