/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Department;
import com.divudi.entity.Fee;
import com.divudi.entity.Item;
import com.divudi.entity.MedicalPackage;
import com.divudi.entity.MedicalPackageFee;
import com.divudi.entity.MedicalPackageItem;
import com.divudi.entity.Staff;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.InvestigationFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.MedicalPackageFeeFacade;
import com.divudi.facade.MedicalPackageItemFacade;
import com.divudi.facade.PackegeFacade;
import com.divudi.facade.StaffFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 Informatics)
 */
@Named
@SessionScoped
public class MedicalPackageItemFeeController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private ItemFacade ejbFacade;
    @EJB
    private MedicalPackageFeeFacade packageFeeFacade;
    @EJB
    private MedicalPackageItemFacade packageItemFacade;
    @EJB
    private PackegeFacade packegeFacade;
    @EJB
    private InvestigationFacade investigationFacade;
    private MedicalPackage currentPackege;
    private List<MedicalPackageFee> fees;
    private Item currentIx;
    private MedicalPackageFee currentFee;
    private MedicalPackageFee removingMedicalPackageFee;
    private double total;
    List<MedicalPackageItem> items;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    private StaffFacade staffFacade;
    @Inject
    private BillBeanController billBean;

    public List<Staff> completeStaff(String query) {
        List<Staff> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (getCurrentFee().getSpeciality() == null) {
                sql = "select p from Staff p where p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            } else {
                sql = "select p from Staff p where p.speciality.id=" + getCurrentFee().getSpeciality().getId() + " and p.retired=false and ((p.person.name) like '%" + query.toUpperCase() + "%'or  (p.code) like '%" + query.toUpperCase() + "%' ) order by p.person.name";
            }
            //////// // System.out.println(sql);
            suggestions = getStaffFacade().findByJpql(sql);
        }
        return suggestions;
    }

    public List<Department> getInstitutionDepatrments() {
        List<Department> d;
        //////// // System.out.println("gettin ins dep ");
        //if (getCurrentFee()==null && getCurrentFee().getInstitution() == null && getCurrentFee().getInstitution().getId() == null) {
        if (getCurrentFee().getInstitution() == null) {
            return new ArrayList<>();
        } else {
            String sql = "Select d From Department d where d.retired=false and d.institution.id=" + getCurrentFee().getInstitution().getId();
            d = getDepartmentFacade().findByJpql(sql);
        }

        return d;
    }

    public List<MedicalPackageItem> getItems() {
        String temSql;
        if (getCurrentPackege() != null) {
            temSql = "SELECT i FROM MedicalPackageItem i where i.retired=false and i.packege.id = " + getCurrentPackege().getId();
            items = getMedicalPackageItemFacade().findByJpql(temSql);
        } else {
            items = null;
        }

        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void saveCharge() {

        if (currentFee == null) {
            JsfUtil.addErrorMessage("Please select a charge");
            return;
        }
        if (currentIx == null) {
            JsfUtil.addErrorMessage("Please select a Investigation");
            return;
        }

        currentFee.setPackege(currentPackege);
        currentFee.setItem(currentIx);
        if (currentFee.getId() == null || currentFee.getId() == 0) {
            currentFee.setCreatedAt(new Date());
            currentFee.setCreater(getSessionController().getLoggedUser());

            getMedicalPackageFeeFacade().create(currentFee);
            JsfUtil.addSuccessMessage("Fee Added");
        } else {
            getMedicalPackageFeeFacade().edit(currentFee);
            JsfUtil.addSuccessMessage("Fee Saved");
        }

        setPakageTotal();

        setCharges(null);
        currentFee = null;

    }

    private void setPakageTotal() {
        total = 0.0;
        List<Item> itemList = getBillBean().itemFromMedicalPackage(currentPackege);

        for (Item i : itemList) {
            List<Fee> tmp = getBillBean().getMedicalPackageFee(currentPackege, i);
            for (Fee ff : tmp) {
                total += ff.getFee();
            }
        }

      //  //System.err.println("total : " + total);
        currentPackege.setTotal(total);
        getFacade().edit(currentPackege);
    }

    public ItemFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(ItemFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public MedicalPackageItemFeeController() {
    }

    public Item getCurrentIx() {
        return currentIx;
    }

    public void setCurrentIx(Item ix) {
        this.currentIx = ix;

    }

    public void removeFee() {
        if (getRemovedMedicalPackageFee().getId() == null || getRemovedMedicalPackageFee().getId() == 0) {
            JsfUtil.addErrorMessage("Nothing to remove");
            return;
        } else {
            getRemovedMedicalPackageFee().setRetired(true);
            getRemovedMedicalPackageFee().setRetirer(getSessionController().getLoggedUser());
            getRemovedMedicalPackageFee().setRetiredAt(new Date());
            getMedicalPackageFeeFacade().edit(getRemovedMedicalPackageFee()); // Flag as retired, so that will never appearing when calling from database

            setCharges(null);
            getCharges();
            getCurrentIx().setTotal(getTotal());

        }
    }

    public void delete() {

        if (currentIx != null) {
            currentIx.setRetired(true);
            currentIx.setRetiredAt(new Date());
            currentIx.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(currentIx);
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
        }

        currentIx = null;
        setCharges(null);
    }

    private ItemFacade getFacade() {
        return ejbFacade;
    }

    public List<MedicalPackageFee> getCharges() {

        if (currentIx != null && currentIx.getId() != null) {
            setCharges(getMedicalPackageFeeFacade().findByJpql("select c from MedicalPackageFee c where c.retired = false and c.packege.id=" + currentPackege.getId() + " and c.item.id = " + currentIx.getId()));

        } else {
            setCharges(new ArrayList<MedicalPackageFee>());

        }
        if (fees == null) {
            fees = new ArrayList<>();

        }
        return fees;
    }

    public void setCharges(List<MedicalPackageFee> charges) {
        this.fees = charges;
    }

    public MedicalPackageFee getCurrentFee() {
        if (currentFee == null) {
            currentFee = new MedicalPackageFee();
        }
        return currentFee;
    }

    public void setCurrentFee(MedicalPackageFee currentFee) {
        this.currentFee = currentFee;
    }

    public double getTotal() {
        double d = 0.0;
        for (MedicalPackageFee f : getCharges()) {
            d = d + f.getFee();
        }
        total = d;
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public MedicalPackageFeeFacade getMedicalPackageFeeFacade() {
        return packageFeeFacade;
    }

    public void setMedicalPackageFeeFacade(MedicalPackageFeeFacade packageFeeFacade) {
        this.packageFeeFacade = packageFeeFacade;

    }

    public MedicalPackageFee getRemovedMedicalPackageFee() {
        return removingMedicalPackageFee;
    }

    public void setRemovedMedicalPackageFee(MedicalPackageFee removedMedicalPackageFee) {
        this.removingMedicalPackageFee = removedMedicalPackageFee;
    }

    public MedicalPackage getCurrentPackege() {
        return currentPackege;
    }

    public void setCurrentPackege(MedicalPackage currentPackege) {
        this.currentPackege = currentPackege;
        getCharges();
        getTotal();
    }

    public List<MedicalPackageFee> getFees() {
        return fees;
    }

    public void setFees(List<MedicalPackageFee> fees) {
        this.fees = fees;
    }

    public MedicalPackageFee getRemovingMedicalPackageFee() {
        return removingMedicalPackageFee;
    }

    public void setRemovingMedicalPackageFee(MedicalPackageFee removingMedicalPackageFee) {
        this.removingMedicalPackageFee = removingMedicalPackageFee;
    }

    public MedicalPackageItemFacade getMedicalPackageItemFacade() {
        return packageItemFacade;
    }

    public void setMedicalPackageItemFacade(MedicalPackageItemFacade packageItemFacade) {
        this.packageItemFacade = packageItemFacade;
    }

    public PackegeFacade getPackegeFacade() {
        return packegeFacade;
    }

    public void setPackegeFacade(PackegeFacade packegeFacade) {
        this.packegeFacade = packegeFacade;
    }

    public InvestigationFacade getInvestigationFacade() {
        return investigationFacade;
    }

    public void setInvestigationFacade(InvestigationFacade investigationFacade) {
        this.investigationFacade = investigationFacade;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    public StaffFacade getStaffFacade() {
        return staffFacade;
    }

    public void setStaffFacade(StaffFacade staffFacade) {
        this.staffFacade = staffFacade;
    }

    public BillBeanController getBillBean() {
        return billBean;
    }

    public void setBillBean(BillBeanController billBean) {
        this.billBean = billBean;
    }

    public MedicalPackageFeeFacade getPackageFeeFacade() {
        return packageFeeFacade;
    }

    public void setPackageFeeFacade(MedicalPackageFeeFacade packageFeeFacade) {
        this.packageFeeFacade = packageFeeFacade;
    }

    public MedicalPackageItemFacade getPackageItemFacade() {
        return packageItemFacade;
    }

    public void setPackageItemFacade(MedicalPackageItemFacade packageItemFacade) {
        this.packageItemFacade = packageItemFacade;
    }

    /**
     *
     *
     *
     */
    @FacesConverter("conMedicalPackageFee")
    public static class MedicalPackageFeeControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            MedicalPackageItemController controller = (MedicalPackageItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "medicalPackageItemFeeController");
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
            if (object instanceof MedicalPackageFee) {
                MedicalPackageFee o = (MedicalPackageFee) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + MedicalPackageItemFeeController.class.getName());
            }
        }
    }
}
