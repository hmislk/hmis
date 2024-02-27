/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.lab;

import com.divudi.bean.common.SessionController;

import com.divudi.entity.lab.InvestigationTube;
import com.divudi.facade.InvestigationTubeFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
 * Informatics)
 */
@Named
@SessionScoped
public class InvestigationTubeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;

    @EJB
    private InvestigationTubeFacade ejbFacade;

    private InvestigationTube current;
    private List<InvestigationTube> items = null;
    
    public String navigateToManageContainers() {
        prepareAdd();
        return "/admin/lims/manage_containers?faces-redirect=true";
    }

    public String navigateToAddTube() {
        current = new InvestigationTube();
        return "/admin/lims/tube?faces-redirect=true";
    }

    public InvestigationTube getAnyTube(){
        return getItems().get(0);
    }
    
    public String navigateToEditTube() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing Selected");
            return "";
        }
        return "/admin/lims/tube?faces-redirect=true";
    }

    public String navigateToListTubes() {
        getItems();
        return "/admin/lims/tube_list?faces-redirect=true";
    }

    public void prepareAdd() {
        current = new InvestigationTube();
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
            current.setCreater(sessionController.getLoggedUser());
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }
    
    public InvestigationTube findAndCreateInvestigationTubeByName(String qry) {
        InvestigationTube i;
        String jpql;
        jpql = "select i from "
                + " InvestigationTube i "
                + " where i.retired=:ret "
                + " and i.name=:name "
                + " order by i.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("name", qry);
        i = getFacade().findFirstByJpql(jpql, m);
        if(i==null){
            i = new InvestigationTube();
            i.setName(qry);
            i.setCreatedAt(new Date());
            i.setCreater(sessionController.getLoggedUser());
            getFacade().create(i);
        }
        return i;
    }

    public InvestigationTubeFacade getEjbFacade() {
        return ejbFacade;
    }

    public InvestigationTubeController() {
    }

    public InvestigationTube getCurrent() {
        if(current == null){
            current = new InvestigationTube();
        }
        return current;
    }

    public void setCurrent(InvestigationTube current) {
        this.current = current;
    }

    public void delete() {
        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(sessionController.getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
    }

    private InvestigationTubeFacade getFacade() {
        return ejbFacade;
    }

    public List<InvestigationTube> getItems() {
        if (items == null) {
            items = fillItems();
        }
        return items;
    }

    private List<InvestigationTube> fillItems() {
        String jpql = "select c "
                + " from InvestigationTube c "
                + " where c.retired=:ret "
                + " order by c.name";
        Map m = new HashMap();
        m.put("ret", false);
        return getFacade().findByJpql(jpql, m);
    }

    /**
     *
     */
    @FacesConverter(forClass = InvestigationTube.class)
    public static class InvestigationTubeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            InvestigationTubeController controller = (InvestigationTubeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "investigationTubeController");
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
            if (object instanceof InvestigationTube) {
                InvestigationTube o = (InvestigationTube) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + InvestigationTubeController.class.getName());
            }
        }
    }
}
