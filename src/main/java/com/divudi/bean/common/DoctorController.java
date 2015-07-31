/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import com.divudi.entity.Consultant;
import java.util.TimeZone;
import com.divudi.entity.Person;
import com.divudi.facade.DoctorFacade;
import com.divudi.entity.Doctor;
import com.divudi.entity.Speciality;
import com.divudi.facade.PersonFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
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
public class DoctorController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private DoctorFacade ejbFacade;
    @EJB
    private PersonFacade personFacade;
    List<Doctor> selectedItems;
    private Doctor current;
    private List<Doctor> items = null;
    String selectText = "";
    List<Doctor> doctors;
    Speciality speciality;
    

    public List<Doctor> completeDoctor(String query) {
        List<Doctor> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = " select p from Doctor p "
                    + " where p.retired=false "
                    + " and upper(p.person.name) like :q "
                    + " order by p.person.name";
            HashMap hm = new HashMap();
            hm.put("q", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findBySQL(sql,hm);
        }
        return suggestions;
    }

    public void listDoctors(){
         String temSql;
            temSql = "SELECT d FROM Doctor d where d.retired=false ";
            doctors = getFacade().findBySQL(temSql);   

    }
    public List<Doctor> getSelectedItems() {
        String sql = "";
        HashMap hm = new HashMap();
        hm.put("class", Consultant.class);
        if (selectText.trim().equals("")) {
            sql = "select c from Doctor c "
                    + " where c.retired=false "
                    + " and type(c)!=:class "
                    + "order by c.person.name";

        } else {
            sql = "select c from Doctor c "
                    + "where c.retired=false "
                    + " and type(c)!=:class "
                    + " and upper(c.person.name) like :q "
                    + " order by c.person.name";

            hm.put("q", "%" + getSelectText().toUpperCase() + "%");
        }

        selectedItems = getFacade().findBySQL(sql, hm);

        return selectedItems;
    }

    public void prepareAdd() {
        current = new Doctor();
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
      //  getItems();
        current = null;
        getCurrent();
    }

    public void setSelectedItems(List<Doctor> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    public void saveSelected() {
        if (current == null) {
            UtilityController.addErrorMessage("Nothing to save");
            return;
        }
        if (current.getPerson() == null) {
            UtilityController.addErrorMessage("Nothing to save");
            return;
        }
        if (current.getPerson().getName().trim().equals("")) {
            UtilityController.addErrorMessage("Please enter a name");
            return;
        }
        if (current.getSpeciality()==null) {
            UtilityController.addErrorMessage("Please Select Speciality.");
            return;
        }
        if (current.getPerson().getId() == null || current.getPerson().getId() == 0) {
            getPersonFacade().create(current.getPerson());
        } else {
            getPersonFacade().edit(current.getPerson());
        }
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
       // getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public DoctorFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(DoctorFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public DoctorController() {
    }

    public Doctor getCurrent() {
        if (current == null) {
            Person p = new Person();
            current = new Doctor();
            current.setPerson(p);
        }
        return current;
    }

    public void setCurrent(Doctor current) {
        this.current = current;
    }

    public List<Doctor> getDoctors() {
        return doctors;
    }

    public void setDoctors(List<Doctor> doctors) {
        this.doctors = doctors;
    }

    private DoctorFacade getFacade() {
        return ejbFacade;
    }

    public List<Doctor> getItems() {
        if (items == null) {
            String temSql;
            temSql = "SELECT i FROM Doctor i where i.retired=false ";
            items = getFacade().findBySQL(temSql);
        }
        return items;
    }

    public PersonFacade getPersonFacade() {
        return personFacade;
    }

    public void setPersonFacade(PersonFacade personFacade) {
        this.personFacade = personFacade;
    }

    public Speciality getSpeciality() {
        return speciality;
    }

    public void setSpeciality(Speciality speciality) {
        this.speciality = speciality;
    }

    
    
    /**
     *
     */
    @FacesConverter(forClass = Doctor.class)
    public static class DoctorControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DoctorController controller = (DoctorController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "doctorController");
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
            if (object instanceof Doctor) {
                Doctor o = (Doctor) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DoctorController.class.getName());
            }
        }
    }

    @FacesConverter("conDoc")
    public static class DoctorConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DoctorController controller = (DoctorController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "doctorController");
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
            if (object instanceof Doctor) {
                Doctor o = (Doctor) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DoctorController.class.getName());
            }
        }
    }
}
