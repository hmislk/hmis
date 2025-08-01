package com.divudi.bean.process;

import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.process.ProcessDefinition;
import com.divudi.core.entity.process.ProcessStepActionDefinition;
import com.divudi.core.entity.process.ProcessStepDefinition;
import com.divudi.core.facade.ProcessStepActionDefinitionFacade;
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
 * Controller for managing Process Step Action Definitions. Handles creation,
 * editing, deletion, and listing of ProcessStepActionDefinition entities.
 *
 * Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class ProcessStepActionDefinitionController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ProcessStepActionDefinitionFacade processStepActionDefinitionFacade;

    // Current ProcessStepActionDefinition being created or edited
    private ProcessStepActionDefinition current;

    // List of all ProcessStepActionDefinitions
    private List<ProcessStepActionDefinition> items = null;

    private ProcessDefinition processDefinition;
    private ProcessStepDefinition processStepDefinition;

    // Editable status flag
    private boolean editable;

    /**
     * Default constructor.
     */
    public ProcessStepActionDefinitionController() {
        // Initialization logic if needed
    }

    // Getter and Setter for 'current'
    public ProcessStepActionDefinition getCurrent() {
        return current;
    }

    public void setCurrent(ProcessStepActionDefinition current) {
        this.current = current;
    }

    // Getter for 'items'
    public List<ProcessStepActionDefinition> getItems() {
        if (items == null) {
            fillAllProcessStepActionDefinitions();
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
     * Navigates to the manage Process Step Action Definitions view.
     *
     * @return navigation outcome string
     */
    public String navigateToManageProcessStepActionDefinitions() {
        fillAllProcessStepActionDefinitions();
        return "/process/admin/process_step_action_definitions?faces-redirect=true";
    }

    public List<ProcessStepActionDefinition> getItemsOfSelectedProcessStepDefinition(ProcessStepDefinition psd) {
        String jpql = "SELECT p FROM ProcessStepActionDefinition p "
                + " WHERE p.retired = :ret "
                + " and p.processStepDefinition=:psd "
                + " ORDER BY p.name";
        HashMap<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("psd", psd);
        current = null;
        return getProcessStepActionDefinitionFacade().findByJpql(jpql, params);
    }

    /**
     * Retrieves all active ProcessStepActionDefinitions.
     */
    public void fillAllProcessStepActionDefinitions() {
        String jpql = "SELECT p FROM ProcessStepActionDefinition p WHERE p.retired = :ret ORDER BY p.name";
        HashMap<String, Object> params = new HashMap<>();
        params.put("ret", false);
        items = processStepActionDefinitionFacade.findByJpql(jpql, params, TemporalType.TIME);
    }

    /**
     * Initializes the creation of a new ProcessStepActionDefinition.
     *
     * @return navigation outcome string
     */
    public String addNewProcessStepActionDefinition() {
        current = new ProcessStepActionDefinition();
        current.setProcessStepDefinition(processStepDefinition);
        current.setVersion("1.0");
        current.setSequenceOrder((double) (getItems().size() + 1));
        editable = true;
        return null; // Stay on the same page
    }

    /**
     * Initializes the editing of an existing ProcessStepActionDefinition.
     *
     * @param processStepActionDefinition the ProcessStepActionDefinition to
     * edit
     * @return navigation outcome string
     */
    public String editExistingProcessStepActionDefinition() {
        if (current == null) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_WARN, "No Action Selected", "Please select an action to edit."));
            return null;
        }
        this.editable = true;
        return null; // Stay on the same page
    }

    /**
     * Saves or updates the current ProcessStepActionDefinition.
     *
     * @return navigation outcome string
     */
    public String saveOrUpdateProcessStepActionDefinition() {
        try {
            if (current.getId() == null) {
                processStepActionDefinitionFacade.create(current);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Action Created", "Process step action has been successfully created."));
            } else {
                processStepActionDefinitionFacade.edit(current);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Action Updated", "Process step action has been successfully updated."));
            }
            items = null; // Invalidate list to trigger re-query
            editable = false;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "An error occurred while saving the process step action."));
        }
        return null; // Stay on the same page
    }

    /**
     * Deletes the specified ProcessStepActionDefinition by flagging it as
     * retired.
     *
     * @param processStepActionDefinition the ProcessStepActionDefinition to
     * delete
     * @return navigation outcome string
     */
    public String deleteProcessStepActionDefinition() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to delete");
            return null;
        }

        try {
            current.setRetired(true);
            processStepActionDefinitionFacade.edit(current);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Action Retired", "Process step action has been successfully retired."));
            items = null; // Invalidate list to trigger re-query
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "An error occurred while retiring the process step action."));
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
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Reset", "Process step action form has been reset."));
    }

    /**
     * Getter for ProcessStepActionDefinitionFacade.
     *
     * @return the ProcessStepActionDefinitionFacade
     */
    public ProcessStepActionDefinitionFacade getProcessStepActionDefinitionFacade() {
        return processStepActionDefinitionFacade;
    }

    public ProcessDefinition getProcessDefinition() {
        return processDefinition;
    }

    public void setProcessDefinition(ProcessDefinition processDefinition) {
        this.processDefinition = processDefinition;
    }

    public ProcessStepDefinition getProcessStepDefinition() {
        return processStepDefinition;
    }

    public void setProcessStepDefinition(ProcessStepDefinition processStepDefinition) {
        this.processStepDefinition = processStepDefinition;
    }

    /**
     * Converter class for ProcessStepActionDefinition entities.
     */
    @FacesConverter(forClass = ProcessStepActionDefinition.class)
    public static class ProcessStepActionDefinitionControllerConverter implements Converter {

        /**
         * Converts a String value to a ProcessStepActionDefinition object.
         *
         * @param facesContext the FacesContext
         * @param component the UIComponent
         * @param value the String value to convert
         * @return the corresponding ProcessStepActionDefinition object
         */
        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            ProcessStepActionDefinitionController controller = (ProcessStepActionDefinitionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "processStepActionDefinitionController");
            return controller.getProcessStepActionDefinitionFacade().find(getKey(value));
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
         * Converts a ProcessStepActionDefinition object to its String
         * representation.
         *
         * @param facesContext the FacesContext
         * @param component the UIComponent
         * @param object the ProcessStepActionDefinition object to convert
         * @return the String representation of the ProcessStepActionDefinition
         */
        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return "";
            }
            if (object instanceof ProcessStepActionDefinition) {
                ProcessStepActionDefinition o = (ProcessStepActionDefinition) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ProcessStepActionDefinition.class.getName());
            }
        }
    }

}
