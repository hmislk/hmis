package com.divudi.bean.process;

import com.divudi.core.entity.process.ProcessInstance;
import com.divudi.core.facade.ProcessInstanceFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Dr M H B Ariyaratne
 *
 */
@Named
@SessionScoped
public class ProcessInstanceController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ProcessInstanceFacade processInstanceFacade;

    private ProcessInstance current;
    private List<ProcessInstance> items = null;
    private boolean editable;
    private int index;

    public ProcessInstanceController() {
    }

    public ProcessInstance getCurrent() {
        return current;
    }

    public void setCurrent(ProcessInstance current) {
        this.current = current;
    }

    public List<ProcessInstance> getItems() {
        if (items == null) {
            fillAllProcesses();
        }
        return items;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public void fillAllProcesses() {
        items = processInstanceFacade.findAll();
    }

    public String addNewProcess() {
        current = new ProcessInstance();
        editable = true;
        return null;
    }

    public String editExistingProcess(ProcessInstance processInstance) {
        if (processInstance != null) {
            this.current = processInstance;
            this.editable = true;
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "No Process Selected", "Please select a process to edit."));
        }
        return null;
    }

    public String saveOrUpdateProcess() {
        try {
            if (current.getId() == null) {
                processInstanceFacade.create(current);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Process Created", "Process has been successfully created."));
            } else {
                processInstanceFacade.edit(current);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Process Updated", "Process has been successfully updated."));
            }
            items = null;
            editable = false;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "An error occurred while saving the process."));
        }
        return null;
    }

    public String deleteProcess(ProcessInstance processInstance) {
        if (processInstance != null) {
            try {
                processInstance.setRetired(true);
                processInstanceFacade.edit(processInstance);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Process Retired", "Process has been successfully retired."));
                items = null;
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "An error occurred while retiring the process."));
            }
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "No Process Selected", "Please select a process to retire."));
        }
        return null;
    }

    public void reset() {
        current = null;
        items = null;
        editable = false;
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Reset", "Process form has been reset."));
    }

    public ProcessInstanceFacade getProcessInstanceFacade() {
        return processInstanceFacade;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Converter class for ProcessInstance entities.
     */
    @FacesConverter(forClass = ProcessInstance.class)
    public static class ProcessInstanceConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            ProcessInstanceController controller = (ProcessInstanceController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "processInstanceController");
            return controller.getProcessInstanceFacade().find(Long.valueOf(value));
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return "";
            }
            if (object instanceof ProcessInstance) {
                ProcessInstance o = (ProcessInstance) object;
                return o.getId() != null ? o.getId().toString() : null;
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ProcessInstance.class.getName());
            }
        }
    }

}
