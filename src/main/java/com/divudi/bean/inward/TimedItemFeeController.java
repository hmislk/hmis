/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.inward;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.entity.Department;
import com.divudi.entity.inward.TimedItemFee;
import com.divudi.entity.inward.TimedItem;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.TimedItemFeeFacade;
import com.divudi.facade.TimedItemFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
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
public class TimedItemFeeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private TimedItemFacade ejbFacade;
    @EJB
    private TimedItemFeeFacade itemFeeFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    private List<TimedItemFee> fees;
    private TimedItem currentIx;
    private TimedItemFee currentFee;
    private TimedItemFee removingTimedItemFee;

    public List<Department> getInstitutionDepatrments() {
        List<Department> d;
        //////System.out.println("gettin ins dep ");
        if (getCurrentFee().getInstitution() == null) {
            return new ArrayList<Department>();
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution=:ins";
            HashMap hm = new HashMap();
            hm.put("ins", getCurrentFee().getInstitution());
            d = getDepartmentFacade().findBySQL(sql, hm);
        }

        return d;
    }

    public void saveCharge() {
        if (currentIx == null) {
            UtilityController.addErrorMessage("Please select a charge");
            return;
        }
        if (currentFee == null) {
            UtilityController.addErrorMessage("Please select a charge");
            return;
        }
        currentFee.setItem(currentIx);
        if (currentFee.getId() == null || currentFee.getId() == 0) {
            currentFee.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            currentFee.setCreater(getSessionController().getLoggedUser());
            getTimedItemFeeFacade().create(currentFee);
            UtilityController.addSuccessMessage("Fee Added");
        } else {
            getTimedItemFeeFacade().edit(currentFee);
            UtilityController.addSuccessMessage("Fee Saved");
        }
        currentIx.setTotal(calTot());

        getEjbFacade().edit(currentIx);
        setCharges(null);
        currentFee = null;
    }

    private double calTot() {
        double tot = 0.0;

        for (TimedItemFee i : getCharges()) {
            tot += i.getFee();
        }
        return tot;
    }

    public TimedItemFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(TimedItemFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public TimedItemFeeController() {
    }

    public TimedItem getCurrentIx() {
        return currentIx;
    }

    public void setCurrentIx(TimedItem ix) {
        this.currentIx = ix;

    }

    public void updateFee(TimedItemFee tif) {
        if (currentIx == null) {
            UtilityController.addErrorMessage("Please select a Time Item");
            return;
        }
        if (tif.getName() == null) {
            UtilityController.addErrorMessage("Please Enter Fee Name.");
            return;
        }
        tif.setEditedAt(new Date());
        tif.setCreater(getSessionController().getLoggedUser());
        System.out.println("tif.getFee() = " + tif.getFee());
        getTimedItemFeeFacade().edit(tif);
        System.out.println("tif.getFee() = " + tif.getFee());
        JsfUtil.addSuccessMessage("Fee Updated");
        currentIx.setTotal(calTot());
        getEjbFacade().edit(currentIx);
        JsfUtil.addSuccessMessage("Time Item Updated");
    }

    public void removeFee() {
        if (currentIx == null) {
            UtilityController.addErrorMessage("Please select a investigation");
            return;
        }
        if (getRemovedTimedItemFee() == null) {
            UtilityController.addErrorMessage("Please select one to remove");
            return;
        }

        if (getRemovedTimedItemFee().getId() == null || getRemovedTimedItemFee().getId() == 0) {
            UtilityController.addErrorMessage("Nothing to remove");
            return;
        } else {
            getRemovedTimedItemFee().setRetired(true);
            getRemovedTimedItemFee().setRetirer(getSessionController().getLoggedUser());
            getRemovedTimedItemFee().setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getTimedItemFeeFacade().edit(getRemovedTimedItemFee()); // Flag as retired, so that will never appearing when calling from database
            fillCharges();
            currentIx.setTotal(calTot());
            getEjbFacade().edit(currentIx);

//            setCharges(null);
//            getCharges();
//            getCurrentIx().setTotal(calTot());
        }
    }

    public void delete() {

        if (currentIx != null) {
            currentIx.setRetired(true);
            currentIx.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            currentIx.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(currentIx);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }

        currentIx = null;
        setCharges(null);
    }

    private TimedItemFacade getFacade() {
        return ejbFacade;
    }

    public void fillCharges() {
        fees = new ArrayList<>();
        if (currentIx != null && currentIx.getId() != null) {
            HashMap hm = new HashMap();
            hm.put("it", getCurrentIx());
            fees = getTimedItemFeeFacade().findBySQL("select c from TimedItemFee c where c.retired = false and c.item=:it", hm);

        }
    }

    public List<TimedItemFee> getCharges() {
//        if (currentIx != null && currentIx.getId() != null) {
//            HashMap hm = new HashMap();
//            hm.put("it", getCurrentIx());
//            setCharges(getTimedItemFeeFacade().findBySQL("select c from TimedItemFee c where c.retired = false and c.item=:it", hm));
//        } else {
//            setCharges(new ArrayList<TimedItemFee>());
//        }
        if (fees == null) {
            fillCharges();
        }
        return fees;
    }

    public void setCharges(List<TimedItemFee> charges) {
        this.fees = charges;
    }

    public TimedItemFee getCurrentFee() {
        if (currentFee == null) {
            currentFee = new TimedItemFee();
            currentFee.setBooleanValue(true);
        }
        return currentFee;
    }

    public void setCurrentFee(TimedItemFee currentFee) {
        this.currentFee = currentFee;
    }

    public TimedItemFeeFacade getTimedItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setTimedItemFeeFacade(TimedItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;

    }

    public TimedItemFee getRemovedTimedItemFee() {
        return removingTimedItemFee;
    }

    public void setRemovedTimedItemFee(TimedItemFee removedTimedItemFee) {
        this.removingTimedItemFee = removedTimedItemFee;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public TimedItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(TimedItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;
    }

    public List<TimedItemFee> getFees() {
        return fees;
    }

    public void setFees(List<TimedItemFee> fees) {
        this.fees = fees;
    }

    public TimedItemFee getRemovingTimedItemFee() {
        return removingTimedItemFee;
    }

    public void setRemovingTimedItemFee(TimedItemFee removingTimedItemFee) {
        this.removingTimedItemFee = removingTimedItemFee;
    }

    /**
     *
     */
    @FacesConverter(forClass = TimedItemFee.class)
    public static class TimedItemFeeConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            TimedItemFeeController controller = (TimedItemFeeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "timedItemFeeController");
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
            if (object instanceof TimedItemFee) {
                TimedItemFee o = (TimedItemFee) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + TimedItemFeeController.class.getName());
            }
        }
    }

    @FacesConverter("conTimFee")
    public static class TimedItemFeeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            TimedItemFeeController controller = (TimedItemFeeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "timedItemFeeController");
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
            if (object instanceof TimedItemFee) {
                TimedItemFee o = (TimedItemFee) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + TimedItemFeeController.class.getName());
            }
        }
    }
}
