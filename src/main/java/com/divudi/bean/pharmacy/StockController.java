/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Laboratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.UtilityController;
import com.divudi.bean.store.StoreBean;
import com.divudi.data.DepartmentType;
import com.divudi.entity.Department;
import com.divudi.entity.Item;
import com.divudi.facade.StockFacade;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ItemFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Named;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 * Informatics)
 */
@Named
@SessionScoped
public class StockController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private StockFacade ejbFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    List<Stock> selectedItems;
    private Stock current;
    private List<Stock> items = null;
    String selectText = "";

    public List<Stock> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from Stock c where c.retired=false and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    @EJB
    ItemFacade itemFacade;

    public ItemFacade getItemFacade() {
        return itemFacade;
    }

    public void setItemFacade(ItemFacade itemFacade) {
        this.itemFacade = itemFacade;
    }

    @Inject
    StoreBean storeBean;

    public StoreBean getStoreBean() {
        return storeBean;
    }

    public void removeStoreItemsWithoutStocks() {
        Map m = new HashMap();
        m.put("dt", DepartmentType.Store);
        String jpsql = "Select i from Item i where i.departmentType=:dt and i.retired=false ";
        List<Item> items = getItemFacade().findBySQL(jpsql, m);
        for (Item i : items) {
            if (storeBean.getStockQty(i) < 0.0 || storeBean.getStockQty(i) == 0.0) {
                i.setRetired(true);
                i.setRetirer(getSessionController().getLoggedUser());
                i.setRetiredAt(new Date());
                i.setRetireComments("unnecessary");
                getItemFacade().edit(i);
            }
        }
    }

    public List<Stock> completeStock(String qry) {
        List<Stock> a = null;
        if (qry != null) {
            a = getFacade().findBySQL("select c from Stock c where c.retired=false and upper(c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
        }
        if (a == null) {
            a = new ArrayList<>();
        }
        return a;
    }

    public Double departmentItemStock(Department dept, Item item) {
        String sql;
        Map m = new HashMap();
        m.put("dept", dept);
        m.put("item", item);
        sql = "select sum(c.stock) "
                + " from Stock c where "
                + " c.department=:dept "
                + " and c.itemBatch.item=:item";
        return getFacade().findDoubleByJpql(sql, m);
    }

    public Double departmentItemStock(Item item) {
        return departmentItemStock(sessionController.getDepartment(), item);
    }

    public void prepareAdd() {
        current = new Stock();
    }

    public void setSelectedItems(List<Stock> selectedItems) {
        this.selectedItems = selectedItems;
    }

    public String getSelectText() {
        return selectText;
    }

    private void recreateModel() {
        items = null;
    }

    private boolean errorCheck() {
        if (getCurrent().getDepartment() == null) {
            UtilityController.addErrorMessage("Please Select Manufacturer");
            return true;
        }
        return false;
    }

    public void saveSelected() {
        if (errorCheck()) {
            return;
        }

        if (getCurrent().getId() != null && getCurrent().getId() > 0) {
            getFacade().edit(current);
            UtilityController.addSuccessMessage("Updated Successfully.");
        } else {
            getFacade().create(current);
            UtilityController.addSuccessMessage("Saved Successfully");
        }
        recreateModel();
        getItems();
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public StockFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(StockFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public StockController() {
    }

    public Stock getCurrent() {
        if (current == null) {
            current = new Stock();
        }
        return current;
    }

    public void setCurrent(Stock current) {
        this.current = current;
    }

    public void delete() {

        if (current != null) {
            getFacade().remove(current);
            UtilityController.addSuccessMessage("Deleted Successfully");
        } else {
            UtilityController.addSuccessMessage("Nothing to Delete");
        }
        recreateModel();
        getItems();
        current = null;
        getCurrent();
    }

    private StockFacade getFacade() {
        return ejbFacade;
    }

    public List<Stock> getItems() {
        items = getFacade().findAll("name", true);
        return items;
    }

    public DepartmentFacade getDepartmentFacade() {
        return departmentFacade;
    }

    public void setDepartmentFacade(DepartmentFacade departmentFacade) {
        this.departmentFacade = departmentFacade;
    }

    /**
     *
     */
    @FacesConverter(forClass = Stock.class)
    public static class StockConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StockController controller = (StockController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "stockController");
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
            if (object instanceof Stock) {
                Stock o = (Stock) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StockController.class.getName());
            }
        }
    }

    /**
     *
     */
    @FacesConverter("stockCon")
    public static class StockControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StockController controller = (StockController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "stockController");
            return controller.getEjbFacade().find(getKey(value));
        }

        java.lang.Long getKey(String value) {
            java.lang.Long key;
            try {
                key = Long.valueOf(value);
            } catch (NumberFormatException e) {
                key = 0l;
            }
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
            if (object instanceof Stock) {
                Stock o = (Stock) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StockController.class.getName());
            }
        }
    }
}
