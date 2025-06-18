package com.divudi.service;

import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.entity.Department;
import com.divudi.core.entity.Institution;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.CommonFunctions;
import com.divudi.core.data.StockValueRow;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.TemporalType;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.Stateless;

/**
 *
 * @author M H B Ariyaratne, buddhika.ari@gmail.com
 *
 */
@Stateless
public class StockService {

    @EJB
    StockFacade stockFacade;

    // ChatGPT contributed - 2025-05
    @Asynchronous
    @Deprecated // Now these details are NOT recorded in the Stock. Instead have to take from Item Batch as it used to be
    public void addItemNamesToAllStocksAsync() {
        int batchSize = 500;
        int start = 0;
        List<Stock> batch;
        do {
            batch = stockFacade.findByJpqlWithRange("SELECT s FROM Stock s order by s.stock desc", start, batchSize);
            for (Stock s : batch) {
                if (s.getItemBatch() != null && s.getItemBatch().getItem() != null && s.getItemBatch().getItem().getName() != null) {
                    s.setItemName(s.getItemBatch().getItem().getName());
                    s.setBarcode(s.getItemBatch().getItem().getBarcode());
                    String code = s.getItemBatch().getItem().getCode();
                    Long longCode = CommonFunctions.stringToLong(code);
                    s.setLongCode(longCode);
                    s.setDateOfExpire(s.getItemBatch().getDateOfExpire());
                    s.setRetailsaleRate(s.getItemBatch().getRetailsaleRate());
                } else {
                    s.setItemName("UNKNOWN");
                }
            }
            stockFacade.batchEdit(batch, batchSize);
            start += batchSize;
        } while (!batch.isEmpty());
    }

// ChatGPT contributed - 2025-05
    @Deprecated // Now these details are NOT recorded in the Stock. Instead have to take from Item Batch as it used to be
    public void addItemNamesToAllStocks() {
        List<Stock> allStocks = stockFacade.findByJpql("SELECT s FROM Stock s");
        int count = 0;
        for (Stock s : allStocks) {
            if (s.getItemBatch() != null && s.getItemBatch().getItem() != null && s.getItemBatch().getItem().getName() != null) {
                s.setItemName(s.getItemBatch().getItem().getName());
                s.setBarcode(s.getItemBatch().getItem().getBarcode());
                String code = s.getItemBatch().getItem().getCode();
                Long longCode = CommonFunctions.stringToLong(code);
                s.setLongCode(longCode);
                s.setDateOfExpire(s.getItemBatch().getDateOfExpire());
                s.setRetailsaleRate(s.getItemBatch().getRetailsaleRate());
            } else {
                s.setItemName("UNKNOWN");
            }
            stockFacade.edit(s);
            count++;
        }
    }

    // ChatGPT contributed - 2025-06
    public StockValueRow calculateStockValues(Institution institution, Institution site, Department department) {
        System.out.println("calculateStockValues");
        System.out.println("department = " + department);
        Map<String, Object> params = new HashMap<>();
        StringBuilder jpql = new StringBuilder();
        jpql.append("select sum(s.stock * s.itemBatch.purcahseRate), "
                + "sum(s.stock * s.itemBatch.retailsaleRate), "
                + "sum(s.stock * coalesce(s.itemBatch.costRate,0)) "
                + "from Stock s where s.stock>0");

        if (department != null) {
            jpql.append(" and s.department=:dep");
            params.put("dep", department);
        } else if (site != null) {
            jpql.append(" and s.department.site=:site");
            params.put("site", site);
        } else if (institution != null) {
            jpql.append(" and s.department.institution=:ins");
            params.put("ins", institution);
        }

        Object[] obj = stockFacade.findAggregateModified(jpql.toString(), params, TemporalType.TIMESTAMP);
        
        
        StockValueRow row = new StockValueRow();
        row.setInstitution(institution);
        row.setSite(site);
        row.setDepartment(department);
        if (obj != null) {
            if (obj[0] != null) {
                row.setPurchaseValue((Double) obj[0]);
            }
            if (obj[1] != null) {
                row.setRetailValue((Double) obj[1]);
            }
            if (obj[2] != null) {
                row.setCostValue((Double) obj[2]);
            }
        }
        return row;
    }

}
