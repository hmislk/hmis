/*
 * Milk Payment System for Lucky Lanka Milk Processing Company
 *
 * Development and Implementation of Web-based System by ww.divudi.com
 Development and Implementation of Web-based System by ww.divudi.com
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.CommonController;
import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.dataStructure.SearchKeyword;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.PackageFee;
import com.divudi.entity.pharmacy.ItemsDistributors;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.ItemsDistributorsFacade;
import com.divudi.facade.PackageFeeFacade;
import com.divudi.facade.PackegeFacade;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
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
    CommonController commonController;
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
        ItemsDistributors tmp = getFacade().findFirstByJpql(sql);
        if (tmp != null) {
            return true;
        } else {
            return false;
        }
    }

    public Institution getDistributor(Item i) {
        String sql = "Select i from ItemsDistributors i where i.retired=false"
                + " and i.item=:item"
                + " order by i.id desc";
        Map m = new HashMap();
        m.put("item", i);
        ItemsDistributors tmp = getFacade().findFirstByJpql(sql, m);
        if (tmp != null) {
            return tmp.getInstitution();
        } else {
            return null;
        }
    }

    public void addItemToDistributor() {
        if (getCurrentInstituion() == null) {
            JsfUtil.addErrorMessage("Please select a package");
            return;
        }
        if (getCurrentItem() == null) {
            JsfUtil.addErrorMessage("Please select an item");
            return;
        }

        if (checkItem()) {
            JsfUtil.addErrorMessage("Item already added for this dealor");
            return;
        }

        ItemsDistributors pi = new ItemsDistributors();

        pi.setInstitution(getCurrentInstituion());
        pi.setItem(getCurrentItem());
        pi.setCreatedAt(new Date());
        pi.setCreater(getSessionController().getLoggedUser());
        if (pi.getId() == null) {
            getFacade().create(pi);
        }
        JsfUtil.addSuccessMessage("Added");
        recreateModel();
        listItemForDistributer();
    }

    public void removeFromPackage() {
        if (getCurrentInstituion() == null) {
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
        listItemForDistributer();
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
        return items;
    }
    
    public void listItemForDistributer(){
        String temSql;
        HashMap hm = new HashMap();

        temSql = "SELECT i FROM ItemsDistributors i"
                + " where i.retired=false "
                + " and i.item.retired=false"
                + " and i.institution=:ins "
                + " order by i.item.name";

        hm.put("ins", getCurrentInstituion());

        items = getFacade().findByJpql(temSql, hm);

        if (items == null) {
            items = new ArrayList<>();
        }
    }

    public void createItemDistributorTable() {
        Date startTime = new Date();
        Date fromDate = null;
        Date toDate = null;

        searchItems = null;
        String sql;
        HashMap tmp = new HashMap();

        sql = " SELECT b FROM ItemsDistributors b where b.item.retired=false "
                + " and b.institution.retired=false and b.retired=false";

        if (getSearchKeyword().getInstitution() != null && !getSearchKeyword().getInstitution().trim().equals("")) {
            sql += " and  ((b.institution.name) like :ins )";
            tmp.put("ins", "%" + getSearchKeyword().getInstitution().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getItemName() != null && !getSearchKeyword().getItemName().trim().equals("")) {
            sql += " and  ((b.item.name) like :itm )";
            tmp.put("itm", "%" + getSearchKeyword().getItemName().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCode() != null && !getSearchKeyword().getCode().trim().equals("")) {
            sql += " and  ((b.item.code) like :cde )";
            tmp.put("cde", "%" + getSearchKeyword().getCode().trim().toUpperCase() + "%");
        }

        if (getSearchKeyword().getCategory() != null && !getSearchKeyword().getCategory().trim().equals("")) {
            sql += " and  ((b.item.category.name) like :cat )";
            tmp.put("cat", "%" + getSearchKeyword().getCategory().trim().toUpperCase() + "%");
        }

        sql += " order by b.institution.name,b.item.name ";

        searchItems = getFacade().findByJpql(sql, tmp);
        
        commonController.printReportDetails(fromDate, toDate, startTime, "Pharmacy/Reports/Administration/Check enterd data/Item distributor(/faces/pharmacy/pharmacy_item_by_distributor.xhtml)");

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
     * Delete the current ItemsDistributors
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

    public CommonController getCommonController() {
        return commonController;
    }

    public void setCommonController(CommonController commonController) {
        this.commonController = commonController;
    }
    
}
