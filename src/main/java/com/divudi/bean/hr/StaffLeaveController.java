/*
 * Open Hospital Management Information System
 * Dr M H B Ariyaratne
 * buddhika.ari@gmail.com
 */
package com.divudi.bean.hr;


import com.divudi.data.hr.LeaveType;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.ejb.HumanResourceBean;
import com.divudi.entity.hr.Grade;
import com.divudi.entity.hr.StaffLeave;
import com.divudi.facade.StaffLeaveFacade;
import com.divudi.java.CommonFunctions;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Named;

/**
 *
 * @author safrin
 */
@Named
@SessionScoped
public class StaffLeaveController implements Serializable {

    private StaffLeave current;
    List<StaffLeave> staffLeaves;
    @EJB
    private StaffLeaveFacade staffLeaveFacade;

    private CommonFunctions commonFunctions;
    @EJB
    private HumanResourceBean humanResourceBean;

    public void createLeave() {
        staffLeaves = getHumanResourceBean().getStaffLeave(getCurrent().getStaff(),
                getCommonFunctions().getFirstDayOfYear(new Date()),
                getCommonFunctions().getLastDayOfYear(new Date()));
    }

    public LeaveType[] getLeaveType() {
        return LeaveType.values();
    }

    private boolean errorCheck() {
        if (getCurrent().getLeaveType() == null) {
            JsfUtil.addErrorMessage("Please select Leave Type");
            return true;
        }

        return false;
    }

    public void save() {
        if (errorCheck()) {
            return;
        }

        getStaffLeaveFacade().create(getCurrent());
        current = null;
        JsfUtil.addSuccessMessage("Staff Leave Added");
    }

    public void clear() {
        current = null;
    }

    /**
     * Creates a new instance of StaffLeaveController
     */
    public StaffLeaveController() {
    }

    public StaffLeave getCurrent() {
        if (current == null) {
            current = new StaffLeave();
        }
        return current;
    }

    public void setCurrent(StaffLeave current) {
        this.current = current;
    }

    public StaffLeaveFacade getStaffLeaveFacade() {
        return staffLeaveFacade;
    }

    public void setStaffLeaveFacade(StaffLeaveFacade staffLeaveFacade) {
        this.staffLeaveFacade = staffLeaveFacade;
    }

    public CommonFunctions getCommonFunctions() {
        return commonFunctions;
    }

    public void setCommonFunctions(CommonFunctions commonFunctions) {
        this.commonFunctions = commonFunctions;
    }

    public HumanResourceBean getHumanResourceBean() {
        return humanResourceBean;
    }

    public void setHumanResourceBean(HumanResourceBean humanResourceBean) {
        this.humanResourceBean = humanResourceBean;
    }

    public List<StaffLeave> getStaffLeaves() {
        return staffLeaves;
    }

    public void setStaffLeaves(List<StaffLeave> staffLeaves) {
        this.staffLeaves = staffLeaves;
    }
    
    

    @FacesConverter(forClass = StaffLeave.class)
    public static class LeaveConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StaffLeaveController controller = (StaffLeaveController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "staffLeaveController");
            return controller.getStaffLeaveFacade().find(getKey(value));
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
            if (object instanceof Grade) {
                Grade o = (Grade) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StaffLeaveController.class.getName());
            }
        }
    }

}
