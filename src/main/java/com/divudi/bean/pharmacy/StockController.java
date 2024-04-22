/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.SessionController;
import com.divudi.bean.common.util.JsfUtil;
import com.divudi.bean.store.StoreBean;
import com.divudi.data.DepartmentType;
import com.divudi.entity.Department;
import com.divudi.entity.Institution;
import com.divudi.entity.Item;
import com.divudi.entity.pharmacy.Amp;
import com.divudi.entity.pharmacy.Stock;
import com.divudi.entity.pharmacy.Vmp;
import com.divudi.facade.BillItemFacade;
import com.divudi.facade.DepartmentFacade;
import com.divudi.facade.ItemFacade;
import com.divudi.facade.StockFacade;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ejb.EJB;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.PrimeFaces;

/**
 *
 * @author Dr. M. H. B. Ariyaratne, MBBS, MSc, MD(Health Informatics) Acting
 * Consultant (Health Informatics)
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
    @EJB
    BillItemFacade billItemFacade;

    @Inject
    VmpController vmpController;

    private Stock stock;

    private List<Stock> selectedItemStocks;
    private List<Stock> selectedItemExpiaringStocks;
    private Item selectedItem;
    private double totalStockQty;
    private double expiaringStockQty;
    private Date shortExpiaryDate;

    public List<Stock> getSelectedItems() {
        selectedItems = getFacade().findByJpql("select c from Stock c where c.retired=false and (c.name) like '%" + getSelectText().toUpperCase() + "%' order by c.name");
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

    public void listStocksOfSelectedItem(Item item) {
        selectedItemStocks = null;
        selectedItem = item;
        if (item == null) {
            return;
        }
        String sql;
        Map m = new HashMap();
        double d = 0.0;
        m.put("s", d);
        m.put("item", item);
        sql = "select s "
                + "from Stock s "
                + "where s.stock > :s "
                + "and s.itemBatch.item = :item "
                + "order by s.itemBatch.dateOfExpire desc";
        selectedItemStocks = ejbFacade.findByJpql(sql, m, 20);
        totalStockQty = calculateStockQty(selectedItemStocks);
    }

    public void listExpiaringStocks(Item item) {
        selectedItem = item;
        if (item instanceof Amp) {
            Amp amp = (Amp) item;
            List<Amp> amps = new ArrayList<>();
            amps.add(amp);
            selectedItemExpiaringStocks = fillExpiaringStock(null, amps, null);
        } else if (item instanceof Vmp) {
            List<Amp> amps = vmpController.ampsOfVmp(item);
            selectedItemExpiaringStocks = fillExpiaringStock(null, amps, null);
        } else {
            //TO Do for Ampp, Vmpp,
        }
        expiaringStockQty = calculateStockQty(selectedItemExpiaringStocks);
    }

    public void relistExpiaringStocks() {
        if (selectedItem instanceof Amp) {
            Amp amp = (Amp) selectedItem;
            List<Amp> amps = new ArrayList<>();
            amps.add(amp);
            selectedItemExpiaringStocks = fillExpiaringStock(null, amps, shortExpiaryDate);
        } else if (selectedItem instanceof Vmp) {
            List<Amp> amps = vmpController.ampsOfVmp(selectedItem);
            selectedItemExpiaringStocks = fillExpiaringStock(null, amps, shortExpiaryDate);
        } else {
            //TO Do for Ampp, Vmpp,
        }
        expiaringStockQty = calculateStockQty(selectedItemExpiaringStocks);
    }

    private double calculateStockQty(List<Stock> stks) {
        if (stks == null) {
            return 0.0;
        }
        double d = 0.0;
        for (Stock s : stks) {
            d += s.getStock();
        }
        return d;
    }

    public List<Stock> completeAvailableStocks(String qry) {
        Set<Stock> stockSet = new LinkedHashSet<>(); // Preserve insertion order
        List<Stock> initialStocks = completeAvailableStocksStartsWith(qry);
        if (initialStocks != null) {
            stockSet.addAll(initialStocks);
        }

        // No need to check if initialStocks is empty or null anymore, Set takes care of duplicates
        if (stockSet.size() <= 10) {
            List<Stock> additionalStocks = completeAvailableStocksContains(qry);
            if (additionalStocks != null) {
                stockSet.addAll(additionalStocks);
            }
        }

        return new ArrayList<>(stockSet);
    }

    public List<Stock> findNextAvailableStocks(Stock stock) {
        List<Stock> stockList;
        String jpql;
        Map<String, Object> params = new HashMap<>();
        jpql = "select i "
                + " from Stock i "
                + " where i.stock > :s "
                + " and i.department = :d "
                + " and i.itemBatch.item = :item"
                + " and i != :stock"
                + " and i.itemBatch.dateOfExpire > CURRENT_DATE" // Exclude expired stocks
                + " order by i.itemBatch.dateOfExpire";
        params.put("d", getSessionController().getLoggedUser().getDepartment());
        params.put("s", 0.0);
        params.put("stock", stock);
        params.put("item", stock.getItemBatch().getItem());
        stockList = getStockFacade().findByJpql(jpql, params, 20);
        return stockList;
    }

    public List<Stock> findNextAvailableStocks(List<Stock> excludedStocks) {
        List<Stock> stockList;
        String jpql;
        Map<String, Object> params = new HashMap<>();
        jpql = "select i "
                + " from Stock i "
                + " where i.stock > :s "
                + " and i.department = :d "
                + " and i.itemBatch.dateOfExpire > CURRENT_DATE"
                + " and i.itemBatch.item = :item"
                + (excludedStocks.isEmpty() ? "" : " and i NOT IN :excludedStocks") // Exclude stocks in the provided list if not empty
                + " order by i.itemBatch.dateOfExpire";
        params.put("d", getSessionController().getLoggedUser().getDepartment());
        params.put("s", 0.0);
        // Assuming all stocks in the list belong to the same item, which might need checking or adjustment in real scenarios.
        params.put("item", excludedStocks.get(0).getItemBatch().getItem());
        if (!excludedStocks.isEmpty()) {
            params.put("excludedStocks", excludedStocks);
        }
        stockList = getStockFacade().findByJpql(jpql, params, 20);
        return stockList;
    }

    public List<Stock> completeAvailableStocksStartsWith(String qry) {
        List<Stock> stockList;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        m.put("n", qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n )  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        stockList = getStockFacade().findByJpql(sql, m, 20);
//        itemsWithoutStocks = completeRetailSaleItems(qry);
        //////System.out.println("selectedSaleitems = " + itemsWithoutStocks);
        return stockList;
    }

    public List<Stock> completeAvailableStocksContains(String qry) {
        List<Stock> stockList;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n )  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        stockList = getStockFacade().findByJpql(sql, m, 20);
//        itemsWithoutStocks = completeRetailSaleItems(qry);
        //////System.out.println("selectedSaleitems = " + itemsWithoutStocks);
        return stockList;
    }

    public void removeStoreItemsWithoutStocks() {
        Map m = new HashMap();
        m.put("dt", DepartmentType.Store);
        String jpsql = "Select i from Item i where i.departmentType=:dt and i.retired=false ";
        List<Item> items = getItemFacade().findByJpql(jpsql, m);
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

    public double findStock(Item item) {
        return findStock(null, item);
    }

    public double findStock(Institution institution, Item item) {
        if (item instanceof Amp) {
            Amp amp = (Amp) item;
            return findStock(institution, amp);
        } else if (item instanceof Vmp) {
            List<Amp> amps = vmpController.ampsOfVmp(item);
            return findStock(institution, amps);
        } else {
            //TO Do for Ampp, Vmpp,
            return 0.0;
        }
    }

    public double findStock(Institution institution, Amp item) {
        List<Amp> amps = new ArrayList<>();
        amps.add(item);
        return findStock(institution, amps);
    }

    public double findExpiaringStock(Item item) {
        return findExpiaringStock(null, item);
    }

    public double findStock(Institution institution, List<Amp> amps) {
        Double stock = null;
        String jpql;
        Map m = new HashMap();

        m.put("amps", amps);
        jpql = "select sum(i.stock) "
                + " from Stock i ";
        if (institution == null) {
            jpql += " where i.itemBatch.item in :amps ";
        } else {
            m.put("ins", institution);
            jpql += " where i.department.institution=:ins "
                    + " and i.itemBatch.item in :amps ";
        }

        stock = billItemFacade.findDoubleByJpql(jpql, m);
        if (stock != null) {
            return stock;
        }
        return 0.0;
    }

    public double findStock(Department department, List<Amp> amps) {
        Double stock = null;
        String jpql;
        Map m = new HashMap();

        m.put("amps", amps);
        jpql = "select sum(i.stock) "
                + " from Stock i ";
        if (department == null) {
            jpql += " where i.itemBatch.item in :amps ";
        } else {
            m.put("dep", department);
            jpql += " where i.department=:dep "
                    + " and i.itemBatch.item in :amps ";
        }

        stock = billItemFacade.findDoubleByJpql(jpql, m);
        if (stock != null) {
            return stock;
        }
        return 0.0;
    }

    public double findExpiaringStock(Institution institution, Item item) {
        if (item instanceof Amp) {
            Amp amp = (Amp) item;
            return findExpiaringStock(institution, amp);
        } else if (item instanceof Vmp) {
            List<Amp> amps = vmpController.ampsOfVmp(item);
            return findExpiaringStock(institution, amps);
        } else {
            //TO Do for Ampp, Vmpp,
            return 0.0;
        }
    }

    public double findExpiaringStock(Institution institution, Amp item) {
        List<Amp> amps = new ArrayList<>();
        amps.add(item);
        return findExpiaringStock(institution, amps);
    }

    public double findExpiaringStock(Institution institution, List<Amp> amps) {
        if (amps == null) {
            return 0.0;
        }
        if (amps.isEmpty()) {
            return 0.0;
        }
        Double stock = null;
        String jpql;
        Map m = new HashMap();
        Vmp tvmp = amps.get(0).getVmp();
        int daysToMarkAsExpiaring;
        if (tvmp != null) {
            daysToMarkAsExpiaring = tvmp.getNumberOfDaysToMarkAsShortExpiary();
        } else {
            daysToMarkAsExpiaring = 30;
        }
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, daysToMarkAsExpiaring);
        Date doe = c.getTime();
        m.put("amps", amps);
        m.put("doe", doe);
        jpql = "select sum(i.stock) "
                + " from Stock i ";
        if (institution == null) {
            jpql += " where i.itemBatch.item in :amps "
                    + " and i.itemBatch.dateOfExpire < :doe ";
        } else {
            m.put("ins", institution);
            jpql += " where i.department.institution=:ins "
                    + " and i.itemBatch.item in :amps ";
        }

        stock = billItemFacade.findDoubleByJpql(jpql, m);

        return stock;

    }

    public List<Stock> fillExpiaringStock(Institution institution, List<Amp> amps, Date inputShortExpiaryDate) {
        if (amps == null) {
            return null;
        }
        if (amps.isEmpty()) {
            return null;
        }
        String jpql;
        Map m = new HashMap();
        Vmp tvmp = amps.get(0).getVmp();
        int daysToMarkAsExpiaring = tvmp.getNumberOfDaysToMarkAsShortExpiary();
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, daysToMarkAsExpiaring);
        Date doe = c.getTime();
        m.put("amps", amps);

        if (inputShortExpiaryDate == null) {
            inputShortExpiaryDate = doe;
        }
        m.put("doe", inputShortExpiaryDate);
        jpql = "select i "
                + " from Stock i ";
        if (institution == null) {
            jpql += " where i.itemBatch.item in :amps "
                    + " and i.itemBatch.dateOfExpire < :doe ";
        } else {
            m.put("ins", institution);
            jpql += " where i.department.institution=:ins "
                    + " and i.itemBatch.item in :amps ";
        }
        jpql += " and i.stock > :sqty ";
        m.put("sqty", 0.0);

        return billItemFacade.findByJpql(jpql, m);
    }

    public List<Stock> completeStock(String qry) {
        List<Stock> a = null;
        if (qry != null) {
            a = getFacade().findByJpql("select c from Stock c where c.retired=false and (c.name) like '%" + qry.toUpperCase() + "%' order by c.name");
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
            JsfUtil.addErrorMessage("Please Select Manufacturer");
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
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getFacade().create(current);
            JsfUtil.addSuccessMessage("Saved Successfully");
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
            JsfUtil.addSuccessMessage("Deleted Successfully");
        } else {
            JsfUtil.addSuccessMessage("Nothing to Delete");
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

    private StockFacade getStockFacade() {
        return ejbFacade;
    }

    public Stock getStock() {
        return stock;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public List<Stock> getSelectedItemStocks() {
        return selectedItemStocks;
    }

    public void setSelectedItemStocks(List<Stock> selectedItemStocks) {
        this.selectedItemStocks = selectedItemStocks;
    }

    public Item getSelectedItem() {
        return selectedItem;
    }

    public void setSelectedItem(Item selectedItem) {
        this.selectedItem = selectedItem;
    }

    public double getTotalStockQty() {
        return totalStockQty;
    }

    public void setTotalStockQty(double totalStockQty) {
        this.totalStockQty = totalStockQty;
    }

    public double getExpiaringStockQty() {
        return expiaringStockQty;
    }

    public void setExpiaringStockQty(double expiaringStockQty) {
        this.expiaringStockQty = expiaringStockQty;
    }

    public Date getShortExpiaryDate() {
        return shortExpiaryDate;
    }

    public void setShortExpiaryDate(Date shortExpiaryDate) {
        this.shortExpiaryDate = shortExpiaryDate;
    }

    public List<Stock> getSelectedItemExpiaringStocks() {
        return selectedItemExpiaringStocks;
    }

    public void setSelectedItemExpiaringStocks(List<Stock> selectedItemExpiaringStocks) {
        this.selectedItemExpiaringStocks = selectedItemExpiaringStocks;
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
                        + object.getClass().getName() + "; expected type: " + Stock.class.getName());
            }
        }
    }

    /**
     *
     */
}
