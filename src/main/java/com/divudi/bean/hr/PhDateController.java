/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.hr.DayType;
import com.divudi.ejb.CommonFunctions;
import com.divudi.facade.PhDateFacade;
import com.divudi.entity.hr.PhDate;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class PhDateController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private PhDateFacade ejbFacade;
    @EJB
    CommonFunctions commonFunctions;
    private PhDate current;
    private List<PhDate> items = null;

    List<PhDate> phDates;

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date frDate;
    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    Date toDate;

    public List<PhDate> completePhDate(String qry) {
        List<PhDate> a = null;
        if (qry != null) {
            a = getFacade().findBySQL("select c from PhDate c where c.retired=false and upper(c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        }
        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

    public Long calHolidayCount(List<DayType> dayTypes, Date fromDate, Date toDate) {
        String sql = "select count(c) from "
                + " PhDate c "
                + " where c.retired=false"
                + " and c.dayType in :dt "
                + " and c.phDate between :fd and :td ";
        HashMap hm = new HashMap();
        hm.put("fd", fromDate);
        hm.put("td", toDate);
        hm.put("dt", dayTypes);
        Long dbl = getFacade().findLongByJpql(sql, hm, TemporalType.TIMESTAMP);

        return dbl;

    }

    public void createHollydays() {
        String sql;
        HashMap m = new HashMap();

        sql = "Select d From PhDate d "
                + " Where d.retired=false "
                + " and d.phDate between :fd and :td "
                + " order by d.phDate ";
        
        m.put("fd", frDate);
        m.put("td", toDate);
        
        phDates=getFacade().findBySQL(sql, m);
        
    }

    public DayType getHolidayType(Date d) {
        String sql = "Select d.phType From PhDate d "
                + " Where d.retired=false "
                + " and d.phDate=:dtd ";
        HashMap hm = new HashMap();
        hm.put("dtd", d);

        Object obj = ejbFacade.findFirstObjectBySQL(sql, hm, TemporalType.DATE);

        return obj != null ? (DayType) obj : null;

    }

    public void prepareAdd() {
        current = new PhDate();
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

    public PhDateFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(PhDateFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public PhDateController() {
    }

    public PhDate getCurrent() {
        if (current == null) {
            current = new PhDate();
        }
        return current;
    }

    public void setCurrent(PhDate current) {
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

    private PhDateFacade getFacade() {
        return ejbFacade;
    }

    public List<PhDate> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    public Date getFrDate() {
        if (frDate == null) {
            frDate = com.divudi.java.CommonFunctions.getStartOfMonth(new Date());
        }
        return frDate;
    }

    public void setFrDate(Date frDate) {
        this.frDate = frDate;
    }

    public Date getToDate() {
        if (toDate == null) {
            toDate = com.divudi.java.CommonFunctions.getEndOfMonth(new Date());
        }
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public List<PhDate> getPhDates() {
        return phDates;
    }

    public void setPhDates(List<PhDate> phDates) {
        this.phDates = phDates;
    }

    /**
     *
     */
    @FacesConverter("phDateCon")
    public static class PhDateControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PhDateController controller = (PhDateController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "phDateController");
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
            if (object instanceof PhDate) {
                PhDate o = (PhDate) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PhDateController.class.getName());
            }
        }
    }
}
