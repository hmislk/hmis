/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.hr;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.hr.PaysheetComponentType;
import com.divudi.entity.hr.PaysheetComponent;
import com.divudi.facade.PaysheetComponentFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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
public class PaysheetComponentSystemController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private PaysheetComponentFacade ejbFacade;
    List<PaysheetComponent> selectedItems;
    private PaysheetComponent current;
    private List<PaysheetComponent> items = null;
    String selectText = "";
   
    public List<PaysheetComponent> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from PaysheetComponent c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public List<PaysheetComponent> completePaysheetComponent(String qry) {
        List<PaysheetComponent> a = null;
        if (qry != null) {
            a = getFacade().findByJpql("select c from PaysheetComponent c where c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        }
        if (a == null) {
            a = new ArrayList<PaysheetComponent>();
        }
        return a;
    }

    public void prepareAdd() {
        current = new PaysheetComponent();
    }

    public void setSelectedItems(List<PaysheetComponent> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    private boolean checkComponent() {
        String sql = "";
        sql = "Select s From PaysheetComponent s "
                + " where s.retired=false "
                + " and s.componentType in :ct ";
        HashMap hm = new HashMap();

        hm.put("ct", PaysheetComponentType.addition.getSystemDefinedComponents());

        PaysheetComponent tmp = getEjbFacade().findFirstByJpql(sql, hm);

        if (tmp != null) {
            return true;
        } else {
            return false;
        }

    }

    public void saveSelected() {

        if (getCurrent().getComponentType() == null) {
            JsfUtil.addErrorMessage("Pls Select Compnent Type");
            return;
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        }else{
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Save New Successfull");
        }
//        else {
//            if (checkComponent()) {
//                JsfUtil.addErrorMessage("This Component Type Already Exist");
//                return;
//            }
//
//            current.setCreatedAt(new Date());
//            current.setCreater(getSessionController().getLoggedUser());
//            getFacade().create(current);
//            JsfUtil.addSuccessMessage("Saved Successfully");
//        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public PaysheetComponentFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(PaysheetComponentFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PaysheetComponentSystemController() {
    }

    public PaysheetComponent getCurrent() {
        if (current == null) {
            current = new PaysheetComponent();
        }
        return current;
    }

    public void setCurrent(PaysheetComponent current) {
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

    private PaysheetComponentFacade getFacade() {
        return ejbFacade;
    }

    public List<PaysheetComponent> getItems() {
        String sql = "Select s from PaysheetComponent s"
                + " where s.retired=false "
                + " and s.componentType in :tp ";
        HashMap hm = new HashMap();
        hm.put("tp", PaysheetComponentType.addition.getSystemDefinedComponents());
        items = ejbFacade.findByJpql(sql, hm);
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = PaysheetComponent.class)
    public static class PaysheetComponentSystemConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PaysheetComponentSystemController controller = (PaysheetComponentSystemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "paysheetComponentSystemController");
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
            if (object instanceof PaysheetComponent) {
                PaysheetComponent o = (PaysheetComponent) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PaysheetComponentSystemController.class.getName());
            }
        }
    }

  
}
