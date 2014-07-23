/*
 * Milk Payment System for Lucky Lanka Milk Processing Company
 *
 * Development and Implementation of Web-based System by ww.divudi.com
 Development and Implementation of Web-based System by ww.divudi.com
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.data.BillType;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.entity.Bill;
import com.divudi.entity.Institution;
import java.util.TimeZone;
import com.divudi.entity.Item;
import com.divudi.entity.PackageFee;
import com.divudi.facade.ItemsDistributorsFacade;
import com.divudi.entity.pharmacy.ItemsDistributors;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.PackageFeeFacade;

import com.divudi.facade.PackegeFacade;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class ItemsDistributorsController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private ItemsDistributorsFacade ejbFacade;
    @EJB
    private ItemFacade itemFacade;
    @EJB
    private PackegeFacade packegeFacade;
    @EJB
    private PackageFeeFacade packageFeeFacade;
    @Inject
    SessionController sessionController;
    @Inject
    private DealerController dealerController;
    private ItemsDistributors current;
    private List<ItemsDistributors> items = null;
    private List<ItemsDistributors> searchItems = null;
    private SearchKeyword searchKeyword;
    private List<PackageFee> charges;
    //private List<Packege> packegeList = null;
    Institution currentInstituion;
    private Item currentItem;
    private PackageFee currentFee;
    private Double total = 0.0;

    public Institution getCurrentInstituion() {
        return currentInstituion;
    }

    public void setCurrentInstituion(Institution currentInstituion) {
        this.currentInstituion = currentInstituion;
    }

    /**
     *
     */
    public ItemsDistributorsController() {
    }

    private boolean checkItem() {
        String sql = "Select i from ItemsDistributors i where i.retired=false"
                + " and i.institution.id= " + getCurrentInstituion().getId() + " and "
                + " i.item.id=" + getCurrentItem().getId();
        ItemsDistributors tmp = getFacade().findFirstBySQL(sql);
        if (tmp != null) {
            return true;
        } else {
            return false;
        }
    }

    public void addToPackage() {
        if (getCurrentInstituion() == null) {
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

        ItemsDistributors pi = new ItemsDistributors();

        pi.setInstitution(getCurrentInstituion());
        pi.setItem(getCurrentItem());
        pi.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
        pi.setCreater(getSessionController().getLoggedUser());
        if (pi.getId() == null) {
            getFacade().create(pi);
        }
        UtilityController.addSuccessMessage("Added");
        recreateModel();
    }

    public void removeFromPackage() {
        if (getCurrentInstituion() == null) {
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
        current = new ItemsDistributors();
    }

    /**
     *
     * @return
     */
    public ItemsDistributorsFacade getEjbFacade() {
        return ejbFacade;
    }

    /**
     *
     * @param ejbFacade
     */
    public void setEjbFacade(ItemsDistributorsFacade ejbFacade) {
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
    public ItemsDistributors getCurrent() {
        if (current == null) {
            current = new ItemsDistributors();
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
    public void setCurrent(ItemsDistributors current) {
        this.current = current;
    }

    private ItemsDistributorsFacade getFacade() {
        return ejbFacade;
    }

    /**
     *
     * @return
     */
    public List<ItemsDistributors> getItems() {
        String temSql;
        HashMap hm = new HashMap();

        temSql = "SELECT i FROM ItemsDistributors i where i.retired=false and"
                + " i.institution=:ins "
                + " order by i.item.name";

        hm.put("ins", getCurrentInstituion());

        items = getFacade().findBySQL(temSql, hm);

        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void createItemDistributorTable() {
        searchItems = null;
        String sql;
        HashMap tmp = new HashMap();

        sql = " SELECT b FROM ItemsDistributors b where b.item.retired=false "
                + " and b.institution.retired=false and b.retired=false";

        if (getSearchKeyword().getInstitution() != null && !getSearchKeyword().getInstitution().trim().equals("")) {
            sql += " and  (upper(b.institution.name) like :ins )";
            tmp.put("ins", "%" + getSearchKeyword().getInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  (upper(b.item.name) like :itm )";
            tmp.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  (upper(b.item.code) like :cde )";
            tmp.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCategory() != null && !getSearchKeyword().getCategory().trim().equals("")) {
            sql += " and  (upper(b.item.category.name) like :cat )";
            tmp.put("cat", "%" + getSearchKeyword().getCategory().trim().toUpperCase() + "%");
        }

        sql += " order by b.institution.name,b.item.name ";

        searchItems = getFacade().findBySQL(sql, tmp);

    }

    /**
     *
     * Set all ItemsDistributorss to null
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

            UtilityController.addSuccessMessage("savedOldSuccessfully");
        } else {
            current.setCreatedAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setCreater(getSessionController().getLoggedUser());
            getFacade().create(current);
            UtilityController.addSuccessMessage("savedNewSuccessfully");
        }
        recreateModel();
        getItems();
    }

    /**
     *
     * Delete the current ItemsDistributors
     *
     */
    public void delete() {

        if (current != null) {
            current.setRetired(true);
            current.setRetiredAt(Calendar.getInstance(TimeZone.getTimeZone("IST")).getTime());
            current.setRetirer(getSessionController().getLoggedUser());
            getFacade().edit(current);
            UtilityController.addSuccessMessage("DeleteSuccessfull");
        } else {
            UtilityController.addSuccessMessage("NothingToDelete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    public void setItems(List<ItemsDistributors> items) {
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

    public DealerController getDealerController() {
        return dealerController;
    }

    public void setDealerController(DealerController dealerController) {
        this.dealerController = dealerController;
    }

    public SearchKeyword getSearchKeyword() {
        return searchKeyword;
    }

    public void setSearchKeyword(SearchKeyword searchKeyword) {
        this.searchKeyword = searchKeyword;
    }

    public List<ItemsDistributors> getSearchItems() {
        if (searchKeyword == null) {
            searchKeyword = new SearchKeyword();
        }
        return searchItems;
    }

    public void setSearchItems(List<ItemsDistributors> searchItems) {
        this.searchItems = searchItems;
    }

    /**
     *
     */
    @FacesConverter(forClass = ItemsDistributors.class)
    public static class ItemsDistributorsControllerConverter implements Converter {

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
            ItemsDistributorsController controller = (ItemsDistributorsController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "itemsDistributorsController");
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
            if (object instanceof ItemsDistributors) {
                ItemsDistributors o = (ItemsDistributors) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ItemsDistributorsController.class.getName());
            }
        }
    }
}
