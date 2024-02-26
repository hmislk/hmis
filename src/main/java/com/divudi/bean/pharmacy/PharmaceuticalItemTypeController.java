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
import com.divudi.entity.pharmacy.PharmaceuticalItemType;
import com.divudi.facade.PharmaceuticalItemTypeFacade;
import java.io.Serializable;
import java.util.ArrayList;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class PharmaceuticalItemTypeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private PharmaceuticalItemTypeFacade ejbFacade;
    List<PharmaceuticalItemType> selectedItems;
    private PharmaceuticalItemType current;
    private List<PharmaceuticalItemType> items = null;
    String selectText = "";

    public void prepareAdd() {
        current = new PharmaceuticalItemType();
    }

    public void setSelectedItems(List<PharmaceuticalItemType> selectedItems) {
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

    public PharmaceuticalItemTypeFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(PharmaceuticalItemTypeFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PharmaceuticalItemTypeController() {
    }

    public PharmaceuticalItemType getCurrent() {
        if (current == null) {
            current = new PharmaceuticalItemType();
        }
        return current;
    }

    public void setCurrent(PharmaceuticalItemType current) {
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

    private PharmaceuticalItemTypeFacade getFacade() {
        return ejbFacade;
    }

    public List<PharmaceuticalItemType> getItems() {
        if (items == null) {
            String j;
            j = "select t "
                    + " from PharmaceuticalItemType t "
                    + " where t.retired=false "
                    + " order by t.name";
            items = getFacade().findByJpql(j);
        }
        return items;
    }

    List<PharmaceuticalItemType> pharmaceuticalItemTypeList = null;

    public List<PharmaceuticalItemType> completeCategory(String qry) {

        Map m = new HashMap();
        m.put("n", "%" + qry + "%");
        if (qry != null) {
            pharmaceuticalItemTypeList = getFacade().findByJpql("select c from PharmaceuticalItemType c where "
                    + " c.retired=false and ((c.name) like :n) order by c.name", m, 20);
            //////// // System.out.println("a size is " + a.size());
        }
        if (pharmaceuticalItemTypeList == null) {
            pharmaceuticalItemTypeList = new ArrayList<>();
        }
        return pharmaceuticalItemTypeList;
    }

    /**
     *
     */
    @FacesConverter(forClass = PharmaceuticalItemType.class)
    public static class PharmaceuticalItemTypeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PharmaceuticalItemTypeController controller = (PharmaceuticalItemTypeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "pharmaceuticalItemTypeController");
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
            if (object instanceof PharmaceuticalItemType) {
                PharmaceuticalItemType o = (PharmaceuticalItemType) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PharmaceuticalItemTypeController.class.getName());
            }
        }
    }

    
}
