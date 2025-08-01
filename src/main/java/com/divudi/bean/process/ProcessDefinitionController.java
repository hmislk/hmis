package com.divudi.bean.process;

import com.divudi.core.entity.process.ProcessDefinition;
import com.divudi.core.facade.ProcessDefinitionFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr
 * M H B Ariyaratne
 *
 */
@Named
@SessionScoped
public class ProcessDefinitionController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ProcessDefinitionFacade processDefinitionFacade;

    private int index = 0;

    // Current ProcessDefinition being created or edited
    private ProcessDefinition current;

    // List of all ProcessDefinitions
    private List<ProcessDefinition> items = null;

    // Editable status flag
    private boolean editable;

    /**
     * Default constructor.
     */
    public ProcessDefinitionController() {
        // Initialization logic if needed
    }

    // Getter and Setter for 'current'
    public ProcessDefinition getCurrent() {
        return current;
    }

    public void setCurrent(ProcessDefinition current) {
        this.current = current;
    }

    // Getter for 'items'
    public List<ProcessDefinition> getItems() {
        if (items == null) {
            fillAllProcesses();
        }
        return items;
    }

    // Getter and Setter for 'editable'
    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * Navigates to the manage Process Definitions view.
     *
     * @return navigation outcome string
     */
    public String navigateToManageProcessDefinitions() {
        fillAllProcesses();
        return "/process/admin/process_definitions?faces-redirect=true";
    }

    /**
     * Navigates to the manage Process Definitions view.
     *
     * @return navigation outcome string
     */
    @Deprecated // Use Data Administration
    public String navigateToManageProcessIndex() {
        fillAllProcesses();
        return "/process/admin/index?faces-redirect=true";
    }

    /**
     * Retrieves all active ProcessDefinitions.
     */
    public void fillAllProcesses() {
        String jpql = "SELECT p FROM ProcessDefinition p WHERE p.retired = :ret ORDER BY p.name";
        HashMap<String, Object> params = new HashMap<>();
        params.put("ret", false);
        items = processDefinitionFacade.findByJpql(jpql, params, TemporalType.TIME);
    }

    /**
     * Initializes the creation of a new ProcessDefinition.
     *
     * @return navigation outcome string
     */
    public String addNewProcess() {
        current = new ProcessDefinition();
        editable = true;
        return null; // Stay on the same page
    }

    /**
     * Initializes the editing of an existing ProcessDefinition.
     *
     * @param processDefinition the ProcessDefinition to edit
     * @return navigation outcome string
     */
    public String editExistingProcess(ProcessDefinition processDefinition) {
        if (processDefinition != null) {
            this.current = processDefinition;
            this.editable = true;
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "No Process Selected", "Please select a process to edit."));
        }
        return null; // Stay on the same page
    }

    /**
     * Saves or updates the current ProcessDefinition.
     *
     * @return navigation outcome string
     */
    public String saveOrUpdateProcess() {
        try {
            if (current.getId() == null) {
                processDefinitionFacade.create(current);
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Process Created", "Process has been successfully created."));
            } else {
                processDefinitionFacade.edit(current);
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Process Updated", "Process has been successfully updated."));
            }
            items = null; // Invalidate list to trigger re-query
            editable = false;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "An error occurred while saving the process."));
        }
        return null; // Stay on the same page
    }



    /**
     * Deletes the specified ProcessDefinition by flagging it as retired.
     *
     * @param processDefinition the ProcessDefinition to delete
     * @return navigation outcome string
     */
    public String deleteProcess(ProcessDefinition processDefinition) {
        if (processDefinition != null) {
            try {
                processDefinition.setRetired(true);
                processDefinitionFacade.edit(processDefinition);
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Process Retired", "Process has been successfully retired."));
                items = null; // Invalidate list to trigger re-query
            } catch (Exception e) {
                FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "An error occurred while retiring the process."));
            }
        } else {
            FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, "No Process Selected", "Please select a process to retire."));
        }
        return null; // Stay on the same page
    }

    /**
     * Resets the controller's state.
     */
    public void reset() {
        current = null;
        items = null;
        editable = false;
        FacesContext.getCurrentInstance().addMessage(null,
            new FacesMessage(FacesMessage.SEVERITY_INFO, "Reset", "Process form has been reset."));
    }

    /**
     * Getter for ProcessDefinitionFacade.
     *
     * @return the ProcessDefinitionFacade
     */
    public ProcessDefinitionFacade getProcessDefinitionFacade() {
        return processDefinitionFacade;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    /**
     * Converter class for ProcessDefinition entities.
     */
    @FacesConverter(forClass = ProcessDefinition.class)
    public static class ProcessDefinitionControllerConverter implements Converter {

        /**
         * Converts a String value to a ProcessDefinition object.
         *
         * @param facesContext the FacesContext
         * @param component the UIComponent
         * @param value the String value to convert
         * @return the corresponding ProcessDefinition object
         */
        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            ProcessDefinitionController controller = (ProcessDefinitionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "processDefinitionController");
            return controller.getProcessDefinitionFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key = null;
            try {
                key = Long.valueOf(value);
            } catch (NumberFormatException e) {
                // Handle exception as needed
            }
            return key;
        }

        String getStringKey(java.lang.Long value) {
            return value != null ? value.toString() : "";
        }

        /**
         * Converts a ProcessDefinition object to its String representation.
         *
         * @param facesContext the FacesContext
         * @param component the UIComponent
         * @param object the ProcessDefinition object to convert
         * @return the String representation of the ProcessDefinition
         */
        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return "";
            }
            if (object instanceof ProcessDefinition) {
                ProcessDefinition o = (ProcessDefinition) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ProcessDefinition.class.getName());
            }
        }
    }

}
