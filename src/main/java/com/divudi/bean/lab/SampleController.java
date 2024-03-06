/*
 * Open Hospital Management Information System
 * 
 * Dr M H B Ariyaratne 
 * Acting Consultant (Health Informatics) 
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;

import com.divudi.entity.Category;
import com.divudi.entity.lab.Sample;
import com.divudi.facade.SampleFacade;
import com.divudi.bean.common.util.JsfUtil;
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
public class SampleController implements Serializable {

    @Inject
    SessionController sessionController;
    @EJB
    private SampleFacade ejbFacade;
    private Sample current;
    private List<Sample> items = null;

    public List<Sample> fillAllItems() {
        String j = "select s "
                + " from Sample s "
                + " where s.retired=:ret "
                + " order by s.name";
        Map m = new HashMap();
        m.put("ret", false);
        return getFacade().findByJpql(j,m);
    }

    public void prepareAdd() {
        current = new Sample();
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
    
    public Sample findAndCreateSampleByName(String qry) {
        Sample s;
        String jpql;
        jpql = "select s from "
                + " Sample s "
                + " where s.retired=:ret "
                + " and s.name=:name "
                + " order by s.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("name", qry);
        s = getFacade().findFirstByJpql(jpql, m);
        if(s==null){
            s = new Sample();
            s.setName(qry);
            s.setCreatedAt(new Date());
            getFacade().create(s);
        }
        return s;
    }

    private SampleFacade getEjbFacade() {
        return ejbFacade;
    }

    private SessionController getSessionController() {
        return sessionController;
    }

    public SampleController() {
    }

    public Sample getCurrent() {
        if(current == null){
            current = new Sample();
        }
        return current;
    }

    public Sample getAnySample() {
        return getItems().get(0);
    }

    public void setCurrent(Sample current) {
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
    }

    public String navigateToManageSpecimens() {
        prepareAdd();
        return "/admin/lims/manage_specimens?faces-redirect=true";
    }
    
    public String navigateToListItems() {
        return "/admin/lims/speciman_list?faces-redirect=true";
    }

    public String navigateToAddItem() {
        prepareAdd();
        return "/admin/lims/speciman?faces-redirect=true";
    }

    public String navigateToEditItem() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }
        return "/admin/lims/speciman?faces-redirect=true";
    }

    private SampleFacade getFacade() {
        return ejbFacade;
    }

    public List<Sample> getItems() {
        if (items == null) {
            items = fillAllItems();
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = Sample.class)
    public static class SampleControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SampleController controller = (SampleController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "sampleController");
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
            if (object instanceof Sample) {
                Sample o = (Sample) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SampleController.class.getName());
            }
        }
    }
}
