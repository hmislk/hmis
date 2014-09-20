/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.hr;

import com.divudi.data.hr.ReportKeyWord;
import com.divudi.entity.hr.StaffShift;
import com.divudi.facade.StaffShiftFacade;
import java.io.Serializable;
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

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class StaffShiftController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    StaffShiftFacade ejbFacade;
    Date fromDate;
    Date toDate;
    ReportKeyWord reportKeyWord;
    @Inject
    ShiftController shiftController;

    public void selectRosterListener() {
        shiftController.setCurrentRoster(getReportKeyWord().getRoster());
    }

    public List<StaffShift> completeStaffShift(String qry) {
        if (qry == null) {
            return null;
        }

        List<StaffShift> lst;
        HashMap hm = new HashMap();
        String sql = "select c from StaffShift c "
                + " where c.retired=false "
                + " and (upper(c.shift.name) like :q "
                + " or upper(c.staff.person.name) like :q)"
                + " order by c.name";
        hm.put("q", "%" + qry.toUpperCase() + "%");
        lst = ejbFacade.findBySQL(sql);
        //   System.out.println("lst = " + lst);
        return lst;
    }

    public void makeNull() {
        fromDate = null;
        toDate = null;
        reportKeyWord = null;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    public ReportKeyWord getReportKeyWord() {
        if (reportKeyWord == null) {
            reportKeyWord = new ReportKeyWord();
        }
        return reportKeyWord;
    }

    public void setReportKeyWord(ReportKeyWord reportKeyWord) {
        this.reportKeyWord = reportKeyWord;
    }

    public StaffShiftFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(StaffShiftFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    /**
     *
     */
    @FacesConverter(forClass = StaffShift.class)
    public static class StaffShiftControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StaffShiftController controller = (StaffShiftController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "staffShiftController");
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
            if (object instanceof StaffShift) {
                StaffShift o = (StaffShift) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StaffShiftController.class.getName());
            }
        }
    }

    /**
     *
     */
    @FacesConverter("staffShiftConverter")
    public static class StaffShiftConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StaffShiftController controller = (StaffShiftController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "staffShiftController");
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
            if (object instanceof StaffShift) {
                StaffShift o = (StaffShift) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StaffShiftController.class.getName());
            }
        }
    }

}
