/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;

import com.divudi.entity.Category;
import com.divudi.entity.Item;
import com.divudi.entity.Nationality;
import com.divudi.entity.Religion;
import com.divudi.entity.ServiceCategory;
import com.divudi.entity.ServiceSubCategory;
import com.divudi.entity.inward.TimedItemCategory;
import com.divudi.entity.lab.InvestigationCategory;
import com.divudi.entity.pharmacy.AssetCategory;
import com.divudi.entity.pharmacy.ConsumableCategory;
import com.divudi.entity.pharmacy.PharmaceuticalCategory;
import com.divudi.entity.pharmacy.PharmaceuticalItemCategory;
import com.divudi.facade.CategoryFacade;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.SymanticHyrachi;
import com.divudi.data.SymanticType;
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
import javax.persistence.TemporalType;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class CategoryController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private CategoryFacade ejbFacade;
    List<Category> selectedItems;
    private Category current;
    Category fromCategory;
    Category toCategory;
    private List<Category> items = null;
    private List<Category> feeListTypes = null;
    String selectText = "";

    @Inject
    ItemController itemController;
    
    
    
    
    public String navigateToManageFeeListTypes(){
        fillFeeItemListTypes();
        return "/admin/pricing/fee_list_types";
    }
    
    private void fillFeeItemListTypes(){
        String jpql = "Select c "
                + " from Category c "
                + " where c.retired=:ret "
                + " and c.symanticType=:st "
                + " order by c.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("st", SymanticHyrachi.Fee_List_Type);
        feeListTypes = getFacade().findByJpql(jpql, m);
    }
    
    
    public void saveFeeListType(){
        if(current==null){
            JsfUtil.addErrorMessage("No Entity to save");
            return;
        }
        current.setSymanticType(SymanticHyrachi.Fee_List_Type);
        save(current);
        fillFeeItemListTypes();
    }
    
    public void prepareAddFeeListType(){
        current= new Category();
        current.setSymanticType(SymanticHyrachi.Fee_List_Type);
    }
    
    
    public void deleteFeeListType(){
        if(current==null){
            JsfUtil.addErrorMessage("No Entity to save");
            return;
        }
        current.setRetired(true);
        current.setRetiredAt(new Date());
        current.setRetirer(sessionController.getLoggedUser());
        save(current);
        fillFeeItemListTypes();
        JsfUtil.addSuccessMessage("Deleted");
    }
    
    
    
    

    public void fromTransferItemsFromFromCategoryToToCategory() {
        if (fromCategory == null) {
            JsfUtil.addErrorMessage("From Category ?");
            return;
        }
        if (toCategory == null) {
            JsfUtil.addErrorMessage("To Category");
            return;
        }
        List<Item> cis = itemController.getItems(fromCategory);
        for (Item i : cis) {
            ////// // System.out.println("i.getName() = " + i.getName());
            i.setCategory(toCategory);
            itemController.saveSelected(i);
        }
    }

    public Category getFromCategory() {
        return fromCategory;
    }

    public void setFromCategory(Category fromCategory) {
        this.fromCategory = fromCategory;
    }

    public Category getToCategory() {
        return toCategory;
    }

    public void setToCategory(Category toCategory) {
        this.toCategory = toCategory;
    }

    public List<Category> completeCategory(String qry) {
        List<Category> c;
        c = getFacade().findByJpql("select c from Category c where c.retired=false and c.name like '%" + qry.toUpperCase() + "%' order by c.name");
        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }
    
    public Category findAndCreateCategoryByName(String qry) {
        Category c;
        String jpql;
        jpql = "select c from "
                + " Category c "
                + " where c.retired=:ret "
                + " and c.name=:name "
                + " order by c.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("name", qry);
        c = getFacade().findFirstByJpql(jpql, m);
        if(c==null){
            c = new Category();
            c.setName(qry);
            c.setCode("category_" + CommonController.nameToCode(qry));
            getFacade().create(c);
        }
        return c;
    }
    
    
    public Category findCategoryByName(String qry) {
//        System.out.println("qry = " + qry);
        Category c;
        String jpql;
        jpql = "select c from "
                + " Category c "
                + " where c.retired=:ret "
                + " and c.name=:name "
                + " order by c.name";
        Map m = new HashMap();
        m.put("ret", false);
        m.put("name", qry);
        c = getFacade().findFirstByJpql(jpql, m);
        return c;
    }
    
    

    public List<Category> getSubCategories(Category cat) {
        List<Category> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();
        sql = "select c from Category c where c.retired=false and c.parentCategory=:cat order by c.name";
        tmpMap.put("cat", cat);
        suggestions = getFacade().findByJpql(sql, tmpMap);
        return suggestions;
    }

    public List<Category> completeServiceCategory(String query) {
        List<Category> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {

            sql = "select c from Category c where c.retired=false and (type(c)= :sup or type(c)= :sub) and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //////// // System.out.println(sql);
            tmpMap.put("sup", ServiceCategory.class);
            tmpMap.put("sub", ServiceSubCategory.class);
            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP);
        }
        return suggestions;
    }

    public List<Category> completeInvestigationCategory(String query) {
        List<Category> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {

            sql = "select c from Category c where c.retired=false and type(c)= :cat and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //////// // System.out.println(sql);
            tmpMap.put("cat", InvestigationCategory.class);
            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP);
        }
        return suggestions;
    }
    
    public List<Category> completeServiceInvestigationCategory(String query) {
        List<Category> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {

            sql = "select c from Category c where c.retired=false and (type(c)= :sup or type(c)= :sub or type(c)= :inv) and (c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            //////// // System.out.println(sql);
            tmpMap.put("sup", ServiceCategory.class);
            tmpMap.put("sub", ServiceSubCategory.class);
            tmpMap.put("inv", InvestigationCategory.class);
            suggestions = getFacade().findByJpql(sql, tmpMap, TemporalType.TIMESTAMP);
        }
        return suggestions;
    }

    public List<Category> completeCategoryMatrix(String qry) {
        List<Category> c;
        String sql;
        Map temMap = new HashMap();

        sql = "select c from Category c where c.retired=false"
                + " and (type(c)= :service or type(c)= :sub or type(c)= :invest or "
                + "type(c)= :time or type(c)= :parm or type(c)= :con  ) and (c.name)"
                + " like :q order by c.name";

        temMap.put("service", ServiceCategory.class);
        temMap.put("sub", ServiceSubCategory.class);
        temMap.put("invest", InvestigationCategory.class);
        temMap.put("time", TimedItemCategory.class);
        temMap.put("parm", PharmaceuticalItemCategory.class);
        temMap.put("con", ConsumableCategory.class);
        temMap.put("q", "%" + qry.toUpperCase() + "%");

        c = getFacade().findByJpql(sql, temMap, TemporalType.DATE);

        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public  List<Category> fetchCategoryList() {
        List<Category> c;
        String sql;
        Map temMap = new HashMap();

        sql = "select c from Category c where c.retired=false"
                + " and (type(c)= :service or type(c)= :sub or type(c)= :invest or "
                + " like :q order by c.name";

        temMap.put("service", ServiceCategory.class);
        temMap.put("sub", ServiceSubCategory.class);
        temMap.put("invest", InvestigationCategory.class);

        c = getFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public List<Category> completeCategoryService(String qry) {
        List<Category> c;
        String sql;
        Map temMap = new HashMap();

        sql = "select c from Category c where c.retired=false"
                + " and (type(c)= :service or type(c)= :sub  )"
                + " and (c.name)"
                + " like :q order by c.name";

        temMap.put("service", ServiceCategory.class);
        temMap.put("sub", ServiceSubCategory.class);
        temMap.put("q", "%" + qry.toUpperCase() + "%");

        c = getFacade().findByJpql(sql, temMap, TemporalType.DATE);

        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public List<Category> completeCategoryServiceInvestigation(String qry) {
        List<Category> c;
        String sql;
        Map temMap = new HashMap();

        sql = "select c from Category c "
                + " where c.retired=false"
                + " and (type(c)= :service "
                + " or type(c)= :sub "
                + " or type(c)=:invest )"
                + " and (c.name)"
                + " like :q order by c.name";

        temMap.put("service", ServiceCategory.class);
        temMap.put("sub", ServiceSubCategory.class);
        temMap.put("invest", InvestigationCategory.class);
        temMap.put("q", "%" + qry.toUpperCase() + "%");

        c = getFacade().findByJpql(sql, temMap, TemporalType.DATE);

        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public List<Category> getServiceCategory() {
        List<Category> c;
        String sql;
        Map temMap = new HashMap();

        sql = "select c from Category c "
                + " where c.retired=false"
                + " and (type(c)= :service "
                + " or type(c)= :sub  )"
                + "order by c.name";

        temMap.put("service", ServiceCategory.class);
        temMap.put("sub", ServiceSubCategory.class);

        c = getFacade().findByJpql(sql, temMap, TemporalType.DATE);

        return c;
    }

    public List<Category> completeCategoryServicePharmacy(String qry) {
        List<Category> c;
        String sql;
        Map temMap = new HashMap();

        sql = "select c from Category c where c.retired=false"
                + " and (type(c)= :service or type(c)= :sub or type(c)= :ph  )"
                + " and (c.name)"
                + " like :q order by c.name";

        temMap.put("service", ServiceCategory.class);
        temMap.put("sub", ServiceSubCategory.class);
        temMap.put("ph", PharmaceuticalItemCategory.class);

        temMap.put("q", "%" + qry.toUpperCase() + "%");

        c = getFacade().findByJpql(sql, temMap, TemporalType.DATE);

        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public List<Category> completeCategoryInvestigation(String qry) {
        List<Category> c;
        String sql;
        Map temMap = new HashMap();

        sql = "select c from Category c where c.retired=false"
                + " and (type(c)= :invest ) and (c.name)"
                + " like :q order by c.name";

        temMap.put("invest", InvestigationCategory.class);
        temMap.put("q", "%" + qry.toUpperCase() + "%");

        c = getFacade().findByJpql(sql, temMap, TemporalType.DATE);

        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public List<Category> completeCategoryPharmacy(String qry) {
        List<Category> c;
        String sql;
        Map temMap = new HashMap();

        sql = "select c from Category c"
                + "  where c.retired=false"
                + " and (type(c)= :parm ) "
                + " and (c.name) like :q "
                + " order by c.name";

        temMap.put("parm", PharmaceuticalItemCategory.class);
        temMap.put("q", "%" + qry.toUpperCase() + "%");

        c = getFacade().findByJpql(sql, temMap, TemporalType.DATE);

        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public List<Category> completeCategoryStore(String qry) {
        List<Category> c;
        String sql;
        Map temMap = new HashMap();

        sql = "select c from Category c where c.retired=false"
                + " and (type(c)= :parm ) and (c.name)"
                + " like :q order by c.name";

        temMap.put("parm", ConsumableCategory.class);
        temMap.put("q", "%" + qry.toUpperCase() + "%");

        c = getFacade().findByJpql(sql, temMap, TemporalType.DATE);

        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public List<Category> getItemsForPharmacy() {
        List<Category> c;
        String sql;
        Map temMap = new HashMap();

        sql = "select c from Category c where c.retired=false and (type(c)= :con)  order by c.name";

        temMap.put("con", PharmaceuticalCategory.class);

        c = getFacade().findByJpql(sql, temMap, TemporalType.DATE);

        if (c == null) {
            c = new ArrayList<Category>();
        }
        return c;
    }

    public List<Category> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from Category c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new Category();
    }

    public void setSelectedItems(List<Category> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }
    
    public void save(Category categoryToSave) {
        if(categoryToSave==null){
            JsfUtil.addErrorMessage("Nothing to save");
            return;
        }
        if (categoryToSave.getId() != null && categoryToSave.getId() > 0) {
            getFacade().edit(categoryToSave);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            categoryToSave.setCreatedAt(new Date());
            categoryToSave.setCreater(getSessionController().getLoggedUser());
            getFacade().create(categoryToSave);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    public void saveSelected() {
        save(current);
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public CategoryFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(CategoryFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public CategoryController() {
    }

    public Category getCurrent() {
        return current;
    }

    public void setCurrent(Category current) {
        this.current = current;
    }

    public List<Category> completeConsumableAndAssetCategory(String qry) {
        List<Category> cc;
        String sql;
        Map temMap = new HashMap();

        sql = "select c from Category c"
                + "  where c.retired=false"
                + " and (type(c)= :parm or type(c)=:assetcat)"
                + " and (c.name) like :q "
                + " order by c.name";

        temMap.put("parm", ConsumableCategory.class);
        temMap.put("assetcat", AssetCategory.class);
        temMap.put("q", "%" + qry.toUpperCase() + "%");

        cc = getFacade().findByJpql(sql, temMap, TemporalType.TIMESTAMP);

        if (cc == null) {
            cc = new ArrayList<>();
        }
        return cc;
    }

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

    private CategoryFacade getFacade() {
        return ejbFacade;
    }

    public List<Category> getItems() {
        if (items == null) {
            String j;
            j = "select c "
                    + " from Category c "
                    + " where c.retired=false "
                    + " order by c.name";
            items = getFacade().findByJpql(j);
        }
        return items;
    }

    List<Category> nationalities;
    List<Category> religions;

    public List<Category> getNationalities() {
        if (nationalities == null) {
            String jpql;
            jpql = "Select n from Nationality n where n.retired=false order by n.name";
            nationalities = getFacade().findByJpql(jpql);
        }
        return nationalities;
    }

    public List<Category> getReligions() {
        if (religions == null) {
            String jpql;
            jpql = "Select n from Religion n where n.retired=false order by n.name";
            religions = getFacade().findByJpql(jpql);
        }
        return religions;
    }

    public void addReligion() {
        if (selectText == null || selectText.trim().equals("")) {
            JsfUtil.addErrorMessage("Enter a name");
            return;
        }
        Religion r = new Religion();
        r.setName(selectText);
        r.setCreatedAt(new Date());
        r.setCreater(getSessionController().getLoggedUser());
        getFacade().create(r);
        religions = null;
        selectText = "";
        getReligions();
    }

    public void addNationality() {
        if (selectText == null || selectText.trim().equals("")) {
            JsfUtil.addErrorMessage("Enter a name");
            return;
        }
        Nationality r = new Nationality();
        r.setName(selectText);
        r.setCreatedAt(new Date());
        r.setCreater(getSessionController().getLoggedUser());
        getFacade().create(r);
        nationalities = null;
        selectText = "";
        getNationalities();
    }

    public void updateCategory(Category cat) {
        if (cat == null) {
            ////// // System.out.println("cat = " + cat);
            return;
        }
        getFacade().edit(cat);
        if (cat instanceof Religion) {
            religions = null;
            getReligions();
        } else if (cat instanceof Nationality) {
            nationalities = null;
            getNationalities();
        } else {
            items = null;
            getItems();
        }
    }

    public List<Category> getFeeListTypes() {
        if(feeListTypes==null){
            fillFeeItemListTypes();
        }
        return feeListTypes;
    }

    public void setFeeListTypes(List<Category> feeListTypes) {
        this.feeListTypes = feeListTypes;
    }
    
    
    

    /**
     *
     */
    @FacesConverter(forClass = Category.class)
    public static class CategoryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            CategoryController controller = (CategoryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "categoryController");
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
            if (object instanceof Category) {
                Category o = (Category) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + CategoryController.class.getName());
            }
        }
    }

    
}
