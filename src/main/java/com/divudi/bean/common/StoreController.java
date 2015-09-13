/*
 * MSc(Biomedical Informatics) Project
 *
 * Development and Implementation of a Web-based Combined Data Repository of
 Genealogical, Clinical, Storeoratory and Genetic Data
 * and
 * a Set of Related Tools
 */
package com.divudi.bean.common;
import java.util.TimeZone;
import com.divudi.data.DepartmentType;
import com.divudi.facade.DepartmentFacade;
import com.divudi.entity.Department;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.StockFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named; import javax.ejb.EJB;
import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, PGIM Trainee for MSc(Biomedical
 Informatics)
 */
@Named
@SessionScoped
public  class StoreController implements Serializable {

    private static final long serialVersionUID = 1L;
    @Inject
    SessionController sessionController;
    @EJB
    private DepartmentFacade ejbFacade;
    List<Department> selectedItems;
    private Department current;
    private List<Department> items = null;
    String selectText = "";
    @EJB
    StockFacade stockFacade;

    public List<Department> getSelectedItems() {
        selectedItems = getFacade().findBySQL("select c from Department c where c.retired=false and i.departmentType = com.divudi.data.DepartmentType.Store and upper(c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
        return selectedItems;
    }

    public void prepareAdd() {
        current = new Department();
        current.setDepartmentType(DepartmentType.Store);
    }

    // Need new Enum Department type
    
    
    public void setSelectedItems(List<Department> selectedItems) {
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
    
    public List<Stock> completeAllStocks(String qry) {
        List<Stock> items;
        String sql;
        double d = 0.0;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("dtp1", DepartmentType.Store); 
        m.put("stk", d);
        
        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select i from Stock i where i.department=:d "
                + " and i.itemBatch.item.departmentType=:dtp1"
                + " and i.stock > :stk "
                + " and (upper(i.itemBatch.item.name) like :n  or "
                + " upper(i.itemBatch.item.code) like :n  or  "
                + " upper(i.itemBatch.item.barcode) like :n ) ";
        items = getStockFacade().findBySQL(sql, m, 20);

        return items;
    }

    public void setSelectText(String selectText) {
        this.selectText = selectText;
    }

    public DepartmentFacade getEjbFacade() {
        return ejbFacade;
    }

    public void setEjbFacade(DepartmentFacade ejbFacade) {
        this.ejbFacade = ejbFacade;
    }

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public StockFacade getStockFacade() {
        return stockFacade;
    }

    public void setStockFacade(StockFacade stockFacade) {
        this.stockFacade = stockFacade;
    }
    
    

    public StoreController() {
    }

    public Department getCurrent() {
        if(current==null){
            current=new Department();
            current.setDepartmentType(DepartmentType.Store);
        }
        return current;
    }

    public void setCurrent(Department current) {
        this.current = current;
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

    private DepartmentFacade getFacade() {
        return ejbFacade;
    }

    public List<Department> getItems() {
       // items = getFacade().findAll("name", true);
        String sql="SELECT i FROM Department i where i.retired=false and i.departmentType = com.divudi.data.DepartmentType.Store order by i.name";
        items=getEjbFacade().findBySQL(sql);
        if(items==null){
            items=new ArrayList<Department>();
        }
        return items;
    }

    /**
     *
     */
    @FacesConverter(forClass = Department.class)
    public static class DepartmentControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StoreController controller = (StoreController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "storeController");
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
            if (object instanceof Department) {
                Department o = (Department) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type "
                        + object.getClass().getName() + "; expected type: " + StoreController.class.getName());
            }
        }
    }
}
