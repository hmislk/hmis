package com.divudi.bean.process;

import com.divudi.core.entity.web.DesignComponentAssignment;
import com.divudi.core.facade.web.DesignComponentAssignmentFacade;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.persistence.TemporalType;

@Named
@SessionScoped
public class DesignComponentAssignmentController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private DesignComponentAssignmentFacade designComponentAssignmentFacade;

    private DesignComponentAssignment current;
    private List<DesignComponentAssignment> items;

    public DesignComponentAssignmentController() {
    }

    public DesignComponentAssignment getCurrent() {
        return current;
    }

    public void setCurrent(DesignComponentAssignment current) {
        this.current = current;
    }

    public List<DesignComponentAssignment> getItems() {
        if (items == null) {
            items = designComponentAssignmentFacade.findAll();
        }
        return items;
    }

    public String prepareCreate() {
        current = new DesignComponentAssignment();
        return "/forms/design_component_assignment_create?faces-redirect=true";
    }

    public String navigateToList() {
        return "/forms/design_component_assignment_list?faces-redirect=true";
    }

    public String prepareEdit(DesignComponentAssignment assignment) {
        current = assignment;
        return "/forms/design_component_assignment_edit?faces-redirect=true";
    }

    public String saveOrUpdate() {
        try {
            if (current.getDesignComponent() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "A Design Component must be selected."));
                return null;
            }

            if (current.getProcessDefinition() == null
                    && current.getProcessStepDefinition() == null
                    && current.getProcessStepActionDefinition() == null) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Validation Error", "At least one entity (Process Definition, Process Step Definition, or Process Step Action Definition) must be assigned."));
                return null;
            }

            if (current.getId() == null) {
                designComponentAssignmentFacade.create(current);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Created", "Assignment successfully created."));
            } else {
                designComponentAssignmentFacade.edit(current);
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Updated", "Assignment successfully updated."));
            }
            items = null; // Refresh list
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "An error occurred while saving the assignment."));
        }
        return null; // Stay on the same page
    }

    public String delete(DesignComponentAssignment assignment) {
        try {
            designComponentAssignmentFacade.remove(assignment);
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO, "Deleted", "Assignment successfully deleted."));
            items = null; // Refresh list
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", "An error occurred while deleting the assignment."));
        }
        return null; // Stay on the same page
    }

    public DesignComponentAssignment find(Long id) {
        return designComponentAssignmentFacade.find(id);
    }

    public List<DesignComponentAssignment> fetchAssignments(Date fromDate, Date toDate, String assignedEntityType) {
        String jpql = "SELECT dca FROM DesignComponentAssignment dca WHERE dca.retired = :ret";
        Map<String, Object> params = new HashMap<>();
        params.put("ret", false);

        if (fromDate != null) {
            jpql += " AND dca.createdAt >= :fromDate";
            params.put("fromDate", fromDate);
        }
        if (toDate != null) {
            jpql += " AND dca.createdAt <= :toDate";
            params.put("toDate", toDate);
        }
        if (assignedEntityType != null && !assignedEntityType.isEmpty()) {
            jpql += " AND dca.assignedEntityType = :assignedEntityType";
            params.put("assignedEntityType", assignedEntityType);
        }

        jpql += " ORDER BY dca.createdAt DESC";

        return designComponentAssignmentFacade.findByJpql(jpql, params, TemporalType.TIMESTAMP);
    }

    @FacesConverter(forClass = DesignComponentAssignment.class)
    public static class DesignComponentAssignmentConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            DesignComponentAssignmentController controller = (DesignComponentAssignmentController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "designComponentAssignmentController");
            return controller.designComponentAssignmentFacade.find(getKey(value));
        }

        Long getKey(String value) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        String getStringKey(Long value) {
            return value != null ? value.toString() : "";
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return "";
            }
            if (object instanceof DesignComponentAssignment) {
                DesignComponentAssignment o = (DesignComponentAssignment) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DesignComponentAssignment.class.getName());
            }
        }
    }
}
