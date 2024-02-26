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
import com.divudi.entity.pharmacy.VirtualProductIngredient;
import com.divudi.facade.VirtualProductIngredientFacade;
import java.io.Serializable;
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
public class VtmInVmpController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private VirtualProductIngredientFacade ejbFacade;
    List<VirtualProductIngredient> selectedItems;
    private VirtualProductIngredient current;
    private List<VirtualProductIngredient> items = null;
    String selectText = "";

    public void prepareAdd() {
        current = new VirtualProductIngredient();
    }

    public void setSelectedItems(List<VirtualProductIngredient> selectedItems) {
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
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void save(VirtualProductIngredient v) {
        if (v.getId() != null) {
            getFacade().edit(v);
        } else {
            getFacade().create(v);
        }
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public VirtualProductIngredientFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(VirtualProductIngredientFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public VtmInVmpController() {
    }

    public VirtualProductIngredient getCurrent() {
        return current;
    }

    public void setCurrent(VirtualProductIngredient current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private VirtualProductIngredientFacade getFacade() {
        return ejbFacade;
    }

    public List<VirtualProductIngredient> getItems() {
        if (items == null) {
            String j;
            j = "select v "
                    + " from VtmsVmps v "
                    + " where v.retired=false "
                    + " order by v.name";
            items = getFacade().findByJpql(j);
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = VirtualProductIngredient.class)
    public static class VtmInVmpControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            VtmInVmpController controller = (VtmInVmpController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "vtmInVmpController");
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
            if (object instanceof VirtualProductIngredient) {
                VirtualProductIngredient o = (VirtualProductIngredient) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + VtmInVmpController.class.getName());
            }
        }
    }
}
