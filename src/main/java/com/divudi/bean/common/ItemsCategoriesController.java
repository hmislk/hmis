/*
 * Milk Payment System for Lucky Lanka Milk Processing Company
 *
 * Development and Implementation of Web-based System by ww.divudi.com
 Development and Implementation of Web-based System by ww.divudi.com
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import com.divudi.entity.Category;
import com.divudi.entity.Item;
import com.divudi.entity.ItemsCategories;
import com.divudi.entity.PackageFee;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemsCategoriesFacade;
import com.divudi.facade.PackageFeeFacade;
import com.divudi.facade.PackegeFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
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
 Informatics)
 */
@Named
@SessionScoped
public class ItemsCategoriesController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private ItemsCategoriesFacade ejbFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private PackegeFacade packegeFacade;
    @EJB
    private PackageFeeFacade packageFeeFacade;
    @Inject
    SessionController sessionController;
    private ItemsCategories current;
    private List<ItemsCategories> items = null;
    
    //private List<Packege> packegeList = null;
    Category currentCategory;
    private Item currentItem;
    private PackageFee currentFee;
    private Double total = 0.0;

    public Category getCurrentCategory() {
        return currentCategory;
    }

    public void setCurrentCategory(Category currentCategory) {
        this.currentCategory = currentCategory;
    }

    /**
     *
     */
    public ItemsCategoriesController() {
    }

    private boolean checkItem() {
        String sql = "Select i from ItemsCategories i where i.retired=false and i.category.id= " + getCurrentCategory().getId() + " and i.item.id=" + getCurrentItem().getId();
        ItemsCategories tmp = getFacade().findFirstBySQL(sql);
        if (tmp != null) {
            return true;
        } else {
            return false;
        }
    }

    public void addToPackage() {
        if (getCurrentCategory() == null) {
            UtilityController.addErrorMessage("Please select a package");
            return;
        }
        if (getCurrentItem() == null) {
            UtilityController.addErrorMessage("Please select an item");
            return;
        }

        if (checkItem()) {
            UtilityController.addErrorMessage("Item already added for this dealor");
            return;
        }


        ItemsCategories pi = new ItemsCategories();

        pi.setCategory(getCurrentCategory());
        pi.setItem(getCurrentItem());
        pi.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        pi.setCreater(getSessionController().getLoggedUser());
        getFacade().create(pi);
        UtilityController.addSuccessMessage("Added");
        recreateModel();
    }

    public void removeFromPackage() {
        if (getCurrentCategory() == null) {
            UtilityController.addErrorMessage("Please select a package");
            return;
        }
        if (getCurrent() == null) {
            UtilityController.addErrorMessage("Please select an item");
            return;
        }

        getCurrent().setRetired(true);
        getCurrent().setRetirer(getSessionController().getLoggedUser());
        getCurrent().setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        getFacade().edit(getCurrent());
        UtilityController.addSuccessMessage("Item Removed");
        recreateModel();
    }

    /**
     * Prepare to add new Collecting Centre
     *
     * @return
     */
    public void prepareAdd() {
        current = new ItemsCategories();
    }

    /**
     *
     * @return
     */
    public ItemsCategoriesFacade getEjbFacade() {
        return ejbFacade;
    }

    /**
     *
     * @param ejbFacade
     */
    public void setEjbFacade(ItemsCategoriesFacade ejbFacade) {
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
     *
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
    public ItemsCategories getCurrent() {
        if (current == null) {
            current = new ItemsCategories();
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
    public void setCurrent(ItemsCategories current) {
        this.current = current;
    }

    private ItemsCategoriesFacade getFacade() {
        return ejbFacade;
    }

    /**
     *
     * @return
     */
    public List<ItemsCategories> getItems() {
        String temSql;
        if (getCurrentCategory() != null) {
            temSql = "SELECT i FROM ItemsCategories i where i.retired=false and i.category.id = " + getCurrentCategory().getId();
            items = getFacade().findBySQL(temSql);
        } else {
            items = null;
        }

        if (items == null) {
            items = new ArrayList<ItemsCategories>();
        }
        return items;
    }

    /**
     *
     * Set all ItemsCategoriess to null
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

            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    /**
     *
     * Delete the current ItemsCategories
     *
     */
    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    public void setItems(List<ItemsCategories> items) {
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

    public void setCurrentFee(PackageFee currentFee) {
        this.currentFee = currentFee;
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

    /**
     *
     */
    @FacesConverter(forClass = ItemsCategories.class)
    public static class ItemsCategoriesControllerConverter implements Converter {

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
            ItemsCategoriesController controller = (ItemsCategoriesController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "ItemsCategoriesController");
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
            if (object instanceof ItemsCategories) {
                ItemsCategories o = (ItemsCategories) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ItemsCategoriesController.class.getName());
            }
        }
    }
}
