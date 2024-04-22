/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;
import com.divudi.entity.Bill;
import com.divudi.entity.ServiceSession;
import com.divudi.facade.ServiceSessionFacade;
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
import com.divudi.bean.common.util.JsfUtil;
/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class ServiceSessionController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ServiceSessionFacade ejbFacade;
    List<ServiceSession> selectedItems;
    private ServiceSession current;
    private List<ServiceSession> items = null;
    String selectText = "";

    public List<ServiceSession> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from ServiceSession c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public List<ServiceSession> completeServiceSession(String qry) {
        List<ServiceSession> a = null;
        if (qry != null) {
            a = getFacade().findByJpql("select c from ServiceSession c where c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        }
        if (a == null) {
            a = new ArrayList<ServiceSession>();
        }
        return a;
    }

    public List<ServiceSession> completeSession(String query) {
        List<ServiceSession> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<ServiceSession>();
        } else {

            sql = "select p from ServiceSession p where p.retired=false and (((p.staff.person.name) like '%" + query.toUpperCase() + "%') or ((p.name) like '%" + query.toUpperCase() + "%') or ((p.staff.speciality.name) like '%" + query.toUpperCase() + "%') ) order by p.name";
            //////// // System.out.println(sql);
            suggestions = getFacade().findByJpql(sql);
        }
        return suggestions;
    }

    public void prepServiceSessiondd() {
        current = new ServiceSession();
    }

    public void setSelectedItems(List<ServiceSession> selectedItems) {
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

    public ServiceSessionFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ServiceSessionFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ServiceSessionController() {
    }

    public ServiceSession getCurrent() {
        return current;
    }

    public void setCurrent(ServiceSession current) {
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

    private ServiceSessionFacade getFacade() {
        return ejbFacade;
    }

    public List<ServiceSession> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    /**
     *
     */
    @FacesConverter("sscon")
    public static class ServiceSessionControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ServiceSessionController controller = (ServiceSessionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "serviceSessionController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            if (value == null) {
                return null;
            }
            java.lang.Long key;
            try {
                key = Long.valueOf(value);
            } catch (Exception e) {
                key = 0l;
            }
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
            if (object instanceof ServiceSession) {
                ServiceSession o = (ServiceSession) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ServiceSessionController.class.getName());
            }
        }
    }
    
    
    @FacesConverter(forClass = ServiceSession.class)
    public static class ServiceSessionConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ServiceSessionController controller = (ServiceSessionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "serviceSessionController");
            ServiceSession found = controller.getFacade().find(getKey(value));
            return found;
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
            if (object instanceof ServiceSession) {
                ServiceSession o = (ServiceSession) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ServiceSession.class.getName());
            }
        }
    }

    
}
