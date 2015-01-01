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
import com.divudi.data.hr.ReportKeyWord;
import com.divudi.entity.hr.StaffShift;
import com.divudi.entity.hr.StaffShiftReplace;
import com.divudi.facade.StaffShiftFacade;
import java.io.Serializable;
import java.util.Calendar;
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
import javax.persistence.TemporalType;

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
    @Inject
    StaffController staffController;
    List<StaffShift> staffShifts;

    public void selectRosterListener() {
        shiftController.setCurrentRoster(getReportKeyWord().getRoster());
        staffController.setRoster(getReportKeyWord().getRoster());
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
        lst = ejbFacade.findBySQL(sql, hm);
        //   System.out.println("lst = " + lst);
        return lst;
    }

    public void updateDayOfWeekStaffShift() {
        List<StaffShift> lst;
        HashMap hm = new HashMap();
        String sql = "select c from StaffShift c "
                + " where c.retired=false "
                + " and c.dayOfWeek is null "
                + " and c.shiftDate is not null ";
        lst = ejbFacade.findBySQL(sql);

        if (lst == null) {
            return;
        }

        for (StaffShift ss : lst) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(ss.getShiftDate());
            ss.setDayOfWeek(cal.get(Calendar.DAY_OF_WEEK));
            staffShiftFacade.edit(ss);
        }
        //   System.out.println("lst = " + lst);

    }

    public void updateStaffShiftStartTimeAndEndTime() {
        List<StaffShift> lst;
        HashMap hm = new HashMap();
        String sql = "select c from StaffShift c "
                + " where c.retired=false "
                + " and c.shiftStartTime is not null"
                + " and c.shiftEndTime is not null ";
        lst = ejbFacade.findBySQL(sql);

        if (lst == null) {
            return;
        }

        for (StaffShift ss : lst) {
            ss.calShiftStartEndTime();
            staffShiftFacade.edit(ss);
        }
        //   System.out.println("lst = " + lst);

    }

    Date date;

    public void staffShiftListner(StaffShift staffShift) {
        reportKeyWord = null;

        date = staffShift.getShiftDate();
//        getReportKeyWord().setRoster(staffShift.getStaff().getRoster());

        System.err.println("Date " + date);

    }

    public List<StaffShift> completeStaffShiftDateRoster(String qry) {
        HashMap hm = new HashMap();
        String sql = "select c from"
                + " StaffShift c,StaffLeave s"
                + " where  c.staff=s.staff"
                + " and c.retired=false "
                + " and c.shiftDate= :dt "
                //                + " and c.staff.roster=:rs"
                + " and (upper(c.shift.name) like :q "
                + " or upper(c.staff.person.name) like :q)"
                + " and s.retired=false "
                + " and (s.fromDate >= c.shiftDate "
                + " and s.toDate <= c.shiftDate)";

        hm.put("dt", date);
//        hm.put("rs", getReportKeyWord().getRoster());
        hm.put("q", "%" + qry.toUpperCase() + "%");
        System.err.println("Qry " + qry);
        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);

        return staffShifts;
    }

    @EJB
    StaffShiftFacade staffShiftFacade;
    @Inject
    SessionController sessionController;

    public void replace() {
        if (getReportKeyWord().getStaffShift() == null) {
            return;
        }
        StaffShiftReplace shiftReplace = new StaffShiftReplace();
        shiftReplace.copy(getReportKeyWord().getStaffShift());
        shiftReplace.setReferenceStaffShift(getReportKeyWord().getStaffShift());
        shiftReplace.setCreatedAt(new Date());
        shiftReplace.setCreater(sessionController.getLoggedUser());
        shiftReplace.setStaff(getReportKeyWord().getReplacingStaff());

        staffShiftFacade.create(shiftReplace);
    }

    public void fetchLeavedStaffShift() {
        HashMap hm = new HashMap();
        String sql = "select c from"
                + " StaffShift c,StaffLeave s"
                + " where c.retired=false "
                + " and c.staff=s.staff "
                + " and c.shiftDate between :fd and :td "
                + " and c.shift=:sh"
                + " and c.staff=:stf "
                + " and s.retired=false"
                + " and s.staff=:stf"
                + " and (s.fromDate >= c.shiftDate "
                + " and s.toDate <= c.shiftDate)";

        hm.put("fd", getFromDate());
        hm.put("td", getToDate());
        hm.put("sh", getReportKeyWord().getShift());
        hm.put("stf", getReportKeyWord().getStaff());

        staffShifts = staffShiftFacade.findBySQL(sql, hm, TemporalType.DATE);
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

    public ShiftController getShiftController() {
        return shiftController;
    }

    public void setShiftController(ShiftController shiftController) {
        this.shiftController = shiftController;
    }

    public StaffController getStaffController() {
        return staffController;
    }

    public void setStaffController(StaffController staffController) {
        this.staffController = staffController;
    }

    public List<StaffShift> getStaffShifts() {
        return staffShifts;
    }

    public void setStaffShifts(List<StaffShift> staffShifts) {
        this.staffShifts = staffShifts;
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
