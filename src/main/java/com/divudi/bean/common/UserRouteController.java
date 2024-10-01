/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 Genealogical, Clinical, Storeoratory and Genetic Data
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Route;
import com.divudi.entity.Institution;
import com.divudi.entity.WebUser;
import com.divudi.entity.WebUserRoute;
import com.divudi.facade.RouteFacade;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.WebUserRouteFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
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
 * Informatics)
 */
@Named
@SessionScoped
public class UserRouteController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private WebUserRouteFacade ejbFacade;
    @EJB
    private RouteFacade RouteFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    List<WebUserRoute> selectedItems;
    private WebUserRoute current;
    private WebUser selectedUser;
    private Institution currentInstituion;
    Route currentRoute;
    private List<Route> lstDep;
    private List<Route> currentInsRoutes;
    private List<Route> selectedUserDeparment;
    private List<Institution> selectedInstitutions;
    private List<WebUserRoute> items = null;
    String selectText = "";

    public Route getCurrentRoute() {
        return currentRoute;
    }

    public void setCurrentRoute(Route currentRoute) {
        this.currentRoute = currentRoute;
    }

    public void prepareAdd() {
        current = new WebUserRoute();
    }

    // Need new Enum WebUserRoute type
    public void setSelectedItems(List<WebUserRoute> selectedItems) {
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
            getEjbFacade().edit(current);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(new Date());
            current.setCreater(getSessionController().getLoggedUser());
            getEjbFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public UserRouteController() {
    }

    public WebUserRoute getCurrent() {
        if (current == null) {
            current = new WebUserRoute();
        }
        return current;
    }

    public void setCurrent(WebUserRoute current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(new Date());
            current.setRetirer(getSessionController().getLoggedUser());
            getEjbFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    public void addRouteForUser() {
        if (selectedUser == null) {
            JsfUtil.addSuccessMessage("Select A User");
            return;
        }
        if (currentRoute == null) {
            JsfUtil.addSuccessMessage("Select a Route");
            return;
        }
        WebUserRoute d = new WebUserRoute();
        d.setCreatedAt(Calendar.getInstance().getTime());
        ///other properties
        d.setRoute(currentRoute);
        d.setWebUser(selectedUser);
        getEjbFacade().create(d);
        items=null;
        currentRoute = null;
        
    }

    public List<WebUserRoute> fillWebUserRoutes(WebUser wu) {
        List<WebUserRoute> tis = new ArrayList<>();
        String sql = "SELECT i "
                + " FROM WebUserRoute i "
                + " where i.retired=:ret "
                + " and i.webUser=:wu "
                + "  order by i.Route.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("wu", wu);
        tis = getEjbFacade().findByJpql(sql, m);
        return tis;
    }

    public List<WebUserRoute> getItems() {
        if (selectedUser == null) {
            items = new ArrayList<>();
            return items;
        }
        if (items == null) {
            items = fillWebUserRoutes(selectedUser);
        }
        return items;
    }
    
    

    public WebUserRouteFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(WebUserRouteFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public WebUser getSelectedUser() {
        return selectedUser;
    }

    public void setSelectedUser(WebUser selectedUser) {
        //////// // System.out.println("Setting user");
        this.selectedUser = selectedUser;
    }

    public List<Route> getSelectedUserDeparment() {
        if (getSelectedUser() == null) {
            return new ArrayList<Route>();
        }

        String sql = "SELECT i.Route FROM WebUserRoute i where i.retired=false and i.webUser=" + getSelectedUser() + "order by i.name";
        selectedUserDeparment = getRouteFacade().findByJpql(sql);

        if (selectedUserDeparment == null) {
            selectedUserDeparment = new ArrayList<Route>();
        }

        return selectedUserDeparment;
    }

    public void setSelectedUserDeparment(List<Route> selectedUserDeparment) {
        this.selectedUserDeparment = selectedUserDeparment;
    }

    public RouteFacade getRouteFacade() {
        return RouteFacade;
    }

    public void setRouteFacade(RouteFacade RouteFacade) {
        this.RouteFacade = RouteFacade;
    }

    public List<Institution> getSelectedInstitutions() {
        if (getSelectedUser() == null) {
            return new ArrayList<Institution>();
        }

        String sql = "SELECT i.institution FROM WebUserRoute i where i.retired=false and i.webUser=" + getSelectedUser() + "order by i.name";
        selectedInstitutions = getInstitutionFacade().findByJpql(sql);

        if (selectedInstitutions == null) {
            selectedInstitutions = new ArrayList<Institution>();
        }

        return selectedInstitutions;
    }

    public void setSelectedInstitutions(List<Institution> selectedInstitutions) {
        this.selectedInstitutions = selectedInstitutions;
    }

    public InstitutionFacade getInstitutionFacade() {
        return institutionFacade;
    }

    public void setInstitutionFacade(InstitutionFacade institutionFacade) {
        this.institutionFacade = institutionFacade;
    }

    public Institution getCurrentInstituion() {
        return currentInstituion;
    }

    public void setCurrentInstituion(Institution currentInstituion) {
        this.currentInstituion = currentInstituion;
//        getCurrentInsRoutes();
    }

//    public List<Route> getCurrentInsRoutes() {
//        if (currentInstituion == null) {
//            //////// // System.out.println("1");
//            return new ArrayList<>();
//        }
//        //////// // System.out.println("2");
//        Map m = new HashMap();
//        m.put("ins", currentInstituion);
//        String sql = "SELECT i FROM Route i where i.retired=false and i.institution=:ins order by i.name";
//        currentInsRoutes = getRouteFacade().findByJpql(sql,m);
//        //////// // System.out.println("3");
//        if (currentInsRoutes == null) {
//            //////// // System.out.println("4");
//            currentInsRoutes = new ArrayList<>();
//        }
//        return currentInsRoutes;
//    }
    public void setCurrentInsRoutes(List<Route> currentInsRoutes) {
        this.currentInsRoutes = currentInsRoutes;
    }

    public List<Route> getLstDep() {
        return lstDep;
    }

    public void setLstDep(List<Route> lstDep) {
        this.lstDep = lstDep;
    }

    public void setItems(List<WebUserRoute> items) {
        this.items = items;
    }

    /**
     *
     */
    @FacesConverter(forClass = WebUserRoute.class)
    public static class UserRouteControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            UserRouteController controller = (UserRouteController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "userRouteController");
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
            if (object instanceof WebUserRoute) {
                WebUserRoute o = (WebUserRoute) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + WebUserRoute.class.getName());
            }
        }
    }
}
