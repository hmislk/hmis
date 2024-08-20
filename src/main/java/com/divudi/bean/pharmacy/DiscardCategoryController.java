/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.pharmacy.DiscardCategory;
import com.divudi.facade.DiscardCategoryFacade;
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
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class DiscardCategoryController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private DiscardCategoryFacade ejbFacade;
    List<DiscardCategory> selectedItems;
    private DiscardCategory current;
    private List<DiscardCategory> items = null;
    String selectText = "";

    public void prepareAdd() {
        current = new DiscardCategory();
    }

    public void setSelectedItems(List<DiscardCategory> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        fillDiscardCategories();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public DiscardCategoryFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(DiscardCategoryFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public DiscardCategoryController() {
    }

    public DiscardCategory getCurrent() {
        if (current == null) {
            current = new DiscardCategory();
        }
        return current;
    }

    public void setCurrent(DiscardCategory current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        fillDiscardCategories();
        getCurrent();
    }

    private DiscardCategoryFacade getFacade() {
        return ejbFacade;
    }

    public void fillDiscardCategories() {
        String j;
        j = "select c "
                + " from DiscardCategory c "
                + " where c.retired=false "
                + " order by c.name";
        items = getFacade().findByJpql(j);
        if (items!=null && !items.isEmpty()){
            current = items.get(0);
        }
        if (items == null){
            items = new ArrayList<>();
        }
    }

    public List<DiscardCategory> getItems() {
        if(items == null){
            items = new ArrayList<>();
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = DiscardCategory.class)
    public static class DiscardCategoryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DiscardCategoryController controller = (DiscardCategoryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "discardCategoryController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            key = Long.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Long value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof DiscardCategory) {
                DiscardCategory o = (DiscardCategory) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DiscardCategoryController.class.getName());
            }
        }
    }
}
