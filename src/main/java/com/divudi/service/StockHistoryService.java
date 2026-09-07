package com.divudi.service;

import com.divudi.core.entity.Department;
import com.divudi.core.entity.HistoricalRecord;
import com.divudi.core.data.HistoricalRecordType;
import com.divudi.core.entity.Item;
import com.divudi.core.entity.pharmacy.StockHistory;
import com.divudi.core.facade.StockHistoryFacade;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.TemporalType;

/**
 *
 * @author M H B Ariyaratne
 *
 */
@Stateless
public class StockHistoryService {

    @EJB
    StockHistoryFacade stockHistoryFacade;
    @EJB
    ItemService itemService;
    @EJB
    HistoricalRecordService historicalRecordService;

    public StockHistory fetchOpeningStockHistory(Department department, Item item, Date date) {
        String jpql = "SELECT sh FROM StockHistory sh "
                + "WHERE sh.department = :department "
                + "AND sh.item = :item "
                + "AND sh.stockAt = :date "
                + "ORDER BY sh.id ASC";

        Map<String, Object> params = new HashMap<>();
        params.put("department", department);
        params.put("item", item);
        params.put("date", date);

        return stockHistoryFacade.findFirstByJpql(jpql, params, TemporalType.DATE);
    }

    public StockHistory fetchClosingStockHistory(Department department, Item item, Date date) {
        String jpql = "SELECT sh FROM StockHistory sh "
                + "WHERE sh.department = :department "
                + "AND sh.item = :item "
                + "AND sh.stockAt = :date "
                + "ORDER BY sh.id DESC";

        Map<String, Object> params = new HashMap<>();
        params.put("department", department);
        params.put("item", item);
        params.put("date", date);

        return stockHistoryFacade.findFirstByJpql(jpql, params, TemporalType.DATE);
    }

    public double fetchOpeningStockQuantity(Department department, Item item, Date date) {
        StockHistory sh = fetchOpeningStockHistory(department, item, date);
        return sh != null ? sh.getStockQty() : 0.0;
    }
    
    public double fetchClosingStockQuantity(Department department, Item item, Date date) {
        StockHistory sh = fetchClosingStockHistory(department, item, date);
        return sh != null ? sh.getStockQty() : 0.0;
    }

    public double fetchOpeningStockQuantity(Department department, Date date) {
        HistoricalRecord openingBalance = historicalRecordService.findRecord(HistoricalRecordType.PHARMACY_STOCK_VALUE_PURCHASE_RATE, null, null, department, date);
        if (openingBalance != null) {
            return openingBalance.getRecordValue();
        } else {
            List<Item> items = itemService.fetchAmps();
            return fetchOpeningStockQuantity(department, items, date);
        }
    }

    public double fetchClosingStockQuantity(Department department, Date date) {
        List<Item> items = itemService.fetchAmps();
        return fetchClosingStockQuantity(department, items, date);
    }

    public double fetchOpeningStockQuantity(Department department, List<Item> items, Date date) {
        double totalQuantity = 0.0;
        for (Item item : items) {
            totalQuantity += fetchOpeningStockQuantity(department, item, date);
        }
        return totalQuantity;
    }

    public double fetchClosingStockQuantity(Department department, List<Item> items, Date date) {
        double totalQuantity = 0.0;
        for (Item item : items) {
            totalQuantity += fetchClosingStockQuantity(department, item, date);
        }
        return totalQuantity;
    }

}
