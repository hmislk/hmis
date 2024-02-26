/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 Genealogical, Clinical, Storeoratory and Genetic Data
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.common;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.data.DepartmentType;
import com.divudi.entity.Department;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.StockFacade;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics)
 * Acting Consultant (Health Informatics)
 */
@Named
@SessionScoped
public class StoreController implements Serializable {

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
        selectedItems = getFacade().findByJpql("select c from Department c where c.retired=false and i.departmentType = com.divudi.data.DepartmentType.Store and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
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
                + " and ((i.itemBatch.item.name) like :n  or "
                + " (i.itemBatch.item.code) like :n  or  "
                + " (i.itemBatch.item.barcode) like :n ) ";
        items = getStockFacade().findByJpql(sql, m, 20);

        return items;
    }
    
    public List<Stock> completeAllStocksWithZero(String qry) {
        List<Stock> items;
        String sql;
//        double d = 0.0;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("dtp1", DepartmentType.Store);
//        m.put("stk", d);

        m.put("n", "%" + qry.toUpperCase() + "%");
        sql = "select i from Stock i where i.department=:d "
                + " and i.itemBatch.item.departmentType=:dtp1"
//                + " and i.stock > :stk "
                + " and ((i.itemBatch.item.name) like :n  or "
                + " (i.itemBatch.item.code) like :n  or  "
                + " (i.itemBatch.item.barcode) like :n ) "
                + " order by i.stock desc ";
        items = getStockFacade().findByJpql(sql, m, 40);

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
        if (current == null) {
            current = new Department();
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

    private DepartmentFacade getFacade() {
        return ejbFacade;
    }

    public List<Department> getItems() {
        if (items == null) {
            String sql = "SELECT i FROM Department i where i.retired=false and i.departmentType = com.divudi.data.DepartmentType.Store order by i.name";
            items = getEjbFacade().findByJpql(sql);
        }
        return items;
    }
}
