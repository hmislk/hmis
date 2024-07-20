/*
 * Milk Payment System for Lucky Lanka Milk Processing Company
 *
 * Development and Implementation of Web-based System by ww.divudi.com
 Development and Implementation of Web-based System by ww.divudi.com
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.entity.Item;
import com.divudi.entity.PackageFee;
import com.divudi.entity.PackageItem;
import com.divudi.entity.Packege;
import com.divudi.entity.Service;
import com.divudi.entity.lab.Investigation;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PackageFeeFacade;
import com.divudi.facade.PackageItemFacade;
import com.divudi.facade.PackegeFacade;
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

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Informatics)
 */
@Named
@SessionScoped
public class PackageItemController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private PackageItemFacade ejbFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private PackegeFacade packegeFacade;
    @EJB
    private PackageFeeFacade packageFeeFacade;
    @Inject
    SessionController sessionController;
    private PackageItem current;
    private List<PackageItem> items = null;
    private List<PackageFee> charges;
    //private List<Packege> packegeList = null;
    Packege currentPackege;
    private Item currentItem;
    private PackageFee currentFee;
    private Double total = 0.0;
    private List<Item> filteredItems;
    List<Item> serviceItems;

    private boolean canRemovePackageItemfromPackage;

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
        if (getCurrentPackege() == null) {
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
        for (PackageFee f : getCharges()) {
            f.setItem(getCurrent().getItem());
            if (f.getId() == null || f.getId() == 0) {
                f.setCreatedAt(new Date());
                f.setCreater(getSessionController().getLoggedUser());
                getPackageFeeFacade().create(f);
            } else {
                getPackageFeeFacade().edit(f);
            }
        }
    }

    public Packege getCurrentPackege() {

        return currentPackege;
    }

    public void setCurrentPackege(Packege currentPackege) {
        this.currentPackege = currentPackege;
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
                getPackageFeeFacade().edit(getCurrentFee()); // Flag as retired, so that will never appearing when calling from database
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
    public PackageItemController() {
    }

    public void addToPackage() {
        if (getCurrentPackege() == null) {
            JsfUtil.addErrorMessage("Please select a package");
            return;
        }
        if (getCurrentItem() == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }
        PackageItem pi = new PackageItem();

        pi.setPackege(getCurrentPackege());
        pi.setItem(getCurrentItem());
        pi.setCreatedAt(new Date());
        pi.setCreater(sessionController.loggedUser);
        if(pi.getId() == null){
            getFacade().create(pi);
        }
        
        pi.getItem().setCanRemoveItemfromPackage(canRemovePackageItemfromPackage);
        
        if(pi.getId() != null){
            itemFacade.edit(pi.getItem());
        }
        
        JsfUtil.addSuccessMessage("Added");
        recreateModel();
    }
    
    public void EditPackageItem() {
        if (getCurrentPackege() == null) {
            JsfUtil.addErrorMessage("Please select a package");
            return;
        }
        if (getCurrent() == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }else{
            getCurrent().getItem().setCanRemoveItemfromPackage(canRemovePackageItemfromPackage);
            itemFacade.edit(getCurrent().getItem());
        }

        recreateModel();
        getItems();
        
        JsfUtil.addSuccessMessage("Updated");
        
    }

    public void removeFromPackage() {
        if (getCurrentPackege() == null) {
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
        getItems();
    }

    /**
     * Prepare to add new Collecting Centre
     *
     * @return
     */
    public void prepareAdd() {
        current = new PackageItem();
    }

    /**
     *
     * @return
     */
    public PackageItemFacade getEjbFacade() {
        return ejbFacade;
    }

    /**
     *
     * @param ejbFacade
     */
    public void setEjbFacade(PackageItemFacade ejbFacade) {
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
    public PackageItem getCurrent() {
        if (current == null) {
            current = new PackageItem();
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
    public void setCurrent(PackageItem current) {
        this.current = current;
    }

    private PackageItemFacade getFacade() {
        return ejbFacade;
    }
    
    public void clearValus(){
        canRemovePackageItemfromPackage = false;
        
    }

    /**
     *
     * @return
     */
    public List<PackageItem> getItems() {
        String temSql;
        if (getCurrentPackege() != null) {
            temSql = "SELECT i FROM PackageItem i where i.retired=false and i.packege.id = " + getCurrentPackege().getId();
            items = getFacade().findByJpql(temSql);
        } else {
            items = null;
        }

        if (items == null) {
            items = new ArrayList<PackageItem>();
        }
        return items;
    }

    /**
     *
     * Set all PackageItems to null
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
     * Delete the current PackageItem
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

    public void setItems(List<PackageItem> items) {
        this.items = items;
    }

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

//    public List<Packege> getPackegeList() {
//        if(packegeList==null){
//            packegeList=new ArrayList<Packege>();
//
//        }
//        return packegeList;
//    }
//
//    public void setPackegeList(List<Packege> packegeList) {
//        this.packegeList = packegeList;
//    }
//  public void addToPackageList(){
//      getPackegeList().add(getCurrent().getPackege());
//  }
    public Item getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(Item currentItem) {
        this.currentItem = currentItem;
    }

    public PackegeFacade getPackegeFacade() {
        return packegeFacade;
    }

    public void setPackegeFacade(PackegeFacade packegeFacade) {
        this.packegeFacade = packegeFacade;
    }

    public PackageFee getCurrentFee() {
        if (currentFee == null) {
            currentFee = new PackageFee();
        }
        return currentFee;
    }

    public void addToFeeList() {
//        getCharges().add(getCurrentFee());

        if (getCurrentFee() != null) {
            getCurrentFee().setPackege(getCurrentPackege());
            getCurrentFee().setItem(getCurrent().getItem());
        }
        if (getCurrentFee().getId() == null || getCurrentFee().getId() == 0) {
            getCurrentFee().setCreatedAt(new Date());
            getCurrentFee().setCreater(getSessionController().getLoggedUser());
            getPackageFeeFacade().create(getCurrentFee());
        } else {
            getPackageFeeFacade().edit(getCurrentFee());
        }
        calculateTotalFee();
        currentFee = new PackageFee();

    }

    public void calculateTotalFee() {
        total = 0.0;
        for (PackageFee f : getCharges()) {
            total += f.getFee();
        }
        //getCurrent().setTotal(total);
        getCurrent().getItem().setTotal(total);
    }

    public void setCurrentFee(PackageFee currentFee) {
        this.currentFee = currentFee;
    }

    public List<PackageFee> getCharges() {
        if (getCurrent() != null && getCurrent().getId() != null) {
            String temp = "SELECT  p from PackageFee p where p.retired=false and p.item.id=" + getCurrent().getItem().getId() + "and p.packege.id=" + getCurrentPackege().getId();
            charges = getPackageFeeFacade().findByJpql(temp);
        }
        if (charges == null) {
            charges = new ArrayList<PackageFee>();
        }
        return charges;
    }

    public void setCharges(List<PackageFee> charges) {
        this.charges = charges;
    }

    public Double getTotal() {
        return total;
    }

    public void setTotal(Double total) {
        this.total = total;
    }

    public PackageFeeFacade getPackageFeeFacade() {
        return packageFeeFacade;
    }

    public void setPackageFeeFacade(PackageFeeFacade packageFeeFacade) {
        this.packageFeeFacade = packageFeeFacade;
    }

    public List<Item> getFilteredItems() {
        return filteredItems;
    }

    public void setFilteredItems(List<Item> filteredItems) {
        this.filteredItems = filteredItems;
    }

    public boolean isCanRemovePackageItemfromPackage() {
        return canRemovePackageItemfromPackage;
    }

    public void setCanRemovePackageItemfromPackage(boolean canRemovePackageItemfromPackage) {
        this.canRemovePackageItemfromPackage = canRemovePackageItemfromPackage;
    }

    /**
     *
     */
    @FacesConverter(forClass = PackageItem.class)
    public static class PackageItemControllerConverter implements Converter {

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
            PackageItemController controller = (PackageItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "packageItemController");
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
            if (object instanceof PackageItem) {
                PackageItem o = (PackageItem) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + PackageItemController.class.getName());
            }
        }
    }
}
