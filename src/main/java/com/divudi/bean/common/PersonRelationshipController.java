package com.divudi.bean.common;

import java.util.TimeZone;
import com.divudi.facade.PersonRelationshipFacade;
import com.divudi.entity.PersonRelationship;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
@SuppressWarnings("serial")
public class PersonRelationshipController implements Serializable {

    @Inject
    SessionController sessionController;
    @EJB
    private PersonRelationshipFacade ejbFacade;
    List<PersonRelationship> selectedItems;
    private PersonRelationship current;
    private List<PersonRelationship> items = null;
    String selectText = "";

    public List<PersonRelationship> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from PersonRelationship c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new PersonRelationship();
    }

    public void setSelectedItems(List<PersonRelationship> selectedItems) {
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
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public PersonRelationshipFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(PersonRelationshipFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PersonRelationshipController() {
    }

    public PersonRelationship getCurrent() {
        return current;
    }

    public void setCurrent(PersonRelationship current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private PersonRelationshipFacade getFacade() {
        return ejbFacade;
    }

    public List<PersonRelationship> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = PersonRelationship.class)
    public static class PersonRelationshipControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PersonRelationshipController controller = (PersonRelationshipController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "personRelationshipController");
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
            if (object instanceof PersonRelationship) {
                PersonRelationship o = (PersonRelationship) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PersonRelationshipController.class.getName());
            }
        }
    }
}
