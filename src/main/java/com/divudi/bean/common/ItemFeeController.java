/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.ItemFee;
import com.divudi.entity.Staff;
import com.divudi.entity.lab.Investigation;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.ItemFeeFacade;
import com.divudi.facade.StaffFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import org.primefaces.event.SelectEvent;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class ItemFeeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private InvestigationFacade ejbFacade;
    @EJB
    private ItemFeeFacade itemFeeFacade;
    private List<ItemFee> fees;
    private Investigation currentIx;
    private ItemFee currentFee;
    private ItemFee removingItemFee;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private StaffFacade staffFacade;

    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<Staff>();
        } else {
            if (getCurrentFee().getSpeciality() == null) {
                sql = "select p from Staff p where p.retired=false and (upper(p.person.name) like '%" + query.toUpperCase() + "%'or  upper(p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            } else {
                sql = "select p from Staff p where p.speciality.id=" + getCurrentFee().getSpeciality().getId() + " and p.retired=false and (upper(p.person.name) like '%" + query.toUpperCase() + "%'or  upper(p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            }
            ////System.out.println(sql);
            suggestions = getStaffFacade().findBySQL(sql);
        }
        return suggestions;
    }

    public List<Department> getInstitutionDepatrments() {
        System.out.println("inside = ");
        List<Department> d;
        ////System.out.println("gettin ins dep ");
        if (getCurrentFee().getInstitution() == null) {
            return new ArrayList<Department>();
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution.id=" + getCurrentFee().getInstitution().getId();
            d = getDepartmentFacade().findBySQL(sql);
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
            getItemFeeFacade().create(currentFee);
            UtilityController.addSuccessMessage("Fee Added");
        } else {
            getItemFeeFacade().edit(currentFee);
            UtilityController.addSuccessMessage("Fee Saved");
        }
        currentIx.setTotal(calTot());

        getEjbFacade().edit(currentIx);
        createCharges();
        currentFee = null;
    }

    private double calTot() {
        double tot = 0.0;
        createCharges();
        for (ItemFee i : getCharges()) {
            tot += i.getFee();
        }
        return tot;
    }

    public InvestigationFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(InvestigationFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public ItemFeeController() {
    }

    public Investigation getCurrentIx() {
        return currentIx;
    }

    public void setCurrentIx(Investigation ix) {
        this.currentIx = ix;

    }

    public void edit(ItemFee itemFee) {
        itemFee.setEditer(getSessionController().getLoggedUser());
        itemFee.setEditedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        itemFeeFacade.edit(itemFee);

        itemFee.getItem().setTotal(calTot());
//        ejbFacade.edit((Investigation) itemFee.getItem());
        getEjbFacade().edit((Investigation) itemFee.getItem());
    }

    public void removeFee() {
        if (currentIx == null) {
            UtilityController.addErrorMessage("Please select a investigation");
            return;
        }
        if (getRemovedItemFee() == null) {
            UtilityController.addErrorMessage("Please select one to remove");
            return;
        }

        if (getRemovedItemFee().getId() == null || getRemovedItemFee().getId() == 0) {
            UtilityController.addErrorMessage("Nothing to remove");
            return;
        } else {
            getRemovedItemFee().setRetired(true);
            getRemovedItemFee().setRetirer(getSessionController().getLoggedUser());
            getRemovedItemFee().setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            getItemFeeFacade().edit(getRemovedItemFee()); // Flag as retired, so that will never appearing when calling from database

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

    private InvestigationFacade getFacade() {
        return ejbFacade;
    }

    public void createCharges() {
        String sql = "select c from ItemFee c where c.retired = false and c.item.id = " + currentIx.getId();
        fees = itemFeeFacade.findBySQL(sql);
    }

    public List<ItemFee> getCharges() {
        return fees;
    }

    public void setCharges(List<ItemFee> charges) {
        this.fees = charges;
    }

    public ItemFee getCurrentFee() {
        if (currentFee == null) {
            currentFee = new ItemFee();
        }
        return currentFee;
    }

    public void setCurrentFee(ItemFee currentFee) {
        this.currentFee = currentFee;
    }

    public ItemFeeFacade getItemFeeFacade() {
        return itemFeeFacade;
    }

    public void setItemFeeFacade(ItemFeeFacade itemFeeFacade) {
        this.itemFeeFacade = itemFeeFacade;

    }

    public ItemFee getRemovedItemFee() {
        return removingItemFee;
    }

    public void setRemovedItemFee(ItemFee removedItemFee) {
        this.removingItemFee = removedItemFee;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public List<ItemFee> getFees() {
        return fees;
    }

    public void setFees(List<ItemFee> fees) {
        this.fees = fees;
    }

    public ItemFee getRemovingItemFee() {
        return removingItemFee;
    }

    public void setRemovingItemFee(ItemFee removingItemFee) {
        this.removingItemFee = removingItemFee;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    /**
     *
     */
    @FacesConverter("conItemFee")
    public static class ItemFeeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ItemFeeController controller = (ItemFeeController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "itemFeeController");
            return controller.getItemFeeFacade().find(getKey(value));
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
            if (object instanceof ItemFee) {
                ItemFee o = (ItemFee) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ItemFeeController.class.getName());
            }
        }
    }
}
