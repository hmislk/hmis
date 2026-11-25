package com.divudi.bean.opd;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.WebUserController;
import com.divudi.core.entity.PatientComplaintCategory;
import com.divudi.core.facade.PatientComplaintCategoryFacade;
import com.divudi.core.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Admin page for the patient complaint category list (#25886). Needs the Admin privilege.
 */
@Named
@SessionScoped
public class PatientComplaintCategoryController implements Serializable {

    private static final long serialVersionUID = 1L;

    @EJB
    private PatientComplaintCategoryFacade facade;
    @Inject
    private SessionController sessionController;
    @Inject
    private WebUserController webUserController;

    private PatientComplaintCategory current;
    private List<PatientComplaintCategory> items;

    public boolean isCanManage() {
        return webUserController.hasPrivilege("Admin");
    }

    private boolean deny() {
        if (isCanManage()) {
            return false;
        }
        JsfUtil.addErrorMessage("You do not have the privilege to manage complaint categories.");
        return true;
    }

    public String navigateToCategories() {
        if (deny()) {
            return null;
        }
        current = new PatientComplaintCategory();
        items = null;
        return "/opd/complaints/complaint_categories?faces-redirect=true";
    }

    public void prepareAdd() {
        current = new PatientComplaintCategory();
    }

    public void save() {
        if (deny()) {
            return;
        }
        String name = current == null || current.getName() == null ? null : current.getName().trim();
        if (name == null || name.isEmpty()) {
            JsfUtil.addErrorMessage("Enter the category name.");
            return;
        }
        current.setName(name);
        if (current.getId() != null) {
            facade.edit(current);
            JsfUtil.addSuccessMessage("Updated.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(sessionController.getLoggedUser());
            facade.create(current);
            JsfUtil.addSuccessMessage("Saved.");
        }
        items = null;
        current = new PatientComplaintCategory();
    }

    public void retire() {
        if (deny()) {
            return;
        }
        if (current == null || current.getId() == null) {
            JsfUtil.addErrorMessage("Select a category first.");
            return;
        }
        current.setRetired(true);
        current.setRetiredAt(new Date());
        current.setRetirer(sessionController.getLoggedUser());
        facade.edit(current);
        JsfUtil.addSuccessMessage("Category removed. Existing complaints keep it.");
        items = null;
        current = new PatientComplaintCategory();
    }

    public PatientComplaintCategory getCurrent() {
        if (current == null) {
            current = new PatientComplaintCategory();
        }
        return current;
    }

    public void setCurrent(PatientComplaintCategory current) {
        this.current = current;
    }

    public List<PatientComplaintCategory> getItems() {
        if (items == null) {
            items = facade.findByJpql("select c from PatientComplaintCategory c where c.retired=false order by c.name");
            if (items == null) {
                items = new ArrayList<>();
            }
        }
        return items;
    }

    public PatientComplaintCategoryFacade getFacade() {
        return facade;
    }

    @FacesConverter(forClass = PatientComplaintCategory.class)
    public static class PatientComplaintCategoryConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            PatientComplaintCategoryController controller = (PatientComplaintCategoryController) facesContext
                    .getApplication().getELResolver()
                    .getValue(facesContext.getELContext(), null, "patientComplaintCategoryController");
            return controller.getFacade().find(Long.valueOf(value));
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof PatientComplaintCategory) {
                Long id = ((PatientComplaintCategory) object).getId();
                return id == null ? null : String.valueOf(id);
            }
            throw new IllegalArgumentException("object " + object + " is of type "
                    + object.getClass().getName() + "; expected type: " + PatientComplaintCategory.class.getName());
        }
    }
}
