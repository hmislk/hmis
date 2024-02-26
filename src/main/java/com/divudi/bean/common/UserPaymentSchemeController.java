/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 Genealogical, Clinical, Storeoratory and Genetic Data
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;
import com.divudi.entity.Institution;
import com.divudi.entity.PaymentScheme;
import com.divudi.entity.WebUser;
import com.divudi.entity.WebUserPaymentScheme;
import com.divudi.facade.InstitutionFacade;
import com.divudi.facade.PaymentSchemeFacade;
import com.divudi.facade.WebUserPaymentSchemeFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent; import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import com.divudi.bean.common.util.JsfUtil;
/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 Informatics)
 */
@Named
@SessionScoped
public  class UserPaymentSchemeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private WebUserPaymentSchemeFacade ejbFacade;
    @EJB
    private PaymentSchemeFacade departmentFacade;
    @EJB
    private InstitutionFacade institutionFacade;
    List<WebUserPaymentScheme> selectedItems;
    private WebUserPaymentScheme current;
    private WebUser selectedUser;
    PaymentScheme currentPaymentScheme;
    private List<PaymentScheme> lstDep;
    private List<PaymentScheme> currentInsPaymentSchemes;
    private List<PaymentScheme> selectedUserDeparment;
    private List<Institution> selectedInstitutions;
    private List<WebUserPaymentScheme> items = null;
    String selectText = "";

    public PaymentScheme getCurrentPaymentScheme() {
        return currentPaymentScheme;
    }

    public void setCurrentPaymentScheme(PaymentScheme currentPaymentScheme) {
        this.currentPaymentScheme = currentPaymentScheme;
    }

    public void prepareAdd() {
        current = new WebUserPaymentScheme();
    }

    // Need new Enum WebUserPaymentScheme type
    public void setSelectedItems(List<WebUserPaymentScheme> selectedItems) {
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

    public UserPaymentSchemeController() {
    }

    public WebUserPaymentScheme getCurrent() {
        if (current == null) {
            current = new WebUserPaymentScheme();
        }
        return current;
    }

    public void setCurrent(WebUserPaymentScheme current) {
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

    public void addPaymentSchemeForUser() {
        if (selectedUser == null) {
            JsfUtil.addSuccessMessage("Select A User");
            return;
        }
        if (currentPaymentScheme == null) {
            JsfUtil.addSuccessMessage("Select a PaymentScheme");
            return;
        }

        for (WebUserPaymentScheme wup : getItems()) {
            if (wup.getPaymentScheme().getId() == getCurrentPaymentScheme().getId()) {
                return;
            }
        }

        WebUserPaymentScheme d = new WebUserPaymentScheme();
        d.setCreatedAt(Calendar.getInstance().getTime());
        ///other properties
        d.setPaymentScheme(currentPaymentScheme);
        d.setWebUser(selectedUser);

        getEjbFacade().create(d);
        currentPaymentScheme = null;
    }
    
    public List<WebUserPaymentScheme> getItemsBill() {


        String sql = "SELECT i FROM WebUserPaymentScheme i where i.retired=false and i.webUser.id = " + getSessionController().getLoggedUser().getId();
        items = getEjbFacade().findByJpql(sql);
        //////// // System.out.println("33");

        if (items == null) {
            items = new ArrayList<WebUserPaymentScheme>();
            //////// // System.out.println("44");
        }
        return items;
    }

    public List<WebUserPaymentScheme> getItems() {

        if (selectedUser == null) {
            items = new ArrayList<WebUserPaymentScheme>();
            return items;
        }

        String sql = "SELECT i FROM WebUserPaymentScheme i where i.retired=false and i.webUser.id = " + selectedUser.getId();
        items = getEjbFacade().findByJpql(sql);
        //////// // System.out.println("33");

        if (items == null) {
            items = new ArrayList<WebUserPaymentScheme>();
            //////// // System.out.println("44");
        }
        return items;
    }

    public WebUserPaymentSchemeFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(WebUserPaymentSchemeFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public WebUser getSelectedUser() {
        return selectedUser;
    }

    public void setSelectedUser(WebUser selectedUser) {
        //////// // System.out.println("Setting user");
        items=null;
        this.selectedUser = selectedUser;
        
    }

    public List<PaymentScheme> getSelectedUserDeparment() {
        if (getSelectedUser() == null) {
            return new ArrayList<PaymentScheme>();
        }

        String sql = "SELECT i.department FROM WebUserPaymentScheme i where i.retired=false and i.webUser=" + getSelectedUser() + "order by i.name";
        selectedUserDeparment = getPaymentSchemeFacade().findByJpql(sql);

        if (selectedUserDeparment == null) {
            selectedUserDeparment = new ArrayList<PaymentScheme>();
        }

        return selectedUserDeparment;
    }

    public void setSelectedUserDeparment(List<PaymentScheme> selectedUserDeparment) {
        this.selectedUserDeparment = selectedUserDeparment;
    }

    public PaymentSchemeFacade getPaymentSchemeFacade() {
        return departmentFacade;
    }

    public void setPaymentSchemeFacade(PaymentSchemeFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public List<Institution> getSelectedInstitutions() {
        if (getSelectedUser() == null) {
            return new ArrayList<Institution>();
        }

        String sql = "SELECT i.institution FROM WebUserPaymentScheme i where i.retired=false and i.webUser=" + getSelectedUser() + "order by i.name";
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

    public List<PaymentScheme> getCurrentInsPaymentSchemes() {

        String sql = "SELECT i FROM PaymentScheme i where i.retired=false order by i.name";
        currentInsPaymentSchemes = getPaymentSchemeFacade().findByJpql(sql);

        if (currentInsPaymentSchemes == null) {
            currentInsPaymentSchemes = new ArrayList<PaymentScheme>();
        }

        return currentInsPaymentSchemes;
    }

    public void setCurrentInsPaymentSchemes(List<PaymentScheme> currentInsPaymentSchemes) {
        this.currentInsPaymentSchemes = currentInsPaymentSchemes;
    }

    public List<PaymentScheme> getLstDep() {
        return lstDep;
    }

    public void setLstDep(List<PaymentScheme> lstDep) {
        this.lstDep = lstDep;
    }

    /**
     *
     */
    @FacesConverter(forClass = WebUserPaymentScheme.class)
    public static class PaymentSchemeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            UserPaymentSchemeController controller = (UserPaymentSchemeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "userPaymentSchemeController");
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
            if (object instanceof WebUserPaymentScheme) {
                WebUserPaymentScheme o = (WebUserPaymentScheme) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + UserPaymentSchemeController.class.getName());
            }
        }
    }
}
