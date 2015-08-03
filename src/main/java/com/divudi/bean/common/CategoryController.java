/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
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
import com.divudi.entity.pharmacy.StoreItemCategory;
import com.divudi.facade.CategoryFacade;
import com.divudi.facade.util.JsfUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
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
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
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
    String selectText = "";

    @Inject
    ItemController itemController;
    
    public void fromTransferItemsFromFromCategoryToToCategory(){
        if(fromCategory==null){
            JsfUtil.addErrorMessage("From Category ?");
            return;
        }
        if(toCategory==null){
            JsfUtil.addErrorMessage("To Category");
            return;
        }
        List<Item> cis = itemController.getItems(fromCategory);
        for(Item i:cis){
            //System.out.println("i.getName() = " + i.getName());
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
        c = getFacade().findBySQL("select c from Category c where c.retired=false and upper(c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        if (c == null) {
            c = new ArrayList<>();
        }
        return c;
    }

    public List<Category> getSubCategories(Category cat) {
        List<Category> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();
        sql = "select c from Category c where c.retired=false and c.parentCategory=:cat order by c.name";
        tmpMap.put("cat", cat);
        suggestions = getFacade().findBySQL(sql, tmpMap);
        return suggestions;
    }

    public List<Category> completeServiceCategory(String query) {
        List<Category> suggestions;
        String sql;
        HashMap tmpMap = new HashMap();
        if (query == null) {
            suggestions = new ArrayList<>();
        } else {

            sql = "select c from Category c where c.retired=false and (type(c)= :sup or type(c)= :sub) and upper(c.name) like '%" + query.toUpperCase() + "%' order by c.name";
            ////System.out.println(sql);
            tmpMap.put("sup", ServiceCategory.class);
            tmpMap.put("sub", ServiceSubCategory.class);
            suggestions = getFacade().findBySQL(sql, tmpMap, TemporalType.TIMESTAMP);
        }
        return suggestions;
    }

    public List<Category> completeCategoryMatrix(String qry) {
        List<Category> c;
        String sql;
        Map temMap = new HashMap();

        sql = "select c from Category c where c.retired=false"
                + " and (type(c)= :service or type(c)= :sub or type(c)= :invest or "
                + "type(c)= :time or type(c)= :parm or type(c)= :con  ) and upper(c.name)"
                + " like :q order by c.name";

        temMap.put("service", ServiceCategory.class);
        temMap.put("sub", ServiceSubCategory.class);
        temMap.put("invest", InvestigationCategory.class);
        temMap.put("time", TimedItemCategory.class);
        temMap.put("parm", PharmaceuticalItemCategory.class);
        temMap.put("con", ConsumableCategory.class);
        temMap.put("q", "%" + qry.toUpperCase() + "%");

        c = getFacade().findBySQL(sql, temMap, TemporalType.DATE);

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
                + " and upper(c.name)"
                + " like :q order by c.name";

        temMap.put("service", ServiceCategory.class);
        temMap.put("sub", ServiceSubCategory.class);
        temMap.put("q", "%" + qry.toUpperCase() + "%");

        c = getFacade().findBySQL(sql, temMap, TemporalType.DATE);

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
                + " and upper(c.name)"
                + " like :q order by c.name";

        temMap.put("service", ServiceCategory.class);
        temMap.put("sub", ServiceSubCategory.class);
        temMap.put("invest", InvestigationCategory.class);
        temMap.put("q", "%" + qry.toUpperCase() + "%");

        c = getFacade().findBySQL(sql, temMap, TemporalType.DATE);

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

        c = getFacade().findBySQL(sql, temMap, TemporalType.DATE);

        return c;
    }

    public List<Category> completeCategoryServicePharmacy(String qry) {
        List<Category> c;
        String sql;
        Map temMap = new HashMap();

        sql = "select c from Category c where c.retired=false"
                + " and (type(c)= :service or type(c)= :sub or type(c)= :ph  )"
                + " and upper(c.name)"
                + " like :q order by c.name";

        temMap.put("service", ServiceCategory.class);
        temMap.put("sub", ServiceSubCategory.class);
        temMap.put("ph", PharmaceuticalItemCategory.class);

        temMap.put("q", "%" + qry.toUpperCase() + "%");

        c = getFacade().findBySQL(sql, temMap, TemporalType.DATE);

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
                + " and (type(c)= :invest ) and upper(c.name)"
                + " like :q order by c.name";

        temMap.put("invest", InvestigationCategory.class);
        temMap.put("q", "%" + qry.toUpperCase() + "%");

        c = getFacade().findBySQL(sql, temMap, TemporalType.DATE);

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
                + " and upper(c.name) like :q "
                + " order by c.name";

        temMap.put("parm", PharmaceuticalItemCategory.class);
        temMap.put("q", "%" + qry.toUpperCase() + "%");

        c = getFacade().findBySQL(sql, temMap, TemporalType.DATE);

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
                + " and (type(c)= :parm ) and upper(c.name)"
                + " like :q order by c.name";

        temMap.put("parm", StoreItemCategory.class);
        temMap.put("q", "%" + qry.toUpperCase() + "%");

        c = getFacade().findBySQL(sql, temMap, TemporalType.DATE);

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

        c = getFacade().findBySQL(sql, temMap, TemporalType.DATE);

        if (c == null) {
            c = new ArrayList<Category>();
        }
        return c;
    }

    public List<Category> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from Category c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
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
                + " and upper(c.name) like :q "
                + " order by c.name";

        temMap.put("parm", ConsumableCategory.class);
        temMap.put("assetcat", AssetCategory.class);
        temMap.put("q", "%" + qry.toUpperCase() + "%");

        cc = getFacade().findBySQL(sql, temMap, TemporalType.TIMESTAMP);

        if (cc == null) {
            cc = new ArrayList<>();
        }
        return cc;
    }

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

    private CategoryFacade getFacade() {
        return ejbFacade;
    }

    public List<Category> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    List<Category> nationalities;
    List<Category> religions;

    public List<Category> getNationalities() {
        if (nationalities == null) {
            String jpql;
            jpql = "Select n from Nationality n where n.retired=false order by n.name";
            nationalities = getFacade().findBySQL(jpql);
        }
        return nationalities;
    }

    public List<Category> getReligions() {
        if (religions == null) {
            String jpql;
            jpql = "Select n from Religion n where n.retired=false order by n.name";
            religions = getFacade().findBySQL(jpql);
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
            //System.out.println("cat = " + cat);
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

    /**
     *
     */
    @FacesConverter("categoryConverter")
    public static class CategoryConverter implements Converter {

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
