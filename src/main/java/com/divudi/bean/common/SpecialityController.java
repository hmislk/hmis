/*
 * MSc(Biomedical Informatics) Project
 * 
 * Development and Implementation of a Web-based Combined Data Repository of 
 Genealogical, Clinical, Laboratory and Genetic Data 
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;
import java.util.TimeZone;
import com.divudi.facade.SpecialityFacade;
import com.divudi.entity.Speciality;
import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named; import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 Informatics)
 */
@Named
@SessionScoped
public  class SpecialityController implements Serializable {

    @Inject
    SessionController sessionController;
    @EJB
    private SpecialityFacade ejbFacade;
    List<Speciality> selectedItems;
    private Speciality current;
    private List<Speciality> items = null;
    String selectText = "";

    public List<Speciality> completeSpeciality(String qry) {
        selectedItems = getFacade().findBySQL("select c from Speciality c where c.retired=false and upper(c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        return selectedItems;
    }
    
    public List<Speciality> getSelectedItems() {
       if (selectText.trim().equals("")) {
            selectedItems = getFacade().findBySQL("select c from Speciality c where c.retired=false order by c.name");
        } else {
            selectedItems = getFacade().findBySQL("select c from Speciality c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        }

        return selectedItems;
    }

    public void prepareAdd() {
        current = new Speciality();
    }

    public void setSelectedItems(List<Speciality> selectedItems) {
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
            UtilityController.addSuccessMessage("savedOldSuccessfully");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("savedNewSuccessfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public SpecialityFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(SpecialityFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public SpecialityController() {
    }

    public Speciality getCurrent() {
        if(current==null){
            current= new Speciality();
        }
        return current;
    }

    public void setCurrent(Speciality current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("DeleteSuccessfull");
        } else {
            UtilityController.addSuccessMessage("NothingToDelete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private SpecialityFacade getFacade() {
        return ejbFacade;
    }

    public List<Speciality> getItems() {
       if (items == null) {
            String temSql;
            temSql = "SELECT i FROM Speciality i where i.retired=false order by i.name";
            //System.out.println("Sql for SpacilityController.getItems is " + temSql);
            items = getFacade().findBySQL(temSql);
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = Speciality.class)
    public static class SpecialityControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SpecialityController controller = (SpecialityController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "specialityController");
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
            if (object instanceof Speciality) {
                Speciality o = (Speciality) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SpecialityController.class.getName());
            }
        }
    }
    
    
    @FacesConverter("specilityCon")
    public static class SpecialityConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SpecialityController controller = (SpecialityController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "specialityController");
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
            if (object instanceof Speciality) {
                Speciality o = (Speciality) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + SpecialityController.class.getName());
            }
        }
    }
    
}
