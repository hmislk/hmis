/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.hr.DayType;

import com.divudi.entity.hr.PhDate;
import com.divudi.facade.PhDateFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class PhDateController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    CommonController commonController;
    @EJB
    private PhDateFacade ejbFacade;

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
            a = getFacade().findByJpql("select c from PhDate c where c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
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
        Date startTime = new Date();
        
        String sql;
        HashMap m = new HashMap();

        sql = "Select d From PhDate d "
                + " Where d.retired=false "
                + " and d.phDate between :fd and :td "
                + " order by d.phDate ";

        m.put("fd", frDate);
        m.put("td", toDate);

        phDates = getFacade().findByJpql(sql, m);
        
        

    }

    public DayType getHolidayType(Date d) {
        String sql = "Select d.phType From PhDate d "
                + " Where d.retired=false "
                + " and d.phDate=:dtd ";
        HashMap hm = new HashMap();
        hm.put("dtd", d);

        Object obj = ejbFacade.findFirstObjectByJpql(sql, hm, TemporalType.DATE);

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

    private PhDateFacade getFacade() {
        return ejbFacade;
    }

    public List<PhDate> getItems() {
        if (items == null) {
            String j;
            j="select j "
                    + " from PhDate j "
                    + " where j.retired=false "
                    + " order by j.phDate desc";
            items = getFacade().findByJpql(j);
        }
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

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }
    
    
}
