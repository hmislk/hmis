/*
 * Milk Payment System for Lucky Lanka Milk Processing Company
 *
 * Development and Implementation of Web-based System by ww.divudi.com
 Development and Implementation of Web-based System by ww.divudi.com
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;

import com.divudi.entity.BillExpense;
import com.divudi.data.DepartmentType;
import com.divudi.data.FeeType;
import com.divudi.entity.Category;
import com.divudi.entity.Institution;
import com.divudi.facade.ItemFacade;
import com.divudi.entity.Item;

import com.divudi.entity.Service;
import com.divudi.entity.lab.Investigation;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Ampp;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.entity.pharmacy.Vmpp;
import java.util.TimeZone;

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
import com.divudi.entity.Packege;
import com.divudi.entity.ServiceCategory;
import com.divudi.entity.ServiceSubCategory;
import com.divudi.entity.inward.InwardService;
import com.divudi.entity.lab.InvestigationCategory;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class ItemController implements Serializable {

    private static final long serialVersionUID = 1L;
    @EJB
    private ItemFacade ejbFacade;
    @Inject
    SessionController sessionController;
    private Item current;
    private List<Item> items = null;
    private Institution instituion;

    public List<Item> completeDealorItem(String query) {
        List<Item> suggestions;
        String sql;
        HashMap hm = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c.item from ItemsDistributors c where c.retired=false "
                    + " and c.institution=:ins and (upper(c.item.name) like :q or "
                    + " upper(c.item.barcode) like :q or upper(c.item.code) like :q )order by c.item.name";
            hm.put("ins", getInstituion());
            hm.put("q", "%" + query + "%");
            //System.out.println(sql);
            suggestions = getFacade().findBySQL(sql, hm, 20);
        }
        return suggestions;

    }

    public List<Item> getDealorItem() {
        List<Item> suggestions;
        String sql;
        HashMap hm = new HashMap();

        sql = "select c.item from ItemsDistributors c where c.retired=false "
                + " and c.institution=:ins "
                + " order by c.item.name";
        hm.put("ins", getInstituion());

        //System.out.println(sql);
        suggestions = getFacade().findBySQL(sql, hm);

        return suggestions;

    }

    public List<Item> completeItem(String query) {
        List<Item> suggestions;
        String sql;
        HashMap hm = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c "
                    + " where c.retired=false"
                    + "  and (upper(c.name) like :q"
                    + "  or upper(c.barcode) like :q"
                    + "  or upper(c.code) like :q )"
                    + " order by c.name";
            hm.put("q", "%" + query.toUpperCase() + "%");
//System.out.println(sql);
            suggestions = getFacade().findBySQL(sql, hm, 20);
        }
        return suggestions;

    }

    List<Item> itemList;

    public List<Item> completePharmacyItem(String query) {

        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            itemList = new ArrayList<>();
        } else {

            sql = "select c from Item c where c.retired=false and "
                    + " ( c.departmentType is null or c.departmentType!=:dep ) and"
                    + "(type(c)= :amp or type(c)= :ampp or type(c)= :vmp or"
                    + " type(c)= :vmpp) and (upper(c.name) "
                    + "like :q or upper(c.code) "
                    + "like :q or upper(c.barcode) like :q  ) order by c.name";
            //System.out.println(sql);
            tmpMap.put("amp", Amp.class);
            tmpMap.put("ampp", Ampp.class);
            tmpMap.put("vmp", Vmp.class);
            tmpMap.put("vmpp", Vmpp.class);
            tmpMap.put("dep", DepartmentType.Store);
            tmpMap.put("q", "%" + query.toUpperCase() + "%");
            itemList = getFacade().findBySQL(sql, tmpMap, TemporalType.TIMESTAMP, 10);
        }
        return itemList;

    }

    public List<Item> completeAmps(String query) {
        List<Item> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c where c.retired=false and type(c)= :amp and "
                    + " ( c.departmentType is null or c.departmentType!=:dep ) "
                    + "(upper(c.name) like :q  or "
                    + " upper(c.code) like :q or "
                    + " upper(c.barcode) like :q ) order by c.name";
            //System.out.println(sql);
            tmpMap.put("amp", Amp.class);
            tmpMap.put("dep", DepartmentType.Store);
            tmpMap.put("q", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findBySQL(sql, tmpMap, TemporalType.TIMESTAMP, 10);
        }
        return suggestions;

    }

    List<Item> suggestions;

    public List<Item> completeAmpItem(String query) {

        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {

            sql = "select c from Item c where c.retired=false "
                    + " and (type(c)= :amp) and "
                    + " ( c.departmentType is null or c.departmentType!=:dep ) "
                    + " and (upper(c.name) like :str or upper(c.code) like :str or"
                    + " upper(c.barcode) like :str ) order by c.name";
            //System.out.println(sql);
            tmpMap.put("dep", DepartmentType.Store);
            tmpMap.put("amp", Amp.class);
            tmpMap.put("str", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findBySQL(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;

    }
    
     public List<Item> completeAmpItemAll(String query) {

        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {

            sql = "select c from Item c where "
                    + " (type(c)= :amp) and "
                    + " ( c.departmentType is null or c.departmentType!=:dep ) "
                    + " and (upper(c.name) like :str or upper(c.code) like :str or"
                    + " upper(c.barcode) like :str ) order by c.name";
            //System.out.println(sql);
            tmpMap.put("dep", DepartmentType.Store);
            tmpMap.put("amp", Amp.class);
            tmpMap.put("str", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findBySQL(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;

    }


    public List<Item> completeStoreItem(String query) {
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {

            sql = "select c from Item c "
                    + "where c.retired=false and "
                    + "(type(c)= :amp) "
                    + "and c.departmentType=:dep "
                    + "and (upper(c.name) like :str or "
                    + "upper(c.code) like :str or "
                    + "upper(c.barcode) like :str) "
                    + "order by c.name";
            //System.out.println(sql);
            tmpMap.put("amp", Amp.class);
            tmpMap.put("dep", DepartmentType.Store);
            tmpMap.put("str", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findBySQL(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;

    }

    public List<Item> completeExpenseItem(String query) {
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c "
                    + "where c.retired=false and "
                    + "(type(c)= :amp) "
                    + "and (upper(c.name) like :str or "
                    + "upper(c.code) like :str or "
                    + "upper(c.barcode) like :str) "
                    + "order by c.name";
            //System.out.println(sql);
            tmpMap.put("amp", BillExpense.class);
            tmpMap.put("str", "%" + query.toUpperCase() + "%");
            suggestions = getFacade().findBySQL(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;
    }

    public List<Item> fetchStoreItem() {
        List<Item> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();

        sql = "select c from Item c"
                + "  where c.retired=false and "
                + " (type(c)= :amp) "
                + " and c.departmentType=:dep "
                + " order by c.name";
        //System.out.println(sql);
        tmpMap.put("amp", Amp.class);
        tmpMap.put("dep", DepartmentType.Store);

        suggestions = getFacade().findBySQL(sql, tmpMap, TemporalType.TIMESTAMP);

        return suggestions;

    }

    public List<Item> completeAmpAndAmppItem(String query) {
        List<Item> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            if (query.length() > 4) {
                sql = "select c from Item c where c.retired=false and (type(c)= :amp or type(c)=:ampp ) and (upper(c.name) like '%" + query.toUpperCase() + "%' or upper(c.code) like '%" + query.toUpperCase() + "%' or upper(c.barcode) like '%" + query.toUpperCase() + "%') order by c.name";
            } else {
                sql = "select c from Item c where c.retired=false and (type(c)= :amp or type(c)=:ampp ) and (upper(c.name) like '%" + query.toUpperCase() + "%' or upper(c.code) like '%" + query.toUpperCase() + "%') order by c.name";
            }

//System.out.println(sql);
            tmpMap.put("amp", Amp.class);
            tmpMap.put("ampp", Ampp.class);
            suggestions = getFacade().findBySQL(sql, tmpMap, TemporalType.TIMESTAMP, 30);
        }
        return suggestions;

    }

    public List<Item> completePackage(String query) {
        List<Item> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c where c.retired=false and type(c)=Packege and upper(c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //System.out.println(sql);
            suggestions = getFacade().findBySQL(sql);
        }
        return suggestions;

    }

    public List<Item> completeService(String query) {
        List<Item> suggestions;
        String sql;
        HashMap hm = new HashMap();

        sql = "select c from Item c where c.retired=false and type(c)=:cls"
                + " and upper(c.name) like :q order by c.name";

        hm.put("cls", Service.class);
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getFacade().findBySQL(sql, hm, 20);

        return suggestions;

    }

    public List<Item> completeServiceWithoutProfessional(String query) {
        List<Item> suggestions;
        String sql;
        HashMap hm = new HashMap();

        sql = "select c from Item c where c.retired=false and type(c)=:cls"
                + " and upper(c.name) like :q "
                + " c.id in (Select f.item.id From Itemfee f where f.retired=false "
                + " and f.feeType!=:ftp ) order by c.name ";

        hm.put("ftp", FeeType.Staff);
        hm.put("cls", Service.class);
        hm.put("q", "%" + query.toUpperCase() + "%");
        suggestions = getFacade().findBySQL(sql, hm, 20);

        return suggestions;

    }

    public List<Item> completeMedicalPackage(String query) {
        List<Item> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c where c.retired=false and type(c)=MedicalPackage and upper(c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //System.out.println(sql);
            suggestions = getFacade().findBySQL(sql);
        }
        return suggestions;

    }

    public List<Item> completeInwardItems(String query) {
        List<Item> suggestions;
        HashMap m = new HashMap();
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c "
                    + " where c.retired=false "
                    + " and type(c)!=:pac "
                    + " and (type(c)=:ser "
                    + " or type(c)=:inv"
                    + " or type(c)=:ward)  "
                    + " and upper(c.name) like :q"
                    + " order by c.name";
            m.put("pac", Packege.class);
            m.put("ser", Service.class);
            m.put("inv", Investigation.class);
            m.put("ward", InwardService.class);
            m.put("q", "%" + query.toUpperCase() + "%");
            //    //System.out.println(sql);
            suggestions = getFacade().findBySQL(sql, m, 20);
        }
        return suggestions;
    }

    Category category;

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    private List<Item> fetchInwardItems(String query) {
        HashMap m = new HashMap();
        String sql;
        sql = "select c from Item c "
                + " where c.retired=false "
                + " and type(c)!=:pac "
                + " and (type(c)=:ser "
                + " or type(c)=:inward "
                + " or type(c)=:inv)  "
                + " and upper(c.name) like :q"
                + " order by c.name";
        m.put("pac", Packege.class);
        m.put("ser", Service.class);
        m.put("inward", InwardService.class);
        m.put("inv", Investigation.class);
        m.put("q", "%" + query.toUpperCase() + "%");

        return getFacade().findBySQL(sql, m, 20);

    }

    private List<Item> fetchInwardItems(String query, Category cat) {
        HashMap m = new HashMap();
        String sql;
        sql = "select c from Item c "
                + " where c.retired=false "
                + " and c.category=:ct"
                + " and type(c)!=:pac "
                + " and (type(c)=:ser "
                + " or type(c)=:inward "
                + " or type(c)=:inv)  "
                + " and upper(c.name) like :q"
                + " order by c.name";
        m.put("ct", cat);
        m.put("pac", Packege.class);
        m.put("ser", Service.class);
        m.put("inv", Investigation.class);
        m.put("inward", InwardService.class);
        m.put("q", "%" + query.toUpperCase() + "%");

        return getFacade().findBySQL(sql, m, 20);

    }

    @Inject
    ServiceSubCategoryController serviceSubCategoryController;

    public ServiceSubCategoryController getServiceSubCategoryController() {
        return serviceSubCategoryController;
    }

    public void setServiceSubCategoryController(ServiceSubCategoryController serviceSubCategoryController) {
        this.serviceSubCategoryController = serviceSubCategoryController;
    }

    public List<Item> completeInwardItemsCategory(String query) {
        List<Item> suggestions = new ArrayList<>();

        if (category == null) {
            System.err.println("1");
            suggestions = fetchInwardItems(query);
        } else if (category instanceof ServiceCategory) {
            System.err.println("2");
            suggestions = fetchInwardItems(query, category);
            getServiceSubCategoryController().setParentCategory(category);
            for (ServiceSubCategory ssc : getServiceSubCategoryController().getItems()) {
                suggestions.addAll(fetchInwardItems(query, ssc));
            }
        } else {
            System.err.println("3");
            suggestions = fetchInwardItems(query, category);
        }

        return suggestions;
    }

    public List<Item> completeOpdItemsByNamesAndCode(String query) {
        if (query == null || query.trim().equals("")) {
            return new ArrayList<>();
        }
        List<Item> suggestions;
        HashMap m = new HashMap();
        String sql;

        sql = "select c from Item c "
                + " where c.retired=false "
                + " and type(c)!=:pac "
                + " and type(c)!=:inw "
                + " and (type(c)=:ser "
                + " or type(c)=:inv)  "
                + " and (upper(c.name) like :q or upper(c.fullName) like :q or "
                + " upper(c.code) like :q or upper(c.printName) like :q ) "
                + " order by c.name";
        m.put("pac", Packege.class);
        m.put("inw", InwardService.class);
        m.put("ser", Service.class);
        m.put("inv", Investigation.class);
        m.put("q", "%" + query.toUpperCase() + "%");
        //    //System.out.println(sql);
        suggestions = getFacade().findBySQL(sql, m, 20);

        return suggestions;
    }

    public List<Item> completeOpdItems(String query) {
        List<Item> suggestions;
        HashMap m = new HashMap();
        String sql;
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {
            sql = "select c from Item c "
                    + " where c.retired=false "
                    + " and type(c)!=:pac "
                    + " and type(c)!=:inw "
                    + " and (type(c)=:ser "
                    + " or type(c)=:inv)  "
                    + " and upper(c.name) like :q"
                    + " order by c.name";
            m.put("pac", Packege.class);
            m.put("inw", InwardService.class);
            m.put("ser", Service.class);
            m.put("inv", Investigation.class);
            m.put("q", "%" + query.toUpperCase() + "%");
            //    //System.out.println(sql);
            suggestions = getFacade().findBySQL(sql, m, 20);
        }
        return suggestions;
    }

    public List<Item> completeItemWithoutPackOwn(String query) {
        List<Item> suggestions;
        String sql;
        if (query == null) {
            suggestions = new ArrayList<Item>();
        } else {
            sql = "select c from Item c where c.institution.id = " + getSessionController().getInstitution().getId() + " and c.retired=false and type(c)!=Packege and type(c)!=TimedItem and upper(c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //System.out.println(sql);
            suggestions = getFacade().findBySQL(sql);
        }
        return suggestions;
    }

    /**
     *
     */
    public ItemController() {
    }

    /**
     * Prepare to add new Collecting Centre
     *
     * @return
     */
    public void prepareAdd() {
        current = new Item();
    }

    /**
     *
     * @return
     */
    public ItemFacade getEjbFacade() {
        return ejbFacade;
    }

    /**
     *
     * @param ejbFacade
     */
    public void setEjbFacade(ItemFacade ejbFacade) {
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
    public Item getCurrent() {
        if (current == null) {
            current = new Item();
        }
        return current;
    }

    /**
     * Set the current item
     *
     * @param current
     */
    public void setCurrent(Item current) {
        this.current = current;
    }

    private ItemFacade getFacade() {
        return ejbFacade;
    }

    /**
     *
     * @return
     */
    public List<Item> getItems() {
        String temSql;
        HashMap h = new HashMap();
        temSql = "SELECT i FROM Item i where (type(i)=:t1 or type(i)=:t2 ) and i.retired=false order by i.department.name";
        h.put("t1", Investigation.class);
        h.put("t2", Service.class);
        items = getFacade().findBySQL(temSql, h, TemporalType.TIME);
        return items;
    }

    /**
     *
     * Set all Items to null
     *
     */
    private void recreateModel() {
        items = null;
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
     * Delete the current Item
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

    public Institution getInstituion() {
        return instituion;
    }

    public void setInstituion(Institution instituion) {
        this.instituion = instituion;
    }

    @FacesConverter(forClass = Item.class)
    public static class ItemControllerConverter implements Converter {

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
            ItemController controller = (ItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "itemController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key = 0l;
            try {
                key = Long.valueOf(value);
            } catch (Exception e) {

            }

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
            if (object instanceof Item) {
                Item o = (Item) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ItemController.class.getName());
            }
        }
    }

    /**
     *
     */
    @FacesConverter("itemcon")
    public static class ItemConverter implements Converter {

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
            ItemController controller = (ItemController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "itemController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key = 0l;
            try {
                key = Long.valueOf(value);
            } catch (Exception e) {

            }

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
            if (object instanceof Item) {
                Item o = (Item) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + ItemController.class.getName());
            }
        }
    }
}
