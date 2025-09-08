/*
 * Open Hospital Management Information System
 *
 * Dr M H B Ariyaratne
 * Acting Consultant (Health Informatics)
 * (94) 71 5812399
 * (94) 71 5812399
 */
package com.divudi.bean.pharmacy;

import com.divudi.bean.common.ConfigOptionApplicationController;
import com.divudi.bean.common.SessionController;
import com.divudi.core.util.JsfUtil;
import com.divudi.bean.store.StoreBean;
import com.divudi.core.data.DepartmentType;
import com.divudi.core.data.ItemLight;
import com.divudi.core.data.StockLight;
import com.divudi.core.data.dto.StockDTO;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.Amp;
import com.divudi.core.entity.pharmacy.Ampp;
import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.pharmacy.Vmp;
import com.divudi.core.facade.BillItemFacade;
import com.divudi.core.facade.DepartmentFacade;
import com.divudi.core.facade.ItemFacade;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.service.StockService;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
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
import javax.persistence.TemporalType;

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
    @Inject
    ConfigOptionApplicationController configOptionApplicationController;
    @EJB
    private StockFacade ejbFacade;
    @EJB
    private DepartmentFacade departmentFacade;
    @EJB
    StockService stockService;
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
        selectedItemStocks = new ArrayList<>();
        selectedItem = item;
        if (item == null) {
            return;
        }
        Amp amp;
        if (item instanceof Ampp) {
            Ampp ampp = (Ampp) item;
            amp = ampp.getAmp();
            if (amp == null) {
                return;
            }
        } else if (item instanceof Amp) {
            amp = (Amp) item;
        } else {
            return;
        }
        String jpql;
        Map params = new HashMap();
        double d = 0.0;
        params.put("s", d);
        params.put("item", amp);
        jpql = "select s "
                + "from Stock s "
                + "where s.stock > :s "
                + "and s.itemBatch.item = :item "
                + "order by s.itemBatch.dateOfExpire desc";
        selectedItemStocks = ejbFacade.findByJpql(jpql, params, 20);
        totalStockQty = calculateStockQty(selectedItemStocks);
    }

    public void listExpiaringStocks(Item item) {
        selectedItem = item;
        if (item instanceof Amp) {
            Amp amp = (Amp) item;
            List<Amp> amps = new ArrayList<>();
            amps.add(amp);
            selectedItemExpiaringStocks = fillExpiaringStock(null, amps, null);
        } else if (item instanceof Ampp) {
            Ampp ampp = (Ampp) item;
            Amp amp = ampp.getAmp();
            List<Amp> amps = new ArrayList<>();
            if (amp != null) {
                amps.add(amp);
            }
            selectedItemExpiaringStocks = fillExpiaringStock(null, amps, null);
        } else if (item instanceof Vmp) {
            List<Amp> amps = vmpController.ampsOfVmp(item);
            selectedItemExpiaringStocks = fillExpiaringStock(null, amps, null);
        } else {
            //TO Do for Vmpp
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

// ChatGPT contributed - 2025-05
    public void addItemNamesToAllStocks() {
        try {
            stockService.addItemNamesToAllStocks();
            JsfUtil.addSuccessMessage("Batch update has started in the background. You can continue using the system.");
        } catch (Exception e) {
            JsfUtil.addErrorMessage("Failed to start the batch update: " + e.getMessage());
        }
    }

    public List<Stock> completeAvailableStocks(String qry) {
        Set<Stock> stockSet = new LinkedHashSet<>(); // Preserve insertion order
        List<Stock> initialStocks = completeAvailableStocksStartsWith(qry);
        if (initialStocks != null) {
            stockSet.addAll(initialStocks);
        }

        // No need to check if initialStocks is empty or null anymore, Set takes care of duplicates
        Long itemCountToExtendStockSearch = configOptionApplicationController.getLongValueByKey("Minimum Item Count to extend search for Pharmacy Item Stocks", 5l);

        if (stockSet.size() <= itemCountToExtendStockSearch.intValue()) {
            List<Stock> additionalStocks = completeAvailableStocksContains(qry);
            if (additionalStocks != null) {
                stockSet.addAll(additionalStocks);
            }
        }

        return new ArrayList<>(stockSet);
    }

    public List<Stock> completeAvailableStocksWithItemStock(String qry) {
        Set<Stock> stockSet = new LinkedHashSet<>(); // Preserve insertion order
        List<Stock> initialStocks = completeAvailableStocksStartsWith(qry);
        if (initialStocks != null) {
            stockSet.addAll(initialStocks);
        }

        Integer minItemsForContainsSearch = configOptionApplicationController.getIntegerValueByKey("Minimum number of items before switching from 'starts with' to 'contains' search", 3);

        // No need to check if initialStocks is empty or null anymore, Set takes care of duplicates
        if (stockSet.size() <= minItemsForContainsSearch) {
            List<Stock> additionalStocks = completeAvailableStocksContains(qry);
            if (additionalStocks != null) {
                stockSet.addAll(additionalStocks);
            }
        }

        addItemStockToStocks(stockSet);

        return new ArrayList<>(stockSet);
    }

    public List<StockDTO> completeAvailableStocksWithItemStockDto(String qry) {
        if (qry == null || qry.trim().isEmpty()) {
            return Collections.emptyList();
        }

        qry = qry.replaceAll("[\\n\\r]", "").trim();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("department", getSessionController().getLoggedUser().getDepartment());
        parameters.put("stockMin", 0.0);
        parameters.put("query", "%" + qry + "%");
        parameters.put("barcode", qry.trim());

        boolean searchByItemCode = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by item code", true);
        boolean searchByBarcode = qry.length() > 6
                ? configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", true)
                : configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", false);
        boolean searchByGeneric = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by generic name(VMP)", false);

        // First, get stocks with individual stock quantities
        StringBuilder sql = new StringBuilder("SELECT NEW com.divudi.core.data.dto.StockDTO(")
                .append("i.id, i.itemBatch.item.name, i.itemBatch.item.code, i.itemBatch.item.vmp.name, ")
                .append("i.itemBatch.retailsaleRate, i.stock, i.itemBatch.dateOfExpire) ")
                .append("FROM Stock i ")
                .append("WHERE i.stock > :stockMin ")
                .append("AND i.department = :department ")
                .append("AND (");

        sql.append("i.itemBatch.item.name LIKE :query ");

        if (searchByItemCode) {
            sql.append("OR i.itemBatch.item.code LIKE :query ");
        }

        if (searchByBarcode) {
            sql.append("OR i.itemBatch.item.barcode = :barcode ");
        }

        if (searchByGeneric) {
            sql.append("OR i.itemBatch.item.vmp.vtm.name LIKE :query ");
        }

        sql.append(") ORDER BY i.itemBatch.item.name, i.itemBatch.dateOfExpire");

        List<StockDTO> stockDtos = (List<StockDTO>) getStockFacade().findLightsByJpql(sql.toString(), parameters, TemporalType.TIMESTAMP, 20);

        // Calculate total stock quantities per item
        addItemStockToStockDtos(stockDtos);

        return stockDtos;
    }

    public List<StockDTO> completeAvailableStocksWithItemStockDtoWithoutTotalStock(String qry) {
        if (qry == null || qry.trim().isEmpty()) {
            return Collections.emptyList();
        }

        qry = qry.replaceAll("[\\n\\r]", "").trim();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("department", getSessionController().getLoggedUser().getDepartment());
        parameters.put("stockMin", 0.0);
        parameters.put("query", "%" + qry + "%");
        

        boolean searchByItemCode = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by item code", true);
        boolean searchByBarcode = qry.length() > 6
                ? configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", true)
                : configOptionApplicationController.getBooleanValueByKey(
                        "Enable search medicines by barcode", false);
        boolean searchByGeneric = configOptionApplicationController.getBooleanValueByKey(
                "Enable search medicines by generic name(VMP)", false);

        // Get stocks with individual stock quantities only (no total stock calculation)
        StringBuilder sql = new StringBuilder("SELECT NEW com.divudi.core.data.dto.StockDTO(")
                .append("i.id, i.itemBatch.item.name, i.itemBatch.item.code, i.itemBatch.item.vmp.name, ")
                .append("i.itemBatch.retailsaleRate, i.stock, i.itemBatch.dateOfExpire) ")
                .append("FROM Stock i ")
                .append("WHERE i.stock > :stockMin ")
                .append("AND i.department = :department ")
                .append("AND (");

        sql.append("i.itemBatch.item.name LIKE :query ");

        if (searchByItemCode) {
            sql.append("OR i.itemBatch.item.code LIKE :query ");
        }

        if (searchByBarcode) {
            sql.append("OR i.itemBatch.item.barcode = :barcode ");
            parameters.put("barcode", qry.trim());
        }

        if (searchByGeneric) {
            sql.append("OR i.itemBatch.item.vmp.vtm.name LIKE :query ");
        }

        sql.append(") ORDER BY i.itemBatch.item.name, i.itemBatch.dateOfExpire");

        List<StockDTO> stockDtos = (List<StockDTO>) getStockFacade().findLightsByJpql(sql.toString(), parameters, TemporalType.TIMESTAMP, 20);

        // Do not calculate total stock quantities - return as is

        return stockDtos;
    }

    public List<Stock> completeAvailableStocksForAllowedDepartments(String qry) {
        Set<Stock> stockSet = new LinkedHashSet<>(); // Preserve insertion order
        List<Stock> initialStocks = completeAvailableStocksStartsWithForAllowedDepartments(qry);
        if (initialStocks != null) {
            stockSet.addAll(initialStocks);
        }

        Long itemCountToExtendStockSearch = configOptionApplicationController.getLongValueByKey("Minimum Item Count to extend search for Pharmacy Item Stocks", 5l);

        if (stockSet.size() <= itemCountToExtendStockSearch.intValue()) {
            List<Stock> additionalStocks = completeAvailableStocksContainsForAllowedDepartments(qry);
            if (additionalStocks != null) {
                stockSet.addAll(additionalStocks);
            }
        }

        return new ArrayList<>(stockSet);
    }

    public void addItemStockToStocks(Set<Stock> inputStocks) {
        if (inputStocks == null || inputStocks.isEmpty()) {
            return;
        }
        if (inputStocks.size() > 20) {
            return;
        }

        Map<Item, Double> itemStockTotals = new HashMap<>();

        // First pass: calculate the total stock quantity for each item
        for (Stock s : inputStocks) {
            if (s == null || s.getItemBatch() == null || s.getItemBatch().getItem() == null) {
                continue;
            }
            Item item = s.getItemBatch().getItem();
            itemStockTotals.put(item, itemStockTotals.getOrDefault(item, 0.0) + s.getStock());
        }

        // Second pass: set the total stock quantity for each stock
        for (Stock s : inputStocks) {
            if (s == null || s.getItemBatch() == null || s.getItemBatch().getItem() == null) {
                continue;
            }
            Item item = s.getItemBatch().getItem();
            s.setTransItemStockQty(itemStockTotals.get(item));
        }
    }

    public void addItemStockToStockDtos(List<StockDTO> inputStockDtos) {
        if (inputStockDtos == null || inputStockDtos.isEmpty()) {
            return;
        }
        if (inputStockDtos.size() > 20) {
            return;
        }

        Map<String, Double> itemStockTotals = new HashMap<>();

        // First pass: calculate the total stock quantity for each item
        for (StockDTO dto : inputStockDtos) {
            if (dto == null || dto.getItemName() == null) {
                continue;
            }
            String itemKey = dto.getItemName() + "_" + dto.getCode();
            itemStockTotals.put(itemKey, itemStockTotals.getOrDefault(itemKey, 0.0) + (dto.getStockQty() != null ? dto.getStockQty() : 0.0));
        }

        // Calculate total stock for each item across all departments
        for (StockDTO dto : inputStockDtos) {
            if (dto == null || dto.getItemName() == null) {
                continue;
            }

            // Get total stock for this item across all departments
            Map<String, Object> params = new HashMap<>();
            params.put("itemName", dto.getItemName());
            params.put("stockMin", 0.0);

            String sql = "SELECT COALESCE(SUM(s.stock), 0) FROM Stock s "
                    + "WHERE s.itemBatch.item.name = :itemName "
                    + "AND s.stock > :stockMin";

            try {
                Double totalStock = (Double) getStockFacade().findDoubleByJpql(sql, params);
                dto.setTotalStockQty(totalStock != null ? totalStock : 0.0);
            } catch (Exception e) {
                dto.setTotalStockQty(0.0);
            }
        }
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

    public List<Stock> completeAvailableStocksStartsWithForAllowedDepartments(String qry) {
        List<Stock> stockList;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("dts", sessionController.getAvailableDepartmentTypesForPharmacyTransactions());
        double d = 0.0;
        m.put("s", d);
        m.put("n", qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and i.itemBatch.item.departmentType in :dts and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n )  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and i.itemBatch.item.departmentType in :dts and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        stockList = getStockFacade().findByJpql(sql, m, 20);
        return stockList;
    }

    public List<Stock> completeAvailableStocksContainsForAllowedDepartments(String qry) {
        List<Stock> stockList;
        String sql;
        Map m = new HashMap();
        m.put("d", getSessionController().getLoggedUser().getDepartment());
        m.put("dts", sessionController.getAvailableDepartmentTypesForPharmacyTransactions());
        double d = 0.0;
        m.put("s", d);
        m.put("n", "%" + qry.toUpperCase() + "%");
        if (qry.length() > 4) {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and i.itemBatch.item.departmentType in :dts and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n or (i.itemBatch.item.barcode) like :n )  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        } else {
            sql = "select i from Stock i where i.stock >:s and i.department=:d and i.itemBatch.item.departmentType in :dts and ((i.itemBatch.item.name) like :n or (i.itemBatch.item.code) like :n)  order by i.itemBatch.item.name, i.itemBatch.dateOfExpire";
        }
        stockList = getStockFacade().findByJpql(sql, m, 20);
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

    @Deprecated
    public double findStock(Item item) {
        return findStock(null, item);
    }

    @Deprecated
    public double findStock(Institution institution, Item item) {
        if (item instanceof Amp) {
            Amp amp = (Amp) item;
            return findStock(institution, amp);
        } else if (item instanceof Ampp) {
            Ampp ampp = (Ampp) item;
            Amp amp = ampp.getAmp();
            return findStock(institution, amp);
        } else if (item instanceof Vmp) {
            List<Amp> amps = vmpController.ampsOfVmp(item);
            return findStock(institution, amps);
        } else {
            //TO Do for Ampp, Vmpp,
            return 0.0;
        }
    }

    @Deprecated
    public double findInstitutionStock(Institution institution, Item item) {
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

    @Deprecated
    public double findDepartmentStock(Department department, Item item) {
        if (item instanceof Amp) {
            Amp amp = (Amp) item;
            return findDepartmentStock(department, amp);
        } else if (item instanceof Vmp) {
            List<Amp> amps = vmpController.ampsOfVmp(item);
            return findStock(department, amps);
        } else {
            //TO Do for Ampp, Vmpp,
            return 0.0;
        }
    }

    @Deprecated
    public double findSiteStock(Institution site, Item item) {
        if (item instanceof Amp) {
            Amp amp = (Amp) item;
            return findSiteStock(site, amp);
        } else if (item instanceof Vmp) {
            List<Amp> amps = vmpController.ampsOfVmp(item);
            return findSiteStock(site, amps);
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
        Double stock;
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

    public double findSiteStock(Institution site, List<Amp> amps) {
        Double stock;
        String jpql;
        Map m = new HashMap();

        m.put("amps", amps);
        jpql = "select sum(i.stock) "
                + " from Stock i ";
        if (site == null) {
            jpql += " where i.itemBatch.item in :amps ";
        } else {
            m.put("ins", site);
            jpql += " where i.department.site=:ins "
                    + " and i.itemBatch.item in :amps ";
        }

        stock = billItemFacade.findDoubleByJpql(jpql, m);
        if (stock != null) {
            return stock;
        }
        return 0.0;
    }

    @Deprecated
    public double findStock(Institution institution, Institution site, Department department, Item item) {
        if (item instanceof Amp) {
            return findStock(institution, site, department, (Amp) item);
        } else {
            return 0.0;
        }
    }

    public double findStock(Institution institution, Institution site, Department department, Amp amp) {
        String jpql = "select sum(i.stock) from Stock i where i.retired=false and i.itemBatch.item=:amp";
        Map<String, Object> m = new HashMap<>();
        m.put("amp", amp);
        if (department != null) {
            jpql += " and i.department=:dep";
            m.put("dep", department);
        } else if (institution != null && site != null) {
            jpql += " and i.department.site=:site and i.department.institution=:ins";
            m.put("site", site);
            m.put("ins", institution);
        } else if (institution != null) {
            jpql += " and i.department.institution=:ins";
            m.put("ins", institution);
        } else if (site != null) {
            jpql += " and i.department.site=:site";
            m.put("site", site);
        }
        return billItemFacade.findDoubleByJpql(jpql, m);
    }

    public double findStock(Department department, List<Amp> amps) {
        Double stock;
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
        double stock;
        String jpql;
        Map m = new HashMap();
        Amp tamp = amps.get(0);
        int daysToMarkAsExpiaring;
        if (tamp != null) {
            daysToMarkAsExpiaring = tamp.getNumberOfDaysToMarkAsShortExpiary();
        } else {
            daysToMarkAsExpiaring = 30;
        }
        Calendar today = Calendar.getInstance();
        Calendar shortExpiryDate = Calendar.getInstance();
        shortExpiryDate.add(Calendar.DATE, daysToMarkAsExpiaring);
        Date doeStart = today.getTime();
        Date doeEnd = shortExpiryDate.getTime();
        m.put("amps", amps);
        m.put("doeStart", doeStart);
        m.put("doeEnd", doeEnd);
        jpql = "select sum(i.stock) "
                + " from Stock i ";
        if (institution == null) {
            jpql += " where i.itemBatch.item in :amps "
                    + " and i.itemBatch.dateOfExpire between :doeStart and :doeEnd ";
        } else {
            m.put("ins", institution);
            jpql += " where i.department.institution=:ins "
                    + " and i.itemBatch.item in :amps"
                    + " and i.itemBatch.dateOfExpire between :doeStart and :doeEnd ";
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
        Amp amp = amps.get(0);
        int daysToMarkAsExpiaring = amp.getNumberOfDaysToMarkAsShortExpiary();
        Calendar today = Calendar.getInstance();
        Calendar shortExpiryDate = Calendar.getInstance();
        shortExpiryDate.add(Calendar.DATE, daysToMarkAsExpiaring);
        Date doeStart = today.getTime();
        Date doeEnd = shortExpiryDate.getTime();

        if (inputShortExpiaryDate == null) {
            inputShortExpiaryDate = doeEnd;
        }
        jpql = "select i "
                + " from Stock i ";
        if (institution == null) {
            jpql += " where i.itemBatch.item in :amps "
                    + " and i.itemBatch.dateOfExpire between :doeStart and :doeEnd ";
        } else {
            m.put("ins", institution);
            jpql += " where i.department.institution=:ins "
                    + " and i.itemBatch.item in :amps "
                    + " and i.itemBatch.dateOfExpire between :doeStart and :doeEnd ";
        }
        jpql += " and i.stock > :sqty ";
        m.put("amps", amps);
        m.put("doeStart", doeStart);
        m.put("doeEnd", doeEnd);
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

    public void save(Stock s) {
        if (s == null) {
            return;
        }
        if (s.getId() != null && s.getId() > 0) {
            getFacade().edit(s);
            JsfUtil.addSuccessMessage("Updated Successfully.");
        } else {
            getFacade().create(s);
            JsfUtil.addSuccessMessage("Saved Successfully");
        }
    }

    public void saveSilantly(Stock s) {
        if (s == null) {
            return;
        }
        if (s.getId() != null && s.getId() > 0) {
            getFacade().edit(s);
        } else {
            getFacade().create(s);
        }
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

    public Stock findStockById(String strId) {
        if (strId == null || strId.isEmpty()) {
            throw new IllegalArgumentException("The provided ID string is null or empty.");
        }

        try {
            Long id = Long.parseLong(strId);
            return findStockById(id);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The provided ID string is not a valid number.", e);
        }
    }

    public Stock findStockById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("The provided ID is null.");
        }
        return getFacade().find(id);
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

    public StockLight findStockLightById(Long id) {
        if (id == null) {
            return null;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);

        String jpql = "SELECT NEW com.divudi.core.data.StockLight("
                + "s.id, "
                + "s.itemName, "
                + "s.code, "
                + "s.barcode, "
                + "s.dateOfExpire, "
                + "s.retailsaleRate"
                + "s.stock, "
                + ") FROM Stock s WHERE s.id = :id";

        List<StockLight> result = (List<StockLight>) getFacade().findLightsByJpql(jpql, params, TemporalType.DATE, 1);
        return result.isEmpty() ? null : result.get(0);
    }

    @FacesConverter("stockLightConverter")
    public static class StockLightConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext context, UIComponent component, String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                Long id = Long.valueOf(value);
                StockController controller = (StockController) context.getApplication().getELResolver()
                        .getValue(context.getELContext(), null, "stockController");
                StockLight sl = controller.findStockLightById(id);
                return sl;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        public String getAsString(FacesContext context, UIComponent component, Object value) {
            if (value instanceof StockLight) {
                return ((StockLight) value).getId().toString();
            }
            return null;
        }
    }

    /**
     *
     */
    @FacesConverter(forClass = Stock.class)
    public static class StockConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            try {
                StockController controller = (StockController) facesContext.getApplication().getELResolver()
                        .getValue(facesContext.getELContext(), null, "stockController");
                return controller.getEjbFacade().find(getKey(value));
            } catch (NumberFormatException e) {
                return null;
            }
        }

        Long getKey(String value) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException e) {
                // Rethrow the exception to handle it in getAsObject
                throw e;
            }
        }

        String getStringKey(Long value) {
            if (value == null) {
                return null;
            }
            return value.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Stock) {
                Stock stock = (Stock) object;
                return getStringKey(stock.getId());
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
