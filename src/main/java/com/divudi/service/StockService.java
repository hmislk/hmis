package com.divudi.service;

import com.divudi.core.entity.pharmacy.Stock;
import com.divudi.core.facade.StockFacade;
import com.divudi.core.util.CommonFunctions;
import java.util.List;
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
    public void addItemNamesToAllStocks() {
        int batchSize = 500;
        int start = 0;
        List<Stock> batch;
        do {
            batch = stockFacade.findByJpqlWithRange("SELECT s FROM Stock s", start, batchSize);
            for (Stock s : batch) {
                if (s.getItemBatch() != null && s.getItemBatch().getItem() != null && s.getItemBatch().getItem().getName() != null) {
                    s.setItemName(s.getItemBatch().getItem().getName());
                    s.setBarcode(s.getItemBatch().getItem().getBarcode());
                    String code = s.getItemBatch().getItem().getCode();
                    Long longCode = CommonFunctions.stringToLong(code);
                    s.setLongCode(longCode);
                } else {
                    s.setItemName("UNKNOWN");
                }
            }
            stockFacade.batchEdit(batch, batchSize);
            start += batchSize;
        } while (!batch.isEmpty());
    }

}
