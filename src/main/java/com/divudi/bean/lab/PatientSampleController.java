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
import com.divudi.entity.lab.PatientSample;
import com.divudi.facade.PatientSampleFacade;
import com.divudi.java.CommonFunctions;
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
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class PatientSampleController implements Serializable {

    @Inject
    SessionController sessionController;
    @EJB
    private PatientSampleFacade ejbFacade;
    private PatientSample current;
    private List<PatientSample> items = null;

    private Date fromDate;
    private Date toDate;

    public List<PatientSample> fillAllItems() {
        String j = "select s "
                + " from PatientSample s "
                + " where s.retired=:ret "
                + " order by s.name";
        Map m = new HashMap();
        m.put("ret", false);
        return getFacade().findByJpql(j, m);
    }

    public void prepareAdd() {
        current = new PatientSample();
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

    public PatientSample findAndCreatePatientSampleByName(String qry) {
        PatientSample s;
        String jpql;
        jpql = "select s from "
                + " PatientSample s "
                + " where s.retired=:ret "
                + " and s.name=:name "
                + " order by s.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("name", qry);
        s = getFacade().findFirstByJpql(jpql, m);
        if (s == null) {
            s = new PatientSample();
            s.setCreatedAt(new Date());
            getFacade().create(s);
        }
        return s;
    }

    private PatientSampleFacade getEjbFacade() {
        return ejbFacade;
    }

    private SessionController getSessionController() {
        return sessionController;
    }

    public PatientSampleController() {
    }

    public PatientSample getCurrent() {
        if (current == null) {
            current = new PatientSample();
        }
        return current;
    }

    public String navigateToViewPatientSample(PatientSample ps) {
        if (ps == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }
        current = ps;
        return "/lab/patient_sample?faces-redirect=true;";

    }

    public String navigateToEditPatientSample(PatientSample ps) {
        if (ps == null) {
            JsfUtil.addErrorMessage("Nothing selected");
            return null;
        }
        current = ps;
        return "/lab/patient_sample_edit?faces-redirect=true;";
    }

    public PatientSample getAnyPatientSample() {
        return getItems().get(0);
    }

    public void setCurrent(PatientSample current) {
        this.current = current;
    }

    public void delete() {
        if (current != null) {
//            current.setRetired(true);
//            current.setRetiredAt(new Date());
//            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
    }

    public String navigateToListPatientSamples() {
//        prepareAdd();
        return "/lab/patient_samples?faces-redirect=true";
    }

    private PatientSampleFacade getFacade() {
        return ejbFacade;
    }

    public List<PatientSample> getItems() {
        return items;
    }

    public Date getFromDate() {
        if (fromDate == null) {
            fromDate = CommonFunctions.getStartOfDay();
        }
        return fromDate;
    }

    public void listPatientSamples() {
        PatientSample s = new PatientSample();
        String jpql;
        jpql = "select s from "
                + " PatientSample s "
                + " where s.createdAt between :fd and :td "
                + " order by s.id";
        Map m = new HashMap();
        m.put("fd", fromDate);
        m.put("td", toDate);
        items = getFacade().findByJpql(jpql, m, TemporalType.TIMESTAMP);
        if (s == null) {
            s = new PatientSample();
            s.setCreatedAt(new Date());
            getFacade().create(s);
        }

    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = CommonFunctions.getEndOfDay();
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    @FacesConverter(forClass = PatientSample.class)
    public static class SampleControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            PatientSampleController controller = (PatientSampleController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "patientSampleController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            return Long.valueOf(value);
        }

        String getStringKey(java.lang.Long value) {
            return value.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof PatientSample) {
                PatientSample o = (PatientSample) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PatientSample.class.getName());
            }
        }
    }

}
