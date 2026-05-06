package com.divudi.bean.process;

import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.process.ProcessDefinition;
import com.divudi.core.entity.process.ProcessStepDefinition;
import com.divudi.core.facade.ProcessStepDefinitionFacade;
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
import javax.inject.Inject;
import javax.persistence.TemporalType;

/**
 * Controller for managing Process Step Definitions. Handles creation, editing,
 * deletion, and listing of ProcessStepDefinition entities.
 *
 * Dr M H B Ariyaratne
 */
@Named
@SessionScoped
public class ProcessStepDefinitionController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private ProcessStepDefinitionFacade processStepDefinitionFacade;

    @Inject
    ProcessDefinitionController processDefinitionController;

    // Current ProcessStepDefinition being created or edited
    private ProcessStepDefinition current;

    // List of all ProcessStepDefinitions
    private List<ProcessStepDefinition> items = null;

    // Editable status flag
    private boolean editable;

    /**
     * Default constructor.
     */
    public ProcessStepDefinitionController() {
        // Initialization logic if needed
    }

    // Getter and Setter for 'current'
    public ProcessStepDefinition getCurrent() {
        System.out.println("getCurrent");
        return current;
    }

    public void setCurrent(ProcessStepDefinition current) {
        System.out.println("setCurrent");
        this.current = current;
    }

    // Getter for 'items'
    public List<ProcessStepDefinition> getItems() {
        if (items == null) {
            fillAllProcessStepDefinitions();
        }
        return items;
    }

    public List<ProcessStepDefinition> getItemsOfSelectedProcessDefinition(ProcessDefinition pd) {
        String jpql = "SELECT p FROM ProcessStepDefinition p "
                + " WHERE p.retired = :ret "
                + " and p.processDefinition=:pd "
                + " ORDER BY p.sequenceOrder";
        HashMap<String, Object> params = new HashMap<>();
        params.put("ret", false);
        params.put("pd", pd);
        current = null;
        return getProcessStepDefinitionFacade().findByJpql(jpql, params);
    }

    // Getter and Setter for 'editable'
    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * Navigates to the manage Process Step Definitions view.
     *
     * @return navigation outcome string
     */
    public String navigateToManageProcessStepDefinitions() {
        fillAllProcessStepDefinitions();
        return "/process/admin/process_step_definitions?faces-redirect=true";
    }

    /**
     * Retrieves all active ProcessStepDefinitions.
     */
    public void fillAllProcessStepDefinitions() {
        String jpql = "SELECT p FROM ProcessStepDefinition p WHERE p.retired = :ret ORDER BY p.sequenceOrder";
        HashMap<String, Object> params = new HashMap<>();
        params.put("ret", false);
        items = processStepDefinitionFacade.findByJpql(jpql, params, TemporalType.TIME);
    }

    /**
     * Initializes the creation of a new ProcessStepDefinition.
     *
     * @return navigation outcome string
     */
    public String addNewProcessStepDefinition() {
        System.out.println("addNewProcessStepDefinition");
        current = new ProcessStepDefinition();
        current.setVersion("1.0");
        current.setSequenceOrder((double)(getItems().size()+1));
        System.out.println("current = " + current);
        current.setProcessDefinition(processDefinitionController.getCurrent());
        editable = true;
        return null; // Stay on the same page
    }

    /**
     * Initializes the editing of an existing ProcessStepDefinition.
     *
     * @param processStepDefinition the ProcessStepDefinition to edit
     * @return navigation outcome string
     */
    public String editExistingProcessStepDefinition() {
        if (current == null) {
            JsfUtil.addErrorMessage("Select one to Edit");
            return null;
        }
        editable = true;
        return null; // Stay on the same page
    }

    /**
     * Saves or updates the current ProcessStepDefinition.
     *
     * @return navigation outcome string
     */
    public String saveOrUpdateProcessStepDefinition() {
        if(current==null){
            JsfUtil.addErrorMessage("Nothing to save");
            return null;
        }
        if(current.getVersion()==null){
            current.setVersion("1.0");
        }
        if(current.getSequenceOrder()==null){
            Double sn = (double) getItems().size() + 1;
            current.setSequenceOrder(sn);
        }
        try {
            if (current.getId() == null) {
                processStepDefinitionFacade.create(current);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Step Created", "Process step has been successfully created."));
            } else {
                processStepDefinitionFacade.edit(current);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Step Updated", "Process step has been successfully updated."));
            }
            items = null; // Invalidate list to trigger re-query
            editable = false;
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "An error occurred while saving the process step."));
        }
        return null; // Stay on the same page
    }

    /**
     * Deletes the specified ProcessStepDefinition by flagging it as retired.
     * @return navigation outcome string
     */
    public String deleteProcessStepDefinition() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to Delete");
            return null;
        }
        current.setRetired(true);
        processStepDefinitionFacade.edit(current);
        JsfUtil.addSuccessMessage("Deleted");
        items = null;
        return null;
    }

    /**
     * Resets the controller's state.
     */
    public void reset() {
        current = null;
        items = null;
        editable = false;
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_INFO, "Reset", "Process step form has been reset."));
    }

    /**
     * Getter for ProcessStepDefinitionFacade.
     *
     * @return the ProcessStepDefinitionFacade
     */
    public ProcessStepDefinitionFacade getProcessStepDefinitionFacade() {
        return processStepDefinitionFacade;
    }

    /**
     * Converter class for ProcessStepDefinition entities.
     */
    @FacesConverter(forClass = ProcessStepDefinition.class)
    public static class ProcessStepDefinitionControllerConverter implements Converter {

        /**
         * Converts a String value to a ProcessStepDefinition object.
         *
         * @param facesContext the FacesContext
         * @param component the UIComponent
         * @param value the String value to convert
         * @return the corresponding ProcessStepDefinition object
         */
        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            ProcessStepDefinitionController controller = (ProcessStepDefinitionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "processStepDefinitionController");
            return controller.getProcessStepDefinitionFacade().find(getKey(value));
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
         * Converts a ProcessStepDefinition object to its String representation.
         *
         * @param facesContext the FacesContext
         * @param component the UIComponent
         * @param object the ProcessStepDefinition object to convert
         * @return the String representation of the ProcessStepDefinition
         */
        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return "";
            }
            if (object instanceof ProcessStepDefinition) {
                ProcessStepDefinition o = (ProcessStepDefinition) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ProcessStepDefinition.class.getName());
            }
        }
    }

}
