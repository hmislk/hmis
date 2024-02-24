/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.data.InstitutionType;
import com.divudi.entity.Institution;
import com.divudi.entity.Route;
import com.divudi.facade.RouteFacade;
import com.divudi.bean.common.util.JsfUtil;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class RouteController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private RouteFacade ejbFacade;
    private Route current;
    private List<Route> items = null;

    public String toAddNewRoute() {
        current = new Route();
        return "/admin/institutions/route?faces-redirect=true";
    }
    public String manageRoutes() {
        current = new Route();
        return "/admin/institutions/manage_routes?faces-redirect=true";
    }

    public String toListRoutes() {
        fillItems();
        return "/admin/institutions/routes?faces-redirect=true";
    }

    public String toEditRoute() {
        if (current == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return "";
        }
        return "/admin/institutions/route";
    }

    public void fillItems() {
        String j;
        j = "select i "
                + " from Route i "
                + " where i.retired=:ret"
                + " order by i.name";
        Map m = new HashMap();
        m.put("ret", false);
        items = getFacade().findByJpql(j, m);
    }

    public List<Route> completeRoute(String qry) {
        List<Route> list;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from Route c "
                + " where c.retired=false "
                + " and (c.name) like :q "
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        list = getFacade().findByJpql(sql, hm);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void prepareAdd() {
        current = new Route();
    }

    private void recreateModel() {
        items = null;
    }

    public Route findRouteByName(String name) {
        if (name == null) {
            return null;
        }
        if (name.trim().equals("")) {
            return null;
        }
        String jpql = "select c "
                + " from Route c "
                + " where c.retired=:ret "
                + " and c.name=:n";
        Map m = new HashMap<>();
        m.put("ret", false);
        m.put("n", name);
        return getFacade().findFirstByJpql(jpql, m);
    }
    
    public Route findAndCreateRouteByName(String name){
        Route r =null;
        if (name == null) {
            return null;
        }
        if (name.trim().equals("")) {
            return null;
        }
        String jpql = "select c "
                + " from Route c "
                + " where c.retired=:ret "
                + " and c.name=:n";
        Map m = new HashMap<>();
        m.put("ret", false);
        m.put("n", name);
        r =  getFacade().findFirstByJpql(jpql, m);
        
        if(r==null){
            r = new Route();
            r.setName(name);
            r.setCreatedAt(new Date());
            r.setCreater(sessionController.getLoggedUser());
            getFacade().create(r);
        }
        recreateModel();
        getItems();
        return r;
    }

    public void saveSelected() {
        if (getCurrent().getName().isEmpty() || getCurrent().getName() == null) {
            JsfUtil.addErrorMessage("Please enter Value");
            return;
        }

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

    public void save(Route r) {
        if (r == null) {
            return;
        }
        if (r.getId() != null) {
            getFacade().edit(r);
        } else {
            r.setCreatedAt(new Date());
            r.setCreater(getSessionController().getLoggedUser());
            getFacade().create(r);
        }
    }

    public RouteFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(RouteFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public RouteController() {
    }

    public Route getCurrent() {
        if (current == null) {
            current = new Route();
        }
        return current;
    }

    public void setCurrent(Route current) {
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

    private RouteFacade getFacade() {
        return ejbFacade;
    }

    public List<Route> getItems() {
        if (items == null) {
            fillItems();
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = Route.class)
    public static class RouteConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            RouteController controller = (RouteController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "routeController");
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
            if (object instanceof Route) {
                Route o = (Route) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + RouteController.class.getName());
            }
        }
    }

}
