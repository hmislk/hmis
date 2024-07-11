/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.BillFee;
import com.divudi.facade.BillFeeFacade;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class BillFeeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private BillFeeFacade ejbFacade;
    List<BillFee> selectedItems;
    private BillFee current;
    private List<BillFee> items = null;
    String selectText = "";

    public List<BillFee> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from BillFee c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new BillFee();
    }

    public void setSelectedItems(List<BillFee> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void edit(BillFee billFee) {
        getFacade().edit(billFee);
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

    public BillFeeFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(BillFeeFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public BillFeeController() {
    }

    public BillFee getCurrent() {
        return current;
    }

    public void setCurrent(BillFee current) {
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

    private BillFeeFacade getFacade() {
        return ejbFacade;
    }

    public List<BillFee> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    @FacesConverter(value = "billFeeConverter")
    public static class BillFeeConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.trim().isEmpty() || "null".equals(value.trim())) {
                return null;
            }

            if (value.startsWith("TEMP_")) {
                // Handle objects without ID
                Map<String, BillFee> billFeeMap = (Map<String, BillFee>) facesContext.getExternalContext().getSessionMap().get("billFeeMap");
                return billFeeMap.get(value);
            } else {
                // Handle objects with ID
                BillFeeController controller = (BillFeeController) facesContext.getApplication().getELResolver().
                        getValue(facesContext.getELContext(), null, "billFeeController");

                Long key = getKey(value);
                if (key == null) {
                    // key is null, return null to avoid calling find with a null PK
                    return null;
                }
                return controller.getEjbFacade().find(key);
            }
        }

        Long getKey(String value) {
            try {
                return Long.valueOf(value.trim());
            } catch (NumberFormatException e) {
                // Log or handle the error as appropriate
                return null; // Return null if the input is not a valid long
            }
        }

        String getStringKey(Long value) {
            if (value == null) {
                return null; // Handle null values gracefully
            }
            return value.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof BillFee) {
                BillFee billFee = (BillFee) object;
                if (billFee.getId() != null) {
                    return getStringKey(billFee.getId());
                } else {
                    // Use a temporary key for objects without ID
                    String key = "TEMP_" + billFee.hashCode();
                    Map<String, BillFee> billFeeMap = (Map<String, BillFee>) facesContext.getExternalContext().getSessionMap().computeIfAbsent("billFeeMap", k -> new HashMap<>());
                    billFeeMap.put(key, billFee);
                    return key;
                }
            } else {
                throw new IllegalArgumentException("Object is not of type BillFee: " + object.getClass().getName());
            }
        }
    }

    /**
     *
     */
    @FacesConverter(forClass = BillFee.class)
    public static class BillFeeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            BillFeeController controller = (BillFeeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "billFeeController");
            return controller.getEjbFacade().find(getKey(value));
        }

        Long getKey(String value) {
            try {
                return Long.valueOf(value.trim());
            } catch (NumberFormatException e) {
                // Log or handle the error as appropriate
                return null; // Return null if the input is not a valid long
            }
        }

        String getStringKey(Long value) {
            if (value == null) {
                return null; // Handle null values gracefully
            }
            return value.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof BillFee) {
                BillFee o = (BillFee) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + BillFee.class.getName());
            }
        }
    }
}
