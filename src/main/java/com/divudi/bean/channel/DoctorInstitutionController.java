/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.channel;

import com.divudi.bean.common.*;
import com.divudi.facade.DoctorInstitutionFacade;
import com.divudi.entity.channel.DoctorInstitution;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
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
public class DoctorInstitutionController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private DoctorInstitutionFacade ejbFacade;
    private DoctorInstitution current;
    private List<DoctorInstitution> items = null;

    public List<DoctorInstitution> completeDoctorInstitution(String qry) {
        List<DoctorInstitution> list;
        String sql;
        HashMap hm = new HashMap();
        sql = "select c from DoctorInstitution c "
                + " where c.retired=false "
                + " and upper(c.name) like :q "
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        list = getFacade().findBySQL(sql, hm);

        if (list == null) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void prepareAdd() {
        current = new DoctorInstitution();
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

    public DoctorInstitutionFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(DoctorInstitutionFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public DoctorInstitutionController() {
    }

    public DoctorInstitution getCurrent() {
        if (current == null) {
            current = new DoctorInstitution();
        }
        return current;
    }

    public void setCurrent(DoctorInstitution current) {
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

    private DoctorInstitutionFacade getFacade() {
        return ejbFacade;
    }

    public List<DoctorInstitution> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = DoctorInstitution.class)
    public static class DoctorInstitutionConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DoctorInstitutionController controller = (DoctorInstitutionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "doctorInstitutionController");
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
            if (object instanceof DoctorInstitution) {
                DoctorInstitution o = (DoctorInstitution) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DoctorInstitutionController.class.getName());
            }
        }
    }

    @FacesConverter("doctorInstitutionConverter")
    public static class DoctorInstitutionControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            DoctorInstitutionController controller = (DoctorInstitutionController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "doctorInstitutionController");
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
            if (object instanceof DoctorInstitution) {
                DoctorInstitution o = (DoctorInstitution) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + DoctorInstitutionController.class.getName());
            }
        }
    }
}
