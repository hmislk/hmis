package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.core.entity.inward.SurgeryType;
import com.divudi.core.facade.SurgeryTypeFacade;
import java.io.Serializable;
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

/**
 *
 * @author Dhanesh
 */
@Named
@SessionScoped
public class SurgeryTypeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private SurgeryTypeFacade ejbFacade;
    List<SurgeryType> selectedItems;
    private SurgeryType current;
    private List<SurgeryType> items = null;
    String selectText = "";

    public List<SurgeryType> getSelectedItems() {

        String jpql
                = "SELECT c FROM SurgeryType c "
                + "WHERE c.retired = false "
                + "AND UPPER(c.name) LIKE :q "
                + "ORDER BY c.name";

        Map<String, Object> params = new HashMap<>();
        params.put("q", "%" + getSelectText().toUpperCase() + "%");

        selectedItems = getFacade().findByJpql(jpql, params);
        return selectedItems;
    }

    public void prepareAdd() {
        current = new SurgeryType();
    }

    public void setSelectedItems(List<SurgeryType> selectedItems) {
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
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public SurgeryTypeFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(SurgeryTypeFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public SurgeryTypeController() {
    }

    public SurgeryType getCurrent() {
        if (current == null) {
            current = new SurgeryType();
        }
        return current;
    }

    public void setCurrent(SurgeryType current) {
        this.current = current;
    }

    public void delete() {

        if (current == null) {
            JsfUtil.addErrorMessage("Nothing to delete");
            return;
        }

        if (current.getId() == null) {
            JsfUtil.addErrorMessage("Cannot delete an unsaved Surgery Type");
            current = null;
            return;
        }

        current.setRetired(true);
        current.setRetiredAt(new Date());
        current.setRetirer(getSessionController().getLoggedUser());
        getFacade().edit(current);
        JsfUtil.addSuccessMessage("Deleted Successfully");

        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private SurgeryTypeFacade getFacade() {
        return ejbFacade;
    }

    public List<SurgeryType> getItems() {
        if (items == null) {
            String sql = "SELECT i FROM SurgeryType i where i.retired=false  order by i.name";
            items = getEjbFacade().findByJpql(sql);
        }
        return items;
    }

    public List<SurgeryType> completeSurgeryType(String qry) {
        String sql;
        sql = "select c from SurgeryType c where c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name";
        return getFacade().findByJpql(sql);
    }

    /**
     *
     */
    @FacesConverter(forClass = SurgeryType.class)
    public static class SurgeryTypeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SurgeryTypeController controller = (SurgeryTypeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "surgeryTypeController");
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
            if (object instanceof SurgeryType) {
                SurgeryType o = (SurgeryType) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SurgeryTypeController.class.getName());
            }
        }
    }
}
