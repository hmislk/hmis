/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.inward.RoomCategory;
import com.divudi.facade.RoomCategoryFacade;
import java.io.Serializable;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class RoomCategoryController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private RoomCategoryFacade ejbFacade;
    List<RoomCategory> selectedItems;
    private RoomCategory current;
    private List<RoomCategory> items = null;
    String selectText = "";

    public List<RoomCategory> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from RoomCategory c where c.retired=false  and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new RoomCategory();
    }

    public void setSelectedItems(List<RoomCategory> selectedItems) {
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

    public RoomCategoryFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(RoomCategoryFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public RoomCategoryController() {
    }

    public RoomCategory getCurrent() {
        if (current == null) {
            current = new RoomCategory();
        }
        return current;
    }

    public void setCurrent(RoomCategory current) {
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
        getCurrent();
    }

    private RoomCategoryFacade getFacade() {
        return ejbFacade;
    }

    public List<RoomCategory> getItems() {
        if (items == null) {
            String sql = "SELECT i FROM RoomCategory i where i.retired=false  order by i.name";
            items = getEjbFacade().findByJpql(sql);
        }
        return items;
    }

    public List<RoomCategory> completeRoomCategory(String qry) {
        String sql;
        sql = "select c from RoomCategory c where c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name";
        return getFacade().findByJpql(sql);
    }

    /**
     *
     */

    @FacesConverter(forClass = RoomCategory.class)
    public static class RoomCategoryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            RoomCategoryController controller = (RoomCategoryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "roomCategoryController");
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
            if (object instanceof RoomCategory) {
                RoomCategory o = (RoomCategory) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + RoomCategoryController.class.getName());
            }
        }
    }
}
