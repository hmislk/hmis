/*
 * Milk Payment System for Lucky Lanka Milk Processing Company
 *
 * Development and Implementation of Web-based System by ww.divudi.com
 Development and Implementation of Web-based System by ww.divudi.com
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;
import com.divudi.entity.Item;
import com.divudi.entity.MedicalPackage;
import com.divudi.entity.MedicalPackageFee;
import com.divudi.entity.MedicalPackageItem;
import com.divudi.entity.Service;
import com.divudi.entity.lab.Investigation;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.MedicalPackageFacade;
import com.divudi.facade.MedicalPackageFeeFacade;
import com.divudi.facade.MedicalPackageItemFacade;
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
import javax.persistence.TemporalType;
import com.divudi.bean.common.util.JsfUtil;
/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 Informatics)
 */
@Named
@SessionScoped
public class MedicalPackageItemController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private MedicalPackageItemFacade ejbFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private MedicalPackageFacade packegeFacade;
    @EJB
    private MedicalPackageFeeFacade packageFeeFacade;
    @Inject
    SessionController sessionController;
    private MedicalPackageItem current;
    private List<MedicalPackageItem> items = null;
    private List<MedicalPackageFee> charges;
    //private List<MedicalPackage> packegeList = null;
    MedicalPackage currentMedicalPackage;
    private Item currentItem;
    private MedicalPackageFee currentFee;
    private Double total = 0.0;
    private List<Item> filteredItems;
    List<Item> serviceItems;

    public List<Item> getServiceItems() {
        if (serviceItems == null) {
            String temSql;
            HashMap h = new HashMap();
            temSql = "SELECT i FROM Item i where (type(i)=:t1 or type(i)=:t2 ) and i.retired=false order by i.department.name";
            h.put("t1", Investigation.class);
            h.put("t2", Service.class);
            serviceItems = getItemFacade().findByJpql(temSql, h, TemporalType.TIME);

        }

        return serviceItems;
    }

    public void updateFee() {
        if (getCurrentMedicalPackage() == null) {
            JsfUtil.addErrorMessage("Please select a package");
            return;
        }
        if (getCurrentItem() == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }

        getFacade().edit(getCurrent());
        JsfUtil.addSuccessMessage("savedFeeSuccessfully");
        saveCharge();
        //recreateModel();
        getItems();
    }

    public void saveCharge() {
        for (MedicalPackageFee f : getCharges()) {
            f.setItem(getCurrent().getItem());
            if (f.getId() == null || f.getId() == 0) {
                f.setCreatedAt(new Date());
                f.setCreater(getSessionController().getLoggedUser());
                getMedicalPackageFeeFacade().create(f);
            } else {
                getMedicalPackageFeeFacade().edit(f);
            }
        }
    }

    public MedicalPackage getCurrentMedicalPackage() {

        return currentMedicalPackage;
    }

    public void setCurrentMedicalPackage(MedicalPackage currentMedicalPackage) {
        this.currentMedicalPackage = currentMedicalPackage;
    }

    public void removeFee() {
        try {

            if (getCurrentFee().getId() == null || getCurrentFee().getId() == 0) {
                getCharges().remove(getCurrentFee());
            } else {
                //Hear, it has an id. so previous saved one. If we removed from the list, then it is still there in the
                //databse. Next time when it is called from the database, it will appear. So take it from the database
                //and mark them as retired
                getCurrentFee().getId();
                getCurrentFee().setRetired(true);
                getCurrentFee().setRetirer(getSessionController().getLoggedUser());
                getCurrentFee().setRetiredAt(new Date());
                getMedicalPackageFeeFacade().edit(getCurrentFee()); // Flag as retired, so that will never appearing when calling from database
                getCharges().remove(getCurrentFee());
            }
            calculateTotalFee();
            setCurrent(getCurrent());
        } catch (Exception e) {
            //////// // System.out.println(e.getMessage());
        }
    }

    /**
     *
     */
    public MedicalPackageItemController() {
    }

    private boolean checkPackageItem() {
        for (MedicalPackageItem i : getItems()) {
            //////// // System.out.println("a : " + i.getItem().getId());
            //////// // System.out.println("b : " + getCurrentItem().getId());
            if (i.getItem().getId() == getCurrentItem().getId()) {
                //////// // System.out.println("c : " + i.getItem().getId());
                //////// // System.out.println("d : " + getCurrentItem().getId());
                return true;
            }
        }

        return false;
    }

    public void addToPackage() {
        if (getCurrentMedicalPackage() == null) {
            JsfUtil.addErrorMessage("Please select a package");
            return;
        }
        if (getCurrentItem() == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }

        if (checkPackageItem()) {
            JsfUtil.addErrorMessage("Please item already exist in this package");
            return;
        }

        MedicalPackageItem pi = new MedicalPackageItem();

        pi.setPackege(getCurrentMedicalPackage());
        pi.setItem(getCurrentItem());
        pi.setCreatedAt(new Date());
        pi.setCreater(sessionController.loggedUser);
        getFacade().create(pi);
        JsfUtil.addSuccessMessage("Added");
        recreateModel();
    }

    public void removeFromPackage() {
        if (getCurrentMedicalPackage() == null) {
            JsfUtil.addErrorMessage("Please select a package");
            return;
        }
        if (getCurrent() == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }

        getCurrent().setRetired(true);
        getCurrent().setRetirer(getSessionController().getLoggedUser());
        getCurrent().setRetiredAt(new Date());
        getFacade().edit(getCurrent());
        JsfUtil.addSuccessMessage("Item Removed");
        recreateModel();
    }

    /**
     * Prepare to add new Collecting Centre
     *
     * @return
     */
    public void prepareAdd() {
        current = new MedicalPackageItem();
    }

    /**
     *
     * @return
     */
    public MedicalPackageItemFacade getEjbFacade() {
        return ejbFacade;
    }

    /**
     *
     * @param ejbFacade
     */
    public void setEjbFacade(MedicalPackageItemFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    /**
     *
     * @return
     */
    public SessionController getSessionController() {
        return sessionController;
    }

    /**
     *
     * @param sessionController
     */
    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    /**
     * Return the current item
     *
     * @return
     */
    public MedicalPackageItem getCurrent() {
        if (current == null) {
            current = new MedicalPackageItem();
            total = 0.0;
            currentFee = null;
        }
        return current;
    }

    /**
     * Set the current item
     *
     * @param current
     */
    public void setCurrent(MedicalPackageItem current) {
        this.current = current;
    }

    private MedicalPackageItemFacade getFacade() {
        return ejbFacade;
    }

    /**
     *
     * @return
     */
    public List<MedicalPackageItem> getItems() {
        String temSql;
        if (getCurrentMedicalPackage() != null) {
            temSql = "SELECT i FROM MedicalPackageItem i where i.retired=false and i.packege.id = " + getCurrentMedicalPackage().getId();
            items = getFacade().findByJpql(temSql);
        } else {
            items = null;
        }

        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    /**
     *
     * Set all MedicalPackageItems to null
     *
     */
    private void recreateModel() {
        total = 0.0;
        items = null;
        currentItem = null;
        currentFee = null;
    }

    /**
     *
     */
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

    /**
     *
     * Delete the current MedicalPackageItem
     *
     */
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

    public void setItems(List<MedicalPackageItem> items) {
        this.items = items;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

//    public List<MedicalPackage> getMedicalPackageList() {
//        if(packegeList==null){
//            packegeList=new ArrayList<MedicalPackage>();
//
//        }
//        return packegeList;
//    }
//
//    public void setMedicalPackageList(List<MedicalPackage> packegeList) {
//        this.packegeList = packegeList;
//    }
//  public void addToPackageList(){
//      getMedicalPackageList().add(getCurrent().getMedicalPackage());
//  }
    public Item getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(Item currentItem) {
        this.currentItem = currentItem;
    }

    public MedicalPackageFacade getMedicalPackageFacade() {
        return packegeFacade;
    }

    public void setMedicalPackageFacade(MedicalPackageFacade packegeFacade) {
        this.packegeFacade = packegeFacade;
    }

    public MedicalPackageFee getCurrentFee() {
        if (currentFee == null) {
            currentFee = new MedicalPackageFee();
        }
        return currentFee;
    }

    public void addToFeeList() {
//        getCharges().add(getCurrentFee());

        if (getCurrentFee() != null) {
            getCurrentFee().setPackege(getCurrentMedicalPackage());
            getCurrentFee().setItem(getCurrent().getItem());
        }
        if (getCurrentFee().getId() == null || getCurrentFee().getId() == 0) {
            getCurrentFee().setCreatedAt(new Date());
            getCurrentFee().setCreater(getSessionController().getLoggedUser());
            getMedicalPackageFeeFacade().create(getCurrentFee());
        } else {
            getMedicalPackageFeeFacade().edit(getCurrentFee());
        }
        calculateTotalFee();
        currentFee = new MedicalPackageFee();

    }

    public void calculateTotalFee() {
        total = 0.0;
        for (MedicalPackageFee f : getCharges()) {
            total += f.getFee();
        }
        //getCurrent().setTotal(total);
        getCurrent().getItem().setTotal(total);
    }

    public void setCurrentFee(MedicalPackageFee currentFee) {
        this.currentFee = currentFee;
    }

    public List<MedicalPackageFee> getCharges() {
        if (getCurrent() != null && getCurrent().getId() != null) {
            String temp = "SELECT  p from MedicalPackageFee p where p.retired=false and p.item.id=" + getCurrent().getItem().getId() + "and p.packege.id=" + getCurrentMedicalPackage().getId();
            charges = getMedicalPackageFeeFacade().findByJpql(temp);
        }
        if (charges == null) {
            charges = new ArrayList<>();
        }
        return charges;
    }

    public void setCharges(List<MedicalPackageFee> charges) {
        this.charges = charges;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public MedicalPackageFeeFacade getMedicalPackageFeeFacade() {
        return packageFeeFacade;
    }

    public void setMedicalPackageFeeFacade(MedicalPackageFeeFacade packageFeeFacade) {
        this.packageFeeFacade = packageFeeFacade;
    }

    public List<Item> getFilteredItems() {
        return filteredItems;
    }

    public void setFilteredItems(List<Item> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     *
     */
    @FacesConverter(forClass = MedicalPackageItem.class)
    public static class MedicalPackageItemControllerConverter implements Converter {

        /**
         *
         * @param facesContext
         * @param component
         * @param value
         * @return
         */
        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            MedicalPackageItemController controller = (MedicalPackageItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "medicalPackageItemController");
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

        /**
         *
         * @param facesContext
         * @param component
         * @param object
         * @return
         */
        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof MedicalPackageItem) {
                MedicalPackageItem o = (MedicalPackageItem) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + MedicalPackageItemController.class.getName());
            }
        }
    }
}
