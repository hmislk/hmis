/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.hr;

import com.divudi.bean.common.OpdPreBillController;
import com.divudi.bean.common.SessionController;

import com.divudi.bean.opd.OpdBillController;
import com.divudi.entity.hr.FingerPrintRecord;
import com.divudi.entity.hr.StaffShift;
import com.divudi.entity.hr.WorkingTime;
import com.divudi.facade.WorkingTimeFacade;
import com.divudi.bean.common.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
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

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class WorkingTimeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @Inject
    FingerPrintRecordController fingerPrintRecordController;
    @Inject
    StaffShiftController staffShiftController;
    @Inject
    OpdBillController opdBillController;
    @Inject
    OpdPreBillController opdPreBillController;
    @EJB
    private WorkingTimeFacade ejbFacade;
    List<WorkingTime> selectedItems;
    private WorkingTime current;
    private List<WorkingTime> items = null;
    String selectText = "";

    public List<WorkingTime> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from WorkingTime c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public String navigateToMarkIn() {
        current = new WorkingTime();
        StaffShift staffShift = new StaffShift();
        current.setStaffShift(staffShift);
        FingerPrintRecord sr = new FingerPrintRecord();
        sr.setRecordTimeStamp(new Date());
        current.setStartRecord(sr);
        return "/opd/markIn";
    }

    public String markIn() {
        fingerPrintRecordController.save(current.getStartRecord());
        staffShiftController.save(current.getStaffShift());
        save(current);
        JsfUtil.addSuccessMessage("Marked In");
        opdBillController.reloadCurrentlyWorkingStaff();
        opdPreBillController.reloadCurrentlyWorkingStaff();
        return navigateToListCurrentWorkTimes();
    }

    public String markOut() {
        FingerPrintRecord er = new FingerPrintRecord();
        er.setRecordTimeStamp(new Date());
        fingerPrintRecordController.save(er);
        current.setEndRecord(er);
        save(current);
        opdBillController.reloadCurrentlyWorkingStaff();
        opdPreBillController.reloadCurrentlyWorkingStaff();

        return navigateToListCurrentWorkTimes();
    }

    public String cancel() {
        current.setRetired(true);
        current.setRetiredAt(new Date());
        current.setRetirer(sessionController.getLoggedUser());
        save(current);
        return navigateToListWorkTimes();
    }

    public String navigateToViewWorkTime() {
        String j = "select w "
                + " from WorkingTime w "
                + " where w.retired=:ret ";
        Map m = new HashMap();
        m.put("ret", false);
        items = getFacade().findByJpql(j, m);
        return "/opd/workTimes";
    }

    public void saveWorkTime() {
        save(current);
        JsfUtil.addSuccessMessage("Saved");
    }

    public String navigateToListCurrentWorkTimes() {
        items = findCurrentlyActiveWorkingTimes();
        return "/opd/marked_ins_current";
    }

    public List<WorkingTime> findCurrentlyActiveWorkingTimes() {
        String j = "select w "
                + " from WorkingTime w "
                + " where w.retired=:ret "
                + " and w.endRecord is null";
        Map m = new HashMap();
        m.put("ret", false);
        return getFacade().findByJpql(j, m);
    }

    public String navigateToListWorkTimes() {
        String j = "select w "
                + " from WorkingTime w "
                + " where w.retired=:ret "
                + " and w.endRecord is null";
        Map m = new HashMap();
        m.put("ret", false);
        items = getFacade().findByJpql(j, m);
        return "/opd/workTimes";
    }

    public List<WorkingTime> completeWorkingTime(String qry) {
        List<WorkingTime> a = null;
        if (qry != null) {
            a = getFacade().findByJpql("select c from WorkingTime c where c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        }
        if (a == null) {
            a = new ArrayList<WorkingTime>();
        }
        return a;
    }

    public void prepareAdd() {
        current = new WorkingTime();
    }

    public void setSelectedItems(List<WorkingTime> selectedItems) {
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

    public void save(WorkingTime t) {
        if (t.getId() != null) {
            getFacade().edit(t);
        } else {
            t.setCreatedAt(new Date());
            t.setCreater(getSessionController().getLoggedUser());
            getFacade().create(t);
        }
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public WorkingTimeFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(WorkingTimeFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public WorkingTimeController() {
    }

    public WorkingTime getCurrent() {
        if (current == null) {
            current = new WorkingTime();
        }
        return current;
    }

    public void setCurrent(WorkingTime current) {
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

    private WorkingTimeFacade getFacade() {
        return ejbFacade;
    }

    public List<WorkingTime> getItems() {
        if (items == null) {
            String j;
            j = "select d from WorkingTime d where d.retired=false order by d.name";
            items = getFacade().findByJpql(j);
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = WorkingTime.class)
    public static class WorkingTimeConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            WorkingTimeController controller = (WorkingTimeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "workingTimeController");
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
            if (object instanceof WorkingTime) {
                WorkingTime o = (WorkingTime) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + WorkingTimeController.class.getName());
            }
        }
    }

}
